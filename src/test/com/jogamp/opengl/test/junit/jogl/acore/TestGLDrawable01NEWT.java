/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
 
package com.jogamp.opengl.test.junit.jogl.acore;

import java.io.IOException;

import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

public class TestGLDrawable01NEWT extends UITestCase {
    static final int width = 200, height = 200;

    void doTest(String profile, boolean onscreen, boolean fbo, boolean pbuffer) throws InterruptedException {
        if( !GLProfile.isAvailable(profile) )  {
            System.err.println("Profile "+profile+" n/a");
            return;
        }

        final GLProfile glp = GLProfile.get(profile);
        final GLCapabilities reqGLCaps = new GLCapabilities(glp);
        
        reqGLCaps.setOnscreen(onscreen);
        reqGLCaps.setPBuffer(!onscreen && pbuffer);
        reqGLCaps.setFBO(!onscreen && fbo);
        reqGLCaps.setDoubleBuffered(onscreen);
        // System.out.println("Requested: "+caps);

        //
        // Create native windowing resources .. X11/Win/OSX
        // 
        Window window = NewtFactory.createWindow(reqGLCaps);
        Assert.assertNotNull(window);
        window.setSize(width, height);
        window.setVisible(true);
        AWTRobotUtil.waitForVisible(window, true);
        AWTRobotUtil.waitForRealized(window, true);
        // System.out.println("Created: "+window);

        // Check caps of NativeWindow config w/o GL
        final CapabilitiesImmutable chosenCaps = window.getGraphicsConfiguration().getChosenCapabilities();
        Assert.assertNotNull(chosenCaps);
        Assert.assertTrue(chosenCaps.getGreenBits()>5);
        Assert.assertTrue(chosenCaps.getBlueBits()>5);
        Assert.assertTrue(chosenCaps.getRedBits()>5);

        //
        // Create native OpenGL resources .. XGL/WGL/CGL .. 
        // equivalent to GLAutoDrawable methods: setVisible(true)
        // 
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);
        
        final GLDrawable drawable = factory.createGLDrawable(window);
        Assert.assertNotNull(drawable);
        // System.out.println("Pre: "+drawable);
        //
        drawable.setRealized(true);
        Assert.assertTrue(drawable.isRealized());

        // Check caps of GLDrawable after realization
        final GLCapabilitiesImmutable chosenGLCaps = drawable.getChosenGLCapabilities();
        Assert.assertNotNull(chosenGLCaps);
        Assert.assertTrue(chosenGLCaps.getGreenBits()>5);
        Assert.assertTrue(chosenGLCaps.getBlueBits()>5);
        Assert.assertTrue(chosenGLCaps.getRedBits()>5);
        Assert.assertTrue(chosenGLCaps.getDepthBits()>4);
        Assert.assertEquals(reqGLCaps.isOnscreen(), chosenGLCaps.isOnscreen());
        Assert.assertEquals(reqGLCaps.isOnscreen(), chosenGLCaps.getDoubleBuffered()); // offscreen shall be !dbl-buffer
        // System.out.println("Post: "+drawable);

        GLContext context = drawable.createContext(null);
        Assert.assertNotNull(context);
        // System.out.println(context);
        
        int res = context.makeCurrent();
        Assert.assertTrue(GLContext.CONTEXT_CURRENT_NEW==res || GLContext.CONTEXT_CURRENT==res);

        // draw something ..
        final GL gl = context.getGL();
        gl.glClearColor(1, 1, 1, 1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        gl.glViewport(0, 0, width, height);        
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        drawable.swapBuffers();
        context.release();

        Thread.sleep(50);
        
        context.destroy();    
        drawable.setRealized(false);
        window.destroy();
        // System.out.println("Final: "+window);
    }

    @Test
    public void testGL2OnScreen() throws InterruptedException {
        doTest(GLProfile.GL2, true, false, false);
    }

    @Test
    public void testES2OnScreen() throws InterruptedException {
        doTest(GLProfile.GLES2, true, false, false);
    }
    
    @Test
    public void testGL2PBuffer() throws InterruptedException {
        doTest(GLProfile.GL2, false, false, true);
    }

    @Test
    public void testES2PBuffer() throws InterruptedException {
        doTest(GLProfile.GLES2, false, false, true);
    }
    
    // @Test // TODO: FBO-Drawable via createGLDrawable and pre-exisiting NativeSurface
    public void testGL2FBO() throws InterruptedException {
        doTest(GLProfile.GL2, false, true, false);
    }

    // @Test // TODO: FBO-Drawable via createGLDrawable and pre-exisiting NativeSurface
    public void testES2FBO() throws InterruptedException {
        doTest(GLProfile.GLES2, false, true, false);
    }
    
    public static void main(String args[]) throws IOException {
        org.junit.runner.JUnitCore.main(TestGLDrawable01NEWT.class.getName());
    }

}

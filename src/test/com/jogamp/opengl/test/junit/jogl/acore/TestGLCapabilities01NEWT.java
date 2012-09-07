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
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

public class TestGLCapabilities01NEWT extends UITestCase {
    static final int width = 100;
    static final int height = 100;

    boolean checkProfile(String profile) {
        if( !GLProfile.isAvailable(profile) )  {
            System.err.println("Profile "+profile+" n/a");
            return false;
        }
        return true;
    }
    
    void doTest(GLCapabilities reqGLCaps, GLEventListener demo) throws InterruptedException {
        System.out.println("Requested  GL Caps: "+reqGLCaps);

        //
        // Create native windowing resources .. X11/Win/OSX
        // 
        final Window window = NewtFactory.createWindow(reqGLCaps);
        Assert.assertNotNull(window);
        window.setSize(width, height);
        window.setVisible(true);
        Assert.assertTrue(AWTRobotUtil.waitForVisible(window, true));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(window, true));
        System.out.println("Window: "+window.getClass().getName());
        
        // Check caps of NativeWindow config w/o GL
        final CapabilitiesImmutable chosenCaps = window.getGraphicsConfiguration().getChosenCapabilities();
        System.out.println("Window Caps Pre_GL: "+chosenCaps);
        Assert.assertNotNull(chosenCaps);
        Assert.assertTrue(chosenCaps.getGreenBits()>5);
        Assert.assertTrue(chosenCaps.getBlueBits()>5);
        Assert.assertTrue(chosenCaps.getRedBits()>5);

        //
        // Create native OpenGL resources .. XGL/WGL/CGL .. 
        // equivalent to GLAutoDrawable methods: setVisible(true)
        // 
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(reqGLCaps.getGLProfile());
        
        final GLDrawable drawable = factory.createGLDrawable(window);
        Assert.assertNotNull(drawable);
        System.out.println("Drawable    Pre-GL(0): "+drawable.getClass().getName()+", "+drawable.getNativeSurface().getClass().getName());
        
        //
        drawable.setRealized(true);
        Assert.assertTrue(drawable.isRealized());

        System.out.println("Window Caps PostGL   : "+window.getGraphicsConfiguration().getChosenCapabilities());
        System.out.println("Drawable   Post-GL(1): "+drawable.getClass().getName()+", "+drawable.getNativeSurface().getClass().getName());
        
        // Check caps of GLDrawable after realization
        final GLCapabilitiesImmutable chosenGLCaps = drawable.getChosenGLCapabilities();
        System.out.println("Chosen     GL Caps(1): "+chosenGLCaps);
        Assert.assertNotNull(chosenGLCaps);
        Assert.assertTrue(chosenGLCaps.getGreenBits()>5);
        Assert.assertTrue(chosenGLCaps.getBlueBits()>5);
        Assert.assertTrue(chosenGLCaps.getRedBits()>5);
        Assert.assertTrue(chosenGLCaps.getDepthBits()>4);
        Assert.assertEquals(reqGLCaps.isOnscreen(), chosenGLCaps.isOnscreen());
        Assert.assertEquals(reqGLCaps.isFBO(), chosenGLCaps.isFBO());
        Assert.assertEquals(reqGLCaps.isPBuffer(), chosenGLCaps.isPBuffer());
        Assert.assertEquals(reqGLCaps.isBitmap(), chosenGLCaps.isBitmap());
        if(chosenGLCaps.isOnscreen() || chosenGLCaps.isFBO()) {
            // dbl buffer may be disabled w/ offscreen pbuffer and bitmap
            Assert.assertEquals(reqGLCaps.getDoubleBuffered(), chosenGLCaps.getDoubleBuffered());
        }

        GLContext context = drawable.createContext(null);
        Assert.assertNotNull(context);
        int res = context.makeCurrent();
        Assert.assertTrue(GLContext.CONTEXT_CURRENT_NEW==res || GLContext.CONTEXT_CURRENT==res);
        context.release();
        
        System.out.println("Chosen     GL Caps(2): "+drawable.getChosenGLCapabilities());
        System.out.println("Drawable   Post-GL(2): "+drawable.getClass().getName()+", "+drawable.getNativeSurface().getClass().getName());
        
        drawable.setRealized(false);
        window.destroy();
    }

    @Test
    public void testAvailableInfo() {
        GLDrawableFactory f = GLDrawableFactory.getDesktopFactory();
        if(null != f) {
            System.err.println(JoglVersion.getDefaultOpenGLInfo(f.getDefaultDevice(), null, true).toString());
        }
        f = GLDrawableFactory.getEGLFactory();
        if(null != f) {
            System.err.println(JoglVersion.getDefaultOpenGLInfo(f.getDefaultDevice(), null, true).toString());
        }
    }
    
    //@Test
    public void testGL2OffScreenAutoDblBuf() throws InterruptedException {
        if(!checkProfile(GLProfile.GL2)) {
            return;
        }
        final GLCapabilities reqGLCaps = new GLCapabilities(GLProfile.get(GLProfile.GL2));        
        reqGLCaps.setOnscreen(false);
        doTest(reqGLCaps, new GearsES2(1));
    }

    // @Test
    public void testGL2OffScreenFBODblBuf() throws InterruptedException {
        if(!checkProfile(GLProfile.GL2)) {
            return;
        }
        final GLCapabilities reqGLCaps = new GLCapabilities(GLProfile.get(GLProfile.GL2));        
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setFBO(true);
        doTest(reqGLCaps, new GearsES2(1));
    }
    
    @Test
    public void testGL2OnScreenDblBuf() throws InterruptedException {
        if(!checkProfile(GLProfile.GL2)) {
            return;
        }
        final GLCapabilities reqGLCaps = new GLCapabilities(GLProfile.get(GLProfile.GL2));        
        doTest(reqGLCaps, new GearsES2(1));
    }
    
    @Test
    public void testGL2OffScreenPbufferDblBuf() throws InterruptedException {
        if(!checkProfile(GLProfile.GL2)) {
            return;
        }
        final GLCapabilities reqGLCaps = new GLCapabilities(GLProfile.get(GLProfile.GL2));        
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setPBuffer(true);
        doTest(reqGLCaps, new GearsES2(1));
    }
    
    @Test
    public void testGL2OffScreenBitmapDblBuf() throws InterruptedException {
        if(!checkProfile(GLProfile.GL2)) {
            return;
        }
        final GLCapabilities reqGLCaps = new GLCapabilities(GLProfile.get(GLProfile.GL2));        
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setBitmap(true);
        doTest(reqGLCaps, new GearsES2(1));
    }
    
    @Test
    public void testES2OnScreenDblBuf() throws InterruptedException {
        if(!checkProfile(GLProfile.GLES2)) {
            return;
        }
        final GLCapabilities reqGLCaps = new GLCapabilities(GLProfile.get(GLProfile.GLES2));        
        doTest(reqGLCaps, new GearsES2(1));
    }
    
    @Test
    public void testES2OffScreenPbufferDblBuf() throws InterruptedException {
        if(!checkProfile(GLProfile.GLES2)) {
            return;
        }
        final GLCapabilities reqGLCaps = new GLCapabilities(GLProfile.get(GLProfile.GLES2));        
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setPBuffer(true);
        doTest(reqGLCaps, new GearsES2(1));
    }
    
    /** Not implemented !
    @Test
    public void testES2OffScreenBitmapDblBuf() throws InterruptedException {
        if(!checkProfile(GLProfile.GLES2)) {
            return;
        }
        final GLCapabilities reqGLCaps = new GLCapabilities(GLProfile.get(GLProfile.GLES2));        
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setBitmap(true);
        doTest(reqGLCaps, new GearsES2(1));
    } */
    
    public static void main(String args[]) throws IOException {
        org.junit.runner.JUnitCore.main(TestGLCapabilities01NEWT.class.getName());
    }

}

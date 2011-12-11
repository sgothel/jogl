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
 
package com.jogamp.opengl.test.junit.jogl.drawable;

import com.jogamp.opengl.test.junit.util.UITestCase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import javax.media.opengl.*;

import com.jogamp.newt.*;
import java.io.IOException;

public class TestDrawable01NEWT extends UITestCase {
    static GLProfile glp;
    static GLDrawableFactory factory;
    static int width, height;
    GLCapabilities caps;
    Window window;
    GLDrawable drawable;
    GLContext context;

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getDefault();
        Assert.assertNotNull(glp);
        factory = GLDrawableFactory.getFactory(glp);
        Assert.assertNotNull(factory);
        width  = 640;
        height = 480;
    }

    @AfterClass
    public static void releaseClass() {
        Assert.assertNotNull(factory);
        factory=null;
    }

    @Before
    public void initTest() {
        caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
    }

    void createWindow(boolean onscreen, boolean pbuffer, boolean undecorated) {
        caps.setOnscreen(onscreen);
        caps.setPBuffer(!onscreen && pbuffer);
        caps.setDoubleBuffered(onscreen);
        // System.out.println("Requested: "+caps);

        //
        // Create native windowing resources .. X11/Win/OSX
        // 
        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);

        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);

        window = NewtFactory.createWindow(screen, caps);
        Assert.assertNotNull(window);
        window.setUndecorated(onscreen && undecorated);
        window.setSize(width, height);
        window.setVisible(true);
        // System.out.println("Created: "+window);

        //
        // Create native OpenGL resources .. XGL/WGL/CGL .. 
        // equivalent to GLAutoDrawable methods: setVisible(true)
        // 
        GLCapabilities glCaps = (GLCapabilities) window.getGraphicsConfiguration().getChosenCapabilities();
        Assert.assertNotNull(glCaps);
        Assert.assertTrue(glCaps.getGreenBits()>5);
        Assert.assertTrue(glCaps.getBlueBits()>5);
        Assert.assertTrue(glCaps.getRedBits()>5);
        Assert.assertEquals(glCaps.isOnscreen(),onscreen);
        Assert.assertTrue(onscreen || !pbuffer || glCaps.isPBuffer()); // pass if onscreen, or !pbuffer req. or have pbuffer
        Assert.assertEquals(glCaps.getDoubleBuffered(),onscreen);
        Assert.assertTrue(glCaps.getDepthBits()>4);

        drawable = factory.createGLDrawable(window);
        Assert.assertNotNull(drawable);
        // System.out.println("Pre: "+drawable);
        //
        drawable.setRealized(true);
        // Assert.assertEquals(width,drawable.getWidth());
        // Assert.assertEquals(height,drawable.getHeight());
        // Assert.assertEquals(glCaps,drawable.getChosenGLCapabilities());
        Assert.assertEquals(window,drawable.getNativeSurface());
        // System.out.println("Post: "+drawable);

        context = drawable.createContext(null);
        Assert.assertNotNull(context);
        // System.out.println(context);
        
        int res = context.makeCurrent();
        Assert.assertTrue(GLContext.CONTEXT_CURRENT_NEW==res || GLContext.CONTEXT_CURRENT==res);

        // draw something ..

        drawable.swapBuffers();
        context.release();

        // System.out.println("Final: "+window);
    }

    void destroyWindow() {
        // GLWindow.dispose(..) sequence
        Assert.assertNotNull(context);
        context.destroy();

        Assert.assertNotNull(drawable);
        drawable.setRealized(false);

        // GLWindow.destroy(..) sequence cont..
        Assert.assertNotNull(window);
        window.destroy();

        drawable = null;
        context = null;
        window = null;
    }

    @Test
    public void testOnScreenDecorated() throws InterruptedException {
        createWindow(true, false, false);
        Thread.sleep(1000); // 1000 ms
        destroyWindow();
    }

    @Test
    public void testOnScreenUndecorated() throws InterruptedException {
        createWindow(true, false, true);
        Thread.sleep(1000); // 1000 ms
        destroyWindow();
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestDrawable01NEWT.class.getName();
        org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main(new String[] {
            tstname,
            "filtertrace=true",
            "haltOnError=false",
            "haltOnFailure=false",
            "showoutput=true",
            "outputtoformatters=true",
            "logfailedtests=true",
            "logtestlistenerevents=true",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } );
    }

}

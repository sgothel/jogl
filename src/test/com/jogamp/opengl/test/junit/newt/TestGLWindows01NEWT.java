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
 
package com.jogamp.opengl.test.junit.newt;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.opengl.*;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;
import java.io.IOException;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.gears.Gears;

public class TestGLWindows01NEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;
    static long durationPerTest = 100; // ms

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton(true);
        // GLProfile.initSingleton(false);
        width  = 640;
        height = 480;
        glp = GLProfile.getDefault();
    }

    static GLWindow createWindow(Screen screen, GLCapabilities caps,
                                 int width, int height, boolean onscreen, boolean undecorated,
                                 boolean addGLEventListenerAfterVisible) 
        throws InterruptedException
    {
        Assert.assertNotNull(caps);
        caps.setOnscreen(onscreen);
        // System.out.println("Requested: "+caps);

        //
        // Create native windowing resources .. X11/Win/OSX
        // 
        GLWindow glWindow;
        if(null!=screen) {
            glWindow = GLWindow.create(screen, caps);
            Assert.assertNotNull(glWindow);
        } else {
            glWindow = GLWindow.create(caps);
            Assert.assertNotNull(glWindow);
        }

        glWindow.setUndecorated(onscreen && undecorated);
        Assert.assertEquals(false,glWindow.isVisible());
        Assert.assertEquals(false,glWindow.isNativeValid());

        GLEventListener demo = new Gears();
        setDemoFields(demo, glWindow);
        if(!addGLEventListenerAfterVisible) {
            glWindow.addGLEventListener(demo);
        }
        glWindow.addWindowListener(new TraceWindowAdapter());

        glWindow.setSize(width, height);

        Assert.assertEquals(0, glWindow.getTotalFPSFrames());
        glWindow.setVisible(true);
        Assert.assertEquals(true,glWindow.isVisible());
        Assert.assertEquals(true,glWindow.isNativeValid());
        System.out.println("Frames for initial setVisible(true): "+glWindow.getTotalFPSFrames());
        Assert.assertTrue(0 < glWindow.getTotalFPSFrames());

        //
        // Create native OpenGL resources .. XGL/WGL/CGL .. 
        // equivalent to GLAutoDrawable methods: setVisible(true)
        // 
        GLCapabilitiesImmutable caps2 = glWindow.getChosenGLCapabilities();
        Assert.assertNotNull(caps2);
        Assert.assertTrue(caps2.getGreenBits()>=5);
        Assert.assertTrue(caps2.getBlueBits()>=5);
        Assert.assertTrue(caps2.getRedBits()>=5);
        Assert.assertEquals(caps2.isOnscreen(),onscreen);

        if(addGLEventListenerAfterVisible) {
            glWindow.addGLEventListener(demo);
            glWindow.display();
        }

        return glWindow;
    }

    static void destroyWindow(GLWindow glWindow) {
        if(null!=glWindow) {
            glWindow.destroy();
            Assert.assertEquals(false,glWindow.isNativeValid());
        }
    }

    @Test
    public void testWindowNativeRecreate01aSimple() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        GLWindow window = createWindow(null, caps, width, height, 
                                       true /* onscreen */, false /* undecorated */, 
                                       false /*addGLEventListenerAfterVisible*/);

        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());
        window.destroy();
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());

        window.display();
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());

        window.setVisible(true);
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());

        window.setVisible(false);
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());

        destroyWindow(window);
    }

    @Test
    public void testWindowNativeRecreate01bSimple() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        GLWindow window = createWindow(null, caps, width, height, 
                                       true /* onscreen */, false /* undecorated */, 
                                       true /*addGLEventListenerAfterVisible*/);

        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());
        window.destroy();
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());

        window.display();
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());

        window.setVisible(true);
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());

        window.setVisible(false);
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());

        destroyWindow(window);
    }

    @Test
    public void testWindowDecor01aSimple() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        GLWindow window = createWindow(null, caps, width, height, 
                                       true /* onscreen */, false /* undecorated */, 
                                       false /*addGLEventListenerAfterVisible*/);
        System.out.println("Created: "+window);
        int state;
        for(state=0; state*100<durationPerTest; state++) {
            Thread.sleep(100);
        }
        System.out.println("duration: "+window.getTotalFPSDuration());
        destroyWindow(window);
    }

    @Test
    public void testWindowDecor01bSimple() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        GLWindow window = createWindow(null, caps, width, height, 
                                       true /* onscreen */, false /* undecorated */, 
                                       true /*addGLEventListenerAfterVisible*/);
        System.out.println("Created: "+window);
        int state;
        for(state=0; state*100<durationPerTest; state++) {
            Thread.sleep(100);
        }
        System.out.println("duration: "+window.getTotalFPSDuration());
        destroyWindow(window);
    }

    @Test
    public void testWindowDecor02DestroyWinTwiceA() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        GLWindow window = createWindow(null, caps, width, height, 
                                       true /* onscreen */, false /* undecorated */, 
                                       false /*addGLEventListenerAfterVisible*/);
        int state;
        for(state=0; state*100<durationPerTest; state++) {
            Thread.sleep(100);
        }
        System.out.println("duration: "+window.getTotalFPSDuration());
        destroyWindow(window);
    }

    @Test
    public void testWindowDecor03TwoWinOneDisplay() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);

        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);

        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        GLWindow window1 = createWindow(screen, caps, width, height,
                                       true /* onscreen */, false /* undecorated */, 
                                       false /*addGLEventListenerAfterVisible*/);
        Assert.assertNotNull(window1);

        GLWindow window2 = createWindow(screen, caps, width, height, 
                                       true /* onscreen */, false /* undecorated */, 
                                       false /*addGLEventListenerAfterVisible*/);
        Assert.assertNotNull(window2);

        Assert.assertEquals(1,Display.getActiveDisplayNumber());
        Assert.assertEquals(1,display.getReferenceCount());
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(true,display.getEDTUtil().isRunning());

        Assert.assertEquals(2,screen.getReferenceCount());
        Assert.assertEquals(true,screen.isNativeValid());

        int state;
        for(state=0; state*100<durationPerTest; state++) {
            Thread.sleep(100);
        }
        System.out.println("duration1: "+window1.getTotalFPSDuration());
        System.out.println("duration2: "+window2.getTotalFPSDuration());

        destroyWindow(window1);
        destroyWindow(window2);

        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        Assert.assertEquals(0,display.getReferenceCount());
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(false,display.getEDTUtil().isRunning());

        Assert.assertEquals(0,screen.getReferenceCount());
        Assert.assertEquals(false,screen.isNativeValid());
    }

    @Test
    public void testWindowDecor03TwoWinTwoDisplays() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);

        Display display1 = NewtFactory.createDisplay(null, false); // local display
        Assert.assertNotNull(display1);
        Display display2 = NewtFactory.createDisplay(null, false); // local display
        Assert.assertNotNull(display2);
        Assert.assertNotSame(display1, display2);

        Screen screen1  = NewtFactory.createScreen(display1, 0); // screen 0
        Assert.assertNotNull(screen1);
        GLWindow window1 = createWindow(screen1, caps, width, height, 
                                       true /* onscreen */, false /* undecorated */, 
                                       false /*addGLEventListenerAfterVisible*/);
        Assert.assertNotNull(window1);

        Screen screen2  = NewtFactory.createScreen(display2, 0); // screen 0
        Assert.assertNotNull(screen2);
        GLWindow window2 = createWindow(screen2, caps, width, height, 
                                       true /* onscreen */, false /* undecorated */, 
                                       false /*addGLEventListenerAfterVisible*/);
        Assert.assertNotNull(window2);

        Assert.assertEquals(2,Display.getActiveDisplayNumber());

        Assert.assertEquals(1,display1.getReferenceCount());
        Assert.assertEquals(true,display1.isNativeValid());
        Assert.assertNotNull(display1.getEDTUtil());
        Assert.assertEquals(true,display1.getEDTUtil().isRunning());
        Assert.assertEquals(1,screen1.getReferenceCount());
        Assert.assertEquals(true,screen1.isNativeValid());

        Assert.assertEquals(1,display2.getReferenceCount());
        Assert.assertEquals(true,display2.isNativeValid());
        Assert.assertNotNull(display2.getEDTUtil());
        Assert.assertEquals(true,display2.getEDTUtil().isRunning());
        Assert.assertEquals(1,screen2.getReferenceCount());
        Assert.assertEquals(true,screen2.isNativeValid());

        int state;
        for(state=0; state*100<durationPerTest; state++) {
            Thread.sleep(100);
        }
        System.out.println("duration1: "+window1.getTotalFPSDuration());
        System.out.println("duration2: "+window2.getTotalFPSDuration());

        // It is observed that some X11 drivers, eg ATI, fglrx 8.78.6,
        // are quite sensitive to multiple Display connections (NEWT Display -> X11 Display).
        // In such cases, closing displays shall happen in the same order as
        // opening them, otherwise some driver related bug appears.
        // You may test this, ie just reverse the destroy order below.
        // See also native test: jogl/test/native/displayMultiple02.c
        destroyWindow(window1);
        destroyWindow(window2);

        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        Assert.assertEquals(0,display1.getReferenceCount());
        Assert.assertEquals(false,display1.isNativeValid());
        Assert.assertNotNull(display1.getEDTUtil());
        Assert.assertEquals(false,display1.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen1.getReferenceCount());
        Assert.assertEquals(false,screen1.isNativeValid());

        Assert.assertEquals(0,display2.getReferenceCount());
        Assert.assertEquals(false,display2.isNativeValid());
        Assert.assertNotNull(display2.getEDTUtil());
        Assert.assertEquals(false,display2.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen2.getReferenceCount());
        Assert.assertEquals(false,screen2.isNativeValid()); 
    }

    public static void setDemoFields(GLEventListener demo, GLWindow glWindow) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        if(!MiscUtils.setFieldIfExists(demo, "window", glWindow)) {
            MiscUtils.setFieldIfExists(demo, "glWindow", glWindow);
        }
    }

    static int atoi(String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        System.out.println("durationPerTest: "+durationPerTest);
        String tstname = TestGLWindows01NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}

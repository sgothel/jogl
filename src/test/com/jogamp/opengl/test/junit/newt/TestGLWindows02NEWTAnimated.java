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

import com.jogamp.opengl.util.Animator;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;
import java.io.IOException;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.gears.Gears;

public class TestGLWindows02NEWTAnimated extends UITestCase {
    static GLProfile glp;
    static int width, height;
    static long durationPerTest = 100; // ms

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton(true);
        width  = 640;
        height = 480;
        glp = GLProfile.getDefault();
    }

    static GLWindow createWindow(Screen screen, GLCapabilities caps, int width, int height, boolean onscreen, boolean undecorated) {
        Assert.assertNotNull(caps);
        caps.setOnscreen(onscreen);
        // System.out.println("Requested: "+caps);

        //
        // Create native windowing resources .. X11/Win/OSX
        // 
        GLWindow glWindow;
        if(null!=screen) {
            Window window = NewtFactory.createWindow(screen, caps);
            Assert.assertNotNull(window);
            glWindow = GLWindow.create(window);
        } else {
            glWindow = GLWindow.create(caps);
        }
        glWindow.setUpdateFPSFrames(1, null);        
        Assert.assertNotNull(glWindow);
        glWindow.setUndecorated(onscreen && undecorated);

        GLEventListener demo = new Gears();
        setDemoFields(demo, glWindow);
        glWindow.addGLEventListener(demo);
        glWindow.addWindowListener(new TraceWindowAdapter());
        Assert.assertEquals(false,glWindow.isNativeValid());

        glWindow.setSize(width, height);
        Assert.assertEquals(false,glWindow.isVisible());
        glWindow.setVisible(true);
        Assert.assertEquals(true,glWindow.isVisible());
        Assert.assertEquals(true,glWindow.isNativeValid());
        // Assert.assertEquals(width,glWindow.getWidth());
        // Assert.assertEquals(height,glWindow.getHeight());
        // System.out.println("Created: "+glWindow);

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

        return glWindow;
    }

    static void destroyWindow(GLWindow glWindow) {
        if(null!=glWindow) {
            glWindow.destroy();
        }
    }

    @Test
    public void testWindowDecor01Simple() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        GLWindow window = createWindow(null, caps, width, height, true /* onscreen */, false /* undecorated */);
        Animator animator = new Animator(window);
        animator.setUpdateFPSFrames(1, null);        
        Assert.assertTrue(animator.start());
        while(animator.isAnimating() && animator.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        destroyWindow(window);
        Assert.assertEquals(false, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());
        Assert.assertEquals(true, animator.isStarted());
        Assert.assertTrue(animator.stop());
    }

    @Test
    public void testWindowDecor02DestroyWinTwiceA() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        GLWindow window = createWindow(null, caps, width, height, true /* onscreen */, false /* undecorated */);
        Animator animator = new Animator(window);
        animator.setUpdateFPSFrames(1, null);        
        Assert.assertTrue(animator.start());
        while(animator.isAnimating() && animator.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        destroyWindow(window);
        destroyWindow(window);
        Assert.assertEquals(false, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());
        Assert.assertEquals(true, animator.isStarted());
        Assert.assertTrue(animator.stop());
    }

    @Test
    public void testWindowDecor03TwoWinOneDisplay() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);

        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);

        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        GLWindow window1 = createWindow(screen, caps, width, height, true /* onscreen */, false /* undecorated */);
        Assert.assertNotNull(window1);
        window1.setPosition(0, 0);

        GLWindow window2 = createWindow(screen, caps, width-10, height-10, true /* onscreen */, false /* undecorated */);
        Assert.assertNotNull(window2);
        window2.setPosition(screen.getWidth()-width, 0);

        Animator animator = new Animator();
        animator.setUpdateFPSFrames(1, null);        
        Assert.assertEquals(false, animator.isStarted());
        Assert.assertEquals(false, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());

        Assert.assertTrue(animator.start());
        Assert.assertEquals(true, animator.isStarted());
        Assert.assertEquals(false, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());

        animator.add(window1);
        Assert.assertEquals(true, animator.isStarted());
        Assert.assertEquals(true, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());

        animator.add(window2);
        Assert.assertEquals(true, animator.isStarted());
        Assert.assertEquals(true, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());

        while(animator.isAnimating() && animator.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        window1.destroy();
        Assert.assertEquals(true, animator.isStarted());
        Assert.assertEquals(true, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());

        while(animator.isAnimating() && animator.getTotalFPSDuration()<2*durationPerTest) {
            Thread.sleep(100);
        }
        window2.destroy();
        Assert.assertEquals(true, animator.isStarted());
        Assert.assertEquals(false, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());
        Assert.assertTrue(animator.stop());
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
        GLWindow window1 = createWindow(screen1, caps, width, height, true /* onscreen */, false /* undecorated */);
        Assert.assertNotNull(window1);
        window1.setPosition(0, 0);

        Screen screen2  = NewtFactory.createScreen(display2, 0); // screen 0
        Assert.assertNotNull(screen2);
        GLWindow window2 = createWindow(screen2, caps, width-10, height-10, true /* onscreen */, false /* undecorated */);
        Assert.assertNotNull(window2);
        window2.setPosition(screen2.getWidth()-width, 0);

        Animator animator = new Animator();
        animator.setUpdateFPSFrames(1, null);        
        Assert.assertEquals(false, animator.isStarted());
        Assert.assertEquals(false, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());

        Assert.assertTrue(animator.start());
        Assert.assertEquals(true, animator.isStarted());
        Assert.assertEquals(false, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());

        animator.add(window1);
        Assert.assertEquals(true, animator.isStarted());
        Assert.assertEquals(true, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());

        animator.add(window2);
        Assert.assertEquals(true, animator.isStarted());
        Assert.assertEquals(true, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());

        while(animator.isAnimating() && animator.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        destroyWindow(window1);
        Assert.assertEquals(true, animator.isStarted());
        Assert.assertEquals(true, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());

        while(animator.isAnimating() && animator.getTotalFPSDuration()<2*durationPerTest) {
            Thread.sleep(100);
        }
        destroyWindow(window2);
        Assert.assertEquals(true, animator.isStarted());
        Assert.assertEquals(false, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());

        Assert.assertEquals(true, animator.pause());
        Assert.assertEquals(true, animator.isStarted());                
        Assert.assertEquals(false, animator.isAnimating());
        Assert.assertEquals(true, animator.isPaused());

        Assert.assertEquals(true, animator.resume());
        Assert.assertEquals(true, animator.isStarted());
        Assert.assertEquals(false, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());

        Assert.assertTrue(animator.stop());
        Assert.assertEquals(false, animator.isStarted());
        Assert.assertEquals(false, animator.isAnimating());
        Assert.assertEquals(false, animator.isPaused());
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
        String tstname = TestGLWindows02NEWTAnimated.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}

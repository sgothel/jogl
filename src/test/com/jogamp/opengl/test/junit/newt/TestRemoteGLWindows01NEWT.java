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
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.opengl.*;

import com.jogamp.opengl.util.Animator;

import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;
import java.io.IOException;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es1.GearsES1;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindowException;

public class TestRemoteGLWindows01NEWT extends UITestCase {
    static int width = 640, height = 480;
    static long durationPerTest = 100; // ms
    static String remoteDisplay = "localhost:0.0";

    static GLWindow createWindow(Screen screen, GLCapabilities caps, GLEventListener demo)
        throws InterruptedException
    {
        Assert.assertNotNull(caps);
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
        
        glWindow.addGLEventListener(demo);

        glWindow.setSize(512, 512);
        glWindow.setVisible(true);
        Assert.assertEquals(true,glWindow.isVisible());
        Assert.assertEquals(true,glWindow.isNativeValid());

        return glWindow;
    }

    static void destroyWindow(GLWindow glWindow) {
        if(null!=glWindow) {
            glWindow.destroy();
            Assert.assertEquals(false,glWindow.isNativeValid());
        }
    }

    @Test
    public void testRemoteWindow01() throws InterruptedException {
        Animator animator = new Animator();
        GLProfile glpLocal = GLProfile.getGL2ES1();
        Assert.assertNotNull(glpLocal);
        GLCapabilities capsLocal = new GLCapabilities(glpLocal);
        Assert.assertNotNull(capsLocal);
        GearsES1 demoLocal = new GearsES1(1);
        GLWindow windowLocal = createWindow(null, capsLocal, demoLocal); // local with vsync
        Assert.assertEquals(true,windowLocal.isNativeValid());
        Assert.assertEquals(true,windowLocal.isVisible());
        AbstractGraphicsDevice device1 = windowLocal.getScreen().getDisplay().getGraphicsDevice();

        System.err.println("GLProfiles window1: "+device1.getConnection()+": "+GLProfile.glAvailabilityToString(device1));

        animator.add(windowLocal);

        // Remote Display/Device/Screen/Window ..
        // Eager initialization of NEWT Display -> AbstractGraphicsDevice -> GLProfile (device)
        Display displayRemote; // remote display
        AbstractGraphicsDevice deviceRemote;
        Screen screenRemote;
        GLWindow windowRemote;
        GearsES1 demoRemote = null;
        try {
            displayRemote = NewtFactory.createDisplay(remoteDisplay); // remote display
            displayRemote.createNative();
            System.err.println(displayRemote);
            deviceRemote = displayRemote.getGraphicsDevice();
            System.err.println(deviceRemote);
            GLProfile.initProfiles(deviceRemote); // just to make sure
            System.err.println();
            System.err.println("GLProfiles window2: "+deviceRemote.getConnection()+": "+GLProfile.glAvailabilityToString(deviceRemote));
            GLProfile glpRemote = GLProfile.get(deviceRemote, GLProfile.GL2ES1);
            Assert.assertNotNull(glpRemote);
            GLCapabilities capsRemote = new GLCapabilities(glpRemote);
            Assert.assertNotNull(capsRemote);
            screenRemote  = NewtFactory.createScreen(displayRemote, 0); // screen 0
            demoRemote = new GearsES1(0);
            windowRemote = createWindow(screenRemote, capsRemote, demoRemote); // remote, no vsync
        } catch (NativeWindowException nwe) {
            System.err.println(nwe);
            Assume.assumeNoException(nwe);
            destroyWindow(windowLocal);
            return;
        }

        Assert.assertEquals(true,windowRemote.isNativeValid());
        Assert.assertEquals(true,windowRemote.isVisible());

        animator.add(windowRemote);
        animator.setUpdateFPSFrames(1, null);        
        animator.start();

        while(animator.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        if(null!=demoRemote) {
            System.err.println("demoLocal VBO: "+demoLocal.getGear1().backFace.isVBO());
            System.err.println("demoRemote VBO: "+demoRemote.getGear1().backFace.isVBO());
        }

        destroyWindow(windowLocal);
        destroyWindow(windowRemote);
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
            } else if(args[i].equals("-display")) {
                remoteDisplay = args[++i];
            }
        }
        System.out.println("durationPerTest: "+durationPerTest);
        System.out.println("display: "+remoteDisplay);
        String tstname = TestRemoteGLWindows01NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}

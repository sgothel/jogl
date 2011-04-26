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
import com.jogamp.opengl.test.junit.jogl.demos.gl2.gears.Gears;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindowException;

public class TestRemoteGLWindows01NEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;
    static long durationPerTest = 100; // ms
    static String remoteDisplay = "nowhere:0.0";

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton(true);
        // GLProfile.initSingleton(false);
        width  = 640;
        height = 480;
        glp = GLProfile.getDefault();
    }

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
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        GLWindow window1 = createWindow(null, caps, new Gears(1)); // local with vsync
        Assert.assertEquals(true,window1.isNativeValid());
        Assert.assertEquals(true,window1.isVisible());
        AbstractGraphicsDevice device1 = window1.getScreen().getDisplay().getGraphicsDevice();

        System.err.println("GLProfiles window1: "+device1.getConnection()+": "+GLProfile.glAvailabilityToString(device1));

        animator.add(window1);

        // Remote Display/Device/Screen/Window ..
        // Eager initialization of NEWT Display -> AbstractGraphicsDevice -> GLProfile (device)
        Display display2; // remote display
        AbstractGraphicsDevice device2;
        Screen screen2;
        GLWindow window2;
        try {
            display2 = NewtFactory.createDisplay(remoteDisplay); // remote display
            display2.createNative();
            System.err.println(display2);
            device2 = display2.getGraphicsDevice();
            System.err.println(device2);
            GLProfile.initProfiles(device2); // just to make sure
            System.err.println("");
            System.err.println("GLProfiles window2: "+device2.getConnection()+": "+GLProfile.glAvailabilityToString(device2));
            screen2  = NewtFactory.createScreen(display2, 0); // screen 0
            window2 = createWindow(screen2, caps, new Gears(0)); // remote, no vsync
        } catch (NativeWindowException nwe) {
            System.err.println(nwe);
            Assume.assumeNoException(nwe);
            destroyWindow(window1);
            return;
        }

        Assert.assertEquals(true,window2.isNativeValid());
        Assert.assertEquals(true,window2.isVisible());

        animator.add(window2);
        animator.setUpdateFPSFrames(1, null);        
        animator.start();

        while(animator.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }

        destroyWindow(window1);
        destroyWindow(window2);
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

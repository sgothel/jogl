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

import javax.media.nativewindow.*;

import com.jogamp.newt.*;
import java.io.IOException;

import com.jogamp.opengl.test.junit.util.UITestCase;

public class TestRemoteWindow01NEWT extends UITestCase {
    static int width, height;
    static String remoteDisplay = "localhost:0.0";

    @BeforeClass
    public static void initClass() {
        NativeWindowFactory.initSingleton(true);
        width  = 640;
        height = 480;
    }

    static Window createWindow(Screen screen, Capabilities caps, int width, int height, boolean onscreen, boolean undecorated) {
        Assert.assertNotNull(caps);
        caps.setOnscreen(onscreen);
        // System.out.println("Requested: "+caps);

        //
        // Create native windowing resources .. X11/Win/OSX
        // 
        Window window = NewtFactory.createWindow(screen, caps);
        Assert.assertNotNull(window);
        window.setUndecorated(onscreen && undecorated);
        window.setSize(width, height);
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());
        window.setVisible(true);
        Assert.assertEquals(true,window.isVisible());
        Assert.assertEquals(true,window.isNativeValid());
        // Assert.assertEquals(width,window.getWidth());
        // Assert.assertEquals(height,window.getHeight());
        // System.out.println("Created: "+window);

        //
        // Create native OpenGL resources .. XGL/WGL/CGL .. 
        // equivalent to GLAutoDrawable methods: setVisible(true)
        // 
        CapabilitiesImmutable chosenCapabilities = window.getGraphicsConfiguration().getChosenCapabilities();
        Assert.assertNotNull(chosenCapabilities);
        Assert.assertTrue(chosenCapabilities.getGreenBits()>5);
        Assert.assertTrue(chosenCapabilities.getBlueBits()>5);
        Assert.assertTrue(chosenCapabilities.getRedBits()>5);
        Assert.assertEquals(chosenCapabilities.isOnscreen(),onscreen);

        return window;
    }

    static void destroyWindow(Display display, Screen screen, Window window) {
        if(null!=window) {
            window.destroy();
        }
        if(null!=screen) {
            screen.destroy();
        }
        if(null!=display) {
            display.destroy();
        }
    }

    @Test
    public void testRemoteWindow01() throws InterruptedException {
        Capabilities caps = new Capabilities();
        Display display1 = NewtFactory.createDisplay(null); // local display
        Screen screen1  = NewtFactory.createScreen(display1, 0); // screen 0
        Window window1 = createWindow(screen1, caps, width, height, true /* onscreen */, false /* undecorated */);
        window1.setVisible(true);

        Assert.assertEquals(true,window1.isNativeValid());
        Assert.assertEquals(true,window1.isVisible());

        // Remote Display/Device/Screen/Window ..
        Display display2;
        AbstractGraphicsDevice device2;
        Screen screen2;
        Window window2;
        try {
            display2 = NewtFactory.createDisplay(remoteDisplay);
            display2.createNative(); 
            screen2  = NewtFactory.createScreen(display2, 0); // screen 0
            window2 = createWindow(screen2, caps, width, height, true /* onscreen */, false /* undecorated */);
            window2.setVisible(true);
        } catch (NativeWindowException nwe) {
            System.err.println(nwe);
            Assume.assumeNoException(nwe);
            destroyWindow(display1, screen1, window1);
            return;
        }

        Assert.assertEquals(true,window2.isNativeValid());
        Assert.assertEquals(true,window2.isVisible());

        Thread.sleep(500); // 500 ms

        destroyWindow(display1, screen1, window1);
        destroyWindow(display2, screen2, window2);
    }

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-display")) {
                remoteDisplay = args[++i];
            }
        }
        System.out.println("display: "+remoteDisplay);
        String tstname = TestRemoteWindow01NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}

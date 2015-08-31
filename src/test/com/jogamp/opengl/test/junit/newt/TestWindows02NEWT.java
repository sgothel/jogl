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
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.nativewindow.*;

import com.jogamp.newt.*;
import java.io.IOException;

import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestWindows02NEWT extends UITestCase {
    static int width, height;
    static long durationPerTest = 100; // ms

    @BeforeClass
    public static void initClass() {
        NativeWindowFactory.initSingleton();
        width  = 800;
        height = 600;
    }

    static Window createWindow(final Capabilities caps, final int x, final int y, final int width, final int height, final boolean onscreen, final boolean undecorated) throws InterruptedException {
        final boolean userPos = x>=0 && y>=0 ; // user has specified a position

        Assert.assertNotNull(caps);
        caps.setOnscreen(onscreen);
        // System.out.println("Requested: "+caps);

        //
        // Create native windowing resources .. X11/Win/OSX
        //
        final Window window = NewtFactory.createWindow(caps);
        Assert.assertNotNull(window);
        final Screen screen = window.getScreen();
        final Display display = screen.getDisplay();
        window.setUndecorated(onscreen && undecorated);
        if(userPos) {
            window.setPosition(x, y);
        }
        window.setSize(width, height);
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());
        window.setVisible(true);
        // System.err.println("************* Created: "+window);

        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,window.isVisible());
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(width, window.getWidth());
        Assert.assertEquals(height, window.getHeight());

        final CapabilitiesImmutable chosenCapabilities = window.getGraphicsConfiguration().getChosenCapabilities();
        Assert.assertNotNull(chosenCapabilities);
        Assert.assertTrue(chosenCapabilities.getGreenBits()>=5);
        Assert.assertTrue(chosenCapabilities.getBlueBits()>=5);
        Assert.assertTrue(chosenCapabilities.getRedBits()>=5);
        Assert.assertEquals(chosenCapabilities.isOnscreen(),onscreen);

        return window;
    }

    static void destroyWindow(final Window window, final boolean last) {
        if(null==window) {
            return;
        }
        final Screen screen = window.getScreen();
        final Display display = screen.getDisplay();
        window.destroy();
        // System.err.println("************* Destroyed: "+window);
        if(last) {
            Assert.assertEquals(false,screen.isNativeValid());
            Assert.assertEquals(false,display.isNativeValid());
        } else {
            Assert.assertEquals(true,screen.isNativeValid());
            Assert.assertEquals(true,display.isNativeValid());
        }
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());
    }


    @Test
    public void test01WindowDefault() throws InterruptedException {
        final Capabilities caps = new Capabilities();
        Assert.assertNotNull(caps);

        final Window window = createWindow(caps, -1, -1, width, height, true /* onscreen */, false /* undecorated */);
        final CapabilitiesImmutable chosenCapabilities = window.getGraphicsConfiguration().getChosenCapabilities();
        System.err.println("XXX: "+chosenCapabilities);
        for(int state=0; state*100<durationPerTest; state++) {
            Thread.sleep(100);
        }
        destroyWindow(window, true);
    }

    @Test
    public void test02WindowDefault() throws InterruptedException {
        final Capabilities caps = new Capabilities();
        Assert.assertNotNull(caps);
        caps.setBackgroundOpaque(false);

        final Window window = createWindow(caps, -1, -1, width, height, true /* onscreen */, false /* undecorated */);
        final CapabilitiesImmutable chosenCapabilities = window.getGraphicsConfiguration().getChosenCapabilities();
        System.err.println("XXX: "+chosenCapabilities);
        for(int state=0; state*100<durationPerTest; state++) {
            Thread.sleep(100);
        }
        destroyWindow(window, true);
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atol(args[++i], durationPerTest);
            }
        }
        System.out.println("durationPerTest: "+durationPerTest);
        final String tstname = TestWindows02NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}

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

import javax.media.nativewindow.*;
import javax.media.nativewindow.util.Point;

import com.jogamp.newt.*;
import java.io.IOException;

import com.jogamp.opengl.test.junit.util.UITestCase;

public class TestWindows01NEWT extends UITestCase {
    static int width, height;

    @BeforeClass
    public static void initClass() {
        NativeWindowFactory.initSingleton(true);
        width  = 256;
        height = 256;
    }

    static Window createWindow(Capabilities caps, int x, int y, int width, int height, boolean onscreen, boolean undecorated) throws InterruptedException {
        final boolean userPos = x>=0 && y>=0 ; // user has specified a position
        
        Assert.assertNotNull(caps);
        caps.setOnscreen(onscreen);
        // System.out.println("Requested: "+caps);

        //
        // Create native windowing resources .. X11/Win/OSX
        // 
        Window window = NewtFactory.createWindow(caps);
        Assert.assertNotNull(window);
        Screen screen = window.getScreen();
        Display display = screen.getDisplay();
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

        /** we don't sync on position - unreliable test
        Point p0  = window.getLocationOnScreen(null);
        Assert.assertEquals(p0.getX(), window.getX());
        Assert.assertEquals(p0.getY(), window.getY());
        if(userPos) {
            Assert.assertEquals(x, window.getX());
            Assert.assertEquals(y, window.getY());
        } */

        CapabilitiesImmutable chosenCapabilities = window.getGraphicsConfiguration().getChosenCapabilities();
        Assert.assertNotNull(chosenCapabilities);
        Assert.assertTrue(chosenCapabilities.getGreenBits()>=5);
        Assert.assertTrue(chosenCapabilities.getBlueBits()>=5);
        Assert.assertTrue(chosenCapabilities.getRedBits()>=5);
        Assert.assertEquals(chosenCapabilities.isOnscreen(),onscreen);
        
        return window;
    }

    static void destroyWindow(Window window, boolean last) {
        if(null==window) {
            return;
        }
        Screen screen = window.getScreen();
        Display display = screen.getDisplay();
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
    public void testWindowDecorSimpleWMPos() throws InterruptedException {
        Capabilities caps = new Capabilities();
        Assert.assertNotNull(caps);

        Window window = createWindow(caps, -1, -1, width, height, true /* onscreen */, false /* undecorated */);
        destroyWindow(window, true);
    }


    @Test
    public void testWindowDecorSimpleUserPos() throws InterruptedException {
        Capabilities caps = new Capabilities();
        Assert.assertNotNull(caps);

        Window window = createWindow(caps, 100, 100, width, height, true /* onscreen */, false /* undecorated */);
        destroyWindow(window, true);
    }

    @Test
    public void testWindowNativeRecreate01Simple() throws InterruptedException {
        Capabilities caps = new Capabilities();
        Assert.assertNotNull(caps);

        Window window = createWindow(caps, -1, -1, width, height, true /* onscreen */, false /* undecorated */);
        destroyWindow(window, true);
        
        window.setVisible(true);
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());
        Assert.assertEquals(width, window.getWidth());
        Assert.assertEquals(height, window.getHeight());

        destroyWindow(window, true);
    }
    
    @Test
    public void testWindowDecorDestroyWinTwiceA() throws InterruptedException {
        Capabilities caps = new Capabilities();
        Assert.assertNotNull(caps);

        Window window = createWindow(caps, -1, -1, width, height, true /* onscreen */, false /* undecorated */);
        destroyWindow(window, true);
        destroyWindow(window, true);
    }

    @Test
    public void testWindowDecorTwoWin() throws InterruptedException {
        Capabilities caps = new Capabilities();
        Assert.assertNotNull(caps);

        Window window1 = createWindow(caps, -1, -1, width, height, true /* onscreen */, false /* undecorated */);
        Window window2 = createWindow(caps, 100, 100, width, height, true /* onscreen */, false /* undecorated */);
        destroyWindow(window2, false);
        destroyWindow(window1, true);
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestWindows01NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}

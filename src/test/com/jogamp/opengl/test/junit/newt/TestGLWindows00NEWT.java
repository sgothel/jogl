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

import com.jogamp.opengl.*;

import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;
import java.io.IOException;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import com.jogamp.nativewindow.AbstractGraphicsDevice;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLWindows00NEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;
    static boolean manual = false;
    static int loopVisibleToggle = 10;
    static long durationPerTest = 100; // ms

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
        glp = GLProfile.getDefault();
    }

    static GLWindow createWindow(final Screen screen, final GLCapabilitiesImmutable caps, final boolean undecor)
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
        glWindow.setUpdateFPSFrames(1, null);

        final GLEventListener demo = new GearsES2();
        glWindow.addGLEventListener(demo);

        glWindow.setUndecorated(undecor);
        glWindow.setSize(512, 512);
        System.err.println("XXX VISIBLE.0 -> TRUE");
        glWindow.setVisible(true);
        Assert.assertEquals(true,glWindow.isVisible());
        Assert.assertEquals(true,glWindow.isNativeValid());

        return glWindow;
    }

    static void destroyWindow(final GLWindow glWindow) {
        if(null!=glWindow) {
            glWindow.destroy();
            Assert.assertEquals(false,glWindow.isNativeValid());
            Assert.assertEquals(false,glWindow.isVisible());
        }
    }

    @Test
    public void test01WindowCreateSimple() throws InterruptedException {
        if( manual ) {
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        final GLWindow window = createWindow(null, caps, false /* undecor */); // local
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());

        final AbstractGraphicsDevice device1 = window.getScreen().getDisplay().getGraphicsDevice();
        System.err.println("GLProfiles window1: "+device1.getConnection()+": "+GLProfile.glAvailabilityToString(device1));

        for(int state=0; state*100<durationPerTest; state++) {
            Thread.sleep(100);
        }

        destroyWindow(window);
    }

    @Test
    public void test02WindowCreateUndecor() throws InterruptedException {
        if( manual ) {
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        final GLWindow window = createWindow(null, caps, true /* undecor */); // local
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());

        final AbstractGraphicsDevice device1 = window.getScreen().getDisplay().getGraphicsDevice();
        System.err.println("GLProfiles window1: "+device1.getConnection()+": "+GLProfile.glAvailabilityToString(device1));

        for(int state=0; state*100<durationPerTest; state++) {
            Thread.sleep(100);
        }

        destroyWindow(window);
    }

    @Test
    public void test11WindowSimpleToggleVisibility() throws InterruptedException {
        test1xWindowToggleVisibility(false /* undecor */, loopVisibleToggle);
    }
    @Test
    public void test12WindowUndecorToggleVisibility() throws InterruptedException {
        if( manual ) {
            return;
        }
        test1xWindowToggleVisibility(true /* undecor */, loopVisibleToggle);
    }
    private void test1xWindowToggleVisibility(final boolean undecor, final int loopVisibleToggle) throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        final GLWindow window = createWindow(null, caps, undecor); // local
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());

        window.display();
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());
        for(int state=0; state*100<durationPerTest; state++) {
            Thread.sleep(100);
        }

        for(int i=1; i<=loopVisibleToggle; i++) {
            System.err.println("XXX VISIBLE."+i+" -> FALSE");
            window.setVisible(false);
            Assert.assertEquals(true,window.isNativeValid());
            Assert.assertEquals(false,window.isVisible());
            for(int state=0; state*100<durationPerTest; state++) {
                Thread.sleep(100);
            }

            window.display();
            Assert.assertEquals(true,window.isNativeValid());
            Assert.assertEquals(false,window.isVisible());

            System.err.println("XXX VISIBLE."+i+" -> TRUE");
            window.setVisible(true);
            Assert.assertEquals(true,window.isNativeValid());
            Assert.assertEquals(true,window.isVisible());
            for(int state=0; state*100<durationPerTest; state++) {
                Thread.sleep(100);
            }
            window.display();
            Assert.assertEquals(true,window.isNativeValid());
            Assert.assertEquals(true,window.isVisible());
        }

        destroyWindow(window);
    }

    static int atoi(final String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            } else if(args[i].equals("-loopvt")) {
                loopVisibleToggle = atoi(args[++i]);
            } else if(args[i].equals("-manual")) {
                manual = true;
            }
        }
        System.out.println("durationPerTest: "+durationPerTest);
        final String tstname = TestGLWindows00NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}

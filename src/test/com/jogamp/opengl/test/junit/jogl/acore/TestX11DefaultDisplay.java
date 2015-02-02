/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es1.RedSquareES1;
import com.jogamp.opengl.test.junit.util.DumpGLInfo;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

/**
 * This is a clone of TestGLPointsNEWT which uses the ability to specify
 * the X11 default display programmatically instead of relying on the
 * DISPLAY environment variable.
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestX11DefaultDisplay extends UITestCase {
    static long duration = 500; // ms
    static int width = 512, height = 512;
    static String x11DefaultDisplay = ":0.0";

    @BeforeClass
    public static void initClass() {
        System.setProperty("nativewindow.x11.display.default", x11DefaultDisplay);
    }

    protected void runTestGL(final GLCapabilities caps) throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle(getSimpleTestName("."));
        glWindow.setSize(width, height);

        final RedSquareES1 demo = new RedSquareES1();
        glWindow.addGLEventListener(demo);

        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        snap.setPostSNDetail(demo.getClass().getSimpleName());
        glWindow.addGLEventListener(snap);

        final Animator animator = new Animator(glWindow);
        final QuitAdapter quitAdapter = new QuitAdapter();

        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);

        animator.start();

        glWindow.setVisible(true);

        System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
        System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
        System.err.println("window pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());

        animator.setUpdateFPSFrames(60, System.err);
        snap.setMakeSnapshot();

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        animator.stop();
        glWindow.destroy();
    }

    @Test
    public void test00_DefaultDevice() {
        final AbstractGraphicsDevice defaultDevice = GLProfile.getDefaultDevice();
        System.out.println("GLProfile "+GLProfile.glAvailabilityToString());
        System.out.println("GLProfile.getDefaultDevice(): "+defaultDevice);
        final GLProfile glp = GLProfile.getDefault();
        System.out.println("GLProfile.getDefault(): "+glp);

        final GLCapabilities caps = new GLCapabilities(glp);
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);

        glWindow.addGLEventListener(new DumpGLInfo());

        glWindow.setSize(128, 128);
        glWindow.setVisible(true);

        glWindow.display();
        glWindow.destroy();

        if( NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(true) ) {
            Assert.assertEquals("X11 Default device does not match", defaultDevice.getConnection(), x11DefaultDisplay);
        }
    }

    @Test
    public void test01_GLDefaultRendering() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(null);
        runTestGL(caps);
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-x11DefaultDisplay")) {
                x11DefaultDisplay = args[++i];
            }
        }
        org.junit.runner.JUnitCore.main(TestX11DefaultDisplay.class.getName());
    }
}

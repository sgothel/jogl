/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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


import java.io.IOException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLWindows03NEWTAnimResize extends UITestCase {
    static GLProfile glp;
    static int step = 4;
    static int width, height;
    static long durationPerTest = step*500; // ms

    @BeforeClass
    public static void initClass() {
        width  = 800;
        height = 600;
        glp = GLProfile.getDefault();
    }

    static void test(final GLCapabilitiesImmutable caps, final boolean undecorated) throws InterruptedException {
        Assert.assertNotNull(caps);

        //
        // Create native windowing resources .. X11/Win/OSX
        //
        final GLWindow glWindow = GLWindow.create(caps);

        glWindow.setUpdateFPSFrames(1, null);
        Assert.assertNotNull(glWindow);
        glWindow.setUndecorated(undecorated);

        final GLEventListener demo = new GearsES2(1);
        glWindow.addGLEventListener(demo);
        glWindow.addWindowListener(new TraceWindowAdapter());
        Assert.assertEquals(false,glWindow.isNativeValid());

        glWindow.setPosition(100, 100);
        glWindow.setSize(width/step, height/step);
        Assert.assertEquals(false,glWindow.isVisible());
        glWindow.setVisible(true);
        Assert.assertEquals(true,glWindow.isVisible());
        Assert.assertEquals(true,glWindow.isNativeValid());

        final Animator animator = new Animator(glWindow);
        animator.setUpdateFPSFrames(1, null);
        Assert.assertTrue(animator.start());

        int step_i = 0;
        for(int i=0; i<durationPerTest; i+=50) {
            Thread.sleep(50);
            final int j = (int) ( i / (durationPerTest/step) ) + 1;
            if(j>step_i) {
                final int w = width/step * j;
                final int h = height/step * j;
                System.err.println("resize: "+step_i+" -> "+j+" - "+w+"x"+h);
                glWindow.setSize(w, h);
                step_i = j;
            }
        }
        Thread.sleep(50);

        animator.stop();
        glWindow.destroy();
        Assert.assertEquals(false, glWindow.isNativeValid());
        Assert.assertEquals(false, glWindow.isVisible());
    }

    @Test
    public void test01WindowDecor() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        test(caps, false /* undecorated */);
    }

    @Test
    public void test02WindowUndecor() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        test(caps, true /* undecorated */);
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atol(args[++i], durationPerTest);
            }
        }
        final String tstname = TestGLWindows03NEWTAnimResize.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}

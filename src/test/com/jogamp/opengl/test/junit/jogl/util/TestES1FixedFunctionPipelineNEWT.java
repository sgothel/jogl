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

package com.jogamp.opengl.test.junit.jogl.util;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;

import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.jogl.demos.es1.GearsES1;
import com.jogamp.opengl.test.junit.jogl.demos.es1.RedSquareES1;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestES1FixedFunctionPipelineNEWT extends UITestCase {
    static int width, height;

    @BeforeClass
    public static void initClass() {
        width  = 512;
        height = 512;
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL0(final GLCapabilities caps, final GLEventListener demo) throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle(getSimpleTestName("."));

        glWindow.addGLEventListener(demo);
        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        snap.setPostSNDetail(demo.getClass().getSimpleName());
        glWindow.addGLEventListener(snap);

        final Animator animator = new Animator(glWindow);
        final QuitAdapter quitAdapter = new QuitAdapter();
        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);

        glWindow.setSize(width, height);
        glWindow.setVisible(true);
        animator.setUpdateFPSFrames(1, null);
        animator.start();

        snap.setMakeSnapshot();
        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }
        glWindow.removeGLEventListener(demo);

        animator.stop();
        glWindow.destroy();
    }

    protected void runTestGL(final GLCapabilities caps, final boolean forceFFPEmu) throws InterruptedException {
        final RedSquareES1 demo01 = new RedSquareES1();
        demo01.setForceFFPEmu(forceFFPEmu, false, false, false);
        runTestGL0(caps, demo01);

        final GearsES1 demo02 = new GearsES1();
        demo02.setForceFFPEmu(forceFFPEmu, false, false, false);
        runTestGL0(caps, demo02);

        final DemoGL2ES1ImmModeSink demo03 = new DemoGL2ES1ImmModeSink(true);
        demo03.setForceFFPEmu(forceFFPEmu, false, false, false);
        runTestGL0(caps, demo03);

        final DemoGL2ES1TextureImmModeSink demo04 = new DemoGL2ES1TextureImmModeSink();
        demo04.setForceFFPEmu(forceFFPEmu, false, false, false);
        runTestGL0(caps, demo04);
    }

    @Test
    public void test01GL2Normal() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL2)) { System.err.println("GL2 n/a"); return; }
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        runTestGL(caps, false);
    }

    @Test
    public void test02GL2FFPEmu() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL2)) { System.err.println("GL2 n/a"); return; }
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        runTestGL(caps, true);
    }

    @Test
    public void test03GL2ES1Normal() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL2ES1)) { System.err.println("GL2ES1 n/a"); return; }
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES1));
        runTestGL(caps, false);
    }

    @Test
    public void test04ES2FFPEmu() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GLES2)) { System.err.println("GLES2 n/a"); return; }
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GLES2));
        runTestGL(caps, false); // should be FFPEmu implicit
    }

    static long duration = 1000; // ms

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            }
        }
        org.junit.runner.JUnitCore.main(TestES1FixedFunctionPipelineNEWT.class.getName());
    }
}

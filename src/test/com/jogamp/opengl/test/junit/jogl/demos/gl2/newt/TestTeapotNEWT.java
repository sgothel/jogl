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

package com.jogamp.opengl.test.junit.jogl.demos.gl2.newt;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;

import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.jogl.demos.gl2.Teapot;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTeapotNEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;

    @BeforeClass
    public static void initClass() {
        if(GLProfile.isAvailable(GLProfile.GL2)) {
            glp = GLProfile.get(GLProfile.GL2);
            Assert.assertNotNull(glp);
            width  = 640;
            height = 480;
        } else {
            setTestSupported(false);
        }
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(final GLCapabilities caps, final boolean withAnimator) throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(caps);

        glWindow.setTitle("Teapot NEWT Test");
        final Teapot demo = new Teapot();
        if( !withAnimator ) {
            demo.rotIncr *= 10f;
        }
        glWindow.addGLEventListener(demo);
        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        glWindow.addGLEventListener(snap);

        final Animator animator = withAnimator ? new Animator(glWindow) : null;
        final QuitAdapter quitAdapter = new QuitAdapter();

        //glWindow.addKeyListener(new TraceKeyAdapter(quitAdapter));
        //glWindow.addWindowListener(new TraceWindowAdapter(quitAdapter));
        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);

        glWindow.setSize(width, height);
        glWindow.setVisible(true);
        if( withAnimator ) {
            animator.setUpdateFPSFrames(60, System.err);
            animator.start();
        }

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        int snaps=3;
        while(!quitAdapter.shouldQuit() && t1-t0<duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
            if( snaps-- > 0 ) {
                snap.setMakeSnapshot();
            }
            if( !withAnimator ) {
                glWindow.display();
            }
        }

        if( withAnimator ) {
            animator.stop();
        }
        glWindow.destroy();
    }

    @Test
    public void test01_DefCaps_Anim() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(GLProfile.getMaxFixedFunc(true));
        runTestGL(caps, true);
    }

    @Test
    public void test02_DefCaps_NoAnim() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(GLProfile.getMaxFixedFunc(true));
        runTestGL(caps, false);
    }

    @Test
    public void test12_FBOCaps_NoAnim() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(GLProfile.getMaxFixedFunc(true));
        caps.setHardwareAccelerated(true);
        caps.setDoubleBuffered(true);
        caps.setAlphaBits(8);
        caps.setDepthBits(8);
        caps.setNumSamples(0);
        caps.setSampleBuffers(false);
        caps.setStencilBits(0);
        caps.setRedBits(8);
        caps.setBlueBits(8);
        caps.setGreenBits(8);

        // caps.setPBuffer(true);
        caps.setFBO(true);

        runTestGL(caps, false);
    }

    static long duration = 500; // ms

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            }
        }
        org.junit.runner.JUnitCore.main(TestTeapotNEWT.class.getName());
    }
}

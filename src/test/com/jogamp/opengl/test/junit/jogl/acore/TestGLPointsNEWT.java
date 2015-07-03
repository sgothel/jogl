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

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.UITestCase;

import com.jogamp.opengl.test.junit.jogl.demos.PointsDemo;
import com.jogamp.opengl.test.junit.jogl.demos.es1.PointsDemoES1;
import com.jogamp.opengl.test.junit.jogl.demos.es2.PointsDemoES2;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLPointsNEWT extends UITestCase {
    static int width, height;

    @BeforeClass
    public static void initClass() {
        width  = 512;
        height = 512;
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL0(final GLCapabilities caps, final PointsDemo demo) throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle(getSimpleTestName("."));

        glWindow.addGLEventListener(demo);
        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        snap.setPostSNDetail(demo.getClass().getSimpleName());
        glWindow.addGLEventListener(snap);

        glWindow.setSize(width, height);
        glWindow.setVisible(true);

        demo.setSmoothPoints(false);
        snap.setMakeSnapshot();
        snap.setPostSNDetail("flat");
        glWindow.display();

        demo.setSmoothPoints(true);
        snap.setMakeSnapshot();
        snap.setPostSNDetail("smooth");
        glWindow.display();

        demo.setPointParams(2f, 40f, 0.01f, 0.0f, 0.01f, 1f);
        snap.setMakeSnapshot();
        snap.setPostSNDetail("attn0");
        glWindow.display();

        glWindow.removeGLEventListener(demo);

        glWindow.destroy();
    }

    protected void runTestGL(final GLCapabilities caps, final PointsDemo demo, final boolean forceFFPEmu) throws InterruptedException {
        // final PointsDemoES2 demo01 = new PointsDemoES2();
        runTestGL0(caps, demo);
    }

    @Test
    public void test01FFP__GL2() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL2)) { System.err.println("GL2 n/a"); return; }
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        runTestGL(caps, new PointsDemoES1(), false);
    }

    @Test
    public void test02FFP__ES1() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GLES1)) { System.err.println("GLES1 n/a"); return; }
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GLES1));
        runTestGL(caps, new PointsDemoES1(), false);
    }

    @Test
    public void test03FFP__ES2() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GLES2)) { System.err.println("GLES2 n/a"); return; }
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GLES2));
        final PointsDemoES1 demo = new PointsDemoES1();
        demo.setForceFFPEmu(true, false, false, false);
        runTestGL(caps, demo, false);
    }

    @Test
    public void test04FFP__GL2ES2() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL2ES2)) { System.err.println("GL2ES2 n/a"); return; }
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2));
        final PointsDemoES1 demo = new PointsDemoES1();
        demo.setForceFFPEmu(true, false, false, false);
        runTestGL(caps, demo, false);
    }

    @Test
    public void test11GLSL_GL2() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL2)) { System.err.println("GL2 n/a"); return; }
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        runTestGL(caps, new PointsDemoES2(), false);
    }

    @Test
    public void test12GLSL_ES2() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GLES2)) { System.err.println("GLES2 n/a"); return; }
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GLES2));
        runTestGL(caps, new PointsDemoES2(), false); // should be FFPEmu implicit
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
        org.junit.runner.JUnitCore.main(TestGLPointsNEWT.class.getName());
    }
}

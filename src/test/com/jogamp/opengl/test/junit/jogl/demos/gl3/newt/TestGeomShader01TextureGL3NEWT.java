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
package com.jogamp.opengl.test.junit.jogl.demos.gl3.newt;

import java.io.IOException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.gl3.GeomShader01TextureGL3;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

/**
 * Test Geometry shader demo GeomShader01TextureGL3
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGeomShader01TextureGL3NEWT extends UITestCase {
    static long duration = 500; // ms

    static GLCapabilities getCaps(final String profile) {
        if( !GLProfile.isAvailable(profile) )  {
            System.err.println("Profile "+profile+" n/a");
            return null;
        }
        return new GLCapabilities(GLProfile.get(profile));
    }

    @Test
    public void test01_GL3Core_Passthrough() throws InterruptedException {
        final GLCapabilities caps = getCaps(GLProfile.GL3);
        if( null == caps ) { return; }
        testImpl(caps, 0);
    }

    @Test
    public void test02_GL3Core_FlipXYZ() throws InterruptedException {
        final GLCapabilities caps = getCaps(GLProfile.GL3);
        if( null == caps ) { return; }
        testImpl(caps, 1);
    }

    @Test
    public void test11_GL3Compat_Passthrough() throws InterruptedException {
        final GLCapabilities caps = getCaps(GLProfile.GL3bc);
        if( null == caps ) { return; }
        testImpl(caps, 0);
    }

    @Test
    public void test12_GL3Compat_FlipXYZ() throws InterruptedException {
        final GLCapabilities caps = getCaps(GLProfile.GL3bc);
        if( null == caps ) { return; }
        testImpl(caps, 1);
    }

    private void testImpl(final GLCapabilities caps, final int geomShader) throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setSize(800, 600);
        glWindow.setVisible(true);
        glWindow.setTitle("JOGL Geometry Shader Banana Test");
        Assert.assertTrue(glWindow.isNativeValid());

        final QuitAdapter quitAdapter = new QuitAdapter();
        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);
        glWindow.addGLEventListener( new GeomShader01TextureGL3(geomShader) );

        final SnapshotGLEventListener snapshotGLEventListener = new SnapshotGLEventListener();
        glWindow.addGLEventListener(snapshotGLEventListener);

        final Animator animator = new Animator(glWindow);
        animator.start();

        animator.setUpdateFPSFrames(60, System.err);
        snapshotGLEventListener.setMakeSnapshot();

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        animator.stop();
        glWindow.destroy();
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestGeomShader01TextureGL3NEWT.class.getName());
    }
}

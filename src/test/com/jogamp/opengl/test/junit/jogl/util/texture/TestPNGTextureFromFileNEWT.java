/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.util.texture;


import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.TextureDraw01Accessor;
import com.jogamp.opengl.test.junit.jogl.demos.es2.TextureDraw01ES2Listener;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.TextureDraw01GL2Listener;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;

import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPNGTextureFromFileNEWT extends UITestCase {
    static boolean showFPS = false;
    static long duration = 100; // ms
    InputStream grayTextureStream;

    InputStream testTextureStreamN_3;
    InputStream testTextureStreamN_4;
    InputStream testTextureStreamNG4;

    InputStream testTextureStreamI_3;
    InputStream testTextureStreamIG3;
    InputStream testTextureStreamI_4;
    InputStream testTextureStreamIG4;

    InputStream testTextureStreamP_3;
    InputStream testTextureStreamP_4;

    @Before
    public void initTest() throws IOException {
        grayTextureStream = TestPNGTextureFromFileNEWT.class.getResourceAsStream( "grayscale_texture.png" );
        Assert.assertNotNull(grayTextureStream);
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-ntscN_3-01-160x90.png", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamN_3 = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamN_3);
        }
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-ntscN_4-01-160x90.png", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamN_4 = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamN_4);
        }
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-ntscNG4-01-160x90.png", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamNG4 = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamNG4);
        }

        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-ntscI_3-01-160x90.png", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamI_3 = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamI_3);
        }
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-ntscIG3-01-160x90.png", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamIG3 = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamIG3);
        }
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-ntscI_4-01-160x90.png", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamI_4 = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamI_4);
        }
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-ntscIG4-01-160x90.png", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamIG4 = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamIG4);
        }


        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-ntscP_3-01-160x90.png", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamP_3 = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamP_3);
        }
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-ntscP_4-01-160x90.png", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamP_4 = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamP_4);
        }
    }

    @After
    public void cleanupTest() {
        grayTextureStream = null;
        testTextureStreamN_3 = null;
        testTextureStreamI_3 = null;
        testTextureStreamIG3 = null;
        testTextureStreamP_3 = null;
        testTextureStreamP_4 = null;
    }

    public void testImpl(final boolean useFFP, final InputStream istream) throws InterruptedException, IOException {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        GLProfile glp;
        if(useFFP && GLProfile.isAvailable(GLProfile.GL2)) {
            glp = GLProfile.getMaxFixedFunc(true);
        } else if(!useFFP && GLProfile.isAvailable(GLProfile.GL2ES2)) {
            glp = GLProfile.getGL2ES2();
        } else {
            System.err.println(getSimpleTestName(".")+": GLProfile n/a, useFFP: "+useFFP);
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(1);

        final TextureData texData = TextureIO.newTextureData(glp, istream, false /* mipmap */, TextureIO.PNG);
        System.err.println("TextureData: "+texData);

        final GLWindow glad = GLWindow.create(caps);
        glad.setTitle("TestPNGTextureGL2FromFileNEWT");
        // Size OpenGL to Video Surface
        glad.setSize(texData.getWidth(), texData.getHeight());

        // load texture from file inside current GL context to match the way
        // the bug submitter was doing it
        final GLEventListener gle = useFFP ? new TextureDraw01GL2Listener( texData ) : new TextureDraw01ES2Listener( texData, 0 ) ;
        glad.addGLEventListener(gle);
        glad.addGLEventListener(new GLEventListener() {
            boolean shot = false;

            @Override public void init(final GLAutoDrawable drawable) {}

            public void display(final GLAutoDrawable drawable) {
                // 1 snapshot
                if(null!=((TextureDraw01Accessor)gle).getTexture() && !shot) {
                    shot = true;
                    snapshot(0, null, drawable.getGL(), screenshot, TextureIO.PNG, null);
                }
            }

            @Override public void dispose(final GLAutoDrawable drawable) { }
            @Override public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
        });

        final Animator animator = new Animator(glad);
        animator.setUpdateFPSFrames(60, showFPS ? System.err : null);
        final QuitAdapter quitAdapter = new QuitAdapter();
        glad.addKeyListener(quitAdapter);
        glad.addWindowListener(quitAdapter);
        glad.setVisible(true);
        animator.start();

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        animator.stop();
        glad.destroy();
    }

    @Test
    public void testGray__GL2() throws InterruptedException, IOException {
        testImpl(true, grayTextureStream);
    }
    @Test
    public void testGray__ES2() throws InterruptedException, IOException {
        testImpl(false, grayTextureStream);
    }

    @Test
    public void testRGB3__GL2() throws InterruptedException, IOException {
        testImpl(true, testTextureStreamN_3);
    }
    @Test
    public void testRGB3__ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamN_3);
    }
    @Test
    public void testRGB4__GL2() throws InterruptedException, IOException {
        testImpl(true, testTextureStreamN_4);
    }
    @Test
    public void testRGB4__ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamN_4);
    }
    @Test
    public void testRGB4G_ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamNG4);
    }

    @Test
    public void testInterl3__ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamI_3);
    }
    @Test
    public void testInterl4__ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamI_4);
    }
    @Test
    public void testInterl3G_ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamIG3);
    }
    @Test
    public void testInterl4G_ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamIG4);
    }

    @Test
    public void testPalette3__ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamP_3);
    }
    @Test
    public void testPalette4__ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamP_4);
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestPNGTextureFromFileNEWT.class.getName());
    }
}

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
public class TestJPEGTextureFromFileNEWT extends UITestCase {
    static boolean showFPS = false;
    static long duration = 100; // ms

    InputStream testTextureStream01YUV444_Base;
    InputStream testTextureStream01YUV444_Prog;

    InputStream testTextureStream01YUV422h_Base;
    InputStream testTextureStream01YUV422h_Prog;

    InputStream testTextureStream02YUV420_Base;
    InputStream testTextureStream02YUV420_Prog;
    InputStream testTextureStream02YUV420_BaseGray;

    InputStream testTextureStream03CMYK_01;
    InputStream testTextureStream03YCCK_01;

    InputStream testTextureStream04QTTDefPostFrame;

    @Before
    public void initTest() throws IOException {
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-ntscN_3-01-160x90-90pct-yuv444-base.jpg", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStream01YUV444_Base = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStream01YUV444_Base);
        }
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-ntscN_3-01-160x90-90pct-yuv444-prog.jpg", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStream01YUV444_Prog = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStream01YUV444_Prog);
        }
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-ntscN_3-01-160x90-60pct-yuv422h-base.jpg", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStream01YUV422h_Base = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStream01YUV422h_Base);
        }
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-ntscN_3-01-160x90-60pct-yuv422h-prog.jpg", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStream01YUV422h_Prog = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStream01YUV422h_Prog);
        }

        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("j1-baseline.jpg", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStream02YUV420_Base = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStream02YUV420_Base);
        }
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("j2-progressive.jpg", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStream02YUV420_Prog = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStream02YUV420_Prog);
        }
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("j3-baseline_gray.jpg", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStream02YUV420_BaseGray = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStream02YUV420_BaseGray);
        }

        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-cmyk-01.jpg", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStream03CMYK_01 = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStream03CMYK_01);
        }
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-ycck-01.jpg", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStream03YCCK_01 = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStream03YCCK_01);
        }
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("bug745_qttdef_post_frame.jpg", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStream04QTTDefPostFrame = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStream04QTTDefPostFrame);
        }

    }

    @After
    public void cleanupTest() {
        testTextureStream01YUV444_Base = null;
        testTextureStream01YUV444_Prog = null;
        testTextureStream01YUV422h_Base = null;
        testTextureStream01YUV422h_Prog = null;
        testTextureStream02YUV420_Base = null;
        testTextureStream02YUV420_Prog = null;
        testTextureStream02YUV420_BaseGray = null;
        testTextureStream03CMYK_01 = null;
        testTextureStream03YCCK_01 = null;
        testTextureStream04QTTDefPostFrame = null;
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

        final TextureData texData = TextureIO.newTextureData(glp, istream, false /* mipmap */, TextureIO.JPG);
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
    public void test01YUV444Base__GL2() throws InterruptedException, IOException {
        testImpl(true, testTextureStream01YUV444_Base);
    }
    @Test
    public void test01YUV444Base__ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStream01YUV444_Base);
    }
    @Test
    public void test01YUV444Prog__GL2() throws InterruptedException, IOException {
        testImpl(true, testTextureStream01YUV444_Prog);
    }
    @Test
    public void test01YUV444Prog__ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStream01YUV444_Prog);
    }

    @Test
    public void test01YUV422hBase__ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStream01YUV422h_Base);
    }
    @Test
    public void test01YUV422hProg_ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStream01YUV422h_Prog);
    }

    @Test
    public void test02YUV420Base__ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStream02YUV420_Base);
    }
    @Test
    public void test02YUV420Prog_ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStream02YUV420_Prog);
    }
    @Test
    public void test02YUV420BaseGray_ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStream02YUV420_BaseGray);
    }

    @Test
    public void test03CMYK_01_ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStream03CMYK_01);
    }
    @Test
    public void test03YCCK_01_ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStream03YCCK_01);
    }

    @Test
    public void test04QTTDefPostFrame_ES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStream04QTTDefPostFrame);
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestJPEGTextureFromFileNEWT.class.getName());
    }
}

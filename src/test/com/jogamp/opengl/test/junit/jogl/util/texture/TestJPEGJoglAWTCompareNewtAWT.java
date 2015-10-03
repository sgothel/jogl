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
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;

import javax.imageio.ImageIO;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;

import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureData;
import com.jogamp.opengl.util.texture.spi.JPEGImage;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.GLReadBufferUtil;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestJPEGJoglAWTCompareNewtAWT extends UITestCase {
    static boolean showFPS = false;
    static long duration = 100; // ms

    String[] files = { "test-ntscN_3-01-160x90-90pct-yuv444-base.jpg",   // 0
                       "test-ntscN_3-01-160x90-90pct-yuv444-prog.jpg",   // 1
                       "test-ntscN_3-01-160x90-60pct-yuv422h-base.jpg",  // 2
                       "test-ntscN_3-01-160x90-60pct-yuv422h-prog.jpg",  // 3
                       "j1-baseline.jpg",                                // 4
                       "j2-progressive.jpg", // 5
                       "j3-baseline_gray.jpg", // 6
                       "test-cmyk-01.jpg", // 7
                       "test-ycck-01.jpg" }; // 8

    void testImpl(final String fname) throws InterruptedException, IOException {
        final Animator animator = new Animator();

        final GLWindow w1 = testJOGLJpeg(fname);
        final GLWindow w2 = testAWTJpeg(fname, w1.getSurfaceWidth() + 50);

        animator.add(w1);
        animator.add(w2);
        animator.setUpdateFPSFrames(60, showFPS ? System.err : null);
        final QuitAdapter quitAdapter = new QuitAdapter();
        w1.setVisible(true);
        w2.setVisible(true);
        animator.start();

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        animator.stop();
        w1.destroy();
        w2.destroy();
    }

    GLWindow testJOGLJpeg(final String fname) throws InterruptedException, IOException {
        final URLConnection testTextureUrlConn = IOUtil.getResource(fname, this.getClass().getClassLoader(), this.getClass());
        Assert.assertNotNull(testTextureUrlConn);
        final InputStream istream = testTextureUrlConn.getInputStream();
        Assert.assertNotNull(istream);

        final JPEGImage image = JPEGImage.read(istream);
        Assert.assertNotNull(image);
        System.err.println("JPEGImage: "+image);

        final GLProfile glp = GLProfile.getGL2ES2();
        final int internalFormat = (image.getBytesPerPixel()==4)?GL.GL_RGBA:GL.GL_RGB;
        final TextureData texData = new TextureData(glp, internalFormat,
                                       image.getWidth(),
                                       image.getHeight(),
                                       0,
                                       new GLPixelAttributes(image.getGLFormat(), image.getGLType()),
                                       false /* mipmap */,
                                       false /* compressed */,
                                       false /* must flip-vert */,
                                       image.getData(),
                                       null);
        // final TextureData texData = TextureIO.newTextureData(glp, istream, false /* mipmap */, TextureIO.JPG);
        System.err.println("TextureData: "+texData);

        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(1);

        final GLWindow glad1 = GLWindow.create(caps);
        glad1.setTitle("JPEG JOGL");
        // Size OpenGL to Video Surface
        glad1.setSize(texData.getWidth(), texData.getHeight());
        glad1.setPosition(0, 0);

        // load texture from file inside current GL context to match the way
        // the bug submitter was doing it
        final GLEventListener gle = new TextureDraw01ES2Listener( texData, 0 ) ;
        glad1.addGLEventListener(gle);
        glad1.addGLEventListener(new GLEventListener() {
            boolean shot = false;

            @Override public void init(final GLAutoDrawable drawable) {}

            public void display(final GLAutoDrawable drawable) {
                // 1 snapshot
                if(null!=((TextureDraw01Accessor)gle).getTexture() && !shot) {
                    shot = true;
                    snapshot(0, "JoglJPEG", drawable.getGL(), screenshot, TextureIO.PNG, null);
                }
            }

            @Override public void dispose(final GLAutoDrawable drawable) { }
            @Override public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
        });

        return glad1;
    }

    GLWindow testAWTJpeg(final String fname, final int xpos) throws InterruptedException, IOException {
        final URLConnection testTextureUrlConn = IOUtil.getResource(fname, this.getClass().getClassLoader(), this.getClass());
        Assert.assertNotNull(testTextureUrlConn);
        final InputStream istream = testTextureUrlConn.getInputStream();
        Assert.assertNotNull(istream);

        final GLProfile glp = GLProfile.getGL2ES2();
        TextureData texData = null;
        int w = 300, h = 300;
        try {
            final BufferedImage img = ImageIO.read(istream);
            texData = new AWTTextureData(glp, 0, 0, false, img);
            System.err.println("TextureData: "+texData);
            w = texData.getWidth();
            h = texData.getHeight();
        } catch (final Exception e) {
            System.err.println("AWT ImageIO failure w/ file "+fname+": "+e.getMessage());
            // e.printStackTrace(); // : CMYK, YCCK -> com.sun.imageio.plugins.jpeg.JPEGImageReader.readInternal(Unknown Source)
        }

        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(1);

        final GLWindow glad1 = GLWindow.create(caps);
        glad1.setTitle("JPEG AWT");
        // Size OpenGL to Video Surface
        glad1.setSize(w, h);
        glad1.setPosition(xpos, 0);

        // load texture from file inside current GL context to match the way
        // the bug submitter was doing it
        final GLEventListener gle;
        if( texData != null ) {
            gle = new TextureDraw01ES2Listener( texData, 0 ) ;
            glad1.addGLEventListener(gle);
        } else {
            gle = null;
        }
        glad1.addGLEventListener(new GLEventListener() {
            boolean shot = false;

            @Override public void init(final GLAutoDrawable drawable) {}

            public void display(final GLAutoDrawable drawable) {
                // 1 snapshot
                if( null!=gle && null!=((TextureDraw01Accessor)gle).getTexture() && !shot) {
                    shot = true;
                    snapshot(0, "AWTJPEG", drawable.getGL(), screenshot, TextureIO.PNG, null);
                }
            }

            @Override public void dispose(final GLAutoDrawable drawable) { }
            @Override public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
        });

        return glad1;
    }

    @Test
    public void test01YUV444Base__ES2() throws InterruptedException, IOException {
        testImpl(files[0]);
    }
    @Test
    public void test01YUV444Prog__ES2() throws InterruptedException, IOException {
        testImpl(files[1]);
    }

    @Test
    public void test01YUV422hBase__ES2() throws InterruptedException, IOException {
        testImpl(files[2]);
    }
    @Test
    public void test01YUV422hProg_ES2() throws InterruptedException, IOException {
        testImpl(files[3]);
    }

    @Test
    public void test02YUV420Base__ES2() throws InterruptedException, IOException {
        testImpl(files[4]);
    }
    @Test
    public void test02YUV420Prog_ES2() throws InterruptedException, IOException {
        testImpl(files[5]);
    }
    @Test
    public void test02YUV420BaseGray_ES2() throws InterruptedException, IOException {
        testImpl(files[6]);
    }

    @Test
    public void test03CMYK_01_ES2() throws InterruptedException, IOException {
        testImpl(files[7]);
    }
    @Test
    public void test03YCCK_01_ES2() throws InterruptedException, IOException {
        testImpl(files[8]);
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestJPEGJoglAWTCompareNewtAWT.class.getName());
    }
}

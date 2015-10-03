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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URLConnection;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.TextureDraw01Accessor;
import com.jogamp.opengl.test.junit.jogl.demos.es2.TextureDraw01ES2Listener;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.spi.JPEGImage;
import com.jogamp.opengl.GL;

/**
 * Test reading and displaying a JPG image.
 * <p>
 * Main function accepts arbitrary JPG file name for manual tests.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestJPEGImage01NEWT extends UITestCase {

    static boolean showFPS = false;
    static long duration = 100; // ms

    public void testImpl(final InputStream istream) throws InterruptedException, IOException {
        final JPEGImage image = JPEGImage.read(istream);
        Assert.assertNotNull(image);
        final boolean hasAlpha = 4 == image.getBytesPerPixel();
        System.err.println("JPEGImage: "+image+", hasAlpha "+hasAlpha);

        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        if( hasAlpha ) {
            caps.setAlphaBits(1);
        }

        final int internalFormat;
        if(glp.isGL2ES3()) {
            internalFormat = hasAlpha ? GL.GL_RGBA8 : GL.GL_RGB8;
        } else {
            internalFormat = hasAlpha ? GL.GL_RGBA : GL.GL_RGB;
        }
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

        final GLWindow glad = GLWindow.create(caps);
        glad.setTitle("TestJPEGImage01NEWT");
        // Size OpenGL to Video Surface
        glad.setSize(texData.getWidth(), texData.getHeight());

        // load texture from file inside current GL context to match the way
        // the bug submitter was doing it
        final GLEventListener gle = new TextureDraw01ES2Listener( texData, 0 ) ;
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
    public void testReadES2_RGBn() throws InterruptedException, IOException, MalformedURLException {
        final String fname = null == _fname ? "test-ntscN_3-01-160x90-90pct-yuv444-base.jpg" : _fname;
        final URLConnection urlConn = IOUtil.getResource(fname, this.getClass().getClassLoader(), this.getClass());
        testImpl(urlConn.getInputStream());
    }

    static String _fname = null;
    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-file")) {
                i++;
                _fname = args[i];
            }
        }
        org.junit.runner.JUnitCore.main(TestJPEGImage01NEWT.class.getName());
    }
}

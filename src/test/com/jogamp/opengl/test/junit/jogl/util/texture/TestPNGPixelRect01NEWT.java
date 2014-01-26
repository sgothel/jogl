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
package com.jogamp.opengl.test.junit.jogl.util.texture;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URLConnection;

import javax.media.nativewindow.util.PixelFormat;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

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
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.PNGPixelRect;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * Test reading and displaying a PNG image.
 * <p>
 * Main function accepts arbitrary PNG file name for manual tests.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPNGPixelRect01NEWT extends UITestCase {
    static boolean showFPS = false;
    static long duration = 200; // ms

    public void testImpl(final int num, final String basename, final InputStream istream, final PixelFormat destFmt) throws InterruptedException, IOException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final PNGPixelRect image = PNGPixelRect.read(istream, destFmt, true /* directBuffer */, 0 /* destMinStrideInBytes */, true /* destIsGLOriented */);
        Assert.assertNotNull(image);
        final GLPixelAttributes glpa = GLPixelAttributes.convert(image.getPixelformat(), glp);
        final boolean hasAlpha = 4 == glpa.bytesPerPixel;
        System.err.println("PNGPixelRect: "+basename+", "+image+", glpa "+glpa);

        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        final GLCapabilities caps = new GLCapabilities(glp);
        if( hasAlpha  ) {
            caps.setAlphaBits(1);
        }

        final int internalFormat;
        if(glp.isGL2ES3()) {
            internalFormat = hasAlpha ? GL.GL_RGBA8 : GL.GL_RGB8;
        } else {
            internalFormat = hasAlpha ? GL.GL_RGBA : GL.GL_RGB;
        }
        final TextureData texData = new TextureData(glp, internalFormat,
                                       image.getSize().getWidth(),
                                       image.getSize().getHeight(),
                                       0,
                                       glpa,
                                       false /* mipmap */,
                                       false /* compressed */,
                                       false /* must flip-vert */,
                                       image.getPixels(),
                                       null);

        // final TextureData texData = TextureIO.newTextureData(glp, istream, false /* mipmap */, TextureIO.PNG);
        System.err.println("TextureData: "+texData);

        final GLWindow glad = GLWindow.create(caps);
        glad.setTitle(this.getSimpleTestName("."));
        // Size OpenGL to Video Surface
        glad.setSize(texData.getWidth(), texData.getHeight());

        // load texture from file inside current GL context to match the way
        // the bug submitter was doing it
        final TextureDraw01ES2Listener gle = new TextureDraw01ES2Listener( texData, 0 ) ;
        // gle.setClearColor(new float[] { 1.0f, 0.0f, 0.0f, 1.0f } );

        glad.addGLEventListener(gle);
        glad.addGLEventListener(new GLEventListener() {
            boolean shot = false;

            @Override public void init(GLAutoDrawable drawable) {
                System.err.println("Chosen Caps: " + drawable.getChosenGLCapabilities());
                System.err.println("GL ctx: " + drawable.getGL().getContext());
            }

            @Override public void display(GLAutoDrawable drawable) {
                // 1 snapshot
                if(null!=((TextureDraw01Accessor)gle).getTexture() && !shot) {
                    shot = true;
                    snapshot(num, basename, drawable.getGL(), screenshot, TextureIO.PNG, null);
                }
            }

            @Override public void dispose(GLAutoDrawable drawable) { }
            @Override public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) { }
        });

        Animator animator = new Animator(glad);
        animator.setUpdateFPSFrames(60, showFPS ? System.err : null);
        QuitAdapter quitAdapter = new QuitAdapter();
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
    public void testRead00_Manual() throws InterruptedException, IOException, MalformedURLException {
        if( null == _fname ) {
            return;
        }
        final URLConnection urlConn = IOUtil.getResource(this.getClass(), _fname);
        if( null == urlConn ) {
            throw new IOException("Cannot find "+_fname+".png");
        }
        testImpl(0, _fname, urlConn.getInputStream(), null);
    }

    @Test
    public void testRead01_All() throws InterruptedException, IOException, MalformedURLException {
        if( null != _fname ) {
            return;
        }
        for(int i=0; i<PNGTstFiles.allBasenames.length; i++) {
            final String basename = PNGTstFiles.allBasenames[i];
            final URLConnection urlConn = IOUtil.getResource(this.getClass(), basename+".png");
            if( null == urlConn ) {
                throw new IOException("Cannot find "+basename+".png");
            }
            testImpl(i, basename, urlConn.getInputStream(), null);
        }
    }
    @Test
    public void testRead02_Gray2RGBA() throws InterruptedException, IOException, MalformedURLException {
        if( null != _fname ) {
            return;
        }
        for(int i=0; i<PNGTstFiles.greyBasenames.length; i++) {
            final String basename = PNGTstFiles.greyBasenames[i];
            final URLConnection urlConn = IOUtil.getResource(this.getClass(), basename+".png");
            if( null == urlConn ) {
                throw new IOException("Cannot find "+basename+".png");
            }
            testImpl(i, basename, urlConn.getInputStream(), PixelFormat.RGBA8888);
        }
    }

    static String _fname = null;
    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-file")) {
                i++;
                _fname = args[i];
            }
        }
        org.junit.runner.JUnitCore.main(TestPNGPixelRect01NEWT.class.getName());
    }
}

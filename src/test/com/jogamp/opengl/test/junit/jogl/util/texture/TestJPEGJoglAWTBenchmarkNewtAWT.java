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
import com.jogamp.opengl.test.junit.util.UITestCase;

import javax.imageio.ImageIO;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLProfile;

import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.awt.AWTTextureData;
import com.jogamp.opengl.util.texture.spi.JPEGImage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.Buffer;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestJPEGJoglAWTBenchmarkNewtAWT extends UITestCase {
    static boolean showFPS = false;
    static String fname = "j1-baseline.jpg";

    @Test
    public void benchmark() throws IOException {
        benchmarkImpl(100, fname);
    }
    void benchmarkImpl(final int loops, final String fname) throws IOException {
        {
            final long t0 = System.currentTimeMillis();
            for(int i = 0; i< loops; i++ ) {
                final URLConnection urlConn = IOUtil.getResource(fname, this.getClass().getClassLoader(), this.getClass());
                final InputStream istream = urlConn.getInputStream();
                final JPEGImage image = JPEGImage.read(istream); // parsing & completion done !!!
                final int internalFormat = (image.getBytesPerPixel()==4)?GL.GL_RGBA:GL.GL_RGB;
                final TextureData texData = new TextureData(GLProfile.getGL2ES2(), internalFormat,
                                               image.getWidth(),
                                               image.getHeight(),
                                               0,
                                               new GLPixelAttributes(image.getGLFormat(), image.getGLType()),
                                               false /* mipmap */,
                                               false /* compressed */,
                                               false /* must flip-vert */,
                                               image.getData(),
                                               null);
                if(0==i || loops-1==i) {
                    System.err.println(i+": "+image.toString());
                    System.err.println(i+": "+texData+", buffer "+texData.getBuffer());
                }
                istream.close();
            }
            final long t1 = System.currentTimeMillis();
            final long dt = t1 - t0;
            final float msPl = (float)dt / (float)loops ;
            System.err.println("JOGL.RGB Loops "+loops+", dt "+dt+" ms, "+msPl+" ms/l");
        }
        {
            final long t0 = System.currentTimeMillis();
            for(int i = 0; i< loops; i++ ) {
                final URLConnection urlConn = IOUtil.getResource(fname, this.getClass().getClassLoader(), this.getClass());
                final InputStream istream = urlConn.getInputStream();
                final JPEGImage image = JPEGImage.read(istream, TextureData.ColorSpace.YCbCr); // parsing & completion done !!!
                final int internalFormat = (image.getBytesPerPixel()==4)?GL.GL_RGBA:GL.GL_RGB;
                final TextureData texData = new TextureData(GLProfile.getGL2ES2(), internalFormat,
                                               image.getWidth(),
                                               image.getHeight(),
                                               0,
                                               new GLPixelAttributes(image.getGLFormat(), image.getGLType()),
                                               false /* mipmap */,
                                               false /* compressed */,
                                               false /* must flip-vert */,
                                               image.getData(),
                                               null);
                if(0==i || loops-1==i) {
                    System.err.println(i+": "+image.toString());
                    System.err.println(i+": "+texData+", buffer "+texData.getBuffer());
                }
                istream.close();
            }
            final long t1 = System.currentTimeMillis();
            final long dt = t1 - t0;
            final float msPl = (float)dt / (float)loops ;
            System.err.println("JOGL.YUV Loops "+loops+", dt "+dt+" ms, "+msPl+" ms/l");
        }
        {
            final long t0 = System.currentTimeMillis();
            for(int i = 0; i< loops; i++ ) {
                final URLConnection urlConn = IOUtil.getResource(fname, this.getClass().getClassLoader(), this.getClass());
                final InputStream istream = urlConn.getInputStream();
                Buffer data = null;
                try {
                    final BufferedImage img = ImageIO.read(istream);
                    final AWTTextureData texData = new AWTTextureData(GLProfile.getGL2ES2(), 0, 0, false, img);
                    data = texData.getBuffer(); // completes data conversion !!!
                    if(0==i || loops-1==i) {
                        System.err.println(i+": "+texData+", buffer "+data);
                    }
                } catch (final Exception e) {
                    System.err.println("AWT ImageIO failure w/ file "+fname+": "+e.getMessage());
                }
                istream.close();
            }
            final long t1 = System.currentTimeMillis();
            final long dt = t1 - t0;
            final float msPl = (float)dt / (float)loops ;
            System.err.println("AWT..... Loops "+loops+", dt "+dt+" ms, "+msPl+" ms/l");
        }
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-file")) {
                i++;
                fname = args[i];
            }
        }
        org.junit.runner.JUnitCore.main(TestJPEGJoglAWTBenchmarkNewtAWT.class.getName());
    }
}

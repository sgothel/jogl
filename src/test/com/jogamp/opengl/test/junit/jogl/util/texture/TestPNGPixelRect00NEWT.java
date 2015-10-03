/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URLConnection;

import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.nativewindow.util.PixelFormatUtil;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.PNGPixelRect;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPNGPixelRect00NEWT extends UITestCase {
    @Test
    public void testPNGRead01_All() throws InterruptedException, IOException, MalformedURLException {
        for(int i=0; i<PNGTstFiles.allBasenames.length; i++) {
            final String basename = PNGTstFiles.allBasenames[i];
            final String pathname="";
            testPNG01Impl(pathname, basename, null, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
        }
    }

    @Test
    public void testPNGRead02_RGB888_to_RGBA8888() throws InterruptedException, IOException, MalformedURLException {
        final String basename ="test-ntscN_3-01-160x90";
        final String pathname="";
        testPNG01Impl(pathname, basename, PixelFormat.RGBA8888, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }

    @Test
    public void testPNGRead03_RGB888_to_RGBA8888_stride1000() throws InterruptedException, IOException, MalformedURLException {
        final String basename ="test-ntscN_3-01-160x90"; // 640 bytes = 4 * 160
        final String pathname="";
        testPNG01Impl(pathname, basename, PixelFormat.RGBA8888, 1000 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }

    @Test
    public void testPNGRead04_RGB888_to_RGBA8888_stride999() throws InterruptedException, IOException, MalformedURLException {
        final String basename ="test-ntscN_3-01-160x90"; // 640 bytes = 4 * 160
        final String pathname="";
        testPNG01Impl(pathname, basename, PixelFormat.RGBA8888, 999 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }

    @Test
    public void testPNGRead11_RGBA8888_to_LUMINA() throws InterruptedException, IOException, MalformedURLException {
        final String basename ="test-ntscN_4-01-160x90";
        final String pathname="";
        testPNG02Impl(pathname, basename, PixelFormat.LUMINANCE, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }

    @Test
    public void testPNGRead12_RGBA8888_to_RGB888() throws InterruptedException, IOException, MalformedURLException {
        final String basename ="test-ntscN_4-01-160x90";
        final String pathname="";
        testPNG02Impl(pathname, basename, PixelFormat.RGB888, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }

    @Test
    public void testPNGRead13_RGBA8888_to_BGR888() throws InterruptedException, IOException, MalformedURLException {
        final String basename ="test-ntscN_4-01-160x90";
        final String pathname="";
        testPNG02Impl(pathname, basename, PixelFormat.BGR888, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }

    @Test
    public void testPNGRead14_RGBA8888_to_BGRA8888() throws InterruptedException, IOException, MalformedURLException {
        final String basename ="test-ntscN_4-01-160x90";
        final String pathname="";
        testPNG02Impl(pathname, basename, PixelFormat.BGRA8888, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }
    @Test
    public void testPNGRead15_RGBA8888_to_ARGB8888() throws InterruptedException, IOException, MalformedURLException {
        final String basename ="test-ntscN_4-01-160x90";
        final String pathname="";
        testPNG02Impl(pathname, basename, PixelFormat.ARGB8888, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }
    @Test
    public void testPNGRead16_RGBA8888_to_ABGR8888() throws InterruptedException, IOException, MalformedURLException {
        final String basename ="test-ntscN_4-01-160x90";
        final String pathname="";
        testPNG02Impl(pathname, basename, PixelFormat.ABGR8888, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }

    private void testPNG01Impl(final String pathname, final String basename,
                             final PixelFormat destFmt, final int destMinStrideInBytes, final boolean destIsGLOriented)
            throws InterruptedException, IOException, MalformedURLException
    {
        System.err.println("Test01: "+pathname+basename+".png, destFmt "+destFmt+", destMinStrideInBytes "+destMinStrideInBytes+", destIsGLOriented "+destIsGLOriented);

        final File out1_f=new File(getSimpleTestName(".")+"-01-"+basename+"-orig.png");
        final File out2F_f=new File(getSimpleTestName(".")+"-02-"+basename+"-flipped.png");
        final File out2R_f=new File(getSimpleTestName(".")+"-03-"+basename+"-reversed.png");
        final File out2RF_f=new File(getSimpleTestName(".")+"-04-"+basename+"-reversed_flipped.png");
        final URLConnection urlConn = IOUtil.getResource(pathname+basename+".png", this.getClass().getClassLoader(), this.getClass());
        if( null == urlConn ) {
            throw new IOException("Cannot find "+pathname+basename+".png");
        }
        final PNGPixelRect image1 = PNGPixelRect.read(urlConn.getInputStream(), destFmt, false /* directBuffer */, destMinStrideInBytes, destIsGLOriented);
        System.err.println("PNGPixelRect - Orig: "+image1);
        {
            final OutputStream outs = new BufferedOutputStream(IOUtil.getFileOutputStream(out1_f, true /* allowOverwrite */));
            image1.write(outs, true /* close */);
            {
                final PNGPixelRect image1_R = PNGPixelRect.read(out1_f.toURI().toURL().openStream(), image1.getPixelformat(), false /* directBuffer */, destMinStrideInBytes, destIsGLOriented);
                System.err.println("PNGPixelRect - Orig (Read Back): "+image1_R);
                Assert.assertEquals(image1.getPixels(), image1_R.getPixels());
            }
        }

        //
        // Flipped Orientation
        //
        {
            final PNGPixelRect image2F = new PNGPixelRect(image1.getPixelformat(), image1.getSize(),
                                                          image1.getStride(), !image1.isGLOriented(), image1.getPixels(),
                                                          image1.getDpi()[0], image1.getDpi()[1]);
            System.err.println("PNGPixelRect - Flip : "+image2F);
            final OutputStream outs = new BufferedOutputStream(IOUtil.getFileOutputStream(out2F_f, true /* allowOverwrite */));
            image2F.write(outs, true /* close */);
            {
                // flip again .. to compare w/ original
                final PNGPixelRect image2F_R = PNGPixelRect.read(out2F_f.toURI().toURL().openStream(), image1.getPixelformat(), false /* directBuffer */, destMinStrideInBytes, !destIsGLOriented);
                System.err.println("PNGPixelRect - Flip (Read Back): "+image2F_R);
                Assert.assertEquals(image1.getPixels(), image2F_R.getPixels());
            }
        }

        //
        // Reversed Components
        //
        final PixelFormat revFmt = PixelFormatUtil.getReversed(image1.getPixelformat());
        {
            final PNGPixelRect image2R = new PNGPixelRect(revFmt, image1.getSize(),
                                                          image1.getStride(), image1.isGLOriented(), image1.getPixels(),
                                                          image1.getDpi()[0], image1.getDpi()[1]);
            System.err.println("PNGPixelRect - Reversed : "+image2R);
            final OutputStream outs = new BufferedOutputStream(IOUtil.getFileOutputStream(out2R_f, true /* allowOverwrite */));
            image2R.write(outs, true /* close */);
            {
                // reverse again .. to compare w/ original
                final PNGPixelRect image2R_R = PNGPixelRect.read(out2R_f.toURI().toURL().openStream(), revFmt, false /* directBuffer */, destMinStrideInBytes, destIsGLOriented);
                System.err.println("PNGPixelRect - Reversed (Read Back): "+image2R_R);
                Assert.assertEquals(image1.getPixels(), image2R_R.getPixels());
            }
        }

        // reversed channels and flipped
        {
            final PNGPixelRect image2RF = new PNGPixelRect(revFmt, image1.getSize(),
                                                           image1.getStride(), !image1.isGLOriented(), image1.getPixels(),
                                                           image1.getDpi()[0], image1.getDpi()[1]);
            System.err.println("PNGPixelRect - Reversed+Flipped : "+image2RF);
            final OutputStream outs = new BufferedOutputStream(IOUtil.getFileOutputStream(out2RF_f, true /* allowOverwrite */));
            image2RF.write(outs, true /* close */);
            {
                // reverse+flip again .. to compare w/ original
                final PNGPixelRect image2RF_R = PNGPixelRect.read(out2RF_f.toURI().toURL().openStream(), revFmt, false /* directBuffer */, destMinStrideInBytes, !destIsGLOriented);
                System.err.println("PNGPixelRect - Reversed+FLipped (Read Back): "+image2RF_R);
                Assert.assertEquals(image1.getPixels(), image2RF_R.getPixels());
            }
        }
    }

    private void testPNG02Impl(final String pathname, final String basename,
                             final PixelFormat destFmt, final int destMinStrideInBytes, final boolean destIsGLOriented)
            throws InterruptedException, IOException, MalformedURLException
    {
        System.err.println("Test02: "+pathname+basename+".png, destFmt "+destFmt+", destMinStrideInBytes "+destMinStrideInBytes+", destIsGLOriented "+destIsGLOriented);

        final File out1_f=new File(getSimpleTestName(".")+"-"+basename+"-orig.png");
        final URLConnection urlConn = IOUtil.getResource(pathname+basename+".png", this.getClass().getClassLoader(), this.getClass());

        final PNGPixelRect image1 = PNGPixelRect.read(urlConn.getInputStream(), destFmt, false /* directBuffer */, destMinStrideInBytes, destIsGLOriented);
        System.err.println("PNGPixelRect - Orig: "+image1);
        {
            final OutputStream outs = new BufferedOutputStream(IOUtil.getFileOutputStream(out1_f, true /* allowOverwrite */));
            image1.write(outs, true /* close */);
            {
                final PNGPixelRect image1_R = PNGPixelRect.read(out1_f.toURI().toURL().openStream(), image1.getPixelformat(), false /* directBuffer */, destMinStrideInBytes, destIsGLOriented);
                System.err.println("PNGPixelRect - Orig (Read Back): "+image1_R);
                Assert.assertEquals(image1.getPixels(), image1_R.getPixels());
            }
        }
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestPNGPixelRect00NEWT.class.getName());
    }
}

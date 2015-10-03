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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLConnection;

import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.nativewindow.util.PixelFormatUtil;
import com.jogamp.nativewindow.util.PixelRectangle;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.util.Bitstream;
import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.PNGPixelRect;

/**
 * Testing PixelFormatUtil's Conversion using PNG test data
 * including strides, endian-order and PixelFormat conversions:
 *    { PixelFormat.RGBA8888, PixelFormat.ABGR8888, PixelFormat.BGRA8888, PixelFormat.ARGB8888 }
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPixelFormatUtil01NEWT extends UITestCase {
    @Test
    public void testPNGRead11_fromRGBA8888() throws InterruptedException, IOException, MalformedURLException {
        final String basename ="test-ntscN_4-01-160x90";
        final String pathname="";
        testPNG01Impl(pathname, basename, PixelFormat.RGBA8888, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }
    @Test
    public void testPNGRead12_fromABGR8888() throws InterruptedException, IOException, MalformedURLException {
        final String basename ="test-ntscN_4-01-160x90";
        final String pathname="";
        testPNG01Impl(pathname, basename, PixelFormat.ABGR8888, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }
    @Test
    public void testPNGRead13_fromBGRA8888() throws InterruptedException, IOException, MalformedURLException {
        final String basename ="test-ntscN_4-01-160x90";
        final String pathname="";
        testPNG01Impl(pathname, basename, PixelFormat.BGRA8888, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }
    @Test
    public void testPNGRead14_fromARGB8888() throws InterruptedException, IOException, MalformedURLException {
        final String basename ="test-ntscN_4-01-160x90";
        final String pathname="";
        testPNG01Impl(pathname, basename, PixelFormat.ARGB8888, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }

    private void testPNG01Impl(final String pathname, final String basename, final PixelFormat srcFmt,
                               final int destMinStrideInBytes, final boolean destIsGLOriented)
            throws InterruptedException, IOException, MalformedURLException
    {
        System.err.println("Test01: "+pathname+basename+".png, srcFmt "+srcFmt+", destMinStrideInBytes "+destMinStrideInBytes+", destIsGLOriented "+destIsGLOriented);

        final URLConnection urlConn = IOUtil.getResource(pathname+basename+".png", this.getClass().getClassLoader(), this.getClass());

        final PNGPixelRect image1 = PNGPixelRect.read(urlConn.getInputStream(), srcFmt, false /* directBuffer */, destMinStrideInBytes, false /* isGLOriented */);
        System.err.println("PNGPixelRect - Orig: "+image1);
        System.err.printf("Image Data: %s%n", Bitstream.toHexBinString(true, image1.getPixels(), 0, image1.getPixelformat().comp.bytesPerPixel()));
        TestPixelFormatUtil00NEWT.dumpComponents(image1, 0, 0, 3, 3);

        final PixelFormat[] formats = new PixelFormat[] { PixelFormat.RGBA8888, PixelFormat.ABGR8888, PixelFormat.BGRA8888, PixelFormat.ARGB8888 };
        for(int i=0; i<formats.length; i++) {
            final PixelFormat destFmt = formats[i];
            System.err.println("CONVERT["+i+"]: "+srcFmt+" -> "+destFmt);
            final PixelRectangle imageConv1 = PixelFormatUtil.convert(image1, destFmt, destMinStrideInBytes, destIsGLOriented, false /* nio */);
            System.err.println("PNGPixelRect - Conv1: "+imageConv1);
            System.err.printf("Conv1 Data: %s%n", Bitstream.toHexBinString(true, imageConv1.getPixels(), 0, imageConv1.getPixelformat().comp.bytesPerPixel()));
            TestPixelFormatUtil00NEWT.dumpComponents(imageConv1, 0, 0, 3, 3);
            final PixelRectangle imageConv2 = PixelFormatUtil.convert(imageConv1, image1.getPixelformat(), image1.getStride(), image1.isGLOriented(), false /* nio */);
            System.err.println("PNGPixelRect - Conv2: "+imageConv2);
            System.err.printf("Conv2 Data: %s%n", Bitstream.toHexBinString(true, imageConv2.getPixels(), 0, imageConv2.getPixelformat().comp.bytesPerPixel()));
            TestPixelFormatUtil00NEWT.dumpComponents(imageConv2, 0, 0, 3, 3);
            Assert.assertEquals(image1.getPixels(), imageConv2.getPixels());
        }
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestPixelFormatUtil01NEWT.class.getName());
    }
}

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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.PixelFormat;
import javax.media.nativewindow.util.PixelFormatUtil;
import javax.media.nativewindow.util.PixelRectangle;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Testing PixelFormatUtil's Conversion using synthetic test data
 * including strides, endian-order and all PixelFormat conversions.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPixelFormatUtil00NEWT extends UITestCase {
    @Test
    public void testConversion01_srcS000_BE_TL_destS000_TL() throws InterruptedException, IOException, MalformedURLException {
        testPNG00Impl(0 /* srcMinStrideInBytes */, ByteOrder.BIG_ENDIAN, false /* srcIsGLOriented */,
                      0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }
    @Test
    public void testConversion02_srcS000_LE_TL_destS000_TL() throws InterruptedException, IOException, MalformedURLException {
        testPNG00Impl(0 /* srcMinStrideInBytes */, ByteOrder.LITTLE_ENDIAN, false /* srcIsGLOriented */,
                      0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }
    @Test
    public void testConversion03_srcS000_BE_TL_destS259_TL() throws InterruptedException, IOException, MalformedURLException {
        testPNG00Impl(0 /* srcMinStrideInBytes */, ByteOrder.BIG_ENDIAN, false /* srcIsGLOriented */,
                      259 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }
    @Test
    public void testConversion04_srcS259_BE_TL_destS259_TL() throws InterruptedException, IOException, MalformedURLException {
        testPNG00Impl(259 /* srcMinStrideInBytes */, ByteOrder.BIG_ENDIAN, false /* srcIsGLOriented */,
                      259 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }
    @Test
    public void testConversion05_srcS301_BE_TL_destS259_TL() throws InterruptedException, IOException, MalformedURLException {
        testPNG00Impl(301 /* srcMinStrideInBytes */, ByteOrder.BIG_ENDIAN, false /* srcIsGLOriented */,
                      259 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }

    static final byte red___val = (byte)0x01;
    static final byte green_val = (byte)0x02;
    static final byte blue__val = (byte)0x03;
    static final byte alpha_val = (byte)0x04;
    static final byte undef_val = (byte)0xff;

    static final void getComponents(final int srcComps, final PixelFormat fmt, final byte[] components) {
        final byte b1, b2, b3, b4;
        if( 1 == srcComps ) {
            // LUM -> Fmt Conversion
            switch(fmt) {
                case LUMINANCE:
                    b1 = red___val;
                    b2 = undef_val;
                    b3 = undef_val;
                    b4 = undef_val;
                    break;
                case RGB888:
                    b1 = red___val;
                    b2 = red___val;
                    b3 = red___val;
                    b4 = undef_val;
                    break;
                case BGR888:
                    b1 = red___val;
                    b2 = red___val;
                    b3 = red___val;
                    b4 = undef_val;
                    break;
                case RGBA8888:
                    b1 = red___val;
                    b2 = red___val;
                    b3 = red___val;
                    b4 = undef_val;
                    break;
                case ABGR8888:
                    b1 = undef_val;
                    b2 = red___val;
                    b3 = red___val;
                    b4 = red___val;
                    break;
                case BGRA8888:
                    b1 = red___val;
                    b2 = red___val;
                    b3 = red___val;
                    b4 = undef_val;
                    break;
                case ARGB8888:
                    b1 = undef_val;
                    b2 = red___val;
                    b3 = red___val;
                    b4 = red___val;
                    break;
                default:
                    throw new InternalError("Unhandled format "+fmt);
            }
        } else {
            // 1:1 values
            switch(fmt) {
                case LUMINANCE:
                    if( srcComps > 1 ) {
                        b1 = ( red___val + green_val+ blue__val ) / 3;
                        b2 = undef_val;
                        b3 = undef_val;
                        b4 = undef_val;
                    } else {
                        b1 = red___val;
                        b2 = undef_val;
                        b3 = undef_val;
                        b4 = undef_val;
                    }
                    break;
                case RGB888:
                    b1 = red___val;
                    b2 = green_val;
                    b3 = blue__val;
                    b4 = undef_val;
                    break;
                case BGR888:
                    b1 = blue__val;
                    b2 = green_val;
                    b3 = red___val;
                    b4 = undef_val;
                    break;
                case RGBA8888:
                    b1 = red___val;
                    b2 = green_val;
                    b3 = blue__val;
                    b4 = srcComps > 3 ? alpha_val : undef_val;
                    break;
                case ABGR8888:
                    b1 = srcComps > 3 ? alpha_val : undef_val;
                    b2 = blue__val;
                    b3 = green_val;
                    b4 = red___val;
                    break;
                case BGRA8888:
                    b1 = blue__val;
                    b2 = green_val;
                    b3 = red___val;
                    b4 = srcComps > 3 ? alpha_val : undef_val;
                    break;
                case ARGB8888:
                    b1 = srcComps > 3 ? alpha_val : undef_val;
                    b2 = red___val;
                    b3 = green_val;
                    b4 = blue__val;
                    break;
                default:
                    throw new InternalError("Unhandled format "+fmt);
            }
        }
        components[0] = b1;
        components[1] = b2;
        components[2] = b3;
        components[3] = b4;
    }
    private void testPNG00Impl(final int srcMinStrideInBytes, final ByteOrder srcByteOrder, final boolean srcIsGLOriented,
                               final int destMinStrideInBytes, final boolean destIsGLOriented)
            throws InterruptedException, IOException, MalformedURLException
    {
        System.err.println("Test00: srcMinStrideInBytes "+srcMinStrideInBytes+", srcByteOrder "+srcByteOrder+", srcIsGLOriented "+srcIsGLOriented+
                           ", destMinStrideInBytes "+destMinStrideInBytes+", destIsGLOriented "+destIsGLOriented);

        final PixelFormat[] formats = PixelFormat.values();
        final int width  = 64, height = 64;

        for(int i=0; i<formats.length; i++) {
            final PixelFormat srcFmt = formats[i];
            final int srcBpp = srcFmt.bytesPerPixel();
            final int srcStrideBytes = Math.max(srcMinStrideInBytes, width*srcBpp);
            final ByteBuffer srcPixels = ByteBuffer.allocate(height*srcStrideBytes).order(srcByteOrder);
            final byte[] srcComponents = new byte[4];
            getComponents(srcFmt.componentCount, srcFmt, srcComponents);
            for(int y=0; y<height; y++) {
                int o = y*srcStrideBytes;
                for(int x=0; x<width; x++) {
                    switch(srcFmt) {
                        case LUMINANCE:
                            srcPixels.put(o++, srcComponents[0]);
                            break;
                        case RGB888:
                        case BGR888:
                            srcPixels.put(o++, srcComponents[0]);
                            srcPixels.put(o++, srcComponents[1]);
                            srcPixels.put(o++, srcComponents[2]);
                            break;
                        case RGBA8888:
                        case ABGR8888:
                        case BGRA8888:
                        case ARGB8888:
                            srcPixels.put(o++, srcComponents[0]);
                            srcPixels.put(o++, srcComponents[1]);
                            srcPixels.put(o++, srcComponents[2]);
                            srcPixels.put(o++, srcComponents[3]);
                            break;
                        default:
                            throw new InternalError("Unhandled format "+srcFmt);
                    }
                }
            }
            final PixelRectangle imageSrc = new PixelRectangle.GenericPixelRect(srcFmt, new Dimension(width, height), srcStrideBytes, srcIsGLOriented, srcPixels);
            System.err.println("CONVERT["+i+"][*]: Image0 - Orig: "+imageSrc);
            testComponents(imageSrc, 0, 0, srcComponents);
            testComponents(imageSrc, width-1, height-1, srcComponents);

            for(int j=0; j<formats.length; j++) {
                final PixelFormat destFmt = formats[j];
                System.err.println("CONVERT["+i+"]["+j+"]: "+srcFmt+" -> "+destFmt);

                final int destStrideBytes = Math.max(destMinStrideInBytes, width*destFmt.bytesPerPixel());
                final byte[] destComponents = new byte[4];
                getComponents(srcFmt.componentCount, destFmt, destComponents);
                final PixelRectangle imageConv1 = PixelFormatUtil.convert32(imageSrc, destFmt, destStrideBytes, destIsGLOriented, false /* nio */);
                System.err.println("CONVERT["+i+"]["+j+"]: Conv1: "+imageConv1);
                testComponents(imageConv1, 0, 0, destComponents);
                testComponents(imageConv1, width-1, height-1, destComponents);
                if( PixelFormat.LUMINANCE != srcFmt && PixelFormat.LUMINANCE == destFmt ) {
                    // Cannot convert: RGB* -> LUM -> RGB*
                    System.err.println("CONVERT["+i+"]["+j+"]: Conv2: Dropped due to RGB* -> LUM");
                } else if( srcFmt.componentCount > destFmt.componentCount ) {
                    // Cannot convert back if: src.componentCount > dest.componentCount
                    System.err.println("CONVERT["+i+"]["+j+"]: Conv2: Dropped due to src.componentCount > dest.componentCount");
                } else {
                    final PixelRectangle imageConv2 = PixelFormatUtil.convert32(imageConv1, imageSrc.getPixelformat(), imageSrc.getStride(), imageSrc.isGLOriented(), false /* nio */);
                    System.err.println("CONVERT["+i+"]["+j+"]: Conv2: "+imageConv2);
                    testComponents(imageConv2, 0, 0, srcComponents);
                    testComponents(imageConv2, width-1, height-1, srcComponents);
                    if( imageSrc.getStride() == imageConv1.getStride() ) {
                        Assert.assertEquals(imageSrc.getPixels(), imageConv2.getPixels());
                    }
                }
            }
        }
    }
    private void dumpComponents(final PixelRectangle image, int x1, int y1, final int w, final int h) {
        if( x1 + w >= image.getSize().getWidth() ) {
            x1 = image.getSize().getWidth() - w;
        }
        if( y1 + h >= image.getSize().getHeight() ) {
            y1 = image.getSize().getHeight() - h;
        }
        System.err.print("PixelsBytes "+x1+"/"+y1+" "+w+"x"+h+":");
        final ByteBuffer bb = image.getPixels();
        final int bpp = image.getPixelformat().bytesPerPixel();
        for(int y = y1; y< y1+h; y++) {
            System.err.printf("%n[%3d][%3d] ", x1, y);
            int o = y * image.getStride()+x1*bpp;
            for(int x = x1; x< x1+w; x++) {
                switch(bpp) {
                    case 1: {
                        final byte a = bb.get(o++);
                        System.err.printf(" 0x%02X", a);
                        }
                        break;
                    case 2: {
                        final byte a = bb.get(o++), b = bb.get(o++);
                        System.err.printf(" 0x%02X%02X", b, a);
                      }
                      break;
                    case 3: {
                        final byte a = bb.get(o++), b = bb.get(o++), c = bb.get(o++);
                        System.err.printf(" 0x%02X%02X%02X", c, b, a);
                      }
                      break;
                    case 4: {
                        final byte a = bb.get(o++), b = bb.get(o++), c = bb.get(o++), d = bb.get(o++);
                        System.err.printf(" 0x%02X%02X%02X%02X", d, c, b, a);
                      }
                      break;
                }
            }
        }
        System.err.println();
    }
    private void testComponents(final PixelRectangle image, final int x, final int y, final byte[] components) {
        dumpComponents(image, x, y, 3, 3);
        final ByteBuffer bb = image.getPixels();
        final int bpp = image.getPixelformat().bytesPerPixel();
        int o = y * image.getStride()+x*bpp;
        switch(bpp) {
            case 1: {
                final byte c1 = bb.get(o++);
                final boolean equal = c1==components[0];
                System.err.printf("Test [%3d][%3d] exp 0x%02X == has 0x%02X : %b%n",
                        x, y, components[0], c1, equal );
                Assert.assertEquals(components[0], c1);
                }
                break;
            case 2: {
                final byte c1 = bb.get(o++), c2 = bb.get(o++);
                final boolean equal = c1==components[0] && c2==components[1];
                System.err.printf("Test [%3d][%3d] exp 0x%02X%02X == has 0x%02X%02X : %b%n",
                        x, components[1], components[0], c2, c1, equal );
                Assert.assertEquals(components[0], c1);
                Assert.assertEquals(components[1], c2);
              }
              break;
            case 3: {
                final byte c1 = bb.get(o++), c2 = bb.get(o++), c3 = bb.get(o++);
                final boolean equal = c1==components[0] && c2==components[1] && c3==components[2];
                System.err.printf("Test [%3d][%3d] exp 0x%02X%02X%02X == has 0x%02X%02X%02X : %b%n",
                        x, y, components[2], components[1], components[0], c3, c2, c1, equal );
                Assert.assertEquals(components[0], c1);
                Assert.assertEquals(components[1], c2);
                Assert.assertEquals(components[2], c3);
              }
              break;
            case 4: {
                final byte c1 = bb.get(o++), c2 = bb.get(o++), c3 = bb.get(o++), c4 = bb.get(o++);
                final boolean equal = c1==components[0] && c2==components[1] && c3==components[2] && c4==components[3];
                System.err.printf("Test [%3d][%3d] exp 0x%02X%02X%02X%02X == has 0x%02X%02X%02X%02X : %b%n",
                        x, y, components[3], components[2], components[1], components[0], c4, c3, c2, c1, equal );
                Assert.assertEquals(components[0], c1);
                Assert.assertEquals(components[1], c2);
                Assert.assertEquals(components[2], c3);
                Assert.assertEquals(components[3], c4);
              }
              break;
        }
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestPixelFormatUtil00NEWT.class.getName());
    }
}

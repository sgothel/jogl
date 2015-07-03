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

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.nativewindow.util.PixelFormatUtil;
import com.jogamp.nativewindow.util.PixelRectangle;
import com.jogamp.nativewindow.util.PixelFormat.CType;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.util.Bitstream;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Testing PixelFormatUtil's Conversion using synthetic test data
 * including strides, endian-order and all PixelFormat conversions.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPixelFormatUtil00NEWT extends UITestCase {
    static final byte undef_val = (byte)0xFF;
    static final PixelFormat.Composition comp_val = PixelFormat.RGBA8888.comp;
    static final float red___valF;
    static final float green_valF;
    static final float blue__valF;
    static final float alpha_valF;
    static final float lum___valF;
    static {
        // Using am equal stepping of 0x30 = 48 between each RGBA and undefined values,
        // dividing 0xff equally by 5 excluding zero.
        final byte red___val = (byte)0x30;
        final byte green_val = (byte)0x60;
        final byte blue__val = (byte)0x90;
        final byte alpha_val = (byte)0xC0;
        red___valF = comp_val.toFloat(red___val, 0, false);
        green_valF = comp_val.toFloat(green_val, 1, false);
        blue__valF = comp_val.toFloat(blue__val, 2, false);
        alpha_valF = comp_val.toFloat(alpha_val, 3, false);
        lum___valF = ( red___valF + green_valF + blue__valF ) / 3f;
    }

    @Test
    public void testConversion00() throws InterruptedException, IOException, MalformedURLException {
        {
            final PixelFormat fmt = PixelFormat.RGBA5551;
            final PixelFormat.Composition comp = fmt.comp;
            System.err.printf("%s, %s:%n", fmt, comp);
            final int u16_alpha = comp.encode4CompI8((byte)comp.fromFloat(red___valF, 0, false),
                                                     (byte)comp.fromFloat(green_valF, 0, false),
                                                     (byte)comp.fromFloat(blue__valF, 0, false),
                                                     (byte)comp.fromFloat(alpha_valF, 0, false));
            final int u16_undef = comp.encode4CompI8((byte)comp.fromFloat(red___valF, 0, false),
                                                     (byte)comp.fromFloat(green_valF, 0, false),
                                                     (byte)comp.fromFloat(blue__valF, 0, false),
                                                     undef_val);
            System.err.printf("    u16_alpha %s%n", Bitstream.toHexBinString(true, u16_alpha, comp.bitsPerPixel()));
            System.err.printf("    u16_undef %s%n", Bitstream.toHexBinString(true, u16_undef, comp.bitsPerPixel()));
            {
                final byte c4NormI8_alpha = (byte)comp.fromFloat(alpha_valF, 0, false);
                final byte c4NormI8_undef = undef_val;
                final int compBitShift = 15;
                final int compMask = 0x1;
                final int v_alpha = ( c4NormI8_alpha & compMask ) << compBitShift ;
                final int v_undef = ( c4NormI8_undef & compMask ) << compBitShift ;
                System.err.printf("    xx_alpha %s%n", Bitstream.toHexBinString(true, v_alpha, comp.bitsPerPixel()));
                System.err.printf("    xx_undef %s%n", Bitstream.toHexBinString(true, v_undef, comp.bitsPerPixel()));
            }
        }
        {
            final int r8 = 0x30;
            final int g8 = 0x60;
            final int b8 = 0x90;
            final int a8 = 0xC0;

            final int l1 = 0x1;
            final int r5 = 0x6;
            final int g6 = 0xC;
            final int b5 = 0x6;

            final PixelFormat rgba8888Fmt = PixelFormat.RGBA8888;
            final PixelFormat.Composition rgba8888Comp = rgba8888Fmt.comp;
            final PixelFormat rgb565Fmt = PixelFormat.RGB565;
            final PixelFormat.Composition rgb565Comp = rgb565Fmt.comp;
            final PixelFormat lumFmt = PixelFormat.LUMINANCE;
            final PixelFormat.Composition lumComp = lumFmt.comp;
            System.err.printf("%s, %s -> %s %s%n", rgb565Fmt, rgb565Comp, lumFmt, lumComp);

            {
                final float r8f = rgba8888Comp.toFloat(r8, 0, false);
                final int r8fi = rgba8888Comp.fromFloat(r8f, 0, false);
                final float g8f = rgba8888Comp.toFloat(g8, 1, false);
                final int g8fi = rgba8888Comp.fromFloat(g8f, 1, false);
                final float b8f = rgba8888Comp.toFloat(b8, 2, false);
                final int b8fi = rgba8888Comp.fromFloat(b8f, 2, false);
                final float a8f = rgba8888Comp.toFloat(a8, 3, false);
                final int a8fi = rgba8888Comp.fromFloat(a8f, 3, false);

                System.err.printf("res00.0.r %s -> %f -> %s%n", Bitstream.toHexBinString(true, r8, 8), r8f, Bitstream.toHexBinString(true, r8fi, 8));
                System.err.printf("res00.0.g %s -> %f -> %s%n", Bitstream.toHexBinString(true, g8, 8), g8f, Bitstream.toHexBinString(true, g8fi, 8));
                System.err.printf("res00.0.b %s -> %f -> %s%n", Bitstream.toHexBinString(true, b8, 8), b8f, Bitstream.toHexBinString(true, b8fi, 8));
                System.err.printf("res00.0.a %s -> %f -> %s%n", Bitstream.toHexBinString(true, a8, 8), a8f, Bitstream.toHexBinString(true, a8fi, 8));
            }
            {
                final float res00_0 = ( red___valF + green_valF + blue__valF ) / 3f;
                final int res00 = rgba8888Comp.fromFloat(res00_0, 0, false);
                System.err.printf("res01.0  ( %f + %f + %f ) / 3f = %f -> %s%n",
                        red___valF, green_valF, blue__valF, res00_0, Bitstream.toHexBinString(true, res00, 8));
            }
            {
                final float res00_0 = ( red___valF + green_valF + blue__valF ) / 3f;
                final int res00 = lumComp.fromFloat(res00_0, 0, false);
                System.err.printf("res02.1  ( %f + %f + %f ) / 3f = %f -> %s%n",
                        red___valF, green_valF, blue__valF, res00_0, Bitstream.toHexBinString(true, res00, 8));
            }
            {
                // sourceNorm static -> lum
                final int rl1 = lumComp.fromFloat(red___valF, 0, false);
                final int gl1 = lumComp.fromFloat(green_valF, 0, false);
                final int bl1 = lumComp.fromFloat(blue__valF, 0, false);
                final float rl2 = lumComp.toFloat(rl1, 0, false);
                final float gl2 = lumComp.toFloat(gl1, 0, false);
                final float bl2 = lumComp.toFloat(bl1, 0, false);
                System.err.printf("res20.l1  ( %s + %s + %s )%n",
                        Bitstream.toHexBinString(true, rl1, 8),
                        Bitstream.toHexBinString(true, gl1, 8),
                        Bitstream.toHexBinString(true, bl1, 8));
                System.err.printf("res20.l2 ( %f + %f + %f )%n", rl2, gl2, bl2);
                final float res02_l2_0 = ( rl2 + gl2 + bl2 ) / 3f;
                final int res02_l2_x = lumComp.fromFloat(res02_l2_0, 0, false);
                System.err.printf("res20.l3 ( %f + %f + %f ) / 3f = %f -> %s%n",
                        rl2, gl2, bl2, res02_l2_0, Bitstream.toHexBinString(true, res02_l2_x, 8));

                // rescale lum -> rgb565
                final int r_1 = rgb565Comp.fromFloat(rl2, 0, false);
                final int g_1 = rgb565Comp.fromFloat(gl2, 1, false);
                final int b_1 = rgb565Comp.fromFloat(bl2, 2, false);
                final float r_2 = rgb565Comp.toFloat(r_1, 0, false);
                final float g_2 = rgb565Comp.toFloat(g_1, 1, false);
                final float b_2 = rgb565Comp.toFloat(b_1, 2, false);
                System.err.printf("res20._1  ( %s + %s + %s )%n",
                        Bitstream.toHexBinString(true, r_1, 8),
                        Bitstream.toHexBinString(true, g_1, 8),
                        Bitstream.toHexBinString(true, b_1, 8));
                System.err.printf("res20._2 ( %f + %f + %f )%n", r_2, g_2, b_2);
                final float res02__3_0 = ( r_2 + g_2 + b_2 ) / 3f;
                final int res02__3_x = lumComp.fromFloat(res02__3_0, 0, false);
                System.err.printf("res20._3 ( %f + %f + %f ) / 3f = %f -> %s%n",
                        r_2, g_2, b_2, res02__3_0, Bitstream.toHexBinString(true, res02__3_x, 8));
            }
            {
                // sourceNorm static -> lum
                // rescale lum -> rgb565
                final float rF = rgb565Comp.toFloat(rescaleComp(lumComp, 0, rgb565Comp, 0, red___valF), 0, false);
                final float gF = rgb565Comp.toFloat(rescaleComp(lumComp, 0, rgb565Comp, 1, green_valF), 1, false);
                final float bF = rgb565Comp.toFloat(rescaleComp(lumComp, 0, rgb565Comp, 2, blue__valF), 2, false);
                final float res01_0 = ( rF + gF + bF ) / 3f;
                final int res01 = lumComp.fromFloat(res01_0, 0, false);
                System.err.printf("res30.xx  ( %f + %f + %f ) / 3f = %f -> %s%n",
                        rF, gF, bF, res01_0, Bitstream.toHexBinString(true, res01, 8));
            }
            {
                final float rF = rgb565Comp.toFloat(r5, 0, false);
                final float gF = rgb565Comp.toFloat(g6, 1, false);
                final float bF = rgb565Comp.toFloat(b5, 2, false);

                final float lF = ( rF + gF + bF ) / 3f;
                final int res00 = lumComp.fromFloat(lF, 0, false);

                System.err.printf("res40  ( %f + %f + %f ) / 3f = %s%n",
                        rF, gF, bF, Bitstream.toHexBinString(true, res00, 8));
            }
        }

    }

    @Test
    public void testConversion01_srcS000_BE_TL_destS000_TL() throws InterruptedException, IOException, MalformedURLException {
        testConversionImpl(0 /* srcMinStrideInBytes */, ByteOrder.BIG_ENDIAN, false /* srcIsGLOriented */,
                           0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }
    @Test
    public void testConversion02_srcS000_LE_TL_destS000_TL() throws InterruptedException, IOException, MalformedURLException {
        testConversionImpl(0 /* srcMinStrideInBytes */, ByteOrder.LITTLE_ENDIAN, false /* srcIsGLOriented */,
                           0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }
    @Test
    public void testConversion03_srcS000_BE_TL_destS259_TL() throws InterruptedException, IOException, MalformedURLException {
        testConversionImpl(0 /* srcMinStrideInBytes */, ByteOrder.BIG_ENDIAN, false /* srcIsGLOriented */,
                           259 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }
    @Test
    public void testConversion04_srcS259_BE_TL_destS259_TL() throws InterruptedException, IOException, MalformedURLException {
        testConversionImpl(259 /* srcMinStrideInBytes */, ByteOrder.BIG_ENDIAN, false /* srcIsGLOriented */,
                           259 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }
    @Test
    public void testConversion05_srcS301_BE_TL_destS259_TL() throws InterruptedException, IOException, MalformedURLException {
        testConversionImpl(301 /* srcMinStrideInBytes */, ByteOrder.BIG_ENDIAN, false /* srcIsGLOriented */,
                           259 /* destMinStrideInBytes */, false /* destIsGLOriented */);
    }

    /**
     * Note: Fixes bit-rounding errors, i.e. RGBA5551: A 0.6f -> 0x01 -> 1f ... -> RGBA8888: A 0xff
     */
    static final float sourceNorm(final PixelFormat.Composition srcComp, final int sIdx, final float f) {
        if( sIdx >= 0 && sIdx < srcComp.componentCount() ) {
            return srcComp.toFloat(srcComp.fromFloat(f, sIdx, false), sIdx, false);
        } else {
            return 0f;
        }
    }
    static final byte rescaleComp(final PixelFormat.Composition srcComp, final int sIdx,
                                  final PixelFormat.Composition dstComp, final int dIdx, final float f) {
        if( dIdx >= 0 && dIdx < dstComp.componentCount() ) {
            return (byte)dstComp.fromFloat(sourceNorm(srcComp, sIdx, f), dIdx, false);
        } else {
            return (byte)0;
        }
    }
    static final void getComponentData(final PixelFormat srcFmt, final PixelFormat dstFmt, final byte[] components) {
        final PixelFormat.Composition srcComp = srcFmt.comp;
        final PixelFormat.Composition dstComp = dstFmt.comp;
        final byte b1, b2, b3, b4;
        int u16;
        if( PixelFormat.LUMINANCE == srcFmt ) {
            // LUM -> Fmt Conversion
            switch(dstFmt) {
                case LUMINANCE:
                    b1 = rescaleComp(srcComp, 0, dstComp, 0, lum___valF);
                    b2 = undef_val;
                    b3 = undef_val;
                    b4 = undef_val;
                    break;
                case RGB565:
                case BGR565:
                    u16 = dstComp.encode3CompI8(
                            rescaleComp(srcComp, 0, dstComp, 0, lum___valF),
                            rescaleComp(srcComp, 0, dstComp, 1, lum___valF),
                            rescaleComp(srcComp, 0, dstComp, 2, lum___valF));
                    b1 = (byte)( u16 & 0xff );
                    b2 = (byte)( ( u16 >>> 8 ) & 0xff );
                    b3 = undef_val;
                    b4 = undef_val;
                    break;
                case RGBA5551:
                    u16 = dstComp.encode4CompI8(
                            rescaleComp(srcComp, 0, dstComp, 0, lum___valF),
                            rescaleComp(srcComp, 0, dstComp, 1, lum___valF),
                            rescaleComp(srcComp, 0, dstComp, 2, lum___valF),
                            undef_val);
                    b1 = (byte)( u16 & 0xff );
                    b2 = (byte)( ( u16 >>> 8 ) & 0xff );
                    b3 = undef_val;
                    b4 = undef_val;
                    break;
                case ABGR1555:
                    u16 = dstComp.encode4CompI8(
                            undef_val,
                            rescaleComp(srcComp, 0, dstComp, 0, lum___valF),
                            rescaleComp(srcComp, 0, dstComp, 1, lum___valF),
                            rescaleComp(srcComp, 0, dstComp, 2, lum___valF) );
                    b1 = (byte)( u16 & 0xff );
                    b2 = (byte)( ( u16 >>> 8 ) & 0xff );
                    b3 = undef_val;
                    b4 = undef_val;
                    break;
                case BGRx8888:
                case RGBx8888:
                case RGB888:
                case BGR888:
                case RGBA8888:
                    b1 = rescaleComp(srcComp, 0, dstComp, 0, lum___valF);
                    b2 = rescaleComp(srcComp, 0, dstComp, 1, lum___valF);
                    b3 = rescaleComp(srcComp, 0, dstComp, 2, lum___valF);
                    b4 = undef_val;
                    break;
                case ABGR8888:
                case ARGB8888:
                    b1 = undef_val;
                    b2 = rescaleComp(srcComp, 0, dstComp, 1, lum___valF);
                    b3 = rescaleComp(srcComp, 0, dstComp, 2, lum___valF);
                    b4 = rescaleComp(srcComp, 0, dstComp, 3, lum___valF);
                    break;
                case BGRA8888:
                    b1 = rescaleComp(srcComp, 0, dstComp, 0, lum___valF);
                    b2 = rescaleComp(srcComp, 0, dstComp, 1, lum___valF);
                    b3 = rescaleComp(srcComp, 0, dstComp, 2, lum___valF);
                    b4 = undef_val;
                    break;
                default:
                    throw new InternalError("Unhandled format "+dstFmt);
            }
        } else {
            final int srcIdxR = srcComp.find(CType.R);
            final int srcIdxG = srcComp.find(CType.G);
            final int srcIdxB = srcComp.find(CType.B);
            final int srcIdxA = srcComp.find(CType.A);
            final boolean srcHasAlpha = 0 <= srcIdxA;
            final boolean srcHasRGB = 0 <= srcIdxR && 0 <= srcIdxG && 0 <= srcIdxB;
            // 1:1 values
            switch(dstFmt) {
                case LUMINANCE:
                    if( srcHasRGB ) {
                        final float rF = sourceNorm(srcComp, srcIdxR, red___valF);
                        final float gF = sourceNorm(srcComp, srcIdxG, green_valF);
                        final float bF = sourceNorm(srcComp, srcIdxB, blue__valF);
                        b1 = (byte)dstComp.fromFloat( ( rF + gF + bF ) / 3f, 0, false);
                        b2 = undef_val;
                        b3 = undef_val;
                        b4 = undef_val;
                    } else {
                        b1 = rescaleComp(srcComp, 0, dstComp, 0, red___valF);
                        b2 = undef_val;
                        b3 = undef_val;
                        b4 = undef_val;
                    }
                    break;
                case RGB565:
                    u16 = dstComp.encode3CompI8(
                            rescaleComp(srcComp, srcIdxR, dstComp, 0, red___valF),
                            rescaleComp(srcComp, srcIdxG, dstComp, 1, green_valF),
                            rescaleComp(srcComp, srcIdxB, dstComp, 2, blue__valF));
                    b1 = (byte)( u16 & 0xff );
                    b2 = (byte)( ( u16 >>> 8 ) & 0xff );
                    b3 = undef_val;
                    b4 = undef_val;
                    break;
                case BGR565:
                    u16 = dstComp.encode3CompI8(
                            rescaleComp(srcComp, srcIdxB, dstComp, 0, blue__valF),
                            rescaleComp(srcComp, srcIdxG, dstComp, 1, green_valF),
                            rescaleComp(srcComp, srcIdxR, dstComp, 2, red___valF));
                    b1 = (byte)( u16 & 0xff );
                    b2 = (byte)( ( u16 >>> 8 ) & 0xff );
                    b3 = undef_val;
                    b4 = undef_val;
                    break;
                case RGBA5551:
                    u16 = dstComp.encode4CompI8(
                            rescaleComp(srcComp, srcIdxR, dstComp, 0, red___valF),
                            rescaleComp(srcComp, srcIdxG, dstComp, 1, green_valF),
                            rescaleComp(srcComp, srcIdxB, dstComp, 2, blue__valF),
                            srcHasAlpha ? rescaleComp(srcComp, srcIdxA, dstComp, 3, alpha_valF) : undef_val);
                    b1 = (byte)( u16 & 0xff );
                    b2 = (byte)( ( u16 >>> 8 ) & 0xff );
                    b3 = undef_val;
                    b4 = undef_val;
                    break;
                case ABGR1555:
                    u16 = dstComp.encode4CompI8(
                            srcHasAlpha ? rescaleComp(srcComp, srcIdxA, dstComp, 0, alpha_valF) : undef_val,
                            rescaleComp(srcComp, srcIdxB, dstComp, 1, blue__valF),
                            rescaleComp(srcComp, srcIdxG, dstComp, 2, green_valF),
                            rescaleComp(srcComp, srcIdxR, dstComp, 3, red___valF) );
                    b1 = (byte)( u16 & 0xff );
                    b2 = (byte)( ( u16 >>> 8 ) & 0xff );
                    b3 = undef_val;
                    b4 = undef_val;
                    break;
                case RGBx8888:
                case RGB888:
                    b1 = rescaleComp(srcComp, srcIdxR, dstComp, 0, red___valF);
                    b2 = rescaleComp(srcComp, srcIdxG, dstComp, 1, green_valF);
                    b3 = rescaleComp(srcComp, srcIdxB, dstComp, 2, blue__valF);
                    b4 = undef_val;
                    break;
                case BGRx8888:
                case BGR888:
                    b1 = rescaleComp(srcComp, srcIdxB, dstComp, 0, blue__valF);
                    b2 = rescaleComp(srcComp, srcIdxG, dstComp, 1, green_valF);
                    b3 = rescaleComp(srcComp, srcIdxR, dstComp, 2, red___valF);
                    b4 = undef_val;
                    break;
                case RGBA8888:
                    b1 = rescaleComp(srcComp, srcIdxR, dstComp, 0, red___valF);
                    b2 = rescaleComp(srcComp, srcIdxG, dstComp, 1, green_valF);
                    b3 = rescaleComp(srcComp, srcIdxB, dstComp, 2, blue__valF);
                    if( srcHasAlpha ) {
                        b4 = rescaleComp(srcComp, srcIdxA, dstComp, 3, alpha_valF);
                    } else {
                        b4 = undef_val;
                    }
                    break;
                case ABGR8888:
                    if( srcHasAlpha ) {
                        b1 = rescaleComp(srcComp, srcIdxA, dstComp, 0, alpha_valF);
                    } else {
                        b1 = undef_val;
                    }
                    b2 = rescaleComp(srcComp, srcIdxB, dstComp, 1, blue__valF);
                    b3 = rescaleComp(srcComp, srcIdxG, dstComp, 2, green_valF);
                    b4 = rescaleComp(srcComp, srcIdxR, dstComp, 3, red___valF);
                    break;
                case BGRA8888:
                    b1 = rescaleComp(srcComp, srcIdxB, dstComp, 0, blue__valF);
                    b2 = rescaleComp(srcComp, srcIdxG, dstComp, 1, green_valF);
                    b3 = rescaleComp(srcComp, srcIdxR, dstComp, 2, red___valF);
                    if( srcHasAlpha ) {
                        b4 = rescaleComp(srcComp, srcIdxA, dstComp, 3, alpha_valF);
                    } else {
                        b4 = undef_val;
                    }
                    break;
                case ARGB8888:
                    if( srcHasAlpha ) {
                        b1 = rescaleComp(srcComp, srcIdxA, dstComp, 0, alpha_valF);
                    } else {
                        b1 = undef_val;
                    }
                    b2 = rescaleComp(srcComp, srcIdxR, dstComp, 1, red___valF);
                    b3 = rescaleComp(srcComp, srcIdxG, dstComp, 2, green_valF);
                    b4 = rescaleComp(srcComp, srcIdxB, dstComp, 3, blue__valF);
                    break;
                default:
                    throw new InternalError("Unhandled format "+dstFmt);
            }
        }
        components[0] = b1;
        components[1] = b2;
        components[2] = b3;
        components[3] = b4;
    }
    private void testConversionImpl(final int srcMinStrideInBytes, final ByteOrder srcByteOrder, final boolean srcIsGLOriented,
                               final int destMinStrideInBytes, final boolean destIsGLOriented)
            throws InterruptedException, IOException, MalformedURLException
    {
        System.err.println("Test00: srcMinStrideInBytes "+srcMinStrideInBytes+", srcByteOrder "+srcByteOrder+", srcIsGLOriented "+srcIsGLOriented+
                           ", destMinStrideInBytes "+destMinStrideInBytes+", destIsGLOriented "+destIsGLOriented);

        // final PixelFormat[] srcFormats = { PixelFormat.LUMINANCE };
        // final PixelFormat[] dstFormats = { PixelFormat.RGBx8888 };
        // final PixelFormat[] dstFormats = { PixelFormat.RGB5551 };
        // final PixelFormat[] dstFormats = { PixelFormat.RGB888 };
        // final PixelFormat[] srcFormats = { PixelFormat.RGB888 };
        // final PixelFormat[] dstFormats = { PixelFormat.RGB565 };
        final PixelFormat[] srcFormats = PixelFormat.values();
        final PixelFormat[] dstFormats = PixelFormat.values();
        final int width  = 64, height = 64;

        for(int i=0; i<srcFormats.length; i++) {
            final PixelFormat srcFmt = srcFormats[i];
            final int srcBpp = srcFmt.comp.bytesPerPixel();
            final int srcStrideBytes = Math.max(srcMinStrideInBytes, width*srcBpp);
            final ByteBuffer srcPixels = ByteBuffer.allocate(height*srcStrideBytes).order(srcByteOrder);
            final byte[] srcData = new byte[4];
            getComponentData(srcFmt, srcFmt, srcData);
            for(int y=0; y<height; y++) {
                int o = y*srcStrideBytes;
                for(int x=0; x<width; x++) {
                    switch(srcFmt) {
                        case LUMINANCE:
                            srcPixels.put(o++, srcData[0]);
                            break;
                        case BGR565:
                        case RGB565:
                        case ABGR1555:
                        case RGBA5551:
                            srcPixels.put(o++, srcData[0]);
                            srcPixels.put(o++, srcData[1]);
                            break;
                        case RGB888:
                        case BGR888:
                            srcPixels.put(o++, srcData[0]);
                            srcPixels.put(o++, srcData[1]);
                            srcPixels.put(o++, srcData[2]);
                            break;
                        case RGBx8888:
                        case BGRx8888:
                        case RGBA8888:
                        case ABGR8888:
                        case BGRA8888:
                        case ARGB8888:
                            srcPixels.put(o++, srcData[0]);
                            srcPixels.put(o++, srcData[1]);
                            srcPixels.put(o++, srcData[2]);
                            srcPixels.put(o++, srcData[3]);
                            break;
                        default:
                            throw new InternalError("Unhandled format "+srcFmt);
                    }
                }
            }
            final PixelRectangle imageSrc = new PixelRectangle.GenericPixelRect(srcFmt, new Dimension(width, height),
                                                                                srcStrideBytes, srcIsGLOriented, srcPixels);

            System.err.println("CONVERT["+i+"][*]: Image0 - Orig: "+imageSrc);
            System.err.printf("Source %s, %s%n", srcFmt, srcFmt.comp);
            System.err.printf("Source Data: %s%n", Bitstream.toHexBinString(true, srcData, 0, srcFmt.comp.bytesPerPixel()));
            testComponents(imageSrc, 0, 0, srcData, 0);
            testComponents(imageSrc, width-1, height-1, srcData, 0);

            final int maxDelta = 12;

            for(int j=0; j<dstFormats.length; j++) {
                final PixelFormat destFmt = dstFormats[j];
                System.err.println("CONVERT["+i+"]["+j+"]: "+srcFmt+" -> "+destFmt);
                final int destStrideBytes = Math.max(destMinStrideInBytes, width*destFmt.comp.bytesPerPixel());
                final byte[] destComponents = new byte[4];
                getComponentData(srcFmt, destFmt, destComponents);
                System.err.printf("Source %s, %s%n", srcFmt, srcFmt.comp);
                System.err.printf("Source Data: %s%n", Bitstream.toHexBinString(true, srcData, 0, srcFmt.comp.bytesPerPixel()));
                System.err.printf("Dest %s, %s%n", destFmt, destFmt.comp);
                System.err.printf("Dest Data: %s%n", Bitstream.toHexBinString(true, destComponents, 0, destFmt.comp.bytesPerPixel()));
                final PixelRectangle imageConv1 = PixelFormatUtil.convert(imageSrc, destFmt, destStrideBytes, destIsGLOriented, false /* nio */);
                System.err.println("CONVERT["+i+"]["+j+"]: Conv1: "+imageConv1+", maxDelta "+maxDelta);
                System.err.printf("Conv1 Data: %s%n", Bitstream.toHexBinString(true, imageConv1.getPixels(), 0, destFmt.comp.bytesPerPixel()));
                testComponents(imageConv1, 0, 0, destComponents, maxDelta);
                testComponents(imageConv1, width-1, height-1, destComponents, maxDelta);
                if( PixelFormat.LUMINANCE != srcFmt && PixelFormat.LUMINANCE == destFmt ) {
                    // Cannot convert: RGB* -> LUM -> RGB*
                    System.err.println("CONVERT["+i+"]["+j+"]: Conv2: Dropped due to RGB* -> LUM");
                } else if( srcFmt.comp.componentCount() > destFmt.comp.componentCount() ) {
                    // Cannot convert back if: src.componentCount > dest.componentCount
                    System.err.println("CONVERT["+i+"]["+j+"]: Conv2: Dropped due to src.componentCount > dest.componentCount");
                } else {
                    final PixelRectangle imageConv2 = PixelFormatUtil.convert(imageConv1, imageSrc.getPixelformat(), imageSrc.getStride(), imageSrc.isGLOriented(), false /* nio */);
                    System.err.println("CONVERT["+i+"]["+j+"]: Conv2: "+imageConv2+", maxDelta "+maxDelta);
                    System.err.printf("Conv2 Data: %s%n", Bitstream.toHexBinString(true, imageConv2.getPixels(), 0, srcFmt.comp.bytesPerPixel()));
                    final byte[] destReComponents = new byte[4];
                    getComponentData(destFmt, srcFmt, destReComponents);
                    System.err.printf("DestRe Data: %s%n", Bitstream.toHexBinString(true, destReComponents, 0, srcFmt.comp.bytesPerPixel()));
                    testComponents(imageConv2, 0, 0, destReComponents, maxDelta);
                    testComponents(imageConv2, width-1, height-1, destReComponents, maxDelta);
                    /**
                     * Due to 'dead' components or value range re-scale,
                     * identity comparison on byte level is not correct.
                     *
                    if( imageSrc.getStride() == imageConv1.getStride() ) {
                        Assert.assertEquals(imageSrc.getPixels(), imageConv2.getPixels());
                    }
                     */
                }
            }
        }
    }
    static void dumpComponents(final PixelRectangle image, int x1, int y1, final int w, final int h) {
        if( x1 + w >= image.getSize().getWidth() ) {
            x1 = image.getSize().getWidth() - w;
        }
        if( y1 + h >= image.getSize().getHeight() ) {
            y1 = image.getSize().getHeight() - h;
        }
        System.err.print("PixelsBytes "+x1+"/"+y1+" "+w+"x"+h+":");
        final ByteBuffer bb = image.getPixels();
        final int bpp = image.getPixelformat().comp.bytesPerPixel();
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

    static final void assertEquals(final int a, final int b, final int maxDelta) {
        final int d = Math.abs( a - b );
        Assert.assertTrue(String.format("Not equal: abs(%s - %s) = %d, > %d maxDelta",
                            Bitstream.toHexBinString(true, a, 8), Bitstream.toHexBinString(true, b, 8), d, maxDelta),
                          d <= maxDelta);
    }
    static final boolean equals(final int a, final int b, final int maxDelta) {
        final int d = Math.abs( a - b );
        return d <= maxDelta;
    }

    /**
     *
     * @param image actual data
     * @param x position in actual data
     * @param y position in actual data
     * @param expData expected data
     * @param maxDelta the maximum delta between expected {@code components} and actual {@code image} data
     */
    static void testComponents(final PixelRectangle image, final int x, final int y, final byte[] expData, final int maxDelta) {
        dumpComponents(image, x, y, 3, 3);
        final PixelFormat.Composition imgComp = image.getPixelformat().comp;
        final ByteBuffer bb = image.getPixels();
        final int bytesPerPixel = imgComp.bytesPerPixel();
        final int compCount = imgComp.componentCount();
        final int[] compBitCount = imgComp.componentBitCount();

        final int srcPixOffset = y * image.getStride()+x*bytesPerPixel;
        final int bbPos = bb.position();
        bb.position(bbPos+srcPixOffset);

        final long srcPix64 = PixelFormatUtil.getShiftedI64(imgComp.bytesPerPixel(), bb, true);
        final int[] srcComponents = new int[compCount];
        final long expPix64 = PixelFormatUtil.getShiftedI64(imgComp.bytesPerPixel(), expData, 0);
        final int[] expComponents = new int[compCount];
        boolean equal = true;
        for(int i=0; i<compCount; i++) {
            srcComponents[i] = imgComp.decodeSingleI64(srcPix64, i);
            expComponents[i] = imgComp.decodeSingleI64(expPix64, i);
            equal = equal && equals(srcComponents[i], expComponents[i], maxDelta);
        }
        System.err.printf("Test [%3d][%3d] exp ", x, y);
        for(int i=0; i<compCount; i++) { System.err.printf("%s ", Bitstream.toHexBinString(true, expComponents[i], compBitCount[i])); }
        System.err.printf("==%nTest [%3d][%3d] has ", x, y);
        for(int i=0; i<compCount; i++) { System.err.printf("%s ", Bitstream.toHexBinString(true, srcComponents[i], compBitCount[i])); }
        System.err.printf(": equal %b%n%n", equal);
        for(int i=0; i<compCount; i++) {
            assertEquals(srcComponents[i], expComponents[i], maxDelta);
        }

        bb.position(bbPos);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestPixelFormatUtil00NEWT.class.getName());
    }
}

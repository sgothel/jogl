/**
 * Copyright (c) 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.nativewindow.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.Bitstream;

/**
 * Pixel Rectangle Utilities.
 * <p>
 * All conversion methods are endian independent.
 * </p>
 */
public class PixelFormatUtil {
    private static boolean DEBUG = false;

    public static class ComponentMap {
        /**
         * Contains the source index for each destination index,
         * length is {@link Composition#componentCount()} of destination.
         */
        final int[] dst2src;
        /**
         * Contains the destination index for each source index,
         * length is {@link Composition#componentCount()} of source.
         */
        final int[] src2dst;

        /**
         * Contains the source index of RGBA components.
         */
        final int[] srcRGBA;
        final boolean hasSrcRGB;

        public ComponentMap(final PixelFormat.Composition src, final PixelFormat.Composition dst) {
            final int sCompCount = src.componentCount();
            final int dCompCount = dst.componentCount();
            final PixelFormat.CType[] sCompOrder = src.componentOrder();
            final PixelFormat.CType[] dCompOrder = dst.componentOrder();

            dst2src = new int[dCompCount];
            for(int dIdx=0; dIdx<dCompCount; dIdx++) {
                dst2src[dIdx] = PixelFormatUtil.find(dCompOrder[dIdx], sCompOrder, true);
            }
            src2dst = new int[sCompCount];
            for(int sIdx=0; sIdx<sCompCount; sIdx++) {
                src2dst[sIdx] = PixelFormatUtil.find(sCompOrder[sIdx], dCompOrder, true);
            }
            srcRGBA = new int[4];
            srcRGBA[0] = PixelFormatUtil.find(PixelFormat.CType.R, sCompOrder, false);
            srcRGBA[1] = PixelFormatUtil.find(PixelFormat.CType.G, sCompOrder, false);
            srcRGBA[2] = PixelFormatUtil.find(PixelFormat.CType.B, sCompOrder, false);
            srcRGBA[3] = PixelFormatUtil.find(PixelFormat.CType.A, sCompOrder, false);
            hasSrcRGB = 0 <= srcRGBA[0] && 0 <= srcRGBA[1] && 0 <= srcRGBA[2];
        }
    }

    public static final int find(final PixelFormat.CType s,
                                 final PixelFormat.CType[] pool, final boolean mapRGB2Y) {
        int i=pool.length-1;
        while( i >= 0 && pool[i] != s) { i--; }

        if( 0 > i && mapRGB2Y && 1 == pool.length && pool[0] == PixelFormat.CType.Y &&
            ( PixelFormat.CType.R == s ||
              PixelFormat.CType.G == s ||
              PixelFormat.CType.B == s ) )
        {
            // Special case, fallback for RGB mapping -> LUMINANCE/Y
            return 0;
        } else {
            return i;
        }
    }

    /**
     * Returns shifted bytes from the given {@code data} at given {@code offset}
     * of maximal 4 {@code bytesPerPixel}.
     * @param bytesPerPixel number of bytes per pixel to fetch, a maximum of 4 are allowed
     * @param data byte buffer covering complete pixel at position {@code offset}
     * @param offset byte offset of pixel {@code data} start
     * @return the shifted 32bit integer value of the pixel
     */
    public static int getShiftedI32(final int bytesPerPixel, final byte[] data, final int offset) {
        if( bytesPerPixel <= 4 ) {
            int shiftedI32 = 0;
            for(int i=0; i<bytesPerPixel; i++) {
                shiftedI32 |= ( 0xff & data[offset+i] ) << 8*i;
            }
            return shiftedI32;
        } else {
            throw new UnsupportedOperationException(bytesPerPixel+" bytesPerPixel too big, i.e. > 4");
        }
    }
    /**
     * Returns shifted bytes from the given {@code data} at given {@code offset}
     * of maximal 8 {@code bytesPerPixel}.
     * @param bytesPerPixel number of bytes per pixel to fetch, a maximum of 4 are allowed
     * @param data byte buffer covering complete pixel at position {@code offset}
     * @param offset byte offset of pixel {@code data} start
     * @return the shifted 64bit integer value of the pixel
     */
    public static long getShiftedI64(final int bytesPerPixel, final byte[] data, final int offset) {
        if( bytesPerPixel <= 8 ) {
            long shiftedI64 = 0;
            for(int i=0; i<bytesPerPixel; i++) {
                shiftedI64 |= ( 0xff & data[offset+i] ) << 8*i;
            }
            return shiftedI64;
        } else {
            throw new UnsupportedOperationException(bytesPerPixel+" bytesPerPixel too big, i.e. > 8");
        }
    }
    /**
     * Returns shifted bytes from the given {@code data} at current position
     * of maximal 4 {@code bytesPerPixel}.
     * @param bytesPerPixel number of bytes per pixel to fetch, a maximum of 4 are allowed
     * @param data byte buffer covering complete pixel at position {@code offset}
     * @param retainDataPos if true, absolute {@link ByteBuffer#get(int)} is used and the {@code data} position stays unchanged.
     *                      Otherwise relative {@link ByteBuffer#get()} is used and the {@code data} position changes.
     * @return the shifted 32bit integer value of the pixel
     */
    public static int getShiftedI32(final int bytesPerPixel, final ByteBuffer data, final boolean retainDataPos) {
        if( bytesPerPixel <= 4 ) {
            int shiftedI32 = 0;
            if( retainDataPos ) {
                final int offset = data.position();
                for(int i=0; i<bytesPerPixel; i++) {
                    shiftedI32 |= ( 0xff & data.get(offset+i) ) << 8*i;
                }
            } else {
                for(int i=0; i<bytesPerPixel; i++) {
                    shiftedI32 |= ( 0xff & data.get() ) << 8*i;
                }
            }
            return shiftedI32;
        } else {
            throw new UnsupportedOperationException(bytesPerPixel+" bytesPerPixel too big, i.e. > 4");
        }
    }
    /**
     * Returns shifted bytes from the given {@code data} at current position
     * of maximal 8 {@code bytesPerPixel}.
     * @param bytesPerPixel number of bytes per pixel to fetch, a maximum of 4 are allowed
     * @param data byte buffer covering complete pixel at position {@code offset}
     * @param retainDataPos if true, absolute {@link ByteBuffer#get(int)} is used and the {@code data} position stays unchanged.
     *                      Otherwise relative {@link ByteBuffer#get()} is used and the {@code data} position changes.
     * @return the shifted 64bit integer value of the pixel
     */
    public static long getShiftedI64(final int bytesPerPixel, final ByteBuffer data, final boolean retainDataPos) {
        if( bytesPerPixel <= 8 ) {
            long shiftedI64 = 0;
            if( retainDataPos ) {
                final int offset = data.position();
                for(int i=0; i<bytesPerPixel; i++) {
                    shiftedI64 |= ( 0xff & data.get(offset+i) ) << 8*i;
                }
            } else {
                for(int i=0; i<bytesPerPixel; i++) {
                    shiftedI64 |= ( 0xff & data.get() ) << 8*i;
                }
            }
            return shiftedI64;
        } else {
            throw new UnsupportedOperationException(bytesPerPixel+" bytesPerPixel too big, i.e. > 8");
        }
    }

    /**
     * Returns the {@link PixelFormat} with reversed components of <code>fmt</code>.
     * If no reversed  {@link PixelFormat} is available, returns <code>fmt</code>.
     */
    public static PixelFormat getReversed(final PixelFormat fmt) {
        switch(fmt) {
            case RGB565:
                return PixelFormat.BGR565;
            case BGR565:
                return PixelFormat.RGB565;
            case RGBA5551:
                return PixelFormat.ABGR1555;
            case ABGR1555:
                return PixelFormat.RGBA5551;
            case RGB888:
                return PixelFormat.BGR888;
            case BGR888:
                return PixelFormat.RGB888;
            case RGBA8888:
                return PixelFormat.ABGR8888;
            case ABGR8888:
                return PixelFormat.RGBA8888;
            case ARGB8888:
                return PixelFormat.BGRA8888;
            case BGRA8888:
                return PixelFormat.ABGR8888;
            default:
                return fmt;
        }
    }

    public static int convertToInt32(final PixelFormat dst_fmt, final byte r, final byte g, final byte b, final byte a) {
        switch(dst_fmt) {
            case LUMINANCE: {
                final byte l = ( byte) ( ( ( ( 0xff & r ) + ( 0xff & g ) + ( 0xff & b ) ) / 3 ) * a );
                return ( 0xff     ) << 24 | ( 0xff & l ) << 16 | ( 0xff & l ) << 8 | ( 0xff & l );
            }
            case RGB888:
                return ( 0xff     ) << 24 | ( 0xff & b ) << 16 | ( 0xff & g ) << 8 | ( 0xff & r );
            case BGR888:
                return ( 0xff     ) << 24 | ( 0xff & r ) << 16 | ( 0xff & g ) << 8 | ( 0xff & b );
            case RGBA8888:
                return ( 0xff & a ) << 24 | ( 0xff & b ) << 16 | ( 0xff & g ) << 8 | ( 0xff & r );
            case ABGR8888:
                return ( 0xff & r ) << 24 | ( 0xff & g ) << 16 | ( 0xff & b ) << 8 | ( 0xff & a );
            case ARGB8888:
                return ( 0xff & b ) << 24 | ( 0xff & g ) << 16 | ( 0xff & r ) << 8 | ( 0xff & a );
            case BGRA8888:
                return ( 0xff & a ) << 24 | ( 0xff & r ) << 16 | ( 0xff & g ) << 8 | ( 0xff & b );
            default:
                throw new InternalError("Unhandled format "+dst_fmt);
        }
    }

    public static int convertToInt32(final PixelFormat dst_fmt, final PixelFormat src_fmt, final ByteBuffer src, int srcOff) {
        final byte r, g, b, a;
        switch(src_fmt) {
            case LUMINANCE:
                r  = src.get(srcOff++); // R
                g  = r;                 // G
                b  = r;                 // B
                a  = (byte) 0xff;       // A
                break;
            case RGB888:
                r  = src.get(srcOff++); // R
                g  = src.get(srcOff++); // G
                b  = src.get(srcOff++); // B
                a  = (byte) 0xff;       // A
                break;
            case BGR888:
                b  = src.get(srcOff++); // B
                g  = src.get(srcOff++); // G
                r  = src.get(srcOff++); // R
                a  = (byte) 0xff;       // A
                break;
            case RGBA8888:
                r  = src.get(srcOff++); // R
                g  = src.get(srcOff++); // G
                b  = src.get(srcOff++); // B
                a  = src.get(srcOff++); // A
                break;
            case ABGR8888:
                a  = src.get(srcOff++); // A
                b  = src.get(srcOff++); // B
                g  = src.get(srcOff++); // G
                r  = src.get(srcOff++); // R
                break;
            case ARGB8888:
                a  = src.get(srcOff++); // A
                r  = src.get(srcOff++); // R
                g  = src.get(srcOff++); // G
                b  = src.get(srcOff++); // B
                break;
            case BGRA8888:
                b  = src.get(srcOff++); // B
                g  = src.get(srcOff++); // G
                r  = src.get(srcOff++); // R
                a  = src.get(srcOff++); // A
                break;
            default:
                throw new InternalError("Unhandled format "+src_fmt);
        }
        return convertToInt32(dst_fmt, r, g, b, a);
    }

    public static int convertToInt32(final PixelFormat dest_fmt, final PixelFormat src_fmt, final int src_pixel) {
        final byte r, g, b, a;
        switch(src_fmt) {
            case LUMINANCE:
                r  = (byte) ( src_pixel       );  // R
                g  = r;                           // G
                b  = r;                           // B
                a  = (byte) 0xff;                 // A
                break;
            case RGB888:
                r  = (byte) ( src_pixel        ); // R
                g  = (byte) ( src_pixel >>>  8 ); // G
                b  = (byte) ( src_pixel >>> 16 ); // B
                a  = (byte) 0xff;                 // A
                break;
            case BGR888:
                b  = (byte) ( src_pixel        ); // B
                g  = (byte) ( src_pixel >>>  8 ); // G
                r  = (byte) ( src_pixel >>> 16 ); // R
                a  = (byte) 0xff;                 // A
                break;
            case RGBA8888:
                r  = (byte) ( src_pixel        ); // R
                g  = (byte) ( src_pixel >>>  8 ); // G
                b  = (byte) ( src_pixel >>> 16 ); // B
                a  = (byte) ( src_pixel >>> 24 ); // A
                break;
            case ABGR8888:
                a  = (byte) ( src_pixel        ); // A
                b  = (byte) ( src_pixel >>>  8 ); // B
                g  = (byte) ( src_pixel >>> 16 ); // G
                r  = (byte) ( src_pixel >>> 24 ); // R
                break;
            case ARGB8888:
                a  = (byte) ( src_pixel        ); // A
                r  = (byte) ( src_pixel >>>  8 ); // R
                g  = (byte) ( src_pixel >>> 16 ); // G
                b  = (byte) ( src_pixel >>> 24 ); // B
                break;
            case BGRA8888:
                b  = (byte) ( src_pixel        ); // B
                g  = (byte) ( src_pixel >>>  8 ); // G
                r  = (byte) ( src_pixel >>> 16 ); // R
                a  = (byte) ( src_pixel >>> 24 ); // A
                break;
            default:
                throw new InternalError("Unhandled format "+src_fmt);
        }
        return convertToInt32(dest_fmt, r, g, b, a);
    }

    public static PixelRectangle convert(final PixelRectangle src,
                                         final PixelFormat destFmt, final int ddestStride, final boolean isGLOriented,
                                         final boolean destIsDirect) {
        final int width = src.getSize().getWidth();
        final int height = src.getSize().getHeight();
        final int bpp = destFmt.comp.bytesPerPixel();
        final int destStride;
        if( 0 != ddestStride ) {
            destStride = ddestStride;
        } else {
            destStride = bpp * width;
        }
        final int capacity = destStride*height;
        final ByteBuffer destBB = destIsDirect ? Buffers.newDirectByteBuffer(capacity) : ByteBuffer.allocate(capacity).order(src.getPixels().order());
        convert(src, destBB, destFmt, isGLOriented, destStride);
        return new PixelRectangle.GenericPixelRect(destFmt, src.getSize(), destStride, isGLOriented, destBB);
    }

    /**
     * @param src
     * @param dst_bb  {@link ByteBuffer} sink
     * @param dst_fmt destination {@link PixelFormat}
     * @param dst_glOriented if true, the source memory is laid out in OpenGL's coordinate system, <i>origin at bottom left</i>,
     *                       otherwise <i>origin at top left</i>.
     * @param dst_lineStride line stride in byte-size for destination, i.e. byte count from one line to the next.
     *                       Must be >= {@link PixelFormat.Composition#bytesPerPixel() dst_fmt.comp.bytesPerPixel()} * width
     *                       or {@code zero} for default stride.
     *
     * @throws IllegalStateException
     * @throws IllegalArgumentException if {@code src_lineStride} or {@code dst_lineStride} is invalid
     */
    public static void convert(final PixelRectangle src,
                               final ByteBuffer dst_bb, final PixelFormat dst_fmt, final boolean dst_glOriented, final int dst_lineStride)
           throws IllegalStateException
    {
        convert(src.getSize().getWidth(), src.getSize().getHeight(),
                src.getPixels(), src.getPixelformat(), src.isGLOriented(), src.getStride(),
                dst_bb, dst_fmt, dst_glOriented, dst_lineStride);
    }


    /**
     * @param width width of the to be converted pixel rectangle
     * @param height height of the to be converted pixel rectangle
     * @param src_bb  {@link ByteBuffer} source
     * @param src_fmt source {@link PixelFormat}
     * @param src_glOriented if true, the source memory is laid out in OpenGL's coordinate system, <i>origin at bottom left</i>,
     *                       otherwise <i>origin at top left</i>.
     * @param src_lineStride line stride in byte-size for source, i.e. byte count from one line to the next.
     *                       Must be >= {@link PixelFormat.Composition#bytesPerPixel() src_fmt.comp.bytesPerPixel()} * width
     *                       or {@code zero} for default stride.
     * @param dst_bb  {@link ByteBuffer} sink
     * @param dst_fmt destination {@link PixelFormat}
     * @param dst_glOriented if true, the source memory is laid out in OpenGL's coordinate system, <i>origin at bottom left</i>,
     *                       otherwise <i>origin at top left</i>.
     * @param dst_lineStride line stride in byte-size for destination, i.e. byte count from one line to the next.
     *                       Must be >= {@link PixelFormat.Composition#bytesPerPixel() dst_fmt.comp.bytesPerPixel()} * width
     *                       or {@code zero} for default stride.
     *
     * @throws IllegalStateException
     * @throws IllegalArgumentException if {@code src_lineStride} or {@code dst_lineStride} is invalid
     */
    public static void convert(final int width, final int height,
                               final ByteBuffer src_bb, final PixelFormat src_fmt, final boolean src_glOriented, int src_lineStride,
                               final ByteBuffer dst_bb, final PixelFormat dst_fmt, final boolean dst_glOriented, int dst_lineStride
                              ) throws IllegalStateException, IllegalArgumentException {
        final PixelFormat.Composition src_comp = src_fmt.comp;
        final PixelFormat.Composition dst_comp = dst_fmt.comp;
        final int src_bpp = src_comp.bytesPerPixel();
        final int dst_bpp = dst_comp.bytesPerPixel();

        if( 0 != src_lineStride ) {
            if( src_lineStride < src_bpp * width ) {
                throw new IllegalArgumentException(String.format("Invalid %s stride %d, must be greater than bytesPerPixel %d * width %d",
                        "source", src_lineStride, src_bpp, width));
            }
        } else {
            src_lineStride = src_bpp * width;
        }
        if( 0 != dst_lineStride ) {
            if( dst_lineStride < dst_bpp * width ) {
                throw new IllegalArgumentException(String.format("Invalid %s stride %d, must be greater than bytesPerPixel %d * width %d",
                        "destination", dst_lineStride, dst_bpp, width));
            }
        } else {
            dst_lineStride = dst_bpp * width;
        }

        // final int src_comp_bitStride = src_comp.bitStride();
        final int dst_comp_bitStride = dst_comp.bitStride();
        final boolean vert_flip = src_glOriented != dst_glOriented;
        final boolean fast_copy = src_comp.equals(dst_comp) && 0 == dst_comp_bitStride%8;
        if( DEBUG ) {
            System.err.println("XXX: size "+width+"x"+height+", fast_copy "+fast_copy);
            System.err.println("XXX: SRC fmt "+src_fmt+", "+src_comp+", stride "+src_lineStride+", isGLOrient "+src_glOriented);
            System.err.println("XXX: DST fmt "+dst_fmt+", "+dst_comp+", stride "+dst_lineStride+", isGLOrient "+dst_glOriented);
        }

        if( fast_copy ) {
            // Fast copy
            for(int y=0; y<height; y++) {
                int src_off = vert_flip ? ( height - 1 - y ) * src_lineStride : y * src_lineStride;
                int dst_off = dst_lineStride*y;
                for(int x=0; x<width; x++) {
                    dst_bb.put(dst_off+0, src_bb.get(src_off+0)); // 1
                    if( 2 <= dst_bpp ) {
                        dst_bb.put(dst_off+1, src_bb.get(src_off+1)); // 2
                        if( 3 <= dst_bpp ) {
                            dst_bb.put(dst_off+2, src_bb.get(src_off+2)); // 3
                            if( 4 <= dst_bpp ) {
                                dst_bb.put(dst_off+3, src_bb.get(src_off+3)); // 4
                            }
                        }
                    }
                    src_off += src_bpp;
                    dst_off += dst_bpp;
                }
            }
        } else {
            // Conversion
            final ComponentMap cmap = new ComponentMap(src_fmt.comp, dst_fmt.comp);

            final Bitstream.ByteBufferStream srcBBS = new Bitstream.ByteBufferStream(src_bb);
            final Bitstream<ByteBuffer> srcBitStream = new Bitstream<ByteBuffer>(srcBBS, false /* outputMode */);
            srcBitStream.setThrowIOExceptionOnEOF(true);

            final Bitstream.ByteBufferStream dstBBS = new Bitstream.ByteBufferStream(dst_bb);
            final Bitstream<ByteBuffer> dstBitStream = new Bitstream<ByteBuffer>(dstBBS, true /* outputMode */);
            dstBitStream.setThrowIOExceptionOnEOF(true);

            if( DEBUG ) {
                System.err.println("XXX: cmap.dst2src "+Arrays.toString(cmap.dst2src));
                System.err.println("XXX: cmap.src2dst "+Arrays.toString(cmap.src2dst));
                System.err.println("XXX: cmap.srcRGBA "+Arrays.toString(cmap.srcRGBA));
                System.err.println("XXX: srcBitStream "+srcBitStream);
                System.err.println("XXX: dstBitStream "+dstBitStream);
            }
            try {
                for(int y=0; y<height; y++) {
                    final int src_off = vert_flip ? ( height - 1 - y ) * src_lineStride * 8 : y * src_lineStride * 8;
                    // final int dst_off = dst_lineStride*8*y;
                    srcBitStream.position(src_off);
                    for(int x=0; x<width; x++) {
                        convert(cmap, dst_comp, dstBitStream, src_comp, srcBitStream);
                    }
                    // srcBitStream.skip(( src_lineStride * 8 ) - ( src_comp_bitStride * width ));
                    dstBitStream.skip(( dst_lineStride * 8 ) - ( dst_comp_bitStride * width ));
                }
            } catch(final IOException ioe) {
                throw new RuntimeException(ioe);
            }
            if( DEBUG ) {
                System.err.println("XXX: srcBitStream "+srcBitStream);
                System.err.println("XXX: dstBitStream "+dstBitStream);
            }
        }
    }

    public static void convert(final ComponentMap cmap,
                               final PixelFormat.Composition dstComp,
                               final Bitstream<ByteBuffer> dstBitStream,
                               final PixelFormat.Composition srcComp,
                               final Bitstream<ByteBuffer> srcBitStream) throws IllegalStateException, IOException {
        final int sCompCount = srcComp.componentCount();
        final int dCompCount = dstComp.componentCount();
        final int[] sc = new int[sCompCount];
        final int[] dcDef = new int[dCompCount];
        final int[] srcCompBitCount = srcComp.componentBitCount();
        final int[] srcCompBitMask = srcComp.componentBitMask();
        final int[] dstCompBitCount = dstComp.componentBitCount();

        // Fill w/ source values
        for(int sIdx=0; sIdx<sCompCount; sIdx++) {
            sc[sIdx] = srcBitStream.readBits31(srcCompBitCount[sIdx]) & srcCompBitMask[sIdx];
        }
        srcBitStream.skip(srcComp.bitStride() - srcComp.bitsPerPixel());

        // Cache missing defaults
        for(int i=0; i<dCompCount; i++) {
            dcDef[i] = dstComp.defaultValue(i, false);
        }

        if( 1 == dCompCount &&
            PixelFormat.CType.Y == dstComp.componentOrder()[0] &&
            cmap.hasSrcRGB
          )
        {
            // RGB[A] -> Y conversion
            final int r = sc[cmap.srcRGBA[0]];
            final int g = sc[cmap.srcRGBA[1]];
            final int b = sc[cmap.srcRGBA[2]];
            final float rF = srcComp.toFloat(r, cmap.srcRGBA[0], false);
            final float gF = srcComp.toFloat(g, cmap.srcRGBA[1], false);
            final float bF = srcComp.toFloat(b, cmap.srcRGBA[2], false);
            final int a;
            final float aF;
            /** if( 0 <= cmap.srcRGBA[3] ) { // disable premultiplied-alpha
                a = sc[cmap.srcRGBA[3]];
                aF = srcComp.toFloat(a, false, cmap.srcRGBA[3]);
            } else */ {
                a = 1;
                aF = 1f;
            }
            final float lF = ( rF + gF + bF ) * aF / 3f;
            final int v = dstComp.fromFloat(lF, 0, false);

            dstBitStream.writeBits31(dstCompBitCount[0], v);
            dstBitStream.skip(dstComp.bitStride() - dstComp.bitsPerPixel());
            if( DEBUG ) {
                if( srcBitStream.position() <= 8*4 ) {
                    System.err.printf("convert: rgb[a] -> Y: rgb 0x%02X 0x%02X 0x%02X 0x%02X -> %f %f %f %f"+
                            " -> %f -> dstC 0 0x%08X (%d bits: %s)%n",
                            r, g, b, a,
                            rF, gF, bF, aF,
                            lF, v, dstCompBitCount[0], Bitstream.toBinString(true, v, dstCompBitCount[0])
                            );
                }
            }
            return;
        }

        for(int dIdx=0; dIdx<dCompCount; dIdx++) {
            int sIdx;
            if( 0 <= ( sIdx = cmap.dst2src[dIdx] ) ) {
                final float f = srcComp.toFloat(sc[sIdx], sIdx, false);
                final int v = dstComp.fromFloat(f, dIdx, false);
                dstBitStream.writeBits31(dstCompBitCount[dIdx], v);
                if( DEBUG ) {
                    if( srcBitStream.position() <= 8*4 ) {
                        System.err.printf("convert: srcC %d: 0x%08X -> %f -> dstC %d 0x%08X (%d bits: %s)%n",
                                sIdx, sc[sIdx], f, dIdx, v, dstCompBitCount[dIdx], Bitstream.toBinString(true, v, dstCompBitCount[dIdx]));
                    }
                }
            } else {
                dstBitStream.writeBits31(dstCompBitCount[dIdx], dcDef[dIdx]);
                if( DEBUG ) {
                    if( srcBitStream.position() <= 8*4 ) {
                        System.err.printf("convert: srcC %d: undef -> dstC %d 0x%08X (%d bits: %s)%n",
                                sIdx, dIdx, dcDef[dIdx], dstCompBitCount[dIdx], Bitstream.toBinString(true, dcDef[dIdx], dstCompBitCount[dIdx]));
                    }
                }
            }
        }
        dstBitStream.skip(dstComp.bitStride() - dstComp.bitsPerPixel());
        return;
    }
}


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
package javax.media.nativewindow.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.jogamp.common.nio.Buffers;

/**
 * Pixel Rectangle Utilities.
 * <p>
 * All conversion methods are endian independent.
 * </p>
 */
public class PixelFormatUtil {
    public static interface PixelSink {
        /** Return the sink's destination pixelformat. */
        PixelFormat getPixelformat();

        /**
         * Returns stride in byte-size, i.e. byte count from one line to the next.
         * <p>
         * Must be >= {@link #getPixelformat()}.{@link PixelFormat#bytesPerPixel() bytesPerPixel()} * {@link #getSize()}.{@link DimensionImmutable#getWidth() getWidth()}.
         * </p>
         */
        int getStride();

        /**
         * Returns <code>true</code> if the sink's memory is laid out in
         * OpenGL's coordinate system, <i>origin at bottom left</i>.
         * Otherwise returns <code>false</code>, i.e. <i>origin at top left</i>.
         */
        boolean isGLOriented();
    }
    /**
     * Pixel sink for up-to 32bit.
     */
    public static interface PixelSink32 extends PixelSink {
        /**
         * Will be invoked over all rows top-to down
         * and all columns left-to-right.
         * <p>
         * Shall consider dest pixelformat and only store as much components
         * as defined, up to 32bit.
         * </p>
         * <p>
         * Implementation may better write single bytes from low-to-high bits,
         * e.g. {@link ByteOrder#LITTLE_ENDIAN} order.
         * Otherwise a possible endian conversion must be taken into consideration.
         * </p>
         * @param x
         * @param y
         * @param pixel
         */
        void store(int x, int y, int pixel);
    }

    /**
     * Returns the {@link PixelFormat} with reversed components of <code>fmt</code>.
     * If no reversed  {@link PixelFormat} is available, returns <code>fmt</code>.
     */
    public static PixelFormat getReversed(PixelFormat fmt) {
        switch(fmt) {
            case LUMINANCE:
                return PixelFormat.LUMINANCE;
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
                throw new InternalError("Unhandled format "+fmt);
        }
    }

    public static int getValue32(PixelFormat src_fmt, ByteBuffer src, int srcOff) {
        switch(src_fmt) {
            case LUMINANCE: {
                    final byte c1 = src.get(srcOff++);
                    return ( 0xff      ) << 24 | ( 0xff & c1 ) << 16 | ( 0xff & c1 ) << 8 | ( 0xff & c1 );
                }
            case RGB888:
            case BGR888: {
                    final byte c1  = src.get(srcOff++);
                    final byte c2  = src.get(srcOff++);
                    final byte c3  = src.get(srcOff++);
                    return ( 0xff      ) << 24 | ( 0xff & c3 ) << 16 | ( 0xff & c2 ) << 8 | ( 0xff & c1 );
                }
            case RGBA8888:
            case ABGR8888:
            case ARGB8888:
            case BGRA8888: {
                    final byte c1  = src.get(srcOff++);
                    final byte c2  = src.get(srcOff++);
                    final byte c3  = src.get(srcOff++);
                    final byte c4  = src.get(srcOff++);
                    return ( 0xff & c4 ) << 24 | ( 0xff & c3 ) << 16 | ( 0xff & c2 ) << 8 | ( 0xff & c1 );
                }
            default:
                throw new InternalError("Unhandled format "+src_fmt);
        }
    }

    public static int convertToInt32(PixelFormat dest_fmt, final byte r, final byte g, final byte b, final byte a) {
        switch(dest_fmt) {
            case LUMINANCE: {
                final byte l = ( byte) ( ( ( ( 0xff & r ) + ( 0xff & g ) + ( 0xff & b ) ) / 3 ) );
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
                throw new InternalError("Unhandled format "+dest_fmt);
        }
    }

    public static int convertToInt32(PixelFormat dest_fmt, PixelFormat src_fmt, ByteBuffer src, int srcOff) {
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
        return convertToInt32(dest_fmt, r, g, b, a);
    }

    public static int convertToInt32(PixelFormat dest_fmt, PixelFormat src_fmt, final int src_pixel) {
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

    public static PixelRectangle convert32(final PixelRectangle src,
                                           final PixelFormat destFmt, int ddestStride, final boolean isGLOriented,
                                           final boolean destIsDirect) {
        final int width = src.getSize().getWidth();
        final int height = src.getSize().getHeight();
        final int bpp = destFmt.bytesPerPixel();
        final int destStride;
        if( 0 != ddestStride ) {
            destStride = ddestStride;
            if( destStride < bpp * width ) {
                throw new IllegalArgumentException("Invalid stride "+destStride+", must be greater than bytesPerPixel "+bpp+" * width "+width);
            }
        } else {
            destStride = bpp * width;
        }
        final int capacity = destStride*height;
        final ByteBuffer bb = destIsDirect ? Buffers.newDirectByteBuffer(capacity) : ByteBuffer.allocate(capacity).order(src.getPixels().order());

        // System.err.println("XXX: SOURCE "+src);
        // System.err.println("XXX: DEST fmt "+destFmt+", stride "+destStride+" ("+ddestStride+"), isGL "+isGLOriented+", "+width+"x"+height+", capacity "+capacity+", "+bb);

        final PixelFormatUtil.PixelSink32 imgSink = new PixelFormatUtil.PixelSink32() {
            public void store(int x, int y, int pixel) {
                int o = destStride*y+x*bpp;
                bb.put(o++, (byte) ( pixel        )); // 1
                if( 3 <= bpp ) {
                    bb.put(o++, (byte) ( pixel >>>  8 )); // 2
                    bb.put(o++, (byte) ( pixel >>> 16 )); // 3
                    if( 4 <= bpp ) {
                        bb.put(o++, (byte) ( pixel >>> 24 )); // 4
                    }
                }
            }
            @Override
            public final PixelFormat getPixelformat() {
                return destFmt;
            }
            @Override
            public final int getStride() {
                return destStride;
            }
            @Override
            public final boolean isGLOriented() {
                return isGLOriented;
            }
        };
        convert32(imgSink, src);
        return new PixelRectangle.GenericPixelRect(destFmt, src.getSize(), destStride, isGLOriented, bb);
    }

    public static void convert32(PixelSink32 destInt32, final PixelRectangle src) {
        convert32(destInt32,
                  src.getPixels(), src.getPixelformat(),
                  src.isGLOriented(),
                  src.getSize().getWidth(), src.getSize().getHeight(),
                  src.getStride());
    }

    /**
     *
     * @param dest32 32bit pixel sink
     * @param src_bb
     * @param src_fmt
     * @param src_glOriented if true, the source memory is laid out in OpenGL's coordinate system, <i>origin at bottom left</i>,
     *                       otherwise <i>origin at top left</i>.
     * @param width
     * @param height
     * @param strideInBytes stride in byte-size, i.e. byte count from one line to the next.
     *                      If zero, stride is set to <code>width * bytes-per-pixel</code>.
     *                      If not zero, value must be >= <code>width * bytes-per-pixel</code>.
     * @param stride_bytes stride in byte-size, i.e. byte count from one line to the next.
     *                     Must be >= {@link PixelFormat#bytesPerPixel() src_fmt.bytesPerPixel()} * width.
     * @throws IllegalArgumentException if <code>strideInBytes</code> is invalid
     */
    public static void convert32(PixelSink32 dest32,
                                 final ByteBuffer src_bb, final PixelFormat src_fmt, final boolean src_glOriented, final int width, final int height, int stride_bytes) {
        final int src_bpp = src_fmt.bytesPerPixel();
        if( 0 != stride_bytes ) {
            if( stride_bytes < src_bpp * width ) {
                throw new IllegalArgumentException("Invalid stride "+stride_bytes+", must be greater than bytesPerPixel "+src_bpp+" * width "+width);
            }
        } else {
            stride_bytes = src_bpp * width;
        }
        final PixelFormat dest_fmt = dest32.getPixelformat();
        final boolean vert_flip = src_glOriented != dest32.isGLOriented();
        final boolean fast_copy = src_fmt == dest_fmt && dest_fmt.bytesPerPixel() == 4 ;
        // System.err.println("XXX: SRC fmt "+src_fmt+", stride "+stride_bytes+", isGL "+src_glOriented+", "+width+"x"+height);
        // System.err.println("XXX: DST fmt "+dest_fmt+", fast_copy "+fast_copy);

        if( fast_copy ) {
            // Fast copy
            for(int y=0; y<height; y++) {
                int o = vert_flip ? ( height - 1 - y ) * stride_bytes : y * stride_bytes;
                for(int x=0; x<width; x++) {
                    dest32.store(x, y, getValue32(src_fmt, src_bb, o));
                    o += src_bpp;
                }
            }
        } else {
            // Conversion
            for(int y=0; y<height; y++) {
                int o = vert_flip ? ( height - 1 - y ) * stride_bytes : y * stride_bytes;
                for(int x=0; x<width; x++) {
                    dest32.store( x, y, convertToInt32( dest_fmt, src_fmt, src_bb, o));
                    o += src_bpp;
                }
            }
        }
    }
}


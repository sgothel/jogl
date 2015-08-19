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
package com.jogamp.opengl.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.nativewindow.util.PixelRectangle;
import com.jogamp.nativewindow.util.PixelFormatUtil;

import jogamp.opengl.Debug;
import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.ImageLine;
import jogamp.opengl.util.pngj.ImageLineHelper;
import jogamp.opengl.util.pngj.PngReader;
import jogamp.opengl.util.pngj.PngWriter;
import jogamp.opengl.util.pngj.chunks.PngChunkPLTE;
import jogamp.opengl.util.pngj.chunks.PngChunkTRNS;
import jogamp.opengl.util.pngj.chunks.PngChunkTextVar;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.IOUtil;

public class PNGPixelRect extends PixelRectangle.GenericPixelRect {
    private static final boolean DEBUG = Debug.debug("PNG");

    /**
     * Reads a PNG image from the specified InputStream.
     * <p>
     * Implicitly converts the image to match the desired:
     * <ul>
     *   <li>{@link PixelFormat}, see {@link #getPixelformat()}</li>
     *   <li><code>destStrideInBytes</code>, see {@link #getStride()}</li>
     *   <li><code>destIsGLOriented</code>, see {@link #isGLOriented()}</li>
     * </ul>
     * </p>
     *
     * @param in input stream
     * @param destFmt desired destination {@link PixelFormat} incl. conversion, maybe <code>null</code> to use source {@link PixelFormat}
     * @param destDirectBuffer if true, using a direct NIO buffer, otherwise an array backed buffer
     * @param destMinStrideInBytes used if greater than PNG's stride, otherwise using PNG's stride. Stride is width * bytes-per-pixel.
     * @param destIsGLOriented
     * @return the newly created PNGPixelRect instance
     * @throws IOException
     */
    public static PNGPixelRect read(final InputStream in,
                                    final PixelFormat ddestFmt, final boolean destDirectBuffer, final int destMinStrideInBytes,
                                    final boolean destIsGLOriented) throws IOException {
        final BufferedInputStream bin = (in instanceof BufferedInputStream) ? (BufferedInputStream)in : new BufferedInputStream(in);
        final PngReader pngr = new PngReader(bin, null);
        final ImageInfo imgInfo = pngr.imgInfo;
        final PngChunkPLTE plte = pngr.getMetadata().getPLTE();
        final PngChunkTRNS trns = pngr.getMetadata().getTRNS();
        final boolean indexed = imgInfo.indexed;
        final boolean hasAlpha = indexed ? ( trns != null ) : imgInfo.alpha ;

        if(DEBUG) {
            System.err.println("PNGPixelRect: "+imgInfo);
        }
        final int channels = indexed ? ( hasAlpha ? 4 : 3 ) : imgInfo.channels ;
        final boolean isGrayAlpha = 2 == channels && imgInfo.greyscale && imgInfo.alpha;
        if ( ! ( 1 == channels || 3 == channels || 4 == channels || isGrayAlpha ) ) {
            throw new RuntimeException("PNGPixelRect can only handle Lum/RGB/RGBA [1/3/4 channels] or Lum+A (GA) images for now. Channels "+channels + " Paletted: " + indexed);
        }
        final int bytesPerPixel = indexed ? channels : imgInfo.bytesPixel ;
        if ( ! ( 1 == bytesPerPixel || 3 == bytesPerPixel || 4 == bytesPerPixel || isGrayAlpha ) ) {
            throw new RuntimeException("PNGPixelRect can only handle Lum/RGB/RGBA [1/3/4 bpp] images for now. BytesPerPixel "+bytesPerPixel);
        }
        if( channels != bytesPerPixel ) {
            throw new RuntimeException("PNGPixelRect currently only handles Channels [1/3/4] == BytePerPixel [1/3/4], channels: "+channels+", bytesPerPixel "+bytesPerPixel);
        }
        final int width = imgInfo.cols;
        final int height = imgInfo.rows;
        final double dpiX, dpiY;
        {
            final double[] dpi = pngr.getMetadata().getDpi();
            dpiX = dpi[0];
            dpiY = dpi[1];
        }
        final PixelFormat srcFmt;
        if ( indexed ) {
            if ( hasAlpha ) {
                srcFmt = PixelFormat.RGBA8888;
            } else {
                srcFmt = PixelFormat.RGB888;
            }
        } else {
            switch( channels ) {
                case 1: srcFmt = PixelFormat.LUMINANCE; break;
                case 2: srcFmt = isGrayAlpha ? PixelFormat.LUMINANCE : null; break;
                case 3: srcFmt = PixelFormat.RGB888; break;
                case 4: srcFmt = PixelFormat.RGBA8888; break;
                default: srcFmt = null;
            }
            if( null == srcFmt ) {
                throw new InternalError("XXX: channels: "+channels+", bytesPerPixel "+bytesPerPixel);
            }
        }
        final PixelFormat destFmt;
        if( null == ddestFmt ) {
            if( isGrayAlpha ) {
                destFmt = PixelFormat.BGRA8888; // save alpha value on gray-alpha
            } else {
                destFmt = srcFmt; // 1:1
            }
        } else {
            destFmt = ddestFmt; // user choice
        }
        final int destStrideInBytes = Math.max(destMinStrideInBytes, destFmt.comp.bytesPerPixel() * width);
        final ByteBuffer destPixels = destDirectBuffer ? Buffers.newDirectByteBuffer(destStrideInBytes * height) :
                                                         ByteBuffer.allocate(destStrideInBytes * height);
        {
            final int reqBytes = destStrideInBytes * height;
            if( destPixels.limit() < reqBytes ) {
                throw new IndexOutOfBoundsException("Dest buffer has insufficient bytes left, needs "+reqBytes+": "+destPixels);
            }
        }
        final boolean vert_flip = destIsGLOriented;

        int[] rgbaScanline = indexed ? new int[width * channels] : null;
        if(DEBUG) {
            System.err.println("PNGPixelRect: indexed "+indexed+", alpha "+hasAlpha+", grayscale "+imgInfo.greyscale+", channels "+channels+"/"+imgInfo.channels+
                               ", bytesPerPixel "+bytesPerPixel+"/"+imgInfo.bytesPixel+
                               ", grayAlpha "+isGrayAlpha+", pixels "+width+"x"+height+", dpi "+dpiX+"x"+dpiY+", format "+srcFmt);
            System.err.println("PNGPixelRect: destFormat "+destFmt+" ("+ddestFmt+", fast-path "+(destFmt==srcFmt)+"), destDirectBuffer "+destDirectBuffer+", destIsGLOriented (flip) "+destIsGLOriented);
            System.err.println("PNGPixelRect: destStrideInBytes "+destStrideInBytes+" (destMinStrideInBytes "+destMinStrideInBytes+")");
        }

        for (int row = 0; row < height; row++) {
            final ImageLine l1 = pngr.readRow(row);
            int lineOff = 0;
            int dataOff = vert_flip ? ( height - 1 - row ) * destStrideInBytes : row * destStrideInBytes;
            if( indexed ) {
                for (int j = width - 1; j >= 0; j--) {
                    rgbaScanline = ImageLineHelper.palette2rgb(l1, plte, trns, rgbaScanline); // reuse rgbaScanline and update if resized
                    dataOff = getPixelRGBA8ToAny(destFmt, destPixels, dataOff, rgbaScanline, lineOff, hasAlpha);
                    lineOff += bytesPerPixel;
                }
            } else if( 1 == channels ) {
                for (int j = width - 1; j >= 0; j--) {
                    dataOff = getPixelLUMToAny(destFmt, destPixels, dataOff, (byte)l1.scanline[lineOff++], (byte)0xff); // Luminance, 1 bytesPerPixel
                }
            } else if( isGrayAlpha ) {
                for (int j = width - 1; j >= 0; j--) {
                    dataOff = getPixelLUMToAny(destFmt, destPixels, dataOff, (byte)l1.scanline[lineOff++], (byte)l1.scanline[lineOff++]); // Luminance+Alpha, 2 bytesPerPixel
                }
            } else if( srcFmt == destFmt ) { // fast-path
                for (int j = width - 1; j >= 0; j--) {
                    dataOff = getPixelRGBSame(destPixels, dataOff, l1.scanline, lineOff, bytesPerPixel);
                    lineOff += bytesPerPixel;
                }
            } else {
                for (int j = width - 1; j >= 0; j--) {
                    dataOff = getPixelRGBA8ToAny(destFmt, destPixels, dataOff, l1.scanline, lineOff, hasAlpha);
                    lineOff += bytesPerPixel;
                }
            }
        }
        pngr.end();

        return new PNGPixelRect(destFmt, new Dimension(width, height), destStrideInBytes, destIsGLOriented, destPixels, dpiX, dpiY);
    }

    private static final int getPixelLUMToAny(final PixelFormat dest_fmt, final ByteBuffer d, int dOff, final byte lum, final byte alpha) {
        switch(dest_fmt) {
            case LUMINANCE:
                d.put(dOff++, lum);
                break;
            case BGR888:
            case RGB888:
                d.put(dOff++, lum);
                d.put(dOff++, lum);
                d.put(dOff++, lum);
                break;
            case ABGR8888:
            case ARGB8888:
                d.put(dOff++, alpha); // A
                d.put(dOff++, lum);
                d.put(dOff++, lum);
                d.put(dOff++, lum);
                break;
            case BGRA8888:
            case RGBA8888:
                d.put(dOff++, lum);
                d.put(dOff++, lum);
                d.put(dOff++, lum);
                d.put(dOff++, alpha); // A
                break;
            default:
                throw new InternalError("Unhandled format "+dest_fmt);
        }
        return dOff;
    }
    private static final int getPixelRGBA8ToAny(final PixelFormat dest_fmt, final ByteBuffer d, int dOff, final int[] scanline, final int lineOff, final boolean srcHasAlpha) {
        final int p = PixelFormatUtil.convertToInt32(dest_fmt, (byte)scanline[lineOff],   // R
                                                               (byte)scanline[lineOff+1], // G
                                                               (byte)scanline[lineOff+2], // B
                                                               srcHasAlpha ? (byte)scanline[lineOff+3] : (byte)0xff); // A
        final int dbpp = dest_fmt.comp.bytesPerPixel();
        d.put(dOff++, (byte) ( p ));                // 1
        if( 1 < dbpp ) {
            d.put(dOff++, (byte) ( p >>>  8 ));     // 2
            d.put(dOff++, (byte) ( p >>> 16 ));     // 3
            if( 4 == dbpp ) {
                d.put(dOff++, (byte) ( p >>> 24 )); // 4
            }
        }
        return dOff;
    }
    private static final int getPixelRGBSame(final ByteBuffer d, int dOff, final int[] scanline, final int lineOff, final int bpp) {
        d.put(dOff++, (byte)scanline[lineOff]);             // R
        if( 1 < bpp ) {
            d.put(dOff++, (byte)scanline[lineOff + 1]);     // G
            d.put(dOff++, (byte)scanline[lineOff + 2]);     // B
            if( 4 == bpp ) {
                d.put(dOff++, (byte)scanline[lineOff + 3]); // A
            }
        }
        return dOff;
    }
    private int setPixelRGBA8(final ImageLine line, final int lineOff, final ByteBuffer src, final int srcOff, final int bytesPerPixel, final boolean hasAlpha) {
        final int b = hasAlpha ? 4-1 : 3-1;
        if( src.limit() <= srcOff + b ) {
            throw new IndexOutOfBoundsException("Buffer has unsufficient bytes left, needs ["+srcOff+".."+(srcOff+b)+"]: "+src);
        }
        final int p = PixelFormatUtil.convertToInt32(hasAlpha ? PixelFormat.RGBA8888 : PixelFormat.RGB888, pixelformat, src, srcOff);
        line.scanline[lineOff    ] = 0xff &   p;              // R
        line.scanline[lineOff + 1] = 0xff & ( p >>> 8 );      // G
        line.scanline[lineOff + 2] = 0xff & ( p >>> 16 );     // B
        if(hasAlpha) {
            line.scanline[lineOff + 3] = 0xff & ( p >>> 24 ); // A
        }
        return srcOff + pixelformat.comp.bytesPerPixel();
    }

    private static void setPixelRGBA8(final PixelFormat pixelformat, final ImageLine line, final int lineOff, final int srcPix, final int bytesPerPixel, final boolean hasAlpha) {
        final int p = PixelFormatUtil.convertToInt32(hasAlpha ? PixelFormat.RGBA8888 : PixelFormat.RGB888, pixelformat, srcPix);
        line.scanline[lineOff    ] = 0xff &   p;              // R
        line.scanline[lineOff + 1] = 0xff & ( p >>> 8 );      // G
        line.scanline[lineOff + 2] = 0xff & ( p >>> 16 );     // B
        if(hasAlpha) {
            line.scanline[lineOff + 3] = 0xff & ( p >>> 24 ); // A
        }
    }

    /**
     * Creates a PNGPixelRect from data supplied by the end user. Shares
     * data with the passed ByteBuffer.
     *
     * @param pixelformat
     * @param size
     * @param strideInBytes
     * @param isGLOriented see {@link #isGLOriented()}.
     * @param pixels
     * @param dpiX
     * @param dpiY
     */
    public PNGPixelRect(final PixelFormat pixelformat, final DimensionImmutable size,
                        final int strideInBytes, final boolean isGLOriented, final ByteBuffer pixels,
                        final double dpiX, final double dpiY) {
        super(pixelformat, size, strideInBytes, isGLOriented, pixels);
        this.dpi = new double[] { dpiX, dpiY };
    }
    public PNGPixelRect(final PixelRectangle src, final double dpiX, final double dpiY) {
        super(src);
        this.dpi = new double[] { dpiX, dpiY };
    }
    private final double[] dpi;

    /** Returns the dpi of the image. */
    public double[] getDpi() { return dpi; }

    public void write(final OutputStream outstream, final boolean closeOutstream) throws IOException {
        final int width = size.getWidth();
        final int height = size.getHeight();
        final int bytesPerPixel = pixelformat.comp.bytesPerPixel();
        final ImageInfo imi = new ImageInfo(width, height, 8 /* bitdepth */,
                                            (4 == bytesPerPixel) ? true : false /* alpha */,
                                            (1 == bytesPerPixel) ? true : false /* grayscale */,
                                            false /* indexed */);

        // open image for writing to a output stream
        try {
            final PngWriter png = new PngWriter(outstream, imi);
            // add some optional metadata (chunks)
            png.getMetadata().setDpi(dpi[0], dpi[1]);
            png.getMetadata().setTimeNow(0); // 0 seconds from now = now
            png.getMetadata().setText(PngChunkTextVar.KEY_Title, "JogAmp PNGPixelRect");
            final boolean hasAlpha = 4 == bytesPerPixel;

            final ImageLine l1 = new ImageLine(imi);
            for (int row = 0; row < height; row++) {
                int dataOff = isGLOriented ? ( height - 1 - row ) * strideInBytes : row * strideInBytes;
                int lineOff = 0;
                if(1 == bytesPerPixel) {
                    for (int j = width - 1; j >= 0; j--) {
                        l1.scanline[lineOff++] = pixels.get(dataOff++); // // Luminance, 1 bytesPerPixel
                    }
                } else {
                    for (int j = width - 1; j >= 0; j--) {
                        dataOff = setPixelRGBA8(l1, lineOff, pixels, dataOff, bytesPerPixel, hasAlpha);
                        lineOff += bytesPerPixel;
                    }
                }
                png.writeRow(l1, row);
            }
            png.end();
        } finally {
            if( closeOutstream ) {
                IOUtil.close(outstream, false);
            }
        }
    }

    public static void write(final PixelFormat pixelformat, final DimensionImmutable size,
                             int strideInPixels, final boolean isGLOriented, final IntBuffer pixels,
                             final double dpiX, final double dpiY,
                             final OutputStream outstream, final boolean closeOutstream) throws IOException {
        final int width = size.getWidth();
        final int height = size.getHeight();
        final int bytesPerPixel = pixelformat.comp.bytesPerPixel();
        final ImageInfo imi = new ImageInfo(width, height, 8 /* bitdepth */,
                                            (4 == bytesPerPixel) ? true : false /* alpha */,
                                            (1 == bytesPerPixel) ? true : false /* grayscale */,
                                            false /* indexed */);
        if( 0 != strideInPixels ) {
            if( strideInPixels < size.getWidth()) {
                throw new IllegalArgumentException("Invalid stride "+bytesPerPixel+", must be greater than width "+size.getWidth());
            }
        } else {
            strideInPixels = size.getWidth();
        }
        final int reqPixels = strideInPixels * size.getHeight();
        if( pixels.limit() < reqPixels ) {
            throw new IndexOutOfBoundsException("Dest buffer has insufficient pixels left, needs "+reqPixels+": "+pixels);
        }

        // open image for writing to a output stream
        try {
            final PngWriter png = new PngWriter(outstream, imi);
            // add some optional metadata (chunks)
            png.getMetadata().setDpi(dpiX, dpiY);
            png.getMetadata().setTimeNow(0); // 0 seconds from now = now
            png.getMetadata().setText(PngChunkTextVar.KEY_Title, "JogAmp PNGPixelRect");
            final boolean hasAlpha = 4 == bytesPerPixel;

            final ImageLine l1 = new ImageLine(imi);
            for (int row = 0; row < height; row++) {
                int dataOff = isGLOriented ? ( height - 1 - row ) * strideInPixels : row * strideInPixels;
                int lineOff = 0;
                if(1 == bytesPerPixel) {
                    for (int j = width - 1; j >= 0; j--) {
                        l1.scanline[lineOff++] = pixels.get(dataOff++); // // Luminance, 1 bytesPerPixel
                    }
                } else {
                    for (int j = width - 1; j >= 0; j--) {
                        setPixelRGBA8(pixelformat, l1, lineOff, pixels.get(dataOff++), bytesPerPixel, hasAlpha);
                        lineOff += bytesPerPixel;
                    }
                }
                png.writeRow(l1, row);
            }
            png.end();
        } finally {
            if( closeOutstream ) {
                IOUtil.close(outstream, false);
            }
        }
    }
}

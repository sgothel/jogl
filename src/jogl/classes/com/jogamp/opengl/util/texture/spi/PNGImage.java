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
package com.jogamp.opengl.util.texture.spi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;

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

public class PNGImage {
    private static final boolean DEBUG = Debug.debug("PNGImage");

    /**
     * Creates a PNGImage from data supplied by the end user. Shares
     * data with the passed ByteBuffer. Assumes the data is already in
     * the correct byte order for writing to disk, i.e., LUMINANCE, RGB or RGBA.
     * Orientation is <i>bottom-to-top</i> (OpenGL coord. default)
     * or <i>top-to-bottom</i> depending on <code>isGLOriented</code>.
     *
     * @param width
     * @param height
     * @param dpiX
     * @param dpiY
     * @param bytesPerPixel
     * @param reversedChannels
     * @param isGLOriented see {@link #isGLOriented()}.
     * @param data
     * @return
     */
    public static PNGImage createFromData(int width, int height, double dpiX, double dpiY,
                                          int bytesPerPixel, boolean reversedChannels, boolean isGLOriented, ByteBuffer data) {
        return new PNGImage(width, height, dpiX, dpiY, bytesPerPixel, reversedChannels, isGLOriented, data);
    }

    /**
     * Reads a PNG image from the specified InputStream.
     * <p>
     * Implicitly flip image to GL orientation, see {@link #isGLOriented()}.
     * </p>
     */
    public static PNGImage read(InputStream in) throws IOException {
        return new PNGImage(in);
    }

    /** Reverse read and store, implicitly flip image to GL orientation, see {@link #isGLOriented()}. */
    private static final int getPixelRGBA8(ByteBuffer d, int dOff, int[] scanline, int lineOff, boolean hasAlpha) {
        final int b = hasAlpha ? 4-1 : 3-1;
        if( d.limit() <= dOff || dOff - b < 0 ) {
            throw new IndexOutOfBoundsException("Buffer has unsufficient bytes left, needs ["+(dOff-b)+".."+dOff+"]: "+d);
        }
    	if(hasAlpha) {
            d.put(dOff--, (byte)scanline[lineOff + 3]); // A
        }
        d.put(dOff--, (byte)scanline[lineOff + 2]); // B
        d.put(dOff--, (byte)scanline[lineOff + 1]); // G
        d.put(dOff--, (byte)scanline[lineOff    ]); // R
        return dOff;
    }

    /** Reverse write and store, implicitly flip image from current orientation, see {@link #isGLOriented()}. Handle reversed channels (BGR[A]). */
    private int setPixelRGBA8(ImageLine line, int lineOff, ByteBuffer d, int dOff, boolean hasAlpha) {
        final int b = hasAlpha ? 4-1 : 3-1;
        if( d.limit() <= dOff + b ) {
            throw new IndexOutOfBoundsException("Buffer has unsufficient bytes left, needs ["+dOff+".."+(dOff+b)+"]: "+d);
        }
        if( reversedChannels ) {
            if(hasAlpha) {
                line.scanline[lineOff + 3] = d.get(dOff++); // A
            }
            line.scanline[lineOff + 2] = d.get(dOff++); // R
            line.scanline[lineOff + 1] = d.get(dOff++); // G
            line.scanline[lineOff    ] = d.get(dOff++); // B
        } else {
            line.scanline[lineOff    ] = d.get(dOff++); // R
            line.scanline[lineOff + 1] = d.get(dOff++); // G
            line.scanline[lineOff + 2] = d.get(dOff++); // B
            if(hasAlpha) {
                line.scanline[lineOff + 3] = d.get(dOff++); // A
            }
        }
        return isGLOriented ? dOff - bytesPerPixel - bytesPerPixel : dOff;
    }

    private PNGImage(int width, int height, double dpiX, double dpiY, int bytesPerPixel, boolean reversedChannels, boolean isGLOriented, ByteBuffer data) {
        pixelWidth=width;
        pixelHeight=height;
        dpi = new double[] { dpiX, dpiY };
        if(4 == bytesPerPixel) {
            glFormat = GL.GL_RGBA;
        } else if (3 == bytesPerPixel) {
            glFormat = GL.GL_RGB;
        } else {
            throw new InternalError("XXX: bytesPerPixel "+bytesPerPixel);
        }
        this.bytesPerPixel = bytesPerPixel;
        this.reversedChannels = reversedChannels;
        this.isGLOriented = isGLOriented;
        this.data = data;
    }

    private PNGImage(InputStream in) {
        final PngReader pngr = new PngReader(new BufferedInputStream(in), null);
        final ImageInfo imgInfo = pngr.imgInfo;
        final PngChunkPLTE plte = pngr.getMetadata().getPLTE();
        final PngChunkTRNS trns = pngr.getMetadata().getTRNS();
        final boolean indexed = imgInfo.indexed;
        final boolean hasAlpha = indexed ? ( trns != null ) : imgInfo.alpha ;

        final int channels = indexed ? ( hasAlpha ? 4 : 3 ) : imgInfo.channels ;
        if ( ! ( 1 == channels || 3 == channels || 4 == channels ) ) {
            throw new RuntimeException("PNGImage can only handle Lum/RGB/RGBA [1/3/4 channels] images for now. Channels "+channels + " Paletted: " + indexed);
        }

        bytesPerPixel = indexed ? channels : imgInfo.bytesPixel ;
        if ( ! ( 1 == bytesPerPixel || 3 == bytesPerPixel || 4 == bytesPerPixel ) ) {
            throw new RuntimeException("PNGImage can only handle Lum/RGB/RGBA [1/3/4 bpp] images for now. BytesPerPixel "+bytesPerPixel);
        }
        if( channels != bytesPerPixel ) {
            throw new RuntimeException("PNGImage currently only handles Channels [1/3/4] == BytePerPixel [1/3/4], channels: "+channels+", bytesPerPixel "+bytesPerPixel);
        }
        pixelWidth = imgInfo.cols;
        pixelHeight = imgInfo.rows;
        dpi = new double[2];
        {
            final double[] dpi2 = pngr.getMetadata().getDpi();
            dpi[0]=dpi2[0];
            dpi[1]=dpi2[1];
        }
        if ( indexed ) {
        	if ( hasAlpha ) {
        		glFormat = GL.GL_RGBA;
        	} else {
        		glFormat = GL.GL_RGB;
        	}
        } else {
        	switch( channels ) {
                case 1: glFormat = GL.GL_LUMINANCE; break;
                case 3: glFormat = GL.GL_RGB; break;
                case 4: glFormat = GL.GL_RGBA; break;
                default: throw new InternalError("XXX: channels: "+channels+", bytesPerPixel "+bytesPerPixel);
            }
        }
        if(DEBUG) {
            System.err.println("PNGImage: "+imgInfo);
            System.err.println("PNGImage: indexed "+indexed+", alpha "+hasAlpha+", channels "+channels+"/"+imgInfo.channels+
                               ", bytesPerPixel "+bytesPerPixel+"/"+imgInfo.bytesPixel+
                               ", pixels "+pixelWidth+"x"+pixelHeight+", dpi "+dpi[0]+"x"+dpi[1]+", glFormat 0x"+Integer.toHexString(glFormat));
        }

        data = Buffers.newDirectByteBuffer(bytesPerPixel * pixelWidth * pixelHeight);
        reversedChannels = false; // RGB[A]
        isGLOriented = true;
        int dataOff = bytesPerPixel * pixelWidth * pixelHeight - 1; // start at end-of-buffer, reverse store

        int[] rgbaScanline = indexed ? new int[imgInfo.cols * channels] : null;

        for (int row = 0; row < pixelHeight; row++) {
            final ImageLine l1 = pngr.readRow(row);
            int lineOff = ( pixelWidth - 1 ) * bytesPerPixel ; // start w/ last pixel in line, reverse read (PNG top-left -> OpenGL bottom-left origin)
            if( indexed ) {
                for (int j = pixelWidth - 1; j >= 0; j--) {
                    rgbaScanline = ImageLineHelper.palette2rgb(l1, plte, trns, rgbaScanline); // reuse rgbaScanline and update if resized
                    dataOff = getPixelRGBA8(data, dataOff, rgbaScanline, lineOff, hasAlpha);
                    lineOff -= bytesPerPixel;
                }
            } else if( 1 == channels ) {
                for (int j = pixelWidth - 1; j >= 0; j--) {
                    data.put(dataOff--, (byte)l1.scanline[lineOff--]); // Luminance, 1 bytesPerPixel
                }
            } else {
                for (int j = pixelWidth - 1; j >= 0; j--) {
            		dataOff = getPixelRGBA8(data, dataOff, l1.scanline, lineOff, hasAlpha);
                    lineOff -= bytesPerPixel;
                }
            }
        }
        pngr.end();
    }
    private final int pixelWidth, pixelHeight, glFormat, bytesPerPixel;
    private final boolean reversedChannels;
    private final boolean isGLOriented;
    private final double[] dpi;
    private final ByteBuffer data;

    /** Returns the width of the image. */
    public int getWidth()    { return pixelWidth; }

    /** Returns the height of the image. */
    public int getHeight()   { return pixelHeight; }

    /** Returns true if data has the channels reversed to BGR or BGRA, otherwise RGB or RGBA is expected. */
    public boolean getHasReversedChannels() { return reversedChannels; }

    /**
     * Returns <code>true</code> if the drawable is rendered in
     * OpenGL's coordinate system, <i>origin at bottom left</i>.
     * Otherwise returns <code>false</code>, i.e. <i>origin at top left</i>.
     * <p>
     * Default impl. is <code>true</code>, i.e. OpenGL coordinate system.
     * </p>
     */
    public boolean isGLOriented() { return isGLOriented; }

    /** Returns the dpi of the image. */
    public double[] getDpi() { return dpi; }

    /** Returns the OpenGL format for this texture; e.g. GL.GL_LUMINANCE, GL.GL_RGB or GL.GL_RGBA. */
    public int getGLFormat() { return glFormat; }

    /** Returns the OpenGL data type: GL.GL_UNSIGNED_BYTE. */
    public int getGLType() { return GL.GL_UNSIGNED_BYTE; }

    /** Returns the bytes per pixel */
    public int getBytesPerPixel() { return bytesPerPixel; }

    /** Returns the raw data for this texture in the correct
        (bottom-to-top) order for calls to glTexImage2D. */
    public ByteBuffer getData()  { return data; }

    public void write(File out, boolean allowOverwrite) throws IOException {
        final ImageInfo imi = new ImageInfo(pixelWidth, pixelHeight, 8, (4 == bytesPerPixel) ? true : false); // 8 bits per channel, no alpha
        // open image for writing to a output stream
        final OutputStream outs = new BufferedOutputStream(IOUtil.getFileOutputStream(out, allowOverwrite));
        try {
            final PngWriter png = new PngWriter(outs, imi);
            // add some optional metadata (chunks)
            png.getMetadata().setDpi(dpi[0], dpi[1]);
            png.getMetadata().setTimeNow(0); // 0 seconds fron now = now
            png.getMetadata().setText(PngChunkTextVar.KEY_Title, "JogAmp PNGImage");
            // png.getMetadata().setText("my key", "my text");
            final boolean hasAlpha = 4 == bytesPerPixel;
            final ImageLine l1 = new ImageLine(imi);
            if( isGLOriented ) {
                // start at last pixel at end-of-buffer, reverse read (OpenGL bottom-left -> PNG top-left origin)
                int dataOff = ( pixelWidth * bytesPerPixel * ( pixelHeight - 1 ) ) + // full lines - 1 line
                              ( ( pixelWidth - 1 ) * bytesPerPixel );                // one line - 1 pixel
                for (int row = 0; row < pixelHeight; row++) {
                    int lineOff = ( pixelWidth - 1 ) * bytesPerPixel ; // start w/ last pixel in line, reverse store (OpenGL bottom-left -> PNG top-left origin)
                    if(1 == bytesPerPixel) {
                        for (int j = pixelWidth - 1; j >= 0; j--) {
                            l1.scanline[lineOff--] = data.get(dataOff--); // // Luminance, 1 bytesPerPixel
                        }
                    } else {
                        for (int j = pixelWidth - 1; j >= 0; j--) {
                            dataOff = setPixelRGBA8(l1, lineOff, data, dataOff, hasAlpha);
                            lineOff -= bytesPerPixel;
                        }
                    }
                    png.writeRow(l1, row);
                }
            } else {
                int dataOff = 0; // start at first pixel at start-of-buffer, normal read (same origin: top-left)
                for (int row = 0; row < pixelHeight; row++) {
                    int lineOff = 0; // start w/ first pixel in line, normal store (same origin: top-left)
                    if(1 == bytesPerPixel) {
                        for (int j = pixelWidth - 1; j >= 0; j--) {
                            l1.scanline[lineOff++] = data.get(dataOff++); // // Luminance, 1 bytesPerPixel
                        }
                    } else {
                        for (int j = pixelWidth - 1; j >= 0; j--) {
                            dataOff = setPixelRGBA8(l1, lineOff, data, dataOff, hasAlpha);
                            lineOff += bytesPerPixel;
                        }
                    }
                    png.writeRow(l1, row);
                }
            }
            png.end();
        } finally {
            IOUtil.close(outs, false);
        }
    }

    @Override
    public String toString() { return "PNGImage["+pixelWidth+"x"+pixelHeight+", dpi "+dpi[0]+" x "+dpi[1]+", bytesPerPixel "+bytesPerPixel+", reversedChannels "+reversedChannels+", "+data+"]"; }
}

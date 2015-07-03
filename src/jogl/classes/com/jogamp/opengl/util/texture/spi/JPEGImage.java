/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;

import jogamp.opengl.Debug;
import jogamp.opengl.util.jpeg.JPEGDecoder;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.TextureData.ColorSpace;

public class JPEGImage {
    private static final boolean DEBUG = Debug.debug("JPEGImage");


    /**
     * Reads a JPEG image from the specified InputStream, using the given color space for storage.
     *
     * @param in
     * @param cs Storage color space, either {@link ColorSpace#RGB} or {@link ColorSpace#YCbCr}. {@link ColorSpace#YCCK} and {@link ColorSpace#CMYK} will throw an exception!
     * @return
     * @throws IOException
     */
    public static JPEGImage read(final InputStream in, final ColorSpace cs) throws IOException {
        return new JPEGImage(in, cs);
    }

    /** Reads a JPEG image from the specified InputStream, using the {@link ColorSpace#RGB}. */
    public static JPEGImage read(final InputStream in) throws IOException {
        return new JPEGImage(in, ColorSpace.RGB);
    }

    private static class JPEGColorSink implements JPEGDecoder.ColorSink  {
        int width=0, height=0;
        int sourceComponents=0;
        ColorSpace sourceCS = ColorSpace.YCbCr;
        int storageComponents;
        final ColorSpace storageCS;
        ByteBuffer data = null;

        JPEGColorSink(final ColorSpace storageCM) {
            this.storageCS = storageCM;
            switch(storageCS) {
            case RGB:
            case YCbCr:
                storageComponents = 3;
                break;
            default:
                throw new IllegalArgumentException("Unsupported storage color-space: "+storageCS);
            }
        }

        @Override
        public final ColorSpace allocate(final int width, final int height, final ColorSpace sourceCM, final int sourceComponents) throws RuntimeException {
            this.width = width;
            this.height = height;
            this.sourceComponents = sourceComponents;
            this.sourceCS = sourceCM;
            this.data = Buffers.newDirectByteBuffer(width * height * storageComponents);
            return storageCS;
        }

        @Override
        public final void storeRGB(final int x, final int y, final byte r, final byte g, final byte b) {
            int i = ( ( height - y - 1 ) * width + x ) * storageComponents;
            data.put(i++, r);
            data.put(i++, g);
            data.put(i++, b);
            // data.put(i++, (byte)0xff);
        }

        @Override
        public final void store2(final int x, final int y, final byte c1, final byte c2) {
            throw new RuntimeException("not supported yet");
        }

        @Override
        public final void storeYCbCr(final int x, final int y, final byte Y, final byte Cb, final byte Cr) {
            int i = ( ( height - y - 1 ) * width + x ) * storageComponents;
            data.put(i++, Y);
            data.put(i++, Cb);
            data.put(i++, Cr);
        }

        @Override
        public String toString() {
            return "JPEGPixels["+width+"x"+height+", sourceComp "+sourceComponents+", sourceCS "+sourceCS+", storageCS "+storageCS+", storageComp "+storageComponents+"]";
        }
    };

    private JPEGImage(final InputStream in, final ColorSpace cs) throws IOException {
        pixelStorage = new JPEGColorSink(cs);
        final JPEGDecoder decoder = new JPEGDecoder();
        decoder.parse(in);
        pixelWidth = decoder.getWidth();
        pixelHeight = decoder.getHeight();
        decoder.getPixel(pixelStorage, pixelWidth, pixelHeight);
        data = pixelStorage.data;
        final boolean hasAlpha = false;

        bytesPerPixel = 3;
        glFormat = GL.GL_RGB;
        reversedChannels = false; // RGB[A]
        if(DEBUG) {
            System.err.println("JPEGImage: alpha "+hasAlpha+", bytesPerPixel "+bytesPerPixel+
                               ", pixels "+pixelWidth+"x"+pixelHeight+", glFormat 0x"+Integer.toHexString(glFormat));
            System.err.println("JPEGImage: "+decoder);
            System.err.println("JPEGImage: "+pixelStorage);
        }
        decoder.clear(null);
    }
    private final JPEGColorSink pixelStorage;
    private final int pixelWidth, pixelHeight, glFormat, bytesPerPixel;
    private final boolean reversedChannels;
    private final ByteBuffer data;

    /** Returns the color space of the pixel data */
    public ColorSpace getColorSpace() { return pixelStorage.storageCS; }

    /** Returns the number of components of the pixel data */
    public int getComponentCount() { return pixelStorage.storageComponents; }

    /** Returns the width of the image. */
    public int getWidth()    { return pixelWidth; }

    /** Returns the height of the image. */
    public int getHeight()   { return pixelHeight; }

    /** Returns true if data has the channels reversed to BGR or BGRA, otherwise RGB or RGBA is expected. */
    public boolean getHasReversedChannels() { return reversedChannels; }

    /** Returns the OpenGL format for this texture; e.g. GL.GL_LUMINANCE, GL.GL_RGB or GL.GL_RGBA. */
    public int getGLFormat() { return glFormat; }

    /** Returns the OpenGL data type: GL.GL_UNSIGNED_BYTE. */
    public int getGLType() { return GL.GL_UNSIGNED_BYTE; }

    /** Returns the bytes per pixel */
    public int getBytesPerPixel() { return bytesPerPixel; }

    /** Returns the raw data for this texture in the correct
        (bottom-to-top) order for calls to glTexImage2D. */
    public ByteBuffer getData()  { return data; }

    @Override
    public String toString() { return "JPEGImage["+pixelWidth+"x"+pixelHeight+", bytesPerPixel "+bytesPerPixel+", reversedChannels "+reversedChannels+", "+pixelStorage+", "+data+"]"; }
}

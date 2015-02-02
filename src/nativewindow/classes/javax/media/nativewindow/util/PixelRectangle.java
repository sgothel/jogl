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

import java.nio.ByteBuffer;

/**
 * Pixel Rectangle identified by it's {@link #hashCode()}.
 * <p>
 * The {@link #getPixels()} are assumed to be immutable.
 * </p>
 */
public interface PixelRectangle {
    /**
     * <p>
     * Computes a hash code over:
     * <ul>
     *   <li>pixelformat</li>
     *   <li>size</li>
     *   <li>stride</li>
     *   <li>isGLOriented</li>
     *   <li>pixels</li>
     * </ul>
     * </p>
     * <p>
     * The hashCode shall be computed only once with first call
     * and stored for later retrieval to enhance performance.
     * </p>
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    int hashCode();

    /** Returns the {@link PixelFormat}. */
    PixelFormat getPixelformat();

    /** Returns the size, i.e. width and height. */
    DimensionImmutable getSize();

    /**
     * Returns stride in byte-size, i.e. byte count from one line to the next.
     * <p>
     * Must be >= {@link #getPixelformat()}.{@link PixelFormat#bytesPerPixel() bytesPerPixel()} * {@link #getSize()}.{@link DimensionImmutable#getWidth() getWidth()}.
     * </p>
     */
    int getStride();

    /**
     * Returns <code>true</code> if the memory is laid out in
     * OpenGL's coordinate system, <i>origin at bottom left</i>.
     * Otherwise returns <code>false</code>, i.e. <i>origin at top left</i>.
     */
    public boolean isGLOriented();

    /** Returns the pixels. */
    ByteBuffer getPixels();

    @Override
    String toString();

    /**
     * Generic PixelRectangle implementation
     */
    public static class GenericPixelRect implements PixelRectangle {
        protected final PixelFormat pixelformat;
        protected final DimensionImmutable size;
        protected final int strideInBytes;
        protected final boolean isGLOriented;
        protected final ByteBuffer pixels;
        private int hashCode = 0;
        private volatile boolean hashCodeComputed = false;

        /**
         *
         * @param pixelformat
         * @param size
         * @param strideInBytes stride in byte-size, i.e. byte count from one line to the next.
         *                      If not zero, value must be >= <code>width * bytes-per-pixel</code>.
         *                      If zero, stride is set to <code>width * bytes-per-pixel</code>.
         * @param isGLOriented
         * @param pixels
         * @throws IllegalArgumentException if <code>strideInBytes</code> is invalid.
         * @throws IndexOutOfBoundsException if <code>pixels</code> has insufficient bytes left
         */
        public GenericPixelRect(final PixelFormat pixelformat, final DimensionImmutable size, int strideInBytes, final boolean isGLOriented, final ByteBuffer pixels)
                throws IllegalArgumentException, IndexOutOfBoundsException
        {
            if( 0 != strideInBytes ) {
                if( strideInBytes < pixelformat.comp.bytesPerPixel() * size.getWidth()) {
                    throw new IllegalArgumentException("Invalid stride "+strideInBytes+", must be greater than bytesPerPixel "+pixelformat.comp.bytesPerPixel()+" * width "+size.getWidth());
                }
            } else {
                strideInBytes = pixelformat.comp.bytesPerPixel() * size.getWidth();
            }
            final int reqBytes = strideInBytes * size.getHeight();
            if( pixels.limit() < reqBytes ) {
                throw new IndexOutOfBoundsException("Dest buffer has insufficient bytes left, needs "+reqBytes+": "+pixels);
            }
            this.pixelformat = pixelformat;
            this.size = size;
            this.strideInBytes = strideInBytes;
            this.isGLOriented = isGLOriented;
            this.pixels = pixels;
        }

        /**
         * Copy ctor validating src.
         * @param src
         * @throws IllegalArgumentException if <code>strideInBytes</code> is invalid.
         * @throws IndexOutOfBoundsException if <code>pixels</code> has insufficient bytes left
         */
        public GenericPixelRect(final PixelRectangle src)
                throws IllegalArgumentException, IndexOutOfBoundsException
        {
            this(src.getPixelformat(), src.getSize(), src.getStride(), src.isGLOriented(), src.getPixels());
        }

        @Override
        public int hashCode() {
            if( !hashCodeComputed ) { // DBL CHECKED OK VOLATILE
                synchronized (this) {
                    if( !hashCodeComputed ) {
                        // 31 * x == (x << 5) - x
                        int hash = pixelformat.comp.hashCode();
                        hash = ((hash << 5) - hash) + size.hashCode();
                        hash = ((hash << 5) - hash) + strideInBytes;
                        hash = ((hash << 5) - hash) + ( isGLOriented ? 1 : 0);
                        hashCode = ((hash << 5) - hash) + pixels.hashCode();
                        hashCodeComputed = true;
                    }
                }
            }
            return hashCode;
        }

        @Override
        public PixelFormat getPixelformat() {
            return pixelformat;
        }

        @Override
        public DimensionImmutable getSize() {
            return size;
        }

        @Override
        public int getStride() {
            return strideInBytes;
        }

        @Override
        public boolean isGLOriented() {
            return isGLOriented;
        }

        @Override
        public ByteBuffer getPixels() {
            return pixels;
        }

        @Override
        public final String toString() {
            return "PixelRect[obj 0x"+Integer.toHexString(super.hashCode())+", "+pixelformat+", "+size+", stride "+strideInBytes+", isGLOrient "+isGLOriented+", pixels "+pixels+"]";
        }
    }
}


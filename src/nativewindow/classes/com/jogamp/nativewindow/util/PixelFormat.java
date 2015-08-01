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

import java.util.Arrays;

import com.jogamp.common.util.Bitfield;

/**
 * Basic pixel formats
 * <p>
 * Notation follows OpenGL notation, i.e.
 * name consist of all it's component names
 * followed by their bit size.
 * </p>
 * <p>
 * Order of component names is from lowest-bit to highest-bit.
 * </p>
 * <p>
 * In case component-size is 1 byte (e.g. OpenGL data-type GL_UNSIGNED_BYTE),
 * component names are ordered from lowest-byte to highest-byte.
 * Note that OpenGL applies special interpretation if
 * data-type is e.g. GL_UNSIGNED_8_8_8_8_REV or GL_UNSIGNED_8_8_8_8_REV.
 * </p>
 * <p>
 * PixelFormat can be converted to OpenGL GLPixelAttributes
 * via
 * <pre>
 *  GLPixelAttributes glpa = GLPixelAttributes.convert(PixelFormat pixFmt, GLProfile glp);
 * </pre>
 * </p>
 * <p>
 * See OpenGL Specification 4.3 - February 14, 2013, Core Profile,
 * Section 8.4.4 Transfer of Pixel Rectangles, p. 161-174.
 * </ul>
 *
 * </p>
 */
public enum PixelFormat {
    /**
     * Stride is 8 bits, 8 bits per pixel, 1 component of 8 bits.
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_ALPHA (< GL3), GL_RED (>= GL3), data-type GL_UNSIGNED_BYTE</li>
     *   <li>AWT: <i>none</i></li>
     * </ul>
     * </p>
     */
    LUMINANCE(new CType[]{ CType.Y }, 1, 8, 8),

    /**
     * Stride is 16 bits, 16 bits per pixel, 3 {@link PackedComposition#isUniform() discrete} components.
     * <p>
     * The {@link PackedComposition#isUniform() discrete} {@link PixelFormat#composition components}
     * are interleaved in the order Low to High:
     * <ol>
     *   <li>R: 0x1F <<  0</li>
     *   <li>G: 0x3F <<  5</li>
     *   <li>B: 0x1F << 11</li>
     * </ol>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_RGB, data-type GL_UNSIGNED_SHORT_5_6_5_REV</li>
     *   <li>AWT: <i>None</i></li>
     * </ul>
     * </p>
     */
    RGB565(new CType[]{ CType.R, CType.G, CType.B },
           new int[]{   0x1F,    0x3F,    0x1F },
           new int[]{   0,       5,       5+6 },
           16 ),

    /**
     * Stride is 16 bits, 16 bits per pixel, 3 {@link PackedComposition#isUniform() discrete} components.
     * <p>
     * The {@link PackedComposition#isUniform() discrete} {@link PixelFormat#composition components}
     * are interleaved in the order Low to High:
     * <ol>
     *   <li>B: 0x1F <<  0</li>
     *   <li>G: 0x3F <<  5</li>
     *   <li>R: 0x1F << 11</li>
     * </ol>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_RGB, data-type GL_UNSIGNED_SHORT_5_6_5</li>
     *   <li>AWT: <i>None</i></li>
     * </ul>
     * </p>
     */
    BGR565(new CType[]{ CType.B, CType.G, CType.R},
           new int[]{   0x1F,    0x3F,    0x1F },
           new int[]{   0,       5,       5+6 },
           16 ),

    /**
     * Stride is 16 bits, 16 bits per pixel, 4 {@link PackedComposition#isUniform() discrete} components.
     * <p>
     * The {@link PackedComposition#isUniform() discrete} {@link PixelFormat#composition components}
     * are interleaved in the order Low to High:
     * <ol>
     *   <li>R: 0x1F <<  0</li>
     *   <li>G: 0x1F <<  5</li>
     *   <li>B: 0x1F << 10</li>
     *   <li>A: 0x01 << 15</li>
     * </ol>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_RGBA, data-type GL_UNSIGNED_SHORT_1_5_5_5_REV</li>
     *   <li>AWT: <i>None</i></li>
     * </ul>
     * </p>
     */
    RGBA5551(new CType[]{ CType.R, CType.G, CType.B, CType.A},
             new int[]{   0x1F,    0x1F,    0x1F,    0x01 },
             new int[]{   0,          5,    5+5,     5+5+5 },
             16 ),

    /**
     * Stride is 16 bits, 16 bits per pixel, 4 {@link PackedComposition#isUniform() discrete} components.
     * <p>
     * The {@link PackedComposition#isUniform() discrete} {@link PixelFormat#composition components}
     * are interleaved in the order Low to High:
     * <ol>
     *   <li>A: 0x01 <<  0</li>
     *   <li>B: 0x1F <<  1</li>
     *   <li>G: 0x1F <<  6</li>
     *   <li>R: 0x1F << 11</li>
     * </ol>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_RGBA, data-type GL_UNSIGNED_SHORT_5_5_5_1</li>
     *   <li>AWT: <i>None</i></li>
     * </ul>
     * </p>
     */
    ABGR1555(new CType[]{ CType.A, CType.B, CType.G, CType.R },
             new int[]{   0x01,    0x1F,    0x1F,    0x1F },
             new int[]{   0,       1,       1+5,     1+5+5 },
             16 ),

    /**
     * Stride 24 bits, 24 bits per pixel, 3 {@link PackedComposition#isUniform() uniform} components of 8 bits.
     * <p>
     * The {@link PackedComposition#isUniform() uniform} {@link PixelFormat#composition components}
     * are interleaved in the order Low to High:
     * <ol>
     *   <li>R: 0xFF <<  0</li>
     *   <li>G: 0xFF <<  8</li>
     *   <li>B: 0xFF << 16</li>
     * </ol>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_RGB, data-type GL_UNSIGNED_BYTE</li>
     *   <li>AWT: <i>None</i></li>
     * </ul>
     * </p>
     */
    RGB888(new CType[]{ CType.R, CType.G, CType.B }, 3, 8, 24),

    /**
     * Stride is 24 bits, 24 bits per pixel, 3 {@link PackedComposition#isUniform() uniform} components of of 8 bits.
     * <p>
     * The {@link PackedComposition#isUniform() uniform} {@link PixelFormat#composition components}
     * are interleaved in the order Low to High:
     * <ol>
     *   <li>B: 0xFF <<  0</li>
     *   <li>G: 0xFF <<  8</li>
     *   <li>R: 0xFF << 16</li>
     * </ol>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_BGR (>= GL2), data-type GL_UNSIGNED_BYTE</li>
     *   <li>AWT: {@link java.awt.image.BufferedImage#TYPE_3BYTE_BGR TYPE_3BYTE_BGR}</li>
     * </ul>
     * </p>
     */
    BGR888(new CType[]{ CType.B, CType.G, CType.R }, 3, 8, 24),

    /**
     * Stride is 32 bits, 24 bits per pixel, 3 {@link PackedComposition#isUniform() uniform} components of 8 bits.
     * <p>
     * The {@link PackedComposition#isUniform() uniform} {@link PixelFormat#composition components}
     * are interleaved in the order Low to High:
     * <ol>
     *   <li>R: 0xFF <<  0</li>
     *   <li>G: 0xFF <<  8</li>
     *   <li>B: 0xFF << 16</li>
     * </ol>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_RGBA, data-type GL_UNSIGNED_BYTE, with alpha discarded!</li>
     *   <li>AWT: {@link java.awt.image.BufferedImage#TYPE_INT_BGR TYPE_INT_BGR}</li>
     * </ul>
     * </p>
     */
    RGBx8888(new CType[]{ CType.R, CType.G, CType.B }, 3, 8, 32),

    /**
     * Stride is 32 bits, 24 bits per pixel, 3 {@link PackedComposition#isUniform() uniform} components of 8 bits.
     * <p>
     * The {@link PackedComposition#isUniform() uniform} {@link PixelFormat#composition components}
     * are interleaved in the order Low to High:
     * <ol>
     *   <li>B: 0xFF <<  0</li>
     *   <li>G: 0xFF <<  8</li>
     *   <li>R: 0xFF << 16</li>
     * </ol>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_BGRA, data-type GL_UNSIGNED_BYTE - with alpha discarded!</li>
     *   <li>AWT: {@link java.awt.image.BufferedImage#TYPE_INT_RGB TYPE_INT_RGB}</li>
     * </ul>
     * </p>
     */
    BGRx8888(new CType[]{ CType.B, CType.G, CType.R }, 3, 8, 32),

    /**
     * Stride is 32 bits, 32 bits per pixel, 4 {@link PackedComposition#isUniform() uniform} components of 8 bits.
     * <p>
     * The {@link PackedComposition#isUniform() uniform} {@link PixelFormat#composition components}
     * are interleaved in the order Low to High:
     * <ol>
     *   <li>R: 0xFF <<  0</li>
     *   <li>G: 0xFF <<  8</li>
     *   <li>B: 0xFF << 16</li>
     *   <li>A: 0xFF << 24</li>
     * </ol>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_RGBA, data-type GL_UNSIGNED_BYTE</li>
     *   <li>AWT: <i>None</i></li>
     *   <li>PointerIcon: OSX (NSBitmapImageRep)</li>
     *   <li>Window Icon: OSX (NSBitmapImageRep)</li>
     *   <li>PNGJ: Scanlines</li>
     * </ul>
     * </p>
     */
    RGBA8888(new CType[]{ CType.R, CType.G, CType.B, CType.A }, 4, 8, 32),

    /**
     * Stride is 32 bits, 32 bits per pixel, 4 {@link PackedComposition#isUniform() uniform} components of 8 bits.
     * <p>
     * The {@link PackedComposition#isUniform() uniform} {@link PixelFormat#composition components}
     * are interleaved in the order Low to High:
     * <ol>
     *   <li>A: 0xFF <<  0</li>
     *   <li>B: 0xFF <<  8</li>
     *   <li>G: 0xFF << 16</li>
     *   <li>R: 0xFF << 24</li>
     * </ol>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_RGBA, data-type GL_UNSIGNED_INT_8_8_8_8</li>
     *   <li>AWT: {@link java.awt.image.BufferedImage#TYPE_4BYTE_ABGR TYPE_4BYTE_ABGR}</li>
     * </ul>
     * </p>
     */
    ABGR8888(new CType[]{ CType.A, CType.B, CType.G, CType.R }, 4, 8, 32),

    /**
     * Stride is 32 bits, 32 bits per pixel, 4 {@link PackedComposition#isUniform() uniform} components of 8 bits.
     * <p>
     * The {@link PackedComposition#isUniform() uniform} {@link PixelFormat#composition components}
     * are interleaved in the order Low to High:
     * <ol>
     *   <li>A: 0xFF <<  0</li>
     *   <li>R: 0xFF <<  8</li>
     *   <li>G: 0xFF << 16</li>
     *   <li>B: 0xFF << 24</li>
     * </ol>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_BGRA, data-type GL_UNSIGNED_INT_8_8_8_8</li>
     *   <li>AWT: <i>None</i></li>
     * </ul>
     * </p>
     */
    ARGB8888(new CType[]{ CType.A, CType.R, CType.G, CType.B }, 4, 8, 32),

    /**
     * Stride is 32 bits, 32 bits per pixel, 4 {@link PackedComposition#isUniform() uniform} components of 8 bits.
     * <p>
     * The {@link PackedComposition#isUniform() uniform} {@link PixelFormat#composition components}
     * are interleaved in the order Low to High:
     * <ol>
     *   <li>B: 0xFF <<  0</li>
     *   <li>G: 0xFF <<  8</li>
     *   <li>R: 0xFF << 16</li>
     *   <li>A: 0xFF << 24</li>
     * </ol>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_BGRA, data-type GL_UNSIGNED_BYTE</li>
     *   <li>AWT: {@link java.awt.image.BufferedImage#TYPE_INT_ARGB TYPE_INT_ARGB}</li>
     *   <li>PointerIcon: X11 (XCURSOR), Win32, AWT</li>
     *   <li>Window Icon: X11, Win32</li>
     * </ul>
     * </p>
     */
    BGRA8888(new CType[]{ CType.B, CType.G, CType.R, CType.A }, 4, 8, 32);

    /** Unique {@link Composition Pixel Composition}, i.e. layout of its components. */
    public final Composition comp;

    /**
     * @param componentOrder {@link CType Component type} order of all components, see {@link Composition#componentBitMask()}.
     * @param componentCount number of components
     * @param bpc bits per component
     * @param bitStride stride bits to next pixel
     */
    private PixelFormat(final CType[] componentOrder, final int componentCount, final int bpc, final int bitStride) {
        this.comp = new PackedComposition(componentOrder, componentCount, bpc, bitStride);
    }

    /**
     * @param componentOrder {@link CType Component type} order of all components, see {@link Composition#componentBitMask()}.
     * @param componentMask bit-mask of of all components, see {@link Composition##componentBitMask()}.
     * @param componentBitShift bit-shift of all components, see {@link Composition##componentBitMask()}.
     * @param bitStride stride bits to next pixel
     */
    private PixelFormat(final CType[] componentOrder, final int[] componentMask, final int[] componentBitShift, final int bitStride) {
        this.comp = new PackedComposition(componentOrder, componentMask, componentBitShift, bitStride);
    }

    /**
     * Returns the unique matching {@link PixelFormat} of the given {@link Composition}
     * or {@code null} if none is available.
     */
    public static PixelFormat valueOf(final Composition comp) {
        final PixelFormat[] all = PixelFormat.values();
        for(int i=all.length-1; i>=0; i--) {
            final PixelFormat pf = all[i];
            if( comp.hashCode() == pf.comp.hashCode() && comp.equals(pf.comp) ) {
                return pf;
            }
        }
        return null;
    }

    /** Component types */
    public static enum CType {
        /** Red component */
        R,
        /** Green component */
        G,
        /** Blue component */
        B,
        /** Alpha component */
        A,
        /** Luminance component, e.g. grayscale or Y of YUV */
        Y,
        /** U component of YUV */
        U,
        /** V component of YUV */
        V;
    }

    /**
     * Pixel composition, i.e. layout of its components.
     */
    public static interface Composition {
        /** {@value} */
        public static final int UNDEF = -1;

        /**
         * Returns {@code true} if all components are of same bit-size, e.g. {@link PixelFormat#RGBA8888 RGBA8888},
         * otherwise {@code false}, e.g. {@link PixelFormat#RGBA5551 RGBA5551}
         */
        boolean isUniform();

        /**
         * Returns {@code true} if all components are packed, i.e. interleaved, e.g. {@link PixelFormat#RGBA8888 RGBA8888},
         * otherwise {@code false}.
         */
        boolean isInterleaved();

        /** Number of components per pixel, e.g. 3 for {@link PixelFormat#RGBx8888 RGBx8888}. */
        int componentCount();
        /** Number of bits per pixel, e.g. 24 bits for {@link PixelFormat#RGBx8888 RGBx8888}. */
        int bitsPerPixel();
        /**
         * Bit distance between pixels.
         * <p>
         * For packed pixels e.g. 32 bits for {@link PixelFormat#RGBx8888 RGBx8888}.
         * </p>
         */
        int bitStride();
        /** Number of bytes per pixel, i.e. packed {@link #bitStride()} in bytes, e.g. 4 for {@link PixelFormat#RGBx8888 RGBx8888}. */
        int bytesPerPixel();
        /**
         * Returns the {@link CType Component type} order of all components, see {@link #componentBitMask()}.
         */
        CType[] componentOrder();
        /**
         * Returns the index of given {@link CType} within {@link #componentOrder()}, -1 if not exists.
         */
        int find(final PixelFormat.CType s);
        /**
         * Returns the un-shifted bit-mask of all components.
         * <p>
         * Components mask is returned in the order Low-Index to High-Index, e.g.:
         * <ul>
         *   <li>{@link PixelFormat#RGB565 RGB565}: 0: R 0x1F, 1: G 0x3F, 2: B 0x1F</li>
         *   <li>{@link PixelFormat#RGBA5551 RGBA5551}: 0: R 0x1F, 1: G 0x1F, 2: B 0x1F, 3: A 0x01</li>
         *   <li>{@link PixelFormat#RGBA8888 RGBA8888}: 0: R 0xFF, 1: G 0xFF, 2: B 0xFF, 3: A 0xFF</li>
         * </ul>
         * </p>
         * <p>
         */
        int[] componentBitMask();
        /**
         * Returns the number of bits of all components, see {@link #componentBitMask()}.
         */
        int[] componentBitCount();
        /**
         * Returns the bit-shift of all components, see {@link #componentBitMask()}.
         */
        int[] componentBitShift();

        /**
         * Decodes a component from the shifted pixel data with a {@link #bytesPerPixel()} of up to 32bit.
         * @param shifted complete pixel encoded into on 32bit integer
         * @param cIdx the desired component index
         * @return the decoded component value
         */
        int decodeSingleI32(final int shifted, final int cIdx);
        /**
         * Decodes a component from the shifted pixel data with a {@link #bytesPerPixel()} of up to 64bit.
         * @param shifted complete pixel encoded into on 64bit integer
         * @param cIdx the desired component index
         * @return the decoded component value
         */
        int decodeSingleI64(final long shifted, final int cIdx);

        int encodeSingleI32(final int norm, final int cIdx);
        long encodeSingleI64(final int norm, final int cIdx);

        int encode3CompI32(final int c1NormI32, final int c2NormI32, final int c3NormI32);
        int encode4CompI32(final int c1NormI32, final int c2NormI32, final int c3NormI32, final int c4NormI32);

        int encodeSingleI8(final byte normalI8, final int cIdx);
        int encode3CompI8(final byte c1NormI8, final byte c2NormI8, final byte c3NormI8);
        int encode4CompI8(final byte c1NormI8, final byte c2NormI8, final byte c3NormI8, final byte c4NormI8);

        float toFloat(final int i32, final int cIdx, final boolean i32Shifted);
        int fromFloat(final float f, final int cIdx, final boolean shiftResult);

        int defaultValue(final int cIdx, final boolean shiftResult);

        /**
         * Returns cached immutable hash value, see {@link Object#hashCode()}.
         */
        int hashCode();
        /**
         * Returns {@link Object#equals(Object)}
         */
        boolean equals(final Object o);

        /**
         * Returns {@link Object#toString()}.
         */
        String toString();
    }

    /**
     * Packed pixel composition, see {@link Composition}.
     * <p>
     * Components are interleaved, i.e. packed.
     * </p>
     */
    public static class PackedComposition implements Composition {
        private final CType[] compOrder;
        private final int[] compMask;
        private final int[] compBitCount;
        private final int[] compBitShift;
        private final int bitsPerPixel;
        private final int bitStride;
        private final boolean uniform;
        private final int hashCode;

        public final String toString() {
            return String.format("PackedComp[order %s, stride %d, bpp %d, uni %b, comp %d: %s]",
                    Arrays.toString(compOrder), bitStride, bitsPerPixel, uniform,
                    compMask.length, toHexString(compBitCount, compMask, compBitShift));
        }

        /**
         * @param componentOrder {@link CType Component type} order of all components, see {@link #componentBitMask()}.
         * @param componentCount number of components
         * @param bpc bits per component
         * @param bitStride stride bits to next pixel
         */
        public PackedComposition(final CType[] componentOrder, final int componentCount, final int bpc, final int bitStride) {
            this.compOrder = componentOrder;
            this.compMask = new int[componentCount];
            this.compBitShift = new int[componentCount];
            this.compBitCount = new int[componentCount];
            final int compMask = ( 1 << bpc ) - 1;
            for(int i=0; i<componentCount; i++) {
                this.compMask[i] = compMask;
                this.compBitShift[i] = bpc * i;
                this.compBitCount[i] = bpc;
            }
            this.uniform = true;
            this.bitsPerPixel = bpc * componentCount;
            this.bitStride = bitStride;
            if( this.bitStride < this.bitsPerPixel ) {
                throw new IllegalArgumentException(String.format("bit-stride %d < bitsPerPixel %d", this.bitStride, this.bitsPerPixel));
            }
            this.hashCode = hashCodeImpl();
        }

        /**
         * @param componentOrder {@link CType Component type} order of all components, see {@link #componentBitMask()}.
         * @param componentMask bit-mask of of all components, see {@link #componentBitMask()}.
         * @param componentBitShift bit-shift of all components, see {@link #componentBitMask()}.
         * @param bitStride stride bits to next pixel
         */
        public PackedComposition(final CType[] componentOrder, final int[] componentMask, final int[] componentBitShift, final int bitStride) {
            this.compOrder = componentOrder;
            this.compMask = componentMask;
            this.compBitShift = componentBitShift;
            this.compBitCount = new int[componentMask.length];
            int bpp = 0;
            boolean uniform = true;
            for(int i = componentMask.length-1; i>=0; i--) {
                final int cmask = componentMask[i];
                final int bitCount = Bitfield.Util.bitCount(cmask);
                bpp += bitCount;
                this.compBitCount[i] = bitCount;
                if( i > 0 && uniform ) {
                    uniform = componentMask[i-1] == cmask;
                }
            }
            this.uniform = uniform;
            this.bitsPerPixel = bpp;
            this.bitStride = bitStride;
            if( this.bitStride < this.bitsPerPixel ) {
                throw new IllegalArgumentException(String.format("bit-stride %d < bitsPerPixel %d", this.bitStride, this.bitsPerPixel));
            }
            this.hashCode = hashCodeImpl();
        }

        @Override
        public final boolean isUniform() { return uniform; }
        /**
         * {@inheritDoc}
         * <p>
         * Instances of {@link PackedComposition} returns {@code true}.
         * </p>
         */
        @Override
        public final boolean isInterleaved() { return true; }
        @Override
        public final int componentCount() { return compMask.length; }
        @Override
        public final int bitsPerPixel() { return bitsPerPixel; }
        @Override
        public final int bitStride() { return bitStride; }
        @Override
        public final int bytesPerPixel() { return (7+bitStride)/8; }
        @Override
        public final CType[] componentOrder() { return compOrder; }
        @Override
        public final int find(final PixelFormat.CType s) { return PixelFormatUtil.find(s, compOrder, false /* mapRGB2Y */); }
        @Override
        public final int[] componentBitMask() { return compMask; }
        @Override
        public final int[] componentBitCount() { return compBitCount; }
        @Override
        public final int[] componentBitShift() { return compBitShift; }

        @Override
        public final int decodeSingleI32(final int shifted, final int cIdx) {
            return ( shifted >>> compBitShift[cIdx] ) & compMask[cIdx];
        }
        @Override
        public final int decodeSingleI64(final long shifted, final int cIdx) {
            return ( (int)( 0xffffffffL & ( shifted >>> compBitShift[cIdx] ) ) ) & compMask[cIdx];
        }
        @Override
        public final int encodeSingleI32(final int norm, final int cIdx) {
            return ( norm & compMask[cIdx] ) << compBitShift[cIdx] ;
        }
        @Override
        public final long encodeSingleI64(final int norm, final int cIdx) {
            return ( 0xffffffffL & ( norm & compMask[cIdx] ) ) << compBitShift[cIdx] ;
        }
        @Override
        public final int encode3CompI32(final int c1NormI32, final int c2NormI32, final int c3NormI32) {
            return ( c1NormI32 & compMask[0] ) << compBitShift[0] |
                   ( c2NormI32 & compMask[1] ) << compBitShift[1] |
                   ( c3NormI32 & compMask[2] ) << compBitShift[2] ;
        }
        @Override
        public final int encode4CompI32(final int c1NormI32, final int c2NormI32, final int c3NormI32, final int c4NormI32) {
            return ( c1NormI32 & compMask[0] ) << compBitShift[0] |
                   ( c2NormI32 & compMask[1] ) << compBitShift[1] |
                   ( c3NormI32 & compMask[2] ) << compBitShift[2] |
                   ( c4NormI32 & compMask[3] ) << compBitShift[3] ;
        }
        @Override
        public final int encodeSingleI8(final byte normI8, final int cIdx) {
            return ( normI8 & compMask[cIdx] ) << compBitShift[cIdx] ;
        }
        @Override
        public final int encode3CompI8(final byte c1NormI8, final byte c2NormI8, final byte c3NormI8) {
            return ( c1NormI8 & compMask[0] ) << compBitShift[0] |
                   ( c2NormI8 & compMask[1] ) << compBitShift[1] |
                   ( c3NormI8 & compMask[2] ) << compBitShift[2] ;
        }
        @Override
        public final int encode4CompI8(final byte c1NormI8, final byte c2NormI8, final byte c3NormI8, final byte c4NormI8) {
            return ( c1NormI8 & compMask[0] ) << compBitShift[0] |
                   ( c2NormI8 & compMask[1] ) << compBitShift[1] |
                   ( c3NormI8 & compMask[2] ) << compBitShift[2] |
                   ( c4NormI8 & compMask[3] ) << compBitShift[3] ;
        }

        @Override
        public final float toFloat(final int i32, final int cIdx, final boolean i32Shifted) {
            if( i32Shifted ) {
                return ( ( i32 >>> compBitShift[cIdx] ) & compMask[cIdx] ) / (float)( compMask[cIdx] ) ;
            } else {
                return (   i32                          & compMask[cIdx] ) / (float)( compMask[cIdx] ) ;
            }
        }
        @Override
        public final int fromFloat(final float f, final int cIdx, final boolean shiftResult) {
            final int v = (int)(f * compMask[cIdx] + 0.5f);
            return shiftResult ? v << compBitShift[cIdx] : v;
        }

        @Override
        public final int defaultValue(final int cIdx, final boolean shiftResult) {
            final int v = ( CType.A == compOrder[cIdx] || CType.Y == compOrder[cIdx] )
                          ? compMask[cIdx] : 0;
            return shiftResult ? v << compBitShift[cIdx] : v;
        }

        @Override
        public final int hashCode() { return hashCode; }
        private final int hashCodeImpl() {
            // 31 * x == (x << 5) - x
            int hash = 31 + bitStride;
            hash = ((hash << 5) - hash) + bitsPerPixel;
            hash = ((hash << 5) - hash) + compMask.length;
            for(int i=compOrder.length-1; i>=0; i--) {
                hash = ((hash << 5) - hash) + compOrder[i].ordinal();
            }
            for(int i=compMask.length-1; i>=0; i--) {
                hash = ((hash << 5) - hash) + compMask[i];
            }
            for(int i=compBitShift.length-1; i>=0; i--) {
                hash = ((hash << 5) - hash) + compBitShift[i];
            }
            return hash;
        }

        @Override
        public final boolean equals(final Object obj) {
            if(this == obj)  { return true; }
            if( obj instanceof PackedComposition ) {
                final PackedComposition other = (PackedComposition) obj;
                return bitStride == other.bitStride &&
                       bitsPerPixel == other.bitsPerPixel &&
                       Arrays.equals(compOrder, other.compOrder) &&
                       Arrays.equals(compMask, other.compMask) &&
                       Arrays.equals(compBitShift, other.compBitShift);
            } else {
                return false;
            }
        }
    }

    private static String toHexString(final int[] bitCount, final int[] mask, final int[] shift) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");
        final int l = mask.length;
        for(int i=0; i < l; i++) {
            if(i > 0) {
                sb.append(", ");
            }
            sb.append(bitCount[i]).append(": ").
            append("0x").append(Integer.toHexString(mask[i])).append(" << ").append(shift[i]);
        }
        return sb.append("]").toString();
    }
}

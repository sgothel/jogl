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
     * Pixel size is 1 bytes (8 bits) with one component of size 1 byte (8 bits).
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_ALPHA (< GL3), GL_RED (>= GL3), data-type GL_UNSIGNED_BYTE</li>
     *   <li>AWT: <i>none</i></li>
     * </ul>
     * </p>
     */
    LUMINANCE(1, 8),

    /**
     * Pixel size is 3 bytes (24 bits) with each component of size 1 byte (8 bits).
     * <p>
     * The components are interleaved in the order:
     * <ul>
     *   <li>Low to High: R, G, B</li>
     * </ul>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_RGB, data-type GL_UNSIGNED_BYTE</li>
     *   <li>AWT: <i>None</i></li>
     * </ul>
     * </p>
     */
    RGB888(3, 24),

    /**
     * Pixel size is 3 bytes (24 bits) with each component of size 1 byte (8 bits).
     * <p>
     * The components are interleaved in the order:
     * <ul>
     *   <li>Low to High: B, G, R</li>
     * </ul>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_BGR (>= GL2), data-type GL_UNSIGNED_BYTE</li>
     *   <li>AWT: {@link java.awt.image.BufferedImage#TYPE_3BYTE_BGR TYPE_3BYTE_BGR}</li>
     * </ul>
     * </p>
     */
    BGR888(3, 24),

    /**
     * Pixel size is 4 bytes (32 bits) with each component of size 1 byte (8 bits).
     * <p>
     * The components are interleaved in the order:
     * <ul>
     *   <li>Low to High: R, G, B, A</li>
     * </ul>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_RGBA, data-type GL_UNSIGNED_BYTE</li>
     *   <li>AWT: <i>None</i></li>
     *   <li>PointerIcon: X11 (XCURSOR)</li>
     *   <li>PNGJ: Scanlines</li>
     * </ul>
     * </p>
     */
    RGBA8888(4, 32),

    /**
     * Pixel size is 4 bytes (32 bits) with each component of size 1 byte (8 bits).
     * <p>
     * The components are interleaved in the order:
     * <ul>
     *   <li>Low to High: A, B, G, R</li>
     * </ul>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_RGBA, data-type GL_UNSIGNED_8_8_8_8</li>
     *   <li>AWT: {@link java.awt.image.BufferedImage#TYPE_4BYTE_ABGR TYPE_4BYTE_ABGR}</li>
     * </ul>
     * </p>
     */
    ABGR8888(4, 32),

    /**
     * Pixel size is 4 bytes (32 bits) with each component of size 1 byte (8 bits).
     * <p>
     * The components are interleaved in the order:
     * <ul>
     *   <li>Low to High: A, R, G, B</li>
     * </ul>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_BGRA, data-type GL_UNSIGNED_INT_8_8_8_8</li>
     *   <li>AWT: <i>None</i></li>
     * </ul>
     * </p>
     */
    ARGB8888(4, 32),

    /**
     * Pixel size is 4 bytes (32 bits) with each component of size 1 byte (8 bits).
     * <p>
     * The components are interleaved in the order:
     * <ul>
     *   <li>Low to High: B, G, R, A</li>
     * </ul>
     * </p>
     * <p>
     * Compatible with:
     * <ul>
     *   <li>OpenGL: data-format GL_BGRA, data-type GL_UNSIGNED_BYTE</li>
     *   <li>AWT: {@link java.awt.image.BufferedImage#TYPE_INT_ARGB TYPE_INT_ARGB}</li>
     *   <li>PointerIcon: Win32, OSX (NSBitmapImageRep), AWT</li>
     *   <li>Window Icon: X11, Win32, OSX (NSBitmapImageRep)</li>
     * </ul>
     * </p>
     */
    BGRA8888(4, 32);

    /** Number of components per pixel, e.g. 4 for RGBA. */
    public final int componentCount;
    /** Number of bits per pixel, e.g. 32 for RGBA. */
    public final int bitsPerPixel;
    /** Number of bytes per pixel, e.g. 4 for RGBA. */
    public final int bytesPerPixel() { return (7+bitsPerPixel)/8; }

    private PixelFormat(int componentCount, int bpp) {
        this.componentCount = componentCount;
        this.bitsPerPixel = bpp;
    }
}

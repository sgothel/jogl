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
package com.jogamp.opengl.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.TextureData;

/**
 * OpenGL pixel data buffer, allowing user to provide buffers via their {@link GLPixelBufferProvider} implementation.
 * <p>
 * {@link GLPixelBufferProvider} produces a {@link GLPixelBuffer}.
 * </p>
 * <p>
 * You may use {@link #defaultProviderNoRowStride}.
 * </p>
 */
public class GLPixelBuffer {

    /** Allows user to interface with another toolkit to define {@link GLPixelAttributes} and memory buffer to produce {@link TextureData}. */
    public static interface GLPixelBufferProvider {
        /** Allow {@link GL2ES3#GL_PACK_ROW_LENGTH}, or {@link GL2ES2#GL_UNPACK_ROW_LENGTH}. */
        boolean getAllowRowStride();

        /**
         * Returns RGB[A] {@link GLPixelAttributes} matching {@link GL}, {@code componentCount} and {@code pack}.
         *
         * @param gl the corresponding current {@link GL} context object
         * @param componentCount RGBA component count, i.e. 1 (luminance, alpha or red), 3 (RGB) or 4 (RGBA)
         * @param pack {@code true} for read mode GPU -> CPU, e.g. {@link GL#glReadPixels(int, int, int, int, int, int, Buffer) glReadPixels}.
         *             {@code false} for write mode CPU -> GPU, e.g. {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, Buffer) glTexImage2D}.
         */
        GLPixelAttributes getAttributes(GL gl, int componentCount, boolean pack);

        /**
         * Returns the host {@link PixelFormat.Composition} matching {@link GL} and {@code componentCount}
         * if required by implementation, otherwise {@code null}.
         *
         * @param glp the corresponding current {@link GL} context object
         * @param componentCount RGBA component count, i.e. 1 (luminance, alpha or red), 3 (RGB) or 4 (RGBA)
         */
        PixelFormat.Composition getHostPixelComp(final GLProfile glp, final int componentCount);

        /**
         * Allocates a new {@link GLPixelBuffer} object.
         * <p>
         * The minimum required {@link Buffer#remaining() remaining} byte size equals to <code>minByteSize</code>, if &gt; 0,
         * otherwise utilize {@link GLBuffers#sizeof(GL, int[], int, int, int, int, int, boolean)}
         * to calculate it.
         * </p>
         *
         * @param gl the corresponding current {@link GL} context object
         * @param hostPixComp host {@link PixelFormat pixel format}, i.e. of the source or sink depending on {@code pack},
         *                    e.g. fetched via {@link #getHostPixelComp(GLProfile, int)}.
         *                    If {@code null}, {@code pixelAttributes} instance maybe used or an exception is thrown,
         *                    depending on implementation semantics.
         * @param pixelAttributes the desired {@link GLPixelAttributes}, e.g. fetched via {@link #getAttributes(GL, int, boolean)}
         * @param pack {@code true} for read mode GPU -> CPU, e.g. {@link GL#glReadPixels(int, int, int, int, int, int, Buffer) glReadPixels}.
         *             {@code false} for write mode CPU -> GPU, e.g. {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, Buffer) glTexImage2D}.
         * @param width in pixels
         * @param height in pixels
         * @param depth in pixels
         * @param minByteSize if &gt; 0, the pre-calculated minimum byte-size for the resulting buffer, otherwise ignore.
         * @see #getHostPixelComp(GLProfile, int)
         * @see #getAttributes(GL, int, boolean)
         */
        GLPixelBuffer allocate(GL gl, PixelFormat.Composition hostPixComp, GLPixelAttributes pixelAttributes,
                               boolean pack, int width, int height, int depth, int minByteSize);
    }

    /** Single {@link GLPixelBuffer} provider. */
    public static interface SingletonGLPixelBufferProvider extends GLPixelBufferProvider {
        /**
         * {@inheritDoc}
         * <p>
         * Being called to gather the initial {@link GLPixelBuffer},
         * or a new replacement {@link GLPixelBuffer} if {@link GLPixelBuffer#requiresNewBuffer(GL, int, int, int)}.
         * </p>
         */
        @Override
        GLPixelBuffer allocate(GL gl, PixelFormat.Composition hostPixComp, GLPixelAttributes pixelAttributes,
                               boolean pack, int width, int height, int depth, int minByteSize);

        /**
         * Return the last {@link #allocate(GL, PixelFormat.Composition, GLPixelAttributes, boolean, int, int, int, int) allocated} {@link GLPixelBuffer}
         * matching the given parameter.
         * <p>
         * May return {@code null} if none has been allocated yet.
         * </p>
         * <p>
         * Returned {@link GLPixelBuffer} may be {@link GLPixelBuffer#isValid() invalid}.
         * </p>
         * @param hostPixComp host {@link PixelFormat pixel format}, i.e. of the source or sink depending on {@code pack},
         *                    e.g. fetched via {@link #getHostPixelComp(GLProfile, int)}.
         *                    If {@code null}, {@code pixelAttributes} instance maybe used or an exception is thrown,
         *                    depending on implementation semantics.
         * @param pixelAttributes the desired {@link GLPixelAttributes}, e.g. fetched via {@link #getAttributes(GL, int, boolean)}
         * @param pack {@code true} for read mode GPU -> CPU, e.g. {@link GL#glReadPixels(int, int, int, int, int, int, Buffer) glReadPixels}.
         *             {@code false} for write mode CPU -> GPU, e.g. {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, Buffer) glTexImage2D}.
         */
        GLPixelBuffer getSingleBuffer(PixelFormat.Composition hostPixelComp, GLPixelAttributes pixelAttributes, boolean pack);
        /**
         * Initializes the single {@link GLPixelBuffer} w/ a given size,
         * if not yet {@link #allocate(GL, PixelFormat.Composition, GLPixelAttributes, boolean, int, int, int, int) allocated}.
         *
         * @param glp
         * @param componentCount RGBA component count, i.e. 1 (luminance, alpha or red), 3 (RGB) or 4 (RGBA)
         * @param pack {@code true} for read mode GPU -> CPU, e.g. {@link GL#glReadPixels(int, int, int, int, int, int, Buffer) glReadPixels}.
         *             {@code false} for write mode CPU -> GPU, e.g. {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, Buffer) glTexImage2D}.
         * @param width
         * @param height
         * @param depth
         * @return the newly initialized single {@link GLPixelBuffer}, or null if already allocated.
         */
        GLPixelBuffer initSingleton(GLProfile glp, int componentCount, boolean pack, int width, int height, int depth);

        /** Dispose all resources.*/
        void dispose();
    }

    public static class DefaultGLPixelBufferProvider implements GLPixelBufferProvider {
        private final boolean allowRowStride;

        /**
         * @param allowRowStride If <code>true</code>, allow row-stride, otherwise not.
         * See {@link #getAllowRowStride()} and {@link GLPixelBuffer#requiresNewBuffer(GL, int, int, int)}.
         */
        public DefaultGLPixelBufferProvider(final boolean allowRowStride) {
            this.allowRowStride = allowRowStride;
        }

        @Override
        public boolean getAllowRowStride() { return allowRowStride; }

        @Override
        public GLPixelAttributes getAttributes(final GL gl, final int componentCount, final boolean pack) {
            final GLPixelAttributes res = GLPixelAttributes.convert(gl, componentCount, pack);
            if( null == res ) {
                throw new GLException("Unsupported componentCount "+componentCount+", contact maintainer to enhance");
            } else {
                return res;
            }
        }

        /**
         * {@inheritDoc}
         * <p>
         * Returns {@code null}!
         * </p>
         */
        @Override
        public PixelFormat.Composition getHostPixelComp(final GLProfile glp, final int componentCount) {
            return null;
        }

        /**
         * {@inheritDoc}
         * <p>
         * Returns an NIO {@link ByteBuffer}.
         * </p>
         */
        @Override
        public GLPixelBuffer allocate(final GL gl, final PixelFormat.Composition hostPixComp, final GLPixelAttributes pixelAttributes,
                                      final boolean pack, final int width, final int height, final int depth, final int minByteSize) {
            // unused: hostPixComp
            if( minByteSize > 0 ) {
                return new GLPixelBuffer(pixelAttributes, pack, width, height, depth, Buffers.newDirectByteBuffer(minByteSize), getAllowRowStride());
            } else {
                final int[] tmp = { 0 };
                final int byteSize = GLBuffers.sizeof(gl, tmp, pixelAttributes.pfmt.comp.bytesPerPixel(), width, height, depth, pack);
                return new GLPixelBuffer(pixelAttributes, pack, width, height, depth, Buffers.newDirectByteBuffer(byteSize), getAllowRowStride());
            }
        }
    }

    /**
     * Default {@link GLPixelBufferProvider} with {@link GLPixelBufferProvider#getAllowRowStride()} == <code>false</code>,
     * utilizing best match for {@link GLPixelAttributes}
     * and {@link GLPixelBufferProvider#allocate(GL, PixelFormat.Composition, GLPixelAttributes, boolean, int, int, int, int) allocating} a {@link ByteBuffer}.
     */
    public static final GLPixelBufferProvider defaultProviderNoRowStride = new DefaultGLPixelBufferProvider(false);

    /**
     * Default {@link GLPixelBufferProvider} with {@link GLPixelBufferProvider#getAllowRowStride()} == <code>true</code>,
     * utilizing best match for {@link GLPixelAttributes}
     * and {@link GLPixelBufferProvider#allocate(GL, PixelFormat.Composition, GLPixelAttributes, boolean, int, int, int, int) allocating} a {@link ByteBuffer}.
     */
    public static final GLPixelBufferProvider defaultProviderWithRowStride = new DefaultGLPixelBufferProvider(true);

    /** Pixel attributes. */
    public static class GLPixelAttributes {
        /** Undefined instance of {@link GLPixelAttributes}, having componentCount:=0, format:=0 and type:= 0. */
        public static final GLPixelAttributes UNDEF = new GLPixelAttributes(null, PixelFormat.LUMINANCE, 0, 0, true, false);

        /**
         * Returns the matching {@link PixelFormat} for the given GL format and type if exists,
         * otherwise returns <code>null</code>.
         */
        public static final PixelFormat getPixelFormat(final int glFormat, final int glDataType) {
            PixelFormat pixFmt = null;

            switch(glFormat) {
                case GL.GL_ALPHA:
                case GL.GL_LUMINANCE:
                case GL2ES2.GL_RED:
                    pixFmt = PixelFormat.LUMINANCE;
                    break;
                case GL.GL_RGB:
                    switch(glDataType) {
                        case GL2GL3.GL_UNSIGNED_SHORT_5_6_5_REV:
                            pixFmt = PixelFormat.RGB565;
                            break;
                        case GL.GL_UNSIGNED_SHORT_5_6_5:
                            pixFmt = PixelFormat.BGR565;
                            break;
                        case GL.GL_UNSIGNED_BYTE:
                            pixFmt = PixelFormat.RGB888;
                            break;
                    }
                    break;
                case GL.GL_RGBA:
                    switch(glDataType) {
                        case GL2GL3.GL_UNSIGNED_SHORT_1_5_5_5_REV:
                            pixFmt = PixelFormat.RGBA5551;
                            break;
                        case GL.GL_UNSIGNED_SHORT_5_5_5_1:
                            pixFmt = PixelFormat.ABGR1555;
                            break;
                        case GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV:
                            // fall through intended
                        case GL.GL_UNSIGNED_BYTE:
                            pixFmt = PixelFormat.RGBA8888;
                            break;
                        case GL2GL3.GL_UNSIGNED_INT_8_8_8_8:
                            pixFmt = PixelFormat.ABGR8888;
                            break;
                    }
                    break;
                case GL.GL_BGR:
                    if( GL.GL_UNSIGNED_BYTE == glDataType ) {
                        pixFmt = PixelFormat.BGR888;
                    }
                    break;
                case GL.GL_BGRA:
                    switch(glDataType) {
                        case GL2GL3.GL_UNSIGNED_INT_8_8_8_8:
                            pixFmt = PixelFormat.ARGB8888;
                            break;
                        case GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV:
                            // fall through intended
                        case GL.GL_UNSIGNED_BYTE:
                            pixFmt = PixelFormat.BGRA8888;
                            break;
                    }
                    break;
            }
            return pixFmt;
        }

        /**
         * Returns the matching {@link GLPixelAttributes} for the given byte sized RGBA {@code componentCount} and {@link GL} if exists,
         * otherwise returns {@code null}.
         *
         * @param gl the corresponding current {@link GL} context object
         * @param componentCount RGBA component count, i.e. 1 (luminance, alpha or red), 3 (RGB) or 4 (RGBA)
         * @param pack {@code true} for read mode GPU -> CPU, e.g. {@link GL#glReadPixels(int, int, int, int, int, int, Buffer) glReadPixels}.
         *             {@code false} for write mode CPU -> GPU, e.g. {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, Buffer) glTexImage2D}.
         */
        public static GLPixelAttributes convert(final GL gl, final int componentCount, final boolean pack) {
            final int dFormat, dType;
            final boolean glesReadMode = pack && gl.isGLES();

            if( 1 == componentCount && !glesReadMode ) {
                if( gl.isGL3ES3() ) {
                    // RED is supported on ES3 and >= GL3 [core]; ALPHA is deprecated on core
                    dFormat = GL2ES2.GL_RED;
                } else {
                    // ALPHA is supported on ES2 and GL2, i.e. <= GL3 [core] or compatibility
                    dFormat = GL.GL_ALPHA;
                }
                dType   = GL.GL_UNSIGNED_BYTE;
            } else if( 3 == componentCount && !glesReadMode ) {
                dFormat = GL.GL_RGB;
                dType   = GL.GL_UNSIGNED_BYTE;
            } else if( 4 == componentCount || glesReadMode ) {
                final GLContext ctx = gl.getContext();
                final int _dFormat = ctx.getDefaultPixelDataFormat();
                final int dComps = GLBuffers.componentCount(_dFormat);
                if( dComps == componentCount || 4 == dComps ) { // accept if desired component count or 4 components
                    dFormat = _dFormat;
                    dType   = ctx.getDefaultPixelDataType();
                } else {
                    dFormat = GL.GL_RGBA;
                    dType   = GL.GL_UNSIGNED_BYTE;
                }
            } else {
                return null;
            }
            return new GLPixelAttributes(dFormat, dType);
        }

        /**
         * Returns the matching {@link GLPixelAttributes} for the given {@link GLProfile}, {@link PixelFormat} and {@code pack} if exists,
         * otherwise returns {@code null}.
         * @param glp the corresponding {@link GLProfile}
         * @param pixFmt the to be matched {@link PixelFormat pixel format}
         * @param pack {@code true} for read mode GPU -> CPU, e.g. {@link GL#glReadPixels(int, int, int, int, int, int, Buffer) glReadPixels}.
         *             {@code false} for write mode CPU -> GPU, e.g. {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, Buffer) glTexImage2D}.
         */
        public static final GLPixelAttributes convert(final GLProfile glp, final PixelFormat pixFmt, final boolean pack) {
            final int[] df = new int[1];
            final int[] dt = new int[1];
            convert(glp, pixFmt, pack, df, dt);
            if( 0 != df[0] ) {
                return new GLPixelAttributes(null, pixFmt, df[0], dt[0], true /* not used */, true);
            }
            return null;
        }
        private static final int convert(final GLProfile glp, final PixelFormat pixFmt, final boolean pack,
                                         final int[] dfRes, final int[] dtRes) {
            final boolean glesReadMode = pack && glp.isGLES();
            int df = 0; // format
            int dt = GL.GL_UNSIGNED_BYTE; // data type
            switch(pixFmt) {
                case LUMINANCE:
                    if( !glesReadMode ) {
                        if( glp.isGL3ES3() ) {
                            // RED is supported on ES3 and >= GL3 [core]; ALPHA/LUMINANCE is deprecated on core
                            df = GL2ES2.GL_RED;
                        } else {
                            // ALPHA/LUMINANCE is supported on ES2 and GL2, i.e. <= GL3 [core] or compatibility
                            df = GL.GL_LUMINANCE;
                        }
                    }
                    break;
                case RGB565:
                    if( glp.isGL2GL3() ) {
                        df = GL.GL_RGB; dt = GL2GL3.GL_UNSIGNED_SHORT_5_6_5_REV;
                    }
                    break;
                case BGR565:
                    if( glp.isGL2GL3() ) {
                        df = GL.GL_RGB; dt = GL.GL_UNSIGNED_SHORT_5_6_5;
                    }
                    break;
                case RGBA5551:
                    if( glp.isGL2GL3() ) {
                        df = GL.GL_RGBA; dt = GL2GL3.GL_UNSIGNED_SHORT_1_5_5_5_REV;
                    }
                    break;
                case ABGR1555:
                    if( glp.isGL2GL3() ) {
                        df = GL.GL_RGBA; dt = GL.GL_UNSIGNED_SHORT_5_5_5_1;
                    }
                    break;
                case RGB888:
                    if( !glesReadMode ) {
                        df = GL.GL_RGB;
                    }
                    break;
                case BGR888:
                    if( glp.isGL2GL3() ) {
                        df = GL.GL_BGR;
                    }
                    break;
                case RGBx8888:
                case RGBA8888:
                    df = GL.GL_RGBA;
                    break;
                case ABGR8888:
                    if( glp.isGL2GL3() ) {
                        df = GL.GL_RGBA; dt = GL2GL3.GL_UNSIGNED_INT_8_8_8_8;
                    }
                    break;
                case ARGB8888:
                    if( glp.isGL2GL3() ) {
                        df = GL.GL_BGRA; dt = GL2GL3.GL_UNSIGNED_INT_8_8_8_8;
                    }
                    break;
                case BGRx8888:
                case BGRA8888:
                    if( glp.isGL2GL3() ) { // FIXME: or if( !glesReadMode ) ? BGRA n/a on GLES
                        df = GL.GL_BGRA;
                    }
                    break;
            }
            dfRes[0] = df;
            dtRes[0] = dt;
            return df;
        }

        /** The OpenGL pixel data format */
        public final int format;
        /** The OpenGL pixel data type  */
        public final int type;

        /** {@link PixelFormat} describing the {@link PixelFormat.Composition component} layout */
        public final PixelFormat pfmt;

        @Override
        public final int hashCode() {
            // 31 * x == (x << 5) - x
            int hash = pfmt.hashCode();
            hash = ((hash << 5) - hash) + format;
            return ((hash << 5) - hash) + type;
        }

        @Override
        public final boolean equals(final Object obj) {
            if(this == obj)  { return true; }
            if( obj instanceof GLPixelAttributes ) {
                final GLPixelAttributes other = (GLPixelAttributes) obj;
                return format == other.format &&
                       type == other.type &&
                       pfmt.equals(other.pfmt);
            } else {
                return false;
            }
        }

        /**
         * Create a new {@link GLPixelAttributes} instance based on GL format and type.
         * @param dataFormat GL data format
         * @param dataType GL data type
         * @throws GLException if {@link PixelFormat} could not be determined, see {@link #getPixelFormat(int, int)}.
         */
        public GLPixelAttributes(final int dataFormat, final int dataType) throws GLException {
            this(null, null, dataFormat, dataType, true /* not used */, true);
        }

        /**
         * Create a new {@link GLPixelAttributes} instance based on {@link GLProfile}, {@link PixelFormat} and {@code pack}.
         * @param glp the corresponding {@link GLProfile}
         * @param pixFmt the to be matched {@link PixelFormat pixel format}
         * @param pack {@code true} for read mode GPU -> CPU, e.g. {@link GL#glReadPixels(int, int, int, int, int, int, Buffer) glReadPixels}.
         *             {@code false} for write mode CPU -> GPU, e.g. {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, Buffer) glTexImage2D}.
         * @throws GLException if GL format or type could not be determined, see {@link #convert(GLProfile, PixelFormat, boolean)}.
         */
        public GLPixelAttributes(final GLProfile glp, final PixelFormat pixFmt, final boolean pack) throws GLException {
            this(glp, pixFmt, 0, 0, pack, true);
        }

        private GLPixelAttributes(final GLProfile glp, final PixelFormat pixFmt,
                                  final int dataFormat, final int dataType, final boolean pack, final boolean checkArgs) throws GLException {
            if( checkArgs && ( 0 == dataFormat || 0 == dataType ) ) {
                if( null == pixFmt || null == glp ) {
                    throw new GLException("Zero format and/or type w/o pixFmt or glp: "+this);
                }
                final int[] df = new int[1];
                final int[] dt = new int[1];
                if( 0 == convert(glp, pixFmt, pack, df, dt) ) {
                    throw new GLException("Could not find format and type for "+pixFmt+" and "+glp+", "+this);
                }
                this.format = df[0];
                this.type = dt[0];
                this.pfmt = pixFmt;
            } else {
                this.format = dataFormat;
                this.type = dataType;
                this.pfmt = null != pixFmt ? pixFmt : getPixelFormat(dataFormat, dataType);
                if( null == this.pfmt ) {
                    throw new GLException("Could not find PixelFormat for format and/or type: "+this);
                }
            }
            if( checkArgs ) {
                final int bytesPerPixel = GLBuffers.bytesPerPixel(this.format, this.type);
                if( 0 == bytesPerPixel ) {
                    throw new GLException("Zero bytesPerPixel: "+this);
                }
            }
        }

        @Override
        public String toString() {
            return "PixelAttributes[fmt 0x"+Integer.toHexString(format)+", type 0x"+Integer.toHexString(type)+", "+pfmt+"]";
        }
    }

    /** The {@link GLPixelAttributes}. */
    public final GLPixelAttributes pixelAttributes;
    /**
     * Width in pixels, representing {@link #buffer}'s {@link #byteSize}.
     * <p>
     * May not represent actual image width as user may re-use buffer for different dimensions, see {@link #requiresNewBuffer(GL, int, int, int)}.
     * </p>
     */
    public final int width;
    /**
     * Height in pixels, representing {@link #buffer}'s {@link #byteSize}.
     * <p>
     * May not represent actual image height as user may re-use buffer for different dimensions, see {@link #requiresNewBuffer(GL, int, int, int)}.
     * </p>
     */
    public final int height;
    /** Depth in pixels. */
    public final int depth;
    /**
     * Data packing direction.
     * <p>{@code true} for read mode GPU -> CPU, e.g. {@link GL#glReadPixels(int, int, int, int, int, int, Buffer) glReadPixels}.</p>
     * <p>{@code false} for write mode CPU -> GPU, e.g. {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, Buffer) glTexImage2D}.</p>
     */
    public final boolean pack;
    /** Byte size of the buffer. Actually the number of {@link Buffer#remaining()} bytes when passed in ctor. */
    public final int byteSize;
    /**
     * Buffer holding the pixel data. If {@link #rewind()}, it holds <code>byteSize</code> {@link Buffer#remaining()} bytes.
     * <p>
     * By default the {@link Buffer} is a {@link ByteBuffer}, due to {@link DefProvider#allocate(GL, PixelFormat.Composition, GLPixelAttributes, boolean, int, int, int, int)}.
     * However, other {@link GLPixelBufferProvider} may utilize different {@link Buffer} types.
     * </p>
     */
    public final Buffer buffer;
    /** Buffer element size in bytes. */
    public final int bufferElemSize;

    /** Allow {@link GL2ES3#GL_PACK_ROW_LENGTH}, or {@link GL2ES2#GL_UNPACK_ROW_LENGTH}. See {@link #requiresNewBuffer(GL, int, int, int)}. */
    public final boolean allowRowStride;

    private boolean disposed = false;

    public StringBuilder toString(StringBuilder sb) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        sb.append(pixelAttributes).append(", dim ").append(width).append("x").append(height).append("x").append(depth).append(", pack ").append(pack)
        .append(", disposed ").append(disposed).append(", valid ").append(isValid())
        .append(", buffer[bytes ").append(byteSize).append(", elemSize ").append(bufferElemSize).append(", ").append(buffer).append("]");
        return sb;
    }
    @Override
    public String toString() {
        return "GLPixelBuffer["+toString(null).toString()+"]";
    }

    /**
     * @param pixelAttributes the desired {@link GLPixelAttributes}
     * @param pack {@code true} for read mode GPU -> CPU, e.g. {@link GL#glReadPixels(int, int, int, int, int, int, Buffer) glReadPixels}.
     *             {@code false} for write mode CPU -> GPU, e.g. {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, Buffer) glTexImage2D}.
     * @param width in pixels
     * @param height in pixels
     * @param depth in pixels
     * @param buffer the backing array
     * @param allowRowStride If <code>true</code>, allow row-stride, otherwise not. See {@link #requiresNewBuffer(GL, int, int, int)}.
     * @param hostPixelComp the host {@link PixelFormat.Composition}
     */
    public GLPixelBuffer(final GLPixelAttributes pixelAttributes, final boolean pack, final int width, final int height, final int depth, final Buffer buffer, final boolean allowRowStride) {
        this.pixelAttributes = pixelAttributes;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.pack = pack;
        this.buffer = buffer;
        this.byteSize = Buffers.remainingBytes(buffer);
        this.bufferElemSize = Buffers.sizeOfBufferElem(buffer);
        this.allowRowStride = allowRowStride;
    }

    /** Allow {@link GL2ES3#GL_PACK_ROW_LENGTH}, or {@link GL2ES2#GL_UNPACK_ROW_LENGTH}. */
    public final boolean getAllowRowStride() { return allowRowStride; }

    /** Is not {@link #dispose() disposed} and has {@link #byteSize} &gt; 0. */
    public boolean isValid() {
        return !disposed && 0 < byteSize;
    }

    /** See {@link Buffer#rewind()}. */
    public Buffer rewind() {
        return buffer.rewind();
    }

    /** Returns the byte position of the {@link #buffer}. */
    public int position() {
        return buffer.position() * bufferElemSize;
    }

    /** Sets the byte position of the {@link #buffer}. */
    public Buffer position(final int bytePos) {
        return buffer.position( bytePos / bufferElemSize );
    }

    /** Returns the byte capacity of the {@link #buffer}. */
    public int capacity() {
        return buffer.capacity() * bufferElemSize;
    }

    /** Returns the byte limit of the {@link #buffer}. */
    public int limit() {
        return buffer.limit() * bufferElemSize;
    }

    /** See {@link Buffer#flip()}. */
    public Buffer flip() {
        return buffer.flip();
    }

    /** See {@link Buffer#clear()}. */
    public Buffer clear() {
        return buffer.clear();
    }

    /**
     * Returns true, if {@link #isValid() invalid} or implementation requires a new buffer based on the new size
     * due to pixel alignment or byte size, otherwise false.
     * <p>
     * It is assumed that <code>pixelAttributes</code>, <code>depth</code> and <code>pack</code> stays the same!
     * </p>
     * <p>
     * The minimum required byte size equals to <code>minByteSize</code>, if &gt; 0,
     * otherwise {@link GLBuffers#sizeof(GL, int[], int, int, int, int, int, boolean) GLBuffers.sizeof(..)}
     * is being used to calculate it. This value is referred to <i>newByteSize</i>.
     * </p>
     * <p>
     * If <code>{@link #allowRowStride} = false</code>,
     * method returns <code>true</code> if the <i>newByteSize</i> &gt; <i>currentByteSize</i>
     * or the <code>newWidth</code> != <code>currentWidth</code>.
     * </p>
     * <p>
     * If <code>{@link #allowRowStride} = true</code>, see {@link GLPixelBufferProvider#getAllowRowStride()},
     * method returns <code>true</code> only if the <i>newByteSize</i> &gt; <i>currentByteSize</i>.
     * Assuming user utilizes the row-stride when dealing w/ the data, i.e. {@link GL2ES3#GL_PACK_ROW_LENGTH}.
     * </p>
     * @param gl the corresponding current GL context object
     * @param newWidth new width in pixels
     * @param newHeight new height in pixels
     * @param newByteSize if &gt; 0, the pre-calculated minimum byte-size for the resulting buffer, otherwise ignore.
     * @see GLPixelBufferProvider#allocate(GL, PixelFormat.Composition, GLPixelAttributes, boolean, int, int, int, int)
     */
    public boolean requiresNewBuffer(final GL gl, final int newWidth, final int newHeight, int newByteSize) {
        if( !isValid() ) {
            return true;
        }
        if( 0 >= newByteSize ) {
            final int[] tmp = { 0 };
            newByteSize = GLBuffers.sizeof(gl, tmp, pixelAttributes.pfmt.comp.bytesPerPixel(), newWidth, newHeight, 1, true);
        }
        if( allowRowStride ) {
            return byteSize < newByteSize;
        }
        return byteSize < newByteSize || width != newWidth;
    }

    /** Dispose resources. See {@link #isValid()}. */
    public void dispose() {
        disposed = true;
        buffer.clear();
    }
}


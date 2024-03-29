/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

import java.io.File;
import java.io.IOException;

import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLException;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.GLPixelBuffer;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelBufferProvider;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * Utility to read out the current FB to TextureData, optionally writing the data back to a texture object.
 * <p>May be used directly to write the TextureData to file (screenshot).</p>
 */
public class GLReadBufferUtil {
    protected final GLPixelBufferProvider pixelBufferProvider;
    protected final Texture readTexture;
    protected final GLPixelStorageModes psm;
    protected final boolean alphaRequested;

    protected boolean hasAlpha;
    protected GLPixelBuffer readPixelBuffer = null;
    protected TextureData readTextureData = null;
    protected int readBuffer = -1;

    /**
     * Using the default {@link GLPixelBuffer}: {@link GLPixelBuffer#defaultProviderNoRowStride}.
     *
     * @param requestAlpha true for RGBA readPixels, otherwise RGB readPixels. Disclaimer: {@link #hasAlpha()}==true maybe forced depending on the used {@link GLPixelBufferProvider}, i.e. on ES platforms, when calling {@link #readPixels(GL, int, int, int, int, boolean) readPixels}.
     * @param write2Texture true if readPixel's TextureData shall be written to a 2d Texture
     */
    public GLReadBufferUtil(final boolean requestAlpha, final boolean write2Texture) {
        this(GLPixelBuffer.defaultProviderNoRowStride, requestAlpha, write2Texture);
    }

    /**
     * @param pixelBufferProvider custom {@link GLPixelBuffer}
     * @param requestAlpha true for RGBA readPixels, otherwise RGB readPixels. Disclaimer: {@link #hasAlpha()}==true maybe forced depending on the used {@link GLPixelBufferProvider}, i.e. on ES platforms, when calling {@link #readPixels(GL, int, int, int, int, boolean) readPixels}.
     * @param write2Texture true if readPixel's TextureData shall be written to a 2d Texture
     */
    public GLReadBufferUtil(final GLPixelBufferProvider pixelBufferProvider, final boolean requestAlpha, final boolean write2Texture) {
        this.pixelBufferProvider = pixelBufferProvider;
        this.readTexture = write2Texture ? new Texture(GL.GL_TEXTURE_2D) : null ;
        this.psm = new GLPixelStorageModes();
        this.alphaRequested = requestAlpha;
        this.hasAlpha = requestAlpha; // preset
    }

    public void setReadBuffer(final int name) { readBuffer = name; }

    /** Returns the {@link GLPixelBufferProvider} used by this instance. */
    public GLPixelBufferProvider getPixelBufferProvider() { return pixelBufferProvider; }

    public boolean isValid() {
      return null!=readTextureData && null!=readPixelBuffer && readPixelBuffer.isValid();
    }

    /** Returns true if the OpenGL read data contains alpha. This value is lazily determined after the first call of {@link #readPixels(GL, int, int, int, int, boolean) readPixels} */
    public boolean hasAlpha() { return hasAlpha; }

    public GLPixelStorageModes getGLPixelStorageModes() { return psm; }

    /**
     * Returns the {@link GLPixelBuffer}, created and filled by {@link #readPixels(GLAutoDrawable, boolean)}.
     */
    public GLPixelBuffer getPixelBuffer() { return readPixelBuffer; }

    /**
     * rewind the raw pixel ByteBuffer
     */
    public void rewindPixelBuffer() { if( null != readPixelBuffer ) { readPixelBuffer.rewind(); } }

    /**
     * @return the resulting TextureData, filled by {@link #readPixels(GLAutoDrawable, boolean)}
     */
    public TextureData getTextureData() { return readTextureData; }

    /**
     * @return the Texture object filled by {@link #readPixels(GLAutoDrawable, boolean)},
     *         if this instance writes to a 2d Texture, otherwise null.
     * @see #GLReadBufferUtil(boolean, boolean)
     */
    public Texture getTexture() { return readTexture; }

    /**
     * Write the TextureData filled by {@link #readPixels(GLAutoDrawable, boolean)} to file
     */
    public void write(final File dest) {
        try {
            TextureIO.write(readTextureData, dest);
            rewindPixelBuffer();
        } catch (final IOException ex) {
            throw new RuntimeException("can not write to file: " + dest.getAbsolutePath(), ex);
        }
    }

    /**
     * Read the drawable's pixels to TextureData and Texture, if requested at construction.
     *
     * @param gl the current GL context object. It's read drawable is being used as the pixel source.
     * @param mustFlipVertically indicates whether to flip the data vertically or not.
     *                           The context's drawable {@link GLDrawable#isGLOriented()} state
     *                           is taken into account.
     *                           Vertical flipping is propagated to TextureData
     *                           and handled in a efficient manner there (TextureCoordinates and TextureIO writer).
     *
     * @see #GLReadBufferUtil(boolean, boolean)
     */
    public boolean readPixels(final GL gl, final boolean mustFlipVertically) {
        return readPixels(gl, 0, 0, 0, 0, mustFlipVertically);
    }

    /**
     * Read the drawable's pixels to TextureData and Texture, if requested at construction.
     *
     * @param gl the current GL context object. It's read drawable is being used as the pixel source.
     * @param inX readPixel x offset
     * @param inY readPixel y offset
     * @param inWidth optional readPixel width value, used if [1 .. drawable.width], otherwise using drawable.width
     * @param inHeight optional readPixel height, used if [1 .. drawable.height], otherwise using drawable.height
     * @param mustFlipVertically indicates whether to flip the data vertically or not.
     *                           The context's drawable {@link GLDrawable#isGLOriented()} state
     *                           is taken into account.
     *                           Vertical flipping is propagated to TextureData
     *                           and handled in a efficient manner there (TextureCoordinates and TextureIO writer).
     * @see #GLReadBufferUtil(boolean, boolean)
     */
    public boolean readPixels(final GL gl, final int inX, final int inY, final int inWidth, final int inHeight, final boolean mustFlipVertically) {
        final GLDrawable drawable = gl.getContext().getGLReadDrawable();
        final int width, height;
        if( 0 >= inWidth || drawable.getSurfaceWidth() < inWidth ) {
            width = drawable.getSurfaceWidth();
        } else {
            width = inWidth;
        }
        if( 0 >= inHeight || drawable.getSurfaceHeight() < inHeight ) {
            height = drawable.getSurfaceHeight();
        } else {
            height= inHeight;
        }
        return readPixelsImpl(drawable, gl, inX, inY, width, height, mustFlipVertically);
    }

    protected boolean readPixelsImpl(final GLDrawable drawable, final GL gl,
                                     final int inX, final int inY, final int width, final int height,
                                     final boolean mustFlipVertically) {
        final int glerr0 = gl.glGetError();
        if(GL.GL_NO_ERROR != glerr0) {
            System.err.println("Info: GLReadBufferUtil.readPixels: pre-exisiting GL error 0x"+Integer.toHexString(glerr0));
        }
        final int reqCompCount = alphaRequested ? 4 : 3; // see Bug 1381: we keep host PixelFormat functional using requested immutable alphaRequested
        final PixelFormat.Composition hostPixelComp = pixelBufferProvider.getHostPixelComp(gl.getGLProfile(), reqCompCount);
        final GLPixelAttributes pixelAttribs = pixelBufferProvider.getAttributes(gl, reqCompCount, true);
        final int componentCount = pixelAttribs.pfmt.comp.componentCount();
        hasAlpha = 0 <= pixelAttribs.pfmt.comp.find(PixelFormat.CType.A);
        final int alignment, internalFormat;
        if( 4 == componentCount ) {
            alignment = 4;
            internalFormat = GL.GL_RGBA;
        } else {
            alignment = 1;
            internalFormat = GL.GL_RGB;
        }
        final boolean flipVertically;
        if( drawable.isGLOriented() ) {
            flipVertically = mustFlipVertically;
        } else {
            flipVertically = !mustFlipVertically;
        }

        final int tmp[] = new int[1];
        final int readPixelSize = GLBuffers.sizeof(gl, tmp, pixelAttribs.pfmt.comp.bytesPerPixel(), width, height, 1, true);

        boolean newData = false;
        if( null == readPixelBuffer || readPixelBuffer.requiresNewBuffer(gl, width, height, readPixelSize) ) {
            readPixelBuffer = pixelBufferProvider.allocate(gl, hostPixelComp, pixelAttribs, true, width, height, 1, readPixelSize);
            Buffers.rangeCheckBytes(readPixelBuffer.buffer, readPixelSize);
            try {
                readTextureData = new TextureData(
                           gl.getGLProfile(),
                           internalFormat,
                           width, height,
                           0,
                           pixelAttribs,
                           false, false,
                           flipVertically,
                           readPixelBuffer.buffer,
                           null /* Flusher */);
                newData = true;
            } catch (final Exception e) {
                readTextureData = null;
                readPixelBuffer = null;
                throw new RuntimeException("can not fetch offscreen texture", e);
            }
        } else {
            readTextureData.setInternalFormat(internalFormat);
            readTextureData.setWidth(width);
            readTextureData.setHeight(height);
            readTextureData.setPixelAttributes(pixelAttribs);
        }
        boolean res = null!=readPixelBuffer && readPixelBuffer.isValid();
        if(res) {
            psm.setPackAlignment(gl, alignment);
            if(gl.isGL2ES3()) {
                final GL2ES3 gl2es3 = gl.getGL2ES3();
                psm.setPackRowLength(gl2es3, width);
                if( 0 > readBuffer ) {
                    gl2es3.glReadBuffer(gl2es3.getDefaultReadBuffer());
                } else {
                    gl2es3.glReadBuffer(readBuffer);
                }
            }
            readPixelBuffer.clear();
            try {
                gl.glReadPixels(inX, inY, width, height, pixelAttribs.format, pixelAttribs.type, readPixelBuffer.buffer);
            } catch(final GLException gle) { res = false; gle.printStackTrace(); }
            readPixelBuffer.position( readPixelSize );
            readPixelBuffer.flip();
            final int glerr1 = gl.glGetError();
            if(GL.GL_NO_ERROR != glerr1) {
                System.err.println("GLReadBufferUtil.readPixels: readPixels error 0x"+Integer.toHexString(glerr1)+
                                   " "+width+"x"+height+
                                   ", "+pixelAttribs+
                                   ", "+readPixelBuffer+", sz "+readPixelSize);
                res = false;
            }
            if(res && null != readTexture) {
                if(newData) {
                    readTexture.updateImage(gl, readTextureData);
                } else {
                    readTexture.updateSubImage(gl, readTextureData, 0,
                                               inX, inY, // dst offset
                                               0,   0,   // src offset
                                               width, height);
                }
                readPixelBuffer.rewind();
            }
            psm.restore(gl);
        }
        return res;
    }

    public void dispose(final GL gl) {
        if(null != readTexture) {
            readTexture.destroy(gl);
            readTextureData = null;
        }
        if(null != readPixelBuffer) {
            readPixelBuffer.dispose();
            readPixelBuffer = null;
        }
    }

}

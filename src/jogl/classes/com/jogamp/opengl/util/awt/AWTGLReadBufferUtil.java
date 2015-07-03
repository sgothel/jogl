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
package com.jogamp.opengl.util.awt;

import java.awt.image.BufferedImage;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLProfile;

import com.jogamp.opengl.util.GLReadBufferUtil;

/**
 * {@link GLReadBufferUtil} specialization allowing to
 * read out a frambuffer to an AWT BufferedImage
 * utilizing {@link AWTPixelBufferProviderInt} for further AWT processing.
 */
public class AWTGLReadBufferUtil extends GLReadBufferUtil {
    /**
     * {@inheritDoc}
     *
     * @param alpha
     */
    public AWTGLReadBufferUtil(final GLProfile glp, final boolean alpha) {
        super(new AWTGLPixelBuffer.AWTGLPixelBufferProvider( glp.isGL2ES3() /* allowRowStride */ ), alpha, false);
    }

    /**
     * Returns the {@link AWTGLPixelBuffer}, as filled by previous call to {@link #readPixels(GL, int, int, int, int, boolean)}.
     */
    public AWTGLPixelBuffer getAWTGLPixelBuffer() { return (AWTGLPixelBuffer)this.getPixelBuffer(); }

    /**
     * Read the drawable's pixels to TextureData and Texture, if requested at construction,
     * and returns an aligned {@link BufferedImage}.
     *
     * @param gl the current GL context object. It's read drawable is being used as the pixel source.
     * @param awtOrientation flips the data vertically if <code>true</code>.
     *                       The context's drawable {@link GLDrawable#isGLOriented()} state
     *                       is taken into account.
     *                       Vertical flipping is propagated to TextureData
     *                       and handled in a efficient manner there (TextureCoordinates and TextureIO writer).
     * @see #AWTGLReadBufferUtil(GLProfile, boolean)
     */
    public BufferedImage readPixelsToBufferedImage(final GL gl, final boolean awtOrientation) {
        return readPixelsToBufferedImage(gl, 0, 0, 0, 0, awtOrientation);
    }

    /**
     * Read the drawable's pixels to TextureData and Texture, if requested at construction,
     * and returns an aligned {@link BufferedImage}.
     *
     * @param gl the current GL context object. It's read drawable is being used as the pixel source.
     * @param inX readPixel x offset
     * @param inY readPixel y offset
     * @param inWidth optional readPixel width value, used if [1 .. drawable.width], otherwise using drawable.width
     * @param inHeight optional readPixel height, used if [1 .. drawable.height], otherwise using drawable.height
     * @param awtOrientation flips the data vertically if <code>true</code>.
     *                       The context's drawable {@link GLDrawable#isGLOriented()} state
     *                       is taken into account.
     *                       Vertical flipping is propagated to TextureData
     *                       and handled in a efficient manner there (TextureCoordinates and TextureIO writer).
     * @see #AWTGLReadBufferUtil(GLProfile, boolean)
     */
    public BufferedImage readPixelsToBufferedImage(final GL gl, final int inX, final int inY, final int inWidth, final int inHeight, final boolean awtOrientation) {
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
        if( readPixelsImpl(drawable, gl, inX, inY, width, height, awtOrientation) ) {
            final BufferedImage image = getAWTGLPixelBuffer().getAlignedImage(width, height);
            if( getTextureData().getMustFlipVertically()  ) {
                ImageUtil.flipImageVertically(image);
            }
            return image;
        }
        return null;
    }
}

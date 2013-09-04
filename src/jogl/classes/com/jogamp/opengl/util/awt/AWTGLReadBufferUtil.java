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

import javax.media.opengl.GL;
import javax.media.opengl.GLProfile;

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
    public AWTGLReadBufferUtil(GLProfile glp, boolean alpha) {
        super(new AWTGLPixelBuffer.AWTGLPixelBufferProvider( glp.isGL2GL3() || glp.isGL3ES3() /* allowRowStride */ ), alpha, false);
    }

    public AWTGLPixelBuffer getAWTGLPixelBuffer() { return (AWTGLPixelBuffer)this.getPixelBuffer(); }
    
    public BufferedImage readPixelsToBufferedImage(GL gl, boolean awtOrientation) {
        if( readPixels(gl, awtOrientation) ) {
            final BufferedImage image = getAWTGLPixelBuffer().image;
            if( getTextureData().getMustFlipVertically()  ) {
                ImageUtil.flipImageVertically(image);
            }
            return image;
        }
        return null;
    }
    public BufferedImage readPixelsToBufferedImage(GL gl, int inX, int inY, int inWidth, int inHeight, boolean awtOrientation) {
        if( readPixels(gl, inX, inY, inWidth, inHeight, awtOrientation) ) {
            final BufferedImage image = getAWTGLPixelBuffer().image;
            if( getTextureData().getMustFlipVertically()  ) {
                ImageUtil.flipImageVertically(image);
            }
            return image;
        }
        return null;
    }
}

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
 
package com.jogamp.opengl.test.junit.jogl.offscreen;

import com.jogamp.opengl.util.GLBuffers;
import java.nio.*;
import javax.media.opengl.*;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;

public class ReadBufferUtil {
    protected int readPixelSizeLast = 0;
    protected Buffer readPixelBuffer = null;
    protected TextureData readTextureData = null;
    protected Texture readTexture = new Texture(GL.GL_TEXTURE_2D);

    public Buffer getPixelBuffer() { return readPixelBuffer; }
    public void rewindPixelBuffer() { readPixelBuffer.rewind(); }

    public TextureData getTextureData() { return readTextureData; }
    public Texture getTexture() { return readTexture; }

    public boolean isValid() {
      return null!=readTexture && null!=readTextureData && null!=readPixelBuffer ;
    }

    public void fetchOffscreenTexture(GLDrawable drawable, GL gl) {
        int readPixelSize = drawable.getWidth() * drawable.getHeight() * 3 ; // RGB
        boolean newData = false;
        if(readPixelSize>readPixelSizeLast) {
            readPixelBuffer = GLBuffers.newDirectGLBuffer(GL.GL_UNSIGNED_BYTE, readPixelSize);
            readPixelSizeLast = readPixelSize ;
            try {
                readTextureData = new TextureData(
                           gl.getGLProfile(),
                           // gl.isGL2GL3()?gl.GL_RGBA:gl.GL_RGB,
                           gl.GL_RGB,
                           drawable.getWidth(), drawable.getHeight(),
                           0, 
                           gl.GL_RGB,
                           gl.GL_UNSIGNED_BYTE,
                           false, false, 
                           false /* flip */,
                           readPixelBuffer,
                           null /* Flusher */);
                newData = true;
            } catch (Exception e) {
                readTextureData = null;
                readPixelBuffer = null;
                readPixelSizeLast = 0;
                throw new RuntimeException("can not fetch offscreen texture", e);
            }
        }
        if(null!=readPixelBuffer) {
            readPixelBuffer.clear();
            gl.glReadPixels(0, 0, drawable.getWidth(), drawable.getHeight(), GL.GL_RGB, GL.GL_UNSIGNED_BYTE, readPixelBuffer);
            readPixelBuffer.rewind();
            if(newData) {
                readTexture.updateImage(gl, readTextureData);
            } else {
                readTexture.updateSubImage(gl, readTextureData, 0, 
                                           0, 0, // src offset
                                           0, 0, // dst offset
                                           drawable.getWidth(), drawable.getHeight());
            }
            readPixelBuffer.rewind();
        }
    }

    public void dispose(GL gl) {
        readTexture.destroy(gl);
        readTextureData = null;
        readPixelBuffer.clear();
        readPixelBuffer = null;
        readPixelSizeLast = 0;
    }

}


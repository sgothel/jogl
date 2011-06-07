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

import com.jogamp.common.nio.Buffers;

import java.io.File;
import java.io.IOException;
import java.nio.*;
import javax.media.opengl.*;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * Utility to read out the current FB to TextureData, optionally writing the data back to a texture object.
 * <p>May be used directly to write the TextureData to file (screenshot).</p>
 */
public class GLReadBufferUtil {
    protected final int components, alignment; 
    protected final Texture readTexture;
    protected final GLPixelStorageModes psm;
    
    protected int readPixelSizeLast = 0;
    protected ByteBuffer readPixelBuffer = null;
    protected TextureData readTextureData = null;

    /**
     * @param alpha true for RGBA readPixels, otherwise RGB readPixels 
     * @param write2Texture true if readPixel's TextureData shall be written to a 2d Texture
     */
    public GLReadBufferUtil(boolean alpha, boolean write2Texture) {
        components = alpha ? 4 : 3 ;
        alignment = alpha ? 4 : 1 ; 
        readTexture = write2Texture ? new Texture(GL.GL_TEXTURE_2D) : null ;
        psm = new GLPixelStorageModes();
    }
    
    public boolean isValid() {
      return null!=readTextureData && null!=readPixelBuffer ;
    }
    
    /**
     * @return the raw pixel ByteBuffer, filled by {@link #readPixels(GLAutoDrawable, boolean)}
     */
    public ByteBuffer getPixelBuffer() { return readPixelBuffer; }
    
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
    public void write(File dest) {
        try {
            TextureIO.write(readTextureData, dest);
            rewindPixelBuffer();
        } catch (IOException ex) {
            throw new RuntimeException("can not write to file: " + dest.getAbsolutePath(), ex);
        }
    }

    /**
     * Read the drawable's pixels to TextureData and Texture, if requested at construction
     * 
     * @param gl the current GL object
     * @param drawable the drawable to read from
     * @param flip weather to flip the data vertically or not
     * 
     * @see #GLReadBufferUtil(boolean, boolean)
     */
    public void readPixels(GL gl, GLDrawable drawable, boolean flip) {
        final int textureInternalFormat, textureDataFormat;
        final int textureDataType = GL.GL_UNSIGNED_BYTE;
        if(4 == components) {
            textureInternalFormat=GL.GL_RGBA;
            textureDataFormat=GL.GL_RGBA;
        } else {
            textureInternalFormat=GL.GL_RGB;
            textureDataFormat=GL.GL_RGB;
        }
        final int readPixelSize = drawable.getWidth() * drawable.getHeight() * components ;
        boolean newData = false;
        if(readPixelSize>readPixelSizeLast) {
            readPixelBuffer = Buffers.newDirectByteBuffer(readPixelSize);
            readPixelSizeLast = readPixelSize ;
            try {
                readTextureData = new TextureData(
                           gl.getGLProfile(),
                           textureInternalFormat,
                           drawable.getWidth(), drawable.getHeight(),
                           0, 
                           textureDataFormat,
                           textureDataType,
                           false, false, 
                           flip,
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
            psm.setAlignment(gl, alignment, alignment);
            readPixelBuffer.clear();
            gl.glReadPixels(0, 0, drawable.getWidth(), drawable.getHeight(), textureDataFormat, textureDataType, readPixelBuffer);
            readPixelBuffer.rewind();
            if(null != readTexture) {
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
            psm.restore(gl);
        }
    }

    public void dispose(GL gl) {  
        if(null != readTexture) {
            readTexture.destroy(gl);
            readTextureData = null;
        }
        if(null != readPixelBuffer) {
            readPixelBuffer.clear();
            readPixelBuffer = null;
        }
        readPixelSizeLast = 0;
    }

}


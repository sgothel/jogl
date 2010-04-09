/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name Sven Gothel or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.jogamp.test.junit.jogl.offscreen;

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
                e.printStackTrace();
                readTextureData = null;
                readPixelBuffer = null;
                readPixelSizeLast = 0;
            }
        }
        if(null!=readPixelBuffer) {
            readPixelBuffer.clear();
            gl.glReadPixels(0, 0, drawable.getWidth(), drawable.getHeight(), GL.GL_RGB, GL.GL_UNSIGNED_BYTE, readPixelBuffer);
            readPixelBuffer.rewind();
            if(newData) {
                readTexture.updateImage(readTextureData);
            } else {
                readTexture.updateSubImage(readTextureData, 0, 
                                           0, 0, // src offset
                                           0, 0, // dst offset
                                           drawable.getWidth(), drawable.getHeight());
            }
            readPixelBuffer.rewind();
        }
    }

    public void dispose() {
        readTexture.dispose();
        readTextureData = null;
        readPixelBuffer.clear();
        readPixelBuffer = null;
        readPixelSizeLast = 0;
    }

}


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
 
package com.jogamp.opengl.test.junit.jogl.util.texture.gl2;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

public class TextureGL2ListenerDraw1 implements GLEventListener {
    private GLU      glu = new GLU();
    private TextureData textureData;
    private Texture  texture;
    
    public TextureGL2ListenerDraw1(TextureData td) {
        this.textureData = td;
    }

    public void init(GLAutoDrawable drawable) {
        if(null!=textureData) {
            this.texture = TextureIO.newTexture(textureData);
        }
    }

    public void setTexture( Texture texture ) {
        this.texture = texture;
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glMatrixMode(GL2ES1.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0, 1, 0, 1);
        gl.glMatrixMode(GL2ES1.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        if(null!=texture) {
            texture.disable(gl);
            texture.destroy(gl);
        }
        if(null!=textureData) {
            textureData.destroy();
        }
    }

    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // need a valid GL context for this ..

        /** OpenGL ..
        texture.updateSubImage(textureData, 0,
                               20, 20, 
                               20, 20,
                               100, 100); */

    
        // Now draw one quad with the texture
        if(null!=texture) {
            texture.enable(gl);
            texture.bind(gl);
            gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
            TextureCoords coords = texture.getImageTexCoords();
            gl.glBegin(GL2.GL_QUADS);
            gl.glTexCoord2f(coords.left(), coords.bottom());
            gl.glVertex3f(0, 0, 0);
            gl.glTexCoord2f(coords.right(), coords.bottom());
            gl.glVertex3f(1, 0, 0);
            gl.glTexCoord2f(coords.right(), coords.top());
            gl.glVertex3f(1, 1, 0);
            gl.glTexCoord2f(coords.left(), coords.top());
            gl.glVertex3f(0, 1, 0);
            gl.glEnd();
            texture.disable(gl);
        }
    }
}


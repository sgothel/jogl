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

package com.jogamp.opengl.test.junit.jogl.demos.gl2;

import com.jogamp.opengl.test.junit.jogl.demos.TextureDraw01Accessor;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;

public class TextureDraw01GL2Listener implements GLEventListener, TextureDraw01Accessor {
    private final GLU      glu = new GLU();
    private final TextureData textureData;
    private Texture  texture;
    boolean keepTextureBound;

    public TextureDraw01GL2Listener(final TextureData td) {
        this.textureData = td;
        this.keepTextureBound = false;
    }

    @Override
    public void setKeepTextureBound(final boolean v) {
        this.keepTextureBound = v;
    }
    @Override
    public Texture getTexture( ) {
        return this.texture;
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();
        if(null!=textureData) {
            this.texture = TextureIO.newTexture(drawable.getGL(), textureData);
            if( keepTextureBound ) {
                texture.enable(gl);
                texture.bind(gl);
            }
        }
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL2 gl = drawable.getGL().getGL2();
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0, 1, 0, 1);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();
        if(null!=texture) {
            texture.disable(gl);
            texture.destroy(gl);
        }
        if(null!=textureData) {
            textureData.destroy();
        }
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();

        // draw one quad with the texture
        if(null!=texture) {
            if( !keepTextureBound ) {
                texture.enable(gl);
                texture.bind(gl);
            }
            gl.glTexEnvi(GL2ES1.GL_TEXTURE_ENV, GL2ES1.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);
            final TextureCoords coords = texture.getImageTexCoords();
            gl.glBegin(GL2GL3.GL_QUADS);
            gl.glTexCoord2f(coords.left(), coords.bottom());
            gl.glVertex3f(0, 0, 0);
            gl.glTexCoord2f(coords.right(), coords.bottom());
            gl.glVertex3f(1, 0, 0);
            gl.glTexCoord2f(coords.right(), coords.top());
            gl.glVertex3f(1, 1, 0);
            gl.glTexCoord2f(coords.left(), coords.top());
            gl.glVertex3f(0, 1, 0);
            gl.glEnd();
            if( !keepTextureBound ) {
                texture.disable(gl);
            }
        }
    }
}


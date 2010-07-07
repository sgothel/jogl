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

package com.jogamp.test.junit.jogl.util.texture.gl2;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import javax.media.opengl.GL;
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
            texture.disable();
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
            texture.enable();
            texture.bind();
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
            texture.disable();
        }
    }
}


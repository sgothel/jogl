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

import java.nio.*;
import javax.media.opengl.*;
import javax.media.opengl.fixedfunc.*;

import com.jogamp.opengl.util.*;

import javax.media.opengl.fixedfunc.GLPointerFunc;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.GLArrayDataClient;
import com.jogamp.opengl.util.GLArrayDataServer;

public class ReadBuffer2Screen extends ReadBufferBase {
    PMVMatrix pmvMatrix;
    GLArrayDataClient readTextureVertices = null;
    GLArrayDataClient readTextureCoords = null;
    boolean enableBufferAlways = false; // FIXME
    boolean enableBufferVBO    = true; // FIXME

    public ReadBuffer2Screen (GLDrawable externalRead) {
        super(externalRead);
    }

    public void init(GLAutoDrawable drawable) {
        super.init(drawable);

        GL gl = drawable.getGL();

        pmvMatrix = new PMVMatrix();

        float f_edge = 1f;
        if(null==readTextureVertices) {
            //readTextureVertices = GLArrayDataClient.createFixed(gl, GLPointerFunc.GL_VERTEX_ARRAY, "mgl_Vertex", 
            //                                                    2, GL.GL_FLOAT, true, 4);
            readTextureVertices = GLArrayDataServer.createFixed(gl, GLPointerFunc.GL_VERTEX_ARRAY, "mgl_Vertex", 
                                                                2, GL.GL_FLOAT, true, 4, GL.GL_STATIC_DRAW);
            readTextureVertices.setEnableAlways(enableBufferAlways);
            readTextureVertices.setVBOUsage(enableBufferVBO);
            {
                FloatBuffer vb = (FloatBuffer)readTextureVertices.getBuffer();
                vb.put(-f_edge); vb.put(-f_edge);
                vb.put( f_edge); vb.put(-f_edge);
                vb.put(-f_edge); vb.put( f_edge);
                vb.put( f_edge); vb.put( f_edge);
            }
            readTextureVertices.seal(gl, true);
            System.out.println(readTextureVertices);
        }

        // Clear background to gray
        gl.glClearColor(0.5f, 0.5f, 0.5f, 0.4f);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        super.reshape(drawable, x, y, width, height);

        GL gl = drawable.getGL();

        gl.glViewport(0, 0, width, height);

        if(gl instanceof GLLightingFunc) {
            ((GLLightingFunc)gl).glShadeModel(GLLightingFunc.GL_SMOOTH);
        }

        GLMatrixFunc glM;
        if(gl instanceof GLMatrixFunc) {
            glM = (GLMatrixFunc)gl;
        } else {
            throw new GLException("ES2 currently unhandled .. ");
        }

        // Identity ..
        pmvMatrix.glMatrixMode(pmvMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -2.5f);
        if(null!=glM) {
            glM.glMatrixMode(pmvMatrix.GL_MODELVIEW);
            glM.glLoadMatrixf(pmvMatrix.glGetMvMatrixf());
        }

        // Set location in front of camera
        pmvMatrix.glMatrixMode(pmvMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0f, (float)width / (float)height, 1.0f, 100.0f);
        if(null!=glM) {
            glM.glMatrixMode(pmvMatrix.GL_PROJECTION);
            glM.glLoadMatrixf(pmvMatrix.glGetPMatrixf());
        }
    }

    public void dispose(GLAutoDrawable drawable) {
        super.dispose(drawable);
    }

    void renderOffscreenTexture(GL gl) {
      if(!readBufferUtil.isValid()) return;

      // Now draw one quad with the texture
      readBufferUtil.getTexture().enable();
      readBufferUtil.getTexture().bind();

      if(gl.isGL2ES1()) {
          // gl.getGL2ES1().glTexEnvi(GL2ES1.GL_TEXTURE_ENV, GL2ES1.GL_TEXTURE_ENV_MODE, GL2ES1.GL_REPLACE);
          gl.getGL2ES1().glTexEnvi(GL2ES1.GL_TEXTURE_ENV, GL2ES1.GL_TEXTURE_ENV_MODE, GL2ES1.GL_MODULATE);
      }

      updateTextureCoords(gl, false);

      readTextureVertices.enableBuffer(gl, true);
      if(null!=readTextureCoords) {
          readTextureCoords.enableBuffer(gl, true);
      }
      gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, readTextureVertices.getElementNumber());
      /**
      if(null!=readTextureCoords) {
          readTextureCoords.enableBuffer(gl, false);
      }
      readTextureVertices.enableBuffer(gl, false); */

      readBufferUtil.getTexture().disable();
    }

    void updateTextureCoords(GL gl, boolean force) {
        if(force || null==readTextureCoords) {
            readTextureCoords = GLArrayDataServer.createFixed(gl, GLPointerFunc.GL_TEXTURE_COORD_ARRAY, "mgl_MultiTexCoord0", 
                                                              2, GL.GL_FLOAT, true, 4, GL.GL_STATIC_DRAW);
            readTextureCoords.setEnableAlways(enableBufferAlways);
            readTextureCoords.setVBOUsage(enableBufferVBO);
            {
                TextureCoords coords = readBufferUtil.getTexture().getImageTexCoords();
                FloatBuffer cb = (FloatBuffer)readTextureCoords.getBuffer();
                cb.put(coords.left());  cb.put(coords.bottom());
                cb.put(coords.right()); cb.put(coords.bottom());
                cb.put(coords.left());  cb.put(coords.top());
                cb.put(coords.right()); cb.put(coords.top());
            }
            readTextureCoords.seal(gl, true);
            System.out.println(readTextureCoords);
        }
    }

    public void display(GLAutoDrawable drawable) {
        super.display(drawable);

        GL gl = drawable.getGL();

        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        if(gl instanceof GLLightingFunc) {
            ((GLLightingFunc)gl).glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        }

        renderOffscreenTexture(gl);
    }
}


/**
 * Copyright (C) 2011 JogAmp Community. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * BRIAN PAUL BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.jogamp.opengl.test.junit.jogl.demos.es2;

import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

public class Mix2TexturesES2 implements GLEventListener {
    private final int swapInterval;
    
    private final ShaderState st;
    private final PMVMatrix pmvMatrix;
    private final GLUniformData texUnit0, texUnit1;
    
    private Object syncTexIDs = new Object();
    private int texID0, texID1;
    private ShaderProgram sp0;
    private GLUniformData pmvMatrixUniform;
    private GLArrayDataServer interleavedVBO;
    
    public Mix2TexturesES2(int swapInterval, int texUnit0, int texUnit1) {
        this.swapInterval = swapInterval;
        
        st = new ShaderState();
        // st.setVerbose(true);        
        pmvMatrix = new PMVMatrix();
        
        if(0 == texUnit1) {
            this.texUnit0 = new GLUniformData("mgl_ActiveTexture", texUnit0);
            this.texUnit1 = null;
        } else {
            this.texUnit0 = new GLUniformData("mgl_Texture0", texUnit0);
            this.texUnit1 = new GLUniformData("mgl_Texture1", texUnit1);
        }
        this.texID0 = 0;
        this.texID1 = 0;
    }
    
    public void setTexID0(int texID) {
        synchronized( syncTexIDs ) {
            this.texID0 = texID;
        }
    }
    public void setTexID1(int texID) {
        synchronized( syncTexIDs ) {
            this.texID1 = texID;
        }
    }
        
    @Override
    public void init(GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, Mix2TexturesES2.class, "shader",
                "shader/bin", "texture01_xxx", true);
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, Mix2TexturesES2.class, "shader",
                "shader/bin", null == texUnit1 ? "texture01_xxx" : "texture02_xxx", true);
        vp0.defaultShaderCustomization(gl, true, true);
        fp0.defaultShaderCustomization(gl, true, true);
        
        sp0 = new ShaderProgram();
        sp0.add(gl, vp0, System.err);
        sp0.add(gl, fp0, System.err);       
        st.attachShaderProgram(gl, sp0, true);
        
        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        st.ownUniform(pmvMatrixUniform);       
        st.uniform(gl, pmvMatrixUniform);
        
        interleavedVBO = GLArrayDataServer.createGLSLInterleaved(3+4+2, GL.GL_FLOAT, false, 3*4, GL.GL_STATIC_DRAW);
        {        
            interleavedVBO.addGLSLSubArray("mgl_Vertex",        3, GL.GL_ARRAY_BUFFER);            
            interleavedVBO.addGLSLSubArray("mgl_Color",         4, GL.GL_ARRAY_BUFFER);            
            //interleavedVBO.addGLSLSubArray("mgl_Normal",        3, GL.GL_ARRAY_BUFFER);
            interleavedVBO.addGLSLSubArray("mgl_MultiTexCoord", 2, GL.GL_ARRAY_BUFFER);

            FloatBuffer ib = (FloatBuffer)interleavedVBO.getBuffer();
            
            for(int i=0; i<4; i++) {
                ib.put(s_quadVertices,  i*3, 3);
                ib.put(s_quadColors,    i*4, 4);  
                //ib.put(s_cubeNormals,   i*3, 3);
                ib.put(s_quadTexCoords, i*2, 2);
            }                        
        }
        interleavedVBO.seal(gl, true);
        interleavedVBO.enableBuffer(gl, false);
        st.ownAttribute(interleavedVBO, true);
                
        st.ownUniform(texUnit0);
        st.uniform(gl, texUnit0);
        if(null != texUnit1) {
            st.ownUniform(texUnit1);       
            st.uniform(gl, texUnit1);
        }
        
        st.useProgram(gl, false);        
    }
    
    @Override
    public void dispose(GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        st.destroy(gl);
        
        sp0 = null;
        pmvMatrixUniform = null;
        interleavedVBO = null;
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        st.useProgram(gl, true);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        
        interleavedVBO.enableBuffer(gl, true);
        
        synchronized( syncTexIDs ) {
            if(0<texID0) {
                gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit0.intValue());
                gl.glBindTexture(GL.GL_TEXTURE_2D, texID0);
            }
            
            if(0<texID1 && null != texUnit1) {
                gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit1.intValue());
                gl.glBindTexture(GL.GL_TEXTURE_2D, texID1);
            }
            
            if( !gl.isGLcore() ) {
                gl.glEnable(GL.GL_TEXTURE_2D);
            }
            
            gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        }
        
        interleavedVBO.enableBuffer(gl, false);
        
        st.useProgram(gl, false);        
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        if(-1 != swapInterval) {        
            gl.setSwapInterval(swapInterval); // in case switching the drawable (impl. may bound attribute there)
        }
        
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 10.0f);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        
        st.useProgram(gl, true);
        st.uniform(gl, pmvMatrixUniform);
        st.useProgram(gl, false);
        
    }

    private static final float[] s_quadVertices = { 
      -1f, -1f, 0f, // LB
       1f, -1f, 0f, // RB
      -1f,  1f, 0f, // LT
       1f,  1f, 0f  // RT 
    };
    private static final float[] s_quadColors = { 
      1f, 1f, 1f, 1f,
      1f, 1f, 1f, 1f,
      1f, 1f, 1f, 1f,
      1f, 1f, 1f, 1f };
    private static final float[] s_quadTexCoords = { 
            0f, 0f, // LB
            1f, 0f, // RB
            0f, 1f, // LT   
            1f, 1f  // RT
    };
}

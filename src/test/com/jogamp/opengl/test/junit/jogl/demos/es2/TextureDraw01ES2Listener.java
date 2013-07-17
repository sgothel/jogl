/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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
 
package com.jogamp.opengl.test.junit.jogl.demos.es2;

import java.nio.FloatBuffer;

import com.jogamp.opengl.test.junit.jogl.demos.TextureDraw01Accessor;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

public class TextureDraw01ES2Listener implements GLEventListener, TextureDraw01Accessor {
    TextureData textureData;
    Texture  texture;
    
    ShaderState st;
    PMVMatrix pmvMatrix;
    GLUniformData pmvMatrixUniform;
    GLArrayDataServer interleavedVBO;
    float[] clearColor = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
    
    public TextureDraw01ES2Listener(TextureData td) {
        this.textureData = td;
    }
    
    public void setClearColor(float[] clearColor) {
        this.clearColor = clearColor;
    }

    static final String shaderBasename = "texture01_xxx";
       
    private void initShader(GL2ES2 gl, boolean use_program) {
        // Create & Compile the shader objects
        ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(), 
                                            "shader", "shader/bin", shaderBasename, true);
        ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(), 
                                            "shader", "shader/bin", shaderBasename, true);
        rsVp.defaultShaderCustomization(gl, true, true);
        rsFp.defaultShaderCustomization(gl, true, true);
        
        // Create & Link the shader program
        ShaderProgram sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);
        if(!sp.link(gl, System.err)) {
            throw new GLException("Couldn't link program: "+sp);
        }

        // Let's manage all our states using ShaderState.
        st = new ShaderState();
        st.attachShaderProgram(gl, sp, use_program);
    }
    
    public void init(GLAutoDrawable glad) {
        if(null!=textureData) {
            this.texture = TextureIO.newTexture(glad.getGL(), textureData);
        }
        GL2ES2 gl = glad.getGL().getGL2ES2();
        
        initShader(gl, true);
                
        // setup mgl_PMVMatrix
        pmvMatrix = new PMVMatrix();
        pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();       
        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf()); // P, Mv
        
        st.ownUniform(pmvMatrixUniform);
        if(!st.uniform(gl, pmvMatrixUniform)) {
            throw new GLException("Error setting PMVMatrix in shader: "+st);
        }
        if(!st.uniform(gl, new GLUniformData("mgl_ActiveTexture", 0))) {
            throw new GLException("Error setting mgl_ActiveTexture in shader: "+st);
        }

        // fetch the flipped texture coordinates
        texture.getImageTexCoords().getST_LB_RB_LT_RT(s_quadTexCoords, 0, 1f, 1f);
        
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
        
        // OpenGL Render Settings
        gl.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);
        st.useProgram(gl, false);        
    }

    public Texture getTexture( ) {
        return this.texture;
    }
    
    /**
    public void setTextureData(GL gl, TextureData textureData ) {
        if(null!=texture) {
            texture.disable(gl);
            texture.destroy(gl);
        }
        if(null!=this.textureData) {
            this.textureData.destroy();
        }
        this.textureData = textureData;
        this.texture = TextureIO.newTexture(this.textureData);
        
        // fix VBO !
    } */

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        gl.glViewport(0, 0, width, height);

        // Clear background to white
        gl.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        if(null != st) {
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmvMatrix.glLoadIdentity();
            pmvMatrix.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 10.0f);
    
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmvMatrix.glLoadIdentity();
            
            st.useProgram(gl, true);
            st.uniform(gl, pmvMatrixUniform);
            st.useProgram(gl, false);
        }        
    }

    public void dispose(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(null!=texture) {
            texture.disable(gl);
            texture.destroy(gl);
        }
        if(null!=textureData) {
            textureData.destroy();
        }

        pmvMatrixUniform = null;
        pmvMatrix.destroy();
        pmvMatrix=null;
        st.destroy(gl);
        st=null;
    }

    public void display(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        st.useProgram(gl, true);        
        interleavedVBO.enableBuffer(gl, true);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        texture.enable(gl);
        texture.bind(gl);
        
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        
        texture.disable(gl);                                
        interleavedVBO.enableBuffer(gl, false);        
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


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

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

public class TextureDraw02ES2ListenerFBO implements GLEventListener {
    private final GLEventListener demo;
    private final int swapInterval;
    private boolean clearBuffers = true;
    private int numSamples;    
    int textureUnit;
    boolean keepTextureBound;
    
    private final ShaderState st;
    private final PMVMatrix pmvMatrix;
    
    private final FBObject fbo0;    
    
    private TextureAttachment fbo0Tex;
        
    private ShaderProgram sp0;
    private GLUniformData pmvMatrixUniform;
    private GLArrayDataServer interleavedVBO;
    private GLUniformData texUnit0;
    
    public TextureDraw02ES2ListenerFBO(GLEventListener demo, int swapInterval, int textureUnit) {
        this.demo = demo;
        this.swapInterval = swapInterval;
        this.textureUnit = textureUnit;
        this.keepTextureBound = false;
        
        st = new ShaderState();
        // st.setVerbose(true);        
        pmvMatrix = new PMVMatrix();
        
        fbo0 = new FBObject();
        
        numSamples = 0;
    }
    
    public void setClearBuffers(boolean v) { clearBuffers = v; }
    
    public void setKeepTextureBound(boolean v) {
        this.keepTextureBound = v;
    }
    public void setMSAA(int numSamples) { 
        this.numSamples=numSamples; 
    }
    public int getMSAA() { return numSamples; }
    
    @Override
    public void init(GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        demo.init(drawable);
        
        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, TextureDraw02ES2ListenerFBO.class, "shader",
                "shader/bin", "texture01_xxx", true);
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, TextureDraw02ES2ListenerFBO.class, "shader",
                "shader/bin", "texture02_xxx", true);
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
                
        texUnit0 = new GLUniformData("mgl_Texture0", textureUnit);
        st.ownUniform(texUnit0);       
        st.uniform(gl, texUnit0);
        
        st.useProgram(gl, false);
                
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);        
    }
    
    private void initFBOs(GL gl, int width, int height) {
        // remove all texture attachments, since MSAA uses just color-render-buffer
        // and non-MSAA uses texture2d-buffer
        fbo0.detachAllColorbuffer(gl);
            
        fbo0.reset(gl, width, height, numSamples, false);
        numSamples = fbo0.getNumSamples();
        
        if(numSamples>0) {
            fbo0.attachColorbuffer(gl, 0, true);
            fbo0.resetSamplingSink(gl);
            fbo0Tex = fbo0.getSamplingSink();
        } else {
            fbo0Tex = fbo0.attachTexture2D(gl, 0, true);
        }        
        numSamples=fbo0.getNumSamples();
        fbo0.attachRenderbuffer(gl, Type.DEPTH, 24);
        fbo0.unbind(gl);
    }

    private void resetFBOs(GL gl, int width, int height) {
        fbo0.reset(gl, width, height, numSamples, true);
        numSamples = fbo0.getNumSamples();
        if(numSamples>0) {
            fbo0Tex = fbo0.getSamplingSink();
        } else {
            fbo0Tex = (TextureAttachment) fbo0.getColorbuffer(0);
        }        
    }
    
    @Override
    public void dispose(GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        demo.dispose(drawable);
        fbo0.destroy(gl);
        st.destroy(gl);
        
        fbo0Tex = null;
        sp0 = null;
        pmvMatrixUniform = null;
        interleavedVBO = null;
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        if( fbo0.getNumSamples() != numSamples ) {
            System.err.println("**** NumSamples: "+fbo0.getNumSamples()+" -> "+numSamples);
            resetFBOs(gl, drawable.getWidth(), drawable.getHeight());
        }
        
        if(0 < numSamples) {
            gl.glEnable(GL.GL_MULTISAMPLE);
        }
        
        fbo0.bind(gl);
        demo.display(drawable);
        fbo0.unbind(gl);
        
        st.useProgram(gl, true);
        if( clearBuffers ) {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        }
        
        if( !keepTextureBound ) {
            gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit0.intValue());
            fbo0.use(gl, fbo0Tex);
            if( !gl.isGLcore() ) {
                gl.glEnable(GL.GL_TEXTURE_2D);
            }
        }
        gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit0.intValue());
        interleavedVBO.enableBuffer(gl, true);
                
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        
        interleavedVBO.enableBuffer(gl, false);
        
        if( !keepTextureBound ) {
            fbo0.unuse(gl);
        }
        
        st.useProgram(gl, false);        
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        if(-1 != swapInterval) {        
            gl.setSwapInterval(swapInterval); // in case switching the drawable (impl. may bound attribute there)
        }
        
        if( !fbo0.isInitialized() ) {
            System.err.println("**** Reshape.Init: "+width+"x"+height);
            initFBOs(gl, width, height);
        } else {        
            System.err.println("**** Reshape.Reset: "+width+"x"+height);
            if( keepTextureBound ) {
                fbo0.unuse(gl);
            }
            resetFBOs(gl, width, height);
        }
        
        fbo0.bind(gl);
        demo.reshape(drawable, x, y, width, height);
        fbo0.unbind(gl);
        
        if( keepTextureBound ) {
            gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit0.intValue());
            fbo0.use(gl, fbo0Tex);
            if( !gl.isGLcore() ) {
                gl.glEnable(GL.GL_TEXTURE_2D);
            }
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

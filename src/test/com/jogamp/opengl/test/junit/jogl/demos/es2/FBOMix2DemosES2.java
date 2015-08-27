/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

public class FBOMix2DemosES2 implements GLEventListener {
    private final GearsES2 demo0;
    private final RedSquareES2 demo1;
    private final int swapInterval;
    private volatile int numSamples;
    private boolean demo0Only;


    private final ShaderState st;
    private final PMVMatrix pmvMatrix;

    private final FBObject fbo0;
    private final FBObject fbo1;

    private TextureAttachment fbo0Tex;
    private TextureAttachment fbo1Tex;

    private ShaderProgram sp0;
    private GLUniformData pmvMatrixUniform;
    private GLArrayDataServer interleavedVBO;
    private GLUniformData texUnit0;
    private GLUniformData texUnit1;

    public FBOMix2DemosES2(final int swapInterval) {
        demo0 = new GearsES2(-1);
        demo0.setIgnoreFocus(true);
        demo1 = new RedSquareES2(-1);
        this.swapInterval = swapInterval;

        st = new ShaderState();
        // st.setVerbose(true);
        pmvMatrix = new PMVMatrix();

        fbo0 = new FBObject();
        fbo1 = new FBObject();

        numSamples = 0;
        demo0Only = false;
    }

    public void setDemo0Only(final boolean v) {
        this.demo0Only = v;
    }
    public boolean getDemo0Only() { return demo0Only; }

    public void setMSAA(final int numSamples) {
        this.numSamples=numSamples;
    }
    public int getMSAA() { return numSamples; }

    public void setDoRotation(final boolean rotate) { demo1.setDoRotation(rotate); }

    @Override
    public void init(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        demo0.init(drawable);
        demo1.init(drawable);

        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, FBOMix2DemosES2.class, "shader",
                "shader/bin", "texture01_xxx", true);
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, FBOMix2DemosES2.class, "shader",
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

            final FloatBuffer ib = (FloatBuffer)interleavedVBO.getBuffer();

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

        texUnit0 = new GLUniformData("mgl_Texture0", 0);
        st.ownUniform(texUnit0);
        st.uniform(gl, texUnit0);
        texUnit1 = new GLUniformData("mgl_Texture1", 1);
        st.ownUniform(texUnit1);
        st.uniform(gl, texUnit1);

        st.useProgram(gl, false);

        System.err.println("**** Init");
        initFBOs(gl, drawable);

        gl.glEnable(GL.GL_DEPTH_TEST);
    }

    private void initFBOs(final GL gl, final GLAutoDrawable drawable) {
        fbo0.init(gl, drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), numSamples);
        fbo1.init(gl, drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), numSamples);
        if(fbo0.getNumSamples() != fbo1.getNumSamples()) {
            throw new InternalError("sample size mismatch: \n\t0: "+fbo0+"\n\t1: "+fbo1);
        }
        numSamples = fbo0.getNumSamples();

        if(numSamples>0) {
            fbo0.attachColorbuffer(gl, 0, true);
            fbo0.resetSamplingSink(gl);
            fbo1.attachColorbuffer(gl, 0, true);
            fbo1.resetSamplingSink(gl);
            fbo0Tex = fbo0.getSamplingSink().getTextureAttachment();
            fbo1Tex = fbo1.getSamplingSink().getTextureAttachment();
        } else {
            fbo0Tex = fbo0.attachTexture2D(gl, 0, true);
            fbo1Tex = fbo1.attachTexture2D(gl, 0, true);
        }
        numSamples=fbo0.getNumSamples();
        fbo0.attachRenderbuffer(gl, Type.DEPTH, FBObject.CHOSEN_BITS);
        fbo0.unbind(gl);
        fbo1.attachRenderbuffer(gl, Type.DEPTH, FBObject.CHOSEN_BITS);
        fbo1.unbind(gl);
    }

    private void resetFBOs(final GL gl, final GLAutoDrawable drawable) {
        fbo0.reset(gl, drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), numSamples);
        fbo1.reset(gl, drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), numSamples);
        if(fbo0.getNumSamples() != fbo1.getNumSamples()) {
            throw new InternalError("sample size mismatch: \n\t0: "+fbo0+"\n\t1: "+fbo1);
        }
        numSamples = fbo0.getNumSamples();
        if(numSamples>0) {
            fbo0Tex = fbo0.getSamplingSink().getTextureAttachment();
            fbo1Tex = fbo1.getSamplingSink().getTextureAttachment();
        } else {
            fbo0Tex = fbo0.getColorbuffer(0).getTextureAttachment();
            fbo1Tex = fbo1.getColorbuffer(0).getTextureAttachment();
        }
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        demo0.dispose(drawable);
        demo1.dispose(drawable);
        fbo0.destroy(gl);
        fbo1.destroy(gl);
        st.destroy(gl);

        fbo0Tex = null;
        fbo1Tex = null;
        sp0 = null;
        pmvMatrixUniform = null;
        interleavedVBO = null;
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        if( fbo0.getNumSamples() != numSamples ) {
            System.err.println("**** NumSamples: "+fbo0.getNumSamples()+" -> "+numSamples);
            resetFBOs(gl, drawable);
        }

        if(0 < numSamples) {
            gl.glEnable(GL.GL_MULTISAMPLE);
        }

        fbo0.bind(gl);
        demo0.display(drawable);
        fbo0.unbind(gl);

        if(!demo0Only) {
            fbo1.bind(gl);
            demo1.display(drawable);
            fbo1.unbind(gl);
        }

        st.useProgram(gl, true);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit0.intValue());
        fbo0.use(gl, fbo0Tex);
        if(!demo0Only) {
            gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit1.intValue());
            fbo1.use(gl, fbo1Tex);
        }
        interleavedVBO.enableBuffer(gl, true);

        if( !gl.isGLcore() ) {
            gl.glEnable(GL.GL_TEXTURE_2D);
        }

        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);

        interleavedVBO.enableBuffer(gl, false);
        fbo0.unuse(gl);
        if(!demo0Only) {
            fbo1.unuse(gl);
        }

        st.useProgram(gl, false);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.setSwapInterval(swapInterval); // in case switching the drawable (impl. may bound attribute there)

        System.err.println("**** Reshape: "+width+"x"+height);
        resetFBOs(gl, drawable);

        fbo0.bind(gl);
        demo0.reshape(drawable, x, y, width, height);
        fbo0.unbind(gl);
        fbo1.bind(gl);
        demo1.reshape(drawable, x, y, width, height);
        fbo1.unbind(gl);

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

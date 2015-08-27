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

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.test.junit.jogl.demos.PointsDemo;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

public class PointsDemoES2 extends PointsDemo {
    ShaderState st;
    PMVMatrix pmvMatrix;
    GLUniformData pmvMatrixUniform;
    GLArrayDataServer vertices ;
    GLArrayDataServer pointSizes ;
    private int swapInterval = 0;
    final int edge = 8; // 8*8
    /** vec4[2]: { (sz, smooth, attnMinSz, attnMaxSz), (attnCoeff(3), attnFadeTs) } */
    private static final String mgl_PointParams     = "mgl_PointParams";

    /** ( pointSize, pointSmooth, attn. pointMinSize, attn. pointMaxSize ) , ( attenuation coefficients 1f 0f 0f, attenuation fade theshold 1f )   */
    private final FloatBuffer pointParams = Buffers.newDirectFloatBuffer(new float[] {  1.0f, 0.0f, 0.0f, 4096.0f, 1.0f, 0.0f, 0.0f, 1.0f });

    public PointsDemoES2(final int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public PointsDemoES2() {
        this.swapInterval = 1;
    }

    public void setSmoothPoints(final boolean v) {
        pointParams.put(1, v ? 1.0f : 0.0f);
    }

    public void setPointParams(final float minSize, final float maxSize, final float distAttenConst, final float distAttenLinear, final float distAttenQuadratic, final float fadeThreshold) {
        pointParams.put(2, minSize);
        pointParams.put(3, maxSize);
        pointParams.put(4+0, distAttenConst);
        pointParams.put(4+1, distAttenLinear);
        pointParams.put(4+2, distAttenQuadratic);
        pointParams.put(4+3, fadeThreshold);
    }

    public void init(final GLAutoDrawable glad) {
        final GL2ES2 gl = glad.getGL().getGL2ES2();

        System.err.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
        System.err.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
        System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
        System.err.println("GL GLSL: "+gl.hasGLSL()+", has-compiler-func: "+gl.isFunctionAvailable("glCompileShader")+", version "+(gl.hasGLSL() ? gl.glGetString(GL2ES2.GL_SHADING_LANGUAGE_VERSION) : "none"));
        System.err.println("GL Profile: "+gl.getGLProfile());

        st = new ShaderState();
        st.setVerbose(true);
        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(), "shader",
                "shader/bin", "PointsShader", true);
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(), "shader",
                "shader/bin", "PointsShader", true);
        vp0.defaultShaderCustomization(gl, true, true);
        fp0.defaultShaderCustomization(gl, true, true);
        final ShaderProgram sp0 = new ShaderProgram();
        sp0.add(gl, vp0, System.err);
        sp0.add(gl, fp0, System.err);
        st.attachShaderProgram(gl, sp0, true);

        // setup mgl_PMVMatrix
        pmvMatrix = new PMVMatrix();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf()); // P, Mv
        st.ownUniform(pmvMatrixUniform);
        st.uniform(gl, pmvMatrixUniform);

        st.uniform(gl, new GLUniformData(mgl_PointParams, 4, pointParams));

        final GLUniformData colorStaticUniform = new GLUniformData("mgl_ColorStatic", 4, Buffers.newDirectFloatBuffer(new float[] { 1.0f, 1.0f, 1.0f, 1.0f }) );
        st.uniform(gl, colorStaticUniform);
        st.ownUniform(colorStaticUniform);

        // Allocate Vertex Array
        vertices = GLArrayDataServer.createGLSL("mgl_Vertex", 3, GL.GL_FLOAT, false, edge*edge, GL.GL_STATIC_DRAW);
        pointSizes = GLArrayDataServer.createGLSL("mgl_PointSize", 1, GL.GL_FLOAT, false, edge*edge, GL.GL_STATIC_DRAW);
        for(int i=0; i<edge; i++) {
            for(int j=0; j<edge; j++) {
                final float x = -3+j*0.7f;
                final float y = -3+i*0.7f;
                final float p = (i*edge+j)*0.5f;
                // System.err.println("["+j+"/"+i+"]: "+x+"/"+y+": "+p);
                vertices.putf(x); vertices.putf(y); vertices.putf( 0);
                pointSizes.putf(p);
            }
        }
        vertices.seal(gl, true);
        st.ownAttribute(vertices, true);
        vertices.enableBuffer(gl, false);
        pointSizes.seal(gl, true);
        st.ownAttribute(pointSizes, true);
        pointSizes.enableBuffer(gl, false);

        // OpenGL Render Settings
        gl.glEnable(GL.GL_DEPTH_TEST);
        st.useProgram(gl, false);
    }

    public void display(final GLAutoDrawable glad) {
        final GL2ES2 gl = glad.getGL().getGL2ES2();
        gl.glClearColor(0f, 0f, 0f, 0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        st.useProgram(gl, true);
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        st.uniform(gl, pmvMatrixUniform);

        final GLUniformData ud = st.getUniform(mgl_PointParams);
        if(null!=ud) {
            // same data object
            st.uniform(gl, ud);
        }

        vertices.enableBuffer(gl, true);
        pointSizes.enableBuffer(gl, true);

        if(gl.isGL2GL3()) {
            gl.glEnable(GL2GL3.GL_VERTEX_PROGRAM_POINT_SIZE);
        }
        if(gl.isGL2ES1()) {
            gl.glEnable(GL2ES1.GL_POINT_SPRITE); // otherwise no gl_PointCoord
        }
        gl.glEnable ( GL.GL_BLEND );
        gl.glBlendFunc ( GL.GL_SRC_ALPHA, GL.GL_ONE );

        gl.glDrawArrays(GL.GL_POINTS, 0, edge*edge);

        if(gl.isGL2GL3()) {
            gl.glDisable(GL2GL3.GL_VERTEX_PROGRAM_POINT_SIZE);
        }

        pointSizes.enableBuffer(gl, false);
        vertices.enableBuffer(gl, false);
        st.useProgram(gl, false);
    }

    public void reshape(final GLAutoDrawable glad, final int x, final int y, final int width, final int height) {
        // Thread.dumpStack();
        final GL2ES2 gl = glad.getGL().getGL2ES2();

        gl.setSwapInterval(swapInterval); // in case switching the drawable (impl. may bound attribute there)

        st.useProgram(gl, true);
        // Set location in front of camera
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0F, ( (float) width / (float) height ) / 1.0f, 1.0F, 100.0F);
        //pmvMatrix.glOrthof(-4.0f, 4.0f, -4.0f, 4.0f, 1.0f, 100.0f);
        st.uniform(gl, pmvMatrixUniform);
        st.useProgram(gl, false);
    }

    public void dispose(final GLAutoDrawable glad) {
        final GL2ES2 gl = glad.getGL().getGL2ES2();
        st.destroy(gl);
        st = null;
        pmvMatrix = null;
    }
}

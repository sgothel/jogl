/**
 * Copyright (C) 2015 JogAmp Community. All rights reserved.
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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.opengl.util.ImmModeSink;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

public class LineSquareXDemoES2 implements GLEventListener {

    private boolean multisample, clearBuffers;
    private final ShaderState st;
    private final PMVMatrix pmvMatrix;
    private ShaderProgram sp0;
    private GLUniformData pmvMatrixUniform;
    private ImmModeSink immModeSink;

    public LineSquareXDemoES2(final boolean multisample) {
        this.multisample = multisample;
        this.clearBuffers = true;
        st = new ShaderState();
        st.setVerbose(true);
        pmvMatrix = new PMVMatrix();
    }

    public void setClearBuffers(final boolean v) { clearBuffers = v; }

    public void init(final GLAutoDrawable glad) {
        final GL2ES2 gl = glad.getGL().getGL2ES2();

        System.err.println();
        System.err.println("req. msaa: "+multisample);
        System.err.println("Requested: " + glad.getNativeSurface().getGraphicsConfiguration().getRequestedCapabilities());
        multisample = multisample && glad.getChosenGLCapabilities().getNumSamples() > 0 ;
        System.err.println("Chosen   : " + glad.getChosenGLCapabilities());
        System.err.println("has  msaa: "+multisample);
        System.err.println();

        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, LineSquareXDemoES2.class, "shader",
                "shader/bin", "mgl_default_xxx", true);
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, LineSquareXDemoES2.class, "shader",
                "shader/bin", "mgl_default_xxx", true);
        vp0.defaultShaderCustomization(gl, true, true);
        fp0.defaultShaderCustomization(gl, true, true);

        sp0 = new ShaderProgram();
        sp0.add(gl, vp0, System.err);
        sp0.add(gl, fp0, System.err);
        st.attachShaderProgram(gl, sp0, true);

        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        st.ownUniform(pmvMatrixUniform);
        st.uniform(gl, pmvMatrixUniform);

        final float c = 0f;
        final float eX = 0.5f;
        final float eH = 0.98f;
        final float e2 = 1f;

        // Using predef array names, see
        //    GLPointerFuncUtil.getPredefinedArrayIndexName(glArrayIndex);
        immModeSink = ImmModeSink.createGLSL(20*2,
                                              3, GL.GL_FLOAT,  // vertex
                                              4, GL.GL_FLOAT,  // color
                                              0, GL.GL_FLOAT,  // normal
                                              0, GL.GL_FLOAT,  // texCoords
                                              GL.GL_STATIC_DRAW, st);
        immModeSink.glBegin(GL.GL_LINES);

        // Rectangle
        immModeSink.glVertex3f(-eX, -eH, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f(-eX,  eH, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f(-eX,  eH, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f( eX,  eH, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f( eX,  eH, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f( eX, -eH, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f( eX, -eH, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f(-eX, -eH, 0f); immModeSink.glColor4f( c, c, c, c );

        // Square
        immModeSink.glVertex3f(-eH, -eH, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f(-eH,  eH, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f(-eH,  eH, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f( eH,  eH, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f( eH,  eH, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f( eH, -eH, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f( eH, -eH, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f(-eH, -eH, 0f); immModeSink.glColor4f( c, c, c, c );

        // X
        immModeSink.glVertex3f(-e2, -e2, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f( e2,  e2, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f(-e2,  e2, 0f); immModeSink.glColor4f( c, c, c, c );
        immModeSink.glVertex3f( e2, -e2, 0f); immModeSink.glColor4f( c, c, c, c );

        immModeSink.glEnd(gl, false);

        st.useProgram(gl, false);
    }

    public void dispose(final GLAutoDrawable glad) {
        final GL2ES2 gl = glad.getGL().getGL2ES2();
        immModeSink.destroy(gl);
        immModeSink = null;
        st.destroy(gl);
    }

    public void display(final GLAutoDrawable glad) {
        final GL2ES2 gl = glad.getGL().getGL2ES2();
        if (multisample) {
            gl.glEnable(GL.GL_MULTISAMPLE);
        }
        if( clearBuffers ) {
            final float c = 0.9f;
            gl.glClearColor(c, c, c, 0);
            //      gl.glEnable(GL.GL_DEPTH_TEST);
            //      gl.glDepthFunc(GL.GL_LESS);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        }

        st.useProgram(gl, true);

        immModeSink.draw(gl, true);

        st.useProgram(gl, false);
    }

    // Unused routines
    public void reshape(final GLAutoDrawable glad, final int x, final int y, final int width, final int height) {
        System.err.println("reshape ..");
        final GL2ES2 gl = glad.getGL().getGL2ES2();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        final float left, right, bottom, top;
        if( height > width ) {
            final float a = (float)height / (float)width;
            left = -1.0f;
            right = 1.0f;
            bottom = -a;
            top = a;
        } else {
            final float a = (float)width / (float)height;
            left = -a;
            right = a;
            bottom = -1.0f;
            top = 1.0f;
        }
        // pmvMatrix.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);
        // pmvMatrix.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 10.0f);
        pmvMatrix.glOrthof(left, right, top, bottom, 0.0f, 10.0f);
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();

        st.useProgram(gl, true);
        st.uniform(gl, pmvMatrixUniform);
        st.useProgram(gl, false);
    }

    public void displayChanged(final GLAutoDrawable drawable, final boolean modeChanged, final boolean deviceChanged) {
    }
}

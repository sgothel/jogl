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

package com.jogamp.opengl.test.junit.jogl.util;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.opengl.util.ImmModeSink;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

public class DemoGL2ES2ImmModeSink implements GLEventListener {

    private final ShaderState st;
    private final PMVMatrix pmvMatrix;
    private final int glBufferUsage;
    private ShaderProgram sp;
    private GLUniformData pmvMatrixUniform;
    private ImmModeSink ims;

    public DemoGL2ES2ImmModeSink(final boolean useVBO, final boolean useShaderState) {
        if(useShaderState) {
            st = new ShaderState();
            st.setVerbose(true);
        } else {
            st = null;
        }
        glBufferUsage = useVBO ? GL.GL_STATIC_DRAW : 0;
        pmvMatrix = new PMVMatrix();
    }

    public void init(final GLAutoDrawable glad) {
        final GL2ES2 gl = glad.getGL().getGL2ES2();

        System.err.println("GL_VENDOR   "+gl.glGetString(GL.GL_VENDOR));
        System.err.println("GL_RENDERER "+gl.glGetString(GL.GL_RENDERER));
        System.err.println("GL_VERSION  "+gl.glGetString(GL.GL_VERSION));

        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, DemoGL2ES2ImmModeSink.class,
                "../demos/es2/shader", "../demos/es2/shader/bin", "mgl_default_xxx", true);
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, DemoGL2ES2ImmModeSink.class,
                "../demos/es2/shader", "../demos/es2/shader/bin", "mgl_default_xxx", true);
        vp0.defaultShaderCustomization(gl, true, true);
        fp0.defaultShaderCustomization(gl, true, true);

        sp = new ShaderProgram();
        sp.add(gl, vp0, System.err);
        sp.add(gl, fp0, System.err);
        if( null != st ) {
            st.attachShaderProgram(gl, sp, true);
        } else {
            if(!sp.link(gl, System.err)) {
                throw new GLException("Could not link program: "+sp);
            }
            sp.useProgram(gl, true);
        }

        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        if(null != st) {
            st.ownUniform(pmvMatrixUniform);
            st.uniform(gl, pmvMatrixUniform);
        } else {
            if( pmvMatrixUniform.setLocation(gl, sp.program()) < 0 ) {
                throw new GLException("Could not find location for uniform: "+pmvMatrixUniform+", "+sp);
            }
            gl.glUniform(pmvMatrixUniform);
        }

        // Using predef array names, see
        //    GLPointerFuncUtil.getPredefinedArrayIndexName(glArrayIndex);
        if( null != st ) {
            ims = ImmModeSink.createGLSL(40,
                                         3, GL.GL_FLOAT,  // vertex
                                         4, GL.GL_FLOAT,  // color
                                         0, GL.GL_FLOAT,  // normal
                                         0, GL.GL_FLOAT,  // texCoords
                                         glBufferUsage, st);
        } else {
            ims = ImmModeSink.createGLSL(40,
                                         3, GL.GL_FLOAT,  // vertex
                                         4, GL.GL_FLOAT,  // color
                                         0, GL.GL_FLOAT,  // normal
                                         0, GL.GL_FLOAT,  // texCoords
                                         glBufferUsage, sp.program());
        }
        final int numSteps = 20;
        final double increment = Math.PI / numSteps;
        final double radius = 1;
        ims.glBegin(GL.GL_LINES);
        for (int i = numSteps - 1; i >= 0; i--) {
            ims.glVertex3f((float) (radius * Math.cos(i * increment)),
                                   (float) (radius * Math.sin(i * increment)),
                                   0f);
            ims.glColor4f( 1f, 1f, 1f, 1f );
            ims.glVertex3f((float) (-1.0 * radius * Math.cos(i * increment)),
                                   (float) (-1.0 * radius * Math.sin(i * increment)),
                                   0f);
            ims.glColor4f( 1f, 1f, 1f, 1f );
        }
        ims.glEnd(gl, false);

        if(null != st) {
            st.useProgram(gl, false);
        } else {
            gl.glUseProgram(0);
        }
    }

    public void dispose(final GLAutoDrawable glad) {
        final GL2ES2 gl = glad.getGL().getGL2ES2();
        ims.destroy(gl);
        ims = null;
        if(null != st) {
            st.destroy(gl);
        }
    }

    public void display(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClear( GL.GL_COLOR_BUFFER_BIT );

        // draw a triangle filling the window
        ims.glBegin(GL.GL_TRIANGLES);
        ims.glColor3f( 1, 0, 0 );
        ims.glVertex2f( 0, 0 );
        ims.glColor3f( 0, 1, 0 );
        ims.glVertex2f( drawable.getSurfaceWidth(), 0 );
        ims.glColor3f( 0, 0, 1 );
        ims.glVertex2f( drawable.getSurfaceWidth() / 2f, drawable.getSurfaceHeight() );
        ims.glEnd(gl, true);
    }

    // Unused routines
    public void reshape(final GLAutoDrawable glad, final int x, final int y, final int width, final int height) {
        System.err.println("reshape ..");
        final GL2ES2 gl = glad.getGL().getGL2ES2();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();

        // coordinate system origin at lower left with width and height same as the window
        pmvMatrix.glOrthof( 0.0f, width, 0.0f, height, -1, 1 );

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();

        if(null != st) {
            st.useProgram(gl, true);
            st.uniform(gl, pmvMatrixUniform);
            st.useProgram(gl, false);
        } else {
            gl.glUseProgram(sp.program());
            gl.glUniform(pmvMatrixUniform);
            gl.glUseProgram(0);
        }
    }

    public void displayChanged(final GLAutoDrawable drawable, final boolean modeChanged, final boolean deviceChanged) {
    }
}

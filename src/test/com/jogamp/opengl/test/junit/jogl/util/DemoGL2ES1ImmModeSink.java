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
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.gl2es1.GLUgl2es1;

import com.jogamp.opengl.util.ImmModeSink;
import com.jogamp.opengl.util.glsl.fixedfunc.FixedFuncUtil;
import com.jogamp.opengl.util.glsl.fixedfunc.ShaderSelectionMode;

class DemoGL2ES1ImmModeSink implements GLEventListener {
    private boolean debugFFPEmu = false;
    private boolean verboseFFPEmu = false;
    private boolean traceFFPEmu = false;
    private boolean forceFFPEmu = false;
    final ImmModeSink ims;
    final GLU glu;

    DemoGL2ES1ImmModeSink(final boolean useVBO) {
        ims = ImmModeSink.createFixed(3*3,
                                      3, GL.GL_FLOAT, // vertex
                                      3, GL.GL_FLOAT, // color
                                      0, GL.GL_FLOAT, // normal
                                      0, GL.GL_FLOAT, // texCoords
                                      useVBO ? GL.GL_STATIC_DRAW : 0);
        glu = new GLUgl2es1();
    }

    public void setForceFFPEmu(final boolean forceFFPEmu, final boolean verboseFFPEmu, final boolean debugFFPEmu, final boolean traceFFPEmu) {
        this.forceFFPEmu = forceFFPEmu;
        this.verboseFFPEmu = verboseFFPEmu;
        this.debugFFPEmu = debugFFPEmu;
        this.traceFFPEmu = traceFFPEmu;
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        GL _gl = drawable.getGL();
        if(debugFFPEmu) {
            // Debug ..
            _gl = _gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", GL2ES2.class, _gl, null) );
        }
        if(traceFFPEmu) {
            // Trace ..
            _gl = _gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", GL2ES2.class, _gl, new Object[] { System.err } ) );
        }
        final GL2ES1 gl = FixedFuncUtil.wrapFixedFuncEmul(_gl, ShaderSelectionMode.AUTO, null, forceFFPEmu, verboseFFPEmu);

        System.err.println("GL_VENDOR   "+gl.glGetString(GL.GL_VENDOR));
        System.err.println("GL_RENDERER "+gl.glGetString(GL.GL_RENDERER));
        System.err.println("GL_VERSION  "+gl.glGetString(GL.GL_VERSION));
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL2ES1 gl = drawable.getGL().getGL2ES1();

        gl.glMatrixMode( GLMatrixFunc.GL_PROJECTION );
        gl.glLoadIdentity();

        // coordinate system origin at lower left with width and height same as the window
        glu.gluOrtho2D( 0.0f, width, 0.0f, height );

        gl.glMatrixMode( GLMatrixFunc.GL_MODELVIEW );
        gl.glLoadIdentity();
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL2ES1 gl = drawable.getGL().getGL2ES1();

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

    @Override
    public void dispose(final GLAutoDrawable drawable) {
    }
}
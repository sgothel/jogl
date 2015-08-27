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
package com.jogamp.opengl.test.junit.jogl.demos.es1;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.test.junit.jogl.demos.PointsDemo;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.fixedfunc.FixedFuncUtil;
import com.jogamp.opengl.util.glsl.fixedfunc.ShaderSelectionMode;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.fixedfunc.GLPointerFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.gl2es1.GLUgl2es1;

public class PointsDemoES1 extends PointsDemo {
    final static GLU glu = new GLUgl2es1();
    private boolean debugFFPEmu = false;
    private boolean verboseFFPEmu = false;
    private boolean traceFFPEmu = false;
    private boolean forceFFPEmu = false;
    private boolean debug = false ;
    private boolean trace = false ;
    GLArrayDataServer vertices ;
    float[] pointSizes ;
    private int swapInterval = 0;
    final int edge = 8; // 8*8
    boolean smooth = false;

    public PointsDemoES1(final int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public PointsDemoES1() {
        this.swapInterval = 1;
    }

    public void setForceFFPEmu(final boolean forceFFPEmu, final boolean verboseFFPEmu, final boolean debugFFPEmu, final boolean traceFFPEmu) {
        this.forceFFPEmu = forceFFPEmu;
        this.verboseFFPEmu = verboseFFPEmu;
        this.debugFFPEmu = debugFFPEmu;
        this.traceFFPEmu = traceFFPEmu;
    }

    public void setSmoothPoints(final boolean v) { smooth = v; }

    public void init(final GLAutoDrawable glad) {
        GL _gl = glad.getGL();

        if(debugFFPEmu) {
            // Debug ..
            _gl = _gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", GL2ES2.class, _gl, null) );
            debug = false;
        }
        if(traceFFPEmu) {
            // Trace ..
            _gl = _gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", GL2ES2.class, _gl, new Object[] { System.err } ) );
            trace = false;
        }
        GL2ES1 gl = FixedFuncUtil.wrapFixedFuncEmul(_gl, ShaderSelectionMode.AUTO, null, forceFFPEmu, verboseFFPEmu);

        if(debug) {
            try {
                // Debug ..
                gl = (GL2ES1) gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", GL2ES1.class, gl, null) );
            } catch (final Exception e) {e.printStackTrace();}
        }
        if(trace) {
            try {
                // Trace ..
                gl = (GL2ES1) gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", GL2ES1.class, gl, new Object[] { System.err } ) );
            } catch (final Exception e) {e.printStackTrace();}
        }

        System.err.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
        System.err.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
        System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
        System.err.println("GL Profile: "+gl.getGLProfile());

        // Allocate Vertex Array
        vertices = GLArrayDataServer.createFixed(GLPointerFunc.GL_VERTEX_ARRAY, 3, GL.GL_FLOAT, false, edge*edge, GL.GL_STATIC_DRAW);
        pointSizes = new float[edge*edge];
        for(int i=0; i<edge; i++) {
            for(int j=0; j<edge; j++) {
                final float x = -3+j*0.7f;
                final float y = -3+i*0.7f;
                final float p = Math.max(0.000001f, (i*edge+j)*0.5f); // no zero point size!
                // System.err.println("["+j+"/"+i+"]: "+x+"/"+y+": "+p);
                vertices.putf(x); vertices.putf(y); vertices.putf( 0);
                pointSizes[(i*edge+j)] = p;
            }
        }
        vertices.seal(gl, true);
        vertices.enableBuffer(gl, false);

        // OpenGL Render Settings
        gl.glEnable(GL.GL_DEPTH_TEST);
    }

    public void setPointParams(final float minSize, final float maxSize, final float distAttenConst, final float distAttenLinear, final float distAttenQuadratic, final float fadeThreshold) {
        pointMinSize = minSize;
        pointMaxSize = maxSize;
        pointFadeThreshold = fadeThreshold;
        pointDistAtten.put(0, distAttenConst);
        pointDistAtten.put(1, distAttenLinear);
        pointDistAtten.put(2, distAttenQuadratic);
    }

    /** default values */
    private float pointMinSize = 0.0f;
    private float pointMaxSize = 4096.0f;
    private float pointFadeThreshold = 1.0f;
    private final FloatBuffer pointDistAtten = Buffers.newDirectFloatBuffer(new float[] {  1.0f, 0.0f, 0.0f });

    public void display(final GLAutoDrawable glad) {
        final GL2ES1 gl = glad.getGL().getGL2ES1();
        gl.glClearColor(0f, 0f, 0f, 0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -10);

        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f );

        vertices.enableBuffer(gl, true);

        gl.glEnable ( GL.GL_BLEND );
        gl.glBlendFunc ( GL.GL_SRC_ALPHA, GL.GL_ONE );
        if(smooth) {
            gl.glEnable(GL2ES1.GL_POINT_SMOOTH);
        } else {
            gl.glDisable(GL2ES1.GL_POINT_SMOOTH);
        }
        gl.glPointParameterf(GL2ES1.GL_POINT_SIZE_MIN, pointMinSize );
        gl.glPointParameterf(GL2ES1.GL_POINT_SIZE_MAX, pointMaxSize );
        gl.glPointParameterf(GL.GL_POINT_FADE_THRESHOLD_SIZE, pointFadeThreshold);
        gl.glPointParameterfv(GL2ES1.GL_POINT_DISTANCE_ATTENUATION, pointDistAtten );

        for(int i=edge*edge-1; i>=0; i--) {
            gl.glPointSize(pointSizes[i]);
            gl.glDrawArrays(GL.GL_POINTS, i, 1);
        }

        vertices.enableBuffer(gl, false);
    }

    public void reshape(final GLAutoDrawable glad, final int x, final int y, final int width, final int height) {
        // Thread.dumpStack();
        final GL2ES1 gl = glad.getGL().getGL2ES1();

        gl.setSwapInterval(swapInterval); // in case switching the drawable (impl. may bound attribute there)

        // Set location in front of camera
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0F, ( (float) width / (float) height ) / 1.0f, 1.0F, 100.0F);
        //gl.glOrthof(-4.0f, 4.0f, -4.0f, 4.0f, 1.0f, 100.0f);
    }

    public void dispose(final GLAutoDrawable glad) {
        final GL2ES1 gl = glad.getGL().getGL2ES1();
        vertices.destroy(gl);
        vertices = null;
    }
}

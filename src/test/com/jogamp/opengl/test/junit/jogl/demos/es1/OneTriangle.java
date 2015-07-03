/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.gl2es1.GLUgl2es1;

import com.jogamp.opengl.util.ImmModeSink;

/**
 * A utility class to encapsulate drawing a single triangle for unit tests.
 * @author Wade Walker
 */
public class OneTriangle {

    public static void setup( final GL2ES1 gl, final int width, final int height ) {
        gl.glMatrixMode( GLMatrixFunc.GL_PROJECTION );
        gl.glLoadIdentity();

        // coordinate system origin at lower left with width and height same as the window
        final GLU glu = new GLUgl2es1();
        glu.gluOrtho2D( 0.0f, width, 0.0f, height );

        gl.glMatrixMode( GLMatrixFunc.GL_MODELVIEW );
        gl.glLoadIdentity();

        gl.glViewport( 0, 0, width, height );
    }

    public static void render( final GL2ES1 gl, final int width, final int height) {
        gl.glClear( GL.GL_COLOR_BUFFER_BIT );

        // draw a triangle filling the window
        gl.glLoadIdentity();

        final ImmModeSink immModeSink = ImmModeSink.createFixed(3*3,
                                                          3, GL.GL_FLOAT, // vertex
                                                          3, GL.GL_FLOAT, // color
                                                          0, GL.GL_FLOAT, // normal
                                                          0, GL.GL_FLOAT, // texCoords
                                                          GL.GL_STATIC_DRAW);
        immModeSink.glBegin(GL.GL_TRIANGLES);
        immModeSink.glColor3f( 1, 0, 0 );
        immModeSink.glVertex2f( 0, 0 );
        immModeSink.glColor3f( 0, 1, 0 );
        immModeSink.glVertex2f( width, 0 );
        immModeSink.glColor3f( 0, 0, 1 );
        immModeSink.glVertex2f( width / 2f, height );
        immModeSink.glEnd(gl, true);
    }
}

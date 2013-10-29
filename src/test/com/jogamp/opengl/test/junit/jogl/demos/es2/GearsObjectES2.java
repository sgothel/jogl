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
import javax.media.opengl.GLUniformData;

import com.jogamp.opengl.test.junit.jogl.demos.GearsObject;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderState;

/**
 * GearsObjectES2.java <BR>
 * @author Brian Paul (converted to Java by Ron Cemer and Sven Gothel) <P>
 */
public class GearsObjectES2 extends GearsObject {
    final PMVMatrix pmvMatrix;
    final GLUniformData pmvMatrixUniform;
    final GLUniformData colorUniform;
    final ShaderState st;

    public GearsObjectES2(ShaderState st, FloatBuffer gearColor, float inner_radius, float outer_radius,
                          float width, int teeth,
                          float tooth_depth,
                          PMVMatrix pmvMatrix,
                          GLUniformData pmvMatrixUniform, GLUniformData colorUniform)
    {
        super(gearColor, inner_radius, outer_radius, width, teeth, tooth_depth);
        this.pmvMatrix = pmvMatrix;
        this.pmvMatrixUniform = pmvMatrixUniform;
        this.colorUniform = colorUniform;
        this.st = st;
        associate(st);
    }

    public GearsObjectES2(GearsObjectES2 shared,
                          ShaderState st,
                          PMVMatrix pmvMatrix,
                          GLUniformData pmvMatrixUniform, GLUniformData colorUniform)
    {
        super(shared);
        this.pmvMatrix = pmvMatrix;
        this.pmvMatrixUniform = pmvMatrixUniform;
        this.colorUniform = colorUniform;
        this.st = st;
        associate(st);
    }

    private void associate(ShaderState st) {
        frontFace.associate(st, true);
        frontSide.associate(st, true);
        backFace.associate(st, true);
        backSide.associate(st, true);
        outwardFace.associate(st, true);
        insideRadiusCyl.associate(st, true);
    }

    @Override
    public GLArrayDataServer createInterleaved(int comps, int dataType, boolean normalized, int initialSize, int vboUsage) {
        return GLArrayDataServer.createGLSLInterleaved(comps, dataType, normalized, initialSize, vboUsage);
    }

    @Override
    public void addInterleavedVertexAndNormalArrays(GLArrayDataServer array, int components) {
        array.addGLSLSubArray("vertices", components, GL.GL_ARRAY_BUFFER);
        array.addGLSLSubArray("normals", components, GL.GL_ARRAY_BUFFER);
    }

    private void draw(GL2ES2 gl, GLArrayDataServer array, int mode, int face) {
        array.enableBuffer(gl, true);
        // System.err.println("XXX Draw face "+face+" of "+this);
        gl.glDrawArrays(mode, 0, array.getElementCount());
        array.enableBuffer(gl, false);
    }

    @Override
    public void draw(GL _gl, float x, float y, float angle) {
        final GL2ES2 gl = _gl.getGL2ES2();
        pmvMatrix.glPushMatrix();
        pmvMatrix.glTranslatef(x, y, 0f);
        pmvMatrix.glRotatef(angle, 0f, 0f, 1f);
        if( pmvMatrix.update() ) {
            st.uniform(gl, pmvMatrixUniform);
        } else {
            throw new InternalError("PMVMatrix.update() returns false after mutable operations");
        }

        colorUniform.setData(gearColor);
        st.uniform(gl, colorUniform);

        draw(gl, frontFace, GL.GL_TRIANGLE_STRIP, 0);
        draw(gl, frontSide, GL.GL_TRIANGLES, 1);
        draw(gl, backFace, GL.GL_TRIANGLE_STRIP, 2);
        draw(gl, backSide, GL.GL_TRIANGLES, 3);
        draw(gl, outwardFace, GL.GL_TRIANGLE_STRIP, 4);
        draw(gl, insideRadiusCyl, GL.GL_TRIANGLE_STRIP, 5);

        pmvMatrix.glPopMatrix();
    }
}

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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLBufferStorage;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLUniformData;

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

    public GearsObjectES2(final GL gl, final boolean useMappedBuffers, final ShaderState st, final FloatBuffer gearColor,
                          final float inner_radius, final float outer_radius,
                          final float width,
                          final int teeth,
                          final float tooth_depth, final PMVMatrix pmvMatrix, final GLUniformData pmvMatrixUniform, final GLUniformData colorUniform, final boolean validateBuffers)
    {
        super(gl, useMappedBuffers, gearColor, inner_radius, outer_radius, width, teeth, tooth_depth, validateBuffers);
        this.pmvMatrix = pmvMatrix;
        this.pmvMatrixUniform = pmvMatrixUniform;
        this.colorUniform = colorUniform;
        this.st = st;
        associate(st);
    }

    public GearsObjectES2(final GearsObjectES2 shared,
                          final ShaderState st,
                          final PMVMatrix pmvMatrix,
                          final GLUniformData pmvMatrixUniform, final GLUniformData colorUniform)
    {
        super(shared);
        this.pmvMatrix = pmvMatrix;
        this.pmvMatrixUniform = pmvMatrixUniform;
        this.colorUniform = colorUniform;
        this.st = st;
        associate(st);
    }

    private void associate(final ShaderState st) {
        frontFace.associate(st, true);
        frontSide.associate(st, true);
        backFace.associate(st, true);
        backSide.associate(st, true);
        outwardFace.associate(st, true);
        insideRadiusCyl.associate(st, true);
    }

    @Override
    public GLArrayDataServer createInterleaved(final boolean useMappedBuffers, final int comps, final int dataType, final boolean normalized, final int initialSize, final int vboUsage) {
        if( useMappedBuffers ) {
            return GLArrayDataServer.createGLSLInterleavedMapped(comps, dataType, normalized, initialSize, vboUsage);
        } else {
            return GLArrayDataServer.createGLSLInterleaved(comps, dataType, normalized, initialSize, vboUsage);
        }
    }

    @Override
    public void addInterleavedVertexAndNormalArrays(final GLArrayDataServer array, final int components) {
        array.addGLSLSubArray("vertices", components, GL.GL_ARRAY_BUFFER);
        array.addGLSLSubArray("normals", components, GL.GL_ARRAY_BUFFER);
    }

    private void draw(final GL2ES2 gl, final GLArrayDataServer array, final int mode, final int face) {
        if( !isShared || gl.glIsBuffer(array.getVBOName()) ) {
            if( validateBuffers ) {
                array.bindBuffer(gl, true);
                final int bufferTarget = array.getVBOTarget();
                final int bufferName = array.getVBOName();
                final long bufferSize = array.getSizeInBytes();
                final int hasBufferName = gl.getBoundBuffer(bufferTarget);
                final GLBufferStorage hasStorage = gl.getBufferStorage(hasBufferName);
                final boolean ok = bufferName == hasBufferName &&
                                   bufferName == hasStorage.getName() &&
                                   bufferSize == hasStorage.getSize();
                if( !ok ) {
                    throw new GLException("GLBufferStorage Validation Error: Target[exp 0x"+Integer.toHexString(bufferTarget)+", has 0x"+Integer.toHexString(bufferTarget)+
                                          ", Name[exp "+bufferName+", has "+hasBufferName+", Size exp "+bufferSize+", Storage "+hasStorage+"]");
                }
            }
            array.enableBuffer(gl, true);
            // System.err.println("XXX Draw face "+face+" of "+this);
            gl.glDrawArrays(mode, 0, array.getElementCount());
            array.enableBuffer(gl, false);
        }
    }

    @Override
    public void draw(final GL _gl, final float x, final float y, final float angle) {
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

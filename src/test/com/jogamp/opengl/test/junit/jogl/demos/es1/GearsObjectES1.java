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
package com.jogamp.opengl.test.junit.jogl.demos.es1;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GLBufferStorage;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLPointerFunc;

import com.jogamp.opengl.test.junit.jogl.demos.GearsObject;
import com.jogamp.opengl.util.GLArrayDataServer;

/**
 * GearsObjectES1.java <BR>
 * @author Brian Paul (converted to Java by Ron Cemer and Sven Gothel) <P>
 */
public class GearsObjectES1 extends GearsObject {

    public GearsObjectES1(final GL gl, final boolean useMappedBuffers, final FloatBuffer gearColor, final float inner_radius,
            final float outer_radius, final float width, final int teeth, final float tooth_depth, final boolean validateBuffers) {
        super(gl, useMappedBuffers, gearColor, inner_radius, outer_radius, width, teeth, tooth_depth, validateBuffers);
    }

    public GearsObjectES1(final GearsObject shared) {
        super(shared);
    }

    @Override
    public GLArrayDataServer createInterleaved(final boolean useMappedBuffers, final int comps, final int dataType, final boolean normalized, final int initialSize, final int vboUsage) {
        if( useMappedBuffers ) {
            return GLArrayDataServer.createFixedInterleavedMapped(comps, dataType, normalized, initialSize, vboUsage);
        } else {
            return GLArrayDataServer.createFixedInterleaved(comps, dataType, normalized, initialSize, vboUsage);
        }
    }

    @Override
    public void addInterleavedVertexAndNormalArrays(final GLArrayDataServer array, final int components) {
        array.addFixedSubArray(GLPointerFunc.GL_VERTEX_ARRAY, components, GL.GL_ARRAY_BUFFER);
        array.addFixedSubArray(GLPointerFunc.GL_NORMAL_ARRAY, components, GL.GL_ARRAY_BUFFER);
    }

    private void draw(final GL2ES1 gl, final GLArrayDataServer array, final int mode) {
        if( !isShared || gl.glIsBuffer(array.getVBOName()) ) {
            array.enableBuffer(gl, true);
            if( validateBuffers ) {
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
            gl.glDrawArrays(mode, 0, array.getElementCount());
            array.enableBuffer(gl, false);
        }
    }

    @Override
    public void draw(final GL _gl, final float x, final float y, final float angle) {
        final GL2ES1 gl = _gl.getGL2ES1();
        gl.glPushMatrix();
        gl.glTranslatef(x, y, 0f);
        gl.glRotatef(angle, 0f, 0f, 1f);
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GLLightingFunc.GL_AMBIENT_AND_DIFFUSE, gearColor);

        gl.glShadeModel(GLLightingFunc.GL_FLAT);
        draw(gl, frontFace, GL.GL_TRIANGLE_STRIP);
        draw(gl, frontSide, GL.GL_TRIANGLES);
        draw(gl, backFace, GL.GL_TRIANGLE_STRIP);
        draw(gl, backSide, GL.GL_TRIANGLES);
        draw(gl, outwardFace, GL.GL_TRIANGLE_STRIP);
        gl.glShadeModel(GLLightingFunc.GL_SMOOTH);
        draw(gl, insideRadiusCyl, GL.GL_TRIANGLE_STRIP);
        gl.glPopMatrix();
    }
}

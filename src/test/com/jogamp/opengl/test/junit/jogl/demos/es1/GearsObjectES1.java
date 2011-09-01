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

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.fixedfunc.GLPointerFunc;


import com.jogamp.opengl.test.junit.jogl.demos.GearsObject;
import com.jogamp.opengl.util.GLArrayDataServer;

/**
 * GearsObjectES1.java <BR>
 * @author Brian Paul (converted to Java by Ron Cemer and Sven Gothel) <P>
 */
public class GearsObjectES1 extends GearsObject {

    public GearsObjectES1(float inner_radius, float outer_radius, float width,
            int teeth, float tooth_depth) {
        super(inner_radius, outer_radius, width, teeth, tooth_depth);
    }

    @Override
    public GLArrayDataServer createInterleaved(int comps, int dataType, boolean normalized, int initialSize, int vboUsage) {
        return GLArrayDataServer.createFixedInterleaved(comps, dataType, normalized, initialSize, vboUsage);
    }
    
    @Override
    public void addInterleavedVertexAndNormalArrays(GLArrayDataServer array,
            int components) {
        array.addFixedSubArray(GLPointerFunc.GL_VERTEX_ARRAY, 3, GL.GL_ARRAY_BUFFER);
        array.addFixedSubArray(GLPointerFunc.GL_NORMAL_ARRAY, 3, GL.GL_ARRAY_BUFFER);
    }

    private void draw(GL2ES1 gl, GLArrayDataServer array, int mode) {
        array.enableBuffer(gl, true);
        gl.glDrawArrays(mode, 0, array.getElementCount());
        array.enableBuffer(gl, false);
    }

    @Override
    public void draw(GL _gl, float x, float y, float angle, FloatBuffer color) {
        GL2ES1 gl = _gl.getGL2ES1();        
        gl.glPushMatrix();
        gl.glTranslatef(x, y, 0f);
        gl.glRotatef(angle, 0f, 0f, 1f);
        gl.glMaterialfv(GL2ES1.GL_FRONT_AND_BACK, GL2ES1.GL_AMBIENT_AND_DIFFUSE, color);
        
        gl.glShadeModel(GL2ES1.GL_FLAT);
        draw(gl, frontFace, GL.GL_TRIANGLE_STRIP);
        draw(gl, frontSide, GL.GL_TRIANGLES);
        draw(gl, backFace, GL.GL_TRIANGLE_STRIP);
        draw(gl, backSide, GL.GL_TRIANGLES);
        draw(gl, outwardFace, GL.GL_TRIANGLE_STRIP);
        gl.glShadeModel(GL2ES1.GL_SMOOTH);          
        draw(gl, insideRadiusCyl, GL.GL_TRIANGLE_STRIP);
        gl.glPopMatrix();
    }    
}

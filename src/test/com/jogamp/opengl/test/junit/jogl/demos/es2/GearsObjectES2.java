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
    PMVMatrix pmvMatrix;
    GLUniformData pmvMatrixUniform;
    GLUniformData colorUniform;
    
    public GearsObjectES2(float inner_radius, float outer_radius, float width,
                          int teeth, float tooth_depth,
                          PMVMatrix pmvMatrix, 
                          GLUniformData pmvMatrixUniform,
                          GLUniformData colorUniform) 
    {
        super(inner_radius, outer_radius, width, teeth, tooth_depth);
        this.pmvMatrix = pmvMatrix;
        this.pmvMatrixUniform = pmvMatrixUniform;
        this.colorUniform = colorUniform;
    }

    public GearsObjectES2(GearsObject shared,
                          PMVMatrix pmvMatrix, 
                          GLUniformData pmvMatrixUniform,
                          GLUniformData colorUniform) 
    {
        super(shared);
        this.pmvMatrix = pmvMatrix;
        this.pmvMatrixUniform = pmvMatrixUniform;
        this.colorUniform = colorUniform;
    }

    @Override
    public GLArrayDataServer createInterleaved(int comps, int dataType, boolean normalized, int initialSize, int vboUsage) {
        return GLArrayDataServer.createGLSLInterleaved(comps, dataType, normalized, initialSize, vboUsage);
    }
    
    @Override
    public void addInterleavedVertexAndNormalArrays(GLArrayDataServer array,
            int components) {
        array.addGLSLSubArray("vertices", 3, GL.GL_ARRAY_BUFFER);
        array.addGLSLSubArray("normals", 3, GL.GL_ARRAY_BUFFER);
    }

    private void draw(GL2ES2 gl, GLArrayDataServer array, int mode) {
        array.enableBuffer(gl, true);
        gl.glDrawArrays(mode, 0, array.getElementCount());
        array.enableBuffer(gl, false);
    }

    @Override
    public void draw(GL _gl, float x, float y, float angle, FloatBuffer color) {
        final GL2ES2 gl = _gl.getGL2ES2();
        final ShaderState st = ShaderState.getShaderState(gl);
        pmvMatrix.glPushMatrix();
        pmvMatrix.glTranslatef(x, y, 0f);
        pmvMatrix.glRotatef(angle, 0f, 0f, 1f);
        pmvMatrix.update();
        st.uniform(gl, pmvMatrixUniform);

        colorUniform.setData(color);
        st.uniform(gl, colorUniform);

        draw(gl, frontFace, GL.GL_TRIANGLE_STRIP);
        draw(gl, frontSide, GL.GL_TRIANGLES);
        draw(gl, backFace, GL.GL_TRIANGLE_STRIP);
        draw(gl, backSide, GL.GL_TRIANGLES);
        draw(gl, outwardFace, GL.GL_TRIANGLE_STRIP);
        draw(gl, insideRadiusCyl, GL.GL_TRIANGLE_STRIP);
        
        pmvMatrix.glPopMatrix();
    }    
}

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
package com.jogamp.opengl.test.junit.jogl.demos.es2;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.GLArrayDataWrapper;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.test.junit.jogl.demos.es2.shader.RedSquareShader;
import com.jogamp.opengl.test.junit.util.GLSLSimpleProgram;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.FloatBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLUniformData;
import org.junit.Assert;

public class RedSquare0 implements GLEventListener {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream pbaos = new PrintStream(baos);
    GLSLSimpleProgram myShader;
    PMVMatrix pmvMatrix;
    int mgl_PMVMatrix;
    GLUniformData pmvMatrixUniform;
    int mgl_Vertex;
    int mgl_Color;
    long t0;

    public void init(GLAutoDrawable glad) {
        GL2ES2 gl = glad.getGL().getGL2ES2();
        myShader = GLSLSimpleProgram.create(gl, RedSquareShader.VERTEX_SHADER_TEXT, RedSquareShader.FRAGMENT_SHADER_TEXT, true);
        gl.glUseProgram(myShader.getShaderProgram());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        // setup gcu_PMVMatrix
        pmvMatrix = new PMVMatrix();
        pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        mgl_PMVMatrix = gl.glGetUniformLocation(myShader.getShaderProgram(), "gcu_PMVMatrix");
        Assert.assertTrue(0 <= mgl_PMVMatrix);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        pmvMatrixUniform = new GLUniformData("gcu_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        pmvMatrixUniform.setLocation(mgl_PMVMatrix);
        gl.glUniform(pmvMatrixUniform);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        // Allocate Vertex Array
        int components = 3;
        int numElements = 4;
        mgl_Vertex = gl.glGetAttribLocation(myShader.getShaderProgram(), "mgl_Vertex");
        Assert.assertTrue(0 <= mgl_Vertex);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        FloatBuffer buffer = Buffers.newDirectFloatBuffer(numElements * components);
        GLArrayDataWrapper vertices = GLArrayDataWrapper.createGLSL("mgl_Vertex", 3, gl.GL_FLOAT, false, 0, buffer, -1, 0, -1);
        {
            // Fill them up
            FloatBuffer verticeb = (FloatBuffer) vertices.getBuffer();
            verticeb.put(-2);
            verticeb.put(2);
            verticeb.put(0);
            verticeb.put(2);
            verticeb.put(2);
            verticeb.put(0);
            verticeb.put(-2);
            verticeb.put(-2);
            verticeb.put(0);
            verticeb.put(2);
            verticeb.put(-2);
            verticeb.put(0);
        }
        buffer.flip();
        vertices.setLocation(mgl_Vertex);
        gl.glEnableVertexAttribArray(mgl_Vertex);
        gl.glVertexAttribPointer(vertices);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        // Allocate Color Array
        components = 4;
        numElements = 4;
        mgl_Color = gl.glGetAttribLocation(myShader.getShaderProgram(), "mgl_Color");
        Assert.assertTrue(0 <= mgl_Color);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        buffer = Buffers.newDirectFloatBuffer(numElements * components);
        GLArrayDataWrapper colors = GLArrayDataWrapper.createGLSL("mgl_Color", 4, gl.GL_FLOAT, false, 0, buffer, -1, 0, -1);
        {
            // Fill them up
            FloatBuffer colorb = (FloatBuffer) colors.getBuffer();
            colorb.put(1);
            colorb.put(0);
            colorb.put(0);
            colorb.put(1);
            colorb.put(0);
            colorb.put(0);
            colorb.put(1);
            colorb.put(1);
            colorb.put(1);
            colorb.put(0);
            colorb.put(0);
            colorb.put(1);
            colorb.put(1);
            colorb.put(0);
            colorb.put(0);
            colorb.put(1);
        }
        buffer.flip();
        colors.setLocation(mgl_Color);
        gl.glEnableVertexAttribArray(mgl_Color);
        gl.glVertexAttribPointer(colors);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        // OpenGL Render Settings
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);
        gl.glUseProgram(0);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        t0 = System.currentTimeMillis();
    }

    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        GL2ES2 gl = glad.getGL().getGL2ES2();
        gl.glUseProgram(myShader.getShaderProgram());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        // Set location in front of camera
        pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0F, (float) width / (float) height, 1.0F, 100.0F);
        //pmvMatrix.glOrthof(-4.0f, 4.0f, -4.0f, 4.0f, 1.0f, 100.0f);
        gl.glUniform(pmvMatrixUniform);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        gl.glUseProgram(0);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
    }

    public void display(GLAutoDrawable glad) {
        long t1 = System.currentTimeMillis();

        GL2ES2 gl = glad.getGL().getGL2ES2();
        gl.glUseProgram(myShader.getShaderProgram());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        // One rotation every four seconds
        pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        float ang = ((float) (t1 - t0) * 360.0F) / 4000.0F;
        pmvMatrix.glRotatef(ang, 0, 0, 1);
        pmvMatrix.glRotatef(ang, 0, 1, 0);
        gl.glUniform(pmvMatrixUniform);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        // Draw a square
        gl.glDrawArrays(gl.GL_TRIANGLE_STRIP, 0, 4);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        gl.glUseProgram(0);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
    }

    public void dispose(GLAutoDrawable glad) {
        GL2ES2 gl = glad.getGL().getGL2ES2();
        gl.glDisableVertexAttribArray(mgl_Vertex);
        gl.glDisableVertexAttribArray(mgl_Color);
        myShader.release(gl);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        pmvMatrix.destroy();
        pmvMatrix = null;
        System.err.println("dispose done");
    }
}

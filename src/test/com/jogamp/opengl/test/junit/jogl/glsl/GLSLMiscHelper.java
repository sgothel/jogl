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
package com.jogamp.opengl.test.junit.jogl.glsl;

import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderState;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLBufferStorage;
import com.jogamp.opengl.GLDrawable;

import org.junit.Assert;

public class GLSLMiscHelper {
    public static final int frames_perftest = 600; // frames
    public static final int frames_warmup   = 100; // frames

    public static void validateGLArrayDataServerState(final GL2ES2 gl, final ShaderState st, final GLArrayDataServer data) {
        final int[] qi = new int[1];
        if(null != st) {
            Assert.assertEquals(data, st.getAttribute(data.getName()));
            if(st.shaderProgram().linked()) {
                Assert.assertEquals(data.getLocation(), st.getCachedAttribLocation(data.getName()));
                Assert.assertEquals(data.getLocation(), st.getAttribLocation(gl, data));
                Assert.assertEquals(data.getLocation(), st.getAttribLocation(gl, data.getName()));
                Assert.assertEquals(data.getLocation(), gl.glGetAttribLocation(st.shaderProgram().program(), data.getName()));
            }
        }
        gl.glGetVertexAttribiv(data.getLocation(), GL2ES2.GL_VERTEX_ATTRIB_ARRAY_ENABLED, qi, 0);
        Assert.assertEquals(data.enabled()?GL.GL_TRUE:GL.GL_FALSE, qi[0]);
        gl.glGetVertexAttribiv(data.getLocation(), GL2ES2.GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING, qi, 0);
        Assert.assertEquals(data.getVBOName(), qi[0]);
        final GLBufferStorage glStore = gl.getBufferStorage(data.getVBOName());
        Assert.assertEquals("GLBufferStorage size mismatch, storage "+glStore, data.getSizeInBytes(), null != glStore ? glStore.getSize() : -1);
    }

    public static void pause(final long ms) throws InterruptedException {
        final long t0 = System.currentTimeMillis();
        while( System.currentTimeMillis() - t0 < ms) {
            Thread.sleep(ms);
        }
    }

    public static void displayVCArrays(final GLDrawable drawable, final GL2ES2 gl, final ShaderState st, final boolean preEnable, final GLArrayDataServer vertices, final GLArrayDataServer colors, final boolean postDisable, final int num, final long postDelay) throws InterruptedException {
        System.err.println("screen #"+num);
        if(preEnable) {
            vertices.enableBuffer(gl, true);
            // invalid - Assert.assertEquals(vertices.getVBOName(), gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER));
            colors.enableBuffer(gl, true);
            // invalid - Assert.assertEquals(colors.getVBOName(), gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER));
            //
            // Above assertions are invalid, since GLSLArrayHandler will not bind the VBO to target
            // if the VBO is already bound to the attribute itself.
            // validateGLArrayDataServerState(..) does check proper VBO to attribute binding.
        }
        Assert.assertTrue(vertices.enabled());
        Assert.assertTrue(colors.enabled());

        validateGLArrayDataServerState(gl, st, vertices);
        validateGLArrayDataServerState(gl, st, colors);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        if(postDisable) {
            vertices.enableBuffer(gl, false);
            colors.enableBuffer(gl, false);
            Assert.assertTrue(!vertices.enabled());
            Assert.assertTrue(!colors.enabled());
        }
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        drawable.swapBuffers();
        if(postDelay>0) { pause(postDelay); }
    }

    public static void displayVCArraysNoChecks(final GLDrawable drawable, final GL2ES2 gl, final boolean preEnable, final GLArrayDataServer vertices, final GLArrayDataServer colors, final boolean postDisable) throws InterruptedException {
        if(preEnable) {
            vertices.enableBuffer(gl, true);
            colors.enableBuffer(gl, true);
        }
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        if(postDisable) {
            vertices.enableBuffer(gl, false);
            colors.enableBuffer(gl, false);
        }
        drawable.swapBuffers();
    }

    public static GLArrayDataServer createVertices(final GL2ES2 gl, final ShaderState st, final int shaderProgram, final int location, final float[] vertices) {
        if(null != st && 0 != shaderProgram) {
            throw new InternalError("Use either ShaderState _or_ shader-program, not both");
        }
        if(null == st && 0 == shaderProgram) {
            throw new InternalError("Pass a valid ShaderState _xor_ shader-program, not none");
        }
        // Allocate Vertex Array0
        final GLArrayDataServer vDataArray = GLArrayDataServer.createGLSL("mgl_Vertex", 3, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        if(null != st) {
            st.ownAttribute(vDataArray, true);
            if(0<=location) {
                st.bindAttribLocation(gl, location, vDataArray);
            }
        } else {
            if(0<=location) {
                vDataArray.setLocation(gl, shaderProgram, location);
            } else {
                vDataArray.setLocation(gl, shaderProgram);
            }
        }
        Assert.assertTrue(vDataArray.isVBO());
        Assert.assertTrue(vDataArray.isVertexAttribute());
        Assert.assertTrue(!vDataArray.isVBOWritten());
        Assert.assertTrue(!vDataArray.sealed());
        int i=0;
        vDataArray.putf(vertices[i++]); vDataArray.putf(vertices[i++]);  vDataArray.putf(vertices[i++]);
        vDataArray.putf(vertices[i++]); vDataArray.putf(vertices[i++]);  vDataArray.putf(vertices[i++]);
        vDataArray.putf(vertices[i++]); vDataArray.putf(vertices[i++]);  vDataArray.putf(vertices[i++]);
        vDataArray.putf(vertices[i++]); vDataArray.putf(vertices[i++]);  vDataArray.putf(vertices[i++]);
        vDataArray.seal(gl, true);
        Assert.assertTrue(vDataArray.isVBOWritten());
        Assert.assertTrue(vDataArray.sealed());
        Assert.assertEquals(4, vDataArray.getElementCount());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        Assert.assertEquals(0, gl.getBoundBuffer(GL.GL_ARRAY_BUFFER)); // should be cleared ASAP
        validateGLArrayDataServerState(gl, st, vDataArray);
        return vDataArray;
    }
    public static final float[] vertices0 = new float[] { -2f,  2f, 0f,
                                                           2f,  2f, 0f,
                                                          -2f, -2f, 0f,
                                                           2f, -2f, 0f };

    public static final float[] vertices1 = new float[] { -2f,  1f, 0f,
                                                           2f,  1f, 0f,
                                                          -2f, -1f, 0f,
                                                           2f, -1f, 0f };

    public static GLArrayDataServer createColors(final GL2ES2 gl, final ShaderState st, final int shaderProgram, final int location, final float[] colors) {
        if(null != st && 0 != shaderProgram) {
            throw new InternalError("Use either ShaderState _or_ shader-program, not both");
        }
        if(null == st && 0 == shaderProgram) {
            throw new InternalError("Pass a valid ShaderState _xor_ shader-program, not none");
        }
        final GLArrayDataServer cDataArray = GLArrayDataServer.createGLSL("mgl_Color", 4, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        if(null != st) {
            st.ownAttribute(cDataArray, true);
            if(0<=location) {
                st.bindAttribLocation(gl, location, cDataArray);
            }
        } else {
            if(0<=location) {
                cDataArray.setLocation(gl, shaderProgram, location);
            } else {
                cDataArray.setLocation(gl, shaderProgram);
            }
        }
        int i=0;
        cDataArray.putf(colors[i++]); cDataArray.putf(colors[i++]); cDataArray.putf(colors[i++]); cDataArray.putf(colors[i++]);
        cDataArray.putf(colors[i++]); cDataArray.putf(colors[i++]); cDataArray.putf(colors[i++]); cDataArray.putf(colors[i++]);
        cDataArray.putf(colors[i++]); cDataArray.putf(colors[i++]); cDataArray.putf(colors[i++]); cDataArray.putf(colors[i++]);
        cDataArray.putf(colors[i++]); cDataArray.putf(colors[i++]); cDataArray.putf(colors[i++]); cDataArray.putf(colors[i++]);
        cDataArray.seal(gl, true);
        Assert.assertTrue(cDataArray.isVBO());
        Assert.assertTrue(cDataArray.isVertexAttribute());
        Assert.assertTrue(cDataArray.isVBOWritten());
        Assert.assertTrue(cDataArray.sealed());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        Assert.assertEquals(0, gl.getBoundBuffer(GL.GL_ARRAY_BUFFER)); // should be cleared ASAP
        validateGLArrayDataServerState(gl, st, cDataArray);
        return cDataArray;
    }
    public static final float[] colors0 = new float[] { 1f, 0f, 0f, 1f,
                                                        0f, 0f, 1f, 1f,
                                                        1f, 0f, 0f, 1f,
                                                        1f, 0f, 1f, 1f };

    public static final float[] colors1 = new float[] { 1f, 0f, 1f, 1f,
                                                        0f, 1f, 0f, 1f,
                                                        1f, 0f, 1f, 1f,
                                                        1f, 0f, 1f, 1f };

}

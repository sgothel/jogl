/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.jogl.demos.gl4;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;

import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

/**
 * JOGL Tessellation ShaderCode GL4 test case.
 * <p>
 * Demonstrates tessellation-control and -evaluation shaders.
 * </p>
 *
 * @author Raymond L. Rivera, 2014
 * @author Sven Gothel
 */
public class TessellationShader01bGL4 implements GLEventListener  {
    private static final double ANIMATION_RATE = 950.0;

    private ShaderProgram program;
    private final int[] vertexArray = new int[1];
    private FloatBuffer vertexOffset;
    private FloatBuffer backgroundColor;


    @Override
    public void init(final GLAutoDrawable auto) {
        final GL4 gl = auto.getGL().getGL4();
        program = createProgram(auto);
        if( null == program ) {
            return;
        }

        final double theta = System.currentTimeMillis() / ANIMATION_RATE;
        vertexOffset = FloatBuffer.allocate(4);
        vertexOffset.put(0, (float)(Math.sin(theta) * 0.5f));
        vertexOffset.put(1, (float)(Math.cos(theta) * 0.6f));
        vertexOffset.put(2, 0.0f);
        vertexOffset.put(3, 0.0f);

        backgroundColor = FloatBuffer.allocate(4);
        backgroundColor.put(0, 0.25f);
        backgroundColor.put(1, 0.25f);
        backgroundColor.put(2, 0.25f);
        backgroundColor.put(3, 1.0f);

        gl.glGenVertexArrays(vertexArray.length, vertexArray, 0);
        gl.glBindVertexArray(vertexArray[0]);
        gl.glPatchParameteri(GL4.GL_PATCH_VERTICES, 3);
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
    }

    @Override
    public void display(final GLAutoDrawable auto) {
        if( null == program ) {
            return;
        }
        final GL4 gl = auto.getGL().getGL4();
        final double value = System.currentTimeMillis() / ANIMATION_RATE;
        gl.glClearBufferfv(GL2ES3.GL_COLOR, 0, backgroundColor);
        gl.glUseProgram(program.program());
        vertexOffset.put(0, (float)(Math.sin(value) * 0.5f));
        vertexOffset.put(1, (float)(Math.cos(value) * 0.6f));
        gl.glVertexAttrib4fv(0, vertexOffset);
        gl.glDrawArrays(GL4.GL_PATCHES, 0, 3);
    }

    @Override
    public void dispose(final GLAutoDrawable auto) {
        if( null == program ) {
            return;
        }
        final GL4 gl = auto.getGL().getGL4();
        gl.glDeleteVertexArrays(vertexArray.length, vertexArray, 0);
        program.destroy(gl);
    }

    @Override
    public void reshape(final GLAutoDrawable auto, final int x, final int y, final int width, final int height) {
        // final GL4 gl = auto.getGL().getGL4();
    }

    static final String shaderBasename = "tess_example01";

    private ShaderProgram createProgram(final GLAutoDrawable auto) {
        final GL4 gl = auto.getGL().getGL4();

        final ShaderProgram sp;
        {
            final ShaderCode vs, tcs, tes, fs;
            vs = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(),
                                   "shader", "shader/bin", shaderBasename, true);
            tcs = ShaderCode.create(gl, GL4.GL_TESS_CONTROL_SHADER, this.getClass(),
                                   "shader", "shader/bin", shaderBasename, true);
            tes = ShaderCode.create(gl, GL4.GL_TESS_EVALUATION_SHADER, this.getClass(),
                                   "shader", "shader/bin", shaderBasename, true);
            fs = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(),
                                   "shader", "shader/bin", shaderBasename, true);
            vs.defaultShaderCustomization(gl, true, true);
            tcs.defaultShaderCustomization(gl, true, true);
            tes.defaultShaderCustomization(gl, true, true);
            fs.defaultShaderCustomization(gl, true, true);

            sp = new ShaderProgram();
            sp.add(gl, vs, System.err);
            sp.add(gl, tcs, System.err);
            sp.add(gl, tes, System.err);
            sp.add(gl, fs, System.err);
        }
        if( !sp.link(gl, System.err) ) {
            System.err.println("[error] Couldn't link program: "+sp);
            sp.destroy(gl);
            return null;
        } else {
            return sp;
        }
    }
}

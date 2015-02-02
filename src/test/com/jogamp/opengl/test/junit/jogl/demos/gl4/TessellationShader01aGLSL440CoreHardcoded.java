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
public class TessellationShader01aGLSL440CoreHardcoded implements GLEventListener  {
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

    private ShaderProgram createProgram(final GLAutoDrawable auto) {
        final GL4 gl = auto.getGL().getGL4();
        final String vertexSource =
            "#version 440 core                                          \n" +
            "                                                           \n" +
            "layout (location = 0) in vec4 offset;                      \n" +
            "                                                           \n" +
            "void main(void)                                            \n" +
            "{                                                          \n" +
            "   const vec4 vertices[3] = vec4[3] (                      \n" +
            "                           vec4( 0.25,  0.25, 0.5, 1.0),   \n" +
            "                           vec4(-0.25, -0.25, 0.5, 1.0),   \n" +
            "                           vec4( 0.25, -0.25, 0.5, 1.0));  \n" +
            "   gl_Position = vertices[gl_VertexID] + offset;           \n" +
            "}                                                          \n";
        final String tessCtrlSource   =
            "#version 440 core                                          \n" +
            "layout (vertices = 3) out;                                 \n" +
            "                                                           \n" +
            "void main(void)                                            \n" +
            "{                                                          \n" +
            "   if (gl_InvocationID == 0)                               \n" +
            "   {                                                       \n" +
            "       gl_TessLevelInner[0] = 5.0;                         \n" +
            "       gl_TessLevelOuter[0] = 5.0;                         \n" +
            "       gl_TessLevelOuter[1] = 5.0;                         \n" +
            "       gl_TessLevelOuter[2] = 5.0;                         \n" +
            "   }                                                       \n" +
            "   gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;\n" +
            "}                                                          \n";
        final String tessEvalSource   =
            "#version 440 core                                          \n" +
            "                                                           \n" +
            "layout (triangles, equal_spacing, cw) in;                  \n" +
            "                                                           \n" +
            "void main(void)                                            \n" +
            "{                                                          \n" +
            "   gl_Position = (gl_TessCoord.x * gl_in[0].gl_Position) + \n" +
            "               (gl_TessCoord.y * gl_in[1].gl_Position)   + \n" +
            "               (gl_TessCoord.z * gl_in[2].gl_Position);    \n" +
            "}                                                          \n";
        final String fragmentSource   =
            "#version 440 core                                          \n" +
            "                                                           \n" +
            "out vec4 color;                                            \n" +
            "                                                           \n" +
            "void main(void)                                            \n" +
            "{                                                          \n" +
            "   color = vec4(1.0, 1.0, 1.0, 1.0);                       \n" +
            "}                                                          \n";

        final ShaderCode vertexShader     = createShader(gl, GL2ES2.GL_VERTEX_SHADER, vertexSource);
        if( null == vertexShader ) {
            return null;
        }
        final ShaderCode tessCtrlShader   = createShader(gl, GL4.GL_TESS_CONTROL_SHADER, tessCtrlSource);
        if( null == tessCtrlShader ) {
            vertexShader.destroy(gl);
            return null;
        }
        final ShaderCode tessEvalShader   = createShader(gl, GL4.GL_TESS_EVALUATION_SHADER, tessEvalSource);
        if( null == tessEvalShader ) {
            vertexShader.destroy(gl);
            tessCtrlShader.destroy(gl);
            return null;
        }
        final ShaderCode fragmentShader   = createShader(gl, GL2ES2.GL_FRAGMENT_SHADER, fragmentSource);
        if( null == fragmentShader ) {
            vertexShader.destroy(gl);
            tessCtrlShader.destroy(gl);
            tessEvalShader.destroy(gl);
            return null;
        }

        final ShaderProgram program       = new ShaderProgram();

        program.init(gl);
        program.add(vertexShader);
        program.add(tessCtrlShader);
        program.add(tessEvalShader);
        program.add(fragmentShader);

        program.link(gl, System.err);
        if( !program.validateProgram(gl, System.out) ) {
            System.err.println("[error] Program linking failed.");
            program.destroy(gl);
            return null;
        } else {
            return program;
        }
    }

    private ShaderCode createShader(final GL4 gl, final int shaderType, final String source) {
        final String[][] sources = new String[1][1];
        sources[0] = new String[]{ source };
        final ShaderCode shader = new ShaderCode(shaderType, sources.length, sources);

        final boolean compiled = shader.compile(gl, System.err);
        if (!compiled) {
            System.err.println("[error] Shader compilation failed.");
            shader.destroy(gl);
            return null;
        } else {
            return shader;
        }
    }

}

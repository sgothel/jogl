/*
 * Copyright 2012 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.util.awt;

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.util.glsl.ShaderUtil;


/**
 * Utility to load shaders from files, URLs, and strings.
 *
 * <p><i>ShaderLoader</i> is a simple utility for loading shaders.  It takes
 * shaders directly as strings.  It will create and compile the shaders, and
 * link them together into a program.  Both compiling and linking are verified.
 * If a problem occurs a {@link GLException} is thrown with the appropriate log
 * attached.
 *
 * <p>In general most developers should be able to use one of the
 * <i>loadProgram</i> methods, which simply return the shader program for a pair
 * of vertex and fragment shader sources.  However, if the developer needs the
 * shader handles too it may be worth it to use the <i>loadShader</i> and
 * <i>loadProgram</i> methods separately.
 *
 * <p>Note it is highly recommended that if the developer passes the strings
 * directly to <i>ShaderLoader</i> that they contain newlines.  That way if any
 * errors do occur their line numbers will be reported correctly.  This means
 * that if the shader is to be embedded in Java code, a <i>\n</i> should be
 * appended to every line.
 */
class ShaderLoader {

    /**
     * Loads a shader program from a pair of strings.
     *
     * @param gl Current OpenGL context
     * @param vss Vertex shader source
     * @param fss Fragment shader source
     * @return OpenGL handle to the shader program
     * @throws AssertionError if context is <tt>null</tt>
     * @throws AssertionError if vertex shader or fragment shader is <tt>null</tt> or empty
     * @throws GLException if program did not compile, link, or validate successfully
     */
    static int loadProgram(final GL2ES2 gl, final String vss, final String fss) {

        assert (gl != null);
        assert (vss != null);
        assert (!vss.isEmpty());
        assert (fss != null);
        assert (!fss.isEmpty());

        // Create the shaders
        final int vs = loadShader(gl, vss, GL2ES2.GL_VERTEX_SHADER);
        final int fs = loadShader(gl, fss, GL2ES2.GL_FRAGMENT_SHADER);

        // Create a program and attach the shaders
        final int program = gl.glCreateProgram();
        gl.glAttachShader(program, vs);
        gl.glAttachShader(program, fs);

        // Link and validate the program
        gl.glLinkProgram(program);
        gl.glValidateProgram(program);
        if ((!isProgramLinked(gl, program)) || (!isProgramValidated(gl, program))) {
            final String log = ShaderUtil.getProgramInfoLog(gl, program);
            throw new GLException(log);
        }

        // Clean up the shaders
        gl.glDeleteShader(vs);
        gl.glDeleteShader(fs);

        // Return the program
        return program;
    }

    //------------------------------------------------------------------
    // Helpers
    //

    /**
     * Checks that a shader was compiled correctly.
     *
     * @param gl OpenGL context that supports programmable shaders
     * @param shader OpenGL handle to a shader
     * @return True if <tt>shader</tt> was compiled without errors
     */
    private static boolean isCompiled(final GL2ES2 gl, final int shader) {
        return ShaderUtil.isShaderStatusValid(gl, shader, GL2ES2.GL_COMPILE_STATUS, null);
    }

    /**
     * Checks that a shader program was linked successfully.
     *
     * @param gl OpenGL context that supports programmable shaders
     * @param program OpenGL handle to a shader program
     * @return True if <tt>program</tt> was linked successfully
     */
    private static boolean isProgramLinked(final GL2ES2 gl, final int program) {
        return ShaderUtil.isProgramStatusValid(gl, program, GL2ES2.GL_LINK_STATUS);
    }

    /**
     * Checks that a shader program was validated successfully.
     *
     * @param gl OpenGL context that supports programmable shaders
     * @param program OpenGL handle to a shader program
     * @return True if <tt>program</tt> was validated successfully
     */
    private static boolean isProgramValidated(final GL2ES2 gl, final int program) {
        return ShaderUtil.isProgramStatusValid(gl, program, GL2ES2.GL_VALIDATE_STATUS);
    }

    /**
     * Determines if a shader type is valid.
     *
     * <p>Valid shader types are:
     * <ul>
     *   <li><i>GL_VERTEX_SHADER</i>
     *   <li><i>GL_FRAGMENT_SHADER</i>
     * </ul>
     *
     * @param type Type of a shader
     * @return True if <tt>type</tt> is a valid shader type
     */
    private static boolean isValidType(final int type) {
        switch (type) {
        case GL2ES2.GL_VERTEX_SHADER:
        case GL2ES2.GL_FRAGMENT_SHADER:
            return true;
        default:
            return false;
        }
    }

    /**
     * Loads the source of a shader from a string.
     *
     * @param gl Current OpenGL context
     * @param source Source code of the shader as one long string
     * @param type Either <i>GL_FRAGMENT_SHADER</i> or <i>GL_VERTEX_SHADER</i>
     * @return OpenGL handle to the shader
     * @throws AssertionError if context is <tt>null</tt>
     * @throws AssertionError if source is empty
     * @throws AssertionError if type is invalid
     * @throws GLException if a GLSL-capable context is not active
     * @throws GLException if could not compile shader
     */
    private static int loadShader(final GL2ES2 gl, final String source, final int type) {

        assert (gl != null);
        assert (!source.isEmpty());
        assert (isValidType(type));

        // Create and read source
        final int shader = gl.glCreateShader(type);
        gl.glShaderSource(
                shader,                    // shader handle
                1,                         // number of strings
                new String[] {source},     // array of strings
                null);                     // lengths of strings

        // Compile
        gl.glCompileShader(shader);
        if (!isCompiled(gl, shader)) {
            final String log = ShaderUtil.getShaderInfoLog(gl, shader);
            throw new GLException(log);
        }

        // Return the shader
        return shader;
    }
}

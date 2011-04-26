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

package com.jogamp.opengl.test.junit.jogl.demos.es2.shader;

public class RedSquareShader {
    public static String VERTEX_SHADER_TEXT =
                " #ifdef GL_ES\n" +
                "  #define MEDIUMP mediump\n" +
                "  #define HIGHP highp\n" +
                "#else\n" +
                "  #define MEDIUMP\n" +
                "  #define HIGHP\n" +
                "#endif\n" +
                "\n" +
                "uniform MEDIUMP mat4    gcu_PMVMatrix[2];\n" +
                "attribute HIGHP vec4    mgl_Vertex;\n" +
                "attribute HIGHP vec4    mgl_Color;\n" +
                "varying   HIGHP vec4    frontColor;\n" +
                "\n" +
                "void main(void)\n" +
                "{\n" +
                "  frontColor=mgl_Color;\n" +
                "  gl_Position = gcu_PMVMatrix[0] * gcu_PMVMatrix[1] * mgl_Vertex;\n" +
                "}\n" ;

    public static String FRAGMENT_SHADER_TEXT =
                "#ifdef GL_ES\n" +
                "  #define MEDIUMP mediump\n" +
                "  #define HIGHP highp\n" +
                "#else\n" +
                "  #define MEDIUMP\n" +
                "  #define HIGHP\n" +
                "#endif\n" +
                "\n" +
                "varying   HIGHP vec4    frontColor;\n" +
                "\n" +
                "void main (void)\n" +
                "{\n" +
                "    gl_FragColor = frontColor;\n" +
                "}\n" ;

}

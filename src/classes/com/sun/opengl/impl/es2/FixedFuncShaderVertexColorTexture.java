
package com.sun.opengl.impl.es2;

import javax.media.opengl.util.*;
import javax.media.opengl.*;
import java.nio.*;

public class FixedFuncShaderVertexColorTexture extends ShaderData {

    public static final String[][] vertShaderSource = new String[][] { {
        "#ifdef GL_ES\n"+
        "  #define MEDIUMP mediump\n"+
        "  #define HIGHP highp\n"+
        "#else\n"+
        "  #define MEDIUMP\n"+
        "  #define HIGHP\n"+
        "#endif\n"+
        "\n"+
        "uniform MEDIUMP mat4    mgl_PMVMatrix[2];\n"+
        "attribute HIGHP vec4    mgl_Vertex;\n"+
        "attribute HIGHP vec4    mgl_Color;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord0;\n"+
        "varying   HIGHP vec4    gl_TexCoord[7];\n"+
        "varying   HIGHP vec4    frontColor;\n"+
        "void main(void)\n"+
        "{\n"+
        "  frontColor=mgl_Color;\n"+
        "  gl_TexCoord[0] = mgl_MultiTexCoord0;\n"+
        "  gl_Position = mgl_PMVMatrix[0] * mgl_PMVMatrix[1] * mgl_Vertex;\n"+
        "}\n" } } ;

    public static final String[][] fragShaderSource = new String[][] { {
        "#ifdef GL_ES\n"+
        "  #define MEDIUMP mediump\n"+
        "  #define HIGHP highp\n"+
        "#else\n"+
        "  #define MEDIUMP\n"+
        "  #define HIGHP\n"+
        "#endif\n"+
        "\n"+
        "uniform   HIGHP sampler2D mgl_ActiveTexture;\n"+
        "varying   HIGHP vec4    frontColor;\n"+
        "const     HIGHP vec4    one  = vec4(1.0, 1.0, 1.0, 1.0);\n"+
        "const     HIGHP vec4    zero = vec4(0.0, 0.0, 0.0, 0.0);\n"+
        "void main (void)\n"+
        "{\n"+
        "    vec4 texColor = texture2D(mgl_ActiveTexture,gl_TexCoord[0].st);\n"+
        "    if(greaterThan(texColor, zero)) {\n"+
        "       gl_FragColor = vec4(frontColor.rgb*texColor.rgb, frontColor.a*texColor.a);\n"+
        "    } else {\n"+
        "       gl_FragColor = frontColor;\n"+
        "    }\n"+
        "}\n" } } ;

    public FixedFuncShaderVertexColorTexture() {
        super(1, 1);
    }

    public int        vertexShaderBinaryFormat() { return 0; }
    public Buffer     vertexShaderBinary() { return null; }
    public String[][] vertexShaderSource() { return vertShaderSource; }

    public int        fragmentShaderBinaryFormat() { return 0; }
    public Buffer     fragmentShaderBinary() { return null; }
    public String[][] fragmentShaderSource() { return fragShaderSource; }
}


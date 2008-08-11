
package com.sun.opengl.impl.glsl;

import javax.media.opengl.util.*;
import javax.media.opengl.*;
import java.nio.*;

public class FixedFuncShaderVertexColor {

    public static final String[][] vertShaderSource = new String[][] { {
        "#ifdef GL_ES\n"+
        "  #define MEDIUMP mediump\n"+
        "  #define HIGHP highp\n"+
        "#else\n"+
        "  #define MEDIUMP\n"+
        "  #define HIGHP\n"+
        "#endif\n"+
        "\n"+
        "const   MEDIUMP int     MAX_TEXTURE_UNITS = 8; // <=gl_MaxTextureImageUnits \n"+
        "const   MEDIUMP int     MAX_LIGHTS = 8; \n"+
        "\n"+
        "uniform   HIGHP mat4    mgl_PMVMatrix[3]; // P, Mv, and Mvi\n"+
        "uniform MEDIUMP int     mgl_ColorEnabled;\n"+
        "uniform   HIGHP vec4    mgl_ColorStatic;\n"+
        "uniform MEDIUMP int     mgl_TexCoordEnabled;\n"+
        "\n"+
        "attribute HIGHP vec4    mgl_Vertex;\n"+
        "attribute HIGHP vec4    mgl_Color;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord0;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord1;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord2;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord3;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord4;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord5;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord6;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord7;\n"+
        "\n"+
        "varying   HIGHP vec4    frontColor;\n"+
        "varying   HIGHP vec4    mgl_TexCoord[MAX_TEXTURE_UNITS];\n"+
        "\n"+
        "void setTexCoord(in HIGHP vec4 defpos) {\n"+
        "  mgl_TexCoord[0] = ( 0 != (mgl_TexCoordEnabled &   1) ) ? mgl_MultiTexCoord0 : defpos;\n"+
        "  mgl_TexCoord[1] = ( 0 != (mgl_TexCoordEnabled &   2) ) ? mgl_MultiTexCoord1 : defpos;\n"+
        "  mgl_TexCoord[2] = ( 0 != (mgl_TexCoordEnabled &   4) ) ? mgl_MultiTexCoord2 : defpos;\n"+
        "  mgl_TexCoord[3] = ( 0 != (mgl_TexCoordEnabled &   8) ) ? mgl_MultiTexCoord3 : defpos;\n"+
        "  mgl_TexCoord[4] = ( 0 != (mgl_TexCoordEnabled &  16) ) ? mgl_MultiTexCoord4 : defpos;\n"+
        "  mgl_TexCoord[5] = ( 0 != (mgl_TexCoordEnabled &  32) ) ? mgl_MultiTexCoord5 : defpos;\n"+
        "  mgl_TexCoord[6] = ( 0 != (mgl_TexCoordEnabled &  64) ) ? mgl_MultiTexCoord6 : defpos;\n"+
        "  mgl_TexCoord[7] = ( 0 != (mgl_TexCoordEnabled & 128) ) ? mgl_MultiTexCoord7 : defpos;\n"+
        "}\n"+
        "\n"+
        "void main(void)\n"+
        "{\n"+
        "  if(mgl_ColorEnabled>0) {\n"+
        "    frontColor=mgl_Color;\n"+
        "  } else {\n"+
        "    frontColor=mgl_ColorStatic;\n"+
        "  }\n"+
        "\n"+
        "  gl_Position = mgl_PMVMatrix[0] * mgl_PMVMatrix[1] * mgl_Vertex;\n"+
        "\n"+
        "  setTexCoord(gl_Position);\n"+
        "}\n" } } ;
}


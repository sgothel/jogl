
package com.sun.opengl.impl.es2;

import javax.media.opengl.util.*;
import javax.media.opengl.*;
import java.nio.*;

public class FixedFuncShaderFragmentColorTexture {

    public static final String[][] fragShaderSource = new String[][] { {
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
        "const   HIGHP   vec4    zero = vec4(0.0, 0.0, 0.0, 0.0);\n"+
        "\n"+
        "uniform   HIGHP sampler2D mgl_ActiveTexture;\n"+
        "uniform MEDIUMP int       mgl_ActiveTextureIdx;\n"+
        "\n"+
        "varying   HIGHP vec4    frontColor;\n"+
        "varying   HIGHP vec4    mgl_TexCoord[MAX_TEXTURE_UNITS];\n"+
        "\n"+
        "void main (void)\n"+
        "{\n"+
        "    vec4 texColor = texture2D(mgl_ActiveTexture,mgl_TexCoord[mgl_ActiveTextureIdx].st);\n"+
        "    if(greaterThan(texColor, zero)) {\n"+
        "       gl_FragColor = vec4(frontColor.rgb*texColor.rgb, frontColor.a * texColor.a) ; \n"+
        "    } else {\n"+
        "       gl_FragColor = frontColor;\n"+
        "    }\n"+
        "}\n" } } ;
}


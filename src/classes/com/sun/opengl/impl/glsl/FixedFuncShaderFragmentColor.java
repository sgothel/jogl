
package com.sun.opengl.impl.glsl;

import javax.media.opengl.util.*;
import javax.media.opengl.*;
import java.nio.*;

public class FixedFuncShaderFragmentColor {

    public static final String[][] fragShaderSource = new String[][] { {
        "#ifdef GL_ES\n"+
        "  #define MEDIUMP mediump\n"+
        "  #define HIGHP highp\n"+
        "#else\n"+
        "  #define MEDIUMP\n"+
        "  #define HIGHP\n"+
        "#endif\n"+
        "\n"+
        "varying   HIGHP vec4    frontColor;\n"+
        "\n"+
        "void main (void)\n"+
        "{\n"+
        "  gl_FragColor = frontColor;\n"+
        "}\n" } } ;

}


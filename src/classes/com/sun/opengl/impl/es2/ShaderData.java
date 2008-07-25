
package com.sun.opengl.impl.es2;

import javax.media.opengl.util.*;
import javax.media.opengl.*;
import java.nio.*;

public abstract class ShaderData {
    public abstract int        vertexShaderBinaryFormat();
    public abstract Buffer     vertexShaderBinary();
    public abstract String[][] vertexShaderSource();

    public abstract int        fragmentShaderBinaryFormat();
    public abstract Buffer     fragmentShaderBinary();
    public abstract String[][] fragmentShaderSource();

    public boolean    isValid() { return valid; }
    public void       setValid(boolean val) { valid=val; }

    public ShaderData(int numVertexShader, int numFragmentShader) {
        vertShader = BufferUtil.newIntBuffer(numVertexShader);
        fragShader = BufferUtil.newIntBuffer(numFragmentShader);
    }

    public IntBuffer  vertexShader() { return vertShader; }
    public IntBuffer  fragmentShader() { return fragShader; }

    public boolean createAndCompile(GL2ES2 gl) {
        if(isValid()) return true;
        boolean res;

        // Create & Compile the vertex/fragment shader objects
        res=gl.glCreateCompileShader(vertexShader(), gl.GL_VERTEX_SHADER,
                                     vertexShaderBinaryFormat(), vertexShaderBinary(),
                                     vertexShaderSource(), System.err);
        if(!res) return false;

        res=gl.glCreateCompileShader(fragmentShader(), gl.GL_FRAGMENT_SHADER,
                                     fragmentShaderBinaryFormat(), fragmentShaderBinary(),
                                     fragmentShaderSource(), System.err);
        if(!res) return false;

        setValid(true);
        return true;
    }

    public void release(GL2ES2 gl) {
        if(isValid()) {
            gl.glDeleteShader(fragmentShader());
            gl.glDeleteShader(vertexShader());
            setValid(false);
        }
    }

    protected IntBuffer vertShader = null;
    protected IntBuffer fragShader = null;

    protected boolean valid=false;
}


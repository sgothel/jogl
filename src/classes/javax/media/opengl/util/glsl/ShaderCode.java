
package javax.media.opengl.util.glsl;

import javax.media.opengl.util.*;
import javax.media.opengl.*;

import java.nio.*;
import java.io.PrintStream;

public class ShaderCode {
    public ShaderCode(int type, int number, 
                      int binFormat, Buffer binary, String[][] source) {
        switch (type) {
            case GL2ES2.GL_VERTEX_SHADER:
            case GL2ES2.GL_FRAGMENT_SHADER:
                break;
            default:
                throw new GLException("Unknown shader type: "+type);
        }
        shaderSource = source;
        shaderBinaryFormat = binFormat;
        shaderBinary = binary;
        shaderType   = type;
        shader = BufferUtil.newIntBuffer(number);
        id = getNextID();
    }

    /**
     * returns the uniq shader id as an integer
     * @see #key()
     */
    public int        id() { return id.intValue(); }

    /**
     * returns the uniq shader id as an Integer
     *
     * @see #id()
     */
    public Integer    key() { return id; }

    public int        shaderType() { return shaderType; }
    public String     shaderTypeStr() { return shaderTypeStr(shaderType); }

    public static String shaderTypeStr(int type) { 
        switch (type) {
            case GL2ES2.GL_VERTEX_SHADER:
                return "VERTEX_SHADER";
            case GL2ES2.GL_FRAGMENT_SHADER:
                return "FRAGMENT_SHADER";
        }
        return "UNKNOWN_SHADER";
    }

    public int        shaderBinaryFormat() { return shaderBinaryFormat; }
    public Buffer     shaderBinary() { return shaderBinary; }
    public String[][] shaderSource() { return shaderSource; }

    public boolean    isValid() { return valid; }

    public IntBuffer  shader() { return shader; }

    public boolean compile(GL2ES2 gl) {
        return compile(gl, null);
    }
    public boolean compile(GL2ES2 gl, PrintStream verboseOut) {
        if(isValid()) return true;

        // Create & Compile the vertex/fragment shader objects
        valid=gl.glCreateCompileShader(shader, shaderType,
                                       shaderBinaryFormat, shaderBinary,
                                       shaderSource, verboseOut);
        shader.clear();
        return valid;
    }

    public void release(GL2ES2 gl) {
        if(isValid()) {
            gl.glDeleteShader(shader());
            valid=false;
        }
    }

    public boolean equals(Object obj) {
        if(this==obj) return true;
        if(obj instanceof ShaderCode) {
            return id()==((ShaderCode)obj).id();
        }
        return false;
    }
    public int hashCode() {
        return id.intValue();
    }
    public String toString() {
        StringBuffer buf = new StringBuffer("ShaderCode [id="+id+", type="+shaderTypeStr()+", valid="+valid);
        /*
        if(shaderSource!=null) {
            for(int i=0; i<shaderSource.length; i++) {
                for(int j=0; j<shaderSource[i].length; j++) {
                    buf.append("\n\t, ShaderSource["+i+"]["+j+"]:\n");
                    buf.append(shaderSource[i][j]);
                }
            }
        } */
        buf.append("]");
        return buf.toString();
    }

    protected String[][] shaderSource = null;
    protected Buffer     shaderBinary = null;
    protected int        shaderBinaryFormat = -1;
    protected IntBuffer  shader = null;
    protected int        shaderType = -1;
    protected Integer    id = null;

    protected boolean valid=false;

    private static synchronized Integer getNextID() {
        return new Integer(nextID++);
    }
    protected static int nextID = 1;
}


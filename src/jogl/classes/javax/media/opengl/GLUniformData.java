
package javax.media.opengl;

import java.nio.*;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.math.FloatUtil;

public class GLUniformData {

    /**
     * int atom
     *
     * Number of objects is 1
     *
     */
    public GLUniformData(String name, int val) {
        init(name, 1, new Integer(val));
    }

    /**
     * float atom
     *
     * Number of objects is 1
     *
     */
    public GLUniformData(String name, float val) {
        init(name, 1, new Float(val));
    }

    /**
     * Multiple IntBuffer Vector
     *
     * Number of objects is calculated by data.limit()/components
     *
     * @param components number of elements of one object, ie 4 for GL_FLOAT_VEC4,
     */
    public GLUniformData(String name, int components, IntBuffer data) {
        init(name, components, data);
    }

    /**
     * Multiple FloatBuffer Vector
     *
     * Number of objects is calculated by data.limit()/components
     *
     * @param components number of elements of one object, ie 4 for GL_FLOAT_VEC4,
     */
    public GLUniformData(String name, int components, FloatBuffer data) {
        init(name, components, data);
    }

    /**
     * Multiple FloatBuffer Matrix
     *
     * Number of objects is calculated by data.limit()/(rows*columns)
     *
     * @param rows the matrix rows
     * @param column the matrix column
     */
    public GLUniformData(String name, int rows, int columns, FloatBuffer data) {
        init(name, rows, columns, data);
    }

    public GLUniformData setData(int data) { init(new Integer(data)); return this; }
    public GLUniformData setData(float data) { init(new Float(data)); return this; }
    public GLUniformData setData(IntBuffer data) { init(data); return this; }
    public GLUniformData setData(FloatBuffer data) { init(data); return this; }

    public int       intValue()   { return ((Integer)data).intValue(); };
    public float     floatValue() { return ((Float)data).floatValue(); };
    public IntBuffer intBufferValue()   { return (IntBuffer)data; };
    public FloatBuffer floatBufferValue() { return (FloatBuffer)data; };

    public StringBuilder toString(StringBuilder sb) {
      if(null == sb) {
          sb = new StringBuilder();
      }
      sb.append("GLUniformData[name ").append(name).
                       append(", location ").append(location).
                       append(", size ").append(rows).append("x").append(columns).
                       append(", count ").append(count).
                       append(", data ");
      if(isMatrix() && data instanceof FloatBuffer) {
          sb.append("\n");
          final FloatBuffer fb = (FloatBuffer)getBuffer();
          for(int i=0; i<count; i++) {
              FloatUtil.matrixToString(sb, i+": ", "%10.5f", fb, i*rows*columns, rows, columns, false);
              sb.append(",\n");
          }
      } else if(isBuffer()) {
          Buffers.toString(sb, null, getBuffer());
      } else {
          sb.append(data);
      }
      sb.append("]");
      return sb;
    }

    @Override
    public String toString() {
        return toString(null).toString();
    }

    private void init(String name, int rows, int columns, Object data) {
        if( 2>rows || rows>4 || 2>columns || columns>4 ) {
            throw new GLException("rowsXcolumns must be within [2..4]X[2..4], is: "+rows+"X"+columns);
        }
        this.name=name;
        this.rows=rows;
        this.columns=columns;
        this.isMatrix=true;
        this.location=-1;
        init(data);
    }

    private void init(String name, int components, Object data) {
        if( 1>components || components>4 ) {
            throw new GLException("components must be within [1..4], is: "+components);
        }
        this.name=name;
        this.columns=components;
        this.rows=1;
        this.isMatrix=false;
        this.location=-1;
        init(data);
    }

    private void init(Object data) {
        if(data instanceof Buffer) {
            final int sz = rows*columns;
            final Buffer buffer = (Buffer)data;
            if(buffer.remaining()<sz || 0!=buffer.remaining()%sz) {
                throw new GLException("remaining data buffer size invalid: buffer: "+buffer.toString()+"\n\t"+this);
            }
            this.count=buffer.remaining()/(rows*columns);
        } else {
            if(isMatrix) {
                throw new GLException("Atom type not allowed for matrix : "+this);
            }
            this.count=1;
        }
        this.data=data;
    }

    public String getName() { return name; }

    public int getLocation() { return location; }

    /**
     * Sets the given location of the shader uniform.
     * @return the given location
     */
    public int setLocation(int location) { this.location=location; return location; }

    /**
     * Retrieves the location of the shader uniform from the linked shader program.
     * <p>
     * No validation is performed within the implementation.
     * </p>
     * @param gl
     * @param program
     * @return &ge;0 denotes a valid uniform location as found and used in the given shader program.
     *         &lt;0 denotes an invalid location, i.e. not found or used in the given shader program.
     */
    public int setLocation(GL2ES2 gl, int program) {
        location = gl.glGetUniformLocation(program, name);
        return location;
    }

    public Object getObject() {
        return data;
    }
    public Buffer getBuffer() {
        return (data instanceof Buffer)?(Buffer)data:null;
    }
    public boolean isBuffer() {
        return (data instanceof Buffer);
    }
    public boolean isMatrix() { return isMatrix; }

    public int     count() { return count; }
    public int     components() { return rows*columns; }
    public int     rows() { return rows; }
    public int     columns() { return columns; }

    private String name;
    private int    location;
    private int    rows, columns;
    private int    count;
    private Object data;
    private boolean isMatrix;
}

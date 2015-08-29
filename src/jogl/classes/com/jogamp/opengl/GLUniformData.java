
package com.jogamp.opengl;

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
    public GLUniformData(final String name, final int val) {
        initScalar(name, 1, Integer.valueOf(val));
    }

    /**
     * float atom
     *
     * Number of objects is 1
     *
     */
    public GLUniformData(final String name, final float val) {
        initScalar(name, 1, Float.valueOf(val));
    }

    /**
     * Multiple IntBuffer Vector
     *
     * Number of objects is calculated by data.limit()/components
     *
     * @param components number of elements of one object, ie 4 for GL_FLOAT_VEC4,
     */
    public GLUniformData(final String name, final int components, final IntBuffer data) {
        initBuffer(name, components, data);
    }

    /**
     * Multiple FloatBuffer Vector
     *
     * Number of objects is calculated by data.limit()/components
     *
     * @param components number of elements of one object, ie 4 for GL_FLOAT_VEC4,
     */
    public GLUniformData(final String name, final int components, final FloatBuffer data) {
        initBuffer(name, components, data);
    }

    private GLUniformData(final int components, final String name) {
        initBuffer(name, components, null);
    }

    public static GLUniformData creatEmptyVector(final String name, final int components) {
        return new GLUniformData(components, name);
    }

    public static GLUniformData creatEmptyMatrix(final String name, final int rows, final int columns) {
        return new GLUniformData(name, rows, columns, null);
    }

    /**
     * Multiple FloatBuffer Matrix
     *
     * Number of objects is calculated by data.limit()/(rows*columns)
     *
     * @param rows the matrix rows
     * @param column the matrix column
     */
    public GLUniformData(final String name, final int rows, final int columns, final FloatBuffer data) {
        initBuffer(name, rows, columns, data);
    }

    public GLUniformData setData(final int data) { initScalar(Integer.valueOf(data)); return this; }
    public GLUniformData setData(final float data) { initScalar(Float.valueOf(data)); return this; }
    public GLUniformData setData(final IntBuffer data) { initBuffer(data); return this; }
    public GLUniformData setData(final FloatBuffer data) { initBuffer(data); return this; }

    public int       intValue()   { return ((Integer)data).intValue(); };
    public float     floatValue() { return ((Float)data).floatValue(); };
    public IntBuffer intBufferValue()   { return (IntBuffer)data; };
    public FloatBuffer floatBufferValue() { return (FloatBuffer)data; };

    @SuppressWarnings("deprecation")
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

    private void initBuffer(final String name, final int rows, final int columns, final Buffer buffer) {
        if( 2>rows || rows>4 || 2>columns || columns>4 ) {
            throw new GLException("rowsXcolumns must be within [2..4]X[2..4], is: "+rows+"X"+columns);
        }
        this.name=name;
        this.rows=rows;
        this.columns=columns;
        this.isMatrix=true;
        this.location=-1;
        initBuffer(buffer);
    }
    private void initScalar(final String name, final int components, final Object data) {
        if( 1>components || components>4 ) {
            throw new GLException("components must be within [1..4], is: "+components);
        }
        this.name=name;
        this.columns=components;
        this.rows=1;
        this.isMatrix=false;
        this.location=-1;
        initScalar(data);
    }
    private void initBuffer(final String name, final int components, final Buffer buffer) {
        if( 1>components || components>4 ) {
            throw new GLException("components must be within [1..4], is: "+components);
        }
        this.name=name;
        this.columns=components;
        this.rows=1;
        this.isMatrix=false;
        this.location=-1;
        initBuffer(buffer);
    }

    private void initScalar(final Object data) {
        if(data instanceof Buffer) {
            initBuffer((Buffer)data);
        } else if( null != data ) {
            if(isMatrix) {
                throw new GLException("Atom type not allowed for matrix : "+this);
            }
            this.count=1;
            this.data=data;
        } else {
            this.count=0;
            this.data=data;
        }
    }

    private void initBuffer(final Buffer buffer) {
        if( null != buffer ) {
            final int sz = rows*columns;
            if(buffer.remaining()<sz || 0!=buffer.remaining()%sz) {
                throw new GLException("remaining data buffer size invalid: buffer: "+buffer.toString()+"\n\t"+this);
            }
            this.count=buffer.remaining()/sz;
            this.data=buffer;
        } else {
            this.count=0;
            this.data=null;
        }
    }

    public String getName() { return name; }

    public int getLocation() { return location; }

    /**
     * Sets the given location of the shader uniform.
     * @return the given location
     */
    public int setLocation(final int location) { this.location=location; return location; }

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
    public int setLocation(final GL2ES2 gl, final int program) {
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

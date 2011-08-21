
package javax.media.opengl;

import java.nio.*;

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

    public String toString() {
      return "GLUniformData[name "+name+
                       ", location "+location+ 
                       ", size "+rows+"*"+columns+
                       ", count "+count+ 
                       ", matrix "+isMatrix+ 
                       ", data "+data+ 
                       "]";
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
            if(buffer.limit()<sz || 0!=buffer.limit()%sz) {
                throw new GLException("data buffer size invalid: new buffer limit: "+buffer.limit()+"\n\t"+this);
            }
            this.count=buffer.limit()/(rows*columns);
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
     * Sets the determined location of the shader uniform.
     */
    public GLUniformData setLocation(int location) { this.location=location; return this; }

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

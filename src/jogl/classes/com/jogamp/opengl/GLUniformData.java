/**
 * Copyright 2009-2023 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.SyncAction;
import com.jogamp.opengl.util.SyncBuffer;

/**
 * GLSL uniform data wrapper encapsulating data to be uploaded to the GPU as a uniform.
 */
public final class GLUniformData {

    /**
     * int atom
     *
     * Number of objects is 1
     *
     * @param name the uniform name as used in the shader
     */
    public GLUniformData(final String name, final int val) {
        initScalar(name, 1, Integer.valueOf(val));
    }

    /**
     * float atom
     *
     * Number of objects is 1
     *
     * @param name the uniform name as used in the shader
     */
    public GLUniformData(final String name, final float val) {
        initScalar(name, 1, Float.valueOf(val));
    }

    /**
     * Multiple IntBuffer Vector
     *
     * Number of objects is calculated by data.limit()/components
     *
     * @param name the uniform name as used in the shader
     * @param components number of elements of one object, ie 4 for GL_FLOAT_VEC4,
     * @param data the data
     */
    public GLUniformData(final String name, final int components, final IntBuffer data) {
        initBuffer(name, components, data, null);
    }

    /**
     * Multiple FloatBuffer Vector
     *
     * Number of objects is calculated by data.limit()/components
     *
     * @param name the uniform name as used in the shader
     * @param components number of elements of one object, ie 4 for GL_FLOAT_VEC4,
     * @param data the underlying data
     */
    public GLUniformData(final String name, final int components, final FloatBuffer data) {
        initBuffer(name, components, data, null);
    }

    /**
     * Multiple IntBuffer or FloatBuffer Vector
     *
     * Number of objects is calculated by data.limit()/components
     *
     * @param name the uniform name as used in the shader
     * @param components number of elements of one object, ie 4 for GL_FLOAT_VEC4,
     * @param syncBuffer {@link SyncBuffer} providing {@link SyncAction} and {@link Buffer}, allowing to sync the buffer with the underlying data, see {@link #getBuffer()}
     */
    public GLUniformData(final String name, final int components, final SyncBuffer syncBuffer) {
        initBuffer(name, components, syncBuffer.getBuffer(), syncBuffer.getAction());
    }

    private GLUniformData(final int components, final String name) {
        initBuffer(name, components, null, null);
    }

    public static GLUniformData creatEmptyVector(final String name, final int components) {
        return new GLUniformData(components, name);
    }

    public static GLUniformData creatEmptyMatrix(final String name, final int rows, final int columns) {
        return new GLUniformData(name, rows, columns, (FloatBuffer)null);
    }

    /**
     * Multiple FloatBuffer Matrix
     *
     * Number of objects is calculated by data.limit()/(rows*columns)
     *
     * @param name the uniform name as used in the shader
     * @param rows the matrix rows
     * @param column the matrix column
     * @param data the underlying data
     */
    public GLUniformData(final String name, final int rows, final int columns, final FloatBuffer data) {
        initBuffer(name, rows, columns, data, null);
    }

    /**
     * Multiple FloatBuffer Matrix
     *
     * Number of objects is calculated by data.limit()/(rows*columns)
     *
     * @param name the uniform name as used in the shader
     * @param rows the matrix rows
     * @param column the matrix column
     * @param syncBuffer {@link SyncBuffer} providing {@link SyncAction} and {@link Buffer}, allowing to sync the buffer with the underlying data, see {@link #getBuffer()}
     */
    public GLUniformData(final String name, final int rows, final int columns, final SyncBuffer syncBuffer) {
        initBuffer(name, rows, columns, syncBuffer.getBuffer(), syncBuffer.getAction());
    }

    public GLUniformData setData(final int data) { initScalar(Integer.valueOf(data)); return this; }
    public GLUniformData setData(final float data) { initScalar(Float.valueOf(data)); return this; }

    public GLUniformData setData(final IntBuffer data) { initBuffer(data, null); return this; }
    public GLUniformData setData(final FloatBuffer data) { initBuffer(data, null); return this; }
    public GLUniformData setData(final SyncBuffer syncedBuffer) { initBuffer(syncedBuffer.getBuffer(), syncedBuffer.getAction()); return this; }

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

    private void initBuffer(final String name, final int rows, final int columns, final Buffer buffer, final SyncAction syncAction) {
        if( 2>rows || rows>4 || 2>columns || columns>4 ) {
            throw new GLException("rowsXcolumns must be within [2..4]X[2..4], is: "+rows+"X"+columns);
        }
        this.name=name;
        this.rows=rows;
        this.columns=columns;
        this.bits=BIT_MATRIX;
        this.location=-1;
        initBuffer(buffer, syncAction);
    }
    private void initScalar(final String name, final int components, final Object data) {
        if( 1>components || components>4 ) {
            throw new GLException("components must be within [1..4], is: "+components);
        }
        this.name=name;
        this.columns=components;
        this.rows=1;
        this.bits=0;
        this.location=-1;
        initScalar(data);
    }
    private void initBuffer(final String name, final int components, final Buffer buffer, final SyncAction syncAction) {
        if( 1>components || components>4 ) {
            throw new GLException("components must be within [1..4], is: "+components);
        }
        this.name=name;
        this.columns=components;
        this.rows=1;
        this.bits=0;
        this.location=-1;
        initBuffer(buffer, syncAction);
    }

    private void initScalar(final Object data) {
        if(data instanceof Buffer) {
            initBuffer((Buffer)data, null);
        } else if( null != data ) {
            if( isMatrix() ) {
                throw new GLException("Atom type not allowed for matrix : "+this);
            }
            this.count=1;
            this.data=data;
        } else {
            this.count=0;
            this.data=data;
        }
    }

    private void initBuffer(final Buffer buffer, final SyncAction syncAction) {
        if( null != buffer ) {
            this.bits |= BIT_BUFFER;
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
        this.syncAction = syncAction;
    }

    /** Return the uniform name as used in the shader */
    public String getName() { return name; }

    public int getLocation() { return location; }

    /**
     * Sets the given location of the shader uniform.
     * @return the given location
     */
    public int setLocation(final int location) { this.location=location; return location; }

    /**
     * Retrieves the location of the shader uniform with {@link #getName()} from the linked shader program.
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

    /**
     * Returns the data object.
     * <p>
     * In case a {@link SyncAction} has been set,
     * it is invoked to {@link SyncAction#sync() synchronize} the object with the underlying data before returning the object.
     * </p>
     * @return the data object.
     * @see SyncAction#sync()
     */
    public Object getObject() {
        if( null != syncAction ) {
            syncAction.sync();
        }
        return data;
    }

    /**
     * Returns the data buffer.
     * <p>
     * In case a {@link SyncAction} has been set,
     * it is invoked to {@link SyncAction#sync() synchronize} the buffer with the underlying data before returning the buffer.
     * </p>
     * @return the data buffer.
     * @see SyncAction#sync()
     */
    public Buffer getBuffer() {
        if( null != syncAction ) {
            syncAction.sync();
        }
        return (data instanceof Buffer)?(Buffer)data:null;
    }

    public boolean isMatrix() { return 0 != ( BIT_MATRIX & bits ); }
    public boolean isBuffer() { return 0 != ( BIT_BUFFER & bits ); }

    public int     count() { return count; }
    public int     components() { return rows*columns; }
    public int     rows() { return rows; }
    public int     columns() { return columns; }

    private static final short BIT_MATRIX = 0b0000000000000001;
    private static final short BIT_BUFFER = 0b0000000000000010;

    private String name;
    private int    location;
    private int    rows, columns;
    private int    count;
    private Object data;
    private short  bits;
    private SyncAction syncAction;
}

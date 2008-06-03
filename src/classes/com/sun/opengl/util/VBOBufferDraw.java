
package com.sun.opengl.util;

import javax.media.opengl.*;
import java.nio.*;

public class VBOBufferDraw {

  public VBOBufferDraw(int glArrayType, int glDataType, int glBufferUsage, int comps, int initialSize) {
    switch(glArrayType) {
        case GL.GL_VERTEX_ARRAY:
        case GL.GL_NORMAL_ARRAY:
        case GL.GL_COLOR_ARRAY:
        case GL.GL_TEXTURE_COORD_ARRAY:
            break;
        default:
            throw new GLException("invalid glArrayType: "+glArrayType+":\n\t"+this); 
    }
    this.glArrayType = glArrayType;
    this.glDataType = glDataType;
    this.clazz = getBufferClass(glDataType);
    this.buffer = null;
    this.components = comps;
    this.initialSize = initialSize;
    switch(glBufferUsage) {
        case GL.GL_STATIC_DRAW:
        // FIXME: case GL.GL_STREAM_DRAW:
        case GL.GL_DYNAMIC_DRAW:
            break;
        default:
            throw new GLException("invalid glBufferUsage: "+glBufferUsage+":\n\t"+this); 
    }
    this.glBufferUsage = glBufferUsage;
    this.vboName = 0;
    this.sealed=false;
    this.bufferEnabled=false;
    growVBO(initialSize);
  }

  public int getGLArrayType() {
    return glArrayType;
  }

  public int getGlDataType() {
    return glDataType;
  }

  public int getComponents() {
    return components;
  }

  public Class getBufferClass() {
    return clazz;
  }

  public Buffer getBuffer() {
    return buffer;
  }

  public int getBufferUsage() {
    return glBufferUsage;
  }

  public void destroy(GL gl) {
    reset(gl);
    if(vboName!=0) {
        int[] tmp = new int[1];
        tmp[0] = vboName;
        gl.glDeleteBuffers(1, tmp, 0);
        vboName = 0;
    }
  }

  public void reset() {
    reset(null);
  }

  public void reset(GL gl) {
    if(gl!=null) {
        disableBuffer(gl);
    }
    this.sealed=false;
    if(buffer!=null) {
        buffer.clear();
    }
  }

  private final void init_vbo(GL gl) {
    if(vboName==0) {
        int[] tmp = new int[1];
        gl.glGenBuffers(1, tmp, 0);
        vboName = tmp[0];
    }
  }

  private final void checkSeal(boolean test) throws GLException {
    if(sealed!=test) {
        if(test) {
            throw new GLException("Not Sealed yet, seal first:\n\t"+this); 
        } else {
            throw new GLException("Already Sealed, can't modify VBO:\n\t"+this); 
        }
    }
  }

  public final boolean growVBOIfNecessary(int spare) {
    if(buffer==null) {
        throw new GLException("buffer no configured:\n\t"+this);
    }
    if(buffer!=null && buffer.remaining()<spare) { 
        growVBO();
        return true;
    }
    return false;
  }

  public final void growVBO() {
    growVBO(initialSize);
  }

  public static final Class getBufferClass(int glDataType) {
    switch(glDataType) {
        case GL.GL_BYTE:
        case GL.GL_UNSIGNED_BYTE:
            return ByteBuffer.class;
        case GL.GL_SHORT:
        case GL.GL_UNSIGNED_SHORT:
            return ShortBuffer.class;
        case GL.GL_FIXED:
            return IntBuffer.class;
        case GL.GL_FLOAT:
            return FloatBuffer.class;
        default:        
            throw new GLException("Given OpenGL data type not supported: "+glDataType);
    }
  }

  public final int getBufferCompSize() {
    if(clazz==ByteBuffer.class) {
        return BufferUtil.SIZEOF_BYTE;
    }
    if(clazz==ShortBuffer.class) {
        return BufferUtil.SIZEOF_SHORT;
    }
    if(clazz==IntBuffer.class) {
        return BufferUtil.SIZEOF_INT;
    }
    if(clazz==FloatBuffer.class) {
        return BufferUtil.SIZEOF_FLOAT;
    }
    throw new GLException("Given Buffer Class not supported: "+clazz+":\n\t"+this);
  }

  public final void growVBO(int additional) {
    int osize;

    checkSeal(false);

    if(components>0) {
        osize = (buffer!=null)?buffer.capacity():0;
        if(clazz==ByteBuffer.class) {
            ByteBuffer newBBuffer = BufferUtil.newByteBuffer( (osize+additional) * components );
            if(buffer!=null) {
                buffer.flip();
                newBBuffer.put((ByteBuffer)buffer);
            }
            buffer = newBBuffer;
        } else if(clazz==ShortBuffer.class) {
            ShortBuffer newSBuffer = BufferUtil.newShortBuffer( (osize+additional) * components );
            if(buffer!=null) {
                buffer.flip();
                newSBuffer.put((ShortBuffer)buffer);
            }
            buffer = newSBuffer;
        } else if(clazz==IntBuffer.class) {
            IntBuffer newIBuffer = BufferUtil.newIntBuffer( (osize+additional) * components );
            if(buffer!=null) {
                buffer.flip();
                newIBuffer.put((IntBuffer)buffer);
            }
            buffer = newIBuffer;
        } else if(clazz==FloatBuffer.class) {
            FloatBuffer newFBuffer = BufferUtil.newFloatBuffer( (osize+additional) * components );
            if(buffer!=null) {
                buffer.flip();
                newFBuffer.put((FloatBuffer)buffer);
            }
            buffer = newFBuffer;
        } else {
            throw new GLException("Given Buffer Class not supported: "+clazz+":\n\t"+this);
        }
    }
  }

  public void rewind() {
    checkSeal(true);

    if(buffer!=null) {
        buffer.rewind();
    }
  }

  public int getVerticeNumber() {
    return ( buffer!=null ) ? ( buffer.limit() / components ) : 0 ;
  }

  public void seal(GL gl, boolean disableAfterSeal)
  {
    checkSeal(false);
    sealed = true;
    init_vbo(gl);

    if (null!=buffer) {
        buffer.flip();
        enableBuffer(gl, true);
    }
    if(null==buffer || disableAfterSeal) {
        disableBuffer(gl);
    }

  }

  public void enableBuffer(GL gl)
  {
        enableBuffer(gl, false);
  }

  private void enableBuffer(GL gl, boolean newData)
  {
    checkSeal(true);
    
    if(!bufferEnabled && null!=buffer) {
        gl.glEnableClientState(glArrayType);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboName);
        if(newData) {
            gl.glBufferData(GL.GL_ARRAY_BUFFER, buffer.limit() * getBufferCompSize(), buffer, glBufferUsage);
        }
        switch(glArrayType) {
            case GL.GL_VERTEX_ARRAY:
                gl.glVertexPointer(components, glDataType, 0, 0);
                break;
            case GL.GL_NORMAL_ARRAY:
                gl.glNormalPointer(components, glDataType, 0);
                break;
            case GL.GL_COLOR_ARRAY:
                gl.glColorPointer(components, glDataType, 0, 0);
                break;
            case GL.GL_TEXTURE_COORD_ARRAY:
                gl.glTexCoordPointer(components, glDataType, 0, 0);
                break;
            default:
                throw new GLException("invalid glArrayType: "+glArrayType+":\n\t"+this); 
        }
        bufferEnabled = true;
    }
  }

  public void disableBuffer(GL gl) {
    if(bufferEnabled && null!=buffer) {
        gl.glDisableClientState(glArrayType);
        bufferEnabled = false;
    }
  }

  public void padding(int done) {
    if(buffer==null) return; // JAU
    if(buffer==null) {
        throw new GLException("buffer no configured:\n\t"+this);
    }
    while(done<components) {
        if(clazz==ByteBuffer.class) {
            ((ByteBuffer)buffer).put((byte)0);
        } else if(clazz==ShortBuffer.class) {
            ((ShortBuffer)buffer).put((short)0);
        } else if(clazz==IntBuffer.class) {
            ((IntBuffer)buffer).put(0);
        } else if(clazz==FloatBuffer.class) {
            ((FloatBuffer)buffer).put(0f);
        } else {
            throw new GLException("Given Buffer Class not supported: "+clazz+" :\n\t"+this);
        }
        done++;
    }
  }

  public void putb(byte v) {
    if(buffer==null) return; // JAU
    growVBOIfNecessary(1);
    if(clazz==ByteBuffer.class) {
        ((ByteBuffer)buffer).put(v);
    } else if(clazz==ShortBuffer.class) {
        ((ShortBuffer)buffer).put((short)v);
    } else if(clazz==IntBuffer.class) {
        ((IntBuffer)buffer).put((int)v);
    } else {
        throw new GLException("Byte doesn't match Buffer Class: "+clazz+" :\n\t"+this);
    }
  }

  public void puts(short v) {
    if(buffer==null) return; // JAU
    growVBOIfNecessary(1);
    if(clazz==ShortBuffer.class) {
        ((ShortBuffer)buffer).put(v);
    } else if(clazz==IntBuffer.class) {
        ((IntBuffer)buffer).put((int)v);
    } else {
        throw new GLException("Short doesn't match Buffer Class: "+clazz+" :\n\t"+this);
    }
  }

  public void puti(int v) {
    if(buffer==null) return; // JAU
    growVBOIfNecessary(1);
    if(clazz==IntBuffer.class) {
        ((IntBuffer)buffer).put(v);
    } else {
        throw new GLException("Integer doesn't match Buffer Class: "+clazz+" :\n\t"+this);
    }
  }

  public void putx(int v) {
    if(buffer==null) return; // JAU
    growVBOIfNecessary(1);
    if(clazz==IntBuffer.class) {
        ((IntBuffer)buffer).put(v);
    } else {
        throw new GLException("Fixed doesn't match Buffer Class: "+clazz+" :\n\t"+this);
    }
  }

  public void putf(float v) {
    if(buffer==null) return; // JAU
    growVBOIfNecessary(1);
    if(clazz==FloatBuffer.class) {
        ((FloatBuffer)buffer).put(v);
    } else if(clazz==IntBuffer.class) {
        ((IntBuffer)buffer).put(Float2Fixed(v));
    } else {
        throw new GLException("Float doesn't match Buffer Class: "+clazz+" :\n\t"+this);
    }
  }

  public void putd(double v) {
    if(buffer==null) return; // JAU
    growVBOIfNecessary(1);
    if(clazz==FloatBuffer.class) {
        // FIXME: ok ?
        ((FloatBuffer)buffer).put((float)v);
    } else {
        throw new GLException("Double doesn't match Buffer Class: "+clazz+" :\n\t"+this);
    }
  }

  public String toString() {
    return "VBOBufferDraw[vertices "+getVerticeNumber()+
                       ", glArrayType "+glArrayType+
                       ", glDataType "+glDataType+ 
                       ", bufferClazz "+clazz+ 
                       ", components "+components+ 
                       ", initialSize "+initialSize+ 
                       ", glBufferUsage "+glBufferUsage+ 
                       ", vboName "+vboName+ 
                       ", sealed "+sealed+ 
                       ", bufferEnabled "+bufferEnabled+ 
                       ",\n\tbuffer "+buffer+ 
                       "]";
  }

  public static final int Float2Fixed(float value)
  {
    if (value < -32768) value = -32768;
    if (value > 32767) value = 32767;
    return (int)(value * 65536);
  }

  private int glArrayType;
  private int glDataType;
  private Class clazz;
  private Buffer buffer;
  private int components;
  private int initialSize;
  private int glBufferUsage;
  private int vboName;
  private boolean sealed;
  private boolean bufferEnabled;

}


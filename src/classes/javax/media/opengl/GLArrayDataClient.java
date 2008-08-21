
package javax.media.opengl;

import javax.media.opengl.util.*;
import com.sun.opengl.impl.*;
import com.sun.opengl.impl.glsl.*;

import java.nio.*;

public class GLArrayDataClient implements GLArrayData {

  /**
   * The OpenGL ES emulation on the PC probably has a buggy VBO implementation,
   * where we have to 'refresh' the VertexPointer or VertexAttribArray after each
   * BindBuffer !
   *
   * This should not be necessary on proper native implementations.
   */
  public static final boolean hasVBOBug = (SystemUtil.getenv("JOGL_VBO_BUG") != null);

  /**
   * @arg index The GL array index
   * @arg name  The optional custom name for the GL array index, maybe null.
   *            If null, the default name mapping will be used, see 'getPredefinedArrayIndexName(int)'.
   *            This name might be used as the shader attribute name.
   * @arg comps The array component number
   * @arg dataType The array index GL data type
   * @arg normalized Wheather the data shall be normalized
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataClient createFixed(int index, String name, int comps, int dataType, boolean normalized, 
                                              int initialSize)
    throws GLException
  {
      GLProfile.isValidateArrayDataType(index, comps, dataType, false, true);
      GLArrayDataClient adc = new GLArrayDataClient();
      GLArrayHandler glArrayHandler = new GLFixedArrayHandler(adc);
      adc.init(name, index, comps, dataType, normalized, 0, null, initialSize, false, glArrayHandler);
      return adc;
  }

  public static GLArrayDataClient createFixed(int index, String name, int comps, int dataType, boolean normalized, 
                                              int stride, Buffer buffer)
    throws GLException
  {
      GLProfile.isValidateArrayDataType(index, comps, dataType, false, true);
      GLArrayDataClient adc = new GLArrayDataClient();
      GLArrayHandler glArrayHandler = new GLFixedArrayHandler(adc);
      adc.init(name, index, comps, dataType, normalized, stride, buffer, comps*comps, false, glArrayHandler);
      return adc;
  }

  public static GLArrayDataClient createGLSL(String name, int comps, int dataType, boolean normalized, 
                                             int initialSize)
    throws GLException
  {
      if(!GLProfile.isGL2ES2()) {
        throw new GLException("GLArrayDataServer not supported for profile: "+GLProfile.getProfile());
      }
      GLProfile.isValidateArrayDataType(-1, comps, dataType, true, true);

      GLArrayDataClient adc = new GLArrayDataClient();
      GLArrayHandler glArrayHandler = new GLSLArrayHandler(adc);
      adc.init(name, -1, comps, dataType, normalized, 0, null, initialSize, true, glArrayHandler);
      return adc;
  }

  public static GLArrayDataClient createGLSL(String name, int comps, int dataType, boolean normalized, 
                                             int stride, Buffer buffer)
    throws GLException
  {
      if(!GLProfile.isGL2ES2()) {
        throw new GLException("GLArrayDataServer not supported for profile: "+GLProfile.getProfile());
      }
      GLProfile.isValidateArrayDataType(-1, comps, dataType, true, true);

      GLArrayDataClient adc = new GLArrayDataClient();
      GLArrayHandler glArrayHandler = new GLSLArrayHandler(adc);
      adc.init(name, -1, comps, dataType, normalized, stride, buffer, comps*comps, true, glArrayHandler);
      return adc;
  }

  // 
  // Data read access
  //

  public final boolean isVertexAttribute() { return isVertexAttribute; }

  public final int getIndex() { return index; }

  public final int getLocation() { return location; }

  public final void setLocation(int v) { location = v; }

  public final String getName() { return name; }

  public long getOffset() { return -1; }

  public final boolean isBufferWritten() { return bufferWritten; }

  public boolean isVBO() { return false; }

  public int getVBOName() { return -1; }

  public int getBufferUsage() { return -1; }

  public final int getComponentNumber() { return components; }

  public final int getComponentType() { return dataType; }

  public final int getComponentSize() {
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

  public final int getElementNumber() {
    if(null==buffer) return 0;
    return ( sealed ) ? ( buffer.limit() / components ) : ( buffer.position() / components ) ;
  }

  public final boolean getNormalized() { return normalized; }

  public final int getStride() { return stride; }

  public final boolean sealed() { return sealed; }

  public final Class getBufferClass() { return clazz; }

  //
  // Data and GL state modification ..
  //

  public final Buffer getBuffer() { return buffer; }

  public final void setBufferWritten(boolean written) { bufferWritten=written; }

  public void setName(String newName) {
    location = -1;
    name = newName;
  }

  public void destroy(GL gl) {
    reset(gl);
  }

  public void reset(GL gl) {
    enableBuffer(gl, false);
    reset();
  }

  public void seal(GL gl, boolean seal)
  {
    seal(seal);
    if(sealedGL==seal) return;
    sealedGL = seal;
    if(seal) {
        init_vbo(gl);

        enableBuffer(gl, true);
    } else {
        enableBuffer(gl, false);
    }
  }

  public void enableBuffer(GL gl, boolean enable) {
    if(enableBufferAlways && enable) {
        bufferEnabled = false;
    }
    if( bufferEnabled != enable && components>0 ) {
        if(enable) {
            checkSeal(true);
            if(null!=buffer) {
                buffer.rewind();
            }
        }
        glArrayHandler.enableBuffer(gl, enable);
        bufferEnabled = enable;
    }
  }

  public void setEnableAlways(boolean always) {
    enableBufferAlways = always;
  }

  //
  // Data modification ..
  //

  public void reset() {
    if(buffer!=null) {
        buffer.clear();
    }
    this.sealed=false;
    this.bufferEnabled=false;
    this.bufferWritten=false;
  }

  public void seal(boolean seal)
  {
    if(sealed==seal) return;
    sealed = seal;
    if(seal) {
        bufferWritten=false;
        if (null!=buffer) {
            buffer.flip();
        }
    } else {
        if (null!=buffer) {
            buffer.position(buffer.limit());
            buffer.limit(buffer.capacity());
        }
    }
  }


  public void rewind() {
    if(buffer!=null) {
        buffer.rewind();
    }
  }

  public void padding(int done) {
    if ( buffer==null || sealed ) return;
    while(done<strideL) {
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

  /**
   * Generic buffer relative put method.
   *
   * This class buffer Class must match the arguments buffer class.
   * The arguments remaining elements must be a multiple of this arrays element stride.
   */
  public void put(Buffer v) {
    if ( buffer==null || sealed ) return;
    if(0!=(v.remaining() % strideL)) {
        throw new GLException("Buffer length ("+v.remaining()+") is not a multiple of component-stride:\n\t"+this);
    }
    Class vClazz = v.getClass();
    if(!GLReflection.instanceOf(vClazz, clazz.getName())) {
        throw new GLException("This array's buffer class "+clazz+" doesn't match the argument's Class: "+vClazz+" :\n\t"+this);
    }
    growBufferIfNecessary(v.remaining());
    if(clazz==ByteBuffer.class) {
        ((ByteBuffer)buffer).put((ByteBuffer)v);
    } else if(clazz==ShortBuffer.class) {
        ((ShortBuffer)buffer).put((ShortBuffer)v);
    } else if(clazz==IntBuffer.class) {
        ((IntBuffer)buffer).put((IntBuffer)v);
    } else if(clazz==FloatBuffer.class) {
        ((FloatBuffer)buffer).put((FloatBuffer)v);
    }
  }

  public void putb(byte v) {
    if ( buffer==null || sealed ) return;
    growBufferIfNecessary(1);
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
    if ( buffer==null || sealed ) return;
    growBufferIfNecessary(1);
    if(clazz==ShortBuffer.class) {
        ((ShortBuffer)buffer).put(v);
    } else if(clazz==IntBuffer.class) {
        ((IntBuffer)buffer).put((int)v);
    } else {
        throw new GLException("Short doesn't match Buffer Class: "+clazz+" :\n\t"+this);
    }
  }

  public void puti(int v) {
    if ( buffer==null || sealed ) return;
    growBufferIfNecessary(1);
    if(clazz==IntBuffer.class) {
        ((IntBuffer)buffer).put(v);
    } else {
        throw new GLException("Integer doesn't match Buffer Class: "+clazz+" :\n\t"+this);
    }
  }

  public void putx(int v) {
    puti(v);
  }

  public void putf(float v) {
    if ( buffer==null || sealed ) return;
    growBufferIfNecessary(1);
    if(clazz==FloatBuffer.class) {
        ((FloatBuffer)buffer).put(v);
    } else if(clazz==IntBuffer.class) {
        ((IntBuffer)buffer).put(FixedPoint.toFixed(v));
    } else {
        throw new GLException("Float doesn't match Buffer Class: "+clazz+" :\n\t"+this);
    }
  }

  public void putd(double v) {
    if ( buffer==null || sealed ) return;
    growBufferIfNecessary(1);
    if(clazz==FloatBuffer.class) {
        ((FloatBuffer)buffer).put((float)v);
    } else {
        throw new GLException("Double doesn't match Buffer Class: "+clazz+" :\n\t"+this);
    }
  }

  public String toString() {
    return "GLArrayDataClient["+name+
                       ", index "+index+
                       ", location "+location+
                       ", isVertexAttribute "+isVertexAttribute+
                       ", dataType "+dataType+ 
                       ", bufferClazz "+clazz+ 
                       ", elements "+getElementNumber()+
                       ", components "+components+ 
                       ", stride "+stride+"u "+strideB+"b "+strideL+"c"+
                       ", initialSize "+initialSize+ 
                       ", sealed "+sealed+ 
                       ", bufferEnabled "+bufferEnabled+ 
                       ", bufferWritten "+bufferWritten+ 
                       ", buffer "+buffer+ 
                       "]";
  }

  public static final Class getBufferClass(int dataType) {
    switch(dataType) {
        case GL.GL_BYTE:
        case GL.GL_UNSIGNED_BYTE:
            return ByteBuffer.class;
        case GL.GL_SHORT:
        case GL.GL_UNSIGNED_SHORT:
            return ShortBuffer.class;
        case GL2ES1.GL_FIXED:
            return IntBuffer.class;
        case GL.GL_FLOAT:
            return FloatBuffer.class;
        default:    
            throw new GLException("Given OpenGL data type not supported: "+dataType);
    }
  }

  // non public matters

  protected final boolean growBufferIfNecessary(int spare) {
    if(buffer==null || buffer.remaining()<spare) { 
        growBuffer(initialSize);
        return true;
    }
    return false;
  }

  protected final void growBuffer(int additional) {
    if(sealed || 0==additional || 0==components) return;

    // add the stride delta
    additional += (additional/components)*(strideL-components);

    if(components>0) {
        int osize = (buffer!=null)?buffer.capacity():0;
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

  protected final void checkSeal(boolean test) throws GLException {
    if(sealed!=test) {
        if(test) {
            throw new GLException("Not Sealed yet, seal first:\n\t"+this); 
        } else {
            throw new GLException("Already Sealed, can't modify VBO:\n\t"+this); 
        }
    }
  }

  protected void init(String name, int index, int comps, int dataType, boolean normalized, int stride, Buffer data, 
                      int initialSize, boolean isVertexAttribute, GLArrayHandler handler) 
    throws GLException
  {
    this.glArrayHandler = handler;
    this.isVertexAttribute = isVertexAttribute;
    this.index = index;
    this.location = -1;
    this.name = (null==name)?GLContext.getPredefinedArrayIndexName(index):name;
    if(null==this.name) {
        throw new GLException("Not a valid GL array index: "+index);
    }
    this.dataType = dataType;
    this.clazz = getBufferClass(dataType);
    switch(dataType) {
        case GL.GL_BYTE:
        case GL.GL_UNSIGNED_BYTE:
        case GL.GL_SHORT:
        case GL.GL_UNSIGNED_SHORT:
        case GL2ES1.GL_FIXED:
            this.normalized = normalized;
            break;
        default:    
            this.normalized = false;
    }

    int bpc = getComponentSize();
    if(0<stride && stride<comps*bpc) {
        throw new GLException("stride ("+stride+") lower than component bytes, "+comps+" * "+bpc);
    }
    if(0<stride && stride%bpc!=0) {
        throw new GLException("stride ("+stride+") not a multiple of bpc "+bpc);
    }
    this.buffer = data;
    this.components = comps;
    this.stride=stride;
    this.strideB=(0==stride)?comps*bpc:stride;
    this.strideL=(0==stride)?comps:strideB/bpc;
    this.initialSize = initialSize;
    this.sealed=false;
    this.sealedGL=false;
    this.bufferEnabled=false;
    this.enableBufferAlways=false;
    this.bufferWritten=false;
    if(null==buffer) {
        growBuffer(initialSize);
    }
  }

  protected void init_vbo(GL gl) {}

  protected GLArrayDataClient() { }

  protected int index;
  protected int location;
  protected String name;
  protected int components;
  protected int dataType;
  protected boolean normalized;
  protected int stride;  // user given stride
  protected int strideB; // stride in bytes
  protected int strideL; // stride in logical components
  protected Class clazz;
  protected Buffer buffer;
  protected int initialSize;
  protected boolean isVertexAttribute;
  protected boolean sealed, sealedGL;
  protected boolean bufferEnabled;
  protected boolean bufferWritten;
  protected boolean enableBufferAlways;

  protected GLArrayHandler glArrayHandler;
}


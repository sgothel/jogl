
package com.sun.opengl.util;

import java.security.*;

import javax.media.opengl.*;

import com.sun.opengl.util.glsl.*;

import com.sun.opengl.impl.SystemUtil;

import java.nio.*;

public class GLArrayDataClient extends GLArrayDataWrapper implements GLArrayDataEditable {

  /**
   * The OpenGL ES emulation on the PC probably has a buggy VBO implementation,
   * where we have to 'refresh' the VertexPointer or VertexAttribArray after each
   * BindBuffer !
   *
   * This should not be necessary on proper native implementations.
   */
  public static final boolean hasVBOBug = AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
              return SystemUtil.getenv("JOGL_VBO_BUG");
          }
      }) != null;

  /**
   * @param index The GL array index
   * @param name  The optional custom name for the GL array index, maybe null.
   *            If null, the default name mapping will be used, see 'getPredefinedArrayIndexName(int)'.
   *            This name might be used as the shader attribute name.
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Wheather the data shall be normalized
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataClient createFixed(GL gl, int index, String name, int comps, int dataType, boolean normalized, 
                                              int initialSize)
    throws GLException
  {
      gl.getGLProfile().isValidArrayDataType(index, comps, dataType, false, true);
      GLArrayDataClient adc = new GLArrayDataClient();
      GLArrayHandler glArrayHandler = new GLFixedArrayHandler(adc);
      adc.init(name, index, comps, dataType, normalized, 0, null, initialSize, false, glArrayHandler, 0, 0);
      return adc;
  }

  public static GLArrayDataClient createFixed(GL gl, int index, String name, int comps, int dataType, boolean normalized, 
                                              int stride, Buffer buffer)
    throws GLException
  {
      gl.getGLProfile().isValidArrayDataType(index, comps, dataType, false, true);
      GLArrayDataClient adc = new GLArrayDataClient();
      GLArrayHandler glArrayHandler = new GLFixedArrayHandler(adc);
      adc.init(name, index, comps, dataType, normalized, stride, buffer, comps*comps, false, glArrayHandler, 0, 0);
      return adc;
  }

  public static GLArrayDataClient createGLSL(GL gl, String name, int comps, int dataType, boolean normalized, 
                                             int initialSize)
    throws GLException
  {
      if(!gl.hasGLSL()) {
        throw new GLException("GLArrayDataClient.GLSL not supported: "+gl);
      }
      gl.getGLProfile().isValidArrayDataType(-1, comps, dataType, true, true);

      GLArrayDataClient adc = new GLArrayDataClient();
      GLArrayHandler glArrayHandler = new GLSLArrayHandler(adc);
      adc.init(name, -1, comps, dataType, normalized, 0, null, initialSize, true, glArrayHandler, 0, 0);
      return adc;
  }

  public static GLArrayDataClient createGLSL(GL gl, String name, int comps, int dataType, boolean normalized, 
                                             int stride, Buffer buffer)
    throws GLException
  {
      if(!gl.hasGLSL()) {
        throw new GLException("GLArrayDataClient.GLSL not supported: "+gl);
      }
      gl.getGLProfile().isValidArrayDataType(-1, comps, dataType, true, true);

      GLArrayDataClient adc = new GLArrayDataClient();
      GLArrayHandler glArrayHandler = new GLSLArrayHandler(adc);
      adc.init(name, -1, comps, dataType, normalized, stride, buffer, comps*comps, true, glArrayHandler, 0, 0);
      return adc;
  }

  // 
  // Data read access
  //

  public final boolean isBufferWritten() { return bufferWritten; }

  public final boolean sealed() { return sealed; }

  public int getBufferUsage() { return -1; }

  //
  // Data and GL state modification ..
  //

  public final void setBufferWritten(boolean written) { bufferWritten=written; }

  public void destroy(GL gl) {
    reset(gl);
    buffer=null;
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
        BufferUtil.putb(buffer, (byte)0);
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
    growBufferIfNecessary(v.remaining());
    BufferUtil.put(buffer, v);
  }

  public void putb(byte v) {
    if ( buffer==null || sealed ) return;
    growBufferIfNecessary(1);
    BufferUtil.putb(buffer, v);
  }

  public void puts(short v) {
    if ( buffer==null || sealed ) return;
    growBufferIfNecessary(1);
    BufferUtil.puts(buffer, v);
  }

  public void puti(int v) {
    if ( buffer==null || sealed ) return;
    growBufferIfNecessary(1);
    BufferUtil.puti(buffer, v);
  }

  public void putx(int v) {
    puti(v);
  }

  public void putf(float v) {
    if ( buffer==null || sealed ) return;
    growBufferIfNecessary(1);
    BufferUtil.putf(buffer, v);
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
                      int initialSize, boolean isVertexAttribute, GLArrayHandler handler,
                      int vboName, long bufferOffset)
    throws GLException
  {
    super.init(name, index, comps, dataType, normalized, stride, data, isVertexAttribute,
               vboName, bufferOffset);

    this.initialSize = initialSize;
    this.glArrayHandler = handler;
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

  protected boolean sealed, sealedGL;
  protected boolean bufferEnabled;
  protected boolean bufferWritten;
  protected boolean enableBufferAlways;

  protected int initialSize;

  protected GLArrayHandler glArrayHandler;
}


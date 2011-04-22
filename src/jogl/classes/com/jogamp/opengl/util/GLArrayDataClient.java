
package com.jogamp.opengl.util;

import com.jogamp.common.nio.Buffers;
import java.security.*;

import javax.media.opengl.*;

import com.jogamp.opengl.util.glsl.*;

import jogamp.opengl.SystemUtil;

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
   * Create a client side buffer object, using a predefined fixed function array index
   * and starting with a new created Buffer object with initialSize size
   *
   * On profiles GL2 and ES1 the fixed function pipeline behavior is as expected.
   * On profile ES2 the fixed function emulation will transform these calls to 
   * EnableVertexAttribArray and VertexAttribPointer calls,
   * and a predefined vertex attribute variable name will be chosen.
   * 
   * @param index The GL array index
   * @param name  The optional custom name for the GL array index, maybe null.
   *            If null, the default name mapping will be used, see 'getPredefinedArrayIndexName(int)'.
   *            This name might be used as the shader attribute name.
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param initialSize
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */  
  public static GLArrayDataClient createFixed(int index, String name, int comps, int dataType, boolean normalized, int initialSize)
    throws GLException
  {
      GLArrayDataClient adc = new GLArrayDataClient();
      GLArrayHandler glArrayHandler = new GLFixedArrayHandler(adc);
      adc.init(name, index, comps, dataType, normalized, 0, null, initialSize, false, glArrayHandler, -1, -1, -1, -1);
      return adc;
  }

  /**
   * Create a client side buffer object, using a predefined fixed function array index
   * and starting with a given Buffer object incl it's stride
   *
   * On profiles GL2 and ES1 the fixed function pipeline behavior is as expected.
   * On profile ES2 the fixed function emulation will transform these calls to 
   * EnableVertexAttribArray and VertexAttribPointer calls,
   * and a predefined vertex attribute variable name will be chosen.
   * 
   * @param index The GL array index
   * @param name  The optional custom name for the GL array index, maybe null.
   *            If null, the default name mapping will be used, see 'getPredefinedArrayIndexName(int)'.
   *            This name might be used as the shader attribute name.
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param stride
   * @param buffer the user define data
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */  
  public static GLArrayDataClient createFixed(int index, String name, int comps, int dataType, boolean normalized, int stride, 
                                              Buffer buffer)
    throws GLException
  {
      GLArrayDataClient adc = new GLArrayDataClient();
      GLArrayHandler glArrayHandler = new GLFixedArrayHandler(adc);
      adc.init(name, index, comps, dataType, normalized, stride, buffer, comps*comps, false, glArrayHandler, -1, -1, -1, -1);
      return adc;
  }

  /**
   * Create a client side buffer object, using a custom GLSL array attribute name
   * and starting with a new created Buffer object with initialSize size
   * 
   * @param st The ShaderState managing the state of the used shader program, vertex attributes and uniforms 
   * @param name  The custom name for the GL attribute. 
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param initialSize
   */
  public static GLArrayDataClient createGLSL(ShaderState st, String name, 
                                             int comps, int dataType, boolean normalized, int initialSize)
    throws GLException
  {
      GLArrayDataClient adc = new GLArrayDataClient();
      GLArrayHandler glArrayHandler = new GLSLArrayHandler(st, adc);
      adc.init(name, -1, comps, dataType, normalized, 0, null, initialSize, true, glArrayHandler, -1, -1, -1, -1);
      return adc;
  }

  /**
   * Create a client side buffer object, using a custom GLSL array attribute name
   * and starting with a given Buffer object incl it's stride
   * 
   * @param st The ShaderState managing the state of the used shader program, vertex attributes and uniforms
   * @param name  The custom name for the GL attribute. 
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param stride
   * @param buffer the user define data
   */
  public static GLArrayDataClient createGLSL(ShaderState st, String name,
                                             int comps, int dataType, boolean normalized, int stride, 
                                             Buffer buffer)
    throws GLException
  {
      GLArrayDataClient adc = new GLArrayDataClient();
      GLArrayHandler glArrayHandler = new GLSLArrayHandler(st, adc);
      adc.init(name, -1, comps, dataType, normalized, stride, buffer, comps*comps, true, glArrayHandler, -1, -1, -1, -1);
      return adc;
  }

  // 
  // Data read access
  //

  public final boolean isVBOWritten() { return bufferWritten; }

  public final boolean sealed() { return sealed; }
  
  public final boolean enabled() { return bufferEnabled; }

  //
  // Data and GL state modification ..
  //

  public final void setVBOWritten(boolean written) { bufferWritten=written; }

  public void destroy(GL gl) {
    reset(gl);
    super.destroy(gl);
  }

  public void reset(GL gl) {
    enableBuffer(gl, false);
    reset();
  }

  public void seal(GL gl, boolean seal) {
    seal(seal);
    enableBuffer(gl, seal);
  }

  public void enableBuffer(GL gl, boolean enable) {
    if( enableBufferAlways || bufferEnabled != enable ) { 
        if(enable) {
            checkSeal(true);
            // init/generate VBO name if not done yet
            init_vbo(gl);
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
    bufferWritten=false;
    if(seal) {
        if (null!=buffer) {
            buffer.flip();
        }
    } else if (null!=buffer) {
        buffer.position(buffer.limit());
        buffer.limit(buffer.capacity());
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
        Buffers.putb(buffer, (byte)0);
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
    if ( sealed ) return;
    if(0!=(v.remaining() % strideL)) {
        throw new GLException("Buffer length ("+v.remaining()+") is not a multiple of component-stride:\n\t"+this);
    }
    growBufferIfNecessary(v.remaining());
    Buffers.put(buffer, v);
  }

  public void putb(byte v) {
    if ( sealed ) return;
    growBufferIfNecessary(1);
    Buffers.putb(buffer, v);
  }

  public void puts(short v) {
    if ( sealed ) return;
    growBufferIfNecessary(1);
    Buffers.puts(buffer, v);
  }

  public void puti(int v) {
    if ( sealed ) return;
    growBufferIfNecessary(1);
    Buffers.puti(buffer, v);
  }

  public void putx(int v) {
    puti(v);
  }

  public void putf(float v) {
    if ( sealed ) return;
    growBufferIfNecessary(1);
    Buffers.putf(buffer, v);
  }

  public String toString() {
    return "GLArrayDataClient["+name+
                       ", index "+index+
                       ", location "+location+
                       ", isVertexAttribute "+isVertexAttribute+
                       ", dataType "+componentType+ 
                       ", bufferClazz "+componentClazz+ 
                       ", elements "+getElementNumber()+
                       ", components "+components+ 
                       ", stride "+stride+"u "+strideB+"b "+strideL+"c"+
                       ", initialSize "+initialSize+ 
                       ", sealed "+sealed+ 
                       ", bufferEnabled "+bufferEnabled+ 
                       ", bufferWritten "+bufferWritten+ 
                       ", buffer "+buffer+
                       ", alive "+alive+
                       "]";
  }

  // non public matters

  protected final boolean growBufferIfNecessary(int spare) {
    if(buffer==null || buffer.remaining()<spare) { 
        growBuffer(Math.max(initialSize, spare));
        return true;
    }
    return false;
  }

  protected final void growBuffer(int additional) {     
    if(!alive || sealed) {
       throw new GLException("Invalid state: "+this); 
    }

    // add the stride delta
    additional += (additional/components)*(strideL-components);

    int osize = (buffer!=null)?buffer.capacity():0;
    if(componentClazz==ByteBuffer.class) {
        ByteBuffer newBBuffer = Buffers.newDirectByteBuffer( (osize+additional) * components );
        if(buffer!=null) {
            buffer.flip();
            newBBuffer.put((ByteBuffer)buffer);
        }
        buffer = newBBuffer;
    } else if(componentClazz==ShortBuffer.class) {
        ShortBuffer newSBuffer = Buffers.newDirectShortBuffer( (osize+additional) * components );
        if(buffer!=null) {
            buffer.flip();
            newSBuffer.put((ShortBuffer)buffer);
        }
        buffer = newSBuffer;
    } else if(componentClazz==IntBuffer.class) {
        IntBuffer newIBuffer = Buffers.newDirectIntBuffer( (osize+additional) * components );
        if(buffer!=null) {
            buffer.flip();
            newIBuffer.put((IntBuffer)buffer);
        }
        buffer = newIBuffer;
    } else if(componentClazz==FloatBuffer.class) {
        FloatBuffer newFBuffer = Buffers.newDirectFloatBuffer( (osize+additional) * components );
        if(buffer!=null) {
            buffer.flip();
            newFBuffer.put((FloatBuffer)buffer);
        }
        buffer = newFBuffer;
    } else {
        throw new GLException("Given Buffer Class not supported: "+componentClazz+":\n\t"+this);
    }
  }

  protected final void checkSeal(boolean test) throws GLException {
    if(!alive) {
        throw new GLException("Invalid state: "+this); 
    }    
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
                      int vboName, long vboOffset, int vboUsage, int vboTarget)
    throws GLException
  {
    super.init(name, index, comps, dataType, normalized, stride, data, isVertexAttribute,
               vboName, vboOffset, vboUsage, vboTarget);

    this.initialSize = initialSize;
    this.glArrayHandler = handler;
    this.sealed=false;
    this.bufferEnabled=false;
    this.enableBufferAlways=false;
    this.bufferWritten=false;
    if(null==buffer && initialSize>0) {
        growBuffer(initialSize);
    }
  }

  private boolean isValidated = false;
  
  protected void init_vbo(GL gl) {
      if(!isValidated ) {
          isValidated = true;
          validate(gl.getGLProfile(), true);
      }      
  }

  protected GLArrayDataClient() { }

  protected boolean sealed;
  protected boolean bufferEnabled;
  protected boolean bufferWritten;
  protected boolean enableBufferAlways;

  protected int initialSize;

  protected GLArrayHandler glArrayHandler;
}


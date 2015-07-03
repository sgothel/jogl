/**
 * Copyright 2010  JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.util;

import java.lang.reflect.Constructor;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.fixedfunc.GLPointerFuncUtil;

import jogamp.opengl.util.GLArrayHandler;
import jogamp.opengl.util.GLFixedArrayHandler;
import jogamp.opengl.util.glsl.GLSLArrayHandler;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.glsl.ShaderState;


public class GLArrayDataClient extends GLArrayDataWrapper implements GLArrayDataEditable {

  /**
   * Create a client side buffer object, using a predefined fixed function array index
   * and starting with a new created Buffer object with initialElementCount size
   *
   * On profiles GL2 and ES1 the fixed function pipeline behavior is as expected.
   * On profile ES2 the fixed function emulation will transform these calls to
   * EnableVertexAttribArray and VertexAttribPointer calls,
   * and a predefined vertex attribute variable name will be chosen.
   *
   * The default name mapping will be used,
   * see {@link GLPointerFuncUtil#getPredefinedArrayIndexName(int)}.
   *
   * @param index The GL array index
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param initialElementCount
   *
   * @see com.jogamp.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataClient createFixed(final int index, final int comps, final int dataType, final boolean normalized, final int initialElementCount)
    throws GLException
  {
      final GLArrayDataClient adc = new GLArrayDataClient();
      final GLArrayHandler glArrayHandler = new GLFixedArrayHandler(adc);
      adc.init(null, index, comps, dataType, normalized, 0, null, initialElementCount, 0 /* mappedElementCount */, false, glArrayHandler, 0, 0, 0, 0, false);
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
   * The default name mapping will be used,
   * see {@link GLPointerFuncUtil#getPredefinedArrayIndexName(int)}.
   *
   * @param index The GL array index
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param stride
   * @param buffer the user define data
   *
   * @see com.jogamp.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataClient createFixed(final int index, final int comps, final int dataType, final boolean normalized, final int stride,
                                              final Buffer buffer)
    throws GLException
  {
      final GLArrayDataClient adc = new GLArrayDataClient();
      final GLArrayHandler glArrayHandler = new GLFixedArrayHandler(adc);
      adc.init(null, index, comps, dataType, normalized, stride, buffer, comps*comps, 0 /* mappedElementCount */, false, glArrayHandler, 0, 0, 0, 0, false);
      return adc;
  }

  /**
   * Create a client side buffer object, using a custom GLSL array attribute name
   * and starting with a new created Buffer object with initialElementCount size
   * @param name  The custom name for the GL attribute.
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param initialElementCount
   */
  public static GLArrayDataClient createGLSL(final String name, final int comps,
                                             final int dataType, final boolean normalized, final int initialElementCount)
    throws GLException
  {
      final GLArrayDataClient adc = new GLArrayDataClient();
      final GLArrayHandler glArrayHandler = new GLSLArrayHandler(adc);
      adc.init(name, -1, comps, dataType, normalized, 0, null, initialElementCount, 0 /* mappedElementCount */, true, glArrayHandler, 0, 0, 0, 0, true);
      return adc;
  }

  /**
   * Create a client side buffer object, using a custom GLSL array attribute name
   * and starting with a given Buffer object incl it's stride
   * @param name  The custom name for the GL attribute.
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param stride
   * @param buffer the user define data
   */
  public static GLArrayDataClient createGLSL(final String name, final int comps,
                                             final int dataType, final boolean normalized, final int stride, final Buffer buffer)
    throws GLException
  {
      final GLArrayDataClient adc = new GLArrayDataClient();
      final GLArrayHandler glArrayHandler = new GLSLArrayHandler(adc);
      adc.init(name, -1, comps, dataType, normalized, stride, buffer, comps*comps, 0 /* mappedElementCount */, true, glArrayHandler, 0, 0, 0, 0, true);
      return adc;
  }

  @Override
  public void associate(final Object obj, final boolean enable) {
      if(obj instanceof ShaderState) {
          if(enable) {
              shaderState = (ShaderState)obj;
          } else {
              shaderState = null;
          }
      }
  }

  //
  // Data read access
  //

  @Override
  public final boolean isVBOWritten() { return bufferWritten; }

  @Override
  public final boolean sealed() { return sealed; }

  @Override
  public final boolean enabled() { return bufferEnabled; }

  //
  // Data and GL state modification ..
  //

  @Override
  public final void setVBOWritten(final boolean written) {
      bufferWritten = ( 0 == mappedElementCount ) ? written : true;
  }

  @Override
  public void destroy(final GL gl) {
    reset(gl);
    super.destroy(gl);
  }

  @Override
  public void reset(final GL gl) {
    enableBuffer(gl, false);
    reset();
  }

  @Override
  public void seal(final GL gl, final boolean seal) {
    seal(seal);
    enableBuffer(gl, seal);
  }

  @Override
  public void enableBuffer(final GL gl, final boolean enable) {
    if( enableBufferAlways || bufferEnabled != enable ) {
        if(enable) {
            checkSeal(true);
            // init/generate VBO name if not done yet
            init_vbo(gl);
        }
        glArrayHandler.enableState(gl, enable, usesGLSL ? shaderState : null);
        bufferEnabled = enable;
    }
  }

  @Override
  public boolean bindBuffer(final GL gl, final boolean bind) {
      if(bind) {
          checkSeal(true);
          // init/generate VBO name if not done yet
          init_vbo(gl);
      }
      return glArrayHandler.bindBuffer(gl, bind);
  }

  @Override
  public void setEnableAlways(final boolean always) {
    enableBufferAlways = always;
  }

  //
  // Data modification ..
  //

  @Override
  public void reset() {
    if( buffer != null ) {
        buffer.clear();
    }
    sealed = false;
    bufferEnabled = false;
    bufferWritten = ( 0 == mappedElementCount ) ? false : true;
  }

  @Override
  public void seal(final boolean seal)
  {
    if( sealed == seal ) return;
    sealed = seal;
    bufferWritten = ( 0 == mappedElementCount ) ? false : true;
    if( seal ) {
        if ( null != buffer ) {
            buffer.flip();
        }
    } else if ( null != buffer ) {
        buffer.position(buffer.limit());
        buffer.limit(buffer.capacity());
    }
  }


  @Override
  public void rewind() {
    if(buffer!=null) {
        buffer.rewind();
    }
  }

  @Override
  public void padding(int doneInByteSize) {
    if ( buffer==null || sealed ) return;
    while(doneInByteSize<strideB) {
        Buffers.putb(buffer, (byte)0);
        doneInByteSize++;
    }
  }

  /**
   * Generic buffer relative put method.
   *
   * This class buffer Class must match the arguments buffer class.
   * The arguments remaining elements must be a multiple of this arrays element stride.
   */
  @Override
  public void put(final Buffer v) {
    if ( sealed ) return;
    /** FIXME: isn't true for interleaved arrays !
    if(0!=(v.remaining() % strideL)) {
        throw new GLException("Buffer length ("+v.remaining()+") is not a multiple of component-stride:\n\t"+this);
    } */
    growBufferIfNecessary(v.remaining());
    Buffers.put(buffer, v);
  }

  @Override
  public void putb(final byte v) {
    if ( sealed ) return;
    growBufferIfNecessary(1);
    Buffers.putb(buffer, v);
  }

  @Override
  public void puts(final short v) {
    if ( sealed ) return;
    growBufferIfNecessary(1);
    Buffers.puts(buffer, v);
  }

  @Override
  public void puti(final int v) {
    if ( sealed ) return;
    growBufferIfNecessary(1);
    Buffers.puti(buffer, v);
  }

  @Override
  public void putx(final int v) {
    puti(v);
  }

  @Override
  public void putf(final float v) {
    if ( sealed ) return;
    growBufferIfNecessary(1);
    Buffers.putf(buffer, v);
  }

  @Override
  public String toString() {
    return "GLArrayDataClient["+name+
                       ", index "+index+
                       ", location "+location+
                       ", isVertexAttribute "+isVertexAttribute+
                       ", usesGLSL "+usesGLSL+
                       ", usesShaderState "+(null!=shaderState)+
                       ", dataType 0x"+Integer.toHexString(componentType)+
                       ", bufferClazz "+componentClazz+
                       ", elements "+getElementCount()+
                       ", components "+componentsPerElement+
                       ", stride "+strideB+"b "+strideL+"c"+
                       ", mappedElementCount "+mappedElementCount+
                       ", initialElementCount "+initialElementCount+
                       ", sealed "+sealed+
                       ", bufferEnabled "+bufferEnabled+
                       ", bufferWritten "+bufferWritten+
                       ", buffer "+buffer+
                       ", alive "+alive+
                       "]";
  }

  // non public matters

  protected final boolean growBufferIfNecessary(final int spareComponents) {
    if( buffer==null || buffer.remaining()<spareComponents ) {
        if( 0 != mappedElementCount ) {
            throw new GLException("Mapped buffer can't grow. Insufficient storage size: Needed "+spareComponents+" components, "+
                                  "mappedElementCount "+mappedElementCount+
                                  ", has mapped buffer "+buffer+"; "+this);
        }
        growBuffer(Math.max(initialElementCount, (spareComponents+componentsPerElement-1)/componentsPerElement));
        return true;
    }
    return false;
  }

  protected final void growBuffer(int additionalElements) {
    if(!alive || sealed) {
       throw new GLException("Invalid state: "+this);
    }

    // add the stride delta
    additionalElements += (additionalElements/componentsPerElement)*(strideL-componentsPerElement);

    final int osize = (buffer!=null) ? buffer.capacity() : 0;
    final int nsize = osize + ( additionalElements * componentsPerElement );
    final Buffer oldBuffer = buffer;

    if(componentClazz==ByteBuffer.class) {
        final ByteBuffer newBBuffer = Buffers.newDirectByteBuffer( nsize );
        if(buffer!=null) {
            buffer.flip();
            newBBuffer.put((ByteBuffer)buffer);
        }
        buffer = newBBuffer;
    } else if(componentClazz==ShortBuffer.class) {
        final ShortBuffer newSBuffer = Buffers.newDirectShortBuffer( nsize );
        if(buffer!=null) {
            buffer.flip();
            newSBuffer.put((ShortBuffer)buffer);
        }
        buffer = newSBuffer;
    } else if(componentClazz==IntBuffer.class) {
        final IntBuffer newIBuffer = Buffers.newDirectIntBuffer( nsize );
        if(buffer!=null) {
            buffer.flip();
            newIBuffer.put((IntBuffer)buffer);
        }
        buffer = newIBuffer;
    } else if(componentClazz==FloatBuffer.class) {
        final FloatBuffer newFBuffer = Buffers.newDirectFloatBuffer( nsize );
        if(buffer!=null) {
            buffer.flip();
            newFBuffer.put((FloatBuffer)buffer);
        }
        buffer = newFBuffer;
    } else {
        throw new GLException("Given Buffer Class not supported: "+componentClazz+":\n\t"+this);
    }
    if(DEBUG) {
        System.err.println("*** Grow: comps: "+componentsPerElement+", "+(osize/componentsPerElement)+"/"+osize+" -> "+(nsize/componentsPerElement)+"/"+nsize+
                           "; "+oldBuffer+" -> "+buffer+"; "+this);
    }
  }

  protected final void checkSeal(final boolean test) throws GLException {
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

  protected void init(final String name, final int index, final int comps, final int dataType, final boolean normalized, final int stride, final Buffer data,
                      final int initialElementCount, final int mappedElementCount, final boolean isVertexAttribute,
                      final GLArrayHandler handler, final int vboName, final long vboOffset, final int vboUsage, final int vboTarget, final boolean usesGLSL)
    throws GLException
  {
    super.init(name, index, comps, dataType, normalized, stride, data, mappedElementCount,
               isVertexAttribute, vboName, vboOffset, vboUsage, vboTarget);

    if( 0<mappedElementCount && 0<initialElementCount ) { // null!=buffer case validated in super.init(..)
        throw new IllegalArgumentException("mappedElementCount:="+mappedElementCount+" specified, but passing non zero initialElementSize");
    }
    this.initialElementCount = initialElementCount;
    this.glArrayHandler = handler;
    this.usesGLSL = usesGLSL;
    this.sealed=false;
    this.bufferEnabled=false;
    this.enableBufferAlways=false;
    this.bufferWritten = ( 0 == mappedElementCount ) ? false : true;

    if(null==buffer && initialElementCount>0) {
        growBuffer(initialElementCount);
    }
  }

  private boolean isValidated = false;

  protected void init_vbo(final GL gl) {
      if(!isValidated ) {
          isValidated = true;
          validate(gl.getGLProfile(), true);
      }
  }

  protected GLArrayDataClient() { }

  /**
   * Copy Constructor
   * <p>
   * Buffer is {@link Buffers#slice(Buffer) sliced}, i.e. sharing content but using own state.
   * </p>
   * <p>
   * All other values are simply copied.
   * </p>
   */
  public GLArrayDataClient(final GLArrayDataClient src) {
    super(src);
    this.isValidated = src.isValidated;
    this.sealed = src.sealed;
    this.bufferEnabled = src.bufferEnabled;
    this.bufferWritten = src.bufferWritten;
    this.enableBufferAlways = src.enableBufferAlways;
    this.initialElementCount = src.initialElementCount;
    if( null != src.glArrayHandler ) {
        final Class<? extends GLArrayHandler> clazz = src.glArrayHandler.getClass();
        try {
            final Constructor<? extends GLArrayHandler> ctor = clazz.getConstructor(GLArrayDataEditable.class);
            this.glArrayHandler = ctor.newInstance(this);
        } catch (final Exception e) {
            throw new RuntimeException("Could not ctor "+clazz.getName()+"("+this.getClass().getName()+")", e);
        }
    } else {
        this.glArrayHandler = null;
    }
    this.usesGLSL = src.usesGLSL;
    this.shaderState = src.shaderState;
  }

  protected boolean sealed;
  protected boolean bufferEnabled;
  protected boolean bufferWritten;
  protected boolean enableBufferAlways;

  protected int initialElementCount;

  protected GLArrayHandler glArrayHandler;
  protected boolean usesGLSL;
  protected ShaderState shaderState;

}


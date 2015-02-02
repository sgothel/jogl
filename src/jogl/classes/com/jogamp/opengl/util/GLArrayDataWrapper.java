/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLArrayData;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLPointerFuncUtil;

import com.jogamp.common.nio.Buffers;

import jogamp.opengl.Debug;

public class GLArrayDataWrapper implements GLArrayData {
  public static final boolean DEBUG = Debug.debug("GLArrayData");

  /**
   * Create a VBO, using a predefined fixed function array index, wrapping the given data.
   *
   * @param index The GL array index
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param stride
   * @param buffer the user define data
   * @param vboName
   * @param vboOffset
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   * @param vboTarget {@link GL#GL_ARRAY_BUFFER} or {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   * @return the new create instance
   *
   * @throws GLException
   */
  public static GLArrayDataWrapper createFixed(final int index, final int comps, final int dataType, final boolean normalized, final int stride,
                                               final Buffer buffer, final int vboName, final long vboOffset, final int vboUsage, final int vboTarget)
    throws GLException
  {
      final GLArrayDataWrapper adc = new GLArrayDataWrapper();
      adc.init(null, index, comps, dataType, normalized, stride, buffer, 0 /* mappedElementCount */,
               false, vboName, vboOffset, vboUsage, vboTarget);
      return adc;
  }

  /**
   * Create a VBO, using a predefined fixed function array index, wrapping the mapped data characteristics.
   *
   * @param index The GL array index
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param stride
   * @param mappedElementCount
   * @param vboName
   * @param vboOffset
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   * @param vboTarget {@link GL#GL_ARRAY_BUFFER} or {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   * @return the new create instance
   *
   * @throws GLException
   */
  public static GLArrayDataWrapper createFixed(final int index, final int comps, final int dataType, final boolean normalized, final int stride,
                                               final int mappedElementCount, final int vboName, final long vboOffset, final int vboUsage, final int vboTarget)
    throws GLException
  {
      final GLArrayDataWrapper adc = new GLArrayDataWrapper();
      adc.init(null, index, comps, dataType, normalized, stride, null, mappedElementCount,
               false, vboName, vboOffset, vboUsage, vboTarget);
      return adc;
  }

  /**
   * Create a VBO, using a custom GLSL array attribute name, wrapping the given data.
   *
   * @param name  The custom name for the GL attribute, maybe null if gpuBufferTarget is {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param stride
   * @param buffer the user define data
   * @param vboName
   * @param vboOffset
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   * @param vboTarget {@link GL#GL_ARRAY_BUFFER} or {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   * @return the new create instance
   * @throws GLException
   */
  public static GLArrayDataWrapper createGLSL(final String name, final int comps, final int dataType, final boolean normalized, final int stride,
                                             final Buffer buffer, final int vboName, final long vboOffset, final int vboUsage, final int vboTarget)
    throws GLException
  {
      final GLArrayDataWrapper adc = new GLArrayDataWrapper();
      adc.init(name, -1, comps, dataType, normalized, stride, buffer, 0  /* mappedElementCount */,
              true, vboName, vboOffset, vboUsage, vboTarget);
      return adc;
  }

  /**
   * Create a VBO, using a custom GLSL array attribute name, wrapping the mapped data characteristics.
   *
   * @param name  The custom name for the GL attribute, maybe null if gpuBufferTarget is {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param stride
   * @param mappedElementCount
   * @param vboName
   * @param vboOffset
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   * @param vboTarget {@link GL#GL_ARRAY_BUFFER} or {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   * @return the new create instance
   * @throws GLException
   */
  public static GLArrayDataWrapper createGLSL(final String name, final int comps, final int dataType, final boolean normalized, final int stride,
                                              final int mappedElementCount, final int vboName, final long vboOffset, final int vboUsage, final int vboTarget)
    throws GLException
  {
      final GLArrayDataWrapper adc = new GLArrayDataWrapper();
      adc.init(name, -1, comps, dataType, normalized, stride, null, mappedElementCount,
              true, vboName, vboOffset, vboUsage, vboTarget);
      return adc;
  }

  /**
   * Validates this instance's parameter. Called automatically by {@link GLArrayDataClient} and {@link GLArrayDataServer}.
   * {@link GLArrayDataWrapper} does not validate it's instance by itself.
   *
   * @param glp the GLProfile to use
   * @param throwException whether to throw an exception if this instance has invalid parameter or not
   * @return true if this instance has invalid parameter, otherwise false
   */
  public final boolean validate(final GLProfile glp, final boolean throwException) {
    if(!alive) {
        if(throwException) {
            throw new GLException("Instance !alive "+this);
        }
        return false;
    }
    if(this.isVertexAttribute() && !glp.hasGLSL()) {
        if(throwException) {
            throw new GLException("GLSL not supported on "+glp+", "+this);
        }
        return false;
    }
    return glp.isValidArrayDataType(getIndex(), getComponentCount(), getComponentType(), isVertexAttribute(), throwException);
  }

  @Override
  public void associate(final Object obj, final boolean enable) {
      // nop
  }

  //
  // Data read access
  //

  @Override
  public final boolean isVertexAttribute() { return isVertexAttribute; }

  @Override
  public final int getIndex() { return index; }

  @Override
  public final int getLocation() { return location; }

  @Override
  public final int setLocation(final int v) { location = v; return location; }

  @Override
  public final int setLocation(final GL2ES2 gl, final int program) {
      location = gl.glGetAttribLocation(program, name);
      return location;
  }

  @Override
  public final int setLocation(final GL2ES2 gl, final int program, final int location) {
      this.location = location;
      gl.glBindAttribLocation(program, location, name);
      return location;
  }

  @Override
  public final String getName() { return name; }

  @Override
  public final long getVBOOffset() { return vboEnabled?vboOffset:0; }

  @Override
  public final int getVBOName() { return vboEnabled?vboName:0; }

  @Override
  public final boolean isVBO() { return vboEnabled; }

  @Override
  public final int getVBOUsage() { return vboEnabled?vboUsage:0; }

  @Override
  public final int getVBOTarget() { return vboEnabled?vboTarget:0; }

  @Override
  public Buffer getBuffer() { return buffer; }

  @Override
  public final int getComponentCount() { return componentsPerElement; }

  @Override
  public final int getComponentType() { return componentType; }

  @Override
  public final int getComponentSizeInBytes() { return componentByteSize; }

  @Override
  public final int getElementCount() {
    if( 0 != mappedElementCount ) {
        return mappedElementCount;
    } else if( null != buffer ) {
        final int remainingComponents = ( 0 == buffer.position() ) ? buffer.limit() : buffer.position();
        return ( remainingComponents * componentByteSize ) / strideB ;
    } else {
        return 0;
    }
  }

  @Override
  public final int getSizeInBytes() {
    if( 0 != mappedElementCount ) {
        return mappedElementCount * componentsPerElement * componentByteSize ;
    } else if( null != buffer ) {
        return ( buffer.position()==0 ) ? ( buffer.limit() * componentByteSize ) : ( buffer.position() * componentByteSize ) ;
    } else {
        return 0;
    }
  }

  @Override
  public final boolean getNormalized() { return normalized; }

  @Override
  public final int getStride() { return strideB; }

  public final Class<?> getBufferClass() { return componentClazz; }

  @Override
  public void destroy(final GL gl) {
    buffer = null;
    vboName=0;
    vboEnabled=false;
    vboOffset=0;
    alive = false;
  }

  @Override
  public String toString() {
    return "GLArrayDataWrapper["+name+
                       ", index "+index+
                       ", location "+location+
                       ", isVertexAttribute "+isVertexAttribute+
                       ", dataType 0x"+Integer.toHexString(componentType)+
                       ", bufferClazz "+componentClazz+
                       ", elements "+getElementCount()+
                       ", components "+componentsPerElement+
                       ", stride "+strideB+"b "+strideL+"c"+
                       ", mappedElementCount "+mappedElementCount+
                       ", buffer "+buffer+
                       ", vboEnabled "+vboEnabled+
                       ", vboName "+vboName+
                       ", vboUsage 0x"+Integer.toHexString(vboUsage)+
                       ", vboTarget 0x"+Integer.toHexString(vboTarget)+
                       ", vboOffset "+vboOffset+
                       ", alive "+alive+
                       "]";
  }

  public static final Class<?> getBufferClass(final int dataType) {
    switch(dataType) {
        case GL.GL_BYTE:
        case GL.GL_UNSIGNED_BYTE:
            return ByteBuffer.class;
        case GL.GL_SHORT:
        case GL.GL_UNSIGNED_SHORT:
            return ShortBuffer.class;
        case GL.GL_UNSIGNED_INT:
        case GL.GL_FIXED:
        case GL2ES2.GL_INT:
            return IntBuffer.class;
        case GL.GL_FLOAT:
            return FloatBuffer.class;
        default:
            throw new GLException("Given OpenGL data type not supported: "+dataType);
    }
  }

  @Override
  public void setName(final String newName) {
    location = -1;
    name = newName;
  }

  /**
   * Enable or disable use of VBO.
   * Only possible if a VBO buffer name is defined.
   * @see #setVBOName(int)
   */
  public void setVBOEnabled(final boolean vboEnabled) {
    this.vboEnabled=vboEnabled;
  }

  /**
   * Set the VBO buffer name, if valid (!= 0) enable use of VBO,
   * otherwise (==0) disable VBO usage.
   *
   * @see #setVBOEnabled(boolean)
   */
  public void    setVBOName(final int vboName) {
    this.vboName=vboName;
    setVBOEnabled(0!=vboName);
  }

 /**
  * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
  */
  public void setVBOUsage(final int vboUsage) {
      this.vboUsage = vboUsage;
  }

  /**
   * @param vboTarget either {@link GL#GL_ARRAY_BUFFER} or {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   */
  public void setVBOTarget(final int vboTarget) {
      this.vboTarget = vboTarget;
  }

  protected void init(final String name, final int index, final int componentsPerElement, final int componentType,
                      final boolean normalized, final int stride, final Buffer data, final int mappedElementCount,
                      final boolean isVertexAttribute, final int vboName, final long vboOffset, final int vboUsage, final int vboTarget)
    throws GLException
  {
    if( 0<mappedElementCount && null != data ) {
        throw new IllegalArgumentException("mappedElementCount:="+mappedElementCount+" specified, but passing non null buffer");
    }
    this.isVertexAttribute = isVertexAttribute;
    this.index = index;
    this.location = -1;
    // We can't have any dependence on the FixedFuncUtil class here for build bootstrapping reasons

    if( GL.GL_ELEMENT_ARRAY_BUFFER == vboTarget ) {
        // OK ..
    } else if( ( 0 == vboUsage && 0 == vboTarget ) || GL.GL_ARRAY_BUFFER == vboTarget ) {
        // Set/Check name .. - Required for GLSL case. Validation and debug-name for FFP.
        this.name = ( null == name ) ? GLPointerFuncUtil.getPredefinedArrayIndexName(index) : name ;
        if(null == this.name ) {
            throw new GLException("Not a valid array buffer index: "+index);
        }
    } else if( 0 < vboTarget ) {
        throw new GLException("Invalid GPUBuffer target: 0x"+Integer.toHexString(vboTarget));
    }

    this.componentType = componentType;
    componentClazz = getBufferClass(componentType);
    if( GLBuffers.isGLTypeFixedPoint(componentType) ) {
        this.normalized = normalized;
    } else {
        this.normalized = false;
    }
    componentByteSize = GLBuffers.sizeOfGLType(componentType);
    if(0 > componentByteSize) {
        throw new GLException("Given componentType not supported: "+componentType+":\n\t"+this);
    }
    if(0 >= componentsPerElement) {
        throw new GLException("Invalid number of components: " + componentsPerElement);
    }
    this.componentsPerElement = componentsPerElement;

    if(0<stride && stride<componentsPerElement*componentByteSize) {
        throw new GLException("stride ("+stride+") lower than component bytes, "+componentsPerElement+" * "+componentByteSize);
    }
    if(0<stride && stride%componentByteSize!=0) {
        throw new GLException("stride ("+stride+") not a multiple of bpc "+componentByteSize);
    }
    this.buffer = data;
    this.mappedElementCount = mappedElementCount;
    this.strideB=(0==stride)?componentsPerElement*componentByteSize:stride;
    this.strideL=strideB/componentByteSize;
    this.vboName= vboName;
    this.vboEnabled= 0 != vboName ;
    this.vboOffset=vboOffset;

    switch(vboUsage) {
        case 0: // nop
        case GL.GL_STATIC_DRAW:
        case GL.GL_DYNAMIC_DRAW:
        case GL2ES2.GL_STREAM_DRAW:
            break;
        default:
            throw new GLException("invalid gpuBufferUsage: "+vboUsage+":\n\t"+this);
    }
    switch(vboTarget) {
        case 0: // nop
        case GL.GL_ARRAY_BUFFER:
        case GL.GL_ELEMENT_ARRAY_BUFFER:
            break;
        default:
            throw new GLException("invalid gpuBufferTarget: "+vboTarget+":\n\t"+this);
    }
    this.vboUsage=vboUsage;
    this.vboTarget=vboTarget;
    this.alive=true;
  }

  protected GLArrayDataWrapper() { }

  /**
   * Copy Constructor
   * <p>
   * Buffer is {@link Buffers#slice(Buffer) sliced}, i.e. sharing content but using own state.
   * </p>
   * <p>
   * All other values are simply copied.
   * </p>
   */
  public GLArrayDataWrapper(final GLArrayDataWrapper src) {
    this.alive = src.alive;
    this.index = src.index;
    this.location = src.location;
    this.name = src.name;
    this.componentsPerElement = src.componentsPerElement;
    this.componentType = src.componentType;
    this.componentClazz = src.componentClazz;
    this.componentByteSize = src.componentByteSize;
    this.normalized = src.normalized;
    this.strideB = src.strideB;
    this.strideL = src.strideL;
    if( null != src.buffer ) {
        if( src.buffer.position() == 0 ) {
            this.buffer = Buffers.slice(src.buffer);
        } else {
            this.buffer = Buffers.slice(src.buffer, 0, src.buffer.limit());
        }
    } else {
        this.buffer = null;
    }
    this.mappedElementCount = src.mappedElementCount;
    this.isVertexAttribute = src.isVertexAttribute;
    this.vboOffset = src.vboOffset;
    this.vboName = src.vboName;
    this.vboEnabled = src.vboEnabled;
    this.vboUsage = src.vboUsage;
    this.vboTarget = src.vboTarget;
  }

  protected boolean alive;
  protected int index;
  protected int location;
  protected String name;
  protected int componentsPerElement;
  protected int componentType;
  protected Class<?> componentClazz;
  protected int componentByteSize;
  protected boolean normalized;
  /** stride in bytes; strideB >= componentsPerElement * componentByteSize */
  protected int strideB;
  /** stride in logical components */
  protected int strideL;
  protected Buffer buffer;
  protected int mappedElementCount;
  protected boolean isVertexAttribute;
  protected long vboOffset;
  protected int vboName;
  protected boolean vboEnabled;
  protected int vboUsage;
  protected int vboTarget;
}


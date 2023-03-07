/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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
   * <p>
   * This buffer is always {@link #sealed()}.
   * </p>
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
      return new GLArrayDataWrapper(null, index, comps, dataType, normalized, stride, buffer, 0 /* mappedElementCount */,
                                    false, vboName, vboOffset, vboUsage, vboTarget);
  }

  /**
   * Create a VBO, using a predefined fixed function array index, wrapping the mapped data characteristics.
   * <p>
   * This buffer is always {@link #sealed()}.
   * </p>
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
      return new GLArrayDataWrapper(null, index, comps, dataType, normalized, stride, null, mappedElementCount,
                                    false, vboName, vboOffset, vboUsage, vboTarget);
  }

  /**
   * Create a VBO, using a custom GLSL array attribute name, wrapping the given data.
   * <p>
   * This buffer is always {@link #sealed()}.
   * </p>
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
      return new GLArrayDataWrapper(name, -1, comps, dataType, normalized, stride, buffer, 0  /* mappedElementCount */,
                                    true, vboName, vboOffset, vboUsage, vboTarget);
  }

  /**
   * Create a VBO, using a custom GLSL array attribute name, wrapping the mapped data characteristics.
   * <p>
   * This buffer is always {@link #sealed()}.
   * </p>
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
      return new GLArrayDataWrapper(name, -1, comps, dataType, normalized, stride, null, mappedElementCount,
                                    true, vboName, vboOffset, vboUsage, vboTarget);
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
    // Skip GLProfile based index, comps, type validation, might not be future proof.
    // glp.isValidArrayDataType(getIndex(), getCompsPerElem(), getCompType(), isVertexAttribute(), throwException);
    return true;
  }

  @Override
  public void associate(final Object obj, final boolean enable) {
      // nop
  }

  //
  // Data read access
  //

  @Override
  public final boolean isVertexAttribute() { return isVertexAttr; }

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
  public final int getCompsPerElem() { return compsPerElement; }

  @Override
  public final int getCompType() { return compType; }

  @Override
  public final int getBytesPerComp() { return bytesPerComp; }

  @Override
  public final boolean sealed() { return sealed; }

  @Override
  public final int getElemCount() {
    if( 0 != mappedElemCount ) {
        return mappedElemCount;
    } else if( null != buffer ) {
        if( sealed ) {
            return ( buffer.limit() * bytesPerComp ) / strideB ;
        } else {
            return ( buffer.position() * bytesPerComp ) / strideB ;
        }
    } else {
        return 0;
    }
  }

  @Override
  public final int elemPosition() {
    if( 0 != mappedElemCount ) {
        return mappedElemCount;
    } else if( null != buffer ) {
        return ( buffer.position() * bytesPerComp ) / strideB ;
    } else {
        return 0;
    }
  }

  @Override
  public int remainingElems() {
      if( null != buffer ) {
          return ( buffer.remaining() * bytesPerComp ) / strideB ;
      } else {
          return 0;
      }
  }

  @Override
  public int getElemCapacity() {
    if( null != buffer ) {
        return ( buffer.capacity() * bytesPerComp ) / strideB ;
    } else {
        return 0;
    }
  }

  @Override
  public final int getByteCount() {
    if( 0 != mappedElemCount ) {
        return mappedElemCount * compsPerElement * bytesPerComp ;
    } else if( null != buffer ) {
        if( sealed ) {
            return buffer.limit() * bytesPerComp ;
        } else {
            return buffer.position() * bytesPerComp ;
        }
    } else {
        return 0;
    }
  }

  @Override
  public final int bytePosition() {
    if( 0 != mappedElemCount ) {
        return mappedElemCount * compsPerElement * bytesPerComp ;
    } else if( null != buffer ) {
        return buffer.position() * bytesPerComp;
    } else {
        return 0;
    }
  }

  @Override
  public int remainingBytes() {
      if( null != buffer ) {
          return buffer.remaining() * bytesPerComp;
      } else {
          return 0;
      }
  }

  @Override
  public int getByteCapacity() {
    if( null != buffer ) {
        return buffer.capacity() * bytesPerComp;
    } else {
        return 0;
    }
  }

  @Override
  public String fillStatsToString() {
      final int cnt_bytes = getByteCount();
      final int cap_bytes = getByteCapacity();
      final float filled = (float)cnt_bytes/(float)cap_bytes;
      return String.format("elements %,d cnt / %,d cap, bytes %,d cnt / %,d cap, filled %.1f%%, left %.1f%%",
              getElemCount(), getElemCapacity(), cnt_bytes, cap_bytes, filled*100f, (1f-filled)*100f);
  }

  @Override
  public String elemStatsToString() {
      final int elem_limit = null != buffer ? ( buffer.limit() * bytesPerComp ) / strideB : 0;
      return String.format("sealed %b, elements %,d cnt, [%,d pos .. %,d rem .. %,d lim .. %,d cap]",
              sealed(), getElemCount(), elemPosition(), remainingElems(), elem_limit, getElemCapacity());
  }

  @Override
  public final boolean getNormalized() { return normalized; }

  @Override
  public final int getStride() { return strideB; }

  public final Class<?> getBufferClass() { return compClazz; }

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
                       ", isVertexAttribute "+isVertexAttr+
                       ", dataType 0x"+Integer.toHexString(compType)+
                       ", bufferClazz "+compClazz+
                       ", compsPerElem "+compsPerElement+
                       ", stride "+strideB+"b "+strideL+"c"+
                       ", mappedElemCount "+mappedElemCount+
                       ", "+elemStatsToString()+
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
  public void setVBOName(final int vboName) {
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

  protected GLArrayDataWrapper(final String name, final int index, final int componentsPerElement, final int componentType,
                               final boolean normalized, final int stride, final Buffer data, final int mappedElementCount,
                               final boolean isVertexAttribute, final int vboName, final long vboOffset, final int vboUsage, final int vboTarget)
    throws GLException
  {
    if( 0<mappedElementCount && null != data ) {
        throw new IllegalArgumentException("mappedElementCount:="+mappedElementCount+" specified, but passing non null buffer");
    }
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

    // immutable types
    this.compType = componentType;
    compClazz = getBufferClass(componentType);
    bytesPerComp = GLBuffers.sizeOfGLType(componentType);
    if(0 > bytesPerComp) {
        throw new GLException("Given componentType not supported: "+componentType+":\n\t"+this);
    }
    if(0 >= componentsPerElement) {
        throw new GLException("Invalid number of components: " + componentsPerElement);
    }
    this.compsPerElement = componentsPerElement;

    if(0<stride && stride<componentsPerElement*bytesPerComp) {
        throw new GLException("stride ("+stride+") lower than component bytes, "+componentsPerElement+" * "+bytesPerComp);
    }
    if(0<stride && stride%bytesPerComp!=0) {
        throw new GLException("stride ("+stride+") not a multiple of bpc "+bytesPerComp);
    }
    this.strideB=(0==stride)?componentsPerElement*bytesPerComp:stride;
    this.strideL=strideB/bytesPerComp;

    if( GLBuffers.isGLTypeFixedPoint(componentType) ) {
        this.normalized = normalized;
    } else {
        this.normalized = false;
    }
    this.mappedElemCount = mappedElementCount;
    this.isVertexAttr = isVertexAttribute;

    // mutable types
    this.index = index;
    this.location = -1;
    this.buffer = data;
    this.vboName= vboName;
    this.vboOffset=vboOffset;
    this.vboEnabled= 0 != vboName ;

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
    this.sealed = true;
  }

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
    // immutable types
    this.compType = src.compType;
    this.compClazz = src.compClazz;
    this.bytesPerComp = src.bytesPerComp;
    this.compsPerElement = src.compsPerElement;
    this.strideB = src.strideB;
    this.strideL = src.strideL;
    this.normalized = src.normalized;
    this.mappedElemCount = src.mappedElemCount;
    this.isVertexAttr = src.isVertexAttr;

    // mutable types
    this.alive = src.alive;
    this.index = src.index;
    this.location = src.location;
    this.name = src.name;
    if( null != src.buffer ) {
        if( src.buffer.position() == 0 ) {
            this.buffer = Buffers.slice(src.buffer);
        } else {
            this.buffer = Buffers.slice(src.buffer, 0, src.buffer.limit());
        }
    } else {
        this.buffer = null;
    }
    this.vboName = src.vboName;
    this.vboOffset = src.vboOffset;
    this.vboEnabled = src.vboEnabled;
    this.vboUsage = src.vboUsage;
    this.vboTarget = src.vboTarget;
    this.sealed = src.sealed;
  }

  protected final int compType;
  protected final Class<?> compClazz;
  protected final int bytesPerComp;
  protected final int compsPerElement;
  /** stride in bytes; strideB >= compsPerElement * bytesPerComp */
  protected final int strideB;
  /** stride in logical components */
  protected final int strideL;
  protected final boolean normalized;
  protected final int mappedElemCount;
  protected final boolean isVertexAttr;

  // mutable types
  protected boolean alive;
  protected int index;
  protected int location;
  protected String name;
  protected Buffer buffer;
  protected int vboName;
  protected long vboOffset;
  protected boolean vboEnabled;
  protected int vboUsage;
  protected int vboTarget;
  protected boolean sealed;
}


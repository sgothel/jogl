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
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLArrayData;
import com.jogamp.opengl.GLBufferStorage;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.fixedfunc.GLPointerFuncUtil;

import com.jogamp.common.nio.Buffers;

import jogamp.opengl.util.GLArrayHandler;
import jogamp.opengl.util.GLArrayHandlerInterleaved;
import jogamp.opengl.util.GLDataArrayHandler;
import jogamp.opengl.util.GLFixedArrayHandler;
import jogamp.opengl.util.GLFixedArrayHandlerFlat;
import jogamp.opengl.util.glsl.GLSLArrayHandler;
import jogamp.opengl.util.glsl.GLSLArrayHandlerFlat;
import jogamp.opengl.util.glsl.GLSLArrayHandlerInterleaved;


public class GLArrayDataServer extends GLArrayDataClient implements GLArrayDataEditable {

  //
  // lifetime matters
  //

  /**
   * Create a VBO, using a predefined fixed function array index
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
   * @param compsPerElement component count per element
   * @param dataType The component's OpenGL data type
   * @param normalized Whether the data shall be normalized
   * @param stride in bytes from one element to the other. If zero, compsPerElement * compSizeInBytes
   * @param buffer the user define data
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   *
   * @see com.jogamp.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataServer createFixed(final int index, final int compsPerElement, final int dataType, final boolean normalized, final int stride,
                                              final Buffer buffer, final int vboUsage)
    throws GLException
  {
    final GLArrayDataServer ads = new GLArrayDataServer();
    final GLArrayHandler glArrayHandler = new GLFixedArrayHandler(ads);
    ads.init(null, index, compsPerElement, dataType, normalized, stride, buffer, buffer.limit(), 0 /* mappedElementCount */, false,
             glArrayHandler, 0, 0, vboUsage, GL.GL_ARRAY_BUFFER, false);
    return ads;
  }

  /**
   * Create a VBO, using a predefined fixed function array index
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
   * @param compsPerElement component count per element
   * @param dataType The component's OpenGL data type
   * @param normalized Whether the data shall be normalized
   * @param initialElementCount
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   *
   * @see com.jogamp.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataServer createFixed(final int index, final int compsPerElement, final int dataType, final boolean normalized, final int initialElementCount,
                                              final int vboUsage)
    throws GLException
  {
    final GLArrayDataServer ads = new GLArrayDataServer();
    final GLArrayHandler glArrayHandler = new GLFixedArrayHandler(ads);
    ads.init(null, index, compsPerElement, dataType, normalized, 0, null, initialElementCount, 0 /* mappedElementCount */, false,
             glArrayHandler, 0, 0, vboUsage, GL.GL_ARRAY_BUFFER, false);
    return ads;
  }

  /**
   * Create a VBO, using a custom GLSL array attribute name
   * and starting with a new created Buffer object with initialElementCount size
   * @param name  The custom name for the GL attribute
   * @param compsPerElement component count per element
   * @param dataType The component's OpenGL data type
   * @param normalized Whether the data shall be normalized
   * @param initialElementCount
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createGLSL(final String name, final int compsPerElement,
                                             final int dataType, final boolean normalized, final int initialElementCount, final int vboUsage)
    throws GLException
  {
    final GLArrayDataServer ads = new GLArrayDataServer();
    final GLArrayHandler glArrayHandler = new GLSLArrayHandler(ads);
    ads.init(name, -1, compsPerElement, dataType, normalized, 0, null, initialElementCount,
             0 /* mappedElementCount */, true, glArrayHandler, 0, 0, vboUsage, GL.GL_ARRAY_BUFFER, true);
    return ads;
  }

  /**
   * Create a VBO, using a custom GLSL array attribute name
   * intended for GPU buffer storage mapping, see {@link GLBufferStorage}, via {@link #mapStorage(GL, int)} and {@link #mapStorage(GL, long, long, int)}.
   * @param name  The custom name for the GL attribute
   * @param compsPerElement component count per element
   * @param dataType The component's OpenGL data type
   * @param normalized Whether the data shall be normalized
   * @param mappedElementCount
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createGLSLMapped(final String name, final int compsPerElement,
                                                   final int dataType, final boolean normalized, final int mappedElementCount, final int vboUsage)
    throws GLException
  {
    final GLArrayDataServer ads = new GLArrayDataServer();
    final GLArrayHandler glArrayHandler = new GLSLArrayHandler(ads);
    ads.init(name, -1, compsPerElement, dataType, normalized, 0, null, 0 /* initialElementCount */,
             mappedElementCount, true, glArrayHandler, 0, 0, vboUsage, GL.GL_ARRAY_BUFFER, true);
    ads.seal(true);
    return ads;
  }

  /**
   * Create a VBO, using a custom GLSL array attribute name
   * and starting with a given Buffer object incl it's stride
   * @param name  The custom name for the GL attribute
   * @param compsPerElement component count per element
   * @param dataType The component's OpenGL data type
   * @param normalized Whether the data shall be normalized
   * @param stride in bytes from one element to the other. If zero, compsPerElement * compSizeInBytes
   * @param buffer the user define data
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createGLSL(final String name, final int compsPerElement,
                                             final int dataType, final boolean normalized, final int stride, final Buffer buffer,
                                             final int vboUsage)
    throws GLException
  {
    final GLArrayDataServer ads = new GLArrayDataServer();
    final GLArrayHandler glArrayHandler = new GLSLArrayHandler(ads);
    ads.init(name, -1, compsPerElement, dataType, normalized, stride, buffer, buffer.limit(), 0 /* mappedElementCount */, true,
             glArrayHandler, 0, 0, vboUsage, GL.GL_ARRAY_BUFFER, true);
    return ads;
  }

  /**
   * Create a VBO data object for any target w/o render pipeline association, ie {@link GL#GL_ELEMENT_ARRAY_BUFFER}.
   *
   * Hence no index, name for a fixed function pipeline nor vertex attribute is given.
   *
   * @param compsPerElement component count per element
   * @param dataType The component's OpenGL data type
   * @param stride in bytes from one element to the other. If zero, compsPerElement * compSizeInBytes
   * @param buffer the user define data
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   * @param vboTarget {@link GL#GL_ELEMENT_ARRAY_BUFFER}, ..
   * {@link GL#glGenBuffers(int, int[], int)
   */
  public static GLArrayDataServer createData(final int compsPerElement, final int dataType, final int stride,
                                             final Buffer buffer, final int vboUsage, final int vboTarget)
    throws GLException
  {
    final GLArrayDataServer ads = new GLArrayDataServer();
    final GLArrayHandler glArrayHandler = new GLDataArrayHandler(ads);
    ads.init(null, -1, compsPerElement, dataType, false, stride, buffer, buffer.limit(), 0 /* mappedElementCount */, false,
             glArrayHandler, 0, 0, vboUsage, vboTarget, false);
    return ads;
  }

  /**
   * Create a VBO data object for any target w/o render pipeline association, ie {@link GL#GL_ELEMENT_ARRAY_BUFFER}.
   *
   * Hence no index, name for a fixed function pipeline nor vertex attribute is given.
   *
   * @param compsPerElement component count per element
   * @param dataType The component's OpenGL data type
   * @param initialElementCount
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   * @param vboTarget {@link GL#GL_ELEMENT_ARRAY_BUFFER}, ..
   */
  public static GLArrayDataServer createData(final int compsPerElement, final int dataType, final int initialElementCount,
                                             final int vboUsage, final int vboTarget)
    throws GLException
  {
    final GLArrayDataServer ads = new GLArrayDataServer();
    final GLArrayHandler glArrayHandler = new GLDataArrayHandler(ads);
    ads.init(null, -1, compsPerElement, dataType, false, 0, null, initialElementCount, 0 /* mappedElementCount */, false,
             glArrayHandler, 0, 0, vboUsage, vboTarget, false);
    return ads;
  }

  /**
   * Create a VBO data object for any target w/o render pipeline association, i.e. {@link GL#GL_ELEMENT_ARRAY_BUFFER},
   * intended for GPU buffer storage mapping, see {@link GLBufferStorage}, via {@link #mapStorage(GL, int)} and {@link #mapStorage(GL, long, long, int)}.
   * <p>
   * No index, name for a fixed function pipeline nor vertex attribute is given.
   * </p>
   *
   * @param compsPerElement component count per element
   * @param dataType The component's OpenGL data type
   * @param initialElementCount
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   * @param vboTarget {@link GL#GL_ELEMENT_ARRAY_BUFFER}, ..
   */
  public static GLArrayDataServer createDataMapped(final int compsPerElement, final int dataType, final int mappedElementCount,
                                                   final int vboUsage, final int vboTarget)
    throws GLException
  {
    final GLArrayDataServer ads = new GLArrayDataServer();
    final GLArrayHandler glArrayHandler = new GLDataArrayHandler(ads);
    ads.init(null, -1, compsPerElement, dataType, false, 0, null, 0 /* initialElementCount */, mappedElementCount, false,
             glArrayHandler, 0, 0, vboUsage, vboTarget, false);
    return ads;
  }

  /**
   * Create a VBO for fixed function interleaved array data
   * starting with a new created Buffer object with initialElementCount size.
   * <p>User needs to <i>configure</i> the interleaved segments via {@link #addFixedSubArray(int, int, int)}.</p>
   *
   * @param compsPerElement The total number of all interleaved components per element.
   * @param dataType The component's OpenGL data type
   * @param normalized Whether the data shall be normalized
   * @param initialElementCount The initial number of all interleaved elements
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createFixedInterleaved(final int compsPerElement, final int dataType, final boolean normalized, final int initialElementCount,
                                              final int vboUsage)
    throws GLException
  {
    final GLArrayDataServer ads = new GLArrayDataServer();
    final GLArrayHandler glArrayHandler = new GLArrayHandlerInterleaved(ads);
    ads.init(GLPointerFuncUtil.mgl_InterleaveArray, -1, compsPerElement, dataType, false, 0, null, initialElementCount, 0 /* mappedElementCount */, false,
             glArrayHandler, 0, 0, vboUsage, GL.GL_ARRAY_BUFFER, false);
    return ads;
  }

  /**
   * Create a VBO for fixed function interleaved array data
   * intended for GPU buffer storage mapping, see {@link GLBufferStorage}, via {@link #mapStorage(GL, int)} and {@link #mapStorage(GL, long, long, int)}.
   * <p>User needs to <i>configure</i> the interleaved segments via {@link #addFixedSubArray(int, int, int)}.</p>
   *
   * @param compsPerElement The total number of all interleaved components per element.
   * @param dataType The component's OpenGL data type
   * @param normalized Whether the data shall be normalized
   * @param mappedElementCount The total number of all interleaved elements
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createFixedInterleavedMapped(final int compsPerElement, final int dataType, final boolean normalized, final int mappedElementCount,
                                                               final int vboUsage)
    throws GLException
  {
    final GLArrayDataServer ads = new GLArrayDataServer();
    final GLArrayHandler glArrayHandler = new GLArrayHandlerInterleaved(ads);
    ads.init(GLPointerFuncUtil.mgl_InterleaveArray, -1, compsPerElement, dataType, false, 0, null, 0 /* initialElementCount */, mappedElementCount, false,
             glArrayHandler, 0, 0, vboUsage, GL.GL_ARRAY_BUFFER, false);
    ads.seal(true);
    return ads;
  }

  /**
   * Create a VBO for fixed function interleaved array data
   * starting with a given Buffer object incl it's stride
   * <p>User needs to <i>configure</i> the interleaved segments via {@link #addFixedSubArray(int, int, int)}.</p>
   *
   * @param compsPerElement The total number of all interleaved components per element.
   * @param dataType The component's OpenGL data type
   * @param normalized Whether the data shall be normalized
   * @param stride in bytes from one element of a sub-array to the other. If zero, compsPerElement * compSizeInBytes
   * @param buffer The user define data of all interleaved elements
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createFixedInterleaved(final int compsPerElement, final int dataType, final boolean normalized, final int stride, final Buffer buffer,
                                              final int vboUsage)
    throws GLException
  {
    final GLArrayDataServer ads = new GLArrayDataServer();
    final GLArrayHandler glArrayHandler = new GLArrayHandlerInterleaved(ads);
    ads.init(GLPointerFuncUtil.mgl_InterleaveArray, -1, compsPerElement, dataType, normalized, stride, buffer, buffer.limit(), 0 /* mappedElementCount */, false,
             glArrayHandler, 0, 0, vboUsage, GL.GL_ARRAY_BUFFER, false);
    return ads;
  }

  /**
   * Configure a segment of this fixed function interleaved array (see {@link #createFixedInterleaved(int, int, boolean, int, int)}).
   * <p>
   * This method may be called several times as long the sum of interleaved components does not
   * exceed the total component count of the created interleaved array.</p>
   * <p>
   * The memory of the the interleaved array is being used.</p>
   * <p>
   * Must be called before using the array, eg: {@link #seal(boolean)}, {@link #putf(float)}, .. </p>
   *
   * @param index The GL array index, maybe -1 if vboTarget is {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   * @param comps This interleaved array segment's component count per element
   * @param vboTarget {@link GL#GL_ARRAY_BUFFER} or {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   */
  public GLArrayData addFixedSubArray(final int index, final int comps, final int vboTarget) {
      if(interleavedOffset >= getComponentCount() * getComponentSizeInBytes()) {
          final int iOffC = interleavedOffset / getComponentSizeInBytes();
          throw new GLException("Interleaved offset > total components ("+iOffC+" > "+getComponentCount()+")");
      }
      if(usesGLSL) {
          throw new GLException("buffer uses GLSL");
      }
      final int subStrideB = ( 0 == getStride() ) ? getComponentCount() * getComponentSizeInBytes() : getStride();
      final GLArrayDataWrapper ad;
      if( 0 < mappedElementCount ) {
          ad = GLArrayDataWrapper.createFixed(
                  index, comps, getComponentType(),
                  getNormalized(), subStrideB, mappedElementCount,
                  getVBOName(), interleavedOffset, getVBOUsage(), vboTarget);
      } else {
          ad = GLArrayDataWrapper.createFixed(
                  index, comps, getComponentType(),
                  getNormalized(), subStrideB, getBuffer(),
                  getVBOName(), interleavedOffset, getVBOUsage(), vboTarget);
      }
      ad.setVBOEnabled(isVBO());
      interleavedOffset += comps * getComponentSizeInBytes();
      if(GL.GL_ARRAY_BUFFER == vboTarget) {
          glArrayHandler.addSubHandler(new GLFixedArrayHandlerFlat(ad));
      }
      return ad;
  }

  /**
   * Create a VBO for GLSL interleaved array data
   * starting with a new created Buffer object with initialElementCount size.
   * <p>User needs to <i>configure</i> the interleaved segments via {@link #addGLSLSubArray(int, int, int)}.</p>
   *
   * @param compsPerElement The total number of all interleaved components per element.
   * @param dataType The component's OpenGL data type
   * @param normalized Whether the data shall be normalized
   * @param initialElementCount The initial number of all interleaved elements
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createGLSLInterleaved(final int compsPerElement, final int dataType, final boolean normalized, final int initialElementCount,
                                                        final int vboUsage)
    throws GLException
  {
    final GLArrayDataServer ads = new GLArrayDataServer();
    final GLArrayHandler glArrayHandler = new GLSLArrayHandlerInterleaved(ads);
    ads.init(GLPointerFuncUtil.mgl_InterleaveArray, -1, compsPerElement, dataType, normalized, 0, null, initialElementCount, 0 /* mappedElementCount */, false,
             glArrayHandler, 0, 0, vboUsage, GL.GL_ARRAY_BUFFER, true);
    return ads;
  }

  /**
   * Create a VBO for GLSL interleaved array data
   * intended for GPU buffer storage mapping, see {@link GLBufferStorage}, via {@link #mapStorage(GL, int)} and {@link #mapStorage(GL, long, long, int)}.
   * <p>User needs to <i>configure</i> the interleaved segments via {@link #addGLSLSubArray(int, int, int)}.</p>
   *
   * @param compsPerElement The total number of all interleaved components per element.
   * @param dataType The component's OpenGL data type
   * @param normalized Whether the data shall be normalized
   * @param mappedElementCount The total number of all interleaved elements
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createGLSLInterleavedMapped(final int compsPerElement, final int dataType, final boolean normalized, final int mappedElementCount, final int vboUsage)
    throws GLException
  {
    final GLArrayDataServer ads = new GLArrayDataServer();
    final GLArrayHandler glArrayHandler = new GLSLArrayHandlerInterleaved(ads);
    ads.init(GLPointerFuncUtil.mgl_InterleaveArray, -1, compsPerElement, dataType, normalized, 0, null, 0 /* initialElementCount */, mappedElementCount, false,
             glArrayHandler, 0, 0, vboUsage, GL.GL_ARRAY_BUFFER, true);
    ads.seal(true);
    return ads;
  }

  /**
   * Create a VBO for GLSL interleaved array data
   * starting with a given Buffer object incl it's stride
   * <p>User needs to <i>configure</i> the interleaved segments via {@link #addGLSLSubArray(int, int, int)}.</p>
   *
   * @param compsPerElement The total number of all interleaved components per element.
   * @param dataType The component's OpenGL data type
   * @param normalized Whether the data shall be normalized
   * @param stride in bytes from one element of a sub-array to the other. If zero, compsPerElement * compSizeInBytes
   * @param buffer The user define data of all interleaved elements
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createGLSLInterleaved(final int compsPerElement, final int dataType, final boolean normalized, final int stride, final Buffer buffer,
                                                        final int vboUsage)
    throws GLException
  {
    final GLArrayDataServer ads = new GLArrayDataServer();
    final GLArrayHandler glArrayHandler = new GLSLArrayHandlerInterleaved(ads);
    ads.init(GLPointerFuncUtil.mgl_InterleaveArray, -1, compsPerElement, dataType, normalized, stride, buffer, buffer.limit(), 0 /* mappedElementCount */, false,
             glArrayHandler, 0, 0, vboUsage, GL.GL_ARRAY_BUFFER, true);
    return ads;
  }

  /**
   * Configure a segment of this GLSL interleaved array (see {@link #createGLSLInterleaved(int, int, boolean, int, int)}).
   * <p>
   * This method may be called several times as long the sum of interleaved components does not
   * exceed the total component count of the created interleaved array.</p>
   * <p>
   * The memory of the the interleaved array is being used.</p>
   * <p>
   * Must be called before using the array, eg: {@link #seal(boolean)}, {@link #putf(float)}, .. </p>
   * @param name  The custom name for the GL attribute, maybe null if vboTarget is {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   * @param comps This interleaved array segment's component count per element
   * @param vboTarget {@link GL#GL_ARRAY_BUFFER} or {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   */
  public GLArrayData addGLSLSubArray(final String name, final int comps, final int vboTarget) {
      if(interleavedOffset >= getComponentCount() * getComponentSizeInBytes()) {
          final int iOffC = interleavedOffset / getComponentSizeInBytes();
          throw new GLException("Interleaved offset > total components ("+iOffC+" > "+getComponentCount()+")");
      }
      if(!usesGLSL) {
          throw new GLException("buffer uses fixed function");
      }
      final int subStrideB = ( 0 == getStride() ) ? getComponentCount() * getComponentSizeInBytes() : getStride();
      final GLArrayDataWrapper ad;
      if( 0 < mappedElementCount ) {
          ad = GLArrayDataWrapper.createGLSL(
                  name, comps, getComponentType(),
                  getNormalized(), subStrideB, mappedElementCount,
                  getVBOName(), interleavedOffset, getVBOUsage(), vboTarget);
      } else {
          ad = GLArrayDataWrapper.createGLSL(
                  name, comps, getComponentType(),
                  getNormalized(), subStrideB, getBuffer(),
                  getVBOName(), interleavedOffset, getVBOUsage(), vboTarget);
      }
      ad.setVBOEnabled(isVBO());
      interleavedOffset += comps * getComponentSizeInBytes();
      if(GL.GL_ARRAY_BUFFER == vboTarget) {
          glArrayHandler.addSubHandler(new GLSLArrayHandlerFlat(ad));
      }
      return ad;
  }

  public final void setInterleavedOffset(final int interleavedOffset) {
    this.interleavedOffset = interleavedOffset;
  }

  public final int getInterleavedOffset() {
    return interleavedOffset;
  }

  //
  // Data matters GLArrayData
  //

  //
  // Data and GL state modification ..
  //

  @Override
  public void destroy(final GL gl) {
    // super.destroy(gl):
    // - GLArrayDataClient.destroy(gl): disables & clears client-side buffer
    //   - GLArrayDataWrapper.destroy(gl) (clears all values 'vboName' ..)
    final int _vboName = vboName;
    super.destroy(gl);
    if(_vboName!=0) {
        final int[] tmp = new int[] { _vboName } ;
        gl.glDeleteBuffers(1, tmp, 0);
        vboName = 0;
    }
  }

  //
  // data matters
  //

  /**
   * Convenient way do disable the VBO behavior and
   * switch to client side data one
   * Only possible if buffer is defined.
   */
  @Override
  public void setVBOEnabled(final boolean vboUsage) {
    checkSeal(false);
    super.setVBOEnabled(vboUsage);
  }

  public GLBufferStorage mapStorage(final GL gl, final int access) {
      if( null != this.getBuffer() ) {
          throw new IllegalStateException("user buffer not null");
      }
      if( null != mappedStorage ) {
          throw new IllegalStateException("already mapped: "+mappedStorage);
      }
      checkSeal(true);
      bindBuffer(gl, true);
      gl.glBufferData(getVBOTarget(), getSizeInBytes(), null, getVBOUsage());
      final GLBufferStorage storage = gl.mapBuffer(getVBOTarget(), access);
      setMappedBuffer(storage);
      bindBuffer(gl, false);
      seal(false);
      rewind();
      return storage;
  }
  public GLBufferStorage mapStorage(final GL gl, final long offset, final long length, final int access) {
      if( null != this.getBuffer() ) {
          throw new IllegalStateException("user buffer not null");
      }
      if( null != mappedStorage ) {
          throw new IllegalStateException("already mapped: "+mappedStorage);
      }
      checkSeal(true);
      bindBuffer(gl, true);
      gl.glBufferData(getVBOTarget(), getSizeInBytes(), null, getVBOUsage());
      final GLBufferStorage storage = gl.mapBufferRange(getVBOTarget(), offset, length, access);
      setMappedBuffer(storage);
      bindBuffer(gl, false);
      seal(false);
      rewind();
      return storage;
  }
  private final void setMappedBuffer(final GLBufferStorage storage) {
      mappedStorage = storage;
      final ByteBuffer bb = storage.getMappedBuffer();
      if(componentClazz==ByteBuffer.class) {
          buffer = bb;
      } else if(componentClazz==ShortBuffer.class) {
          buffer = bb.asShortBuffer();
      } else if(componentClazz==IntBuffer.class) {
          buffer = bb.asIntBuffer();
      } else if(componentClazz==FloatBuffer.class) {
          buffer = bb.asFloatBuffer();
      } else {
          throw new GLException("Given Buffer Class not supported: "+componentClazz+":\n\t"+this);
      }
  }

  public void unmapStorage(final GL gl) {
      if( null == mappedStorage ) {
          throw new IllegalStateException("not mapped");
      }
      mappedStorage = null;
      buffer = null;
      seal(true);
      bindBuffer(gl, true);
      gl.glUnmapBuffer(getVBOTarget());
      bindBuffer(gl, false);
  }

  @Override
  public String toString() {
    return "GLArrayDataServer["+name+
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
                       ", initialElementCount "+initialElementCount+
                       ", mappedElementCount "+mappedElementCount+
                       ", mappedStorage "+mappedStorage+
                       ", vboEnabled "+vboEnabled+
                       ", vboName "+vboName+
                       ", vboUsage 0x"+Integer.toHexString(vboUsage)+
                       ", vboTarget 0x"+Integer.toHexString(vboTarget)+
                       ", vboOffset "+vboOffset+
                       ", sealed "+sealed+
                       ", bufferEnabled "+bufferEnabled+
                       ", bufferWritten "+bufferWritten+
                       ", buffer "+buffer+
                       ", alive "+alive+
                       "]";
  }

  //
  // non public matters ..
  //

  @Override
  protected void init(final String name, final int index, final int comps, final int dataType, final boolean normalized,
                      final int stride, final Buffer data, final int initialElementCount, final int mappedElementCount,
                      final boolean isVertexAttribute,
                      final GLArrayHandler glArrayHandler, final int vboName, final long vboOffset, final int vboUsage, final int vboTarget, final boolean usesGLSL)
    throws GLException
  {
    super.init(name, index, comps, dataType, normalized, stride, data, initialElementCount, mappedElementCount, isVertexAttribute,
               glArrayHandler, vboName, vboOffset, vboUsage, vboTarget, usesGLSL);

    vboEnabled=true;
  }

  @Override
  protected void init_vbo(final GL gl) {
    super.init_vbo(gl);
    if(vboEnabled && vboName==0) {
        final int[] tmp = new int[1];
        gl.glGenBuffers(1, tmp, 0);
        vboName = tmp[0];
        if(0 < interleavedOffset) {
            glArrayHandler.setSubArrayVBOName(vboName);
        }
    }
  }

  protected GLArrayDataServer() { }

  /**
   * Copy Constructor
   * <p>
   * Buffer is {@link Buffers#slice(Buffer) sliced}, i.e. sharing content but using own state.
   * </p>
   * <p>
   * All other values are simply copied.
   * </p>
   */
  public GLArrayDataServer(final GLArrayDataServer src) {
    super(src);
    this.interleavedOffset = src.interleavedOffset;
    this.mappedStorage = src.mappedStorage;
  }

  private int interleavedOffset = 0;
  private GLBufferStorage mappedStorage = null;
}


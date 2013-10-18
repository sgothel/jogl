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

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLArrayData;
import javax.media.opengl.GLException;
import javax.media.opengl.fixedfunc.GLPointerFuncUtil;

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
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param stride
   * @param buffer the user define data
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataServer createFixed(int index, int comps, int dataType, boolean normalized, int stride,
                                              Buffer buffer, int vboUsage)
    throws GLException
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLFixedArrayHandler(ads);
    ads.init(null, index, comps, dataType, normalized, stride, buffer, buffer.limit(), false, glArrayHandler,
             0, 0, vboUsage, GL.GL_ARRAY_BUFFER, false);
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
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param initialElementCount
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataServer createFixed(int index, int comps, int dataType, boolean normalized, int initialElementCount,
                                              int vboUsage)
    throws GLException
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLFixedArrayHandler(ads);
    ads.init(null, index, comps, dataType, normalized, 0, null, initialElementCount, false, glArrayHandler,
             0, 0, vboUsage, GL.GL_ARRAY_BUFFER, false);
    return ads;
  }

  /**
   * Create a VBO, using a custom GLSL array attribute name
   * and starting with a new created Buffer object with initialElementCount size
   * @param name  The custom name for the GL attribute
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param initialElementCount
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createGLSL(String name, int comps,
                                             int dataType, boolean normalized, int initialElementCount, int vboUsage)
    throws GLException
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLSLArrayHandler(ads);
    ads.init(name, -1, comps, dataType, normalized, 0, null, initialElementCount,
             true, glArrayHandler, 0, 0, vboUsage, GL.GL_ARRAY_BUFFER, true);
    return ads;
  }

  /**
   * Create a VBO, using a custom GLSL array attribute name
   * and starting with a given Buffer object incl it's stride
   * @param name  The custom name for the GL attribute
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param stride
   * @param buffer the user define data
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createGLSL(String name, int comps,
                                             int dataType, boolean normalized, int stride, Buffer buffer,
                                             int vboUsage)
    throws GLException
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLSLArrayHandler(ads);
    ads.init(name, -1, comps, dataType, normalized, stride, buffer, buffer.limit(), true, glArrayHandler,
             0, 0, vboUsage, GL.GL_ARRAY_BUFFER, true);
    return ads;
  }

  /**
   * Create a VBO data object for any target w/o render pipeline association, ie {@link GL#GL_ELEMENT_ARRAY_BUFFER}.
   *
   * Hence no index, name for a fixed function pipeline nor vertex attribute is given.
   *
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param stride
   * @param buffer the user define data
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   * @param vboTarget {@link GL#GL_ELEMENT_ARRAY_BUFFER}, ..
   * {@link GL#glGenBuffers(int, int[], int)
   */
  public static GLArrayDataServer createData(int comps, int dataType, int stride,
                                             Buffer buffer, int vboUsage, int vboTarget)
    throws GLException
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLDataArrayHandler(ads);
    ads.init(null, -1, comps, dataType, false, stride, buffer, buffer.limit(), false, glArrayHandler,
             0, 0, vboUsage, vboTarget, false);
    return ads;
  }

  /**
   * Create a VBO data object for any target w/o render pipeline association, ie {@link GL#GL_ELEMENT_ARRAY_BUFFER}.
   *
   * Hence no index, name for a fixed function pipeline nor vertex attribute is given.
   *
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param initialElementCount
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   * @param vboTarget {@link GL#GL_ELEMENT_ARRAY_BUFFER}, ..
   */
  public static GLArrayDataServer createData(int comps, int dataType, int initialElementCount,
                                             int vboUsage, int vboTarget)
    throws GLException
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLDataArrayHandler(ads);
    ads.init(null, -1, comps, dataType, false, 0, null, initialElementCount, false, glArrayHandler,
             0, 0, vboUsage, vboTarget, false);
    return ads;
  }


  /**
   * Create a VBO for fixed function interleaved array data
   * starting with a new created Buffer object with initialElementCount size.
   * <p>User needs to <i>configure</i> the interleaved segments via {@link #addFixedSubArray(int, int, int)}.</p>
   *
   * @param comps The total number of all interleaved components.
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param initialElementCount
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createFixedInterleaved(int comps, int dataType, boolean normalized, int initialElementCount,
                                              int vboUsage)
    throws GLException
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLArrayHandlerInterleaved(ads);
    ads.init(GLPointerFuncUtil.mgl_InterleaveArray, -1, comps, dataType, false, 0, null, initialElementCount, false, glArrayHandler,
             0, 0, vboUsage, GL.GL_ARRAY_BUFFER, false);
    return ads;
  }

  /**
   * Configure a segment of this fixed function interleaved array (see {@link #createFixedInterleaved(int, int, boolean, int, int)}).
   * <p>
   * This method may be called several times as long the sum of interleaved components does not
   * exceed the total number of components of the created interleaved array.</p>
   * <p>
   * The memory of the the interleaved array is being used.</p>
   * <p>
   * Must be called before using the array, eg: {@link #seal(boolean)}, {@link #putf(float)}, .. </p>
   *
   * @param index The GL array index, maybe -1 if vboTarget is {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   * @param comps This interleaved array segment's component number
   * @param vboTarget {@link GL#GL_ARRAY_BUFFER} or {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   */
  public GLArrayData addFixedSubArray(int index, int comps, int vboTarget) {
      if(interleavedOffset >= getComponentCount() * getComponentSizeInBytes()) {
          final int iOffC = interleavedOffset / getComponentSizeInBytes();
          throw new GLException("Interleaved offset > total components ("+iOffC+" > "+getComponentCount()+")");
      }
      if(usesGLSL) {
          throw new GLException("buffer uses GLSL");
      }
      final GLArrayDataWrapper ad = GLArrayDataWrapper.createFixed(
              index, comps, getComponentType(),
              getNormalized(), getStride(), getBuffer(),
              getVBOName(), interleavedOffset, getVBOUsage(), vboTarget);
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
   * @param comps The total number of all interleaved components.
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param initialElementCount
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createGLSLInterleaved(int comps, int dataType, boolean normalized, int initialElementCount,
                                              int vboUsage)
    throws GLException
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLSLArrayHandlerInterleaved(ads);
    ads.init(GLPointerFuncUtil.mgl_InterleaveArray, -1, comps, dataType, false, 0, null, initialElementCount, false, glArrayHandler,
             0, 0, vboUsage, GL.GL_ARRAY_BUFFER, true);
    return ads;
  }

  /**
   * Configure a segment of this GLSL interleaved array (see {@link #createGLSLInterleaved(int, int, boolean, int, int)}).
   * <p>
   * This method may be called several times as long the sum of interleaved components does not
   * exceed the total number of components of the created interleaved array.</p>
   * <p>
   * The memory of the the interleaved array is being used.</p>
   * <p>
   * Must be called before using the array, eg: {@link #seal(boolean)}, {@link #putf(float)}, .. </p>
   * @param name  The custom name for the GL attribute, maybe null if vboTarget is {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   * @param comps This interleaved array segment's component number
   * @param vboTarget {@link GL#GL_ARRAY_BUFFER} or {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   */
  public GLArrayData addGLSLSubArray(String name, int comps, int vboTarget) {
      if(interleavedOffset >= getComponentCount() * getComponentSizeInBytes()) {
          final int iOffC = interleavedOffset / getComponentSizeInBytes();
          throw new GLException("Interleaved offset > total components ("+iOffC+" > "+getComponentCount()+")");
      }
      if(!usesGLSL) {
          throw new GLException("buffer uses fixed function");
      }
      final GLArrayDataWrapper ad = GLArrayDataWrapper.createGLSL(
              name, comps, getComponentType(),
              getNormalized(), getStride(), getBuffer(),
              getVBOName(), interleavedOffset, getVBOUsage(), vboTarget);
      ad.setVBOEnabled(isVBO());
      interleavedOffset += comps * getComponentSizeInBytes();
      if(GL.GL_ARRAY_BUFFER == vboTarget) {
          glArrayHandler.addSubHandler(new GLSLArrayHandlerFlat(ad));
      }
      return ad;
  }

  //
  // Data matters GLArrayData
  //

  //
  // Data and GL state modification ..
  //

  @Override
  public void destroy(GL gl) {
    // super.destroy(gl):
    // - GLArrayDataClient.destroy(gl): disables & clears client-side buffer
    //   - GLArrayDataWrapper.destroy(gl) (clears all values 'vboName' ..)
    int _vboName = vboName;
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
  public void    setVBOEnabled(boolean vboUsage) {
    checkSeal(false);
    super.setVBOEnabled(vboUsage);
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
                       ", components "+components+
                       ", stride "+strideB+"b "+strideL+"c"+
                       ", initialElementCount "+initialElementCount+
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
  protected void init(String name, int index, int comps, int dataType, boolean normalized,
                      int stride, Buffer data, int initialElementCount, boolean isVertexAttribute,
                      GLArrayHandler glArrayHandler,
                      int vboName, long vboOffset, int vboUsage, int vboTarget, boolean usesGLSL)
    throws GLException
  {
    super.init(name, index, comps, dataType, normalized, stride, data, initialElementCount, isVertexAttribute, glArrayHandler,
               vboName, vboOffset, vboUsage, vboTarget, usesGLSL);

    vboEnabled=true;
  }

  @Override
  protected void init_vbo(GL gl) {
    super.init_vbo(gl);
    if(vboEnabled && vboName==0) {
        int[] tmp = new int[1];
        gl.glGenBuffers(1, tmp, 0);
        vboName = tmp[0];
        if(0 < interleavedOffset) {
            glArrayHandler.setSubArrayVBOName(vboName);
        }
    }
  }

  private int interleavedOffset = 0;
}


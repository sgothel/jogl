
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
   * and starting with a new created Buffer object with initialSize size
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
   * @param initialSize
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataServer createFixed(int index, int comps, int dataType, boolean normalized, int initialSize, 
                                              int vboUsage)
    throws GLException
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLFixedArrayHandler(ads);
    ads.init(null, index, comps, dataType, normalized, 0, null, initialSize, false, glArrayHandler,
             0, 0, vboUsage, GL.GL_ARRAY_BUFFER, false);
    return ads;
  }

  /**
   * Create a VBO, using a custom GLSL array attribute name
   * and starting with a new created Buffer object with initialSize size
   * @param name  The custom name for the GL attribute    
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param initialSize
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createGLSL(String name, int comps,
                                             int dataType, boolean normalized, int initialSize, int vboUsage) 
    throws GLException 
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLSLArrayHandler(ads);
    ads.init(name, -1, comps, dataType, normalized, 0, null, initialSize,
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
   * @param initialSize
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   * @param vboTarget {@link GL#GL_ELEMENT_ARRAY_BUFFER}, ..
   */
  public static GLArrayDataServer createData(int comps, int dataType, int initialSize, 
                                             int vboUsage, int vboTarget)
    throws GLException
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLDataArrayHandler(ads);
    ads.init(null, -1, comps, dataType, false, 0, null, initialSize, false, glArrayHandler,
             0, 0, vboUsage, vboTarget, false);
    return ads;
  }

  
  /**
   * Create a VBO for fixed function interleaved array data
   * starting with a new created Buffer object with initialSize size.
   * <p>User needs to <i>configure</i> the interleaved segments via {@link #addFixedSubArray(int, int, int)}.</p>
   * 
   * @param comps The total number of all interleaved components.
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param initialSize 
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createFixedInterleaved(int comps, int dataType, boolean normalized, int initialSize, 
                                              int vboUsage)
    throws GLException
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLArrayHandlerInterleaved(ads);
    ads.init(GLPointerFuncUtil.mgl_InterleaveArray, -1, comps, dataType, false, 0, null, initialSize, false, glArrayHandler,
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
   * starting with a new created Buffer object with initialSize size.
   * <p>User needs to <i>configure</i> the interleaved segments via {@link #addGLSLSubArray(int, int, int)}.</p>
   * 
   * @param comps The total number of all interleaved components.
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param initialSize 
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createGLSLInterleaved(int comps, int dataType, boolean normalized, int initialSize, 
                                              int vboUsage)
    throws GLException
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLSLArrayHandlerInterleaved(ads);
    ads.init(GLPointerFuncUtil.mgl_InterleaveArray, -1, comps, dataType, false, 0, null, initialSize, false, glArrayHandler,
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

  public void destroy(GL gl) {
    super.destroy(gl);
    if(vboName!=0) {
        int[] tmp = new int[1];
        tmp[0] = vboName;
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
  public void    setVBOEnabled(boolean vboUsage) { 
    checkSeal(false);
    super.setVBOEnabled(vboUsage);
  }

  public String toString() {
    return "GLArrayDataServer["+name+
                       ", index "+index+
                       ", location "+location+
                       ", isVertexAttribute "+isVertexAttribute+
                       ", dataType "+componentType+ 
                       ", bufferClazz "+componentClazz+ 
                       ", elements "+getElementCount()+
                       ", components "+components+ 
                       ", stride "+strideB+"b "+strideL+"c"+
                       ", initialSize "+initialSize+
                       ", vboEnabled "+vboEnabled+ 
                       ", vboName "+vboName+ 
                       ", vboUsage 0x"+Integer.toHexString(vboUsage)+ 
                       ", vboTarget 0x"+Integer.toHexString(vboTarget)+ 
                       ", vboOffset 0x"+Long.toHexString(vboOffset)+                        
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

  protected void init(String name, int index, int comps, int dataType, boolean normalized, 
                      int stride, Buffer data, int initialSize, boolean isVertexAttribute,
                      GLArrayHandler glArrayHandler,
                      int vboName, long vboOffset, int vboUsage, int vboTarget, boolean usesGLSL)
    throws GLException
  {
    super.init(name, index, comps, dataType, normalized, stride, data, initialSize, isVertexAttribute, glArrayHandler,
               vboName, vboOffset, vboUsage, vboTarget, usesGLSL);

    vboEnabled=true;
  }

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


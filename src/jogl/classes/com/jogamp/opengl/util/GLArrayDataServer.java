
package com.jogamp.opengl.util;

import javax.media.opengl.*;

import java.nio.*;

import com.jogamp.opengl.util.glsl.*;

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
   * @param index The GL array index
   * @param name  The optional custom name for the GL array index, maybe null.
   *              If null, the default name mapping will be used, see 'getPredefinedArrayIndexName(int)'.
   *              This name might be used as the shader attribute name.
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param stride
   * @param buffer the user define data
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataServer createFixed(int index, String name, int comps, int dataType, boolean normalized, int stride,
                                              Buffer buffer, int vboUsage)
    throws GLException
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLFixedArrayHandler(ads);
    ads.init(name, index, comps, dataType, normalized, stride, buffer, buffer.limit(), false, glArrayHandler,
             0, 0, vboUsage, GL.GL_ARRAY_BUFFER);
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
   * @param index The GL array index
   * @param name  The optional custom name for the GL array index, maybe null.
   *              If null, the default name mapping will be used, see 'getPredefinedArrayIndexName(int)'.
   *              This name might be used as the shader attribute name.
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param initialSize
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataServer createFixed(int index, String name, int comps, int dataType, boolean normalized, int initialSize, 
                                              int vboUsage)
    throws GLException
  {
      GLArrayDataServer ads = new GLArrayDataServer();
      GLArrayHandler glArrayHandler = new GLFixedArrayHandler(ads);
      ads.init(name, index, comps, dataType, normalized, 0, null, initialSize, false, glArrayHandler,
               0, 0, vboUsage, GL.GL_ARRAY_BUFFER);
    return ads;
  }

  /**
   * Create a VBO, using a custom GLSL array attribute name
   * and starting with a new created Buffer object with initialSize size
   * 
   * @param st The ShaderState managing the state of the used shader program, vertex attributes and uniforms
   * @param name  The custom name for the GL attribute, maybe null if gpuBufferTarget is {@link GL#GL_ELEMENT_ARRAY_BUFFER}    
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param initialSize
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createGLSL(ShaderState st, String name,
                                             int comps, int dataType, boolean normalized, int initialSize, 
                                             int vboUsage) 
    throws GLException 
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLSLArrayHandler(st, ads);
    ads.init(name, -1, comps, dataType, normalized, 0, null, initialSize,
             true, glArrayHandler, 0, 0, vboUsage, GL.GL_ARRAY_BUFFER);
    return ads;
  }  
  
  /**
   * Create a VBO, using a custom GLSL array attribute name
   * and starting with a given Buffer object incl it's stride
   * 
   * @param st The ShaderState managing the state of the used shader program, vertex attributes and uniforms
   * @param name  The custom name for the GL attribute, maybe null if gpuBufferTarget is     
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param stride
   * @param buffer the user define data
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   */
  public static GLArrayDataServer createGLSL(ShaderState st, String name,
                                             int comps, int dataType, boolean normalized, int stride,
                                             Buffer buffer, int vboUsage) 
    throws GLException
  {
    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLSLArrayHandler(st, ads);
    ads.init(name, -1, comps, dataType, normalized, stride, buffer, buffer.limit(), true, glArrayHandler,
             0, 0, vboUsage, GL.GL_ARRAY_BUFFER);
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
             0, 0, vboUsage, vboTarget);
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
             0, 0, vboUsage, vboTarget);
    return ads;
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
                       ", elements "+getElementNumber()+
                       ", components "+components+ 
                       ", stride "+stride+"u "+strideB+"b "+strideL+"c"+
                       ", initialSize "+initialSize+ 
                       ", vboUsage 0x"+Integer.toHexString(vboUsage)+ 
                       ", vboTarget 0x"+Integer.toHexString(vboTarget)+ 
                       ", vboEnabled "+vboEnabled+ 
                       ", vboName "+vboName+ 
                       ", sealed "+sealed+ 
                       ", bufferEnabled "+bufferEnabled+ 
                       ", bufferWritten "+bufferWritten+ 
                       ", buffer "+buffer+ 
                       ", offset "+vboOffset+
                       ", alive "+alive+                       
                       "]";
  }

  //
  // non public matters ..
  //

  protected void init(String name, int index, int comps, int dataType, boolean normalized, 
                      int stride, Buffer data, int initialSize, boolean isVertexAttribute,
                      GLArrayHandler glArrayHandler,
                      int vboName, long vboOffset, int vboUsage, int vboTarget)
    throws GLException
  {
    super.init(name, index, comps, dataType, normalized, stride, data, initialSize, isVertexAttribute, glArrayHandler,
               vboName, vboOffset, vboUsage, vboTarget);

    vboEnabled=true;
  }

  protected void init_vbo(GL gl) {
    super.init_vbo(gl);
    if(vboEnabled && vboName==0) {
        int[] tmp = new int[1];
        gl.glGenBuffers(1, tmp, 0);
        vboName = tmp[0];
    }
  }
}



package com.sun.opengl.util;

import javax.media.opengl.*;
import java.nio.*;

import com.sun.opengl.util.glsl.*;

public class GLArrayDataServer extends GLArrayDataClient implements GLArrayDataEditable {

  //
  // lifetime matters
  //

  /**
   * Create a VBOBuffer object, using a predefined fixed function array index
   * and starting with a given Buffer object incl it's stride
   *
   * On profiles GL2 and ES1 the fixed function pipeline behavior is as expected.
   * On profile ES2 the fixed function emulation will transform these calls to 
   * EnableVertexAttribArray and VertexAttribPointer calls,
   * and a predefined vertex attribute variable name will be choosen.
   *
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
  public static GLArrayDataServer createFixed(GL gl, int index, String name, int comps, int dataType, boolean normalized,
                                              int stride, Buffer buffer, int vboBufferUsage)
    throws GLException
  {
    gl.getGLProfile().isValidArrayDataType(index, comps, dataType, false, true);

    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLFixedArrayHandler(ads);
    ads.init(gl, name, index, comps, dataType, normalized, stride, buffer, buffer.limit(), false, glArrayHandler,
             0, 0, vboBufferUsage);
    return ads;
  }

  /**
   * Create a VBOBuffer object, using a predefined fixed function array index
   * and starting with a new created Buffer object with initialSize size
   *
   * On profiles GL2 and ES1 the fixed function pipeline behavior is as expected.
   * On profile ES2 the fixed function emulation will transform these calls to 
   * EnableVertexAttribArray and VertexAttribPointer calls,
   * and a predefined vertex attribute variable name will be choosen.
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataServer createFixed(GL gl, int index, String name, int comps, int dataType, boolean normalized, 
                                              int initialSize, int vboBufferUsage)
    throws GLException
  {
    gl.getGLProfile().isValidArrayDataType(index, comps, dataType, false, true);

    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLFixedArrayHandler(ads);
    ads.init(gl, name, index, comps, dataType, normalized, 0, null, initialSize, false, glArrayHandler,
             0, 0, vboBufferUsage);
    return ads;
  }

  /**
   * Create a VBOBuffer object, using a custom GLSL array attribute name
   * and starting with a new created Buffer object with initialSize size
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataServer createGLSL(GL gl, String name, int comps, int dataType, boolean normalized,
                                             int initialSize, int vboBufferUsage) 
    throws GLException
  {
    if(!gl.hasGLSL()) {
        throw new GLException("GLArrayDataServer.GLSL not supported: "+gl);
    }
    gl.getGLProfile().isValidArrayDataType(-1, comps, dataType, true, true);

    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLSLArrayHandler(ads);
    ads.init(gl, name, -1, comps, dataType, normalized, 0, null, initialSize, true, glArrayHandler,
             0, 0, vboBufferUsage);
    return ads;
  }

  /**
   * Create a VBOBuffer object, using a custom GLSL array attribute name
   * and starting with a given Buffer object incl it's stride
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataServer createGLSL(GL gl, String name, int comps, int dataType, boolean normalized,
                                             int stride, Buffer buffer, int vboBufferUsage) 
    throws GLException
  {
    if(!gl.hasGLSL()) {
        throw new GLException("GLArrayDataServer.GLSL not supported: "+gl);
    }
    gl.getGLProfile().isValidArrayDataType(-1, comps, dataType, true, true);

    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLSLArrayHandler(ads);
    ads.init(gl, name, -1, comps, dataType, normalized, stride, buffer, buffer.limit(), true, glArrayHandler,
             0, 0, vboBufferUsage);
    return ads;
  }

  //
  // Data matters GLArrayData
  //

  public int getBufferUsage() { return vboBufferUsage; }

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
  public void    setVBOUsage(boolean vboUsage) { 
    checkSeal(false);
    super.setVBOUsage(vboUsage);
  }

  public String toString() {
    return "GLArrayDataServer["+name+
                       ", index "+index+
                       ", location "+location+
                       ", isVertexAttribute "+isVertexAttribute+
                       ", dataType "+dataType+ 
                       ", bufferClazz "+clazz+ 
                       ", elements "+getElementNumber()+
                       ", components "+components+ 
                       ", stride "+stride+"u "+strideB+"b "+strideL+"c"+
                       ", initialSize "+initialSize+ 
                       ", vboBufferUsage "+vboBufferUsage+ 
                       ", vboUsage "+vboUsage+ 
                       ", vboName "+vboName+ 
                       ", sealed "+sealed+ 
                       ", bufferEnabled "+bufferEnabled+ 
                       ", bufferWritten "+bufferWritten+ 
                       ", buffer "+buffer+ 
                       ", offset "+bufferOffset+ 
                       "]";
  }

  //
  // non public matters ..
  //

  protected void init(GL gl, String name, int index, int comps, int dataType, boolean normalized, 
                      int stride, Buffer data, int initialSize, boolean isVertexAttribute,
                      GLArrayHandler glArrayHandler,
                      int vboName, long bufferOffset, int vboBufferUsage)
    throws GLException
  {
    super.init(name, index, comps, dataType, normalized, stride, data, initialSize, isVertexAttribute, glArrayHandler,
               vboName, bufferOffset);

    vboUsage=true;

    if( ! (gl.isGL2ES2() && vboBufferUsage==GL2ES2.GL_STREAM_DRAW) ) {
        switch(vboBufferUsage) {
            case -1: // nop
            case GL.GL_STATIC_DRAW:
            case GL.GL_DYNAMIC_DRAW:
                break;
            default:
                throw new GLException("invalid vboBufferUsage: "+vboBufferUsage+":\n\t"+this); 
        }
    }
    this.vboBufferUsage=vboBufferUsage;
  }

  protected void init_vbo(GL gl) {
    if(vboUsage && vboName==0) {
        int[] tmp = new int[1];
        gl.glGenBuffers(1, tmp, 0);
        vboName = tmp[0];
    }
  }

  protected int vboBufferUsage;
}


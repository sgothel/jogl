
package javax.media.opengl;

import javax.media.opengl.*;
import java.nio.*;
import com.sun.opengl.impl.*;
import com.sun.opengl.impl.glsl.*;

public class GLArrayDataServer extends GLArrayDataClient implements GLArrayData {

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
   * @arg index The GL array index
   * @arg name  The optional custom name for the GL array index, maybe null.
   *            If null, the default name mapping will be used, see 'getPredefinedArrayIndexName(int)'.
   *            This name might be used as the shader attribute name.
   * @arg comps The array component number
   * @arg dataType The array index GL data type
   * @arg normalized Wheather the data shall be normalized
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataServer createFixed(int index, String name, int comps, int dataType, boolean normalized,
                                              int stride, Buffer buffer, int glBufferUsage)
    throws GLException
  {
    GLProfile.isValidateArrayDataType(index, comps, dataType, false, true);

    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLFixedArrayHandler(ads);
    ads.init(name, index, comps, dataType, normalized, stride, buffer, 0, buffer.limit(), glBufferUsage, false, glArrayHandler);
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
  public static GLArrayDataServer createFixed(int index, String name, int comps, int dataType, boolean normalized, 
                                              int initialSize, int glBufferUsage)
    throws GLException
  {
    GLProfile.isValidateArrayDataType(index, comps, dataType, false, true);

    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLFixedArrayHandler(ads);
    ads.init(name, index, comps, dataType, normalized, 0, null, 0, initialSize, glBufferUsage, false, glArrayHandler);
    return ads;
  }

  /**
   * Create a VBOBuffer object, using a predefined fixed function array index
   * and starting with a given Buffer offset incl it's stride
   *
   * The object will be created in a sealed state, 
   * where the data has been written (previously).
   *
   * This object can be enabled, but since no knowledge of the orginal client data is available,
   * we cannot send it down again.
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataServer createFixed(int index, String name, int comps, int dataType, boolean normalized,
                                              int stride, long bufferOffset, int vboName)
    throws GLException
  {
    GLProfile.isValidateArrayDataType(index, comps, dataType, false, true);

    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLSLArrayHandler(ads);
    ads.init(name, index, comps, dataType, normalized, stride, null, bufferOffset, 0, -1, false, glArrayHandler);
    ads.vboName = vboName;
    ads.bufferWritten = true;
    ads.sealed = true;
    return ads;
  }

  /**
   * Create a VBOBuffer object, using a custom GLSL array attribute name
   * and starting with a new created Buffer object with initialSize size
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataServer createGLSL(String name, int comps, int dataType, boolean normalized,
                                             int initialSize, int glBufferUsage) 
    throws GLException
  {
    if(!GLProfile.isGL2ES2()) {
        throw new GLException("GLArrayDataServer not supported for profile: "+GLProfile.getProfile());
    }

    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLSLArrayHandler(ads);
    ads.init(name, -1, comps, dataType, normalized, 0, null, 0, initialSize, glBufferUsage, true, glArrayHandler);
    return ads;
  }

  /**
   * Create a VBOBuffer object, using a custom GLSL array attribute name
   * and starting with a given Buffer object incl it's stride
   *
   * @see javax.media.opengl.GLContext#getPredefinedArrayIndexName(int)
   */
  public static GLArrayDataServer createGLSL(String name, int comps, int dataType, boolean normalized,
                                             int stride, Buffer buffer, int glBufferUsage) 
    throws GLException
  {
    if(!GLProfile.isGL2ES2()) {
        throw new GLException("GLArrayDataServer not supported for profile: "+GLProfile.getProfile());
    }

    GLArrayDataServer ads = new GLArrayDataServer();
    GLArrayHandler glArrayHandler = new GLSLArrayHandler(ads);
    ads.init(name, -1, comps, dataType, normalized, stride, buffer, 0, buffer.limit(), glBufferUsage, true, glArrayHandler);
    return ads;
  }

  //
  // Data matters GLArrayData
  //

  public final long getOffset() { return vboUsage?bufferOffset:-1; }

  public final int getVBOName() { return vboUsage?vboName:-1; }

  public final boolean isVBO() { return vboUsage; }

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
    this.vboUsage=(null!=buffer)?vboUsage:true; 
  }

  public int getBufferUsage() {
    return glBufferUsage;
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
                       ", glBufferUsage "+glBufferUsage+ 
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

  protected void init(String name, int index, int comps, int dataType, boolean normalized, 
                      int stride, Buffer data, long offset, int initialSize, int glBufferUsage, boolean isVertexAttribute,
                      GLArrayHandler glArrayHandler)
    throws GLException
  {
    super.init(name, index, comps, dataType, normalized, stride, data, initialSize, isVertexAttribute, glArrayHandler);

    vboUsage=true;

    if( ! (GLProfile.isGL2ES2() && glBufferUsage==GL2ES2.GL_STREAM_DRAW) ) {
        switch(glBufferUsage) {
            case -1: // nop
            case GL2ES1.GL_STATIC_DRAW:
            case GL2ES1.GL_DYNAMIC_DRAW:
                break;
            default:
                throw new GLException("invalid glBufferUsage: "+glBufferUsage+":\n\t"+this); 
        }
    }
    this.bufferOffset=offset;
    this.glBufferUsage = glBufferUsage;
    this.vboName = 0;
  }

  protected void init_vbo(GL gl) {
    if(vboUsage && vboName==0) {
        int[] tmp = new int[1];
        gl.glGenBuffers(1, tmp, 0);
        vboName = tmp[0];
    }
  }

  protected long bufferOffset;
  protected int glBufferUsage;
  protected int vboName;
  protected boolean vboUsage;

}


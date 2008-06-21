
package javax.media.opengl.util.gl2es1;

import javax.media.opengl.util.VBOBufferDraw;
import javax.media.opengl.*;
import java.nio.*;

public class VBOBufferDrawGL2ES1 extends VBOBufferDraw {

  public VBOBufferDrawGL2ES1(int glArrayType, int glDataType, int glBufferUsage, int comps, int initialSize) {
    init(glArrayType, glDataType, glBufferUsage, comps, initialSize);
  }

  protected void enableBufferGLImpl(GL _gl, boolean newData) {
    GL2ES1 gl = _gl.getGL2ES1();
    if(!bufferEnabled && null!=buffer) {
        gl.glEnableClientState(glArrayType);
        gl.glBindBuffer(GL2ES1.GL_ARRAY_BUFFER, vboName);
        if(newData) {
            gl.glBufferData(GL2ES1.GL_ARRAY_BUFFER, buffer.limit() * getBufferCompSize(), buffer, glBufferUsage);
        }
        switch(glArrayType) {
            case GL2ES1.GL_VERTEX_ARRAY:
                gl.glVertexPointer(components, glDataType, 0, 0);
                break;
            case GL2ES1.GL_NORMAL_ARRAY:
                gl.glNormalPointer(components, glDataType, 0);
                break;
            case GL2ES1.GL_COLOR_ARRAY:
                gl.glColorPointer(components, glDataType, 0, 0);
                break;
            case GL2ES1.GL_TEXTURE_COORD_ARRAY:
                gl.glTexCoordPointer(components, glDataType, 0, 0);
                break;
            default:
                throw new GLException("invalid glArrayType: "+glArrayType+":\n\t"+this); 
        }
        bufferEnabled = true;
    }
  }

  protected void disableBufferGLImpl(GL _gl) {
    GL2ES1 gl = _gl.getGL2ES1();
    if(bufferEnabled && null!=buffer) {
        gl.glDisableClientState(glArrayType);
        bufferEnabled = false;
    }
  }

}


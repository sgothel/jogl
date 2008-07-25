
package com.sun.opengl.impl.gl2es1;

import javax.media.opengl.util.VBOBufferDraw;
import javax.media.opengl.*;
import java.nio.*;

public class VBOBufferDrawGL2ES1 extends VBOBufferDraw {

  public VBOBufferDrawGL2ES1(int glArrayType, int glDataType, int glBufferUsage, int comps, int initialSize) {
    init(glArrayType, glDataType, glBufferUsage, comps, initialSize);
    setVBOUsage(false);
    //System.err.println("new VBOBufferDrawGL2ES1: "+this);
  }

  protected void enableBufferGLImpl(GL gl, boolean newData) {
    if(!bufferEnabled && null!=buffer) {
        gl.glEnableClientState(glArrayType);
        if(vboUsage) {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboName);
            if(newData) {
                gl.glBufferData(GL.GL_ARRAY_BUFFER, buffer.limit() * getBufferCompSize(), buffer, glBufferUsage);
            }
            switch(glArrayType) {
                case GL.GL_VERTEX_ARRAY:
                    gl.glVertexPointer(components, glDataType, 0, 0);
                    break;
                case GL.GL_NORMAL_ARRAY:
                    gl.glNormalPointer(glDataType, 0, 0);
                    break;
                case GL.GL_COLOR_ARRAY:
                    gl.glColorPointer(components, glDataType, 0, 0);
                    break;
                case GL.GL_TEXTURE_COORD_ARRAY:
                    gl.glTexCoordPointer(components, glDataType, 0, 0);
                    break;
                default:
                    throw new GLException("invalid glArrayType: "+glArrayType+":\n\t"+this); 
            }
        } else {
            switch(glArrayType) {
                case GL.GL_VERTEX_ARRAY:
                    gl.glVertexPointer(components, glDataType, 0, buffer);
                    break;
                case GL.GL_NORMAL_ARRAY:
                    gl.glNormalPointer(glDataType, 0, buffer);
                    break;
                case GL.GL_COLOR_ARRAY:
                    gl.glColorPointer(components, glDataType, 0, buffer);
                    break;
                case GL.GL_TEXTURE_COORD_ARRAY:
                    gl.glTexCoordPointer(components, glDataType, 0, buffer);
                    break;
                default:
                    throw new GLException("invalid glArrayType: "+glArrayType+":\n\t"+this); 
            }
        }
        bufferEnabled = true;
    }
  }

  protected void disableBufferGLImpl(GL gl) {
    if(bufferEnabled && null!=buffer) {
        gl.glDisableClientState(glArrayType);
        bufferEnabled = false;
    }
  }

}


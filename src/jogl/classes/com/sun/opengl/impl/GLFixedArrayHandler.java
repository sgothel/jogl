
package com.sun.opengl.impl;

import javax.media.opengl.*;
import javax.media.opengl.sub.*;
import javax.media.opengl.sub.fixed.*;
import com.sun.opengl.util.*;
import java.nio.*;

public class GLFixedArrayHandler implements GLArrayHandler {
  private GLArrayDataEditable ad;

  public GLFixedArrayHandler(GLArrayDataEditable ad) {
    this.ad = ad;
  }

  protected final void passArrayPointer(GLPointerIf gl) {
    switch(ad.getIndex()) {
        case GLPointerIf.GL_VERTEX_ARRAY:
            gl.glVertexPointer(ad);
            break;
        case GLPointerIf.GL_NORMAL_ARRAY:
            gl.glNormalPointer(ad);
            break;
        case GLPointerIf.GL_COLOR_ARRAY:
            gl.glColorPointer(ad);
            break;
        case GLPointerIf.GL_TEXTURE_COORD_ARRAY:
            gl.glTexCoordPointer(ad);
            break;
        default:
            throw new GLException("invalid glArrayIndex: "+ad.getIndex()+":\n\t"+ad); 
    }
  }

  public void enableBuffer(GL gl, boolean enable) {
    GLPointerIf glp = gl.getGL2ES1();
    if(enable) {
        glp.glEnableClientState(ad.getIndex());

        Buffer buffer = ad.getBuffer();

        if(ad.isVBO()) {
            // always bind and refresh the VBO mgr, 
            // in case more than one gl*Pointer objects are in use
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, ad.getVBOName());
            if(!ad.isBufferWritten()) {
                if(null!=buffer) {
                    gl.glBufferData(GL.GL_ARRAY_BUFFER, buffer.limit() * ad.getComponentSize(), buffer, ad.getBufferUsage());
                }
                ad.setBufferWritten(true);
            }
            passArrayPointer(glp);
        } else if(null!=buffer) {
            passArrayPointer(glp);
            ad.setBufferWritten(true);
        }
    } else {
        if(ad.isVBO()) {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        }
        glp.glDisableClientState(ad.getIndex());
    }
  }
}


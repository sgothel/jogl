
package com.jogamp.opengl.util;

import javax.media.opengl.*;
import javax.media.opengl.fixedfunc.*;
import com.jogamp.opengl.util.*;
import java.nio.*;

public class GLDataArrayHandler implements GLArrayHandler {
  private GLArrayDataEditable ad;

  public GLDataArrayHandler(GLArrayDataEditable ad) {
    this.ad = ad;
  }

  public void enableBuffer(GL gl, boolean enable) {
    GLPointerFunc glp = gl.getGL2ES1();
    if(enable) {
        Buffer buffer = ad.getBuffer();

        if(ad.isVBO()) {
            // always bind and refresh the VBO mgr, 
            // in case more than one gl*Pointer objects are in use
            gl.glBindBuffer(ad.getVBOTarget(), ad.getVBOName());
            if(!ad.isVBOWritten()) {
                if(null!=buffer) {
                    gl.glBufferData(ad.getVBOTarget(), buffer.limit() * ad.getComponentSize(), buffer, ad.getVBOUsage());
                }
                ad.setVBOWritten(true);
            }
        } else if(null!=buffer) {
            ad.setVBOWritten(true);
        }
    } else {
        if(ad.isVBO()) {
            gl.glBindBuffer(ad.getVBOTarget(), 0);
        }
    }
  }
}


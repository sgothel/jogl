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

package jogamp.opengl.util;

import javax.media.opengl.*;
import javax.media.opengl.fixedfunc.*;

import com.jogamp.opengl.util.GLArrayDataEditable;

import java.nio.*;

/**
 * Used for 1:1 fixed function arrays, i.e. where the buffer data 
 * represents this array only. 
 */
public class GLFixedArrayHandler implements GLArrayHandler {
  private GLArrayDataEditable ad;

  public GLFixedArrayHandler(GLArrayDataEditable ad) {
    this.ad = ad;
  }
  
  public final void setSubArrayVBOName(int vboName) {
      throw new UnsupportedOperationException();
  }
  
  public final void addSubHandler(GLArrayHandlerFlat handler) {
      throw new UnsupportedOperationException();
  }
  
  public final void syncData(GL gl, boolean enable, Object ext) {
    if(enable) {
        final Buffer buffer = ad.getBuffer();
        if(ad.isVBO()) {
            // always bind and refresh the VBO mgr, 
            // in case more than one gl*Pointer objects are in use
            gl.glBindBuffer(ad.getVBOTarget(), ad.getVBOName());
            if(!ad.isVBOWritten()) {
                if(null!=buffer) {
                    gl.glBufferData(ad.getVBOTarget(), buffer.limit() * ad.getComponentSizeInBytes(), buffer, ad.getVBOUsage());
                }
                ad.setVBOWritten(true);
            }
        }
        final GLPointerFunc glp = gl.getGL2ES1();
        switch(ad.getIndex()) {
            case GLPointerFunc.GL_VERTEX_ARRAY:
                glp.glVertexPointer(ad);
                break;
            case GLPointerFunc.GL_NORMAL_ARRAY:
                glp.glNormalPointer(ad);
                break;
            case GLPointerFunc.GL_COLOR_ARRAY:
                glp.glColorPointer(ad);
                break;
            case GLPointerFunc.GL_TEXTURE_COORD_ARRAY:
                glp.glTexCoordPointer(ad);
                break;
            default:
                throw new GLException("invalid glArrayIndex: "+ad.getIndex()+":\n\t"+ad); 
        }
    } else if(ad.isVBO()) {
        gl.glBindBuffer(ad.getVBOTarget(), 0);
    }
  }
  
  public final void enableState(GL gl, boolean enable, Object ext) {
    final GLPointerFunc glp = gl.getGL2ES1();
    if(enable) {
        glp.glEnableClientState(ad.getIndex());        
    } else {
        glp.glDisableClientState(ad.getIndex());
    }
  }
}


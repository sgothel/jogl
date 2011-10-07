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

package jogamp.opengl.util.glsl;

import java.nio.Buffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;

import jogamp.opengl.util.GLArrayHandler;
import jogamp.opengl.util.GLArrayHandlerFlat;

import com.jogamp.opengl.util.GLArrayDataEditable;
import com.jogamp.opengl.util.glsl.ShaderState;

/**
 * Used for 1:1 GLSL arrays, i.e. where the buffer data 
 * represents this array only. 
 */
public class GLSLArrayHandler implements GLArrayHandler {
  private GLArrayDataEditable ad;

  public GLSLArrayHandler(GLArrayDataEditable ad) {
    this.ad = ad;
  }
  
  public final void setSubArrayVBOName(int vboName) {
      throw new UnsupportedOperationException();
  }
  
  public final void addSubHandler(GLArrayHandlerFlat handler) {
      throw new UnsupportedOperationException();
  }
  
  public final void syncData(GL gl, boolean enable, Object ext) {
    final GL2ES2 glsl = gl.getGL2ES2();
    final ShaderState st = (ShaderState) ext;
    
    if(enable) {
        final Buffer buffer = ad.getBuffer();
        /*
         * This would be the non optimized code path:
         * 
        if(ad.isVBO()) {
            glsl.glBindBuffer(ad.getVBOTarget(), ad.getVBOName());
            if(!ad.isVBOWritten()) {
                if(null!=buffer) {
                    glsl.glBufferData(ad.getVBOTarget(), ad.getSizeInBytes(), buffer, ad.getVBOUsage());
                }
                ad.setVBOWritten(true);
            }
        }
        st.vertexAttribPointer(glsl, ad);
        */
        if(ad.isVBO()) {
            // bind and refresh the VBO / vertex-attr only if necessary
            if(!ad.isVBOWritten()) {
                glsl.glBindBuffer(ad.getVBOTarget(), ad.getVBOName());
                if(null!=buffer) {
                    glsl.glBufferData(ad.getVBOTarget(), ad.getSizeInBytes(), buffer, ad.getVBOUsage());
                }
                ad.setVBOWritten(true);
                st.vertexAttribPointer(glsl, ad);
            } else if(st.getAttribLocation(glsl, ad) >= 0) {
                // didn't experience a performance hit on this query ..
                // (using ShaderState's location query above to validate the location)
                final int[] qi = new int[1];
                glsl.glGetVertexAttribiv(ad.getLocation(), GL2ES2.GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING, qi, 0);
                if(ad.getVBOName() != qi[0]) {
                    glsl.glBindBuffer(ad.getVBOTarget(), ad.getVBOName());
                    st.vertexAttribPointer(glsl, ad);
                }
            }
        } else if(null!=buffer) {
            st.vertexAttribPointer(glsl, ad);
        }
    } else if(ad.isVBO()) {
        glsl.glBindBuffer(ad.getVBOTarget(), 0);
    }      
  }
  
  public final void enableState(GL gl, boolean enable, Object ext) {
    final GL2ES2 glsl = gl.getGL2ES2();
    final ShaderState st = (ShaderState) ext;
    
    if(enable) {
        st.enableVertexAttribArray(glsl, ad);
    } else {
        st.disableVertexAttribArray(glsl, ad);
    }
  }

}


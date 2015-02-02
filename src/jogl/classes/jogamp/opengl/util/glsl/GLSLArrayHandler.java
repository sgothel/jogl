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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;

import jogamp.opengl.util.GLArrayHandlerFlat;
import jogamp.opengl.util.GLVBOArrayHandler;

import com.jogamp.opengl.util.GLArrayDataEditable;
import com.jogamp.opengl.util.glsl.ShaderState;

/**
 * Used for 1:1 GLSL arrays, i.e. where the buffer data
 * represents this array only.
 */
public class GLSLArrayHandler extends GLVBOArrayHandler {

  public GLSLArrayHandler(final GLArrayDataEditable ad) {
    super(ad);
  }

  @Override
  public final void setSubArrayVBOName(final int vboName) {
      throw new UnsupportedOperationException();
  }

  @Override
  public final void addSubHandler(final GLArrayHandlerFlat handler) {
      throw new UnsupportedOperationException();
  }

  @Override
  public final void enableState(final GL gl, final boolean enable, final Object ext) {
    final GL2ES2 glsl = gl.getGL2ES2();
    if( null != ext ) {
        enableShaderState(glsl, enable, (ShaderState)ext);
    } else {
        enableSimple(glsl, enable);
    }
  }

  private final int[] tempI = new int[1];

  private final void enableShaderState(final GL2ES2 glsl, final boolean enable, final ShaderState st) {
    if(enable) {
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
        final Buffer buffer = ad.getBuffer();
        if(ad.isVBO()) {
            // bind and refresh the VBO / vertex-attr only if necessary
            if(!ad.isVBOWritten()) {
                glsl.glBindBuffer(ad.getVBOTarget(), ad.getVBOName());
                if(null!=buffer) {
                    glsl.glBufferData(ad.getVBOTarget(), ad.getSizeInBytes(), buffer, ad.getVBOUsage());
                }
                ad.setVBOWritten(true);
                st.vertexAttribPointer(glsl, ad);
                glsl.glBindBuffer(ad.getVBOTarget(), 0);
            } else if(st.getAttribLocation(glsl, ad) >= 0) {
                // didn't experience a performance hit on this query ..
                // (using ShaderState's location query above to validate the location)
                glsl.glGetVertexAttribiv(ad.getLocation(), GL2ES2.GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING, tempI, 0);
                if(ad.getVBOName() != tempI[0]) {
                    glsl.glBindBuffer(ad.getVBOTarget(), ad.getVBOName());
                    st.vertexAttribPointer(glsl, ad);
                    glsl.glBindBuffer(ad.getVBOTarget(), 0);
                }
            }
        } else if(null!=buffer) {
            st.vertexAttribPointer(glsl, ad);
        }

        st.enableVertexAttribArray(glsl, ad);
    } else {
        st.disableVertexAttribArray(glsl, ad);
    }
  }

  private final void enableSimple(final GL2ES2 glsl, final boolean enable) {
    final int location = ad.getLocation();
    if( 0 > location ) {
        return;
    }
    if(enable) {
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
        final Buffer buffer = ad.getBuffer();
        if(ad.isVBO()) {
            // bind and refresh the VBO / vertex-attr only if necessary
            if(!ad.isVBOWritten()) {
                glsl.glBindBuffer(ad.getVBOTarget(), ad.getVBOName());
                if(null!=buffer) {
                    glsl.glBufferData(ad.getVBOTarget(), ad.getSizeInBytes(), buffer, ad.getVBOUsage());
                }
                ad.setVBOWritten(true);
                glsl.glVertexAttribPointer(ad);
                glsl.glBindBuffer(ad.getVBOTarget(), 0);
            } else {
                // didn't experience a performance hit on this query ..
                // (using ShaderState's location query above to validate the location)
                glsl.glGetVertexAttribiv(location, GL2ES2.GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING, tempI, 0);
                if(ad.getVBOName() != tempI[0]) {
                    glsl.glBindBuffer(ad.getVBOTarget(), ad.getVBOName());
                    glsl.glVertexAttribPointer(ad);
                    glsl.glBindBuffer(ad.getVBOTarget(), 0);
                }
            }
        } else if(null!=buffer) {
            glsl.glVertexAttribPointer(ad);
        }

        glsl.glEnableVertexAttribArray(location);
    } else {
        glsl.glDisableVertexAttribArray(location);
    }
  }
}


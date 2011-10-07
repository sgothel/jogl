/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;

import jogamp.opengl.util.GLArrayHandlerFlat;

import com.jogamp.opengl.util.GLArrayDataWrapper;
import com.jogamp.opengl.util.glsl.ShaderState;

/**
 * Used for interleaved GLSL arrays, i.e. where the buffer data itself is handled 
 * separately and interleaves many arrays.
 */
public class GLSLArrayHandlerFlat implements GLArrayHandlerFlat {
  private GLArrayDataWrapper ad;

  public GLSLArrayHandlerFlat(GLArrayDataWrapper ad) {
    this.ad = ad;
  }

  public GLArrayDataWrapper getData() {
      return ad;
  }
    
  public final void syncData(GL gl, boolean enable, boolean force, Object ext) {
    if(enable) {
        final GL2ES2 glsl = gl.getGL2ES2();
        final ShaderState st = (ShaderState) ext;

        st.vertexAttribPointer(glsl, ad);
        /**
         * Due to probable application VBO switching, this might not make any sense ..
         * 
        if(force) {
            st.vertexAttribPointer(glsl, ad);
        } else if(st.getAttribLocation(glsl, ad) >= 0) {
            final int[] qi = new int[1];
            glsl.glGetVertexAttribiv(ad.getLocation(), GL2ES2.GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING, qi, 0);
            if(ad.getVBOName() != qi[0]) {
                System.err.println("XXX1: "+ad.getName()+", vbo ad "+ad.getVBOName()+", gl "+qi[0]+", "+ad);
                st.vertexAttribPointer(glsl, ad);
            } else {
                System.err.println("XXX0: "+ad.getName()+", vbo ad "+ad.getVBOName()+", gl "+qi[0]+", "+ad);
            }
        }*/
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


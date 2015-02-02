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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;

import jogamp.opengl.util.GLArrayHandlerFlat;

import com.jogamp.opengl.util.GLArrayDataWrapper;
import com.jogamp.opengl.util.glsl.ShaderState;

/**
 * Used for interleaved GLSL arrays, i.e. where the buffer data itself is handled
 * separately and interleaves many arrays.
 */
public class GLSLArrayHandlerFlat implements GLArrayHandlerFlat {
  private final GLArrayDataWrapper ad;

  public GLSLArrayHandlerFlat(final GLArrayDataWrapper ad) {
    this.ad = ad;
  }

  @Override
  public GLArrayDataWrapper getData() {
      return ad;
  }

  @Override
  public final void syncData(final GL gl, final Object ext) {
    final GL2ES2 glsl = gl.getGL2ES2();
    if( null != ext ) {
        ((ShaderState)ext).vertexAttribPointer(glsl, ad);
    } else {
        if( 0 <= ad.getLocation() ) {
            glsl.glVertexAttribPointer(ad);
        }
    }
    /**
     * Due to probable application VBO switching, this might not make any sense ..
     *
    if(!written) {
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

  @Override
  public final void enableState(final GL gl, final boolean enable, final Object ext) {
    final GL2ES2 glsl = gl.getGL2ES2();
    if( null != ext ) {
        final ShaderState st = (ShaderState)ext;
        if(enable) {
            st.enableVertexAttribArray(glsl, ad);
        } else {
            st.disableVertexAttribArray(glsl, ad);
        }
    } else {
        final int location = ad.getLocation();
        if( 0 <= location ) {
            if(enable) {
                glsl.glEnableVertexAttribArray(location);
            } else {
                glsl.glDisableVertexAttribArray(location);
            }
        }
    }
  }
}

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

package com.jogamp.opengl.util.glsl;

import javax.media.opengl.*;
import com.jogamp.opengl.util.*;
import java.nio.*;

public class GLSLArrayHandler implements GLArrayHandler {
  private GLArrayDataEditable ad;

  public GLSLArrayHandler(GLArrayDataEditable ad) {
    this.ad = ad;
  }

  protected final void passVertexAttribPointer(GL2ES2 gl, ShaderState st) {
    st.glVertexAttribPointer(gl, ad);
  }

  public void enableBuffer(GL gl, boolean enable) {
    if(!gl.isGL2ES2()) {
        throw new GLException("GLSLArrayHandler expects a GL2ES2 implementation");
    }
    GL2ES2 glsl = gl.getGL2ES2();
    ShaderState st = ShaderState.getCurrent();
    if(null==st) {
        throw new GLException("No ShaderState current");
    }

    if(enable) {
        st.glEnableVertexAttribArray(glsl, ad.getName());

        Buffer buffer = ad.getBuffer();

        if(ad.isVBO()) {
            // always bind and refresh the VBO mgr,
            // in case more than one gl*Pointer objects are in use
            glsl.glBindBuffer(GL.GL_ARRAY_BUFFER, ad.getVBOName());
            if(!ad.isBufferWritten()) {
                if(null!=buffer) {
                    glsl.glBufferData(GL.GL_ARRAY_BUFFER, buffer.limit() * ad.getComponentSize(), buffer, ad.getBufferUsage());
                }
                ad.setBufferWritten(true);
            }
            passVertexAttribPointer(glsl, st);
        } else if(null!=buffer) {
            passVertexAttribPointer(glsl, st);
            ad.setBufferWritten(true);
        }
    } else {
        if(ad.isVBO()) {
            glsl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        }
        st.glDisableVertexAttribArray(glsl, ad.getName());
    }
  }

}


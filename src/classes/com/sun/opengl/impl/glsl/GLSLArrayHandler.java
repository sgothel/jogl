
package com.sun.opengl.impl.glsl;

import com.sun.opengl.impl.*;

import javax.media.opengl.*;
import javax.media.opengl.glsl.ShaderState;
import java.nio.*;

public class GLSLArrayHandler implements GLArrayHandler {
  private GLArrayData ad;

  public GLSLArrayHandler(GLArrayData ad) {
    this.ad = ad;
  }

  protected final void passVertexAttribPointer(GL2ES2 gl, ShaderState st) {
    if ( ! st.glVertexAttribPointer(gl, ad) ) {
        throw new RuntimeException("Internal Error");
    }
  }

  public void enableBuffer(GL gl, boolean enable) {
    GL2ES2 glsl = gl.getGL2ES2();
    ShaderState st = ShaderState.getCurrent();
    if(null==st) {
        throw new GLException("No ShaderState current");
    }

    if(enable) {
        if(!st.glEnableVertexAttribArray(glsl, ad.getName())) {
            throw new RuntimeException("Internal Error");
        }

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
            passVertexAttribPointer(glsl, st);
        } else if(null!=buffer) {
            passVertexAttribPointer(glsl, st);
            ad.setBufferWritten(true);
        }
    } else {
        if(ad.isVBO()) {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        }
        if(!st.glDisableVertexAttribArray(glsl, ad.getName())) {
            throw new RuntimeException("Internal Error");
        }
    }
  }

}



package com.sun.opengl.impl.glsl;

import javax.media.opengl.*;
import javax.media.opengl.util.glsl.ShaderState;
import java.nio.*;

public class GLSLArrayDataServer extends GLArrayDataServer {

  public GLSLArrayDataServer(String name, int comps, int dataType, boolean normalized, 
                             int stride, Buffer buffer, int glBufferUsage) {
    init(name, -1, comps, dataType, normalized, stride, buffer, 0, buffer.limit(), glBufferUsage, true);
  }

  public GLSLArrayDataServer(String name, int comps, int dataType, boolean normalized, 
                             int stride, long bufferOffset) {
    init(name, -1, comps, dataType, normalized, stride, null, bufferOffset, 0, -1, true);
  }

  public GLSLArrayDataServer(String name, int comps, int dataType, boolean normalized, 
                             int initialSize, int glBufferUsage) {
    init(name, -1, comps, dataType, normalized, 0, null, 0, initialSize, glBufferUsage, true);
  }


  protected void enableBufferGLImpl(GL gl, boolean enable) {
    GL2ES2 glsl = gl.getGL2ES2();
    ShaderState st = ShaderState.getCurrent();
    if(null==st) {
        throw new GLException("No ShaderState current");
    }

    if(enable) {
        if(!st.glEnableVertexAttribArray(glsl, name)) {
            throw new RuntimeException("Internal Error");
        }
        bufferEnabled = true;

        if(vboUsage) {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboName);
            if(!bufferWritten) {
                gl.glBufferData(GL.GL_ARRAY_BUFFER, buffer.limit() * getBufferCompSize(), buffer, glBufferUsage);
            }
        }
        if ( ! st.glVertexAttribPointer(glsl, this) ) {
            throw new RuntimeException("Internal Error");
        }
        bufferWritten=true;
    } else {
        if(!st.glDisableVertexAttribArray(glsl, name)) {
            throw new RuntimeException("Internal Error");
        }
        bufferEnabled = false;
    }
  }

}


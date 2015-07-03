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

package jogamp.opengl.util;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.fixedfunc.GLPointerFunc;

import com.jogamp.opengl.util.GLArrayDataWrapper;

/**
 * Used for interleaved fixed function arrays, i.e. where the buffer data itself is handled
 * separately and interleaves many arrays.
 */
public class GLFixedArrayHandlerFlat implements GLArrayHandlerFlat {
  private final GLArrayDataWrapper ad;

  public GLFixedArrayHandlerFlat(final GLArrayDataWrapper ad) {
    this.ad = ad;
  }

  @Override
  public GLArrayDataWrapper getData() {
      return ad;
  }

  @Override
  public final void syncData(final GL gl, final Object ext) {
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
  }

  @Override
  public final void enableState(final GL gl, final boolean enable, final Object ext) {
    final GLPointerFunc glp = gl.getGL2ES1();
    if(enable) {
        glp.glEnableClientState(ad.getIndex());
    } else {
        glp.glDisableClientState(ad.getIndex());
    }
  }
}


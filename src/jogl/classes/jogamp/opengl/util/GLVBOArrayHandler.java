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

import java.nio.Buffer;

import com.jogamp.opengl.GL;

import com.jogamp.opengl.util.GLArrayDataEditable;

/**
 * Interleaved fixed function arrays, i.e. where this buffer data
 * represents many arrays.
 */
public abstract class GLVBOArrayHandler implements GLArrayHandler {
  protected GLArrayDataEditable ad;

  public GLVBOArrayHandler(final GLArrayDataEditable ad) {
    this.ad = ad;
  }

  @Override
  public final boolean bindBuffer(final GL gl, final boolean bind) {
    if( !ad.isVBO() ) {
        return false;
    }
    if(bind) {
        // always bind and refresh the VBO mgr,
        // in case more than one gl*Pointer objects are in use
        gl.glBindBuffer(ad.getVBOTarget(), ad.getVBOName());
        if(!ad.isVBOWritten()) {
            final Buffer buffer = ad.getBuffer();
            if(null!=buffer) {
                gl.glBufferData(ad.getVBOTarget(), buffer.limit() * ad.getComponentSizeInBytes(), buffer, ad.getVBOUsage());
            }
            ad.setVBOWritten(true);
        }
    } else {
        gl.glBindBuffer(ad.getVBOTarget(), 0);
    }
    return true;
  }

}


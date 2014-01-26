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

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import com.jogamp.opengl.util.GLArrayDataEditable;

/**
 * Interleaved fixed function arrays, i.e. where this buffer data
 * represents many arrays.
 */
public class GLArrayHandlerInterleaved extends GLVBOArrayHandler {
  private final List<GLArrayHandlerFlat> subArrays = new ArrayList<GLArrayHandlerFlat>();

  public GLArrayHandlerInterleaved(GLArrayDataEditable ad) {
    super(ad);
  }

  @Override
  public final void setSubArrayVBOName(int vboName) {
      for(int i=0; i<subArrays.size(); i++) {
          subArrays.get(i).getData().setVBOName(vboName);
      }
  }

  @Override
  public final void addSubHandler(GLArrayHandlerFlat handler) {
      subArrays.add(handler);
  }

  private final void syncSubData(GL gl, Object ext) {
      for(int i=0; i<subArrays.size(); i++) {
          subArrays.get(i).syncData(gl, ext);
      }
  }

  @Override
  public final void enableState(GL gl, boolean enable, Object ext) {
    if(enable) {
        final boolean vboBound = bindBuffer(gl, true);
        syncSubData(gl, ext);
        if(vboBound) {
            bindBuffer(gl, false);
        }
    }
    for(int i=0; i<subArrays.size(); i++) {
        subArrays.get(i).enableState(gl, enable, ext);
    }
  }
}


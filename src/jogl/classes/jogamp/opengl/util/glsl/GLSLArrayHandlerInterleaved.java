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

import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL;

import jogamp.opengl.util.GLArrayHandlerFlat;
import jogamp.opengl.util.GLVBOArrayHandler;

import com.jogamp.opengl.util.GLArrayDataEditable;

/**
 * Interleaved fixed function arrays, i.e. where this buffer data
 * represents many arrays.
 */
public class GLSLArrayHandlerInterleaved extends GLVBOArrayHandler {
  private final List<GLArrayHandlerFlat> subArrays = new ArrayList<GLArrayHandlerFlat>();

  public GLSLArrayHandlerInterleaved(final GLArrayDataEditable ad) {
    super(ad);
  }

  @Override
  public final void setSubArrayVBOName(final int vboName) {
      for(int i=0; i<subArrays.size(); i++) {
          subArrays.get(i).getData().setVBOName(vboName);
      }
  }

  @Override
  public final void addSubHandler(final GLArrayHandlerFlat handler) {
      subArrays.add(handler);
  }

  private final void syncSubData(final GL gl, final Object ext) {
      for(int i=0; i<subArrays.size(); i++) {
          subArrays.get(i).syncData(gl, ext);
      }
  }

  @Override
  public final void enableState(final GL gl, final boolean enable, final Object ext) {
    if(enable) {
        if(!ad.isVBO()) {
            throw new InternalError("Interleaved handle is not VBO: "+ad);
        }
        bindBuffer(gl, true);
        // sub data will decide whether to update the vertex attrib pointer
        syncSubData(gl, ext);
        bindBuffer(gl, false);
    }
    for(int i=0; i<subArrays.size(); i++) {
        subArrays.get(i).enableState(gl, enable, ext);
    }
  }
}


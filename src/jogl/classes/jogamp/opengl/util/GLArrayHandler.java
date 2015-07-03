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

import com.jogamp.opengl.*;

/**
 * Handles consistency of buffer data and array state.<br/>
 * Implementations shall consider buffer types (VBO, ..), interleaved, etc.<br/>
 * They also need to consider array state types, i.e. fixed function or GLSL.<br/>
 */
public interface GLArrayHandler {


  /**
   * if <code>bind</code> is true and the data uses VBO,
   * the latter will be bound and data written to the GPU if required.
   * <p>
   * If  <code>bind</code> is false and the data uses VBO,
   * the latter will be unbound.
   * </p>
   *
   * @param gl current GL object
   * @param bind true if VBO shall be bound and data written,
   *        otherwise clear VBO binding.
   * @return true if data uses VBO and action was performed, otherwise false
   */
  public boolean bindBuffer(GL gl, boolean bind);

  /**
   * Implementation shall enable or disable the array state.
   * <p>
   * Before enabling the array state,
   * implementation shall synchronize the data with the GPU
   * and associate the data with the array.
   * </p>
   *
   * @param gl current GL object
   * @param enable true if array shall be enabled, otherwise false.
   * @param ext extension object allowing passing of an implementation detail
   */
  public void enableState(GL gl, boolean enable, Object ext);

  /**
   * Supporting interleaved arrays, where sub handlers may handle
   * the array state and the <i>master</i> handler the buffer consistency.
   *
   * @param handler the sub handler
   * @throws UnsupportedOperationException if this array handler does not support interleaved arrays
   */
  public void addSubHandler(GLArrayHandlerFlat handler) throws UnsupportedOperationException;

  public void setSubArrayVBOName(int vboName);

}


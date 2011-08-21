
package com.jogamp.opengl.util;

import javax.media.opengl.*;

/**
 * Handles consistency of buffer data and array state.
 * Implementations shall consider buffer types (VBO, ..), interleaved, etc.
 * They also need to consider array state types, i.e. fixed function or GLSL.
 */
public interface GLArrayHandler {

  /**
   * Implementation shall ensure the buffers data is synchronized to the GPU
   * and the array state is enabled.
   * 
   * @param gl current GL object
   * @param enable true if array shall be enabled, otherwise false.
   */
  public void enableBuffer(GL gl, boolean enable);
  
  /**
   * Supporting interleaved arrays, where sub handlers may handle 
   * the array state and the <i>master</i> handler the buffer consistency.
   *   
   * @param handler the sub handler
   * @throws UnsupportedOperationException if this array handler does not support interleaved arrays
   */
  public void addSubHandler(GLArrayHandler handler) throws UnsupportedOperationException;

}


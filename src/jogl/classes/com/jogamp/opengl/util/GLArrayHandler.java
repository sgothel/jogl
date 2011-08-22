
package com.jogamp.opengl.util;

import javax.media.opengl.*;

/**
 * Handles consistency of buffer data and array state.
 * Implementations shall consider buffer types (VBO, ..), interleaved, etc.
 * They also need to consider array state types, i.e. fixed function or GLSL.
 */
public interface GLArrayHandler {

  /**
   * Implementation shall associate the data with the array
   * and synchronize the data with the GPU.
   * 
   * @param gl current GL object
   * @param enable true if array data shall be valid, otherwise false.
   */
  public void syncData(GL gl, boolean enable);
  
  /**
   * Implementation shall enable or disable the array state.
   * 
   * @param gl current GL object
   * @param enable true if array shall be enabled, otherwise false.
   */
  public void enableState(GL gl, boolean enable);
  
  /**
   * Supporting interleaved arrays, where sub handlers may handle 
   * the array state and the <i>master</i> handler the buffer consistency.
   *   
   * @param handler the sub handler
   * @throws UnsupportedOperationException if this array handler does not support interleaved arrays
   */
  public void addSubHandler(GLArrayHandler handler) throws UnsupportedOperationException;

}


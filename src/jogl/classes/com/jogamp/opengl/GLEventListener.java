/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.jogamp.opengl;

import java.util.EventListener;

/** Declares events which client code can use to manage OpenGL
    rendering into a {@link GLAutoDrawable}. At the time any of these
    methods is called, the drawable has made its associated OpenGL
    context current, so it is valid to make OpenGL calls. */

public interface GLEventListener extends EventListener {
  /** Called by the drawable immediately after the OpenGL context is
      initialized. Can be used to perform one-time OpenGL
      initialization per GLContext, such as setup of lights and display lists.<p>

      Note that this method may be called more than once if the underlying
      OpenGL context for the GLAutoDrawable is destroyed and
      recreated, for example if a GLCanvas is removed from the widget
      hierarchy and later added again.
  */
  public void init(GLAutoDrawable drawable);

  /** Notifies the listener to perform the release of all OpenGL
      resources per GLContext, such as memory buffers and GLSL programs.<P>

      Called by the drawable before the OpenGL context is
      destroyed by an external event, like a reconfiguration of the
      {@link GLAutoDrawable} closing an attached window,
      but also manually by calling {@link GLAutoDrawable#destroy destroy}.<P>

      Note that this event does not imply the end of life of the application.
      It could be produced with a followup call to {@link #init(GLAutoDrawable)}
      in case the GLContext has been recreated,
      e.g. due to a pixel configuration change in a multihead environment.
  */
  public void dispose(GLAutoDrawable drawable);

  /** Called by the drawable to initiate OpenGL rendering by the
      client. After all GLEventListeners have been notified of a
      display event, the drawable will swap its buffers if {@link
      GLAutoDrawable#setAutoSwapBufferMode setAutoSwapBufferMode} is
      enabled. */
  public void display(GLAutoDrawable drawable);

  /**
   * Called by the drawable during the first repaint after the
   * component has been resized.
   * <p>
   * The client can update it's viewport associated data
   * and view volume of the window appropriately.
   * </p>
   * <p>
   * For efficiency the GL viewport has already been updated
   * via <code>glViewport(x, y, width, height)</code> when this method is called.
   * </p>
   *
   * @param drawable the triggering {@link GLAutoDrawable}
   * @param x viewport x-coord in pixel units
   * @param y viewport y-coord in pixel units
   * @param width viewport width in pixel units
   * @param height viewport height in pixel units
   */
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height);
}

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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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

package net.java.games.jogl;

import java.util.EventListener;

/** Declares events which client code can use to manage OpenGL
    rendering into a {@link GLDrawable}. At the time any of these
    methods is called, the drawable has made its associated OpenGL
    context current, so it is valid to make OpenGL calls. */

public interface GLEventListener extends EventListener {
  /** Called by the drawable immediately after the OpenGL context is
      initialized for the first time. Can be used to perform one-time
      OpenGL initialization such as setup of lights and display lists.
  */
  public void init(GLDrawable drawable);
  
  /** Called by the drawable to initiate OpenGL rendering by the
      client. After all GLEventListeners have been notified of a
      display event, the drawable will swap its buffers if necessary.
  */
  public void display(GLDrawable drawable);

  /** Called by the drawable during the first repaint after the
      component has been resized. The client can update the viewport
      and view volume of the window appropriately, for example by a
      call to {@link net.java.games.jogl.GL#glViewport}; note that for
      convenience the component has already called {@link
      net.java.games.jogl.GL#glViewport}(x, y, width, height) when this method
      is called, so the client may not have to do anything in this
      method.
  */
  public void reshape(GLDrawable drawable, int x, int y, int width, int height);

  /** Called by the drawable when the display mode or the display device
      associated with the GLDrawable has changed. The two boolean parameters
      indicate the types of change(s) that have occurred. (<b> !!! CURRENTLY
      UNIMPLEMENTED !!! </b>)
      <P>

      An example of a display <i>mode</i> change is when the bit depth changes (e.g.,
      from 32-bit to 16-bit color) on monitor upon which the GLDrawable is
      currently being displayed. <p>

      An example of a display <i>device</i> change is when the user drags the
      window containing the GLDrawable from one monitor to another in a
      multiple-monitor setup. <p>

      The reason that this function handles both types of changes (instead of
      handling mode and device changes in separate methods) is so that
      applications have the opportunity to respond to display changes the most
      efficient manner. For example, the application may need make fewer
      adjustments to compensate for a device change if it knows that the mode
      on the new device is identical the previous mode.
  */
  public void displayChanged(GLDrawable drawable, boolean modeChanged, boolean deviceChanged);
}

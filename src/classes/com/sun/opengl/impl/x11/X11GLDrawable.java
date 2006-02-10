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

package com.sun.opengl.impl.x11;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public abstract class X11GLDrawable extends GLDrawableImpl {
  protected static final boolean DEBUG = Debug.debug("X11GLDrawable");

  protected long display;
  protected long drawable;
  protected long visualID;
  protected GLCapabilities capabilities;
  protected GLCapabilitiesChooser chooser;

  public X11GLDrawable(GLCapabilities capabilities,
                       GLCapabilitiesChooser chooser) {
    this.capabilities = (capabilities == null) ? null :
      ((GLCapabilities) capabilities.clone());
    this.chooser = chooser;
  }

  public void setRealized(boolean val) {
    throw new GLException("Should not call this (should only be called for onscreen GLDrawables)");
  }

  public void destroy() {
    throw new GLException("Should not call this (should only be called for offscreen GLDrawables)");
  }

  public void swapBuffers() throws GLException {
  }

  public long getDisplay() {
    return display;
  }

  public long getDrawable() {
    return drawable;
  }

  //---------------------------------------------------------------------------
  // Internals only below this point
  //

  protected XVisualInfo chooseVisual(boolean onscreen) {
    if (display == 0) {
      throw new GLException("null display");
    }

    // FIXME
    if (onscreen) {
      // The visual has already been chosen by the time we get here;
      // it's specified by the GraphicsConfiguration of the
      // GLCanvas. Fortunately, the JAWT supplies the visual ID for
      // the component in a portable fashion, so all we have to do is
      // use XGetVisualInfo with a VisualIDMask to get the
      // corresponding XVisualInfo to pass into glXChooseVisual.
      int[] count = new int[1];
      XVisualInfo template = XVisualInfo.create();
      // FIXME: probably not 64-bit clean
      template.visualid((int) visualID);
      lockToolkit();
      XVisualInfo[] infos = GLX.XGetVisualInfo(display, GLX.VisualIDMask, template, count, 0);
      unlockToolkit();
      if (infos == null || infos.length == 0) {
        throw new GLException("Error while getting XVisualInfo for visual ID " + visualID);
      }
      if (DEBUG) {
        System.err.println("!!! Fetched XVisualInfo for visual ID 0x" + Long.toHexString(visualID));
        System.err.println("!!! Resulting XVisualInfo: visualid = 0x" + Long.toHexString(infos[0].visualid()));
      }

      // FIXME: the storage for the infos array is leaked (should
      // clean it up somehow when we're done with the visual we're
      // returning)
      return infos[0];
    } else {
      // It isn't clear to me whether we need this much code to handle
      // the offscreen case, where we're creating a pixmap into which
      // to render...this is what we (incorrectly) used to do for the
      // onscreen case

      int screen = 0; // FIXME: provide way to specify this?
      XVisualInfo vis = null;
      int[] count = new int[1];
      XVisualInfo template = XVisualInfo.create();
      template.screen(screen);
      XVisualInfo[] infos = null;
      GLCapabilities[] caps = null;
      lockToolkit();
      try {
        infos = GLX.XGetVisualInfo(display, GLX.VisualScreenMask, template, count, 0);
        if (infos == null) {
          throw new GLException("Error while enumerating available XVisualInfos");
        }
        caps = new GLCapabilities[infos.length];
        for (int i = 0; i < infos.length; i++) {
          caps[i] = X11GLDrawableFactory.xvi2GLCapabilities(display, infos[i]);
        }
      } finally {
        unlockToolkit();
      }
      int chosen = chooser.chooseCapabilities(capabilities, caps, -1);
      if (chosen < 0 || chosen >= caps.length) {
        throw new GLException("GLCapabilitiesChooser specified invalid index (expected 0.." + (caps.length - 1) + ")");
      }
      if (DEBUG) {
        System.err.println("Chosen visual (" + chosen + "):");
        System.err.println(caps[chosen]);
      }
      vis = infos[chosen];
      if (vis == null) {
        throw new GLException("GLCapabilitiesChooser chose an invalid visual");
      }
      // FIXME: the storage for the infos array is leaked (should
      // clean it up somehow when we're done with the visual we're
      // returning)

      return vis;
    }
  }


  // These synchronization primitives prevent the AWT from making
  // requests from the X server asynchronously to this code.
  protected void lockToolkit() {
    X11GLDrawableFactory.getX11Factory().lockToolkit();
  }

  protected void unlockToolkit() {
    X11GLDrawableFactory.getX11Factory().unlockToolkit();
  }
}

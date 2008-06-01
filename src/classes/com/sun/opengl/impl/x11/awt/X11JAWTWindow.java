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

package com.sun.opengl.impl.x11.awt;

import com.sun.opengl.impl.x11.*;
import com.sun.opengl.impl.awt.*;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public class X11JAWTWindow extends JAWTWindow {

  // Variables for lockSurface/unlockSurface
  private JAWT_DrawingSurface ds;
  private JAWT_DrawingSurfaceInfo dsi;
  private JAWT_X11DrawingSurfaceInfo x11dsi;
  
  //---------------------------------------------------------------------------
  // Xinerama-related functionality
  //

  private boolean checkedXinerama;
  private boolean xineramaEnabled;
  protected synchronized boolean isXineramaEnabled() {
    if (!checkedXinerama) {
      checkedXinerama = true;
      lockToolkit();
      long display = getDisplayConnection();
      xineramaEnabled = GLX.XineramaEnabled(display);
      unlockToolkit();
    }
    return xineramaEnabled;
  }

  public X11JAWTWindow(Object comp) {
    super(comp);
  }

  public int lockSurface() throws NativeWindowException {
    super.lockSurface();
    ds = JAWT.getJAWT().GetDrawingSurface(component);
    if (ds == null) {
      // Widget not yet realized
      return LOCK_SURFACE_NOT_READY;
    }
    int res = ds.Lock();
    if ((res & JAWTFactory.JAWT_LOCK_ERROR) != 0) {
      throw new NativeWindowException("Unable to lock surface");
    }
    // See whether the surface changed and if so destroy the old
    // OpenGL context so it will be recreated (NOTE: removeNotify
    // should handle this case, but it may be possible that race
    // conditions can cause this code to be triggered -- should test
    // more)
    int ret = LOCK_SUCCESS;
    if ((res & JAWTFactory.JAWT_LOCK_SURFACE_CHANGED) != 0) {
      ret = LOCK_SURFACE_CHANGED;
    }
    dsi = ds.GetDrawingSurfaceInfo();
    if (dsi == null) {
      // Widget not yet realized
      ds.Unlock();
      JAWT.getJAWT().FreeDrawingSurface(ds);
      ds = null;
      return LOCK_SURFACE_NOT_READY;
    }
    x11dsi = (JAWT_X11DrawingSurfaceInfo) dsi.platformInfo();
    display = x11dsi.display();
    drawable = x11dsi.drawable();
    visualID = x11dsi.visualID();
    screen= null;
    if (isXineramaEnabled()) {
      screenIndex = 0;
    } else {
      screenIndex = X11SunJDKReflection.graphicsDeviceGetScreen(config.getDevice());
    }
    if (display == 0 || drawable == 0) {
      // Widget not yet realized
      ds.FreeDrawingSurfaceInfo(dsi);
      ds.Unlock();
      JAWT.getJAWT().FreeDrawingSurface(ds);
      ds = null;
      dsi = null;
      x11dsi = null;
      display = 0;
      drawable = 0;
      visualID = 0;
      screen= null;
      screenIndex = -1;
      return LOCK_SURFACE_NOT_READY;
    }
    return ret;
  }

  public void unlockSurface() {
    super.unlockSurface();
    ds.FreeDrawingSurfaceInfo(dsi);
    ds.Unlock();
    JAWT.getJAWT().FreeDrawingSurface(ds);
    ds = null;
    dsi = null;
    x11dsi = null;
  }
}

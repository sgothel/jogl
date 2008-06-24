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
 */

package com.sun.opengl.impl.jawt.x11;

import com.sun.opengl.impl.x11.*;
import com.sun.opengl.impl.jawt.*;
import com.sun.opengl.impl.*;

import javax.media.opengl.*;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

public class X11JAWTWindow extends JAWTWindow {

  public X11JAWTWindow(Object comp) {
    super(comp);
  }

  protected void initNative() throws NativeWindowException {
  }

  public int lockSurface() throws NativeWindowException {
    int ret = super.lockSurface();
    if(LOCK_SUCCESS != ret) {
        return ret;
    }
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
    screen= 0;
    if (X11Lib.XineramaEnabled(display)) {
      screenIndex = 0;
    } else {
      GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
      screenIndex = X11SunJDKReflection.graphicsDeviceGetScreen(device);
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
      screen= 0;
      screenIndex = -1;
      return LOCK_SURFACE_NOT_READY;
    }
    return ret;
  }

  public void unlockSurface() {
    if(!isSurfaceLocked()) return;
    ds.FreeDrawingSurfaceInfo(dsi);
    ds.Unlock();
    JAWT.getJAWT().FreeDrawingSurface(ds);
    ds = null;
    dsi = null;
    x11dsi = null;
    super.unlockSurface();
  }

  // Variables for lockSurface/unlockSurface
  private JAWT_DrawingSurface ds;
  private JAWT_DrawingSurfaceInfo dsi;
  private JAWT_X11DrawingSurfaceInfo x11dsi;
  
}

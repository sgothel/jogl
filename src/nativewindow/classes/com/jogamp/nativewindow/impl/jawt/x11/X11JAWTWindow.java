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

package com.jogamp.nativewindow.impl.jawt.x11;

import javax.media.nativewindow.*;
import javax.media.nativewindow.awt.*;
import javax.media.nativewindow.x11.*;

import com.jogamp.nativewindow.impl.x11.*;
import com.jogamp.nativewindow.impl.jawt.*;
import com.jogamp.nativewindow.impl.*;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

public class X11JAWTWindow extends JAWTWindow {

  public X11JAWTWindow(Object comp, AbstractGraphicsConfiguration config) {
    super(comp, config);
  }

  protected void initNative() throws NativeWindowException {
    if(0==config.getScreen().getDevice().getHandle()) {
        AWTGraphicsDevice awtDevice = (AWTGraphicsDevice) config.getScreen().getDevice();
        long displayHandle = X11SunJDKReflection.graphicsDeviceGetDisplay(awtDevice.getGraphicsDevice());
        if(0==displayHandle) {
            displayHandle = X11Util.createThreadLocalDisplay(null);
        }
        awtDevice.setHandle(displayHandle);
    }
  }

  protected int lockSurfaceImpl() throws NativeWindowException {
    // JAWTUtil.lockToolkit();
    // config.getNativeGraphicsConfiguration().getScreen().getDevice().lock();
    try {
        int ret = NativeWindow.LOCK_SUCCESS;
        ds = JAWT.getJAWT().GetDrawingSurface(component);
        if (ds == null) {
          // Widget not yet realized
          unlockSurface();
          return LOCK_SURFACE_NOT_READY;
        }
        int res = ds.Lock();
        dsLocked = ( 0 == ( res & JAWTFactory.JAWT_LOCK_ERROR ) ) ;
        if (!dsLocked) {
          unlockSurface();
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
          unlockSurface();
          return LOCK_SURFACE_NOT_READY;
        }
        x11dsi = (JAWT_X11DrawingSurfaceInfo) dsi.platformInfo();
        if (x11dsi == null) {
          unlockSurface();
          return LOCK_SURFACE_NOT_READY;
        }
        drawable = x11dsi.getDrawable();
        if (drawable == 0) {
          unlockSurface();
          return LOCK_SURFACE_NOT_READY;
        } else {
          updateBounds(dsi.getBounds());
        }
        return ret;
    } finally {
        // config.getNativeGraphicsConfiguration().getScreen().getDevice().unlock();
        // JAWTUtil.unlockToolkit();
    }
  }

  protected void unlockSurfaceImpl() throws NativeWindowException {
    // JAWTUtil.lockToolkit();
    // config.getNativeGraphicsConfiguration().getScreen().getDevice().lock();
    try {
        if(null!=ds) {
            if (null!=dsi) {
                ds.FreeDrawingSurfaceInfo(dsi);
            }
            if (dsLocked) {
                ds.Unlock();
            }
            JAWT.getJAWT().FreeDrawingSurface(ds);
        }
        ds = null;
        dsi = null;
        x11dsi = null;
    } finally {
        // config.getNativeGraphicsConfiguration().getScreen().getDevice().unlock();
        // JAWTUtil.unlockToolkit();
    }
  }

  // Variables for lockSurface/unlockSurface
  private JAWT_DrawingSurface ds;
  private boolean dsLocked;
  private JAWT_DrawingSurfaceInfo dsi;
  private JAWT_X11DrawingSurfaceInfo x11dsi;
  
}

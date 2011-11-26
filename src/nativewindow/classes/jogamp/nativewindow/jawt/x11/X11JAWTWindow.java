/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package jogamp.nativewindow.jawt.x11;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.util.Point;

import jogamp.nativewindow.jawt.JAWT;
import jogamp.nativewindow.jawt.JAWTFactory;
import jogamp.nativewindow.jawt.JAWTUtil;
import jogamp.nativewindow.jawt.JAWTWindow;
import jogamp.nativewindow.jawt.JAWT_DrawingSurface;
import jogamp.nativewindow.jawt.JAWT_DrawingSurfaceInfo;
import jogamp.nativewindow.x11.X11Lib;

public class X11JAWTWindow extends JAWTWindow {

  public X11JAWTWindow(Object comp, AbstractGraphicsConfiguration config) {
    super(comp, config);
  }

  protected void invalidateNative() { }

  protected void attachSurfaceLayerImpl(final long layerHandle) {
      throw new UnsupportedOperationException("offscreen layer not supported");
  }
  protected void detachSurfaceLayerImpl(final long layerHandle) {
      throw new UnsupportedOperationException("offscreen layer not supported");
  }
  
  protected JAWT fetchJAWTImpl() throws NativeWindowException {
      return JAWTUtil.getJAWT(false); // no offscreen
  }
    
  protected int lockSurfaceImpl() throws NativeWindowException {
    int ret = NativeWindow.LOCK_SUCCESS;
    ds = getJAWT().GetDrawingSurface(component);
    if (ds == null) {
      // Widget not yet realized
      unlockSurfaceImpl();
      return LOCK_SURFACE_NOT_READY;
    }
    int res = ds.Lock();
    dsLocked = ( 0 == ( res & JAWTFactory.JAWT_LOCK_ERROR ) ) ;
    if (!dsLocked) {
      unlockSurfaceImpl();
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
      unlockSurfaceImpl();
      return LOCK_SURFACE_NOT_READY;
    }
    updateBounds(dsi.getBounds());
    x11dsi = (JAWT_X11DrawingSurfaceInfo) dsi.platformInfo(getJAWT());
    if (x11dsi == null) {
      unlockSurfaceImpl();
      return LOCK_SURFACE_NOT_READY;
    }
    drawable = x11dsi.getDrawable();
    if (drawable == 0) {
      unlockSurfaceImpl();
      return LOCK_SURFACE_NOT_READY;
    }
    return ret;
  }

  protected void unlockSurfaceImpl() throws NativeWindowException {
    if(null!=ds) {
        if (null!=dsi) {
            ds.FreeDrawingSurfaceInfo(dsi);
        }
        if (dsLocked) {
            ds.Unlock();
        }
        getJAWT().FreeDrawingSurface(ds);
    }
    ds = null;
    dsi = null;
    x11dsi = null;
  }

  protected Point getLocationOnScreenNativeImpl(int x, int y) {
    return X11Lib.GetRelativeLocation( getDisplayHandle(), getScreenIndex(), getWindowHandle(), 0 /*root win*/, x, y);
  }
  
  // Variables for lockSurface/unlockSurface
  private JAWT_DrawingSurface ds;
  private boolean dsLocked;
  private JAWT_DrawingSurfaceInfo dsi;
  private JAWT_X11DrawingSurfaceInfo x11dsi;
  
}

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
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package jogamp.nativewindow.jawt.macosx;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.util.Point;

import jogamp.nativewindow.jawt.JAWT;
import jogamp.nativewindow.jawt.JAWTFactory;
import jogamp.nativewindow.jawt.JAWTWindow;
import jogamp.nativewindow.jawt.JAWT_DrawingSurface;
import jogamp.nativewindow.jawt.JAWT_DrawingSurfaceInfo;

public class MacOSXJAWTWindow extends JAWTWindow {

  public MacOSXJAWTWindow(Object comp, AbstractGraphicsConfiguration config) {
    super(comp, config);
  }

  protected void validateNative() throws NativeWindowException {
  }

  protected int lockSurfaceImpl() throws NativeWindowException {
    int ret = NativeWindow.LOCK_SUCCESS;
    ds = JAWT.getJAWT().GetDrawingSurface(component);
    if (ds == null) {
      // Widget not yet realized
      unlockSurfaceImpl();
      return NativeWindow.LOCK_SURFACE_NOT_READY;
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
      ret = NativeWindow.LOCK_SURFACE_CHANGED;
    }
    if (firstLock) {
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            dsi = ds.GetDrawingSurfaceInfo();
            return null;
          }
        });
    } else {
      dsi = ds.GetDrawingSurfaceInfo();
    }
    if (dsi == null) {
      unlockSurfaceImpl();
      return NativeWindow.LOCK_SURFACE_NOT_READY;
    }
    firstLock = false;
    macosxdsi = (JAWT_MacOSXDrawingSurfaceInfo) dsi.platformInfo();
    if (macosxdsi == null) {
      unlockSurfaceImpl();
      return NativeWindow.LOCK_SURFACE_NOT_READY;
    }
    drawable = macosxdsi.getCocoaViewRef();

    if (drawable == 0) {
      unlockSurfaceImpl();
      return NativeWindow.LOCK_SURFACE_NOT_READY;
    } else {
      updateBounds(dsi.getBounds());
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
        JAWT.getJAWT().FreeDrawingSurface(ds);
    }
    ds = null;
    dsi = null;
    macosxdsi = null;
  }

  protected Point getLocationOnScreenImpl(int x, int y) {
    return null; // FIXME
  }

  // Variables for lockSurface/unlockSurface
  private JAWT_DrawingSurface ds;
  private boolean dsLocked;
  private JAWT_DrawingSurfaceInfo dsi;
  private JAWT_MacOSXDrawingSurfaceInfo macosxdsi;

  // Workaround for instance of 4796548
  private boolean firstLock = true;

}


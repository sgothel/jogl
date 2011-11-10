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

import java.awt.Component;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.Capabilities;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.SurfaceChangeable;
import javax.media.nativewindow.util.Point;

import jogamp.nativewindow.jawt.JAWT;
import jogamp.nativewindow.jawt.JAWTFactory;
import jogamp.nativewindow.jawt.JAWTUtil;
import jogamp.nativewindow.jawt.JAWTWindow;
import jogamp.nativewindow.jawt.JAWT_DrawingSurface;
import jogamp.nativewindow.jawt.JAWT_DrawingSurfaceInfo;
import jogamp.nativewindow.macosx.OSXUtil;

public class MacOSXJAWTWindow extends JAWTWindow implements SurfaceChangeable {
  public MacOSXJAWTWindow(Object comp, AbstractGraphicsConfiguration config) {
    super(comp, config);
    isOffscreenLayerSurface = JAWTUtil.isCachedJAWTUsingOffscreenLayer();
    dumpInfo();
  }

  protected void validateNative() throws NativeWindowException {
  }  

  protected void invalidateNative() {
      surfaceHandle=0;
      if(isOffscreenLayerSurface) {
          if(0 != drawable) {
              OSXUtil.DestroyNSWindow(drawable);
              drawable = 0;
          }
      }
  }

  public final boolean isOffscreenLayerSurface() { 
      return isOffscreenLayerSurface;
  }  
  public void attachSurfaceLayer(final long layerHandle) {
      if( !isOffscreenLayerSurface() ) {
          throw new NativeWindowException("Not using CALAYER");
      }
      int lockRes = lockSurface();
      if (NativeSurface.LOCK_SURFACE_NOT_READY >= lockRes) {
          throw new NativeWindowException("Could not lock layeredSurfaceHost: "+this);
      }
      try {
          if(DEBUG) {
            System.err.println("MacOSXJAWTWindow.attachSurfaceLayer(): 0x"+Long.toHexString(layerHandle));
          }
          OSXUtil.AddCASublayer(rootSurfaceLayerHandle, layerHandle);
      } finally {
          unlockSurface();
      }
  }
  public void detachSurfaceLayer(final long layerHandle) {
      if( !isOffscreenLayerSurface() ) {
          throw new NativeWindowException("Not using CALAYER");
      }
      int lockRes = lockSurface();
      if (NativeSurface.LOCK_SURFACE_NOT_READY >= lockRes) {
          throw new NativeWindowException("Could not lock layeredSurfaceHost: "+this);
      }
      try {
          if(DEBUG) {
            System.err.println("MacOSXJAWTWindow.detachSurfaceLayer(): 0x"+Long.toHexString(layerHandle));
          }
          OSXUtil.RemoveCASublayer(rootSurfaceLayerHandle, layerHandle);
      } finally {
          unlockSurface();
      }
  }
    
  public long getSurfaceHandle() {
    return isOffscreenLayerSurface ? surfaceHandle : super.getSurfaceHandle() ;
  }
  
  public void setSurfaceHandle(long surfaceHandle) {
      if( !isOffscreenLayerSurface() ) {
          throw new java.lang.UnsupportedOperationException("Not using CALAYER");
      }
      if(DEBUG) {
        System.err.println("MacOSXJAWTWindow.setSurfaceHandle(): 0x"+Long.toHexString(surfaceHandle));
      }
      this.surfaceHandle = surfaceHandle;
  }

  public void surfaceSizeChanged(int width, int height) {
      sscSet = true;
      sscWidth = width;
      sscHeight = height;
  }

  public int getWidth() {
    return sscSet ? sscWidth : super.getWidth();
  }

  public int getHeight() {
    return sscSet ? sscHeight: super.getHeight();
  }

  protected int lockSurfaceImpl() throws NativeWindowException {
    int ret = NativeWindow.LOCK_SURFACE_NOT_READY;
    if(null == ds) {
        final JAWT jawt = JAWT.getJAWT();
        ds = jawt.GetDrawingSurface(component);
        if (ds == null) {
          // Widget not yet realized
          unlockSurfaceImpl();
          return NativeWindow.LOCK_SURFACE_NOT_READY;
        }
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
    if(null == dsi) {
        if (firstLock) {
          AccessController.doPrivileged(new PrivilegedAction<Object>() {
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
    }
    updateBounds(dsi.getBounds());
    if (DEBUG && firstLock) {
      dumpInfo();
    }
    firstLock = false;
    if( !isOffscreenLayerSurface ) {
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
          ret = NativeWindow.LOCK_SUCCESS;
        }
    } else {
        /**
         * Only create a fake invisible NSWindow for the drawable handle
         * to please frameworks requiring such (eg. NEWT).
         * 
         * The actual surface/ca-layer shall be created/attached 
         * by the upper framework (JOGL) since they require more information. 
         */
        if(0 == drawable) {
            drawable = OSXUtil.CreateNSWindow(0, 0, getBounds().getWidth(), getBounds().getHeight());
            if(0 == drawable) {
              unlockSurfaceImpl();
              throw new NativeWindowException("Unable to created dummy NSWindow (layered case)");
            }
            // fix caps reflecting offscreen!
            Capabilities caps = (Capabilities) config.getChosenCapabilities().cloneMutable();
            caps.setOnscreen(false);
            config.setChosenCapabilities(caps);
        } 
        if(0 == rootSurfaceLayerHandle) {
            rootSurfaceLayerHandle = OSXUtil.CreateCALayer();
            if(0 == rootSurfaceLayerHandle) {
              OSXUtil.DestroyNSWindow(drawable);
              drawable = 0;
              unlockSurfaceImpl();
              throw new NativeWindowException("Could not create root CALayer: "+this);                
            }
            if(!OSXUtil.AttachJAWTSurfaceLayer(dsi, rootSurfaceLayerHandle)) {
              OSXUtil.DestroyCALayer(rootSurfaceLayerHandle);
              rootSurfaceLayerHandle = 0;
              OSXUtil.DestroyNSWindow(drawable);
              drawable = 0;
              unlockSurfaceImpl();
              throw new NativeWindowException("Could not attach JAWT surfaceLayerHandle: "+this);
            }
        }
        ret = NativeWindow.LOCK_SUCCESS;
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
  }

  private void dumpInfo() {
      System.err.println("MaxOSXJAWTWindow: 0x"+Integer.toHexString(this.hashCode())+" - thread: "+Thread.currentThread().getName());
      // System.err.println(this);
      System.err.println("JAWT version: 0x"+Integer.toHexString(JAWT.getJAWT().getVersionCached())+
                         ", CA_LAYER: "+ (0!=(JAWT.getJAWT().getVersionCached() & JAWT.JAWT_MACOSX_USE_CALAYER))+
                         ", isLayeredSurface "+isOffscreenLayerSurface()+", bounds "+bounds);
      // Thread.dumpStack();
  }
  
  protected Point getLocationOnScreenImpl(final int x0, final int y0) {
      int x = x0; 
      int y = y0;
      Component c = component;
      while(null != c) {
          x += c.getX();
          y += c.getY();
          c = c.getParent();
      }
      return new Point(x, y);
  }

  // Variables for lockSurface/unlockSurface
  private JAWT_DrawingSurface ds;
  private boolean dsLocked;
  private JAWT_DrawingSurfaceInfo dsi;
  
  private JAWT_MacOSXDrawingSurfaceInfo macosxdsi;
  
  private final boolean isOffscreenLayerSurface;
  private long rootSurfaceLayerHandle = 0; // is autoreleased, once it is attached to the JAWT_SurfaceLayer
  
  private long surfaceHandle = 0;
  private int sscWidth, sscHeight;
  private boolean sscSet = false;
   
  // Workaround for instance of 4796548
  private boolean firstLock = true;

}


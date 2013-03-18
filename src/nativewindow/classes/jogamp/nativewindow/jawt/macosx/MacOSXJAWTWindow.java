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

import java.nio.Buffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.Capabilities;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.MutableSurface;
import javax.media.nativewindow.util.Point;

import com.jogamp.nativewindow.awt.JAWTWindow;

import jogamp.nativewindow.jawt.JAWT;
import jogamp.nativewindow.jawt.JAWTFactory;
import jogamp.nativewindow.jawt.JAWTUtil;
import jogamp.nativewindow.jawt.JAWT_DrawingSurface;
import jogamp.nativewindow.jawt.JAWT_DrawingSurfaceInfo;
import jogamp.nativewindow.jawt.macosx.JAWT_MacOSXDrawingSurfaceInfo;
import jogamp.nativewindow.macosx.OSXUtil;

public class MacOSXJAWTWindow extends JAWTWindow implements MutableSurface {
  public MacOSXJAWTWindow(Object comp, AbstractGraphicsConfiguration config) {
    super(comp, config);
    if(DEBUG) {
        dumpInfo();
    }
  }

  @Override
  protected void invalidateNative() {
      if(DEBUG) {
          System.err.println("MacOSXJAWTWindow.invalidateNative(): osh-enabled "+isOffscreenLayerSurfaceEnabled()+
                             ", osd-set "+offscreenSurfaceDrawableSet+
                             ", osd "+toHexString(offscreenSurfaceDrawable)+
                             ", osl "+toHexString(getAttachedSurfaceLayer())+
                             ", rsl "+toHexString(rootSurfaceLayer)+
                             ", wh "+toHexString(windowHandle)+" - "+Thread.currentThread().getName());
      }
      offscreenSurfaceDrawable=0;
      offscreenSurfaceDrawableSet=false;
      if( isOffscreenLayerSurfaceEnabled() ) {
          if(0 != windowHandle) {
              OSXUtil.DestroyNSWindow(windowHandle);
          }
          if(0 != rootSurfaceLayer) {
              final JAWT jawt = getJAWT();
              if( null != jawt ) {
                  final JAWT_DrawingSurface ds = jawt.GetDrawingSurface(component);
                  if (ds != null) {
                      if ( 0 == ( ds.Lock() & JAWTFactory.JAWT_LOCK_ERROR ) ) {
                          JAWT_DrawingSurfaceInfo dsi = null;
                          try {
                              dsi = ds.GetDrawingSurfaceInfo();
                              try {
                                  UnsetJAWTRootSurfaceLayer0(dsi.getBuffer(), rootSurfaceLayer);
                              } catch (Exception e) {                                  
                                  System.err.println("Error clearing JAWT rootSurfaceLayerHandle "+toHexString(rootSurfaceLayer));
                                  e.printStackTrace();
                              }
                          } finally {
                              if ( null != dsi ) {
                                  ds.FreeDrawingSurfaceInfo(dsi);
                              }
                              ds.Unlock();
                          }
                      }
                      jawt.FreeDrawingSurface(ds);
                  }
              }
              
              OSXUtil.RunOnMainThread(false, new Runnable() {
                  public void run() {
                      OSXUtil.DestroyCALayer(rootSurfaceLayer, false);
                      rootSurfaceLayer = 0;
                  }
              });
          }
      }
      windowHandle=0;
  }
  
  @Override
  protected void attachSurfaceLayerImpl(final long layerHandle) {
      OSXUtil.AddCASublayer(rootSurfaceLayer, layerHandle, getWidth(), getHeight());
  }
  
  @Override
  protected void layoutSurfaceLayerImpl(long layerHandle, int width, int height) {
      if(DEBUG) {
        System.err.println("JAWTWindow.layoutSurfaceLayerImpl: "+toHexString(layerHandle) + ", "+width+"x"+height+"; "+this);
      }
      OSXUtil.FixCALayerLayout(rootSurfaceLayer, layerHandle, width, height);
  }
  
  @Override
  protected void detachSurfaceLayerImpl(final long layerHandle, final Runnable detachNotify) {
      OSXUtil.RunOnMainThread(false, new Runnable() {
          public void run() {
              final long l = MacOSXJAWTWindow.this.getAttachedSurfaceLayer();
              if( 0 != l ) {
                  detachNotify.run();
                  OSXUtil.RemoveCASublayer(rootSurfaceLayer, layerHandle, false);
              }
          } } );
  }
  
  @Override
  public final long getWindowHandle() {
    return windowHandle;
  }
  
  @Override
  public final long getSurfaceHandle() {
    return offscreenSurfaceDrawableSet ? offscreenSurfaceDrawable : drawable /* super.getSurfaceHandle() */ ;
  }
  
  public void setSurfaceHandle(long surfaceHandle) {
      if( !isOffscreenLayerSurfaceEnabled() ) {
          throw new java.lang.UnsupportedOperationException("Not using CALAYER");
      }
      if(DEBUG) {
        System.err.println("MacOSXJAWTWindow.setSurfaceHandle(): "+toHexString(surfaceHandle));
      }
      this.offscreenSurfaceDrawable = surfaceHandle;
      this.offscreenSurfaceDrawableSet = true;
  }

  protected JAWT fetchJAWTImpl() throws NativeWindowException {
       // use offscreen if supported and [ applet or requested ]
      return JAWTUtil.getJAWT(getShallUseOffscreenLayer() || isApplet());
  }
  
  protected int lockSurfaceImpl() throws NativeWindowException {
    int ret = NativeWindow.LOCK_SURFACE_NOT_READY;
    ds = getJAWT().GetDrawingSurface(component);
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
    updateBounds(dsi.getBounds());
    if (DEBUG && firstLock ) {
      dumpInfo();
    }
    firstLock = false;
    if( !isOffscreenLayerSurfaceEnabled() ) {
        macosxdsi = (JAWT_MacOSXDrawingSurfaceInfo) dsi.platformInfo(getJAWT());
        if (macosxdsi == null) {
          unlockSurfaceImpl();
          return NativeWindow.LOCK_SURFACE_NOT_READY;
        }
        drawable = macosxdsi.getCocoaViewRef();
    
        if (drawable == 0) {
          unlockSurfaceImpl();
          return NativeWindow.LOCK_SURFACE_NOT_READY;
        } else {
          windowHandle = OSXUtil.GetNSWindow(drawable);
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
        String errMsg = null;
        if(0 == drawable) {
            windowHandle = OSXUtil.CreateNSWindow(0, 0, 64, 64);
            if(0 == windowHandle) {
              errMsg = "Unable to create dummy NSWindow (layered case)";
            } else {
                drawable = OSXUtil.GetNSView(windowHandle);
                if(0 == drawable) {
                  errMsg = "Null NSView of NSWindow "+toHexString(windowHandle);
                }
            }
            if(null == errMsg) {
                // Fix caps reflecting offscreen! (no GL available here ..)
                final Capabilities caps = (Capabilities) getGraphicsConfiguration().getChosenCapabilities().cloneMutable();
                caps.setOnscreen(false);
                setChosenCapabilities(caps);
            }
        }
        if(null == errMsg) {
            if(0 == rootSurfaceLayer) {        
                rootSurfaceLayer = OSXUtil.CreateCALayer(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
                if(0 == rootSurfaceLayer) {
                  errMsg = "Could not create root CALayer";                
                } else {
                    try {
                        SetJAWTRootSurfaceLayer0(dsi.getBuffer(), rootSurfaceLayer);
                    } catch(Exception e) {
                        errMsg = "Could not set JAWT rootSurfaceLayerHandle "+toHexString(rootSurfaceLayer)+", cause: "+e.getMessage();
                    }
                }
            }
        }
        if(null != errMsg) {
            if(0 != rootSurfaceLayer) {
              OSXUtil.DestroyCALayer(rootSurfaceLayer, true);
              rootSurfaceLayer = 0;
            }
            if(0 != windowHandle) {
              OSXUtil.DestroyNSWindow(windowHandle);
              windowHandle = 0;
            }
            drawable = 0;
            unlockSurfaceImpl();
            throw new NativeWindowException(errMsg+": "+this);
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
        getJAWT().FreeDrawingSurface(ds);
    }
    ds = null;
    dsi = null;
  }

  private void dumpInfo() {
      System.err.println("MaxOSXJAWTWindow: 0x"+Integer.toHexString(this.hashCode())+" - thread: "+Thread.currentThread().getName());
      dumpJAWTInfo();
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * On OS X locking the surface at this point (ie after creation and for location validation)
   * is 'tricky' since the JVM traverses through many threads and crashes at:
   *   lockSurfaceImpl() {
   *      ..
   *      ds = getJAWT().GetDrawingSurface(component);
   * due to a SIGSEGV.
   * 
   * Hence we have some threading / sync issues with the native JAWT implementation.
   * </p>      
   */
  @Override
  public Point getLocationOnScreen(Point storage) {     
      return getLocationOnScreenNonBlocking(storage, component);     
  }  
  protected Point getLocationOnScreenNativeImpl(final int x0, final int y0) { return null; }
  
  /** 
   * Set the given root CALayer in the JAWT surface
   */  
  private static native void SetJAWTRootSurfaceLayer0(Buffer jawtDrawingSurfaceInfoBuffer, long caLayer);
  
  /** 
   * Unset the given root CALayer in the JAWT surface, passing the NIO DrawingSurfaceInfo buffer
   */  
  private static native void UnsetJAWTRootSurfaceLayer0(Buffer jawtDrawingSurfaceInfoBuffer, long caLayer);
  
  // Variables for lockSurface/unlockSurface
  private JAWT_DrawingSurface ds;
  private boolean dsLocked;
  private JAWT_DrawingSurfaceInfo dsi;
  
  private JAWT_MacOSXDrawingSurfaceInfo macosxdsi;
  
  private volatile long rootSurfaceLayer = 0; // attached to the JAWT_SurfaceLayer
  
  private long windowHandle = 0;
  private long offscreenSurfaceDrawable = 0;
  private boolean offscreenSurfaceDrawableSet = false;
   
  // Workaround for instance of 4796548
  private boolean firstLock = true;

}


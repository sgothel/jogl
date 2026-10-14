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
import java.awt.GraphicsConfiguration;
import java.nio.Buffer;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.Capabilities;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.MutableSurface;
import com.jogamp.nativewindow.util.Point;

import com.jogamp.common.util.PropertyAccess;
import com.jogamp.nativewindow.awt.JAWTWindow;

import jogamp.nativewindow.Debug;
import jogamp.nativewindow.awt.AWTMisc;
import jogamp.nativewindow.jawt.JAWT;
import jogamp.nativewindow.jawt.JAWTFactory;
import jogamp.nativewindow.jawt.JAWTUtil;
import jogamp.nativewindow.jawt.JAWT_DrawingSurface;
import jogamp.nativewindow.jawt.JAWT_DrawingSurfaceInfo;
import jogamp.nativewindow.macosx.OSXUtil;
import jogamp.nativewindow.macosx.OSXUtil.WinAndView;

public class MacOSXJAWTWindow extends JAWTWindow implements MutableSurface {
    /** May lead to deadlock, due to AWT pos comparison .. don't enable for Applets! */
    private static final boolean DEBUG_CALAYER_POS_CRITICAL;
    private static final boolean DEBUG_OSX_LOCK;
    private static final boolean DEBUG_OSX_LOCK_STACK;

    static {
        Debug.initSingleton();
        DEBUG_CALAYER_POS_CRITICAL = PropertyAccess.isPropertyDefined("nativewindow.debug.JAWT.OSXCALayerPos", true /* jnlpAlias */);
        DEBUG_OSX_LOCK = Debug.debug("JAWT.OSXLock");
        DEBUG_OSX_LOCK_STACK = Debug.debug("JAWT.OSXLock.Stack");
    }

  public MacOSXJAWTWindow(final Object comp, final AbstractGraphicsConfiguration config) {
    super(comp, config);
    if(DEBUG) {
        dumpInfo();
    }
  }

  @Override
  protected void invalidateNative(final long _offscreenSurfaceLayer) {
      if(DEBUG) {
          System.err.println("MacOSXJAWTWindow.invalidateNative(): osh-enabled "+isOffscreenLayerSurfaceEnabled()+
                             ", osd-set "+offscreenSurfaceDrawableSet+
                             ", osd "+toHexString(offscreenSurfaceDrawable)+
                             ", osl "+toHexString(_offscreenSurfaceLayer)+
                             ", jsl "+toHexString(jawtSurfaceLayersHandle)+
                             ", rsl "+toHexString(rootSurfaceLayer)+
                             ", wh "+toHexString(windowHandle)+" - "+Thread.currentThread().getName());
      }
      offscreenSurfaceDrawable=0;
      offscreenSurfaceDrawableSet=false;
      if( isOffscreenLayerSurfaceEnabled() ) {
          final long _windowHandle = windowHandle;
          windowHandle = 0;
          final long _rootSurfaceLayer = rootSurfaceLayer;
          rootSurfaceLayer = 0;
          final long _jawtSurfaceLayersHandle = jawtSurfaceLayersHandle;
          jawtSurfaceLayersHandle = 0;
          OSXUtil.RunOnMainThread(false /* wait */, true /* kickNSApp */, new Runnable() {
              @Override
              public void run() {
                  if(0 != _windowHandle) {
                      OSXUtil.DestroyNSWindow(_windowHandle);
                  }
                  if( 0 != _rootSurfaceLayer && 0 != _offscreenSurfaceLayer ) { // Bug 1389
                      // throws if null == _rootSurfaceLayer
                      OSXUtil.RemoveCASublayer(_rootSurfaceLayer, _offscreenSurfaceLayer, true);
                  }
                  if( 0 != _jawtSurfaceLayersHandle) {
                      // null rootSurfaceLayer OK
                      UnsetJAWTRootSurfaceLayer0(_jawtSurfaceLayersHandle, _rootSurfaceLayer);
                  }
                  if( 0 != _rootSurfaceLayer ) {
                      OSXUtil.DestroyCALayer(_rootSurfaceLayer);
                  }
              }
          });
      }
      windowHandle=0;
  }

  @Override
  protected void attachSurfaceLayerImpl(final long _offscreenSurfaceLayer) {
      OSXUtil.RunOnMainThread(false /* wait */, false /* kickNSApp */, new Runnable() {
              @Override
              public void run() {
                  // AWT position is top-left w/ insets, where CALayer position is bottom/left from root CALayer w/o insets.
                  // Determine p0: components location on screen w/o insets.
                  // CALayer position will be determined in native code.
                  // See detailed description in {@link JAWTUtil#JAWT_OSX_CALAYER_QUIRK_LAYOUT}
                  final Point p0 = new Point();
                  final Component outterComp = AWTMisc.getLocationOnScreenNonBlocking(p0, component, DEBUG);
                  final java.awt.Insets outterInsets = AWTMisc.getInsets(outterComp, true);
                  final Point p1 = (Point)p0.cloneMutable();
                  p1.translate(-outterComp.getX(), -outterComp.getY());
                  if( null != outterInsets ) {
                      p1.translate(-outterInsets.left, -outterInsets.top);
                  }

                  if( DEBUG_CALAYER_POS_CRITICAL ) {
                      final java.awt.Point pA0 = component.getLocationOnScreen();
                      final Point pA1 = new Point(pA0.x, pA0.y);
                      pA1.translate(-outterComp.getX(), -outterComp.getY());
                      if( null != outterInsets ) {
                          pA1.translate(-outterInsets.left, -outterInsets.top);
                      }
                      System.err.println("JAWTWindow.attachSurfaceLayerImpl: "+toHexString(_offscreenSurfaceLayer) + ", [ins "+outterInsets+"], pA "+pA0+" -> "+pA1+
                              ", p0 "+p0+" -> "+p1+", bounds "+jawt_surface_bounds);
                  } else if( DEBUG ) {
                      System.err.println("JAWTWindow.attachSurfaceLayerImpl: "+toHexString(_offscreenSurfaceLayer) + ", [ins "+outterInsets+"], p0 "+p0+" -> "+p1+", bounds "+jawt_surface_bounds);
                  }
                  // HiDPI: uniform pixel scale
                  OSXUtil.AddCASublayer(rootSurfaceLayer, _offscreenSurfaceLayer, p1.getX(), p1.getY(), getWidth(), getHeight(), getPixelScaleX(), JAWTUtil.getOSXCALayerQuirks());
              } } );
  }

  @Override
  protected void layoutSurfaceLayerImpl(final boolean visible) {
      final int caLayerQuirks = JAWTUtil.getOSXCALayerQuirks();
      // AWT position is top-left w/ insets, where CALayer position is bottom/left from root CALayer w/o insets.
      // Determine p0: components location on screen w/o insets.
      // CALayer position will be determined in native code.
      // See detailed description in {@link JAWTUtil#JAWT_OSX_CALAYER_QUIRK_LAYOUT}
      final Point p0 = new Point();
      final Component outterComp = AWTMisc.getLocationOnScreenNonBlocking(p0, component, DEBUG);
      final java.awt.Insets outterInsets = AWTMisc.getInsets(outterComp, true);
      final Point p1 = (Point)p0.cloneMutable();
      p1.translate(-outterComp.getX(), -outterComp.getY());
      if( null != outterInsets ) {
          p1.translate(-outterInsets.left, -outterInsets.top);
      }

      if( DEBUG_CALAYER_POS_CRITICAL ) {
          final java.awt.Point pA0 = component.getLocationOnScreen();
          final Point pA1 = new Point(pA0.x, pA0.y);
          pA1.translate(-outterComp.getX(), -outterComp.getY());
          if( null != outterInsets ) {
              pA1.translate(-outterInsets.left, -outterInsets.top);
          }
          System.err.println("JAWTWindow.layoutSurfaceLayerImpl: "+toHexString(getAttachedSurfaceLayer()) + ", quirks "+caLayerQuirks+", visible "+visible+
                  ", [ins "+outterInsets+"], pA "+pA0+" -> "+pA1+
                  ", p0 "+p0+" -> "+p1+", bounds "+jawt_surface_bounds);
      } else if( DEBUG ) {
          System.err.println("JAWTWindow.layoutSurfaceLayerImpl: "+toHexString(getAttachedSurfaceLayer()) + ", quirks "+caLayerQuirks+", visible "+visible+
                  ", [ins "+outterInsets+"], p0 "+p0+" -> "+p1+", bounds "+jawt_surface_bounds);
      }
      OSXUtil.RunOnMainThread(false /* wait */, false, new Runnable() {
          @Override
          public void run() {
              final long osl = getAttachedSurfaceLayer();
              if( 0 != rootSurfaceLayer && 0 != osl ) {
                  OSXUtil.FixCALayerLayout(rootSurfaceLayer, osl, visible, p1.getX(), p1.getY(), getWidth(), getHeight(), caLayerQuirks);
              }
          }
      });
  }

  @Override
  protected void detachSurfaceLayerImpl(final long _offscreenSurfaceLayer) {
      OSXUtil.RunOnMainThread(false /* wait */, true /* kickNSApp */, new Runnable() {
              @Override
              public void run() {
                  if( 0 != rootSurfaceLayer && 0 != _offscreenSurfaceLayer ) { // Bug 1389
                      // throws if null == rootSurfaceLayer
                      OSXUtil.RemoveCASublayer(rootSurfaceLayer, _offscreenSurfaceLayer, false);
                  }
              } });
  }

  @Override
  public final long getWindowHandle() {
    return windowHandle;
  }

  @Override
  public final long getSurfaceHandle() {
    return offscreenSurfaceDrawableSet ? offscreenSurfaceDrawable : drawable /* super.getSurfaceHandle() */ ;
  }

  @Override
  public void setSurfaceHandle(final long surfaceHandle) {
      if( !isOffscreenLayerSurfaceEnabled() ) {
          throw new java.lang.UnsupportedOperationException("Not using CALAYER");
      }
      if(DEBUG) {
        System.err.println("MacOSXJAWTWindow.setSurfaceHandle(): "+toHexString(surfaceHandle));
      }
      this.offscreenSurfaceDrawable = surfaceHandle;
      this.offscreenSurfaceDrawableSet = true;
  }

  @Override
  protected JAWT fetchJAWTImpl() throws NativeWindowException {
       // use offscreen if supported and [ applet or requested ]
      return JAWTUtil.getJAWT(getShallUseOffscreenLayer());
  }

  @Override
  protected int lockSurfaceImpl(final GraphicsConfiguration gc) throws NativeWindowException {
      int ret = NativeSurface.LOCK_SURFACE_NOT_READY;
      /**
       * Offscreen-layer (JAWT CALayer) path: Create an invisible dummy NSWindow/NSView pair once.
       * <p>
       * NEWT (and other upper layers) require a non-zero NSView handle as the {@code drawable} handle,
       * even though actual rendering is done via the JAWT offscreen-layer / CALayer.
       * </p>
       * <p>
       * This operation must run on the AppKit main thread and may be executed while the AWT EDT
       * is in the middle of peer realization / layout. On macOS 13+ this can lead to a deadlock
       * between AWT EDT and NEWT EDT if we keep {@link JAWTWindow}'s surface lock and the
       * {@link com.jogamp.nativewindow.AbstractGraphicsDevice} lock held while synchronously waiting:
       * the AWT nested event loop (used to avoid freezing the EDT, see Bug 1478) can re-enter NEWT
       * parenting code, while NEWT simultaneously tries to lock the same surface.
       * </p>
       * <p>
       * To avoid this lock inversion, temporarily drop both locks <em>before</em> touching the JAWT
       * {@link JAWT_DrawingSurface}, perform the main-thread work, then reacquire the locks and continue.
       * </p>
       */
      if( isOffscreenLayerSurfaceEnabled() && 0 == drawable ) {
          final com.jogamp.nativewindow.AbstractGraphicsDevice adevice = getGraphicsConfiguration().getScreen().getDevice();
          final com.jogamp.common.util.locks.RecursiveLock surfaceLock = getLock();
          final WinAndView wv;

          if( DEBUG_OSX_LOCK ) {
              System.err.println("MacOSXJAWTWindow.lockSurfaceImpl: create dummy NSWindow/NSView start, thread "+Thread.currentThread().getName()+
                      ", drawable 0x"+Long.toHexString(drawable)+", windowHandle 0x"+Long.toHexString(windowHandle)+
                      ", surfaceLock[hold "+surfaceLock.getHoldCount()+", qlen "+surfaceLock.getQueueLength()+", owner "+surfaceLock.getOwner()+"]"+
                      ", adevice "+adevice);
              if( DEBUG_OSX_LOCK_STACK ) {
                  com.jogamp.common.ExceptionUtils.dumpStack(System.err);
              }
          }

          // Drop locks in reverse acquisition order (device -> surface) and reacquire (surface -> device).
          adevice.unlock();
          surfaceLock.unlock();
          try {
              if( DEBUG_OSX_LOCK ) {
                  System.err.println("MacOSXJAWTWindow.lockSurfaceImpl: create dummy NSWindow/NSView - dropped locks, calling OSXUtil.CreateNSWindow2(..)");
              }
              wv = OSXUtil.CreateNSWindow2(0, 0, 64, 64);
          } finally {
              surfaceLock.lock();
              adevice.lock();
          }

          if( DEBUG_OSX_LOCK ) {
              System.err.println("MacOSXJAWTWindow.lockSurfaceImpl: create dummy NSWindow/NSView - reacquired locks, got win 0x"+Long.toHexString(wv.win)+
                      ", view 0x"+Long.toHexString(wv.view)+", thread "+Thread.currentThread().getName()+
                      ", surfaceLock[hold "+surfaceLock.getHoldCount()+", qlen "+surfaceLock.getQueueLength()+", owner "+surfaceLock.getOwner()+"]");
          }

          // Another thread may have initialized while the locks were dropped.
          if( 0 == drawable ) {
              windowHandle = wv.win;
              if(0 == windowHandle) {
                  throw new NativeWindowException("Unable to create dummy NSWindow (layered case): "+this);
              }
              drawable = wv.view;
              if(0 == drawable) {
                  final long createdWin = windowHandle;
                  windowHandle = 0;
                  OSXUtil.RunOnMainThread(false /* wait */, false, new Runnable() {
                      @Override
                      public void run() {
                          OSXUtil.DestroyNSWindow(createdWin);
                      } } );
                  throw new NativeWindowException("Null NSView of NSWindow "+toHexString(createdWin)+": "+this);
              }
              if( DEBUG_OSX_LOCK ) {
                  System.err.println("MacOSXJAWTWindow.lockSurfaceImpl: installed dummy NSWindow/NSView win 0x"+Long.toHexString(windowHandle)+
                          ", view 0x"+Long.toHexString(drawable)+", thread "+Thread.currentThread().getName());
              }

              // Fix caps reflecting offscreen! (no GL available here ..)
              final Capabilities caps = (Capabilities) getGraphicsConfiguration().getChosenCapabilities().cloneMutable();
              caps.setOnscreen(false);
              setChosenCapabilities(caps);
          } else if( 0 != wv.win ) {
              // Cleanup: We created a dummy NSWindow, but another thread won the race and installed its own.
              // Destroy the now-unreferenced window on the AppKit main thread.
              final long createdWin = wv.win;
              if( DEBUG_OSX_LOCK ) {
                  System.err.println("MacOSXJAWTWindow.lockSurfaceImpl: discarding extra dummy NSWindow 0x"+Long.toHexString(createdWin)+
                          ", thread "+Thread.currentThread().getName());
              }
              OSXUtil.RunOnMainThread(false /* wait */, false /* kickNSApp */, new Runnable() {
                  @Override
                  public void run() {
                      OSXUtil.DestroyNSWindow(createdWin);
                  } } );
          }
      }
      ds = getJAWT().GetDrawingSurface(component);
      if (ds == null) {
          // Widget not yet realized
          unlockSurfaceImpl();
          return NativeSurface.LOCK_SURFACE_NOT_READY;
      }
      final int res = ds.Lock();
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
          ret = NativeSurface.LOCK_SURFACE_CHANGED;
      }
      if (firstLock) {
          dsi = ds.GetDrawingSurfaceInfo();
      } else {
          dsi = ds.GetDrawingSurfaceInfo();
      }
      if (dsi == null) {
          unlockSurfaceImpl();
          return NativeSurface.LOCK_SURFACE_NOT_READY;
      }
      updateLockedData(dsi.getBounds(), gc);
      if (DEBUG && firstLock ) {
          dumpInfo();
      }
      firstLock = false;
      if( !isOffscreenLayerSurfaceEnabled() ) {
          macosxdsi = (JAWT_MacOSXDrawingSurfaceInfo) dsi.platformInfo(getJAWT());
          if (macosxdsi == null) {
              unlockSurfaceImpl();
              return NativeSurface.LOCK_SURFACE_NOT_READY;
          }
          drawable = macosxdsi.getCocoaViewRef();

          if (drawable == 0) {
              unlockSurfaceImpl();
              return NativeSurface.LOCK_SURFACE_NOT_READY;
          } else {
              windowHandle = OSXUtil.GetNSWindow(drawable);
              ret = NativeSurface.LOCK_SUCCESS;
          }
      } else {
          /**
           * The fake invisible NSWindow for the drawable handle
           * to please frameworks requiring such (eg. NEWT) has been created above.
           *
           * The actual surface/ca-layer shall be created/attached
           * by the upper framework (JOGL) since they require more information.
           */
          jawtSurfaceLayersHandle = GetJAWTSurfaceLayersHandle0(dsi.getBuffer());
          OSXUtil.RunOnMainThread(false /* wait */, false, new Runnable() {
              @Override
              public void run() {
                  String errMsg = null;
                  if(0 == rootSurfaceLayer && 0 != jawtSurfaceLayersHandle) {
                      rootSurfaceLayer = OSXUtil.CreateCALayer(jawt_surface_bounds.getWidth(), jawt_surface_bounds.getHeight(), getPixelScaleX()); // HiDPI: uniform pixel scale
                      if(0 == rootSurfaceLayer) {
                          errMsg = "Could not create root CALayer";
                      } else {
                          try {
                              SetJAWTRootSurfaceLayer0(jawtSurfaceLayersHandle, rootSurfaceLayer);
                          } catch(final Exception e) {
                              errMsg = "Could not set JAWT rootSurfaceLayerHandle "+toHexString(rootSurfaceLayer)+", cause: "+e.getMessage();
                          }
                      }
                      if(null != errMsg) {
                          if(0 != rootSurfaceLayer) {
                              OSXUtil.DestroyCALayer(rootSurfaceLayer);
                              rootSurfaceLayer = 0;
                          }
                          throw new NativeWindowException(errMsg+": "+MacOSXJAWTWindow.this);
                      }
                  }
              } } );
          ret = NativeSurface.LOCK_SUCCESS;
      }
      return ret;
  }

  @Override
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
      if( null == storage ) {
          storage = new Point();
      }
      AWTMisc.getLocationOnScreenNonBlocking(storage, component, DEBUG);
      return storage;
  }
  @Override
  protected Point getLocationOnScreenNativeImpl(final int x0, final int y0) { return null; }


  private static native long GetJAWTSurfaceLayersHandle0(Buffer jawtDrawingSurfaceInfoBuffer);

  /**
   * Set the given root CALayer in the JAWT surface
   */
  private static native void SetJAWTRootSurfaceLayer0(long jawtSurfaceLayersHandle, long rootCALayer);

  /**
   * Unset the given root CALayer in the JAWT surface, passing the NIO DrawingSurfaceInfo buffer
   */
  private static native void UnsetJAWTRootSurfaceLayer0(long jawtSurfaceLayersHandle, long rootCALayer);

  // Variables for lockSurface/unlockSurface
  private JAWT_DrawingSurface ds;
  private boolean dsLocked;
  private JAWT_DrawingSurfaceInfo dsi;
  private long jawtSurfaceLayersHandle;

  private JAWT_MacOSXDrawingSurfaceInfo macosxdsi;

  private volatile long rootSurfaceLayer = 0; // attached to the JAWT_SurfaceLayer

  private long windowHandle = 0;
  private long offscreenSurfaceDrawable = 0;
  private boolean offscreenSurfaceDrawableSet = false;

  // Workaround for instance of 4796548
  private boolean firstLock = true;

}


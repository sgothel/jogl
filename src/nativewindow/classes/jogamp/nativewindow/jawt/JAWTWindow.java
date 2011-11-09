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

package jogamp.nativewindow.jawt;

import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;

import java.awt.Component;
import java.awt.Window;
import java.applet.Applet;
import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.SurfaceUpdatedListener;
import javax.media.nativewindow.awt.AWTGraphicsConfiguration;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.nativewindow.util.Point;
import javax.media.nativewindow.util.Rectangle;
import javax.media.nativewindow.util.RectangleImmutable;

import jogamp.nativewindow.SurfaceUpdatedHelper;

public abstract class JAWTWindow implements NativeWindow {
  protected static final boolean DEBUG = JAWTUtil.DEBUG;

  // lifetime: forever
  protected Component component;
  protected boolean isApplet;
  protected AWTGraphicsConfiguration config;
  private SurfaceUpdatedHelper surfaceUpdatedHelper = new SurfaceUpdatedHelper();

  // lifetime: valid after lock, forever until invalidate
  protected long drawable;
  protected Rectangle bounds;

  /**
   * Constructed by {@link jogamp.nativewindow.NativeWindowFactoryImpl#getNativeWindow(Object, AbstractGraphicsConfiguration)}
   * via this platform's specialization (X11, OSX, Windows, ..).
   * 
   * @param comp
   * @param config
   */
  protected JAWTWindow(Object comp, AbstractGraphicsConfiguration config) {
    if (config == null) {
        throw new NativeWindowException("Error: AbstractGraphicsConfiguration is null");
    }
    if(! ( config instanceof AWTGraphicsConfiguration ) ) {
        throw new NativeWindowException("Error: AbstractGraphicsConfiguration is not an AWTGraphicsConfiguration");
    }
    this.config = (AWTGraphicsConfiguration) config;
    init((Component)comp);
  }

  private void init(Component windowObject) throws NativeWindowException {
    invalidate();
    this.component = windowObject;
    this.isApplet = false;
    while(!isApplet && null != windowObject) {
        isApplet = windowObject instanceof Applet;
        windowObject = windowObject.getParent();
    }
    if(isApplet) {
        JAWTUtil.setJAWTVersionFlags(true); // useOffScreenLayerIfAvailable := true
    } else {
        // test
        JAWTUtil.setJAWTVersionFlags(true); // useOffScreenLayerIfAvailable := true
    }
    validateNative();
  }
  protected abstract void validateNative() throws NativeWindowException;

  protected synchronized void invalidate() {
    invalidateNative();
    drawable= 0;
    bounds = new Rectangle();
  }
  protected abstract void invalidateNative();

  protected final void updateBounds(JAWT_Rectangle jawtBounds) {
    bounds.setX(jawtBounds.getX());
    bounds.setY(jawtBounds.getY());
    bounds.setWidth(jawtBounds.getWidth());
    bounds.setHeight(jawtBounds.getHeight());
  }

  /** @return the JAWT_DrawingSurfaceInfo's (JAWT_Rectangle) bounds, updated with lock */
  public final RectangleImmutable getBounds() { return bounds; }
  
  public final InsetsImmutable getInsets() { return Insets.getZero(); }

  public final Component getAWTComponent() {
    return component;
  }
  
  public final boolean isApplet() {
      return isApplet;
  }

  //
  // SurfaceUpdateListener
  //

  public void addSurfaceUpdatedListener(SurfaceUpdatedListener l) {
      surfaceUpdatedHelper.addSurfaceUpdatedListener(l);
  }

  public void addSurfaceUpdatedListener(int index, SurfaceUpdatedListener l) throws IndexOutOfBoundsException {
      surfaceUpdatedHelper.addSurfaceUpdatedListener(index, l);
  }

  public void removeSurfaceUpdatedListener(SurfaceUpdatedListener l) {
      surfaceUpdatedHelper.removeSurfaceUpdatedListener(l);
  }

  public void surfaceUpdated(Object updater, NativeSurface ns, long when) {
      surfaceUpdatedHelper.surfaceUpdated(updater, ns, when);
  }    
  
  //
  // NativeSurface
  //

  public NativeSurface getNativeSurface() {
      return this;
  }
  
  private RecursiveLock surfaceLock = LockFactory.createRecursiveLock();

  protected abstract int lockSurfaceImpl() throws NativeWindowException;

  public final int lockSurface() throws NativeWindowException {
    surfaceLock.lock();
    int res = surfaceLock.getHoldCount() == 1 ? LOCK_SURFACE_NOT_READY : LOCK_SUCCESS; // new lock ?

    if ( LOCK_SURFACE_NOT_READY == res ) {
        try {
            final AbstractGraphicsDevice adevice = config.getScreen().getDevice();
            adevice.lock();
            try {
                res = lockSurfaceImpl();
            } finally {
                if (LOCK_SURFACE_NOT_READY >= res) {
                    adevice.unlock();
                }
            }
        } finally {
            if (LOCK_SURFACE_NOT_READY >= res) {
                surfaceLock.unlock();
            }
        }
    }
    return res;
  }

  protected abstract void unlockSurfaceImpl() throws NativeWindowException;

  public final void unlockSurface() {
    surfaceLock.validateLocked();

    if (surfaceLock.getHoldCount() == 1) {
        final AbstractGraphicsDevice adevice = config.getScreen().getDevice();
        try {
            unlockSurfaceImpl();
        } finally {
            adevice.unlock();
        }
    }
    surfaceLock.unlock();
  }

  public final boolean isSurfaceLockedByOtherThread() {
    return surfaceLock.isLockedByOtherThread();
  }

  public final boolean isSurfaceLocked() {
    return surfaceLock.isLocked();
  }

  public final Thread getSurfaceLockOwner() {
    return surfaceLock.getOwner();
  }

  public boolean surfaceSwap() {
    return false;
  }

  public long getSurfaceHandle() {
    return drawable;
  }
  
  public final AbstractGraphicsConfiguration getGraphicsConfiguration() {
    return config;
  }

  public final long getDisplayHandle() {
    return config.getScreen().getDevice().getHandle();
  }

  public final int getScreenIndex() {
    return config.getScreen().getIndex();
  }

  public final void setSize(int width, int height) {
    component.setSize(width, height);
  }

  public final int getWidth() {
    return component.getWidth();
  }

  public final int getHeight() {
    return component.getHeight();
  }

  //
  // NativeWindow
  //

  public NativeWindow getNativeWindow() {
      return this;
  }
  
  public synchronized void destroy() {
    invalidate();
    if(null!=component) {
        if(component instanceof Window) {
            ((Window)component).dispose();
        }
        component = null;
    }
  }

  public final NativeWindow getParent() {
      return null;
  }

  public long getWindowHandle() {
    return drawable;
  }
  
  public final int getX() {
      return component.getX();
  }

  public final int getY() {
      return component.getY();
  }

  public Point getLocationOnScreen(Point storage) {
      int lockRes = lockSurface();
      if(LOCK_SURFACE_NOT_READY == lockRes) {
          // FIXME: Shall we deal with already locked or unrealized surfaces ?
          System.err.println("Warning: JAWT Lock couldn't be acquired!");
          Thread.dumpStack();
          return null;
      }
      try {
          Point d = getLocationOnScreenImpl(0, 0);
          if(null!=d) {
            if(null!=storage) {
                storage.translate(d.getX(),d.getY());
                return storage;
            }
            return d;
          }
          // fall through intended ..
          if(!Thread.holdsLock(component.getTreeLock())) {
              // FIXME: Verify if this check is still required!
              System.err.println("Warning: JAWT Lock hold, but not the AWT tree lock!");
              Thread.dumpStack();
              return null; // avoid deadlock ..
          }
          java.awt.Point awtLOS = component.getLocationOnScreen();
          int dx = (int) ( awtLOS.getX() + .5 ) ;
          int dy = (int) ( awtLOS.getY() + .5 ) ;
          if(null!=storage) {
              return storage.translate(dx, dy);
          }
          return new Point(dx, dy);
      } finally {
          unlockSurface();
      }
  }
  protected abstract Point getLocationOnScreenImpl(int x, int y);

    @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("JAWT-Window["+
                "windowHandle 0x"+Long.toHexString(getWindowHandle())+
                ", surfaceHandle 0x"+Long.toHexString(getSurfaceHandle())+
                ", bounds "+bounds);
    if(null!=component) {
      sb.append(", pos "+getX()+"/"+getY()+", size "+getWidth()+"x"+getHeight()+
                ", visible "+component.isVisible());
    } else {
      sb.append(", component NULL");
    }
    sb.append(", lockedExt "+isSurfaceLockedByOtherThread()+
              ",\n\tconfig "+config+
              ",\n\tawtComponent "+getAWTComponent()+"]");

    return sb.toString();
  }

}

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
import java.awt.Container;
import java.applet.Applet;
import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.OffscreenLayerOption;
import javax.media.nativewindow.OffscreenLayerSurface;
import javax.media.nativewindow.SurfaceUpdatedListener;
import javax.media.nativewindow.awt.AWTGraphicsConfiguration;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.nativewindow.util.Point;
import javax.media.nativewindow.util.Rectangle;
import javax.media.nativewindow.util.RectangleImmutable;

import jogamp.nativewindow.SurfaceUpdatedHelper;

public abstract class JAWTWindow implements NativeWindow, OffscreenLayerSurface, OffscreenLayerOption {
  protected static final boolean DEBUG = JAWTUtil.DEBUG;

  // user properties
  protected boolean shallUseOffscreenLayer = false;
  
  // lifetime: forever
  protected Component component;
  private AWTGraphicsConfiguration config; // control access due to delegation
  private SurfaceUpdatedHelper surfaceUpdatedHelper = new SurfaceUpdatedHelper();

  // lifetime: valid after lock but may change with each 1st lock, purges after invalidate
  private boolean isApplet;
  private JAWT jawt;
  private boolean isOffscreenLayerSurface;
  protected long drawable;
  protected Rectangle bounds;
  protected Insets insets;
  
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
        throw new NativeWindowException("Error: AbstractGraphicsConfiguration is not an AWTGraphicsConfiguration: "+config);
    }
    this.config = (AWTGraphicsConfiguration) config;
    init((Component)comp);
  }

  private void init(Component windowObject) throws NativeWindowException {
    invalidate();
    this.component = windowObject;
    this.isApplet = false;
  }
  
  public void setShallUseOffscreenLayer(boolean v) {
      shallUseOffscreenLayer = v;
  }
  
  public final boolean getShallUseOffscreenLayer() {
      return shallUseOffscreenLayer;
  }
  
  public final boolean isOffscreenLayerSurfaceEnabled() { 
      return isOffscreenLayerSurface;
  }
  
  protected synchronized void invalidate() {
    invalidateNative();
    jawt = null;
    isOffscreenLayerSurface = false;
    drawable= 0;
    bounds = new Rectangle();
    insets = new Insets();
  }
  protected abstract void invalidateNative();

  protected final void updateBounds(JAWT_Rectangle jawtBounds) {
    bounds.setX(jawtBounds.getX());
    bounds.setY(jawtBounds.getY());
    bounds.setWidth(jawtBounds.getWidth());
    bounds.setHeight(jawtBounds.getHeight());
    
    if(component instanceof Container) {
        java.awt.Insets contInsets = ((Container)component).getInsets();
        insets.setLeftWidth(contInsets.left);
        insets.setRightWidth(contInsets.right);
        insets.setTopHeight(contInsets.top);
        insets.setBottomHeight(contInsets.bottom);
    }
  }

  /** @return the JAWT_DrawingSurfaceInfo's (JAWT_Rectangle) bounds, updated with lock */
  public final RectangleImmutable getBounds() { return bounds; }
  
  public final InsetsImmutable getInsets() { return insets; }

  public final Component getAWTComponent() {
    return component;
  }
  
  /** 
   * Returns true if the AWT component is parented to an {@link java.applet.Applet}, 
   * otherwise false. This information is valid only after {@link #lockSurface()}. 
   */
  public final boolean isApplet() {
      return isApplet;
  }

  /** Returns the underlying JAWT instance created @ {@link #lockSurface()}. */
  public final JAWT getJAWT() {
      return jawt;
  }

  /** 
   * {@inheritDoc}
   */
  public final void attachSurfaceLayer(final long layerHandle) throws NativeWindowException {  
      if( !isOffscreenLayerSurfaceEnabled() ) {
          throw new NativeWindowException("Not an offscreen layer surface");
      }
      int lockRes = lockSurface();
      if (NativeSurface.LOCK_SURFACE_NOT_READY >= lockRes) {
          throw new NativeWindowException("Could not lock (offscreen layer): "+this);
      }
      try {
          if(DEBUG) {
            System.err.println("JAWTWindow.attachSurfaceHandle(): 0x"+Long.toHexString(layerHandle));
          }
          attachSurfaceLayerImpl(layerHandle);
      } finally {
          unlockSurface();
      }
  }  
  protected abstract void attachSurfaceLayerImpl(final long layerHandle);
  
  /** 
   * {@inheritDoc}
   */
  public final void detachSurfaceLayer(final long layerHandle) throws NativeWindowException {  
      if( !isOffscreenLayerSurfaceEnabled() ) {
          throw new java.lang.UnsupportedOperationException("Not an offscreen layer surface");
      }
      int lockRes = lockSurface();
      if (NativeSurface.LOCK_SURFACE_NOT_READY >= lockRes) {
          throw new NativeWindowException("Could not lock (offscreen layer): "+this);
      }
      try {
          if(DEBUG) {
            System.err.println("JAWTWindow.detachSurfaceHandle(): 0x"+Long.toHexString(layerHandle));
          }
          detachSurfaceLayerImpl(layerHandle);
      } finally {
          unlockSurface();
      }
  }  
  protected abstract void detachSurfaceLayerImpl(final long layerHandle);
  
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

  private RecursiveLock surfaceLock = LockFactory.createRecursiveLock();

  private void determineIfApplet() {
    Component c = component;
    while(!isApplet && null != c) {
        isApplet = c instanceof Applet;
        c = c.getParent();
    }
  }
  
  /**
   * If JAWT offscreen layer is supported, 
   * implementation shall respect {@link #getShallUseOffscreenLayer()}
   * and may respect {@link #isApplet()}.
   * 
   * @return The JAWT instance reflecting offscreen layer support, etc.
   * 
   * @throws NativeWindowException
   */
  protected abstract JAWT fetchJAWTImpl() throws NativeWindowException;
  protected abstract int lockSurfaceImpl() throws NativeWindowException;

  public final int lockSurface() throws NativeWindowException {
    surfaceLock.lock();
    int res = surfaceLock.getHoldCount() == 1 ? LOCK_SURFACE_NOT_READY : LOCK_SUCCESS; // new lock ?

    if ( LOCK_SURFACE_NOT_READY == res ) {
        determineIfApplet();
        try {
            final AbstractGraphicsDevice adevice = getGraphicsConfiguration().getScreen().getDevice();
            adevice.lock();
            try {
                jawt = fetchJAWTImpl();
                isOffscreenLayerSurface = JAWTUtil.isJAWTUsingOffscreenLayer(jawt);
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
        final AbstractGraphicsDevice adevice = getGraphicsConfiguration().getScreen().getDevice();
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
  
  public final AWTGraphicsConfiguration getPrivateGraphicsConfiguration() {
    return config;
  }
  
  public final AbstractGraphicsConfiguration getGraphicsConfiguration() {
    return config.getNativeGraphicsConfiguration();
  }

  public final long getDisplayHandle() {
    return getGraphicsConfiguration().getScreen().getDevice().getHandle();
  }

  public final int getScreenIndex() {
    return getGraphicsConfiguration().getScreen().getIndex();
  }

  public int getWidth() {
    return component.getWidth();
  }

  public int getHeight() {
    return component.getHeight();
  }

  //
  // NativeWindow
  //

  public synchronized void destroy() {
    invalidate();    
    component = null; // don't dispose the AWT component, since we are merely an immutable uplink 
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
  
  /**
   * {@inheritDoc}
   * 
   * <p>
   * This JAWT default implementation is currently still using 
   * a blocking implementation. It first attempts to retrieve the location
   * via a native implementation. If this fails, it tries the blocking AWT implementation.
   * If the latter fails due to an external AWT tree-lock, the non block 
   * implementation {@link #getLocationOnScreenNonBlocking(Point, Component)} is being used.
   * The latter simply traverse up to the AWT component tree and sums the rel. position.
   * We have to determine whether the latter is good enough for all cases,
   * currently only OS X utilizes the non blocking method per default.
   * </p>   
   */
  public Point getLocationOnScreen(Point storage) {
      Point los = getLocationOnScreenNative(storage);
      if(null == los) {
          if(!Thread.holdsLock(component.getTreeLock())) {
              // avoid deadlock ..
              if(DEBUG) {
                  System.err.println("Warning: JAWT Lock hold, but not the AWT tree lock: "+this);
                  Thread.dumpStack();
              }
              return getLocationOnScreenNonBlocking(storage, component);
          }
          java.awt.Point awtLOS = component.getLocationOnScreen();
          if(null!=storage) {
              los = storage.translate(awtLOS.x, awtLOS.y);
          } else {
              los = new Point(awtLOS.x, awtLOS.y);
          }
      }
      return los;
  }
  
  protected Point getLocationOnScreenNative(Point storage) {
      int lockRes = lockSurface();
      if(LOCK_SURFACE_NOT_READY == lockRes) {
          if(DEBUG) {
              System.err.println("Warning: JAWT Lock couldn't be acquired: "+this);
              Thread.dumpStack();
          }
          return null;
      }
      try {
          Point d = getLocationOnScreenNativeImpl(0, 0);
          if(null!=d) {
            if(null!=storage) {
                storage.translate(d.getX(),d.getY());
                return storage;
            }
          }
          return d;
      } finally {
          unlockSurface();
      }      
  }
  protected abstract Point getLocationOnScreenNativeImpl(int x, int y);

  protected static Point getLocationOnScreenNonBlocking(Point storage, Component comp) {     
      int x = 0; 
      int y = 0;
      while(null != comp) {
          x += comp.getX();
          y += comp.getY();
          comp = comp.getParent();
      }
      if(null!=storage) {
          storage.translate(x, y);
          return storage;
      }
      return new Point(x, y);
  }
  
  public boolean hasFocus() {
      return component.hasFocus();
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("JAWT-Window["+
                "windowHandle 0x"+Long.toHexString(getWindowHandle())+
                ", surfaceHandle 0x"+Long.toHexString(getSurfaceHandle())+
                ", bounds "+bounds+", insets "+insets+
                ", shallUseOffscreenLayer "+shallUseOffscreenLayer+", isOffscreenLayerSurface "+isOffscreenLayerSurface);
    if(null!=component) {
      sb.append(", pos "+getX()+"/"+getY()+", size "+getWidth()+"x"+getHeight()+
                ", visible "+component.isVisible());
    } else {
      sb.append(", component NULL");
    }
    sb.append(", lockedExt "+isSurfaceLockedByOtherThread()+
              ",\n\tconfig "+getPrivateGraphicsConfiguration()+
              ",\n\tawtComponent "+getAWTComponent()+"]");

    return sb.toString();
  }

}

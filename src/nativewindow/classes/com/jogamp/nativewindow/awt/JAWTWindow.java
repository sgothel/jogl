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

package com.jogamp.nativewindow.awt;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.awt.AWTEDTExecutor;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.nativewindow.MutableGraphicsConfiguration;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.applet.Applet;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.OffscreenLayerOption;
import javax.media.nativewindow.OffscreenLayerSurface;
import javax.media.nativewindow.SurfaceUpdatedListener;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.nativewindow.util.PixelRectangle;
import javax.media.nativewindow.util.Point;
import javax.media.nativewindow.util.PointImmutable;
import javax.media.nativewindow.util.Rectangle;
import javax.media.nativewindow.util.RectangleImmutable;

import jogamp.nativewindow.SurfaceUpdatedHelper;
import jogamp.nativewindow.awt.AWTMisc;
import jogamp.nativewindow.jawt.JAWT;
import jogamp.nativewindow.jawt.JAWTUtil;
import jogamp.nativewindow.jawt.JAWT_Rectangle;

public abstract class JAWTWindow implements NativeWindow, OffscreenLayerSurface, OffscreenLayerOption {
  protected static final boolean DEBUG = JAWTUtil.DEBUG;

  // user properties
  protected boolean shallUseOffscreenLayer = false;

  // lifetime: forever
  protected final Component component;
  private final AWTGraphicsConfiguration config; // control access due to delegation
  private final SurfaceUpdatedHelper surfaceUpdatedHelper = new SurfaceUpdatedHelper();
  private final RecursiveLock surfaceLock = LockFactory.createRecursiveLock();
  private final JAWTComponentListener jawtComponentListener;

  // lifetime: valid after lock but may change with each 1st lock, purges after invalidate
  private boolean isApplet;
  private JAWT jawt;
  private boolean isOffscreenLayerSurface;
  protected long drawable;
  protected Rectangle bounds;
  protected Insets insets;
  private volatile long offscreenSurfaceLayer;

  private long drawable_old;

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
    this.component = (Component)comp;
    this.config = (AWTGraphicsConfiguration) config;
    this.jawtComponentListener = new JAWTComponentListener();
    invalidate();
    this.isApplet = false;
    this.offscreenSurfaceLayer = 0;
  }
  private static String id(Object obj) { return "0x" + ( null!=obj ? Integer.toHexString(obj.hashCode()) : "nil" ); }
  private String jawtStr() { return "JAWTWindow["+id(JAWTWindow.this)+"]"; }

  private class JAWTComponentListener implements ComponentListener, HierarchyListener {
        private boolean isShowing;

        private String str(final Object obj) {
            if( null == obj ) {
                return "0xnil: null";
            } else if( obj instanceof Component ) {
                final Component c = (Component)obj;
                return id(obj)+": "+c.getClass().getSimpleName()+"[visible "+c.isVisible()+", showing "+c.isShowing()+", valid "+c.isValid()+
                        ", displayable "+c.isDisplayable()+", "+c.getX()+"/"+c.getY()+" "+c.getWidth()+"x"+c.getHeight()+"]";
            } else {
                return id(obj)+": "+obj.getClass().getSimpleName()+"[..]";
            }
        }
        private String s(final ComponentEvent e) {
            return "visible[isShowing "+isShowing+"],"+Platform.getNewline()+
                   "    ** COMP "+str(e.getComponent())+Platform.getNewline()+
                   "    ** SOURCE "+str(e.getSource())+Platform.getNewline()+
                   "    ** THIS "+str(component)+Platform.getNewline()+
                   "    ** THREAD "+getThreadName();
        }
        private String s(final HierarchyEvent e) {
            return "visible[isShowing "+isShowing+"], changeBits 0x"+Long.toHexString(e.getChangeFlags())+Platform.getNewline()+
                   "    ** COMP "+str(e.getComponent())+Platform.getNewline()+
                   "    ** SOURCE "+str(e.getSource())+Platform.getNewline()+
                   "    ** CHANGED "+str(e.getChanged())+Platform.getNewline()+
                   "    ** CHANGEDPARENT "+str(e.getChangedParent())+Platform.getNewline()+
                   "    ** THIS "+str(component)+Platform.getNewline()+
                   "    ** THREAD "+getThreadName();
        }
        @Override
        public final String toString() {
            return "visible[isShowing "+isShowing+"],"+Platform.getNewline()+
                   "    ** THIS "+str(component)+Platform.getNewline()+
                   "    ** THREAD "+getThreadName();
        }

        private JAWTComponentListener() {
            isShowing = component.isShowing();
            if(DEBUG) {
                System.err.println(jawtStr()+".attach @ Thread "+getThreadName()+": "+toString());
            }
            component.addComponentListener(this);
            component.addHierarchyListener(this);
        }

        private final void detach() {
            if(DEBUG) {
                System.err.println(jawtStr()+".detach @ Thread "+getThreadName()+": "+toString());
            }
            component.removeComponentListener(this);
            component.removeHierarchyListener(this);
        }

        @Override
        public final void componentResized(ComponentEvent e) {
            if(DEBUG) {
                System.err.println(jawtStr()+".componentResized: "+s(e));
            }
            layoutSurfaceLayerIfEnabled(isShowing);
        }

        @Override
        public final void componentMoved(ComponentEvent e) {
            if(DEBUG) {
                System.err.println(jawtStr()+".componentMoved: "+s(e));
            }
            layoutSurfaceLayerIfEnabled(isShowing);
        }

        @Override
        public final void componentShown(ComponentEvent e) {
            if(DEBUG) {
                System.err.println(jawtStr()+".componentShown: "+s(e));
            }
            layoutSurfaceLayerIfEnabled(isShowing);
        }

        @Override
        public final void componentHidden(ComponentEvent e) {
            if(DEBUG) {
                System.err.println(jawtStr()+".componentHidden: "+s(e));
            }
            layoutSurfaceLayerIfEnabled(isShowing);
        }

        @Override
        public final void hierarchyChanged(HierarchyEvent e) {
            final boolean wasShowing = isShowing;
            isShowing = component.isShowing();
            int action = 0;
            if( 0 != ( java.awt.event.HierarchyEvent.SHOWING_CHANGED & e.getChangeFlags() ) ) {
                if( e.getChanged() != component && wasShowing != isShowing ) {
                    // A parent component changed and caused a 'showing' state change,
                    // propagate to offscreen-layer!
                    layoutSurfaceLayerIfEnabled(isShowing);
                    action = 1;
                }
            }
            if(DEBUG) {
                final java.awt.Component changed = e.getChanged();
                final boolean displayable = changed.isDisplayable();
                final boolean showing = changed.isShowing();
                System.err.println(jawtStr()+".hierarchyChanged: action "+action+", displayable "+displayable+", showing [changed "+showing+", comp "+isShowing+"], "+s(e));
            }
        }
  }

  private static String getThreadName() { return Thread.currentThread().getName(); }

  protected synchronized void invalidate() {
    if(DEBUG) {
        System.err.println(jawtStr()+".invalidate() - "+jawtComponentListener.toString());
        if( isSurfaceLayerAttached() ) {
            System.err.println("OffscreenSurfaceLayer still attached: 0x"+Long.toHexString(offscreenSurfaceLayer));
        }
        // Thread.dumpStack();
    }
    invalidateNative();
    jawt = null;
    isOffscreenLayerSurface = false;
    drawable= 0;
    drawable_old = 0;
    bounds = new Rectangle();
    insets = new Insets();
  }
  protected abstract void invalidateNative();

  protected final boolean updateBounds(JAWT_Rectangle jawtBounds) {
    final Rectangle jb = new Rectangle(jawtBounds.getX(), jawtBounds.getY(), jawtBounds.getWidth(), jawtBounds.getHeight());
    final boolean changed = !bounds.equals(jb);

    if(changed) {
        if(DEBUG) {
            System.err.println("JAWTWindow.updateBounds: "+bounds+" -> "+jb);
            // Thread.dumpStack();
        }
        bounds.set(jawtBounds.getX(), jawtBounds.getY(), jawtBounds.getWidth(), jawtBounds.getHeight());

        if(component instanceof Container) {
            final java.awt.Insets contInsets = ((Container)component).getInsets();
            insets.set(contInsets.left, contInsets.right, contInsets.top, contInsets.bottom);
        }
    }
    return changed;
  }

  /** @return the JAWT_DrawingSurfaceInfo's (JAWT_Rectangle) bounds, updated with lock */
  public final RectangleImmutable getBounds() { return bounds; }

  @Override
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

  //
  // OffscreenLayerOption
  //

  @Override
  public void setShallUseOffscreenLayer(boolean v) {
      shallUseOffscreenLayer = v;
  }

  @Override
  public final boolean getShallUseOffscreenLayer() {
      return shallUseOffscreenLayer;
  }

  @Override
  public final boolean isOffscreenLayerSurfaceEnabled() {
      return isOffscreenLayerSurface;
  }

  //
  // OffscreenLayerSurface
  //

  @Override
  public final void attachSurfaceLayer(final long layerHandle) throws NativeWindowException {
      if( !isOffscreenLayerSurfaceEnabled() ) {
          throw new NativeWindowException("Not an offscreen layer surface");
      }
      attachSurfaceLayerImpl(layerHandle);
      offscreenSurfaceLayer = layerHandle;
      component.repaint();
  }
  protected void attachSurfaceLayerImpl(final long layerHandle) {
      throw new UnsupportedOperationException("offscreen layer not supported");
  }

  /**
   * Layout the offscreen layer according to the implementing class's constraints.
   * <p>
   * This method allows triggering a re-layout of the offscreen surface
   * in case the implementation requires it.
   * </p>
   * <p>
   * Call this method if any parent or ancestor's layout has been changed,
   * which could affects the layout of this surface.
   * </p>
   * @see #isOffscreenLayerSurfaceEnabled()
   * @throws NativeWindowException if {@link #isOffscreenLayerSurfaceEnabled()} == false
   */
  protected void layoutSurfaceLayerImpl(long layerHandle, boolean visible) {}

  private final void layoutSurfaceLayerIfEnabled(boolean visible) throws NativeWindowException {
      if( isOffscreenLayerSurfaceEnabled() && 0 != offscreenSurfaceLayer ) {
          layoutSurfaceLayerImpl(offscreenSurfaceLayer, visible);
      }
  }


  @Override
  public final void detachSurfaceLayer() throws NativeWindowException {
      if( 0 == offscreenSurfaceLayer) {
          throw new NativeWindowException("No offscreen layer attached: "+this);
      }
      if(DEBUG) {
        System.err.println("JAWTWindow.detachSurfaceHandle(): osh "+toHexString(offscreenSurfaceLayer));
      }
      detachSurfaceLayerImpl(offscreenSurfaceLayer, detachSurfaceLayerNotify);
  }
  private final Runnable detachSurfaceLayerNotify = new Runnable() {
    @Override
    public void run() {
        offscreenSurfaceLayer = 0;
    }
  };

  /**
   * @param detachNotify Runnable to be called before native detachment
   */
  protected void detachSurfaceLayerImpl(final long layerHandle, final Runnable detachNotify) {
      throw new UnsupportedOperationException("offscreen layer not supported");
  }


  @Override
  public final long getAttachedSurfaceLayer() {
      return offscreenSurfaceLayer;
  }

  @Override
  public final boolean isSurfaceLayerAttached() {
      return 0 != offscreenSurfaceLayer;
  }

  @Override
  public final void setChosenCapabilities(CapabilitiesImmutable caps) {
      ((MutableGraphicsConfiguration)getGraphicsConfiguration()).setChosenCapabilities(caps);
      config.setChosenCapabilities(caps);
  }

  @Override
  public final RecursiveLock getLock() {
      return surfaceLock;
  }

  @Override
  public final boolean setCursor(final PixelRectangle pixelrect, final PointImmutable hotSpot) {
      AWTEDTExecutor.singleton.invoke(false, new Runnable() {
          public void run() {
              Cursor c = null;
              if( null == pixelrect || null == hotSpot ) {
                  c = Cursor.getDefaultCursor();
              } else {
                  final java.awt.Point awtHotspot = new java.awt.Point(hotSpot.getX(), hotSpot.getY());
                  try {
                      c = AWTMisc.getCursor(pixelrect, awtHotspot);
                  } catch (Exception e) {
                      e.printStackTrace();
                  }
              }
              if( null != c ) {
                  component.setCursor(c);
              }
          } } );
      return true;
  }

  @Override
  public final boolean hideCursor() {
      AWTEDTExecutor.singleton.invoke(false, new Runnable() {
          public void run() {
              component.setCursor(AWTMisc.getNullCursor());
          } } );
      return true;
  }

  //
  // SurfaceUpdateListener
  //

  @Override
  public void addSurfaceUpdatedListener(SurfaceUpdatedListener l) {
      surfaceUpdatedHelper.addSurfaceUpdatedListener(l);
  }

  @Override
  public void addSurfaceUpdatedListener(int index, SurfaceUpdatedListener l) throws IndexOutOfBoundsException {
      surfaceUpdatedHelper.addSurfaceUpdatedListener(index, l);
  }

  @Override
  public void removeSurfaceUpdatedListener(SurfaceUpdatedListener l) {
      surfaceUpdatedHelper.removeSurfaceUpdatedListener(l);
  }

  @Override
  public void surfaceUpdated(Object updater, NativeSurface ns, long when) {
      surfaceUpdatedHelper.surfaceUpdated(updater, ns, when);
  }

  //
  // NativeSurface
  //

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

  protected void dumpJAWTInfo() {
      System.err.println(jawt2String(null).toString());
      // Thread.dumpStack();
  }

  @Override
  public final int lockSurface() throws NativeWindowException, RuntimeException  {
    surfaceLock.lock();
    int res = surfaceLock.getHoldCount() == 1 ? LOCK_SURFACE_NOT_READY : LOCK_SUCCESS; // new lock ?

    if ( LOCK_SURFACE_NOT_READY == res ) {
        if( !component.isDisplayable() ) {
            // W/o native peer, we cannot utilize JAWT for locking.
            surfaceLock.unlock();
            if(DEBUG) {
                System.err.println("JAWTWindow: Can't lock surface, component peer n/a. Component displayable "+component.isDisplayable()+", "+component);
                Thread.dumpStack();
            }
        } else {
            determineIfApplet();
            try {
                final AbstractGraphicsDevice adevice = getGraphicsConfiguration().getScreen().getDevice();
                adevice.lock();
                try {
                    if(null == jawt) { // no need to re-fetch for each frame
                        jawt = fetchJAWTImpl();
                        isOffscreenLayerSurface = JAWTUtil.isJAWTUsingOffscreenLayer(jawt);
                    }
                    res = lockSurfaceImpl();
                    if(LOCK_SUCCESS == res && drawable_old != drawable) {
                        res = LOCK_SURFACE_CHANGED;
                        if(DEBUG) {
                            System.err.println("JAWTWindow: surface change "+toHexString(drawable_old)+" -> "+toHexString(drawable));
                            // Thread.dumpStack();
                        }
                    }
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
    }
    return res;
  }

  protected abstract void unlockSurfaceImpl() throws NativeWindowException;

  @Override
  public final void unlockSurface() {
    surfaceLock.validateLocked();
    drawable_old = drawable;

    if (surfaceLock.getHoldCount() == 1) {
        final AbstractGraphicsDevice adevice = getGraphicsConfiguration().getScreen().getDevice();
        try {
            if(null != jawt) {
                unlockSurfaceImpl();
            }
        } finally {
            adevice.unlock();
        }
    }
    surfaceLock.unlock();
  }

  @Override
  public final boolean isSurfaceLockedByOtherThread() {
    return surfaceLock.isLockedByOtherThread();
  }

  @Override
  public final Thread getSurfaceLockOwner() {
    return surfaceLock.getOwner();
  }

  @Override
  public boolean surfaceSwap() {
    return false;
  }

  @Override
  public long getSurfaceHandle() {
    return drawable;
  }

  @Override
  public final AbstractGraphicsConfiguration getGraphicsConfiguration() {
    return config.getNativeGraphicsConfiguration();
  }

  @Override
  public final long getDisplayHandle() {
    return getGraphicsConfiguration().getScreen().getDevice().getHandle();
  }

  @Override
  public final int getScreenIndex() {
    return getGraphicsConfiguration().getScreen().getIndex();
  }

  @Override
  public final int getWidth() {
    return component.getWidth();
  }

  @Override
  public final int getHeight() {
    return component.getHeight();
  }

  //
  // NativeWindow
  //

  @Override
  public void destroy() {
    surfaceLock.lock();
    try {
        jawtComponentListener.detach();
        invalidate();
    } finally {
        surfaceLock.unlock();
    }
  }

  @Override
  public final NativeWindow getParent() {
      return null;
  }

  @Override
  public long getWindowHandle() {
    return drawable;
  }

  @Override
  public final int getX() {
      return component.getX();
  }

  @Override
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
  @Override
  public Point getLocationOnScreen(Point storage) {
      Point los = getLocationOnScreenNative(storage);
      if(null == los) {
          if(!Thread.holdsLock(component.getTreeLock())) {
              // avoid deadlock ..
              if(DEBUG) {
                  System.err.println("Warning: JAWT Lock hold, but not the AWT tree lock: "+this);
                  Thread.dumpStack();
              }
              if( null == storage ) {
                  storage = new Point();
              }
              getLocationOnScreenNonBlocking(storage, component);
              return storage;
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

  protected static Component getLocationOnScreenNonBlocking(Point storage, Component comp) {
      final java.awt.Insets insets = new java.awt.Insets(0, 0, 0, 0); // DEBUG
      Component last = null;
      while(null != comp) {
          final int dx = comp.getX();
          final int dy = comp.getY();
          if( DEBUG ) {
              final java.awt.Insets ins = AWTMisc.getInsets(comp, false);
              if( null != ins ) {
                  insets.bottom += ins.bottom;
                  insets.top += ins.top;
                  insets.left += ins.left;
                  insets.right += ins.right;
              }
              System.err.print("LOS: "+storage+" + "+comp.getClass().getName()+"["+dx+"/"+dy+", vis "+comp.isVisible()+", ins "+ins+" -> "+insets+"] -> ");
          }
          storage.translate(dx, dy);
          if( DEBUG ) {
              System.err.println(storage);
          }
          last = comp;
          if( comp instanceof Window ) { // top-level heavy-weight ?
              break;
          }
          comp = comp.getParent();
      }
      return last;
  }

  @Override
  public boolean hasFocus() {
      return component.hasFocus();
  }

  protected StringBuilder jawt2String(StringBuilder sb) {
      if( null == sb ) {
          sb = new StringBuilder();
      }
      sb.append("JVM version: ").append(Platform.JAVA_VERSION).append(" (").
      append(Platform.JAVA_VERSION_NUMBER).
      append(" update ").append(Platform.JAVA_VERSION_UPDATE).append(")").append(Platform.getNewline());
      if(null != jawt) {
          sb.append("JAWT version: 0x").append(Integer.toHexString(jawt.getCachedVersion())).
          append(", CA_LAYER: ").append(JAWTUtil.isJAWTUsingOffscreenLayer(jawt)).
          append(", isLayeredSurface ").append(isOffscreenLayerSurfaceEnabled()).append(", bounds ").append(bounds).append(", insets ").append(insets);
      } else {
          sb.append("JAWT n/a, bounds ").append(bounds).append(", insets ").append(insets);
      }
      return sb;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(jawtStr()+"[");
    jawt2String(sb);
    sb.append(  ", shallUseOffscreenLayer "+shallUseOffscreenLayer+", isOffscreenLayerSurface "+isOffscreenLayerSurface+
                ", attachedSurfaceLayer "+toHexString(getAttachedSurfaceLayer())+
                ", windowHandle "+toHexString(getWindowHandle())+
                ", surfaceHandle "+toHexString(getSurfaceHandle())+
                ", bounds "+bounds+", insets "+insets
                );
    sb.append(", pos "+getX()+"/"+getY()+", size "+getWidth()+"x"+getHeight()+
              ", visible "+component.isVisible());
    sb.append(", lockedExt "+isSurfaceLockedByOtherThread()+
              ",\n\tconfig "+config+
              ",\n\tawtComponent "+getAWTComponent()+
              ",\n\tsurfaceLock "+surfaceLock+"]");

    return sb.toString();
  }

  protected final String toHexString(long l) {
      return "0x"+Long.toHexString(l);
  }
}

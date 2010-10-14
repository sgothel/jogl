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

package com.jogamp.nativewindow.impl.jawt;

import com.jogamp.nativewindow.impl.*;
import com.jogamp.common.util.locks.RecursiveLock;

import java.awt.Component;
import java.awt.Window;
import java.awt.GraphicsEnvironment;
import javax.media.nativewindow.*;
import javax.media.nativewindow.util.Point;
import javax.media.nativewindow.util.Rectangle;

public abstract class JAWTWindow implements NativeWindow {
  protected static final boolean DEBUG = JAWTUtil.DEBUG;

  // lifetime: forever
  protected Component component;
  protected AbstractGraphicsConfiguration config;

  // lifetime: valid after lock, forever until invalidate
  protected long drawable;
  protected Rectangle bounds;

  public JAWTWindow(Object comp, AbstractGraphicsConfiguration config) {
    if (config == null) {
        throw new NativeWindowException("Error: AbstractGraphicsConfiguration is null");
    }
    this.config = config;
    init((Component)comp);
  }

  protected void init(Component windowObject) throws NativeWindowException {
    invalidate();
    this.component = windowObject;
    initNative();
  }

  protected abstract void initNative() throws NativeWindowException;

  protected synchronized void invalidate() {
    component = null;
    drawable= 0;
    bounds = new Rectangle();
  }

  protected void updateBounds(JAWT_Rectangle jawtBounds) {
    bounds.setX(jawtBounds.getX());
    bounds.setY(jawtBounds.getY());
    bounds.setWidth(jawtBounds.getWidth());
    bounds.setHeight(jawtBounds.getHeight());
  }

  /** @return the JAWT_DrawingSurfaceInfo's (JAWT_Rectangle) bounds, updated with lock */
  public Rectangle getBounds() { return bounds; }

  public Component getAWTComponent() {
    return component;
  }

  //
  // SurfaceUpdateListener
  //

  public void surfaceUpdated(Object updater, NativeSurface ns, long when) {
      // nop
  }
  
  //
  // NativeSurface
  //

  private RecursiveLock recurLock = new RecursiveLock();

  protected abstract int lockSurfaceImpl() throws NativeWindowException;

  public final int lockSurface() throws NativeWindowException {
    int res = LOCK_SURFACE_NOT_READY;

    recurLock.lock();

    if(recurLock.getRecursionCount() == 0) {
        config.getScreen().getDevice().lock();
        try {
            res = lockSurfaceImpl();
        } finally {
            // Unlock in case surface couldn't be locked
            if(LOCK_SURFACE_NOT_READY >= res ) {
                config.getScreen().getDevice().unlock();
                recurLock.unlock();
            }
        }
    } else {
        res = LOCK_SUCCESS;
    }

    return res;
  }

  protected abstract void unlockSurfaceImpl() throws NativeWindowException;

  public final void unlockSurface() {
    recurLock.validateLocked();

    if(recurLock.getRecursionCount()==0) {
        try {
            unlockSurfaceImpl();
        } finally {
            config.getScreen().getDevice().unlock();
        }
    }
    recurLock.unlock();
  }

  public final boolean isSurfaceLockedByOtherThread() {
    return recurLock.isLockedByOtherThread();
  }

  public final boolean isSurfaceLocked() {
    return recurLock.isLocked();
  }

  public final Thread getSurfaceLockOwner() {
    return recurLock.getOwner();
  }

  public boolean surfaceSwap() {
    return false;
  }

  public void surfaceUpdated(Object updater, NativeWindow window, long when) { }

  public long getSurfaceHandle() {
    return drawable;
  }
  public AbstractGraphicsConfiguration getGraphicsConfiguration() {
    return config;
  }

  public long getDisplayHandle() {
    return config.getScreen().getDevice().getHandle();
  }

  public int getScreenIndex() {
    return config.getScreen().getIndex();
  }

  public void setSize(int width, int height) {
    component.setSize(width, height);
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
    if(null!=component) {
        if(component instanceof Window) {
            ((Window)component).dispose();
        }
    }
    invalidate();
  }

  public NativeWindow getParent() {
      return null;
  }

  public long getWindowHandle() {
    return drawable;
  }

  public int getX() {
      return component.getX();
  }

  public int getY() {
      return component.getY();
  }

  public Point getLocationOnScreen(Point point) {
        java.awt.Point awtLOS = component.getLocationOnScreen();
        int dx = (int) ( awtLOS.getX() + .5 ) ;
        int dy = (int) ( awtLOS.getY() + .5 ) ;
        if(null!=point) {
            return point.translate(dx, dy);
        }
        return new Point(dx, dy);
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();

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

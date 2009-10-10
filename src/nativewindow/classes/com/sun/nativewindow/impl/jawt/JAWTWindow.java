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

package com.sun.nativewindow.impl.jawt;

import com.sun.nativewindow.impl.*;

import java.awt.Component;
import java.awt.Window;
import java.awt.GraphicsEnvironment;
import javax.media.nativewindow.*;
import com.sun.nativewindow.impl.*;

public abstract class JAWTWindow implements NativeWindow {
  protected static final boolean DEBUG = Debug.debug("JAWT");

  // See whether we're running in headless mode
  private static boolean headlessMode;

  static {
    headlessMode = GraphicsEnvironment.isHeadless();
  }

  // lifetime: forever
  protected Component component;
  protected AbstractGraphicsConfiguration config;

  // lifetime: valid after lock, forever until invalidate
  protected long drawable;

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

  public synchronized void invalidate() {
    component = null;
    drawable= 0;
  }

  public synchronized void destroy() {
    if(null!=component) {
        if(component instanceof Window) {
            ((Window)component).dispose();
        }
    }
    invalidate();
  }

  private volatile Exception lockedStack = null;

  public synchronized int lockSurface() throws NativeWindowException {
    // We have to be the owner of the JAWT ToolkitLock 'lock' to benefit from it's
    // recursive and blocking lock capabitlites. 
    // Otherwise a followup ToolkitLock would deadlock, 
    // since we already have locked JAWT with the surface lock.
    NativeWindowFactory.getDefaultFactory().getToolkitLock().lock();

    // recursion not necessary here, due to the blocking ToolkitLock ..
    if (null!=lockedStack) {
      lockedStack.printStackTrace();
      throw new NativeWindowException("JAWT Surface already locked - "+Thread.currentThread().getName()+" "+this);
    }
    lockedStack = new Exception("JAWT Surface previously locked by "+Thread.currentThread().getName());

    return LOCK_SUCCESS;
  }

  public synchronized void unlockSurface() {
    if (null!=lockedStack) {
        lockedStack = null;
    } else {
        throw new NativeWindowException("JAWT Surface not locked");
    }
    NativeWindowFactory.getDefaultFactory().getToolkitLock().unlock();
  }

  public synchronized boolean isSurfaceLocked() {
    return null!=lockedStack;
  }

  public Exception getLockedStack() {
    return lockedStack;
  }

  public boolean surfaceSwap() {
    return false;
  }

  public void surfaceUpdated(Object updater, NativeWindow window, long when) { }

  public long getDisplayHandle() {
    return config.getScreen().getDevice().getHandle();
  }
  public int getScreenIndex() {
    return config.getScreen().getIndex();
  }
  public long getWindowHandle() {
    return drawable;
  }
  public long getSurfaceHandle() {
    return drawable;
  }
  public AbstractGraphicsConfiguration getGraphicsConfiguration() {
    return config;
  }

  public Object getWrappedWindow() {
    return component;
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

  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("JAWT-Window["+
                "windowHandle 0x"+Long.toHexString(getWindowHandle())+
                ", surfaceHandle 0x"+Long.toHexString(getSurfaceHandle()));
    if(null!=component) {
      sb.append(", pos "+component.getX()+"/"+component.getY()+", size "+getWidth()+"x"+getHeight()+
                ", visible "+component.isVisible());
    } else {
      sb.append(", component NULL");
    }
    sb.append(", locked "+isSurfaceLocked()+
              ",\n\tconfig "+config+
              ",\n\twrappedWindow "+getWrappedWindow()+"]");

    return sb.toString();
  }
}

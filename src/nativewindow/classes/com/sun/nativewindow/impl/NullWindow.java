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

package com.sun.nativewindow.impl;

import javax.media.nativewindow.*;

public class NullWindow implements NativeWindow, SurfaceChangeable {
  private Exception lockedStack = null;
  protected int width, height, scrnIndex;
  protected long surfaceHandle, displayHandle;
  protected AbstractGraphicsConfiguration config;

  public NullWindow(AbstractGraphicsConfiguration cfg) {
    invalidate();
    config = cfg;
    displayHandle=cfg.getScreen().getDevice().getHandle();
    scrnIndex=cfg.getScreen().getIndex();
  }

  protected void init(Object windowObject) throws NativeWindowException {
  }

  protected void initNative() throws NativeWindowException {
  }

  public void destroy() {
    invalidate();
  }

  public synchronized void invalidate() {
    displayHandle=0;
    scrnIndex=-1;
    surfaceHandle=0;
  }

  public synchronized int lockSurface() throws NativeWindowException {
    if (null!=lockedStack) {
      lockedStack.printStackTrace();
      throw new NativeWindowException("Surface already locked - "+this);
    }
    lockedStack = new Exception("NullWindow previously locked by "+Thread.currentThread().getName());
    return LOCK_SUCCESS;
  }

  public synchronized void unlockSurface() {
    if (null!=lockedStack) {
        lockedStack = null;
    } else {
        throw new NativeWindowException("NullWindow not locked");
    }
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
    return displayHandle;
  }
  public int getScreenIndex() {
    return scrnIndex;
  }
  public long getWindowHandle() {
    return 0;
  }
  public long getSurfaceHandle() {
    return surfaceHandle;
  }
  public void setSurfaceHandle(long surfaceHandle) {
    this.surfaceHandle=surfaceHandle;
  }
  public AbstractGraphicsConfiguration getGraphicsConfiguration() {
    return config;
  }

  public Object getWrappedWindow() {
    return null;
  }

  public final boolean isTerminalObject() {
    return true;
  }

  public void setSize(int width, int height) {
    this.width=width;
    this.height=height;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public String toString() {
    return "NullWindow[config "+config+
                ", displayHandle 0x"+Long.toHexString(getDisplayHandle())+
                ", surfaceHandle 0x"+Long.toHexString(getSurfaceHandle())+
                ", size "+getWidth()+"x"+getHeight()+"]";
  }

}

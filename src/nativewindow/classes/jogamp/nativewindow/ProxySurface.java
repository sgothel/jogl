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

package com.jogamp.nativewindow.impl;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.SurfaceChangeable;

import com.jogamp.common.util.locks.RecursiveLock;

public class ProxySurface implements NativeSurface, SurfaceChangeable {
  private RecursiveLock recurLock = new RecursiveLock();
  protected AbstractGraphicsConfiguration config;
  protected long displayHandle;
  protected long surfaceHandle;
  protected int scrnIndex;
  protected int width, height;

  public ProxySurface(AbstractGraphicsConfiguration cfg) {
      this(cfg, 0);
  }

  public ProxySurface(AbstractGraphicsConfiguration cfg, long handle) {
    invalidate();
    config = cfg;
    displayHandle=cfg.getScreen().getDevice().getHandle();
    surfaceHandle=handle;
    scrnIndex=cfg.getScreen().getIndex();
  }

  protected void init(Object windowObject) throws NativeWindowException {
  }

  protected void initNative() throws NativeWindowException {
  }

  public NativeWindow getParent() {
    return null;
  }

  public void destroy() {
    invalidate();
  }

  public synchronized void invalidate() {
    displayHandle=0;
    scrnIndex=-1;
    surfaceHandle=0;
  }

  public final int lockSurface() throws NativeWindowException {
    recurLock.lock();

    if(recurLock.getRecursionCount() == 0) {
        config.getScreen().getDevice().lock();
    }
    return LOCK_SUCCESS;
  }

  public final void unlockSurface() {
    recurLock.validateLocked();

    if(recurLock.getRecursionCount()==0) {
        config.getScreen().getDevice().unlock();
    }
    recurLock.unlock();
  }

  public final void validateSurfaceLocked() {
      recurLock.validateLocked();
  }

  public final int getSurfaceRecursionCount() {
    return recurLock.getRecursionCount();
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

  public long getSurfaceHandle() {
    return surfaceHandle;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public AbstractGraphicsConfiguration getGraphicsConfiguration() {
    return config;
  }

  public void surfaceUpdated(Object updater, NativeSurface ns, long when) { }

  public long getDisplayHandle() {
    return displayHandle;
  }
  public int getScreenIndex() {
    return scrnIndex;
  }
  
  public void setSurfaceHandle(long surfaceHandle) {
    this.surfaceHandle=surfaceHandle;
  }

  public void setSize(int width, int height) {
    this.width=width;
    this.height=height;
  }

  public String toString() {
    return "ProxySurface[config "+config+
                ", displayHandle 0x"+Long.toHexString(getDisplayHandle())+
                ", surfaceHandle 0x"+Long.toHexString(getSurfaceHandle())+
                ", size "+getWidth()+"x"+getHeight()+"]";
  }

}

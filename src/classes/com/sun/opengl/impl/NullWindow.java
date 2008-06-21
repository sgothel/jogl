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

package com.sun.opengl.impl;

import javax.media.opengl.*;

public class NullWindow implements NativeWindow {
  protected boolean locked;
  protected int width, height, scrnIndex;
  protected long windowHandle, displayHandle;


  public NullWindow() {
    locked=false;
    width=0;
    height=0;
    scrnIndex=-1;
    windowHandle=0;
    displayHandle=0;
  }

  protected void init(Object windowObject) throws NativeWindowException {
  }

  protected void initNative() throws NativeWindowException {
  }

  public synchronized void invalidate() {
    locked = false;
  }

  public synchronized int lockSurface() throws NativeWindowException {
    if (locked) {
      throw new NativeWindowException("Surface already locked");
    }
    locked = true;
    return LOCK_SUCCESS;
  }

  public synchronized void unlockSurface() {
    if (locked) {
        locked = false;
    }
  }

  public synchronized boolean isSurfaceLocked() {
    return locked;
  }

  public long getDisplayHandle() {
    return windowHandle;
  }
  public void setDisplayHandle(long handle) {
    windowHandle=handle;
  }
  public long getScreenHandle() {
    return 0;
  }
  public int getScreenIndex() {
    return scrnIndex;
  }
  public void setScreenIndex(int idx) {
    scrnIndex=idx;
  }
  public long getWindowHandle() {
    return windowHandle;
  }
  public void setWindowHandle(long handle) {
    windowHandle=handle;
  }
  public long getVisualID() {
    return 0;
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

  public int getX() {
    return 0;
  }
  public int getY() {
    return 0;
  }

  public void setPosition(int x, int y) {
  }

  public void setVisible(boolean visible) {
  }

  public boolean isVisible() {
    return false;
  }

  public boolean setFullscreen(boolean fullscreen) {
    return false; // FIXME
  }

  public boolean isFullscreen() {
    return false; // FIXME
  }

}

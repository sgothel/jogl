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
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.impl.awt;

import com.sun.opengl.impl.*;

import java.awt.Component;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public abstract class JAWTWindow implements NativeWindow {
  protected static final boolean DEBUG = Debug.debug("GLDrawable");

  // lifetime: forever
  protected Component component;
  protected boolean locked;

  // lifetime: valid while locked
  protected long display;
  protected long screen;
  protected long drawable;
  // lifetime: valid after lock, forever
  protected long visualID;
  protected int  screenIndex;

  public JAWTWindow(Object comp) {
    init(comp);
  }

  protected void init(Object windowObject) throws NativeWindowException {
    this.locked = false;
    this.component = (Component)comp;
    this.display= null;
    this.screen= null;
    this.screenIndex = -1;
    this.drawable= null;
    this.visualID = 0;
    initNative();
  }

  public abstract boolean initNative() throws NativeWindowException;

  public boolean lockSurface() throws NativeWindowException {
    if (locked) {
      throw new NativeWindowException("Surface already locked");
    }
    locked = true;
  }

  public void unlockSurface() {
    if (!locked) {
      throw new NativeWindowException("Surface already locked");
    }
    locked = false;
    display= null;
    screen= null;
    drawable= null;
  }

  public boolean isSurfaceLocked() {
    return locked;
  }

  public long getDisplayHandle() {
    return display;
  }
  public long getScreenHandle() {
    return screen;
  }
  public int getScreenIndex() {
    return screenIndex;
  }
  public long getWindowHandle() {
    return drawable;
  }
  public long getVisualID() {
    return visualID;
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

  public int getX() {
    return component.getX();
  }
  public int getY() {
    return component.getY();
  }

  public void setPosition(int x, int y) {
    component.setLocation(x,y);
  }

  public void setVisible(boolean visible) {
    component.setVisible(visible);
  }

  public boolean isVisible() {
    return component.isVisible();
  }

  public boolean setFullscreen(boolean fullscreen) {
    return false; // FIXME
  }

  public boolean isFullscreen() {
    return false; // FIXME
  }

}

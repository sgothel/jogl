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

package com.sun.opengl.impl.jawt;

import com.sun.opengl.impl.*;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import javax.media.nwi.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public abstract class JAWTWindow implements NativeWindow {
  protected static final boolean DEBUG = Debug.debug("GLDrawable");

  // See whether we're running in headless mode
  private static boolean headlessMode;

  static {
    headlessMode = GraphicsEnvironment.isHeadless();
  }

  // lifetime: forever
  protected Component component;
  protected boolean locked;

  // lifetime: valid after lock, forever until invalidate
  protected long display;
  protected long screen;
  protected long drawable;
  protected long visualID;
  protected int  screenIndex;

  public JAWTWindow(Object comp) {
    init((Component)comp);
  }

  protected void init(Component windowObject) throws NativeWindowException {
    invalidate();
    this.component = windowObject;
    initNative();
  }

  protected abstract void initNative() throws NativeWindowException;

  public synchronized void invalidate() {
    locked = false;
    component = null;
    display= 0;
    screen= 0;
    screenIndex = -1;
    drawable= 0;
    visualID = 0;
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
  public long getSurfaceHandle() {
    return drawable;
  }
  public long getVisualID() {
    return visualID;
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

    sb.append("JAWT-Window[windowHandle "+getWindowHandle()+
                ", surfaceHandle "+getSurfaceHandle()+
                ", pos "+component.getX()+"/"+component.getY()+", size "+getWidth()+"x"+getHeight()+
                ", visible "+component.isVisible()+
                ", wrappedWindow "+getWrappedWindow()+
                ", visualID "+visualID+
                ", screen handle/index "+getScreenHandle()+"/"+getScreenIndex() +
                ", display handle "+getDisplayHandle()+"]");

    return sb.toString();
  }
}

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

package com.sun.opengl.impl;

import javax.media.opengl.*;

public abstract class GLDrawableImpl implements GLDrawable {
  private GLCapabilities requestedCapabilities;

  protected GLDrawableImpl(GLDrawableFactory factory,
                           NativeWindow comp,
                           GLCapabilities requestedCapabilities,
                           boolean realized) {
      this.factory = factory;
      this.component = comp;
      this.requestedCapabilities =
          (requestedCapabilities == null) ? null : (GLCapabilities) requestedCapabilities.clone();
      this.realized = realized;
  }

  public GLDrawableFactoryImpl getFactoryImpl() {
    return (GLDrawableFactoryImpl) getFactory();
  }

  /** For offscreen GLDrawables (pbuffers and "pixmap" drawables),
      indicates that native resources should be reclaimed. */
  public void destroy() throws GLException {
    setRealized(false);
  }

  public void swapBuffers() throws GLException {
  }

  public static String toHexString(long hex) {
    return GLContextImpl.toHexString(hex);
  }

  protected GLCapabilities getRequestedGLCapabilities() {
    return requestedCapabilities;
  }

  public GLCapabilities getChosenGLCapabilities() {
    if (chosenCapabilities == null)
      return null;

    // Must return a new copy to avoid mutation by end user
    return (GLCapabilities) chosenCapabilities.clone();
  }

  protected void setChosenGLCapabilities(GLCapabilities caps) {
    chosenCapabilities = (caps==null) ? null : (GLCapabilities) caps.clone();
  }

  public NativeWindow getNativeWindow() {
    return component;
  }

  public GLDrawableFactory getFactory() {
    return factory;
  }

  public void setRealized(boolean realized) {
    this.realized = realized;
    if(!realized) {
        setChosenGLCapabilities(null);
        component.invalidate();
    }
  }

  public boolean getRealized() {
    return realized;
  }

  public int getWidth() {
    return component.getWidth();
  }

  /** Returns the current height of this GLDrawable. */
  public int getHeight() {
    return component.getHeight();
  }

  public int lockSurface() throws GLException {
    if (!realized) {
      return NativeWindow.LOCK_SURFACE_NOT_READY;
    }
    return component.lockSurface();
  }

  public void unlockSurface() {
    component.unlockSurface();
  }

  public boolean isSurfaceLocked() {
    return component.isSurfaceLocked();
  }

  public String toString() {
    return "GLDrawable[realized "+getRealized()+
                ", window "+getNativeWindow()+
                ", factory "+getFactory()+"]";
  }

  protected GLDrawableFactory factory;
  protected NativeWindow component;
  private GLCapabilities chosenCapabilities;

  // Indicates whether the component (if an onscreen context) has been
  // realized. Plausibly, before the component is realized the JAWT
  // should return an error or NULL object from some of its
  // operations; this appears to be the case on Win32 but is not true
  // at least with Sun's current X11 implementation (1.4.x), which
  // crashes with no other error reported if the DrawingSurfaceInfo is
  // fetched from a locked DrawingSurface during the validation as a
  // result of calling show() on the main thread. To work around this
  // we prevent any JAWT or OpenGL operations from being done until
  // addNotify() is called on the component.
  protected boolean realized;

}

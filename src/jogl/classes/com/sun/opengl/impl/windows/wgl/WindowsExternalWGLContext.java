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

package com.sun.opengl.impl.windows.wgl;

import java.nio.*;
import java.util.*;
import javax.media.opengl.*;
import javax.media.nativewindow.*;
import com.sun.opengl.impl.*;
import com.sun.nativewindow.impl.NullWindow;

public class WindowsExternalWGLContext extends WindowsWGLContext {
  private boolean firstMakeCurrent = true;
  private boolean created = true;
  private GLContext lastContext;

  private WindowsExternalWGLContext(Drawable drawable, long hglrc, WindowsWGLGraphicsConfiguration cfg) {
    super(drawable, null);
    this.hglrc = hglrc;
    if (DEBUG) {
      System.err.println(getThreadName() + ": !!! Created external OpenGL context " + toHexString(hglrc) + " for " + this);
    }
    GLContextShareSet.contextCreated(this);
    setGLFunctionAvailability(false);
    cfg.updateCapabilitiesByWGL(this);
    getGLStateTracker().setEnabled(false); // external context usage can't track state in Java
  }

  protected static WindowsExternalWGLContext create(GLDrawableFactory factory, GLProfile glp) {
    long hdc = WGL.wglGetCurrentDC();
    if (0==hdc) {
      throw new GLException("Error: attempted to make an external GLDrawable without a drawable current");
    }
    long hglrc = WGL.wglGetCurrentContext();
    if (hglrc == 0) {
      throw new GLException("Error: attempted to make an external GLContext without a context current");
    }
    int pfdID = WGL.GetPixelFormat(hdc);
    if (pfdID == 0) {
      throw new GLException("Error: attempted to make an external GLContext without a valid pixelformat");
    }

    AbstractGraphicsScreen aScreen = DefaultGraphicsScreen.createDefault();
    WindowsWGLGraphicsConfiguration cfg = WindowsWGLGraphicsConfiguration.create(hdc, pfdID, glp, aScreen, true, true);

    NullWindow nw = new NullWindow(cfg);
    nw.setSurfaceHandle(hdc);

    return new WindowsExternalWGLContext(new Drawable(factory, nw), hglrc, cfg);
  }

  public int makeCurrent() throws GLException {
    // Save last context if necessary to allow external GLContexts to
    // talk to other GLContexts created by this library
    GLContext cur = getCurrent();
    if (cur != null && cur != this) {
      lastContext = cur;
      setCurrent(null);
    }
    return super.makeCurrent();
  }  

  public void release() throws GLException {
    super.release();
    setCurrent(lastContext);
    lastContext = null;
  }

  protected int makeCurrentImpl() throws GLException {
    if (firstMakeCurrent) {
      firstMakeCurrent = false;
      return CONTEXT_CURRENT_NEW;
    }
    return CONTEXT_CURRENT;
  }

  protected void releaseImpl() throws GLException {
  }

  protected void destroyImpl() throws GLException {
    created = false;
    GLContextShareSet.contextDestroyed(this);
  }

  public boolean isCreated() {
    return created;
  }

  // Need to provide the display connection to extension querying APIs
  static class Drawable extends WindowsWGLDrawable {
    Drawable(GLDrawableFactory factory, NativeWindow comp) {
      super(factory, comp, true);
    }

    public GLContext createContext(GLContext shareWith) {
      throw new GLException("Should not call this");
    }

    public int getWidth() {
      throw new GLException("Should not call this");
    }

    public int getHeight() {
      throw new GLException("Should not call this");
    }

    public void setSize(int width, int height) {
      throw new GLException("Should not call this");
    }
  }
}

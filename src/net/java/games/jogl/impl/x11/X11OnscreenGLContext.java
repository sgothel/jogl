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

package net.java.games.jogl.impl.x11;

import java.awt.Component;
import java.util.*;

import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class X11OnscreenGLContext extends X11GLContext {
  // Variables for lockSurface/unlockSurface
  private JAWT_DrawingSurface ds;
  private JAWT_DrawingSurfaceInfo dsi;
  private JAWT_X11DrawingSurfaceInfo x11dsi;

  // Variables for pbuffer support
  List pbuffersToInstantiate = new ArrayList();

  public X11OnscreenGLContext(Component component,
                              GLCapabilities capabilities,
                              GLCapabilitiesChooser chooser,
                              GLContext shareWith) {
    super(component, capabilities, chooser, shareWith);
  }
  
  protected GL createGL()
  {
    return new X11GLImpl(this);
  }
  
  protected boolean isOffscreen() {
    return false;
  }
  
  public int getOffscreenContextBufferedImageType() {
    throw new GLException("Should not call this");
  }

  public int getOffscreenContextReadBuffer() {
    throw new GLException("Should not call this");
  }

  public boolean offscreenImageNeedsVerticalFlip() {
    throw new GLException("Should not call this");
  }

  public boolean canCreatePbufferContext() {
    // FIXME: should we gate this on GLX 1.3 being available?
    return true;
  }

  public synchronized GLContext createPbufferContext(GLCapabilities capabilities,
                                                     int initialWidth,
                                                     int initialHeight) {
    X11PbufferGLContext ctx = new X11PbufferGLContext(capabilities, initialWidth, initialHeight);
    pbuffersToInstantiate.add(ctx);
    return ctx;
  }

  public void bindPbufferToTexture() {
    throw new GLException("Should not call this");
  }

  public void releasePbufferFromTexture() {
    throw new GLException("Should not call this");
  }

  public void setSwapInterval(int interval) {
    GL gl = getGL();
    if (gl.isExtensionAvailable("GLX_SGI_swap_control")) {
      gl.glXSwapIntervalSGI(interval);
    }
  }

  protected synchronized boolean makeCurrent(Runnable initAction) throws GLException {
    try {
      if (!lockSurface()) {
        return false;
      }
      boolean ret = super.makeCurrent(initAction);
      if (ret) {
        // Instantiate any pending pbuffers
        while (!pbuffersToInstantiate.isEmpty()) {
          X11PbufferGLContext ctx =
            (X11PbufferGLContext) pbuffersToInstantiate.remove(pbuffersToInstantiate.size() - 1);
          ctx.createPbuffer(display, context, getGL());
        }
      }
      return ret;
    } catch (RuntimeException e) {
      try {
        unlockSurface();
      } catch (Exception e2) {
        // do nothing if unlockSurface throws
      }
      throw(e); 
    }
  }

  protected synchronized void free() throws GLException {
    try {
      super.free();
    } finally {
      unlockSurface();
    }
  }

  public synchronized void swapBuffers() throws GLException {
    // FIXME: this cast to int would be wrong on 64-bit platforms
    // where the argument type to glXMakeCurrent would change (should
    // probably make GLXDrawable, and maybe XID, Opaque as long)
    GLX.glXSwapBuffers(display, (int) drawable);
  }

  private boolean lockSurface() throws GLException {
    if (drawable != 0) {
      throw new GLException("Surface already locked");
    }
    ds = getJAWT().GetDrawingSurface(component);
    if (ds == null) {
      // Widget not yet realized
      return false;
    }
    int res = ds.Lock();
    if ((res & JAWTFactory.JAWT_LOCK_ERROR) != 0) {
      throw new GLException("Unable to lock surface");
    }
    // See whether the surface changed and if so destroy the old
    // OpenGL context so it will be recreated
    if ((res & JAWTFactory.JAWT_LOCK_SURFACE_CHANGED) != 0) {
      if (context != 0) {
        GLX.glXDestroyContext(display, context);
        context = 0;
      }
    }
    dsi = ds.GetDrawingSurfaceInfo();
    if (dsi == null) {
      // Widget not yet realized
      ds.Unlock();
      getJAWT().FreeDrawingSurface(ds);
      ds = null;
      return false;
    }
    x11dsi = (JAWT_X11DrawingSurfaceInfo) dsi.platformInfo();
    display = x11dsi.display();
    drawable = x11dsi.drawable();
    visualID = x11dsi.visualID();
    if (display == 0 || drawable == 0) {
      // Widget not yet realized
      ds.FreeDrawingSurfaceInfo(dsi);
      ds.Unlock();
      getJAWT().FreeDrawingSurface(ds);
      ds = null;
      dsi = null;
      x11dsi = null;
      display = 0;
      drawable = 0;
      visualID = 0;
      return false;
    }
    mostRecentDisplay = display;
    return true;
  }

  private void unlockSurface() {
    if (drawable == 0) {
      throw new GLException("Surface already unlocked");
    }
    ds.FreeDrawingSurfaceInfo(dsi);
    ds.Unlock();
    getJAWT().FreeDrawingSurface(ds);
    ds = null;
    dsi = null;
    x11dsi = null;
    display = 0;
    drawable = 0;
    visualID = 0;
  }

  protected void create() {
    chooseVisualAndCreateContext(true);
  }    
}

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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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

package net.java.games.jogl.impl.windows;

import java.awt.Component;
import java.util.*;

import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class WindowsOnscreenGLContext extends WindowsGLContext {
  // Variables for lockSurface/unlockSurface
  JAWT_DrawingSurface ds;
  JAWT_DrawingSurfaceInfo dsi;
  JAWT_Win32DrawingSurfaceInfo win32dsi;

  // Variables for pbuffer support
  List pbuffersToInstantiate = new ArrayList();

  public WindowsOnscreenGLContext(Component component,
                                  GLCapabilities capabilities,
                                  GLCapabilitiesChooser chooser,
                                  GLContext shareWith) {
    super(component, capabilities, chooser, shareWith);
  }
  
  public void invokeGL(Runnable runnable, boolean isReshape, Runnable initAction) throws GLException {
    // Unfortunately, invokeGL can be called with the AWT tree lock
    // held, and the Windows onscreen implementation of
    // choosePixelFormatAndCreateContext calls
    // Component.getGraphicsConfiguration(), which grabs the tree
    // lock. To avoid deadlock we have to lock the tree lock before
    // grabbing the GLContext's lock if we're going to create an
    // OpenGL context during this call. This code might not be
    // completely correct, and we might need to uniformly grab the AWT
    // tree lock, which might become a performance issue...
    if (hglrc == 0) {
      synchronized(component.getTreeLock()) {
        super.invokeGL(runnable, isReshape, initAction);
      }
    } else {
      super.invokeGL(runnable, isReshape, initAction);
    }
  }

  protected GL createGL()
  {
    return new WindowsGLImpl(this);
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
    return haveWGLARBPbuffer();
  }

  public synchronized GLContext createPbufferContext(GLCapabilities capabilities,
                                                     int initialWidth,
                                                     int initialHeight) {
    WindowsPbufferGLContext ctx = new WindowsPbufferGLContext(capabilities, initialWidth, initialHeight);
    pbuffersToInstantiate.add(ctx);
    return ctx;
  }

  public void bindPbufferToTexture() {
    throw new GLException("Should not call this");
  }

  public void releasePbufferFromTexture() {
    throw new GLException("Should not call this");
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
          WindowsPbufferGLContext ctx =
            (WindowsPbufferGLContext) pbuffersToInstantiate.remove(pbuffersToInstantiate.size() - 1);
          ctx.createPbuffer(hdc, hglrc);
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
    if (!WGL.SwapBuffers(hdc) && (WGL.GetLastError() != 0)) {
      throw new GLException("Error swapping buffers");
    }
  }

  private boolean lockSurface() throws GLException {
    if (hdc != 0) {
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
    // OpenGL context so it will be recreated (NOTE: removeNotify
    // should handle this case, but it may be possible that race
    // conditions can cause this code to be triggered -- should test
    // more)
    if ((res & JAWTFactory.JAWT_LOCK_SURFACE_CHANGED) != 0) {
      if (hglrc != 0) {
        if (!WGL.wglDeleteContext(hglrc)) {
          throw new GLException("Unable to delete old GL context after surface changed");
        }
        GLContextShareSet.contextDestroyed(this);
        if (DEBUG) {
          System.err.println("!!! Destroyed OpenGL context " + hglrc + " due to JAWT_LOCK_SURFACE_CHANGED");
        }
        hglrc = 0;
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
    win32dsi = (JAWT_Win32DrawingSurfaceInfo) dsi.platformInfo();
    hdc = win32dsi.hdc();
    if (hdc == 0) {
      // Widget not yet realized
      ds.FreeDrawingSurfaceInfo(dsi);
      ds.Unlock();
      getJAWT().FreeDrawingSurface(ds);
      ds = null;
      dsi = null;
      win32dsi = null;
      return false;
    }
    return true;
  }

  private void unlockSurface() {
    if (hdc == 0) {
      throw new GLException("Surface already unlocked");
    }
    ds.FreeDrawingSurfaceInfo(dsi);
    ds.Unlock();
    getJAWT().FreeDrawingSurface(ds);
    ds = null;
    dsi = null;
    win32dsi = null;
    hdc = 0;
  }

  protected void create() {
    choosePixelFormatAndCreateContext(true);
  }    
}

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

package net.java.games.jogl.impl.macosx;

import java.awt.Component;
import java.util.*;

import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class MacOSXOnscreenGLContext extends MacOSXGLContext {
  // Variables for lockSurface/unlockSurface
  private JAWT_DrawingSurface ds;
  private JAWT_DrawingSurfaceInfo dsi;
  private JAWT_MacOSXDrawingSurfaceInfo macosxdsi;
  private Runnable myDeferredReshapeAction;
    
  // Variables for pbuffer support
  List pbuffersToInstantiate = new ArrayList();

  public MacOSXOnscreenGLContext(Component component,
                                 GLCapabilities capabilities,
                                 GLCapabilitiesChooser chooser,
                                 GLContext shareWith) {
    super(component, capabilities, chooser, shareWith);
  }

  // gznote: remove when updater is thread safe!
  public synchronized void invokeGL(final Runnable runnable, boolean isReshape, Runnable initAction) throws GLException {
    if (isReshape) {
      myDeferredReshapeAction = new Runnable() {
          public void run() {
            CGL.updateContext(nsContext, nsView);
            runnable.run();
          }
        };
    } else {
      if (myDeferredReshapeAction != null) {
        super.invokeGL(myDeferredReshapeAction, true, initAction);
        myDeferredReshapeAction = null;
      }
      super.invokeGL(runnable, isReshape, initAction);
    }
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
    return true;
  }
    
  public synchronized GLContext createPbufferContext(GLCapabilities capabilities, int initialWidth, int initialHeight) {
    MacOSXPbufferGLContext ctx = new MacOSXPbufferGLContext(capabilities, initialWidth, initialHeight);
    pbuffersToInstantiate.add(ctx);
    return ctx;
  }
    
  public void bindPbufferToTexture() {
    throw new GLException("Should not call this");
  }
    
  public void releasePbufferFromTexture() {
    throw new GLException("Should not call this");
  }
    
  public synchronized void setRenderingThread(Thread currentThreadOrNull, Runnable initAction) {
    this.willSetRenderingThread = false;
    // FIXME: the JAWT in the Panther developer release
    // requires all JAWT operations to be done on the AWT
    // thread. This means that setRenderingThread won't work
    // yet on this platform. This method can be deleted once
    // the update for that release ships.
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
          MacOSXPbufferGLContext ctx =
            (MacOSXPbufferGLContext) pbuffersToInstantiate.remove(pbuffersToInstantiate.size() - 1);
          ctx.createPbuffer(nsView, nsContext);
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
    if (!CGL.flushBuffer(nsContext, nsView)) {
      throw new GLException("Error swapping buffers");
    }
  }
        
  private boolean lockSurface() throws GLException {
    if (nsView != 0) {
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
    // OpenGL nsContext so it will be recreated
    if ((res & JAWTFactory.JAWT_LOCK_SURFACE_CHANGED) != 0) {
      if (nsContext != 0) {
		//CGL.updateContextUnregister(nsContext, nsView, updater); // gznote: not thread safe yet!
        if (!CGL.deleteContext(nsContext, nsView)) {
          throw new GLException("Unable to delete old GL nsContext after surface changed");
        }
      }
    }
        
    dsi = ds.GetDrawingSurfaceInfo();
    if (dsi == null) {
      ds.Unlock();
      getJAWT().FreeDrawingSurface(ds);
      ds = null;
            
      // Widget not yet realized
      return false;
    }
        
    macosxdsi = (JAWT_MacOSXDrawingSurfaceInfo) dsi.platformInfo();
    if (macosxdsi == null) {
      ds.FreeDrawingSurfaceInfo(dsi);
      ds.Unlock();
      getJAWT().FreeDrawingSurface(ds);
      ds = null;
      dsi = null;
                
      // Widget not yet realized
      return false;
    }
                        
    nsView = macosxdsi.cocoaViewRef();
    if (nsView == 0) {
      ds.FreeDrawingSurfaceInfo(dsi);
      ds.Unlock();
      getJAWT().FreeDrawingSurface(ds);
      ds = null;
      dsi = null;
      macosxdsi = null;
                
      // Widget not yet realized
      return false;
    }
        
    return true;
  }
    
  private void unlockSurface() throws GLException {
    if (nsView == 0) {
      throw new GLException("Surface already unlocked");
    }
        
    ds.FreeDrawingSurfaceInfo(dsi);
    ds.Unlock();
    getJAWT().FreeDrawingSurface(ds);
    ds = null;
    dsi = null;
    macosxdsi = null;
    nsView = 0;
  }
}

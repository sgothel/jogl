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
  private long nsView; // NSView
  private Runnable myDeferredReshapeAction;
    
  public MacOSXOnscreenGLContext(Component component,
                                 GLCapabilities capabilities,
                                 GLCapabilitiesChooser chooser,
                                 GLContext shareWith) {
    super(component, capabilities, chooser, shareWith);
  }
    
  public synchronized void invokeGL(final Runnable runnable, boolean isReshape, Runnable initAction) throws GLException {
    if (isReshape) {
      myDeferredReshapeAction = new Runnable() {
          public void run() {
            CGL.updateContext(nsView, nsContext);
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
    
  protected GL createGL() {
    return new MacOSXGLImpl(this);
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
    // For now say no
    return false;
  }
    
  public synchronized GLContext createPbufferContext(GLCapabilities capabilities, int initialWidth, int initialHeight) {
    throw new GLException("Not supported");
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

  protected void create() {
    MacOSXGLContext other = (MacOSXGLContext) GLContextShareSet.getShareContext(this);
    long share = 0;
    if (other != null) {
      share = other.getNSContext();
      if (share == 0) {
        throw new GLException("GLContextShareSet returned an invalid OpenGL context");
      }
    }
    nsContext = CGL.createContext(nsView, share);
    if (nsContext == 0) {
      throw new GLException("Error creating nsContext");
    }
    GLContextShareSet.contextCreated(this);
  }    
    
  protected synchronized boolean makeCurrent(Runnable initAction) throws GLException {
    try {
      if (!lockSurface()) {
        return false;
      }
            
      boolean created = false;
      if (nsContext == 0) {
        create();
        if (DEBUG) {
          System.err.println("!!! Created GL nsContext for " + getClass().getName());
        }
        created = true;
      }
            
      if (!CGL.makeCurrentContext(nsView, nsContext)) {
        throw new GLException("Error making nsContext current");
      }
            
      if (created) {
        resetGLFunctionAvailability();
        if (initAction != null) {
          initAction.run();
        }
      }
      return true;
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
      if (!CGL.clearCurrentContext(nsView, nsContext)) {
        throw new GLException("Error freeing OpenGL nsContext");
      }
    } finally {
      unlockSurface();
    }
  }
    
  protected synchronized void swapBuffers() throws GLException {
    if (!CGL.flushBuffer(nsView, nsContext)) {
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
        if (!CGL.deleteContext(nsView, nsContext)) {
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

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

package com.sun.opengl.impl.macosx;

import java.util.*;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public class MacOSXOnscreenGLContext extends MacOSXGLContext {
  protected MacOSXOnscreenGLDrawable drawable;

  public MacOSXOnscreenGLContext(MacOSXOnscreenGLDrawable drawable,
                                 GLContext shareWith) {
    super(drawable, shareWith);
    this.drawable = drawable;
  }

  protected int makeCurrentImpl() throws GLException {
    int lockRes = drawable.lockSurface();
    boolean exceptionOccurred = false;
    try {
      if (lockRes == MacOSXOnscreenGLDrawable.LOCK_SURFACE_NOT_READY) {
        return CONTEXT_NOT_CURRENT;
      }
      if (lockRes == MacOSXOnscreenGLDrawable.LOCK_SURFACE_CHANGED) {
        destroyImpl();
      }
      int ret = super.makeCurrentImpl();
      if ((ret == CONTEXT_CURRENT) ||
          (ret == CONTEXT_CURRENT_NEW)) {
        // Assume the canvas might have been resized or moved and tell the OpenGL
        // context to update itself. This used to be done only upon receiving a
        // reshape event but that doesn't appear to be sufficient. An experiment
        // was also done to add a HierarchyBoundsListener to the GLCanvas and
        // do this updating only upon reshape of this component or reshape or movement
        // of an ancestor, but this also wasn't sufficient and left garbage on the
        // screen in some situations.
        CGL.updateContext(nsContext);
      } else {
        if (!isOptimizable()) {
          // This can happen if the window currently is zero-sized, for example.
          // Make sure we don't leave the surface locked in this case.
          drawable.unlockSurface();
        }
      }
      return ret;
    } catch (RuntimeException e) {
      exceptionOccurred = true;
      throw e;
    } finally {
      if (exceptionOccurred ||
          (isOptimizable() && lockRes != MacOSXOnscreenGLDrawable.LOCK_SURFACE_NOT_READY)) {
        drawable.unlockSurface();
      }
    }
  }
    
  protected void releaseImpl() throws GLException {
    try {
      super.releaseImpl();
    } finally {
      if (!isOptimizable()) {
        drawable.unlockSurface();
      }
    }
  }

  public void swapBuffers() throws GLException {
    if (!CGL.flushBuffer(nsContext)) {
      throw new GLException("Error swapping buffers");
    }
  }

  protected void update() throws GLException {
    if (nsContext == 0) {
      throw new GLException("Context not created");
    }
    CGL.updateContext(nsContext);
  }

  protected boolean create() {
    return create(false, false);
  }

  public void setOpenGLMode(int mode) {
    if (mode != MacOSXGLDrawable.NSOPENGL_MODE)
      throw new GLException("OpenGL mode switching not supported for on-screen GLContexts");
  }
    
  public int  getOpenGLMode() {
    return MacOSXGLDrawable.NSOPENGL_MODE;
  }
}

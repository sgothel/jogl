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

package com.sun.opengl.impl.x11;

import java.util.*;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public class X11OnscreenGLContext extends X11GLContext {
  protected X11OnscreenGLDrawable drawable;
  // This indicates whether the context we have created is indirect
  // and therefore requires the toolkit to be locked around all GL
  // calls rather than just all GLX calls
  protected boolean isIndirect;

  public X11OnscreenGLContext(X11OnscreenGLDrawable drawable,
                              GLContext shareWith) {
    super(drawable, shareWith);
    this.drawable = drawable;
  }
  
  protected int makeCurrentImpl() throws GLException {
    int lockRes = drawable.lockSurface();
    boolean exceptionOccurred = false;
    try {
      if (lockRes == X11OnscreenGLDrawable.LOCK_SURFACE_NOT_READY) {
        return CONTEXT_NOT_CURRENT;
      }
      if (lockRes == X11OnscreenGLDrawable.LOCK_SURFACE_CHANGED) {
        destroyImpl();
      }
      return super.makeCurrentImpl();
    } catch (RuntimeException e) {
      exceptionOccurred = true;
      throw e;
    } finally {
      if (exceptionOccurred ||
          (isOptimizable() && lockRes != X11OnscreenGLDrawable.LOCK_SURFACE_NOT_READY)) {
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

  public boolean isOptimizable() {
    return super.isOptimizable() && !isIndirect;
  }

  protected void create() {
    createContext(true);
    isIndirect = !GLX.glXIsDirect(drawable.getDisplay(), context);
  }
}

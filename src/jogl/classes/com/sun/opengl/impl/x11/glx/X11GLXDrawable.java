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

package com.sun.opengl.impl.x11.glx;

import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.nativewindow.impl.x11.*;
import com.sun.gluegen.runtime.DynamicLookupHelper;

public abstract class X11GLXDrawable extends GLDrawableImpl {
  protected X11GLXDrawable(GLDrawableFactory factory, NativeWindow comp, boolean realized) {
    super(factory, comp, realized);
  }

  public DynamicLookupHelper getDynamicLookupHelper() {
    return (X11GLXDrawableFactory) getFactoryImpl() ;
  }

  protected void setRealizedImpl() {
    if(!realized) {
        return; // nothing to do 
    }

    if(NativeWindow.LOCK_SURFACE_NOT_READY == lockSurface()) {
      throw new GLException("X11GLXDrawable.setRealized(true): lockSurface - surface not ready");
    }
    try {
        X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration();
        config.updateGraphicsConfiguration();

        if (DEBUG) {
          System.err.println("!!! X11GLXDrawable.setRealized(true): "+config);
        }
    } finally {
        unlockSurface();
    }
  }

  protected void swapBuffersImpl() {
    boolean didLock = false;
    try {
      if ( !isSurfaceLocked() ) {
          // Usually the surface shall be locked within [makeCurrent .. swap .. release]
          if (lockSurface() == NativeWindow.LOCK_SURFACE_NOT_READY) {
              return;
          }
          didLock=true;
      }

      GLX.glXSwapBuffers(component.getDisplayHandle(), component.getSurfaceHandle());

    } finally {
      if(didLock) {
          unlockSurface();
      }
    }
  }

  //---------------------------------------------------------------------------
  // Internals only below this point
  //
}

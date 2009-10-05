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

import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.gluegen.runtime.DynamicLookupHelper;

public abstract class WindowsWGLDrawable extends GLDrawableImpl {
  private static final int MAX_SET_PIXEL_FORMAT_FAIL_COUNT = 5;
  private static final boolean PROFILING = Debug.debug("WindowsWGLDrawable.profiling");
  private static final int PROFILING_TICKS = 200;
  private int  profilingLockSurfaceTicks;
  private long profilingLockSurfaceTime;
  private int  profilingUnlockSurfaceTicks;
  private long profilingUnlockSurfaceTime;
  private int  profilingSwapBuffersTicks;
  private long profilingSwapBuffersTime;


  public WindowsWGLDrawable(GLDrawableFactory factory, NativeWindow comp, boolean realized) {
    super(factory, comp, realized);
  }

  protected void setRealizedImpl() {
    if(!realized) {
        return; // nothing todo ..
    }

    if(NativeWindow.LOCK_SURFACE_NOT_READY == lockSurface()) {
      throw new GLException("WindowsWGLDrawable.setRealized(true): lockSurface - surface not ready");
    }
    try {
        NativeWindow nativeWindow = getNativeWindow();
        WindowsWGLGraphicsConfiguration config = (WindowsWGLGraphicsConfiguration)nativeWindow.getGraphicsConfiguration().getNativeGraphicsConfiguration();
        config.updateGraphicsConfiguration(getFactory(), nativeWindow);
        if (DEBUG) {
          System.err.println("!!! WindowsWGLDrawable.setRealized(true): "+config);
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
            didLock = true;
        }

        long startTime = 0;
        if (PROFILING) {
          startTime = System.currentTimeMillis();
        }

        if (!WGL.SwapBuffers(getNativeWindow().getSurfaceHandle()) && (WGL.GetLastError() != 0)) {
          throw new GLException("Error swapping buffers");
        }

        if (PROFILING) {
          long endTime = System.currentTimeMillis();
          profilingSwapBuffersTime += (endTime - startTime);
          int ticks = PROFILING_TICKS;
          if (++profilingSwapBuffersTicks == ticks) {
            System.err.println("SwapBuffers calls: " + profilingSwapBuffersTime + " ms / " + ticks + "  calls (" +
                               ((float) profilingSwapBuffersTime / (float) ticks) + " ms/call)");
            profilingSwapBuffersTime = 0;
            profilingSwapBuffersTicks = 0;
          }
        }
    } finally {
        if (didLock) {
          unlockSurface();
        }
    }
  }

  public DynamicLookupHelper getDynamicLookupHelper() {
    return (WindowsWGLDrawableFactory) getFactoryImpl() ;
  }

  protected static String getThreadName() {
    return Thread.currentThread().getName();
  }
}

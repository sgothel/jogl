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

public abstract class WindowsWGLDrawable extends GLDrawableImpl {
  protected static final boolean DEBUG = Debug.debug("WindowsWGLDrawable");

  // Workaround for problems on Intel 82855 cards
  private int  setPixelFormatFailCount;
  private static final int MAX_SET_PIXEL_FORMAT_FAIL_COUNT = 5;

  public WindowsWGLDrawable(GLDrawableFactory factory, NativeWindow comp, boolean realized) {
    super(factory, comp, realized);
  }

  public int lockSurface() throws GLException {
    int ret = super.lockSurface();
    if(NativeWindow.LOCK_SURFACE_NOT_READY == ret) {
      if (DEBUG) {
          System.err.println("WindowsWGLDrawable.lockSurface: surface not ready");
      }
      return ret;
    }
    NativeWindow nativeWindow = getNativeWindow();
    WindowsWGLGraphicsConfiguration config = (WindowsWGLGraphicsConfiguration)nativeWindow.getGraphicsConfiguration().getNativeGraphicsConfiguration();
    if (!config.getIsUpdated()) {
      try {
        config.update(getFactory(), nativeWindow, false);
        setPixelFormatFailCount = 0;
      } catch (RuntimeException e) {
        if (DEBUG) {
          System.err.println("WindowsWGLDrawable.lockSurface: squelching exception");
          e.printStackTrace();
        }
        // Workaround for problems seen on Intel 82855 cards in particular
        // Make it look like the lockSurface() call didn't succeed
        unlockSurface();
        if (e instanceof GLException) {
          if (++setPixelFormatFailCount == MAX_SET_PIXEL_FORMAT_FAIL_COUNT) {
            setPixelFormatFailCount = 0;
            throw e;
          }
          return NativeWindow.LOCK_SURFACE_NOT_READY;
        } else {
          // Probably a user error in the GLCapabilitiesChooser or similar.
          // Don't propagate non-GLExceptions out because calling code
          // expects to catch only that exception type
          throw new GLException(e);
        }
      }
    }
    return ret;
  }

  protected static String getThreadName() {
    return Thread.currentThread().getName();
  }
}

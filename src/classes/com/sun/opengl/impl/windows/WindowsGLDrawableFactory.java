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

package com.sun.opengl.impl.windows;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public class WindowsGLDrawableFactory extends GLDrawableFactoryImpl {
  private static final boolean DEBUG = Debug.debug("WindowsGLDrawableFactory");
  private static final boolean VERBOSE = Debug.verbose();

  // Handle to GLU32.dll
  // FIXME: this should go away once we delete support for the C GLU library
  private long hglu32;

  static {
    NativeLibLoader.load();
  }
  
  public GraphicsConfiguration chooseGraphicsConfiguration(GLCapabilities capabilities,
                                                           GLCapabilitiesChooser chooser,
                                                           GraphicsDevice device) {
    return null;
  }

  public GLDrawable getGLDrawable(Object target,
                                  GLCapabilities capabilities,
                                  GLCapabilitiesChooser chooser) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    if (!(target instanceof Component)) {
      throw new IllegalArgumentException("GLDrawables not supported for objects of type " +
                                         target.getClass().getName() + " (only Components are supported in this implementation)");
    }
    if (capabilities == null) {
      capabilities = new GLCapabilities();
    }
    if (chooser == null) {
      chooser = new DefaultGLCapabilitiesChooser();
    }
    return new WindowsOnscreenGLDrawable((Component) target, capabilities, chooser);
  }

  public GLDrawableImpl createOffscreenDrawable(GLCapabilities capabilities,
                                                GLCapabilitiesChooser chooser) {
    return new WindowsOffscreenGLDrawable(capabilities, chooser);
  }

  private boolean pbufferSupportInitialized = false;
  private boolean canCreateGLPbuffer = false;
  public boolean canCreateGLPbuffer() {
    if (!pbufferSupportInitialized) {
      Runnable r = new Runnable() {
          public void run() {
            WindowsDummyGLDrawable dummyDrawable = new WindowsDummyGLDrawable();
            GLContext dummyContext  = dummyDrawable.createContext(null);
            if (dummyContext != null) {
              GLContext lastContext = GLContext.getCurrent();
              if (lastContext != null) {
                lastContext.release();
              }
              dummyContext.makeCurrent();
              GL dummyGL = dummyContext.getGL();
              canCreateGLPbuffer = dummyGL.isExtensionAvailable("GL_ARB_pbuffer");
              pbufferSupportInitialized = true;
              dummyContext.release();
              dummyContext.destroy();
              dummyDrawable.destroy();
              if (lastContext != null) {
                lastContext.makeCurrent();
              }
            }
          }
        };
      maybeDoSingleThreadedWorkaround(r);
    }
    return canCreateGLPbuffer;
  }

  public GLPbuffer createGLPbuffer(final GLCapabilities capabilities,
                                   final int initialWidth,
                                   final int initialHeight,
                                   final GLContext shareWith) {
    if (!canCreateGLPbuffer()) {
      throw new GLException("Pbuffer support not available with current graphics card");
    }
    final List returnList = new ArrayList();
    Runnable r = new Runnable() {
        public void run() {
          WindowsDummyGLDrawable dummyDrawable = new WindowsDummyGLDrawable();
          WindowsGLContext       dummyContext  = (WindowsGLContext) dummyDrawable.createContext(null);
          GLContext lastContext = GLContext.getCurrent();
          if (lastContext != null) {
            lastContext.release();
          }
          dummyContext.makeCurrent();
          WGLExt dummyWGLExt = dummyContext.getWGLExt();
          try {
            WindowsPbufferGLDrawable pbufferDrawable = new WindowsPbufferGLDrawable(capabilities,
                                                                                    initialWidth,
                                                                                    initialHeight,
                                                                                    dummyDrawable,
                                                                                    dummyWGLExt);
            GLPbufferImpl pbuffer = new GLPbufferImpl(pbufferDrawable, shareWith);
            returnList.add(pbuffer);
            dummyContext.release();
            dummyContext.destroy();
            dummyDrawable.destroy();
          } finally {
            if (lastContext != null) {
              lastContext.makeCurrent();
            }
          }
        }
      };
    maybeDoSingleThreadedWorkaround(r);
    return (GLPbuffer) returnList.get(0);
  }

  public GLContext createExternalGLContext() {
    return new WindowsExternalGLContext();
  }

  public boolean canCreateExternalGLDrawable() {
    return true;
  }

  public GLDrawable createExternalGLDrawable() {
    return new WindowsExternalGLDrawable();
  }

  public long dynamicLookupFunction(String glFuncName) {
    long res = WGL.wglGetProcAddress(glFuncName);
    if (res == 0) {
      // GLU routines aren't known to the OpenGL function lookup
      if (hglu32 == 0) {
        hglu32 = WGL.LoadLibraryA("GLU32");
        if (hglu32 == 0) {
          throw new GLException("Error loading GLU32.DLL");
        }
      }
      res = WGL.GetProcAddress(hglu32, glFuncName);
    }
    return res;
  }

  static String wglGetLastError() {
    int err = WGL.GetLastError();
    String detail = null;
    switch (err) {
      case WGL.ERROR_INVALID_PIXEL_FORMAT: detail = "ERROR_INVALID_PIXEL_FORMAT";       break;
      case WGL.ERROR_NO_SYSTEM_RESOURCES:  detail = "ERROR_NO_SYSTEM_RESOURCES";        break;
      case WGL.ERROR_INVALID_DATA:         detail = "ERROR_INVALID_DATA";               break;
      case WGL.ERROR_PROC_NOT_FOUND:       detail = "ERROR_PROC_NOT_FOUND";             break;
      case WGL.ERROR_INVALID_WINDOW_HANDLE:detail = "ERROR_INVALID_WINDOW_HANDLE";      break;
      default:                             detail = "(Unknown error code " + err + ")"; break;
    }
    return detail;
  }

  private void maybeDoSingleThreadedWorkaround(Runnable action) {
    if (Threading.isSingleThreaded() &&
        !Threading.isOpenGLThread()) {
      Threading.invokeOnOpenGLThread(action);
    } else {
      action.run();
    }
  }

  public void lockAWTForJava2D() {
  }

  public void unlockAWTForJava2D() {
  }
}

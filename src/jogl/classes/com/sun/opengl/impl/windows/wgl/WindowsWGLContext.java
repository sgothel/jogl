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

import java.nio.*;
import java.util.*;
import javax.media.opengl.*;
import javax.media.nativewindow.*;
import com.sun.opengl.impl.*;
import com.sun.gluegen.runtime.ProcAddressTable;

public class WindowsWGLContext extends GLContextImpl {
  protected WindowsWGLDrawable drawable;
  protected long hglrc;
  private boolean wglGetExtensionsStringEXTInitialized;
  private boolean wglGetExtensionsStringEXTAvailable;
  private static final Map/*<String, String>*/ functionNameMap;
  private static final Map/*<String, String>*/ extensionNameMap;
  private WGLExt wglExt;
  // Table that holds the addresses of the native C-language entry points for
  // WGL extension functions.
  private WGLExtProcAddressTable wglExtProcAddressTable;

  static {
    functionNameMap = new HashMap();
    functionNameMap.put("glAllocateMemoryNV", "wglAllocateMemoryNV");
    functionNameMap.put("glFreeMemoryNV", "wglFreeMemoryNV");

    extensionNameMap = new HashMap();
    extensionNameMap.put("GL_ARB_pbuffer", "WGL_ARB_pbuffer");
    extensionNameMap.put("GL_ARB_pixel_format", "WGL_ARB_pixel_format");
  }

  // FIXME: figure out how to hook back in the Java 2D / JOGL bridge
  public WindowsWGLContext(WindowsWGLDrawable drawable,
                          GLContext shareWith) {
    super(drawable.getGLProfile(), shareWith);
    this.drawable = drawable;
  }

  public Object getPlatformGLExtensions() {
    return getWGLExt();
  }

  public WGLExt getWGLExt() {
    if (wglExt == null) {
      wglExt = new WGLExtImpl(this);
    }
    return wglExt;
  }

  public final ProcAddressTable getPlatformExtProcAddressTable() {
    return getWGLExtProcAddressTable();
  }

  public final WGLExtProcAddressTable getWGLExtProcAddressTable() {
    return wglExtProcAddressTable;
  }

  public GLDrawable getGLDrawable() {
    return drawable;
  }

  protected String mapToRealGLFunctionName(String glFunctionName) {
    String lookup = (String) functionNameMap.get(glFunctionName);
    if (lookup != null) {
      return lookup;
    }
    return glFunctionName;
  }

  protected String mapToRealGLExtensionName(String glExtensionName) {
    String lookup = (String) extensionNameMap.get(glExtensionName);
    if (lookup != null) {
      return lookup;
    }
    return glExtensionName;
  }

  /**
   * Creates and initializes an appropriate OpenGL context. Should only be
   * called by {@link #makeCurrentImpl()}.
   */
  protected void create() {
    GLCapabilities glCaps = drawable.getChosenGLCapabilities();
    if(DEBUG) {
          System.err.println("WindowsWGLContext.create got "+glCaps);
    }

    if (drawable.getNativeWindow().getSurfaceHandle() == 0) {
      throw new GLException("Internal error: attempted to create OpenGL context without an associated drawable");
    }
    // Windows can set up sharing of display lists after creation time
    WindowsWGLContext other = (WindowsWGLContext) GLContextShareSet.getShareContext(this);
    long hglrc2 = 0;
    if (other != null) {
      hglrc2 = other.getHGLRC();
      if (hglrc2 == 0) {
        throw new GLException("GLContextShareSet returned an invalid OpenGL context");
      }
    }

    // To use WGL_ARB_create_context, we have to make a temp context current,
    // so we are able to use GetProcAddress
    long temp_hglrc = WGL.wglCreateContext(drawable.getNativeWindow().getSurfaceHandle());
    if (DEBUG) {
      System.err.println(getThreadName() + ": !!! Created temp OpenGL context " + toHexString(temp_hglrc) + " for " + this + ", device context " + toHexString(drawable.getNativeWindow().getSurfaceHandle()) + ", not yet sharing");
    }
    if (temp_hglrc == 0) {
      throw new GLException("Unable to create temp OpenGL context for device context " + toHexString(drawable.getNativeWindow().getSurfaceHandle()));
    } else {
        if (!WGL.wglMakeCurrent(drawable.getNativeWindow().getSurfaceHandle(), temp_hglrc)) {
            throw new GLException("Error making temp context current: " + WGL.GetLastError());
        }
        setGLFunctionAvailability(true);

        if( !isFunctionAvailable("wglCreateContextAttribsARB") ||
            !isExtensionAvailable("WGL_ARB_create_context") ) {
            if(glCaps.getGLProfile().isGL3()) {
              if (!WGL.wglMakeCurrent(0, 0)) {
                throw new GLException("Error freeing temp OpenGL context: " + WGL.GetLastError());
              }
              if (!WGL.wglDeleteContext(temp_hglrc)) {
                throw new GLException("Unable to delete OpenGL context");
              }
              throw new GLException("Unable to create OpenGL 3.1 context (no WGL_ARB_create_context)");
            }

            // continue with temp context for GL < 3.0
            hglrc = temp_hglrc;
            if(DEBUG) {
              System.err.println("WindowsWGLContext.create done (old ctx < 3.0 - no WGL_ARB_create_context) 0x"+Long.toHexString(hglrc));
            }
        } else {
            WGLExt wglExt = getWGLExt();

            // preset with default values
            int attribs[] = {
                WGLExt.WGL_CONTEXT_MAJOR_VERSION_ARB, 3,
                WGLExt.WGL_CONTEXT_MINOR_VERSION_ARB, 0,
                WGLExt.WGL_CONTEXT_FLAGS_ARB, 0,      
                0
            };

            if(glCaps.getGLProfile().isGL3()) {
                attribs[1] |= 3;
                attribs[3] |= 1;
                // attribs[5] |= WGLExt.WGL_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB ; // NVidia WGL driver doesn't support this one ..
                // attribs[5] |= WGLExt.WGL_CONTEXT_DEBUG_BIT_ARB ;
            }

            hglrc = wglExt.wglCreateContextAttribsARB(drawable.getNativeWindow().getSurfaceHandle(), hglrc2, attribs, 0); 
            if(0==hglrc) {
                if(glCaps.getGLProfile().isGL3()) {
                  if (!WGL.wglMakeCurrent(0, 0)) {
                    throw new GLException("Error freeing temp OpenGL context: " + WGL.GetLastError());
                  }
                  if (!WGL.wglDeleteContext(temp_hglrc)) {
                    throw new GLException("Unable to delete OpenGL context");
                  }
                  throw new GLException("Unable to create OpenGL 3.1 context (have WGL_ARB_create_context)");
                }

                // continue with temp context for GL < 3.0
                hglrc = temp_hglrc;
                if(DEBUG) {
                  System.err.println("WindowsWGLContext.create done (old ctx < 3.0 - no 3.0) 0x"+Long.toHexString(hglrc));
                }
            } else {
                hglrc2 = 0; // mark as shared ..
                if (!WGL.wglMakeCurrent(0, 0)) {
                    throw new GLException("Error freeing temp OpenGL context: " + WGL.GetLastError());
                }
                if (!WGL.wglDeleteContext(temp_hglrc)) {
                    throw new GLException("Unable to delete temp OpenGL context");
                }

                if (!WGL.wglMakeCurrent(drawable.getNativeWindow().getSurfaceHandle(), hglrc)) {
                    throw new GLException("Error making new context current: " + WGL.GetLastError());
                }
                updateGLProcAddressTable();
                if(DEBUG) {
                  System.err.println("WindowsWGLContext.create done (new ctx >= 3.0) 0x"+Long.toHexString(hglrc));
                }
            }
        }
    }
    if(0!=hglrc2) {
        if (!WGL.wglShareLists(hglrc2, hglrc)) {
            throw new GLException("wglShareLists(" + toHexString(hglrc2) +
                                  ", " + toHexString(hglrc) + ") failed: error code " +
                                  WGL.GetLastError());
        }
    }
    GLContextShareSet.contextCreated(this);
    if (DEBUG) {
      System.err.println(getThreadName() + ": !!! Created OpenGL context " + toHexString(hglrc) + " for " + this + ", device context " + toHexString(drawable.getNativeWindow().getSurfaceHandle()) + ", sharing with " + toHexString(hglrc2));
    }
  }
  
  protected int makeCurrentImpl() throws GLException {
    if (drawable.getNativeWindow().getSurfaceHandle() == 0) {
        if (DEBUG) {
          System.err.println("drawable not properly initialized");
        }
        return CONTEXT_NOT_CURRENT;
    }
    boolean created = false;
    if (hglrc == 0) {
      create();
      if (DEBUG) {
        System.err.println(getThreadName() + ": !!! Created GL context for " + getClass().getName());
      }
      created = true;
    }

    if (WGL.wglGetCurrentContext() != hglrc) {
      if (!WGL.wglMakeCurrent(drawable.getNativeWindow().getSurfaceHandle(), hglrc)) {
        throw new GLException("Error making context current: " + WGL.GetLastError());
      } else {
        if (DEBUG && VERBOSE) {
          System.err.println(getThreadName() + ": wglMakeCurrent(hdc " + toHexString(drawable.getNativeWindow().getSurfaceHandle()) +
                             ", hglrc " + toHexString(hglrc) + ") succeeded");
        }
      }
    }

    if (created) {
      setGLFunctionAvailability(false);
      return CONTEXT_CURRENT_NEW;
    }
    return CONTEXT_CURRENT;
  }

  protected void releaseImpl() throws GLException {
    if (!WGL.wglMakeCurrent(0, 0)) {
        throw new GLException("Error freeing OpenGL context: " + WGL.GetLastError());
    }
  }

  protected void destroyImpl() throws GLException {
    if (DEBUG) {
        Exception e = new Exception(getThreadName() + ": !!! Destroyed OpenGL context " + toHexString(hglrc));
        e.printStackTrace();
    }
    if (hglrc != 0) {
      if (!WGL.wglDeleteContext(hglrc)) {
        throw new GLException("Unable to delete OpenGL context");
      }
      hglrc = 0;
      GLContextShareSet.contextDestroyed(this);
    }
  }

  public boolean isCreated() {
    return (hglrc != 0);
  }

  public void copy(GLContext source, int mask) throws GLException {
    long dst = getHGLRC();
    long src = ((WindowsWGLContext) source).getHGLRC();
    if (src == 0) {
      throw new GLException("Source OpenGL context has not been created");
    }
    if (dst == 0) {
      throw new GLException("Destination OpenGL context has not been created");
    }
    if (!WGL.wglCopyContext(src, dst, mask)) {
      throw new GLException("wglCopyContext failed");
    }
  }

  protected void updateGLProcAddressTable() {
    super.updateGLProcAddressTable();
    if (DEBUG) {
      System.err.println(getThreadName() + ": !!! Initializing WGL extension address table for " + this);
    }
    if (wglExtProcAddressTable == null) {
      // FIXME: cache ProcAddressTables by capability bits so we can
      // share them among contexts with the same capabilities
      wglExtProcAddressTable = new WGLExtProcAddressTable();
    }          
    resetProcAddressTable(getWGLExtProcAddressTable());
  }
  
  public String getPlatformExtensionsString() {
    if (!wglGetExtensionsStringEXTInitialized) {
      wglGetExtensionsStringEXTAvailable = (WGL.wglGetProcAddress("wglGetExtensionsStringEXT") != 0);
      wglGetExtensionsStringEXTInitialized = true;
    }
    if (wglGetExtensionsStringEXTAvailable) {
      return getWGLExt().wglGetExtensionsStringEXT();
    } else {
      return "";
    }
  }

  public boolean isFunctionAvailable(String glFunctionName)
  {
    boolean available = super.isFunctionAvailable(glFunctionName);
    
    // Sanity check for implementations that use proc addresses for run-time
    // linking: if the function IS available, then make sure there's a proc
    // address for it if it's an extension or not part of the OpenGL 1.1 core
    // (post GL 1.1 functions are run-time linked on windows).
    /* FIXME: 
    assert(!available ||
           (getGLProcAddressTable().getAddressFor(mapToRealGLFunctionName(glFunctionName)) != 0 ||
            FunctionAvailabilityCache.isPartOfGLCore("1.1", mapToRealGLFunctionName(glFunctionName)))
           ); */

    return available;
  }
  
  public void setSwapInterval(int interval) {
    // FIXME: make the context current first? Currently assumes that
    // will not be necessary. Make the caller do this?
    WGLExt wglExt = getWGLExt();
    if (wglExt.isExtensionAvailable("WGL_EXT_swap_control")) {
      wglExt.wglSwapIntervalEXT(interval);
    }
  }

  public ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3) {
    return getWGLExt().wglAllocateMemoryNV(arg0, arg1, arg2, arg3);
  }

  public int getOffscreenContextPixelDataType() {
    throw new GLException("Should not call this");
  }

  public int getOffscreenContextReadBuffer() {
    throw new GLException("Should not call this");
  }

  public boolean offscreenImageNeedsVerticalFlip() {
    throw new GLException("Should not call this");
  }

  public void bindPbufferToTexture() {
    throw new GLException("Should not call this");
  }

  public void releasePbufferFromTexture() {
    throw new GLException("Should not call this");
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  public long getHGLRC() {
    return hglrc;
  }
}

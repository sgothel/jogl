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

package net.java.games.jogl.impl.windows;

import java.util.*;
import net.java.games.gluegen.runtime.*; // for PROCADDRESS_VAR_PREFIX
import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class WindowsGLContext extends GLContextImpl {
  protected WindowsGLDrawable drawable;
  protected long hglrc;
  private boolean wglGetExtensionsStringEXTInitialized;
  private boolean wglGetExtensionsStringEXTAvailable;
  private static final Map/*<String, String>*/ functionNameMap;
  private static final Map/*<String, String>*/ extensionNameMap;
  // Table that holds the addresses of the native C-language entry points for
  // OpenGL functions.
  private GLProcAddressTable glProcAddressTable;
  // Handle to GLU32.dll
  private long hglu32;
  private boolean haveWGLARBPbuffer = true;

  static {
    functionNameMap = new HashMap();
    functionNameMap.put("glAllocateMemoryNV", "wglAllocateMemoryNV");
    functionNameMap.put("glFreeMemoryNV", "wglFreeMemoryNV");

    extensionNameMap = new HashMap();
    extensionNameMap.put("GL_ARB_pbuffer", "WGL_ARB_pbuffer");
    extensionNameMap.put("GL_ARB_pixel_format", "WGL_ARB_pixel_format");
  }

  public WindowsGLContext(WindowsGLDrawable drawable,
                          GLContext shareWith) {
    super(shareWith);
    this.drawable = drawable;
  }
  
  protected GL createGL()
  {
    return new WindowsGLImpl(this);
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
    if (drawable.getHDC() == 0) {
      throw new GLException("Internal error: attempted to create OpenGL context without an associated drawable");
    }
    hglrc = WGL.wglCreateContext(drawable.getHDC());
    if (hglrc == 0) {
      throw new GLException("Unable to create OpenGL context");
    }
    // Windows can set up sharing of display lists after creation time
    WindowsGLContext other = (WindowsGLContext) GLContextShareSet.getShareContext(this);
    if (other != null) {
      long hglrc2 = other.getHGLRC();
      if (hglrc2 == 0) {
        throw new GLException("GLContextShareSet returned an invalid OpenGL context");
      }
      if (!WGL.wglShareLists(hglrc2, hglrc)) {
        throw new GLException("wglShareLists(0x" + Long.toHexString(hglrc2) +
                              ", 0x" + Long.toHexString(hglrc) + ") failed: error code " +
                              WGL.GetLastError());
      }
    }
    GLContextShareSet.contextCreated(this);      
    if (DEBUG) {
      System.err.println(getThreadName() + ": !!! Created OpenGL context " + toHexString(hglrc) + " for " + this + ", device context " + toHexString(drawable.getHDC()));
    }
  }
  
  protected int makeCurrentImpl() throws GLException {
    boolean created = false;
    if (hglrc == 0) {
      create();
      if (DEBUG) {
        System.err.println(getThreadName() + ": !!! Created GL context for " + getClass().getName());
      }
      created = true;
    }

    boolean skipMakeCurrent = false;
    if (NO_FREE) {
      if (WGL.wglGetCurrentContext() == hglrc) {
        if (DEBUG && VERBOSE) {
          System.err.println(getThreadName() + ": skipping wglMakeCurrent because context already current");
        }
        skipMakeCurrent = true;
      }
    }

    if (!skipMakeCurrent) {
      if (!WGL.wglMakeCurrent(drawable.getHDC(), hglrc)) {
        throw new GLException("Error making context current: " + WGL.GetLastError());
      } else {
        if (DEBUG && VERBOSE) {
          System.err.println(getThreadName() + ": wglMakeCurrent(hdc " + toHexString(drawable.getHDC()) +
                             ", hglrc " + toHexString(hglrc) + ") succeeded");
        }
      }
    }

    if (created) {
      resetGLFunctionAvailability();
      haveWGLARBPbuffer = (isExtensionAvailable("WGL_ARB_pbuffer") &&
                           isExtensionAvailable("WGL_ARB_pixel_format"));
      return CONTEXT_CURRENT_NEW;
    }
    return CONTEXT_CURRENT;
  }

  protected void releaseImpl() throws GLException {
    if (!NO_FREE) {
      if (!WGL.wglMakeCurrent(0, 0)) {
        throw new GLException("Error freeing OpenGL context: " + WGL.GetLastError());
      }
    }
  }

  protected void destroyImpl() throws GLException {
    if (hglrc != 0) {
      if (!WGL.wglDeleteContext(hglrc)) {
        throw new GLException("Unable to delete OpenGL context");
      }
      hglrc = 0;
      GLContextShareSet.contextDestroyed(this);
      if (DEBUG) {
        System.err.println(getThreadName() + ": !!! Destroyed OpenGL context " + toHexString(hglrc));
      }
    }
  }

  protected long dynamicLookupFunction(String glFuncName) {
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

  public boolean isCreated() {
    return (hglrc != 0);
  }

  protected void resetGLFunctionAvailability() {
    super.resetGLFunctionAvailability();
    if (DEBUG) {
      System.err.println(getThreadName() + ": !!! Initializing OpenGL extension address table");
    }
    resetProcAddressTable(getGLProcAddressTable());
  }
  
  public GLProcAddressTable getGLProcAddressTable() {
    if (glProcAddressTable == null) {
      // FIXME: cache ProcAddressTables by capability bits so we can
      // share them among contexts with the same capabilities
      glProcAddressTable = new GLProcAddressTable();
    }          
    return glProcAddressTable;
  }
  
  public String getPlatformExtensionsString() {
    if (!wglGetExtensionsStringEXTInitialized) {
      wglGetExtensionsStringEXTAvailable = (WGL.wglGetProcAddress("wglGetExtensionsStringEXT") != 0);
      wglGetExtensionsStringEXTInitialized = true;
    }
    if (wglGetExtensionsStringEXTAvailable) {
      return gl.wglGetExtensionsStringEXT();
    } else {
      return "";
    }
  }

  protected boolean isFunctionAvailable(String glFunctionName)
  {
    boolean available = super.isFunctionAvailable(glFunctionName);
    
    // Sanity check for implementations that use proc addresses for run-time
    // linking: if the function IS available, then make sure there's a proc
    // address for it if it's an extension or not part of the OpenGL 1.1 core
    // (post GL 1.1 functions are run-time linked on windows).
    assert(!available ||
           (getGLProcAddressTable().getAddressFor(mapToRealGLFunctionName(glFunctionName)) != 0 ||
            FunctionAvailabilityCache.isPartOfGLCore("1.1", mapToRealGLFunctionName(glFunctionName)))
           );

    return available;
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

  public boolean canCreatePbufferContext() {
    return false;
  }

  public GLDrawableImpl createPbufferDrawable(GLCapabilities capabilities,
                                              int initialWidth,
                                              int initialHeight) {
    throw new GLException("Not supported");
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

  protected long getHGLRC() {
    return hglrc;
  }

  protected boolean haveWGLARBPbuffer() {
    return haveWGLARBPbuffer;
  }
}

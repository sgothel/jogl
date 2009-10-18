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
  protected long hglrc;
  private boolean wglGetExtensionsStringEXTInitialized;
  private boolean wglGetExtensionsStringEXTAvailable;
  private boolean wglMakeContextCurrentInitialized;
  private boolean wglMakeContextCurrentARBAvailable;
  private boolean wglMakeContextCurrentEXTAvailable;
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
  public WindowsWGLContext(GLDrawableImpl drawable, GLDrawableImpl drawableRead,
                           GLContext shareWith) {
    super(drawable, drawableRead, shareWith);
  }

  public WindowsWGLContext(GLDrawableImpl drawable,
                           GLContext shareWith) {
    this(drawable, null, shareWith);
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

  public boolean wglMakeContextCurrent(long hDrawDC, long hReadDC, long hglrc) {
    WGLExt wglExt = getWGLExt();
    if (!wglMakeContextCurrentInitialized) {
      wglMakeContextCurrentARBAvailable = isFunctionAvailable("wglMakeContextCurrentARB");
      wglMakeContextCurrentEXTAvailable = isFunctionAvailable("wglMakeContextCurrentEXT");
      wglMakeContextCurrentInitialized = true;
      if(DEBUG) {
          System.err.println("WindowsWGLContext.wglMakeContextCurrent: ARB "+wglMakeContextCurrentARBAvailable+", EXT "+wglMakeContextCurrentEXTAvailable);
      }
    }
    if(wglMakeContextCurrentARBAvailable) {
        return wglExt.wglMakeContextCurrentARB(hDrawDC, hReadDC, hglrc);
    } else if(wglMakeContextCurrentEXTAvailable) {
        return wglExt.wglMakeContextCurrentEXT(hDrawDC, hReadDC, hglrc);
    }
    return WGL.wglMakeCurrent(hDrawDC, hglrc);
  }

  public final ProcAddressTable getPlatformExtProcAddressTable() {
    return getWGLExtProcAddressTable();
  }

  public final WGLExtProcAddressTable getWGLExtProcAddressTable() {
    return wglExtProcAddressTable;
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
            throw new GLException("Error making temp context current: 0x" + Integer.toHexString(WGL.GetLastError()));
        }
        setGLFunctionAvailability(true);

        if( !isFunctionAvailable("wglCreateContextAttribsARB") ||
            !isExtensionAvailable("WGL_ARB_create_context") ) {
            if(glCaps.getGLProfile().isGL3()) {
              WGL.wglMakeCurrent(0, 0);
              WGL.wglDeleteContext(temp_hglrc);
              throw new GLException("Unable to create OpenGL >= 3.1 context (no WGL_ARB_create_context)");
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
                /*  0 */ WGLExt.WGL_CONTEXT_MAJOR_VERSION_ARB, 3,
                /*  2 */ WGLExt.WGL_CONTEXT_MINOR_VERSION_ARB, 0,
                /*  4 */ WGLExt.WGL_CONTEXT_FLAGS_ARB,         0 /* WGLExt.WGL_CONTEXT_DEBUG_BIT_ARB */,
                /*  6 */ 0,                                    0,
                /*  8 */ 0
            };

            if(glCaps.getGLProfile().isGL3()) {
                // Try >= 3.2 core first !
                // In contrast to GLX no verify with a None drawable binding (default framebuffer) is necessary,
                // if no 3.2 is available creation fails already!
                attribs[0+1]  = 3;
                attribs[2+1]  = 2;
                if(glCaps.getGLProfile().isGL3bc()) {
                    attribs[6+0]  = WGLExt.WGL_CONTEXT_PROFILE_MASK_ARB;
                    attribs[6+1]  = WGLExt.WGL_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB;
                }
                /**
                 * don't stricten requirements any further, even compatible would be fine
                 *
                 } else {
                    attribs[6+0]  = WGLExt.WGL_CONTEXT_PROFILE_MASK_ARB;
                    attribs[6+1]  = WGLExt.WGL_CONTEXT_CORE_PROFILE_BIT_ARB;
                 } 
                 */
                hglrc = wglExt.wglCreateContextAttribsARB(drawable.getNativeWindow().getSurfaceHandle(), hglrc2, attribs, 0); 
                if(0==hglrc) {
                    if(DEBUG) {
                        System.err.println("WindowsWGLContext.createContext couldn't create >= 3.2 core context - fallback");
                    }
                    // Try >= 3.1 forward compatible - last resort for GL3 !
                    attribs[0+1]  = 3;
                    attribs[2+1]  = 1;
                    if(!glCaps.getGLProfile().isGL3bc()) {
                        attribs[4+1] |= WGLExt.WGL_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB;
                    }
                    attribs[6+0]  = 0;
                    attribs[6+1]  = 0;
                } else if(DEBUG) {
                  System.err.println("WindowsWGLContext.createContext >= 3.2 available 0x"+Long.toHexString(hglrc));
                }
            }
            if(0==hglrc) {
                // 3.1 or 3.0 ..
                hglrc = wglExt.wglCreateContextAttribsARB(drawable.getNativeWindow().getSurfaceHandle(), hglrc2, attribs, 0); 
                if(DEBUG) {
                  if(0==hglrc) {
                      System.err.println("WindowsWGLContext.createContext couldn't create >= 3.0 context - fallback");
                  } else {
                      System.err.println("WindowsWGLContext.createContext >= 3.0 available 0x"+Long.toHexString(hglrc));
                  }
                }
            }

            if(0==hglrc) {
                if(glCaps.getGLProfile().isGL3()) {
                  WGL.wglMakeCurrent(0, 0);
                  WGL.wglDeleteContext(temp_hglrc);
                  throw new GLException("Unable to create OpenGL >= 3.1 context (have WGL_ARB_create_context)");
                }

                // continue with temp context for GL < 3.0
                hglrc = temp_hglrc;
                if (!WGL.wglMakeCurrent(drawable.getNativeWindow().getSurfaceHandle(), hglrc)) {
                    throw new GLException("Error making old context current: 0x" + Integer.toHexString(WGL.GetLastError()));
                }
                updateGLProcAddressTable();
                if(DEBUG) {
                  System.err.println("WindowsWGLContext.create done (old ctx < 3.0 - no 3.0) 0x"+Long.toHexString(hglrc));
                }
            } else {
                hglrc2 = 0; // mark as shared ..
                WGL.wglMakeCurrent(0, 0);
                WGL.wglDeleteContext(temp_hglrc);

                if (!WGL.wglMakeCurrent(drawable.getNativeWindow().getSurfaceHandle(), hglrc)) {
                    throw new GLException("Error making new context current: 0x" + Integer.toHexString(WGL.GetLastError()));
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
                                  ", " + toHexString(hglrc) + ") failed: error code 0x" +
                                  Integer.toHexString(WGL.GetLastError()));
        }
    }
    GLContextShareSet.contextCreated(this);
    WGL.wglMakeCurrent(0, 0); // release immediatly to gain from ARB/EXT wglMakeContextCurrent(draw, read, ctx)!
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
      if (!wglMakeContextCurrent(drawable.getNativeWindow().getSurfaceHandle(), drawableRead.getNativeWindow().getSurfaceHandle(), hglrc)) {
        throw new GLException("Error making context current: 0x" + Integer.toHexString(WGL.GetLastError()));
      } else {
        if (DEBUG && VERBOSE) {
          System.err.println(getThreadName() + ": wglMakeCurrent(hdc " + toHexString(drawable.getNativeWindow().getSurfaceHandle()) +
                             ", hglrc " + toHexString(hglrc) + ") succeeded");
        }
      }
    }

    if (created) {
      setGLFunctionAvailability(false);

      WindowsWGLGraphicsConfiguration config = 
        (WindowsWGLGraphicsConfiguration)drawable.getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration();
      config.updateCapabilitiesByWGL(this);

      return CONTEXT_CURRENT_NEW;
    }
    return CONTEXT_CURRENT;
  }

  protected void releaseImpl() throws GLException {
    if (!wglMakeContextCurrent(0, 0, 0)) {
        throw new GLException("Error freeing OpenGL context: 0x" + Integer.toHexString(WGL.GetLastError()));
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
    if (DEBUG) {
      System.err.println(getThreadName() + ": !!! Initializing WGL extension address table for " + this);
    }
    wglGetExtensionsStringEXTInitialized=false;
    wglGetExtensionsStringEXTAvailable=false;
    wglMakeContextCurrentInitialized=false;
    wglMakeContextCurrentARBAvailable=false;
    wglMakeContextCurrentEXTAvailable=false;

    if (wglExtProcAddressTable == null) {
      // FIXME: cache ProcAddressTables by capability bits so we can
      // share them among contexts with the same capabilities
      wglExtProcAddressTable = new WGLExtProcAddressTable();
    }          
    resetProcAddressTable(getWGLExtProcAddressTable());
    super.updateGLProcAddressTable();
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

  protected void setSwapIntervalImpl(int interval) {
    WGLExt wglExt = getWGLExt();
    if (wglExt.isExtensionAvailable("WGL_EXT_swap_control")) {
      if ( wglExt.wglSwapIntervalEXT(interval) ) {
        currentSwapInterval = interval ;
      }
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

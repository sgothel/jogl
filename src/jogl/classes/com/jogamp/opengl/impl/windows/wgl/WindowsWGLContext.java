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

package com.jogamp.opengl.impl.windows.wgl;

import java.nio.*;
import java.util.*;
import javax.media.opengl.*;
import javax.media.nativewindow.*;
import com.jogamp.opengl.impl.*;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;

public class WindowsWGLContext extends GLContextImpl {
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

  public boolean wglMakeContextCurrent(long hDrawDC, long hReadDC, long ctx) {
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
        return wglExt.wglMakeContextCurrentARB(hDrawDC, hReadDC, ctx);
    } else if(wglMakeContextCurrentEXTAvailable) {
        return wglExt.wglMakeContextCurrentEXT(hDrawDC, hReadDC, ctx);
    }
    return WGL.wglMakeCurrent(hDrawDC, ctx);
  }

  public final ProcAddressTable getPlatformExtProcAddressTable() {
    return getWGLExtProcAddressTable();
  }

  public final WGLExtProcAddressTable getWGLExtProcAddressTable() {
    return wglExtProcAddressTable;
  }

  protected Map/*<String, String>*/ getFunctionNameMap() { return functionNameMap; }

  protected Map/*<String, String>*/ getExtensionNameMap() { return extensionNameMap; }

  protected void destroyContextARBImpl(long context) {
    WGL.wglMakeCurrent(0, 0);
    WGL.wglDeleteContext(context);
  }

  protected long createContextARBImpl(long share, boolean direct, int ctp, int major, int minor) {
    WindowsWGLDrawableFactory factory = (WindowsWGLDrawableFactory)drawable.getFactoryImpl();
    WGLExt wglExt;
    if(null==factory.getSharedContext()) {
        wglExt = getWGLExt();
    } else {
        wglExt = ((WindowsWGLContext)factory.getSharedContext()).getWGLExt();
    }

    boolean ctBwdCompat = 0 != ( CTX_PROFILE_COMPAT & ctp ) ;
    boolean ctFwdCompat = 0 != ( CTX_OPTION_FORWARD & ctp ) ;
    boolean ctDebug     = 0 != ( CTX_OPTION_DEBUG & ctp ) ;

    long ctx=0;

    final int idx_flags = 4;
    final int idx_profile = 6;

    /*  WGLExt.WGL_CONTEXT_LAYER_PLANE_ARB,   WGLExt.WGL_CONTEXT_LAYER_PLANE_ARB, */

    int attribs[] = {
        /*  0 */ WGLExt.WGL_CONTEXT_MAJOR_VERSION_ARB, major,
        /*  2 */ WGLExt.WGL_CONTEXT_MINOR_VERSION_ARB, minor,
        /*  4 */ WGLExt.WGL_CONTEXT_FLAGS_ARB,         0,
        /*  6 */ 0,                                    0,
        /*  8 */ 0
    };

    if ( major > 3 || major == 3 && minor >= 2  ) {
        // FIXME: Verify with a None drawable binding (default framebuffer)
        attribs[idx_profile+0]  = WGLExt.WGL_CONTEXT_PROFILE_MASK_ARB;
        if( ctBwdCompat ) {
            attribs[idx_profile+1]  = WGLExt.WGL_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB;
        } else {
            attribs[idx_profile+1]  = WGLExt.WGL_CONTEXT_CORE_PROFILE_BIT_ARB;
        } 
    } 

    if ( major >= 3 ) {
        if( !ctBwdCompat && ctFwdCompat ) {
            attribs[idx_flags+1] |= WGLExt.WGL_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB;
        }
        if( ctDebug) {
            attribs[idx_flags+1] |= WGLExt.WGL_CONTEXT_DEBUG_BIT_ARB;
        }
    }

    ctx = wglExt.wglCreateContextAttribsARB(drawable.getNativeWindow().getSurfaceHandle(), share, attribs, 0); 
    if(DEBUG) {
      System.err.println("WindowsWGLContext.createContextARB success: "+(0!=ctx)+" - "+getGLVersion(major, minor, ctp, "@creation")+", bwdCompat "+ctBwdCompat+", fwdCompat "+ctFwdCompat);
    }
    if(0!=ctx) {
        // In contrast to GLX no verification with a drawable binding, ie default framebuffer, is necessary,
        // if no 3.2 is available creation fails already!
        // Nevertheless .. we do it ..
        if (!WGL.wglMakeCurrent(drawable.getNativeWindow().getSurfaceHandle(), ctx)) {
            if(DEBUG) {
              System.err.println("WindowsWGLContext.createContextARB couldn't make current "+getGLVersion(major, minor, ctp, "@creation"));
            }
            WGL.wglMakeCurrent(0, 0);
            WGL.wglDeleteContext(ctx);
            ctx = 0;
        }
    }
    return ctx;
  }

  /**
   * Creates and initializes an appropriate OpenGL context. Should only be
   * called by {@link #makeCurrentImpl()}.
   */
  protected void create() {
    if(0!=contextHandle) {
        throw new GLException("context is not null: "+contextHandle);
    }
    WindowsWGLDrawableFactory factory = (WindowsWGLDrawableFactory)drawable.getFactoryImpl();
    GLCapabilities glCaps = drawable.getChosenGLCapabilities();

    // Windows can set up sharing of display lists after creation time
    WindowsWGLContext other = (WindowsWGLContext) GLContextShareSet.getShareContext(this);
    long share = 0;
    if (other != null) {
      share = other.getHandle();
      if (share == 0) {
        throw new GLException("GLContextShareSet returned an invalid OpenGL context");
      }
    }

    int minor[] = new int[1];
    int major[] = new int[1];
    int ctp[] = new int[1];
    boolean createContextARBTried = false;

    // utilize the shared context's GLXExt in case it was using the ARB method and it already exists
    if( null!=factory.getSharedContext() && factory.getSharedContext().isCreatedWithARBMethod() ) {
        if(DEBUG) {
          System.err.println("WindowsWGLContext.createContext using shared Context: "+factory.getSharedContext());
        }
        contextHandle = createContextARB(share, true, major, minor, ctp);
        createContextARBTried = true;
    }

    long temp_ctx = 0;
    if(0==contextHandle) {
        // To use WGL_ARB_create_context, we have to make a temp context current,
        // so we are able to use GetProcAddress
        temp_ctx = WGL.wglCreateContext(drawable.getNativeWindow().getSurfaceHandle());
        if (temp_ctx == 0) {
          throw new GLException("Unable to create temp OpenGL context for device context " + toHexString(drawable.getNativeWindow().getSurfaceHandle()));
        }
        if (!WGL.wglMakeCurrent(drawable.getNativeWindow().getSurfaceHandle(), temp_ctx)) {
            throw new GLException("Error making temp context current: 0x" + toHexString(temp_ctx) + ", werr: 0x"+Integer.toHexString(WGL.GetLastError()));
        }
        setGLFunctionAvailability(true, 0, 0, CTX_PROFILE_COMPAT|CTX_OPTION_ANY);

        if( createContextARBTried ||
            !isFunctionAvailable("wglCreateContextAttribsARB") /* ||
            !isExtensionAvailable("WGL_ARB_create_context") */ ) { // unresolved case where client version is 1.4 without this extension
            if(glCaps.getGLProfile().isGL3()) {
              WGL.wglMakeCurrent(0, 0);
              WGL.wglDeleteContext(temp_ctx);
              throw new GLException("Unable to create OpenGL >= 3.1 context (no WGL_ARB_create_context)");
            }

            // continue with temp context for GL < 3.0
            contextHandle = temp_ctx;
            return;
        } 
        contextHandle = createContextARB(share, true, major, minor, ctp);
        createContextARBTried=true;
    }
    
    if(0!=contextHandle) {
        share = 0; // mark as shared ..

        WGL.wglMakeCurrent(0, 0);
        WGL.wglDeleteContext(temp_ctx);

        if (!wglMakeContextCurrent(drawable.getNativeWindow().getSurfaceHandle(), drawableRead.getNativeWindow().getSurfaceHandle(), contextHandle)) {
            throw new GLException("Cannot make previous verified context current: 0x" + toHexString(contextHandle) + ", werr: 0x" + Integer.toHexString(WGL.GetLastError()));
        }
    } else {
        if(glCaps.getGLProfile().isGL3()) {
          WGL.wglMakeCurrent(0, 0);
          WGL.wglDeleteContext(temp_ctx);
          throw new GLException("WindowsWGLContext.createContext failed, but context > GL2 requested "+getGLVersion(major[0], minor[0], ctp[0], "@creation")+", ");
        }
        if(DEBUG) {
          System.err.println("WindowsWGLContext.createContext failed, fall back to !ARB context "+getGLVersion(major[0], minor[0], ctp[0], "@creation"));
        }

        // continue with temp context for GL < 3.0
        contextHandle = temp_ctx;
        if (!wglMakeContextCurrent(drawable.getNativeWindow().getSurfaceHandle(), drawableRead.getNativeWindow().getSurfaceHandle(), contextHandle)) {
            WGL.wglMakeCurrent(0, 0);
            WGL.wglDeleteContext(contextHandle);
            throw new GLException("Error making old context current: 0x" + toHexString(contextHandle) + ", werr: 0x" + Integer.toHexString(WGL.GetLastError()));
        }
    }

    if(0!=share) {
        if (!WGL.wglShareLists(share, contextHandle)) {
            throw new GLException("wglShareLists(" + toHexString(share) +
                                  ", " + toHexString(contextHandle) + ") failed: werr 0x" +
                                  Integer.toHexString(WGL.GetLastError()));
        }
    }
    GLContextShareSet.contextCreated(this);
  }
  
  protected int makeCurrentImpl() throws GLException {
    if (0 == drawable.getNativeWindow().getSurfaceHandle()) {
        throw new GLException("drawable has invalid surface handle: "+drawable);
    }
    boolean newCreated = false;
    if (!isCreated()) {
      create(); // throws exception if fails!
      newCreated = true;
      if (DEBUG) {
        System.err.println(getThreadName() + ": !!! Created GL context " + toHexString(contextHandle) + " for " + getClass().getName());
      }
    }

    if (WGL.wglGetCurrentContext() != contextHandle) {
      if (!wglMakeContextCurrent(drawable.getNativeWindow().getSurfaceHandle(), drawableRead.getNativeWindow().getSurfaceHandle(), contextHandle)) {
        throw new GLException("Error making context current: 0x" + toHexString(contextHandle) + ", werr: 0x" + Integer.toHexString(WGL.GetLastError()) + ", " + this);
      } else {
        if (DEBUG && VERBOSE) {
          System.err.println(getThreadName() + ": wglMakeCurrent(hdc " + toHexString(drawable.getNativeWindow().getSurfaceHandle()) +
                             ", contextHandle " + toHexString(contextHandle) + ") succeeded");
        }
      }
    }

    if (newCreated) {
      setGLFunctionAvailability(false, -1, -1, CTX_PROFILE_COMPAT|CTX_OPTION_ANY);

      WindowsWGLGraphicsConfiguration config = 
        (WindowsWGLGraphicsConfiguration)drawable.getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration();
      config.updateCapabilitiesByWGL(this);

      return CONTEXT_CURRENT_NEW;
    }
    return CONTEXT_CURRENT;
  }

  protected void releaseImpl() throws GLException {
    if (!wglMakeContextCurrent(0, 0, 0)) {
        throw new GLException("Error freeing OpenGL context, werr: 0x" + Integer.toHexString(WGL.GetLastError()));
    }
  }

  protected void destroyImpl() throws GLException {
    if (DEBUG) {
        Exception e = new Exception(getThreadName() + ": !!! Destroyed OpenGL context " + toHexString(contextHandle));
        e.printStackTrace();
    }
    if (contextHandle != 0) {
      if (!WGL.wglDeleteContext(contextHandle)) {
        throw new GLException("Unable to delete OpenGL context");
      }
      contextHandle = 0;
      GLContextShareSet.contextDestroyed(this);
    }
  }

  public void copy(GLContext source, int mask) throws GLException {
    long dst = getHandle();
    long src = source.getHandle();
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

  protected void updateGLProcAddressTable(int major, int minor, int ctp) {
    if (DEBUG) {
      System.err.println(getThreadName() + ": !!! Initializing WGL extension address table for " + this);
    }
    wglGetExtensionsStringEXTInitialized=false;
    wglGetExtensionsStringEXTAvailable=false;
    wglMakeContextCurrentInitialized=false;
    wglMakeContextCurrentARBAvailable=false;
    wglMakeContextCurrentEXTAvailable=false;

    if (wglExtProcAddressTable == null) {
      // FIXME: cache ProcAddressTables by OpenGL context type bits so we can
      // share them among contexts classes (GL4, GL4bc, GL3, GL3bc, ..)
      wglExtProcAddressTable = new WGLExtProcAddressTable(new GLProcAddressResolver());
    }          
    resetProcAddressTable(getWGLExtProcAddressTable());
    super.updateGLProcAddressTable(major, minor, ctp);
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

}

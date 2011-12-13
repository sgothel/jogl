/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package jogamp.opengl.windows.wgl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.GLCapabilitiesImmutable;

import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;
import jogamp.nativewindow.windows.GDI;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableImpl;

public class WindowsWGLContext extends GLContextImpl {
  static final Map<String, String> functionNameMap;
  static final Map<String, String> extensionNameMap;
  private boolean wglGetExtensionsStringEXTInitialized;
  private boolean wglGetExtensionsStringEXTAvailable;
  private boolean wglGLReadDrawableAvailableSet;
  private boolean wglGLReadDrawableAvailable;
  private WGLExt _wglExt;
  // Table that holds the addresses of the native C-language entry points for
  // WGL extension functions.
  private WGLExtProcAddressTable wglExtProcAddressTable;
  private int hasSwapIntervalSGI = 0;
  private int hasSwapGroupNV = 0;

  static {
    functionNameMap = new HashMap<String, String>();
    functionNameMap.put("glAllocateMemoryNV", "wglAllocateMemoryNV");
    functionNameMap.put("glFreeMemoryNV", "wglFreeMemoryNV");

    extensionNameMap = new HashMap<String, String>();
    extensionNameMap.put("GL_ARB_pbuffer", "WGL_ARB_pbuffer");
    extensionNameMap.put("GL_ARB_pixel_format", "WGL_ARB_pixel_format");
  }

  // FIXME: figure out how to hook back in the Java 2D / JOGL bridge
  WindowsWGLContext(GLDrawableImpl drawable,
                    GLContext shareWith) {
    super(drawable, shareWith);
  }

  @Override
  protected void resetStates() {
    wglGetExtensionsStringEXTInitialized=false;
    wglGetExtensionsStringEXTAvailable=false;
    wglGLReadDrawableAvailableSet=false;
    wglGLReadDrawableAvailable=false;
    // no inner state _wglExt=null;
    wglExtProcAddressTable=null;
    hasSwapIntervalSGI = 0;
    hasSwapGroupNV = 0;    
    super.resetStates();    
  }
  
  public Object getPlatformGLExtensions() {
    return getWGLExt();
  }

  /* package private */ final WGLExt getWGLExt() {
    if( null == getWGLExtProcAddressTable()) {
        throw new InternalError("Null WGLExtProcAddressTable");
    }
    if (_wglExt == null) {
      _wglExt = new WGLExtImpl(this);
    }
    return _wglExt;
  }

  public final boolean isGLReadDrawableAvailable() {
    if(!wglGLReadDrawableAvailableSet && null != getWGLExtProcAddressTable()) {
        WindowsWGLDrawableFactory factory = (WindowsWGLDrawableFactory)drawable.getFactoryImpl();
        AbstractGraphicsConfiguration config = drawable.getNativeSurface().getGraphicsConfiguration();
        AbstractGraphicsDevice device = config.getScreen().getDevice();
        switch( factory.isReadDrawableAvailable(device) ) {
            case  1:
                wglGLReadDrawableAvailable = true;
                wglGLReadDrawableAvailableSet=true;
                break;
            case  0:
                wglGLReadDrawableAvailable = false;
                wglGLReadDrawableAvailableSet=true;
                break;
        }
    }
    return wglGLReadDrawableAvailable;
  }

  private final boolean wglMakeContextCurrent(long hDrawDC, long hReadDC, long ctx) {
    boolean ok = false;
    if(wglGLReadDrawableAvailable) {
        // needs initilized WGL ProcAddress table
        ok = getWGLExt().wglMakeContextCurrent(hDrawDC, hReadDC, ctx);
    } else if ( hDrawDC == hReadDC ) {
        ok = WGL.wglMakeCurrent(hDrawDC, ctx);
    } else {
        // should not happen due to 'isGLReadDrawableAvailable()' query in GLContextImpl
        throw new InternalError("Given readDrawable but no driver support");
    }
    int werr = ( !ok ) ? GDI.GetLastError() : GDI.ERROR_SUCCESS;
    if(DEBUG && !ok) {
        Throwable t = new Throwable ("Info: wglMakeContextCurrent draw "+
                GLContext.toHexString(hDrawDC) + ", read " + GLContext.toHexString(hReadDC) +
                ", ctx " + GLContext.toHexString(ctx) + ", werr " + werr);
        t.printStackTrace();
    }
    if(!ok && 0==hDrawDC && 0==hReadDC) {
        // Some GPU's falsely fails with a zero error code (success),
        // in case this is a release context request we tolerate this
        return werr == GDI.ERROR_SUCCESS ;
    }
    return ok;
  }

  public final ProcAddressTable getPlatformExtProcAddressTable() {
    return getWGLExtProcAddressTable();
  }

  public final WGLExtProcAddressTable getWGLExtProcAddressTable() {
    return wglExtProcAddressTable;
  }

  protected Map<String, String> getFunctionNameMap() { return functionNameMap; }

  protected Map<String, String> getExtensionNameMap() { return extensionNameMap; }

  protected void destroyContextARBImpl(long context) {
    WGL.wglMakeCurrent(0, 0);
    WGL.wglDeleteContext(context);
  }

  protected long createContextARBImpl(long share, boolean direct, int ctp, int major, int minor) {
    if( null == getWGLExtProcAddressTable()) {
        updateGLXProcAddressTable();
    }
    WGLExt _wglExt = getWGLExt();
    if(DEBUG) {
      System.err.println(getThreadName()+" - WindowWGLContext.createContextARBImpl: "+getGLVersion(major, minor, ctp, "@creation") +
                         ", handle "+toHexString(drawable.getHandle()) + ", share "+toHexString(share)+", direct "+direct+
                         ", wglCreateContextAttribsARB: "+toHexString(wglExtProcAddressTable._addressof_wglCreateContextAttribsARB));
      Thread.dumpStack();
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

    try {
        ctx = _wglExt.wglCreateContextAttribsARB(drawable.getHandle(), share, attribs, 0);
    } catch (RuntimeException re) {
        if(DEBUG) {
          Throwable t = new Throwable("Info: WindowWGLContext.createContextARBImpl wglCreateContextAttribsARB failed with "+getGLVersion(major, minor, ctp, "@creation"), re);
          t.printStackTrace();
        }
    }

    if(0!=ctx) {
        if (!wglMakeContextCurrent(drawable.getHandle(), drawableRead.getHandle(), ctx)) {
            if(DEBUG) {
              System.err.println("WindowsWGLContext.createContextARB couldn't make current "+getGLVersion(major, minor, ctp, "@creation"));
            }
            WGL.wglMakeCurrent(0, 0);
            WGL.wglDeleteContext(ctx);
            ctx = 0;
        } else if (DEBUG) {
            System.err.println(getThreadName() + ": createContextARBImpl: OK "+getGLVersion(major, minor, ctp, "@creation")+", share "+share+", direct "+direct);
        }
    } else if (DEBUG) {
        System.err.println(getThreadName() + ": createContextARBImpl: NO "+getGLVersion(major, minor, ctp, "@creation"));
    }
    return ctx;
  }

  /**
   * Creates and initializes an appropriate OpenGL context. Should only be
   * called by {@link #makeCurrentImpl()}.
   */
  protected boolean createImpl(GLContextImpl shareWith) {
    AbstractGraphicsConfiguration config = drawable.getNativeSurface().getGraphicsConfiguration();
    AbstractGraphicsDevice device = config.getScreen().getDevice();
    WindowsWGLDrawableFactory factory = (WindowsWGLDrawableFactory)drawable.getFactoryImpl();
    WindowsWGLContext sharedContext = (WindowsWGLContext) factory.getOrCreateSharedContextImpl(device);
    GLCapabilitiesImmutable glCaps = drawable.getChosenGLCapabilities();

    isGLReadDrawableAvailable(); // trigger setup wglGLReadDrawableAvailable

    // Windows can set up sharing of display lists after creation time
    long share = 0;
    if (null != shareWith) {
      share = shareWith.getHandle();
      if (share == 0) {
        throw new GLException("GLContextShareSet returned an invalid OpenGL context");
      }
    }

    boolean createContextARBTried = false;

    // utilize the shared context's GLXExt in case it was using the ARB method and it already exists
    if( null!=sharedContext && sharedContext.isCreatedWithARBMethod() ) {
        contextHandle = createContextARB(share, true);
        createContextARBTried = true;
        if (DEBUG && 0!=contextHandle) {
            System.err.println(getThreadName() + ": createImpl: OK (ARB, using sharedContext) share "+share);
        }
    }

    long temp_ctx = 0;
    if(0==contextHandle) {
        // To use WGL_ARB_create_context, we have to make a temp context current,
        // so we are able to use GetProcAddress
        temp_ctx = WGL.wglCreateContext(drawable.getHandle());
        if (temp_ctx == 0) {
          throw new GLException("Unable to create temp OpenGL context for device context " + toHexString(drawable.getHandle()));
        }
        if (!WGL.wglMakeCurrent(drawable.getHandle(), temp_ctx)) {
            throw new GLException("Error making temp context current: 0x" + toHexString(temp_ctx) + ", werr: "+GDI.GetLastError());
        }
        setGLFunctionAvailability(true, 0, 0, CTX_PROFILE_COMPAT|CTX_OPTION_ANY);  // use GL_VERSION
        boolean isCreateContextAttribsARBAvailable = isFunctionAvailable("wglCreateContextAttribsARB");
        WGL.wglMakeCurrent(0, 0); // release temp context

        if( !createContextARBTried) {
            if(isCreateContextAttribsARBAvailable &&
               isExtensionAvailable("WGL_ARB_create_context") ) {
                // initial ARB context creation
                contextHandle = createContextARB(share, true);
                createContextARBTried=true;
                if (DEBUG) {
                    if(0!=contextHandle) {
                        System.err.println(getThreadName() + ": createContextImpl: OK (ARB, initial) share "+share);
                    } else {
                        System.err.println(getThreadName() + ": createContextImpl: NOT OK (ARB, initial) - creation failed - share "+share);
                    }
                }
            } else if (DEBUG) {
                System.err.println(getThreadName() + ": createContextImpl: NOT OK (ARB, initial) - extension not available - share "+share);
            }
        }
    }
    
    if(0!=contextHandle) {
        share = 0; // mark as shared thx to the ARB create method
        if(0!=temp_ctx) {
            WGL.wglMakeCurrent(0, 0);
            WGL.wglDeleteContext(temp_ctx);
            if (!wglMakeContextCurrent(drawable.getHandle(), drawableRead.getHandle(), contextHandle)) {
                throw new GLException("Cannot make previous verified context current: 0x" + toHexString(contextHandle) + ", werr: " + GDI.GetLastError());
            }
        }
    } else {
        if(glCaps.getGLProfile().isGL3()) {
          WGL.wglMakeCurrent(0, 0);
          WGL.wglDeleteContext(temp_ctx);
          throw new GLException("WindowsWGLContext.createContext ctx !ARB, context > GL2 requested "+getGLVersion());
        }
        if(DEBUG) {
          System.err.println("WindowsWGLContext.createContext failed, fall back to !ARB context "+getGLVersion());
        }

        // continue with temp context for GL < 3.0
        contextHandle = temp_ctx;
        if (!wglMakeContextCurrent(drawable.getHandle(), drawableRead.getHandle(), contextHandle)) {
            WGL.wglMakeCurrent(0, 0);
            WGL.wglDeleteContext(contextHandle);
            throw new GLException("Error making old context current: 0x" + toHexString(contextHandle) + ", werr: " + GDI.GetLastError());
        }
        if(0!=share) {
            // Only utilize the classic GDI 'wglShareLists' shared context method 
            // for traditional non ARB context.
            if (!WGL.wglShareLists(share, contextHandle)) {
                throw new GLException("wglShareLists(" + toHexString(share) +
                                      ", " + toHexString(contextHandle) + ") failed: werr " + GDI.GetLastError());
            }
        }
        if (DEBUG) {
            System.err.println(getThreadName() + ": createImpl: OK (old) share "+share);
        }
    }

    return true;
  }
  
  protected void  makeCurrentImpl() throws GLException {
    if (WGL.wglGetCurrentContext() != contextHandle) {
      if (!wglMakeContextCurrent(drawable.getHandle(), drawableRead.getHandle(), contextHandle)) {
        throw new GLException("Error making context current: 0x" + toHexString(contextHandle) + ", werr: " + GDI.GetLastError() + ", " + this);
      }
    }
  }

  protected void releaseImpl() throws GLException {
    if (!wglMakeContextCurrent(0, 0, 0)) {
        throw new GLException("Error freeing OpenGL context, werr: " + GDI.GetLastError());
    }
  }

  protected void destroyImpl() throws GLException {
      WGL.wglMakeCurrent(0, 0);
      if (!WGL.wglDeleteContext(contextHandle)) {
        throw new GLException("Unable to delete OpenGL context");
      }
  }

  protected void copyImpl(GLContext source, int mask) throws GLException {
    if (!WGL.wglCopyContext(source.getHandle(), getHandle(), mask)) {
      throw new GLException("wglCopyContext failed");
    }
  }

  protected final void updateGLXProcAddressTable() {
    final AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration();
    final AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
    final String key = "WGL-"+adevice.getUniqueID();
    if (DEBUG) {
      System.err.println(getThreadName() + ": !!! Initializing WGL extension address table: "+key);
    }
    wglGetExtensionsStringEXTInitialized=false;
    wglGetExtensionsStringEXTAvailable=false;
    wglGLReadDrawableAvailableSet=false;
    wglGLReadDrawableAvailable=false;

    ProcAddressTable table = null;
    synchronized(mappedContextTypeObjectLock) {
        table = mappedGLXProcAddress.get( key );
    }
    if(null != table) {
        wglExtProcAddressTable = (WGLExtProcAddressTable) table;
        if(DEBUG) {
            System.err.println(getThreadName() + ": !!! GLContext WGL ProcAddressTable reusing key("+key+") -> "+table.hashCode());
        }
    } else {
        if (wglExtProcAddressTable == null) {
          // FIXME: cache ProcAddressTables by OpenGL context type bits so we can
          // share them among contexts classes (GL4, GL4bc, GL3, GL3bc, ..)
          wglExtProcAddressTable = new WGLExtProcAddressTable(new GLProcAddressResolver());
        }
        resetProcAddressTable(wglExtProcAddressTable);
        synchronized(mappedContextTypeObjectLock) {
            mappedGLXProcAddress.put(key, getWGLExtProcAddressTable());
            if(DEBUG) {
                System.err.println(getThreadName() + ": !!! GLContext WGL ProcAddressTable mapping key("+key+") -> "+getWGLExtProcAddressTable().hashCode());
            }
        }
    }
  }
  
  protected final StringBuffer getPlatformExtensionsStringImpl() {
    StringBuffer sb = new StringBuffer();
    
    if (!wglGetExtensionsStringEXTInitialized) {
      wglGetExtensionsStringEXTAvailable = (WGL.wglGetProcAddress("wglGetExtensionsStringEXT") != 0);
      wglGetExtensionsStringEXTInitialized = true;
    }
    if (wglGetExtensionsStringEXTAvailable) {
      sb.append(getWGLExt().wglGetExtensionsStringEXT());
    }
    return sb;
  }
  
  @Override
  protected void setSwapIntervalImpl(int interval) {
    WGLExt wglExt = getWGLExt();
    if(0==hasSwapIntervalSGI) {
        try {
            hasSwapIntervalSGI = wglExt.isExtensionAvailable("WGL_EXT_swap_control")?1:-1;
        } catch (Throwable t) { hasSwapIntervalSGI=1; }
    }
    if (hasSwapIntervalSGI>0) {
        try {
            if ( wglExt.wglSwapIntervalEXT(interval) ) {
                currentSwapInterval = interval ;
            }
        } catch (Throwable t) { hasSwapIntervalSGI=-1; }
    }
  }
  
  private final int initSwapGroupImpl(WGLExt wglExt) {
      if(0==hasSwapGroupNV) {
        try {
            hasSwapGroupNV = wglExt.isExtensionAvailable("WGL_NV_swap_group")?1:-1;
        } catch (Throwable t) { hasSwapGroupNV=1; }
        if(DEBUG) {
            System.err.println("initSwapGroupImpl: hasSwapGroupNV: "+hasSwapGroupNV);
        }
      }
      return hasSwapGroupNV;
  }
  
  @Override
  protected final boolean queryMaxSwapGroupsImpl(int[] maxGroups, int maxGroups_offset,
                                                 int[] maxBarriers, int maxBarriers_offset) {
      boolean res = false;
      WGLExt wglExt = getWGLExt();
      if (initSwapGroupImpl(wglExt)>0) {
        final NativeSurface ns = drawable.getNativeSurface();
        try {
            if( wglExt.wglQueryMaxSwapGroupsNV(ns.getDisplayHandle(), 
                                               maxGroups, maxGroups_offset,
                                               maxBarriers, maxBarriers_offset) ) {
                res = true;
            }
        } catch (Throwable t) { hasSwapGroupNV=-1; }
      }
      return res;
  }
  
  @Override
  protected final boolean joinSwapGroupImpl(int group) {
      boolean res = false;
      WGLExt wglExt = getWGLExt();
      if (initSwapGroupImpl(wglExt)>0) {
        try {
            if( wglExt.wglJoinSwapGroupNV(drawable.getHandle(), group) ) {
                currentSwapGroup = group;
                res = true;
            }
        } catch (Throwable t) { hasSwapGroupNV=-1; }
      }
      return res;
  }
  
  @Override
  protected final boolean bindSwapBarrierImpl(int group, int barrier) {
      boolean res = false;
      WGLExt wglExt = getWGLExt();
      if (initSwapGroupImpl(wglExt)>0) {
        try {
            if( wglExt.wglBindSwapBarrierNV(group, barrier) ) {
                res = true;
            }
        } catch (Throwable t) { hasSwapGroupNV=-1; }
      }
      return res;  
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

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

package jogamp.opengl.x11.glx;

import java.nio.*;
import java.util.*;

import javax.media.opengl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.X11GraphicsDevice;

import com.jogamp.common.util.VersionNumber;
import jogamp.opengl.*;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;
import jogamp.nativewindow.x11.X11Util;

public abstract class X11GLXContext extends GLContextImpl {
  protected static final boolean TRACE_CONTEXT_CURRENT = false; // true;

  private static final Map<String, String> functionNameMap;
  private static final Map<String, String> extensionNameMap;
  private VersionNumber glXVersion;
  private boolean glXVersionOneThreeCapable;
  private boolean glXQueryExtensionsStringInitialized;
  private boolean glXQueryExtensionsStringAvailable;
  private GLXExt glXExt;
  // Table that holds the addresses of the native C-language entry points for
  // GLX extension functions.
  private GLXExtProcAddressTable glXExtProcAddressTable;
  private int hasSwapIntervalSGI = 0;

  // This indicates whether the context we have created is indirect
  // and therefore requires the toolkit to be locked around all GL
  // calls rather than just all GLX calls
  protected boolean isDirect;

  static {
    functionNameMap = new HashMap<String, String>();
    functionNameMap.put("glAllocateMemoryNV", "glXAllocateMemoryNV");
    functionNameMap.put("glFreeMemoryNV", "glXFreeMemoryNV");

    extensionNameMap = new HashMap<String, String>();
    extensionNameMap.put("GL_ARB_pbuffer",      "GLX_SGIX_pbuffer");
    extensionNameMap.put("GL_ARB_pixel_format", "GLX_SGIX_pbuffer"); // good enough
  }

  X11GLXContext(GLDrawableImpl drawable,
                GLContext shareWith) {
    super(drawable, shareWith);
  }
  
  protected void resetState() {
    glXVersion = null;
    glXVersionOneThreeCapable = false;
    glXQueryExtensionsStringInitialized=false;
    glXQueryExtensionsStringAvailable=false;
    // no inner state glXExt=null;
    glXExtProcAddressTable = null;
    hasSwapIntervalSGI = 0;
    isDirect = false;
  }

  public final ProcAddressTable getPlatformExtProcAddressTable() {
    return getGLXExtProcAddressTable();
  }

  public final GLXExtProcAddressTable getGLXExtProcAddressTable() {
    return glXExtProcAddressTable;
  }

  public Object getPlatformGLExtensions() {
    return getGLXExt();
  }

  public GLXExt getGLXExt() {
    if (glXExt == null) {
      glXExt = new GLXExtImpl(this);
    }
    return glXExt;
  }

  protected Map<String, String> getFunctionNameMap() { return functionNameMap; }

  protected Map<String, String> getExtensionNameMap() { return extensionNameMap; }

  public final boolean isGLXVersionGreaterEqualOneThree() {
    if(null == glXVersion) {
        X11GLXDrawableFactory factory = (X11GLXDrawableFactory)drawable.getFactoryImpl();

        X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
        X11GraphicsDevice device = (X11GraphicsDevice) config.getScreen().getDevice();

        glXVersion = factory.getGLXVersion(device);
        glXVersionOneThreeCapable = ( null != glXVersion ) ? glXVersion.compareTo(X11GLXDrawableFactory.versionOneThree) >= 0 : false ;
    }
    return glXVersionOneThreeCapable;
  }

  public final boolean isGLReadDrawableAvailable() {
    return isGLXVersionGreaterEqualOneThree();
  }

  private final boolean glXMakeContextCurrent(long dpy, long writeDrawable, long readDrawable, long ctx) {
    boolean res = false;

    try {
        if(TRACE_CONTEXT_CURRENT) {
            Throwable t = new Throwable(Thread.currentThread()+" - glXMakeContextCurrent("+toHexString(dpy)+", "+
                    toHexString(writeDrawable)+", "+toHexString(readDrawable)+", "+toHexString(ctx)+") - GLX >= 1.3 "+ glXVersionOneThreeCapable);
            t.printStackTrace();
        }
        if ( glXVersionOneThreeCapable ) {
            res = GLX.glXMakeContextCurrent(dpy, writeDrawable, readDrawable, ctx);
        } else if ( writeDrawable == readDrawable ) {
            res = GLX.glXMakeCurrent(dpy, writeDrawable, ctx);
        } else {
            // should not happen due to 'isGLReadDrawableAvailable()' query in GLContextImpl
            throw new InternalError("Given readDrawable but no driver support");
        }
    } catch (RuntimeException re) {
        if(DEBUG) {
          System.err.println("Warning: X11GLXContext.glXMakeContextCurrent failed: "+re+", with "+
            "dpy "+toHexString(dpy)+
            ", write "+toHexString(writeDrawable)+
            ", read "+toHexString(readDrawable)+
            ", ctx "+toHexString(ctx));
          re.printStackTrace();
        }
    }
    return res;
  }

  protected void destroyContextARBImpl(long ctx) {
    X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    long display = config.getScreen().getDevice().getHandle();

    glXMakeContextCurrent(display, 0, 0, 0);
    GLX.glXDestroyContext(display, ctx);
  }

  protected long createContextARBImpl(long share, boolean direct, int ctp, int major, int minor) {
    updateGLXProcAddressTable();
    GLXExt _glXExt = getGLXExt();
    if(DEBUG) {
      System.err.println("X11GLXContext.createContextARBImpl: "+getGLVersion(major, minor, ctp, "@creation") +
                         ", handle "+toHexString(drawable.getHandle()) + ", share "+toHexString(share)+", direct "+direct+
                         ", glXCreateContextAttribsARB: "+toHexString(glXExtProcAddressTable._addressof_glXCreateContextAttribsARB));
      Thread.dumpStack();
    }

    boolean ctBwdCompat = 0 != ( CTX_PROFILE_COMPAT & ctp ) ;
    boolean ctFwdCompat = 0 != ( CTX_OPTION_FORWARD & ctp ) ;
    boolean ctDebug     = 0 != ( CTX_OPTION_DEBUG & ctp ) ;

    long ctx=0;

    final int idx_flags = 6;
    final int idx_profile = 8;

    int attribs[] = {
        /*  0 */ GLX.GLX_CONTEXT_MAJOR_VERSION_ARB, major,
        /*  2 */ GLX.GLX_CONTEXT_MINOR_VERSION_ARB, minor,
        /*  4 */ GLX.GLX_RENDER_TYPE,               GLX.GLX_RGBA_TYPE, // default
        /*  6 */ GLX.GLX_CONTEXT_FLAGS_ARB,         0,
        /*  8 */ 0,                                 0,
        /* 10 */ 0
    };

    if ( major > 3 || major == 3 && minor >= 2  ) {
        // FIXME: Verify with a None drawable binding (default framebuffer)
        attribs[idx_profile+0]  = GLX.GLX_CONTEXT_PROFILE_MASK_ARB;
        if( ctBwdCompat ) {
            attribs[idx_profile+1]  = GLX.GLX_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB;
        } else {
            attribs[idx_profile+1]  = GLX.GLX_CONTEXT_CORE_PROFILE_BIT_ARB;
        } 
    } 

    if ( major >= 3 ) {
        if( !ctBwdCompat && ctFwdCompat ) {
            attribs[idx_flags+1] |= GLX.GLX_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB;
        }
        if( ctDebug) {
            attribs[idx_flags+1] |= GLX.GLX_CONTEXT_DEBUG_BIT_ARB;
        }
    }

    X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    AbstractGraphicsDevice device = config.getScreen().getDevice();
    long display = device.getHandle();

    try {
        // critical path, a remote display might not support this command,
        // hence we need to catch the X11 Error within this block.
        X11Util.XSync(display, false);
        ctx = _glXExt.glXCreateContextAttribsARB(display, config.getFBConfig(), share, direct, attribs, 0);
        X11Util.XSync(display, false);
    } catch (RuntimeException re) {
        if(DEBUG) {
          Throwable t = new Throwable("Info: X11GLXContext.createContextARBImpl glXCreateContextAttribsARB failed with "+getGLVersion(major, minor, ctp, "@creation"), re);
          t.printStackTrace();
        }
    }
    if(0!=ctx) {
        if (!glXMakeContextCurrent(display, drawable.getHandle(), drawableRead.getHandle(), ctx)) {
            if(DEBUG) {
              System.err.println("X11GLXContext.createContextARBImpl couldn't make current "+getGLVersion(major, minor, ctp, "@creation"));
            }
            // release & destroy
            glXMakeContextCurrent(display, 0, 0, 0);
            GLX.glXDestroyContext(display, ctx);
            ctx = 0;
        } else {
            if (DEBUG) {
                System.err.println(getThreadName() + ": createContextARBImpl: OK "+getGLVersion(major, minor, ctp, "@creation")+", share "+share+", direct "+direct);
            }
            // the following is issued by the caller 'GLContextImpl.createContextARB()'
            // setGLFunctionAvailability(true, major, minor, ctp);
        }
    } else if (DEBUG) {
        System.err.println(getThreadName() + ": createContextARBImpl: NO "+getGLVersion(major, minor, ctp, "@creation"));
    }

    return ctx;
  }

  protected boolean createImpl() {
      // covers the whole context creation loop incl createContextARBImpl and destroyContextARBImpl
      X11Util.setX11ErrorHandler(true, true);
      try {
          return createImplRaw();
      } finally {
          X11Util.setX11ErrorHandler(false, false);
      }
  }

  private boolean createImplRaw() {
    boolean direct = true; // try direct always
    isDirect = false; // fall back

    X11GLXDrawableFactory factory = (X11GLXDrawableFactory)drawable.getFactoryImpl();
    X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    AbstractGraphicsDevice device = config.getScreen().getDevice();
    X11GLXContext sharedContext = (X11GLXContext) factory.getOrCreateSharedContextImpl(device);
    long display = device.getHandle();

    isGLReadDrawableAvailable(); // trigger setup glXVersionOneThreeCapable

    X11GLXContext other = (X11GLXContext) GLContextShareSet.getShareContext(this);
    long share = 0;
    if (other != null) {
      share = other.getHandle();
      if (share == 0) {
        throw new GLException("GLContextShareSet returned an invalid OpenGL context");
      }
      direct = GLX.glXIsDirect(display, share);
    }

    GLCapabilitiesImmutable glCaps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    GLProfile glp = glCaps.getGLProfile();

    if(config.getFBConfigID()<0) {
        // not able to use FBConfig
        if(glp.isGL3()) {
          throw new GLException("Unable to create OpenGL >= 3.1 context");
        }
        contextHandle = GLX.glXCreateContext(display, config.getXVisualInfo(), share, direct);
        if (contextHandle == 0) {
          throw new GLException("Unable to create context(0)");
        }
        if (!glXMakeContextCurrent(display, drawable.getHandle(), drawableRead.getHandle(), contextHandle)) {
          throw new GLException("Error making temp context(0) current: display "+toHexString(display)+", context "+toHexString(contextHandle)+", drawable "+drawable);
        }
        setGLFunctionAvailability(true, 0, 0, CTX_PROFILE_COMPAT|CTX_OPTION_ANY); // use GL_VERSION
        isDirect = GLX.glXIsDirect(display, contextHandle);
        if (DEBUG) {
            System.err.println(getThreadName() + ": createContextImpl: OK (old-1) share "+share+", direct "+isDirect+"/"+direct);
        }
        return true;
    }

    boolean createContextARBTried = false;

    // utilize the shared context's GLXExt in case it was using the ARB method and it already exists
    if(null!=sharedContext && sharedContext.isCreatedWithARBMethod()) {
        contextHandle = createContextARB(share, direct);
        createContextARBTried = true;
        if (DEBUG && 0!=contextHandle) {
            System.err.println(getThreadName() + ": createContextImpl: OK (ARB, using sharedContext) share "+share);
        }
    }

    long temp_ctx = 0;
    if(0==contextHandle) {
        // To use GLX_ARB_create_context, we have to make a temp context current,
        // so we are able to use GetProcAddress
        temp_ctx = GLX.glXCreateNewContext(display, config.getFBConfig(), GLX.GLX_RGBA_TYPE, share, direct);
        if (temp_ctx == 0) {
            throw new GLException("Unable to create temp OpenGL context(1)");
        }
        if (!glXMakeContextCurrent(display, drawable.getHandle(), drawableRead.getHandle(), temp_ctx)) {
          throw new GLException("Error making temp context(1) current: display "+toHexString(display)+", context "+toHexString(temp_ctx)+", drawable "+drawable);
        }
        setGLFunctionAvailability(true, 0, 0, CTX_PROFILE_COMPAT|CTX_OPTION_ANY); // use GL_VERSION
        boolean isCreateContextAttribsARBAvailable = isFunctionAvailable("glXCreateContextAttribsARB");
        glXMakeContextCurrent(display, 0, 0, 0); // release temp context

        if( !createContextARBTried ) {
            if ( isCreateContextAttribsARBAvailable &&
                 isExtensionAvailable("GLX_ARB_create_context") ) {
                // initial ARB context creation
                contextHandle = createContextARB(share, direct);
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
        if(0!=temp_ctx) {
            glXMakeContextCurrent(display, 0, 0, 0);
            GLX.glXDestroyContext(display, temp_ctx);
            if (!glXMakeContextCurrent(display, drawable.getHandle(), drawableRead.getHandle(), contextHandle)) {
                throw new GLException("Cannot make previous verified context current");
            }
        }
    } else {
        if(glp.isGL3()) {
          glXMakeContextCurrent(display, 0, 0, 0);
          GLX.glXDestroyContext(display, temp_ctx);
          throw new GLException("X11GLXContext.createContextImpl failed, but context > GL2 requested "+getGLVersion()+", ");
        }
        if(DEBUG) {
          System.err.println("X11GLXContext.createContextImpl failed, fall back to !ARB context "+getGLVersion());
        }

        // continue with temp context for GL <= 3.0
        contextHandle = temp_ctx;
        if (!glXMakeContextCurrent(display, drawable.getHandle(), drawableRead.getHandle(), contextHandle)) {
          glXMakeContextCurrent(display, 0, 0, 0);
          GLX.glXDestroyContext(display, temp_ctx);
          throw new GLException("Error making context(1) current: display "+toHexString(display)+", context "+toHexString(contextHandle)+", drawable "+drawable);
        }
        if (DEBUG) {
            System.err.println(getThreadName() + ": createContextImpl: OK (old-2) share "+share);
        }
    }
    isDirect = GLX.glXIsDirect(display, contextHandle);
    if (DEBUG) {
        System.err.println(getThreadName() + ": createContextImpl: OK direct "+isDirect+"/"+direct);
    }
    return true;
  }

  protected void makeCurrentImpl(boolean newCreated) throws GLException {
    long dpy = drawable.getNativeSurface().getDisplayHandle();

    if (GLX.glXGetCurrentContext() != contextHandle) {
        X11Util.setX11ErrorHandler(true, true);
        try {
            if (!glXMakeContextCurrent(dpy, drawable.getHandle(), drawableRead.getHandle(), contextHandle)) {
                throw new GLException("Error making context current: "+this);
            }
        } finally {
            X11Util.setX11ErrorHandler(false, false);
        }
        if (DEBUG && newCreated) {
            System.err.println(getThreadName() + ": glXMakeCurrent(display " + 
                               toHexString(dpy)+
                               ", drawable " + toHexString(drawable.getHandle()) +
                               ", drawableRead " + toHexString(drawableRead.getHandle()) +
                               ", context " + toHexString(contextHandle) + ") succeeded");
        }
    }
  }

  protected void releaseImpl() throws GLException {
    long display = drawable.getNativeSurface().getDisplayHandle();
    X11Util.setX11ErrorHandler(true, true);
    try {
        if (!glXMakeContextCurrent(display, 0, 0, 0)) {
            throw new GLException("Error freeing OpenGL context");
        }
    } finally {
        X11Util.setX11ErrorHandler(false, false);
    }
  }

  protected void destroyImpl() throws GLException {
    long display = drawable.getNativeSurface().getDisplayHandle();
    if (DEBUG) {
      System.err.println("glXDestroyContext(dpy " +
                         toHexString(display)+
                         ", ctx " +
                         toHexString(contextHandle) + ")");
    }
    GLX.glXDestroyContext(display, contextHandle);
    if (DEBUG) {
      System.err.println("!!! Destroyed OpenGL context " + contextHandle);
    }
  }

  protected void copyImpl(GLContext source, int mask) throws GLException {
    long dst = getHandle();
    long src = source.getHandle();
    long display = drawable.getNativeSurface().getDisplayHandle();
    if (0 == display) {
      throw new GLException("Connection to X display not yet set up");
    }
    GLX.glXCopyContext(display, src, dst, mask);
    // Should check for X errors and raise GLException
  }

  protected final void updateGLXProcAddressTable() {
    AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
    String key = adevice.getUniqueID();
    if (DEBUG) {
      System.err.println(getThreadName() + ": !!! Initializing GLX extension address table: "+key);
    }
    glXQueryExtensionsStringInitialized = false;
    glXQueryExtensionsStringAvailable = false;

    ProcAddressTable table = null;
    synchronized(mappedContextTypeObjectLock) {
        table = mappedGLXProcAddress.get( key );
    }
    if(null != table) {
        glXExtProcAddressTable = (GLXExtProcAddressTable) table;
        if(DEBUG) {
            System.err.println(getThreadName() + ": !!! GLContext GLX ProcAddressTable reusing key("+key+") -> "+table.hashCode());
        }
    } else {
        if (glXExtProcAddressTable == null) {
          glXExtProcAddressTable = new GLXExtProcAddressTable(new GLProcAddressResolver());
        }
        resetProcAddressTable(getGLXExtProcAddressTable());
        synchronized(mappedContextTypeObjectLock) {
            mappedGLXProcAddress.put(key, getGLXExtProcAddressTable());
            if(DEBUG) {
                System.err.println(getThreadName() + ": !!! GLContext GLX ProcAddressTable mapping key("+key+") -> "+getGLXExtProcAddressTable().hashCode());
                Thread.dumpStack();
            }
        }
    }
  }

  public synchronized String getPlatformExtensionsString() {
    if (!glXQueryExtensionsStringInitialized) {
      glXQueryExtensionsStringAvailable =
        getDrawableImpl().getGLDynamicLookupHelper().dynamicLookupFunction("glXQueryExtensionsString") != 0;
      glXQueryExtensionsStringInitialized = true;
    }
    if (glXQueryExtensionsStringAvailable) {
        NativeSurface ns = drawable.getNativeSurface();
        String ret = GLX.glXQueryExtensionsString(ns.getDisplayHandle(), ns.getScreenIndex());
        if (DEBUG) {
          System.err.println("!!! GLX extensions: " + ret);
        }
        return ret;
    } else {
      return "";
    }
  }

  public boolean isExtensionAvailable(String glExtensionName) {
    if (glExtensionName.equals("GL_ARB_pbuffer") ||
        glExtensionName.equals("GL_ARB_pixel_format")) {
      return getGLDrawable().getFactory().canCreateGLPbuffer(
          drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration().getScreen().getDevice() );
    }
    return super.isExtensionAvailable(glExtensionName);
  }

  protected void setSwapIntervalImpl(int interval) {
    X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    GLCapabilitiesImmutable glCaps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    if(!glCaps.isOnscreen()) return;

    GLXExt glXExt = getGLXExt();
    if(0==hasSwapIntervalSGI) {
        try {
            hasSwapIntervalSGI = glXExt.isExtensionAvailable("GLX_SGI_swap_control")?1:-1;
        } catch (Throwable t) { hasSwapIntervalSGI=1; }
    }
    if (hasSwapIntervalSGI>0) {
        try {
            if( 0 == glXExt.glXSwapIntervalSGI(interval) ) {
                currentSwapInterval = interval;
            }
        } catch (Throwable t) { hasSwapIntervalSGI=-1; }
    }
  }

  public ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3) {
    return getGLXExt().glXAllocateMemoryNV(arg0, arg1, arg2, arg3);
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

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(getClass().getName());
    sb.append(" [");
    super.append(sb);
    sb.append(", direct ");
    sb.append(isDirect);
    sb.append("] ");
    return sb.toString();
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //
}

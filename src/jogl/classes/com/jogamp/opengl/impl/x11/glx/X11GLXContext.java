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

package com.jogamp.opengl.impl.x11.glx;

import java.nio.*;
import java.util.*;
import javax.media.opengl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;
import com.jogamp.opengl.impl.*;
import com.jogamp.opengl.impl.x11.glx.*;
import com.jogamp.nativewindow.impl.x11.*;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;

public abstract class X11GLXContext extends GLContextImpl {
  protected long context;
  private boolean glXQueryExtensionsStringInitialized;
  private boolean glXQueryExtensionsStringAvailable;
  private static final Map/*<String, String>*/ functionNameMap;
  private static final Map/*<String, String>*/ extensionNameMap;
  private GLXExt glXExt;
  // Table that holds the addresses of the native C-language entry points for
  // GLX extension functions.
  private GLXExtProcAddressTable glXExtProcAddressTable;

  static {
    functionNameMap = new HashMap();
    functionNameMap.put("glAllocateMemoryNV", "glXAllocateMemoryNV");
    functionNameMap.put("glFreeMemoryNV", "glXFreeMemoryNV");

    extensionNameMap = new HashMap();
    extensionNameMap.put("GL_ARB_pbuffer",      "GLX_SGIX_pbuffer");
    extensionNameMap.put("GL_ARB_pixel_format", "GLX_SGIX_pbuffer"); // good enough
  }

  public X11GLXContext(GLDrawableImpl drawable, GLDrawableImpl drawableRead,
                      GLContext shareWith) {
    super(drawable, drawableRead, shareWith);
  }

  public X11GLXContext(GLDrawableImpl drawable,
                      GLContext shareWith) {
    this(drawable, null, shareWith);
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

  protected Map/*<String, String>*/ getFunctionNameMap() { return functionNameMap; }

  protected Map/*<String, String>*/ getExtensionNameMap() { return extensionNameMap; }

  protected boolean glXMakeContextCurrent(long dpy, long writeDrawable, long readDrawable, long ctx) {
    boolean res = false;

    try {
        // at least on ATI we receive 'often' SEGV in case of 
        // highly multithreaded MakeContextCurrent calls with writeDrawable==readDrawable
        if(writeDrawable==readDrawable) {
            res = GLX.glXMakeCurrent(dpy, writeDrawable, ctx);
        } else {
            res = GLX.glXMakeContextCurrent(dpy, writeDrawable, readDrawable, ctx);
        }
    } catch (RuntimeException re) {
        if(DEBUG) {
          System.err.println("X11GLXContext.glXMakeContextCurrent failed: "+re+", with "+
            "dpy "+toHexString(dpy)+
            ", write "+toHexString(writeDrawable)+
            ", read "+toHexString(readDrawable)+
            ", ctx "+toHexString(ctx));
          re.printStackTrace();
        }
    }
    return res;
  }

  protected void destroyContextARBImpl(long _context) {
    X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)drawable.getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    long display = config.getScreen().getDevice().getHandle();

    glXMakeContextCurrent(display, 0, 0, 0);
    GLX.glXDestroyContext(display, _context);
  }

  protected long createContextARBImpl(long share, boolean direct, int ctp, int major, int minor) {
    X11GLXDrawableFactory factory = (X11GLXDrawableFactory)drawable.getFactoryImpl();
    X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)drawable.getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    long display = config.getScreen().getDevice().getHandle();

    GLXExt glXExt;
    if(null==factory.getSharedContext()) {
        glXExt = getGLXExt();
    } else {
        glXExt = ((X11GLXContext)factory.getSharedContext()).getGLXExt();
    }

    boolean ctBwdCompat = 0 != ( CTX_PROFILE_COMPAT & ctp ) ;
    boolean ctFwdCompat = 0 != ( CTX_OPTION_FORWARD & ctp ) ;
    boolean ctDebug     = 0 != ( CTX_OPTION_DEBUG & ctp ) ;

    long _context=0;

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

    try {
        _context = glXExt.glXCreateContextAttribsARB(display, config.getFBConfig(), share, direct, attribs, 0);
    } catch (RuntimeException re) {
        if(DEBUG) {
          System.err.println("X11GLXContext.createContextARB glXCreateContextAttribsARB failed: "+re+", with "+getGLVersion(major, minor, ctp, "@creation"));
          re.printStackTrace();
        }
    }
    if(DEBUG) {
      System.err.println("X11GLXContext.createContextARB success: "+(0!=_context)+" - "+getGLVersion(major, minor, ctp, "@creation")+", bwdCompat "+ctBwdCompat+", fwdCompat "+ctFwdCompat);
    }
    if(0!=_context) {
        if (!glXMakeContextCurrent(display,
                                   drawable.getNativeWindow().getSurfaceHandle(), 
                                   drawableRead.getNativeWindow().getSurfaceHandle(), 
                                   _context)) {
            if(DEBUG) {
              System.err.println("X11GLXContext.createContextARB couldn't make current "+getGLVersion(major, minor, ctp, "@creation"));
            }
            glXMakeContextCurrent(display, 0, 0, 0);
            GLX.glXDestroyContext(display, _context);
            _context = 0;
        }
    }
    return _context;
  }

  /**
   * Creates and initializes an appropriate OpenGL context. Should only be
   * called by {@link #create()}.
   * Note: The direct parameter may be overwritten by the direct state of a shared context.
   */
  protected void createContext(boolean direct) {
    if(0!=context) {
        throw new GLException("context is not null: "+context);
    }
    X11GLXDrawableFactory factory = (X11GLXDrawableFactory)drawable.getFactoryImpl();
    X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)drawable.getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    long display = config.getScreen().getDevice().getHandle();

    X11GLXContext other = (X11GLXContext) GLContextShareSet.getShareContext(this);
    long share = 0;
    if (other != null) {
      share = other.getContext();
      if (share == 0) {
        throw new GLException("GLContextShareSet returned an invalid OpenGL context");
      }
      direct = GLX.glXIsDirect(display, share);
    }

    GLCapabilities glCaps = (GLCapabilities) config.getChosenCapabilities();
    GLProfile glp = glCaps.getGLProfile();
    isVendorATI = factory.isVendorATI();

    if(config.getFBConfigID()<0) {
        // not able to use FBConfig
        if(glp.isGL3()) {
          throw new GLException("Unable to create OpenGL >= 3.1 context");
        }
        context = GLX.glXCreateContext(display, config.getXVisualInfo(), share, direct);
        if (context == 0) {
          throw new GLException("Unable to create context(0)");
        }
        if (!glXMakeContextCurrent(display,
                                   drawable.getNativeWindow().getSurfaceHandle(), 
                                   drawableRead.getNativeWindow().getSurfaceHandle(), 
                                   context)) {
          throw new GLException("Error making temp context(0) current: display "+toHexString(display)+", context "+toHexString(context)+", drawable "+drawable);
        }
        setGLFunctionAvailability(true, 0, 0, CTX_PROFILE_COMPAT|CTX_OPTION_ANY); // use GL_VERSION
        return;
    }

    int minor[] = new int[1];
    int major[] = new int[1];
    int ctp[] = new int[1];
    boolean createContextARBTried = false;

    // utilize the shared context's GLXExt in case it was using the ARB method and it already exists
    if(null!=factory.getSharedContext() && factory.getSharedContext().isCreatedWithARBMethod()) {
        if(DEBUG) {
          System.err.println("X11GLXContext.createContext using shared Context: "+factory.getSharedContext());
        }
        context = createContextARB(share, direct, major, minor, ctp);
        createContextARBTried = true;
    }

    long temp_context = 0;
    if(0==context) {
        // To use GLX_ARB_create_context, we have to make a temp context current,
        // so we are able to use GetProcAddress
        temp_context = GLX.glXCreateNewContext(display, config.getFBConfig(), GLX.GLX_RGBA_TYPE, share, direct);
        if (temp_context == 0) {
            throw new GLException("Unable to create temp OpenGL context(1)");
        }
        if (!glXMakeContextCurrent(display,
                                   drawable.getNativeWindow().getSurfaceHandle(), 
                                   drawableRead.getNativeWindow().getSurfaceHandle(), 
                                   temp_context)) {
          throw new GLException("Error making temp context(1) current: display "+toHexString(display)+", context "+toHexString(temp_context)+", drawable "+drawable);
        }
        setGLFunctionAvailability(true, 0, 0, CTX_PROFILE_COMPAT|CTX_OPTION_ANY); // use GL_VERSION

        if( createContextARBTried ||
            !isFunctionAvailable("glXCreateContextAttribsARB") ||
            !isExtensionAvailable("GLX_ARB_create_context") )  {
            if(glp.isGL3()) {
              glXMakeContextCurrent(display, 0, 0, 0);
              GLX.glXDestroyContext(display, temp_context);
              throw new GLException("Unable to create OpenGL >= 3.1 context (failed GLX_ARB_create_context), GLProfile "+glp+", Drawable "+drawable);
            }

            // continue with temp context for GL < 3.0
            context = temp_context;
            return;
        }
        context = createContextARB(share, direct, major, minor, ctp);
        createContextARBTried=true;
    }

    if(0!=context) {
        if(0!=temp_context) {
            glXMakeContextCurrent(display, 0, 0, 0);
            GLX.glXDestroyContext(display, temp_context);
            if (!glXMakeContextCurrent(display,
                                       drawable.getNativeWindow().getSurfaceHandle(), 
                                       drawableRead.getNativeWindow().getSurfaceHandle(), 
                                       context)) {
                throw new GLException("Cannot make previous verified context current");
            }
        }
    } else {
        if(glp.isGL3()) {
          glXMakeContextCurrent(display, 0, 0, 0);
          GLX.glXDestroyContext(display, temp_context);
          throw new GLException("X11GLXContext.createContext failed, but context > GL2 requested "+getGLVersion(major[0], minor[0], ctp[0], "@creation")+", ");
        }
        if(DEBUG) {
          System.err.println("X11GLXContext.createContext failed, fall back to !ARB context "+getGLVersion(major[0], minor[0], ctp[0], "@creation"));
        }

        // continue with temp context for GL <= 3.0
        context = temp_context;
        if (!glXMakeContextCurrent(display,
                                   drawable.getNativeWindow().getSurfaceHandle(), 
                                   drawableRead.getNativeWindow().getSurfaceHandle(), 
                                   context)) {
          glXMakeContextCurrent(display, 0, 0, 0);
          GLX.glXDestroyContext(display, temp_context);
          throw new GLException("Error making context(1) current: display "+toHexString(display)+", context "+toHexString(context)+", drawable "+drawable);
        }
    }
  }

  // Note: Usually the surface shall be locked within [makeCurrent .. swap .. release]
  protected int makeCurrentImpl() throws GLException {
    boolean exceptionOccurred = false;
    int lockRes = drawable.lockSurface();
    try {
      if (lockRes == NativeWindow.LOCK_SURFACE_NOT_READY) {
        return CONTEXT_NOT_CURRENT;
      }
      if (0 == drawable.getNativeWindow().getSurfaceHandle()) {
          throw new GLException("drawable has invalid surface handle: "+drawable);
      }
      return makeCurrentImplAfterLock();
    } catch (RuntimeException e) {
      exceptionOccurred = true;
      throw e;
    } finally {
      if (exceptionOccurred ||
          (isOptimizable() && lockRes != NativeWindow.LOCK_SURFACE_NOT_READY) && drawable.isSurfaceLocked()) {
        drawable.unlockSurface();
      }
    }
  }

  // Note: Usually the surface shall be locked within [makeCurrent .. swap .. release]
  protected void releaseImpl() throws GLException {
    try {
      releaseImplAfterLock();
    } finally {
      if (!isOptimizable() && drawable.isSurfaceLocked()) {
        drawable.unlockSurface();
      }
    }
  }

  protected int makeCurrentImplAfterLock() throws GLException {
    long dpy = drawable.getNativeWindow().getDisplayHandle();

    getDrawableImpl().getFactoryImpl().lockToolkit();
    try {
        boolean created = false;
        if (context == 0) {
          create();
          created = true;
          GLContextShareSet.contextCreated(this);
          if (DEBUG) {
            System.err.println(getThreadName() + ": !!! Created GL context for " + getClass().getName());
          }
        }

        if (GLX.glXGetCurrentContext() != context) {
            
            if (!glXMakeContextCurrent(dpy,
                                       drawable.getNativeWindow().getSurfaceHandle(), 
                                       drawableRead.getNativeWindow().getSurfaceHandle(), 
                                       context)) {
              throw new GLException("Error making context current: "+this);
            }
            if (DEBUG && (VERBOSE || created)) {
              System.err.println(getThreadName() + ": glXMakeCurrent(display " + 
                                 toHexString(dpy)+
                                 ", drawable " + toHexString(drawable.getNativeWindow().getSurfaceHandle()) +
                                 ", drawableRead " + toHexString(drawableRead.getNativeWindow().getSurfaceHandle()) +
                                 ", context " + toHexString(context) + ") succeeded");
            }
        }

        if (created) {
          setGLFunctionAvailability(false, -1, -1, CTX_PROFILE_COMPAT|CTX_OPTION_ANY);
          return CONTEXT_CURRENT_NEW;
        }
        return CONTEXT_CURRENT;
    } finally {
        getDrawableImpl().getFactoryImpl().unlockToolkit();
    }
  }

  protected void releaseImplAfterLock() throws GLException {
    X11GLXDrawableFactory factory = (X11GLXDrawableFactory)drawable.getFactoryImpl();
    factory.lockToolkit();
    try {
        if (!glXMakeContextCurrent(drawable.getNativeWindow().getDisplayHandle(), 0, 0, 0)) {
            throw new GLException("Error freeing OpenGL context");
        }
    } finally {
        factory.unlockToolkit();
    }
  }

  protected void destroyImpl() throws GLException {
    getDrawableImpl().getFactoryImpl().lockToolkit();
    try {
        if (context != 0) {
            if (DEBUG) {
              System.err.println("glXDestroyContext(" +
                                 toHexString(drawable.getNativeWindow().getDisplayHandle()) +
                                 ", " +
                                 toHexString(context) + ")");
            }
            GLX.glXDestroyContext(drawable.getNativeWindow().getDisplayHandle(), context);
            if (DEBUG) {
              System.err.println("!!! Destroyed OpenGL context " + context);
            }
            context = 0;
            GLContextShareSet.contextDestroyed(this);
        }
    } finally {
        getDrawableImpl().getFactoryImpl().unlockToolkit();
    }
  }

  public boolean isCreated() {
    return (context != 0);
  }

  public void copy(GLContext source, int mask) throws GLException {
    long dst = getContext();
    long src = ((X11GLXContext) source).getContext();
    if (src == 0) {
      throw new GLException("Source OpenGL context has not been created");
    }
    if (dst == 0) {
      throw new GLException("Destination OpenGL context has not been created");
    }
    if (drawable.getNativeWindow().getDisplayHandle() == 0) {
      throw new GLException("Connection to X display not yet set up");
    }
    getDrawableImpl().getFactoryImpl().lockToolkit();
    try {
      GLX.glXCopyContext(drawable.getNativeWindow().getDisplayHandle(), src, dst, mask);
      // Should check for X errors and raise GLException
    } finally {
      getDrawableImpl().getFactoryImpl().unlockToolkit();
    }
  }

  protected void updateGLProcAddressTable(int major, int minor, int ctp) {
    if (DEBUG) {
      System.err.println(getThreadName() + ": !!! Initializing GLX extension address table");
    }
    glXQueryExtensionsStringInitialized = false;
    glXQueryExtensionsStringAvailable = false;

    if (glXExtProcAddressTable == null) {
      // FIXME: cache ProcAddressTables by OpenGL context type bits so we can
      // share them among contexts classes (GL4, GL4bc, GL3, GL3bc, ..)
      glXExtProcAddressTable = new GLXExtProcAddressTable(new GLProcAddressResolver());
    }          
    resetProcAddressTable(getGLXExtProcAddressTable());
    super.updateGLProcAddressTable(major, minor, ctp);
  }

  public synchronized String getPlatformExtensionsString() {
    if (!glXQueryExtensionsStringInitialized) {
      glXQueryExtensionsStringAvailable =
        getDrawableImpl().getDynamicLookupHelper().dynamicLookupFunction("glXQueryExtensionsString") != 0;
      glXQueryExtensionsStringInitialized = true;
    }
    if (glXQueryExtensionsStringAvailable) {
      GLDrawableFactoryImpl factory = getDrawableImpl().getFactoryImpl();
      factory.lockToolkit();
      try {
        String ret = GLX.glXQueryExtensionsString(drawable.getNativeWindow().getDisplayHandle(), 
                                                  drawable.getNativeWindow().getScreenIndex());
        if (DEBUG) {
          System.err.println("!!! GLX extensions: " + ret);
        }
        return ret;
      } finally {
        factory.unlockToolkit();
      }
    } else {
      return "";
    }
  }

  public boolean isExtensionAvailable(String glExtensionName) {
    if (glExtensionName.equals("GL_ARB_pbuffer") ||
        glExtensionName.equals("GL_ARB_pixel_format")) {
      return getGLDrawable().getFactory().canCreateGLPbuffer(
          drawable.getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration().getScreen().getDevice() );
    }
    return super.isExtensionAvailable(glExtensionName);
  }


  private int hasSwapIntervalSGI = 0;

  protected void setSwapIntervalImpl(int interval) {
    X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)drawable.getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    GLCapabilities glCaps = (GLCapabilities) config.getChosenCapabilities();
    if(!glCaps.isOnscreen()) return;

    getDrawableImpl().getFactoryImpl().lockToolkit();
    try {
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
    } finally {
      getDrawableImpl().getFactoryImpl().unlockToolkit();
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

  public boolean isOptimizable() {
    return (super.isOptimizable() && !isVendorATI);
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  public long getContext() {
    return context;
  }

  private boolean isVendorATI = false;

}

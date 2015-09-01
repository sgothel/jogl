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

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.nativewindow.x11.X11Lib;
import jogamp.nativewindow.x11.X11Util;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.GLXExtensions;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.opengl.GLExtensions;

public class X11GLXContext extends GLContextImpl {
  private static final Map<String, String> extensionNameMap;
  private GLXExt _glXExt;
  // Table that holds the addresses of the native C-language entry points for
  // GLX extension functions.
  private GLXExtProcAddressTable glXExtProcAddressTable;
  /** 3 SGI, 2 GLX_EXT_swap_control_tear, 1 GLX_EXT_swap_control, 0 undefined, -1 none */
  private int hasSwapInterval = 0;
  private int hasSwapGroupNV = 0;

  // This indicates whether the context we have created is indirect
  // and therefore requires the toolkit to be locked around all GL
  // calls rather than just all GLX calls
  protected boolean isDirect;
  protected volatile VersionNumber glXServerVersion;
  protected volatile boolean isGLXVersionGreaterEqualOneThree;

  static {
    extensionNameMap = new HashMap<String, String>();
    extensionNameMap.put(GLExtensions.ARB_pbuffer,      X11GLXDrawableFactory.GLX_SGIX_pbuffer);
    extensionNameMap.put(GLExtensions.ARB_pixel_format, X11GLXDrawableFactory.GLX_SGIX_pbuffer); // good enough
  }

  X11GLXContext(final GLDrawableImpl drawable,
                final GLContext shareWith) {
    super(drawable, shareWith);
  }

  @Override
  protected void resetStates(final boolean isInit) {
    // no inner state _glXExt=null;
    glXExtProcAddressTable = null;
    hasSwapInterval = 0;
    hasSwapGroupNV = 0;
    isDirect = false;
    glXServerVersion = null;
    isGLXVersionGreaterEqualOneThree = false;
    super.resetStates(isInit);
  }

  @Override
  public final ProcAddressTable getPlatformExtProcAddressTable() {
    return getGLXExtProcAddressTable();
  }

  public final GLXExtProcAddressTable getGLXExtProcAddressTable() {
    return glXExtProcAddressTable;
  }

  @Override
  public Object getPlatformGLExtensions() {
    return getGLXExt();
  }

  public GLXExt getGLXExt() {
    if (_glXExt == null) {
      _glXExt = new GLXExtImpl(this);
    }
    return _glXExt;
  }

  @Override
  protected Map<String, String> getFunctionNameMap() { return null; }

  @Override
  protected Map<String, String> getExtensionNameMap() { return extensionNameMap; }

  protected final boolean isGLXVersionGreaterEqualOneThree() { // fast-path: use cached boolean
    if(null != glXServerVersion) {
        return isGLXVersionGreaterEqualOneThree;
    }
    glXServerVersion = ((X11GLXDrawableFactory)drawable.getFactoryImpl()).getGLXVersionNumber(drawable.getNativeSurface().getGraphicsConfiguration().getScreen().getDevice());
    isGLXVersionGreaterEqualOneThree = null != glXServerVersion ? glXServerVersion.compareTo(X11GLXDrawableFactory.versionOneThree) >= 0 : false;
    return isGLXVersionGreaterEqualOneThree;
  }
  protected final void forceGLXVersionOneOne() {
    glXServerVersion = X11GLXDrawableFactory.versionOneOne;
    isGLXVersionGreaterEqualOneThree = false;
    if(DEBUG) {
        System.err.println("X11GLXContext.forceGLXVersionNumber: "+glXServerVersion);
    }
  }

  @Override
  public final boolean isGLReadDrawableAvailable() {
    return isGLXVersionGreaterEqualOneThree();
  }

  private final boolean glXMakeContextCurrent(final long dpy, final long writeDrawable, final long readDrawable, final long ctx) {
    boolean res = false;

    try {
        if ( isGLXVersionGreaterEqualOneThree() ) {
            // System.err.println(getThreadName() +": X11GLXContext.makeCurrent: obj " + toHexString(hashCode()) + " / ctx "+toHexString(contextHandle)+": ctx "+toHexString(ctx)+", [write "+toHexString(writeDrawable)+", read "+toHexString(readDrawable)+"] - switch");
            res = GLX.glXMakeContextCurrent(dpy, writeDrawable, readDrawable, ctx);
        } else if ( writeDrawable == readDrawable ) {
            // System.err.println(getThreadName() +": X11GLXContext.makeCurrent: obj " + toHexString(hashCode()) + " / ctx "+toHexString(contextHandle)+": ctx "+toHexString(ctx)+", [write "+toHexString(writeDrawable)+"] - switch");
            res = GLX.glXMakeCurrent(dpy, writeDrawable, ctx);
        } else {
            // should not happen due to 'isGLReadDrawableAvailable()' query in GLContextImpl
            throw new InternalError("Given readDrawable but no driver support");
        }
    } catch (final RuntimeException re) {
        if( DEBUG_TRACE_SWITCH ) {
          System.err.println(getThreadName()+": Warning: X11GLXContext.glXMakeContextCurrent failed: "+re+", with "+
            "dpy "+toHexString(dpy)+
            ", write "+toHexString(writeDrawable)+
            ", read "+toHexString(readDrawable)+
            ", ctx "+toHexString(ctx));
          re.printStackTrace();
        }
    }
    return res;
  }

  private final boolean glXReleaseContext(final long dpy) {
    boolean res = false;

    try {
        if ( isGLXVersionGreaterEqualOneThree() ) {
            // System.err.println(getThreadName() +": X11GLXContext.releaseCurrent: obj " + toHexString(hashCode()) + " / ctx "+toHexString(contextHandle)+": ctx "+toHexString(ctx)+" - switch");
            res = GLX.glXMakeContextCurrent(dpy, 0, 0, 0);
        } else {
            // System.err.println(getThreadName() +": X11GLXContext.releaseCurrent: obj " + toHexString(hashCode()) + " / ctx "+toHexString(contextHandle)+": ctx "+toHexString(ctx)+" - switch");
            res = GLX.glXMakeCurrent(dpy, 0, 0);
        }
    } catch (final RuntimeException re) {
        if( DEBUG_TRACE_SWITCH ) {
          System.err.println(getThreadName()+": Warning: X11GLXContext.glXReleaseContext failed: "+re+", with "+
            "dpy "+toHexString(dpy));
          re.printStackTrace();
        }
    }
    return res;
  }

  @Override
  protected void destroyContextARBImpl(final long ctx) {
    final long display = drawable.getNativeSurface().getDisplayHandle();

    glXReleaseContext(display);
    GLX.glXDestroyContext(display, ctx);
  }
  private static final int ctx_arb_attribs_idx_major = 0;
  private static final int ctx_arb_attribs_idx_minor = 2;
  private static final int ctx_arb_attribs_idx_flags = 6;
  private static final int ctx_arb_attribs_idx_profile = 8;
  private static final int ctx_arb_attribs_rom[] = {
        /*  0 */ GLX.GLX_CONTEXT_MAJOR_VERSION_ARB, 0,
        /*  2 */ GLX.GLX_CONTEXT_MINOR_VERSION_ARB, 0,
        /*  4 */ GLX.GLX_RENDER_TYPE,               GLX.GLX_RGBA_TYPE, // default
        /*  6 */ GLX.GLX_CONTEXT_FLAGS_ARB,         0,
        /*  8 */ 0,                                 0,
        /* 10 */ 0
    };

  @Override
  protected long createContextARBImpl(final long share, final boolean direct, final int ctp, final int major, final int minor) {
    if(DEBUG) {
      System.err.println(getThreadName()+": X11GLXContext.createContextARBImpl: "+getGLVersion(major, minor, ctp, "@creation") +
                         ", handle "+toHexString(drawable.getHandle()) + ", share "+toHexString(share)+", direct "+direct);
    }
    final boolean ctDesktopGL = 0 == ( CTX_PROFILE_ES & ctp );
    final boolean ctBwdCompat = 0 != ( CTX_PROFILE_COMPAT & ctp ) ;
    final boolean ctFwdCompat = 0 != ( CTX_OPTION_FORWARD & ctp ) ;
    final boolean ctDebug     = 0 != ( CTX_OPTION_DEBUG & ctp ) ;
    if( !ctDesktopGL ) {
        if(DEBUG) {
            System.err.println(getThreadName() + ": X11GLXContext.createContextARBImpl: GL ES not avail "+getGLVersion(major, minor, ctp, "@creation"));
        }
        return 0; // n/a
    }
    final GLDynamicLookupHelper dlh = getGLDynamicLookupHelper(major, ctp);
    if( null == dlh ) {
        if(DEBUG) {
            System.err.println(getThreadName()+" - X11GLXContext.createContextARBImpl: Null GLDynamicLookupHelper");
        }
        return 0;
    } else {
        updateGLXProcAddressTable(null, dlh);
    }
    final GLXExt _glXExt = getGLXExt();
    if(DEBUG) {
      System.err.println(getThreadName()+": X11GLXContext.createContextARBImpl: "+
                         ", glXCreateContextAttribsARB: "+toHexString(glXExtProcAddressTable._addressof_glXCreateContextAttribsARB));
    }

    final IntBuffer attribs = Buffers.newDirectIntBuffer(ctx_arb_attribs_rom);
    attribs.put(ctx_arb_attribs_idx_major + 1, major);
    attribs.put(ctx_arb_attribs_idx_minor + 1, minor);

    if ( major > 3 || major == 3 && minor >= 2  ) {
        attribs.put(ctx_arb_attribs_idx_profile + 0, GLX.GLX_CONTEXT_PROFILE_MASK_ARB);
        if( ctBwdCompat ) {
            attribs.put(ctx_arb_attribs_idx_profile + 1, GLX.GLX_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB);
        } else {
            attribs.put(ctx_arb_attribs_idx_profile + 1, GLX.GLX_CONTEXT_CORE_PROFILE_BIT_ARB);
        }
    }

    if ( major >= 3 ) {
        int flags = attribs.get(ctx_arb_attribs_idx_flags + 1);
        if( !ctBwdCompat && ctFwdCompat ) {
            flags |= GLX.GLX_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB;
        }
        if( ctDebug ) {
            flags |= GLX.GLX_CONTEXT_DEBUG_BIT_ARB;
        }
        attribs.put(ctx_arb_attribs_idx_flags + 1, flags);
    }

    final X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)drawable.getNativeSurface().getGraphicsConfiguration();
    final AbstractGraphicsDevice device = config.getScreen().getDevice();
    final long display = device.getHandle();
    long ctx=0;

    try {
        // critical path, a remote display might not support this command,
        // hence we need to catch the X11 Error within this block.
        X11Util.setX11ErrorHandler(true, DEBUG ? false : true); // make sure X11 error handler is set
        X11Lib.XSync(display, false);
        ctx = _glXExt.glXCreateContextAttribsARB(display, config.getFBConfig(), share, direct, attribs);
    } catch (final RuntimeException re) {
        if(DEBUG) {
          System.err.println(getThreadName()+": Info: X11GLXContext.createContextARBImpl glXCreateContextAttribsARB failed with "+getGLVersion(major, minor, ctp, "@creation"));
          ExceptionUtils.dumpThrowable("", re);
        }
    }

    if(0!=ctx) {
        if (!glXMakeContextCurrent(display, drawable.getHandle(), drawableRead.getHandle(), ctx)) {
            if(DEBUG) {
              System.err.println(getThreadName()+": X11GLXContext.createContextARBImpl couldn't make current "+getGLVersion(major, minor, ctp, "@creation"));
            }
            // release & destroy
            glXReleaseContext(display);
            GLX.glXDestroyContext(display, ctx);
            ctx = 0;
        } else if (DEBUG) {
            System.err.println(getThreadName() + ": createContextARBImpl: OK "+getGLVersion(major, minor, ctp, "@creation")+", share "+share+", direct "+direct);
        }
    } else if (DEBUG) {
        System.err.println(getThreadName() + ": createContextARBImpl: NO "+getGLVersion(major, minor, ctp, "@creation"));
    }

    return ctx;
  }

  @Override
  protected boolean createImpl(final long shareWithHandle) throws GLException {
    boolean direct = true; // try direct always
    isDirect = false; // fall back

    final X11GLXDrawableFactory factory = (X11GLXDrawableFactory)drawable.getFactoryImpl();
    final X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)drawable.getNativeSurface().getGraphicsConfiguration();
    final AbstractGraphicsDevice device = config.getScreen().getDevice();
    final X11GLXContext sharedContext = (X11GLXContext) factory.getOrCreateSharedContext(device);
    final long display = device.getHandle();

    if ( 0 != shareWithHandle ) {
        direct = GLX.glXIsDirect(display, shareWithHandle);
    }

    final GLCapabilitiesImmutable glCaps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    final GLProfile glp = glCaps.getGLProfile();
    final boolean createContextARBAvailable = isCreateContextARBAvail(device) && config.hasFBConfig();
    final boolean sharedCreatedWithARB = null != sharedContext && sharedContext.isCreatedWithARBMethod();
    if(DEBUG) {
        System.err.println(getThreadName() + ": X11GLXContext.createImpl: START "+glCaps+", share "+toHexString(shareWithHandle));
        System.err.println(getThreadName() + ": Use ARB[avail["+getCreateContextARBAvailStr(device)+
                "], fbCfg "+config.hasFBConfig()+" -> "+createContextARBAvailable+
                "], shared "+sharedCreatedWithARB+"]");
    }
    if( glp.isGLES() ) {
        throw new GLException(getThreadName()+": Unable to create OpenGL ES context on desktopDevice "+device+
                ", config "+config+", "+glp+", shareWith "+toHexString(shareWithHandle));
    }

    if( !config.hasFBConfig() ) {
        // not able to use FBConfig -> GLX 1.1
        forceGLXVersionOneOne();
        if( glp.isGL3() ) {
          throw new GLException(getThreadName()+": Unable to create OpenGL >= 3.1 context w/o FBConfig");
        }
        contextHandle = GLX.glXCreateContext(display, config.getXVisualInfo(), shareWithHandle, direct);
        if ( 0 == contextHandle ) {
          throw new GLException(getThreadName()+": Unable to create context(0)");
        }
        if ( !glXMakeContextCurrent(display, drawable.getHandle(), drawableRead.getHandle(), contextHandle) ) {
          throw new GLException(getThreadName()+": Error making temp context(0) current: display "+toHexString(display)+", context "+toHexString(contextHandle)+", drawable "+drawable);
        }
        if( !setGLFunctionAvailability(true, 0, 0, CTX_PROFILE_COMPAT, false /* strictMatch */, null == sharedContext /* withinGLVersionsMapping */) ) { // use GL_VERSION
            glXReleaseContext(display); // release temp context
            GLX.glXDestroyContext(display, contextHandle);
            contextHandle = 0;
            throw new GLException("setGLFunctionAvailability !strictMatch failed.1");
        }
        isDirect = GLX.glXIsDirect(display, contextHandle);
        if (DEBUG) {
            System.err.println(getThreadName() + ": createImpl: OK (old-1) share "+toHexString(shareWithHandle)+", direct "+isDirect+"/"+direct);
        }
        return true;
    }
    boolean createContextARBTried = false;

    // utilize the shared context's GLXExt in case it was using the ARB method and it already exists
    if( createContextARBAvailable && sharedCreatedWithARB ) {
        contextHandle = createContextARB(shareWithHandle, direct);
        createContextARBTried = true;
        if ( DEBUG && 0 != contextHandle ) {
            System.err.println(getThreadName() + ": createImpl: OK (ARB, using sharedContext) share "+toHexString(shareWithHandle));
        }
    }

    final long temp_ctx;
    if( 0 == contextHandle ) {
        // To use GLX_ARB_create_context, we have to make a temp context current,
        // so we are able to use GetProcAddress
        temp_ctx = GLX.glXCreateNewContext(display, config.getFBConfig(), GLX.GLX_RGBA_TYPE, shareWithHandle, direct);
        if ( 0 == temp_ctx ) {
            throw new GLException(getThreadName()+": Unable to create temp OpenGL context(1)");
        }
        if ( !glXMakeContextCurrent(display, drawable.getHandle(), drawableRead.getHandle(), temp_ctx) ) {
            throw new GLException(getThreadName()+": Error making temp context(1) current: display "+toHexString(display)+", context "+toHexString(temp_ctx)+", drawable "+drawable);
        }
        if( !setGLFunctionAvailability(true, 0, 0, CTX_PROFILE_COMPAT, false /* strictMatch */, null == sharedContext /* withinGLVersionsMapping */) ) { // use GL_VERSION
            glXReleaseContext(display); // release temp context
            GLX.glXDestroyContext(display, temp_ctx);
            throw new GLException("setGLFunctionAvailability !strictMatch failed.2");
        }
        glXReleaseContext(display); // release temp context
        if( createContextARBAvailable && !createContextARBTried ) {
            // is*Available calls are valid since setGLFunctionAvailability(..) was called
            final boolean isProcCreateContextAttribsARBAvailable = isFunctionAvailable("glXCreateContextAttribsARB");
            final boolean isExtARBCreateContextAvailable = isExtensionAvailable("GLX_ARB_create_context");
            if ( isProcCreateContextAttribsARBAvailable && isExtARBCreateContextAvailable ) {
                // initial ARB context creation
                contextHandle = createContextARB(shareWithHandle, direct);
                createContextARBTried=true;
                if (DEBUG) {
                    if( 0 != contextHandle ) {
                        System.err.println(getThreadName() + ": createImpl: OK (ARB, initial) share "+toHexString(shareWithHandle));
                    } else {
                        System.err.println(getThreadName() + ": createImpl: NOT OK (ARB, initial) - creation failed - share "+toHexString(shareWithHandle));
                    }
                }
            } else if( DEBUG ) {
                System.err.println(getThreadName() + ": createImpl: NOT OK (ARB, initial) - extension not available - share "+toHexString(shareWithHandle)+
                                   ", isProcCreateContextAttribsARBAvailable "+isProcCreateContextAttribsARBAvailable+
                                   ", isExtGLXARBCreateContextAvailable "+isExtARBCreateContextAvailable);
            }
        }
    } else {
        temp_ctx = 0;
    }

    if( 0 != contextHandle ) {
        if( 0 != temp_ctx ) {
            glXReleaseContext(display);
            GLX.glXDestroyContext(display, temp_ctx);
            if ( !glXMakeContextCurrent(display, drawable.getHandle(), drawableRead.getHandle(), contextHandle) ) {
                throw new GLException(getThreadName()+": Cannot make previous verified context current");
            }
        }
    } else {
        if( glp.isGL3() && createContextARBTried ) {
            // We shall not allow context creation >= GL3 w/ non ARB methods if ARB is used,
            // otherwise context of similar profile but different creation method may not be share-able.
            glXReleaseContext(display);
            GLX.glXDestroyContext(display, temp_ctx);
            throw new GLException(getThreadName()+": createImpl ARB n/a but required, profile > GL2 requested (OpenGL >= 3.1). Requested: "+glp+", current: "+getGLVersion());
        }
        if(DEBUG) {
            System.err.println(getThreadName()+": createImpl ARB not used[avail "+createContextARBAvailable+
                               ", tried "+createContextARBTried+"], fall back to !ARB context "+getGLVersion());
        }

        // continue with temp context
        contextHandle = temp_ctx;
        if ( !glXMakeContextCurrent(display, drawable.getHandle(), drawableRead.getHandle(), contextHandle) ) {
            glXReleaseContext(display);
            GLX.glXDestroyContext(display, temp_ctx);
            throw new GLException(getThreadName()+": Error making context(1) current: display "+toHexString(display)+", context "+toHexString(contextHandle)+", drawable "+drawable);
        }
        if (DEBUG) {
            System.err.println(getThreadName() + ": createImpl: OK (old-2) share "+toHexString(shareWithHandle));
        }
    }
    isDirect = GLX.glXIsDirect(display, contextHandle);
    if (DEBUG) {
        System.err.println(getThreadName() + ": createImpl: OK direct "+isDirect+"/"+direct);
    }

    return true;
  }

  @Override
  protected void makeCurrentImpl() throws GLException {
    final long dpy = drawable.getNativeSurface().getDisplayHandle();
    if ( !glXMakeContextCurrent(dpy, drawable.getHandle(), drawableRead.getHandle(), contextHandle) ) {
        throw new GLException("Error making context " + toHexString(contextHandle) +
                " current on Thread " + getThreadName() +
                " with display " + toHexString(dpy) +
                ", drawableWrite " + toHexString(drawable.getHandle()) +
                ", drawableRead "+ toHexString(drawableRead.getHandle()) +
                " - " + this);
    }
  }

  @Override
  protected void releaseImpl() throws GLException {
    final long display = drawable.getNativeSurface().getDisplayHandle();
    if ( !glXReleaseContext(display) ) {
        throw new GLException(getThreadName()+": Error freeing OpenGL context");
    }
  }

  @Override
  protected void destroyImpl() throws GLException {
    destroyContextARBImpl(contextHandle);
  }

  @Override
  protected void copyImpl(final GLContext source, final int mask) throws GLException {
    final long dst = getHandle();
    final long src = source.getHandle();
    final long display = drawable.getNativeSurface().getDisplayHandle();
    if (0 == display) {
      throw new GLException(getThreadName()+": Connection to X display not yet set up");
    }
    GLX.glXCopyContext(display, src, dst, mask);
    // Should check for X errors and raise GLException
  }

  /**
   * {@inheritDoc}
   * <p>
   * Ignoring {@code contextFQN}, using {@code GLX}+{@link AbstractGraphicsDevice#getUniqueID()}.
   * </p>
   */
  @Override
  protected final void updateGLXProcAddressTable(final String contextFQN, final GLDynamicLookupHelper dlh) {
    if( null == dlh ) {
        throw new GLException("No GLDynamicLookupHelper for "+this);
    }
    final AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration();
    final AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
    final String key = "GLX-"+adevice.getUniqueID();
    if (DEBUG) {
      System.err.println(getThreadName() + ": Initializing GLX extension address table: "+key);
    }
    ProcAddressTable table = null;
    synchronized(mappedContextTypeObjectLock) {
        table = mappedGLXProcAddress.get( key );
    }
    if(null != table) {
        glXExtProcAddressTable = (GLXExtProcAddressTable) table;
        if(DEBUG) {
            System.err.println(getThreadName() + ": GLContext GLX ProcAddressTable reusing key("+key+") -> "+toHexString(table.hashCode()));
        }
    } else {
        glXExtProcAddressTable = new GLXExtProcAddressTable(new GLProcAddressResolver());
        resetProcAddressTable(getGLXExtProcAddressTable(), dlh);
        synchronized(mappedContextTypeObjectLock) {
            mappedGLXProcAddress.put(key, getGLXExtProcAddressTable());
            if(DEBUG) {
                System.err.println(getThreadName() + ": GLContext GLX ProcAddressTable mapping key("+key+") -> "+toHexString(getGLXExtProcAddressTable().hashCode()));
            }
        }
    }
  }

  @Override
  protected final StringBuilder getPlatformExtensionsStringImpl() {
    final NativeSurface ns = drawable.getNativeSurface();
    final X11GraphicsDevice x11Device = (X11GraphicsDevice) ns.getGraphicsConfiguration().getScreen().getDevice();
    final StringBuilder sb = new StringBuilder();
    x11Device.lock();
    try{
        if (DEBUG) {
          System.err.println("GLX Version client "+ GLXUtil.getClientVersionNumber()+
                             ", server: "+ GLXUtil.getGLXServerVersionNumber(x11Device));
        }
        if(((X11GLXDrawableFactory)drawable.getFactoryImpl()).isGLXVersionGreaterEqualOneOne(x11Device)) {
            {
                final String ret = GLX.glXGetClientString(x11Device.getHandle(), GLX.GLX_EXTENSIONS);
                if (DEBUG) {
                  System.err.println("GLX extensions (glXGetClientString): " + ret);
                }
                sb.append(ret).append(" ");
            }
            {
                final String ret = GLX.glXQueryExtensionsString(x11Device.getHandle(), ns.getScreenIndex());
                if (DEBUG) {
                  System.err.println("GLX extensions (glXQueryExtensionsString): " + ret);
                }
                sb.append(ret).append(" ");
            }
            {
                final String ret = GLX.glXQueryServerString(x11Device.getHandle(), ns.getScreenIndex(), GLX.GLX_EXTENSIONS);
                if (DEBUG) {
                  System.err.println("GLX extensions (glXQueryServerString): " + ret);
                }
                sb.append(ret).append(" ");
            }
        }
    } finally {
        x11Device.unlock();
    }
    return sb;
  }

  @Override
  protected final Integer setSwapIntervalImpl2(final int interval) {
    if( !drawable.getChosenGLCapabilities().isOnscreen() ) {
        return null;
    }
    final long displayHandle = drawable.getNativeSurface().getDisplayHandle();
    if( 0 == hasSwapInterval ) {
        try {
            if ( isExtensionAvailable(GLXExtensions.GLX_EXT_swap_control) ) {
                hasSwapInterval = 1;
                if ( isExtensionAvailable(GLXExtensions.GLX_EXT_swap_control_tear) ) {
                    try {
                        final IntBuffer val = Buffers.newDirectIntBuffer(1);
                        GLX.glXQueryDrawable(displayHandle, drawable.getHandle(), GLX.GLX_LATE_SWAPS_TEAR_EXT, val);
                        if( 1 == val.get(0) ) {
                            hasSwapInterval =  2;
                            if(DEBUG) { System.err.println("X11GLXContext.setSwapInterval.2 using: "+GLXExtensions.GLX_EXT_swap_control_tear + ", " + GLXExtensions.GLX_EXT_swap_control_tear); }
                        } else if(DEBUG) {
                            System.err.println("X11GLXContext.setSwapInterval.2 n/a: "+GLXExtensions.GLX_EXT_swap_control_tear+", query: "+val.get(0));
                        }
                    } catch (final Throwable t) { if(DEBUG) { ExceptionUtils.dumpThrowable("", t); } }
                }
                if(DEBUG) {
                    if( 1 == hasSwapInterval ) {
                        System.err.println("X11GLXContext.setSwapInterval.1 using: "+GLXExtensions.GLX_EXT_swap_control);
                    }
                }
            } else if ( isExtensionAvailable(GLXExtensions.GLX_SGI_swap_control) ) {
                hasSwapInterval =  3;
                if(DEBUG) { System.err.println("X11GLXContext.setSwapInterval.3 using: "+GLXExtensions.GLX_SGI_swap_control); }
            } else {
                hasSwapInterval = -1;
                if(DEBUG) { System.err.println("X11GLXContext.setSwapInterval.0 N/A"); }
            }
        } catch (final Throwable t) { hasSwapInterval=-1; if(DEBUG) { ExceptionUtils.dumpThrowable("", t); } }
    }
    if (3 == hasSwapInterval) {
        final int useInterval;
        if( 0 > interval ) {
            useInterval = Math.abs(interval);
        } else {
            useInterval = interval;
        }
        try {
            final GLXExt glXExt = getGLXExt();
            if( 0 == glXExt.glXSwapIntervalSGI(useInterval) ) {
                return Integer.valueOf(useInterval);
            }
        } catch (final Throwable t) { hasSwapInterval=-1; if(DEBUG) { ExceptionUtils.dumpThrowable("", t); } }
    } else if ( 0 < hasSwapInterval ) { // 2 || 1
        final int useInterval;
        if( 1 == hasSwapInterval && 0 > interval ) {
            useInterval = Math.abs(interval);
        } else {
            useInterval = interval;
        }
        try {
            GLX.glXSwapIntervalEXT(displayHandle, drawable.getHandle(), useInterval);
            return Integer.valueOf(useInterval);
        } catch (final Throwable t) { hasSwapInterval=-1; if(DEBUG) { ExceptionUtils.dumpThrowable("", t); } }
    }
    return null;
  }

  private final int initSwapGroupImpl(final GLXExt glXExt) {
      if(0==hasSwapGroupNV) {
        try {
            hasSwapGroupNV = glXExt.isExtensionAvailable(GLXExtensions.GLX_NV_swap_group)?1:-1;
        } catch (final Throwable t) { hasSwapGroupNV=1; }
        if(DEBUG) {
            System.err.println("initSwapGroupImpl: "+GLXExtensions.GLX_NV_swap_group+": "+hasSwapGroupNV);
        }
      }
      return hasSwapGroupNV;
  }

  @Override
  protected final boolean queryMaxSwapGroupsImpl(final int[] maxGroups, final int maxGroups_offset,
                                                 final int[] maxBarriers, final int maxBarriers_offset) {
      boolean res = false;
      final GLXExt glXExt = getGLXExt();
      if (initSwapGroupImpl(glXExt)>0) {
        final NativeSurface ns = drawable.getNativeSurface();
        try {
            final IntBuffer maxGroupsNIO = Buffers.newDirectIntBuffer(maxGroups.length - maxGroups_offset);
            final IntBuffer maxBarriersNIO = Buffers.newDirectIntBuffer(maxBarriers.length - maxBarriers_offset);

            if( glXExt.glXQueryMaxSwapGroupsNV(ns.getDisplayHandle(), ns.getScreenIndex(),
                                               maxGroupsNIO, maxBarriersNIO) ) {
                maxGroupsNIO.get(maxGroups, maxGroups_offset, maxGroupsNIO.remaining());
                maxBarriersNIO.get(maxGroups, maxGroups_offset, maxBarriersNIO.remaining());
                res = true;
            }
        } catch (final Throwable t) { hasSwapGroupNV=-1; }
      }
      return res;
  }

  @Override
  protected final boolean joinSwapGroupImpl(final int group) {
      boolean res = false;
      final GLXExt glXExt = getGLXExt();
      if (initSwapGroupImpl(glXExt)>0) {
        try {
            if( glXExt.glXJoinSwapGroupNV(drawable.getNativeSurface().getDisplayHandle(), drawable.getHandle(), group) ) {
                currentSwapGroup = group;
                res = true;
            }
        } catch (final Throwable t) { hasSwapGroupNV=-1; }
      }
      return res;
  }

  @Override
  protected final boolean bindSwapBarrierImpl(final int group, final int barrier) {
      boolean res = false;
      final GLXExt glXExt = getGLXExt();
      if (initSwapGroupImpl(glXExt)>0) {
        try {
            if( glXExt.glXBindSwapBarrierNV(drawable.getNativeSurface().getDisplayHandle(), group, barrier) ) {
                res = true;
            }
        } catch (final Throwable t) { hasSwapGroupNV=-1; }
      }
      return res;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
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

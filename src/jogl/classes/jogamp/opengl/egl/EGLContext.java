/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2011 JogAmp Community. All rights reserved.
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
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package jogamp.opengl.egl;

import java.nio.IntBuffer;
import java.util.Map;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.egl.EGLExtImpl;
import jogamp.opengl.egl.EGLExtProcAddressTable;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.GLRendererQuirks;
import com.jogamp.opengl.egl.EGL;
import com.jogamp.opengl.egl.EGLExt;

public class EGLContext extends GLContextImpl {
    // Table that holds the addresses of the native C-language entry points for
    // EGL extension functions.
    private EGLExtProcAddressTable eglExtProcAddressTable;
    private EGLExtImpl eglExtImpl;

    static final int CTX_PROFILE_COMPAT  = GLContext.CTX_PROFILE_COMPAT;
    static final int CTX_PROFILE_CORE    = GLContext.CTX_PROFILE_CORE;
    static final int CTX_PROFILE_ES      = GLContext.CTX_PROFILE_ES;

    public static String getGLProfile(final int major, final int minor, final int ctp) throws GLException {
        return GLContext.getGLProfile(major, minor, ctp);
    }

    EGLContext(final GLDrawableImpl drawable,
               final GLContext shareWith) {
        super(drawable, shareWith);
    }

    @Override
    protected void resetStates(final boolean isInit) {
        eglExtProcAddressTable = null;
        eglExtImpl = null;
        super.resetStates(isInit);
    }

    @Override
    public Object getPlatformGLExtensions() {
      return getEGLExt();
    }

    public final EGLExt getEGLExt() {
      return eglExtImpl;
    }

    @Override
    public final ProcAddressTable getPlatformExtProcAddressTable() {
        return eglExtProcAddressTable;
    }

    @Override
    protected Map<String, String> getFunctionNameMap() { return null; }

    @Override
    protected Map<String, String> getExtensionNameMap() { return null; }

    @Override
    public final boolean isGLReadDrawableAvailable() {
        return true;
    }

    @Override
    protected void makeCurrentImpl() throws GLException {
        final long dpy = drawable.getNativeSurface().getDisplayHandle();
        if ( !EGL.eglMakeCurrent(dpy, drawable.getHandle(), drawableRead.getHandle(), contextHandle) ) {
            throw new GLException("Error making context " + toHexString(contextHandle) +
                    " current on Thread " + getThreadName() +
                    " with display " + toHexString(dpy) +
                    ", drawableWrite " + toHexString(drawable.getHandle()) +
                    ", drawableRead "+ toHexString(drawableRead.getHandle()) +
                    " - Error code " + toHexString(EGL.eglGetError()) + ", " + this);
        }
    }

    @Override
    protected void releaseImpl() throws GLException {
      if (!EGL.eglMakeCurrent(drawable.getNativeSurface().getDisplayHandle(), EGL.EGL_NO_SURFACE, EGL.EGL_NO_SURFACE, EGL.EGL_NO_CONTEXT)) {
            throw new GLException("Error freeing OpenGL context " + toHexString(contextHandle) +
                                  ": error code " + toHexString(EGL.eglGetError()));
      }
    }

    @Override
    protected void destroyImpl() throws GLException {
        destroyContextARBImpl(contextHandle);
    }

    @Override
    protected void destroyContextARBImpl(final long _context) {
        if (!EGL.eglDestroyContext(drawable.getNativeSurface().getDisplayHandle(), _context)) {
            final int eglError = EGL.eglGetError();
            if(EGL.EGL_SUCCESS != eglError) { /* oops, Mesa EGL impl. may return false, but has no EGL error */
                throw new GLException("Error destroying OpenGL context " + toHexString(_context) +
                        ": error code " + toHexString(eglError));
            }
        }
    }

    private static final int ctx_attribs_idx_major = 0;
    private static final int ctx_attribs_rom[] = {
        /*  0 */ EGLExt.EGL_CONTEXT_MAJOR_VERSION_KHR,  0,            // alias of EGL.EGL_CONTEXT_CLIENT_VERSION
        /*  2 */ EGL.EGL_NONE,                          EGL.EGL_NONE, // EGLExt.EGL_CONTEXT_MINOR_VERSION_KHR
        /*  4 */ EGL.EGL_NONE,                          EGL.EGL_NONE, // EGLExt.EGL_CONTEXT_FLAGS_KHR
        /*  6 */ EGL.EGL_NONE,                          EGL.EGL_NONE, // EGLExt.EGL_CONTEXT_OPENGL_PROFILE_MASK_KHR
        /*  8 */ EGL.EGL_NONE,                          EGL.EGL_NONE, // EGLExt.EGL_CONTEXT_OPENGL_RESET_NOTIFICATION_STRATEGY_KHR
        /* 10 */ EGL.EGL_NONE
    };

    @Override
    protected long createContextARBImpl(final long share, final boolean direct, final int ctp, final int reqMajor, final int reqMinor) {
        final EGLGraphicsConfiguration config = (EGLGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration();
        final EGLGraphicsDevice device = (EGLGraphicsDevice) config.getScreen().getDevice();
        final long eglDisplay = device.getHandle();
        final long eglConfig = config.getNativeConfig();
        final EGLDrawableFactory factory = (EGLDrawableFactory) drawable.getFactoryImpl();

        final boolean hasFullOpenGLAPISupport = factory.hasOpenGLDesktopSupport();
        final boolean useKHRCreateContext = factory.hasDefaultDeviceKHRCreateContext();
        final boolean ctDesktopGL = 0 == ( CTX_PROFILE_ES & ctp );
        final boolean ctBwdCompat = 0 != ( CTX_PROFILE_COMPAT & ctp ) ;
        final boolean ctFwdCompat = 0 != ( CTX_OPTION_FORWARD & ctp ) ;
        final boolean ctDebug     = 0 != ( CTX_OPTION_DEBUG & ctp ) ;

        if(DEBUG) {
            System.err.println(getThreadName() + ": EGLContext.createContextARBImpl: Start "+getGLVersion(reqMajor, reqMinor, ctp, "@creation")
                                               + ", useKHRCreateContext "+useKHRCreateContext
                                               + ", OpenGL API Support "+hasFullOpenGLAPISupport
                                               + ", device "+device);
        }
        if ( 0 == eglDisplay ) {
            throw new GLException("Error: attempted to create an OpenGL context without a display connection");
        }
        if ( 0 == eglConfig ) {
            throw new GLException("Error: attempted to create an OpenGL context without a graphics configuration");
        }

        /**
         * It has been experienced w/ Mesa 10.3.2 (EGL 1.4/Gallium)
         * that even though initial OpenGL context can be created w/o 'EGL_KHR_create_context',
         * switching the API via 'eglBindAPI(EGL_OpenGL_API)' the latter 'eglCreateContext(..)' fails w/ EGL_BAD_ACCESS.
         * Hence we require both: OpenGL API support _and_  'EGL_KHR_create_context'.
         *
         * FIXME: Evaluate this issue in more detail!
         *
         * FIXME: Utilization of eglBindAPI(..) must be re-evaluated in case we mix ES w/ OpenGL, see EGL 1.4 spec.
         *        This is due to new semantics, i.e. API is bound on a per thread base,
         *        hence it must be switched before makeCurrent w/ different APIs, see:
         *           eglWaitClient();
         */
        if( ctDesktopGL && !hasFullOpenGLAPISupport ) {
            if(DEBUG) {
                System.err.println(getThreadName() + ": EGLContext.createContextARBImpl: DesktopGL not avail "+getGLVersion(reqMajor, reqMinor, ctp, "@creation"));
            }
            return 0; // n/a
        }

        try {
            if( hasFullOpenGLAPISupport && device.getEGLVersion().compareTo(Version1_2) >= 0 ) {
                EGL.eglWaitClient(); // EGL >= 1.2
            }
            if( !EGL.eglBindAPI( ctDesktopGL ? EGL.EGL_OPENGL_API : EGL.EGL_OPENGL_ES_API) ) {
                throw new GLException("Caught: eglBindAPI to "+(ctDesktopGL ? "ES" : "GL")+" failed , error "+toHexString(EGL.eglGetError())+" - "+getGLVersion(reqMajor, reqMinor, ctp, "@creation"));
            }
        } catch (final GLException glex) {
            if (DEBUG) {
                ExceptionUtils.dumpThrowable("", glex);
            }
        }

        final int useMajor;
        if( reqMajor >= 3 &&
            GLRendererQuirks.existStickyDeviceQuirk( GLDrawableFactory.getEGLFactory().getDefaultDevice(), GLRendererQuirks.GLES3ViaEGLES2Config) ) {
            useMajor = 2;
        } else {
            useMajor = reqMajor;
        }

        final IntBuffer attribs = Buffers.newDirectIntBuffer(ctx_attribs_rom);
        if( useKHRCreateContext ) {
            attribs.put(ctx_attribs_idx_major + 1, useMajor);

            int index = ctx_attribs_idx_major + 2;

            if( reqMinor >= 0 ) {
                attribs.put(index + 0, EGLExt.EGL_CONTEXT_MINOR_VERSION_KHR);
                attribs.put(index + 1, reqMinor);
                index += 2;
            }

            if( ctDesktopGL && ( useMajor > 3 || useMajor == 3 && reqMinor >= 2 ) ) {
                attribs.put(index + 0, EGLExt.EGL_CONTEXT_OPENGL_PROFILE_MASK_KHR);
                if( ctBwdCompat ) {
                    attribs.put(index + 1, EGLExt.EGL_CONTEXT_OPENGL_COMPATIBILITY_PROFILE_BIT_KHR);
                } else {
                    attribs.put(index + 1, EGLExt.EGL_CONTEXT_OPENGL_CORE_PROFILE_BIT_KHR);
                }
                index += 2;
            }
            int flags = 0;
            if( ctDesktopGL && useMajor >= 3 && !ctBwdCompat && ctFwdCompat ) {
                flags |= EGLExt.EGL_CONTEXT_OPENGL_FORWARD_COMPATIBLE_BIT_KHR;
            }
            if( ctDebug ) {
                flags |= EGLExt.EGL_CONTEXT_OPENGL_DEBUG_BIT_KHR;
            }
            // TODO: flags |= EGL_CONTEXT_OPENGL_ROBUST_ACCESS_BIT_KHR
            if( 0 != flags ) {
                attribs.put(index + 0, EGLExt.EGL_CONTEXT_FLAGS_KHR);
                attribs.put(index + 1, flags);
                index += 2;
            }
            if(DEBUG) {
                System.err.println(getThreadName() + ": EGLContext.createContextARBImpl: attrs.1: major "+useMajor+", flags "+toHexString(flags)+", index "+index);
            }
        } else {
            attribs.put(ctx_attribs_idx_major + 1, useMajor);
            if(DEBUG) {
                System.err.println(getThreadName() + ": EGLContext.createContextARBImpl: attrs.2: major "+useMajor);
            }
        }

        long ctx=0;
        try {
            ctx = EGL.eglCreateContext(eglDisplay, eglConfig, share, attribs);
        } catch (final RuntimeException re) {
            if(DEBUG) {
                System.err.println(getThreadName()+": Info: EGLContext.createContextARBImpl glXCreateContextAttribsARB failed with "+getGLVersion(reqMajor, reqMinor, ctp, "@creation"));
                ExceptionUtils.dumpThrowable("", re);
            }
        }

        if(0!=ctx) {
            if (!EGL.eglMakeCurrent(eglDisplay, drawable.getHandle(), drawableRead.getHandle(), ctx)) {
                if(DEBUG) {
                    System.err.println(getThreadName()+": EGLContext.createContextARBImpl couldn't make current "+getGLVersion(reqMajor, reqMinor, ctp, "@creation")+" - error "+toHexString(EGL.eglGetError()));
                }
                // release & destroy
                EGL.eglMakeCurrent(eglDisplay, EGL.EGL_NO_SURFACE, EGL.EGL_NO_SURFACE, EGL.EGL_NO_CONTEXT);
                EGL.eglDestroyContext(eglDisplay, ctx);
                ctx = 0;
            } else if (DEBUG) {
                System.err.println(getThreadName() + ": EGLContext.createContextARBImpl: OK "+getGLVersion(reqMajor, reqMinor, ctp, "@creation")+", share "+share+", direct "+direct);
            }
        } else if (DEBUG) {
            System.err.println(getThreadName() + ": EGLContext.createContextARBImpl: NO "+getGLVersion(reqMajor, reqMinor, ctp, "@creation")+" - error "+toHexString(EGL.eglGetError()));
        }

        return ctx;
    }

    @Override
    protected boolean createImpl(final long shareWithHandle) throws GLException {
        final EGLGraphicsConfiguration config = (EGLGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration();
        final AbstractGraphicsDevice device = config.getScreen().getDevice();
        final GLCapabilitiesImmutable glCaps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
        final GLProfile glp = glCaps.getGLProfile();
        final boolean createContextARBAvailable = isCreateContextARBAvail(device);
        if(DEBUG) {
            System.err.println(getThreadName() + ": EGLContext.createImpl: START "+glCaps+", share "+toHexString(shareWithHandle));
            System.err.println(getThreadName() + ": Use ARB[avail["+getCreateContextARBAvailStr(device)+
                    "] -> "+createContextARBAvailable+"]]");
        }
        if( createContextARBAvailable ) {
            contextHandle = createContextARB(shareWithHandle, true);
            if (DEBUG) {
                if( 0 != contextHandle ) {
                    System.err.println(getThreadName() + ": createImpl: OK (ARB) on eglDevice "+device+
                            ", eglConfig "+config+", "+glp+", shareWith "+toHexString(shareWithHandle)+", error "+toHexString(EGL.eglGetError()));
                } else {
                    System.err.println(getThreadName() + ": createImpl: NOT OK (ARB) - creation failed on eglDevice "+device+
                            ", eglConfig "+config+", "+glp+", shareWith "+toHexString(shareWithHandle)+", error "+toHexString(EGL.eglGetError()));
                }
            }
        } else {
            contextHandle = 0;
        }
        if( 0 == contextHandle ) {
            if( !glp.isGLES() ) {
                throw new GLException(getThreadName()+": Unable to create desktop OpenGL context(ARB n/a) on eglDevice "+device+
                        ", eglConfig "+config+", "+glp+", shareWith "+toHexString(shareWithHandle)+", error "+toHexString(EGL.eglGetError()));
            }
            final int[] reqMajorCTP = new int[] { 0, 0 };
            GLContext.getRequestMajorAndCompat(glp, reqMajorCTP);
            reqMajorCTP[1] |= getContextCreationFlags();

            contextHandle = createContextARBImpl(shareWithHandle, true, reqMajorCTP[1], reqMajorCTP[0], 0);
            if( 0 == contextHandle ) {
                throw new GLException(getThreadName()+": Unable to create ES OpenGL context on eglDevice "+device+
                                   ", eglConfig "+config+", "+glp+", shareWith "+toHexString(shareWithHandle)+", error "+toHexString(EGL.eglGetError()));
            }
            if( !setGLFunctionAvailability(true, reqMajorCTP[0], 0, reqMajorCTP[1], false /* strictMatch */, false /* withinGLVersionsMapping */) ) {
                EGL.eglMakeCurrent(drawable.getNativeSurface().getDisplayHandle(), EGL.EGL_NO_SURFACE, EGL.EGL_NO_SURFACE, EGL.EGL_NO_CONTEXT);
                EGL.eglDestroyContext(drawable.getNativeSurface().getDisplayHandle(), contextHandle);
                contextHandle = 0;
                throw new GLException("setGLFunctionAvailability !strictMatch failed");
            }
        }
        if (DEBUG) {
            System.err.println(getThreadName() + ": createImpl: Created OpenGL context 0x" +
                               Long.toHexString(contextHandle) +
                               ",\n\twrite surface 0x" + Long.toHexString(drawable.getHandle()) +
                               ",\n\tread  surface 0x" + Long.toHexString(drawableRead.getHandle())+
                               ",\n\t"+this+
                               ",\n\tsharing with 0x" + Long.toHexString(shareWithHandle));
        }
        return true;
    }

    @Override
    protected final void updateGLXProcAddressTable(final String contextFQN, final GLDynamicLookupHelper dlh) {
        if( null == dlh ) {
            throw new GLException("No GLDynamicLookupHelper for "+this);
        }
        final AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration();
        final AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
        final String key = "EGL-"+adevice.getUniqueID();
        // final String key = contextFQN;
        if (DEBUG) {
          System.err.println(getThreadName() + ": Initializing EGLextension address table: "+key);
        }
        ProcAddressTable table = null;
        synchronized(mappedContextTypeObjectLock) {
            table = mappedGLXProcAddress.get( key );
        }
        if(null != table) {
            eglExtProcAddressTable = (EGLExtProcAddressTable) table;
            if(DEBUG) {
                System.err.println(getThreadName() + ": GLContext EGL ProcAddressTable reusing key("+key+") -> "+toHexString(table.hashCode()));
            }
            if( null == eglExtImpl || eglExtImpl.getProcAdressTable() != eglExtProcAddressTable ) {
                eglExtImpl = new EGLExtImpl(this, eglExtProcAddressTable);
            }
        } else {
            eglExtProcAddressTable = new EGLExtProcAddressTable(new GLProcAddressResolver());
            resetProcAddressTable(eglExtProcAddressTable, dlh);
            synchronized(mappedContextTypeObjectLock) {
                mappedGLXProcAddress.put(key, eglExtProcAddressTable);
                if(DEBUG) {
                    System.err.println(getThreadName() + ": GLContext EGL ProcAddressTable mapping key("+key+") -> "+toHexString(eglExtProcAddressTable.hashCode()));
                }
            }
            eglExtImpl = new EGLExtImpl(this, eglExtProcAddressTable);
        }
    }

    @Override
    protected final StringBuilder getPlatformExtensionsStringImpl() {
        final EGLGraphicsDevice device = (EGLGraphicsDevice) drawable.getNativeSurface().getGraphicsConfiguration().getScreen().getDevice();
        return getPlatformExtensionsStringImpl(device);
    }
    final static StringBuilder getPlatformExtensionsStringImpl(final EGLGraphicsDevice device) {
        final StringBuilder sb = new StringBuilder();
        device.lock();
        try{
            final long handle = device.getHandle();
            if (DEBUG) {
                System.err.println("EGL PlatformExtensions: Device "+device);
                EGLDrawableFactory.dumpEGLInfo("EGL PlatformExtensions: ", handle);
            }
            if( device.getEGLVersion().compareTo(Version1_5) >= 0 ) {
                final String ret = EGL.eglQueryString(EGL.EGL_NO_DISPLAY, EGL.EGL_EXTENSIONS);
                if (DEBUG) {
                    System.err.println("EGL extensions (Client): " + ret);
                }
                sb.append(ret).append(" ");
            }
            if( 0 != handle ) {
                final String ret = EGL.eglQueryString(handle, EGL.EGL_EXTENSIONS);
                if (DEBUG) {
                    System.err.println("EGL extensions (Server): " + ret);
                }
                sb.append(ret).append(" ");
            }
        } finally {
            device.unlock();
        }
        return sb;
    }

    @Override
    protected final Integer setSwapIntervalImpl2(final int interval) {
        if( !drawable.getChosenGLCapabilities().isOnscreen() ||
            hasRendererQuirk(GLRendererQuirks.NoSetSwapInterval) ) {
            return null;
        }
        final int useInterval;
        if( 0 > interval ) {
            useInterval = Math.abs(interval);
        } else {
            useInterval = interval;
        }
        if( EGL.eglSwapInterval(drawable.getNativeSurface().getDisplayHandle(), useInterval) ) {
            return Integer.valueOf(useInterval);
        }
        return null;
    }

    static long eglGetProcAddress(final long eglGetProcAddressHandle, final String procname)
    {
        if (0 == eglGetProcAddressHandle) {
            throw new GLException("Passed null pointer for method \"eglGetProcAddress\"");
        }
        return dispatch_eglGetProcAddress0(procname, eglGetProcAddressHandle);
    }
    /** Entry point to C language function: <code> __EGLFuncPtr eglGetProcAddress(const char *  procname) </code> <br>Part of <code>EGL_VERSION_1_X</code>   */
    static private native long dispatch_eglGetProcAddress0(String procname, long procAddress);

    //
    // Accessible ..
    //

    /* pp */ static final boolean isGLES1(final int majorVersion, final int ctxOptions) {
        return 0 != ( ctxOptions & GLContext.CTX_PROFILE_ES ) && majorVersion == 1 ;
    }
    /* pp */ static final boolean isGLES2ES3(final int majorVersion, final int ctxOptions) {
        if( 0 != ( ctxOptions & CTX_PROFILE_ES ) ) {
            return 2 == majorVersion || 3 == majorVersion;
        } else {
            return false;
        }
    }
    /* pp */ static final boolean isGLDesktop(final int ctxOptions) {
        return 0 != (ctxOptions & (CTX_PROFILE_COMPAT|CTX_PROFILE_CORE));
    }
    protected static StringBuilder getGLProfile(final StringBuilder sb, final int ctp) {
        return GLContext.getGLProfile(sb, ctp);
    }
    /* pp */ int getContextOptions() { return ctxOptions; }
    protected static void remapAvailableGLVersions(final AbstractGraphicsDevice fromDevice, final AbstractGraphicsDevice toDevice) {
        GLContextImpl.remapAvailableGLVersions(fromDevice, toDevice);
    }
    protected static synchronized void setMappedGLVersionListener(final MappedGLVersionListener mvl) {
        GLContextImpl.setMappedGLVersionListener(mvl);
    }

    protected static String getGLVersion(final int major, final int minor, final int ctp, final String gl_version) {
        return GLContext.getGLVersion(major, minor, ctp, gl_version);
    }

    protected static String toHexString(final int hex) {
        return GLContext.toHexString(hex);
    }
    protected static String toHexString(final long hex) {
        return GLContext.toHexString(hex);
    }

    //----------------------------------------------------------------------
    // Currently unimplemented stuff
    //

    @Override
    protected void copyImpl(final GLContext source, final int mask) throws GLException {
        throw new GLException("Not yet implemented");
    }
}

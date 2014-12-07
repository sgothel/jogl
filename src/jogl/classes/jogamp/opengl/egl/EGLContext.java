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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.egl.EGL;
import jogamp.opengl.egl.EGLExt;
import jogamp.opengl.egl.EGLExtImpl;
import jogamp.opengl.egl.EGLExtProcAddressTable;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.GLRendererQuirks;

public class EGLContext extends GLContextImpl {
    private EGLExt _eglExt;
    // Table that holds the addresses of the native C-language entry points for
    // EGL extension functions.
    private EGLExtProcAddressTable eglExtProcAddressTable;

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
        // no inner state _eglExt = null;
        super.resetStates(isInit);
    }

    @Override
    public Object getPlatformGLExtensions() {
      return getEGLExt();
    }

    public EGLExt getEGLExt() {
      if (_eglExt == null) {
        _eglExt = new EGLExtImpl(this);
      }
      return _eglExt;
    }

    @Override
    public final ProcAddressTable getPlatformExtProcAddressTable() {
        return eglExtProcAddressTable;
    }

    public final EGLExtProcAddressTable getEGLExtProcAddressTable() {
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
        if (EGL.eglGetCurrentContext() != contextHandle) {
            final long dpy = drawable.getNativeSurface().getDisplayHandle();
            if (!EGL.eglMakeCurrent(dpy, drawable.getHandle(), drawableRead.getHandle(), contextHandle)) {
                throw new GLException("Error making context " + toHexString(contextHandle) +
                                      " current on Thread " + getThreadName() +
                                      " with display " + toHexString(dpy) +
                                      ", drawableWrite " + toHexString(drawable.getHandle()) +
                                      ", drawableRead "+ toHexString(drawableRead.getHandle()) +
                                      " - Error code " + toHexString(EGL.eglGetError()) + ", " + this);
            }
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

        final boolean useKHRCreateContext = !GLProfile.disableOpenGLARBContext && factory.hasDefaultDeviceKHRCreateContext();
        final boolean ctDesktopGL = 0 == ( GLContext.CTX_PROFILE_ES & ctp );
        final boolean ctBwdCompat = 0 != ( CTX_PROFILE_COMPAT & ctp ) ;
        final boolean ctFwdCompat = 0 != ( CTX_OPTION_FORWARD & ctp ) ;
        final boolean ctDebug     = 0 != ( CTX_OPTION_DEBUG & ctp ) ;

        if(DEBUG) {
            System.err.println(getThreadName() + ": EGLContext.createContextARBImpl: Start "+getGLVersion(reqMajor, reqMinor, ctp, "@creation")
                                               + ", useKHRCreateContext "+useKHRCreateContext
                                               + ", device "+device);
        }
        if ( 0 == eglDisplay ) {
            throw new GLException("Error: attempted to create an OpenGL context without a display connection");
        }
        if ( 0 == eglConfig ) {
            throw new GLException("Error: attempted to create an OpenGL context without a graphics configuration");
        }

        if( !useKHRCreateContext && ctDesktopGL ) {
            if(DEBUG) {
                System.err.println(getThreadName() + ": EGLContext.createContextARBImpl: DesktopGL not avail "+getGLVersion(reqMajor, reqMinor, ctp, "@creation"));
            }
            return 0; // n/a
        }
        try {
            // might be unavailable on EGL < 1.2
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

            /** if( ctDesktopGL && reqMinor >= 0 ) { // FIXME: No minor version probing for ES currently!
                attribs.put(index + 0, EGLExt.EGL_CONTEXT_MINOR_VERSION_KHR);
                attribs.put(index + 1, reqMinor);
                index += 2;
            } */

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
        final boolean availableGLVersionsSet = GLContext.getAvailableGLVersionsSet(device);

        if( !GLProfile.disableOpenGLARBContext && availableGLVersionsSet ) {
            contextHandle = createContextARB(shareWithHandle, true);
            if( 0 == contextHandle ) {
                throw new GLException(getThreadName()+": Unable to create temp OpenGL context(0) on eglDevice "+device+
                                   ", eglConfig "+config+", "+drawable.getGLProfile()+", shareWith "+toHexString(shareWithHandle)+", error "+toHexString(EGL.eglGetError()));
            }
        } else {
            final GLProfile glProfile = drawable.getGLProfile();
            final int reqMajor;
            if ( glProfile.usesNativeGLES3() ) {
                reqMajor = 3;
            } else if ( glProfile.usesNativeGLES2() ) {
                reqMajor = 2;
            } else if ( glProfile.usesNativeGLES1() ) {
                reqMajor = 1;
            } else {
                throw new GLException("Error creating OpenGL context - invalid GLProfile: "+glProfile);
            }
            final int ctp = GLContext.CTX_PROFILE_ES | getContextCreationFlags();
            contextHandle = createContextARBImpl(shareWithHandle, true, ctp, reqMajor, 0);
            if( 0 == contextHandle ) {
                throw new GLException(getThreadName()+": Unable to create temp OpenGL context(1) on eglDevice "+device+
                                   ", eglConfig "+config+", "+drawable.getGLProfile()+", shareWith "+toHexString(shareWithHandle)+", error "+toHexString(EGL.eglGetError()));
            }
            if( !setGLFunctionAvailability(true, reqMajor, 0, ctp, false /* strictMatch */, false /* withinGLVersionsMapping */) ) {
                EGL.eglMakeCurrent(drawable.getNativeSurface().getDisplayHandle(), EGL.EGL_NO_SURFACE, EGL.EGL_NO_SURFACE, EGL.EGL_NO_CONTEXT);
                EGL.eglDestroyContext(drawable.getNativeSurface().getDisplayHandle(), contextHandle);
                contextHandle = 0;
                throw new InternalError("setGLFunctionAvailability !strictMatch failed");
            }
        }
        if (DEBUG) {
            System.err.println(getThreadName() + ": EGLContext.createImpl: Created OpenGL context 0x" +
                               Long.toHexString(contextHandle) +
                               ",\n\twrite surface 0x" + Long.toHexString(drawable.getHandle()) +
                               ",\n\tread  surface 0x" + Long.toHexString(drawableRead.getHandle())+
                               ",\n\t"+this+
                               ",\n\tsharing with 0x" + Long.toHexString(shareWithHandle));
        }
        return true;
    }

    @Override
    protected final void updateGLXProcAddressTable() {
        final AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration();
        final AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
        final String key = "EGL-"+adevice.getUniqueID();
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
        } else {
            eglExtProcAddressTable = new EGLExtProcAddressTable(new GLProcAddressResolver());
            resetProcAddressTable(getEGLExtProcAddressTable());
            synchronized(mappedContextTypeObjectLock) {
                mappedGLXProcAddress.put(key, getEGLExtProcAddressTable());
                if(DEBUG) {
                    System.err.println(getThreadName() + ": GLContext EGL ProcAddressTable mapping key("+key+") -> "+toHexString(getEGLExtProcAddressTable().hashCode()));
                }
            }
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
    protected boolean setSwapIntervalImpl(final int interval) {
        if( hasRendererQuirk(GLRendererQuirks.NoSetSwapInterval) ) {
            return false;
        }
        return EGL.eglSwapInterval(drawable.getNativeSurface().getDisplayHandle(), interval);
    }

    //
    // Accessible ..
    //

    /* pp */ void mapCurrentAvailableGLVersion(final AbstractGraphicsDevice device) {
        mapStaticGLVersion(device, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
    }
    /* pp */ int getContextOptions() { return ctxOptions; }
    /* pp */ static void mapStaticGLESVersion(final AbstractGraphicsDevice device, final GLCapabilitiesImmutable caps) {
        final GLProfile glp = caps.getGLProfile();
        final int[] reqMajorCTP = new int[2];
        GLContext.getRequestMajorAndCompat(glp, reqMajorCTP);
        if( glp.isGLES() ) {
            if( reqMajorCTP[0] >= 3 ) {
                reqMajorCTP[1] |= GLContext.CTX_IMPL_ES3_COMPAT | GLContext.CTX_IMPL_ES2_COMPAT | GLContext.CTX_IMPL_FBO ;
            } else if( reqMajorCTP[0] >= 2 ) {
                reqMajorCTP[1] |= GLContext.CTX_IMPL_ES2_COMPAT | GLContext.CTX_IMPL_FBO ;
            }
        }
        if( !caps.getHardwareAccelerated() ) {
            reqMajorCTP[1] |= GLContext.CTX_IMPL_ACCEL_SOFT;
        }
        mapStaticGLVersion(device, reqMajorCTP[0], 0, reqMajorCTP[1]);
    }
    /* pp */ static void mapStaticGLVersion(final AbstractGraphicsDevice device, final int major, final int minor, final int ctp) {
        if( 0 != ( ctp & GLContext.CTX_PROFILE_ES) ) {
            // ES1, ES2, ES3, ..
            mapStaticGLVersion(device, major /* reqMajor */, major, minor, ctp);
            if( 3 == major ) {
                // map ES2 -> ES3
                mapStaticGLVersion(device, 2 /* reqMajor */, major, minor, ctp);
            }
        }
    }
    private static void mapStaticGLVersion(final AbstractGraphicsDevice device, final int reqMajor, final int major, final int minor, final int ctp) {
        GLContext.mapAvailableGLVersion(device, reqMajor, GLContext.CTX_PROFILE_ES, major, minor, ctp);
        if(! ( device instanceof EGLGraphicsDevice ) ) {
            final EGLGraphicsDevice eglDevice = new EGLGraphicsDevice(device.getHandle(), EGL.EGL_NO_DISPLAY, device.getConnection(), device.getUnitID(), null);
            GLContext.mapAvailableGLVersion(eglDevice, reqMajor, GLContext.CTX_PROFILE_ES, major, minor, ctp);
        }
    }
    protected static String getGLVersion(final int major, final int minor, final int ctp, final String gl_version) {
        return GLContext.getGLVersion(major, minor, ctp, gl_version);
    }

    protected static boolean getAvailableGLVersionsSet(final AbstractGraphicsDevice device) {
        return GLContext.getAvailableGLVersionsSet(device);
    }
    protected static void setAvailableGLVersionsSet(final AbstractGraphicsDevice device, final boolean set) {
        GLContext.setAvailableGLVersionsSet(device, set);
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

    @Override
    public final ByteBuffer glAllocateMemoryNV(final int size, final float readFrequency, final float writeFrequency, final float priority) {
        throw new GLException("Should not call this");
    }

    @Override
    public final void glFreeMemoryNV(final ByteBuffer pointer) {
        throw new GLException("Should not call this");
    }
}

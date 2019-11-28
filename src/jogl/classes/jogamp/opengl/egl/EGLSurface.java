/**
 * Copyright 2014 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package jogamp.opengl.egl;

import java.nio.IntBuffer;

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLException;
import com.jogamp.common.nio.Buffers;
import com.jogamp.nativewindow.GenericUpstreamSurfacelessHook;
import com.jogamp.opengl.egl.EGL;

import jogamp.nativewindow.ProxySurfaceImpl;
import jogamp.nativewindow.WrappedSurface;
import jogamp.opengl.GLDrawableImpl;

/**
 * <pre>
 * EGLSurface [ is_a -> WrappedSurface -> ProxySurfaceImpl -> ProxySurface -> MutableSurface -> NativeSurface] has_a
 *     EGLUpstreamSurfaceHook [ is_a -> UpstreamSurfaceHook.MutableSize -> UpstreamSurfaceHook ] has_a
 *        NativeSurface (e.g. native [X11, WGL, ..] surface, or WrappedSurface, ..)
 * </pre>
 */
public class EGLSurface extends WrappedSurface {
    static boolean DEBUG = EGLDrawable.DEBUG || ProxySurface.DEBUG;

    public static EGLSurface get(final NativeSurface surface) {
        if(surface instanceof EGLSurface) {
            return (EGLSurface)surface;
        }
        return new EGLSurface(surface);
    }
    private EGLSurface(final NativeSurface surface) {
        super(surface.getGraphicsConfiguration(), EGL.EGL_NO_SURFACE, new EGLUpstreamSurfaceHook(surface), false /* tbd in UpstreamSurfaceHook */);
        if(EGLDrawableFactory.DEBUG) {
            System.err.println("EGLSurface.ctor().1: "+this);
            ProxySurfaceImpl.dumpHierarchy(System.err, this);
        }
    }

    public static EGLSurface createWrapped(final EGLGraphicsConfiguration cfg, final long handle,
                                           final UpstreamSurfaceHook upstream, final boolean ownsDevice) {
        return new EGLSurface(cfg, handle, upstream, ownsDevice);
    }
    private EGLSurface(final EGLGraphicsConfiguration cfg, final long handle,
                       final UpstreamSurfaceHook upstream, final boolean ownsDevice) {
        super(cfg, EGL.EGL_NO_SURFACE, new EGLUpstreamSurfaceHook(cfg, handle, upstream, ownsDevice), false /* tbd in UpstreamSurfaceHook */);
        if(EGLDrawableFactory.DEBUG) {
            System.err.println("EGLSurface.ctor().2: "+this);
            ProxySurfaceImpl.dumpHierarchy(System.err, this);
        }
    }

    public static EGLSurface createSurfaceless(final EGLGraphicsConfiguration cfg, final GenericUpstreamSurfacelessHook upstream, final boolean ownsDevice) {
        return new EGLSurface(cfg, upstream, ownsDevice);
    }
    private EGLSurface(final EGLGraphicsConfiguration cfg, final GenericUpstreamSurfacelessHook upstream, final boolean ownsDevice) {
        super(cfg, EGL.EGL_NO_SURFACE, upstream, ownsDevice);
        if(EGLDrawableFactory.DEBUG) {
            System.err.println("EGLSurface.ctor().3: "+this);
            ProxySurfaceImpl.dumpHierarchy(System.err, this);
        }
    }

    /**
     * Entry point to C language function:
     * <br><code> EGLSurface eglCreateWindowSurface(EGLDisplay dpy, EGLConfig config, EGLNativeWindowType win, const EGLint *  attrib_list) </code> <br>Part of <code>EGL_VERSION_1_0</code><br>
     * <br>or<br>
     * <p>
     * <code> EGLSurface eglCreatePlatformWindowSurface(EGLDisplay dpy, EGLConfig config, void *  native_window, const EGLAttrib *  attrib_list) </code> <br>Part of <code>EGL_VERSION_1_5</code>, <code>EGL_EXT_platform_base</code><br>Alias for: <code>eglCreatePlatformWindowSurfaceEXT</code>
     * </p>
     */
    public static long eglCreateWindowSurface(final long dpy, final long config, final long win)  {
        final int eglPlatform = EGLDisplayUtil.getEGLPlatformType(true);
        if( false && 0 != eglPlatform ) {
            // Not necessarily required and also not used in demo code
            // Also causes a crash using NVIDIA 430.40 on GNU/Linux X11
            // TODO: Forward bugreport to related parties?
            return EGL.eglCreatePlatformWindowSurface(dpy, config, win, null);
        } else {
            return EGL.eglCreateWindowSurface(dpy, config, win, null);
        }
    }

    public void setEGLSurfaceHandle() throws GLException {
        setSurfaceHandle( createEGLSurface() );
    }
    private long createEGLSurface() throws GLException {
        final EGLGraphicsConfiguration config = (EGLGraphicsConfiguration) getGraphicsConfiguration();
        final NativeSurface nativeSurface = getUpstreamSurface();
        final boolean isPBuffer = ((GLCapabilitiesImmutable) config.getChosenCapabilities()).isPBuffer();

        long eglSurface = createEGLSurface(isPBuffer, true /* useNativeSurface */, config, nativeSurface);
        if( DEBUG ) {
            System.err.println(getThreadName() + ": EGLSurface: EGL.eglCreateSurface.0: 0x"+Long.toHexString(eglSurface));
            ProxySurfaceImpl.dumpHierarchy(System.err, this);
        }

        if ( EGL.EGL_NO_SURFACE == eglSurface ) {
            final int eglError0 = EGL.eglGetError();
            if( EGL.EGL_BAD_NATIVE_WINDOW == eglError0 && !isPBuffer ) {
                // Try window handle if available and differs (Windows HDC / HWND).
                // ANGLE impl. required HWND on Windows.
                if( hasUniqueNativeWindowHandle(nativeSurface) ) {
                    eglSurface = createEGLSurface(isPBuffer, false /* useNativeSurface */, config, nativeSurface);
                    if( DEBUG ) {
                        System.err.println(getThreadName() + ": Info: Creation of window surface w/ surface handle failed: "+config+", error "+GLDrawableImpl.toHexString(eglError0)+", retry w/ windowHandle");
                        System.err.println(getThreadName() + ": EGLSurface: EGL.eglCreateSurface.1: 0x"+Long.toHexString(eglSurface));
                    }
                    if (EGL.EGL_NO_SURFACE == eglSurface) {
                        throw new GLException("Creation of window surface w/ window handle failed: "+config+", "+this+", error "+GLDrawableImpl.toHexString(EGL.eglGetError()));
                    }
                } else {
                    throw new GLException("Creation of window surface w/ surface handle failed (2): "+config+", "+this+", error "+GLDrawableImpl.toHexString(eglError0));
                }
            } else {
                throw new GLException("Creation of window surface w/ surface handle failed (1): "+config+", "+this+", error "+GLDrawableImpl.toHexString(eglError0));
            }
        }
        if(DEBUG) {
            System.err.println(getThreadName() + ": createEGLSurface handle "+GLDrawableImpl.toHexString(eglSurface));
        }
        return eglSurface;
    }
    private long createEGLSurface(final boolean isPBuffer, final boolean useNativeSurface,
                                  final EGLGraphicsConfiguration config, final NativeSurface nativeSurface) {
        if( isPBuffer ) {
            return EGLDrawableFactory.createPBufferSurfaceImpl(config, getSurfaceWidth(), getSurfaceHeight(), false);
        } else {
            final long eglNativeWin = useNativeSurface ?  nativeSurface.getSurfaceHandle() : ((NativeWindow)nativeSurface).getWindowHandle();
            final long eglSurface = eglCreateWindowSurface(config.getScreen().getDevice().getHandle(), config.getNativeConfig(), eglNativeWin);
            if(DEBUG) {
                final int eglPlatform = EGLDisplayUtil.getEGLPlatformType(true);
                System.err.println("EGLSurface.createEGLSurface.X: useNativeSurface "+useNativeSurface+
                        ", nativeWin "+EGLContext.toHexString(eglNativeWin)+") @ "+
                        eglPlatform+"/"+NativeWindowFactory.getNativeWindowType(true)+": "+
                        EGLContext.toHexString(eglSurface)+
                        ", "+((EGL.EGL_NO_SURFACE != eglSurface)?"OK":"Failed")+" - with config "+config);
            }
            return eglSurface;
        }
    }
    private static boolean hasUniqueNativeWindowHandle(final NativeSurface nativeSurface) {
        return nativeSurface instanceof NativeWindow &&
               ((NativeWindow)nativeSurface).getWindowHandle() != nativeSurface.getSurfaceHandle();
    }
    static String getThreadName() { return Thread.currentThread().getName(); }

    public static boolean isValidEGLSurfaceHandle(final long eglDisplayHandle, final long eglSurfaceHandle) {
        if( 0 == eglSurfaceHandle ) {
            return false;
        }
        final IntBuffer val = Buffers.newDirectIntBuffer(1);
        final boolean eglSurfaceValid = EGL.eglQuerySurface(eglDisplayHandle, eglSurfaceHandle, EGL.EGL_CONFIG_ID, val);
        if( !eglSurfaceValid ) {
            final int eglErr = EGL.eglGetError();
            if(DEBUG) {
                System.err.println(getThreadName() + ": EGLSurface.isValidEGLSurfaceHandle eglQuerySuface failed: error "+GLDrawableImpl.toHexString(eglErr)+", "+GLDrawableImpl.toHexString(eglSurfaceHandle));
            }
        }
        return eglSurfaceValid;
    }
}

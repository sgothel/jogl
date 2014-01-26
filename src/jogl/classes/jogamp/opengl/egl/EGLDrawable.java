/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package jogamp.opengl.egl;

import java.nio.IntBuffer;

import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.ProxySurface;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;

import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;

import com.jogamp.common.nio.Buffers;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;

public abstract class EGLDrawable extends GLDrawableImpl {

    protected EGLDrawable(EGLDrawableFactory factory, NativeSurface component) throws GLException {
        super(factory, component, false);
    }

    @Override
    public abstract GLContext createContext(GLContext shareWith);

    protected abstract long createSurface(EGLGraphicsConfiguration config, int width, int height, long nativeSurfaceHandle);

    private final long createEGLSurface() {
        final EGLWrappedSurface eglws = (EGLWrappedSurface) surface;
        final EGLGraphicsConfiguration eglConfig = (EGLGraphicsConfiguration) eglws.getGraphicsConfiguration();
        final NativeSurface upstreamSurface = eglws.getUpstreamSurface();

        long eglSurface = createSurface(eglConfig, eglws.getWidth(), eglws.getHeight(), upstreamSurface.getSurfaceHandle());

        int eglError0;
        if (EGL.EGL_NO_SURFACE == eglSurface) {
            eglError0 = EGL.eglGetError();
            if(EGL.EGL_BAD_NATIVE_WINDOW == eglError0) {
                // Try window handle if available and differs (Windows HDC / HWND).
                // ANGLE impl. required HWND on Windows.
                if(upstreamSurface instanceof NativeWindow) {
                    final NativeWindow nw = (NativeWindow) upstreamSurface;
                    if(nw.getWindowHandle() != nw.getSurfaceHandle()) {
                        if(DEBUG) {
                            System.err.println(getThreadName() + ": Info: Creation of window surface w/ surface handle failed: "+eglConfig+", error "+toHexString(eglError0)+", retry w/ windowHandle");
                        }
                        eglSurface = createSurface(eglConfig, eglws.getWidth(), eglws.getHeight(), nw.getWindowHandle());
                        if (EGL.EGL_NO_SURFACE == eglSurface) {
                            eglError0 = EGL.eglGetError();
                        }
                    }
                }
            }
        } else {
            eglError0 = EGL.EGL_SUCCESS;
        }
        if (EGL.EGL_NO_SURFACE == eglSurface) {
            throw new GLException("Creation of window surface failed: "+eglConfig+", "+surface+", error "+toHexString(eglError0));
        }
        if(DEBUG) {
            System.err.println(getThreadName() + ": createEGLSurface handle "+toHexString(eglSurface));
        }
        return eglSurface;
    }

    @Override
    protected final void createHandle() {
        final EGLWrappedSurface eglws = (EGLWrappedSurface) surface;
        if(DEBUG) {
            System.err.println(getThreadName() + ": createHandle of "+eglws);
        }
        if( eglws.containsUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE ) ) {
            if( EGL.EGL_NO_SURFACE != eglws.getSurfaceHandle() ) {
                throw new InternalError("Set surface but claimed to be invalid: "+eglws);
            }
            eglws.setSurfaceHandle( createEGLSurface() );
        } else if( EGL.EGL_NO_SURFACE == eglws.getSurfaceHandle() ) {
            throw new InternalError("Nil surface but claimed to be valid: "+eglws);
        }
    }

    @Override
    protected void destroyHandle() {
        final EGLWrappedSurface eglws = (EGLWrappedSurface) surface;
        if(DEBUG) {
            System.err.println(getThreadName() + ": destroyHandle of "+eglws);
        }
        if( EGL.EGL_NO_SURFACE == eglws.getSurfaceHandle() ) {
            throw new InternalError("Nil surface but claimed to be valid: "+eglws);
        }
        final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) eglws.getGraphicsConfiguration().getScreen().getDevice();
        if( eglws.containsUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE ) ) {
            EGL.eglDestroySurface(eglDevice.getHandle(), eglws.getSurfaceHandle());
            eglws.setSurfaceHandle(EGL.EGL_NO_SURFACE);
        }
    }

    protected static boolean isValidEGLSurface(long eglDisplayHandle, long surfaceHandle) {
        if( 0 == surfaceHandle ) {
            return false;
        }
        final IntBuffer val = Buffers.newDirectIntBuffer(1);
        final boolean eglSurfaceValid = EGL.eglQuerySurface(eglDisplayHandle, surfaceHandle, EGL.EGL_CONFIG_ID, val);
        if( !eglSurfaceValid ) {
            final int eglErr = EGL.eglGetError();
            if(DEBUG) {
                System.err.println(getThreadName() + ": EGLDrawable.isValidEGLSurface eglQuerySuface failed: error "+toHexString(eglErr)+", "+toHexString(surfaceHandle));
            }
        }
        return eglSurfaceValid;
    }

    @Override
    protected final void setRealizedImpl() {
        if(DEBUG) {
            System.err.println(getThreadName() + ": EGLDrawable.setRealized("+realized+"): NOP - "+surface);
        }
    }

    @Override
    protected final void swapBuffersImpl(boolean doubleBuffered) {
        if(doubleBuffered) {
            final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) surface.getGraphicsConfiguration().getScreen().getDevice();
            // single-buffer is already filtered out @ GLDrawableImpl#swapBuffers()
            if(!EGL.eglSwapBuffers(eglDevice.getHandle(), surface.getSurfaceHandle())) {
                throw new GLException("Error swapping buffers, eglError "+toHexString(EGL.eglGetError())+", "+this);
            }
        }
    }

    @Override
    public GLDynamicLookupHelper getGLDynamicLookupHelper() {
        if (getGLProfile().usesNativeGLES3()) {
            return getFactoryImpl().getGLDynamicLookupHelper(3);
        } else if (getGLProfile().usesNativeGLES2()) {
            return getFactoryImpl().getGLDynamicLookupHelper(2);
        } else if (getGLProfile().usesNativeGLES1()) {
            return getFactoryImpl().getGLDynamicLookupHelper(1);
        } else {
            throw new GLException("Unsupported: "+getGLProfile());
        }
    }

    @Override
    public String toString() {
        return getClass().getName()+"[realized "+isRealized()+
                    ",\n\tfactory    "+getFactory()+
                    ",\n\tsurface    "+getNativeSurface()+
                    ",\n\teglSurface "+toHexString(surface.getSurfaceHandle())+
                    ",\n\teglConfig  "+surface.getGraphicsConfiguration()+
                    ",\n\trequested  "+getRequestedGLCapabilities()+
                    ",\n\tchosen     "+getChosenGLCapabilities()+"]";
    }
}

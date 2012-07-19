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

import javax.media.nativewindow.MutableSurface;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.ProxySurface;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;

import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;

import com.jogamp.nativewindow.egl.EGLGraphicsDevice;

public abstract class EGLDrawable extends GLDrawableImpl {
    private boolean ownEGLSurface = false; // for destruction

    protected EGLDrawable(EGLDrawableFactory factory, NativeSurface component) throws GLException {
        super(factory, component, false);
    }

    @Override
    public abstract GLContext createContext(GLContext shareWith);

    protected abstract long createSurface(EGLGraphicsConfiguration config, long nativeSurfaceHandle);

    private final void recreateSurface() {
        final EGLGraphicsConfiguration eglConfig = (EGLGraphicsConfiguration) surface.getGraphicsConfiguration();
        final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) eglConfig.getScreen().getDevice();
        if(DEBUG) {
            System.err.println(getThreadName() + ": createSurface using "+eglConfig);
        }        
        if( EGL.EGL_NO_SURFACE != surface.getSurfaceHandle() ) {
            EGL.eglDestroySurface(eglDevice.getHandle(), surface.getSurfaceHandle());
        }
        
        final EGLUpstreamSurfaceHook upstreamHook = (EGLUpstreamSurfaceHook) ((ProxySurface)surface).getUpstreamSurfaceHook();
        final NativeSurface upstreamSurface = upstreamHook.getUpstreamSurface();
        long eglSurface = createSurface(eglConfig, upstreamSurface.getSurfaceHandle());
        
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
                        eglSurface = createSurface(eglConfig, nw.getWindowHandle());
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
            System.err.println(getThreadName() + ": setSurface using component: handle "+toHexString(surface.getSurfaceHandle())+" -> "+toHexString(eglSurface));
        }
        
        ((MutableSurface)surface).setSurfaceHandle(eglSurface);
    }

    @Override
    protected final void updateHandle() {
        if(ownEGLSurface) {
            recreateSurface();
        }
    }

    @Override
    protected final void setRealizedImpl() {
        final EGLGraphicsConfiguration eglConfig = (EGLGraphicsConfiguration) surface.getGraphicsConfiguration();
        final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) eglConfig.getScreen().getDevice();
        if (realized) {
            final long eglDisplayHandle = eglDevice.getHandle();
            if (EGL.EGL_NO_DISPLAY == eglDisplayHandle) {
                throw new GLException("Invalid EGL display in EGLGraphicsDevice "+eglDevice);
            }
            int[] tmp = new int[1];
            boolean eglSurfaceValid = 0 != surface.getSurfaceHandle();
            if(eglSurfaceValid) {
                eglSurfaceValid = EGL.eglQuerySurface(eglDisplayHandle, surface.getSurfaceHandle(), EGL.EGL_CONFIG_ID, tmp, 0);
                if(!eglSurfaceValid) {
                    if(DEBUG) {
                        System.err.println(getThreadName() + ": EGLDrawable.setRealizedImpl eglQuerySuface failed: "+toHexString(EGL.eglGetError())+", "+surface);
                    }                    
                }
            }
            if(eglSurfaceValid) {
                // surface holds valid EGLSurface
                if(DEBUG) {
                    System.err.println(getThreadName() + ": EGLDrawable.setRealizedImpl re-using component's EGLSurface: handle "+toHexString(surface.getSurfaceHandle()));
                }
                ownEGLSurface=false;
            } else {
                // EGLSurface is ours - subsequent updateHandle() will issue recreateSurface();
                // However .. let's validate the surface object first
                if( ! (surface instanceof ProxySurface) ) {
                    throw new InternalError("surface not ProxySurface: "+surface.getClass().getName()+", "+surface);
                }
                final ProxySurface.UpstreamSurfaceHook upstreamHook = ((ProxySurface)surface).getUpstreamSurfaceHook();
                if( null == upstreamHook ) {
                    throw new InternalError("null upstreamHook of: "+surface);
                }
                if( ! (upstreamHook instanceof EGLUpstreamSurfaceHook) ) {
                    throw new InternalError("upstreamHook not EGLUpstreamSurfaceHook: Surface: "+surface.getClass().getName()+", "+surface+"; UpstreamHook: "+upstreamHook.getClass().getName()+", "+upstreamHook);
                }
                if( null == ((EGLUpstreamSurfaceHook)upstreamHook).getUpstreamSurface() ) {
                    throw new InternalError("null upstream surface");
                }
                ownEGLSurface=true;
                if(DEBUG) {
                    System.err.println(getThreadName() + ": EGLDrawable.setRealizedImpl owning EGLSurface");
                }
            }
        } else if (ownEGLSurface && surface.getSurfaceHandle() != EGL.EGL_NO_SURFACE) {
            if(DEBUG) {
                System.err.println(getThreadName() + ": EGLDrawable.setRealized(false): ownSurface "+ownEGLSurface+", "+eglDevice+", eglSurface: "+toHexString(surface.getSurfaceHandle()));
            }
            // Destroy the window surface
            if (!EGL.eglDestroySurface(eglDevice.getHandle(), surface.getSurfaceHandle())) {
                throw new GLException("Error destroying window surface (eglDestroySurface)");
            }
            ((MutableSurface)surface).setSurfaceHandle(EGL.EGL_NO_SURFACE);
        }
    }

    @Override
    protected final void swapBuffersImpl() {
        final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) surface.getGraphicsConfiguration().getScreen().getDevice();
        // single-buffer is already filtered out @ GLDrawableImpl#swapBuffers()
        if(!EGL.eglSwapBuffers(eglDevice.getHandle(), surface.getSurfaceHandle())) {
            throw new GLException("Error swapping buffers, eglError "+toHexString(EGL.eglGetError())+", "+this);
        }
    }

    @Override
    public GLDynamicLookupHelper getGLDynamicLookupHelper() {
        if (getGLProfile().usesNativeGLES2()) {
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

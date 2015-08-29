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

import com.jogamp.common.ExceptionUtils;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLException;

import jogamp.nativewindow.ProxySurfaceImpl;
import jogamp.opengl.GLDrawableImpl;

import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.egl.EGL;

public class EGLDrawable extends GLDrawableImpl {
    static boolean DEBUG = GLDrawableImpl.DEBUG;

    protected EGLDrawable(final EGLDrawableFactory factory, final EGLSurface component) throws GLException {
        super(factory, component, false);
    }

    @Override
    public final GLContext createContext(final GLContext shareWith) {
        return new EGLContext(this, shareWith);
    }

    @Override
    protected final void createHandle() {
        final EGLSurface eglSurf = (EGLSurface) surface;
        if(DEBUG) {
            System.err.println(getThreadName() + ": createHandle of "+eglSurf);
            ProxySurfaceImpl.dumpHierarchy(System.err, eglSurf);
        }
        if( eglSurf.containsUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE ) ) {
            if( EGL.EGL_NO_SURFACE != eglSurf.getSurfaceHandle() ) {
                throw new InternalError("Set surface but claimed to be invalid: "+eglSurf);
            }
            if( !eglSurf.containsUpstreamOptionBits( ProxySurface.OPT_UPSTREAM_SURFACELESS ) ) {
                eglSurf.setEGLSurfaceHandle();
            }
        } else if( EGL.EGL_NO_SURFACE == eglSurf.getSurfaceHandle() ) {
            throw new InternalError("Nil surface but claimed to be valid: "+eglSurf);
        }
    }

    @Override
    protected void destroyHandle() {
        final EGLSurface eglSurf = (EGLSurface) surface;
        final long eglSurfHandle = eglSurf.getSurfaceHandle();
        if(DEBUG) {
            System.err.println(getThreadName() + ": EGLDrawable: destroyHandle of "+toHexString(eglSurfHandle));
            ProxySurfaceImpl.dumpHierarchy(System.err, eglSurf);
            System.err.println(getThreadName() + ": EGLSurface         : "+eglSurf);
            ExceptionUtils.dumpStack(System.err);
        }
        if( !eglSurf.containsUpstreamOptionBits( ProxySurface.OPT_UPSTREAM_SURFACELESS ) &&
            EGL.EGL_NO_SURFACE == eglSurfHandle ) {
            throw new InternalError("Nil surface but claimed to be valid: "+eglSurf);
        }
        final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) eglSurf.getGraphicsConfiguration().getScreen().getDevice();
        if( eglSurf.containsUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE ) ) {
            if( EGL.EGL_NO_SURFACE != eglSurfHandle ) {
                EGL.eglDestroySurface(eglDevice.getHandle(), eglSurfHandle);
                eglSurf.setSurfaceHandle(EGL.EGL_NO_SURFACE);
            }
        }
    }

    @Override
    protected final void setRealizedImpl() {
        if(DEBUG) {
            System.err.println(getThreadName() + ": EGLDrawable.setRealized("+realized+"): NOP - "+surface);
        }
    }

    @Override
    protected final void swapBuffersImpl(final boolean doubleBuffered) {
        if(doubleBuffered) {
            final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) surface.getGraphicsConfiguration().getScreen().getDevice();
            // single-buffer is already filtered out @ GLDrawableImpl#swapBuffers()
            if(!EGL.eglSwapBuffers(eglDevice.getHandle(), surface.getSurfaceHandle())) {
                throw new GLException("Error swapping buffers, eglError "+toHexString(EGL.eglGetError())+", "+this);
            }
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

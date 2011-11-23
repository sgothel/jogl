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

import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.GLDrawableImpl;

import javax.media.nativewindow.*;
import javax.media.nativewindow.egl.*;
import javax.media.opengl.*;

public abstract class EGLDrawable extends GLDrawableImpl {
    protected boolean ownEGLDisplay = false; // for destruction
    protected boolean ownEGLSurface = false; // for destruction
    private EGLGraphicsConfiguration eglConfig;
    protected long eglDisplay;
    protected long eglSurface;

    protected EGLDrawable(EGLDrawableFactory factory,
                       NativeSurface component) throws GLException {
        super(factory, component, false);
        eglSurface=EGL.EGL_NO_SURFACE;
        eglDisplay=0;
    }

    public long getDisplay() {
        return eglDisplay;
    }

    public long getHandle() {
        return eglSurface;
    }

    public EGLGraphicsConfiguration getGraphicsConfiguration() {
        return eglConfig;
    }

    public GLCapabilitiesImmutable getChosenGLCapabilities() {
        return (null==eglConfig)?super.getChosenGLCapabilities():(GLCapabilitiesImmutable)eglConfig.getChosenCapabilities();
    }

    public abstract GLContext createContext(GLContext shareWith);

    protected abstract long createSurface(long eglDpy, long eglNativeCfg, long surfaceHandle);

    private void recreateSurface() {
        // create a new EGLSurface ..
        if(EGL.EGL_NO_SURFACE!=eglSurface) {
            EGL.eglDestroySurface(eglDisplay, eglSurface);
        }

        if(DEBUG) {
            System.err.println("createSurface using eglDisplay "+toHexString(eglDisplay)+", "+eglConfig);
        }

        eglSurface = createSurface(eglDisplay, eglConfig.getNativeConfig(), surface.getSurfaceHandle());
        if (EGL.EGL_NO_SURFACE==eglSurface) {
            throw new GLException("Creation of window surface failed: "+eglConfig+", error "+toHexString(EGL.eglGetError()));
        }

        if(DEBUG) {
            System.err.println("setSurface using component: handle "+toHexString(surface.getSurfaceHandle())+" -> "+toHexString(eglSurface));
        }
    }

    protected void setRealizedImpl() {
        if (realized) {
            AbstractGraphicsConfiguration aConfig = surface.getGraphicsConfiguration();
            AbstractGraphicsDevice aDevice = aConfig.getScreen().getDevice();
            if(aDevice instanceof EGLGraphicsDevice) {
                if(DEBUG) {
                    System.err.println("EGLDrawable.setRealized(true): using existing EGL config - START");
                }
                // just fetch the data .. trust but verify ..
                eglDisplay = aDevice.getHandle();
                if (eglDisplay == EGL.EGL_NO_DISPLAY) {
                    throw new GLException("Invalid EGL display in EGLGraphicsDevice from "+aDevice);
                }
                if(aConfig instanceof EGLGraphicsConfiguration) {
                    eglConfig = (EGLGraphicsConfiguration) aConfig; // done ..
                    if (null == eglConfig) {
                        throw new GLException("Null EGLGraphicsConfiguration from "+aConfig);
                    }

                    int[] tmp = new int[1];
                    if ( 0 != surface.getSurfaceHandle() &&
                         EGL.eglQuerySurface(eglDisplay, surface.getSurfaceHandle(), EGL.EGL_CONFIG_ID, tmp, 0) ) {
                        // surface holds static EGLSurface
                        eglSurface = surface.getSurfaceHandle();
                        if(DEBUG) {
                            System.err.println("setSurface re-using component's EGLSurface: handle "+toHexString(eglSurface));
                        }
                    } else {
                        // EGLSurface is ours ..
                        ownEGLSurface=true;
                        recreateSurface();
                    }
                } else {
                    throw new GLException("EGLGraphicsDevice hold by non EGLGraphicsConfiguration: "+aConfig);
                }
            } else {
                if(DEBUG) {
                    System.err.println("EGLDrawable.setRealized(true): creating new EGL config - START");
                }
                // create a new EGL config ..
                ownEGLDisplay=true;
                // EGLSurface is ours ..
                ownEGLSurface=true;

                long nDisplay=0;
                if( NativeWindowFactory.TYPE_WINDOWS.equals(NativeWindowFactory.getNativeWindowType(false)) ) {
                    nDisplay = surface.getSurfaceHandle(); // don't even ask ..
                } else {
                    nDisplay = aDevice.getHandle(); // 0 == EGL.EGL_DEFAULT_DISPLAY
                }
                eglDisplay = EGL.eglGetDisplay(nDisplay);
                if (eglDisplay == EGL.EGL_NO_DISPLAY) {
                    if(DEBUG) {
                        System.err.println("eglDisplay("+toHexString(nDisplay)+" <surfaceHandle>): failed, using EGL_DEFAULT_DISPLAY");
                    }
                    nDisplay = EGL.EGL_DEFAULT_DISPLAY;
                    eglDisplay = EGL.eglGetDisplay(nDisplay);
                }
                if (eglDisplay == EGL.EGL_NO_DISPLAY) {
                    throw new GLException("Failed to created EGL display: nhandle "+toHexString(nDisplay)+", "+aDevice+", error "+toHexString(EGL.eglGetError()));
                } else if(DEBUG) {
                    System.err.println("eglDisplay("+toHexString(nDisplay)+"): "+toHexString(eglDisplay));
                }
                if (!EGL.eglInitialize(eglDisplay, null, null)) {
                    throw new GLException("eglInitialize failed"+", error "+Integer.toHexString(EGL.eglGetError()));
                }
                EGLGraphicsDevice e = new EGLGraphicsDevice(eglDisplay, AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);
                DefaultGraphicsScreen s = new DefaultGraphicsScreen(e, aConfig.getScreen().getIndex());
                // yes, use the already chosen/requested Capabilities (x11,win32,..)
                final GLCapabilitiesImmutable capsRequested = (GLCapabilitiesImmutable) aConfig.getRequestedCapabilities();
                final GLCapabilitiesImmutable capsChosen = (GLCapabilitiesImmutable) aConfig.getChosenCapabilities();
                eglConfig = (EGLGraphicsConfiguration) GraphicsConfigurationFactory.getFactory(e).chooseGraphicsConfiguration(
                        capsChosen, capsRequested, null, s);
                if (null == eglConfig) {
                    throw new GLException("Couldn't create EGLGraphicsConfiguration from "+s);
                } else if(DEBUG) {
                    System.err.println("Chosen eglConfig: "+eglConfig);
                }
                recreateSurface();
            }
            if(DEBUG) {
                System.err.println("EGLDrawable.setRealized(true): END: ownDisplay "+ownEGLDisplay+", ownSurface "+ownEGLSurface+" - "+this);
            }
        } else if (ownEGLSurface && eglSurface != EGL.EGL_NO_SURFACE) {
            if(DEBUG) {
                System.err.println("EGLDrawable.setRealized(false): ownDisplay "+ownEGLDisplay+", ownSurface "+ownEGLSurface);
            }
            // Destroy the window surface
            if (!EGL.eglDestroySurface(eglDisplay, eglSurface)) {
                throw new GLException("Error destroying window surface (eglDestroySurface)");
            }
            eglSurface = EGL.EGL_NO_SURFACE;
            if (ownEGLDisplay && EGL.EGL_NO_DISPLAY!=eglDisplay) {
                EGL.eglTerminate(eglDisplay);
            }
            eglDisplay=EGL.EGL_NO_DISPLAY;
            eglConfig=null;
        }
    }

    protected final void swapBuffersImpl() {
        // single-buffer is already filtered out @ GLDrawableImpl#swapBuffers()
        if(!EGL.eglSwapBuffers(eglDisplay, eglSurface)) {
            if(DEBUG) {
                System.err.println("eglSwapBuffers failed:");
                Thread.dumpStack();
            }
        }
    }

    public int getWidth() {
        int[] tmp = new int[1];
        if (!EGL.eglQuerySurface(eglDisplay, eglSurface, EGL.EGL_WIDTH, tmp, 0)) {
            throw new GLException("Error querying surface width");
        }
        return tmp[0];
    }

    public int getHeight() {
        int[] tmp = new int[1];
        if (!EGL.eglQuerySurface(eglDisplay, eglSurface, EGL.EGL_HEIGHT, tmp, 0)) {
            throw new GLException("Error querying surface height");
        }
        return tmp[0];
    }

    public GLDynamicLookupHelper getGLDynamicLookupHelper() {
        if (getGLProfile().usesNativeGLES2()) {
            return getFactoryImpl().getGLDynamicLookupHelper(2);
        } else if (getGLProfile().usesNativeGLES1()) {
            return getFactoryImpl().getGLDynamicLookupHelper(1);
        } else {
            throw new GLException("Unsupported: "+getGLProfile());
        }
    }

    public String toString() {
        return getClass().getName()+"[realized "+isRealized()+
                    ",\n\tfactory    "+getFactory()+
                    ",\n\tsurface    "+getNativeSurface()+
                    ",\n\teglSurface "+toHexString(eglSurface)+
                    ",\n\teglConfig  "+eglConfig+
                    ",\n\trequested  "+getRequestedGLCapabilities()+
                    ",\n\tchosen     "+getChosenGLCapabilities()+"]";
    }
}

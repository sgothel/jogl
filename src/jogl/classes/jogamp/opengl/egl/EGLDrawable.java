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
import javax.media.nativewindow.VisualIDHolder.VIDType;
import javax.media.opengl.*;

import com.jogamp.nativewindow.egl.*;

public abstract class EGLDrawable extends GLDrawableImpl {
    private boolean ownEGLDisplay = false; // for destruction
    private boolean ownEGLSurface = false; // for destruction
    private EGLGraphicsConfiguration eglConfig;
    private EGLGraphicsDevice eglDevice;
    private long eglSurface;

    protected EGLDrawable(EGLDrawableFactory factory,
                       NativeSurface component) throws GLException {
        super(factory, component, false);
        eglSurface=EGL.EGL_NO_SURFACE;
        eglDevice=null;
    }

    public final long getDisplay() {
        return null != eglDevice ? eglDevice.getHandle() : 0;
    }

    @Override
    public final long getHandle() {
        return eglSurface;
    }

    public final EGLGraphicsConfiguration getGraphicsConfiguration() {
        return eglConfig;
    }

    @Override
    public final GLCapabilitiesImmutable getChosenGLCapabilities() {
        return (null==eglConfig)?super.getChosenGLCapabilities():(GLCapabilitiesImmutable)eglConfig.getChosenCapabilities();
    }

    @Override
    public abstract GLContext createContext(GLContext shareWith);

    protected abstract long createSurface(long eglDpy, long eglNativeCfg, long surfaceHandle);

    private final void recreateSurface() {
        // create a new EGLSurface ..
        if(EGL.EGL_NO_SURFACE!=eglSurface) {
            EGL.eglDestroySurface(eglDevice.getHandle(), eglSurface);
        }

        if(DEBUG) {
            System.err.println(getThreadName() + ": createSurface using "+eglDevice+", "+eglConfig);
        }

        eglSurface = createSurface(eglDevice.getHandle(), eglConfig.getNativeConfig(), surface.getSurfaceHandle());
        int eglError0 = EGL.EGL_SUCCESS;
        if (EGL.EGL_NO_SURFACE == eglSurface) {
            eglError0 = EGL.eglGetError();
            if(EGL.EGL_BAD_NATIVE_WINDOW == eglError0) {
                // Try window handle if available and differs (Windows HDC / HWND).
                // ANGLE impl. required HWND on Windows.
                if(surface instanceof NativeWindow) {
                    final NativeWindow nw = (NativeWindow) surface;
                    if(nw.getWindowHandle() != nw.getSurfaceHandle()) {
                        if(DEBUG) {
                            System.err.println(getThreadName() + ": Info: Creation of window surface w/ surface handle failed: "+eglConfig+", error "+toHexString(eglError0)+", retry w/ windowHandle");
                        }
                        eglSurface = createSurface(eglDevice.getHandle(), eglConfig.getNativeConfig(), nw.getWindowHandle());
                        if (EGL.EGL_NO_SURFACE == eglSurface) {
                            eglError0 = EGL.eglGetError();
                        }
                    }
                }
            }
        }
        if (EGL.EGL_NO_SURFACE == eglSurface) {
            throw new GLException("Creation of window surface failed: "+eglConfig+", "+surface+", error "+toHexString(eglError0));
        }

        if(DEBUG) {
            System.err.println(getThreadName() + ": setSurface using component: handle "+toHexString(surface.getSurfaceHandle())+" -> "+toHexString(eglSurface));
        }
    }

    @Override
    protected final void updateHandle() {
        if(ownEGLSurface) {
            recreateSurface();
        }
    }

    @Override
    protected final void setRealizedImpl() {
        if (realized) {
            AbstractGraphicsConfiguration aConfig = surface.getGraphicsConfiguration();
            AbstractGraphicsDevice aDevice = aConfig.getScreen().getDevice();
            if(aDevice instanceof EGLGraphicsDevice) {
                if(DEBUG) {
                    System.err.println(getThreadName() + ": EGLDrawable.setRealized(true): using existing EGL config - START");
                }
                // just fetch the data .. trust but verify ..
                ownEGLDisplay = false;
                eglDevice = (EGLGraphicsDevice) aDevice;
                if (eglDevice.getHandle() == EGL.EGL_NO_DISPLAY) {
                    throw new GLException("Invalid EGL display in EGLGraphicsDevice "+eglDevice);
                }
                if(aConfig instanceof EGLGraphicsConfiguration) {
                    eglConfig = (EGLGraphicsConfiguration) aConfig; // done ..
                    if (null == eglConfig) {
                        throw new GLException("Null EGLGraphicsConfiguration from "+aConfig);
                    }

                    int[] tmp = new int[1];
                    if ( 0 != surface.getSurfaceHandle() &&
                         EGL.eglQuerySurface(eglDevice.getHandle(), surface.getSurfaceHandle(), EGL.EGL_CONFIG_ID, tmp, 0) ) {
                        // surface holds static EGLSurface
                        eglSurface = surface.getSurfaceHandle();
                        if(DEBUG) {
                            System.err.println(getThreadName() + ": setSurface re-using component's EGLSurface: handle "+toHexString(eglSurface));
                        }
                        ownEGLSurface=false;
                    } else {
                        // EGLSurface is ours - subsequent updateHandle() will issue recreateSurface();
                        ownEGLSurface=true;
                    }
                } else {
                    throw new GLException("EGLGraphicsDevice hold by non EGLGraphicsConfiguration: "+aConfig);
                }
            } else {
                if(DEBUG) {
                    System.err.println(getThreadName() + ": EGLDrawable.setRealized(true): creating new EGL config - START");
                }
                // create a new EGL config ..
                ownEGLDisplay=true;
                // EGLSurface is ours ..
                ownEGLSurface=true;

                eglDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(surface, true);
                AbstractGraphicsScreen eglScreen = new DefaultGraphicsScreen(eglDevice, aConfig.getScreen().getIndex());
                final GLCapabilitiesImmutable capsRequested = (GLCapabilitiesImmutable) aConfig.getRequestedCapabilities();
                if(aConfig instanceof EGLGraphicsConfiguration) {
                    final EGLGLCapabilities capsChosen = (EGLGLCapabilities) aConfig.getChosenCapabilities();
                    if(0 == capsChosen.getEGLConfig()) {
                        // 'refresh' the native EGLConfig handle
                        capsChosen.setEGLConfig(EGLGraphicsConfiguration.EGLConfigId2EGLConfig(eglDevice.getHandle(), capsChosen.getEGLConfigID()));
                        if(0 == capsChosen.getEGLConfig()) {
                            throw new GLException("Refreshing native EGLConfig handle failed: "+capsChosen+" of "+aConfig);
                        }
                    }
                    eglConfig  = new EGLGraphicsConfiguration(eglScreen, capsChosen, capsRequested, null);
                    if(DEBUG) {
                        System.err.println(getThreadName() + ": Reusing chosenCaps: "+eglConfig);
                    }
                } else {
                    eglConfig = EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(
                            capsRequested, capsRequested, null, eglScreen, aConfig.getVisualID(VIDType.NATIVE), false);

                    if (null == eglConfig) {
                        throw new GLException("Couldn't create EGLGraphicsConfiguration from "+eglScreen);
                    } else if(DEBUG) {
                        System.err.println(getThreadName() + ": Chosen eglConfig: "+eglConfig);
                    }
                }
                // subsequent updateHandle() will issue recreateSurface();
            }
            if(DEBUG) {
                System.err.println(getThreadName() + ": EGLDrawable.setRealized(true): END: ownDisplay "+ownEGLDisplay+", ownSurface "+ownEGLSurface);
            }
        } else if (ownEGLSurface && eglSurface != EGL.EGL_NO_SURFACE) {
            if(DEBUG) {
                System.err.println(getThreadName() + ": EGLDrawable.setRealized(false): ownDisplay "+ownEGLDisplay+", ownSurface "+ownEGLSurface+", "+eglDevice+", eglSurface: "+toHexString(eglSurface));
            }
            // Destroy the window surface
            if (!EGL.eglDestroySurface(eglDevice.getHandle(), eglSurface)) {
                throw new GLException("Error destroying window surface (eglDestroySurface)");
            }
            eglSurface = EGL.EGL_NO_SURFACE;
            eglConfig=null;
            eglDevice.close();
            eglDevice=null;
        }
    }

    @Override
    protected final void swapBuffersImpl() {
        // single-buffer is already filtered out @ GLDrawableImpl#swapBuffers()
        if(!EGL.eglSwapBuffers(eglDevice.getHandle(), eglSurface)) {
            throw new GLException("Error swapping buffers, eglError "+toHexString(EGL.eglGetError())+", "+this);
        }
    }

    /**
     * Surface not realizes yet (onscreen) .. Quering EGL surface size only makes sense for external drawable.
     * Leave it here for later impl. of an EGLExternalDrawable.
    public int getWidth() {
        int[] tmp = new int[1];
        if (!EGL.eglQuerySurface(eglDisplay, eglSurface, EGL.EGL_WIDTH, tmp, 0)) {
            throw new GLException("Error querying surface width, eglError "+toHexString(EGL.eglGetError()));
        }
        return tmp[0];
    }

    public int getHeight() {
        int[] tmp = new int[1];
        if (!EGL.eglQuerySurface(eglDisplay, eglSurface, EGL.EGL_HEIGHT, tmp, 0)) {
            throw new GLException("Error querying surface height, eglError "+toHexString(EGL.eglGetError()));
        }
        return tmp[0];
    } */

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
                    ",\n\tdevice     "+eglDevice+
                    ",\n\tsurface    "+getNativeSurface()+
                    ",\n\teglSurface "+toHexString(eglSurface)+
                    ",\n\teglConfig  "+eglConfig+
                    ",\n\trequested  "+getRequestedGLCapabilities()+
                    ",\n\tchosen     "+getChosenGLCapabilities()+"]";
    }
}

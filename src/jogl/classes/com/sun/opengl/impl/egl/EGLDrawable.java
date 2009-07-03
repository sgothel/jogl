/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.opengl.impl.egl;

import com.sun.opengl.impl.GLDrawableImpl;
import com.sun.nativewindow.impl.NWReflection;
import com.sun.gluegen.runtime.DynamicLookupHelper;

import javax.media.nativewindow.*;
import javax.media.nativewindow.egl.*;
import javax.media.opengl.*;

public abstract class EGLDrawable extends GLDrawableImpl {
    protected boolean ownEGLDisplay = false;
    private EGLGraphicsConfiguration eglConfig;
    protected long eglDisplay;
    protected long eglSurface;
    private int[] tmp = new int[1];

    protected EGLDrawable(EGLDrawableFactory factory,
                       NativeWindow component) throws GLException {
        super(factory, component, false);
        eglSurface=EGL.EGL_NO_SURFACE;
        eglDisplay=0;
    }

    public long getDisplay() {
        return eglDisplay;
    }

    public long getSurface() {
        return eglSurface;
    }

    public EGLGraphicsConfiguration getGraphicsConfiguration() {
        return eglConfig;
    }

    public GLCapabilities getChosenGLCapabilities() {
        return (null==eglConfig)?super.getChosenGLCapabilities():(GLCapabilities)eglConfig.getChosenCapabilities();
    }

    public abstract GLContext createContext(GLContext shareWith);

    protected abstract long createSurface(long eglDpy, _EGLConfig eglNativeCfg);

    private void recreateSurface() {
        if(EGL.EGL_NO_SURFACE!=eglSurface) {
            EGL.eglDestroySurface(eglDisplay, eglSurface);
        }
        eglSurface = createSurface(eglDisplay, eglConfig.getNativeConfig());

        if(DEBUG) {
            System.err.println("setSurface using component: handle 0x"+Long.toHexString(component.getWindowHandle())+" -> 0x"+Long.toHexString(eglSurface));
        }
    }

    public void setRealized(boolean realized) {
        super.setRealized(realized);

        if (realized) {
            if ( NativeWindow.LOCK_SURFACE_NOT_READY == lockSurface() ) {
                throw new GLException("Couldn't lock surface");
            }
            // lockSurface() also resolved the window/surface handles
            try {
                AbstractGraphicsConfiguration aConfig = component.getGraphicsConfiguration().getNativeGraphicsConfiguration();
                AbstractGraphicsDevice aDevice = aConfig.getScreen().getDevice();
                if(aDevice instanceof EGLGraphicsDevice) {
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
                        eglConfig.updateGraphicsConfiguration();
                    } else {
                        throw new GLException("EGLGraphicsConfiguration doesn't carry a EGLGraphicsDevice: "+aConfig);
                    }
                } else {
                    // create a new EGL config ..
                    ownEGLDisplay=true;
                    long nDisplay;
                    if( NativeWindowFactory.TYPE_WINDOWS.equals(NativeWindowFactory.getNativeWindowType(false)) ) {
                        nDisplay = component.getSurfaceHandle(); // don't even ask ..
                    } else {
                        nDisplay = aDevice.getHandle(); // 0 == EGL.EGL_DEFAULT_DISPLAY
                    }
                    eglDisplay = EGL.eglGetDisplay(nDisplay);
                    if (eglDisplay == EGL.EGL_NO_DISPLAY) {
                        if(DEBUG) {
                            System.err.println("eglDisplay("+Long.toHexString(nDisplay)+" <surfaceHandle>): failed, using EGL_DEFAULT_DISPLAY");
                        }
                        nDisplay = EGL.EGL_DEFAULT_DISPLAY;
                        eglDisplay = EGL.eglGetDisplay(nDisplay);
                    }
                    if (eglDisplay == EGL.EGL_NO_DISPLAY) {
                        throw new GLException("Failed to created EGL display: nhandle 0x"+Long.toHexString(nDisplay)+", "+aDevice+", error 0x"+Integer.toHexString(EGL.eglGetError()));
                    } else if(DEBUG) {
                        System.err.println("eglDisplay("+Long.toHexString(nDisplay)+"): 0x"+Long.toHexString(eglDisplay));
                    }
                    if (!EGL.eglInitialize(eglDisplay, null, null)) {
                        throw new GLException("eglInitialize failed"+", error 0x"+Integer.toHexString(EGL.eglGetError()));
                    }
                    EGLGraphicsDevice e = new EGLGraphicsDevice(eglDisplay);
                    DefaultGraphicsScreen s = new DefaultGraphicsScreen(e, aConfig.getScreen().getIndex());
                    GLCapabilities caps = (GLCapabilities) aConfig.getChosenCapabilities(); // yes, use the already choosen Capabilities (x11,win32,..)
                    eglConfig = (EGLGraphicsConfiguration) GraphicsConfigurationFactory.getFactory(e).chooseGraphicsConfiguration(caps, null, s);
                    if (null == eglConfig) {
                        throw new GLException("Couldn't create EGLGraphicsConfiguration from "+s);
                    } else if(DEBUG) {
                        System.err.println("Chosen eglConfig: "+eglConfig);
                    }
                }
                recreateSurface();
            } finally {
              unlockSurface();
            }
        } else if (eglSurface != EGL.EGL_NO_SURFACE) {
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

    public int getWidth() {
        if (!EGL.eglQuerySurface(eglDisplay, eglSurface, EGL.EGL_WIDTH, tmp, 0)) {
            throw new GLException("Error querying surface width");
        }
        return tmp[0];
    }

    public int getHeight() {
        if (!EGL.eglQuerySurface(eglDisplay, eglSurface, EGL.EGL_HEIGHT, tmp, 0)) {
            throw new GLException("Error querying surface height");
        }
        return tmp[0];
    }

    public DynamicLookupHelper getDynamicLookupHelper() {
        return EGLDynamicLookupHelper.getDynamicLookupHelper(getGLProfile());
    }

    public String toString() {
        return "EGLDrawable[ realized "+getRealized()+
                           ", window "+getNativeWindow()+
                           ", egl surface " + eglSurface +
                           ", "+eglConfig+
                           ", factory "+getFactory()+"]";
    }
}

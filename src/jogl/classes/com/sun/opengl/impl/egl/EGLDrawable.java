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

import javax.media.nativewindow.*;
import javax.media.opengl.*;

public class EGLDrawable extends GLDrawableImpl {
    private NWCapabilitiesChooser chooser;
    private long display;
    private EGLConfig config;
    private long surface;
    private int[] tmp = new int[1];

    public EGLDrawable(EGLDrawableFactory factory,
                       NativeWindow component,
                       NWCapabilities requestedCapabilities,
                       NWCapabilitiesChooser chooser) throws GLException {
        super(factory, component, requestedCapabilities, false);
        this.chooser = chooser;
        surface=EGL.EGL_NO_SURFACE;
        display=0;
        config=null;
    }

    public long getDisplay() {
        return display;
    }

    public EGLConfig getEGLConfig() {
        return config;
    }

    public long getSurface() {
        return surface;
    }

    protected void setSurface() {
        if (EGL.EGL_NO_SURFACE==surface) {
            try {
                lockSurface();
                // Create the window surface
                surface = EGL.eglCreateWindowSurface(display, config.getNativeConfig(), component.getWindowHandle(), null);
                if (EGL.EGL_NO_SURFACE==surface) {
                    throw new GLException("Creation of window surface (eglCreateWindowSurface) failed, component: "+component);
                }
            } finally {
              unlockSurface();
            }
        }
    }

    public GLContext createContext(GLContext shareWith) {
        return new EGLContext(this, shareWith);
    }

    public void setRealized(boolean realized) {
        if (realized) {
            if (NWReflection.instanceOf(component, "com.sun.javafx.newt.kd.KDWindow")) {
                // KDWindows holds already determined EGL values
                display = component.getDisplayHandle();
                if (display==0) {
                    throw new GLException("KDWindow has null display");
                }
                if (display == EGL.EGL_NO_DISPLAY) {
                    throw new GLException("KDWindow has EGL_NO_DISPLAY");
                }
                Long setConfigID = new Long(component.getVisualID());
                if ( 0 <= setConfigID.longValue() && setConfigID.longValue() <= Integer.MAX_VALUE ) {
                    config = new EGLConfig(display, setConfigID.intValue());
                } else {
                    throw new GLException("KDWindow has invalid visualID/configID");
                }
            } else {
                display = EGL.eglGetDisplay((0!=component.getDisplayHandle())?component.getDisplayHandle():EGL.EGL_DEFAULT_DISPLAY);
                if (display == EGL.EGL_NO_DISPLAY) {
                    throw new GLException("eglGetDisplay failed");
                }
                if (!EGL.eglInitialize(display, null, null)) {
                    throw new GLException("eglInitialize failed");
                }
                config = new EGLConfig(display, getRequestedNWCapabilities());
            }
            setChosenNWCapabilities(config.getCapabilities());
        } else if (surface != EGL.EGL_NO_SURFACE) {
            // Destroy the window surface
            if (!EGL.eglDestroySurface(display, surface)) {
                throw new GLException("Error destroying window surface (eglDestroySurface)");
            }
            surface = EGL.EGL_NO_SURFACE;
            if (EGL.EGL_NO_DISPLAY!=display) {
                EGL.eglTerminate(display);
                display=EGL.EGL_NO_DISPLAY;
            }
        }
        super.setRealized(realized);
    }

    public void setSize(int width, int height) {
        // FIXME: anything to do here?
    }

    public int getWidth() {
        if (!EGL.eglQuerySurface(display, surface, EGL.EGL_WIDTH, tmp, 0)) {
            throw new GLException("Error querying surface width");
        }
        return tmp[0];
    }

    public int getHeight() {
        if (!EGL.eglQuerySurface(display, surface, EGL.EGL_HEIGHT, tmp, 0)) {
            throw new GLException("Error querying surface height");
        }
        return tmp[0];
    }

    public void swapBuffers() throws GLException {
        getFactoryImpl().lockToolkit();
        try {
          if (component.getSurfaceHandle() == 0) {
            if (lockSurface() == NativeWindow.LOCK_SURFACE_NOT_READY) {
              return;
            }
          }

          EGL.eglSwapBuffers(display, surface);

        } finally {
          unlockSurface();
          getFactoryImpl().unlockToolkit();
        }
    }

    public String toString() {
        return "EGLDrawable[ realized "+getRealized()+
                           ", window "+getNativeWindow()+
                           ", egl display " + display +
                           ", " + config +
                           ", egl surface " + surface +
                           ", factory "+getFactory()+"]";
    }
}

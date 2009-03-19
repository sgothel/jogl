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
 */

package com.sun.javafx.newt.opengl.kd;

import com.sun.javafx.newt.*;
import com.sun.javafx.newt.impl.*;
import com.sun.opengl.impl.egl.*;
import javax.media.nativewindow.Capabilities;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.nativewindow.NativeWindowException;

public class KDWindow extends Window {
    private static final String WINDOW_CLASS_NAME = "NewtWindow";
    // non fullscreen dimensions ..
    private int nfs_width, nfs_height, nfs_x, nfs_y;

    static {
        NativeLibLoader.loadNEWT();

        if (!initIDs()) {
            throw new NativeWindowException("Failed to initialize jmethodIDs");
        }
    }

    public KDWindow() {
    }

    protected void createNative(Capabilities caps) {
        int eglRenderableType;
        if(GLProfile.isGLES1()) {
            eglRenderableType = EGL.EGL_OPENGL_ES_BIT;
        }
        else if(GLProfile.isGLES2()) {
            eglRenderableType = EGL.EGL_OPENGL_ES2_BIT;
        } else {
            eglRenderableType = EGL.EGL_OPENGL_BIT;
        }
        GLCapabilities glCaps = null;
        if (caps instanceof GLCapabilities) {
            glCaps = (GLCapabilities) caps;
        } else {
            glCaps = new GLCapabilities();
            glCaps.setRedBits(caps.getRedBits());
            glCaps.setGreenBits(caps.getGreenBits());
            glCaps.setBlueBits(caps.getBlueBits());
            glCaps.setAlphaBits(caps.getAlphaBits());
        }
        EGLConfig config = new EGLConfig(getDisplayHandle(), glCaps);
        this.config = config;
        chosenCaps = config.getCapabilities();

        windowHandle = 0;
        windowID = ++_windowID;
        eglWindowHandle = CreateWindow(windowID, getDisplayHandle(), config.getAttributeList());
        if (eglWindowHandle == 0) {
            throw new NativeWindowException("Error creating egl window: "+eglWindowHandle);
        }
        setVisible0(eglWindowHandle, false);
        /*
        windowHandle = RealizeWindow(eglWindowHandle);
        if (0 == windowHandle) {
            throw new NativeWindowException("Error native Window Handle is null");
        } */
        windowHandleClose = eglWindowHandle;
    }

    protected void closeNative() {
        if(0!=windowHandleClose) {
            CloseWindow(windowHandleClose);
        }
    }

    public void setVisible(boolean visible) {
        if(this.visible!=visible) {
            this.visible=visible;
            setVisible0(eglWindowHandle, visible);
            if ( 0==windowHandle ) {
                windowHandle = RealizeWindow(eglWindowHandle);
                if (0 == windowHandle) {
                    throw new NativeWindowException("Error native Window Handle is null");
                }
            }
            clearEventMask();
        }
    }

    public void setSize(int width, int height) {
        setSize0(eglWindowHandle, width, height);
    }

    public void setPosition(int x, int y) {
        // n/a in KD
        System.err.println("setPosition n/a in KD");
    }

    public boolean setFullscreen(boolean fullscreen) {
        if(this.fullscreen!=fullscreen) {
            this.fullscreen=fullscreen;
            if(this.fullscreen) {
                setFullScreen0(eglWindowHandle, true);
            } else {
                setFullScreen0(eglWindowHandle, false);
                setSize0(eglWindowHandle, nfs_width, nfs_height);
            }
        }
        return true;
    }

    protected void dispatchMessages(int eventMask) {
        DispatchMessages(windowID, eglWindowHandle, eventMask);
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    private static native boolean initIDs();
    private        native long CreateWindow(int owner, long displayHandle, int[] attributes);
    private        native long RealizeWindow(long eglWindowHandle);
    private        native int  CloseWindow(long eglWindowHandle);
    private        native void setVisible0(long eglWindowHandle, boolean visible);
    private        native void setSize0(long eglWindowHandle, int width, int height);
    private        native void setFullScreen0(long eglWindowHandle, boolean fullscreen);
    private        native void DispatchMessages(int owner, long eglWindowHandle, int eventMask);

    private void sizeChanged(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;
        if(!fullscreen) {
            nfs_width=width;
            nfs_height=height;
        } else {
            screen.setScreenSize(width, height);
        }
        sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED);
    }

    private long   eglWindowHandle;
    private long   windowHandleClose;
    private int    windowID;
    private static int _windowID = 0;
}

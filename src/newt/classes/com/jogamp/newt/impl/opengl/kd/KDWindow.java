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

package com.jogamp.newt.impl.opengl.kd;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.impl.*;
import com.jogamp.opengl.impl.egl.*;
import javax.media.nativewindow.*;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.nativewindow.NativeWindowException;

public class KDWindow extends Window {
    private static final String WINDOW_CLASS_NAME = "NewtWindow";
    // non fullscreen dimensions ..
    private int nfs_width, nfs_height, nfs_x, nfs_y;

    static {
        KDDisplay.initSingleton();
    }

    public KDWindow() {
    }

    protected void createNativeImpl() {
        if(0!=parentWindowHandle) {
            throw new RuntimeException("Window parenting not supported (yet)");
        }
        config = GraphicsConfigurationFactory.getFactory(getScreen().getDisplay().getGraphicsDevice()).chooseGraphicsConfiguration(caps, null, getScreen().getGraphicsScreen());
        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }

        GLCapabilities eglCaps = (GLCapabilities)config.getChosenCapabilities();
        int[] eglAttribs = EGLGraphicsConfiguration.GLCapabilities2AttribList(eglCaps);

        eglWindowHandle = CreateWindow(getDisplayHandle(), eglAttribs);
        if (eglWindowHandle == 0) {
            throw new NativeWindowException("Error creating egl window: "+config);
        }
        setVisible0(eglWindowHandle, false);
        windowHandle = RealizeWindow(eglWindowHandle);
        if (0 == windowHandle) {
            throw new NativeWindowException("Error native Window Handle is null");
        }
        windowHandleClose = eglWindowHandle;
    }

    protected void closeNativeImpl() {
        if(0!=windowHandleClose) {
            CloseWindow(windowHandleClose, windowUserData);
            windowUserData=0;
        }
    }

    protected void setVisibleImpl(boolean visible) {
        setVisible0(eglWindowHandle, visible);
    }

    protected void setSizeImpl(int width, int height) {
        if(0!=eglWindowHandle) {
            setSize0(eglWindowHandle, width, height);
        }
    }

    protected void setPositionImpl(int x, int y) {
        // n/a in KD
        System.err.println("setPosition n/a in KD");
    }

    protected void setFullscreenImpl(final boolean fullscreen, final int x, final int y, final int w, final int h) {
        if(0!=eglWindowHandle) {
            setFullScreen0(eglWindowHandle, fullscreen);
            if(!fullscreen) {
                setSize0(eglWindowHandle, w, h);
            }
        }
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    protected static native boolean initIDs();
    private        native long CreateWindow(long displayHandle, int[] attributes);
    private        native long RealizeWindow(long eglWindowHandle);
    private        native int  CloseWindow(long eglWindowHandle, long userData);
    private        native void setVisible0(long eglWindowHandle, boolean visible);
    private        native void setSize0(long eglWindowHandle, int width, int height);
    private        native void setFullScreen0(long eglWindowHandle, boolean fullscreen);

    private void windowCreated(long userData) {
        windowUserData=userData;
    }

    protected void sizeChanged(int newWidth, int newHeight) {
        if(fullscreen) {
            ((KDScreen)screen).setScreenSize(width, height);
        }
        super.sizeChanged(newWidth, newHeight);
    }

    private long   eglWindowHandle;
    private long   windowHandleClose;
    private long   windowUserData;
}

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

import com.jogamp.newt.impl.*;
import com.jogamp.opengl.impl.egl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.util.Point;
import javax.media.opengl.GLCapabilitiesImmutable;

public class KDWindow extends WindowImpl {
    private static final String WINDOW_CLASS_NAME = "NewtWindow";

    static {
        KDDisplay.initSingleton();
    }

    public KDWindow() {
    }

    protected void createNativeImpl() {
        if(0!=getParentWindowHandle()) {
            throw new RuntimeException("Window parenting not supported (yet)");
        }
        config = GraphicsConfigurationFactory.getFactory(getScreen().getDisplay().getGraphicsDevice()).chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, getScreen().getGraphicsScreen());
        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }

        GLCapabilitiesImmutable eglCaps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
        int[] eglAttribs = EGLGraphicsConfiguration.GLCapabilities2AttribList(eglCaps);

        eglWindowHandle = CreateWindow(getDisplayHandle(), eglAttribs);
        if (eglWindowHandle == 0) {
            throw new NativeWindowException("Error creating egl window: "+config);
        }
        setVisible0(eglWindowHandle, false);
        setWindowHandle(RealizeWindow(eglWindowHandle));
        if (0 == getWindowHandle()) {
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

    protected void setVisibleImpl(boolean visible, int x, int y, int width, int height) {
        setVisible0(eglWindowHandle, visible);
        reconfigureWindowImpl(x, y, width, height, false, 0, 0);
        visibleChanged(visible);
    }

    protected void requestFocusImpl(boolean reparented) { }

    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, 
                                            boolean parentChange, int fullScreenChange, int decorationChange) {
        if(0!=eglWindowHandle) {
            if(0!=fullScreenChange) {
                boolean fs = fullScreenChange > 0;
                setFullScreen0(eglWindowHandle, fs);
                if(fs) {
                    return true;
                }
            }
            // int _x=(x>=0)?x:this.x;
            // int _y=(x>=0)?y:this.y;
            int _w=(width>0)?width:this.width;
            int _h=(height>0)?height:this.height;
            if(width>0 || height>0) {
                setSize0(eglWindowHandle, _w, _h);
            }
            if(x>=0 || y>=0) {
                System.err.println("setPosition n/a in KD");
            }
        }
        return true;
    }

    protected Point getLocationOnScreenImpl(int x, int y) {
        return new Point(x,y);
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

    protected void sizeChanged(int newWidth, int newHeight, boolean force) {
        if(fullscreen) {
            ((KDScreen)getScreen()).setScreenSize(width, height);
        }
        super.sizeChanged(newWidth, newHeight, force);
    }

    private long   eglWindowHandle;
    private long   windowHandleClose;
    private long   windowUserData;
}

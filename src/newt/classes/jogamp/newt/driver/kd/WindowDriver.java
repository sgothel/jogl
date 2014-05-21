/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2012 JogAmp Community. All rights reserved.
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

package jogamp.newt.driver.kd;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.VisualIDHolder;
import javax.media.nativewindow.VisualIDHolder.VIDType;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.Point;
import javax.media.opengl.GLCapabilitiesImmutable;

import jogamp.newt.WindowImpl;
import jogamp.opengl.egl.EGLGraphicsConfiguration;

public class WindowDriver extends WindowImpl {
    private static final String WINDOW_CLASS_NAME = "NewtWindow";

    static {
        DisplayDriver.initSingleton();
    }

    public WindowDriver() {
    }

    @Override
    protected void createNativeImpl() {
        if(0!=getParentWindowHandle()) {
            throw new RuntimeException("Window parenting not supported (yet)");
        }
        final AbstractGraphicsConfiguration cfg = GraphicsConfigurationFactory.getFactory(getScreen().getDisplay().getGraphicsDevice(), capsRequested).chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, getScreen().getGraphicsScreen(), VisualIDHolder.VID_UNDEFINED);
        if (null == cfg) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        setGraphicsConfiguration(cfg);

        GLCapabilitiesImmutable eglCaps = (GLCapabilitiesImmutable) cfg.getChosenCapabilities();
        int eglConfigID = eglCaps.getVisualID(VIDType.EGL_CONFIG);
        long eglConfig = EGLGraphicsConfiguration.EGLConfigId2EGLConfig(getDisplayHandle(), eglConfigID);

        eglWindowHandle = CreateWindow(getDisplayHandle(), eglConfig);
        if (eglWindowHandle == 0) {
            throw new NativeWindowException("Error creating egl window: "+cfg+", eglConfigID "+eglConfigID+", eglConfig 0x"+Long.toHexString(eglConfig));
        }
        setVisible0(eglWindowHandle, false);
        setWindowHandle(RealizeWindow(eglWindowHandle));
        if (0 == getWindowHandle()) {
            throw new NativeWindowException("Error native Window Handle is null");
        }
        windowHandleClose = eglWindowHandle;
    }

    @Override
    protected void closeNativeImpl() {
        if(0!=windowHandleClose) {
            CloseWindow(windowHandleClose, windowUserData);
            windowUserData=0;
        }
    }

    @Override
    protected void requestFocusImpl(boolean reparented) { }

    @Override
    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, int flags) {
        if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) ) {
            setVisible0(eglWindowHandle, 0 != ( FLAG_IS_VISIBLE & flags));
            visibleChanged(false, 0 != ( FLAG_IS_VISIBLE & flags));
        }

        if(0!=eglWindowHandle) {
            if(0 != ( FLAG_CHANGE_FULLSCREEN & flags)) {
                final boolean fs = 0 != ( FLAG_IS_FULLSCREEN & flags) ;
                setFullScreen0(eglWindowHandle, fs);
                if(fs) {
                    return true;
                }
            }
            // int _x=(x>=0)?x:this.x;
            // int _y=(x>=0)?y:this.y;
            width=(width>0)?width:getSurfaceWidth();
            height=(height>0)?height:getSurfaceHeight();
            if(width>0 || height>0) {
                setSize0(eglWindowHandle, width, height);
            }
            if(x>=0 || y>=0) {
                System.err.println("setPosition n/a in KD");
            }
        }

        if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) ) {
            visibleChanged(false, 0 != ( FLAG_IS_VISIBLE & flags));
        }

        return true;
    }

    @Override
    protected Point getLocationOnScreenImpl(int x, int y) {
        return new Point(x,y);
    }

    @Override
    protected void updateInsetsImpl(Insets insets) {
        // nop ..
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    protected static native boolean initIDs();
    private        native long CreateWindow(long displayHandle, long eglConfig);
    private        native long RealizeWindow(long eglWindowHandle);
    private        native int  CloseWindow(long eglWindowHandle, long userData);
    private        native void setVisible0(long eglWindowHandle, boolean visible);
    private        native void setSize0(long eglWindowHandle, int width, int height);
    private        native void setFullScreen0(long eglWindowHandle, boolean fullscreen);

    private void windowCreated(long userData) {
        windowUserData=userData;
    }

    @Override
    protected void sizeChanged(boolean defer, int newWidth, int newHeight, boolean force) {
        if(isFullscreen()) {
            ((ScreenDriver)getScreen()).sizeChanged(getSurfaceWidth(), getSurfaceHeight());
        }
        super.sizeChanged(defer, newWidth, newHeight, force);
    }

    private long   eglWindowHandle;
    private long   windowHandleClose;
    private long   windowUserData;
}

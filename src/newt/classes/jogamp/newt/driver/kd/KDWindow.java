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

package jogamp.newt.driver.kd;

import jogamp.newt.*;
import jogamp.newt.driver.intel.gdl.Display;
import jogamp.opengl.egl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.util.Insets;
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
        final AbstractGraphicsConfiguration cfg = GraphicsConfigurationFactory.getFactory(getScreen().getDisplay().getGraphicsDevice()).chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, getScreen().getGraphicsScreen());
        if (null == cfg) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        setGraphicsConfiguration(cfg);

        GLCapabilitiesImmutable eglCaps = (GLCapabilitiesImmutable) cfg.getChosenCapabilities();
        int[] eglAttribs = EGLGraphicsConfiguration.GLCapabilities2AttribList(eglCaps);

        eglWindowHandle = CreateWindow(getDisplayHandle(), eglAttribs);
        if (eglWindowHandle == 0) {
            throw new NativeWindowException("Error creating egl window: "+cfg);
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

    protected void requestFocusImpl(boolean reparented) { }

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
            width=(width>0)?width:this.width;
            height=(height>0)?height:this.height;
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

    protected Point getLocationOnScreenImpl(int x, int y) {
        return new Point(x,y);
    }

    protected void updateInsetsImpl(Insets insets) {
        // nop ..        
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

    @Override
    protected void sizeChanged(boolean defer, int newWidth, int newHeight, boolean force) {
        if(fullscreen) {
            ((KDScreen)getScreen()).sizeChanged(width, height);
        }
        super.sizeChanged(defer, newWidth, newHeight, force);
    }

    private long   eglWindowHandle;
    private long   windowHandleClose;
    private long   windowUserData;
}

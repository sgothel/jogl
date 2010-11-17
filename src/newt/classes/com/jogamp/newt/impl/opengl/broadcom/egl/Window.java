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

package com.jogamp.newt.impl.opengl.broadcom.egl;

import com.jogamp.opengl.impl.egl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.util.Point;
import javax.media.opengl.GLCapabilitiesImmutable;

public class Window extends com.jogamp.newt.impl.WindowImpl {
    static {
        Display.initSingleton();
    }

    public Window() {
    }

    protected void createNativeImpl() {
        if(0!=getParentWindowHandle()) {
            throw new RuntimeException("Window parenting not supported (yet)");
        }
        // query a good configuration .. even thought we drop this one 
        // and reuse the EGLUtil choosen one later.
        config = GraphicsConfigurationFactory.getFactory(getScreen().getDisplay().getGraphicsDevice()).chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, getScreen().getGraphicsScreen());
        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        setSizeImpl(getScreen().getWidth(), getScreen().getHeight());

        setWindowHandle(realizeWindow(true, width, height));
        if (0 == getWindowHandle()) {
            throw new NativeWindowException("Error native Window Handle is null");
        }
    }

    protected void closeNativeImpl() {
        if(0!=windowHandleClose) {
            CloseWindow(getDisplayHandle(), windowHandleClose);
        }
    }

    protected void setVisibleImpl(boolean visible, int x, int y, int width, int height) {
        reconfigureWindowImpl(x, y, width, height, false, 0, 0);
        visibleChanged(visible);
    }

    protected void requestFocusImpl(boolean reparented) { }

    protected void setSizeImpl(int width, int height) {
        if(0!=getWindowHandle()) {
            // n/a in BroadcomEGL
            System.err.println("BCEGL Window.setSizeImpl n/a in BroadcomEGL with realized window");
        } else {
            this.width = width;
            this.height = height;
        }
    }

    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, 
                                            boolean parentChange, int fullScreenChange, int decorationChange) {
        if(0!=getWindowHandle()) {
            if(0!=fullScreenChange) {
                if( fullScreenChange > 0 ) {
                    // n/a in BroadcomEGL
                    System.err.println("setFullscreen n/a in BroadcomEGL");
                    return false;
                }
            }
        }
        if(width>0 || height>0) {
            if(0!=getWindowHandle()) {
                // n/a in BroadcomEGL
                System.err.println("BCEGL Window.setSizeImpl n/a in BroadcomEGL with realized window");
            } else {
                this.width=(width>0)?width:this.width;
                this.height=(height>0)?height:this.height;
            }
        }
        if(x>=0 || y>=0) {
            System.err.println("BCEGL Window.setPositionImpl n/a in BroadcomEGL");
        }
        return true;
    }

    protected Point getLocationOnScreenImpl(int x, int y) {
        return new Point(x,y);
    }


    public boolean surfaceSwap() {
        SwapWindow(getDisplayHandle(), getWindowHandle());
        return true;
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    protected static native boolean initIDs();
    private        native long CreateWindow(long eglDisplayHandle, boolean chromaKey, int width, int height);
    private        native void CloseWindow(long eglDisplayHandle, long eglWindowHandle);
    private        native void SwapWindow(long eglDisplayHandle, long eglWindowHandle);


    private long realizeWindow(boolean chromaKey, int width, int height) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("BCEGL Window.realizeWindow() with: chroma "+chromaKey+", "+width+"x"+height+", "+config);
        }
        long handle = CreateWindow(getDisplayHandle(), chromaKey, width, height);
        if (0 == handle) {
            throw new NativeWindowException("Error native Window Handle is null");
        }
        windowHandleClose = handle;
        return handle;
    }

    private void windowCreated(int cfgID, int width, int height) {
        this.width = width;
        this.height = height;
        GLCapabilitiesImmutable capsReq = (GLCapabilitiesImmutable) config.getRequestedCapabilities();
        config = EGLGraphicsConfiguration.create(capsReq, getScreen().getGraphicsScreen(), cfgID);
        if (config == null) {
            throw new NativeWindowException("Error creating EGLGraphicsConfiguration from id: "+cfgID+", "+this);
        }
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("BCEGL Window.windowCreated(): "+toHexString(cfgID)+", "+width+"x"+height+", "+config);
        }
    }

    private long   windowHandleClose;
}

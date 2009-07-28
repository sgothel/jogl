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

package com.sun.javafx.newt.opengl.broadcom;

import com.sun.javafx.newt.*;
import com.sun.javafx.newt.impl.*;
import com.sun.opengl.impl.egl.*;
import javax.media.nativewindow.*;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.nativewindow.NativeWindowException;

public class BCEGLWindow extends Window {
    static {
        BCEGLDisplay.initSingleton();
    }

    public BCEGLWindow() {
    }

    protected void createNative(Capabilities caps) {
        // query a good configuration .. even thought we drop this one 
        // and reuse the EGLUtil choosen one later.
        config = GraphicsConfigurationFactory.getFactory(getScreen().getDisplay().getGraphicsDevice()).chooseGraphicsConfiguration(caps, null, getScreen().getGraphicsScreen());
        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
    }

    protected void closeNative() {
        if(0!=windowHandleClose) {
            CloseWindow(getDisplayHandle(), windowHandleClose);
        }
    }

    public void setVisible(boolean visible) {
        if(this.visible!=visible) {
            this.visible=visible;
            if ( 0==windowHandle ) {
                windowHandle = realizeWindow(true, width, height);
                if (0 == windowHandle) {
                    throw new NativeWindowException("Error native Window Handle is null");
                }
            }
            clearEventMask();
        }
    }

    public void setSize(int width, int height) {
        if(0!=windowHandle) {
            // n/a in BroadcomEGL
            System.err.println("setSize n/a in BroadcomEGL with realized window");
        } else {
            this.width = width;
            this.height = height;
        }
    }

    public void setPosition(int x, int y) {
        // n/a in BroadcomEGL
        System.err.println("setPosition n/a in BroadcomEGL");
    }

    public boolean setFullscreen(boolean fullscreen) {
        // n/a in BroadcomEGL
        System.err.println("setFullscreen n/a in BroadcomEGL");
        return false;
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    protected static native boolean initIDs();
    private        native long CreateWindow(long eglDisplayHandle, boolean chromaKey, int width, int height);
    private        native void CloseWindow(long eglDisplayHandle, long eglWindowHandle);


    private long realizeWindow(boolean chromaKey, int width, int height) {
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
        GLCapabilities capsReq = (GLCapabilities) config.getRequestedCapabilities();
        config = EGLGraphicsConfiguration.create(capsReq, screen.getGraphicsScreen(), cfgID);
        if (config == null) {
            throw new NativeWindowException("Error creating EGLGraphicsConfiguration from id: "+cfgID+", "+this);
        }
    }

    private long   windowHandleClose;
}

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

package com.sun.javafx.newt.windows;

import javax.media.nwi.NWCapabilities;
import javax.media.nwi.NativeWindowException;

import com.sun.javafx.newt.*;
import com.sun.opengl.impl.*;

public class WindowsWindow extends Window {

    private long hdc;
    private long windowHandleClose;

    private static final String WINDOW_CLASS_NAME = "NewtWindow";
    static {
        NativeLibLoader.loadNEWT();

        if (!initIDs()) {
            throw new NativeWindowException("Failed to initialize jmethodIDs");
        }
    }

    public WindowsWindow() {
    }

    public long getSurfaceHandle() {
        if (hdc == 0) {
            hdc = GetDC(windowHandle);
        }
        return hdc;
    }

    protected void createNative(NWCapabilities caps) {
        long wndClass = getWindowClass();
        chosenCaps = (NWCapabilities) caps.clone(); // FIXME: visualID := f1(caps); caps := f2(visualID)
        visualID = 0; // n/a
        windowHandle = CreateWindow(WINDOW_CLASS_NAME, getHInstance(), visualID, x, y, width, height);
        if (windowHandle == 0) {
            throw new NativeWindowException("Error creating window");
        }
        windowHandleClose = windowHandle;
    }

    protected void closeNative() {
        if (hdc != 0) {
            if(windowHandleClose != 0) {
                ReleaseDC(windowHandleClose, hdc);
            }
            hdc = 0;
        }
        if(windowHandleClose != 0) {
            DestroyWindow(windowHandleClose);
        }
    }

    public void setVisible(boolean visible) {
        if(this.visible!=visible) {
            this.visible=visible;
            setVisible0(windowHandle, visible);
        }
    }

    public void setSize(int width, int height) {
        setSize0(windowHandle, width, height);
    }

    public void setPosition(int x, int y) {
    }

    public boolean setFullscreen(boolean fullscreen) {
        if(this.fullscreen!=fullscreen) {
            boolean res = setFullScreen0(windowHandle, fullscreen);
            if (fullscreen && res) {
                this.fullscreen = true;
            } else {
                this.fullscreen = false;
            }
            return fullscreen;
        }
        return true;
    }

    protected void dispatchMessages(int eventMask) {
        DispatchMessages(windowHandle, eventMask);
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    private static long windowClass;
    private static synchronized long getWindowClass() {
        if (windowClass == 0) {
            windowClass = RegisterWindowClass(WINDOW_CLASS_NAME, getHInstance());
            if (windowClass == 0) {
                throw new NativeWindowException("Error while registering window class");
            }
        }
        return windowClass;
    }
    private static long hInstance;
    private static synchronized long getHInstance() {
        if (hInstance == 0) {
            hInstance = LoadLibraryW("newt");
            if (hInstance == 0) {
                throw new NativeWindowException("Error finding HINSTANCE for \"newt\"");
            }
        }
        return hInstance;
    }

    private static native boolean initIDs();
    private static native long LoadLibraryW(String libraryName);
    private static native long RegisterWindowClass(String windowClassName, long hInstance);
    private        native long CreateWindow(String windowClassName, long hInstance, long visualID,
                                            int x, int y, int width, int height);
    private        native void DestroyWindow(long windowHandle);
    private        native long GetDC(long windowHandle);
    private        native void ReleaseDC(long windowHandle, long hdc);
    private        native void setVisible0(long windowHandle, boolean visible);
    private static native void DispatchMessages(long windowHandle, int eventMask);
    private        native void setSize0(long windowHandle, int width, int height);
    private        native boolean setFullScreen0(long windowHandle, boolean fullscreen);

    private void sizeChanged(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;
        sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED);
    }

    private void positionChanged(int newX, int newY) {
        x = newX;
        y = newY;
        sendWindowEvent(WindowEvent.EVENT_WINDOW_MOVED);
    }

    private void windowClosed() {
    }

    private void windowDestroyed() {
    }
}

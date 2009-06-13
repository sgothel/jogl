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

import javax.media.nativewindow.*;

import com.sun.javafx.newt.*;
import com.sun.javafx.newt.impl.*;

public class WindowsWindow extends Window {

    private long hmon;
    private long hdc;
    private long windowHandleClose;
    // non fullscreen dimensions ..
    private int nfs_width, nfs_height, nfs_x, nfs_y;

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
            hmon = MonitorFromWindow(windowHandle);
            if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
                Exception e = new Exception("!!! Window new surface handle "+Thread.currentThread().getName()+
                                            ",HDC 0x"+Long.toHexString(hdc)+", HMON 0x"+Long.toHexString(hmon));
                e.printStackTrace();
            }
        }
        return hdc;
    }

    public boolean hasDeviceChanged() {
        long _hmon = MonitorFromWindow(windowHandle);
        if (hmon != _hmon) {
            if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
                Exception e = new Exception("!!! Window Device Changed "+Thread.currentThread().getName()+
                                            ", HMON 0x"+Long.toHexString(hmon)+" -> 0x"+Long.toHexString(_hmon));
                e.printStackTrace();
            }
            hmon = _hmon;
            return true;
        }
        return false;
    }

    public void disposeSurfaceHandle() {
        if (hdc != 0) {
            ReleaseDC(windowHandle, hdc);
            hdc=0;
            if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
                Exception e = new Exception("!!! Window surface handle disposed "+Thread.currentThread().getName());
                e.printStackTrace();
            }
        }
    }

    protected void createNative(Capabilities caps) {
        config = GraphicsConfigurationFactory.getFactory(getScreen().getDisplay().getGraphicsDevice()).chooseGraphicsConfiguration(caps, null, getScreen().getGraphicsScreen());
        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        windowHandle = CreateWindow(getWindowClassAtom(), WINDOW_CLASS_NAME, getHInstance(), 0, undecorated, x, y, width, height);
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
            windowHandleClose = 0;
        }
    }

    protected void windowDestroyed() {
        // singleton ATOM CleanupWindowResources(getWindowClassAtom(), getHInstance());
        windowHandleClose = 0;
        super.windowDestroyed();
    }

    public void setVisible(boolean visible) {
        if(this.visible!=visible) {
            this.visible=visible;
            setVisible0(windowHandle, visible);
        }
    }

    // @Override
    public void setSize(int width, int height) {
        if (width != this.width || this.height != height) {
            if(!fullscreen) {
                nfs_width=width;
                nfs_height=height;
            }
            this.width = width;
            this.height = height;
            setSize0(windowHandle, width, height);
        }
    }

    //@Override
    public void setPosition(int x, int y) {
        if (this.x != x || this.y != y) {
            if(!fullscreen) {
                nfs_x=x;
                nfs_y=y;
            }
            this.x = x;
            this.y = y;
            setPosition(windowHandle, x , y);
        }
    }

    public boolean setFullscreen(boolean fullscreen) {
        if(this.fullscreen!=fullscreen) {
            int x,y,w,h;
            this.fullscreen=fullscreen;
            if(fullscreen) {
                x = 0; y = 0;
                w = screen.getWidth();
                h = screen.getHeight();
            } else {
                x = nfs_x;
                y = nfs_y;
                w = nfs_width;
                h = nfs_height;
            }
            if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
                System.err.println("WindowsWindow fs: "+fullscreen+" "+x+"/"+y+" "+w+"x"+h);
            }
            setFullscreen0(windowHandle, x, y, w, h, undecorated, fullscreen);
        }
        return fullscreen;
    }

    // @Override
    public void requestFocus() {
        super.requestFocus();
        if (windowHandle != 0L) {
            requestFocus(windowHandle);
        }
    }

    // @Override
    public void setTitle(String title) {
        if (title == null) {
            title = "";
        }
        if (!title.equals(getTitle())) {
            super.setTitle(title);
            setTitle(windowHandle, title);
        }
    }

    protected void dispatchMessages(int eventMask) {
        DispatchMessages(windowHandle, eventMask);
    }

    //----------------------------------------------------------------------
    // Internals only
    //
    private static final String WINDOW_CLASS_NAME = "NewtWindowClass";

    private static int windowClassAtom = 0;
    private static synchronized int getWindowClassAtom() {
        if(0 == windowClassAtom) {
            windowClassAtom = RegisterWindowClass(WINDOW_CLASS_NAME, getHInstance());
            if (windowClassAtom == 0) {
                throw new NativeWindowException("Error while registering window class");
            }
        }
        return windowClassAtom;
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
    private static native int  RegisterWindowClass(String windowClassName, long hInstance);
    private        native long CreateWindow(int wndClassAtom, String wndName, 
                                            long hInstance, long visualID,
                                            boolean isUndecorated,
                                            int x, int y, int width, int height);
    private        native void CleanupWindowResources(int wndClassAtom, long hInstance);
    private        native void DestroyWindow(long windowHandle);
    private        native long GetDC(long windowHandle);
    private        native void ReleaseDC(long windowHandle, long hdc);
    private        native long MonitorFromWindow(long windowHandle);
    private static native void setVisible0(long windowHandle, boolean visible);
    private static native void DispatchMessages(long windowHandle, int eventMask);
    private        native void setSize0(long windowHandle, int width, int height);
    private        native void setPosition(long windowHandle, int x, int y);
    private        native void setFullscreen0(long windowHandle, int x, int y, int width, int height, boolean isUndecorated, boolean on);
    private static native void setTitle(long windowHandle, String title);
    private static native void requestFocus(long windowHandle);

    private void sizeChanged(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;
        if(!fullscreen) {
            nfs_width=width;
            nfs_height=height;
        }
        sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED);
    }

    private void positionChanged(int newX, int newY) {
        x = newX;
        y = newY;
        if(!fullscreen) {
            nfs_x=x;
            nfs_y=y;
        }
        sendWindowEvent(WindowEvent.EVENT_WINDOW_MOVED);
    }

    /**
     *
     * @param focusOwner if focusGained is true, focusOwner is the previous
     * focus owner, if focusGained is false, focusOwner is the new focus owner
     * @param focusGained
     */
    private void focusChanged(long focusOwner, boolean focusGained) {
        if (focusGained) {
            sendWindowEvent(WindowEvent.EVENT_WINDOW_GAINED_FOCUS);
        } else {
            sendWindowEvent(WindowEvent.EVENT_WINDOW_LOST_FOCUS);
        }
    }
}

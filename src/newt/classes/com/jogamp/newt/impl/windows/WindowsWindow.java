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

package com.jogamp.newt.impl.windows;

import javax.media.nativewindow.*;
import com.jogamp.newt.*;
import com.jogamp.newt.util.*;
import com.jogamp.newt.impl.WindowImpl;

public class WindowsWindow extends WindowImpl {

    private long hmon;
    private long hdc;
    private long windowHandleClose;
    private final Insets insets = new Insets(0, 0, 0, 0);

    static {
        WindowsDisplay.initSingleton();
    }

    public WindowsWindow() {
    }

    protected int lockSurfaceImpl() {
        if( 0 != getWindowHandle() && 0 == hdc ) {
            hdc = GetDC0(getWindowHandle());
            hmon = MonitorFromWindow0(getWindowHandle());
        }
        return LOCK_SUCCESS;
    }

    protected void unlockSurfaceImpl() {
        if ( 0 != hdc && 0 != getWindowHandle() && getWindowLockRecursionCount() == 0) {
            ReleaseDC0(getWindowHandle(), hdc);
            hdc=0;
        }
    }

    public final long getSurfaceHandle() {
        return hdc;
    }

    public boolean hasDeviceChanged() {
        if(0!=getWindowHandle()) {
            long _hmon = MonitorFromWindow0(getWindowHandle());
            if (hmon != _hmon) {
                if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
                    Exception e = new Exception("!!! Window Device Changed "+Thread.currentThread().getName()+
                                                ", HMON "+toHexString(hmon)+" -> "+toHexString(_hmon));
                    e.printStackTrace();
                }
                hmon = _hmon;
                return true;
            }
        }
        return false;
    }

    protected void createNativeImpl() {
        WindowsScreen  screen = (WindowsScreen) getScreen();
        WindowsDisplay display = (WindowsDisplay) screen.getDisplay();
        config = GraphicsConfigurationFactory.getFactory(display.getGraphicsDevice()).chooseGraphicsConfiguration(caps, null, screen.getGraphicsScreen());
        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        setWindowHandle(CreateWindow0(getParentWindowHandle(),
                                    display.getWindowClassAtom(), display.WINDOW_CLASS_NAME, display.getHInstance(), 
                                    0, undecorated, x, y, width, height));
        if (getWindowHandle() == 0) {
            throw new NativeWindowException("Error creating window");
        }
        windowHandleClose = getWindowHandle();
        if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
            Exception e = new Exception("!!! Window new window handle "+Thread.currentThread().getName()+
                                        " (Parent HWND "+toHexString(getParentWindowHandle())+
                                        ") : HWND "+toHexString(getWindowHandle())+", "+Thread.currentThread());
            e.printStackTrace();
        }
    }

    protected void closeNativeImpl() {
        if (hdc != 0) {
            if(windowHandleClose != 0) {
                try {
                    ReleaseDC0(windowHandleClose, hdc);
                } catch (Throwable t) {
                    if(DEBUG_IMPLEMENTATION) { 
                        Exception e = new Exception("closeNativeImpl failed - "+Thread.currentThread().getName(), t);
                        e.printStackTrace();
                    }
                }
            }
            hdc = 0;
        }
        if(windowHandleClose != 0) {
            try {
                DestroyWindow0(windowHandleClose);
            } catch (Throwable t) {
                if(DEBUG_IMPLEMENTATION) {
                    Exception e = new Exception("closeNativeImpl failed - "+Thread.currentThread().getName(), t);
                    e.printStackTrace();
                }
            } finally {
                windowHandleClose = 0;
            }
        }
    }

    protected void windowDestroyed() {
        windowHandleClose = 0;
        super.windowDestroyed();
    }

    protected void setVisibleImpl(boolean visible) {
        setVisible0(getWindowHandle(), visible);
    }

    protected void setSizeImpl(int width, int height) {
        // this width/height will be set by sizeChanged, called by Windows
        setSize0(getParentWindowHandle(), getWindowHandle(), x, y, width, height);
    }

    protected void setPositionImpl(int x, int y) {
        // this x/y will be set by positionChanged, called by Windows
        setPosition0(getParentWindowHandle(), getWindowHandle(), x , y /*, width, height*/);
    }

    protected void reconfigureWindowImpl(int x, int y, int width, int height) {
        reconfigureWindow0(fullscreen?0:getParentWindowHandle(), getWindowHandle(), x, y, width, height, isUndecorated(fullscreen));
    }

    protected boolean reparentWindowImpl() {
        reparentWindow0(fullscreen?0:getParentWindowHandle(), getWindowHandle(), x, y, width, height, isUndecorated());
        return true;
    }

    protected void requestFocusImpl(boolean reparented) {
        requestFocus0(getWindowHandle(), reparented);
    }

    protected void setTitleImpl(final String title) {
        setTitle0(getWindowHandle(), title);
    }

    public Insets getInsets() {
        return (Insets)insets.clone();
    }

    //----------------------------------------------------------------------
    // Internals only
    //
    protected static native boolean initIDs0();
    private        native long CreateWindow0(long parentWindowHandle, 
                                            int wndClassAtom, String wndName, 
                                            long hInstance, long visualID,
                                            boolean isUndecorated,
                                            int x, int y, int width, int height);
    private        native void DestroyWindow0(long windowHandle);
    private        native long GetDC0(long windowHandle);
    private        native void ReleaseDC0(long windowHandle, long hdc);
    private        native long MonitorFromWindow0(long windowHandle);
    private static native void setVisible0(long windowHandle, boolean visible);
    private        native void setSize0(long parentWindowHandle, long windowHandle, int x, int y, int width, int height);
    private static native void setPosition0(long parentWindowHandle, long windowHandle, int x, int y /*, int width, int height*/);
    private        native void reconfigureWindow0(long parentWindowHandle, long windowHandle, 
                                                  int x, int y, int width, int height, boolean isUndecorated);
    private        native void reparentWindow0(long parentWindowHandle, long windowHandle, int x, int y, int width, int height, boolean isUndecorated);
    private static native void setTitle0(long windowHandle, String title);
    private        native void requestFocus0(long windowHandle, boolean reparented);

    private void insetsChanged(int left, int top, int right, int bottom) {
        if (left != -1 && top != -1 && right != -1 && bottom != -1) {
            insets.left = left;
            insets.top = top;
            insets.right = right;
            insets.bottom = bottom;
        }
    }
}

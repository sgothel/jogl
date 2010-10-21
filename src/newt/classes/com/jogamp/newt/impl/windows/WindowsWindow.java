/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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
import com.jogamp.newt.impl.WindowImpl;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.Point;

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
                    Exception e = new Exception("Info: Window Device Changed "+Thread.currentThread().getName()+
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
            Exception e = new Exception("Info: Window new window handle "+Thread.currentThread().getName()+
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
                        Exception e = new Exception("Warning: closeNativeImpl failed - "+Thread.currentThread().getName(), t);
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
                    Exception e = new Exception("Warning: closeNativeImpl failed - "+Thread.currentThread().getName(), t);
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

    protected void setVisibleImpl(boolean visible, int x, int y, int width, int height) {
        setVisible0(getWindowHandle(), visible, (getParentWindowHandle()==0)?true:false, x, y, width, height);
        visibleChanged(visible);
    }

    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, 
                                            boolean parentChange, int fullScreenChange, int decorationChange) {
        reconfigureWindow0( (fullScreenChange>0)?0:getParentWindowHandle(), 
                             getWindowHandle(), x, y, width, height, isVisible(), parentChange, fullScreenChange, decorationChange);
        return true;
    }

    protected void requestFocusImpl(boolean force) {
        requestFocus0(getWindowHandle(), force);
    }

    protected void setTitleImpl(final String title) {
        setTitle0(getWindowHandle(), title);
    }

    public Insets getInsets() {
        return (Insets)insets.clone();
    }

    protected Point getLocationOnScreenImpl(int x, int y) {
        return (Point) getRelativeLocation0( getWindowHandle(), 0 /*root win*/, x, y);
    }

    //----------------------------------------------------------------------
    // Internals only
    //
    protected static native boolean initIDs0();
    private native long CreateWindow0(long parentWindowHandle, 
                                            int wndClassAtom, String wndName, 
                                            long hInstance, long visualID,
                                            boolean isUndecorated,
                                            int x, int y, int width, int height);
    private native void DestroyWindow0(long windowHandle);
    private native long GetDC0(long windowHandle);
    private native void ReleaseDC0(long windowHandle, long hdc);
    private native long MonitorFromWindow0(long windowHandle);
    private native void setVisible0(long windowHandle, boolean visible, boolean top, int x, int y, int width, int height);
    private native void reconfigureWindow0(long parentWindowHandle, long windowHandle, 
                                                  int x, int y, int width, int height, boolean isVisible,
                                                  boolean parentChange, int fullScreenChange, int decorationChange);
    private static native void setTitle0(long windowHandle, String title);
    private native void requestFocus0(long windowHandle, boolean force);
    private native Object getRelativeLocation0(long src_win, long dest_win, int src_x, int src_y);

    private void insetsChanged(int left, int top, int right, int bottom) {
        if (left != -1 && top != -1 && right != -1 && bottom != -1) {
            if (left != insets.left || top != insets.top || right != insets.right || bottom != insets.bottom) {
                insets.left = left;
                insets.top = top;
                insets.right = right;
                insets.bottom = bottom;
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window.insetsChanged: "+insets);
                }
            }
        }
    }
}

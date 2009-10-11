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

package com.sun.javafx.newt.x11;

import com.sun.javafx.newt.*;
import com.sun.javafx.newt.impl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;

public class X11Window extends Window {
    private static final String WINDOW_CLASS_NAME = "NewtWindow";
    // non fullscreen dimensions ..
    private int nfs_width, nfs_height, nfs_x, nfs_y;

    static {
        X11Display.initSingleton();
    }

    public X11Window() {
    }

    protected void createNative(long parentWindowHandle, Capabilities caps) {
        X11Screen screen = (X11Screen) getScreen();
        X11Display display = (X11Display) screen.getDisplay();
        config = GraphicsConfigurationFactory.getFactory(display.getGraphicsDevice()).chooseGraphicsConfiguration(caps, null, screen.getGraphicsScreen());
        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        X11GraphicsConfiguration x11config = (X11GraphicsConfiguration) config;
        long visualID = x11config.getVisualID();
        long w = CreateWindow(parentWindowHandle, 
                              display.getHandle(), screen.getIndex(), visualID, 
                              display.getJavaObjectAtom(), display.getWindowDeleteAtom(), x, y, width, height);
        if (w == 0 || w!=windowHandle) {
            throw new NativeWindowException("Error creating window: "+w);
        }
        this.parentWindowHandle = parentWindowHandle;
        windowHandleClose = windowHandle;
        displayHandleClose = display.getHandle();
    }

    protected void closeNative() {
        if(0!=displayHandleClose && 0!=windowHandleClose && null!=getScreen() ) {
            X11Display display = (X11Display) getScreen().getDisplay();
            CloseWindow(displayHandleClose, windowHandleClose, display.getJavaObjectAtom());
            windowHandleClose = 0;
            displayHandleClose = 0;
        }
    }

    protected void windowDestroyed() {
        windowHandleClose = 0;
        displayHandleClose = 0;
        super.windowDestroyed();
    }

    public void setVisible(boolean visible) {
        if(0!=windowHandle && this.visible!=visible) {
            this.visible=visible;
            setVisible0(getDisplayHandle(), windowHandle, visible);
            clearEventMask();
        }
    }

    public void requestFocus() {
        super.requestFocus();
    }

    public void setSize(int width, int height) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("X11Window setSize: "+this.x+"/"+this.y+" "+this.width+"x"+this.height+" -> "+width+"x"+height);
            // Exception e = new Exception("XXXXXXXXXX");
            // e.printStackTrace();
        }
        this.width = width;
        this.height = height;
        if(!fullscreen) {
            nfs_width=width;
            nfs_height=height;
        }
        if(0!=windowHandle) {
            setSize0(parentWindowHandle, getDisplayHandle(), getScreenIndex(), windowHandle, x, y, width, height, (undecorated||fullscreen)?-1:1, false);
        }
    }

    public void setPosition(int x, int y) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("X11Window setPosition: "+this.x+"/"+this.y+" -> "+x+"/"+y);
            // Exception e = new Exception("XXXXXXXXXX");
            // e.printStackTrace();
        }
        this.x = x;
        this.y = y;
        if(!fullscreen) {
            nfs_x=x;
            nfs_y=y;
        }
        if(0!=windowHandle) {
            setPosition0(getDisplayHandle(), windowHandle, x, y);
        }
    }

    public boolean setFullscreen(boolean fullscreen) {
        if(0!=windowHandle && this.fullscreen!=fullscreen) {
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
                System.err.println("X11Window fs: "+fullscreen+" "+x+"/"+y+" "+w+"x"+h);
            }
            setSize0(parentWindowHandle, getDisplayHandle(), getScreenIndex(), windowHandle, x, y, w, h, (undecorated||fullscreen)?-1:1, false);
        }
        return fullscreen;
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    protected static native boolean initIDs();
    private        native long CreateWindow(long parentWindowHandle, long display, int screen_index, 
                                            long visualID, long javaObjectAtom, long windowDeleteAtom, int x, int y, int width, int height);
    private        native void CloseWindow(long display, long windowHandle, long javaObjectAtom);
    private        native void setVisible0(long display, long windowHandle, boolean visible);
    private        native void setSize0(long parentWindowHandle, long display, int screen_index, long windowHandle, 
                                        int x, int y, int width, int height, int decorationToggle, boolean setVisible);
    private        native void setPosition0(long display, long windowHandle, int x, int y);

    private void windowChanged(int newX, int newY, int newWidth, int newHeight) {
        if(width != newWidth || height != newHeight) {
            if(DEBUG_IMPLEMENTATION) {
                System.err.println("X11Window windowChanged size: "+this.width+"x"+this.height+" -> "+newWidth+"x"+newHeight);
            }
            width = newWidth;
            height = newHeight;
            if(!fullscreen) {
                nfs_width=width;
                nfs_height=height;
            }
            sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED);
        }
        if( 0==parentWindowHandle && ( x != newX || y != newY ) ) {
            if(DEBUG_IMPLEMENTATION) {
                System.err.println("X11Window windowChanged position: "+this.x+"/"+this.y+" -> "+newX+"x"+newY);
            }
            x = newX;
            y = newY;
            if(!fullscreen) {
                nfs_x=x;
                nfs_y=y;
            }
            sendWindowEvent(WindowEvent.EVENT_WINDOW_MOVED);
        }
    }

    private void windowCreated(long windowHandle) {
        this.windowHandle = windowHandle;
    }

    private long   windowHandleClose;
    private long   displayHandleClose;
    private long   parentWindowHandle;
}

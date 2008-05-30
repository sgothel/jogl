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
import com.sun.opengl.impl.*;

public class X11Window extends Window {
    private Screen screen;
    private long visualID;
    private long dpy, scrn, window;
    private int  scrn_idx;
    private static final String WINDOW_CLASS_NAME = "NewtWindow";
    // Default width and height -- will likely be re-set immediately by user
    private int width  = 100;
    private int height = 100;
    private int x=0;
    private int y=0;
    // non fullscreen dimensions ..
    private boolean fullscreen, visible;
    private int nfs_width, nfs_height, nfs_x, nfs_y;

    static {
        NativeLibLoader.loadCore();

        if (!initIDs()) {
            throw new RuntimeException("Failed to initialize jmethodIDs");
        }
    }

    public X11Window() {
    }

    public void initNative(Screen screen, long visualID) {
        this.screen = screen;
        this.visualID = visualID;
        fullscreen=false;
        visible=false;
        long w = CreateWindow(visualID, x, y, width, height);
        if (w == 0 || w!=window) {
            throw new RuntimeException("Error creating window: "+w);
        }
        screen.setHandle(scrn);
        screen.getDisplay().setHandle(dpy);
    }

    public Screen getScreen() {
        return screen;
    }

    public void setVisible(boolean visible) {
        if(this.visible!=visible) {
            this.visible=visible;
            setVisible0(dpy, window, visible);
        }
    }

    public void setSize(int width, int height) {
        setSize0(dpy, window, width, height);
    }

    public void setPosition(int x, int y) {
        setPosition0(dpy, window, x, y);
    }

    public boolean isVisible() {
        return visible;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean setFullscreen(boolean fullscreen) {
        if(this.fullscreen!=fullscreen) {
            int x,y,w,h;
            this.fullscreen=fullscreen;
            if(this.fullscreen) {
                x = 0; y = 0;
                w = getDisplayWidth0(dpy, scrn_idx)/2;
                h = getDisplayHeight0(dpy, scrn_idx)/2;
            } else {
                x = nfs_x;
                y = nfs_y;
                w = nfs_width;
                h = nfs_height;
            }
            setPosition0(dpy, window, x, y);
            setSize0(dpy, window, w, h);
        }
        return true;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public long getWindowHandle() {
        return window;
    }

    public void pumpMessages() {
        DispatchMessages(dpy, window);
    }

    public int getDisplayWidth() {
        return getDisplayWidth0(dpy, scrn_idx);
    }

    public int getDisplayHeight() {
        return getDisplayHeight0(dpy, scrn_idx);
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    private static native boolean initIDs();
    private        native long CreateWindow(long visualID, int x, int y, int width, int height);
    private        native void setVisible0(long display, long window, boolean visible);
    private        native void DispatchMessages(long display, long window);
    private        native void setSize0(long display, long window, int width, int height);
    private        native void setPosition0(long display, long window, int x, int y);
    private        native int  getDisplayWidth0(long display, int scrn_idx);
    private        native int  getDisplayHeight0(long display, int scrn_idx);

    private void sizeChanged(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;
        if(!fullscreen) {
            nfs_width=width;
            nfs_height=height;
        }
    }

    private void positionChanged(int newX, int newY) {
        x = newX;
        y = newY;
        if(!fullscreen) {
            nfs_x=x;
            nfs_y=y;
        }
    }

    private void windowCreated(long dpy, int scrn_idx, long scrn, long window) {
        this.dpy = dpy;
        this.scrn_idx = scrn_idx;
        this.scrn = scrn;
        this.window = window;
    }

    private void windowClosed() {
    }

    private void windowDestroyed() {
    }

}

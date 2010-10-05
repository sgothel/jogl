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

package com.jogamp.newt.impl.intel.gdl;

import javax.media.nativewindow.*;

public class Window extends com.jogamp.newt.impl.WindowImpl {
    static {
        Display.initSingleton();
    }

    public Window() {
    }

    static long nextWindowHandle = 1;

    protected void createNativeImpl() {
        if(0!=getParentWindowHandle()) {
            throw new NativeWindowException("GDL Window does not support window parenting");
        }
        AbstractGraphicsScreen aScreen = getScreen().getGraphicsScreen();
        AbstractGraphicsDevice aDevice = getScreen().getDisplay().getGraphicsDevice();

        config = GraphicsConfigurationFactory.getFactory(aDevice).chooseGraphicsConfiguration(caps, null, aScreen);
        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }

        synchronized(Window.class) {
            setWindowHandle(nextWindowHandle++); // just a marker

            surfaceHandle = CreateSurface(aDevice.getHandle(), getScreen().getWidth(), getScreen().getHeight(), x, y, width, height);
            if (surfaceHandle == 0) {
                throw new NativeWindowException("Error creating window");
            }
        }
    }

    protected void closeNativeImpl() {
        if(0!=surfaceHandle) {
            synchronized(Window.class) {
                CloseSurface(getDisplayHandle(), surfaceHandle);
            }
            surfaceHandle = 0;
            ((Display)getScreen().getDisplay()).setFocus(null);
        }
    }

    protected void setVisibleImpl(boolean visible) {
        if(visible) {
            ((Display)getScreen().getDisplay()).setFocus(this);
        }
    }

    protected void setSizeImpl(int width, int height) {
        Screen  screen = (Screen) getScreen();
        if((x+width)>screen.getWidth()) {
            width=screen.getWidth()-x;
        }
        if((y+height)>screen.getHeight()) {
            height=screen.getHeight()-y;
        }
        if(0!=surfaceHandle) {
            SetBounds0(surfaceHandle, screen.getWidth(), screen.getHeight(), x, y, width, height);
        }
    }

    protected void setPositionImpl(int x, int y) {
        Screen  screen = (Screen) getScreen();
        if((x+width)>screen.getWidth()) {
            x=screen.getWidth()-width;
        }
        if((y+height)>screen.getHeight()) {
            y=screen.getHeight()-height;
        }
        if(0!=surfaceHandle) {
            SetBounds0(surfaceHandle, screen.getWidth(), screen.getHeight(), x, y, width, height);
        }
    }

    protected void reconfigureWindowImpl(int x, int y, int width, int height) {
        if(0!=surfaceHandle) {
            SetBounds0(surfaceHandle, getScreen().getWidth(), getScreen().getHeight(), x, y, width, height);
        }
    }

    protected void requestFocusImpl(boolean reparented) {
        ((Display)getScreen().getDisplay()).setFocus(this);
    }

    public final long getSurfaceHandle() {
        return surfaceHandle;
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    protected static native boolean initIDs();
    private        native long CreateSurface(long displayHandle, int scrn_width, int scrn_height, int x, int y, int width, int height);
    private        native void CloseSurface(long displayHandle, long surfaceHandle);
    private        native void SetBounds0(long surfaceHandle, int scrn_width, int scrn_height, int x, int y, int width, int height);

    private void updateBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    private long   surfaceHandle;
    private int nfs_width, nfs_height, nfs_x, nfs_y;
}

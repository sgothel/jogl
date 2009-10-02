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

package com.sun.javafx.newt.intel.gdl;

import javax.media.nativewindow.*;

public class Window extends com.sun.javafx.newt.Window {
    static {
        Display.initSingleton();
    }

    public Window() {
    }

    static long nextWindowHandle = 1;

    protected void createNative(long parentWindowHandle, Capabilities caps) {
        if(0!=parentWindowHandle) {
            throw new NativeWindowException("GDL Window does not support window parenting");
        }
        AbstractGraphicsScreen aScreen = screen.getGraphicsScreen();
        AbstractGraphicsDevice aDevice = screen.getDisplay().getGraphicsDevice();

        config = GraphicsConfigurationFactory.getFactory(aDevice).chooseGraphicsConfiguration(caps, null, aScreen);
        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }

        synchronized(Window.class) {
            windowHandle = nextWindowHandle++;
        }
    }

    protected void closeNative() {
        if(0!=surfaceHandle) {
            synchronized(Window.class) {
                CloseSurface(getDisplayHandle(), surfaceHandle);
            }
            surfaceHandle = 0;
            ((Display)screen.getDisplay()).setFocus(null);
        }
    }

    public void setVisible(boolean visible) {
        if(this.visible!=visible) {
            this.visible=visible;
            if(visible && 0==surfaceHandle) {
                synchronized(Window.class) {
                    AbstractGraphicsDevice aDevice = screen.getDisplay().getGraphicsDevice();
                    surfaceHandle = CreateSurface(aDevice.getHandle(), screen.getWidth(), screen.getHeight(), x, y, width, height);
                }
                if (surfaceHandle == 0) {
                    throw new NativeWindowException("Error creating window");
                }
                ((Display)screen.getDisplay()).setFocus(this);
            }
        }
    }

    public void setSize(int width, int height) {
        if(0!=surfaceHandle) {
            System.err.println("setSize "+width+"x"+height+" n/a in IntelGDL _after_ surface established");
        } else {
            Screen  screen = (Screen) getScreen();
            if(width>screen.getWidth() || height>screen.getHeight()) {
                width=screen.getWidth();
                height=screen.getHeight();
            }
            this.width = width;
            this.height = height;
        }
    }

    public void setPosition(int x, int y) {
        // n/a in IntelGDL
        System.err.println("setPosition n/a in IntelGDL");
    }

    public boolean setFullscreen(boolean fullscreen) {
        // n/a in IntelGDL
        System.err.println("setFullscreen n/a in IntelGDL");
        return false;
    }

    public void requestFocus() {
        ((Display)screen.getDisplay()).setFocus(this);
    }

    public long getSurfaceHandle() {
        return surfaceHandle;
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    protected static native boolean initIDs();
    private        native long CreateSurface(long displayHandle, int width, int height);
    private        native void CloseSurface(long displayHandle, long surfaceHandle);


    private void updateBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    private long   surfaceHandle;
}

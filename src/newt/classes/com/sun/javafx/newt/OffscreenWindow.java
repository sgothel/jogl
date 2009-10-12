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

package com.sun.javafx.newt;

import javax.media.nativewindow.*;

public class OffscreenWindow extends Window implements SurfaceChangeable {

    long surfaceHandle = 0;

    public OffscreenWindow() {
    }

    static long nextWindowHandle = 0x100; // start here - a marker

    protected void createNative(long parentWindowHandle, Capabilities caps) {
        if(0!=parentWindowHandle) {
            throw new NativeWindowException("OffscreenWindow does not support window parenting");
        }
        if(caps.isOnscreen()) {
            throw new NativeWindowException("Capabilities is onscreen");
        }
        AbstractGraphicsScreen aScreen = screen.getGraphicsScreen();
        config = GraphicsConfigurationFactory.getFactory(aScreen.getDevice()).chooseGraphicsConfiguration(caps, null, aScreen);
        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }

        synchronized(OffscreenWindow.class) {
            windowHandle = nextWindowHandle++;
        }
    }

    protected void closeNative() {
        // nop
    }

    public void invalidate() {
        super.invalidate();
        surfaceHandle = 0;
    }

    public synchronized void destroy() {
        surfaceHandle = 0;
    }

    public void setSurfaceHandle(long handle) {
        surfaceHandle = handle ;
    }

    public long getSurfaceHandle() {
        return surfaceHandle;
    }

    public void setVisible(boolean visible) {
        if(!visible) {
            this.visible = visible;
        }
    }

    public void setSize(int width, int height) {
        if(!visible) {
            this.width = width;
            this.height = height;
        }
    }

    public void setPosition(int x, int y) {
        // nop
    }

    public boolean setFullscreen(boolean fullscreen) {
        // nop
        return false;
    }
}


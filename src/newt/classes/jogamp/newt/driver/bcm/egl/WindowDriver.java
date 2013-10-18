/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2012 JogAmp Community. All rights reserved.
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

package jogamp.newt.driver.bcm.egl;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.VisualIDHolder;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.Point;
import javax.media.opengl.GLCapabilitiesImmutable;

import jogamp.opengl.egl.EGLGraphicsConfiguration;

public class WindowDriver extends jogamp.newt.WindowImpl {
    static {
        DisplayDriver.initSingleton();
    }

    public WindowDriver() {
    }

    protected void createNativeImpl() {
        if(0!=getParentWindowHandle()) {
            throw new RuntimeException("Window parenting not supported (yet)");
        }
        // query a good configuration, however chose the final one by the native queried egl-cfg-id
        // after creation at {@link #windowCreated(int, int, int)}.
        final AbstractGraphicsConfiguration cfg = GraphicsConfigurationFactory.getFactory(getScreen().getDisplay().getGraphicsDevice(), capsRequested).chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, getScreen().getGraphicsScreen(), VisualIDHolder.VID_UNDEFINED);
        if (null == cfg) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        setGraphicsConfiguration(cfg);
        setSizeImpl(getScreen().getWidth(), getScreen().getHeight());

        setWindowHandle(realizeWindow(true, getWidth(), getHeight()));
        if (0 == getWindowHandle()) {
            throw new NativeWindowException("Error native Window Handle is null");
        }
    }

    protected void closeNativeImpl() {
        if(0!=windowHandleClose) {
            CloseWindow(getDisplayHandle(), windowHandleClose);
        }
    }

    protected void requestFocusImpl(boolean reparented) { }

    protected void setSizeImpl(int width, int height) {
        if(0!=getWindowHandle()) {
            // n/a in BroadcomEGL
            System.err.println("BCEGL Window.setSizeImpl n/a in BroadcomEGL with realized window");
        } else {
            defineSize(width, height);
        }
    }

    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, int flags) {
        if(0!=getWindowHandle()) {
            if(0 != ( FLAG_CHANGE_FULLSCREEN & flags)) {
                if( 0 != ( FLAG_IS_FULLSCREEN & flags) ) {
                    // n/a in BroadcomEGL
                    System.err.println("setFullscreen n/a in BroadcomEGL");
                    return false;
                }
            }
        }
        if(width>0 || height>0) {
            if(0!=getWindowHandle()) {
                // n/a in BroadcomEGL
                System.err.println("BCEGL Window.setSizeImpl n/a in BroadcomEGL with realized window");
            } else {
                defineSize((width>0)?width:getWidth(), (height>0)?height:getHeight());
            }
        }
        if(x>=0 || y>=0) {
            System.err.println("BCEGL Window.setPositionImpl n/a in BroadcomEGL");
        }

        if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) ) {
            visibleChanged(false, 0 != ( FLAG_IS_VISIBLE & flags));
        }
        return true;
    }

    protected Point getLocationOnScreenImpl(int x, int y) {
        return new Point(x,y);
    }

    protected void updateInsetsImpl(Insets insets) {
        // nop ..
    }

    @Override
    public boolean surfaceSwap() {
        SwapWindow(getDisplayHandle(), getWindowHandle());
        return true;
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    protected static native boolean initIDs();
    private        native long CreateWindow(long eglDisplayHandle, boolean chromaKey, int width, int height);
    private        native void CloseWindow(long eglDisplayHandle, long eglWindowHandle);
    private        native void SwapWindow(long eglDisplayHandle, long eglWindowHandle);


    private long realizeWindow(boolean chromaKey, int width, int height) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("BCEGL Window.realizeWindow() with: chroma "+chromaKey+", "+width+"x"+height+", "+getGraphicsConfiguration());
        }
        long handle = CreateWindow(getDisplayHandle(), chromaKey, width, height);
        if (0 == handle) {
            throw new NativeWindowException("Error native Window Handle is null");
        }
        windowHandleClose = handle;
        return handle;
    }

    private void windowCreated(int cfgID, int width, int height) {
        defineSize(width, height);
        GLCapabilitiesImmutable capsReq = (GLCapabilitiesImmutable) getGraphicsConfiguration().getRequestedCapabilities();
        final AbstractGraphicsConfiguration cfg = EGLGraphicsConfiguration.create(capsReq, getScreen().getGraphicsScreen(), cfgID);
        if (null == cfg) {
            throw new NativeWindowException("Error creating EGLGraphicsConfiguration from id: "+cfgID+", "+this);
        }
        setGraphicsConfiguration(cfg);
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("BCEGL Window.windowCreated(): "+toHexString(cfgID)+", "+width+"x"+height+", "+cfg);
        }
    }

    private long   windowHandleClose;
}

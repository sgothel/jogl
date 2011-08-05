/**
 * Copyright 2011 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package jogamp.newt.driver.android;

import jogamp.opengl.egl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.util.Point;
import javax.media.opengl.GLCapabilitiesImmutable;

import android.content.Context;

public class Window extends jogamp.newt.WindowImpl {
    static {
        Display.initSingleton();
    }

    public Window() {
    }

    public Window(Context ctx) {
    }

    public static Class[] getCustomConstructorArgumentTypes() {
        return new Class[] { Context.class } ;
    }
    
    protected void createNativeImpl() {
        if(0!=getParentWindowHandle()) {
            throw new RuntimeException("Window parenting not supported (yet)");
        }
        // query a good configuration .. even thought we drop this one 
        // and reuse the EGLUtil choosen one later.
        config = GraphicsConfigurationFactory.getFactory(getScreen().getDisplay().getGraphicsDevice()).chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, getScreen().getGraphicsScreen());
        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        setSizeImpl(getScreen().getWidth(), getScreen().getHeight());

        setWindowHandle(realizeWindow(true, width, height));
        if (0 == getWindowHandle()) {
            throw new NativeWindowException("Error native Window Handle is null");
        }
    }

    protected void closeNativeImpl() {
        if(0!=windowHandleClose) {
            CloseWindow(getDisplayHandle(), windowHandleClose);
        }
    }

    protected void setVisibleImpl(boolean visible, int x, int y, int width, int height) {
        reconfigureWindowImpl(x, y, width, height, false, 0, 0);
        visibleChanged(visible);
    }

    protected void requestFocusImpl(boolean reparented) { }

    protected void setSizeImpl(int width, int height) {
        if(0!=getWindowHandle()) {
            // n/a in BroadcomEGL
            System.err.println("BCEGL Window.setSizeImpl n/a in BroadcomEGL with realized window");
        } else {
            this.width = width;
            this.height = height;
        }
    }

    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, 
                                            boolean parentChange, int fullScreenChange, int decorationChange) {
        if(0!=getWindowHandle()) {
            if(0!=fullScreenChange) {
                if( fullScreenChange > 0 ) {
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
                this.width=(width>0)?width:this.width;
                this.height=(height>0)?height:this.height;
            }
        }
        if(x>=0 || y>=0) {
            System.err.println("BCEGL Window.setPositionImpl n/a in BroadcomEGL");
        }
        return true;
    }

    protected Point getLocationOnScreenImpl(int x, int y) {
        return new Point(x,y);
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
            System.err.println("BCEGL Window.realizeWindow() with: chroma "+chromaKey+", "+width+"x"+height+", "+config);
        }
        long handle = CreateWindow(getDisplayHandle(), chromaKey, width, height);
        if (0 == handle) {
            throw new NativeWindowException("Error native Window Handle is null");
        }
        windowHandleClose = handle;
        return handle;
    }

    private void windowCreated(int cfgID, int width, int height) {
        this.width = width;
        this.height = height;
        GLCapabilitiesImmutable capsReq = (GLCapabilitiesImmutable) config.getRequestedCapabilities();
        config = EGLGraphicsConfiguration.create(capsReq, getScreen().getGraphicsScreen(), cfgID);
        if (config == null) {
            throw new NativeWindowException("Error creating EGLGraphicsConfiguration from id: "+cfgID+", "+this);
        }
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("BCEGL Window.windowCreated(): "+toHexString(cfgID)+", "+width+"x"+height+", "+config);
        }
    }

    private long   windowHandleClose;
}

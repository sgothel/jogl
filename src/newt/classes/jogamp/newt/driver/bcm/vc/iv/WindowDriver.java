/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

package jogamp.newt.driver.bcm.vc.iv;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.Capabilities;
import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.VisualIDHolder;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.Point;

import com.jogamp.nativewindow.egl.EGLGraphicsDevice;

import jogamp.newt.WindowImpl;
import jogamp.newt.driver.linux.LinuxEventDeviceTracker;
import jogamp.newt.driver.linux.LinuxMouseTracker;
import jogamp.opengl.egl.EGLDisplayUtil;

public class WindowDriver extends WindowImpl {
    private static final String WINDOW_CLASS_NAME = "NewtWindow";

    static {
        DisplayDriver.initSingleton();
    }

    public WindowDriver() {
    }

    protected void createNativeImpl() {
        if(0!=getParentWindowHandle()) {
            throw new RuntimeException("Window parenting not supported (yet)");
        }
        // Create own screen/device resource instance allowing independent ownership,
        // while still utilizing shared EGL resources.
        final AbstractGraphicsScreen aScreen = getScreen().getGraphicsScreen();
        final EGLGraphicsDevice aDevice = (EGLGraphicsDevice) aScreen.getDevice();
        final EGLGraphicsDevice eglDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(aDevice.getNativeDisplayID(), aDevice.getConnection(), aDevice.getUnitID());
        final DefaultGraphicsScreen eglScreen = new DefaultGraphicsScreen(eglDevice, aScreen.getIndex());

        final AbstractGraphicsConfiguration cfg = GraphicsConfigurationFactory.getFactory(getScreen().getDisplay().getGraphicsDevice(), capsRequested).chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, eglScreen, VisualIDHolder.VID_UNDEFINED);
        if (null == cfg) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        final Capabilities chosenCaps = (Capabilities) cfg.getChosenCapabilities();
        // FIXME: Pass along opaque flag, since EGL doesn't determine it
        if(capsRequested.isBackgroundOpaque() != chosenCaps.isBackgroundOpaque()) {
            chosenCaps.setBackgroundOpaque(capsRequested.isBackgroundOpaque());
        }
        setGraphicsConfiguration(cfg);
        nativeWindowHandle = CreateWindow(getWidth(), getHeight(), chosenCaps.isBackgroundOpaque(), chosenCaps.getAlphaBits());
        if (nativeWindowHandle == 0) {
            throw new NativeWindowException("Error creating egl window: "+cfg);
        }
        setVisible0(nativeWindowHandle, false);
        setWindowHandle(nativeWindowHandle);
        if (0 == getWindowHandle()) {
            throw new NativeWindowException("Error native Window Handle is null");
        }
        windowHandleClose = nativeWindowHandle;
        addWindowListener(LinuxMouseTracker.getSingleton());
        addWindowListener(LinuxEventDeviceTracker.getSingleton());
        focusChanged(false, true);
    }

    protected void closeNativeImpl() {
        final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) getGraphicsConfiguration().getScreen().getDevice();

        removeWindowListener(LinuxMouseTracker.getSingleton());
        removeWindowListener(LinuxEventDeviceTracker.getSingleton());

        if(0!=windowHandleClose) {
            CloseWindow(windowHandleClose, windowUserData);
            windowUserData=0;
        }

        eglDevice.close();
    }

    protected void requestFocusImpl(boolean reparented) {
        focusChanged(false, true);
    }

    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, int flags) {
        if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) ) {
            setVisible0(nativeWindowHandle, 0 != ( FLAG_IS_VISIBLE & flags));
            visibleChanged(false, 0 != ( FLAG_IS_VISIBLE & flags));
        }

        if(0!=nativeWindowHandle) {
            if(0 != ( FLAG_CHANGE_FULLSCREEN & flags)) {
                final boolean fs = 0 != ( FLAG_IS_FULLSCREEN & flags) ;
                setFullScreen0(nativeWindowHandle, fs);
                if(fs) {
                    return true;
                }
            }
            // int _x=(x>=0)?x:this.x;
            // int _y=(x>=0)?y:this.y;
            width=(width>0)?width:getWidth();
            height=(height>0)?height:getHeight();
            if(width>0 || height>0) {
                setSize0(nativeWindowHandle, width, height);
            }
            if(x>=0 || y>=0) {
                System.err.println("setPosition n/a in KD");
            }
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

    //----------------------------------------------------------------------
    // Internals only
    //

    protected static native boolean initIDs();
    private        native long CreateWindow(int width, int height, boolean opaque, int alphaBits);
    private        native long RealizeWindow(long eglWindowHandle);
    private        native int  CloseWindow(long eglWindowHandle, long userData);
    private        native void setVisible0(long eglWindowHandle, boolean visible);
    private        native void setSize0(long eglWindowHandle, int width, int height);
    private        native void setFullScreen0(long eglWindowHandle, boolean fullscreen);

    private void windowCreated(long userData) {
        windowUserData=userData;
    }

    private long   nativeWindowHandle;
    private long   windowHandleClose;
    private long   windowUserData;
}

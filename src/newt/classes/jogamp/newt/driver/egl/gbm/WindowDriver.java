/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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
package jogamp.newt.driver.egl.gbm;

import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.egl.EGL;

import jogamp.newt.WindowImpl;
import jogamp.newt.driver.linux.LinuxEventDeviceTracker;
import jogamp.newt.driver.linux.LinuxMouseTracker;
import jogamp.opengl.egl.EGLGraphicsConfiguration;
import jogamp.opengl.egl.EGLGraphicsConfigurationFactory;
import jogamp.opengl.egl.EGLSurface;

public class WindowDriver extends WindowImpl {

    static {
        DisplayDriver.initSingleton();
    }

    public WindowDriver() {
        linuxMouseTracker = null; // LinuxMouseTracker.getSingleton();
        linuxEventDeviceTracker = null; // LinuxEventDeviceTracker.getSingleton();

        windowHandleClose = 0;
    }

    /**
     * Clamp given rectangle to given screen bounds.
     *
     * @param screen
     * @param rect the {@link RectangleImmutable} in pixel units
     * @param definePosSize if {@code true} issue {@link #definePosition(int, int)} and {@link #defineSize(int, int)}
     *                      if either has changed.
     * @return If position or size has been clamped a new {@link RectangleImmutable} instance w/ clamped values
     *         will be returned, otherwise the given {@code rect} is returned.
     */
    private RectangleImmutable clampRect(final ScreenDriver screen, final RectangleImmutable rect, final boolean definePosSize) {
        int x = rect.getX();
        int y = rect.getY();
        int w = rect.getWidth();
        int h = rect.getHeight();
        final int s_w = screen.getWidth();
        final int s_h = screen.getHeight();
        boolean modPos = false;
        boolean modSize = false;
        if( 0 > x ) {
            x = 0;
            modPos = true;
        }
        if( 0 > y ) {
            y = 0;
            modPos = true;
        }
        if( s_w < x + w ) {
            if( 0 < x ) {
                x = 0;
                modPos = true;
            }
            if( s_w < w ) {
                w = s_w;
                modSize = true;
            }
        }
        if( s_h < y + h ) {
            if( 0 < y ) {
                y = 0;
                modPos = true;
            }
            if( s_h < h ) {
                h = s_h;
                modSize = true;
            }
        }
        if( modPos || modSize ) {
            if( definePosSize ) {
                if( modPos ) {
                    definePosition(x, y);
                }
                if( modSize ) {
                    defineSize(w, h);
                }
            }
            return new Rectangle(x, y, w, h);
        } else {
            return rect;
        }
    }

    @Override
    protected boolean canCreateNativeImpl() {
        // clamp if required incl. redefinition of position and size
        clampRect((ScreenDriver) getScreen(), new Rectangle(getX(), getY(), getWidth(), getHeight()), true);
        return true; // default: always able to be created
    }

    static int fourcc_code(final char a, final char b, final char c, final char d) {
        // return ( (int)(a) | ((int)(b) << 8) | ((int)(c) << 16) | ((int)(d) << 24) );
        return ( (a) | ((b) << 8) | ((c) << 16) | ((d) << 24) );
    }
    /** [31:0] x:R:G:B 8:8:8:8 little endian */
    static final int GBM_FORMAT_XRGB8888 = fourcc_code('X', 'R', '2', '4');
    /** [31:0] A:R:G:B 8:8:8:8 little endian */
    static final int GBM_FORMAT_ARGB8888 = fourcc_code('A', 'R', '2', '4');

    @Override
    protected void createNativeImpl() {
        if (0 != getParentWindowHandle()) {
            throw new RuntimeException("Window parenting not supported (yet)");
        }

        final ScreenDriver screen = (ScreenDriver) getScreen();
        final DisplayDriver display = (DisplayDriver) screen.getDisplay();

        // Create own screen/device resource instance allowing independent ownership,
        // while still utilizing shared EGL resources.
        final AbstractGraphicsScreen aScreen = screen.getGraphicsScreen();
        final int nativeVisualID = capsRequested.isBackgroundOpaque() ? GBM_FORMAT_XRGB8888 : GBM_FORMAT_ARGB8888;

        final EGLGraphicsConfiguration eglConfig = EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(
                (GLCapabilitiesImmutable)capsRequested, (GLCapabilitiesImmutable)capsRequested, (GLCapabilitiesChooser)capabilitiesChooser,
                aScreen, nativeVisualID, !capsRequested.isBackgroundOpaque());
        if (eglConfig == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        setGraphicsConfiguration(eglConfig);
        final long nativeWindowHandle = CreateWindow0(DisplayDriver.getDrmHandle(), display.getGBMHandle(),
                                           getX(), getY(), getWidth(), getHeight(), nativeVisualID);
        if (nativeWindowHandle == 0) {
            throw new NativeWindowException("Error creating egl window: "+eglConfig);
        }
        setGraphicsConfiguration(eglConfig);
        setWindowHandle(nativeWindowHandle);
        if (0 == getWindowHandle()) {
            throw new NativeWindowException("Error native Window Handle is null");
        }
        windowHandleClose = nativeWindowHandle;

        eglSurface = EGLSurface.eglCreateWindowSurface(display.getHandle(), eglConfig.getNativeConfig(), nativeWindowHandle);
        if (EGL.EGL_NO_SURFACE==eglSurface) {
            throw new NativeWindowException("Creation of eglSurface failed: "+eglConfig+", windowHandle 0x"+Long.toHexString(nativeWindowHandle)+", error "+toHexString(EGL.eglGetError()));
        }

        if(false) {
            /**
            if(!EGL.eglSwapBuffers(display.getHandle(), eglSurface)) {
                throw new GLException("Error swapping buffers, eglError "+toHexString(EGL.eglGetError())+", "+this);
            } */
            lastBO = FirstSwapSurface0(DisplayDriver.getDrmHandle(), nativeWindowHandle, display.getHandle(), eglSurface);
        } else {
            lastBO = 0;
        }

        if( null != linuxEventDeviceTracker ) {
            addWindowListener(linuxEventDeviceTracker);
        }
        if( null != linuxMouseTracker ) {
            addWindowListener(linuxMouseTracker);
        }
        visibleChanged(true);
        focusChanged(false, true);
    }

    @Override
    protected void closeNativeImpl() {
        final DisplayDriver display = (DisplayDriver) getScreen().getDisplay();
        final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) getGraphicsConfiguration().getScreen().getDevice();

        if( null != linuxMouseTracker ) {
            removeWindowListener(linuxMouseTracker);
        }
        if( null != linuxEventDeviceTracker ) {
            removeWindowListener(linuxEventDeviceTracker);
        }

        lastBO = 0;
        if(0 != eglSurface) {
            try {
                if (!EGL.eglDestroySurface(eglDevice.getHandle(), eglSurface)) {
                    throw new GLException("Error destroying window surface (eglDestroySurface)");
                }
            } catch (final Throwable t) {
                t.printStackTrace();
            } finally {
                eglSurface = 0;
            }
        }
        if( 0 != windowHandleClose ) {
            CloseWindow0(display.getGBMHandle(), windowHandleClose);
            windowHandleClose = 0;
        }

        eglDevice.close();
    }

    @Override
    public final long getSurfaceHandle() {
        return eglSurface;
    }

    @Override
    public boolean surfaceSwap() {
        final DisplayDriver display = (DisplayDriver) getScreen().getDisplay();
        final long nativeWindowHandle = getWindowHandle();

        if( 0 == lastBO ) {
            /** if(!EGL.eglSwapBuffers(display.getHandle(), eglSurface)) {
                throw new GLException("Error swapping buffers, eglError "+toHexString(EGL.eglGetError())+", "+this);
            } */
            lastBO = FirstSwapSurface0(DisplayDriver.getDrmHandle(), nativeWindowHandle, display.getHandle(), eglSurface);
        }

        /**if(!EGL.eglSwapBuffers(display.getHandle(), eglSurface)) {
            throw new GLException("Error swapping buffers, eglError "+toHexString(EGL.eglGetError())+", "+this);
        } */
        lastBO = NextSwapSurface0(DisplayDriver.getDrmHandle(), nativeWindowHandle, lastBO, display.getHandle(), eglSurface);
        System.exit(1);
        return true; // eglSwapBuffers done!
    }

    @Override
    protected void requestFocusImpl(final boolean reparented) {
        focusChanged(false, true);
    }

    @Override
    protected final int getSupportedReconfigMaskImpl() {
        return minimumReconfigStateMask
               // | STATE_MASK_UNDECORATED
               // | STATE_MASK_ALWAYSONTOP
               // | STATE_MASK_ALWAYSONBOTTOM
               // | STATE_MASK_STICKY
               // | STATE_MASK_RESIZABLE
               // | STATE_MASK_MAXIMIZED_VERT
               // | STATE_MASK_MAXIMIZED_HORZ
               // | STATE_MASK_POINTERVISIBLE
               // | STATE_MASK_POINTERCONFINED
               ;
    }

    @Override
    protected boolean reconfigureWindowImpl(final int x, final int y, final int width, final int height, final int flags) {
        final RectangleImmutable rect = clampRect((ScreenDriver) getScreen(), new Rectangle(x, y, width, height), false);
        // reconfigure0 will issue position/size changed events if required
        reconfigure0(getWindowHandle(), rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), flags);

        return true;
    }

    @Override
    protected Point getLocationOnScreenImpl(final int x, final int y) {
        return new Point(x,y);
    }

    //----------------------------------------------------------------------
    // Internals only
    //
    private final LinuxMouseTracker linuxMouseTracker;
    private final LinuxEventDeviceTracker linuxEventDeviceTracker;
    private long windowHandleClose;
    private long eglSurface;
    private long lastBO;

    protected static native boolean initIDs();
    private native long CreateWindow0(long drmHandle, long gbmHandle, int x, int y, int width, int height, int nativeVisualID);
    private native void CloseWindow0(long gbmDisplay, long eglWindowHandle);
    private native void reconfigure0(long eglWindowHandle, int x, int y, int width, int height, int flags);
    private native long FirstSwapSurface0(long drmHandle, long gbmSurface, long eglDisplay, long eglSurface);
    private native long NextSwapSurface0(long drmHandle, long gbmSurface, long lastBO, long eglDisplay, long eglSurface);
}

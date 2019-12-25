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

import java.net.URLConnection;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.IOUtil;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.newt.Display;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.PNGPixelRect;

import jogamp.nativewindow.drm.DRMLib;
import jogamp.nativewindow.drm.DRMUtil;
import jogamp.newt.DisplayImpl;
import jogamp.newt.NEWTJNILibLoader;
import jogamp.newt.PointerIconImpl;
import jogamp.opengl.egl.EGLDisplayUtil;

public class DisplayDriver extends DisplayImpl {
    static {
        NEWTJNILibLoader.loadNEWTDrmGbm();
        GLProfile.initSingleton();

        if (!DisplayDriver.initIDs()) {
            throw new NativeWindowException("Failed to initialize egl.gbm Display jmethodIDs");
        }
        if (!ScreenDriver.initIDs()) {
            throw new NativeWindowException("Failed to initialize egl.gbm Screen jmethodIDs");
        }
        if (!WindowDriver.initIDs()) {
            throw new NativeWindowException("Failed to initialize egl.gbm Window jmethodIDs");
        }
        NativeWindowFactory.addCustomShutdownHook(false /* head */, new Runnable() {
           @Override
           public void run() {
                Shutdown0();
           } });

        PNGPixelRect image = null;
        if( DisplayImpl.isPNGUtilAvailable() ) {
            final IOUtil.ClassResources res = new IOUtil.ClassResources(new String[] { "newt/data/pointer-grey-alpha-16x24.png" }, DisplayDriver.class.getClassLoader(), null);
            try {
                final URLConnection urlConn = res.resolve(0);
                if( null != urlConn ) {
                    image = PNGPixelRect.read(urlConn.getInputStream(), PixelFormat.BGRA8888, false /* directBuffer */, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        defaultPointerIconImage = image;
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }

    public DisplayDriver() {
        gbmHandle = 0;
    }

    @Override
    protected void createNativeImpl() {
        final int drmFd = DRMUtil.getDrmFd();
        if( 0 > drmFd ) {
            throw new NativeWindowException("Failed to initialize DRM");
        }
        gbmHandle = DRMLib.gbm_create_device(drmFd);
        aDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(gbmHandle, AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);
        aDevice.open();

        if( null != defaultPointerIconImage ) {
            defaultPointerIcon = (PointerIconImpl) createPointerIcon(defaultPointerIconImage, 0, 0);
        } else {
            defaultPointerIcon = null;
        }

        if( DEBUG ) {
            System.err.println("Display.createNativeImpl: "+this);
        }
        if( DEBUG_POINTER_ICON ) {
            System.err.println("Display.createNativeImpl: defaultPointerIcon "+defaultPointerIcon);
        }
    }

    @Override
    protected void closeNativeImpl(final AbstractGraphicsDevice aDevice) {
        if( DEBUG ) {
            System.err.println("Display.closeNativeImpl: "+this);
        }
        if( null != defaultPointerIcon ) {
            defaultPointerIcon.destroy();
            defaultPointerIcon = null;
        }
        aDevice.close();
        DRMLib.gbm_device_destroy(gbmHandle);
        gbmHandle = 0;
    }

    /* pp */ final long getGBMHandle() { return gbmHandle; }

    @Override
    protected void dispatchMessagesNative() {
        DispatchMessages0();
    }

    @Override
    public final PixelFormat getNativePointerIconPixelFormat() { return PixelFormat.BGRA8888; }

    @Override
    protected final long createPointerIconImpl(final PixelFormat pixelformat, final int width, final int height, final ByteBuffer pixels, final int hotX, final int hotY) {
        this.aDevice.lock();
        try {
            return CreatePointerIcon(gbmHandle, pixels, width, height, hotX, hotY);
        } finally {
            this.aDevice.unlock();
        }
    }

    @Override
    protected final void destroyPointerIconImpl(final long displayHandle, final long piHandle) {
        final AbstractGraphicsDevice d = this.aDevice;
        if( null != d ) {
            d.lock();
            try {
                DestroyPointerIcon0(piHandle);
            } finally {
                d.unlock();
            }
        } else {
            DestroyPointerIcon0(piHandle);
        }
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    /* pp */ boolean setPointerIcon(final int crtc_id, final long piHandle, final boolean enable, final int hotX, final int hotY, final int x, final int y) {
        this.aDevice.lock();
        try {
            return SetPointerIcon0(DRMUtil.getDrmFd(), crtc_id, piHandle, enable, hotX, hotY, x, y);
        } finally {
            this.aDevice.unlock();
        }
    }
    /* pp */ boolean movePointerIcon(final int crtc_id, final int x, final int y) {
        this.aDevice.lock();
        try {
            return MovePointerIcon0(DRMUtil.getDrmFd(), crtc_id, x, y);
        } finally {
            this.aDevice.unlock();
        }
    }

    private static native boolean initIDs();
    private static native void Shutdown0();

    private static native void DispatchMessages0();

    private static long CreatePointerIcon(final long gbmDevice, final Buffer pixels, final int width, final int height, final int hotX, final int hotY) {
        if( 0 >= width || width > 64 || 0 >= height || height > 64 ) {
            throw new IllegalArgumentException("implementation only supports BGRA icons of size [1x1] -> [64x64]");
        }
        final boolean pixels_is_direct = Buffers.isDirect(pixels);
        return CreatePointerIcon0(gbmDevice,
                                  pixels_is_direct ? pixels : Buffers.getArray(pixels),
                                  pixels_is_direct ? Buffers.getDirectBufferByteOffset(pixels) : Buffers.getIndirectBufferByteOffset(pixels),
                                  pixels_is_direct,
                                  width, height,
                                  hotX, hotY);
    }
    private static native long CreatePointerIcon0(long gbmDevice, Object pixels, int pixels_byte_offset, boolean pixels_is_direct,
                                                  int width, int height, int hotX, int hotY);
    private static native void DestroyPointerIcon0(long piHandle);

    private static native boolean SetPointerIcon0(int drmFd, int crtc_id, long piHandle, boolean enable, int hotX, int hotY, int x, int y);
    private static native boolean MovePointerIcon0(int drmFd, int crtc_id, int x, int y);

    /* pp */ static final boolean DEBUG_POINTER_ICON = Display.DEBUG_POINTER_ICON;
    /* pp */ PointerIconImpl defaultPointerIcon = null;

    private long gbmHandle;
    private static final PNGPixelRect defaultPointerIconImage;
}

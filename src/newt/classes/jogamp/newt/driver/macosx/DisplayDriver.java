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

package jogamp.newt.driver.macosx;

import java.net.URLConnection;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.util.PixelFormat;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.IOUtil;
import com.jogamp.nativewindow.macosx.MacOSXGraphicsDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.opengl.util.PNGPixelRect;

import jogamp.newt.DisplayImpl;
import jogamp.newt.NEWTJNILibLoader;

public class DisplayDriver extends DisplayImpl {
    private static final PNGPixelRect defaultIconData;

    static {
        NEWTJNILibLoader.loadNEWT();

        if(!initNSApplication0()) {
            throw new NativeWindowException("Failed to initialize native Application hook");
        }
        if(!WindowDriver.initIDs0()) {
            throw new NativeWindowException("Failed to initialize jmethodIDs");
        }
        {
            PNGPixelRect image=null;
            if( DisplayImpl.isPNGUtilAvailable() ) {
                try {
                    // NOTE: MUST BE DIRECT BUFFER, since NSBitmapImageRep uses buffer directly!
                    final IOUtil.ClassResources iconRes = NewtFactory.getWindowIcons();
                    final URLConnection urlConn = iconRes.resolve(iconRes.resourceCount()-1);
                    if( null != urlConn ) {
                        image = PNGPixelRect.read(urlConn.getInputStream(), PixelFormat.RGBA8888, true /* directBuffer */, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
            defaultIconData = image;
            if( null != defaultIconData ) {
                final Buffer pixels = defaultIconData.getPixels();
                DisplayDriver.setAppIcon0(
                      pixels, Buffers.getDirectBufferByteOffset(pixels), true /* pixels_is_direct */,
                      defaultIconData.getSize().getWidth(), defaultIconData.getSize().getHeight());
            }
        }

        if(DEBUG) {
            System.err.println("MacDisplay.init App and IDs OK "+Thread.currentThread().getName());
        }
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }

    public DisplayDriver() {
    }

    @Override
    public PixelFormat getNativePointerIconPixelFormat() { return PixelFormat.RGBA8888; }

    @Override
    protected void dispatchMessagesNative() {
        // nop
    }

    @Override
    protected void createNativeImpl() {
        aDevice = new MacOSXGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
    }

    @Override
    protected void closeNativeImpl(final AbstractGraphicsDevice aDevice) {
        aDevice.close();
    }

    /**
     * {@inheritDoc}
     * <p>
     * NOTE: MUST BE DIRECT BUFFER, since NSBitmapImageRep uses buffer directly!
     * </p>
     */
    @Override
    public final boolean getNativePointerIconForceDirectNIO() { return true; }

    @Override
    protected final long createPointerIconImpl(final PixelFormat pixelformat, final int width, final int height, final ByteBuffer pixels, final int hotX, final int hotY) {
        return createPointerIcon0(
              pixels, Buffers.getDirectBufferByteOffset(pixels), true /* pixels_is_direct */,
              width, height, hotX, hotY);
    }

    @Override
    protected final void destroyPointerIconImpl(final long displayHandle, final long piHandle) {
        destroyPointerIcon0(piHandle);
    }

    public static void runNSApplication() {
        runNSApplication0();
    }
    public static void stopNSApplication() {
        stopNSApplication0();
    }

    private static native boolean initNSApplication0();
    private static native void runNSApplication0();
    private static native void stopNSApplication0();
    /* pp */ static native void setAppIcon0(Object pixels, int pixels_byte_offset, boolean pixels_is_direct, int width, int height);
    private static native long createPointerIcon0(Object pixels, int pixels_byte_offset, boolean pixels_is_direct, int width, int height, int hotX, int hotY);
    private static native long destroyPointerIcon0(long handle);

}


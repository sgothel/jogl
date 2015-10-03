/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package jogamp.newt.driver.windows;

import java.net.URLConnection;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import jogamp.nativewindow.windows.RegisteredClass;
import jogamp.nativewindow.windows.RegisteredClassFactory;
import jogamp.newt.DisplayImpl;
import jogamp.newt.NEWTJNILibLoader;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.util.PixelFormat;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.IOUtil;
import com.jogamp.nativewindow.windows.WindowsGraphicsDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.opengl.util.PNGPixelRect;

public class DisplayDriver extends DisplayImpl {

    private static final String newtClassBaseName = "_newt_clazz" ;
    private static final long[] defaultIconHandles;
    private static RegisteredClassFactory sharedClassFactory;

    static {
        NEWTJNILibLoader.loadNEWT();
        {
            final long[] _defaultIconHandle = { 0, 0 };
            if( DisplayImpl.isPNGUtilAvailable() ) {
                try {
                    final IOUtil.ClassResources iconRes = NewtFactory.getWindowIcons();
                    {
                        final URLConnection urlConn = iconRes.resolve(0);
                        if( null != urlConn ) {
                            final PNGPixelRect image = PNGPixelRect.read(urlConn.getInputStream(), PixelFormat.BGRA8888, false /* directBuffer */, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
                            _defaultIconHandle[0] = DisplayDriver.createBGRA8888Icon0(image.getPixels(), image.getSize().getWidth(), image.getSize().getHeight(), false, 0, 0);
                        }
                    }
                    {
                        final URLConnection urlConn = iconRes.resolve(iconRes.resourceCount()-1);
                        if( null != urlConn ) {
                            final PNGPixelRect image = PNGPixelRect.read(urlConn.getInputStream(), PixelFormat.BGRA8888, false /* directBuffer */, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
                            _defaultIconHandle[1] = DisplayDriver.createBGRA8888Icon0(image.getPixels(), image.getSize().getWidth(), image.getSize().getHeight(), false, 0, 0);
                        }
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
            defaultIconHandles = _defaultIconHandle; // null is a valid value for an icon handle
        }
        sharedClassFactory = new RegisteredClassFactory(newtClassBaseName, WindowDriver.getNewtWndProc0(),
                                                        false /* useDummyDispatchThread */, defaultIconHandles[0], defaultIconHandles[1]);

        if (!WindowDriver.initIDs0(RegisteredClassFactory.getHInstance())) {
            throw new NativeWindowException("Failed to initialize WindowsWindow jmethodIDs");
        }
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }

    protected static long getHInstance() {
        return RegisteredClassFactory.getHInstance();
    }

    private RegisteredClass sharedClass;

    public DisplayDriver() {
    }

    @Override
    protected void createNativeImpl() {
        sharedClass = sharedClassFactory.getSharedClass();
        aDevice = new WindowsGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
    }

    @Override
    protected void closeNativeImpl(final AbstractGraphicsDevice aDevice) {
        sharedClassFactory.releaseSharedClass();
        aDevice.close();
    }

    @Override
    protected void dispatchMessagesNative() {
        DispatchMessages0();
    }

    protected String getWindowClassName() {
        return sharedClass.getName();
    }

    @Override
    protected final long createPointerIconImpl(final PixelFormat pixelformat, final int width, final int height, final ByteBuffer pixels, final int hotX, final int hotY) {
        return createBGRA8888Icon0(pixels, width, height, true, hotX, hotY);
    }

    @Override
    protected final void destroyPointerIconImpl(final long displayHandle, final long piHandle) {
        destroyIcon0(piHandle);
    }

    //----------------------------------------------------------------------
    // Internals only
    //
    private static native void DispatchMessages0();

    static long createBGRA8888Icon0(final Buffer pixels, final int width, final int height, final boolean isCursor, final int hotX, final int hotY) {
        if( null == pixels ) {
            throw new IllegalArgumentException("data buffer/size");
        }
        final boolean pixels_is_direct = Buffers.isDirect(pixels);
        return createBGRA8888Icon0(
                      pixels_is_direct ? pixels : Buffers.getArray(pixels),
                      pixels_is_direct ? Buffers.getDirectBufferByteOffset(pixels) : Buffers.getIndirectBufferByteOffset(pixels),
                      pixels_is_direct,
                      width, height, isCursor, hotX, hotY);
    }
    private static native long createBGRA8888Icon0(Object pixels, int pixels_byte_offset, boolean pixels_is_direct, int width, int height, boolean isCursor, int hotX, int hotY);
    private static native void destroyIcon0(long handle);
}


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

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import jogamp.nativewindow.windows.RegisteredClass;
import jogamp.nativewindow.windows.RegisteredClassFactory;
import jogamp.newt.DisplayImpl;
import jogamp.newt.NEWTJNILibLoader;
import jogamp.newt.driver.PNGIcon;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindowException;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.IOUtil;
import com.jogamp.nativewindow.windows.WindowsGraphicsDevice;

public class DisplayDriver extends DisplayImpl {

    private static final String newtClassBaseName = "_newt_clazz" ;
    private static RegisteredClassFactory sharedClassFactory;

    static {
        NEWTJNILibLoader.loadNEWT();

        sharedClassFactory = new RegisteredClassFactory(newtClassBaseName, WindowDriver.getNewtWndProc0(), false /* useDummyDispatchThread */);

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
    protected void closeNativeImpl(AbstractGraphicsDevice aDevice) {
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
    protected PointerIcon createPointerIconImpl(final IOUtil.ClassResources pngResource, final int hotX, final int hotY) throws MalformedURLException, InterruptedException, IOException {
        if( PNGIcon.isAvailable() ) {
            final int[] width = { 0 }, height = { 0 }, data_size = { 0 }, elem_bytesize = { 0 };
            if( null != pngResource && 0 < pngResource.resourceCount() ) {
                final ByteBuffer data = PNGIcon.singleToRGBAImage(pngResource, 0, true /* toBGRA */, width, height, data_size, elem_bytesize);
                return new PointerIconImpl( createBGRA8888Icon0(data, width[0], height[0], true, hotX, hotY) );
            }
        }
        return null;
    }

    @Override
    protected final void destroyPointerIconImpl(final long displayHandle, final PointerIcon pi) {
        destroyIcon0(((PointerIconImpl)pi).handle);
    }

    //----------------------------------------------------------------------
    // Internals only
    //
    private static native void DispatchMessages0();

    static long createBGRA8888Icon0(Buffer data, int width, int height, boolean isCursor, int hotX, int hotY) {
        if( null == data ) {
            throw new IllegalArgumentException("data buffer/size");
        }
        if( !Buffers.isDirect(data) ) {
            throw new IllegalArgumentException("data buffer is not direct "+data);
        }
        return createBGRA8888Icon0(data, Buffers.getDirectBufferByteOffset(data), width, height, isCursor, hotX, hotY);
    }
    private static native long createBGRA8888Icon0(Object data, int data_offset, int width, int height, boolean isCursor, int hotX, int hotY);
    private static native void destroyIcon0(long handle);
}


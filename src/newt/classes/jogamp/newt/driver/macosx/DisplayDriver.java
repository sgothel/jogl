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

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.Point;

import com.jogamp.common.util.IOUtil;
import com.jogamp.nativewindow.macosx.MacOSXGraphicsDevice;
import com.jogamp.newt.NewtFactory;

import jogamp.newt.DisplayImpl;
import jogamp.newt.NEWTJNILibLoader;
import jogamp.newt.driver.PNGIcon;

public class DisplayDriver extends DisplayImpl {
    private static final int defaultIconWidth, defaultIconHeight;
    private static final Buffer defaultIconData;

    static {
        NEWTJNILibLoader.loadNEWT();

        if(!initNSApplication0()) {
            throw new NativeWindowException("Failed to initialize native Application hook");
        }
        if(!WindowDriver.initIDs0()) {
            throw new NativeWindowException("Failed to initialize jmethodIDs");
        }
        {
            final int[] width = { 0 }, height = { 0 }, data_size = { 0 };
            Buffer data=null;
            if( PNGIcon.isAvailable() ) {
                try {
                    final IOUtil.ClassResources iconRes = NewtFactory.getWindowIcons();
                    data = PNGIcon.singleToRGBAImage(iconRes, iconRes.resourceCount()-1, false /* toBGRA */, width, height, data_size);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            defaultIconWidth = width[0];
            defaultIconHeight = height[0];
            defaultIconData = data;
            if( null != defaultIconData ) {
                DisplayDriver.setAppIcon0(defaultIconData, defaultIconWidth, defaultIconHeight);
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
    protected void dispatchMessagesNative() {
        // nop
    }

    @Override
    protected void createNativeImpl() {
        aDevice = new MacOSXGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
    }

    @Override
    protected void closeNativeImpl(AbstractGraphicsDevice aDevice) {
        aDevice.close();
    }

    @Override
    protected PointerIcon createPointerIconImpl(final IOUtil.ClassResources pngResource, final int hotX, final int hotY) throws MalformedURLException, InterruptedException, IOException {
        if( PNGIcon.isAvailable() ) {
            final int[] width = { 0 }, height = { 0 }, data_size = { 0 };
            if( null != pngResource && 0 < pngResource.resourceCount() ) {
                return new PointerIconImpl( createPointerIcon0(data, width[0], height[0], hotX, hotY),
                                            new Dimension(width[0], height[0]), new Point(hotX, hotY));
                final ByteBuffer data = PNGIcon.singleToRGBAImage(pngResource, 0, true /* toBGRA */, width, height, data_size);
            }
        }
        return null;
    }

    @Override
    protected final void destroyPointerIconImpl(final long displayHandle, final PointerIcon pi) {
        destroyPointerIcon0(((PointerIconImpl)pi).handle);
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
    /* pp */ static native void setAppIcon0(Object iconData, int iconWidth, int iconHeight);
    private static native long createPointerIcon0(Object iconData, int iconWidth, int iconHeight, int hotX, int hotY);
    private static native long destroyPointerIcon0(long handle);

}


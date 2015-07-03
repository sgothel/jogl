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

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowException;

import jogamp.newt.NEWTJNILibLoader;

import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.egl.EGL;

public class DisplayDriver extends jogamp.newt.DisplayImpl {

    static {
        NEWTJNILibLoader.loadNEWT();

        if (!WindowDriver.initIDs()) {
            throw new NativeWindowException("Failed to initialize BCEGL Window jmethodIDs");
        }
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }


    public DisplayDriver() {
    }

    @Override
    protected void createNativeImpl() {
        final long handle = CreateDisplay(ScreenDriver.fixedWidth, ScreenDriver.fixedHeight);
        if (handle == EGL.EGL_NO_DISPLAY) {
            throw new NativeWindowException("BC EGL CreateDisplay failed");
        }
        aDevice = new EGLGraphicsDevice(EGL.EGL_DEFAULT_DISPLAY, handle, AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT, null);
    }

    @Override
    protected void closeNativeImpl(final AbstractGraphicsDevice aDevice) {
        if (aDevice.getHandle() != EGL.EGL_NO_DISPLAY) {
            DestroyDisplay(aDevice.getHandle());
        }
        aDevice.close();
    }

    @Override
    protected void dispatchMessagesNative() {
        // n/a .. DispatchMessages();
    }

    private native long CreateDisplay(int width, int height);
    private native void DestroyDisplay(long dpy);
    private native void DispatchMessages();
}


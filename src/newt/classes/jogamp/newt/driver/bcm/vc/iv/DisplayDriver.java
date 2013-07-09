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

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindowException;

import jogamp.newt.DisplayImpl;
import jogamp.newt.NEWTJNILibLoader;
import jogamp.opengl.egl.EGL;
import jogamp.opengl.egl.EGLDisplayUtil;

public class DisplayDriver extends DisplayImpl {
    static {
        NEWTJNILibLoader.loadNEWT();

        if (!DisplayDriver.initIDs()) {
            throw new NativeWindowException("Failed to initialize bcm.vc.iv Display jmethodIDs");
        }
        if (!ScreenDriver.initIDs()) {
            throw new NativeWindowException("Failed to initialize bcm.vc.iv Screen jmethodIDs");
        }
        if (!WindowDriver.initIDs()) {
            throw new NativeWindowException("Failed to initialize bcm.vc.iv Window jmethodIDs");
        }
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }


    public DisplayDriver() {
    }

    protected void createNativeImpl() {
        // FIXME: map name to EGL_*_DISPLAY
        aDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(EGL.EGL_DEFAULT_DISPLAY, AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);
    }

    protected void closeNativeImpl(AbstractGraphicsDevice aDevice) {
        aDevice.close();
    }

    protected void dispatchMessagesNative() {
        DispatchMessages();
    }

    protected static native boolean initIDs();    
    private native void DispatchMessages();
}


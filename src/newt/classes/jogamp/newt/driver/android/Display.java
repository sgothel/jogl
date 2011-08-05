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

import jogamp.newt.*;
import jogamp.opengl.egl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.egl.*;
import android.content.Context;

public class Display extends jogamp.newt.DisplayImpl {

    /*package*/ Context appContext;
    
    static {
        NEWTJNILibLoader.loadNEWT();

        if (!Window.initIDs()) {
            throw new NativeWindowException("Failed to initialize BCEGL Window jmethodIDs");
        }
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }


    public Display() {
    }

    protected void createNativeImpl() {
        long handle = CreateDisplay(Screen.fixedWidth, Screen.fixedHeight);
        if (handle == EGL.EGL_NO_DISPLAY) {
            throw new NativeWindowException("BC EGL CreateDisplay failed");
        }
        aDevice = new EGLGraphicsDevice(handle, AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);
    }

    protected void closeNativeImpl() {
        if (aDevice.getHandle() != EGL.EGL_NO_DISPLAY) {
            DestroyDisplay(aDevice.getHandle());
        }
    }

    protected void dispatchMessagesNative() {
        // n/a .. DispatchMessages();
    }

    private native long CreateDisplay(int width, int height);
    private native void DestroyDisplay(long dpy);
    private native void DispatchMessages();
}


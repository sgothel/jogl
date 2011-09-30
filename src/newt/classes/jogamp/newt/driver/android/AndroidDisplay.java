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
import javax.media.opengl.GLException;

public class AndroidDisplay extends jogamp.newt.DisplayImpl {
    static {
        NEWTJNILibLoader.loadNEWT();

        if (!AndroidWindow.initIDs0()) {
            throw new NativeWindowException("Failed to initialize Android NEWT Windowing library");
        }
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }


    public AndroidDisplay() {
    }

    protected void createNativeImpl() {
        // EGL Device
        final long eglDisplay = EGL.eglGetDisplay(EGL.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL.EGL_NO_DISPLAY) {
            throw new GLException("Failed to created EGL default display: error 0x"+Integer.toHexString(EGL.eglGetError()));
        } else if(DEBUG) {
            System.err.println("Android Display.createNativeImpl: eglDisplay(EGL_DEFAULT_DISPLAY): 0x"+Long.toHexString(eglDisplay));
        }
        if (!EGL.eglInitialize(eglDisplay, null, null)) {
            throw new GLException("eglInitialize failed eglDisplay 0x"+Long.toHexString(eglDisplay)+", error 0x"+Integer.toHexString(EGL.eglGetError()));
        }
        aDevice = new EGLGraphicsDevice(eglDisplay, AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);        
    }

    protected void closeNativeImpl() {
        if (aDevice.getHandle() != EGL.EGL_NO_DISPLAY) {
            EGL.eglTerminate(aDevice.getHandle());
        }
    }

    protected void dispatchMessagesNative() {
        // n/a .. DispatchMessages();
    }    
}


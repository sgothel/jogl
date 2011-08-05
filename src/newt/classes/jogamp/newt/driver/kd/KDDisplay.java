/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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

package jogamp.newt.driver.kd;

import com.jogamp.newt.*;
import jogamp.newt.*;
import jogamp.opengl.egl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.egl.*;

public class KDDisplay extends DisplayImpl {

    static {
        NEWTJNILibLoader.loadNEWT();

        if (!KDWindow.initIDs()) {
            throw new NativeWindowException("Failed to initialize KDWindow jmethodIDs");
        }
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }


    public KDDisplay() {
    }

    protected void createNativeImpl() {
        // FIXME: map name to EGL_*_DISPLAY
        long handle = EGL.eglGetDisplay(EGL.EGL_DEFAULT_DISPLAY);
        if (handle == EGL.EGL_NO_DISPLAY) {
            throw new NativeWindowException("eglGetDisplay failed");
        }
        if (!EGL.eglInitialize(handle, null, null)) {
            throw new NativeWindowException("eglInitialize failed");
        }
        aDevice = new EGLGraphicsDevice(handle, AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);
    }

    protected void closeNativeImpl() {
        if (aDevice.getHandle() != EGL.EGL_NO_DISPLAY) {
            EGL.eglTerminate(aDevice.getHandle());
        }
    }

    protected void dispatchMessagesNative() {
        DispatchMessages();
    }

    private native void DispatchMessages();
}


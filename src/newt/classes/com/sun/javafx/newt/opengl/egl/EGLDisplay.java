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

package com.sun.javafx.newt.opengl.egl;

import com.sun.javafx.newt.*;
import com.sun.javafx.newt.impl.*;
import com.sun.opengl.impl.egl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.egl.*;

public class EGLDisplay extends Display {

    static {
        NativeLibLoader.loadNEWT();

        System.loadLibrary("EglUtil");
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }


    public EGLDisplay() {
    }

    protected void createNative() {
        try {
            int windowWidth = 1920, windowHeight = 1080;
            int width[] = { windowWidth };
            int height[] = { windowHeight };
            long eglDisplayHandle = 
                    EGL.EGLUtil_CreateDisplayByNative(windowWidth, windowHeight);
            long eglSurfaceHandle = 
                    EGL.EGLUtil_CreateWindowByNative(eglDisplayHandle, 1,
                                                      width, 0, height, 0);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    protected void closeNative() {
        if (aDevice.getHandle() != EGL.EGL_NO_DISPLAY) {
            EGL.eglTerminate(aDevice.getHandle());
        }
    }

    protected void dispatchMessages() {
        DispatchMessages();
    }

    private native void DispatchMessages();
}


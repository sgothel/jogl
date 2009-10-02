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

package com.sun.javafx.newt.opengl.broadcom.egl;

import com.sun.javafx.newt.impl.*;
import com.sun.opengl.impl.egl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.egl.*;

public class Display extends com.sun.javafx.newt.Display {

    static {
        NativeLibLoader.loadNEWT();

        if (!Window.initIDs()) {
            throw new NativeWindowException("Failed to initialize BCEGL Window jmethodIDs");
        }
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }


    public Display() {
    }

    protected void createNative() {
        long handle = CreateDisplay(Screen.fixedWidth, Screen.fixedHeight);
        if (handle == EGL.EGL_NO_DISPLAY) {
            throw new NativeWindowException("BC EGL CreateDisplay failed");
        }
        aDevice = new EGLGraphicsDevice(handle);
    }

    protected void closeNative() {
        if (aDevice.getHandle() != EGL.EGL_NO_DISPLAY) {
            DestroyDisplay(aDevice.getHandle());
        }
    }

    protected void dispatchMessages() {
        // n/a .. DispatchMessages();
    }

    private native long CreateDisplay(int width, int height);
    private native void DestroyDisplay(long dpy);
    private native void DispatchMessages();
}


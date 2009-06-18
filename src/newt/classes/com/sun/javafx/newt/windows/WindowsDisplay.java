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

package com.sun.javafx.newt.windows;

import javax.media.nativewindow.*;
import javax.media.nativewindow.windows.*;
import com.sun.javafx.newt.*;
import com.sun.javafx.newt.impl.*;

public class WindowsDisplay extends Display {

    protected static final String WINDOW_CLASS_NAME = "NewtWindowClass";
    private static int windowClassAtom;
    private static long hInstance;

    static {
        NativeLibLoader.loadNEWT();

        if (!WindowsWindow.initIDs()) {
            throw new NativeWindowException("Failed to initialize WindowsWindow jmethodIDs");
        }
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }


    public WindowsDisplay() {
    }

    protected void createNative() {
        aDevice = new WindowsGraphicsDevice();
    }

    protected void closeNative() { 
        // Can't do .. only at application shutdown 
        // UnregisterWindowClass(getWindowClassAtom(), getHInstance());
    }

    protected void dispatchMessages() {
        DispatchMessages();
    }

    protected static synchronized int getWindowClassAtom() {
        if(0 == windowClassAtom) {
            windowClassAtom = RegisterWindowClass(WINDOW_CLASS_NAME, getHInstance());
            if (0 == windowClassAtom) {
                throw new NativeWindowException("Error while registering window class");
            }
        }
        return windowClassAtom;
    }

    protected static synchronized long getHInstance() {
        if(0 == hInstance) {
            hInstance = LoadLibraryW("newt");
            if (0 == hInstance) {
                throw new NativeWindowException("Error finding HINSTANCE for \"newt\"");
            }
        }
        return hInstance;
    }

    //----------------------------------------------------------------------
    // Internals only
    //
    private static native long LoadLibraryW(String libraryName);
    private static native int  RegisterWindowClass(String windowClassName, long hInstance);
    private static native void UnregisterWindowClass(int wndClassAtom, long hInstance);

    private static native void DispatchMessages();
}


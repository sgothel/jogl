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

package com.sun.javafx.newt.x11;

import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;
import com.sun.javafx.newt.*;
import com.sun.javafx.newt.impl.*;
import com.sun.nativewindow.impl.x11.X11Util;

public class X11Display extends Display {
    static {
        NativeLibLoader.loadNEWT();

        if (!initIDs()) {
            throw new NativeWindowException("Failed to initialize X11Display jmethodIDs");
        }

        if (!X11Window.initIDs()) {
            throw new NativeWindowException("Failed to initialize X11Window jmethodIDs");
        }
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }


    public X11Display() {
    }

    protected void createNative() {
        long handle= X11Util.getThreadLocalDisplay(name);
        if (handle == 0 ) {
            throw new RuntimeException("Error creating display: "+name);
        }
        try {
            CompleteDisplay(handle);
        } catch(RuntimeException e) {
            X11Util.closeThreadLocalDisplay(name);
            throw e;
        }
        aDevice = new X11GraphicsDevice(handle);
    }

    protected void closeNative() {
        if(0==X11Util.closeThreadLocalDisplay(name)) {
            throw new NativeWindowException(this+" was not mapped");
        }
    }

    protected void dispatchMessages() {
        DispatchMessages(getHandle(), javaObjectAtom, windowDeleteAtom);
    }

    protected void lockDisplay() {
        super.lockDisplay();
        LockDisplay(getHandle());
    }

    protected void unlockDisplay() {
        UnlockDisplay(getHandle());
        super.unlockDisplay();
    }

    protected long getJavaObjectAtom() { return javaObjectAtom; }
    protected long getWindowDeleteAtom() { return windowDeleteAtom; }

    //----------------------------------------------------------------------
    // Internals only
    //
    private static native boolean initIDs();

    private native void LockDisplay(long handle);
    private native void UnlockDisplay(long handle);

    private native void CompleteDisplay(long handle);

    private native void DispatchMessages(long display, long javaObjectAtom, long windowDeleteAtom);

    private void displayCompleted(long javaObjectAtom, long windowDeleteAtom) {
        this.javaObjectAtom=javaObjectAtom;
        this.windowDeleteAtom=windowDeleteAtom;
    }

    private long windowDeleteAtom;
    private long javaObjectAtom;
}


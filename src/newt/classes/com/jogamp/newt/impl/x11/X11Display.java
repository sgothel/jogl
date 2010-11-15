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

package com.jogamp.newt.impl.x11;

import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;
import com.jogamp.newt.impl.*;
import com.jogamp.nativewindow.impl.x11.X11Util;

public class X11Display extends DisplayImpl {

    static {
        NEWTJNILibLoader.loadNEWT();

        if ( !initIDs0() ) {
            throw new NativeWindowException("Failed to initialize X11Display jmethodIDs");
        }

        if (!X11Window.initIDs0()) {
            throw new NativeWindowException("Failed to initialize X11Window jmethodIDs");
        }
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }


    public X11Display() {
    }

    public String validateDisplayName(String name, long handle) {
        return X11Util.validateDisplayName(name, handle);
    }

    protected void createNativeImpl() {
        long handle = X11Util.createDisplay(name);
        if( 0 == handle ) {
            throw new RuntimeException("Error creating display: "+name);
        }
        try {
            CompleteDisplay0(handle);
        } catch(RuntimeException e) {
            X11Util.closeDisplay(handle);
            throw e;
        }
        aDevice = new X11GraphicsDevice(handle, AbstractGraphicsDevice.DEFAULT_UNIT, NativeWindowFactory.getNullToolkitLock());
        // aDevice = new X11GraphicsDevice(handle, NativeWindowFactory.createDefaultToolkitLockNoAWT(NativeWindowFactory.TYPE_X11, handle));
        // aDevice = new X11GraphicsDevice(handle);

    }

    protected void closeNativeImpl() {
        X11Util.closeDisplay(getHandle());
    }

    protected void dispatchMessagesNative() {
        if(0==getHandle()) {
            throw new RuntimeException("display handle null");
        }
        DispatchMessages0(getHandle(), javaObjectAtom, windowDeleteAtom);
    }

    protected long getJavaObjectAtom() { return javaObjectAtom; }
    protected long getWindowDeleteAtom() { return windowDeleteAtom; }

    //----------------------------------------------------------------------
    // Internals only
    //
    private static native boolean initIDs0();

    private native void CompleteDisplay0(long handle);

    private native void DispatchMessages0(long display, long javaObjectAtom, long windowDeleteAtom);

    private void displayCompleted(long javaObjectAtom, long windowDeleteAtom) {
        this.javaObjectAtom=javaObjectAtom;
        this.windowDeleteAtom=windowDeleteAtom;
    }

    private long windowDeleteAtom;
    private long javaObjectAtom;
}


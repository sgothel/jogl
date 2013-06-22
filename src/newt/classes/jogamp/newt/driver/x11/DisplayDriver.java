/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package jogamp.newt.driver.x11;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindowException;

import com.jogamp.nativewindow.x11.X11GraphicsDevice;

import jogamp.nativewindow.x11.X11Util;
import jogamp.newt.DisplayImpl;
import jogamp.newt.NEWTJNILibLoader;

public class DisplayDriver extends DisplayImpl {

    static {
        NEWTJNILibLoader.loadNEWT();

        if ( !initIDs0(X11Util.XERROR_STACKDUMP) ) {
            throw new NativeWindowException("Failed to initialize X11Display jmethodIDs");
        }

        if (!WindowDriver.initIDs0()) {
            throw new NativeWindowException("Failed to initialize X11Window jmethodIDs");
        }
    }

    /** Ensure static init has been run. */
    /* pp */static void initSingleton() { }

    public DisplayDriver() {
    }

    public String validateDisplayName(String name, long handle) {
        return X11Util.validateDisplayName(name, handle);
    }

    /**
     * {@inheritDoc}
     *
     * We use a private non-shared X11 Display instance for EDT window operations and one for exposed animation, eg. OpenGL.
     */
    protected void createNativeImpl() {
        X11Util.setX11ErrorHandler(true, DEBUG ? false : true); // make sure X11 error handler is set
        long handle = X11Util.openDisplay(name);
        if( 0 == handle ) {
            throw new RuntimeException("Error creating display(Win): "+name);
        }
        aDevice = new X11GraphicsDevice(handle, AbstractGraphicsDevice.DEFAULT_UNIT, true /* owner */);
        try {
            CompleteDisplay0(aDevice.getHandle());
        } catch(RuntimeException e) {
            closeNativeImpl();
            throw e;                
        }
    }

    @Override
    protected void closeNativeImpl() {
        DisplayRelease0(aDevice.getHandle(), javaObjectAtom, windowDeleteAtom /*, kbdHandle */); // XKB disabled for now
        javaObjectAtom = 0;
        windowDeleteAtom = 0;
        // kbdHandle = 0;
        aDevice.close(); // closes X11 display
    }

    @Override
    protected void dispatchMessagesNative() {
        aDevice.lock();
        try {
            final long handle = aDevice.getHandle();
            if(0 != handle) {
                DispatchMessages0(handle, javaObjectAtom, windowDeleteAtom /*, kbdHandle */); // XKB disabled for now
            }
        } finally {
            if(null != aDevice) { // could be pulled by destroy event
                aDevice.unlock();
            }
        }
    }

    protected long getJavaObjectAtom() { return javaObjectAtom; }
    protected long getWindowDeleteAtom() { return windowDeleteAtom; }
    // protected long getKbdHandle() { return kbdHandle; } // XKB disabled for now
    
    /** Returns <code>null</code> if !{@link #isNativeValid()}, otherwise the Boolean value of {@link X11GraphicsDevice#isXineramaEnabled()}. */ 
    protected Boolean isXineramaEnabled() { return isNativeValid() ? Boolean.valueOf(((X11GraphicsDevice)aDevice).isXineramaEnabled()) : null; }
    
    //----------------------------------------------------------------------
    // Internals only
    //
    
    private static native boolean initIDs0(boolean debug);

    private native void CompleteDisplay0(long handle);

    private void displayCompleted(long javaObjectAtom, long windowDeleteAtom /*, long kbdHandle */) {
        this.javaObjectAtom=javaObjectAtom;
        this.windowDeleteAtom=windowDeleteAtom;
        // this.kbdHandle = kbdHandle; // XKB disabled for now
    }
    private native void DisplayRelease0(long handle, long javaObjectAtom, long windowDeleteAtom /*, long kbdHandle */); // XKB disabled for now

    private native void DispatchMessages0(long display, long javaObjectAtom, long windowDeleteAtom /* , long kbdHandle */); // XKB disabled for now

    /** X11 Window delete atom marker used on EDT */
    private long windowDeleteAtom;
    
    /** X11 Window java object property used on EDT */
    private long javaObjectAtom;    
    
    /** X11 Keyboard handle used on EDT */
    // private long kbdHandle; // XKB disabled for now
}


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
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.x11.X11GraphicsDevice;

import jogamp.nativewindow.x11.X11Util;
import jogamp.newt.DisplayImpl;
import jogamp.newt.NEWTJNILibLoader;

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

    /**
     * {@inheritDoc}
     *
     * We use a private non-shared X11 Display instance for EDT window operations and one for exposed animation, eg. OpenGL.
     * <p>
     * In case {@link X11Util#HAS_XLOCKDISPLAY_BUG} and {@link X11Util#XINITTHREADS_ALWAYS_ENABLED}, 
     * we use null locking. Even though this seems not to be rational, it gives most stable results on all platforms.
     * </p>
     * <p>
     * Otherwise we use basic locking via the constructor {@link X11GraphicsDevice#X11GraphicsDevice(long, int, boolean)},
     * since it is possible to share this device via {@link com.jogamp.newt.NewtFactory#createDisplay(String, boolean)}.
     * </p> 
     */
    @SuppressWarnings("unused")
    protected void createNativeImpl() {
        long handle = X11Util.openDisplay(name);
        if( 0 == handle ) {
            throw new RuntimeException("Error creating display(Win): "+name);
        }
        if(USE_SEPARATE_DISPLAY_FOR_EDT) {
            edtDisplayHandle = X11Util.openDisplay(name);
            if( 0 == edtDisplayHandle ) {
                X11Util.closeDisplay(handle);
                throw new RuntimeException("Error creating display(EDT): "+name);
            }
        } else {
            edtDisplayHandle = handle;
        }
        try {
            CompleteDisplay0(edtDisplayHandle);
        } catch(RuntimeException e) {
            closeNativeImpl();
            throw e;
        }
        
        // see API doc above!
        if(X11Util.XINITTHREADS_ALWAYS_ENABLED && X11Util.HAS_XLOCKDISPLAY_BUG) {
            aDevice = new X11GraphicsDevice(handle, AbstractGraphicsDevice.DEFAULT_UNIT, NativeWindowFactory.getNullToolkitLock(), false);            
        } else {
            aDevice = new X11GraphicsDevice(handle, AbstractGraphicsDevice.DEFAULT_UNIT, false);
        }
    }

    protected void closeNativeImpl() {
        DisplayRelease0(edtDisplayHandle, javaObjectAtom, windowDeleteAtom);
        javaObjectAtom = 0;
        windowDeleteAtom = 0;
        // closing using ATI driver bug 'same order'
        final long handle = getHandle();
        X11Util.closeDisplay(handle);
        if(handle != edtDisplayHandle) {
            X11Util.closeDisplay(edtDisplayHandle);
        }
        edtDisplayHandle = 0;
    }

    protected void dispatchMessagesNative() {
        if(0 != edtDisplayHandle) {
            DispatchMessages0(edtDisplayHandle, javaObjectAtom, windowDeleteAtom);
        }
    }

    protected long getEDTHandle() { return edtDisplayHandle; }
    protected long getJavaObjectAtom() { return javaObjectAtom; }
    protected long getWindowDeleteAtom() { return windowDeleteAtom; }
    
    //----------------------------------------------------------------------
    // Internals only
    //
    private static native boolean initIDs0();

    private native void CompleteDisplay0(long handle);

    private void displayCompleted(long javaObjectAtom, long windowDeleteAtom) {
        this.javaObjectAtom=javaObjectAtom;
        this.windowDeleteAtom=windowDeleteAtom;
    }
    private native void DisplayRelease0(long handle, long javaObjectAtom, long windowDeleteAtom);

    private native void DispatchMessages0(long display, long javaObjectAtom, long windowDeleteAtom);

    /**
     * 2011/06/14 libX11 1.4.2 and libxcb 1.7 bug 20708 - Multithreading Issues w/ OpenGL, ..
     *            https://bugs.freedesktop.org/show_bug.cgi?id=20708
     *            https://jogamp.org/bugzilla/show_bug.cgi?id=502
     *            Affects: Ubuntu 11.04, OpenSuSE 11, ..
     *            Workaround: Using a separate X11 Display connection for event dispatching (EDT)
     */    
    private final boolean USE_SEPARATE_DISPLAY_FOR_EDT = true;
    
    private long edtDisplayHandle;
    
    /** X11 Window delete atom marker used on EDT */
    private long windowDeleteAtom;
    
    /** X11 Window java object property used on EDT */
    private long javaObjectAtom;
}


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

package jogamp.newt.driver.x11;

import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;
import jogamp.newt.*;
import jogamp.nativewindow.x11.X11Util;

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
        if(USE_SEPARATE_DISPLAY_FOR_EDT) {
            edtDisplayHandle = X11Util.createDisplay(name);
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
        
        if(X11Util.XINITTHREADS_ALWAYS_ENABLED) {
            // Hack: Force non X11Display locking, even w/ AWT and w/o isFirstUIActionOnProcess() 
            aDevice = new X11GraphicsDevice(handle, AbstractGraphicsDevice.DEFAULT_UNIT, NativeWindowFactory.getNullToolkitLock());            
        } else {
            // Proper: Use AWT/X11Display locking w/ AWT and X11Display locking only w/o isFirstUIActionOnProcess()
            aDevice = new X11GraphicsDevice(handle, AbstractGraphicsDevice.DEFAULT_UNIT);
        }
    }

    protected void closeNativeImpl() {
        final long handle = getHandle();
        if(handle != edtDisplayHandle) {
            X11Util.closeDisplay(edtDisplayHandle);
        }
        X11Util.closeDisplay(handle);
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

    private native void CompleteDisplay0(long handleEDT);

    private native void DispatchMessages0(long display, long javaObjectAtom, long windowDeleteAtom);

    private void displayCompleted(long javaObjectAtom, long windowDeleteAtom) {
        this.javaObjectAtom=javaObjectAtom;
        this.windowDeleteAtom=windowDeleteAtom;
    }

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


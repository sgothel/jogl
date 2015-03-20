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

import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.common.nio.Buffers;
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

    @Override
    public String validateDisplayName(final String name, final long handle) {
        return X11Util.validateDisplayName(name, handle);
    }

    /**
     * {@inheritDoc}
     *
     * We use a private non-shared X11 Display instance for EDT window operations and one for exposed animation, eg. OpenGL.
     */
    @Override
    protected void createNativeImpl() {
        X11Util.setX11ErrorHandler(true, DEBUG ? false : true); // make sure X11 error handler is set
        final long handle = X11Util.openDisplay(name);
        if( 0 == handle ) {
            throw new RuntimeException("Error creating display(Win): "+name);
        }
        aDevice = new X11GraphicsDevice(handle, AbstractGraphicsDevice.DEFAULT_UNIT, true /* owner */);
        try {
            CompleteDisplay0(aDevice.getHandle());
        } catch(final RuntimeException e) {
            closeNativeImpl(aDevice);
            throw e;
        }
    }

    @Override
    protected void closeNativeImpl(final AbstractGraphicsDevice aDevice) {
        DisplayRelease0(aDevice.getHandle(), javaObjectAtom, windowDeleteAtom /*, kbdHandle */); // XKB disabled for now
        javaObjectAtom = 0;
        windowDeleteAtom = 0;
        // kbdHandle = 0;
        aDevice.close(); // closes X11 display
    }

    @Override
    protected void dispatchMessagesNative() {
        final AbstractGraphicsDevice _aDevice = aDevice; // aDevice could be pulled by destroy event
        _aDevice.lock();
        try {
            final long handle = _aDevice.getHandle();
            if(0 != handle) {
                DispatchMessages0(handle, javaObjectAtom, windowDeleteAtom /*, kbdHandle */, // XKB disabled for now
                                          randr_event_base, randr_error_base);
            }
        } finally {
            _aDevice.unlock();
        }
    }

    protected long getJavaObjectAtom() { return javaObjectAtom; }
    protected long getWindowDeleteAtom() { return windowDeleteAtom; }
    // protected long getKbdHandle() { return kbdHandle; } // XKB disabled for now
    protected int getRandREventBase() { return randr_event_base; }
    protected int getRandRErrorBase() { return randr_error_base; }

    /** Returns <code>null</code> if !{@link #isNativeValid()}, otherwise the Boolean value of {@link X11GraphicsDevice#isXineramaEnabled()}. */
    protected Boolean isXineramaEnabled() { return isNativeValid() ? Boolean.valueOf(((X11GraphicsDevice)aDevice).isXineramaEnabled()) : null; }

    @Override
    protected final long createPointerIconImpl(final PixelFormat pixelformat, final int width, final int height, final ByteBuffer pixels, final int hotX, final int hotY) {
        return createPointerIcon(getHandle(), pixels, width, height, hotX, hotY);
    }

    @Override
    protected final void destroyPointerIconImpl(final long displayHandle, final long piHandle) {
        destroyPointerIcon0(displayHandle, piHandle);
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    private static native boolean initIDs0(boolean debug);

    private native void CompleteDisplay0(long handle);

    private void displayCompleted(final long javaObjectAtom, final long windowDeleteAtom /*, long kbdHandle */,
                                  final int randr_event_base, final int randr_error_base) {
        this.javaObjectAtom=javaObjectAtom;
        this.windowDeleteAtom=windowDeleteAtom;
        // this.kbdHandle = kbdHandle; // XKB disabled for now
        this.randr_event_base = randr_event_base;
        this.randr_error_base = randr_error_base;
    }
    private void sendRRScreenChangeNotify(final long event) {
        if( null != rAndR ) {
            rAndR.sendRRScreenChangeNotify(getHandle(), event);
        }
    }
    void registerRandR(final RandR rAndR) {
        this.rAndR = rAndR;
    }
    private native void DisplayRelease0(long handle, long javaObjectAtom, long windowDeleteAtom /*, long kbdHandle */); // XKB disabled for now

    private native void DispatchMessages0(long display, long javaObjectAtom, long windowDeleteAtom /* , long kbdHandle */, // XKB disabled for now
                                          final int randr_event_base, final int randr_error_base);

    private static long createPointerIcon(final long display, final Buffer pixels, final int width, final int height, final int hotX, final int hotY) {
        final boolean pixels_is_direct = Buffers.isDirect(pixels);
        return createPointerIcon0(display,
                                  pixels_is_direct ? pixels : Buffers.getArray(pixels),
                                  pixels_is_direct ? Buffers.getDirectBufferByteOffset(pixels) : Buffers.getIndirectBufferByteOffset(pixels),
                                  pixels_is_direct,
                                  width, height, hotX, hotY);
    }
    private static native long createPointerIcon0(long display, Object pixels, int pixels_byte_offset, boolean pixels_is_direct, int width, int height, int hotX, int hotY);

    private static native void destroyPointerIcon0(long display, long handle);

    /** X11 Window delete atom marker used on EDT */
    private long windowDeleteAtom;

    /** X11 Window java object property used on EDT */
    private long javaObjectAtom;

    /** X11 Keyboard handle used on EDT */
    // private long kbdHandle; // XKB disabled for now
    private int randr_event_base, randr_error_base;

    private RandR rAndR;
}


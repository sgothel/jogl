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
 */

package com.jogamp.nativewindow.x11;

import jogamp.nativewindow.x11.X11Lib;
import jogamp.nativewindow.x11.X11Util;

import com.jogamp.nativewindow.DefaultGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.ToolkitLock;

/** Encapsulates a graphics device on X11 platforms.
 */

public class X11GraphicsDevice extends DefaultGraphicsDevice implements Cloneable {
    /* final */ boolean handleOwner;
    final boolean isXineramaEnabled;

    /** Constructs a new X11GraphicsDevice corresponding to the given connection and default
     *  {@link com.jogamp.nativewindow.ToolkitLock} via {@link NativeWindowFactory#getDefaultToolkitLock(String)}.<br>
     *  Note that this is not an open connection, ie no native display handle exist.
     *  This constructor exist to setup a default device connection.
     *  @see DefaultGraphicsDevice#DefaultGraphicsDevice(String, String, int)
     */
    public X11GraphicsDevice(final String connection, final int unitID) {
        super(NativeWindowFactory.TYPE_X11, connection, unitID);
        handleOwner = false;
        isXineramaEnabled = false;
    }

    /** Constructs a new X11GraphicsDevice corresponding to the given native display handle and default
     *  {@link com.jogamp.nativewindow.ToolkitLock} via {@link NativeWindowFactory#getDefaultToolkitLock(String, long)}.
     *  @see DefaultGraphicsDevice#DefaultGraphicsDevice(String, String, int, long)
     */
    public X11GraphicsDevice(final long display, final int unitID, final boolean owner) {
        this(display, unitID, NativeWindowFactory.getDefaultToolkitLock(NativeWindowFactory.TYPE_X11, display), owner);
    }

    /**
     * @param display the Display connection
     * @param locker custom {@link com.jogamp.nativewindow.ToolkitLock}, eg to force null locking w/ private connection
     * @see DefaultGraphicsDevice#DefaultGraphicsDevice(String, String, int, long, ToolkitLock)
     */
    public X11GraphicsDevice(final long display, final int unitID, final ToolkitLock locker, final boolean owner) {
        super(NativeWindowFactory.TYPE_X11, X11Lib.XDisplayString(display), unitID, display, locker);
        if(0==display) {
            throw new NativeWindowException("null display");
        }
        handleOwner = owner;
        isXineramaEnabled = X11Util.XineramaIsEnabled(this);
    }

    /**
     * Constructs a new X11GraphicsDevice corresponding to the given display connection.
     * <p>
     * The constructor opens the native connection and takes ownership.
     * </p>
     * @param displayConnection the semantic display connection name
     * @param locker custom {@link com.jogamp.nativewindow.ToolkitLock}, eg to force null locking w/ private connection
     * @see DefaultGraphicsDevice#DefaultGraphicsDevice(String, String, int, long, ToolkitLock)
     */
    public X11GraphicsDevice(final String displayConnection, final int unitID, final ToolkitLock locker) {
        super(NativeWindowFactory.TYPE_X11, displayConnection, unitID, 0, locker);
        handleOwner = true;
        open();
        isXineramaEnabled = X11Util.XineramaIsEnabled(this);
    }

    private static int getDefaultScreenImpl(final long dpy) {
        return X11Lib.DefaultScreen(dpy);
    }

    /**
     * Returns the default screen number as referenced by the display connection, i.e. 'somewhere:0.1' -> 1
     * <p>
     * Implementation uses the XLib macro <code>DefaultScreen(display)</code>.
     * </p>
     */
    public int getDefaultScreen() {
        final long display = getHandle();
        if(0==display) {
            throw new NativeWindowException("null display");
        }
        final int ds = getDefaultScreenImpl(display);
        if(DEBUG) {
            System.err.println(Thread.currentThread().getName() + " - X11GraphicsDevice.getDefaultDisplay() of "+this+": "+ds+", count "+X11Lib.ScreenCount(display));
        }
        return ds;
    }

    public int getDefaultVisualID() {
        final long display = getHandle();
        if(0==display) {
            throw new NativeWindowException("null display");
        }
        return X11Lib.DefaultVisualID(display, getDefaultScreenImpl(display));
    }

    public final boolean isXineramaEnabled() {
        return isXineramaEnabled;
    }

    @Override
    public Object clone() {
      return super.clone();
    }

    @Override
    public boolean open() {
        if(handleOwner && 0 == handle) {
            if(DEBUG) {
                System.err.println(Thread.currentThread().getName() + " - X11GraphicsDevice.open(): "+this);
            }
            handle = X11Util.openDisplay(connection);
            if(0 == handle) {
                throw new NativeWindowException("X11GraphicsDevice.open() failed: "+this);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean close() {
        if(handleOwner && 0 != handle) {
            if(DEBUG) {
                System.err.println(Thread.currentThread().getName() + " - X11GraphicsDevice.close(): "+this);
            }
            X11Util.closeDisplay(handle);
        }
        return super.close();
    }

    @Override
    public boolean isHandleOwner() {
        return handleOwner;
    }
    @Override
    public void clearHandleOwner() {
        handleOwner = false;
    }
    @Override
    protected Object getHandleOwnership() {
        return Boolean.valueOf(handleOwner);
    }
    @Override
    protected Object setHandleOwnership(final Object newOwnership) {
        final Boolean oldOwnership = Boolean.valueOf(handleOwner);
        handleOwner = ((Boolean) newOwnership).booleanValue();
        return oldOwnership;
    }
}

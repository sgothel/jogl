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

package javax.media.nativewindow.x11;

import jogamp.nativewindow.Debug;
import jogamp.nativewindow.x11.X11Lib;
import jogamp.nativewindow.x11.X11Util;
import javax.media.nativewindow.DefaultGraphicsDevice;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.ToolkitLock;

/** Encapsulates a graphics device on X11 platforms.
 */

public class X11GraphicsDevice extends DefaultGraphicsDevice implements Cloneable {
    public static final boolean DEBUG = Debug.debug("GraphicsDevice");
    final boolean closeDisplay;

    /** Constructs a new X11GraphicsDevice corresponding to the given connection and default
     *  {@link javax.media.nativewindow.ToolkitLock} via {@link NativeWindowFactory#getDefaultToolkitLock(String)}.<br>
     *  Note that this is not an open connection, ie no native display handle exist.
     *  This constructor exist to setup a default device connection.
     *  @see DefaultGraphicsDevice#DefaultGraphicsDevice(String, String, int)
     */
    public X11GraphicsDevice(String connection, int unitID) {
        super(NativeWindowFactory.TYPE_X11, connection, unitID);
        closeDisplay = false;
    }

    /** Constructs a new X11GraphicsDevice corresponding to the given native display handle and default
     *  {@link javax.media.nativewindow.ToolkitLock} via {@link NativeWindowFactory#createDefaultToolkitLock(String, long)}.
     *  @see DefaultGraphicsDevice#DefaultGraphicsDevice(String, String, int, long)
     */
    public X11GraphicsDevice(long display, int unitID, boolean owner) {
        // FIXME: derive unitID from connection could be buggy, one DISPLAY for all screens for example..
        super(NativeWindowFactory.TYPE_X11, X11Lib.XDisplayString(display), unitID, display);
        if(0==display) {
            throw new NativeWindowException("null display");
        }
        closeDisplay = owner;
    }

    /**
     * @param display the Display connection
     * @param locker custom {@link javax.media.nativewindow.ToolkitLock}, eg to force null locking in NEWT
     * @see DefaultGraphicsDevice#DefaultGraphicsDevice(String, String, int, long, ToolkitLock)
     */
    public X11GraphicsDevice(long display, int unitID, ToolkitLock locker, boolean owner) {
        super(NativeWindowFactory.TYPE_X11, X11Lib.XDisplayString(display), unitID, display, locker);
        if(0==display) {
            throw new NativeWindowException("null display");
        }
        closeDisplay = owner;
    }

    public Object clone() {
      return super.clone();
    }

    public boolean close() {
        // FIXME: shall we respect the unitID ?
        if(closeDisplay && 0 != handle) {
            if(DEBUG) {
                System.err.println(Thread.currentThread().getName() + " - X11GraphicsDevice.close(): "+this);
            }
            X11Util.closeDisplay(handle);
        }
        return super.close();
    }
}


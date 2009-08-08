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
 */

package com.sun.nativewindow.impl;

import java.lang.reflect.*;
import java.security.*;

import javax.media.nativewindow.*;

public class NativeWindowFactoryImpl extends NativeWindowFactory {
    protected static final boolean DEBUG = Debug.debug("NativeWindowFactoryImpl");

    // This subclass of NativeWindowFactory handles the case of
    // NativeWindows being passed in
    protected NativeWindow getNativeWindowImpl(Object winObj, AbstractGraphicsConfiguration config) throws IllegalArgumentException {
        if (null == winObj) {
            throw new IllegalArgumentException("winObj is null");
        }
        if (winObj instanceof NativeWindow) {
            // Use the NativeWindow directly
            return (NativeWindow) winObj;
        }

        if (null == config) {
            throw new IllegalArgumentException("AbstractGraphicsConfiguration is null with a non NativeWindow object");
        }

        if (NWReflection.isAWTComponent(winObj)) {
            return getAWTNativeWindow(winObj, config);
        }

        throw new IllegalArgumentException("Target window object type " +
                                           winObj.getClass().getName() + " is unsupported; expected " +
                                           "javax.media.nativewindow.NativeWindow or java.awt.Component");
    }
    
    private Constructor nativeWindowConstructor = null;

    private NativeWindow getAWTNativeWindow(Object winObj, AbstractGraphicsConfiguration config) {
        if (nativeWindowConstructor == null) {
            try {
                String osType = getNativeWindowType(true);
                String windowClassName = null;

                // We break compile-time dependencies on the AWT here to
                // make it easier to run this code on mobile devices

                if (osType.equals(TYPE_WINDOWS)) {
                    windowClassName = "com.sun.nativewindow.impl.jawt.windows.WindowsJAWTWindow";
                } else if (osType.equals(TYPE_MACOSX)) {
                    windowClassName = "com.sun.nativewindow.impl.jawt.macosx.MacOSXJAWTWindow";
                } else if (osType.equals(TYPE_X11)) {
                    // Assume Linux, Solaris, etc. Should probably test for these explicitly.
                    windowClassName = "com.sun.nativewindow.impl.jawt.x11.X11JAWTWindow";
                } else {
                    throw new IllegalArgumentException("OS " + getNativeOSName(false) + " not yet supported");
                }

                nativeWindowConstructor = NWReflection.getConstructor(windowClassName, new Class[] { Object.class, AbstractGraphicsConfiguration.class });
            } catch (Exception e) {
                throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
            }
        }

        try {
            return (NativeWindow) nativeWindowConstructor.newInstance(new Object[] { winObj, config });
        } catch (Exception ie) {
            throw (IllegalArgumentException) new IllegalArgumentException().initCause(ie);
        }
    }

    // On most platforms the toolkit lock is a no-op
    private ToolkitLock toolkitLock = new ToolkitLock() {
            public void lock() {
            }

            public void unlock() {
            }
        };

    public ToolkitLock getToolkitLock() {
        return toolkitLock;
    }
}

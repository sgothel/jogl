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

package jogamp.nativewindow;

import java.lang.reflect.Constructor;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.ToolkitLock;

import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.ReflectionUtil.AWTNames;

public class NativeWindowFactoryImpl extends NativeWindowFactory {
    private static final ToolkitLock nullToolkitLock = new NullToolkitLock();

    public static ToolkitLock getNullToolkitLock() {
        return nullToolkitLock;
    }

    // This subclass of NativeWindowFactory handles the case of
    // NativeWindows being passed in
    @Override
    protected NativeWindow getNativeWindowImpl(final Object winObj, final AbstractGraphicsConfiguration config) throws IllegalArgumentException {
        if (winObj instanceof NativeWindow) {
            // Use the NativeWindow directly
            return (NativeWindow) winObj;
        }

        if (null == config) {
            throw new IllegalArgumentException("AbstractGraphicsConfiguration is null with a non NativeWindow object");
        }

        if (NativeWindowFactory.isAWTAvailable() && ReflectionUtil.instanceOf(winObj, AWTNames.ComponentClass)) {
            return getAWTNativeWindow(winObj, config);
        }

        throw new IllegalArgumentException("Target window object type " +
                                           winObj.getClass().getName() + " is unsupported; expected " +
                                           "com.jogamp.nativewindow.NativeWindow or "+AWTNames.ComponentClass);
    }

    private Constructor<?> nativeWindowConstructor = null;

    private NativeWindow getAWTNativeWindow(final Object winObj, final AbstractGraphicsConfiguration config) {
        if (nativeWindowConstructor == null) {
            try {
                final String windowingType = getNativeWindowType(true);
                final String windowClassName;

                // We break compile-time dependencies on the AWT here to
                // make it easier to run this code on mobile devices

                if (TYPE_WINDOWS == windowingType) {
                    windowClassName = "jogamp.nativewindow.jawt.windows.WindowsJAWTWindow";
                } else if (TYPE_MACOSX == windowingType) {
                    windowClassName = "jogamp.nativewindow.jawt.macosx.MacOSXJAWTWindow";
                } else if (TYPE_X11 == windowingType) {
                    // Assume Linux, Solaris, etc. Should probably test for these explicitly.
                    windowClassName = "jogamp.nativewindow.jawt.x11.X11JAWTWindow";
                } else {
                    throw new IllegalArgumentException("Native windowing type " + windowingType + " (custom) not yet supported, platform reported native windowing type: "+getNativeWindowType(false));
                }

                nativeWindowConstructor = ReflectionUtil.getConstructor(
                                            windowClassName, new Class[] { Object.class, AbstractGraphicsConfiguration.class },
                                            true, getClass().getClassLoader());
            } catch (final Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        try {
            return (NativeWindow) nativeWindowConstructor.newInstance(new Object[] { winObj, config });
        } catch (final Exception ie) {
            throw new IllegalArgumentException(ie);
        }
    }
}

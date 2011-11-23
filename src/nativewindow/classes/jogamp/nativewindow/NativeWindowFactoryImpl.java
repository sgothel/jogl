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

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.ToolkitLock;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.ReflectionUtil;

public class NativeWindowFactoryImpl extends NativeWindowFactory {
    private static final ToolkitLock nullToolkitLock = new NullToolkitLock();

    public static ToolkitLock getNullToolkitLock() {
            return nullToolkitLock;
    }
    
    // This subclass of NativeWindowFactory handles the case of
    // NativeWindows being passed in
    protected NativeWindow getNativeWindowImpl(Object winObj, AbstractGraphicsConfiguration config) throws IllegalArgumentException {
        if (winObj instanceof NativeWindow) {
            // Use the NativeWindow directly
            return (NativeWindow) winObj;
        }

        if (null == config) {
            throw new IllegalArgumentException("AbstractGraphicsConfiguration is null with a non NativeWindow object");
        }

        if (NativeWindowFactory.isAWTAvailable() && ReflectionUtil.instanceOf(winObj, AWTComponentClassName)) {
            return getAWTNativeWindow(winObj, config);
        }

        throw new IllegalArgumentException("Target window object type " +
                                           winObj.getClass().getName() + " is unsupported; expected " +
                                           "javax.media.nativewindow.NativeWindow or "+AWTComponentClassName);
    }
    
    private Constructor<?> nativeWindowConstructor = null;

    private NativeWindow getAWTNativeWindow(Object winObj, AbstractGraphicsConfiguration config) {
        if (nativeWindowConstructor == null) {
            try {
                String windowingType = getNativeWindowType(true);
                String windowClassName = null;

                // We break compile-time dependencies on the AWT here to
                // make it easier to run this code on mobile devices

                if (windowingType.equals(TYPE_WINDOWS)) {
                    windowClassName = "jogamp.nativewindow.jawt.windows.WindowsJAWTWindow";
                } else if (windowingType.equals(TYPE_MACOSX)) {
                    windowClassName = "jogamp.nativewindow.jawt.macosx.MacOSXJAWTWindow";
                } else if (windowingType.equals(TYPE_X11)) {
                    // Assume Linux, Solaris, etc. Should probably test for these explicitly.
                    windowClassName = "jogamp.nativewindow.jawt.x11.X11JAWTWindow";
                } else {
                    throw new IllegalArgumentException("OS " + Platform.getOSName() + " not yet supported");
                }

                nativeWindowConstructor = ReflectionUtil.getConstructor(
                                            windowClassName, new Class[] { Object.class, AbstractGraphicsConfiguration.class }, 
                                            getClass().getClassLoader());
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        try {
            return (NativeWindow) nativeWindowConstructor.newInstance(new Object[] { winObj, config });
        } catch (Exception ie) {
            throw new IllegalArgumentException(ie);
        }
    }
}

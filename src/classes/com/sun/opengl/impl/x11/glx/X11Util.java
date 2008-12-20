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

package com.sun.opengl.impl.x11.glx;

import javax.media.opengl.*;

import com.sun.opengl.impl.*;
import com.sun.opengl.impl.x11.*;

public class X11Util {
    private static final boolean DEBUG = Debug.debug("X11Util");

    private X11Util() {}

    // ATI's proprietary drivers apparently send GLX tokens even for
    // direct contexts, so we need to disable the context optimizations
    // in this case
    private static boolean isVendorATI;

    // Display connection for use by visual selection algorithm and by all offscreen surfaces
    private static long staticDisplay=0;
    private static boolean xineramaEnabled=false;
    private static boolean multisampleAvailable=false;
    public static long getDisplayConnection() {
        if (staticDisplay == 0) {
            NativeWindowFactory.getDefaultFactory().getToolkitLock().lock();
            try {
                staticDisplay = X11Lib.XOpenDisplay(null);
                if (DEBUG && (staticDisplay != 0)) {
                    long display = staticDisplay;
                    int screen = X11Lib.DefaultScreen(display);
                    System.err.println("!!! GLX server vendor : " +
                                       GLX.glXQueryServerString(display, screen, GLX.GLX_VENDOR));
                    System.err.println("!!! GLX server version: " +
                                       GLX.glXQueryServerString(display, screen, GLX.GLX_VERSION));
                    System.err.println("!!! GLX client vendor : " +
                                       GLX.glXGetClientString(display, GLX.GLX_VENDOR));
                    System.err.println("!!! GLX client version: " +
                                       GLX.glXGetClientString(display, GLX.GLX_VERSION));
                }

                if (staticDisplay != 0) {
                    String vendor = GLX.glXGetClientString(staticDisplay, GLX.GLX_VENDOR);
                    if (vendor != null && vendor.startsWith("ATI")) {
                        isVendorATI = true;
                    }
                    xineramaEnabled = X11Lib.XineramaEnabled(staticDisplay);
                    String exts = GLX.glXGetClientString(staticDisplay, GLX.GLX_EXTENSIONS);
                    if (exts != null) {
                        multisampleAvailable = (exts.indexOf("GLX_ARB_multisample") >= 0);
                    }
                }
            } finally {
                NativeWindowFactory.getDefaultFactory().getToolkitLock().unlock();
            }
            if (staticDisplay == 0) {
                throw new GLException("Unable to open default display, needed for visual selection and offscreen surface handling");
            }
        }
        return staticDisplay;
    }

    public static boolean isXineramaEnabled() {
        if (staticDisplay == 0) {
            getDisplayConnection(); // will set xineramaEnabled
        }
        return xineramaEnabled;
    }

    public static boolean isMultisampleAvailable() {
        if (staticDisplay == 0) {
            getDisplayConnection(); // will set multisampleAvailable
        }
        return multisampleAvailable;
    }

    /** Workaround for apparent issue with ATI's proprietary drivers
        where direct contexts still send GLX tokens for GL calls */
    public static boolean isVendorATI() {
        return isVendorATI;
    }
}

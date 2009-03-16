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

package com.sun.nwi.impl.x11;

import javax.media.nwi.*;

import com.sun.nwi.impl.*;

public class X11Util {
    static {
        NativeLibLoaderBase.loadNWI("x11");
    }

    private static final boolean DEBUG = Debug.debug("X11Util");

    private X11Util() {}

    // Display connection for use by visual selection algorithm and by all offscreen surfaces
    private static long staticDisplay=0;
    private static boolean xineramaEnabled=false;
    public static long getDisplayConnection() {
        if (staticDisplay == 0) {
            NativeWindowFactory.getDefaultFactory().getToolkitLock().lock();
            try {
                staticDisplay = X11Lib.XOpenDisplay(null);
                if (staticDisplay != 0) {
                    xineramaEnabled = X11Lib.XineramaEnabled(staticDisplay);
                }
            } finally {
                NativeWindowFactory.getDefaultFactory().getToolkitLock().unlock();
            }
            if (staticDisplay == 0) {
                throw new NWException("Unable to open default display");
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
}

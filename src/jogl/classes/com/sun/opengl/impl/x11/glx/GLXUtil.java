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
import javax.media.nativewindow.NativeWindowFactory;
import com.sun.nativewindow.impl.x11.*;

public class GLXUtil {
    public static boolean isMultisampleAvailable(long display) {
        try {
            X11Lib.XLockDisplay(display);
            String exts = GLX.glXGetClientString(display, GLX.GLX_EXTENSIONS);
            if (exts != null) {
                return (exts.indexOf("GLX_ARB_multisample") >= 0);
            }
            return false;
        } finally {
            X11Lib.XUnlockDisplay(display);
        }
    }

    /** Workaround for apparent issue with ATI's proprietary drivers
        where direct contexts still send GLX tokens for GL calls */
    public static boolean isVendorATI(long display) {
        try {
            X11Lib.XLockDisplay(display);
            String vendor = GLX.glXGetClientString(display, GLX.GLX_VENDOR);
            return vendor != null && vendor.startsWith("ATI") ;
        } finally {
            X11Lib.XUnlockDisplay(display);
        }
    }
}

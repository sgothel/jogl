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

package com.sun.nativewindow.impl.x11;

import java.util.HashMap;

import javax.media.nativewindow.*;

import com.sun.nativewindow.impl.*;

/**
 * Contains a thread safe X11 utility to retrieve tread local display connection,<br>
 * as well as the static global discplay connection.<br>
 *
 * The TLS variant is thread safe per se, but has the memory leak risk on applications
 * heavily utilizing runnables on a new thread.<br>
 * 
 * The static variant is more nice to resources, but involves the default toolkit
 * locking mechanism, which invocation you have to provide as shown below.<br>
 * <PRE>
   NativeWindowFactory.getDefaultFactory().getToolkitLock().lock();
   try {
     long displayHandle = X11Util.getStaticDefaultDisplay();
     ...
   } finally {
     NativeWindowFactory.getDefaultFactory().getToolkitLock().unlock();
   }
 * </PRE><br>
 *
 * We will use the TLS variant, where a long term display connection is being used,
 * ie in JOGL's <code>X11AWTGLXGraphicsConfigurationFactory</code>,
 * on all other situations where we just query some X11 attributes,
 * the locked static variant shall be used instead.<br>
 */
public class X11Util {
    private static final boolean DEBUG = Debug.debug("X11Util");

    static {
        NativeLibLoaderBase.loadNativeWindow("x11");
    }

    private X11Util() {}

    private static ThreadLocal currentDisplayAssociation = new ThreadLocal();

    private static long    staticDefaultDisplay=0; 
    private static boolean staticDefaultDisplayXineramaEnable=false; 

    private static long fetchStaticDefaultDisplay() {
        if(0==staticDefaultDisplay) {
            synchronized (X11Util.class) {
                if(0==staticDefaultDisplay) {
                    staticDefaultDisplay = X11Lib.XOpenDisplay(null);
                    if(0==staticDefaultDisplay) {
                        throw new NativeWindowException("Unable to create a static default display connection");
                    }
                    staticDefaultDisplayXineramaEnable = X11Lib.XineramaEnabled(staticDefaultDisplay);
                }
            }
        }
        return staticDefaultDisplay;
    }

    /** Returns the global static default display connection, read the toolkit lock/unlock
      * requirements {@link X11Util above} for synchronization. */
    public static long getStaticDefaultDisplay() {
        return fetchStaticDefaultDisplay();
    }

    /** Returns the global static default display connection, read the toolkit lock/unlock
      * requirements {@link X11Util above} for synchronization. */
    public static boolean isXineramaEnabledOnStaticDefaultDisplay() {
        fetchStaticDefaultDisplay();
        return staticDefaultDisplayXineramaEnable;
    }


    /** Returns this thread current default display. */
    public static long getThreadLocalDefaultDisplay() {
        Long dpyL = (Long) currentDisplayAssociation.get();
        if(null==dpyL) {
            long dpy = X11Lib.XOpenDisplay(null);
            if(0==dpy) {
                throw new NativeWindowException("Unable to create a default display connection on Thread "+Thread.currentThread().getName());
            }
            dpyL = new Long(dpy);
            currentDisplayAssociation.set( dpyL );
            if(DEBUG) {
                Exception e = new Exception("Created new TLS display connection 0x"+Long.toHexString(dpy)+" for thread "+Thread.currentThread().getName());
                e.printStackTrace();
            }
        }
        return dpyL.longValue();
    }
}

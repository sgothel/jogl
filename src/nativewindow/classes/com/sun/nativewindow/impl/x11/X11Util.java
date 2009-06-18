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
import java.util.Map;

import javax.media.nativewindow.*;

import com.sun.nativewindow.impl.*;

/**
 * Contains a thread safe X11 utility to retrieve thread local display connection,<br>
 * as well as the static global discplay connection.<br>
 *
 * The TLS variant is thread safe per se, but be aware of the memory leak risk 
 * where an application heavily utilizing this class on temporary new threads.<br>
 */
public class X11Util {
    private static final boolean DEBUG = Debug.debug("X11Util");

    static {
        NativeLibLoaderBase.loadNativeWindow("x11");
    }

    private X11Util() {}

    private static ThreadLocal currentDisplayMap = new ThreadLocal();

    public static class NamedDisplay implements Cloneable {
        private String name;
        private long   handle;

        protected NamedDisplay(String name, long handle) {
            this.name=name;
            this.handle=handle;
        }

        public String getName() { return name; }
        public long   getHandle() { return handle; }

        public Object clone() throws CloneNotSupportedException {
          return super.clone();
        }
    }

    /** Returns a clone of the thread local display map, you may {@link Object#wait()} on it */
    public static Map getCurrentDisplayMap() {
        return (Map) ((HashMap)getCurrentDisplayMapImpl()).clone();
    }

    /** Returns this thread current default display.  If it doesn not exist, it is being created */
    public static long getThreadLocalDefaultDisplay() {
        return getThreadLocalDisplay(null);
    }

    /** Returns this thread named display. If it doesn not exist, it is being created */
    public static long getThreadLocalDisplay(String name) {
        NamedDisplay namedDpy = getCurrentDisplay(name);
        if(null==namedDpy) {
            long dpy = X11Lib.XOpenDisplay(name);
            if(0==dpy) {
                throw new NativeWindowException("X11Util.Display: Unable to create a display("+name+") connection in Thread "+Thread.currentThread().getName());
            }
            namedDpy = new NamedDisplay(name, dpy);
            setCurrentDisplay( namedDpy );
            if(DEBUG) {
                Exception e = new Exception("X11Util.Display: Created new TLS display("+name+") connection 0x"+Long.toHexString(dpy)+" in thread "+Thread.currentThread().getName());
                e.printStackTrace();
            }
        }
        return namedDpy.getHandle();
    }

    /** Closes this thread named display. It returns the handle of the closed display or 0, if it does not exist. */
    public static long closeThreadLocalDisplay(String name) {
        NamedDisplay namedDpy = removeCurrentDisplay(name);
        if(null==namedDpy) {
            if(DEBUG) {
                Exception e = new Exception("X11Util.Display: Display("+name+") with given handle is not mapped to TLS in thread "+Thread.currentThread().getName());
                e.printStackTrace();
            }
            return 0;
        }
        long dpy = namedDpy.getHandle();
        X11Lib.XCloseDisplay(dpy);
        if(DEBUG) {
            Exception e = new Exception("X11Util.Display: Closed TLS Display("+name+") with handle 0x"+Long.toHexString(dpy)+" in thread "+Thread.currentThread().getName());
            e.printStackTrace();
        }
        return dpy;
    }

    private static Map getCurrentDisplayMapImpl() {
        Map displayMap = (Map) currentDisplayMap.get();
        if(null==displayMap) {
            displayMap = new HashMap();
            currentDisplayMap.set( displayMap );
        }
        return displayMap;
    }

    /** maps the given display to the thread local display map
      * and notifies all threads synchronized to this display map. */
    private static NamedDisplay setCurrentDisplay(NamedDisplay newDisplay) {
        Map displayMap = getCurrentDisplayMapImpl();
        NamedDisplay oldDisplay = null;
        synchronized(displayMap) {
            String name = (null==newDisplay.getName())?"nil":newDisplay.getName();
            oldDisplay = (NamedDisplay) displayMap.put(name, newDisplay);
            displayMap.notifyAll();
        }
        return oldDisplay;
    }

    /** removes the mapping of the given name from the thread local display map
      * and notifies all threads synchronized to this display map. */
    private static NamedDisplay removeCurrentDisplay(String name) {
        Map displayMap = getCurrentDisplayMapImpl();
        NamedDisplay oldDisplay = null;
        synchronized(displayMap) {
            if(null==name) name="nil";
            oldDisplay = (NamedDisplay) displayMap.remove(name);
            displayMap.notifyAll();
        }
        return oldDisplay;
    }

    /** Returns the thread local display mapped to the given name */
    private static NamedDisplay getCurrentDisplay(String name) {
        if(null==name) name="nil";
        Map displayMap = getCurrentDisplayMapImpl();
        return (NamedDisplay) displayMap.get(name);
    }

}

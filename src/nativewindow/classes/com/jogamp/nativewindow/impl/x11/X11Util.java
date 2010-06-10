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

package com.jogamp.nativewindow.impl.x11;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import com.jogamp.common.util.LongObjectHashMap;

import javax.media.nativewindow.*;

import com.jogamp.nativewindow.impl.*;

/**
 * Contains a thread safe X11 utility to retrieve thread local display connection,<br>
 * as well as the static global display connection.<br>
 *
 * The TLS variant is thread safe per se, but be aware of the memory leak risk 
 * where an application heavily utilizing this class on temporary new threads.<br>
 */
public class X11Util {
    private static final boolean DEBUG = Debug.debug("X11Util");

    public static final String nullDisplayName;

    static {
        NWJNILibLoader.loadNativeWindow("x11");

        initialize( true );

        long dpy = X11Lib.XOpenDisplay(null);
        nullDisplayName = X11Lib.XDisplayString(dpy);
        X11Lib.XCloseDisplay(dpy);
        if(DEBUG) {
            System.out.println("X11 Display(NULL) <"+nullDisplayName+">");
        }
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }

    private X11Util() {}

    // not exactly thread safe, but good enough for our purpose,
    // which is to tag a NamedDisplay uncloseable after creation.
    private static Object globalLock = new Object(); 
    private static LongObjectHashMap globalNamedDisplayMap = new LongObjectHashMap();

    private static ThreadLocal currentDisplayMap = new ThreadLocal();

    public static class NamedDisplay extends RecursiveToolkitLock implements Cloneable {
        String name;
        long   handle;
        int    refCount;
        boolean unCloseable;

        protected NamedDisplay(String name, long handle) {
            this.name=name;
            this.handle=handle;
            this.refCount=1;
            this.unCloseable=false;
        }

        public final String getName() { return name; }
        public final long   getHandle() { return handle; }
        public final int    getRefCount() { return refCount; }
        public final boolean isUncloseable() { return unCloseable; }

        public Object clone() throws CloneNotSupportedException {
          return super.clone();
        }

        public String toString() {
            return "NamedX11Display["+name+", 0x"+Long.toHexString(handle)+", refCount "+refCount+", unCloseable "+unCloseable+"]";
        }
    }

    /** Returns the number of unclosed X11 Displays.
      * @param realXClosePendingDisplays if true, call XCloseDisplay on the remaining ones
      */
    public static int shutdown(boolean realXClosePendingDisplays, boolean verbose) {
        int num=0;
        String msg = null;
        if(DEBUG||verbose) {
            msg = "X11Util.Display: Shutdown (global: "+globalNamedDisplayMap.size()+ ")" ;
            if(DEBUG) {
                Exception e = new Exception(msg);
                e.printStackTrace();
            } else if(verbose) {
                System.err.println(msg);
            }
        }

        synchronized(globalLock) {
            globalNamedDisplayMap.clear();
        }
        return num;
    }

    /*******************************
     **
     ** TLS Management
     ** 
     *******************************/

    /** Returns a clone of the thread local display map, you may {@link Object#wait()} on it */
    public static Map getCurrentDisplayMap() {
        return (Map) ((HashMap)getCurrentDisplayMapImpl()).clone();
    }

    /** Returns this thread named display. If it doesn not exist, it is being created, otherwise the reference count is increased */
    public static long createThreadLocalDisplay(String name) {
        name = validateDisplayName(name);
        NamedDisplay namedDpy = getCurrentDisplay(name);
        if(null==namedDpy) {
            long dpy = X11Lib.XOpenDisplay(name);
            if(0==dpy) {
                throw new NativeWindowException("X11Util.Display: Unable to create a display("+name+") connection in Thread "+Thread.currentThread().getName());
            }
            // if you like to debug and synchronize X11 commands ..
            // setSynchronizeDisplay(dpy, true);
            namedDpy = new NamedDisplay(name, dpy);
            addCurrentDisplay( namedDpy );
            synchronized(globalLock) {
                globalNamedDisplayMap.put(dpy, namedDpy);
            }
            if(DEBUG) {
                Exception e = new Exception("X11Util.Display: Created new TLS "+namedDpy+" in thread "+Thread.currentThread().getName());
                e.printStackTrace();
            }
        } else {
            namedDpy.refCount++;
            if(DEBUG) {
                Exception e = new Exception("X11Util.Display: Reused TLS "+namedDpy+" in thread "+Thread.currentThread().getName());
                e.printStackTrace();
            }
        }
        return namedDpy.getHandle();
    }

    /** Decrease the reference count of this thread named display. If it reaches 0, close it. 
        It returns the handle of the to be closed display.
        It throws a RuntimeException in case the named display does not exist, 
        or the reference count goes below 0.
     */
    public static long closeThreadLocalDisplay(String name) {
        name = validateDisplayName(name);
        NamedDisplay namedDpy = getCurrentDisplay(name);
        if(null==namedDpy) {
            throw new RuntimeException("X11Util.Display: Display("+name+") with given name is not mapped to TLS in thread "+Thread.currentThread().getName());
        }
        if(0==namedDpy.refCount) {
            throw new RuntimeException("X11Util.Display: "+namedDpy+" has refCount already 0 in thread "+Thread.currentThread().getName());
        }
        long dpy = namedDpy.getHandle();
        namedDpy.refCount--;
        if(0==namedDpy.refCount) {
            if(DEBUG) {
                String type = namedDpy.isUncloseable() ? "passive" : "real" ;
                Exception e = new Exception("X11Util.Display: Closing ( "+type+" ) TLS "+namedDpy+" in thread "+Thread.currentThread().getName());
                e.printStackTrace();
            }
            removeCurrentDisplay(namedDpy);
            synchronized(globalLock) {
                if(null==globalNamedDisplayMap.remove(dpy)) { throw new RuntimeException("Internal: "+namedDpy); }
            }
            if(!namedDpy.isUncloseable()) {
                X11Lib.XCloseDisplay(dpy);
            }
        } else if(DEBUG) {
            Exception e = new Exception("X11Util.Display: Keep TLS "+namedDpy+" in thread "+Thread.currentThread().getName());
            e.printStackTrace();
        }
        return dpy;
    }

    public static long closeThreadLocalDisplay(long handle) {
        NamedDisplay ndpy;
        synchronized(globalLock) {
            ndpy = (NamedDisplay) globalNamedDisplayMap.get(handle);
        }
        if(null==ndpy) {
            throw new RuntimeException("X11Util.Display: Display(0x"+Long.toHexString(handle)+") with given handle is not mapped, in thread "+Thread.currentThread().getName());
        }
        return closeThreadLocalDisplay(ndpy.getName());
    }

    public static boolean markThreadLocalDisplayUncloseable(long handle) {
        NamedDisplay ndpy;
        synchronized(globalLock) {
            ndpy = (NamedDisplay) globalNamedDisplayMap.get(handle);
        }
        if( null != ndpy ) {
            ndpy.unCloseable=true;
            return true;
        }
        return false;
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
    private static NamedDisplay addCurrentDisplay(NamedDisplay newDisplay) {
        Map displayMap = getCurrentDisplayMapImpl();
        NamedDisplay oldDisplay = null;
        synchronized(displayMap) {
            oldDisplay = (NamedDisplay) displayMap.put(newDisplay.getName(), newDisplay);
            displayMap.notifyAll();
        }
        return oldDisplay;
    }

    /** removes the mapping of the given name from the thread local display map
      * and notifies all threads synchronized to this display map. */
    private static NamedDisplay removeCurrentDisplay(NamedDisplay ndpy) {
        Map displayMap = getCurrentDisplayMapImpl();
        synchronized(displayMap) {
            NamedDisplay ndpyDel = (NamedDisplay) displayMap.remove(ndpy.getName());
            if(ndpyDel!=ndpy) {
                throw new RuntimeException("Wrong mapping req: "+ndpy+", got "+ndpyDel);
            }
            displayMap.notifyAll();
        }
        return ndpy;
    }

    /** Returns the thread local display mapped to the given name */
    private static NamedDisplay getCurrentDisplay(String name) {
        Map displayMap = getCurrentDisplayMapImpl();
        return (NamedDisplay) displayMap.get(name);
    }

    /*******************************
     **
     ** Non TLS Functions
     ** 
     *******************************/

    /** 
     * @return If name is null, it returns the previous queried NULL display name,
     * otherwise the name. */
    public static String validateDisplayName(String name) {
        return ( null == name ) ? nullDisplayName : name ;
    }

    public static String validateDisplayName(String name, long handle) {
        if(null==name && 0!=handle) {
            name = getNameOfDisplay(handle);
        }
        return validateDisplayName(name);
    }

    public static boolean setSynchronizeDisplay(long handle, boolean onoff) {
        boolean res = X11Lib.XSynchronize(handle, onoff);
        return res;
    }

    public static String getNameOfDisplay(long handle) {
        String name = X11Lib.XDisplayString(handle);
        return name;
    }

    public static void XLockDisplay(long handle) {
        if(DEBUG) {
            System.out.println("... X11 Display Lock try 0x"+Long.toHexString(handle));
        }
        X11Lib.XLockDisplay(handle);
        if(DEBUG) {
            System.out.println("+++ X11 Display Lock got 0x"+Long.toHexString(handle));
        }
    }

    public static void XUnlockDisplay(long handle) {
        if(DEBUG) {
            System.out.println("--- X11 Display Lock rel 0x"+Long.toHexString(handle));
        }
        X11Lib.XUnlockDisplay(handle);
    }

    private static native void initialize(boolean initConcurrentThreadSupport);
}

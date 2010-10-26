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
import com.jogamp.common.util.LongObjectHashMap;
import com.jogamp.common.util.locks.RecursiveLock;

import javax.media.nativewindow.*;

import com.jogamp.nativewindow.impl.*;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.security.AccessController;

/**
 * Contains a thread safe X11 utility to retrieve thread local display connection,<br>
 * as well as the static global display connection.<br>
 *
 * The TLS variant is thread safe per se, but be aware of the memory leak risk 
 * where an application heavily utilizing this class on temporary new threads.<br>
 */
public class X11Util {
    private static final boolean DEBUG = Debug.debug("X11Util");
    private static final boolean TRACE_DISPLAY_LIFECYCLE = Debug.getBooleanProperty("nativewindow.debug.X11Util.TraceDisplayLifecycle", true, AccessController.getContext());

    private static String nullDisplayName = null;
    private static boolean isFirstX11ActionOnProcess = false;
    private static boolean isInit = false;

    public static synchronized void initSingleton(boolean firstX11ActionOnProcess) {
        if(!isInit) {
            NWJNILibLoader.loadNativeWindow("x11");

            /**
             * Always issue XInitThreads() since we have independent
             * off-thread created Display connections able to utilize multithreading, ie NEWT */
            initialize( true );
            // initialize( firstX11ActionOnProcess );
            isFirstX11ActionOnProcess = firstX11ActionOnProcess;

            if(DEBUG) {
                System.out.println("X11Util.isFirstX11ActionOnProcess: "+isFirstX11ActionOnProcess);
            }
            isInit = true;
        }
    }

    public static boolean isFirstX11ActionOnProcess() {
        return isFirstX11ActionOnProcess;
    }

    public static void lockDefaultToolkit(long dpyHandle) {
        NativeWindowFactory.getDefaultToolkitLock().lock();
        if(!isFirstX11ActionOnProcess) {
            X11Util.XLockDisplay(dpyHandle);
        }
    }

    public static void unlockDefaultToolkit(long dpyHandle) {
        if(!isFirstX11ActionOnProcess) {
            X11Util.XUnlockDisplay(dpyHandle);
        }
        NativeWindowFactory.getDefaultToolkitLock().unlock();
    }

    public static String getNullDisplayName() {
        if(null==nullDisplayName) {
            synchronized(X11Util.class) {
                if(null==nullDisplayName) {
                    NativeWindowFactory.getDefaultToolkitLock().lock();
                    try {
                        long dpy = X11Lib.XOpenDisplay(null);
                        nullDisplayName = X11Lib.XDisplayString(dpy);
                        X11Lib.XCloseDisplay(dpy);
                    } finally {
                        NativeWindowFactory.getDefaultToolkitLock().unlock();
                    }
                    if(DEBUG) {
                        System.out.println("X11 Display(NULL) <"+nullDisplayName+">");
                    }
                }
            }
        }
        return nullDisplayName;
    }

    private X11Util() {}

    // not exactly thread safe, but good enough for our purpose,
    // which is to tag a NamedDisplay uncloseable after creation.
    private static Object globalLock = new Object(); 
    private static LongObjectHashMap globalNamedDisplayMap = new LongObjectHashMap();

    private static ThreadLocal currentDisplayMap = new ThreadLocal();

    public static class NamedDisplay {
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

        public void setUncloseable(boolean v) { unCloseable = v; }
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
        if(DEBUG||verbose) {
            String msg = "X11Util.Display: Shutdown (closePendingDisplays: "+realXClosePendingDisplays+
                         ", global: "+globalNamedDisplayMap.size()+ ")" ;
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
            long dpy = XOpenDisplay(name);
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
                XCloseDisplay(dpy);
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
            ndpy.setUncloseable(true);
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

    /** Returns this created named display. */
    public static long createDisplay(String name) {
        name = validateDisplayName(name);
        long dpy = XOpenDisplay(name);
        if(0==dpy) {
            throw new NativeWindowException("X11Util.Display: Unable to create a display("+name+") connection. Thread "+Thread.currentThread().getName());
        }
        // if you like to debug and synchronize X11 commands ..
        // setSynchronizeDisplay(dpy, true);
        NamedDisplay namedDpy = new NamedDisplay(name, dpy);
        synchronized(globalLock) {
            globalNamedDisplayMap.put(dpy, namedDpy);
        }
        if(DEBUG) {
            Exception e = new Exception("X11Util.Display: Created new "+namedDpy+". Thread "+Thread.currentThread().getName());
            e.printStackTrace();
        }
        return namedDpy.getHandle();
    }

    public static void closeDisplay(long handle) {
        NamedDisplay namedDpy;

        synchronized(globalLock) {
            namedDpy = (NamedDisplay) globalNamedDisplayMap.remove(handle);
        }
        if(null==namedDpy) {
            throw new RuntimeException("X11Util.Display: Display(0x"+Long.toHexString(handle)+") with given handle is not mapped. Thread "+Thread.currentThread().getName());
        }
        if(namedDpy.getHandle()!=handle) {
            throw new RuntimeException("X11Util.Display: Display(0x"+Long.toHexString(handle)+") Mapping error: "+namedDpy+". Thread "+Thread.currentThread().getName());
        }

        if(DEBUG) {
            Exception e = new Exception("X11Util.Display: Closing new "+namedDpy+". Thread "+Thread.currentThread().getName());
            e.printStackTrace();
        }

        if(!namedDpy.isUncloseable()) {
            XCloseDisplay(namedDpy.getHandle());
        }
    }

    public static NamedDisplay getNamedDisplay(long handle) {
        synchronized(globalLock) {
            return (NamedDisplay) globalNamedDisplayMap.get(handle);
        }
    }

    /** 
     * @return If name is null, it returns the previous queried NULL display name,
     * otherwise the name. */
    public static String validateDisplayName(String name) {
        return ( null == name ) ? getNullDisplayName() : name ;
    }

    public static String validateDisplayName(String name, long handle) {
        if(null==name && 0!=handle) {
            name = XDisplayString(handle);
        }
        return validateDisplayName(name);
    }

    /*******************************
     **
     ** Locked X11Lib wrapped functions
     **
     *******************************/
    public static long XOpenDisplay(String arg0) {
        NativeWindowFactory.getDefaultToolkitLock().lock();
        try {
            long handle = X11Lib.XOpenDisplay(arg0);
            if(TRACE_DISPLAY_LIFECYCLE) {
                Throwable t = new Throwable(Thread.currentThread()+" - X11Util.XOpenDisplay("+arg0+") 0x"+Long.toHexString(handle));
                t.printStackTrace();
            }
            return handle;
        } finally {
            NativeWindowFactory.getDefaultToolkitLock().unlock();
        }
    }

    public static int XCloseDisplay(long display) {
        NativeWindowFactory.getDefaultToolkitLock().lock();
        try {
            if(TRACE_DISPLAY_LIFECYCLE) {
                Throwable t = new Throwable(Thread.currentThread()+" - X11Util.XCloseDisplay() 0x"+Long.toHexString(display));
                t.printStackTrace();
            }
            return X11Lib.XCloseDisplay(display);
        } finally {
            NativeWindowFactory.getDefaultToolkitLock().unlock();
        }
    }

    public static int XFree(Buffer arg0)  {
        NativeWindowFactory.getDefaultToolkitLock().lock();
        try {
            return X11Lib.XFree(arg0);
        } finally {
            NativeWindowFactory.getDefaultToolkitLock().unlock();
        }
    }

    public static int XSync(long display, boolean discard) {
        lockDefaultToolkit(display);
        try {
            return X11Lib.XSync(display, discard);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static boolean XSynchronize(long display, boolean onoff) {
        lockDefaultToolkit(display);
        try {
            return X11Lib.XSynchronize(display, onoff);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static boolean XineramaEnabled(long display) {
        lockDefaultToolkit(display);
        try {
            return X11Lib.XineramaEnabled(display);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static int DefaultScreen(long display) {
        lockDefaultToolkit(display);
        try {
            return X11Lib.DefaultScreen(display);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static long RootWindow(long display, int screen_number) {
        lockDefaultToolkit(display);
        try {
            return X11Lib.RootWindow(display, screen_number);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static long XCreatePixmap(long display, long arg1, int arg2, int arg3, int arg4) {
        lockDefaultToolkit(display);
        try {
            return X11Lib.XCreatePixmap(display, arg1, arg2, arg3, arg4);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static String XDisplayString(long display) {
        lockDefaultToolkit(display);
        try {
            return X11Lib.XDisplayString(display);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static int XFlush(long display) {
        lockDefaultToolkit(display);
        try {
            return X11Lib.XFlush(display);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static int XFreePixmap(long display, long arg1) {
        lockDefaultToolkit(display);
        try {
            return X11Lib.XFreePixmap(display, arg1);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static long DefaultVisualID(long display, int screen) {
        lockDefaultToolkit(display);
        try {
            return X11Lib.DefaultVisualID(display, screen);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static long CreateDummyWindow(long display, int screen_index, long visualID) {
        lockDefaultToolkit(display);
        try {
            return X11Lib.CreateDummyWindow(display, screen_index, visualID);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static void DestroyDummyWindow(long display, long window) {
        lockDefaultToolkit(display);
        try {
            X11Lib.DestroyDummyWindow(display, window);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static XVisualInfo[] XGetVisualInfo(long display, long arg1, XVisualInfo arg2, int[] arg3, int arg3_offset) {
        lockDefaultToolkit(display);
        try {
            return X11Lib.XGetVisualInfo(display, arg1, arg2, arg3, arg3_offset);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static boolean XF86VidModeGetGammaRamp(long display, int screen, int size, ShortBuffer red_array, ShortBuffer green_array, ShortBuffer blue_array)  {
        lockDefaultToolkit(display);
        try {
            return X11Lib.XF86VidModeGetGammaRamp(display, screen, size, red_array, green_array, blue_array);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static boolean XF86VidModeGetGammaRamp(long display, int screen, int size, short[] red_array, int red_array_offset, short[] green_array, int green_array_offset, short[] blue_array, int blue_array_offset)  {
        lockDefaultToolkit(display);
        try {
            return X11Lib.XF86VidModeGetGammaRamp(display, screen, size, red_array, red_array_offset, green_array, green_array_offset, blue_array, blue_array_offset);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static boolean XF86VidModeGetGammaRampSize(long display, int screen, IntBuffer size)  {
        lockDefaultToolkit(display);
        try {
            return X11Lib.XF86VidModeGetGammaRampSize(display, screen, size);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static boolean XF86VidModeGetGammaRampSize(long display, int screen, int[] size, int size_offset)  {
        lockDefaultToolkit(display);
        try {
            return X11Lib.XF86VidModeGetGammaRampSize(display, screen, size, size_offset);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static boolean XF86VidModeSetGammaRamp(long display, int screen, int size, ShortBuffer red_array, ShortBuffer green_array, ShortBuffer blue_array)  {
        lockDefaultToolkit(display);
        try {
            return X11Lib.XF86VidModeSetGammaRamp(display, screen, size, red_array, green_array, blue_array);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static boolean XF86VidModeSetGammaRamp(long display, int screen, int size, short[] red_array, int red_array_offset, short[] green_array, int green_array_offset, short[] blue_array, int blue_array_offset)  {
        lockDefaultToolkit(display);
        try {
            return X11Lib.XF86VidModeSetGammaRamp(display, screen, size, red_array, red_array_offset, green_array, green_array_offset, blue_array, blue_array_offset);
        } finally {
            unlockDefaultToolkit(display);
        }
    }

    public static void XLockDisplay(long handle) {
        if(ToolkitLock.TRACE_LOCK) {
            System.out.println("+++ X11 Display Lock get 0x"+Long.toHexString(handle));
        }
        X11Lib.XLockDisplay(handle);
    }

    public static void XUnlockDisplay(long handle) {
        if(ToolkitLock.TRACE_LOCK) {
            System.out.println("--- X11 Display Lock rel 0x"+Long.toHexString(handle));
        }
        X11Lib.XUnlockDisplay(handle);
    }

    private static native void initialize(boolean firstUIActionOnProcess);
}

/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package jogamp.nativewindow.x11;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.NativeWindowFactory;

import jogamp.nativewindow.Debug;
import jogamp.nativewindow.NWJNILibLoader;

import com.jogamp.common.util.LongObjectHashMap;

/**
 * Contains a thread safe X11 utility to retrieve display connections.
 */
public class X11Util {
    /** 
     * See Bug 515 - https://jogamp.org/bugzilla/show_bug.cgi?id=515
     * <p> 
     * It is observed that ATI X11 drivers, eg.
     * <ul> 
     *   <li>fglrx 8.78.6,</li>
     *   <li>fglrx 11.08/8.881 and </li>
     *   <li>fglrx 11.11/8.911</li>
     * </ul>  
     * are quite sensitive to multiple Display connections.
     * </p>
     * <p>
     * With the above drivers closing displays shall happen in the same order as
     * they were opened, <b>or</b> shall not be closed at all!
     * If closed, some driver related bug appears and brings down the JVM.
     * </p>
     * <p>
     * You may test this, ie just reverse the destroy order below.
     * See also native test: jogl/test/native/displayMultiple02.c
     * </p>
     * <p>
     * Workaround is to not close them at all if driver vendor is ATI.
     * </p>
     */
    public static final boolean ATI_HAS_XCLOSEDISPLAY_BUG = true;

    /** Value is <code>true</code>, best 'stable' results if always using XInitThreads(). */
    public static final boolean XINITTHREADS_ALWAYS_ENABLED = true;
    
    /** Value is <code>true</code>, best 'stable' results if not using XLockDisplay/XUnlockDisplay at all. */
    public static final boolean HAS_XLOCKDISPLAY_BUG = true;
    
    private static final boolean DEBUG = Debug.debug("X11Util");
    private static final boolean TRACE_DISPLAY_LIFECYCLE = Debug.getBooleanProperty("nativewindow.debug.X11Util.TraceDisplayLifecycle", true, AccessController.getContext());

    private static String nullDisplayName = null;
    private static boolean isX11LockAvailable = false;
    private static boolean requiresX11Lock = true;
    private static volatile boolean isInit = false;
    private static boolean markAllDisplaysUnclosable = false; // ATI/AMD X11 driver issues

    private static int setX11ErrorHandlerRecCount = 0;
    private static Object setX11ErrorHandlerLock = new Object();

    
    @SuppressWarnings("unused")
    public static void initSingleton(final boolean firstX11ActionOnProcess) {
        if(!isInit) {
            synchronized(X11Util.class) {
                if(!isInit) {
                    isInit = true;
                    NWJNILibLoader.loadNativeWindow("x11");
        
                    final boolean callXInitThreads = XINITTHREADS_ALWAYS_ENABLED || firstX11ActionOnProcess;
                    final boolean isXInitThreadsOK = initialize0( XINITTHREADS_ALWAYS_ENABLED || firstX11ActionOnProcess );
                    isX11LockAvailable = isXInitThreadsOK && !HAS_XLOCKDISPLAY_BUG ;
        
                    final long dpy = X11Lib.XOpenDisplay(null);
                    try {
                        nullDisplayName = X11Lib.XDisplayString(dpy);
                    } finally {
                        X11Lib.XCloseDisplay(dpy);
                    }
                    
                    if(DEBUG) {
                        System.err.println("X11Util firstX11ActionOnProcess: "+firstX11ActionOnProcess+
                                           ", requiresX11Lock "+requiresX11Lock+
                                           ", XInitThreads [called "+callXInitThreads+", OK "+isXInitThreadsOK+"]"+
                                           ", isX11LockAvailable "+isX11LockAvailable+
                                           ", X11 Display(NULL) <"+nullDisplayName+">");
                        // Thread.dumpStack();
                    }
                }
            }
        }
    }
    
    public static synchronized boolean isNativeLockAvailable() {
        return isX11LockAvailable;
    }

    public static synchronized boolean requiresToolkitLock() {
        return requiresX11Lock;
    }

    public static void setX11ErrorHandler(boolean onoff, boolean quiet) {
        synchronized(setX11ErrorHandlerLock) {
            if(onoff) {
                if(0==setX11ErrorHandlerRecCount) {
                    setX11ErrorHandler0(true, quiet);
                }
                setX11ErrorHandlerRecCount++;
            } else {
                if(0 >= setX11ErrorHandlerRecCount) {
                    throw new InternalError();
                }
                setX11ErrorHandlerRecCount--;
                if(0==setX11ErrorHandlerRecCount) {
                    setX11ErrorHandler0(false, false);
                }
            }
        }
    }

    public static String getNullDisplayName() {
        return nullDisplayName;
    }
    
    public static boolean getMarkAllDisplaysUnclosable() {
        return markAllDisplaysUnclosable;
    }
    public static void setMarkAllDisplaysUnclosable(boolean v) {
        markAllDisplaysUnclosable = v;
    }
    
    private X11Util() {}

    // not exactly thread safe, but good enough for our purpose,
    // which is to tag a NamedDisplay uncloseable after creation.
    private static Object globalLock = new Object(); 
    private static LongObjectHashMap openDisplayMap = new LongObjectHashMap(); // handle -> name
    private static List<NamedDisplay> openDisplayList = new ArrayList<NamedDisplay>();
    private static List<NamedDisplay> pendingDisplayList = new ArrayList<NamedDisplay>();

    public static class NamedDisplay {
        final String name;
        final long   handle;
        final int    hash32;
        int    refCount;
        boolean unCloseable;
        Throwable creationStack;

        protected NamedDisplay(String name, long handle) {
            this.name=name;
            this.handle=handle;
            this.refCount=0;
            this.unCloseable=false;
            {
                int h32;
                h32 = 31 +                 (int)   handle          ; // lo
                h32 = ((h32 << 5) - h32) + (int) ( handle >>> 32 ) ; // hi
                hash32 = h32;
            }
            if(DEBUG) {
                this.creationStack=new Throwable("NamedDisplay Created at:");
            } else {
                this.creationStack=null;
            }
        }

        public final int hashCode() {
            return hash32;
        }
        
        public final boolean equals(Object obj) {
            if(this == obj) { return true; }
            if(obj instanceof NamedDisplay) {
                NamedDisplay n = (NamedDisplay) obj;
                return handle == n.handle;
            }
            return false;
        }
        
        public final void addRef() { refCount++; }
        public final void removeRef() { refCount--; }
        
        public final String getName() { return name; }
        public final long   getHandle() { return handle; }
        public final int    getRefCount() { return refCount; }

        public final void setUncloseable(boolean v) { unCloseable = v; }
        public final boolean isUncloseable() { return unCloseable; }
        public final Throwable getCreationStack() { return creationStack; }

        @Override
        public Object clone() throws CloneNotSupportedException {
          return super.clone();
        }

        @Override
        public String toString() {
            return "NamedX11Display["+name+", 0x"+Long.toHexString(handle)+", refCount "+refCount+", unCloseable "+unCloseable+"]";
        }
    }

    /** 
     * Cleanup resources. 
     * If <code>realXCloseOpenAndPendingDisplays</code> is <code>false</code>, 
     * keep alive all references (open display connection) for restart on same ClassLoader.
     * 
     * @return number of unclosed X11 Displays.<br>
     * @param realXCloseOpenAndPendingDisplays if true, {@link #closePendingDisplayConnections()} is called.
     */
    public static int shutdown(boolean realXCloseOpenAndPendingDisplays, boolean verbose) {
        int num=0;
        if(DEBUG||verbose||pendingDisplayList.size() > 0) {
            System.err.println("X11Util.Display: Shutdown (close open / pending Displays: "+realXCloseOpenAndPendingDisplays+
                               ", open (no close attempt): "+openDisplayMap.size()+"/"+openDisplayList.size()+
                               ", pending (not closed, marked uncloseable): "+pendingDisplayList.size()+")");
            if(DEBUG) {
                Thread.dumpStack();
            }
            if( openDisplayList.size() > 0) {
                X11Util.dumpOpenDisplayConnections();
            }
            if( pendingDisplayList.size() > 0 ) {
                X11Util.dumpPendingDisplayConnections();
            }
        }

        synchronized(globalLock) {
            if(realXCloseOpenAndPendingDisplays) {
                closePendingDisplayConnections();    
                openDisplayList.clear();
                pendingDisplayList.clear();
                openDisplayMap.clear();
                shutdown0();
            }
        }
        return num;
    }

    /**
     * Closing pending Display connections in reverse order.
     *
     * @return number of closed Display connections
     */
    public static int closePendingDisplayConnections() {
        int num=0;
        synchronized(globalLock) {
            if(DEBUG) {
                System.err.println("X11Util: Closing Pending X11 Display Connections: "+pendingDisplayList.size());
            }
            for(int i=pendingDisplayList.size()-1; i>=0; i--) {
                NamedDisplay ndpy = (NamedDisplay) pendingDisplayList.get(i);
                if(DEBUG) {
                    System.err.println("X11Util.closePendingDisplayConnections(): Closing ["+i+"]: "+ndpy);
                }
                XCloseDisplay(ndpy.getHandle());
                num++;
            }
        }
        return num;
    }

    public static int getOpenDisplayConnectionNumber() {
        synchronized(globalLock) {
            return openDisplayList.size();
        }
    }
        
    public static void dumpOpenDisplayConnections() {
        synchronized(globalLock) {
            System.err.println("X11Util: Open X11 Display Connections: "+openDisplayList.size());
            for(int i=0; i<openDisplayList.size(); i++) {
                NamedDisplay ndpy = openDisplayList.get(i);
                System.err.println("X11Util: Open["+i+"]: "+ndpy);
                if(null!=ndpy) {
                    Throwable t = ndpy.getCreationStack();
                    if(null!=t) {
                        t.printStackTrace();
                    }
                }
            }
        }
    }
    
    public static int getPendingDisplayConnectionNumber() {
        synchronized(globalLock) {
            return pendingDisplayList.size();
        }
    }

    public static void dumpPendingDisplayConnections() {
        synchronized(globalLock) {
            System.err.println("X11Util: Pending X11 Display Connections: "+pendingDisplayList.size());
            for(int i=0; i<pendingDisplayList.size(); i++) {
                NamedDisplay ndpy = (NamedDisplay) pendingDisplayList.get(i);
                System.err.println("X11Util: Pending["+i+"]: "+ndpy);
                if(null!=ndpy) {
                    Throwable t = ndpy.getCreationStack();
                    if(null!=t) {
                        t.printStackTrace();
                    }
                }
            }
        }
    }

    public static boolean markDisplayUncloseable(long handle) {
        NamedDisplay ndpy;
        synchronized(globalLock) {
            ndpy = (NamedDisplay) openDisplayMap.get(handle);
        }
        if( null != ndpy ) {
            ndpy.setUncloseable(true);
            return true;
        }
        return false;
    }

    /** Returns a created or reused named display. */
    public static long openDisplay(String name) {
        long dpy = 0;
        NamedDisplay namedDpy = null;
        name = validateDisplayName(name);
        boolean reused = false;
        
        synchronized(globalLock) {            
            for(int i=0; i<pendingDisplayList.size(); i++) {
                if(pendingDisplayList.get(i).getName().equals(name)) {
                    namedDpy = pendingDisplayList.remove(i);
                    dpy = namedDpy.getHandle();
                    reused = true;
                    break;
                }
            }
            if(0 == dpy) {
                dpy = XOpenDisplay(name);
                if(0==dpy) {
                    throw new NativeWindowException("X11Util.Display: Unable to create a display("+name+") connection. Thread "+Thread.currentThread().getName());
                }
                // if you like to debug and synchronize X11 commands ..
                // setSynchronizeDisplay(dpy, true);
                namedDpy = new NamedDisplay(name, dpy);
            }
            namedDpy.addRef();
            openDisplayMap.put(dpy, namedDpy);
            openDisplayList.add(namedDpy);
            if(markAllDisplaysUnclosable) {
                namedDpy.setUncloseable(true);
            }
        }
        if(DEBUG) {
            System.err.println("X11Util.Display: openDisplay [reuse "+reused+"] "+namedDpy+". Thread "+Thread.currentThread().getName());
            // Thread.dumpStack();
        }
        return namedDpy.getHandle();
    }

    public static void closeDisplay(long handle) {
        NamedDisplay namedDpy;

        synchronized(globalLock) {
            namedDpy = (NamedDisplay) openDisplayMap.remove(handle);
            if(null==namedDpy) {
                X11Util.dumpPendingDisplayConnections();
                throw new RuntimeException("X11Util.Display: Display(0x"+Long.toHexString(handle)+") with given handle is not mapped. Thread "+Thread.currentThread().getName());
            }
            if(namedDpy.getHandle()!=handle) {
                X11Util.dumpPendingDisplayConnections();
                throw new RuntimeException("X11Util.Display: Display(0x"+Long.toHexString(handle)+") Mapping error: "+namedDpy+". Thread "+Thread.currentThread().getName());
            }
    
            namedDpy.removeRef();
            if(!openDisplayList.remove(namedDpy)) { throw new RuntimeException("Internal: "+namedDpy); }
            
            if(!namedDpy.isUncloseable()) {
                XCloseDisplay(namedDpy.getHandle());
            } else {
                // for reuse
                pendingDisplayList.add(namedDpy);
            }
            
            if(DEBUG) {
                System.err.println("X11Util.Display: Closed (real: "+(!namedDpy.isUncloseable())+") "+namedDpy+". Thread "+Thread.currentThread().getName());
            }    
        }
    }

    public static NamedDisplay getNamedDisplay(long handle) {
        synchronized(globalLock) {
            return (NamedDisplay) openDisplayMap.get(handle);
        }
    }

    /** 
     * @return If name is null, it returns the previous queried NULL display name,
     * otherwise the name. */
    public static String validateDisplayName(String name) {
        return ( null == name || AbstractGraphicsDevice.DEFAULT_CONNECTION.equals(name) ) ? getNullDisplayName() : name ;
    }

    public static String validateDisplayName(String name, long handle) {
        if( ( null==name || AbstractGraphicsDevice.DEFAULT_CONNECTION.equals(name) ) && 0!=handle) {
            name = X11Lib.XDisplayString(handle);
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
                System.err.println(Thread.currentThread()+" - X11Util.XOpenDisplay("+arg0+") 0x"+Long.toHexString(handle));
                // Thread.dumpStack();
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
                System.err.println(Thread.currentThread()+" - X11Util.XCloseDisplay() 0x"+Long.toHexString(display));
                // Thread.dumpStack();
            }
            int res = -1;            
            X11Util.setX11ErrorHandler(true, DEBUG ? false : true);
            try {
                res = X11Lib.XCloseDisplay(display);
            } catch (Exception ex) {
                System.err.println("X11Util: Catched Exception:");
                ex.printStackTrace();
            } finally {
                X11Util.setX11ErrorHandler(false, false);                    
            }
            return res;            
        } finally {
            NativeWindowFactory.getDefaultToolkitLock().unlock();
        }
    }

    private static native boolean initialize0(boolean firstUIActionOnProcess);
    private static native void shutdown0();
    private static native void setX11ErrorHandler0(boolean onoff, boolean quiet);
}

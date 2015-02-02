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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;

import jogamp.nativewindow.Debug;
import jogamp.nativewindow.NWJNILibLoader;
import jogamp.nativewindow.ToolkitProperties;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.util.LongObjectHashMap;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;

/**
 * Contains a thread safe X11 utility to retrieve display connections.
 */
public class X11Util implements ToolkitProperties {
    public static final boolean DEBUG = Debug.debug("X11Util");

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
     * See also native test: jogl/test-native/displayMultiple02.c
     * </p>
     * <p>
     * Workaround is to not close them at all if driver vendor is ATI
     * during operation.
     * </p>
     * <p>
     * With ATI X11 drivers all connections must be closed at JVM shutdown,
     * otherwise a SIGSEGV (after JVM safepoint) will be caused.
     * </p>
     */
    public static final boolean ATI_HAS_XCLOSEDISPLAY_BUG = !Debug.isPropertyDefined("nativewindow.debug.X11Util.ATI_HAS_NO_XCLOSEDISPLAY_BUG", true);

    /** See {@link #ATI_HAS_XCLOSEDISPLAY_BUG}. */
    public static final boolean HAS_XCLOSEDISPLAY_BUG = Debug.isPropertyDefined("nativewindow.debug.X11Util.HAS_XCLOSEDISPLAY_BUG", true);

    /**
     * See Bug 623 - https://jogamp.org/bugzilla/show_bug.cgi?id=623
     */
    public static final boolean ATI_HAS_MULTITHREADING_BUG = !Debug.isPropertyDefined("nativewindow.debug.X11Util.ATI_HAS_NO_MULTITHREADING_BUG", true);

    public static final boolean XSYNC_ENABLED = Debug.isPropertyDefined("nativewindow.debug.X11Util.XSync", true);
    public static final boolean XERROR_STACKDUMP = DEBUG || Debug.isPropertyDefined("nativewindow.debug.X11Util.XErrorStackDump", true);
    private static final boolean TRACE_DISPLAY_LIFECYCLE = Debug.isPropertyDefined("nativewindow.debug.X11Util.TraceDisplayLifecycle", true);
    private static String nullDisplayName = null;
    private static volatile boolean isInit = false;
    private static boolean markAllDisplaysUnclosable = false; // ATI/AMD X11 driver issues, or GLRendererQuirks.DontCloseX11Display
    private static boolean hasThreadingIssues = false; // ATI/AMD X11 driver issues

    private static final Object setX11ErrorHandlerLock = new Object();
    private static final String X11_EXTENSION_ATIFGLRXDRI     = "ATIFGLRXDRI";
    private static final String X11_EXTENSION_ATIFGLEXTENSION = "ATIFGLEXTENSION";

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     * @see ToolkitProperties
     */
    public static void initSingleton() {
        if(!isInit) {
            synchronized(X11Util.class) {
                if(!isInit) {
                    isInit = true;
                    if(DEBUG) {
                        System.out.println("X11Util.initSingleton()");
                    }
                    if(!NWJNILibLoader.loadNativeWindow("x11")) {
                        throw new NativeWindowException("NativeWindow X11 native library load error.");
                    }

                    final boolean isInitOK = initialize0( XERROR_STACKDUMP );

                    final boolean hasX11_EXTENSION_ATIFGLRXDRI, hasX11_EXTENSION_ATIFGLEXTENSION;
                    final long dpy = X11Lib.XOpenDisplay(PropertyAccess.getProperty("nativewindow.x11.display.default", true));
                    if(0 != dpy) {
                        if(XSYNC_ENABLED) {
                            X11Lib.XSynchronize(dpy, true);
                        }
                        try {
                            nullDisplayName = X11Lib.XDisplayString(dpy);
                            hasX11_EXTENSION_ATIFGLRXDRI = X11Lib.QueryExtension(dpy, X11_EXTENSION_ATIFGLRXDRI);
                            hasX11_EXTENSION_ATIFGLEXTENSION = X11Lib.QueryExtension(dpy, X11_EXTENSION_ATIFGLEXTENSION);
                        } finally {
                            X11Lib.XCloseDisplay(dpy);
                        }
                    } else {
                        nullDisplayName = "nil";
                        hasX11_EXTENSION_ATIFGLRXDRI = false;
                        hasX11_EXTENSION_ATIFGLEXTENSION = false;
                    }
                    final boolean isATIFGLRX = hasX11_EXTENSION_ATIFGLRXDRI || hasX11_EXTENSION_ATIFGLEXTENSION ;
                    hasThreadingIssues = ATI_HAS_MULTITHREADING_BUG && isATIFGLRX;
                    if ( !markAllDisplaysUnclosable ) {
                        markAllDisplaysUnclosable = ( ATI_HAS_XCLOSEDISPLAY_BUG && isATIFGLRX ) || HAS_XCLOSEDISPLAY_BUG;
                    }

                    if(DEBUG) {
                        System.err.println("X11Util.initSingleton(): OK "+isInitOK+"]"+
                                           ",\n\t X11 Display(NULL) <"+nullDisplayName+">"+
                                           ",\n\t XSynchronize Enabled: " + XSYNC_ENABLED+
                                           ",\n\t X11_EXTENSION_ATIFGLRXDRI " + hasX11_EXTENSION_ATIFGLRXDRI+
                                           ",\n\t X11_EXTENSION_ATIFGLEXTENSION " + hasX11_EXTENSION_ATIFGLEXTENSION+
                                           ",\n\t requiresToolkitLock "+requiresToolkitLock()+
                                           ",\n\t hasThreadingIssues "+hasThreadingIssues()+
                                           ",\n\t markAllDisplaysUnclosable "+getMarkAllDisplaysUnclosable()
                                           );
                        // Thread.dumpStack();
                    }
                }
            }
        }
    }

    // not exactly thread safe, but good enough for our purpose,
    // which is to tag a NamedDisplay uncloseable after creation.
    private static Object globalLock = new Object();
    private static LongObjectHashMap openDisplayMap = new LongObjectHashMap(); // handle -> name
    private static List<NamedDisplay> openDisplayList = new ArrayList<NamedDisplay>();     // open, no close attempt
    private static List<NamedDisplay> reusableDisplayList = new ArrayList<NamedDisplay>(); // close attempt, marked uncloseable, for reuse
    private static List<NamedDisplay> pendingDisplayList = new ArrayList<NamedDisplay>();  // all open (close attempt or reusable) in creation order
    private static final HashMap<String /* displayName */, Boolean> displayXineramaEnabledMap = new HashMap<String, Boolean>();

    /**
     * Cleanup resources.
     * <p>
     * Called by {@link NativeWindowFactory#shutdown()}
     * </p>
     * @see ToolkitProperties
     */
    public static void shutdown() {
        if(isInit) {
            synchronized(X11Util.class) {
                if(isInit) {
                    final boolean isJVMShuttingDown = NativeWindowFactory.isJVMShuttingDown() ;
                    if( DEBUG ||
                        ( ( openDisplayMap.size() > 0 || reusableDisplayList.size() > 0 || pendingDisplayList.size() > 0 ) &&
                          ( reusableDisplayList.size() != pendingDisplayList.size() || !markAllDisplaysUnclosable )
                        ) ) {
                        System.err.println("X11Util.Display: Shutdown (JVM shutdown: "+isJVMShuttingDown+
                                           ", open (no close attempt): "+openDisplayMap.size()+"/"+openDisplayList.size()+
                                           ", reusable (open, marked uncloseable): "+reusableDisplayList.size()+
                                           ", pending (open in creation order): "+pendingDisplayList.size()+
                                           ")");
                        if(DEBUG) {
                            ExceptionUtils.dumpStack(System.err);
                        }
                        if( openDisplayList.size() > 0) {
                            X11Util.dumpOpenDisplayConnections();
                        }
                        if(DEBUG) {
                            if( reusableDisplayList.size() > 0 || pendingDisplayList.size() > 0 ) {
                                X11Util.dumpPendingDisplayConnections();
                            }
                        }
                    }

                    // Only at JVM shutdown time, since AWT impl. seems to
                    // dislike closing of X11 Display's (w/ ATI driver).
                    if( isJVMShuttingDown ) {
                        synchronized(globalLock) {
                            isInit = false;
                            closePendingDisplayConnections();
                            openDisplayList.clear();
                            reusableDisplayList.clear();
                            pendingDisplayList.clear();
                            openDisplayMap.clear();
                            displayXineramaEnabledMap.clear();
                            shutdown0();
                        }
                    }
                }
            }
        }
    }

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     * @see ToolkitProperties
     */
    public static final boolean requiresToolkitLock() {
        return true; // JAWT locking: yes, instead of native X11 locking w use a recursive lock per display connection.
    }

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     * @see ToolkitProperties
     */
    public static final boolean hasThreadingIssues() {
        return hasThreadingIssues; // JOGL impl. may utilize special locking "somewhere"
    }

    public static void setX11ErrorHandler(final boolean onoff, final boolean quiet) {
        synchronized(setX11ErrorHandlerLock) {
            setX11ErrorHandler0(onoff, quiet);
        }
    }

    public static String getNullDisplayName() {
        return nullDisplayName;
    }

    public static void markAllDisplaysUnclosable() {
        synchronized(globalLock) {
            markAllDisplaysUnclosable = true;
            for(int i=0; i<openDisplayList.size(); i++) {
                openDisplayList.get(i).setUncloseable(true);
            }
            for(int i=0; i<reusableDisplayList.size(); i++) {
                reusableDisplayList.get(i).setUncloseable(true);
            }
            for(int i=0; i<pendingDisplayList.size(); i++) {
                pendingDisplayList.get(i).setUncloseable(true);
            }
        }
    }

    public static boolean getMarkAllDisplaysUnclosable() {
        return markAllDisplaysUnclosable;
    }

    private X11Util() {}

    public static class NamedDisplay {
        final String name;
        final long   handle;
        final int    hash32;
        int    refCount;
        boolean unCloseable;
        Throwable creationStack;

        protected NamedDisplay(final String name, final long handle) {
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

        @Override
        public final int hashCode() {
            return hash32;
        }

        @Override
        public final boolean equals(final Object obj) {
            if(this == obj) { return true; }
            if(obj instanceof NamedDisplay) {
                return handle == ((NamedDisplay) obj).handle;
            }
            return false;
        }

        public final void addRef() { refCount++; }
        public final void removeRef() { refCount--; }

        public final String getName() { return name; }
        public final long   getHandle() { return handle; }
        public final int    getRefCount() { return refCount; }

        public final void setUncloseable(final boolean v) { unCloseable = v; }
        public final boolean isUncloseable() { return unCloseable; }
        public final Throwable getCreationStack() { return creationStack; }

        @Override
        public String toString() {
            return "NamedX11Display["+name+", 0x"+Long.toHexString(handle)+", refCount "+refCount+", unCloseable "+unCloseable+"]";
        }
    }

    /**
     * Closing pending Display connections in original creation order, if {@link #getMarkAllDisplaysUnclosable()} is true.
     *
     * @return number of closed Display connections
     */
    private static int closePendingDisplayConnections() {
        int num=0;
        synchronized(globalLock) {
            if( getMarkAllDisplaysUnclosable() ) {
                for(int i=0; i<pendingDisplayList.size(); i++) {
                    final NamedDisplay ndpy = pendingDisplayList.get(i);
                    if(DEBUG) {
                        final boolean closeAttempted = !openDisplayMap.containsKey(ndpy.getHandle());
                        System.err.println("X11Util.closePendingDisplayConnections(): Closing ["+i+"]: "+ndpy+" - closeAttempted "+closeAttempted);
                    }
                    XCloseDisplay(ndpy.getHandle());
                    num++;
                }
                if(DEBUG) {
                    System.err.println("X11Util.closePendingDisplayConnections(): Closed "+num+" pending display connections");
                }
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
                final NamedDisplay ndpy = openDisplayList.get(i);
                System.err.println("X11Util: Open["+i+"]: "+ndpy);
                if(null!=ndpy) {
                    final Throwable t = ndpy.getCreationStack();
                    if(null!=t) {
                        t.printStackTrace();
                    }
                }
            }
        }
    }

    public static int getReusableDisplayConnectionNumber() {
        synchronized(globalLock) {
            return reusableDisplayList.size();
        }
    }

    public static int getPendingDisplayConnectionNumber() {
        synchronized(globalLock) {
            return pendingDisplayList.size();
        }
    }

    public static void dumpPendingDisplayConnections() {
        synchronized(globalLock) {
            System.err.println("X11Util: Reusable X11 Display Connections: "+reusableDisplayList.size());
            for(int i=0; i<reusableDisplayList.size(); i++) {
                final NamedDisplay ndpy = reusableDisplayList.get(i);
                System.err.println("X11Util: Reusable["+i+"]: "+ndpy);
                if(null!=ndpy) {
                    final Throwable t = ndpy.getCreationStack();
                    if(null!=t) {
                        t.printStackTrace();
                    }
                }
            }
            System.err.println("X11Util: Pending X11 Display Connections (creation order): "+pendingDisplayList.size());
            for(int i=0; i<pendingDisplayList.size(); i++) {
                final NamedDisplay ndpy = pendingDisplayList.get(i);
                System.err.println("X11Util: Pending["+i+"]: "+ndpy);
                if(null!=ndpy) {
                    final Throwable t = ndpy.getCreationStack();
                    if(null!=t) {
                        t.printStackTrace();
                    }
                }
            }
        }
    }

    public static boolean markDisplayUncloseable(final long handle) {
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
            for(int i=0; i<reusableDisplayList.size(); i++) {
                if(reusableDisplayList.get(i).getName().equals(name)) {
                    namedDpy = reusableDisplayList.remove(i);
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
                pendingDisplayList.add(namedDpy);
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

    public static void closeDisplay(final long handle) {
        synchronized(globalLock) {
            final NamedDisplay namedDpy = (NamedDisplay) openDisplayMap.remove(handle);
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

            if( markAllDisplaysUnclosable ) {
                 // if set-mark 'slipped' this one .. just to be safe!
                namedDpy.setUncloseable(true);
            }
            if( !namedDpy.isUncloseable() ) {
                XCloseDisplay(namedDpy.getHandle());
                pendingDisplayList.remove(namedDpy);
            } else {
                // for reuse
                X11Lib.XSync(namedDpy.getHandle(), true); // flush output buffer and discard all events
                reusableDisplayList.add(namedDpy);
            }

            if(DEBUG) {
                System.err.println("X11Util.Display: Closed (real: "+(!namedDpy.isUncloseable())+") "+namedDpy+". Thread "+Thread.currentThread().getName());
            }
        }
    }

    public static NamedDisplay getNamedDisplay(final long handle) {
        synchronized(globalLock) {
            return (NamedDisplay) openDisplayMap.get(handle);
        }
    }

    /**
     * @return If name is null, it returns the previous queried NULL display name,
     * otherwise the name. */
    public static String validateDisplayName(final String name) {
        return ( null == name || AbstractGraphicsDevice.DEFAULT_CONNECTION.equals(name) ) ? getNullDisplayName() : name ;
    }

    public static String validateDisplayName(String name, final long handle) {
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

    public static long XOpenDisplay(final String arg0) {
        final long handle = X11Lib.XOpenDisplay(arg0);
        if(XSYNC_ENABLED && 0 != handle) {
            X11Lib.XSynchronize(handle, true);
        }
        if(TRACE_DISPLAY_LIFECYCLE) {
            System.err.println(Thread.currentThread()+" - X11Util.XOpenDisplay("+arg0+") 0x"+Long.toHexString(handle));
            // Thread.dumpStack();
        }
        return handle;
    }

    public static int XCloseDisplay(final long display) {
        if(TRACE_DISPLAY_LIFECYCLE) {
            System.err.println(Thread.currentThread()+" - X11Util.XCloseDisplay() 0x"+Long.toHexString(display));
            // Thread.dumpStack();
        }
        int res = -1;
        try {
            res = X11Lib.XCloseDisplay(display);
        } catch (final Exception ex) {
            System.err.println("X11Util: Caught exception:");
            ex.printStackTrace();
        }
        return res;
    }

    static volatile boolean XineramaFetched = false;
    static long XineramaLibHandle = 0;
    static long XineramaQueryFunc = 0;

    public static boolean XineramaIsEnabled(final X11GraphicsDevice device) {
        if(null == device) {
            throw new IllegalArgumentException("X11 Display device is NULL");
        }
        device.lock();
        try {
            return XineramaIsEnabled(device.getHandle());
        } finally {
            device.unlock();
        }
    }

    public static boolean XineramaIsEnabled(final long displayHandle) {
        if( 0 == displayHandle ) {
            throw new IllegalArgumentException("X11 Display handle is NULL");
        }
        final String displayName = X11Lib.XDisplayString(displayHandle);
        synchronized(displayXineramaEnabledMap) {
            final Boolean b = displayXineramaEnabledMap.get(displayName);
            if(null != b) {
                return b.booleanValue();
            }
        }
        final boolean res;
        if(!XineramaFetched) { // volatile: ok
            synchronized(X11Util.class) {
                if( !XineramaFetched ) {
                    XineramaLibHandle = X11Lib.XineramaGetLibHandle();
                    if(0 != XineramaLibHandle) {
                        XineramaQueryFunc = X11Lib.XineramaGetQueryFunc(XineramaLibHandle);
                    }
                    XineramaFetched = true;
                }
            }
        }
        if(0!=XineramaQueryFunc) {
            res = X11Lib.XineramaIsEnabled(XineramaQueryFunc, displayHandle);
        } else {
            if(DEBUG) {
                System.err.println("XineramaIsEnabled: Couldn't bind to Xinerama - lib 0x"+Long.toHexString(XineramaLibHandle)+
                                   "query 0x"+Long.toHexString(XineramaQueryFunc));
            }
            res = false;
        }
        synchronized(displayXineramaEnabledMap) {
            if(DEBUG) {
                System.err.println("XineramaIsEnabled Cache: Display "+displayName+" (0x"+Long.toHexString(displayHandle)+") -> "+res);
            }
            displayXineramaEnabledMap.put(displayName, Boolean.valueOf(res));
        }
        return res;
    }

    private static final String getCurrentThreadName() { return Thread.currentThread().getName(); } // Callback for JNI
    private static final void dumpStack() { ExceptionUtils.dumpStack(System.err); } // Callback for JNI

    private static native boolean initialize0(boolean debug);
    private static native void shutdown0();
    private static native void setX11ErrorHandler0(boolean onoff, boolean quiet);
}

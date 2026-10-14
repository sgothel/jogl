/**
 * Copyright 2011 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package jogamp.nativewindow.macosx;

import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.util.Insets;
import com.jogamp.nativewindow.util.Point;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.util.Function;
import com.jogamp.common.util.FunctionTask;
import com.jogamp.common.util.InterruptedRuntimeException;
import com.jogamp.common.util.RunnableTask;

import jogamp.nativewindow.Debug;
import jogamp.nativewindow.NWJNILibLoader;
import jogamp.nativewindow.ToolkitProperties;

public class OSXUtil implements ToolkitProperties {
    private static boolean isInit = false;
    private static final boolean DEBUG = Debug.debug("OSXUtil");
    private static final boolean DEBUG_RUNONMAIN = Debug.debug("OSXUtil.RunOnMainThread");
    private static final boolean DEBUG_RUNONMAIN_STACK = Debug.debug("OSXUtil.RunOnMainThread.Stack");
    private static final boolean DEBUG_AWTEDTUTIL = Debug.debug("OSXUtil.AWTEDTUtil");

    private static final ThreadLocal<Boolean> tlsIsMainThread = new ThreadLocal<Boolean>();

    /**
     * Optional AWT/EDT helpers used to avoid macOS AppKit &amp; AWT-EDT deadlocks when synchronously
     * executing tasks on the AppKit main thread.
     * <p>
     * {@code nativewindow} can be used without AWT (and even without the {@code java.desktop} module)
     * in some configurations, hence these AWT classes are resolved via reflection and are additionally
     * guarded by {@link NativeWindowFactory#isAWTAvailable()}.
     * </p>
     * <p>
     * Rationale (Bug 1478): On macOS 13/14, calling {@link #RunOnMainThread(boolean, boolean, Runnable)}
     * with {@code waitUntilDone=true} from the AWT Event Dispatch Thread (EDT) can deadlock if the
     * AppKit-side work needs the EDT to keep pumping events (e.g. during peer/window realization).
     * Using {@code java.awt.SecondaryLoop} keeps the EDT dispatching while still providing
     * synchronous semantics.
     * </p>
     */
    private static final class AWTEDTUtil {
        private static final boolean available;
        private static final Method eventQueueIsDispatchThread;
        private static final Method toolkitGetDefaultToolkit;
        private static final Method toolkitGetSystemEventQueue;
        private static final Method eventQueueCreateSecondaryLoop;
        private static final Method secondaryLoopEnter;
        private static final Method secondaryLoopExit;

        static {
            boolean ok = false;
            Method mIsDispatchThread = null;
            Method mGetDefaultToolkit = null;
            Method mGetSystemEventQueue = null;
            Method mCreateSecondaryLoop = null;
            Method mEnter = null;
            Method mExit = null;

            try {
                final ClassLoader cl = OSXUtil.class.getClassLoader();
                final Class<?> eventQueueClass = Class.forName("java.awt.EventQueue", false, cl);
                final Class<?> toolkitClass = Class.forName("java.awt.Toolkit", false, cl);
                final Class<?> secondaryLoopClass = Class.forName("java.awt.SecondaryLoop", false, cl);

                mIsDispatchThread = eventQueueClass.getMethod("isDispatchThread");
                mGetDefaultToolkit = toolkitClass.getMethod("getDefaultToolkit");
                mGetSystemEventQueue = toolkitClass.getMethod("getSystemEventQueue");
                mCreateSecondaryLoop = eventQueueClass.getMethod("createSecondaryLoop");
                mEnter = secondaryLoopClass.getMethod("enter");
                mExit = secondaryLoopClass.getMethod("exit");

                ok = true;
            } catch (final Throwable t) {
                ok = false;
            }

            available = ok;
            eventQueueIsDispatchThread = mIsDispatchThread;
            toolkitGetDefaultToolkit = mGetDefaultToolkit;
            toolkitGetSystemEventQueue = mGetSystemEventQueue;
            eventQueueCreateSecondaryLoop = mCreateSecondaryLoop;
            secondaryLoopEnter = mEnter;
            secondaryLoopExit = mExit;

            if( DEBUG_AWTEDTUTIL ) {
                System.err.println("OSXUtil.AWTEDTUtil: available "+available+
                        ", awtAvailable "+NativeWindowFactory.isAWTAvailable()+
                        ", EventQueue.isDispatchThread "+(null != eventQueueIsDispatchThread)+
                        ", EventQueue.createSecondaryLoop "+(null != eventQueueCreateSecondaryLoop)+
                        ", SecondaryLoop.enter/exit "+(null != secondaryLoopEnter)+"/"+(null != secondaryLoopExit));
            }
        }

        /**
         * @return {@code true} if the current thread is the AWT EDT, otherwise {@code false}.
         */
        private static boolean isDispatchThread() {
            if( !available || !NativeWindowFactory.isAWTAvailable() ) {
                return false;
            }
            try {
                return ((Boolean) eventQueueIsDispatchThread.invoke(null)).booleanValue();
            } catch (final Throwable t) {
                return false;
            }
        }

        /**
         * Creates an AWT {@code SecondaryLoop} for the current {@code EventQueue}.
         * <p>
         * When entered on the EDT, the secondary loop keeps dispatching AWT events, allowing re-entrant
         * operations that would otherwise deadlock if the EDT was blocked in {@code Object.wait()}.
         * </p>
         *
         * @return the secondary loop instance, or {@code null} if unavailable.
         */
        private static Object createSecondaryLoop() {
            if( !available || !NativeWindowFactory.isAWTAvailable() ) {
                return null;
            }
            try {
                final Object toolkit = toolkitGetDefaultToolkit.invoke(null);
                final Object eventQueue = toolkitGetSystemEventQueue.invoke(toolkit);
                return eventQueueCreateSecondaryLoop.invoke(eventQueue);
            } catch (final Throwable t) {
                return null;
            }
        }

        /**
         * Enters the given AWT {@code SecondaryLoop}.
         *
         * @param loop secondary loop instance, may be {@code null}.
         * @return {@code true} if entered, otherwise {@code false}.
         */
        private static boolean enterSecondaryLoop(final Object loop) {
            if( null == loop ) {
                return false;
            }
            try {
                return ((Boolean) secondaryLoopEnter.invoke(loop)).booleanValue();
            } catch (final Throwable t) {
                return false;
            }
        }

        /**
         * Exits the given AWT {@code SecondaryLoop}.
         * <p>
         * Safe to call even if the loop is already exiting.
         * </p>
         *
         * @param loop secondary loop instance, may be {@code null}.
         */
        private static void exitSecondaryLoop(final Object loop) {
            if( null == loop ) {
                return;
            }
            try {
                secondaryLoopExit.invoke(loop);
            } catch (final Throwable t) { /* ignore */ }
        }
    }

    /** FIXME HiDPI: OSX unique and maximum value {@value} */
    public static final int MAX_PIXELSCALE = 2;

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     * @see ToolkitProperties
     */
    public static synchronized void initSingleton() {
        if(!isInit) {
            final boolean useMainThreadChecker = Debug.debug("OSXUtil.MainThreadChecker");
            if(DEBUG || useMainThreadChecker) {
                System.out.println("OSXUtil.initSingleton() - useMainThreadChecker "+useMainThreadChecker);
            }
            if(!NWJNILibLoader.loadNativeWindow("macosx")) {
                throw new NativeWindowException("NativeWindow MacOSX native library load error.");
            }
            if( useMainThreadChecker ) {
                final String libMainThreadChecker = "/Applications/Xcode.app/Contents/Developer/usr/lib/libMainThreadChecker.dylib";
                final NativeLibrary lib = NativeLibrary.open(libMainThreadChecker, false, false, OSXUtil.class.getClassLoader(), true);
                if( null == lib ) {
                    System.err.println("Could not load "+libMainThreadChecker);
                } else {
                    System.err.println("Loaded "+lib);
                }
            }

            if( !initIDs0() ) {
                throw new NativeWindowException("MacOSX: Could not initialized native stub");
            }
            isInit = true;
        }
    }

    /**
     * Called by {@link NativeWindowFactory#shutdown()}
     * @see ToolkitProperties
     */
    public static void shutdown() { }

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     * @see ToolkitProperties
     */
    public static boolean requiresToolkitLock() { return false; }

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     * @see ToolkitProperties
     */
    public static final boolean hasThreadingIssues() { return false; }

    public static boolean isNSView(final long object) {
        return 0 != object ? isNSView0(object) : false;
    }

    public static boolean isNSWindow(final long object) {
        return 0 != object ? isNSWindow0(object) : false;
    }

    /**
     * @param windowOrView
     * @param src_x
     * @param src_y
     * @return top-left client-area position in window units
     */
    public static Point GetLocationOnScreen(final long windowOrView, final int src_x, final int src_y) {
        return (Point) GetLocationOnScreen0(windowOrView, src_x, src_y);
    }

    public static Insets GetInsets(final long windowOrView) {
        return (Insets) GetInsets0(windowOrView);
    }

    public static float GetScreenPixelScaleByDisplayID(final int displayID) {
        if( 0 != displayID ) {
            return GetScreenPixelScale1(displayID);
        } else {
            return 1.0f; // default
        }
    }
    public static float GetScreenPixelScale(final long windowOrView) {
        if( 0 != windowOrView ) {
            return GetScreenPixelScale2(windowOrView);
        } else {
            return 1.0f; // default
        }
    }
    public static float GetWindowPixelScale(final long windowOrView) {
        if( 0 != windowOrView ) {
            return GetWindowPixelScale1(windowOrView);
        } else {
            return 1.0f; // default
        }
    }
    public static void SetWindowPixelScale(final long windowOrView, final float reqPixelScale) {
        if( 0 != windowOrView ) {
            SetWindowPixelScale1(windowOrView, reqPixelScale);
        }
    }

    /** Creates an NSWindow on the main-thread */
    public static long CreateNSWindow(final int x, final int y, final int width, final int height) {
        return RunOnMainThreadLong(false /* kickNSApp */, () -> { return CreateNSWindow0(x, y, width, height); });
    }
    public static class WinAndView {
        public final long win;
        public final long view;
        public WinAndView(final long w, final long v) { win = w; view = v; }
    }
    /** Creates an NSWindow and retrieves its NSView on the main-thread */
    public static WinAndView CreateNSWindow2(final int x, final int y, final int width, final int height) {
        final AtomicLong nsWin0 = new AtomicLong(0);
        final AtomicLong nsView0 = new AtomicLong(0);

        if( DEBUG_RUNONMAIN ) {
            System.err.println("OSXUtil.CreateNSWindow2: enter x/y "+x+"/"+y+", size "+width+"x"+height+
                    ", onMain "+IsMainThread()+", onAWTEDT "+AWTEDTUtil.isDispatchThread()+
                    ", thread "+Thread.currentThread().getName());
            if( DEBUG_RUNONMAIN_STACK ) {
                ExceptionUtils.dumpStack(System.err);
            }
        }

        // Waking NSApp helps ensuring the runnable gets processed promptly on the AppKit main thread
        // (important when invoked while AWT is in the middle of realization/layout).
        OSXUtil.RunOnMainThread(true, true /* kickNSApp */, () -> {
            final long w = OSXUtil.CreateNSWindow0(x, y, width, height);
            if( 0 != w ) {
                nsWin0.set( w );
                nsView0.set( OSXUtil.GetNSView0(w) );
            }
            if( DEBUG_RUNONMAIN ) {
                System.err.println("OSXUtil.CreateNSWindow2: appkit created win 0x"+Long.toHexString(w)+
                        ", view 0x"+Long.toHexString(nsView0.get())+
                        ", thread "+Thread.currentThread().getName());
            }
        });
        if( DEBUG_RUNONMAIN ) {
            System.err.println("OSXUtil.CreateNSWindow2: exit win 0x"+Long.toHexString(nsWin0.get())+
                    ", view 0x"+Long.toHexString(nsView0.get())+
                    ", thread "+Thread.currentThread().getName());
        }
        return new WinAndView(nsWin0.get(), nsView0.get());
    }

    public static void DestroyNSWindow(final long nsWindow) {
        DestroyNSWindow0(nsWindow);
    }
    public static long GetNSView(final long nsWindow, final boolean onMainThread) {
        if( onMainThread ) {
            return RunOnMainThreadLong(false /* kickNSApp */, () -> { return GetNSView0(nsWindow); });
        } else {
            return GetNSView0(nsWindow);
        }
    }
    public static long GetNSWindow(final long nsView) {
        return GetNSWindow0(nsView);
    }

    /**
     * Create a CALayer suitable to act as a root CALayer.
     * @param width width of the CALayer in window units (points)
     * @param height height of the CALayer in window units (points)
     * @param contentsScale scale for HiDPI support: pixel-dim = window-dim x scale
     * @return the new CALayer object
     * @see #DestroyCALayer(long)
     * @see #AddCASublayer(long, long)
     */
    public static long CreateCALayer(final int width, final int height, final float contentsScale) {
        final long l = CreateCALayer0(width, height, contentsScale);
        if(DEBUG) {
            System.err.println("OSXUtil.CreateCALayer: 0x"+Long.toHexString(l)+" - "+Thread.currentThread().getName());
        }
        return l;
    }

    /**
     * Attach a sub CALayer to the root CALayer
     * <p>
     * Method will trigger a <code>display</code>
     * call to the CALayer hierarchy to enforce resource creation if required, e.g. an NSOpenGLContext.
     * </p>
     * <p>
     * Hence it is important that related resources are not locked <i>if</i>
     * they will be used for creation.
     * </p>
     * @param rootCALayer
     * @param subCALayer
     * @param x x-coord of the sub-CALayer in window units (points)
     * @param y y-coord of the sub-CALayer in window units (points)
     * @param width width of the sub-CALayer in window units (points)
     * @param height height of the sub-CALayer in window units (points)
     * @param contentsScale scale for HiDPI support: pixel-dim = window-dim x scale
     * @param caLayerQuirks
     * @see #CreateCALayer(int, int, float)
     * @see #RemoveCASublayer(long, long, boolean)
     */
    public static void AddCASublayer(final long rootCALayer, final long subCALayer,
                                     final int x, final int y, final int width, final int height,
                                     final float contentsScale, final int caLayerQuirks) {
        if(0==rootCALayer || 0==subCALayer) {
            throw new IllegalArgumentException("rootCALayer 0x"+Long.toHexString(rootCALayer)+", subCALayer 0x"+Long.toHexString(subCALayer));
        }
        if(DEBUG) {
            System.err.println("OSXUtil.AttachCALayer: caLayerQuirks "+caLayerQuirks+", 0x"+Long.toHexString(subCALayer)+" - "+Thread.currentThread().getName());
        }
        AddCASublayer0(rootCALayer, subCALayer, x, y, width, height, contentsScale, caLayerQuirks);
    }

    /**
     * Fix root and sub CALayer position to 0/0 and size
     * <p>
     * If the sub CALayer implements the Objective-C NativeWindow protocol NWDedicatedSize (e.g. JOGL's MyNSOpenGLLayer),
     * the dedicated size is passed to the layer, which propagates it appropriately.
     * </p>
     * <p>
     * On OSX/Java7 our root CALayer's frame position and size gets corrupted by its NSView,
     * hence we have created the NWDedicatedSize protocol.
     * </p>
     *
     * @param rootCALayer the root surface layer, maybe null.
     * @param subCALayer the client surface layer, maybe null.
     * @param visible TODO
     * @param width the expected width in window units (points)
     * @param height the expected height in window units (points)
     * @param caLayerQuirks TODO
     */
    public static void FixCALayerLayout(final long rootCALayer, final long subCALayer, final boolean visible, final int x, final int y, final int width, final int height, final int caLayerQuirks) {
        if( 0==rootCALayer && 0==subCALayer ) {
            return;
        }
        FixCALayerLayout0(rootCALayer, subCALayer, visible, x, y, width, height, caLayerQuirks);
    }

    /**
     * Set root and sub CALayer pixelScale / contentScale for HiDPI
     *
     * @param rootCALayer the root surface layer, maybe null.
     * @param subCALayer the client surface layer, maybe null.
     * @param contentsScale scale for HiDPI support: pixel-dim = window-dim x scale
     */
    public static void SetCALayerPixelScale(final long rootCALayer, final long subCALayer, final float contentsScale) {
        if( 0==rootCALayer && 0==subCALayer ) {
            return;
        }
        SetCALayerPixelScale0(rootCALayer, subCALayer, contentsScale);
    }

    /**
     * Detach a sub CALayer from the root CALayer.
     * @param subCALayerRelease if true, native call will issue a final {@code [subCALayerRelease release]}.
     */
    public static void RemoveCASublayer(final long rootCALayer, final long subCALayer, final boolean subCALayerRelease) {
        if(0==rootCALayer || 0==subCALayer) {
            throw new IllegalArgumentException("rootCALayer 0x"+Long.toHexString(rootCALayer)+", subCALayer 0x"+Long.toHexString(subCALayer));
        }
        if(DEBUG) {
            System.err.println("OSXUtil.DetachCALayer: 0x"+Long.toHexString(subCALayer)+" - "+Thread.currentThread().getName());
        }
        RemoveCASublayer0(rootCALayer, subCALayer, subCALayerRelease);
    }

    /**
     * Destroy a CALayer.
     * @see #CreateCALayer(int, int, float)
     */
    public static void DestroyCALayer(final long caLayer) {
        if(0==caLayer) {
            throw new IllegalArgumentException("caLayer 0x"+Long.toHexString(caLayer));
        }
        if(DEBUG) {
            System.err.println("OSXUtil.DestroyCALayer: 0x"+Long.toHexString(caLayer)+" - "+Thread.currentThread().getName());
        }
        DestroyCALayer0(caLayer);
    }

    /**
     * Run on OSX UI main thread.
     * <p>
     * 'waitUntilDone' is implemented on Java site via lock/wait on {@link RunnableTask} to not freeze OSX main thread.
     * </p>
     *
     * @param waitUntilDone
     * @param kickNSApp if <code>true</code> issues {@link #KickNSApp()}
     * @param runnable
     */
    public static void RunOnMainThread(final boolean waitUntilDone, final boolean kickNSApp, final Runnable runnable) {
        final long t0;
        if( DEBUG_RUNONMAIN ) {
            t0 = System.currentTimeMillis();
            System.err.println("OSXUtil.RunOnMainThread: enter wait "+waitUntilDone+", kickNSApp "+kickNSApp+
                    ", onMain "+IsMainThread()+", onAWTEDT "+AWTEDTUtil.isDispatchThread()+
                    ", thread "+Thread.currentThread().getName());
            if( DEBUG_RUNONMAIN_STACK ) {
                ExceptionUtils.dumpStack(System.err);
            }
        } else {
            t0 = 0;
        }
        if( IsMainThread() ) {
            runnable.run(); // don't leave the JVM
            if( DEBUG_RUNONMAIN ) {
                final long dt = System.currentTimeMillis() - t0;
                System.err.println("OSXUtil.RunOnMainThread: exit (onMain) dt "+dt+" ms, thread "+Thread.currentThread().getName());
            }
            return;
        }
        if( waitUntilDone && AWTEDTUtil.isDispatchThread() ) {
            // Bug 1478: Avoid blocking the AWT EDT in Object.wait() while synchronously waiting for the
            // AppKit main-thread runnable. The AppKit-side work (e.g. NSWindow/NSView creation used by the
            // CALayer/JAWT path) can require AWT events to be processed during window/peer realization.
            // Using an AWT SecondaryLoop keeps the EDT dispatching while we wait for completion.
            final Object secondaryLoop = AWTEDTUtil.createSecondaryLoop();
            if( null != secondaryLoop ) {
                final AtomicReference<Throwable> throwableRef = new AtomicReference<Throwable>();
                final Runnable runnable0 = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if( DEBUG_RUNONMAIN ) {
                                System.err.println("OSXUtil.RunOnMainThread: appkit-runner start, thread "+Thread.currentThread().getName());
                            }
                            runnable.run();
                        } catch (final Throwable t) {
                            throwableRef.set(t);
                        } finally {
                            // Ensure the EDT leaves the nested event loop even if the runnable fails.
                            if( DEBUG_RUNONMAIN ) {
                                System.err.println("OSXUtil.RunOnMainThread: appkit-runner done, thread "+Thread.currentThread().getName());
                            }
                            AWTEDTUtil.exitSecondaryLoop(secondaryLoop);
                        }
                    }
                };
                if( DEBUG_RUNONMAIN ) {
                    System.err.println("OSXUtil.RunOnMainThread: using AWT SecondaryLoop");
                }
                RunOnMainThread0(kickNSApp, runnable0);
                AWTEDTUtil.enterSecondaryLoop(secondaryLoop);
                final Throwable throwable = throwableRef.get();
                if( null != throwable ) {
                    throw new RuntimeException(throwable);
                }
                if( DEBUG_RUNONMAIN ) {
                    final long dt = System.currentTimeMillis() - t0;
                    System.err.println("OSXUtil.RunOnMainThread: exit (SecondaryLoop) dt "+dt+" ms, thread "+Thread.currentThread().getName());
                }
                return;
            }
        }
        // Utilize Java side lock/wait and simply pass the Runnable async to OSX main thread,
        // otherwise we may freeze the OSX main thread.
        final Object sync = new Object();
        final RunnableTask rt = new RunnableTask( runnable, waitUntilDone ? sync : null, true, waitUntilDone ? null : System.err );
        synchronized(sync) {
            if( DEBUG_RUNONMAIN ) {
                System.err.println("OSXUtil.RunOnMainThread: using sync-wait, thread "+Thread.currentThread().getName());
            }
            RunOnMainThread0(kickNSApp, rt);
            if( waitUntilDone ) {
                while( rt.isInQueue() ) {
                    try {
                        sync.wait();
                    } catch (final InterruptedException ie) {
                        throw new InterruptedRuntimeException(ie);
                    }
                    final Throwable throwable = rt.getThrowable();
                    if(null!=throwable) {
                        throw new RuntimeException(throwable);
                    }
                }
            }
        }
        if( DEBUG_RUNONMAIN ) {
            final long dt = System.currentTimeMillis() - t0;
            System.err.println("OSXUtil.RunOnMainThread: exit (sync-wait) dt "+dt+" ms, wait "+waitUntilDone+
                    ", kickNSApp "+kickNSApp+", thread "+Thread.currentThread().getName());
        }
    }
    static public interface LongTask {
        public long eval();
    }
    public static long RunOnMainThreadLong(final boolean kickNSApp, final LongTask task) {
        if( IsMainThread() ) {
            return task.eval(); // don't leave the JVM
        }
        if( AWTEDTUtil.isDispatchThread() ) {
            // See RunOnMainThread(..): keep the AWT EDT responsive while synchronously waiting for AppKit.
            final Object secondaryLoop = AWTEDTUtil.createSecondaryLoop();
            if( null != secondaryLoop ) {
                final AtomicLong result = new AtomicLong(0);
                final AtomicReference<Throwable> throwableRef = new AtomicReference<Throwable>();
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            result.set(task.eval());
                        } catch (final Throwable t) {
                            throwableRef.set(t);
                        } finally {
                            // Ensure the EDT leaves the nested event loop even if the runnable fails.
                            AWTEDTUtil.exitSecondaryLoop(secondaryLoop);
                        }
                    }
                };
                RunOnMainThread0(kickNSApp, runnable);
                AWTEDTUtil.enterSecondaryLoop(secondaryLoop);
                final Throwable throwable = throwableRef.get();
                if( null != throwable ) {
                    throw new RuntimeException(throwable);
                }
                return result.get();
            }
        }
        // Utilize Java side lock/wait and simply pass the Runnable async to OSX main thread,
        // otherwise we may freeze the OSX main thread.
        final Object sync = new Object();
        final AtomicLong result = new AtomicLong(0);
        final Runnable task0 = () -> { result.set( task.eval() ); };
        final RunnableTask rt = new RunnableTask( task0, sync, true, null);
        synchronized(sync) {
            RunOnMainThread0(kickNSApp, rt);
            while( rt.isInQueue() ) {
                try {
                    sync.wait();
                } catch (final InterruptedException ie) {
                    throw new InterruptedRuntimeException(ie);
                }
                final Throwable throwable = rt.getThrowable();
                if(null!=throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        }
        return result.get();
    }

    /**
     * Run later on ..
     * @param onMain if true, run on main-thread, otherwise on the current OSX thread.
     * @param runnable
     * @param delay delay to run the runnable in milliseconds
     */
    public static void RunLater(final boolean onMain, final Runnable runnable, final int delay) {
        RunLater0(onMain, false /* kickNSApp */, new RunnableTask( runnable, null, true, System.err ), delay);
    }

    /**
     * Wakes up NSApp thread by sending an empty NSEvent ..
     * <p>
     * This is deemed important <i>sometimes</i> where resources shall get freed ASAP, e.g. GL context etc.
     * </p>
     * <p>
     * The following scenarios requiring this <i>wake-up</i> are currently known:
     * <ul>
     *   <li>Destruction of an OpenGL context</li>
     *   <li>Destruction of Windows .. ?</li>
     *   <li>Stopping the NSApp</li>
     * </ul>
     * </p>
     * FIXME: Complete list of scenarios and reason it.
     */
    public static void KickNSApp() {
        KickNSApp0();
    }

    private static Runnable _nop = new Runnable() { @Override public void run() {}; };

    /** Issues a {@link #RunOnMainThread(boolean, boolean, Runnable)} w/ an <i>NOP</i> runnable, while waiting until done and issuing {@link #KickNSApp()}. */
    public static void WaitUntilFinish() {
        RunOnMainThread(true, true /* kickNSApp */, _nop);
    }

    /**
     * Run on OSX UI main thread.
     * <p>
     * 'waitUntilDone' is implemented on Java site via lock/wait on {@link FunctionTask} to not freeze OSX main thread.
     * </p>
     *
     * @param kickNSApp if <code>true</code> issues {@link #KickNSApp()}
     * @param func
     */
    public static <R,A> R RunOnMainThread(final boolean kickNSApp, final Function<R,A> func, final A... args) {
        if( IsMainThread() ) {
            return func.eval(args); // don't leave the JVM
        }
        if( AWTEDTUtil.isDispatchThread() ) {
            // See RunOnMainThread(..): keep the AWT EDT responsive while synchronously waiting for AppKit.
            final Object secondaryLoop = AWTEDTUtil.createSecondaryLoop();
            if( null != secondaryLoop ) {
                final AtomicReference<R> result = new AtomicReference<R>();
                final AtomicReference<Throwable> throwableRef = new AtomicReference<Throwable>();
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            result.set(func.eval(args));
                        } catch (final Throwable t) {
                            throwableRef.set(t);
                        } finally {
                            // Ensure the EDT leaves the nested event loop even if the runnable fails.
                            AWTEDTUtil.exitSecondaryLoop(secondaryLoop);
                        }
                    }
                };
                RunOnMainThread0(kickNSApp, runnable);
                AWTEDTUtil.enterSecondaryLoop(secondaryLoop);
                final Throwable throwable = throwableRef.get();
                if( null != throwable ) {
                    throw new RuntimeException(throwable);
                }
                return result.get();
            }
        }
        // Utilize Java side lock/wait and simply pass the Runnable async to OSX main thread,
        // otherwise we may freeze the OSX main thread.
        final Object sync = new Object();
        final FunctionTask<R,A> rt = new FunctionTask<R,A>( func, sync, true, null);
        synchronized(sync) {
            rt.setArgs(args);
            RunOnMainThread0(kickNSApp, rt);
            while( rt.isInQueue() ) {
                try {
                    sync.wait();
                } catch (final InterruptedException ie) {
                    throw new InterruptedRuntimeException(ie);
                }
                final Throwable throwable = rt.getThrowable();
                if(null!=throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        }
        return rt.getResult();
    }

    /**
     * Returns true if the current is the NSApplication main-thread.
     * <p>
     * Implementation utilizes a {@link ThreadLocal} storage boolean holding the answer,
     * which only gets set at the first call from each individual thread.
     * This minimizes unnecessary native callbacks.
     * </p>
     * @return {@code true} if current thread is the NSApplication main-thread, otherwise false.
     */
    public static boolean IsMainThread() {
        Boolean isMainThread = tlsIsMainThread.get();
        if( null == isMainThread ) {
            isMainThread = new Boolean(IsMainThread0());
            tlsIsMainThread.set(isMainThread);
        }
        return isMainThread.booleanValue();
    }

    /** Returns the screen refresh rate in Hz. If unavailable, returns 60Hz. */
    public static int GetScreenRefreshRate(final int scrn_idx) {
        return GetScreenRefreshRate0(scrn_idx);
    }

    private static final String getCurrentThreadName() { return Thread.currentThread().getName(); } // Callback for JNI
    private static final void dumpStack() { // Callback for JNI
        System.err.println("Stacktrace on thread "+Thread.currentThread().getName());
        ExceptionUtils.dumpStack(System.err);
    }

    private static native boolean initIDs0();
    private static native boolean isNSView0(long object);
    private static native boolean isNSWindow0(long object);
    private static native Object GetLocationOnScreen0(long windowOrView, int src_x, int src_y);
    private static native Object GetInsets0(long windowOrView);
    private static native float GetScreenPixelScale1(int displayID);
    private static native float GetScreenPixelScale2(long windowOrView);
    private static native float GetWindowPixelScale1(long windowOrView);
    private static native void SetWindowPixelScale1(final long windowOrView, final float reqPixelScale);
    private static native long CreateNSWindow0(int x, int y, int width, int height);
    private static native void DestroyNSWindow0(long nsWindow);
    private static native long GetNSView0(long nsWindow);
    private static native long GetNSWindow0(long nsView);
    private static native long CreateCALayer0(int width, int height, float contentsScale);
    private static native void AddCASublayer0(long rootCALayer, long subCALayer, int x, int y, int width, int height, float contentsScale, int caLayerQuirks);
    private static native void FixCALayerLayout0(long rootCALayer, long subCALayer, boolean visible, int x, int y, int width, int height, int caLayerQuirks);
    private static native void SetCALayerPixelScale0(long rootCALayer, long subCALayer, float contentsScale);
    private static native void RemoveCASublayer0(long rootCALayer, long subCALayer, boolean subCALayerRelease);
    private static native void DestroyCALayer0(long caLayer);
    private static native void RunOnMainThread0(boolean kickNSApp, Runnable runnable);
    private static native void RunLater0(boolean onMain, boolean kickNSApp, Runnable runnable, int delay);
    private static native void KickNSApp0();
    private static native boolean IsMainThread0();
    private static native int GetScreenRefreshRate0(int scrn_idx);
}
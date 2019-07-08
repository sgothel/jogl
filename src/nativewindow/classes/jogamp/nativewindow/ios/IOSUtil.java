/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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
package jogamp.nativewindow.ios;

import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.util.Insets;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.common.util.Function;
import com.jogamp.common.util.FunctionTask;
import com.jogamp.common.util.InterruptedRuntimeException;
import com.jogamp.common.util.RunnableTask;

import jogamp.nativewindow.Debug;
import jogamp.nativewindow.NWJNILibLoader;
import jogamp.nativewindow.ToolkitProperties;

public class IOSUtil implements ToolkitProperties {
    private static boolean isInit = false;
    private static final boolean DEBUG = Debug.debug("IOSUtil");

    /** FIXME HiDPI: OSX unique and maximum value {@value} */
    public static final int MAX_PIXELSCALE = 2;

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     * @see ToolkitProperties
     */
    public static synchronized void initSingleton() {
      if(!isInit) {
          if(DEBUG) {
              System.out.println("IOSUtil.initSingleton()");
          }
          if(!NWJNILibLoader.loadNativeWindow("ios")) {
              throw new NativeWindowException("NativeWindow IOS native library load error.");
          }

          if( !initIDs0() ) {
              throw new NativeWindowException("IOS: Could not initialized native stub");
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

    public static boolean isCALayer(final long object) {
        return 0 != object ? isCALayer0(object) : false;
    }

    public static boolean isCAEAGLLayer(final long object) {
        return 0 != object ? isCAEAGLLayer0(object) : false;
    }

    public static boolean isUIView(final long object) {
        return 0 != object ? isUIView0(object) : false;
    }

    public static boolean isUIWindow(final long object) {
        return 0 != object ? isUIWindow0(object) : false;
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

    public static float GetScreenPixelScaleByScreenIdx(final int screenIdx) {
      if( 0 <= screenIdx ) {
          return GetScreenPixelScale1(screenIdx);
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

    public static long CreateUIWindow(final int x, final int y, final int width, final int height) {
      final long res[] = { 0 };
      RunOnMainThread(true, false /* kickNSApp */, new Runnable() {
          @Override
          public void run() {
              res[0] = CreateUIWindow0(x, y, width, height);
          } } );
      return res[0];
    }
    public static void DestroyUIWindow(final long uiWindow) {
      DestroyUIWindow0(uiWindow);
    }
    public static long GetCALayer(final long uiView) {
      return 0 != uiView ? GetCALayer0(uiView) : 0;
    }
    public static long GetCAEAGLLayer(final long uiView) {
      return 0 != uiView ? GetCAEAGLLayer0(uiView) : 0;
    }
    public static long GetUIView(final long uiWindow, final boolean onlyEAGL) {
      return 0 != uiWindow ? GetUIView0(uiWindow, onlyEAGL) : 0;
    }
    public static long GetUIWindow(final long uiView) {
      return 0 != uiView ? GetUIWindow0(uiView) : 0;
    }

    /**
     * Set the UIView's pixelScale / contentScale for HiDPI
     *
     * @param uiView the mutable UIView instance
     * @param contentScaleFactor scale for HiDPI support: pixel-dim = window-dim x scale
     */
    public static void SetUIViewPixelScale(final long uiView, final float contentScaleFactor) {
        SetUIViewPixelScale0(uiView, contentScaleFactor);
    }
    /**
     * Get the UIView's pixelScale / contentScale for HiDPI
     *
     * @param uiView the UIView instance
     * @return used scale for HiDPI support: pixel-dim = window-dim x scale
     */
    public static float GetUIViewPixelScale(final long uiView) {
        return GetUIViewPixelScale0(uiView);
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
     * Get the CALayer's pixelScale / contentScale for HiDPI
     *
     * @param caLayer the CALayer instance
     * @return used scale for HiDPI support: pixel-dim = window-dim x scale
     */
    public static float GetCALayerPixelScale(final long caLayer) {
        return GetCALayerPixelScale0(caLayer);
    }

    /**
     * Run on OSX UI main thread.
     * <p>
     * 'waitUntilDone' is implemented on Java site via lock/wait on {@link RunnableTask} to not freeze OSX main thread.
     * </p>
     *
     * @param waitUntilDone
     * @param kickNSApp if <code>true</code> issues {@link #KickUIApp()}
     * @param runnable
     */
    public static void RunOnMainThread(final boolean waitUntilDone, final boolean kickNSApp, final Runnable runnable) {
        if( IsMainThread0() ) {
            runnable.run(); // don't leave the JVM
        } else {
            // Utilize Java side lock/wait and simply pass the Runnable async to OSX main thread,
            // otherwise we may freeze the OSX main thread.
            final Object sync = new Object();
            final RunnableTask rt = new RunnableTask( runnable, waitUntilDone ? sync : null, true, waitUntilDone ? null : System.err );
            synchronized(sync) {
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
        }
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

    private static Runnable _nop = new Runnable() { @Override public void run() {}; };

    /** Issues a {@link #RunOnMainThread(boolean, boolean, Runnable)} w/ an <i>NOP</i> runnable, while waiting until done and issuing {@link #KickUIApp()}. */
    public static void WaitUntilFinish() {
        RunOnMainThread(true, true /* kickNSApp */, _nop);
    }

    /**
     * Run on OSX UI main thread.
     * <p>
     * 'waitUntilDone' is implemented on Java site via lock/wait on {@link FunctionTask} to not freeze OSX main thread.
     * </p>
     *
     * @param waitUntilDone
     * @param kickUIApp if <code>true</code> issues {@link #KickUIApp()}
     * @param func
     */
    public static <R,A> R RunOnMainThread(final boolean waitUntilDone, final boolean kickUIApp, final Function<R,A> func, final A... args) {
        if( IsMainThread0() ) {
            return func.eval(args); // don't leave the JVM
        } else {
            // Utilize Java side lock/wait and simply pass the Runnable async to OSX main thread,
            // otherwise we may freeze the OSX main thread.
            final Object sync = new Object();
            final FunctionTask<R,A> rt = new FunctionTask<R,A>( func, waitUntilDone ? sync : null, true, waitUntilDone ? null : System.err );
            synchronized(sync) {
                rt.setArgs(args);
                RunOnMainThread0(kickUIApp, rt);
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
            return rt.getResult();
        }
    }

    public static boolean IsMainThread() {
        return IsMainThread0();
    }

    /** Returns the screen refresh rate in Hz. If unavailable, returns 60Hz. */
    public static int GetScreenRefreshRate(final int scrn_idx) {
        return GetScreenRefreshRate0(scrn_idx);
    }

    public static void CreateGLViewDemoA() {
        CreateGLViewDemoA0();
    }

    private static native boolean initIDs0();
    private static native boolean isCALayer0(long object);
    private static native boolean isCAEAGLLayer0(long object);
    private static native boolean isUIView0(long object);
    private static native boolean isUIWindow0(long object);
    private static native Object GetLocationOnScreen0(long windowOrView, int src_x, int src_y);
    private static native Object GetInsets0(long windowOrView);
    private static native float GetScreenPixelScale1(int screenIdx);
    private static native float GetScreenPixelScale2(long windowOrView);
    private static native long CreateUIWindow0(int x, int y, int width, int height);
    private static native void DestroyUIWindow0(long uiWindow);
    private static native long GetCALayer0(long uiView);
    private static native long GetCAEAGLLayer0(long uiView);
    private static native long GetUIView0(long uiWindow, boolean onlyEAGL);
    private static native long GetUIWindow0(long uiView);
    private static native void SetUIViewPixelScale0(final long uiView, final float contentScaleFactor);
    private static native float GetUIViewPixelScale0(final long uiView);
    private static native void SetCALayerPixelScale0(long rootCALayer, long subCALayer, float contentsScale);
    private static native float GetCALayerPixelScale0(final long caLayer);

    private static native void RunOnMainThread0(boolean kickNSApp, Runnable runnable);
    private static native void RunLater0(boolean onMain, boolean kickNSApp, Runnable runnable, int delay);
    private static native void KickUIApp0();
    private static native boolean IsMainThread0();
    private static native int GetScreenRefreshRate0(int scrn_idx);
    private static native void CreateGLViewDemoA0();

}

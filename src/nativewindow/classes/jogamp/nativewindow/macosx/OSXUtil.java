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

import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.Point;

import com.jogamp.common.util.Function;
import com.jogamp.common.util.FunctionTask;
import com.jogamp.common.util.RunnableTask;

import jogamp.nativewindow.Debug;
import jogamp.nativewindow.NWJNILibLoader;
import jogamp.nativewindow.ToolkitProperties;

public class OSXUtil implements ToolkitProperties {
    private static boolean isInit = false;  
    private static final boolean DEBUG = Debug.debug("OSXUtil");
    
    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     * @see ToolkitProperties
     */
    public static synchronized void initSingleton() {
      if(!isInit) {
          if(DEBUG) {
              System.out.println("OSXUtil.initSingleton()");
          }
          if(!NWJNILibLoader.loadNativeWindow("macosx")) {
              throw new NativeWindowException("NativeWindow MacOSX native library load error.");
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
    
    public static boolean isNSView(long object) {
        return 0 != object ? isNSView0(object) : false;
    }
    
    public static boolean isNSWindow(long object) {
        return 0 != object ? isNSWindow0(object) : false;
    }
    
    /**
     * In case the <code>windowOrView</code> is top-level,
     * you shall set <code>topLevel</code> to true where
     * insets gets into account to compute the client position as follows:
     * <pre>
      if(topLevel) {
          // top-level position -> client window position
          final Insets insets = GetInsets(windowOrView);
          los.setX(los.getX() + insets.getLeftWidth());
          los.setY(los.getY() + insets.getTopHeight());
      }
     * </pre>
     * @param windowOrView
     * @param topLevel
     * @param src_x
     * @param src_y
     * @return the client position
     */
    public static Point GetLocationOnScreen(long windowOrView, boolean topLevel, int src_x, int src_y) {      
      final Point los = (Point) GetLocationOnScreen0(windowOrView, src_x, src_y);
      if(topLevel) {
          // top-level position -> client window position
          final Insets insets = GetInsets(windowOrView);
          los.set(los.getX() + insets.getLeftWidth(), los.getY() + insets.getTopHeight());
      }
      return los;
    }
    
    public static Insets GetInsets(long windowOrView) {
      return (Insets) GetInsets0(windowOrView);
    }
    
    public static long CreateNSWindow(int x, int y, int width, int height) {
      return CreateNSWindow0(x, y, width, height);
    }
    public static void DestroyNSWindow(long nsWindow) {
        DestroyNSWindow0(nsWindow);
    }
    public static long GetNSView(long nsWindow) {
      return GetNSView0(nsWindow);
    }
    public static long GetNSWindow(long nsView) {
      return GetNSWindow0(nsView);
    }
    
    /** 
     * Create a CALayer suitable to act as a root CALayer.
     * @see #DestroyCALayer(long)
     * @see #AddCASublayer(long, long) 
     */
    public static long CreateCALayer(final int width, final int height) {
      final long l = CreateCALayer0(width, height);
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
     * @param caLayerQuirks TODO
     * @see #CreateCALayer(int, int)
     * @see #RemoveCASublayer(long, long, boolean)
     */
    public static void AddCASublayer(final long rootCALayer, final long subCALayer, final int x, final int y, final int width, final int height, final int caLayerQuirks) {
        if(0==rootCALayer || 0==subCALayer) {
            throw new IllegalArgumentException("rootCALayer 0x"+Long.toHexString(rootCALayer)+", subCALayer 0x"+Long.toHexString(subCALayer));
        }
        if(DEBUG) {
            System.err.println("OSXUtil.AttachCALayer: caLayerQuirks "+caLayerQuirks+", 0x"+Long.toHexString(subCALayer)+" - "+Thread.currentThread().getName());
        }
        AddCASublayer0(rootCALayer, subCALayer, x, y, width, height, caLayerQuirks);
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
     * @param width the expected width
     * @param height the expected height
     * @param caLayerQuirks TODO
     */
    public static void FixCALayerLayout(final long rootCALayer, final long subCALayer, final boolean visible, final int x, final int y, final int width, final int height, final int caLayerQuirks) {
        if( 0==rootCALayer && 0==subCALayer ) {
            return;
        }
        FixCALayerLayout0(rootCALayer, subCALayer, visible, x, y, width, height, caLayerQuirks);
    }
    
    /** 
     * Detach a sub CALayer from the root CALayer.
     */
    public static void RemoveCASublayer(final long rootCALayer, final long subCALayer) {
        if(0==rootCALayer || 0==subCALayer) {
            throw new IllegalArgumentException("rootCALayer 0x"+Long.toHexString(rootCALayer)+", subCALayer 0x"+Long.toHexString(subCALayer));
        }
        if(DEBUG) {
            System.err.println("OSXUtil.DetachCALayer: 0x"+Long.toHexString(subCALayer)+" - "+Thread.currentThread().getName());
        }
        RemoveCASublayer0(rootCALayer, subCALayer);
    }
    
    /** 
     * Destroy a CALayer.
     * @see #CreateCALayer(int, int)
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
     * @param runnable
     */
    public static void RunOnMainThread(boolean waitUntilDone, Runnable runnable) {
        if( IsMainThread0() ) {
            runnable.run(); // don't leave the JVM
        } else {
            // Utilize Java side lock/wait and simply pass the Runnable async to OSX main thread,
            // otherwise we may freeze the OSX main thread.            
            Throwable throwable = null;
            final Object sync = new Object();
            final RunnableTask rt = new RunnableTask( runnable, waitUntilDone ? sync : null, true, waitUntilDone ? null : System.err ); 
            synchronized(sync) {
                RunOnMainThread0(rt);
                if( waitUntilDone ) {
                    try {
                        sync.wait();
                    } catch (InterruptedException ie) {
                        throwable = ie;
                    }
                    if(null==throwable) {
                        throwable = rt.getThrowable();
                    }
                    if(null!=throwable) {
                        throw new RuntimeException(throwable);
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
    public static void RunLater(boolean onMain, Runnable runnable, int delay) {
        RunLater0(onMain, new RunnableTask( runnable, null, true, System.err ), delay);
    }
    
    private static Runnable _nop = new Runnable() { public void run() {}; };
    
    /** Issues a {@link #RunOnMainThread(boolean, Runnable)} w/ an <i>NOP</i> runnable, while waiting until done. */ 
    public static void WaitUntilFinish() {
        RunOnMainThread(true, _nop);
    }
    
    /**
     * Run on OSX UI main thread.
     * <p> 
     * 'waitUntilDone' is implemented on Java site via lock/wait on {@link FunctionTask} to not freeze OSX main thread.
     * </p>
     * 
     * @param waitUntilDone
     * @param func
     */
    public static <R,A> R RunOnMainThread(boolean waitUntilDone, Function<R,A> func, A... args) {
        if( IsMainThread0() ) {
            return func.eval(args); // don't leave the JVM
        } else {
            // Utilize Java side lock/wait and simply pass the Runnable async to OSX main thread,
            // otherwise we may freeze the OSX main thread.            
            Throwable throwable = null;
            final Object sync = new Object();
            final FunctionTask<R,A> rt = new FunctionTask<R,A>( func, waitUntilDone ? sync : null, true, waitUntilDone ? null : System.err ); 
            synchronized(sync) {
                rt.setArgs(args);
                RunOnMainThread0(rt);
                if( waitUntilDone ) {
                    try {
                        sync.wait();
                    } catch (InterruptedException ie) {
                        throwable = ie;
                    }
                    if(null==throwable) {
                        throwable = rt.getThrowable();
                    }
                    if(null!=throwable) {
                        throw new RuntimeException(throwable);
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
    public static int GetScreenRefreshRate(int scrn_idx) {
        return GetScreenRefreshRate0(scrn_idx);
    }
    
    /***
    private static boolean  isAWTEDTMainThreadInit = false;
    private static boolean  isAWTEDTMainThread;
    
    public synchronized static boolean isAWTEDTMainThread() {
        if(!isAWTEDTMainThreadInit) {
            isAWTEDTMainThreadInit = true;
            if(Platform.AWT_AVAILABLE) {
                AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                   public void run() {
                       isAWTEDTMainThread = IsMainThread();
                       System.err.println("XXX: "+Thread.currentThread().getName()+" - isAWTEDTMainThread "+isAWTEDTMainThread);
                   }
                });
            } else {
                isAWTEDTMainThread = false;
            }
        }        
        return isAWTEDTMainThread;
    } */
    
    private static native boolean initIDs0();
    private static native boolean isNSView0(long object);
    private static native boolean isNSWindow0(long object);
    private static native Object GetLocationOnScreen0(long windowOrView, int src_x, int src_y);
    private static native Object GetInsets0(long windowOrView);
    private static native long CreateNSWindow0(int x, int y, int width, int height);
    private static native void DestroyNSWindow0(long nsWindow);
    private static native long GetNSView0(long nsWindow);
    private static native long GetNSWindow0(long nsView);
    private static native long CreateCALayer0(int width, int height);
    private static native void AddCASublayer0(long rootCALayer, long subCALayer, int x, int y, int width, int height, int caLayerQuirks);
    private static native void FixCALayerLayout0(long rootCALayer, long subCALayer, boolean visible, int x, int y, int width, int height, int caLayerQuirks);
    private static native void RemoveCASublayer0(long rootCALayer, long subCALayer);
    private static native void DestroyCALayer0(long caLayer);
    private static native void RunOnMainThread0(Runnable runnable);
    private static native void RunLater0(boolean onMain, Runnable runnable, int delay);
    private static native boolean IsMainThread0();
    private static native int GetScreenRefreshRate0(int scrn_idx);
}

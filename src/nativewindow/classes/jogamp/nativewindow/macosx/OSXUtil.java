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
        return isNSView0(object);
    }
    
    public static boolean isNSWindow(long object) {
        return isNSWindow0(object);
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
          los.setX(los.getX() + insets.getLeftWidth());
          los.setY(los.getY() + insets.getTopHeight());
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
    
    public static long CreateCALayer(int x, int y, int width, int height) {
        return CreateCALayer0(x, y, width, height);
    }
    public static void AddCASublayer(long rootCALayer, long subCALayer) {
        if(0==rootCALayer || 0==subCALayer) {
            throw new IllegalArgumentException("rootCALayer 0x"+Long.toHexString(rootCALayer)+", subCALayer 0x"+Long.toHexString(subCALayer));
        }
        AddCASublayer0(rootCALayer, subCALayer);
    }
    public static void RemoveCASublayer(long rootCALayer, long subCALayer) {
        if(0==rootCALayer || 0==subCALayer) {
            throw new IllegalArgumentException("rootCALayer 0x"+Long.toHexString(rootCALayer)+", subCALayer 0x"+Long.toHexString(subCALayer));
        }
        RemoveCASublayer0(rootCALayer, subCALayer);
    }
    public static void DestroyCALayer(long caLayer) {
        if(0==caLayer) {
            throw new IllegalArgumentException("caLayer 0x"+Long.toHexString(caLayer));
        }
        DestroyCALayer0(caLayer);    
    }
    
    public static void RunOnMainThread(boolean waitUntilDone, Runnable runnable) {
        if(IsMainThread0()) {
            runnable.run(); // don't leave the JVM
        } else {
            RunOnMainThread0(waitUntilDone, runnable);
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
    private static native long CreateCALayer0(int x, int y, int width, int height);
    private static native void AddCASublayer0(long rootCALayer, long subCALayer);
    private static native void RemoveCASublayer0(long rootCALayer, long subCALayer);
    private static native void DestroyCALayer0(long caLayer);
    private static native void RunOnMainThread0(boolean waitUntilDone, Runnable runnable);
    private static native boolean IsMainThread0();
    private static native int GetScreenRefreshRate0(int scrn_idx);
}

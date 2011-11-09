package jogamp.nativewindow.macosx;

import java.nio.Buffer;

import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.util.Point;

import jogamp.nativewindow.Debug;
import jogamp.nativewindow.NWJNILibLoader;
import jogamp.nativewindow.jawt.JAWT_DrawingSurfaceInfo;

public class OSXUtil {
    private static boolean isInit = false;  
    private static final boolean DEBUG = Debug.debug("OSXUtil");
    
    public static synchronized void initSingleton(boolean firstX11ActionOnProcess) {
      if(!isInit) {
          NWJNILibLoader.loadNativeWindow("macosx");
          
          if( !initIDs0() ) {
              throw new NativeWindowException("MacOSX: Could not initialized native stub");
          }
  
          if(DEBUG) {
              System.out.println("OSX.isFirstX11ActionOnProcess: "+firstX11ActionOnProcess);
          }
  
          isInit = true;
      }
    }

    public static boolean requiresToolkitLock() {
        return false;
    }
    
    public static Point GetLocationOnScreen(long windowOrView, int src_x, int src_y) {
      return (Point) GetLocationOnScreen0(windowOrView, src_x, src_y);
    }
    
    public static long CreateNSView(int x, int y, int width, int height) {
      return CreateNSView0(x, y, width, height);
    }
    public static void DestroyNSView(long nsView) {
        DestroyNSView0(nsView);
    }

    public static long CreateNSWindow(int x, int y, int width, int height) {
      return CreateNSWindow0(x, y, width, height);
    }
    public static void DestroyNSWindow(long nsWindow) {
        DestroyNSWindow0(nsWindow);
    }
    
    public static boolean AttachJAWTSurfaceLayer0(JAWT_DrawingSurfaceInfo dsi, long caLayer) {
        return AttachJAWTSurfaceLayer0(dsi.getBuffer(), caLayer);
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
    
    private static native boolean initIDs0();
    private static native Object GetLocationOnScreen0(long windowOrView, int src_x, int src_y);
    private static native long CreateNSView0(int x, int y, int width, int height);
    private static native void DestroyNSView0(long nsView);
    private static native long CreateNSWindow0(int x, int y, int width, int height);
    private static native void DestroyNSWindow0(long nsWindow);
    private static native boolean AttachJAWTSurfaceLayer0(Buffer jawtDrawingSurfaceInfoBuffer, long caLayer);
    private static native void RunOnMainThread0(boolean waitUntilDone, Runnable runnable);
    private static native boolean IsMainThread0();
}

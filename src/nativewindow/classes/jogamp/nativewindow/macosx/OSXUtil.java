package jogamp.nativewindow.macosx;

import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.util.Point;

import jogamp.nativewindow.Debug;
import jogamp.nativewindow.NWJNILibLoader;

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
    
    public static void RunOnMainThread(boolean waitUntilDone, Runnable runnable) {
        RunOnMainThread0(waitUntilDone, runnable);
    }
    
    public static boolean IsMainThread() {
        return IsMainThread0();
    }
    
    private static native boolean initIDs0();
    private static native Object GetLocationOnScreen0(long windowOrView, int src_x, int src_y);
    private static native void RunOnMainThread0(boolean waitUntilDone, Runnable runnable);
    private static native boolean IsMainThread0();
}

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
    
    public static long CreateCALayer() {
        return CreateCALayer0();
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
    
    private static native boolean initIDs0();
    private static native Object GetLocationOnScreen0(long windowOrView, int src_x, int src_y);
    private static native long CreateNSView0(int x, int y, int width, int height);
    private static native void DestroyNSView0(long nsView);
    private static native long CreateNSWindow0(int x, int y, int width, int height);
    private static native void DestroyNSWindow0(long nsWindow);
    private static native long CreateCALayer0();
    private static native void AddCASublayer0(long rootCALayer, long subCALayer);
    private static native void RemoveCASublayer0(long rootCALayer, long subCALayer);
    private static native void DestroyCALayer0(long caLayer);
    private static native void RunOnMainThread0(boolean waitUntilDone, Runnable runnable);
    private static native boolean IsMainThread0();
}

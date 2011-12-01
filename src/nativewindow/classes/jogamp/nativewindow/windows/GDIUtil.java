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
package jogamp.nativewindow.windows;

import javax.media.nativewindow.util.Point;
import javax.media.nativewindow.NativeWindowException;

import jogamp.nativewindow.NWJNILibLoader;
import jogamp.nativewindow.Debug;
import jogamp.nativewindow.x11.X11Util;

public class GDIUtil {
    private static final boolean DEBUG = Debug.debug("GDIUtil");
  
    private static final String dummyWindowClassNameBase = "_dummyWindow_clazz" ;
    private static RegisteredClassFactory dummyWindowClassFactory;
    private static boolean isInit = false;
  
    public static synchronized void initSingleton(boolean firstX11ActionOnProcess) {
        if(!isInit) {
            synchronized(X11Util.class) {
                if(!isInit) {
                    isInit = true;
                    NWJNILibLoader.loadNativeWindow("win32");

                    if( !initIDs0() ) {
                        throw new NativeWindowException("GDI: Could not initialized native stub");
                    }

                    if(DEBUG) {
                        System.out.println("GDI.isFirstX11ActionOnProcess: "+firstX11ActionOnProcess);
                    }

                    dummyWindowClassFactory = new RegisteredClassFactory(dummyWindowClassNameBase, getDummyWndProc0());
                }
            }
        }
    }
  
    public static boolean requiresToolkitLock() { return false; }
  
    private static RegisteredClass dummyWindowClass = null;
    private static Object dummyWindowSync = new Object();
  
    public static long CreateDummyWindow(int x, int y, int width, int height) {
        synchronized(dummyWindowSync) {
            dummyWindowClass = dummyWindowClassFactory.getSharedClass();
            return CreateDummyWindow0(dummyWindowClass.getHandle(), dummyWindowClass.getName(), dummyWindowClass.getName(), x, y, width, height);
        }
    }
  
    public static boolean DestroyDummyWindow(long hwnd) {
        boolean res;
        synchronized(dummyWindowSync) {
            if( null == dummyWindowClass ) {
                throw new InternalError("GDI Error ("+dummyWindowClassFactory.getSharedRefCount()+"): SharedClass is null");
            }
            res = GDI.DestroyWindow(hwnd);
            dummyWindowClassFactory.releaseSharedClass();
        }
        return res;
    }
  
    public static Point GetRelativeLocation(long src_win, long dest_win, int src_x, int src_y) {
        return (Point) GetRelativeLocation0(src_win, dest_win, src_x, src_y);
    }
    
    public static native boolean CreateWindowClass(long hInstance, String clazzName, long wndProc);
    public static native boolean DestroyWindowClass(long hInstance, String className);
    
    private static native boolean initIDs0();
    private static native long getDummyWndProc0();  
    private static native Object GetRelativeLocation0(long src_win, long dest_win, int src_x, int src_y);
  
    static native long CreateDummyWindow0(long hInstance, String className, String windowName, int x, int y, int width, int height);  
}

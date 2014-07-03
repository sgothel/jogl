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
import javax.media.nativewindow.NativeWindowFactory;

import jogamp.nativewindow.NWJNILibLoader;
import jogamp.nativewindow.Debug;
import jogamp.nativewindow.ToolkitProperties;

public class GDIUtil implements ToolkitProperties {
    private static final boolean DEBUG = Debug.debug("GDIUtil");

    private static final String dummyWindowClassNameBase = "_dummyWindow_clazz" ;
    private static RegisteredClassFactory dummyWindowClassFactory;
    private static boolean isInit = false;

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     * @see ToolkitProperties
     */
    public static synchronized void initSingleton() {
        if(!isInit) {
            synchronized(GDIUtil.class) {
                if(!isInit) {
                    if(DEBUG) {
                        System.out.println("GDI.initSingleton()");
                    }
                    if(!NWJNILibLoader.loadNativeWindow("win32")) {
                        throw new NativeWindowException("NativeWindow Windows native library load error.");
                    }
                    if( !initIDs0() ) {
                        throw new NativeWindowException("GDI: Could not initialized native stub");
                    }
                    dummyWindowClassFactory = new RegisteredClassFactory(dummyWindowClassNameBase, getDummyWndProc0(),
                                                                         true /* useDummyDispatchThread */,
                                                                         0 /* iconSmallHandle */, 0 /* iconBigHandle */);
                    if(DEBUG) {
                        System.out.println("GDI.initSingleton() dummyWindowClassFactory "+dummyWindowClassFactory);
                    }
                    isInit = true;
                }
            }
        }
    }

    /**
     * Called by {@link NativeWindowFactory#shutdown()}
     * @see ToolkitProperties
     */
    public static void shutdown() {
    }

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

    private static RegisteredClass dummyWindowClass = null;
    private static Object dummyWindowSync = new Object();

    public static long CreateDummyWindow(final int x, final int y, final int width, final int height) {
        synchronized(dummyWindowSync) {
            dummyWindowClass = dummyWindowClassFactory.getSharedClass();
            if(DEBUG) {
                System.out.println("GDI.CreateDummyWindow() dummyWindowClassFactory "+dummyWindowClassFactory);
                System.out.println("GDI.CreateDummyWindow() dummyWindowClass "+dummyWindowClass);
            }
            return CreateDummyWindow0(dummyWindowClass.getHInstance(), dummyWindowClass.getName(), dummyWindowClass.getHDispThreadContext(), dummyWindowClass.getName(), x, y, width, height);
        }
    }

    public static boolean DestroyDummyWindow(final long hwnd) {
        boolean res;
        synchronized(dummyWindowSync) {
            if( null == dummyWindowClass ) {
                throw new InternalError("GDI Error ("+dummyWindowClassFactory.getSharedRefCount()+"): SharedClass is null");
            }
            res = DestroyWindow0(dummyWindowClass.getHDispThreadContext(), hwnd);
            dummyWindowClassFactory.releaseSharedClass();
        }
        return res;
    }

    public static Point GetRelativeLocation(final long src_win, final long dest_win, final int src_x, final int src_y) {
        return (Point) GetRelativeLocation0(src_win, dest_win, src_x, src_y);
    }

    public static boolean IsUndecorated(final long win) {
        return IsUndecorated0(win);
    }

    public static boolean IsChild(final long win) {
        return IsChild0(win);
    }

    private static final void dumpStack() { Thread.dumpStack(); } // Callback for JNI

    /** Creates WNDCLASSEX instance */
    static native boolean CreateWindowClass0(long hInstance, String clazzName, long wndProc, long iconSmallHandle, long iconBigHandle);
    /** Destroys WNDCLASSEX instance */
    static native boolean DestroyWindowClass0(long hInstance, String className, long dispThreadCtx);
    static native long CreateDummyDispatchThread0();

    private static native boolean initIDs0();
    private static native long getDummyWndProc0();
    private static native Object GetRelativeLocation0(long src_win, long dest_win, int src_x, int src_y);
    private static native boolean IsChild0(long win);
    private static native boolean IsUndecorated0(long win);

    private static native long CreateDummyWindow0(long hInstance, String className, long dispThreadCtx, String windowName, int x, int y, int width, int height);
    private static native boolean DestroyWindow0(long dispThreadCtx, long win);
}

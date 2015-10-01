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

import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionNumber;

import jogamp.nativewindow.NWJNILibLoader;
import jogamp.nativewindow.Debug;
import jogamp.nativewindow.ToolkitProperties;

public class GDIUtil implements ToolkitProperties {
    private static final boolean DEBUG = Debug.debug("GDIUtil");

    private static final String dummyWindowClassNameBase = "_dummyWindow_clazz" ;
    private static RegisteredClassFactory dummyWindowClassFactory;
    private static volatile boolean isInit = false;

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

    /**
     * Windows >= 8, even if not manifested
     * @see https://msdn.microsoft.com/en-us/library/windows/desktop/ms724832%28v=vs.85%29.aspx
     */
    public static final VersionNumber Win8Version = new VersionNumber(6, 2, 0);

    /**
     * Windows >= 10, manifested
     * @see https://msdn.microsoft.com/en-us/library/windows/desktop/ms724832%28v=vs.85%29.aspx
     */
    public static final VersionNumber Win10Version = new VersionNumber(10, 0, 0);

    /**
     * Wrapper for {@link GDI#DwmIsCompositionEnabled()}
     * taking the Windows 8 version into account.
     * <p>
     * If Windows version >= {@link #Win8Version} method always returns {@code true},
     * otherwise value of {@link GDI#DwmIsCompositionEnabled()} is returned.
     * </p>
     * @see https://msdn.microsoft.com/en-us/library/windows/desktop/aa969518%28v=vs.85%29.aspx
     */
    public static boolean DwmIsCompositionEnabled() {
        final VersionNumber winVer = Platform.getOSVersionNumber();
        if( winVer.compareTo(Win8Version) >= 0 ) {
            return true;
        } else {
            return GDI.DwmIsCompositionEnabled();
        }
    }

    public static boolean DwmSetupTranslucency(final long hwnd, final boolean enable) {
        if( !GDI.DwmIsExtensionAvailable() ) {
            if(DEBUG) {
                System.err.println("GDIUtil.DwmSetupTranslucency on wnd 0x"+Long.toHexString(hwnd)+": enable "+enable+" -> failed, extension not available");
            }
            return !enable;
        }
        final VersionNumber winVer = Platform.getOSVersionNumber();
        final boolean isWin8 = winVer.compareTo(Win8Version) >= 0;
        if( !isWin8 && !GDI.DwmIsCompositionEnabled() ) {
            if(DEBUG) {
                System.err.println("GDIUtil.DwmSetupTranslucency on wnd 0x"+Long.toHexString(hwnd)+": enable "+enable+" -> failed, composition disabled");
            }
            return !enable;
        }
        final boolean hasWinCompEXT = GDI.IsWindowCompositionExtensionAvailable();
        final boolean useWinCompEXT = isWin8 && hasWinCompEXT;
        final boolean isUndecorated = IsUndecorated(hwnd);
        boolean ok;
        if( useWinCompEXT && !isUndecorated ) {
            final AccentPolicy accentPolicy = AccentPolicy.create();
            if( enable ) {
                // For undecorated windows, this would also enable the Glass effect!
                accentPolicy.setAccentState(GDI.ACCENT_ENABLE_BLURBEHIND);
            } else {
                accentPolicy.setAccentState(GDI.ACCENT_DISABLED);
            }
            ok = GDI.SetWindowCompositionAccentPolicy(hwnd, accentPolicy);
        } else {
            // Works even for >= Win8, if undecorated
            final DWM_BLURBEHIND bb = DWM_BLURBEHIND.create();
            final int dwFlags = enable ? GDI.DWM_BB_ENABLE | GDI.DWM_BB_BLURREGION | GDI.DWM_BB_TRANSITIONONMAXIMIZED : GDI.DWM_BB_ENABLE;
            // final int dwFlags = GDI.DWM_BB_ENABLE;
            bb.setDwFlags( dwFlags );
            bb.setFEnable( enable ? 1 : 0 );
            bb.setHRgnBlur(0);
            bb.setFTransitionOnMaximized(1);
            ok = GDI.DwmEnableBlurBehindWindow(hwnd, bb);
            if( ok ) {
                final MARGINS m = MARGINS.create();
                m.setCxLeftWidth(-1);
                m.setCxRightWidth(-1);
                m.setCyBottomHeight(-1);
                m.setCyTopHeight(-1);
                ok = GDI.DwmExtendFrameIntoClientArea(hwnd, m);
            }
        }
        /***
         * Not required ..
         *
        if( ok && isWin8 && !isUndecorated ) {
            final IntBuffer pvAttribute = Buffers.newDirectIntBuffer(1);
            if( enable ) {
                // Glass Effect even if undecorated, hence not truly 100% translucent!
                pvAttribute.put(0, GDI.DWMNCRP_ENABLED);
            } else {
                pvAttribute.put(0, GDI.DWMNCRP_DISABLED);
            }
            final int err = GDI.DwmSetWindowAttribute(hwnd, GDI.DWMWA_NCRENDERING_POLICY,
                                                      pvAttribute,
                                                      Buffers.sizeOfBufferElem(pvAttribute)*pvAttribute.capacity());
            ok = 0 == err; // S_OK
        } */
        if(DEBUG) {
            final boolean isChild = IsChild(hwnd);
            System.err.println("GDIUtil.DwmSetupTranslucency on wnd 0x"+Long.toHexString(hwnd)+": enable "+enable+", isUndecorated "+isUndecorated+", isChild "+isChild+
                               ", version "+winVer+", isWin8 "+isWin8+", hasWinCompEXT "+hasWinCompEXT+", useWinCompEXT "+useWinCompEXT+" -> ok: "+ok);
        }
        return ok;
    }

    public static boolean IsUndecorated(final long win) {
        return IsUndecorated0(win);
    }

    public static boolean IsChild(final long win) {
        return IsChild0(win);
    }

    public static void SetProcessThreadsAffinityMask(final long affinityMask, final boolean verbose) {
        SetProcessThreadsAffinityMask0(affinityMask, verbose);
    }

    private static final void dumpStack() { ExceptionUtils.dumpStack(System.err); } // Callback for JNI

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

    private static native void SetProcessThreadsAffinityMask0(long affinityMask, boolean verbose);

    private static native long CreateDummyWindow0(long hInstance, String className, long dispThreadCtx, String windowName, int x, int y, int width, int height);
    private static native boolean DestroyWindow0(long dispThreadCtx, long win);
}

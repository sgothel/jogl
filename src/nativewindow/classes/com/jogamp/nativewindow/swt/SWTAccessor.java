/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
package com.jogamp.nativewindow.swt;

import com.jogamp.common.os.Platform;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.eclipse.swt.graphics.GCData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.VisualIDHolder;

import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.nativewindow.macosx.MacOSXGraphicsDevice;
import com.jogamp.nativewindow.windows.WindowsGraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;

import jogamp.nativewindow.macosx.OSXUtil;
import jogamp.nativewindow.x11.X11Lib;

public class SWTAccessor {
    private static final boolean DEBUG = true;

    private static final Field swt_control_handle;
    private static final boolean swt_uses_long_handles;

    private static Object swt_osx_init = new Object();
    private static Field swt_osx_control_view = null;
    private static Field swt_osx_view_id = null;

    private static final String nwt;
    public static final boolean isOSX;
    public static final boolean isWindows;
    public static final boolean isX11;
    public static final boolean isX11GTK;

    // X11/GTK, Windows/GDI, ..
    private static final String str_handle = "handle";

    // OSX/Cocoa
    private static final String str_osx_view = "view";  // OSX
    private static final String str_osx_id = "id";    // OSX
    // static final String str_NSView = "org.eclipse.swt.internal.cocoa.NSView";

    private static final Method swt_control_internal_new_GC;
    private static final Method swt_control_internal_dispose_GC;
    private static final String str_internal_new_GC = "internal_new_GC";
    private static final String str_internal_dispose_GC = "internal_dispose_GC";

    private static final String str_OS_gtk_class = "org.eclipse.swt.internal.gtk.OS";
    public static final Class<?> OS_gtk_class;
    private static final String str_OS_gtk_version = "GTK_VERSION";
    public static final VersionNumber OS_gtk_version;

    private static final Method OS_gtk_widget_realize;
    private static final Method OS_gtk_widget_unrealize; // optional (removed in SWT 4.3)
    private static final Method OS_GTK_WIDGET_WINDOW;
    private static final Method OS_gtk_widget_get_window;
    private static final Method OS_gdk_x11_drawable_get_xdisplay;
    private static final Method OS_gdk_x11_display_get_xdisplay;
    private static final Method OS_gdk_window_get_display;
    private static final Method OS_gdk_x11_drawable_get_xid;
    private static final Method OS_gdk_x11_window_get_xid;
    private static final Method OS_gdk_window_set_back_pixmap;

    private static final String str_gtk_widget_realize = "gtk_widget_realize";
    private static final String str_gtk_widget_unrealize = "gtk_widget_unrealize";
    private static final String str_GTK_WIDGET_WINDOW = "GTK_WIDGET_WINDOW";
    private static final String str_gtk_widget_get_window = "gtk_widget_get_window";
    private static final String str_gdk_x11_drawable_get_xdisplay = "gdk_x11_drawable_get_xdisplay";
    private static final String str_gdk_x11_display_get_xdisplay = "gdk_x11_display_get_xdisplay";
    private static final String str_gdk_window_get_display = "gdk_window_get_display";
    private static final String str_gdk_x11_drawable_get_xid = "gdk_x11_drawable_get_xid";
    private static final String str_gdk_x11_window_get_xid = "gdk_x11_window_get_xid";
    private static final String str_gdk_window_set_back_pixmap = "gdk_window_set_back_pixmap";

    private static final VersionNumber GTK_VERSION_2_14_0 = new VersionNumber(2, 14, 0);
    private static final VersionNumber GTK_VERSION_2_24_0 = new VersionNumber(2, 24, 0);
    private static final VersionNumber GTK_VERSION_3_0_0  = new VersionNumber(3,  0, 0);

    private static VersionNumber GTK_VERSION(final int version) {
        // return (major << 16) + (minor << 8) + micro;
        final int micro = ( version       ) & 0xff;
        final int minor = ( version >>  8 ) & 0xff;
        final int major = ( version >> 16 ) & 0xff;
        return new VersionNumber(major, minor, micro);
    }

    static {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                NativeWindowFactory.initSingleton(); // last resort ..
                return null;
            } } );

        nwt = NativeWindowFactory.getNativeWindowType(false);
        isOSX = NativeWindowFactory.TYPE_MACOSX == nwt;
        isWindows = NativeWindowFactory.TYPE_WINDOWS == nwt;
        isX11 = NativeWindowFactory.TYPE_X11 == nwt;

        Field f = null;
        if( !isOSX ) {
            try {
                f = Control.class.getField(str_handle);
            } catch (final Exception ex) {
                throw new NativeWindowException(ex);
            }
        }
        swt_control_handle = f; // maybe null !

        boolean ulh;
        if (null != swt_control_handle) {
            ulh = swt_control_handle.getGenericType().toString().equals(long.class.toString());
        } else {
            ulh = Platform.is64Bit();
        }
        swt_uses_long_handles = ulh;
        // System.err.println("SWT long handles: " + swt_uses_long_handles);
        // System.err.println("Platform 64bit: "+Platform.is64Bit());

        Method m=null;
        try {
            m = ReflectionUtil.getMethod(Control.class, str_internal_new_GC, new Class[] { GCData.class });
        } catch (final Exception ex) {
            throw new NativeWindowException(ex);
        }
        swt_control_internal_new_GC = m;

        try {
            if(swt_uses_long_handles) {
                m = Control.class.getDeclaredMethod(str_internal_dispose_GC, new Class[] { long.class, GCData.class });
            } else {
                m = Control.class.getDeclaredMethod(str_internal_dispose_GC, new Class[] { int.class, GCData.class });
            }
        } catch (final NoSuchMethodException ex) {
            throw new NativeWindowException(ex);
        }
        swt_control_internal_dispose_GC = m;

        Class<?> c=null;
        VersionNumber _gtk_version = new VersionNumber(0, 0, 0);
        Method m1=null, m2=null, m3=null, m4=null, m5=null, m6=null, m7=null, m8=null, m9=null, ma=null;
        final Class<?> handleType = swt_uses_long_handles  ? long.class : int.class ;
        if( isX11 ) {
            // mandatory
            try {
                c = ReflectionUtil.getClass(str_OS_gtk_class, false, SWTAccessor.class.getClassLoader());
                final Field field_OS_gtk_version = c.getField(str_OS_gtk_version);
                _gtk_version = GTK_VERSION(field_OS_gtk_version.getInt(null));
                m1 = c.getDeclaredMethod(str_gtk_widget_realize, handleType);
                if (_gtk_version.compareTo(GTK_VERSION_2_14_0) >= 0) {
                    m4 = c.getDeclaredMethod(str_gtk_widget_get_window, handleType);
                } else {
                    m3 = c.getDeclaredMethod(str_GTK_WIDGET_WINDOW, handleType);
                }
                if (_gtk_version.compareTo(GTK_VERSION_2_24_0) >= 0) {
                    m6 = c.getDeclaredMethod(str_gdk_x11_display_get_xdisplay, handleType);
                    m7 = c.getDeclaredMethod(str_gdk_window_get_display, handleType);
                } else {
                    m5 = c.getDeclaredMethod(str_gdk_x11_drawable_get_xdisplay, handleType);
                }
                if (_gtk_version.compareTo(GTK_VERSION_3_0_0) >= 0) {
                    m9 = c.getDeclaredMethod(str_gdk_x11_window_get_xid, handleType);
                } else {
                    m8 = c.getDeclaredMethod(str_gdk_x11_drawable_get_xid, handleType);
                }
                ma = c.getDeclaredMethod(str_gdk_window_set_back_pixmap, handleType, handleType, boolean.class);
            } catch (final Exception ex) { throw new NativeWindowException(ex); }
            // optional
            try {
                m2 = c.getDeclaredMethod(str_gtk_widget_unrealize, handleType);
            } catch (final Exception ex) { }
        }
        OS_gtk_class = c;
        OS_gtk_version = _gtk_version;
        OS_gtk_widget_realize = m1;
        OS_gtk_widget_unrealize = m2;
        OS_GTK_WIDGET_WINDOW = m3;
        OS_gtk_widget_get_window = m4;
        OS_gdk_x11_drawable_get_xdisplay = m5;
        OS_gdk_x11_display_get_xdisplay = m6;
        OS_gdk_window_get_display = m7;
        OS_gdk_x11_drawable_get_xid = m8;
        OS_gdk_x11_window_get_xid = m9;
        OS_gdk_window_set_back_pixmap = ma;

        isX11GTK = isX11 && null != OS_gtk_class;

        if(DEBUG) {
            System.err.println("SWTAccessor.<init>: GTK Version: "+OS_gtk_version);
        }
    }

    private static Number getIntOrLong(final long arg) {
        if(swt_uses_long_handles) {
            return Long.valueOf(arg);
        }
        return Integer.valueOf((int) arg);
    }

    private static void callStaticMethodL2V(final Method m, final long arg) {
        ReflectionUtil.callMethod(null, m, new Object[] { getIntOrLong(arg) });
    }

    private static void callStaticMethodLLZ2V(final Method m, final long arg0, final long arg1, final boolean arg3) {
        ReflectionUtil.callMethod(null, m, new Object[] { getIntOrLong(arg0), getIntOrLong(arg1), Boolean.valueOf(arg3) });
    }

    private static long callStaticMethodL2L(final Method m, final long arg) {
        final Object o = ReflectionUtil.callMethod(null, m, new Object[] { getIntOrLong(arg) });
        if(o instanceof Number) {
            return ((Number)o).longValue();
        } else {
            throw new InternalError("SWT method "+m.getName()+" didn't return int or long but "+o.getClass());
        }
    }

    //
    // Public properties
    //

    public static boolean isUsingLongHandles() {
        return swt_uses_long_handles;
    }

    public static boolean useX11GTK() { return isX11GTK; }
    public static VersionNumber GTK_VERSION() { return OS_gtk_version; }

    //
    // Common GTK
    //

    public static long gdk_widget_get_window(final long handle) {
        final long window;
        if (OS_gtk_version.compareTo(GTK_VERSION_2_14_0) >= 0) {
            window = callStaticMethodL2L(OS_gtk_widget_get_window, handle);
        } else {
            window = callStaticMethodL2L(OS_GTK_WIDGET_WINDOW, handle);
        }
        if(0 == window) {
            throw new NativeWindowException("Null gtk-window-handle of SWT handle 0x"+Long.toHexString(handle));
        }
        return window;
    }

    public static long gdk_window_get_xdisplay(final long window) {
        final long xdisplay;
        if (OS_gtk_version.compareTo(GTK_VERSION_2_24_0) >= 0) {
            final long display = callStaticMethodL2L(OS_gdk_window_get_display, window);
            if(0 == display) {
                throw new NativeWindowException("Null display-handle of gtk-window-handle 0x"+Long.toHexString(window));
            }
            xdisplay = callStaticMethodL2L(OS_gdk_x11_display_get_xdisplay, display);
        } else {
            xdisplay = callStaticMethodL2L(OS_gdk_x11_drawable_get_xdisplay, window);
        }
        if(0 == xdisplay) {
            throw new NativeWindowException("Null x11-display-handle of gtk-window-handle 0x"+Long.toHexString(window));
        }
        return xdisplay;
    }

    public static long gdk_window_get_xwindow(final long window) {
        final long xWindow;
        if (OS_gtk_version.compareTo(GTK_VERSION_3_0_0) >= 0) {
            xWindow = callStaticMethodL2L(OS_gdk_x11_window_get_xid, window);
        } else {
            xWindow = callStaticMethodL2L(OS_gdk_x11_drawable_get_xid, window);
        }
        if(0 == xWindow) {
            throw new NativeWindowException("Null x11-window-handle of gtk-window-handle 0x"+Long.toHexString(window));
        }
        return xWindow;
    }

    public static void gdk_window_set_back_pixmap(final long window, final long pixmap, final boolean parent_relative) {
        callStaticMethodLLZ2V(OS_gdk_window_set_back_pixmap, window, pixmap, parent_relative);
    }

    //
    // Common any toolkit
    //

    /**
     * @param swtControl the SWT Control to retrieve the native widget-handle from
     * @return the native widget-handle
     * @throws NativeWindowException if the widget handle is null
     */
    public static long getHandle(final Control swtControl) throws NativeWindowException {
        long h = 0;
        if( isOSX ) {
            synchronized(swt_osx_init) {
                try {
                    if(null == swt_osx_view_id) {
                        swt_osx_control_view = Control.class.getField(str_osx_view);
                        final Object view = swt_osx_control_view.get(swtControl);
                        swt_osx_view_id = view.getClass().getField(str_osx_id);
                        h = swt_osx_view_id.getLong(view);
                    } else {
                        h = swt_osx_view_id.getLong( swt_osx_control_view.get(swtControl) );
                    }
                } catch (final Exception ex) {
                    throw new NativeWindowException(ex);
                }
            }
        } else {
            try {
                h = swt_control_handle.getLong(swtControl);
            } catch (final Exception ex) {
                throw new NativeWindowException(ex);
            }
        }
        if(0 == h) {
            throw new NativeWindowException("Null widget-handle of SWT "+swtControl.getClass().getName()+": "+swtControl.toString());
        }
        return h;
    }

    public static void setRealized(final Control swtControl, final boolean realize)
            throws NativeWindowException
    {
        if(!realize && swtControl.isDisposed()) {
            return;
        }
        final long handle = getHandle(swtControl);

        if(null != OS_gtk_class) {
            invoke(true, new Runnable() {
                @Override
                public void run() {
                    if(realize) {
                        callStaticMethodL2V(OS_gtk_widget_realize, handle);
                    } else if(null != OS_gtk_widget_unrealize) {
                        callStaticMethodL2V(OS_gtk_widget_unrealize, handle);
                    }
                }
            });
        }
    }

    /**
     * @param swtControl the SWT Control to retrieve the native device handle from
     * @return the AbstractGraphicsDevice w/ the native device handle
     * @throws NativeWindowException if the widget handle is null
     * @throws UnsupportedOperationException if the windowing system is not supported
     */
    public static AbstractGraphicsDevice getDevice(final Control swtControl) throws NativeWindowException, UnsupportedOperationException {
        final long handle = getHandle(swtControl);
        if( isX11GTK ) {
            final long xdisplay0 = gdk_window_get_xdisplay( gdk_widget_get_window( handle ) );
            return new X11GraphicsDevice(xdisplay0, AbstractGraphicsDevice.DEFAULT_UNIT, false /* owner */);
        }
        if( isWindows ) {
            return new WindowsGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
        }
        if( isOSX ) {
            return new MacOSXGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
        }
        throw new UnsupportedOperationException("n/a for this windowing system: "+nwt);
    }

    /**
     * @param device
     * @param screen -1 is default screen of the given device, e.g. maybe 0 or determined by native API. >= 0 is specific screen
     * @return
     */
    public static AbstractGraphicsScreen getScreen(final AbstractGraphicsDevice device, final int screen) {
        return NativeWindowFactory.createScreen(device, screen);
    }

    public static int getNativeVisualID(final AbstractGraphicsDevice device, final long windowHandle) {
        if( isX11 ) {
            return X11Lib.GetVisualIDFromWindow(device.getHandle(), windowHandle);
        }
        if( isWindows || isOSX ) {
            return VisualIDHolder.VID_UNDEFINED;
        }
        throw new UnsupportedOperationException("n/a for this windowing system: "+nwt);
    }

    /**
     * @param swtControl the SWT Control to retrieve the native window handle from
     * @return the native window handle
     * @throws NativeWindowException if the widget handle is null
     * @throws UnsupportedOperationException if the windowing system is not supported
     */
    public static long getWindowHandle(final Control swtControl) throws NativeWindowException, UnsupportedOperationException {
        final long handle = getHandle(swtControl);
        if(0 == handle) {
            throw new NativeWindowException("Null SWT handle of SWT control "+swtControl);
        }
        if( isX11GTK ) {
            return gdk_window_get_xwindow( gdk_widget_get_window( handle ) );
        }
        if( isWindows || isOSX ) {
            return handle;
        }
        throw new UnsupportedOperationException("n/a for this windowing system: "+nwt);
    }

    public static long newGC(final Control swtControl, final GCData gcData) {
        final Object[] o = new Object[1];
        invoke(true, new Runnable() {
            @Override
            public void run() {
                o[0] = ReflectionUtil.callMethod(swtControl, swt_control_internal_new_GC, new Object[] { gcData });
            }
        });
        if(o[0] instanceof Number) {
            return ((Number)o[0]).longValue();
        } else {
            throw new InternalError("SWT internal_new_GC did not return int or long but "+o[0].getClass());
        }
    }

    public static void disposeGC(final Control swtControl, final long gc, final GCData gcData) {
        invoke(true, new Runnable() {
            @Override
            public void run() {
                if(swt_uses_long_handles) {
                    ReflectionUtil.callMethod(swtControl, swt_control_internal_dispose_GC, new Object[] { Long.valueOf(gc), gcData });
                }  else {
                    ReflectionUtil.callMethod(swtControl, swt_control_internal_dispose_GC, new Object[] { Integer.valueOf((int)gc), gcData });
                }
            }
        });
    }

   /**
    * Runs the specified action in an SWT compatible thread, which is:
    * <ul>
    *   <li>Mac OSX
    *   <ul>
    *     <!--li>AWT EDT: In case AWT is available, the AWT EDT is the OSX UI main thread</li-->
    *     <li><i>Main Thread</i>: Run on OSX UI main thread. 'wait' is implemented on Java site via lock/wait on {@link RunnableTask} to not freeze OSX main thread.</li>
    *   </ul></li>
    *   <li>Linux, Windows, ..
    *   <ul>
    *     <li>Current thread.</li>
    *   </ul></li>
    * </ul>
    * @see Platform#AWT_AVAILABLE
    * @see Platform#getOSType()
    */
    public static void invoke(final boolean wait, final Runnable runnable) {
        if( isOSX ) {
            // Use SWT main thread! Only reliable config w/ -XStartOnMainThread !?
            OSXUtil.RunOnMainThread(wait, false, runnable);
        } else {
            runnable.run();
        }
    }

   /**
    * Runs the specified action on the SWT UI thread.
    * <p>
    * If <code>display</code> is disposed or the current thread is the SWT UI thread
    * {@link #invoke(boolean, Runnable)} is being used.
    * @see #invoke(boolean, Runnable)
    */
    public static void invoke(final org.eclipse.swt.widgets.Display display, final boolean wait, final Runnable runnable) {
        if( display.isDisposed() || Thread.currentThread() == display.getThread() ) {
            invoke(wait, runnable);
        } else if( wait ) {
            display.syncExec(runnable);
        } else {
            display.asyncExec(runnable);
        }
    }

    //
    // Specific X11 GTK ChildWindow - Using plain X11 native parenting (works well)
    //

    public static long createCompatibleX11ChildWindow(final AbstractGraphicsScreen screen, final Control swtControl, final int visualID, final int width, final int height) {
        final long handle = getHandle(swtControl);
        final long parentWindow = gdk_widget_get_window( handle );
        gdk_window_set_back_pixmap (parentWindow, 0, false);

        final long x11ParentHandle = gdk_window_get_xwindow(parentWindow);
        final long x11WindowHandle = X11Lib.CreateWindow(x11ParentHandle, screen.getDevice().getHandle(), screen.getIndex(), visualID, width, height, true, true);

        return x11WindowHandle;
    }

    public static void resizeX11Window(final AbstractGraphicsDevice device, final Rectangle clientArea, final long x11Window) {
        X11Lib.SetWindowPosSize(device.getHandle(), x11Window, clientArea.x, clientArea.y, clientArea.width, clientArea.height);
    }
    public static void destroyX11Window(final AbstractGraphicsDevice device, final long x11Window) {
        X11Lib.DestroyWindow(device.getHandle(), x11Window);
    }

    //
    // Specific X11 SWT/GTK ChildWindow - Using SWT/GTK native parenting (buggy - sporadic resize flickering, sporadic drop of rendering)
    //
    // FIXME: Need to use reflection for 32bit access as well !
    //

    // public static final int GDK_WA_TYPE_HINT = 1 << 9;
    // public static final int GDK_WA_VISUAL = 1 << 6;

    public static long createCompatibleGDKChildWindow(final Control swtControl, final int visualID, final int width, final int height) {
        return 0;
        /**
        final long handle = SWTAccessor.getHandle(swtControl);
        final long parentWindow = gdk_widget_get_window( handle );

        final long screen = OS.gdk_screen_get_default ();
        final long gdkvisual = OS.gdk_x11_screen_lookup_visual (screen, visualID);

        final GdkWindowAttr attrs = new GdkWindowAttr();
        attrs.width = width > 0 ? width : 1;
        attrs.height = height > 0 ? height : 1;
        attrs.event_mask = OS.GDK_KEY_PRESS_MASK | OS.GDK_KEY_RELEASE_MASK |
                           OS.GDK_FOCUS_CHANGE_MASK | OS.GDK_POINTER_MOTION_MASK |
                           OS.GDK_BUTTON_PRESS_MASK | OS.GDK_BUTTON_RELEASE_MASK |
                           OS.GDK_ENTER_NOTIFY_MASK | OS.GDK_LEAVE_NOTIFY_MASK |
                           OS.GDK_EXPOSURE_MASK | OS.GDK_VISIBILITY_NOTIFY_MASK |
                           OS.GDK_POINTER_MOTION_HINT_MASK;
        attrs.window_type = OS.GDK_WINDOW_CHILD;
        attrs.visual = gdkvisual;

        final long childWindow = OS.gdk_window_new (parentWindow, attrs, OS.GDK_WA_VISUAL|GDK_WA_TYPE_HINT);
        OS.gdk_window_set_user_data (childWindow, handle);
        OS.gdk_window_set_back_pixmap (parentWindow, 0, false);

        OS.gdk_window_show (childWindow);
        OS.gdk_flush();
        return childWindow; */
    }

    public static void showGDKWindow(final long gdkWindow) {
        /* OS.gdk_window_show (gdkWindow);
        OS.gdk_flush(); */
    }
    public static void focusGDKWindow(final long gdkWindow) {
        /*
        OS.gdk_window_show (gdkWindow);
        OS.gdk_window_focus(gdkWindow, 0);
        OS.gdk_flush(); */
    }
    public static void resizeGDKWindow(final Rectangle clientArea, final long gdkWindow) {
        /**
        OS.gdk_window_move (gdkWindow, clientArea.x, clientArea.y);
        OS.gdk_window_resize (gdkWindow, clientArea.width, clientArea.height);
        OS.gdk_flush(); */
    }

    public static void destroyGDKWindow(final long gdkWindow) {
        // OS.gdk_window_destroy (gdkWindow);
    }
}

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
import org.eclipse.swt.widgets.Control;

import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.VisualIDHolder;

import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.nativewindow.macosx.MacOSXGraphicsDevice;
import com.jogamp.nativewindow.windows.WindowsGraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsScreen;

import jogamp.nativewindow.macosx.OSXUtil;
import jogamp.nativewindow.x11.X11Lib;

public class SWTAccessor {
    private static final boolean DEBUG = true;
    
    private static final Field swt_control_handle;
    private static final boolean swt_uses_long_handles;
    
    // X11/GTK, Windows/GDI, ..
    private static final String str_handle = "handle";
    
    // OSX/Cocoa
    private static final String str_view = "view";  // OSX
    private static final String str_id = "id";    // OSX
    // static final String str_NSView = "org.eclipse.swt.internal.cocoa.NSView";
    
    private static final Method swt_control_internal_new_GC;
    private static final Method swt_control_internal_dispose_GC;
    private static final String str_internal_new_GC = "internal_new_GC";
    private static final String str_internal_dispose_GC = "internal_dispose_GC";

    private static final String str_OS_gtk_class = "org.eclipse.swt.internal.gtk.OS";
    private static final Class<?> OS_gtk_class;
    private static final String str_OS_gtk_version = "GTK_VERSION";
    private static final VersionNumber OS_gtk_version;
    private static final Method OS_gtk_widget_realize;
    private static final Method OS_gtk_widget_unrealize; // optional (removed in SWT 4.3)
    private static final Method OS_GTK_WIDGET_WINDOW;
    private static final Method OS_gtk_widget_get_window;
    private static final Method OS_gdk_x11_drawable_get_xdisplay;
    private static final Method OS_gdk_x11_display_get_xdisplay;
    private static final Method OS_gdk_window_get_display;    
    private static final Method OS_gdk_x11_drawable_get_xid;  
    private static final Method OS_gdk_x11_window_get_xid;
    private static final String str_gtk_widget_realize = "gtk_widget_realize";
    private static final String str_gtk_widget_unrealize = "gtk_widget_unrealize";
    private static final String str_GTK_WIDGET_WINDOW = "GTK_WIDGET_WINDOW";
    private static final String str_gtk_widget_get_window = "gtk_widget_get_window";
    private static final String str_gdk_x11_drawable_get_xdisplay = "gdk_x11_drawable_get_xdisplay";
    private static final String str_gdk_x11_display_get_xdisplay = "gdk_x11_display_get_xdisplay";
    private static final String str_gdk_window_get_display = "gdk_window_get_display";
    private static final String str_gdk_x11_drawable_get_xid = "gdk_x11_drawable_get_xid";
    private static final String str_gdk_x11_window_get_xid = "gdk_x11_window_get_xid";
    
    private static final VersionNumber GTK_VERSION_2_14_0 = new VersionNumber(2, 14, 0);
    private static final VersionNumber GTK_VERSION_2_24_0 = new VersionNumber(2, 24, 0);
    private static final VersionNumber GTK_VERSION_3_0_0  = new VersionNumber(3,  0, 0);
    
    private static VersionNumber GTK_VERSION(int version) {
        // return (major << 16) + (minor << 8) + micro;
        final int micro = ( version       ) & 0x0f;
        final int minor = ( version >>  8 ) & 0x0f;
        final int major = ( version >> 16 ) & 0x0f;
        return new VersionNumber(major, minor, micro);
    }
    
    static {
        Field f = null;
        
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                NativeWindowFactory.initSingleton(); // last resort ..
                return null;
            } } );
        
        final String nwt = NativeWindowFactory.getNativeWindowType(false);

        if(NativeWindowFactory.TYPE_MACOSX != nwt ) {
            try {
                f = Control.class.getField(str_handle);
            } catch (Exception ex) {
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
        } catch (Exception ex) {
            throw new NativeWindowException(ex);
        }
        swt_control_internal_new_GC = m;
        
        try {
            if(swt_uses_long_handles) {
                m = Control.class.getDeclaredMethod(str_internal_dispose_GC, new Class[] { long.class, GCData.class });            
            } else {
                m = Control.class.getDeclaredMethod(str_internal_dispose_GC, new Class[] { int.class, GCData.class });                
            }
        } catch (NoSuchMethodException ex) {
            throw new NativeWindowException(ex);
        }
        swt_control_internal_dispose_GC = m;

        Class<?> c=null;
        VersionNumber _gtk_version = new VersionNumber(0, 0, 0);
        Method m1=null, m2=null, m3=null, m4=null, m5=null, m6=null, m7=null, m8=null, m9=null;
        Class<?> handleType = swt_uses_long_handles  ? long.class : int.class ;
        if( NativeWindowFactory.TYPE_X11 == nwt ) {
            // mandatory
            try {
                c = ReflectionUtil.getClass(str_OS_gtk_class, false, SWTAccessor.class.getClassLoader());
                Field field_OS_gtk_version = c.getField(str_OS_gtk_version);
                _gtk_version = GTK_VERSION(field_OS_gtk_version.getInt(null));
                m1 = c.getDeclaredMethod(str_gtk_widget_realize, handleType);        
                if( _gtk_version.compareTo(GTK_VERSION_2_14_0) < 0 ) {
                    m3 = c.getDeclaredMethod(str_GTK_WIDGET_WINDOW, handleType);
                } else {
                    m4 = c.getDeclaredMethod(str_gtk_widget_get_window, handleType);
                }
                if( _gtk_version.compareTo(GTK_VERSION_2_24_0) < 0 ) {
                    m5 = c.getDeclaredMethod(str_gdk_x11_drawable_get_xdisplay, handleType);
                } else {
                    m6 = c.getDeclaredMethod(str_gdk_x11_display_get_xdisplay, handleType);
                    m7 = c.getDeclaredMethod(str_gdk_window_get_display, handleType);                
                }                                
                if( _gtk_version.compareTo(GTK_VERSION_3_0_0) < 0 ) {
                    m8 = c.getDeclaredMethod(str_gdk_x11_drawable_get_xid, handleType);
                } else {
                    m9 = c.getDeclaredMethod(str_gdk_x11_window_get_xid, handleType);
                }
            } catch (Exception ex) { throw new NativeWindowException(ex); }
            // optional 
            try {
                m2 = c.getDeclaredMethod(str_gtk_widget_unrealize, handleType);
            } catch (Exception ex) { }
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
        
        if(DEBUG) {
            System.err.println("SWTAccessor.<init>: GTK Version: "+OS_gtk_version);
        }
    }
    
    
    static Object getIntOrLong(long arg) {
        if(swt_uses_long_handles) {
            return new Long(arg);
        }
        return new Integer((int) arg);
    }
    
    static void callStaticMethodL2V(Method m, long arg) {        
        ReflectionUtil.callMethod(null, m, new Object[] { getIntOrLong(arg) });
    }
    
    static long callStaticMethodL2L(Method m, long arg) {
        Object o = ReflectionUtil.callMethod(null, m, new Object[] { getIntOrLong(arg) });
        if(o instanceof Number) {
            return ((Number)o).longValue();
        } else {
            throw new InternalError("SWT method "+m.getName()+" didn't return int or long but "+o.getClass());
        }
    }
        
    public static boolean isUsingLongHandles() {
        return swt_uses_long_handles;
    }

    public static VersionNumber GTK_VERSION() { return OS_gtk_version; }
    
    /**
     * @param swtControl the SWT Control to retrieve the native widget-handle from
     * @return the native widget-handle
     * @throws NativeWindowException if the widget handle is null
     */
    public static long getHandle(Control swtControl) throws NativeWindowException {
        long h = 0;
        if(NativeWindowFactory.TYPE_MACOSX == NativeWindowFactory.getNativeWindowType(false) ) {
            try {
                Field fView = Control.class.getField(str_view);
                Object view = fView.get(swtControl);
                Field fId = view.getClass().getField(str_id);
                h = fId.getLong(view);
            } catch (Exception ex) {
                throw new NativeWindowException(ex);
            }            
        } else {       
            try {
                h = swt_control_handle.getLong(swtControl);
            } catch (Exception ex) {
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
        
    private static long gdk_x11_display_get_xdisplay(long window) {
        final long xdisplay;
        if ( OS_gtk_version.compareTo(GTK_VERSION_2_24_0) >= 0 ) {
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
    
    private static long gdk_widget_get_window(long handle) {
        final long window;
        if ( OS_gtk_version.compareTo(GTK_VERSION_2_14_0) >= 0 ) {
            window = callStaticMethodL2L(OS_gtk_widget_get_window, handle);
        } else {
            window = callStaticMethodL2L(OS_GTK_WIDGET_WINDOW, handle);
        }
        if(0 == window) {
            throw new NativeWindowException("Null gtk-window-handle of SWT handle 0x"+Long.toHexString(handle));
        }
        return window;
    }
    
    private static long gdk_window_get_xwindow(long window) {
        final long xwindow;
        if ( OS_gtk_version.compareTo(GTK_VERSION_3_0_0) >= 0 ) {
            xwindow = callStaticMethodL2L(OS_gdk_x11_window_get_xid, window);
        } else {
            xwindow = callStaticMethodL2L(OS_gdk_x11_drawable_get_xid, window);
        }
        if(0 == xwindow) {
            throw new NativeWindowException("Null x11-window-handle of gtk-window-handle 0x"+Long.toHexString(window));
        }
        return xwindow;
    }
    
    /**
     * @param swtControl the SWT Control to retrieve the native device handle from
     * @return the AbstractGraphicsDevice w/ the native device handle
     * @throws NativeWindowException if the widget handle is null 
     * @throws UnsupportedOperationException if the windowing system is not supported
     */
    public static AbstractGraphicsDevice getDevice(Control swtControl) throws NativeWindowException, UnsupportedOperationException {
        long handle = getHandle(swtControl);
        if( null != OS_gtk_class ) {
            final long xdisplay0 = gdk_x11_display_get_xdisplay( gdk_widget_get_window( handle ) );
            // final String displayName = X11Lib.XDisplayString(xdisplay0);
            // final long xdisplay1 = X11Util.openDisplay(displayName);
            // return new X11GraphicsDevice(xdisplay1, AbstractGraphicsDevice.DEFAULT_UNIT, true /* owner */);
            return new X11GraphicsDevice(xdisplay0, AbstractGraphicsDevice.DEFAULT_UNIT, false /* owner */);
        }
        final String nwt = NativeWindowFactory.getNativeWindowType(false);
        if( NativeWindowFactory.TYPE_WINDOWS == nwt ) {
            return new WindowsGraphicsDevice(AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);
        }
        if( NativeWindowFactory.TYPE_MACOSX == nwt ) {
            return new MacOSXGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
        }
        throw new UnsupportedOperationException("n/a for this windowing system: "+nwt);
    }
    
    public static AbstractGraphicsScreen getScreen(AbstractGraphicsDevice device, int screen) throws UnsupportedOperationException {
        if( null != OS_gtk_class ) {
            return new X11GraphicsScreen((X11GraphicsDevice)device, screen);
        }
        final String nwt = NativeWindowFactory.getNativeWindowType(false);
        if( NativeWindowFactory.TYPE_WINDOWS == nwt ||
            NativeWindowFactory.TYPE_MACOSX == nwt ) {
            return new DefaultGraphicsScreen(device, screen);
        }
        throw new UnsupportedOperationException("n/a for this windowing system: "+nwt);        
    }
    
    public static int getNativeVisualID(AbstractGraphicsDevice device, long windowHandle) {
        if( null != OS_gtk_class ) {
            return X11Lib.GetVisualIDFromWindow(device.getHandle(), windowHandle);
        }
        final String nwt = NativeWindowFactory.getNativeWindowType(false);
        if( NativeWindowFactory.TYPE_WINDOWS == nwt ||
            NativeWindowFactory.TYPE_MACOSX == nwt ) {
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
    public static long getWindowHandle(Control swtControl) throws NativeWindowException, UnsupportedOperationException {
        final long handle = getHandle(swtControl);        
        if( null != OS_gtk_class ) {
            return gdk_window_get_xwindow( gdk_widget_get_window( handle ) );            
        }
        final String nwt = NativeWindowFactory.getNativeWindowType(false);
        if( NativeWindowFactory.TYPE_WINDOWS == nwt ||
            NativeWindowFactory.TYPE_MACOSX == nwt ) {
            return handle;
        }
        throw new UnsupportedOperationException("n/a for this windowing system: "+nwt);
    }
    
    public static long newGC(final Control swtControl, final GCData gcData) {
        final Object[] o = new Object[1];
        invoke(true, new Runnable() {
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
            public void run() {
                if(swt_uses_long_handles) {
                    ReflectionUtil.callMethod(swtControl, swt_control_internal_dispose_GC, new Object[] { new Long(gc), gcData });
                }  else {
                    ReflectionUtil.callMethod(swtControl, swt_control_internal_dispose_GC, new Object[] { new Integer((int)gc), gcData });
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
    *     <li><i>Main Thread</i>: Run on OSX UI main thread.</li>
    *   </ul></li>
    *   <li>Linux, Windows, ..
    *   <ul>
    *     <li>Current thread.</li>
    *   </ul></li>  
    * </ul>
    * @see Platform#AWT_AVAILABLE
    * @see Platform#getOSType()
    */
    public static void invoke(boolean wait, Runnable runnable) {
        if( Platform.OS_TYPE == Platform.OSType.MACOS ) {
            // Use SWT main thread! Only reliable config w/ -XStartOnMainThread !?
            OSXUtil.RunOnMainThread(wait, runnable);
        } else {
            runnable.run();
        }        
    }
    
}

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
package jogamp.nativewindow.swt;

import com.jogamp.common.os.Platform;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.swt.graphics.GCData;
import org.eclipse.swt.widgets.Control;

import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.windows.WindowsGraphicsDevice;
import javax.media.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.common.util.ReflectionUtil;
import javax.media.nativewindow.macosx.MacOSXGraphicsDevice;

import jogamp.nativewindow.macosx.OSXUtil;

public class SWTAccessor {
    static final Field swt_control_handle;
    static final boolean swt_uses_long_handles;
    
    // X11/GTK, Windows/GDI, ..
    static final String str_handle = "handle";
    
    // OSX/Cocoa
    static final String str_view = "view";  // OSX
    static final String str_id = "id";    // OSX
    // static final String str_NSView = "org.eclipse.swt.internal.cocoa.NSView";
    
    static final Method swt_control_internal_new_GC;
    static final Method swt_control_internal_dispose_GC;
    static final String str_internal_new_GC = "internal_new_GC";
    static final String str_internal_dispose_GC = "internal_dispose_GC";

    static final String str_OS_gtk_class = "org.eclipse.swt.internal.gtk.OS";
    static final Class<?> OS_gtk_class;
    static final Method OS_gtk_widget_realize;
    static final Method OS_gtk_widget_unrealize;
    static final Method OS_GTK_WIDGET_WINDOW;
    static final Method OS_gdk_x11_drawable_get_xdisplay;
    static final Method OS_gdk_x11_drawable_get_xid;    
    static final String str_gtk_widget_realize = "gtk_widget_realize";
    static final String str_gtk_widget_unrealize = "gtk_widget_unrealize";
    static final String str_GTK_WIDGET_WINDOW = "GTK_WIDGET_WINDOW";
    static final String str_gdk_x11_drawable_get_xdisplay = "gdk_x11_drawable_get_xdisplay";
    static final String str_gdk_x11_drawable_get_xid = "gdk_x11_drawable_get_xid";
    
    static {
        Field f = null;
        
        if(NativeWindowFactory.TYPE_MACOSX != NativeWindowFactory.getNativeWindowType(false) ) {
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
        Method m1=null, m2=null, m3=null, m4=null, m5=null;
        Class<?> handleType = swt_uses_long_handles  ? long.class : int.class ;
        if( NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(false) ) {
            try {
                c = ReflectionUtil.getClass(str_OS_gtk_class, false, SWTAccessor.class.getClassLoader());
                m1 = c.getDeclaredMethod(str_gtk_widget_realize, handleType);        
                m2 = c.getDeclaredMethod(str_gtk_widget_unrealize, handleType);
                m3 = c.getDeclaredMethod(str_GTK_WIDGET_WINDOW, handleType);
                m4 = c.getDeclaredMethod(str_gdk_x11_drawable_get_xdisplay, handleType);
                m5 = c.getDeclaredMethod(str_gdk_x11_drawable_get_xid, handleType);
            } catch (Exception ex) { throw new NativeWindowException(ex); }
        }
        OS_gtk_class = c;
        OS_gtk_widget_realize = m1;
        OS_gtk_widget_unrealize = m2;
        OS_GTK_WIDGET_WINDOW = m3;
        OS_gdk_x11_drawable_get_xdisplay = m4;
        OS_gdk_x11_drawable_get_xid = m5;
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

    public static long getHandle(Control swtControl) {
        long h = 0;
        if(NativeWindowFactory.TYPE_MACOSX == NativeWindowFactory.getNativeWindowType(false) ) {
            try {
                Field fView = Control.class.getField(str_view);
                Object view = fView.get(swtControl);
                Field fId = view.getClass().getField(str_id);
                return fId.getLong(view);
            } catch (Exception ex) {
                throw new NativeWindowException(ex);
            }            
        }
        
        try {
            h = swt_control_handle.getLong(swtControl);
        } catch (Exception ex) {
            throw new NativeWindowException(ex);
        }
        return h;
    }

    public static void setRealized(final Control swtControl, final boolean realize) {
        final long handle = getHandle(swtControl);
        
        if(null != OS_gtk_class) {
            invoke(true, new Runnable() {
                public void run() {
                    if(realize) {
                        callStaticMethodL2V(OS_gtk_widget_realize, handle);
                    } else {
                        callStaticMethodL2V(OS_gtk_widget_unrealize, handle);
                    }                    
                }
            });
        }
    }
    
    public static AbstractGraphicsDevice getDevice(Control swtControl) {
        long handle = getHandle(swtControl);
        if( null != OS_gtk_class ) {
            long widgedHandle = callStaticMethodL2L(OS_GTK_WIDGET_WINDOW, handle);
            long displayHandle = callStaticMethodL2L(OS_gdk_x11_drawable_get_xdisplay, widgedHandle);
            // FIXME: May think about creating a private non-shared X11 Display handle, like we use to for AWT
            //        to avoid locking problems !
            return new X11GraphicsDevice(displayHandle, AbstractGraphicsDevice.DEFAULT_UNIT, false);
        }
        if( NativeWindowFactory.TYPE_WINDOWS == NativeWindowFactory.getNativeWindowType(false) ) {
            return new WindowsGraphicsDevice(AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);
        }
        if( NativeWindowFactory.TYPE_MACOSX == NativeWindowFactory.getNativeWindowType(false) ) {
            return new MacOSXGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
        }
        throw new UnsupportedOperationException("n/a for this windowing system: "+NativeWindowFactory.getNativeWindowType(false));
    }
    
    public static long getWindowHandle(Control swtControl) {
        long handle = getHandle(swtControl);        
        if( null != OS_gtk_class ) {
            long widgedHandle = callStaticMethodL2L(OS_GTK_WIDGET_WINDOW, handle);
            return callStaticMethodL2L(OS_gdk_x11_drawable_get_xid, widgedHandle);
        }
        if( NativeWindowFactory.TYPE_WINDOWS == NativeWindowFactory.getNativeWindowType(false) ||
            NativeWindowFactory.TYPE_MACOSX == NativeWindowFactory.getNativeWindowType(false) ) {
            return handle;
        }
        throw new UnsupportedOperationException("n/a for this windowing system: "+NativeWindowFactory.getNativeWindowType(false));
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
    
    public static void invoke(boolean wait, Runnable runnable) {
        if(Platform.OS_TYPE == Platform.OSType.MACOS) {
            OSXUtil.RunOnMainThread(wait, runnable);
        } else {
            runnable.run();
        }        
    }
    
}

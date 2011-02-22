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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.swt.graphics.GCData;
import org.eclipse.swt.widgets.Control;

import javax.media.nativewindow.NativeWindowException;
import com.jogamp.common.util.ReflectionUtil;

public class SWTAccessor {
    static final Method swt_control_internal_new_GC;
    static final Method swt_control_internal_dispose_GC;
    static final boolean swt_uses_long_handles;
    static final Field swt_control_handle;

    static final String str_internal_new_GC = "internal_new_GC";
    static final String str_internal_dispose_GC = "internal_dispose_GC";
    static final String str_handle = "handle";

    static {
        Method m=null;
        try {
            m = ReflectionUtil.getMethod(Control.class, str_internal_new_GC, new Class[] { GCData.class });
        } catch (Exception ex) {
            throw new NativeWindowException(ex);
        }
        swt_control_internal_new_GC = m;

        boolean swt_uses_long_tmp = false;
        try {
            m = Control.class.getDeclaredMethod(str_internal_dispose_GC, new Class[] { int.class, GCData.class });
            swt_uses_long_tmp = false;
        } catch (NoSuchMethodException ex1) {
            try {
                m = Control.class.getDeclaredMethod(str_internal_dispose_GC, new Class[] { long.class, GCData.class });
                swt_uses_long_tmp = true;
            } catch (NoSuchMethodException ex2) {
                throw new NativeWindowException("Neither 'int' nor 'long' variant of '"+str_internal_dispose_GC+"' exist", ex2);
            }
        }
        swt_uses_long_handles = swt_uses_long_tmp;
        swt_control_internal_dispose_GC = m;

        Field f = null;
        try {
            f = Control.class.getField(str_handle);
        } catch (Exception ex) {
            throw new NativeWindowException(ex);
        }
        swt_control_handle = f;
    }

    public static boolean isUsingLongHandles() {
        return swt_uses_long_handles;
    }
    
    public static long getHandle(Control swtControl) {
        long h = 0;
        try {
            h = swt_control_handle.getLong(swtControl);
        } catch (Exception ex) {
            throw new NativeWindowException(ex);
        }
        return h;
    }
    
    public static long newGC(Control swtControl, GCData gcData) {
        Object o = ReflectionUtil.callMethod(swtControl, swt_control_internal_new_GC, new Object[] { gcData });
        if(o instanceof Number) {
            return ((Number)o).longValue();
        } else {
            throw new InternalError("SWT internal_new_GC did not return int or long but "+o.getClass());
        }
    }

    public static void disposeGC(Control swtControl, long gc, GCData gcData) {
        if(swt_uses_long_handles) {
            ReflectionUtil.callMethod(swtControl, swt_control_internal_dispose_GC, new Object[] { new Long(gc), gcData });
        }  else {
            ReflectionUtil.callMethod(swtControl, swt_control_internal_dispose_GC, new Object[] { new Integer((int)gc), gcData });
        }
    }
}

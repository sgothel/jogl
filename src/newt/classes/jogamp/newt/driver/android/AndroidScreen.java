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

package jogamp.newt.driver.android;

import javax.media.nativewindow.*;
import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.Point;

import com.jogamp.newt.ScreenMode;
import com.jogamp.newt.util.ScreenModeUtil;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

public class AndroidScreen extends jogamp.newt.ScreenImpl {

    static {
        AndroidDisplay.initSingleton();
    }

    public AndroidScreen() {
    }

    protected void createNativeImpl() {
        aScreen = new DefaultGraphicsScreen(getDisplay().getGraphicsDevice(), screen_idx);
    }

    protected void closeNativeImpl() { }

    protected ScreenMode getCurrentScreenModeImpl() {
        final Context ctx = jogamp.common.os.android.StaticContext.getContext();
        final WindowManager wmgr = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        final DisplayMetrics outMetrics = new DisplayMetrics();
        final android.view.Display aDisplay = wmgr.getDefaultDisplay();
        aDisplay.getMetrics(outMetrics);
        
        final int arot = aDisplay.getRotation();
        final int nrot = androidRotation2NewtRotation(arot);
        int[] props = new int[ScreenModeUtil.NUM_SCREEN_MODE_PROPERTIES_ALL];
        int offset = 1; // set later for verification of iterator
        offset = getScreenSize(outMetrics, nrot, props, offset);
        offset = getBpp(aDisplay, props, offset); 
        offset = getScreenSizeMM(outMetrics, props, offset);
        props[offset++] = (int) aDisplay.getRefreshRate();
        props[offset++] = nrot;
        props[offset - ScreenModeUtil.NUM_SCREEN_MODE_PROPERTIES_ALL] = offset; // count
        return ScreenModeUtil.streamIn(props, 0);
    }
    
    protected int validateScreenIndex(int idx) {
        return 0; // FIXME: only one screen available ? 
    }
    
    protected void getVirtualScreenOriginAndSize(Point virtualOrigin, Dimension virtualSize) {
        virtualOrigin.setX(0);
        virtualOrigin.setY(0);
        final ScreenMode sm = getCurrentScreenMode();
        virtualSize.setWidth(sm.getRotatedWidth());
        virtualSize.setHeight(sm.getRotatedHeight());
    }
    
    //----------------------------------------------------------------------
    // Internals only
    //
    static int androidRotation2NewtRotation(int arot) {
        switch(arot) {
            case Surface.ROTATION_270: return ScreenMode.ROTATE_270;
            case Surface.ROTATION_180: return ScreenMode.ROTATE_180;
            case Surface.ROTATION_90: return ScreenMode.ROTATE_90;
            case Surface.ROTATION_0:
        }
        return ScreenMode.ROTATE_0;
    }
    static int getScreenSize(DisplayMetrics outMetrics, int nrot, int[] props, int offset) {
        // swap width and height, since Android reflects rotated dimension, we don't
        if (ScreenMode.ROTATE_90 == nrot || ScreenMode.ROTATE_270 == nrot) {
            props[offset++] = outMetrics.heightPixels;
            props[offset++] = outMetrics.widthPixels;
        } else {
            props[offset++] = outMetrics.widthPixels;
            props[offset++] = outMetrics.heightPixels;
        }
        return offset;
    }    
    static int getBpp(android.view.Display aDisplay, int[] props, int offset) {
        int bpp;
        switch(aDisplay.getPixelFormat()) {
            case PixelFormat.RGBA_8888: bpp=32; break;
            case PixelFormat.RGBX_8888: bpp=32; break;
            case PixelFormat.RGB_888:   bpp=24; break;
            case PixelFormat.RGB_565:   bpp=16; break;
            case PixelFormat.RGBA_5551: bpp=16; break;
            case PixelFormat.RGBA_4444: bpp=16; break;
            case PixelFormat.RGB_332:   bpp= 8; break;
            default: bpp=32;   
        }            
        props[offset++] = bpp;        
        return offset;
    }
    static int getScreenSizeMM(DisplayMetrics outMetrics, int[] props, int offset) {
        final float iw = (float) outMetrics.widthPixels / outMetrics.xdpi;
        final float ih = (float) outMetrics.heightPixels / outMetrics.xdpi;
        final float mmpi = 25.4f;
        props[offset++] = (int) ((iw * mmpi)+0.5);
        props[offset++]   = (int) ((ih * mmpi)+0.5);
        return offset;
    }    
}


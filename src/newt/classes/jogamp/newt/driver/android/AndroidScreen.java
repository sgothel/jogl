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
import javax.media.nativewindow.util.DimensionReadOnly;
import javax.media.nativewindow.util.SurfaceSize;

import com.jogamp.newt.ScreenMode;
import com.jogamp.newt.util.MonitorMode;

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

    public synchronized boolean setAppContext(Context ctx) {
        if(!((AndroidDisplay) getDisplay()).setAppContext(ctx)) {
            return false;
        }
        final WindowManager wmgr = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        sm = getScreenMode(wmgr.getDefaultDisplay());
        setScreenSize(sm.getMonitorMode().getSurfaceSize().getResolution().getWidth(),
                      sm.getMonitorMode().getSurfaceSize().getResolution().getHeight());
        return true;
    }
    public synchronized Context getAppContext() {
        return ((AndroidDisplay) getDisplay()).getAppContext();
    }
    
    protected ScreenMode getCurrentScreenModeImpl() {
        return sm;
    }
    
    ScreenMode sm = null;
    
    //----------------------------------------------------------------------
    // Internals only
    //
    static DimensionReadOnly getScreenSize(DisplayMetrics outMetrics) {
        return new Dimension(outMetrics.widthPixels, outMetrics.heightPixels);
    }    
    static SurfaceSize getSurfaceSize(android.view.Display aDisplay, DimensionReadOnly dim) {
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
        return new SurfaceSize(dim, bpp);        
    }
    static DimensionReadOnly getScreenSizeMM(DisplayMetrics outMetrics) {
        final float iw = (float) outMetrics.widthPixels / outMetrics.xdpi;
        final float ih = (float) outMetrics.heightPixels / outMetrics.xdpi;
        final float mmpi = 25.4f;
        return new Dimension((int) ((iw * mmpi)+0.5), (int) ((ih * mmpi)+0.5)); 
    }    
    static int getRotation(int androidRotation) {
        int nrot;
        switch(androidRotation) {
            case Surface.ROTATION_270: nrot = ScreenMode.ROTATE_270; break;
            case Surface.ROTATION_180: nrot = ScreenMode.ROTATE_180; break;
            case Surface.ROTATION_90: nrot = ScreenMode.ROTATE_90; break;
            case Surface.ROTATION_0:
            default: nrot = ScreenMode.ROTATE_0;
        }
        return nrot;        
    }
    static ScreenMode getScreenMode(android.view.Display aDisplay) {
        final DisplayMetrics outMetrics = new DisplayMetrics();
        aDisplay.getMetrics(outMetrics);
        
        final DimensionReadOnly screenSize = getScreenSize(outMetrics);
        final SurfaceSize surfaceSize = getSurfaceSize(aDisplay, screenSize);
        final DimensionReadOnly screenSizeMM = getScreenSizeMM(outMetrics);        
        final int refreshRate = (int) aDisplay.getRefreshRate();
        final MonitorMode mm = new MonitorMode(surfaceSize, screenSizeMM, refreshRate);
        
        final int rotation = getRotation(aDisplay.getRotation());
        return new ScreenMode(mm, rotation);
    }
    

}


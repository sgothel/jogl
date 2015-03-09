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

import com.jogamp.nativewindow.DefaultGraphicsScreen;

import jogamp.newt.MonitorModeProps;
import jogamp.newt.MonitorModeProps.Cache;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;

public class ScreenDriver extends jogamp.newt.ScreenImpl {

    static {
        DisplayDriver.initSingleton();
    }

    public ScreenDriver() {
    }

    @Override
    protected void createNativeImpl() {
        aScreen = new DefaultGraphicsScreen(getDisplay().getGraphicsDevice(), screen_idx);
    }

    @Override
    protected void closeNativeImpl() { }

    @Override
    protected int validateScreenIndex(final int idx) {
        return 0; // FIXME: only one screen available ?
    }

    private final MonitorMode getModeImpl(final Cache cache, final android.view.Display aDisplay, final DisplayMetrics outMetrics, final int modeIdx, final int screenSizeNRot, final int nrot) {
        final int[] props = new int[MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL];
        int i = 0;
        props[i++] = MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL;
        i = getScreenSize(outMetrics, screenSizeNRot, props, i); // width, height
        i = getBpp(aDisplay, props, i); // bpp
        props[i++] = (int) ( aDisplay.getRefreshRate() * 100.0f ); // Hz * 100
        props[i++] = 0; // flags
        props[i++] = modeIdx; // modeId;
        props[i++] = nrot;
        return MonitorModeProps.streamInMonitorMode(null, cache, props, 0);
    }

    @Override
    protected void collectNativeMonitorModesAndDevicesImpl(final Cache cache) {
        // FIXME: Multi Monitor Implementation missing [for newer Android version ?]

        final Context ctx = jogamp.common.os.android.StaticContext.getContext();
        final WindowManager wmgr = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        final DisplayMetrics outMetrics = new DisplayMetrics();
        final android.view.Display aDisplay = wmgr.getDefaultDisplay();
        aDisplay.getMetrics(outMetrics);

        final int arot = aDisplay.getRotation();
        final int nrot = androidRotation2NewtRotation(arot);

        final int modeIdx=0; // no native modeId in use - use 0
        MonitorMode currentMode = null;
        for(int r=0; r<4; r++) { // for all rotations
            final int nrot_i = r*MonitorMode.ROTATE_90;
            final MonitorMode mode = getModeImpl(cache, aDisplay, outMetrics, modeIdx, 0, nrot_i);
            if( nrot == nrot_i ) {
                currentMode = mode;
            }
        }

        final int[] props = new int[MonitorModeProps.MIN_MONITOR_DEVICE_PROPERTIES - 1 - MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES];
        int i = 0;
        props[i++] = props.length;
        props[i++] = 0; // crt_idx
        props[i++] = 0; // is-clone
        props[i++] = 1; // is-primary
        i = getScreenSizeMM(outMetrics, props, i); // sizeMM
        props[i++] = 0; // rotated viewport x pixel-units
        props[i++] = 0; // rotated viewport y pixel-units
        props[i++] = outMetrics.widthPixels; // rotated viewport width pixel-units
        props[i++] = outMetrics.heightPixels; // rotated viewport height pixel-units
        props[i++] = 0; // rotated viewport x window-units
        props[i++] = 0; // rotated viewport y window-units
        props[i++] = outMetrics.widthPixels; // rotated viewport width window-units
        props[i++] = outMetrics.heightPixels; // rotated viewport height window-units
        MonitorModeProps.streamInMonitorDevice(cache, this, currentMode, null, cache.monitorModes, props, 0, null);
    }

    @Override
    protected MonitorMode queryCurrentMonitorModeImpl(final MonitorDevice monitor) {
        final Context ctx = jogamp.common.os.android.StaticContext.getContext();
        final WindowManager wmgr = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        final DisplayMetrics outMetrics = new DisplayMetrics();
        final android.view.Display aDisplay = wmgr.getDefaultDisplay();
        aDisplay.getMetrics(outMetrics);

        final int currNRot = androidRotation2NewtRotation(aDisplay.getRotation());
        return getModeImpl(null, aDisplay, outMetrics, 0, currNRot, currNRot);
    }

    @Override
    protected boolean setCurrentMonitorModeImpl(final MonitorDevice monitor, final MonitorMode mode) {
        return false;
    }

    //----------------------------------------------------------------------
    // Internals only
    //
    static int androidRotation2NewtRotation(final int arot) {
        switch(arot) {
            case Surface.ROTATION_270: return MonitorMode.ROTATE_270;
            case Surface.ROTATION_180: return MonitorMode.ROTATE_180;
            case Surface.ROTATION_90: return MonitorMode.ROTATE_90;
            case Surface.ROTATION_0:
        }
        return MonitorMode.ROTATE_0;
    }
    static int getScreenSize(final DisplayMetrics outMetrics, final int nrot, final int[] props, int offset) {
        // swap width and height, since Android reflects rotated dimension, we don't
        if (MonitorMode.ROTATE_90 == nrot || MonitorMode.ROTATE_270 == nrot) {
            props[offset++] = outMetrics.heightPixels;
            props[offset++] = outMetrics.widthPixels;
        } else {
            props[offset++] = outMetrics.widthPixels;
            props[offset++] = outMetrics.heightPixels;
        }
        return offset;
    }
    static int getBpp(final android.view.Display aDisplay, final int[] props, int offset) {
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
    static int getScreenSizeMM(final DisplayMetrics outMetrics, final int[] props, int offset) {
        final float inW = outMetrics.widthPixels / outMetrics.xdpi;
        final float inH = outMetrics.heightPixels / outMetrics.ydpi;
        final float mmpi = 25.4f;
        final float mmW = inW * mmpi;
        final float mmH = inH * mmpi;
        if( DEBUG ) {
            System.err.println("Screen A screen "+outMetrics.widthPixels+" x "+outMetrics.heightPixels);
            System.err.println("Screen A xy dpi "+outMetrics.xdpi+" x "+outMetrics.ydpi);
            System.err.println("Screen A densityDPI "+outMetrics.densityDpi);
            System.err.println("Screen A density "+outMetrics.density);
            System.err.println("Screen N xy inch "+inW+" x "+inH);
            System.err.println("Screen N xy mm "+mmW+" x "+mmH);
        }
        props[offset++] = Math.round(mmW);
        props[offset++] = Math.round(mmH);
        return offset;
    }
}


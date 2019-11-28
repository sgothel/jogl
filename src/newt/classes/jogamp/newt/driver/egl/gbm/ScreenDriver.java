/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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
package jogamp.newt.driver.egl.gbm;

import com.jogamp.nativewindow.DefaultGraphicsScreen;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;

import jogamp.nativewindow.drm.DRMUtil;
import jogamp.nativewindow.drm.DrmMode;
import jogamp.newt.MonitorModeProps;
import jogamp.newt.ScreenImpl;

public class ScreenDriver extends ScreenImpl {
    static {
        DisplayDriver.initSingleton();
    }

    public ScreenDriver() {
    }

    @Override
    protected void createNativeImpl() {
        aScreen = new DefaultGraphicsScreen(getDisplay().getGraphicsDevice(), screen_idx);
        drmMode = DrmMode.create(DRMUtil.getDrmFd(), true /* preferNativeMode */);
        if( DEBUG ) {
            drmMode.print(System.err);
        }
    }

    @Override
    protected void closeNativeImpl() {
        drmMode.destroy();
        drmMode = null;
    }

    @Override
    protected int validateScreenIndex(final int idx) {
        // FIXME add multi-monitor support
        /**
        if( 0 <= idx && idx < drmMode.count ) {
            return idx;
        } */
        return 0;
    }

    @Override
    protected void collectNativeMonitorModesAndDevicesImpl(final MonitorModeProps.Cache cache) {
        // FIXME add multi-monitor multi-mode support
        final int scridx = 0; // getIndex();

        int[] props = new int[ MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL ];
        int i = 0;
        props[i++] = MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL;
        props[i++] = drmMode.getModes()[scridx].getHdisplay();
        props[i++] = drmMode.getModes()[scridx].getVdisplay();
        props[i++] = ScreenImpl.default_sm_bpp; // FIXME
        props[i++] = drmMode.getModes()[scridx].getVrefresh() * 100;
        props[i++] = 0; // flags
        props[i++] = 0; // mode_idx
        props[i++] = 0; // rotation
        final MonitorMode currentMode = MonitorModeProps.streamInMonitorMode(null, cache, props, 0);

        props = new int[MonitorModeProps.MIN_MONITOR_DEVICE_PROPERTIES - 1 - MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES];
        i = 0;
        props[i++] = props.length;
        props[i++] = 0; // crt_idx
        props[i++] = 0; // is-clone
        props[i++] = 1; // is-primary
        props[i++] = drmMode.getConnectors()[scridx].getMmWidth();
        props[i++] = drmMode.getConnectors()[scridx].getMmHeight();
        props[i++] = 0; // rotated viewport x pixel-units
        props[i++] = 0; // rotated viewport y pixel-units
        props[i++] = drmMode.getModes()[scridx].getHdisplay(); // rotated viewport width pixel-units
        props[i++] = drmMode.getModes()[scridx].getVdisplay(); // rotated viewport height pixel-units
        props[i++] = 0; // rotated viewport x window-units
        props[i++] = 0; // rotated viewport y window-units
        props[i++] = drmMode.getModes()[scridx].getHdisplay(); // rotated viewport width window-units
        props[i++] = drmMode.getModes()[scridx].getVdisplay(); // rotated viewport height window-units
        MonitorModeProps.streamInMonitorDevice(cache, this, currentMode, null, cache.monitorModes, props, 0, null);
    }

    @Override
    protected MonitorMode queryCurrentMonitorModeImpl(final MonitorDevice monitor) {
        // FIXME add multi-monitor multi-mode support
        return monitor.getSupportedModes().get(0);
    }

    @Override
    protected boolean setCurrentMonitorModeImpl(final MonitorDevice monitor, final MonitorMode mode) {
        // FIXME add multi-monitor multi-mode support
        return false;
    }

    @Override
    protected void calcVirtualScreenOriginAndSize(final Rectangle viewport, final Rectangle viewportInWindowUnits) {
        // FIXME add multi-monitor support
        final int scridx = 0; // getIndex();
        viewport.set(0, 0, drmMode.getModes()[scridx].getHdisplay(), drmMode.getModes()[scridx].getVdisplay());
        viewportInWindowUnits.set(viewport);
    }

    /* pp */ DrmMode drmMode;

    protected static native boolean initIDs();
    protected native void initNative(long drmHandle);
}

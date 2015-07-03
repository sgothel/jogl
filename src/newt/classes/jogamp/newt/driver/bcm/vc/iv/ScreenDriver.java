/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

package jogamp.newt.driver.bcm.vc.iv;

import com.jogamp.nativewindow.DefaultGraphicsScreen;
import com.jogamp.nativewindow.util.Rectangle;

import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;

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
        initNative();
    }

    @Override
    protected void closeNativeImpl() { }

    @Override
    protected int validateScreenIndex(final int idx) {
        return 0; // only one screen available
    }

    @Override
    protected final void collectNativeMonitorModesAndDevicesImpl(final MonitorModeProps.Cache cache) {
        int[] props = new int[ MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL ];
        int i = 0;
        props[i++] = MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL;
        props[i++] = cachedWidth; // width
        props[i++] = cachedHeight; // height
        props[i++] = ScreenImpl.default_sm_bpp; // FIXME
        props[i++] = ScreenImpl.default_sm_rate * 100; // FIXME
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
        props[i++] = ScreenImpl.default_sm_widthmm; // FIXME
        props[i++] = ScreenImpl.default_sm_heightmm; // FIXME
        props[i++] = 0; // rotated viewport x pixel-units
        props[i++] = 0; // rotated viewport y pixel-units
        props[i++] = cachedWidth; // rotated viewport width pixel-units
        props[i++] = cachedWidth; // rotated viewport height pixel-units
        props[i++] = 0; // rotated viewport x window-units
        props[i++] = 0; // rotated viewport y window-units
        props[i++] = cachedWidth; // rotated viewport width window-units
        props[i++] = cachedWidth; // rotated viewport height window-units
        MonitorModeProps.streamInMonitorDevice(cache, this, currentMode, null, cache.monitorModes, props, 0, null);
    }

    @Override
    protected MonitorMode queryCurrentMonitorModeImpl(final MonitorDevice monitor) {
        return monitor.getSupportedModes().get(0);
    }

    @Override
    protected boolean setCurrentMonitorModeImpl(final MonitorDevice monitor, final MonitorMode mode) {
        return false;
    }

    @Override
    protected void calcVirtualScreenOriginAndSize(final Rectangle viewport, final Rectangle viewportInWindowUnits) {
        viewport.set(0, 0, cachedWidth, cachedHeight);
        viewportInWindowUnits.set(viewport);
    }

    /** Called from {@link #initNative()}. */
    protected void setScreenSize(final int width, final int height) {
        cachedWidth = width;   // write to static field intended
        cachedHeight = height; // write to static field intended
    }

    private static int cachedWidth = 0;
    private static int cachedHeight = 0;

    protected static native boolean initIDs();
    protected native void initNative();
}

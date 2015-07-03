/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 */

package jogamp.newt.driver.awt;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;

import jogamp.newt.MonitorModeProps.Cache;
import jogamp.newt.MonitorModeProps;
import jogamp.newt.ScreenImpl;

import com.jogamp.nativewindow.awt.AWTGraphicsDevice;
import com.jogamp.nativewindow.awt.AWTGraphicsScreen;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;

public class ScreenDriver extends ScreenImpl {
    public ScreenDriver() {
    }

    @Override
    protected void createNativeImpl() {
        aScreen = new AWTGraphicsScreen((AWTGraphicsDevice)display.getGraphicsDevice());
    }

    protected void setAWTGraphicsScreen(final AWTGraphicsScreen s) {
        aScreen = s;
    }

    /**
     *  Used by AWTWindow ..
     */
    @Override
    protected void updateVirtualScreenOriginAndSize() {
        super.updateVirtualScreenOriginAndSize();
    }

    @Override
    protected void closeNativeImpl() { }

    @Override
    protected int validateScreenIndex(final int idx) {
        return idx; // pass through ...
    }

    private static MonitorMode getModeProps(final Cache cache, final DisplayMode mode) {
        int rate = mode.getRefreshRate();
        if( DisplayMode.REFRESH_RATE_UNKNOWN == rate ) {
            rate = ScreenImpl.default_sm_rate;
        }
        int bpp = mode.getBitDepth();
        if( DisplayMode.BIT_DEPTH_MULTI == bpp ) {
            bpp= ScreenImpl.default_sm_bpp;
        }
        final int[] props = new int[ MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL ];
        int i = 0;
        props[i++] = MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL;
        props[i++] = mode.getWidth();
        props[i++] = mode.getHeight();
        props[i++] = bpp;
        props[i++] = rate * 100;
        props[i++] = 0; // flags
        props[i++] = 0; // mode_idx
        props[i++] = 0; // rotation
        return MonitorModeProps.streamInMonitorMode(null, cache, props, 0);
    }

    @Override
    protected void collectNativeMonitorModesAndDevicesImpl(final Cache cache) {
        final GraphicsDevice awtGD = ((AWTGraphicsDevice)getDisplay().getGraphicsDevice()).getGraphicsDevice();
        final DisplayMode[] awtModes = awtGD.getDisplayModes();
        for(int i=0; i<awtModes.length; i++) {
            getModeProps(cache, awtModes[i]);
        }
        final MonitorMode currentMode = getModeProps(cache, awtGD.getDisplayMode());

        final int[] props = new int[MonitorModeProps.MIN_MONITOR_DEVICE_PROPERTIES - 1 - MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES];
        int i = 0;
        props[i++] = props.length;
        props[i++] = 0; // crt_idx
        props[i++] = 0; // is-clone
        props[i++] = 1; // is-primary
        props[i++] = ScreenImpl.default_sm_widthmm; // FIXME
        props[i++] = ScreenImpl.default_sm_heightmm; // FIXME
        props[i++] = 0; // rotated viewport x pixel-units
        props[i++] = 0; // rotated viewport y pixel-units
        props[i++] = currentMode.getRotatedWidth(); // rotated viewport width pixel-units
        props[i++] = currentMode.getRotatedHeight(); // rotated viewport height pixel-units
        props[i++] = 0; // rotated viewport x window-units
        props[i++] = 0; // rotated viewport y window-units
        props[i++] = currentMode.getRotatedWidth(); // rotated viewport width window-units
        props[i++] = currentMode.getRotatedHeight(); // rotated viewport height window-units
        MonitorModeProps.streamInMonitorDevice(cache, this, currentMode, null, cache.monitorModes, props, 0, null);
    }

    @Override
    protected MonitorMode queryCurrentMonitorModeImpl(final MonitorDevice monitor) {
        return null;
    }

    @Override
    protected boolean setCurrentMonitorModeImpl(final MonitorDevice monitor, final MonitorMode mode) {
        return false;
    }

}

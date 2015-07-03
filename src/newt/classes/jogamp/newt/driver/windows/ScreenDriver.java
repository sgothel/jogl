/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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
package jogamp.newt.driver.windows;

import com.jogamp.nativewindow.DefaultGraphicsScreen;
import com.jogamp.nativewindow.util.Rectangle;

import jogamp.newt.MonitorModeProps;
import jogamp.newt.ScreenImpl;

import com.jogamp.common.util.ArrayHashSet;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.Screen;

public class ScreenDriver extends ScreenImpl {

    static {
        DisplayDriver.initSingleton();
        if( Screen.DEBUG ) {
            dumpMonitorInfo0();
        }
    }

    public ScreenDriver() {
    }

    @Override
    protected void createNativeImpl() {
        aScreen = new DefaultGraphicsScreen(getDisplay().getGraphicsDevice(), screen_idx);
    }

    @Override
    protected void closeNativeImpl() {
    }

    private final String getAdapterName(final int adapter_idx) {
        return getAdapterName0(adapter_idx);
    }
    private final String getMonitorName(final String adapterName, final int monitor_idx, final boolean onlyActive) {
        return getMonitorName0(adapterName, monitor_idx, onlyActive);
    }

    private final MonitorMode getMonitorModeImpl(final MonitorModeProps.Cache cache, final String adapterName, final int mode_idx) {
        if( null == adapterName ) {
            return null;
        }
        final int[] modeProps = getMonitorMode0(adapterName, mode_idx);
        if ( null == modeProps || 0 >= modeProps.length) {
            return null;
        }
        return MonitorModeProps.streamInMonitorMode(null, cache, modeProps, 0);
    }

    private static final int getMonitorId(final int adapterIdx, final int monitorIdx) {
        if( adapterIdx > 0xff ) {
            throw new InternalError("Unsupported adapter idx > 0xff: "+adapterIdx);
        }
        if( monitorIdx > 0xff ) {
            throw new InternalError("Unsupported monitor idx > 0xff: "+monitorIdx);
        }
        return ( adapterIdx & 0x000000ff ) << 8 | ( monitorIdx & 0x000000ff );
    }
    private static final int getAdapterIndex(final int monitorId) {
        return ( monitorId >>> 8 ) & 0x000000ff;
    }
    private static final int getMonitorIndex(final int monitorId) {
        return monitorId & 0x000000ff;
    }

    @Override
    protected void collectNativeMonitorModesAndDevicesImpl(final MonitorModeProps.Cache cache) {
        ArrayHashSet<MonitorMode> supportedModes =
                new ArrayHashSet<MonitorMode>(false, ArrayHashSet.DEFAULT_INITIAL_CAPACITY, ArrayHashSet.DEFAULT_LOAD_FACTOR);
        String adapterName;
        for(int adapterIdx=0; null != ( adapterName = getAdapterName(adapterIdx) ); adapterIdx++ ) {
            for(int monitorIdx=0; null != getMonitorName(adapterName, monitorIdx, true); monitorIdx++ ) {
                int modeIdx = 0;
                MonitorMode mode;
                do {
                    mode = getMonitorModeImpl(cache, adapterName, modeIdx);
                    if( null != mode ) {
                        supportedModes.getOrAdd(mode);
                        // next mode on same monitor
                        modeIdx++;
                    }
                } while( null != mode);
                if( 0 < modeIdx ) {
                    // has at least one mode -> add device
                    final MonitorMode currentMode = getMonitorModeImpl(cache, adapterName, -1);
                    if ( null != currentMode ) { // enabled
                        final int[] monitorProps = getMonitorDevice0(adapterIdx, monitorIdx, getMonitorId(adapterIdx, monitorIdx));
                        // merge monitor-props + supported modes
                        MonitorModeProps.streamInMonitorDevice(cache, this, currentMode, null, supportedModes, monitorProps, 0, null);

                        // next monitor, 1st mode
                        supportedModes = new ArrayHashSet<MonitorMode>(false, ArrayHashSet.DEFAULT_INITIAL_CAPACITY, ArrayHashSet.DEFAULT_LOAD_FACTOR);
                    }
                }
            }
        }
    }

    @Override
    protected boolean updateNativeMonitorDeviceViewportImpl(final MonitorDevice monitor, final float[] pixelScale, final Rectangle viewportPU, final Rectangle viewportWU) {
        final int monitorId = monitor.getId();
        final int adapterIdx = getAdapterIndex(monitorId);
        final int monitorIdx = getMonitorIndex(monitorId);
        final String adapterName = getAdapterName(adapterIdx);
        if( null != adapterName ) {
            if( null != getMonitorName(adapterName, monitorIdx, true) ) {
                final int[] monitorProps = getMonitorDevice0(adapterIdx, monitorIdx, getMonitorId(adapterIdx, monitorIdx));
                int offset = MonitorModeProps.IDX_MONITOR_DEVICE_VIEWPORT;
                viewportPU.set(monitorProps[offset++], monitorProps[offset++], monitorProps[offset++], monitorProps[offset++]);
                viewportWU.set(monitorProps[offset++], monitorProps[offset++], monitorProps[offset++], monitorProps[offset++]);
                return true;
            }
        }
        return false;
    }

    @Override
    protected MonitorMode queryCurrentMonitorModeImpl(final MonitorDevice monitor) {
        return getMonitorModeImpl(null, getAdapterName(getAdapterIndex(monitor.getId())), -1);
    }

    @Override
    protected boolean setCurrentMonitorModeImpl(final MonitorDevice monitor, final MonitorMode mode)  {
        return setMonitorMode0(getAdapterIndex(monitor.getId()),
                               -1, -1, // no fixed position!
                               mode.getSurfaceSize().getResolution().getWidth(),
                               mode.getSurfaceSize().getResolution().getHeight(),
                               mode.getSurfaceSize().getBitsPerPixel(),
                               (int)mode.getRefreshRate(), // simply cut-off, orig is int
                               mode.getFlags(),
                               mode.getRotation());
    }

    @Override
    protected int validateScreenIndex(final int idx) {
        return 0; // big-desktop w/ multiple monitor attached, only one screen available
    }

    @Override
    protected void calcVirtualScreenOriginAndSize(final Rectangle viewport, final Rectangle viewportInWindowUnits) {
        viewport.set(getVirtualOriginX0(), getVirtualOriginY0(), getVirtualWidthImpl0(), getVirtualHeightImpl0());
        viewportInWindowUnits.set(viewport);
    }

    // Native calls
    private native int getVirtualOriginX0();
    private native int getVirtualOriginY0();
    private native int getVirtualWidthImpl0();
    private native int getVirtualHeightImpl0();

    private static native void dumpMonitorInfo0();
    private native String getAdapterName0(int adapter_idx);
    private native String getMonitorName0(String adapterName, int monitor_idx, boolean onlyActive);
    private native int[] getMonitorMode0(String adapterName, int mode_idx);
    private native int[] getMonitorDevice0(int adapter_idx, int monitor_idx, int monitorId);
    private native boolean setMonitorMode0(int adapter_idx, int x, int y, int width, int height, int bits, int freq, int flags, int rot);
}

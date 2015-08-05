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
package jogamp.newt.driver.x11;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.util.RectangleImmutable;

import jogamp.nativewindow.x11.X11Util;
import jogamp.newt.Debug;
import jogamp.newt.DisplayImpl;
import jogamp.newt.MonitorModeProps;
import jogamp.newt.DisplayImpl.DisplayRunnable;
import jogamp.newt.ScreenImpl;

import com.jogamp.common.util.ArrayHashSet;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsScreen;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;

public class ScreenDriver extends ScreenImpl {
    protected static final boolean DEBUG_TEST_RANDR13_DISABLED;

    static {
        Debug.initSingleton();
        DEBUG_TEST_RANDR13_DISABLED = PropertyAccess.isPropertyDefined("newt.test.Screen.disableRandR13", true);

        DisplayDriver.initSingleton();
    }

    /** Ensure static init has been run. */
    /* pp */static void initSingleton() { }

    public ScreenDriver() {
    }

    @Override
    protected void createNativeImpl() {
        // validate screen index
        final Long handle = runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Long>() {
            @Override
            public Long run(final long dpy) {
                return Long.valueOf(GetScreen0(dpy, screen_idx));
            } } );
        if (handle.longValue() == 0) {
            throw new RuntimeException("Error creating screen: " + screen_idx);
        }
        final X11GraphicsDevice x11dev = (X11GraphicsDevice) getDisplay().getGraphicsDevice();
        final long dpy = x11dev.getHandle();
        aScreen = new X11GraphicsScreen(x11dev, screen_idx);
        {
            final int v[] = getRandRVersion0(dpy);
            randrVersion = new VersionNumber(v[0], v[1], 0);
        }
        {
            if( !DEBUG_TEST_RANDR13_DISABLED && randrVersion.compareTo(RandR.version130) >= 0 ) {
                rAndR = new RandR13(randrVersion);
            } else if( randrVersion.compareTo(RandR.version110) >= 0 ) {
                rAndR = new RandR11(randrVersion);
            } else {
                rAndR = null;
            }
        }
        ((DisplayDriver)display).registerRandR(rAndR);
        if( DEBUG ) {
            System.err.println("Using "+rAndR);
            rAndR.dumpInfo(dpy, screen_idx);
        }
    }

    @Override
    protected void closeNativeImpl() {
    }

    private VersionNumber randrVersion;
    private RandR rAndR;

    @Override
    protected final void collectNativeMonitorModesAndDevicesImpl(final MonitorModeProps.Cache cache) {
        if( null == rAndR ) { return; }
        final AbstractGraphicsDevice device = getDisplay().getGraphicsDevice();
        device.lock();
        try {
            if( rAndR.beginInitialQuery(device.getHandle(), this) ) {
                try {
                    final int[] crt_ids = rAndR.getMonitorDeviceIds(device.getHandle(), this);
                    final int crtCount = null != crt_ids ? crt_ids.length : 0;

                    // Gather all available rotations
                    final ArrayHashSet<Integer> availableRotations = new ArrayHashSet<Integer>(false, ArrayHashSet.DEFAULT_INITIAL_CAPACITY, ArrayHashSet.DEFAULT_LOAD_FACTOR);
                    for(int i = 0; i < crtCount; i++) {
                        final int[] rotations = rAndR.getAvailableRotations(device.getHandle(), this, crt_ids[i]);
                        if( null != rotations ) {
                            final List<Integer> rotationList = new ArrayList<Integer>(rotations.length);
                            for(int j=0; j<rotations.length; j++ ) { rotationList.add(rotations[j]); }
                            availableRotations.addAll(rotationList);
                        }
                    }

                    // collect all modes, while injecting all available rotations
                    {
                        int modeIdx = 0;
                        int[] props;
                        do {
                            props = rAndR.getMonitorModeProps(device.getHandle(), this, modeIdx++);
                            if( null != props ) {
                                for(int i = 0; i < availableRotations.size(); i++) {
                                    props[MonitorModeProps.IDX_MONITOR_MODE_ROT] = availableRotations.get(i);
                                    MonitorModeProps.streamInMonitorMode(null, cache, props, 0);
                                }
                            }
                        } while( null != props);
                    }
                    if( cache.monitorModes.size() > 0 ) {
                        for(int i = 0; i < crtCount; i++) {
                            final int[] monitorProps = rAndR.getMonitorDeviceProps(device.getHandle(), this, cache, crt_ids[i]);
                            if( null != monitorProps &&
                                MonitorModeProps.MIN_MONITOR_DEVICE_PROPERTIES <= monitorProps[0] && // Enabled ? I.e. contains active modes ?
                                MonitorModeProps.MIN_MONITOR_DEVICE_PROPERTIES <= monitorProps.length ) {
                                MonitorModeProps.streamInMonitorDevice(cache, this, null, monitorProps, 0, null);
                            }
                        }
                    }
                } finally {
                    rAndR.endInitialQuery(device.getHandle(), this);
                }
            }
        } finally {
            device.unlock();
        }
    }

    @Override
    protected boolean updateNativeMonitorDeviceViewportImpl(final MonitorDevice monitor, final float[] pixelScale, final Rectangle viewportPU, final Rectangle viewportWU) {
        final AbstractGraphicsDevice device = getDisplay().getGraphicsDevice();
        device.lock();
        try {
            final int[] viewportProps = rAndR.getMonitorDeviceViewport(device.getHandle(), this, monitor.getId());
            if( null != viewportProps ) {
                viewportPU.set(viewportProps[0], viewportProps[1], viewportProps[2], viewportProps[3]);
                viewportWU.set(viewportProps[0], viewportProps[1], viewportProps[2], viewportProps[3]); // equal window-units and pixel-units
                return true;
            } else {
                return false;
            }
        } finally {
            device.unlock();
        }
    }

    @Override
    protected MonitorMode queryCurrentMonitorModeImpl(final MonitorDevice monitor) {
        if( null == rAndR ) { return null; }

        return runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<MonitorMode>() {
            @Override
            public MonitorMode run(final long dpy) {
                final int[] currentModeProps = rAndR.getCurrentMonitorModeProps(dpy, ScreenDriver.this, monitor.getId());
                if( null != currentModeProps ) {
                    return MonitorModeProps.streamInMonitorMode(null, null, currentModeProps, 0);
                } else {
                    return null;
                }
            } } );
    }

    @Override
    protected boolean setCurrentMonitorModeImpl(final MonitorDevice monitor, final MonitorMode mode) {
        if( null == rAndR ) { return false; }

        final long t0 = System.currentTimeMillis();
        final boolean started = runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Boolean>() {
            @Override
            public Boolean run(final long dpy) {
                return Boolean.valueOf( rAndR.setCurrentMonitorModeStart(dpy, ScreenDriver.this, monitor, mode) );
            }
        }).booleanValue();
        final boolean done;
        if( started ) {
            done = rAndR.setCurrentMonitorModeWait(this);
        } else {
            done = false;
        }
        if(DEBUG || !done) {
            System.err.println("X11Screen.setCurrentMonitorModeImpl: "+(done?" OK":"NOK")+" (started "+started+"): t/TO "+
                               (System.currentTimeMillis()-t0)+"/"+SCREEN_MODE_CHANGE_TIMEOUT+"ms; "+monitor.getCurrentMode()+" -> "+mode);
        }
        return done;
    }

    private final DisplayImpl.DisplayRunnable<Boolean> xineramaEnabledQueryWithTemp = new DisplayImpl.DisplayRunnable<Boolean>() {
        @Override
        public Boolean run(final long dpy) {
            return Boolean.valueOf(X11Util.XineramaIsEnabled(dpy));
        } };

    @Override
    protected int validateScreenIndex(final int idx) {
        final DisplayDriver x11Display = (DisplayDriver) getDisplay();
        final Boolean r = x11Display.isXineramaEnabled();
        if( null != r ) {
            return r.booleanValue() ? 0 : idx;
        } else {
            return runWithTempDisplayHandle( xineramaEnabledQueryWithTemp ).booleanValue() ? 0 : idx;
        }
    }

    @Override
    protected void calcVirtualScreenOriginAndSize(final Rectangle viewport, final Rectangle viewportInWindowUnits) {
        final RectangleImmutable ov = DEBUG ? (RectangleImmutable) getViewport().cloneMutable() : null;
        /**
        if( null != rAndR && rAndR.getVersion().compareTo(RandR.version130) >= 0 && getMonitorDevices().size()>0 ) {
            super.calcVirtualScreenOriginAndSize(vOriginSize);
            if( DEBUG ) {
                System.err.println("X11Screen.calcVirtualScreenOriginAndSize: UpdatingViewport "+ov+" -> "+vOriginSize);
            }
            runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Object>() {
                public Object run(long dpy) {
                    rAndR.updateScreenViewport(dpy, ScreenDriver.this, vOriginSize);
                    return null;
                } } );
        } else */ {
            runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Object>() {
                @Override
                public Object run(final long dpy) {
                    viewport.set(0, 0, getWidth0(dpy, screen_idx), getHeight0(dpy, screen_idx));
                    viewportInWindowUnits.set(viewport);
                    return null;
                } } );
            if( DEBUG ) {
                System.err.println("X11Screen.calcVirtualScreenOriginAndSize: Querying X11: "+ov+" -> "+viewport);
            }
        }
    }

    //----------------------------------------------------------------------
    // Internals only
    //
    private final <T> T runWithLockedDisplayDevice(final DisplayRunnable<T> action) {
        return display.runWithLockedDisplayDevice(action);
    }

    private final <T> T runWithTempDisplayHandle(final DisplayRunnable<T> action) {
        final long displayHandle = X11Util.openDisplay(display.getName());
        if(0 == displayHandle) {
            throw new RuntimeException("null device");
        }
        T res;
        try {
            res = action.run(displayHandle);
        } finally {
            X11Util.closeDisplay(displayHandle);
        }
        return res;
    }

    private static native long GetScreen0(long dpy, int scrn_idx);

    private static native int getWidth0(long display, int scrn_idx);

    private static native int getHeight0(long display, int scrn_idx);

    private static native int[] getRandRVersion0(long display);
}

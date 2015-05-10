package jogamp.newt.driver.gbm;

import com.jogamp.nativewindow.DefaultGraphicsScreen;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;
import jogamp.newt.MonitorModeProps;
import jogamp.newt.ScreenImpl;

public class ScreenDriver extends ScreenImpl {


    @Override
    protected void createNativeImpl() {
        this.aScreen = new DefaultGraphicsScreen(getDisplay().getGraphicsDevice(),
                                                 this.screen_idx);
    }

    @Override
    protected void closeNativeImpl() {
    }

    @Override
    protected int validateScreenIndex(final int idx) {
        return 0;
    }

    @Override
    protected void collectNativeMonitorModesAndDevicesImpl(final MonitorModeProps.Cache cache) {
//        DisplayDriver display = (DisplayDriver) getDisplay();
//        final drmModeConnector connector = display.getConnector();
//        final drmModeEncoder encoder = display.getEncoder();
//        //TODO collect info from init method
//        int[] props = new int[MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL];
//        int   i     = 0;
//        props[i++] = MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL;
//        props[i++] = connector.modes.hdisplay; // width
//        props[i++] = connector.modes.vdisplay; // height
//        props[i++] = ScreenImpl.default_sm_bpp; // FIXME
//        props[i++] = ScreenImpl.default_sm_rate * 100; // FIXME
//        props[i++] = connector.modes.flags; // flags
//        props[i++] = 0; // mode_idx
//        props[i++] = 0; // rotation
//        final MonitorMode currentMode = MonitorModeProps.streamInMonitorMode(null,
//                cache,
//                props,
//                0);
//
//        props = new int[MonitorModeProps.MIN_MONITOR_DEVICE_PROPERTIES - 1 - MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES];
//        i = 0;
//        props[i++] = props.length;
//        props[i++] = encoder.crtc_id; // crt_idx
//        props[i++] = 0; // is-clone
//        props[i++] = 1; // is-primary
//        props[i++] = ScreenImpl.default_sm_widthmm; // FIXME
//        props[i++] = ScreenImpl.default_sm_heightmm; // FIXME
//        props[i++] = 0; // rotated viewport x pixel-units
//        props[i++] = 0; // rotated viewport y pixel-units
//        props[i++] = connector.modes.hdisplay; // rotated viewport width pixel-units
//        props[i++] = connector.modes.vdisplay; // rotated viewport height pixel-units
//        props[i++] = 0; // rotated viewport x window-units
//        props[i++] = 0; // rotated viewport y window-units
//        props[i++] = connector.modes.hdisplay; // rotated viewport width window-units
//        props[i++] = connector.modes.vdisplay; // rotated viewport height window-units
//        MonitorModeProps.streamInMonitorDevice(cache,
//                this,
//                currentMode,
//                null,
//                cache.monitorModes,
//                props,
//                0,
//                null);
    }

    @Override
    protected MonitorMode queryCurrentMonitorModeImpl(final MonitorDevice monitor) {
        //TODO collect info from init method

        return null;
    }

    @Override
    protected boolean setCurrentMonitorModeImpl(final MonitorDevice monitor,
                                                final MonitorMode mode) {
        //TODO collect info from init method

        return false;
    }
}

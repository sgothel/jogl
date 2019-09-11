//Copyright 2015 Erik De Rijcke
//
//Licensed under the Apache License,Version2.0(the"License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,software
//distributed under the License is distributed on an"AS IS"BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
package jogamp.newt.driver.gbm;

import com.jogamp.nativewindow.*;
import com.jogamp.nativewindow.util.Insets;
import com.jogamp.nativewindow.util.Point;
import jogamp.newt.WindowImpl;
import jogamp.newt.driver.linux.LinuxEventDeviceTracker;
import jogamp.newt.driver.linux.LinuxMouseTracker;

public class WindowDriver extends WindowImpl {

    private final LinuxMouseTracker linuxMouseTracker;
    private final LinuxEventDeviceTracker linuxEventDeviceTracker;

    public WindowDriver() {
        this.linuxMouseTracker = LinuxMouseTracker.getSingleton();
        this.linuxEventDeviceTracker = LinuxEventDeviceTracker.getSingleton();
    }


    @Override
    protected final int getSupportedReconfigMaskImpl() {
        return minimumReconfigStateMask;
    }

    @Override
    protected void createNativeImpl() {
        if (0 != getParentWindowHandle()) {
            throw new RuntimeException("Window parenting not supported (yet)");
        }

        final ScreenDriver screen = (ScreenDriver) getScreen();
        final DisplayDriver display = (DisplayDriver) screen.getDisplay();

        // Create own screen/device resource instance allowing independent ownership,
        // while still utilizing shared EGL resources.
        final AbstractGraphicsScreen aScreen = screen.getGraphicsScreen();
        final AbstractGraphicsDevice aDevice = display.getGraphicsDevice();
        final DefaultGraphicsScreen eglScreen = new DefaultGraphicsScreen(aDevice, aScreen.getIndex());

        final AbstractGraphicsConfiguration cfg = GraphicsConfigurationFactory.getFactory(getScreen().getDisplay().getGraphicsDevice(), capsRequested).chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, eglScreen, VisualIDHolder.VID_UNDEFINED);
        if (null == cfg) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        final Capabilities chosenCaps = (Capabilities) cfg.getChosenCapabilities();
        // FIXME: Pass along opaque flag, since EGL doesn't determine it
        if(capsRequested.isBackgroundOpaque() != chosenCaps.isBackgroundOpaque()) {
            chosenCaps.setBackgroundOpaque(capsRequested.isBackgroundOpaque());
        }
        setGraphicsConfiguration(cfg);
        long nativeWindowHandle = createSurface();
        if (nativeWindowHandle == 0) {
            throw new NativeWindowException("Error creating egl window: "+cfg);
        }
        setWindowHandle(nativeWindowHandle);
        if (0 == getWindowHandle()) {
            throw new NativeWindowException("Error native Window Handle is null");
        }

        addWindowListener(linuxEventDeviceTracker);
        addWindowListener(linuxMouseTracker);
        focusChanged(false, true);
    }

    private native long createSurface();
    //{
        //        surface = GbmLibrary.INSTANCE.gbm_surface_create(dev,
//                                                         mode.hdisplay, mode.vdisplay,
//                                                         GbmLibrary.Constants.GBM_FORMAT_XRGB8888,
//                                                         GBM_BO.GBM_BO_USE_SCANOUT | GBM_BO.GBM_BO_USE_RENDERING);
//        if (surface == null) {
//            throw new NativeWindowException("failed to create gbm surface");
//        }
//
//        return 0;
    //}

    @Override
    protected void closeNativeImpl() {
        removeWindowListener(this.linuxMouseTracker);
        removeWindowListener(this.linuxEventDeviceTracker);
    }

    @Override
    protected void requestFocusImpl(final boolean force) {
        focusChanged(false,
                     true);
    }

    @Override
    protected boolean reconfigureWindowImpl(final int x,
                                            final int y,
                                            final int width,
                                            final int height,
                                            final int flags) {
        return false;
    }

    @Override
    protected Point getLocationOnScreenImpl(final int x,
                                            final int y) {
        return new Point(x,
                         y);
    }

}

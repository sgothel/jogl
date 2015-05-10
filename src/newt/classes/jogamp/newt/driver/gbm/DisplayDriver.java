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

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.GLDrawableFactory;
import jogamp.newt.DisplayImpl;
import jogamp.newt.NEWTJNILibLoader;
import jogamp.opengl.egl.EGLDisplayUtil;

public class DisplayDriver extends DisplayImpl {

    static {
        NEWTJNILibLoader.loadNEWT();

        if (!DisplayDriver.initIDs()) {
            throw new NativeWindowException("Failed to initialize gbm Display jmethodIDs");
        }
//        if (!ScreenDriver.initIDs()) {
//            throw new NativeWindowException("Failed to initialize gbm Screen jmethodIDs");
//        }
//        if (!WindowDriver.initIDs()) {
//            throw new NativeWindowException("Failed to initialize gbm Window jmethodIDs");
//        }
    }

    protected static native boolean initIDs();

    private long dev;
    private long surface;

    private int fd;
    private long mode;

    static {
        NEWTJNILibLoader.loadNEWT();
        GLDrawableFactory.initSingleton();
    }

    //private final EGLGraphicsDevice.EGLDisplayLifecycleCallback eglDisplayLifecycleCallback;
    private long connector;
    private long encoder;

    public long getConnector() {
        return connector;
    }

    public long getEncoder() {
        return encoder;
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }

    @Override
    protected void createNativeImpl() {
        final EGLGraphicsDevice eglGraphicsDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(dev, AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);
        eglGraphicsDevice.open();

        this.aDevice = eglGraphicsDevice;
    }

    private native void initGbm();
//    {
//
//
//    }

    public long getSurface() {
        return surface;
    }

    private native void init();
//    {
//
//        String[] modules = {
//                "i915", "radeon", "nouveau", "vmwgfx", "omapdrm", "exynos", "msm"
//        };
//        drmModeRes resources;
//        connector = null;
//        encoder = null;
//
//        for (int i = 0; i < modules.length; i++) {
//            if(DEBUG){
//                System.out.println(String.format("trying to load module %s...", modules[i]));
//            }
//            fd = DrmLibrary.INSTANCE.drmOpen(modules[i], null);
//            if (fd < 0) {
//                throw new NativeWindowException("Can not open drm device.");
//            } else {
//                break;
//            }
//        }
//
//        if (fd < 0) {
//            throw new NativeWindowException("could not open drm device");
//        }
//
//        resources = DrmLibrary.INSTANCE.drmModeGetResources(fd);
//        if (resources == null) {
//            throw new NativeWindowException("drmModeGetResources failed");
//        }
//
//	/* find a connected connector: */
//        for (int i = 0; i < resources.count_connectors; i++) {
//            connector = DrmLibrary.INSTANCE.drmModeGetConnector(fd, resources.connectors.getInt(i));
//            if (connector.connection == drmModeConnection.DRM_MODE_CONNECTED) {
//                        /* it's connected, let's use this! */
//                break;
//            }
//            DrmLibrary.INSTANCE.drmModeFreeConnector(connector);
//            connector = null;
//        }
//
//        if (connector == null) {
//                /* we could be fancy and listen for hotplug events and wait for
//                 * a connector..
//		 */
//            throw new NativeWindowException("no connected connector!");
//        }
//
//	/* find highest resolution mode: */
//        drmModeModeInfo[] drmModeModeInfos = (drmModeModeInfo[]) connector.modes.toArray(connector.count_modes);
//        for (int i = 0, area = 0; i < drmModeModeInfos.length; i++) {
//            drmModeModeInfo current_mode = drmModeModeInfos[i];
//            int current_area = current_mode.hdisplay * current_mode.vdisplay;
//            if (current_area > area) {
//                mode = current_mode;
//                area = current_area;
//            }
//        }
//
//        if (mode == null) {
//            throw new NativeWindowException("could not find mode!");
//        }
//
//	/* find encoder: */
//        for (int i = 0; i < resources.count_encoders; i++) {
//            encoder = DrmLibrary.INSTANCE.drmModeGetEncoder(fd, resources.encoders.getInt(i));
//            if (encoder.encoder_id == connector.encoder_id) {
//                break;
//            }
//            DrmLibrary.INSTANCE.drmModeFreeEncoder(encoder);
//            encoder = null;
//        }
//
//        if (encoder == null) {
//            throw new NativeWindowException("no encoder!");
//        }
//
//        final int crtc_id = encoder.crtc_id;
//        final int connector_id = connector.connector_id;
//
//        dev = GbmLibrary.INSTANCE.gbm_create_device(fd);
//    }

    @Override
    protected void closeNativeImpl(final AbstractGraphicsDevice aDevice) {
        //DrmLibrary.INSTANCE.drmModeFreeConnector(this.connector);
        aDevice.close();
        destroyDisplay();
        //GbmLibrary.INSTANCE.gbm_device_destroy(dev);
        this.dev = 0;
        //CLibrary.INSTANCE.close(this.fd);
        this.fd = 0;
    }

    private native void destroyDisplay();

    @Override
    protected void dispatchMessagesNative() {
        //NA
    }

    public long getGbmDevice() {
        return dev;
    }
}

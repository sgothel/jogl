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
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import jogamp.newt.DisplayImpl;
import jogamp.newt.NEWTJNILibLoader;
import jogamp.opengl.egl.EGLDisplayUtil;

public class DisplayDriver extends DisplayImpl {

    static {
        NEWTJNILibLoader.loadNEWT();
    }


    public static void initSingleton() {
        // just exist to ensure static init has been run
    }

    @Override
    protected void createNativeImpl() {
        final EGLGraphicsDevice eglGraphicsDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(initGbm(), AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);
        eglGraphicsDevice.open();

        this.aDevice = eglGraphicsDevice;
    }

    private native long initGbm();
//    {
//
//
//    }

    @Override
    protected void closeNativeImpl(final AbstractGraphicsDevice aDevice) {
        //DrmLibrary.INSTANCE.drmModeFreeConnector(this.connector);
        aDevice.close();
        destroyDisplay();
        //GbmLibrary.INSTANCE.gbm_device_destroy(dev);
        //CLibrary.INSTANCE.close(this.fd);
    }

    private native void destroyDisplay();

    @Override
    protected void dispatchMessagesNative() {
        //NA
    }
}

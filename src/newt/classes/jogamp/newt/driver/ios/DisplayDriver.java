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

package jogamp.newt.driver.ios;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.ios.IOSGraphicsDevice;
import com.jogamp.nativewindow.util.PixelFormat;

import jogamp.newt.DisplayImpl;
import jogamp.newt.NEWTJNILibLoader;

public class DisplayDriver extends DisplayImpl {

    static {
        NEWTJNILibLoader.loadNEWTHead();

        if(!initUIApplication0()) {
            throw new NativeWindowException("Failed to initialize native Application hook");
        }
        if(!WindowDriver.initIDs0()) {
            throw new NativeWindowException("Failed to initialize jmethodIDs");
        }
        if(DEBUG) {
            System.err.println("MacDisplay.init App and IDs OK "+Thread.currentThread().getName());
        }
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }

    public DisplayDriver() {
    }

    @Override
    public PixelFormat getNativePointerIconPixelFormat() { return PixelFormat.RGBA8888; }

    @Override
    protected void dispatchMessagesNative() {
        // nop
    }

    @Override
    protected void createNativeImpl() {
        aDevice = new IOSGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
    }

    @Override
    protected void closeNativeImpl(final AbstractGraphicsDevice aDevice) {
        aDevice.close();
    }

    /**
     * {@inheritDoc}
     * <p>
     * NOTE: MUST BE DIRECT BUFFER, since NSBitmapImageRep uses buffer directly!
     * </p>
     */
    @Override
    public final boolean getNativePointerIconForceDirectNIO() { return true; }

    private static native boolean initUIApplication0();
}


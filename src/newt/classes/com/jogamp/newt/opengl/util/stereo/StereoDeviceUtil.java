/**
 * Copyright 2015 JogAmp Community. All rights reserved.
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
package com.jogamp.newt.opengl.util.stereo;

import java.util.List;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.PointImmutable;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.util.RectangleImmutable;

import com.jogamp.newt.Display;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.util.MonitorModeUtil;
import com.jogamp.opengl.util.stereo.StereoDevice;

/**
 * {@link StereoDevice} NEWT related utilities.
 */
public class StereoDeviceUtil {
    /**
     * Returns the {@link StereoDevice}'s associated {@link MonitorDevice} or {@code null}, if none is attached.
     * <p>
     * The returned {@link MonitorDevice}'s {@link Screen}, retrieved via {@link MonitorDevice#getScreen()},
     * has been natively created via {@link Screen#addReference()} and caller shall ensure
     * {@link Screen#removeReference()} will be called when no more in use.
     * </p>
     * <p>
     * If {@code adjustRotation} is {@code true} and the {@link StereoDevice}
     * {@link StereoDevice#getRequiredRotation() requires rotation}, the {@link MonitorDevice}
     * will be rotated.
     * </p>
     * @param stereoDevice the {@link StereoDevice}
     * @param adjustRotation if {@code true} rotate the {@link MonitorDevice} if {@link StereoDevice#getRequiredRotation() required}.
     */
    public static MonitorDevice getMonitorDevice(final StereoDevice stereoDevice, final boolean adjustRotation) {
        final PointImmutable devicePos = stereoDevice.getPosition();
        final DimensionImmutable deviceRes = stereoDevice.getSurfaceSize();
        final int deviceReqRotation = stereoDevice.getRequiredRotation();
        final RectangleImmutable rect = new Rectangle(devicePos.getX(), devicePos.getY(), 128, 128);

        final Display display = NewtFactory.createDisplay(null);
        final Screen screen = NewtFactory.createScreen(display, 0);
        screen.addReference();
        final MonitorDevice monitor = screen.getMainMonitor(rect);
        System.err.println("StereoDevice Monitor: "+monitor);
        final MonitorMode currentMode = monitor.getCurrentMode();
        if( adjustRotation && deviceReqRotation != currentMode.getRotation() ) {
            System.err.println("StereoDevice Current Mode: "+currentMode+", requires rotation: "+deviceReqRotation);
            final DimensionImmutable deviceRotRes;
            if( 90 == deviceReqRotation || 270 == deviceReqRotation ) {
                deviceRotRes = new Dimension(deviceRes.getHeight(), deviceRes.getWidth());
            } else {
                deviceRotRes = deviceRes;
            }
            final List<MonitorMode> mmodes0 = monitor.getSupportedModes();
            final List<MonitorMode> mmodes1 = MonitorModeUtil.filterByResolution(mmodes0, deviceRotRes);
            final List<MonitorMode> mmodes2 = MonitorModeUtil.filterByRotation(mmodes1, deviceReqRotation);
            if( mmodes2.size() > 0 ) {
                final MonitorMode newMode = mmodes2.get(0);
                System.err.println("StereoDevice Set Mode: "+newMode);
                monitor.setCurrentMode(newMode);
            }
            final MonitorMode queriedMode = monitor.queryCurrentMode();
            System.err.println("StereoDevice Post-Set Mode: "+queriedMode);
        } else {
            System.err.println("StereoDevice Keeps Mode: "+currentMode);
        }
        return monitor;
    }
}

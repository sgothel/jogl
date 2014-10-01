/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package jogamp.opengl.oculusvr;

import com.jogamp.oculusvr.OVR;
import com.jogamp.oculusvr.OVRVersion;
import com.jogamp.oculusvr.OvrHmdContext;
import com.jogamp.opengl.util.stereo.StereoDevice;
import com.jogamp.opengl.util.stereo.StereoDevice.Config;
import com.jogamp.opengl.util.stereo.StereoDeviceFactory;

public class OVRStereoDeviceFactory extends StereoDeviceFactory {

    public static boolean isAvailable() {
        if( OVR.ovr_Initialize() ) { // recursive ..
            return 0 < OVR.ovrHmd_Detect();
        }
        return false;
    }

    @Override
    public final StereoDevice createDevice(final int deviceIndex, final Config config, final boolean verbose) {
        final OvrHmdContext hmdCtx = OVR.ovrHmd_Create(deviceIndex);
        if( null == hmdCtx ) {
            if( verbose ) {
                System.err.println("Failed to create hmdCtx for device index "+deviceIndex+" on thread "+Thread.currentThread().getName());
                Thread.dumpStack();
            }
            return null;
        }
        final OVRStereoDevice ctx = new OVRStereoDevice(this, hmdCtx, deviceIndex);
        if( verbose ) {
            System.err.println(OVRVersion.getAvailableCapabilitiesInfo(ctx.hmdDesc, deviceIndex, null).toString());
        }
        return ctx;
    }
}

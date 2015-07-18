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
import com.jogamp.oculusvr.ovrHmdDesc;
import com.jogamp.opengl.util.stereo.StereoDeviceConfig;
import com.jogamp.opengl.util.stereo.StereoDevice;
import com.jogamp.opengl.util.stereo.StereoDeviceFactory;

public class OVRStereoDeviceFactory extends StereoDeviceFactory {
    public static boolean isAvailable() {
        if( OVR.ovr_Initialize(OVRVersion.defaultInitParams) ) { // recursive ..
            boolean res = false;
            final int count = OVR.ovrHmd_Detect();
            if( StereoDevice.DEBUG ) {
                System.err.println("Detect.0: ovrHmd_Detect() -> "+count);
            }
            if( 0 < count ) {
                res = true;
            } else {
                // SDK 0.4.4 w/ DK1 (Linux): ovrHmd_Detect() returns zero!
                final ovrHmdDesc hmdDesc = OVR.ovrHmd_Create(0);
                if( null != hmdDesc ) {
                    final OvrHmdContext ctx = hmdDesc.getHandle();
                    if( null != ctx ) {
                        res = true;
                        System.err.println("Detect.1: hmdDesc: "+hmdDesc.getProductNameAsString());
                    }
                    OVR.ovrHmd_Destroy(hmdDesc);
                } else {
                    if( StereoDevice.DEBUG ) {
                        System.err.println("Detect.1: hmdDesc: null");
                    }
                }
            }
            return res;
        } else {
            System.err.println("ovr_Initialize() failed");
        }
        return false;
    }

    private boolean isValid = true;

    private void dumpCaps(final ovrHmdDesc hmdDesc, final int deviceIndex) {
        System.err.println(OVRVersion.getAvailableCapabilitiesInfo(hmdDesc, deviceIndex, null).toString());
    }

    @Override
    protected final StereoDevice createDeviceImpl(final int deviceIndex, final StereoDeviceConfig config, final boolean verbose) {
        final ovrHmdDesc hmdDesc = OVR.ovrHmd_Create(deviceIndex);
        if( null == hmdDesc ) {
            if( verbose ) {
                System.err.println("Failed to create hmdCtx for device index "+deviceIndex+" on thread "+Thread.currentThread().getName());
                Thread.dumpStack();
            }
            return null;
        }
        final int hmdCaps = hmdDesc.getHmdCaps();
        if( 0 == ( hmdCaps & OVR.ovrHmdCap_ExtendDesktop ) ) {
            System.err.println("Device "+deviceIndex+" is not in ExtendDesktop mode as required.");
            dumpCaps(hmdDesc, deviceIndex);
            return null;
        }
        if( verbose ) {
            dumpCaps(hmdDesc, deviceIndex);
        }
        return new OVRStereoDevice(this, hmdDesc, deviceIndex);
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public final void shutdown() {
        if( isValid ) {
            OVR.ovr_Shutdown();
            isValid = false;
        }
    }
}

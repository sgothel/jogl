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

import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.PointImmutable;
import javax.media.nativewindow.util.Rectangle;
import javax.media.nativewindow.util.RectangleImmutable;

import com.jogamp.oculusvr.OVR;
import com.jogamp.oculusvr.OvrHmdContext;
import com.jogamp.oculusvr.ovrEyeRenderDesc;
import com.jogamp.oculusvr.ovrFovPort;
import com.jogamp.oculusvr.ovrHmdDesc;
import com.jogamp.oculusvr.ovrSizei;
import com.jogamp.opengl.math.FovHVHalves;
import com.jogamp.opengl.util.stereo.StereoDevice;
import com.jogamp.opengl.util.stereo.StereoDeviceRenderer;

public class OVRStereoDevice implements StereoDevice {
    public final OvrHmdContext handle;
    public final int deviceIndex;
    public final ovrHmdDesc hmdDesc;

    private boolean sensorsStarted = false;

    public OVRStereoDevice(final OvrHmdContext nativeContext, final int deviceIndex) {
        this.handle = nativeContext;
        this.deviceIndex = deviceIndex;
        this.hmdDesc = ovrHmdDesc.create();
        OVR.ovrHmd_GetDesc(handle, hmdDesc);
    }

    @Override
    public final void dispose() {
        // NOP
    }

    @Override
    public final PointImmutable getPosition() {
        return OVRUtil.getVec2iAsPoint(hmdDesc.getWindowsPos());
    }

    @Override
    public final DimensionImmutable getSurfaceSize() {
        return OVRUtil.getOVRSizei(hmdDesc.getResolution());
    }

    @Override
    public final FovHVHalves[] getDefaultFOV() {
        final ovrFovPort[] defaultEyeFov = hmdDesc.getDefaultEyeFov(0, new ovrFovPort[2]);
        final FovHVHalves[] eyeFov = new FovHVHalves[2];
        eyeFov[0] = OVRUtil.getFovHV(defaultEyeFov[0]);
        eyeFov[1] = OVRUtil.getFovHV(defaultEyeFov[1]);
        return eyeFov;
    }

    @Override
    public final boolean startSensors(final boolean start) {
        if( start && !sensorsStarted ) {
            // Start the sensor which provides the Rift’s pose and motion.
            final int requiredSensorCaps = 0;
            final int supportedSensorCaps = requiredSensorCaps | OVR.ovrSensorCap_Orientation | OVR.ovrSensorCap_YawCorrection | OVR.ovrSensorCap_Position;
            if( OVR.ovrHmd_StartSensor(handle, supportedSensorCaps, requiredSensorCaps) ) {
                sensorsStarted = true;
                return true;
            } else {
                sensorsStarted = false;
                return false;
            }
        } else if( sensorsStarted ) {
            OVR.ovrHmd_StopSensor(handle);
            sensorsStarted = false;
            return true;
        } else {
            // No state change -> Success
            return true;
        }
    }
    @Override
    public boolean getSensorsStarted() { return sensorsStarted; }

    @Override
    public final StereoDeviceRenderer createRenderer(final int distortionBits,
                                                     final int textureCount, final float[] eyePositionOffset,
                                                     final FovHVHalves[] eyeFov, final float pixelsPerDisplayPixel, final int textureUnit) {
        final ovrFovPort ovrEyeFov0 = OVRUtil.getOVRFovPort(eyeFov[0]);
        final ovrFovPort ovrEyeFov1 = OVRUtil.getOVRFovPort(eyeFov[1]);

        final ovrEyeRenderDesc[] eyeRenderDesc = new ovrEyeRenderDesc[2];
        eyeRenderDesc[0] = OVR.ovrHmd_GetRenderDesc(handle, OVR.ovrEye_Left, ovrEyeFov0);
        eyeRenderDesc[1] = OVR.ovrHmd_GetRenderDesc(handle, OVR.ovrEye_Right, ovrEyeFov1);
        if( StereoDevice.DEBUG ) {
            System.err.println("XXX: eyeRenderDesc[0] "+OVRUtil.toString(eyeRenderDesc[0]));
            System.err.println("XXX: eyeRenderDesc[1] "+OVRUtil.toString(eyeRenderDesc[1]));
        }

        final ovrSizei recommenedTex0Size = OVR.ovrHmd_GetFovTextureSize(handle, OVR.ovrEye_Left,  eyeRenderDesc[0].getFov(), pixelsPerDisplayPixel);
        final ovrSizei recommenedTex1Size = OVR.ovrHmd_GetFovTextureSize(handle, OVR.ovrEye_Right, eyeRenderDesc[1].getFov(), pixelsPerDisplayPixel);
        if( StereoDevice.DEBUG ) {
            System.err.println("XXX: recommenedTex0Size "+OVRUtil.toString(recommenedTex0Size));
            System.err.println("XXX: recommenedTex1Size "+OVRUtil.toString(recommenedTex1Size));
        }
        final int unifiedW = Math.max(recommenedTex0Size.getW(), recommenedTex1Size.getW());
        final int unifiedH = Math.max(recommenedTex0Size.getH(), recommenedTex1Size.getH());

        final DimensionImmutable singleTextureSize = new Dimension(unifiedW, unifiedH);
        final DimensionImmutable totalTextureSize = new Dimension(recommenedTex0Size.getW() + recommenedTex1Size.getW(), unifiedH);
        if( StereoDevice.DEBUG ) {
            System.err.println("XXX: textureSize Single "+singleTextureSize);
            System.err.println("XXX: textureSize Total  "+totalTextureSize);
        }

        final RectangleImmutable[] eyeRenderViewports = new RectangleImmutable[2];
        if( 1 == textureCount ) { // validated in ctor below!
            eyeRenderViewports[0] = new Rectangle(0, 0,
                                                  totalTextureSize.getWidth() / 2,
                                                  totalTextureSize.getHeight());

            eyeRenderViewports[1] = new Rectangle((totalTextureSize.getWidth() + 1) / 2, 0,
                                                  totalTextureSize.getWidth() / 2,
                                                  totalTextureSize.getHeight());
        } else {
            eyeRenderViewports[0] = new Rectangle(0, 0,
                                                  singleTextureSize.getWidth(),
                                                  singleTextureSize.getHeight());
            eyeRenderViewports[1] = eyeRenderViewports[0];
        }
        return new OVRStereoDeviceRenderer(this, distortionBits, textureCount, eyePositionOffset,
                                           eyeRenderDesc, singleTextureSize, totalTextureSize, eyeRenderViewports, textureUnit);
    }
}
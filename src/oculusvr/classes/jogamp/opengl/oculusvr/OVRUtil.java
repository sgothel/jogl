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

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.PointImmutable;
import com.jogamp.nativewindow.util.RectangleImmutable;

import com.jogamp.oculusvr.OVR;
import com.jogamp.oculusvr.ovrEyeRenderDesc;
import com.jogamp.oculusvr.ovrFovPort;
import com.jogamp.oculusvr.ovrQuatf;
import com.jogamp.oculusvr.ovrRecti;
import com.jogamp.oculusvr.ovrSizei;
import com.jogamp.oculusvr.ovrVector2f;
import com.jogamp.oculusvr.ovrVector2i;
import com.jogamp.oculusvr.ovrVector3f;
import com.jogamp.opengl.math.FovHVHalves;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.util.stereo.StereoDevice;
import com.jogamp.opengl.util.stereo.StereoDeviceRenderer;

/**
 * OculusVR Data Conversion Helper Functions
 */
public class OVRUtil {
    public static ovrRecti createOVRRecti(final int[] rect) {
        final ovrRecti res = ovrRecti.create();
        final ovrVector2i pos = res.getPos();
        final ovrSizei size = res.getSize();
        pos.setX(rect[0]);
        pos.setY(rect[1]);
        size.setW(rect[2]);
        size.setH(rect[3]);
        return res;
    }
    public static ovrRecti createOVRRecti(final RectangleImmutable rect) {
        final ovrRecti res = ovrRecti.create();
        final ovrVector2i pos = res.getPos();
        final ovrSizei size = res.getSize();
        pos.setX(rect.getX());
        pos.setY(rect.getY());
        size.setW(rect.getWidth());
        size.setH(rect.getHeight());
        return res;
    }
    public static ovrRecti[] createOVRRectis(final int[][] rects) {
        final ovrRecti[] res = new ovrRecti[rects.length];
        for(int i=0; i<res.length; i++) {
            res[0] = createOVRRecti(rects[i]);
        }
        return res;
    }
    public static ovrSizei createOVRSizei(final int[] size) {
        final ovrSizei res = ovrSizei.create();
        res.setW(size[0]);
        res.setH(size[1]);
        return res;
    }
    public static ovrSizei createOVRSizei(final DimensionImmutable size) {
        final ovrSizei res = ovrSizei.create();
        res.setW(size.getWidth());
        res.setH(size.getHeight());
        return res;
    }
    public static DimensionImmutable getOVRSizei(final ovrSizei v) {
        return new Dimension(v.getW(), v.getH());
    }
    public static PointImmutable getVec2iAsPoint(final ovrVector2i v) {
        return new Point(v.getX(), v.getY());
    }
    public static int[] getVec2i(final ovrVector2i v) {
        return new int[] { v.getX(), v.getY() };
    }
    public static void copyVec2iToInt(final ovrVector2i v, final int[] res) {
        res[0] = v.getX();
        res[1] = v.getY();
    }
    public static float[] getVec3f(final ovrVector3f v) {
        return new float[] { v.getX(), v.getY(), v.getZ() };
    }
    public static void copyVec3fToFloat(final ovrVector3f v, final float[] res) {
        res[0] = v.getX();
        res[1] = v.getY();
        res[2] = v.getZ();
    }
    public static Quaternion getQuaternion(final ovrQuatf q) {
        return new Quaternion(q.getX(), q.getY(), q.getZ(), q.getW());
    }
    public static void copyToQuaternion(final ovrQuatf in, final Quaternion  out) {
        out.set(in.getX(), in.getY(), in.getZ(), in.getW());
    }

    public static FovHVHalves getFovHV(final ovrFovPort tanHalfFov) {
        return new FovHVHalves(tanHalfFov.getLeftTan(), tanHalfFov.getRightTan(),
                               tanHalfFov.getUpTan(), tanHalfFov.getDownTan(),
                               true);
    }
    public static ovrFovPort getOVRFovPort(final FovHVHalves fovHVHalves) {
        final ovrFovPort tanHalfFov = ovrFovPort.create();
        final FovHVHalves fovHVHalvesTan = fovHVHalves.toTangents();
        tanHalfFov.setLeftTan(fovHVHalvesTan.left);
        tanHalfFov.setRightTan(fovHVHalvesTan.right);
        tanHalfFov.setUpTan(fovHVHalvesTan.top);
        tanHalfFov.setDownTan(fovHVHalvesTan.bottom);
        return tanHalfFov;
    }

    public static int ovrDistCaps2DistBits(final int ovrDistortionCaps) {
        int bits = StereoDeviceRenderer.DISTORTION_BARREL;
        if( 0 != ( OVR.ovrDistortionCap_TimeWarp & ovrDistortionCaps ) ) {
            bits |= StereoDeviceRenderer.DISTORTION_TIMEWARP;
        }
        if( 0 != ( OVR.ovrDistortionCap_Chromatic & ovrDistortionCaps ) ) {
            bits |= StereoDeviceRenderer.DISTORTION_CHROMATIC;
        }
        if( 0 != ( OVR.ovrDistortionCap_Vignette & ovrDistortionCaps ) ) {
            bits |= StereoDeviceRenderer.DISTORTION_VIGNETTE;
        }
        return bits;
    }

    public static int ovrTrackingCaps2SensorBits(final int ovrTrackingCaps) {
        int bits = 0;
        if( 0 != ( OVR.ovrTrackingCap_Orientation & ovrTrackingCaps ) ) {
            bits |= StereoDevice.SENSOR_ORIENTATION;
        }
        if( 0 != ( OVR.ovrTrackingCap_MagYawCorrection & ovrTrackingCaps ) ) {
            bits |= StereoDevice.SENSOR_YAW_CORRECTION;
        }
        if( 0 != ( OVR.ovrTrackingCap_Position & ovrTrackingCaps ) ) {
            bits |= StereoDevice.SENSOR_POSITION;
        }
        return bits;
    }
    public static int ovrTrackingStats2SensorBits(final int ovrTrackingCaps) {
        int bits = 0;
        if( 0 != ( OVR.ovrStatus_OrientationTracked & ovrTrackingCaps ) ) {
            bits |= StereoDevice.SENSOR_ORIENTATION;
        }
        if( 0 != ( OVR.ovrStatus_PositionTracked & ovrTrackingCaps ) ) {
            bits |= StereoDevice.SENSOR_POSITION;
        }
        return bits;
    }
    public static int sensorBits2TrackingCaps(final int sensorBits) {
        int caps = 0;
        if( 0 != ( StereoDevice.SENSOR_ORIENTATION  & sensorBits ) ) {
            caps |= OVR.ovrTrackingCap_Orientation;
        }
        if( 0 != ( StereoDevice.SENSOR_YAW_CORRECTION & sensorBits ) ) {
            caps |= OVR.ovrTrackingCap_MagYawCorrection;
        }
        if( 0 != ( StereoDevice.SENSOR_POSITION & sensorBits ) ) {
            caps |= OVR.ovrTrackingCap_Position;
        }
        return caps;
    }

    public static String toString(final ovrFovPort fov) {
        return "["+fov.getLeftTan()+" l, "+fov.getRightTan()+" r, "+
                   fov.getUpTan()+" u, "+fov.getDownTan()+" d]";
    }
    public static String toString(final ovrSizei rect) {
        return "["+rect.getW()+" x "+rect.getH()+"]";
    }
    public static String toString(final ovrRecti rect) {
        return "["+rect.getPos().getX()+"  / "+rect.getPos().getY()+" "+
                   rect.getSize().getW()+" x "+rect.getSize().getH()+"]";
    }
    public static String toString(final ovrVector2f v2) {
        return "["+v2.getX()+", "+v2.getY()+"]";
    }
    public static String toString(final ovrVector3f v3) {
        return "["+v3.getX()+", "+v3.getY()+", "+v3.getZ()+"]";
    }
    public static String toString(final ovrEyeRenderDesc desc) {
        return "["+desc.getEye()+", fov"+toString(desc.getFov())+
                 ", viewport"+toString(desc.getDistortedViewport())+
                 ", pptCtr"+toString(desc.getPixelsPerTanAngleAtCenter())+
                 ", view-adjust"+toString(desc.getHmdToEyeViewOffset())+"]";
    }
}

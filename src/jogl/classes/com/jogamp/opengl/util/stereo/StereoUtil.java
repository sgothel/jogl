/**
 * Copyright 2014-2023 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.util.stereo;

import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.util.CustomGLEventListener;
import com.jogamp.opengl.util.stereo.StereoDeviceRenderer.Eye;

public class StereoUtil {
    /**
     * Return the vertical pupil center from the screen top in the range [0..1].
     * @param screenHeightInMeters
     * @param pupilCenterFromScreenTopInMeters
     */
    public static float getVertPupilCenterFromTop(final float screenHeightInMeters, final float pupilCenterFromScreenTopInMeters) {
        return pupilCenterFromScreenTopInMeters / screenHeightInMeters;
    }

    /**
     * Return the horizontal pupil center from the left side for both eyes in the range [0..1].
     * <pre>
            <-------------left eye------------->|                       |<-----------right eye-------------->
            <------------------------------------screenSizeInMeters.Width----------------------------------->
                                       <------interpupillaryDistanceInMeters------>
            <--centerFromLeftInMeters->
                                       ^
                                 center of pupil
     * </pre>
     *
     * @param screenWidthInMeters
     * @param interpupillaryDistanceInMeters
     */
    public static float[] getHorizPupilCenterFromLeft(final float screenWidthInMeters, final float interpupillaryDistanceInMeters) {
        final float visibleWidthOfOneEye = 0.5f * screenWidthInMeters;
        final float leftPupilCenterFromLeftInMeters = ( screenWidthInMeters - interpupillaryDistanceInMeters ) * 0.5f;
        final float rightPupilCenterFromMiddleInMeters = leftPupilCenterFromLeftInMeters + interpupillaryDistanceInMeters - visibleWidthOfOneEye;
        return new float[] { leftPupilCenterFromLeftInMeters    / visibleWidthOfOneEye,
                             rightPupilCenterFromMiddleInMeters / visibleWidthOfOneEye };
    }

    /** See {@link StereoDeviceRenderer#getDistortionBits()}. */
    public static boolean usesBarrelDistortion(final int distortionBits) { return 0 != ( distortionBits & StereoDeviceRenderer.DISTORTION_BARREL ) ; }
    /** See {@link StereoDeviceRenderer#getDistortionBits()}. */
    public static boolean usesTimewarpDistortion(final int distortionBits) { return 0 != ( distortionBits & StereoDeviceRenderer.DISTORTION_TIMEWARP ) ; }
    /** See {@link StereoDeviceRenderer#getDistortionBits()}. */
    public static boolean usesChromaticDistortion(final int distortionBits) { return 0 != ( distortionBits & StereoDeviceRenderer.DISTORTION_CHROMATIC ) ; }
    /** See {@link StereoDeviceRenderer#getDistortionBits()}. */
    public static boolean usesVignetteDistortion(final int distortionBits) { return 0 != ( distortionBits & StereoDeviceRenderer.DISTORTION_VIGNETTE ) ; }

    /** See {@link StereoDeviceRenderer#getDistortionBits()}. */
    public static String distortionBitsToString(final int distortionBits) {
        boolean appendComma = false;
        final StringBuilder sb = new StringBuilder();
        if( usesBarrelDistortion(distortionBits) ) {
            if( appendComma ) { sb.append(", "); };
            sb.append("barrel"); appendComma=true;
        }
        if( usesVignetteDistortion(distortionBits) ) {
            if( appendComma ) { sb.append(", "); };
            sb.append("vignette"); appendComma=true;
        }
        if( usesChromaticDistortion(distortionBits) ) {
            if( appendComma ) { sb.append(", "); };
            sb.append("chroma"); appendComma=true;
        }
        if( usesTimewarpDistortion(distortionBits) ) {
            if( appendComma ) { sb.append(", "); };
            sb.append("timewarp"); appendComma=true;
        }
        return sb.toString();
    }

    /** See {@link StereoDevice#getSupportedSensorBits()} and {@link StereoDevice#getEnabledSensorBits()}. */
    public static boolean usesOrientationSensor(final int sensorBits) { return 0 != ( sensorBits & StereoDevice.SENSOR_ORIENTATION ) ; }
    /** See {@link StereoDevice#getSupportedSensorBits()} and {@link StereoDevice#getEnabledSensorBits()}. */
    public static boolean usesYawCorrectionSensor(final int sensorBits) { return 0 != ( sensorBits & StereoDevice.SENSOR_YAW_CORRECTION ) ; }
    /** See {@link StereoDevice#getSupportedSensorBits()} and {@link StereoDevice#getEnabledSensorBits()}. */
    public static boolean usesPositionSensor(final int sensorBits) { return 0 != ( sensorBits & StereoDevice.SENSOR_POSITION ) ; }

    /** See {@link StereoDevice#getSupportedSensorBits()} and {@link StereoDevice#getEnabledSensorBits()}. */
    public static String sensorBitsToString(final int sensorBits) {
        boolean appendComma = false;
        final StringBuilder sb = new StringBuilder();
        if( usesOrientationSensor(sensorBits) ) {
            if( appendComma ) { sb.append(", "); };
            sb.append("orientation"); appendComma=true;
        }
        if( usesYawCorrectionSensor(sensorBits) ) {
            if( appendComma ) { sb.append(", "); };
            sb.append("yaw-corr"); appendComma=true;
        }
        if( usesPositionSensor(sensorBits) ) {
            if( appendComma ) { sb.append(", "); };
            sb.append("position"); appendComma=true;
        }
        return sb.toString();
    }

    /**
     * Calculates the <i>Side By Side</i>, SBS, projection- and modelview matrix for one eye.
     * <p>
     * {@link #updateViewerPose(int)} must be called upfront.
     * </p>
     * <p>
     * This method merely exist as an example implementation to compute the matrices,
     * which shall be adopted by the
     * {@link CustomGLEventListener#reshape(com.jogamp.opengl.GLAutoDrawable, int, int, int, int, EyeParameter, ViewerPose) upstream client code}.
     * </p>
     * @param viewerPose
     * @param eye
     * @param zNear frustum near value
     * @param zFar frustum far value
     * @param mat4Projection projection matrix result
     * @param mat4Modelview modelview matrix result
     */
    public static void getSBSUpstreamPMV(final ViewerPose viewerPose, final Eye eye,
                                         final float zNear, final float zFar,
                                         final Matrix4f mat4Projection, final Matrix4f mat4Modelview) {
        final Matrix4f mat4Tmp1 = new Matrix4f();
        final Matrix4f mat4Tmp2 = new Matrix4f();
        final Vec3f vec3Tmp1 = new Vec3f();
        final Vec3f vec3Tmp2 = new Vec3f();
        final Vec3f vec3Tmp3 = new Vec3f();

        final EyeParameter eyeParam = eye.getEyeParameter();

        //
        // Projection
        //
        mat4Projection.setToPerspective(eyeParam.fovhv, zNear, zFar);

        //
        // Modelview
        //
        final Quaternion rollPitchYaw = new Quaternion();
        // private final float eyeYaw = FloatUtil.PI; // 180 degrees in radians
        // rollPitchYaw.rotateByAngleY(eyeYaw);
        final Vec3f shiftedEyePos = rollPitchYaw.rotateVector(viewerPose.position, vec3Tmp1).add(eyeParam.positionOffset);

        rollPitchYaw.mult(viewerPose.orientation);
        final Vec3f up = rollPitchYaw.rotateVector(Vec3f.UNIT_Y, vec3Tmp2);
        final Vec3f forward = rollPitchYaw.rotateVector(Vec3f.UNIT_Z_NEG, vec3Tmp3); // -> center
        final Vec3f center = forward.add(shiftedEyePos);

        final Matrix4f mLookAt = mat4Tmp2.setToLookAt(shiftedEyePos, center, up, mat4Tmp1);
        mat4Modelview.mul( mat4Tmp1.setToTranslation( eyeParam.distNoseToPupilX,
                                                      eyeParam.distMiddleToPupilY,
                                                      eyeParam.eyeReliefZ ), mLookAt);
    }

}

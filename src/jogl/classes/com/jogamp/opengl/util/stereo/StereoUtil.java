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
package com.jogamp.opengl.util.stereo;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.util.CustomGLEventListener;
import com.jogamp.opengl.util.stereo.StereoDeviceRenderer.Eye;

public class StereoUtil {

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

    /**
     * Calculates the <i>Side By Side</i>, SBS, projection- and modelview matrix for one eye.
     * <p>
     * {@link #updateEyePose(int)} must be called upfront.
     * </p>
     * <p>
     * This method merely exist as an example implementation to compute the matrices,
     * which shall be adopted by the
     * {@link CustomGLEventListener#reshape(javax.media.opengl.GLAutoDrawable, int, int, int, int, EyeParameter, EyePose) upstream client code}.
     * </p>
     * @param eyeNum eye denominator
     * @param zNear frustum near value
     * @param zFar frustum far value
     * @param mat4Projection float[16] projection matrix result
     * @param mat4Modelview float[16] modelview matrix result
     */
    public static void getSBSUpstreamPMV(final Eye eye, final float zNear, final float zFar,
                                         final float[] mat4Projection, final float[] mat4Modelview) {
        final float[] mat4Tmp1 = new float[16];
        final float[] mat4Tmp2 = new float[16];
        final float[] vec3Tmp1 = new float[3];
        final float[] vec3Tmp2 = new float[3];
        final float[] vec3Tmp3 = new float[3];

        final EyeParameter eyeParam = eye.getEyeParameter();
        final EyePose eyePose = eye.getLastEyePose();

        //
        // Projection
        //
        FloatUtil.makePerspective(mat4Projection, 0, true, eyeParam.fovhv, zNear, zFar);

        //
        // Modelview
        //
        final Quaternion rollPitchYaw = new Quaternion();
        // private final float eyeYaw = FloatUtil.PI; // 180 degrees in radians
        // rollPitchYaw.rotateByAngleY(eyeYaw);
        final float[] shiftedEyePos = rollPitchYaw.rotateVector(vec3Tmp1, 0, eyePose.position, 0);
        VectorUtil.addVec3(shiftedEyePos, shiftedEyePos, eyeParam.positionOffset);

        rollPitchYaw.mult(eyePose.orientation);
        final float[] up = rollPitchYaw.rotateVector(vec3Tmp2, 0, VectorUtil.VEC3_UNIT_Y, 0);
        final float[] forward = rollPitchYaw.rotateVector(vec3Tmp3, 0, VectorUtil.VEC3_UNIT_Z_NEG, 0);
        final float[] center = VectorUtil.addVec3(forward, shiftedEyePos, forward);

        final float[] mLookAt = FloatUtil.makeLookAt(mat4Tmp2, 0, shiftedEyePos, 0, center, 0, up, 0, mat4Tmp1);
        final float[] mViewAdjust = FloatUtil.makeTranslation(mat4Modelview, true,
                                                              eyeParam.distNoseToPupilX,
                                                              eyeParam.distMiddleToPupilY,
                                                              eyeParam.eyeReliefZ);

        /* mat4Modelview = */ FloatUtil.multMatrix(mViewAdjust, mLookAt);
    }

}

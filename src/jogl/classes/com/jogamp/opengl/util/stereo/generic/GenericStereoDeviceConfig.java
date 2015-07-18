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
package com.jogamp.opengl.util.stereo.generic;

import java.util.Arrays;

import jogamp.opengl.util.stereo.DistortionMesh;
import jogamp.opengl.util.stereo.GenericStereoDevice;

import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.opengl.util.stereo.EyeParameter;
import com.jogamp.opengl.util.stereo.StereoDeviceConfig;
import com.jogamp.opengl.util.stereo.StereoDeviceRenderer;
import com.jogamp.opengl.util.stereo.StereoUtil;
import com.jogamp.opengl.util.stereo.StereoDevice;

/**
 * Configuration for {@link GenericStereoDevice}s.
 */
public class GenericStereoDeviceConfig extends StereoDeviceConfig {
    public static enum ShutterType {
        Global, RollingLeftToRight, RollingRightToLeft, RollingTopToBottom
    }
    public GenericStereoDeviceConfig(final String name,
                  final GenericStereoDeviceConfig.ShutterType shutterType,
                  final DimensionImmutable surfaceSizeInPixels,
                  final float[] screenSizeInMeters,
                  final DimensionImmutable[/*pre-eye*/] eyeTextureSize,
                  final float pupilCenterFromScreenTopInMeters,
                  final float interpupillaryDistanceInMeters,
                  final int[] eyeRenderOrder,
                  final EyeParameter[] defaultEyeParam,
                  final int supportedSensorBits,
                  final DistortionMesh.Producer distortionMeshProducer,
                  final int supportedDistortionBits,
                  final int recommendedDistortionBits,
                  final int minimumDistortionBits
                  ) {
        if( eyeRenderOrder.length != defaultEyeParam.length ) {
            throw new IllegalArgumentException("eye arrays of different length");
        }
        this.name = name;
        this.shutterType = shutterType;
        this.surfaceSizeInPixels = surfaceSizeInPixels;
        this.screenSizeInMeters = screenSizeInMeters;
        this.eyeTextureSizes = eyeTextureSize;
        this.pupilCenterFromScreenTopInMeters = pupilCenterFromScreenTopInMeters;
        this.interpupillaryDistanceInMeters = interpupillaryDistanceInMeters;
        this.eyeRenderOrder = eyeRenderOrder;
        this.defaultEyeParam = defaultEyeParam;
        this.supportedSensorBits = supportedSensorBits;
        this.distortionMeshProducer = distortionMeshProducer;
        this.supportedDistortionBits = supportedDistortionBits;
        this.recommendedDistortionBits = recommendedDistortionBits;
        this.minimumDistortionBits = minimumDistortionBits;
        this.pupilCenterFromTopLeft = new float[2][2];
        calcPupilCenterFromTopLeft();
    }
    /** A variation w/ different surface/screen specs */
    public GenericStereoDeviceConfig(final GenericStereoDeviceConfig source,
                  final DimensionImmutable surfaceSizeInPixels,
                  final float[] screenSizeInMeters,
                  final DimensionImmutable[/*pre-eye*/] eyeTextureSize) {
        this.name = source.name;
        this.shutterType = source.shutterType;
        this.surfaceSizeInPixels = surfaceSizeInPixels;
        this.screenSizeInMeters = screenSizeInMeters;
        this.eyeTextureSizes = eyeTextureSize;
        this.pupilCenterFromScreenTopInMeters = source.pupilCenterFromScreenTopInMeters;
        this.interpupillaryDistanceInMeters = source.interpupillaryDistanceInMeters;
        this.eyeRenderOrder = source.eyeRenderOrder;
        this.defaultEyeParam = source.defaultEyeParam;
        this.supportedSensorBits = source.supportedSensorBits;
        this.distortionMeshProducer = source.distortionMeshProducer;
        this.supportedDistortionBits = source.supportedDistortionBits;
        this.recommendedDistortionBits = source.recommendedDistortionBits;
        this.minimumDistortionBits = source.minimumDistortionBits;
        this.pupilCenterFromTopLeft = new float[2][2];
        calcPupilCenterFromTopLeft();
    }
    private void calcPupilCenterFromTopLeft() {
        final float visibleWidthOfOneEye = 0.5f * screenSizeInMeters[0];
        final float leftPupilCenterFromLeftInMeters = ( screenSizeInMeters[0] - interpupillaryDistanceInMeters ) * 0.5f;
        final float rightPupilCenterFromMiddleInMeters = leftPupilCenterFromLeftInMeters + interpupillaryDistanceInMeters - visibleWidthOfOneEye;
        pupilCenterFromTopLeft[0][0] = leftPupilCenterFromLeftInMeters / visibleWidthOfOneEye;
        pupilCenterFromTopLeft[0][1] = pupilCenterFromScreenTopInMeters     / screenSizeInMeters[1];
        pupilCenterFromTopLeft[1][0] = rightPupilCenterFromMiddleInMeters / visibleWidthOfOneEye;
        pupilCenterFromTopLeft[1][1] =  pupilCenterFromTopLeft[0][1];
    }

    /**
     * One time lazy initialization before use.
     * @see #isInitialized()
     */
    public synchronized void init() {
        if( !isInitialized ) {
            if( null != distortionMeshProducer ) {
                final float[] eyeReliefInMeters = new float[defaultEyeParam.length];
                if( 0 < defaultEyeParam.length ) {
                    eyeReliefInMeters[0] = defaultEyeParam[0].eyeReliefZ;
                }
                if( 1 < defaultEyeParam.length ) {
                    eyeReliefInMeters[1] = defaultEyeParam[1].eyeReliefZ;
                }
                distortionMeshProducer.init(this, eyeReliefInMeters);
            }
            isInitialized = true;
        }
    }
    /**
     * Returns {@code true} if {@link #init() initialized}, otherwise {@code false}.
     * @see #init()
     */
    public final boolean isInitialized() { return isInitialized; }

    @Override
    public String toString() { return "StereoConfig["+name+", shutter "+shutterType+", surfaceSize "+surfaceSizeInPixels+
                               ", screenSize "+screenSizeInMeters[0]+" x "+screenSizeInMeters[0]+
                               " [m], eyeTexSize "+Arrays.toString(eyeTextureSizes)+", IPD "+interpupillaryDistanceInMeters+
                               " [m], eyeParam "+Arrays.toString(defaultEyeParam)+
                               ", distortionBits[supported ["+StereoUtil.distortionBitsToString(supportedDistortionBits)+
                                              "], recommended ["+StereoUtil.distortionBitsToString(recommendedDistortionBits)+
                                              "], minimum ["+StereoUtil.distortionBitsToString(minimumDistortionBits)+"]]"+
                               ", sensorBits[supported ["+StereoUtil.sensorBitsToString(supportedSensorBits)+"]]]";
    }

    /** Configuration Name */
    public final String name;
    public final GenericStereoDeviceConfig.ShutterType shutterType;

    public final DimensionImmutable surfaceSizeInPixels;
    public final float[] screenSizeInMeters;
    /** Texture size per eye */
    public final DimensionImmutable[/*pre-eye*/] eyeTextureSizes;

    /** Vertical distance from pupil to screen-top in meters */
    public final float pupilCenterFromScreenTopInMeters;
    /** Horizontal interpupillary distance (IPD) in meters */
    public final float interpupillaryDistanceInMeters;
    /**
     * Pupil center from top left per eye, ranging from [0..1], maybe used to produce FovHVHalves,
     * see {@link #getHorizPupilCenterFromLeft(float, float)} and {@link #getVertPupilCenterFromTop(float, float)}.
     */
    public final float[/*per-eye*/][/*xy*/] pupilCenterFromTopLeft;
    public final int[] eyeRenderOrder;
    public final EyeParameter[] defaultEyeParam;

    /** Supported sensor bits, see {@link StereoDevice#SENSOR_ORIENTATION}. */
    public final int supportedSensorBits;

    public final DistortionMesh.Producer distortionMeshProducer;

    /** Supported distortion bits, see {@link StereoDeviceRenderer#DISTORTION_BARREL}. */
    public final int supportedDistortionBits;
    /** Recommended distortion bits, see {@link StereoDeviceRenderer.DISTORTION_BARREL}. */
    public final int recommendedDistortionBits;
    /** Required distortion bits, see {@link StereoDeviceRenderer.DISTORTION_BARREL}. */
    public final int minimumDistortionBits;

    private boolean isInitialized = false;
}
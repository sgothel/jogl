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
package com.jogamp.opengl.util.stereo.generic;

import jogamp.opengl.util.stereo.DistortionMesh;
import jogamp.opengl.util.stereo.GenericStereoDevice;

import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.FovHVHalves;
import com.jogamp.opengl.util.stereo.EyeParameter;
import com.jogamp.opengl.util.stereo.StereoDevice;
import com.jogamp.opengl.util.stereo.StereoDeviceConfig;
import com.jogamp.opengl.util.stereo.StereoDeviceFactory;
import com.jogamp.opengl.util.stereo.StereoDeviceRenderer;
import com.jogamp.opengl.util.stereo.StereoUtil;

public class GenericStereoDeviceFactory extends StereoDeviceFactory {

    /**
     * Create a generic monoscopic {@link GenericStereoDeviceConfig generic device config}.
     * @param name
     * @param surfaceSizeInPixel
     * @param screenSizeInMeters
     * @param defaultEyePositionOffset
     */
    public static GenericStereoDeviceConfig createMono(final String name,
                             final DimensionImmutable surfaceSizeInPixel, final float[] screenSizeInMeters,
                             final float[] defaultEyePositionOffset) {
        final float pupilCenterFromScreenTopInMeters = screenSizeInMeters[1] / 2f;
        final float d2r = FloatUtil.PI / 180.0f;
        return new GenericStereoDeviceConfig(
                        name,
                        GenericStereoDeviceConfig.ShutterType.RollingTopToBottom,
                        surfaceSizeInPixel,                // resolution
                        screenSizeInMeters,                // screenSize [m]
                        new DimensionImmutable[] { surfaceSizeInPixel }, // eye textureSize
                        pupilCenterFromScreenTopInMeters,  // pupilCenterFromScreenTop [m]
                        0f,                                // IPD [m]
                        new int[] { 0 },                   // eye order
                        new EyeParameter[] {
                            new EyeParameter(0, defaultEyePositionOffset,
                                             // degrees: 45/2 l, 45/2 r, 45/2 * aspect t, 45/2 * aspect b
                                             FovHVHalves.byFovyRadianAndAspect(45f*d2r, 1280f / 800f),
                                             0f /* distNoseToPupil */, 0f /* verticalDelta */, 0f /* eyeReliefInMeters */) },
                        0,      // supported sensor bits
                        null, // mash producer distortion bits
                        0,    // supported distortion bits
                        0,    // recommended distortion bits
                        0     // minimum distortion bits
                        );
    }

    /**
     * Create a generic homogenous side-by-side stereoscopic {@link GenericStereoDeviceConfig generic device config}.
     * @param name
     * @param surfaceSizeInPixel
     * @param screenSizeInMeters
     * @param interpupillaryDistanceInMeters
     * @param fovy
     * @param defaultEyePositionOffset
     */
    public static GenericStereoDeviceConfig createStereoSBS(final String name,
                             final DimensionImmutable surfaceSizeInPixel, final float[] screenSizeInMeters,
                             final float interpupillaryDistanceInMeters, final float fovy,
                             final float[] defaultEyePositionOffset) {
        final float pupilCenterFromScreenTopInMeters = screenSizeInMeters[1] / 2f;
        final float d2r = FloatUtil.PI / 180.0f;

        final DimensionImmutable eyeTextureSize = new Dimension(surfaceSizeInPixel.getWidth()/2, surfaceSizeInPixel.getHeight());
        final float[] horizPupilCenterFromLeft = StereoUtil.getHorizPupilCenterFromLeft(screenSizeInMeters[0], interpupillaryDistanceInMeters);
        final float vertPupilCenterFromTop = StereoUtil.getVertPupilCenterFromTop(screenSizeInMeters[1], pupilCenterFromScreenTopInMeters);
        final float aspect = (float)eyeTextureSize.getWidth() / (float)eyeTextureSize.getHeight();
        final FovHVHalves defaultSBSEyeFovLeft = FovHVHalves.byFovyRadianAndAspect(fovy * d2r, vertPupilCenterFromTop, aspect, horizPupilCenterFromLeft[0]);
        final FovHVHalves defaultSBSEyeFovRight = FovHVHalves.byFovyRadianAndAspect(fovy * d2r, vertPupilCenterFromTop, aspect, horizPupilCenterFromLeft[1]);

        return new GenericStereoDeviceConfig(
                        name,
                        GenericStereoDeviceConfig.ShutterType.RollingTopToBottom,
                        surfaceSizeInPixel,                // resolution
                        screenSizeInMeters,                // screenSize [m]
                        new DimensionImmutable[] { eyeTextureSize, eyeTextureSize }, // eye textureSize
                        pupilCenterFromScreenTopInMeters,  // pupilCenterFromScreenTop [m]
                        interpupillaryDistanceInMeters,    // IPD [m]
                        new int[] { 0, 1 },                // eye order
                        new EyeParameter[] {
                            new EyeParameter(0, defaultEyePositionOffset, defaultSBSEyeFovLeft,
                                          interpupillaryDistanceInMeters/2f /* distNoseToPupil */, 0f /* verticalDelta */, 0.010f /* eyeReliefInMeters */),
                            new EyeParameter(1, defaultEyePositionOffset, defaultSBSEyeFovRight,
                                         -interpupillaryDistanceInMeters/2f /* distNoseToPupil */, 0f /* verticalDelta */, 0.010f /* eyeReliefInMeters */) },
                        0,      // supported sensor bits
                        null,   // mash producer distortion bits
                        0,      // supported distortion bits
                        0,      // recommended distortion bits
                        0       // minimum distortion bits
                        );

    }

    /**
     * Create a generic lense distorted side-by-side stereoscopic {@link GenericStereoDeviceConfig generic device config}.
     * @param name
     * @param surfaceSizeInPixel
     * @param screenSizeInMeters
     * @param interpupillaryDistanceInMeters
     * @param fovy
     * @param eyeTextureSize
     * @param defaultEyePositionOffset
     */
    public static GenericStereoDeviceConfig createStereoSBSLense(final String name,
                             final DimensionImmutable surfaceSizeInPixel, final float[] screenSizeInMeters,
                             final float interpupillaryDistanceInMeters, final float fovy,
                             final DimensionImmutable eyeTextureSize,
                             final float[] defaultEyePositionOffset) {
        DistortionMesh.Producer lenseDistMeshProduce = null;
        try {
            lenseDistMeshProduce =
                (DistortionMesh.Producer)
                ReflectionUtil.createInstance("jogamp.opengl.oculusvr.stereo.lense.DistortionMeshProducer", GenericStereoDevice.class.getClassLoader());
        } catch (final Throwable t) {
            if(StereoDevice.DEBUG) { System.err.println("Caught: "+t.getMessage()); t.printStackTrace(); }
        }
        if( null == lenseDistMeshProduce ) {
            return null;
        }
        final float pupilCenterFromScreenTopInMeters = screenSizeInMeters[1] / 2f;
        final float d2r = FloatUtil.PI / 180.0f;

        final float[] horizPupilCenterFromLeft = StereoUtil.getHorizPupilCenterFromLeft(screenSizeInMeters[0], interpupillaryDistanceInMeters);
        final float vertPupilCenterFromTop = StereoUtil.getVertPupilCenterFromTop(screenSizeInMeters[1], pupilCenterFromScreenTopInMeters);
        final float aspect = (float)eyeTextureSize.getWidth() / (float)eyeTextureSize.getHeight();
        final FovHVHalves defaultSBSEyeFovLeft = FovHVHalves.byFovyRadianAndAspect(fovy * d2r, vertPupilCenterFromTop, aspect, horizPupilCenterFromLeft[0]);
        final FovHVHalves defaultSBSEyeFovRight = FovHVHalves.byFovyRadianAndAspect(fovy * d2r, vertPupilCenterFromTop, aspect, horizPupilCenterFromLeft[1]);

        return new GenericStereoDeviceConfig(
                        name,
                        GenericStereoDeviceConfig.ShutterType.RollingTopToBottom,
                        surfaceSizeInPixel,                // resolution
                        screenSizeInMeters,                // screenSize [m]
                        new DimensionImmutable[] { eyeTextureSize, eyeTextureSize }, // eye textureSize
                        pupilCenterFromScreenTopInMeters,  // pupilCenterFromScreenTop [m]
                        interpupillaryDistanceInMeters,    // IPD [m]
                        new int[] { 0, 1 },                // eye order
                        new EyeParameter[] {
                            new EyeParameter(0, defaultEyePositionOffset, defaultSBSEyeFovLeft,
                                          interpupillaryDistanceInMeters/2f /* distNoseToPupil */, 0f /* verticalDelta */, 0.010f /* eyeReliefInMeters */),
                            new EyeParameter(1, defaultEyePositionOffset, defaultSBSEyeFovRight,
                                         -interpupillaryDistanceInMeters/2f /* distNoseToPupil */, 0f /* verticalDelta */, 0.010f /* eyeReliefInMeters */) },
                        0,      // supported sensor bits
                        lenseDistMeshProduce,
                        // supported distortion bits
                        StereoDeviceRenderer.DISTORTION_BARREL | StereoDeviceRenderer.DISTORTION_CHROMATIC | StereoDeviceRenderer.DISTORTION_VIGNETTE,
                        // recommended distortion bits
                        StereoDeviceRenderer.DISTORTION_BARREL | StereoDeviceRenderer.DISTORTION_CHROMATIC | StereoDeviceRenderer.DISTORTION_VIGNETTE,
                        // minimum distortion bits
                        StereoDeviceRenderer.DISTORTION_BARREL
                        );
    }


    public static boolean isAvailable() {
        return true;
    }

    @Override
    protected final StereoDevice createDeviceImpl(final int deviceIndex, final StereoDeviceConfig config, final boolean verbose) {
        return new GenericStereoDevice(this, deviceIndex, config);
    }

    private boolean isValid = true;

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public final void shutdown() {
        if( isValid ) {
            // NOP
            isValid = false;
        }
    }
}

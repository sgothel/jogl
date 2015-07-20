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
package jogamp.opengl.util.stereo;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.PointImmutable;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.opengl.math.FovHVHalves;
import com.jogamp.opengl.util.stereo.StereoDeviceConfig;
import com.jogamp.opengl.util.stereo.EyeParameter;
import com.jogamp.opengl.util.stereo.LocationSensorParameter;
import com.jogamp.opengl.util.stereo.StereoDevice;
import com.jogamp.opengl.util.stereo.StereoDeviceFactory;
import com.jogamp.opengl.util.stereo.StereoDeviceRenderer;
import com.jogamp.opengl.util.stereo.StereoUtil;
import com.jogamp.opengl.util.stereo.generic.GenericStereoDeviceConfig;
import com.jogamp.opengl.util.stereo.generic.GenericStereoDeviceFactory;

public class GenericStereoDevice implements StereoDevice {
    /** A mono view configuration DK1, only one eye is supported */
    public static final GenericStereoDeviceConfig config01Mono01;
    /** A mono view configuration DK2, only one eye is supported */
    public static final GenericStereoDeviceConfig config01Mono02;

    /** A default stereo SBS view configuration DK1 */
    public static final GenericStereoDeviceConfig config02StereoSBS01;
    /** A default stereo SBS view configuration DK2 */
    public static final GenericStereoDeviceConfig config02StereoSBS02;

    /** A default stereo SBS lense view configuration DK1, utilizing similar settings as OculusVR DK1 */
    public static final GenericStereoDeviceConfig config03StereoSBSLense01;
    /** A default stereo SBS lense view configuration DK2, utilizing similar settings as OculusVR DK1 */
    public static final GenericStereoDeviceConfig config03StereoSBSLense02;

    private static final GenericStereoDeviceConfig[] configs;

    static {
        final float[] DEFAULT_EYE_POSITION_OFFSET_STEREO_LENSES = { 0.0f, 1.6f, -5.0f };  // 1.6 up, 5 forward
        final float[] DEFAULT_EYE_POSITION_OFFSET_STEREO        = { 0.0f, 0.3f,  3.0f };  // 0.3 up, 3 back
        final float[] DEFAULT_EYE_POSITION_OFFSET_MONO          = { 0.0f, 0.0f,  3.0f };  //         3 back

        final DimensionImmutable surfaceSizeInPixelDK1 = new Dimension(1280, 800);
        final float[] screenSizeInMetersDK1 = new float[] { 0.14976f, 0.0936f };
        final DimensionImmutable eyeTextureSizeDK1 = new Dimension(1122, 1553);

        final DimensionImmutable surfaceSizeInPixelDK2 = new Dimension(1920, 1080);
        final float[] screenSizeInMetersDK2 = new float[] { 0.12576f, 0.07074f };
        final DimensionImmutable eyeTextureSizeDK2 = new Dimension(1182, 1461);

        final float interpupillaryDistanceInMeters = 0.0635f;

        config01Mono01 = GenericStereoDeviceFactory.createMono("Def01Mono01",
                                    surfaceSizeInPixelDK1, screenSizeInMetersDK1,
                                    DEFAULT_EYE_POSITION_OFFSET_MONO);

        config02StereoSBS01 = GenericStereoDeviceFactory.createStereoSBS("Def02StereoSBS01",
                                  surfaceSizeInPixelDK1, screenSizeInMetersDK1,
                                  interpupillaryDistanceInMeters, 45f /* fovy */,
                                  DEFAULT_EYE_POSITION_OFFSET_STEREO);

        config03StereoSBSLense01 = GenericStereoDeviceFactory.createStereoSBSLense("Def03StereoSBSLense01",
                                  surfaceSizeInPixelDK1, screenSizeInMetersDK1,
                                  interpupillaryDistanceInMeters, 129f /* fovy */,
                                  eyeTextureSizeDK1,
                                  DEFAULT_EYE_POSITION_OFFSET_STEREO_LENSES);

        config01Mono02 = GenericStereoDeviceFactory.createMono("Def01Mono02",
                                    surfaceSizeInPixelDK2, screenSizeInMetersDK2,
                                    DEFAULT_EYE_POSITION_OFFSET_MONO);

        config02StereoSBS02 = GenericStereoDeviceFactory.createStereoSBS("Def02StereoSBS02",
                                  surfaceSizeInPixelDK2, screenSizeInMetersDK2,
                                  interpupillaryDistanceInMeters, 45f /* fovy */,
                                  DEFAULT_EYE_POSITION_OFFSET_STEREO);

        config03StereoSBSLense02 = GenericStereoDeviceFactory.createStereoSBSLense("Def03StereoSBSLense02",
                                  surfaceSizeInPixelDK2, screenSizeInMetersDK2,
                                  interpupillaryDistanceInMeters, 129f /* fovy */,
                                  eyeTextureSizeDK2,
                                  DEFAULT_EYE_POSITION_OFFSET_STEREO_LENSES);

        configs = new GenericStereoDeviceConfig[] {
                                 config01Mono01, config02StereoSBS01, config03StereoSBSLense01,
                                 config01Mono02, config02StereoSBS02, config03StereoSBSLense02 };
    }

    private final StereoDeviceFactory factory;
    public final int deviceIndex;
    public final GenericStereoDeviceConfig config;

    public final Point surfacePos;
    private final FovHVHalves[] defaultEyeFov;

    private int usedSensorBits;
    private boolean sensorsStarted = false;

    public GenericStereoDevice(final StereoDeviceFactory factory, final int deviceIndex, final StereoDeviceConfig customConfig) {
        this.factory = factory;
        this.deviceIndex = deviceIndex;

        if( customConfig instanceof GenericStereoDeviceConfig) {
            this.config = (GenericStereoDeviceConfig) customConfig;
        } else {
            final int cfgIdx = Math.min(deviceIndex % 10, configs.length-1);
            this.config = null != configs[cfgIdx] ? configs[cfgIdx] : config02StereoSBS01;
        }
        config.init();

        this.surfacePos = new Point(0, 0);

        defaultEyeFov = new FovHVHalves[config.defaultEyeParam.length];
        for(int i=0; i<defaultEyeFov.length; i++) {
            defaultEyeFov[i] = config.defaultEyeParam[i].fovhv;
        }

        // default
        usedSensorBits = 0;
    }

    @Override
    public final StereoDeviceFactory getFactory() { return factory; }

    @Override
    public String toString() {
        return "GenericStereoDevice["+config+", surfacePos "+surfacePos+
                ", sensorBits[enabled ["+StereoUtil.sensorBitsToString(getEnabledSensorBits())+"]]]";
    }

    public void setSurfacePosition(final int x, final int y) {
        surfacePos.set(x, y);
    }

    @Override
    public final void dispose() {
        stopSensors();
    }

    @Override
    public boolean isValid() { return true; }

    @Override
    public final PointImmutable getPosition() { return surfacePos; }

    @Override
    public final DimensionImmutable getSurfaceSize() { return config.surfaceSizeInPixels; }
    @Override
    public int getRequiredRotation() { return 0; }

    @Override
    public float[] getDefaultEyePositionOffset() { return config.defaultEyeParam[0].positionOffset; }

    @Override
    public final FovHVHalves[] getDefaultFOV() { return defaultEyeFov; }

    @Override
    public final LocationSensorParameter getLocationSensorParams() { return null; }

    @Override
    public final void resetLocationSensorOrigin() { }

    @Override
    public final boolean startSensors(final int desiredSensorBits, final int requiredSensorBits) {
        if( !sensorsStarted ) {
            if( requiredSensorBits != ( config.supportedSensorBits & requiredSensorBits ) ) {
                // required sensors not available
                return false;
            }
            if( 0 == ( config.supportedSensorBits & ( requiredSensorBits | desiredSensorBits ) ) ) {
                // no sensors available
                return false;
            }
            if( startSensorsImpl(true, desiredSensorBits, requiredSensorBits) ) {
                sensorsStarted = true;
                return true;
            } else {
                return false;
            }
        } else {
            // No state change -> Success
            return true;
        }
    }
    protected boolean startSensorsImpl(final boolean start, final int desiredSensorBits, final int requiredSensorBits) {
        // TODO: Add SPI for sensors
        // TODO: start sensors in override / or SPI
        // TODO: set usedSensorBits
        return false;
    }

    @Override
    public final boolean stopSensors() {
        if( sensorsStarted ) {
            if( startSensorsImpl(false, 0, 0) ) {
                sensorsStarted = false;
                usedSensorBits = 0;
                return true;
            } else {
                return false;
            }
        } else {
            // No state change -> Success
            return true;
        }
    }

    @Override
    public boolean getSensorsStarted() { return sensorsStarted; }

    @Override
    public final int getSupportedSensorBits() {
        return config.supportedSensorBits;
    }

    @Override
    public final int getEnabledSensorBits() {
        return usedSensorBits;
    }

    @Override
    public int[] getEyeRenderOrder() { return config.eyeRenderOrder; }

    @Override
    public int getSupportedDistortionBits() {
        return config.supportedDistortionBits;
    };

    @Override
    public int getRecommendedDistortionBits() {
        return config.recommendedDistortionBits;
    }

    @Override
    public int getMinimumDistortionBits() {
        return config.minimumDistortionBits;
    }

    @Override
    public final StereoDeviceRenderer createRenderer(final int distortionBits,
                                                     final int textureCount, final float[] eyePositionOffset,
                                                     final FovHVHalves[] eyeFov, final float pixelsPerDisplayPixel,
                                                     final int textureUnit) {
       final EyeParameter[] eyeParam = new EyeParameter[eyeFov.length];
       for(int i=0; i<eyeParam.length; i++) {
           final EyeParameter defaultEyeParam = config.defaultEyeParam[i];
           eyeParam[i] = new EyeParameter(i, eyePositionOffset, eyeFov[i],
                                          defaultEyeParam.distNoseToPupilX, defaultEyeParam.distMiddleToPupilY, defaultEyeParam.eyeReliefZ);
       }

       final boolean usePP = null != config.distortionMeshProducer && 0 != distortionBits; // use post-processing

       final RectangleImmutable[] eyeViewports = new RectangleImmutable[eyeParam.length];
       final DimensionImmutable totalTextureSize;
       if( 1 < eyeParam.length ) {
           // Stereo SBS
           final DimensionImmutable eye0TextureSize = config.eyeTextureSizes[0];
           final DimensionImmutable eye1TextureSize = config.eyeTextureSizes[1];
           final int maxHeight = Math.max(eye0TextureSize.getHeight(), eye1TextureSize.getHeight());

           totalTextureSize = new Dimension(eye0TextureSize.getWidth()+eye1TextureSize.getWidth(), maxHeight);
           if( 1 == textureCount ) { // validated in ctor below!
               // one big texture/FBO, viewport to target space
               eyeViewports[0] = new Rectangle(0, 0,
                                               eye0TextureSize.getWidth(),
                                               maxHeight);
               eyeViewports[1] = new Rectangle(eye0TextureSize.getWidth(), 0,
                                               eye1TextureSize.getWidth(),
                                               maxHeight);
           } else if( usePP ) {
               // two textures/FBOs w/ postprocessing, which renders textures/FBOs into target space
               eyeViewports[0] = new Rectangle(0, 0,
                                               eye0TextureSize.getWidth(),
                                               eye0TextureSize.getHeight());
               eyeViewports[1] = new Rectangle(0, 0,
                                               eye1TextureSize.getWidth(),
                                               eye1TextureSize.getHeight());
           } else {
               // two textures/FBOs w/o postprocessing, viewport to target space
               eyeViewports[0] = new Rectangle(0, 0,
                                               eye0TextureSize.getWidth(),
                                               eye0TextureSize.getHeight());
               eyeViewports[1] = new Rectangle(eye0TextureSize.getWidth(), 0,
                                               eye1TextureSize.getWidth(),
                                               eye1TextureSize.getHeight());
           }
       } else {
           // Mono
           final DimensionImmutable eye0TextureSize = config.eyeTextureSizes[0];
           totalTextureSize = eye0TextureSize;
           eyeViewports[0] = new Rectangle(0, 0, eye0TextureSize.getWidth(), eye0TextureSize.getHeight());
       }
       return new GenericStereoDeviceRenderer(this, distortionBits, textureCount, eyePositionOffset, eyeParam,
                                              pixelsPerDisplayPixel, textureUnit,
                                              config.eyeTextureSizes, totalTextureSize, eyeViewports);
    }
}
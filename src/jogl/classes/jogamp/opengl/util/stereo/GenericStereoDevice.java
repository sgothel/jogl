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

import java.util.Arrays;

import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.Point;
import javax.media.nativewindow.util.PointImmutable;
import javax.media.nativewindow.util.Rectangle;
import javax.media.nativewindow.util.RectangleImmutable;

import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.FovHVHalves;
import com.jogamp.opengl.util.stereo.EyeParameter;
import com.jogamp.opengl.util.stereo.StereoDevice;
import com.jogamp.opengl.util.stereo.StereoDeviceRenderer;
import com.jogamp.opengl.util.stereo.StereoUtil;

public class GenericStereoDevice implements StereoDevice {
    public static enum ShutterType {
        Global, RollingLeftToRight, RollingRightToLeft, RollingTopToBottom
    }
    public static class Config extends StereoDevice.Config {
        public Config(final String name,
                      final ShutterType shutterType,
                      final DimensionImmutable surfaceSizeInPixels,
                      final float[] screenSizeInMeters,
                      final DimensionImmutable eyeTextureSize,
                      final float pupilCenterFromScreenTopInMeters,
                      final float interpupillaryDistanceInMeters,
                      final int[] eyeRenderOrder,
                      final EyeParameter[] defaultEyeParam,
                      final DistortionMesh.Producer distortionMeshProducer,
                      final int supportedDistortionBits,
                      final int recommendedDistortionBits,
                      final int minimumDistortionBits
                      ) {
            this.name = name;
            this.shutterType = shutterType;
            this.surfaceSizeInPixels = surfaceSizeInPixels;
            this.screenSizeInMeters = screenSizeInMeters;
            this.eyeTextureSize = eyeTextureSize;
            this.pupilCenterFromScreenTopInMeters = pupilCenterFromScreenTopInMeters;
            this.interpupillaryDistanceInMeters = interpupillaryDistanceInMeters;
            this.eyeRenderOrder = eyeRenderOrder;
            this.defaultEyeParam = defaultEyeParam;
            this.distortionMeshProducer = distortionMeshProducer;
            this.supportedDistortionBits = supportedDistortionBits;
            this.recommendedDistortionBits = recommendedDistortionBits;
            this.minimumDistortionBits = minimumDistortionBits;
            this.pupilCenterFromTopLeft = new float[2][2];
            calcPupilCenterFromTopLeft();
        }
        /** A variation w/ different surface/screen specs */
        public Config(final Config source,
                      final DimensionImmutable surfaceSizeInPixels,
                      final float[] screenSizeInMeters,
                      final DimensionImmutable eyeTextureSize) {
            this.name = source.name;
            this.shutterType = source.shutterType;
            this.surfaceSizeInPixels = surfaceSizeInPixels;
            this.screenSizeInMeters = screenSizeInMeters;
            this.eyeTextureSize = eyeTextureSize;
            this.pupilCenterFromScreenTopInMeters = source.pupilCenterFromScreenTopInMeters;
            this.interpupillaryDistanceInMeters = source.interpupillaryDistanceInMeters;
            this.eyeRenderOrder = source.eyeRenderOrder;
            this.defaultEyeParam = source.defaultEyeParam;
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

        private void init() {
            final float[] eyeReliefInMeters = new float[defaultEyeParam.length];
            if( 0 < defaultEyeParam.length ) {
                eyeReliefInMeters[0] = defaultEyeParam[0].eyeReliefZ;
            }
            if( 1 < defaultEyeParam.length ) {
                eyeReliefInMeters[1] = defaultEyeParam[1].eyeReliefZ;
            }
            if( null != distortionMeshProducer ) {
                distortionMeshProducer.init(this, eyeReliefInMeters);
            }
        }

        @Override
        public String toString() { return "StereoConfig["+name+", shutter "+shutterType+", surfaceSize "+surfaceSizeInPixels+
                                   ", screenSize "+screenSizeInMeters[0]+" x "+screenSizeInMeters[0]+
                                   " [m], eyeTexSize "+eyeTextureSize+", IPD "+interpupillaryDistanceInMeters+
                                   " [m], eyeParam "+Arrays.toString(defaultEyeParam)+
                                   ", distortionBits[supported ["+StereoUtil.distortionBitsToString(supportedDistortionBits)+
                                                  "], recommended ["+StereoUtil.distortionBitsToString(recommendedDistortionBits)+
                                                  "], minimum ["+StereoUtil.distortionBitsToString(minimumDistortionBits)+"]]]";
        }

        /** Configuration Name */
        public final String name;
        public final ShutterType shutterType;

        public final DimensionImmutable surfaceSizeInPixels;
        public final float[] screenSizeInMeters;
        /** Texture size per eye */
        public final DimensionImmutable eyeTextureSize;

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
        public final DistortionMesh.Producer distortionMeshProducer;

        public final int supportedDistortionBits;
        public final int recommendedDistortionBits;
        public final int minimumDistortionBits;
    }

    /** A mono view configuration, only one eye is supported */
    public static final Config config01Mono01;

    /** A default stereo SBS view configuration */
    public static final Config config02StereoSBS01;

    /** A default stereo SBS lense view configuration, utilizing similar settings as OculusVR DK1 */
    public static final Config config03StereoSBSLense01;

    private static final Config[] configs;

    static {
        final float[] DEFAULT_EYE_POSITION_OFFSET_STEREO_LENSES = { 0.0f, 1.6f, -5.0f };  // 1.6 up, 5 forward
        final float[] DEFAULT_EYE_POSITION_OFFSET_STEREO        = { 0.0f, 0.3f,  3.0f };  // 0.3 up, 3 back
        final float[] DEFAULT_EYE_POSITION_OFFSET_MONO          = { 0.0f, 0.0f,  3.0f };  //         3 back

        final float d2r = FloatUtil.PI / 180.0f;
        {
            config01Mono01 = new Config(
                            "Def01Mono01",
                            ShutterType.RollingTopToBottom,
                            new Dimension(1280, 800),          // resolution
                            new float[] { 0.1498f, 0.0936f },  // screenSize [m]
                            new Dimension(1280, 800),          // eye textureSize
                            0.0936f/2f,                        // pupilCenterFromScreenTop [m]
                            0.0635f,                           // IPD [m]
                            new int[] { 0 },                   // eye order
                            new EyeParameter[] {
                                new EyeParameter(0, DEFAULT_EYE_POSITION_OFFSET_MONO,
                                                 // degrees: 45/2 l, 45/2 r, 45/2 * aspect t, 45/2 * aspect b
                                                 FovHVHalves.byFovyRadianAndAspect(45f*d2r, 1280f / 800f),
                                                 0f /* distNoseToPupil */, 0f /* verticalDelta */, 0f /* eyeReliefInMeters */) },
                            null, // mash producer distortion bits
                            0,    // supported distortion bits
                            0,    // recommended distortion bits
                            0     // minimum distortion bits
                            );
        }

        {
            final DimensionImmutable surfaceSizeInPixel = new Dimension(1280, 800);
            final float[] screenSizeInMeters = new float[] { 0.1498f, 0.0936f };
            final float interpupillaryDistanceInMeters = 0.0635f;
            final float pupilCenterFromScreenTopInMeters = screenSizeInMeters[1] / 2f;
            final float[] horizPupilCenterFromLeft = Config.getHorizPupilCenterFromLeft(screenSizeInMeters[0], interpupillaryDistanceInMeters);
            final float vertPupilCenterFromTop = Config.getVertPupilCenterFromTop(screenSizeInMeters[1], pupilCenterFromScreenTopInMeters);
            final float fovy = 45f;
            final float aspect = ( surfaceSizeInPixel.getWidth() / 2.0f ) / surfaceSizeInPixel.getHeight();
            final FovHVHalves defaultSBSEyeFovLeft = FovHVHalves.byFovyRadianAndAspect(fovy * d2r, vertPupilCenterFromTop, aspect, horizPupilCenterFromLeft[0]);
            final FovHVHalves defaultSBSEyeFovRight = FovHVHalves.byFovyRadianAndAspect(fovy * d2r, vertPupilCenterFromTop, aspect, horizPupilCenterFromLeft[1]);

            config02StereoSBS01 = new Config(
                            "Def02StereoSBS01",
                            ShutterType.RollingTopToBottom,
                            surfaceSizeInPixel,                // resolution
                            screenSizeInMeters,                // screenSize [m]
                            new Dimension(1280/2, 800),        // eye textureSize
                            0.0936f/2f,                        // pupilCenterFromScreenTop [m]
                            interpupillaryDistanceInMeters,    // IPD [m]
                            new int[] { 0, 1 },                // eye order
                            new EyeParameter[] {
                                new EyeParameter(0, DEFAULT_EYE_POSITION_OFFSET_STEREO, defaultSBSEyeFovLeft,
                                              0.032f /* distNoseToPupil */, 0f /* verticalDelta */, 0.010f /* eyeReliefInMeters */),
                                new EyeParameter(1, DEFAULT_EYE_POSITION_OFFSET_STEREO, defaultSBSEyeFovRight,
                                             -0.032f /* distNoseToPupil */, 0f /* verticalDelta */, 0.010f /* eyeReliefInMeters */) },
                            null,   // mash producer distortion bits
                            0,      // supported distortion bits
                            0,      // recommended distortion bits
                            0       // minimum distortion bits
                            );
        }

        {
            DistortionMesh.Producer lenseDistMeshProduce = null;
            try {
                lenseDistMeshProduce =
                    (DistortionMesh.Producer)
                    ReflectionUtil.createInstance("jogamp.opengl.oculusvr.stereo.lense.DistortionMeshProducer", GenericStereoDevice.class.getClassLoader());
            } catch (final Throwable t) {
                if(StereoDevice.DEBUG) { System.err.println("Caught: "+t.getMessage()); t.printStackTrace(); }
            }

            final DimensionImmutable surfaceSizeInPixel = new Dimension(1280, 800);
            final float[] screenSizeInMeters = new float[] { 0.1498f, 0.0936f };
            final DimensionImmutable eyeTextureSize = new Dimension(1122, 1553);
            final float interpupillaryDistanceInMeters = 0.0635f;
            final float pupilCenterFromScreenTopInMeters = screenSizeInMeters[1] / 2f;
            final float[] horizPupilCenterFromLeft = Config.getHorizPupilCenterFromLeft(screenSizeInMeters[0], interpupillaryDistanceInMeters);
            final float vertPupilCenterFromTop = Config.getVertPupilCenterFromTop(screenSizeInMeters[1], pupilCenterFromScreenTopInMeters);
            final float fovy = 129f;
            final float aspect = eyeTextureSize.getWidth() / eyeTextureSize.getHeight();
            final FovHVHalves defaultSBSEyeFovLenseLeft = FovHVHalves.byFovyRadianAndAspect(fovy * d2r, vertPupilCenterFromTop, aspect, horizPupilCenterFromLeft[0]);
            final FovHVHalves defaultSBSEyeFovLenseRight = FovHVHalves.byFovyRadianAndAspect(fovy * d2r, vertPupilCenterFromTop, aspect, horizPupilCenterFromLeft[1]);

            config03StereoSBSLense01 = null == lenseDistMeshProduce ? null :
                           new Config(
                            "Def03StereoSBSLense01",
                            ShutterType.RollingTopToBottom,
                            surfaceSizeInPixel,                // resolution
                            screenSizeInMeters,                // screenSize [m]
                            eyeTextureSize,                    // eye textureSize
                            pupilCenterFromScreenTopInMeters,  // pupilCenterFromScreenTop [m]
                            interpupillaryDistanceInMeters,    // IPD [m]
                            new int[] { 0, 1 },                // eye order
                            new EyeParameter[] {
                                new EyeParameter(0, DEFAULT_EYE_POSITION_OFFSET_STEREO_LENSES, defaultSBSEyeFovLenseLeft,
                                              0.032f /* distNoseToPupil */, 0f /* verticalDelta */, 0.010f /* eyeReliefInMeters */),
                                new EyeParameter(1, DEFAULT_EYE_POSITION_OFFSET_STEREO_LENSES, defaultSBSEyeFovLenseRight,
                                             -0.032f /* distNoseToPupil */, 0f /* verticalDelta */, 0.010f /* eyeReliefInMeters */) },
                            lenseDistMeshProduce,
                            // supported distortion bits
                            StereoDeviceRenderer.DISTORTION_BARREL | StereoDeviceRenderer.DISTORTION_CHROMATIC | StereoDeviceRenderer.DISTORTION_VIGNETTE,
                            // recommended distortion bits
                            StereoDeviceRenderer.DISTORTION_BARREL | StereoDeviceRenderer.DISTORTION_CHROMATIC | StereoDeviceRenderer.DISTORTION_VIGNETTE,
                            // minimum distortion bits
                            StereoDeviceRenderer.DISTORTION_BARREL
                            );
        }
        configs = new Config[] { config01Mono01, config02StereoSBS01, config03StereoSBSLense01 };
    }

    public final int deviceIndex;
    public final Config config;

    public final Point surfacePos;
    private final FovHVHalves[] defaultEyeFov;

    private boolean sensorsStarted = false;

    public GenericStereoDevice(final int deviceIndex, final StereoDevice.Config customConfig) {
        this.deviceIndex = deviceIndex;

        if( customConfig instanceof GenericStereoDevice.Config) {
            this.config = (GenericStereoDevice.Config) customConfig;
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
    }

    @Override
    public String toString() {
        return "GenericStereoDevice["+config+", surfacePos "+surfacePos+"]";
    }

    public void setSurfacePosition(final int x, final int y) {
        surfacePos.set(x, y);
    }

    @Override
    public final void dispose() {
        // NOP
    }

    @Override
    public final PointImmutable getPosition() {
        return surfacePos;
    }

    @Override
    public final DimensionImmutable getSurfaceSize() {
        return config.surfaceSizeInPixels;
    }

    @Override
    public float[] getDefaultEyePositionOffset() {
        return config.defaultEyeParam[0].positionOffset;
    }

    @Override
    public final FovHVHalves[] getDefaultFOV() {
        return defaultEyeFov;
    }

    @Override
    public final boolean startSensors(final boolean start) {
        if( start && !sensorsStarted ) {
            if( startSensorsImpl(true) ) {
                sensorsStarted = true;
                return true;
            } else {
                sensorsStarted = false;
                return false;
            }
        } else if( sensorsStarted ) {
            if( startSensorsImpl(false) ) {
                sensorsStarted = false;
                return true;
            } else {
                sensorsStarted = true;
                return false;
            }
        } else {
            // No state change -> Success
            return true;
        }
    }
    private boolean startSensorsImpl(final boolean start) { return start; }

    @Override
    public boolean getSensorsStarted() { return sensorsStarted; }

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
                                                     final FovHVHalves[] eyeFov, final float pixelsPerDisplayPixel, final int textureUnit) {
       final EyeParameter[] eyeParam = new EyeParameter[eyeFov.length];
       for(int i=0; i<eyeParam.length; i++) {
           final EyeParameter defaultEyeParam = config.defaultEyeParam[i];
           eyeParam[i] = new EyeParameter(i, eyePositionOffset, eyeFov[i],
                                          defaultEyeParam.distNoseToPupilX, defaultEyeParam.distMiddleToPupilY, defaultEyeParam.eyeReliefZ);
       }

       final RectangleImmutable[] eyeViewports = new RectangleImmutable[eyeParam.length];
       final DimensionImmutable eyeTextureSize = config.eyeTextureSize;
       final DimensionImmutable totalTextureSize;
       if( 1 < eyeParam.length ) {
           // Stereo SBS
           totalTextureSize = new Dimension(eyeTextureSize.getWidth()*2, eyeTextureSize.getHeight());
           if( 1 == textureCount ) { // validated in ctor below!
               eyeViewports[0] = new Rectangle(0, 0,
                       totalTextureSize.getWidth() / 2, totalTextureSize.getHeight());

               eyeViewports[1] = new Rectangle((totalTextureSize.getWidth() + 1) / 2, 0,
                       totalTextureSize.getWidth() / 2, totalTextureSize.getHeight());
           } else {
               eyeViewports[0] = new Rectangle(0, 0, eyeTextureSize.getWidth(), eyeTextureSize.getHeight());
               eyeViewports[1] = eyeViewports[0];
           }
       } else {
           // Mono
           totalTextureSize = eyeTextureSize;
           eyeViewports[0] = new Rectangle(0, 0, totalTextureSize.getWidth(), totalTextureSize.getHeight());
       }
       return new GenericStereoDeviceRenderer(this, distortionBits, textureCount, eyePositionOffset, eyeParam, pixelsPerDisplayPixel, textureUnit,
                                              eyeTextureSize, totalTextureSize, eyeViewports);
    }
}
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
package com.jogamp.opengl.test.junit.jogl.stereo;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.PointImmutable;
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.math.FovHVHalves;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.av.MovieSBSStereo;
import com.jogamp.opengl.test.junit.jogl.demos.es2.av.MovieSimple;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.stereo.StereoDevice;
import com.jogamp.opengl.util.stereo.StereoDeviceRenderer;
import com.jogamp.opengl.util.stereo.StereoDeviceFactory;
import com.jogamp.opengl.util.stereo.StereoClientRenderer;
import com.jogamp.opengl.util.stereo.StereoGLEventListener;

/**
 * All distortions, no multisampling, bilinear filtering, manual-swap and using two FBOs (default, good)
 * <pre>
 * java StereoDemo01 -time 10000000
 * </pre>
 * All distortions, 8x multisampling, bilinear filtering, manual-swap and using two FBOs (best - slowest)
 * <pre>
 * java StereoDemo01 -time 10000000 -samples 8
 * </pre>
 * All distortions, 8x multisampling, bilinear filtering, manual-swap and using one a big single FBO (w/ all commandline params)
 * <pre>
 * java StereoDemo01 -time 10000000 -vignette true -chromatic true -timewarp false -samples 8 -biLinear true -autoSwap false -singleFBO true -mainScreen false
 * </pre>
 * No distortions, no multisampling, no filtering, auto-swap and using a big single FBO (worst and fastest)
 * <pre>
 * java StereoDemo01 -time 10000000 -vignette false -chromatic false -timewarp false -samples 0 -biLinear false -autoSwap true -singleFBO true
 * </pre>
 * Test on main screen:
 * <pre>
 * java StereoDemo01 -time 10000000 -mainScreen true
 * </pre>
 * Test a 3D SBS Movie:
 * <pre>
 * java StereoDemo01 -time 10000000 -filmFile Some_SBS_3D_Movie.mkv
 * java StereoDemo01 -time 10000000 -filmURI http://whoknows.not/Some_SBS_3D_Movie.mkv
 * </pre>
 * <p>
 * Key 'R' enables/disables the VR's sensors, i.e. head rotation ..
 * </p>
 *
 */
public class StereoDemo01 {
    static long duration = 10000; // ms

    static boolean useStereoScreen = true;

    static int numSamples = 0;
    static boolean biLinear = true;
    static boolean useSingleFBO = false;
    static boolean useVignette = true;
    static boolean useChromatic = true;
    static boolean useTimewarp = true;
    static boolean useAutoSwap = false;
    static String useFilmFile = null;
    static String useFilmURI = null;
    static String stereoRendererListenerName = null;

    public static void main(final String args[]) throws InterruptedException, URISyntaxException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-samples")) {
                i++;
                numSamples = MiscUtils.atoi(args[i], numSamples);
            } else if(args[i].equals("-biLinear")) {
                i++;
                biLinear = MiscUtils.atob(args[i], biLinear);
            } else if(args[i].equals("-singleFBO")) {
                i++;
                useSingleFBO = MiscUtils.atob(args[i], useSingleFBO);
            } else if(args[i].equals("-vignette")) {
                i++;
                useVignette = MiscUtils.atob(args[i], useVignette);
            } else if(args[i].equals("-chromatic")) {
                i++;
                useChromatic = MiscUtils.atob(args[i], useChromatic);
            } else if(args[i].equals("-timewarp")) {
                i++;
                useTimewarp = MiscUtils.atob(args[i], useTimewarp);
            } else if(args[i].equals("-vignette")) {
                i++;
                useVignette = MiscUtils.atob(args[i], useVignette);
            } else if(args[i].equals("-mainScreen")) {
                i++;
                useStereoScreen = !MiscUtils.atob(args[i], useStereoScreen);
            } else if(args[i].equals("-autoSwap")) {
                i++;
                useAutoSwap = MiscUtils.atob(args[i], useAutoSwap);
            } else if(args[i].equals("-test")) {
                i++;
                stereoRendererListenerName = args[i];
            } else if(args[i].equals("-filmFile")) {
                i++;
                useFilmFile = args[i];
            } else if(args[i].equals("-filmURI")) {
                i++;
                useFilmURI = args[i];
            }
        }
        if( null != stereoRendererListenerName ) {
            try {
                final Object stereoRendererListener = ReflectionUtil.createInstance(stereoRendererListenerName, null);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        final StereoGLEventListener upstream;
        final MovieSBSStereo movieSimple;
        final URI movieURI;
        if( null != useFilmFile ) {
            movieSimple = new MovieSBSStereo();
            movieURI = IOUtil.toURISimple(new File(useFilmFile));
            upstream = movieSimple;
        } else if( null != useFilmURI ) {
            movieSimple = new MovieSBSStereo();
            movieURI = new URI(useFilmURI);
            upstream = movieSimple;
        } else {
            final GearsES2 demo = new GearsES2(0);
            demo.setVerbose(false);
            upstream = demo;
            movieSimple = null;
            movieURI = null;
        }
        final StereoDemo01 demo01 = new StereoDemo01();
        demo01.doIt(0, upstream, movieSimple, movieURI, biLinear, numSamples, useSingleFBO, useVignette, useChromatic, useTimewarp,
                    useAutoSwap, true /* useAnimator */, false /* exclusiveContext*/);
    }

    public void doIt(final int stereoDeviceIndex,
                     final StereoGLEventListener upstream, final MovieSBSStereo movieSimple, final URI movieURI,
                     final boolean biLinear, final int numSamples, final boolean useSingleFBO,
                     final boolean useVignette, final boolean useChromatic, final boolean useTimewarp,
                     final boolean useAutoSwap, final boolean useAnimator, final boolean exclusiveContext) throws InterruptedException {

        System.err.println("glob duration "+duration);
        System.err.println("glob useStereoScreen "+useStereoScreen);
        System.err.println("biLinear "+biLinear);
        System.err.println("numSamples "+numSamples);
        System.err.println("useSingleFBO "+useSingleFBO);
        System.err.println("useVignette "+useVignette);
        System.err.println("useChromatic "+useChromatic);
        System.err.println("useTimewarp "+useTimewarp);
        System.err.println("useAutoSwap "+useAutoSwap);

        final StereoDeviceFactory stereoDeviceFactory = StereoDeviceFactory.createDefaultFactory();
        if( null == stereoDeviceFactory ) {
            System.err.println("No StereoDeviceFactory available");
            return;
        }

        final StereoDevice stereoDevice = stereoDeviceFactory.createDevice(stereoDeviceIndex, true /* verbose */);
        if( null == stereoDevice ) {
            System.err.println("No StereoDevice.Context available for index "+stereoDeviceIndex);
            return;
        }

        // Start the sensor which provides the Rift’s pose and motion.
        if( !stereoDevice.startSensors(true) ) {
            System.err.println("Could not start sensors on device "+stereoDeviceIndex);
        }

        //
        //
        //
        final GLCapabilities caps = new GLCapabilities(GLProfile.getMaxProgrammable(true /* favorHardwareRasterizer */));
        final GLWindow window = GLWindow.create(caps);

        final PointImmutable devicePos = stereoDevice.getPosition();
        final DimensionImmutable deviceRes = stereoDevice.getSurfaceSize();
        window.setSize(deviceRes.getWidth(), deviceRes.getHeight());
        if( useStereoScreen ) {
            window.setPosition(devicePos.getX(), devicePos.getY());
        }
        window.setAutoSwapBufferMode(useAutoSwap);
        window.setUndecorated(true);

        final Animator animator = useAnimator ? new Animator() : null;
        if( useAnimator ) {
            animator.setModeBits(false, AnimatorBase.MODE_EXPECT_AWT_RENDERING_THREAD);
            animator.setExclusiveContext(exclusiveContext);
        }

        //
        // Oculus Rift setup
        //
        // EyePos.y = ovrHmd_GetFloat(HMD, OVR_KEY_EYE_HEIGHT, EyePos.y);
        final FovHVHalves[] defaultEyeFov = stereoDevice.getDefaultFOV();
        System.err.println("Default Fov[0]: "+defaultEyeFov[0]);
        System.err.println("Default Fov[0]: "+defaultEyeFov[0].toStringInDegrees());
        System.err.println("Default Fov[1]: "+defaultEyeFov[1]);
        System.err.println("Default Fov[1]: "+defaultEyeFov[1].toStringInDegrees());

        final float[] eyePositionOffset = null == movieSimple ? StereoDevice.DEFAULT_EYE_POSITION_OFFSET // default
                                                              : new float[] { 0f, 0.3f, 0f };            // better fixed movie position
        final int textureUnit = 0;
        final int distortionBits = ( useVignette ? StereoDeviceRenderer.DISTORTION_VIGNETTE : 0 ) |
                                   ( useChromatic ? StereoDeviceRenderer.DISTORTION_CHROMATIC : 0 ) |
                                   ( useTimewarp ? StereoDeviceRenderer.DISTORTION_TIMEWARP : 0 );
        final float pixelsPerDisplayPixel = 1f;
        final StereoDeviceRenderer stereoDeviceRenderer =
                stereoDevice.createRenderer(distortionBits, useSingleFBO ? 1 : 2, eyePositionOffset,
                                            defaultEyeFov, pixelsPerDisplayPixel, textureUnit);
        System.err.println("StereoDeviceRenderer: "+stereoDeviceRenderer);

        final int texFilter = biLinear ? GL.GL_LINEAR : GL.GL_NEAREST;
        final StereoClientRenderer renderer = new StereoClientRenderer(stereoDeviceRenderer, true /* ownsDist */, texFilter, texFilter, numSamples);
        if( null != movieSimple && null != movieURI) {
            movieSimple.setScaleOrig(true);
            final GLMediaPlayer mp = movieSimple.getGLMediaPlayer();
            mp.attachObject(MovieSimple.WINDOW_KEY, window);
            mp.attachObject(MovieSBSStereo.STEREO_RENDERER_KEY, renderer);
            mp.addEventListener(MovieSBSStereo.stereoGLMediaEventListener);
            movieSimple.initStream(movieURI, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.STREAM_ID_AUTO, 3);
        } else {
            renderer.addGLEventListener(upstream);
        }
        window.addGLEventListener(renderer);

        final QuitAdapter quitAdapter = new QuitAdapter();
        window.addKeyListener(quitAdapter);
        window.addWindowListener(quitAdapter);

        window.addKeyListener(new KeyAdapter() {
            public void keyReleased(final KeyEvent e)  {
                if( e.isAutoRepeat() ) {
                    return;
                }
                switch(e.getKeySymbol()) {
                    case KeyEvent.VK_R: {
                        stereoDevice.startSensors(!stereoDevice.getSensorsStarted());
                        break;
                    }
                }
            } } );

        if( useAnimator ) {
            animator.add(window);
            animator.start();
        }
        window.setVisible(true);
        if( useAnimator ) {
            animator.setUpdateFPSFrames(60, System.err);
        }

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!quitAdapter.shouldQuit() && t1-t0<duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        if( useAnimator ) {
            animator.stop();
        }
        window.destroy();
        stereoDevice.dispose();
    }
}

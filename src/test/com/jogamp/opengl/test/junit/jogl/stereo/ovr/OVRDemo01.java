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
package com.jogamp.opengl.test.junit.jogl.stereo.ovr;

import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import jogamp.opengl.oculusvr.OVRDistortion;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.oculusvr.OVR;
import com.jogamp.oculusvr.OVRException;
import com.jogamp.oculusvr.OVRVersion;
import com.jogamp.oculusvr.OvrHmdContext;
import com.jogamp.oculusvr.ovrFovPort;
import com.jogamp.oculusvr.ovrHmdDesc;
import com.jogamp.oculusvr.ovrSizei;
import com.jogamp.oculusvr.ovrVector2i;
import com.jogamp.opengl.oculusvr.OVRSBSRendererDualFBO;
import com.jogamp.opengl.oculusvr.OVRSBSRendererSingleFBO;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;

/**
 * All distortions, no multisampling, bilinear filtering, manual-swap and using two FBOs (default, good)
 * <pre>
 * java OVRDemo01 -time 10000000
 * </pre>
 * All distortions, 8x multisampling, bilinear filtering, manual-swap and using two FBOs (best - slowest)
 * <pre>
 * java OVRDemo01 -time 10000000 -samples 8
 * </pre>
 * All distortions, 8x multisampling, bilinear filtering, manual-swap and using one a big single FBO (w/ all commandline params)
 * <pre>
 * java OVRDemo01 -time 10000000 -vignette true -chromatic true -timewarp false -samples 8 -biLinear true -autoSwap false -singleFBO true -mainScreen false
 * </pre>
 * No distortions, no multisampling, no filtering, auto-swap and using a big single FBO (worst and fastest)
 * <pre>
 * java OVRDemo01 -time 10000000 -vignette false -chromatic false -timewarp false -samples 0 -biLinear false -autoSwap true -singleFBO true
 * </pre>
 * Test on main screen:
 * <pre>
 * java OVRDemo01 -time 10000000 -mainScreen true
 * </pre>
 *
 */
public class OVRDemo01 {
    static long duration = 10000; // ms

    static boolean useOVRScreen = true;

    static int numSamples = 0;
    static boolean biLinear = true;
    static boolean useSingleFBO = false;
    static boolean useVignette = true;
    static boolean useChromatic = true;
    static boolean useTimewarp = true;
    static boolean useAutoSwap = false;

    public static void main(final String args[]) throws InterruptedException {
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
                useOVRScreen = !MiscUtils.atob(args[i], useOVRScreen);
            } else if(args[i].equals("-autoSwap")) {
                i++;
                useAutoSwap = MiscUtils.atob(args[i], useAutoSwap);
            }
        }
        final OVRDemo01 demo01 = new OVRDemo01();
        demo01.doIt(0, biLinear, numSamples, useSingleFBO, useVignette, useChromatic, useTimewarp,
                    useAutoSwap, true /* useAnimator */, false /* exclusiveContext*/);
    }

    public void doIt(final int ovrHmdIndex, final boolean biLinear, final int numSamples,
                     final boolean useSingleFBO,
                     final boolean useVignette, final boolean useChromatic, final boolean useTimewarp,
                     final boolean useAutoSwap,
                     final boolean useAnimator, final boolean exclusiveContext) throws InterruptedException {

        System.err.println("glob duration "+duration);
        System.err.println("glob useOVRScreen "+useOVRScreen);
        System.err.println("biLinear "+biLinear);
        System.err.println("numSamples "+numSamples);
        System.err.println("useSingleFBO "+useSingleFBO);
        System.err.println("useVignette "+useVignette);
        System.err.println("useChromatic "+useChromatic);
        System.err.println("useTimewarp "+useTimewarp);
        System.err.println("useAutoSwap "+useAutoSwap);

        // Initialize LibOVR...
        if( !OVR.ovr_Initialize() ) { // recursive ..
            throw new OVRException("OVR not available");
        }
        final OvrHmdContext hmdCtx = OVR.ovrHmd_Create(ovrHmdIndex);
        if( null == hmdCtx ) {
            throw new OVRException("OVR HMD #"+ovrHmdIndex+" not available");
        }
        final ovrHmdDesc hmdDesc = ovrHmdDesc.create();
        OVR.ovrHmd_GetDesc(hmdCtx, hmdDesc);
        System.err.println(OVRVersion.getAvailableCapabilitiesInfo(hmdDesc, ovrHmdIndex, null).toString());

        // Start the sensor which provides the Rift’s pose and motion.
        final int requiredSensorCaps = 0;
        final int supportedSensorCaps = requiredSensorCaps | OVR.ovrSensorCap_Orientation | OVR.ovrSensorCap_YawCorrection | OVR.ovrSensorCap_Position;
        if( !OVR.ovrHmd_StartSensor(hmdCtx, supportedSensorCaps, requiredSensorCaps) ) {
            throw new OVRException("OVR HMD #"+ovrHmdIndex+" required sensors not available");
        }

        //
        //
        //

        final GLCapabilities caps = new GLCapabilities(GLProfile.getMaxProgrammable(true /* favorHardwareRasterizer */));
        final GLWindow window = GLWindow.create(caps);
        final ovrVector2i ovrPos = hmdDesc.getWindowsPos();
        final ovrSizei ovrRes = hmdDesc.getResolution();
        window.setSize(ovrRes.getW(), ovrRes.getH());
        if( useOVRScreen ) {
            window.setPosition(ovrPos.getX(), ovrPos.getY());
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
        final float[] eyePositionOffset = { 0.0f, 1.6f, -5.0f };
        // EyePos.y = ovrHmd_GetFloat(HMD, OVR_KEY_EYE_HEIGHT, EyePos.y);

        final ovrFovPort[] defaultEyeFov = hmdDesc.getDefaultEyeFov(0, new ovrFovPort[2]);
        final int distortionCaps = ( useVignette ? OVR.ovrDistortionCap_Vignette : 0 ) |
                                   ( useChromatic ? OVR.ovrDistortionCap_Chromatic : 0 ) |
                                   ( useTimewarp ? OVR.ovrDistortionCap_TimeWarp : 0 );
        final float pixelsPerDisplayPixel = 1f;
        final OVRDistortion dist = OVRDistortion.create(hmdCtx, useSingleFBO, eyePositionOffset, defaultEyeFov, pixelsPerDisplayPixel, distortionCaps);
        System.err.println("OVRDistortion: "+dist);

        final int texFilter = biLinear ? GL.GL_LINEAR : GL.GL_NEAREST;
        final GearsES2 upstream = new GearsES2(0);
        upstream.setVerbose(false);
        final GLEventListener renderer;
        if( useSingleFBO ) {
            renderer = new OVRSBSRendererSingleFBO(dist, true /* ownsDist */, upstream, texFilter, texFilter, numSamples);
        } else {
            renderer = new OVRSBSRendererDualFBO(dist, true /* ownsDist */, upstream, texFilter, texFilter, numSamples);
        }
        window.addGLEventListener(renderer);

        final QuitAdapter quitAdapter = new QuitAdapter();
        window.addKeyListener(quitAdapter);
        window.addWindowListener(quitAdapter);

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
        // OVR.ovr_Shutdown();
    }
}

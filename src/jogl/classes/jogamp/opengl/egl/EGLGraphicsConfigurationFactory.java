/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package jogamp.opengl.egl;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.CapabilitiesChooser;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.egl.EGLGraphicsDevice;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLDrawableFactory;

import com.jogamp.common.nio.PointerBuffer;
import jogamp.opengl.GLGraphicsConfigurationFactory;
import jogamp.opengl.GLGraphicsConfigurationUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.PrintStream;


/** Subclass of GraphicsConfigurationFactory used when non-AWT tookits
    are used on X11 platforms. Toolkits will likely need to delegate
    to this one to change the accepted and returned types of the
    GraphicsDevice and GraphicsConfiguration abstractions. */

public class EGLGraphicsConfigurationFactory extends GLGraphicsConfigurationFactory {
    protected static final boolean DEBUG = GraphicsConfigurationFactory.DEBUG || jogamp.opengl.Debug.debug("EGL");
    static EGLGLCapabilities.EglCfgIDComparator EglCfgIDComparator = new EGLGLCapabilities.EglCfgIDComparator();

    EGLGraphicsConfigurationFactory() {
        // become the selector for KD/EGL ..
        GraphicsConfigurationFactory.registerFactory(javax.media.nativewindow.egl.EGLGraphicsDevice.class, this);
    }

    protected AbstractGraphicsConfiguration chooseGraphicsConfigurationImpl (
            CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested,
            CapabilitiesChooser chooser, AbstractGraphicsScreen absScreen) {
        if (absScreen == null) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only AbstractGraphicsDevice objects");
        }

        if (! (capsChosen instanceof GLCapabilitiesImmutable) ) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects - chosen");
        }

        if (! (capsRequested instanceof GLCapabilitiesImmutable) ) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects - requested");
        }

        if (chooser != null &&
            !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilitiesChooser objects");
        }

        return chooseGraphicsConfigurationStatic((GLCapabilitiesImmutable) capsChosen,
                                                 (GLCapabilitiesImmutable) capsRequested,
                                                 (GLCapabilitiesChooser) chooser,
                                                 absScreen);
    }

    protected static List/*<EGLGLCapabilities>*/ getAvailableCapabilities(EGLDrawableFactory factory, AbstractGraphicsDevice device) {
        EGLDrawableFactory.SharedResource sharedResource = factory.getOrCreateSharedResource(device);
        if(null == sharedResource) {
            throw new GLException("Shared resource for device n/a: "+device);
        }
        EGLGraphicsDevice eglDevice = sharedResource.getDevice();
        long eglDisplay = eglDevice.getHandle();

        List/*<EGLGLCapabilities>*/ availableCaps = null;
        int[] maxConfigs = new int[1];

        if(!EGL.eglGetConfigs(eglDisplay, null, 0, maxConfigs, 0)) {
            throw new GLException("Graphics configuration get maxConfigs (eglGetConfigs) call failed, error "+toHexString(EGL.eglGetError()));
        }
        if(0 == maxConfigs[0]) {
            throw new GLException("Graphics configuration get maxConfigs (eglGetConfigs) no configs");
        }

        PointerBuffer configs = PointerBuffer.allocateDirect(maxConfigs[0]);
        int[] numConfigs = new int[1];

        if(!EGL.eglGetConfigs(eglDisplay, configs, configs.capacity(), numConfigs, 0)) {
            throw new GLException("Graphics configuration get all configs (eglGetConfigs) call failed, error "+toHexString(EGL.eglGetError()));
        }
        if (numConfigs[0] > 0) {
            GLProfile glp = GLProfile.getDefault(device);
            availableCaps = eglConfigs2GLCaps(glp, eglDisplay, configs, numConfigs[0], GLGraphicsConfigurationUtil.ALL_BITS);
            if( null != availableCaps && availableCaps.size() > 1) {
                Collections.sort(availableCaps, EglCfgIDComparator);
            }
        }

        return availableCaps;
    }

    private static EGLGraphicsConfiguration chooseGraphicsConfigurationStatic(GLCapabilitiesImmutable capsChosen,
                                                                              GLCapabilitiesImmutable capsReq,
                                                                              GLCapabilitiesChooser chooser,
                                                                              AbstractGraphicsScreen absScreen) {
        if (capsChosen == null) {
            capsChosen = new GLCapabilities(null);
        }

        if(null==absScreen) {
            throw new GLException("Null AbstractGraphicsScreen");
        }
        AbstractGraphicsDevice absDevice = absScreen.getDevice();

        if(null==absDevice || !(absDevice instanceof EGLGraphicsDevice)) {
            throw new GLException("GraphicsDevice must be a valid EGLGraphicsDevice");
        }
        long eglDisplay = absDevice.getHandle();

        if (eglDisplay == EGL.EGL_NO_DISPLAY) {
            throw new GLException("Invalid EGL display: "+absDevice);
        }

        EGLDrawableFactory factory = (EGLDrawableFactory) GLDrawableFactory.getEGLFactory();
        capsChosen = GLGraphicsConfigurationUtil.fixGLCapabilities( capsChosen, factory.canCreateGLPbuffer(absDevice) );

        GLProfile glp = capsChosen.getGLProfile();

        EGLGraphicsConfiguration res = eglChooseConfig(eglDisplay, capsChosen, capsReq, chooser, absScreen);
        if(null!=res) {
            return res;
        }
        if(DEBUG) {
            System.err.println("eglChooseConfig failed with given capabilities "+capsChosen);
        }

        // Last try .. add a fixed embedded profile [ATI, Nokia, Intel, ..]
        //
        // rgb888 - d16, s4
        GLCapabilities fixedCaps = new GLCapabilities(glp);
        fixedCaps.setRedBits(8);
        fixedCaps.setGreenBits(8);
        fixedCaps.setBlueBits(8);
        fixedCaps.setDepthBits(16);
        fixedCaps.setSampleBuffers(true);
        fixedCaps.setNumSamples(4);
        if(DEBUG) {
            System.err.println("trying fixed caps (1): "+fixedCaps);
        }
        res = eglChooseConfig(eglDisplay, fixedCaps, capsReq, chooser, absScreen);
        if(null!=res) {
            return res;
        }

        //
        // rgb565 - d16, s0
        fixedCaps = new GLCapabilities(glp);
        fixedCaps.setRedBits(5);
        fixedCaps.setGreenBits(6);
        fixedCaps.setBlueBits(5);
        fixedCaps.setDepthBits(16);
        if(DEBUG) {
            System.err.println("trying fixed caps (2): "+fixedCaps);
        }
        res = eglChooseConfig(eglDisplay, fixedCaps, capsReq, chooser, absScreen);
        if(null!=res) {
            return res;
        }

        //
        // rgb565 - d16, s4
        fixedCaps = new GLCapabilities(glp);
        fixedCaps.setRedBits(5);
        fixedCaps.setGreenBits(6);
        fixedCaps.setBlueBits(5);
        fixedCaps.setDepthBits(16);
        fixedCaps.setSampleBuffers(true);
        fixedCaps.setNumSamples(4);
        if(DEBUG) {
            System.err.println("trying fixed caps (3): "+fixedCaps);
        }
        res = eglChooseConfig(eglDisplay, fixedCaps, capsReq, chooser, absScreen);
        if(null!=res) {
            return res;
        }
        throw new GLException("Graphics configuration failed [direct caps, eglGetConfig/chooser and fixed-caps(1-3)]");
    }

    static EGLGraphicsConfiguration eglChooseConfig(long eglDisplay, 
                                                    GLCapabilitiesImmutable capsChosen, GLCapabilitiesImmutable capsRequested,
                                                    GLCapabilitiesChooser chooser,
                                                    AbstractGraphicsScreen absScreen) {
        GLProfile glp = capsChosen.getGLProfile();
        boolean onscreen = capsChosen.isOnscreen();
        boolean usePBuffer = capsChosen.isPBuffer();
        List/*<EGLGLCapabilities>*/ availableCaps = null;
        final int winattrmask = GLGraphicsConfigurationUtil.getWinAttributeBits(onscreen, usePBuffer);
        int recommendedIndex = -1;
        long recommendedEGLConfig = -1;
        int[] maxConfigs = new int[1];

        if(!EGL.eglGetConfigs(eglDisplay, null, 0, maxConfigs, 0)) {
            throw new GLException("Graphics configuration get maxConfigs (eglGetConfigs) call failed, error "+toHexString(EGL.eglGetError()));
        }
        if(0 == maxConfigs[0]) {
            throw new GLException("Graphics configuration get maxConfigs (eglGetConfigs) no configs");
        }
        if (DEBUG) {
            System.err.println("!!! eglChooseConfig maxConfigs: "+maxConfigs[0]);
        }

        int[] attrs = EGLGraphicsConfiguration.GLCapabilities2AttribList(capsChosen);
        PointerBuffer configs = PointerBuffer.allocateDirect(maxConfigs[0]);
        int[] numConfigs = new int[1];

        // 1st choice: get GLCapabilities based on users GLCapabilities setting recommendedIndex as preferred choice
        if (!EGL.eglChooseConfig(eglDisplay,
                                 attrs, 0,
                                 configs, configs.capacity(),
                                 numConfigs, 0)) {
            throw new GLException("Graphics configuration selection (eglChooseConfig) failed for "+capsChosen+", error "+toHexString(EGL.eglGetError()));
        }
        if (numConfigs[0] > 0) {
            availableCaps = eglConfigs2GLCaps(glp, eglDisplay, configs, numConfigs[0], winattrmask);
            if(availableCaps.size() > 0) {
                recommendedEGLConfig =  configs.get(0);
                recommendedIndex = 0;
                if (DEBUG) {
                    System.err.println("!!! eglChooseConfig recommended fbcfg " + toHexString(recommendedEGLConfig) + ", idx " + recommendedIndex);
                    System.err.println("!!! user  caps " + capsChosen);
                    System.err.println("!!! fbcfg caps " + availableCaps.get(recommendedIndex));
                }
            } else if (DEBUG) {
                System.err.println("!!! eglChooseConfig no caps for recommended fbcfg " + toHexString(configs.get(0)));
                System.err.println("!!! user  caps " + capsChosen);
            }
        }

        // 2nd choice: get all GLCapabilities available, no preferred recommendedIndex available
        if( null == availableCaps || 0 == availableCaps.size() ) {
            // reset ..
            recommendedEGLConfig = -1;
            recommendedIndex = -1;

            if(!EGL.eglGetConfigs(eglDisplay, configs, configs.capacity(), numConfigs, 0)) {
                throw new GLException("Graphics configuration get all configs (eglGetConfigs) call failed, error "+toHexString(EGL.eglGetError()));
            }
            if (numConfigs[0] > 0) {
                availableCaps = eglConfigs2GLCaps(glp, eglDisplay, configs, numConfigs[0], winattrmask);
            }
        }

        if( null == availableCaps || 0 == availableCaps.size() ) {
            if(DEBUG) {
                // FIXME: this happens on a ATI PC Emulation ..
                System.err.println("Graphics configuration 1st choice and 2nd choice failed - no configs");
            }
            return null;
        }

        int chosenIndex = chooseCapabilities(chooser, capsChosen, availableCaps, recommendedIndex);
        if ( 0 > chosenIndex ) {
            if (DEBUG) {
                Thread.dumpStack();
            }
            return null;
        }
        EGLGLCapabilities chosenCaps = (EGLGLCapabilities) availableCaps.get(chosenIndex);

        return new EGLGraphicsConfiguration(absScreen, chosenCaps, capsRequested, chooser);
    }

    static List/*<GLCapabilitiesImmutable>*/ eglConfigs2GLCaps(GLProfile glp, long eglDisplay, PointerBuffer configs, int num, int winattrmask) {
        ArrayList caps = new ArrayList(num);
        for(int i=0; i<num; i++) {
            EGLGraphicsConfiguration.EGLConfig2Capabilities(caps, glp, eglDisplay, configs.get(i), winattrmask);
        }
        return caps;
    }

    static void printCaps(String prefix, List/*GLCapabilitiesImmutable*/ caps, PrintStream out) {
        for(int i=0; i<caps.size(); i++) {
            out.println(prefix+"["+i+"] "+caps.get(i));
        }
    }

    static EGLGraphicsConfiguration createOffscreenGraphicsConfiguration(AbstractGraphicsDevice device, GLCapabilitiesImmutable capsChosen, GLCapabilitiesImmutable capsReq, GLCapabilitiesChooser chooser) {
        if(capsChosen.isOnscreen()) {
            throw new GLException("Error: Onscreen set: "+capsChosen);
        }

        if(capsChosen.getDoubleBuffered()) {
            // OFFSCREEN !DOUBLE_BUFFER // FIXME DBLBUFOFFSCRN
            GLCapabilities caps2 = (GLCapabilities) capsChosen.cloneMutable();
            caps2.setDoubleBuffered(false);
            capsChosen = caps2;
        }

        DefaultGraphicsScreen screen = new DefaultGraphicsScreen(device, 0);
        EGLGraphicsConfiguration eglConfig = chooseGraphicsConfigurationStatic(capsChosen, capsReq, chooser, screen);
        if (null == eglConfig) {
            throw new GLException("Couldn't create EGLGraphicsConfiguration from "+screen);
        } else if(DEBUG) {
            System.err.println("Chosen eglConfig: "+eglConfig);
        }
        return eglConfig;
    }
}


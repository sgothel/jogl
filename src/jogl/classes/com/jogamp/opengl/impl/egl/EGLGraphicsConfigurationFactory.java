/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.jogamp.opengl.impl.egl;

import com.jogamp.common.nio.PointerBuffer;
import java.io.PrintStream;
import javax.media.nativewindow.*;
import javax.media.nativewindow.egl.*;

import javax.media.opengl.*;


/** Subclass of GraphicsConfigurationFactory used when non-AWT tookits
    are used on X11 platforms. Toolkits will likely need to delegate
    to this one to change the accepted and returned types of the
    GraphicsDevice and GraphicsConfiguration abstractions. */

public class EGLGraphicsConfigurationFactory extends GraphicsConfigurationFactory {
    protected static final boolean DEBUG = GraphicsConfigurationFactory.DEBUG || com.jogamp.opengl.impl.Debug.debug("EGL");

    public EGLGraphicsConfigurationFactory() {
        // become the selector for KD/EGL ..
        GraphicsConfigurationFactory.registerFactory(javax.media.nativewindow.egl.EGLGraphicsDevice.class, this);
    }

    protected AbstractGraphicsConfiguration chooseGraphicsConfigurationImpl (
            Capabilities capabilities, CapabilitiesChooser chooser, AbstractGraphicsScreen absScreen) {
        if (absScreen == null) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only AbstractGraphicsDevice objects");
        }

        if (capabilities != null &&
            !(capabilities instanceof GLCapabilities)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects");
        }

        if (chooser != null &&
            !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilitiesChooser objects");
        }

        return chooseGraphicsConfigurationStatic((GLCapabilities) capabilities,
                                                 (GLCapabilitiesChooser) chooser,
                                                 absScreen);
    }

    private static EGLGraphicsConfiguration chooseGraphicsConfigurationStatic(GLCapabilities capabilities,
                                                                             GLCapabilitiesChooser chooser,
                                                                             AbstractGraphicsScreen absScreen) {
        if (capabilities == null) {
            capabilities = new GLCapabilities(null);
        }
        GLProfile glp = capabilities.getGLProfile();

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

        GLCapabilities caps2 = (GLCapabilities) capabilities.clone();
        if(!caps2.isOnscreen()) {
            // OFFSCREEN !DOUBLE_BUFFER // FIXME DBLBUFOFFSCRN
            caps2.setDoubleBuffered(false);
        }

        EGLGraphicsConfiguration res = eglChooseConfig(eglDisplay, caps2, capabilities, chooser, absScreen);
        if(null!=res) {
            return res;
        }
        if(DEBUG) {
            System.err.println("eglChooseConfig failed with given capabilities "+caps2);
        }

        if (chooser == null) {
            chooser = new DefaultGLCapabilitiesChooser();
        }

        PointerBuffer configs = PointerBuffer.allocateDirect(10);
        int[] numConfigs = new int[1];

        if(!EGL.eglGetConfigs(eglDisplay, configs, configs.capacity(), numConfigs, 0)) {
            throw new GLException("Graphics configuration fetch (eglGetConfigs) failed");
        }
        if (numConfigs[0] == 0) {
            throw new GLException("Graphics configuration fetch (eglGetConfigs) - no EGLConfig found");
        }
        GLCapabilities[] caps = eglConfigs2GLCaps(glp, eglDisplay, configs, numConfigs[0],
                                                    caps2.isOnscreen(), caps2.isPBuffer());
        if(DEBUG) {
            System.err.println("EGL Get Configs: "+numConfigs[0]+", Caps "+caps.length);
            printCaps("eglGetConfigs", caps, System.err);
        }
        int chosen = -1;
        try {
            chosen = chooser.chooseCapabilities(caps2, caps, -1);
        } catch (NativeWindowException e) { throw new GLException(e); }
        if(chosen<0) {
            throw new GLException("Graphics configuration chooser failed");
        }
        if(DEBUG) {
            System.err.println("Chosen "+caps[chosen]);
        }
        res = eglChooseConfig(eglDisplay, caps[chosen], capabilities, chooser, absScreen);
        if(null!=res) {
            return res;
        }
        if(DEBUG) {
            System.err.println("eglChooseConfig failed with eglGetConfig/choosen capabilities "+caps[chosen]);
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
        res = eglChooseConfig(eglDisplay, fixedCaps, capabilities, chooser, absScreen);
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
        res = eglChooseConfig(eglDisplay, fixedCaps, capabilities, chooser, absScreen);
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
        res = eglChooseConfig(eglDisplay, fixedCaps, capabilities, chooser, absScreen);
        if(null!=res) {
            return res;
        }
        throw new GLException("Graphics configuration failed [direct caps, eglGetConfig/chooser and fixed-caps(1-3)]");
    }

    protected static EGLGraphicsConfiguration eglChooseConfig(long eglDisplay, 
                                                              GLCapabilities capsChosen0, GLCapabilities capsRequested, GLCapabilitiesChooser chooser,
                                                              AbstractGraphicsScreen absScreen) {
        GLProfile glp = capsChosen0.getGLProfile();
        int[] attrs = EGLGraphicsConfiguration.GLCapabilities2AttribList(capsChosen0);
        PointerBuffer configs = PointerBuffer.allocateDirect(1);
        int[] numConfigs = new int[1];
        if (!EGL.eglChooseConfig(eglDisplay,
                                 attrs, 0,
                                 configs, configs.capacity(),
                                 numConfigs, 0)) {
            throw new GLException("Graphics configuration selection (eglChooseConfig) failed for "+capsChosen0);
        }
        if (numConfigs[0] > 0) {
            if(DEBUG) {
                GLCapabilities[] caps = eglConfigs2GLCaps(glp, eglDisplay, configs, numConfigs[0],
                                                    capsChosen0.isOnscreen(), capsChosen0.isPBuffer());
                System.err.println("EGL Choose Configs: "+numConfigs[0]+", Caps "+caps.length);
                printCaps("eglChooseConfig", caps, System.err);
            }
            int[] val = new int[1];
            // get the configID 
            if(!EGL.eglGetConfigAttrib(eglDisplay, configs.get(0), EGL.EGL_CONFIG_ID, val, 0)) {
                if(DEBUG) {
                    // FIXME: this happens on a ATI PC Emulation ..
                    System.err.println("EGL couldn't retrieve ConfigID for already chosen eglConfig "+capsChosen0+", error 0x"+Integer.toHexString(EGL.eglGetError()));
                }
                return null;
            }
            GLCapabilities capsChosen1 = EGLGraphicsConfiguration.EGLConfig2Capabilities(glp, eglDisplay, configs.get(0),
                                                                    true, capsChosen0.isOnscreen(), capsChosen0.isPBuffer());
            if(null!=capsChosen1) {
                if(DEBUG) {
                    System.err.println("eglChooseConfig found: eglDisplay 0x"+Long.toHexString(eglDisplay)+
                                                            ", eglConfig ID 0x"+Integer.toHexString(val[0])+
                                                            ", "+capsChosen0+" -> "+capsChosen1);
                }
                return new EGLGraphicsConfiguration(absScreen, capsChosen1, capsRequested, chooser, configs.get(0), val[0]);
            }
            if(DEBUG) {
                System.err.println("eglChooseConfig couldn't verify: eglDisplay 0x"+Long.toHexString(eglDisplay)+
                                                                        ", eglConfig ID 0x"+Integer.toHexString(val[0])+
                                                                        ", for "+capsChosen0);
            }
        } else {
            if(DEBUG) {
                System.err.println("EGL Choose Configs: None using eglDisplay 0x"+Long.toHexString(eglDisplay)+
                                                                ", "+capsChosen0);
            }
        }
        return null;
    }

    protected static GLCapabilities[] eglConfigs2GLCaps(GLProfile glp, long eglDisplay, PointerBuffer configs, int num,
                                                        boolean onscreen, boolean usePBuffer) {
        GLCapabilities[] caps = new GLCapabilities[num];
        for(int i=0; i<num; i++) {
            caps[i] = EGLGraphicsConfiguration.EGLConfig2Capabilities(glp, eglDisplay, configs.get(i),
                                            true, onscreen, usePBuffer);
        }
        return caps;
    }

    protected static void printCaps(String prefix, GLCapabilities[] caps, PrintStream out) {
        for(int i=0; i<caps.length; i++) {
            out.println(prefix+"["+i+"] "+caps[i]);
        }
    }

    protected static EGLGraphicsConfiguration createOffscreenGraphicsConfiguration(GLCapabilities caps, GLCapabilitiesChooser chooser) {
        if(caps.isOnscreen()) {
            throw new GLException("Error: Onscreen set: "+caps);
        }
        caps.setDoubleBuffered(false); // FIXME DBLBUFOFFSCRN
        long eglDisplay = EGL.eglGetDisplay(EGL.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL.EGL_NO_DISPLAY) {
            throw new GLException("Failed to created EGL default display: error 0x"+Integer.toHexString(EGL.eglGetError()));
        } else if(DEBUG) {
            System.err.println("eglDisplay(EGL_DEFAULT_DISPLAY): 0x"+Long.toHexString(eglDisplay));
        }
        if (!EGL.eglInitialize(eglDisplay, null, null)) {
            throw new GLException("eglInitialize failed"+", error 0x"+Integer.toHexString(EGL.eglGetError()));
        }
        EGLGraphicsDevice e = new EGLGraphicsDevice(eglDisplay);
        DefaultGraphicsScreen s = new DefaultGraphicsScreen(e, 0);
        EGLGraphicsConfiguration eglConfig = chooseGraphicsConfigurationStatic(caps, chooser, s);
        if (null == eglConfig) {
            EGL.eglTerminate(eglDisplay);
            throw new GLException("Couldn't create EGLGraphicsConfiguration from "+s);
        } else if(DEBUG) {
            System.err.println("Chosen eglConfig: "+eglConfig);
        }
        return eglConfig;
    }
}


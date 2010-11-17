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
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.jogamp.opengl.impl.egl;

import com.jogamp.common.nio.PointerBuffer;
import javax.media.nativewindow.*;
import javax.media.nativewindow.egl.*;
import javax.media.opengl.*;
import com.jogamp.opengl.impl.*;

public class EGLGraphicsConfiguration extends DefaultGraphicsConfiguration implements Cloneable {
    protected static final boolean DEBUG = Debug.debug("GraphicsConfiguration");

    public long getNativeConfig() {
        return config;
    }

    public int getNativeConfigID() {
        return configID;
    }

    public EGLGraphicsConfiguration(AbstractGraphicsScreen absScreen, 
                                    GLCapabilitiesImmutable capsChosen, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser,
                                    long cfg, int cfgID) {
        super(absScreen, capsChosen, capsRequested);
        this.chooser = chooser;
        config = cfg;
        configID = cfgID;
    }

    public static EGLGraphicsConfiguration create(GLCapabilitiesImmutable capsRequested, AbstractGraphicsScreen absScreen, int cfgID) {
        AbstractGraphicsDevice absDevice = absScreen.getDevice();
        if(null==absDevice || !(absDevice instanceof EGLGraphicsDevice)) {
            throw new GLException("GraphicsDevice must be a valid EGLGraphicsDevice");
        }
        long dpy = absDevice.getHandle();
        if (dpy == EGL.EGL_NO_DISPLAY) {
            throw new GLException("Invalid EGL display: "+absDevice);
        }
        GLProfile glp = capsRequested.getGLProfile();
        long cfg = EGLConfigId2EGLConfig(glp, dpy, cfgID);
        GLCapabilitiesImmutable caps = EGLConfig2Capabilities(glp, dpy, cfg, false, capsRequested.isOnscreen(), capsRequested.isPBuffer());
        return new EGLGraphicsConfiguration(absScreen, caps, capsRequested, new DefaultGLCapabilitiesChooser(), cfg, cfgID);
    }

    public Object clone() {
        return super.clone();
    }

    protected void updateGraphicsConfiguration() {
        EGLGraphicsConfiguration newConfig = (EGLGraphicsConfiguration)
            GraphicsConfigurationFactory.getFactory(getScreen().getDevice()).chooseGraphicsConfiguration(
                getChosenCapabilities(), getRequestedCapabilities(), chooser, getScreen());
        if(null!=newConfig) {
            // FIXME: setScreen( ... );
            setChosenCapabilities(newConfig.getChosenCapabilities());
            config = newConfig.getNativeConfig();
            configID = newConfig.getNativeConfigID();
            if(DEBUG) {
                System.err.println("!!! updateGraphicsConfiguration: "+this);
            }
        }
    }

    public static long EGLConfigId2EGLConfig(GLProfile glp, long display, int configID) {
        int[] attrs = new int[] {
                EGL.EGL_CONFIG_ID, configID,
                EGL.EGL_NONE
            };
        PointerBuffer configs = PointerBuffer.allocateDirect(1);
        int[] numConfigs = new int[1];
        if (!EGL.eglChooseConfig(display,
                                 attrs, 0,
                                 configs, 1,
                                 numConfigs, 0)) {
            return 0;
        }
        if (numConfigs[0] == 0) {
            return 0;
        }
        return configs.get(0);
    }

    public static boolean EGLConfigDrawableTypeVerify(int val, boolean onscreen, boolean usePBuffer) {
        boolean res;

        if ( onscreen ) {
            res = ( 0 != (val & EGL.EGL_WINDOW_BIT) ) ;
        } else {
            if ( usePBuffer ) {
                res = ( 0 != (val & EGL.EGL_PBUFFER_BIT) ) ;
            } else {
                res = ( 0 != (val & EGL.EGL_PIXMAP_BIT) ) ;
            }
        }

        return res;
    }

    public static GLCapabilitiesImmutable EGLConfig2Capabilities(GLProfile glp, long display, long config,
                                                                 boolean relaxed, boolean onscreen, boolean usePBuffer) {
        GLCapabilities caps = new GLCapabilities(glp);
        int[] val = new int[1];

        // Read the actual configuration into the choosen caps
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_RED_SIZE, val, 0)) {
            caps.setRedBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_GREEN_SIZE, val, 0)) {
            caps.setGreenBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_BLUE_SIZE, val, 0)) {
            caps.setBlueBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_ALPHA_SIZE, val, 0)) {
            caps.setAlphaBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_STENCIL_SIZE, val, 0)) {
            caps.setStencilBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_DEPTH_SIZE, val, 0)) {
            caps.setDepthBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_SAMPLES, val, 0)) {
            caps.setSampleBuffers(val[0]>0?true:false);
            caps.setNumSamples(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_TRANSPARENT_TYPE, val, 0)) {
            caps.setBackgroundOpaque(val[0] != EGL.EGL_TRANSPARENT_RGB);
        }
        if(!caps.isBackgroundOpaque()) {
            if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_TRANSPARENT_RED_VALUE, val, 0)) {
                caps.setTransparentRedValue(val[0]==EGL.EGL_DONT_CARE?-1:val[0]);
            }
            if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_TRANSPARENT_GREEN_VALUE, val, 0)) {
                caps.setTransparentGreenValue(val[0]==EGL.EGL_DONT_CARE?-1:val[0]);
            }
            if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_TRANSPARENT_BLUE_VALUE, val, 0)) {
                caps.setTransparentBlueValue(val[0]==EGL.EGL_DONT_CARE?-1:val[0]);
            }
            /** Not defined in EGL 
            if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_TRANSPARENT_ALPHA_VALUE, val, 0)) {
                caps.setTransparentAlphaValue(val[0]==EGL.EGL_DONT_CARE?-1:val[0]);
            } */
        }
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_SURFACE_TYPE, val, 0)) {
            if(EGLConfigDrawableTypeVerify(val[0], onscreen, usePBuffer)) {
                caps.setDoubleBuffered(onscreen);
                caps.setOnscreen(onscreen);
                caps.setPBuffer(usePBuffer);
            } else if(relaxed) {
                caps.setDoubleBuffered( 0 != (val[0] & EGL.EGL_WINDOW_BIT) );
                caps.setOnscreen( 0 != (val[0] & EGL.EGL_WINDOW_BIT) );
                caps.setPBuffer ( 0 != (val[0] & EGL.EGL_PBUFFER_BIT) );
            } else {
                if(DEBUG) {
                  System.err.println("EGL_SURFACE_TYPE does not match: req(onscrn "+onscreen+", pbuffer "+usePBuffer+"), got(onscreen "+( 0 != (val[0] & EGL.EGL_WINDOW_BIT) )+", pbuffer "+( 0 != (val[0] & EGL.EGL_PBUFFER_BIT) )+", pixmap "+( 0 != (val[0] & EGL.EGL_PIXMAP_BIT) )+")");
                }
                return null;
            }
        } else {
            throw new GLException("Could not determine EGL_SURFACE_TYPE !!!");
        }

        return caps;
    }

    public static int[] GLCapabilities2AttribList(GLCapabilitiesImmutable caps) {
        int[] attrs = new int[32];
        int idx=0;

        attrs[idx++] = EGL.EGL_SURFACE_TYPE;
        attrs[idx++] = caps.isOnscreen() ? ( EGL.EGL_WINDOW_BIT ) : ( caps.isPBuffer() ? EGL.EGL_PBUFFER_BIT : EGL.EGL_PIXMAP_BIT ) ;

        attrs[idx++] = EGL.EGL_RED_SIZE;
        attrs[idx++] = caps.getRedBits();

        attrs[idx++] = EGL.EGL_GREEN_SIZE;
        attrs[idx++] = caps.getGreenBits();

        attrs[idx++] = EGL.EGL_BLUE_SIZE;
        attrs[idx++] = caps.getBlueBits();

        attrs[idx++] = EGL.EGL_ALPHA_SIZE;
        attrs[idx++] = caps.getAlphaBits() > 0 ? caps.getAlphaBits() : EGL.EGL_DONT_CARE;

        attrs[idx++] = EGL.EGL_STENCIL_SIZE;
        attrs[idx++] = caps.getStencilBits() > 0 ? caps.getStencilBits() : EGL.EGL_DONT_CARE;

        attrs[idx++] = EGL.EGL_DEPTH_SIZE;
        attrs[idx++] = caps.getDepthBits();

        attrs[idx++] = EGL.EGL_SAMPLES;
        attrs[idx++] = caps.getSampleBuffers() ? caps.getNumSamples() : 1;

        attrs[idx++] = EGL.EGL_TRANSPARENT_TYPE;
        attrs[idx++] = caps.isBackgroundOpaque() ? EGL.EGL_NONE : EGL.EGL_TRANSPARENT_TYPE;

        // 20

        if(!caps.isBackgroundOpaque()) {
            attrs[idx++] = EGL.EGL_TRANSPARENT_RED_VALUE;
            attrs[idx++] = caps.getTransparentRedValue()>=0?caps.getTransparentRedValue():EGL.EGL_DONT_CARE;

            attrs[idx++] = EGL.EGL_TRANSPARENT_GREEN_VALUE;
            attrs[idx++] = caps.getTransparentGreenValue()>=0?caps.getTransparentGreenValue():EGL.EGL_DONT_CARE;

            attrs[idx++] = EGL.EGL_TRANSPARENT_BLUE_VALUE;
            attrs[idx++] = caps.getTransparentBlueValue()>=0?caps.getTransparentBlueValue():EGL.EGL_DONT_CARE;

            /** Not define in EGL
            attrs[idx++] = EGL.EGL_TRANSPARENT_ALPHA_VALUE;
            attrs[idx++] = caps.getTransparentAlphaValue()>=0?caps.getTransparentAlphaValue():EGL.EGL_DONT_CARE; */
        }

        // 26 

        attrs[idx++] = EGL.EGL_RENDERABLE_TYPE;
        if(caps.getGLProfile().usesNativeGLES1()) {
            attrs[idx++] = EGL.EGL_OPENGL_ES_BIT;
        }
        else if(caps.getGLProfile().usesNativeGLES2()) {
            attrs[idx++] = EGL.EGL_OPENGL_ES2_BIT;
        } else {
            attrs[idx++] = EGL.EGL_OPENGL_BIT;
        }

        // 28

        attrs[idx++] = EGL.EGL_NONE;

        return attrs;
    }

    public static int[] CreatePBufferSurfaceAttribList(int width, int height, int texFormat) {
        int[] attrs = new int[16];
        int idx=0;

        attrs[idx++] = EGL.EGL_WIDTH;
        attrs[idx++] = width;

        attrs[idx++] = EGL.EGL_HEIGHT;
        attrs[idx++] = height;

        attrs[idx++] = EGL.EGL_TEXTURE_FORMAT;
        attrs[idx++] = texFormat;

        attrs[idx++] = EGL.EGL_TEXTURE_TARGET;
        attrs[idx++] = EGL.EGL_NO_TEXTURE==texFormat ? EGL.EGL_NO_TEXTURE : EGL.EGL_TEXTURE_2D;

        attrs[idx++] = EGL.EGL_NONE;

        return attrs;
    }

    public String toString() {
        return getClass().toString()+"["+getScreen()+", eglConfigID 0x"+Integer.toHexString(configID)+
                                     ",\n\trequested " + getRequestedCapabilities()+
                                     ",\n\tchosen    " + getChosenCapabilities()+
                                     "]";

    }

    private GLCapabilitiesChooser chooser;
    private long config;
    private int configID;
}


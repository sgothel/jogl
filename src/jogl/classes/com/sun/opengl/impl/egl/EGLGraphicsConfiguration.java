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
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.impl.egl;

import java.util.*;
import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.gluegen.runtime.NativeLibrary;

public class EGLGraphicsConfiguration extends DefaultGraphicsConfiguration implements Cloneable {
    protected static final boolean DEBUG = Debug.debug("GraphicsConfiguration");

    public _EGLConfig getNativeConfig() {
        return _config;
    }

    public int getNativeConfigID() {
        return configID;
    }

    public EGLGraphicsConfiguration(AbstractGraphicsScreen screen, 
                                    GLCapabilities capsChosen, GLCapabilities capsRequested, GLCapabilitiesChooser chooser,
                                    _EGLConfig cfg, int cfgID) {
        super(screen, capsChosen, capsRequested);
        this.chooser = chooser;
        _config = cfg;
        configID = cfgID;
    }

    public Object clone() {
        return super.clone();
    }

    protected void updateGraphicsConfiguration() {
        EGLGraphicsConfiguration newConfig = (EGLGraphicsConfiguration)
            GraphicsConfigurationFactory.getFactory(getScreen().getDevice()).chooseGraphicsConfiguration(getRequestedCapabilities(),
                                                                                                         chooser,
                                                                                                         getScreen());
        if(null!=newConfig) {
            // FIXME: setScreen( ... );
            setChosenCapabilities(newConfig.getChosenCapabilities());
            _config = newConfig.getNativeConfig();
            configID = newConfig.getNativeConfigID();
            if(DEBUG) {
                System.err.println("!!! updateGraphicsConfiguration: "+this);
            }
        }
    }

    public static _EGLConfig EGLConfigId2EGLConfig(GLProfile glp, long display, int configID) {
        int[] attrs = new int[] {
                EGL.EGL_CONFIG_ID, configID,
                EGL.EGL_NONE
            };
        _EGLConfig[] configs = new _EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL.eglChooseConfig(display,
                                 attrs, 0,
                                 configs, 1,
                                 numConfigs, 0)) {
            return null;
        }
        if (numConfigs[0] == 0) {
            return null;
        }
        return configs[0];
    }

    public static GLCapabilities EGLConfig2Capabilities(GLProfile glp, long display, _EGLConfig _config) {
        GLCapabilities caps = new GLCapabilities(glp);
        int[] val = new int[1];

        // Read the actual configuration into the choosen caps
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_RED_SIZE, val, 0)) {
            caps.setRedBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_GREEN_SIZE, val, 0)) {
            caps.setGreenBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_BLUE_SIZE, val, 0)) {
            caps.setBlueBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_ALPHA_SIZE, val, 0)) {
            caps.setAlphaBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_STENCIL_SIZE, val, 0)) {
            caps.setStencilBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_DEPTH_SIZE, val, 0)) {
            caps.setDepthBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_SAMPLES, val, 0)) {
            caps.setSampleBuffers(val[0]>0?true:false);
            caps.setNumSamples(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_TRANSPARENT_TYPE, val, 0)) {
            caps.setBackgroundOpaque(val[0] != EGL.EGL_TRANSPARENT_RGB);
        }
        if(!caps.isBackgroundOpaque()) {
            if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_TRANSPARENT_RED_VALUE, val, 0)) {
                caps.setTransparentRedValue(val[0]==EGL.EGL_DONT_CARE?-1:val[0]);
            }
            if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_TRANSPARENT_GREEN_VALUE, val, 0)) {
                caps.setTransparentGreenValue(val[0]==EGL.EGL_DONT_CARE?-1:val[0]);
            }
            if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_TRANSPARENT_BLUE_VALUE, val, 0)) {
                caps.setTransparentBlueValue(val[0]==EGL.EGL_DONT_CARE?-1:val[0]);
            }
            /** Not defined in EGL 
            if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_TRANSPARENT_ALPHA_VALUE, val, 0)) {
                caps.setTransparentAlphaValue(val[0]==EGL.EGL_DONT_CARE?-1:val[0]);
            } */
        }
        return caps;
    }

    public static int[] GLCapabilities2AttribList(GLCapabilities caps, int eglSurfaceType) {
        int[] attrs = new int[32];
        int idx=0;

        switch(eglSurfaceType) {
            case EGL.EGL_WINDOW_BIT:
            case EGL.EGL_PBUFFER_BIT:
            case EGL.EGL_PIXMAP_BIT:
                break;
            default:
                throw new GLException("Invalid EGL_SURFACE_TYPE 0x"+Integer.toHexString(eglSurfaceType));
        }

        attrs[idx++] = EGL.EGL_SURFACE_TYPE;
        attrs[idx++] = eglSurfaceType;

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
        return getClass().toString()+"["+getScreen()+", eglConfigID "+configID+
                                     ",\n\trequested " + getRequestedCapabilities()+
                                     ",\n\tchosen    " + getChosenCapabilities()+
                                     "]";

    }

    private GLCapabilitiesChooser chooser;
    private _EGLConfig _config;
    private int configID;
}


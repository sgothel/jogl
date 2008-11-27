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
import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.gluegen.runtime.NativeLibrary;

public class EGLConfig {
    
    public _EGLConfig getNativeConfig() {
        return _config;
    }

    public int getNativeConfigID() {
        return configID;
    }

    public GLCapabilities getCapabilities() {
        return capabilities;
    }

    public int[] getAttributeList() {
        return glCapabilities2AttribList(capabilities);
    }

    public EGLConfig(long display, int configID) {
        int[] attrs = new int[] {
                EGL.EGL_RENDERABLE_TYPE, -1,
                EGL.EGL_CONFIG_ID, configID,
                EGL.EGL_NONE
            };
        if (GLProfile.isGLES2()) {
            attrs[1] = EGL.EGL_OPENGL_ES2_BIT;
        } else if (GLProfile.isGLES1()) {
            attrs[1] = EGL.EGL_OPENGL_ES_BIT;
        } else {
            throw new GLException("Error creating EGL drawable - invalid GLProfile");
        }
        _EGLConfig[] configs = new _EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL.eglChooseConfig(display,
                                 attrs, 0,
                                 configs, 1,
                                 numConfigs, 0)) {
            throw new GLException("Graphics configuration selection (eglChooseConfig) failed");
        }
        if (numConfigs[0] == 0) {
            throw new GLException("No valid graphics configuration selected from eglChooseConfig");
        }
        capabilities = new GLCapabilities();
        setup(display, configID, configs[0]);
    }

    public EGLConfig(long display, GLCapabilities caps) {
        int[] attrs = glCapabilities2AttribList(caps);
        _EGLConfig[] configs = new _EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL.eglChooseConfig(display,
                                 attrs, 0,
                                 configs, 1,
                                 numConfigs, 0)) {
            throw new GLException("Graphics configuration selection (eglChooseConfig) failed");
        }
        if (numConfigs[0] == 0) {
            throw new GLException("No valid graphics configuration selected from eglChooseConfig");
        }
        capabilities = (GLCapabilities)caps.clone();
        setup(display, -1, configs[0]);
    }

    private void setup(long display, int setConfigID, _EGLConfig _config) {
        this._config = _config;
        int[] val = new int[1];
        // get the configID 
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_CONFIG_ID, val, 0)) {
            configID = val[0];
            if( setConfigID>=0 && setConfigID!=this.configID ) {
                throw new GLException("EGL ConfigID mismatch, ask "+setConfigID+", got "+configID);
            }
        } else {
            throw new GLException("EGL couldn't retrieve ConfigID");
        }
        // Read the actual configuration into the choosen caps
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_RED_SIZE, val, 0)) {
            capabilities.setRedBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_GREEN_SIZE, val, 0)) {
            capabilities.setGreenBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_BLUE_SIZE, val, 0)) {
            capabilities.setBlueBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_ALPHA_SIZE, val, 0)) {
            capabilities.setAlphaBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_STENCIL_SIZE, val, 0)) {
            capabilities.setStencilBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_DEPTH_SIZE, val, 0)) {
            capabilities.setDepthBits(val[0]);
        }
    }

    public static int[] glCapabilities2AttribList(GLCapabilities caps) {
        int[] attrs = new int[] {
                EGL.EGL_RENDERABLE_TYPE, -1,
                // FIXME: does this need to be configurable?
                EGL.EGL_SURFACE_TYPE,    EGL.EGL_WINDOW_BIT,
                EGL.EGL_RED_SIZE,        caps.getRedBits(),
                EGL.EGL_GREEN_SIZE,      caps.getGreenBits(),
                EGL.EGL_BLUE_SIZE,       caps.getBlueBits(),
                EGL.EGL_ALPHA_SIZE,      (caps.getAlphaBits() > 0 ? caps.getAlphaBits() : EGL.EGL_DONT_CARE),
                EGL.EGL_STENCIL_SIZE,    (caps.getStencilBits() > 0 ? caps.getStencilBits() : EGL.EGL_DONT_CARE),
                EGL.EGL_DEPTH_SIZE,      caps.getDepthBits(),
                EGL.EGL_NONE
            };
        if (GLProfile.isGLES2()) {
            attrs[1] = EGL.EGL_OPENGL_ES2_BIT;
        } else if (GLProfile.isGLES1()) {
            attrs[1] = EGL.EGL_OPENGL_ES_BIT;
        } else {
            throw new GLException("Error creating EGL drawable - invalid GLProfile");
        }

        return attrs;
    }

    public String toString() {
        return "EGLConfig[ id "+configID+
                           ", "+capabilities+"]";
    }
    private _EGLConfig _config;
    private int configID;
    private GLCapabilities capabilities;
}


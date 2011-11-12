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

package jogamp.opengl.egl;

import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.egl.EGLGraphicsDevice;
import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import jogamp.nativewindow.MutableGraphicsConfiguration;
import jogamp.opengl.Debug;
import jogamp.opengl.GLGraphicsConfigurationUtil;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;

public class EGLGraphicsConfiguration extends MutableGraphicsConfiguration implements Cloneable {
    protected static final boolean DEBUG = Debug.debug("GraphicsConfiguration");

    public final long getNativeConfig() {
        return ((EGLGLCapabilities)capabilitiesChosen).getEGLConfig();
    }

    public final int getNativeConfigID() {
        return ((EGLGLCapabilities)capabilitiesChosen).getEGLConfigID();
    }

    EGLGraphicsConfiguration(AbstractGraphicsScreen absScreen, 
                             EGLGLCapabilities capsChosen, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser) {
        super(absScreen, capsChosen, capsRequested);
        this.chooser = chooser;
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
        EGLGLCapabilities caps = EGLConfig2Capabilities(glp, dpy, cfg, false, capsRequested.isOnscreen(), capsRequested.isPBuffer());
        caps = (EGLGLCapabilities) GLGraphicsConfigurationUtil.fixOpaqueGLCapabilities(caps, capsRequested.isBackgroundOpaque()); // FIXME: valid to override EGL transparency ?
        return new EGLGraphicsConfiguration(absScreen, caps, capsRequested, new DefaultGLCapabilitiesChooser());
    }

    @Override
    public Object clone() {
        return super.clone();
    }
        
    void updateGraphicsConfiguration() {
        EGLGraphicsConfiguration newConfig = (EGLGraphicsConfiguration)
            GraphicsConfigurationFactory.getFactory(getScreen().getDevice()).chooseGraphicsConfiguration(
                getChosenCapabilities(), getRequestedCapabilities(), chooser, getScreen());
        if(null!=newConfig) {
            // FIXME: setScreen( ... );
            setChosenCapabilities(newConfig.getChosenCapabilities());
            if(DEBUG) {
                System.err.println("!!! updateGraphicsConfiguration(1): "+this);
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

    static int EGLConfigDrawableTypeBits(final long display, final long config) {
        int val = 0;

        int[] stype = new int[1];
        if(! EGL.eglGetConfigAttrib(display, config, EGL.EGL_SURFACE_TYPE, stype, 0)) {
            throw new GLException("Could not determine EGL_SURFACE_TYPE !!!");
        }

        if ( 0 != ( stype[0] & EGL.EGL_WINDOW_BIT ) ) {
            val |= GLGraphicsConfigurationUtil.WINDOW_BIT;
        }
        if ( 0 != ( stype[0] & EGL.EGL_PIXMAP_BIT ) ) {
            val |= GLGraphicsConfigurationUtil.BITMAP_BIT;
        }
        if ( 0 != ( stype[0] & EGL.EGL_PBUFFER_BIT ) ) {
            val |= GLGraphicsConfigurationUtil.PBUFFER_BIT;
        }

        return val;
    }

    public static EGLGLCapabilities EGLConfig2Capabilities(GLProfile glp, long display, long config,
                                                           boolean relaxed, boolean onscreen, boolean usePBuffer) {
        ArrayList bucket = new ArrayList();
        final int winattrmask = GLGraphicsConfigurationUtil.getWinAttributeBits(onscreen, usePBuffer);
        if( EGLConfig2Capabilities(bucket, glp, display, config, winattrmask) ) {
            return (EGLGLCapabilities) bucket.get(0);
        } else if ( relaxed && EGLConfig2Capabilities(bucket, glp, display, config, GLGraphicsConfigurationUtil.ALL_BITS) ) {
            return (EGLGLCapabilities) bucket.get(0);
        }
        return null;
    }

    public static boolean EGLConfig2Capabilities(ArrayList capsBucket,
                                                 GLProfile glp, long display, long config,
                                                 int winattrmask) {
        final int allDrawableTypeBits = EGLConfigDrawableTypeBits(display, config);
        final int drawableTypeBits = winattrmask & allDrawableTypeBits;

        if( 0 == drawableTypeBits ) {
            return false;
        }

        final IntBuffer val = Buffers.newDirectIntBuffer(1);
        final int cfgID;
        final int rType;
        
        // get the configID
        if(!EGL.eglGetConfigAttrib(display, config, EGL.EGL_CONFIG_ID, val)) {
            if(DEBUG) {
                // FIXME: this happens on a ATI PC Emulation ..
                System.err.println("EGL couldn't retrieve ConfigID for config "+toHexString(config)+", error "+toHexString(EGL.eglGetError()));
            }
            return false;
        }
        cfgID = val.get(0);
        
        if(!EGL.eglGetConfigAttrib(display, config, EGL.EGL_RENDERABLE_TYPE, val)) {
            if(DEBUG) {
                System.err.println("EGL couldn't retrieve EGL_RENDERABLE_TYPE for config "+toHexString(config)+", error "+toHexString(EGL.eglGetError()));
            }
            return false;
        }
        rType = val.get(0);
        
        EGLGLCapabilities caps = null;        
        try {
            caps = new EGLGLCapabilities(config, cfgID, glp, rType);
        } catch (GLException gle) {
            if(DEBUG) {
                System.err.println("config "+toHexString(config)+": "+gle);
            }
            return false;
        }        
                
        // Read the actual configuration into the chosen caps
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_NATIVE_VISUAL_ID, val)) {
            caps.setNativeVisualID(val.get(0));
        }                
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_RED_SIZE, val)) {
            caps.setRedBits(val.get(0));
        }
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_GREEN_SIZE, val)) {
            caps.setGreenBits(val.get(0));
        }
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_BLUE_SIZE, val)) {
            caps.setBlueBits(val.get(0));
        }
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_ALPHA_SIZE, val)) {
            caps.setAlphaBits(val.get(0));
        }
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_STENCIL_SIZE, val)) {
            caps.setStencilBits(val.get(0));
        }
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_DEPTH_SIZE, val)) {
            caps.setDepthBits(val.get(0));
        }
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_SAMPLES, val)) {
            caps.setSampleBuffers(val.get(0)>0?true:false);
            caps.setNumSamples(val.get(0));
        }
        if(!caps.getSampleBuffers()) {
            // try NV_coverage_sample extension 
            if(EGL.eglGetConfigAttrib(display, config, EGLExt.EGL_COVERAGE_BUFFERS_NV, val)) {
                if(val.get(0)>0 &&
                   EGL.eglGetConfigAttrib(display, config, EGLExt.EGL_COVERAGE_SAMPLES_NV, val)) {
                    caps.setSampleExtension(GLGraphicsConfigurationUtil.NV_coverage_sample); 
                    caps.setSampleBuffers(true);
                    caps.setNumSamples(val.get(0));
                }
            }
        }
        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_TRANSPARENT_TYPE, val)) {
            caps.setBackgroundOpaque(val.get(0) != EGL.EGL_TRANSPARENT_RGB);
        }
        if(!caps.isBackgroundOpaque()) {
            if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_TRANSPARENT_RED_VALUE, val)) {
                caps.setTransparentRedValue(val.get(0)==EGL.EGL_DONT_CARE?-1:val.get(0));
            }
            if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_TRANSPARENT_GREEN_VALUE, val)) {
                caps.setTransparentGreenValue(val.get(0)==EGL.EGL_DONT_CARE?-1:val.get(0));
            }
            if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_TRANSPARENT_BLUE_VALUE, val)) {
                caps.setTransparentBlueValue(val.get(0)==EGL.EGL_DONT_CARE?-1:val.get(0));
            }
            /** Not defined in EGL 
            if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_TRANSPARENT_ALPHA_VALUE, val)) {
                caps.setTransparentAlphaValue(val.get(0)==EGL.EGL_DONT_CARE?-1:val.get(0));
            } */
        }
        return GLGraphicsConfigurationUtil.addGLCapabilitiesPermutations(capsBucket, caps, drawableTypeBits );
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

        if(caps.getSampleBuffers()) {
            if(caps.getSampleExtension().equals(GLGraphicsConfigurationUtil.NV_coverage_sample)) {
                attrs[idx++] = EGLExt.EGL_COVERAGE_BUFFERS_NV;
                attrs[idx++] = 1;
                attrs[idx++] = EGLExt.EGL_COVERAGE_SAMPLES_NV;
                attrs[idx++] = caps.getNumSamples();
            } else {
                // try default ..
                attrs[idx++] = EGL.EGL_SAMPLE_BUFFERS;
                attrs[idx++] = 1;
                attrs[idx++] = EGL.EGL_SAMPLES;
                attrs[idx++] = caps.getNumSamples();
            }
        }

        attrs[idx++] = EGL.EGL_TRANSPARENT_TYPE;
        attrs[idx++] = caps.isBackgroundOpaque() ? EGL.EGL_NONE : EGL.EGL_TRANSPARENT_TYPE;

        // 22

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

        // 28 
        attrs[idx++] = EGL.EGL_RENDERABLE_TYPE;
        if(caps.getGLProfile().usesNativeGLES1()) {
            attrs[idx++] = EGL.EGL_OPENGL_ES_BIT;
        } else if(caps.getGLProfile().usesNativeGLES2()) {
            attrs[idx++] = EGL.EGL_OPENGL_ES2_BIT;
        } else {
            attrs[idx++] = EGL.EGL_OPENGL_BIT;
        }

        // 30

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

    @Override
    public String toString() {
        return getClass().getSimpleName()+"["+getScreen()+", eglConfigID "+toHexString(getNativeConfigID())+
                                     ",\n\trequested " + getRequestedCapabilities()+
                                     ",\n\tchosen    " + getChosenCapabilities()+
                                     "]";

    }

    private GLCapabilitiesChooser chooser;
}


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

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.VisualIDHolder;
import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import jogamp.opengl.GLGraphicsConfigurationUtil;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.nativewindow.MutableGraphicsConfiguration;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;

public class EGLGraphicsConfiguration extends MutableGraphicsConfiguration implements Cloneable {

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

    /**
     * @param capsRequested
     * @param absScreen
     * @param eglConfigID {@link EGL#EGL_CONFIG_ID} for which the config is being created for.
     * @return
     * @throws GLException if invalid EGL display.
     */
    public static EGLGraphicsConfiguration create(GLCapabilitiesImmutable capsRequested, AbstractGraphicsScreen absScreen, int eglConfigID) {
        final AbstractGraphicsDevice absDevice = absScreen.getDevice();
        if(null==absDevice || !(absDevice instanceof EGLGraphicsDevice)) {
            throw new GLException("GraphicsDevice must be a valid EGLGraphicsDevice");
        }
        final long dpy = absDevice.getHandle();
        if (dpy == EGL.EGL_NO_DISPLAY) {
            throw new GLException("Invalid EGL display: "+absDevice);
        }
        final long cfg = EGLConfigId2EGLConfig(dpy, eglConfigID);
        if(0 < cfg) {
            final int winattrmask = GLGraphicsConfigurationUtil.getExclusiveWinAttributeBits(capsRequested);
            final EGLGLCapabilities caps = EGLConfig2Capabilities((EGLGraphicsDevice)absDevice, capsRequested.getGLProfile(), cfg, winattrmask, false);
            return new EGLGraphicsConfiguration(absScreen, caps, capsRequested, new DefaultGLCapabilitiesChooser());
        }
        return null;
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    void updateGraphicsConfiguration() {
        CapabilitiesImmutable capsChosen = getChosenCapabilities();
        EGLGraphicsConfiguration newConfig = (EGLGraphicsConfiguration)
            GraphicsConfigurationFactory.getFactory(getScreen().getDevice(), capsChosen).chooseGraphicsConfiguration(
                capsChosen, getRequestedCapabilities(), chooser, getScreen(), VisualIDHolder.VID_UNDEFINED);
        if(null!=newConfig) {
            // FIXME: setScreen( ... );
            setChosenCapabilities(newConfig.getChosenCapabilities());
            if(DEBUG) {
                System.err.println("updateGraphicsConfiguration(1): "+this);
            }
        }
    }

    public static long EGLConfigId2EGLConfig(long display, int configID) {
        final IntBuffer attrs = Buffers.newDirectIntBuffer(new int[] {
                EGL.EGL_CONFIG_ID, configID,
                EGL.EGL_NONE
            });
        final PointerBuffer configs = PointerBuffer.allocateDirect(1);
        final IntBuffer numConfigs = Buffers.newDirectIntBuffer(1);
        if (!EGL.eglChooseConfig(display,
                                 attrs,
                                 configs, 1,
                                 numConfigs)) {
            return 0;
        }
        if (numConfigs.get(0) == 0) {
            return 0;
        }
        return configs.get(0);
    }

    public static boolean isEGLConfigValid(long display, long config) {
        if(0 == config) {
            return false;
        }
        final IntBuffer val = Buffers.newDirectIntBuffer(1);

        // get the configID
        if(!EGL.eglGetConfigAttrib(display, config, EGL.EGL_CONFIG_ID, val)) {
            final int eglErr = EGL.eglGetError();
            if(DEBUG) {
                System.err.println("Info: Couldn't retrieve EGL ConfigID for config "+toHexString(config)+", error "+toHexString(eglErr));
            }
            return false;
        }
        return true;
    }

    static int EGLConfigDrawableTypeBits(final EGLGraphicsDevice device, final long config) {
        int val = 0;

        final IntBuffer stype = Buffers.newDirectIntBuffer(1);
        if(! EGL.eglGetConfigAttrib(device.getHandle(), config, EGL.EGL_SURFACE_TYPE, stype)) {
            throw new GLException("Could not determine EGL_SURFACE_TYPE");
        }

        final int _stype = stype.get(0);
        if ( 0 != ( _stype & EGL.EGL_WINDOW_BIT ) ) {
            val |= GLGraphicsConfigurationUtil.WINDOW_BIT;
        }
        if ( 0 != ( _stype & EGL.EGL_PIXMAP_BIT ) ) {
            val |= GLGraphicsConfigurationUtil.BITMAP_BIT;
        }
        if ( 0 != ( _stype & EGL.EGL_PBUFFER_BIT ) ) {
            val |= GLGraphicsConfigurationUtil.PBUFFER_BIT |
                   GLGraphicsConfigurationUtil.FBO_BIT;
        }
        return val;
    }

    /**
     * @param device
     * @param glp desired GLProfile, may be null
     * @param config
     * @param winattrmask
     * @param forceTransparentFlag
     * @return
     */
    public static EGLGLCapabilities EGLConfig2Capabilities(EGLGraphicsDevice device, GLProfile glp, long config,
                                                           int winattrmask, boolean forceTransparentFlag) {
        final long display = device.getHandle();
        final IntBuffer val = Buffers.newDirectIntBuffer(1);
        final int cfgID;
        final int rType;
        final int visualID;

        // get the configID
        if(!EGL.eglGetConfigAttrib(display, config, EGL.EGL_CONFIG_ID, val)) {
            if(DEBUG) {
                // FIXME: this happens on a ATI PC Emulation ..
                System.err.println("EGL couldn't retrieve ConfigID for config "+toHexString(config)+", error "+toHexString(EGL.eglGetError()));
            }
            return null;
        }
        cfgID = val.get(0);

        if(!EGL.eglGetConfigAttrib(display, config, EGL.EGL_RENDERABLE_TYPE, val)) {
            if(DEBUG) {
                System.err.println("EGL couldn't retrieve EGL_RENDERABLE_TYPE for config "+toHexString(config)+", error "+toHexString(EGL.eglGetError()));
            }
            return null;
        }
        rType = val.get(0);

        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_NATIVE_VISUAL_ID, val)) {
            visualID = val.get(0);
        } else {
            visualID = VisualIDHolder.VID_UNDEFINED;
        }

        EGLGLCapabilities caps = null;
        try {
            if(null == glp) {
                glp = EGLGLCapabilities.getCompatible(device, rType);
            }
            if(!EGLGLCapabilities.isCompatible(glp, rType)) {
                if(DEBUG) {
                    System.err.println("config "+toHexString(config)+": Requested GLProfile "+glp+
                                " not compatible with EGL-RenderableType["+EGLGLCapabilities.renderableTypeToString(null, rType)+"]");
                }
                return null;
            }
            caps = new EGLGLCapabilities(config, cfgID, visualID, glp, rType);
        } catch (GLException gle) {
            if(DEBUG) {
                System.err.println("config "+toHexString(config)+": "+gle);
            }
            return null;
        }

        if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_CONFIG_CAVEAT, val)) {
            if( EGL.EGL_SLOW_CONFIG == val.get(0) ) {
                caps.setHardwareAccelerated(false);
            }
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
        if(forceTransparentFlag) {
            caps.setBackgroundOpaque(false);
        } else if(EGL.eglGetConfigAttrib(display, config, EGL.EGL_TRANSPARENT_TYPE, val)) {
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
        // ALPHA shall be set at last - due to it's auto setting by the above (!opaque / samples)
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

        // Since the passed GLProfile may be null,
        // we use EGL_RENDERABLE_TYPE derived profile as created in the EGLGLCapabilities constructor.
        final int availableTypeBits = EGLConfigDrawableTypeBits(device, config);
        final int drawableTypeBits = winattrmask & availableTypeBits;

        if( 0 == drawableTypeBits ) {
            return null;
        }

        return (EGLGLCapabilities) GLGraphicsConfigurationUtil.fixWinAttribBitsAndHwAccel(device, drawableTypeBits, caps);
    }

    public static IntBuffer GLCapabilities2AttribList(GLCapabilitiesImmutable caps) {
        final IntBuffer attrs = Buffers.newDirectIntBuffer(32);
        int idx=0;

        attrs.put(idx++, EGL.EGL_SURFACE_TYPE);
        final int surfaceType;
        if( caps.isOnscreen() ) {
            surfaceType = EGL.EGL_WINDOW_BIT;
        } else if( caps.isFBO() ) {
            surfaceType = EGL.EGL_PBUFFER_BIT;  // native replacement!
        } else if( caps.isPBuffer() ) {
            surfaceType = EGL.EGL_PBUFFER_BIT;
        } else if( caps.isBitmap() ) {
            surfaceType = EGL.EGL_PIXMAP_BIT;
        } else {
            throw new GLException("no surface type set in caps: "+caps);
        }
        attrs.put(idx++, surfaceType);

        attrs.put(idx++, EGL.EGL_RED_SIZE);
        attrs.put(idx++, caps.getRedBits());

        attrs.put(idx++, EGL.EGL_GREEN_SIZE);
        attrs.put(idx++, caps.getGreenBits());

        attrs.put(idx++, EGL.EGL_BLUE_SIZE);
        attrs.put(idx++, caps.getBlueBits());

        if(caps.getAlphaBits()>0) {
            attrs.put(idx++, EGL.EGL_ALPHA_SIZE);
            attrs.put(idx++, caps.getAlphaBits());
        }

        if(caps.getStencilBits()>0) {
            attrs.put(idx++, EGL.EGL_STENCIL_SIZE);
            attrs.put(idx++, caps.getStencilBits());
        }

        attrs.put(idx++, EGL.EGL_DEPTH_SIZE);
        attrs.put(idx++, caps.getDepthBits());

        if(caps.getSampleBuffers()) {
            if(caps.getSampleExtension().equals(GLGraphicsConfigurationUtil.NV_coverage_sample)) {
                attrs.put(idx++, EGLExt.EGL_COVERAGE_BUFFERS_NV);
                attrs.put(idx++, 1);
                attrs.put(idx++, EGLExt.EGL_COVERAGE_SAMPLES_NV);
                attrs.put(idx++, caps.getNumSamples());
            } else {
                // try default ..
                attrs.put(idx++, EGL.EGL_SAMPLE_BUFFERS);
                attrs.put(idx++, 1);
                attrs.put(idx++, EGL.EGL_SAMPLES);
                attrs.put(idx++, caps.getNumSamples());
            }
        }

        attrs.put(idx++, EGL.EGL_TRANSPARENT_TYPE);
        attrs.put(idx++, caps.isBackgroundOpaque() ? EGL.EGL_NONE : EGL.EGL_TRANSPARENT_TYPE);

        // 22

        if(!caps.isBackgroundOpaque()) {
            attrs.put(idx++, EGL.EGL_TRANSPARENT_RED_VALUE);
            attrs.put(idx++, caps.getTransparentRedValue()>=0?caps.getTransparentRedValue():EGL.EGL_DONT_CARE);

            attrs.put(idx++, EGL.EGL_TRANSPARENT_GREEN_VALUE);
            attrs.put(idx++, caps.getTransparentGreenValue()>=0?caps.getTransparentGreenValue():EGL.EGL_DONT_CARE);

            attrs.put(idx++, EGL.EGL_TRANSPARENT_BLUE_VALUE);
            attrs.put(idx++, caps.getTransparentBlueValue()>=0?caps.getTransparentBlueValue():EGL.EGL_DONT_CARE);

            /** Not define in EGL
            attrs.put(idx++, EGL.EGL_TRANSPARENT_ALPHA_VALUE;
            attrs.put(idx++, caps.getTransparentAlphaValue()>=0?caps.getTransparentAlphaValue():EGL.EGL_DONT_CARE; */
        }

        // 28
        attrs.put(idx++, EGL.EGL_RENDERABLE_TYPE);
        if(caps.getGLProfile().usesNativeGLES1()) {
            attrs.put(idx++, EGL.EGL_OPENGL_ES_BIT);
        } else if(caps.getGLProfile().usesNativeGLES2()) {
            attrs.put(idx++, EGL.EGL_OPENGL_ES2_BIT);
        } else {
            attrs.put(idx++, EGL.EGL_OPENGL_BIT);
        }

        // 30

        attrs.put(idx++, EGL.EGL_NONE);

        return attrs;
    }

    public static IntBuffer CreatePBufferSurfaceAttribList(int width, int height, int texFormat) {
        IntBuffer attrs = Buffers.newDirectIntBuffer(16);
        int idx=0;

        attrs.put(idx++, EGL.EGL_WIDTH);
        attrs.put(idx++, width);

        attrs.put(idx++, EGL.EGL_HEIGHT);
        attrs.put(idx++, height);

        attrs.put(idx++, EGL.EGL_TEXTURE_FORMAT);
        attrs.put(idx++, texFormat);

        attrs.put(idx++, EGL.EGL_TEXTURE_TARGET);
        attrs.put(idx++, EGL.EGL_NO_TEXTURE==texFormat ? EGL.EGL_NO_TEXTURE : EGL.EGL_TEXTURE_2D);

        attrs.put(idx++, EGL.EGL_NONE);

        return attrs;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"["+getScreen()+
                                     ",\n\teglConfigHandle "+toHexString(getNativeConfig())+", eglConfigID "+toHexString(getNativeConfigID())+
                                     ",\n\trequested " + getRequestedCapabilities()+
                                     ",\n\tchosen    " + getChosenCapabilities()+
                                     "]";

    }

    private GLCapabilitiesChooser chooser;
}


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

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.VisualIDHolder;
import com.jogamp.opengl.DefaultGLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.opengl.GLGraphicsConfigurationUtil;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.nativewindow.MutableGraphicsConfiguration;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.GLRendererQuirks;
import com.jogamp.opengl.egl.EGL;
import com.jogamp.opengl.egl.EGLExt;

public class EGLGraphicsConfiguration extends MutableGraphicsConfiguration implements Cloneable {

    private static final String dbgCfgFailIntro = "Info: EGLConfig could not retrieve ";
    private static final String dbgCfgFailForConfig = " for config ";
    private static final String dbgCfgFailError = ", error ";

    public final long getNativeConfig() {
        return ((EGLGLCapabilities)capabilitiesChosen).getEGLConfig();
    }

    public final int getNativeConfigID() {
        return ((EGLGLCapabilities)capabilitiesChosen).getEGLConfigID();
    }

    EGLGraphicsConfiguration(final AbstractGraphicsScreen absScreen,
                             final EGLGLCapabilities capsChosen, final GLCapabilitiesImmutable capsRequested, final GLCapabilitiesChooser chooser) {
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
    public static EGLGraphicsConfiguration create(final GLCapabilitiesImmutable capsRequested, final AbstractGraphicsScreen absScreen, final int eglConfigID) {
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
            final GLRendererQuirks defaultQuirks = GLRendererQuirks.getStickyDeviceQuirks( GLDrawableFactory.getEGLFactory().getDefaultDevice() );
            final int winattrmask = GLGraphicsConfigurationUtil.getExclusiveWinAttributeBits(capsRequested);
            final EGLGLCapabilities caps = EGLConfig2Capabilities(defaultQuirks, (EGLGraphicsDevice)absDevice, capsRequested.getGLProfile(), cfg, winattrmask, false);
            return new EGLGraphicsConfiguration(absScreen, caps, capsRequested, new DefaultGLCapabilitiesChooser());
        }
        return null;
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    void updateGraphicsConfiguration() {
        final CapabilitiesImmutable capsChosen = getChosenCapabilities();
        final EGLGraphicsConfiguration newConfig = (EGLGraphicsConfiguration)
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

    public static long EGLConfigId2EGLConfig(final long display, final int configID) {
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

    public static boolean isEGLConfigValid(final long display, final long config) {
        if(0 == config) {
            return false;
        }
        final IntBuffer val = Buffers.newDirectIntBuffer(1);

        // get the configID
        if(!EGL.eglGetConfigAttrib(display, config, EGL.EGL_CONFIG_ID, val)) {
            final int eglErr = EGL.eglGetError();
            if(DEBUG) {
                System.err.println(dbgCfgFailIntro+"EGL_CONFIG_ID"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(eglErr));
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
     * @param defaultQuirks GLRendererQuirks of the EGLDrawableFactory's defaultDevice
     * @param device
     * @param glp desired GLProfile, may be null
     * @param config
     * @param winattrmask
     * @param forceTransparentFlag
     * @return
     */
    public static EGLGLCapabilities EGLConfig2Capabilities(final GLRendererQuirks defaultQuirks, final EGLGraphicsDevice device, GLProfile glp,
                                                           final long config, final int winattrmask, final boolean forceTransparentFlag) {
        final long display = device.getHandle();
        final int cfgID;
        final int rType;
        final int visualID;

        final int _attributes[] = {
            EGL.EGL_CONFIG_ID,                 // 0
            EGL.EGL_RENDERABLE_TYPE,
            EGL.EGL_NATIVE_VISUAL_ID,
            EGL.EGL_CONFIG_CAVEAT,
            EGL.EGL_RED_SIZE,                  // 4
            EGL.EGL_GREEN_SIZE,
            EGL.EGL_BLUE_SIZE,
            EGL.EGL_ALPHA_SIZE,                // 7
            EGL.EGL_STENCIL_SIZE,              // 8
            EGL.EGL_DEPTH_SIZE,
            EGL.EGL_TRANSPARENT_TYPE,          // 10
            EGL.EGL_TRANSPARENT_RED_VALUE,
            EGL.EGL_TRANSPARENT_GREEN_VALUE,
            EGL.EGL_TRANSPARENT_BLUE_VALUE,
            EGL.EGL_SAMPLES,                   // 14
            EGLExt.EGL_COVERAGE_BUFFERS_NV,    // 15
            EGLExt.EGL_COVERAGE_SAMPLES_NV
        };
        final IntBuffer attributes = Buffers.newDirectIntBuffer(_attributes);
        final IntBuffer values = Buffers.newDirectIntBuffer(attributes.remaining());
        EGL.eglGetConfigAttributes(display, config, attributes, values);

        // get the configID
        if( EGL.EGL_CONFIG_ID != attributes.get(0) ) {
            if(DEBUG) {
                // FIXME: this happens on a ATI PC Emulation ..
                System.err.println(dbgCfgFailIntro+"ConfigID"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
            }
            return null;
        }
        cfgID = values.get(0);

        if( EGL.EGL_RENDERABLE_TYPE != attributes.get(1) ) {
            if(DEBUG) {
                System.err.println(dbgCfgFailIntro+"EGL_RENDERABLE_TYPE"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
            }
            return null;
        }
        {
            final int rTypeOrig = values.get(1);
            if( defaultQuirks.exist(GLRendererQuirks.GLES3ViaEGLES2Config) && 0 != ( EGL.EGL_OPENGL_ES2_BIT & rTypeOrig ) ) {
                rType = rTypeOrig | EGLExt.EGL_OPENGL_ES3_BIT_KHR;
            } else {
                rType = rTypeOrig;
            }
        }

        if( EGL.EGL_NATIVE_VISUAL_ID == attributes.get(2) ) {
            visualID = values.get(2);
        } else {
            if(DEBUG) {
                System.err.println(dbgCfgFailIntro+"EGL_NATIVE_VISUAL_ID"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
            }
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
                                " with quirks "+defaultQuirks+" not compatible with EGL-RenderableType["+EGLGLCapabilities.renderableTypeToString(null, rType)+"]");
                }
                return null;
            }
            caps = new EGLGLCapabilities(config, cfgID, visualID, glp, rType);
        } catch (final GLException gle) {
            if(DEBUG) {
                System.err.println("config "+toHexString(config)+": "+gle);
            }
            return null;
        }

        if( EGL.EGL_CONFIG_CAVEAT == attributes.get(3) ) {
            if( EGL.EGL_SLOW_CONFIG == values.get(3) ) {
                caps.setHardwareAccelerated(false);
            }
        } else if(DEBUG) {
            System.err.println(dbgCfgFailIntro+"EGL_CONFIG_CAVEAT"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
        }
        // ALPHA shall be set at last - due to it's auto setting by the above (!opaque / samples)
        if( EGL.EGL_RED_SIZE == attributes.get(4) ) {
            caps.setRedBits(values.get(4));
        } else if(DEBUG) {
            System.err.println(dbgCfgFailIntro+"EGL_RED_SIZE"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
        }
        if( EGL.EGL_GREEN_SIZE == attributes.get(5) ) {
            caps.setGreenBits(values.get(5));
        } else if(DEBUG) {
            System.err.println(dbgCfgFailIntro+"EGL_GREEN_SIZE"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
        }
        if( EGL.EGL_BLUE_SIZE == attributes.get(6) ) {
            caps.setBlueBits(values.get(6));
        } else if(DEBUG) {
            System.err.println(dbgCfgFailIntro+"EGL_BLUE_SIZE"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
        }
        if( EGL.EGL_ALPHA_SIZE == attributes.get(7) ) {
            caps.setAlphaBits(values.get(7));
        } else if(DEBUG) {
            System.err.println(dbgCfgFailIntro+"EGL_ALPHA_SIZE"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
        }
        if( EGL.EGL_STENCIL_SIZE == attributes.get(8) ) {
            caps.setStencilBits(values.get(8));
        } else if(DEBUG) {
            System.err.println(dbgCfgFailIntro+"EGL_STENCIL_SIZE"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
        }
        if( EGL.EGL_DEPTH_SIZE == attributes.get(9) ) {
            caps.setDepthBits(values.get(9));
        } else if(DEBUG) {
            System.err.println(dbgCfgFailIntro+"EGL_DEPTH_SIZE"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
        }
        if( forceTransparentFlag ) {
            caps.setBackgroundOpaque(false);
        } else if( EGL.EGL_TRANSPARENT_TYPE == attributes.get(10) ) {
            caps.setBackgroundOpaque(values.get(10) != EGL.EGL_TRANSPARENT_RGB);
        } else if(DEBUG) {
            System.err.println(dbgCfgFailIntro+"EGL_TRANSPARENT_TYPE"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
        }
        if(!caps.isBackgroundOpaque()) {
            if( EGL.EGL_TRANSPARENT_RED_VALUE == attributes.get(11) ) {
                final int v = values.get(11);
                caps.setTransparentRedValue(EGL.EGL_DONT_CARE==v?-1:v);
            } else if(DEBUG) {
                System.err.println(dbgCfgFailIntro+"EGL_TRANSPARENT_RED_VALUE"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
            }
            if( EGL.EGL_TRANSPARENT_GREEN_VALUE == attributes.get(12) ) {
                final int v = values.get(12);
                caps.setTransparentGreenValue(EGL.EGL_DONT_CARE==v?-1:v);
            } else if(DEBUG) {
                System.err.println(dbgCfgFailIntro+"EGL_TRANSPARENT_GREEN_VALUE"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
            }
            if( EGL.EGL_TRANSPARENT_BLUE_VALUE == attributes.get(13) ) {
                final int v = values.get(13);
                caps.setTransparentBlueValue(EGL.EGL_DONT_CARE==v?-1:v);
            } else if(DEBUG) {
                System.err.println(dbgCfgFailIntro+"EGL_TRANSPARENT_BLUE_VALUE"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
            }
            /** Not defined in EGL
            if( EGL.EGL_TRANSPARENT_ALPHA_VALUE == attributes.get(??) ) {
                final int v = values.get(??);
                caps.setTransparentAlphaValue(EGL.EGL_DONT_CARE==v?-1:v);
            } else if(DEBUG) {
                System.err.println(dbgStr01+"EGL_TRANSPARENT_ALPHA_VALUE"+dbgStr02+toHexString(config)+dbgEGLCfgFailError+toHexString(EGL.eglGetError()));
            } */
        }
        if( EGL.EGL_SAMPLES == attributes.get(14) ) {
            final int numSamples = values.get(14);
            caps.setSampleBuffers(numSamples>0?true:false);
            caps.setNumSamples(numSamples);
        } else if(DEBUG) {
            System.err.println(dbgCfgFailIntro+"EGL_SAMPLES"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
        }
        if(!caps.getSampleBuffers()) {
            // try NV_coverage_sample extension
            if( EGLExt.EGL_COVERAGE_BUFFERS_NV == attributes.get(15) ) {
                final boolean enabled = values.get(15) > 0;
                if( enabled && EGLExt.EGL_COVERAGE_SAMPLES_NV == attributes.get(16) ) {
                    caps.setSampleExtension(GLGraphicsConfigurationUtil.NV_coverage_sample);
                    caps.setSampleBuffers(true);
                    caps.setNumSamples(values.get(16));
                } else if(DEBUG) {
                    System.err.println(dbgCfgFailIntro+"EGL_COVERAGE_SAMPLES_NV"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
                }
            } /** else if(DEBUG) { // Not required - vendor extension - don't be verbose!
                System.err.println(dbgCfgFailIntro+"EGL_COVERAGE_BUFFERS_NV"+dbgCfgFailForConfig+toHexString(config)+dbgCfgFailError+toHexString(EGL.eglGetError()));
            } */
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

    public static IntBuffer GLCapabilities2AttribList(final GLCapabilitiesImmutable caps) {
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
        } else if(caps.getGLProfile().usesNativeGLES3()) {
            if( GLRendererQuirks.existStickyDeviceQuirk(GLDrawableFactory.getEGLFactory().getDefaultDevice(), GLRendererQuirks.GLES3ViaEGLES2Config) ) {
                attrs.put(idx++, EGL.EGL_OPENGL_ES2_BIT);
            } else {
                attrs.put(idx++, EGLExt.EGL_OPENGL_ES3_BIT_KHR);
            }
        } else {
            attrs.put(idx++, EGL.EGL_OPENGL_BIT);
        }

        // 30

        attrs.put(idx++, EGL.EGL_NONE);

        return attrs;
    }

    public static IntBuffer CreatePBufferSurfaceAttribList(final int width, final int height, final int texFormat) {
        final IntBuffer attrs = Buffers.newDirectIntBuffer(16);
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

    private final GLCapabilitiesChooser chooser;
}


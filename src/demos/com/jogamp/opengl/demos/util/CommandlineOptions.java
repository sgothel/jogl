/**
 * Copyright 2023 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.demos.util;

import com.jogamp.graph.curve.Region;
import com.jogamp.math.FloatUtil;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

public class CommandlineOptions {
    /**
     * Default DPI threshold value to disable {@link Region#VBAA_RENDERING_BIT VBAA}: {@value} dpi
     * @see #UISceneDemo20(float)
     * @see #UISceneDemo20(float, boolean, boolean)
     */
    public static final float DefaultNoAADPIThreshold = 200f;

    public int surface_width, surface_height;
    public String glProfileName = GLProfile.GL2ES2;
    public float noAADPIThreshold = DefaultNoAADPIThreshold;
    public int renderModes = Region.NORM_RENDERING_BIT;
    public int sceneMSAASamples = 0;
    /** Sample count for Graph Region AA {@link Region#getRenderModes() render-modes}: {@link Region#VBAA_RENDERING_BIT} or {@link Region#MSAA_RENDERING_BIT} */
    public int graphAASamples = 0;
    /** Pass2 AA-quality rendering for Graph Region AA {@link Region#getRenderModes() render-modes}: {@link #VBAA_RENDERING_BIT}. Defaults to {@link Region#DEFAULT_AA_QUALITY}. */
    public int graphAAQuality = Region.DEFAULT_AA_QUALITY;
    public boolean exclusiveContext = false;
    public boolean wait_to_start = false;
    public boolean keepRunning = false;
    public boolean stayOpen = false;
    public int swapInterval = -1; // auto
    public float total_duration = 0f; // [s]
    /** Is true if values haven't changed throug parse() */
    public boolean default_setting = true;
    /** Is true if AA values haven't changed through parse() */
    public boolean default_aa_setting = true;

    static {
        GLProfile.initSingleton(); // ensure JOGL is completely initialized
    }

    /**
     * Commandline options defining default_setting and default_aa_setting
     * @param width viewport width in pixels
     * @param height viewport height in pixels
     * @param renderModes {@link Region#getRenderModes()}, if {@link Region#isGraphAA(int)} {@link #graphAASamples} is set to {@code 4}.
     */
    public CommandlineOptions(final int width, final int height, final int renderModes) {
        this(width, height, renderModes, Region.DEFAULT_AA_QUALITY, Region.isGraphAA(renderModes) ? 4 : 0, 0);
    }

    /**
     * Commandline options defining default_setting and default_aa_setting
     * @param width viewport width in pixels
     * @param height viewport height in pixels
     * @param renderModes {@link Region#getRenderModes()}
     * @param graphAAQuality if {@link Region#VBAA_RENDERING_BIT} this is the AA-quality shader selection, clipped via {@link Region#clipAAQuality(int)}
     * @param graphAASamples if {@link Region#isGraphAA(int)} this is the graph sample count, clipped via {@link Region#clipAASampleCount(int)}
     * @param sceneMSAASamples if !{@link Region#isGraphAA(int)} and this value is > 0, it enables scene (fullscreen) MSAA mode by the GPU, usually 4 and 8 is good.
     */
    public CommandlineOptions(final int width, final int height, final int renderModes, final int graphAAQuality, final int graphAASamples, final int sceneMSAASamples) {
        this.surface_width = width;
        this.surface_height = height;
        this.renderModes = renderModes;
        this.graphAASamples = Region.clipAASampleCount(graphAASamples);
        this.graphAAQuality = Region.clipAAQuality(graphAAQuality);
        this.sceneMSAASamples = !Region.isGraphAA(renderModes) ? sceneMSAASamples : 0;
    }
    public void parse(final String[] args) {
        final int[] idx = { 0 };
        for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
            parse(args, idx);
        }
    }
    public boolean parse(final String[] args, final int[] idx) {
        if( 0 > idx[0] || idx[0] >= args.length ) {
            return false;
        }
        boolean res = true;
        if (args[idx[0]].equals("-hhd")) {
            surface_width = 1280;
            surface_height = 720;
        } else if (args[idx[0]].equals("-fhd")) {
            surface_width = 1920;
            surface_height = 1080;
        } else if (args[idx[0]].equals("-w")) {
            ++idx[0];
            surface_width = MiscUtils.atoi(args[idx[0]], surface_width);
        } else if (args[idx[0]].equals("-h")) {
            ++idx[0];
            surface_height = MiscUtils.atoi(args[idx[0]], surface_height);
        } else if(args[idx[0]].equals("-es2")) {
            glProfileName = GLProfile.GLES2;
        } else if(args[idx[0]].equals("-es3")) {
            glProfileName = GLProfile.GLES3;
        } else if(args[idx[0]].equals("-gl2")) {
            glProfileName = GLProfile.GL2;
        } else if(args[idx[0]].equals("-gl3bc")) {
            glProfileName = GLProfile.GL3bc;
        } else if(args[idx[0]].equals("-gl3")) {
            glProfileName = GLProfile.GL3;
        } else if(args[idx[0]].equals("-gl4")) {
            glProfileName = GLProfile.GL4;
        } else if(args[idx[0]].equals("-gl4bc")) {
            glProfileName = GLProfile.GL4bc;
        } else if(args[idx[0]].equals("-gldef")) {
            glProfileName = null;
        } else if(args[idx[0]].equals("-gnone")) {
            sceneMSAASamples = 0;
            graphAASamples = 0;
            renderModes = Region.NORM_RENDERING_BIT;
            default_aa_setting = false;
        } else if(args[idx[0]].equals("-smsaa")) {
            ++idx[0];
            graphAASamples = 0;
            sceneMSAASamples = MiscUtils.atoi(args[idx[0]], 4);
            renderModes &= ~Region.AA_RENDERING_MASK;
            default_aa_setting = false;
        } else if(args[idx[0]].equals("-gmsaa")) {
            ++idx[0];
            sceneMSAASamples = 0;
            graphAASamples = MiscUtils.atoi(args[idx[0]], 4);
            renderModes &= ~Region.AA_RENDERING_MASK;
            renderModes |= Region.MSAA_RENDERING_BIT;
            default_aa_setting = false;
        } else if(args[idx[0]].equals("-gvbaa")) {
            ++idx[0];
            sceneMSAASamples = 0;
            graphAASamples = MiscUtils.atoi(args[idx[0]], 4);
            renderModes &= ~Region.AA_RENDERING_MASK;
            renderModes |= Region.VBAA_RENDERING_BIT;
            default_aa_setting = false;
        } else if(args[idx[0]].equals("-color")) {
            renderModes |= Region.COLORCHANNEL_RENDERING_BIT;
        } else if(args[idx[0]].equals("-no-color")) {
            renderModes &= ~Region.COLORCHANNEL_RENDERING_BIT;
        } else if(args[idx[0]].equals("-gaaq")) {
            ++idx[0];
            graphAAQuality = Region.clipAAQuality( MiscUtils.atoi(args[idx[0]], graphAAQuality) );
        } else if(args[idx[0]].equals("-exclusiveContext")) {
            exclusiveContext = true;
        } else if(args[idx[0]].equals("-wait")) {
            wait_to_start = true;
        } else if (args[idx[0]].equals("-keep")) {
            keepRunning = true;
            stayOpen = true;
        } else if (args[idx[0]].equals("-stay")) {
            stayOpen = true;
        } else if (args[idx[0]].equals("-swapInterval")) {
            ++idx[0];
            swapInterval = MiscUtils.atoi(args[idx[0]], swapInterval);
        } else if (args[idx[0]].equals("-duration")) {
            ++idx[0];
            total_duration = MiscUtils.atof(args[idx[0]], total_duration);
        } else {
            res = false;
        }
        default_setting = default_setting && !res;
        return res;
    }
    public GLProfile getGLProfile() {
        return GLProfile.get(glProfileName);
    }
    public void setGLProfile(final String name) {
        glProfileName = name;
    }
    public GLCapabilities getGLCaps() {
        final GLProfile glp = getGLProfile();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        if( sceneMSAASamples > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(sceneMSAASamples);
        }
        return caps;
    }

    /**
     * Changes default AA rendering bit if not modified via parse(), i.e. default_aa_setting is true.
     *
     * AA rendering will be enabled if dpiV < noAADPIThreshold, otherwise enabled.
     *
     * @param dpiV display vertical DPI
     * @return the previous renderModes
     */
    public int fixDefaultAARenderModeWithDPIThreshold(final float dpiV) {
        //return fixAARenderModeWithDPIThreshold(default_aa_setting, dpiV);
        final int o = renderModes;
        final boolean usesAA = Region.isGraphAA(renderModes) || 0 < sceneMSAASamples;
        if(default_aa_setting && !FloatUtil.isZero(noAADPIThreshold)) {
            if( dpiV >= noAADPIThreshold ) {
                renderModes &= ~Region.AA_RENDERING_MASK;
            } else if( !usesAA ) {
                renderModes = Region.VBAA_RENDERING_BIT;
            }
        }
        return o;
    }

    @Override
    public String toString() {
        return "Options{surface[width "+surface_width+" x "+surface_height+"], glp "+glProfileName+
               ", renderModes "+Region.getRenderModeString(renderModes)+", aa-q "+graphAAQuality+
               ", gmsaa "+graphAASamples+", smsaa "+sceneMSAASamples+
               ", exclusiveContext "+exclusiveContext+", wait "+wait_to_start+", keep "+keepRunning+", stay "+stayOpen+", swap-ival "+swapInterval+", dur "+total_duration+"s"+
               "}";
    }
}

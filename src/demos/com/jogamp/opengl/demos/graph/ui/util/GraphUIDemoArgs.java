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
package com.jogamp.opengl.demos.graph.ui.util;

import com.jogamp.graph.curve.Region;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.util.MiscUtils;

public class GraphUIDemoArgs {
    public int surface_width, surface_height;
    public String glProfileName = GLProfile.GL2ES2;
    public boolean wait_to_start = false;
    public boolean keepRunning = false;
    public boolean stayOpen = false;
    public int renderModes;
    public int sceneMSAASamples = 0;
    public float debugBoxThickness = 0f;

    public GraphUIDemoArgs(final int width, final int height, final int renderModes) {
        this.surface_width = width;
        this.surface_height = height;
        this.renderModes = renderModes;
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
        } else if(args[idx[0]].equals("-gl3")) {
            glProfileName = GLProfile.GL3;
        } else if(args[idx[0]].equals("-gl4")) {
            glProfileName = GLProfile.GL4;
        } else if(args[idx[0]].equals("-gldef")) {
            glProfileName = null;
        } else if(args[idx[0]].equals("-wait")) {
            wait_to_start = true;
        } else if (args[idx[0]].equals("-keep")) {
            keepRunning = true;
            stayOpen = true;
        } else if (args[idx[0]].equals("-stay")) {
            stayOpen = true;
        } else if(args[idx[0]].equals("-gnone")) {
            sceneMSAASamples = 0;
            renderModes = 0;
        } else if(args[idx[0]].equals("-color")) {
            renderModes |= Region.COLORCHANNEL_RENDERING_BIT;
        } else if(args[idx[0]].equals("-no-color")) {
            renderModes &= ~Region.COLORCHANNEL_RENDERING_BIT;
        } else if(args[idx[0]].equals("-smsaa")) {
            ++idx[0];
            sceneMSAASamples = MiscUtils.atoi(args[idx[0]], 4);
            renderModes &= ~(Region.VBAA_RENDERING_BIT | Region.MSAA_RENDERING_BIT );
        } else if(args[idx[0]].equals("-gmsaa")) {
            sceneMSAASamples = 0;
            renderModes &= ~(Region.VBAA_RENDERING_BIT | Region.MSAA_RENDERING_BIT );
            renderModes |= Region.MSAA_RENDERING_BIT;
        } else if(args[idx[0]].equals("-gvbaa")) {
            sceneMSAASamples = 0;
            renderModes &= ~(Region.VBAA_RENDERING_BIT | Region.MSAA_RENDERING_BIT );
            renderModes |= Region.VBAA_RENDERING_BIT;
        } else if (args[idx[0]].equals("-dbgbox")) {
            ++idx[0];
            debugBoxThickness = MiscUtils.atof(args[idx[0]], debugBoxThickness);
        } else {
            res = false;
        }
        return res;
    }
    @Override
    public String toString() {
        return "Options{surface[width "+surface_width+" x "+surface_height+"], glp "+glProfileName+
               ", wait "+wait_to_start+", keep "+keepRunning+", stay "+stayOpen+
               ", renderModes "+Region.getRenderModeString(renderModes)+
               ", smsaa "+sceneMSAASamples+", dbgbox "+debugBoxThickness+"}";
    }
}

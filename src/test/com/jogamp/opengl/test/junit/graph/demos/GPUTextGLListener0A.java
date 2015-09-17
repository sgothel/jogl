/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.graph.demos;


import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.newt.opengl.GLWindow;

public class GPUTextGLListener0A extends GPUTextRendererListenerBase01 {

    public GPUTextGLListener0A() {
        super(RenderState.createRenderState(SVertex.factory()), Region.VBAA_RENDERING_BIT, 4, true, false, false);
    }

    public GPUTextGLListener0A(final RenderState rs, final int renderModes, final int sampleCount, final boolean blending, final boolean debug, final boolean trace) {
        super(rs, renderModes, sampleCount, blending, debug, trace);
    }

    public void init(final GLAutoDrawable drawable) {
        if(drawable instanceof GLWindow) {
            final GLWindow glw = (GLWindow) drawable;
            attachInputListenerTo(glw);
        }
        super.init(drawable);

        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        final RenderState rs = getRenderer().getRenderState();

        gl.setSwapInterval(1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_BLEND);
        rs.setColorStatic(0.1f, 0.1f, 0.1f, 1.0f);
    }

    public void dispose(final GLAutoDrawable drawable) {
        if(drawable instanceof GLWindow) {
            final GLWindow glw = (GLWindow) drawable;
            detachInputListenerFrom(glw);
        }
        super.dispose(drawable);
    }
}

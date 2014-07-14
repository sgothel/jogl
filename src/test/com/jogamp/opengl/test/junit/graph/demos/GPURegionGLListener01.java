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

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.opengl.util.PMVMatrix;

/** Demonstrate the rendering of multiple outlines into one region/OutlineShape
 *  These Outlines are not necessary connected or contained.
 *  The output of this demo shows two identical shapes but the left one
 *  has some vertices with off-curve flag set to true, and the right allt he vertices
 *  are on the curve. Demos the Res. Independent Nurbs based Curve rendering
 *
 */
public class GPURegionGLListener01 extends GPURendererListenerBase01 {
    OutlineShape outlineShape = null;

    public GPURegionGLListener01 (final RenderState rs, final int renderModes, final int sampleCount, final boolean debug, final boolean trace) {
        super(RegionRenderer.create(rs, RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable), renderModes, debug, trace);
        rs.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);
        setMatrix(-20, 00, -50, 0f, sampleCount);
    }

    private void createTestOutline(){
        float offset = 0;
        outlineShape = new OutlineShape(getRenderer().getRenderState().getVertexFactory());
        outlineShape.addVertex(0.0f,-10.0f, true);
        outlineShape.addVertex(15.0f,-10.0f, true);
        outlineShape.addVertex(10.0f,5.0f, false);
        outlineShape.addVertex(15.0f,10.0f, true);
        outlineShape.addVertex(6.0f,15.0f, false);
        outlineShape.addVertex(5.0f,8.0f, false);
        outlineShape.addVertex(0.0f,10.0f,true);
        outlineShape.closeLastOutline(true);
        outlineShape.addEmptyOutline();
        outlineShape.addVertex(5.0f,-5.0f,true);
        outlineShape.addVertex(10.0f,-5.0f, false);
        outlineShape.addVertex(10.0f,0.0f, true);
        outlineShape.addVertex(5.0f,0.0f, false);
        outlineShape.closeLastOutline(true);

        /** Same shape as above but without any off-curve vertices */
        offset = 30;
        outlineShape.addEmptyOutline();
        outlineShape.addVertex(offset+0.0f,-10.0f, true);
        outlineShape.addVertex(offset+17.0f,-10.0f, true);
        outlineShape.addVertex(offset+11.0f,5.0f, true);
        outlineShape.addVertex(offset+16.0f,10.0f, true);
        outlineShape.addVertex(offset+7.0f,15.0f, true);
        outlineShape.addVertex(offset+6.0f,8.0f, true);
        outlineShape.addVertex(offset+0.0f,10.0f, true);
        outlineShape.closeLastOutline(true);
        outlineShape.addEmptyOutline();
        outlineShape.addVertex(offset+5.0f,0.0f, true);
        outlineShape.addVertex(offset+5.0f,-5.0f, true);
        outlineShape.addVertex(offset+10.0f,-5.0f, true);
        outlineShape.addVertex(offset+10.0f,0.0f, true);
        outlineShape.closeLastOutline(true);

        region = GLRegion.create(getRenderModes(), null);
        region.addOutlineShape(outlineShape, null, region.hasColorChannel() ? getRenderer().getRenderState().getColorStatic(new float[4]) : null);
    }

    public void init(final GLAutoDrawable drawable) {
        super.init(drawable);

        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        final RenderState rs = getRenderer().getRenderState();

        gl.setSwapInterval(1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_BLEND);
        rs.setColorStatic(0.0f, 0.0f, 0.0f, 1.0f);

        createTestOutline();
    }

    public void display(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final RegionRenderer regionRenderer = getRenderer();
        final PMVMatrix pmv = regionRenderer.getMatrix();
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glLoadIdentity();
        pmv.glTranslatef(getXTran(), getYTran(), getZTran());
        pmv.glRotatef(getAngle(), 0, 1, 0);
        if( weight != regionRenderer.getRenderState().getWeight() ) {
            regionRenderer.getRenderState().setWeight(weight);
        }
        region.draw(gl, regionRenderer, getSampleCount());
    }
}

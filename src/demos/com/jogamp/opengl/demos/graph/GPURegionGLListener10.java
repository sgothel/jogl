/**
 * Copyright 2010-2024 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.demos.graph;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.opengl.util.PMVMatrix;

/** Demonstrate the rendering of multiple OutlineShapes
 *  into one region
 *
 */
public class GPURegionGLListener10 extends GPURendererListenerBase01 {
    List<OutlineShape> outlineShapes = new ArrayList<OutlineShape>();

    public GPURegionGLListener10 (final int renderModes, final int aaQuality, final int sampleCount, final boolean debug, final boolean trace) {
        super(RegionRenderer.create(RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable), renderModes, debug, trace);
        this.getRenderer().setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);
        setMatrix(-20, 00, -50, 0f);
        getRenderer().setAAQuality(aaQuality);
        getRenderer().setSampleCount(sampleCount);
    }

    private void createTestOutline(final GLProfile glp){
        OutlineShape shape = new OutlineShape();
        outlineShapes.add(shape);
        shape.addVertex(0.0f,-10.0f,true);
        shape.addVertex(15.0f,-10.0f, true);
        shape.addVertex(10.0f,5.0f, false);
        shape.addVertex(15.0f,10.0f, true);
        shape.addVertex(6.0f,15.0f, false);
        shape.addVertex(5.0f,8.0f, false);
        shape.addVertex(0.0f,10.0f,true);
        shape.closeLastOutline(true);
        shape.addEmptyOutline();
        shape.addVertex(5.0f,-5.0f,true);
        shape.addVertex(10.0f,-5.0f, false);
        shape.addVertex(10.0f,0.0f, true);
        shape.addVertex(5.0f,0.0f, false);
        shape.closeLastOutline(true);
        final int[/*2*/] vertIndexCount = Region.countOutlineShape(shape, new int[2]);

        /** Same shape as above but without any off-curve vertices */
        shape = new OutlineShape();
        outlineShapes.add(shape);
        final float offset = 30;
        shape.addVertex(offset+0.0f,-10.0f, true);
        shape.addVertex(offset+17.0f,-10.0f, true);
        shape.addVertex(offset+11.0f,5.0f, true);
        shape.addVertex(offset+16.0f,10.0f, true);
        shape.addVertex(offset+7.0f,15.0f, true);
        shape.addVertex(offset+6.0f,8.0f, true);
        shape.addVertex(offset+0.0f,10.0f, true);
        shape.closeLastOutline(true);
        shape.addEmptyOutline();
        shape.addVertex(offset+5.0f,0.0f, true);
        shape.addVertex(offset+5.0f,-5.0f, true);
        shape.addVertex(offset+10.0f,-5.0f, true);
        shape.addVertex(offset+10.0f,0.0f, true);
        shape.closeLastOutline(true);
        Region.countOutlineShape(shape, vertIndexCount);

        region = GLRegion.create(glp, getRenderModes(), null, vertIndexCount[0], vertIndexCount[1]);
        region.addOutlineShapes(outlineShapes, null, null);
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        super.init(drawable);

        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.setSwapInterval(1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_BLEND);
        getRenderer().setColorStatic(0.0f, 0.0f, 0.0f, 1.0f);

        createTestOutline(gl.getGLProfile());
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final RegionRenderer regionRenderer = getRenderer();

        final PMVMatrix4f pmv = regionRenderer.getMatrix();
        pmv.loadMvIdentity();
        pmv.translateMv(getXTran(), getYTran(), getZTran());
        pmv.rotateMv(getAngleRad(), 0, 1, 0);
        if( weight != regionRenderer.getWeight() ) {
            regionRenderer.setWeight(weight);
        }
        regionRenderer.enable(gl, true);
        region.draw(gl, regionRenderer);
        regionRenderer.enable(gl, false);

    }
}

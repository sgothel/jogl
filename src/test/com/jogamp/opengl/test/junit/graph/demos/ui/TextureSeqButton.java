/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.graph.demos.ui;

import com.jogamp.opengl.GL2ES2;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.opengl.util.texture.TextureSequence;

/**
 * GPU based resolution independent {@link TextureSequence} Button impl
 */
public class TextureSeqButton extends RoundButton {
    protected final TextureSequence texSeq;

    public TextureSeqButton(final Factory<? extends Vertex> factory, final int renderModes,
                         final float width, final float height, final TextureSequence texSeq) {
        super(factory, renderModes | Region.COLORTEXTURE_RENDERING_BIT, width, height);
        this.texSeq = texSeq;
    }

    @Override
    protected GLRegion createGLRegion() {
        return GLRegion.create(getRenderModes(), texSeq);
    }

    public final TextureSequence getTextureSequence() { return this.texSeq; }

    @Override
    protected void addShapeToRegion(final GL2ES2 gl, final RegionRenderer renderer) {
        final OutlineShape shape = new OutlineShape(renderer.getRenderState().getVertexFactory());
        if(corner == 0.0f) {
            createSharpOutline(shape, 0f);
        } else {
            createCurvedOutline(shape, 0f);
        }
        shape.setIsQuadraticNurbs();
        shape.setSharpness(shapesSharpness);
        region.addOutlineShape(shape, null, rgbaColor);
        box.resize(shape.getBounds());

        final float[] ctr = box.getCenter();
        setRotationOrigin( ctr[0], ctr[1], ctr[2]);

        if( DRAW_DEBUG_BOX ) {
            System.err.println("XXX.UIShape.TextureSeqButton: Added Shape: "+shape+", "+box);
        }
    }
}

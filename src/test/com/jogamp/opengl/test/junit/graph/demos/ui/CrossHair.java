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
package com.jogamp.opengl.test.junit.graph.demos.ui;

import javax.media.opengl.GL2ES2;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.OutlineShapeXForm;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;

/**
 * GPU based resolution independent Button impl
 */
public class CrossHair extends UIShape {
    private float width, height, lineWidth;

    public CrossHair(Factory<? extends Vertex> factory, float width, float height, float linewidth) {
        super(factory);
        this.width = width;
        this.height = height;
        this.lineWidth = linewidth;
    }

    public final float getWidth() { return width; }
    public final float getHeight() { return height; }
    public final float getLineWidth() { return lineWidth; }

    public void setDimension(float width, float height, float lineWidth) {
        this.width = width;
        this.height = height;
        this.lineWidth = lineWidth;
        dirty |= DIRTY_SHAPE | DIRTY_REGION;
    }

    @Override
    protected void clearImpl(GL2ES2 gl, RegionRenderer renderer) {
    }

    @Override
    protected void destroyImpl(GL2ES2 gl, RegionRenderer renderer) {
    }

    @Override
    protected void createShape(GL2ES2 gl, RegionRenderer renderer) {
        final OutlineShape shape = new OutlineShape(renderer.getRenderState().getVertexFactory());

        final float tw = getWidth();
        final float th = getHeight();
        final float twh = tw/2f;
        final float thh = th/2f;
        final float lwh = lineWidth/2f;

        final float ctrX = 0f, ctrY = 0f;
        float ctrZ = 0f;

        // vertical (CCW!)
        shape.addVertex(ctrX-lwh, ctrY-thh, ctrZ,  true);
        shape.addVertex(ctrX+lwh, ctrY-thh, ctrZ,  true);
        shape.addVertex(ctrX+lwh, ctrY+thh, ctrZ,  true);
        shape.addVertex(ctrX-lwh, ctrY+thh, ctrZ,  true);
        shape.closeLastOutline(true);

        ctrZ -= 0.05f;

        // horizontal (CCW!)
        shape.addEmptyOutline();
        shape.addVertex(ctrX-twh, ctrY-lwh, ctrZ,  true);
        shape.addVertex(ctrX+twh, ctrY-lwh, ctrZ,  true);
        shape.addVertex(ctrX+twh, ctrY+lwh, ctrZ,  true);
        shape.addVertex(ctrX-twh, ctrY+lwh, ctrZ,  true);
        shape.closeLastOutline(true);

        shapes.add(new OutlineShapeXForm(shape, null));

        box.resize(shape.getBounds());
    }

    @Override
    public String toString() {
        return "CrossHair [" + translate[0]+getWidth()/2f+" / "+translate[1]+getHeight()/2f+" "+getWidth() + "x" + getHeight() + ", "+box+"]";
    }
}

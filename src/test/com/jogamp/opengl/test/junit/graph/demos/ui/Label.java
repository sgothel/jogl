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

import com.jogamp.opengl.GL2ES2;

import jogamp.graph.geom.plane.AffineTransform;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;

public class Label extends UIShape {
    protected Font font;
    protected float pixelSize;
    protected String text;

    public Label(final Factory<? extends Vertex> factory, final int renderModes, final Font font, final float pixelSize, final String text) {
        super(factory, renderModes);
        this.font = font;
        this.pixelSize = pixelSize;
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
        markShapeDirty();
    }

    public Font getFont() {
        return font;
    }

    public void setFont(final Font font) {
        this.font = font;
        markShapeDirty();
    }

    public float getPixelSize() {
        return pixelSize;
    }

    public float getLineHeight() {
        return font.getLineHeight(pixelSize);
    }

    public void setPixelSize(final float pixelSize) {
        this.pixelSize = pixelSize;
        markShapeDirty();
    }

    @Override
    protected void clearImpl(final GL2ES2 gl, final RegionRenderer renderer) {
    }

    @Override
    protected void destroyImpl(final GL2ES2 gl, final RegionRenderer renderer) {
    }

    private final float[] tmpV3 = new float[3];
    private final AffineTransform tempT1 = new AffineTransform();
    private final AffineTransform tempT2 = new AffineTransform();

    private final TextRegionUtil.ShapeVisitor shapeVisitor = new TextRegionUtil.ShapeVisitor() {
        @Override
        public void visit(final OutlineShape shape, final AffineTransform t) {
            shape.setSharpness(shapesSharpness);
            region.addOutlineShape(shape, t, rgbaColor);
            box.resize(shape.getBounds(), t, tmpV3);
        }
    };

    @Override
    protected void addShapeToRegion(final GL2ES2 gl, final RegionRenderer renderer) {
        TextRegionUtil.processString(shapeVisitor, null, font, pixelSize, text, tempT1, tempT2);
        final float[] ctr = box.getCenter();
        setRotationOrigin( ctr[0], ctr[1], ctr[2]);
    }

    @Override
    public String getSubString() {
        final int m = Math.min(text.length(), 8);
        return super.getSubString()+", psize " + pixelSize + ", '" + text.substring(0, m)+"'";
    }
}

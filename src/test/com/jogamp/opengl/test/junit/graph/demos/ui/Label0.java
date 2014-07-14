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

import jogamp.graph.geom.plane.AffineTransform;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.opengl.math.geom.AABBox;

public class Label0 {
    protected Font font;
    protected String text;
    protected final float[] rgbaColor;
    protected final AABBox box;

    public Label0(final Font font, final String text, final float[] rgbaColor) {
        this.font = font;
        this.text = text;
        this.rgbaColor = rgbaColor;
        this.box = new AABBox();
    }

    public final String getText() { return text; }

    public final float[] getColor() { return rgbaColor; }

    public final void setColor(final float r, final float g, final float b, final float a) {
        this.rgbaColor[0] = r;
        this.rgbaColor[1] = g;
        this.rgbaColor[2] = b;
        this.rgbaColor[3] = a;
    }

    public final void setText(final String text) {
        this.text = text;
    }

    public final Font getFont() { return font; }

    public final void setFont(final Font font) {
        this.font = font;
    }

    public final AABBox getBounds() { return box; }

    private final float[] tmpV3 = new float[3];
    private final AffineTransform tempT1 = new AffineTransform();
    private final AffineTransform tempT2 = new AffineTransform();

    private final TextRegionUtil.ShapeVisitor shapeVisitor = new TextRegionUtil.ShapeVisitor() {
        @Override
        public void visit(final OutlineShape shape, final AffineTransform t) {
            final AffineTransform t1 = t.preConcatenate(tLeft);
            region.addOutlineShape(shape, t1, rgbaColor);
            box.resize(shape.getBounds(), t1, tmpV3);
        }
    };

    private Region region;
    private AffineTransform tLeft;

    public final AABBox addShapeToRegion(final float pixelSize, final Region region, final AffineTransform tLeft) {
        box.reset();
        this.region = region;
        this.tLeft = tLeft;
        TextRegionUtil.processString(shapeVisitor, null, font, pixelSize, text, tempT1, tempT2);
        this.region = null;
        this.tLeft = null;
        return box;
    }

    @Override
    public final String toString(){
        final int m = Math.min(text.length(), 8);
        return "Label0 ['" + text.substring(0, m) + "']";
    }
}

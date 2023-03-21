/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.ui.gl.shapes;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.ui.gl.Shape;

/**
 * A GraphUI Crosshair {@link Shape}
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 */
public class CrossHair extends Shape {
    private float width, height, lineWidth;

    public CrossHair(final int renderModes, final float width, final float height, final float linewidth) {
        super(renderModes);
        this.width = width;
        this.height = height;
        this.lineWidth = linewidth;
    }

    public final float getWidth() { return width; }
    public final float getHeight() { return height; }
    public final float getLineWidth() { return lineWidth; }

    public void setDimension(final float width, final float height, final float lineWidth) {
        this.width = width;
        this.height = height;
        this.lineWidth = lineWidth;
        markShapeDirty();
    }

    @Override
    protected void addShapeToRegion() {
        final OutlineShape shape = new OutlineShape(vertexFactory);

        final float lwh = lineWidth/2f;

        final float tw = getWidth();
        final float th = getHeight();
        final float twh = tw/2f;
        final float thh = th/2f;

        final float ctrX = 0f, ctrY = 0f;
        final float ctrZ = 0f;

        // middle vertical (CCW!)
        shape.moveTo(ctrX-lwh, ctrY-thh, ctrZ);
        shape.lineTo(ctrX+lwh, ctrY-thh, ctrZ);
        shape.lineTo(ctrX+lwh, ctrY+thh, ctrZ);
        shape.lineTo(ctrX-lwh, ctrY+thh, ctrZ);
        shape.closePath();

        // middle horizontal (CCW!)
        shape.moveTo(ctrX-twh, ctrY-lwh, ctrZ);
        shape.lineTo(ctrX+twh, ctrY-lwh, ctrZ);
        shape.lineTo(ctrX+twh, ctrY+lwh, ctrZ);
        shape.lineTo(ctrX-twh, ctrY+lwh, ctrZ);
        shape.closePath();

        shape.setIsQuadraticNurbs();
        shape.setSharpness(shapesSharpness);
        region.addOutlineShape(shape, null, rgbaColor);

        box.resize(shape.getBounds());
    }

    @Override
    public String getSubString() {
        return super.getSubString()+", dim "+getWidth() + " x " + getHeight();
    }
}

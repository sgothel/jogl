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
package com.jogamp.graph.ui.shapes;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.ui.GraphShape;
import com.jogamp.graph.ui.Shape;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.geom.AABBox;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureSequence;

/**
 * A GraphUI rectangle {@link GraphShape}
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 */
public class Rectangle extends GraphShape {
    private float minX, minY, zPos;
    private float width;
    private float height;
    private float lineWidth;

    /**
     * Create a rectangular Graph based {@link GLRegion} UI {@link Shape}.
     *
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param minX
     * @param minY
     * @param width
     * @param height
     * @param lineWidth line thickness, use zero for filled rectangle
     * @param zPos
     */
    public Rectangle(final int renderModes, final float minX, final float minY, final float width, final float height, final float lineWidth, final float zPos) {
        super( renderModes );
        this.minX = minX;
        this.minY = minY;
        this.zPos = zPos;
        this.width = width;
        this.height = height;
        this.lineWidth = lineWidth;
    }

    /**
     * Create a rectangular Graph based {@link GLRegion} UI {@link Shape}.
     *
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param abox
     * @param lineWidth line thickness, use zero for filled rectangle
     */
    public Rectangle(final int renderModes, final AABBox abox, final float lineWidth) {
        this( renderModes, abox.getMinX(), abox.getMinY(), abox.getWidth(), abox.getHeight(), lineWidth, abox.getCenter().z());
    }

    /**
     * Create a rectangular Graph based {@link GLRegion} UI {@link Shape}.
     *
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param minX
     * @param minY
     * @param width
     * @param height
     * @param lineWidth line thickness, use zero for filled rectangle
     */
    public Rectangle(final int renderModes, final float minX, final float minY, final float width, final float height, final float lineWidth) {
        this( renderModes, minX, minY, width, height, lineWidth, 0);
    }

    /**
     * Create a rectangular Graph based {@link GLRegion} UI {@link Shape}.
     *
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param width
     * @param height
     * @param lineWidth line thickness, use zero for filled rectangle
     */
    public Rectangle(final int renderModes, final float width, final float height, final float lineWidth) {
        this( renderModes, 0, 0, width, height, lineWidth, 0);
    }

    public final float getWidth() { return width; }
    public final float getHeight() { return height; }
    public final float getLineWidth() { return lineWidth; }

    public void setPosition(final float minX, final float minY, final float zPos) {
        this.minX = minX;
        this.minY = minY;
        this.zPos = zPos;
        markShapeDirty();
    }
    public void setDimension(final float width, final float height, final float lineWidth) {
        this.width = width;
        this.height = height;
        this.lineWidth = lineWidth;
        markShapeDirty();
    }
    public void setBounds(final AABBox abox, final float lineWidth) {
        setPosition(abox.getMinX(), abox.getMinY(), abox.getCenter().z());
        setDimension(abox.getWidth(), abox.getHeight(), lineWidth);
    }

    @Override
    protected void addShapeToRegion(final GLProfile glp, final GL2ES2 gl) {
        final OutlineShape shape = new OutlineShape();
        final float x1 = minX;
        final float y1 = minY;
        final float x2 = minX + getWidth();
        final float y2 = minY + getHeight();
        final float z = zPos;
        {
            // Outer OutlineShape as Winding.CCW.
            shape.moveTo(x1, y1, z);
            shape.lineTo(x2, y1, z);
            shape.lineTo(x2, y2, z);
            shape.lineTo(x1, y2, z);
            shape.lineTo(x1, y1, z);
            shape.closeLastOutline(true);
        }
        if( !FloatUtil.isZero(lineWidth) ) {
            shape.addEmptyOutline();
            // Inner OutlineShape as Winding.CW.
            // final float dxy0 = getWidth() < getHeight() ? getWidth() : getHeight();
            final float dxy = lineWidth; // dxy0 * getDebugBox();
            shape.moveTo(x1+dxy, y1+dxy, z);
            shape.lineTo(x1+dxy, y2-dxy, z);
            shape.lineTo(x2-dxy, y2-dxy, z);
            shape.lineTo(x2-dxy, y1+dxy, z);
            shape.lineTo(x1+dxy, y1+dxy, z);
            shape.closeLastOutline(true);
        }
        shape.setIsQuadraticNurbs();
        shape.setSharpness(oshapeSharpness);

        resetGLRegion(glp, gl, null, shape);
        region.addOutlineShape(shape, null, rgbaColor);
        box.resize(shape.getBounds());
        setRotationPivot( box.getCenter() );
    }

    @Override
    public String getSubString() {
        return super.getSubString()+", dim "+getWidth() + " x " + getHeight();
    }
}

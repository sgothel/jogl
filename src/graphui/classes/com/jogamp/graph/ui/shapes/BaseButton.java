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
import com.jogamp.graph.ui.GraphShape;

/**
 * An abstract GraphUI base filled button {@link GraphShape},
 * usually used as a backdrop or base shape for more informative button types.
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 * <p>
 * This button is rendered with a round oval shape {@link #ROUND_CORNER by default},
 * but can be set to {@link #PERP_CORNER rectangular shape}.
 * </p>
 */
public class BaseButton extends GraphShape {

    /** {@link #setCorner(float) Round corner}, value {@value}. This is the default value. */
    public static final float ROUND_CORNER = 1f;
    /** {@link #setCorner(float) Perpendicular corner} for a rectangular shape, value {@value}. */
    public static final float PERP_CORNER = 0f;

    protected float width;
    protected float height;
    protected float corner = ROUND_CORNER;

    public BaseButton(final int renderModes, final float width, final float height) {
        super(renderModes);
        this.width = width;
        this.height = height;
    }

    public final float getWidth() { return width; }

    public final float getHeight() { return height; }

    public final float getCorner() { return corner; }

    /**
     * Set corner size with range [0.01 .. 1.00] for round corners
     * or `zero` for perpendicular corners.
     * <p>
     *  , default is {@link #ROUND_CORNER round corner},
     * alternative a {@link #PERP_CORNER perpendicular corner} for a rectangular shape is available.
     * </p>
     * @see #ROUND_CORNER
     * @see #PERP_CORNER
     */
    public BaseButton setCorner(final float corner) {
        if( 0.01f <= corner && corner <= 1.0f ) {
            this.corner = corner;
        }
        if( corner > 1.0f ){
            this.corner = 1.0f;
        } else if( corner < 0.01f ){
            this.corner = 0.0f;
        } else {
            this.corner = corner;
        }
        markShapeDirty();
        return this;
    }

    public BaseButton setSize(final float width, final float height) {
        this.width = width;
        this.height = height;
        markShapeDirty();
        return this;
    }

    @Override
    protected void addShapeToRegion() {
        addBaseShapeToRegion(0f);
    }
    protected OutlineShape addBaseShapeToRegion(final float zOffset) {
        final OutlineShape shape = new OutlineShape();
        if(corner == 0.0f) {
            createSharpOutline(shape, zOffset);
        } else {
            createCurvedOutline(shape, zOffset);
        }
        shape.setIsQuadraticNurbs();
        shape.setSharpness(oshapeSharpness);
        region.addOutlineShape(shape, null, rgbaColor);
        box.resize(shape.getBounds());
        setRotationPivot( box.getCenter() );
        if( DEBUG_DRAW ) {
            System.err.println("GraphShape.RoundButton: Added Shape: "+shape+", "+box);
        }
        return shape;
    }

    protected void createSharpOutline(final OutlineShape shape, final float zOffset) {
        final float tw = getWidth();
        final float th = getHeight();

        final float minX = 0;
        final float minY = 0;
        final float minZ = zOffset;

        shape.addVertex(minX, minY, minZ,  true);
        shape.addVertex(minX+tw, minY,  minZ, true);
        shape.addVertex(minX+tw, minY + th, minZ,  true);
        shape.addVertex(minX, minY + th, minZ,  true);
        shape.closeLastOutline(true);
    }

    protected void createCurvedOutline(final OutlineShape shape, final float zOffset) {
        final float tw = getWidth();
        final float th = getHeight();
        final float dC = 0.5f*corner*Math.min(tw, th);

        final float minX = 0;
        final float minY = 0;
        final float minZ = zOffset;

        shape.addVertex(minX,           minY + dC,      minZ, true);
        shape.addVertex(minX,           minY,           minZ, false);

        shape.addVertex(minX + dC,      minY,           minZ, true);

        shape.addVertex(minX + tw - dC, minY,           minZ, true);
        shape.addVertex(minX + tw,      minY,           minZ, false);
        shape.addVertex(minX + tw,      minY + dC,      minZ, true);
        shape.addVertex(minX + tw,      minY + th- dC,  minZ, true);
        shape.addVertex(minX + tw,      minY + th,      minZ, false);
        shape.addVertex(minX + tw - dC, minY + th,      minZ, true);
        shape.addVertex(minX + dC,      minY + th,      minZ, true);
        shape.addVertex(minX,           minY + th,      minZ, false);
        shape.addVertex(minX,           minY + th - dC, minZ, true);

        shape.closeLastOutline(true);
    }

    @Override
    public String getSubString() {
        return super.getSubString()+", dim "+getWidth() + " x " + getHeight() + ", corner " + corner;
    }
}

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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.opengl.math.geom.AABBox;

/**
 * GPU based resolution independent Button impl
 */
public class LabelButton extends RoundButton {
    /** {@value} */
    public static final float DEFAULT_SPACING_X = 0.08f;
    /** {@value} */
    public static final float DEFAULT_SPACING_Y = 0.40f;

    private static final float DEFAULT_2PASS_LABEL_ZOFFSET = -0.05f;

    private final Label0 label;
    private float spacingX = DEFAULT_SPACING_X;
    private float spacingY = DEFAULT_SPACING_Y;

    public LabelButton(final Factory<? extends Vertex> factory, final int renderModes,
                       final Font labelFont, final String labelText,
                       final float width, final float height) {
        super(factory, renderModes | Region.COLORCHANNEL_RENDERING_BIT, width, height);
        this.label = new Label0(labelFont, labelText, new float[] { 1.33f, 1.33f, 1.33f, 1.0f }); // 0.75 * 1.33 = 1.0
        setColor(0.75f, 0.75f, 0.75f, 1.0f);
        setPressedColorMod(0.9f, 0.9f, 0.9f, 0.7f);
        setToggleOffColorMod(0.65f, 0.65f, 0.65f, 1.0f);
        setToggleOnColorMod(0.85f, 0.85f, 0.85f, 1.0f);
    }

    @Override
    public void drawShape(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount) {
        if( false ) {
            // Setup poly offset for z-fighting
            gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(0f, 1f);
            super.drawShape(gl, renderer, sampleCount);
            gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
        } else {
            super.drawShape(gl, renderer, sampleCount);
        }
    }

    @Override
    protected void addShapeToRegion(final GL2ES2 gl, final RegionRenderer renderer) {
        final OutlineShape shape = new OutlineShape(renderer.getRenderState().getVertexFactory());
        if(corner == 0.0f) {
            createSharpOutline(shape, DEFAULT_2PASS_LABEL_ZOFFSET);
        } else {
            createCurvedOutline(shape, DEFAULT_2PASS_LABEL_ZOFFSET);
        }
        shape.setIsQuadraticNurbs();
        shape.setSharpness(shapesSharpness);
        region.addOutlineShape(shape, null, rgbaColor);
        box.resize(shape.getBounds());

        // Precompute text-box size .. guessing pixelSize
        final float lPixelSize0 = 10f;
        final float lw = width * ( 1f - spacingX ) ;
        final float lh = height * ( 1f - spacingY ) ;
        final AABBox lbox0 = label.font.getMetricBounds(label.text, lPixelSize0);
        final float lsx = lw / lbox0.getWidth();
        final float lsy = lh / lbox0.getHeight();
        final float lPixelSize1 = lsx < lsy ? lPixelSize0 * lsx : lPixelSize0 * lsy;
        if( DRAW_DEBUG_BOX ) {
            System.err.println("RIButton: spacing "+spacingX+", "+spacingY);
            System.err.println("RIButton: bbox "+box);
            System.err.println("RIButton: lbox "+lbox0+", "+label.text);
            System.err.println("RIButton: net-text "+lw+" x "+lh);
            System.err.println("RIButton: lsx "+lsx+", lsy "+lsy+": pixelSize "+lPixelSize0+" -> "+lPixelSize1);
        }

        // Setting pixelSize based on actual text-box size
        final AABBox lbox1 = label.font.getPointsBounds(null, label.text, lPixelSize1, tempT1, tempT2);
        // Center text .. (share same center w/ button)
        final float[] lctr = lbox1.getCenter();
        final float[] ctr = box.getCenter();
        final float[] ltx = new float[] { ctr[0] - lctr[0], ctr[1] - lctr[1], 0f };

        final AABBox lbox2 = label.addShapeToRegion(lPixelSize1, region, tempT1.setToTranslation(ltx[0], ltx[1]));
        if( DRAW_DEBUG_BOX ) {
            System.err.printf("RIButton.0: lbox1 %s%n", lbox1);
            System.err.printf("RIButton.0: lbox2 %s%n", lbox2);
        }

        setRotationOrigin( ctr[0], ctr[1], ctr[2]);

        if( DRAW_DEBUG_BOX ) {
            System.err.println("XXX.UIShape.RIButton: Added Shape: "+shape+", "+box);
        }
    }

    public final float getSpacingX() { return spacingX; }
    public final float getSpacingY() { return spacingY; }

    /**
     * In percent of text label
     * @param spacingX spacing in percent on X, default is {@link #DEFAULT_SPACING_X}
     * @param spacingY spacing in percent on Y, default is {@link #DEFAULT_SPACING_Y}
     */
    public final void setSpacing(final float spacingX, final float spacingY) {
        if ( spacingX < 0.0f ) {
            this.spacingX = 0.0f;
        } else if ( spacingX > 1.0f ) {
            this.spacingX = 1.0f;
        } else {
            this.spacingX = spacingX;
        }
        if ( spacingY < 0.0f ) {
            this.spacingY = 0.0f;
        } else if ( spacingY > 1.0f ) {
            this.spacingY = 1.0f;
        } else {
            this.spacingY = spacingY;
        }
        markShapeDirty();
    }

    public final float[] getLabelColor() {
        return label.getColor();
    }

    public final void setLabelColor(final float r, final float g, final float b) {
        label.setColor(r, g, b, 1.0f);
        markShapeDirty();
    }

    public final void setLabelText(final Font labelFont, final String labelText) {
        label.setFont(labelFont);
        label.setText(labelText);
        markShapeDirty();
    }

    @Override
    public String getSubString() {
        return super.getSubString()+", "+ label + ", " + "spacing: " + spacingX+"/"+spacingY;
    }
}

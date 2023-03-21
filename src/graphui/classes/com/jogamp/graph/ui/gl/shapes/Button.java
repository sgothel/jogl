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

import com.jogamp.opengl.GL2ES2;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.opengl.math.geom.AABBox;

import jogamp.graph.ui.shapes.Label0;

/**
 * A GraphUI text labeled {@link RoundButton} {@link Shape}
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 * <p>
 * This button is rendered with a round oval shape.
 * To render it rectangular, {@link #setCorner(float)} to zero.
 * </p>
 */
public class Button extends RoundButton {
    /** {@value} */
    public static final float DEFAULT_SPACING_X = 0.12f;
    /** {@value} */
    public static final float DEFAULT_SPACING_Y = 0.42f;

    private static final float DEFAULT_2PASS_LABEL_ZOFFSET = -0.005f; // -0.05f;
    private float twoPassLabelZOffset = DEFAULT_2PASS_LABEL_ZOFFSET;

    private final Label0 label;
    private float spacingX = DEFAULT_SPACING_X;
    private float spacingY = DEFAULT_SPACING_Y;

    public Button(final int renderModes, final Font labelFont,
                  final String labelText, final float width,
                  final float height) {
        super(renderModes | Region.COLORCHANNEL_RENDERING_BIT, width, height);
        this.label = new Label0(labelFont, labelText, new float[] { 1.33f, 1.33f, 1.33f, 1.0f }); // 0.75 * 1.33 = 1.0
        setColor(0.75f, 0.75f, 0.75f, 1.0f);
        setPressedColorMod(0.9f, 0.9f, 0.9f, 0.7f);
        setToggleOffColorMod(0.65f, 0.65f, 0.65f, 1.0f);
        setToggleOnColorMod(0.85f, 0.85f, 0.85f, 1.0f);
    }

    public Font getFont() { return label.getFont(); }
    public String getLaben() { return label.getText(); }

    @Override
    public void draw(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount) {
        // Setup poly offset for z-fighting
        // gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
        // gl.glPolygonOffset(0f, 1f);
        super.draw(gl, renderer, sampleCount);
        // gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
    }

    @Override
    protected void addShapeToRegion() {
        final OutlineShape shape = new OutlineShape(vertexFactory);
        if(corner == 0.0f) {
            createSharpOutline(shape, twoPassLabelZOffset);
        } else {
            createCurvedOutline(shape, twoPassLabelZOffset);
        }
        shape.setIsQuadraticNurbs();
        shape.setSharpness(shapesSharpness);
        region.addOutlineShape(shape, null, rgbaColor);
        box.resize(shape.getBounds());

        // Precompute text-box size .. guessing pixelSize
        final float lw = width * ( 1f - spacingX ) ;
        final float lh = height * ( 1f - spacingY ) ;
        final AABBox lbox0_em = label.getFont().getGlyphBounds(label.getText(), tempT1, tempT2);
        final float lsx = lw / lbox0_em.getWidth();
        final float lsy = lh / lbox0_em.getHeight();
        final float lScale = lsx < lsy ? lsx : lsy;

        // Setting left-corner transform using text-box in font em-size [0..1]
        final AABBox lbox1_s = new AABBox(lbox0_em).scale2(lScale, new float[3]);
        // Center text .. (share same center w/ button)
        final float[] lctr = lbox1_s.getCenter();
        final float[] ctr = box.getCenter();
        final float[] ltx = new float[] { ctr[0] - lctr[0], ctr[1] - lctr[1], 0f };

        if( DRAW_DEBUG_BOX ) {
            System.err.println("RIButton: dim "+width+" x "+height+", spacing "+spacingX+", "+spacingY);
            System.err.println("RIButton: net-text "+lw+" x "+lh);
            System.err.println("RIButton: shape "+box);
            System.err.println("RIButton: text_em "+lbox0_em+" em, "+label.getText());
            System.err.println("RIButton: lscale "+lsx+" x "+lsy+" -> "+lScale);
            System.err.printf ("RIButton: text_s  %s%n", lbox1_s);
            System.err.printf ("RIButton: tleft %f / %f, %f / %f%n", ltx[0], ltx[1], ltx[0] * lScale, ltx[1] * lScale);
        }

        final AABBox lbox2 = label.addShapeToRegion(lScale, region, tempT1.setToTranslation(ltx[0], ltx[1]), tempT2, tempT3, tempT4);
        if( DRAW_DEBUG_BOX ) {
            System.err.printf("RIButton.X: lbox2 %s%n", lbox2);
        }

        setRotationOrigin( ctr[0], ctr[1], ctr[2]);

        if( DRAW_DEBUG_BOX ) {
            System.err.println("XXX.UIShape.RIButton: Added Shape: "+shape+", "+box);
        }
    }

    public float get2PassLabelZOffset() { return twoPassLabelZOffset; }

    public void set2PassLabelZOffset(final float v) {
        twoPassLabelZOffset = v;
        markShapeDirty();
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

    public final void setFont(final Font labelFont) {
        if( !label.getFont().equals(labelFont) ) {
            label.setFont(labelFont);
            markShapeDirty();
        }
    }
    public final void setLabel(final String labelText) {
        if( !label.getText().equals(labelText) ) {
            label.setText(labelText);
            markShapeDirty();
        }
    }
    public final void setLabel(final Font labelFont, final String labelText) {
        if( !label.getText().equals(labelText) || !label.getFont().equals(labelFont) ) {
            label.setFont(labelFont);
            label.setText(labelText);
            markShapeDirty();
        }
    }

    @Override
    public String getSubString() {
        return super.getSubString()+", "+ label + ", " + "spacing["+spacingX+", "+spacingY+"]";
    }
}

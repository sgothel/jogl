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

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.ui.GraphShape;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.Scene.PMVMatrixSetup;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.plane.AffineTransform;
import com.jogamp.opengl.util.texture.TextureSequence;

import jogamp.graph.ui.shapes.Label0;

/**
 * A GraphUI text labeled {@link BaseButton} {@link GraphShape}
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 * <p>
 * This button is rendered with a round oval shape.
 * To render it rectangular, {@link #setCorner(float)} to zero.
 * </p>
 */
public class Button extends BaseButton {
    /** {@value} */
    public static final float DEFAULT_SPACING_X = 0.12f;
    /** {@value} */
    public static final float DEFAULT_SPACING_Y = 0.42f;

    /**
     * Default {@link #setLabelZOffset(float) Z-axis offset},
     * using the smallest resolvable Z separation rounded value {@value} at 16-bits depth buffer, -1 z-distance and 0.1 z-near,
     * used to separate the {@link BaseButton} from the {@link Label}.
     * <p>
     * {@link FloatUtil#getZBufferEpsilon(int, float, float)}
     * <pre>
     * 1.5256461E-4 = 16 zBits, -0.2 zDist, 0.1 zNear
     * 6.1033297E-6 = 16 zBits, -1.0 zDist, 0.1 zNear
     * </pre>
     * </p>
     */
    public static final float DEFAULT_LABEL_ZOFFSET = 0.000153f; // 0.00015256461 = 16 zBits, -1 zDist, 0.1 zNear, i.e. FloatUtil.getZBufferEpsilon(16, -1f, 0.1f)
    private float labelZOffset;

    private final Label0 label;
    private float spacingX = DEFAULT_SPACING_X;
    private float spacingY = DEFAULT_SPACING_Y;

    /**
     * Create a text labeled button Graph based {@link GLRegion} UI {@link Shape}.
     * <p>
     * Sets the {@link #setLabelZOffset(float) Z-axis offset} to
     * a default smallest resolvable Z separation rounded value {@code 0.000153} at 16-bits depth buffer, -1 z-distance and 0.1 z-near,
     * used to separate the {@link BaseButton} from the {@link Label}.
     * </p>
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param labelFont {@link Font} for the label
     * @param labelText the label text
     * @param width width of the button
     * @param height height of the button
     * @see #Button(int, Font, CharSequence, float, float, float)
     */
    public Button(final int renderModes, final Font labelFont, final CharSequence labelText,
                  final float width, final float height) {
        this(renderModes, labelFont, labelText, width, height, DEFAULT_LABEL_ZOFFSET);
    }

    /**
     * Create a text labeled button Graph based {@link GLRegion} UI {@link Shape}.
     * <p>
     * Sets the {@link #setLabelZOffset(float) Z-axis offset} to the smallest resolvable Z separation at the given range,
     * used to separate the {@link BaseButton} from the {@link Label}.
     * </p>
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param labelFont {@link Font} for the label
     * @param labelText the label text
     * @param width width of the button
     * @param height height of the button
     * @param zBits depth buffer bit-depth, minimum 16-bit
     * @param setup {@link PMVMatrixSetup} for scene-distance as z-position and zNear
     * @see FloatUtil#getZBufferEpsilon(int, float, float)
     * @see Scene#getZEpsilon(int, com.jogamp.graph.ui.Scene.PMVMatrixSetup)
     */
    public Button(final int renderModes, final Font labelFont, final CharSequence labelText,
                  final float width, final float height, final int zBits, final Scene.PMVMatrixSetup setup) {
        this(renderModes, labelFont, labelText, width, height, Scene.getZEpsilon(zBits, setup));
    }

    /**
     * Create a text labeled button Graph based {@link GLRegion} UI {@link Shape}.
     * <p>
     * Sets the {@link #setLabelZOffset(float) Z-axis offset} to the smallest resolvable Z separation at the given range,
     * used to separate the {@link BaseButton} from the {@link Label}.
     * </p>
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param labelFont {@link Font} for the label
     * @param labelText the label text
     * @param width width of the button
     * @param height height of the button
     * @param zBits depth buffer bit-depth, minimum 16-bit
     * @param scene {@link Scene} to use {@link Scene#getZEpsilon(int)}
     * @see FloatUtil#getZBufferEpsilon(int, float, float)
     * @see Scene#getZEpsilon(int)
     */
    public Button(final int renderModes, final Font labelFont, final CharSequence labelText,
                  final float width, final float height, final int zBits, final Scene scene) {
        this(renderModes, labelFont, labelText, width, height, scene.getZEpsilon(zBits));
    }

    /**
     * Create a text labeled button Graph based {@link GLRegion} UI {@link Shape}.
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param labelFont {@link Font} for the label
     * @param labelText the label text
     * @param width width of the button
     * @param height height of the button
     * @param zOffset the Z-axis offset, used to separate the {@link BaseButton} from the {@link Label}
     * @see FloatUtil#getZBufferEpsilon(int, float, float)
     */
    public Button(final int renderModes, final Font labelFont, final CharSequence labelText,
                  final float width, final float height, final float zOffset) {
        super(renderModes | Region.COLORCHANNEL_RENDERING_BIT, width, height);
        this.labelZOffset = zOffset;
        this.label = new Label0(labelFont, labelText, new Vec4f( 1.66f, 1.66f, 1.66f, 1.0f )); // 0.60 * 1.66 ~= 1.0
    }

    /** Returns the label {@link Font}. */
    public Font getFont() { return label.getFont(); }
    /** Returns the label text. */
    public CharSequence getText() { return label.getText(); }

    @Override
    public void draw(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount) {
        // No need to setup an poly offset for z-fighting, using one region now
        // Setup poly offset for z-fighting
        // gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
        // gl.glPolygonOffset(0f, 1f);
        super.draw(gl, renderer, sampleCount);
        // gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
    }

    @Override
    protected void addShapeToRegion(final GLProfile glp, final GL2ES2 gl) {
        final AffineTransform tempT1 = new AffineTransform();
        final AffineTransform tempT2 = new AffineTransform();
        final AffineTransform tempT3 = new AffineTransform();

        final OutlineShape shape = createBaseShape( FloatUtil.isZero(labelZOffset) ? 0f : -labelZOffset );
        box.resize(shape.getBounds());
        setRotationPivot( box.getCenter() );

        // Sum Region buffer size of base-shape + text
        final int[/*2*/] vertIndexCount = Region.countOutlineShape(shape, new int[2]);
        TextRegionUtil.countStringRegion(label.getFont(), label.getText(), vertIndexCount);
        resetGLRegion(glp, gl, null, vertIndexCount[0], vertIndexCount[1]);

        region.addOutlineShape(shape, null, rgbaColor);

        // Precompute text-box size .. guessing pixelSize
        final float lw = box.getWidth() * ( 1f - spacingX ) ;
        final float lh = box.getHeight() * ( 1f - spacingY ) ;
        final AABBox lbox0_em = label.getFont().getGlyphBounds(label.getText(), tempT1, tempT2);
        // final AABBox lbox0_em = label.getFont().getGlyphShapeBounds(null, label.getText(), tempT1, tempT2);
        final float lsx = lw / lbox0_em.getWidth();
        final float lsy = lh / lbox0_em.getHeight();
        final float lScale = lsx < lsy ? lsx : lsy;

        // Setting left-corner transform using text-box in font em-size [0..1]
        final AABBox lbox1_s = new AABBox(lbox0_em).scale2(lScale);
        // Center text .. (share same center w/ button)
        final Vec3f lctr = lbox1_s.getCenter();
        final Vec3f ctr = box.getCenter();
        final Vec2f ltxy = new Vec2f(ctr.x() - lctr.x(), ctr.y() - lctr.y() );

        if( DEBUG_DRAW ) {
            System.err.println("Button: dim "+width+" x "+height+", spacing "+spacingX+", "+spacingY);
            System.err.println("Button: net-text "+lw+" x "+lh);
            System.err.println("Button: shape "+box);
            System.err.println("Button: text_em "+lbox0_em+" em, "+label.getText());
            System.err.println("Button: lscale "+lsx+" x "+lsy+" -> "+lScale);
            System.err.printf ("Button: text_s  %s%n", lbox1_s);
            System.err.printf ("Button: ltxy %s, %f / %f%n", ltxy, ltxy.x() * lScale, ltxy.y() * lScale);
        }

        final AABBox lbox2 = label.addShapeToRegion(lScale, region, ltxy, tempT1, tempT2, tempT3);
        box.resize(lbox2);
        if( DEBUG_DRAW ) {
            System.err.printf("Button.X: lbox2 %s%n", lbox2);
            System.err.printf("Button.X: shape %s%n", box);
        }
    }

    public float getLabelZOffset() { return labelZOffset; }

    /**
     * Set the Z-axis offset to the given value,
     * used to separate the {@link BaseButton} from the {@link Label}.
     * @param v the zoffset
     * @return this instance for chaining
     * @see FloatUtil#getZBufferEpsilon(int, float, float)
     */
    public Button setLabelZOffset(final float v) {
        labelZOffset = v;
        markShapeDirty();
        return this;
    }

    /**
     * Set the Z-axis offset to the smallest resolvable Z separation at the given range,
     * used to separate the {@link BaseButton} from the {@link Label}.
     * @param zBits number of bits of Z precision, i.e. z-buffer depth
     * @param zDist distance from the eye to the object
     * @param zNear distance from eye to near clip plane
     * @return this instance for chaining
     * @see FloatUtil#getZBufferEpsilon(int, float, float)
     * @see Scene#getZEpsilon(int, com.jogamp.graph.ui.Scene.PMVMatrixSetup)
     */
    public Button setLabelZOffset(final int zBits, final float zDist, final float zNear) {
        return setLabelZOffset( FloatUtil.getZBufferEpsilon(zBits, zDist, zNear) );
    }

    /**
     * Set the Z-axis offset to the smallest resolvable Z separation at the given range,
     * used to separate the {@link BaseButton} from the {@link Label}.
     * @param zBits depth buffer bit-depth, minimum 16-bit
     * @param setup {@link PMVMatrixSetup} for scene-distance as z-position and zNear
     * @return this instance for chaining
     * @see FloatUtil#getZBufferEpsilon(int, float, float)
     * @see Scene#getZEpsilon(int, com.jogamp.graph.ui.Scene.PMVMatrixSetup)
     */
    public Button setLabelZOffset(final int zBits, final Scene.PMVMatrixSetup setup) {
        return setLabelZOffset( Scene.getZEpsilon(zBits, setup) );
    }

    /**
     * Set the Z-axis offset to the smallest resolvable Z separation at the given range,
     * used to separate the {@link BaseButton} from the {@link Label}.
     * @param zBits depth buffer bit-depth, minimum 16-bit
     * @param scene {@link Scene} to use {@link Scene#getZEpsilon(int)}
     * @return this instance for chaining
     * @see FloatUtil#getZBufferEpsilon(int, float, float)
     * @see Scene#getZEpsilon(int)
     */
    public Button setLabelZOffset(final int zBits, final Scene scene) {
        return setLabelZOffset( scene.getZEpsilon(zBits) );
    }

    public final float getSpacingX() { return spacingX; }
    public final float getSpacingY() { return spacingY; }

    /**
     * In percent of text label
     * @param spacingX spacing in percent on X, default is {@link #DEFAULT_SPACING_X}
     * @param spacingY spacing in percent on Y, default is {@link #DEFAULT_SPACING_Y}
     */
    public final Button setSpacing(final float spacingX, final float spacingY) {
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
        return this;
    }

    public final Vec4f getLabelColor() {
        return label.getColor();
    }

    public final Button setLabelColor(final float r, final float g, final float b) {
        label.setColor(r, g, b, 1.0f);
        markShapeDirty();
        return this;
    }

    public final Button setFont(final Font labelFont) {
        if( !label.getFont().equals(labelFont) ) {
            label.setFont(labelFont);
            markShapeDirty();
        }
        return this;
    }
    public final Button setText(final CharSequence labelText) {
        if( !label.getText().equals(labelText) ) {
            label.setText(labelText);
            markShapeDirty();
        }
        return this;
    }
    public final Button setText(final Font labelFont, final CharSequence labelText) {
        if( !label.getText().equals(labelText) || !label.getFont().equals(labelFont) ) {
            label.setFont(labelFont);
            label.setText(labelText);
            markShapeDirty();
        }
        return this;
    }

    @Override
    public String getSubString() {
        return super.getSubString()+", "+ label + ", " + "spacing["+spacingX+", "+spacingY+"]";
    }
}

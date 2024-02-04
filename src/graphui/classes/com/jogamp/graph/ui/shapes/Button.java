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
    public static final float DEFAULT_SPACING_X = 0.20f;
    /** {@value} */
    public static final float DEFAULT_SPACING_Y = 0.46f;

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

    private final Label0 labelOff, labelOn;
    private volatile Label0 labelNow;
    private final Vec2f spacing = new Vec2f(DEFAULT_SPACING_X, DEFAULT_SPACING_Y);
    private final Vec2f fixedLabelSize = new Vec2f(0, 0);

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
        this(renderModes, labelFont, labelText, null, width, height, DEFAULT_LABEL_ZOFFSET);
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
        this(renderModes, labelFont, labelText, null, width, height, zOffset);
    }

    /**
     * Create a text labeled button Graph based {@link GLRegion} UI {@link Shape}.
     * <p>
     * If {@code labelTextOn} is not {@code null}, constructor enables {@link #setToggleable(boolean) toggle-able} mode
     * to automatically switch the labels depending on {@link #isToggleOn()}.
     * </p>
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param labelFont {@link Font} for the label
     * @param labelTextOff the label text of the toggle-off state (current at creation), see {@link #isToggleOn()}
     * @param labelTextOn optional label text of the toggle-on state, see {@link #isToggleOn()}. If not {@code null}, enables {@link #setToggleable(boolean) toggle-able} mode.
     * @param width width of the button
     * @param height height of the button
     * @param zOffset the Z-axis offset, used to separate the {@link BaseButton} from the {@link Label}
     * @see FloatUtil#getZBufferEpsilon(int, float, float)
     */
    public Button(final int renderModes, final Font labelFont, final CharSequence labelTextOff, final CharSequence labelTextOn,
                  final float width, final float height, final float zOffset) {
        super(renderModes | Region.COLORCHANNEL_RENDERING_BIT, width, height);
        this.labelZOffset = zOffset;
        this.labelOff = new Label0(labelFont, labelTextOff, new Vec4f( 1.66f, 1.66f, 1.66f, 1.0f )); // 0.60 * 1.66 ~= 1.0
        this.labelNow = this.labelOff;
        if( null != labelTextOn ) {
            this.labelOn = new Label0(labelFont, labelTextOn, new Vec4f( 1.66f, 1.66f, 1.66f, 1.0f )); // 0.60 * 1.66 ~= 1.0
            this.setToggleable(true);
        } else {
            this.labelOn = null;
        }
    }

    @Override
    protected void toggleNotify(final boolean on) {
        int i=0;
        if( null != labelOn ) {
            if( on ) {
                labelNow = labelOn;
                i = 1;
            } else {
                labelNow = labelOff;
                i = -1;
            }
            markShapeDirty();
        }
    }

    /** Returns the label {@link Font}. */
    public Font getFont() { return labelNow.getFont(); }

    /** Returns the text of the current label. */
    public CharSequence getText() { return labelNow.getText(); }

    @Override
    public void draw(final GL2ES2 gl, final RegionRenderer renderer) {
        // No need to setup an poly offset for z-fighting, using one region now
        // Setup poly offset for z-fighting
        // gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
        // gl.glPolygonOffset(0f, 1f);
        super.draw(gl, renderer);
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
        TextRegionUtil.countStringRegion(labelNow.getFont(), labelNow.getText(), vertIndexCount);
        resetGLRegion(glp, gl, null, vertIndexCount[0], vertIndexCount[1]);

        region.addOutlineShape(shape, null, rgbaColor);

        // Precompute text-box size .. guessing pixelSize
        final AABBox lbox0_em = labelNow.getFont().getGlyphBounds(labelNow.getText(), tempT1, tempT2);
        final float lw = box.getWidth() * ( 1f - spacing.x() ) ;
        final float lsx = lw / Math.max(fixedLabelSize.x(), lbox0_em.getWidth());
        final float lh = box.getHeight() * ( 1f - spacing.y() ) ;
        final float lsy = lh / Math.max(fixedLabelSize.y(), lbox0_em.getHeight());
        final float lScale = lsx < lsy ? lsx : lsy;

        // Setting left-corner transform using text-box in font em-size [0..1]
        final AABBox lbox0_s = new AABBox(lbox0_em).scale2(lScale);
        // Center text .. (share same center w/ button)
        final Vec3f lctr = lbox0_s.getCenter();
        final Vec3f ctr = box.getCenter();
        final Vec2f ltxy = new Vec2f(ctr.x() - lctr.x(), ctr.y() - lctr.y() );

        if( DEBUG_DRAW ) {
            System.err.println("Button: dim "+width+" x "+height+", spacing "+spacing+", fixedLabelSize "+fixedLabelSize);
            System.err.println("Button: text0_em "+lbox0_em+" em, "+labelNow.getText());
            System.err.println("Button: shape   "+box);
            System.err.println("Button: text-space "+lw+" x "+lh);
            System.err.println("Button: lscale "+lsx+" x "+lsy+" -> "+lScale);
            System.err.printf ("Button: text0_s  %s%n", lbox0_s);
            System.err.printf ("Button: ltxy %s, %f / %f%n", ltxy, ltxy.x() * lScale, ltxy.y() * lScale);
            final float x0 = ( box.getWidth() - lbox0_s.getWidth() ) * 0.5f;
            final float y0 = ( box.getHeight() - lbox0_s.getHeight() ) * 0.5f;
            final AABBox lbox3 = new AABBox(new Vec3f(x0, y0, 0), new Vec3f(x0 + lbox0_s.getWidth(), y0 + lbox0_s.getHeight(), 0));
            addRectangle(region, this.oshapeSharpness, lbox3, null, 0.0001f, new Vec4f(0, 0, 0, 1));
            System.err.printf("Button.X: lbox3 %s%n", lbox3);
        }

        final AABBox lbox2 = labelNow.addShapeToRegion(lScale, region, ltxy, tempT1, tempT2, tempT3);
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

    /** Returns the current fixed label font size, see {@Link #setFixedLabelSize(Vec2f)} and {@link #setSpacing(Vec2f, Vec2f)}. */
    public final Vec2f getFixedLabelSize() { return fixedLabelSize; }

    /**
     * Sets fixed label font size clipped to range [0 .. 1], defaults to {@code 0, 0}.
     * <p>
     * Use {@code w=0, h=1} when using single symbols from fixed sized symbol fonts!
     * Use {@link #setSpacing(Vec2f, Vec2f)} to also set spacing.
     * </p>
     * <p>
     * The fixed label font size is used as the denominator when scaling.{@code max(fixedLabelSize, fontLabelSize)},
     * hence reasonable values are either {@code 1} to enable using the given font-size
     * for the axis or {@code 0} to scale up/down the font to match the button box less spacing for the axis.
     * </p>
     * @see #setSpacing(Vec2f, Vec2f)
     * @see #setSpacing(Vec2f)
     */
    public final Button setFixedLabelSize(final float w, final float h) {
        fixedLabelSize.set(
            Math.max(0f, Math.min(1f, w)),
            Math.max(0f, Math.min(1f, h)) );
        markShapeDirty();
        return this;
    }
    public final Button setFixedLabelSize(final Vec2f v) {
        return setFixedLabelSize(v.x(), v.y());
    }

    /** Returns the current spacing size, see {@Link #setSpacing(Vec2f)} and {@link #setSpacing(Vec2f, Vec2f)}. */
    public final Vec2f getSpacing() { return spacing; }

    /**
     * Sets spacing in percent of text label, clipped to range [0 .. 1].
     * @param spacingX spacing in percent on X, default is {@link #DEFAULT_SPACING_X}
     * @param spacingY spacing in percent on Y, default is {@link #DEFAULT_SPACING_Y}
     * @see #setSpacing(Vec2f)
     * @see #setSpacing(Vec2f, Vec2f)
     */
    public final Button setSpacing(final float spacingX, final float spacingY) {
        spacing.set(
            Math.max(0f, Math.min(1f, spacingX)),
            Math.max(0f, Math.min(1f, spacingY)) );
        markShapeDirty();
        return this;
    }
    /**
     * Sets spacing in percent of text label, clipped to range [0 .. 1].
     * @param spacingX spacing in percent on X, default is {@link #DEFAULT_SPACING_X}
     * @param spacingY spacing in percent on Y, default is {@link #DEFAULT_SPACING_Y}
     * @see #setSpacing(Vec2f, Vec2f)
     */
    public final Button setSpacing(final Vec2f spacing) {
        return setSpacing(spacing.x(), spacing.y());
    }
    /**
     * Sets spacing {@link #setSpacing(Vec2f)} and fixed label font size {@link #setFixedLabelSize(Vec2f)} for convenience.
     * @see #setSpacing(Vec2f)
     * @see #setFixedLabelSize(Vec2f)
     */
    public final Button setSpacing(final Vec2f spacing, final Vec2f fixedLabelSize) {
        setSpacing(spacing.x(), spacing.y());
        setFixedLabelSize(fixedLabelSize.x(), fixedLabelSize.y());
        return this;
    }

    /** Returns the label color. */
    public final Vec4f getLabelColor() {
        return labelNow.getColor();
    }

    /** Sets the label color, consider using alpha 1 */
    public final Button setLabelColor(final Vec4f c) {
        labelOff.setColor(c);
        if( null != labelOn ) {
            labelOn.setColor(c);
        }
        markShapeDirty();
        return this;
    }

    /** Sets the label color, consider using alpha 1 */
    public final Button setLabelColor(final float r, final float g, final float b, final float a) {
        labelOff.setColor(r, g, b, a);
        if( null != labelOn ) {
            labelOn.setColor(r, g, b, a);
        }
        markShapeDirty();
        return this;
    }

    /** Sets the label font. */
    public final Button setFont(final Font labelFont) {
        if( !labelOff.getFont().equals(labelFont) ) {
            labelOff.setFont(labelFont);
            markShapeDirty();
        }
        if( null != labelOn ) {
            if( !labelOn.getFont().equals(labelFont) ) {
                labelOn.setFont(labelFont);
                markShapeDirty();
            }
        }
        return this;
    }

    /** Sets the current label text. */
    public final Button setText(final CharSequence labelText) {
        if( !labelNow.getText().equals(labelText) ) {
            labelNow.setText(labelText);
            markShapeDirty();
        }
        return this;
    }

    /** Sets the current label text. */
    public final Button setText(final Font labelFont, final CharSequence labelText) {
        if( !labelNow.getText().equals(labelText) || !labelNow.getFont().equals(labelFont) ) {
            labelNow.setFont(labelFont);
            labelNow.setText(labelText);
            markShapeDirty();
        }
        return this;
    }

    @Override
    public String getSubString() {
        final String onS = null != labelOn ? ( labelOn + (labelNow == labelOn ? "*" : "" ) + ", " ) : "";
        final String offS = labelOff + (labelNow == labelOff ? "*" : "" ) + ", ";
        final String flsS = fixedLabelSize.isZero() ? "" : "fixedLabelSize["+fixedLabelSize+"], ";
        return super.getSubString()+", "+ offS + onS + "spacing["+spacing+"], "+flsS+"zOff "+labelZOffset;
    }
}

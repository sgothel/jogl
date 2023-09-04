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
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.texture.TextureSequence;
import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.Font.Glyph;
import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.graph.ui.GraphShape;

/**
 * A GraphUI text label {@link GraphShape}
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 */
public class Label extends GraphShape {
    private Font font;
    private float fontScale;
    private CharSequence text;

    /**
     * Label ctor using a separate {@code fontScale} to scale the em-sized type glyphs.
     * <p>
     * If possible, try using {@link Label#Label(int, Font, CharSequence)} and {@link #scale(float, float, float)}.
     * </p>
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param font {@link Font} for this label
     * @param fontScale font-scale factor, by which the em-sized type glyphs shall be scaled
     * @param text the label text
     * @see #Label(int, Font, CharSequence)
     */
    public Label(final int renderModes, final Font font, final float fontScale, final CharSequence text) {
        super(renderModes);
        this.font = font;
        this.fontScale = fontScale;
        this.text = text;
    }

    /**
     * Label ctor using em-size type glyphs
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param font {@link Font} for this label
     * @param text the label text
     * @see #Label(int, Font, float, CharSequence)
     */
    public Label(final int renderModes, final Font font, final CharSequence text) {
        this(renderModes, font, 1f, text);
    }

    /** Returns the label text. */
    public CharSequence getText() {
        return text;
    }

    /**
     * Set the text to be rendered. Shape update is pending until next {@link #draw(GL2ES2, RegionRenderer, int[])} or {@link #validate(GL2ES2)}.
     * @param text the text to be set.
     * @return true if text has been updated, false if unchanged.
     */
    public boolean setText(final CharSequence text) {
        if( !this.text.equals(text) ) {
            this.text = text;
            markShapeDirty();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Set the text to be rendered and immediately updates the shape if necessary.
     * @param gl {@link GL2ES2} to issue {@link #validate(GL2ES2)} in case text changed to immediately update shape and {@link #getBounds()}
     * @param text the text to be set.
     * @return true if text has been updated, false if unchanged.
     */
    public boolean setText(final GL2ES2 gl, final CharSequence text) {
        if( setText(text) ) {
            validate(gl);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Set the text to be rendered and immediately updates the shape if necessary.
     * @param glp {@link GLProfile} to issue {@link #validate(GLProfile)} in case text changed to immediately update shape and {@link #getBounds()}
     * @param text the text to be set.
     * @return true if text has been updated, false if unchanged.
     */
    public boolean setText(final GLProfile glp, final CharSequence text) {
        if( setText(text) ) {
            validate(glp);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the {@link Font} used to render the text
     */
    public Font getFont() {
        return font;
    }

    /**
     * Set the {@link Font} used to render the text
     * @param font the font to be set.
     * @return true if font has been updated, false if unchanged.
     */
    public boolean setFont(final Font font) {
        if( !this.font.equals(font) ) {
            this.font = font;
            markShapeDirty();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the font-scale factor, by which the em-sized type glyphs shall be scaled.
     */
    public float getFontScale() {
        return fontScale;
    }

    /** Returns {@link Font#getLineHeight()} * {@link #getFontScale()}. */
    public float getLineHeight() {
        return fontScale * font.getLineHeight();
    }

    /** Returns {@link Font#getLineHeight()} * {@link #getFontScale()} * {@link #getScaleY()}. */
    public float getScaledLineHeight() {
        return getScale().y() * fontScale * font.getLineHeight();
    }

    /**
     * Sets the font-scale factor, by which the em-sized type glyphs shall be scaled.
     * <p>
     * This will lead to a recreate the shape's region in case fontScale differs.
     * </p>
     * <p>
     * Use {@link #scale(float, float, float)} for non-expensive shape scaling.
     * </p>
     * @param fontScale font-scale factor, by which the em-sized type glyphs shall be scaled
     * @return true if font-scale has been updated, false if unchanged.
     */
    public boolean setFontScale(final float fontScale) {
        if( this.fontScale != fontScale ) {
            this.fontScale = fontScale;
            markShapeDirty();
            return true;
        } else {
            return false;
        }
    }

    private final Font.GlyphVisitor glyphVisitor = new Font.GlyphVisitor() {
        @Override
        public void visit(final char symbol, final Glyph glyph, final AffineTransform t) {
            if( !glyph.isNonContour() ) {
                final OutlineShape shape = glyph.getShape();
                shape.setSharpness(oshapeSharpness);
                region.addOutlineShape(shape, t, rgbaColor);
            }
        }
    };

    @Override
    protected void addShapeToRegion(final GLProfile glp, final GL2ES2 gl) {
        final AffineTransform tempT1 = new AffineTransform();
        final AffineTransform tempT2 = new AffineTransform();
        final AffineTransform tempT3 = new AffineTransform();

        final int[] vertIndCount = TextRegionUtil.countStringRegion(font, text, new int[2]);
        resetGLRegion(glp, gl, null, vertIndCount[0], vertIndCount[1]);

        AABBox fbox = font.getGlyphBounds(text, tempT2, tempT3);
        tempT1.setToScale(fontScale, fontScale);
        tempT1.translate(-fbox.getMinX(), -fbox.getMinY(), tempT2); // enforce bottom-left origin @ 0/0 for good drag-zoom experience
        fbox = font.processString(glyphVisitor, tempT1, text, tempT2, tempT3);
        setRotationPivot( fbox.getCenter() );
        box.copy(fbox);
    }

    @Override
    public String getSubString() {
        final int m = Math.min(text.length(), 8);
        return super.getSubString()+", fscale " + fontScale + ", '" + text.subSequence(0, m)+"'";
    }
}

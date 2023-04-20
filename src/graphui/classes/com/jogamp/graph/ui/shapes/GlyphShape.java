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

import java.util.List;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.Font.Glyph;
import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.graph.ui.GraphShape;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.texture.TextureSequence;

/**
 * Representing a single {@link Font.Glyph} as a {@link GraphShape}
 *
 * A GlyphShape is represented in font em-size [0..1] unscaled w/ bottom-left origin at 0/0
 * while preserving an intended position, see {@link #getOrigPos()} and {@link #getOrigPos(float)}.
 *
 * Scaling, if any, should be applied via {@link #setScale(float, float, float)} etc.
 */
public class GlyphShape extends GraphShape {
    private final char symbol;
    private final Glyph glyph;
    private final int regionVertCount;
    private final int regionIdxCount;
    private final Vec3f origPos;

    /**
     * Creates a new GlyphShape
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param symbol the represented character
     * @param glyph the {@link Font.Glyph}
     * @param x the intended unscaled X position of this Glyph, e.g. if part of a string - otherwise use zero.
     * @param y the intended unscaled Y position of this Glyph, e.g. if part of a string - otherwise use zero.
     * @see #processString(List, int, Font, String)
     */
    public GlyphShape(final int renderModes, final char symbol, final Glyph glyph, final float x, final float y) {
        super(renderModes);
        this.symbol = symbol;
        this.glyph = glyph;
        this.origPos = new Vec3f(x, y, 0f);
        if( glyph.isWhiteSpace() || null == glyph.getShape() ) {
            setEnabled(false);
        }
        final int[/*2*/] vertIndexCount = Region.countOutlineShape(glyph.getShape(), new int[2]);
        regionVertCount = vertIndexCount[0];
        regionIdxCount = vertIndexCount[1];
    }

    /**
     * Creates a new GlyphShape
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param font the {@link Font} to lookup the symbol's {@link Font.Glyph}
     * @param symbol the represented character
     * @param x the intended unscaled X position of this Glyph, e.g. if part of a string - otherwise use zero.
     * @param y the intended unscaled Y position of this Glyph, e.g. if part of a string - otherwise use zero.
     */
    public GlyphShape(final int renderModes, final Font font, final char symbol, final float x, final float y) {
        this(renderModes, symbol, font.getGlyph( font.getGlyphID(symbol) ), x, y);
    }

    /** Returns the char symbol to be rendered. */
    public char getSymbol() {
        return symbol;
    }

    /**
     * Returns the {@link Font.Glyph} to be rendered.
     */
    public Glyph getGlyph() {
        return glyph;
    }

    /**
     * Returns the {@link Font} used to render the text
     */
    public Font getFont() {
        return glyph.getFont();
    }

    /**
     * Returns the unscaled original position of this glyph, e.g. if part of a string, otherwise zero.
     *
     * Method borrows and returns the internal instance.
     *
     * @see #processString(List, int, Font, String)
     */
    public Vec3f getOrigPos() { return origPos; }

    /**
     * Returns the unscaled original position of this glyph, e.g. if part of a string, otherwise zero.
     *
     * @param s {@link Vec3f} storage to be returned
     * @return storage containing the unscaled original position
     * @see #processString(List, int, Font, String)
     */
    public Vec3f getOrigPos(final Vec3f s) { return s.set(origPos); }

    /**
     * Returns a copy of the scaled original position of this glyph, see {@link #getOrigPos(Vec3f)}
     * @see #processString(List, int, Font, String)
     */
    public Vec3f getOrigPos(final float scale) { return origPos.mul(scale); }

    /**
     * Returns the scaled original position of this glyph, see {@link #getOrigPos(float)}
     * @param s {@link Vec3f} storage to be returned
     * @return storage containing the scaled original position
     * @see #processString(List, int, Font, String)
     */
    public Vec3f getOrigPos(final Vec3f s, final float scale) { return s.set(origPos).scale(scale); }

    /** Resets this Shape's position to the scaled original position, see {@link #getOrigPos(float)}. */
    public void resetPos(final float scale) {
        moveTo(origPos.x() * scale, origPos.y() * scale, 0f);
    }

    /** Resets this Shape's position to the scaled original position and {@link #setScale(float, float, float) set scale}, see {@link #resetPos(float)}. */
    public void resetPosAndScale(final float scale) {
        moveTo(origPos.x() * scale, origPos.y() * scale, 0f);
        setScale(scale, scale, 1f);
    }

    /** Returns {@link Font#getLineHeight()}. */
    public float getLineHeight() {
        return glyph.getFont().getLineHeight();
    }

    /**
     * Process the given text resulting in a list of {@link GlyphShape}s with stored original position {@link #getOrigX()} and {@link #getOrigY()} each at font em-size [0..1].
     * @param res storage for resulting {@link GlyphShape}s.
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param font {@link Font} used
     * @param text text to be represented
     * @return the bounding box of the given string by taking each glyph's font em-sized [0..1] OutlineShape into account.
     * @see #getOrigX()
     * @see #getOrigY()
     */
    public static final AABBox processString(final List<GlyphShape> res, final int renderModes, final Font font, final String text) {
        final Font.GlyphVisitor fgv = new Font.GlyphVisitor() {
            @Override
            public void visit(final char symbol, final Glyph glyph, final AffineTransform t) {
                if( !glyph.isWhiteSpace() && null != glyph.getShape() ) {
                    res.add( new GlyphShape(renderModes, symbol, glyph, t.getTranslateX(), t.getTranslateY()) );
                }
            }
        };
        return font.processString(fgv, null, text, new AffineTransform(), new AffineTransform());
    }

    @Override
    protected void addShapeToRegion(final GLProfile glp, final GL2ES2 gl) {
        final OutlineShape shape = glyph.getShape();
        box.reset();
        if( null != shape ) {
            final AABBox sbox = shape.getBounds();
            final AffineTransform tmp = new AffineTransform();
            // Enforce bottom-left origin @ 0/0 for good drag-zoom experience,
            // but keep the underline (decline) intact!
            tmp.setToTranslation(-sbox.getMinX(), -sbox.getMinY() + glyph.getBounds().getMinY());
            shape.setSharpness(oshapeSharpness);

            updateGLRegion(glp, gl, null, regionVertCount, regionIdxCount);
            region.addOutlineShape(shape, tmp, rgbaColor);
            box.resize(tmp.transform(sbox, new AABBox()));
            setRotationPivot( box.getCenter() );
        }
    }

    @Override
    public String getSubString() {
        return super.getSubString()+", origPos " + origPos.x() + " / " + origPos.y() + ", '" + symbol + "'";
    }
}

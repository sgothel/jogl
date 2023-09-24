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
import com.jogamp.graph.ui.GraphShape;
import com.jogamp.math.Vec3f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.plane.AffineTransform;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureSequence;

/**
 * Representing a single {@link Font.Glyph} as a {@link GraphShape}
 *
 * A GlyphShape is represented in font em-size [0..1] unscaled w/ bottom-left origin at 0/0
 * while preserving an intended position, see {@link #getOrigPos()}.
 *
 * Scaling, if any, should be applied via {@link #setScale(float, float, float)} etc.
 */
public class GlyphShape extends GraphShape {
    private final Glyph glyph;
    private final int regionVertCount;
    private final int regionIdxCount;
    private final Vec3f origPos;

    /**
     * Creates a new GlyphShape
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param glyph the {@link Font.Glyph}
     * @param x the intended unscaled X position of this Glyph, e.g. if part of a string - otherwise use zero.
     * @param y the intended unscaled Y position of this Glyph, e.g. if part of a string - otherwise use zero.
     * @see #processString(List, int, Font, String)
     */
    public GlyphShape(final int renderModes, final Glyph glyph, final float x, final float y) {
        super(renderModes);
        this.glyph = glyph;
        this.origPos = new Vec3f(x, y, 0f);
        if( glyph.isNonContour() ) {
            setEnabled(false);
        }
        final int[/*2*/] vertIndexCount = Region.countOutlineShape(glyph.getShape(), new int[2]);
        regionVertCount = vertIndexCount[0];
        regionIdxCount = vertIndexCount[1];
    }

    /**
     * Creates a new GlyphShape
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param glyph the {@link Font.Glyph}
     * @param pos the intended unscaled Vec3f position of this Glyph, e.g. if part of a string - otherwise use zero.
     * @see #processString(List, int, Font, String)
     */
    public GlyphShape(final int renderModes, final Glyph glyph, final Vec3f pos) {
        this(renderModes, glyph, pos.x(), pos.y());
    }

    /**
     * Creates a new GlyphShape
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param font the {@link Font} to lookup the symbol's {@link Font.Glyph}
     * @param codepoint the represented character unicode `codepoint` symbol
     * @param x the intended unscaled X position of this Glyph, e.g. if part of a string - otherwise use zero.
     * @param y the intended unscaled Y position of this Glyph, e.g. if part of a string - otherwise use zero.
     */
    public GlyphShape(final int renderModes, final Font font, final char codepoint, final float x, final float y) {
        this(renderModes, font.getGlyph( codepoint ), x, y);
    }

    /** GlyphShape copy-ctor */
    public GlyphShape(final GlyphShape orig) {
        this(orig.renderModes, orig.glyph, orig.origPos);
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
    public static final AABBox processString(final List<GlyphShape> res, final int renderModes,
                                             final Font font, final CharSequence text)
    {
        final Font.GlyphVisitor fgv = new Font.GlyphVisitor() {
            @Override
            public void visit(final Glyph glyph, final AffineTransform t) {
                if( !glyph.isNonContour() ) {
                    res.add( new GlyphShape(renderModes, glyph, t.getTranslateX(), t.getTranslateY()) );
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

            resetGLRegion(glp, gl, null, regionVertCount, regionIdxCount);
            region.addOutlineShape(shape, tmp, rgbaColor);
            box.resize(tmp.transform(sbox, new AABBox()));
            setRotationPivot( box.getCenter() );
        } else {
            // needs a dummy 'region'
            resetGLRegion(glp, gl, null, regionVertCount, regionIdxCount);
        }
    }

    @Override
    public String getSubString() {
        return super.getSubString()+", origPos " + origPos.x() + " / " + origPos.y() + ", cp 0x" + Integer.toHexString(glyph.getCodepoint());
    }
}

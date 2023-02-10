/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
package jogamp.graph.font.typecast;

import com.jogamp.common.util.IntIntHashMap;
import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.font.Font;
import com.jogamp.opengl.math.geom.AABBox;

import jogamp.graph.font.typecast.ot.table.PostTable;

public final class TypecastGlyph implements Font.Glyph {

    /** Scaled hmtx value */
    public static final class Advance
    {
        private final Font      font;
        private final int       advance; // in font-units
        private final IntIntHashMap size2advanceI;

        public Advance(final Font font, final int advance)
        {
            this.font = font;
            this.advance = advance;
            if( TypecastFont.USE_PRESCALED_ADVANCE ) {
                size2advanceI = new IntIntHashMap();
                size2advanceI.setKeyNotFoundValue(0);
            } else {
                size2advanceI = null;
            }
        }

        public final void reset() {
            if( TypecastFont.USE_PRESCALED_ADVANCE ) {
                size2advanceI.clear();
            }
        }

        public final Font getFont() { return font; }

        public final int getUnitsPerEM() { return this.font.getMetrics().getUnitsPerEM(); }

        public final float getScale(final int funits)
        {
            return this.font.getMetrics().getScale(funits);
        }

        public final void add(final float pixelSize, final float advance)
        {
            if( TypecastFont.USE_PRESCALED_ADVANCE ) {
                size2advanceI.put(Float.floatToIntBits(pixelSize), Float.floatToIntBits(advance));
            }
        }

        public final float get(final float pixelSize)
        {
            if( !TypecastFont.USE_PRESCALED_ADVANCE ) {
                return pixelSize * font.getMetrics().getScale( advance );
            } else {
                final int sI = Float.floatToIntBits( (float) Math.ceil( pixelSize ) );
                final int aI = size2advanceI.get(sI);
                if( 0 != aI ) {
                    return Float.intBitsToFloat(aI);
                }
                return pixelSize * font.getMetrics().getScale( advance );
            }
        }

        @Override
        public final String toString()
        {
            return "\nAdvance:"+
                "\n  advance: "+this.advance+
                "\n advances: \n"+size2advanceI;
        }
    }

    public static final class Metrics
    {
        private final TypecastFont font;
        private final AABBox    bbox; // in font-units
        private final int      advance; // in font-units
        private final Advance advance2;

        /**
         *
         * @param font
         * @param bbox in font-units
         * @param advance hmtx value in font-units
         */
        public Metrics(final TypecastFont font, final AABBox bbox, final int advance)
        {
            this.font = font;
            this.bbox = bbox;
            this.advance = advance;
            if( TypecastFont.USE_PRESCALED_ADVANCE ) {
                this.advance2 = new Advance(font, advance);
            } else {
                this.advance2 = null;
            }
        }

        public final void reset() {
            if( TypecastFont.USE_PRESCALED_ADVANCE ) {
                advance2.reset();
            }
        }

        public final TypecastFont getFont() { return font; }

        public final int getUnitsPerEM() { return font.getMetrics().getUnitsPerEM(); }

        public final float getScale(final int funits) { return font.getMetrics().getScale(funits); }

        /** in font-units */
        public final AABBox getBBoxFU() { return this.bbox; }

        /** Return advance in font units to be divided by unitsPerEM */
        public final int getAdvanceFU() { return this.advance; }

        public final void addAdvance(final float pixelSize, final float advance) {
            if( TypecastFont.USE_PRESCALED_ADVANCE ) {
                this.advance2.add(pixelSize, advance);
            }
        }

        public final float getAdvance(final float pixelSize) {
            if( TypecastFont.USE_PRESCALED_ADVANCE ) {
                return this.advance2.get(pixelSize);
            } else {
                return pixelSize * font.getMetrics().getScale( advance );
            }
        }

        @Override
        public final String toString()
        {
            return "\nMetrics:"+
                "\n  bbox: "+this.bbox+
                "\n  advance: "+this.advance+
                "\n  advance2: "+this.advance2;
        }
    }

    public static final short INVALID_ID    = (short)((1 << 16) - 1);
    public static final short MAX_ID        = (short)((1 << 16) - 2);

    private final char symbol;
    private final OutlineShape shape; // in EM units
    private final int id;
    private final Metrics metrics;

    /**
     *
     * @param font
     * @param symbol
     * @param id
     * @param bbox in font-units
     * @param advance from hmtx in font-units
     * @param shape
     */
    protected TypecastGlyph(final TypecastFont font, final char symbol, final int id, final AABBox bbox, final int advance, final OutlineShape shape) {
        this.symbol = symbol;
        this.shape = shape;
        this.id = id;
        this.metrics = new Metrics(font, bbox, advance);
    }

    @Override
    public final Font getFont() {
        return this.metrics.getFont();
    }

    @Override
    public final char getSymbol() {
        return this.symbol;
    }

    public final Metrics getMetrics() {
        return this.metrics;
    }

    @Override
    public final int getID() {
        return this.id;
    }

    @Override
    public final float getScale(final int funits) {
        return this.metrics.getScale(funits);
    }

    @Override
    public final AABBox getBBox(final AABBox dest, final float pixelSize, final float[] tmpV3) {
        return dest.copy(metrics.getBBoxFU()).scale(pixelSize/metrics.getUnitsPerEM(), tmpV3);
    }

    @Override
    public final AABBox getBBoxFU(final AABBox dest) {
        return dest.copy(metrics.getBBoxFU());
    }

    @Override
    public final AABBox getBBoxFU() {
        return metrics.getBBoxFU();
    }

    @Override
    public final int getAdvanceFU() { return metrics.getAdvanceFU(); }

    @Override
    public float getAdvance() { return getScale( getAdvanceFU() ); }

    protected final void addAdvance(final float pixelSize, final float advance) {
        if( TypecastFont.USE_PRESCALED_ADVANCE ) {
            this.metrics.addAdvance(pixelSize, advance);
        }
    }

    @Override
    public final float getAdvance(final float pixelSize) {
        return metrics.getAdvance(pixelSize);
    }

    @Override
    public final OutlineShape getShape() {
        return this.shape;
    }

    @Override
    public final int hashCode() {
        // 31 * x == (x << 5) - x
        final int hash = 31 + getFont().getName(Font.NAME_UNIQUNAME).hashCode();
        return ((hash << 5) - hash) + id;
    }

    @Override
    public String toString() {
        final PostTable post = metrics.getFont().getPostTable();
        final String glyph_name = null != post ? post.getGlyphName(id) : "n/a";
        return new StringBuilder()
            .append("Glyph id ").append(id).append(" '").append(glyph_name).append("'")
            .append(", advance ").append(getAdvanceFU())
            .append(", ").append(getBBoxFU())
            .toString();
    }
}

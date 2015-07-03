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

public final class TypecastGlyph implements Font.Glyph {
    public static final class Advance
    {
        private final Font      font;
        private final float     advance;
        private final IntIntHashMap size2advanceI = new IntIntHashMap();

        public Advance(final Font font, final float advance)
        {
            this.font = font;
            this.advance = advance;
            size2advanceI.setKeyNotFoundValue(0);
        }

        public final void reset() {
            size2advanceI.clear();
        }

        public final Font getFont() { return font; }

        public final float getScale(final float pixelSize)
        {
            return this.font.getMetrics().getScale(pixelSize);
        }

        public final void add(final float advance, final float size)
        {
            size2advanceI.put(Float.floatToIntBits(size), Float.floatToIntBits(advance));
        }

        public final float get(final float pixelSize, final boolean useFrationalMetrics)
        {
            final int sI = Float.floatToIntBits(pixelSize);
            final int aI = size2advanceI.get(sI);
            if( 0 != aI ) {
                return Float.intBitsToFloat(aI);
            }
            final float a;
            if ( useFrationalMetrics ) {
                a = this.advance * getScale(pixelSize);
            } else {
                // a = Math.ceil(this.advance * getScale(pixelSize));
                a = Math.round(this.advance * getScale(pixelSize)); // TODO: check whether ceil should be used instead?
            }
            size2advanceI.put(sI, Float.floatToIntBits(a));
            return a;
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
        private final AABBox    bbox;
        private final Advance advance;

        public Metrics(final Font font, final AABBox bbox, final float advance)
        {
            this.bbox = bbox;
            this.advance = new Advance(font, advance);
        }

        public final void reset() {
            advance.reset();
        }

        public final Font getFont() { return advance.getFont(); }

        public final float getScale(final float pixelSize)
        {
            return this.advance.getScale(pixelSize);
        }

        public final AABBox getBBox()
        {
            return this.bbox;
        }

        public final void addAdvance(final float advance, final float size)
        {
            this.advance.add(advance, size);
        }

        public final float getAdvance(final float pixelSize, final boolean useFrationalMetrics)
        {
            return this.advance.get(pixelSize, useFrationalMetrics);
        }

        @Override
        public final String toString()
        {
            return "\nMetrics:"+
                "\n  bbox: "+this.bbox+
                this.advance;
        }
    }

    public static final short INVALID_ID    = (short)((1 << 16) - 1);
    public static final short MAX_ID        = (short)((1 << 16) - 2);

    private final char symbol;
    private final OutlineShape shape; // in EM units
    private final short id;
    private final Metrics metrics;

    protected TypecastGlyph(final Font font, final char symbol, final short id, final AABBox bbox, final int advance, final OutlineShape shape) {
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

    final AABBox getBBoxUnsized() {
        return this.metrics.getBBox();
    }

    @Override
    public final AABBox getBBox() {
        return this.metrics.getBBox();
    }

    public final Metrics getMetrics() {
        return this.metrics;
    }

    @Override
    public final short getID() {
        return this.id;
    }

    @Override
    public final float getScale(final float pixelSize) {
        return this.metrics.getScale(pixelSize);
    }

    @Override
    public final AABBox getBBox(final AABBox dest, final float pixelSize, final float[] tmpV3) {
        return dest.copy(getBBox()).scale(getScale(pixelSize), tmpV3);
    }

    protected final void addAdvance(final float advance, final float size) {
        this.metrics.addAdvance(advance, size);
    }

    @Override
    public final float getAdvance(final float pixelSize, final boolean useFrationalMetrics) {
        return this.metrics.getAdvance(pixelSize, useFrationalMetrics);
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
}

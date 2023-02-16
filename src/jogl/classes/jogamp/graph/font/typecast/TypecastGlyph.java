/**
 * Copyright 2011-2023 JogAmp Community. All rights reserved.
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

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.font.Font;
import com.jogamp.opengl.math.geom.AABBox;

import jogamp.graph.font.typecast.ot.table.KernSubtable;
import jogamp.graph.font.typecast.ot.table.KerningPair;
import jogamp.graph.font.typecast.ot.table.PostTable;

public final class TypecastGlyph implements Font.Glyph {

    public static final class Metrics
    {
        private final TypecastFont font;
        private final AABBox    bbox; // in font-units
        private final int      advance; // in font-units

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
        }

        public final TypecastFont getFont() { return font; }

        public final int getUnitsPerEM() { return font.getMetrics().getUnitsPerEM(); }

        public final float getScale(final int funits) { return font.getMetrics().getScale(funits); }

        /** in font-units */
        public final AABBox getBBoxFU() { return this.bbox; }

        /** Return advance in font units, sourced from `hmtx` table. */
        public final int getAdvanceFU() { return this.advance; }

        @Override
        public final String toString()
        {
            return "\nMetrics:"+
                "\n  bbox: "+this.bbox+
                "\n  advance: "+this.advance;
        }
    }

    public static final short INVALID_ID    = (short)((1 << 16) - 1);
    public static final short MAX_ID        = (short)((1 << 16) - 2);

    private static int[][] growPairArray(final int[][] src) {
        final int length = src.length;
        final int new_length = length * 2;
        final int[/*right_glyphid*/][/*value*/] dst = new int[new_length][2];
        for (int i = 0; i < length; i++) {
            dst[i][0] = src[i][0];
            dst[i][1] = src[i][1];
        }
        return dst;
    }

    private static int[][] trimPairArray(final int[][] src, final int new_length) {
        final int length = src.length;
        if( new_length >= length ) {
            return src;
        }
        final int[/*right_glyphid*/][/*value*/] dst = new int[new_length][2];
        for (int i = 0; i < new_length; i++) {
            dst[i][0] = src[i][0];
            dst[i][1] = src[i][1];
        }
        return dst;
    }

    private final int id;
    private final int[/*right_glyphid*/][/*value*/] kerning;
    private final boolean kerning_horizontal;
    private final boolean kerning_crossstream;
    private final OutlineShape shape; // in EM units
    private final Metrics metrics;

    /**
     *
     * @param font
     * @param id
     * @param bbox in font-units
     * @param advance from hmtx in font-units
     * @param shape
     */
    protected TypecastGlyph(final TypecastFont font, final int id, final AABBox bbox, final int advance,
                            final KernSubtable kernSub, final OutlineShape shape) {
        this.id = id;
        if( null != kernSub && kernSub.areKerningValues() ) {
            int pair_sz = 64;
            int pair_idx = 0;
            int[/*right_glyphid*/][/*value*/] pairs = new int[pair_sz][2];
            for (int i = 0; i < kernSub.getKerningPairCount(); i++) {
                final KerningPair kpair = kernSub.getKerningPair(i);
                if( kpair.getLeft() == id ) {
                    if( pair_idx == pair_sz ) {
                        pairs = growPairArray(pairs);
                        pair_sz = pairs.length;
                    }
                    pairs[pair_idx][0] = kpair.getRight();
                    pairs[pair_idx][1] = kpair.getValue();
                    ++pair_idx;
                } else if( kpair.getLeft() > id ) {
                    break; // early out
                }
            }
            this.kerning = trimPairArray(pairs, pair_idx);
            this.kerning_horizontal = kernSub.isHorizontal();
            this.kerning_crossstream = kernSub.isCrossstream();
        } else {
            this.kerning = new int[0][0];
            this.kerning_horizontal = true;
            this.kerning_crossstream = true;
        }
        this.shape = shape;
        this.metrics = new Metrics(font, bbox, advance);
    }

    @Override
    public final Font getFont() {
        return this.metrics.getFont();
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
    public final AABBox getBBoxFU() {
        return metrics.getBBoxFU();
    }

    @Override
    public final AABBox getBBoxFU(final AABBox dest) {
        return dest.copy(metrics.getBBoxFU());
    }

    @Override
    public final AABBox getBBox(final AABBox dest, final float[] tmpV3) {
        return dest.copy(metrics.getBBoxFU()).scale2(1.0f/metrics.getUnitsPerEM(), tmpV3);
    }

    @Override
    public final AABBox getBBox() {
        final AABBox dest = new AABBox();
        return dest.copy(metrics.getBBoxFU()).scale2(1.0f/metrics.getUnitsPerEM(), new float[2]);
    }

    @Override
    public final int getAdvanceFU() { return metrics.getAdvanceFU(); }

    @Override
    public float getAdvance() { return getScale( getAdvanceFU() ); }

    @Override
    public final boolean isKerningHorizontal() { return kerning_horizontal; }

    @Override
    public final boolean isKerningCrossstream() { return kerning_crossstream; }

    @Override
    public final int getKerningPairCount() { return kerning.length; }

    @Override
    public final int getKerningFU(final int right_glyphid) {
        // binary search in ordered kerning table
        int l = 0;
        int h = kerning.length-1;
        while( l <= h ) {
            final int i = ( l + h ) / 2;
            final int k_right = kerning[i][0];
            if ( k_right < right_glyphid ) {
                l = i + 1;
            } else if ( k_right > right_glyphid ) {
                h = i - 1;
            } else {
                return kerning[i][1];
            }
        }
        return 0;
    }

    @Override
    public final float getKerning(final int right_glyphid) {
        return getScale( getKerningFU(right_glyphid) );
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
        final StringBuilder sb = new StringBuilder();
        sb.append("Glyph id ").append(id).append(" '").append(glyph_name).append("'")
          .append(", advance ").append(getAdvanceFU())
          .append(", kerning[size ").append(kerning.length).append(", horiz ").append(this.isKerningHorizontal()).append(", cross ").append(this.isKerningCrossstream()).append("]");
        return sb.toString();
    }

    @Override
    public String fullString() {
        final PostTable post = metrics.getFont().getPostTable();
        final String glyph_name = null != post ? post.getGlyphName(id) : "n/a";
        final StringBuilder sb = new StringBuilder();
        sb.append("Glyph id ").append(id).append(" '").append(glyph_name).append("'")
          .append(", advance ").append(getAdvanceFU())
          .append(", ").append(getBBoxFU());

        sb.append("\n    Kerning: size ").append(kerning.length).append(", horiz ").append(this.isKerningHorizontal()).append(", cross ").append(this.isKerningCrossstream());
        final int left = getID();
        for (int i = 0; i < kerning.length; i++) {
            final int right = kerning[i][0];
            final int value = kerning[i][1];
            final String leftS;
            final String rightS;
            if( null == post ) {
                leftS = String.valueOf(left);
                rightS = String.valueOf(left);
            } else {
                leftS = post.getGlyphName(left)+"/"+String.valueOf(left);
                rightS = post.getGlyphName(right)+"/"+String.valueOf(right);
            }
            sb.append("\n      kp[").append(i).append("]: ").append(leftS).append(" -> ").append(rightS).append(" = ").append(value);
        }
        return sb.toString();
    }
}

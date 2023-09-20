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
import com.jogamp.math.geom.AABBox;

import jogamp.graph.font.typecast.ot.table.KernSubtable;
import jogamp.graph.font.typecast.ot.table.KerningPair;
import jogamp.graph.font.typecast.ot.table.PostTable;

public final class TypecastGlyph implements Font.Glyph {

    public static final short INVALID_ID    = (short)((1 << 16) - 1);
    public static final short MAX_ID        = (short)((1 << 16) - 2);
    private static final String dot_undef_NAME = ".notdef";
    private static final String NULL_NAME = "NULL";
    private static final String null_NAME = "null";
    private static final String dot_null_NAME = ".null";

    /* pp */ static final boolean isUndefName(final String name) {
        if( null != name ) {
            if( TypecastGlyph.dot_undef_NAME.equals(name) ) {
                return true;
            } else if( TypecastGlyph.NULL_NAME.equals(name) ) {
                return true;
            } else if( TypecastGlyph.null_NAME.equals(name) ) {
                return true;
            } else if( TypecastGlyph.dot_null_NAME.equals(name) ) {
                return true;
            }
        }
        return false;
    }

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
    private final String name;
    private final boolean isUndefined;
    private final boolean isWhitespace;

    private final TypecastFont font;
    private final AABBox bbox; // in font-units
    private final int advance; // in font-units
    private final int leftSideBearings; // in font-units

    private final int[/*right_glyphid*/][/*value*/] kerning;
    private final boolean kerning_horizontal;
    private final boolean kerning_crossstream;
    private final OutlineShape shape; // in EM units

    /**
     *
     * @param font
     * @param name from `post` table
     * @param id
     * @param bbox in font-units
     * @param advance from hmtx in font-units
     * @param leftSideBearings from hmtx in font-units
     * @param shape
     */
    protected TypecastGlyph(final TypecastFont font, final int id, final String name,
                            final AABBox bbox, final int advance, final int leftSideBearings,
                            final KernSubtable kernSub, final OutlineShape shape,
                            final boolean isUndefined, final boolean isWhiteSpace) {
        this.id = id;
        this.name = name;
        this.isUndefined = isUndefined;
        this.isWhitespace = isWhiteSpace;
        this.font = font;
        this.bbox = bbox;
        this.advance = advance;
        this.leftSideBearings = leftSideBearings;
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
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public final int getID() { return id; }

    @Override
    public final String getName() { return name; }

    @Override
    public final boolean isWhitespace() { return this.isWhitespace; }

    @Override
    public final boolean isUndefined() { return this.isUndefined; }

    @Override
    public final boolean isNonContour() { return isUndefined() || isWhitespace(); }

    @Override
    public final AABBox getBoundsFU() { return bbox; }

    @Override
    public final AABBox getBoundsFU(final AABBox dest) { return dest.copy(bbox); }

    @Override
    public final AABBox getBounds(final AABBox dest) {
        return dest.copy(bbox).scale2(1.0f/font.getMetrics().getUnitsPerEM());
    }

    @Override
    public final AABBox getBounds() {
        final AABBox dest = new AABBox(bbox);
        return dest.scale2(1.0f/font.getMetrics().getUnitsPerEM());
    }

    @Override
    public final int getAdvanceFU() { return advance; }

    @Override
    public float getAdvance() { return font.getMetrics().getScale( advance ); }

    @Override
    public final int getLeftSideBearingsFU() { return leftSideBearings; }

    @Override
    public final float getLeftSideBearings() { return font.getMetrics().getScale( leftSideBearings ); }

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
        return font.getMetrics().getScale( getKerningFU(right_glyphid) );
    }

    @Override
    public final OutlineShape getShape() {
        return this.shape;
    }

    @Override
    public final int hashCode() {
        // 31 * x == (x << 5) - x
        final int hash = 31 + font.getName(Font.NAME_UNIQUNAME).hashCode();
        return ((hash << 5) - hash) + id;
    }

    @Override
    public final boolean equals(final Object o) {
        if( this == o ) { return true; }
        if( o instanceof TypecastGlyph ) {
            final TypecastGlyph og = (TypecastGlyph)o;
            return og.font.getName(Font.NAME_UNIQUNAME).equals(font.getName(Font.NAME_UNIQUNAME)) &&
                   og.id == id;
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final String contour_s;
        if( isNonContour() ) {
            final String ws_s = isWhitespace() ? "whitespace" : "";
            final String undef_s = isUndefined() ? "undefined" : "";
            contour_s = "non-cont("+ws_s+undef_s+")";
        } else {
            contour_s = "contour";
        }
        final String name_s = null != name ? name : "";
        final String shape_s = null != shape ? "shape "+shape.getVertexCount()+"v" : "shape null";
        sb.append("Glyph[id ").append(id).append(" '").append(name_s).append("', ").append(contour_s)
          .append(", ").append(shape_s)
          .append(", advance ").append(getAdvanceFU())
          .append(", leftSideBearings ").append(getLeftSideBearingsFU())
          .append(", kerning[size ").append(kerning.length).append(", horiz ").append(this.isKerningHorizontal()).append(", cross ").append(this.isKerningCrossstream()).append("]")
          .append("]");
        return sb.toString();
    }

    @Override
    public String fullString() {
        final PostTable post = font.getPostTable();
        final StringBuilder sb = new StringBuilder();
        final String contour_s;
        if( isNonContour() ) {
            final String ws_s = isWhitespace() ? "whitespace" : "";
            final String undef_s = isUndefined() ? "undefined" : "";
            contour_s = "non-cont("+ws_s+undef_s+")";
        } else {
            contour_s = "contour";
        }
        final String name_s = null != name ? name : "";
        final String shape_s = null != shape ? "shape "+shape.getVertexCount()+"v" : "shape null";
        sb.append("Glyph id ").append(id).append(" '").append(name_s).append("', ").append(contour_s)
          .append(", shape ").append(shape_s)
          .append(", advance ").append(getAdvanceFU())
          .append(", leftSideBearings ").append(getLeftSideBearingsFU())
          .append(", ").append(getBoundsFU());

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

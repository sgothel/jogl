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

import jogamp.graph.font.typecast.ot.OTFontCollection;
import jogamp.graph.font.typecast.ot.TTFont;
import jogamp.graph.font.typecast.ot.table.CmapFormat;
import jogamp.graph.font.typecast.ot.table.CmapTable;
import jogamp.graph.font.typecast.ot.table.GlyfDescript;
import jogamp.graph.font.typecast.ot.table.GlyfTable;
import jogamp.graph.font.typecast.ot.table.HheaTable;
import jogamp.graph.font.typecast.ot.table.HmtxTable;
import jogamp.graph.font.typecast.ot.table.ID;
import jogamp.graph.font.typecast.ot.table.KernSubtable;
import jogamp.graph.font.typecast.ot.table.KernSubtableFormat0;
import jogamp.graph.font.typecast.ot.table.KernTable;
import jogamp.graph.font.typecast.ot.table.KerningPair;
import jogamp.graph.font.typecast.ot.table.PostTable;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import com.jogamp.common.os.Clock;
import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.common.util.PerfCounterCtrl;
import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.opengl.math.geom.AABBox;

class TypecastFont implements Font {
    static final boolean DEBUG = false;
    private static final Vertex.Factory<SVertex> vertexFactory = SVertex.factory();

    // private final OTFontCollection fontset;
    /* pp */ final TTFont font;
    private final CmapFormat cmapFormat;
    private final int cmapentries;
    private final IntObjectHashMap idToGlyph;
    private final TypecastHMetrics metrics;
    // FIXME: Add cache size to limit memory usage ??

    private static final boolean forceAscii = false; // FIXME ??? (ASCII/Macintosh cmap format)

    public TypecastFont(final OTFontCollection fontset) {
        // this.fontset = fontset;
        this.font = fontset.getFont(0);

        final CmapTable cmapTable = font.getCmapTable();
        int platform = -1;
        int encoding = -1;

        // Decide upon a cmap table to use for our character to glyph look-up
        CmapFormat cmapFmt;
        platform = ID.platformMicrosoft;
        if (forceAscii) {
            // We've been asked to use the ASCII/Macintosh cmap format
            encoding = ID.encodingRoman;
            cmapFmt = cmapTable.getCmapFormat(ID.platformMacintosh, ID.encodingRoman);
        } else {
            // The default behaviour is to use the Unicode cmap encoding
            encoding = ID.encodingUnicode;
            cmapFmt = cmapTable.getCmapFormat(ID.platformMicrosoft, ID.encodingUnicode);
            if (cmapFmt == null) {
                // This might be a symbol font
                encoding = ID.encodingSymbol;
                cmapFmt = cmapTable.getCmapFormat(ID.platformMicrosoft, ID.encodingSymbol);
            }
        }
        if (cmapFmt == null) {
            throw new RuntimeException("Cannot find a suitable cmap table");
        }
        cmapFormat = cmapFmt;
        if(DEBUG) {
            System.err.println("Selected CmapFormat: platform " + platform + ", encoding "+encoding + ": "+cmapFormat);
        }

        {
            int _cmapentries = 0;
            for (int i = 0; i < cmapFormat.getRangeCount(); ++i) {
                final CmapFormat.Range range = cmapFormat.getRange(i);
                _cmapentries += range.getEndCode() - range.getStartCode() + 1; // end included
            }
            cmapentries = _cmapentries;
        }
        idToGlyph = new IntObjectHashMap(cmapentries + cmapentries/4);
        metrics = new TypecastHMetrics(this);

        if(DEBUG) {
            final int max_id = 36; // "A"
            System.err.println("font direction hint: "+font.getHeadTable().getFontDirectionHint());
            System.err.println("num glyphs: "+font.getNumGlyphs());
            System.err.println("num cmap entries: "+cmapentries);
            System.err.println("num cmap ranges: "+cmapFormat.getRangeCount());

            for (int i = 0; i < cmapFormat.getRangeCount(); ++i) {
                final CmapFormat.Range range = cmapFormat.getRange(i);
                for (int j = range.getStartCode(); j <= range.getEndCode(); ++j) {
                    final int code = cmapFormat.mapCharCode(j);
                    if(code <= max_id) {
                        System.err.println(" char: " + j + " ( " + (char)j +" ) -> " + code);
                    }
                }
            }
            final HmtxTable hmtx = font.getHmtxTable();
            final HheaTable hhea = font.getHheaTable();
            final GlyfTable glyfTable = font.getGlyfTable();
            for(int i=0; i <= max_id; ++i) {
                final jogamp.graph.font.typecast.ot.Glyph tc_g = font.getGlyph(i);
                final Glyph g = getGlyph(i);
                final GlyfDescript gd = glyfTable.getDescription(i);
                System.err.println("Index "+i);
                System.err.println("  hmtx aw "+hmtx.getAdvanceWidth(i)+", lsb "+hmtx.getLeftSideBearing(i));
                System.err.println("  hhea aw-max "+hhea.getAdvanceWidthMax()+", x-max "+hhea.getXMaxExtent());
                if( null != gd ) {
                    System.err.println("  gdesc idx "+gd.getGlyphIndex()+", isComp "+gd.isComposite()+", contours "+gd.getContourCount()+", points "+gd.getPointCount());
                } else {
                    System.err.println("  gdesc null");
                }
                if( null != tc_g) {
                    System.err.println("  tc_glyph "+tc_g);
                } else {
                    System.err.println("  tc_glyph null");
                }
                System.err.println("  glyph "+g);
            }
            System.err.println( fullString() );
        }
    }

    @Override
    public String getName(final int nameIndex) {
        return font.getName(nameIndex);
    }
    @Override
    public StringBuilder getAllNames(final StringBuilder sb, final String separator) {
        return font.getAllNames(sb, separator);
    }
    @Override
    public String getFullFamilyName() {
        return getName(Font.NAME_FAMILY) + "-" + getName(Font.NAME_SUBFAMILY);
    }

    @Override
    public float getAdvanceWidth(final int glyphID) {
        return metrics.getScale( font.getHmtxTable().getAdvanceWidth(glyphID) );
    }
    @Override
    public int getAdvanceWidthFU(final int glyphID) {
        return font.getHmtxTable().getAdvanceWidth(glyphID);
    }

    @Override
    public final Metrics getMetrics() {
        return metrics;
    }

    @Override
    public int getGlyphID(final char symbol) {
        final int glyphID = cmapFormat.mapCharCode(symbol);
        if( 0 < glyphID ) {
            return glyphID;
        }
        return Glyph.ID_UNKNOWN;
    }

    /** pp **/ PostTable getPostTable() {
        return font.getPostTable();
    }

    @Override
    public Glyph getGlyph(final int glyph_id) {
        TypecastGlyph result = (TypecastGlyph) idToGlyph.get(glyph_id);
        if (null == result) {
            final jogamp.graph.font.typecast.ot.Glyph glyph = font.getGlyph(glyph_id);
            final PostTable post = font.getPostTable();
            final String glyph_name = null != post ? post.getGlyphName(glyph_id) : "";

            final int glyph_advance;
            final int glyph_leftsidebearings;
            final AABBox glyph_bbox;
            final OutlineShape shape;
            if( null != glyph ) {
                glyph_advance = glyph.getAdvanceWidth();
                glyph_leftsidebearings = glyph.getLeftSideBearing();
                glyph_bbox = glyph.getBBox();
                shape = TypecastRenderer.buildShape(metrics.getUnitsPerEM(), glyph, vertexFactory);
            } else {
                final int glyph_height = metrics.getAscentFU() - metrics.getDescentFU();
                glyph_advance = getAdvanceWidthFU(glyph_id);
                glyph_leftsidebearings = 0;
                glyph_bbox = new AABBox(0f,0f,0f, glyph_advance, glyph_height, 0f);
                shape = null;
            }
            KernSubtable kernSub = null;
            {
                final KernTable kern = font.getKernTable();
                if (kern != null ) {
                    kernSub = kern.getSubtable0();
                }
            }
            result = new TypecastGlyph(this, glyph_id, glyph_name, glyph_bbox, glyph_advance, glyph_leftsidebearings, kernSub, shape);
            if(DEBUG) {
                System.err.println("New glyph: " + glyph_id + "/'"+glyph_name+"', shape " + (null != shape));
                System.err.println("  tc_glyph "+glyph);
                System.err.println("     glyph "+result);
            }
            if( null != glyph ) {
                glyph.clearPointData();
            }

            idToGlyph.put(glyph_id, result);
        }
        return result;
    }

    @Override
    public float getLineHeight() {
        return metrics.getScale( getLineHeightFU() );
    }

    @Override
    public int getLineHeightFU() {
        final Metrics metrics = getMetrics();
        return metrics.getAscentFU() - metrics.getDescentFU() + metrics.getLineGapFU();
    }

    @Override
    public AABBox getMetricBounds(final CharSequence string) {
        return getMetricBoundsFU(string).scale2(1.0f/metrics.getUnitsPerEM(), new float[3]);
    }

    @Override
    public AABBox getMetricBoundsFU(final CharSequence string) {
        if (null == string || 0 == string.length() ) {
            return new AABBox();
        }
        final AABBox res = new AABBox();
        final int charCount = string.length();

        final int lineHeight = getLineHeightFU();

        int y = 0;
        int advanceTotal = 0;

        for (int i=0; i<charCount; i++) {
            final char character = string.charAt(i);
            if (character == '\n') {
                advanceTotal = 0;
                y -= lineHeight;
            } else {
                advanceTotal += getAdvanceWidthFU( getGlyphID( character ) );
            }
            res.resize(advanceTotal, y, 0f);
        }
        if( 0 < advanceTotal ) {
            // add one line for current non '\n' terminated
            y -= lineHeight;
            res.resize(advanceTotal, y, 0f);
        }
        return res;
    }

    @Override
    public AABBox getGlyphBounds(final CharSequence string) {
        return getGlyphBounds(string, new AffineTransform(), new AffineTransform());
    }
    @Override
    public AABBox getGlyphBounds(final CharSequence string, final AffineTransform tmp1, final AffineTransform tmp2) {
        return getGlyphBoundsFU(string, tmp1, tmp2).scale2(1.0f/metrics.getUnitsPerEM(), new float[3]);
    }

    @Override
    public AABBox getGlyphBoundsFU(final CharSequence string) {
        return getGlyphBoundsFU(string, new AffineTransform(), new AffineTransform());
    }
    @Override
    public AABBox getGlyphBoundsFU(final CharSequence string, final AffineTransform temp1, final AffineTransform temp2) {
        if (null == string || 0 == string.length() ) {
            return new AABBox();
        }
        final AABBox res = new AABBox();
        final int charCount = string.length();

        final int lineHeight = getLineHeightFU();

        int y = 0;
        int advanceTotal = 0;
        Font.Glyph left_glyph = null;
        final AABBox temp_box = new AABBox();

        for(int i=0; i< charCount; i++) {
            final char character = string.charAt(i);
            if( '\n' == character ) {
                y -= lineHeight;
                advanceTotal = 0;
                left_glyph = null;
            } else {
                // reset transform
                temp1.setToIdentity();
                final int glyph_id = getGlyphID(character);
                final Font.Glyph glyph = getGlyph(glyph_id);
                final OutlineShape glyphShape = glyph.getShape();
                if( null == glyphShape ) { // also covers 'space' and all non-contour symbols
                    advanceTotal += glyph.getAdvanceFU();
                    left_glyph = null; // break kerning
                    continue;
                }
                if( null != left_glyph ) {
                    advanceTotal += left_glyph.getKerningFU(glyph_id);
                }
                temp1.translate(advanceTotal, y, temp2);
                res.resize(temp1.transform(glyph.getBBoxFU(), temp_box));

                advanceTotal += glyph.getAdvanceFU();
                left_glyph = glyph;
            }
        }
        return res;
    }

    @Override
    public AABBox getGlyphShapeBounds(final CharSequence string) {
        return getGlyphShapeBounds(null, string);
    }
    @Override
    public AABBox getGlyphShapeBounds(final AffineTransform transform, final CharSequence string) {
        if (null == string || 0 == string.length() ) {
            return new AABBox();
        }
        final OutlineShape.Visitor visitor = new OutlineShape.Visitor() {
            @Override
            public final void visit(final OutlineShape shape, final AffineTransform t) {
                // nop
            } };
        return processString(visitor, transform, string);
    }

    @Override
    public AABBox processString(final OutlineShape.Visitor visitor, final AffineTransform transform,
                                final CharSequence string) {
        return processString(visitor, transform, string, new AffineTransform(), new AffineTransform());
    }

    @Override
    public AABBox processString(final OutlineShape.Visitor visitor, final AffineTransform transform,
                                final CharSequence string,
                                final AffineTransform temp1, final AffineTransform temp2) {
        if (null == string || 0 == string.length() ) {
            return new AABBox();
        }
        final AABBox res = new AABBox();
        final int charCount = string.length();

        // region.setFlipped(true);
        final float lineHeight = getLineHeight();

        float y = 0;
        float advanceTotal = 0;
        Font.Glyph left_glyph = null;
        final AABBox temp_box = new AABBox();

        for(int i=0; i< charCount; i++) {
            final char character = string.charAt(i);
            if( '\n' == character ) {
                y -= lineHeight;
                advanceTotal = 0;
                left_glyph = null;
            } else {
                // reset transform
                if( null != transform ) {
                    temp1.setTransform(transform);
                } else {
                    temp1.setToIdentity();
                }
                final int glyph_id = getGlyphID(character);

                final Font.Glyph glyph = getGlyph(glyph_id);
                final OutlineShape glyphShape = glyph.getShape();

                if( null == glyphShape ) { // also covers 'space' and all non-contour symbols
                    advanceTotal += glyph.getAdvance();
                    left_glyph = null; // break kerning
                    continue;
                }
                if( null != left_glyph ) {
                    advanceTotal += left_glyph.getKerning(glyph_id);
                }
                temp1.translate(advanceTotal, y, temp2);
                res.resize(temp1.transform(glyphShape.getBounds(), temp_box));
                visitor.visit(glyphShape, temp1);
                advanceTotal += glyph.getAdvance();
                left_glyph = glyph;
            }
        }
        return res;
    }

    @Override
    final public int getNumGlyphs() {
        return font.getNumGlyphs();
    }

    @Override
    public boolean isPrintableChar( final char c ) {
        return FontFactory.isPrintableChar(c);
    }

    @Override
    public final int hashCode() {
        return font.getName(Font.NAME_UNIQUNAME).hashCode();
    }

    @Override
    public final boolean equals(final Object o) {
        if( this == o ) { return true; }
        if( o instanceof TypecastFont ) {
            final TypecastFont of = (TypecastFont)o;
            return of.font.getName(Font.NAME_UNIQUNAME).equals(font.getName(Font.NAME_UNIQUNAME));
        }
        return false;
    }

    @Override
    public String toString() {
        return getFullFamilyName();
    }

    @SuppressWarnings("unused")
    @Override
    public String fullString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(toString()).append("[ ").append(font.toString());
        sb.append("\n").append(font.getHeadTable());
        sb.append("\n\n").append(font.getHheaTable());
        if( null != font.getVheaTable() ) {
            sb.append("\n\n").append(font.getVheaTable());
        }
        if( false && null != font.getKernTable() ) { // too long
            final PostTable post = font.getPostTable();
            final KernTable kern = font.getKernTable();
            sb.append("\n\n").append(kern);
            final KernSubtableFormat0 ks0 = kern.getSubtable0();
            if( null != ks0 ) {
                final int sz = ks0.getKerningPairCount();
                for(int i=0; i<sz; ++i) {
                    final KerningPair kp = ks0.getKerningPair(i);
                    final int left = kp.getLeft();
                    final int right = kp.getRight();
                    final String leftS;
                    final String rightS;
                    if( null == post ) {
                        leftS = String.valueOf(left);
                        rightS = String.valueOf(left);
                    } else {
                        leftS = post.getGlyphName(left)+"/"+String.valueOf(left);
                        rightS = post.getGlyphName(right)+"/"+String.valueOf(right);
                    }
                    sb.append("\n      kp[").append(i).append("]: ").append(leftS).append(" -> ").append(rightS).append(" = ").append(kp.getValue());
                }
            }
        }

        sb.append("\n\n").append(font.getCmapTable());
        /* if( null != font.getHdmxTable() ) {
            sb.append("\n").append(font.getHdmxTable()); // too too long
        } */
        // glyf
        // sb.append("\n").append(font.getHmtxTable()); // too long
        /* if( null != font.getLocaTable() ) {
            sb.append("\n").append(font.getLocaTable()); // too long
        } */
        sb.append("\n").append(font.getMaxpTable());
        // sb.append("\n\n").append(font.getNameTable()); // no toString()
        sb.append("\n\n").append(font.getOS2Table());
        // sb.append("\n\n").append(font.getPostTable()); // too long
        sb.append("\n]");
        return sb.toString();
    }
}

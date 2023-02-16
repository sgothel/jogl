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
import jogamp.graph.font.typecast.ot.table.CmapIndexEntry;
import jogamp.graph.font.typecast.ot.table.CmapTable;
import jogamp.graph.font.typecast.ot.table.ID;
import jogamp.graph.font.typecast.ot.table.KernSubtable;
import jogamp.graph.font.typecast.ot.table.KernSubtableFormat0;
import jogamp.graph.font.typecast.ot.table.KernTable;
import jogamp.graph.font.typecast.ot.table.KerningPair;
import jogamp.graph.font.typecast.ot.table.PostTable;

import com.jogamp.common.util.IntObjectHashMap;
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

    public TypecastFont(final OTFontCollection fontset) {
        // this.fontset = fontset;
        this.font = fontset.getFont(0);

        // FIXME: Generic attempt to find the best CmapTable,
        // which is assumed to be the one with the most entries (stupid 'eh?)
        final CmapTable cmapTable = font.getCmapTable();
        final CmapFormat[] _cmapFormatP = { null, null, null, null };
        int platform = -1;
        int platformLength = -1;
        int encoding = -1;
        for(int i=0; i<cmapTable.getNumTables(); i++) {
            final CmapIndexEntry cmapIdxEntry = cmapTable.getCmapIndexEntry(i);
            final int pidx = cmapIdxEntry.getPlatformId();
            final CmapFormat cf = cmapIdxEntry.getFormat();
            if(DEBUG) {
                System.err.println("CmapFormat["+i+"]: platform " + pidx +
                                   ", encoding "+cmapIdxEntry.getEncodingId() + ": "+cf);
            }
            if( _cmapFormatP[pidx] == null ||
                _cmapFormatP[pidx].getLength() < cf.getLength() ) {
                _cmapFormatP[pidx] = cf;
                if( cf.getLength() > platformLength ) {
                    platformLength = cf.getLength() ;
                    platform = pidx;
                    encoding = cmapIdxEntry.getEncodingId();
                }
            }
        }
        if(0 <= platform) {
            cmapFormat = _cmapFormatP[platform];
            if(DEBUG) {
                System.err.println("Selected CmapFormat: platform " + platform +
                                   ", encoding "+encoding + ": "+cmapFormat);
            }
        } else {
            CmapFormat _cmapFormat = null;
            /*if(null == _cmapFormat) {
                platform = ID.platformMacintosh;
                encoding = ID.encodingASCII;
                _cmapFormat = cmapTable.getCmapFormat(platform, encoding);
            } */
            if(null == _cmapFormat) {
                // default unicode
                platform = ID.platformMicrosoft;
                encoding = ID.encodingUnicode;
                _cmapFormat = cmapTable.getCmapFormat((short)platform, (short)encoding);
            }
            if(null == _cmapFormat) {
                // maybe a symbol font ?
                platform = ID.platformMicrosoft;
                encoding = ID.encodingSymbol;
                _cmapFormat = cmapTable.getCmapFormat((short)platform, (short)encoding);
            }
            if(null == _cmapFormat) {
                throw new RuntimeException("Cannot find a suitable cmap table for font "+font);
            }
            cmapFormat = _cmapFormat;
            if(DEBUG) {
                System.err.println("Selected CmapFormat (2): platform " + platform + ", encoding "+encoding + ": "+cmapFormat);
            }
        }

        {
            int _cmapentries = 0;
            for (int i = 0; i < cmapFormat.getRangeCount(); ++i) {
                final CmapFormat.Range range = cmapFormat.getRange(i);
                _cmapentries += range.getEndCode() - range.getStartCode() + 1; // end included
            }
            cmapentries = _cmapentries;
        }
        if(DEBUG) {
            System.err.println("font direction hint: "+font.getHeadTable().getFontDirectionHint());
            System.err.println("num glyphs: "+font.getNumGlyphs());
            System.err.println("num cmap entries: "+cmapentries);
            System.err.println("num cmap ranges: "+cmapFormat.getRangeCount());

            for (int i = 0; i < cmapFormat.getRangeCount(); ++i) {
                final CmapFormat.Range range = cmapFormat.getRange(i);
                for (int j = range.getStartCode(); j <= range.getEndCode(); ++j) {
                    final int code = cmapFormat.mapCharCode(j);
                    if(code < 15) {
                        System.err.println(" char: " + j + " ( " + (char)j +" ) -> " + code);
                    }
                }
            }
        }
        idToGlyph = new IntObjectHashMap(cmapentries + cmapentries/4);
        metrics = new TypecastHMetrics(this);
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
        // enforce mapping as some fonts have an erroneous cmap (FreeSerif-Regular)
        switch(symbol) {
            case ' ':  return Glyph.ID_SPACE;
            case '\n': return Glyph.ID_CR;
        }
        final int glyphID = cmapFormat.mapCharCode(symbol);
        if( 0 != glyphID ) {
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
            jogamp.graph.font.typecast.ot.Glyph glyph = font.getGlyph(glyph_id);
            final int glyph_advance;
            final AABBox glyph_bbox;
            if(null == glyph) {
                // fallback
                glyph = font.getGlyph(Glyph.ID_UNKNOWN);
            }
            switch( glyph_id ) {
                case Glyph.ID_SPACE:
                    /** fallthrough */
                case Glyph.ID_CR:
                    glyph_advance = getAdvanceWidthFU(glyph_id);
                    glyph_bbox = new AABBox(0, 0, 0, glyph_advance, getLineHeightFU(), 0);
                    break;
                default:
                    glyph_advance = glyph.getAdvanceWidth();
                    glyph_bbox = glyph.getBBox();
                    break;
            }
            if(null == glyph) {
                throw new RuntimeException("Could not retrieve glyph for glyph id "+glyph_id);
            }
            final OutlineShape shape = TypecastRenderer.buildShape(metrics.getUnitsPerEM(), glyph, vertexFactory);
            KernSubtable kernSub = null;
            {
                final KernTable kern = font.getKernTable();
                if (kern != null ) {
                    kernSub = kern.getSubtable0();
                }
            }
            result = new TypecastGlyph(this, glyph_id, glyph_bbox, glyph_advance, kernSub, shape);
            if(DEBUG) {
                final PostTable post = font.getPostTable();
                final String glyph_name = null != post ? post.getGlyphName(glyph_id) : "n/a";
                System.err.println("New glyph: " + glyph_id + "/'"+glyph_name+"', contours " + glyph.getPointCount() + ": " + shape);
                System.err.println("  "+glyph);
                System.err.println("  "+result);
            }
            glyph.clearPointData();

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
        final int lineGap = metrics.getLineGapFU() ; // negative value!
        final int descent = metrics.getDescentFU() ; // positive value!
        final int ascent = metrics.getAscentFU() ;   // negative value!
        final int advanceY = lineGap - descent + ascent;  // negative value!
        return -advanceY;
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
            } else if (character == ' ') {
                advanceTotal += getAdvanceWidthFU(Glyph.ID_SPACE);
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
        return getGlyphBoundsFU(string).scale2(1.0f/metrics.getUnitsPerEM(), new float[3]);
    }

    @Override
    public AABBox getGlyphBoundsFU(final CharSequence string) {
        if (null == string || 0 == string.length() ) {
            return new AABBox();
        }
        final AffineTransform temp1 = new AffineTransform();
        final AffineTransform temp2 = new AffineTransform();

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
            } else if (character == ' ') {
                advanceTotal += getAdvanceWidthFU(Glyph.ID_SPACE);
                left_glyph = null;
            } else {
                // reset transform
                temp1.setToIdentity();
                final int glyph_id = getGlyphID(character);
                final Font.Glyph glyph = getGlyph(glyph_id);
                final OutlineShape glyphShape = glyph.getShape();
                if( null == glyphShape ) {
                    left_glyph = null;
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
            } else if (character == ' ') {
                advanceTotal += getAdvanceWidth(Glyph.ID_SPACE);
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
                if( null == glyphShape ) {
                    left_glyph = null;
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

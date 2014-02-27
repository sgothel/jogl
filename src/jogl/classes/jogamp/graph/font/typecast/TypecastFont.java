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

import jogamp.graph.font.typecast.ot.OTFont;
import jogamp.graph.font.typecast.ot.OTFontCollection;
import jogamp.graph.font.typecast.ot.table.CmapFormat;
import jogamp.graph.font.typecast.ot.table.CmapIndexEntry;
import jogamp.graph.font.typecast.ot.table.CmapTable;
import jogamp.graph.font.typecast.ot.table.HdmxTable;
import jogamp.graph.font.typecast.ot.table.ID;

import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.math.geom.AABBox;

class TypecastFont implements Font {
    static final boolean DEBUG = false;
    private static final Vertex.Factory<SVertex> vertexFactory = SVertex.factory();

    // private final OTFontCollection fontset;
    /* pp */ final OTFont font;
    private final CmapFormat cmapFormat;
    private final int cmapentries;
    private final IntObjectHashMap char2Glyph;
    private final TypecastHMetrics metrics;
    // FIXME: Add cache size to limit memory usage ??

    public TypecastFont(final OTFontCollection fontset) {
        // this.fontset = fontset;
        this.font = fontset.getFont(0);

        // FIXME: Generic attempt to find the best CmapTable,
        // which is assumed to be the one with the most entries (stupid 'eh?)
        CmapTable cmapTable = font.getCmapTable();
        CmapFormat[] _cmapFormatP = { null, null, null, null };
        int platform = -1;
        int platformLength = -1;
        int encoding = -1;
        for(int i=0; i<cmapTable.getNumTables(); i++) {
            CmapIndexEntry cmapIdxEntry = cmapTable.getCmapIndexEntry(i);
            int pidx = cmapIdxEntry.getPlatformId();
            CmapFormat cf = cmapIdxEntry.getFormat();
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
                CmapFormat.Range range = cmapFormat.getRange(i);
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
                CmapFormat.Range range = cmapFormat.getRange(i);
                for (int j = range.getStartCode(); j <= range.getEndCode(); ++j) {
                    final int code = cmapFormat.mapCharCode(j);
                    if(code < 15) {
                        System.err.println(" char: " + j + " ( " + (char)j +" ) -> " + code);
                    }
                }
            }
        }
        char2Glyph = new IntObjectHashMap(cmapentries + cmapentries/4);
        metrics = new TypecastHMetrics(this);
    }

    @Override
    public StringBuilder getName(StringBuilder sb, int nameIndex) {
        return font.getName(nameIndex, sb);
    }
    @Override
    public String getName(int nameIndex) {
        return getName(null, nameIndex).toString();
    }
    @Override
    public StringBuilder getAllNames(StringBuilder sb, String separator) {
        return font.getAllNames(sb, separator);
    }
    @Override
    public StringBuilder getFullFamilyName(StringBuilder sb) {
        sb = getName(sb, Font.NAME_FAMILY).append("-");
        getName(sb, Font.NAME_SUBFAMILY);
        return sb;
    }

    @Override
    public float getAdvanceWidth(int i, float pixelSize) {
        return font.getHmtxTable().getAdvanceWidth(i) * metrics.getScale(pixelSize);
    }

    @Override
    public final Metrics getMetrics() {
        return metrics;
    }

    @Override
    public Glyph getGlyph(char symbol) {
        TypecastGlyph result = (TypecastGlyph) char2Glyph.get(symbol);
        if (null == result) {
            // final short code = (short) char2Code.get(symbol);
            short code = (short) cmapFormat.mapCharCode(symbol);
            if(0 == code && 0 != symbol) {
                // reserved special glyph IDs by convention
                switch(symbol) {
                    case ' ':  code = Glyph.ID_SPACE; break;
                    case '\n': code = Glyph.ID_CR; break;
                    default:   code = Glyph.ID_UNKNOWN;
                }
            }

            jogamp.graph.font.typecast.ot.OTGlyph glyph = font.getGlyph(code);
            if(null == glyph) {
                glyph = font.getGlyph(Glyph.ID_UNKNOWN);
            }
            if(null == glyph) {
                throw new RuntimeException("Could not retrieve glyph for symbol: <"+symbol+"> "+(int)symbol+" -> glyph id "+code);
            }
            final OutlineShape shape = TypecastRenderer.buildShape(glyph, vertexFactory);
            result = new TypecastGlyph(this, symbol, code, glyph.getBBox(), glyph.getAdvanceWidth(), shape);
            if(DEBUG) {
                System.err.println("New glyph: " + (int)symbol + " ( " + symbol +" ) -> " + code + ", contours " + glyph.getPointCount() + ": " + shape);
            }
            final HdmxTable hdmx = font.getHdmxTable();
            if (null!= result && null != hdmx) {
                /*if(DEBUG) {
                    System.err.println("hdmx "+hdmx);
                }*/
                for (int i=0; i<hdmx.getNumberOfRecords(); i++)
                {
                    final HdmxTable.DeviceRecord dr = hdmx.getRecord(i);
                    result.addAdvance(dr.getWidth(code), dr.getPixelSize());
                    /* if(DEBUG) {
                        System.err.println("hdmx advance : pixelsize = "+dr.getWidth(code)+" : "+ dr.getPixelSize());
                    } */
                }
            }
            char2Glyph.put(symbol, result);
        }
        return result;
    }

    @Override
    public float getStringWidth(CharSequence string, float pixelSize) {
        float width = 0;
        final int len = string.length();
        for (int i=0; i< len; i++)
        {
            char character = string.charAt(i);
            if (character == '\n') {
                width = 0;
            } else {
                Glyph glyph = getGlyph(character);
                width += glyph.getAdvance(pixelSize, false);
            }
        }

        return (int)(width + 0.5f);
    }

    @Override
    public float getStringHeight(CharSequence string, float pixelSize) {
        int height = 0;

        for (int i=0; i<string.length(); i++)
        {
            char character = string.charAt(i);
            if (character != ' ')
            {
                Glyph glyph = getGlyph(character);
                AABBox bbox = glyph.getBBox(pixelSize);
                height = (int)Math.ceil(Math.max(bbox.getHeight(), height));
            }
        }
        return height;
    }

    @Override
    public AABBox getStringBounds(CharSequence string, float pixelSize) {
        if (string == null) {
            return new AABBox();
        }
        final Metrics metrics = getMetrics();
        final float lineGap = metrics.getLineGap(pixelSize);
        final float ascent = metrics.getAscent(pixelSize);
        final float descent = metrics.getDescent(pixelSize);
        final float advanceY = lineGap - descent + ascent;
        float totalHeight = 0;
        float totalWidth = 0;
        float curLineWidth = 0;
        for (int i=0; i<string.length(); i++) {
            char character = string.charAt(i);
            if (character == '\n') {
                totalWidth = Math.max(curLineWidth, totalWidth);
                curLineWidth = 0;
                totalHeight -= advanceY;
                continue;
            }
            Glyph glyph = getGlyph(character);
            curLineWidth += glyph.getAdvance(pixelSize, true);
        }
        if (curLineWidth > 0) {
            totalHeight -= advanceY;
            totalWidth = Math.max(curLineWidth, totalWidth);
        }
        return new AABBox(0, 0, 0, totalWidth, totalHeight,0);
    }

    @Override
    final public int getNumGlyphs() {
        return font.getNumGlyphs();
    }

    @Override
    public boolean isPrintableChar( char c ) {
        return FontFactory.isPrintableChar(c);
    }

    @Override
    public String toString() {
        return getFullFamilyName(null).toString();
    }
}

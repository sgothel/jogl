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

import java.io.File;
import java.io.IOException;

import jogamp.graph.font.FontInt;
import jogamp.graph.font.JavaFontLoader;
import jogamp.graph.font.ttf.TTFontGlyph;
import jogamp.graph.font.ttf.TTFont.Metrics;
import jogamp.graph.geom.plane.AffineTransform;
import jogamp.graph.geom.plane.Path2D;
import net.java.dev.typecast.ot.OTFont;
import net.java.dev.typecast.ot.OTFontCollection;
import net.java.dev.typecast.ot.table.CmapFormat;
import net.java.dev.typecast.ot.table.CmapTable;
import net.java.dev.typecast.ot.table.HdmxTable;
import net.java.dev.typecast.ot.table.ID;

import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Vertex;

class TypecastFont implements FontInt {
	static final boolean DEBUG = false;
	
	final Vertex.Factory<? extends Vertex> pointFactory;
	final OTFontCollection fontset;
	final OTFont font;
    TypecastMetrics metrics;
    final CmapFormat cmapFormat;
	int cmapentries;
    // final IntIntHashMap char2Code;
    IntObjectHashMap char2Glyph; 

    public static TypecastFont create(Vertex.Factory<? extends Vertex> factory, String name) {
    	String path = JavaFontLoader.getByName(name);
    	OTFontCollection fontset;
		try {
			fontset = OTFontCollection.create(new File(path));
            return new TypecastFont(factory, fontset);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;    
    }
    
    public TypecastFont(Vertex.Factory<? extends Vertex> factory, OTFontCollection fontset) {
    	this.pointFactory = factory;
    	this.fontset = fontset;
        this.font = fontset.getFont(0);
        
    	CmapTable cmapTable = font.getCmapTable();
    	CmapFormat _cmapFormat = null;
    	/*
    	if(null == _cmapFormat) {
    		_cmapFormat = cmapTable.getCmapFormat(ID.platformMacintosh, ID.encodingASCII);
    	} */
    	if(null == _cmapFormat) {
    		// default unicode
    		_cmapFormat = cmapTable.getCmapFormat(ID.platformMicrosoft, ID.encodingUnicode);
    	}
    	if(null == _cmapFormat) {
    		// maybe a symbol font ?
    		_cmapFormat = cmapTable.getCmapFormat(ID.platformMicrosoft, ID.encodingSymbol);
    	}
    	if(null == _cmapFormat) {
    		throw new RuntimeException("Cannot find a suitable cmap table for font "+font);
    	}
    	cmapFormat = _cmapFormat;

    	cmapentries = 0;
        for (int i = 0; i < cmapFormat.getRangeCount(); ++i) {
            CmapFormat.Range range = cmapFormat.getRange(i);
            cmapentries += range.getEndCode() - range.getStartCode() + 1; // end included 
        }    	
        if(DEBUG) {
        	System.err.println("num glyphs: "+font.getNumGlyphs());
        	System.err.println("num cmap entries: "+cmapentries);
        }
       
        /*
    	char2Code = new IntIntHashMap(cmapentries + cmapentries/4);    	
        for (int i = 0; i < cmapFormat.getRangeCount(); ++i) {
            CmapFormat.Range range = cmapFormat.getRange(i);
            for (int j = range.getStartCode(); j <= range.getEndCode(); ++j) {
            	final int code = cmapFormat.mapCharCode(j);
            	char2Code.put(j, code);
            	if(code < 50) {
            		System.err.println(" char: " + (int)j + " ( " + (char)j +" ) -> " + code);
            	}
            }
        }
        */
        char2Glyph = new IntObjectHashMap(cmapentries + cmapentries/4);
    }

    public String getName() {
        return fontset.getFileName();
    }

    public Metrics getMetrics() {
        if (metrics == null) {
            metrics = new TypecastMetrics(this);
        }
        return metrics;
    }

    public Glyph getGlyph(char symbol) {
    	TypecastGlyph result = (TypecastGlyph) char2Glyph.get(symbol);    	
        if (null == result) {
        	// final short code = (short) char2Code.get(symbol);
        	final short code = (short) cmapFormat.mapCharCode(symbol);
        	net.java.dev.typecast.ot.OTGlyph glyph = font.getGlyph(code);
        	final Path2D path = TypecastRenderer.buildPath(glyph);
        	result = new TypecastGlyph(this, symbol, code, glyph.getBBox(), glyph.getAdvanceWidth(), path);
        	if(DEBUG) {
        		System.err.println("New glyph: " + (int)symbol + " ( " + (char)symbol +" ) -> " + code + ", contours " + glyph.getPointCount() + ": " + path);
        	}
        	final HdmxTable hdmx = font.getHdmxTable();        	
            if (null != hdmx) {
                /*if(DEBUG) {
                    System.err.println("hdmx "+hdmx);
                }*/
                for (int i=0; i<hdmx.getNumberOfRecords(); i++)
                {
                    final HdmxTable.DeviceRecord dr = hdmx.getRecord(i); 
                    result.addAdvance(dr.getWidth(code), dr.getPixelSize());
                    if(DEBUG) {
                        System.err.println("hdmx advance : pixelsize = "+dr.getWidth(code)+" : "+ dr.getPixelSize());
                    }
                }
            }        	
        	char2Glyph.put(symbol, result);
        }
        return result;
    }

    public void getOutline(String string, float pixelSize, AffineTransform transform, Path2D[] result) {
    	TypecastRenderer.getOutline(pointFactory, this, string, pixelSize, transform, result);
    }

    public float getStringWidth(String string, float pixelSize) {
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

    public float getStringHeight(String string, float pixelSize) {
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

    final public int getNumGlyphs() {
    	return font.getNumGlyphs();
    }
}
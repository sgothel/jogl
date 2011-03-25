/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.font;

import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.graph.geom.plane.Path2D;

public interface Font {

	/**
	 * Metrics for font based on pixel size !
	 * 
	 * If no pixelSize is given, this font's static pixelSize is being used.
	 *  
	 * value = Table.value * fontSize * 1.0f / HeadTable.UnitsPerEm
	 */
    public interface Metrics {  
        public float getAscent();
        public float getDescent();
        public float getLineGap();
        public float getScale();
    	public float getScaleForPixelSize(float pixelSize);    	
		public AABBox getBBox();
    }

	/**
	 * Glyph for font symbols based on pixel size !
	 * 
	 * If no pixelSize is given, this font's static pixelSize is being used.
	 */
    public interface Glyph {
        public Font getFont();
        public char getSymbol();
        public AABBox getBBox();
        public float getAdvance();
        public float getAdvanceForPixelSize(float pixelSize, boolean useFrationalMetrics);
        public Path2D getPath();
        public Path2D getPathForPixelSize(float pixelSize); 
    }


    public String getName();
    public float getSize();
    public Metrics getMetrics();
    public Glyph getGlyph(char symbol);
    
    public float getStringWidth(String string);
    public float getStringHeight(String string);
    public AABBox getStringBounds(CharSequence string);

    public void getOutline(String string,
            AffineTransform transform,
            Path2D[] result);
    
    public int getNumGlyphs();
}
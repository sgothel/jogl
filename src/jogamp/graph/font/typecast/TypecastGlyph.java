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

import com.jogamp.graph.font.Font;
import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.graph.geom.plane.Path2D;

public class TypecastGlyph implements Font.Glyph {
    public class Advance
    {
        Font            font;
        float           advance; 
        float           advances[]; // in pixels
        float           sizes[];
        float           advanceCached = -1; // in pixels
        float           sizeCached = -1;
		
        public Advance(Font font, float advance)
        {
            this.font = font;
            this.advance = advance;
            
            this.advances = new float[0];
            this.sizes = new float[0];
        }
        
        public float getScaleForPixelSize(float pixelSize)
        {
            return this.font.getMetrics().getScaleForPixelSize(pixelSize);
        }
        
        public void add(float advance, float size)
        {
            float advancesNew[] = new float[this.advances.length+1];
            float sizesNew[] = new float[this.sizes.length+1];
			
            for (int i=0; i<this.advances.length; i++) {
	            advancesNew[i] = this.advances[i];
	            sizesNew[i] = this.sizes[i];
            }
			
            advancesNew[advancesNew.length-1] = advance;
            sizesNew[sizesNew.length-1] = size;
			
            this.advances = advancesNew;
            this.sizes = sizesNew;
        }
        
        public float get(float size, boolean useFrationalMetrics)
        {
            if (this.sizeCached != size) {
                this.sizeCached = size;
			
                float value = (this.advance * getScaleForPixelSize(size));
                if (useFrationalMetrics == false) {
                    //value = (float)Math.ceil(value);
                    // value = (int)value;
                    value = (int) ( value + 0.5f ) ; // TODO: check 
                }
            
                if (true)
                    {
                        for (int i=0; i<this.advances.length; i++)
                            {
                                if (this.sizes[i] == size)
                                    {
                                        value = this.advances[i];
                                        break;
                                    }
                            }
                    }
			
                this.advanceCached = value;
            }
            return this.advanceCached;
        }
        
        public String toString()
        {
            String string = "";
            for (int i=0; i<this.advances.length; i++) {
                string += "    size: "+this.sizes[i]+" advance: "+this.advances[i]+"\n";
            }
            if (string.length() > 0) {
                string = "\n  advances: \n"+string;
            }
            return "\nAdvance:"+
                "\n  advance: "+this.advance+
                string;
        }
    }
	
    public class Metrics
    {
    	AABBox	bbox;
    	AABBox	bbox_sized;
        Advance		advance;
		
        public Metrics(Font font, AABBox bbox, float advance)
        {
            this.bbox = bbox;
            this.advance = new Advance(font, advance);
        }
        
        public float getScaleForPixelSize(float pixelSize)
        {
            return this.advance.getScaleForPixelSize(pixelSize);
        }
        
        public AABBox getBBox()
        {
            return this.bbox;
        }
        
        public void addAdvance(float advance, float size)
        {
            this.advance.add(advance, size);
        }
        
        public float getAdvanceForPixelSize(float size, boolean useFrationalMetrics)
        {
            return this.advance.get(size, useFrationalMetrics);
        }
        
        public String toString()
        {
            return "\nMetrics:"+
                "\n  bbox: "+this.bbox+
                this.advance;
        }
    }	

    public static final short INVALID_ID    = (short)((1 << 16) - 1);
    public static final short MAX_ID        = (short)((1 << 16) - 2);
    
    private final Font font;
	    
    char        symbol;
    short       id;
    int         advance;
    Metrics     metrics;
    
    protected Path2D path; // in EM units
    protected Path2D pathSized;
    protected float numberSized;
	
    protected TypecastGlyph(Font font, char symbol) {
    	this.font = font;
        this.symbol = symbol;
    }
    
    protected TypecastGlyph(Font font,
    		                char symbol, short id, AABBox bbox, int advance, Path2D path) {
    	this.font = font;
        this.symbol = symbol;
        this.advance = advance;
        
        init(id, bbox, advance);
        
        this.path = path;
        this.pathSized = null;
        this.numberSized = 0.0f;
    }
	
    void init(short id, AABBox bbox, int advance) {
        this.id = id;
        this.advance = advance;
        this.metrics = new Metrics(this.font, bbox, this.advance);
    }
    
    public Font getFont() {
        return this.font;
    }
	
    public char getSymbol() {
        return this.symbol;
    }
	
    AABBox getBBoxUnsized() {
        return this.metrics.getBBox();
    }
    
    public AABBox getBBox() {
        return this.metrics.getBBox();
    }
    
    public Metrics getMetrics() {
        return this.metrics;
    }
	
    public short getID() {
        return this.id;
    }
	
    public float getScaleForPixelSize(float pixelSize) {
        return this.metrics.getScaleForPixelSize(pixelSize);
    }
	
    public AABBox getBBox(float size) {
    	AABBox newBox = getBBox().clone();
    	newBox.scale(size);
    	return newBox;
    }
	
    public AABBox getBBoxForPixelSize(float pixelSize) {
        return getBBox(getScaleForPixelSize(pixelSize));
    }
	
    protected void addAdvance(float advance, float size) {
        this.metrics.addAdvance(advance, size);
    }
    
    public float getAdvanceForPixelSize(float size, boolean useFrationalMetrics) {
        return this.metrics.getAdvanceForPixelSize(size, useFrationalMetrics);
    }
    
    public float getAdvance() {
        return getAdvanceForPixelSize(font.getSize(), false);
    }
    
    public Path2D getPath() {
    	return getPath(getScaleForPixelSize(font.getSize()));
    }
    	
    private Path2D getPath(float size)
    {
        if (this.numberSized != size) {
	        this.numberSized = size;
	        this.pathSized = AffineTransform.getScaleInstance(null, size, size).createTransformedShape(getPath());
        }        
        return this.pathSized;
    }
    
    public Path2D getPathForPixelSize(float pixelSize) {
        return getPath(getScaleForPixelSize(pixelSize));
    }
    
    public Path2D getNormalPath() {
        return this.path;
    }
    	    
}

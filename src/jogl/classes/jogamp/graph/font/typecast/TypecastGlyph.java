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

import java.util.HashMap;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.font.Font;
import com.jogamp.opengl.math.geom.AABBox;

public class TypecastGlyph implements Font.Glyph {
    public class Advance
    {
        private final Font      font;
        private final float     advance;
        HashMap<Float, Float> size2advance = new HashMap<Float, Float>();

        public Advance(Font font, float advance)
        {
            this.font = font;
            this.advance = advance;
        }

        public void reset() {
            size2advance.clear();
        }

        public float getScale(float pixelSize)
        {
            return this.font.getMetrics().getScale(pixelSize);
        }

        public void add(float advance, float size)
        {
            size2advance.put(size, advance);
        }

        public float get(float size, boolean useFrationalMetrics)
        {
            final Float fo = size2advance.get(size);
            if(null == fo) {
                float value = (this.advance * getScale(size));
                if (useFrationalMetrics == false) {
                    //value = (float)Math.ceil(value);
                    // value = (int)value;
                    value = (int) ( value + 0.5f ) ; // TODO: check
                }
                size2advance.put(size, value);
                return value;
            }
            return fo.floatValue();
        }

        @Override
        public String toString()
        {
            return "\nAdvance:"+
                "\n  advance: "+this.advance+
                "\n advances: \n"+size2advance;
        }
    }

    public class Metrics
    {
        private final AABBox    bbox;
        private final Advance advance;

        public Metrics(Font font, AABBox bbox, float advance)
        {
            this.bbox = bbox;
            this.advance = new Advance(font, advance);
        }

        public void reset() {
            advance.reset();
        }

        public float getScale(float pixelSize)
        {
            return this.advance.getScale(pixelSize);
        }

        public AABBox getBBox()
        {
            return this.bbox;
        }

        public void addAdvance(float advance, float size)
        {
            this.advance.add(advance, size);
        }

        public float getAdvance(float size, boolean useFrationalMetrics)
        {
            return this.advance.get(size, useFrationalMetrics);
        }

        @Override
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
    private final char symbol;
    private final OutlineShape shape; // in EM units
    private final short id;
    private final int advance;
    private final Metrics metrics;

    protected TypecastGlyph(Font font, char symbol, short id, AABBox bbox, int advance, OutlineShape shape) {
        this.font = font;
        this.symbol = symbol;
        this.shape = shape;
        this.id = id;
        this.advance = advance;
        this.metrics = new Metrics(this.font, bbox, this.advance);
    }

    /**
    public void reset(Path2D path) {
        this.path = path;
        this.metrics.reset();
    } */

    @Override
    public final Font getFont() {
        return this.font;
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
    public final float getScale(float pixelSize) {
        return this.metrics.getScale(pixelSize);
    }

    @Override
    public final AABBox getBBox(float pixelSize, float[] tmpV3) {
        final float size = getScale(pixelSize);
        AABBox newBox = getBBox().clone();
        newBox.scale(size, tmpV3);
        return newBox;
    }

    protected final void addAdvance(float advance, float size) {
        this.metrics.addAdvance(advance, size);
    }

    @Override
    public final float getAdvance(float pixelSize, boolean useFrationalMetrics) {
        return this.metrics.getAdvance(pixelSize, useFrationalMetrics);
    }

    @Override
    public final OutlineShape getShape() {
        return this.shape;
    }

    @Override
    public final int hashCode() {
        // 31 * x == (x << 5) - x
        int hash = 31 + font.getName(Font.NAME_UNIQUNAME).hashCode();
        return ((hash << 5) - hash) + id;
    }
}

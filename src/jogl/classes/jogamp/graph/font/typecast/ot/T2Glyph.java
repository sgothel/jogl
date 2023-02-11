/*
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004-2016 David Schweinsberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jogamp.graph.font.typecast.ot;

import com.jogamp.opengl.math.geom.AABBox;

import jogamp.graph.font.typecast.cff.CharstringType2;
import jogamp.graph.font.typecast.cff.T2Interpreter;

/**
 * An individual Type 2 Charstring glyph within a font.
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class T2Glyph extends Glyph {
    private final short _leftSideBearing;
    private final int _advanceWidth;
    private Point[] _points;
    private Integer[] _hstems;
    private Integer[] _vstems;

    /**
     * Construct a Glyph from a PostScript outline described by a Charstring.
     * @param glyph_id the assigned glyph_id of this instance
     * @param cs The CharstringType2 describing the glyph.
     * @param lsb The Left Side Bearing.
     * @param advance The advance width.
     */
    public T2Glyph(
            final int glyph_id, 
            CharstringType2 cs,
            short lsb,
            int advance) {
        super( glyph_id );
        _leftSideBearing = lsb;
        _advanceWidth = advance;
        T2Interpreter t2i = new T2Interpreter();
        _points = t2i.execute(cs);
        _hstems = t2i.getHStems();
        _vstems = t2i.getVStems();
        {
            AABBox bbox = new AABBox();
            for (Point p : _points) {
                bbox.resize(p.x, p.y, 0);
            }
            _bbox = bbox;             
        }
    }

    public final void clearPointData() {
        _points = null;
        _hstems = null;
        _vstems = null;
    }
    
    @Override
    public int getAdvanceWidth() {
        return _advanceWidth;
    }

    @Override
    public short getLeftSideBearing() {
        return _leftSideBearing;
    }

    @Override
    public Point getPoint(int i) {
        return _points[i];
    }

    @Override
    public int getPointCount() {
        return _points.length;
    }

    public Integer[] getHStems() {
        return _hstems;
    }

    public Integer[] getVStems() {
        return _vstems;
    }
    
    @Override
    public String toString() {
        return new StringBuilder()
            .append("T2Glyph id ").append(_glyph_id).append(", points ").append(_points.length)
            .append(", advance ").append(getAdvanceWidth())
            .append(", ").append(_bbox)
            .toString();
    }
}

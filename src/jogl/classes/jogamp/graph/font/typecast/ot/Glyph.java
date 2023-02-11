/*
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004-2015 David Schweinsberg
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

/**
 * An individual glyph within a font.
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public abstract class Glyph {
    final int _glyph_id;
    protected AABBox _bbox;

    public Glyph(final int glyph_id) {
        _glyph_id = glyph_id;
    }
    
    /** Return the assigned glyph ID of this instance */
    public final int getGlyphIndex() { return _glyph_id; }
    
    public abstract void clearPointData();

    /** Return the AABBox in font-units */
    public final AABBox getBBox() { return _bbox; }
    
    /** hmtx value */
    public abstract int getAdvanceWidth();
    
    /** hmtx value */
    public abstract short getLeftSideBearing();

    public abstract Point getPoint(int i);

    public abstract int getPointCount();
    
    @Override
    public abstract String toString();
}

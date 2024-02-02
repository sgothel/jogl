/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Batik" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/

package jogamp.graph.font.typecast.ot.table;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Glyph description for composite glyphs.  Composite glyphs are made up of one
 * or more simple glyphs, usually with some sort of transformation applied to
 * each.
 *
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class GlyfCompositeDescript extends GlyfDescript {

    /**
     * See {@link #getComponent(int)}
     */
    private final List<GlyfCompositeComp> _components = new ArrayList<>();

    /**
     * Creates a {@link GlyfCompositeDescript} from the given reader.
     *
     * <p>
     * A composite glyph starts with two uint16 values (“flags” and
     * “glyphIndex,” i.e. the index of the first contour in this composite
     * glyph); the data then varies according to “flags”).
     * </p>
     *
     * @param parentTable See {@link #getParentTable()}.
     * @param glyphIndex See {@link #getGlyphIndex()}.
     * @param di The reader to read from.
     */
    public GlyfCompositeDescript(
            final GlyfTable parentTable,
            final int glyphIndex,
            final DataInput di) throws IOException {
        super(parentTable, glyphIndex, (short) -1, di);

        // Get all of the composite components
        GlyfCompositeComp comp;
        int firstIndex = 0;
        int firstContour = 0;
        do {
            _components.add(comp = new GlyfCompositeComp(firstIndex, firstContour, di));
            final GlyfDescript desc = parentTable.getDescription(comp.getGlyphIndex());
            if (desc != null) {
                firstIndex += desc.getPointCount();
                firstContour += desc.getContourCount();
            }
        } while ((comp.getFlags() & GlyfCompositeComp.MORE_COMPONENTS) != 0);

        // Are there hinting intructions to read?
        if ((comp.getFlags() & GlyfCompositeComp.WE_HAVE_INSTRUCTIONS) != 0) {
            readInstructions(di, di.readShort());
        }
    }

    @Override
    public int getEndPtOfContours(final int contour) {
        final GlyfCompositeComp c = getCompositeCompEndPt(contour);
        if (c != null) {
            final GlyphDescription gd = getReferencedGlyph(c);
            return gd.getEndPtOfContours(contour - c.getFirstContour()) + c.getFirstIndex();
        }
        return 0;
    }

    @Override
    public byte getFlags(final int i) {
        final GlyfCompositeComp c = getCompositeComp(i);
        if (c != null) {
            final GlyphDescription gd = getReferencedGlyph(c);
            return gd.getFlags(i - c.getFirstIndex());
        }
        return 0;
    }

    @Override
    public short getXCoordinate(final int i) {
        final GlyfCompositeComp c = getCompositeComp(i);
        if (c != null) {
            final GlyphDescription gd = getReferencedGlyph(c);
            final int n = i - c.getFirstIndex();
            final int x = gd.getXCoordinate(n);
            final int y = gd.getYCoordinate(n);
            return c.transformX(x, y);
        }
        return 0;
    }

    private GlyfDescript getReferencedGlyph(final GlyfCompositeComp c) {
        return c.getReferencedGlyph(_parentTable);
    }

    @Override
    public short getYCoordinate(final int i) {
        final GlyfCompositeComp c = getCompositeComp(i);
        if (c != null) {
            final GlyphDescription gd = getReferencedGlyph(c);
            final int n = i - c.getFirstIndex();
            final int x = gd.getXCoordinate(n);
            final int y = gd.getYCoordinate(n);
            return c.transformY(x, y);
        }
        return 0;
    }

    @Override
    public boolean isComposite() {
        return true;
    }

    @Override
    public int getPointCount() {
        final GlyfCompositeComp last = lastComponent();
        final GlyphDescription gd = getReferencedGlyph(last);
        if (gd != null) {
            return last.getFirstIndex() + gd.getPointCount();
        } else {
            return 0;
        }
    }

    @Override
    public int getContourCount() {
        final GlyfCompositeComp last = lastComponent();
        return last.getFirstContour() + getReferencedGlyph(last).getContourCount();
    }

    private GlyfCompositeComp lastComponent() {
        return _components.get(_components.size() - 1);
    }

    public int getComponentIndex(final int i) {
        return _components.get(i).getFirstIndex();
    }

    /**
     * The number of {@link GlyfCompositeComp} in this {@link GlyfCompositeDescript}.
     *
     * @see #getComponent(int)
     */
    public int getComponentCount() {
        return _components.size();
    }

    /**
     * The {@link GlyfCompositeComp} with the given index.
     *
     * @see #getComponentCount()
     */
    public GlyfCompositeComp getComponent(final int i) {
        return _components.get(i);
    }

    private GlyfCompositeComp getCompositeComp(final int i) {
        GlyfCompositeComp c;
        for (final GlyfCompositeComp component : _components) {
            c = component;
            final GlyphDescription gd = getReferencedGlyph(c);
            if (c.getFirstIndex() <= i && i < (c.getFirstIndex() + gd.getPointCount())) {
                return c;
            }
        }
        return null;
    }

    private GlyfCompositeComp getCompositeCompEndPt(final int i) {
        GlyfCompositeComp c;
        for (final GlyfCompositeComp component : _components) {
            c = component;
            final GlyphDescription gd = getReferencedGlyph(c);
            if (c.getFirstContour() <= i && i < (c.getFirstContour() + gd.getContourCount())) {
                return c;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("        Composite Glyph\n");
        sb.append("        ---------------\n");
        sb.append(super.toString());
        sb.append("\n\n");
        for (final GlyfCompositeComp component : _components) {
            sb.append("        Component\n");
            sb.append("        ---------\n");
            sb.append(component.toString());
            sb.append("\n\n");
        }
        return sb.toString();
    }
}

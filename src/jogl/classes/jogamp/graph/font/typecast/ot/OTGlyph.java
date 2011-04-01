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

package jogamp.graph.font.typecast.ot;

import jogamp.graph.font.typecast.ot.table.Charstring;
import jogamp.graph.font.typecast.ot.table.CharstringType2;
import jogamp.graph.font.typecast.ot.table.GlyfDescript;
import jogamp.graph.font.typecast.ot.table.GlyphDescription;
import jogamp.graph.font.typecast.t2.T2Interpreter;

import com.jogamp.graph.geom.AABBox;



/**
 * An individual glyph within a font.
 * @version $Id: Glyph.java,v 1.3 2007-02-21 12:23:54 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>, Sven Gothel
 */
public class OTGlyph {

    protected short _leftSideBearing;
    protected int _advanceWidth;
    private Point[] _points;
    AABBox _bbox;

    /**
     * Construct a Glyph from a TrueType outline described by
     * a GlyphDescription.
     * @param cs The Charstring describing the glyph.
     * @param lsb The Left Side Bearing.
     * @param advance The advance width.
     */
    public OTGlyph(GlyphDescription gd, short lsb, int advance) {
        _leftSideBearing = lsb;
        _advanceWidth = advance;
        describe(gd);
    }

    /**
     * Construct a Glyph from a PostScript outline described by a Charstring.
     * @param cs The Charstring describing the glyph.
     * @param lsb The Left Side Bearing.
     * @param advance The advance width.
     */
    public OTGlyph(Charstring cs, short lsb, int advance) {
        _leftSideBearing = lsb;
        _advanceWidth = advance;
        if (cs instanceof CharstringType2) {
            T2Interpreter t2i = new T2Interpreter();
            _points = t2i.execute((CharstringType2) cs);
        } else {
            //throw unsupported charstring type
        }
    }

    public AABBox getBBox() { 
    	return _bbox; 
    }
    
    public int getAdvanceWidth() {
        return _advanceWidth;
    }

    public short getLeftSideBearing() {
        return _leftSideBearing;
    }

    public Point getPoint(int i) {
        return _points[i];
    }

    public int getPointCount() {
        return _points.length;
    }

    /**
     * Resets the glyph to the TrueType table settings
     */
    public void reset() {
    }

    /**
     * @param factor a 16.16 fixed value
     */
    public void scale(int factor) {
        for (int i = 0; i < _points.length; i++) {
            //points[i].x = ( points[i].x * factor ) >> 6;
            //points[i].y = ( points[i].y * factor ) >> 6;
            _points[i].x = ((_points[i].x<<10) * factor) >> 26;
            _points[i].y = ((_points[i].y<<10) * factor) >> 26;
        }
        _leftSideBearing = (short)(( _leftSideBearing * factor) >> 6);
        _advanceWidth = (_advanceWidth * factor) >> 6;
    }

    /**
     * Set the points of a glyph from the GlyphDescription
     */
    private void describe(GlyphDescription gd) {
        int endPtIndex = 0;
        _points = new Point[gd.getPointCount() /* + 2 */ ];
        for (int i = 0; i < gd.getPointCount(); i++) {
            boolean endPt = gd.getEndPtOfContours(endPtIndex) == i;
            if (endPt) {
                endPtIndex++;
            }
            _points[i] = new Point(
                    gd.getXCoordinate(i),
                    gd.getYCoordinate(i),
                    (gd.getFlags(i) & GlyfDescript.onCurve) != 0,
                    endPt);
        }

        // Append the origin and advanceWidth points (n & n+1)
        // _points[gd.getPointCount()] = new Point(0, 0, true, true);
        // _points[gd.getPointCount()+1] = new Point(_advanceWidth, 0, true, true);
        
		_bbox = new AABBox(gd.getXMinimum(), gd.getYMinimum(), 0, gd.getXMaximum(), gd.getYMaximum(), 0);
    }
}

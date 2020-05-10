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

/**
 * Specifies access to glyph description classes, simple and composite.
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public interface GlyphDescription {
    
    /**
     * Index of the glyph in the {@link GlyfTable}.
     * 
     * @see GlyfTable#getDescription(int)
     */
    int getGlyphIndex();
    
    /**
     * int16
     * 
     * If the number of contours is greater than or equal to zero, this is a
     * simple glyph. If negative, this is a composite glyph — the value -1
     * should be used for composite glyphs.
     */
    int getNumberOfContours();

    /**
     * int16    xMin    Minimum x for coordinate data.
     */
    short getXMinimum();

    /**
     * int16    yMin    Minimum y for coordinate data.
     */
    short getYMinimum();

    /**
     * int16    xMax    Maximum x for coordinate data.
     */
    short getXMaximum();

    /**
     * int16    yMax    Maximum y for coordinate data.
     */
    short getYMaximum();

    /**
     * uint16 endPtsOfContours[numberOfContours] Array of point indices for the
     * last point of each contour, in increasing numeric order.
     */
    int getEndPtOfContours(int contour);
    
    /**
     * The flags for the point with the given index.
     * 
     * @see GlyfDescript#ON_CURVE_POINT
     * @see GlyfDescript#X_SHORT_VECTOR
     * @see GlyfDescript#Y_SHORT_VECTOR
     * @see GlyfDescript#REPEAT_FLAG
     * @see GlyfDescript#X_IS_SAME_OR_POSITIVE_X_SHORT_VECTOR
     * @see GlyfDescript#Y_IS_SAME_OR_POSITIVE_Y_SHORT_VECTOR
     * @see GlyfDescript#OVERLAP_SIMPLE
     */
    byte getFlags(int i);
    
    short getXCoordinate(int i);
    
    short getYCoordinate(int i);
    
    boolean isComposite();
    
    int getPointCount();
    
    int getContourCount();

    //  public int getComponentIndex(int c);
    //  public int getComponentCount();
}

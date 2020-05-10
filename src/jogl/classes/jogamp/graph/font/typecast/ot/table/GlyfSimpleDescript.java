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

import jogamp.graph.font.typecast.ot.Disassembler;
import jogamp.graph.font.typecast.ot.Fmt;

/**
 * Simple Glyph Description
 * 
 * <p>
 * This is the table information needed if numberOfContours is greater than or
 * equal to zero, that is, a glyph is not a composite. Note that point numbers
 * are base-zero indices that are numbered sequentially across all of the
 * contours for a glyph; that is, the first point number of each contour (except
 * the first) is one greater than the last point number of the preceding
 * contour.
 * </p>
 * 
 * @see "https://docs.microsoft.com/en-us/typography/opentype/spec/glyf"
 * 
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class GlyfSimpleDescript extends GlyfDescript {

    /**
     * @see #getEndPtOfContours(int)
     */
    private int[] _endPtsOfContours;
    
    /**
     * @see #getFlags(int)
     */
    private byte[] _flags;
    
    /**
     * @see #getXCoordinate(int)
     */
    private short[] _xCoordinates;
    
    /**
     * @see #getYCoordinate(int)
     */
    private short[] _yCoordinates;
    
    /**
     * @see #getPointCount()
     */
    private int _count;

    /**
     * Creates a {@link GlyfSimpleDescript}.
     *
     * @param parentTable
     *        The {@link GlyfTable} this instance belongs to.
     * @param glyphIndex
     *        See {@link #getGlyphIndex()}
     * @param numberOfContours
     *        See {@link #getNumberOfContours()}
     * @param di
     *        The reader to read from.
     */
    public GlyfSimpleDescript(
            GlyfTable parentTable,
            int glyphIndex,
            short numberOfContours,
            DataInput di) throws IOException {
        super(parentTable, glyphIndex, numberOfContours, di);
        
        // Simple glyph description
        _endPtsOfContours = new int[numberOfContours];
        for (int i = 0; i < numberOfContours; i++) {
            _endPtsOfContours[i] = di.readUnsignedShort();
        }

        // The last end point index reveals the total number of points
        _count = _endPtsOfContours[numberOfContours-1] + 1;
        _flags = new byte[_count];
        _xCoordinates = new short[_count];
        _yCoordinates = new short[_count];

        int instructionCount = di.readUnsignedShort();
        readInstructions(di, instructionCount);
        readFlags(_count, di);
        readCoords(_count, di);
    }
    
    @Override
    public int getNumberOfContours() {
        return _endPtsOfContours.length;
    }

    @Override
    public int getEndPtOfContours(int contour) {
        return _endPtsOfContours[contour];
    }

    @Override
    public byte getFlags(int i) {
        return _flags[i];
    }

    @Override
    public short getXCoordinate(int i) {
        return _xCoordinates[i];
    }

    @Override
    public short getYCoordinate(int i) {
        return _yCoordinates[i];
    }

    @Override
    public boolean isComposite() {
        return false;
    }

    @Override
    public int getPointCount() {
        return _count;
    }

    @Override
    public int getContourCount() {
        return getNumberOfContours();
    }
    /*
    public int getComponentIndex(int c) {
    return 0;
    }

    public int getComponentCount() {
    return 1;
    }
     */

    /**
     * Reads the glyph coordinates.
     * 
     * <p>
     * Note: In the 'glyf' table, the position of a point is not stored in
     * absolute terms but as a vector relative to the previous point. The
     * delta-x and delta-y vectors represent these (often small) changes in
     * position. Coordinate values are in font design units, as defined by the
     * {@link HeadTable#getUnitsPerEm() unitsPerEm} field in the
     * {@link HeadTable 'head'} table. Note that smaller
     * {@link HeadTable#getUnitsPerEm() unitsPerEm} values will make it more
     * likely that delta-x and delta-y values can fit in a smaller
     * representation (8-bit rather than 16-bit), though with a trade-off in the
     * level or precision that can be used for describing an outline.
     * </p>
     * 
     * <p>
     * The table is stored as relative values, but we'll store them as absolutes
     * </p>
     */
    private void readCoords(int count, DataInput di) throws IOException {
        short x = 0;
        short y = 0;
        for (int i = 0; i < count; i++) {
            byte flag = _flags[i];
            if ((flag & X_IS_SAME_OR_POSITIVE_X_SHORT_VECTOR) != 0) {
                if ((flag & X_SHORT_VECTOR) != 0) {
                    x += (short) di.readUnsignedByte();
                }
            } else {
                if ((flag & X_SHORT_VECTOR) != 0) {
                    x -= (short) di.readUnsignedByte();
                } else {
                    x += di.readShort();
                }
            }
            _xCoordinates[i] = x;
        }

        for (int i = 0; i < count; i++) {
            if ((_flags[i] & Y_IS_SAME_OR_POSITIVE_Y_SHORT_VECTOR) != 0) {
                if ((_flags[i] & Y_SHORT_VECTOR) != 0) {
                    y += (short) di.readUnsignedByte();
                }
            } else {
                if ((_flags[i] & Y_SHORT_VECTOR) != 0) {
                    y -= (short) di.readUnsignedByte();
                } else {
                    y += di.readShort();
                }
            }
            _yCoordinates[i] = y;
        }
    }

    /**
     * Reads the flags table.
     * 
     * <p>
     * Note: The flags are run-length encoded.
     * </p>
     * 
     * <p>
     * Each element in the flags array is a single byte, each of which has
     * multiple flag bits with distinct meanings, see {@link #getFlags(int)}.
     * </p>
     * 
     * <p>
     * In logical terms, there is one flag byte element, one x-coordinate, and
     * one y-coordinate for each point. Note, however, that the flag byte
     * elements and the coordinate arrays use packed representations. In
     * particular, if a logical sequence of flag elements or sequence of x- or
     * y-coordinates is repeated, then the actual flag byte element or
     * coordinate value can be given in a single entry, with special flags used
     * to indicate that this value is repeated for subsequent logical entries.
     * The actual stored size of the flags or coordinate arrays must be
     * determined by parsing the flags array entries. See the flag descriptions
     * below for details.
     * </p>
     * 
     * @see #getFlags(int)
     */
    private void readFlags(int flagCount, DataInput di) throws IOException {
        try {
            for (int index = 0; index < flagCount; index++) {
                _flags[index] = di.readByte();
                if ((_flags[index] & REPEAT_FLAG) != 0) {
                    int repeats = di.readUnsignedByte();
                    for (int i = 1; i <= repeats; i++) {
                        _flags[index + i] = _flags[index];
                    }
                    index += repeats;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("error: array index out of bounds");
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("        Simple Glyph\n");
        sb.append("        ------------\n");
        sb.append(super.toString());
        sb.append("\n\n");
        sb.append("        EndPoints\n");
        sb.append("        ---------");
        for (int i = 0; i < _endPtsOfContours.length; i++) {
            sb.append("\n          ").append(i).append(": ").append(_endPtsOfContours[i]);
        }
        sb.append("\n\n");
        sb.append("        Instructions\n");
        sb.append("        ------------\n");
        sb.append("          length: ");
        sb.append(getInstructions().length).append("\n");
        sb.append(Disassembler.disassemble(getInstructions(), 10));
        sb.append("\n        Flags\n        -----");
        for (int i = 0; i < _flags.length; i++) {
            sb.append("\n          ").append(Fmt.pad(3, i)).append(":  ");
            if ((_flags[i] & OVERLAP_SIMPLE) != 0) {
                sb.append("SOver ");
            } else {
                sb.append("      ");
            }
            if ((_flags[i] & Y_IS_SAME_OR_POSITIVE_Y_SHORT_VECTOR) != 0) {
                sb.append("YDual ");
            } else {
                sb.append("      ");
            }
            if ((_flags[i] & X_IS_SAME_OR_POSITIVE_X_SHORT_VECTOR) != 0) {
                sb.append("XDual ");
            } else {
                sb.append("      ");
            }
            if ((_flags[i] & REPEAT_FLAG) != 0) {
                sb.append("Repeat ");
            } else {
                sb.append("       ");
            }
            if ((_flags[i] & Y_SHORT_VECTOR) != 0) {
                sb.append("Y-Short ");
            } else {
                sb.append("        ");
            }
            if ((_flags[i] & X_SHORT_VECTOR) != 0) {
                sb.append("X-Short ");
            } else {
                sb.append("        ");
            }
            if ((_flags[i] & ON_CURVE_POINT) != 0) {
                sb.append("On");
            } else {
                sb.append("  ");
            }
        }
        sb.append("\n\n        Coordinates\n        -----------");
        short oldX = 0;
        short oldY = 0;
        for (int i = 0; i < _xCoordinates.length; i++) {
            sb.append("\n          ").append(Fmt.pad(3, i))
                .append(": Rel (").append(_xCoordinates[i] - oldX)
                .append(", ").append(_yCoordinates[i] - oldY)
                .append(")  ->  Abs (").append(_xCoordinates[i])
                .append(", ").append(_yCoordinates[i]).append(")");
            oldX = _xCoordinates[i];
            oldY = _yCoordinates[i];
        }
        return sb.toString();
    }
}

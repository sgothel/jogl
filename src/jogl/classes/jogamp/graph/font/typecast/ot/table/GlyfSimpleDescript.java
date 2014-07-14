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

/**
 * @version $Id: GlyfSimpleDescript.java,v 1.3 2007-01-24 09:47:47 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class GlyfSimpleDescript extends GlyfDescript {

    private final int[] _endPtsOfContours;
    private final byte[] _flags;
    private final short[] _xCoordinates;
    private final short[] _yCoordinates;
    private final int _count;

    public GlyfSimpleDescript(
            final GlyfTable parentTable,
            final int glyphIndex,
            final short numberOfContours,
            final DataInput di) throws IOException {
        super(parentTable, glyphIndex, numberOfContours, di);

        // Simple glyph description
        _endPtsOfContours = new int[numberOfContours];
        for (int i = 0; i < numberOfContours; i++) {
            _endPtsOfContours[i] = di.readShort();
        }

        // The last end point index reveals the total number of points
        _count = _endPtsOfContours[numberOfContours-1] + 1;
        _flags = new byte[_count];
        _xCoordinates = new short[_count];
        _yCoordinates = new short[_count];

        final int instructionCount = di.readShort();
        readInstructions(di, instructionCount);
        readFlags(_count, di);
        readCoords(_count, di);
    }

    @Override
    public int getEndPtOfContours(final int i) {
        return _endPtsOfContours[i];
    }

    @Override
    public byte getFlags(final int i) {
        return _flags[i];
    }

    @Override
    public short getXCoordinate(final int i) {
        return _xCoordinates[i];
    }

    @Override
    public short getYCoordinate(final int i) {
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
     * The table is stored as relative values, but we'll store them as absolutes
     */
    private void readCoords(final int count, final DataInput di) throws IOException {
        short x = 0;
        short y = 0;
        for (int i = 0; i < count; i++) {
            if ((_flags[i] & xDual) != 0) {
                if ((_flags[i] & xShortVector) != 0) {
                    x += (short) di.readUnsignedByte();
                }
            } else {
                if ((_flags[i] & xShortVector) != 0) {
                    x += (short) -((short) di.readUnsignedByte());
                } else {
                    x += di.readShort();
                }
            }
            _xCoordinates[i] = x;
        }

        for (int i = 0; i < count; i++) {
            if ((_flags[i] & yDual) != 0) {
                if ((_flags[i] & yShortVector) != 0) {
                    y += (short) di.readUnsignedByte();
                }
            } else {
                if ((_flags[i] & yShortVector) != 0) {
                    y += (short) -((short) di.readUnsignedByte());
                } else {
                    y += di.readShort();
                }
            }
            _yCoordinates[i] = y;
        }
    }

    /**
     * The flags are run-length encoded
     */
    private void readFlags(final int flagCount, final DataInput di) throws IOException {
        try {
            for (int index = 0; index < flagCount; index++) {
                _flags[index] = di.readByte();
                if ((_flags[index] & repeat) != 0) {
                    final int repeats = di.readByte();
                    for (int i = 1; i <= repeats; i++) {
                        _flags[index + i] = _flags[index];
                    }
                    index += repeats;
                }
            }
        } catch (final ArrayIndexOutOfBoundsException e) {
            System.out.println("error: array index out of bounds");
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("\n\n        EndPoints\n        ---------");
        for (int i = 0; i < _endPtsOfContours.length; i++) {
            sb.append("\n          ").append(i).append(": ").append(_endPtsOfContours[i]);
        }
        sb.append("\n\n          Length of Instructions: ");
        sb.append(getInstructions().length).append("\n");
        sb.append(Disassembler.disassemble(getInstructions(), 8));
        sb.append("\n        Flags\n        -----");
        for (int i = 0; i < _flags.length; i++) {
            sb.append("\n          ").append(i).append(":  ");
            if ((_flags[i] & 0x20) != 0) {
                sb.append("YDual ");
            } else {
                sb.append("      ");
            }
            if ((_flags[i] & 0x10) != 0) {
                sb.append("XDual ");
            } else {
                sb.append("      ");
            }
            if ((_flags[i] & 0x08) != 0) {
                sb.append("Repeat ");
            } else {
                sb.append("       ");
            }
            if ((_flags[i] & 0x04) != 0) {
                sb.append("Y-Short ");
            } else {
                sb.append("        ");
            }
            if ((_flags[i] & 0x02) != 0) {
                sb.append("X-Short ");
            } else {
                sb.append("        ");
            }
            if ((_flags[i] & 0x01) != 0) {
                sb.append("On");
            } else {
                sb.append("  ");
            }
        }
        sb.append("\n\n        Coordinates\n        -----------");
        short oldX = 0;
        short oldY = 0;
        for (int i = 0; i < _xCoordinates.length; i++) {
            sb.append("\n          ").append(i)
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

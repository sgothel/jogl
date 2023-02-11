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

import jogamp.graph.font.typecast.ot.Fixed;

/**
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class HeadTable implements Table {

    private final int _versionNumber;
    private final int _fontRevision;
    private final int _checkSumAdjustment;
    private final int _magicNumber;
    private final short _flags;
    private final int _unitsPerEm;
    private final long _created;
    private final long _modified;
    private final short _xMin;
    private final short _yMin;
    private final short _xMax;
    private final short _yMax;
    private final short _macStyle;
    private final short _lowestRecPPEM;
    private final short _fontDirectionHint;
    private final short _indexToLocFormat;
    private final short _glyphDataFormat;

    public HeadTable(final DataInput di) throws IOException {
        _versionNumber = di.readInt();
        _fontRevision = di.readInt();
        _checkSumAdjustment = di.readInt();
        _magicNumber = di.readInt();
        _flags = di.readShort();
        _unitsPerEm = di.readUnsignedShort();
        _created = di.readLong();
        _modified = di.readLong();
        _xMin = di.readShort();
        _yMin = di.readShort();
        _xMax = di.readShort();
        _yMax = di.readShort();
        _macStyle = di.readShort();
        _lowestRecPPEM = di.readShort();
        _fontDirectionHint = di.readShort();
        _indexToLocFormat = di.readShort();
        _glyphDataFormat = di.readShort();
    }

    public int getCheckSumAdjustment() {
        return _checkSumAdjustment;
    }

    public long getCreated() {
        return _created;
    }

    public short getFlags() {
        return _flags;
    }

    public short getFontDirectionHint() {
        return _fontDirectionHint;
    }

    public int getFontRevision(){
        return _fontRevision;
    }

    public short getGlyphDataFormat() {
        return _glyphDataFormat;
    }

    public short getIndexToLocFormat() {
        return _indexToLocFormat;
    }

    public short getLowestRecPPEM() {
        return _lowestRecPPEM;
    }

    public short getMacStyle() {
        return _macStyle;
    }

    public long getModified() {
        return _modified;
    }

    public int getType() {
        return head;
    }

    public int getUnitsPerEm() {
        return _unitsPerEm;
    }

    public int getVersionNumber() {
        return _versionNumber;
    }

    public short getXMax() {
        return _xMax;
    }

    public short getXMin() {
        return _xMin;
    }

    public short getYMax() {
        return _yMax;
    }

    public short getYMin() {
        return _yMin;
    }

    @Override
    public String toString() {
        return "'head' Table - Font Header\n--------------------------" +
                "\n  'head' version:      " + Fixed.floatValue(_versionNumber) +
                "\n  fontRevision:        " + Fixed.roundedFloatValue(_fontRevision, 8) +
                "\n  checkSumAdjustment:  0x" + Integer.toHexString(_checkSumAdjustment).toUpperCase() +
                "\n  magicNumber:         0x" + Integer.toHexString(_magicNumber).toUpperCase() +
                "\n  flags:               0x" + Integer.toHexString(_flags).toUpperCase() +
                "\n  unitsPerEm:          " + _unitsPerEm +
                "\n  created:             " + _created +
                "\n  modified:            " + _modified +
                "\n  xMin:                " + _xMin +
                "\n  yMin:                " + _yMin +
                "\n  xMax:                " + _xMax +
                "\n  yMax:                " + _yMax +
                "\n  macStyle bits:       " + Integer.toHexString(_macStyle).toUpperCase() +
                "\n  lowestRecPPEM:       " + _lowestRecPPEM +
                "\n  fontDirectionHint:   " + _fontDirectionHint +
                "\n  indexToLocFormat:    " + _indexToLocFormat +
                "\n  glyphDataFormat:     " + _glyphDataFormat;
    }

}

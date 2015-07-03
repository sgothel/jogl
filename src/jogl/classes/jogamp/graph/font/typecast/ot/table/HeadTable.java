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
 * @version $Id: HeadTable.java,v 1.2 2004-12-21 10:23:20 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class HeadTable implements Table {

    private final DirectoryEntry _de;
    private final int _versionNumber;
    private final int _fontRevision;
    private final int _checkSumAdjustment;
    private final int _magicNumber;
    private final short _flags;
    private final short _unitsPerEm;
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

    protected HeadTable(final DirectoryEntry de, final DataInput di) throws IOException {
        this._de = (DirectoryEntry) de.clone();
        _versionNumber = di.readInt();
        _fontRevision = di.readInt();
        _checkSumAdjustment = di.readInt();
        _magicNumber = di.readInt();
        _flags = di.readShort();
        _unitsPerEm = di.readShort();
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

    @Override
    public int getType() {
        return head;
    }

    public short getUnitsPerEm() {
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
        return new StringBuilder()
            .append("'head' Table - Font Header\n--------------------------")
            .append("\n  'head' version:      ").append(Fixed.floatValue(_versionNumber))
            .append("\n  fontRevision:        ").append(Fixed.roundedFloatValue(_fontRevision, 8))
            .append("\n  checkSumAdjustment:  0x").append(Integer.toHexString(_checkSumAdjustment).toUpperCase())
            .append("\n  magicNumber:         0x").append(Integer.toHexString(_magicNumber).toUpperCase())
            .append("\n  flags:               0x").append(Integer.toHexString(_flags).toUpperCase())
            .append("\n  unitsPerEm:          ").append(_unitsPerEm)
            .append("\n  created:             ").append(_created)
            .append("\n  modified:            ").append(_modified)
            .append("\n  xMin:                ").append(_xMin)
            .append("\n  yMin:                ").append(_yMin)
            .append("\n  xMax:                ").append(_xMax)
            .append("\n  yMax:                ").append(_yMax)
            .append("\n  macStyle bits:       ").append(Integer.toHexString(_macStyle).toUpperCase())
            .append("\n  lowestRecPPEM:       ").append(_lowestRecPPEM)
            .append("\n  fontDirectionHint:   ").append(_fontDirectionHint)
            .append("\n  indexToLocFormat:    ").append(_indexToLocFormat)
            .append("\n  glyphDataFormat:     ").append(_glyphDataFormat)
            .toString();
    }

    /**
     * Get a directory entry for this table.  This uniquely identifies the
     * table in collections where there may be more than one instance of a
     * particular table.
     * @return A directory entry
     */
    @Override
    public DirectoryEntry getDirectoryEntry() {
        return _de;
    }

}

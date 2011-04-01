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

/**
 * @version $Id: Os2Table.java,v 1.2 2004-12-09 23:46:21 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class Os2Table implements Table {

    private DirectoryEntry _de;
    private int _version;
    private short _xAvgCharWidth;
    private int _usWeightClass;
    private int _usWidthClass;
    private short _fsType;
    private short _ySubscriptXSize;
    private short _ySubscriptYSize;
    private short _ySubscriptXOffset;
    private short _ySubscriptYOffset;
    private short _ySuperscriptXSize;
    private short _ySuperscriptYSize;
    private short _ySuperscriptXOffset;
    private short _ySuperscriptYOffset;
    private short _yStrikeoutSize;
    private short _yStrikeoutPosition;
    private short _sFamilyClass;
    private Panose _panose;
    private int _ulUnicodeRange1;
    private int _ulUnicodeRange2;
    private int _ulUnicodeRange3;
    private int _ulUnicodeRange4;
    private int _achVendorID;
    private short _fsSelection;
    private int _usFirstCharIndex;
    private int _usLastCharIndex;
    private short _sTypoAscender;
    private short _sTypoDescender;
    private short _sTypoLineGap;
    private int _usWinAscent;
    private int _usWinDescent;
    private int _ulCodePageRange1;
    private int _ulCodePageRange2;
    private short _sxHeight;
    private short _sCapHeight;
    private int _usDefaultChar;
    private int _usBreakChar;
    private int _usMaxContext;

    protected Os2Table(DirectoryEntry de, DataInput di) throws IOException {
        this._de = (DirectoryEntry) de.clone();
        _version = di.readUnsignedShort();
        _xAvgCharWidth = di.readShort();
        _usWeightClass = di.readUnsignedShort();
        _usWidthClass = di.readUnsignedShort();
        _fsType = di.readShort();
        _ySubscriptXSize = di.readShort();
        _ySubscriptYSize = di.readShort();
        _ySubscriptXOffset = di.readShort();
        _ySubscriptYOffset = di.readShort();
        _ySuperscriptXSize = di.readShort();
        _ySuperscriptYSize = di.readShort();
        _ySuperscriptXOffset = di.readShort();
        _ySuperscriptYOffset = di.readShort();
        _yStrikeoutSize = di.readShort();
        _yStrikeoutPosition = di.readShort();
        _sFamilyClass = di.readShort();
        byte[] buf = new byte[10];
        di.readFully(buf);
        _panose = new Panose(buf);
        _ulUnicodeRange1 = di.readInt();
        _ulUnicodeRange2 = di.readInt();
        _ulUnicodeRange3 = di.readInt();
        _ulUnicodeRange4 = di.readInt();
        _achVendorID = di.readInt();
        _fsSelection = di.readShort();
        _usFirstCharIndex = di.readUnsignedShort();
        _usLastCharIndex = di.readUnsignedShort();
        _sTypoAscender = di.readShort();
        _sTypoDescender = di.readShort();
        _sTypoLineGap = di.readShort();
        _usWinAscent = di.readUnsignedShort();
        _usWinDescent = di.readUnsignedShort();
        _ulCodePageRange1 = di.readInt();
        _ulCodePageRange2 = di.readInt();
        
        // OpenType 1.3
        if (_version == 2) {
            _sxHeight = di.readShort();
            _sCapHeight = di.readShort();
            _usDefaultChar = di.readUnsignedShort();
            _usBreakChar = di.readUnsignedShort();
            _usMaxContext = di.readUnsignedShort();
        }
    }

    public int getVersion() {
        return _version;
    }

    public short getAvgCharWidth() {
        return _xAvgCharWidth;
    }

    public int getWeightClass() {
        return _usWeightClass;
    }

    public int getWidthClass() {
        return _usWidthClass;
    }

    public short getLicenseType() {
        return _fsType;
    }

    public short getSubscriptXSize() {
        return _ySubscriptXSize;
    }

    public short getSubscriptYSize() {
        return _ySubscriptYSize;
    }

    public short getSubscriptXOffset() {
        return _ySubscriptXOffset;
    }

    public short getSubscriptYOffset() {
        return _ySubscriptYOffset;
    }

    public short getSuperscriptXSize() {
        return _ySuperscriptXSize;
    }

    public short getSuperscriptYSize() {
        return _ySuperscriptYSize;
    }

    public short getSuperscriptXOffset() {
        return _ySuperscriptXOffset;
    }

    public short getSuperscriptYOffset() {
        return _ySuperscriptYOffset;
    }

    public short getStrikeoutSize() {
        return _yStrikeoutSize;
    }

    public short getStrikeoutPosition() {
        return _yStrikeoutPosition;
    }

    public short getFamilyClass() {
        return _sFamilyClass;
    }

    public Panose getPanose() {
        return _panose;
    }

    public int getUnicodeRange1() {
        return _ulUnicodeRange1;
    }

    public int getUnicodeRange2() {
        return _ulUnicodeRange2;
    }

    public int getUnicodeRange3() {
        return _ulUnicodeRange3;
    }

    public int getUnicodeRange4() {
        return _ulUnicodeRange4;
    }

    public int getVendorID() {
        return _achVendorID;
    }

    public short getSelection() {
        return _fsSelection;
    }

    public int getFirstCharIndex() {
        return _usFirstCharIndex;
    }

    public int getLastCharIndex() {
        return _usLastCharIndex;
    }

    public short getTypoAscender() {
        return _sTypoAscender;
    }

    public short getTypoDescender() {
        return _sTypoDescender;
    }

    public short getTypoLineGap() {
        return _sTypoLineGap;
    }

    public int getWinAscent() {
        return _usWinAscent;
    }

    public int getWinDescent() {
        return _usWinDescent;
    }

    public int getCodePageRange1() {
        return _ulCodePageRange1;
    }

    public int getCodePageRange2() {
        return _ulCodePageRange2;
    }

    public short getXHeight() {
        return _sxHeight;
    }
    
    public short getCapHeight() {
        return _sCapHeight;
    }
    
    public int getDefaultChar() {
        return _usDefaultChar;
    }
    
    public int getBreakChar() {
        return _usBreakChar;
    }
    
    public int getMaxContext() {
        return _usMaxContext;
    }

    public int getType() {
        return OS_2;
    }

    public String toString() {
        return new StringBuffer()
            .append("'OS/2' Table - OS/2 and Windows Metrics\n---------------------------------------")
            .append("\n  'OS/2' version:      ").append(_version)
            .append("\n  xAvgCharWidth:       ").append(_xAvgCharWidth)
            .append("\n  usWeightClass:       ").append(_usWeightClass)
            .append("\n  usWidthClass:        ").append(_usWidthClass)
            .append("\n  fsType:              0x").append(Integer.toHexString(_fsType).toUpperCase())
            .append("\n  ySubscriptXSize:     ").append(_ySubscriptXSize)
            .append("\n  ySubscriptYSize:     ").append(_ySubscriptYSize)
            .append("\n  ySubscriptXOffset:   ").append(_ySubscriptXOffset)
            .append("\n  ySubscriptYOffset:   ").append(_ySubscriptYOffset)
            .append("\n  ySuperscriptXSize:   ").append(_ySuperscriptXSize)
            .append("\n  ySuperscriptYSize:   ").append(_ySuperscriptYSize)
            .append("\n  ySuperscriptXOffset: ").append(_ySuperscriptXOffset)
            .append("\n  ySuperscriptYOffset: ").append(_ySuperscriptYOffset)
            .append("\n  yStrikeoutSize:      ").append(_yStrikeoutSize)
            .append("\n  yStrikeoutPosition:  ").append(_yStrikeoutPosition)
            .append("\n  sFamilyClass:        ").append(_sFamilyClass>>8)
            .append("    subclass = ").append(_sFamilyClass&0xff)
            .append("\n  PANOSE:              ").append(_panose.toString())
            .append("\n  Unicode Range 1( Bits 0 - 31 ): ").append(Integer.toHexString(_ulUnicodeRange1).toUpperCase())
            .append("\n  Unicode Range 2( Bits 32- 63 ): ").append(Integer.toHexString(_ulUnicodeRange2).toUpperCase())
            .append("\n  Unicode Range 3( Bits 64- 95 ): ").append(Integer.toHexString(_ulUnicodeRange3).toUpperCase())
            .append("\n  Unicode Range 4( Bits 96-127 ): ").append(Integer.toHexString(_ulUnicodeRange4).toUpperCase())
            .append("\n  achVendID:           '").append(getVendorIDAsString())
            .append("'\n  fsSelection:         0x").append(Integer.toHexString(_fsSelection).toUpperCase())
            .append("\n  usFirstCharIndex:    0x").append(Integer.toHexString(_usFirstCharIndex).toUpperCase())
            .append("\n  usLastCharIndex:     0x").append(Integer.toHexString(_usLastCharIndex).toUpperCase())
            .append("\n  sTypoAscender:       ").append(_sTypoAscender)
            .append("\n  sTypoDescender:      ").append(_sTypoDescender)
            .append("\n  sTypoLineGap:        ").append(_sTypoLineGap)
            .append("\n  usWinAscent:         ").append(_usWinAscent)
            .append("\n  usWinDescent:        ").append(_usWinDescent)
            .append("\n  CodePage Range 1( Bits 0 - 31 ): ").append(Integer.toHexString(_ulCodePageRange1).toUpperCase())
            .append("\n  CodePage Range 2( Bits 32- 63 ): ").append(Integer.toHexString(_ulCodePageRange2).toUpperCase())
            .toString();
    }
    
    private String getVendorIDAsString() {
        return new StringBuffer()
            .append((char)((_achVendorID>>24)&0xff))
            .append((char)((_achVendorID>>16)&0xff))
            .append((char)((_achVendorID>>8)&0xff))
            .append((char)((_achVendorID)&0xff))
            .toString();
    }
    
    /**
     * Get a directory entry for this table.  This uniquely identifies the
     * table in collections where there may be more than one instance of a
     * particular table.
     * @return A directory entry
     */
    public DirectoryEntry getDirectoryEntry() {
        return _de;
    }
}

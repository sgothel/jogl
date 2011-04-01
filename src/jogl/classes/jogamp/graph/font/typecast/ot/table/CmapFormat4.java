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
 * @version $Id: CmapFormat4.java,v 1.3 2004-12-21 16:57:23 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class CmapFormat4 extends CmapFormat {

    private int _segCountX2;
    private int _searchRange;
    private int _entrySelector;
    private int _rangeShift;
    private int[] _endCode;
    private int[] _startCode;
    private int[] _idDelta;
    private int[] _idRangeOffset;
    private int[] _glyphIdArray;
    private int _segCount;

    protected CmapFormat4(DataInput di) throws IOException {
        super(di); // 6
        _format = 4;
        _segCountX2 = di.readUnsignedShort(); // +2 (8)
        _segCount = _segCountX2 / 2;
        _endCode = new int[_segCount];
        _startCode = new int[_segCount];
        _idDelta = new int[_segCount];
        _idRangeOffset = new int[_segCount];
        _searchRange = di.readUnsignedShort(); // +2 (10)
        _entrySelector = di.readUnsignedShort(); // +2 (12)
        _rangeShift = di.readUnsignedShort(); // +2 (14)
        for (int i = 0; i < _segCount; i++) {
            _endCode[i] = di.readUnsignedShort();
        } // + 2*segCount (2*segCount + 14)
        di.readUnsignedShort(); // reservePad  +2 (2*segCount + 16)
        for (int i = 0; i < _segCount; i++) {
            _startCode[i] = di.readUnsignedShort();
        } // + 2*segCount (4*segCount + 16)
        for (int i = 0; i < _segCount; i++) {
            _idDelta[i] = di.readUnsignedShort();
        } // + 2*segCount (6*segCount + 16)
        for (int i = 0; i < _segCount; i++) {
            _idRangeOffset[i] = di.readUnsignedShort();
        } // + 2*segCount (8*segCount + 16)

        // Whatever remains of this header belongs in glyphIdArray
        int count = (_length - (8*_segCount + 16)) / 2;
        _glyphIdArray = new int[count];
        for (int i = 0; i < count; i++) {
            _glyphIdArray[i] = di.readUnsignedShort();
        } // + 2*count (8*segCount + 2*count + 18)
        
        // Are there any padding bytes we need to consume?
//        int leftover = length - (8*segCount + 2*count + 18);
//        if (leftover > 0) {
//            di.skipBytes(leftover);
//        }
    }

    public int getRangeCount() {
        return _segCount;
    }
    
    public Range getRange(int index) throws ArrayIndexOutOfBoundsException {
        if (index < 0 || index >= _segCount) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return new Range(_startCode[index], _endCode[index]);
    }

    public int mapCharCode(int charCode) {
        try {
            for (int i = 0; i < _segCount; i++) {
                if (_endCode[i] >= charCode) {
                    if (_startCode[i] <= charCode) {
                        if (_idRangeOffset[i] > 0) {
                            return _glyphIdArray[_idRangeOffset[i]/2 + (charCode - _startCode[i]) - (_segCount - i)];
                        } else {
                            return (_idDelta[i] + charCode) % 65536;
                        }
                    } else {
                        break;
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("error: Array out of bounds - " + e.getMessage());
        }
        return 0;
    }

    public String toString() {
        return new StringBuffer()
            .append(super.toString())
            .append(", segCountX2: ")
            .append(_segCountX2)
            .append(", searchRange: ")
            .append(_searchRange)
            .append(", entrySelector: ")
            .append(_entrySelector)
            .append(", rangeShift: ")
            .append(_rangeShift)
            .append(", endCode: ")
            .append(_endCode)
            .append(", startCode: ")
            .append(_endCode)
            .append(", idDelta: ")
            .append(_idDelta)
            .append(", idRangeOffset: ")
            .append(_idRangeOffset).toString();
    }
}

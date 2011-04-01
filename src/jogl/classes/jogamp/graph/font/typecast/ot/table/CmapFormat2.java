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
 * High-byte mapping through table cmap format.
 * @version $Id: CmapFormat2.java,v 1.3 2004-12-21 16:56:54 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class CmapFormat2 extends CmapFormat {

    private class SubHeader {
        int _firstCode;
        int _entryCount;
        short _idDelta;
        int _idRangeOffset;
        int _arrayIndex;
    }
    
    private int[] _subHeaderKeys = new int[256];
    private SubHeader[] _subHeaders;
    private int[] _glyphIndexArray;

    protected CmapFormat2(DataInput di) throws IOException {
        super(di);
        _format = 2;
        
        int pos = 6;
        
        // Read the subheader keys, noting the highest value, as this will
        // determine the number of subheaders to read.
        int highest = 0;
        for (int i = 0; i < 256; ++i) {
            _subHeaderKeys[i] = di.readUnsignedShort();
            highest = Math.max(highest, _subHeaderKeys[i]);
            pos += 2;
        }
        int subHeaderCount = highest / 8 + 1;
        _subHeaders = new SubHeader[subHeaderCount];
        
        // Read the subheaders, once again noting the highest glyphIndexArray
        // index range.
        int indexArrayOffset = 8 * subHeaderCount + 518;
        highest = 0;
        for (int i = 0; i < _subHeaders.length; ++i) {
            SubHeader sh = new SubHeader();
            sh._firstCode = di.readUnsignedShort();
            sh._entryCount = di.readUnsignedShort();
            sh._idDelta = di.readShort();
            sh._idRangeOffset = di.readUnsignedShort();
            
            // Calculate the offset into the _glyphIndexArray
            pos += 8;
            sh._arrayIndex =
                    (pos - 2 + sh._idRangeOffset - indexArrayOffset) / 2;
            
            // What is the highest range within the glyphIndexArray?
            highest = Math.max(highest, sh._arrayIndex + sh._entryCount);
            
            _subHeaders[i] = sh;
        }
        
        // Read the glyphIndexArray
        _glyphIndexArray = new int[highest];
        for (int i = 0; i < _glyphIndexArray.length; ++i) {
            _glyphIndexArray[i] = di.readUnsignedShort();
        }
    }

    public int getRangeCount() {
        return _subHeaders.length;
    }
    
    public Range getRange(int index) throws ArrayIndexOutOfBoundsException {
        if (index < 0 || index >= _subHeaders.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        
        // Find the high-byte (if any)
        int highByte = 0;
        if (index != 0) {
            for (int i = 0; i < 256; ++i) {
                if (_subHeaderKeys[i] / 8 == index) {
                    highByte = i << 8;
                    break;
                }
            }
        }
        
        return new Range(
                highByte | _subHeaders[index]._firstCode,
                highByte | (_subHeaders[index]._firstCode +
                    _subHeaders[index]._entryCount - 1));
    }

    public int mapCharCode(int charCode) {
        
        // Get the appropriate subheader
        int index = 0;
        int highByte = charCode >> 8;
        if (highByte != 0) {
            index = _subHeaderKeys[highByte] / 8;
        }
        SubHeader sh = _subHeaders[index];
        
        // Is the charCode out-of-range?
        int lowByte = charCode & 0xff;
        if (lowByte < sh._firstCode ||
                lowByte >= (sh._firstCode + sh._entryCount)) {
            return 0;
        }
        
        // Now calculate the glyph index
        int glyphIndex =
                _glyphIndexArray[sh._arrayIndex + (lowByte - sh._firstCode)];
        if (glyphIndex != 0) {
            glyphIndex += sh._idDelta;
            glyphIndex %= 65536;
        }
        return glyphIndex;
    }
}

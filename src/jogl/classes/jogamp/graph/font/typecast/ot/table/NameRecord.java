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
 * @version $Id: NameRecord.java,v 1.2 2004-12-09 23:47:23 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class NameRecord {

    private short _platformId;
    private short _encodingId;
    private short _languageId;
    private short _nameId;
    private short _stringLength;
    private short _stringOffset;
    private String _record;

    protected NameRecord(DataInput di) throws IOException {
        _platformId = di.readShort();
        _encodingId = di.readShort();
        _languageId = di.readShort();
        _nameId = di.readShort();
        _stringLength = di.readShort();
        _stringOffset = di.readShort();
    }
    
    public short getEncodingId() {
        return _encodingId;
    }
    
    public short getLanguageId() {
        return _languageId;
    }
    
    public short getNameId() {
        return _nameId;
    }
    
    public short getPlatformId() {
        return _platformId;
    }

    public StringBuilder getRecordString(StringBuilder sb) {
        sb.append(_record);
        return sb;
    }

    protected void loadString(DataInput di) throws IOException {
        StringBuffer sb = new StringBuffer();
        di.skipBytes(_stringOffset);
        if (_platformId == ID.platformUnicode) {
            
            // Unicode (big-endian)
            for (int i = 0; i < _stringLength/2; i++) {
                sb.append(di.readChar());
            }
        } else if (_platformId == ID.platformMacintosh) {

            // Macintosh encoding, ASCII
            for (int i = 0; i < _stringLength; i++) {
                sb.append((char) di.readByte());
            }
        } else if (_platformId == ID.platformISO) {
            
            // ISO encoding, ASCII
            for (int i = 0; i < _stringLength; i++) {
                sb.append((char) di.readByte());
            }
        } else if (_platformId == ID.platformMicrosoft) {
            
            // Microsoft encoding, Unicode
            char c;
            for (int i = 0; i < _stringLength/2; i++) {
                c = di.readChar();
                sb.append(c);
            }
        }
        _record = sb.toString();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        
        sb.append("             Platform ID:       ").append(_platformId)
            .append("\n             Specific ID:       ").append(_encodingId)
            .append("\n             Language ID:       ").append(_languageId)
            .append("\n             Name ID:           ").append(_nameId)
            .append("\n             Length:            ").append(_stringLength)
            .append("\n             Offset:            ").append(_stringOffset)
            .append("\n\n").append(_record);
        
        return sb.toString();
    }
}

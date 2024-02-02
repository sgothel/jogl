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

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The naming table allows multilingual strings to be associated with the
 * OpenType font file. These strings can represent copyright notices, font
 * names, family names, style names, and so on.
 * 
 * Other parts of the OpenType font that require these strings can refer to them
 * using a language-independent name ID. In addition to language variants, the
 * table also allows for platform-specific character-encoding variants. Clients
 * that need a particular string can look it up by its platform ID, encoding ID,
 * language ID and name ID. Note that different platforms may have different
 * requirements for the encoding of strings.
 * 
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 * 
 * @see "https://docs.microsoft.com/en-us/typography/opentype/spec/name"
 */
public class NameTable implements Table {

    @SuppressWarnings("unused")
    private final short _formatSelector;
    private final short _numberOfNameRecords;
    private final short _stringStorageOffset;
    private final NameRecord[] _records;

    public NameTable(final DataInput di, final int length) throws IOException {
        _formatSelector = di.readShort();
        _numberOfNameRecords = di.readShort();
        _stringStorageOffset = di.readShort();
        _records = new NameRecord[_numberOfNameRecords];

        // Load the records, which contain the encoding information and string
        // offsets
        for (int i = 0; i < _numberOfNameRecords; i++) {
            _records[i] = new NameRecord(di);
        }

        // Load the string data into a buffer so the records can copy out the
        // bits they are interested in
        final byte[] buffer = new byte[length - _stringStorageOffset];
        di.readFully(buffer);

        // Now let the records get their hands on them
        for (int i = 0; i < _numberOfNameRecords; i++) {
            _records[i].loadString(
                    new DataInputStream(new ByteArrayInputStream(buffer)));
        }
    }
    
    @Override
    public int getType() {
        return name;
    }

    /**
     * uint16   Format selector (=0 or 1).
     */
    public short getFormat() {
        return _formatSelector;
    }

    /**
     * uint16   count   Number of name records.
     */
    public short getNumberOfNameRecords() {
        return _numberOfNameRecords;
    }

    /**
     * Offset16     stringOffset    Offset to start of string storage (from start of table).
     */
    public short getStringStorageOffset() {
        return _stringStorageOffset;
    }


    public NameRecord getRecord(final int i) {
        if(_numberOfNameRecords > i) {
            return _records[i];
        }
        return null;
    }

    public String getRecordsRecordString(final int i) {
        if(_numberOfNameRecords > i) {
            return _records[i].getRecordString();
        } else {
            return "";
        }
    }

    /** Return a named record string */
    public String getRecordString(final short nameId) {
        // Search for the first instance of this name ID
        for (int i = 0; i < _numberOfNameRecords; i++) {
            if (_records[i].getNameId() == nameId) {
                return _records[i].getRecordString();
            }
        }
        return "";
    }

    @Override
    public String toString() {
        return "'name' Table - Naming Table\n--------------------------------" +
                "\n        'name' format:       " + _formatSelector +
                "\n        count:               " + _numberOfNameRecords +
                "\n        stringOffset:        " + _stringStorageOffset +
                "\n        records:" +
                Arrays.asList(_records).stream().map(r -> "\n" + r.toString()).collect(Collectors.joining("\n"));
    }

}

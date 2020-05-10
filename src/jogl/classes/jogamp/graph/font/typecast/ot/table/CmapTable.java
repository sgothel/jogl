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
import java.util.Arrays;

/**
 * Character to Glyph Index Mapping Table
 * 
 * <p>
 * This table defines the mapping of character codes to the glyph index values
 * used in the font. It may contain more than one subtable, in order to support
 * more than one character encoding scheme.
 * </p>
 * 
 * <h2>Overview</h2>
 * 
 * <p>
 * This table defines mapping of character codes to a default glyph index.
 * Different subtables may be defined that each contain mappings for different
 * character encoding schemes. The table header indicates the character
 * encodings for which subtables are present.
 * </p>
 * 
 * <p>
 * Regardless of the encoding scheme, character codes that do not correspond to
 * any glyph in the font should be mapped to glyph index 0. The glyph at this
 * location must be a special glyph representing a missing character, commonly
 * known as .notdef.
 * </p>
 * 
 * <p>
 * Each subtable is in one of seven possible formats and begins with a format
 * field indicating the format used. The first four formats — formats 0, 2, 4
 * and 6 — were originally defined prior to Unicode 2.0. These formats allow for
 * 8-bit single-byte, 8-bit multi-byte, and 16-bit encodings. With the
 * introduction of supplementary planes in Unicode 2.0, the Unicode addressable
 * code space extends beyond 16 bits. To accommodate this, three additional
 * formats were added — formats 8, 10 and 12 — that allow for 32-bit encoding
 * schemes.
 * </p>
 * 
 * <p>
 * Other enhancements in Unicode led to the addition of other subtable formats.
 * Subtable format 13 allows for an efficient mapping of many characters to a
 * single glyph; this is useful for “last-resort” fonts that provide fallback
 * rendering for all possible Unicode characters with a distinct fallback glyph
 * for different Unicode ranges. Subtable format 14 provides a unified mechanism
 * for supporting Unicode variation sequences.
 * </p>
 * 
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class CmapTable implements Table {

    /**
     * @see #getVersion()
     */
    public static final int VERSION = 0x0000;
    
    private int _version = VERSION;
    private int _numTables;
    private CmapIndexEntry[] _entries;

    /**
     * Creates a {@link CmapTable}.
     *
     * @param di The reader to read from.
     */
    public CmapTable(DataInput di) throws IOException {
        _version = di.readUnsignedShort();
        _numTables = di.readUnsignedShort();
        long bytesRead = 4;
        
        // Get each of the index entries
        
        // Note: The encoding record entries in the 'cmap' header must be sorted
        // first by platform ID, then by platform-specific encoding ID, and then
        // by the language field in the corresponding subtable. Each platform
        // ID, platform-specific encoding ID, and subtable language combination
        // may appear only once in the 'cmap' table.
        _entries = new CmapIndexEntry[_numTables];
        for (int i = 0; i < _numTables; i++) {
            _entries[i] = new CmapIndexEntry(di);
            bytesRead += 8;
        }

        // Sort into their order of offset
        Arrays.sort(_entries);

        // Get each of the tables
        int lastOffset = -1;
        CmapFormat lastFormat = null;
        for (int i = 0; i < _numTables; i++) {
            if (_entries[i].getOffset() == lastOffset) {

                // This is a multiple entry
                _entries[i].setFormat(lastFormat);
                continue;
            } else if (_entries[i].getOffset() > bytesRead) {
                di.skipBytes(_entries[i].getOffset() - (int) bytesRead);
            } else if (_entries[i].getOffset() != bytesRead) {
                
                // Something is amiss
                throw new IOException();
            }
            int formatType = di.readUnsignedShort();
            lastFormat = CmapFormat.create(formatType, di);
            lastOffset = _entries[i].getOffset();
            _entries[i].setFormat(lastFormat);
            bytesRead += lastFormat.getLength();
        }
    }

    @Override
    public int getType() {
        return cmap;
    }

    /**
     * uint16 Table version number ({@link #VERSION}}).
     * 
     * <p>
     * Note: The 'cmap' table version number remains at {@link #VERSION} for
     * fonts that make use of the newer subtable formats.
     * </p>
     */
    public int getVersion() {
        return _version;
    }
    
    /**
     * uint16
     * 
     * Number of encoding tables that follow.
     */
    public int getNumTables() {
        return _numTables;
    }
    
    public CmapIndexEntry getCmapIndexEntry(int i) {
        return _entries[i];
    }
    
    public CmapFormat getCmapFormat(short platformId, short encodingId) {

        // Find the requested format
        for (int i = 0; i < _numTables; i++) {
            if (_entries[i].getPlatformId() == platformId
                    && _entries[i].getEncodingId() == encodingId) {
                return _entries[i].getFormat();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("cmap Table\n");
        sb.append("----------\n");
        sb.append("  numTables: " + _numTables + "\n");

        // Get each of the index entries
        for (int i = 0; i < _numTables; i++) {
            sb.append("\n").append(_entries[i].toString());
        }

        // Get each of the tables
//        for (int i = 0; i < numTables; i++) {
//            sb.append("\t").append(formats[i].toString()).append("\n");
//        }
        return sb.toString();
    }

}

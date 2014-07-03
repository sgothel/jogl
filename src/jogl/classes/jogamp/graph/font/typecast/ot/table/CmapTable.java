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
 * @version $Id: CmapTable.java,v 1.3 2004-12-21 10:22:56 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class CmapTable implements Table {

    private final DirectoryEntry _de;
    private final int _version;
    private final int _numTables;
    private final CmapIndexEntry[] _entries;

    protected CmapTable(final DirectoryEntry de, final DataInput di) throws IOException {
        _de = (DirectoryEntry) de.clone();
        _version = di.readUnsignedShort();
        _numTables = di.readUnsignedShort();
        long bytesRead = 4;
        _entries = new CmapIndexEntry[_numTables];

        // Get each of the index entries
        for (int i = 0; i < _numTables; i++) {
            _entries[i] = new CmapIndexEntry(di);
            bytesRead += 8;
        }

        // Sort into their order of offset
        Arrays.sort(_entries);

        // Get each of the tables
        int lastOffset = 0;
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
            final int formatType = di.readUnsignedShort();
            lastFormat = CmapFormat.create(formatType, di);
            lastOffset = _entries[i].getOffset();
            _entries[i].setFormat(lastFormat);
            bytesRead += lastFormat.getLength();
        }
    }

    public int getVersion() {
        return _version;
    }

    public int getNumTables() {
        return _numTables;
    }

    public CmapIndexEntry getCmapIndexEntry(final int i) {
        return _entries[i];
    }

    public CmapFormat getCmapFormat(final short platformId, final short encodingId) {

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
    public int getType() {
        return cmap;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder().append("cmap\n");

        // Get each of the index entries
        for (int i = 0; i < _numTables; i++) {
            sb.append("\t").append(_entries[i].toString()).append("\n");
        }

        // Get each of the tables
//        for (int i = 0; i < numTables; i++) {
//            sb.append("\t").append(formats[i].toString()).append("\n");
//        }
        return sb.toString();
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

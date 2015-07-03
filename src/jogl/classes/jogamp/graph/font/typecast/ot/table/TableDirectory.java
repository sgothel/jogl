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
 * @version $Id: TableDirectory.java,v 1.2 2004-12-09 23:46:21 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class TableDirectory {

    private int _version = 0;
    private short _numTables = 0;
    private short _searchRange = 0;
    private short _entrySelector = 0;
    private short _rangeShift = 0;
    private final DirectoryEntry[] _entries;

    public TableDirectory(final DataInput di) throws IOException {
        _version = di.readInt();
        _numTables = di.readShort();
        _searchRange = di.readShort();
        _entrySelector = di.readShort();
        _rangeShift = di.readShort();
        _entries = new DirectoryEntry[_numTables];
        for (int i = 0; i < _numTables; i++) {
            _entries[i] = new DirectoryEntry(di);
        }
    }

    public DirectoryEntry getEntry(final int index) {
        return _entries[index];
    }

    public DirectoryEntry getEntryByTag(final int tag) {
        for (int i = 0; i < _numTables; i++) {
            if (_entries[i].getTag() == tag) {
                return _entries[i];
            }
        }
        return null;
    }

    public short getEntrySelector() {
        return _entrySelector;
    }

    public short getNumTables() {
        return _numTables;
    }

    public short getRangeShift() {
        return _rangeShift;
    }

    public short getSearchRange() {
        return _searchRange;
    }

    public int getVersion() {
        return _version;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder()
            .append("Offset Table\n------ -----")
            .append("\n  sfnt version:     ").append(Fixed.floatValue(_version))
            .append("\n  numTables =       ").append(_numTables)
            .append("\n  searchRange =     ").append(_searchRange)
            .append("\n  entrySelector =   ").append(_entrySelector)
            .append("\n  rangeShift =      ").append(_rangeShift)
            .append("\n\n");
        for (int i = 0; i < _numTables; i++) {
            sb.append(i).append(". ").append(_entries[i].toString()).append("\n");
        }
        return sb.toString();
    }
}

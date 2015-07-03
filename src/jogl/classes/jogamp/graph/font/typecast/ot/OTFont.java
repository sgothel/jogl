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

package jogamp.graph.font.typecast.ot;

import java.io.DataInputStream;
import java.io.IOException;

import jogamp.graph.font.typecast.ot.table.CmapTable;
import jogamp.graph.font.typecast.ot.table.DirectoryEntry;
import jogamp.graph.font.typecast.ot.table.GlyfDescript;
import jogamp.graph.font.typecast.ot.table.GlyfTable;
import jogamp.graph.font.typecast.ot.table.HdmxTable;
import jogamp.graph.font.typecast.ot.table.HeadTable;
import jogamp.graph.font.typecast.ot.table.HheaTable;
import jogamp.graph.font.typecast.ot.table.HmtxTable;
import jogamp.graph.font.typecast.ot.table.LocaTable;
import jogamp.graph.font.typecast.ot.table.MaxpTable;
import jogamp.graph.font.typecast.ot.table.NameTable;
import jogamp.graph.font.typecast.ot.table.Os2Table;
import jogamp.graph.font.typecast.ot.table.PostTable;
import jogamp.graph.font.typecast.ot.table.Table;
import jogamp.graph.font.typecast.ot.table.TableDirectory;
import jogamp.graph.font.typecast.ot.table.TableFactory;
import jogamp.graph.font.typecast.ot.table.VheaTable;


/**
 * The TrueType font.
 * @version $Id: OTFont.java,v 1.6 2007-01-31 01:49:18 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>, Sven Gothel
 */
public class OTFont {

    private final OTFontCollection _fc;
    private TableDirectory _tableDirectory = null;
    private Table[] _tables;
    private Os2Table _os2;
    private CmapTable _cmap;
    private GlyfTable _glyf;
    private HeadTable _head;
    private HheaTable _hhea;
    private HdmxTable _hdmx;
    private HmtxTable _hmtx;
    private LocaTable _loca;
    private MaxpTable _maxp;
    private NameTable _name;
    private PostTable _post;
    private VheaTable _vhea;

    /**
     * Constructor
     */
    public OTFont(final OTFontCollection fc) {
        _fc = fc;
    }
    public StringBuilder getName(final int nameIndex, StringBuilder sb) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        return _name.getRecordsRecordString(sb, nameIndex);
    }

    public StringBuilder getAllNames(StringBuilder sb, final String separator) {
        if(null != _name) {
            if(null == sb) {
                sb = new StringBuilder();
            }
            for(int i=0; i<_name.getNumberOfNameRecords(); i++) {
                _name.getRecord(i).getRecordString(sb).append(separator);
            }
        }
        return sb;
    }

    public Table getTable(final int tableType) {
        for (int i = 0; i < _tables.length; i++) {
            if ((_tables[i] != null) && (_tables[i].getType() == tableType)) {
                return _tables[i];
            }
        }
        return null;
    }

    public Os2Table getOS2Table() {
        return _os2;
    }

    public CmapTable getCmapTable() {
        return _cmap;
    }

    public HeadTable getHeadTable() {
        return _head;
    }

    public HheaTable getHheaTable() {
        return _hhea;
    }

    public HdmxTable getHdmxTable() {
        return _hdmx;
    }

    public HmtxTable getHmtxTable() {
        return _hmtx;
    }

    public LocaTable getLocaTable() {
        return _loca;
    }

    public MaxpTable getMaxpTable() {
        return _maxp;
    }

    public NameTable getNameTable() {
        return _name;
    }

    public PostTable getPostTable() {
        return _post;
    }

    public VheaTable getVheaTable() {
        return _vhea;
    }

    public int getAscent() {
        return _hhea.getAscender();
    }

    public int getDescent() {
        return _hhea.getDescender();
    }

    public int getNumGlyphs() {
        return _maxp.getNumGlyphs();
    }

    public OTGlyph getGlyph(final int i) {

        final GlyfDescript _glyfDescr = _glyf.getDescription(i);
        return (null != _glyfDescr)
            ? new OTGlyph(
                _glyfDescr,
                _hmtx.getLeftSideBearing(i),
                _hmtx.getAdvanceWidth(i))
            : null;
    }

    public TableDirectory getTableDirectory() {
        return _tableDirectory;
    }

    private Table readTable(
            final DataInputStream dis,
            final int tablesOrigin,
            final int tag) throws IOException {
        dis.reset();
        final DirectoryEntry entry = _tableDirectory.getEntryByTag(tag);
        if (entry == null) {
            return null;
        }
        dis.skip(tablesOrigin + entry.getOffset());
        return TableFactory.create(_fc, this, entry, dis);
    }

    /**
     * @param dis OpenType/TrueType font file data.
     * @param directoryOffset The Table Directory offset within the file.  For a
     * regular TTF/OTF file this will be zero, but for a TTC (Font Collection)
     * the offset is retrieved from the TTC header.  For a Mac font resource,
     * offset is retrieved from the resource headers.
     * @param tablesOrigin The point the table offsets are calculated from.
     * Once again, in a regular TTF file, this will be zero.  In a TTC is is
     * also zero, but within a Mac resource, it is the beggining of the
     * individual font resource data.
     */
    protected void read(
            final DataInputStream dis,
            final int directoryOffset,
            final int tablesOrigin) throws IOException {

        // Load the table directory
        dis.reset();
        dis.skip(directoryOffset);
        _tableDirectory = new TableDirectory(dis);
        _tables = new Table[_tableDirectory.getNumTables()];

        // Load some prerequisite tables
        _head = (HeadTable) readTable(dis, tablesOrigin, Table.head);
        _hhea = (HheaTable) readTable(dis, tablesOrigin, Table.hhea);
        _maxp = (MaxpTable) readTable(dis, tablesOrigin, Table.maxp);
        _loca = (LocaTable) readTable(dis, tablesOrigin, Table.loca);
        _vhea = (VheaTable) readTable(dis, tablesOrigin, Table.vhea);

        int index = 0;
        _tables[index++] = _head;
        _tables[index++] = _hhea;
        _tables[index++] = _maxp;
        if (_loca != null) {
            _tables[index++] = _loca;
        }
        if (_vhea != null) {
            _tables[index++] = _vhea;
        }

        // Load all other tables
        for (int i = 0; i < _tableDirectory.getNumTables(); i++) {
            final DirectoryEntry entry = _tableDirectory.getEntry(i);
            if (entry.getTag() == Table.head
                    || entry.getTag() == Table.hhea
                    || entry.getTag() == Table.maxp
                    || entry.getTag() == Table.loca
                    || entry.getTag() == Table.vhea) {
                continue;
            }
            dis.reset();
            dis.skip(tablesOrigin + entry.getOffset());
            _tables[index] = TableFactory.create(_fc, this, entry, dis);
            ++index;
        }

        // Get references to commonly used tables (these happen to be all the
        // required tables)
        _cmap = (CmapTable) getTable(Table.cmap);
        _hdmx = (HdmxTable) getTable(Table.hdmx);
        _hmtx = (HmtxTable) getTable(Table.hmtx);
        _name = (NameTable) getTable(Table.name);
        _os2 = (Os2Table) getTable(Table.OS_2);
        _post = (PostTable) getTable(Table.post);

        // If this is a TrueType outline, then we'll have at least the
        // 'glyf' table (along with the 'loca' table)
        _glyf = (GlyfTable) getTable(Table.glyf);
    }

    @Override
    public String toString() {
        if (_tableDirectory != null) {
            return _tableDirectory.toString();
        } else {
            return "Empty font";
        }
    }
}

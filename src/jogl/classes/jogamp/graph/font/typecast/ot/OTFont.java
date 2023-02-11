/*
 * Typecast
 *
 * Copyright © 2004-2019 David Schweinsberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jogamp.graph.font.typecast.ot;

import java.io.DataInputStream;
import java.io.IOException;

import jogamp.graph.font.typecast.ot.table.CmapTable;
import jogamp.graph.font.typecast.ot.table.GsubTable;
import jogamp.graph.font.typecast.ot.table.HeadTable;
import jogamp.graph.font.typecast.ot.table.HheaTable;
import jogamp.graph.font.typecast.ot.table.HmtxTable;
import jogamp.graph.font.typecast.ot.table.MaxpTable;
import jogamp.graph.font.typecast.ot.table.NameRecord;
import jogamp.graph.font.typecast.ot.table.NameTable;
import jogamp.graph.font.typecast.ot.table.Os2Table;
import jogamp.graph.font.typecast.ot.table.PostTable;
import jogamp.graph.font.typecast.ot.table.Table;
import jogamp.graph.font.typecast.ot.table.TableDirectory;
import jogamp.graph.font.typecast.ot.table.VheaTable;


/**
 * The TrueType font.
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public abstract class OTFont {

    private final Os2Table _os2;
    private final CmapTable _cmap;
    private final HeadTable _head;
    private final HheaTable _hhea;
    private final HmtxTable _hmtx;
    private final MaxpTable _maxp;
    private final NameTable _name;
    private final PostTable _post;
    private final VheaTable _vhea;
    private final GsubTable _gsub;

    /**
     *
     * @param dis input stream marked at start with read-ahead set to known stream length
     * @param tableDirectory
     * @param tablesOrigin
     * @throws IOException
     */
    OTFont(final DataInputStream dis, final TableDirectory tableDirectory, final int tablesOrigin) throws IOException {
        // Load some prerequisite tables
        // (These are tables that are referenced by other tables, so we need to load
        // them first)
        seekTable(tableDirectory, dis, tablesOrigin, Table.head);
        _head = new HeadTable(dis);

        // 'hhea' is required by 'hmtx'
        seekTable(tableDirectory, dis, tablesOrigin, Table.hhea);
        _hhea = new HheaTable(dis);

        // 'maxp' is required by 'glyf', 'hmtx', 'loca', and 'vmtx'
        seekTable(tableDirectory, dis, tablesOrigin, Table.maxp);
        _maxp = new MaxpTable(dis);

        // 'vhea' is required by 'vmtx'
        int length = seekTable(tableDirectory, dis, tablesOrigin, Table.vhea);
        if (length > 0) {
            _vhea = new VheaTable(dis);
        } else {
            _vhea = null;
        }

        // 'post' is required by 'glyf'
        seekTable(tableDirectory, dis, tablesOrigin, Table.post);
        _post = new PostTable(dis);

        // Load all the other required tables
        seekTable(tableDirectory, dis, tablesOrigin, Table.cmap);
        _cmap = new CmapTable(dis);
        length = seekTable(tableDirectory, dis, tablesOrigin, Table.hmtx);
        _hmtx = new HmtxTable(dis, length, _hhea, _maxp);
        length = seekTable(tableDirectory, dis, tablesOrigin, Table.name);
        _name = new NameTable(dis, length);
        seekTable(tableDirectory, dis, tablesOrigin, Table.OS_2);
        _os2 = new Os2Table(dis);

        _gsub = null; // FIXME: delete?
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

    public HmtxTable getHmtxTable() {
        return _hmtx;
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

    public GsubTable getGsubTable() {
        return _gsub;
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

    public abstract Glyph getGlyph(int i);

    int seekTable(
            final TableDirectory tableDirectory,
            final DataInputStream dis,
            final int tablesOrigin,
            final int tag) throws IOException {
        dis.reset();
        final TableDirectory.Entry entry = tableDirectory.getEntryByTag(tag);
        if (entry == null) {
            return 0;
        }
        dis.skip(tablesOrigin + entry.getOffset());
        return entry.getLength();
    }

    public String getName(final int nameIndex) {
        return _name.getRecordsRecordString(nameIndex);
    }

    public StringBuilder getAllNames(StringBuilder sb, final String separator) {
        if(null != _name) {
            if(null == sb) {
                sb = new StringBuilder();
            }
            for(int i=0; i<_name.getNumberOfNameRecords(); i++) {
                final NameRecord nr = _name.getRecord(i);
                if( null != nr ) {
                    sb.append( nr.getRecordString() ).append(separator);
                }
            }
        }
        return sb;
    }

    @Override
    public String toString() {
        return _head.toString();
    }
}

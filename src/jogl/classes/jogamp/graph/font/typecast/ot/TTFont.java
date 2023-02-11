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

import jogamp.graph.font.typecast.ot.table.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class TTFont extends OTFont {

    private final GlyfTable _glyf;
    private GaspTable _gasp;
    private KernTable _kern;
    private HdmxTable _hdmx;
    private VdmxTable _vdmx;

    /**
     * Constructor method
     * @param dis
     * @param directoryOffset
     * @param tablesOrigin
     * @return
     * @throws IOException
     */
    public static TTFont read(final DataInputStream dis, final int directoryOffset, final int tablesOrigin) throws IOException {
        // Load the table directory
        dis.reset();
        dis.skip(directoryOffset);
        final TableDirectory tableDirectory = new TableDirectory(dis);
        return new TTFont(dis, tableDirectory, tablesOrigin);
    }

    private TTFont(final DataInputStream dis, final TableDirectory tableDirectory, final int tablesOrigin) throws IOException {
        super(dis, tableDirectory, tablesOrigin);

        // 'loca' is required by 'glyf'
        int length = seekTable(tableDirectory, dis, tablesOrigin, Table.loca);
        final LocaTable loca = new LocaTable(dis, length, this.getHeadTable(), this.getMaxpTable());

        // If this is a TrueType outline, then we'll have at least the
        // 'glyf' table (along with the 'loca' table)
        length = seekTable(tableDirectory, dis, tablesOrigin, Table.glyf);
        _glyf = new GlyfTable(dis, length, this.getMaxpTable(), loca);

        length = seekTable(tableDirectory, dis, tablesOrigin, Table.gasp);
        if (length > 0) {
            _gasp = new GaspTable(dis);
        }

        length = seekTable(tableDirectory, dis, tablesOrigin, Table.kern);
        if (length > 0) {
            _kern = new KernTable(dis);
        }

        length = seekTable(tableDirectory, dis, tablesOrigin, Table.hdmx);
        if (length > 0) {
            _hdmx = new HdmxTable(dis, length, this.getMaxpTable());
        }

        length = seekTable(tableDirectory, dis, tablesOrigin, Table.VDMX);
        if (length > 0) {
            _vdmx = new VdmxTable(dis);
        }
    }

    public GlyfTable getGlyfTable() {
        return _glyf;
    }

    public GaspTable getGaspTable() {
        return _gasp;
    }

    public KernTable getKernTable() {
        return _kern;
    }

    public HdmxTable getHdmxTable() {
        return _hdmx;
    }

    public VdmxTable getVdmxTable() {
        return _vdmx;
    }

    @Override
    public Glyph getGlyph(final int i) {
        final GlyfDescript glyfDescr = _glyf.getDescription(i);
        if( null != glyfDescr ) {
            return new TTGlyph(
                    glyfDescr,
                    getHmtxTable().getLeftSideBearing(i),
                    getHmtxTable().getAdvanceWidth(i));
        } else {
            return null;
        }
    }

}

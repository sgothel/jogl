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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import jogamp.graph.font.typecast.ot.table.GaspTable;
import jogamp.graph.font.typecast.ot.table.GlyfDescript;
import jogamp.graph.font.typecast.ot.table.GlyfTable;
import jogamp.graph.font.typecast.ot.table.HdmxTable;
import jogamp.graph.font.typecast.ot.table.KernTable;
import jogamp.graph.font.typecast.ot.table.LocaTable;
import jogamp.graph.font.typecast.ot.table.Table;
import jogamp.graph.font.typecast.ot.table.TableDirectory;
import jogamp.graph.font.typecast.ot.table.VdmxTable;

public class TTFont extends OTFont {

    private final GlyfTable _glyf;
    private GaspTable _gasp;
    private KernTable _kern;
    private HdmxTable _hdmx;
    private VdmxTable _vdmx;

    private static TableDirectory readTableDir(final DataInputStream dis, final int directoryOffset) throws IOException {
        // Load the table directory
        dis.reset(); // throws if not marked or mark not supported
        dis.skip(directoryOffset);
        return new TableDirectory(dis);
    }

    private static DataInputStream openStream(final File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File <"+file.getName()+"> doesn't exist.");
        }
        final int streamLen = (int) file.length();
        final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file), streamLen);
        if( !bis.markSupported() ) {
            throw new IllegalArgumentException("stream of type "+bis.getClass().getName()+" doesn't support mark");
        }
        bis.mark(streamLen);
        return new DataInputStream(bis);
    }

    private static DataInputStream openStream(final InputStream is, final int streamLen) throws IOException {
        final BufferedInputStream bis = new BufferedInputStream(is, streamLen);
        if( !bis.markSupported() ) {
            throw new IllegalArgumentException("stream of type "+is.getClass().getName()+" doesn't support mark");
        }
        bis.mark(streamLen);
        return new DataInputStream(bis);
    }

    /**
     * Constructor
     * @param file standalone font file
     * @param tablesOrigin
     * @throws IOException
     */
    public TTFont(final File file) throws IOException {
        this(openStream(file), 0, 0);
    }

    /**
     * Constructor
     * @param is standalone font input stream
     * @param streamLen length of input stream to rewind across whole font data set
     * @throws IOException
     */
    public TTFont(final InputStream is, final int streamLen) throws IOException {
        this(openStream(is, streamLen), 0, 0);
    }

    /**
     * Constructor
     * @param dis input stream marked at start with read-ahead set to known stream length
     * @param directoryOffset
     * @param tablesOrigin
     * @return
     * @throws IOException
     */
    public TTFont(final DataInputStream dis, final int directoryOffset, final int tablesOrigin) throws IOException {
        this(dis, readTableDir(dis, directoryOffset), tablesOrigin);
    }

    /**
     *
     * @param dis input stream marked at start with read-ahead set to known stream length
     * @param tableDirectory
     * @param tablesOrigin
     * @throws IOException
     */
    TTFont(final DataInputStream dis, final TableDirectory tableDirectory, final int tablesOrigin) throws IOException {
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
    public int getGlyphCount() { return _glyf.getSize(); }

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

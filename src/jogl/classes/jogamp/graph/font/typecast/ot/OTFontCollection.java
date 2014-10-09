/*
 * $Id: OTFontCollection.java,v 1.6 2010-08-10 11:38:11 davidsch Exp $
 *
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004 David Schweinsberg
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

import java.io.File;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;

import jogamp.graph.font.typecast.ot.mac.ResourceHeader;
import jogamp.graph.font.typecast.ot.mac.ResourceMap;
import jogamp.graph.font.typecast.ot.mac.ResourceReference;
import jogamp.graph.font.typecast.ot.mac.ResourceType;
import jogamp.graph.font.typecast.ot.table.DirectoryEntry;
import jogamp.graph.font.typecast.ot.table.TTCHeader;
import jogamp.graph.font.typecast.ot.table.Table;


/**
 *
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: OTFontCollection.java,v 1.6 2010-08-10 11:38:11 davidsch Exp $
 */
public class OTFontCollection {

    private String _pathName;
    private String _fileName;
    private TTCHeader _ttcHeader;
    private OTFont[] _fonts;
    private final ArrayList<Table> _tables = new ArrayList<Table>();
    private boolean _resourceFork = false;

    /** Creates new FontCollection */
    protected OTFontCollection() {
    }

    /**
     * @param file The OpenType font file
     */
    public static OTFontCollection create(final File file) throws IOException {
        final OTFontCollection fc = new OTFontCollection();
        fc.read(file);
        return fc;
    }

    /**
     * @param istream The OpenType font input stream
     * @param streamLen the length of the OpenType font segment in the stream
     */
    public static OTFontCollection create(final InputStream istream, final int streamLen) throws IOException {
        final OTFontCollection fc = new OTFontCollection();
        fc.read(istream, streamLen);
        return fc;
    }

    public String getPathName() {
        return _pathName;
    }

    public String getFileName() {
        return _fileName;
    }

    public OTFont getFont(final int i) {
        return _fonts[i];
    }

    public int getFontCount() {
        return _fonts.length;
    }

    public TTCHeader getTtcHeader() {
        return _ttcHeader;
    }

    public Table getTable(final DirectoryEntry de) {
        for (int i = 0; i < _tables.size(); i++) {
            final Table table = _tables.get(i);
            if ((table.getDirectoryEntry().getTag() == de.getTag()) &&
                (table.getDirectoryEntry().getOffset() == de.getOffset())) {
                return table;
            }
        }
        return null;
    }

    public void addTable(final Table table) {
        _tables.add(table);
    }

    /**
     * @param file The OpenType font file
     */
    protected void read(File file) throws IOException {
        _pathName = file.getPath();
        _fileName = file.getName();

        if (!file.exists()) {
            throw new IOException("File <"+file.getName()+"> doesn't exist.");
        }

        // Do we need to modify the path name to deal with font resources
        // in a Mac resource fork?
        if (file.length() == 0) {
            file = new File(file, "..namedfork/rsrc");
            if (!file.exists()) {
                throw new IOException();
            }
            _resourceFork = true;
        }
        final int streamLen = (int) file.length();
        final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file), streamLen);
        try {
            readImpl(bis, streamLen);
        } finally {
            bis.close();
        }
    }

    /**
     * @param is The OpenType font stream
     * @param streamLen the length of the OpenType font segment in the stream
     */
    protected void read(final InputStream is, final int streamLen) throws IOException {
        _pathName = "";
        _fileName = "";
        final InputStream bis;
        if( is.markSupported() ) {
            bis = is;
        } else {
            bis = new BufferedInputStream(is, streamLen);
        }
        readImpl(bis, streamLen);
    }

    /**
     * @param is The OpenType font stream, must {@link InputStream#markSupported() support mark}!
     */
    private void readImpl(final InputStream bis, final int streamLen) throws IOException {
        if( !bis.markSupported() ) {
            throw new IllegalArgumentException("stream of type "+bis.getClass().getName()+" doesn't support mark");
        }
        bis.mark(streamLen);
        final DataInputStream dis = new DataInputStream(bis);
        if (_resourceFork || _pathName.endsWith(".dfont")) {

            // This is a Macintosh font suitcase resource
            final ResourceHeader resourceHeader = new ResourceHeader(dis);

            // Seek to the map offset and read the map
            dis.reset();
            dis.skip(resourceHeader.getMapOffset());
            final ResourceMap map = new ResourceMap(dis);

            // Get the 'sfnt' resources
            final ResourceType resourceType = map.getResourceType("sfnt");

            // Load the font data
            _fonts = new OTFont[resourceType.getCount()];
            for (int i = 0; i < resourceType.getCount(); i++) {
                final ResourceReference resourceReference = resourceType.getReference(i);
                _fonts[i] = new OTFont(this);
                final int offset = resourceHeader.getDataOffset() +
                        resourceReference.getDataOffset() + 4;
                _fonts[i].read(dis, offset, offset);
            }

        } else if (TTCHeader.isTTC(dis)) {

            // This is a TrueType font collection
            dis.reset();
            _ttcHeader = new TTCHeader(dis);
            _fonts = new OTFont[_ttcHeader.getDirectoryCount()];
            for (int i = 0; i < _ttcHeader.getDirectoryCount(); i++) {
                _fonts[i] = new OTFont(this);
                _fonts[i].read(dis, _ttcHeader.getTableDirectory(i), 0);
            }
        } else {

            // This is a standalone font file
            _fonts = new OTFont[1];
            _fonts[0] = new OTFont(this);
            _fonts[0].read(dis, 0, 0);
        }
    }
}

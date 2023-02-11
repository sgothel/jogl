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
import jogamp.graph.font.typecast.ot.mac.ResourceHeader;
import jogamp.graph.font.typecast.ot.mac.ResourceMap;
import jogamp.graph.font.typecast.ot.mac.ResourceReference;
import jogamp.graph.font.typecast.ot.mac.ResourceType;
import jogamp.graph.font.typecast.ot.table.TTCHeader;


/**
 *
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class OTFontCollection {
    private final boolean DEBUG = false;
    private TTCHeader _ttcHeader;
    private TTFont[] _fonts;
    private String _pathName;
    private String _fileName;
    private boolean _resourceFork = false;

    public String getPathName() {
        return _pathName;
    }

    public String getFileName() {
        return _fileName;
    }

    public TTFont getFont(final int i) {
        return _fonts[i];
    }

    public int getFontCount() {
        return _fonts.length;
    }

    public TTCHeader getTtcHeader() {
        return _ttcHeader;
    }

    /**
     * @param file The OpenType font file
     */
    public OTFontCollection(final File file) throws IOException {
        read(file);
    }

    /**
     * @param istream The OpenType font input stream
     * @param streamLen the length of the OpenType font segment in the stream
     */
    public OTFontCollection(final InputStream istream, final int streamLen) throws IOException {
        read(istream, streamLen);
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
                throw new IOException("File <"+file.getName()+"> doesn't exist.");
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

            if( DEBUG ) {
                // Dump some info about the font suitcase
                for (int i = 0; i < map.getResourceTypeCount(); ++i) {
                    System.err.println(map.getResourceType(i).getTypeAsString());
                }

                final ResourceType type = map.getResourceType("FOND");
                for (int i = 0; i < type.getCount(); ++i) {
                    final ResourceReference reference = type.getReference(i);
                    System.err.println(reference.getName());
                }
            }

            // Get the 'sfnt' resources
            final ResourceType resourceType = map.getResourceType("sfnt");

            // Load the font data
            _fonts = new TTFont[resourceType.getCount()];
            for (int i = 0; i < resourceType.getCount(); i++) {
                final ResourceReference resourceReference = resourceType.getReference(i);
                final int offset = resourceHeader.getDataOffset() +
                                   resourceReference.getDataOffset() + 4;
                _fonts[i] = new TTFont(dis, offset, offset);
            }

        } else if (TTCHeader.isTTC(dis)) {

            // This is a TrueType font collection
            dis.reset();
            _ttcHeader = new TTCHeader(dis);
            _fonts = new TTFont[_ttcHeader.getDirectoryCount()];
            for (int i = 0; i < _ttcHeader.getDirectoryCount(); i++) {
                _fonts[i] = new TTFont(dis, _ttcHeader.getTableDirectory(i), 0);
            }
        } else {

            // This is a standalone font file
            _fonts = new TTFont[1];
            _fonts[0] = new TTFont(dis, 0, 0);

            // TODO T2Fonts
        }
        dis.close();
    }
}

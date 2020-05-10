/*
 * Copyright (c) 2020 Business Operation Systems GmbH. All Rights Reserved.
 */
package jogamp.graph.font.typecast.ot.table;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

/**
 * The SVG (Scalable Vector Graphics) table
 * 
 * <p>
 * This table contains SVG descriptions for some or all of the glyphs in the font.
 * </p>
 * 
 * @see "https://docs.microsoft.com/en-us/typography/opentype/spec/svg"
 * 
 * @author <a href="mailto:bhu@top-logic.com">Bernhard Haumacher</a>
 */
public class SVGTable implements Table {

    /**
     * @see #getVersion()
     */
    public static final int VERSION = 0;
    
    /**
     * @see #getVersion()
     */
    private int _version = VERSION;

    /**
     * @see #getDocumentRecords()
     */
    private SVGDocumentRecord[] _documentRecords;

    /** 
     * Creates a {@link SVGTable}.
     *
     * @param di The reader to read from.
     */
    public SVGTable(DataInput di) throws IOException {
        _version = di.readUnsignedShort();
        
        // Offset to the SVG Document List, from the start of the SVG table.
        // Must be non-zero. 
        int offsetToSVGDocumentList = di.readInt();
        di.skipBytes(offsetToSVGDocumentList - 6);

        // SVG Document List starts here.
        int offset = 0;

        // uint16 Number of SVG document records. Must be non-zero.
        int numEntries = di.readUnsignedShort();
        offset += 2;
        
        _documentRecords = new SVGDocumentRecord[numEntries];
        for (int n = 0; n < numEntries; n++) {
            _documentRecords[n] = new SVGDocumentRecord(di);
        }
        offset += numEntries * 12;
        
        SVGDocumentRecord[] recordsInOffsetOrder = new SVGDocumentRecord[numEntries];
        System.arraycopy(_documentRecords, 0, recordsInOffsetOrder, 0, numEntries);
        Arrays.sort(recordsInOffsetOrder, (a, b) -> Integer.compare(a.getSvgDocOffset(), b.getSvgDocOffset()));
        
        int lastOffset = 0;
        for (int n = 0; n < numEntries; n++) {
            SVGDocumentRecord record = recordsInOffsetOrder[n];
            
            int docOffset = record.getSvgDocOffset();
            if (docOffset == lastOffset) {
                // Pointing to the same document.
                record.setDocument(recordsInOffsetOrder[n - 1].getDocument());
            } else {
                int skip = docOffset - offset;
                assert skip >= 0;
                di.skipBytes(skip);
                offset = docOffset;
            }
            lastOffset = offset;
            record.readDoc(di);
            offset += record.getSvgDocLength();
        }
    }
    
    /**
     * Records must be sorted in order of increasing startGlyphID. For any given
     * record, the startGlyphID must be less than or equal to the endGlyphID of
     * that record, and also must be greater than the endGlyphID of any previous
     * record.
     * 
     * <p>
     * Note: Two or more records can point to the same SVG document. In this
     * way, a single SVG document can provide glyph descriptions for
     * discontinuous glyph ID ranges.
     * </p>
     */
    public SVGDocumentRecord[] getDocumentRecords() {
        return _documentRecords;
    }
    
    /**
     * uint16   version     Table version (starting at 0). Set to {@link #VERSION}.
     */
    public int getVersion() {
        return _version;
    }

    @Override
    public int getType() {
        return 0;
    }
    
    @Override
    public String toString() {
        return "SVG Table\n" +
               "---------\n" + 
               "  Version: " + getVersion() + "\n" +
               "  Number of records: " + getDocumentRecords().length;
    }
    
    @Override
    public void dump(Writer out) throws IOException {
        Table.super.dump(out);
        
        for (SVGDocumentRecord record : getDocumentRecords()) {
            out.write("\n\n");
            out.write(record.toString());
        }
    }

    /**
     * Each SVG document record specifies a range of glyph IDs (from
     * startGlyphID to endGlyphID, inclusive), and the location of its
     * associated SVG document in the SVG table.
     */
    public static class SVGDocumentRecord {

        /**
         * @see #getStartGlyphID()
         */
        private int _startGlyphID;

        /**
         * @see #getEndGlyphID()
         */
        private int _endGlyphID;
        
        /**
         * @see #getSvgDocOffset()
         */
        private int _svgDocOffset;
        
        /**
         * @see #getSvgDocLength()
         */
        private int _svgDocLength;

        private String _document;

        /** 
         * Creates a {@link SVGDocumentRecord}.
         *
         * @param di
         */
        public SVGDocumentRecord(DataInput di) throws IOException {
            _startGlyphID = di.readUnsignedShort();
            _endGlyphID = di.readUnsignedShort();
            _svgDocOffset = di.readInt();
            _svgDocLength = di.readInt();
        }
        
        /** 
         * Reads the SVG document from the given reader.
         */
        public void readDoc(DataInput di) throws IOException {
            byte[] docData = new byte[getSvgDocLength()];
            di.readFully(docData);
            
            if (docData[0] == 0x1F && docData[1] == 0x8B && docData[2] == 0x08) {
                // Gzip encoded document.
                try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(docData))) {
                    readDoc(in);
                }
            } else {
                try (InputStream in = new ByteArrayInputStream(docData)) {
                    readDoc(in);
                }
            }
        }

        private void readDoc(InputStream in) throws IOException {
            StringBuilder doc = new StringBuilder();
            char[] buffer = new char[4096];
            try (InputStreamReader r = new InputStreamReader(in, Charset.forName("utf-8"))) {
                while (true) {
                    int direct = r.read(buffer);
                    if (direct < 0) {
                        break;
                    }
                    doc.append(buffer, 0, direct);
                }
            }
            
            _document = doc.toString();
        }

        /**
         * uint16
         * 
         * The first glyph ID for the range covered by this record.
         */
        public int getStartGlyphID() {
            return _startGlyphID;
        }
        
        /**
         * uint16
         * 
         * The last glyph ID for the range covered by this record.
         */
        public int getEndGlyphID() {
            return _endGlyphID;
        }
        
        /**
         * Offset32 
         * 
         * Offset from the beginning of the SVGDocumentList to an SVG document. Must be non-zero.
         */
        public int getSvgDocOffset() {
            return _svgDocOffset;
        }
        
        /**
         * uint32
         * 
         * Length of the SVG document data. Must be non-zero.
         */
        public int getSvgDocLength() {
            return _svgDocLength;
        }
        
        /**
         * The SVG document as XML string.
         */
        public String getDocument() {
            return _document;
        }

        /** 
         * @see #getDocument()
         */
        public void setDocument(String document) {
            _document = document;
        }
        
        @Override
        public String toString() {
            return 
                "    SVG document record\n" + 
                "    -------------------\n" + 
                "      startGlyphID: " + getStartGlyphID() + "\n" +
                "      endGlyphID:   " + getEndGlyphID() + "\n" + 
                "      svg:          " + getDocument();
        }
    }
}

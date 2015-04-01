/**
 * Original JavaScript code from <https://github.com/notmasteryet/jpgjs/blob/master/jpg.js>,
 * ported to Java for JogAmp Community.
 *
 * Enhancements:
 *  * InputStream instead of memory buffer
 *  * User provided memory handler
 *  * Fixed JPEG Component ID/Index mapping
 *  * Color space conversion (YCCK, CMYK -> RGB)
 *  * More error tolerant
 *
 * *****************
 *
 * Copyright 2011 notmasteryet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *****************
 *
 * Copyright 2013 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package jogamp.opengl.util.jpeg;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import jogamp.opengl.Debug;

import com.jogamp.common.util.ArrayHashSet;
import com.jogamp.common.util.Bitstream;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureData.ColorSpace;

/**
 *
 * <ul>
 *   <li> The JPEG specification can be found in the ITU CCITT Recommendation T.81
 *        (www.w3.org/Graphics/JPEG/itu-t81.pdf) </li>
 *   <li> The JFIF specification can be found in the JPEG File Interchange Format
 *        (www.w3.org/Graphics/JPEG/jfif3.pdf)</li>
 *   <li> The Adobe Application-Specific JPEG markers in the Supporting the DCT Filters
 *        in PostScript Level 2, Technical Note #5116
 *        (partners.adobe.com/public/developer/en/ps/sdk/5116.DCT_Filter.pdf)</li>
 *   <li> http://halicery.com/jpeg/huffman.html </li>
 *   <li> https://en.wikipedia.org/wiki/Jpg#Syntax_and_structure </li>
 *   <li> http://www.cs.sfu.ca/CourseCentral/365/mark/material/notes/Chap4/Chap4.2/Chap4.2.html </li>
 *   <li> https://github.com/notmasteryet/jpgjs/blob/master/jpg.js </li>
 * </ul>
 */
public class JPEGDecoder {
    private static final boolean DEBUG = Debug.debug("JPEGImage");
    private static final boolean DEBUG_IN = false;

    /** Allows user to hook a {@link ColorSink} to another toolkit to produce {@link TextureData}. */
    public static interface ColorSink {
        /**
         * @param width
         * @param height
         * @param sourceCS the color-space of the decoded JPEG
         * @param sourceComponents number of components used for the given source color-space
         * @return Either {@link TextureData.ColorSpace#RGB} or {@link TextureData.ColorSpace#YCbCr}. {@link TextureData.ColorSpace#YCCK} and {@link TextureData.ColorSpace#CMYK} will throw an exception!
         * @throws RuntimeException
         */
        public TextureData.ColorSpace allocate(int width, int height, TextureData.ColorSpace sourceCS, int sourceComponents) throws RuntimeException;
        public void store2(int x, int y, byte c1, byte c2);
        public void storeRGB(int x, int y, byte r, byte g, byte b);
        public void storeYCbCr(int x, int y, byte Y, byte Cb, byte Cr);
    }

    public static class JFIF {
        final VersionNumber version;
        final int densityUnits;
        final int xDensity;
        final int yDensity;
        final int thumbWidth;
        final int thumbHeight;
        final byte[] thumbData;

        private JFIF(final byte data[]) {
            version = new VersionNumber(data[5], data[6], 0);
            densityUnits = data[7];
            xDensity = ((data[ 8] << 8) & 0xff00) | (data[ 9] & 0xff);
            yDensity = ((data[10] << 8) & 0xff00) | (data[11] & 0xff);
            thumbWidth = data[12];
            thumbHeight = data[13];
            if( 0 < thumbWidth && 0 < thumbHeight ) {
                final int len = 14 + 3 * thumbWidth * thumbHeight;
                thumbData = new byte[len];
                System.arraycopy(data, 14, thumbData, 0, len);
            } else {
                thumbData = null;
            }
        }

        public static final JFIF get(final byte[] data) throws RuntimeException {
            if ( data[0] == (byte)0x4A && data[1] == (byte)0x46 && data[2] == (byte)0x49 &&
                 data[3] == (byte)0x46 && data[4] == (byte)0x0) { // 'JFIF\x00'
                final JFIF r = new JFIF(data);
                return r;
            } else {
                return null;
            }
        }

        @Override
        public final String toString() {
            return "JFIF[ver "+version+", density[units "+densityUnits+", "+xDensity+"x"+yDensity+"], thumb "+thumbWidth+"x"+thumbHeight+"]";
        }
    }

    public static class Adobe {
        final short version;
        final short flags0;
        final short flags1;
        final short colorCode;
        final ColorSpace colorSpace;

        private Adobe(final byte[] data) {
            version = data[6];
            flags0 = (short)(((data[7] << 8) & 0xff00) | (data[ 8] & 0xff));
            flags1 = (short)(((data[9] << 8) & 0xff00) | (data[10] & 0xff));
            colorCode = data[11];
            switch( colorCode ) {
                case 2: colorSpace = ColorSpace.YCCK; break;
                case 1: colorSpace = ColorSpace.YCbCr; break;
                default: colorSpace = ColorSpace.CMYK; break;
            }
        }
        public static final Adobe get(final byte[] data) throws RuntimeException {
            if (data[0] == (byte)0x41 && data[1] == (byte)0x64 && data[2] == (byte)0x6F &&
                data[3] == (byte)0x62 && data[4] == (byte)0x65 && data[5] == (byte)0) { // 'Adobe\x00'
                final Adobe r = new Adobe(data);
                return r;
            } else {
                return null;
            }
        }
        @Override
        public final String toString() {
            return "Adobe[ver "+version+", flags["+toHexString(flags0)+", "+toHexString(flags1)+"], colorSpace/Code "+colorSpace+"/"+toHexString(colorCode)+"]";
        }
    }
    /** TODO */
    public static class EXIF {
        private EXIF(final byte data[]) {
        }

        public static final EXIF get(final byte[] data) throws RuntimeException {
            if ( data[0] == (byte)0x45 && data[1] == (byte)0x78 && data[2] == (byte)0x69 &&
                 data[3] == (byte)0x66 && data[4] == (byte)0x0) { // 'Exif\x00'
                final EXIF r = new EXIF(data);
                return r;
            } else {
                return null;
            }
        }
        @Override
        public final String toString() {
            return "EXIF[]";
        }
    }

    @SuppressWarnings("serial")
    public static class CodecException extends RuntimeException {
        CodecException(final String message) {
            super(message);
        }
    }
    @SuppressWarnings("serial")
    public static class MarkerException extends CodecException {
        final int marker;
        MarkerException(final int marker, final String message) {
            super(message+" - Marker "+toHexString(marker));
            this.marker = marker;
        }
        public int getMarker() { return marker; }
    }

    /** Start of Image */
    private static final int M_SOI   = 0xFFD8;
    /** End of Image */
    private static final int M_EOI   = 0xFFD9;
    /** Start of Frame - Baseline DCT */
    private static final int M_SOF0  = 0xFFC0;
    /** Start of Frame - Extended sequential DCT */
    // private static final int M_SOF1  = 0xFFC1;
    /** Start of Frame - Progressive DCT */
    private static final int M_SOF2  = 0xFFC2;
    /** DHT (Define Huffman Tables) */
    private static final int M_DHT   = 0xFFC4;
    // private static final int M_DAC   = 0xFFCC;
    /** SOS (Start of Scan) */
    private static final int M_SOS   = 0xFFDA;
    /** DQT (Define Quantization Tables) */
    private static final int M_QTT   = 0xFFDB;
    /** DRI (Define Restart Interval) */
    private static final int M_DRI   = 0xFFDD;
    /** APP0 (Application Specific) - JFIF Header */
    private static final int M_APP00 = 0xFFE0;
    /** APP1 (Application Specific) - Exif Header */
    private static final int M_APP01 = 0xFFE1;
    /** APP2 (Application Specific) */
    private static final int M_APP02 = 0xFFE2;
    /** APP3 (Application Specific) */
    private static final int M_APP03 = 0xFFE3;
    /** APP4 (Application Specific) */
    private static final int M_APP04 = 0xFFE4;
    /** APP5 (Application Specific) */
    private static final int M_APP05 = 0xFFE5;
    /** APP6 (Application Specific) */
    private static final int M_APP06 = 0xFFE6;
    /** APP7 (Application Specific) */
    private static final int M_APP07 = 0xFFE7;
    /** APP8 (Application Specific) */
    private static final int M_APP08 = 0xFFE8;
    /** APP9 (Application Specific) */
    private static final int M_APP09 = 0xFFE9;
    /** APP10 (Application Specific) */
    private static final int M_APP10 = 0xFFEA;
    /** APP11 (Application Specific) */
    private static final int M_APP11 = 0xFFEB;
    /** APP12 (Application Specific) */
    private static final int M_APP12 = 0xFFEC;
    /** APP13 (Application Specific) */
    private static final int M_APP13 = 0xFFED;
    /** APP14 (Application Specific) - ADOBE Header */
    private static final int M_APP14 = 0xFFEE;
    /** APP15 (Application Specific) */
    private static final int M_APP15 = 0xFFEF;

    /** Annotation / Comment */
    private static final int M_ANO   = 0xFFFE;

    static final int[] dctZigZag = new int[] {
        0,
        1,  8,
        16,  9,  2,
        3, 10, 17, 24,
        32, 25, 18, 11, 4,
        5, 12, 19, 26, 33, 40,
        48, 41, 34, 27, 20, 13,  6,
        7, 14, 21, 28, 35, 42, 49, 56,
        57, 50, 43, 36, 29, 22, 15,
        23, 30, 37, 44, 51, 58,
        59, 52, 45, 38, 31,
        39, 46, 53, 60,
        61, 54, 47,
        55, 62,
        63
    };

    static final int dctCos1  =   4017;   // cos(pi/16)
    static final int dctSin1  =    799;   // sin(pi/16)
    static final int dctCos3  =   3406;   // cos(3*pi/16)
    static final int dctSin3  =   2276;   // sin(3*pi/16)
    static final int dctCos6  =   1567;   // cos(6*pi/16)
    static final int dctSin6  =   3784;   // sin(6*pi/16)
    static final int dctSqrt2 =   5793;   // sqrt(2)
    static final int dctSqrt1d2 = 2896;   // sqrt(2) / 2

    static class Frame {
        final boolean progressive;
        final int precision;
        final int scanLines;
        final int samplesPerLine;
        private final ArrayHashSet<Integer> compIDs;
        private final ComponentIn[] comps;
        private final int compCount;
        /** quantization tables */
        final int[][] qtt;
        int maxCompID;
        int maxH;
        int maxV;
        int mcusPerLine;
        int mcusPerColumn;

        Frame(final boolean progressive, final int precision, final int scanLines, final int samplesPerLine, final int componentsCount, final int[][] qtt) {
            this.progressive = progressive;
            this.precision = precision;
            this.scanLines = scanLines;
            this.samplesPerLine = samplesPerLine;
            compIDs = new ArrayHashSet<Integer>(false, componentsCount, ArrayHashSet.DEFAULT_LOAD_FACTOR);
            comps = new ComponentIn[componentsCount];
            this.compCount = componentsCount;
            this.qtt = qtt;
        }

        private final void checkBounds(final int idx) {
            if( 0 > idx || idx >= compCount ) {
                throw new CodecException("Idx out of bounds "+idx+", "+this);
            }
        }
        public final void validateComponents() {
            for(int i=0; i<compCount; i++) {
                final ComponentIn c = comps[i];
                if( null == c ) {
                    throw new CodecException("Component["+i+"] null");
                }
                if( null == this.qtt[c.qttIdx] ) {
                    throw new CodecException("Component["+i+"].qttIdx -> null QTT");
                }
            }
        }

        public final int getCompCount() { return compCount; }
        public final int getMaxCompID() { return maxCompID; }

        public final void putOrdered(final int compID, final ComponentIn component) {
            if( maxCompID < compID ) {
                maxCompID = compID;
            }
            final int idx = compIDs.size();
            checkBounds(idx);
            compIDs.add(compID);
            comps[idx] = component;
        }
        public final ComponentIn getCompByIndex(final int i) {
            checkBounds(i);
            return comps[i];
        }
        public final ComponentIn getCompByID(final int componentID) {
            return getCompByIndex( compIDs.indexOf(componentID) );
        }
        public final int getCompID(final int idx) {
            return compIDs.get(idx);
        }
        public final boolean hasCompID(final int componentID) {
            return compIDs.contains(componentID);
        }
        @Override
        public final String toString() {
            return "Frame[progressive "+progressive+", precision "+precision+", scanLines "+scanLines+", samplesPerLine "+samplesPerLine+
                    ", components[count "+compCount+", maxID "+maxCompID+", componentIDs "+compIDs+", comps "+Arrays.asList(comps)+"]]";
        }
    }

    /** The JPEG encoded components */
    static class ComponentIn {
        final int h, v;
        /** index to frame.qtt[] */
        final int qttIdx;
        int blocksPerColumn;
        int blocksPerColumnForMcu;
        int blocksPerLine;
        int blocksPerLineForMcu;
        /** [blocksPerColumnForMcu][blocksPerLineForMcu][64]; */
        int[][][] blocks;
        int pred;
        BinObj huffmanTableAC;
        BinObj huffmanTableDC;

        ComponentIn(final int h, final int v, final int qttIdx) {
            this.h = h;
            this.v = v;
            this.qttIdx = qttIdx;
        }

        public final void allocateBlocks(final int blocksPerColumn, final int blocksPerColumnForMcu, final int blocksPerLine, final int blocksPerLineForMcu) {
            this.blocksPerColumn = blocksPerColumn;
            this.blocksPerColumnForMcu = blocksPerColumnForMcu;
            this.blocksPerLine = blocksPerLine;
            this.blocksPerLineForMcu = blocksPerLineForMcu;
            this.blocks = new int[blocksPerColumnForMcu][blocksPerLineForMcu][64];
        }
        public final int[] getBlock(final int row, final int col) {
            if( row >= blocksPerColumnForMcu || col >= blocksPerLineForMcu ) {
                throw new CodecException("Out of bounds given ["+row+"]["+col+"] - "+this);
            }
            return blocks[row][col];
        }

        @Override
        public final String toString() {
            return "CompIn[h "+h+", v "+v+", qttIdx "+qttIdx+", blocks["+blocksPerColumn+", mcu "+blocksPerColumnForMcu+"]["+blocksPerLine+", mcu "+blocksPerLineForMcu+"][64]]";
        }
    }

    /** The decoded components */
    static class ComponentOut {
        private final ArrayList<byte[]> lines;
        final float scaleX;
        final float scaleY;

        ComponentOut(final ArrayList<byte[]> lines, final float scaleX, final float scaleY) {
            this.lines = lines;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
        }

        /** Safely returning a line, if index exceeds number of lines, last line is returned. */
        public final byte[] getLine(final int i) {
            final int sz = lines.size();
            return lines.get( i < sz ? i : sz - 1);
        }

        @Override
        public final String toString() {
            return "CompOut[lines "+lines.size()+", scale "+scaleX+"x"+scaleY+"]";
        }
    }

    @Override
    public String toString() {
        final String jfifS = null != jfif ? jfif.toString() : "JFIF nil";
        final String exifS = null != exif ? exif.toString() : "Exif nil";
        final String adobeS = null != adobe ? adobe.toString() : "Adobe nil";
        final String compOuts = null != components ? Arrays.asList(components).toString() : "nil";
        return "JPEG[size "+width+"x"+height+", compOut "+compOuts+", "+jfifS+", "+exifS+", "+adobeS+"]";
    }

    private final Bitstream<InputStream> bstream = new Bitstream<InputStream>(new Bitstream.ByteInputStream(null), false /* outputMode */);

    private int width = 0;
    private int height = 0;
    private JFIF jfif = null;
    private EXIF exif = null;
    private Adobe adobe = null;
    private ComponentOut[] components = null;

    public final JFIF getJFIFHeader() { return jfif; }
    public final EXIF getEXIFHeader() { return exif; }
    public final Adobe getAdobeHeader() { return adobe; }
    public final int getWidth() { return width; }
    public final int getHeight() { return height; }

    private final void setStream(final InputStream is) {
        try {
            bstream.setStream(is, false /* outputMode */);
        } catch (final Exception e) {
            throw new RuntimeException(e); // should not happen, no flush()
        }
    }

    private final int readUInt8() throws IOException {
        return bstream.readUInt8();
    }

    private final int readUInt16() throws IOException {
        return bstream.readUInt16(true /* bigEndian */);
    }

    private final int readNumber() throws IOException {
        final int len=readUInt16();
        if(len!=4){
            throw new CodecException("ERROR: Define number format error [Len!=4, but "+len+"]");
        }
        return readUInt16();
    }

    private final byte[] readDataBlock() throws IOException {
        int count=0, i=0;
        final int len=readUInt16();   count+=2;
        final byte[] data = new byte[len-2];
        while(count<len){
            data[i++] = (byte)readUInt8(); count++;
        }
        if(DEBUG_IN) { System.err.println("JPEG.readDataBlock: net-len "+(len-2)+", "+this); dumpData(data, 0, len-2); }
        return data;
    }
    static final void dumpData(final byte[] data, final int offset, final int len) {
        for(int i=0; i<len; ) {
            System.err.print(i%8+": ");
            for(int j=0; j<8 && i<len; j++, i++) {
                System.err.print(toHexString(0x000000FF & data[offset+i])+", ");
            }
            System.err.println("");
        }
    }

    public synchronized void clear(final InputStream inputStream) {
        setStream(inputStream);
        width = 0;
        height = 0;
        jfif = null;
        exif = null;
        adobe = null;
        components = null;
    }
    public synchronized JPEGDecoder parse(final InputStream inputStream) throws IOException {
        clear(inputStream);

        final int[][] quantizationTables = new int[0x0F][]; // 4 bits
        final BinObj[] huffmanTablesAC = new BinObj[0x0F]; // Huffman table spec - 4 bits
        final BinObj[] huffmanTablesDC = new BinObj[0x0F]; // Huffman table spec - 4 bits
        // final ArrayList<Frame> frames = new ArrayList<Frame>(); // JAU: max 1-frame

        Frame frame = null;
        int resetInterval = 0;
        int fileMarker = readUInt16();
        if ( fileMarker != M_SOI ) {
            throw new CodecException("SOI not found, but has marker "+toHexString(fileMarker));
        }

        fileMarker = readUInt16();
        while (fileMarker != M_EOI) {
            if(DEBUG) { System.err.println("JPG.parse got marker "+toHexString(fileMarker)); }
            switch(fileMarker) {
            case M_APP00:
            case M_APP01:
            case M_APP02:
            case M_APP03:
            case M_APP04:
            case M_APP05:
            case M_APP06:
            case M_APP07:
            case M_APP08:
            case M_APP09:
            case M_APP10:
            case M_APP11:
            case M_APP12:
            case M_APP13:
            case M_APP14:
            case M_APP15:
            case M_ANO: {
                final byte[] appData = readDataBlock();

                if ( fileMarker == M_APP00 ) {
                    jfif = JFIF.get( appData );
                }
                if ( fileMarker == M_APP01 ) {
                    exif = EXIF.get(appData);
                }
                if (fileMarker == M_APP14) {
                    adobe = Adobe.get(appData);
                }
                fileMarker = 0; // consumed and get-next
            }
            break;

            case M_QTT: {
                int count = 0;
                final int quantizationTablesLength = readUInt16(); count+=2;
                while( count < quantizationTablesLength ) {
                    final int quantizationTableSpec = readUInt8(); count++;
                    final int precisionID = quantizationTableSpec >> 4;
                    final int tableIdx = quantizationTableSpec & 0x0F;
                    final int[] tableData = new int[64];
                    if ( precisionID == 0 ) { // 8 bit values
                        for (int j = 0; j < 64; j++) {
                            final int z = dctZigZag[j];
                            tableData[z] = readUInt8(); count++;
                        }
                    } else if ( precisionID == 1) { //16 bit
                        for (int j = 0; j < 64; j++) {
                            final int z = dctZigZag[j];
                            tableData[z] = readUInt16(); count+=2;
                        }
                    } else {
                        throw new CodecException("DQT: invalid table precision "+precisionID+", quantizationTableSpec "+quantizationTableSpec+", idx "+tableIdx);
                    }
                    quantizationTables[tableIdx] = tableData;
                    if( DEBUG ) {
                        System.err.println("JPEG.parse.QTT["+tableIdx+"]: spec "+quantizationTableSpec+", precision "+precisionID+", data "+count+"/"+quantizationTablesLength);
                    }
                }
                if(count!=quantizationTablesLength){
                    throw new CodecException("ERROR: QTT format error [count!=Length]: "+count+"/"+quantizationTablesLength);
                }
                fileMarker = 0; // consumed and get-next
            }
            break;

            case M_SOF0:
            case M_SOF2: {
                if( null != frame ) { // JAU: max 1-frame
                    throw new CodecException("only single frame JPEGs supported");
                }
                int count = 0;
                final int sofLen = readUInt16(); count+=2; // header length;
                final int componentsCount;
                {
                    final boolean progressive = (fileMarker == M_SOF2);
                    final int precision = readUInt8(); count++;
                    final int scanLines = readUInt16(); count+=2;
                    final int samplesPerLine = readUInt16(); count+=2;
                    componentsCount = readUInt8(); count++;
                    frame = new Frame(progressive, precision, scanLines, samplesPerLine, componentsCount, quantizationTables);
                    width = frame.samplesPerLine;
                    height = frame.scanLines;
                }
                for (int i = 0; i < componentsCount; i++) {
                    final int componentId = readUInt8(); count++;
                    final int temp = readUInt8(); count++;
                    final int h = temp >> 4;
                    final int v = temp & 0x0F;
                    final int qttIdx = readUInt8(); count++;
                    final ComponentIn compIn = new ComponentIn(h, v, qttIdx);
                    frame.putOrdered(componentId, compIn);
                }
                if(count!=sofLen){
                    throw new CodecException("ERROR: SOF format error [count!=Length]");
                }
                prepareComponents(frame);
                // frames.add(frame); // JAU: max 1-frame
                if(DEBUG) { System.err.println("JPG.parse.SOF[02]: Got frame "+frame); }
                fileMarker = 0; // consumed and get-next
            }
            break;

            case M_DHT: {
                int count = 0;
                final int huffmanLength = readUInt16(); count+=2;
                int i=count, codeLengthTotal = 0;
                while( i < huffmanLength ) {
                    final int huffmanTableSpec = readUInt8(); count++;
                    final int[] codeLengths = new int[16];
                    int codeLengthSum = 0;
                    for (int j = 0; j < 16; j++) {
                        codeLengthSum += (codeLengths[j] = readUInt8()); count++;
                    }
                    final byte[] huffmanValues = new byte[codeLengthSum];
                    for (int j = 0; j < codeLengthSum; j++) {
                        huffmanValues[j] = (byte)readUInt8(); count++;
                    }
                    codeLengthTotal += codeLengthSum;
                    i += 17 + codeLengthSum;
                    final BinObj[] table = ( huffmanTableSpec >> 4 ) == 0 ? huffmanTablesDC : huffmanTablesAC;
                    table[huffmanTableSpec & 0x0F] = buildHuffmanTable(codeLengths, huffmanValues);
                }
                if(count!=huffmanLength || i!=count){
                    throw new CodecException("ERROR: Huffman table format error [count!=Length]");
                }
                if(DEBUG) { System.err.println("JPG.parse.DHT: Got Huffman CodeLengthTotal "+codeLengthTotal); }
                fileMarker = 0; // consumed and get-next
            }
            break;

            case M_DRI:
                resetInterval = readNumber();
                if(DEBUG) { System.err.println("JPG.parse.DRI: Got Reset Interval "+resetInterval); }
                fileMarker = 0; // consumed and get-next
                break;

            case M_SOS: {
                int count = 0;
                final int sosLen = readUInt16(); count+=2;
                final int selectorsCount = readUInt8(); count++;
                final ArrayList<ComponentIn> components = new ArrayList<ComponentIn>();
                if(DEBUG) { System.err.println("JPG.parse.SOS: selectorCount [0.."+(selectorsCount-1)+"]: "+frame); }
                for (int i = 0; i < selectorsCount; i++) {
                    final int compID = readUInt8(); count++;
                    final ComponentIn component = frame.getCompByID(compID);
                    final int tableSpec = readUInt8(); count++;
                    component.huffmanTableDC = huffmanTablesDC[tableSpec >> 4];
                    component.huffmanTableAC = huffmanTablesAC[tableSpec & 15];
                    components.add(component);
                }
                final int spectralStart = readUInt8(); count++;
                final int spectralEnd = readUInt8(); count++;
                final int successiveApproximation = readUInt8(); count++;
                if(count!=sosLen){
                    throw new CodecException("ERROR: scan header format error [count!=Length]");
                }
                fileMarker = decoder.decodeScan(frame, components, resetInterval,
                                                spectralStart, spectralEnd,
                                                successiveApproximation >> 4, successiveApproximation & 15);
                if(DEBUG) { System.err.println("JPG.parse.SOS.decode result "+toHexString(fileMarker)); }
            }
            break;
            default:
                /**
                if (data[offset - 3] == 0xFF &&
                data[offset - 2] >= 0xC0 && data[offset - 2] <= 0xFE) {
                    // could be incorrect encoding -- last 0xFF byte of the previous
                    // block was eaten by the encoder
                    offset -= 3;
                    break;
                } */
                throw new CodecException("unknown JPEG marker " + toHexString(fileMarker) + ", " + bstream);
            }
            if( 0 == fileMarker ) {
                fileMarker = readUInt16();
            }
        }
        if(DEBUG) { System.err.println("JPG.parse.2: End of parsing input "+this); }
        /** // JAU: max 1-frame
        if ( frames.size() != 1 ) {
            throw new CodecException("only single frame JPEGs supported "+this);
        } */
        if( null == frame ) {
            throw new CodecException("no single frame found in stream "+this);
        }
        frame.validateComponents();

        final int compCount = frame.getCompCount();
        this.components = new ComponentOut[compCount];
        for (int i = 0; i < compCount; i++) {
            final ComponentIn component = frame.getCompByIndex(i);
            // System.err.println("JPG.parse.buildComponentData["+i+"]: "+component); // JAU
            // System.err.println("JPG.parse.buildComponentData["+i+"]: "+frame); // JAU
            this.components[i] = new ComponentOut( output.buildComponentData(frame, component),
                                                   (float)component.h / (float)frame.maxH,
                                                   (float)component.v / (float)frame.maxV );
        }
        if(DEBUG) { System.err.println("JPG.parse.X: End of processing input "+this); }
        return this;
    }

    private void prepareComponents(final Frame frame) {
        int maxH = 0, maxV = 0;
        // for (componentId in frame.components) {
        final int compCount = frame.getCompCount();
        for (int i=0; i<compCount; i++) {
            final ComponentIn component = frame.getCompByIndex(i);
            if (maxH < component.h) maxH = component.h;
            if (maxV < component.v) maxV = component.v;
        }
        final int mcusPerLine = (int) Math.ceil(frame.samplesPerLine / 8f / maxH);
        final int mcusPerColumn = (int) Math.ceil(frame.scanLines / 8f / maxV);
        // for (componentId in frame.components) {
        for (int i=0; i<compCount; i++) {
            final ComponentIn component = frame.getCompByIndex(i);
            final int blocksPerLine = (int) Math.ceil(Math.ceil(frame.samplesPerLine / 8f) * component.h / maxH);
            final int blocksPerColumn = (int) Math.ceil(Math.ceil(frame.scanLines  / 8f) * component.v / maxV);
            final int blocksPerLineForMcu = mcusPerLine * component.h;
            final int blocksPerColumnForMcu = mcusPerColumn * component.v;
            component.allocateBlocks(blocksPerColumn, blocksPerColumnForMcu, blocksPerLine, blocksPerLineForMcu);
        }
        frame.maxH = maxH;
        frame.maxV = maxV;
        frame.mcusPerLine = mcusPerLine;
        frame.mcusPerColumn = mcusPerColumn;
    }

    static class BinObjIdxed {
        final BinObj children;
        byte index;
        BinObjIdxed() {
            this.children = new BinObj();
            this.index = 0;
        }
    }
    static class BinObj {
        final boolean isValue;
        final BinObj[] tree;
        final byte b;

        BinObj(final byte b) {
            this.isValue= true;
            this.b = b;
            this.tree = null;
        }
        BinObj() {
            this.isValue= false;
            this.b = (byte)0;
            this.tree = new BinObj[2];
        }
        final byte getValue() { return b; }
        final BinObj get(final int i) { return tree[i]; }
        final void set(final byte i, final byte v) { tree[i] = new BinObj(v); }
        final void set(final byte i, final BinObj v) { tree[i] = v; }
    }

    private BinObj buildHuffmanTable(final int[] codeLengths, final byte[] values) {
        int k = 0;
        int length = 16;
        final ArrayList<BinObjIdxed> code = new ArrayList<BinObjIdxed>();
        while (length > 0 && 0==codeLengths[length - 1]) {
            length--;
        }
        code.add(new BinObjIdxed());
        BinObjIdxed p = code.get(0), q;
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < codeLengths[i]; j++) {
                p = code.remove(code.size()-1);
                p.children.set(p.index, values[k]);
                while (p.index > 0) {
                    p = code.remove(code.size()-1);
                }
                p.index++;
                code.add(p);
                while (code.size() <= i) {
                    q = new BinObjIdxed();
                    code.add(q);
                    p.children.set(p.index, q.children);
                    p = q;
                }
                k++;
            }
            if (i + 1 < length) {
                // p here points to last code
                q = new BinObjIdxed();
                code.add(q);
                p.children.set(p.index, q.children);
                p = q;
            }
        }
        return code.get(0).children;
    }

    private final Output output = new Output();
    static class Output {
        private int blocksPerLine;
        private int blocksPerColumn;
        private int samplesPerLine;

        private ArrayList<byte[]> buildComponentData(final Frame frame, final ComponentIn component) {
            final ArrayList<byte[]> lines = new ArrayList<byte[]>();
            blocksPerLine = component.blocksPerLine;
            blocksPerColumn = component.blocksPerColumn;
            samplesPerLine = blocksPerLine << 3;
            final int[] R = new int[64];
            final byte[] r = new byte[64];

            for (int blockRow = 0; blockRow < blocksPerColumn; blockRow++) {
                final int scanLine = blockRow << 3;
                // System.err.println("JPG.buildComponentData: row "+blockRow+"/"+blocksPerColumn+" -> scanLine "+scanLine); // JAU
                for (int i = 0; i < 8; i++) {
                    lines.add(new byte[samplesPerLine]);
                }
                for (int blockCol = 0; blockCol < blocksPerLine; blockCol++) {
                    // System.err.println("JPG.buildComponentData: col "+blockCol+"/"+blocksPerLine+", comp.qttIdx "+component.qttIdx+", qtt "+frame.qtt[component.qttIdx]); // JAU
                    quantizeAndInverse(component.getBlock(blockRow, blockCol), r, R, frame.qtt[component.qttIdx]);

                    final int sample = blockCol << 3;
                    int offset = 0;
                    for (int j = 0; j < 8; j++) {
                        final byte[] line = lines.get(scanLine + j);
                        for (int i = 0; i < 8; i++)
                            line[sample + i] = r[offset++];
                    }
                }
            }
            return lines;
        }

        // A port of poppler's IDCT method which in turn is taken from:
        //   Christoph Loeffler, Adriaan Ligtenberg, George S. Moschytz,
        //   "Practical Fast 1-D DCT Algorithms with 11 Multiplications",
        //   IEEE Intl. Conf. on Acoustics, Speech & Signal Processing, 1989,
        //   988-991.
        private void quantizeAndInverse(final int[] zz, final byte[] dataOut, final int[] dataIn, final int[] qt) {
            int v0, v1, v2, v3, v4, v5, v6, v7, t;
            final int[] p = dataIn;
            int i;

            // dequant
            for (i = 0; i < 64; i++) {
                p[i] = zz[i] * qt[i];
            }

            // inverse DCT on rows
            for (i = 0; i < 8; ++i) {
                final int row = 8 * i;

                // check for all-zero AC coefficients
                if (p[1 + row] == 0 && p[2 + row] == 0 && p[3 + row] == 0 &&
                        p[4 + row] == 0 && p[5 + row] == 0 && p[6 + row] == 0 &&
                        p[7 + row] == 0) {
                    t = (dctSqrt2 * p[0 + row] + 512) >> 10;
                    p[0 + row] = t;
                    p[1 + row] = t;
                    p[2 + row] = t;
                    p[3 + row] = t;
                    p[4 + row] = t;
                    p[5 + row] = t;
                    p[6 + row] = t;
                    p[7 + row] = t;
                    continue;
                }

                // stage 4
                v0 = (dctSqrt2 * p[0 + row] + 128) >> 8;
                        v1 = (dctSqrt2 * p[4 + row] + 128) >> 8;
                v2 = p[2 + row];
                v3 = p[6 + row];
                v4 = (dctSqrt1d2 * (p[1 + row] - p[7 + row]) + 128) >> 8;
                v7 = (dctSqrt1d2 * (p[1 + row] + p[7 + row]) + 128) >> 8;
                v5 = p[3 + row] << 4;
                v6 = p[5 + row] << 4;

                // stage 3
                t = (v0 - v1+ 1) >> 1;
                v0 = (v0 + v1 + 1) >> 1;
                v1 = t;
                t = (v2 * dctSin6 + v3 * dctCos6 + 128) >> 8;
                v2 = (v2 * dctCos6 - v3 * dctSin6 + 128) >> 8;
                v3 = t;
                t = (v4 - v6 + 1) >> 1;
                v4 = (v4 + v6 + 1) >> 1;
                v6 = t;
                t = (v7 + v5 + 1) >> 1;
                v5 = (v7 - v5 + 1) >> 1;
                v7 = t;

                // stage 2
                t = (v0 - v3 + 1) >> 1;
                v0 = (v0 + v3 + 1) >> 1;
                v3 = t;
                t = (v1 - v2 + 1) >> 1;
                v1 = (v1 + v2 + 1) >> 1;
                v2 = t;
                t = (v4 * dctSin3 + v7 * dctCos3 + 2048) >> 12;
                v4 = (v4 * dctCos3 - v7 * dctSin3 + 2048) >> 12;
                v7 = t;
                t = (v5 * dctSin1 + v6 * dctCos1 + 2048) >> 12;
                v5 = (v5 * dctCos1 - v6 * dctSin1 + 2048) >> 12;
                v6 = t;

                // stage 1
                p[0 + row] = v0 + v7;
                p[7 + row] = v0 - v7;
                p[1 + row] = v1 + v6;
                p[6 + row] = v1 - v6;
                p[2 + row] = v2 + v5;
                p[5 + row] = v2 - v5;
                p[3 + row] = v3 + v4;
                p[4 + row] = v3 - v4;
            }

            // inverse DCT on columns
            for (i = 0; i < 8; ++i) {
                final int col = i;

                // check for all-zero AC coefficients
                if (p[1*8 + col] == 0 && p[2*8 + col] == 0 && p[3*8 + col] == 0 &&
                        p[4*8 + col] == 0 && p[5*8 + col] == 0 && p[6*8 + col] == 0 &&
                        p[7*8 + col] == 0) {
                    t = (dctSqrt2 * dataIn[i+0] + 8192) >> 14;
                    p[0*8 + col] = t;
                    p[1*8 + col] = t;
                    p[2*8 + col] = t;
                    p[3*8 + col] = t;
                    p[4*8 + col] = t;
                    p[5*8 + col] = t;
                    p[6*8 + col] = t;
                    p[7*8 + col] = t;
                    continue;
                }

                // stage 4
                v0 = (dctSqrt2 * p[0*8 + col] + 2048) >> 12;
                v1 = (dctSqrt2 * p[4*8 + col] + 2048) >> 12;
                v2 = p[2*8 + col];
                v3 = p[6*8 + col];
                v4 = (dctSqrt1d2 * (p[1*8 + col] - p[7*8 + col]) + 2048) >> 12;
                v7 = (dctSqrt1d2 * (p[1*8 + col] + p[7*8 + col]) + 2048) >> 12;
                v5 = p[3*8 + col];
                v6 = p[5*8 + col];

                // stage 3
                t = (v0 - v1 + 1) >> 1;
                v0 = (v0 + v1 + 1) >> 1;
                v1 = t;
                t = (v2 * dctSin6 + v3 * dctCos6 + 2048) >> 12;
                v2 = (v2 * dctCos6 - v3 * dctSin6 + 2048) >> 12;
                v3 = t;
                t = (v4 - v6 + 1) >> 1;
                v4 = (v4 + v6 + 1) >> 1;
                v6 = t;
                t = (v7 + v5 + 1) >> 1;
                v5 = (v7 - v5 + 1) >> 1;
                v7 = t;

                // stage 2
                t = (v0 - v3 + 1) >> 1;
                v0 = (v0 + v3 + 1) >> 1;
                v3 = t;
                t = (v1 - v2 + 1) >> 1;
                v1 = (v1 + v2 + 1) >> 1;
                v2 = t;
                t = (v4 * dctSin3 + v7 * dctCos3 + 2048) >> 12;
                v4 = (v4 * dctCos3 - v7 * dctSin3 + 2048) >> 12;
                v7 = t;
                t = (v5 * dctSin1 + v6 * dctCos1 + 2048) >> 12;
                v5 = (v5 * dctCos1 - v6 * dctSin1 + 2048) >> 12;
                v6 = t;

                // stage 1
                p[0*8 + col] = v0 + v7;
                p[7*8 + col] = v0 - v7;
                p[1*8 + col] = v1 + v6;
                p[6*8 + col] = v1 - v6;
                p[2*8 + col] = v2 + v5;
                p[5*8 + col] = v2 - v5;
                p[3*8 + col] = v3 + v4;
                p[4*8 + col] = v3 - v4;
            }

            // convert to 8-bit integers
            for (i = 0; i < 64; ++i) {
                final int sample = 128 + ((p[i] + 8) >> 4);
                dataOut[i] = (byte) ( sample < 0 ? 0 : sample > 0xFF ? 0xFF : sample );
            }
        }
    }

    static interface DecoderFunction {
        void decode(ComponentIn component, int[] zz) throws IOException;
    }

    class Decoder {
        // private int precision;
        // private int samplesPerLine;
        // private int scanLines;
        private int mcusPerLine;
        private boolean progressive;
        // private int maxH, maxV;
        private int spectralStart, spectralEnd;
        private int successive;
        private int eobrun;
        private int successiveACState, successiveACNextValue;

        private int decodeScan(final Frame frame, final ArrayList<ComponentIn> components, int resetInterval,
                final int spectralStart, final int spectralEnd, final int successivePrev, final int successive) throws IOException {
            // this.precision = frame.precision;
            // this.samplesPerLine = frame.samplesPerLine;
            // this.scanLines = frame.scanLines;
            this.mcusPerLine = frame.mcusPerLine;
            this.progressive = frame.progressive;
            // this.maxH = frame.maxH;
            // this.maxV = frame.maxV;
            bstream.skip( bstream.getBitCount() ); // align to next byte
            this.spectralStart = spectralStart;
            this.spectralEnd = spectralEnd;
            this.successive = successive;

            final int componentsLength = components.size();

            final DecoderFunction decodeFn;
            if (progressive) {
                if (spectralStart == 0) {
                    decodeFn = successivePrev == 0 ? decodeDCFirst : decodeDCSuccessive;
                } else {
                    decodeFn = successivePrev == 0 ? decodeACFirst : decodeACSuccessive;
                }
            } else {
                decodeFn = decodeBaseline;
            }

            int mcu = 0;
            int mcuExpected;
            if (componentsLength == 1) {
                final ComponentIn c = components.get(0);
                mcuExpected = c.blocksPerLine * c.blocksPerColumn;
            } else {
                mcuExpected = mcusPerLine * frame.mcusPerColumn;
            }
            if (0 == resetInterval) {
                resetInterval = mcuExpected;
            }
            if(DEBUG) {
                System.err.println("JPEG.decodeScan.1 resetInterval "+resetInterval+", mcuExpected "+mcuExpected+", sA "+spectralStart+", sP "+successivePrev+", sE "+spectralEnd+", suc "+successive+", decodeFn "+decodeFn.getClass().getSimpleName());
            }
            int marker = 0;
            while ( /* untilMarker || */ mcu < mcuExpected) {
                // reset interval stuff
                for (int i = 0; i < componentsLength; i++) {
                    components.get(i).pred = 0;
                }
                eobrun = 0;

                try {
                    if (componentsLength == 1) {
                        final ComponentIn component = components.get(0);
                        for (int n = 0; n < resetInterval; n++) {
                            decodeBlock(component, decodeFn, mcu);
                            mcu++;
                        }
                    } else {
                        for (int n = 0; n < resetInterval; n++) {
                            for (int i = 0; i < componentsLength; i++) {
                                final ComponentIn component = components.get(i);
                                final int h = component.h;
                                final int v = component.v;
                                for (int j = 0; j < v; j++) {
                                    for (int k = 0; k < h; k++) {
                                        decodeMcu(component, decodeFn, mcu, j, k);
                                    }
                                }
                            }
                            mcu++;
                        }
                    }
                } catch (final MarkerException markerException) {
                    if(DEBUG) { System.err.println("JPEG.decodeScan: Marker exception: "+markerException.getMessage()); markerException.printStackTrace(); }
                    return markerException.getMarker();
                } catch (final CodecException codecException) {
                    if(DEBUG) { System.err.println("JPEG.decodeScan: Codec exception: "+codecException.getMessage()); codecException.printStackTrace(); }
                    bstream.skip( bstream.getBitCount() ); // align to next byte
                    return M_EOI; // force end !
                }

                // find marker
                bstream.skip( bstream.getBitCount() ); // align to next byte
                bstream.mark(2);
                marker = readUInt16();
                if( marker < 0xFF00 ) {
                    bstream.reset();
                    throw new CodecException("marker not found @ mcu "+mcu+"/"+mcuExpected+", u16: "+toHexString(marker));
                }
                final boolean isRSTx = 0xFFD0 <= marker && marker <= 0xFFD7; // !RSTx
                if(DEBUG) {
                    System.err.println("JPEG.decodeScan: MCUs "+mcu+"/"+mcuExpected+", u16 "+toHexString(marker)+", RSTx "+isRSTx+", "+frame);
                }
                if ( !isRSTx ) {
                    break; // handle !RSTx marker in caller
                }
            }
            return marker;
        }

        private final int readBit() throws MarkerException, IOException {
            final int bit = bstream.readBit(true /* msbFirst */);
            if( Bitstream.EOS == bit || 7 != bstream.getBitCount() ) {
                return bit;
            }
            // new byte read, i.e. bitCount == 7
            final int bitsData = bstream.getBitBuffer(); // peek for marker
            if ( 0xFF == bitsData ) { // marker prefix
                final int nextByte = bstream.getStream().read(); // snoop marker signature, will be dropped!
                if( -1 == nextByte ) {
                    throw new CodecException("marked prefix 0xFF, then EOF");
                }
                if (0 != nextByte) {
                    final int marker = (bitsData << 8) | nextByte;
                    throw new MarkerException(marker, "Marker at readBit pos " + bstream);
                }
                // unstuff 0
            }
            return bit;
        }

        private int decodeHuffman(final BinObj tree) throws IOException {
            BinObj node = tree;
            int bit;
            while ( ( bit = readBit() ) != -1 ) {
                node = node.get(bit);
                if ( node.isValue ) {
                    return 0x000000FF & node.getValue();
                }
            }
            throw new CodecException("EOF reached at "+bstream);
        }
        private int receive(int length) throws IOException {
            int n = 0;
            while (length > 0) {
                final int bit = readBit();
                if (bit == -1) {
                    return -1;
                }
                n = (n << 1) | bit;
                length--;
            }
            return n;
        }
        private int receiveAndExtend(final int length) throws IOException {
            final int n = receive(length);
            if (n >= 1 << (length - 1)) {
                return n;
            }
            return n + (-1 << length) + 1;
        }

        final DecoderFunction decodeBaseline = new BaselineDecoder();
        final DecoderFunction decodeDCFirst = new DCFirstDecoder();
        final DecoderFunction decodeDCSuccessive = new DCSuccessiveDecoder();
        final DecoderFunction decodeACFirst = new ACFirstDecoder();
        final DecoderFunction decodeACSuccessive = new ACSuccessiveDecoder();

        class BaselineDecoder implements DecoderFunction {
            @Override
            public void decode(final ComponentIn component, final int[] zz) throws IOException {
                final int t = decodeHuffman(component.huffmanTableDC);
                final int diff = ( t == 0 ) ? 0 : receiveAndExtend(t);
                zz[0] = ( component.pred += diff );
                int k = 1;
                while (k < 64) {
                    final int rs = decodeHuffman(component.huffmanTableAC);
                    final int s = rs & 15, r = rs >> 4;
                    if (s == 0) {
                        if (r < 15) {
                            break;
                        }
                        k += 16;
                        continue;
                    }
                    k += r;
                    final int z = dctZigZag[k];
                    zz[z] = receiveAndExtend(s);
                    k++;
                }
            }
        }
        class DCFirstDecoder implements DecoderFunction {
            @Override
            public void decode(final ComponentIn component, final int[] zz) throws IOException {
                final int t = decodeHuffman(component.huffmanTableDC);
                final int diff = ( t == 0 ) ? 0 : (receiveAndExtend(t) << successive);
                zz[0] = ( component.pred += diff );
            }
        }
        class DCSuccessiveDecoder implements DecoderFunction {
            @Override
            public void decode(final ComponentIn component, final int[] zz) throws IOException {
                zz[0] |= readBit() << successive;
            }
        }

        class ACFirstDecoder implements DecoderFunction {
            @Override
            public void decode(final ComponentIn component, final int[] zz) throws IOException {
                if (eobrun > 0) {
                    eobrun--;
                    return;
                }
                int k = spectralStart;
                final int e = spectralEnd;
                while (k <= e) {
                    final int rs = decodeHuffman(component.huffmanTableAC);
                    final int s = rs & 15, r = rs >> 4;
                    if (s == 0) {
                        if (r < 15) {
                            eobrun = receive(r) + (1 << r) - 1;
                            break;
                        }
                        k += 16;
                        continue;
                    }
                    k += r;
                    final int z = dctZigZag[k];
                    zz[z] = receiveAndExtend(s) * (1 << successive);
                    k++;
                }
            }
        }
        class ACSuccessiveDecoder implements DecoderFunction {
            @Override
            public void decode(final ComponentIn component, final int[] zz) throws IOException {
                int k = spectralStart;
                final int e = spectralEnd;
                int r = 0;
                while (k <= e) {
                    final int z = dctZigZag[k];
                    switch (successiveACState) {
                    case 0: // initial state
                        final int rs = decodeHuffman(component.huffmanTableAC);
                        final int s = rs & 15;
                        r = rs >> 4;
                        if (s == 0) {
                            if (r < 15) {
                                eobrun = receive(r) + (1 << r);
                                successiveACState = 4;
                            } else {
                                r = 16;
                                successiveACState = 1;
                            }
                        } else {
                            // if (s !== 1) {
                            if (s != 1) {
                                throw new CodecException("invalid ACn encoding");
                            }
                            successiveACNextValue = receiveAndExtend(s);
                            successiveACState = r != 0 ? 2 : 3;
                        }
                        continue;
                    case 1: // skipping r zero items
                    case 2:
                        if ( zz[z] != 0 ) {
                            zz[z] += (readBit() << successive);
                        } else {
                            r--;
                            if (r == 0) {
                                successiveACState = successiveACState == 2 ? 3 : 0;
                            }
                        }
                        break;
                    case 3: // set value for a zero item
                        if ( zz[z] != 0 ) {
                            zz[z] += (readBit() << successive);
                        } else {
                            zz[z] = successiveACNextValue << successive;
                            successiveACState = 0;
                        }
                        break;
                    case 4: // eob
                        if ( zz[z] != 0 ) {
                            zz[z] += (readBit() << successive);
                        }
                        break;
                        }
                        k++;
                    }
                    if (successiveACState == 4) {
                        eobrun--;
                        if (eobrun == 0) {
                            successiveACState = 0;
                        }
                    }
            }
        }
        void decodeMcu(final ComponentIn component, final DecoderFunction decoder, final int mcu, final int row, final int col) throws IOException {
            final int mcuRow = (mcu / mcusPerLine) | 0;
            final int mcuCol = mcu % mcusPerLine;
            final int blockRow = mcuRow * component.v + row;
            final int blockCol = mcuCol * component.h + col;
            decoder.decode(component, component.getBlock(blockRow, blockCol));
        }
        void decodeBlock(final ComponentIn component, final DecoderFunction decoder, final int mcu) throws IOException {
            final int blockRow = (mcu / component.blocksPerLine) | 0;
            final int blockCol = mcu % component.blocksPerLine;
            decoder.decode(component, component.getBlock(blockRow, blockCol));
        }
    }

    private final Decoder decoder = new Decoder();

    /** wrong color space ..
    private final void storeYCbCr2BGR(final PixelStorage pixelStorage, int x, int y, int Y, final int Cb, final int Cr)
    {
        if(Y<0) Y=0;
        int B = Y + ( ( 116130 * Cb ) >> 16 ) ;
        if(B<0) B=0;
        else if(B>255) B=255;

        int G = Y - ( ( 22554 * Cb + 46802 * Cr ) >> 16 ) ;
        if(G<0) G=0;
        else if(G>255) G=255;

        int R = Y + ( ( 91881 * Cr ) >> 16 );
        if(R<0) R=0;
        else if(R>255) R=255;

        pixelStorage.storeRGB(x, y, (byte)R, (byte)G, (byte)B);
    } */

    public synchronized void getPixel(final JPEGDecoder.ColorSink pixelStorage, final int width, final int height) {
        final int scaleX = this.width / width, scaleY = this.height / height;

        final int componentCount = this.components.length;
        final ColorSpace sourceCS = ( null != adobe ) ? adobe.colorSpace : ColorSpace.YCbCr;
        final ColorSpace storageCS = pixelStorage.allocate(width, height, sourceCS, componentCount);
        if( ColorSpace.RGB != storageCS && ColorSpace.YCbCr != storageCS ) {
            throw new IllegalArgumentException("Unsupported storage color space: "+storageCS);
        }

        switch (componentCount) {
        case 1: {
            // Grayscale
            final ComponentOut component1 = this.components[0];
            for (int y = 0; y < height; y++) {
                final byte[] component1Line = component1.getLine((int)(y * component1.scaleY * scaleY));
                for (int x = 0; x < width; x++) {
                    final byte Y = component1Line[(int)(x * component1.scaleX * scaleX)];
                    if( ColorSpace.YCbCr == storageCS ) {
                        pixelStorage.storeYCbCr(x, y, Y, (byte)0, (byte)0);
                    } else {
                        pixelStorage.storeRGB(x, y, Y, Y, Y);
                    }
                }
            }
        }
        break;
        case 2: {
            // PDF might compress two component data in custom colorspace
            final ComponentOut component1 = this.components[0];
            final ComponentOut component2 = this.components[1];
            for (int y = 0; y < height; y++) {
                final int ys = y * scaleY;
                final byte[] component1Line = component1.getLine((int)(ys * component1.scaleY));
                final byte[] component2Line = component1.getLine((int)(ys * component2.scaleY));
                for (int x = 0; x < width; x++) {
                    final int xs = x * scaleX;
                    final byte Y1 = component1Line[(int)(xs * component1.scaleX)];
                    final byte Y2 = component2Line[(int)(xs * component2.scaleX)];
                    pixelStorage.store2(x, y, Y1, Y2);
                }
            }
        }
        break;
        case 3: {
            if (ColorSpace.YCbCr != sourceCS) {
                throw new CodecException("Unsupported source color space w 3 components: "+sourceCS);
            }
            final ComponentOut component1 = this.components[0];
            final ComponentOut component2 = this.components[1];
            final ComponentOut component3 = this.components[2];
            for (int y = 0; y < height; y++) {
                final int ys = y * scaleY;
                final byte[] component1Line = component1.getLine((int)(ys * component1.scaleY));
                final byte[] component2Line = component2.getLine((int)(ys * component2.scaleY));
                final byte[] component3Line = component3.getLine((int)(ys * component3.scaleY));
                if( ColorSpace.YCbCr == storageCS ) {
                    for (int x = 0; x < width; x++) {
                        final int xs = x * scaleX;
                        final byte Y  = component1Line[(int)(xs * component1.scaleX)];
                        final byte Cb = component2Line[(int)(xs * component2.scaleX)];
                        final byte Cr = component3Line[(int)(xs * component3.scaleX)];
                        pixelStorage.storeYCbCr(x, y, Y, Cb, Cr);
                    }
                } else {
                    for (int x = 0; x < width; x++) {
                        final int xs = x * scaleX;
                        final int Y  = 0x000000FF & component1Line[(int)(xs * component1.scaleX)];
                        final int Cb = 0x000000FF & component2Line[(int)(xs * component2.scaleX)];
                        final int Cr = 0x000000FF & component3Line[(int)(xs * component3.scaleX)];
                        // storeYCbCr2BGR(pixelStorage, x, y, Y, Cb, Cr);
                        final byte R = clampTo8bit(Y + 1.402f * (Cr - 128f));
                        final byte G = clampTo8bit(Y - 0.3441363f * (Cb - 128f) - 0.71413636f * (Cr - 128f));
                        final byte B = clampTo8bit(Y + 1.772f * (Cb - 128f));
                        pixelStorage.storeRGB(x, y, R, G, B);
                    }
                }
            }
        }
        break;
        case 4: {
            if (ColorSpace.YCCK != sourceCS && ColorSpace.CMYK != sourceCS) {
                throw new CodecException("Unsupported source color space w 4 components: "+sourceCS);
            }
            final ComponentOut component1 = this.components[0];
            final ComponentOut component2 = this.components[1];
            final ComponentOut component3 = this.components[2];
            final ComponentOut component4 = this.components[3];
            for (int y = 0; y < height; y++) {
                final int ys = y * scaleY;
                final byte[] component1Line = component1.getLine((int)(ys * component1.scaleY));
                final byte[] component2Line = component2.getLine((int)(ys * component2.scaleY));
                final byte[] component3Line = component3.getLine((int)(ys * component3.scaleY));
                final byte[] component4Line = component4.getLine((int)(ys * component4.scaleY));
                if( ColorSpace.YCbCr == storageCS ) {
                    if (ColorSpace.YCCK != sourceCS) {
                        throw new CodecException("Unsupported storage color space "+storageCS+" with source color space "+sourceCS);
                    }
                    for (int x = 0; x < width; x++) {
                        final int xs = x * scaleX;
                        final byte Y1 = component1Line[(int)(xs * component1.scaleX)];
                        final byte C1 = component2Line[(int)(xs * component2.scaleX)];
                        final byte C2 = component3Line[(int)(xs * component3.scaleX)];
                        // final byte K  = component4Line[(int)(xs * component4.scaleX)];
                        // FIXME: YCCK is not really YCbCr, since K (black) is missing!
                        pixelStorage.storeYCbCr(x, y, Y1, C1, C2);
                    }
                } else {
                    if (ColorSpace.CMYK == sourceCS) {
                        for (int x = 0; x < width; x++) {
                            final int xs = x * scaleX;
                            final int cC = 0x000000FF & component1Line[(int)(xs * component1.scaleX)];
                            final int cM = 0x000000FF & component2Line[(int)(xs * component2.scaleX)];
                            final int cY = 0x000000FF & component3Line[(int)(xs * component3.scaleX)];
                            final int cK = 0x000000FF & component4Line[(int)(xs * component4.scaleX)];
                            // CMYK -> RGB
                            final byte R = clampTo8bit( ( cC * cK ) / 255f );
                            final byte G = clampTo8bit( ( cM * cK ) / 255f );
                            final byte B = clampTo8bit( ( cY * cK ) / 255f );
                            pixelStorage.storeRGB(x, y, R, G, B);
                        }
                    } else { // ColorModel.YCCK == sourceCM
                        for (int x = 0; x < width; x++) {
                            final int xs = x * scaleX;
                            final int Y  = 0x000000FF & component1Line[(int)(xs * component1.scaleX)];
                            final int Cb = 0x000000FF & component2Line[(int)(xs * component2.scaleX)];
                            final int Cr = 0x000000FF & component3Line[(int)(xs * component3.scaleX)];
                            final int cK = 0x000000FF & component4Line[(int)(xs * component4.scaleX)];
                            // YCCK -> 255f - [ R'G'B' ] -> CMYK
                            final float cC = 255f - ( Y + 1.402f * (Cr - 128f) );
                            final float cM = 255f - ( Y - 0.3441363f * (Cb - 128f) - 0.71413636f * (Cr - 128f) );
                            final float cY = 255f - ( Y + 1.772f * (Cb - 128f) );
                            // CMYK -> RGB
                            final byte R = clampTo8bit( ( cC * cK ) / 255f );
                            final byte G = clampTo8bit( ( cM * cK ) / 255f );
                            final byte B = clampTo8bit( ( cY * cK ) / 255f );
                            pixelStorage.storeRGB(x, y, R, G, B);
                        }
                    }
                }
            }
        }
        break;
        default:
            throw new CodecException("Unsupported color model: Space "+sourceCS+", components "+componentCount);
        }
    }

    private static byte clampTo8bit(final float a) {
        return (byte) ( a < 0f ? 0 : a > 255f ? 255 : a );
    }

    private static String toHexString(final int v) {
        return "0x"+Integer.toHexString(v);
    }
}
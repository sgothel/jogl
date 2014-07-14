/*
 * $Id: BaseTable.java,v 1.3 2007-02-08 04:31:31 davidsch Exp $
 *
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004-2007 David Schweinsberg
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

package jogamp.graph.font.typecast.ot.table;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Baseline Table
 * @version $Id: BaseTable.java,v 1.3 2007-02-08 04:31:31 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class BaseTable implements Table {

    abstract class BaseCoord {

        abstract int getBaseCoordFormat();

        abstract short getCoordinate();
    }

    class BaseCoordFormat1 extends BaseCoord {

        private final short _coordinate;

        protected BaseCoordFormat1(final DataInput di) throws IOException {
            _coordinate = di.readShort();
        }

        @Override
        int getBaseCoordFormat() {
            return 1;
        }

        @Override
        short getCoordinate() {
            return _coordinate;
        }

    }

    class BaseCoordFormat2 extends BaseCoord {

        private final short _coordinate;
        // private final int _referenceGlyph;
        // private final int _baseCoordPoint;

        protected BaseCoordFormat2(final DataInput di) throws IOException {
            _coordinate = di.readShort();
            /* _referenceGlyph = */ di.readUnsignedShort();
            /* _baseCoordPoint = */ di.readUnsignedShort();
        }

        @Override
        int getBaseCoordFormat() {
            return 2;
        }

        @Override
        short getCoordinate() {
            return _coordinate;
        }

    }

    class BaseCoordFormat3 extends BaseCoord {

        private final short _coordinate;
        // private final int _deviceTableOffset;

        protected BaseCoordFormat3(final DataInput di) throws IOException {
            _coordinate = di.readShort();
            /* _deviceTableOffset = */ di.readUnsignedShort();
        }

        @Override
        int getBaseCoordFormat() {
            return 2;
        }

        @Override
        short getCoordinate() {
            return _coordinate;
        }

    }

    static class FeatMinMaxRecord {

        // private final int _tag;
        // private final int _minCoordOffset;
        // private final int _maxCoordOffset;

        protected FeatMinMaxRecord(final DataInput di) throws IOException {
            /* _tag = */ di.readInt();
            /* _minCoordOffset = */ di.readUnsignedShort();
            /* _maxCoordOffset = */ di.readUnsignedShort();
        }
    }

    class MinMax {

        // private final int _minCoordOffset;
        // private final int _maxCoordOffset;
        private final int _featMinMaxCount;
        private final FeatMinMaxRecord[] _featMinMaxRecord;

        protected MinMax(final int minMaxOffset) throws IOException {
            final DataInput di = getDataInputForOffset(minMaxOffset);
            /* _minCoordOffset = */ di.readUnsignedShort();
            /* _maxCoordOffset = */ di.readUnsignedShort();
            _featMinMaxCount = di.readUnsignedShort();
            _featMinMaxRecord = new FeatMinMaxRecord[_featMinMaxCount];
            for (int i = 0; i < _featMinMaxCount; ++i) {
                _featMinMaxRecord[i] = new FeatMinMaxRecord(di);
            }
        }
    }

    class BaseValues {

        // private final int _defaultIndex;
        private final int _baseCoordCount;
        private final int[] _baseCoordOffset;
        private final BaseCoord[] _baseCoords;

        protected BaseValues(final int baseValuesOffset) throws IOException {
            final DataInput di = getDataInputForOffset(baseValuesOffset);
            /* _defaultIndex = */ di.readUnsignedShort();
            _baseCoordCount = di.readUnsignedShort();
            _baseCoordOffset = new int[_baseCoordCount];
            for (int i = 0; i < _baseCoordCount; ++i) {
                _baseCoordOffset[i] = di.readUnsignedShort();
            }
            _baseCoords = new BaseCoord[_baseCoordCount];
            for (int i = 0; i < _baseCoordCount; ++i) {
                final int format = di.readUnsignedShort();
                switch (format) {
                    case 1:
                        _baseCoords[i] = new BaseCoordFormat1(di);
                        break;
                    case 2:
                        _baseCoords[i] = new BaseCoordFormat2(di);
                        break;
                    case 3:
                        _baseCoords[i] = new BaseCoordFormat3(di);
                        break;
                }
            }
        }
    }

    static class BaseLangSysRecord {

        // private final int _baseLangSysTag;
        private final int _minMaxOffset;

        protected BaseLangSysRecord(final DataInput di) throws IOException {
            /* _baseLangSysTag = */ di.readInt();
            _minMaxOffset = di.readUnsignedShort();
        }

        /**
        int getBaseLangSysTag() {
            return _baseLangSysTag;
        } */

        int getMinMaxOffset() {
            return _minMaxOffset;
        }
    }

    class BaseScript {

        private final int _thisOffset;
        private final int _baseValuesOffset;
        private final int _defaultMinMaxOffset;
        private final int _baseLangSysCount;
        private final BaseLangSysRecord[] _baseLangSysRecord;
        private BaseValues _baseValues;
        private MinMax[] _minMax;

        protected BaseScript(final int baseScriptOffset) throws IOException {
            _thisOffset = baseScriptOffset;
            final DataInput di = getDataInputForOffset(baseScriptOffset);
            _baseValuesOffset = di.readUnsignedShort();
            _defaultMinMaxOffset = di.readUnsignedShort();
            _baseLangSysCount = di.readUnsignedShort();
            _baseLangSysRecord = new BaseLangSysRecord[_baseLangSysCount];
            for (int i = 0; i < _baseLangSysCount; ++i) {
                _baseLangSysRecord[i] = new BaseLangSysRecord(di);
            }
            if (_baseValuesOffset > 0) {
                _baseValues = new BaseValues(baseScriptOffset + _baseValuesOffset);
            }
            for (int i = 0; i < _baseLangSysCount; ++i) {
                _minMax[i] = new MinMax(baseScriptOffset + _baseLangSysRecord[i].getMinMaxOffset());
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder()
                .append("\nBaseScript BaseScriptT").append(Integer.toHexString(_thisOffset))
                .append("\nBaseValuesT").append(Integer.toHexString(_thisOffset + _baseValuesOffset))
                .append("\nMinMaxT").append(Integer.toHexString(_thisOffset + _defaultMinMaxOffset))
                .append("\n").append(Integer.toHexString(_baseLangSysCount));
//            for (int i = 0; i < _baseLangSysCount; ++i) {
//                sb.append("\n                          ; BaseScriptRecord[").append(i);
//                sb.append("]\n'").append(tagAsString(_baseScriptRecord[i].getBaseScriptTag())).append("'");
//                sb.append("\nBaseScriptT").append(Integer.toHexString(_thisOffset + _baseScriptRecord[i].getBaseScriptOffset()));
//            }
//            for (int i = 0; i < _baseScriptCount; ++i) {
//                sb.append("\n").append(_baseScripts[i].toString());
//            }
            if (_baseValues != null) {
                sb.append("\n").append(_baseValues.toString());
            }
            return sb.toString();
        }
    }

    static class BaseScriptRecord {

        private final int _baseScriptTag;
        private final int _baseScriptOffset;

        protected BaseScriptRecord(final DataInput di) throws IOException {
            _baseScriptTag = di.readInt();
            _baseScriptOffset = di.readUnsignedShort();
        }

        int getBaseScriptTag() {
            return _baseScriptTag;
        }

        int getBaseScriptOffset() {
            return _baseScriptOffset;
        }
    }

    class BaseScriptList {

        private final int _thisOffset;
        private final int _baseScriptCount;
        private final BaseScriptRecord[] _baseScriptRecord;
        private final BaseScript[] _baseScripts;

        protected BaseScriptList(final int baseScriptListOffset) throws IOException {
            _thisOffset = baseScriptListOffset;
            final DataInput di = getDataInputForOffset(baseScriptListOffset);
            _baseScriptCount = di.readUnsignedShort();
            _baseScriptRecord = new BaseScriptRecord[_baseScriptCount];
            for (int i = 0; i < _baseScriptCount; ++i) {
                _baseScriptRecord[i] = new BaseScriptRecord(di);
            }
            _baseScripts = new BaseScript[_baseScriptCount];
            for (int i = 0; i < _baseScriptCount; ++i) {
                _baseScripts[i] = new BaseScript(
                        baseScriptListOffset + _baseScriptRecord[i].getBaseScriptOffset());
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder()
                .append("\nBaseScriptList BaseScriptListT").append(Integer.toHexString(_thisOffset))
                .append("\n").append(Integer.toHexString(_baseScriptCount));
            for (int i = 0; i < _baseScriptCount; ++i) {
                sb.append("\n                          ; BaseScriptRecord[").append(i);
                sb.append("]\n'").append(tagAsString(_baseScriptRecord[i].getBaseScriptTag())).append("'");
                sb.append("\nBaseScriptT").append(Integer.toHexString(_thisOffset + _baseScriptRecord[i].getBaseScriptOffset()));
            }
            for (int i = 0; i < _baseScriptCount; ++i) {
                sb.append("\n").append(_baseScripts[i].toString());
            }
            return sb.toString();
        }
     }

    class BaseTagList {

        private final int _thisOffset;
        private final int _baseTagCount;
        private final int[] _baselineTag;

        protected BaseTagList(final int baseTagListOffset) throws IOException {
            _thisOffset = baseTagListOffset;
            final DataInput di = getDataInputForOffset(baseTagListOffset);
            _baseTagCount = di.readUnsignedShort();
            _baselineTag = new int[_baseTagCount];
            for (int i = 0; i < _baseTagCount; ++i) {
                _baselineTag[i] = di.readInt();
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder()
                .append("\nBaseTagList BaseTagListT").append(Integer.toHexString(_thisOffset))
                .append("\n").append(Integer.toHexString(_baseTagCount));
            for (int i = 0; i < _baseTagCount; ++i) {
                sb.append("\n'").append(tagAsString(_baselineTag[i])).append("'");
            }
            return sb.toString();
        }
    }

    class Axis {

        private final int _thisOffset;
        private final int _baseTagListOffset;
        private final int _baseScriptListOffset;
        private BaseTagList _baseTagList;
        private BaseScriptList _baseScriptList;

        protected Axis(final int axisOffset) throws IOException {
            _thisOffset = axisOffset;
            final DataInput di = getDataInputForOffset(axisOffset);
            _baseTagListOffset = di.readUnsignedShort();
            _baseScriptListOffset = di.readUnsignedShort();
            if (_baseTagListOffset != 0) {
                _baseTagList = new BaseTagList(axisOffset + _baseTagListOffset);
            }
            if (_baseScriptListOffset != 0) {
                _baseScriptList = new BaseScriptList(
                        axisOffset + _baseScriptListOffset);
            }
        }

        @Override
        public String toString() {
            return new StringBuilder()
                .append("\nAxis AxisT").append(Integer.toHexString(_thisOffset))
                .append("\nBaseTagListT").append(Integer.toHexString(_thisOffset + _baseTagListOffset))
                .append("\nBaseScriptListT").append(Integer.toHexString(_thisOffset + _baseScriptListOffset))
                .append("\n").append(_baseTagList)
                .append("\n").append(_baseScriptList)
                .toString();
        }
    }

    private final DirectoryEntry _de;
    private final int _version;
    private final int _horizAxisOffset;
    private final int _vertAxisOffset;
    private Axis _horizAxis;
    private Axis _vertAxis;
    private byte[] _buf;

    /** Creates a new instance of BaseTable */
    protected BaseTable(final DirectoryEntry de, final DataInput di) throws IOException {
        _de = (DirectoryEntry) de.clone();

        // Load entire table into a buffer, and create another input stream
        _buf = new byte[de.getLength()];
        di.readFully(_buf);
        final DataInput di2 = getDataInputForOffset(0);

        _version = di2.readInt();
        _horizAxisOffset = di2.readUnsignedShort();
        _vertAxisOffset = di2.readUnsignedShort();
        if (_horizAxisOffset != 0) {
            _horizAxis = new Axis(_horizAxisOffset);
        }
        if (_vertAxisOffset != 0) {
            _vertAxis = new Axis(_vertAxisOffset);
        }

        // Let go of the buffer
        _buf = null;
    }

    private DataInput getDataInputForOffset(final int offset) {
        return new DataInputStream(new ByteArrayInputStream(
                _buf, offset,
                _de.getLength() - offset));
    }

//    private String valueAsShortHex(int value) {
//        return String.format("%1$4x", value);
//    }
//
//    private String valueAsLongHex(int value) {
//        return String.format("%1$8x", value);
//    }

    static protected String tagAsString(final int tag) {
        final char[] c = new char[4];
        c[0] = (char)((tag >> 24) & 0xff);
        c[1] = (char)((tag >> 16) & 0xff);
        c[2] = (char)((tag >> 8) & 0xff);
        c[3] = (char)(tag & 0xff);
        return String.valueOf(c);
    }

    @Override
    public int getType() {
        return BASE;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder()
            .append("; 'BASE' Table - Baseline\n;-------------------------------------\n\n")
            .append("BASEHeader BASEHeaderT").append(Integer.toHexString(0))
            .append("\n").append(Integer.toHexString(_version))
            .append("\nAxisT").append(Integer.toHexString(_horizAxisOffset))
            .append("\nAxisT").append(Integer.toHexString(_vertAxisOffset));
        if (_horizAxis != null) {
            sb.append("\n").append(_horizAxis.toString());
        }
        if (_vertAxis != null) {
            sb.append("\n").append(_vertAxis.toString());
        }
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

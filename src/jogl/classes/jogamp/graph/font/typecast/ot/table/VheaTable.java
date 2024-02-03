/*
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

import java.io.DataInput;
import java.io.IOException;
import jogamp.graph.font.typecast.ot.Fixed;

/**
 * Vertical Header Table
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class VheaTable implements Table {

    private int _version;
    private short _ascent;
    private short _descent;
    private short _lineGap;
    private short _advanceHeightMax;
    private short _minTopSideBearing;
    private short _minBottomSideBearing;
    private short _yMaxExtent;
    private short _caretSlopeRise;
    private short _caretSlopeRun;
    private short _metricDataFormat;
    private int _numberOfLongVerMetrics;

    public VheaTable(DataInput di) throws IOException {
        _version = di.readInt();
        _ascent = di.readShort();
        _descent = di.readShort();
        _lineGap = di.readShort();
        _advanceHeightMax = di.readShort();
        _minTopSideBearing = di.readShort();
        _minBottomSideBearing = di.readShort();
        _yMaxExtent = di.readShort();
        _caretSlopeRise = di.readShort();
        _caretSlopeRun = di.readShort();
        for (int i = 0; i < 5; ++i) {
            di.readShort();
        }
        _metricDataFormat = di.readShort();
        _numberOfLongVerMetrics = di.readUnsignedShort();
    }

    @Override
    public int getType() {
        return vhea;
    }

    public short getAdvanceHeightMax() {
        return _advanceHeightMax;
    }

    public short getAscent() {
        return _ascent;
    }

    public short getCaretSlopeRise() {
        return _caretSlopeRise;
    }

    public short getCaretSlopeRun() {
        return _caretSlopeRun;
    }

    public short getDescent() {
        return _descent;
    }

    public short getLineGap() {
        return _lineGap;
    }

    public short getMetricDataFormat() {
        return _metricDataFormat;
    }

    public short getMinTopSideBearing() {
        return _minTopSideBearing;
    }

    public short getMinBottomSideBearing() {
        return _minBottomSideBearing;
    }

    public int getNumberOfLongVerMetrics() {
        return _numberOfLongVerMetrics;
    }

    public short getYMaxExtent() {
        return _yMaxExtent;
    }

    public String toString() {
        return "'vhea' Table - Vertical Header\n------------------------------" +
                "\n        'vhea' version:       " + Fixed.floatValue(_version) +
                "\n        xAscender:            " + _ascent +
                "\n        xDescender:           " + _descent +
                "\n        xLineGap:             " + _lineGap +
                "\n        advanceHeightMax:     " + _advanceHeightMax +
                "\n        minTopSideBearing:    " + _minTopSideBearing +
                "\n        minBottomSideBearing: " + _minBottomSideBearing +
                "\n        yMaxExtent:           " + _yMaxExtent +
                "\n        horizCaretSlopeNum:   " + _caretSlopeRise +
                "\n        horizCaretSlopeDenom: " + _caretSlopeRun +
                "\n        reserved0:            0" +
                "\n        reserved1:            0" +
                "\n        reserved2:            0" +
                "\n        reserved3:            0" +
                "\n        reserved4:            0" +
                "\n        metricDataFormat:     " + _metricDataFormat +
                "\n        numOf_LongVerMetrics: " + _numberOfLongVerMetrics;
    }
    
}

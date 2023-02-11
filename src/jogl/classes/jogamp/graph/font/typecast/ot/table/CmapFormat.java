/*
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004-2016 David Schweinsberg
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

/**
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public abstract class CmapFormat {
    
    public static class Range {
        
        private final int _startCode;
        private final int _endCode;
        
        Range(int startCode, int endCode) {
            _startCode = startCode;
            _endCode = endCode;
        }
        
        public int getStartCode() {
            return _startCode;
        }
        
        public int getEndCode() {
            return _endCode;
        }
    }

    static CmapFormat create(int format, DataInput di)
    throws IOException {
        switch(format) {
            case 0:
                return new CmapFormat0(di);
            case 2:
                return new CmapFormat2(di);
            case 4:
                return new CmapFormat4(di);
            case 6:
                return new CmapFormat6(di);
            case 12:
                return new CmapFormat12(di);
            default:
                return new CmapFormatUnknown(format, di);
        }
    }

    protected abstract int getFormat();

    public abstract int getLength();

    protected abstract int getLanguage();

    public abstract int getRangeCount();
    
    public abstract Range getRange(int index)
        throws ArrayIndexOutOfBoundsException;

    public abstract int mapCharCode(int charCode);
    
    @Override
    public String toString() {
        return "format: " +
                getFormat() +
                ", length: " +
                getLength() +
                ", language: " +
                getLanguage();
    }
}

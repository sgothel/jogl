/*
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004-2015 David Schweinsberg
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
 */
package jogamp.graph.font.typecast.cff;

import java.io.DataInput;
import java.io.IOException;

/**
 *
 * @author dschweinsberg
 */
public class StringIndex extends Index {
    
    public StringIndex(DataInput di) throws IOException {
        super(di);
    }

    public String getString(int index) {
        if (index < CffStandardStrings.standardStrings.length) {
            return CffStandardStrings.standardStrings[index];
        } else {
            index -= CffStandardStrings.standardStrings.length;
            if (index >= getCount()) {
                return null;
            }
            int offset = getOffset(index) - 1;
            int len = getOffset(index + 1) - offset - 1;
            StringBuilder sb = new StringBuilder();
            for (int i = offset; i < offset + len; ++i) {
                sb.append((char) getData()[i]);
            }
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        int nonStandardBase = CffStandardStrings.standardStrings.length;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getCount(); ++i) {
            sb.append(nonStandardBase + i).append(": ");
            sb.append(getString(nonStandardBase + i)).append("\n");
        }
        return sb.toString();
    }
    
}

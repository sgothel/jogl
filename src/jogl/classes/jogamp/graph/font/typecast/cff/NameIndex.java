/*
 * Copyright 2015 dschweinsberg.
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
public class NameIndex extends Index {
    
    public NameIndex(DataInput di) throws IOException {
        super(di);
    }

    private String getName(int index) {
        String name;
        int offset = getOffset(index) - 1;
        int len = getOffset(index + 1) - offset - 1;
        // Ensure the name hasn't been deleted
        if (getData()[offset] != 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = offset; i < offset + len; ++i) {
                sb.append((char) getData()[i]);
            }
            name = sb.toString();
        } else {
            name = "DELETED NAME";
        }
        return name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getCount(); ++i) {
            sb.append(getName(i)).append("\n");
        }
        return sb.toString();
    }
    
}

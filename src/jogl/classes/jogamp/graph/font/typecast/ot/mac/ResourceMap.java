/*
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

package jogamp.graph.font.typecast.ot.mac;

import java.io.DataInput;
import java.io.IOException;

/**
 *
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class ResourceMap {

    private final byte[] headerCopy = new byte[16];
    @SuppressWarnings("unused")
    private int nextResourceMap;
    @SuppressWarnings("unused")
    private int fileReferenceNumber;
    @SuppressWarnings("unused")
    private int attributes;
    private ResourceType[] types;
    
    /** Creates new ResourceMap */
    @SuppressWarnings("unused")
    public ResourceMap(DataInput di) throws IOException {
        di.readFully(headerCopy);
        nextResourceMap = di.readInt();
        fileReferenceNumber = di.readUnsignedShort();
        attributes = di.readUnsignedShort();
        int typeOffset = di.readUnsignedShort();
        int nameOffset = di.readUnsignedShort();
        int typeCount = di.readUnsignedShort() + 1;
        
        // Read types
        types = new ResourceType[typeCount];
        for (int i = 0; i < typeCount; i++) {
            types[i] = new ResourceType(di);
        }
        
        // Read the references
        for (int i = 0; i < typeCount; i++) {
            types[i].readRefs(di);
        }
        
        // Read the names
        for (int i = 0; i < typeCount; i++) {
            types[i].readNames(di);
        }
    }

    public ResourceType getResourceType(String typeName) {
        for (ResourceType type : types) {
            String s = type.getTypeAsString();
            if (type.getTypeAsString().equals(typeName)) {
                return type;
            }
        }
        return null;
    }

    public ResourceType getResourceType(int i) {
        return types[i];
    }
    
    public int getResourceTypeCount() {
        return types.length;
    }
}

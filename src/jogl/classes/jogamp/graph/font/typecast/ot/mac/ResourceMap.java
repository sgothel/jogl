/*
 * $Id: ResourceMap.java,v 1.1.1.1 2004-12-05 23:14:32 davidsch Exp $
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

package jogamp.graph.font.typecast.ot.mac;

import java.io.DataInput;
import java.io.IOException;

/**
 *
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: ResourceMap.java,v 1.1.1.1 2004-12-05 23:14:32 davidsch Exp $
 */
public class ResourceMap {

    private final byte[] headerCopy = new byte[16];
    // private final int nextResourceMap;
    // private final int fileReferenceNumber;
    // private final int attributes;
    private final ResourceType[] types;

    /** Creates new ResourceMap */
    public ResourceMap(final DataInput di) throws IOException {
        di.readFully(headerCopy);
        /* nextResourceMap = */ di.readInt();
        /* fileReferenceNumber = */ di.readUnsignedShort();
        /* attributes = */ di.readUnsignedShort();
        /* final int typeOffset = */ di.readUnsignedShort();
        /* final int nameOffset = */ di.readUnsignedShort();
        final int typeCount = di.readUnsignedShort() + 1;

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

    public ResourceType getResourceType(final String typeName) {
        for (int i = 0; i < types.length; i++) {
            if (types[i].getTypeAsString().equals(typeName)) {
                return types[i];
            }
        }
        return null;
    }

    public ResourceType getResourceType(final int i) {
        return types[i];
    }

    public int getResourceTypeCount() {
        return types.length;
    }
}

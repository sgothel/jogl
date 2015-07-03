/*
 * $Id: ResourceReference.java,v 1.1.1.1 2004-12-05 23:14:32 davidsch Exp $
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
 * @version $Id: ResourceReference.java,v 1.1.1.1 2004-12-05 23:14:32 davidsch Exp $
 */
public class ResourceReference {

    private final int id;
    private final short nameOffset;
    private final short attributes;
    private final int dataOffset;
    private final int handle;
    private String name;

    /** Creates new ResourceReference */
    protected ResourceReference(final DataInput di) throws IOException {
        id = di.readUnsignedShort();
        nameOffset = di.readShort();
        attributes = (short) di.readUnsignedByte();
        dataOffset = (di.readUnsignedByte()<<16) | di.readUnsignedShort();
        handle = di.readInt();
    }

    protected void readName(final DataInput di) throws IOException {
        if (nameOffset > -1) {
            final int len = di.readUnsignedByte();
            final byte[] buf = new byte[len];
            di.readFully(buf);
            name = new String(buf);
        }
    }

    public int getId() {
        return id;
    }

    public short getNameOffset() {
        return nameOffset;
    }

    public short getAttributes() {
        return attributes;
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public int getHandle() {
        return handle;
    }

    public String getName() {
        return name;
    }
}

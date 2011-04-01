/*
 * $Id: ResourceHeader.java,v 1.1.1.1 2004-12-05 23:14:32 davidsch Exp $
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
 * @version $Id: ResourceHeader.java,v 1.1.1.1 2004-12-05 23:14:32 davidsch Exp $
 */
public class ResourceHeader {

    private int dataOffset;
    private int mapOffset;
    private int dataLen;
    private int mapLen;

    /** Creates new ResourceHeader */
    public ResourceHeader(DataInput di) throws IOException {
        dataOffset = di.readInt();
        mapOffset = di.readInt();
        dataLen = di.readInt();
        mapLen = di.readInt();
    }

    public int getDataOffset() {
        return dataOffset;
    }
    
    public int getMapOffset() {
        return mapOffset;
    }
    
    public int getDataLength() {
        return dataLen;
    }
    
    public int getMapLength() {
        return mapLen;
    }
}

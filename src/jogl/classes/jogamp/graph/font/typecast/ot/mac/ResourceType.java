/*
 * $Id: ResourceType.java,v 1.1.1.1 2004-12-05 23:14:33 davidsch Exp $
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
 * @version $Id: ResourceType.java,v 1.1.1.1 2004-12-05 23:14:33 davidsch Exp $
 */
public class ResourceType {

    private int type;
    private int count;
    private int offset;
    private ResourceReference[] references;
    
    /** Creates new ResourceType */
    protected ResourceType(DataInput di) throws IOException {
        type = di.readInt();
        count = di.readUnsignedShort() + 1;
        offset = di.readUnsignedShort();
        references = new ResourceReference[count];
    }
    
    protected void readRefs(DataInput di) throws IOException {
        for (int i = 0; i < count; i++) {
            references[i] = new ResourceReference(di);
        }
    }

    protected void readNames(DataInput di) throws IOException {
        for (int i = 0; i < count; i++) {
            references[i].readName(di);
        }
    }

    public int getType() {
        return type;
    }
    
    public String getTypeAsString() {
        return new StringBuffer()
            .append((char)((type>>24)&0xff))
            .append((char)((type>>16)&0xff))
            .append((char)((type>>8)&0xff))
            .append((char)((type)&0xff))
            .toString();
    }
    
    public int getCount() {
        return count;
    }
    
    public int getOffset() {
        return offset;
    }

    public ResourceReference getReference(int i) {
        return references[i];
    }
}

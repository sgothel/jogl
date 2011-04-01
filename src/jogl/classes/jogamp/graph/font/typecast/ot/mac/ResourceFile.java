/*
 * $Id: ResourceFile.java,v 1.2 2007-01-29 04:01:53 davidsch Exp $
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

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Mac resource loading test.
 * TODO: incorporate this into the test suite.
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: ResourceFile.java,v 1.2 2007-01-29 04:01:53 davidsch Exp $
 */
public class ResourceFile {

    private ResourceHeader header;
    private ResourceMap map;
    
    /** Creates new Resource */
    public ResourceFile(RandomAccessFile raf) throws IOException {

        // Read header at the beginning of the file
        raf.seek(0);
        header = new ResourceHeader(raf);
        
        // Seek to the map offset and read the map
        raf.seek(header.getMapOffset());
        map = new ResourceMap(raf);
    }

    public ResourceMap getResourceMap() {
        return map;
    }

    public static void main(String[] args) {
        try {
            //RandomAccessFile raf = new RandomAccessFile("/Library/Fonts/GillSans.dfont", "r");
            
            // Tests loading a font from a resource fork on Mac OS X
            RandomAccessFile raf = new RandomAccessFile("/Library/Fonts/Georgia/..namedfork/rsrc", "r");
            ResourceFile resource = new ResourceFile(raf);
            for (int i = 0; i < resource.getResourceMap().getResourceTypeCount(); i++) {
                System.out.println(resource.getResourceMap().getResourceType(i).getTypeAsString());
            }
            
            // Get the first 'sfnt' resource
            ResourceType type = resource.getResourceMap().getResourceType("sfnt");
            ResourceReference reference = type.getReference(0);
            
            type = resource.getResourceMap().getResourceType("FOND");
            for (int i = 0; i < type.getCount(); ++i) {
                reference = type.getReference(i);
                System.out.println(reference.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

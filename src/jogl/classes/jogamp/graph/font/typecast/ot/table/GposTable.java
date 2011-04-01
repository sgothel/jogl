/*
 * $Id: GposTable.java,v 1.2 2007-01-24 09:47:47 davidsch Exp $
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

package jogamp.graph.font.typecast.ot.table;

import java.io.DataInput;
import java.io.IOException;

/**
 * TODO: To be implemented
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: GposTable.java,v 1.2 2007-01-24 09:47:47 davidsch Exp $
 */
public class GposTable implements Table {

    private DirectoryEntry _de;

    protected GposTable(DirectoryEntry de, DataInput di) throws IOException {
        _de = (DirectoryEntry) de.clone();

        // GPOS Header
        int version = di.readInt();
        int scriptList = di.readInt();
        int featureList = di.readInt();
        int lookupList = di.readInt();
    }

    /** Get the table type, as a table directory value.
     * @return The table type
     */
    public int getType() {
        return GPOS;
    }
    
    public String toString() {
        return "GPOS";
    }

    /**
     * Get a directory entry for this table.  This uniquely identifies the
     * table in collections where there may be more than one instance of a
     * particular table.
     * @return A directory entry
     */
    public DirectoryEntry getDirectoryEntry() {
        return _de;
    }
    
}

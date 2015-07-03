/*
 * $Id: CmapFormatUnknown.java,v 1.1 2004-12-21 10:21:23 davidsch Exp $
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
 * When we encounter a cmap format we don't understand, we can use this class
 * to hold the bare minimum information about it.
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: CmapFormatUnknown.java,v 1.1 2004-12-21 10:21:23 davidsch Exp $
 */
public class CmapFormatUnknown extends CmapFormat {

    /** Creates a new instance of CmapFormatUnknown */
    protected CmapFormatUnknown(final int format, final DataInput di) throws IOException {
        super(di);
        _format = format;

        // We don't know how to handle this data, so we'll just skip over it
        di.skipBytes(_length - 4);
    }

    @Override
    public int getRangeCount() {
        return 0;
    }

    @Override
    public Range getRange(final int index) throws ArrayIndexOutOfBoundsException {
        throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    public int mapCharCode(final int charCode) {
        return 0;
    }
}

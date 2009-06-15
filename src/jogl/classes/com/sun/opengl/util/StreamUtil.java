/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.util;

import java.io.*;
import java.nio.*;

/** Utilities for dealing with streams. */

public class StreamUtil {
    private StreamUtil() {}

    public static byte[] readAll2Array(InputStream stream) throws IOException {
        BytesRead bytesRead = readAllImpl(stream);
        byte[] data = bytesRead.data;
        if (bytesRead.payloadLen != data.length) {
            data = new byte[bytesRead.payloadLen];
            System.arraycopy(bytesRead.data, 0, data, 0, bytesRead.payloadLen);
        }
        return data;
    }

    public static ByteBuffer readAll2Buffer(InputStream stream) throws IOException {
        BytesRead bytesRead = readAllImpl(stream);
        return BufferUtil.newByteBuffer(bytesRead.data, 0, bytesRead.payloadLen);
    }

    private static BytesRead readAllImpl(InputStream stream) throws IOException {
        // FIXME: Shall we do this here ?
        if( !(stream instanceof BufferedInputStream) ) {
            stream = new BufferedInputStream(stream);
        }
        int avail = stream.available();
        byte[] data = new byte[avail];
        int numRead = 0;
        int pos = 0;
        do {
            if (pos + avail > data.length) {
                byte[] newData = new byte[pos + avail];
                System.arraycopy(data, 0, newData, 0, pos);
                data = newData;
            }
            numRead = stream.read(data, pos, avail);
            if (numRead >= 0) {
                pos += numRead;
            }
            avail = stream.available();
        } while (avail > 0 && numRead >= 0);

        return new BytesRead(pos, data);
    }

    private static class BytesRead {
        BytesRead(int payloadLen, byte[] data) {
            this.payloadLen=payloadLen;
            this.data=data;
        }
        int payloadLen;
        byte[] data;
    }
}

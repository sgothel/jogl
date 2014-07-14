/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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

package com.jogamp.opengl.util.texture.spi;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Little Endian Data Output Stream.
 *
 * This class implements an output stream filter to allow writing
 * of java native datatypes to an output stream which has those
 * native datatypes stored in a little endian byte order.<p>
 *
 * This is the sister class of the DataOutputStream which allows
 * for writing of java native datatypes to an output stream with
 * the datatypes stored in big endian byte order.<p>
 *
 * This class implements the minimum required and calls DataOutputStream
 * for some of the required methods for DataOutput.<p>
 *
 * Not all methods are implemented due to lack of immediate requirement
 * for that functionality. It is not clear if it is ever going to be
 * functionally required to be able to read UTF data in a LittleEndianManner<p>
 *
 */
public class LEDataOutputStream extends FilterOutputStream implements DataOutput
{
    /**
     * To reuse some of the non endian dependent methods from
     * DataOutputStream's methods.
     */
    DataOutputStream dataOut;

    public LEDataOutputStream(final OutputStream out)
    {
        super(out);
        dataOut = new DataOutputStream(out);
    }

    @Override
    public void close() throws IOException
    {
        dataOut.close(); // better close as we create it.
        // this will close underlying as well.
    }

    @Override
    public synchronized final void write(final byte b[]) throws IOException
    {
        dataOut.write(b, 0, b.length);
    }

    @Override
    public synchronized final void write(final byte b[], final int off, final int len) throws IOException
    {
        dataOut.write(b, off, len);
    }

    @Override
    public final void write(final int b) throws IOException
    {
        dataOut.write(b);
    }

    @Override
    public final void writeBoolean(final boolean v) throws IOException
    {
        dataOut.writeBoolean(v);
    }

    @Override
    public final void writeByte(final int v) throws IOException
    {
        dataOut.writeByte(v);
    }

    /** Don't call this -- not implemented */
    @Override
    public final void writeBytes(final String s) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void writeChar(final int v) throws IOException
    {
        dataOut.writeChar(((v >> 8) & 0xff) |
                          ((v & 0xff) << 8));
    }

    /** Don't call this -- not implemented */
    @Override
    public final void writeChars(final String s) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void writeDouble(final double v) throws IOException
    {
        writeLong(Double.doubleToRawLongBits(v));
    }

    @Override
    public final void writeFloat(final float v) throws IOException
    {
        writeInt(Float.floatToRawIntBits(v));
    }

    @Override
    public final void writeInt(final int v) throws IOException
    {
        dataOut.writeInt((v >>> 24) |
                         ((v >>> 8) & 0xff00) |
                         ((v << 8)  & 0x00ff00) |
                         (v << 24));
    }

    @Override
    public final void writeLong(final long v) throws IOException
    {
        writeInt((int) v);
        writeInt((int) (v >>> 32));
    }

    @Override
    public final void writeShort(final int v) throws IOException
    {
        dataOut.writeShort(((v >> 8) & 0xff) |
                           ((v & 0xff) << 8));
    }

    /** Don't call this -- not implemented */
    @Override
    public final void writeUTF(final String s) throws IOException
    {
        throw new UnsupportedOperationException();
    }
}

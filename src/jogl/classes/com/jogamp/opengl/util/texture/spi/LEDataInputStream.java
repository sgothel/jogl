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

package    com.jogamp.opengl.util.texture.spi;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * Little Endian Data Input Stream.
 *
 * This class implements an input stream filter to allow reading
 * of java native datatypes from an input stream which has those
 * native datatypes stored in a little endian byte order.<p>
 *
 * This is the sister class of the DataInputStream which allows
 * for reading of java native datatypes from an input stream with
 * the datatypes stored in big endian byte order.<p>
 *
 * This class implements the minimum required and calls DataInputStream
 * for some of the required methods for DataInput.<p>
 *
 * Not all methods are implemented due to lack of immediatte requirement
 * for that functionality. It is not clear if it is ever going to be
 * functionally required to be able to read UTF data in a LittleEndianManner<p>
 *
 * @author    Robin Luiten
 * @version    1.1    15/Dec/1997
 */
public class LEDataInputStream extends FilterInputStream implements DataInput
{
    /**
     * To reuse    some of    the    non    endian dependent methods from
     * DataInputStreams    methods.
     */
    DataInputStream    dataIn;

    public LEDataInputStream(final InputStream in)
    {
        super(in);
        dataIn = new DataInputStream(in);
    }

    @Override
    public void close() throws IOException
    {
        dataIn.close();        // better close as we create it.
        // this will close underlying as well.
    }

    @Override
    public synchronized    final int read(final byte    b[]) throws    IOException
    {
        return dataIn.read(b, 0, b.length);
    }

    @Override
    public synchronized    final int read(final byte    b[], final int off, final int len) throws IOException
    {
        final int    rl = dataIn.read(b,    off, len);
        return rl;
    }

    @Override
    public final void readFully(final byte b[]) throws IOException
    {
        dataIn.readFully(b,    0, b.length);
    }

    @Override
    public final void readFully(final byte b[], final int off, final int len)    throws IOException
    {
        dataIn.readFully(b,    off, len);
    }

    @Override
    public final int skipBytes(final int n) throws IOException
    {
        return dataIn.skipBytes(n);
    }

    @Override
    public final boolean readBoolean() throws IOException
    {
        final int    ch = dataIn.read();
        if (ch < 0)
            throw new EOFException();
        return (ch != 0);
    }

    @Override
    public final byte readByte() throws    IOException
    {
        final int    ch = dataIn.read();
        if (ch < 0)
            throw new EOFException();
        return (byte)(ch);
    }

    @Override
    public final int readUnsignedByte()    throws IOException
    {
        final int    ch = dataIn.read();
        if (ch < 0)
            throw new EOFException();
        return ch;
    }

    @Override
    public final short readShort() throws IOException
    {
        final int    ch1    = dataIn.read();
        final int    ch2    = dataIn.read();
        if ((ch1 | ch2)    < 0)
            throw new EOFException();
        return (short)((ch1    << 0) +    (ch2 <<    8));
    }

    @Override
    public final int readUnsignedShort() throws    IOException
    {
        final int    ch1    = dataIn.read();
        final int    ch2    = dataIn.read();
        if ((ch1 | ch2)    < 0)
            throw new EOFException();
        return (ch1    << 0) +    (ch2 <<    8);
    }

    @Override
    public final char readChar() throws    IOException
    {
        final int    ch1    = dataIn.read();
        final int    ch2    = dataIn.read();
        if ((ch1 | ch2)    < 0)
            throw new EOFException();
        return (char)((ch1 << 0) + (ch2    << 8));
    }

    @Override
    public final int readInt() throws IOException
    {
        final int    ch1    = dataIn.read();
        final int    ch2    = dataIn.read();
        final int    ch3    = dataIn.read();
        final int    ch4    = dataIn.read();
        if ((ch1 | ch2 | ch3 | ch4)    < 0)
            throw new EOFException();
        return ((ch1 <<    0) + (ch2 << 8)    + (ch3 << 16) +    (ch4 <<    24));
    }

    @Override
    public final long readLong() throws    IOException
    {
        final int    i1 = readInt();
        final int    i2 = readInt();
        return (i1 & 0xFFFFFFFFL) + ((long)i2 << 32);
    }

    @Override
    public final float readFloat() throws IOException
    {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public final double    readDouble() throws    IOException
    {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * dont call this it is not implemented.
     * @return empty new string
     **/
    @Override
    public final String    readLine() throws IOException
    {
        return "";
    }

    /**
     * dont call this it is not implemented
     * @return empty new string
     **/
    @Override
    public final String    readUTF() throws IOException
    {
        return "";
    }

    /**
     * dont call this it is not implemented
     * @return empty new string
     **/
    public final static    String readUTF(final DataInput in) throws    IOException
    {
        return "";
    }
}


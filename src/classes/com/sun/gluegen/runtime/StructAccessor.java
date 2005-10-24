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

package com.sun.gluegen.runtime;

import java.nio.*;

public class StructAccessor {
  private ByteBuffer   bb;
  private CharBuffer   cb;
  private DoubleBuffer db;
  private FloatBuffer  fb;
  private IntBuffer    ib;
  private LongBuffer   lb;
  private ShortBuffer  sb;

  public StructAccessor(ByteBuffer bb) {
    // Setting of byte order is concession to native code which needs
    // to instantiate these
    this.bb = bb.order(ByteOrder.nativeOrder());
  }

  public ByteBuffer getBuffer() {
    return bb;
  }

  /** Return a slice of the current ByteBuffer starting at the
      specified byte offset and extending the specified number of
      bytes. Note that this method is not thread-safe with respect to
      the other methods in this class. */
  public ByteBuffer slice(int byteOffset, int byteLength) {
    bb.position(byteOffset);
    bb.limit(byteOffset + byteLength);
    ByteBuffer newBuf = bb.slice();
    bb.position(0);
    bb.limit(bb.capacity());
    return newBuf;
  }

  /** Retrieves the byte at the specified slot (byte offset). */
  public byte getByteAt(int slot) {
    return bb.get(slot);
  }

  /** Puts a byte at the specified slot (byte offset). */
  public void setByteAt(int slot, byte v) {
    bb.put(slot, v);
  }

  /** Retrieves the char at the specified slot (2-byte offset). */
  public char getCharAt(int slot) {
    return charBuffer().get(slot);
  }

  /** Puts a char at the specified slot (2-byte offset). */
  public void setCharAt(int slot, char v) {
    charBuffer().put(slot, v);
  }

  /** Retrieves the double at the specified slot (8-byte offset). */
  public double getDoubleAt(int slot) {
    return doubleBuffer().get(slot);
  }

  /** Puts a double at the specified slot (8-byte offset). */
  public void setDoubleAt(int slot, double v) {
    doubleBuffer().put(slot, v);
  }

  /** Retrieves the float at the specified slot (4-byte offset). */
  public float getFloatAt(int slot) {
    return floatBuffer().get(slot);
  }

  /** Puts a float at the specified slot (4-byte offset). */
  public void setFloatAt(int slot, float v) {
    floatBuffer().put(slot, v);
  }

  /** Retrieves the int at the specified slot (4-byte offset). */
  public int getIntAt(int slot) {
    return intBuffer().get(slot);
  }

  /** Puts a int at the specified slot (4-byte offset). */
  public void setIntAt(int slot, int v) {
    intBuffer().put(slot, v);
  }

  /** Retrieves the long at the specified slot (8-byte offset). */
  public long getLongAt(int slot) {
    return longBuffer().get(slot);
  }

  /** Puts a long at the specified slot (8-byte offset). */
  public void setLongAt(int slot, long v) {
    longBuffer().put(slot, v);
  }

  /** Retrieves the short at the specified slot (2-byte offset). */
  public short getShortAt(int slot) {
    return shortBuffer().get(slot);
  }

  /** Puts a short at the specified slot (2-byte offset). */
  public void setShortAt(int slot, short v) {
    shortBuffer().put(slot, v);
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private CharBuffer charBuffer() {
    if (cb == null) {
      cb = bb.asCharBuffer();
    }
    return cb;
  }

  private DoubleBuffer doubleBuffer() {
    if (db == null) {
      db = bb.asDoubleBuffer();
    }
    return db;
  }

  private FloatBuffer floatBuffer() {
    if (fb == null) {
      fb = bb.asFloatBuffer();
    }
    return fb;
  }

  private IntBuffer intBuffer() {
    if (ib == null) {
      ib = bb.asIntBuffer();
    }
    return ib;
  }

  private LongBuffer longBuffer() {
    if (lb == null) {
      lb = bb.asLongBuffer();
    }
    return lb;
  }

  private ShortBuffer shortBuffer() {
    if (sb == null) {
      sb = bb.asShortBuffer();
    }
    return sb;
  }
}

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

package com.sun.gluegen.cgram.types;

/** Represents a bitfield in a struct. */

public class BitType extends IntType {
  private IntType underlyingType;
  private int sizeInBits;
  private int offset;

  public BitType(IntType underlyingType, int sizeInBits, int lsbOffset, int cvAttributes) {
    super(underlyingType.getName(), underlyingType.getSize(), underlyingType.isUnsigned(), cvAttributes);
    this.underlyingType = underlyingType;
    this.sizeInBits = sizeInBits;
    this.offset = lsbOffset;
  }

  public boolean equals(Object arg) {
    if (arg == this) return true;
    if (arg == null || (!(arg instanceof BitType))) {
      return false;
    }
    BitType t = (BitType) arg;
    return (super.equals(arg) && underlyingType.equals(t.underlyingType) &&
            (sizeInBits == t.sizeInBits) && (offset == t.offset));
  }

  public BitType asBit() { return this; }

  /** Size in bits of this type. */
  public int getSizeInBits() {
    return sizeInBits;
  }

  /** Offset from the least-significant bit (LSB) of the LSB of this
      type */
  public int getOffset() {
    return offset;
  }

  public void visit(TypeVisitor arg) {
    super.visit(arg);
    underlyingType.visit(arg);
  }

  Type newCVVariant(int cvAttributes) {
    return new BitType(underlyingType, sizeInBits, offset, cvAttributes);
  }  
}

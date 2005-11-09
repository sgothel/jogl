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

public class IntType extends PrimitiveType {
  private boolean unsigned;
  private boolean typedefedUnsigned;

  public IntType(String name, SizeThunk size, boolean unsigned, int cvAttributes) {
    this(name, size, unsigned, cvAttributes, false);
  }

  private IntType(String name, SizeThunk size, boolean unsigned, int cvAttributes, boolean typedefedUnsigned) {
    super(name, size, cvAttributes);
    this.unsigned = unsigned;
    this.typedefedUnsigned = typedefedUnsigned;
  }

  public boolean equals(Object arg) {
    if (arg == this) return true;
    if (arg == null || (!(arg instanceof IntType))) {
      return false;
    }
    IntType t = (IntType) arg;
    return (super.equals(arg) && (unsigned == t.unsigned));
  }  

  public void setName(String name) {
    super.setName(name);
    typedefedUnsigned = unsigned;
  }

  public IntType asInt()      { return this; }

  /** Indicates whether this type is unsigned */
  public boolean isUnsigned() { return unsigned; }

  public String toString() {
    return getCVAttributesString() + ((isUnsigned() & (!typedefedUnsigned)) ? "unsigned " : "") + getName();
  }

  Type newCVVariant(int cvAttributes) {
    return new IntType(getName(), getSize(), isUnsigned(), cvAttributes, typedefedUnsigned);
  }
}

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

/** Represents an array type. This differs from a pointer type in C
    syntax by the use of "[]" rather than "*". The length may or may
    not be known; if the length is unknown then a negative number
    should be passed in to the constructor. */

public class ArrayType extends Type {
  private Type elementType;
  private int length;
  private String computedName;

  public ArrayType(Type elementType, SizeThunk sizeInBytes, int length, int cvAttributes) {
    super(elementType.getName() + " *", sizeInBytes, cvAttributes);
    this.elementType = elementType;
    this.length      = length;
  }

  public boolean equals(Object arg) {
    if (arg == this) return true;
    if (arg == null || (!(arg instanceof ArrayType))) {
      return false;
    }
    ArrayType t = (ArrayType) arg;
    return (super.equals(arg) && elementType.equals(t.elementType) && (length == t.length));
  }

  public String getName(boolean includeCVAttrs) {
    // Lazy computation of name due to lazy setting of compound type
    // names during parsing
    // Note: don't think cvAttributes can be set for array types (unlike pointer types)
    if (computedName == null) {
      computedName = elementType.getName() + " *";
      computedName = computedName.intern();
    }
    return computedName;
  }

  public ArrayType asArray()      { return this; }

  public Type    getElementType() { return elementType; }
  public int     getLength()      { return length;      }
  public boolean hasLength()      { return length >= 0; }

  /** Return the bottommost element type if this is a multidimensional
      array. */
  public Type    getBaseElementType() {
    ArrayType t = this;
    while (t.getElementType().isArray()) {
      t = t.getElementType().asArray();
    }
    return t.getElementType();
  }

  /** Recompute the size of this array if necessary. This needs to be
      done when the base element type is a compound type. */
  public void    recomputeSize() {
    ArrayType arrayElementType = getElementType().asArray();
    if (arrayElementType != null) {
      arrayElementType.recomputeSize();
    }
    // FIXME: this doesn't take into account struct alignment, which may be necessary
    // See also FIXME below and in HeaderParser.g
    super.setSize(SizeThunk.mul(SizeThunk.constant(getLength()), elementType.getSize()));
  }

  public String toString() {
    return toString(null);
  }

  public String toString(String variableName) {
    StringBuffer buf = new StringBuffer();
    buf.append(elementType.getName());
    if (variableName != null) {
      buf.append(" ");
      buf.append(variableName);
    }
    buf.append("[");
    buf.append(length);
    buf.append("]");
    return buf.toString();
  }

  public void visit(TypeVisitor arg) {
    super.visit(arg);
    elementType.visit(arg);
  }

  Type newCVVariant(int cvAttributes) {
    return new ArrayType(elementType, getSize(), length, cvAttributes);
  }  
}

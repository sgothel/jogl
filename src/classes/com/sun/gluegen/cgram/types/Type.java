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

import java.util.List;

/** Models a C type. Primitive types include int, float, and
    double. All types have an associated name. Structs and unions are
    modeled as "compound" types -- composed of fields of primitive or
    other types. */

public abstract class Type {
  private String name;
  private SizeThunk size;
  private int    cvAttributes;
  private int    typedefedCVAttributes;
  private boolean hasTypedefName;

  protected Type(String name, SizeThunk size, int cvAttributes) {
    setName(name);
    this.size = size;
    this.cvAttributes = cvAttributes;
    hasTypedefName = false;
  }

  /** Returns the name of this type. The returned string is suitable
      for use as a type specifier. Does not include any const/volatile
      attributes. */
  public String       getName()    { return getName(false); }

  /** Returns the name of this type, optionally including
      const/volatile attributes. The returned string is suitable for
      use as a type specifier. */
  public String       getName(boolean includeCVAttrs) {
    if (!includeCVAttrs) {
      return name;
    }
    return getCVAttributesString() + name;
  }

  /** Set the name of this type; used for handling typedefs. */
  public void         setName(String name) {
    if (name == null) {
      this.name = name;
    } else {
      this.name = name.intern();
    }
    // Capture the const/volatile attributes at the time of typedef so
    // we don't redundantly repeat them in the CV attributes string
    typedefedCVAttributes = cvAttributes;
    hasTypedefName = true;
  }

  /** SizeThunk which computes size of this type in bytes. */
  public SizeThunk    getSize()    { return size; }
  /** Size of this type in bytes according to the given MachineDescription. */
  public long         getSize(MachineDescription machDesc) {
    SizeThunk thunk = getSize();
    if (thunk == null) {
      throw new RuntimeException("No size set for type \"" + getName() + "\"");
    }
    return thunk.compute(machDesc);
  }
  /** Set the size of this type; only available for CompoundTypes. */
  void                setSize(SizeThunk size) { this.size = size; }

  /** Casts this to a BitType or returns null if not a BitType. */
  public BitType      asBit()      { return null; }
  /** Casts this to an IntType or returns null if not an IntType. */
  public IntType      asInt()      { return null; }
  /** Casts this to an EnumType or returns null if not an EnumType. */
  public EnumType     asEnum()     { return null; }
  /** Casts this to a FloatType or returns null if not a FloatType. */
  public FloatType    asFloat()    { return null; }
  /** Casts this to a DoubleType or returns null if not a DoubleType. */
  public DoubleType   asDouble()   { return null; }
  /** Casts this to a PointerType or returns null if not a PointerType. */
  public PointerType  asPointer()  { return null; }
  /** Casts this to an ArrayType or returns null if not an ArrayType. */
  public ArrayType    asArray()    { return null; }
  /** Casts this to a CompoundType or returns null if not a CompoundType. */
  public CompoundType asCompound() { return null; }
  /** Casts this to a FunctionType or returns null if not a FunctionType. */
  public FunctionType asFunction() { return null; }
  /** Casts this to a VoidType or returns null if not a VoidType. */
  public VoidType     asVoid()     { return null; }

  /** Indicates whether this is a BitType. */
  public boolean      isBit()      { return (asBit()      != null); }
  /** Indicates whether this is an IntType. */
  public boolean      isInt()      { return (asInt()      != null); }
  /** Indicates whether this is an EnumType. */
  public boolean      isEnum()     { return (asEnum()     != null); }
  /** Indicates whether this is a FloatType. */
  public boolean      isFloat()    { return (asFloat()    != null); }
  /** Indicates whether this is a DoubleType. */
  public boolean      isDouble()   { return (asDouble()   != null); }
  /** Indicates whether this is a PointerType. */
  public boolean      isPointer()  { return (asPointer()  != null); }
  /** Indicates whether this is an ArrayType. */
  public boolean      isArray()    { return (asArray()    != null); }
  /** Indicates whether this is a CompoundType. */
  public boolean      isCompound() { return (asCompound() != null); }
  /** Indicates whether this is a FunctionType. */
  public boolean      isFunction() { return (asFunction() != null); }
  /** Indicates whether this is a VoidType. */
  public boolean      isVoid()     { return (asVoid()     != null); }

  /** Indicates whether this type is const. */
  public boolean      isConst()    { return (((cvAttributes & ~typedefedCVAttributes) & CVAttributes.CONST) != 0); }
  /** Indicates whether this type is volatile. */
  public boolean      isVolatile() { return (((cvAttributes & ~typedefedCVAttributes) & CVAttributes.VOLATILE) != 0); }

  /** Indicates whether this type is a primitive type. */
  public boolean      isPrimitive(){ return false; }

  /** Convenience routine indicating whether this Type is a pointer to
      a function. */
  public boolean isFunctionPointer() {
    return (isPointer() && asPointer().getTargetType().isFunction());
  }

  /** Hashcode for Types. */
  public int hashCode() {
    if (name == null) {
      return 0;
    }
    
    if (cvAttributes != 0)
    {
      String nameWithAttribs = name + cvAttributes;
      return nameWithAttribs.hashCode();
    }
    return name.hashCode();
  }
  
  /**
   * Equality test for Types.
   */
  public boolean equals(Object arg) {
    if (arg == this) {
      return true;
    }
    if (arg == null || (!(arg instanceof Type))) {
      return false;
    }
    Type t = (Type) arg;
    return ((name == t.name || (name != null && name.equals(name))) &&
            (size == t.size) &&
            (cvAttributes == t.cvAttributes));
  }

  /** Returns a string representation of this type. This string is not
      necessarily suitable for use as a type specifier; for example,
      it will contain an expanded description of structs/unions. */
  public String toString() {
    return getName(true);
  }

  /** Visit this type and all of the component types of this one; for
      example, the return type and argument types of a FunctionType. */
  public void visit(TypeVisitor visitor) {
    visitor.visitType(this);
  }

  public final int getCVAttributes() {
    return cvAttributes;
  }

  /** Returns a string indicating the const/volatile attributes of
      this type. */
  public final String getCVAttributesString() {
    if (isConst() && isVolatile()) return "const volatile ";
    if (isConst()) return "const ";
    if (isVolatile()) return "volatile ";
    return "";
  }

  /** Return a variant of this type matching the given const/volatile
      attributes. May return this object if the attributes match. */
  public final Type getCVVariant(int cvAttributes) {
    if (this.cvAttributes == cvAttributes) {
      return this;
    }
    return newCVVariant(cvAttributes);
  }

  /** Create a new variant of this type matching the given
      const/volatile attributes. */
  abstract Type newCVVariant(int cvAttributes);

  /** Indicates whether setName() has been called on this type,
      indicating that it already has a typedef name. */
  public boolean hasTypedefName() {
    return hasTypedefName;
  }

  /** Helper method for determining how many pointer indirections this
      type represents (i.e., "void **" returns 2). Returns 0 if this
      type is not a pointer type. */
  public int pointerDepth() {
    PointerType pt = asPointer();
    if (pt == null) {
      return 0;
    }
    return 1 + pt.getTargetType().pointerDepth();
  }

  /** Helper method for determining how many array dimentions this
      type represents (i.e., "char[][]" returns 2). Returns 0 if this
      type is not an array type. */
  public int arrayDimension() {
    ArrayType arrayType = asArray();
    if (arrayType == null) {
      return 0;
    }
    return 1 + arrayType.getElementType().arrayDimension();
  }

  /** Helper routine for list equality comparison */
  static boolean listsEqual(List a, List b) {
    return ((a == null && b == null) ||
            (a != null && b != null && a.equals(b)));
  }
}

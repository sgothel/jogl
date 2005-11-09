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

import java.util.*;

/** Models all compound types, i.e., those containing fields: structs
    and unions. The boolean type accessors indicate how the type is
    really defined. */

public class CompoundType extends Type {
  private CompoundTypeKind kind;
  // The name "foo" in the construct "struct foo { ... }";
  private String structName;
  private ArrayList fields;
  private boolean visiting;
  private boolean bodyParsed;
  private boolean computedHashcode;
  private int     hashcode;

  public CompoundType(String name, SizeThunk size, CompoundTypeKind kind, int cvAttributes) {
    this(name, size, kind, cvAttributes, null);
  }

  private CompoundType(String name, SizeThunk size, CompoundTypeKind kind, int cvAttributes, String structName) {
    super(name, size, cvAttributes);
    assert kind != null;
    this.kind = kind;
    this.structName = structName;
  }

  public int hashCode() {
    if (computedHashcode) {
      return hashcode;
    }

    if (structName != null) {
      hashcode = structName.hashCode();
    } else if (getName() != null) {
      hashcode = getName().hashCode();
    } else {
      hashcode = 0;
    }

    computedHashcode = true;
    return hashcode;
  }

  public boolean equals(Object arg) {
    if (arg == this) return true;
    if (arg == null || (!(arg instanceof CompoundType))) {
      return false;
    }
    CompoundType t = (CompoundType) arg;
    return (super.equals(arg) &&
            (structName == t.structName || (structName != null && structName.equals(t.structName))) &&
            kind == t.kind &&
            listsEqual(fields, t.fields));
  }

  /** Returns the struct name of this CompoundType, i.e. the "foo" in
      the construct "struct foo { ... };". */
  public String getStructName() {
    return structName;
  }
  
  /** Sets the struct name of this CompoundType, i.e. the "foo" in the
      construct "struct foo { ... };". */
  public void setStructName(String structName) {
    this.structName = structName;
  }

  public void setSize(SizeThunk size) {
    super.setSize(size);
  }

  public CompoundType asCompound() { return this; }

  /** Returns the number of fields in this type. */
  public int   getNumFields() {
    return ((fields == null) ? 0 : fields.size());
  }

  /** Returns the <i>i</i>th field of this type. */
  public Field getField(int i) {
    return (Field) fields.get(i);
  }

  /** Adds a field to this type. */
  public void addField(Field f) {
    if (bodyParsed) {
      throw new RuntimeException("Body of this CompoundType has already been parsed; should not be adding more fields");
    }
    if (fields == null) {
      fields = new ArrayList();
    }
    fields.add(f);
  }

  /** Indicates to this CompoundType that its body has been parsed and
      that no more {@link #addField} operations will be made. */
  public void setBodyParsed() {
    bodyParsed = true;
  }

  /** Indicates whether this type was declared as a struct. */
  public boolean isStruct() { return (kind == CompoundTypeKind.STRUCT); }
  /** Indicates whether this type was declared as a union. */
  public boolean isUnion()  { return (kind == CompoundTypeKind.UNION); }

  public String toString() {
    String cvAttributesString = getCVAttributesString();
    if (getName() != null) {
      return cvAttributesString + getName();
    } else if (getStructName() != null) {
      return cvAttributesString + "struct " + getStructName();
    } else {
      return cvAttributesString + getStructString();
    }
  }

  public void visit(TypeVisitor arg) {
    if (visiting) {
      return;
    }
    try {
      visiting = true;
      super.visit(arg);
      int n = getNumFields();
      for (int i = 0; i < n; i++) {
        Field f = getField(i);
        f.getType().visit(arg);
      }
    } finally {
      visiting = false;
    }
  }

  public String getStructString() {
    if (visiting) {
      if (getName() != null) {
        return getName();
      }
      return "struct {/*Recursive type reference*/}";
    }

    try {
      visiting = true;
      String kind = (isStruct() ? "struct {" : "union {");
      StringBuffer res = new StringBuffer();
      res.append(kind);
      int n = getNumFields();
      for (int i = 0; i < n; i++) {
        res.append(" ");
        res.append(getField(i));
      }
      res.append(" }");
      return res.toString();
    } finally {
      visiting = false;
    }
  }

  Type newCVVariant(int cvAttributes) {
    CompoundType t = new CompoundType(getName(), getSize(), kind, cvAttributes, structName);
    t.fields = fields;
    return t;
  }
}

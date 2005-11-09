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

/** Describes enumerated types. Enumerations are like ints except that
    they have a set of named values. */

public class EnumType extends IntType {
  private IntType underlyingType;

  private static class Enum {
    String name;
    long   value;
    Enum(String name, long value) {
      this.name = name;
      this.value = value;
    }

    String getName()  { return name; }
    long   getValue() { return value; }
  }
  private List/*<Enum>*/ enums;
  
  public EnumType(String name) {
    super(name, SizeThunk.LONG, false, CVAttributes.CONST ); 
    this.underlyingType = new IntType(name, SizeThunk.LONG, false, CVAttributes.CONST);
  }

  public EnumType(String name, SizeThunk enumSizeInBytes) {
    super(name, enumSizeInBytes, false, CVAttributes.CONST );
    this.underlyingType = new IntType(name, enumSizeInBytes, false, CVAttributes.CONST);
  }

  protected EnumType(String name, IntType underlyingType, int cvAttributes) {
    super(name, underlyingType.getSize(), underlyingType.isUnsigned(), cvAttributes);
    this.underlyingType = underlyingType;
  }
  
  public boolean equals(Object arg) {
    if (arg == this) return true;
    if (arg == null || (!(arg instanceof EnumType))) {
      return false;
    }
    EnumType t = (EnumType) arg;
    return (super.equals(arg) &&
            underlyingType.equals(t.underlyingType) &&
            listsEqual(enums, t.enums));
  }

  public EnumType asEnum()    { return this; }

  public void addEnum(String name, long val) {
    if (enums == null) {
      enums = new ArrayList();
    }
    enums.add(new Enum(name, val));
  }

  /** Number of enumerates defined in this enum. */
  public int    getNumEnumerates()  { return enums.size(); }
  /** Fetch <i>i</i>th (0..getNumEnumerates() - 1) name */
  public String getEnumName(int i)  { return ((Enum) enums.get(i)).getName();  }
  /** Fetch <i>i</i>th (0..getNumEnumerates() - 1) value */
  public long   getEnumValue(int i) { return ((Enum) enums.get(i)).getValue(); }
  /** Fetch the value of the enumerate with the given name. */
  public long   getEnumValue(String name) {
    for (int i = 0; i < enums.size(); ++i) {
      Enum n = ((Enum)enums.get(i));
      if (n.getName().equals(name)) { return n.getValue(); }
    }
    throw new NoSuchElementException(
      "No enumerate named \"" + name + "\" in EnumType \"" +
      getName() + "\"");
  }
  /** Does this enum type contain an enumerate with the given name? */
  public boolean containsEnumerate(String name) {
    for (int i = 0; i < enums.size(); ++i) {
      if (((Enum)enums.get(i)).getName().equals(name)) { return true; }
    }
    return false;
  }
  /** Remove the enumerate with the given name. Returns true if it was found
   * and removed; false if it was not found.
   */
  public boolean removeEnumerate(String name) {
    for (int i = 0; i < enums.size(); ++i) {
      Enum e = (Enum)enums.get(i);
      if (e.getName().equals(name)) {
        enums.remove(e);
        return true;
      }
    }
    return false;
  }

  public void visit(TypeVisitor arg) {
    super.visit(arg);
    underlyingType.visit(arg);
  }

  Type newCVVariant(int cvAttributes) {
    EnumType t = new EnumType(getName(), underlyingType, cvAttributes);
    t.enums = enums;
    return t;
  }  
}

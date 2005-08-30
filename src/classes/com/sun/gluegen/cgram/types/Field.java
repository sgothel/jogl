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

/** Represents a field in a struct or union. */

public class Field {
  private String  name;
  private Type    type;
  private long    offset;

  public Field(String name, Type type, long offset) {
    this.name = name;
    this.type = type;
    this.offset = offset;
  }

  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(Object arg) {
    if (arg == null || (!(arg instanceof Field))) {
      return false;
    }

    Field f = (Field) arg;
    return (((name != null && name.equals(f.name)) ||
             (name == null && f.name == null)) &&
            type.equals(f.type) &&
            offset == f.offset);
  }

  /** Name of this field in the containing data structure. */
  public String  getName()   { return name; }

  /** Type of this field. */
  public Type    getType()   { return type; }

  /** Offset, in bytes, of this field in the containing data structure. */
  public long    getOffset() { return offset; }

  /** Sets the offset of this field in the containing data structure. */
  public void    setOffset(long offset) { this.offset = offset; }

  public String toString() {
    if (!getType().isFunctionPointer()) {
      if (getName() == null &&
          getType().asCompound() != null &&
          getType().asCompound().isUnion()) {
        return "" + getType() + ";";
      }
      return "" + getType() + " " + getName() + ";";
    } else {
      FunctionType ft = getType().asPointer().getTargetType().asFunction();
      return ft.toString(getName(), true) + ";";
    }
  }
}

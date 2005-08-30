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

/** Describes a function symbol, which includes the name and
    type. Since we are currently only concerned with processing
    functions this is the only symbol type, though plausibly more
    types should be added and a true symbol table constructed during
    parsing. */

public class FunctionSymbol {
  private String name;
  private FunctionType type;

  public FunctionSymbol(String name, FunctionType type) {
    this.name = name;
    this.type = type;
  }

  public String getName() { return name; }

  /** Returns the type of this function. Do not add arguments to it
      directly; use addArgument instead. */
  public FunctionType getType() { return type; }

  /** Returns the return type of this function. */
  public Type getReturnType()   { return type.getReturnType(); }

  public int getNumArguments()  { return type.getNumArguments(); }

  /** Returns the name of the <i>i</i>th argument. May return null if
      no argument names were available during parsing. */
  public String getArgumentName(int i) {
    return type.getArgumentName(i);
  }

  /** Returns the type of the <i>i</i>th argument. */
  public Type getArgumentType(int i) {
    return type.getArgumentType(i);
  }
  
  /** Add an argument's name and type. Use null for unknown argument
      names. */
  public void addArgument(Type argumentType, String argumentName) {
    type.addArgument(argumentType, argumentName);
  }

  public String toString() {
    return getType().toString(getName());
  }

  public int hashCode() {
    if (name == null) {
      return 0;
    }
    return name.hashCode();
  }

  public boolean equals(Object arg) {
    if (arg == this) {
      return true;
    }
    
    if (arg == null || (!(arg instanceof FunctionSymbol))) {
      return false;
    }
    
    FunctionSymbol other = (FunctionSymbol) arg;
    return (
      (getName() == other.getName() || getName().equals(other.getName()))
      && type.equals(other.type));
  }
}

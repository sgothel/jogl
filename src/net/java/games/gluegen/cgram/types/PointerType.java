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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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

package net.java.games.gluegen.cgram.types;

public class PointerType extends Type {
  private Type targetType;
  private String computedName;
  private boolean hasTypedefedName;

  public PointerType(int size, Type targetType, int cvAttributes) {
    // can pass null for the final name parameter because the PointerType's getName() 
    // completely replaces superclass behavior
		this(size, targetType, cvAttributes, false, null);
  }

  private PointerType(int size, Type targetType, int cvAttributes, boolean hasTypedefedName, String typedefedName) {
    super(targetType.getName() + " *", size, cvAttributes);
    this.hasTypedefedName = false;
    this.targetType = targetType;
    if (hasTypedefedName) {
      setName(typedefedName);
    }
  }

  public int hashCode() {
    return targetType.hashCode();
  }

  public boolean equals(Object arg) {
    if (arg == this) return true;
    if (arg == null || (!(arg instanceof PointerType))) {
      return false;
    }
    PointerType t = (PointerType) arg;
    // Note we ignore the name of this type (which might be a typedef
    // name) for comparison purposes because this is what allows
    // e.g. a newly-fabricated type "PIXELFORMATDESCRIPTOR *" to be
    // canonicalized to e.g. "LPPIXELFORMATDESCRIPTOR"
    return ((getSize() == t.getSize()) &&
            (getCVAttributes() == t.getCVAttributes()) &&
            targetType.equals(t.targetType));
  }

  public void setName(String name) {
    super.setName(name);
    hasTypedefedName = true;
  }

  public String getName(boolean includeCVAttrs) {
    if (hasTypedefedName) {
      return super.getName(includeCVAttrs);
    } else {
      // Lazy computation of name due to lazy setting of compound type
      // names during parsing
      if (computedName == null) {
        computedName = targetType.getName(includeCVAttrs) + " *";
        computedName = computedName.intern();
      }
      if (!includeCVAttrs) {
        return computedName;
      }
      return targetType.getName(includeCVAttrs) + " * " + getCVAttributesString();
    }
  }

  public PointerType asPointer() { return this; }

  public Type getTargetType() { return targetType; }

  public boolean isFunctionPointer() { return targetType.isFunction(); }

  public String toString() {
    if (hasTypedefedName) {
      return super.getName(true);
    } else {
      if (!targetType.isFunction()) {
        return targetType.toString() + " * " + getCVAttributesString();
      }
      return toString(null); // this is a pointer to an unnamed function
    }
  }

  /** For use only when printing function pointers */
  public String toString(String functionName) {
    if (!targetType.isFunction()) {
      throw new RuntimeException("<Internal error or misuse> This method is only for use when printing function pointers");
    }
    return ((FunctionType) targetType).toString(functionName, true);
  }

  public void visit(TypeVisitor arg) {
    super.visit(arg);
    targetType.visit(arg);
  }

  Type newCVVariant(int cvAttributes) {
    return new PointerType(getSize(), targetType, cvAttributes, hasTypedefedName, (hasTypedefedName ? getName() : null));
  }  
}

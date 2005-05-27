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

package net.java.games.gluegen;

import java.util.*;

import net.java.games.gluegen.cgram.types.*;

/** Represents the binding of a C function to a Java method. Also used
    to represent calls through function pointers contained in
    structs. */

public class MethodBinding {

  private FunctionSymbol sym;
  private JavaType       javaReturnType;
  private List           javaArgumentTypes;
  private boolean        computedSignatureProperties;
  private boolean        signatureUsesNIO;
  private boolean        signatureUsesCArrays;
  private boolean        signatureUsesPrimitiveArrays;
  private JavaType       containingType;
  private Type           containingCType;
  private int            thisPointerIndex = -1;

  /**
   * Constructs a new MethodBinding that is an exact clone of the
   * argument, including the java return type and java argument
   * types. It's safe to modify this binding after construction.
   */
  public MethodBinding(MethodBinding bindingToCopy) {  
    this.sym = bindingToCopy.sym;

    this.containingType               = bindingToCopy.containingType;
    this.containingCType              = bindingToCopy.containingCType;
    this.javaReturnType               = bindingToCopy.javaReturnType;
    this.javaArgumentTypes            = (List)((ArrayList)bindingToCopy.javaArgumentTypes).clone();
    this.computedSignatureProperties  = bindingToCopy.computedSignatureProperties;
    this.signatureUsesNIO             = bindingToCopy.signatureUsesNIO;
    this.signatureUsesCArrays         = bindingToCopy.signatureUsesCArrays;
    this.signatureUsesPrimitiveArrays = bindingToCopy.signatureUsesPrimitiveArrays;
    this.thisPointerIndex             = bindingToCopy.thisPointerIndex;
  }
  
  /** Constructor for calling a C function. */
  public MethodBinding(FunctionSymbol sym) {
    this.sym = sym;
  }

  /** Constructor for calling a function pointer contained in a
      struct. */
  public MethodBinding(FunctionSymbol sym, JavaType containingType, Type containingCType) {
    this.sym = sym;
    this.containingType = containingType;
    this.containingCType = containingCType;
  }

  public void           setJavaReturnType(JavaType type) {
    javaReturnType = type;
    computedSignatureProperties = false;
  }

  public void           addJavaArgumentType(JavaType type) {
    if (javaArgumentTypes == null) {
      javaArgumentTypes = new ArrayList();
    }
    javaArgumentTypes.add(type);
    computedSignatureProperties = false;
  }

  public JavaType       getJavaReturnType() {
    return javaReturnType;
  }

  public int            getNumArguments() {
    return sym.getNumArguments();
  }

  public JavaType       getJavaArgumentType(int i) {
    return (JavaType) javaArgumentTypes.get(i);
  }

  public Type           getCReturnType() {
    return sym.getReturnType();
  }

  public Type           getCArgumentType(int i) {
    return sym.getArgumentType(i);
  }

  public FunctionSymbol getCSymbol() {
    return sym;
  }

  /** Returns either the argument name specified by the underlying
      FunctionSymbol or a fabricated argument name based on the
      position. Note that it is currently not guaranteed that there
      are no namespace clashes with these fabricated argument
      names. */
  public String         getArgumentName(int i) {
    String ret = sym.getArgumentName(i);
    if (ret != null) {
      return ret;
    }
    return "arg" + i;
  }

  public String         getName() {
    return sym.getName();
  }

  /** Replaces the C primitive pointer argument at slot <i>argumentNumber</i>
      (0..getNumArguments() - 1) with the specified type. If
      argumentNumber is less than 0 then replaces the return type. */
  public MethodBinding  createCPrimitivePointerVariant(int argumentNumber,
                                                       JavaType newArgType) {
    MethodBinding binding = new MethodBinding(sym);
    if (argumentNumber < 0) {
      binding.setJavaReturnType(newArgType);
    } else {
      binding.setJavaReturnType(javaReturnType);
    }
    for (int i = 0; i < getNumArguments(); i++) {
      JavaType type = getJavaArgumentType(i);
      if (i == argumentNumber) {
        type = newArgType;
      }
      binding.addJavaArgumentType(type);
    }
    return binding;
  }

  /**
   * Returns true if the return type or any of the outgoing arguments
   * in the method's signature require conversion or checking due to
   * the use of New I/O.
   */
  public boolean signatureUsesNIO() {
    computeSignatureProperties();
    return signatureUsesNIO;
  }

  /**
   * Returns true if any of the outgoing arguments in the method's
   * signature represent fixed-length C arrays which require length
   * checking during the call.
   */
  public boolean signatureUsesCArrays() {
    computeSignatureProperties();
    return signatureUsesCArrays;
  }

  /**
   * Returns true if any of the outgoing arguments in the method's
   * signature represent primitive arrays which require a
   * GetPrimitiveArrayCritical or similar operation during the call.
   */
  public boolean signatureUsesPrimitiveArrays() {
    computeSignatureProperties();
    return signatureUsesPrimitiveArrays;
  }

  /**
   * Computes summary information about the method's C and Java
   * signatures.
   */
  protected void computeSignatureProperties() {
    if (computedSignatureProperties)
      return;
    
    signatureUsesNIO = false;
    signatureUsesCArrays = false;
    signatureUsesPrimitiveArrays = false;

    if (javaReturnType.isCompoundTypeWrapper() ||
        javaReturnType.isNIOByteBuffer() ||
        javaReturnType.isArrayOfCompoundTypeWrappers()) {
      // Needs wrapping and/or setting of byte order (neither of
      // which can be done easily from native code)
      signatureUsesNIO = true;
    }

    for (int i = 0; i < getNumArguments(); i++) {
      JavaType javaArgType = getJavaArgumentType(i);
      Type cArgType = getCArgumentType(i);
      if (javaArgType.isCompoundTypeWrapper() ||
          javaArgType.isNIOBuffer() ||
          javaArgType.isNIOBufferArray()) {
        // Needs unwrapping of accessors or checking of direct
        // buffer property
        signatureUsesNIO = true;
      }

      if (cArgType.isArray()) {
        // Needs checking of array lengths
        signatureUsesCArrays = true;
      }

      if (javaArgType.isPrimitiveArray()) {
        // Needs getPrimitiveArrayCritical or similar construct
        // depending on native code calling convention
        signatureUsesPrimitiveArrays = true;
      }
    }

    computedSignatureProperties = true;
  }


  public MethodBinding  createNIOBufferVariant() {
    if (!signatureUsesNIO()) {
      return this;
    }
    MethodBinding binding = new MethodBinding(sym, containingType, containingCType);
    binding.thisPointerIndex = thisPointerIndex;
    if (javaReturnType.isCompoundTypeWrapper()) {
      binding.setJavaReturnType(JavaType.forNIOByteBufferClass());
    } else if (javaReturnType.isArrayOfCompoundTypeWrappers()) {
      binding.setJavaReturnType(JavaType.forNIOByteBufferArrayClass());
    } else {
      binding.setJavaReturnType(javaReturnType);
    }
    for (int i = 0; i < getNumArguments(); i++) {
      JavaType type = getJavaArgumentType(i);
      if (type.isCompoundTypeWrapper()) {
        type = JavaType.forNIOBufferClass();
      }
      binding.addJavaArgumentType(type);
    }
    return binding;
  }

  /** Indicates whether this MethodBinding is for a function pointer
      contained in a struct. */
  public boolean hasContainingType() {
    return (getContainingType() != null);
  }

  /** Retrieves the containing type of this MethodBinding if it is for
      a function pointer contained in a struct. */
  public JavaType getContainingType() {
    return containingType;
  }

  /** Retrieves the containing C type of this MethodBinding if it is for
      a function pointer contained in a struct. */
  public Type getContainingCType() {
    return containingCType;
  }

  /** Find the leftmost argument matching the type of the containing
      type (for function pointer MethodBindings) and record that as a
      "this" pointer, meaning that it does not need to be explicitly
      passed at the Java level. */
  public void findThisPointer() {
    clearThisPointer();
    for (int i = 0; i < getNumArguments(); i++) {
      JavaType arg = getJavaArgumentType(i);
      if (arg.equals(containingType)) {
        thisPointerIndex = i;
        break;
      }
      
      if (!arg.isJNIEnv()) {
        break; // this pointer must be leftmost argument excluding JNIEnvs
      }
    }
  }

  /** Clears any record of a this pointer for this MethodBinding. */
  public void clearThisPointer() {
    thisPointerIndex = -1;
  }

  /** Indicates whether the <i>i</i>th argument to this MethodBinding
      is actually a "this" pointer. */
  public boolean isArgumentThisPointer(int i) {
    return (thisPointerIndex == i);
  }
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    
    if (obj == null || ! (obj instanceof MethodBinding)) {
      return false;
    }

    MethodBinding other = (MethodBinding)obj;
    if (!(sym.equals(other.sym))) { return false; }
    if (!(javaReturnType.equals(other.getJavaReturnType()))) { return false; }
    if (containingType != null &&
        other.getContainingCType() != null &&
        (!(containingCType.equals(other.getContainingCType())))) {
        return false;
      }    
    if (javaArgumentTypes.size() != other.javaArgumentTypes.size()) {
      return false;
    }

    for (int i = 0; i < javaArgumentTypes.size(); ++i) {
      Object typeThis = javaArgumentTypes.get(i);
      Object typeOther = other.getJavaArgumentType(i);
      if (!(typeThis.equals(typeOther))) {
        return false;
      }
    }
    
    return true;
  }

  // FIXME!! Implement hashCode() to match equals(Object)

  /** Returns the signature of this binding. */
  public String toString() {
    StringBuffer buf = new StringBuffer(200);
    buf.append(getJavaReturnType().getName());
    buf.append(" ");
    buf.append(getName());
    buf.append("(");
    boolean needComma = false;
    for (int i = 0; i < getNumArguments(); i++) {
      JavaType type = getJavaArgumentType(i);
      if (type.isVoid()) { 
        // Make sure this is the only param to the method; if it isn't,
        // there's something wrong with our parsing of the headers.
        assert(getNumArguments() == 1);
        continue;
      } 
      if (type.isJNIEnv() || isArgumentThisPointer(i)) {
        // Don't need to expose these at the Java level
        continue;
      }
      
      if (needComma) {
        buf.append(", ");
      }

      buf.append(type.getName());
      buf.append(" ");
      buf.append(getArgumentName(i));
      needComma = true;
    }
    buf.append(")");
    return buf.toString();
  }

  public final Object clone() {
    return new MethodBinding(this);
  }

}


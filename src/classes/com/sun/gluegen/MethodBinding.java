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

package com.sun.gluegen;

import java.util.*;

import com.sun.gluegen.cgram.types.*;

/** Represents the binding of a C function to a Java method. Also used
    to represent calls through function pointers contained in
    structs. */

public class MethodBinding {

  private FunctionSymbol sym;
  private String         renamedMethodName;
  private JavaType       javaReturnType;
  private List           javaArgumentTypes;
  private boolean        computedSignatureProperties;
  private boolean        argumentsUseNIO;
  private boolean        signatureUsesNIO;
  private boolean        signatureCanUseIndirectNIO;
  private boolean        signatureUsesCompoundTypeWrappers;
  private boolean        signatureUsesCVoidPointers;
  private boolean        signatureUsesCPrimitivePointers;
  private boolean        signatureUsesCArrays;
  private boolean        signatureUsesJavaPrimitiveArrays;
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

    this.renamedMethodName                = bindingToCopy.renamedMethodName;
    this.containingType                   = bindingToCopy.containingType;
    this.containingCType                  = bindingToCopy.containingCType;
    this.javaReturnType                   = bindingToCopy.javaReturnType;
    this.javaArgumentTypes                = (List)((ArrayList)bindingToCopy.javaArgumentTypes).clone();
    this.computedSignatureProperties      = bindingToCopy.computedSignatureProperties;
    this.argumentsUseNIO                  = bindingToCopy.argumentsUseNIO;
    this.signatureUsesNIO                 = bindingToCopy.signatureUsesNIO;
    this.signatureCanUseIndirectNIO       = bindingToCopy.signatureCanUseIndirectNIO;
    this.signatureUsesCompoundTypeWrappers = bindingToCopy.signatureUsesCompoundTypeWrappers;
    this.signatureUsesCVoidPointers       = bindingToCopy.signatureUsesCVoidPointers;
    this.signatureUsesCPrimitivePointers  = bindingToCopy.signatureUsesCPrimitivePointers;
    this.signatureUsesCArrays             = bindingToCopy.signatureUsesCArrays;
    this.signatureUsesJavaPrimitiveArrays = bindingToCopy.signatureUsesJavaPrimitiveArrays;
    this.thisPointerIndex                 = bindingToCopy.thisPointerIndex;
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

  public String         getRenamedMethodName() {
    // Defaults to same as C symbol unless renamed
    if (renamedMethodName != null) {
      return renamedMethodName;
    }
    return sym.getName();
  }

  /** Supports renaming C function in Java binding. */
  public void           setRenamedMethodName(String name) {
    renamedMethodName = name;
  }

  /** Creates a new MethodBinding replacing the specified Java
      argument type with a new argument type. If argumentNumber is
      less than 0 then replaces the return type. */
  public MethodBinding replaceJavaArgumentType(int argumentNumber,
                                               JavaType newArgType) {
    MethodBinding binding = (MethodBinding) clone();
    binding.javaArgumentTypes = null;
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
   * Returns true if any of the outgoing arguments in the method's
   * signature require conversion or checking due to the use of New
   * I/O.
   */
  public boolean argumentsUseNIO() {
    computeSignatureProperties();
    return argumentsUseNIO;
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
   * Returns true if it is possible for any of the outgoing arguments
   * to be indirect NIO buffers.
   */
  public boolean signatureCanUseIndirectNIO() {
    computeSignatureProperties();
    return signatureCanUseIndirectNIO;
  }

  /**
   * Returns true if the return type or any of the outgoing arguments
   * in the method's signature use "compound type wrappers", or
   * NIO-based wrappers for C data structures.
   */
  public boolean signatureUsesCompoundTypeWrappers() {
    computeSignatureProperties();
    return signatureUsesCompoundTypeWrappers;
  }

  /**
   * Returns true if the function needs NIO-related
   * wrapping/unwrapping or conversion of various arguments. Currently
   * this returns the logical OR of signatureUsesNIO() and
   * signatureUsesCompoundTypeWrappers().
   */
  public boolean needsNIOWrappingOrUnwrapping() {
    return (signatureUsesNIO() || signatureUsesCompoundTypeWrappers());
  }

  /**
   * Returns true if the return type or any of the outgoing arguments
   * in the method's signature represent C void* pointers.
   */
  public boolean signatureUsesCVoidPointers() {
    computeSignatureProperties();
    return signatureUsesCVoidPointers;
  }

  /**
   * Returns true if the return type or any of the outgoing arguments
   * in the method's signature represent C primitive pointers.
   */
  public boolean signatureUsesCPrimitivePointers() {
    computeSignatureProperties();
    return signatureUsesCPrimitivePointers;
  }

  /**
   * Returns true if the return type or any of the outgoing arguments
   * in the method's signature represent C arrays.
   */
  public boolean signatureUsesCArrays() {
    computeSignatureProperties();
    return signatureUsesCArrays;
  }

  /**
   * Returns true if the return type or any of the outgoing arguments
   * in the method's signature represent Java primitive arrays.
   */
  public boolean signatureUsesJavaPrimitiveArrays() {
    computeSignatureProperties();
    return signatureUsesJavaPrimitiveArrays;
  }

  /**
   * Computes summary information about the method's C and Java
   * signatures.
   */
  protected void computeSignatureProperties() {
    if (computedSignatureProperties)
      return;
    
    argumentsUseNIO = false;
    signatureUsesNIO = false;
    signatureCanUseIndirectNIO = false;
    signatureUsesCompoundTypeWrappers = false;
    signatureUsesCVoidPointers = false;
    signatureUsesCPrimitivePointers = false;
    signatureUsesCArrays = false;
    signatureUsesJavaPrimitiveArrays = false;

    if (javaReturnType.isCompoundTypeWrapper()) {
      // Needs wrapping and/or setting of byte order (neither of which
      // can be done easily from native code)
      signatureUsesCompoundTypeWrappers = true;
    }

    if (javaReturnType.isNIOByteBuffer() ||
        javaReturnType.isArrayOfCompoundTypeWrappers()) {
      // Needs setting of byte order which can't be done easily from
      // native code
      signatureUsesNIO = true;
    }

    Type cRetType = sym.getReturnType();
    if (cRetType.isArray()) {
      // Needs checking of array lengths
      signatureUsesCArrays = true;
      if (cRetType.asArray().getElementType().isPrimitive()) {
        signatureUsesCPrimitivePointers = true;
      }
    }

    if (cRetType.isPointer()) {
      if (cRetType.asPointer().getTargetType().isPrimitive()) {
        signatureUsesCPrimitivePointers = true;
      } else if (cRetType.asPointer().getTargetType().isVoid()) {
        signatureUsesCVoidPointers = true;
      }
    }

    for (int i = 0; i < getNumArguments(); i++) {
      JavaType javaArgType = getJavaArgumentType(i);
      Type cArgType = getCArgumentType(i);
      if (javaArgType.isCompoundTypeWrapper()) {
        // Needs unwrapping of accessors
        signatureUsesCompoundTypeWrappers = true;
      }

      if (javaArgType.isNIOBuffer() ||
          javaArgType.isNIOBufferArray()) {
        // Needs checking of direct buffer property
        signatureUsesNIO = true;
        argumentsUseNIO = true;

        if (javaArgType.isNIOBuffer()) {
          // Potential conversion to indirect buffer
          signatureCanUseIndirectNIO = true;
        }
      }

      if (cArgType.isArray()) {
        // Needs checking of array lengths
        signatureUsesCArrays = true;
        if (cArgType.asArray().getElementType().isPrimitive()) {
          signatureUsesCPrimitivePointers = true;
        }
      }

      if (cArgType.isPointer()) {
        if (cArgType.asPointer().getTargetType().isPrimitive()) {
          signatureUsesCPrimitivePointers = true;
        } else if (cArgType.asPointer().getTargetType().isVoid()) {
          signatureUsesCVoidPointers = true;
        }
      }

      if (javaArgType.isPrimitiveArray()) {
        // Needs getPrimitiveArrayCritical or similar construct
        // depending on native code calling convention
        signatureUsesJavaPrimitiveArrays = true;
      }
    }

    computedSignatureProperties = true;
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
    buf.append(getRenamedMethodName());
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

  /** Returns a String containing the descriptor (signature in
      internal format) of this MethodBinding as it will be
      emitted. This is used to disambiguate between overloadings when
      manually specifying prologue and epilogue code, for example. */
  public String getDescriptor(boolean forImplementingMethodCall,
                              boolean eraseBufferAndArrayTypes) {
    StringBuffer buf = new StringBuffer();

    buf.append("(");

    if (forImplementingMethodCall && hasContainingType()) {
      // Always emit outgoing "this" argument
      buf.append("Ljava/nio/ByteBuffer;");
    }

    for (int i = 0; i < getNumArguments(); i++) {
      JavaType type = getJavaArgumentType(i);
      if (type.isVoid()) { 
        // Make sure this is the only param to the method; if it isn't,
        // there's something wrong with our parsing of the headers.
        if (getNumArguments() != 1) {
          throw new InternalError(
            "\"void\" argument type found in " +
            "multi-argument function \"" + this + "\"");
        }
        continue;
      } 

      if (type.isJNIEnv() || isArgumentThisPointer(i)) {
        // Don't need to expose these at the Java level
        continue;
      }

      buf.append(erasedTypeDescriptor(type, eraseBufferAndArrayTypes, false));

      // Add Buffer and array index offset arguments after each associated argument
      if (forImplementingMethodCall) {
        if (type.isNIOBuffer()) {
          buf.append("I");
        } else if (type.isNIOBufferArray()) {
          buf.append("[I");
        }
      }

      // Add offset argument after each primitive array
      if (type.isPrimitiveArray()) {
        buf.append("I");
      }
    }

    buf.append(")");

    // Emit return type for completeness even though we can't overload
    // based solely on return type
    buf.append(erasedTypeDescriptor(getJavaReturnType(), eraseBufferAndArrayTypes, false));

    return buf.toString();
  }

  protected String erasedTypeDescriptor(JavaType type, boolean eraseBufferAndArrayTypes, boolean skipBuffers) {
    if (eraseBufferAndArrayTypes) {
      if (type.isNIOBuffer() ||
          type.isPrimitiveArray()) {
        if (!skipBuffers) {
          // Direct buffers and arrays sent down as Object (but
          // returned as e.g. ByteBuffer)
          return "Ljava/lang/Object;";
        }
      } else if (type.isCompoundTypeWrapper()) {
        // Compound type wrappers are unwrapped to ByteBuffer
        return "Ljava/nio/ByteBuffer;";
      } else if (type.isArrayOfCompoundTypeWrappers()) {
        return "Ljava/nio/ByteBuffer;";
      }
    }
    return type.getDescriptor();
  }
}


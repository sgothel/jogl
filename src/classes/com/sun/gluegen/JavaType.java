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

import java.nio.*;

import com.sun.gluegen.cgram.types.*;

/**
 * Describes a java-side representation of a type that is used to represent
 * the same data on both the Java-side and C-side during a JNI operation. Also
 * contains some utility methods for creating common types.
 */
public class JavaType {
  private static final int PTR_C_VOID   = 1;
  private static final int PTR_C_CHAR   = 2;
  private static final int PTR_C_SHORT  = 3;
  private static final int PTR_C_INT32  = 4;
  private static final int PTR_C_INT64  = 5;
  private static final int PTR_C_FLOAT  = 6;
  private static final int PTR_C_DOUBLE = 7;

  private Class  clazz; // Primitive types and other types representable as Class objects
  private String name;  // Types we're generating glue code for (i.e., C structs)
  private Type   elementType; // Element type if this JavaType represents a C array
  private int    primitivePointerType; // Represents C arrays that
                                       // will / can be represented
                                       // with NIO buffers (resolved
                                       // down to another JavaType
                                       // later in processing)
  private static JavaType nioBufferType;
  private static JavaType nioByteBufferType;
  private static JavaType nioShortBufferType;
  private static JavaType nioIntBufferType;
  private static JavaType nioLongBufferType;
  private static JavaType nioFloatBufferType;
  private static JavaType nioDoubleBufferType;
  private static JavaType nioByteBufferArrayType;

  public boolean equals(Object arg) {
    if ((arg == null) || (!(arg instanceof JavaType))) {
      return false;
    }
    JavaType t = (JavaType) arg;
    return (this == t ||
            (t.clazz == clazz &&
             ((name == t.name) ||
              ((name != null) && (t.name != null) && (name.equals(t.name)))) &&
             ((elementType == t.elementType) ||
              (elementType != null) && (t.elementType != null) && (elementType.equals(t.elementType))) &&
             (primitivePointerType == t.primitivePointerType)));
  }

  public int hashCode() {
    if (clazz == null) {
      if (name == null) {
        return 0;
      }
      return name.hashCode();
    }
    return clazz.hashCode();
  }

  public JavaType getElementType() {
       return new JavaType(elementType);
  }

  /** Creates a JavaType corresponding to the given Java type. This
      can be used to represent arrays of primitive values or Strings;
      the emitters understand how to perform proper conversion from
      the corresponding C type. */
  public static JavaType createForClass(Class clazz) {
    return new JavaType(clazz);
  }

  /** Creates a JavaType corresponding to the specified C CompoundType
      name; for example, if "Foo" is supplied, then this JavaType
      represents a "Foo *" by way of a StructAccessor. */
  public static JavaType createForCStruct(String name) {
    return new JavaType(name);
  }

  /** Creates a JavaType corresponding to an array of the given
      element type. This is used to represent arrays of "Foo **" which
      should be mapped to Foo[] in Java. */
  public static JavaType createForCArray(Type elementType) {
    return new JavaType(elementType);
  }

  public static JavaType createForVoidPointer() {
    return new JavaType(PTR_C_VOID);
  }

  public static JavaType createForCCharPointer() {
    return new JavaType(PTR_C_CHAR);
  }

  public static JavaType createForCShortPointer() {
    return new JavaType(PTR_C_SHORT);
  }

  public static JavaType createForCInt32Pointer() {
    return new JavaType(PTR_C_INT32);
  }

  public static JavaType createForCInt64Pointer() {
    return new JavaType(PTR_C_INT64);
  }

  public static JavaType createForCFloatPointer() {
    return new JavaType(PTR_C_FLOAT);
  }

  public static JavaType createForCDoublePointer() {
    return new JavaType(PTR_C_DOUBLE);
  }

  public static JavaType createForJNIEnv() {
    return createForCStruct("JNIEnv");
  }

  public static JavaType forNIOBufferClass() {
    if (nioBufferType == null) {
      nioBufferType = createForClass(java.nio.Buffer.class);
    }
    return nioBufferType;
  }

  public static JavaType forNIOByteBufferClass() {
    if (nioByteBufferType == null) {
      nioByteBufferType = createForClass(java.nio.ByteBuffer.class);
    }
    return nioByteBufferType;
  }

  public static JavaType forNIOShortBufferClass() {
    if (nioShortBufferType == null) {
      nioShortBufferType = createForClass(java.nio.ShortBuffer.class);
    }
    return nioShortBufferType;
  }

  public static JavaType forNIOIntBufferClass() {
    if (nioIntBufferType == null) {
      nioIntBufferType = createForClass(java.nio.IntBuffer.class);
    }
    return nioIntBufferType;
  }

  public static JavaType forNIOLongBufferClass() {
    if (nioLongBufferType == null) {
      nioLongBufferType = createForClass(java.nio.LongBuffer.class);
    }
    return nioLongBufferType;
  }

  public static JavaType forNIOFloatBufferClass() {
    if (nioFloatBufferType == null) {
      nioFloatBufferType = createForClass(java.nio.FloatBuffer.class);
    }
    return nioFloatBufferType;
  }

  public static JavaType forNIODoubleBufferClass() {
    if (nioDoubleBufferType == null) {
      nioDoubleBufferType = createForClass(java.nio.DoubleBuffer.class);
    }
    return nioDoubleBufferType;
  }

  public static JavaType forNIOByteBufferArrayClass() {
    if (nioByteBufferArrayType == null) {
      ByteBuffer[] tmp = new ByteBuffer[0];
      nioByteBufferArrayType = createForClass(tmp.getClass());
    }
    return nioByteBufferArrayType;
  }

  /**
   * Returns the Java Class corresponding to this type. Returns null if this
   * object corresponds to a C primitive array type.
   */
  public Class getJavaClass() {
    return clazz;
  }

  /**
   * Returns the Java type name corresponding to this type.
   */
  public String getName() {
    if (clazz != null) {
      if (clazz.isArray()) {
        return arrayName(clazz);
      }
      return clazz.getName();
    }
    if (elementType != null) {
      return elementType.getName();
    }
    return name;
  }

  /**
   * Returns the descriptor (internal type signature) corresponding to
   * this type.
   */
  public String getDescriptor() {
    // FIXME: this is not completely accurate at this point (for
    // example, it knows nothing about the packages for compound
    // types)
    if (clazz != null) {
      return descriptor(clazz);
    }
    if (elementType != null) {
      return "[" + descriptor(elementType.getName());
    }
    return descriptor(name);
  }

  /** Returns the String corresponding to the JNI type for this type,
      or NULL if it can't be represented (i.e., it's a boxing class
      that we need to call getBuffer() on.) */
  public String jniTypeName() {
    if (isCompoundTypeWrapper()) {
      // These are sent down as Buffers (e.g., jobject)
      return "jobject";
    }

    if (isArrayOfCompoundTypeWrappers()) {
      // These are returned as arrays of ByteBuffers (e.g., jobjectArray)
      return "jobjectArray /* of ByteBuffer */";
    }

    if (clazz == null) {
      return null;
    }

    if (isVoid()) {
      return "void";
    }

    if (isPrimitive()) {
      return "j" + clazz.getName();
    }

    if (isPrimitiveArray() || isNIOBuffer()) {
      // We now pass primitive arrays and buffers uniformly down to native code as java.lang.Object.
      return "jobject";
    }

    if (isArray()) {
      if (isStringArray()) {
        return "jobjectArray /*elements are String*/";
      }

      Class elementType = clazz.getComponentType();

      if (isNIOBufferArray()) {
        return "jobjectArray /*elements are " + elementType.getName() + "*/";
      }

      if (elementType.isArray()) {
        // Type is array-of-arrays-of-something
        
        if (elementType.getComponentType().isPrimitive()) {          
          // Type is an array-of-arrays-of-primitive          
          return "jobjectArray /* elements are " + elementType.getComponentType() + "[]*/";
          //return "jobjectArray";
        } else {
          throw new RuntimeException("Multi-dimensional arrays of types that are not primitives or Strings are not supported.");          
        }
      }

      // Some unusual type that we don't handle
      throw new RuntimeException("Unexpected and unsupported array type: \"" + this + "\"");
    }

    if (isString()) {
      return "jstring";
    }

    return "jobject";
  }

  public boolean isNIOBuffer() {
    return (clazz != null && java.nio.Buffer.class.isAssignableFrom(clazz));
  }

  public boolean isNIOByteBuffer() {
    return (clazz == java.nio.ByteBuffer.class);
  }

  public boolean isNIOByteBufferArray() {
    return (this == nioByteBufferArrayType);
  }

  public boolean isNIOBufferArray() {
    return (isArray() &&
            (java.nio.Buffer.class.isAssignableFrom(clazz.getComponentType())));
  }

  public boolean isString() {
    return (clazz == java.lang.String.class);
  }

  public boolean isArray() {
    return ((clazz != null) && clazz.isArray());
  }

  public boolean isFloatArray() {
     return(clazz.isArray() && clazz.getComponentType() == Float.TYPE);
  }

  public boolean isDoubleArray() {
     return(clazz.isArray() && clazz.getComponentType() == Double.TYPE);
  }

  public boolean isByteArray() {
     return(clazz.isArray() && clazz.getComponentType() == Byte.TYPE);
  }

  public boolean isIntArray() {
     return(clazz.isArray() && clazz.getComponentType() == Integer.TYPE);
  }

  public boolean isShortArray() {
     return(clazz.isArray() && clazz.getComponentType() == Short.TYPE);
  }

  public boolean isLongArray() {
     return(clazz.isArray() && clazz.getComponentType() == Long.TYPE);
  }

  public boolean isStringArray() {
     return(clazz.isArray() && clazz.getComponentType() == java.lang.String.class);
  }


  public boolean isPrimitive() {
    return ((clazz != null) && !isArray() && clazz.isPrimitive() && (clazz != Void.TYPE));
  }

  public boolean isPrimitiveArray() {
    return (isArray() && (clazz.getComponentType().isPrimitive()));
  }

  public boolean isShort() {
    return (clazz == Short.TYPE);
  }

  public boolean isFloat() {
    return (clazz == Float.TYPE);
  }

  public boolean isDouble() {
    return (clazz == Double.TYPE);
  }

  public boolean isByte() {
    return (clazz == Byte.TYPE);
  }

  public boolean isLong() {
    return (clazz == Long.TYPE);
  }

  public boolean isInt() {
    return (clazz == Integer.TYPE);
  }

  public boolean isVoid() {
    return (clazz == Void.TYPE);
  }

  public boolean isCompoundTypeWrapper() {
    return (clazz == null && name != null && !isJNIEnv());
  }
  
  public boolean isArrayOfCompoundTypeWrappers() {
    return (elementType != null);
  }
  
  public boolean isCPrimitivePointerType() {
    return (primitivePointerType != 0);
  }

  public boolean isCVoidPointerType() {
    return (primitivePointerType == PTR_C_VOID);
  }

  public boolean isCCharPointerType() {
    return (primitivePointerType == PTR_C_CHAR);
  }

  public boolean isCShortPointerType() {
    return (primitivePointerType == PTR_C_SHORT);
  }

  public boolean isCInt32PointerType() {
    return (primitivePointerType == PTR_C_INT32);
  }

  public boolean isCInt64PointerType() {
    return (primitivePointerType == PTR_C_INT64);
  }

  public boolean isCFloatPointerType() {
    return (primitivePointerType == PTR_C_FLOAT);
  }

  public boolean isCDoublePointerType() {
    return (primitivePointerType == PTR_C_DOUBLE);
  }

  public boolean isJNIEnv() {
    return clazz == null && name == "JNIEnv";
  }

  public Object clone() {
    JavaType clone = new JavaType(primitivePointerType);

    clone.clazz = this.clazz;
    clone.name = this.name;
    clone.elementType = this.elementType;

    return clone;
  }

  public String toString() {
    return getName();
  }
  
  //----------------------------------------------------------------------
  // Internals only below this point
  //

  // For debugging
  public void dump() {
    System.err.println("[clazz = " + clazz + " , name = " + name + " , elementType = " + elementType + " , primitivePointerType = " + primitivePointerType + "]");
  }

  /**
   * Constructs a representation for a type corresponding to the given Class
   * argument.
   */
  private JavaType(Class clazz) {
    this.clazz = clazz;
  }

  /** Constructs a type representing a named C struct. */
  private JavaType(String name) {
    this.name = name;
  }

  /** Constructs a type representing an array of C pointers. */
  private JavaType(Type elementType) {
    this.elementType = elementType;
  }

  /** Constructs a type representing a pointer to a C primitive
      (integer, floating-point, or void pointer) type. */
  private JavaType(int primitivePointerType) {
    this.primitivePointerType = primitivePointerType;
  }

  private String arrayName(Class clazz) {
    StringBuffer buf = new StringBuffer();
    int arrayCount = 0;
    while (clazz.isArray()) {
      ++arrayCount;
      clazz = clazz.getComponentType();
    }
    buf.append(clazz.getName());
    while (--arrayCount >= 0) {
      buf.append("[]");
    }
    return buf.toString();
  }

  private String arrayDescriptor(Class clazz) {
    StringBuffer buf = new StringBuffer();
    int arrayCount = 0;
    while (clazz.isArray()) {
      buf.append("[");
      clazz = clazz.getComponentType();
    }
    buf.append(descriptor(clazz));
    return buf.toString();
  }

  private String descriptor(Class clazz) {
    if (clazz.isPrimitive()) {
      if (clazz == Boolean.TYPE) return "Z";
      if (clazz == Byte.TYPE)    return "B";
      if (clazz == Double.TYPE)  return "D";
      if (clazz == Float.TYPE)   return "F";
      if (clazz == Integer.TYPE) return "I";
      if (clazz == Long.TYPE)    return "J";
      if (clazz == Short.TYPE)   return "S";
      if (clazz == Void.TYPE)    return "V";
      throw new RuntimeException("Unexpected primitive type " + clazz.getName());
    }
    if (clazz.isArray()) {
      return arrayDescriptor(clazz);
    }
    return descriptor(clazz.getName());
  }

  private String descriptor(String referenceTypeName) {
    return "L" + referenceTypeName.replace('.', '/') + ";";
  }
}

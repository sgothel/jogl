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

package net.java.games.gluegen;

import java.nio.*;

import net.java.games.gluegen.cgram.types.*;

/**
 * Describes a java-side representation of a type that is used to represent
 * the same data on both the Java-side and C-side during a JNI operation. Also
 * contains some utility methods for creating common types.
 */
public class JavaType {
  private Class  clazz; // Primitive types and other types representable as Class objects
  private String name;  // Types we're generating glue code for (i.e., C structs)
  private Type   elementType; // Element type if this JavaType represents a C array
  private static JavaType nioBufferType;
  private static JavaType nioByteBufferType;
  private static JavaType nioByteBufferArrayType;

  public boolean equals(Object arg) {
    if ((arg == null) || (!(arg instanceof JavaType))) {
      return false;
    }
    JavaType t = (JavaType) arg;
    return (t.clazz == clazz &&
            ((t.name == name) ||
             ((name != null) && (t.name != null) && (t.name.equals(name)))));
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
    return new JavaType();
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

  public static JavaType forNIOByteBufferArrayClass() {
    if (nioByteBufferArrayType == null) {
      ByteBuffer[] tmp = new ByteBuffer[0];
      nioByteBufferArrayType = createForClass(tmp.getClass());
    }
    return nioByteBufferArrayType;
  }

  /**
   * Returns the Java Class corresponding to this type. Returns null if this
   * object corresponds to a C "void*" type.
   */
  public Class getJavaClass() {
    return clazz;
  }

  /**
   * Returns the name corresponding to this type. Returns null when this
   * object does not represent a C-language "struct" type.
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

  /** Returns the String corresponding to the JNI type for this type,
      or NULL if it can't be represented (i.e., it's a boxing class
      that we need to call getBuffer() on.) */
  public String jniTypeName() {
    if (clazz == null) {
      return null;
    }

    if (isVoid()) {
      return "void";
    }

    if (clazz.isPrimitive()) {
      return "j" + clazz.getName();
    }

    if (clazz.isArray()) {
      Class elementType = clazz.getComponentType();
      if (elementType.isPrimitive())
      {
        // Type is array-of-primitive
        return "j" + elementType.getName() + "Array";
      }
      else if (elementType == java.lang.String.class)
      {
        // Type is array-of-string
        return "jobjectArray /*elements are String*/";
        //return "jobjectArray";
      }
      else if (elementType == java.nio.ByteBuffer.class)
      {
        return "jobjectArray /*elements are ByteBuffer*/";
      }
      else if (elementType.isArray())
      {
        // Type is array-of-arrays-of-something
        
        if (elementType.getComponentType().isPrimitive())
        {          
          // Type is an array-of-arrays-of-primitive          
          return "jobjectArray /* elements are " + elementType.getComponentType() + "[]*/";
          //return "jobjectArray";
        }
        else
        {
          throw new RuntimeException("Multi-dimensional arrays of types that are not primitives or Strings are not supported.");          
        }
      }
      else
      {
        // Some unusual type that we don't handle
        throw new RuntimeException("Unexpected and unsupported type: \"" + this + "\"");          
      }
    } // end array type case

    if (isString()) {
      return "jstring";
    }

    return "jobject";
  }

  public boolean isNIOBuffer() {
    return (clazz == java.nio.Buffer.class ||
            clazz == java.nio.ByteBuffer.class);
  }

  public boolean isNIOByteBuffer() {
    return (clazz == java.nio.ByteBuffer.class);
  }

  public boolean isNIOByteBufferArray() {
    return (this == nioByteBufferArrayType);
  }

  public boolean isString() {
    return (clazz == java.lang.String.class);
  }

  public boolean isArray() {
    return ((clazz != null) && clazz.isArray());
  }

  public boolean isPrimitive() {
    return ((clazz != null) && !isArray() && clazz.isPrimitive() && (clazz != Void.TYPE));
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
  
  public boolean isVoidPointerType() {
    return (clazz == null && name == null && elementType == null);
  }

  public boolean isJNIEnv() {
    return clazz == null && name == "JNIEnv";
  }

  public Object clone() {
    JavaType clone = new JavaType();

    clone.clazz = this.clazz;
    clone.name = this.name;
    
    return clone;
  }

  public String toString() {
    return getName();
  }
  
  //----------------------------------------------------------------------
  // Internals only below this point
  //

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

  /**
   * Default constructor; the type is initialized to the equivalent of a
   * C-language "void *".
   */
  private JavaType() {
    
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
  
}

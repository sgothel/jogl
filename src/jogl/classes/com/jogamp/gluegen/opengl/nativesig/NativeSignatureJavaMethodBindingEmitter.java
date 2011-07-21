/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.gluegen.opengl.nativesig;

import com.jogamp.gluegen.JavaMethodBindingEmitter;
import com.jogamp.gluegen.JavaType;
import com.jogamp.gluegen.MethodBinding;
import com.jogamp.gluegen.cgram.types.Type;
import com.jogamp.gluegen.opengl.GLEmitter;
import com.jogamp.gluegen.opengl.GLJavaMethodBindingEmitter;
import com.jogamp.gluegen.procaddress.ProcAddressJavaMethodBindingEmitter;

import java.io.PrintWriter;

public class NativeSignatureJavaMethodBindingEmitter extends GLJavaMethodBindingEmitter {

  public NativeSignatureJavaMethodBindingEmitter(GLJavaMethodBindingEmitter methodToWrap) {
    super(methodToWrap);
  }

  public NativeSignatureJavaMethodBindingEmitter(ProcAddressJavaMethodBindingEmitter methodToWrap, GLEmitter emitter, boolean bufferObjectVariant) {
    super(methodToWrap, emitter, bufferObjectVariant);
  }

  public NativeSignatureJavaMethodBindingEmitter(JavaMethodBindingEmitter methodToWrap, NativeSignatureEmitter emitter) {
    super(methodToWrap, false, null, false, false, emitter);
  }

    @Override
  protected void emitSignature(PrintWriter writer) {
    writer.print(getBaseIndentString());
    emitNativeSignatureAnnotation(writer);
    super.emitSignature(writer);
  }

  protected void emitNativeSignatureAnnotation(PrintWriter writer) {
    if (hasModifier(JavaMethodBindingEmitter.NATIVE)) {
      // Emit everything as a leaf for now
      // FIXME: make this configurable
      writer.print("@NativeSignature(\"l");
      MethodBinding binding = getBinding();
      if (callThroughProcAddress) {
        writer.print("p");
      }
      writer.print("(");
      if (callThroughProcAddress) {
        writer.print("P");
      }
      for (int i = 0; i < binding.getNumArguments(); i++) {
        emitNativeSignatureElement(writer, binding.getJavaArgumentType(i), binding.getCArgumentType(i), i);
      }
      writer.print(")");
      emitNativeSignatureElement(writer, binding.getJavaReturnType(), binding.getCReturnType(), -1);
      writer.println("\")");
    }
  }

  protected void emitNativeSignatureElement(PrintWriter writer, JavaType type, Type cType, int index) {
    if (type.isVoid()) {
      if (index > 0) {
        throw new InternalError("Error parsing arguments -- void should not be seen aside from argument 0");
      }
      return;
    }

    if (type.isNIOBuffer()) {
      writer.print("A");
    } else if (type.isPrimitiveArray()) {
      writer.print("MO");
    } else if (type.isPrimitive()) {
      Class clazz = type.getJavaClass();
      if      (clazz == Byte.TYPE)      { writer.print("B"); }
      else if (clazz == Character.TYPE) { writer.print("C"); }
      else if (clazz == Double.TYPE)    { writer.print("D"); }
      else if (clazz == Float.TYPE)     { writer.print("F"); }
      else if (clazz == Integer.TYPE)   { writer.print("I"); }
      else if (clazz == Long.TYPE)      {
        // See if this is intended to be a pointer at the C level
        if (cType.isPointer()) {
          writer.print("A");
        } else {
          writer.print("J");
        }
      }
      else if (clazz == Short.TYPE)     { writer.print("S"); }
      else if (clazz == Boolean.TYPE)   { writer.print("Z"); }
      else throw new InternalError("Unhandled primitive type " + clazz);
    } else if (type.isString()) {
      writer.print("A");
    } else {
      throw new RuntimeException("Type not yet handled: " + type);
    }
  }

  protected String getReturnTypeString(boolean skipArray) {
    if (isForImplementingMethodCall()) {
      JavaType returnType = getBinding().getJavaReturnType();
      if (returnType.isString() || returnType.isNIOByteBuffer()) {
        // Treat these as addresses
        return "long";
      }
    }
    return super.getReturnTypeString(skipArray);
  }

  protected void emitPreCallSetup(MethodBinding binding, PrintWriter writer) {
    super.emitPreCallSetup(binding, writer);
    for (int i = 0; i < binding.getNumArguments(); i++) {
      JavaType type = binding.getJavaArgumentType(i);
      if (type.isNIOBuffer() && !directNIOOnly) {
        // Emit declarations for variables holding primitive arrays as type Object
        // We don't know 100% sure we're going to use these at this point in the code, though
        writer.println("  Object " + getNIOBufferArrayName(i) + " = (_direct ? null : Buffers.getArray(" +
                       getArgumentName(i) + "));");
      } else if (type.isString()) {
        writer.println("    long " + binding.getArgumentName(i) + "_c_str = BuffersInternal.newCString(" + binding.getArgumentName(i) + ");");
      }
      // FIXME: going to need more of these for Buffer[] and String[], at least
    }
  }

  protected String getNIOBufferArrayName(int argNumber) {
    return "__buffer_array_" + argNumber;
  }

  protected int emitArguments(PrintWriter writer)
  {
    boolean needComma = false;
    int numEmitted = 0;

    if (callThroughProcAddress) {
      if (changeNameAndArguments) {
        writer.print("long procAddress");
        ++numEmitted;
        needComma = true;
      }
    }

    if (forImplementingMethodCall && binding.hasContainingType()) {
      if (needComma) {
        writer.print(", ");
      }

      // Always emit outgoing "this" argument
      writer.print("long ");
      writer.print(javaThisArgumentName());      
      ++numEmitted;
      needComma = true;
    }

    for (int i = 0; i < binding.getNumArguments(); i++) {
      JavaType type = binding.getJavaArgumentType(i);
      if (type.isVoid()) { 
        // Make sure this is the only param to the method; if it isn't,
        // there's something wrong with our parsing of the headers.
        if (binding.getNumArguments() != 1) {
          throw new InternalError(
            "\"void\" argument type found in " +
            "multi-argument function \"" + binding + "\"");
        }
        continue;
      } 

      if (type.isJNIEnv() || binding.isArgumentThisPointer(i)) {
        // Don't need to expose these at the Java level
        continue;
      }

      if (needComma) {
        writer.print(", ");
      }

      if (forImplementingMethodCall &&
          (forDirectBufferImplementation && type.isNIOBuffer() ||
           type.isString())) {
        // Direct Buffers and Strings go out as longs
        writer.print("long");
        // FIXME: will need more tests here to handle other constructs like String and direct Buffer arrays
      } else {
        writer.print(erasedTypeString(type, false));
      }
      writer.print(" ");
      writer.print(getArgumentName(i));

      ++numEmitted;
      needComma = true;

      // Add Buffer and array index offset arguments after each associated argument
      if (forIndirectBufferAndArrayImplementation) {
        if (type.isNIOBuffer()) {
          writer.print(", int " + byteOffsetArgName(i));
        } else if (type.isNIOBufferArray()) {
          writer.print(", int[] " + 
                       byteOffsetArrayArgName(i));
        }
      }

      // Add offset argument after each primitive array
      if (type.isPrimitiveArray()) {
        writer.print(", int " + offsetArgName(i));
      }
    }
    return numEmitted;
  }

  protected void emitReturnVariableSetupAndCall(MethodBinding binding, PrintWriter writer) {
    writer.print("    ");
    JavaType returnType = binding.getJavaReturnType();
    boolean needsResultAssignment = false;

    if (!returnType.isVoid()) {
      if (returnType.isCompoundTypeWrapper() ||
          returnType.isNIOByteBuffer()) {
        writer.println("java.nio.ByteBuffer _res;");
        needsResultAssignment = true;
      } else if (returnType.isArrayOfCompoundTypeWrappers()) {
        writer.println("java.nio.ByteBuffer[] _res;");
        needsResultAssignment = true;
      } else if (returnType.isString() || returnType.isNIOByteBuffer()) {
        writer.print(returnType);
        writer.println(" _res;");
        needsResultAssignment = true;
      } else {
        // Always assign to "_res" variable so we can clean up
        // outgoing String arguments, for example
        emitReturnType(writer);
        writer.println(" _res;");
        needsResultAssignment = true;
      }
    }

    if (binding.signatureCanUseIndirectNIO() && !directNIOOnly) {
      // Must generate two calls for this gated on whether the NIO
      // buffers coming in are all direct or indirect
      writer.println("if (_direct) {");
      writer.print  ("    ");
    }

    if (needsResultAssignment) {
      writer.print("  _res = ");
      if (returnType.isString()) {
        writer.print("BuffersInternal.newJavaString(");
      } else if (returnType.isNIOByteBuffer()) {
        writer.print("BuffersInternal.newDirectByteBuffer(");
      }
    } else {
      writer.print("  ");
      if (!returnType.isVoid()) {
        writer.print("return ");
      }
    }

    if (binding.signatureUsesJavaPrimitiveArrays() &&
        !binding.signatureCanUseIndirectNIO()) {
      // FIXME: what happens with a C function of the form
      //  void foo(int* arg0, void* arg1);
      // ?

      // Only one call being made in this body, going to indirect
      // buffer / array entry point
      emitCall(binding, writer);
      if (returnType.isString() || returnType.isNIOByteBuffer()) {
        writer.print(")");
      }
      writer.print(";");
      writer.println();
    } else {
      emitCall(binding, writer);
      if (returnType.isString() || returnType.isNIOByteBuffer()) {
        writer.print(")");
      }
      writer.print(";");
    }

    if (binding.signatureCanUseIndirectNIO() && !directNIOOnly) {
      // Must generate two calls for this gated on whether the NIO
      // buffers coming in are all direct or indirect
      writer.println();
      writer.println("    } else {");
      writer.print  ("    ");
      if (needsResultAssignment) {
        writer.print("    _res = ");
      } else {
        writer.print("  ");
        if (!returnType.isVoid()) {
          writer.print("return ");
        }
      }
      emitCall(binding, writer);
      writer.print(";");
      writer.println();
      writer.println("    }");
    } else {
      writer.println();
    }
    emitPrologueOrEpilogue(epilogue, writer);
    if (needsResultAssignment) {
      emitCallResultReturn(binding, writer);
    }
  }

  protected int emitCallArguments(MethodBinding binding, PrintWriter writer, boolean direct) {
    // Note that we override this completely because we both need to
    // move the potential location of the outgoing proc address as
    // well as change the way we pass out Buffers, arrays, Strings, etc.

    boolean needComma = false;
    int numArgsEmitted = 0;

    if (callThroughProcAddress) {
      writer.print("__addr_");
      needComma = true;
      ++numArgsEmitted;
    }

    if (binding.hasContainingType()) {
      // Emit this pointer
      assert(binding.getContainingType().isCompoundTypeWrapper());
      writer.print("BuffersInternal.getDirectBufferAddress(");
      writer.print("getBuffer()");
      writer.print(")");
      needComma = true;
      ++numArgsEmitted;
    }
    for (int i = 0; i < binding.getNumArguments(); i++) {
      JavaType type = binding.getJavaArgumentType(i);
      if (type.isJNIEnv() || binding.isArgumentThisPointer(i)) {
        // Don't need to expose these at the Java level
        continue;
      }

      if (type.isVoid()) {
        // Make sure this is the only param to the method; if it isn't,
        // there's something wrong with our parsing of the headers.
        assert(binding.getNumArguments() == 1);
        continue;
      } 

      if (needComma) {
        writer.print(", ");
      }

      if (type.isCompoundTypeWrapper()) {
        writer.print("BuffersInternal.getDirectBufferAddress(");
        writer.print("((");
      }

      if (type.isNIOBuffer()) {
        if (!direct) {
          writer.print(getNIOBufferArrayName(i));
        } else {
          writer.print("BuffersInternal.getDirectBufferAddress(");
          writer.print(getArgumentName(i));
          writer.print(")");
        }
      } else {
        writer.print(getArgumentName(i));
      }

      if (type.isCompoundTypeWrapper()) {
        writer.print(" == null) ? null : ");
        writer.print(getArgumentName(i));
        writer.print(".getBuffer())");
        writer.print(")");
      }

      if (type.isNIOBuffer()) {
        if (direct) {
          writer.print("+ Buffers.getDirectBufferByteOffset(" + getArgumentName(i) + ")");
        } else {
          writer.print(", BuffersInternal.arrayBaseOffset(" +
                       getNIOBufferArrayName(i) +
                       ") + Buffers.getIndirectBufferByteOffset(" + getArgumentName(i) + ")");
        }
      } else if (type.isNIOBufferArray()) {
        writer.print(", " + byteOffsetArrayArgName(i));
      }

      // Add Array offset parameter for primitive arrays
      if (type.isPrimitiveArray()) {
        writer.print(", ");
        writer.print("BuffersInternal.arrayBaseOffset(" + getArgumentName(i) + ") + ");
        if(type.isFloatArray()) {
          writer.print("Buffers.SIZEOF_FLOAT * ");
        } else if(type.isDoubleArray()) {
          writer.print("Buffers.SIZEOF_DOUBLE * ");
        } else if(type.isByteArray()) {
          writer.print("1 * ");
        } else if(type.isLongArray()) {
          writer.print("Buffers.SIZEOF_LONG * ");
        } else if(type.isShortArray()) {
          writer.print("Buffers.SIZEOF_SHORT * ");
        } else if(type.isIntArray()) {
          writer.print("Buffers.SIZEOF_INT * ");
        } else {
          throw new RuntimeException("Unsupported type for calculating array offset argument for " +
                                     getArgumentName(i) +
                                     "-- error occurred while processing Java glue code for " + getName());
        }
        writer.print(offsetArgName(i));
      }

      if (type.isString()) {
        writer.print("_c_str");
      }

      if (type.isCompoundTypeWrapper()) {
        writer.print(")");
      }

      needComma = true;
      ++numArgsEmitted;
    }
    return numArgsEmitted;
  }

  protected void emitCallResultReturn(MethodBinding binding, PrintWriter writer) {
    for (int i = 0; i < binding.getNumArguments(); i++) {
      JavaType type = binding.getJavaArgumentType(i);
      if (type.isString()) {
        writer.println(";");
        writer.println("    BuffersInternal.freeCString(" + binding.getArgumentName(i) + "_c_str);");
      }
      // FIXME: will need more of these cleanups for things like Buffer[] and String[] (see above)
    }

    super.emitCallResultReturn(binding, writer);
  }

  public String getName() {
    String res = super.getName();
    if (forImplementingMethodCall && bufferObjectVariant) {
      return res + "BufObj";
    }
    return res;
  }

  protected String getImplMethodName(boolean direct) {
    String name = null;
    if (direct) {
      name = binding.getName() + "$0";
    } else {
      name = binding.getName() + "$1";
    }
    if (bufferObjectVariant) {
      return name + "BufObj";
    }
    return name;
  }
}

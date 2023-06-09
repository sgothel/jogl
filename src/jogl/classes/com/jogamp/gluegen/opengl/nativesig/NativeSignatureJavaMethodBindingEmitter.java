/*
 * Copyright (c) 2010-2023 JogAmp Community. All rights reserved.
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
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

import com.jogamp.gluegen.GlueGenException;
import com.jogamp.gluegen.JavaMethodBindingEmitter;
import com.jogamp.gluegen.JavaType;
import com.jogamp.gluegen.MethodBinding;
import com.jogamp.gluegen.cgram.types.Type;
import com.jogamp.gluegen.opengl.GLEmitter;
import com.jogamp.gluegen.opengl.GLJavaMethodBindingEmitter;
import com.jogamp.gluegen.procaddress.ProcAddressJavaMethodBindingEmitter;

/** Review: This Package/Class is not used and subject to be deleted. */
public class NativeSignatureJavaMethodBindingEmitter extends GLJavaMethodBindingEmitter {

  public NativeSignatureJavaMethodBindingEmitter(final GLJavaMethodBindingEmitter methodToWrap) {
    super(methodToWrap);
  }

  public NativeSignatureJavaMethodBindingEmitter(final ProcAddressJavaMethodBindingEmitter methodToWrap, final GLEmitter emitter, final boolean bufferObjectVariant) {
    super(methodToWrap, emitter, bufferObjectVariant);
  }

  public NativeSignatureJavaMethodBindingEmitter(final JavaMethodBindingEmitter methodToWrap, final NativeSignatureEmitter emitter) {
    super(methodToWrap, false, null, false, false, emitter);
  }

    @Override
  protected void emitSignature() {
    unit.emit(getBaseIndentString());
    emitNativeSignatureAnnotation();
    super.emitSignature();
  }

  protected void emitNativeSignatureAnnotation() {
    if (hasModifier(JavaMethodBindingEmitter.NATIVE)) {
      // Emit everything as a leaf for now
      // FIXME: make this configurable
      unit.emit("@NativeSignature(\"l");
      final MethodBinding binding = getBinding();
      if (callThroughProcAddress) {
        unit.emit("p");
      }
      unit.emit("(");
      if (callThroughProcAddress) {
        unit.emit("P");
      }
      for (int i = 0; i < binding.getNumArguments(); i++) {
        emitNativeSignatureElement(binding.getJavaArgumentType(i), binding.getCArgumentType(i), i);
      }
      unit.emit(")");
      emitNativeSignatureElement(binding.getJavaReturnType(), binding.getCReturnType(), -1);
      unit.emitln("\")");
    }
  }

  protected void emitNativeSignatureElement(final JavaType type, final Type cType, final int index) {
    if (type.isVoid()) {
      if (index > 0) {
        throw new InternalError("Error parsing arguments -- void should not be seen aside from argument 0");
      }
      return;
    }

    if (type.isNIOBuffer()) {
      unit.emit("A");
    } else if (type.isPrimitiveArray()) {
      unit.emit("MO");
    } else if (type.isPrimitive()) {
      final Class<?> clazz = type.getJavaClass();
      if      (clazz == Byte.TYPE)      { unit.emit("B"); }
      else if (clazz == Character.TYPE) { unit.emit("C"); }
      else if (clazz == Double.TYPE)    { unit.emit("D"); }
      else if (clazz == Float.TYPE)     { unit.emit("F"); }
      else if (clazz == Integer.TYPE)   { unit.emit("I"); }
      else if (clazz == Long.TYPE)      {
        // See if this is intended to be a pointer at the C level
        if (cType.isPointer()) {
          unit.emit("A");
        } else {
          unit.emit("J");
        }
      }
      else if (clazz == Short.TYPE)     { unit.emit("S"); }
      else if (clazz == Boolean.TYPE)   { unit.emit("Z"); }
      else throw new InternalError("Unhandled primitive type " + clazz);
    } else if (type.isString()) {
      unit.emit("A");
    } else {
      throw new RuntimeException("Type not yet handled: " + type);
    }
  }

  @Override
  protected String getReturnTypeString(final boolean skipArray) {
    if (isPrivateNativeMethod()) {
      final JavaType returnType = getBinding().getJavaReturnType();
      if (returnType.isString() || returnType.isNIOByteBuffer()) {
        // Treat these as addresses
        return "long";
      }
    }
    return super.getReturnTypeString(skipArray);
  }

  @Override
  protected void emitPreCallSetup(final MethodBinding binding) {
    super.emitPreCallSetup(binding);
    for (int i = 0; i < binding.getNumArguments(); i++) {
      final JavaType type = binding.getJavaArgumentType(i);
      if (type.isNIOBuffer() && !useNIODirectOnly ) {
        // Emit declarations for variables holding primitive arrays as type Object
        // We don't know 100% sure we're going to use these at this point in the code, though
        unit.emitln("  Object " + getNIOBufferArrayName(i) + " = (_direct ? null : Buffers.getArray(" +
                       getArgumentName(i) + "));");
      } else if (type.isString()) {
        unit.emitln("    long " + binding.getArgumentName(i) + "_c_str = BuffersInternal.newCString(" + binding.getArgumentName(i) + ");");
      }
      // FIXME: going to need more of these for Buffer[] and String[], at least
    }
  }

  protected String getNIOBufferArrayName(final int argNumber) {
    return "__buffer_array_" + argNumber;
  }

  @Override
  protected int emitArguments()
  {
    boolean needComma = false;
    int numEmitted = 0;

    if (callThroughProcAddress) {
      if (changeNameAndArguments) {
        unit.emit("long procAddress");
        ++numEmitted;
        needComma = true;
      }
    }

    if (isPrivateNativeMethod() && binding.hasContainingType()) {
      if (needComma) {
        unit.emit(", ");
      }

      // Always emit outgoing "this" argument
      unit.emit("long ");
      unit.emit(javaThisArgumentName());
      ++numEmitted;
      needComma = true;
    }

    for (int i = 0; i < binding.getNumArguments(); i++) {
      final JavaType type = binding.getJavaArgumentType(i);
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
        unit.emit(", ");
      }

      if (isPrivateNativeMethod() &&
          (isForDirectBufferImplementation() && type.isNIOBuffer() ||
           type.isString())) {
        // Direct Buffers and Strings go out as longs
        unit.emit("long");
        // FIXME: will need more tests here to handle other constructs like String and direct Buffer arrays
      } else {
        unit.emit(erasedTypeString(type, false));
      }
      unit.emit(" ");
      unit.emit(getArgumentName(i));

      ++numEmitted;
      needComma = true;

      // Add Buffer and array index offset arguments after each associated argument
      if (isForIndirectBufferAndArrayImplementation()) {
        if (type.isNIOBuffer()) {
          unit.emit(", int " + byteOffsetArgName(i));
        } else if (type.isNIOBufferArray()) {
          unit.emit(", int[] " +
                       byteOffsetArrayArgName(i));
        }
      }

      // Add offset argument after each primitive array
      if (type.isPrimitiveArray()) {
        unit.emit(", int " + offsetArgName(i));
      }
    }
    return numEmitted;
  }

  @Override
  protected void emitReturnVariableSetupAndCall(final MethodBinding binding) {
    unit.emit("    ");
    final JavaType returnType = binding.getJavaReturnType();
    boolean needsResultAssignment = false;

    if (!returnType.isVoid()) {
      if (returnType.isCompoundTypeWrapper() ||
          returnType.isNIOByteBuffer()) {
        unit.emitln("final java.nio.ByteBuffer _res;");
        needsResultAssignment = true;
      } else if (returnType.isArrayOfCompoundTypeWrappers()) {
        unit.emitln("final java.nio.ByteBuffer[] _res;");
        needsResultAssignment = true;
      } else if (returnType.isString() || returnType.isNIOByteBuffer()) {
        unit.emit("final ");
        unit.emit(returnType.toString());
        unit.emitln(" _res;");
        needsResultAssignment = true;
      } else {
        // Always assign to "_res" variable so we can clean up
        // outgoing String arguments, for example
        unit.emit("final ");
        emitReturnType();
        unit.emitln(" _res;");
        needsResultAssignment = true;
      }
    }

    if (binding.signatureCanUseIndirectNIO() && !useNIODirectOnly) {
      // Must generate two calls for this gated on whether the NIO
      // buffers coming in are all direct or indirect
      unit.emitln("if (_direct) {");
      unit.emit  ("    ");
    }

    if (needsResultAssignment) {
      unit.emit("  _res = ");
      if (returnType.isString()) {
        unit.emit("BuffersInternal.newJavaString(");
      } else if (returnType.isNIOByteBuffer()) {
        unit.emit("BuffersInternal.newDirectByteBuffer(");
      }
    } else {
      unit.emit("  ");
      if (!returnType.isVoid()) {
        unit.emit("return ");
      }
    }

    if (binding.signatureUsesJavaPrimitiveArrays() &&
        !binding.signatureCanUseIndirectNIO()) {
      // FIXME: what happens with a C function of the form
      //  void foo(int* arg0, void* arg1);
      // ?

      // Only one call being made in this body, going to indirect
      // buffer / array entry point
      emitCall(binding);
      if (returnType.isString() || returnType.isNIOByteBuffer()) {
        unit.emit(")");
      }
      unit.emit(";");
      unit.emitln();
    } else {
      emitCall(binding);
      if (returnType.isString() || returnType.isNIOByteBuffer()) {
        unit.emit(")");
      }
      unit.emit(";");
    }

    if (binding.signatureCanUseIndirectNIO() && !useNIODirectOnly) {
      // Must generate two calls for this gated on whether the NIO
      // buffers coming in are all direct or indirect
      unit.emitln();
      unit.emitln("    } else {");
      unit.emit  ("    ");
      if (needsResultAssignment) {
        unit.emit("    _res = ");
      } else {
        unit.emit("  ");
        if (!returnType.isVoid()) {
          unit.emit("return ");
        }
      }
      emitCall(binding);
      unit.emit(";");
      unit.emitln();
      unit.emitln("    }");
    } else {
      unit.emitln();
    }
    emitPrologueOrEpilogue(epilogue);
    if (needsResultAssignment) {
      emitCallResultReturn(binding);
    }
  }

  protected int emitCallArguments(final MethodBinding binding, final boolean direct) {
    // Note that we override this completely because we both need to
    // move the potential location of the outgoing proc address as
    // well as change the way we pass out Buffers, arrays, Strings, etc.

    boolean needComma = false;
    int numArgsEmitted = 0;

    if (callThroughProcAddress) {
      unit.emit("__addr_");
      needComma = true;
      ++numArgsEmitted;
    }

    if (binding.hasContainingType()) {
      // Emit this pointer
      assert(binding.getContainingType().isCompoundTypeWrapper());
      unit.emit("BuffersInternal.getDirectBufferAddress(");
      unit.emit("getBuffer()");
      unit.emit(")");
      needComma = true;
      ++numArgsEmitted;
    }
    for (int i = 0; i < binding.getNumArguments(); i++) {
      final JavaType type = binding.getJavaArgumentType(i);
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
        unit.emit(", ");
      }

      if (type.isCompoundTypeWrapper()) {
        unit.emit("BuffersInternal.getDirectBufferAddress(");
        unit.emit("((");
      }

      if (type.isNIOBuffer()) {
        if (!direct) {
          unit.emit(getNIOBufferArrayName(i));
        } else {
          unit.emit("BuffersInternal.getDirectBufferAddress(");
          unit.emit(getArgumentName(i));
          unit.emit(")");
        }
      } else {
        unit.emit(getArgumentName(i));
      }

      if (type.isCompoundTypeWrapper()) {
        unit.emit(" == null) ? null : ");
        unit.emit(getArgumentName(i));
        unit.emit(".getBuffer())");
        unit.emit(")");
      }

      if (type.isNIOBuffer()) {
        if (direct) {
          unit.emit("+ Buffers.getDirectBufferByteOffset(" + getArgumentName(i) + ")");
        } else {
          unit.emit(", BuffersInternal.arrayBaseOffset(" +
                       getNIOBufferArrayName(i) +
                       ") + Buffers.getIndirectBufferByteOffset(" + getArgumentName(i) + ")");
        }
      } else if (type.isNIOBufferArray()) {
        unit.emit(", " + byteOffsetArrayArgName(i));
      }

      // Add Array offset parameter for primitive arrays
      if (type.isPrimitiveArray()) {
        unit.emit(", ");
        unit.emit("BuffersInternal.arrayBaseOffset(" + getArgumentName(i) + ") + ");
        if(type.isFloatArray()) {
          unit.emit("Buffers.SIZEOF_FLOAT * ");
        } else if(type.isDoubleArray()) {
          unit.emit("Buffers.SIZEOF_DOUBLE * ");
        } else if(type.isByteArray()) {
          unit.emit("1 * ");
        } else if(type.isLongArray()) {
          unit.emit("Buffers.SIZEOF_LONG * ");
        } else if(type.isShortArray()) {
          unit.emit("Buffers.SIZEOF_SHORT * ");
        } else if(type.isIntArray()) {
          unit.emit("Buffers.SIZEOF_INT * ");
        } else {
          throw new GlueGenException("Unsupported type for calculating array offset argument for " +
                                     getArgumentName(i) +
                                     "-- error occurred while processing Java glue code for " + binding.getCSymbol().getAliasedString(),
                                     binding.getCSymbol().getASTLocusTag());
        }
        unit.emit(offsetArgName(i));
      }

      if (type.isString()) {
        unit.emit("_c_str");
      }

      if (type.isCompoundTypeWrapper()) {
        unit.emit(")");
      }

      needComma = true;
      ++numArgsEmitted;
    }
    return numArgsEmitted;
  }

  @Override
  protected void emitCallResultReturn(final MethodBinding binding) {
    for (int i = 0; i < binding.getNumArguments(); i++) {
      final JavaType type = binding.getJavaArgumentType(i);
      if (type.isString()) {
        unit.emitln(";");
        unit.emitln("    BuffersInternal.freeCString(" + binding.getArgumentName(i) + "_c_str);");
      }
      // FIXME: will need more of these cleanups for things like Buffer[] and String[] (see above)
    }

    super.emitCallResultReturn(binding);
  }

  @Override
  public String getNativeName() {
    final String res = super.getNativeName();
    if (isPrivateNativeMethod() && bufferObjectVariant) {
      return res + "BufObj";
    }
    return res;
  }

  protected String getImplMethodName(final boolean direct) {
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

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

import java.io.*;
import java.util.*;
import java.text.MessageFormat;

import net.java.games.gluegen.cgram.types.*;

/** Emits the Java-side component of the Java<->C JNI binding. */
public class JavaMethodBindingImplEmitter extends JavaMethodBindingEmitter
{
  private boolean isUnimplemented;

  public JavaMethodBindingImplEmitter(MethodBinding binding, PrintWriter output, String runtimeExceptionType)
  {
    this(binding, output, runtimeExceptionType, false);
  }

  public JavaMethodBindingImplEmitter(MethodBinding binding,
                                      PrintWriter output,
                                      String runtimeExceptionType,
                                      boolean isUnimplemented)
  {
    super(binding, output, runtimeExceptionType);
    setCommentEmitter(defaultJavaCommentEmitter);
    this.isUnimplemented = isUnimplemented;
  }

  public JavaMethodBindingImplEmitter(JavaMethodBindingEmitter arg) {
    super(arg);
    if (arg instanceof JavaMethodBindingImplEmitter) {
      this.isUnimplemented = ((JavaMethodBindingImplEmitter) arg).isUnimplemented;
    }
  }

  protected void emitBody(PrintWriter writer)
  {    
    MethodBinding binding = getBinding();
    if (needsBody()) {
      writer.println();
      writer.println("  {");
      if (isUnimplemented) {
        writer.println("    throw new " + getRuntimeExceptionType() + "(\"Unimplemented\");");
      } else {
        emitPreCallSetup(binding, writer);
        emitReturnVariableSetup(binding, writer);
        emitCall(binding, writer);
      }
      writer.println("  }");
    } else {
      writer.println(";");
    }
  }

  protected boolean isUnimplemented() {
    return isUnimplemented;
  }

  protected boolean needsBody() {
    return (isUnimplemented ||
            getBinding().signatureUsesNIO() ||
            getBinding().signatureUsesCArrays() ||
            getBinding().hasContainingType());
  }

  protected void emitPreCallSetup(MethodBinding binding, PrintWriter writer) {
    emitArrayLengthAndNIOBufferChecks(binding, writer);
  }

  protected void emitArrayLengthAndNIOBufferChecks(MethodBinding binding, PrintWriter writer) {
    // Check lengths of any incoming arrays if necessary
    for (int i = 0; i < binding.getNumArguments(); i++) {
      Type type = binding.getCArgumentType(i);
      if (type.isArray()) {
        ArrayType arrayType = type.asArray();
        writer.println("    if (" + binding.getArgumentName(i) + ".length < " + arrayType.getLength() + ")");
        writer.println("      throw new " + getRuntimeExceptionType() + "(\"Length of array \\\"" + binding.getArgumentName(i) +
                       "\\\" was less than the required " + arrayType.getLength() + "\");");
      } else {
        JavaType javaType = binding.getJavaArgumentType(i);
        if (javaType.isNIOBuffer()) {
          writer.println("    if (!BufferFactory.isDirect(" + binding.getArgumentName(i) + "))");
          writer.println("      throw new " + getRuntimeExceptionType() + "(\"Argument \\\"" +
                         binding.getArgumentName(i) + "\\\" was not a direct buffer\");");
        } else if (javaType.isNIOBufferArray()) {
          String argName = binding.getArgumentName(i);
          // Check direct buffer properties of all buffers within
          writer.println("    if (" + argName + " != null) {");
          writer.println("      for (int _ctr = 0; _ctr < " + argName + ".length; _ctr++) {");
          writer.println("        if (!BufferFactory.isDirect(" + argName + "[_ctr])) {");
          writer.println("          throw new " + getRuntimeExceptionType() + "(\"Element \" + _ctr + \" of argument \\\"" +
                         binding.getArgumentName(i) + "\\\" was not a direct buffer\");");
          writer.println("        }");
          writer.println("      }");
          writer.println("    }");
        }
      }
    }
  }

  protected void emitReturnVariableSetup(MethodBinding binding, PrintWriter writer) {
    writer.print("    ");
    JavaType returnType = binding.getJavaReturnType();
    if (!returnType.isVoid()) {
      if (returnType.isCompoundTypeWrapper() ||
          returnType.isNIOByteBuffer()) {
        writer.println("ByteBuffer _res;");
        writer.print("    _res = ");
      } else if (returnType.isArrayOfCompoundTypeWrappers()) {
        writer.println("ByteBuffer[] _res;");
        writer.print("    _res = ");
      } else {
        writer.print("return ");
      }
    }
  }

  protected void emitCall(MethodBinding binding, PrintWriter writer) {
    writer.print(getImplMethodName());
    writer.print("(");
    emitCallArguments(binding, writer);
    writer.print(")");
    emitCallResultReturn(binding, writer);
  }
  
  protected int emitCallArguments(MethodBinding binding, PrintWriter writer) {
    boolean needComma = false;
    int numArgsEmitted = 0;
    if (binding.hasContainingType()) {
      // Emit this pointer
      assert(binding.getContainingType().isCompoundTypeWrapper());
      writer.print("getBuffer()");
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
        writer.print("((");
      }
      writer.print(binding.getArgumentName(i));
      if (type.isCompoundTypeWrapper()) {
        writer.print(" == null) ? null : ");
        writer.print(binding.getArgumentName(i));
        writer.print(".getBuffer())");
      }
      needComma = true;
      ++numArgsEmitted;
    }
    return numArgsEmitted;
  }

  protected void emitCallResultReturn(MethodBinding binding, PrintWriter writer) {
    JavaType returnType = binding.getJavaReturnType();
    if (returnType.isCompoundTypeWrapper()) {
      writer.println(";");
      String fmt = getReturnedArrayLengthExpression();
      writer.println("    if (_res == null) return null;");
      if (fmt == null) {
        writer.print("    return new " + returnType.getName() + "(_res.order(ByteOrder.nativeOrder()))");
      } else {
        writer.println("    _res.order(ByteOrder.nativeOrder());");
        String[] argumentNames = new String[binding.getNumArguments()];
        for (int i = 0; i < binding.getNumArguments(); i++) {
          argumentNames[i] = binding.getArgumentName(i);
        }
        String expr = new MessageFormat(fmt).format(argumentNames);
        PointerType cReturnTypePointer = binding.getCReturnType().asPointer();
        CompoundType cReturnType = null;
        if (cReturnTypePointer != null) {
          cReturnType = cReturnTypePointer.getTargetType().asCompound();
        }
        if (cReturnType == null) {
          throw new RuntimeException("ReturnedArrayLength directive currently only supported for pointers to compound types " +
                                     "(error occurred while generating Java glue code for " + binding.getName() + ")");
        }
        writer.println("    " + getReturnTypeString(false) + " _retarray = new " + getReturnTypeString(true) + "[" + expr + "];");
        writer.println("    for (int _count = 0; _count < " + expr + "; _count++) {");
        // Create temporary ByteBuffer slice
        // FIXME: probably need Type.getAlignedSize() for arrays of
        // compound types (rounding up to machine-dependent alignment)
        writer.println("      _res.position(_count * " + cReturnType.getSize() + ");");
        writer.println("      _res.limit   ((1 + _count) * " + cReturnType.getSize() + ");");
        writer.println("      ByteBuffer _tmp = _res.slice();");
        writer.println("      _tmp.order(ByteOrder.nativeOrder());");
        writer.println("      _res.position(0);");
        writer.println("      _res.limit(_res.capacity());");
        writer.println("      _retarray[_count] = new " + getReturnTypeString(true) + "(_tmp);");
        writer.println("    }");
        writer.print  ("    return _retarray");
      }
    } else if (returnType.isNIOBuffer()) {
      writer.println(";");
      writer.println("    if (_res == null) return null;");
      writer.print("    return _res.order(ByteOrder.nativeOrder())");
    } else if (returnType.isArrayOfCompoundTypeWrappers()) {
      writer.println(";");
      writer.println("    if (_res == null) return null;");
      writer.println("    " + getReturnTypeString(false) + " _retarray = new " + getReturnTypeString(true) + "[_res.length];");
      writer.println("    for (int _count = 0; _count < _res.length; _count++) {");
      writer.println("      _retarray[_count] = new " + getReturnTypeString(true) + "(_res[_count]);");
      writer.println("    }");
      writer.print  ("    return _retarray");
    }
    writer.println(";");
  }
}


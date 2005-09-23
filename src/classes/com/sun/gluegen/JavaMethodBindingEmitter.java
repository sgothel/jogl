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

import java.io.*;
import java.util.*;
import java.text.MessageFormat;

import com.sun.gluegen.cgram.types.*;
import com.sun.gluegen.cgram.*;

/**
 * An emitter that emits only the interface for a Java<->C JNI binding.
 */
public class JavaMethodBindingEmitter extends FunctionEmitter
{
  public static final EmissionModifier PUBLIC = new EmissionModifier("public");
  public static final EmissionModifier PROTECTED = new EmissionModifier("protected");
  public static final EmissionModifier PRIVATE = new EmissionModifier("private");
  public static final EmissionModifier ABSTRACT = new EmissionModifier("abstract");
  public static final EmissionModifier FINAL = new EmissionModifier("final");
  public static final EmissionModifier NATIVE = new EmissionModifier("native");
  public static final EmissionModifier SYNCHRONIZED = new EmissionModifier("synchronized");

  protected final CommentEmitter defaultJavaCommentEmitter = new DefaultCommentEmitter();
  protected final CommentEmitter defaultInterfaceCommentEmitter =
    new InterfaceCommentEmitter();
  
  // Exception type raised in the generated code if runtime checks fail
  private String runtimeExceptionType;

  protected boolean emitBody;
  protected boolean eraseBufferAndArrayTypes;
  protected boolean directNIOOnly;
  protected boolean forImplementingMethodCall;
  protected boolean forDirectBufferImplementation;
  protected boolean forIndirectBufferAndArrayImplementation;
  protected boolean isUnimplemented;

  protected MethodBinding binding;

  // Manually-specified prologue and epilogue code
  protected List/*<String>*/ prologue;
  protected List/*<String>*/ epilogue;

  // A non-null value indicates that rather than returning a compound
  // type accessor we are returning an array of such accessors; this
  // expression is a MessageFormat string taking the names of the
  // incoming Java arguments as parameters and computing as an int the
  // number of elements of the returned array.
  private String returnedArrayLengthExpression;

  public JavaMethodBindingEmitter(MethodBinding binding,
                                  PrintWriter output,
                                  String runtimeExceptionType,
                                  boolean emitBody,
                                  boolean eraseBufferAndArrayTypes,
                                  boolean directNIOOnly,
                                  boolean forImplementingMethodCall,
                                  boolean forDirectBufferImplementation,
                                  boolean forIndirectBufferAndArrayImplementation,
                                  boolean isUnimplemented)
  {
    super(output);
    this.binding = binding;
    this.runtimeExceptionType = runtimeExceptionType;
    this.emitBody = emitBody;
    this.eraseBufferAndArrayTypes = eraseBufferAndArrayTypes;
    this.directNIOOnly = directNIOOnly;
    this.forImplementingMethodCall = forImplementingMethodCall;
    this.forDirectBufferImplementation = forDirectBufferImplementation;
    this.forIndirectBufferAndArrayImplementation = forIndirectBufferAndArrayImplementation;
    this.isUnimplemented = isUnimplemented;
    if (forImplementingMethodCall) {
      setCommentEmitter(defaultJavaCommentEmitter);
    } else {
      setCommentEmitter(defaultInterfaceCommentEmitter);
    }
  }
  
  public JavaMethodBindingEmitter(JavaMethodBindingEmitter arg) {
    super(arg);
    binding                       = arg.binding;
    runtimeExceptionType          = arg.runtimeExceptionType;
    emitBody                      = arg.emitBody;
    eraseBufferAndArrayTypes      = arg.eraseBufferAndArrayTypes;
    directNIOOnly                 = arg.directNIOOnly;
    forImplementingMethodCall     = arg.forImplementingMethodCall;
    forDirectBufferImplementation = arg.forDirectBufferImplementation;
    forIndirectBufferAndArrayImplementation = arg.forIndirectBufferAndArrayImplementation;
    isUnimplemented               = arg.isUnimplemented;
    returnedArrayLengthExpression = arg.returnedArrayLengthExpression;
    prologue                      = arg.prologue;
    epilogue                      = arg.epilogue;
  }

  public final MethodBinding getBinding() { return binding; }

  public boolean isForImplementingMethodCall() { return forImplementingMethodCall; }

  public String getName() {
    return binding.getRenamedMethodName();
  }

  protected String getArgumentName(int i) {
    return binding.getArgumentName(i);
  }

  /** The type of exception (must subclass
      <code>java.lang.RuntimeException</code>) raised if runtime
      checks fail in the generated code. */
  public String getRuntimeExceptionType() {
    return runtimeExceptionType;
  }

  /** If the underlying function returns an array (currently only
      arrays of compound types are supported) as opposed to a pointer
      to an object, this method should be called to provide a
      MessageFormat string containing an expression that computes the
      number of elements of the returned array. The parameters to the
      MessageFormat expression are the names of the incoming Java
      arguments. */
  public void setReturnedArrayLengthExpression(String expr) {
    returnedArrayLengthExpression = expr;
  }

  /** Sets the manually-generated prologue code for this emitter. */
  public void setPrologue(List/*<String>*/ prologue) {
    this.prologue = prologue;
  }

  /** Sets the manually-generated epilogue code for this emitter. */
  public void setEpilogue(List/*<String>*/ epilogue) {
    this.epilogue = epilogue;
  }

  /** Indicates whether this emitter will print only a signature, or
      whether it will emit Java code for the body of the method as
      well. */
  public boolean signatureOnly() {
    return !emitBody;
  }

  /** Accessor for subclasses. */
  public void setEmitBody(boolean emitBody) {
    this.emitBody = emitBody;
  }

  /** Accessor for subclasses. */
  public void setEraseBufferAndArrayTypes(boolean erase) {
    this.eraseBufferAndArrayTypes = erase;
  }

  /** Accessor for subclasses. */
  public void setForImplementingMethodCall(boolean impl) {
    this.forImplementingMethodCall = impl;
  }

  protected void emitReturnType(PrintWriter writer)
  {    
    writer.print(getReturnTypeString(false));
  }

  protected String erasedTypeString(JavaType type, boolean skipBuffers) {
    if (eraseBufferAndArrayTypes) {
      if (type.isNIOBuffer() ||
          type.isPrimitiveArray()) {
        if (!skipBuffers) {
          // Direct buffers and arrays sent down as Object (but
          // returned as e.g. ByteBuffer)
          return "Object";
        }
      } else if (type.isCompoundTypeWrapper()) {
        // Compound type wrappers are unwrapped to ByteBuffer
        return "java.nio.ByteBuffer";
      } else if (type.isArrayOfCompoundTypeWrappers()) {
        return "java.nio.ByteBuffer";
      }
    }
    return type.getName();
  }

  protected String getReturnTypeString(boolean skipArray) {
    // The first arm of the "if" clause is used by the glue code
    // generation for arrays of compound type wrappers
    if (skipArray ||
    // The following arm is used by most other kinds of return types
        (getReturnedArrayLengthExpression() == null && 
         !binding.getJavaReturnType().isArrayOfCompoundTypeWrappers()) ||
    // The following arm is used specifically to get the splitting up
    // of one returned ByteBuffer into an array of compound type
    // wrappers to work (e.g., XGetVisualInfo)
        (eraseBufferAndArrayTypes &&
         binding.getJavaReturnType().isCompoundTypeWrapper() &&
         (getReturnedArrayLengthExpression() != null))) {
      return erasedTypeString(binding.getJavaReturnType(), true);
    }
    return erasedTypeString(binding.getJavaReturnType(), true) + "[]";
  }

  protected void emitName(PrintWriter writer)
  {
    if (forImplementingMethodCall) {
      if (forIndirectBufferAndArrayImplementation) {
        writer.print(getImplMethodName(false));
      } else {
        writer.print(getImplMethodName(true));
      }
    } else {
      writer.print(getName());
    }
  }

  protected int emitArguments(PrintWriter writer)
  {
    boolean needComma = false;
    int numEmitted = 0;

    if (forImplementingMethodCall  && binding.hasContainingType()) {
      // Always emit outgoing "this" argument
      writer.print("java.nio.ByteBuffer ");
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

      writer.print(erasedTypeString(type, false));
      writer.print(" ");
      writer.print(getArgumentName(i));

      ++numEmitted;
      needComma = true;

      // Add Buffer and array index offset arguments after each associated argument
      if (forDirectBufferImplementation || forIndirectBufferAndArrayImplementation) {
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


  protected String getImplMethodName(boolean direct) {
    if (direct) {
      return binding.getRenamedMethodName() + "0";
    } else {
      return binding.getRenamedMethodName() + "1";
    }
  }

  protected String byteOffsetArgName(int i) {
    return byteOffsetArgName(getArgumentName(i));
  }

  protected String byteOffsetArgName(String s) {
    return s + "_byte_offset";
  }
                                                                                                            
  protected String byteOffsetArrayArgName(int i) {
    return getArgumentName(i) + "_byte_offset_array";
  }
                                                                                                            
  protected String offsetArgName(int i) {
    return getArgumentName(i) + "_offset";
  }

  protected void emitBody(PrintWriter writer)
  {
    if (!emitBody) {
      writer.println(';');
    } else {
      MethodBinding binding = getBinding();
      writer.println();
      writer.println("  {");
      if (isUnimplemented) {
        writer.println("    throw new " + getRuntimeExceptionType() + "(\"Unimplemented\");");
      } else {
        emitPrologueOrEpilogue(prologue, writer);
        emitPreCallSetup(binding, writer);
        //emitReturnVariableSetup(binding, writer);
        emitReturnVariableSetupAndCall(binding, writer);
        emitPrologueOrEpilogue(epilogue, writer);
      }
      writer.println("  }");
    }
  }

  protected void emitPrologueOrEpilogue(List/*<String>*/ code, PrintWriter writer) {
    if (code != null) {
      for (Iterator iter = code.iterator(); iter.hasNext(); ) {
        writer.println("    " + (String) iter.next());
      }
    }
  }

  protected void emitPreCallSetup(MethodBinding binding, PrintWriter writer) {
    emitArrayLengthAndNIOBufferChecks(binding, writer);
  }

  protected void emitArrayLengthAndNIOBufferChecks(MethodBinding binding, PrintWriter writer) {
    int numBufferOffsetArrayArgs = 0;
    boolean firstBuffer = true;
    // Check lengths of any incoming arrays if necessary
    for (int i = 0; i < binding.getNumArguments(); i++) {
      Type type = binding.getCArgumentType(i);
      if (type.isArray()) {
        ArrayType arrayType = type.asArray();
        writer.println("    if (" + getArgumentName(i) + ".length < " +
                       arrayType.getLength() + ")");
        writer.println("      throw new " + getRuntimeExceptionType() +
                       "(\"Length of array \\\"" + getArgumentName(i) +
                       "\\\" was less than the required " + arrayType.getLength() + "\");");
      } else {
        JavaType javaType = binding.getJavaArgumentType(i);
        if (javaType.isNIOBuffer()) {
          if (directNIOOnly) {
            writer.println("    if (!BufferFactory.isDirect(" + getArgumentName(i) + "))");
            writer.println("      throw new " + getRuntimeExceptionType() + "(\"Argument \\\"" +
                           getArgumentName(i) + "\\\" was not a direct buffer\");");
          } else {
            if(firstBuffer) {
              firstBuffer = false;
              writer.println("    boolean _direct = BufferFactory.isDirect(" + getArgumentName(i) + ");");
            } else {
              writer.println("    if (_direct != BufferFactory.isDirect(" + getArgumentName(i) + "))");
              writer.println("      throw new " + getRuntimeExceptionType() +
                             "(\"Argument \\\"" + getArgumentName(i) +
                             "\\\" : Buffers passed to this method must all be either direct or indirect\");");
            }
          }
        } else if (javaType.isNIOBufferArray()) {
          // All buffers passed down in an array of NIO buffers must be direct
          String argName = getArgumentName(i);
          String arrayName = byteOffsetArrayArgName(i);
          writer.println("    int[] " + arrayName + " = new int[" + argName + ".length];");
          // Check direct buffer properties of all buffers within
          writer.println("    if (" + argName + " != null) {");
          writer.println("      for (int _ctr = 0; _ctr < " + argName + ".length; _ctr++) {");
          writer.println("        if (!BufferFactory.isDirect(" + argName + "[_ctr])) {");
          writer.println("          throw new " + getRuntimeExceptionType() + 
                         "(\"Element \" + _ctr + \" of argument \\\"" +
                         getArgumentName(i) + "\\\" was not a direct buffer\");");
          writer.println("        }");
          // get the Buffer Array offset values and save them into another array to send down to JNI
          writer.print  ("        " + arrayName + "[_ctr] = BufferFactory.getDirectBufferByteOffset(");
          writer.println(argName + "[_ctr]);");
          writer.println("      }");
          writer.println("    }");
        } else if (javaType.isPrimitiveArray()) {
          String argName = getArgumentName(i);
          String offsetArg = offsetArgName(i);
          writer.println("    if(" + argName + " != null && " + argName + ".length <= " + offsetArg + ")");
          writer.print  ("      throw new " + getRuntimeExceptionType()); 
          writer.println("(\"array offset argument \\\"" + offsetArg + "\\\" (\" + " + offsetArg +
                         " + \") equals or exceeds array length (\" + " + argName + ".length + \")\");");
        }
      }
    }
  }

  protected void emitCall(MethodBinding binding, PrintWriter writer, boolean direct) {
    writer.print(getImplMethodName(direct));
    writer.print("(");
    emitCallArguments(binding, writer, direct);
    writer.print(");");
  }


  protected void emitReturnVariableSetupAndCall(MethodBinding binding, PrintWriter writer) {
    writer.print("    ");
    JavaType returnType = binding.getJavaReturnType();
    boolean needsResultAssignment = false;

    if (!returnType.isVoid()) {
      if (returnType.isCompoundTypeWrapper() ||
          returnType.isNIOByteBuffer()) {
        writer.println("ByteBuffer _res;");
        needsResultAssignment = true;
      } else if (returnType.isArrayOfCompoundTypeWrappers()) {
        writer.println("ByteBuffer[] _res;");
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
      emitCall(binding, writer, false);
      writer.println();
    } else {
      emitCall(binding, writer, true);
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
      emitCall(binding, writer, false);
      writer.println();
      writer.println("    }");
    } else {
      writer.println();
    }
    if (needsResultAssignment) {
      emitCallResultReturn(binding, writer);
    }
  }
    
  protected int emitCallArguments(MethodBinding binding, PrintWriter writer, boolean direct) {
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

      if (type.isNIOBuffer() && !direct) {
         writer.print("BufferFactory.getArray(" + getArgumentName(i) + ")");
      } else {
         writer.print(getArgumentName(i));
      }

      if (type.isCompoundTypeWrapper()) {
        writer.print(" == null) ? null : ");
        writer.print(getArgumentName(i));
        writer.print(".getBuffer())");
      }
      needComma = true;
      ++numArgsEmitted;
      if (type.isNIOBuffer()) {
        if (direct) {
          writer.print(", BufferFactory.getDirectBufferByteOffset(" + getArgumentName(i) + ")");
        } else {
          writer.print(", BufferFactory.getIndirectBufferByteOffset(" + getArgumentName(i) + ")");
        }
      } else if (type.isNIOBufferArray()) {
        writer.print(", " + byteOffsetArrayArgName(i));
      }

      // Add Array offset parameter for primitive arrays
      if (type.isPrimitiveArray()) {
        if(type.isFloatArray()) {
          writer.print(", BufferFactory.SIZEOF_FLOAT * ");
        } else if(type.isDoubleArray()) {
          writer.print(", BufferFactory.SIZEOF_DOUBLE * ");
        } else if(type.isByteArray()) {
          writer.print(", ");
        } else if(type.isLongArray()) {
          writer.print(", BufferFactory.SIZEOF_LONG * ");
        } else if(type.isShortArray()) {
          writer.print(", BufferFactory.SIZEOF_SHORT * ");
        } else if(type.isIntArray()) {
          writer.print(", BufferFactory.SIZEOF_INT * ");
        } else {
          throw new RuntimeException("Unsupported type for calculating array offset argument for " +
                                     getArgumentName(i) +
                                     "-- error occurred while processing Java glue code for " + getName());
        }
        writer.print(offsetArgName(i));
      }
    }
    return numArgsEmitted;
  }

  protected void emitCallResultReturn(MethodBinding binding, PrintWriter writer) {
    JavaType returnType = binding.getJavaReturnType();

    if (returnType.isCompoundTypeWrapper()) {
      String fmt = getReturnedArrayLengthExpression();
      writer.println("    if (_res == null) return null;");
      if (fmt == null) {
        writer.print("    return new " + returnType.getName() + "(_res.order(ByteOrder.nativeOrder()))");
      } else {
        writer.println("    _res.order(ByteOrder.nativeOrder());");
        String[] argumentNames = new String[binding.getNumArguments()];
        for (int i = 0; i < binding.getNumArguments(); i++) {
          argumentNames[i] = getArgumentName(i);
        }
        String expr = new MessageFormat(fmt).format(argumentNames);
        PointerType cReturnTypePointer = binding.getCReturnType().asPointer();
        CompoundType cReturnType = null;
        if (cReturnTypePointer != null) {
          cReturnType = cReturnTypePointer.getTargetType().asCompound();
        }
        if (cReturnType == null) {
          throw new RuntimeException("ReturnedArrayLength directive currently only supported for pointers to compound types " +
                                     "(error occurred while generating Java glue code for " + getName() + ")");
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
      writer.println(";");
    } else if (returnType.isNIOBuffer()) {
      writer.println("    if (_res == null) return null;");
      writer.println("    return _res.order(ByteOrder.nativeOrder());");
    } else if (returnType.isArrayOfCompoundTypeWrappers()) {
      writer.println("    if (_res == null) return null;");
      writer.println("    " + getReturnTypeString(false) + " _retarray = new " + getReturnTypeString(true) + "[_res.length];");
      writer.println("    for (int _count = 0; _count < _res.length; _count++) {");
      writer.println("      _retarray[_count] = new " + getReturnTypeString(true) + "(_res[_count]);");
      writer.println("    }");
      writer.println("    return _retarray;");
    }
  }

  public static String javaThisArgumentName() {
    return "jthis0";
  }

  protected String getCommentStartString() { return "/** "; }

  protected String getBaseIndentString() { return "  "; }

  protected String getReturnedArrayLengthExpression() {
    return returnedArrayLengthExpression;
  }

  /**
   * Class that emits a generic comment for JavaMethodBindingEmitters; the comment
   * includes the C signature of the native method that is being bound by the
   * emitter java method.
   */
  protected class DefaultCommentEmitter implements CommentEmitter {
    public void emit(FunctionEmitter emitter, PrintWriter writer) {
      emitBeginning(emitter, writer);
      emitBindingCSignature(((JavaMethodBindingEmitter)emitter).getBinding(), writer);
      emitEnding(emitter, writer);
    }
    protected void emitBeginning(FunctionEmitter emitter, PrintWriter writer) {
      writer.print("Entry point to C language function: <br> ");
    }
    protected void emitBindingCSignature(MethodBinding binding, PrintWriter writer) {      
      writer.print("<code> ");
      writer.print(binding.getCSymbol());
      writer.print(" </code> ");
    }
    protected void emitEnding(FunctionEmitter emitter, PrintWriter writer) {
      // If argument type is a named enum, then emit a comment detailing the
      // acceptable values of that enum.
      // If we're emitting a direct buffer variant only, then declare
      // that the NIO buffer arguments must be direct.
      MethodBinding binding = ((JavaMethodBindingEmitter)emitter).getBinding();
      for (int i = 0; i < binding.getNumArguments(); i++) {
        Type type = binding.getCArgumentType(i);
        JavaType javaType = binding.getJavaArgumentType(i);
        // don't emit param comments for anonymous enums, since we can't
        // distinguish between the values found within multiple anonymous
        // enums in the same C translation unit.
        if (type.isEnum() && type.getName() != HeaderParser.ANONYMOUS_ENUM_NAME) {
          EnumType enumType = (EnumType)type;
          writer.println();
          writer.print(emitter.getBaseIndentString()); 
          writer.print("    ");
          writer.print("@param ");
          writer.print(getArgumentName(i));
          writer.print(" valid values are: <code>");
          for (int j = 0; j < enumType.getNumEnumerates(); ++j) {
            if (j>0) writer.print(", ");
            writer.print(enumType.getEnumName(j));
          }
          writer.println("</code>");
        } else if (directNIOOnly && javaType.isNIOBuffer()) {
          writer.println();
          writer.print(emitter.getBaseIndentString()); 
          writer.print("    ");
          writer.print("@param ");
          writer.print(getArgumentName(i));
          writer.print(" a direct {@link " + javaType.getName() + "}");
        }
      }
    }
  }

  protected class InterfaceCommentEmitter
    extends JavaMethodBindingEmitter.DefaultCommentEmitter
  {
    protected void emitBeginning(FunctionEmitter emitter,
                                 PrintWriter writer) {
      writer.print("Interface to C language function: <br> ");
    }
  }
}


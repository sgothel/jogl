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

import java.util.*;
import java.io.*;

import net.java.games.gluegen.cgram.types.*;
import net.java.games.gluegen.cgram.*;

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

  protected static final CommentEmitter defaultJavaCommentEmitter = new DefaultCommentEmitter();
  protected static final CommentEmitter defaultInterfaceCommentEmitter =
    new InterfaceCommentEmitter();
  
  // Exception type raised in the generated code if runtime checks fail
  private String runtimeExceptionType;

  private MethodBinding binding;
  private boolean forImplementingMethodCall;

  // A non-null value indicates that rather than returning a compound
  // type accessor we are returning an array of such accessors; this
  // expression is a MessageFormat string taking the names of the
  // incoming Java arguments as parameters and computing as an int the
  // number of elements of the returned array.
  private String returnedArrayLengthExpression;

  public JavaMethodBindingEmitter(MethodBinding binding, PrintWriter output, String runtimeExceptionType)
  {
    this(binding, output, runtimeExceptionType, false);
  }
  
  public JavaMethodBindingEmitter(MethodBinding binding, PrintWriter output, String runtimeExceptionType, boolean forImplementingMethodCall)
  {
    super(output);
    this.binding = binding;
    this.forImplementingMethodCall = forImplementingMethodCall;
    this.runtimeExceptionType = runtimeExceptionType;
    setCommentEmitter(defaultInterfaceCommentEmitter);
  }
  
  public JavaMethodBindingEmitter(JavaMethodBindingEmitter arg) {
    super(arg);
    runtimeExceptionType          = arg.runtimeExceptionType;
    binding                       = arg.binding;
    forImplementingMethodCall     = arg.forImplementingMethodCall;
    returnedArrayLengthExpression = arg.returnedArrayLengthExpression;
  }

  public final MethodBinding getBinding() { return binding; }

  public boolean isForImplementingMethodCall() { return forImplementingMethodCall; }

  public String getName() {
    return binding.getName();
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

  protected void emitReturnType(PrintWriter writer)
  {    
    writer.print(getReturnTypeString(false));
  }

  protected String getReturnTypeString(boolean skipArray) {
    if (skipArray || (getReturnedArrayLengthExpression() == null && !binding.getJavaReturnType().isArrayOfCompoundTypeWrappers())) {
      return binding.getJavaReturnType().getName();
    }
    return binding.getJavaReturnType().getName() + "[]";
  }

  protected void emitName(PrintWriter writer)
  {
    if (forImplementingMethodCall) {
      writer.print(getImplMethodName());
    } else {
      writer.print(getName());
    }
  }

  protected int emitArguments(PrintWriter writer)
  {
    boolean needComma = false;
    int numEmitted = 0;

    if (forImplementingMethodCall && binding.hasContainingType()) {
      // Always emit outgoing "this" argument
      writer.print("java.nio.Buffer ");
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
      
      writer.print(type.getName());
      writer.print(" ");
      writer.print(binding.getArgumentName(i));
      ++numEmitted;
      needComma = true;
    }
    return numEmitted;
  }

  protected String getImplMethodName()
  {
    return binding.getName() + "0";
  }

  protected void emitBody(PrintWriter writer)
  {
    writer.println(';');
  }

  protected static String javaThisArgumentName() {
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
  protected static class DefaultCommentEmitter implements CommentEmitter {
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
      MethodBinding binding = ((JavaMethodBindingEmitter)emitter).getBinding();
      for (int i = 0; i < binding.getNumArguments(); i++) {
        Type type = binding.getCArgumentType(i);
        // don't emit param comments for anonymous enums, since we can't
        // distinguish between the values found within multiple anonymous
        // enums in the same C translation unit.
        if (type.isEnum() && type.getName() != HeaderParser.ANONYMOUS_ENUM_NAME) {
          EnumType enumType = (EnumType)type;
          writer.println();
          writer.print(emitter.getBaseIndentString()); 
          writer.print("    ");
          writer.print("@param ");
          writer.print(binding.getArgumentName(i));
          writer.print(" valid values are: <code>");
          for (int j = 0; j < enumType.getNumEnumerates(); ++j) {
            if (j>0) writer.print(", ");
            writer.print(enumType.getEnumName(j));
          }
          writer.println("</code>");
        }
      }
    }
  }

  protected static class InterfaceCommentEmitter
    extends JavaMethodBindingEmitter.DefaultCommentEmitter
  {
    protected void emitBeginning(FunctionEmitter emitter,
                                 PrintWriter writer) {
      writer.print("Interface to C language function: <br> ");
    }
  }
}


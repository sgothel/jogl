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
import java.io.*;
import java.text.MessageFormat;

import net.java.games.gluegen.cgram.types.*;

/** Emits the C-side component of the Java<->C JNI binding. */
public class CMethodBindingEmitter extends FunctionEmitter
{
  protected static final CommentEmitter defaultCommentEmitter =
    new DefaultCommentEmitter();

  protected static final String arrayResLength = "_array_res_length";
  protected static final String arrayRes       = "_array_res";
  protected static final String arrayIdx       = "_array_idx";
  
  private MethodBinding binding;

  /** Name of the package in which the corresponding Java method resides.*/
  private String packageName;

  /** Name of the class in which the corresponding Java method resides.*/
  private String className;

  /**
   * Whether or not the Java<->C JNI binding for this emitter's MethodBinding
   * is overloaded.
   */
  private boolean isOverloadedBinding;

  /**
   * Whether or not the Java-side of the Java<->C JNI binding for this
   * emitter's MethodBinding is static.
   */
  private boolean isJavaMethodStatic;

  /**
   * Optional List of Strings containing temporary C variables to declare.
   */
  private List/*<String>*/ temporaryCVariableDeclarations;

  /**
   * Optional List of Strings containing assignments to temporary C variables
   * to make after the call is completed.
   */
  private List/*<String>*/ temporaryCVariableAssignments;

  /**
   * Capacity of the return value in the event that it is encapsulated in a
   * java.nio.Buffer. Is ignored if binding.getJavaReturnType().isNIOBuffer()
   * == false;
   */
  private MessageFormat returnValueCapacityExpression = null;
  
  /**
   * Length of the returned array. Is ignored if
   * binding.getJavaReturnType().isArray() is false.
   */
  private MessageFormat returnValueLengthExpression = null;
  
  // Note: the VC++ 6.0 compiler emits hundreds of warnings when the
  // (necessary) null-checking code is enabled. This appears to just
  // be a compiler bug, but would be good to track down exactly why it
  // is happening. When the null checking is enabled for just the
  // GetPrimitiveArrayCritical calls, there are five warnings
  // generated for several thousand new if tests added to the code.
  // Which ones are the ones at fault? The line numbers for the
  // warnings are incorrect.
  private static final boolean EMIT_NULL_CHECKS = true;

  /**
   * Constructs an emitter for the specified binding, and sets a default
   * comment emitter that will emit the signature of the C function that is
   * being bound.
   */
  public CMethodBindingEmitter(MethodBinding binding,
                               boolean isOverloadedBinding,
                               String javaPackageName,
                               String javaClassName,                   
                               boolean isJavaMethodStatic,
                               PrintWriter output)
  {
    super(output);

    assert(binding != null);
    assert(javaClassName != null);
    assert(javaPackageName != null);
    
    this.binding = binding;
    this.packageName = javaPackageName;
    this.className = javaClassName;
    this.isOverloadedBinding = isOverloadedBinding;
    this.isJavaMethodStatic = isJavaMethodStatic;
    setCommentEmitter(defaultCommentEmitter);    
  }

  public final MethodBinding getBinding() { return binding; }

  public String getName() {
    return binding.getName();
  }

  /**
   * Get the expression for the capacity of the returned java.nio.Buffer.
   */
  public final MessageFormat getReturnValueCapacityExpression()
  {
    return returnValueCapacityExpression;
  }

  /**
   * If this function returns a void* encapsulated in a
   * java.nio.Buffer, sets the expression for the capacity of the
   * returned Buffer.
   *
   * @param expression a MessageFormat which, when applied to an array
   * of type String[] that contains each of the arguments names of the
   * Java-side binding, returns an expression that will (when compiled
   * by a C compiler) evaluate to an integer-valued expression. The
   * value of this expression is the capacity of the java.nio.Buffer
   * returned from this method.
   *
   * @throws IllegalArgumentException if the <code>
   * binding.getJavaReturnType().isNIOBuffer() == false
   * </code>
   */
  public final void setReturnValueCapacityExpression(MessageFormat expression)
  {
    returnValueCapacityExpression = expression;
    
    if (!binding.getJavaReturnType().isNIOBuffer())
    {
      throw new IllegalArgumentException(
        "Cannot specify return value capacity for a method that does not " +
        "return java.nio.Buffer: \"" + binding + "\"");      
    }
  }

  /**
   * Get the expression for the length of the returned array
   */
  public final MessageFormat getReturnValueLengthExpression()
  {
    return returnValueLengthExpression;
  }

  /**
   * If this function returns an array, sets the expression for the
   * length of the returned array.
   *
   * @param expression a MessageFormat which, when applied to an array
   * of type String[] that contains each of the arguments names of the
   * Java-side binding, returns an expression that will (when compiled
   * by a C compiler) evaluate to an integer-valued expression. The
   * value of this expression is the length of the array returned from
   * this method.
   *
   * @throws IllegalArgumentException if the <code>
   * binding.getJavaReturnType().isNIOBuffer() == false
   * </code>
   */
  public final void setReturnValueLengthExpression(MessageFormat expression)
  {
    returnValueLengthExpression = expression;
    
    if (!binding.getJavaReturnType().isArray())
    {
      throw new IllegalArgumentException(
        "Cannot specify return value length for a method that does not " +
        "return an array: \"" + binding + "\"");      
    }
  }

  /**
   * Returns the List of Strings containing declarations for temporary
   * C variables to be assigned to after the underlying function call.
   */
  public final List/*<String>*/ getTemporaryCVariableDeclarations() {
    return temporaryCVariableDeclarations;
  }

  /**
   * Sets up a List of Strings containing declarations for temporary C
   * variables to be assigned to after the underlying function call. A
   * null argument indicates that no manual declarations are to be made.
   */
  public final void setTemporaryCVariableDeclarations(List/*<String>*/ arg) {
    temporaryCVariableDeclarations = arg;
  }

  /**
   * Returns the List of Strings containing assignments for temporary
   * C variables which are made after the underlying function call. A
   * null argument indicates that no manual assignments are to be
   * made.
   */
  public final List/*<String>*/ getTemporaryCVariableAssignments() {
    return temporaryCVariableAssignments;
  }

  /**
   * Sets up a List of Strings containing assignments for temporary C
   * variables which are made after the underlying function call. A
   * null argument indicates that no manual assignments are to be made.
   */
  public final void setTemporaryCVariableAssignments(List/*<String>*/ arg) {
    temporaryCVariableAssignments = arg;
  }

  /**
   * Get the name of the class in which the corresponding Java method
   * resides.
   */
  public String getJavaPackageName() { return packageName; }

  /**
   * Get the name of the package in which the corresponding Java method
   * resides.
   */
  public String getJavaClassName() { return className; }

  /**
   * Is the Java<->C JNI binding for this emitter's MethodBinding one of
   * several overloaded methods with the same name?
   */
  public final boolean getIsOverloadedBinding() { return isOverloadedBinding; }

  /**
   * Is the Java side of the Java<->C JNI binding for this emitter's
   * MethodBinding a static method?.
   */
  public final boolean getIsJavaMethodStatic() { return isJavaMethodStatic; }

  protected void emitReturnType(PrintWriter writer)
  {    
    writer.print("JNIEXPORT ");
    writer.print(binding.getJavaReturnType().jniTypeName());
    writer.print(" JNICALL");
  }

  protected void emitName(PrintWriter writer)
  {
    writer.println(); // start name on new line
    writer.print("Java_");
    writer.print(jniMangle(getJavaPackageName()));
    writer.print("_");
    writer.print(jniMangle(getJavaClassName()));
    writer.print("_");
    if (isOverloadedBinding)
    {
      writer.print(jniMangle(binding));
      //System.err.println("OVERLOADED MANGLING FOR " + binding.getName() +
      //                   " = " + jniMangle(binding));
    }
    else
    {
      writer.print(jniMangle(binding.getName()));
      //System.err.println("    NORMAL MANGLING FOR " + binding.getName() +
      //                   " = " + jniMangle(binding.getName()));
    }
  }

  protected int emitArguments(PrintWriter writer)
  {
    writer.print("JNIEnv *env, ");
    int numEmitted = 1; // initially just the JNIEnv
    if (isJavaMethodStatic && !binding.hasContainingType())
    {
      writer.print("jclass");
    }
    else
    {
      writer.print("jobject");
    }
    writer.print(" _unused");
    ++numEmitted;
    
    if (binding.hasContainingType())
    {
      // "this" argument always comes down in argument 0 as direct buffer
      writer.print(", jobject " + JavaMethodBindingEmitter.javaThisArgumentName());
    }
    for (int i = 0; i < binding.getNumArguments(); i++) {
      JavaType javaArgType = binding.getJavaArgumentType(i);
      // Handle case where only param is void
      if (javaArgType.isVoid()) {
        // Make sure this is the only param to the method; if it isn't,
        // there's something wrong with our parsing of the headers.
        assert(binding.getNumArguments() == 1);
        continue;
      } 
      if (javaArgType.isJNIEnv() || binding.isArgumentThisPointer(i)) {
        continue;
      }
      writer.print(", ");
      writer.print(javaArgType.jniTypeName());
      writer.print(" ");
      writer.print(binding.getArgumentName(i));
      ++numEmitted;
    }

    return numEmitted;
  }

  
  protected void emitBody(PrintWriter writer)
  {    
    writer.println(" {");
    emitBodyVariableDeclarations(writer);
    emitBodyUserVariableDeclarations(writer);
    emitBodyVariablePreCallSetup(writer, false);
    emitBodyVariablePreCallSetup(writer, true);
    emitBodyCallCFunction(writer);    
    emitBodyUserVariableAssignments(writer);
    emitBodyVariablePostCallCleanup(writer, true);
    emitBodyVariablePostCallCleanup(writer, false);
    emitBodyReturnResult(writer);
    writer.println("}");
    writer.println();
  }

  protected void emitBodyVariableDeclarations(PrintWriter writer)
  {
    // Emit declarations for all pointer and String conversion variables
    if (binding.hasContainingType()) {
      emitPointerDeclaration(writer,
                             binding.getContainingType(),
                             binding.getContainingCType(),
                             CMethodBindingEmitter.cThisArgumentName(),
                             null);
    }

    boolean emittedDataCopyTemps = false;
    for (int i = 0; i < binding.getNumArguments(); i++) {
      JavaType type = binding.getJavaArgumentType(i);
      if (type.isJNIEnv() || binding.isArgumentThisPointer(i)) {
        continue;
      }

      if (type.isArray() || type.isNIOBuffer()) {
        String convName = pointerConversionArgumentName(i);
        // handle array/buffer argument types
        boolean needsDataCopy =
          emitPointerDeclaration(writer,
                                 binding.getJavaArgumentType(i),
                                 binding.getCArgumentType(i),
                                 convName,
                                 binding.getArgumentName(i));
        if (needsDataCopy && !emittedDataCopyTemps) {
          // emit loop counter and array length variables used during data
          // copy 
          writer.println("  jobject _tmpObj;");
          writer.println("  int _copyIndex;");
          writer.println("  jsize _tmpArrayLen;");
          emittedDataCopyTemps = true;
        }
      } else if (type.isString()) {
        writer.print("  const char* _UTF8");
        writer.print(binding.getArgumentName(i));
        writer.println(" = NULL;");
      }
      
    }

    // Emit declaration for return value if necessary
    Type cReturnType = binding.getCReturnType();

    JavaType javaReturnType = binding.getJavaReturnType();
    String capitalizedComponentType = null;
    if (!cReturnType.isVoid()) {
      writer.print("  ");
      // Note we must respect const/volatile for return argument
      writer.print(binding.getCSymbol().getReturnType().getName(true));
      writer.println(" _res;");
      if (javaReturnType.isArray()) {
        if (javaReturnType.isNIOByteBufferArray()) {
          writer.print("  int ");
          writer.print(arrayResLength);
          writer.println(";");
          writer.print("  int ");
          writer.print(arrayIdx);
          writer.println(";");
          writer.print("  jobjectArray ");
          writer.print(arrayRes);
          writer.println(";");
        } else {
          writer.print("  int ");
          writer.print(arrayResLength);
          writer.println(";");

          Class componentType = javaReturnType.getJavaClass().getComponentType();
          if (componentType.isArray()) {
            throw new RuntimeException("Multi-dimensional arrays not supported yet");            
          }

          String javaTypeName = componentType.getName();
          capitalizedComponentType =
            "" + Character.toUpperCase(javaTypeName.charAt(0)) + javaTypeName.substring(1);
          String javaArrayTypeName = "j" + javaTypeName + "Array";
          writer.print("  ");
          writer.print(javaArrayTypeName);
          writer.print(" ");
          writer.print(arrayRes);
          writer.println(";");
        }
      }
    } 
  }

  /** Emits the user-defined C variable declarations from the
      TemporaryCVariableDeclarations directive in the .cfg file. */
  protected void emitBodyUserVariableDeclarations(PrintWriter writer) {
    if (temporaryCVariableDeclarations != null) {
      for (Iterator iter = temporaryCVariableDeclarations.iterator(); iter.hasNext(); ) {
        String val = (String) iter.next();
        writer.print("  ");
        writer.println(val);
      }
    }
  }

  /**
   * Code to init the variables that were declared in
   * emitBodyVariableDeclarations(), PRIOR TO calling the actual C
   * function.
   */
  protected void emitBodyVariablePreCallSetup(PrintWriter writer,
                                              boolean emittingPrimitiveArrayCritical)
  {
    if (!emittingPrimitiveArrayCritical) {
      // Convert all Buffers to pointers first so we don't have to
      // call ReleasePrimitiveArrayCritical for any arrays if any
      // incoming buffers aren't direct
      if (binding.hasContainingType()) {
        emitPointerConversion(writer, binding,
                              binding.getContainingType(),
                              binding.getContainingCType(),
                              JavaMethodBindingEmitter.javaThisArgumentName(),
                              CMethodBindingEmitter.cThisArgumentName());
      }
    
      for (int i = 0; i < binding.getNumArguments(); i++) {
        JavaType type = binding.getJavaArgumentType(i);
        if (type.isJNIEnv() || binding.isArgumentThisPointer(i)) {
          continue;
        }
        if (type.isNIOBuffer()) {
          emitPointerConversion(writer, binding, type,
                                binding.getCArgumentType(i),
                                binding.getArgumentName(i),
                                pointerConversionArgumentName(i));
        }
      }
    }

    // Convert all arrays to pointers, and get UTF-8 versions of jstring args
    for (int i = 0; i < binding.getNumArguments(); i++) {
      JavaType javaArgType = binding.getJavaArgumentType(i);

      if (javaArgType.isJNIEnv() || binding.isArgumentThisPointer(i)) {
        continue;
      }

      if (javaArgType.isArray()) {
        boolean needsDataCopy = javaArgTypeNeedsDataCopy(javaArgType);
        Class subArrayElementJavaType = javaArgType.getJavaClass().getComponentType();

        // We only defer the emission of GetPrimitiveArrayCritical
        // calls that won't be matched up until after the function
        // we're calling
        if ((!needsDataCopy && !emittingPrimitiveArrayCritical) ||
            (needsDataCopy  && emittingPrimitiveArrayCritical)) {
          continue;
        }

        if (EMIT_NULL_CHECKS) {
          writer.print("  if (");
          writer.print(binding.getArgumentName(i));
          writer.println(" != NULL) {");
        }

        Type cArgType = binding.getCArgumentType(i);
        String cArgTypeName = cArgType.getName();

        String convName = pointerConversionArgumentName(i);

        if (!needsDataCopy) {
          writer.print("    ");
          writer.print(convName);
          writer.print(" = (");
          if (javaArgType.isArray() &&
              javaArgType.getJavaClass().getComponentType() == java.lang.String.class) {
            // java-side type is String[]
            cArgTypeName = "jstring *";
          }        
          writer.print(cArgTypeName);
          writer.print(") (*env)->GetPrimitiveArrayCritical(env, ");
          writer.print(binding.getArgumentName(i));
          writer.println(", NULL);");
        } else {
          // Handle the case where the array elements are of a type that needs a
          // data copy operation to convert from the java memory model to the C
          // memory model (e.g., int[][], String[], etc)
          //
          // FIXME: should factor out this whole block of code into a separate
          // method for clarity and maintenance purposes
          if (cArgType.toString().indexOf("const") == -1) {
            // FIXME: if the arg type is non-const, the sematics might be that
            // the function modifies the argument -- we don't yet support
            // this.
            //
            // Note: the check for "const" in the CVAttributes string isn't
            // truly checking the constness of the target types at both
            // pointer depths. However, it's a quick approximation, and quite
            // often C code doesn't get the constness right anyhow.
            throw new RuntimeException(
              "Cannot copy data for ptr-to-ptr arg type \"" + cArgType +
              "\": support for non-const ptr-to-ptr types not implemented.");
          }

          writer.println();
          writer.println("    /* Copy contents of " + binding.getArgumentName(i) +
                         " into " + convName + "_copy */");

          // get length of array being copied
          String arrayLenName = "_tmpArrayLen";
          writer.print("    ");
          writer.print(arrayLenName);
          writer.print(" = (*env)->GetArrayLength(env, ");
          writer.print(binding.getArgumentName(i)); 
          writer.println(");");

          // allocate an array to hold each element
          if (cArgType.pointerDepth() != 2) {
            throw new RuntimeException(
              "Could not copy data for type \"" + cArgType +
              "\"; copying only supported for types of the form " +
              "ptr-to-ptr-to-type.");
          }
          PointerType cArgPtrType = cArgType.asPointer();
          if (cArgPtrType == null) {
            throw new RuntimeException(
              "Could not copy data for type \"" + cArgType +
              "\"; currently only pointer types supported.");
          }
          PointerType cArgElementType = cArgPtrType.getTargetType().asPointer();          
          emitMalloc(
            writer,
            convName+"_copy",
            cArgElementType.getName(),
            arrayLenName,
            "Could not allocate buffer for copying data in argument \\\""+binding.getArgumentName(i)+"\\\"");

          // process each element in the array
          writer.println("    for (_copyIndex = 0; _copyIndex < "+arrayLenName+"; ++_copyIndex) {");

          // get each array element
          writer.println("      /* get each element of the array argument \"" + binding.getArgumentName(i) + "\" */");    
          String subArrayElementJNITypeString = jniType(subArrayElementJavaType);
          writer.print("      _tmpObj = (");
          writer.print(subArrayElementJNITypeString);
          writer.print(") (*env)->GetObjectArrayElement(env, ");
          writer.print(binding.getArgumentName(i));
          writer.println(", _copyIndex);");            

          if (subArrayElementJNITypeString == "jstring")
          {            
            writer.print("  ");
            emitGetStringUTFChars(writer,
                                  "(jstring) _tmpObj",
                                  convName+"_copy[_copyIndex]");
          }
          else if (isNIOBufferClass(subArrayElementJavaType))
          {
            emitGetDirectBufferAddress(writer,
                                       "_tmpObj",
                                       cArgElementType.getName(),
                                       convName + "_copy[_copyIndex]");
          }
          else
          {
            // Question: do we always need to copy the sub-arrays, or just
            // GetPrimitiveArrayCritical on each jobjectarray element and
            // assign it to the appropriate elements at pointer depth 1?
            // Probably depends on const-ness of the argument.
            // Malloc enough space to hold a copy of each sub-array
            writer.print("      ");
            emitMalloc(
              writer,
              convName+"_copy[_copyIndex]",
              cArgElementType.getTargetType().getName(), // assumes cArgPtrType is ptr-to-ptr-to-primitive !!
              "(*env)->GetArrayLength(env, _tmpObj)",
              "Could not allocate buffer during copying of data in argument \\\""+binding.getArgumentName(i)+"\\\"");
            // FIXME: copy the data (use matched Get/ReleasePrimitiveArrayCritical() calls)
            if (true) throw new RuntimeException(
              "Cannot yet handle type \"" + cArgType.getName() +
              "\"; need to add support for copying ptr-to-ptr-to-primitiveType subarrays");

 
          }
          writer.println("    }");

          writer.println();
        } // end of data copy
        
        if (EMIT_NULL_CHECKS) {
          writer.println("  }");
        }

      } else if (javaArgType.isString()) {
        if (emittingPrimitiveArrayCritical) {
          continue;
        }

        if (EMIT_NULL_CHECKS) {
          writer.print("  if (");
          writer.print(binding.getArgumentName(i));
          writer.println(" != NULL) {");
        }

        emitGetStringUTFChars(writer,
                              binding.getArgumentName(i),
                              "_UTF8" + binding.getArgumentName(i));

        if (EMIT_NULL_CHECKS) {
          writer.println("  }");
        }
      } else if (javaArgType.isArrayOfCompoundTypeWrappers()) {

        // FIXME
        throw new RuntimeException("Outgoing arrays of StructAccessors not yet implemented");
      }
    }
  }

  
  /**
   * Code to clean up any variables that were declared in
   * emitBodyVariableDeclarations(), AFTER calling the actual C function.
   */
  protected void emitBodyVariablePostCallCleanup(PrintWriter writer,
                                                 boolean emittingPrimitiveArrayCritical)
  {
    // Release primitive arrays and temporary UTF8 strings if necessary
    for (int i = 0; i < binding.getNumArguments(); i++) {
      JavaType javaArgType = binding.getJavaArgumentType(i);
      if (javaArgType.isJNIEnv() || binding.isArgumentThisPointer(i)) {
        continue;
      }
      if (javaArgType.isArray()) {
        boolean needsDataCopy = javaArgTypeNeedsDataCopy(javaArgType);
        Class subArrayElementJavaType = javaArgType.getJavaClass().getComponentType();

        if ((!needsDataCopy && !emittingPrimitiveArrayCritical) ||
            (needsDataCopy  && emittingPrimitiveArrayCritical)) {
          continue;
        }

        if (EMIT_NULL_CHECKS) {
          writer.print("  if (");
          writer.print(binding.getArgumentName(i));
          writer.println(" != NULL) {");
        }

        String convName = pointerConversionArgumentName(i);

        if (!needsDataCopy) {
          // Release array 
          writer.print("    (*env)->ReleasePrimitiveArrayCritical(env, ");
          writer.print(binding.getArgumentName(i));
          writer.print(", ");
          writer.print(convName);
          writer.println(", JNI_ABORT);");
        } else {
          // clean up the case where the array elements are of a type that needed
          // a data copy operation to convert from the java memory model to the
          // C memory model (e.g., int[][], String[], etc)
          //
          // FIXME: should factor out this whole block of code into a separate
          // method for clarity and maintenance purposes
          Type cArgType = binding.getCArgumentType(i);
          String cArgTypeName = cArgType.getName();

          if (cArgType.toString().indexOf("const") == -1) {
            // FIXME: handle any cleanup from treatment of non-const args,
            // assuming they were treated differently in
            // emitBodyVariablePreCallSetup() (see the similar section in that
            // method for details). 
            throw new RuntimeException(
              "Cannot clean up copied data for ptr-to-ptr arg type \"" + cArgType +
              "\": support for cleaning up non-const ptr-to-ptr types not implemented.");
          }

          writer.println("    /* Clean up " + convName + "_copy */");

          // Only need to perform cleanup for individual array
          // elements if they are not direct buffers
          if (!isNIOBufferClass(subArrayElementJavaType)) {
            // Re-fetch length of array that was copied
            String arrayLenName = "_tmpArrayLen";
            writer.print("    ");
            writer.print(arrayLenName);
            writer.print(" = (*env)->GetArrayLength(env, ");
            writer.print(binding.getArgumentName(i)); 
            writer.println(");");

            // free each element
            PointerType cArgPtrType = cArgType.asPointer();
            if (cArgPtrType == null) {
              throw new RuntimeException(
                "Could not copy data for type \"" + cArgType +
                "\"; currently only pointer types supported.");
            }
            PointerType cArgElementType = cArgPtrType.getTargetType().asPointer();          
         
            // process each element in the array
            writer.println("    for (_copyIndex = 0; _copyIndex < " + arrayLenName +"; ++_copyIndex) {");

            // get each array element
            writer.println("      /* free each element of " +convName +"_copy */");    
            String subArrayElementJNITypeString = jniType(subArrayElementJavaType);
            writer.print("      _tmpObj = (");
            writer.print(subArrayElementJNITypeString);
            writer.print(") (*env)->GetObjectArrayElement(env, ");
            writer.print(binding.getArgumentName(i));
            writer.println(", _copyIndex);");            

            if (subArrayElementJNITypeString == "jstring") {            
              writer.print("     (*env)->ReleaseStringUTFChars(env, ");
              writer.print("(jstring) _tmpObj");
              writer.print(", ");
              writer.print(convName+"_copy[_copyIndex]");
              writer.println(");");           
            } else {
              if (true) throw new RuntimeException(
                "Cannot yet handle type \"" + cArgType.getName() +
                "\"; need to add support for cleaning up copied ptr-to-ptr-to-primitiveType subarrays"); 
            }
            writer.println("    }");
          }

          // free the main array
          writer.print("    free((void*) ");
          writer.print(convName+"_copy");
          writer.println(");");
        } // end of cleaning up copied data

        if (EMIT_NULL_CHECKS) {
          writer.println("  }");
        }
      } else if (javaArgType.isString()) {
        if (emittingPrimitiveArrayCritical) {
          continue;
        }

        if (EMIT_NULL_CHECKS) {
          writer.print("  if (");
          writer.print(binding.getArgumentName(i));
          writer.println(" != NULL) {");
        }

        writer.print("    (*env)->ReleaseStringUTFChars(env, ");
        writer.print(binding.getArgumentName(i));
        writer.print(", _UTF8");
        writer.print(binding.getArgumentName(i));
        writer.println(");");

        if (EMIT_NULL_CHECKS) {
          writer.println("  }");
        }
      } else if (javaArgType.isArrayOfCompoundTypeWrappers()) {

        // FIXME
        throw new RuntimeException("Outgoing arrays of StructAccessors not yet implemented");

      }
    }
  }

  protected void emitBodyCallCFunction(PrintWriter writer)
  {
    // Make the call to the actual C function
    writer.print("  ");

    // WARNING: this code assumes that the return type has already been
    // typedef-resolved.
    Type cReturnType = binding.getCReturnType();

    if (!cReturnType.isVoid()) {
      writer.print("_res = ");
    }
    if (binding.hasContainingType()) {
      // Call through function pointer
      writer.print(CMethodBindingEmitter.cThisArgumentName() + "->");
    }
    writer.print(binding.getCSymbol().getName());
    writer.print("(");
    for (int i = 0; i < binding.getNumArguments(); i++) {
      if (i != 0) {
        writer.print(", ");
      }
      JavaType javaArgType = binding.getJavaArgumentType(i);
      // Handle case where only param is void.
      if (javaArgType.isVoid()) {
        // Make sure this is the only param to the method; if it isn't,
        // there's something wrong with our parsing of the headers.
        assert(binding.getNumArguments() == 1);
        continue;
      } 

      if (javaArgType.isJNIEnv()) {
        writer.print("env");
      } else if (binding.isArgumentThisPointer(i)) {
        writer.print(CMethodBindingEmitter.cThisArgumentName());
      } else {
        writer.print("(");        
        Type cArgType = binding.getCSymbol().getArgumentType(i);
        writer.print(cArgType.getName());
        writer.print(") ");
        if (binding.getCArgumentType(i).isPointer() && binding.getJavaArgumentType(i).isPrimitive()) {
          writer.print("(intptr_t) ");
        }
        if (javaArgType.isArray() || javaArgType.isNIOBuffer()) {
          writer.print(pointerConversionArgumentName(i));
          if (javaArgTypeNeedsDataCopy(javaArgType)) {
            writer.print("_copy");
          }
        } else {
          if (javaArgType.isString()) { writer.print("_UTF8"); }
          writer.print(binding.getArgumentName(i));          
        }
      }
    }
    writer.println(");");
  }
  
  /** Emits the user-defined C variable assignments from the
      TemporaryCVariableAssignments directive in the .cfg file. */
  protected void emitBodyUserVariableAssignments(PrintWriter writer) {
    if (temporaryCVariableAssignments != null) {
      for (Iterator iter = temporaryCVariableAssignments.iterator(); iter.hasNext(); ) {
        String val = (String) iter.next();
        writer.print("  ");
        writer.println(val);
      }
    }
  }

  // FIXME: refactor this so that subclasses (in particular,
  // net.java.games.gluegen.opengl.CGLPAWrapperEmitter) don't have to copy the whole
  // method
  protected void emitBodyReturnResult(PrintWriter writer)
  {
    // WARNING: this code assumes that the return type has already been
    // typedef-resolved.
    Type cReturnType = binding.getCReturnType();

    // Return result if necessary
    if (!cReturnType.isVoid()) {
      JavaType javaReturnType = binding.getJavaReturnType();
      if (javaReturnType.isPrimitive()) {
        writer.print("  return ");
        if (cReturnType.isPointer()) {
          // Pointer being converted to int or long: cast this result
          // (through intptr_t to avoid compiler warnings with gcc)
          writer.print("(" + javaReturnType.jniTypeName() + ") (intptr_t) ");
        }
        writer.println("_res;");
      } else if (javaReturnType.isNIOBuffer()) {
        writer.println("  if (_res == NULL) return NULL;");
        writer.print("  return (*env)->NewDirectByteBuffer(env, _res, ");
        // See whether capacity has been specified
        if (returnValueCapacityExpression != null) {
          String[] argumentNames = new String[binding.getNumArguments()];
          for (int i = 0; i < binding.getNumArguments(); i++)
          {
            argumentNames[i] = binding.getArgumentName(i);
          }
          writer.print(
            returnValueCapacityExpression.format(argumentNames));
        } else {
          int sz = 0;
          if (cReturnType.isPointer() &&
              cReturnType.asPointer().getTargetType().isCompound()) {
            sz = cReturnType.asPointer().getTargetType().getSize();
            if (sz == -1) {
              throw new InternalError(
                "Error emitting code for compound return type "+
                "for function \"" + binding + "\": " +
                "Structs to be emitted should have been laid out by this point " +
                "(type " + cReturnType.asPointer().getTargetType().getName() + " / " +
                cReturnType.asPointer().getTargetType() + " was not)"
              );
            }
          } else {
            sz = cReturnType.getSize();
          }
          writer.print(sz);
          System.err.println(
            "WARNING: No capacity specified for java.nio.Buffer return " +
            "value for function \"" + binding + "\";" +
            " assuming size of equivalent C return type (" + sz + " bytes): " + binding); 
        }
        writer.println(");");
      } else if (javaReturnType.isString()) {
        writer.print("  if (_res == NULL) return NULL;");
        writer.println("  return (*env)->NewStringUTF(env, _res);");
      } else if (javaReturnType.isArray()) {
        if (javaReturnType.isNIOByteBufferArray()) {
          writer.println("  if (_res == NULL) return NULL;");
          if (returnValueLengthExpression == null) {
            throw new RuntimeException("Error while generating C code: no length specified for array returned from function " +
                                       binding);
          }
          String[] argumentNames = new String[binding.getNumArguments()];
          for (int i = 0; i < binding.getNumArguments(); i++) {
            argumentNames[i] = binding.getArgumentName(i);
          }
          writer.println("  " + arrayResLength + " = " + returnValueLengthExpression.format(argumentNames) + ";");
          writer.println("  " + arrayRes + " = (*env)->NewObjectArray(env, " + arrayResLength + ", (*env)->FindClass(env, \"java/nio/ByteBuffer\"), NULL);");
          writer.println("  for (" + arrayIdx + " = 0; " + arrayIdx + " < " + arrayResLength + "; " + arrayIdx + "++) {");
          Type retType = binding.getCSymbol().getReturnType();
          Type baseType;
          if (retType.isPointer()) {
            baseType = retType.asPointer().getTargetType().asPointer().getTargetType();
          } else {
            baseType = retType.asArray().getElementType().asPointer().getTargetType();
          }
          int sz = baseType.getSize();
          if (sz < 0)
            sz = 0;
          writer.println("    (*env)->SetObjectArrayElement(env, " + arrayRes + ", " + arrayIdx +
                         ", (*env)->NewDirectByteBuffer(env, _res[" + arrayIdx + "], " + sz + "));");
          writer.println("  }");
          writer.println("  return " + arrayRes + ";");
        } else {
          // FIXME: must have user provide length of array in .cfg file
          // by providing a constant value, input parameter, or
          // expression which computes the array size (already present
          // as ReturnValueCapacity, not yet implemented / tested here)

          throw new RuntimeException(
                                     "Could not emit native code for function \"" + binding +
                                     "\": array return values for non-char types not implemented yet");

          // FIXME: This is approximately what will be required here
          //
          //writer.print("  ");
          //writer.print(arrayRes);
          //writer.print(" = (*env)->New");
          //writer.print(capitalizedComponentType);
          //writer.print("Array(env, ");
          //writer.print(arrayResLength);
          //writer.println(");");
          //writer.print("  (*env)->Set");
          //writer.print(capitalizedComponentType);
          //writer.print("ArrayRegion(env, ");
          //writer.print(arrayRes);
          //writer.print(", 0, ");
          //writer.print(arrayResLength);
          //writer.println(", _res);");
          //writer.print("  return ");
          //writer.print(arrayRes);
          //writer.println(";");
        }
      }
    }
  }  

  protected static String cThisArgumentName() {
    return "this0";
  }
  
  // Mangle a class, package or function name
  protected String jniMangle(String name) {
    return name.replaceAll("_", "_1").replace('.', '_');
  }

  protected String jniMangle(MethodBinding binding) {
    StringBuffer buf = new StringBuffer();
    buf.append(jniMangle(binding.getName()));
    buf.append("__");
    for (int i = 0; i < binding.getNumArguments(); i++) {
      JavaType type = binding.getJavaArgumentType(i);
      Class c = type.getJavaClass();
      if (c != null) {
        jniMangle(c, buf);
      } else {
        // FIXME: add support for char* -> String conversion
        throw new RuntimeException("Unknown kind of JavaType: name="+type.getName());
      }
    }
    return buf.toString();
  }

  protected void jniMangle(Class c, StringBuffer res) {
    if (c.isArray()) {
      res.append("_3");
      jniMangle(c.getComponentType(), res);
    } else if (c.isPrimitive()) {
           if (c == Boolean.TYPE)   res.append("Z");
      else if (c == Byte.TYPE)      res.append("B");
      else if (c == Character.TYPE) res.append("C");
      else if (c == Short.TYPE)     res.append("S");
      else if (c == Integer.TYPE)   res.append("I");
      else if (c == Long.TYPE)      res.append("J");
      else if (c == Float.TYPE)     res.append("F");
      else if (c == Double.TYPE)    res.append("D");
      else throw new InternalError("Illegal primitive type");
    } else {
      res.append("L");
      res.append(c.getName().replace('.', '_'));
      res.append("_2");
    }
  }

  private String jniType(Class javaType)
  {
    if (javaType.isPrimitive()) {
      return "j" + javaType.getName();
    } else if (javaType == java.lang.String.class) {
      return "jstring";
    } else if (isNIOBufferClass(javaType)) {
      return "jobject";
    } else {
      throw new RuntimeException(
        "Could not determine JNI type for Java class \"" +
        javaType.getName() + "\"; was not String, primitive or direct buffer");
    }
  }
  
  private void emitOutOfMemoryCheck(PrintWriter writer, String varName,
                                    String errorMessage)
  {
    writer.print("    if (");
    writer.print(varName);
    writer.println(" == NULL) {");
    writer.println("      (*env)->ThrowNew(env, (*env)->FindClass(env, \"java/lang/OutOfMemoryError\"),");
    writer.print("                       \"" + errorMessage);
    writer.print(" in native dispatcher for \\\"");
    writer.print(binding.getName());
    writer.println("\\\"\");");
    writer.print("      return");
    if (!binding.getJavaReturnType().isVoid()) {
      writer.print(" 0");
    }
    writer.println(";");
    writer.println("    }");
  }

  private void emitMalloc(PrintWriter writer,
                          String targetVarName,
                          String elementTypeString,
                          String numElementsExpression,
                          String mallocFailureErrorString)
  {
    writer.print("    ");
    writer.print(targetVarName);
    writer.print(" = (");
    writer.print(elementTypeString);
    writer.print(" *) malloc(");
    writer.print(numElementsExpression);
    writer.print(" * sizeof(");       
    writer.print(elementTypeString);
    writer.println("));");
    // Catch memory allocation failure
    if (EMIT_NULL_CHECKS) {
      emitOutOfMemoryCheck(
        writer, targetVarName,
        mallocFailureErrorString);
    }
  }

  private void emitGetStringUTFChars(PrintWriter writer,
                                     String sourceVarName,
                                     String receivingVarName)
  {
    writer.print("    if (");
    writer.print(sourceVarName);
    writer.println(" != NULL) {");
    writer.print("      ");
    writer.print(receivingVarName);
    writer.print(" = (*env)->GetStringUTFChars(env, ");
    writer.print(sourceVarName);
    writer.println(", (jboolean*)NULL);");
    // Catch memory allocation failure in the event that the VM didn't pin
    // the String and failed to allocate a copy    
    if (EMIT_NULL_CHECKS) {
      emitOutOfMemoryCheck(
        writer, receivingVarName,
        "Failed to get UTF-8 chars for argument \\\""+sourceVarName+"\\\"");
    }
    writer.println("    } else {");
    writer.print("      ");
    writer.print(receivingVarName);
    writer.println(" = NULL;");
    writer.println("    }");
  }      

  private void emitGetDirectBufferAddress(PrintWriter writer,
                                          String sourceVarName,
                                          String receivingVarTypeString,
                                          String receivingVarName) {
    if (EMIT_NULL_CHECKS) {
      writer.print("    if (");
      writer.print(sourceVarName);
      writer.println(" != NULL) {");
    }
    writer.print("      ");
    writer.print(receivingVarName);
    writer.print(" = (");
    writer.print(receivingVarTypeString);
    writer.print(") (*env)->GetDirectBufferAddress(env, ");
    writer.print(sourceVarName);
    writer.println(");");
    if (EMIT_NULL_CHECKS) {
      writer.println("    } else {");
      writer.print("      ");
      writer.print(receivingVarName);
      writer.println(" = NULL;");
      writer.println("    }");
    }
  }
                                          
  
  // Note: if the data in the Type needs to be converted from the Java memory
  // model to the C memory model prior to calling any C-side functions, then
  // an extra variable named XXX_copy (where XXX is the value of the
  // cVariableName argument) will be emitted and TRUE will be returned.
  private boolean emitPointerDeclaration(PrintWriter writer,
                                         JavaType javaType,
                                         Type cType,
                                         String cVariableName,
                                         String javaArgumentName) {
    String ptrTypeString = null;
    boolean needsDataCopy = false;

    // Emit declaration for the pointer variable.
    //
    // Note that we don't need to obey const/volatile for outgoing arguments
    //
    if (javaType.isNIOBuffer())
    {
      ptrTypeString = cType.getName();
    }
    else if (javaType.isArray()) {
      needsDataCopy = javaArgTypeNeedsDataCopy(javaType);
      // It's an array; get the type of the elements in the array
      Class elementType = javaType.getJavaClass().getComponentType();
      if (elementType.isPrimitive())
      {
        ptrTypeString = cType.getName();
      }
      else if (elementType == java.lang.String.class)
      {
        ptrTypeString = "jstring";
      }
      else if (elementType.isArray())
      {
        Class subElementType = elementType.getComponentType();
        if (subElementType.isPrimitive())
        {
          // type is pointer to pointer to primitive
          ptrTypeString = cType.getName();
        }
        else
        {
          // type is pointer to pointer of some type we don't support (maybe
          // it's an array of pointers to structs?)
          throw new RuntimeException("Unsupported pointer type: \"" + cType.getName() + "\"");    
        }

      }
      else if (isNIOBufferClass(elementType))
      {
        // type is an array of direct buffers of some sort
        ptrTypeString = cType.getName();
      }
      else
      {
        // Type is pointer to something we can't/don't handle
        throw new RuntimeException("Unsupported pointer type: \"" + cType.getName() + "\"");
      }
    }
    else if (javaType.isArrayOfCompoundTypeWrappers())
    {
      // FIXME
      throw new RuntimeException("Outgoing arrays of StructAccessors not yet implemented");
    }
    else
    {
      ptrTypeString = cType.getName();
    }

    if (!needsDataCopy)
    {
      // declare the pointer variable
      writer.print("  ");
      writer.print(ptrTypeString);
      writer.print(" ");
      writer.print(cVariableName);
      writer.println(" = NULL;");
    }
    else
    {
      // Declare a variable to hold a copy of the argument data in which the
      // incoming data has been properly laid out in memory to match the C
      // memory model
      //writer.print("  const ");
      Class elementType = javaType.getJavaClass().getComponentType();
      if (javaType.isArray() &&
          javaType.getJavaClass().getComponentType() == java.lang.String.class) {
        writer.print("  const char **");
      } else {
        writer.print(ptrTypeString);
      }
      writer.print(" ");
      writer.print(cVariableName);
      writer.print("_copy = NULL; /* copy of data in ");
      writer.print(javaArgumentName);
      writer.println(", laid out according to C memory model */");
    }

    return needsDataCopy;
  }

  private void emitPointerConversion(PrintWriter writer,
                                     MethodBinding binding,
                                     JavaType type,
                                     Type cType,
                                     String incomingArgumentName,
                                     String cVariableName) {
    emitGetDirectBufferAddress(writer,
                               incomingArgumentName,
                               cType.getName(),
                               cVariableName);

    /*
    if (EMIT_NULL_CHECKS) {
      writer.print("  if (");
      writer.print(incomingArgumentName);
      writer.println(" != NULL) {");
    }
    
    writer.print("    ");
    writer.print(cVariableName);
    writer.print(" = (");
    writer.print(cType.getName());
    writer.print(") (*env)->GetDirectBufferAddress(env, ");
    writer.print(incomingArgumentName);
    writer.println(");");
          
    if (EMIT_NULL_CHECKS) {
      writer.println("  }");
    }
    */
  }

  protected String pointerConversionArgumentName(int i) {
    return "_ptr" + i;
  }

  /**
   * Class that emits a generic comment for CMethodBindingEmitters; the comment
   * includes the C signature of the native method that is being bound by the
   * emitter java method.
   */
  protected static class DefaultCommentEmitter implements CommentEmitter {
    public void emit(FunctionEmitter emitter, PrintWriter writer) {     
      emitBeginning((CMethodBindingEmitter)emitter, writer);
      emitEnding((CMethodBindingEmitter)emitter, writer);
    }
    protected void emitBeginning(CMethodBindingEmitter emitter, PrintWriter writer) {
      writer.println("  Java->C glue code:");
      writer.print(" *   Java package: ");
      writer.print(emitter.getJavaPackageName());
      writer.print(".");
      writer.println(emitter.getJavaClassName());
      writer.print(" *    Java method: ");
      MethodBinding binding = emitter.getBinding();
      writer.println(binding);
      writer.println(" *     C function: " + binding.getCSymbol());
    }
    protected void emitEnding(CMethodBindingEmitter emitter, PrintWriter writer) {
    }
  }

  protected boolean javaArgTypeNeedsDataCopy(JavaType javaArgType) {
    if (javaArgType.isArray()) {
      Class subArrayElementJavaType = javaArgType.getJavaClass().getComponentType();
      return (subArrayElementJavaType.isArray() ||
              subArrayElementJavaType == java.lang.String.class ||
              isNIOBufferClass(subArrayElementJavaType));
    }
    return false;
  }

  protected static boolean isNIOBufferClass(Class c) {
    return java.nio.Buffer.class.isAssignableFrom(c);
  }
}


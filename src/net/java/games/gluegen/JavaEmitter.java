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

import java.io.*;
import java.util.*;
import java.text.MessageFormat;

import net.java.games.gluegen.cgram.types.*;

// PROBLEMS:
//  - what if something returns 'const int *'? Could we
//    return an IntBuffer that has read-only behavior? Or do we copy the array
//    (but we don't know its size!). What do we do if it returns a non-const
//    int*? Should the user be allowed to write back to the returned pointer?
//
//  - Non-const array types must be properly released with JNI_COMMIT
//    in order to see side effects if the array was copied.


public class JavaEmitter implements GlueEmitter {
  private StructLayout layout;
  private TypeDictionary typedefDictionary;
  private TypeDictionary structDictionary;
  private Map            canonMap;
  private JavaConfiguration cfg;

  /**
   * Style of code emission. Can emit everything into one class
   * (AllStatic), separate interface and implementing classes
   * (InterfaceAndImpl), only the interface (InterfaceOnly), or only
   * the implementation (ImplOnly).
   */
  static final int ALL_STATIC = 1;
  static final int INTERFACE_AND_IMPL = 2;
  static final int INTERFACE_ONLY = 3;
  static final int IMPL_ONLY = 4;

  private PrintWriter javaWriter; // Emits either interface or, in AllStatic mode, everything
  private PrintWriter javaImplWriter; // Only used in non-AllStatic modes for impl class
  private PrintWriter cWriter;
  private MachineDescription machDesc;
  
  public void readConfigurationFile(String filename) throws Exception {
    cfg = createConfig();
    cfg.read(filename);
  }

  public void setMachineDescription(MachineDescription md) {
    machDesc = md;
  }

  public void beginEmission(GlueEmitterControls controls) throws IOException
  {
    try
    {
      openWriters();
    }
    catch (Exception e)
    {
      throw new RuntimeException(
        "Unable to open files for writing", e);
    }
    
    emitAllFileHeaders();

    // Request emission of any structs requested
    for (Iterator iter = cfg.forcedStructs().iterator(); iter.hasNext(); ) {
      controls.forceStructEmission((String) iter.next());
    }
  }

  public void endEmission()
  {
    emitAllFileFooters();

    try
    {
      closeWriters();
    }
    catch (Exception e)
    {
      throw new RuntimeException(
        "Unable to close open files", e);
    }
  }

  public void beginDefines() throws Exception
  {
    if (cfg.allStatic() || cfg.emitInterface()) {
      javaWriter().println();
    }
  }

  public void emitDefine(String name, String value, String optionalComment) throws Exception
  {
    if (cfg.allStatic() || cfg.emitInterface()) {
      // TODO: Some defines (e.g., GL_DOUBLE_EXT in gl.h) are defined in terms
      // of other defines -- should we emit them as references to the original
      // define (not even sure if the lexer supports this)? Right now they're
      // emitted as the numeric value of the original definition. If we decide
      // emit them as references we'll also have to emit them in the correct
      // order. It's probably not an issue right now because the emitter
      // currently only emits only numeric defines -- if it handled #define'd
      // objects it would make a bigger difference.
 
      if (!cfg.shouldIgnore(name)) {
        String type = null;

        // FIXME: need to handle when type specifier is in last char (e.g.,
        // "1.0d or 2759L", because parseXXX() methods don't allow the type
        // specifier character in the string.
        //
        //char lastChar = value.charAt(value.length()-1);
        
        try {
          // see if it's a long or int
          int radix;
          String parseValue;
          // FIXME: are you allowed to specify hex/octal constants with
          // negation, e.g. "-0xFF" or "-056"? If so, need to modify the
          // following "if(..)" checks and parseValue computation
          if (value.startsWith("0x") || value.startsWith("0X")) {
            radix = 16;
            parseValue = value.substring(2);
          }
          else if (value.startsWith("0") && value.length() > 1) {
            // TODO: is "0" the prefix in C to indicate octal???
            radix = 8; 
            parseValue = value.substring(1);
          }
          else {
            radix = 10;
            parseValue = value;
          }
          //System.err.println("parsing " + value + " as long w/ radix " + radix);
          long longVal = Long.parseLong(parseValue, radix);
          type = "long";
          // if constant is small enough, store it as an int instead of a long
          if (longVal > Integer.MIN_VALUE && longVal < Integer.MAX_VALUE) {
            type = "int";
          }
          
        } catch (NumberFormatException e) {
          try {
            // see if it's a double or float
            double dVal = Double.parseDouble(value);
            type = "double";
            // if constant is small enough, store it as a float instead of a double
            if (dVal > Float.MIN_VALUE && dVal < Float.MAX_VALUE) {
              type = "float";
            }
            
          } catch (NumberFormatException e2) {            
            throw new RuntimeException(
              "Cannot emit define \""+name+"\": value \""+value+
              "\" cannot be assigned to a int, long, float, or double", e2);
          }
        }

        if (type == null) {
            throw new RuntimeException(
              "Cannot emit define (2) \""+name+"\": value \""+value+
              "\" cannot be assigned to a int, long, float, or double");
        }
        if (optionalComment != null && optionalComment.length() != 0) {
          javaWriter().println("  /** " + optionalComment + " */");
        }
        javaWriter().println("  public static final " + type + " " + name + " = " + value + ";");
      }
    }
  }

  public void endDefines() throws Exception
  {
  }

  public void beginFunctions(TypeDictionary typedefDictionary,
                             TypeDictionary structDictionary,
                             Map            canonMap) throws Exception {
    this.typedefDictionary = typedefDictionary;
    this.structDictionary  = structDictionary;
    this.canonMap          = canonMap;
    if (cfg.allStatic() || cfg.emitInterface()) {
      javaWriter().println();
    }
  }

  public Iterator emitFunctions(List/*<FunctionSymbol>*/ originalCFunctions)
    throws Exception {
    // Sometimes headers will have the same function prototype twice, once
    // with the argument names and once without. We'll remember the signatures
    // we've already processed we don't generate duplicate bindings.
    //
    // Note: this code assumes that on the equals() method in FunctionSymbol
    // only considers function name and argument types (i.e., it does not
    // consider argument *names*) when comparing FunctionSymbols for equality    
    Set funcsToBindSet = new HashSet(100);
    for (Iterator cIter = originalCFunctions.iterator(); cIter.hasNext(); ) {
      FunctionSymbol cFunc = (FunctionSymbol) cIter.next();
      if (!funcsToBindSet.contains(cFunc)) {
        funcsToBindSet.add(cFunc);
      }
    }

    ArrayList funcsToBind = new ArrayList(funcsToBindSet.size());
    funcsToBind.addAll(funcsToBindSet);
    // sort functions to make them easier to find in native code
    Collections.sort(
      funcsToBind,
      new Comparator() {
          public int compare(Object o1, Object o2) {
            return ((FunctionSymbol)o1).getName().compareTo(
              ((FunctionSymbol)o2).getName());
          }
          public boolean equals(Object obj) {
            return obj.getClass() == this.getClass();
          }
        });

    // Bind all the C funcs to Java methods
    ArrayList/*<FunctionEmitter>*/ methodBindingEmitters = new ArrayList(2*funcsToBind.size());
    for (Iterator iter = funcsToBind.iterator(); iter.hasNext(); ) {
      FunctionSymbol cFunc = (FunctionSymbol) iter.next();
      // Check to see whether this function should be ignored
      if (cfg.shouldIgnore(cFunc.getName())) {
        continue; // don't generate bindings for this symbol
      }
      
      List allBindings = generateMethodBindingEmitters(cFunc);
      methodBindingEmitters.addAll(allBindings);
    }

    // Emit all the methods
    for (int i = 0; i < methodBindingEmitters.size(); ++i) {
      FunctionEmitter emitter = (FunctionEmitter)methodBindingEmitters.get(i);      
      try {
        emitter.emit();
      } catch (Exception e) {
        throw new RuntimeException(
            "Error while emitting binding for \"" + emitter.getName() + "\"", e);
      }
      emitter.getDefaultOutput().println(); // put newline after method body
    }

    // Return the list of FunctionSymbols that we generated gluecode for
    return funcsToBind.iterator();
  }

  /**
   * Create the object that will read and store configuration information for
   * this JavaEmitter.
   */
  protected JavaConfiguration createConfig() {
    return new JavaConfiguration();
  }

  /**
   * Get the configuration information for this JavaEmitter.
   */
  protected JavaConfiguration getConfig() {
    return cfg;
  }

  /**
   * Generates the public emitters for this MethodBinding which will
   * produce either simply signatures (for the interface class, if
   * any) or function definitions with or without a body (depending on
   * whether or not the implementing function can go directly to
   * native code because it doesn't need any processing of the
   * outgoing arguments).
   */
  protected void generatePublicEmitters(MethodBinding binding,
                                        List allEmitters,
                                        boolean signatureOnly) {
    PrintWriter writer = ((signatureOnly || cfg.allStatic()) ? javaWriter() : javaImplWriter());

    if (cfg.manuallyImplement(binding.getName()) && !signatureOnly) {
      // We only generate signatures for manually-implemented methods;
      // user provides the implementation
      return;
    }

    // It's possible we may not need a body even if signatureOnly is
    // set to false; for example, if the routine doesn't take any
    // arrays or buffers as arguments
    boolean isUnimplemented = cfg.isUnimplemented(binding.getName());
    boolean needsBody = (isUnimplemented ||
                         (binding.needsNIOWrappingOrUnwrapping() ||
                          binding.signatureUsesJavaPrimitiveArrays()));

    JavaMethodBindingEmitter emitter =
      new JavaMethodBindingEmitter(binding,
                                   writer,
                                   cfg.runtimeExceptionType(),
                                   !signatureOnly && needsBody,
                                   false,
                                   cfg.nioDirectOnly(binding.getName()),
                                   false,
                                   false,
                                   false,
                                   isUnimplemented);
    emitter.addModifier(JavaMethodBindingEmitter.PUBLIC);
    if (cfg.allStatic()) {
      emitter.addModifier(JavaMethodBindingEmitter.STATIC);
    }
    if (!isUnimplemented && !needsBody && !signatureOnly) {
      emitter.addModifier(JavaMethodBindingEmitter.NATIVE);
    }
    emitter.setReturnedArrayLengthExpression(cfg.returnedArrayLength(binding.getName()));
    allEmitters.add(emitter);
  }

  /**
   * Generates the private emitters for this MethodBinding. On the
   * Java side these will simply produce signatures for native
   * methods. On the C side these will create the emitters which will
   * write the JNI code to interface to the functions. We need to be
   * careful to make the signatures all match up and not produce too
   * many emitters which would lead to compilation errors from
   * creating duplicated methods / functions.
   */
  protected void generatePrivateEmitters(MethodBinding binding,
                                         List allEmitters) {
    if (cfg.manuallyImplement(binding.getName())) {
      // Don't produce emitters for the implementation class
      return;
    }

    // If we already generated a public native entry point for this
    // method, don't emit another one
    if (!cfg.isUnimplemented(binding.getName()) &&
        (binding.needsNIOWrappingOrUnwrapping() ||
         binding.signatureUsesJavaPrimitiveArrays())) {
      PrintWriter writer = (cfg.allStatic() ? javaWriter() : javaImplWriter());

      // If the binding uses primitive arrays, we are going to emit
      // the private native entry point for it along with the version
      // taking only NIO buffers
      if (!binding.signatureUsesJavaPrimitiveArrays()) {
        // (Always) emit the entry point taking only direct buffers
        JavaMethodBindingEmitter emitter =
          new JavaMethodBindingEmitter(binding,
                                       writer,
                                       cfg.runtimeExceptionType(),
                                       false,
                                       true,
                                       cfg.nioDirectOnly(binding.getName()),
                                       true,
                                       true,
                                       false,
                                       false);
        emitter.addModifier(JavaMethodBindingEmitter.PRIVATE);
        if (cfg.allStatic()) {
          emitter.addModifier(JavaMethodBindingEmitter.STATIC);
        }
        emitter.addModifier(JavaMethodBindingEmitter.NATIVE);
        emitter.setReturnedArrayLengthExpression(cfg.returnedArrayLength(binding.getName()));
        allEmitters.add(emitter);

        // Optionally emit the entry point taking arrays which handles
        // both the public entry point taking arrays as well as the
        // indirect buffer case
        if (!cfg.nioDirectOnly(binding.getName()) &&
            binding.signatureCanUseIndirectNIO()) {
          emitter =
            new JavaMethodBindingEmitter(binding,
                                         writer,
                                         cfg.runtimeExceptionType(),
                                         false,
                                         true,
                                         false,
                                         true,
                                         false,
                                         true,
                                         false);

          emitter.addModifier(JavaMethodBindingEmitter.PRIVATE);
          if (cfg.allStatic()) {
            emitter.addModifier(JavaMethodBindingEmitter.STATIC);
          }
          emitter.addModifier(JavaMethodBindingEmitter.NATIVE);
          emitter.setReturnedArrayLengthExpression(cfg.returnedArrayLength(binding.getName()));
          allEmitters.add(emitter);
        }
      }
    }

    // Now generate the C emitter(s). We need to produce one for every
    // Java native entry point (public or private). The only
    // situations where we don't produce one are (a) when the method
    // is unimplemented, and (b) when the signature contains primitive
    // arrays, since the latter is handled by the method binding
    // variant taking only NIO Buffers.
    if (!cfg.isUnimplemented(binding.getName()) &&
        !binding.signatureUsesJavaPrimitiveArrays()) {
      // See whether we need an expression to help calculate the
      // length of any return type
      MessageFormat returnValueCapacityFormat = null;         
      MessageFormat returnValueLengthFormat = null;         
      JavaType javaReturnType = binding.getJavaReturnType();
      if (javaReturnType.isNIOBuffer() ||
          javaReturnType.isCompoundTypeWrapper()) {
        // See whether capacity has been specified
        String capacity = cfg.returnValueCapacity(binding.getName());
        if (capacity != null) {
          returnValueCapacityFormat = new MessageFormat(capacity);
        }
      } else if (javaReturnType.isArray() ||
                 javaReturnType.isArrayOfCompoundTypeWrappers()) {
        // NOTE: adding a check here because the CMethodBindingEmitter
        // also doesn't yet handle returning scalar arrays. In order
        // to implement this, return the type as a Buffer instead
        // (i.e., IntBuffer, FloatBuffer) and add code as necessary.
        if (javaReturnType.isPrimitiveArray()) {
          throw new RuntimeException("Primitive array return types not yet supported");
        }

        // See whether length has been specified
        String len = cfg.returnValueLength(binding.getName());
        if (len != null) {
          returnValueLengthFormat = new MessageFormat(len);
        }
      }

      CMethodBindingEmitter cEmitter =
        new CMethodBindingEmitter(binding,
                                  cWriter(),
                                  cfg.implPackageName(),
                                  cfg.implClassName(),
                                  true, /* NOTE: we always disambiguate with a suffix now, so this is optional */
                                  cfg.allStatic(),
                                  binding.needsNIOWrappingOrUnwrapping(),
                                  false);
      if (returnValueCapacityFormat != null) {
        cEmitter.setReturnValueCapacityExpression(returnValueCapacityFormat);
      }
      if (returnValueLengthFormat != null) {
        cEmitter.setReturnValueLengthExpression(returnValueLengthFormat);
      }
      cEmitter.setTemporaryCVariableDeclarations(cfg.temporaryCVariableDeclarations(binding.getName()));
      cEmitter.setTemporaryCVariableAssignments(cfg.temporaryCVariableAssignments(binding.getName()));
      allEmitters.add(cEmitter);

      // Now see if we have to emit another entry point to handle the
      // indirect buffer and array case
      if (binding.argumentsUseNIO() &&
          binding.signatureCanUseIndirectNIO() &&
          !cfg.nioDirectOnly(binding.getName())) {
        cEmitter =
          new CMethodBindingEmitter(binding,
                                    cWriter(),
                                    cfg.implPackageName(),
                                    cfg.implClassName(),
                                    true, /* NOTE: we always disambiguate with a suffix now, so this is optional */
                                    cfg.allStatic(),
                                    binding.needsNIOWrappingOrUnwrapping(),
                                    true);
        if (returnValueCapacityFormat != null) {
          cEmitter.setReturnValueCapacityExpression(returnValueCapacityFormat);
        }
        if (returnValueLengthFormat != null) {
          cEmitter.setReturnValueLengthExpression(returnValueLengthFormat);
        }
        cEmitter.setTemporaryCVariableDeclarations(cfg.temporaryCVariableDeclarations(binding.getName()));
        cEmitter.setTemporaryCVariableAssignments(cfg.temporaryCVariableAssignments(binding.getName()));
        allEmitters.add(cEmitter);
      }
    }
  }

  /**
   * Generate all appropriate Java bindings for the specified C function
   * symbols.
   */
  protected List generateMethodBindingEmitters(FunctionSymbol sym) throws Exception {

    ArrayList/*<FunctionEmitter>*/ allEmitters = new ArrayList();

    try {
      // Get Java binding for the function
      MethodBinding mb = bindFunction(sym, null, null);
      
      // JavaTypes representing C pointers in the initial
      // MethodBinding have not been lowered yet to concrete types
      List bindings = expandMethodBinding(mb);
      
      for (Iterator iter = bindings.iterator(); iter.hasNext(); ) {
        MethodBinding binding = (MethodBinding) iter.next();        

        if (cfg.allStatic() && binding.hasContainingType()) {
          // This should not currently happen since structs are emitted using a different mechanism
          throw new IllegalArgumentException("Cannot create binding in AllStatic mode because method has containing type: \"" +
                                             binding + "\"");
        }

        // The structure of the generated glue code looks something like this:
        // Simple method (no arrays, void pointers, etc.):
        //   Interface class:
        //     public void fooMethod();
        //   Implementation class:
        //     public native void fooMethod();
        //
        // Method taking void* argument:
        //   Interface class:
        //     public void fooMethod(Buffer arg);
        //   Implementation class:
        //     public void fooMethod(Buffer arg) {
        //       ... bounds checks, etc. ...
        //       if (arg.isDirect()) {
        //         fooMethod0(arg, computeDirectBufferByteOffset(arg));
        //       } else {
        //         fooMethod1(getIndirectBufferArray(arg), computeIndirectBufferByteOffset(arg));
        //       }
        //     }
        //     private native void fooMethod0(Object arg, int arg_byte_offset);
        //     private native void fooMethod1(Object arg, int arg_byte_offset);
        //
        // Method taking primitive array argument:
        //   Interface class:
        //     public void fooMethod(int[] arg, int arg_offset);
        //     public void fooMethod(IntBuffer arg);
        //   Implementing class:
        //     public void fooMethod(int[] arg, int arg_offset) {
        //       ... range checks, etc. ...
        //       fooMethod1(arg, SIZEOF_INT * arg_offset);
        //     }
        //     public void fooMethod(IntBuffer arg) {
        //       ... bounds checks, etc. ...
        //       if (arg.isDirect()) {
        //         fooMethod0(arg, computeDirectBufferByteOffset(arg));
        //       } else {
        //         fooMethod1(getIndirectBufferArray(arg), computeIndirectBufferByteOffset(arg));
        //       }
        //     }
        //     private native void fooMethod0(Object arg, int arg_byte_offset);
        //     private native void fooMethod1(Object arg, int arg_byte_offset);
        //
        // Note in particular that the public entry point taking an
        // array is merely a special case of the indirect buffer case.

        if (cfg.emitInterface()) {
          generatePublicEmitters(binding, allEmitters, true);
        }
        if (cfg.emitImpl()) {
          generatePublicEmitters(binding, allEmitters, false);
          generatePrivateEmitters(binding, allEmitters);
        }
      } // end iteration over expanded bindings
    } catch (Exception e) {
      throw new RuntimeException(
        "Error while generating bindings for \"" + sym + "\"", e);
    }

    return allEmitters;
  }

    
  public void endFunctions() throws Exception
  {
    if (cfg.allStatic() || cfg.emitInterface()) {
      emitCustomJavaCode(javaWriter(), cfg.className());
    }
    if (!cfg.allStatic() && cfg.emitImpl()) {
      emitCustomJavaCode(javaImplWriter(), cfg.implClassName());
    }
  }

  public void beginStructLayout() throws Exception {}
  public void layoutStruct(CompoundType t) throws Exception {
    getLayout().layout(t);
  }
  public void endStructLayout() throws Exception {}

  public void beginStructs(TypeDictionary typedefDictionary,
                           TypeDictionary structDictionary,
                           Map            canonMap) throws Exception {
    this.typedefDictionary = typedefDictionary;
    this.structDictionary  = structDictionary;
    this.canonMap          = canonMap;
  }

  public void emitStruct(CompoundType structType, String alternateName) throws Exception {
    String name = structType.getName();
    if (name == null && alternateName != null) {
      name = alternateName;
    }

    if (name == null) {
      System.err.println("WARNING: skipping emission of unnamed struct \"" + structType + "\"");
      return;
    }

    if (cfg.shouldIgnore(name)) {
      return;
    }

    Type containingCType = canonicalize(new PointerType(machDesc.pointerSizeInBytes(), structType, 0));
    JavaType containingType = typeToJavaType(containingCType, false);
    if (!containingType.isCompoundTypeWrapper()) {
      return;
    }
    String containingTypeName = containingType.getName();

    boolean needsNativeCode = false;
    for (int i = 0; i < structType.getNumFields(); i++) {
      if (structType.getField(i).getType().isFunctionPointer()) {
        needsNativeCode = true;
        break;
      }
    }

    String structClassPkg = cfg.packageForStruct(name);
    PrintWriter writer = null;
    PrintWriter cWriter = null;
    try
    {
      writer = openFile(
        cfg.javaOutputDir() + File.separator +
        CodeGenUtils.packageAsPath(structClassPkg) +
        File.separator + containingTypeName + ".java");
      CodeGenUtils.emitAutogeneratedWarning(writer, this);
      if (needsNativeCode) {
        String nRoot = cfg.nativeOutputDir();
        if (cfg.nativeOutputUsesJavaHierarchy()) {
          nRoot +=
            File.separator +
            CodeGenUtils.packageAsPath(cfg.packageName());
        }
        cWriter = openFile(nRoot + File.separator + containingTypeName + "_JNI.c");
        CodeGenUtils.emitAutogeneratedWarning(cWriter, this);
        emitCHeader(cWriter, containingTypeName);
      }
    }
    catch(Exception e)
    {
      throw new RuntimeException(
        "Unable to open files for emission of struct class", e);
    }
    
    writer.println();
    writer.println("package " + structClassPkg + ";");
    writer.println();
    writer.println("import java.nio.*;");
    writer.println();
    writer.println("import net.java.games.gluegen.runtime.*;");
    writer.println();
    List/*<String>*/ imports = cfg.imports();
    for (Iterator iter = imports.iterator(); iter.hasNext(); ) {
      writer.print("import ");
      writer.print(iter.next());
      writer.println(";");
    }
    List/*<String>*/ javadoc = cfg.javadocForClass(containingTypeName);
    for (Iterator iter = javadoc.iterator(); iter.hasNext(); ) {
      writer.println((String) iter.next());
    }
    writer.println();
    writer.print("public class " + containingTypeName + " ");
    boolean firstIteration = true;
    List/*<String>*/ userSpecifiedInterfaces = cfg.implementedInterfaces(containingTypeName);
    for (Iterator iter = userSpecifiedInterfaces.iterator(); iter.hasNext(); ) {
      if (firstIteration) {
        writer.print("implements ");
      }
      firstIteration = false;
      writer.print(iter.next());
      writer.print(" ");
    }
    writer.println("{");
    writer.println("  private StructAccessor accessor;");
    writer.println();
    writer.println("  public static int size() {");
    writer.println("    return " + structType.getSize() + ";");
    writer.println("  }");
    writer.println();
    writer.println("  public " + containingTypeName + "() {");
    writer.println("    this(BufferFactory.newDirectByteBuffer(size()));");
    writer.println("  }");
    writer.println();
    writer.println("  public " + containingTypeName + "(ByteBuffer buf) {");
    writer.println("    accessor = new StructAccessor(buf);");
    writer.println("  }");
    writer.println();
    writer.println("  public ByteBuffer getBuffer() {");
    writer.println("    return accessor.getBuffer();");
    writer.println("  }");
    for (int i = 0; i < structType.getNumFields(); i++) {
      Field field = structType.getField(i);
      Type fieldType = field.getType();
      if (!cfg.shouldIgnore(name + " " + field.getName())) {
        if (fieldType.isFunctionPointer()) {
          try {
            // Emit method call and associated native code
            FunctionType   funcType     = fieldType.asPointer().getTargetType().asFunction();
            FunctionSymbol funcSym      = new FunctionSymbol(field.getName(), funcType);
            MethodBinding  binding      = bindFunction(funcSym, containingType, containingCType);
            binding.findThisPointer(); // FIXME: need to provide option to disable this on per-function basis
            writer.println();

            // Emit public Java entry point for calling this function pointer
            JavaMethodBindingEmitter emitter =
              new JavaMethodBindingEmitter(binding,
                                           writer,
                                           cfg.runtimeExceptionType(),
                                           true,
                                           false,
                                           true, // FIXME: should unify this with the general emission code
                                           false,
                                           false, // FIXME: should unify this with the general emission code
                                           false, // FIXME: should unify this with the general emission code
                                           false);
            emitter.addModifier(JavaMethodBindingEmitter.PUBLIC);
            emitter.emit();

            // Emit private native Java entry point for calling this function pointer
            emitter =
              new JavaMethodBindingEmitter(binding,
                                           writer,
                                           cfg.runtimeExceptionType(),
                                           false,
                                           true,
                                           true, // FIXME: should unify this with the general emission code
                                           true,
                                           true, // FIXME: should unify this with the general emission code
                                           false, // FIXME: should unify this with the general emission code
                                           false);
            emitter.addModifier(JavaMethodBindingEmitter.PRIVATE);
            emitter.addModifier(JavaMethodBindingEmitter.NATIVE);
            emitter.emit();

            // Emit (private) C entry point for calling this function pointer
            CMethodBindingEmitter cEmitter =
              new CMethodBindingEmitter(binding,
                                        cWriter,
                                        structClassPkg,
                                        containingTypeName,
                                        true, // FIXME: this is optional at this point
                                        false,
                                        true,
                                        false); // FIXME: should unify this with the general emission code
            cEmitter.emit();
          } catch (Exception e) {
            System.err.println("While processing field " + field + " of type " + name + ":");
            throw(e);
          }
        } else if (fieldType.isCompound()) {
          // FIXME: will need to support this at least in order to
          // handle the union in jawt_Win32DrawingSurfaceInfo (fabricate
          // a name?)
          if (fieldType.getName() == null) {
            throw new RuntimeException("Anonymous structs as fields not supported yet (field \"" +
                                       field + "\" in type \"" + name + "\")");
          }
        
          writer.println();
          writer.println("  public " + fieldType.getName() + " " + field.getName() + "() {");
          writer.println("    return new " + fieldType.getName() + "(accessor.slice(" +
                         field.getOffset() + ", " + fieldType.getSize() + "));");
          writer.println("  }");

          // FIXME: add setter by autogenerating "copyTo" for all compound type wrappers
        } else if (fieldType.isArray()) {
          System.err.println("WARNING: Array fields (field \"" + field + "\" of type \"" + name +
                             "\") not implemented yet");
        } else {
          JavaType javaType = null;
          try {
            javaType = typeToJavaType(fieldType, false);
          } catch (Exception e) {
            System.err.println("Error occurred while creating accessor for field \"" +
                               field.getName() + "\" in type \"" + name + "\"");
            e.printStackTrace();
            throw(e);
          }
          if (javaType.isPrimitive()) {
            // Primitive type
            String externalJavaTypeName = javaType.getName();
            String internalJavaTypeName = externalJavaTypeName;
            if (isOpaque(fieldType)) {
              internalJavaTypeName = compatiblePrimitiveJavaTypeName(fieldType, javaType);
            }
            String capitalized =
              "" + Character.toUpperCase(internalJavaTypeName.charAt(0)) + internalJavaTypeName.substring(1);
            int slot = slot(fieldType, (int) field.getOffset());
            // Setter
            writer.println();
            writer.println("  public " + containingTypeName + " " + field.getName() + "(" + externalJavaTypeName + " val) {");
            writer.print  ("    accessor.set" + capitalized + "At(" + slot + ", ");
            if (!externalJavaTypeName.equals(internalJavaTypeName)) {
              writer.print("(" + internalJavaTypeName + ") ");
            }
            writer.println("val);");
            writer.println("    return this;");
            writer.println("  }");
            // Getter
            writer.println();
            writer.println("  public " + externalJavaTypeName + " " + field.getName() + "() {");
            writer.print  ("    return ");
            if (!externalJavaTypeName.equals(internalJavaTypeName)) {
              writer.print("(" + externalJavaTypeName + ") ");
            }
            writer.println("accessor.get" + capitalized + "At(" + slot + ");");
            writer.println("  }");
          } else {
            // FIXME
            System.err.println("WARNING: Complicated fields (field \"" + field + "\" of type \"" + name +
                               "\") not implemented yet");
            //          throw new RuntimeException("Complicated fields (field \"" + field + "\" of type \"" + t +
            //                                     "\") not implemented yet");
          }
        }
      }
    }
    emitCustomJavaCode(writer, containingTypeName);
    writer.println("}");
    writer.flush();
    writer.close();
    if (needsNativeCode) {
      cWriter.flush();
      cWriter.close();
    }
  }
  public void endStructs() throws Exception {}

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private JavaType typeToJavaType(Type cType, boolean outgoingArgument) {
    // Recognize JNIEnv* case up front
    PointerType opt = cType.asPointer();
    if ((opt != null) &&
        (opt.getTargetType().getName() != null) &&
        (opt.getTargetType().getName().equals("JNIEnv"))) {
      return JavaType.createForJNIEnv();
    }

    // Opaque specifications override automatic conversions
    TypeInfo info = cfg.typeInfo(cType, typedefDictionary);
    if (info != null) {
      return info.javaType();
    }
    Type t = cType;
    if (t.isInt() || t.isEnum()) {
      switch (t.getSize()) {
       case 1:  return javaType(Byte.TYPE);
       case 2:  return javaType(Short.TYPE);
       case 4:  return javaType(Integer.TYPE);
       case 8:  return javaType(Long.TYPE);
       default: throw new RuntimeException("Unknown integer type of size " +
                                           t.getSize() + " and name " + t.getName());
      }
    } else if (t.isFloat()) {
      return javaType(Float.TYPE);
    } else if (t.isDouble()) {
      return javaType(Double.TYPE);
    } else if (t.isVoid()) {
      return javaType(Void.TYPE);
    } else {
      if (t.pointerDepth() > 0 || t.arrayDimension() > 0) {
        Type targetType; // target type 
        if (t.isPointer()) {
          // t is <type>*, we need to get <type>
          targetType = t.asPointer().getTargetType();
        } else {
          // t is <type>[], we need to get <type>
          targetType = t.asArray().getElementType(); 
        }

        // Handle Types of form pointer-to-type or array-of-type, like
        // char* or int[]; these are expanded out into Java primitive
        // arrays, NIO buffers, or both in expandMethodBinding
        if (t.pointerDepth() == 1 || t.arrayDimension() == 1) {
          if (targetType.isVoid()) {
            return JavaType.createForVoidPointer();
          } else if (targetType.isInt()) {
            switch (targetType.getSize()) {
              case 1:  return JavaType.createForCCharPointer();
              case 2:  return JavaType.createForCShortPointer();
              case 4:  return JavaType.createForCInt32Pointer();
              case 8:  return JavaType.createForCInt64Pointer();
              default: throw new RuntimeException("Unknown integer array type of size " +
                                                  t.getSize() + " and name " + t.getName());
            }
          } else if (targetType.isFloat()) {
            return JavaType.createForCFloatPointer();
          } else if (targetType.isDouble()) {
            return JavaType.createForCDoublePointer();
          } else if (targetType.isCompound()) {
            if (t.isArray()) {
              throw new RuntimeException("Arrays of compound types not handled yet");
            }
            // Special cases for known JNI types (in particular for converting jawt.h)
            if (t.getName() != null &&
                t.getName().equals("jobject")) {
              return javaType(java.lang.Object.class);
            }

            String name = targetType.getName();
            if (name == null) {
              // Try containing pointer type for any typedefs
              name = t.getName();
              if (name == null) {
                throw new RuntimeException("Couldn't find a proper type name for pointer type " + t);
              }
            }

            return JavaType.createForCStruct(cfg.renameJavaType(name));
          } else {
            throw new RuntimeException("Don't know how to convert pointer/array type \"" +
                                       t + "\"");
          }
        }
        // Handle Types of form pointer-to-pointer-to-type or
        // array-of-arrays-of-type, like char** or int[][]
        else if (t.pointerDepth() == 2 || t.arrayDimension() == 2) {
          // Get the target type of the target type (targetType was computer earlier
          // as to be a pointer to the target type, so now we need to get its
          // target type)
          Type bottomType;
          if (targetType.isPointer()) {
            // t is<type>**, targetType is <type>*, we need to get <type>
            bottomType = targetType.asPointer().getTargetType(); 
          } else {
            // t is<type>[][], targetType is <type>[], we need to get <type>
            bottomType = targetType.asArray().getElementType(); 
          }

          if (bottomType.isPrimitive()) {
            if (bottomType.isInt()) {
              switch (bottomType.getSize()) {
                case 1: return javaType(ArrayTypes.byteBufferArrayClass);
                case 2: return javaType(ArrayTypes.shortBufferArrayClass);
                case 4: return javaType(ArrayTypes.intBufferArrayClass);
                case 8: return javaType(ArrayTypes.longBufferArrayClass);
                default: throw new RuntimeException("Unknown two-dimensional integer array type of element size " +
                                                    bottomType.getSize() + " and name " + bottomType.getName());
              }
            } else if (bottomType.isFloat()) {
              return javaType(ArrayTypes.floatBufferArrayClass);
            } else if (bottomType.isDouble()) {
              return javaType(ArrayTypes.doubleBufferArrayClass);
            } else {
              throw new RuntimeException("Unexpected primitive type " + bottomType.getName() +
                                         " in two-dimensional array");
            }
          } else if (bottomType.isVoid()) {
            return javaType(ArrayTypes.bufferArrayClass);
          } else if (targetType.isPointer() && (targetType.pointerDepth() == 1) &&
                     targetType.asPointer().getTargetType().isCompound()) {
            // Array of pointers; convert as array of StructAccessors
            return JavaType.createForCArray(targetType);
          } else {
            throw new RuntimeException(
              "Could not convert C type \"" + t + "\" " +
              "to appropriate Java type; need to add more support for " +
              "depth=2 pointer/array types [debug info: targetType=\"" +
              targetType + "\"]");            
          }
        } else {
          // can't handle this type of pointer/array argument
          throw new RuntimeException(
            "Could not convert C pointer/array \"" + t + "\" to " +
            "appropriate Java type; types with pointer/array depth " +
            "greater than 2 are not yet supported [debug info: " +
            "pointerDepth=" + t.pointerDepth() + " arrayDimension=" +
            t.arrayDimension() + " targetType=\"" + targetType + "\"]");
        }
        
      } else {
        throw new RuntimeException(
          "Could not convert C type \"" + t + "\" (class " +
          t.getClass().getName() + ") to appropriate Java type");
      }
    }    
  }

  private static boolean isIntegerType(Class c) {
    return ((c == Byte.TYPE) ||
            (c == Short.TYPE) ||
            (c == Character.TYPE) ||
            (c == Integer.TYPE) ||
            (c == Long.TYPE));
  }

  private int slot(Type t, int byteOffset) {
    if (t.isInt()) {
      switch (t.getSize()) {
       case 1:  
       case 2:  
       case 4:  
       case 8:  return byteOffset / t.getSize();
       default: throw new RuntimeException("Illegal type");
      }
    } else if (t.isFloat()) {
      return byteOffset / 4;
    } else if (t.isDouble()) {
      return byteOffset / 8;
    } else if (t.isPointer()) {
      return byteOffset / machDesc.pointerSizeInBytes();
    } else {
      throw new RuntimeException("Illegal type " + t);
    }
  }

  private StructLayout getLayout() {
    if (layout == null) {
      layout = StructLayout.createForCurrentPlatform();
    }
    return layout;
  }

  protected PrintWriter openFile(String filename) throws IOException {
    //System.out.println("Trying to open: " + filename);
    File file = new File(filename);
    String parentDir = file.getParent();
    if (parentDir != null)
    {
      File pDirFile = new File(parentDir);
      pDirFile.mkdirs();
    }
    return new PrintWriter(new BufferedWriter(new FileWriter(file)));
  }

  private boolean isOpaque(Type type) {
    return (cfg.typeInfo(type, typedefDictionary) != null);
  }

  private String compatiblePrimitiveJavaTypeName(Type fieldType,
                                                 JavaType javaType) {
    Class c = javaType.getJavaClass();
    if (!isIntegerType(c)) {
      // FIXME
      throw new RuntimeException("Can't yet handle opaque definitions of structs' fields to non-integer types (byte, short, int, long, etc.)");
    }
    switch (fieldType.getSize()) {
      case 1:  return "byte";
      case 2:  return "short";
      case 4:  return "int";
      case 8:  return "long";
      default: throw new RuntimeException("Can't handle opaque definitions if the starting type isn't compatible with integral types");
    }
  }

  private void openWriters() throws IOException {
    String jRoot =
      cfg.javaOutputDir() + File.separator +
      CodeGenUtils.packageAsPath(cfg.packageName());
    String jImplRoot = null;
    if (!cfg.allStatic()) {
      jImplRoot =
        cfg.javaOutputDir() + File.separator +
        CodeGenUtils.packageAsPath(cfg.implPackageName());
    }
    String nRoot = cfg.nativeOutputDir();
    if (cfg.nativeOutputUsesJavaHierarchy())
    {
      nRoot +=
        File.separator + CodeGenUtils.packageAsPath(cfg.packageName());
    }
    
    if (cfg.allStatic() || cfg.emitInterface()) {
      javaWriter = openFile(jRoot + File.separator + cfg.className() + ".java");
    }
    if (!cfg.allStatic() && cfg.emitImpl()) {
      javaImplWriter = openFile(jImplRoot + File.separator + cfg.implClassName() + ".java");
    }
    if (cfg.emitImpl()) {
      cWriter = openFile(nRoot + File.separator + cfg.implClassName() + "_JNI.c");
    }

    if (javaWriter != null) {
      CodeGenUtils.emitAutogeneratedWarning(javaWriter, this);
    }
    if (javaImplWriter != null) {
      CodeGenUtils.emitAutogeneratedWarning(javaImplWriter, this);
    }
    if (cWriter != null) {
      CodeGenUtils.emitAutogeneratedWarning(cWriter, this);
    }
  }

  protected PrintWriter javaWriter() {
    if (!cfg.allStatic() && !cfg.emitInterface()) {
      throw new InternalError("Should not call this");
    }
    return javaWriter;
  }

  protected PrintWriter javaImplWriter() {
    if (cfg.allStatic() || !cfg.emitImpl()) {
      throw new InternalError("Should not call this");
    }
    return javaImplWriter;
  }
  
  protected PrintWriter cWriter() {
    if (!cfg.emitImpl()) {
      throw new InternalError("Should not call this");
    }
    return cWriter;
  }

  private void closeWriter(PrintWriter writer) throws IOException {
    writer.flush();
    writer.close();
  }

  private void closeWriters() throws IOException {
    if (javaWriter != null) {
      closeWriter(javaWriter);
    }
    if (javaImplWriter != null) {
      closeWriter(javaImplWriter);
    }
    if (cWriter != null) {
      closeWriter(cWriter);
    }
    javaWriter = null;
    javaImplWriter = null;
    cWriter = null;
  }

  /**
   * Returns the value that was specified by the configuration directive
   * "JavaOutputDir", or the default if none was specified.
   */
  protected String getJavaOutputDir() {
    return cfg.javaOutputDir();
  }

  /**
   * Returns the value that was specified by the configuration directive
   * "Package", or the default if none was specified.
   */
  protected String getJavaPackageName() {
    return cfg.packageName();
  }

  /**
   * Returns the value that was specified by the configuration directive
   * "ImplPackage", or the default if none was specified.
   */
  protected String getImplPackageName() {
    return cfg.implPackageName();
  }

  /**
   * Emit all the strings specified in the "CustomJavaCode" parameters of
   * the configuration file.
   */
  protected void emitCustomJavaCode(PrintWriter writer, String className) throws Exception
  {
    List code = cfg.customJavaCodeForClass(className);
    if (code.size() == 0)
      return;

    writer.println();
    writer.println("  // --- Begin CustomJavaCode .cfg declarations"); 
    for (Iterator iter = code.iterator(); iter.hasNext(); ) {
      writer.println((String) iter.next());
    }
    writer.println("  // ---- End CustomJavaCode .cfg declarations"); 
  }
  
  /**
   * Write out any header information for the output files (class declaration
   * and opening brace, import statements, etc).
   */
  protected void emitAllFileHeaders() throws IOException {    
    try {    
      if (cfg.allStatic() || cfg.emitInterface()) {
        String[] interfaces;
        List userSpecifiedInterfaces = null;
        if (cfg.emitInterface()) {
          userSpecifiedInterfaces = cfg.extendedInterfaces(cfg.className());
        } else {
          userSpecifiedInterfaces = cfg.implementedInterfaces(cfg.className());
        }
        interfaces = new String[userSpecifiedInterfaces.size()];
        userSpecifiedInterfaces.toArray(interfaces);
        
        final List/*<String>*/ intfDocs = cfg.javadocForClass(cfg.className());
        CodeGenUtils.EmissionCallback docEmitter =
          new CodeGenUtils.EmissionCallback() {
            public void emit(PrintWriter w) {
              for (Iterator iter = intfDocs.iterator(); iter.hasNext(); ) {
                w.println((String) iter.next());
              }
            }
          };

        CodeGenUtils.emitJavaHeaders(
          javaWriter,
          cfg.packageName(),
          cfg.className(),
          cfg.allStatic() ? true : false, 
          (String[]) cfg.imports().toArray(new String[] {}),
          new String[] { "public" },
          interfaces,
          null,
          docEmitter);               
      }
    
      if (!cfg.allStatic() && cfg.emitImpl()) {
        final List/*<String>*/ implDocs = cfg.javadocForClass(cfg.className());
        CodeGenUtils.EmissionCallback docEmitter =
          new CodeGenUtils.EmissionCallback() {
            public void emit(PrintWriter w) {
              for (Iterator iter = implDocs.iterator(); iter.hasNext(); ) {
                w.println((String) iter.next());
              }
            }
          };

        String[] interfaces;
        List userSpecifiedInterfaces = null;
        userSpecifiedInterfaces = cfg.implementedInterfaces(cfg.implClassName());
        interfaces = new String[1 + userSpecifiedInterfaces.size()];
        userSpecifiedInterfaces.toArray(interfaces);
        interfaces[userSpecifiedInterfaces.size()] = cfg.className();

        CodeGenUtils.emitJavaHeaders(
          javaImplWriter,
          cfg.implPackageName(),
          cfg.implClassName(),
          true,
          (String[]) cfg.imports().toArray(new String[] {}),
          new String[] { "public" },
          interfaces,
          null,
          docEmitter);                      
      }
          
      if (cfg.emitImpl()) {
        PrintWriter cWriter = cWriter();
        emitCHeader(cWriter, cfg.implClassName());
      }
    } catch (Exception e) {
      throw new RuntimeException(
        "Error emitting all file headers: cfg.allStatic()=" + cfg.allStatic() +
        " cfg.emitImpl()=" + cfg.emitImpl() + " cfg.emitInterface()=" + cfg.emitInterface(),
        e);       
    }
    
  }
  
  protected void emitCHeader(PrintWriter cWriter, String className) {
    cWriter.println("#include <jni.h>");
    cWriter.println();

    if (getConfig().emitImpl()) {
      cWriter.println("#include <assert.h>"); 
      cWriter.println();
    }

    for (Iterator iter = cfg.customCCode().iterator(); iter.hasNext(); ) {
      cWriter.println((String) iter.next());
    }
    cWriter.println();
  }
  
  /**
   * Write out any footer information for the output files (closing brace of
   * class definition, etc).
   */
  protected void emitAllFileFooters(){
    if (cfg.allStatic() || cfg.emitInterface()) {
      javaWriter().println();
      javaWriter().println("} // end of class " + cfg.className());
    }
    if (!cfg.allStatic() && cfg.emitImpl())
    {
      javaImplWriter().println();
      javaImplWriter().println("} // end of class " + cfg.implClassName());
    }
  }

  private JavaType javaType(Class c) {
    return JavaType.createForClass(c);
  }

  /** Maps the C types in the specified function to Java types through
      the MethodBinding interface. Note that the JavaTypes in the
      returned MethodBinding are "intermediate" JavaTypes (some
      potentially representing C pointers rather than true Java types)
      and must be lowered to concrete Java types before creating
      emitters for them. */
  private MethodBinding bindFunction(FunctionSymbol sym,
                                     JavaType containingType,
                                     Type containingCType) {

    MethodBinding binding = new MethodBinding(sym, containingType, containingCType);
    
    if (cfg.returnsString(binding.getName())) {
      PointerType prt = sym.getReturnType().asPointer();
      if (prt == null ||
          prt.getTargetType().asInt() == null ||
          prt.getTargetType().getSize() != 1) {
        throw new RuntimeException(
          "Cannot apply ReturnsString configuration directive to \"" + sym +
          "\". ReturnsString requires native method to have return type \"char *\"");
      }
      binding.setJavaReturnType(javaType(java.lang.String.class));
    } else {
      binding.setJavaReturnType(typeToJavaType(sym.getReturnType(), false));
    }

    // List of the indices of the arguments in this function that should be
    // converted from byte[] to String
    List stringArgIndices = cfg.stringArguments(binding.getName());

    for (int i = 0; i < sym.getNumArguments(); i++) {
      Type cArgType = sym.getArgumentType(i);
      JavaType mappedType = typeToJavaType(cArgType, true);
      //System.out.println("C arg type -> \"" + cArgType + "\"" );
      //System.out.println("      Java -> \"" + mappedType + "\"" );
     
      // Take into account any ArgumentIsString configuration directives that apply
      if (stringArgIndices != null && stringArgIndices.contains(new Integer(i))) {   
        //System.out.println("Forcing conversion of " + binding.getName() + " arg #" + i + " from byte[] to String ");
        if (mappedType.isCVoidPointerType() ||
            mappedType.isCCharPointerType() ||
            (mappedType.isArray() && mappedType.getJavaClass() == ArrayTypes.byteBufferArrayClass)) {
          // convert mapped type from void* and byte[] to String, or ByteBuffer[] to String[]
          if (mappedType.getJavaClass() == ArrayTypes.byteBufferArrayClass) {
            mappedType = javaType(ArrayTypes.stringArrayClass);
          } else {         
            mappedType = javaType(String.class);
          }
        }
        else {
        throw new RuntimeException(
          "Cannot apply ArgumentIsString configuration directive to " +
          "argument " + i + " of \"" + sym + "\": argument type is not " +
          "a \"void*\", \"char *\", or \"char**\" equivalent");
        }
      }
      binding.addJavaArgumentType(mappedType);
      //System.out.println("During binding of [" + sym + "], added mapping from C type: " + cArgType + " to Java type: " + mappedType);
    }

    //System.err.println("---> " + binding);
    //System.err.println("    ---> " + binding.getCSymbol());
    return binding;
  }
  

  private MethodBinding lowerMethodBindingPointerTypes(MethodBinding inputBinding,
                                                       boolean convertToArrays,
                                                       boolean[] canProduceArrayVariant) {
    MethodBinding result = inputBinding;
    boolean arrayPossible = false;
    
    for (int i = 0; i < inputBinding.getNumArguments(); i++) {
      JavaType t = inputBinding.getJavaArgumentType(i);
      if (t.isCPrimitivePointerType()) {
        if (t.isCVoidPointerType()) {
          // These are always bound to java.nio.Buffer
          result = result.replaceJavaArgumentType(i, JavaType.forNIOBufferClass());
        } else if (t.isCCharPointerType()) {
          arrayPossible = true;
          if (convertToArrays) {
            result = result.replaceJavaArgumentType(i, javaType(ArrayTypes.byteArrayClass));
          } else {
            result = result.replaceJavaArgumentType(i, JavaType.forNIOByteBufferClass());
          }
        } else if (t.isCShortPointerType()) {
          arrayPossible = true;
          if (convertToArrays) {
            result = result.replaceJavaArgumentType(i, javaType(ArrayTypes.shortArrayClass));
          } else {
            result = result.replaceJavaArgumentType(i, JavaType.forNIOShortBufferClass());
          }
        } else if (t.isCInt32PointerType()) {
          arrayPossible = true;
          if (convertToArrays) {
            result = result.replaceJavaArgumentType(i, javaType(ArrayTypes.intArrayClass));
          } else {
            result = result.replaceJavaArgumentType(i, JavaType.forNIOIntBufferClass());
          }
        } else if (t.isCInt64PointerType()) {
          arrayPossible = true;
          if (convertToArrays) {
            result = result.replaceJavaArgumentType(i, javaType(ArrayTypes.longArrayClass));
          } else {
            result = result.replaceJavaArgumentType(i, JavaType.forNIOLongBufferClass());
          }
        } else if (t.isCFloatPointerType()) {
          arrayPossible = true;
          if (convertToArrays) {
            result = result.replaceJavaArgumentType(i, javaType(ArrayTypes.floatArrayClass));
          } else {
            result = result.replaceJavaArgumentType(i, JavaType.forNIOFloatBufferClass());
          }
        } else if (t.isCDoublePointerType()) {
          arrayPossible = true;
          if (convertToArrays) {
            result = result.replaceJavaArgumentType(i, javaType(ArrayTypes.doubleArrayClass));
          } else {
            result = result.replaceJavaArgumentType(i, JavaType.forNIODoubleBufferClass());
          }
        } else {
          throw new RuntimeException("Unknown C pointer type " + t);
        }
      }
    }

    // Always return primitive pointer types as NIO buffers
    JavaType t = result.getJavaReturnType();
    if (t.isCPrimitivePointerType()) {
      if (t.isCVoidPointerType()) {
        result = result.replaceJavaArgumentType(-1, JavaType.forNIOByteBufferClass());
      } else if (t.isCCharPointerType()) {
        result = result.replaceJavaArgumentType(-1, JavaType.forNIOByteBufferClass());
      } else if (t.isCShortPointerType()) {
        result = result.replaceJavaArgumentType(-1, JavaType.forNIOShortBufferClass());
      } else if (t.isCInt32PointerType()) {
        result = result.replaceJavaArgumentType(-1, JavaType.forNIOIntBufferClass());
      } else if (t.isCInt64PointerType()) {
        result = result.replaceJavaArgumentType(-1, JavaType.forNIOLongBufferClass());
      } else if (t.isCFloatPointerType()) {
        result = result.replaceJavaArgumentType(-1, JavaType.forNIOFloatBufferClass());
      } else if (t.isCDoublePointerType()) {
        result = result.replaceJavaArgumentType(-1, JavaType.forNIODoubleBufferClass());
      } else {
        throw new RuntimeException("Unknown C pointer type " + t);
      }
    }

    if (canProduceArrayVariant != null) {
      canProduceArrayVariant[0] = arrayPossible;
    }

    return result;
  }

  // Expands a MethodBinding containing C primitive pointer types into
  // multiple variants taking Java primitive arrays and NIO buffers, subject
  // to the per-function "NIO only" rule in the configuration file
  private List/*<MethodBinding>*/ expandMethodBinding(MethodBinding binding) {
    List result = new ArrayList();
    // Indicates whether it is possible to produce an array variant
    // Prevents e.g. char* -> String conversions from emitting two entry points
    boolean[] canProduceArrayVariant = new boolean[1];

    if (binding.signatureUsesCPrimitivePointers() ||
        binding.signatureUsesCVoidPointers() ||
        binding.signatureUsesCArrays()) {
      result.add(lowerMethodBindingPointerTypes(binding, false, canProduceArrayVariant));

      // FIXME: should add new configuration flag for this
      if (canProduceArrayVariant[0] &&
          (binding.signatureUsesCPrimitivePointers() ||
           binding.signatureUsesCArrays()) &&
          !cfg.nioDirectOnly(binding.getName())) {
        result.add(lowerMethodBindingPointerTypes(binding, true, null));
      }
    } else {
      result.add(binding);
    }

    return result;
  }

  private String resultName() {
    return "_res";
  }

  private Type canonicalize(Type t) {
    Type res = (Type) canonMap.get(t);
    if (res != null) {
      return res;
    }
    canonMap.put(t, t);
    return t;
  }
}

/*
 * Copyright (c) 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.gluegen.procaddress;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import com.sun.gluegen.*;
import com.sun.gluegen.cgram.types.*;
import com.sun.gluegen.runtime.*;

/**
 * A subclass of JavaEmitter that modifies the normal emission of C
 * and Java code to allow dynamic lookups of the C entry points
 * associated with the Java methods.
 */

public class ProcAddressEmitter extends JavaEmitter
{
  public static final String PROCADDRESS_VAR_PREFIX = ProcAddressHelper.PROCADDRESS_VAR_PREFIX;
  protected static final String WRAP_PREFIX = "dispatch_";
  private TypeDictionary typedefDictionary;
  private PrintWriter tableWriter;
  private String tableClassPackage;
  private String tableClassName;

  public void beginFunctions(TypeDictionary typedefDictionary,
                             TypeDictionary structDictionary,
                             Map            canonMap) throws Exception
  {
    this.typedefDictionary = typedefDictionary;

    if (getProcAddressConfig().emitProcAddressTable())
    {
      beginProcAddressTable();
    }
    super.beginFunctions(typedefDictionary, structDictionary, canonMap);
  }

  public void endFunctions() throws Exception
  {
    if (getProcAddressConfig().emitProcAddressTable())
    {
      endProcAddressTable();
    }
    super.endFunctions();
  }
  
  public void beginStructs(TypeDictionary typedefDictionary,
                           TypeDictionary structDictionary,
                           Map            canonMap) throws Exception {
    super.beginStructs(typedefDictionary, structDictionary, canonMap);
  }

  public String runtimeExceptionType() {
    return getConfig().runtimeExceptionType();
  }

  protected JavaConfiguration createConfig() {
    return new ProcAddressConfiguration();
  }

  protected List generateMethodBindingEmitters(FunctionSymbol sym) throws Exception
  {
    return generateMethodBindingEmittersImpl(sym);
  }

  protected boolean needsModifiedEmitters(FunctionSymbol sym) {
    if (!needsProcAddressWrapper(sym) ||
        getConfig().isUnimplemented(sym.getName())) {
      return false;
    }

    return true;
  }

  private List generateMethodBindingEmittersImpl(FunctionSymbol sym) throws Exception
  {
    List defaultEmitters = super.generateMethodBindingEmitters(sym);

    // if the superclass didn't generate any bindings for the symbol, let's
    // honor that (for example, the superclass might have caught an Ignore
    // direction that matched the symbol's name).
    if (defaultEmitters.isEmpty())
    {
      return defaultEmitters;
    }
    
    // Don't do anything special if this symbol doesn't require
    // modifications
    if (!needsModifiedEmitters(sym))
    {
      return defaultEmitters;
    }

    ArrayList modifiedEmitters = new ArrayList(defaultEmitters.size());

    if (needsProcAddressWrapper(sym)) {
      if (getProcAddressConfig().emitProcAddressTable()) {
        // emit an entry in the GL proc address table for this method.
        emitProcAddressTableEntryForSymbol(sym);
      }
    }
    
    for (Iterator iter = defaultEmitters.iterator(); iter.hasNext(); )
    {
      FunctionEmitter emitter = (FunctionEmitter) iter.next();
      if (emitter instanceof JavaMethodBindingEmitter)
      {
        generateModifiedEmitters((JavaMethodBindingEmitter) emitter, modifiedEmitters);
      }
      else if (emitter instanceof CMethodBindingEmitter)
      {
        generateModifiedEmitters((CMethodBindingEmitter) emitter, modifiedEmitters);
      }
      else
      {
        throw new RuntimeException("Unexpected emitter type: " +
                                   emitter.getClass().getName());
      }
    }
    
    return modifiedEmitters;
  }

  /**
   * Returns the name of the typedef for a pointer to the function
   * represented by the argument as defined by the ProcAddressNameExpr
   * in the .cfg file. For example, in the OpenGL headers, if the
   * argument is the function "glFuncName", the value returned will be
   * "PFNGLFUNCNAMEPROC". This returns a valid string regardless of
   * whether or not the typedef is actually defined.
   */
  protected String getFunctionPointerTypedefName(FunctionSymbol sym) {
    return getProcAddressConfig().convertToFunctionPointerName(sym.getName());
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //
  
  protected void generateModifiedEmitters(JavaMethodBindingEmitter baseJavaEmitter, List emitters) {
    if (getConfig().manuallyImplement(baseJavaEmitter.getName())) {
      // User will provide Java-side implementation of this routine;
      // pass through any emitters which will produce signatures for
      // it unmodified
      emitters.add(baseJavaEmitter);
      return;
    }
    
    // See whether we need a proc address entry for this one
    boolean callThroughProcAddress = needsProcAddressWrapper(baseJavaEmitter.getBinding().getCSymbol());

    ProcAddressJavaMethodBindingEmitter emitter =
      new ProcAddressJavaMethodBindingEmitter(baseJavaEmitter,
                                              callThroughProcAddress,
                                              getProcAddressConfig().getProcAddressTableExpr(),
                                              baseJavaEmitter.isForImplementingMethodCall(),
                                              this);
    emitters.add(emitter);

    // If this emitter doesn't have a body (i.e., is a public native
    // call), we need to force it to emit a body, and produce another
    // one to act as the entry point
    if (baseJavaEmitter.signatureOnly() &&
        baseJavaEmitter.hasModifier(JavaMethodBindingEmitter.PUBLIC) &&
        baseJavaEmitter.hasModifier(JavaMethodBindingEmitter.NATIVE) &&
        callThroughProcAddress) {
      emitter.setEmitBody(true);
      emitter.removeModifier(JavaMethodBindingEmitter.NATIVE);
      emitter = new ProcAddressJavaMethodBindingEmitter(baseJavaEmitter,
                                                        callThroughProcAddress,
                                                        getProcAddressConfig().getProcAddressTableExpr(),
                                                        true,
                                                        this);
      emitter.setForImplementingMethodCall(true);
      emitters.add(emitter);
    }
  }

  protected void generateModifiedEmitters(CMethodBindingEmitter baseCEmitter, List emitters)
  {    
    // See whether we need a proc address entry for this one
    boolean callThroughProcAddress = needsProcAddressWrapper(baseCEmitter.getBinding().getCSymbol());
    // Note that we don't care much about the naming of the C argument
    // variables so to keep things simple we ignore the buffer object
    // property for the binding

    // The C-side JNI binding for this particular function will have an
    // extra final argument, which is the address (the OpenGL procedure
    // address) of the function it needs to call
    ProcAddressCMethodBindingEmitter res = new ProcAddressCMethodBindingEmitter(baseCEmitter, callThroughProcAddress, this);
    MessageFormat exp = baseCEmitter.getReturnValueCapacityExpression();
    if (exp != null) {
      res.setReturnValueCapacityExpression(exp);
    }
    emitters.add(res);
  }
    
  protected boolean needsProcAddressWrapper(FunctionSymbol sym)
  {
    String symName =  sym.getName();

    ProcAddressConfiguration config = getProcAddressConfig();

    // We should only generate code to call through a function pointer
    // if the symbol has an associated function pointer typedef.
    String funcPointerTypedefName = getFunctionPointerTypedefName(sym);
    boolean shouldWrap = typedefDictionary.containsKey(funcPointerTypedefName);
    //System.err.println(funcPointerTypedefName + " defined: " + shouldWrap);      

    if (config.skipProcAddressGen(symName)) {
      shouldWrap = false;
    }
    
    if (shouldWrap)
    {
      // Hoist argument names from function pointer if not supplied in prototype
      FunctionType typedef = typedefDictionary.get(funcPointerTypedefName).asPointer().getTargetType().asFunction();
      FunctionType fun = sym.getType();
      int numarg = typedef.getNumArguments();
      for ( int i =0; i < numarg; i++ )
      {
        if ( fun.getArgumentName(i) == null )
          fun.setArgumentName(i,typedef.getArgumentName(i));
      }
    }
    
    return shouldWrap;
  }

  private void beginProcAddressTable() throws Exception
  {
    tableClassPackage = getProcAddressConfig().tableClassPackage();
    tableClassName    = getProcAddressConfig().tableClassName();

    // Table defaults to going into the impl directory unless otherwise overridden
    String implPackageName = tableClassPackage;
    if (implPackageName == null) {
      implPackageName = getImplPackageName();
    }
    String jImplRoot =
      getJavaOutputDir() + File.separator +
      CodeGenUtils.packageAsPath(implPackageName);

    tableWriter = openFile(jImplRoot + File.separator + tableClassName + ".java");

    CodeGenUtils.emitAutogeneratedWarning(tableWriter, this);
    
    tableWriter.println("package " + implPackageName + ";");
    tableWriter.println();
    for (Iterator iter = getConfig().imports().iterator(); iter.hasNext(); ) {
      tableWriter.println("import " + ((String) iter.next()) + ";");
    }
    tableWriter.println();

    tableWriter.println("/**");
    tableWriter.println(" * This table is a cache of pointers to the dynamically-linkable C");
    tableWriter.println(" * functions this autogenerated Java binding has exposed. Some");
    tableWriter.println(" * libraries such as OpenGL, OpenAL and others define function pointer");
    tableWriter.println(" * signatures rather than statically linkable entry points for the");
    tableWriter.println(" * purposes of being able to query at run-time whether a particular");
    tableWriter.println(" * extension is available. This table acts as a cache of these");
    tableWriter.println(" * function pointers. Each function pointer is typically looked up at");
    tableWriter.println(" * run-time by a platform-dependent mechanism such as dlsym(),");
    tableWriter.println(" * wgl/glXGetProcAddress(), or alGetProcAddress(). The associated");
    tableWriter.println(" * autogenerated Java and C code accesses the fields in this table to");
    tableWriter.println(" * call the various functions. If the field containing the function");
    tableWriter.println(" * pointer is 0, the function is considered to be unavailable and can");
    tableWriter.println(" * not be called.");
    tableWriter.println(" */");
    tableWriter.println("public class " + tableClassName);
    tableWriter.println("{");

    for (Iterator iter = getProcAddressConfig().getForceProcAddressGen().iterator(); iter.hasNext(); ) {
      emitProcAddressTableEntryForString((String) iter.next());
    }
  }

  private void endProcAddressTable() throws Exception
  {
    PrintWriter w = tableWriter;

    w.println("  /**");
    w.println("   * This is a convenience method to get (by name) the native function");
    w.println("   * pointer for a given function. It lets you avoid having to");
    w.println("   * manually compute the &quot;" + PROCADDRESS_VAR_PREFIX + " + ");
    w.println("   * &lt;functionName&gt;&quot; member variable name and look it up via");
    w.println("   * reflection; it also will throw an exception if you try to get the");
    w.println("   * address of an unknown function, or one that is statically linked");
    w.println("   * and therefore does not have a function pointer in this table.");
    w.println("   *");
    w.println("   * @throws RuntimeException if the function pointer was not found in");
    w.println("   *   this table, either because the function was unknown or because");
    w.println("   *   it was statically linked.");
    w.println("   */");
    w.println("  public long getAddressFor(String functionName) {");
    w.println("    String addressFieldName = com.sun.gluegen.runtime.ProcAddressHelper.PROCADDRESS_VAR_PREFIX + functionName;");
    w.println("    try { ");
    w.println("      java.lang.reflect.Field addressField = getClass().getField(addressFieldName);");
    w.println("      return addressField.getLong(this);");
    w.println("    } catch (Exception e) {");
    w.println("      // The user is calling a bogus function or one which is not");
    w.println("      // runtime linked");
    w.println("      throw new RuntimeException(");
    w.println("          \"WARNING: Address query failed for \\\"\" + functionName +");
    w.println("          \"\\\"; it's either statically linked or is not a known \" +");
    w.println("          \"function\", e);");
    w.println("    } ");
    w.println("  }");

    w.println("} // end of class " + tableClassName);
    w.flush();
    w.close();
  }

  protected void emitProcAddressTableEntryForSymbol(FunctionSymbol cFunc)
  {
    emitProcAddressTableEntryForString(cFunc.getName());
  }

  protected void emitProcAddressTableEntryForString(String str)
  {
    tableWriter.print("  public long ");
    tableWriter.print(PROCADDRESS_VAR_PREFIX);
    tableWriter.print(str);
    tableWriter.println(";");
  }

  protected ProcAddressConfiguration getProcAddressConfig() {
    return (ProcAddressConfiguration) getConfig();
  }
}

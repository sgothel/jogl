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

package net.java.games.gluegen.opengl;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import net.java.games.gluegen.*;
import net.java.games.gluegen.cgram.types.*;
import net.java.games.gluegen.runtime.*;

/**
 * A subclass of JavaEmitter that modifies the normal emission of C and Java
 * code in order to allow a high-performance, cross-platform binding of Java
 * to OpenGL.
 */
public class GLEmitter extends JavaEmitter
{
  public static final String PROCADDRESS_VAR_PREFIX = ProcAddressHelper.PROCADDRESS_VAR_PREFIX;
  protected static final String WRAP_PREFIX = "dispatch_";
  private TypeDictionary typedefDictionary;
  private PrintWriter tableWriter;
  private String tableClassPackage;
  private String tableClassName;
  private int numProcAddressEntries;
  
  public void beginFunctions(TypeDictionary typedefDictionary,
                             TypeDictionary structDictionary,
                             Map            canonMap) throws Exception
  {
    this.typedefDictionary = typedefDictionary;

    if (getGLConfig().emitProcAddressTable())
    {
      beginGLProcAddressTable();
    }
    super.beginFunctions(typedefDictionary, structDictionary, canonMap);
  }

  public void endFunctions() throws Exception
  {
    if (getGLConfig().emitProcAddressTable())
    {
      endGLProcAddressTable();
    }
    super.endFunctions();
  }
  
  public void beginStructs(TypeDictionary typedefDictionary,
                           TypeDictionary structDictionary,
                           Map            canonMap) throws Exception {
    super.beginStructs(typedefDictionary, structDictionary, canonMap);
  }

  protected JavaConfiguration createConfig() {
    return new GLConfiguration();
  }

  protected List generateMethodBindingEmitters(FunctionSymbol sym) throws Exception
  {
    return generateMethodBindingEmittersImpl(sym);
  }

  protected List generateMethodBindingEmitters(FunctionSymbol sym, boolean skipProcessing) throws Exception {
    if (skipProcessing) {
      return super.generateMethodBindingEmitters(sym);
    } else {
      return generateMethodBindingEmittersImpl(sym);
    }
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
    
    // Don't do anything special if this symbol doesn't require passing of
    // Opengl procedure addresses in order to function correctly.
    if (!needsProcAddressWrapper(sym) || getConfig().isUnimplemented(sym.getName()))
    {
      return defaultEmitters;
    }

    // 9 is default # expanded bindings for void*
    ArrayList modifiedEmitters = new ArrayList(9); 

    if (getGLConfig().emitProcAddressTable())
    {
      // emit an entry in the GL proc address table for this method.
      emitGLProcAddressTableEntryForSymbol(sym);
    }
    
    for (Iterator iter = defaultEmitters.iterator(); iter.hasNext(); )
    {
      FunctionEmitter emitter = (FunctionEmitter) iter.next();
      if (emitter instanceof JavaMethodBindingEmitter)
      {
        JavaMethodBindingEmitter newEmitter =
          generateModifiedEmitter((JavaMethodBindingEmitter)emitter);
        if (newEmitter != null) {
          modifiedEmitters.add(newEmitter);
        }
      }
      else if (emitter instanceof CMethodBindingEmitter)
      {
        modifiedEmitters.add(
          generateModifiedEmitter((CMethodBindingEmitter)emitter));
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
   * Returns the name of the typedef for a pointer to the GL function
   * represented by the argument. For example, if the argument is the function
   * "glFuncName", the value returned will be "PFNGLFUNCNAMEPROC". This
   * returns a valid string regardless of whether or not the typedef is
   * actually defined.
   */
  static String getGLFunctionPointerTypedefName(FunctionSymbol sym)
  {
    String symName = sym.getName();
    StringBuffer buf = new StringBuffer(symName.length() + 8);
    buf.append("PFN");
    buf.append(symName.toUpperCase());
    buf.append("PROC");
    return buf.toString();
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //
  
  protected JavaMethodBindingEmitter generateModifiedEmitter(JavaMethodBindingEmitter baseJavaEmitter)
  {    
    if (!(baseJavaEmitter instanceof JavaMethodBindingImplEmitter)) {
      // We only want to wrap the native entry point in the implementation
      // class, not the public interface in the interface class.
      //
      // If the superclass has generated a "0" emitter for this routine because
      // it needs argument conversion or similar, filter that out since we will
      // be providing such an emitter ourselves. Otherwise return the emitter
      // unmodified.
      if (baseJavaEmitter.isForImplementingMethodCall())
        return null;
      return baseJavaEmitter;
    }
    if (getGLConfig().manuallyImplement(baseJavaEmitter.getName())) {
      // User will provide Java-side implementation of this routine
      return null;
    }
    return new JavaGLPAWrapperEmitter(baseJavaEmitter, getGLConfig().getProcAddressTableExpr());
  }

  protected CMethodBindingEmitter generateModifiedEmitter(CMethodBindingEmitter baseCEmitter)
  {    
    // The C-side JNI binding for this particular function will have an
    // extra final argument, which is the address (the OpenGL procedure
    // address) of the function it needs to call
    CGLPAWrapperEmitter res = new CGLPAWrapperEmitter(baseCEmitter);
    MessageFormat exp = baseCEmitter.getReturnValueCapacityExpression();
    if (exp != null) {
      res.setReturnValueCapacityExpression(exp);
    }
    return res;
  }
    
  protected boolean needsProcAddressWrapper(FunctionSymbol sym)
  {
    String symName =  sym.getName();

    GLConfiguration config = getGLConfig();

    // We should only wrap the GL symbol if its function pointer typedef has
    // been defined (most likely in glext.h).
    String funcPointerTypedefName = getGLFunctionPointerTypedefName(sym);
    boolean shouldWrap = typedefDictionary.containsKey(funcPointerTypedefName);
    //System.err.println(funcPointerTypedefName + " defined: " + shouldWrap);      

    if (config.skipProcAddressGen(symName)) {
      shouldWrap = false;
    }
    
    if (!shouldWrap)
    {
      //System.err.println("WARNING (GL): *not* run-time linking: " + sym +
      //                   "(" + funcPointerTypedefName + " undefined)");
    }
    else 
    {
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
  
  private void beginGLProcAddressTable() throws Exception
  {
    tableClassPackage = getGLConfig().tableClassPackage();
    tableClassName    = getGLConfig().tableClassName();

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
    tableWriter.println(" * This table is a cache of the native pointers to OpenGL extension");
    tableWriter.println(" * functions, to be used for run-time linking of these extensions. ");
    tableWriter.println(" * These pointers are obtained by the OpenGL context via a ");
    tableWriter.println(" * platform-specific function (e.g., wglGetProcAddress() on Win32,");
    tableWriter.println(" * glXGetProcAddress() on X11, etc).  If the member variable ");
    tableWriter.println(" * " + PROCADDRESS_VAR_PREFIX + "glFuncName is non-zero then function");
    tableWriter.println(" * \"glFuncName\" can be called through the associated GLContext; ");
    tableWriter.println(" * if it is 0, then the extension is not available and cannot be called.");
    tableWriter.println(" */");
    tableWriter.println("public class " + tableClassName);
    tableWriter.println("{");
    numProcAddressEntries = 0;

    for (Iterator iter = getGLConfig().getForceProcAddressGen().iterator(); iter.hasNext(); ) {
      emitGLProcAddressTableEntryForString((String) iter.next());
    }
  }

  private void endGLProcAddressTable() throws Exception
  {
    PrintWriter w = tableWriter;

    w.println();
    w.println("  /**");
    w.println("   * This is a convenience method to get (by name) the native function ");
    w.println("   * pointer for a given extension function. It lets you avoid ");
    w.println("   * having to manually compute the " + PROCADDRESS_VAR_PREFIX + "<glFunctionName>");
    w.println("   * member variable name and look it up via reflection; it also");
    w.println("   * will throw an exception if you try to get the address of an");
    w.println("   * unknown GL extension, or one that is statically linked ");
    w.println("   * and therefore does not have a valid GL procedure address. ");
    w.println("   */");
    w.println("  public long getAddressFor(String glFunctionName) {");   
    w.println("   String addressFieldName = net.java.games.gluegen.opengl.GLEmitter.PROCADDRESS_VAR_PREFIX + glFunctionName;");
    w.println("   try { ");
    w.println("      java.lang.reflect.Field addressField = this.getClass().getField(addressFieldName);");
    w.println("      return addressField.getLong(this);");
    w.println("    } catch (Exception e) {");
    w.println("      // The user is calling a bogus function or one which is not runtime");
    w.println("      // linked (extensions and core post-OpenGL 1.1 functions are runtime linked)");
    w.println("      if (!FunctionAvailabilityCache.isPartOfGLCore(\"1.1\", glFunctionName)) ");
    w.println("      {");
    w.println("        throw new RuntimeException("  );
    w.println("          \"WARNING: Address query failed for \\\"\" + glFunctionName +");
    w.println("          \"\\\"; either it's not runtime linked or it is not a known \" +");
    w.println("          \"OpenGL function\", e);");
    w.println("      }");
    w.println("   } ");
    w.println("   assert(false); // should never get this far");
    w.println("   return 0;");
    w.println("   }");

    w.println("} // end of class " + tableClassName);
    w.flush();
    w.close();
  }

  protected void emitGLProcAddressTableEntryForSymbol(FunctionSymbol cFunc)
  {
    emitGLProcAddressTableEntryForString(cFunc.getName());
  }

  protected void emitGLProcAddressTableEntryForString(String str)
  {
    tableWriter.print("  public long ");
    tableWriter.print(PROCADDRESS_VAR_PREFIX);
    tableWriter.print(str);
    tableWriter.println(";");
    ++numProcAddressEntries;    
  }

  protected GLConfiguration getGLConfig() {
    return (GLConfiguration) getConfig();
  }

  protected static class GLConfiguration extends JavaConfiguration
  {
    private boolean emitProcAddressTable = false;
    private String  tableClassPackage;
    private String  tableClassName = "ProcAddressTable";
    private Set/*<String>*/ skipProcAddressGen  = new HashSet();
    private List/*<String>*/ forceProcAddressGen  = new ArrayList();
    private String  contextVariableName = "context";
    private String  defaultGetProcAddressTableExpr = ".getGLProcAddressTable()";
    private String  getProcAddressTableExpr;

    protected void dispatch(String cmd, StringTokenizer tok, File file, String filename, int lineNo) throws IOException {
      if (cmd.equalsIgnoreCase("EmitProcAddressTable"))
      {
        emitProcAddressTable =
          readBoolean("EmitProcAddressTable", tok, filename, lineNo).booleanValue();
      }
      else if (cmd.equalsIgnoreCase("ProcAddressTablePackage"))
      {
        tableClassPackage = readString("ProcAddressTablePackage", tok, filename, lineNo);
      }
      else if (cmd.equalsIgnoreCase("ProcAddressTableClassName"))
      {
        tableClassName = readString("ProcAddressTableClassName", tok, filename, lineNo);
      }
      else if (cmd.equalsIgnoreCase("SkipProcAddressGen"))
      {
        String sym = readString("SkipProcAddressGen", tok, filename, lineNo);
        skipProcAddressGen.add(sym);
      }
      else if (cmd.equalsIgnoreCase("ForceProcAddressGen"))
      {
        String sym = readString("ForceProcAddressGen", tok, filename, lineNo);
        forceProcAddressGen.add(sym);
      }
      else if (cmd.equalsIgnoreCase("ContextVariableName"))
      {
        contextVariableName = readString("ContextVariableName", tok, filename, lineNo);
      }
      else if (cmd.equalsIgnoreCase("GetProcAddressTableExpr"))
      {
        getProcAddressTableExpr = readGetProcAddressTableExpr(tok, filename, lineNo);
      }
      else
      {
        super.dispatch(cmd,tok,file,filename,lineNo);
      }
    }

    protected String readGetProcAddressTableExpr(StringTokenizer tok, String filename, int lineNo) {
      try {
        String restOfLine = tok.nextToken("\n\r\f");
        return restOfLine.trim();
      } catch (NoSuchElementException e) {
        throw new RuntimeException("Error parsing \"GetProcAddressTableExpr\" command at line " + lineNo +
                                   " in file \"" + filename + "\"", e);
      }
    }

    public boolean emitProcAddressTable()           { return emitProcAddressTable;               }
    public String  tableClassPackage()              { return tableClassPackage;                  }
    public String  tableClassName()                 { return tableClassName;                     }
    public boolean skipProcAddressGen (String name) { return skipProcAddressGen.contains(name);  }
    public List    getForceProcAddressGen()         { return forceProcAddressGen;                }
    public String  contextVariableName()            { return contextVariableName;                }
    public String  getProcAddressTableExpr() {
      if (getProcAddressTableExpr == null) {
        getProcAddressTableExpr = contextVariableName + defaultGetProcAddressTableExpr;
      }
      return getProcAddressTableExpr;
    }
  } // end class GLConfiguration
}
  

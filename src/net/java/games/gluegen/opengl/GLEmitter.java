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

/**
 * A subclass of JavaEmitter that modifies the normal emission of C and Java
 * code in order to allow a high-performance, cross-platform binding of Java
 * to OpenGL.
 */
public class GLEmitter extends JavaEmitter
{
  public static final String PROCADDRESS_VAR_PREFIX = "_addressof_";
  protected static final String WRAP_PREFIX = "dispatch_";
  private TypeDictionary typedefDictionary;
  private PrintWriter tableWriter;
  private String tableClassName = "ProcAddressTable";
  private int numProcAddressEntries;
  
  public void beginFunctions(TypeDictionary typedefDictionary,
                             TypeDictionary structDictionary,
                             Map            canonMap) throws Exception
  {
    this.typedefDictionary = typedefDictionary;

    if (getConfig().emitImpl()) {
      cWriter().println("#include <assert.h> /* this include emitted by GLEmitter.java */"); 
      cWriter().println();
    }

    if (((GLConfiguration)getConfig()).emitProcAddressTable())
    {
      beginGLProcAddressTable();
    }
    super.beginFunctions(typedefDictionary, structDictionary, canonMap);
  }

  public void endFunctions() throws Exception
  {
    if (((GLConfiguration)getConfig()).emitProcAddressTable())
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

  protected Iterator generateMethodBindingEmitters(FunctionSymbol sym) throws Exception
  {
    Iterator defaultEmitters = super.generateMethodBindingEmitters(sym);

    // if the superclass didn't generate any bindings for the symbol, let's
    // honor that (for example, the superclass might have caught an Ignore
    // direction that matched the symbol's name).
    if (!defaultEmitters.hasNext())
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

    if (((GLConfiguration)getConfig()).emitProcAddressTable())
    {
      // emit an entry in the GL proc address table for this method.
      emitGLProcAddressTableEntryForSymbol(sym);
    }
    
    while (defaultEmitters.hasNext())
    {
      FunctionEmitter emitter = (FunctionEmitter)defaultEmitters.next();
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
    
    return modifiedEmitters.iterator();
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
  
  private JavaMethodBindingEmitter generateModifiedEmitter(JavaMethodBindingEmitter baseJavaEmitter)
  {    
    if (!(baseJavaEmitter instanceof JavaMethodBindingImplEmitter)) {
      // We only want to wrap the native entry point in the implementation
      // class, not the public interface in the interface class.
      //
      // If the superclass has generated a "0" emitter for this routine because
      // it needs argument conversion or similar, filter that out since we will
      // be providing such an emitter ourselves. Otherwise return the emitter
      // unmodified.
      if (baseJavaEmitter.isForNIOBufferBaseRoutine())
        return null;
      return baseJavaEmitter;
    }
    return new JavaGLPAWrapperEmitter(baseJavaEmitter);
  }

  private CMethodBindingEmitter generateModifiedEmitter(CMethodBindingEmitter baseCEmitter)
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
    
  private boolean needsProcAddressWrapper(FunctionSymbol sym)
  {
    String symName =  sym.getName();

    // We should only wrap the GL symbol if its function pointer typedef has
    // been defined (most likely in glext.h).
    String funcPointerTypedefName = getGLFunctionPointerTypedefName(sym);
    boolean shouldWrap = typedefDictionary.containsKey(funcPointerTypedefName);
    //System.err.println(funcPointerTypedefName + " defined: " + shouldWrap);      
    
    if (!shouldWrap)
    {
      //System.err.println("WARNING (GL): *not* run-time linking: " + sym +
      //                   "(" + funcPointerTypedefName + " undefined)");
    }
    return shouldWrap;
  }
  
  private void beginGLProcAddressTable() throws Exception
  {
    String implPackageName = getImplPackageName();
    String jImplRoot =
      getJavaOutputDir() + File.separator +
      CodeGenUtils.packageAsPath(implPackageName);

    // HACK: until we have a way to make the impl dir different from the
    // WindowsGLImpl dir and the interface dir
    //tableWriter = openFile(jImplRoot + File.separator + tableClassName + ".java");
    File tmpFile = new File(jImplRoot);
    tmpFile = tmpFile.getParentFile();
    tmpFile = new File(tmpFile, tableClassName + ".java");
    tableWriter = openFile(tmpFile.getPath());
    // tableWriter = openFile(jImplRoot + File.separator + ".." + File.separator + tableClassName + ".java");

    CodeGenUtils.emitAutogeneratedWarning(tableWriter, this);
    
    // HACK: until we have a way to make the impl dir different from the
    // WindowsGLImpl dir and the interface dir
    //tableWriter.println("package " + implPackageName + ";");
    tableWriter.println("package " + getJavaPackageName() + ".impl;");
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
  }

  private void endGLProcAddressTable() throws Exception
  {
    PrintWriter w = tableWriter;
    w.print("  protected static long __PROCADDRESSINDEX__LASTINDEX = ");
    w.print(numProcAddressEntries-1);
    w.println(';');

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

  private void emitGLProcAddressTableEntryForSymbol(FunctionSymbol cFunc)
  {
    tableWriter.print("  public static long ");
    tableWriter.print(PROCADDRESS_VAR_PREFIX);
    tableWriter.print(cFunc.getName());
    tableWriter.println(";");
    ++numProcAddressEntries;    
  }

  protected static class GLConfiguration extends JavaConfiguration
  {
    private boolean emitProcAddressTable = false;
    
    protected void dispatch(String cmd, StringTokenizer tok, File file, String filename, int lineNo) throws IOException {
      if (cmd.equalsIgnoreCase("EmitProcAddressTable"))
      {
        emitProcAddressTable =
          readBoolean("EmitProcAddressTable", tok, filename, lineNo).booleanValue();
      }
      else
      {
        super.dispatch(cmd,tok,file,filename,lineNo);
      }
    }

    public boolean emitProcAddressTable() { return emitProcAddressTable; }
  } // end class GLConfiguration
}
  

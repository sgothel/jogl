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

package com.sun.gluegen.opengl;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import com.sun.gluegen.*;
import com.sun.gluegen.cgram.types.*;
import com.sun.gluegen.runtime.*;

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
  // Keeps track of which MethodBindings were created for handling
  // Buffer Object variants. Used as a Set rather than a Map.
  private Map/*<MethodBinding>*/ bufferObjectMethodBindings = new IdentityHashMap();

  static class BufferObjectKind {
    private BufferObjectKind() {}

    static final BufferObjectKind UNPACK_PIXEL = new BufferObjectKind();
    static final BufferObjectKind PACK_PIXEL   = new BufferObjectKind();
    static final BufferObjectKind ARRAY        = new BufferObjectKind();
    static final BufferObjectKind ELEMENT      = new BufferObjectKind();
  }
  
  public void beginEmission(GlueEmitterControls controls) throws IOException
  {
    getGLConfig().parseGLHeaders(controls);
    super.beginEmission(controls);
  }

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

  /** In order to implement Buffer Object variants of certain
      functions we generate another MethodBinding which maps the void*
      argument to a Java long. The generation of emitters then takes
      place as usual. We do however need to keep track of the modified
      MethodBinding object so that we can also modify the emitters
      later to inform them that their argument has changed. We might
      want to push this functionality down into the MethodBinding
      (i.e., mutators for argument names). We also would need to
      inform the CMethodBindingEmitter that it is overloaded in this
      case (though we default to true currently). */
  protected List/*<MethodBinding>*/ expandMethodBinding(MethodBinding binding) {
    List/*<MethodBinding>*/ bindings = super.expandMethodBinding(binding);
    
    if (!getGLConfig().isBufferObjectFunction(binding.getName())) {
      return bindings;
    }

    List/*<MethodBinding>*/ newBindings = new ArrayList();
    newBindings.addAll(bindings);

    // Need to expand each one of the generated bindings to take a
    // Java long instead of a Buffer for each void* argument
    for (Iterator iter = bindings.iterator(); iter.hasNext(); ) {
      MethodBinding cur = (MethodBinding) iter.next();
      
      // Some of these routines (glBitmap) take strongly-typed
      // primitive pointers as arguments which are expanded into
      // non-void* arguments
      // This test (rather than !signatureUsesNIO) is used to catch
      // more unexpected situations
      if (cur.signatureUsesJavaPrimitiveArrays()) {
        continue;
      }

      MethodBinding result = cur;
      for (int i = 0; i < cur.getNumArguments(); i++) {
        if (cur.getJavaArgumentType(i).isNIOBuffer()) {
          result = result.replaceJavaArgumentType(i, JavaType.createForClass(Long.TYPE));
        }
      }

      if (result == cur) {
        throw new RuntimeException("Error: didn't find any void* arguments for BufferObject function " +
                                   binding.getName());
      }

      newBindings.add(result);
      // Now need to flag this MethodBinding so that we generate the
      // correct flags in the emitters later
      bufferObjectMethodBindings.put(result, result);
    }

    return newBindings;
  }

  protected List generateMethodBindingEmitters(FunctionSymbol sym) throws Exception
  {
    return generateMethodBindingEmittersImpl(sym);
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
    // OpenGL-related modifications
    if ((!needsProcAddressWrapper(sym) && !needsBufferObjectVariant(sym)) ||
        getConfig().isUnimplemented(sym.getName()))
    {
      return defaultEmitters;
    }

    ArrayList modifiedEmitters = new ArrayList(defaultEmitters.size());

    if (needsProcAddressWrapper(sym)) {
      if (getGLConfig().emitProcAddressTable()) {
        // emit an entry in the GL proc address table for this method.
        emitGLProcAddressTableEntryForSymbol(sym);
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
  
  protected void generateModifiedEmitters(JavaMethodBindingEmitter baseJavaEmitter, List emitters) {
    if (getGLConfig().manuallyImplement(baseJavaEmitter.getName())) {
      // User will provide Java-side implementation of this routine;
      // pass through any emitters which will produce signatures for
      // it unmodified
      emitters.add(baseJavaEmitter);
      return;
    }
    
    // See whether we need a proc address entry for this one
    boolean callThroughProcAddress = needsProcAddressWrapper(baseJavaEmitter.getBinding().getCSymbol());
    // See whether this is one of the Buffer Object variants
    boolean bufferObjectVariant = bufferObjectMethodBindings.containsKey(baseJavaEmitter.getBinding());

    GLJavaMethodBindingEmitter emitter =
      new GLJavaMethodBindingEmitter(baseJavaEmitter,
                                     callThroughProcAddress,
                                     getGLConfig().getProcAddressTableExpr(),
                                     baseJavaEmitter.isForImplementingMethodCall(),
                                     bufferObjectVariant);
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
      emitter = new GLJavaMethodBindingEmitter(baseJavaEmitter,
                                               callThroughProcAddress,
                                               getGLConfig().getProcAddressTableExpr(),
                                               true,
                                               bufferObjectVariant);
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
    GLCMethodBindingEmitter res = new GLCMethodBindingEmitter(baseCEmitter, callThroughProcAddress);
    MessageFormat exp = baseCEmitter.getReturnValueCapacityExpression();
    if (exp != null) {
      res.setReturnValueCapacityExpression(exp);
    }
    emitters.add(res);
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

  protected boolean needsBufferObjectVariant(FunctionSymbol sym) {
    return getGLConfig().isBufferObjectFunction(sym.getName());
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
    w.println("   String addressFieldName = com.sun.gluegen.opengl.GLEmitter.PROCADDRESS_VAR_PREFIX + glFunctionName;");
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

  protected class GLConfiguration extends JavaConfiguration
  {
    private boolean emitProcAddressTable = false;
    private String  tableClassPackage;
    private String  tableClassName = "ProcAddressTable";
    private Set/*<String>*/ skipProcAddressGen  = new HashSet();
    private List/*<String>*/ forceProcAddressGen  = new ArrayList();
    private String  contextVariableName = "context";
    private String  getProcAddressTableExpr;
    // The following data members support ignoring an entire extension at a time
    private List/*<String>*/ glHeaders = new ArrayList();
    private Set/*<String>*/ ignoredExtensions = new HashSet();
    private BuildStaticGLInfo glInfo;
    // Maps function names to the kind of buffer object it deals with
    private Map/*<String,BufferObjectKind>*/ bufferObjectKinds = new HashMap();

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
      else if (cmd.equalsIgnoreCase("IgnoreExtension"))
      {
        String sym = readString("IgnoreExtension", tok, filename, lineNo);
        ignoredExtensions.add(sym);
      }
      else if (cmd.equalsIgnoreCase("GLHeader"))
      {
        String sym = readString("GLHeader", tok, filename, lineNo);
        glHeaders.add(sym);
      }
      else if (cmd.equalsIgnoreCase("BufferObjectKind"))
      {
        readBufferObjectKind(tok, filename, lineNo);
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

    protected void readBufferObjectKind(StringTokenizer tok, String filename, int lineNo) {
      try {
        String kindString = tok.nextToken();
        BufferObjectKind kind = null;
        String target = tok.nextToken();
        if (kindString.equalsIgnoreCase("UnpackPixel")) {
          kind = BufferObjectKind.UNPACK_PIXEL;
        } else if (kindString.equalsIgnoreCase("PackPixel")) {
          kind = BufferObjectKind.PACK_PIXEL;
        } else if (kindString.equalsIgnoreCase("Array")) {
          kind = BufferObjectKind.ARRAY;
        } else if (kindString.equalsIgnoreCase("Element")) {
          kind = BufferObjectKind.ELEMENT;
        } else {
          throw new RuntimeException("Error parsing \"BufferObjectKind\" command at line " + lineNo +
                                     " in file \"" + filename + "\": illegal BufferObjectKind \"" +
                                     kindString + "\", expected one of UnpackPixel, PackPixel, Array, or Element");
        }

        bufferObjectKinds.put(target, kind);
      } catch (NoSuchElementException e) {
        throw new RuntimeException("Error parsing \"BufferObjectKind\" command at line " + lineNo +
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
        getProcAddressTableExpr = contextVariableName + ".get" + tableClassName + "()";
      }
      return getProcAddressTableExpr;
    }

    public boolean shouldIgnore(String symbol) {
      // Check ignored extensions based on our knowledge of the static GL info
      if (glInfo != null) {
        String extension = glInfo.getExtension(symbol);
        if (extension != null &&
            ignoredExtensions.contains(extension)) {
          return true;
        }
      }

      return super.shouldIgnore(symbol);
    }

    /** Overrides javaPrologueForMethod in superclass and
        automatically generates prologue code for functions associated
        with buffer objects. */
    public List/*<String>*/ javaPrologueForMethod(MethodBinding binding,
                                                  boolean forImplementingMethodCall,
                                                  boolean eraseBufferAndArrayTypes) {
      List/*<String>*/ res = super.javaPrologueForMethod(binding,
                                                         forImplementingMethodCall,
                                                         eraseBufferAndArrayTypes);
      BufferObjectKind kind = getBufferObjectKind(binding.getName());
      if (kind != null) {
        // Need to generate appropriate prologue based on both buffer
        // object kind and whether this variant of the MethodBinding
        // is the one accepting a "long" as argument
        if (res == null) {
          res = new ArrayList();
        }

        String prologue = "check";

        if (kind == BufferObjectKind.UNPACK_PIXEL) {
          prologue = prologue + "UnpackPBO";
        } else if (kind == BufferObjectKind.PACK_PIXEL) {
          prologue = prologue + "PackPBO";
        } else if (kind == BufferObjectKind.ARRAY) {
          prologue = prologue + "ArrayVBO";
        } else if (kind == BufferObjectKind.ELEMENT) {
          prologue = prologue + "ElementVBO";
        } else {
          throw new RuntimeException("Unknown BufferObjectKind " + kind);
        }

        if (bufferObjectMethodBindings.containsKey(binding)) {
          prologue = prologue + "Enabled";
        } else {
          prologue = prologue + "Disabled";
        }

        prologue = prologue + "();";

        res.add(0, prologue);
      }

      return res;
    }

    /** Returns the kind of buffer object this function deals with, or
        null if none. */
    public BufferObjectKind getBufferObjectKind(String name) {
      return (BufferObjectKind) bufferObjectKinds.get(name);
    }

    public boolean isBufferObjectFunction(String name) {
      return (getBufferObjectKind(name) != null);
    }

    /** Parses any GL headers specified in the configuration file for
        the purpose of being able to ignore an extension at a time. */
    public void parseGLHeaders(GlueEmitterControls controls) throws IOException {
      if (!glHeaders.isEmpty()) {
        glInfo = new BuildStaticGLInfo();
        for (Iterator iter = glHeaders.iterator(); iter.hasNext(); ) {
          String file = (String) iter.next();
          String fullPath = controls.findHeaderFile(file);
          if (fullPath == null) {
            throw new IOException("Unable to locate header file \"" + file + "\"");
          }
          glInfo.parse(fullPath);
        }
      }
    }
  } // end class GLConfiguration
}

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
import java.util.regex.*;

import net.java.games.gluegen.cgram.types.*;

/** Parses and provides access to the contents of .cfg files for the
    JavaEmitter. */

public class JavaConfiguration {
  private int nestedReads;
  private String packageName;
  private String implPackageName;
  private String className;
  private String implClassName;
  /**
   * Root directory for the hierarchy of generated java classes. Default is
   * working directory.
   */
  private String javaOutputDir = ".";
  /**
   * Directory into which generated native JNI code will be written. Default
   * is current working directory.
   */
  private String nativeOutputDir = ".";
  /**
   * If true, then each native *.c and *.h file will be generated in the
   * directory nativeOutputDir/packageAsPath(packageName). Default is false.
   */
  private boolean nativeOutputUsesJavaHierarchy;
  /**
   * Style of code emission. Can emit everything into one class
   * (AllStatic), separate interface and implementing classes
   * (InterfaceAndImpl), only the interface (InterfaceOnly), or only
   * the implementation (ImplOnly).
   */
  private int emissionStyle = JavaEmitter.ALL_STATIC;
  /**
   * List of imports to emit at the head of the output files.
   */
  private List/*<String>*/ imports = new ArrayList();
  /**
   * The kind of exception raised by the generated code if run-time
   * checks fail. Defaults to RuntimeException.
   */
  private String runtimeExceptionType = "RuntimeException";
  private Map/*<String,TypeInfo>*/ typeInfoMap = new HashMap();
  private Set/*<String>*/ returnsString = new HashSet();
  private Map/*<String, String>*/ returnedArrayLengths = new HashMap();
  /**
   * Key is function that has some byte[] arguments that should be
   * converted to String args; value is List of Integer argument indices
   */
  private Map/*<String,List<Integer>>*/ argumentsAreString = new HashMap();
  private Set/*<Pattern>*/ ignores = new HashSet();
  private Set/*<Pattern>*/ ignoreNots = new HashSet();
  private Set/*<Pattern>*/ unimplemented = new HashSet();
  private Set/*<String>*/ nioOnly = new HashSet();
  /** See {@link #nioMode} */
  public static final int NIO_MODE_VOID_ONLY = 1;
  /** See {@link #nioMode} */
  public static final int NIO_MODE_ALL_POINTERS = 2;
  private int nioMode = NIO_MODE_VOID_ONLY;
  private Set/*<String>*/ noNio = new HashSet();
  private Set/*<String>*/ forcedNio = new HashSet();
  private boolean flattenNIOVariants = true;
  private Set/*<String>*/ manuallyImplement = new HashSet();
  private Map/*<String,List<String>>*/ customJavaCode = new HashMap();
  private Map/*<String,List<String>>*/ classJavadoc = new HashMap();
  private Map/*<String,String>*/ structPackages = new HashMap();
  private List/*<String>*/ customCCode = new ArrayList();
  private List/*<String>*/ forcedStructs = new ArrayList();
  private Map/*<String,List<Integer>>*/ mirroredArgs = new HashMap(); 
  private Map/*<String, String>*/ returnValueCapacities = new HashMap(); 
  private Map/*<String, String>*/ returnValueLengths = new HashMap(); 
  private Map/*<String, List<String>>*/ temporaryCVariableDeclarations = new HashMap();
  private Map/*<String, List<String>>*/ temporaryCVariableAssignments = new HashMap();
  private Map/*<String,List<String>>*/ extendedInterfaces = new HashMap();
  private Map/*<String,List<String>>*/ implementedInterfaces = new HashMap();
  private Map/*<String,String>*/ javaTypeRenames = new HashMap();

  /** Reads the configuration file.
      @param filename path to file that should be read
  */
  public final void read(String filename) throws  IOException {
    read(filename, null);
  }
  
  /** Reads the specified file, treating each line as if it started with the
      specified string.
      @param filename path to file that should be read
      @param linePrefix if not null, treat each line read as if it were
      prefixed with the specified string.
  */
  protected final void read(String filename, String linePrefix) throws IOException {
    File file = new File(filename);
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(file));
    }
    catch (FileNotFoundException fnfe) {
      throw new RuntimeException("Could not read file \"" + file + "\"", fnfe);
    }
    int lineNo = 0;
    String line = null;
    boolean hasPrefix = linePrefix != null && linePrefix.length() > 0;
    try {
      ++nestedReads;
      while ((line = reader.readLine()) != null) {
        ++lineNo;
        if (hasPrefix)
        {
          line = linePrefix + " " + line;
        }

        if (line.trim().startsWith("#"))
        {
          // comment line
          continue;
        }
        
        StringTokenizer tok = new StringTokenizer(line);
        if (tok.hasMoreTokens())
        {
          // always reset delimiters in case of CustomJavaCode, etc.
          String cmd = tok.nextToken(" \t\n\r\f");

          dispatch(cmd, tok, file, filename, lineNo);
        }
      }
      reader.close();
    } finally {
      --nestedReads;
    }

    if (nestedReads == 0) {
      if (allStatic() && implClassName != null) {
        throw new IllegalStateException(
                                        "Error in configuration file \"" + filename + "\": Cannot use " +
                                        "directive \"ImplJavaClass\" in conjunction with " +
                                        "\"Style AllStatic\"");
      }

      if (className == null) {
        throw new RuntimeException("Output class name was not specified in configuration file");
      }
      if (packageName == null) {
        throw new RuntimeException("Output package name was not specified in configuration file");
      }

      if (allStatic()) {
        implClassName = className;
        // If we're using the "Style AllStatic" directive, then the
        // implPackageName is the same as the regular package name
        implPackageName = packageName;
      } else {
        if (implClassName == null) {
          // implClassName defaults to "<className>Impl" if ImplJavaClass
          // directive is not used
          implClassName = className + "Impl";
        }
        if (implPackageName == null) {
          // implPackageName defaults to "<packageName>.impl" if ImplPackage
          // directive is not used
          implPackageName = packageName + ".impl";
        }
      }
    }
  }

  /** Returns the package name parsed from the configuration file. */
  public String      packageName()                   { return packageName; }
  /** Returns the implementation package name parsed from the configuration file. */
  public String      implPackageName()               { return implPackageName; }
  /** Returns the class name parsed from the configuration file. */
  public String      className()                     { return className; }
  /** Returns the implementation class name parsed from the configuration file. */
  public String      implClassName()                 { return implClassName; }
  /** Returns the Java code output directory parsed from the configuration file. */
  public String      javaOutputDir()                 { return javaOutputDir; }
  /** Returns the native code output directory parsed from the configuration file. */
  public String      nativeOutputDir()               { return nativeOutputDir; }
  /** Returns whether the native code directory structure mirrors the Java hierarchy. */
  public boolean     nativeOutputUsesJavaHierarchy() { return nativeOutputUsesJavaHierarchy; }
  /** Returns the code emission style (constants in JavaEmitter) parsed from the configuration file. */
  public int         emissionStyle()                 { return emissionStyle; }
  /** Returns the kind of exception to raise if run-time checks fail in the generated code. */
  public String      runtimeExceptionType()          { return runtimeExceptionType; }
  /** Returns the list of imports that should be emitted at the top of each .java file. */
  public List/*<String>*/ imports()                  { return imports; }

  /** If this type should be considered opaque, returns the TypeInfo
      describing the replacement type. Returns null if this type
      should not be considered opaque. */
  public TypeInfo typeInfo(Type type, TypeDictionary typedefDictionary) {
    // Because typedefs of pointer types can show up at any point,
    // walk the pointer chain looking for a typedef name that is in
    // the TypeInfo map.
    int pointerDepth = type.pointerDepth();
    for (int i = 0; i <= pointerDepth; i++) {
      String name = type.getName();
      if (name != null) {
        TypeInfo info = (TypeInfo) typeInfoMap.get(name);
        while (info != null) {
          if (info.name().equals(name) && info.pointerDepth() == i) {
            return info;
          }
          info = info.next();
        }
      }

      if (type.isCompound()) {
        // Try struct name as well
        name = type.asCompound().getStructName();
        if (name != null) {
          TypeInfo info = (TypeInfo) typeInfoMap.get(name);
          while (info != null) {
            if (info.name().equals(name) && info.pointerDepth() == i) {
              return info;
            }
            info = info.next();
          }
        }
      }

      // Try all typedef names that map to this type
      Set entrySet = typedefDictionary.entrySet();
      for (Iterator iter = entrySet.iterator(); iter.hasNext(); ) {
        Map.Entry entry = (Map.Entry) iter.next();
        // "eq" equality is OK to use here since all types have been canonicalized
        if (entry.getValue() == type) {
          name = (String) entry.getKey();
          TypeInfo info = (TypeInfo) typeInfoMap.get(name);
          while (info != null) {
            if (info.name().equals(name) && info.pointerDepth() == i) {
              return info;
            }
            info = info.next();
          }
        }
      }

      if (type.isPointer()) {
        type = type.asPointer().getTargetType();
      }
    }

    return null;
  }

  /** Indicates whether the given function (which returns a
      <code>char*</code> in C) should be translated as returning a
      <code>java.lang.String</code>. */
  public boolean returnsString(String functionName) {
    return returnsString.contains(functionName);
  }

  /** Provides a Java MessageFormat expression indicating the number
      of elements in the returned array from the specified function
      name. Indicates that the given return value, which must be a
      pointer to a CompoundType, is actually an array of the
      CompoundType rather than a pointer to a single object. */
  public String returnedArrayLength(String functionName) {
    return (String) returnedArrayLengths.get(functionName);
  }

  /** Returns a list of <code>Integer</code>s which are the indices of <code>const char*</code>
      arguments that should be converted to <code>String</code>s. Returns null if there are no
      such hints for the given function name. */

  public List/*<Integer>*/ stringArguments(String functionName) {
    return (List) argumentsAreString.get(functionName);
  }

  /** Returns true if the given function should only create a java.nio
      variant, and no array variants, for <code>void*</code> and other
      C primitive pointers. */
  public boolean nioOnly(String functionName) {
    return nioOnly.contains(functionName);
  }

  /** Returns true if the user requested that the given function
      should only create array variants, and no java.nio variant, for
      <code>void*</code> and other C primitive pointers, overriding
      the NIO mode default. */
  public boolean noNio(String functionName) {
    return noNio.contains(functionName);
  }

  /** Returns true if the user requested that the given function
      should create a java.nio variant for the given function's
      <code>void*</code> and other C primitive pointers, overriding
      the NIO mode default. */
  public boolean forcedNio(String functionName) {
    return forcedNio.contains(functionName);
  }

  /** Returns the default NIO generation mode for C primitive pointer
      arguments. NIO_MODE_VOID_ONLY is the default and specifies
      that only void* arguments will have java.nio variants generated
      for them. NIO_MODE_ALL_POINTERS specifies that all C
      primitive arguments will have java.nio variants generated. */
  public int nioMode() {
    return nioMode;
  }

  /** Returns true if, for the plethora of java.nio variants generated
      for primitive C pointer types, the emitter should flatten the
      output down to two variants: one taking only Java primitive
      arrays as arguments, and one taking only java.nio.Buffers as
      arguments. */
  public boolean flattenNIOVariants() {
    return flattenNIOVariants;
  }

  /** Returns true if the glue code for the given function will be
      manually implemented by the end user. */
  public boolean manuallyImplement(String functionName) {
    return manuallyImplement.contains(functionName);
  }

  /** Returns a list of Strings containing user-implemented code for
      the given Java type name (not fully-qualified, only the class
      name); returns either null or an empty list if there is no
      custom code for the class. */
  public List customJavaCodeForClass(String className) {
    List res = (List) customJavaCode.get(className);
    if (res == null) {
      res = new ArrayList();
      customJavaCode.put(className, res);
    }
    return res;
  }

  /** Returns a list of Strings containing Javadoc documentation for
      the given Java type name (not fully-qualified, only the class
      name); returns either null or an empty list if there is no
      Javadoc documentation for the class. */
  public List javadocForClass(String className) {
    List res = (List) classJavadoc.get(className);
    if (res == null) {
      res = new ArrayList();
      classJavadoc.put(className, res);
    }
    return res;
  }

  /** Returns the package into which to place the glue code for
      accessing the specified struct. Defaults to emitting into the
      regular package (i.e., the result of {@link #packageName}). */
  public String packageForStruct(String structName) {
    String res = (String) structPackages.get(structName);
    if (res == null) {
      res = packageName;
    }
    return res;
  }

  /** Returns, as a List of Strings, the custom C code to be emitted
      along with the glue code for the main class. */
  public List/*<String>*/ customCCode() {
    return customCCode;
  }

  /** Returns, as a List of Strings, the structs for which glue code
      emission should be forced. */
  public List/*<String>*/ forcedStructs() {
    return forcedStructs;
  }

  /** Returns a List of Integers indicating the indices of arguments
      in this function that should be expanded to the same type when
      binding functions with multiple void* arguments. Returns null if
      no such indices were specified. */
  public List/*<Integer>*/ mirroredArgs(String functionName) {
    return (List) mirroredArgs.get(functionName);
  }

  /** Returns a MessageFormat string of the C expression calculating
      the capacity of the java.nio.ByteBuffer being returned from a
      native method, or null if no expression has been specified. */
  public String returnValueCapacity(String functionName) {
    return (String) returnValueCapacities.get(functionName);
  }

  /** Returns a MessageFormat string of the C expression calculating
      the length of the array being returned from a native method, or
      null if no expression has been specified. */
  public String returnValueLength(String functionName) {
    return (String) returnValueLengths.get(functionName);
  }

  /** Returns a List of Strings of expressions declaring temporary C
      variables in the glue code for the specified function. */
  public List/*<String>*/ temporaryCVariableDeclarations(String functionName) {
    return (List) temporaryCVariableDeclarations.get(functionName);
  }

  /** Returns a List of Strings of expressions containing assignments
      to temporary C variables in the glue code for the specified
      function. */
  public List/*<String>*/ temporaryCVariableAssignments(String functionName) {
    return (List) temporaryCVariableAssignments.get(functionName);
  }

  /** Returns a List of Strings indicating the interfaces the passed
      interface should declare it extends. May return null or a list
      of zero length if there are none. */
  public List/*<String>*/ extendedInterfaces(String interfaceName) {
    List res = (List) extendedInterfaces.get(interfaceName);
    if (res == null) {
      res = new ArrayList();
      extendedInterfaces.put(interfaceName, res);
    }
    return res;
  }

  /** Returns a List of Strings indicating the interfaces the passed
      class should declare it implements. May return null or a list
      of zero length if there are none. */
  public List/*<String>*/ implementedInterfaces(String className) {
    List res = (List) implementedInterfaces.get(className);
    if (res == null) {
      res = new ArrayList();
      implementedInterfaces.put(className, res);
    }
    return res;
  }

  /** Returns true if this #define, function, struct, or field within
      a struct should be ignored during glue code generation. */
  public boolean shouldIgnore(String symbol) {

    //System.err.println("CHECKING IGNORE: " + symbol);

    // Simple case; the entire symbol is in the ignore table.
    if (ignores.contains(symbol)) {
      return true;
    }

    // Ok, the slow case. We need to check the entire table, in case the table
    // contains an regular expression that matches the symbol.
    for (Iterator iter = ignores.iterator(); iter.hasNext(); ) {
      Pattern regexp = (Pattern)iter.next();
      Matcher matcher = regexp.matcher(symbol);
      if (matcher.matches()) {
        return true;
      }
    }

    // Check negated ignore table if not empty
    if (ignoreNots.size() > 0) {
      // Ok, the slow case. We need to check the entire table, in case the table
      // contains an regular expression that matches the symbol.
      for (Iterator iter = ignoreNots.iterator(); iter.hasNext(); ) {
        Pattern regexp = (Pattern)iter.next();
        Matcher matcher = regexp.matcher(symbol);
        if (!matcher.matches()) {
          return true;
        }
      }
    }

    return false;
  }

  /** Returns true if this function should be given a body which
      throws a run-time exception with an "unimplemented" message
      during glue code generation. */
  public boolean isUnimplemented(String symbol) {

    // Simple case; the entire symbol is in the ignore table.
    if (unimplemented.contains(symbol)) {
      return true;
    }

    // Ok, the slow case. We need to check the entire table, in case the table
    // contains an regular expression that matches the symbol.
    for (Iterator iter = unimplemented.iterator(); iter.hasNext(); ) {
      Pattern regexp = (Pattern)iter.next();
      Matcher matcher = regexp.matcher(symbol);
      if (matcher.matches()) {
        return true;
      }
    }

    return false;
  }

  /** Returns a replacement name for this type, which should be the
      name of a Java wrapper class for a C struct, or the name
      unchanged if no RenameJavaType directive was specified for this
      type. */
  public String renameJavaType(String javaTypeName) {
    String rename = (String) javaTypeRenames.get(javaTypeName);
    if (rename != null) {
      return rename;
    }
    return javaTypeName;
  }

  /** Returns true if the emission style is AllStatic. */
  public boolean allStatic() {
    return (emissionStyle == JavaEmitter.ALL_STATIC);
  }

  /** Returns true if an interface should be emitted during glue code generation. */
  public boolean emitInterface() {
    return (emissionStyle() == JavaEmitter.INTERFACE_AND_IMPL ||
            emissionStyle() == JavaEmitter.INTERFACE_ONLY);
  }

  /** Returns true if an implementing class should be emitted during glue code generation. */
  public boolean emitImpl() {
    return (emissionStyle() == JavaEmitter.ALL_STATIC ||
            emissionStyle() == JavaEmitter.INTERFACE_AND_IMPL ||
            emissionStyle() == JavaEmitter.IMPL_ONLY);
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  protected void dispatch(String cmd, StringTokenizer tok, File file, String filename, int lineNo) throws IOException {
    //System.err.println("read cmd = [" + cmd + "]"); 
    if (cmd.equalsIgnoreCase("Package")) {
      packageName = readString("package", tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("ImplPackage")) {
      implPackageName = readString("ImplPackage", tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("JavaClass")) {
      className = readString("JavaClass", tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("ImplJavaClass")) {
      implClassName = readString("ImplJavaClass", tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("JavaOutputDir")) {
      javaOutputDir = readString("JavaOutputDir", tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("NativeOutputDir")) {
      nativeOutputDir = readString("NativeOutputDir", tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("HierarchicalNativeOutput")) {
      String tmp = readString("HierarchicalNativeOutput", tok, filename, lineNo);
      nativeOutputUsesJavaHierarchy = Boolean.valueOf(tmp).booleanValue();
    } else if (cmd.equalsIgnoreCase("Style")) {
      String style = readString("Style", tok, filename, lineNo);
      if (style.equalsIgnoreCase("AllStatic")) {
        emissionStyle = JavaEmitter.ALL_STATIC;
      } else if (style.equalsIgnoreCase("InterfaceAndImpl")) {
        emissionStyle = JavaEmitter.INTERFACE_AND_IMPL;
      } else if (style.equalsIgnoreCase("InterfaceOnly")) {
        emissionStyle = JavaEmitter.INTERFACE_ONLY;
      } else if (style.equalsIgnoreCase("ImplOnly")) {
        emissionStyle = JavaEmitter.IMPL_ONLY;
      } else {
        System.err.println("WARNING: Error parsing \"style\" command at line " + lineNo +
                           " in file \"" + filename + "\"");
      }
    } else if (cmd.equalsIgnoreCase("Import")) {
      imports.add(readString("Import", tok, filename, lineNo));
    } else if (cmd.equalsIgnoreCase("Opaque")) {
      readOpaque(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("ReturnsString")) {
      readReturnsString(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("ReturnedArrayLength")) {
      readReturnedArrayLength(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because ReturnedArrayLength changes them.
    } else if (cmd.equalsIgnoreCase("ArgumentIsString")) {
      readArgumentIsString(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("Ignore")) {
      readIgnore(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("IgnoreNot")) {
      readIgnoreNot(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("Unimplemented")) {
      readUnimplemented(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("IgnoreField")) {
      readIgnoreField(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("ManuallyImplement")) {
      readManuallyImplement(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("CustomJavaCode")) {
      readCustomJavaCode(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because readCustomJavaCode changes them.
    } else if (cmd.equalsIgnoreCase("CustomCCode")) {
      readCustomCCode(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because readCustomCCode changes them.
    } else if (cmd.equalsIgnoreCase("ClassJavadoc")) {
      readClassJavadoc(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because readClassJavadoc changes them.
    } else if (cmd.equalsIgnoreCase("NioOnly")) {
      nioOnly.add(readString("NioOnly", tok, filename, lineNo));
    } else if (cmd.equalsIgnoreCase("NoNio")) {
      noNio.add(readString("NoNio", tok, filename, lineNo));
    } else if (cmd.equalsIgnoreCase("ForcedNio")) {
      forcedNio.add(readString("ForcedNio", tok, filename, lineNo));
    } else if (cmd.equalsIgnoreCase("NioMode")) {
      readNioMode(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("FlattenNIOVariants")) {
      flattenNIOVariants = readBoolean("FlattenNIOVariants", tok, filename, lineNo).booleanValue();
    } else if (cmd.equalsIgnoreCase("EmitStruct")) {
      forcedStructs.add(readString("EmitStruct", tok, filename, lineNo));
    } else if (cmd.equalsIgnoreCase("MirrorExpandedBindingArgs")) {
      readMirrorExpandedBindingArgs(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("StructPackage")) {
      readStructPackage(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("TemporaryCVariableDeclaration")) {
      readTemporaryCVariableDeclaration(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because TemporaryCVariableDeclaration changes them.
    } else if (cmd.equalsIgnoreCase("TemporaryCVariableAssignment")) {
      readTemporaryCVariableAssignment(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because TemporaryCVariableAssignment changes them.
    } else if (cmd.equalsIgnoreCase("ReturnValueCapacity")) {
      readReturnValueCapacity(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because ReturnValueCapacity changes them.
    } else if (cmd.equalsIgnoreCase("ReturnValueLength")) {
      readReturnValueLength(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because ReturnValueLength changes them.
    } else if (cmd.equalsIgnoreCase("Include")) {
      doInclude(tok, file, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("IncludeAs")) {
      doIncludeAs(tok, file, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("Extends")) {
      readExtend(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("Implements")) {
      readImplements(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("RenameJavaType")) {
      readRenameJavaType(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("RuntimeExceptionType")) {
      runtimeExceptionType = readString("RuntimeExceptionType", tok, filename, lineNo);
    } else {
      throw new RuntimeException("Unknown command \"" + cmd +
                                 "\" in command file " + filename +
                                 " at line number " + lineNo);
    }
  }

  protected String readString(String cmd, StringTokenizer tok, String filename, int lineNo) {
    try {
      return tok.nextToken();
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"" + cmd + "\" command at line " + lineNo +
        " in file \"" + filename + "\": missing expected parameter", e);
    }
  }

  protected Boolean readBoolean(String cmd, StringTokenizer tok, String filename, int lineNo) {
    try {
      return Boolean.valueOf(tok.nextToken());
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"" + cmd + "\" command at line " + lineNo +
        " in file \"" + filename + "\": missing expected boolean value", e);
    }
  }

  protected Class stringToPrimitiveType(String type) throws ClassNotFoundException {
    if (type.equals("boolean")) return Boolean.TYPE;
    if (type.equals("byte"))    return Integer.TYPE;
    if (type.equals("char"))    return Character.TYPE;
    if (type.equals("short"))   return Short.TYPE;
    if (type.equals("int"))     return Integer.TYPE;
    if (type.equals("long"))    return Long.TYPE;
    if (type.equals("float"))   return Float.TYPE;
    if (type.equals("double"))  return Double.TYPE;
    throw new RuntimeException("Only primitive types are supported here");
  }

  protected void readOpaque(StringTokenizer tok, String filename, int lineNo) {
    try {
      JavaType javaType = JavaType.createForClass(stringToPrimitiveType(tok.nextToken()));
      String cType = null;
      while (tok.hasMoreTokens()) {
        if (cType == null) {
          cType = tok.nextToken();
        } else {
          cType = cType + " " + tok.nextToken();
        }
      }
      if (cType == null) {
        throw new RuntimeException("No C type for \"Opaque\" command at line " + lineNo +
          " in file \"" + filename + "\"");
      }
      TypeInfo info = parseTypeInfo(cType, javaType);
      addTypeInfo(info);
    } catch (Exception e) {
      throw new RuntimeException("Error parsing \"Opaque\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readReturnsString(StringTokenizer tok, String filename, int lineNo) {
    try {
      String name = tok.nextToken();
      returnsString.add(name);
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ReturnsString\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readReturnedArrayLength(StringTokenizer tok, String filename, int lineNo) {
    try {
      String functionName = tok.nextToken();
      String restOfLine = tok.nextToken("\n\r\f");
      restOfLine = restOfLine.trim();
      returnedArrayLengths.put(functionName, restOfLine);
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ReturnedArrayLength\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readIgnore(StringTokenizer tok, String filename, int lineNo) {
    try {
      String regex = tok.nextToken();
      ignores.add(Pattern.compile(regex));
      //System.err.println("IGNORING " + regex + " / " + ignores.get(regex));
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"Ignore\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readIgnoreNot(StringTokenizer tok, String filename, int lineNo) {
    try {
      String regex = tok.nextToken();
      ignoreNots.add(Pattern.compile(regex));
      //System.err.println("IGNORING NEGATION OF " + regex + " / " + ignores.get(regex));
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"IgnoreNot\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readUnimplemented(StringTokenizer tok, String filename, int lineNo) {
    try {
      String regex = tok.nextToken();
      unimplemented.add(Pattern.compile(regex));
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"Unimplemented\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readIgnoreField(StringTokenizer tok, String filename, int lineNo) {
    try {
      String containingStruct = tok.nextToken();
      String name = tok.nextToken();
      ignores.add(Pattern.compile(containingStruct + " " + name));
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"IgnoreField\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readManuallyImplement(StringTokenizer tok, String filename, int lineNo) {
    try {
      String name = tok.nextToken();
      manuallyImplement.add(name);
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ManuallyImplement\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readCustomJavaCode(StringTokenizer tok, String filename, int lineNo) {
    try {
      String className = tok.nextToken();
      String restOfLine = tok.nextToken("\n\r\f");
      addCustomJavaCode(className, restOfLine);
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"CustomJavaCode\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void addCustomJavaCode(String className, String code) {
    List codeList = customJavaCodeForClass(className);
    codeList.add(code);
  }

  protected void readCustomCCode(StringTokenizer tok, String filename, int lineNo) {
    try {
      String restOfLine = tok.nextToken("\n\r\f");
      customCCode.add(restOfLine);
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"CustomCCode\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readClassJavadoc(StringTokenizer tok, String filename, int lineNo) {
    try {
      String className = tok.nextToken();
      String restOfLine = tok.nextToken("\n\r\f");
      addClassJavadoc(className, restOfLine);
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ClassJavadoc\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void addClassJavadoc(String className, String code) {
    List codeList = javadocForClass(className);
    codeList.add(code);
  }

  /**
   * Sets the default NIO generation mode for C primitive
   * pointers. Options are VOID_ONLY or ALL_POINTERS. When the mode is
   * set to VOID_ONLY, java.nio variants of methods are only generated
   * for C primitive pointers of type <code>void*</code>. All other
   * pointers are translated by default into Java primitive arrays.
   * When the mode is set to ALL_POINTERS, C primitive pointers of
   * other types (i.e., <code>int*</code>) will have java.nio variants
   * generated for them (i.e., <code>IntBuffer</code> as opposed to
   * merely <code>int[]</code>). This default mode can be overridden
   * with the NioOnly and NoNio directives. The default for this mode
   * is currently VOID_ONLY.
   */
  protected void readNioMode(StringTokenizer tok, String filename, int lineNo) {
    try {
      String mode = tok.nextToken();
      if (mode.equalsIgnoreCase("VOID_ONLY")) {
        nioMode = NIO_MODE_VOID_ONLY;
      } else if (mode.equalsIgnoreCase("ALL_POINTERS")) {
        nioMode = NIO_MODE_ALL_POINTERS;
      } else {
        throw new RuntimeException("Error parsing \"NioMode\" command at line " + lineNo +
                                   " in file \"" + filename + "\"; expected VOID_ONLY or ALL_POINTERS");
      }
    } catch (NoSuchElementException e) {
      throw new RuntimeException(
                                 "Error parsing \"NioMode\" command at line " + lineNo +
                                 " in file \"" + filename + "\"", e);
    }
  }

  /**
   * When void* arguments in the C function prototypes are encountered, the
   * emitter will try to expand the binding and create Java entry points for
   * all possible array types. If there are 2 or more void* arguments in the C
   * prototype, this directive lets you specify which of those arguments
   * should always be expanded to the same type. <p>
   *
   * For example, given the C prototype:
   * <pre>
   * void FuncName(void *foo, void *bar);
   * </pre>
   *
   * The emitter will normally emit multiple Java entry points:
   * <pre>
   * public abstract void FuncName(boolean[] foo, java.nio.Buffer bar);
   * public abstract void FuncName(boolean[] foo, boolean[] bar);
   * public abstract void FuncName(boolean[] foo, byte[] bar);
   * public abstract void FuncName(boolean[] foo, char[] bar);
   * public abstract void FuncName(boolean[] foo, short[] bar);
   * public abstract void FuncName(boolean[] foo, int[] bar);
   * public abstract void FuncName(boolean[] foo, long[] bar);
   * public abstract void FuncName(boolean[] foo, float[] bar);
   * public abstract void FuncName(boolean[] foo, double[] bar);
   * 
   * public abstract void FuncName(byte[] foo, java.nio.Buffer bar);
   * public abstract void FuncName(byte[] foo, boolean[] bar);
   * public abstract void FuncName(byte[] foo, byte[] bar);
   * <...etc for all variants on the second parameter...>
   * 
   * public abstract void FuncName(char[] foo, java.nio.Buffer bar);
   * public abstract void FuncName(char[] foo, boolean[] bar);
   * public abstract void FuncName(char[] foo, byte[] bar);
   * <...etc for all variants on the second parameter...>
   * <...and so on for all remaining variants on the first parameter...>
   * </pre>
   * 
   * This directive lets you specify that arguments at a particular index
   * should always be expanded to the same type. For example, the directive: 
   * <pre>
   * MirrorExpandedBindingArgs FuncName 0 1
   * </pre>
   * will force the first and second arguments in function FuncName to be
   * expanded identically. This would result in the emission of the following
   * entry points only:
   * <pre>
   * public abstract void FuncName(java.nio.Buffer[] foo, java.nio.Buffer bar);
   * public abstract void FuncName(boolean[] foo, boolean[] bar);
   * public abstract void FuncName(byte[] foo, byte[] bar);
   * public abstract void FuncName(char[] foo, char[] bar);
   * public abstract void FuncName(short[] foo, short[] bar);
   * public abstract void FuncName(int[] foo, int[] bar);
   * public abstract void FuncName(long[] foo, long[] bar);
   * public abstract void FuncName(float[] foo, float[] bar);
   * public abstract void FuncName(double[] foo, double[] bar);
   * </pre>
   */
   protected void readMirrorExpandedBindingArgs(StringTokenizer tok, String filename, int lineNo) {
     try {
      String methodName = tok.nextToken();
      ArrayList argIndices = new ArrayList(2);
      while (tok.hasMoreTokens())
      {
        Integer idx = Integer.valueOf(tok.nextToken());
        argIndices.add(idx);
      }

      if(argIndices.size() > 1)
      {
        mirroredArgs.put(methodName, argIndices);
      }
      else
      {
        throw new RuntimeException("ERROR: Error parsing \"MirrorExpandedBindingArgs\" command at line " + lineNo +
          " in file \"" + filename + "\": directive requires at least 2 argument indices");
      }
    } catch (NoSuchElementException e) {
      throw new RuntimeException(
        "Error parsing \"MirrorExpandedBindingArgs\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

 /**
   * When const char* arguments in the C function prototypes are encountered,
   * the emitter will normally convert them to <code>byte[]</code>
   * arguments. This directive lets you specify which of those arguments
   * should be converted to <code>String</code> arguments instead of <code>
   * byte[] </code>. <p>
   *
   * For example, given the C prototype:
   * <pre>
   * void FuncName(const char* ugh, int bar, const char *foo, const char* goop);
   * </pre>
   *
   * The emitter will normally emit:
   * <pre>
   * public abstract void FuncName(byte[] ugh, int bar, byte[] foo, byte[] goop);
   * </pre>
   * 
   * However, if you supplied the following directive:
   *
   * <pre>
   * ArgumentIsString FuncName 0 2
   * </pre>
   *
   * The emitter will instead emit:
   * <pre>
   * public abstract void FuncName(String ugh, int bar, String foo, byte[] goop);
   * </pre>
   *
   */
   protected void readArgumentIsString(StringTokenizer tok, String filename, int lineNo) {
     try {
      String methodName = tok.nextToken();
      ArrayList argIndices = new ArrayList(2);
      while (tok.hasMoreTokens()) {
        Integer idx = Integer.valueOf(tok.nextToken());
        argIndices.add(idx);
      }

      if(argIndices.size() > 0) {
        argumentsAreString.put(methodName, argIndices);
      }
      else {
        throw new RuntimeException("ERROR: Error parsing \"ArgumentIsString\" command at line " + lineNo +
          " in file \"" + filename + "\": directive requires specification of at least 1 index");
      }
    } catch (NoSuchElementException e) {
      throw new RuntimeException(
        "Error parsing \"ArgumentIsString\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }
  
  protected void readStructPackage(StringTokenizer tok, String filename, int lineNo) {
    try {
      String struct = tok.nextToken();
      String pkg = tok.nextToken();
      structPackages.put(struct, pkg);
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"StructPackage\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readReturnValueCapacity(StringTokenizer tok, String filename, int lineNo) {
    try {
      String functionName = tok.nextToken();
      String restOfLine = tok.nextToken("\n\r\f");
      restOfLine = restOfLine.trim();
      returnValueCapacities.put(functionName, restOfLine);
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ReturnValueCapacity\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readReturnValueLength(StringTokenizer tok, String filename, int lineNo) {
    try {
      String functionName = tok.nextToken();
      String restOfLine = tok.nextToken("\n\r\f");
      restOfLine = restOfLine.trim();
      returnValueLengths.put(functionName, restOfLine);
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ReturnValueLength\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readTemporaryCVariableDeclaration(StringTokenizer tok, String filename, int lineNo) {
    try {
      String functionName = tok.nextToken();
      String restOfLine = tok.nextToken("\n\r\f");
      restOfLine = restOfLine.trim();
      List list = (List) temporaryCVariableDeclarations.get(functionName);
      if (list == null) {
        list = new ArrayList/*<String>*/();
        temporaryCVariableDeclarations.put(functionName, list);
      }
      list.add(restOfLine);
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"TemporaryCVariableDeclaration\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readTemporaryCVariableAssignment(StringTokenizer tok, String filename, int lineNo) {
    try {
      String functionName = tok.nextToken();
      String restOfLine = tok.nextToken("\n\r\f");
      restOfLine = restOfLine.trim();
      List list = (List) temporaryCVariableAssignments.get(functionName);
      if (list == null) {
        list = new ArrayList/*<String>*/();
        temporaryCVariableAssignments.put(functionName, list);
      }
      list.add(restOfLine);
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"TemporaryCVariableAssignment\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void doInclude(StringTokenizer tok, File file, String filename, int lineNo) throws IOException {
    try {
      String includedFilename = tok.nextToken();
      File includedFile = new File(includedFilename);
      if (!includedFile.isAbsolute()) {
        includedFile = new File(file.getParentFile(), includedFilename);
      }
      read(includedFile.getAbsolutePath());
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"Include\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void doIncludeAs(StringTokenizer tok, File file, String filename, int lineNo) throws IOException {
    try {
      StringBuffer linePrefix = new StringBuffer(128);
      while (tok.countTokens() > 1)
      {
        linePrefix.append(tok.nextToken());
        linePrefix.append(" ");  
      }
      // last token is filename
      String includedFilename = tok.nextToken();
      File includedFile = new File(includedFilename);
      if (!includedFile.isAbsolute()) {
        includedFile = new File(file.getParentFile(), includedFilename);
      }
      read(includedFile.getAbsolutePath(), linePrefix.toString());
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"IncludeAs\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readExtend(StringTokenizer tok, String filename, int lineNo) {
    try {
      String interfaceName = tok.nextToken();
      List intfs = extendedInterfaces(interfaceName);
      intfs.add(tok.nextToken());
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"Extends\" command at line " + lineNo +
        " in file \"" + filename + "\": missing expected parameter", e);
    }
  }

  protected void readImplements(StringTokenizer tok, String filename, int lineNo) {
    try {
      String className = tok.nextToken();
      List intfs = implementedInterfaces(className);
      intfs.add(tok.nextToken());
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"Implements\" command at line " + lineNo +
        " in file \"" + filename + "\": missing expected parameter", e);
    }
  }

  protected void readRenameJavaType(StringTokenizer tok, String filename, int lineNo) {
    try {
      String fromName = tok.nextToken();
      String toName   = tok.nextToken();
      javaTypeRenames.put(fromName, toName);
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"RenameJavaType\" command at line " + lineNo +
        " in file \"" + filename + "\": missing expected parameter", e);
    }
  }

  protected static TypeInfo parseTypeInfo(String cType, JavaType javaType) {
    String typeName = null;
    int pointerDepth = 0;
    int idx = 0;
    while (idx < cType.length() &&
           (cType.charAt(idx) != ' ') &&
           (cType.charAt(idx) != '*')) {
      ++idx;
    }
    typeName = cType.substring(0, idx);
    // Count pointer depth
    while (idx < cType.length()) {
      if (cType.charAt(idx) == '*') {
        ++pointerDepth;
      }
      ++idx;
    }
    return new TypeInfo(typeName, pointerDepth, javaType);
  }

  protected void addTypeInfo(TypeInfo info) {
    TypeInfo tmp = (TypeInfo) typeInfoMap.get(info.name());
    if (tmp == null) {
      typeInfoMap.put(info.name(), info);
      return;
    }
    while (tmp.next() != null) {
      tmp = tmp.next();
    }
    tmp.setNext(info);
  }
}

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
import java.util.*;
import java.util.regex.*;

  /**
   * Builds the StaticGLInfo class from the OpenGL header files (i.e., gl.h
   * and glext.h) whose paths were passed as arguments to {@link
   * #main(String[])}.
   *
   * It relies upon the assumption that a function's membership is scoped by
   * preprocessor blocks in the header files that match the following pattern:
   * <br>
   *
   * <pre>
   * 
   * #ifndef GL_XXXX
   * GLAPI <returnType> <APIENTRY|GLAPIENTRY> glFuncName(<params>)
   * #endif GL_XXXX
   *
   * </pre>
   *
   * For example, if it parses the following data:
   *
   * <pre>
   * 
   * #ifndef GL_VERSION_1_3
   * GLAPI void APIENTRY glActiveTexture (GLenum);
   * GLAPI void APIENTRY glMultiTexCoord1dv (GLenum, const GLdouble *);
   * GLAPI void  <APIENTRY|GLAPIENTRY> glFuncName(<params>)
   * #endif GL_VERSION_1_3
   *
   * #ifndef GL_ARB_texture_compression
   * GLAPI void APIENTRY glCompressedTexImage3DARB (GLenum, GLint, GLenum, GLsizei, GLsizei, GLsizei, GLint, GLsizei, const GLvoid *);
   * GLAPI void APIENTRY glCompressedTexImage2DARB (GLenum, GLint, GLenum, GLsizei, GLsizei, GLint, GLsizei, const GLvoid *);
   * #endif
   * 
   * </pre>
   *
   * It will associate
   *   <code> glActiveTexture </code> and
   *   <code> glMultiTexCoord1dv </code>
   * with the symbol
   *   <code> GL_VERSION_1_3 </code>,
   * and associate
   *   <code> glCompressedTexImage2DARB </code> and
   *   <code> glCompressedTexImage3DARB </code>
   * with the symbol
   *   <code> GL_ARB_texture_compression </code>.
   * */
public class BuildStaticGLInfo
{
  // Handles function pointer 
  protected static Pattern funcPattern =
    Pattern.compile("^(GLAPI|extern)?(\\s*)(\\w+)(\\*)?(\\s+)(GLAPIENTRY|APIENTRY|WINAPI)?(\\s*)([w]?gl\\w+)\\s?(\\(.*)");
  protected static Pattern associationPattern =
    Pattern.compile("\\#ifndef ([W]?GL[X]?_[A-Za-z0-9_]+)");
  protected static Pattern definePattern =
    Pattern.compile("\\#define ([W]?GL[X]?_[A-Za-z0-9_]+)\\s*([A-Za-z0-9_]+)");
  // Maps function / #define names to the names of the extensions they're declared in
  protected Map declarationToExtensionMap = new HashMap();
  // Maps extension names to Set of identifiers (both #defines and
  // function names) this extension declares
  protected Map/*<String, Set<String>*/ extensionToDeclarationMap = new HashMap();

  /**
   * The first argument is the package to which the StaticGLInfo class
   * belongs, the second is the path to the directory in which that package's
   * classes reside, and the remaining arguments are paths to the C header
   * files that should be parsed
   */
  public static void main(String[] args) throws IOException
  {
    if (args.length > 0 && args[0].equals("-test")) {
      BuildStaticGLInfo builder = new BuildStaticGLInfo();
      String[] newArgs = new String[args.length - 1];
      System.arraycopy(args, 1, newArgs, 0, args.length - 1);
      builder.parse(newArgs);
      builder.dump();
      System.exit(0);
    }

    String packageName = args[0];
    String packageDir = args[1];

    String[] cHeaderFilePaths = new String[args.length-2];
    System.arraycopy(args, 2, cHeaderFilePaths, 0, cHeaderFilePaths.length);
    
    BuildStaticGLInfo builder = new BuildStaticGLInfo();
    try {
      builder.parse(cHeaderFilePaths);

      File file = new File(packageDir + File.separatorChar + "StaticGLInfo.java");
      String parentDir = file.getParent();
      if (parentDir != null) {
        File pDirFile = new File(parentDir);
        pDirFile.mkdirs();
      }

      PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
      builder.emitJavaCode(writer, packageName);

      writer.flush();
      writer.close();
    }
    catch (Exception e)
    {
      StringBuffer buf = new StringBuffer("{ ");
      for (int i = 0; i < cHeaderFilePaths.length; ++i)
      {
        buf.append(cHeaderFilePaths[i]);
        buf.append(" ");
      }
      buf.append('}');
      throw new RuntimeException(
        "Error building StaticGLInfo.java from " + buf.toString(), e);        
    }
  }

  
  /** Parses the supplied C header files and adds the function
      associations contained therein to the internal map. */
  public void parse(String[] cHeaderFilePaths) throws IOException {
    for (int i = 0; i < cHeaderFilePaths.length; i++) {
      parse(cHeaderFilePaths[i]);
    }
  }

  /** Parses the supplied C header file and adds the function
      associations contained therein to the internal map. */
  public void parse(String cHeaderFilePath) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(cHeaderFilePath));
    String line, activeAssociation = null;
    Matcher m = null;
    while ((line = reader.readLine()) != null) {
      // see if we're inside a #ifndef GL_XXX block and matching a function
      if (activeAssociation != null) {
        String identifier = null;
        if ((m = funcPattern.matcher(line)).matches()) {
          identifier = m.group(8);
        } else if ((m = definePattern.matcher(line)).matches()) {
          identifier = m.group(1);
        } else if (line.startsWith("#endif")) {
          activeAssociation = null;
        }
        if ((identifier != null) &&
            (activeAssociation != null) &&
            // Handles #ifndef GL_... #define GL_...
            !identifier.equals(activeAssociation)) {
          addAssociation(identifier, activeAssociation);
        }
      } else if ((m = associationPattern.matcher(line)).matches()) {
        // found a new #ifndef GL_XXX block
        activeAssociation = m.group(1);
        
        //System.err.println("FOUND NEW ASSOCIATION BLOCK: " + activeAssociation);
      }
    }
    reader.close();
  }

  public void dump() {
    for (Iterator i1 = extensionToDeclarationMap.keySet().iterator(); i1.hasNext(); ) {
      String name = (String) i1.next();
      Set decls = (Set) extensionToDeclarationMap.get(name);
      System.out.println(name + ":");
      List l = new ArrayList();
      l.addAll(decls);
      Collections.sort(l);
      for (Iterator i2 = l.iterator(); i2.hasNext(); ) {
        System.out.println("  " + (String) i2.next());
      }
    }
  }

  public String getExtension(String identifier) {
    return (String) declarationToExtensionMap.get(identifier);
  }
  
  public Set getDeclarations(String extension) {
    return (Set) extensionToDeclarationMap.get(extension);
  }

  public void emitJavaCode(PrintWriter output, String packageName) {
    output.println("package " + packageName + ";");
    output.println();
    output.println("import java.util.*;");
    output.println();
    output.println("public final class StaticGLInfo");
    output.println("{");

    output.println("  // maps function names to the extension string or OpenGL");
    output.println("  // specification version string to which they correspond.");
    output.println("  private static HashMap funcToAssocMap;");
    output.println();

    output.println("  /**");
    output.println("   * Returns the OpenGL extension string or GL_VERSION string with which the");
    output.println("   * given function is associated. <P>");
    output.println("   *");
    output.println("   * If the");
    output.println("   * function is part of the OpenGL core, the returned value will be");
    output.println("   * GL_VERSION_XXX where XXX represents the OpenGL version of which the");
    output.println("   * function is a member (XXX will be of the form \"A\" or \"A_B\" or \"A_B_C\";");
    output.println("   * e.g., GL_VERSION_1_2_1 for OpenGL version 1.2.1).");
    output.println("   *");
    output.println("   * If the function is an extension function, the returned value will the");
    output.println("   * OpenGL extension string for the extension to which the function");
    output.println("   * corresponds. For example, if glLoadTransposeMatrixfARB is the argument,");
    output.println("   * GL_ARB_transpose_matrix will be the value returned.");
    output.println("   * Please see http://oss.sgi.com/projects/ogl-sample/registry/index.html for");
    output.println("   * a list of extension names and the functions they expose.");
    output.println("   *");
    output.println("   * If the function specified is not part of any known OpenGL core version or");
    output.println("   * extension, then NULL will be returned.");
    output.println("   */");
    output.println("  public static String getFunctionAssociation(String glFunctionName)");
    output.println("  {");
    output.println("    return (String)funcToAssocMap.get(glFunctionName);");
    output.println("  }");
    output.println();

    output.println("  static");
    output.println("  {");

    // Compute max capacity
    int maxCapacity = 0;
    for (Iterator iter = declarationToExtensionMap.keySet().iterator(); iter.hasNext(); ) {
      String name = (String) iter.next();
      if (!name.startsWith("GL")) {
        ++maxCapacity;
      }
    }

    output.println("    funcToAssocMap = new HashMap(" + maxCapacity + "); // approximate max capacity");
    output.println("    String group;");
    ArrayList sets = new ArrayList(extensionToDeclarationMap.keySet());
    Collections.sort(sets);
    for (Iterator iter = sets.iterator(); iter.hasNext(); ) {
      String groupName = (String) iter.next();
      Set funcs = (Set) extensionToDeclarationMap.get(groupName);
      List l = new ArrayList();
      l.addAll(funcs);
      Collections.sort(l);
      Iterator funcIter = l.iterator();
      boolean printedHeader = false;
      while (funcIter.hasNext()) {
        String funcName = (String)funcIter.next();
        if (!funcName.startsWith("GL")) {
          if (!printedHeader) {
            output.println();
            output.println("    //----------------------------------------------------------------");
            output.println("    //                 " + groupName);
            output.println("    //----------------------------------------------------------------");
            output.println("    group = \"" + groupName + "\";");
            printedHeader = true;
          }

          output.println("    funcToAssocMap.put(\"" + funcName + "\", group);");
        }
      }
    }
    output.println("  }");
    output.println("} // end class StaticGLInfo");
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  protected void addAssociation(String identifier, String association) {
    declarationToExtensionMap.put(identifier, association);
    Set/*<String>*/ identifiers = (Set) extensionToDeclarationMap.get(association);
    if (identifiers == null) {
      identifiers = new HashSet/*<String>*/();
      extensionToDeclarationMap.put(association, identifiers);
    }
    identifiers.add(identifier);
  }
}

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
  protected static Pattern funcPattern =
    Pattern.compile("^(GLAPI|extern)?(\\s*)(\\w+)(\\*)?(\\s+)(APIENTRY|WINAPI)?(\\s*)([w]?gl\\w+)\\s?(\\(.*)");
  protected static Pattern associationPattern =
    Pattern.compile("\\#ifndef ([W]?GL[X]?_[A-Za-z0-9_]+)");

  /**
   * The first argument is the package to which the StaticGLInfo class
   * belongs, the second is the path to the directory in which that package's
   * classes reside, and the remaining arguments are paths to the C header
   * files that should be parsed
   */
   public static void main(String[] args)
  {
    String packageName = args[0];
    String packageDir = args[1];

    String[] cHeaderFilePaths = new String[args.length-2];
    System.arraycopy(args, 2, cHeaderFilePaths, 0, cHeaderFilePaths.length);
    
    BuildStaticGLInfo builder = new BuildStaticGLInfo();
    try
    {
      File file = new File(packageDir + File.separatorChar + "StaticGLInfo.java");
      String parentDir = file.getParent();
      if (parentDir != null)
      {
        File pDirFile = new File(parentDir);
        pDirFile.mkdirs();
      }

      PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
      builder.build(writer, packageName, cHeaderFilePaths);

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

  protected void build(PrintWriter output, String packageName, String[] cHeaderFilePaths) throws IOException
  {
    HashMap groupToFuncHash = new HashMap(50);
    for (int i = 0; i < cHeaderFilePaths.length; ++i)
    {
      process(groupToFuncHash, new FileReader(cHeaderFilePaths[i]));
    }

    emitJavaCode(output, packageName, groupToFuncHash);
  }
  
  protected void process(HashMap groupToFuncHash, FileReader headerFile) throws IOException
  {
    BufferedReader reader = new BufferedReader(headerFile);
    String line, activeAssociation = null;
    Matcher m;
    while ((line = reader.readLine()) != null)
    {
      // see if we're inside a #ifndef GL_XXX block and matching a function
      if (activeAssociation != null && (m = funcPattern.matcher(line)).matches())
      {
        // We found a new function associated with the last #ifndef block we
        // were associated with
        
        String funcName = m.group(8);
        HashSet funcsForGroup = (HashSet)groupToFuncHash.get(activeAssociation);
        if (funcsForGroup == null)
        {
          funcsForGroup = new HashSet(8);
          groupToFuncHash.put(activeAssociation, funcsForGroup);
        }
        funcsForGroup.add(funcName);

        //System.err.println("FOUND ASSOCIATION FOR " + activeAssociation + ": " + funcName);
      }
      else if ((m = associationPattern.matcher(line)).matches())
      {
        // found a new #ifndef GL_XXX block
        activeAssociation = m.group(1);

        //System.err.println("FOUND NEW ASSOCIATION BLOCK: " + activeAssociation);
      }
    }
  }
  
  protected void emitJavaCode(PrintWriter output, String packageName, HashMap groupToFuncHash)
  {
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
    output.println("    funcToAssocMap = new HashMap(1536); // approximate max capacity");
    output.println("    String group;");
    ArrayList sets = new ArrayList(groupToFuncHash.keySet());
    Collections.sort(sets);
    for (int i = 0; i < sets.size(); ++i)
    {
      String groupName = (String) sets.get(i);
      //System.err.println(groupName); // debug
      output.println();
      output.println("    //----------------------------------------------------------------");
      output.println("    //                 " + groupName);
      output.println("    //----------------------------------------------------------------");
      output.println("    group = \"" + groupName + "\";");
      HashSet funcs = (HashSet)groupToFuncHash.get(groupName);
      Iterator funcIter = funcs.iterator();      
      while (funcIter.hasNext())
      {
        String funcName = (String)funcIter.next();
        //System.err.println("   " + funcName); // debug        
        output.println("    funcToAssocMap.put(\"" + funcName + "\", group);");
      }
    }
    output.println("  }");

    output.println("} // end class StaticGLInfo");
  }

}

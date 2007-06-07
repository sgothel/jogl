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

import com.sun.gluegen.*;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class BuildComposablePipeline
{
  private String outputDirectory;
  private Class classToComposeAround;
  
  public static void main(String[] args)
  {
    String nameOfClassToComposeAround = args[0];
    Class classToComposeAround;
    try {
      classToComposeAround = Class.forName(nameOfClassToComposeAround);      
    } catch (Exception e) {
      throw new RuntimeException(
	"Could not find class \"" + nameOfClassToComposeAround + "\"", e);
    }    
    
    String outputDir = args[1];

    BuildComposablePipeline composer =
      new BuildComposablePipeline(classToComposeAround, outputDir);

    try
    {
      composer.emit();
    }
    catch (IOException e)
    {
      throw new RuntimeException(
	"Error generating composable pipeline source files", e);
    }
  }
  
  protected BuildComposablePipeline(Class classToComposeAround, String outputDirectory)
  {
    this.outputDirectory = outputDirectory;
    this.classToComposeAround = classToComposeAround;

    if (! classToComposeAround.isInterface())
    {
      throw new IllegalArgumentException(
	classToComposeAround.getName() + " is not an interface class");
    }    
  }

  /**
   * Emit the java source code for the classes that comprise the composable
   * pipeline.
   */
  public void emit() throws IOException
  {
    String pDir = outputDirectory;
    String pInterface = classToComposeAround.getName();    
    List/*<Method>*/ publicMethods = Arrays.asList(classToComposeAround.getMethods());

    (new DebugPipeline(pDir, pInterface)).emit(publicMethods);
    (new TracePipeline(pDir, pInterface)).emit(publicMethods);
  }

  //-------------------------------------------------------

  /**
   * Emits a Java source file that represents one element of the composable
   * pipeline. 
   */
  protected static abstract class PipelineEmitter
  {
    private File file;
    private String basePackage;
    private String baseName; // does not include package!
    private String outputDir;

    /**
     * @param outputDir the directory into which the pipeline classes will be
     * generated.
     * @param baseInterfaceClassName the full class name (including package,
     * e.g. "java.lang.String") of the interface that the pipeline wraps
     * @exception IllegalArgumentException if classToComposeAround is not an
     * interface.
     */
    public PipelineEmitter(String outputDir, String baseInterfaceClassName)
    {
      int lastDot = baseInterfaceClassName.lastIndexOf('.');
      if (lastDot == -1)
      {
	// no package, class is at root level
	this.baseName = baseInterfaceClassName;
	this.basePackage = null;
      }
      else
      {	
	this.baseName = baseInterfaceClassName.substring(lastDot+1);
	this.basePackage = baseInterfaceClassName.substring(0, lastDot);
      }

      this.outputDir = outputDir;
    }

    public void emit(List/*<Method>*/ methodsToWrap) throws IOException
    {
      String pipelineClassName = getPipelineName();
      this.file = new File(outputDir + File.separatorChar + pipelineClassName + ".java"); 
      String parentDir = file.getParent();
      if (parentDir != null)
      {
        File pDirFile = new File(parentDir);
        pDirFile.mkdirs();
      }

      PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(file)));

      CodeGenUtils.emitJavaHeaders(output, 
		  basePackage,
		  pipelineClassName,
                  "com.sun.gluegen.runtime", // FIXME: should make configurable
		  true,
		  new String[] { "java.io.*" },
		  new String[] { "public" },
		  new String[] { baseName },
		  null,
		  new CodeGenUtils.EmissionCallback() {
		    public void emit(PrintWriter w) { emitClassDocComment(w); }
		  }
		  );
      
      preMethodEmissionHook(output);      
		  
      constructorHook(output);

      for (int i = 0; i < methodsToWrap.size(); ++i)
      {
	Method m = (Method)methodsToWrap.get(i);
	emitMethodDocComment(output, m);
	emitSignature(output, m);
	emitBody(output, m);	
      }

      postMethodEmissionHook(output);

      output.println();
      output.print("  private " + baseName + " " + getDownstreamObjectName() + ";");

      // end the class
      output.println();
      output.print("} // end class ");
      output.println(pipelineClassName);

      output.flush();
      output.close();
    }

    /** Get the name of the object through which API calls should be routed. */
    protected String getDownstreamObjectName()
    {
      return "downstream" + baseName;
    }
    
    protected void emitMethodDocComment(PrintWriter output, Method m)
    {
    }
    
    protected void emitSignature(PrintWriter output, Method m)
    {
      output.print("  public ");
      output.print(' ');
      output.print(JavaType.createForClass(m.getReturnType()).getName());
      output.print(' ');
      output.print(m.getName());
      output.print('(');
      output.print(getArgListAsString(m, true, true));
      output.println(")");
    }
    
    protected void emitBody(PrintWriter output, Method m)
    {
      output.println("  {");
      output.print("    ");
      Class retType = m.getReturnType();

      preDownstreamCallHook(output, m);
      
      if (retType != Void.TYPE)
      {
	output.print(JavaType.createForClass(retType).getName());
	output.print(" _res = ");
      }
      output.print(getDownstreamObjectName());
      output.print('.');
      output.print(m.getName());
      output.print('(');
      output.print(getArgListAsString(m, false, true));
      output.println(");");
      
      postDownstreamCallHook(output, m);

      if (retType != Void.TYPE)
      {
	output.println("    return _res;");
      }
      output.println("  }");

    }

    private String getArgListAsString(Method m, boolean includeArgTypes, boolean includeArgNames)
    {
      StringBuffer buf = new StringBuffer(256);
      if (!includeArgNames && !includeArgTypes)
      {
	throw new IllegalArgumentException(
	  "Cannot generate arglist without both arg types and arg names");
      }
      
      Class[] argTypes = m.getParameterTypes();
      for (int i = 0; i < argTypes.length; ++i)
      {
	if (includeArgTypes)
	{
	  buf.append(JavaType.createForClass(argTypes[i]).getName());
	  buf.append(' ');
	}
	
	if (includeArgNames)
	{
	  buf.append("arg");
	  buf.append(i);
	}
	if (i < argTypes.length-1) { buf.append(','); }
      }

      return buf.toString();
    }
    
    /** The name of the class around which this pipeline is being
     * composed. E.g., if this pipeline was constructed with
     * "java.util.Set" as the baseInterfaceClassName, then this method will
     * return "Set".
     */
    protected String getBaseInterfaceName()
    {
      return baseName;
    }
    
    /** Get the name for this pipeline class. */
    protected abstract String getPipelineName();    

    /**
     * Called after the class headers have been generated, but before any
     * method wrappers have been generated.
     */
    protected abstract void preMethodEmissionHook(PrintWriter output);    

    /**
     * Emits the constructor for the pipeline; called after the preMethodEmissionHook.
     */
    protected void constructorHook(PrintWriter output) {
      output.print(  "  public " + getPipelineName() + "(" + baseName + " ");
      output.println(getDownstreamObjectName() + ")");
      output.println("  {");
      output.println("    if (" + getDownstreamObjectName() + " == null) {");
      output.println("      throw new IllegalArgumentException(\"null " + getDownstreamObjectName() + "\");");
      output.println("    }");
      output.print(  "    this." + getDownstreamObjectName());
      output.println(" = " + getDownstreamObjectName() + ";");
      output.println("    // Fetch GLContext object for better error checking (if possible)");
      output.println("    // FIXME: should probably put this method in GL rather than GLImpl");
      output.println("    if (" + getDownstreamObjectName() + " instanceof com.sun.opengl.impl.GLImpl) {");
      output.println("      _context = ((com.sun.opengl.impl.GLImpl) " + getDownstreamObjectName() + ").getContext();");
      output.println("    }");
      output.println("  }");
      output.println();
    }

    /**
     * Called after the method wrappers have been generated, but before the
     * closing parenthesis of the class is emitted.
     */
    protected abstract void postMethodEmissionHook(PrintWriter output);    

    /**
     * Called before the pipeline routes the call to the downstream object.
     */
    protected abstract void preDownstreamCallHook(PrintWriter output, Method m);    

    /**
     * Called after the pipeline has routed the call to the downstream object,
     * but before the calling function exits or returns a value.
     */
    protected abstract void postDownstreamCallHook(PrintWriter output, Method m);    

    /** Emit a Javadoc comment for this pipeline class. */
    protected abstract void emitClassDocComment(PrintWriter output);

  } // end class PipelineEmitter

  //-------------------------------------------------------

  protected class DebugPipeline extends PipelineEmitter
  {
    String className;
    String baseInterfaceClassName;
    public DebugPipeline(String outputDir, String baseInterfaceClassName)
    {
      super(outputDir, baseInterfaceClassName);
      className = "Debug" + getBaseInterfaceName();
    }

    protected String getPipelineName()
    {
      return className;
    }

    protected void preMethodEmissionHook(PrintWriter output)
    {
    }

    protected void postMethodEmissionHook(PrintWriter output)
    {
      output.println("  private void checkGLGetError(String caller)");
      output.println("  {");
      output.println("    if (insideBeginEndPair) {");
      output.println("      return;");
      output.println("    }");
      output.println();
      output.println("    // Debug code to make sure the pipeline is working; leave commented out unless testing this class");
      output.println("    //System.err.println(\"Checking for GL errors " +
		     "after call to \" + caller + \"()\");");
      output.println();
      output.println("    int err = " +
		     getDownstreamObjectName() +
		     ".glGetError();");
      output.println("    if (err == GL_NO_ERROR) { return; }");
      output.println();
      output.println("    StringBuffer buf = new StringBuffer(");
      output.println("      \"glGetError() returned the following error codes " +
		     "after a call to \" + caller + \"(): \");");
      output.println();
      output.println("    // Loop repeatedly to allow for distributed GL implementations,");
      output.println("    // as detailed in the glGetError() specification");
      output.println("    int recursionDepth = 10;");
      output.println("    do {");
      output.println("      switch (err) {");
      output.println("        case GL_INVALID_ENUM: buf.append(\"GL_INVALID_ENUM \"); break;");
      output.println("        case GL_INVALID_VALUE: buf.append(\"GL_INVALID_VALUE \"); break;");
      output.println("        case GL_INVALID_OPERATION: buf.append(\"GL_INVALID_OPERATION \"); break;");
      output.println("        case GL_STACK_OVERFLOW: buf.append(\"GL_STACK_OVERFLOW \"); break;");
      output.println("        case GL_STACK_UNDERFLOW: buf.append(\"GL_STACK_UNDERFLOW \"); break;");
      output.println("        case GL_OUT_OF_MEMORY: buf.append(\"GL_OUT_OF_MEMORY \"); break;");
      output.println("        case GL_NO_ERROR: throw new InternalError(\"Should not be treating GL_NO_ERROR as error\");");
      output.println("        default: throw new InternalError(\"Unknown glGetError() return value: \" + err);");
      output.println("      }");
      output.println("    } while ((--recursionDepth >= 0) && (err = " +
		     getDownstreamObjectName() +
		     ".glGetError()) != GL_NO_ERROR);");
      output.println("    throw new GLException(buf.toString());");
      output.println("  }");

      output.println("  /** True if the pipeline is inside a glBegin/glEnd pair.*/");
      output.println("  private boolean insideBeginEndPair = false;");
      output.println();
      output.println("  private void checkContext() {");
      output.println("    GLContext currentContext = GLContext.getCurrent();");
      output.println("    if (currentContext == null) {");
      output.println("      throw new GLException(\"No OpenGL context is current on this thread\");");
      output.println("    }");
      output.println("    if ((_context != null) && (_context != currentContext)) {");
      output.println("      throw new GLException(\"This GL object is being incorrectly used with a different GLContext than that which created it\");");
      output.println("    }");
      output.println("  }");
      output.println("  private GLContext _context;");
    }

    protected void emitClassDocComment(PrintWriter output)
    {
      output.println("/** <P> Composable pipeline which wraps an underlying {@link GL} implementation,");
      output.println("    providing error checking after each OpenGL method call. If an error occurs,");
      output.println("    causes a {@link GLException} to be thrown at exactly the point of failure.");
      output.println("    Sample code which installs this pipeline: </P>");
      output.println();
      output.println("<PRE>");
      output.println("     drawable.setGL(new DebugGL(drawable.getGL()));");
      output.println("</PRE>");
      output.println("*/");
    }
    
    protected void preDownstreamCallHook(PrintWriter output, Method m)
    {
      output.println("    checkContext();");
    }

    protected void postDownstreamCallHook(PrintWriter output, Method m)
    {
      if (m.getName().equals("glBegin"))
      {
	output.println("    insideBeginEndPair = true;");
	output.println("    // NOTE: can't check glGetError(); it's not allowed inside glBegin/glEnd pair");
      }
      else
      {
	if (m.getName().equals("glEnd"))
	{
	  output.println("    insideBeginEndPair = false;");
	}
	
	// calls to glGetError() are only allowed outside of glBegin/glEnd pairs
	output.println("    checkGLGetError(\"" + m.getName() + "\");");
      }
    }

  } // end class DebugPipeline

  //-------------------------------------------------------

  protected class TracePipeline extends PipelineEmitter
  {
    String className;
    String baseInterfaceClassName;
    public TracePipeline(String outputDir, String baseInterfaceClassName)
    {
      super(outputDir, baseInterfaceClassName);
      className = "Trace" + getBaseInterfaceName();
    }

    protected String getPipelineName()
    {
      return className;
    }

    protected void preMethodEmissionHook(PrintWriter output)
    {
    }

    protected void constructorHook(PrintWriter output) {
      output.print(  "  public " + getPipelineName() + "(" + getBaseInterfaceName() + " ");
      output.println(getDownstreamObjectName() + ", PrintStream " + getOutputStreamName() + ")");
      output.println("  {");
      output.println("    if (" + getDownstreamObjectName() + " == null) {");
      output.println("      throw new IllegalArgumentException(\"null " + getDownstreamObjectName() + "\");");
      output.println("    }");
      output.print(  "    this." + getDownstreamObjectName());
      output.println(" = " + getDownstreamObjectName() + ";");
      output.print(  "    this." + getOutputStreamName());
      output.println(" = " + getOutputStreamName() + ";");
      output.println("  }");
      output.println();
    }

    protected void postMethodEmissionHook(PrintWriter output)
    {
      output.println("private PrintStream " + getOutputStreamName() + ";");
      output.println("private int indent = 0;"); 
      output.println("protected String dumpArray(Object obj)");
      output.println("{");
      output.println("  if (obj == null) return \"[null]\";");
      output.println("  StringBuffer sb = new StringBuffer(\"[\");");
      output.println("  int len  = java.lang.reflect.Array.getLength(obj);");
      output.println("  int count = Math.min(len,16);");
      output.println("  for ( int i =0; i < count; i++ ) {");
      output.println("    sb.append(java.lang.reflect.Array.get(obj,i));");
      output.println("    if (i < count-1)"); 
      output.println("      sb.append(',');");
      output.println("  }");
      output.println("  if ( len > 16 )");
      output.println("    sb.append(\"...\").append(len);");
      output.println("  sb.append(']');");
      output.println("  return sb.toString();");
      output.println("}");
      output.println("protected void print(String str)");
      output.println("{");
      output.println("  "+getOutputStreamName()+".print(str);");
      output.println("}");
      output.println("protected void println(String str)");
      output.println("{");
      output.println("  "+getOutputStreamName()+".println(str);");
      output.println("}");
      output.println("protected void printIndent()");
      output.println("{");
      output.println("  for( int i =0; i < indent; i++) {"+getOutputStreamName()+".print(' ');}");
      output.println("}");
    }
    protected void emitClassDocComment(PrintWriter output)
    {
      output.println("/** <P> Composable pipeline which wraps an underlying {@link GL} implementation,");
      output.println("    providing tracing information to a user-specified {@link java.io.PrintStream}");
      output.println("    before and after each OpenGL method call. Sample code which installs this pipeline: </P>");
      output.println();
      output.println("<PRE>");
      output.println("     drawable.setGL(new TraceGL(drawable.getGL(), System.err));");
      output.println("</PRE>");
      output.println("*/");
    }
    
    protected void preDownstreamCallHook(PrintWriter output, Method m)
    {
      Class[] params = m.getParameterTypes();
      if ( m.getName().equals("glEnd") || m.getName().equals("glEndList")) 
      {
        output.println("indent-=2;");
        output.println("    printIndent();");
      } 
      else 
      {
        output.println("printIndent();");
      }
      
      output.print("    print(\"" + m.getName() + "(\"");
      for ( int i =0; i < params.length; i++ ) 
      {
        if ( params[i].isArray() )
          output.print("+dumpArray(arg"+i+")");
        else
          output.print("+arg"+i);
        if ( i < params.length-1)
          output.print("+\",\"");      
      }
      output.println("+\")\");");
      output.print("    ");
    }

    protected void postDownstreamCallHook(PrintWriter output, Method m)
    {
      Class ret = m.getReturnType();
      if ( ret != Void.TYPE ) 
      {
        output.println("    println(\" = \"+_res);"); 
      }
      else 
      {
        output.println("    println(\"\");");
      }
    }

    private String getOutputStreamName() {
      return "stream";
    }

  } // end class TracePipeline
}

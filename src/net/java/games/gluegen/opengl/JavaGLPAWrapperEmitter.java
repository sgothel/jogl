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
import net.java.games.gluegen.*;
import net.java.games.gluegen.cgram.types.*;

public class JavaGLPAWrapperEmitter extends JavaMethodBindingImplEmitter
{
  private static final CommentEmitter commentEmitterForWrappedMethod =
    new WrappedMethodCommentEmitter();

  private JavaMethodBindingEmitter emitterBeingWrapped;
  private String getProcAddressTableExpr;
  
  public JavaGLPAWrapperEmitter(JavaMethodBindingEmitter methodToWrap, String getProcAddressTableExpr)
  {    
    super(methodToWrap.getBinding(), methodToWrap.getDefaultOutput(), methodToWrap.getRuntimeExceptionType());
    this.getProcAddressTableExpr = getProcAddressTableExpr;

    if (methodToWrap.getBinding().hasContainingType())
    {
      throw new IllegalArgumentException(
        "Cannot create OpenGL proc. address wrapper; method has containing type: \"" +
        methodToWrap.getBinding() + "\"");
    }

    // make a new emitter that will emit the original method's binding, but
    // with WRAP_PREFIX before its name. If a body is needed (for array
    // length checking, unwrapping of wrapper objects to java.nio.Buffers,
    // etc.) then it will be generated; therefore the emitter being wrapped
    // should be an "NIO buffer variant" (i.e., after all unpacking has
    // occurred).
    emitterBeingWrapped =
      new JavaMethodBindingEmitter(methodToWrap.getBinding().createNIOBufferVariant(),
                                   methodToWrap.getDefaultOutput(),
                                   methodToWrap.getRuntimeExceptionType())
      {
        protected void emitName(PrintWriter writer)
        {
          writer.print(GLEmitter.WRAP_PREFIX);
          super.emitName(writer);
        }       
        protected int emitArguments(PrintWriter writer)
        {
          int numEmitted = super.emitArguments(writer);
          if (numEmitted > 0)
          {
            writer.print(", ");
          }
          writer.print("long glProcAddress");
          ++numEmitted;
          
          return numEmitted;
        }
      };
        
    // copy the modifiers from the original emitter
    emitterBeingWrapped.addModifiers(methodToWrap.getModifiers());
    
    // Change the access of the method we're wrapping to PRIVATE
    EmissionModifier origAccess = null; // null is equivalent if package access
    if (emitterBeingWrapped.hasModifier(PUBLIC))
    {
      origAccess = PUBLIC;
    }
    else if (emitterBeingWrapped.hasModifier(PROTECTED))
    {
      origAccess = PROTECTED;
    }
    else if (emitterBeingWrapped.hasModifier(PRIVATE))
    {
      origAccess = PRIVATE;
    }

    if (origAccess != null)
    {
      emitterBeingWrapped.removeModifier(origAccess);
    }
    emitterBeingWrapped.addModifier(PRIVATE);
    emitterBeingWrapped.addModifier(NATIVE);

    // Now make our binding use the original access of the wrapped method
    this.addModifier(origAccess);
    if (emitterBeingWrapped.hasModifier(STATIC)) {
      this.addModifier(STATIC);
    }
  }

  protected boolean needsBody() {
    return true;
  }

  protected String getImplMethodName() {
    return GLEmitter.WRAP_PREFIX + getBinding().getName();
  }

  public void emit(PrintWriter writer)
  {
    // Emit a wrapper that will call the method we want to wrap
    //writer.println("  // Emitter being wrapped = " + emitterBeingWrapped.getClass().getName());
    super.emit(writer);
    writer.println();
    
    // emit the wrapped method
    CommentEmitter origComment = emitterBeingWrapped.getCommentEmitter();
    emitterBeingWrapped.setCommentEmitter(commentEmitterForWrappedMethod);
    emitterBeingWrapped.emit(writer);
    emitterBeingWrapped.setCommentEmitter(origComment);
    writer.println();    
  }

  protected void emitPreCallSetup(MethodBinding binding, PrintWriter writer) {
    super.emitPreCallSetup(binding, writer);

    MethodBinding wrappedBinding = emitterBeingWrapped.getBinding();
    String procAddressVariable =
      GLEmitter.PROCADDRESS_VAR_PREFIX + wrappedBinding.getName();

    writer.println("    final long __addr_ = " + getProcAddressTableExpr + "." + procAddressVariable + ";");
    writer.println("    if (__addr_ == 0) {");
    writer.println("      throw new GLException(\"Method \\\"" + binding.getName() + "\\\" not available\");");
    writer.println("    }");
  }

  protected int emitCallArguments(MethodBinding binding, PrintWriter writer) {
    int numEmitted = super.emitCallArguments(binding, writer);
    if (numEmitted > 0) {
      writer.print(", ");
    }
    writer.print("__addr_");
    return 1 + numEmitted;
  }

  /** This class emits the comment for the wrapper method */
  private static class WrappedMethodCommentEmitter extends JavaMethodBindingEmitter.DefaultCommentEmitter {
    protected void emitBeginning(FunctionEmitter methodEmitter, PrintWriter writer) {
      writer.print("Encapsulates function pointer for OpenGL function <br>: ");
    }
  }
} // end class JavaGLPAWrapperEmitter

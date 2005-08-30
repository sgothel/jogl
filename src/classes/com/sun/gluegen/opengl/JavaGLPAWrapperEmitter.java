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
import com.sun.gluegen.*;
import com.sun.gluegen.cgram.types.*;

public class JavaGLPAWrapperEmitter extends JavaMethodBindingEmitter {
  private final CommentEmitter commentEmitterForWrappedMethod =
    new WrappedMethodCommentEmitter();

  private boolean changeNameAndArguments;
  private String getProcAddressTableExpr;
  
  public JavaGLPAWrapperEmitter(JavaMethodBindingEmitter methodToWrap,
                                String getProcAddressTableExpr,
                                boolean changeNameAndArguments) {
    super(methodToWrap);
    this.changeNameAndArguments = changeNameAndArguments;
    this.getProcAddressTableExpr = getProcAddressTableExpr;
    setCommentEmitter(new WrappedMethodCommentEmitter());

    if (methodToWrap.getBinding().hasContainingType())
    {
      throw new IllegalArgumentException(
        "Cannot create OpenGL proc. address wrapper; method has containing type: \"" +
        methodToWrap.getBinding() + "\"");
    }
  }

  public String getName() {
    String res = super.getName();
    if (changeNameAndArguments) {
      return GLEmitter.WRAP_PREFIX + res;
    }
    return res;
  }

  protected int emitArguments(PrintWriter writer) {
    int numEmitted = super.emitArguments(writer);
    if (changeNameAndArguments) {
      if (numEmitted > 0) {
        writer.print(", ");
      }

      writer.print("long glProcAddress");
      ++numEmitted;
    }
          
    return numEmitted;
  }

  protected String getImplMethodName(boolean direct) {
    return GLEmitter.WRAP_PREFIX + super.getImplMethodName(direct);
  }

  protected void emitPreCallSetup(MethodBinding binding, PrintWriter writer) {
    super.emitPreCallSetup(binding, writer);

    String procAddressVariable =
      GLEmitter.PROCADDRESS_VAR_PREFIX + binding.getName();
    writer.println("    final long __addr_ = " + getProcAddressTableExpr + "." + procAddressVariable + ";");
    writer.println("    if (__addr_ == 0) {");
    writer.println("      throw new GLException(\"Method \\\"" + binding.getName() + "\\\" not available\");");
    writer.println("    }");
  }

  protected int emitCallArguments(MethodBinding binding, PrintWriter writer, boolean indirect) {
    int numEmitted = super.emitCallArguments(binding, writer, indirect);
    if (numEmitted > 0) {
      writer.print(", ");
    }
    writer.print("__addr_");
    return 1 + numEmitted;
  }

  /** This class emits the comment for the wrapper method */
  private class WrappedMethodCommentEmitter extends JavaMethodBindingEmitter.DefaultCommentEmitter {
    protected void emitBeginning(FunctionEmitter methodEmitter, PrintWriter writer) {
      writer.print("Encapsulates function pointer for OpenGL function <br>: ");
    }
  }
} // end class JavaGLPAWrapperEmitter

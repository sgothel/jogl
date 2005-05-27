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

package net.java.games.gluegen;

import java.util.*;
import java.io.*;
import java.text.MessageFormat;

public class CMethodBindingImplEmitter extends CMethodBindingEmitter
{
  protected static final CommentEmitter defaultCImplCommentEmitter =
    new CImplCommentEmitter();

  public CMethodBindingImplEmitter(MethodBinding binding,
                                   boolean isOverloadedBinding,
                                   String javaPackageName,
                                   String javaClassName,
                                   boolean isJavaMethodStatic,
                                   PrintWriter output)
  {
    super(binding, isOverloadedBinding,
          javaPackageName, javaClassName,
          isJavaMethodStatic, output);
    setCommentEmitter(defaultCImplCommentEmitter);
  }

  protected void emitName(PrintWriter writer)
  {
    super.emitName(writer);
    if (!getIsOverloadedBinding()) {
      writer.print("0");
    }
  }

  /**
   * Gets the mangled name for the binding, but assumes that this is an Impl
   * routine
   */
  protected String jniMangle(MethodBinding binding) {
    StringBuffer buf = new StringBuffer();
    buf.append(jniMangle(binding.getName()));
    buf.append("0");
    buf.append("__");
    for (int i = 0; i < binding.getNumArguments(); i++) {
      JavaType type = binding.getJavaArgumentType(i);
      Class c = type.getJavaClass();
      if (c != null) {
        jniMangle(c, buf);
      } else {
        // FIXME: add support for char* -> String conversion
        throw new RuntimeException("Unknown kind of JavaType: name="+type.getName());
      }
    }
    return buf.toString();
  }

  protected static class CImplCommentEmitter extends CMethodBindingEmitter.DefaultCommentEmitter {
    protected void emitBeginning(FunctionEmitter methodEmitter, PrintWriter writer) {
      writer.print(" -- FIXME: PUT A COMMENT HERE -- ");
    }
  }
}

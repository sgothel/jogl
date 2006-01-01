/*
 * Copyright (c) 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.gluegen.procaddress;

import java.io.*;
import java.util.*;
import com.sun.gluegen.*;
import com.sun.gluegen.cgram.types.*;

public class ProcAddressCMethodBindingEmitter extends CMethodBindingEmitter {
  private boolean callThroughProcAddress;
  private static String procAddressJavaTypeName =
    JavaType.createForClass(Long.TYPE).jniTypeName();
  private ProcAddressEmitter emitter;

  public ProcAddressCMethodBindingEmitter(CMethodBindingEmitter methodToWrap,
                                          final boolean callThroughProcAddress,
                                          ProcAddressEmitter emitter) {
    super(
      new MethodBinding(methodToWrap.getBinding()) {
        public String getName() {
          if (callThroughProcAddress) {
            return ProcAddressEmitter.WRAP_PREFIX + super.getName();
          } else {
            return super.getName();
          }
        }

        public String getRenamedMethodName() {
          if (callThroughProcAddress) {
            return ProcAddressEmitter.WRAP_PREFIX + super.getRenamedMethodName();
          } else {
            return super.getRenamedMethodName();
          }
        }
      },
      methodToWrap.getDefaultOutput(),
      methodToWrap.getJavaPackageName(),
      methodToWrap.getJavaClassName(),
      methodToWrap.getIsOverloadedBinding(),
      methodToWrap.getIsJavaMethodStatic(),
      true,
      methodToWrap.forIndirectBufferAndArrayImplementation()
    );

    if (methodToWrap.getReturnValueCapacityExpression() != null) {
      setReturnValueCapacityExpression(methodToWrap.getReturnValueCapacityExpression());
    }
    if (methodToWrap.getReturnValueLengthExpression() != null) {
      setReturnValueLengthExpression(methodToWrap.getReturnValueLengthExpression());
    }
    setTemporaryCVariableDeclarations(methodToWrap.getTemporaryCVariableDeclarations());
    setTemporaryCVariableAssignments (methodToWrap.getTemporaryCVariableAssignments ());
    
    setCommentEmitter(defaultCommentEmitter);
    this.callThroughProcAddress = callThroughProcAddress;
    this.emitter = emitter;
  }
  
  protected int emitArguments(PrintWriter writer) {
    int numEmitted = super.emitArguments(writer);
    if (callThroughProcAddress) {
      if (numEmitted > 0)
        {
          writer.print(", ");
        }
      writer.print(procAddressJavaTypeName);
      writer.print(" procAddress");
      ++numEmitted;
    }

    return numEmitted;
  }

  protected void emitBodyVariableDeclarations(PrintWriter writer) {
    if (callThroughProcAddress) {
      // create variable for the function pointer with the right type, and set
      // it to the value of the passed-in glProcAddress 
      FunctionSymbol cSym = getBinding().getCSymbol();
      String funcPointerTypedefName =
        emitter.getFunctionPointerTypedefName(cSym);
    
      writer.print("  ");
      writer.print(funcPointerTypedefName);
      writer.print(" ptr_");
      writer.print(cSym.getName());
      writer.println(";");
    }

    super.emitBodyVariableDeclarations(writer);
  }

  protected void emitBodyVariablePreCallSetup(PrintWriter writer,
                                              boolean emittingPrimitiveArrayCritical) {
    super.emitBodyVariablePreCallSetup(writer, emittingPrimitiveArrayCritical);

    if (callThroughProcAddress) {
      if (!emittingPrimitiveArrayCritical) {
        // set the function pointer to the value of the passed-in glProcAddress
        FunctionSymbol cSym = getBinding().getCSymbol();
        String funcPointerTypedefName = emitter.getFunctionPointerTypedefName(cSym);

        String ptrVarName = "ptr_" + cSym.getName();
    
        writer.print("  ");
        writer.print(ptrVarName);
        writer.print(" = (");
        writer.print(funcPointerTypedefName);
        writer.println(") (intptr_t) procAddress;");

        writer.println("  assert(" + ptrVarName + " != NULL);");
      }
    }
  }

  protected void emitBodyCallCFunction(PrintWriter writer) {
    if (!callThroughProcAddress) {
      super.emitBodyCallCFunction(writer);
    } else {
      // Make the call to the actual C function
      writer.print("  ");

      // WARNING: this code assumes that the return type has already been
      // typedef-resolved.
      Type cReturnType = binding.getCReturnType();

      if (!cReturnType.isVoid()) {
        writer.print("_res = ");
      }
      MethodBinding binding = getBinding();
      if (binding.hasContainingType()) {
        // FIXME: this can and should be handled and unified with the
        // associated code in the CMethodBindingEmitter
        throw new IllegalStateException("Cannot call through function pointer because binding has containing type: " + binding);
      }

      // call throught the run-time function pointer
      writer.print("(* ptr_");
      writer.print(binding.getCSymbol().getName());
      writer.print(") ");
      writer.print("(");
      emitBodyPassCArguments(writer);
      writer.println(");");
    }
  }

  protected String jniMangle(MethodBinding binding) {
    StringBuffer buf = new StringBuffer(super.jniMangle(binding));
    if (callThroughProcAddress) {
      jniMangle(Long.TYPE, buf, false);  // to account for the additional _addr_ parameter
    }
    return buf.toString();
  }
}

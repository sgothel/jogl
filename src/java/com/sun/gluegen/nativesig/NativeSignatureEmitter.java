/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.gluegen.nativesig;

import java.io.*;
import java.util.*;

import com.sun.gluegen.*;
import com.sun.gluegen.cgram.types.*;
import com.sun.gluegen.opengl.*;
import com.sun.gluegen.procaddress.*;

/** Emitter producing NativeSignature attributes. */

public class NativeSignatureEmitter extends GLEmitter {
  protected List generateMethodBindingEmitters(FunctionSymbol sym) throws Exception {
    // Allow superclass to do most of the work for us
    List res = super.generateMethodBindingEmitters(sym);

    // Filter out all non-JavaMethodBindingEmitters
    for (Iterator iter = res.iterator(); iter.hasNext(); ) {
      FunctionEmitter emitter = (FunctionEmitter) iter.next();
      if (!(emitter instanceof JavaMethodBindingEmitter)) {
        iter.remove();
      }
    }

    if (res.isEmpty()) {
      return res;
    }

    PrintWriter writer = (getConfig().allStatic() ? javaWriter() : javaImplWriter());

    List processed = new ArrayList();

    // First, filter out all emitters going to the "other" (public) writer
    for (Iterator iter = res.iterator(); iter.hasNext(); ) {
      FunctionEmitter emitter = (FunctionEmitter) iter.next();
      if (emitter.getDefaultOutput() != writer) {
        processed.add(emitter);
        iter.remove();
      }
    }

    // Now process all of the remaining emitters sorted by MethodBinding
    while (!res.isEmpty()) {
      List emittersForBinding = new ArrayList();
      JavaMethodBindingEmitter emitter = (JavaMethodBindingEmitter) res.remove(0);
      emittersForBinding.add(emitter);
      MethodBinding binding = emitter.getBinding();
      for (Iterator iter = res.iterator(); iter.hasNext(); ) {
        JavaMethodBindingEmitter emitter2 = (JavaMethodBindingEmitter) iter.next();
        if (emitter2.getBinding() == binding) {
          emittersForBinding.add(emitter2);
          iter.remove();
        }
      }
      generateNativeSignatureEmitters(binding, emittersForBinding);
      processed.addAll(emittersForBinding);
    }

    return processed;
  }

  protected void generateNativeSignatureEmitters(MethodBinding binding,
                                                 List allEmitters) {
    if (allEmitters.isEmpty()) {
      return;
    }

    PrintWriter writer = (getConfig().allStatic() ? javaWriter() : javaImplWriter());
    
    // Give ourselves the chance to interpose on the generation of all code to keep things simple
    List newEmitters = new ArrayList();
    for (Iterator iter = allEmitters.iterator(); iter.hasNext(); ) {
      JavaMethodBindingEmitter javaEmitter = (JavaMethodBindingEmitter) iter.next();
      NativeSignatureJavaMethodBindingEmitter newEmitter = null;
      if (javaEmitter instanceof GLJavaMethodBindingEmitter) {
        newEmitter = new NativeSignatureJavaMethodBindingEmitter((GLJavaMethodBindingEmitter) javaEmitter);
      } else if (javaEmitter instanceof ProcAddressJavaMethodBindingEmitter) {
        newEmitter = new NativeSignatureJavaMethodBindingEmitter((ProcAddressJavaMethodBindingEmitter) javaEmitter);
      } else {
        newEmitter = new NativeSignatureJavaMethodBindingEmitter(javaEmitter, this);
      }
      newEmitters.add(newEmitter);
    }
    allEmitters.clear();
    allEmitters.addAll(newEmitters);

    // Detect whether we need to produce more or modify some of these emitters.
    // Note that at this point we are assuming that generatePublicEmitters has
    // been called with signatureOnly both true and false.
    if (signatureContainsStrings(binding) &&
        !haveEmitterWithBody(allEmitters)) {
      // This basically handles glGetString but also any similar methods
      NativeSignatureJavaMethodBindingEmitter javaEmitter = findEmitterWithWriter(allEmitters, writer);

      // First, we need to clone this emitter to produce the native
      // entry point
      NativeSignatureJavaMethodBindingEmitter emitter =
        new NativeSignatureJavaMethodBindingEmitter(javaEmitter);
      emitter.removeModifier(JavaMethodBindingEmitter.PUBLIC);
      emitter.addModifier(JavaMethodBindingEmitter.PRIVATE);
      emitter.setForImplementingMethodCall(true);
      // Note: this is chosen so we don't have to change the logic in
      // emitReturnVariableSetupAndCall which decides which variant
      // (direct / indirect) to call
      emitter.setForDirectBufferImplementation(true);
      allEmitters.add(emitter);

      // Now make the original emitter non-native and cause it to emit a body
      javaEmitter.removeModifier(JavaMethodBindingEmitter.NATIVE);
      javaEmitter.setEmitBody(true);
    }
  }

  protected boolean signatureContainsStrings(MethodBinding binding) {
    for (int i = 0; i < binding.getNumArguments(); i++) {
      JavaType type = binding.getJavaArgumentType(i);
      if (type.isString() || type.isStringArray()) {
        return true;
      }
    }
    JavaType retType = binding.getJavaReturnType();
    if (retType.isString() || retType.isStringArray()) {
      return true;
    }
    return false;
  }

  protected boolean haveEmitterWithBody(List allEmitters) {
    for (Iterator iter = allEmitters.iterator(); iter.hasNext(); ) {
      JavaMethodBindingEmitter emitter = (JavaMethodBindingEmitter) iter.next();
      if (!emitter.signatureOnly()) {
        return true;
      }
    }
    return false;
  }

  protected NativeSignatureJavaMethodBindingEmitter findEmitterWithWriter(List allEmitters, PrintWriter writer) {
    for (Iterator iter = allEmitters.iterator(); iter.hasNext(); ) {
      NativeSignatureJavaMethodBindingEmitter emitter =
        (NativeSignatureJavaMethodBindingEmitter) iter.next();
      if (emitter.getDefaultOutput() == writer) {
        return emitter;
      }
    }
    throw new RuntimeException("Unexpectedly failed to find an emitter with the given writer");
  }    
}

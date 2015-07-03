/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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
package com.jogamp.gluegen.opengl.nativesig;

import com.jogamp.gluegen.FunctionEmitter;
import com.jogamp.gluegen.JavaMethodBindingEmitter;
import com.jogamp.gluegen.JavaType;
import com.jogamp.gluegen.MethodBinding;
import com.jogamp.gluegen.cgram.types.FunctionSymbol;
import com.jogamp.gluegen.opengl.GLEmitter;
import com.jogamp.gluegen.opengl.GLJavaMethodBindingEmitter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Emitter producing NativeSignature attributes.
 *
 * Review: This Package/Class is not used and subject to be deleted.
 */
public class NativeSignatureEmitter extends GLEmitter {

    @Override
    protected List<? extends FunctionEmitter> generateMethodBindingEmitters(final FunctionSymbol sym) throws Exception {

        // Allow superclass to do most of the work for us
        final List<? extends FunctionEmitter> res = super.generateMethodBindingEmitters(sym);

        // Filter out all non-JavaMethodBindingEmitters
        for (final Iterator<? extends FunctionEmitter> iter = res.iterator(); iter.hasNext();) {
            final FunctionEmitter emitter = iter.next();
            if (!(emitter instanceof JavaMethodBindingEmitter)) {
                iter.remove();
            }
        }

        if (res.isEmpty()) {
            return res;
        }

        final PrintWriter writer = (getConfig().allStatic() ? javaWriter() : javaImplWriter());

        final List<FunctionEmitter> processed = new ArrayList<FunctionEmitter>();

        // First, filter out all emitters going to the "other" (public) writer
        for (final Iterator<? extends FunctionEmitter> iter = res.iterator(); iter.hasNext();) {
            final FunctionEmitter emitter = iter.next();
            if (emitter.getDefaultOutput() != writer) {
                processed.add(emitter);
                iter.remove();
            }
        }

        // Now process all of the remaining emitters sorted by MethodBinding
        while (!res.isEmpty()) {
            final List<JavaMethodBindingEmitter> emittersForBinding = new ArrayList<JavaMethodBindingEmitter>();
            final JavaMethodBindingEmitter emitter = (JavaMethodBindingEmitter) res.remove(0);
            emittersForBinding.add(emitter);
            final MethodBinding binding = emitter.getBinding();
            for (final Iterator<? extends FunctionEmitter> iter = res.iterator(); iter.hasNext();) {
                final JavaMethodBindingEmitter emitter2 = (JavaMethodBindingEmitter) iter.next();
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

    protected void generateNativeSignatureEmitters(final MethodBinding binding, final List<JavaMethodBindingEmitter> allEmitters) {

        if (allEmitters.isEmpty()) {
            return;
        }

        final PrintWriter writer = (getConfig().allStatic() ? javaWriter() : javaImplWriter());

        // Give ourselves the chance to interpose on the generation of all code to keep things simple
        final List<JavaMethodBindingEmitter> newEmitters = new ArrayList<JavaMethodBindingEmitter>();
        for (final JavaMethodBindingEmitter javaEmitter : allEmitters) {
            NativeSignatureJavaMethodBindingEmitter newEmitter = null;
            if (javaEmitter instanceof GLJavaMethodBindingEmitter) {
                newEmitter = new NativeSignatureJavaMethodBindingEmitter((GLJavaMethodBindingEmitter) javaEmitter);
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
        if (signatureContainsStrings(binding) && !haveEmitterWithBody(allEmitters)) {
            // This basically handles glGetString but also any similar methods
            final NativeSignatureJavaMethodBindingEmitter javaEmitter = findEmitterWithWriter(allEmitters, writer);

            // First, we need to clone this emitter to produce the native
            // entry point
            final NativeSignatureJavaMethodBindingEmitter emitter = new NativeSignatureJavaMethodBindingEmitter(javaEmitter);
            emitter.removeModifier(JavaMethodBindingEmitter.PUBLIC);
            emitter.addModifier(JavaMethodBindingEmitter.PRIVATE);
            emitter.setPrivateNativeMethod(true);
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

    protected boolean signatureContainsStrings(final MethodBinding binding) {
        for (int i = 0; i < binding.getNumArguments(); i++) {
            final JavaType type = binding.getJavaArgumentType(i);
            if (type.isString() || type.isStringArray()) {
                return true;
            }
        }
        final JavaType retType = binding.getJavaReturnType();
        if (retType.isString() || retType.isStringArray()) {
            return true;
        }
        return false;
    }

    protected boolean haveEmitterWithBody(final List<JavaMethodBindingEmitter> allEmitters) {
        for (final JavaMethodBindingEmitter emitter : allEmitters) {
            if (!emitter.signatureOnly()) {
                return true;
            }
        }
        return false;
    }

    protected NativeSignatureJavaMethodBindingEmitter findEmitterWithWriter(final List<JavaMethodBindingEmitter> allEmitters, final PrintWriter writer) {
        for (final JavaMethodBindingEmitter jemitter : allEmitters) {
            final NativeSignatureJavaMethodBindingEmitter emitter = (NativeSignatureJavaMethodBindingEmitter)jemitter;
            if (emitter.getDefaultOutput() == writer) {
                return emitter;
            }
        }
        throw new RuntimeException("Unexpectedly failed to find an emitter with the given writer");
    }
}

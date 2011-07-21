/*
 * Copyright (c) 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
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
package com.jogamp.gluegen.opengl;

import com.jogamp.gluegen.CommentEmitter;
import com.jogamp.gluegen.JavaEmitter;
import com.jogamp.gluegen.JavaMethodBindingEmitter;
import com.jogamp.gluegen.MethodBinding;
import com.jogamp.gluegen.cgram.types.Type;
import com.jogamp.gluegen.procaddress.ProcAddressJavaMethodBindingEmitter;

import java.io.PrintWriter;

/** A specialization of the proc address emitter which knows how to
change argument names to take into account Vertex Buffer Object /
Pixel Buffer Object variants. */
public class GLJavaMethodBindingEmitter extends ProcAddressJavaMethodBindingEmitter {

    protected boolean bufferObjectVariant;
    protected GLEmitter glEmitter;
    protected CommentEmitter glCommentEmitter = new GLCommentEmitter();

    public GLJavaMethodBindingEmitter(JavaMethodBindingEmitter methodToWrap, boolean callThroughProcAddress,
            String getProcAddressTableExpr, boolean changeNameAndArguments, boolean bufferObjectVariant, GLEmitter emitter) {

        super(methodToWrap, callThroughProcAddress, getProcAddressTableExpr, changeNameAndArguments, emitter);
        this.bufferObjectVariant = bufferObjectVariant;
        this.glEmitter = emitter;
        setCommentEmitter(glCommentEmitter);
    }

    public GLJavaMethodBindingEmitter(ProcAddressJavaMethodBindingEmitter methodToWrap, GLEmitter emitter, boolean bufferObjectVariant) {
        super(methodToWrap);
        this.bufferObjectVariant = bufferObjectVariant;
        this.glEmitter = emitter;
        setCommentEmitter(glCommentEmitter);
    }

    public GLJavaMethodBindingEmitter(GLJavaMethodBindingEmitter methodToWrap) {
        this(methodToWrap, methodToWrap.glEmitter, methodToWrap.bufferObjectVariant);
    }

    @Override
    protected String getArgumentName(int i) {
        String name = super.getArgumentName(i);

        if (!bufferObjectVariant) {
            return name;
        }

        // Emitters for VBO/PBO-related routines change the outgoing
        // argument name for the buffer
        if (binding.getJavaArgumentType(i).isLong()) {
            Type cType = binding.getCArgumentType(i);
            Type targetType = cType.asPointer().getTargetType();
            if (cType.isPointer() && (targetType.isVoid() || targetType.isPrimitive())) {
                return name + "_buffer_offset";
            }
        }

        return name;
    }

    protected class GLCommentEmitter extends JavaMethodBindingEmitter.DefaultCommentEmitter {

        @Override
        protected void emitBindingCSignature(MethodBinding binding, PrintWriter writer) {

            super.emitBindingCSignature(binding, writer);

            String symbolRenamed = binding.getName();
            StringBuilder newComment = new StringBuilder();

            newComment.append("<br>Part of <code>");
            if (0 == glEmitter.addExtensionsOfSymbols2Buffer(newComment, ", ", symbolRenamed, binding.getAliasedNames())) {
                if (glEmitter.getGLConfig().getAllowNonGLExtensions()) {
                    newComment.append("CORE FUNC");
                } else {
                    StringBuilder sb = new StringBuilder();
                    JavaEmitter.addStrings2Buffer(sb, ", ", symbolRenamed, binding.getAliasedNames());
                    RuntimeException ex = new RuntimeException("Couldn't find extension to: " + binding + " ; " + sb.toString());
                    glEmitter.getGLConfig().getGLInfo().dump();
                    // glEmitter.getGLConfig().dumpRenames();
                    throw ex;
                }
            }
            newComment.append("</code>");
            writer.print(newComment.toString());
        }
    }
}

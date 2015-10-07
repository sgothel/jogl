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

import static java.util.logging.Level.INFO;

import com.jogamp.gluegen.ConstantDefinition;
import com.jogamp.gluegen.FunctionEmitter;
import com.jogamp.gluegen.GlueEmitterControls;
import com.jogamp.gluegen.GlueGen;
import com.jogamp.gluegen.JavaConfiguration;
import com.jogamp.gluegen.JavaEmitter;
import com.jogamp.gluegen.JavaMethodBindingEmitter;
import com.jogamp.gluegen.JavaType;
import com.jogamp.gluegen.MethodBinding;
import com.jogamp.gluegen.SymbolFilter;
import com.jogamp.gluegen.cgram.types.FunctionSymbol;
import com.jogamp.gluegen.cgram.types.Type;
import com.jogamp.gluegen.procaddress.ProcAddressEmitter;
import com.jogamp.gluegen.procaddress.ProcAddressJavaMethodBindingEmitter;
import com.jogamp.gluegen.runtime.opengl.GLNameResolver;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A subclass of ProcAddressEmitter with special OpenGL-specific
 * configuration abilities.
 */
public class GLEmitter extends ProcAddressEmitter {

    // Keeps track of which MethodBindings were created for handling
    // Buffer Object variants. Used as a Set rather than a Map.
    private final Map<MethodBinding, MethodBinding> bufferObjectMethodBindings = new IdentityHashMap<MethodBinding, MethodBinding>();

    enum BufferObjectKind { UNPACK_PIXEL, PACK_PIXEL, ARRAY, ELEMENT, INDIRECT}

    @Override
    public void beginEmission(final GlueEmitterControls controls) throws IOException {
        getGLConfig().parseGLSemHeaders(controls);
        if( null == getGLConfig().getGLSemInfo() ) {
            throw new RuntimeException("No 'GLSemHeader' defined.");
        }
        getGLConfig().parseGLDocHeaders(controls);
        if( null == getGLConfig().getGLDocInfo() ) {
            throw new InternalError("XXX"); // since GLDocHeader contains all GLSemHeader ..
        }
        renameExtensionsIntoCore();
        if ( getGLConfig().getAutoUnifyExtensions() ) {
            unifyExtensions(controls);
        }
        super.beginEmission(controls);
    }

    protected void renameExtensionsIntoCore() {
        final GLConfiguration config = getGLConfig();
        renameExtensionsIntoCore(config, config.getGLSemInfo(), true);
        renameExtensionsIntoCore(config, config.getGLDocInfo(), false);
    }
    protected void renameExtensionsIntoCore(final GLConfiguration config, final BuildStaticGLInfo glInfo, final boolean isSemHeader) {
        // This method handles renaming of entire extensions into the
        // OpenGL core namespace. For example, it is used to move certain
        // OpenGL ES (OES) extensions into the core namespace which are
        // already in the core namespace in desktop OpenGL. It builds upon
        // renaming mechanisms that are built elsewhere.

        final String headerType = isSemHeader ? "GLSemHeader" : "GLDocHeader";
        final Set<String> extensionSet = isSemHeader ? config.getExtensionsRenamedIntoCore() : glInfo.getExtensions();

        for (final String extension : extensionSet) {
            if( isSemHeader && config.isIgnoredExtension(extension) ) {
                LOG.log(INFO, "<RenameExtensionIntoCore: {0} IGNORED {1}>", extension, headerType);
            } else {
                LOG.log(INFO, "<RenameExtensionIntoCore: {0} BEGIN {1}", extension, headerType);
                final Set<String> declarations = glInfo.getDeclarations(extension);
                if (declarations != null) {
                    for (final Iterator<String> iterator = declarations.iterator(); iterator.hasNext();) {
                        final String decl = iterator.next();
                        final boolean isGLFunction = GLNameResolver.isGLFunction(decl);
                        boolean isGLEnumeration = false;
                        if (!isGLFunction) {
                            isGLEnumeration = GLNameResolver.isGLEnumeration(decl);
                        }
                        if (isGLFunction || isGLEnumeration) {
                            final String renamed = GLNameResolver.normalize(decl, isGLFunction);
                            if (!renamed.equals(decl)) {
                                if( isSemHeader ) {
                                    // Sem + Doc
                                    config.addJavaSymbolRename(decl, renamed);
                                } else {
                                    // Doc only
                                    config.addJavaDocSymbolRename(decl, renamed);
                                }
                            }
                        }
                    }
                }
                LOG.log(INFO, "RenameExtensionIntoCore: {0} END>", extension, headerType);
            }
        }
    }

    class ExtensionUnifier implements SymbolFilter {

        private List<ConstantDefinition> constants;
        private List<FunctionSymbol> functions;

        @Override
        public List<ConstantDefinition> getConstants() {
            return constants;
        }
        @Override
        public List<FunctionSymbol> getFunctions() {
            return functions;
        }

        @Override
        public void filterSymbols(final List<ConstantDefinition> inConstList,
                                  final List<FunctionSymbol> inFuncList) {
            final BuildStaticGLInfo glInfo = getGLConfig().getGLSemInfo();
            if (glInfo == null) {
                return;
            }
            // Try to retain a "good" ordering for these symbols
            final Map<String, ConstantDefinition> constantMap = new LinkedHashMap<String, ConstantDefinition>();
            for (final ConstantDefinition def : inConstList) {
                constantMap.put(def.getName(), def);
            }
            final Map<String, FunctionSymbol> functionMap = new LinkedHashMap<String, FunctionSymbol>();
            for (final FunctionSymbol sym : inFuncList) {
                functionMap.put(sym.getName(), sym);
            }

            // Go through all of the declared extensions.
            // For each extension, look at its #define and function symbols.
            // If we find all of the extension's symbols in the core API under
            // non-ARB (or whatever is the suffix) names, then remove this extension
            // from the public API. If it turns out that we are running on hardware
            // that doesn't support the core version of these APIs, the runtime
            // will take care of looking up the extension version of these entry
            // points.
            final Set<String> extensionNames = glInfo.getExtensions();

            for (final String extension : extensionNames) {
                final Set<String> declarations = glInfo.getDeclarations(extension);
                boolean isExtension = true;
                boolean shouldUnify = true;
                String cause = null;
                for (final String decl : declarations) {
                    final boolean isFunc = !decl.startsWith("GL_");
                    if (!GLNameResolver.isExtension(decl, isFunc)) {
                        isExtension = false;
                        break;
                    }
                    // See whether we're emitting glue code for this
                    // entry point or definition at all
                    if (isFunc) {
                        if (!functionMap.containsKey(decl)) {
                            isExtension = false;
                            break;
                        }
                    } else {
                        if (!constantMap.containsKey(decl)) {
                            isExtension = false;
                            break;
                        }
                    }
                    cause = decl;
                    final String unifiedName = GLNameResolver.normalize(decl, isFunc);
                    // NOTE that we look up the unified name in the
                    // BuildStaticGLInfo's notion of the APIs -- since
                    // we might not be emitting glue code for the
                    // headers that actually contain the core entry
                    // point. Think of the case where we are parsing the
                    // GLES2 gl2.h, which contains certain desktop
                    // OpenGL extensions that have been moved into the
                    // core, but later generating the implementing glue
                    // code (not the interface) for the desktop gl.h /
                    // glext.h.
                    shouldUnify = (glInfo.getExtension(unifiedName) != null);
                    //                  if (isFunc) {
                    //                      shouldUnify = functionMap.containsKey(unifiedName);
                    //                  } else {
                    //                      shouldUnify = constantMap.containsKey(unifiedName);
                    //                  }
                    if (!shouldUnify) {
                        break;
                    }
                }
                if (isExtension) {
                    if (shouldUnify) {
                        for (final String decl : declarations) {
                            final boolean isFunc = !decl.startsWith("GL_");
                            if (isFunc) {
                                functionMap.remove(decl);
                            } else {
                                constantMap.remove(decl);
                            }
                        }
                        System.err.println("INFO: unified extension " + extension + " into core API");
                    } else {
                        System.err.println("INFO: didn't unify extension " + extension + " into core API because of " + cause);
                    }
                }
            }
            constants = new ArrayList<ConstantDefinition>(constantMap.values());
            functions = new ArrayList<FunctionSymbol>(functionMap.values());
        }
    }

    private void unifyExtensions(final GlueEmitterControls controls) {
        controls.runSymbolFilter(new ExtensionUnifier());
    }

    @Override
    protected JavaConfiguration createConfig() {
        return new GLConfiguration(this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation sets the binding's native name to it's interface name,
     * which is the final aliased shortest name.
     * The latter is used for the proc-address-table etc ..
     * </p>
     */
    @Override
    protected void mangleBinding(final MethodBinding binding) {
        binding.setNativeName(binding.getInterfaceName());
        super.mangleBinding(binding);
    }

    /** In order to implement Buffer Object variants of certain
    functions we generate another MethodBinding which maps the void*
    argument to a Java long. The generation of emitters then takes
    place as usual. We do however need to keep track of the modified
    MethodBinding object so that we can also modify the emitters
    later to inform them that their argument has changed. We might
    want to push this functionality down into the MethodBinding
    (i.e., mutators for argument names). We also would need to
    inform the CMethodBindingEmitter that it is overloaded in this
    case (though we default to true currently). */
    @Override
    protected List<MethodBinding> expandMethodBinding(final MethodBinding binding) {
        final GLConfiguration glConfig = getGLConfig();
        final List<MethodBinding> bindings = super.expandMethodBinding(binding);

        if ( !glConfig.isBufferObjectFunction(binding.getCSymbol()) ) {
            return bindings;
        }
        final boolean bufferObjectOnly = glConfig.isBufferObjectOnly(binding.getName());

        final List<MethodBinding> newBindings = new ArrayList<MethodBinding>();

        // Need to expand each one of the generated bindings to take a
        // Java long instead of a Buffer for each void* argument
        if( GlueGen.debug() ) {
            System.err.println("expandMethodBinding: j "+binding.toString());
            System.err.println("expandMethodBinding: c "+binding.getCSymbol());
        }

        // for (MethodBinding cur : bindings) {
        int j=0;
        while( j < bindings.size() ) {
            final MethodBinding cur = bindings.get(j);

            // Some of these routines (glBitmap) take strongly-typed
            // primitive pointers as arguments which are expanded into
            // non-void* arguments
            // This test (rather than !signatureUsesNIO) is used to catch
            // more unexpected situations
            if (cur.signatureUsesJavaPrimitiveArrays()) {
                j++;
                continue;
            }

            MethodBinding result = cur;
            int replacedCount = 0;
            for (int i = 0; i < cur.getNumArguments(); i++) {
                final JavaType jt = cur.getJavaArgumentType(i);
                if( jt.isOpaqued() ) {
                    replacedCount++; // already replaced, i.e. due to opaque
                } else if ( jt.isNIOBuffer() ) {
                    result = result.replaceJavaArgumentType(i, JavaType.createForClass(Long.TYPE));
                    replacedCount++;
                }
                if( GlueGen.debug() ) {
                    final Type ct = cur.getCArgumentType(i);
                    System.err.println("  ["+i+"]: #"+replacedCount+", "+ct.getDebugString()+", "+jt.getDebugString());
                }
            }

            if ( 0 == replacedCount ) {
                throw new RuntimeException("Error: didn't find any void* arguments for BufferObject function "
                        + binding.toString());
            }

            // Now need to flag this MethodBinding so that we generate the
            // correct flags in the emitters later
            bufferObjectMethodBindings.put(result, result);

            if( result != cur ) {
                // replaced
                newBindings.add(result);
                if( bufferObjectOnly ) {
                    bindings.remove(j);
                } else {
                    j++;
                }
            } else {
                j++;
            }
        }
        bindings.addAll(newBindings);

        return bindings;
    }

    @Override
    protected boolean needsModifiedEmitters(final FunctionSymbol sym) {
        if ( ( !callThroughProcAddress(sym) && !needsBufferObjectVariant(sym) ) ||
             getConfig().isUnimplemented(sym)
           )
        {
            return false;
        } else {
            return true;
        }
    }

    public boolean isBufferObjectMethodBinding(final MethodBinding binding) {
        return bufferObjectMethodBindings.containsKey(binding);
    }

    @Override
    public void emitDefine(final ConstantDefinition def, final String optionalComment) throws Exception {
        final String symbolRenamed = def.getName();
        final StringBuilder newComment = new StringBuilder();
        if (0 == addExtensionsOfSymbols2Doc(newComment, ", ", ", ", symbolRenamed)) {
            if (def.isEnum()) {
                final String enumName = def.getEnumName();
                if (null == enumName) {
                    newComment.append("Part of CORE ");
                    newComment.append("ENUM");
                }
            } else {
                if (getGLConfig().getAllowNonGLExtensions()) {
                    newComment.append("Part of CORE ");
                    newComment.append("DEF");
                } else {
                    // Note: All GL defines must be contained within an extension marker !
                    // #ifndef GL_EXT_lala
                    // #define GL_EXT_lala 1
                    // ...
                    // #endif
                    final StringBuilder sb = new StringBuilder();
                    JavaEmitter.addStrings2Buffer(sb, ", ", symbolRenamed, def.getAliasedNames());
                    LOG.log(INFO, def.getASTLocusTag(), "Dropping marker: {0}", sb.toString());
                    return;
                }
            }
        }
        if (null != optionalComment) {
            if( newComment.length() > 0 ) {
                newComment.append("<br>");
            }
            newComment.append(optionalComment);
        }

        super.emitDefine(def, newComment.toString());
    }

    private int addExtensionListOfSymbol2Doc(final BuildStaticGLInfo glDocInfo, final StringBuilder buf, final String sep1, final String name) {
        int num = 0;
        final Set<String> extensionNames = glDocInfo.getExtension(name);
        if(null!=extensionNames) {
            for(final Iterator<String> i=extensionNames.iterator(); i.hasNext(); ) {
                final String extensionName = i.next();
                if (null != extensionName) {
                    buf.append("<code>");
                    buf.append(extensionName);
                    buf.append("</code>");
                    if (i.hasNext()) {
                        buf.append(sep1); // same-name seperator
                    }
                    num++;
                }
            }
        }
        return num;
    }
    private int addExtensionListOfAliasedSymbols2Doc(final BuildStaticGLInfo glDocInfo, final StringBuilder buf, final String sep1, final String sep2, final String name) {
        int num = 0;
        if(null != name) {
            num += addExtensionListOfSymbol2Doc(glDocInfo, buf, sep1, name); // extensions of given name
            boolean needsSep2 = num > 0;
            final Set<String> aliases = ((GLConfiguration)cfg).getRenamedJavaDocSymbols(name);
            if(null != aliases) {
                for(final String alias : aliases) {
                    if (needsSep2) {
                        buf.append(sep2);
                    }
                    final int num2 = addExtensionListOfSymbol2Doc(glDocInfo, buf, sep1, alias); // extensions of orig-name
                    needsSep2 = num2 > 0;
                    num += num2;
                }
            }
        }
        return num;
    }

    public int addExtensionsOfSymbols2Doc(StringBuilder buf, final String sep1, final String sep2, final String first) {
        final BuildStaticGLInfo glDocInfo = getGLConfig().getGLDocInfo();
        if (null == glDocInfo) {
            throw new RuntimeException("No GLDocInfo for: " + first);
        }
        if (null == buf) {
            buf = new StringBuilder();
        }
        return addExtensionListOfAliasedSymbols2Doc(glDocInfo, buf, sep1, sep2, first);
    }

    //----------------------------------------------------------------------
    // Internals only below this point
    //
    @Override
    protected void generateModifiedEmitters(final JavaMethodBindingEmitter baseJavaEmitter, final List<FunctionEmitter> emitters) {
        final List<FunctionEmitter> superEmitters = new ArrayList<FunctionEmitter>();
        super.generateModifiedEmitters(baseJavaEmitter, superEmitters);

        // See whether this is one of the Buffer Object variants
        final boolean bufferObjectVariant = bufferObjectMethodBindings.containsKey(baseJavaEmitter.getBinding());

        for (FunctionEmitter emitter : superEmitters) {
            if (emitter instanceof ProcAddressJavaMethodBindingEmitter) {
                emitter = new GLJavaMethodBindingEmitter((ProcAddressJavaMethodBindingEmitter) emitter, this, bufferObjectVariant);
            }
            emitters.add(emitter);
        }
    }

    protected boolean needsBufferObjectVariant(final FunctionSymbol sym) {
        return getGLConfig().isBufferObjectFunction(sym);
    }

    protected GLConfiguration getGLConfig() {
        return (GLConfiguration) getConfig();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void endProcAddressTable() throws Exception {
        final PrintWriter w = tableWriter;

        w.println("  @Override");
        w.println("  protected boolean isFunctionAvailableImpl(String functionNameUsr) throws IllegalArgumentException  {");
        w.println("    final String functionNameBase = "+GLNameResolver.class.getName()+".normalizeVEN(com.jogamp.gluegen.runtime.opengl.GLNameResolver.normalizeARB(functionNameUsr, true), true);");
        w.println("    final String addressFieldNameBase = \"" + PROCADDRESS_VAR_PREFIX + "\" + functionNameBase;");
        w.println("    final int funcNamePermNum = "+GLNameResolver.class.getName()+".getFuncNamePermutationNumber(functionNameBase);");
        w.println("    final java.lang.reflect.Field addressField = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<java.lang.reflect.Field>() {");
        w.println("        public final java.lang.reflect.Field run() {");
        w.println("            java.lang.reflect.Field addressField = null;");
        w.println("            for(int i = 0; i < funcNamePermNum; i++) {");
        w.println("                final String addressFieldName = "+GLNameResolver.class.getName()+".getFuncNamePermutation(addressFieldNameBase, i);");
        w.println("                try {");
        w.println("                    addressField = "+tableClassName+".class.getDeclaredField( addressFieldName );");
        w.println("                    addressField.setAccessible(true); // we need to read the protected value!");
        w.println("                    return addressField;");
        w.println("                } catch (NoSuchFieldException ex) { }");
        w.println("            }");
        w.println("            return null;");
        w.println("        } } );");
        w.println();
        w.println("    if(null==addressField) {");
        w.println("      // The user is calling a bogus function or one which is not");
        w.println("      // runtime linked");
        w.println("      throw new RuntimeException(");
        w.println("          \"WARNING: Address field query failed for \\\"\" + functionNameBase + \"\\\"/\\\"\" + functionNameUsr +");
        w.println("          \"\\\"; it's either statically linked or address field is not a known \" +");
        w.println("          \"function\");");
        w.println("    } ");
        w.println("    try {");
        w.println("      return 0 != addressField.getLong(this);");
        w.println("    } catch (Exception e) {");
        w.println("      throw new RuntimeException(");
        w.println("          \"WARNING: Address query failed for \\\"\" + functionNameBase + \"\\\"/\\\"\" + functionNameUsr +");
        w.println("          \"\\\"; it's either statically linked or is not a known \" +");
        w.println("          \"function\", e);");
        w.println("    }");
        w.println("  }");

        w.println("  @Override");
        w.println("  public long getAddressFor(String functionNameUsr) throws SecurityException, IllegalArgumentException {");
        w.println("    SecurityUtil.checkAllLinkPermission();");
        w.println("    final String functionNameBase = "+GLNameResolver.class.getName()+".normalizeVEN(com.jogamp.gluegen.runtime.opengl.GLNameResolver.normalizeARB(functionNameUsr, true), true);");
        w.println("    final String addressFieldNameBase = \"" + PROCADDRESS_VAR_PREFIX + "\" + functionNameBase;");
        w.println("    final int  funcNamePermNum = "+GLNameResolver.class.getName()+".getFuncNamePermutationNumber(functionNameBase);");
        w.println("    final java.lang.reflect.Field addressField = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<java.lang.reflect.Field>() {");
        w.println("        public final java.lang.reflect.Field run() {");
        w.println("            java.lang.reflect.Field addressField = null;");
        w.println("            for(int i = 0; i < funcNamePermNum; i++) {");
        w.println("                final String addressFieldName = "+GLNameResolver.class.getName()+".getFuncNamePermutation(addressFieldNameBase, i);");
        w.println("                try {");
        w.println("                    addressField = "+tableClassName+".class.getDeclaredField( addressFieldName );");
        w.println("                    addressField.setAccessible(true); // we need to read the protected value!");
        w.println("                    return addressField;");
        w.println("                } catch (NoSuchFieldException ex) { }");
        w.println("            }");
        w.println("            return null;");
        w.println("        } } );");
        w.println();
        w.println("    if(null==addressField) {");
        w.println("      // The user is calling a bogus function or one which is not");
        w.println("      // runtime linked");
        w.println("      throw new RuntimeException(");
        w.println("          \"WARNING: Address field query failed for \\\"\" + functionNameBase + \"\\\"/\\\"\" + functionNameUsr +");
        w.println("          \"\\\"; it's either statically linked or address field is not a known \" +");
        w.println("          \"function\");");
        w.println("    } ");
        w.println("    try {");
        w.println("      return addressField.getLong(this);");
        w.println("    } catch (Exception e) {");
        w.println("      throw new RuntimeException(");
        w.println("          \"WARNING: Address query failed for \\\"\" + functionNameBase + \"\\\"/\\\"\" + functionNameUsr +");
        w.println("          \"\\\"; it's either statically linked or is not a known \" +");
        w.println("          \"function\", e);");
        w.println("    }");
        w.println("  }");

        w.println("} // end of class " + tableClassName);
        w.flush();
        w.close();
    }
}

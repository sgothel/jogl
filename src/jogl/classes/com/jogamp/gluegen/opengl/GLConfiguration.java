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

import com.jogamp.gluegen.GlueEmitterControls;
import com.jogamp.gluegen.GlueGen;
import com.jogamp.gluegen.MethodBinding;
import com.jogamp.gluegen.cgram.types.AliasedSymbol;
import com.jogamp.gluegen.procaddress.ProcAddressConfiguration;
import com.jogamp.gluegen.runtime.opengl.GLNameResolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

public class GLConfiguration extends ProcAddressConfiguration {

    // The following data members support ignoring an entire extension at a time
    private final List<String> glSemHeaders = new ArrayList<String>();
    private final Set<String> ignoredExtensions = new HashSet<String>();
    private final Set<String> forcedExtensions = new HashSet<String>();
    private final Set<String> renameExtensionsIntoCore = new HashSet<String>();
    private BuildStaticGLInfo glSemInfo;

    // GLDocHeaders include GLSemHeaders!
    boolean dropDocInfo = false;
    private final List<String> glDocHeaders = new ArrayList<String>();
    // GLDocInfo include GLSemInfo!
    private BuildStaticGLInfo glDocInfo;
    private final Map<String, String> javaDocSymbolRenames = new HashMap<String, String>();
    private final Map<String, Set<String>> javaDocRenamedSymbols = new HashMap<String, Set<String>>();

    // Maps function names to the kind of buffer object it deals with
    private final Map<String, GLEmitter.BufferObjectKind> bufferObjectKinds = new HashMap<String, GLEmitter.BufferObjectKind>();
    private final Set<String> bufferObjectOnly = new HashSet<String>();
    private final GLEmitter emitter;
    private final Set<String> dropUniqVendorExtensions = new HashSet<String>();

    // This directive is off by default but can help automatically
    // indicate which extensions have been folded into the core OpenGL
    // namespace, and if not, then why not
    private boolean autoUnifyExtensions = false;
    private boolean allowNonGLExtensions = false;

    public GLConfiguration(final GLEmitter emitter) {
        super();
        this.emitter = emitter;
        try {
            setProcAddressNameExpr("PFN $UPPERCASE({0}) PROC");
        } catch (final NoSuchElementException e) {
            throw new RuntimeException("Error configuring ProcAddressNameExpr", e);
        }
    }

    @Override
    protected void dispatch(final String cmd, final StringTokenizer tok, final File file, final String filename, final int lineNo) throws IOException {
        if (cmd.equalsIgnoreCase("IgnoreExtension")) {
            final String sym = readString("IgnoreExtension", tok, filename, lineNo);
            ignoredExtensions.add(sym);
        } else if (cmd.equalsIgnoreCase("ForceExtension")) {
            final String sym = readString("ForceExtension", tok, filename, lineNo);
            forcedExtensions.add(sym);
        } else if (cmd.equalsIgnoreCase("RenameExtensionIntoCore")) {
            final String sym = readString("RenameExtensionIntoCore", tok, filename, lineNo);
            renameExtensionsIntoCore.add(sym);
        } else if (cmd.equalsIgnoreCase("AllowNonGLExtensions")) {
            allowNonGLExtensions = readBoolean("AllowNonGLExtensions", tok, filename, lineNo).booleanValue();
        } else if (cmd.equalsIgnoreCase("AutoUnifyExtensions")) {
            autoUnifyExtensions = readBoolean("AutoUnifyExtensions", tok, filename, lineNo).booleanValue();
        } else if (cmd.equalsIgnoreCase("GLSemHeader")) {
            final String sym = readString("GLSemHeader", tok, filename, lineNo);
            if( !glSemHeaders.contains(sym) ) {
                glSemHeaders.add(sym);
            }
            if( !dropDocInfo && !glDocHeaders.contains(sym) ) {
                glDocHeaders.add(sym);
            }
        } else if (cmd.equalsIgnoreCase("GLDocHeader")) {
            final String sym = readString("GLDocHeader", tok, filename, lineNo);
            if( !dropDocInfo && !glDocHeaders.contains(sym) ) {
                glDocHeaders.add(sym);
            }
        } else if (cmd.equalsIgnoreCase("DropAllGLDocHeader")) {
            dropDocInfo = readBoolean("DropAllGLDocHeader", tok, filename, lineNo).booleanValue();
        } else if (cmd.equalsIgnoreCase("BufferObjectKind")) {
            readBufferObjectKind(tok, filename, lineNo);
        } else if (cmd.equalsIgnoreCase("BufferObjectOnly")) {
            final String sym = readString("BufferObjectOnly", tok, filename, lineNo);
            bufferObjectOnly.add(sym);
        } else if (cmd.equalsIgnoreCase("DropUniqVendorExtensions")) {
            final String sym = readString("DropUniqVendorExtensions", tok, filename, lineNo);
            dropUniqVendorExtensions.add(sym);
        } else {
            super.dispatch(cmd, tok, file, filename, lineNo);
        }
    }

    protected void readBufferObjectKind(final StringTokenizer tok, final String filename, final int lineNo) {
        try {
            final String kindString = tok.nextToken();
            GLEmitter.BufferObjectKind kind = null;
            final String target = tok.nextToken();
            if (kindString.equalsIgnoreCase("UnpackPixel")) {
                kind = GLEmitter.BufferObjectKind.UNPACK_PIXEL;
            } else if (kindString.equalsIgnoreCase("PackPixel")) {
                kind = GLEmitter.BufferObjectKind.PACK_PIXEL;
            } else if (kindString.equalsIgnoreCase("Array")) {
                kind = GLEmitter.BufferObjectKind.ARRAY;
            } else if (kindString.equalsIgnoreCase("Element")) {
                kind = GLEmitter.BufferObjectKind.ELEMENT;
            } else if (kindString.equalsIgnoreCase("Indirect")) {
                kind = GLEmitter.BufferObjectKind.INDIRECT;
            } else {
                throw new RuntimeException("Error parsing \"BufferObjectKind\" command at line " + lineNo
                        + " in file \"" + filename + "\": illegal BufferObjectKind \""
                        + kindString + "\", expected one of UnpackPixel, PackPixel, Array, Element or Indirect");
            }

            bufferObjectKinds.put(target, kind);
        } catch (final NoSuchElementException e) {
            throw new RuntimeException("Error parsing \"BufferObjectKind\" command at line " + lineNo
                    + " in file \"" + filename + "\"", e);
        }
    }

    /** Overrides javaPrologueForMethod in superclass and
    automatically generates prologue code for functions associated
    with buffer objects. */
    @Override
    public List<String> javaPrologueForMethod(final MethodBinding binding, final boolean forImplementingMethodCall, final boolean eraseBufferAndArrayTypes) {

        List<String> res = super.javaPrologueForMethod(binding, forImplementingMethodCall, eraseBufferAndArrayTypes);
        final GLEmitter.BufferObjectKind kind = getBufferObjectKind(binding.getCSymbol());
        if (kind != null) {
            // Need to generate appropriate prologue based on both buffer
            // object kind and whether this variant of the MethodBinding
            // is the one accepting a "long" as argument
            //
            // NOTE we MUST NOT mutate the array returned from the super
            // call!
            final ArrayList<String> res2 = new ArrayList<String>();
            if (res != null) {
                res2.addAll(res);
            }
            res = res2;

            String prologue = "check";

            if (kind == GLEmitter.BufferObjectKind.UNPACK_PIXEL) {
                prologue = prologue + "UnpackPBO";
            } else if (kind == GLEmitter.BufferObjectKind.PACK_PIXEL) {
                prologue = prologue + "PackPBO";
            } else if (kind == GLEmitter.BufferObjectKind.ARRAY) {
                prologue = prologue + "ArrayVBO";
            } else if (kind == GLEmitter.BufferObjectKind.ELEMENT) {
                prologue = prologue + "ElementVBO";
            } else if (kind == GLEmitter.BufferObjectKind.INDIRECT) {
                prologue = prologue + "IndirectVBO";
            } else {
                throw new RuntimeException("Unknown BufferObjectKind " + kind);
            }

            if (emitter.isBufferObjectMethodBinding(binding)) {
                prologue = prologue + "Bound";
            } else {
                prologue = prologue + "Unbound";
            }

            prologue = prologue + "(true);";

            res.add(0, prologue);

            // Must also filter out bogus rangeCheck directives for VBO/PBO
            // variants
            if (emitter.isBufferObjectMethodBinding(binding)) {
                for (final Iterator<String> iter = res.iterator(); iter.hasNext();) {
                    final String line = iter.next();
                    if (line.indexOf("Buffers.rangeCheck") >= 0) {
                        iter.remove();
                    }
                }
            }
        }

        return res;
    }

    @Override
    public void logIgnores() {
        LOG.log(INFO, "GL Ignored extensions: {0}", ignoredExtensions.size());
        for (final String str : ignoredExtensions) {
            LOG.log(INFO, "\t{0}", str);
        }
        LOG.log(INFO, "GL Forced extensions: {0}", forcedExtensions.size());
        for (final String str : forcedExtensions) {
            LOG.log(INFO, "\t{0}", str);
        }
        super.logIgnores();
    }

    @Override
    public void logRenames() {
        LOG.log(INFO, "GL Renamed extensions into core: {0}", renameExtensionsIntoCore.size());
        for (final String str : renameExtensionsIntoCore) {
            LOG.log(INFO, "\t{0}", str);
        }
        super.logRenames();
    }

    protected boolean isIgnoredExtension(final String extensionName) {
        if( ignoredExtensions.contains(extensionName) ) {
            return !forcedExtensions.contains(extensionName);
        } else {
            return false;
        }
    }

    protected boolean shouldIgnoreExtension(final AliasedSymbol symbol) {
        final Set<String> symExtensionNames;
        // collect current-name symbol extensions
        {
            final Set<String> s = glSemInfo.getExtension(symbol.getName());
            if( null != s ) {
                symExtensionNames = s;
            } else {
                symExtensionNames = new HashSet<String>();
            }
        }
        // collect renamed symbol extensions
        if( symbol.hasAliases() ) {
            final Set<String> aliases = symbol.getAliasedNames();
            for(final String alias : aliases) {
                final Set<String> s = glSemInfo.getExtension(alias);
                if( null != s && s.size() > 0 ) {
                    symExtensionNames.addAll(s);
                }
            }
        }
        boolean ignoreExtension = symExtensionNames.size() > 0 &&
                                  ignoredExtensions.containsAll(symExtensionNames);

        if( LOG.isLoggable(INFO) ) {
            final Set<String> ignoredSymExtensionNames = new HashSet<String>();
            final Set<String> notIgnoredSymExtensionNames = new HashSet<String>();
            for(final Iterator<String> i=symExtensionNames.iterator(); i.hasNext(); ) {
                final String extensionName = i.next();
                if ( null != extensionName && ignoredExtensions.contains(extensionName) ) {
                    ignoredSymExtensionNames.add(extensionName);
                } else {
                    notIgnoredSymExtensionNames.add(extensionName);
                }
            }
            if( ignoreExtension ) {
                LOG.log(INFO, getASTLocusTag(symbol), "Ignored symbol {0} of all extensions <{1}>", symbol, symExtensionNames);
            } else if( ignoredSymExtensionNames.size() > 0 ) {
                LOG.log(INFO, getASTLocusTag(symbol), "Not ignored symbol {0};  Ignored in <{1}>, but active in <{2}>",
                        symbol, ignoredSymExtensionNames, notIgnoredSymExtensionNames);
            }
        }
        if( !ignoreExtension ) {
            // Check whether the current-name denotes an ignored vendor extension
            final String name = symbol.getName();
            final boolean isGLEnum = GLNameResolver.isGLEnumeration(name);
            final boolean isGLFunc = GLNameResolver.isGLFunction(name);
            String extSuffix = null;
            if (isGLFunc || isGLEnum) {
                if (GLNameResolver.isExtensionVEN(name, isGLFunc)) {
                    extSuffix = GLNameResolver.getExtensionSuffix(name, isGLFunc);
                    if (getDropUniqVendorExtensions(extSuffix)) {
                        LOG.log(INFO, getASTLocusTag(symbol), "Ignore UniqVendorEXT: {0}, vendor {1}, isGLFunc {2}, isGLEnum {3}",
                                symbol, extSuffix, isGLFunc, isGLEnum);
                        ignoreExtension = true;
                    }
                }
            }
            if (!ignoreExtension) {
                LOG.log(INFO, getASTLocusTag(symbol), "Not ignored UniqVendorEXT: {0}, vendor {1}, isGLFunc {2}, isGLEnum {3}",
                        symbol, extSuffix, isGLFunc, isGLEnum);
            }
        }
        if( ignoreExtension ) {
            ignoreExtension = !shouldForceExtension( symbol, symExtensionNames);
        }
        return ignoreExtension;
    }
    public boolean shouldForceExtension(final AliasedSymbol symbol, final Set<String> symExtensionNames) {
        for(final Iterator<String> i=symExtensionNames.iterator(); i.hasNext(); ) {
            final String extensionName = i.next();
            if ( extensionName != null && forcedExtensions.contains(extensionName) ) {
                LOG.log(INFO, getASTLocusTag(symbol), "Not ignored symbol {0} of extension <{1}>", symbol, extensionName);
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation extends the exclusion query w/ {@link #shouldIgnoreExtension(AliasedSymbol) the list of ignored extensions}.
     * </p>
     * <p>
     * If passing the former, it calls down to {@link #shouldIgnoreInInterface_Int(AliasedSymbol)}.
     * </p>
     */
    @Override
    public boolean shouldIgnoreInInterface(final AliasedSymbol symbol) {
        return shouldIgnoreExtension(symbol) || shouldIgnoreInInterface_Int(symbol);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation extends the exclusion query w/ {@link #shouldIgnoreExtension(AliasedSymbol) the list of ignored extensions}.
     * </p>
     * <p>
     * If passing the former, it calls down to {@link #shouldIgnoreInImpl_Int(AliasedSymbol)}.
     * </p>
     */
    @Override
    public boolean shouldIgnoreInImpl(final AliasedSymbol symbol) {
        return shouldIgnoreExtension(symbol) || shouldIgnoreInImpl_Int(symbol);
    }

    /** Should we automatically ignore extensions that have already been
    fully subsumed into the OpenGL core namespace, and if they have
    not been, indicate which definition is not already in the core? */
    public boolean getAutoUnifyExtensions() {
        return autoUnifyExtensions;
    }

    /** If true, accept all non encapsulated defines and functions,
     * as it is mandatory for GL declarations. */
    public boolean getAllowNonGLExtensions() {
        return allowNonGLExtensions;
    }

    /** shall the non unified (uniq) vendor extensions be dropped ?  */
    public boolean getDropUniqVendorExtensions(final String extName) {
        return dropUniqVendorExtensions.contains(extName);
    }

    /** Returns the kind of buffer object this function deals with, or
    null if none. */
    GLEmitter.BufferObjectKind getBufferObjectKind(final AliasedSymbol symbol) {
        final String name = symbol.getName();
        final Set<String> aliases = symbol.getAliasedNames();
        GLEmitter.BufferObjectKind res = bufferObjectKinds.get( name );
        if( null == res ) {
            res = oneInMap(bufferObjectKinds, aliases);
        }
        return res;
    }

    public boolean isBufferObjectFunction(final AliasedSymbol symbol) {
        return null != getBufferObjectKind(symbol);
    }

    public boolean isBufferObjectOnly(final String name) {
        return bufferObjectOnly.contains(name);
    }

    /**
     * Parses any GL headers specified in the configuration file for
     * the purpose of being able to ignore an extension at a time.
     * <p>
     * Targeting semantic information, i.e. influences code generation.
     * </p>
     */
    public void parseGLSemHeaders(final GlueEmitterControls controls) throws IOException {
        glSemInfo = new BuildStaticGLInfo();
        glSemInfo.setDebug(GlueGen.debug());
        if (!glSemHeaders.isEmpty()) {
            for (final String file : glSemHeaders) {
                final String fullPath = controls.findHeaderFile(file);
                if (fullPath == null) {
                    throw new IOException("Unable to locate header file \"" + file + "\"");
                }
                glSemInfo.parse(fullPath);
            }
        }
    }

    /**
     * Returns the information about the association between #defines,
     * function symbols and the OpenGL extensions they are defined in.
     * <p>
     * This instance targets semantic information, i.e. influences code generation.
     * </p>
     */
    public BuildStaticGLInfo getGLSemInfo() {
        return glSemInfo;
    }

    /**
     * Parses any GL headers specified in the configuration file for
     * the purpose of being able to ignore an extension at a time.
     * <p>
     * Targeting API documentation information, i.e. <i>not</i> influencing code generation.
     * </p>
     */
    public void parseGLDocHeaders(final GlueEmitterControls controls) throws IOException {
        glDocInfo = new BuildStaticGLInfo();
        glDocInfo.setDebug(GlueGen.debug());
        if (!glDocHeaders.isEmpty()) {
            for (final String file : glDocHeaders) {
                final String fullPath = controls.findHeaderFile(file);
                if (fullPath == null) {
                    throw new IOException("Unable to locate header file \"" + file + "\"");
                }
                glDocInfo.parse(fullPath);
            }
        }
    }

    @Override
    public Set<String> getAliasedDocNames(final AliasedSymbol symbol) {
        return getRenamedJavaDocSymbols(symbol.getName());
    }

    /**
     * Returns the information about the association between #defines,
     * function symbols and the OpenGL extensions they are defined in.
     * <p>
     * This instance targets API documentation information, i.e. <i>not</i> influencing code generation.
     * </p>
     * <p>
     * GLDocInfo include GLSemInfo!
     * </p>
     */
    public BuildStaticGLInfo getGLDocInfo() {
        return glDocInfo;
    }

    /** Returns a set of replaced javadoc names to the given <code>aliasedName</code>. */
    public Set<String> getRenamedJavaDocSymbols(final String aliasedName) {
        return javaDocRenamedSymbols.get(aliasedName);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Also adds a javadoc rename directive for the given symbol.
     * </p>
     */
    @Override
    public void addJavaSymbolRename(final String origName, final String newName) {
        super.addJavaSymbolRename(origName, newName);
        if( !dropDocInfo ) {
            addJavaDocSymbolRename(origName, newName);
        }
    }

    /**
     * Adds a javadoc rename directive for the given symbol.
     */
    public void addJavaDocSymbolRename(final String origName, final String newName) {
        LOG.log(INFO, "\tDoc Rename {0} -> {1}", origName, newName);
        final String prevValue = javaDocSymbolRenames.put(origName, newName);
        if(null != prevValue && !prevValue.equals(newName)) {
            throw new RuntimeException("Doc-Rename-Override Attampt: "+origName+" -> "+newName+
                    ", but "+origName+" -> "+prevValue+" already exist. Run in 'debug' mode to analyze!");
        }

        Set<String> origNames = javaDocRenamedSymbols.get(newName);
        if(null == origNames) {
            origNames = new HashSet<String>();
            javaDocRenamedSymbols.put(newName, origNames);
        }
        origNames.add(origName);
    }

    /** Returns the OpenGL extensions that should have all of their
    constant definitions and functions renamed into the core
    namespace; for example, glGenFramebuffersEXT to
    glGenFramebuffers and GL_FRAMEBUFFER_EXT to GL_FRAMEBUFFER. */
    public Set<String> getExtensionsRenamedIntoCore() {
        return renameExtensionsIntoCore;
    }
}

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

import com.jogamp.gluegen.GlueEmitterControls;
import com.jogamp.gluegen.GlueGen;
import com.jogamp.gluegen.MethodBinding;
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
    private final List<String> glHeaders = new ArrayList<String>();
    private final Set<String> ignoredExtensions = new HashSet<String>();
    private final Set<String> forcedExtensions = new HashSet<String>();
    private final Set<String> extensionsRenamedIntoCore = new HashSet<String>();
    private BuildStaticGLInfo glInfo;

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
            extensionsRenamedIntoCore.add(sym);
        } else if (cmd.equalsIgnoreCase("AllowNonGLExtensions")) {
            allowNonGLExtensions = readBoolean("AllowNonGLExtensions", tok, filename, lineNo).booleanValue();
        } else if (cmd.equalsIgnoreCase("AutoUnifyExtensions")) {
            autoUnifyExtensions = readBoolean("AutoUnifyExtensions", tok, filename, lineNo).booleanValue();
        } else if (cmd.equalsIgnoreCase("GLHeader")) {
            final String sym = readString("GLHeader", tok, filename, lineNo);
            glHeaders.add(sym);
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
        final GLEmitter.BufferObjectKind kind = getBufferObjectKind(binding.getName());
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
    public void dumpIgnores() {
        System.err.println("GL Ignored extensions: ");
        for (final String str : ignoredExtensions) {
            System.err.println("\t" + str);
        }
        System.err.println("GL Forced extensions: ");
        for (final String str : forcedExtensions) {
            System.err.println("\t" + str);
        }
        super.dumpIgnores();
    }

    protected boolean shouldIgnoreExtension(final String symbol, final boolean criteria) {
        if (criteria && glInfo != null) {
            final Set<String> extensionNames = glInfo.getExtension(symbol);
            if( null != extensionNames ) {
                boolean ignoredExtension = false;
                for(final Iterator<String> i=extensionNames.iterator(); !ignoredExtension && i.hasNext(); ) {
                    final String extensionName = i.next();
                    if ( extensionName != null && ignoredExtensions.contains(extensionName) ) {
                        if (DEBUG_IGNORES) {
                            System.err.print("Ignore symbol <" + symbol + "> of extension <" + extensionName + ">");
                            if(extensionNames.size()==1) {
                                System.err.println(", single .");
                            } else {
                                System.err.println(", WARNING MULTIPLE OCCURENCE: "+extensionNames);
                            }
                        }
                        ignoredExtension = true;
                    }
                }
                if( ignoredExtension ) {
                    ignoredExtension = !shouldForceExtension( symbol, true, symbol );
                    if( ignoredExtension ) {
                        final Set<String> origSymbols = getRenamedJavaSymbols( symbol );
                        if(null != origSymbols) {
                            for(final String origSymbol : origSymbols) {
                                if( shouldForceExtension( origSymbol, true, symbol ) ) {
                                    ignoredExtension = false;
                                    break;
                                }
                            }
                        }
                    }
                }
                if( ignoredExtension ) {
                    return true;
                }
            }
            final boolean isGLEnum = GLNameResolver.isGLEnumeration(symbol);
            final boolean isGLFunc = GLNameResolver.isGLFunction(symbol);
            if (isGLFunc || isGLEnum) {
                if (GLNameResolver.isExtensionVEN(symbol, isGLFunc)) {
                    final String extSuffix = GLNameResolver.getExtensionSuffix(symbol, isGLFunc);
                    if (getDropUniqVendorExtensions(extSuffix)) {
                        if (DEBUG_IGNORES) {
                            System.err.println("Ignore UniqVendorEXT: " + symbol + ", vendor " + extSuffix);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean shouldForceExtension(final String symbol, final boolean criteria, final String renamedSymbol) {
        if (criteria && glInfo != null) {
            final Set<String> extensionNames = glInfo.getExtension(symbol);
            if( null != extensionNames ) {
                for(final Iterator<String> i=extensionNames.iterator(); i.hasNext(); ) {
                    final String extensionName = i.next();
                    if ( extensionName != null && forcedExtensions.contains(extensionName) ) {
                        if (DEBUG_IGNORES) {
                            System.err.print("Not Ignore symbol <" + symbol + " -> " + renamedSymbol + "> of extension <" + extensionName + ">");
                            if(extensionNames.size()==1) {
                                System.err.println(", single .");
                            } else {
                                System.err.println(", WARNING MULTIPLE OCCURENCE: "+extensionNames);
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean shouldIgnoreInInterface(final String symbol) {
        return shouldIgnoreInInterface(symbol, true);
    }

    public boolean shouldIgnoreInInterface(final String symbol, final boolean checkEXT) {
        return shouldIgnoreExtension(symbol, checkEXT) || super.shouldIgnoreInInterface(symbol);
    }

    @Override
    public boolean shouldIgnoreInImpl(final String symbol) {
        return shouldIgnoreInImpl(symbol, true);
    }

    public boolean shouldIgnoreInImpl(final String symbol, final boolean checkEXT) {
        return shouldIgnoreExtension(symbol, checkEXT) || super.shouldIgnoreInImpl(symbol);
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
    GLEmitter.BufferObjectKind getBufferObjectKind(final String name) {
        return bufferObjectKinds.get(name);
    }

    public boolean isBufferObjectFunction(final String name) {
        return (getBufferObjectKind(name) != null);
    }

    public boolean isBufferObjectOnly(final String name) {
        return bufferObjectOnly.contains(name);
    }

    /** Parses any GL headers specified in the configuration file for
    the purpose of being able to ignore an extension at a time. */
    public void parseGLHeaders(final GlueEmitterControls controls) throws IOException {
        if (!glHeaders.isEmpty()) {
            glInfo = new BuildStaticGLInfo();
            glInfo.setDebug(GlueGen.debug());
            for (final String file : glHeaders) {
                final String fullPath = controls.findHeaderFile(file);
                if (fullPath == null) {
                    throw new IOException("Unable to locate header file \"" + file + "\"");
                }
                glInfo.parse(fullPath);
            }
        }
    }

    /** Returns the information about the association between #defines,
    function symbols and the OpenGL extensions they are defined
    in. */
    public BuildStaticGLInfo getGLInfo() {
        return glInfo;
    }

    /** Returns the OpenGL extensions that should have all of their
    constant definitions and functions renamed into the core
    namespace; for example, glGenFramebuffersEXT to
    glGenFramebuffers and GL_FRAMEBUFFER_EXT to GL_FRAMEBUFFER. */
    public Set<String> getExtensionsRenamedIntoCore() {
        return extensionsRenamedIntoCore;
    }
}

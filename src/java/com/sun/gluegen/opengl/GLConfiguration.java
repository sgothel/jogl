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

package com.sun.gluegen.opengl;

import java.io.*;
import java.util.*;

import com.sun.gluegen.*;
import com.sun.gluegen.procaddress.*;
import com.sun.gluegen.runtime.opengl.GLExtensionNames;

public class GLConfiguration extends ProcAddressConfiguration {
  // The following data members support ignoring an entire extension at a time
  private List/*<String>*/ glHeaders = new ArrayList();
  private Set/*<String>*/ ignoredExtensions = new HashSet();
  private Set/*<String>*/ extensionsRenamedIntoCore = new HashSet();
  private BuildStaticGLInfo glInfo;
  // Maps function names to the kind of buffer object it deals with
  private Map/*<String,GLEmitter.BufferObjectKind>*/ bufferObjectKinds = new HashMap();
  private GLEmitter emitter;
  private Set/*String*/ dropUniqVendorExtensions = new HashSet();
  // This directive is off by default but can help automatically
  // indicate which extensions have been folded into the core OpenGL
  // namespace, and if not, then why not
  private boolean autoUnifyExtensions=false;
  private boolean allowNonGLExtensions=false;

  public GLConfiguration(GLEmitter emitter) {
    super();
    this.emitter = emitter;
    try {
      setProcAddressNameExpr("PFN $UPPERCASE({0}) PROC");
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error configuring ProcAddressNameExpr", e);
    }
  }

  protected void dispatch(String cmd, StringTokenizer tok, File file, String filename, int lineNo) throws IOException {
    if (cmd.equalsIgnoreCase("IgnoreExtension"))
      {
        String sym = readString("IgnoreExtension", tok, filename, lineNo);
        ignoredExtensions.add(sym);
      }
    else if (cmd.equalsIgnoreCase("RenameExtensionIntoCore"))
      {
        String sym = readString("RenameExtensionIntoCore", tok, filename, lineNo);
        extensionsRenamedIntoCore.add(sym);
      }
    else if (cmd.equalsIgnoreCase("AllowNonGLExtensions"))
      {
        allowNonGLExtensions = readBoolean("AllowNonGLExtensions", tok, filename, lineNo).booleanValue();
      }
    else if (cmd.equalsIgnoreCase("AutoUnifyExtensions"))
      {
        autoUnifyExtensions = readBoolean("AutoUnifyExtensions", tok, filename, lineNo).booleanValue();
      }
    else if (cmd.equalsIgnoreCase("GLHeader"))
      {
        String sym = readString("GLHeader", tok, filename, lineNo);
        glHeaders.add(sym);
      }
    else if (cmd.equalsIgnoreCase("BufferObjectKind"))
      {
        readBufferObjectKind(tok, filename, lineNo);
      }
    else if (cmd.equalsIgnoreCase("DropUniqVendorExtensions"))
      {
        String sym = readString("DropUniqVendorExtensions", tok, filename, lineNo);
        dropUniqVendorExtensions.add(sym);
      }
    else
      {
        super.dispatch(cmd,tok,file,filename,lineNo);
      }
  }  

  protected void readBufferObjectKind(StringTokenizer tok, String filename, int lineNo) {
    try {
      String kindString = tok.nextToken();
      GLEmitter.BufferObjectKind kind = null;
      String target = tok.nextToken();
      if (kindString.equalsIgnoreCase("UnpackPixel")) {
        kind = GLEmitter.BufferObjectKind.UNPACK_PIXEL;
      } else if (kindString.equalsIgnoreCase("PackPixel")) {
        kind = GLEmitter.BufferObjectKind.PACK_PIXEL;
      } else if (kindString.equalsIgnoreCase("Array")) {
        kind = GLEmitter.BufferObjectKind.ARRAY;
      } else if (kindString.equalsIgnoreCase("Element")) {
        kind = GLEmitter.BufferObjectKind.ELEMENT;
      } else {
        throw new RuntimeException("Error parsing \"BufferObjectKind\" command at line " + lineNo +
                                   " in file \"" + filename + "\": illegal BufferObjectKind \"" +
                                   kindString + "\", expected one of UnpackPixel, PackPixel, Array, or Element");
      }

      bufferObjectKinds.put(target, kind);
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"BufferObjectKind\" command at line " + lineNo +
                                 " in file \"" + filename + "\"", e);
    }
  }

  /** Overrides javaPrologueForMethod in superclass and
      automatically generates prologue code for functions associated
      with buffer objects. */
  public List/*<String>*/ javaPrologueForMethod(MethodBinding binding,
                                                boolean forImplementingMethodCall,
                                                boolean eraseBufferAndArrayTypes) {
    List/*<String>*/ res = super.javaPrologueForMethod(binding,
                                                       forImplementingMethodCall,
                                                       eraseBufferAndArrayTypes);
    GLEmitter.BufferObjectKind kind = getBufferObjectKind(binding.getName());
    if (kind != null) {
      // Need to generate appropriate prologue based on both buffer
      // object kind and whether this variant of the MethodBinding
      // is the one accepting a "long" as argument
      //
      // NOTE we MUST NOT mutate the array returned from the super
      // call!
      ArrayList res2 = new ArrayList();
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
      } else {
        throw new RuntimeException("Unknown BufferObjectKind " + kind);
      }

      if (emitter.isBufferObjectMethodBinding(binding)) {
        prologue = prologue + "Enabled";
      } else {
        prologue = prologue + "Disabled";
      }

      prologue = prologue + "(true);";

      res.add(0, prologue);

      // Must also filter out bogus rangeCheck directives for VBO/PBO
      // variants
      if (emitter.isBufferObjectMethodBinding(binding)) {
        for (Iterator iter = res.iterator(); iter.hasNext(); ) {
          String line = (String) iter.next();
          if (line.indexOf("BufferFactory.rangeCheck") >= 0) {
            iter.remove();
          }
        }
      }
    }

    return res;
  }

  public void dumpIgnores() {
    System.err.println("GL Ignored extensions: ");
    for (Iterator iter = ignoredExtensions.iterator(); iter.hasNext(); ) {
        System.err.println("\t"+(String)iter.next());
    }
    super.dumpIgnores();
  }

  protected boolean shouldIgnoreExtension(String symbol, boolean criteria) {
    if (criteria && glInfo != null) {
      String extension = glInfo.getExtension(symbol);
      if (extension != null &&
          ignoredExtensions.contains(extension)) {
          return true;
      }
      boolean isGLEnum = GLExtensionNames.isGLEnumeration(symbol);
      boolean isGLFunc = GLExtensionNames.isGLFunction(symbol);
      if(isGLFunc || isGLEnum) {
        if(GLExtensionNames.isExtensionVEN(symbol, isGLFunc)) {
          String extSuffix = GLExtensionNames.getExtensionSuffix(symbol, isGLFunc);
          if( getDropUniqVendorExtensions(extSuffix) ) {
            if(DEBUG_IGNORES) {
              System.err.println("Ignore UniqVendorEXT: "+symbol+", vendor "+extSuffix);
            }
            return true;
          }
        }
      }
    }
    return false;
  }

  public boolean shouldIgnoreInInterface(String symbol) {
    return shouldIgnoreInInterface(symbol, true);
  }

  public boolean shouldIgnoreInInterface(String symbol, boolean checkEXT) {
    return shouldIgnoreExtension(symbol, checkEXT) || super.shouldIgnoreInInterface(symbol);
  }

  public boolean shouldIgnoreInImpl(String symbol) {
    return shouldIgnoreInImpl(symbol, true);
  }

  public boolean shouldIgnoreInImpl(String symbol, boolean checkEXT) {
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
  public boolean getDropUniqVendorExtensions(String extName) {
      return dropUniqVendorExtensions.contains(extName);
  }

  /** Returns the kind of buffer object this function deals with, or
      null if none. */
  public GLEmitter.BufferObjectKind getBufferObjectKind(String name) {
    return (GLEmitter.BufferObjectKind) bufferObjectKinds.get(name);
  }

  public boolean isBufferObjectFunction(String name) {
    return (getBufferObjectKind(name) != null);
  }

  /** Parses any GL headers specified in the configuration file for
      the purpose of being able to ignore an extension at a time. */
  public void parseGLHeaders(GlueEmitterControls controls) throws IOException {
    if (!glHeaders.isEmpty()) {
      glInfo = new BuildStaticGLInfo();
      for (Iterator iter = glHeaders.iterator(); iter.hasNext(); ) {
        String file = (String) iter.next();
        String fullPath = controls.findHeaderFile(file);
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
  public Set/*<String>*/ getExtensionsRenamedIntoCore() {
    return extensionsRenamedIntoCore;
  }
}

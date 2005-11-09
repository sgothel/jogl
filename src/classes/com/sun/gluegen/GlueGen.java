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

package com.sun.gluegen;

import java.io.*;
import java.util.*;

import antlr.*;
import antlr.collections.*;
import com.sun.gluegen.cgram.*;
import com.sun.gluegen.cgram.types.*;
import com.sun.gluegen.pcpp.*;

/** Glue code generator for C functions and data structures. */

public class GlueGen implements GlueEmitterControls {
  private java.util.List forcedStructNames = new ArrayList();
  private PCPP preprocessor;

  public void forceStructEmission(String typedefName) {
    forcedStructNames.add(typedefName);
  }

  public String findHeaderFile(String headerFileName) {
    return preprocessor.findFile(headerFileName);
  }

  public void run(String[] args) {
    try {
      Reader reader = null;
      String filename = null;
      String emitterClass = null;
      java.util.List cfgFiles = new ArrayList();

      if (args.length == 0) {
        usage();
      }

      java.util.List includePaths = new ArrayList();
      for (int i = 0; i < args.length; i++) {
        if (i < args.length - 1) {
          String arg = args[i];
          if (arg.startsWith("-I")) {
            String[] paths = arg.substring(2).split(System.getProperty("path.separator"));
            for (int j = 0; j < paths.length; j++) {
              includePaths.add(paths[j]);
            }
          } else if (arg.startsWith("-E")) {
            emitterClass = arg.substring(2);
          } else if (arg.startsWith("-C")) {
            cfgFiles.add(arg.substring(2));
          } else {
            usage();
          }
        } else {
          String arg = args[i];
          if (arg.equals("-")) {
            reader = new InputStreamReader(System.in);
            filename = "standard input";
          } else {
            if (arg.startsWith("-")) {
              usage();
            }
            filename = arg;
            reader = new BufferedReader(new FileReader(filename));
          }
        }
      }

      preprocessor = new PCPP(includePaths);
      PipedInputStream ppIn   = new PipedInputStream();
      final PipedOutputStream ppOut = new PipedOutputStream(ppIn);
      preprocessor.setOut(ppOut);
      final Reader rdr = reader;
      final String fn  = filename;
      new Thread(new Runnable() {
          public void run() {
            try {
              preprocessor.run(rdr, fn);
              ppOut.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }).start();

      DataInputStream dis = new DataInputStream(ppIn);
      GnuCLexer lexer = new GnuCLexer(dis);
      lexer.setTokenObjectClass(CToken.class.getName());
      lexer.initialize();
      // Parse the input expression.
      GnuCParser parser = new GnuCParser(lexer);
            
      // set AST node type to TNode or get nasty cast class errors
      parser.setASTNodeType(TNode.class.getName());
      TNode.setTokenVocabulary(GNUCTokenTypes.class.getName());

      // invoke parser
      try {
        parser.translationUnit();
      }
      catch (RecognitionException e) {
        System.err.println("Fatal IO error:\n"+e);
        System.exit(1);
      }
      catch (TokenStreamException e) {
        System.err.println("Fatal IO error:\n"+e);
        System.exit(1);
      }

      HeaderParser headerParser = new HeaderParser();
      TypeDictionary td = new TypeDictionary();
      headerParser.setTypedefDictionary(td);
      TypeDictionary sd = new TypeDictionary();
      headerParser.setStructDictionary(sd);
      // set AST node type to TNode or get nasty cast class errors
      headerParser.setASTNodeType(TNode.class.getName());
      // walk that tree
      headerParser.translationUnit( parser.getAST() );

      // For debugging: Dump type dictionary and struct dictionary to System.err
      //td.dumpDictionary(System.err, "All Types");
      //sd.dumpDictionary(System.err, "All Structs");
      
      // At this point we have all of the pieces we need in order to
      // generate glue code: the #defines to constants, the set of
      // typedefs, and the set of functions.

      GlueEmitter emit = null;
      if (emitterClass == null) {
        emit = new JavaEmitter();
      } else {
        try {
          emit = (GlueEmitter) Class.forName(emitterClass).newInstance();
        } catch (Exception e) {
          System.err.println("Exception occurred while instantiating emitter class. Exiting.");
          e.printStackTrace();
          System.exit(1);
        }
      }

      for (Iterator iter = cfgFiles.iterator(); iter.hasNext(); ) {
        emit.readConfigurationFile((String) iter.next());
      }

      // Provide MachineDescriptions to emitter
      MachineDescription md32 = new MachineDescription32Bit();
      MachineDescription md64 = new MachineDescription64Bit();
      emit.setMachineDescription(md32, md64);

      // begin emission of glue code
      emit.beginEmission(this);
      
      emit.beginDefines();
      Set emittedDefines = new HashSet(100);
      // emit java equivalent of enum { ... } statements
      for (Iterator iter = headerParser.getEnums().iterator(); iter.hasNext(); ) {
        EnumType enumeration = (EnumType)iter.next();
        // iterate over all values in the enumeration
        for (int i = 0; i < enumeration.getNumEnumerates(); ++i) {
          String enumElementName = enumeration.getEnumName(i);
          if (emittedDefines.contains(enumElementName) == false) {
            emittedDefines.add(enumElementName);
            String comment = null;
            if (! enumeration.getName().equals("<anonymous>")) {
              comment = "Defined as part of enum type \"" +
                enumeration.getName() + "\"";
            }
            emit.emitDefine(
              enumElementName,
              String.valueOf(enumeration.getEnumValue(i)),
              comment);
          }
        }
      }
      // emit java equivalent of #define statements
      for (Iterator iter = lexer.getDefines().iterator(); iter.hasNext(); ) {
        Define def = (Define) iter.next();
        if (emittedDefines.contains(def.getName()) == false) {
          emittedDefines.add(def.getName());
          emit.emitDefine(def.getName(), def.getValue(), null);
        }
      }
      emit.endDefines();

      java.util.List functions = headerParser.getParsedFunctions();

      // Iterate through the functions finding structs that are referenced in 
      // the function signatures; these will be remembered for later emission
      ReferencedStructs referencedStructs = new ReferencedStructs();
      for (Iterator iter = functions.iterator(); iter.hasNext(); ) {
        FunctionSymbol sym = (FunctionSymbol) iter.next();
        // FIXME: this doesn't take into account the possibility that some of
        // the functions we send to emitMethodBindings() might not actually be
        // emitted (e.g., if an Ignore directive in the JavaEmitter causes it
        // to be skipped). 
        sym.getType().visit(referencedStructs);
      }
      
      // Normally only referenced types will be emitted. The user can force a
      // type to be emitted via a .cfg file directive. Those directives are
      // processed here.
      for (Iterator iter = forcedStructNames.iterator(); iter.hasNext(); ) {
        String name = (String) iter.next();
        Type type = td.get(name);
        if (type == null) {
          System.err.println("WARNING: during forced struct emission: struct \"" + name + "\" not found");
        } else if (!type.isCompound()) {
          System.err.println("WARNING: during forced struct emission: type \"" + name + "\" was not a struct");
        } else {
          type.visit(referencedStructs);
        }
      }
      
      // Lay out structs
      emit.beginStructLayout();
      for (Iterator iter = referencedStructs.results(); iter.hasNext(); ) {
        Type t = (Type) iter.next();
        if (t.isCompound()) {
          emit.layoutStruct(t.asCompound());
        } else if (t.isPointer()) {
          PointerType p = t.asPointer();
          CompoundType c = p.getTargetType().asCompound();
          emit.layoutStruct(c);
        }
      }
      emit.endStructLayout();

      // Emit structs      
      emit.beginStructs(td, sd, headerParser.getCanonMap());
      for (Iterator iter = referencedStructs.results(); iter.hasNext(); ) {
        Type t = (Type) iter.next();
        if (t.isCompound()) {
          emit.emitStruct(t.asCompound(), null);
        } else if (t.isPointer()) {
          PointerType p = t.asPointer();
          CompoundType c = p.getTargetType().asCompound();
          assert p.hasTypedefedName() && c.getName() == null : "ReferencedStructs incorrectly recorded pointer type " + p;
          emit.emitStruct(c, p.getName());
        }
      }
      emit.endStructs();

      // emit java and C code to interface with the native functions
      emit.beginFunctions(td, sd, headerParser.getCanonMap());
      emit.emitFunctions(functions);
      emit.endFunctions();

      // end emission of glue code
      emit.endEmission();

    } catch ( Exception e ) {
      e.printStackTrace();
      System.err.println("Exception occurred while generating glue code. Exiting.");
      System.exit(1);
    }
  }
  
  public static void main(String[] args) {
      new GlueGen().run(args);
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static void usage() {
    System.out.println("Usage: java GlueGen [-I...] [-Eemitter_class_name] [-Ccfg_file_name...] <filename | ->");
    System.out.println();
    System.out.println("Runs C header parser on input file or standard input, first");
    System.out.println("passing input through minimal pseudo-C-preprocessor. Use -I");
    System.out.println("command-line arguments to specify the search path for #includes.");
    System.out.println("Emitter class name can be specified with -E option: i.e.,");
    System.out.println("-Ecom.sun.gluegen.JavaEmitter (the default). Use");
    System.out.println("-Ecom.sun.gluegen.DebugEmitter to print recognized entities");
    System.out.println("(#define directives to constant numbers, typedefs, and function");
    System.out.println("declarations) to standard output. Emitter-specific configuration");
    System.out.println("file or files can be specified with -C option; e.g,");
    System.out.println("-Cjava-emitter.cfg.");
    System.exit(1);
  }
}

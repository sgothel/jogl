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

import java.util.*;

import com.sun.gluegen.cgram.types.*;

/** Debug emitter which prints the parsing results to standard output. */

public class DebugEmitter implements GlueEmitter {
  public void readConfigurationFile(String filename) {}

  public void setMachineDescription(MachineDescription md32,
                                    MachineDescription md64) {}

  public void beginEmission(GlueEmitterControls controls) {
    System.out.println("----- BEGIN EMISSION OF GLUE CODE -----");
  }
  public void endEmission() { 
    System.out.println("----- END EMISSION OF GLUE CODE -----");
  }
  public void beginDefines() {}
  public void emitDefine(String name, String value, String optionalComment) {
    System.out.println("#define " + name + " " + value +
                       (optionalComment != null ? ("// " + optionalComment) : ""));
  }
  public void endDefines() {}
  
  public void beginFunctions(TypeDictionary typedefDictionary,
                             TypeDictionary structDictionary,
                             Map            canonMap) {
    Set keys = typedefDictionary.keySet();
    for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
      String key = (String) iter.next();
      Type value = typedefDictionary.get(key);
      System.out.println("typedef " + value + " " + key + ";");
    }
  }
  public Iterator emitFunctions(List/*<FunctionSymbol>*/ originalCFunctions)
    throws Exception {
    for (Iterator iter = originalCFunctions.iterator(); iter.hasNext(); ) {
      FunctionSymbol sym = (FunctionSymbol) iter.next();
      emitSingleFunction(sym);
    }
    return originalCFunctions.iterator();
  }
  public void emitSingleFunction(FunctionSymbol sym) {
    System.out.println(sym);
    System.out.println(" -> " + sym.toString());
  }
  public void endFunctions() {}

  public void beginStructLayout() throws Exception {}
  public void layoutStruct(CompoundType t) throws Exception {}
  public void endStructLayout() throws Exception {}

  public void beginStructs(TypeDictionary typedefDictionary,
                           TypeDictionary structDictionary,
                           Map            canonMap) {
  }
  public void emitStruct(CompoundType t, String alternateName) {
    String name = t.getName();
    if (name == null && alternateName != null) {
      name = alternateName;
    }

    System.out.println("Referenced type \"" + name + "\"");
  }
  public void endStructs() {}
}

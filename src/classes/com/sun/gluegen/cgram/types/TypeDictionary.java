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

package com.sun.gluegen.cgram.types;

import java.util.*;

/** Utility class for recording names of typedefs and structs. */

public class TypeDictionary {
  /** Mapping from type name to type.*/
  private HashMap/*<String, Type>*/ map = new HashMap/*<String, Type>*/();

  /** Reverse mapping; created lazily from the regular map */
  private HashMap/*<Set<Type>, String>*/ reverseMap = new HashMap/*<Set<Type>, String>*/();

  /** Has a type been added/removed since the last time the reverse map was
   * calculated? */
  private boolean reverseMapOutOfDate = false;

  /**
   * Create a mapping from a type to its name.
   * @param name the name to which the type is defined
   * @param type the type that can be referred to by the specified name.
   */
  public Type put(String name, Type type) {
    reverseMapOutOfDate = true;
    return (Type) map.put(name, type);
  }

  /** Get the type corresponding to the given name. Returns null if no type
   * was found corresponding to the given name. */
  public Type get(String name) {
    return (Type) map.get(name);
  }

  /**
   * Get the names that correspond to the given type. There will be more than
   * one name in the returned list if the type has been defined to multiple
   * names. Returns null if no names were found for given type.
   */
  public Set/*<String>*/ get(Type type) {
    if (reverseMapOutOfDate) {
      rebuildReverseMap();
      reverseMapOutOfDate = false;
    }
    // Don't let callers muck with the set.
    return Collections.unmodifiableSet((Set)reverseMap.get(type));
  }

  /** Remove the mapping from the specified name to its associated type.*/
  public Type remove(String name) {
    reverseMapOutOfDate = true;
    return (Type) map.remove(name);
  }

  /** Get all the names that map to Types. 
    * @return a Set of Strings that are the typedef names that map to Types in the dictionary.
  */
  public Set keySet() {
    return map.keySet();
  }

  public Set entrySet() {
    return map.entrySet();
  }

  public boolean containsKey(String key) {
    return map.containsKey(key);
  }
  
  public boolean containsValue(Type value) {
    return map.containsValue(value);
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  /** Returns a collection of all the Types in the dictionary that are mapped via typedefs names. */
  public Collection values() {
    return map.values();
  }

  /** Build the mapping of from each Type to all the names by which is may be
   * referenced. Warning: this is a slow operation!
   */ 
  private void rebuildReverseMap() {
    reverseMap.clear();
    for (Iterator/*<String>*/ it = map.keySet().iterator(); it.hasNext(); ) {
      String name = (String)it.next();
      Type type = (Type)map.get(name);
      if (type == null) {
        throw new IllegalStateException("Internal error; TypedefDictionary contains null Type for name \"" + name + "\"");
      }
      HashSet allNamesForType = (HashSet)reverseMap.get(type);
      if (allNamesForType == null) {
        allNamesForType = new HashSet/*<String>*/();
        reverseMap.put(type, allNamesForType);
      }
      allNamesForType.add(name);
    }
  }

  /**
   * Dumps the dictionary contents to the specified output stream, annotated
   * with the specified description. Useful for debugging.
   */
  public void dumpDictionary(java.io.PrintStream out, String description) {
    out.println("------------------------------------------------------------------------------");
    out.println("TypeDictionary: " + (description == null ? "" : description));
    out.println("------------------------------------------------------------------------------");
    out.println("Forward mapping: ");
    for (Iterator names = keySet().iterator(); names.hasNext(); ) {
      String typeName = (String)names.next();
      out.println("  [" + typeName + "]\t--> [" + get(typeName) + "]");
    }
    out.println("Reverse mapping: ");

    // because the reverse mapping is built lazily upon query, we must force it to
    // be built if it has not yet been built.
    if (reverseMapOutOfDate) {
      rebuildReverseMap();
      reverseMapOutOfDate = false;
    }
    for (Iterator types = reverseMap.keySet().iterator(); types.hasNext(); ) {
      Type type = (Type)types.next();
      Set names = get(type);
      out.println("  [" + type + "]\t--> " + names + "");
    }
    out.println("------------------------------------------------------------------------------");
  }
}

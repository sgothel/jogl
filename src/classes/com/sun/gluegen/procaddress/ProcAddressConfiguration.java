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

package com.sun.gluegen.procaddress;

import java.io.*;
import java.text.*;
import java.util.*;

import com.sun.gluegen.*;

public class ProcAddressConfiguration extends JavaConfiguration
{
  private boolean emitProcAddressTable = false;
  private String  tableClassPackage;
  private String  tableClassName = "ProcAddressTable";
  private Set/*<String>*/ skipProcAddressGen  = new HashSet();
  private List/*<String>*/ forceProcAddressGen  = new ArrayList();
  private String  getProcAddressTableExpr;
  private ConvNode procAddressNameConverter;

  protected void dispatch(String cmd, StringTokenizer tok, File file, String filename, int lineNo) throws IOException {
    if (cmd.equalsIgnoreCase("EmitProcAddressTable"))
      {
        emitProcAddressTable =
          readBoolean("EmitProcAddressTable", tok, filename, lineNo).booleanValue();
      }
    else if (cmd.equalsIgnoreCase("ProcAddressTablePackage"))
      {
        tableClassPackage = readString("ProcAddressTablePackage", tok, filename, lineNo);
      }
    else if (cmd.equalsIgnoreCase("ProcAddressTableClassName"))
      {
        tableClassName = readString("ProcAddressTableClassName", tok, filename, lineNo);
      }
    else if (cmd.equalsIgnoreCase("SkipProcAddressGen"))
      {
        String sym = readString("SkipProcAddressGen", tok, filename, lineNo);
        skipProcAddressGen.add(sym);
      }
    else if (cmd.equalsIgnoreCase("ForceProcAddressGen"))
      {
        String sym = readString("ForceProcAddressGen", tok, filename, lineNo);
        forceProcAddressGen.add(sym);
      }
    else if (cmd.equalsIgnoreCase("GetProcAddressTableExpr"))
      {
        getProcAddressTableExpr = readGetProcAddressTableExpr(tok, filename, lineNo);
      }
    else if (cmd.equalsIgnoreCase("ProcAddressNameExpr"))
      {
        readProcAddressNameExpr(tok, filename, lineNo);
      }
    else
      {
        super.dispatch(cmd,tok,file,filename,lineNo);
      }
  }

  protected String readGetProcAddressTableExpr(StringTokenizer tok, String filename, int lineNo) {
    try {
      String restOfLine = tok.nextToken("\n\r\f");
      return restOfLine.trim();
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"GetProcAddressTableExpr\" command at line " + lineNo +
                                 " in file \"" + filename + "\"", e);
    }
  }

  protected void setProcAddressNameExpr(String expr) {
    // Parse this into something allowing us to map from a function
    // name to the typedef'ed function pointer name
    List/*<String>*/ tokens = new ArrayList/*<String>*/();
    StringTokenizer tok1 = new StringTokenizer(expr);
    while (tok1.hasMoreTokens()) {
      String sstr = tok1.nextToken();
      StringTokenizer tok2 = new StringTokenizer(sstr, "$()", true);
      while (tok2.hasMoreTokens()) {
        tokens.add(tok2.nextToken());
      }
    }

    // Now that the string is flattened out, convert it to nodes
    procAddressNameConverter = makeConverter(tokens.iterator());
    if (procAddressNameConverter == null) {
      throw new NoSuchElementException("Error creating converter from string");
    }
  }

  protected void readProcAddressNameExpr(StringTokenizer tok, String filename, int lineNo) {
    try {
      String restOfLine = tok.nextToken("\n\r\f");
      restOfLine = restOfLine.trim();
      setProcAddressNameExpr(restOfLine);
    } catch (NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ProcAddressNameExpr\" command at line " + lineNo +
                                 " in file \"" + filename + "\"", e);
    }
  }

  private static ConvNode makeConverter(Iterator/*<String>*/ iter) {
    List/*<ConvNode>*/ result = new ArrayList/*<ConvNode>*/();
    while (iter.hasNext()) {
      String str = (String) iter.next();
      if (str.equals("$")) {
        String command = (String) iter.next();
        String openParen = (String) iter.next();
        if (!openParen.equals("(")) {
          throw new NoSuchElementException("Expected \"(\"");
        }
        boolean uppercase = false;
        if (command.equalsIgnoreCase("UPPERCASE")) {
          uppercase = true;
        } else if (!command.equalsIgnoreCase("LOWERCASE")) {
          throw new NoSuchElementException("Unknown ProcAddressNameExpr command \"" + command + "\"");
        }
        result.add(new CaseNode(uppercase, makeConverter(iter)));
      } else if (str.equals(")")) {
        // Fall through and return
      } else if (str.indexOf('{') >= 0) {
        result.add(new FormatNode(str));
      } else {
        result.add(new ConstStringNode(str));
      }
    }
    if (result.size() == 0) {
      return null;
    } else if (result.size() == 1) {
      return (ConvNode) result.get(0);
    } else {
      return new ConcatNode(result);
    }
  }

  /** Helper class for converting a function name to the typedef'ed
      function pointer name */
  static abstract class ConvNode {
    abstract String convert(String funcName);
  }

  static class FormatNode extends ConvNode {
    private MessageFormat msgFmt;

    FormatNode(String fmt) {
      msgFmt = new MessageFormat(fmt);
    }

    String convert(String funcName) {
      StringBuffer buf = new StringBuffer();
      msgFmt.format(new Object[] { funcName }, buf, null);
      return buf.toString();
    }
  }

  static class ConstStringNode extends ConvNode {
    private String str;

    ConstStringNode(String str) {
      this.str = str;
    }

    String convert(String funcName) {
      return str;
    }
  }

  static class ConcatNode extends ConvNode {
    private List/*<ConvNode>*/ children;

    ConcatNode(List/*<ConvNode>*/ children) {
      this.children = children;
    }

    String convert(String funcName) {
      StringBuffer res = new StringBuffer();
      for (Iterator iter = children.iterator(); iter.hasNext(); ) {
        ConvNode node = (ConvNode) iter.next();
        res.append(node.convert(funcName));
      }
      return res.toString();
    }
  }

  static class CaseNode extends ConvNode {
    private boolean upperCase;
    private ConvNode child;

    CaseNode(boolean upperCase, ConvNode child) {
      this.upperCase = upperCase;
      this.child = child;
    }

    public String convert(String funcName) {
      if (upperCase) {
        return child.convert(funcName).toUpperCase();
      } else {
        return child.convert(funcName).toLowerCase();
      }
    }
  }

  public boolean emitProcAddressTable()           { return emitProcAddressTable;               }
  public String  tableClassPackage()              { return tableClassPackage;                  }
  public String  tableClassName()                 { return tableClassName;                     }
  public boolean skipProcAddressGen (String name) { return skipProcAddressGen.contains(name);  }
  public List    getForceProcAddressGen()         { return forceProcAddressGen;                }
  public String  getProcAddressTableExpr() {
    if (getProcAddressTableExpr == null) {
      throw new RuntimeException("GetProcAddressTableExpr was not defined in .cfg file");
    }
    return getProcAddressTableExpr;
  }
  public String  convertToFunctionPointerName(String funcName) {
    if (procAddressNameConverter == null) {
      throw new RuntimeException("ProcAddressNameExpr was not defined in .cfg file");
    }

    return procAddressNameConverter.convert(funcName);
  }
}

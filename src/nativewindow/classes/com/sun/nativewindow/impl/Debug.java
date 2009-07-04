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

package com.sun.nativewindow.impl;

import java.security.*;

/** Helper routines for logging and debugging. */

public class Debug {
  // Some common properties
  private static boolean verbose;
  private static boolean debugAll;
  private static AccessControlContext localACC;
  
  static {
    localACC=AccessController.getContext();
    verbose = isPropertyDefined("nativewindow.verbose", true);
    debugAll = isPropertyDefined("nativewindow.debug", true);
    if (verbose) {
       Package p = Package.getPackage("javax.media.nativewindow");
       System.err.println("NativeWindow specification version " + p.getSpecificationVersion());
       System.err.println("NativeWindow implementation version " + p.getImplementationVersion());
       System.err.println("NativeWindow implementation vendor " + p.getImplementationVendor());
    }
  }

  static int getIntProperty(final String property, final boolean jnlpAlias) {
      return getIntProperty(property, jnlpAlias, localACC);
  }

  public static int getIntProperty(final String property, final boolean jnlpAlias, final AccessControlContext acc) {
    int i=0;
    try {
        Integer iv = Integer.valueOf(Debug.getProperty(property, jnlpAlias, acc));
        i = iv.intValue();
    } catch (NumberFormatException nfe) {}
    return i;
  }

  static boolean getBooleanProperty(final String property, final boolean jnlpAlias) {
    return getBooleanProperty(property, jnlpAlias, localACC);
  }

  public static boolean getBooleanProperty(final String property, final boolean jnlpAlias, final AccessControlContext acc) {
    Boolean b = Boolean.valueOf(Debug.getProperty(property, jnlpAlias, acc));
    return b.booleanValue();
  }

  static boolean isPropertyDefined(final String property, final boolean jnlpAlias) {
    return isPropertyDefined(property, jnlpAlias, localACC);
  }

  public static boolean isPropertyDefined(final String property, final boolean jnlpAlias, final AccessControlContext acc) {
    return (Debug.getProperty(property, jnlpAlias, acc) != null) ? true : false;
  }

  static String getProperty(final String property, final boolean jnlpAlias) {
    return getProperty(property, jnlpAlias, localACC);
  }

  public static String getProperty(final String property, final boolean jnlpAlias, final AccessControlContext acc) {
    String s=null;
    if(null!=acc && acc.equals(localACC)) {
        s = (String) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
              String val=null;
              try {
                  val = System.getProperty(property);
              } catch (Exception e) {}
              if(null==val && jnlpAlias && !property.startsWith(jnlp_prefix)) {
                  try {
                      val = System.getProperty(jnlp_prefix + property);
                  } catch (Exception e) {}
              }
              return val;
            }
          });
    } else {
        try {
            s = System.getProperty(property);
        } catch (Exception e) {}
        if(null==s && jnlpAlias && !property.startsWith(jnlp_prefix)) {
            try {
                s = System.getProperty(jnlp_prefix + property);
            } catch (Exception e) {}
        }
    }
    return s;
  }
  public static final String jnlp_prefix = "jnlp." ;

  public static boolean verbose() {
    return verbose;
  }

  public static boolean debugAll() {
    return debugAll;
  }

  public static boolean debug(String subcomponent) {
    return debugAll() || isPropertyDefined("nativewindow.debug." + subcomponent, true);
  }
}

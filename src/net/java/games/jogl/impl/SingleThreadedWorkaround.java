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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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

package net.java.games.jogl.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;

/** Encapsulates the workaround of running all display operations on
    the AWT event queue thread for the purposes of working around
    problems seen primarily on ATI cards when rendering into a surface
    that is simultaneously being resized by the event queue thread */

public class SingleThreadedWorkaround {
  private static boolean ATI_WORKAROUND = false;
  // If the user specified the workaround's system property (either
  // true or false), don't let the automatic detection have any effect
  private static boolean systemPropertySpecified = false;
  private static boolean verbose = false;
  
  static {
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          String workaround = System.getProperty("ATI_WORKAROUND");
          if (workaround != null) {
            systemPropertySpecified = true;
            ATI_WORKAROUND = Boolean.valueOf(workaround).booleanValue();
          }
          verbose = (System.getProperty("jogl.verbose") != null);
          printWorkaroundNotice();
          return null;
        }
      });
  }

  public static void shouldDoWorkaround() {
    if (!systemPropertySpecified) {
      ATI_WORKAROUND = true;
      printWorkaroundNotice();
    }
  }

  public static boolean doWorkaround() {
    return ATI_WORKAROUND;
  }

  private static void printWorkaroundNotice() {
    if (ATI_WORKAROUND && verbose) {
      System.err.println("Using ATI workaround of dispatching display() on event thread");
    }
  }
}

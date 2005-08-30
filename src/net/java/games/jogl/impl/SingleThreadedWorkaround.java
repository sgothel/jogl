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

package net.java.games.jogl.impl;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import net.java.games.jogl.*;

/** Encapsulates the workaround of running all display operations on
    the AWT event queue thread for the purposes of working around
    problems seen primarily on ATI cards when rendering into a surface
    that is simultaneously being resized by the event queue thread.
    <p>

    As of JOGL 1.1 b10, this property defaults to true. Problems have
    been seen on Windows, Linux and Mac OS X platforms that are solved
    by switching all OpenGL work to a single thread, which this
    workaround provides. The forthcoming JSR-231 work will rethink how
    such a mechanism is implemented, but the core result of needing to
    perform all OpenGL work on a single thread for best compatibility
    will remain.
*/

public class SingleThreadedWorkaround {
  private static boolean singleThreadedWorkaround = true;
  // If the user specified the workaround's system property (either
  // true or false), don't let the automatic detection have any effect
  private static boolean systemPropertySpecified = false;
  
  static {
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          String workaround = System.getProperty("jogl.1thread");
          if (workaround == null) {
            // Old system property (for compatibility)
            workaround = System.getProperty("JOGL_SINGLE_THREADED_WORKAROUND");
          }
          if (workaround == null) {
            // Older system property (for compatibility)
            workaround = System.getProperty("ATI_WORKAROUND");
          }
          if (workaround != null && (!workaround.equals("auto"))) {
            systemPropertySpecified = true;
            singleThreadedWorkaround = Boolean.valueOf(workaround).booleanValue();
          }
          printWorkaroundNotice();
          return null;
        }
      });
  }

  /** Public method for users to disable the single-threaded
      workaround in application code. Should perhaps eventually
      promote this method to the public API. */
  public static void disableWorkaround() {
    systemPropertySpecified = true;
    singleThreadedWorkaround = false;
    if (Debug.verbose()) {
      System.err.println("Application forced disabling of single-threaded workaround of dispatching display() on event thread");
    }
  }

  public static void shouldDoWorkaround() {
    if (!systemPropertySpecified) {
      singleThreadedWorkaround = true;
      printWorkaroundNotice();
    }
  }

  public static boolean doWorkaround() {
    return singleThreadedWorkaround;
  }

  public static boolean isOpenGLThread() {
    return EventQueue.isDispatchThread();
  }

  public static void invokeOnOpenGLThread(Runnable r) throws GLException {
    try {
      EventQueue.invokeAndWait(r);
    } catch (InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (InterruptedException e) {
      throw new GLException(e);
    }
  }

  private static void printWorkaroundNotice() {
    if (singleThreadedWorkaround && Debug.verbose()) {
      System.err.println("Using single-threaded workaround of dispatching display() on event thread");
    }
  }
}

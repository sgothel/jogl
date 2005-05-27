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

import java.security.*;

public class NativeLibLoader {
  private static volatile boolean doLoading   = true;
  private static volatile boolean doneLoading = false;

  public static void disableLoading() {
    doLoading = false;
  }

  public static void enableLoading() {
    doLoading = true;
  }

  public static synchronized void load() {
    if (doLoading && !doneLoading) {
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            boolean isOSX = System.getProperty("os.name").equals("Mac OS X");
            if (!isOSX) {
              try {
                System.loadLibrary("jawt");
              } catch (UnsatisfiedLinkError e) {
                // Accessibility technologies load JAWT themselves; safe to continue
                // as long as JAWT is loaded by any loader
                if (e.getMessage().indexOf("already loaded") == -1) {
                  throw e;
                }
              }
            }
            System.loadLibrary("jogl");

            // Workaround for 4845371.
            // Make sure the first reference to the JNI GetDirectBufferAddress is done
            // from a privileged context so the VM's internal class lookups will succeed.
            JAWT jawt = new JAWT();
            JAWTFactory.JAWT_GetAWT(jawt);

            return null;
          }
        });
      doneLoading = true;
    }
  }
}

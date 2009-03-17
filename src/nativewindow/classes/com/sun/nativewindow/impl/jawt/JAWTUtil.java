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
 */

package com.sun.nativewindow.impl.jawt;

import com.sun.nativewindow.impl.*;

import javax.media.nativewindow.*;

import java.awt.GraphicsEnvironment;

public class JAWTUtil {
  static {
        NativeLibLoaderBase.loadNativeWindow("awt");
  }

  // See whether we're running in headless mode
  private static boolean headlessMode;

  static {
    lockedToolkit = false;
    headlessMode = GraphicsEnvironment.isHeadless();
  }

  private static boolean lockedToolkit;

  public static synchronized void lockToolkit() throws NWException {
    if (lockedToolkit) {
      throw new NWException("Toolkit already locked");
    }
    lockedToolkit = true;

    if (headlessMode) {
      // Workaround for running (to some degree) in headless
      // environments but still supporting rendering via pbuffers
      // For full correctness, would need to implement a Lock class
      return;
    }

    JAWT.getJAWT().Lock();
  }

  public static synchronized void unlockToolkit() {
    if (lockedToolkit) {
        if (headlessMode) {
          // Workaround for running (to some degree) in headless
          // environments but still supporting rendering via pbuffers
          // For full correctness, would need to implement a Lock class
          return;
        }

        JAWT.getJAWT().Unlock();
        lockedToolkit = false;
    }
  }

  public static boolean isToolkitLocked() {
    return lockedToolkit;
  }
}


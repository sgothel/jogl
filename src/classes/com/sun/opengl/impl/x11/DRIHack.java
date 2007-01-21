/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.opengl.impl.x11;

import java.io.*;
import java.security.*;
import com.sun.gluegen.runtime.*;
import com.sun.opengl.impl.*;

/**
 * Helper class for working around problems with open-source DRI
 * drivers. In the current DRI implementation it is required that the
 * symbols in libGL.so.1.2 be globally visible to be accessible from
 * other libraries that are dynamically loaded by the implementation.
 * Applications may typically satisfy this need either by linking
 * against libGL.so on the command line (-lGL) or by dlopen'ing
 * libGL.so.1.2 with the RTLD_GLOBAL flag. The JOGL implementation
 * links against libGL on all platforms rather than forcing all OpenGL
 * entry points to be called through a function pointer. This allows
 * the JOGL library to link directly to core 1.1 OpenGL entry points
 * like glVertex3f, while calling through function pointers for entry
 * points from later OpenGL versions as well as from
 * extensions. However, because libjogl.so (which links against
 * libGL.so) is loaded by the JVM, and because the JVM implicitly uses
 * RTLD_LOCAL in the implementation of System.loadLibrary(), this
 * means via transitivity that the symbols for libGL.so have only
 * RTLD_LOCAL visibility to the rest of the application, so the DRI
 * drivers can not find the symbols required. <P>
 *
 * There are at least two possible solutions. One would be to change
 * the JOGL implementation to call through function pointers uniformly
 * so that it does not need to link against libGL.so. This is
 * possible, but requires changes to GlueGen and also is not really
 * necessary in any other situation than with the DRI drivers. Another
 * solution is to force the first load of libGL.so.1.2 to be done
 * dynamically with RTLD_GLOBAL before libjogl.so is loaded and causes
 * libGL.so.1.2 to be loaded again. The NativeLibrary class in the
 * GlueGen runtime has this property, and we use it to implement this
 * workaround.
 */

public class DRIHack {
  private static final boolean DEBUG = Debug.debug("DRIHack");
  private static boolean driHackNeeded;
  private static NativeLibrary oglLib;

  public static void begin() {
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          String os = System.getProperty("os.name").toLowerCase();
          // Do DRI hack on all Linux distributions for best robustness
          driHackNeeded =
            (os.startsWith("linux") ||
             new File("/usr/lib/dri").exists() ||
             new File("/usr/X11R6/lib/modules/dri").exists());
          // Allow manual overriding for now as a workaround for
          // problems seen in some situations -- needs more investigation
          if (System.getProperty("jogl.drihack.disable") != null) {
            driHackNeeded = false;
          }
          return null;
        }
      });

    if (driHackNeeded) {
      if (DEBUG) {
        System.err.println("Beginning DRI hack");
      }

      // Try a few different variants for best robustness
      // In theory probably only the first is necessary
      oglLib = NativeLibrary.open("libGL.so.1", null);
      if (DEBUG && oglLib != null) System.err.println(" Found libGL.so.1");
      if (oglLib == null) {
        oglLib = NativeLibrary.open("/usr/lib/libGL.so.1", null);
        if (DEBUG && oglLib != null) System.err.println(" Found /usr/lib/libGL.so.1");
      }
    }
  }

  public static void end() {
    if (oglLib != null) {
      if (DEBUG) {
        System.err.println("Ending DRI hack");
      }

      oglLib.close();
      oglLib = null;
    }
  }
}

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
import java.lang.reflect.*;

public class JAWTUtil {

  // See whether we're running in headless mode
  private static final boolean headlessMode;

  // Java2D magic ..
  private static final Class j2dClazz;
  private static final Method isQueueFlusherThread;
  private static final boolean j2dExist;

  static {
    JAWTNativeLibLoader.loadAWTImpl();
    JAWTNativeLibLoader.loadNativeWindow("awt");

    lockedStack   = null;
    headlessMode = GraphicsEnvironment.isHeadless();

    boolean ok=false;
    Class jC=null;
    Method m=null;
    if(!headlessMode) {
        try {
            jC = Class.forName("com.sun.opengl.impl.awt.Java2D");
            m = jC.getMethod("isQueueFlusherThread", null);
            ok = true;
        } catch (Exception e) {}
    }
    j2dClazz = jC;
    isQueueFlusherThread = m;
    j2dExist = ok;
  }

  private static Exception lockedStack;

  // Just a hook to let this class being initialized,
  // ie loading the native libraries ..
  public static void init() { }

  public static final boolean hasJava2D() {
    return j2dExist;
  }

  public static final boolean isJava2DQueueFlusherThread() {
    boolean b = false;
    if(j2dExist) {
        try {
            b = ((Boolean)isQueueFlusherThread.invoke(null, null)).booleanValue();
        } catch (Exception e) {}
    }
    return b;
  }

  public static boolean isHeadlessMode() {
    return headlessMode;
  }

  public static synchronized void lockToolkit() throws NativeWindowException {
    if (isJava2DQueueFlusherThread()) return;

    if (null!=lockedStack) {
      lockedStack.printStackTrace();
      throw new NativeWindowException("JAWT Toolkit already locked - "+Thread.currentThread().getName());
    }
    lockedStack = new Exception("JAWT Toolkit already locked by: "+Thread.currentThread().getName());

    if (headlessMode) {
      // Workaround for running (to some degree) in headless
      // environments but still supporting rendering via pbuffers
      // For full correctness, would need to implement a Lock class
      return;
    }

    JAWT.getJAWT().Lock();
  }

  public static synchronized void unlockToolkit() {
    if (isJava2DQueueFlusherThread()) return;

    if (null!=lockedStack) {
        lockedStack = null;
        if (headlessMode) {
          // Workaround for running (to some degree) in headless
          // environments but still supporting rendering via pbuffers
          // For full correctness, would need to implement a Lock class
          return;
        }

        JAWT.getJAWT().Unlock();
    }
  }

  public static boolean isToolkitLocked() {
    return null!=lockedStack;
  }

  public static Exception getLockedStack() {
    return lockedStack;
  }
}


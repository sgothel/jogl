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

package com.sun.javafx.newt.impl;

// FIXME: refactor Java SE dependencies
//import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import com.sun.nativewindow.impl.NativeLibLoaderBase;

public class NativeLibLoader extends NativeLibLoaderBase {
  
  public static void loadNEWT() {
    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        String[] preload = { "nativewindow" };
        loadLibrary("newt", preload, true);
        return null;
      }
    });
  }

  //----------------------------------------------------------------------
  // Support for the new JNLPAppletLauncher
  //

  private static class NEWTAction implements NativeLibLoaderBase.LoaderAction {
    public void loadLibrary(String libname, String[] preload,
        boolean preloadIgnoreError) {
      if (null!=preload) {
        for (int i=0; i<preload.length; i++) {
          if(!isLoaded(preload[i])) {
              try {
                if(DEBUG) {
                    System.err.println("NEWT NativeLibLoader preload "+preload[i]);
                }
                loadLibraryInternal(preload[i]);
                addLoaded(preload[i]);
              }
              catch (UnsatisfiedLinkError e) {
                if (!preloadIgnoreError && e.getMessage().indexOf("already loaded") < 0) {
                  throw e;
                }
              }
          }
        }
      }
      
      if(DEBUG) {
        System.err.println("NEWT NativeLibLoader    load "+libname);
      }
      loadLibraryInternal(libname);
      addLoaded(libname);
    }
  }

  private static boolean usingJNLPAppletLauncher;
  private static Method  jnlpLoadLibraryMethod;

  static {
    NativeLibLoaderBase.setLoadingAction(new NEWTAction());
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          String sunAppletLauncher = System.getProperty("sun.jnlp.applet.launcher");
          usingJNLPAppletLauncher = Boolean.valueOf(sunAppletLauncher).booleanValue();
          return null;
        }
      });
  }

  // I hate the amount of delegation currently in this class
  private static void loadLibraryInternal(String libraryName) {
    // Note: special-casing JAWT which is built in to the JDK
    if (usingJNLPAppletLauncher && !libraryName.equals("jawt")) {
        try {
          if (jnlpLoadLibraryMethod == null) {
            Class jnlpAppletLauncherClass = Class.forName("org.jdesktop.applet.util.JNLPAppletLauncher");
            jnlpLoadLibraryMethod = jnlpAppletLauncherClass.getDeclaredMethod("loadLibrary", new Class[] { String.class });
          }
          jnlpLoadLibraryMethod.invoke(null, new Object[] { libraryName });
        } catch (Exception e) {
          Throwable t = e;
          if (t instanceof InvocationTargetException) {
            t = ((InvocationTargetException) t).getTargetException();
          }
          if (t instanceof Error)
            throw (Error) t;
          if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
          }
          // Throw UnsatisfiedLinkError for best compatibility with System.loadLibrary()
          throw (UnsatisfiedLinkError) new UnsatisfiedLinkError().initCause(e);
        }
    } else {
      // FIXME: remove
      // System.out.println("sun.boot.library.path=" + System.getProperty("sun.boot.library.path"));
      System.loadLibrary(libraryName);
      if(DEBUG) {
          System.err.println("NEWT NativeLibLoader loaded "+libraryName);
      }
    }
  }
}

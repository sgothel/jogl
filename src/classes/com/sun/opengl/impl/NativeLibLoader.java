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

package com.sun.opengl.impl;

import java.awt.Toolkit;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;

public class NativeLibLoader {
  public interface LoaderAction {
    /**
     * Loads the library specified by libname. Optionally preloads the libraries specified by
     * preload. The implementation should ignore, if the preload-libraries have already been
     * loaded.
     * @param libname the library to load
     * @param preload the libraries to load before loading the main library
     * @param doPreload true, iff the preload-libraries should be loaded 
     * @param ignoreError true, iff errors during loading the preload-libraries should be ignored 
     */
    void loadLibrary(String libname, String[] preload, 
        boolean doPreload, boolean ignoreError);
  }
  
  private static class DefaultAction implements LoaderAction {
    public void loadLibrary(String libname, String[] preload,
        boolean doPreload, boolean ignoreError) {
      if (doPreload) {
        for (int i=0; i<preload.length; i++) {
          try {
            loadLibraryInternal(preload[i]);
          }
          catch (UnsatisfiedLinkError e) {
            if (!ignoreError && e.getMessage().indexOf("already loaded") < 0) {
              throw e;
            }
          }
        }
      }
      
      loadLibraryInternal(libname);
    }
  }
  
  private static final HashSet loaded = new HashSet();
  private static LoaderAction loaderAction = new DefaultAction();

  public static void disableLoading() {
    setLoadingAction(null);
  }

  public static void enableLoading() {
    setLoadingAction(new DefaultAction());
  }
  
  public static synchronized void setLoadingAction(LoaderAction action) {
    loaderAction = action;
  }

  private static synchronized void loadLibrary(String libname, String[] preload, 
      boolean doPreload, boolean ignoreError) {
    if (loaderAction != null && !loaded.contains(libname))
    {
      loaderAction.loadLibrary(libname, preload, doPreload, ignoreError);    
      loaded.add(libname);
    }
  }
  
  public static void loadCore() {
    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        loadLibrary("jogl", null, false, false);
        return null;
      }
    });
  }

  public static void loadAWTImpl() {
    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        // Make sure that awt.dll is loaded before loading jawt.dll. Otherwise
        // a Dialog with "awt.dll not found" might pop up.
        // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4481947.
        Toolkit.getDefaultToolkit();
        
        // Must pre-load JAWT on all non-Mac platforms to
        // ensure references from jogl_awt shared object
        // will succeed since JAWT shared object isn't in
        // default library path
        boolean isOSX = System.getProperty("os.name").equals("Mac OS X");
        String[] preload = { "jawt" };

        loadLibrary("jogl_awt", preload, !isOSX, false);
        return null;
      }
    });
  }

  public static void loadCgImpl() {
    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        String[] preload = { "cg", "cgGL" };
        loadLibrary("jogl_cg", preload, true, true);
        return null;
      }
    });
  }

  //----------------------------------------------------------------------
  // Support for the new JNLPAppletLauncher
  //

  private static boolean usingJNLPAppletLauncher;
  private static Method  jnlpLoadLibraryMethod;

  static {
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
          throw new RuntimeException(e);
        }
    } else {
      System.loadLibrary(libraryName);
    }
  }
}

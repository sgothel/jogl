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

package com.sun.nativewindow.impl;

// FIXME: refactor Java SE dependencies
//import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;

public class NativeLibLoaderBase {
  public static final boolean DEBUG = Debug.debug("NativeLibLoader");

  public interface LoaderAction {
    /**
     * Loads the library specified by libname. Optionally preloads the libraries specified by
     * preload. The implementation should ignore, if the preload-libraries have already been
     * loaded.
     * @param libname the library to load
     * @param preload the libraries to load before loading the main library if not null
     * @param preloadIgnoreError true, if errors during loading the preload-libraries should be ignored 
     */
    void loadLibrary(String libname, String[] preload, 
        boolean preloadIgnoreError);
  }
  
  private static class DefaultAction implements LoaderAction {
    public void loadLibrary(String libname, String[] preload,
        boolean preloadIgnoreError) {
      if (null!=preload) {
        for (int i=0; i<preload.length; i++) {
          if(!isLoaded(preload[i])) {
              try {
                System.loadLibrary(preload[i]);
                addLoaded(preload[i]);
                if(DEBUG) {
                    System.err.println("NativeLibLoaderBase preloaded "+preload[i]);
                }
              }
              catch (UnsatisfiedLinkError e) {
                if(DEBUG) e.printStackTrace();
                if (!preloadIgnoreError && e.getMessage().indexOf("already loaded") < 0) {
                  throw e;
                }
              }
          }
        }
      }
      System.loadLibrary(libname);
      addLoaded(libname);
      if(DEBUG) {
          System.err.println("NativeLibLoaderBase    loaded "+libname);
      }
    }
  }

  private static final HashSet loaded = new HashSet();
  private static LoaderAction loaderAction = new DefaultAction();

  public static boolean isLoaded(String libName) {
    return loaded.contains(libName);
  }

  public static void addLoaded(String libName) {
    loaded.add(libName);
    if(DEBUG) {
        System.err.println("NativeLibLoaderBase Loaded Native Library: "+libName);
    }
  }

  public static void disableLoading() {
    setLoadingAction(null);
  }

  public static void enableLoading() {
    setLoadingAction(new DefaultAction());
  }
  
  public static synchronized void setLoadingAction(LoaderAction action) {
    loaderAction = action;
  }

  protected static synchronized void loadLibrary(String libname, String[] preload, 
      boolean preloadIgnoreError) {
    if (loaderAction != null && !isLoaded(libname))
    {
      loaderAction.loadLibrary(libname, preload, preloadIgnoreError);    
    }
  }
  
  public static void loadNativeWindow(final String ossuffix) {
    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        loadLibrary("nativewindow_"+ossuffix, null, false);
        return null;
      }
    });
  }
}

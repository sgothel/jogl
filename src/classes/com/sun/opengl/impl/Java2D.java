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

package com.sun.opengl.impl;

import java.awt.*;
import java.lang.reflect.*;
import java.security.*;

import javax.media.opengl.*;

/** Defines integration with the Java2D OpenGL pipeline. This
    integration is only supported in 1.6 and is highly experimental. */

public class Java2D {
  private static boolean DEBUG = Debug.debug("Java2D");
  private static boolean VERBOSE = Debug.verbose();
  private static boolean isOGLPipelineActive;
  private static Method invokeWithOGLContextCurrentMethod;
  private static Method isQueueFlusherThreadMethod;
  private static Method getOGLViewportMethod;
  private static Method getOGLScissorBoxMethod;
  private static Method getOGLSurfaceIdentifierMethod;

  static {
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          if (DEBUG && VERBOSE) {
            System.err.println("Checking for Java2D/OpenGL support");
          }
          // Figure out whether the default graphics configuration is an
          // OpenGL graphics configuration
          GraphicsConfiguration cfg =
            GraphicsEnvironment.getLocalGraphicsEnvironment().
            getDefaultScreenDevice().
            getDefaultConfiguration();
          String name = cfg.getClass().getName();
          if (DEBUG && VERBOSE) {
            System.err.println("Java2D support: default GraphicsConfiguration = " + name);
          }
          isOGLPipelineActive = (name.startsWith("sun.java2d.opengl"));

          if (isOGLPipelineActive) {
            try {
              // Try to get methods we need to integrate
              Class utils = Class.forName("sun.java2d.opengl.OGLUtilities");
              invokeWithOGLContextCurrentMethod = utils.getDeclaredMethod("invokeWithOGLContextCurrent",
                                                                          new Class[] {
                                                                            Graphics.class,
                                                                            Runnable.class
                                                                          });
              invokeWithOGLContextCurrentMethod.setAccessible(true);

              isQueueFlusherThreadMethod = utils.getDeclaredMethod("isQueueFlusherThread",
                                                                   new Class[] {});
              isQueueFlusherThreadMethod.setAccessible(true);

              getOGLViewportMethod = utils.getDeclaredMethod("getOGLViewport",
                                                             new Class[] {
                                                               Graphics.class,
                                                               Integer.TYPE,
                                                               Integer.TYPE
                                                             });
              getOGLViewportMethod.setAccessible(true);

              getOGLScissorBoxMethod = utils.getDeclaredMethod("getOGLScissorBox",
                                                               new Class[] {
                                                                 Graphics.class
                                                               });
              getOGLScissorBoxMethod.setAccessible(true);

              getOGLSurfaceIdentifierMethod = utils.getDeclaredMethod("getOGLSurfaceIdentifier",
                                                                      new Class[] {
                                                                        Graphics.class
                                                                      });
              getOGLSurfaceIdentifierMethod.setAccessible(true);
            } catch (Exception e) {
              if (DEBUG && VERBOSE) {
                e.printStackTrace();
              }
              isOGLPipelineActive = false;
            }
          }

          if (DEBUG) {
            System.err.println("JOGL/Java2D integration " + (isOGLPipelineActive ? "enabled" : "disabled"));
          }
          return null;
        }
      });
  }

  public static boolean isOGLPipelineActive() {
    return isOGLPipelineActive;
  }

  public static boolean isQueueFlusherThread() {
    if (!isOGLPipelineActive()) {
      throw new GLException("Java2D OpenGL pipeline not active (or necessary support not present)");
    }

    try {
      return ((Boolean) isQueueFlusherThreadMethod.invoke(null, new Object[] {})).booleanValue();
    } catch (InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }
  
  /** Makes current the OpenGL context associated with the passed
      Graphics object and runs the given Runnable on the Queue
      Flushing Thread in one atomic action. */
  public static void invokeWithOGLContextCurrent(Graphics g, Runnable r) throws GLException {
    if (!isOGLPipelineActive()) {
      throw new GLException("Java2D OpenGL pipeline not active (or necessary support not present)");
    }

    try {
      GLDrawableFactoryImpl.getFactoryImpl().lockAWTForJava2D();
      try {
        invokeWithOGLContextCurrentMethod.invoke(null, new Object[] {g, r});
      } finally {
        GLDrawableFactoryImpl.getFactoryImpl().unlockAWTForJava2D();
      }
    } catch (InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }

  public static Rectangle getOGLViewport(Graphics g,
                                         int componentWidth,
                                         int componentHeight) {
    if (!isOGLPipelineActive()) {
      throw new GLException("Java2D OpenGL pipeline not active (or necessary support not present)");
    }

    try {
      return (Rectangle) getOGLViewportMethod.invoke(null, new Object[] {g,
                                                                         new Integer(componentWidth),
                                                                         new Integer(componentHeight)});
    } catch (InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }

  public static Rectangle getOGLScissorBox(Graphics g) {
    if (!isOGLPipelineActive()) {
      throw new GLException("Java2D OpenGL pipeline not active (or necessary support not present)");
    }

    try {
      return (Rectangle) getOGLScissorBoxMethod.invoke(null, new Object[] {g});
    } catch (InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }


  public static Object getOGLSurfaceIdentifier(Graphics g) {
    if (!isOGLPipelineActive()) {
      throw new GLException("Java2D OpenGL pipeline not active (or necessary support not present)");
    }

    try {
      return getOGLSurfaceIdentifierMethod.invoke(null, new Object[] {g});
    } catch (InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }
}

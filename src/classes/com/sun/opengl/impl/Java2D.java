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
import java.awt.image.*;
import java.lang.reflect.*;
import java.security.*;

import javax.media.opengl.*;

/** Defines integration with the Java2D OpenGL pipeline. This
    integration is only supported in 1.6 and is highly experimental. */

public class Java2D {
  private static boolean DEBUG = Debug.debug("Java2D");
  private static boolean VERBOSE = Debug.verbose();
  private static boolean isOGLPipelineActive;
  private static boolean isFBOEnabled;
  private static Method invokeWithOGLContextCurrentMethod;
  private static Method isQueueFlusherThreadMethod;
  private static Method getOGLViewportMethod;
  private static Method getOGLScissorBoxMethod;
  private static Method getOGLSurfaceIdentifierMethod;

  // If FBOs are enabled in the Java2D/OpenGL pipeline, all contexts
  // created by JOGL must share textures and display lists with the
  // Java2D contexts in order to access the frame buffer object for
  // potential rendering, and to simultaneously support sharing of
  // textures and display lists with one another. Java2D has the
  // notion of a single shared context with which all other contexts
  // (on the same display device?) share textures and display lists;
  // this is an approximation to that notion which will be refined
  // later.
  private static VolatileImage j2dFBOVolatileImage; // just a dummy image
  private static GLContext j2dFBOShareContext;

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

              String fbo = System.getProperty("sun.java2d.opengl.fbobject");
              isFBOEnabled = (fbo != null) && "true".equals(fbo);
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

  public static boolean isFBOEnabled() {
    return isFBOEnabled;
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

  /** Returns the OpenGL viewport associated with the given Graphics
      object, assuming that the Graphics object is associated with a
      component of the specified width and height. The user should
      call glViewport() with the returned rectangle's bounds in order
      to get correct rendering results. Should only be called from the
      Queue Flusher Thread. */
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

  /** Returns the OpenGL scissor region associated with the given
      Graphics object, taking into account all clipping regions, etc.
      To avoid destroying Java2D's previous rendering results, this
      method should be called and the resulting rectangle's bounds
      passed to a call to glScissor(). Should only be called from the
      Queue Flusher Thread. */
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

  /** Returns an opaque "surface identifier" associated with the given
      Graphics object. If this changes from invocation to invocation,
      the underlying OpenGL drawable for the Graphics object has
      changed and a new external GLDrawable and GLContext should be
      created (and the old ones destroyed). Should only be called from
      the Queue Flusher Thread.*/
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

  /** Returns either the given GLContext or a substitute one with
      which clients should share textures and display lists. Needed
      when the Java2D/OpenGL pipeline is active and FBOs are being
      used for rendering. FIXME: may need to alter the API in the
      future to indicate which GraphicsDevice the source context is
      associated with. */
  public static GLContext filterShareContext(GLContext shareContext) {
    initFBOShareContext();
    if (j2dFBOShareContext != null) {
      return j2dFBOShareContext;
    }
    return shareContext;
  }

  /** Returns the GLContext associated with the Java2D "share
      context", with which all contexts created by JOGL must share
      textures and display lists when the FBO option is enabled for
      the Java2D/OpenGL pipeline. */
  public static GLContext getShareContext() {
    initFBOShareContext();
    return j2dFBOShareContext;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static void initFBOShareContext() {
    // Note 1: this must not be done in the static initalizer due to
    // deadlock problems.

    // Note 2: the first execution of this method must not be from the
    // Java2D Queue Flusher Thread.

    // Note that at this point it's basically impossible that we're
    // executing on the Queue Flusher Thread since all calls (even
    // from end users) should be going through this interface and
    // we're still in the static initializer
    if (isOGLPipelineActive() &&
        isFBOEnabled() &&
        j2dFBOVolatileImage == null) {
      // Create a compatible VolatileImage (FIXME: may need one per
      // display device, and may need to create them lazily, which may
      // cause problems) and create a JOGL GLContext to wrap its
      // GLContext.
      //
      // FIXME: this technique is not really adequate. The
      // VolatileImage may be punted at any time, meaning that its
      // OpenGL context will be destroyed and any shares of
      // server-side objects with it will be gone. This context is
      // currently the "pinch point" through which all of the shares
      // with the set of contexts created by JOGL go through. Java2D
      // has the notion of its own share context with which all of the
      // contexts it creates internally share server-side objects;
      // what is really needed is another API in OGLUtilities to
      // invoke a Runnable with that share context current rather than
      // the context associated with a particular Graphics object, so
      // that JOGL can grab a handle to that persistent context.
      j2dFBOVolatileImage =
        GraphicsEnvironment.
          getLocalGraphicsEnvironment().
          getDefaultScreenDevice().
          getDefaultConfiguration().
          createCompatibleVolatileImage(2, 2);
      invokeWithOGLContextCurrent(j2dFBOVolatileImage.getGraphics(), new Runnable() {
          public void run() {
            j2dFBOShareContext = GLDrawableFactory.getFactory().createExternalGLContext();
          }
        });
    }
  }
}

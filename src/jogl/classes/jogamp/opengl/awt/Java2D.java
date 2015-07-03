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

package jogamp.opengl.awt;

import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import com.jogamp.common.os.Platform;

import jogamp.common.os.PlatformPropsImpl;
import jogamp.opengl.Debug;


/** Defines integration with the Java2D OpenGL pipeline. This
    integration is only supported in 1.6 and is highly experimental. */

public class Java2D {
  private static boolean DEBUG = Debug.debug("Java2D");
  private static boolean isHeadless;
  private static boolean isOGLPipelineActive;
  private static boolean isOGLPipelineResourceCompatible;
  private static Method invokeWithOGLContextCurrentMethod;
  private static Method isQueueFlusherThreadMethod;
  private static Method getOGLViewportMethod;
  private static Method getOGLScissorBoxMethod;
  private static Method getOGLSurfaceIdentifierMethod;
  // This one is currently optional and is only in very recent Mustang builds
  private static Method getOGLTextureTypeMethod;

  // The following methods and fields are needed for proper support of
  // Frame Buffer Objects in the Java2D/OpenGL pipeline
  // (-Dsun.java2d.opengl.fbobject=true)
  private static boolean fbObjectSupportInitialized;
  private static Method invokeWithOGLSharedContextCurrentMethod;
  private static Method getOGLSurfaceTypeMethod;

  // Publicly-visible constants for OpenGL surface types
  public static final int UNDEFINED       = getOGLUtilitiesIntField("UNDEFINED");
  public static final int WINDOW          = getOGLUtilitiesIntField("WINDOW");
  public static final int PBUFFER         = getOGLUtilitiesIntField("PBUFFER");
  public static final int TEXTURE         = getOGLUtilitiesIntField("TEXTURE");
  public static final int FLIP_BACKBUFFER = getOGLUtilitiesIntField("FLIP_BACKBUFFER");
  public static final int FBOBJECT        = getOGLUtilitiesIntField("FBOBJECT");

  // If FBOs are enabled in the Java2D/OpenGL pipeline, all contexts
  // created by JOGL must share textures and display lists with the
  // Java2D contexts in order to access the frame buffer object for
  // potential rendering, and to simultaneously support sharing of
  // textures and display lists with one another. Java2D has the
  // notion of a single shared context with which all other contexts
  // (on the same display device?) share textures and display lists;
  // this is an approximation to that notion which will be refined
  // later.
  private static boolean initializedJ2DFBOShareContext;
  private static GLContext j2dFBOShareContext;

  // Accessors for new methods in sun.java2d.opengl.CGLSurfaceData
  // class on OS X for enabling bridge
  //  public static long    createOGLContextOnSurface(Graphics g, long ctx);
  //  public static boolean makeOGLContextCurrentOnSurface(Graphics g, long ctx);
  //  public static void    destroyOGLContext(long ctx);
  private static Method createOGLContextOnSurfaceMethod;
  private static Method makeOGLContextCurrentOnSurfaceMethod;
  private static Method destroyOGLContextMethod;

  static {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
        @Override
        public Object run() {
          if (DEBUG) {
            System.err.println("Checking for Java2D/OpenGL support");
          }
          Throwable caught = null;
          try {
            isHeadless = true;
            // Figure out whether the default graphics configuration is an
            // OpenGL graphics configuration
            final GraphicsConfiguration cfg;
            final String cfgName;
            final boolean java2dOGLDisabledByOS = PlatformPropsImpl.OS_TYPE == Platform.OSType.MACOS;
            final boolean java2dOGLDisabledByProp;
            {
                boolean enabled = true;
                final String sVal = System.getProperty("sun.java2d.opengl");
                if( null != sVal ) {
                    enabled = Boolean.valueOf(sVal);
                }
                java2dOGLDisabledByProp = !enabled;
            }
            if( !java2dOGLDisabledByProp && !java2dOGLDisabledByOS ) {
                cfg = GraphicsEnvironment.getLocalGraphicsEnvironment().
                        getDefaultScreenDevice().getDefaultConfiguration();
                cfgName = cfg.getClass().getName();
            } else {
                if (DEBUG) {
                  System.err.println("Java2D support disabled: by Property "+java2dOGLDisabledByProp+", by OS "+java2dOGLDisabledByOS);
                }
                cfg = null;
                cfgName = "nil";
            }
            // If we get here, we aren't running in headless mode
            isHeadless = false;
            if (DEBUG) {
              System.err.println("Java2D support: default GraphicsConfiguration = " + cfgName);
            }
            isOGLPipelineActive = cfgName.startsWith("sun.java2d.opengl");
            isOGLPipelineResourceCompatible = isOGLPipelineActive;

            if (isOGLPipelineActive) {
              try {
                // Try to get methods we need to integrate
                final Class<?> utils = Class.forName("sun.java2d.opengl.OGLUtilities");
                invokeWithOGLContextCurrentMethod = utils.getDeclaredMethod("invokeWithOGLContextCurrent",
                                                                            new Class[] {
                                                                              Graphics.class,
                                                                              Runnable.class
                                                                            });
                invokeWithOGLContextCurrentMethod.setAccessible(true);

                isQueueFlusherThreadMethod = utils.getDeclaredMethod("isQueueFlusherThread",
                                                                     new Class[] {});
                isQueueFlusherThreadMethod.setAccessible(true);

                if( isOGLPipelineResourceCompatible ) {
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

                    // Try to get additional methods required for proper FBO support
                    fbObjectSupportInitialized = true;
                    try {
                      invokeWithOGLSharedContextCurrentMethod = utils.getDeclaredMethod("invokeWithOGLSharedContextCurrent",
                                                                                        new Class[] {
                                                                                          GraphicsConfiguration.class,
                                                                                          Runnable.class
                                                                                        });
                      invokeWithOGLSharedContextCurrentMethod.setAccessible(true);

                      getOGLSurfaceTypeMethod = utils.getDeclaredMethod("getOGLSurfaceType",
                                                                        new Class[] {
                                                                          Graphics.class
                                                                        });
                      getOGLSurfaceTypeMethod.setAccessible(true);
                    } catch (final Exception e) {
                      fbObjectSupportInitialized = false;
                      if (DEBUG) {
                        e.printStackTrace();
                        System.err.println("Info: Disabling Java2D/JOGL FBO support");
                      }
                    }

                    // Try to get an additional method for FBO support in recent Mustang builds
                    try {
                      getOGLTextureTypeMethod = utils.getDeclaredMethod("getOGLTextureType",
                                                                        new Class[] {
                                                                          Graphics.class
                                                                        });
                      getOGLTextureTypeMethod.setAccessible(true);
                    } catch (final Exception e) {
                      if (DEBUG) {
                        e.printStackTrace();
                        System.err.println("Info: GL_ARB_texture_rectangle FBO support disabled");
                      }
                    }

                    // Try to set up APIs for enabling the bridge on OS X,
                    // where it isn't possible to create generalized
                    // external GLDrawables
                    Class<?> cglSurfaceData = null;
                    try {
                      cglSurfaceData = Class.forName("sun.java2d.opengl.CGLSurfaceData");
                    } catch (final Exception e) {
                      if (DEBUG) {
                        e.printStackTrace();
                        System.err.println("Info: Unable to find class sun.java2d.opengl.CGLSurfaceData for OS X");
                      }
                    }
                    if (cglSurfaceData != null) {
                      // FIXME: for now, assume that FBO support is not enabled on OS X
                      fbObjectSupportInitialized = false;

                      // We need to find these methods in order to make the bridge work on OS X
                      createOGLContextOnSurfaceMethod = cglSurfaceData.getDeclaredMethod("createOGLContextOnSurface",
                                                                                         new Class[] {
                                                                                           Graphics.class,
                                                                                           Long.TYPE
                                                                                         });
                      createOGLContextOnSurfaceMethod.setAccessible(true);

                      makeOGLContextCurrentOnSurfaceMethod = cglSurfaceData.getDeclaredMethod("makeOGLContextCurrentOnSurface",
                                                                                              new Class[] {
                                                                                                Graphics.class,
                                                                                                Long.TYPE
                                                                                              });
                      makeOGLContextCurrentOnSurfaceMethod.setAccessible(true);

                      destroyOGLContextMethod = cglSurfaceData.getDeclaredMethod("destroyOGLContext",
                                                                                 new Class[] {
                                                                                   Long.TYPE
                                                                                 });
                      destroyOGLContextMethod.setAccessible(true);
                    }
                }
              } catch (final Exception e) {
                caught = e;
                if (DEBUG) {
                  System.err.println("Info: Disabling Java2D/JOGL integration");
                }
                isOGLPipelineActive = false;
                isOGLPipelineResourceCompatible = false;
              }
            }
          } catch (final HeadlessException e) {
            // The AWT is running in headless mode, so the Java 2D / JOGL bridge is clearly disabled
          } catch (final Error e) {
            // issued on OSX Java7: java.lang.Error: Could not find class: sun.awt.HeadlessGraphicsEnvironment
            caught = e;
          }

          if (DEBUG) {
            if(null != caught) {
                caught.printStackTrace();
            }
            System.err.println("JOGL/Java2D OGL Pipeline active " + isOGLPipelineActive + ", resourceCompatible "+isOGLPipelineResourceCompatible);
          }
          return null;
        }
      });
  }

  public static boolean isOGLPipelineActive() {
    return isOGLPipelineActive;
  }

  public static boolean isOGLPipelineResourceCompatible() {
    return isOGLPipelineResourceCompatible;
  }

  public static boolean isFBOEnabled() {
    return fbObjectSupportInitialized;
  }

  public static boolean isQueueFlusherThread() {
    checkActive();

    try {
      return ((Boolean) isQueueFlusherThreadMethod.invoke(null, (Object[])null)).booleanValue();
    } catch (final InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (final Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }

  /** Makes current the OpenGL context associated with the passed
      Graphics object and runs the given Runnable on the Queue
      Flushing Thread in one atomic action. */
  public static void invokeWithOGLContextCurrent(final Graphics g, final Runnable r) throws GLException {
    checkActive();

    try {
      // FIXME: this may need adjustment
      // This seems to be needed in many applications which don't
      // initialize an OpenGL context before this and which would
      // otherwise cause initFBOShareContext to be called from the
      // Queue Flusher Thread, which isn't allowed
      initFBOShareContext(GraphicsEnvironment.
                          getLocalGraphicsEnvironment().
                          getDefaultScreenDevice());

      AWTUtil.lockToolkit();
      try {
        invokeWithOGLContextCurrentMethod.invoke(null, new Object[] {g, r});
      } finally {
        AWTUtil.unlockToolkit();
      }
    } catch (final InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (final Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }

  /** Makes current the "shared" OpenGL context associated with the
      given GraphicsConfiguration object, allowing JOGL to share
      server-side OpenGL objects like textures and display lists with
      this context when necessary. This is needed when Java2D's FBO
      support is enabled, because in order to render into that FBO,
      JOGL must share textures and display lists with it. Returns
      false if the passed GraphicsConfiguration was not an OpenGL
      GraphicsConfiguration. */
  public static boolean invokeWithOGLSharedContextCurrent(final GraphicsConfiguration g, final Runnable r) throws GLException {
    checkCompatible();

    try {
      AWTUtil.lockToolkit();
      try {
        return ((Boolean) invokeWithOGLSharedContextCurrentMethod.invoke(null, new Object[] {g, r})).booleanValue();
      } finally {
        AWTUtil.unlockToolkit();
      }
    } catch (final InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (final Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }

  /** Returns the OpenGL viewport associated with the given Graphics
      object, assuming that the Graphics object is associated with a
      component of the specified width and height. The user should
      call glViewport() with the returned rectangle's bounds in order
      to get correct rendering results. Should only be called from the
      Queue Flusher Thread. */
  public static Rectangle getOGLViewport(final Graphics g,
                                         final int componentWidth,
                                         final int componentHeight) {
    checkCompatible();

    try {
      return (Rectangle) getOGLViewportMethod.invoke(null, new Object[] {g,
                                                                         Integer.valueOf(componentWidth),
                                                                         Integer.valueOf(componentHeight)});
    } catch (final InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (final Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }

  /** Returns the OpenGL scissor region associated with the given
      Graphics object, taking into account all clipping regions, etc.
      To avoid destroying Java2D's previous rendering results, this
      method should be called and the resulting rectangle's bounds
      passed to a call to glScissor(). Should only be called from the
      Queue Flusher Thread. */
  public static Rectangle getOGLScissorBox(final Graphics g) {
    checkCompatible();

    try {
      return (Rectangle) getOGLScissorBoxMethod.invoke(null, new Object[] {g});
    } catch (final InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (final Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }

  /** Returns an opaque "surface identifier" associated with the given
      Graphics object. If this changes from invocation to invocation,
      the underlying OpenGL drawable for the Graphics object has
      changed and a new external GLDrawable and GLContext should be
      created (and the old ones destroyed). Should only be called from
      the Queue Flusher Thread.*/
  public static Object getOGLSurfaceIdentifier(final Graphics g) {
    checkCompatible();

    try {
      return getOGLSurfaceIdentifierMethod.invoke(null, new Object[] {g});
    } catch (final InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (final Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }

  /** Returns the underlying surface type for the given Graphics
      object. This indicates, in particular, whether Java2D is
      currently rendering into a pbuffer or FBO. */
  public static int getOGLSurfaceType(final Graphics g) {
    checkCompatible();

    try {
      // FIXME: fallback path for pre-b73 (?) Mustang builds -- remove
      // once fbobject support is in OGLUtilities
      if (!fbObjectSupportInitialized) {
        return 0;
      }

      return ((Integer) getOGLSurfaceTypeMethod.invoke(null, new Object[] { g })).intValue();
    } catch (final InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (final Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }

  /** Returns the underlying texture target of the given Graphics
      object assuming it is rendering to an FBO. Returns either
      GL_TEXTURE_2D or GL_TEXTURE_RECTANGLE_ARB. */
  public static int getOGLTextureType(final Graphics g) {
    checkCompatible();

    if (getOGLTextureTypeMethod == null) {
      return GL.GL_TEXTURE_2D;
    }

    try {
      return ((Integer) getOGLTextureTypeMethod.invoke(null, new Object[] { g })).intValue();
    } catch (final InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (final Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }

  /** Returns either the given GLContext or a substitute one with
      which clients should share textures and display lists. Needed
      when the Java2D/OpenGL pipeline is active and FBOs are being
      used for rendering. FIXME: may need to alter the API in the
      future to indicate which GraphicsDevice the source context is
      associated with. */
  public static GLContext filterShareContext(final GLContext shareContext) {
    if (isHeadless)
      return shareContext;

    // FIXME: this may need adjustment
    initFBOShareContext(GraphicsEnvironment.
                        getLocalGraphicsEnvironment().
                        getDefaultScreenDevice());
    if (j2dFBOShareContext != null) {
      return j2dFBOShareContext;
    }
    return shareContext;
  }

  /** Returns the GLContext associated with the Java2D "share
      context", with which all contexts created by JOGL must share
      textures and display lists when the FBO option is enabled for
      the Java2D/OpenGL pipeline. */
  public static GLContext getShareContext(final GraphicsDevice device) {
    initFBOShareContext(device);
    // FIXME: for full generality probably need to have multiple of
    // these, one per GraphicsConfiguration seen?
    return j2dFBOShareContext;
  }

  //----------------------------------------------------------------------
  // Mac OS X-specific methods
  //

  /** (Mac OS X-specific) Creates a new OpenGL context on the surface
      associated with the given Graphics object, sharing textures and
      display lists with the specified (CGLContextObj) share context. */
  public static long createOGLContextOnSurface(final Graphics g, final long shareCtx) {
    checkCompatible();

    try {
      return ((Long) createOGLContextOnSurfaceMethod.invoke(null, new Object[] { g, Long.valueOf(shareCtx) })).longValue();
    } catch (final InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (final Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }

  /** (Mac OS X-specific) Makes the given OpenGL context current on
      the surface associated with the given Graphics object. */
  public static boolean makeOGLContextCurrentOnSurface(final Graphics g, final long ctx) {
    checkCompatible();

    try {
      return ((Boolean) makeOGLContextCurrentOnSurfaceMethod.invoke(null, new Object[] { g, Long.valueOf(ctx) })).booleanValue();
    } catch (final InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (final Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }

  /** (Mac OS X-specific) Destroys the given OpenGL context. */
  public static void destroyOGLContext(final long ctx) {
    checkCompatible();

    try {
      destroyOGLContextMethod.invoke(null, new Object[] { Long.valueOf(ctx) });
    } catch (final InvocationTargetException e) {
      throw new GLException(e.getTargetException());
    } catch (final Exception e) {
      throw (InternalError) new InternalError().initCause(e);
    }
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static void checkActive() {
    if (!isOGLPipelineActive()) {
      throw new GLException("Java2D OpenGL pipeline not active");
    }
  }

  private static void checkCompatible() {
    if ( !isOGLPipelineResourceCompatible() ) {
      throw new GLException("Java2D OpenGL pipeline not resource compatible");
    }
  }

  private static int getOGLUtilitiesIntField(final String name) {
    final Integer i = AccessController.doPrivileged(new PrivilegedAction<Integer>() {
        @Override
        public Integer run() {
          try {
            final Class<?> utils = Class.forName("sun.java2d.opengl.OGLUtilities");
            final Field f = utils.getField(name);
            f.setAccessible(true);
            return (Integer) f.get(null);
          } catch (final Exception e) {
            if (DEBUG) {
              e.printStackTrace();
            }
            return null;
          }
        }
      });
    if (i == null)
      return 0;
    if (DEBUG) {
      System.err.println("OGLUtilities." + name + " = " + i.intValue());
    }
    return i.intValue();
  }

  private static void initFBOShareContext(final GraphicsDevice device) {
    // Note 1: this must not be done in the static initalizer due to
    // deadlock problems.

    // Note 2: the first execution of this method must not be from the
    // Java2D Queue Flusher Thread.

    if (isOGLPipelineResourceCompatible() &&
        isFBOEnabled() &&
        !initializedJ2DFBOShareContext) {

      // FIXME: this technique is probably not adequate in multi-head
      // situations. Ideally we would keep track of a given share
      // context on a per-GraphicsConfiguration basis or something
      // similar rather than keeping one share context in a global
      // variable.
      initializedJ2DFBOShareContext = true;
      if (DEBUG) {
        System.err.println("Starting initialization of J2D FBO share context");
      }
      invokeWithOGLSharedContextCurrent(device.getDefaultConfiguration(), new Runnable() {
          @Override
          public void run() {
            j2dFBOShareContext = GLDrawableFactory.getFactory(GLProfile.getDefault(GLProfile.getDefaultDevice())).createExternalGLContext();
          }
        });
      if (DEBUG) {
        System.err.println("Ending initialization of J2D FBO share context");
      }
    }
  }
}

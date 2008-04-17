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

package javax.media.opengl;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.geom.*;
import java.beans.*;
import java.lang.reflect.*;
import java.security.*;
import com.sun.opengl.impl.*;

// FIXME: Subclasses need to call resetGLFunctionAvailability() on their
// context whenever the displayChanged() function is called on our
// GLEventListeners

/** A heavyweight AWT component which provides OpenGL rendering
    support. This is the primary implementation of {@link GLDrawable};
    {@link GLJPanel} is provided for compatibility with Swing user
    interfaces when adding a heavyweight doesn't work either because
    of Z-ordering or LayoutManager problems. */

public class GLCanvas extends Canvas implements GLAutoDrawable {

  private static final boolean DEBUG = Debug.debug("GLCanvas");

  private GLDrawableHelper drawableHelper = new GLDrawableHelper();
  private GLDrawable drawable;
  private GLContext context;
  private boolean autoSwapBufferMode = true;
  private boolean sendReshape = false;
  
  private GraphicsConfiguration chosen;
  private GLCapabilities glCaps;
  private GLCapabilitiesChooser glCapChooser;

  /** Creates a new GLCanvas component with a default set of OpenGL
      capabilities, using the default OpenGL capabilities selection
      mechanism, on the default screen device. */
  public GLCanvas() {
    this(null);
  }

  /** Creates a new GLCanvas component with the requested set of
      OpenGL capabilities, using the default OpenGL capabilities
      selection mechanism, on the default screen device. */
  public GLCanvas(GLCapabilities capabilities) {
    this(capabilities, null, null, null);
  }

  /** Creates a new GLCanvas component. The passed GLCapabilities
      specifies the OpenGL capabilities for the component; if null, a
      default set of capabilities is used. The GLCapabilitiesChooser
      specifies the algorithm for selecting one of the available
      GLCapabilities for the component; a DefaultGLCapabilitesChooser
      is used if null is passed for this argument. The passed
      GLContext specifies an OpenGL context with which to share
      textures, display lists and other OpenGL state, and may be null
      if sharing is not desired. See the note in the overview
      documentation on <a
      href="../../../overview-summary.html#SHARING">context
      sharing</a>. The passed GraphicsDevice indicates the screen on
      which to create the GLCanvas; the GLDrawableFactory uses the
      default screen device of the local GraphicsEnvironment if null
      is passed for this argument. */
  public GLCanvas(GLCapabilities capabilities,
                  GLCapabilitiesChooser chooser,
                  GLContext shareWith,
                  GraphicsDevice device) {
    // The platform-specific GLDrawableFactory will only provide a
    // non-null GraphicsConfiguration on platforms where this is
    // necessary (currently only X11, as Windows allows the pixel
    // format of the window to be set later and Mac OS X seems to
    // handle this very differently than all other platforms). On
    // other platforms this method returns null; it is the case (at
    // least in the Sun AWT implementation) that this will result in
    // equivalent behavior to calling the no-arg super() constructor
    // for Canvas.
    /*
     * Workaround for Xinerama, always pass null so we can detect whether
     * super.getGraphicsConfiguration() is returning the Canvas' GC (null),
     * or an ancestor component's GC (non-null) in the overridden version
     * below.
     */
    super();
    /*
     * Save the chosen capabilities for use in getGraphicsConfiguration().
     */
    chosen = chooseGraphicsConfiguration(capabilities, chooser, device);
    if (chosen != null) {
      /*
       * If we are running on a platform that
       * must select a GraphicsConfiguration now,
       * save these for later use in getGraphicsConfiguration().
       */
      this.glCapChooser = chooser;
      this.glCaps = capabilities;
    }
    if (!Beans.isDesignTime()) {
      drawable = GLDrawableFactory.getFactory().getGLDrawable(this, capabilities, chooser);
      context = drawable.createContext(shareWith);
      context.setSynchronized(true);
    }
  }
  
  /**
   * Overridden to choose a GraphicsConfiguration on a parent container's
   * GraphicsDevice because both devices
   */
  public GraphicsConfiguration getGraphicsConfiguration() {
    /*
     * Workaround for problems with Xinerama and java.awt.Component.checkGD
     * when adding to a container on a different graphics device than the
     * one that this Canvas is associated with.
     * 
     * GC will be null unless:
     *   - A native peer has assigned it. This means we have a native
     *     peer, and are already comitted to a graphics configuration.
     *   - This canvas has been added to a component hierarchy and has
     *     an ancestor with a non-null GC, but the native peer has not
     *     yet been created. This means we can still choose the GC on
     *     all platforms since the peer hasn't been created.
     */
    final GraphicsConfiguration gc = super.getGraphicsConfiguration();
    /*
     * chosen is only non-null on platforms where the GLDrawableFactory
     * returns a non-null GraphicsConfiguration (in the GLCanvas
     * constructor).
     * 
     * if gc is from this Canvas' native peer then it should equal chosen,
     * otherwise it is from an ancestor component that this Canvas is being
     * added to, and we go into this block.
     */
    if (gc != null && chosen != null && !chosen.equals(gc)) {
      /*
       * Check for compatibility with gc. If they differ by only the
       * device then return a new GCconfig with the super-class' GDevice
       * (and presumably the same visual ID in Xinerama).
       * 
       */
      if (!chosen.getDevice().getIDstring().equals(gc.getDevice().getIDstring())) {
        /*
         * Here we select a GraphicsConfiguration on the alternate
         * device that is presumably identical to the chosen
         * configuration, but on the other device.
         * 
         * Should really check to ensure that we select a configuration
         * with the same X visual ID for Xinerama screens, otherwise the
         * GLDrawable may have the wrong visual ID (I don't think this
         * ever gets updated). May need to add a method to
         * X11GLDrawableFactory to do this in a platform specific
         * manner.
         * 
         * However, on platforms where we can actually get into this
         * block, both devices should have the same visual list, and the
         * same configuration should be selected here.
         */
        final GraphicsConfiguration compatible = chooseGraphicsConfiguration(glCaps, glCapChooser, gc.getDevice());

        if (compatible != null) {
          /*
           * Save the new GC for equals test above, and to return to
           * any outside callers of this method.
           */
          chosen = compatible;
        }
      }

      /*
       * If a compatible GC was not found in the block above, this will
       * return the GC that was selected in the constructor (and might
       * cause an exception in Component.checkGD when adding to a
       * container, but in this case that would be the desired behavior).
       * 
       */
      return chosen;
    } else if (gc == null) {
      /*
       * The GC is null, which means we have no native peer, and are not
       * part of a (realized) component hierarchy. So we return the
       * desired visual that was selected in the constructor (possibly
       * null).
       */
      return chosen;
    }

    /*
     * Otherwise we have not explicitly selected a GC in the constructor, so
     * just return what Canvas would have.
     */
    return gc;
  }
  
  public GLContext createContext(GLContext shareWith) {
    return drawable.createContext(shareWith);
  }

  public void setRealized(boolean realized) {
  }

  public void display() {
    maybeDoSingleThreadedWorkaround(displayOnEventDispatchThreadAction,
                                    displayAction);
  }

  /** Overridden to cause OpenGL rendering to be performed during
      repaint cycles. Subclasses which override this method must call
      super.paint() in their paint() method in order to function
      properly. <P>

      <B>Overrides:</B>
      <DL><DD><CODE>paint</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
  public void paint(Graphics g) {
    if (Beans.isDesignTime()) {
      // Make GLCanvas behave better in NetBeans GUI builder
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, getWidth(), getHeight());
      FontMetrics fm = g.getFontMetrics();
      String name = getName();
      if (name == null) {
        name = getClass().getName();
        int idx = name.lastIndexOf('.');
        if (idx >= 0) {
          name = name.substring(idx + 1);
        }
      }
      Rectangle2D bounds = fm.getStringBounds(name, g);
      g.setColor(Color.WHITE);
      g.drawString(name,
                   (int) ((getWidth()  - bounds.getWidth())  / 2),
                   (int) ((getHeight() + bounds.getHeight()) / 2));
      return;
    }

    display();
  }

  /** Overridden to track when this component is added to a container.
      Subclasses which override this method must call
      super.addNotify() in their addNotify() method in order to
      function properly. <P>

      <B>Overrides:</B>
      <DL><DD><CODE>addNotify</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
  public void addNotify() {
    super.addNotify();
    if (!Beans.isDesignTime()) {
      disableBackgroundErase();
      drawable.setRealized(true);
    }
    if (DEBUG) {
      System.err.println("GLCanvas.addNotify()");
    }
  }

  /** Overridden to track when this component is removed from a
      container. Subclasses which override this method must call
      super.removeNotify() in their removeNotify() method in order to
      function properly. <P>

      <B>Overrides:</B>
      <DL><DD><CODE>removeNotify</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
  public void removeNotify() {
    if (Beans.isDesignTime()) {
      super.removeNotify();
    } else {
      try {
        if (Threading.isSingleThreaded() &&
            !Threading.isOpenGLThread()) {
          // Workaround for termination issues with applets --
          // sun.applet.AppletPanel should probably be performing the
          // remove() call on the EDT rather than on its own thread
          if (Threading.isAWTMode() &&
              Thread.holdsLock(getTreeLock())) {
            // The user really should not be invoking remove() from this
            // thread -- but since he/she is, we can not go over to the
            // EDT at this point. Try to destroy the context from here.
            destroyAction.run();
          } else {
            Threading.invokeOnOpenGLThread(destroyAction);
          }
        } else {
          destroyAction.run();
        }
      } finally {
        drawable.setRealized(false);
        super.removeNotify();
        if (DEBUG) {
          System.err.println("GLCanvas.removeNotify()");
        }
      }
    }
  }

  /** Overridden to cause {@link GLDrawableHelper#reshape} to be
      called on all registered {@link GLEventListener}s. Subclasses
      which override this method must call super.reshape() in
      their reshape() method in order to function properly. <P>

      <B>Overrides:</B>
      <DL><DD><CODE>reshape</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
  public void reshape(int x, int y, int width, int height) {
    super.reshape(x, y, width, height);
    sendReshape = true;
  }

  /** <B>Overrides:</B>
      <DL><DD><CODE>update</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
  // Overridden from Canvas to prevent the AWT's clearing of the
  // canvas from interfering with the OpenGL rendering.
  public void update(Graphics g) {
    paint(g);
  }
  
  public void addGLEventListener(GLEventListener listener) {
    drawableHelper.addGLEventListener(listener);
  }

  public void removeGLEventListener(GLEventListener listener) {
    drawableHelper.removeGLEventListener(listener);
  }

  public GLContext getContext() {
    return context;
  }

  public GL getGL() {
    if (Beans.isDesignTime()) {
      return null;
    }

    return getContext().getGL();
  }

  public void setGL(GL gl) {
    if (!Beans.isDesignTime()) {
      getContext().setGL(gl);
    }
  }

  public void setAutoSwapBufferMode(boolean onOrOff) {
    drawableHelper.setAutoSwapBufferMode(onOrOff);
  }

  public boolean getAutoSwapBufferMode() {
    return drawableHelper.getAutoSwapBufferMode();
  }

  public void swapBuffers() {
    maybeDoSingleThreadedWorkaround(swapBuffersOnEventDispatchThreadAction, swapBuffersAction);
  }

  public GLCapabilities getChosenGLCapabilities() {
    if (drawable == null)
      return null;

    return drawable.getChosenGLCapabilities();
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void maybeDoSingleThreadedWorkaround(Runnable eventDispatchThreadAction,
                                               Runnable invokeGLAction) {
    if (Threading.isSingleThreaded() &&
        !Threading.isOpenGLThread()) {
      Threading.invokeOnOpenGLThread(eventDispatchThreadAction);
    } else {
      drawableHelper.invokeGL(drawable, context, invokeGLAction, initAction);
    }
  }

  class InitAction implements Runnable {
    public void run() {
      drawableHelper.init(GLCanvas.this);
    }
  }
  private InitAction initAction = new InitAction();
  
  class DisplayAction implements Runnable {
    public void run() {
      if (sendReshape) {
        // Note: we ignore the given x and y within the parent component
        // since we are drawing directly into this heavyweight component.
        int width = getWidth();
        int height = getHeight();
        getGL().glViewport(0, 0, width, height);
        drawableHelper.reshape(GLCanvas.this, 0, 0, width, height);
        sendReshape = false;
      }

      drawableHelper.display(GLCanvas.this);
    }
  }
  private DisplayAction displayAction = new DisplayAction();

  class SwapBuffersAction implements Runnable {
    public void run() {
      drawable.swapBuffers();
    }
  }
  private SwapBuffersAction swapBuffersAction = new SwapBuffersAction();

  // Workaround for ATI driver bugs related to multithreading issues
  // like simultaneous rendering via Animators to canvases that are
  // being resized on the AWT event dispatch thread
  class DisplayOnEventDispatchThreadAction implements Runnable {
    public void run() {
      drawableHelper.invokeGL(drawable, context, displayAction, initAction);
    }
  }
  private DisplayOnEventDispatchThreadAction displayOnEventDispatchThreadAction =
    new DisplayOnEventDispatchThreadAction();
  class SwapBuffersOnEventDispatchThreadAction implements Runnable {
    public void run() {
      drawableHelper.invokeGL(drawable, context, swapBuffersAction, initAction);
    }
  }
  private SwapBuffersOnEventDispatchThreadAction swapBuffersOnEventDispatchThreadAction =
    new SwapBuffersOnEventDispatchThreadAction();

  class DestroyAction implements Runnable {
    public void run() {
      GLContext current = GLContext.getCurrent();
      if (current == context) {
        context.release();
      }
      context.destroy();
    }
  }
  private DestroyAction destroyAction = new DestroyAction();

  // Disables the AWT's erasing of this Canvas's background on Windows
  // in Java SE 6. This internal API is not available in previous
  // releases, but the system property
  // -Dsun.awt.noerasebackground=true can be specified to get similar
  // results globally in previous releases.
  private static boolean disableBackgroundEraseInitialized;
  private static Method  disableBackgroundEraseMethod;
  private void disableBackgroundErase() {
    if (!disableBackgroundEraseInitialized) {
      try {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
              try {
                disableBackgroundEraseMethod =
                  getToolkit().getClass().getDeclaredMethod("disableBackgroundErase",
                                                            new Class[] { Canvas.class });
                disableBackgroundEraseMethod.setAccessible(true);
              } catch (Exception e) {
              }
              return null;
            }
          });
      } catch (Exception e) {
      }
      disableBackgroundEraseInitialized = true;
    }
    if (disableBackgroundEraseMethod != null) {
      try {
        disableBackgroundEraseMethod.invoke(getToolkit(), new Object[] { this });
      } catch (Exception e) {
        // FIXME: workaround for 6504460 (incorrect backport of 6333613 in 5.0u10)
        // throw new GLException(e);
      }
    }
  }

  private static GraphicsConfiguration chooseGraphicsConfiguration(GLCapabilities capabilities,
                                                                   GLCapabilitiesChooser chooser,
                                                                   GraphicsDevice device) {
    // Make GLCanvas behave better in NetBeans GUI builder
    if (Beans.isDesignTime()) {
      return null;
    }

    AWTGraphicsConfiguration config = (AWTGraphicsConfiguration)
      GLDrawableFactory.getFactory().chooseGraphicsConfiguration(capabilities,
                                                                 chooser,
                                                                 new AWTGraphicsDevice(device));
    if (config == null) {
      return null;
    }

    return config.getGraphicsConfiguration();
  }
}

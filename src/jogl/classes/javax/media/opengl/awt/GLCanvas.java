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

package javax.media.opengl.awt;

import javax.media.opengl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.awt.*;

import com.sun.opengl.impl.*;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.geom.*;
import java.beans.*;
import java.lang.reflect.*;
import java.security.*;

// FIXME: Subclasses need to call resetGLFunctionAvailability() on their
// context whenever the displayChanged() function is called on our
// GLEventListeners

/** A heavyweight AWT component which provides OpenGL rendering
    support. This is the primary implementation of {@link GLDrawable};
    {@link GLJPanel} is provided for compatibility with Swing user
    interfaces when adding a heavyweight doesn't work either because
    of Z-ordering or LayoutManager problems. */

public class GLCanvas extends Canvas implements AWTGLAutoDrawable {

  private static final boolean DEBUG = Debug.debug("GLCanvas");

  static private GLProfile defaultGLProfile = GLProfile.getDefault();
  private GLProfile glProfile;
  private GLDrawableHelper drawableHelper = new GLDrawableHelper();
  private GraphicsConfiguration chosen;
  private AWTGraphicsConfiguration awtConfig;
  private GLDrawable drawable;
  private GLContextImpl context;
  private boolean autoSwapBufferMode = true;
  private boolean sendReshape = false;
  
  // copy of the cstr args ..
  private GLCapabilities capabilities;
  private GLCapabilitiesChooser chooser;
  private GLContext shareWith;
  private GraphicsDevice device;

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
    /*
     * Workaround for Xinerama, always pass null so we can detect whether
     * super.getGraphicsConfiguration() is returning the Canvas' GC (null),
     * or an ancestor component's GC (non-null) in the overridden version
     * below.
     */
    super();

    if(null==capabilities) {
        capabilities = new GLCapabilities(defaultGLProfile);
    }
    glProfile = capabilities.getGLProfile();

    this.capabilities = capabilities;
    this.chooser = chooser;
    this.shareWith=shareWith;
    this.device = device;
  }

  protected interface DestroyMethod {
    public void destroyMethod();
  }

  protected final static Object  addClosingListener(Component c, final DestroyMethod d) {
    WindowAdapter cl = null;
    Window w = getWindow(c);
    if(null!=w) {
        cl = new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                  // we have to issue this call rigth away,
                  // otherwise the window gets destroyed
                  d.destroyMethod();
                }
            };
        w.addWindowListener(cl);
    }
    return cl;
  }

  protected final static Window getWindow(Component c) {
    while ( c!=null && ! ( c instanceof Window ) ) {
        c = c.getParent();
    }
    return (Window)c;
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
        AWTGraphicsConfiguration config = chooseGraphicsConfiguration((GLCapabilities)awtConfig.getRequestedCapabilities(), chooser, gc.getDevice());
        final GraphicsConfiguration compatible = (null!=config)?config.getGraphicsConfiguration():null;
        boolean equalCaps = config.getChosenCapabilities().equals(awtConfig.getChosenCapabilities());
        if(DEBUG) {
            Exception e = new Exception("Call Stack: "+Thread.currentThread().getName());
            e.printStackTrace();
            System.err.println("!!! Created Config (n): HAVE    GC "+chosen);
            System.err.println("!!! Created Config (n): THIS    GC "+gc);
            System.err.println("!!! Created Config (n): Choosen GC "+compatible);
            System.err.println("!!! Created Config (n): HAVE    CF "+awtConfig);
            System.err.println("!!! Created Config (n): Choosen CF "+config);
            System.err.println("!!! Created Config (n): EQUALS CAPS "+equalCaps);
        }

        if (compatible != null) {
          /*
           * Save the new GC for equals test above, and to return to
           * any outside callers of this method.
           */
          chosen = compatible;

          awtConfig = config;

          if( !equalCaps && GLAutoDrawable.SCREEN_CHANGE_ACTION_ENABLED ) {
              dispose(true);
          }
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

  private Object closingListener = null;
  private Object closingListenerLock = new Object();

  public void display() {
    maybeDoSingleThreadedWorkaround(displayOnEventDispatchThreadAction,
                                    displayAction);
    if(null==closingListener) {
      synchronized(closingListenerLock) {
        if(null==closingListener) {
            closingListener=addClosingListener(this, new DestroyMethod() { 
                        public void destroyMethod() { destroy(); } });
        }
      }
    }
  }

  protected void dispose(boolean regenerate) {
    if(DEBUG) {
        Exception ex1 = new Exception("dispose("+regenerate+") - start");
        ex1.printStackTrace();
    }
    disposeRegenerate=regenerate;

    if (Threading.isSingleThreaded() &&
        !Threading.isOpenGLThread()) {
      // Workaround for termination issues with applets --
      // sun.applet.AppletPanel should probably be performing the
      // remove() call on the EDT rather than on its own thread
      if (ThreadingImpl.isAWTMode() &&
          Thread.holdsLock(getTreeLock())) {
        // The user really should not be invoking remove() from this
        // thread -- but since he/she is, we can not go over to the
        // EDT at this point. Try to destroy the context from here.
        drawableHelper.invokeGL(drawable, context, disposeAction, null);
      } else {
        Threading.invokeOnOpenGLThread(disposeOnEventDispatchThreadAction);
      }
    } else {
      drawableHelper.invokeGL(drawable, context, disposeAction, null);
    }

    if(DEBUG) {
        System.err.println("dispose("+regenerate+") - stop");
    }
  }

  /**
   * Just an alias for removeNotify
   */
  public void destroy() {
    removeNotify();
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

        if(null==device) {
            GraphicsConfiguration gc = super.getGraphicsConfiguration();
            if(null!=gc) {
                device = gc.getDevice();
            }
        }

        /*
         * Save the chosen capabilities for use in getGraphicsConfiguration().
         */
        awtConfig = chooseGraphicsConfiguration(capabilities, chooser, device);
        if(DEBUG) {
            Exception e = new Exception("Created Config: "+awtConfig);
            e.printStackTrace();
        }
        if(null!=awtConfig) {
          // update ..
          chosen = awtConfig.getGraphicsConfiguration();

        }
        if(null==awtConfig) {
          throw new GLException("Error: AWTGraphicsConfiguration is null");
        }
        drawable = GLDrawableFactory.getFactory(glProfile).createGLDrawable(NativeWindowFactory.getNativeWindow(this, awtConfig));
        context = (GLContextImpl) drawable.createContext(shareWith);
        context.setSynchronized(true);

        if(DEBUG) {
            System.err.println("Created Drawable: "+drawable);
        }
        drawable.setRealized(true);
    }
  }

  /** Overridden to track when this component is removed from a
      container. Subclasses which override this method must call
      super.removeNotify() in their removeNotify() method in order to
      function properly. <P>

      <B>Overrides:</B>
      <DL><DD><CODE>removeNotify</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
  public void removeNotify() {
    if(DEBUG) {
        Exception ex1 = new Exception("removeNotify - start");
        ex1.printStackTrace();
    }

    if (Beans.isDesignTime()) {
      super.removeNotify();
    } else {
      try {
        dispose(false);
      } finally {
        drawable=null;
        super.removeNotify();
      }
    }
    if(DEBUG) {
        System.out.println("removeNotify - end");
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

  public void setContext(GLContext ctx) {
    context=(GLContextImpl)ctx;
  }

  public GLContext getContext() {
    return context;
  }

  public GL getGL() {
    if (Beans.isDesignTime()) {
      return null;
    }
    GLContext context = getContext();
    return (context == null) ? null : context.getGL();
  }

  public GL setGL(GL gl) {
    GLContext context = getContext();
    if (context != null) {
      context.setGL(gl);
      return gl;
    }
    return null;
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

  public GLProfile getGLProfile() {
    return glProfile;
  }

  public GLCapabilities getChosenGLCapabilities() {
    if (awtConfig == null) {
        throw new GLException("No AWTGraphicsConfiguration: "+this);
    }

    return (GLCapabilities)awtConfig.getChosenCapabilities();
  }

  public GLCapabilities getRequestedGLCapabilities() {
    if (awtConfig == null) {
        throw new GLException("No AWTGraphicsConfiguration: "+this);
    }

    return (GLCapabilities)awtConfig.getRequestedCapabilities();
  }

  public NativeWindow getNativeWindow() {
    return drawable.getNativeWindow();
  }

  public GLDrawableFactory getFactory() {
    return drawable.getFactory();
  }

  public String toString() {
    return "AWT-GLCanvas[ "+awtConfig+", "+((null!=drawable)?drawable.getClass().getName():"null-drawable")+", "+drawableHelper+"]";
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

  private boolean disposeRegenerate;
  private DisposeAction disposeAction = new DisposeAction(this);

  class DisposeAction implements Runnable {
    private GLCanvas canvas;
    public DisposeAction(GLCanvas canvas) {
        this.canvas = canvas;
    }
    public void run() {
      drawableHelper.dispose(GLCanvas.this);

      if(null!=context) {
        context.makeCurrent(); // implicit wait for lock ..
        context.destroy();
        context=null;
      }

      if(null!=drawable) {
          drawable.setRealized(false);
      }

      if(disposeRegenerate) {
          // recreate GLDrawable to reflect it's new graphics configuration
          drawable = GLDrawableFactory.getFactory(glProfile).createGLDrawable(NativeWindowFactory.getNativeWindow(canvas, awtConfig));
          if(DEBUG) {
            System.err.println("GLCanvas.dispose(true): new drawable: "+drawable);
          }
          drawable.setRealized(true);
          context = (GLContextImpl) drawable.createContext(shareWith);
          context.setSynchronized(true);
          sendReshape=true; // ensure a reshape is being send ..
      }
    }
  }

  private DisposeOnEventDispatchThreadAction disposeOnEventDispatchThreadAction =
    new DisposeOnEventDispatchThreadAction();

  class DisposeOnEventDispatchThreadAction implements Runnable {
    public void run() {
      drawableHelper.invokeGL(drawable, context, disposeAction, null);
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
                Class clazz = getToolkit().getClass();
                while (clazz != null && disableBackgroundEraseMethod == null) {
                  try {
                    disableBackgroundEraseMethod =
                      clazz.getDeclaredMethod("disableBackgroundErase",
                                              new Class[] { Canvas.class });
                    disableBackgroundEraseMethod.setAccessible(true);
                  } catch (Exception e) {
                    clazz = clazz.getSuperclass();
                  }
                }
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

  private static AWTGraphicsConfiguration chooseGraphicsConfiguration(GLCapabilities capabilities,
                                                                      GLCapabilitiesChooser chooser,
                                                                      GraphicsDevice device) {
    // Make GLCanvas behave better in NetBeans GUI builder
    if (Beans.isDesignTime()) {
      return null;
    }

    AbstractGraphicsScreen aScreen = AWTGraphicsScreen.createScreenDevice(device);
    AWTGraphicsConfiguration config = (AWTGraphicsConfiguration)
      GraphicsConfigurationFactory.getFactory(AWTGraphicsDevice.class).chooseGraphicsConfiguration(capabilities,
                                                                                                   chooser,
                                                                                                   aScreen);
    if (config == null) {
      throw new GLException("Error: Couldn't fetch AWTGraphicsConfiguration");
    }

    return config;
  }
}

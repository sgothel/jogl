/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

import java.beans.Beans;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.geom.Rectangle2D;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.media.nativewindow.WindowClosingProtocol;
import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.awt.AWTGraphicsConfiguration;
import javax.media.nativewindow.awt.AWTGraphicsDevice;
import javax.media.nativewindow.awt.AWTGraphicsScreen;
import javax.media.nativewindow.awt.AWTWindowClosingProtocol;

import javax.media.opengl.GL;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLRunnable;
import javax.media.opengl.Threading;

import com.jogamp.nativewindow.NativeWindowVersion;
import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.opengl.JoglVersion;

import com.jogamp.common.util.locks.RecursiveLock;
import jogamp.opengl.Debug;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableHelper;
import jogamp.opengl.ThreadingImpl;

// FIXME: Subclasses need to call resetGLFunctionAvailability() on their
// context whenever the displayChanged() function is called on our
// GLEventListeners

/** A heavyweight AWT component which provides OpenGL rendering
    support. This is the primary implementation of an AWT {@link GLDrawable};
    {@link GLJPanel} is provided for compatibility with Swing user
    interfaces when adding a heavyweight doesn't work either because
    of Z-ordering or LayoutManager problems.
 *
 * <h5><A NAME="java2dgl">Java2D OpenGL Remarks</A></h5>
 *
 * To avoid any conflicts with a potential Java2D OpenGL context,<br>
 * you shall consider setting the following JVM properties:<br>
 * <ul>
 *    <li><pre>sun.java2d.opengl=false</pre></li>
 *    <li><pre>sun.java2d.noddraw=true</pre></li>
 * </ul>
 * This is especially true in case you want to utilize a GLProfile other than
 * {@link GLProfile#GL2}, eg. using {@link GLProfile#getMaxFixedFunc()}.<br>
 * On the other hand, if you like to experiment with GLJPanel's utilization
 * of Java2D's OpenGL pipeline, you have to set them to
 * <ul>
 *    <li><pre>sun.java2d.opengl=true</pre></li>
 *    <li><pre>sun.java2d.noddraw=true</pre></li>
 * </ul>
 *
 * <h5><A NAME="backgrounderase">Disable Background Erase</A></h5>
 *
 * GLCanvas tries to disable background erase for the AWT Canvas
 * before native peer creation (X11) and after it (Windows), <br>
 * utilizing the optional {@link java.awt.Toolkit} method <code>disableBeackgroundErase(java.awt.Canvas)</code>.<br>
 * However if this does not give you the desired results, you may want to disable AWT background erase in general:
 * <ul>
 *   <li><pre>sun.awt.noerasebackground=true</pre></li>
 * </ul>
 */

public class GLCanvas extends Canvas implements AWTGLAutoDrawable, WindowClosingProtocol {

  private static final boolean DEBUG;

  static {
      DEBUG = Debug.debug("GLCanvas");
  }

  private GLDrawableHelper drawableHelper = new GLDrawableHelper();
  private AWTGraphicsConfiguration awtConfig;
  private GLDrawable drawable;
  private GLContextImpl context;
  private boolean sendReshape = false;
  
  // copy of the cstr args, mainly for recreation
  private GLCapabilitiesImmutable capsReqUser;
  private GLCapabilitiesChooser chooser;
  private GLContext shareWith;  
  private GraphicsDevice device;

  private AWTWindowClosingProtocol awtWindowClosingProtocol =
          new AWTWindowClosingProtocol(this, new Runnable() {
                public void run() {
                    GLCanvas.this.destroy();
                }
            });

  /** Creates a new GLCanvas component with a default set of OpenGL
      capabilities, using the default OpenGL capabilities selection
      mechanism, on the default screen device. */
  public GLCanvas() {
    this(null);
  }

  /** Creates a new GLCanvas component with the requested set of
      OpenGL capabilities, using the default OpenGL capabilities
      selection mechanism, on the default screen device. 
   * @see GLCanvas#GLCanvas(javax.media.opengl.GLCapabilitiesImmutable, javax.media.opengl.GLCapabilitiesChooser, javax.media.opengl.GLContext, java.awt.GraphicsDevice)
   */
  public GLCanvas(GLCapabilitiesImmutable capsReqUser) {
    this(capsReqUser, null, null, null);
  }

  /** Creates a new GLCanvas component with the requested set of
      OpenGL capabilities, using the default OpenGL capabilities
      selection mechanism, on the default screen device.
   *  This constructor variant also supports using a shared GLContext.
   *
   * @see GLCanvas#GLCanvas(javax.media.opengl.GLCapabilitiesImmutable, javax.media.opengl.GLCapabilitiesChooser, javax.media.opengl.GLContext, java.awt.GraphicsDevice)
   */
  public GLCanvas(GLCapabilitiesImmutable capsReqUser, GLContext shareWith) {
    this(capsReqUser, null, shareWith, null);
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
  public GLCanvas(GLCapabilitiesImmutable capsReqUser,
                  GLCapabilitiesChooser chooser,
                  GLContext shareWith,
                  GraphicsDevice device) {
    /*
     * Determination of the native window is made in 'super.addNotify()',
     * which creates the native peer using AWT's GraphicsConfiguration.
     * GraphicsConfiguration is returned by this class overwritten
     * 'getGraphicsConfiguration()', which returns our OpenGL compatible
     * 'chosen' GraphicsConfiguration.
     */
    super();

    if(null==capsReqUser) {
        capsReqUser = new GLCapabilities(GLProfile.getDefault(GLProfile.getDefaultDesktopDevice()));
    } else {
        // don't allow the user to change data
        capsReqUser = (GLCapabilitiesImmutable) capsReqUser.cloneMutable();
    }

    if(null==device) {
        GraphicsConfiguration gc = super.getGraphicsConfiguration();
        if(null!=gc) {
            device = gc.getDevice();
        }
    }

    // instantiation will be issued in addNotify()
    this.capsReqUser = capsReqUser;
    this.chooser = chooser;
    this.shareWith = shareWith;
    this.device = device;
  }

  /**
   * Overridden to choose a GraphicsConfiguration on a parent container's
   * GraphicsDevice because both devices
   */
    @Override
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
    GraphicsConfiguration chosen =  awtConfig.getGraphicsConfiguration();

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
        AWTGraphicsConfiguration config = chooseGraphicsConfiguration( (GLCapabilitiesImmutable)awtConfig.getChosenCapabilities(),
                                                                       (GLCapabilitiesImmutable)awtConfig.getRequestedCapabilities(),
                                                                       chooser, gc.getDevice());
        final GraphicsConfiguration compatible = (null!=config)?config.getGraphicsConfiguration():null;
        boolean equalCaps = config.getChosenCapabilities().equals(awtConfig.getChosenCapabilities());
        if(DEBUG) {
            Exception e = new Exception("Info: Call Stack: "+Thread.currentThread().getName());
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
      drawableSync.lock();
      try {
        return (null != drawable) ? drawable.createContext(shareWith) : null;
      } finally {
        drawableSync.unlock();
      }
  }

  public void setRealized(boolean realized) {
  }

  public boolean isRealized() {
      drawableSync.lock();
      try {
        return ( null != drawable ) ? drawable.isRealized() : false;
      } finally {
        drawableSync.unlock();
      }
  }

  public int getDefaultCloseOperation() {
      return awtWindowClosingProtocol.getDefaultCloseOperation();
  }

  public int setDefaultCloseOperation(int op) {
      return awtWindowClosingProtocol.setDefaultCloseOperation(op);
  }

  public void display() {
    if( !validateGLDrawable() ) {
        if(DEBUG) {
            System.err.println("Info: GLCanvas display - skipped GL render, drawable not valid yet");
        }
        return; // not yet available ..
    }
    maybeDoSingleThreadedWorkaround(displayOnEventDispatchThreadAction,
                                    displayAction);

    awtWindowClosingProtocol.addClosingListenerOneShot();
  }

  private void dispose(boolean regenerate) {
    drawableSync.lock();
    try {
        if(DEBUG) {
            Exception ex1 = new Exception("Info: dispose("+regenerate+") - start, hasContext " +
                    (null!=context) + ", hasDrawable " + (null!=drawable));
            ex1.printStackTrace();
        }

        if(null!=context) {
            boolean animatorPaused = false;
            GLAnimatorControl animator =  getAnimator();
            if(null!=animator) {
                // can't remove us from animator for recreational addNotify()
                animatorPaused = animator.pause();
            }

            disposeRegenerate=regenerate;

            if (Threading.isSingleThreaded() &&
                !Threading.isOpenGLThread()) {
              // Workaround for termination issues with applets --
              // sun.applet.AppletPanel should probably be performing the
              // remove() call on the EDT rather than on its own thread
              // Hint: User should run remove from EDT.
              if (ThreadingImpl.isAWTMode() &&
                  Thread.holdsLock(getTreeLock())) {
                // The user really should not be invoking remove() from this
                // thread -- but since he/she is, we can not go over to the
                // EDT at this point. Try to destroy the context from here.
                if(context.isCreated()) {
                    drawableHelper.invokeGL(drawable, context, disposeAction, null);
                }
              } else if(context.isCreated()) {
                Threading.invokeOnOpenGLThread(disposeOnEventDispatchThreadAction);
              }
            } else if(context.isCreated()) {
              drawableHelper.invokeGL(drawable, context, disposeAction, null);
            }

            if(animatorPaused) {
                animator.resume();
            }
        }
        if(!regenerate) {
            disposeAbstractGraphicsDevice();
        }

        if(DEBUG) {
            System.err.println("dispose("+regenerate+") - stop");
        }
    } finally {
        drawableSync.unlock();
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
    @Override
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
    if( ! this.drawableHelper.isExternalAnimatorAnimating() ) {
        display();
    }
  }

  RecursiveLock drawableSync = new RecursiveLock();

  /** Overridden to track when this component is added to a container.
      Subclasses which override this method must call
      super.addNotify() in their addNotify() method in order to
      function properly. <P>

      <B>Overrides:</B>
      <DL><DD><CODE>addNotify</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
    @Override
  public void addNotify() {
    if(DEBUG) {
        Exception ex1 = new Exception(Thread.currentThread().getName()+" - Info: addNotify - start, bounds: "+this.getBounds());
        ex1.printStackTrace();
    }

    drawableSync.lock();
    try {
        /**
         * 'super.addNotify()' determines the GraphicsConfiguration,
         * while calling this class's overriden 'getGraphicsConfiguration()' method
         * after which it creates the native peer.
         * Hence we have to set the 'awtConfig' before since it's GraphicsConfiguration
         * is being used in getGraphicsConfiguration().
         * This code order also allows recreation, ie re-adding the GLCanvas.
         */
        awtConfig = chooseGraphicsConfiguration(capsReqUser, capsReqUser, chooser, device);
        if(null==awtConfig) {
            throw new GLException("Error: NULL AWTGraphicsConfiguration");
        }

        if (!Beans.isDesignTime()) {
            // no lock required, since this resource ain't available yet
            drawable = GLDrawableFactory.getFactory(capsReqUser.getGLProfile())
                           .createGLDrawable(NativeWindowFactory.getNativeWindow(this, awtConfig));
            context = (GLContextImpl) drawable.createContext(shareWith);
            context.setSynchronized(true);
        }

        // before native peer is valid: X11
        disableBackgroundErase();

        // issues getGraphicsConfiguration() and creates the native peer
        super.addNotify();

        // after native peer is valid: Windows
        disableBackgroundErase();

        // init drawable by paint/display makes the init sequence more equal
        // for all launch flavors (applet/javaws/..)
        // validateGLDrawable();

        if(DEBUG) {
            System.err.println(Thread.currentThread().getName()+" - Info: addNotify - end: peer: "+getPeer());
        }
    } finally {
       drawableSync.unlock();
    }
  }

  private boolean validateGLDrawable() {
    boolean realized = false;
    if (!Beans.isDesignTime()) {
        drawableSync.lock();
        try {
            if ( null != drawable ) {
                realized = drawable.isRealized();
                if ( !realized && 0 < drawable.getWidth() * drawable.getHeight() ) {
                    drawable.setRealized(true);
                    realized = true;
                    sendReshape=true; // ensure a reshape is being send ..
                    if(DEBUG) {
                        String msg = Thread.currentThread().getName()+" - Realized Drawable: "+drawable.toString();
                        // System.err.println(msg);
                        Throwable t = new Throwable(msg);
                        t.printStackTrace();
                    }
                }
            }
        } finally {
           drawableSync.unlock();
        }
    }
    return realized;
  }

  /** <p>Overridden to track when this component is removed from a
      container. Subclasses which override this method must call
      super.removeNotify() in their removeNotify() method in order to
      function properly. </p>
      <p>User shall not call this method outside of EDT, read the AWT/Swing specs
      about this.</p>
      <B>Overrides:</B>
      <DL><DD><CODE>removeNotify</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
    @Override
  public void removeNotify() {
    if(DEBUG) {
        Exception ex1 = new Exception(Thread.currentThread().getName()+" - Info: removeNotify - start");
        ex1.printStackTrace();
    }

    awtWindowClosingProtocol.removeClosingListener();

    if (Beans.isDesignTime()) {
      super.removeNotify();
    } else {
      drawableSync.lock();
      try {
        dispose(false);
      } finally {
        context=null;
        drawable=null;
        awtConfig=null;
        super.removeNotify();
        drawableSync.unlock();
      }
    }
    if(DEBUG) {
        System.err.println(Thread.currentThread().getName()+" - Info: removeNotify - end, peer: "+getPeer());
    }
  }

  /** Overridden to cause {@link GLDrawableHelper#reshape} to be
      called on all registered {@link GLEventListener}s. Subclasses
      which override this method must call super.reshape() in
      their reshape() method in order to function properly. <P>

      <B>Overrides:</B>
      <DL><DD><CODE>reshape</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
    @Override
  public void reshape(int x, int y, int width, int height) {
    super.reshape(x, y, width, height);
    sendReshape = true;
  }

  /** <B>Overrides:</B>
      <DL><DD><CODE>update</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
  /** 
   * Overridden from Canvas to prevent the AWT's clearing of the
   * canvas from interfering with the OpenGL rendering.
   */
    @Override
  public void update(Graphics g) {
    paint(g);
  }
  
  public void addGLEventListener(GLEventListener listener) {
    drawableHelper.addGLEventListener(listener);
  }

  public void addGLEventListener(int index, GLEventListener listener) {
    drawableHelper.addGLEventListener(index, listener);
  }

  public void removeGLEventListener(GLEventListener listener) {
    drawableHelper.removeGLEventListener(listener);
  }

  public void setAnimator(GLAnimatorControl animatorControl) {
    drawableHelper.setAnimator(animatorControl);
  }

  public GLAnimatorControl getAnimator() {
    return drawableHelper.getAnimator();
  }

  public void invoke(boolean wait, GLRunnable glRunnable) {
    drawableHelper.invoke(this, wait, glRunnable);
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
    GLContext ctx = getContext();
    return (ctx == null) ? null : ctx.getGL();
  }

  public GL setGL(GL gl) {
    GLContext ctx = getContext();
    if (ctx != null) {
      ctx.setGL(gl);
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
    return capsReqUser.getGLProfile();
  }

  public GLCapabilitiesImmutable getChosenGLCapabilities() {
    if (awtConfig == null) {
        throw new GLException("No AWTGraphicsConfiguration: "+this);
    }

    return (GLCapabilitiesImmutable)awtConfig.getChosenCapabilities();
  }

  public GLCapabilitiesImmutable getRequestedGLCapabilities() {
    if (awtConfig == null) {
        return capsReqUser;
    }

    return (GLCapabilitiesImmutable)awtConfig.getRequestedCapabilities();
  }

  public NativeSurface getNativeSurface() {
      drawableSync.lock();
      try {
        return (null != drawable) ? drawable.getNativeSurface() : null;
      } finally {
        drawableSync.unlock();
      }
  }

  public long getHandle() {
      drawableSync.lock();
      try {
        return (null != drawable) ? drawable.getHandle() : 0;
      } finally {
        drawableSync.unlock();
      }
  }

  public GLDrawableFactory getFactory() {
      drawableSync.lock();
      try {
        return (null != drawable) ? drawable.getFactory() : null;
      } finally {
        drawableSync.unlock();
      }
  }

  @Override
  public String toString() {
	final int dw = (null!=drawable) ? drawable.getWidth() : -1;
	final int dh = (null!=drawable) ? drawable.getHeight() : -1;
	
    return "AWT-GLCanvas[Realized "+isRealized()+
                          ",\n\t"+((null!=drawable)?drawable.getClass().getName():"null-drawable")+                         
                          ",\n\tRealized "+isRealized()+
                          ",\n\tFactory   "+getFactory()+
                          ",\n\thandle    0x"+Long.toHexString(getHandle())+
                          ",\n\tDrawable size "+dw+"x"+dh+
                          ",\n\tAWT pos "+getX()+"/"+getY()+", size "+getWidth()+"x"+getHeight()+
                          ",\n\tvisible "+isVisible()+
                          ",\n\t"+awtConfig+"]";
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

  class DisposeAction implements Runnable {
    public void run() {
      drawableHelper.dispose(GLCanvas.this);

      if(null!=context) {
        context.makeCurrent(); // implicit wait for lock ..
        context.destroy();
        context=null;
      }

      if(null!=drawable) {
          drawable.setRealized(false);
          drawable=null;
      }

      if(disposeRegenerate) {
          // recreate GLDrawable to reflect it's new graphics configuration
          drawable = GLDrawableFactory.getFactory(capsReqUser.getGLProfile())
                        .createGLDrawable(NativeWindowFactory.getNativeWindow(GLCanvas.this, awtConfig));
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
  private boolean disposeRegenerate;
  private DisposeAction disposeAction = new DisposeAction();

  private DisposeOnEventDispatchThreadAction disposeOnEventDispatchThreadAction =
    new DisposeOnEventDispatchThreadAction();

  class DisposeOnEventDispatchThreadAction implements Runnable {
    public void run() {
      drawableHelper.invokeGL(drawable, context, disposeAction, null);
    }
  }

  class DisposeAbstractGraphicsDeviceAction implements Runnable {
    public void run() {
      AbstractGraphicsConfiguration aconfig = (null!=awtConfig) ? awtConfig.getNativeGraphicsConfiguration() : null;
      AbstractGraphicsScreen ascreen = (null!=aconfig) ? aconfig.getScreen() : null;
      AbstractGraphicsDevice adevice = (null!=ascreen) ? ascreen.getDevice() : null;
      if(null!=adevice) {
          String adeviceMsg=null;
          if(DEBUG) {
            adeviceMsg = adevice.toString();
          }
          boolean closed = adevice.close();
          if(DEBUG) {
            System.err.println(Thread.currentThread().getName() + " - GLCanvas.dispose(false): closed GraphicsDevice: "+adeviceMsg+", result: "+closed);
          }
      }
      awtConfig=null;
    }
  }
  private DisposeAbstractGraphicsDeviceAction disposeAbstractGraphicsDeviceAction = new DisposeAbstractGraphicsDeviceAction();

  /**
   * Disposes the AbstractGraphicsDevice within EDT,
   * since resources created (X11: Display), must be destroyed in the same thread, where they have been created.
   *
   * @see #chooseGraphicsConfiguration(javax.media.opengl.GLCapabilitiesImmutable, javax.media.opengl.GLCapabilitiesImmutable, javax.media.opengl.GLCapabilitiesChooser, java.awt.GraphicsDevice)
   */
  void disposeAbstractGraphicsDevice()  {
    if( EventQueue.isDispatchThread() || Thread.holdsLock(getTreeLock()) ) {
        disposeAbstractGraphicsDeviceAction.run();
    } else {
        try {
            EventQueue.invokeAndWait(disposeAbstractGraphicsDeviceAction);
        } catch (InvocationTargetException e) {
            throw new GLException(e.getTargetException());
        } catch (InterruptedException e) {
            throw new GLException(e);
        }
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
        if(DEBUG) {
            System.err.println(Thread.currentThread().getName()+" - reshape: "+getWidth()+"x"+getHeight());
        }
        // Note: we ignore the given x and y within the parent component
        // since we are drawing directly into this heavyweight component.
        drawableHelper.reshape(GLCanvas.this, 0, 0, getWidth(), getHeight());
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
      if(DEBUG) {
        System.err.println("GLCanvas: TK disableBackgroundErase method found: "+
                (null!=disableBackgroundEraseMethod));
      }
    }
    if (disableBackgroundEraseMethod != null) {
      Throwable t=null;
      try {
        disableBackgroundEraseMethod.invoke(getToolkit(), new Object[] { this });
      } catch (Exception e) {
        t = e;
      }
      if(DEBUG) {
        System.err.println("GLCanvas: TK disableBackgroundErase error: "+t);
      }
    }
  }

  /**
   * Issues the GraphicsConfigurationFactory's choosing facility within EDT,
   * since resources created (X11: Display), must be destroyed in the same thread, where they have been created.
   *
   * @param capsChosen
   * @param capsRequested
   * @param chooser
   * @param device
   * @return the chosen AWTGraphicsConfiguration
   *
   * @see #disposeAbstractGraphicsDevice()
   */
  private AWTGraphicsConfiguration chooseGraphicsConfiguration(final GLCapabilitiesImmutable capsChosen,
                                                               final GLCapabilitiesImmutable capsRequested,
                                                               final GLCapabilitiesChooser chooser,
                                                               final GraphicsDevice device) {
    // Make GLCanvas behave better in NetBeans GUI builder
    if (Beans.isDesignTime()) {
      return null;
    }

    final AbstractGraphicsScreen aScreen = AWTGraphicsScreen.createScreenDevice(device, AbstractGraphicsDevice.DEFAULT_UNIT);
    AWTGraphicsConfiguration config = null;

    if( EventQueue.isDispatchThread() || Thread.holdsLock(getTreeLock()) ) {
        config = (AWTGraphicsConfiguration)
                GraphicsConfigurationFactory.getFactory(AWTGraphicsDevice.class).chooseGraphicsConfiguration(capsChosen,
                                                                                                             capsRequested,
                                                                                                             chooser, aScreen);
    } else {
        try {
            final ArrayList bucket = new ArrayList(1);
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    AWTGraphicsConfiguration c = (AWTGraphicsConfiguration)
                            GraphicsConfigurationFactory.getFactory(AWTGraphicsDevice.class).chooseGraphicsConfiguration(capsChosen,
                                                                                                                         capsRequested,
                                                                                                                         chooser, aScreen);
                    bucket.add(c);
                }
            });
            config = ( bucket.size() > 0 ) ? (AWTGraphicsConfiguration)bucket.get(0) : null ;
        } catch (InvocationTargetException e) {
            throw new GLException(e.getTargetException());
        } catch (InterruptedException e) {
            throw new GLException(e);
        }
    }

    if (config == null) {
      throw new GLException("Error: Couldn't fetch AWTGraphicsConfiguration");
    }

    return config;
  }
  
  /**
   * A most simple JOGL AWT test entry
   */
  public static void main(String args[]) {
    System.err.println(VersionUtil.getPlatformInfo());
    System.err.println(GlueGenVersion.getInstance());
    System.err.println(NativeWindowVersion.getInstance());
    System.err.println(JoglVersion.getInstance());

    GLDrawableFactory factory = GLDrawableFactory.getDesktopFactory();
    List/*<GLCapabilitiesImmutable>*/ availCaps = factory.getAvailableCapabilities(null);
    for(int i=0; i<availCaps.size(); i++) {
        System.err.println(availCaps.get(i));
    }

    GLCapabilitiesImmutable caps = new GLCapabilities( GLProfile.getDefault(GLProfile.getDefaultDesktopDevice()) );
    Frame frame = new Frame("JOGL AWT Test");

    GLCanvas glCanvas = new GLCanvas(caps);
    frame.add(glCanvas);
    frame.setSize(128, 128);

    glCanvas.addGLEventListener(new GLEventListener() {
        public void init(GLAutoDrawable drawable) {
            GL gl = drawable.getGL();
            System.err.println(JoglVersion.getGLInfo(gl, null));
        }

        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        }

        public void display(GLAutoDrawable drawable) {
        }

        public void dispose(GLAutoDrawable drawable) {
        }
    });

    final Frame _frame = frame;
    final GLCanvas _glCanvas = glCanvas;

    try {
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                _frame.setVisible(true);
            }});
    } catch (Throwable t) {
        t.printStackTrace();
    }
    glCanvas.display();
    try {
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                _frame.setVisible(false);
                _frame.remove(_glCanvas);
                _frame.dispose();
            }});
    } catch (Throwable t) {
        t.printStackTrace();
    }
  }

}

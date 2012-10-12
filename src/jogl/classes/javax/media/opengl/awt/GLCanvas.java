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

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.OffscreenLayerOption;
import javax.media.nativewindow.VisualIDHolder;
import javax.media.nativewindow.WindowClosingProtocol;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;

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

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.nativewindow.awt.AWTGraphicsConfiguration;
import com.jogamp.nativewindow.awt.AWTGraphicsDevice;
import com.jogamp.nativewindow.awt.AWTGraphicsScreen;
import com.jogamp.nativewindow.awt.AWTWindowClosingProtocol;
import com.jogamp.nativewindow.awt.JAWTWindow;
import com.jogamp.opengl.JoglVersion;

import jogamp.common.awt.AWTEDTExecutor;
import jogamp.opengl.Debug;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableHelper;
import jogamp.opengl.GLDrawableImpl;

// FIXME: Subclasses need to call resetGLFunctionAvailability() on their
// context whenever the displayChanged() function is called on our
// GLEventListeners

/** A heavyweight AWT component which provides OpenGL rendering
    support. This is the primary implementation of an AWT {@link GLDrawable};
    {@link GLJPanel} is provided for compatibility with Swing user
    interfaces when adding a heavyweight doesn't work either because
    of Z-ordering or LayoutManager problems.
 *
 * <h5><A NAME="java2dgl">Offscreen Layer Remarks</A></h5>
 * 
 * {@link OffscreenLayerOption#setShallUseOffscreenLayer(boolean) setShallUseOffscreenLayer(true)}
 * maybe called to use an offscreen drawable (FBO or PBuffer) allowing 
 * the underlying JAWT mechanism to composite the image, if supported.
 * <p>
 * {@link OffscreenLayerOption#setShallUseOffscreenLayer(boolean) setShallUseOffscreenLayer(true)}
 * is being called if {@link GLCapabilitiesImmutable#isOnscreen()} is <code>false</code>.
 * </p>
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

@SuppressWarnings("serial")
public class GLCanvas extends Canvas implements AWTGLAutoDrawable, WindowClosingProtocol, OffscreenLayerOption {

  private static final boolean DEBUG = Debug.debug("GLCanvas");

  private final RecursiveLock lock = LockFactory.createRecursiveLock();
  private final GLDrawableHelper helper = new GLDrawableHelper();
  private AWTGraphicsConfiguration awtConfig;
  private volatile GLDrawableImpl drawable; // volatile: avoid locking for read-only access
  private volatile JAWTWindow jawtWindow; // the JAWTWindow presentation of this AWT Canvas, bound to the 'drawable' lifecycle
  private GLContextImpl context;
  private volatile boolean sendReshape = false; // volatile: maybe written by EDT w/o locking

  // copy of the cstr args, mainly for recreation
  private GLCapabilitiesImmutable capsReqUser;
  private GLCapabilitiesChooser chooser;
  private GLContext shareWith;
  private int additionalCtxCreationFlags = 0;
  private GraphicsDevice device;
  private boolean shallUseOffscreenLayer = false;

  private AWTWindowClosingProtocol awtWindowClosingProtocol =
          new AWTWindowClosingProtocol(this, new Runnable() {
                @Override
                public void run() {
                    GLCanvas.this.destroy();
                }
            });

  /** Creates a new GLCanvas component with a default set of OpenGL
      capabilities, using the default OpenGL capabilities selection
      mechanism, on the default screen device.
   * @throws GLException if no default profile is available for the default desktop device.
   */
  public GLCanvas() throws GLException {
    this(null);
  }

  /** Creates a new GLCanvas component with the requested set of
      OpenGL capabilities, using the default OpenGL capabilities
      selection mechanism, on the default screen device.
   * @throws GLException if no GLCapabilities are given and no default profile is available for the default desktop device.
   * @see GLCanvas#GLCanvas(javax.media.opengl.GLCapabilitiesImmutable, javax.media.opengl.GLCapabilitiesChooser, javax.media.opengl.GLContext, java.awt.GraphicsDevice)
   */
  public GLCanvas(GLCapabilitiesImmutable capsReqUser) throws GLException {
    this(capsReqUser, null, null, null);
  }

  /** Creates a new GLCanvas component with the requested set of
      OpenGL capabilities, using the default OpenGL capabilities
      selection mechanism, on the default screen device.
   *  This constructor variant also supports using a shared GLContext.
   *
   * @throws GLException if no GLCapabilities are given and no default profile is available for the default desktop device.
   * @see GLCanvas#GLCanvas(javax.media.opengl.GLCapabilitiesImmutable, javax.media.opengl.GLCapabilitiesChooser, javax.media.opengl.GLContext, java.awt.GraphicsDevice)
   */
  public GLCanvas(GLCapabilitiesImmutable capsReqUser, GLContext shareWith)
          throws GLException
  {
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
      is passed for this argument.
   * @throws GLException if no GLCapabilities are given and no default profile is available for the default desktop device.
   */
  public GLCanvas(GLCapabilitiesImmutable capsReqUser,
                  GLCapabilitiesChooser chooser,
                  GLContext shareWith,
                  GraphicsDevice device)
      throws GLException
  {
    /*
     * Determination of the native window is made in 'super.addNotify()',
     * which creates the native peer using AWT's GraphicsConfiguration.
     * GraphicsConfiguration is returned by this class overwritten
     * 'getGraphicsConfiguration()', which returns our OpenGL compatible
     * 'chosen' GraphicsConfiguration.
     */
    super();

    if(null==capsReqUser) {
        capsReqUser = new GLCapabilities(GLProfile.getDefault(GLProfile.getDefaultDevice()));
    } else {
        // don't allow the user to change data
        capsReqUser = (GLCapabilitiesImmutable) capsReqUser.cloneMutable();
    }
    if(!capsReqUser.isOnscreen()) {
        setShallUseOffscreenLayer(true); // trigger offscreen layer - if supported
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

  @Override
  public final Object getUpstreamWidget() {
    return this;
  }
     
  @Override
  public void setShallUseOffscreenLayer(boolean v) {
      shallUseOffscreenLayer = v;
  }

  @Override
  public final boolean getShallUseOffscreenLayer() {
      return shallUseOffscreenLayer;
  }

  @Override
  public final boolean isOffscreenLayerSurfaceEnabled() {
      final JAWTWindow _jawtWindow = jawtWindow;
      if(null != _jawtWindow) {
          return _jawtWindow.isOffscreenLayerSurfaceEnabled();
      }
      return false;
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
    GraphicsConfiguration chosen =  awtConfig.getAWTGraphicsConfiguration();

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
        final GraphicsConfiguration compatible = (null!=config)?config.getAWTGraphicsConfiguration():null;
        boolean equalCaps = config.getChosenCapabilities().equals(awtConfig.getChosenCapabilities());
        if(DEBUG) {
            System.err.println(getThreadName()+": Info:");
            System.err.println("Created Config (n): HAVE    GC "+chosen);
            System.err.println("Created Config (n): THIS    GC "+gc);
            System.err.println("Created Config (n): Choosen GC "+compatible);
            System.err.println("Created Config (n): HAVE    CF "+awtConfig);
            System.err.println("Created Config (n): Choosen CF "+config);
            System.err.println("Created Config (n): EQUALS CAPS "+equalCaps);
            Thread.dumpStack();
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

  @Override
  public GLContext createContext(final GLContext shareWith) {
    final RecursiveLock _lock = lock;
    _lock.lock();
    try {
        if(drawable != null) {
          final GLContext _ctx = drawable.createContext(shareWith);
          _ctx.setContextCreationFlags(additionalCtxCreationFlags);
          return _ctx;
        }
        return null;
    } finally {
        _lock.unlock();
    }
  }
    
  @Override
  public void setRealized(boolean realized) {
  }

  @Override
  public boolean isRealized() {
      final GLDrawable _drawable = drawable;
      return ( null != _drawable ) ? _drawable.isRealized() : false;
  }

  @Override
  public WindowClosingMode getDefaultCloseOperation() {
      return awtWindowClosingProtocol.getDefaultCloseOperation();
  }

  @Override
  public WindowClosingMode setDefaultCloseOperation(WindowClosingMode op) {
      return awtWindowClosingProtocol.setDefaultCloseOperation(op);
  }

  @Override
  public void display() {
    if( !validateGLDrawable() ) {
        if(DEBUG) {
            System.err.println(getThreadName()+": Info: GLCanvas display - skipped GL render, drawable not valid yet");
        }
        return; // not yet available ..
    }
    Threading.invoke(true, displayOnEDTAction, getTreeLock());
    awtWindowClosingProtocol.addClosingListenerOneShot();
  }

  private void dispose(boolean regenerate) {
    disposeRegenerate=regenerate;
    Threading.invoke(true, disposeOnEDTAction, getTreeLock());
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * This impl. calls this class's {@link #removeNotify()} AWT override,
   * where the actual implementation resides.
   * </p>
   */
  @Override
  public void destroy() {
    removeNotify();
  }

  /** Overridden to cause OpenGL rendering to be performed during
      repaint cycles. Subclasses which override this method must call
      super.paint() in their paint() method in order to function
      properly.
    */
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
    if( ! this.helper.isExternalAnimatorAnimating() ) {
        display();
    }
  }

  /** Overridden to track when this component is added to a container.
      Subclasses which override this method must call
      super.addNotify() in their addNotify() method in order to
      function properly. <P>

      <B>Overrides:</B>
      <DL><DD><CODE>addNotify</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
    @SuppressWarnings("deprecation")
    @Override
  public void addNotify() {
    final RecursiveLock _lock = lock;
    _lock.lock();
    try {    
        if(DEBUG) {
            System.err.println(getThreadName()+": Info: addNotify - start, bounds: "+this.getBounds());
            Thread.dumpStack();
        }
    
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
    
        // before native peer is valid: X11
        disableBackgroundErase();
    
        // issues getGraphicsConfiguration() and creates the native peer
        super.addNotify();
    
        // after native peer is valid: Windows
        disableBackgroundErase();
    
        if (!Beans.isDesignTime()) {
            createDrawableAndContext();
        }
    
        // init drawable by paint/display makes the init sequence more equal
        // for all launch flavors (applet/javaws/..)
        // validateGLDrawable();
    
        if(DEBUG) {
            System.err.println(getThreadName()+": Info: addNotify - end: peer: "+getPeer());
        }
    } finally {
        _lock.unlock();
    }
  }

  private void createDrawableAndContext() {
    // no lock required, since this resource ain't available yet
    jawtWindow = (JAWTWindow) NativeWindowFactory.getNativeWindow(this, awtConfig);
    jawtWindow.setShallUseOffscreenLayer(shallUseOffscreenLayer);
    jawtWindow.lockSurface();
    try {
        drawable = (GLDrawableImpl) GLDrawableFactory.getFactory(capsReqUser.getGLProfile()).createGLDrawable(jawtWindow);
        context = (GLContextImpl) drawable.createContext(shareWith);
        context.setContextCreationFlags(additionalCtxCreationFlags);
    } finally {
        jawtWindow.unlockSurface();
    }
  }

  private boolean validateGLDrawable() {
    final GLDrawable _drawable = drawable;
    if ( null != _drawable ) {
        if( _drawable.isRealized() ) {
            return true;
        }
        if( Beans.isDesignTime() || !isDisplayable() || 0 >= _drawable.getWidth() || 0 >= _drawable.getHeight() ) {
            return false; // early out!
        }
        // make sure drawable realization happens on AWT EDT, due to AWTTree lock
        AWTEDTExecutor.singleton.invoke(getTreeLock(), true, setRealizedOnEDTAction);
        final boolean res = _drawable.isRealized();
        if(DEBUG) {
            System.err.println(getThreadName()+": Realized Drawable: "+res+", "+_drawable.toString());
            Thread.dumpStack();
        }
        return res;
    }
    return false;
  }
  private Runnable setRealizedOnEDTAction = new Runnable() { 
      public void run() { 
          final RecursiveLock _lock = lock;
          _lock.lock();
          try {            
              final GLDrawable _drawable = drawable;
              if( null == _drawable || 0 >= _drawable.getWidth() || 0 >= _drawable.getHeight() ) {
                  return; 
              }
              _drawable.setRealized(true);
              if( _drawable.isRealized() ) {
                  sendReshape=true; // ensure a reshape is being send ..
              }
          } finally {
              _lock.unlock();
          }
      } };

  /** <p>Overridden to track when this component is removed from a
      container. Subclasses which override this method must call
      super.removeNotify() in their removeNotify() method in order to
      function properly. </p>
      <p>User shall not call this method outside of EDT, read the AWT/Swing specs
      about this.</p>
      <B>Overrides:</B>
      <DL><DD><CODE>removeNotify</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
    @SuppressWarnings("deprecation")
    @Override
  public void removeNotify() {
    if(DEBUG) {
        System.err.println(getThreadName()+": Info: removeNotify - start");
        Thread.dumpStack();
    }

    awtWindowClosingProtocol.removeClosingListener();

    if (Beans.isDesignTime()) {
      super.removeNotify();
    } else {
      try {
        dispose(false);
      } finally {
        super.removeNotify();
      }
    }
    if(DEBUG) {
        System.err.println(getThreadName()+": Info: removeNotify - end, peer: "+getPeer());
    }
  }

  /** Overridden to cause {@link GLDrawableHelper#reshape} to be
      called on all registered {@link GLEventListener}s. Subclasses
      which override this method must call super.reshape() in
      their reshape() method in order to function properly. <P>

      <B>Overrides:</B>
      <DL><DD><CODE>reshape</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
    @SuppressWarnings("deprecation")
    @Override
  public void reshape(int x, int y, int width, int height) {
    synchronized (getTreeLock()) { // super.reshape(..) claims tree lock, so we do extend it's lock over reshape
        super.reshape(x, y, width, height);
        
        if(DEBUG) {
            final NativeSurface ns = getNativeSurface();
            final long nsH = null != ns ? ns.getSurfaceHandle() : 0;
            System.err.println("GLCanvas.sizeChanged: ("+Thread.currentThread().getName()+"): "+width+"x"+height+" - surfaceHandle 0x"+Long.toHexString(nsH));
            // Thread.dumpStack();
        }            
        if( validateGLDrawable() ) {
            final GLDrawableImpl _drawable = drawable;
            if( ! _drawable.getChosenGLCapabilities().isOnscreen() ) {
                final RecursiveLock _lock = lock;
                _lock.lock();
                try {
                    final GLDrawableImpl _drawableNew = GLDrawableHelper.resizeOffscreenDrawable(_drawable, context, width, height);
                    if(_drawable != _drawableNew) {
                        // write back 
                        drawable = _drawableNew;
                    }
                } finally {
                   _lock.unlock();
                }
            }
            sendReshape = true; // async if display() doesn't get called below, but avoiding deadlock
        }
    }
  }

  /**
   * Overridden from Canvas to prevent the AWT's clearing of the
   * canvas from interfering with the OpenGL rendering.
   */
  @Override
  public void update(Graphics g) {
    paint(g);
  }

  @Override
  public void addGLEventListener(GLEventListener listener) {
    helper.addGLEventListener(listener);
  }

  @Override
  public void addGLEventListener(int index, GLEventListener listener) throws IndexOutOfBoundsException {
    helper.addGLEventListener(index, listener);
  }

  @Override
  public void removeGLEventListener(GLEventListener listener) {
    helper.removeGLEventListener(listener);
  }

  @Override
  public GLEventListener removeGLEventListener(int index) throws IndexOutOfBoundsException {
    return helper.removeGLEventListener(index);
  }
  
  @Override
  public void setAnimator(GLAnimatorControl animatorControl) {
    helper.setAnimator(animatorControl);
  }

  @Override
  public GLAnimatorControl getAnimator() {
    return helper.getAnimator();
  }

  @Override
  public boolean invoke(boolean wait, GLRunnable glRunnable) {
    return helper.invoke(this, wait, glRunnable);
  }

  @Override
  public GLContext setContext(GLContext newCtx) {
      final RecursiveLock _lock = lock;
      _lock.lock();
      try {
          final GLContext oldCtx = context;
          final boolean newCtxCurrent = GLDrawableHelper.switchContext(drawable, oldCtx, newCtx, additionalCtxCreationFlags);
          context=(GLContextImpl)newCtx;
          if(newCtxCurrent) {
              context.makeCurrent();
          }
          return oldCtx;
      } finally {
          _lock.unlock();
      }
  }

  @Override
  public final GLDrawable getDelegatedDrawable() {
    return drawable;
  }
  
  @Override
  public GLContext getContext() {
    return context;
  }

  @Override
  public GL getGL() {
    if (Beans.isDesignTime()) {
      return null;
    }
    final GLContext _context = context;
    return (_context == null) ? null : _context.getGL();
  }

  @Override
  public GL setGL(GL gl) {
    final GLContext _context = context;
    if (_context != null) {
      _context.setGL(gl);
      return gl;
    }
    return null;
  }


  @Override
  public void setAutoSwapBufferMode(boolean onOrOff) {
    helper.setAutoSwapBufferMode(onOrOff);
  }

  @Override
  public boolean getAutoSwapBufferMode() {
    return helper.getAutoSwapBufferMode();
  }

  @Override
  public void swapBuffers() {
    Threading.invoke(true, swapBuffersOnEDTAction, getTreeLock());
  }

  @Override
  public void setContextCreationFlags(int flags) {
    additionalCtxCreationFlags = flags;
    final GLContext _context = context;
    if(null != _context) {
      _context.setContextCreationFlags(additionalCtxCreationFlags);
    }
  }

  @Override
  public int getContextCreationFlags() {
    return additionalCtxCreationFlags;
  }

  @Override
  public GLProfile getGLProfile() {
    return capsReqUser.getGLProfile();
  }

  @Override
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

  @Override
  public NativeSurface getNativeSurface() {
    final GLDrawable _drawable = drawable;
    return (null != _drawable) ? _drawable.getNativeSurface() : null;
  }

  @Override
  public long getHandle() {
    final GLDrawable _drawable = drawable;
    return (null != _drawable) ? _drawable.getHandle() : 0;
  }

  @Override
  public GLDrawableFactory getFactory() {
    final GLDrawable _drawable = drawable;
    return (null != _drawable) ? _drawable.getFactory() : null;
  }

  @Override
  public String toString() {
    final GLDrawable _drawable = drawable;
    final int dw = (null!=_drawable) ? _drawable.getWidth() : -1;
    final int dh = (null!=_drawable) ? _drawable.getHeight() : -1;

    return "AWT-GLCanvas[Realized "+isRealized()+
                          ",\n\t"+((null!=_drawable)?_drawable.getClass().getName():"null-drawable")+
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

  private boolean disposeRegenerate;
  private final Runnable postDisposeOnEDTAction = new Runnable() {
    @Override
    public void run() {
      context=null;
      if(null!=drawable) {
          drawable.setRealized(false);
          drawable=null;
          if(null!=jawtWindow) {
            jawtWindow.destroy();
            jawtWindow=null;
          }
      }

      if(disposeRegenerate) {
          // Similar process as in addNotify()!

          // Recreate GLDrawable/GLContext to reflect it's new graphics configuration
          createDrawableAndContext();

          if(DEBUG) {
            System.err.println(getThreadName()+": GLCanvas.dispose(true): new drawable: "+drawable);
          }
          validateGLDrawable(); // immediate attempt to recreate the drawable
      }
    }
  };

  private final Runnable disposeOnEDTAction = new Runnable() {
    @Override
    public void run() {
        final RecursiveLock _lock = lock;
        _lock.lock();
        try {            
            final GLAnimatorControl animator =  getAnimator();

            if(DEBUG) {
                System.err.println(getThreadName()+": Info: dispose("+disposeRegenerate+") - START, hasContext " +
                        (null!=context) + ", hasDrawable " + (null!=drawable)+", "+animator);
                Thread.dumpStack();
            }
        
            if(null!=drawable && null!=context) {
                boolean animatorPaused = false;
                if(null!=animator) {
                    // can't remove us from animator for recreational addNotify()
                    animatorPaused = animator.pause();
                }
        
                if(context.isCreated()) {
                    helper.disposeGL(GLCanvas.this, drawable, context, postDisposeOnEDTAction);
                }
        
                if(animatorPaused) {
                    animator.resume();
                }
            }
        
            if(!disposeRegenerate) {
                if(null != awtConfig) {
                    AWTEDTExecutor.singleton.invoke(getTreeLock(), true, disposeAbstractGraphicsDeviceActionOnEDT);
                }
                awtConfig=null;
            }
        
            if(DEBUG) {
                System.err.println(getThreadName()+": dispose("+disposeRegenerate+") - END, "+animator);
            }
            
        } finally {
            _lock.unlock();
        }
    }
  };

  /**
   * Disposes the AbstractGraphicsDevice within EDT,
   * since resources created (X11: Display), must be destroyed in the same thread, where they have been created.
   *
   * @see #chooseGraphicsConfiguration(javax.media.opengl.GLCapabilitiesImmutable, javax.media.opengl.GLCapabilitiesImmutable, javax.media.opengl.GLCapabilitiesChooser, java.awt.GraphicsDevice)
   */
  private final Runnable disposeAbstractGraphicsDeviceActionOnEDT = new Runnable() {
    @Override
    public void run() {
      if(null != awtConfig) {
          final AbstractGraphicsConfiguration aconfig = awtConfig.getNativeGraphicsConfiguration();
          final AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
          final String adeviceMsg;
          if(DEBUG) {
            adeviceMsg = adevice.toString();
          } else {
            adeviceMsg = null;
          }
          boolean closed = adevice.close();
          if(DEBUG) {
            System.err.println(getThreadName()+": GLCanvas.dispose(false): closed GraphicsDevice: "+adeviceMsg+", result: "+closed);
          }
      }
    }
  };
  
  private final Runnable initAction = new Runnable() {
    @Override
    public void run() {
      helper.init(GLCanvas.this, !sendReshape);
    }
  };

  private final Runnable displayAction = new Runnable() {
    @Override
    public void run() {
      if (sendReshape) {
        if(DEBUG) {
            System.err.println(getThreadName()+": Reshape: "+getWidth()+"x"+getHeight());
        }
        // Note: we ignore the given x and y within the parent component
        // since we are drawing directly into this heavyweight component.
        helper.reshape(GLCanvas.this, 0, 0, getWidth(), getHeight());
        sendReshape = false;
      }

      helper.display(GLCanvas.this);
    }
  };

  // Workaround for ATI driver bugs related to multithreading issues
  // like simultaneous rendering via Animators to canvases that are
  // being resized on the AWT event dispatch thread
  private final Runnable displayOnEDTAction = new Runnable() {
    @Override
    public void run() {
        final RecursiveLock _lock = lock;
        _lock.lock();
        try {            
            helper.invokeGL(drawable, context, displayAction, initAction);
        } finally {
            _lock.unlock();
        }
    }
  };

  private final Runnable swapBuffersOnEDTAction = new Runnable() {
    @Override
    public void run() {
        final RecursiveLock _lock = lock;
        _lock.lock();
        try {
            if(null != drawable) {
                drawable.swapBuffers();
            }
        } finally {
            _lock.unlock();
        }
    }
  };

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
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
              try {
                Class<?> clazz = getToolkit().getClass();
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
        System.err.println(getThreadName()+": GLCanvas: TK disableBackgroundErase method found: "+
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
        System.err.println(getThreadName()+": GLCanvas: TK disableBackgroundErase error: "+t);
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
   * @see #disposeAbstractGraphicsDeviceActionOnEDT
   */
  private AWTGraphicsConfiguration chooseGraphicsConfiguration(final GLCapabilitiesImmutable capsChosen,
                                                               final GLCapabilitiesImmutable capsRequested,
                                                               final GLCapabilitiesChooser chooser,
                                                               final GraphicsDevice device) {
    // Make GLCanvas behave better in NetBeans GUI builder
    if (Beans.isDesignTime()) {
      return null;
    }

    final AbstractGraphicsScreen aScreen = null != device ?
            AWTGraphicsScreen.createScreenDevice(device, AbstractGraphicsDevice.DEFAULT_UNIT):
            AWTGraphicsScreen.createDefault();
    AWTGraphicsConfiguration config = null;

    if( EventQueue.isDispatchThread() || Thread.holdsLock(getTreeLock()) ) {
        config = (AWTGraphicsConfiguration)
                GraphicsConfigurationFactory.getFactory(AWTGraphicsDevice.class, GLCapabilitiesImmutable.class).chooseGraphicsConfiguration(capsChosen,
                                                                                                             capsRequested,
                                                                                                             chooser, aScreen, VisualIDHolder.VID_UNDEFINED);
    } else {
        try {
            final ArrayList<AWTGraphicsConfiguration> bucket = new ArrayList<AWTGraphicsConfiguration>(1);
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    AWTGraphicsConfiguration c = (AWTGraphicsConfiguration)
                            GraphicsConfigurationFactory.getFactory(AWTGraphicsDevice.class, GLCapabilitiesImmutable.class).chooseGraphicsConfiguration(capsChosen,
                                                                                                                         capsRequested,
                                                                                                                         chooser, aScreen, VisualIDHolder.VID_UNDEFINED);
                    bucket.add(c);
                }
            });
            config = ( bucket.size() > 0 ) ? bucket.get(0) : null ;
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

  protected static String getThreadName() {
    return Thread.currentThread().getName();
  }

  /**
   * A most simple JOGL AWT test entry
   */
  public static void main(String args[]) {
    System.err.println(VersionUtil.getPlatformInfo());
    System.err.println(GlueGenVersion.getInstance());
    // System.err.println(NativeWindowVersion.getInstance());
    System.err.println(JoglVersion.getInstance());

    System.err.println(JoglVersion.getDefaultOpenGLInfo(null, null, true).toString());

    final GLCapabilitiesImmutable caps = new GLCapabilities( GLProfile.getDefault(GLProfile.getDefaultDevice()) );
    final Frame frame = new Frame("JOGL AWT Test");

    final GLCanvas glCanvas = new GLCanvas(caps);
    frame.add(glCanvas);
    frame.setSize(128, 128);

    glCanvas.addGLEventListener(new GLEventListener() {
        @Override
        public void init(GLAutoDrawable drawable) {
            GL gl = drawable.getGL();
            System.err.println(JoglVersion.getGLInfo(gl, null));
        }
        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) { }
        @Override
        public void display(GLAutoDrawable drawable) { }
        @Override
        public void dispose(GLAutoDrawable drawable) { }
    });

    try {
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);
            }});
    } catch (Throwable t) {
        t.printStackTrace();
    }
    glCanvas.display();
    try {
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.dispose();
            }});
    } catch (Throwable t) {
        t.printStackTrace();
    }
  }

}

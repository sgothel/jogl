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
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

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
import com.jogamp.common.util.awt.AWTEDTExecutor;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.nativewindow.awt.AWTGraphicsConfiguration;
import com.jogamp.nativewindow.awt.AWTGraphicsDevice;
import com.jogamp.nativewindow.awt.AWTGraphicsScreen;
import com.jogamp.nativewindow.awt.AWTWindowClosingProtocol;
import com.jogamp.nativewindow.awt.JAWTWindow;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.util.GLDrawableUtil;
import com.jogamp.opengl.util.TileRenderer;

import jogamp.opengl.Debug;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableHelper;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.awt.AWTTilePainter;

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
public class GLCanvas extends Canvas implements AWTGLAutoDrawable, WindowClosingProtocol, OffscreenLayerOption, AWTPrintLifecycle {

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
                    GLCanvas.this.destroyImpl( true );
                }
            }, null);

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
    
    if( Beans.isDesignTime() ) {
        return gc;
    }
    
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
            // Thread.dumpStack();
        }

        if (compatible != null) {
          /*
           * Save the new GC for equals test above, and to return to
           * any outside callers of this method.
           */
          chosen = compatible;

          if( !equalCaps && GLAutoDrawable.SCREEN_CHANGE_ACTION_ENABLED ) {
              // complete destruction!
              destroyImpl( true );
              // recreation!
              awtConfig = config;
              createDrawableAndContext( true );
              validateGLDrawable();
          } else {
              awtConfig = config;
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
    
  private final void setRealizedImpl(boolean realized) {
      final RecursiveLock _lock = lock;
      _lock.lock();
      try {            
          final GLDrawable _drawable = drawable;
          if( null == _drawable || realized == _drawable.isRealized() ||
              realized && ( 0 >= _drawable.getWidth() || 0 >= _drawable.getHeight() ) ) {
              return;
          }
         _drawable.setRealized(realized);
          if( realized && _drawable.isRealized() ) {
              sendReshape=true; // ensure a reshape is being send ..
          }
      } finally {
          _lock.unlock();
      }
  }  
  private final Runnable realizeOnEDTAction = new Runnable() { public void run() { setRealizedImpl(true); } };
  private final Runnable unrealizeOnEDTAction = new Runnable() { public void run() { setRealizedImpl(false); } };
  
  @Override
  public final void setRealized(boolean realized) {
      // Make sure drawable realization happens on AWT-EDT and only there. Consider the AWTTree lock!
      AWTEDTExecutor.singleton.invoke(getTreeLock(), false /* allowOnNonEDT */, true /* wait */, realized ? realizeOnEDTAction : unrealizeOnEDTAction);
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
    if( isVisible() && !printActive ) {
        Threading.invoke(true, displayOnEDTAction, getTreeLock());
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * This impl. only destroys all GL related resources.
   * </p>
   * <p>
   * This impl. does not remove the GLCanvas from it's parent AWT container
   * so this class's {@link #removeNotify()} AWT override won't get called.
   * To do so, remove this component from it's parent AWT container.
   * </p>
   */
  @Override
  public void destroy() {
    destroyImpl( false );
  }
  
  protected void destroyImpl(boolean destroyJAWTWindowAndAWTDevice) {
    Threading.invoke(true, destroyOnEDTAction, getTreeLock());
    if( destroyJAWTWindowAndAWTDevice ) {
        AWTEDTExecutor.singleton.invoke(getTreeLock(), true /* allowOnNonEDT */, true /* wait */, disposeJAWTWindowAndAWTDeviceOnEDT);
    }
  }

  /** Overridden to cause OpenGL rendering to be performed during
      repaint cycles. Subclasses which override this method must call
      super.paint() in their paint() method in order to function
      properly.
    */
  @Override
  public void paint(Graphics g) {
    if( Beans.isDesignTime() ) {
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
    } else if( !this.helper.isAnimatorAnimatingOnOtherThread() ) {
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
        final boolean isBeansDesignTime = Beans.isDesignTime();
        
        if(DEBUG) {
            System.err.println(getThreadName()+": Info: addNotify - start, bounds: "+this.getBounds()+", isBeansDesignTime "+isBeansDesignTime);
            // Thread.dumpStack();
        }
    
        if( isBeansDesignTime ) {
            super.addNotify();
        } else {
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
        
            createDrawableAndContext( true );
        
            // init drawable by paint/display makes the init sequence more equal
            // for all launch flavors (applet/javaws/..)
            // validateGLDrawable();            
        }
        awtWindowClosingProtocol.addClosingListener();

        if(DEBUG) {
            System.err.println(getThreadName()+": Info: addNotify - end: peer: "+getPeer());
        }
    } finally {
        _lock.unlock();
    }
  }

  private void createDrawableAndContext(boolean createJAWTWindow) {
    if ( !Beans.isDesignTime() ) {
        if( createJAWTWindow ) {
            jawtWindow = (JAWTWindow) NativeWindowFactory.getNativeWindow(this, awtConfig);
            jawtWindow.setShallUseOffscreenLayer(shallUseOffscreenLayer);            
        }
        jawtWindow.lockSurface();
        try {
            drawable = (GLDrawableImpl) GLDrawableFactory.getFactory(capsReqUser.getGLProfile()).createGLDrawable(jawtWindow);
            context = (GLContextImpl) drawable.createContext(shareWith);
            context.setContextCreationFlags(additionalCtxCreationFlags);
        } finally {
            jawtWindow.unlockSurface();
        }
    }
  }  

  private boolean validateGLDrawable() {
      if( Beans.isDesignTime() || !isDisplayable() ) {
          return false; // early out!
      }
      final GLDrawable _drawable = drawable;
      if ( null != _drawable ) {
          if( _drawable.isRealized() ) {
              return true;
          }
          if( 0 >= _drawable.getWidth() || 0 >= _drawable.getHeight() ) {
              return false; // early out!
          }
          setRealized(true);
          final boolean res = _drawable.isRealized();
          if(DEBUG) {
              System.err.println(getThreadName()+": Realized Drawable: isRealized "+res+", "+_drawable.toString());
              // Thread.dumpStack();
          }
          return res;
      }
      return false;
  }
  
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
        // Thread.dumpStack();
    }

    awtWindowClosingProtocol.removeClosingListener();

    if( Beans.isDesignTime() ) {
      super.removeNotify();
    } else {
      try {
        destroyImpl( true );
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
            System.err.println("GLCanvas.sizeChanged: ("+getThreadName()+"): "+width+"x"+height+" - surfaceHandle 0x"+Long.toHexString(nsH));
            // Thread.dumpStack();
        }            
        if( validateGLDrawable() && !printActive ) {
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

  private volatile boolean printActive = false;
  private boolean printUseAA = false;
  private GLAnimatorControl printAnimator = null; 
  private GLAutoDrawable printGLAD = null;
  private AWTTilePainter printAWTTiles = null;
  
  @Override
  public void setupPrint(Graphics2D g2d, double scaleMatX, double scaleMatY) {
      if( !validateGLDrawable() ) {
          if(DEBUG) {
              System.err.println(getThreadName()+": Info: GLCanvas setupPrint - skipped GL render, drawable not valid yet");
          }
          return; // not yet available ..
      }
      if( !isVisible() ) {
          if(DEBUG) {
              System.err.println(getThreadName()+": Info: GLCanvas setupPrint - skipped GL render, drawable visible");
          }
          return; // not yet available ..
      }
      printActive = true; 
      sendReshape = false; // clear reshape flag
      final RenderingHints rHints = g2d.getRenderingHints();
      {
          final Object _useAA = rHints.get(RenderingHints.KEY_ANTIALIASING);
          printUseAA = null != _useAA && ( _useAA == RenderingHints.VALUE_ANTIALIAS_DEFAULT || _useAA == RenderingHints.VALUE_ANTIALIAS_ON );
      }
      if( DEBUG ) {
          System.err.println("AWT print.setup: canvasSize "+getWidth()+"x"+getWidth()+", scaleMat "+scaleMatX+" x "+scaleMatY+", useAA "+printUseAA+", printAnimator "+printAnimator);
          AWTTilePainter.dumpHintsAndScale(g2d);
      }
      final int componentCount = isOpaque() ? 3 : 4;
      final TileRenderer printRenderer = new TileRenderer();
      printAWTTiles = new AWTTilePainter(printRenderer, componentCount, scaleMatX, scaleMatY, DEBUG);
      AWTEDTExecutor.singleton.invoke(getTreeLock(), true /* allowOnNonEDT */, true /* wait */, setupPrintOnEDT);
  }
  private final Runnable setupPrintOnEDT = new Runnable() {
      @Override
      public void run() {
          sendReshape = false; // clear reshape flag
          printAnimator =  helper.getAnimator();
          if( null != printAnimator ) {
              printAnimator.remove(GLCanvas.this);
          }
          final GLCapabilities caps = (GLCapabilities)getChosenGLCapabilities().cloneMutable();
          final GLProfile glp = caps.getGLProfile();
          if( caps.getSampleBuffers() ) {
              // bug / issue w/ swapGLContextAndAllGLEventListener and onscreen MSAA w/ NV/GLX
              printGLAD = GLCanvas.this;
          } else {
              caps.setDoubleBuffered(false);
              caps.setOnscreen(false);
              if( printUseAA && !caps.getSampleBuffers() ) {
                  if ( !glp.isGL2ES3() ) {
                      if( DEBUG ) {
                          System.err.println("Ignore MSAA due to gl-profile < GL2ES3");
                      }
                      printUseAA = false;
                  } else {
                      caps.setSampleBuffers(true);
                      caps.setNumSamples(8);
                  }
              }
              final GLDrawableFactory factory = GLDrawableFactory.getFactory(caps.getGLProfile());
              printGLAD = factory.createOffscreenAutoDrawable(null, caps, null, DEFAULT_PRINT_TILE_SIZE, DEFAULT_PRINT_TILE_SIZE, null);
              GLDrawableUtil.swapGLContextAndAllGLEventListener(GLCanvas.this, printGLAD);
          }
          
          printAWTTiles.renderer.setTileSize(printGLAD.getWidth(), printGLAD.getHeight(), 0);
          printAWTTiles.renderer.attachToAutoDrawable(printGLAD);
          if( DEBUG ) {
              System.err.println("AWT print.setup "+printAWTTiles);
              System.err.println("AWT print.setup AA "+printUseAA+", "+caps);
              System.err.println("AWT print.setup "+printGLAD);
          }
      }
  };
  
  @Override
  public void releasePrint() {
      if( !printActive || null == printGLAD ) {
          throw new IllegalStateException("setupPrint() not called");
      }
      sendReshape = false; // clear reshape flag
      AWTEDTExecutor.singleton.invoke(getTreeLock(), true /* allowOnNonEDT */, true /* wait */, releasePrintOnEDT);
      sendReshape = true; // trigger reshape, i.e. gl-viewport and -listener - this component might got resized!
      display();
  }
  private final Runnable releasePrintOnEDT = new Runnable() {
      @Override
      public void run() {
          if( DEBUG ) {
              System.err.println("AWT print.release "+printAWTTiles);
          }
          printAWTTiles.dispose();
          printAWTTiles= null;
          if( printGLAD != GLCanvas.this ) {
              GLDrawableUtil.swapGLContextAndAllGLEventListener(printGLAD, GLCanvas.this);
              printGLAD.destroy();
          }
          printGLAD = null;
          if( null != printAnimator ) {
              printAnimator.add(GLCanvas.this);
              printAnimator = null;
          }
          printActive = false;
      }
  };
  
  @Override
  public void print(Graphics graphics) {
      if( !printActive || null == printGLAD ) {
          throw new IllegalStateException("setupPrint() not called");
      }
      if(DEBUG && !EventQueue.isDispatchThread()) {
          System.err.println(getThreadName()+": Warning: GLCanvas print - not called from AWT-EDT");
          // we cannot dispatch print on AWT-EDT due to printing internal locking ..
      }
      sendReshape = false; // clear reshape flag
      
      final Graphics2D g2d = (Graphics2D)graphics;
      printAWTTiles.setupGraphics2DAndClipBounds(g2d, getWidth(), getHeight());
      try {
          final TileRenderer tileRenderer = printAWTTiles.renderer;
          if( DEBUG ) {
              System.err.println("AWT print.0: "+tileRenderer);
          }
          do {
              if( printGLAD != GLCanvas.this ) {
                  tileRenderer.display();
              } else {
                  Threading.invoke(true, displayOnEDTAction, getTreeLock());
              }
          } while ( !tileRenderer.eot() );
      } finally {
          printAWTTiles.resetGraphics2D();
      }
      if( DEBUG ) {
          System.err.println("AWT print.X: "+printAWTTiles);
      }
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
  public int getGLEventListenerCount() {
      return helper.getGLEventListenerCount();
  }

  @Override
  public GLEventListener getGLEventListener(int index) throws IndexOutOfBoundsException {
      return helper.getGLEventListener(index);
  }

  @Override
  public boolean getGLEventListenerInitState(GLEventListener listener) {
      return helper.getGLEventListenerInitState(listener);
  }

  @Override
  public void setGLEventListenerInitState(GLEventListener listener, boolean initialized) {
      helper.setGLEventListenerInitState(listener, initialized);
  }
  
  @Override
  public GLEventListener disposeGLEventListener(GLEventListener listener, boolean remove) {
    final DisposeGLEventListenerAction r = new DisposeGLEventListenerAction(listener, remove);
    Threading.invoke(true, r, getTreeLock());
    return r.listener;
  }
  
  @Override
  public GLEventListener removeGLEventListener(GLEventListener listener) {
    return helper.removeGLEventListener(listener);
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
  public final Thread setExclusiveContextThread(Thread t) throws GLException {
      return helper.setExclusiveContextThread(t, context);
  }

  @Override
  public final Thread getExclusiveContextThread() {
      return helper.getExclusiveContextThread();
  }

  @Override
  public boolean invoke(boolean wait, GLRunnable glRunnable) {
    return helper.invoke(this, wait, glRunnable);
  }

  @Override
  public boolean invoke(final boolean wait, final List<GLRunnable> glRunnables) {
    return helper.invoke(this, wait, glRunnables);
  }
  
  @Override
  public GLContext setContext(GLContext newCtx, boolean destroyPrevCtx) {
      final RecursiveLock _lock = lock;
      _lock.lock();
      try {
          final GLContext oldCtx = context;
          GLDrawableHelper.switchContext(drawable, oldCtx, destroyPrevCtx, newCtx, additionalCtxCreationFlags);
          context=(GLContextImpl)newCtx;
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
    if( Beans.isDesignTime() ) {
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
    if( Beans.isDesignTime() ) {
        return capsReqUser;
    } else if( null == awtConfig ) {
        throw new GLException("No AWTGraphicsConfiguration: "+this);
    }
    return (GLCapabilitiesImmutable)awtConfig.getChosenCapabilities();
  }

  public GLCapabilitiesImmutable getRequestedGLCapabilities() {
    if( null == awtConfig ) {
        return capsReqUser;
    }
    return (GLCapabilitiesImmutable)awtConfig.getRequestedCapabilities();
  }

  @Override
  public boolean isGLOriented() {
    final GLDrawable _drawable = drawable;
    return null != _drawable ? _drawable.isGLOriented() : true;
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
                          ",\n\tvisible "+isVisible()+", displayable "+isDisplayable()+
                          ",\n\t"+awtConfig+"]";
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private final Runnable destroyOnEDTAction = new Runnable() {
    @Override
    public void run() {
        final RecursiveLock _lock = lock;
        _lock.lock();
        try {            
            final GLAnimatorControl animator =  getAnimator();

            if(DEBUG) {
                System.err.println(getThreadName()+": Info: destroyOnEDTAction() - START, hasContext " +
                        (null!=context) + ", hasDrawable " + (null!=drawable)+", "+animator);
                // Thread.dumpStack();
            }
                    
            final boolean animatorPaused;
            if(null!=animator) {
                // can't remove us from animator for recreational addNotify()
                animatorPaused = animator.pause();
            } else {
                animatorPaused = false;
            }
            
            // OLS will be detached by disposeGL's context destruction below
            if( null != context ) {
                if( context.isCreated() ) {
                    // Catch dispose GLExceptions by GLEventListener, just 'print' them
                    // so we can continue with the destruction.
                    try {
                        helper.disposeGL(GLCanvas.this, context, true);
                        if(DEBUG) {
                            System.err.println(getThreadName()+": destroyOnEDTAction() - post ctx: "+context);
                        }
                    } catch (GLException gle) {
                        gle.printStackTrace();
                    }
                }
                context = null;
            }
            if( null != drawable ) {
                drawable.setRealized(false);
                if(DEBUG) {
                    System.err.println(getThreadName()+": destroyOnEDTAction() - post drawable: "+drawable);
                }
                drawable = null;
            }

            if(animatorPaused) {
                animator.resume();
            }
            
            if(DEBUG) {
                System.err.println(getThreadName()+": dispose() - END, animator "+animator);
            }
            
        } finally {
            _lock.unlock();
        }
    }
  };

  /**
   * Disposes the JAWTWindow and AbstractGraphicsDevice within EDT,
   * since resources created (X11: Display), must be destroyed in the same thread, where they have been created.
   * <p>
   * The drawable and context handle are null'ed as well, assuming {@link #destroy()} has been called already.
   * </p>
   *
   * @see #chooseGraphicsConfiguration(javax.media.opengl.GLCapabilitiesImmutable, javax.media.opengl.GLCapabilitiesImmutable, javax.media.opengl.GLCapabilitiesChooser, java.awt.GraphicsDevice)
   */
  private final Runnable disposeJAWTWindowAndAWTDeviceOnEDT = new Runnable() {
    @Override
    public void run() {
        context=null;
        drawable=null;

        if( null != jawtWindow ) {
            jawtWindow.destroy();
            if(DEBUG) {
                System.err.println(getThreadName()+": GLCanvas.disposeJAWTWindowAndAWTDeviceOnEDT(): post JAWTWindow: "+jawtWindow);
            }
            jawtWindow=null;
        }
        
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
                System.err.println(getThreadName()+": GLCanvas.disposeJAWTWindowAndAWTDeviceOnEDT(): post GraphicsDevice: "+adeviceMsg+", result: "+closed);
            }
        }
        awtConfig=null;
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

  private final Runnable displayOnEDTAction = new Runnable() {
    @Override
    public void run() {
        final RecursiveLock _lock = lock;
        _lock.lock();
        try {
            if( null != drawable && drawable.isRealized() ) {
                helper.invokeGL(drawable, context, displayAction, initAction);
            }
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
            if( null != drawable && drawable.isRealized() ) {
                drawable.swapBuffers();
            }
        } finally {
            _lock.unlock();
        }
    }
  };

  private class DisposeGLEventListenerAction implements Runnable {
    GLEventListener listener;
    private boolean remove;
    private DisposeGLEventListenerAction(GLEventListener listener, boolean remove) {
        this.listener = listener;
        this.remove = remove;
    }
    
    @Override
    public void run() {
        final RecursiveLock _lock = lock;
        _lock.lock();
        try {
            listener = helper.disposeGLEventListener(GLCanvas.this, drawable, context, listener, remove);
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
   * @see #disposeJAWTWindowAndAWTDeviceOnEDT
   */
  private AWTGraphicsConfiguration chooseGraphicsConfiguration(final GLCapabilitiesImmutable capsChosen,
                                                               final GLCapabilitiesImmutable capsRequested,
                                                               final GLCapabilitiesChooser chooser,
                                                               final GraphicsDevice device) {
    // Make GLCanvas behave better in NetBeans GUI builder
    if( Beans.isDesignTime() ) {
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

  protected static String getThreadName() { return Thread.currentThread().getName(); }

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

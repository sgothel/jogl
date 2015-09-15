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

package com.jogamp.opengl.awt;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.beans.Beans;
import java.nio.IntBuffer;
import java.util.List;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.nativewindow.SurfaceUpdatedListener;
import com.jogamp.nativewindow.WindowClosingProtocol;
import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLFBODrawable;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.GLSharedContextSetter;
import com.jogamp.opengl.Threading;
import javax.swing.JPanel;

import jogamp.nativewindow.SurfaceScaleUtils;
import jogamp.nativewindow.WrappedSurface;
import jogamp.nativewindow.jawt.JAWTUtil;
import jogamp.opengl.Debug;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDrawableHelper;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.awt.AWTTilePainter;
import jogamp.opengl.awt.Java2D;
import jogamp.opengl.util.glsl.GLSLTextureRaster;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.awt.AWTEDTExecutor;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.nativewindow.awt.AWTPrintLifecycle;
import com.jogamp.nativewindow.awt.AWTWindowClosingProtocol;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.GLRendererQuirks;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.GLPixelBuffer.SingletonGLPixelBufferProvider;
import com.jogamp.opengl.util.GLDrawableUtil;
import com.jogamp.opengl.util.GLPixelStorageModes;
import com.jogamp.opengl.util.TileRenderer;
import com.jogamp.opengl.util.awt.AWTGLPixelBuffer;
import com.jogamp.opengl.util.awt.AWTGLPixelBuffer.AWTGLPixelBufferProvider;
import com.jogamp.opengl.util.awt.AWTGLPixelBuffer.SingleAWTGLPixelBufferProvider;
import com.jogamp.opengl.util.texture.TextureState;

/** A lightweight Swing component which provides OpenGL rendering
    support. Provided for compatibility with Swing user interfaces
    when adding a heavyweight doesn't work either because of
    Z-ordering or LayoutManager problems.
    <p>
    The GLJPanel can be made transparent by creating it with a
    GLCapabilities object with alpha bits specified and calling {@link
    #setOpaque}(false). Pixels with resulting OpenGL alpha values less
    than 1.0 will be overlaid on any underlying Swing rendering.
    </p>
    <p>
    This component attempts to use hardware-accelerated rendering via FBO or pbuffers and
    falls back on to software rendering if none of the former are available
    using {@link GLDrawableFactory#createOffscreenDrawable(AbstractGraphicsDevice, GLCapabilitiesImmutable, GLCapabilitiesChooser, int, int) GLDrawableFactory.createOffscreenDrawable(..)}.<br/>
    </p>
    <p>
    <a name="verticalFlip">A vertical-flip is required</a>, if the drawable {@link #isGLOriented()} and {@link #setSkipGLOrientationVerticalFlip(boolean) vertical flip is not skipped}.<br>
    In this case this component performs the required vertical flip to bring the content from OpenGL's orientation into AWT's orientation.<br>
    In case <a href="#fboGLSLVerticalFlip">GLSL based vertical-flip</a> is not available,
    the CPU intensive {@link System#arraycopy(Object, int, Object, int, int) System.arraycopy(..)} is used line by line.
    See details about <a href="#fboGLSLVerticalFlip">FBO and GLSL vertical flipping</a>.
    </p>
    <p>
    For performance reasons, as well as for <a href="#bug842">GL state sideeffects</a>,
    <b>{@link #setSkipGLOrientationVerticalFlip(boolean) skipping vertical flip} is highly recommended</b>!
    </p>
    <p>
    The OpenGL path is concluded by copying the rendered pixels an {@link BufferedImage} via {@link GL#glReadPixels(int, int, int, int, int, int, java.nio.Buffer) glReadPixels(..)}
    for later Java2D composition.
    </p>
    <p>
    Finally the Java2D compositioning takes place via via {@link Graphics#drawImage(java.awt.Image, int, int, int, int, java.awt.image.ImageObserver) Graphics.drawImage(...)}
    on the prepared {@link BufferedImage} as described above.
    </p>
    <P>
 *  Please read <a href="GLCanvas.html#java2dgl">Java2D OpenGL Remarks</a>.
 *  </P>
 *
    <a name="fboGLSLVerticalFlip"><h5>FBO / GLSL Vertical Flip</h5></a>
    If <a href="#verticalFlip">vertical flip is required</a>,
    FBO is used, GLSL is available and {@link #setSkipGLOrientationVerticalFlip(boolean) vertical flip is not skipped}, a fragment shader is utilized
    to flip the FBO texture vertically. This hardware-accelerated step can be disabled via system property <code>jogl.gljpanel.noglsl</code>.
    <p>
    The FBO / GLSL code path uses one texture-unit and binds the FBO texture to it's active texture-target,
    see {@link #setTextureUnit(int)} and {@link #getTextureUnit()}.
    </p>
    <p>
    The active and dedicated texture-unit's {@link GL#GL_TEXTURE_2D} state is preserved via {@link TextureState}.
    See also {@link Texture#textureCallOrder Order of Texture Commands}.
    </p>
    <p>
    The current gl-viewport is preserved.
    </p>
    <p>
    <a name="bug842"><i>Warning (Bug 842)</i></a>: Certain GL states other than viewport and texture (see above)
    influencing rendering, will also influence the GLSL vertical flip, e.g. {@link GL#glFrontFace(int) glFrontFace}({@link GL#GL_CCW}).
    It is recommended to reset those states to default when leaving the {@link GLEventListener#display(GLAutoDrawable)} method!
    We may change this behavior in the future, i.e. preserve all influencing states.
    </p>
    <p>
    <a name="contextSharing"><h5>OpenGL Context Sharing</h5></a>
    To share a {@link GLContext} see the following note in the documentation overview:
    <a href="../../../../overview-summary.html#SHARING">context sharing</a>
    as well as {@link GLSharedContextSetter}.
    </p>
*/

@SuppressWarnings("serial")
public class GLJPanel extends JPanel implements AWTGLAutoDrawable, WindowClosingProtocol, AWTPrintLifecycle, GLSharedContextSetter, ScalableSurface {
  private static final boolean DEBUG;
  private static final boolean DEBUG_FRAMES;
  private static final boolean DEBUG_VIEWPORT;
  private static final boolean USE_GLSL_TEXTURE_RASTERIZER;
  private static final boolean SKIP_VERTICAL_FLIP_DEFAULT;

  /** Indicates whether the Java 2D OpenGL pipeline is requested by user. */
  private static final boolean java2dOGLEnabledByProp;

  /** Indicates whether the Java 2D OpenGL pipeline is enabled, resource-compatible and requested by user. */
  private static final boolean useJava2DGLPipeline;

  /** Indicates whether the Java 2D OpenGL pipeline's usage is error free. */
  private static boolean java2DGLPipelineOK;

  static {
      Debug.initSingleton();
      DEBUG = Debug.debug("GLJPanel");
      DEBUG_FRAMES = PropertyAccess.isPropertyDefined("jogl.debug.GLJPanel.Frames", true);
      DEBUG_VIEWPORT = PropertyAccess.isPropertyDefined("jogl.debug.GLJPanel.Viewport", true);
      USE_GLSL_TEXTURE_RASTERIZER = !PropertyAccess.isPropertyDefined("jogl.gljpanel.noglsl", true);
      SKIP_VERTICAL_FLIP_DEFAULT = PropertyAccess.isPropertyDefined("jogl.gljpanel.noverticalflip", true);
      boolean enabled = PropertyAccess.getBooleanProperty("sun.java2d.opengl", false);
      java2dOGLEnabledByProp = enabled && !PropertyAccess.isPropertyDefined("jogl.gljpanel.noogl", true);

      enabled = false;
      if( java2dOGLEnabledByProp ) {
          // Force eager initialization of part of the Java2D class since
          // otherwise it's likely it will try to be initialized while on
          // the Queue Flusher Thread, which is not allowed
          if (Java2D.isOGLPipelineResourceCompatible() && Java2D.isFBOEnabled()) {
              if( null != Java2D.getShareContext(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()) ) {
                  enabled = true;
              }
          }
      }
      useJava2DGLPipeline = enabled;
      java2DGLPipelineOK = enabled;
      if( DEBUG ) {
          System.err.println("GLJPanel: DEBUG_VIEWPORT "+DEBUG_VIEWPORT);
          System.err.println("GLJPanel: USE_GLSL_TEXTURE_RASTERIZER "+USE_GLSL_TEXTURE_RASTERIZER);
          System.err.println("GLJPanel: SKIP_VERTICAL_FLIP_DEFAULT "+SKIP_VERTICAL_FLIP_DEFAULT);
          System.err.println("GLJPanel: java2dOGLEnabledByProp "+java2dOGLEnabledByProp);
          System.err.println("GLJPanel: useJava2DGLPipeline "+useJava2DGLPipeline);
          System.err.println("GLJPanel: java2DGLPipelineOK "+java2DGLPipelineOK);
      }
  }

  private static SingleAWTGLPixelBufferProvider singleAWTGLPixelBufferProvider = null;
  private static synchronized SingleAWTGLPixelBufferProvider getSingleAWTGLPixelBufferProvider() {
      if( null == singleAWTGLPixelBufferProvider ) {
          singleAWTGLPixelBufferProvider = new SingleAWTGLPixelBufferProvider( true /* allowRowStride */ );
      }
      return singleAWTGLPixelBufferProvider;
  }

  private final RecursiveLock lock = LockFactory.createRecursiveLock();

  private final GLDrawableHelper helper;
  private boolean autoSwapBufferMode;

  private volatile boolean isInitialized;

  //
  // Data used for either pbuffers or pixmap-based offscreen surfaces
  //
  private AWTGLPixelBufferProvider customPixelBufferProvider = null;
  /** Requested single buffered offscreen caps */
  private volatile GLCapabilitiesImmutable reqOffscreenCaps;
  private volatile GLDrawableFactoryImpl factory;
  private final GLCapabilitiesChooser chooser;
  private int additionalCtxCreationFlags = 0;

  // Lazy reshape notification: reshapeWidth -> panelWidth -> backend.width
  private boolean handleReshape = false;
  private boolean sendReshape = true;

  private final float[] minPixelScale = new float[] { ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE };
  private final float[] maxPixelScale = new float[] { ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE };
  private final float[] hasPixelScale = new float[] { ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE };
  private final float[] reqPixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };

  /** For handling reshape events lazily: reshapeWidth -> panelWidth -> backend.width in pixel units (scaled) */
  private int reshapeWidth;
  /** For handling reshape events lazily: reshapeHeight -> panelHeight -> backend.height in pixel units (scaled) */
  private int reshapeHeight;

  /** Scaled pixel width of the actual GLJPanel: reshapeWidth -> panelWidth -> backend.width */
  private int panelWidth   = 0;
  /** Scaled pixel height of the actual GLJPanel: reshapeHeight -> panelHeight -> backend.height */
  private int panelHeight  = 0;

  // These are always set to (0, 0) except when the Java2D / OpenGL
  // pipeline is active
  private int viewportX;
  private int viewportY;

  private int requestedTextureUnit = 0; // default

  // The backend in use
  private volatile Backend backend;

  private boolean skipGLOrientationVerticalFlip = SKIP_VERTICAL_FLIP_DEFAULT;

  // Used by all backends either directly or indirectly to hook up callbacks
  private final Updater updater = new Updater();

  private boolean oglPipelineUsable() {
      return null == customPixelBufferProvider &&  useJava2DGLPipeline && java2DGLPipelineOK;
  }

  private volatile boolean isShowing;
  private final HierarchyListener hierarchyListener = new HierarchyListener() {
      @Override
      public void hierarchyChanged(final HierarchyEvent e) {
          isShowing = GLJPanel.this.isShowing();
      }
  };

  private final AWTWindowClosingProtocol awtWindowClosingProtocol =
          new AWTWindowClosingProtocol(this, new Runnable() {
                @Override
                public void run() {
                    GLJPanel.this.destroy();
                }
            }, null);

  /** Creates a new GLJPanel component with a default set of OpenGL
      capabilities and using the default OpenGL capabilities selection
      mechanism.
      <p>
      See details about <a href="#contextSharing">OpenGL context sharing</a>.
      </p>
   * @throws GLException if no default profile is available for the default desktop device.
   */
  public GLJPanel() throws GLException {
    this(null);
  }

  /** Creates a new GLJPanel component with the requested set of
      OpenGL capabilities, using the default OpenGL capabilities
      selection mechanism.
      <p>
      See details about <a href="#contextSharing">OpenGL context sharing</a>.
      </p>
   * @throws GLException if no GLCapabilities are given and no default profile is available for the default desktop device.
   */
  public GLJPanel(final GLCapabilitiesImmutable userCapsRequest) throws GLException {
    this(userCapsRequest, null);
  }

  /** Creates a new GLJPanel component. The passed GLCapabilities
      specifies the OpenGL capabilities for the component; if null, a
      default set of capabilities is used. The GLCapabilitiesChooser
      specifies the algorithm for selecting one of the available
      GLCapabilities for the component; a DefaultGLCapabilitesChooser
      is used if null is passed for this argument.
      <p>
      See details about <a href="#contextSharing">OpenGL context sharing</a>.
      </p>
    * @throws GLException if no GLCapabilities are given and no default profile is available for the default desktop device.
  */
  public GLJPanel(final GLCapabilitiesImmutable userCapsRequest, final GLCapabilitiesChooser chooser)
          throws GLException
  {
    super();

    // Works around problems on many vendors' cards; we don't need a
    // back buffer for the offscreen surface anyway
    {
        GLCapabilities caps;
        if (userCapsRequest != null) {
            caps = (GLCapabilities) userCapsRequest.cloneMutable();
        } else {
            caps = new GLCapabilities(GLProfile.getDefault(GLProfile.getDefaultDevice()));
        }
        caps.setDoubleBuffered(false);
        reqOffscreenCaps = caps;
    }
    this.factory = GLDrawableFactoryImpl.getFactoryImpl( reqOffscreenCaps.getGLProfile() ); // pre-fetch, reqOffscreenCaps may changed
    this.chooser = chooser;

    helper = new GLDrawableHelper();
    autoSwapBufferMode = helper.getAutoSwapBufferMode();

    this.setFocusable(true); // allow keyboard input!
    this.addHierarchyListener(hierarchyListener);
    this.isShowing = isShowing();
  }

  /**
   * Attempts to initialize the backend, if not initialized yet.
   * <p>
   * If backend is already initialized method returns <code>true</code>.
   * </p>
   * <p>
   * If <code>offthread</code> is <code>true</code>, initialization will kicked off
   * on a <i>short lived</i> arbitrary thread and method returns immediately.<br/>
   * If platform supports such <i>arbitrary thread</i> initialization method returns
   * <code>true</code>, otherwise <code>false</code>.
   * </p>
   * <p>
   * If <code>offthread</code> is <code>false</code>, initialization be performed
   * on the current thread and method returns after initialization.<br/>
   * Method returns <code>true</code> if initialization was successful, otherwise <code>false</code>.
   * <p>
   * @param offthread
   */
  public final boolean initializeBackend(final boolean offthread) {
    if( offthread ) {
        new InterruptSource.Thread(null, null, getThreadName()+"-GLJPanel_Init") {
            public void run() {
              if( !isInitialized ) {
                  initializeBackendImpl();
              }
            } }.start();
        return true;
    } else {
        if( !isInitialized ) {
            return initializeBackendImpl();
        } else {
            return true;
        }
    }
  }

  @Override
  public final void setSharedContext(final GLContext sharedContext) throws IllegalStateException {
      helper.setSharedContext(this.getContext(), sharedContext);
  }

  @Override
  public final void setSharedAutoDrawable(final GLAutoDrawable sharedAutoDrawable) throws IllegalStateException {
      helper.setSharedAutoDrawable(this, sharedAutoDrawable);
  }

  public AWTGLPixelBufferProvider getCustomPixelBufferProvider() { return customPixelBufferProvider; }

  /**
   * @param custom custom {@link AWTGLPixelBufferProvider}
   * @throws IllegalArgumentException if <code>custom</code> is <code>null</code>
   * @throws IllegalStateException if backend is already realized, i.e. this instanced already painted once.
   */
  public void setPixelBufferProvider(final AWTGLPixelBufferProvider custom) throws IllegalArgumentException, IllegalStateException {
      if( null == custom ) {
          throw new IllegalArgumentException("Null PixelBufferProvider");
      }
      if( null != backend ) {
          throw new IllegalStateException("Backend already realized.");
      }
      customPixelBufferProvider = custom;
  }

  @Override
  public final Object getUpstreamWidget() {
    return this;
  }

  @Override
  public final RecursiveLock getUpstreamLock() { return lock; }

  @Override
  public final boolean isThreadGLCapable() { return EventQueue.isDispatchThread(); }

  @Override
  public void display() {
      if( isShowing || ( printActive && isVisible() ) ) {
          if (EventQueue.isDispatchThread()) {
              // Want display() to be synchronous, so call paintImmediately()
              paintImmediatelyAction.run();
          } else {
              // Multithreaded redrawing of Swing components is not allowed,
              // so do everything on the event dispatch thread
              try {
                  EventQueue.invokeAndWait(paintImmediatelyAction);
              } catch (final Exception e) {
                  throw new GLException(e);
              }
          }
      }
  }

  protected void dispose(final Runnable post) {
    if(DEBUG) {
        System.err.println(getThreadName()+": GLJPanel.dispose() - start");
        // Thread.dumpStack();
    }

    if (backend != null && backend.getContext() != null) {
      final boolean animatorPaused;
      final GLAnimatorControl animator =  getAnimator();
      if(null!=animator) {
        animatorPaused = animator.pause();
      } else {
        animatorPaused = false;
      }

      if(backend.getContext().isCreated()) {
          Threading.invoke(true, disposeAction, getTreeLock());
      }
      if(null != backend) {
          // not yet destroyed due to backend.isUsingOwnThreadManagment() == true
          backend.destroy();
          isInitialized = false;
      }
      if( null != post ) {
          post.run();
      }

      if( animatorPaused ) {
        animator.resume();
      }
    }

    if(DEBUG) {
        System.err.println(getThreadName()+": GLJPanel.dispose() - stop");
    }
  }

  /**
   * Just an alias for removeNotify
   */
  @Override
  public void destroy() {
      removeNotify();
  }

  /** Overridden to cause OpenGL rendering to be performed during
      repaint cycles. Subclasses which override this method must call
      super.paintComponent() in their paintComponent() method in order
      to function properly. <P>

      <DL><DD><CODE>paintComponent</CODE> in class <CODE>javax.swing.JComponent</CODE></DD></DL> */
  @Override
  protected void paintComponent(final Graphics g) {
    if (Beans.isDesignTime()) {
      // Make GLJPanel behave better in NetBeans GUI builder
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, getWidth(), getHeight());
      final FontMetrics fm = g.getFontMetrics();
      String name = getName();
      if (name == null) {
        name = getClass().getName();
        final int idx = name.lastIndexOf('.');
        if (idx >= 0) {
          name = name.substring(idx + 1);
        }
      }
      final Rectangle2D bounds = fm.getStringBounds(name, g);
      g.setColor(Color.WHITE);
      g.drawString(name,
                   (int) ((getWidth()  - bounds.getWidth())  / 2),
                   (int) ((getHeight() + bounds.getHeight()) / 2));
      return;
    }

    final RecursiveLock _lock = lock;
    _lock.lock();
    try {
        if( !isInitialized ) {
            initializeBackendImpl();
        }

        if (!isInitialized || printActive) {
            return;
        }

        // NOTE: must do this when the context is not current as it may
        // involve destroying the pbuffer (current context) and
        // re-creating it -- tricky to do properly while the context is
        // current
        if( !printActive ) {
            updatePixelScale(backend);
            if ( handleReshape ) {
                handleReshape = false;
                sendReshape = handleReshape();
            }

            if( isShowing ) {
                updater.setGraphics(g);
                backend.doPaintComponent(g);
            }
        }
    } finally {
        _lock.unlock();
    }
  }

  private final void updateWrappedSurfaceScale(final GLDrawable d) {
      final NativeSurface s = d.getNativeSurface();
      if( s instanceof WrappedSurface ) {
          ((WrappedSurface)s).setSurfaceScale(hasPixelScale);
      }
  }

  @Override
  public final boolean setSurfaceScale(final float[] pixelScale) { // HiDPI support
      System.arraycopy(pixelScale, 0, reqPixelScale, 0, 2);
      final Backend b = backend;
      if ( isInitialized && null != b && isShowing ) {
          if( isShowing || ( printActive && isVisible() ) ) {
              if (EventQueue.isDispatchThread()) {
                  setSurfaceScaleAction.run();
              } else {
                  try {
                      EventQueue.invokeAndWait(setSurfaceScaleAction);
                  } catch (final Exception e) {
                      throw new GLException(e);
                  }
              }
          }
          return true;
      } else {
          return false;
      }
  }
  private final Runnable setSurfaceScaleAction = new Runnable() {
    @Override
    public void run() {
        final Backend b = backend;
        if( null != b && setSurfaceScaleImpl(b) ) {
            if( !helper.isAnimatorAnimatingOnOtherThread() ) {
                paintImmediatelyAction.run(); // display
            }
        }
    }
  };

  private final boolean setSurfaceScaleImpl(final Backend b) {
      if( SurfaceScaleUtils.setNewPixelScale(hasPixelScale, hasPixelScale, reqPixelScale, minPixelScale, maxPixelScale, DEBUG ? getClass().getSimpleName() : null) ) {
          reshapeImpl(getWidth(), getHeight());
          updateWrappedSurfaceScale(b.getDrawable());
          return true;
      }
      return false;
  }

  private final boolean updatePixelScale(final Backend b) {
      if( JAWTUtil.getPixelScale(getGraphicsConfiguration(), minPixelScale, maxPixelScale) ) {
          return setSurfaceScaleImpl(b);
      } else {
          return false;
      }
  }

  @Override
  public final float[] getRequestedSurfaceScale(final float[] result) {
      System.arraycopy(reqPixelScale, 0, result, 0, 2);
      return result;
  }

  @Override
  public final float[] getCurrentSurfaceScale(final float[] result) {
      System.arraycopy(hasPixelScale, 0, result, 0, 2);
      return result;
  }

  @Override
  public float[] getMinimumSurfaceScale(final float[] result) {
      System.arraycopy(minPixelScale, 0, result, 0, 2);
      return result;
  }

  @Override
  public float[] getMaximumSurfaceScale(final float[] result) {
      System.arraycopy(maxPixelScale, 0, result, 0, 2);
      return result;
  }

  /** Overridden to track when this component is added to a container.
      Subclasses which override this method must call
      super.addNotify() in their addNotify() method in order to
      function properly. <P>

      <DL><DD><CODE>addNotify</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
  @Override
  public void addNotify() {
    super.addNotify();
    awtWindowClosingProtocol.addClosingListener();

    // HiDPI support
    JAWTUtil.getPixelScale(getGraphicsConfiguration(), minPixelScale, maxPixelScale);
    SurfaceScaleUtils.setNewPixelScale(hasPixelScale, hasPixelScale, reqPixelScale, minPixelScale, maxPixelScale, DEBUG ? getClass().getSimpleName() : null);

    if (DEBUG) {
        System.err.println(getThreadName()+": GLJPanel.addNotify()");
    }
  }

  /** Overridden to track when this component is removed from a
      container. Subclasses which override this method must call
      super.removeNotify() in their removeNotify() method in order to
      function properly. <P>

      <DL><DD><CODE>removeNotify</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
  @Override
  public void removeNotify() {
    awtWindowClosingProtocol.removeClosingListener();

    dispose(null);
    hasPixelScale[0] = ScalableSurface.IDENTITY_PIXELSCALE;
    hasPixelScale[1] = ScalableSurface.IDENTITY_PIXELSCALE;
    minPixelScale[0] = ScalableSurface.IDENTITY_PIXELSCALE;
    minPixelScale[1] = ScalableSurface.IDENTITY_PIXELSCALE;
    maxPixelScale[0] = ScalableSurface.IDENTITY_PIXELSCALE;
    maxPixelScale[1] = ScalableSurface.IDENTITY_PIXELSCALE;

    super.removeNotify();
  }

  /** Overridden to cause {@link GLDrawableHelper#reshape} to be
      called on all registered {@link GLEventListener}s. Subclasses
      which override this method must call super.reshape() in
      their reshape() method in order to function properly. <P>
   *
   * {@inheritDoc}
   */
  @SuppressWarnings("deprecation")
  @Override
  public void reshape(final int x, final int y, final int width, final int height) {
    super.reshape(x, y, width, height);
    reshapeImpl(width, height);
  }

  private void reshapeImpl(final int width, final int height) {
    final int scaledWidth = SurfaceScaleUtils.scale(width, hasPixelScale[0]);
    final int scaledHeight = SurfaceScaleUtils.scale(height, hasPixelScale[1]);
    if( !printActive && ( handleReshape || scaledWidth != panelWidth || scaledHeight != panelHeight ) ) {
        reshapeWidth = scaledWidth;
        reshapeHeight = scaledHeight;
        handleReshape = true;
    }
    if( DEBUG ) {
        System.err.println(getThreadName()+": GLJPanel.reshape.0 "+this.getName()+" resize ["+(printActive?"printing":"paint")+
                "] [ this "+getWidth()+"x"+getHeight()+", pixelScale "+getPixelScaleStr()+
                ", panel "+panelWidth+"x"+panelHeight +
                "] -> "+(handleReshape?"":"[skipped] ") + width+"x"+height+" * "+getPixelScaleStr()+
                " -> "+scaledWidth+"x"+scaledHeight+", reshapeSize "+reshapeWidth+"x"+reshapeHeight);
    }
  }

  private volatile boolean printActive = false;
  private GLAnimatorControl printAnimator = null;
  private GLAutoDrawable printGLAD = null;
  private AWTTilePainter printAWTTiles = null;

  @Override
  public void setupPrint(final double scaleMatX, final double scaleMatY, final int numSamples, final int tileWidth, final int tileHeight) {
      printActive = true;
      if( DEBUG ) {
          System.err.printf(getThreadName()+": GLJPanel.setupPrint: scale %f / %f, samples %d, tileSz %d x %d%n", scaleMatX, scaleMatY, numSamples, tileWidth, tileHeight);
      }
      final int componentCount = isOpaque() ? 3 : 4;
      final TileRenderer printRenderer = new TileRenderer();
      printAWTTiles = new AWTTilePainter(printRenderer, componentCount, scaleMatX, scaleMatY, numSamples, tileWidth, tileHeight, DEBUG);
      AWTEDTExecutor.singleton.invoke(getTreeLock(), true /* allowOnNonEDT */, true /* wait */, setupPrintOnEDT);
  }
  private final Runnable setupPrintOnEDT = new Runnable() {
      @Override
      public void run() {
          final RecursiveLock _lock = lock;
          _lock.lock();
          try {
              if( !isInitialized ) {
                  initializeBackendImpl();
              }
              if (!isInitialized) {
                  if(DEBUG) {
                      System.err.println(getThreadName()+": Info: GLJPanel setupPrint - skipped GL render, drawable not valid yet");
                  }
                  printActive = false;
                  return; // not yet available ..
              }
              if( !isVisible() ) {
                  if(DEBUG) {
                      System.err.println(getThreadName()+": Info: GLJPanel setupPrint - skipped GL render, panel not visible");
                  }
                  printActive = false;
                  return; // not yet available ..
              }
              sendReshape = false; // clear reshape flag
              handleReshape = false; // ditto
              printAnimator =  helper.getAnimator();
              if( null != printAnimator ) {
                  printAnimator.remove(GLJPanel.this);
              }

              printGLAD = GLJPanel.this; // default: re-use
              final GLCapabilitiesImmutable gladCaps = getChosenGLCapabilities();
              final int printNumSamples = printAWTTiles.getNumSamples(gladCaps);
              GLDrawable printDrawable = printGLAD.getDelegatedDrawable();
              final boolean reqNewGLADSamples = printNumSamples != gladCaps.getNumSamples();
              final boolean reqNewGLADSize = printAWTTiles.customTileWidth != -1 && printAWTTiles.customTileWidth != printDrawable.getSurfaceWidth() ||
                                             printAWTTiles.customTileHeight != -1 && printAWTTiles.customTileHeight != printDrawable.getSurfaceHeight();

              final GLCapabilities newGLADCaps = (GLCapabilities)gladCaps.cloneMutable();
              newGLADCaps.setDoubleBuffered(false);
              newGLADCaps.setOnscreen(false);
              if( printNumSamples != newGLADCaps.getNumSamples() ) {
                  newGLADCaps.setSampleBuffers(0 < printNumSamples);
                  newGLADCaps.setNumSamples(printNumSamples);
              }
              final boolean reqNewGLADSafe = GLDrawableUtil.isSwapGLContextSafe(getRequestedGLCapabilities(), gladCaps, newGLADCaps);

              final boolean reqNewGLAD = ( reqNewGLADSamples || reqNewGLADSize ) && reqNewGLADSafe;

              if( DEBUG ) {
                  System.err.println("AWT print.setup: reqNewGLAD "+reqNewGLAD+"[ samples "+reqNewGLADSamples+", size "+reqNewGLADSize+", safe "+reqNewGLADSafe+"], "+
                                     ", drawableSize "+printDrawable.getSurfaceWidth()+"x"+printDrawable.getSurfaceHeight()+
                                     ", customTileSize "+printAWTTiles.customTileWidth+"x"+printAWTTiles.customTileHeight+
                                     ", scaleMat "+printAWTTiles.scaleMatX+" x "+printAWTTiles.scaleMatY+
                                     ", numSamples "+printAWTTiles.customNumSamples+" -> "+printNumSamples+", printAnimator "+printAnimator);
              }
              if( reqNewGLAD ) {
                  final GLDrawableFactory factory = GLDrawableFactory.getFactory(newGLADCaps.getGLProfile());
                  GLOffscreenAutoDrawable offGLAD = null;
                  try {
                      offGLAD = factory.createOffscreenAutoDrawable(null, newGLADCaps, null,
                              printAWTTiles.customTileWidth != -1 ? printAWTTiles.customTileWidth : DEFAULT_PRINT_TILE_SIZE,
                              printAWTTiles.customTileHeight != -1 ? printAWTTiles.customTileHeight : DEFAULT_PRINT_TILE_SIZE);
                  } catch (final GLException gle) {
                      if( DEBUG ) {
                          System.err.println("Caught: "+gle.getMessage());
                          gle.printStackTrace();
                      }
                  }
                  if( null != offGLAD ) {
                      printGLAD = offGLAD;
                      GLDrawableUtil.swapGLContextAndAllGLEventListener(GLJPanel.this, printGLAD);
                      printDrawable = printGLAD.getDelegatedDrawable();
                  }
              }
              printAWTTiles.setGLOrientation( !GLJPanel.this.skipGLOrientationVerticalFlip && printGLAD.isGLOriented(), printGLAD.isGLOriented() );
              printAWTTiles.renderer.setTileSize(printDrawable.getSurfaceWidth(), printDrawable.getSurfaceHeight(), 0);
              printAWTTiles.renderer.attachAutoDrawable(printGLAD);
              if( DEBUG ) {
                  System.err.println("AWT print.setup "+printAWTTiles);
                  System.err.println("AWT print.setup AA "+printNumSamples+", "+newGLADCaps);
                  System.err.println("AWT print.setup printGLAD: "+printGLAD.getSurfaceWidth()+"x"+printGLAD.getSurfaceHeight()+", "+printGLAD);
                  System.err.println("AWT print.setup printDraw: "+printDrawable.getSurfaceWidth()+"x"+printDrawable.getSurfaceHeight()+", "+printDrawable);
              }
          } finally {
              _lock.unlock();
          }
      }
  };

  @Override
  public void releasePrint() {
      if( !printActive ) {
          throw new IllegalStateException("setupPrint() not called");
      }
      sendReshape = false; // clear reshape flag
      handleReshape = false; // ditto
      AWTEDTExecutor.singleton.invoke(getTreeLock(), true /* allowOnNonEDT */, true /* wait */, releasePrintOnEDT);
  }

  private final Runnable releasePrintOnEDT = new Runnable() {
      @Override
      public void run() {
          final RecursiveLock _lock = lock;
          _lock.lock();
          try {
              if( DEBUG ) {
                  System.err.println(getThreadName()+": GLJPanel.releasePrintOnEDT.0 "+printAWTTiles);
              }
              printAWTTiles.dispose();
              printAWTTiles= null;
              if( printGLAD != GLJPanel.this ) {
                  GLDrawableUtil.swapGLContextAndAllGLEventListener(printGLAD, GLJPanel.this);
                  printGLAD.destroy();
              }
              printGLAD = null;
              if( null != printAnimator ) {
                  printAnimator.add(GLJPanel.this);
                  printAnimator = null;
              }

              // trigger reshape, i.e. gl-viewport and -listener - this component might got resized!
              final int awtWidth = GLJPanel.this.getWidth();
              final int awtHeight= GLJPanel.this.getHeight();
              final int scaledAWTWidth = SurfaceScaleUtils.scale(awtWidth, hasPixelScale[0]);
              final int scaledAWTHeight= SurfaceScaleUtils.scale(awtHeight, hasPixelScale[1]);
              final GLDrawable drawable = GLJPanel.this.getDelegatedDrawable();
              if( scaledAWTWidth != panelWidth || scaledAWTHeight != panelHeight ||
                  drawable.getSurfaceWidth() != panelWidth || drawable.getSurfaceHeight() != panelHeight ) {
                  // -> !( awtSize == panelSize == drawableSize )
                  if ( DEBUG ) {
                      System.err.println(getThreadName()+": GLJPanel.releasePrintOnEDT.0: resize [printing] panel " +panelWidth+"x"+panelHeight + " @ scale "+getPixelScaleStr()+
                              ", draw "+drawable.getSurfaceWidth()+"x"+drawable.getSurfaceHeight()+
                              " -> " + awtWidth+"x"+awtHeight+" * "+getPixelScaleStr()+" -> "+scaledAWTWidth+"x"+scaledAWTHeight);
                  }
                  reshapeWidth = scaledAWTWidth;
                  reshapeHeight = scaledAWTHeight;
                  sendReshape = handleReshape(); // reshapeSize -> panelSize, backend reshape w/ GL reshape
              } else {
                  sendReshape = true; // only GL reshape
              }
              printActive = false;
              display();
          } finally {
              _lock.unlock();
          }
      }
  };

  @Override
  public void print(final Graphics graphics) {
      if( !printActive ) {
          throw new IllegalStateException("setupPrint() not called");
      }
      if(DEBUG && !EventQueue.isDispatchThread()) {
          System.err.println(getThreadName()+": Warning: GLCanvas print - not called from AWT-EDT");
          // we cannot dispatch print on AWT-EDT due to printing internal locking ..
      }
      sendReshape = false; // clear reshape flag
      handleReshape = false; // ditto

      final Graphics2D g2d = (Graphics2D)graphics;
      try {
          printAWTTiles.setupGraphics2DAndClipBounds(g2d, getWidth(), getHeight());
          final TileRenderer tileRenderer = printAWTTiles.renderer;
          if( DEBUG ) {
              System.err.println("AWT print.0: "+tileRenderer);
          }
          if( !tileRenderer.eot() ) {
              try {
                  do {
                      if( printGLAD != GLJPanel.this ) {
                          tileRenderer.display();
                      } else {
                          backend.doPlainPaint();
                      }
                  } while ( !tileRenderer.eot() );
                  if( DEBUG ) {
                      System.err.println("AWT print.1: "+printAWTTiles);
                  }
              } finally {
                  tileRenderer.reset();
                  printAWTTiles.resetGraphics2D();
              }
          }
      } catch (final NoninvertibleTransformException nte) {
          System.err.println("Caught: Inversion failed of: "+g2d.getTransform());
          nte.printStackTrace();
      }
      if( DEBUG ) {
          System.err.println("AWT print.X: "+printAWTTiles);
      }
  }
  @Override
  protected void printComponent(final Graphics g) {
      if( DEBUG ) {
          System.err.println("AWT printComponent.X: "+printAWTTiles);
      }
      print(g);
  }

  @Override
  public void setOpaque(final boolean opaque) {
    if (backend != null) {
      backend.setOpaque(opaque);
    }
    super.setOpaque(opaque);
  }

  @Override
  public void addGLEventListener(final GLEventListener listener) {
    helper.addGLEventListener(listener);
  }

  @Override
  public void addGLEventListener(final int index, final GLEventListener listener) {
    helper.addGLEventListener(index, listener);
  }

  @Override
  public int getGLEventListenerCount() {
      return helper.getGLEventListenerCount();
  }

  @Override
  public GLEventListener getGLEventListener(final int index) throws IndexOutOfBoundsException {
      return helper.getGLEventListener(index);
  }

  @Override
  public boolean areAllGLEventListenerInitialized() {
     return helper.areAllGLEventListenerInitialized();
  }

  @Override
  public boolean getGLEventListenerInitState(final GLEventListener listener) {
      return helper.getGLEventListenerInitState(listener);
  }

  @Override
  public void setGLEventListenerInitState(final GLEventListener listener, final boolean initialized) {
      helper.setGLEventListenerInitState(listener, initialized);
  }

  @Override
  public GLEventListener disposeGLEventListener(final GLEventListener listener, final boolean remove) {
    final DisposeGLEventListenerAction r = new DisposeGLEventListenerAction(listener, remove);
    if (EventQueue.isDispatchThread()) {
      r.run();
    } else {
      // Multithreaded redrawing of Swing components is not allowed,
      // so do everything on the event dispatch thread
      try {
        EventQueue.invokeAndWait(r);
      } catch (final Exception e) {
        throw new GLException(e);
      }
    }
    return r.listener;
  }

  @Override
  public GLEventListener removeGLEventListener(final GLEventListener listener) {
    return helper.removeGLEventListener(listener);
  }

  @Override
  public void setAnimator(final GLAnimatorControl animatorControl) {
    helper.setAnimator(animatorControl);
  }

  @Override
  public GLAnimatorControl getAnimator() {
    return helper.getAnimator();
  }

  @Override
  public final Thread setExclusiveContextThread(final Thread t) throws GLException {
      return helper.setExclusiveContextThread(t, getContext());
  }

  @Override
  public final Thread getExclusiveContextThread() {
      return helper.getExclusiveContextThread();
  }

  @Override
  public boolean invoke(final boolean wait, final GLRunnable glRunnable) throws IllegalStateException {
    return helper.invoke(this, wait, glRunnable);
  }

  @Override
  public boolean invoke(final boolean wait, final List<GLRunnable> glRunnables) throws IllegalStateException {
    return helper.invoke(this, wait, glRunnables);
  }

  @Override
  public void flushGLRunnables() {
      helper.flushGLRunnables();
  }

  @Override
  public GLContext createContext(final GLContext shareWith) {
    final RecursiveLock _lock = lock;
    _lock.lock();
    try {
        final Backend b = backend;
        if ( null == b ) {
            return null;
        }
        return b.createContext(shareWith);
    } finally {
        _lock.unlock();
    }
  }

  @Override
  public void setRealized(final boolean realized) {
  }

  @Override
  public boolean isRealized() {
      return isInitialized;
  }

  @Override
  public GLContext setContext(final GLContext newCtx, final boolean destroyPrevCtx) {
    final RecursiveLock _lock = lock;
    _lock.lock();
    try {
        final Backend b = backend;
        if ( null == b ) {
            return null;
        }
        final GLContext oldCtx = b.getContext();
        GLDrawableHelper.switchContext(b.getDrawable(), oldCtx, destroyPrevCtx, newCtx, additionalCtxCreationFlags);
        b.setContext(newCtx);
        return oldCtx;
    } finally {
        _lock.unlock();
    }
  }


  @Override
  public final GLDrawable getDelegatedDrawable() {
    final Backend b = backend;
    if ( null == b ) {
        return null;
    }
    return b.getDrawable();
  }

  @Override
  public GLContext getContext() {
    final Backend b = backend;
    if ( null == b ) {
        return null;
    }
    return b.getContext();
  }

  @Override
  public GL getGL() {
    if (Beans.isDesignTime()) {
      return null;
    }
    final GLContext context = getContext();
    return (context == null) ? null : context.getGL();
  }

  @Override
  public GL setGL(final GL gl) {
    final GLContext context = getContext();
    if (context != null) {
      context.setGL(gl);
      return gl;
    }
    return null;
  }

  @Override
  public void setAutoSwapBufferMode(final boolean enable) {
    this.autoSwapBufferMode = enable;
    boolean backendHandlesSwapBuffer = false;
    if( isInitialized ) {
        final Backend b = backend;
        if ( null != b ) {
            backendHandlesSwapBuffer= b.handlesSwapBuffer();
        }
    }
    if( !backendHandlesSwapBuffer ) {
        helper.setAutoSwapBufferMode(enable);
    }
  }

  @Override
  public boolean getAutoSwapBufferMode() {
    return autoSwapBufferMode;
  }

  @Override
  public void swapBuffers() {
    if( isInitialized ) {
        final Backend b = backend;
        if ( null != b ) {
            b.swapBuffers();
        }
    }
  }

  @Override
  public void setContextCreationFlags(final int flags) {
    additionalCtxCreationFlags = flags;
  }

  @Override
  public int getContextCreationFlags() {
    return additionalCtxCreationFlags;
  }

  /** For a translucent GLJPanel (one for which {@link #setOpaque
      setOpaque}(false) has been called), indicates whether the
      application should preserve the OpenGL color buffer
      (GL_COLOR_BUFFER_BIT) for correct rendering of the GLJPanel and
      underlying widgets which may show through portions of the
      GLJPanel with alpha values less than 1.  Most Swing
      implementations currently expect the GLJPanel to be completely
      cleared (e.g., by <code>glClear(GL_COLOR_BUFFER_BIT |
      GL_DEPTH_BUFFER_BIT)</code>), but for certain optimized Swing
      implementations which use OpenGL internally, it may be possible
      to perform OpenGL rendering using the GLJPanel into the same
      OpenGL drawable as the Swing implementation uses. */
  public boolean shouldPreserveColorBufferIfTranslucent() {
    return oglPipelineUsable();
  }

  @Override
  public int getSurfaceWidth() {
      return panelWidth; // scaled surface width in pixel units, current as-from reshape
  }

  @Override
  public int getSurfaceHeight() {
      return panelHeight; // scaled surface height in pixel units, current as-from reshape
  }

  /**
   * {@inheritDoc}
   * <p>
   * Method returns a valid value only <i>after</i>
   * the backend has been initialized, either {@link #initializeBackend(boolean) eagerly}
   * or manually via the first display call.<br/>
   * Method always returns a valid value when called from within a {@link GLEventListener}.
   * </p>
   */
  @Override
  public boolean isGLOriented() {
    final Backend b = backend;
    if ( null == b ) {
        return true;
    }
    return b.getDrawable().isGLOriented();
  }

  /**
   * Skip {@link #isGLOriented()} based vertical flip,
   * which usually is required by the offscreen backend,
   * see details about <a href="#verticalFlip">vertical flip</a>
   * and <a href="#fboGLSLVerticalFlip">FBO / GLSL vertical flip</a>.
   * <p>
   * If set to <code>true</code>, user needs to flip the OpenGL rendered scene
   * <i>if {@link #isGLOriented()} == true</i>, e.g. via the projection matrix.<br/>
   * See constraints of {@link #isGLOriented()}.
   * </p>
   */
  public final void setSkipGLOrientationVerticalFlip(final boolean v) {
      skipGLOrientationVerticalFlip = v;
  }
  /** See {@link #setSkipGLOrientationVerticalFlip(boolean)}. */
  public final boolean getSkipGLOrientationVerticalFlip() {
      return skipGLOrientationVerticalFlip;
  }

  @Override
  public GLCapabilitiesImmutable getChosenGLCapabilities() {
    final Backend b = backend;
    if ( null == b ) {
        return null;
    }
    return b.getChosenGLCapabilities();
  }

  @Override
  public final GLCapabilitiesImmutable getRequestedGLCapabilities() {
    return reqOffscreenCaps;
  }

  /**
   * Set a new requested {@link GLCapabilitiesImmutable} for this GLJPanel
   * allowing reconfiguration.
   * <p>
   * Method shall be invoked from the {@link #isThreadGLCapable() AWT-EDT thread}.
   * In case it is not invoked on the AWT-EDT thread, an attempt is made to do so.
   * </p>
   * <p>
   * Method will dispose a previous {@link #isRealized() realized} GLContext and offscreen backend!
   * </p>
   * @param caps new capabilities.
   */
  public final void setRequestedGLCapabilities(final GLCapabilitiesImmutable caps) {
    if( null == caps ) {
        throw new IllegalArgumentException("null caps");
    }
    Threading.invoke(true,
        new Runnable() {
            @Override
            public void run() {
                dispose( new Runnable() {
                    @Override
                    public void run() {
                        // switch to new caps and re-init backend
                        // after actual dispose, but before resume animator
                        reqOffscreenCaps = caps;
                        initializeBackendImpl();
                    } } );
            }
        }, getTreeLock());
  }

  @Override
  public final GLProfile getGLProfile() {
    return reqOffscreenCaps.getGLProfile();
  }

  @Override
  public NativeSurface getNativeSurface() {
    final Backend b = backend;
    if ( null == b ) {
        return null;
    }
    return b.getDrawable().getNativeSurface();
  }

  @Override
  public long getHandle() {
    final Backend b = backend;
    if ( null == b ) {
        return 0;
    }
    return b.getDrawable().getNativeSurface().getSurfaceHandle();
  }

  @Override
  public final GLDrawableFactory getFactory() {
    return factory;
  }

  /**
   * Returns the used texture unit, i.e. a value of [0..n], or -1 if non used.
   * <p>
   * If implementation uses a texture-unit, it will be known only after the first initialization, i.e. display call.
   * </p>
   * <p>
   * See <a href="#fboGLSLVerticalFlip">FBO / GLSL Vertical Flip</a>.
   * </p>
   */
  public final int getTextureUnit() {
    final Backend b = backend;
    if ( null == b ) {
        return -1;
    }
    return b.getTextureUnit();
  }

  /**
   * Allows user to request a texture unit to be used,
   * must be called before the first initialization, i.e. {@link #display()} call.
   * <p>
   * Defaults to <code>0</code>.
   * </p>
   * <p>
   * See <a href="#fboGLSLVerticalFlip">FBO / GLSL Vertical Flip</a>.
   * </p>
   *
   * @param v requested texture unit
   * @see #getTextureUnit()
   */
  public final void setTextureUnit(final int v) {
      requestedTextureUnit = v;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private final Object initSync = new Object();
  private boolean initializeBackendImpl() {
    synchronized(initSync) {
        if( !isInitialized ) {
            if( handleReshape ) {
              if (DEBUG) {
                  System.err.println(getThreadName()+": GLJPanel.createAndInitializeBackend.1: ["+(printActive?"printing":"paint")+"] "+
                          panelWidth+"x"+panelHeight+" @ scale "+getPixelScaleStr() + " -> " +
                          reshapeWidth+"x"+reshapeHeight+" @ scale "+getPixelScaleStr());
              }
              panelWidth = reshapeWidth;
              panelHeight = reshapeHeight;
              handleReshape = false;
            } else {
              if (DEBUG) {
                  System.err.println(getThreadName()+": GLJPanel.createAndInitializeBackend.0: ["+(printActive?"printing":"paint")+"] "+
                          panelWidth+"x"+panelHeight+" @ scale "+getPixelScaleStr());
              }
            }

            if ( 0 >= panelWidth || 0 >= panelHeight ) {
              return false;
            }

            if ( null == backend ) {
                if ( oglPipelineUsable() ) {
                    backend = new J2DOGLBackend();
                } else {
                    backend = new OffscreenBackend(customPixelBufferProvider);
                }
                isInitialized = false;
            }

            if (!isInitialized) {
                this.factory = GLDrawableFactoryImpl.getFactoryImpl( reqOffscreenCaps.getGLProfile() ); // reqOffscreenCaps may have changed
                backend.initialize();
            }
            return isInitialized;
        } else {
            return true;
        }
    }
  }

  private final String getPixelScaleStr() { return "["+hasPixelScale[0]+", "+hasPixelScale[1]+"]"; }

  @Override
  public WindowClosingMode getDefaultCloseOperation() {
      return awtWindowClosingProtocol.getDefaultCloseOperation();
  }

  @Override
  public WindowClosingMode setDefaultCloseOperation(final WindowClosingMode op) {
      return awtWindowClosingProtocol.setDefaultCloseOperation(op);
  }

  private boolean handleReshape() {
    if (DEBUG) {
      System.err.println(getThreadName()+": GLJPanel.handleReshape: "+
                         panelWidth+"x"+panelHeight+" @ scale "+getPixelScaleStr() + " -> " +
                         reshapeWidth+"x"+reshapeHeight+" @ scale "+getPixelScaleStr());
    }
    panelWidth  = reshapeWidth;
    panelHeight = reshapeHeight;

    return backend.handleReshape();
  }

  // This is used as the GLEventListener for the pbuffer-based backend
  // as well as the callback mechanism for the other backends
  class Updater implements GLEventListener {
    private Graphics g;

    public void setGraphics(final Graphics g) {
      this.g = g;
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
      if (!backend.preGL(g)) {
        return;
      }
      helper.init(GLJPanel.this, !sendReshape);
      backend.postGL(g, false);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
      helper.disposeAllGLEventListener(GLJPanel.this, false);
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
      if (!backend.preGL(g)) {
        return;
      }
      if (sendReshape) {
        if (DEBUG) {
          System.err.println(getThreadName()+": GLJPanel.display: reshape(" + viewportX + "," + viewportY + " " + panelWidth + "x" + panelHeight + " @ scale "+getPixelScaleStr()+")");
        }
        helper.reshape(GLJPanel.this, viewportX, viewportY, panelWidth, panelHeight);
        sendReshape = false;
      }

      helper.display(GLJPanel.this);
      backend.postGL(g, true);
    }

    public void plainPaint(final GLAutoDrawable drawable) {
      helper.display(GLJPanel.this);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
      // This is handled above and dispatched directly to the appropriate context
    }
  }

  @Override
  public String toString() {
    final GLDrawable d = ( null != backend ) ? backend.getDrawable() : null;
    return "AWT-GLJPanel[ drawableType "+ ( ( null != d ) ? d.getClass().getName() : "null" ) +
           ", chosenCaps " + getChosenGLCapabilities() +
           "]";
  }

  private final Runnable disposeAction = new Runnable() {
    @Override
    public void run() {
        final RecursiveLock _lock = lock;
        _lock.lock();
        try {
            if ( null != backend ) {
                final GLContext _context = backend.getContext();
                final boolean backendDestroy = !backend.isUsingOwnLifecycle();

                GLException exceptionOnDisposeGL = null;
                if( null != _context && _context.isCreated() ) {
                    try {
                        helper.disposeGL(GLJPanel.this, _context, !backendDestroy);
                    } catch (final GLException gle) {
                        exceptionOnDisposeGL = gle;
                    }
                }
                Throwable exceptionBackendDestroy = null;
                if ( backendDestroy ) {
                    try {
                        backend.destroy();
                    } catch( final Throwable re ) {
                        exceptionBackendDestroy = re;
                    }
                    backend = null;
                    isInitialized = false;
                }

                // throw exception in order of occurrence ..
                if( null != exceptionOnDisposeGL ) {
                    throw exceptionOnDisposeGL;
                }
                if( null != exceptionBackendDestroy ) {
                    throw GLException.newGLException(exceptionBackendDestroy);
                }
            }
        } finally {
            _lock.unlock();
        }
    }
  };

  private final Runnable updaterInitAction = new Runnable() {
    @Override
    public void run() {
      updater.init(GLJPanel.this);
    }
  };

  private final Runnable updaterDisplayAction = new Runnable() {
    @Override
    public void run() {
      updater.display(GLJPanel.this);
    }
  };

  private final Runnable updaterPlainDisplayAction = new Runnable() {
    @Override
    public void run() {
      updater.plainPaint(GLJPanel.this);
    }
  };

  private final Runnable paintImmediatelyAction = new Runnable() {
    @Override
    public void run() {
      paintImmediately(0, 0, getWidth(), getHeight());
    }
  };

  private class DisposeGLEventListenerAction implements Runnable {
      GLEventListener listener;
      private final boolean remove;
      private DisposeGLEventListenerAction(final GLEventListener listener, final boolean remove) {
          this.listener = listener;
          this.remove = remove;
      }

      @Override
      public void run() {
          final Backend b = backend;
          if ( null != b ) {
              listener = helper.disposeGLEventListener(GLJPanel.this, b.getDrawable(), b.getContext(), listener, remove);
          }
      }
  };

  private int getGLInteger(final GL gl, final int which) {
    final int[] tmp = new int[1];
    gl.glGetIntegerv(which, tmp, 0);
    return tmp[0];
  }

  protected static String getThreadName() { return Thread.currentThread().getName(); }

  //----------------------------------------------------------------------
  // Implementations of the various backends
  //

  /**
   * Abstraction of the different rendering backends: i.e., pure
   * software / pixmap rendering, pbuffer-based acceleration, Java 2D
   * JOGL bridge
   */
  static interface Backend {
    /** Create, Destroy, .. */
    public boolean isUsingOwnLifecycle();

    /** Called each time the backend needs to initialize itself */
    public void initialize();

    /** Called when the backend should clean up its resources */
    public void destroy();

    /** Called when the opacity of the GLJPanel is changed */
    public void setOpaque(boolean opaque);

    /**
     * Called to manually create an additional OpenGL context against
     * this GLJPanel
     */
    public GLContext createContext(GLContext shareWith);

    /** Called to set the current backend's GLContext */
    public void setContext(GLContext ctx);

    /** Called to get the current backend's GLContext */
    public GLContext getContext();

    /** Called to get the current backend's GLDrawable */
    public GLDrawable getDrawable();

    /** Returns the used texture unit, i.e. a value of [0..n], or -1 if non used. */
    public int getTextureUnit();

    /** Called to fetch the "real" GLCapabilities for the backend */
    public GLCapabilitiesImmutable getChosenGLCapabilities();

    /** Called to fetch the "real" GLProfile for the backend */
    public GLProfile getGLProfile();

    /**
     * Called to handle a reshape event. When this is called, the
     * OpenGL context associated with the backend is not current, to
     * make it easier to destroy and re-create pbuffers if necessary.
     */
    public boolean handleReshape();

    /**
     * Called before the OpenGL work is done in init() and display().
     * If false is returned, this render is aborted.
     */
    public boolean preGL(Graphics g);

    /**
     * Return true if backend handles 'swap buffer' itself
     * and hence the helper's setAutoSwapBuffer(enable) shall not be called.
     * In this case {@link GLJPanel#autoSwapBufferMode} is being used
     * in the backend to determine whether to swap buffers or not.
     */
    public boolean handlesSwapBuffer();

    /**
     * Shall invoke underlying drawable's swapBuffer.
     */
    public void swapBuffers();

    /**
     * Called after the OpenGL work is done in init() and display().
     * The isDisplay argument indicates whether this was called on
     * behalf of a call to display() rather than init().
     */
    public void postGL(Graphics g, boolean isDisplay);

    /** Called from within paintComponent() to initiate the render */
    public void doPaintComponent(Graphics g);

    /** Called from print .. no backend update desired onscreen */
    public void doPlainPaint();
  }

  // Base class used by both the software (pixmap) and pbuffer
  // backends, both of which rely on reading back the OpenGL frame
  // buffer and drawing it with a BufferedImage
  class OffscreenBackend implements Backend {
    private final AWTGLPixelBufferProvider pixelBufferProvider;
    private final boolean useSingletonBuffer;
    private AWTGLPixelBuffer pixelBuffer;
    private BufferedImage alignedImage;

    // One of these is used to store the read back pixels before storing
    // in the BufferedImage
    protected IntBuffer readBackIntsForCPUVFlip;

    // Implementation using software rendering
    private volatile GLDrawable offscreenDrawable; // volatile: avoid locking for read-only access
    private boolean offscreenIsFBO;
    private FBObject fboFlipped;
    private GLSLTextureRaster glslTextureRaster;

    private volatile GLContextImpl offscreenContext; // volatile: avoid locking for read-only access
    private boolean flipVertical;
    private int frameCount = 0;

    // For saving/restoring of OpenGL state during ReadPixels
    private final GLPixelStorageModes psm =  new GLPixelStorageModes();

    OffscreenBackend(final AWTGLPixelBufferProvider custom) {
        if(null == custom) {
            pixelBufferProvider = getSingleAWTGLPixelBufferProvider();
        } else {
            pixelBufferProvider = custom;
        }
        if( pixelBufferProvider instanceof SingletonGLPixelBufferProvider ) {
            useSingletonBuffer = true;
        } else {
            useSingletonBuffer = false;
        }
    }

    @Override
    public final boolean isUsingOwnLifecycle() { return false; }

    @Override
    public final void initialize() {
      if(DEBUG) {
          System.err.println(getThreadName()+": OffscreenBackend: initialize() - frameCount "+frameCount);
      }
      GLException glException = null;
      try {
          final GLContext[] shareWith = { null };
          if( helper.isSharedGLContextPending(shareWith) ) {
              return; // pending ..
          }
          offscreenDrawable = factory.createOffscreenDrawable(
                                                    null /* default platform device */,
                                                    reqOffscreenCaps,
                                                    chooser,
                                                    panelWidth, panelHeight);
          updateWrappedSurfaceScale(offscreenDrawable);
          offscreenDrawable.setRealized(true);
          if( DEBUG_FRAMES ) {
              offscreenDrawable.getNativeSurface().addSurfaceUpdatedListener(new SurfaceUpdatedListener() {
                  @Override
                  public final void surfaceUpdated(final Object updater, final NativeSurface ns, final long when) {
                      System.err.println(getThreadName()+": OffscreenBackend.swapBuffers - frameCount "+frameCount);
                  } } );
          }

          //
          // Pre context configuration
          //
          flipVertical = !GLJPanel.this.skipGLOrientationVerticalFlip && offscreenDrawable.isGLOriented();
          offscreenIsFBO = offscreenDrawable.getRequestedGLCapabilities().isFBO();
          final boolean useGLSLFlip_pre = flipVertical && offscreenIsFBO && reqOffscreenCaps.getGLProfile().isGL2ES2() && USE_GLSL_TEXTURE_RASTERIZER;
          if( offscreenIsFBO && !useGLSLFlip_pre ) {
              // Texture attachment only required for GLSL vertical flip, hence simply use a color-renderbuffer attachment.
              ((GLFBODrawable)offscreenDrawable).setFBOMode(0);
          }

          offscreenContext = (GLContextImpl) offscreenDrawable.createContext(shareWith[0]);
          offscreenContext.setContextCreationFlags(additionalCtxCreationFlags);
          if( GLContext.CONTEXT_NOT_CURRENT < offscreenContext.makeCurrent() ) {
              isInitialized = true;
              helper.setAutoSwapBufferMode(false); // we handle swap-buffers, see handlesSwapBuffer()

              final GL gl = offscreenContext.getGL();
              // Remedy for Bug 1020, i.e. OSX/Nvidia's FBO needs to be cleared before blitting,
              // otherwise first MSAA frame lacks antialiasing.
              // Clearing of FBO is performed within GLFBODrawableImpl.initialize(..):
              //   gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

              final GLCapabilitiesImmutable chosenCaps = offscreenDrawable.getChosenGLCapabilities();
              final boolean glslCompliant = !offscreenContext.hasRendererQuirk(GLRendererQuirks.GLSLNonCompliant);
              final boolean useGLSLFlip = useGLSLFlip_pre && gl.isGL2ES2() && glslCompliant;
              if( DEBUG ) {
                  System.err.println(getThreadName()+": OffscreenBackend.initialize: useGLSLFlip "+useGLSLFlip+
                          " [flip "+flipVertical+", isFBO "+offscreenIsFBO+", isGL2ES2 "+gl.isGL2ES2()+
                          ", noglsl "+!USE_GLSL_TEXTURE_RASTERIZER+", glslNonCompliant "+!glslCompliant+
                          ", isGL2ES2 " + gl.isGL2ES2()+"\n "+offscreenDrawable+"]");
              }
              if( useGLSLFlip ) {
                  final GLFBODrawable fboDrawable = (GLFBODrawable) offscreenDrawable;
                  fboDrawable.setTextureUnit( GLJPanel.this.requestedTextureUnit );
                  try {
                      fboFlipped = new FBObject();
                      fboFlipped.init(gl, panelWidth, panelHeight, 0);
                      fboFlipped.attachColorbuffer(gl, 0, chosenCaps.getAlphaBits()>0);
                      // fboFlipped.attachRenderbuffer(gl, Attachment.Type.DEPTH, 24);
                      gl.glClear(GL.GL_COLOR_BUFFER_BIT); // Bug 1020 (see above), cannot do in FBObject due to unknown 'first bind' state.
                      glslTextureRaster = new GLSLTextureRaster(fboDrawable.getTextureUnit(), true);
                      glslTextureRaster.init(gl.getGL2ES2());
                      glslTextureRaster.reshape(gl.getGL2ES2(), 0, 0, panelWidth, panelHeight);
                  } catch (final Exception ex) {
                      ex.printStackTrace();
                      if(null != glslTextureRaster) {
                        glslTextureRaster.dispose(gl.getGL2ES2());
                        glslTextureRaster = null;
                      }
                      if(null != fboFlipped) {
                        fboFlipped.destroy(gl);
                        fboFlipped = null;
                      }
                  }
              } else {
                  fboFlipped = null;
                  glslTextureRaster = null;
              }
              offscreenContext.release();
          } else {
              isInitialized = false;
          }
      } catch( final GLException gle ) {
          glException = gle;
      } finally {
          if( !isInitialized ) {
              if(null != offscreenContext) {
                  offscreenContext.destroy();
                  offscreenContext = null;
              }
              if(null != offscreenDrawable) {
                  offscreenDrawable.setRealized(false);
                  offscreenDrawable = null;
              }
          }
          if( null != glException ) {
              throw new GLException("Caught GLException: "+glException.getMessage(), glException);
          }
      }
    }

    @Override
    public final void destroy() {
      if(DEBUG) {
          System.err.println(getThreadName()+": OffscreenBackend: destroy() - offscreenContext: "+(null!=offscreenContext)+" - offscreenDrawable: "+(null!=offscreenDrawable)+" - frameCount "+frameCount);
      }
      if ( null != offscreenContext && offscreenContext.isCreated() ) {
        if( GLContext.CONTEXT_NOT_CURRENT < offscreenContext.makeCurrent() ) {
            try {
                final GL gl = offscreenContext.getGL();
                if(null != glslTextureRaster) {
                    glslTextureRaster.dispose(gl.getGL2ES2());
                }
                if(null != fboFlipped) {
                    fboFlipped.destroy(gl);
                }
            } finally {
                offscreenContext.destroy();
            }
        }
      }
      offscreenContext = null;
      glslTextureRaster = null;
      fboFlipped = null;
      offscreenContext = null;

      if (offscreenDrawable != null) {
        final AbstractGraphicsDevice adevice = offscreenDrawable.getNativeSurface().getGraphicsConfiguration().getScreen().getDevice();
        offscreenDrawable.setRealized(false);
        offscreenDrawable = null;
        if(null != adevice) {
            adevice.close();
        }
      }
      offscreenIsFBO = false;

      if( null != readBackIntsForCPUVFlip ) {
          readBackIntsForCPUVFlip.clear();
          readBackIntsForCPUVFlip = null;
      }
      if( null != pixelBuffer ) {
          if( !useSingletonBuffer ) {
              pixelBuffer.dispose();
          }
          pixelBuffer = null;
      }
      alignedImage = null;
    }

    @Override
    public final void setOpaque(final boolean opaque) {
      if ( opaque != isOpaque() && !useSingletonBuffer ) {
          pixelBuffer.dispose();
          pixelBuffer = null;
          alignedImage = null;
      }
    }

    @Override
    public final boolean preGL(final Graphics g) {
      // Empty in this implementation
      return true;
    }

    @Override
    public final boolean handlesSwapBuffer() {
        return true;
    }

    @Override
    public final void swapBuffers() {
        final GLDrawable d = offscreenDrawable;
        if( null != d ) {
            d.swapBuffers();
        }
    }

    @Override
    public final void postGL(final Graphics g, final boolean isDisplay) {
      if (isDisplay) {
        if( DEBUG_FRAMES ) {
            System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0: - frameCount "+frameCount);
        }

        final GL gl = offscreenContext.getGL();

        //
        // Save TextureState ASAP, i.e. the user values for the used FBO texture-unit
        // and the current active texture-unit (if not same)
        //
        final TextureState usrTexState, fboTexState;
        final int fboTexUnit;

        if( offscreenIsFBO ) {
            fboTexUnit = GL.GL_TEXTURE0 + ((GLFBODrawable)offscreenDrawable).getTextureUnit();
            usrTexState = new TextureState(gl, GL.GL_TEXTURE_2D);
            if( fboTexUnit != usrTexState.getUnit() ) {
                // glActiveTexture(..) + glBindTexture(..) are implicit performed in GLFBODrawableImpl's
                // swapBuffers/contextMadeCurent -> swapFBOImpl.
                // We need to cache the texture unit's bound texture-id before it's overwritten.
                gl.glActiveTexture(fboTexUnit);
                fboTexState = new TextureState(gl, fboTexUnit, GL.GL_TEXTURE_2D);
            } else {
                fboTexState = usrTexState;
            }
        } else {
            fboTexUnit = 0;
            usrTexState = null;
            fboTexState = null;
        }


        if( autoSwapBufferMode ) {
            // Since we only use a single-buffer non-MSAA or double-buffered MSAA offscreenDrawable,
            // we can always swap!
            offscreenDrawable.swapBuffers();
        }

        final int componentCount;
        final int alignment;
        if( isOpaque() ) {
            // w/o alpha
            componentCount = 3;
            alignment = 1;
        } else {
            // with alpha
            componentCount = 4;
            alignment = 4;
        }

        final PixelFormat awtPixelFormat = pixelBufferProvider.getAWTPixelFormat(gl.getGLProfile(), componentCount);
        final GLPixelAttributes pixelAttribs = pixelBufferProvider.getAttributes(gl, componentCount, true);

        if( useSingletonBuffer ) { // attempt to fetch the latest AWTGLPixelBuffer
            pixelBuffer = (AWTGLPixelBuffer) ((SingletonGLPixelBufferProvider)pixelBufferProvider).getSingleBuffer(awtPixelFormat.comp, pixelAttribs, true);
        }
        if( null != pixelBuffer && pixelBuffer.requiresNewBuffer(gl, panelWidth, panelHeight, 0) ) {
            pixelBuffer.dispose();
            pixelBuffer = null;
            alignedImage = null;
        }
        final boolean DEBUG_INIT;
        if ( null == pixelBuffer ) {
          if (0 >= panelWidth || 0 >= panelHeight ) {
              return;
          }
          pixelBuffer = pixelBufferProvider.allocate(gl, awtPixelFormat.comp, pixelAttribs, true, panelWidth, panelHeight, 1, 0);
          if(DEBUG) {
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0: "+GLJPanel.this.getName()+" pixelBufferProvider isSingletonBufferProvider "+useSingletonBuffer+", 0x"+Integer.toHexString(pixelBufferProvider.hashCode())+", "+pixelBufferProvider.getClass().getSimpleName());
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0: "+GLJPanel.this.getName()+" pixelBuffer 0x"+Integer.toHexString(pixelBuffer.hashCode())+", "+pixelBuffer+", alignment "+alignment);
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0: "+GLJPanel.this.getName()+" flippedVertical "+flipVertical+", glslTextureRaster "+(null!=glslTextureRaster)+", isGL2ES3 "+gl.isGL2ES3());
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0: "+GLJPanel.this.getName()+" panelSize "+panelWidth+"x"+panelHeight+" @ scale "+getPixelScaleStr());
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0: "+GLJPanel.this.getName()+" pixelAttribs "+pixelAttribs);
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0: "+GLJPanel.this.getName()+" awtPixelFormat "+awtPixelFormat);
              DEBUG_INIT = true;
          } else {
              DEBUG_INIT = false;
          }
        } else {
            DEBUG_INIT = false;
        }
        if( offscreenDrawable.getSurfaceWidth() != panelWidth || offscreenDrawable.getSurfaceHeight() != panelHeight ) {
            throw new InternalError("OffscreenDrawable panelSize mismatch (reshape missed): panelSize "+panelWidth+"x"+panelHeight+" != drawable "+offscreenDrawable.getSurfaceWidth()+"x"+offscreenDrawable.getSurfaceHeight()+", on thread "+getThreadName());
        }
        if( null == alignedImage ||
            panelWidth != alignedImage.getWidth() || panelHeight != alignedImage.getHeight() ||
            !pixelBuffer.isDataBufferSource(alignedImage) ) {
            alignedImage = pixelBuffer.getAlignedImage(panelWidth, panelHeight);
            if(DEBUG) {
                System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0: "+GLJPanel.this.getName()+" new alignedImage "+alignedImage.getWidth()+"x"+alignedImage.getHeight()+" @ scale "+getPixelScaleStr()+", "+alignedImage+", pixelBuffer "+pixelBuffer.width+"x"+pixelBuffer.height+", "+pixelBuffer);
            }
        }
        final IntBuffer readBackInts;

        if( !flipVertical || null != glslTextureRaster ) {
           readBackInts = (IntBuffer) pixelBuffer.buffer;
        } else {
           if( null == readBackIntsForCPUVFlip || pixelBuffer.width * pixelBuffer.height > readBackIntsForCPUVFlip.remaining() ) {
               readBackIntsForCPUVFlip = IntBuffer.allocate(pixelBuffer.width * pixelBuffer.height);
           }
           readBackInts = readBackIntsForCPUVFlip;
        }

        // Must now copy pixels from offscreen context into surface
        if( DEBUG_FRAMES ) {
            System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.readPixels: - frameCount "+frameCount);
        }

        // Save PACK modes, reset them to defaults and set alignment
        psm.setPackAlignment(gl, alignment);
        if( gl.isGL2ES3() ) {
            final GL2ES3 gl2es3 = gl.getGL2ES3();
            psm.setPackRowLength(gl2es3, panelWidth);
            gl2es3.glReadBuffer(gl2es3.getDefaultReadBuffer());
            if( DEBUG_INIT ) {
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.0: fboDrawable "+offscreenDrawable);
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.0: isGL2ES3, readBuffer 0x"+Integer.toHexString(gl2es3.getDefaultReadBuffer()));
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.0: def-readBuffer 0x"+Integer.toHexString(gl2es3.getDefaultReadBuffer()));
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.0: def-readFBO    0x"+Integer.toHexString(gl2es3.getDefaultReadFramebuffer()));
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.0: bound-readFBO  0x"+Integer.toHexString(gl2es3.getBoundFramebuffer(GL.GL_READ_FRAMEBUFFER)));
            }
        }

        if(null != glslTextureRaster) { // implies flippedVertical
            final boolean viewportChange;
            final int[] usrViewport = new int[] { 0, 0, 0, 0 };
            gl.glGetIntegerv(GL.GL_VIEWPORT, usrViewport, 0);
            viewportChange = 0 != usrViewport[0] || 0 != usrViewport[1] ||
                             panelWidth != usrViewport[2] || panelHeight != usrViewport[3];
            if( DEBUG_VIEWPORT ) {
                System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL: "+GLJPanel.this.getName()+" Viewport: change "+viewportChange+
                         ", "+usrViewport[0]+"/"+usrViewport[1]+" "+usrViewport[2]+"x"+usrViewport[3]+
                         " -> 0/0 "+panelWidth+"x"+panelHeight);
            }
            if( viewportChange ) {
                gl.glViewport(0, 0, panelWidth, panelHeight);
            }

            // perform vert-flipping via OpenGL/FBO
            final GLFBODrawable fboDrawable = (GLFBODrawable)offscreenDrawable;
            final FBObject.TextureAttachment fboTex = fboDrawable.getColorbuffer(GL.GL_FRONT).getTextureAttachment();

            fboFlipped.bind(gl);

            // gl.glActiveTexture(GL.GL_TEXTURE0 + fboDrawable.getTextureUnit()); // implicit by GLFBODrawableImpl: swapBuffers/contextMadeCurent -> swapFBOImpl
            gl.glBindTexture(GL.GL_TEXTURE_2D, fboTex.getName());
            // gl.glClear(GL.GL_DEPTH_BUFFER_BIT); // fboFlipped runs w/o DEPTH!

            glslTextureRaster.display(gl.getGL2ES2());
            if( DEBUG_INIT ) {
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.1: fboDrawable "+fboDrawable);
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.1: read from fbo-rb "+fboFlipped.getReadFramebuffer()+", fbo "+fboFlipped);
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.1: isGL2ES3, readBuffer 0x"+Integer.toHexString(gl.getDefaultReadBuffer()));
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.1: def-readBuffer 0x"+Integer.toHexString(gl.getDefaultReadBuffer()));
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.1: def-readFBO    0x"+Integer.toHexString(gl.getDefaultReadFramebuffer()));
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.1: bound-readFBO  0x"+Integer.toHexString(gl.getBoundFramebuffer(GL.GL_READ_FRAMEBUFFER)));
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.1: "+GLJPanel.this.getName()+" pixelAttribs "+pixelAttribs);
            }
            gl.glReadPixels(0, 0, panelWidth, panelHeight, pixelAttribs.format, pixelAttribs.type, readBackInts);

            fboFlipped.unbind(gl);
            if( DEBUG_INIT ) {
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.2: fboDrawable "+fboDrawable);
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.2: read from fbo-rb "+fboFlipped.getReadFramebuffer()+", fbo "+fboFlipped);
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.2: isGL2ES3, readBuffer 0x"+Integer.toHexString(gl.getDefaultReadBuffer()));
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.2: def-readBuffer 0x"+Integer.toHexString(gl.getDefaultReadBuffer()));
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.2: def-readFBO    0x"+Integer.toHexString(gl.getDefaultReadFramebuffer()));
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0.2: bound-readFBO  0x"+Integer.toHexString(gl.getBoundFramebuffer(GL.GL_READ_FRAMEBUFFER)));
            }
            if( viewportChange ) {
                gl.glViewport(usrViewport[0], usrViewport[1], usrViewport[2], usrViewport[3]);
            }
        } else {
            gl.glReadPixels(0, 0, panelWidth, panelHeight, pixelAttribs.format, pixelAttribs.type, readBackInts);

            if ( flipVertical ) {
                // Copy temporary data into raster of BufferedImage for faster
                // blitting Note that we could avoid this copy in the cases
                // where !offscreenDrawable.isGLOriented(),
                // but that's the software rendering path which is very slow anyway.
                final BufferedImage image = alignedImage;
                final int[] src = readBackInts.array();
                final int[] dest = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
                final int incr = panelWidth;
                int srcPos = 0;
                int destPos = (panelHeight - 1) * panelWidth;
                for (; destPos >= 0; srcPos += incr, destPos -= incr) {
                  System.arraycopy(src, srcPos, dest, destPos, incr);
                }
            }
        }
        if( 0 != fboTexUnit ) { // implies offscreenIsFBO
            fboTexState.restore(gl);
            if( fboTexUnit != usrTexState.getUnit() ) {
                usrTexState.restore(gl);
            }
        }

        // Restore saved modes.
        psm.restore(gl);

        // Note: image will be drawn back in paintComponent() for
        // correctness on all platforms
      }
    }

    @Override
    public final int getTextureUnit() {
        if(null != glslTextureRaster && null != offscreenDrawable) { // implies flippedVertical
            return ((GLFBODrawable)offscreenDrawable).getTextureUnit();
        }
        return -1;
    }

    @Override
    public final void doPaintComponent(final Graphics g) {
      helper.invokeGL(offscreenDrawable, offscreenContext, updaterDisplayAction, updaterInitAction);

      if ( null != alignedImage ) {
        if( DEBUG_FRAMES ) {
            System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.doPaintComponent.drawImage: - frameCount "+frameCount);
        }
        // Draw resulting image in one shot
        g.drawImage(alignedImage, 0, 0,
                    SurfaceScaleUtils.scaleInv(alignedImage.getWidth(), hasPixelScale[0]),
                    SurfaceScaleUtils.scaleInv(alignedImage.getHeight(), hasPixelScale[1]), null); // Null ImageObserver since image data is ready.
      }
      frameCount++;
    }

    @Override
    public final void doPlainPaint() {
      helper.invokeGL(offscreenDrawable, offscreenContext, updaterPlainDisplayAction, updaterInitAction);
    }

    @Override
    public final boolean handleReshape() {
        GLDrawableImpl _drawable = (GLDrawableImpl)offscreenDrawable;
        {
            final GLDrawableImpl _drawableNew = GLDrawableHelper.resizeOffscreenDrawable(_drawable, offscreenContext, panelWidth, panelHeight);
            if(_drawable != _drawableNew) {
                // write back
                _drawable = _drawableNew;
                offscreenDrawable = _drawableNew;
                updateWrappedSurfaceScale(offscreenDrawable);
            }
        }
        if (DEBUG) {
            System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.handleReshape: " +panelWidth+"x"+panelHeight + " @ scale "+getPixelScaleStr() + " -> " + _drawable.getSurfaceWidth()+"x"+_drawable.getSurfaceHeight());
        }
        panelWidth = _drawable.getSurfaceWidth();
        panelHeight = _drawable.getSurfaceHeight();

        if( null != glslTextureRaster ) {
            if( GLContext.CONTEXT_NOT_CURRENT < offscreenContext.makeCurrent() ) {
                try {
                    final GL gl = offscreenContext.getGL();
                    fboFlipped.reset(gl, panelWidth, panelHeight, 0);
                    glslTextureRaster.reshape(gl.getGL2ES2(), 0, 0, panelWidth, panelHeight);
                } finally {
                    offscreenContext.release();
                }
            }
        }
        return _drawable.isRealized();
    }

    @Override
    public final GLContext createContext(final GLContext shareWith) {
      return (null != offscreenDrawable) ? offscreenDrawable.createContext(shareWith) : null;
    }

    @Override
    public final void setContext(final GLContext ctx) {
      offscreenContext=(GLContextImpl)ctx;
    }

    @Override
    public final GLContext getContext() {
      return offscreenContext;
    }

    @Override
    public final GLDrawable getDrawable() {
        return offscreenDrawable;
    }

    @Override
    public final GLCapabilitiesImmutable getChosenGLCapabilities() {
      if (offscreenDrawable == null) {
        return null;
      }
      return offscreenDrawable.getChosenGLCapabilities();
    }

    @Override
    public final GLProfile getGLProfile() {
      if (offscreenDrawable == null) {
        return null;
      }
      return offscreenDrawable.getGLProfile();
    }
  }

  class J2DOGLBackend implements Backend {
    // Opaque Object identifier representing the Java2D surface we are
    // drawing to; used to determine when to destroy and recreate JOGL
    // context
    private Object j2dSurface;
    // Graphics object being used during Java2D update action
    // (absolutely essential to cache this)
    // No-op context representing the Java2D OpenGL context
    private GLContext j2dContext;
    // Context associated with no-op drawable representing the JOGL
    // OpenGL context
    private GLDrawable joglDrawable;
    // The real OpenGL context JOGL uses to render
    private GLContext  joglContext;
    // State captured from Java2D OpenGL context necessary in order to
    // properly render into Java2D back buffer
    private final int[] drawBuffer   = new int[1];
    private final int[] readBuffer   = new int[1];
    // This is required when the FBO option of the Java2D / OpenGL
    // pipeline is active
    private final int[] frameBuffer  = new int[1];
    // Current (as of this writing) NVidia drivers have a couple of bugs
    // relating to the sharing of framebuffer and renderbuffer objects
    // between contexts. It appears we have to (a) reattach the color
    // attachment and (b) actually create new depth buffer storage and
    // attach it in order for the FBO to behave properly in our context.
    private boolean checkedForFBObjectWorkarounds;
    private boolean fbObjectWorkarounds;
    private int[] frameBufferDepthBuffer;
    private int[] frameBufferTexture;
    private boolean createNewDepthBuffer;
    // Current (as of this writing) ATI drivers have problems when the
    // same FBO is bound in two different contexts. Here we check for
    // this case and explicitly release the FBO from Java2D's context
    // before switching to ours. Java2D will re-bind the FBO when it
    // makes its context current the next time. Interestingly, if we run
    // this code path on NVidia hardware, it breaks the rendering
    // results -- no output is generated. This doesn't appear to be an
    // interaction with the abovementioned NVidia-specific workarounds,
    // as even if we disable that code the FBO is still reported as
    // incomplete in our context.
    private boolean checkedGLVendor;
    private boolean vendorIsATI;

    // Holding on to this GraphicsConfiguration is a workaround for a
    // problem in the Java 2D / JOGL bridge when FBOs are enabled; see
    // comment related to Issue 274 below
    private GraphicsConfiguration workaroundConfig;

    @Override
    public final boolean isUsingOwnLifecycle() { return true; }

    @Override
    public final void initialize() {
      if(DEBUG) {
          System.err.println(getThreadName()+": J2DOGL: initialize()");
      }
      // No-op in this implementation; everything is done lazily
      isInitialized = true;
    }

    @Override
    public final void destroy() {
      Java2D.invokeWithOGLContextCurrent(null, new Runnable() {
          @Override
          public void run() {
            if(DEBUG) {
                System.err.println(getThreadName()+": J2DOGL: destroy() - joglContext: "+(null!=joglContext)+" - joglDrawable: "+(null!=joglDrawable));
            }
            if (joglContext != null) {
              joglContext.destroy();
              joglContext = null;
            }
            joglDrawable = null;
            if (j2dContext != null) {
              j2dContext.destroy();
              j2dContext = null;
            }
          }
        });
    }

    @Override
    public final void setOpaque(final boolean opaque) {
      // Empty in this implementation
    }

    @Override
    public final GLContext createContext(final GLContext shareWith) {
      if(null != shareWith) {
          throw new GLException("J2DOGLBackend cannot create context w/ additional shared context, since it already needs to share the context w/ J2D.");
      }
      return (null != joglDrawable && null != j2dContext) ? joglDrawable.createContext(j2dContext) : null;
    }

    @Override
    public final void setContext(final GLContext ctx) {
        joglContext=ctx;
    }

    @Override
    public final GLContext getContext() {
      return joglContext;
    }

    @Override
    public final GLDrawable getDrawable() {
        return joglDrawable;
    }

    @Override
    public final int getTextureUnit() { return -1; }

    @Override
    public final GLCapabilitiesImmutable getChosenGLCapabilities() {
      // FIXME: should do better than this; is it possible to query J2D Caps ?
      return new GLCapabilities(null);
    }

    @Override
    public final GLProfile getGLProfile() {
      // FIXME: should do better than this; is it possible to query J2D's Profile ?
      return GLProfile.getDefault(GLProfile.getDefaultDevice());
    }

    @Override
    public final boolean handleReshape() {
      // Empty in this implementation
      return true;
    }

    @Override
    public final boolean preGL(final Graphics g) {
      final GL2 gl = joglContext.getGL().getGL2();
      // Set up needed state in JOGL context from Java2D context
      gl.glEnable(GL.GL_SCISSOR_TEST);
      final Rectangle r = Java2D.getOGLScissorBox(g);

      if (r == null) {
        if (DEBUG) {
          System.err.println(getThreadName()+": Java2D.getOGLScissorBox() returned null");
        }
        return false;
      }
      if (DEBUG) {
        System.err.println(getThreadName()+": GLJPanel: gl.glScissor(" + r.x + ", " + r.y + ", " + r.width + ", " + r.height + ")");
      }

      gl.glScissor(r.x, r.y, r.width, r.height);
      final Rectangle oglViewport = Java2D.getOGLViewport(g, panelWidth, panelHeight);
      // If the viewport X or Y changes, in addition to the panel's
      // width or height, we need to send a reshape operation to the
      // client
      if ((viewportX != oglViewport.x) ||
          (viewportY != oglViewport.y)) {
        sendReshape = true;
        if (DEBUG) {
          System.err.println(getThreadName()+": Sending reshape because viewport changed");
          System.err.println("  viewportX (" + viewportX + ") ?= oglViewport.x (" + oglViewport.x + ")");
          System.err.println("  viewportY (" + viewportY + ") ?= oglViewport.y (" + oglViewport.y + ")");
        }
      }
      viewportX = oglViewport.x;
      viewportY = oglViewport.y;

      // If the FBO option is active, bind to the FBO from the Java2D
      // context.
      // Note that all of the plumbing in the context sharing stuff will
      // allow us to bind to this object since it's in our namespace.
      if (Java2D.isFBOEnabled() &&
          Java2D.getOGLSurfaceType(g) == Java2D.FBOBJECT) {

        // The texture target for Java2D's OpenGL pipeline when using FBOs
        // -- either GL_TEXTURE_2D or GL_TEXTURE_RECTANGLE_ARB
        final int fboTextureTarget = Java2D.getOGLTextureType(g);

        if (!checkedForFBObjectWorkarounds) {
          checkedForFBObjectWorkarounds = true;
          gl.glBindTexture(fboTextureTarget, 0);
          gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, frameBuffer[0]);
          final int status = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
          if (status != GL.GL_FRAMEBUFFER_COMPLETE) {
              // Need to do workarounds
              fbObjectWorkarounds = true;
              createNewDepthBuffer = true;
              if (DEBUG) {
                  System.err.println(getThreadName()+": GLJPanel: ERR GL_FRAMEBUFFER_BINDING: Discovered Invalid J2D FBO("+frameBuffer[0]+"): "+FBObject.getStatusString(status) +
                                     ", frame_buffer_object workarounds to be necessary");
              }
          } else {
            // Don't need the frameBufferTexture temporary any more
            frameBufferTexture = null;
            if (DEBUG) {
              System.err.println(getThreadName()+": GLJPanel: OK GL_FRAMEBUFFER_BINDING: "+frameBuffer[0]);
            }
          }
        }

        if (fbObjectWorkarounds && createNewDepthBuffer) {
          if (frameBufferDepthBuffer == null)
            frameBufferDepthBuffer = new int[1];

          // Create our own depth renderbuffer and associated storage
          // If we have an old one, delete it
          if (frameBufferDepthBuffer[0] != 0) {
            gl.glDeleteRenderbuffers(1, frameBufferDepthBuffer, 0);
            frameBufferDepthBuffer[0] = 0;
          }

          gl.glBindTexture(fboTextureTarget, frameBufferTexture[0]);
          final int[] width = new int[1];
          final int[] height = new int[1];
          gl.glGetTexLevelParameteriv(fboTextureTarget, 0, GL2ES3.GL_TEXTURE_WIDTH, width, 0);
          gl.glGetTexLevelParameteriv(fboTextureTarget, 0, GL2ES3.GL_TEXTURE_HEIGHT, height, 0);

          gl.glGenRenderbuffers(1, frameBufferDepthBuffer, 0);
          if (DEBUG) {
            System.err.println(getThreadName()+": GLJPanel: Generated frameBufferDepthBuffer " + frameBufferDepthBuffer[0] +
                               " with width " + width[0] + ", height " + height[0]);
          }

          gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, frameBufferDepthBuffer[0]);
          // FIXME: may need a loop here like in Java2D
          gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL.GL_DEPTH_COMPONENT24, width[0], height[0]);

          gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, 0);
          createNewDepthBuffer = false;
        }

        gl.glBindTexture(fboTextureTarget, 0);
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, frameBuffer[0]);

        if (fbObjectWorkarounds) {
          // Hook up the color and depth buffer attachment points for this framebuffer
          gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER,
                                    GL.GL_COLOR_ATTACHMENT0,
                                    fboTextureTarget,
                                    frameBufferTexture[0],
                                    0);
          if (DEBUG) {
            System.err.println(getThreadName()+": GLJPanel: frameBufferDepthBuffer: " + frameBufferDepthBuffer[0]);
          }
          gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER,
                                       GL.GL_DEPTH_ATTACHMENT,
                                       GL.GL_RENDERBUFFER,
                                       frameBufferDepthBuffer[0]);
        }

        if (DEBUG) {
          final int status = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
          if (status != GL.GL_FRAMEBUFFER_COMPLETE) {
            throw new GLException("Error: framebuffer was incomplete: status = 0x" +
                                  Integer.toHexString(status));
          }
        }
      } else {
        if (DEBUG) {
          System.err.println(getThreadName()+": GLJPanel: Setting up drawBuffer " + drawBuffer[0] +
                             " and readBuffer " + readBuffer[0]);
        }

        gl.glDrawBuffer(drawBuffer[0]);
        gl.glReadBuffer(readBuffer[0]);
      }

      return true;
    }

    @Override
    public final boolean handlesSwapBuffer() {
        return false;
    }

    @Override
    public final void swapBuffers() {
        final GLDrawable d = joglDrawable;
        if( null != d ) {
            d.swapBuffers();
        }
    }

    @Override
    public final void postGL(final Graphics g, final boolean isDisplay) {
      // Cause OpenGL pipeline to flush its results because
      // otherwise it's possible we will buffer up multiple frames'
      // rendering results, resulting in apparent mouse lag
      final GL gl = joglContext.getGL();
      gl.glFinish();

      if (Java2D.isFBOEnabled() &&
          Java2D.getOGLSurfaceType(g) == Java2D.FBOBJECT) {
        // Unbind the framebuffer from our context to work around
        // apparent driver bugs or at least unspecified behavior causing
        // OpenGL to run out of memory with certain cards and drivers
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
      }
    }

    @Override
    public final void doPaintComponent(final Graphics g) {
      // This is a workaround for an issue in the Java 2D / JOGL
      // bridge (reported by an end user as JOGL Issue 274) where Java
      // 2D can occasionally leave its internal OpenGL context current
      // to the on-screen window rather than its internal "scratch"
      // pbuffer surface to which the FBO is attached. JOGL expects to
      // find a stable OpenGL drawable (on Windows, an HDC) upon which
      // it can create another OpenGL context. It turns out that, on
      // Windows, when Java 2D makes its internal OpenGL context
      // current against the window in order to put pixels on the
      // screen, it gets the device context for the window, makes its
      // context current, and releases the device context. This means
      // that when JOGL's Runnable gets to run below, the HDC is
      // already invalid. The workaround for this is to force Java 2D
      // to make its context current to the scratch surface, which we
      // can do by executing an empty Runnable with the "shared"
      // context current. This will be fixed in a Java SE 6 update
      // release, hopefully 6u2.
      if (Java2D.isFBOEnabled()) {
        if (workaroundConfig == null) {
          workaroundConfig = GraphicsEnvironment.
            getLocalGraphicsEnvironment().
            getDefaultScreenDevice().
            getDefaultConfiguration();
        }
        Java2D.invokeWithOGLSharedContextCurrent(workaroundConfig, new Runnable() { @Override
        public void run() {}});
      }

      Java2D.invokeWithOGLContextCurrent(g, new Runnable() {
          @Override
          public void run() {
            if (DEBUG) {
              System.err.println(getThreadName()+": GLJPanel.invokeWithOGLContextCurrent");
            }

            // Create no-op context representing Java2D context
            if (j2dContext == null) {
              j2dContext = factory.createExternalGLContext();
              if (DEBUG) {
                System.err.println(getThreadName()+": GLJPanel.Created External Context: "+j2dContext);
              }
              if (DEBUG) {
//                j2dContext.setGL(new DebugGL2(j2dContext.getGL().getGL2()));
              }

              // Check to see whether we can support the requested
              // capabilities or need to fall back to a pbuffer
              // FIXME: add more checks?

              j2dContext.makeCurrent();
              final GL gl = j2dContext.getGL();
              if ((getGLInteger(gl, GL.GL_RED_BITS)         < reqOffscreenCaps.getRedBits())        ||
                  (getGLInteger(gl, GL.GL_GREEN_BITS)       < reqOffscreenCaps.getGreenBits())      ||
                  (getGLInteger(gl, GL.GL_BLUE_BITS)        < reqOffscreenCaps.getBlueBits())       ||
                  //                  (getGLInteger(gl, GL.GL_ALPHA_BITS)       < offscreenCaps.getAlphaBits())      ||
                  (getGLInteger(gl, GL2.GL_ACCUM_RED_BITS)   < reqOffscreenCaps.getAccumRedBits())   ||
                  (getGLInteger(gl, GL2.GL_ACCUM_GREEN_BITS) < reqOffscreenCaps.getAccumGreenBits()) ||
                  (getGLInteger(gl, GL2.GL_ACCUM_BLUE_BITS)  < reqOffscreenCaps.getAccumBlueBits())  ||
                  (getGLInteger(gl, GL2.GL_ACCUM_ALPHA_BITS) < reqOffscreenCaps.getAccumAlphaBits()) ||
                  //          (getGLInteger(gl, GL2.GL_DEPTH_BITS)       < offscreenCaps.getDepthBits())      ||
                  (getGLInteger(gl, GL.GL_STENCIL_BITS)     < reqOffscreenCaps.getStencilBits())) {
                if (DEBUG) {
                  System.err.println(getThreadName()+": GLJPanel: Falling back to pbuffer-based support because Java2D context insufficient");
                  System.err.println("                    Available              Required");
                  System.err.println("GL_RED_BITS         " + getGLInteger(gl, GL.GL_RED_BITS)         + "              " + reqOffscreenCaps.getRedBits());
                  System.err.println("GL_GREEN_BITS       " + getGLInteger(gl, GL.GL_GREEN_BITS)       + "              " + reqOffscreenCaps.getGreenBits());
                  System.err.println("GL_BLUE_BITS        " + getGLInteger(gl, GL.GL_BLUE_BITS)        + "              " + reqOffscreenCaps.getBlueBits());
                  System.err.println("GL_ALPHA_BITS       " + getGLInteger(gl, GL.GL_ALPHA_BITS)       + "              " + reqOffscreenCaps.getAlphaBits());
                  System.err.println("GL_ACCUM_RED_BITS   " + getGLInteger(gl, GL2.GL_ACCUM_RED_BITS)   + "              " + reqOffscreenCaps.getAccumRedBits());
                  System.err.println("GL_ACCUM_GREEN_BITS " + getGLInteger(gl, GL2.GL_ACCUM_GREEN_BITS) + "              " + reqOffscreenCaps.getAccumGreenBits());
                  System.err.println("GL_ACCUM_BLUE_BITS  " + getGLInteger(gl, GL2.GL_ACCUM_BLUE_BITS)  + "              " + reqOffscreenCaps.getAccumBlueBits());
                  System.err.println("GL_ACCUM_ALPHA_BITS " + getGLInteger(gl, GL2.GL_ACCUM_ALPHA_BITS) + "              " + reqOffscreenCaps.getAccumAlphaBits());
                  System.err.println("GL_DEPTH_BITS       " + getGLInteger(gl, GL.GL_DEPTH_BITS)       + "              " + reqOffscreenCaps.getDepthBits());
                  System.err.println("GL_STENCIL_BITS     " + getGLInteger(gl, GL.GL_STENCIL_BITS)     + "              " + reqOffscreenCaps.getStencilBits());
                }
                isInitialized = false;
                backend = null;
                java2DGLPipelineOK = false;
                handleReshape = true;
                j2dContext.destroy();
                j2dContext = null;
                return;
              }
            } else {
              j2dContext.makeCurrent();
            }
            try {
              captureJ2DState(j2dContext.getGL(), g);
              final Object curSurface = Java2D.getOGLSurfaceIdentifier(g);
              if (curSurface != null) {
                if (j2dSurface != curSurface) {
                  if (joglContext != null) {
                    joglContext.destroy();
                    joglContext = null;
                    joglDrawable = null;
                    sendReshape = true;
                    if (DEBUG) {
                      System.err.println(getThreadName()+": Sending reshape because surface changed");
                      System.err.println("New surface = " + curSurface);
                    }
                  }
                  j2dSurface = curSurface;
                  if (DEBUG) {
                      System.err.print(getThreadName()+": Surface type: ");
                      final int surfaceType = Java2D.getOGLSurfaceType(g);
                      if (surfaceType == Java2D.UNDEFINED) {
                        System.err.println("UNDEFINED");
                      } else if (surfaceType == Java2D.WINDOW) {
                        System.err.println("WINDOW");
                      } else if (surfaceType == Java2D.PBUFFER) {
                        System.err.println("PBUFFER");
                      } else if (surfaceType == Java2D.TEXTURE) {
                        System.err.println("TEXTURE");
                      } else if (surfaceType == Java2D.FLIP_BACKBUFFER) {
                        System.err.println("FLIP_BACKBUFFER");
                      } else if (surfaceType == Java2D.FBOBJECT) {
                        System.err.println("FBOBJECT");
                      } else {
                        System.err.println("(Unknown surface type " + surfaceType + ")");
                      }
                  }
                }
                if (joglContext == null) {
                  final AbstractGraphicsDevice device = j2dContext.getGLDrawable().getNativeSurface().getGraphicsConfiguration().getScreen().getDevice();
                  if (factory.canCreateExternalGLDrawable(device)) {
                    joglDrawable = factory.createExternalGLDrawable();
                    joglContext = joglDrawable.createContext(j2dContext);
                    if (DEBUG) {
                        System.err.println("-- Created External Drawable: "+joglDrawable);
                        System.err.println("-- Created Context: "+joglContext);
                    }
                  }
                  if (Java2D.isFBOEnabled() &&
                      Java2D.getOGLSurfaceType(g) == Java2D.FBOBJECT &&
                      fbObjectWorkarounds) {
                    createNewDepthBuffer = true;
                  }
                }
                helper.invokeGL(joglDrawable, joglContext, updaterDisplayAction, updaterInitAction);
              }
            } finally {
              j2dContext.release();
            }
          }
        });
    }

    @Override
    public final void doPlainPaint() {
      helper.invokeGL(joglDrawable, joglContext, updaterPlainDisplayAction, updaterInitAction);
    }

    private final void captureJ2DState(final GL gl, final Graphics g) {
      gl.glGetIntegerv(GL2GL3.GL_DRAW_BUFFER, drawBuffer, 0);
      gl.glGetIntegerv(GL2ES3.GL_READ_BUFFER, readBuffer, 0);
      if (Java2D.isFBOEnabled() &&
          Java2D.getOGLSurfaceType(g) == Java2D.FBOBJECT) {
        gl.glGetIntegerv(GL.GL_FRAMEBUFFER_BINDING, frameBuffer, 0);
        if(!gl.glIsFramebuffer(frameBuffer[0])) {
          checkedForFBObjectWorkarounds=true;
          fbObjectWorkarounds = true;
          createNewDepthBuffer = true;
          if (DEBUG) {
              System.err.println(getThreadName()+": GLJPanel: Fetched ERR GL_FRAMEBUFFER_BINDING: "+frameBuffer[0]+" - NOT A FBO"+
                                 ", frame_buffer_object workarounds to be necessary");
          }
        } else if (DEBUG) {
          System.err.println(getThreadName()+": GLJPanel: Fetched OK GL_FRAMEBUFFER_BINDING: "+frameBuffer[0]);
        }

        if(fbObjectWorkarounds || !checkedForFBObjectWorkarounds) {
            // See above for description of what we are doing here
            if (frameBufferTexture == null)
                frameBufferTexture = new int[1];

            // Query the framebuffer for its color buffer so we can hook
            // it back up in our context (should not be necessary)
            gl.glGetFramebufferAttachmentParameteriv(GL.GL_FRAMEBUFFER,
                                                     GL.GL_COLOR_ATTACHMENT0,
                                                     GL.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME,
                                                     frameBufferTexture, 0);
            if (DEBUG) {
                System.err.println(getThreadName()+": GLJPanel: FBO COLOR_ATTACHMENT0: " + frameBufferTexture[0]);
            }
        }

        if (!checkedGLVendor) {
          checkedGLVendor = true;
          final String vendor = gl.glGetString(GL.GL_VENDOR);

          if ((vendor != null) &&
              vendor.startsWith("ATI")) {
            vendorIsATI = true;
          }
        }

        if (vendorIsATI) {
          // Unbind the FBO from Java2D's context as it appears that
          // driver bugs on ATI's side are causing problems if the FBO is
          // simultaneously bound to more than one context. Java2D will
          // re-bind the FBO during the next validation of its context.
          // Note: this breaks rendering at least on NVidia hardware
          gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        }
      }
    }
  }
}

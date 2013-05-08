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

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.beans.Beans;
import java.nio.IntBuffer;
import java.util.List;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.WindowClosingProtocol;
import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
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
import javax.media.opengl.GLFBODrawable;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLRunnable;
import javax.media.opengl.Threading;
import javax.swing.JPanel;

import jogamp.opengl.Debug;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDrawableHelper;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.awt.Java2D;
import jogamp.opengl.util.glsl.GLSLTextureRaster;
import com.jogamp.nativewindow.awt.AWTWindowClosingProtocol;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.GLPixelStorageModes;
import com.jogamp.opengl.util.texture.TextureData.PixelAttributes;
import com.jogamp.opengl.util.texture.awt.AWTTextureData.AWTPixelBufferProviderInt;

/** A lightweight Swing component which provides OpenGL rendering
    support. Provided for compatibility with Swing user interfaces
    when adding a heavyweight doesn't work either because of
    Z-ordering or LayoutManager problems.
    <P>
    The GLJPanel can be made transparent by creating it with a
    GLCapabilities object with alpha bits specified and calling {@link
    #setOpaque}(false). Pixels with resulting OpenGL alpha values less
    than 1.0 will be overlaid on any underlying Swing rendering. </P>
    <P>
    This component attempts to use hardware-accelerated rendering via FBO or pbuffers and
    falls back on to software rendering if none of the former are available
    using {@link GLDrawableFactory#createOffscreenDrawable(AbstractGraphicsDevice, GLCapabilitiesImmutable, GLCapabilitiesChooser, int, int) GLDrawableFactory.createOffscreenDrawable(..)}.<br/>
    </P>
    <P>
    In case FBO is used and GLSL is available, a fragment shader is utilized
    to flip the FBO texture vertically. This hardware-accelerated step can be disabled via system property <code>jogl.gljpanel.noglsl</code>.
    </P>
    <P>
    The OpenGL path is concluded by copying the rendered pixels an {@link BufferedImage} via {@link GL#glReadPixels(int, int, int, int, int, int, java.nio.Buffer) glReadPixels(..)}
    for later Java2D composition.
    </p>
    <p>
    In case the above mentioned GLSL vertical-flipping is not performed,
    {@link System#arraycopy(Object, int, Object, int, int) System.arraycopy(..)} is used line by line. 
    This step causes more CPU load per frame and is not hardware-accelerated.
    </p>
    <p>
    Finally the Java2D compositioning takes place via via {@link Graphics#drawImage(java.awt.Image, int, int, int, int, java.awt.image.ImageObserver) Graphics.drawImage(...)}
    on the prepared {@link BufferedImage} as described above.
    </p>
    <P>
 *  Please read <A HREF="GLCanvas.html#java2dgl">Java2D OpenGL Remarks</A>.
 *  </P>
*/

@SuppressWarnings("serial")
public class GLJPanel extends JPanel implements AWTGLAutoDrawable, WindowClosingProtocol {
  private static final boolean DEBUG = Debug.debug("GLJPanel");
  private static final boolean DEBUG_VIEWPORT = Debug.isPropertyDefined("jogl.debug.GLJPanel.Viewport", true);
  private static final boolean USE_GLSL_TEXTURE_RASTERIZER = !Debug.isPropertyDefined("jogl.gljpanel.noglsl", true);    

  private GLDrawableHelper helper = new GLDrawableHelper();
  private volatile boolean isInitialized;

  //
  // Data used for either pbuffers or pixmap-based offscreen surfaces
  //
  /** Single buffered offscreen caps */
  private GLCapabilitiesImmutable offscreenCaps;
  private GLProfile             glProfile;
  private GLDrawableFactoryImpl factory;
  private GLCapabilitiesChooser chooser;
  private GLContext             shareWith;
  private int additionalCtxCreationFlags = 0;

  // Lazy reshape notification: reshapeWidth -> panelWidth -> backend.width
  private boolean handleReshape = false;
  private boolean sendReshape = true;

  // For handling reshape events lazily: reshapeWidth -> panelWidth -> backend.width
  private int reshapeWidth;
  private int reshapeHeight;

  // Width of the actual GLJPanel: reshapeWidth -> panelWidth -> backend.width
  private int panelWidth   = 0;
  private int panelHeight  = 0;
  
  // These are always set to (0, 0) except when the Java2D / OpenGL
  // pipeline is active
  private int viewportX;
  private int viewportY;

  // The backend in use
  private volatile Backend backend;

  // Used by all backends either directly or indirectly to hook up callbacks
  private Updater updater = new Updater();

  // Indicates whether the Java 2D OpenGL pipeline is enabled and resource-compatible
  private boolean oglPipelineUsable =
    Java2D.isOGLPipelineResourceCompatible() &&
    !Debug.isPropertyDefined("jogl.gljpanel.noogl", true);

  private AWTWindowClosingProtocol awtWindowClosingProtocol =
          new AWTWindowClosingProtocol(this, new Runnable() {
                @Override
                public void run() {
                    GLJPanel.this.destroy();
                }
            }, null);

  static {
    // Force eager initialization of part of the Java2D class since
    // otherwise it's likely it will try to be initialized while on
    // the Queue Flusher Thread, which is not allowed
    if (Java2D.isOGLPipelineResourceCompatible() && Java2D.isFBOEnabled()) {
      Java2D.getShareContext(GraphicsEnvironment.
                             getLocalGraphicsEnvironment().
                             getDefaultScreenDevice());
    }
  }

  /** Creates a new GLJPanel component with a default set of OpenGL
      capabilities and using the default OpenGL capabilities selection
      mechanism.
   * @throws GLException if no default profile is available for the default desktop device.
   */
  public GLJPanel() throws GLException {
    this(null);
  }

  /** Creates a new GLJPanel component with the requested set of
      OpenGL capabilities, using the default OpenGL capabilities
      selection mechanism.
   * @throws GLException if no GLCapabilities are given and no default profile is available for the default desktop device.
   */
  public GLJPanel(GLCapabilitiesImmutable userCapsRequest) throws GLException {
    this(userCapsRequest, null, null);
  }

  /** Creates a new GLJPanel component. The passed GLCapabilities
      specifies the OpenGL capabilities for the component; if null, a
      default set of capabilities is used. The GLCapabilitiesChooser
      specifies the algorithm for selecting one of the available
      GLCapabilities for the component; a DefaultGLCapabilitesChooser
      is used if null is passed for this argument. The passed
      GLContext specifies an OpenGL context with which to share
      textures, display lists and other OpenGL state, and may be null
      if sharing is not desired. See the note in the overview documentation on
      <a href="../../../overview-summary.html#SHARING">context sharing</a>.
      <P>
      Note: Sharing cannot be enabled using J2D OpenGL FBO sharing,
      since J2D GL Context must be shared and we can only share one context.
    * @throws GLException if no GLCapabilities are given and no default profile is available for the default desktop device.
  */
  public GLJPanel(GLCapabilitiesImmutable userCapsRequest, GLCapabilitiesChooser chooser, GLContext shareWith)
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
        offscreenCaps = caps;
    }
    this.glProfile = offscreenCaps.getGLProfile();
    this.factory = GLDrawableFactoryImpl.getFactoryImpl(glProfile);
    this.chooser = ((chooser != null) ? chooser : new DefaultGLCapabilitiesChooser());
    this.shareWith = shareWith;
    
    this.setFocusable(true); // allow keyboard input!
  }

  @Override
  public final Object getUpstreamWidget() {
    return this;
  }
  
  @Override
  public void display() {
    if( isVisible() ) {
        if (EventQueue.isDispatchThread()) {
          // Want display() to be synchronous, so call paintImmediately()
          paintImmediately(0, 0, getWidth(), getHeight());
        } else {
          // Multithreaded redrawing of Swing components is not allowed,
          // so do everything on the event dispatch thread
          try {
            EventQueue.invokeAndWait(paintImmediatelyAction);
          } catch (Exception e) {
            throw new GLException(e);
          }
        }
    }
  }

  protected void dispose() {
    if(DEBUG) {
        System.err.println(getThreadName()+": GLJPanel.dispose() - start");
        // Thread.dumpStack();
    }

    if (backend != null && backend.getContext() != null) {
      boolean animatorPaused = false;
      GLAnimatorControl animator =  getAnimator();
      if(null!=animator) {
        animatorPaused = animator.pause();
      }

      if(backend.getContext().isCreated()) {
          Threading.invoke(true, disposeAction, getTreeLock());
      }
      if(null != backend) {
          // not yet destroyed due to backend.isUsingOwnThreadManagment() == true
          backend.destroy();
          isInitialized = false;
      }

      if(animatorPaused) {
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

    if (backend == null || !isInitialized) {
      createAndInitializeBackend();
    }

    if (!isInitialized) {
      return;
    }

    // NOTE: must do this when the context is not current as it may
    // involve destroying the pbuffer (current context) and
    // re-creating it -- tricky to do properly while the context is
    // current
    if (handleReshape) {
      handleReshape = false;
      sendReshape = handleReshape();
    }

    if( isVisible() ) {
        updater.setGraphics(g);
    
        backend.doPaintComponent(g);
    }
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

    dispose();
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
  public void reshape(int x, int y, int width, int height) {
    super.reshape(x, y, width, height);

    if (DEBUG) {
        System.err.println(getThreadName()+": GLJPanel.reshape: " +reshapeWidth+"x"+reshapeHeight + " -> " + width+"x"+height);
    }    
    // reshapeX = x;
    // reshapeY = y;
    reshapeWidth = width;
    reshapeHeight = height;
    handleReshape = true;
  }

  @Override
  public void setOpaque(boolean opaque) {
    if (backend != null) {
      backend.setOpaque(opaque);
    }
    super.setOpaque(opaque);
  }

  @Override
  public void addGLEventListener(GLEventListener listener) {
    helper.addGLEventListener(listener);
  }

  @Override
  public void addGLEventListener(int index, GLEventListener listener) {
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
    if (EventQueue.isDispatchThread()) {
      r.run();
    } else {
      // Multithreaded redrawing of Swing components is not allowed,
      // so do everything on the event dispatch thread
      try {
        EventQueue.invokeAndWait(r);
      } catch (Exception e) {
        throw new GLException(e);
      }
    }
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
      return helper.setExclusiveContextThread(t, getContext());
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
  public GLContext createContext(GLContext shareWith) {
    final Backend b = backend;
    if ( null == b ) {
        return null;
    }
    return b.createContext(shareWith);
  }

  @Override
  public void setRealized(boolean realized) {
  }

  @Override
  public boolean isRealized() {
      return isInitialized;
  }

  @Override
  public GLContext setContext(GLContext newCtx, boolean destroyPrevCtx) {
    final Backend b = backend;
    if ( null == b ) {
        return null;
    }
    final GLContext oldCtx = b.getContext();
    GLDrawableHelper.switchContext(b.getDrawable(), oldCtx, destroyPrevCtx, newCtx, additionalCtxCreationFlags);
    b.setContext(newCtx);
    return oldCtx;
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
    GLContext context = getContext();
    return (context == null) ? null : context.getGL();
  }

  @Override
  public GL setGL(GL gl) {
    GLContext context = getContext();
    if (context != null) {
      context.setGL(gl);
      return gl;
    }
    return null;
  }

  @Override
  public void setAutoSwapBufferMode(boolean onOrOff) {
    // In the current implementation this is a no-op. Both the pbuffer
    // and pixmap based rendering paths use a single-buffered surface
    // so swapping the buffers doesn't do anything. We also don't
    // currently have the provision to skip copying the data to the
    // Swing portion of the GLJPanel in any of the rendering paths.
  }

  @Override
  public boolean getAutoSwapBufferMode() {
    // In the current implementation this is a no-op. Both the pbuffer
    // and pixmap based rendering paths use a single-buffered surface
    // so swapping the buffers doesn't do anything. We also don't
    // currently have the provision to skip copying the data to the
    // Swing portion of the GLJPanel in any of the rendering paths.
    return true;
  }

  @Override
  public void swapBuffers() {
    // In the current implementation this is a no-op. Both the pbuffer
    // and pixmap based rendering paths use a single-buffered surface
    // so swapping the buffers doesn't do anything. We also don't
    // currently have the provision to skip copying the data to the
    // Swing portion of the GLJPanel in any of the rendering paths.
  }

  @Override
  public void setContextCreationFlags(int flags) {
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
    return oglPipelineUsable;
  }

  @Override
  public boolean isGLOriented() {
    final Backend b = backend;
    if ( null == b ) {
        return true;
    }
    return b.getDrawable().isGLOriented();
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
  public final GLProfile getGLProfile() {
    return glProfile;
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

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void createAndInitializeBackend() {
    if ( 0 >= panelWidth || 0 >= panelHeight ) {
      // See whether we have a non-zero size yet and can go ahead with
      // initialization
      if (0 >= reshapeWidth || 0 >= reshapeHeight ) {
          return;
      }

      if (DEBUG) {
          System.err.println(getThreadName()+": GLJPanel.createAndInitializeBackend: " +panelWidth+"x"+panelHeight + " -> " + reshapeWidth+"x"+reshapeHeight);
      }      
      // Pull down reshapeWidth and reshapeHeight into panelWidth and
      // panelHeight eagerly in order to complete initialization, and
      // force a reshape later
      panelWidth = reshapeWidth;
      panelHeight = reshapeHeight;
    }

    if ( null == backend ) {
        if (oglPipelineUsable) {
            backend = new J2DOGLBackend();
        } else {
            backend = new OffscreenBackend();
        }
        isInitialized = false;
    }

    if (!isInitialized) {
        backend.initialize();
    }
  }

  @Override
  public WindowClosingMode getDefaultCloseOperation() {
      return awtWindowClosingProtocol.getDefaultCloseOperation();
  }

  @Override
  public WindowClosingMode setDefaultCloseOperation(WindowClosingMode op) {
      return awtWindowClosingProtocol.setDefaultCloseOperation(op);
  }

  private boolean handleReshape() {
    if (DEBUG) {
      System.err.println(getThreadName()+": GLJPanel.handleReshape: " +panelWidth+"x"+panelHeight + " -> " + reshapeWidth+"x"+reshapeHeight);
    }    
    panelWidth  = reshapeWidth;
    panelHeight = reshapeHeight;

    return backend.handleReshape();
  }

  // This is used as the GLEventListener for the pbuffer-based backend
  // as well as the callback mechanism for the other backends
  class Updater implements GLEventListener {
    private Graphics g;

    public void setGraphics(Graphics g) {
      this.g = g;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
      if (!backend.preGL(g)) {
        return;
      }
      helper.init(GLJPanel.this, !sendReshape);
      backend.postGL(g, false);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
      helper.disposeAllGLEventListener(GLJPanel.this, false);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
      if (!backend.preGL(g)) {
        return;
      }
      if (sendReshape) {
        if (DEBUG) {
          System.err.println(getThreadName()+": GLJPanel.display: reshape(" + viewportX + "," + viewportY + " " + panelWidth + "x" + panelHeight + ")");
        }
        helper.reshape(GLJPanel.this, viewportX, viewportY, panelWidth, panelHeight);
        sendReshape = false;
      }

      helper.display(GLJPanel.this);
      backend.postGL(g, true);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      // This is handled above and dispatched directly to the appropriate context
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
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
        if ( null != backend ) {
            final GLContext _context = backend.getContext();
            final boolean backendDestroy = !backend.isUsingOwnLifecycle();
            if( null != _context && _context.isCreated() ) {
                // Catch dispose GLExceptions by GLEventListener, just 'print' them
                // so we can continue with the destruction.
                try {
                    helper.disposeGL(GLJPanel.this, _context, !backendDestroy);
                } catch (GLException gle) {
                    gle.printStackTrace();
                }
            }
            if ( backendDestroy ) {
                backend.destroy();
                backend = null;
                isInitialized = false;
            }
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

  private final Runnable paintImmediatelyAction = new Runnable() {
    @Override
    public void run() {
      paintImmediately(0, 0, getWidth(), getHeight());
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
          final Backend b = backend;
          if ( null != b ) {
              listener = helper.disposeGLEventListener(GLJPanel.this, b.getDrawable(), b.getContext(), listener, remove);
          }
      }
  };
  
  private int getGLInteger(GL gl, int which) {
    int[] tmp = new int[1];
    gl.glGetIntegerv(which, tmp, 0);
    return tmp[0];
  }

  protected static String getThreadName() { return Thread.currentThread().getName(); }

  //----------------------------------------------------------------------
  // Implementations of the various backends
  //

  // Abstraction of the different rendering backends: i.e., pure
  // software / pixmap rendering, pbuffer-based acceleration, Java 2D
  // / JOGL bridge
  static interface Backend {
    // Create, Destroy, ..
    public boolean isUsingOwnLifecycle();

    // Called each time the backend needs to initialize itself
    public void initialize();

    // Called when the backend should clean up its resources
    public void destroy();

    // Called when the opacity of the GLJPanel is changed
    public void setOpaque(boolean opaque);

    // Called to manually create an additional OpenGL context against
    // this GLJPanel
    public GLContext createContext(GLContext shareWith);

    // Called to set the current backend's GLContext
    public void setContext(GLContext ctx);

    // Called to get the current backend's GLContext
    public GLContext getContext();

    // Called to get the current backend's GLDrawable
    public GLDrawable getDrawable();

    // Called to fetch the "real" GLCapabilities for the backend
    public GLCapabilitiesImmutable getChosenGLCapabilities();

    // Called to fetch the "real" GLProfile for the backend
    public GLProfile getGLProfile();

    // Called to handle a reshape event. When this is called, the
    // OpenGL context associated with the backend is not current, to
    // make it easier to destroy and re-create pbuffers if necessary.
    public boolean handleReshape();

    // Called before the OpenGL work is done in init() and display().
    // If false is returned, this render is aborted.
    public boolean preGL(Graphics g);

    // Called after the OpenGL work is done in init() and display().
    // The isDisplay argument indicates whether this was called on
    // behalf of a call to display() rather than init().
    public void postGL(Graphics g, boolean isDisplay);

    // Called from within paintComponent() to initiate the render
    public void doPaintComponent(Graphics g);
  }

  // Base class used by both the software (pixmap) and pbuffer
  // backends, both of which rely on reading back the OpenGL frame
  // buffer and drawing it with a BufferedImage
  class OffscreenBackend implements Backend {
    protected AWTPixelBufferProviderInt pixelBufferProvider = new AWTPixelBufferProviderInt();
    private PixelAttributes pixelAttribs;
    
    // One of these is used to store the read back pixels before storing
    // in the BufferedImage
    protected IntBuffer             readBackInts;
    protected int                   readBackWidthInPixels;
    protected int                   readBackHeightInPixels;

    // Implementation using software rendering
    private GLDrawableImpl offscreenDrawable;
    private FBObject fboFlipped;
    private GLSLTextureRaster glslTextureRaster;
    private final int fboTextureUnit = 0;
    
    private GLContextImpl offscreenContext;
    private boolean flipVertical;          
    
    // For saving/restoring of OpenGL state during ReadPixels
    private final GLPixelStorageModes psm =  new GLPixelStorageModes();

    @Override
    public boolean isUsingOwnLifecycle() { return false; }

    @Override
    public void initialize() {
      if(DEBUG) {
          System.err.println(getThreadName()+": OffscreenBackend: initialize()");
      }
      try {
          offscreenDrawable = (GLDrawableImpl) factory.createOffscreenDrawable(
                                                    null /* default platform device */,
                                                    offscreenCaps,
                                                    chooser,
                                                    panelWidth, panelHeight);
          offscreenDrawable.setRealized(true);
          offscreenContext = (GLContextImpl) offscreenDrawable.createContext(shareWith);
          offscreenContext.setContextCreationFlags(additionalCtxCreationFlags);
          if( GLContext.CONTEXT_NOT_CURRENT < offscreenContext.makeCurrent() ) {
              isInitialized = true;
              final GL gl = offscreenContext.getGL();
              flipVertical = offscreenDrawable.isGLOriented();
              final GLCapabilitiesImmutable chosenCaps = offscreenDrawable.getChosenGLCapabilities();
              if( USE_GLSL_TEXTURE_RASTERIZER && chosenCaps.isFBO() && flipVertical && gl.isGL2ES2() ) {
                  final boolean _autoSwapBufferMode = helper.getAutoSwapBufferMode();
                  helper.setAutoSwapBufferMode(false);
                  final GLFBODrawable fboDrawable = (GLFBODrawable) offscreenDrawable;
                  try {
                      fboFlipped = new FBObject();
                      fboFlipped.reset(gl, fboDrawable.getWidth(), fboDrawable.getHeight(), 0, false);
                      fboFlipped.attachTexture2D(gl, 0, chosenCaps.getAlphaBits()>0);
                      // fboFlipped.attachRenderbuffer(gl, Attachment.Type.DEPTH, 24);
                      glslTextureRaster = new GLSLTextureRaster(fboTextureUnit, true);
                      glslTextureRaster.init(gl.getGL2ES2());
                      glslTextureRaster.reshape(gl.getGL2ES2(), 0, 0, fboDrawable.getWidth(), fboDrawable.getHeight());
                  } catch (Exception ex) {
                      ex.printStackTrace();
                      if(null != glslTextureRaster) {
                        glslTextureRaster.dispose(gl.getGL2ES2());
                        glslTextureRaster = null;
                      }
                      if(null != fboFlipped) {
                        fboFlipped.destroy(gl);
                        fboFlipped = null;
                      }
                      helper.setAutoSwapBufferMode(_autoSwapBufferMode);
                  }
              } else {
                  fboFlipped = null;
                  glslTextureRaster = null;
              }          
              offscreenContext.release();
          } else {
              isInitialized = false;
          }          
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
      }
    }

    @Override
    public void destroy() {
      if(DEBUG) {
          System.err.println(getThreadName()+": OffscreenBackend: destroy() - offscreenContext: "+(null!=offscreenContext)+" - offscreenDrawable: "+(null!=offscreenDrawable));
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
    }

    @Override
    public void setOpaque(boolean opaque) {
      if (opaque != isOpaque()) {
          pixelBufferProvider.dispose();
      }
    }

    @Override
    public boolean preGL(Graphics g) {
      // Empty in this implementation
      return true;
    }

    @Override
    public void postGL(Graphics g, boolean isDisplay) {
      if (isDisplay) {
        final GL gl = offscreenContext.getGL();

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
        
        // Must now copy pixels from offscreen context into surface
        if ( !pixelBufferProvider.hasImage() ) {
          if (0 >= panelWidth || 0 >= panelHeight ) {
              return;
          }
          
          pixelAttribs = pixelBufferProvider.getAttributes(gl, componentCount);
          
          final int[] tmp = { 0 };
          final int readPixelSize = GLBuffers.sizeof(gl, tmp, pixelAttribs.format, pixelAttribs.type, panelWidth, panelHeight, 1, true);
          final IntBuffer intBuffer = (IntBuffer) pixelBufferProvider.allocate(panelWidth, panelHeight, readPixelSize); 
          if(!flipVertical || null != glslTextureRaster) {
              readBackInts = intBuffer;
          } else {
              readBackInts = IntBuffer.allocate(readBackWidthInPixels * readBackHeightInPixels);
          }
          if(DEBUG) {
              final BufferedImage offscreenImage = pixelBufferProvider.getImage();
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0: pixelAttribs "+pixelAttribs);
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0: flippedVertical "+flipVertical+", glslTextureRaster "+(null!=glslTextureRaster));
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0: panelSize "+panelWidth+"x"+panelHeight +", readBackSizeInPixels "+readBackWidthInPixels+"x"+readBackHeightInPixels);
              System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL.0: offscreenImage "+offscreenImage.getWidth()+"x"+offscreenImage.getHeight());
          }
        }

        if( DEBUG_VIEWPORT ) {
            int[] vp = new int[] { 0, 0, 0, 0 };
            gl.glGetIntegerv(GL.GL_VIEWPORT, vp, 0);
            System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.postGL: Viewport: "+vp[0]+"/"+vp[1]+" "+vp[2]+"x"+vp[3]);
        }
        
        // Save current modes
        psm.setAlignment(gl, alignment, alignment);
        if(gl.isGL2GL3()) {
            final GL2GL3 gl2gl3 = gl.getGL2GL3();
            gl2gl3.glPixelStorei(GL2GL3.GL_PACK_ROW_LENGTH, readBackWidthInPixels);
            gl2gl3.glReadBuffer(gl2gl3.getDefaultReadBuffer());
        }

        if(null != glslTextureRaster) { // implies flippedVertical
            // perform vert-flipping via OpenGL/FBO        
            final GLFBODrawable fboDrawable = (GLFBODrawable)offscreenDrawable;
            final FBObject.TextureAttachment fboTex = fboDrawable.getTextureBuffer(GL.GL_FRONT);
            
            fboDrawable.swapBuffers();
            fboFlipped.bind(gl);
            
            // gl.glActiveTexture(fboDrawable.getTextureUnit()); // implicit!
            gl.glBindTexture(GL.GL_TEXTURE_2D, fboTex.getName());
            // gl.glClear(GL.GL_DEPTH_BUFFER_BIT); // fboFlipped runs w/o DEPTH!
            glslTextureRaster.display(gl.getGL2ES2());
            gl.glReadPixels(0, 0, readBackWidthInPixels, readBackHeightInPixels, pixelAttribs.format, pixelAttribs.type, readBackInts);

            fboFlipped.unbind(gl);
        } else {
            gl.glReadPixels(0, 0, readBackWidthInPixels, readBackHeightInPixels, pixelAttribs.format, pixelAttribs.type, readBackInts);
            
            if ( flipVertical ) {
                // Copy temporary data into raster of BufferedImage for faster
                // blitting Note that we could avoid this copy in the cases
                // where !offscreenContext.offscreenImageNeedsVerticalFlip(),
                // but that's the software rendering path which is very slow
                // anyway
                final BufferedImage offscreenImage = pixelBufferProvider.getImage();
                final Object src = readBackInts.array();
                final Object dest = ((DataBufferInt) offscreenImage.getRaster().getDataBuffer()).getData();
                final int srcIncr = readBackWidthInPixels;
                final int destIncr = offscreenImage.getWidth();
                int srcPos = 0;
                int destPos = (offscreenImage.getHeight() - 1) * destIncr;
                for (; destPos >= 0; srcPos += srcIncr, destPos -= destIncr) {
                  System.arraycopy(src, srcPos, dest, destPos, destIncr);
                }
            }
        }

        // Restore saved modes.
        psm.restore(gl);

        // Note: image will be drawn back in paintComponent() for
        // correctness on all platforms
      }
    }

    @Override
    public void doPaintComponent(Graphics g) {
      helper.invokeGL(offscreenDrawable, offscreenContext, updaterDisplayAction, updaterInitAction);
      
      final BufferedImage offscreenImage = pixelBufferProvider.getImage();
      if ( null != offscreenImage ) {
        // Draw resulting image in one shot
        g.drawImage(offscreenImage, 0, 0,
                    offscreenImage.getWidth(),
                    offscreenImage.getHeight(),
                    null /* Null ImageObserver since image data is ready. */);
      }
    }

    @Override
    public boolean handleReshape() {
        /** FIXME: Shall we utilize such resize optimization (snippet kept alive from removed pbuffer backend) ?
        // Use factor larger than 2 during shrinks for some hysteresis
        float shrinkFactor = 2.5f;
        if ( (panelWidth > readBackWidthInPixels)                  || (panelHeight > readBackHeightInPixels) ||
             (panelWidth < (readBackWidthInPixels / shrinkFactor)) || (panelHeight < (readBackHeightInPixels / shrinkFactor))) {
            if (DEBUG) {
                System.err.println(getThreadName()+": Resizing offscreen from (" + readBackWidthInPixels + ", " + readBackHeightInPixels + ") " +
                        " to fit (" + panelWidth + ", " + panelHeight + ")");
            }
        } */
        
        GLDrawableImpl _drawable = offscreenDrawable;
        {
            final GLDrawableImpl _drawableNew = GLDrawableHelper.resizeOffscreenDrawable(_drawable, offscreenContext, panelWidth, panelHeight);
            if(_drawable != _drawableNew) {
                // write back 
                _drawable = _drawableNew;
                offscreenDrawable = _drawableNew;
            }
        }
        if (DEBUG) {
            System.err.println(getThreadName()+": GLJPanel.OffscreenBackend.handleReshape: " +panelWidth+"x"+panelHeight + " -> " + _drawable.getWidth()+"x"+_drawable.getHeight());
        }
        panelWidth = _drawable.getWidth();
        panelHeight = _drawable.getHeight();
        readBackWidthInPixels = panelWidth;
        readBackHeightInPixels = panelHeight;
        
        if( null != glslTextureRaster ) {
            if( GLContext.CONTEXT_NOT_CURRENT < offscreenContext.makeCurrent() ) {            
                try {
                    final GL gl = offscreenContext.getGL();
                    fboFlipped.reset(gl, _drawable.getWidth(), _drawable.getHeight(), 0, false);
                    glslTextureRaster.reshape(gl.getGL2ES2(), 0, 0, _drawable.getWidth(), _drawable.getHeight());
                } finally {
                    offscreenContext.release();
                }
            }
        }

        pixelBufferProvider.dispose();
        return _drawable.isRealized();
    }
    
    @Override
    public GLContext createContext(GLContext shareWith) {
      return (null != offscreenDrawable) ? offscreenDrawable.createContext(shareWith) : null;
    }

    @Override
    public void setContext(GLContext ctx) {
      offscreenContext=(GLContextImpl)ctx;
    }

    @Override
    public GLContext getContext() {
      return offscreenContext;
    }

    @Override
    public GLDrawable getDrawable() {
        return offscreenDrawable;
    }

    @Override
    public GLCapabilitiesImmutable getChosenGLCapabilities() {
      if (offscreenDrawable == null) {
        return null;
      }
      return offscreenDrawable.getChosenGLCapabilities();
    }

    @Override
    public GLProfile getGLProfile() {
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
    private int[] drawBuffer   = new int[1];
    private int[] readBuffer   = new int[1];
    // This is required when the FBO option of the Java2D / OpenGL
    // pipeline is active
    private int[] frameBuffer  = new int[1];
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
    public boolean isUsingOwnLifecycle() { return true; }

    @Override
    public void initialize() {
      if(DEBUG) {
          System.err.println(getThreadName()+": J2DOGL: initialize()");
      }
      // No-op in this implementation; everything is done lazily
      isInitialized = true;
    }

    @Override
    public void destroy() {
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
    public void setOpaque(boolean opaque) {
      // Empty in this implementation
    }

    @Override
    public GLContext createContext(GLContext shareWith) {
      if(null != shareWith) {
          throw new GLException("J2DOGLBackend cannot create context w/ additional shared context, since it already needs to share the context w/ J2D.");
      }
      return (null != joglDrawable && null != j2dContext) ? joglDrawable.createContext(j2dContext) : null;
    }

    @Override
    public void setContext(GLContext ctx) {
        joglContext=ctx;
    }

    @Override
    public GLContext getContext() {
      return joglContext;
    }

    @Override
    public GLDrawable getDrawable() {
        return joglDrawable;
    }

    @Override
    public GLCapabilitiesImmutable getChosenGLCapabilities() {
      // FIXME: should do better than this; is it possible to using only platform-independent code?
      return new GLCapabilities(null);
    }

    @Override
    public GLProfile getGLProfile() {
      // FIXME: should do better than this; is it possible to using only platform-independent code?
      return GLProfile.getDefault(GLProfile.getDefaultDevice());
    }

    @Override
    public boolean handleReshape() {
      // Empty in this implementation
      return true;
    }

    @Override
    public boolean preGL(Graphics g) {
      GL2 gl = joglContext.getGL().getGL2();
      // Set up needed state in JOGL context from Java2D context
      gl.glEnable(GL2.GL_SCISSOR_TEST);
      Rectangle r = Java2D.getOGLScissorBox(g);

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
      Rectangle oglViewport = Java2D.getOGLViewport(g, panelWidth, panelHeight);
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
        int fboTextureTarget = Java2D.getOGLTextureType(g);

        if (!checkedForFBObjectWorkarounds) {
          checkedForFBObjectWorkarounds = true;
          gl.glBindTexture(fboTextureTarget, 0);
          gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBuffer[0]);
          int status = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
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
          int[] width = new int[1];
          int[] height = new int[1];
          gl.glGetTexLevelParameteriv(fboTextureTarget, 0, GL2.GL_TEXTURE_WIDTH, width, 0);
          gl.glGetTexLevelParameteriv(fboTextureTarget, 0, GL2.GL_TEXTURE_HEIGHT, height, 0);

          gl.glGenRenderbuffers(1, frameBufferDepthBuffer, 0);
          if (DEBUG) {
            System.err.println(getThreadName()+": GLJPanel: Generated frameBufferDepthBuffer " + frameBufferDepthBuffer[0] +
                               " with width " + width[0] + ", height " + height[0]);
          }

          gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, frameBufferDepthBuffer[0]);
          // FIXME: may need a loop here like in Java2D
          gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL2GL3.GL_DEPTH_COMPONENT24, width[0], height[0]);

          gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, 0);
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
          int status = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
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
    public void postGL(Graphics g, boolean isDisplay) {
      // Cause OpenGL pipeline to flush its results because
      // otherwise it's possible we will buffer up multiple frames'
      // rendering results, resulting in apparent mouse lag
      GL gl = joglContext.getGL();
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
    public void doPaintComponent(final Graphics g) {
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
              GL gl = j2dContext.getGL();
              if ((getGLInteger(gl, GL.GL_RED_BITS)         < offscreenCaps.getRedBits())        ||
                  (getGLInteger(gl, GL.GL_GREEN_BITS)       < offscreenCaps.getGreenBits())      ||
                  (getGLInteger(gl, GL.GL_BLUE_BITS)        < offscreenCaps.getBlueBits())       ||
                  //                  (getGLInteger(gl, GL.GL_ALPHA_BITS)       < offscreenCaps.getAlphaBits())      ||
                  (getGLInteger(gl, GL2.GL_ACCUM_RED_BITS)   < offscreenCaps.getAccumRedBits())   ||
                  (getGLInteger(gl, GL2.GL_ACCUM_GREEN_BITS) < offscreenCaps.getAccumGreenBits()) ||
                  (getGLInteger(gl, GL2.GL_ACCUM_BLUE_BITS)  < offscreenCaps.getAccumBlueBits())  ||
                  (getGLInteger(gl, GL2.GL_ACCUM_ALPHA_BITS) < offscreenCaps.getAccumAlphaBits()) ||
                  //          (getGLInteger(gl, GL2.GL_DEPTH_BITS)       < offscreenCaps.getDepthBits())      ||
                  (getGLInteger(gl, GL.GL_STENCIL_BITS)     < offscreenCaps.getStencilBits())) {
                if (DEBUG) {
                  System.err.println(getThreadName()+": GLJPanel: Falling back to pbuffer-based support because Java2D context insufficient");
                  System.err.println("                    Available              Required");
                  System.err.println("GL_RED_BITS         " + getGLInteger(gl, GL.GL_RED_BITS)         + "              " + offscreenCaps.getRedBits());
                  System.err.println("GL_GREEN_BITS       " + getGLInteger(gl, GL.GL_GREEN_BITS)       + "              " + offscreenCaps.getGreenBits());
                  System.err.println("GL_BLUE_BITS        " + getGLInteger(gl, GL.GL_BLUE_BITS)        + "              " + offscreenCaps.getBlueBits());
                  System.err.println("GL_ALPHA_BITS       " + getGLInteger(gl, GL.GL_ALPHA_BITS)       + "              " + offscreenCaps.getAlphaBits());
                  System.err.println("GL_ACCUM_RED_BITS   " + getGLInteger(gl, GL2.GL_ACCUM_RED_BITS)   + "              " + offscreenCaps.getAccumRedBits());
                  System.err.println("GL_ACCUM_GREEN_BITS " + getGLInteger(gl, GL2.GL_ACCUM_GREEN_BITS) + "              " + offscreenCaps.getAccumGreenBits());
                  System.err.println("GL_ACCUM_BLUE_BITS  " + getGLInteger(gl, GL2.GL_ACCUM_BLUE_BITS)  + "              " + offscreenCaps.getAccumBlueBits());
                  System.err.println("GL_ACCUM_ALPHA_BITS " + getGLInteger(gl, GL2.GL_ACCUM_ALPHA_BITS) + "              " + offscreenCaps.getAccumAlphaBits());
                  System.err.println("GL_DEPTH_BITS       " + getGLInteger(gl, GL.GL_DEPTH_BITS)       + "              " + offscreenCaps.getDepthBits());
                  System.err.println("GL_STENCIL_BITS     " + getGLInteger(gl, GL.GL_STENCIL_BITS)     + "              " + offscreenCaps.getStencilBits());
                }
                isInitialized = false;
                backend = null;
                oglPipelineUsable = false;
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
              Object curSurface = Java2D.getOGLSurfaceIdentifier(g);
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
                      int surfaceType = Java2D.getOGLSurfaceType(g);
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
                  AbstractGraphicsDevice device = j2dContext.getGLDrawable().getNativeSurface().getGraphicsConfiguration().getScreen().getDevice();
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

    private void captureJ2DState(GL gl, Graphics g) {
      gl.glGetIntegerv(GL2.GL_DRAW_BUFFER, drawBuffer, 0);
      gl.glGetIntegerv(GL2.GL_READ_BUFFER, readBuffer, 0);
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
          String vendor = gl.glGetString(GL.GL_VENDOR);

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

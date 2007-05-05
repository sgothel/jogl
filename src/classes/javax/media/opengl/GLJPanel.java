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

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.beans.*;
import javax.swing.*;
import java.nio.*;
import java.security.*;
import javax.swing.JComponent;
import javax.swing.JPanel;
import com.sun.opengl.impl.*;

// FIXME: Subclasses need to call resetGLFunctionAvailability() on their
// context whenever the displayChanged() function is called on their
// GLEventListeners

/** A lightweight Swing component which provides OpenGL rendering
    support. Provided for compatibility with Swing user interfaces
    when adding a heavyweight doesn't work either because of
    Z-ordering or LayoutManager problems. <P>

    The GLJPanel can be made transparent by creating it with a
    GLCapabilities object with alpha bits specified and calling {@link
    #setOpaque}(false). Pixels with resulting OpenGL alpha values less
    than 1.0 will be overlaid on any underlying Swing rendering. <P>

    Notes specific to the Reference Implementation: This component 
    attempts to use hardware-accelerated rendering via pbuffers and 
    falls back on to software rendering if problems occur. 
    Note that because this component attempts to use pbuffers for
    rendering, and because pbuffers can not be resized, somewhat
    surprising behavior may occur during resize operations; the {@link
    GLEventListener#init} method may be called multiple times as the
    pbuffer is resized to be able to cover the size of the GLJPanel.
    This behavior is correct, as the textures and display lists for
    the GLJPanel will have been lost during the resize operation. The
    application should attempt to make its GLEventListener.init()
    methods as side-effect-free as possible. <P>

*/

public class GLJPanel extends JPanel implements GLAutoDrawable {
  private static final boolean DEBUG = Debug.debug("GLJPanel");
  private static final boolean VERBOSE = Debug.verbose();

  private GLDrawableHelper drawableHelper = new GLDrawableHelper();
  private volatile boolean isInitialized;
  private volatile boolean shouldInitialize = false;

  // Data used for either pbuffers or pixmap-based offscreen surfaces
  private GLCapabilities        offscreenCaps;
  private GLCapabilitiesChooser chooser;
  private GLContext             shareWith;
  // This image is exactly the correct size to render into the panel
  private BufferedImage         offscreenImage;
  // One of these is used to store the read back pixels before storing
  // in the BufferedImage
  private ByteBuffer            readBackBytes;
  private IntBuffer             readBackInts;
  private int                   readBackWidthInPixels;
  private int                   readBackHeightInPixels;
  // Width of the actual GLJPanel
  private int panelWidth   = 0;
  private int panelHeight  = 0;
  private Updater updater;
  private int awtFormat;
  private int glFormat;
  private int glType;
  // Lazy reshape notification
  private boolean handleReshape = false;
  private boolean sendReshape = true;

  // Implementation using pbuffers
  private static boolean hardwareAccelerationDisabled =
    Debug.isPropertyDefined("jogl.gljpanel.nohw");
  private static boolean softwareRenderingDisabled =
    Debug.isPropertyDefined("jogl.gljpanel.nosw");
  private GLPbuffer pbuffer;
  private int       pbufferWidth  = 256;
  private int       pbufferHeight = 256;

  // Implementation using software rendering
  private GLDrawableImpl offscreenDrawable;
  private GLContextImpl offscreenContext;

  // For handling reshape events lazily
  private int reshapeX;
  private int reshapeY;
  private int reshapeWidth;
  private int reshapeHeight;

  // For saving/restoring of OpenGL state during ReadPixels
  private int[] swapbytes    = new int[1];
  private int[] rowlength    = new int[1];
  private int[] skiprows     = new int[1];
  private int[] skippixels   = new int[1];
  private int[] alignment    = new int[1];

  // Implementation using Java2D OpenGL pipeline's back buffer
  private boolean oglPipelineEnabled =
    Java2D.isOGLPipelineActive() &&
    !Debug.isPropertyDefined("jogl.gljpanel.noogl");
  // Opaque Object identifier representing the Java2D surface we are
  // drawing to; used to determine when to destroy and recreate JOGL
  // context
  private Object j2dSurface;
  // Graphics object being used during Java2D update action
  // (absolutely essential to cache this)
  private Graphics cached2DGraphics;
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

  // These are always set to (0, 0) except when the Java2D / OpenGL
  // pipeline is active
  private int   viewportX;
  private int   viewportY;

  static {
    // Force eager initialization of part of the Java2D class since
    // otherwise it's likely it will try to be initialized while on
    // the Queue Flusher Thread, which is not allowed
    if (Java2D.isOGLPipelineActive() && Java2D.isFBOEnabled()) {
      Java2D.getShareContext(GraphicsEnvironment.
                             getLocalGraphicsEnvironment().
                             getDefaultScreenDevice().
                             getDefaultConfiguration());
    }
  }

  /** Creates a new GLJPanel component with a default set of OpenGL
      capabilities and using the default OpenGL capabilities selection
      mechanism. */
  public GLJPanel() {
    this(null);
  }

  /** Creates a new GLJPanel component with the requested set of
      OpenGL capabilities, using the default OpenGL capabilities
      selection mechanism. */
  public GLJPanel(GLCapabilities capabilities) {
    this(capabilities, null, null);
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
  */
  public GLJPanel(GLCapabilities capabilities, GLCapabilitiesChooser chooser, GLContext shareWith) {
    super();

    // Works around problems on many vendors' cards; we don't need a
    // back buffer for the offscreen surface anyway
    if (capabilities != null) {
        offscreenCaps = (GLCapabilities) capabilities.clone();
    } else {
        offscreenCaps = new GLCapabilities();
    }
    offscreenCaps.setDoubleBuffered(false);
    this.chooser = ((chooser != null) ? chooser : new DefaultGLCapabilitiesChooser());
    this.shareWith = shareWith;
  }

  public void display() {
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

  private void captureJ2DState(GL gl, Graphics g) {
    gl.glGetIntegerv(GL.GL_DRAW_BUFFER, drawBuffer, 0);
    gl.glGetIntegerv(GL.GL_READ_BUFFER, readBuffer, 0);
    if (Java2D.isFBOEnabled() &&
        Java2D.getOGLSurfaceType(g) == Java2D.FBOBJECT) {
      if (DEBUG && VERBOSE) {
        System.err.println("GLJPanel: Fetching GL_FRAMEBUFFER_BINDING_EXT");
      }
      gl.glGetIntegerv(GL.GL_FRAMEBUFFER_BINDING_EXT, frameBuffer, 0);

      if (fbObjectWorkarounds ||
          !checkedForFBObjectWorkarounds) {
        // See above for description of what we are doing here
        if (frameBufferTexture == null)
          frameBufferTexture = new int[1];

        // Query the framebuffer for its color buffer so we can hook
        // it back up in our context (should not be necessary)
        gl.glGetFramebufferAttachmentParameterivEXT(GL.GL_FRAMEBUFFER_EXT,
                                                    GL.GL_COLOR_ATTACHMENT0_EXT,
                                                    GL.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME_EXT,
                                                    frameBufferTexture, 0);
        if (DEBUG && VERBOSE) {
          System.err.println("GLJPanel: FBO COLOR_ATTACHMENT0: " + frameBufferTexture[0]);
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
        gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
      }
    }
  }

  private boolean preGL(Graphics g) {
    GL gl = joglContext.getGL();
    // Set up needed state in JOGL context from Java2D context
    gl.glEnable(GL.GL_SCISSOR_TEST);
    Rectangle r = Java2D.getOGLScissorBox(g);

    if (r == null) {
      if (DEBUG && VERBOSE) {
        System.err.println("Java2D.getOGLScissorBox() returned null");
      }
      return false;
    }
    if (DEBUG && VERBOSE) {
      System.err.println("GLJPanel: gl.glScissor(" + r.x + ", " + r.y + ", " + r.width + ", " + r.height + ")");
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
        System.err.println("Sending reshape because viewport changed");
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
      if (DEBUG && VERBOSE) {
        System.err.println("GLJPanel: Binding to framebuffer object " + frameBuffer[0]);
      }

      // The texture target for Java2D's OpenGL pipeline when using FBOs
      // -- either GL_TEXTURE_2D or GL_TEXTURE_RECTANGLE_ARB
      int fboTextureTarget = Java2D.getOGLTextureType(g);

      if (!checkedForFBObjectWorkarounds) {
        checkedForFBObjectWorkarounds = true;
        gl.glBindTexture(fboTextureTarget, 0);
        gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, frameBuffer[0]);
        if (gl.glCheckFramebufferStatusEXT(GL.GL_FRAMEBUFFER_EXT) !=
            GL.GL_FRAMEBUFFER_COMPLETE_EXT) {
          // Need to do workarounds
          fbObjectWorkarounds = true;
          createNewDepthBuffer = true;
          if (DEBUG) {
            System.err.println("-- GLJPanel: discovered frame_buffer_object workarounds to be necessary");
          }
        } else {
          // Don't need the frameBufferTexture temporary any more
          frameBufferTexture = null;
        }
      }

      if (fbObjectWorkarounds && createNewDepthBuffer) {
        if (frameBufferDepthBuffer == null)
          frameBufferDepthBuffer = new int[1];

        // Create our own depth renderbuffer and associated storage
        // If we have an old one, delete it
        if (frameBufferDepthBuffer[0] != 0) {
          gl.glDeleteRenderbuffersEXT(1, frameBufferDepthBuffer, 0);
          frameBufferDepthBuffer[0] = 0;
        }

        gl.glBindTexture(fboTextureTarget, frameBufferTexture[0]);
        int[] width = new int[1];
        int[] height = new int[1];
        gl.glGetTexLevelParameteriv(fboTextureTarget, 0, GL.GL_TEXTURE_WIDTH, width, 0);
        gl.glGetTexLevelParameteriv(fboTextureTarget, 0, GL.GL_TEXTURE_HEIGHT, height, 0);
                    
        gl.glGenRenderbuffersEXT(1, frameBufferDepthBuffer, 0);
        if (DEBUG) {
          System.err.println("GLJPanel: Generated frameBufferDepthBuffer " + frameBufferDepthBuffer[0] +
                             " with width " + width[0] + ", height " + height[0]);
        }
                    
        gl.glBindRenderbufferEXT(GL.GL_RENDERBUFFER_EXT, frameBufferDepthBuffer[0]);
        // FIXME: may need a loop here like in Java2D
        gl.glRenderbufferStorageEXT(GL.GL_RENDERBUFFER_EXT, GL.GL_DEPTH_COMPONENT24, width[0], height[0]);

        gl.glBindRenderbufferEXT(GL.GL_RENDERBUFFER_EXT, 0);
        createNewDepthBuffer = false;
      }

      gl.glBindTexture(fboTextureTarget, 0);
      gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, frameBuffer[0]);

      if (fbObjectWorkarounds) {
        // Hook up the color and depth buffer attachment points for this framebuffer
        gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT,
                                     GL.GL_COLOR_ATTACHMENT0_EXT,
                                     fboTextureTarget,
                                     frameBufferTexture[0],
                                     0);
        if (DEBUG && VERBOSE) {
          System.err.println("GLJPanel: frameBufferDepthBuffer: " + frameBufferDepthBuffer[0]);
        }
        gl.glFramebufferRenderbufferEXT(GL.GL_FRAMEBUFFER_EXT,
                                        GL.GL_DEPTH_ATTACHMENT_EXT,
                                        GL.GL_RENDERBUFFER_EXT,
                                        frameBufferDepthBuffer[0]);
      }

      if (DEBUG) {
        int status = gl.glCheckFramebufferStatusEXT(GL.GL_FRAMEBUFFER_EXT);
        if (status != GL.GL_FRAMEBUFFER_COMPLETE_EXT) {
          throw new GLException("Error: framebuffer was incomplete: status = 0x" +
                                Integer.toHexString(status));
        }
      }
    } else {
      if (DEBUG && VERBOSE) {
        System.err.println("GLJPanel: Setting up drawBuffer " + drawBuffer[0] +
                           " and readBuffer " + readBuffer[0]);
      }

      gl.glDrawBuffer(drawBuffer[0]);
      gl.glReadBuffer(readBuffer[0]);
    }

    return true;
  }

  private void postGL(Graphics g) {
    if (Java2D.isFBOEnabled() &&
        Java2D.getOGLSurfaceType(g) == Java2D.FBOBJECT) {
      // Unbind the framebuffer from our context to work around
      // apparent driver bugs or at least unspecified behavior causing
      // OpenGL to run out of memory with certain cards and drivers
      GL gl = joglContext.getGL();
      gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
    }
  }

  /** Overridden to cause OpenGL rendering to be performed during
      repaint cycles. Subclasses which override this method must call
      super.paintComponent() in their paintComponent() method in order
      to function properly. <P>

      <B>Overrides:</B>
      <DL><DD><CODE>paintComponent</CODE> in class <CODE>javax.swing.JComponent</CODE></DD></DL> */
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

    if (shouldInitialize) {
      initialize();
    }

    if (!isInitialized) {
      return;
    }

    // NOTE: must do this when the context is not current as it may
    // involve destroying the pbuffer (current context) and
    // re-creating it -- tricky to do properly while the context is
    // current
    if (handleReshape) {
      handleReshape();
    }

    updater.setGraphics(g);

    if (oglPipelineEnabled) {

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
        Java2D.invokeWithOGLSharedContextCurrent(workaroundConfig, new Runnable() { public void run() {}});
      }

      Java2D.invokeWithOGLContextCurrent(g, new Runnable() {
          public void run() {
            if (DEBUG && VERBOSE) {
              System.err.println("-- In invokeWithOGLContextCurrent");
            }

            // Create no-op context representing Java2D context
            if (j2dContext == null) {
              j2dContext = GLDrawableFactory.getFactory().createExternalGLContext();
              if (DEBUG) {
                j2dContext.setGL(new DebugGL(j2dContext.getGL()));
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
                  (getGLInteger(gl, GL.GL_ACCUM_RED_BITS)   < offscreenCaps.getAccumRedBits())   ||
                  (getGLInteger(gl, GL.GL_ACCUM_GREEN_BITS) < offscreenCaps.getAccumGreenBits()) ||
                  (getGLInteger(gl, GL.GL_ACCUM_BLUE_BITS)  < offscreenCaps.getAccumBlueBits())  ||
                  (getGLInteger(gl, GL.GL_ACCUM_ALPHA_BITS) < offscreenCaps.getAccumAlphaBits()) ||
                  //          (getGLInteger(gl, GL.GL_DEPTH_BITS)       < offscreenCaps.getDepthBits())      ||
                  (getGLInteger(gl, GL.GL_STENCIL_BITS)     < offscreenCaps.getStencilBits())) {
                if (DEBUG) {
                  System.err.println("GLJPanel: Falling back to pbuffer-based support because Java2D context insufficient");
                  System.err.println("                    Available              Required");
                  System.err.println("GL_RED_BITS         " + getGLInteger(gl, GL.GL_RED_BITS)         + "              " + offscreenCaps.getRedBits());
                  System.err.println("GL_GREEN_BITS       " + getGLInteger(gl, GL.GL_GREEN_BITS)       + "              " + offscreenCaps.getGreenBits());
                  System.err.println("GL_BLUE_BITS        " + getGLInteger(gl, GL.GL_BLUE_BITS)        + "              " + offscreenCaps.getBlueBits());
                  System.err.println("GL_ALPHA_BITS       " + getGLInteger(gl, GL.GL_ALPHA_BITS)       + "              " + offscreenCaps.getAlphaBits());
                  System.err.println("GL_ACCUM_RED_BITS   " + getGLInteger(gl, GL.GL_ACCUM_RED_BITS)   + "              " + offscreenCaps.getAccumRedBits());
                  System.err.println("GL_ACCUM_GREEN_BITS " + getGLInteger(gl, GL.GL_ACCUM_GREEN_BITS) + "              " + offscreenCaps.getAccumGreenBits());
                  System.err.println("GL_ACCUM_BLUE_BITS  " + getGLInteger(gl, GL.GL_ACCUM_BLUE_BITS)  + "              " + offscreenCaps.getAccumBlueBits());
                  System.err.println("GL_ACCUM_ALPHA_BITS " + getGLInteger(gl, GL.GL_ACCUM_ALPHA_BITS) + "              " + offscreenCaps.getAccumAlphaBits());
                  System.err.println("GL_DEPTH_BITS       " + getGLInteger(gl, GL.GL_DEPTH_BITS)       + "              " + offscreenCaps.getDepthBits());
                  System.err.println("GL_STENCIL_BITS     " + getGLInteger(gl, GL.GL_STENCIL_BITS)     + "              " + offscreenCaps.getStencilBits());
                }
                isInitialized = false;
                shouldInitialize = true;
                oglPipelineEnabled = false;
                handleReshape = true;
                j2dContext.release();
                j2dContext.destroy();
                j2dContext = null;
                return;
              }
              j2dContext.release();
            }

            j2dContext.makeCurrent();
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
                      System.err.println("Sending reshape because surface changed");
                      System.err.println("New surface = " + curSurface);
                    }
                  }
                  j2dSurface = curSurface;
                }
                if (joglContext == null) {
                  if (GLDrawableFactory.getFactory().canCreateExternalGLDrawable()) {
                    joglDrawable = GLDrawableFactory.getFactory().createExternalGLDrawable();
                    joglContext = joglDrawable.createContext(shareWith);
                  } else if (GLDrawableFactoryImpl.getFactoryImpl().canCreateContextOnJava2DSurface()) {
                    // Mac OS X code path
                    joglContext = GLDrawableFactoryImpl.getFactoryImpl().createContextOnJava2DSurface(g, shareWith);
                  }
                  if (DEBUG) {
                    joglContext.setGL(new DebugGL(joglContext.getGL()));
                  }

                  if (Java2D.isFBOEnabled() &&
                      Java2D.getOGLSurfaceType(g) == Java2D.FBOBJECT &&
                      fbObjectWorkarounds) {
                    createNewDepthBuffer = true;
                  }
                }
                if (joglContext instanceof Java2DGLContext) {
                  // Mac OS X code path
                  ((Java2DGLContext) joglContext).setGraphics(g);
                }

                if (DEBUG && VERBOSE && Java2D.isFBOEnabled()) {
                  System.err.print("-- Surface type: ");
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

                drawableHelper.invokeGL(joglDrawable, joglContext, displayAction, initAction);
              }
            } finally {
              j2dContext.release();
            }
          }
        });
    } else {
      if (!hardwareAccelerationDisabled) {
        pbuffer.display();
      } else {
        drawableHelper.invokeGL(offscreenDrawable, offscreenContext, displayAction, initAction);
      }

      if (offscreenImage != null) {
        // Draw resulting image in one shot
        g.drawImage(offscreenImage, 0, 0, offscreenImage.getWidth(), offscreenImage.getHeight(), this);
      }
    }
  }

  /** Overridden to track when this component is added to a container.
      Subclasses which override this method must call
      super.addNotify() in their addNotify() method in order to
      function properly. <P>

      <B>Overrides:</B>
      <DL><DD><CODE>addNotify</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
  public void addNotify() {
    super.addNotify();
    shouldInitialize = true;
    if (DEBUG) {
      System.err.println("GLJPanel.addNotify()");
    }
  }

  /** Overridden to track when this component is removed from a
      container. Subclasses which override this method must call
      super.removeNotify() in their removeNotify() method in order to
      function properly. <P>

      <B>Overrides:</B>
      <DL><DD><CODE>removeNotify</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
  public void removeNotify() {
    if (DEBUG) {
      System.err.println("GLJPanel.removeNotify()");
    }
    if (oglPipelineEnabled) {
      Java2D.invokeWithOGLContextCurrent(null, new Runnable() {
          public void run() {
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
    } else {
      if (!hardwareAccelerationDisabled) {
        if (pbuffer != null) {
          pbuffer.destroy();
          pbuffer = null;
        }
      } else {
        if (offscreenContext != null) {
          offscreenContext.destroy();
          offscreenContext = null;
        }
        if (offscreenDrawable != null) {
          offscreenDrawable.destroy();
          offscreenDrawable = null;
        }
      }
    }
    isInitialized = false;
    super.removeNotify();
  }

  /** Overridden to cause {@link GLDrawableHelper#reshape} to be
      called on all registered {@link GLEventListener}s. Subclasses
      which override this method must call super.reshape() in
      their reshape() method in order to function properly. <P>

      <B>Overrides:</B>
      <DL><DD><CODE>reshape</CODE> in class <CODE>java.awt.Component</CODE></DD></DL> */
  public void reshape(int x, int y, int width, int height) {
    super.reshape(x, y, width, height);

    reshapeX = x;
    reshapeY = y;
    reshapeWidth = width;
    reshapeHeight = height;
    handleReshape = true;
  }

  public void setOpaque(boolean opaque) {
    if (opaque != isOpaque()) {
      if (offscreenImage != null) {
        offscreenImage.flush();
        offscreenImage = null;
      }
    }
    super.setOpaque(opaque);
  }

  public void addGLEventListener(GLEventListener listener) {
    drawableHelper.addGLEventListener(listener);
  }

  public void removeGLEventListener(GLEventListener listener) {
    drawableHelper.removeGLEventListener(listener);
  }

  public GLContext createContext(GLContext shareWith) {
    if (!hardwareAccelerationDisabled) {
      return pbuffer.createContext(shareWith);
    } else {
      return offscreenDrawable.createContext(shareWith);
    }
  }

  public void setRealized(boolean realized) {
  }

  public GLContext getContext() {
    if (oglPipelineEnabled) {
      return joglContext;
    } else {
      if (!hardwareAccelerationDisabled) {
        // Workaround for crashes in NetBeans GUI builder
        if (pbuffer == null && Beans.isDesignTime()) {
          return null;
        }
        return pbuffer.getContext();
      } else {
        return offscreenContext;
      }
    }
  }

  public GL getGL() {
    GLContext context = getContext();
    return (context == null) ? null : context.getGL();
  }

  public void setGL(GL gl) {
    GLContext context = getContext();
    if (context != null) {
      context.setGL(gl);
    }
  }

  public void setAutoSwapBufferMode(boolean onOrOff) {
    if (!hardwareAccelerationDisabled) {
      // Workaround for crashes in NetBeans GUI builder
      if (pbuffer == null && Beans.isDesignTime()) {
        return;
      }
      pbuffer.setAutoSwapBufferMode(onOrOff);
    } else {
      drawableHelper.setAutoSwapBufferMode(onOrOff);
    }
  }

  public boolean getAutoSwapBufferMode() {
    if (!hardwareAccelerationDisabled && !oglPipelineEnabled) {
      return pbuffer.getAutoSwapBufferMode();
    } else {
      return drawableHelper.getAutoSwapBufferMode();
    }
  }

  public void swapBuffers() {
    // In the current implementation this is basically a no-op. Both
    // the pbuffer and pixmap based rendering paths use a single-
    // buffered surface so swapping the buffers doesn't do anything.
    // We also don't currently have the provision to skip copying the
    // data to the Swing portion of the GLJPanel in any of the
    // rendering paths.
    if (oglPipelineEnabled) {
      // Do nothing
    } else if (!hardwareAccelerationDisabled) {
      pbuffer.swapBuffers();
    } else {
      drawableHelper.invokeGL(offscreenDrawable, offscreenContext, swapBuffersAction, initAction);
    }
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
    return oglPipelineEnabled;
  }

  public GLCapabilities getChosenGLCapabilities() {
    if (oglPipelineEnabled) {
      // FIXME: should do better than this; is it possible to using only platform-independent code?
      return new GLCapabilities();
    }

    if (hardwareAccelerationDisabled) {
      if (offscreenDrawable != null)
        return offscreenDrawable.getChosenGLCapabilities();
    } else {
      if (pbuffer != null)
        return pbuffer.getChosenGLCapabilities();
    }
    
    return null;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void initialize() {
    if (panelWidth == 0 ||
        panelHeight == 0) {
      // See whether we have a non-zero size yet and can go ahead with
      // initialization
      if (reshapeWidth == 0 ||
          reshapeHeight == 0) {
        return;
      }

      // Pull down reshapeWidth and reshapeHeight into panelWidth and
      // panelHeight eagerly in order to complete initialization, and
      // force a reshape later
      panelWidth = reshapeWidth;
      panelHeight = reshapeHeight;
    }

    if (!oglPipelineEnabled) {
      // Initialize either the hardware-accelerated rendering path or
      // the lightweight rendering path
      if (!hardwareAccelerationDisabled) {
        if (GLDrawableFactory.getFactory().canCreateGLPbuffer()) {
          if (pbuffer != null) {
            throw new InternalError("Creating pbuffer twice without destroying it (memory leak / correctness bug)");
          }
          try {
            pbuffer = GLDrawableFactory.getFactory().createGLPbuffer(offscreenCaps,
                                                                     null,
                                                                     pbufferWidth,
                                                                     pbufferHeight,
                                                                     shareWith);
            updater = new Updater();
            pbuffer.addGLEventListener(updater);
            shouldInitialize = false;
            isInitialized = true;
            return;
          } catch (GLException e) {
            if (DEBUG) {
              e.printStackTrace();
              System.err.println("GLJPanel: Falling back on software rendering because of problems creating pbuffer");
            }
            hardwareAccelerationDisabled = true;
          }
        } else {
          if (DEBUG) {
            System.err.println("GLJPanel: Falling back on software rendering because no pbuffer support");
          }

          // If the factory reports that it can't create a pbuffer,
          // don't try again the next time, and fall through to the
          // software rendering path
          hardwareAccelerationDisabled = true;
        }
      }

      if (softwareRenderingDisabled) {
        throw new GLException("Fallback to software rendering disabled by user");
      }

      // Fall-through path: create an offscreen context instead
      offscreenDrawable = GLDrawableFactoryImpl.getFactoryImpl().createOffscreenDrawable(offscreenCaps, chooser);
      offscreenDrawable.setSize(Math.max(1, panelWidth), Math.max(1, panelHeight));
      offscreenContext = (GLContextImpl) offscreenDrawable.createContext(shareWith);
      offscreenContext.setSynchronized(true);
    }
    updater = new Updater();
    shouldInitialize = false;
    isInitialized = true;
  }

  private void handleReshape() {
    readBackWidthInPixels = 0;
    readBackHeightInPixels = 0;

    panelWidth  = reshapeWidth;
    panelHeight = reshapeHeight;

    if (DEBUG) {
      System.err.println("GLJPanel.handleReshape: (w,h) = (" +
                         panelWidth + "," + panelHeight + ")");
    }

    sendReshape = true;

    if (!oglPipelineEnabled) {
      if (!hardwareAccelerationDisabled) {
        // Use factor larger than 2 during shrinks for some hysteresis
        float shrinkFactor = 2.5f;
        if ((panelWidth > pbufferWidth           )       || (panelHeight > pbufferHeight) ||
            (panelWidth < (pbufferWidth / shrinkFactor)) || (panelHeight < (pbufferWidth / shrinkFactor))) {
          if (DEBUG) {
            System.err.println("Resizing pbuffer from (" + pbufferWidth + ", " + pbufferHeight + ") " +
                               " to fit (" + panelWidth + ", " + panelHeight + ")");
          }
          // Must destroy and recreate pbuffer to fit
          if (pbuffer != null) {
            // Watch for errors during pbuffer destruction (due to
            // buggy / bad OpenGL drivers, in particular SiS) and fall
            // back to software rendering
            try {
              pbuffer.destroy();
            } catch (GLException e) {
              hardwareAccelerationDisabled = true;
              if (DEBUG) {
                System.err.println("WARNING: falling back to software rendering due to bugs in OpenGL drivers");
                e.printStackTrace();
              }
            }
          }
          pbuffer = null;
          isInitialized = false;
          pbufferWidth = getNextPowerOf2(panelWidth);
          pbufferHeight = getNextPowerOf2(panelHeight);
          if (DEBUG && !hardwareAccelerationDisabled) {
            System.err.println("New pbuffer size is (" + pbufferWidth + ", " + pbufferHeight + ")");
          }
          initialize();
        }

        // It looks like NVidia's drivers (at least the ones on my
        // notebook) are buggy and don't allow a rectangle of less than
        // the pbuffer's width to be read...this doesn't really matter
        // because it's the Graphics.drawImage() calls that are the
        // bottleneck. Should probably make the size of the offscreen
        // image be the exact size of the pbuffer to save some work on
        // resize operations...
        if (!hardwareAccelerationDisabled) {
          readBackWidthInPixels  = pbufferWidth;
          readBackHeightInPixels = panelHeight;
        } else {
          // Just disabled hardware acceleration during this resize operation; do a fixup
          readBackWidthInPixels  = Math.max(1, panelWidth);
          readBackHeightInPixels = Math.max(1, panelHeight);
        }
      } else {
        offscreenContext.destroy();
        offscreenDrawable.setSize(Math.max(1, panelWidth), Math.max(1, panelHeight));
        readBackWidthInPixels  = Math.max(1, panelWidth);
        readBackHeightInPixels = Math.max(1, panelHeight);
      }

      if (offscreenImage != null) {
        offscreenImage.flush();
        offscreenImage = null;
      }
    }

    handleReshape = false;
  }

  // FIXME: it isn't clear whether this works any more given that
  // we're accessing the GLDrawable inside of the GLPbuffer directly
  // up in reshape() -- need to rethink and clean this up
  class Updater implements GLEventListener {
    private Graphics g;

    public void setGraphics(Graphics g) {
      this.g = g;
    }

    public void init(GLAutoDrawable drawable) {
      if (oglPipelineEnabled) {
        if (!preGL(g)) {
          return;
        }
      }
      drawableHelper.init(GLJPanel.this);
      if (oglPipelineEnabled) {
        postGL(g);
      }
    }

    public void display(GLAutoDrawable drawable) {
      if (oglPipelineEnabled) {
        if (!preGL(g)) {
          return;
        }
      }

      if (sendReshape) {
        if (DEBUG) {
          System.err.println("glViewport(" + viewportX + ", " + viewportY + ", " + panelWidth + ", " + panelHeight + ")");
        }
        getGL().glViewport(viewportX, viewportY, panelWidth, panelHeight);
        drawableHelper.reshape(GLJPanel.this, viewportX, viewportY, panelWidth, panelHeight);
        sendReshape = false;
      }

      drawableHelper.display(GLJPanel.this);

      if (!oglPipelineEnabled) {
        // Must now copy pixels from offscreen context into surface
        if (offscreenImage == null) {
          if (panelWidth > 0 && panelHeight > 0) {
            // It looks like NVidia's drivers (at least the ones on my
            // notebook) are buggy and don't allow a sub-rectangle to be
            // read from a pbuffer...this doesn't really matter because
            // it's the Graphics.drawImage() calls that are the
            // bottleneck

            int awtFormat = 0;
            int hwGLFormat = 0;
            if (!hardwareAccelerationDisabled) {
              // This seems to be a good choice on all platforms
              hwGLFormat = GL.GL_UNSIGNED_INT_8_8_8_8_REV;
            }

            // Should be more flexible in these BufferedImage formats;
            // perhaps see what the preferred image types are on the
            // given platform
            if (isOpaque()) {
              awtFormat = BufferedImage.TYPE_INT_RGB;
            } else {
              awtFormat = BufferedImage.TYPE_INT_ARGB;
            }

            offscreenImage = new BufferedImage(panelWidth,
                                               panelHeight,
                                               awtFormat);
            switch (awtFormat) {
            case BufferedImage.TYPE_3BYTE_BGR:
              glFormat = GL.GL_BGR;
              glType   = GL.GL_UNSIGNED_BYTE;
              readBackBytes = ByteBuffer.allocate(readBackWidthInPixels * readBackHeightInPixels * 3);
              break;

            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_ARGB:
              glFormat = GL.GL_BGRA;
              glType   = (hardwareAccelerationDisabled
                          ? offscreenContext.getOffscreenContextPixelDataType()
                          : hwGLFormat);
              readBackInts = IntBuffer.allocate(readBackWidthInPixels * readBackHeightInPixels);
              break;

            default:
              // FIXME: Support more off-screen image types (current
              // offscreen context implementations don't use others, and
              // some of the OpenGL formats aren't supported in the 1.1
              // headers, which we're currently using)
              throw new GLException("Unsupported offscreen image type " + awtFormat);
            }
          }
        }

        if (offscreenImage != null) {
          GL gl = getGL();
          // Save current modes
          gl.glGetIntegerv(GL.GL_PACK_SWAP_BYTES,    swapbytes, 0);
          gl.glGetIntegerv(GL.GL_PACK_ROW_LENGTH,    rowlength, 0);
          gl.glGetIntegerv(GL.GL_PACK_SKIP_ROWS,     skiprows, 0);
          gl.glGetIntegerv(GL.GL_PACK_SKIP_PIXELS,   skippixels, 0);
          gl.glGetIntegerv(GL.GL_PACK_ALIGNMENT,     alignment, 0);

          gl.glPixelStorei(GL.GL_PACK_SWAP_BYTES,    GL.GL_FALSE);
          gl.glPixelStorei(GL.GL_PACK_ROW_LENGTH,    readBackWidthInPixels);
          gl.glPixelStorei(GL.GL_PACK_SKIP_ROWS,     0);
          gl.glPixelStorei(GL.GL_PACK_SKIP_PIXELS,   0);
          gl.glPixelStorei(GL.GL_PACK_ALIGNMENT,     1);

          // Actually read the pixels.
          gl.glReadBuffer(GL.GL_FRONT);
          if (readBackBytes != null) {
            gl.glReadPixels(0, 0, readBackWidthInPixels, readBackHeightInPixels, glFormat, glType, readBackBytes);
          } else if (readBackInts != null) {
            gl.glReadPixels(0, 0, readBackWidthInPixels, readBackHeightInPixels, glFormat, glType, readBackInts);
          }

          // Restore saved modes.
          gl.glPixelStorei(GL.GL_PACK_SWAP_BYTES,  swapbytes[0]);
          gl.glPixelStorei(GL.GL_PACK_ROW_LENGTH,  rowlength[0]);
          gl.glPixelStorei(GL.GL_PACK_SKIP_ROWS,   skiprows[0]);
          gl.glPixelStorei(GL.GL_PACK_SKIP_PIXELS, skippixels[0]);
          gl.glPixelStorei(GL.GL_PACK_ALIGNMENT,   alignment[0]);

          if (readBackBytes != null || readBackInts != null) {
            // Copy temporary data into raster of BufferedImage for faster
            // blitting Note that we could avoid this copy in the cases
            // where !offscreenContext.offscreenImageNeedsVerticalFlip(),
            // but that's the software rendering path which is very slow
            // anyway
            Object src  = null;
            Object dest = null;
            int    srcIncr  = 0;
            int    destIncr = 0;

            if (readBackBytes != null) {
              src = readBackBytes.array();
              dest = ((DataBufferByte) offscreenImage.getRaster().getDataBuffer()).getData();
              srcIncr = readBackWidthInPixels * 3;
              destIncr = offscreenImage.getWidth() * 3;
            } else {
              src = readBackInts.array();
              dest = ((DataBufferInt) offscreenImage.getRaster().getDataBuffer()).getData();
              srcIncr = readBackWidthInPixels;
              destIncr = offscreenImage.getWidth();
            }

            if (!hardwareAccelerationDisabled ||
                offscreenContext.offscreenImageNeedsVerticalFlip()) {
              int srcPos = 0;
              int destPos = (offscreenImage.getHeight() - 1) * destIncr;
              for (; destPos >= 0; srcPos += srcIncr, destPos -= destIncr) {
                System.arraycopy(src, srcPos, dest, destPos, destIncr);
              }
            } else {
              int srcPos = 0;
              int destEnd = destIncr * offscreenImage.getHeight();
              for (int destPos = 0; destPos < destEnd; srcPos += srcIncr, destPos += destIncr) {
                System.arraycopy(src, srcPos, dest, destPos, destIncr);
              }
            }

	    // Note: image will be drawn back in paintComponent() for
	    // correctness on all platforms
          }
        }
      } else {
        // Cause OpenGL pipeline to flush its results because
        // otherwise it's possible we will buffer up multiple frames'
        // rendering results, resulting in apparent mouse lag
        GL gl = getGL();
        gl.glFinish();

        postGL(g);
      }
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      // This is handled above and dispatched directly to the appropriate context
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }
  }

  class InitAction implements Runnable {
    public void run() {
      updater.init(GLJPanel.this);
    }
  }
  private InitAction initAction = new InitAction();

  class DisplayAction implements Runnable {
    public void run() {
      updater.display(GLJPanel.this);
    }
  }
  private DisplayAction displayAction = new DisplayAction();

  // This one is used exclusively in the non-hardware-accelerated case
  class SwapBuffersAction implements Runnable {
    public void run() {
      offscreenDrawable.swapBuffers();
    }
  }
  private SwapBuffersAction swapBuffersAction = new SwapBuffersAction();

  class PaintImmediatelyAction implements Runnable {
    public void run() {
      paintImmediately(0, 0, getWidth(), getHeight());
    }
  }
  private PaintImmediatelyAction paintImmediatelyAction = new PaintImmediatelyAction();

  private int getNextPowerOf2(int number) {
    // Workaround for problems where 0 width or height are transiently
    // seen during layout
    if (number == 0) {
      return 2;
    }

    if (((number-1) & number) == 0) {
      //ex: 8 -> 0b1000; 8-1=7 -> 0b0111; 0b1000&0b0111 == 0
      return number;
    }
    int power = 0;
    while (number > 0) {
      number = number>>1;
      power++;
    }
    return (1<<power);
  }

  private int getGLInteger(GL gl, int which) {
    int[] tmp = new int[1];
    gl.glGetIntegerv(which, tmp, 0);
    return tmp[0];
  }
}

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
    Z-ordering or LayoutManager problems. This component attempts to
    use hardware-accelerated rendering via pbuffers and falls back on
    to software rendering if problems occur. This class can not be
    instantiated directly; use {@link GLDrawableFactory} to construct
    them. <P>

    Note that because this component attempts to use pbuffers for
    rendering, and because pbuffers can not be resized, somewhat
    surprising behavior may occur during resize operations; the {@link
    GLEventListener#init} method may be called multiple times as the
    pbuffer is resized to be able to cover the size of the GLJPanel.
    This behavior is correct, as the textures and display lists for
    the GLJPanel will have been lost during the resize operation. The
    application should attempt to make its GLEventListener.init()
    methods as side-effect-free as possible. <P>

    The GLJPanel can be made transparent by creating it with a
    GLCapabilities object with alpha bits specified and calling {@link
    #setOpaque}(false). Pixels with resulting OpenGL alpha values less
    than 1.0 will be overlaid on any underlying Java2D rendering.
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
  private static boolean swingBufferPerWindow =
    Debug.isPropertyDefined("swing.bufferPerWindow") ?
    Debug.getBooleanProperty("swing.bufferPerWindow") :
      true;
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
  // These are always set to (0, 0) except when the Java2D / OpenGL
  // pipeline is active
  private int   viewportX;
  private int   viewportY;

  /** Creates a new GLJPanel component. The passed GLCapabilities must
      be non-null and specifies the OpenGL capabilities for the
      component. The GLCapabilitiesChooser must be non-null and
      specifies the algorithm for selecting one of the available
      GLCapabilities for the component; the GLDrawableFactory uses a
      DefaultGLCapabilitiesChooser if the user does not provide
      one. The passed GLContext may be null and specifies an OpenGL
      context with which to share textures, display lists and other
      OpenGL state. */
  protected GLJPanel(GLCapabilities capabilities, GLCapabilitiesChooser chooser, GLContext shareWith) {
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

  private void captureJ2DState(GL gl) {
    gl.glGetIntegerv(GL.GL_DRAW_BUFFER, drawBuffer, 0);
    gl.glGetIntegerv(GL.GL_READ_BUFFER, readBuffer, 0);
  }

  private boolean preGL(Graphics g) {
    GL gl = joglContext.getGL();
    // Set up needed state in JOGL context from Java2D context
    gl.glEnable(GL.GL_SCISSOR_TEST);
    Rectangle r = Java2D.getOGLScissorBox(g);
    if (r == null) {
      return false;
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

    gl.glDrawBuffer(drawBuffer[0]);
    gl.glReadBuffer(readBuffer[0]);
    return true;
  }

  /** Overridden from JComponent; calls event listeners' {@link
      GLEventListener#display display} methods. Should not be invoked
      by applications directly. */
  public void paintComponent(final Graphics g) {
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
      Java2D.invokeWithOGLContextCurrent(g, new Runnable() {
          public void run() {
            // Create no-op context representing Java2D context
            if (j2dContext == null) {
              j2dContext = GLDrawableFactory.getFactory().createExternalGLContext();

              // Check to see whether we can support the requested
              // capabilities or need to fall back to a pbuffer
              // FIXME: add more checks?

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
                j2dContext.destroy();
                j2dContext = null;
                return;
              }
            }

            j2dContext.makeCurrent();
            try {
              captureJ2DState(j2dContext.getGL());
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
                  joglDrawable = GLDrawableFactory.getFactory().createExternalGLDrawable();
                  joglContext = joglDrawable.createContext(shareWith);
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
    }
  }

  /** Overridden from JPanel; used to indicate that an OpenGL context
      may be created for the component. */
  public void addNotify() {
    super.addNotify();
    shouldInitialize = true;
    if (DEBUG) {
      System.err.println("GLJPanel.addNotify()");
    }
  }

  /** Overridden from JPanel; used to indicate that it's no longer
      safe to have an OpenGL context for the component. */
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

  /** Overridden from Canvas; causes {@link GLEventListener#reshape
      reshape} to be called on all registered {@link
      GLEventListener}s. Called automatically by the AWT; should not
      be invoked by applications directly. */
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
      pbuffer.setAutoSwapBufferMode(onOrOff);
    } else {
      drawableHelper.setAutoSwapBufferMode(onOrOff);
    }
  }

  public boolean getAutoSwapBufferMode() {
    if (!hardwareAccelerationDisabled) {
      return pbuffer.getAutoSwapBufferMode();
    } else {
      return drawableHelper.getAutoSwapBufferMode();
    }
  }

  public void swapBuffers() {
    if (!hardwareAccelerationDisabled) {
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
      GL_DEPTH_BUFFER_BIT)</code>), but for certain optimized Java2D
      and Swing implementations which use OpenGL internally, it may be
      possible to perform OpenGL rendering using the GLJPanel into the
      same OpenGL drawable as the Java2D implementation. */
  public boolean shouldPreserveColorBufferIfTranslucent() {
    return oglPipelineEnabled;
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
            pbuffer.destroy();
          }
          pbuffer = null;
          isInitialized = false;
          pbufferWidth = getNextPowerOf2(panelWidth);
          pbufferHeight = getNextPowerOf2(panelHeight);
          if (DEBUG) {
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
        readBackWidthInPixels  = pbufferWidth;
        readBackHeightInPixels = panelHeight;
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

            // Draw resulting image in one shot
            g.drawImage(offscreenImage, 0, 0, offscreenImage.getWidth(), offscreenImage.getHeight(), GLJPanel.this);
          }
        }
      } else {
        // Cause OpenGL pipeline to flush its results because
        // otherwise it's possible we will buffer up multiple frames'
        // rendering results, resulting in apparent mouse lag
        GL gl = getGL();
        gl.glFinish();
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

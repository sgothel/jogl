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

package net.java.games.jogl;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.nio.*;
import java.security.*;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.java.games.jogl.impl.*;

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

  // For saving/restoring of OpenGL state during ReadPixels
  private int[] swapbytes    = new int[1];
  private int[] rowlength    = new int[1];
  private int[] skiprows     = new int[1];
  private int[] skippixels   = new int[1];
  private int[] alignment    = new int[1];

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
    offscreenCaps = (GLCapabilities) capabilities.clone();
    offscreenCaps.setDoubleBuffered(false);
    this.chooser = chooser;
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

  /** Overridden from JComponent; calls event listeners' {@link
      GLEventListener#display display} methods. Should not be invoked
      by applications directly. */
  public void paintComponent(Graphics g) {
    if (shouldInitialize) {
      initialize();
    }

    if (!isInitialized) {
      return;
    }

    updater.setGraphics(g);
    if (!hardwareAccelerationDisabled) {
      pbuffer.display();
    } else {
      drawableHelper.invokeGL(offscreenDrawable, offscreenContext, displayAction, initAction);
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
    isInitialized = false;
    super.removeNotify();
  }

  /** Overridden from Canvas; causes {@link GLEventListener#reshape
      reshape} to be called on all registered {@link
      GLEventListener}s. Called automatically by the AWT; should not
      be invoked by applications directly. */
  public void reshape(int x, int y, int width, int height) {
    super.reshape(x, y, width, height);

    // Move all reshape requests onto AWT EventQueue thread
    final int fx = x;
    final int fy = y;
    final int fwidth = width;
    final int fheight = height;

    Runnable r = new Runnable() {
        public void run() {
          readBackWidthInPixels = 0;
          readBackHeightInPixels = 0;

          panelWidth  = fwidth;
          panelHeight = fheight;

          sendReshape = true;

          if (shouldInitialize) {
            initialize();
          }

          if (!isInitialized) {
            return;
          }

          if (!hardwareAccelerationDisabled) {
            // Use factor larger than 2 during shrinks for some hysteresis
            float shrinkFactor = 2.5f;
            if ((fwidth > pbufferWidth           )       || (fheight > pbufferHeight) ||
                (fwidth < (pbufferWidth / shrinkFactor)) || (fheight < (pbufferWidth / shrinkFactor))) {
              if (DEBUG) {
                System.err.println("Resizing pbuffer from (" + pbufferWidth + ", " + pbufferHeight + ") " +
                                   " to fit (" + fwidth + ", " + fheight + ")");
              }
              // Must destroy and recreate pbuffer to fit
              if (pbuffer != null) {
                pbuffer.destroy();
              }
              pbuffer = null;
              isInitialized = false;
              pbufferWidth = getNextPowerOf2(fwidth);
              pbufferHeight = getNextPowerOf2(fheight);
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
            readBackHeightInPixels = fheight;
          } else {
            offscreenContext.destroy();
            offscreenDrawable.setSize(Math.max(1, fwidth), Math.max(1, fheight));
            readBackWidthInPixels  = Math.max(1, fwidth);
            readBackHeightInPixels = Math.max(1, fheight);
          }

          if (offscreenImage != null) {
            offscreenImage.flush();
            offscreenImage = null;
          }
        }
      };
    if (EventQueue.isDispatchThread()) {
      r.run();
    } else {
      // Avoid blocking EventQueue thread due to possible deadlocks
      // during component creation
      EventQueue.invokeLater(r);
    }
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
    if (!hardwareAccelerationDisabled) {
      return pbuffer.getContext();
    } else {
      return offscreenContext;
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

  public GLU getGLU() {
    GLContext context = getContext();
    return (context == null) ? null : context.getGLU();
  }
  
  public void setGLU(GLU glu) {
    GLContext context = getContext();
    if (context != null) {
      context.setGLU(glu);
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

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void initialize() {
    if (panelWidth == 0 ||
        panelHeight == 0) {
      // Not sized to non-zero size yet
      return;
    }

    // Initialize either the hardware-accelerated rendering path or
    // the lightweight rendering path
    if (!hardwareAccelerationDisabled) {
      if (GLDrawableFactory.getFactory().canCreateGLPbuffer(offscreenCaps,
                                                            pbufferWidth,
                                                            pbufferHeight)) {
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

    updater = new Updater();
    shouldInitialize = false;
    isInitialized = true;
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
      drawableHelper.init(GLJPanel.this);
    }

    public void display(GLAutoDrawable drawable) {
      if (sendReshape) {
        if (DEBUG) {
          System.err.println("glViewport(0, 0, " + panelWidth + ", " + panelHeight + ")");
        }
        getGL().glViewport(0, 0, panelWidth, panelHeight);
        drawableHelper.reshape(GLJPanel.this, 0, 0, panelWidth, panelHeight);
        sendReshape = false;
      }

      drawableHelper.display(GLJPanel.this);

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
}

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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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
    them. */

public final class GLJPanel extends JPanel implements GLDrawable {
  private static boolean isMacOSX;

  static {
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          isMacOSX = System.getProperty("os.name").equals("Mac OS X");
          return null;
        }
      });
  }

  private GLDrawableHelper drawableHelper = new GLDrawableHelper();

  // Data used for either pbuffers or pixmap-based offscreen surfaces
  private GLCapabilities        offscreenCaps;
  private GLCapabilitiesChooser chooser;
  private GLDrawable            shareWith;
  private BufferedImage         offscreenImage;
  private int                   neededOffscreenImageWidth;
  private int                   neededOffscreenImageHeight;
  private DataBufferByte   dbByte;
  private DataBufferInt    dbInt;
  private Object semaphore = new Object();
  private int panelWidth   = 0;
  private int panelHeight  = 0;
  private Updater updater;
  private int awtFormat;
  private int glFormat;
  private int glType;

  // Implementation using pbuffers
  private static boolean hardwareAccelerationDisabled =
    Debug.isPropertyDefined("jogl.gljpanel.nohw");
  private boolean   pbufferInitializationCompleted;
  private GLPbuffer pbuffer;
  private int       pbufferWidth  = 256;
  private int       pbufferHeight = 256;
  private GLCanvas  heavyweight;
  private Frame     toplevel;

  // Implementation using software rendering
  private GLContext offscreenContext;

  // For saving/restoring of OpenGL state during ReadPixels
  private int[] swapbytes    = new int[1];
  private int[] lsbfirst     = new int[1];
  private int[] rowlength    = new int[1];
  private int[] skiprows     = new int[1];
  private int[] skippixels   = new int[1];
  private int[] alignment    = new int[1];

  GLJPanel(GLCapabilities capabilities, GLCapabilitiesChooser chooser, GLDrawable shareWith) {
    super();

    // Works around problems on many vendors' cards; we don't need a
    // back buffer for the offscreen surface anyway
    offscreenCaps = (GLCapabilities) capabilities.clone();
    offscreenCaps.setDoubleBuffered(false);
    this.chooser = chooser;
    this.shareWith = shareWith;
    
    initialize();
  }

  public void display() {
    if (EventQueue.isDispatchThread()) {
      // Can't block this thread
      repaint();
    } else {
      // Multithreaded redrawing of Swing components is not allowed,
      // so do everything on the event dispatch thread
      try {
        // Wait a reasonable period of time for the repaint to
        // complete, so that we don't swamp the AWT event queue thread
        // with repaint requests. We used to have an explicit flag to
        // detect when the repaint completed; unfortunately, under
        // some circumstances, the top-level window can be torn down
        // while we're waiting for the repaint to complete, which will
        // never happen. It doesn't look like there's enough
        // information in the EventQueue to figure out whether there
        // are pending events without posting to the queue, which we
        // don't want to do during shutdown, and adding a
        // HierarchyListener and watching for displayability events
        // might be fragile since we don't know exactly how this
        // component will be used in users' applications. For these
        // reasons we simply wait up to a brief period of time for the
        // repaint to complete.
        synchronized(semaphore) {
          repaint();
          semaphore.wait(100);
        }
      } catch (InterruptedException e) {
      }
    }
  }

  /** Overridden from JComponent; calls {@link
      GLEventListener#display}. Should not be invoked by applications
      directly. */
  public void paintComponent(Graphics g) {
    updater.setGraphics(g);
    if (!hardwareAccelerationDisabled) {
      if (!pbufferInitializationCompleted) {
        try {
          heavyweight.display();
          pbuffer.display();
        } catch (GLException e) {
          // We consider any exception thrown during updating of the
          // heavyweight or pbuffer during the initialization phases
          // to be an indication that there was a problem
          // instantiating the pbuffer, regardless of whether the
          // exception originated in the user's GLEventListener. In
          // these cases we immediately back off and use software
          // rendering.
          disableHardwareRendering();
        }
      } else {
        pbuffer.display();
      }
    } else {
      offscreenContext.invokeGL(displayAction, false, initAction);
    }
    synchronized(semaphore) {
      semaphore.notifyAll();
    }
  }

  /** Overridden from Canvas; causes {@link GLDrawableHelper#reshape}
      to be called on all registered {@link GLEventListener}s. Called
      automatically by the AWT; should not be invoked by applications
      directly. */
  public void reshape(int x, int y, int width, int height) {
    super.reshape(x, y, width, height);

    GLContext context = null;
    neededOffscreenImageWidth = 0;
    neededOffscreenImageHeight = 0;

    if (!hardwareAccelerationDisabled) {
      if (width > pbufferWidth || height > pbufferHeight) {
        // Must destroy and recreate pbuffer to fit
        pbuffer.destroy();
        if (width > pbufferWidth) {
          pbufferWidth = getNextPowerOf2(width);
        }
        if (height > pbufferHeight) {
          pbufferHeight = getNextPowerOf2(height);
        }
        initialize();
      }
      GLPbufferImpl pbufferImpl = (GLPbufferImpl) pbuffer;
      context = pbufferImpl.getContext();
      // It looks like NVidia's drivers (at least the ones on my
      // notebook) are buggy and don't allow a rectangle of less than
      // the pbuffer's width to be read...this doesn't really matter
      // because it's the Graphics.drawImage() calls that are the
      // bottleneck. Should probably make the size of the offscreen
      // image be the exact size of the pbuffer to save some work on
      // resize operations...
      neededOffscreenImageWidth  = pbufferWidth;
      neededOffscreenImageHeight = height;
    } else {
      offscreenContext.resizeOffscreenContext(width, height);
      context = offscreenContext;
      neededOffscreenImageWidth  = width;
      neededOffscreenImageHeight = height;
    }

    if (offscreenImage != null &&
        (offscreenImage.getWidth()  != neededOffscreenImageWidth ||
         offscreenImage.getHeight() != neededOffscreenImageHeight)) {
      offscreenImage.flush();
      offscreenImage = null;
    }

    panelWidth = width;
    panelHeight = height;
    final int fx      = 0;
    final int fy      = 0;

    context.invokeGL(new Runnable() {
        public void run() {
          getGL().glViewport(fx, fy, panelWidth, panelHeight);
          drawableHelper.reshape(GLJPanel.this, fx, fy, panelWidth, panelHeight);
        }
      }, true, initAction);
  }

  public void addGLEventListener(GLEventListener listener) {
    drawableHelper.addGLEventListener(listener);
  }

  public void removeGLEventListener(GLEventListener listener) {
    drawableHelper.removeGLEventListener(listener);
  }

  public GL getGL() {
    if (!hardwareAccelerationDisabled) {
      return pbuffer.getGL();
    } else {
      return offscreenContext.getGL();
    }
  }

  public void setGL(GL gl) {
    if (!hardwareAccelerationDisabled) {
      pbuffer.setGL(gl);
    } else {
      offscreenContext.setGL(gl);
    }
  }

  public GLU getGLU() {
    if (!hardwareAccelerationDisabled) {
      return pbuffer.getGLU();
    } else {
      return offscreenContext.getGLU();
    }
  }
  
  public void setGLU(GLU glu) {
    if (!hardwareAccelerationDisabled) {
      pbuffer.setGLU(glu);
    } else {
      offscreenContext.setGLU(glu);
    }
  }
  
  public void setRenderingThread(Thread currentThreadOrNull) throws GLException {
    // Not supported for GLJPanel because all repaint requests must be
    // handled by the AWT thread
  }

  public Thread getRenderingThread() {
    return null;
  }

  public void setNoAutoRedrawMode(boolean noAutoRedraws) {
  }

  public boolean getNoAutoRedrawMode() {
    return false;
  }

  public void setAutoSwapBufferMode(boolean onOrOff) {
    if (!hardwareAccelerationDisabled) {
      pbuffer.setAutoSwapBufferMode(onOrOff);
    } else {
      offscreenContext.setAutoSwapBufferMode(onOrOff);
    }
  }

  public boolean getAutoSwapBufferMode() {
    if (!hardwareAccelerationDisabled) {
      return pbuffer.getAutoSwapBufferMode();
    } else {
      return offscreenContext.getAutoSwapBufferMode();
    }
  }

  public void swapBuffers() {
    if (!hardwareAccelerationDisabled) {
      pbuffer.swapBuffers();
    } else {
      offscreenContext.invokeGL(swapBuffersAction, false, initAction);
    }
  }

  public boolean canCreateOffscreenDrawable() {
    // For now let's say no, although we could using the heavyweight
    // if hardware acceleration is still enabled
    return false;
  }

  public GLPbuffer createOffscreenDrawable(GLCapabilities capabilities,
                                           int initialWidth,
                                           int initialHeight) {
    throw new GLException("Not supported");
  }

  GLContext getContext() {
    if (!hardwareAccelerationDisabled) {
      return ((GLPbufferImpl) pbuffer).getContext();
    } else {
      return offscreenContext;
    }
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void disableHardwareRendering() {
    if (Debug.verbose()) {
      System.err.println("GLJPanel: Falling back on software rendering due to pbuffer problems");
    }
    hardwareAccelerationDisabled = true;
    pbufferInitializationCompleted = false;
    EventQueue.invokeLater(new Runnable() {
        public void run() {
          toplevel.setVisible(false);
          // Should dispose of this -- not sure about stability on
          // various cards -- should test (FIXME)
          // toplevel.dispose();
        }
      });
    initialize();
  }

  private void initialize() {
    // Initialize either the hardware-accelerated rendering path or
    // the lightweight rendering path
    if (!hardwareAccelerationDisabled) {
      boolean firstTime = false;
      if (heavyweight == null) {
        // Make the heavyweight share with the "shareWith" parameter.
        // The pbuffer shares textures and display lists with the
        // heavyweight, so by transitivity the pbuffer will share with
        // it as well.
        heavyweight = GLDrawableFactory.getFactory().createGLCanvas(new GLCapabilities(), shareWith);
        firstTime = true;
      }
      if (heavyweight.canCreateOffscreenDrawable()) {
        if (firstTime) {
          toplevel = new Frame();
          toplevel.setUndecorated(true);
        }
        pbuffer = heavyweight.createOffscreenDrawable(offscreenCaps, pbufferWidth, pbufferHeight);
        updater = new Updater();
        pbuffer.addGLEventListener(updater);
        pbufferInitializationCompleted = false;
        if (firstTime) {
          toplevel.add(heavyweight);
          toplevel.setSize(0, 0);
        }
        EventQueue.invokeLater(new Runnable() {
            public void run() {
              try {
                toplevel.setVisible(true);
              } catch (GLException e) {
                disableHardwareRendering();
              }
            }
          });
        return;
      } else {
        // If the heavyweight reports that it can't create an
        // offscreen drawable (pbuffer), don't try again the next
        // time, and fall through to the software rendering path
        hardwareAccelerationDisabled = true;
      }
    }

    // Create an offscreen context instead
    offscreenContext = GLContextFactory.getFactory().createGLContext(null, offscreenCaps, chooser,
                                                                     GLContextHelper.getContext(shareWith));
    offscreenContext.resizeOffscreenContext(panelWidth, panelHeight);
    updater = new Updater();
    if (panelWidth > 0 && panelHeight > 0) {
      offscreenContext.invokeGL(new Runnable() {
          public void run() {
            getGL().glViewport(0, 0, panelWidth, panelHeight);
            drawableHelper.reshape(GLJPanel.this, 0, 0, panelWidth, panelHeight);
          }
        }, true, initAction);
    }
  }

  class Updater implements GLEventListener {
    private Graphics g;

    public void setGraphics(Graphics g) {
      this.g = g;
    }

    public void init(GLDrawable drawable) {
      if (!hardwareAccelerationDisabled) {
        pbufferInitializationCompleted = true;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
              toplevel.setVisible(false);
            }
          });
      }
      drawableHelper.init(GLJPanel.this);
    }

    public void display(GLDrawable drawable) {
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
            // Should be more flexible in these BufferedImage formats;
            // perhaps see what the preferred image types are on the
            // given platform
            if (offscreenCaps.getAlphaBits() > 0) {
              awtFormat = BufferedImage.TYPE_INT_ARGB;
            } else {
              awtFormat = BufferedImage.TYPE_INT_RGB;
            }

            // Choose better pixel format on Mac OS X
            if (isMacOSX) {
              hwGLFormat = GL.GL_UNSIGNED_INT_8_8_8_8_REV;
            } else {
              hwGLFormat = GL.GL_UNSIGNED_BYTE;
            }
          } else {
            awtFormat = offscreenContext.getOffscreenContextBufferedImageType();
          }

          offscreenImage = new BufferedImage(neededOffscreenImageWidth,
                                             neededOffscreenImageHeight,
                                             awtFormat);
          switch (awtFormat) {
            case BufferedImage.TYPE_3BYTE_BGR:
              glFormat = GL.GL_BGR;
              glType   = GL.GL_UNSIGNED_BYTE;
              dbByte   = (DataBufferByte) offscreenImage.getRaster().getDataBuffer();
              break;

            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_ARGB:
              glFormat = GL.GL_BGRA;
              glType   = (hardwareAccelerationDisabled
                            ? offscreenContext.getOffscreenContextPixelDataType()
                            : hwGLFormat);
              dbInt    = (DataBufferInt) offscreenImage.getRaster().getDataBuffer();
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
        gl.glGetIntegerv(GL.GL_PACK_SWAP_BYTES,    swapbytes);
        gl.glGetIntegerv(GL.GL_PACK_LSB_FIRST,     lsbfirst);
        gl.glGetIntegerv(GL.GL_PACK_ROW_LENGTH,    rowlength);
        gl.glGetIntegerv(GL.GL_PACK_SKIP_ROWS,     skiprows);
        gl.glGetIntegerv(GL.GL_PACK_SKIP_PIXELS,   skippixels);
        gl.glGetIntegerv(GL.GL_PACK_ALIGNMENT,     alignment);

        // Little endian machines (DEC Alpha, Intel X86, PPC (in LSB
        // mode)...  for example) could benefit from setting
        // GL_PACK_LSB_FIRST to GL_TRUE instead of GL_FALSE, but this
        // would require changing the generated bitmaps too.
        gl.glPixelStorei(GL.GL_PACK_SWAP_BYTES,    GL.GL_FALSE);
        gl.glPixelStorei(GL.GL_PACK_LSB_FIRST,     GL.GL_TRUE);
        gl.glPixelStorei(GL.GL_PACK_ROW_LENGTH,    offscreenImage.getWidth());
        gl.glPixelStorei(GL.GL_PACK_SKIP_ROWS,     0);
        gl.glPixelStorei(GL.GL_PACK_SKIP_PIXELS,   0);
        gl.glPixelStorei(GL.GL_PACK_ALIGNMENT,     1);

        // Actually read the pixels.
        gl.glReadBuffer(GL.GL_FRONT);
        if (dbByte != null) {
          gl.glReadPixels(0, 0, offscreenImage.getWidth(), offscreenImage.getHeight(), glFormat, glType, dbByte.getData());
        } else if (dbInt != null) {
          gl.glReadPixels(0, 0, offscreenImage.getWidth(), offscreenImage.getHeight(), glFormat, glType, dbInt.getData());
        }

        // Restore saved modes.
        gl.glPixelStorei(GL.GL_PACK_SWAP_BYTES,  swapbytes[0]);
        gl.glPixelStorei(GL.GL_PACK_LSB_FIRST,   lsbfirst[0]);
        gl.glPixelStorei(GL.GL_PACK_ROW_LENGTH,  rowlength[0]);
        gl.glPixelStorei(GL.GL_PACK_SKIP_ROWS,   skiprows[0]);
        gl.glPixelStorei(GL.GL_PACK_SKIP_PIXELS, skippixels[0]);
        gl.glPixelStorei(GL.GL_PACK_ALIGNMENT,   alignment[0]);
      
        if (!hardwareAccelerationDisabled ||
            offscreenContext.offscreenImageNeedsVerticalFlip()) {
          // This performs reasonably well; the snippet below does not.
          // Should figure out if we need to set the image scaling
          // preference to FAST since it doesn't require subsampling
          // of pixels -- FIXME
          for (int i = 0; i < panelHeight - 1; i++) {
            g.drawImage(offscreenImage,
                        0, i, panelWidth, i+1,
                        0, panelHeight - i - 2, panelWidth, panelHeight - i - 1,
                        GLJPanel.this);
          }
        } else {
          g.drawImage(offscreenImage, 0, 0, offscreenImage.getWidth(), offscreenImage.getHeight(), GLJPanel.this);
        }
      }
    }

    public void reshape(GLDrawable drawable, int x, int y, int width, int height) {
      // This is handled above and dispatched directly to the appropriate context
    }

    public void displayChanged(GLDrawable drawable, boolean modeChanged, boolean deviceChanged) {
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
      offscreenContext.swapBuffers();
    }
  }
  private SwapBuffersAction swapBuffersAction = new SwapBuffersAction();

  private int getNextPowerOf2(int number) {
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

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
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.java.games.jogl.impl.*;

// FIXME: Subclasses need to call resetGLFunctionAvailability() on their
// context whenever the displayChanged() function is called on their
// GLEventListeners

/** A lightweight Swing component which provides OpenGL rendering
    support. Provided for compatibility with Swing user interfaces
    when adding a heavyweight doesn't work either because of
    Z-ordering or LayoutManager problems. Currently implemented using
    offscreen (i.e., non-hardware accelerated) rendering, so
    performance will likely be poor. This class can not be
    instantiated directly; use {@link GLDrawableFactory} to construct
    them. */

public final class GLJPanel extends JPanel implements GLDrawable {
  private GLDrawableHelper drawableHelper = new GLDrawableHelper();
  private GLContext context;
  private BufferedImage offscreenImage;
  private int awtFormat;
  private int glFormat;
  private int glType;
  private int glComps;
  private DataBufferByte   dbByte;
  private DataBufferInt    dbInt;
  private Object semaphore = new Object();
  private boolean repaintDone;

  // For saving/restoring of OpenGL state during ReadPixels
  private int[] swapbytes    = new int[1];
  private int[] lsbfirst     = new int[1];
  private int[] rowlength    = new int[1];
  private int[] skiprows     = new int[1];
  private int[] skippixels   = new int[1];
  private int[] alignment    = new int[1];

  GLJPanel(GLCapabilities capabilities, GLCapabilitiesChooser chooser, GLDrawable shareWith) {
    super();
    context = GLContextFactory.getFactory().createGLContext(null, capabilities, chooser,
                                                            GLContextHelper.getContext(shareWith));
  }

  public void display() {
    // Multithreaded redrawing of Swing components is not allowed
    try {
      synchronized(semaphore) {
        repaintDone = false;
        repaint();
        while (!repaintDone) {
          semaphore.wait();
        }
      }
    } catch (InterruptedException e) {
    }
  }

  /** Overridden from JComponent; calls {@link #display}. Should not
      be invoked by applications directly. */
  public void paintComponent(Graphics g) {
    displayAction.setGraphics(g);
    context.invokeGL(displayAction, false, initAction);
    synchronized(semaphore) {
      repaintDone = true;
      semaphore.notifyAll();
    }
  }

  /** Overridden from Canvas; causes {@link GLDrawableHelper#reshape}
      to be called on all registered {@link GLEventListener}s. Called
      automatically by the AWT; should not be invoked by applications
      directly. */
  public void reshape(int x, int y, int width, int height) {
    super.reshape(x, y, width, height);
    // NOTE: we don't pay attention to the x and y provided since we
    // are blitting into this component directly
    final int fx      = 0;
    final int fy      = 0;
    final int fwidth  = width;
    final int fheight = height;
    context.resizeOffscreenContext(width, height);
    context.invokeGL(new Runnable() {
        public void run() {
          getGL().glViewport(fx, fy, fwidth, fheight);
          drawableHelper.reshape(GLJPanel.this, fx, fy, fwidth, fheight);
          if (offscreenImage != null &&
              (offscreenImage.getWidth()  != context.getOffscreenContextWidth() ||
               offscreenImage.getHeight() != context.getOffscreenContextHeight())) {
            offscreenImage.flush();
            offscreenImage = null;
          }
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
    return context.getGL();
  }

  public void setGL(GL gl) {
    context.setGL(gl);
  }

  public GLU getGLU() {
    return context.getGLU();
  }
  
  public void setGLU(GLU glu) {
    context.setGLU(glu);
  }
  
  public void setRenderingThread(Thread currentThreadOrNull) throws GLException {
    // Not supported for GLJPanel because all repaint requests must be
    // handled by the AWT thread
  }

  public Thread getRenderingThread() {
    return context.getRenderingThread();
  }

  public void setNoAutoRedrawMode(boolean noAutoRedraws) {
  }

  public boolean getNoAutoRedrawMode() {
    return false;
  }

  public boolean canCreateOffscreenDrawable() {
    // For now let's say no; maybe we can reimplement this class in
    // terms of pbuffers (though not all vendors support them, and
    // they seem to require an onscreen context)
    return false;
  }

  public GLPbuffer createOffscreenDrawable(GLCapabilities capabilities,
                                           int initialWidth,
                                           int initialHeight) {
    throw new GLException("Not supported");
  }

  GLContext getContext() {
    return context;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  class InitAction implements Runnable {
    public void run() {
      drawableHelper.init(GLJPanel.this);
    }
  }
  private InitAction initAction = new InitAction();
  
  class DisplayAction implements Runnable {
    private Graphics g;

    public void setGraphics(Graphics g) {
      this.g = g;
    }

    public void run() {
      drawableHelper.display(GLJPanel.this);
      // Must now copy pixels from offscreen context into surface
      if (offscreenImage == null) {
        int awtFormat = context.getOffscreenContextBufferedImageType();
        offscreenImage = new BufferedImage(context.getOffscreenContextWidth(), context.getOffscreenContextHeight(), awtFormat);
        switch (awtFormat) {
          case BufferedImage.TYPE_3BYTE_BGR:
            glFormat = GL.GL_BGR;
            glType   = GL.GL_UNSIGNED_BYTE;
            glComps  = 3;
            dbByte   = (DataBufferByte) offscreenImage.getRaster().getDataBuffer();
            break;

          case BufferedImage.TYPE_INT_RGB:
            glFormat = GL.GL_BGRA;
            glType   = GL.GL_UNSIGNED_BYTE;
            glComps  = 4;
            dbInt    = (DataBufferInt) offscreenImage.getRaster().getDataBuffer();
            break;

          case BufferedImage.TYPE_INT_ARGB:
            glFormat = GL.GL_BGRA;
            glType   = context.getOffscreenContextPixelDataType();
            glComps  = 4;
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
      gl.glReadBuffer(context.getOffscreenContextReadBuffer());
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
      
      gl.glFlush();
      gl.glFinish();

      if (context.offscreenImageNeedsVerticalFlip()) {
        g.drawImage(offscreenImage,
                    0, 0, offscreenImage.getWidth(), offscreenImage.getHeight(),
                    0, offscreenImage.getHeight(), offscreenImage.getWidth(), 0,
                    GLJPanel.this);
      } else {
        g.drawImage(offscreenImage, 0, 0, offscreenImage.getWidth(), offscreenImage.getHeight(), GLJPanel.this);
      }
    }
  }
  private DisplayAction displayAction = new DisplayAction();
}

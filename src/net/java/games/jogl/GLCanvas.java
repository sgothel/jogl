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

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import net.java.games.jogl.impl.*;

// FIXME: Subclasses need to call resetGLFunctionAvailability() on their
// context whenever the displayChanged() function is called on our
// GLEventListeners

/** A heavyweight AWT component which provides OpenGL rendering
    support. This is the primary implementation of {@link GLDrawable};
    {@link GLJPanel} is provided for compatibility with Swing user
    interfaces when adding a heavyweight doesn't work either because
    of Z-ordering or LayoutManager problems. This class can not be
    instantiated directly; use {@link GLDrawableFactory} to construct
    them. */

public final class GLCanvas extends Canvas implements GLDrawable {

  private GLDrawableHelper drawableHelper = new GLDrawableHelper();
  private GLContext context;
  
  GLCanvas(GraphicsConfiguration config,
           GLCapabilities capabilities,
           GLCapabilitiesChooser chooser,
           GLDrawable shareWith) {
    super(config);
    context = GLContextFactory.getFactory().createGLContext(this, capabilities, chooser,
                                                            GLContextHelper.getContext(shareWith));
  }
  
  public void display() {
    displayImpl();
  }

  /** Overridden from Canvas; calls {@link #display}. Should not be
      invoked by applications directly. */
  public void paint(Graphics g) {
    if (!context.getNoAutoRedrawMode()) {
      display();
    }
  }

  /** Overridden from Canvas; causes {@link GLDrawableHelper#reshape}
      to be called on all registered {@link GLEventListener}s. Called
      automatically by the AWT; should not be invoked by applications
      directly. */
  public void reshape(int x, int y, int width, int height) {
    super.reshape(x, y, width, height);
    // Note: we ignore the given x and y within the parent component
    // since we are drawing directly into this heavyweight component.
    final int fx      = 0;
    final int fy      = 0;
    final int fwidth  = width;
    final int fheight = height;
    context.invokeGL(new Runnable() {
        public void run() {
          getGL().glViewport(fx, fy, fwidth, fheight);
          drawableHelper.reshape(GLCanvas.this, fx, fy, fwidth, fheight);
        }
      }, true, initAction);
  }

  /** Overridden from Canvas to prevent Java2D's clearing of the
      canvas from interfering with the OpenGL rendering. */
  public void update(Graphics g) {
    paint(g);
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
  
  void willSetRenderingThread() {
    context.willSetRenderingThread();
  }

  public void setRenderingThread(Thread currentThreadOrNull) throws GLException {
    context.setRenderingThread(currentThreadOrNull, initAction);
  }

  public Thread getRenderingThread() {
    return context.getRenderingThread();
  }

  public void setNoAutoRedrawMode(boolean noAutoRedraw) {
    context.setNoAutoRedrawMode(noAutoRedraw);
  }

  public boolean getNoAutoRedrawMode() {
    return context.getNoAutoRedrawMode();
  }

  public void setAutoSwapBufferMode(boolean onOrOff) {
    context.setAutoSwapBufferMode(onOrOff);
  }

  public boolean getAutoSwapBufferMode() {
    return context.getAutoSwapBufferMode();
  }

  public void swapBuffers() {
    context.invokeGL(swapBuffersAction, false, initAction);
  }

  public boolean canCreateOffscreenDrawable() {
    return context.canCreatePbufferContext();
  }

  public GLPbuffer createOffscreenDrawable(GLCapabilities capabilities,
                                           int initialWidth,
                                           int initialHeight) {
    return new GLPbufferImpl(context.createPbufferContext(capabilities, initialWidth, initialHeight));
  }

  GLContext getContext() {
    return context;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void displayImpl() {
    context.invokeGL(displayAction, false, initAction);
  }

  class InitAction implements Runnable {
    public void run() {
      drawableHelper.init(GLCanvas.this);
    }
  }
  private InitAction initAction = new InitAction();
  
  class DisplayAction implements Runnable {
    public void run() {
      drawableHelper.display(GLCanvas.this);
    }
  }
  private DisplayAction displayAction = new DisplayAction();

  class SwapBuffersAction implements Runnable {
    public void run() {
      context.swapBuffers();
    }
  }
  private SwapBuffersAction swapBuffersAction = new SwapBuffersAction();
}

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

package net.java.games.jogl.impl;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.*;
import java.beans.PropertyChangeListener;

import net.java.games.jogl.*;

/** Platform-independent class exposing pbuffer functionality to
    applications. This class is not exposed in the public API as it
    would probably add no value; however it implements the GLDrawable
    interface so can be interacted with via its display() method. */

public class GLPbufferImpl implements GLPbuffer {
  // GLPbufferContext
  private GLContext context;
  private GLDrawableHelper drawableHelper = new GLDrawableHelper();
  private boolean isInitialized=false;
  private int floatMode;

  public GLPbufferImpl(GLContext context) {
    this.context = context;
  }

  public void display() {
    maybeDoSingleThreadedWorkaround(displayOnEventDispatchThreadAction,
                                    displayAction,
                                    false);
  }

  public void setSize(int width, int height) {
    // FIXME
    throw new GLException("Not yet implemented");
  }

  public void setSize(Dimension d) {
    setSize(d.width, d.height);
  }

  public Dimension getSize() {
    return getSize(null);
  }

  public Dimension getSize(Dimension d) {
    // FIXME
    throw new GLException("Not yet implemented");
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
    // Not supported for pbuffers
  }

  public void setRenderingThread(Thread currentThreadOrNull) throws GLException {
    // Not supported for pbuffers
  }

  public Thread getRenderingThread() {
    // Not supported for pbuffers
    return null;
  }

  public void setNoAutoRedrawMode(boolean noAutoRedraws) {
  }

  public boolean getNoAutoRedrawMode() {
    return false;
  }

  public void setAutoSwapBufferMode(boolean onOrOff) {
    context.setAutoSwapBufferMode(onOrOff);
  }

  public boolean getAutoSwapBufferMode() {
    return context.getAutoSwapBufferMode();
  }

  public void swapBuffers() {
    maybeDoSingleThreadedWorkaround(swapBuffersOnEventDispatchThreadAction, swapBuffersAction, false);
  }

  public boolean canCreateOffscreenDrawable() {
    return false;
  }

  public GLPbuffer createOffscreenDrawable(GLCapabilities capabilities,
                                           int initialWidth,
                                           int initialHeight) {
    throw new GLException("Not supported");
  }

  public void bindTexture() {
    // Doesn't make much sense to try to do this on the event dispatch
    // thread given that it has to be called while the context is current
    context.bindPbufferToTexture();
  }

  public void releaseTexture() {
    // Doesn't make much sense to try to do this on the event dispatch
    // thread given that it has to be called while the context is current
    context.releasePbufferFromTexture();
  }

  public GLContext getContext() {
    return context;
  }

  //----------------------------------------------------------------------
  // No-ops for ComponentEvents
  //

  public void addComponentListener(ComponentListener l) {}
  public void removeComponentListener(ComponentListener l) {}
  public void addFocusListener(FocusListener l) {}
  public void removeFocusListener(FocusListener l) {}
  public void addHierarchyBoundsListener(HierarchyBoundsListener l) {}
  public void removeHierarchyBoundsListener(HierarchyBoundsListener l) {}
  public void addHierarchyListener(HierarchyListener l) {}
  public void removeHierarchyListener(HierarchyListener l) {}
  public void addInputMethodListener(InputMethodListener l) {}
  public void removeInputMethodListener(InputMethodListener l) {}
  public void addKeyListener(KeyListener l) {}
  public void removeKeyListener(KeyListener l) {}
  public void addMouseListener(MouseListener l) {}
  public void removeMouseListener(MouseListener l) {}
  public void addMouseMotionListener(MouseMotionListener l) {}
  public void removeMouseMotionListener(MouseMotionListener l) {}
  public void addMouseWheelListener(MouseWheelListener l) {}
  public void removeMouseWheelListener(MouseWheelListener l) {}
  public void addPropertyChangeListener(PropertyChangeListener listener) {}
  public void removePropertyChangeListener(PropertyChangeListener listener) {}
  public void addPropertyChangeListener(String propertyName,
                                        PropertyChangeListener listener) {}
  public void removePropertyChangeListener(String propertyName,
                                           PropertyChangeListener listener) {}

    /** Queries initialization status of this pBuffer.
     * @return true if initialized
     * */
  public boolean isInitialized(){
    return isInitialized;
  }

  public void destroy() {
    context.destroy();
  }

  public int getFloatingPointMode() {
    if (floatMode == 0) {
      throw new GLException("Pbuffer not initialized, or floating-point support not requested");
    }
    return floatMode;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void maybeDoSingleThreadedWorkaround(Runnable eventDispatchThreadAction,
                                               Runnable invokeGLAction,
                                               boolean  isReshape) {
    if (SingleThreadedWorkaround.doWorkaround() && !EventQueue.isDispatchThread()) {
      try {
        // Reshape events must not block on the event queue due to the
        // possibility of deadlocks during initial component creation.
        // This solution is not optimal, because it changes the
        // semantics of reshape() to have some of the processing being
        // done asynchronously, but at least it preserves the
        // semantics of the single-threaded workaround.
        if (!isReshape) {
          EventQueue.invokeAndWait(eventDispatchThreadAction);
        } else {
          EventQueue.invokeLater(eventDispatchThreadAction);
        }
      } catch (Exception e) {
        throw new GLException(e);
      }
    } else {
      context.invokeGL(invokeGLAction, isReshape, initAction);
    }
  }

  class InitAction implements Runnable {
    public void run() {
      isInitialized=true;
      floatMode = context.getFloatingPointMode();
      drawableHelper.init(GLPbufferImpl.this);
    }
  }
  private InitAction initAction = new InitAction();
  
  class DisplayAction implements Runnable {
    public void run() {
      drawableHelper.display(GLPbufferImpl.this);
    }
  }
  private DisplayAction displayAction = new DisplayAction();

  class SwapBuffersAction implements Runnable {
    public void run() {
      context.swapBuffers();
    }
  }
  private SwapBuffersAction swapBuffersAction = new SwapBuffersAction();

  // Workaround for ATI driver bugs related to multithreading issues
  // like simultaneous rendering via Animators to canvases that are
  // being resized on the AWT event dispatch thread
  class DisplayOnEventDispatchThreadAction implements Runnable {
    public void run() {
      context.invokeGL(displayAction, false, initAction);
    }
  }
  private DisplayOnEventDispatchThreadAction displayOnEventDispatchThreadAction =
    new DisplayOnEventDispatchThreadAction();
  class SwapBuffersOnEventDispatchThreadAction implements Runnable {
    public void run() {
      context.invokeGL(swapBuffersAction, false, initAction);
    }
  }
  private SwapBuffersOnEventDispatchThreadAction swapBuffersOnEventDispatchThreadAction =
    new SwapBuffersOnEventDispatchThreadAction();
}

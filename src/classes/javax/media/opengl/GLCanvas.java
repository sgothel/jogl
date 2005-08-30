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

import java.awt.Canvas;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import com.sun.opengl.impl.*;

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

public class GLCanvas extends Canvas implements GLAutoDrawable {

  private static final boolean DEBUG = Debug.debug("GLCanvas");

  private GLDrawableHelper drawableHelper = new GLDrawableHelper();
  private GLDrawable drawable;
  private GLContextImpl context;
  private boolean autoSwapBufferMode = true;
  private boolean sendReshape = false;

  /** Creates a new GLCanvas component. The passed GLCapabilities must
      be non-null and specifies the OpenGL capabilities for the
      component. The GLCapabilitiesChooser must be non-null and
      specifies the algorithm for selecting one of the available
      GLCapabilities for the component; the GLDrawableFactory uses a
      DefaultGLCapabilitesChooser if the user does not provide
      one. The passed GLContext may be null and specifies an OpenGL
      context with which to share textures, display lists and other
      OpenGL state. The passed GraphicsDevice must be non-null and
      indicates the screen on which to create the GLCanvas; the
      GLDrawableFactory uses the default screen device of the local
      GraphicsEnvironment if the user does not provide one. */
  protected GLCanvas(GLCapabilities capabilities,
                     GLCapabilitiesChooser chooser,
                     GLContext shareWith,
                     GraphicsDevice device) {
    // The platform-specific GLDrawableFactory will only provide a
    // non-null GraphicsConfiguration on platforms where this is
    // necessary (currently only X11, as Windows allows the pixel
    // format of the window to be set later and Mac OS X seems to
    // handle this very differently than all other platforms). On
    // other platforms this method returns null; it is the case (at
    // least in the Sun AWT implementation) that this will result in
    // equivalent behavior to calling the no-arg super() constructor
    // for Canvas.
    super(GLDrawableFactory.getFactory().chooseGraphicsConfiguration(capabilities, chooser, device));
    drawable = GLDrawableFactory.getFactory().getGLDrawable(this, capabilities, chooser);
    context = (GLContextImpl) drawable.createContext(shareWith);
    context.setSynchronized(true);
  }
  
  public GLContext createContext(GLContext shareWith) {
    return drawable.createContext(shareWith);
  }

  public void setRealized(boolean realized) {
  }

  public void display() {
    maybeDoSingleThreadedWorkaround(displayOnEventDispatchThreadAction,
                                    displayAction,
                                    false);
  }

  /** Overridden from Canvas; calls {@link #display}. Should not be
      invoked by applications directly. */
  public void paint(Graphics g) {
    display();
  }

  /** Overridden from Canvas; used to indicate when it's safe to
      create an OpenGL context for the component. */
  public void addNotify() {
    super.addNotify();
    drawable.setRealized(true);
    if (DEBUG) {
      System.err.println("GLCanvas.addNotify()");
    }
  }

  /** Overridden from Canvas; used to indicate that it's no longer
      safe to have an OpenGL context for the component. */
  public void removeNotify() {
    context.destroy();
    drawable.setRealized(false);
    super.removeNotify();
    if (DEBUG) {
      System.err.println("GLCanvas.removeNotify()");
    }
  }

  /** Overridden from Canvas; causes {@link GLDrawableHelper#reshape}
      to be called on all registered {@link GLEventListener}s. Called
      automatically by the AWT; should not be invoked by applications
      directly. */
  public void reshape(int x, int y, int width, int height) {
    super.reshape(x, y, width, height);
    sendReshape = true;
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

  public GLContext getContext() {
    return context;
  }

  public GL getGL() {
    return getContext().getGL();
  }

  public void setGL(GL gl) {
    getContext().setGL(gl);
  }

  public GLU getGLU() {
    return getContext().getGLU();
  }
  
  public void setGLU(GLU glu) {
    getContext().setGLU(glu);
  }
  
  public void setAutoSwapBufferMode(boolean onOrOff) {
    drawableHelper.setAutoSwapBufferMode(onOrOff);
  }

  public boolean getAutoSwapBufferMode() {
    return drawableHelper.getAutoSwapBufferMode();
  }

  public void swapBuffers() {
    maybeDoSingleThreadedWorkaround(swapBuffersOnEventDispatchThreadAction, swapBuffersAction, false);
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
      drawableHelper.invokeGL(drawable, context, invokeGLAction, initAction);
    }
  }

  class InitAction implements Runnable {
    public void run() {
      drawableHelper.init(GLCanvas.this);
    }
  }
  private InitAction initAction = new InitAction();
  
  class DisplayAction implements Runnable {
    public void run() {
      if (sendReshape) {
        // Note: we ignore the given x and y within the parent component
        // since we are drawing directly into this heavyweight component.
        int width = getWidth();
        int height = getHeight();
        getGL().glViewport(0, 0, width, height);
        drawableHelper.reshape(GLCanvas.this, 0, 0, width, height);
        sendReshape = false;
      }

      drawableHelper.display(GLCanvas.this);
    }
  }
  private DisplayAction displayAction = new DisplayAction();

  class SwapBuffersAction implements Runnable {
    public void run() {
      drawable.swapBuffers();
    }
  }
  private SwapBuffersAction swapBuffersAction = new SwapBuffersAction();

  // Workaround for ATI driver bugs related to multithreading issues
  // like simultaneous rendering via Animators to canvases that are
  // being resized on the AWT event dispatch thread
  class DisplayOnEventDispatchThreadAction implements Runnable {
    public void run() {
      drawableHelper.invokeGL(drawable, context, displayAction, initAction);
    }
  }
  private DisplayOnEventDispatchThreadAction displayOnEventDispatchThreadAction =
    new DisplayOnEventDispatchThreadAction();
  class SwapBuffersOnEventDispatchThreadAction implements Runnable {
    public void run() {
      drawableHelper.invokeGL(drawable, context, swapBuffersAction, initAction);
    }
  }
  private SwapBuffersOnEventDispatchThreadAction swapBuffersOnEventDispatchThreadAction =
    new SwapBuffersOnEventDispatchThreadAction();
}

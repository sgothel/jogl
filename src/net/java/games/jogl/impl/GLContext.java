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

package net.java.games.jogl.impl;

import java.awt.Component;
import java.awt.EventQueue;
import net.java.games.jogl.*;

public abstract class GLContext {
  protected static final boolean DEBUG = false;

  static {
    NativeLibLoader.load();
  }

  protected Component component;

  // Indicates whether the component (if an onscreen context) has been
  // realized. Plausibly, before the component is realized the JAWT
  // should return an error or NULL object from some of its
  // operations; this appears to be the case on Win32 but is not true
  // at least with Sun's current X11 implementation (1.4.x), which
  // crashes with no other error reported if the DrawingSurfaceInfo is
  // fetched from a locked DrawingSurface during the validation as a
  // result of calling show() on the main thread. To work around this
  // we prevent any JAWT or OpenGL operations from being done until
  // the first event is received from the AWT event dispatch thread.
  private boolean realized;

  // This avoids (as much as possible) losing the effects of
  // setRenderingThread, which may need to be deferred if the
  // component is not realized
  private boolean deferredSetRenderingThread;

  protected GLCapabilities capabilities;
  protected GLCapabilitiesChooser chooser;
  protected GL gl;
  // All GLU interfaces eventually route calls down to gluRoot. It can be
  // static because GLU it doesn't actually need to own context, it just makes
  // GL calls and assumes some context is active.
  protected static final GLU gluRoot = new GLUImpl();
  protected static GLU glu = gluRoot; // this is the context's GLU interface
  protected Thread renderingThread;
  protected Runnable deferredReshapeAction;

  // This is a workaround for a bug in NVidia's drivers where
  // vertex_array_range is only safe for single-threaded use; a bug
  // has been filed, ID 80174. When an Animator is created for a
  // GLDrawable, the expectation is that the Animator will be started
  // shortly and that the user doesn't want rendering to occur from
  // the AWT thread. However, there is a small window between when the
  // Animator is created and attached to the GLDrawable and when it's
  // started (and sets the rendering thread) when repaint events can
  // be issued by the AWT thread if the component is realized. To work
  // around this problem, we currently specify in the Animator's API
  // that between the time it's created and started no redraws will
  // occur.
  protected volatile boolean willSetRenderingThread;

  // Flag for disabling all repaint and resize processing on the AWT
  // thread to avoid application-level deadlocks; only really used for
  // GLCanvas
  protected boolean noAutoRedraw;

  // Offscreen context handling. Offscreen contexts should handle
  // these resize requests in makeCurrent and clear the
  // pendingOffscreenResize flag.
  protected boolean pendingOffscreenResize;
  protected int     pendingOffscreenWidth;
  protected int     pendingOffscreenHeight;

  // Cache of the functions that are available to be called at the current
  // moment in time
  protected FunctionAvailabilityCache functionAvailability;
      
  public GLContext(Component component, GLCapabilities capabilities, GLCapabilitiesChooser chooser) {
    this.component = component;
    try {
      this.capabilities = (GLCapabilities) capabilities.clone();
    } catch (CloneNotSupportedException e) {
      throw new GLException(e);
    }
    this.chooser = chooser;
    gl = createGL();
    functionAvailability = new FunctionAvailabilityCache(this);
  }

  /** Runs the given runnable with this OpenGL context valid. */
  public synchronized void invokeGL(Runnable runnable, boolean isReshape, Runnable initAction) throws GLException {
    Thread currentThread = null;
    
    // Defer JAWT and OpenGL operations until onscreen components are
    // realized
    if (!realized()) {
      realized = EventQueue.isDispatchThread();
    }

    if (!realized() ||
	willSetRenderingThread ||
        (renderingThread != null &&
         renderingThread != (currentThread = Thread.currentThread()))) {
      if (isReshape) {
        deferredReshapeAction = runnable;
      }
      return;
    }

    if (isReshape && noAutoRedraw) {
      // Don't process reshape requests on the AWT thread
      deferredReshapeAction = runnable;
      return;
    }

    if (renderingThread == null || deferredSetRenderingThread) {
      deferredSetRenderingThread = false;
      if (!makeCurrent(initAction)) {
        // Couldn't make the thread current because the component has not yet
        // been visualized, and therefore the context cannot be created.
        // We'll defer any actions until invokeGL() is called again at a time
        // when the component has been visualized.
        if (isReshape) {
          deferredReshapeAction = runnable;
        }
        return;
      }
    }

    // At this point the OpenGL context is current. Offscreen contexts
    // handle resizing the backing bitmap in makeCurrent. Therefore we
    // may need to free and make the context current again if we
    // didn't actually make it current above.
    if (pendingOffscreenResize && renderingThread != null) {
      free();
      if (!makeCurrent(initAction)) {
        throw new GLException("Error while resizing offscreen context");
      }
    }

    boolean caughtException = false;

    try {
      if (deferredReshapeAction != null) {
        deferredReshapeAction.run();
        deferredReshapeAction = null;
      }
      runnable.run();
      swapBuffers();
    } catch (RuntimeException e) {
      caughtException = true;
      throw(e);
    } finally {
      if (caughtException) {
        // Disallow setRenderingThread if display action is throwing exceptions
        renderingThread = null;
      }
      if (renderingThread == null) {
        free();
      }
    }
  }

  public GL getGL() {
    return gl;
  }

  public void setGL(GL gl) {
    this.gl = gl;
  }

  public GLU getGLU() {    
    return glu;
  }
  
  public void setGLU(GLU glu) {
    this.glu = glu;
  }
  
  /** Gives a hint to the context that setRenderingThread will be
      called in the near future; causes redraws to be halted. This is
      a workaround for bugs in NVidia's drivers and is used only by
      the Animator class. */
  public synchronized void willSetRenderingThread() {
    this.willSetRenderingThread = true;
  }

  public synchronized void setRenderingThread(Thread currentThreadOrNull, Runnable initAction) {
    Thread currentThread = Thread.currentThread();
    if (currentThreadOrNull != null && currentThreadOrNull != currentThread) {
      throw new GLException("Argument must be either the current thread or null");
    }
    if (renderingThread != null && currentThreadOrNull != null) {
      throw new GLException("Attempt to re-set or change rendering thread");
    }
    if (renderingThread == null && currentThreadOrNull == null) {
      throw new GLException("Attempt to clear rendering thread when already cleared");
    }
    this.willSetRenderingThread = false;
    if (currentThreadOrNull == null) {
      renderingThread = null;
      if (realized()) {
        // We are guaranteed to own the context
        free();
      }
    } else {
      renderingThread = currentThreadOrNull;
      if (realized()) {
        makeCurrent(initAction);
      }
    }
  }

  public Thread getRenderingThread() {
    return renderingThread;
  }

  public void setNoAutoRedrawMode(boolean noAutoRedraw) {
    this.noAutoRedraw = noAutoRedraw;
  }

  public boolean getNoAutoRedrawMode() {
    return noAutoRedraw;
  }

  /** Routine needed only for offscreen contexts in order to resize
      the underlying bitmap. Called by GLJPanel. */
  public void resizeOffscreenContext(int newWidth, int newHeight) {
    if (!isOffscreen()) {
      throw new GLException("Should only call for offscreen OpenGL contexts");
    }
    pendingOffscreenResize = true;
    pendingOffscreenWidth  = newWidth;
    pendingOffscreenHeight = newHeight;
  }

  /** Returns a non-null (but possibly empty) string containing the
      space-separated list of available platform-dependent (e.g., WGL,
      GLX) extensions. Can only be called while this context is
      current. */
  public abstract String getPlatformExtensionsString();

  /**
   * Resets the cache of which GL functions are available for calling through this
   * context. See {@link #isFunctionAvailable(String)} for more information on
   * the definition of "available".
   */
  protected void resetGLFunctionAvailability() {
    functionAvailability.flush();
  }

  /**
   * Returns true if the specified OpenGL core- or extension-function can be
   * successfully called using this GL context given the current host (OpenGL
   * <i>client</i>) and display (OpenGL <i>server</i>) configuration.
   *
   * See {@link GL#isFunctionAvailable(String)} for more details.
   *
   * @param glFunctionName the name of the OpenGL function (e.g., use
   * "glPolygonOffsetEXT" to check if the {@link
   * net.java.games.jogl.glPolygonOffsetEXT(float,float)} is available).
   */
  protected boolean isFunctionAvailable(String glFunctionName) {
    return functionAvailability.isFunctionAvailable(mapToRealGLFunctionName(glFunctionName));
  }

  /**
   * Returns true if the specified OpenGL extension can be
   * successfully called using this GL context given the current host (OpenGL
   * <i>client</i>) and display (OpenGL <i>server</i>) configuration.
   *
   * See {@link GL#isExtensionAvailable(String)} for more details.
   *
   * @param glExtensionName the name of the OpenGL extension (e.g.,
   * "GL_VERTEX_PROGRAM_ARB").
   */
  public boolean isExtensionAvailable(String glExtensionName) {
    return functionAvailability.isExtensionAvailable(mapToRealGLExtensionName(glExtensionName));
  }
  
  /**
   * Pbuffer support; indicates whether this context is capable of
   * creating a subordinate pbuffer context (distinct from an
   * "offscreen context", which is typically software-rendered on all
   * platforms).
   */
  public abstract boolean canCreatePbufferContext();

  /**
   * Pbuffer support; creates a subordinate GLContext for a pbuffer
   * associated with this context.
   */
  public abstract GLContext createPbufferContext(GLCapabilities capabilities,
                                                 int initialWidth,
                                                 int initialHeight);

  /**
   * Pbuffer support; given that this is a GLContext associated with a
   * pbuffer, binds this pbuffer to its texture target.
   */
  public abstract void bindPbufferToTexture();

  /**
   * Pbuffer support; given that this is a GLContext associated with a
   * pbuffer, releases this pbuffer from its texture target.
   */
  public abstract void releasePbufferFromTexture();

  /** Maps the given "platform-independent" function name to a real function
      name. Currently this is only used to map "glAllocateMemoryNV" and
      associated routines to wglAllocateMemoryNV / glXAllocateMemoryNV. */
  protected abstract String mapToRealGLFunctionName(String glFunctionName);

  /** Maps the given "platform-independent" extension name to a real
      function name. Currently this is only used to map
      "GL_ARB_pbuffer" and "GL_ARB_pixel_format" to "WGL_ARB_pbuffer"
      and "WGL_ARB_pixel_format" (not yet mapped to X11). */
  protected abstract String mapToRealGLExtensionName(String glExtensionName);

  /** Create the GL for this context. */
  protected abstract GL createGL();
  
  /** Hook indicating whether the concrete GLContext implementation is
      offscreen and therefore whether we need to process resize
      requests. */
  protected abstract boolean isOffscreen();

  /** Only called for offscreen contexts; returns the type of
      BufferedImage required for reading this context's pixels. */
  public abstract int getOffscreenContextBufferedImageType();

  /** Only called for offscreen contexts; returns the buffer from
      which to read pixels (GL.GL_FRONT or GL.GL_BACK). */
  public abstract int getOffscreenContextReadBuffer();

  /** On some platforms the mismatch between OpenGL's coordinate
      system (origin at bottom left) and the window system's
      coordinate system (origin at top left) necessitates a vertical
      flip of pixels read from offscreen contexts. */
  public abstract boolean offscreenImageNeedsVerticalFlip();

  /** Attempts to make the GL context current. If necessary, creates a
      context and calls the initAction once the context is current.
      Most error conditions cause an exception to be thrown, except
      for the case where the context can not be created because the
      component has not yet been visualized. In this case makeCurrent
      returns false and the caller should abort any OpenGL event
      processing and instead return immediately.  */
  protected abstract boolean makeCurrent(Runnable initAction) throws GLException;

  /** Frees the OpenGL context. All error conditions cause a
      GLException to be thrown. */
  protected abstract void free() throws GLException;

  /** Swaps the buffers of the OpenGL context if necessary. All error
      conditions cause a GLException to be thrown. */
  protected abstract void swapBuffers() throws GLException;

  //----------------------------------------------------------------------
  // Internals only below this point
  //
  private boolean realized() {
    return ((component == null) || realized || component.isDisplayable());
  }
}

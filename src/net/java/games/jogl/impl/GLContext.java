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
import net.java.games.jogl.*;
import net.java.games.gluegen.runtime.*;

public abstract class GLContext {
  protected static final boolean DEBUG = Debug.debug("GLContext");

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
  // addNotify() is called on the component.
  protected boolean realized;

  protected GLCapabilities capabilities;
  protected GLCapabilitiesChooser chooser;
  protected GL gl;
  protected static final GLUProcAddressTable gluProcAddressTable = new GLUProcAddressTable();
  protected static boolean haveResetGLUProcAddressTable;
  protected GLU glu = new GLUImpl(gluProcAddressTable);
  protected Thread renderingThread;
  protected Runnable deferredReshapeAction;
  // Support for OpenGL context destruction and recreation in the face
  // of the setRenderingThread optimization, which makes the context
  // permanently current on the animation thread. FIXME: should make
  // this more uniform and general, possibly by implementing in terms
  // of Runnables; however, necessary sequence of operations in
  // invokeGL makes this tricky.
  protected boolean deferredDestroy;
  protected boolean deferredSetRealized;

  // Error checking for setRenderingThread to ensure that one thread
  // doesn't attempt to call setRenderingThread on more than one
  // drawable
  protected static final ThreadLocal perThreadRenderingContext = new ThreadLocal();

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

  // Flag for enabling / disabling automatic swapping of the front and
  // back buffers
  protected boolean autoSwapBuffers = true;

  // Offscreen context handling. Offscreen contexts should handle
  // these resize requests in makeCurrent and clear the
  // pendingOffscreenResize flag.
  protected boolean pendingOffscreenResize;
  protected int     pendingOffscreenWidth;
  protected int     pendingOffscreenHeight;

  // Cache of the functions that are available to be called at the current
  // moment in time
  protected FunctionAvailabilityCache functionAvailability;

  // Support for recursive makeCurrent() calls as well as calling
  // other drawables' display() methods from within another one's
  protected static final ThreadLocal perThreadContextStack = new ThreadLocal() {
      protected synchronized Object initialValue() {
        return new GLContextStack();
      }
    };
  // This thread-local variable helps implement setRenderingThread()'s
  // optimized context handling. When the bottommost invokeGL() on the
  // execution stack finishes for the rendering thread for that
  // context, we pop the context off the context stack but do not free
  // it, instead storing it in this thread-local variable. This gives
  // us enough information to recover the context stack state in
  // subsequent invokeGL() calls.
  protected static final ThreadLocal perThreadSavedCurrentContext = new ThreadLocal() {
      protected synchronized Object initialValue() {
        return new GLContextInitActionPair(null, null);
      }
    };
      
  public GLContext(Component component,
                   GLCapabilities capabilities,
                   GLCapabilitiesChooser chooser,
                   GLContext shareWith) {
    this.component = component;
    this.capabilities = (GLCapabilities) capabilities.clone();
    this.chooser = chooser;
    setGL(createGL());
    functionAvailability = new FunctionAvailabilityCache(this);
    if (shareWith != null) {
      GLContextShareSet.registerSharing(this, shareWith);
    }
  }

  /** Runs the given runnable with this OpenGL context valid. */
  public synchronized void invokeGL(Runnable runnable, boolean isReshape, Runnable initAction) throws GLException {
    // Could be more clever about not calling this every time, but
    // Thread.currentThread() is very fast and this makes the logic simpler
    Thread currentThread = Thread.currentThread();
    
    // Defer JAWT and OpenGL operations until onscreen components are
    // realized
    if (!isRealized() ||
	willSetRenderingThread ||
        (renderingThread != null &&
         renderingThread != currentThread)) {
      // Support for removeNotify()/addNotify() when the
      // setRenderingThread optimization is in effect and before the
      // animation thread gets a chance to handle either request
      if (!isRealized() && deferredSetRealized) {
        setRealized();
        deferredSetRealized = false;
      } else {
        if (isReshape) {
          deferredReshapeAction = runnable;
        }
        return;
      }
    }

    if (isReshape && noAutoRedraw && !SingleThreadedWorkaround.doWorkaround()) {
      // Don't process reshape requests on the AWT thread
      deferredReshapeAction = runnable;
      return;
    }

    if (deferredDestroy) {
      deferredDestroy = false;
      if (renderingThread != null) {
        // Need to disable the setRenderingThread optimization to free
        // up the context
        setRenderingThread(null, initAction);
      }
      destroy();
      return;
    }

    // The goal of this code is to optimize OpenGL context handling as
    // much as possible. In particular:
    //
    // - setRenderingThread() works by making the "bottommost" OpenGL
    //   context current once and not freeing it until the rendering
    //   thread has been unset. Note that subsequent pushes of other
    //   contexts will still necessarily cause them to be made current
    //   and freed.
    //
    // - If the same context is pushed on the per-thread context stack
    //   more than once back-to-back, the subsequent pushes will not
    //   actually cause a makeCurrent/free to occur.
    //
    // Complexities occur because setRenderingThread() can be called
    // at any time. Currently we implement the rendering thread
    // optimization by popping it off the OpenGL context stack and
    // storing it in a thread-local variable.

    GLContextStack ctxStack = getPerThreadContextStack();
    GLContext savedPerThreadContext = getPerThreadSavedCurrentContext();
    Runnable savedPerThreadInitAction = getPerThreadSavedInitAction();
    setPerThreadSavedCurrentContext(null, null);
    if (ctxStack.size() == 0 &&
        savedPerThreadContext != null) {
      // The setRenderingThread optimization moved the current context
      // into thread-local storage. Put it back on the context stack,
      // because we might need to free it later.
      ctxStack.push(savedPerThreadContext, savedPerThreadInitAction);
    }

    GLContext curContext = ctxStack.peekContext();
    Runnable  curInitAction = ctxStack.peekInitAction();
    boolean mustDoMakeCurrent = true;

    if (curContext == this) {
      mustDoMakeCurrent = false;
    }

    if (mustDoMakeCurrent) {
      if (curContext != null) {
        if (DEBUG) {
          System.err.println("Freeing context " + curContext + " due to recursive makeCurrent");
        }
        curContext.free();
      }

      if (!makeCurrent(initAction)) {
        // Couldn't make the thread current because the component has not yet
        // been visualized, and therefore the context cannot be created.
        // We'll defer any actions until invokeGL() is called again at a time
        // when the component has been visualized.
        if (isReshape) {
          deferredReshapeAction = runnable;
        }

        // Clean up after ourselves on the way out.
        // NOTE that this is an abbreviated version of the code below
        // and should probably be refactored/cleaned up -- this bug
        // fix was done without a lot of intense thought about the
        // situation
        if (curContext != null) {
          curContext.makeCurrent(curInitAction);
        }
        return;
      }
      if (DEBUG) {
        System.err.println("Making context " + this + " current");
      }
    }
    ctxStack.push(this, initAction);

    // At this point the OpenGL context is current. Offscreen contexts
    // handle resizing the backing bitmap in makeCurrent. Therefore we
    // may need to free and make the context current again if we
    // didn't actually make it current above.
    if (pendingOffscreenResize && renderingThread != null) {
      ctxStack.pop();
      free();
      if (!makeCurrent(initAction)) {
        throw new GLException("Error while resizing offscreen context");
      }
      ctxStack.push(this, initAction);
    }

    RuntimeException userException = null;
    GLException internalException  = null;

    try {
      if (deferredReshapeAction != null) {
        deferredReshapeAction.run();
        deferredReshapeAction = null;
      }
      runnable.run();
      if (autoSwapBuffers && !isReshape) {
        swapBuffers();
      }
    } catch (RuntimeException e) {
      userException = e;
      throw(userException);
    } finally {
      if (userException != null) {
        // Disallow setRenderingThread if display action is throwing exceptions
        renderingThread = null;
      }

      boolean mustSkipFreeForRenderingThread = false;
      if (currentThread == renderingThread && curContext == null) {
        mustSkipFreeForRenderingThread = true;
        setPerThreadSavedCurrentContext(this, initAction);
      }
    
      // Always pop myself off the per-thread context stack
      ctxStack.pop();

      // Free the context unless the setRenderingThread optimization
      // kicks in.
      if (mustDoMakeCurrent && !mustSkipFreeForRenderingThread) {
        if (DEBUG) {
          System.err.println("Freeing context " + this);
        }

        try {
          free();
        } catch (GLException e) {
          internalException = e;
        }

        if (curContext != null) {
          if (DEBUG) {
            System.err.println("Making context " + curContext + " current again");
          }
          try {
            curContext.makeCurrent(curInitAction);
          } catch (GLException e) {
            internalException = e;
          }
        }
      }

      // Check to see whether we pushed any remaining entry on the
      // per-thread context stack. If so, put it back in thread-local
      // storage unless the rendering thread optimization was recently
      // disabled.
      if (savedPerThreadContext != null) {
        assert(savedPerThreadContext == curContext);
        ctxStack.pop();
        if (savedPerThreadContext.getRenderingThread() == null) {
          try {
            savedPerThreadContext.free();
          } catch (GLException e) {
            internalException = e;
          }
        } else {
          setPerThreadSavedCurrentContext(savedPerThreadContext, savedPerThreadInitAction);
        }
      }

      // Make sure the end user's exception shows up in any stack
      // traces; the rethrow of the userException above should take
      // precedence if the internalException will otherwise squelch it
      if (internalException != null) {
        if (userException != null &&
            internalException.getCause() == null) {
          internalException.initCause(userException);
          throw(internalException);
        } else if (userException == null) {
          throw(internalException);
        }
      }
    }
  }

  public GL getGL() {
    return gl;
  }

  public void setGL(GL gl) {
    this.gl = gl;
    // Also reset the GL object for the pure-Java GLU implementation
    ((GLUImpl) glu).setGL(gl);
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
    if (SingleThreadedWorkaround.doWorkaround()) {
      willSetRenderingThread = false;
      return;
    }

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

    Object currentThreadRenderingContext = perThreadRenderingContext.get();
    if (currentThreadOrNull != null &&
        currentThreadRenderingContext != null &&
        currentThreadRenderingContext != this) {
      throw new GLException("Attempt to call setRenderingThread on more than one drawable in this thread");
    }

    this.willSetRenderingThread = false;
    if (currentThreadOrNull == null) {
      renderingThread = null;
      perThreadRenderingContext.set(null);
      // Just in case the end user wasn't planning on drawing the
      // drawable even once more (which would give us a chance to free
      // the context), try to free the context now by performing an
      // invokeGL with a do-nothing action
      invokeGL(new Runnable() {
          public void run() {
          }
        }, false, initAction);
    } else {
      renderingThread = currentThreadOrNull;
      perThreadRenderingContext.set(this);
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

  public void setAutoSwapBufferMode(boolean autoSwapBuffers) {
    this.autoSwapBuffers = autoSwapBuffers;
  }

  public boolean getAutoSwapBufferMode() {
    return autoSwapBuffers;
  }

  /** Swaps the buffers of the OpenGL context if necessary. All error
      conditions cause a GLException to be thrown. */
  public abstract void swapBuffers() throws GLException;

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
    if (!haveResetGLUProcAddressTable) {
      if (DEBUG) {
        System.err.println("!!! Initializing GLU extension address table");
      }
      resetProcAddressTable(gluProcAddressTable);
      haveResetGLUProcAddressTable = true; // Only need to do this once globally
    }
    recomputeSingleThreadedWorkaround();
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
   * net.java.games.jogl.GL#glPolygonOffsetEXT(float,float)} is available).
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

  /** Only called for offscreen contexts; needed by glReadPixels */
  public abstract int getOffscreenContextWidth();
  
  /** Only called for offscreen contexts; needed by glReadPixels */
  public abstract int getOffscreenContextHeight();
  
  /** Only called for offscreen contexts; needed by glReadPixels */
  public abstract int getOffscreenContextPixelDataType();
  
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

  /** Inform the system that the associated heavyweight widget has
      been realized and that it is safe to create an associated OpenGL
      context. If the widget is later destroyed then destroy() should
      be called, which will cause the underlying OpenGL context to be
      destroyed as well as the realized bit to be set to false. */
  public void setRealized() {
    if (getRenderingThread() != null &&
        Thread.currentThread() != getRenderingThread()) {
      deferredSetRealized = true;
      return;
    }
    setRealized(true);
  }

  /** Sets only the "realized" bit. Should be called by subclasses
      from within the destroy() implementation. */
  protected synchronized void setRealized(boolean realized) {
    this.realized = realized;
    if (DEBUG) {
      System.err.println("GLContext.setRealized(" + realized + ") for context " + this);
    }
  }

  /** Indicates whether the component associated with this context has
      been realized. */
  public synchronized boolean getRealized() {
    return realized;
  }

  /** Destroys the underlying OpenGL context and changes the realized
      state to false. This should be called when the widget is being
      destroyed. */
  public synchronized void destroy() throws GLException {
    if (getRenderingThread() != null &&
        Thread.currentThread() != getRenderingThread()) {
      deferredDestroy = true;
      return;
    }
    setRealized(false);
    GLContextShareSet.contextDestroyed(this);
    destroyImpl();
  }

  /** Destroys the underlying OpenGL context. */
  protected abstract void destroyImpl() throws GLException;

  public synchronized boolean isRealized() {
    return (component == null || getRealized());
  }

  /** Helper routine which resets a ProcAddressTable generated by the
      GLEmitter by looking up anew all of its function pointers. */
  protected void resetProcAddressTable(Object table) {
    Class tableClass = table.getClass();
    java.lang.reflect.Field[] fields = tableClass.getDeclaredFields();
    
    for (int i = 0; i < fields.length; ++i) {
      String addressFieldName = fields[i].getName();
      if (!addressFieldName.startsWith(ProcAddressHelper.PROCADDRESS_VAR_PREFIX)) {
        // not a proc address variable
        continue;
      }
      int startOfMethodName = ProcAddressHelper.PROCADDRESS_VAR_PREFIX.length();
      String glFuncName = addressFieldName.substring(startOfMethodName);
      try {
        java.lang.reflect.Field addressField = tableClass.getDeclaredField(addressFieldName);
        assert(addressField.getType() == Long.TYPE);
        long newProcAddress = dynamicLookupFunction(glFuncName);
        // set the current value of the proc address variable in the table object
        addressField.setLong(table, newProcAddress); 
        if (DEBUG) {
          //          System.err.println(glFuncName + " = 0x" + Long.toHexString(newProcAddress));
        }
      } catch (Exception e) {
        throw new GLException("Cannot get GL proc address for method \"" +
                              glFuncName + "\": Couldn't set value of field \"" + addressFieldName +
                              "\" in class " + tableClass.getName(), e);
      }
    }
  }

  /** Dynamically looks up the given function. */
  protected abstract long dynamicLookupFunction(String glFuncName);

  /** Indicates whether the underlying OpenGL context has been
      created. This is used to manage sharing of display lists and
      textures between contexts. */
  public abstract boolean isCreated();

  /** Support for recursive makeCurrent() calls as well as calling
      other drawables' display() methods from within another one's */
  protected static GLContextStack getPerThreadContextStack() {
    return (GLContextStack) perThreadContextStack.get();
  }

  /** Support for setRenderingThread()'s optimized context handling */
  protected static GLContext getPerThreadSavedCurrentContext() {
    return ((GLContextInitActionPair) perThreadSavedCurrentContext.get()).getContext();
  }

  /** Support for setRenderingThread()'s optimized context handling */
  protected static Runnable getPerThreadSavedInitAction() {
    return ((GLContextInitActionPair) perThreadSavedCurrentContext.get()).getInitAction();
  }

  /** Support for setRenderingThread()'s optimized context handling */
  protected static void setPerThreadSavedCurrentContext(GLContext context, Runnable initAction) {
    perThreadSavedCurrentContext.set(new GLContextInitActionPair(context, initAction));
  }

  /** Support for automatic detection of whether we need to enable the
      single-threaded workaround for ATI and other vendors' cards.
      Should be called by subclasses for onscreen rendering inside
      their makeCurrent() implementation once the context is
      current. */
  private void recomputeSingleThreadedWorkaround() {
    GL gl = getGL();
    String str = gl.glGetString(GL.GL_VENDOR);
    if (str != null && str.indexOf("ATI") >= 0) {
      // Doing this instead of calling setRenderingThread(null) should
      // be OK since we are doing this very early in the maintenance
      // of the per-thread context stack, before we are actually
      // pushing any GLContext objects on it
      SingleThreadedWorkaround.shouldDoWorkaround();
      if( SingleThreadedWorkaround.doWorkaround() ) {
        renderingThread = null;
      }
    }
  }
}

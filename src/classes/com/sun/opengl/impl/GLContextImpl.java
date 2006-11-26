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

package com.sun.opengl.impl;

import java.awt.Component;
import java.nio.*;

import javax.media.opengl.*;
import com.sun.gluegen.runtime.*;

public abstract class GLContextImpl extends GLContext {
  protected GLContextLock lock = new GLContextLock();
  protected static final boolean DEBUG = Debug.debug("GLContextImpl");
  protected static final boolean VERBOSE = Debug.verbose();
  protected static final boolean NO_FREE = Debug.isPropertyDefined("jogl.GLContext.nofree");
  // NOTE: default sense of GLContext optimization disabled in JSR-231
  // 1.0 beta 5 due to problems on X11 platforms (both Linux and
  // Solaris) when moving and resizing windows. Apparently GLX tokens
  // get sent to the X server under the hood (and out from under the
  // cover of the AWT lock) in these situations. Users requiring
  // multi-screen X11 applications can manually enable this flag. It
  // basically had no tangible effect on the Windows or Mac OS X
  // platforms anyway in particular with the disabling of the
  // GLWorkerThread which we found to be necessary in 1.0 beta 4.
  protected boolean optimizationEnabled = Debug.isPropertyDefined("jogl.GLContext.optimize");

  // Cache of the functions that are available to be called at the current
  // moment in time
  protected FunctionAvailabilityCache functionAvailability;
  // Table that holds the addresses of the native C-language entry points for
  // OpenGL functions.
  private GLProcAddressTable glProcAddressTable;

  // Tracks creation and initialization of buffer objects to avoid
  // repeated glGet calls upon glMapBuffer operations
  private GLBufferSizeTracker bufferSizeTracker;

  // Tracks creation and deletion of server-side OpenGL objects when
  // the Java2D/OpenGL pipeline is active and using FBOs to render
  private GLObjectTracker tracker;
  // Supports deletion of these objects when no other context is
  // current which can support immediate deletion of them
  private GLObjectTracker deletedObjectTracker;

  protected GL gl;
  public GLContextImpl(GLContext shareWith) {
    this(shareWith, false);
  }

  public GLContextImpl(GLContext shareWith, boolean dontShareWithJava2D) {
    functionAvailability = new FunctionAvailabilityCache(this);
    GLContext shareContext = shareWith;
    if (!dontShareWithJava2D) {
      shareContext = Java2D.filterShareContext(shareWith);
    }
    if (shareContext != null) {
      GLContextShareSet.registerSharing(this, shareContext);
    }
    // Always indicate real behind-the-scenes sharing to track deleted objects
    if (shareContext == null) {
      shareContext = Java2D.filterShareContext(shareWith);
    }
    GLContextShareSet.registerForObjectTracking(shareWith, this, shareContext);
    GLContextShareSet.registerForBufferObjectSharing(shareWith, this);
    // This must occur after the above calls into the
    // GLContextShareSet, which set up state needed by the GL object
    setGL(createGL());
  }

  public int makeCurrent() throws GLException {
    // Support calls to makeCurrent() over and over again with
    // different contexts without releasing them
    // Could implement this more efficiently without explicit
    // releasing of the underlying context; would require more error
    // checking during the makeCurrentImpl phase
    GLContext current = getCurrent();
    if (current != null) {
      if (current == this) {
        // Assume we don't need to make this context current again
        // For Mac OS X, however, we need to update the context to track resizes
        update();
        return CONTEXT_CURRENT;
      } else {
        current.release();
      }
    }

    if (GLWorkerThread.isStarted() &&
        !GLWorkerThread.isWorkerThread()) {
      // Kick the GLWorkerThread off its current context
      GLWorkerThread.invokeLater(new Runnable() { public void run() {} });
    }

    lock.lock();
    int res = 0;
    try {
      res = makeCurrentImpl();
      if ((tracker != null) &&
          (res == CONTEXT_CURRENT_NEW)) {
        // Increase reference count of GLObjectTracker
        tracker.ref();
      }
    } catch (GLException e) {
      lock.unlock();
      throw(e);
    }
    if (res == CONTEXT_NOT_CURRENT) {
      lock.unlock();
    } else {
      setCurrent(this);

      // Try cleaning up any stale server-side OpenGL objects
      // FIXME: not sure what to do here if this throws
      if (deletedObjectTracker != null) {
        deletedObjectTracker.clean(getGL());
      }
    }
    return res;
  }

  protected abstract int makeCurrentImpl() throws GLException;

  public void release() throws GLException {
    if (!lock.isHeld()) {
      throw new GLException("Context not current on current thread");
    }
    setCurrent(null);
    try {
      releaseImpl();
    } finally {
      lock.unlock();
    }
  }

  protected abstract void releaseImpl() throws GLException;

  public void destroy() {
    if (lock.isHeld()) {
      throw new GLException("Can not destroy context while it is current");
    }

    if (tracker != null) {
      // Don't need to do anything for contexts that haven't been
      // created yet
      if (isCreated()) {
        // If we are tracking creation and destruction of server-side
        // OpenGL objects, we must decrement the reference count of the
        // GLObjectTracker upon context destruction.
        //
        // Note that we can only eagerly delete these server-side
        // objects if there is another context currrent right now
        // which shares textures and display lists with this one.
        tracker.unref(deletedObjectTracker);
      }
    }

    // Because we don't know how many other contexts we might be
    // sharing with (and it seems too complicated to implement the
    // GLObjectTracker's ref/unref scheme for the buffer-related
    // optimizations), simply clear the cache of known buffers' sizes
    // when we destroy contexts
    bufferSizeTracker.clearCachedBufferSizes();

    // Must hold the lock around the destroy operation to make sure we
    // don't destroy the context out from under another thread rendering to it
    lock.lock();
    try {
      destroyImpl();
    } finally {
      lock.unlock();
    }
  }

  protected abstract void destroyImpl() throws GLException;

  // This is only needed for Mac OS X on-screen contexts
  protected void update() throws GLException {
  }

  public boolean isSynchronized() {
    return !lock.getFailFastMode();
  }

  public void setSynchronized(boolean isSynchronized) {
    lock.setFailFastMode(!isSynchronized);
  }

  public GL getGL() {
    return gl;
  }

  public void setGL(GL gl) {
    this.gl = gl;
  }

  public abstract Object getPlatformGLExtensions();

  //----------------------------------------------------------------------
  // Helpers for various context implementations
  //

  /** Create the GL for this context. */
  protected GL createGL() {
    GLImpl gl = new GLImpl(this);
    if (tracker != null) {
      gl.setObjectTracker(tracker);
    }
    return gl;
  }
  
  public GLProcAddressTable getGLProcAddressTable() {
    if (glProcAddressTable == null) {
      // FIXME: cache ProcAddressTables by capability bits so we can
      // share them among contexts with the same capabilities
      glProcAddressTable = new GLProcAddressTable();
    }          
    return glProcAddressTable;
  }
  
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

  public abstract ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3);

  /*
   * Sets the swap interval for onscreen OpenGL contexts. Has no
   * effect for offscreen contexts.
   */
  public void setSwapInterval(final int interval) {
  }

  /** Maps the given "platform-independent" function name to a real function
      name. Currently this is only used to map "glAllocateMemoryNV" and
      associated routines to wglAllocateMemoryNV / glXAllocateMemoryNV. */
  protected abstract String mapToRealGLFunctionName(String glFunctionName);

  /** Maps the given "platform-independent" extension name to a real
      function name. Currently this is only used to map
      "GL_ARB_pbuffer" and "GL_ARB_pixel_format" to "WGL_ARB_pbuffer"
      and "WGL_ARB_pixel_format" (not yet mapped to X11). */
  protected abstract String mapToRealGLExtensionName(String glExtensionName);

  /** Returns a non-null (but possibly empty) string containing the
      space-separated list of available platform-dependent (e.g., WGL,
      GLX) extensions. Can only be called while this context is
      current. */
  public abstract String getPlatformExtensionsString();

  /** Helper routine which resets a ProcAddressTable generated by the
      GLEmitter by looking up anew all of its function pointers. */
  protected void resetProcAddressTable(Object table) {
    ProcAddressHelper.resetProcAddressTable(table, GLDrawableFactoryImpl.getFactoryImpl());
  }

  /** Indicates whether the underlying OpenGL context has been
      created. This is used to manage sharing of display lists and
      textures between contexts. */
  public abstract boolean isCreated();

  /**
   * Resets the cache of which GL functions are available for calling through this
   * context. See {@link #isFunctionAvailable(String)} for more information on
   * the definition of "available".
   */
  protected void resetGLFunctionAvailability() {
    // In order to be able to allow the user to uniformly install the
    // debug and trace pipelines in their GLEventListener.init()
    // method (for both GLCanvas and GLJPanel), we need to reset the
    // actual GL object in the GLDrawable as well
    setGL(createGL());

    functionAvailability.flush();
    if (DEBUG) {
      System.err.println(getThreadName() + ": !!! Initializing OpenGL extension address table for " + this);
    }
    resetProcAddressTable(getGLProcAddressTable());
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
   * javax.media.opengl.GL#glPolygonOffsetEXT(float,float)} is available).
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

  /** Indicates which floating-point pbuffer implementation is in
      use. Returns one of GLPbuffer.APPLE_FLOAT, GLPbuffer.ATI_FLOAT,
      or GLPbuffer.NV_FLOAT. */
  public int getFloatingPointMode() throws GLException {
    throw new GLException("Not supported on non-pbuffer contexts");
  }

  /** On some platforms the mismatch between OpenGL's coordinate
      system (origin at bottom left) and the window system's
      coordinate system (origin at top left) necessitates a vertical
      flip of pixels read from offscreen contexts. */
  public abstract boolean offscreenImageNeedsVerticalFlip();

  /** Only called for offscreen contexts; needed by glReadPixels */
  public abstract int getOffscreenContextPixelDataType();

  protected static String getThreadName() {
    return Thread.currentThread().getName();
  }

  public static String toHexString(long hex) {
    return "0x" + Long.toHexString(hex);
  }

  //----------------------------------------------------------------------
  // Helpers for buffer object optimizations
  
  public void setBufferSizeTracker(GLBufferSizeTracker bufferSizeTracker) {
    this.bufferSizeTracker = bufferSizeTracker;
  }

  public GLBufferSizeTracker getBufferSizeTracker() {
    return bufferSizeTracker;
  }

  //---------------------------------------------------------------------------
  // Helpers for integration with Java2D/OpenGL pipeline when FBOs are
  // being used
  //

  public void setObjectTracker(GLObjectTracker tracker) {
    this.tracker = tracker;
  }
  
  public GLObjectTracker getObjectTracker() {
    return tracker;
  }

  public void setDeletedObjectTracker(GLObjectTracker deletedObjectTracker) {
    this.deletedObjectTracker = deletedObjectTracker;
  }

  public GLObjectTracker getDeletedObjectTracker() {
    return deletedObjectTracker;
  }

  //---------------------------------------------------------------------------
  // Helpers for context optimization where the last context is left
  // current on the OpenGL worker thread
  //

  public boolean isOptimizable() {
    return optimizationEnabled;
  }

  public boolean hasWaiters() {
    return lock.hasWaiters();
  }
}

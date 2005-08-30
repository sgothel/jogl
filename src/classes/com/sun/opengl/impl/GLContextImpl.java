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

  // Cache of the functions that are available to be called at the current
  // moment in time
  protected FunctionAvailabilityCache functionAvailability;
  // Table that holds the addresses of the native C-language entry points for
  // OpenGL functions.
  private GLProcAddressTable glProcAddressTable;

  protected GL gl;
  protected GLU glu = new GLUImpl(gluProcAddressTable);
  protected static final GLUProcAddressTable gluProcAddressTable = new GLUProcAddressTable();
  protected static boolean haveResetGLUProcAddressTable;

  public GLContextImpl(GLContext shareWith) {
    setGL(createGL());
    functionAvailability = new FunctionAvailabilityCache(this);
    if (shareWith != null) {
      GLContextShareSet.registerSharing(this, shareWith);
    }
  }

  public int makeCurrent() throws GLException {
    lock.lock();
    int res = 0;
    try {
      res = makeCurrentImpl();
    } catch (GLException e) {
      lock.unlock();
      throw(e);
    }
    if (res == CONTEXT_NOT_CURRENT) {
      lock.unlock();
    } else {
      setCurrent(this);
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
    // Should we check the lock state? It should not be current on any
    // thread.
    destroyImpl();
  }

  protected abstract void destroyImpl() throws GLException;

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
    // Also reset the GL object for the pure-Java GLU implementation
    ((GLUImpl) glu).setGL(gl);
  }

  public GLU getGLU() {    
    return glu;
  }
  
  public void setGLU(GLU glu) {
    this.glu = glu;
  }

  public abstract Object getPlatformGLExtensions();

  //----------------------------------------------------------------------
  // Helpers for various context implementations
  //

  /** Create the GL for this context. */
  protected GL createGL() {
    return new GLImpl(this);
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
    GLDrawableFactoryImpl.getFactoryImpl().resetProcAddressTable(table);
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
    if (!haveResetGLUProcAddressTable) {
      if (DEBUG) {
        System.err.println(getThreadName() + ": !!! Initializing GLU extension address table");
      }
      resetProcAddressTable(gluProcAddressTable);
      haveResetGLUProcAddressTable = true; // Only need to do this once globally
    }
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
}

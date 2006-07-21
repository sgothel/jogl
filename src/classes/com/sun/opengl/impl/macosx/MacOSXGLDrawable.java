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

package com.sun.opengl.impl.macosx;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public abstract class MacOSXGLDrawable extends GLDrawableImpl {
  protected static final boolean DEBUG = Debug.debug("MacOSXGLDrawable");

  protected GLCapabilities capabilities;
  protected GLCapabilitiesChooser chooser;
  
  // The Java2D/OpenGL pipeline on OS X uses low-level CGLContextObjs
  // to represent the contexts for e.g. the Java2D back buffer. When
  // the Java2D/JOGL bridge is active, this means that if we want to
  // be able to share textures and display lists with the Java2D
  // contexts, we need to use the CGL APIs rather than the NSOpenGL
  // APIs on the JOGL side. For example, if we create a pbuffer using
  // the NSOpenGL APIs and want to share textures and display lists
  // between it and the Java2D back buffer, there is no way to do so,
  // because the Java2D context is actually a CGLContextObj and the
  // NSOpenGLContext's initWithFormat:shareContext: only accepts an
  // NSOpenGLContext as its second argument. Of course there is no way
  // to wrap an NSOpenGLContext around an arbitrary CGLContextObj.
  //
  // The situation we care most about is allowing a GLPbuffer to share
  // textures, etc. with a GLJPanel when the Java2D/JOGL bridge is
  // active; several of the demos rely on this functionality. We aim
  // to get there by allowing a GLPBuffer to switch its implementation
  // between using an NSOpenGLPixelBuffer and a CGLPBufferObj. In
  // order to track whether this has been done we need to have the
  // notion of a "mode" of both the MacOSXGLDrawable and the
  // MacOSXGLContext. Initially the mode is "unspecified", meaning it
  // leans toward the default (NSOpenGL). If sharing is requested
  // between either a GLJPanel and a GLPbuffer or a GLCanvas and a
  // GLPbuffer, the GLPbuffer will be switched into the appropriate
  // mode: CGL mode for a GLJPanel and NSOpenGL mode for a GLCanvas.
  // To avoid thrashing we support exactly one such switch during the
  // lifetime of a given GLPbuffer. This is not a fully general
  // solution (for example, you can't share textures among a
  // GLPbuffer, a GLJPanel and a GLCanvas simultaneously) but should
  // be enough to get things off the ground.
  public static final int NSOPENGL_MODE = 1;
  public static final int CGL_MODE      = 2;

  public MacOSXGLDrawable(GLCapabilities capabilities,
                          GLCapabilitiesChooser chooser) {
    this.capabilities = (GLCapabilities) capabilities.clone();
    this.chooser = chooser;
 }

  public void setRealized(boolean val) {
    throw new GLException("Should not call this (should only be called for onscreen GLDrawables)");
  }

  public void destroy() {
    throw new GLException("Should not call this (should only be called for offscreen GLDrawables)");
  }

  public void swapBuffers() throws GLException {
  }

  public GLCapabilities getCapabilities() {
	int numFormats = 1;
	GLCapabilities availableCaps[] = new GLCapabilities[numFormats];
	availableCaps[0] = capabilities;
	int pixelFormat = chooser.chooseCapabilities(capabilities, availableCaps, 0);
	if ((pixelFormat < 0) || (pixelFormat >= numFormats)) {
      throw new GLException("Invalid result " + pixelFormat +
							  " from GLCapabilitiesChooser (should be between 0 and " +
							  (numFormats - 1) + ")");
	}
	if (DEBUG) {
      System.err.println(getThreadName() + ": Chosen pixel format (" + pixelFormat + "):");
      System.err.println(availableCaps[pixelFormat]);
	}
    return availableCaps[pixelFormat];
  }

  // Only used for on-screen contexts
  public long getView() {
    return 0;
  }
  
  protected static String getThreadName() {
    return Thread.currentThread().getName();
  }

  // Support for "mode switching" as per above
  public abstract void setOpenGLMode(int mode);
  public abstract int  getOpenGLMode();
}

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

package net.java.games.jogl.impl.macosx;

import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

/** This MacOSXGLContext implementation provides interoperability with
    the NSOpenGLView Cocoa widget. The MacOSXGLImpl can be
    instantiated without a GLContext, in which case it expects that
    the end user will handle all OpenGL context management. Dynamic
    function lookup is supported in this configuration by having this
    object provide the FunctionAvailabilityTable. */

class MacOSXDummyGLContext extends MacOSXGLContext
{
  private MacOSXGLImpl gl;

  MacOSXDummyGLContext(MacOSXGLImpl gl) {
    super(null, null, null, null);
    this.gl = gl;
  }
	
  protected GL createGL() {
    return gl;
  }
	
  protected boolean isOffscreen() {
    return false;
  }
	
  public int getOffscreenContextBufferedImageType() {
    throw new GLException("Should not call this");
  }
	
  public int getOffscreenContextReadBuffer() {
    throw new GLException("Should not call this");
  }
	
  public boolean offscreenImageNeedsVerticalFlip() {
    throw new GLException("Should not call this");
  }
	
  public boolean canCreatePbufferContext() {
    throw new GLException("Should not call this");
  }
	
  public synchronized GLContext createPbufferContext(GLCapabilities capabilities, int initialWidth, int initialHeight) {
    throw new GLException("Should not call this");
  }
	
  public void bindPbufferToTexture() {
    throw new GLException("Should not call this");
  }
	
  public void releasePbufferFromTexture() {
    throw new GLException("Should not call this");
  }
	
  protected synchronized boolean makeCurrent(Runnable initAction) throws GLException {
    throw new GLException("Should not call this");
  }
	
  public synchronized void swapBuffers() throws GLException {
    throw new GLException("Should not call this");
  }
	
  protected synchronized void free() throws GLException {
    throw new GLException("Should not call this");
  }

  protected boolean create() {
    throw new GLException("Should not call this");
  }
	
  public void destroy() {
    throw new GLException("Should not call this");
  }

  public void resetGLFunctionAvailability() {
    super.resetGLFunctionAvailability();
  }
}

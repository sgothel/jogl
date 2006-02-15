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

  protected long nsView; // NSView
  protected GLCapabilities capabilities;
  protected GLCapabilitiesChooser chooser;
  
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

  public long getView() {
    return nsView;
  }
  
  protected static String getThreadName() {
    return Thread.currentThread().getName();
  }
}

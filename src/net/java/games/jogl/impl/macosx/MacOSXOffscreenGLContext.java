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

package net.java.games.jogl.impl.macosx;

import java.awt.image.BufferedImage;
import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class MacOSXOffscreenGLContext extends MacOSXGLContext
{
  // Width and height of the underlying bitmap
  private int  width;
  private int  height;
  
  public MacOSXOffscreenGLContext(GLCapabilities capabilities,
                                  GLCapabilitiesChooser chooser,
                                  GLContext shareWith) {
    super(null, capabilities, chooser, shareWith);
  }
	
  protected GL createGL() {
    return new MacOSXGLImpl(this);
  }
	
  protected boolean isOffscreen() {
    return true;
  }
	
  public int getOffscreenContextBufferedImageType() {
      return BufferedImage.TYPE_INT_ARGB;
  }
	
  public int getOffscreenContextReadBuffer() {
    return GL.GL_FRONT;
  }
	
  public boolean offscreenImageNeedsVerticalFlip() {
    // We can take care of this in the DIB creation (see below)
    return false;
  }
	
  public boolean canCreatePbufferContext() {
    // For now say no
    return false;
  }
	
  public synchronized GLContext createPbufferContext(GLCapabilities capabilities, int initialWidth, int initialHeight) {
    throw new GLException("Not supported");
  }
	
  public void bindPbufferToTexture() {
    throw new GLException("Should not call this");
  }
	
  public void releasePbufferFromTexture() {
    throw new GLException("Should not call this");
  }
	
  protected synchronized boolean makeCurrent(Runnable initAction) throws GLException {
    if (pendingOffscreenResize) {
      if (pendingOffscreenWidth != width || pendingOffscreenHeight != height) {
        if (nsContext != 0) {
          destroy();
        }
        width  = pendingOffscreenWidth;
        height = pendingOffscreenHeight;
        pendingOffscreenResize = false;
      }
    }
    return super.makeCurrent(initAction);
  }
	
  protected synchronized void swapBuffers() throws GLException {
  }
	
  private void destroy() {
	free();
  }
}

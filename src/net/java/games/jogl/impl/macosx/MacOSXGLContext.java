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

import java.awt.Component;
import java.util.*;
import net.java.games.gluegen.opengl.*; // for PROCADDRESS_VAR_PREFIX
import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public abstract class MacOSXGLContext extends GLContext {

  public MacOSXGLContext(Component component, GLCapabilities capabilities, GLCapabilitiesChooser chooser) {
    super(component, capabilities, chooser);
  }
  
  protected GL createGL()
  {
    return new MacOSXGLImpl(this);
  }
  
  protected String mapToRealGLFunctionName(String glFunctionName) {
    return glFunctionName;
  }

  protected abstract boolean isOffscreen();
  
  public abstract int getOffscreenContextBufferedImageType();

  public abstract int getOffscreenContextReadBuffer();

  public abstract boolean offscreenImageNeedsVerticalFlip();

  /**
   * Creates and initializes an appropriate OpenGl context. Should only be
   * called by {@link makeCurrent(Runnable)}.
   */
  protected abstract void create();
  
  protected synchronized boolean makeCurrent(Runnable initAction) throws GLException
  {
	  throw new RuntimeException(" FIXME: not implemented ");
  }

  protected synchronized void free() throws GLException {
   throw new RuntimeException(" FIXME: not implemented ");
   }

  protected abstract void swapBuffers() throws GLException;


  protected void resetGLFunctionAvailability() {
	  throw new RuntimeException(" FIXME: not implemented ");
  }
  
  protected void resetGLProcAddressTable() {
	  throw new RuntimeException(" FIXME: not implemented ");
  }
  
  public net.java.games.jogl.impl.ProcAddressTable getGLProcAddressTable() {
	  throw new RuntimeException(" FIXME: not implemented ");
  }
  
  public String getPlatformExtensionsString() {
	  throw new RuntimeException(" FIXME: not implemented ");
  }

  protected boolean isFunctionAvailable(String glFunctionName)
  {
	  throw new RuntimeException(" FIXME: not implemented ");
  }
  
}

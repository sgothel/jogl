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
import net.java.games.gluegen.runtime.*; // for PROCADDRESS_VAR_PREFIX
import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public abstract class MacOSXGLContext extends GLContext
{	
  private static JAWT jawt;
  protected long nsContext; // NSOpenGLContext
  // Table that holds the addresses of the native C-language entry points for
  // OpenGL functions.
  private GLProcAddressTable glProcAddressTable;
	
  public MacOSXGLContext(Component component, GLCapabilities capabilities, GLCapabilitiesChooser chooser)
  {
    super(component, capabilities, chooser);
  }
	
  protected String mapToRealGLFunctionName(String glFunctionName)
  {
    return glFunctionName;
  }
	
  protected String mapToRealGLExtensionName(String glFunctionName)
  {
    return glFunctionName;
  }
	
  protected boolean isFunctionAvailable(String glFunctionName)
  {
    return super.isFunctionAvailable(glFunctionName);
  }
  
  protected abstract boolean isOffscreen();
	
  public abstract int getOffscreenContextBufferedImageType();

  public abstract int getOffscreenContextReadBuffer();

  public abstract boolean offscreenImageNeedsVerticalFlip();

  /**
   * Creates and initializes an appropriate OpenGl nsContext. Should only be
   * called by {@link makeCurrent(Runnable)}.
   */
  protected abstract void create();
	
  protected abstract boolean makeCurrent(Runnable initAction) throws GLException;
	
  protected abstract void free() throws GLException;
	
  protected abstract void swapBuffers() throws GLException;
	
  protected long dynamicLookupFunction(String glFuncName) {
    return CGL.getProcAddress(glFuncName);
  }
	
  protected void resetGLFunctionAvailability()
  {
    super.resetGLFunctionAvailability();
    if (DEBUG) {
      System.err.println("!!! Initializing OpenGL extension address table");
    }
    resetProcAddressTable(getGLProcAddressTable());
  }
	
  public GLProcAddressTable getGLProcAddressTable()
  {
    if (glProcAddressTable == null) {
      // FIXME: cache ProcAddressTables by capability bits so we can
      // share them among contexts with the same capabilities
      glProcAddressTable = new GLProcAddressTable();
    }          
    return glProcAddressTable;
  }
	
  public String getPlatformExtensionsString()
  {
    return "";
  }
	
  //----------------------------------------------------------------------
  // Internals only below this point
  //
	
  protected JAWT getJAWT()
  {
    if (jawt == null)
      {
	JAWT j = new JAWT();
	j.version(JAWTFactory.JAWT_VERSION_1_4);
	if (!JAWTFactory.JAWT_GetAWT(j))
	  {
	    throw new RuntimeException("Unable to initialize JAWT");
	  }
	jawt = j;
      }
    return jawt;
  }
}

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
  protected long nsView; // NSView
  protected long updater; // ContextUpdater
  // Table that holds the addresses of the native C-language entry points for
  // OpenGL functions.
  private GLProcAddressTable glProcAddressTable;
  
  public MacOSXGLContext(Component component,
                         GLCapabilities capabilities,
                         GLCapabilitiesChooser chooser,
                         GLContext shareWith)
  {
    super(component, capabilities, chooser, shareWith);
  }
	
  protected GL createGL()
  {
    return new MacOSXGLImpl(this);
  }
  
  protected String mapToRealGLFunctionName(String glFunctionName)
  {
    return glFunctionName;
  }
	
  protected String mapToRealGLExtensionName(String glExtensionName)
  {
    return glExtensionName;
  }
	
  protected boolean isFunctionAvailable(String glFunctionName)
  {
    return super.isFunctionAvailable(glFunctionName);
  }
  
  public boolean isExtensionAvailable(String glExtensionName) {
    if (glExtensionName.equals("GL_ARB_pbuffer") ||
        glExtensionName.equals("GL_ARB_pixel_format")) {
      return true;
    }
    return super.isExtensionAvailable(glExtensionName);
  }
  
  protected abstract boolean isOffscreen();
	
  public int getOffscreenContextBufferedImageType() {
    throw new GLException("Should not call this");
  }

  public int getOffscreenContextReadBuffer() {
    throw new GLException("Should not call this");
  }

  public int getOffscreenContextWidth() {
    throw new GLException("Should not call this");
  }
  
  public int getOffscreenContextHeight() {
    throw new GLException("Should not call this");
  }
  
  public int getOffscreenContextPixelDataType() {
    throw new GLException("Should not call this");
  }

  public boolean offscreenImageNeedsVerticalFlip() {
    throw new GLException("Should not call this");
  }

  /**
   * Creates and initializes an appropriate OpenGl nsContext. Should only be
   * called by {@link makeCurrent(Runnable)}.
   */
  protected void create() {
    MacOSXGLContext other = (MacOSXGLContext) GLContextShareSet.getShareContext(this);
    long share = 0;
    if (other != null) {
      share = other.getNSContext();
      if (share == 0) {
        throw new GLException("GLContextShareSet returned an invalid OpenGL context");
      }
    }
    nsContext = CGL.createContext(share,
                                  nsView,
                                  capabilities.getDoubleBuffered() ? 1 : 0,
                                  capabilities.getRedBits(),
                                  capabilities.getGreenBits(),
                                  capabilities.getBlueBits(),
                                  capabilities.getAlphaBits(),
                                  capabilities.getDepthBits(),
                                  capabilities.getStencilBits(),
                                  capabilities.getAccumRedBits(),
                                  capabilities.getAccumGreenBits(),
                                  capabilities.getAccumBlueBits(),
                                  capabilities.getAccumAlphaBits(),
                                  capabilities.getSampleBuffers() ? 1 : 0,
                                  capabilities.getNumSamples());
    if (nsContext == 0) {
      throw new GLException("Error creating nsContext");
    }
	//updater = CGL.updateContextRegister(nsContext, nsView); // gznote: not thread safe yet!
    GLContextShareSet.contextCreated(this);
  }    
	
  protected synchronized boolean makeCurrent(Runnable initAction) throws GLException {
      boolean created = false;
      if (nsContext == 0) {
        create();
        if (DEBUG) {
          System.err.println("!!! Created GL nsContext for " + getClass().getName());
        }
        created = true;
      }
            
      if (!CGL.makeCurrentContext(nsContext, nsView)) {
        throw new GLException("Error making nsContext current");
      }
            
      if (created) {
        resetGLFunctionAvailability();
        if (initAction != null) {
          initAction.run();
        }
      }
      return true;
  }
	
  protected synchronized void free() throws GLException {
      if (!CGL.clearCurrentContext(nsContext, nsView)) {
        throw new GLException("Error freeing OpenGL nsContext");
      }
  }
	
  protected void destroyImpl() throws GLException {
    if (nsContext != 0) {
      if (!CGL.deleteContext(nsContext, 0)) {
        throw new GLException("Unable to delete OpenGL context");
      }
      if (DEBUG) {
        System.err.println("!!! Destroyed OpenGL context " + nsContext);
      }
      nsContext = 0;
    }
  }

  public abstract void swapBuffers() throws GLException;
	
  protected long dynamicLookupFunction(String glFuncName) {
    return CGL.getProcAddress(glFuncName);
  }

  public boolean isCreated() {
    return (nsContext != 0);
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
	
  protected long getNSContext() {
    return nsContext;
  }

  protected long getNSView() {
    return nsView;
  }

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

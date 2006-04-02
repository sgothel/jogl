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

import java.nio.*;
import java.util.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public abstract class MacOSXGLContext extends GLContextImpl
{	
  protected MacOSXGLDrawable drawable;
  protected long nsContext; // NSOpenGLContext
  private CGLExt cglExt;
  // Table that holds the addresses of the native C-language entry points for
  // CGL extension functions.
  private CGLExtProcAddressTable cglExtProcAddressTable;
  
  public MacOSXGLContext(MacOSXGLDrawable drawable,
                         GLContext shareWith)
  {
    super(shareWith);
    this.drawable = drawable;
  }
	
  public Object getPlatformGLExtensions() {
    return getCGLExt();
  }

  public CGLExt getCGLExt() {
    if (cglExt == null) {
      cglExt = new CGLExtImpl(this);
    }
    return cglExt;
  }

  public GLDrawable getGLDrawable() {
    return drawable;
  }

  protected String mapToRealGLFunctionName(String glFunctionName)
  {
    return glFunctionName;
  }
	
  protected String mapToRealGLExtensionName(String glExtensionName)
  {
    return glExtensionName;
  }
	
  protected abstract boolean create();

  /**
   * Creates and initializes an appropriate OpenGl nsContext. Should only be
   * called by {@link makeCurrentImpl()}.
   */
  protected boolean create(boolean pbuffer, boolean floatingPoint) {
    MacOSXGLContext other = (MacOSXGLContext) GLContextShareSet.getShareContext(this);
    long share = 0;
    if (other != null) {
      share = other.getNSContext();
      if (share == 0) {
        throw new GLException("GLContextShareSet returned an invalid OpenGL context");
      }
    }
    int[] viewNotReady = new int[1];
    GLCapabilities capabilities = drawable.getCapabilities();
    nsContext = CGL.createContext(share,
                                  drawable.getView(),
                                  capabilities.getDoubleBuffered() ? 1 : 0,
                                  capabilities.getStereo() ? 1 : 0,
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
                                  capabilities.getNumSamples(),
                                  (pbuffer ? 1 : 0),
                                  (floatingPoint ? 1 : 0),
                                  viewNotReady, 0);
    if (nsContext == 0) {
      if (viewNotReady[0] == 1) {
        if (DEBUG) {
          System.err.println("!!! View not ready for " + getClass().getName());
        }
        // View not ready at the window system level -- this is OK
        return false;
      }
      throw new GLException("Error creating nsContext");
    }
    GLContextShareSet.contextCreated(this);
    return true;
  }    
	
  protected int makeCurrentImpl() throws GLException {
    boolean created = false;
    if (nsContext == 0) {
      if (!create()) {
        return CONTEXT_NOT_CURRENT;
      }
      if (DEBUG) {
        System.err.println("!!! Created GL nsContext for " + getClass().getName());
      }
      created = true;
    }
            
    if (!CGL.makeCurrentContext(nsContext)) {
      throw new GLException("Error making nsContext current");
    }
            
    if (created) {
      resetGLFunctionAvailability();
      return CONTEXT_CURRENT_NEW;
    }
    return CONTEXT_CURRENT;
  }
	
  protected void releaseImpl() throws GLException {
    if (!CGL.clearCurrentContext(nsContext)) {
      throw new GLException("Error freeing OpenGL nsContext");
    }
  }
	
  protected void destroyImpl() throws GLException {
    if (nsContext != 0) {
      if (!CGL.deleteContext(nsContext)) {
        throw new GLException("Unable to delete OpenGL context");
      }
      if (DEBUG) {
        System.err.println("!!! Destroyed OpenGL context " + nsContext);
      }
      nsContext = 0;
      GLContextShareSet.contextDestroyed(this);
    }
  }

  public boolean isCreated() {
    return (nsContext != 0);
  }
	
  protected void resetGLFunctionAvailability()
  {
    super.resetGLFunctionAvailability();
    if (DEBUG) {
      System.err.println("!!! Initializing CGL extension address table");
    }
    resetProcAddressTable(getCGLExtProcAddressTable());
  }
	
  public CGLExtProcAddressTable getCGLExtProcAddressTable() {
    if (cglExtProcAddressTable == null) {
      // FIXME: cache ProcAddressTables by capability bits so we can
      // share them among contexts with the same capabilities
      cglExtProcAddressTable = new CGLExtProcAddressTable();
    }          
    return cglExtProcAddressTable;
  }

  public String getPlatformExtensionsString()
  {
    return "";
  }
	
  public void setSwapInterval(int interval) {
    if (nsContext == 0) {
      throw new GLException("OpenGL context not current");
    }
    CGL.setSwapInterval(nsContext, interval);
  }

  public ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3) {
    // FIXME: apparently the Apple extension doesn't require a custom memory allocator
    throw new GLException("Not yet implemented");
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
  
  public int getOffscreenContextPixelDataType() {
    throw new GLException("Should not call this");
  }

  public int getOffscreenContextReadBuffer() {
    throw new GLException("Should not call this");
  }

  public boolean offscreenImageNeedsVerticalFlip() {
    throw new GLException("Should not call this");
  }

  public void bindPbufferToTexture() {
    throw new GLException("Should not call this");
  }
    
  public void releasePbufferFromTexture() {
    throw new GLException("Should not call this");
  }
    
  //----------------------------------------------------------------------
  // Internals only below this point
  //
	
  public long getNSContext() {
    return nsContext;
  }
}

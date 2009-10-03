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

package com.sun.opengl.impl.macosx.cgl;

import java.nio.*;
import java.util.*;
import javax.media.opengl.*;
import javax.media.nativewindow.*;
import com.sun.opengl.impl.*;
import com.sun.gluegen.runtime.ProcAddressTable;

public abstract class MacOSXCGLContext extends GLContextImpl
{	
  protected long nsContext;  // NSOpenGLContext
  protected long cglContext; // CGLContextObj
  private CGLExt cglExt;
  // Table that holds the addresses of the native C-language entry points for
  // CGL extension functions.
  private CGLExtProcAddressTable cglExtProcAddressTable;
  
  public MacOSXCGLContext(GLDrawableImpl drawable, GLDrawableImpl drawableRead,
                          GLContext shareWith) {
    super(drawable, drawableRead, shareWith);
  }

  public MacOSXCGLContext(GLDrawableImpl drawable,
                          GLContext shareWith) {
    this(drawable, null, shareWith);
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

  public final ProcAddressTable getPlatformExtProcAddressTable() {
    return getCGLExtProcAddressTable();
  }

  public final CGLExtProcAddressTable getCGLExtProcAddressTable() {
    return cglExtProcAddressTable;
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
    MacOSXCGLContext other = (MacOSXCGLContext) GLContextShareSet.getShareContext(this);
    long share = 0;
    if (other != null) {
      share = other.getNSContext();
      if (share == 0) {
        throw new GLException("GLContextShareSet returned an invalid OpenGL context");
      }
    }
    MacOSXCGLGraphicsConfiguration config = (MacOSXCGLGraphicsConfiguration) drawable.getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    GLCapabilities capabilitiesRequested = (GLCapabilities)config.getRequestedCapabilities();
    GLProfile glProfile = capabilitiesRequested.getGLProfile();
    if(glProfile.isGL3()) {
        throw new GLException("GL3 profile currently not supported on MacOSX, due to the lack of a OpenGL 3.1 implementation");
    }
    // HACK .. bring in OnScreen/PBuffer selection to the DrawableFactory !!
    GLCapabilities capabilities = (GLCapabilities) capabilitiesRequested.clone();
    capabilities.setPBuffer(pbuffer);
    capabilities.setPbufferFloatingPointBuffers(floatingPoint);

    long pixelFormat = MacOSXCGLGraphicsConfiguration.GLCapabilities2NSPixelFormat(capabilities);
    if (pixelFormat == 0) {
      throw new GLException("Unable to allocate pixel format with requested GLCapabilities");
    }
    config.setChosenPixelFormat(pixelFormat);
    try {
      int[] viewNotReady = new int[1];
      // Try to allocate a context with this
      nsContext = CGL.createContext(share,
                                    drawable.getNativeWindow().getSurfaceHandle(),
                                    pixelFormat,
                                    viewNotReady, 0);
      if (nsContext == 0) {
        if (viewNotReady[0] == 1) {
          if (DEBUG) {
            System.err.println("!!! View not ready for " + getClass().getName());
          }
          // View not ready at the window system level -- this is OK
          return false;
        }
        throw new GLException("Error creating NSOpenGLContext with requested pixel format");
      }

      if (!pbuffer && !capabilities.isBackgroundOpaque()) {
          // Set the context opacity
          CGL.setContextOpacity(nsContext, 0);
      }

      GLCapabilities caps = MacOSXCGLGraphicsConfiguration.NSPixelFormat2GLCapabilities(glProfile, pixelFormat);
      config.setChosenCapabilities(caps);
    } finally {
      CGL.deletePixelFormat(pixelFormat);
    }
    if (!CGL.makeCurrentContext(nsContext)) {
      throw new GLException("Error making nsContext current");
    }
    setGLFunctionAvailability(true);
    GLContextShareSet.contextCreated(this);
    return true;
  }    
	
  protected int makeCurrentImpl() throws GLException {
    if (0 == cglContext && drawable.getNativeWindow().getSurfaceHandle() == 0) {
        if (DEBUG) {
          System.err.println("drawable not properly initialized");
        }
        return CONTEXT_NOT_CURRENT;
    }
    boolean created = false;
    if ( 0 == cglContext && 0 == nsContext) {
      if (!create()) {
        return CONTEXT_NOT_CURRENT;
      }
      if (DEBUG) {
        System.err.println("!!! Created OpenGL context " + toHexString(nsContext) + " for " + getClass().getName());
      }
      created = true;
    }
            
    if ( 0 != cglContext ) {
        if (CGL.kCGLNoError != CGL.CGLSetCurrentContext(cglContext)) {
          throw new GLException("Error making cglContext current");
        }
    } else {
        if (!CGL.makeCurrentContext(nsContext)) {
          throw new GLException("Error making nsContext current");
        }
    }
            
    if (created) {
      setGLFunctionAvailability(false);
      return CONTEXT_CURRENT_NEW;
    }
    return CONTEXT_CURRENT;
  }
	
  protected void releaseImpl() throws GLException {
    if ( 0 != cglContext ) {
        CGL.CGLReleaseContext(cglContext);
    } else {
        if (!CGL.clearCurrentContext(nsContext)) {
          throw new GLException("Error freeing OpenGL nsContext");
        }
    }
  }
	
  protected void destroyImpl() throws GLException {
    boolean hadContext = isCreated();
    if ( 0 != cglContext ) {
      if (CGL.kCGLNoError != CGL.CGLDestroyContext(cglContext)) {
        throw new GLException("Unable to delete OpenGL cglContext");
      }
      if (DEBUG) {
        System.err.println("!!! Destroyed OpenGL cglContext " + cglContext);
      }
      cglContext = 0;
      GLContextShareSet.contextDestroyed(this);
    } else if ( 0 != nsContext ) {
      if (!CGL.deleteContext(nsContext)) {
        throw new GLException("Unable to delete OpenGL nsContext");
      }
      if (DEBUG) {
        System.err.println("!!! Destroyed OpenGL nsContext " + nsContext);
      }
      nsContext = 0;
    }
    if(hadContext) {
      GLContextShareSet.contextDestroyed(this);
    }
  }

  public boolean isCreated() {
    return 0 != cglContext || 0 != nsContext ;
  }
	
  public void copy(GLContext source, int mask) throws GLException {
    long dst = getCGLContext();
    long src = 0;
    if( 0 != dst ) {
        src = ((MacOSXCGLContext) source).getCGLContext();
        if (src == 0) {
          throw new GLException("Source OpenGL cglContext has not been created ; Destination has a cglContext.");
        }
        CGL.CGLCopyContext(src, dst, mask);
    } else {
        dst = getNSContext();
        src = ((MacOSXCGLContext) source).getNSContext();
        if (src == 0) {
          throw new GLException("Source OpenGL nsContext has not been created");
        }
        if (dst == 0) {
          throw new GLException("Destination OpenGL nsContext has not been created");
        }
        CGL.copyContext(dst, src, mask);
    }
  }

  protected void updateGLProcAddressTable() {
    if (DEBUG) {
      System.err.println("!!! Initializing CGL extension address table");
    }
    if (cglExtProcAddressTable == null) {
      // FIXME: cache ProcAddressTables by capability bits so we can
      // share them among contexts with the same capabilities
      cglExtProcAddressTable = new CGLExtProcAddressTable();
    }          
    resetProcAddressTable(getCGLExtProcAddressTable());
    super.updateGLProcAddressTable();
  }
	
  public String getPlatformExtensionsString()
  {
    return "";
  }
	
  protected void setSwapIntervalImpl(int interval) {
    if ( 0 != cglContext ) {
        int[] lval = new int[] { (int) interval } ;
        CGL.CGLSetParameter(cglContext, CGL.kCGLCPSwapInterval, lval, 0);
    } else if ( 0 != nsContext ) {
        CGL.setSwapInterval(nsContext, interval);
    } else {
      throw new GLException("OpenGL context not current");
    }
    currentSwapInterval = interval ;
  }

  public ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3) {
    // FIXME: apparently the Apple extension doesn't require a custom memory allocator
    throw new GLException("Not yet implemented");
  }

  public boolean isFunctionAvailable(String glFunctionName)
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
    
  // Support for "mode switching" as described in MacOSXCGLDrawable
  public abstract void setOpenGLMode(int mode);
  public abstract int  getOpenGLMode();

  //----------------------------------------------------------------------
  // Internals only below this point
  //
	
  public long getCGLContext() {
    return cglContext;
  }
  public long getNSContext() {
    return nsContext;
  }
}

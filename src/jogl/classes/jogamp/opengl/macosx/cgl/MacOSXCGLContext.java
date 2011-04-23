/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package jogamp.opengl.macosx.cgl;

import java.nio.*;
import java.util.*;
import javax.media.opengl.*;
import javax.media.nativewindow.*;
import jogamp.opengl.*;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;

public abstract class MacOSXCGLContext extends GLContextImpl
{    
  protected boolean isNSContext;
  private CGLExt cglExt;
  // Table that holds the addresses of the native C-language entry points for
  // CGL extension functions.
  private CGLExtProcAddressTable cglExtProcAddressTable;
  
  protected MacOSXCGLContext(GLDrawableImpl drawable,
                   GLContext shareWith) {
    super(drawable, shareWith);
  }
  
  public Object getPlatformGLExtensions() {
    return getCGLExt();
  }

  protected boolean isNSContext() { return isNSContext; }

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

  protected Map<String, String> getFunctionNameMap() { return null; }

  protected Map<String, String> getExtensionNameMap() { return null; }

  protected long createContextARBImpl(long share, boolean direct, int ctp, int major, int minor) {
      return 0; // FIXME
  }

  protected void destroyContextARBImpl(long _context) {
      // FIXME
  }

  public final boolean isGLReadDrawableAvailable() {
    return false;
  }

  /**
   * Creates and initializes an appropriate OpenGl Context (NS). Should only be
   * called by {@link makeCurrentImpl()}.
   */
  protected boolean create(boolean pbuffer, boolean floatingPoint) {
    MacOSXCGLContext other = (MacOSXCGLContext) GLContextShareSet.getShareContext(this);
    long share = 0;
    if (other != null) {
      if (!other.isNSContext()) {
        throw new GLException("GLContextShareSet is not a NS Context");
      }
      share = other.getHandle();
      if (share == 0) {
        throw new GLException("GLContextShareSet returned a NULL OpenGL context");
      }
    }
    MacOSXCGLGraphicsConfiguration config = (MacOSXCGLGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    GLCapabilitiesImmutable capabilitiesRequested = (GLCapabilitiesImmutable) config.getRequestedCapabilities();
    GLProfile glProfile = capabilitiesRequested.getGLProfile();
    if(glProfile.isGL3()) {
        throw new GLException("GL3 profile currently not supported on MacOSX, due to the lack of a OpenGL 3.1 implementation");
    }
    // HACK .. bring in OnScreen/PBuffer selection to the DrawableFactory !!
    GLCapabilities capabilities = (GLCapabilities) capabilitiesRequested.cloneMutable();
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
      contextHandle = CGL.createContext(share,
                                    drawable.getHandle(),
                                    pixelFormat,
                                    viewNotReady, 0);
      if (contextHandle == 0) {
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
          CGL.setContextOpacity(contextHandle, 0);
      }

      GLCapabilitiesImmutable caps = MacOSXCGLGraphicsConfiguration.NSPixelFormat2GLCapabilities(glProfile, pixelFormat);
      config.setChosenCapabilities(caps);
    } finally {
      CGL.deletePixelFormat(pixelFormat);
    }
    if (!CGL.makeCurrentContext(contextHandle)) {
      throw new GLException("Error making Context (NS) current");
    }
    isNSContext = true;
    setGLFunctionAvailability(true, 0, 0, CTX_PROFILE_COMPAT|CTX_OPTION_ANY);
    GLContextShareSet.contextCreated(this);
    return true;
  }

  protected void makeCurrentImpl(boolean newCreated) throws GLException {
    if ( isNSContext ) {
        if (!CGL.makeCurrentContext(contextHandle)) {
          throw new GLException("Error making Context (NS) current");
        }
    } else {
        if (CGL.kCGLNoError != CGL.CGLSetCurrentContext(contextHandle)) {
          throw new GLException("Error making Context (CGL) current");
        }
    }
  }
    
  protected void releaseImpl() throws GLException {
    if ( isNSContext ) {
        if (!CGL.clearCurrentContext(contextHandle)) {
          throw new GLException("Error freeing OpenGL Context (NS)");
        }
    } else {
        CGL.CGLReleaseContext(contextHandle);
    }
  }
    
  protected void destroyImpl() throws GLException {
    if ( !isNSContext ) {
      if (CGL.kCGLNoError != CGL.CGLDestroyContext(contextHandle)) {
        throw new GLException("Unable to delete OpenGL Context (CGL)");
      }
      if (DEBUG) {
        System.err.println("!!! Destroyed OpenGL Context (CGL) " + contextHandle);
      }
    } else {
      if (!CGL.deleteContext(contextHandle)) {
        throw new GLException("Unable to delete OpenGL Context (NS)");
      }
      if (DEBUG) {
        System.err.println("!!! Destroyed OpenGL Context (NS) " + contextHandle);
      }
    }
  }

  protected void copyImpl(GLContext source, int mask) throws GLException {
    long dst = getHandle();
    long src = source.getHandle();
    if( !isNSContext() ) {
        if ( ((MacOSXCGLContext)source).isNSContext() ) {
          throw new GLException("Source OpenGL Context is NS ; Destination Context is CGL.");
        }
        CGL.CGLCopyContext(src, dst, mask);
    } else {
        if ( !((MacOSXCGLContext)source).isNSContext() ) {
          throw new GLException("Source OpenGL Context is CGL ; Destination Context is NS.");
        }
        CGL.copyContext(dst, src, mask);
    }
  }

  protected final void updateGLXProcAddressTable() {
    AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
    String key = adevice.getUniqueID();
    if (DEBUG) {
      System.err.println(getThreadName() + ": !!! Initializing CGL extension address table: "+key);
    }
    ProcAddressTable table = null;
    synchronized(mappedContextTypeObjectLock) {
        table = mappedGLXProcAddress.get( key );
    }
    if(null != table) {
        cglExtProcAddressTable = (CGLExtProcAddressTable) table;
        if(DEBUG) {
            System.err.println(getThreadName() + ": !!! GLContext CGL ProcAddressTable reusing key("+key+") -> "+table.hashCode());
        }
    } else {
        if (cglExtProcAddressTable == null) {
          // FIXME: cache ProcAddressTables by capability bits so we can
          // share them among contexts with the same capabilities
          cglExtProcAddressTable = new CGLExtProcAddressTable(new GLProcAddressResolver());
        }
        resetProcAddressTable(getCGLExtProcAddressTable());
        synchronized(mappedContextTypeObjectLock) {
            mappedGLXProcAddress.put(key, getCGLExtProcAddressTable());
            if(DEBUG) {
                System.err.println(getThreadName() + ": !!! GLContext CGL ProcAddressTable mapping key("+key+") -> "+getCGLExtProcAddressTable().hashCode());
            }
        }
    }
  }
    
  public String getPlatformExtensionsString()
  {
    return "";
  }
    
  protected void swapBuffers() {
    DefaultGraphicsConfiguration config = (DefaultGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable)config.getChosenCapabilities();
    if(caps.isOnscreen()) {
        if(isNSContext) {
            if (!CGL.flushBuffer(contextHandle)) {
              throw new GLException("Error swapping buffers (NS)");
            }
        } else {
            if (CGL.kCGLNoError != CGL.CGLFlushDrawable(contextHandle)) {
              throw new GLException("Error swapping buffers (CGL)");
            }
        }
    }
  }

  protected void setSwapIntervalImpl(int interval) {
    if( ! isCreated() ) {
        throw new GLException("OpenGL context not created");
    } 
    if ( isNSContext ) {
        CGL.setSwapInterval(contextHandle, interval);
    } else {
        int[] lval = new int[] { (int) interval } ;
        CGL.CGLSetParameter(contextHandle, CGL.kCGLCPSwapInterval, lval, 0);
    }
    currentSwapInterval = interval ;
  }

  public ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3) {
    // FIXME: apparently the Apple extension doesn't require a custom memory allocator
    throw new GLException("Not yet implemented");
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
}

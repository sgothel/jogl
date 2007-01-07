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
    int[] iattribs = new int[128];
    int[] ivalues = new int[128];
    int idx = 0;
    if (pbuffer) {
      iattribs[idx] = CGL.NSOpenGLPFAPixelBuffer;   ivalues[idx] = 1;  idx++;
    }
    if (floatingPoint) {
      iattribs[idx] = CGL.kCGLPFAColorFloat;        ivalues[idx] = 1;  idx++;
    }
    iattribs[idx] = CGL.NSOpenGLPFADoubleBuffer;  ivalues[idx] = (capabilities.getDoubleBuffered() ? 1 : 0);  idx++;
    iattribs[idx] = CGL.NSOpenGLPFAStereo;        ivalues[idx] = (capabilities.getStereo() ? 1 : 0);          idx++;
    iattribs[idx] = CGL.NSOpenGLPFAColorSize;     ivalues[idx] = (capabilities.getRedBits() +
                                                              capabilities.getGreenBits() +
                                                              capabilities.getBlueBits());                    idx++;
    iattribs[idx] = CGL.NSOpenGLPFAAlphaSize;     ivalues[idx] = capabilities.getAlphaBits();                 idx++;
    iattribs[idx] = CGL.NSOpenGLPFADepthSize;     ivalues[idx] = capabilities.getDepthBits();                 idx++;
    iattribs[idx] = CGL.NSOpenGLPFAAccumSize;     ivalues[idx] = (capabilities.getAccumRedBits() +
                                                              capabilities.getAccumGreenBits() +
                                                              capabilities.getAccumBlueBits() +
                                                              capabilities.getAccumAlphaBits());              idx++;
    iattribs[idx] = CGL.NSOpenGLPFAStencilSize;   ivalues[idx] = capabilities.getStencilBits();               idx++;
    if (capabilities.getSampleBuffers()) {
      iattribs[idx] = CGL.NSOpenGLPFASampleBuffers; ivalues[idx] = 1;                             idx++;
      iattribs[idx] = CGL.NSOpenGLPFASamples;       ivalues[idx] = capabilities.getNumSamples();  idx++;
    }

    long pixelFormat = CGL.createPixelFormat(iattribs, 0, idx, ivalues, 0);
    if (pixelFormat == 0) {
      throw new GLException("Unable to allocate pixel format with requested GLCapabilities");
    }
    try {
      // Try to allocate a context with this
      nsContext = CGL.createContext(share,
                                    drawable.getView(),
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

      // On this platform the pixel format is associated with the
      // context and not the drawable. However it's a reasonable
      // approximation to just store the chosen pixel format up in the
      // drawable since the public API doesn't provide for a different
      // GLCapabilities per context.
      if (drawable.getChosenGLCapabilities() == null) {
        // Figure out what attributes we really got
        GLCapabilities caps = new GLCapabilities();
        CGL.queryPixelFormat(pixelFormat, iattribs, 0, idx, ivalues, 0);
        for (int i = 0; i < idx; i++) {
          int attr = iattribs[i];
          switch (attr) {
          case CGL.kCGLPFAColorFloat:
            caps.setPbufferFloatingPointBuffers(ivalues[i] != 0);
            break;

          case CGL.NSOpenGLPFADoubleBuffer:
            caps.setDoubleBuffered(ivalues[i] != 0);
            break;

          case CGL.NSOpenGLPFAStereo:
            caps.setStereo(ivalues[i] != 0);
            break;

          case CGL.NSOpenGLPFAColorSize:
            {
              int bitSize = ivalues[i];
              if (bitSize == 32)
                bitSize = 24;
              bitSize /= 3;
              caps.setRedBits(bitSize);
              caps.setGreenBits(bitSize);
              caps.setBlueBits(bitSize);
            }
            break;

          case CGL.NSOpenGLPFAAlphaSize:
            caps.setAlphaBits(ivalues[i]);
            break;

          case CGL.NSOpenGLPFADepthSize:
            caps.setDepthBits(ivalues[i]);
            break;

          case CGL.NSOpenGLPFAAccumSize:
            {
              int bitSize = ivalues[i] / 4;
              caps.setAccumRedBits(bitSize);
              caps.setAccumGreenBits(bitSize);
              caps.setAccumBlueBits(bitSize);
              caps.setAccumAlphaBits(bitSize);
            }
            break;

          case CGL.NSOpenGLPFAStencilSize:
            caps.setStencilBits(ivalues[i]);
            break;

          case CGL.NSOpenGLPFASampleBuffers:
            caps.setSampleBuffers(ivalues[i] != 0);
            break;

          case CGL.NSOpenGLPFASamples:
            caps.setNumSamples(ivalues[i]);
            break;

          default:
            break;
          }
        }

        drawable.setChosenGLCapabilities(caps);
      }
      
      
    } finally {
      CGL.deletePixelFormat(pixelFormat);
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
        System.err.println("!!! Created OpenGL context " + toHexString(nsContext) + " for " + getClass().getName());
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
	
  public void copy(GLContext source, int mask) throws GLException {
    long dst = getNSContext();
    long src = ((MacOSXGLContext) source).getNSContext();
    if (src == 0) {
      throw new GLException("Source OpenGL context has not been created");
    }
    if (dst == 0) {
      throw new GLException("Destination OpenGL context has not been created");
    }
    CGL.copyContext(dst, src, mask);
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
    
  // Support for "mode switching" as described in MacOSXGLDrawable
  public abstract void setOpenGLMode(int mode);
  public abstract int  getOpenGLMode();

  //----------------------------------------------------------------------
  // Internals only below this point
  //
	
  public long getNSContext() {
    return nsContext;
  }
}

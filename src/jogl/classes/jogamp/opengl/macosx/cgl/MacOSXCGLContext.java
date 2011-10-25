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

import java.nio.ByteBuffer;
import java.security.AccessController;
import java.util.Map;
import java.util.StringTokenizer;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.DefaultGraphicsConfiguration;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import jogamp.opengl.Debug;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLContextShareSet;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLGraphicsConfigurationUtil;
import jogamp.opengl.macosx.cgl.MacOSXCGLDrawable.GLBackendType;

import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;


public abstract class MacOSXCGLContext extends GLContextImpl
{    
  // Abstract interface for implementation of this context (either
  // NSOpenGL-based or CGL-based)
  protected interface GLBackendImpl {
        boolean isNSContext();
        boolean create(long share);
        boolean destroy();
        boolean copyImpl(long src, int mask);
        boolean makeCurrent();
        boolean release();
        boolean setSwapInterval(int interval);
        boolean swapBuffers(boolean isOnscreen);
  }
      
  private static boolean isTigerOrLater;

  static {
    String osVersion = Debug.getProperty("os.version", false, AccessController.getContext());
    StringTokenizer tok = new StringTokenizer(osVersion, ". ");
    int major = Integer.parseInt(tok.nextToken());
    int minor = Integer.parseInt(tok.nextToken());
    isTigerOrLater = ((major > 10) || (minor > 3));
  }

  private boolean haveSetOpenGLMode = false;
  private GLBackendType openGLMode = GLBackendType.NSOPENGL;
  
  // Implementation object (either NSOpenGL-based or CGL-based)
  protected GLBackendImpl impl;
      
  private CGLExt _cglExt;
  // Table that holds the addresses of the native C-language entry points for
  // CGL extension functions.
  private CGLExtProcAddressTable cglExtProcAddressTable;
  
  protected MacOSXCGLContext(GLDrawableImpl drawable,
                   GLContext shareWith) {
    super(drawable, shareWith);
    initOpenGLImpl(getOpenGLMode());
  }
  
  @Override
  protected void resetStates() {
    // no inner state _cglExt = null;
    super.resetStates();
  }

  public Object getPlatformGLExtensions() {
    return getCGLExt();
  }

  protected boolean isNSContext() { 
      return (null != impl) ? impl.isNSContext() : this.openGLMode == GLBackendType.NSOPENGL; 
  }

  public CGLExt getCGLExt() {
    if (_cglExt == null) {
      _cglExt = new CGLExtImpl(this);
    }
    return _cglExt;
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

  protected long createImplPreset() throws GLException {
    MacOSXCGLContext other = (MacOSXCGLContext) GLContextShareSet.getShareContext(this);
    long share = 0;
    if (other != null) {
      // Change our OpenGL mode to match that of any share context before we create ourselves
      setOpenGLMode(other.getOpenGLMode());
      share = other.getHandle();
      if (share == 0) {
        throw new GLException("GLContextShareSet returned a NULL OpenGL context");
      }
    }
    MacOSXCGLGraphicsConfiguration config = (MacOSXCGLGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    GLCapabilitiesImmutable capabilitiesChosen = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    if (capabilitiesChosen.getPbufferFloatingPointBuffers() &&
        !isTigerOrLater) {
       throw new GLException("Floating-point pbuffers supported only on OS X 10.4 or later");
    }
    GLProfile glProfile = capabilitiesChosen.getGLProfile();
    if(glProfile.isGL3()) {
        throw new GLException("GL3 profile currently not supported on MacOSX, due to the lack of a OpenGL 3.1 implementation");
    }
    if (DEBUG) {
      System.err.println("!!! Share context is " + toHexString(share) + " for " + this);
    }
    return share;
  }
    
  protected boolean createImpl() throws GLException {
    long share = createImplPreset();
    
    // Will throw exception upon error
    if(!impl.create(share)) {
        return false;
    }
    if (!impl.makeCurrent()) {
      throw new GLException("Error making Context (NS:"+isNSContext()+") current");
    }
    setGLFunctionAvailability(true, true, 0, 0, CTX_PROFILE_COMPAT|CTX_OPTION_ANY);
    if (DEBUG) {
      System.err.println("!!! Created " + this);
    }
    return true;
  }
  
  protected void makeCurrentImpl(boolean newCreated) throws GLException {
    if (getOpenGLMode() != ((MacOSXCGLDrawable)drawable).getOpenGLMode()) {
      setOpenGLMode(((MacOSXCGLDrawable)drawable).getOpenGLMode());
    }

    if (!impl.makeCurrent()) {
      throw new GLException("Error making Context current: "+this);
    }      
  }
    
  protected void releaseImpl() throws GLException {
    if (!impl.release()) {
      throw new GLException("Error releasing OpenGL Context: "+this);
    }
  }

  protected void destroyImpl() throws GLException {
    if(!impl.destroy()) {
        throw new GLException("Error destroying OpenGL Context: "+this);
    }
  }

  protected void copyImpl(GLContext source, int mask) throws GLException {
    if( isNSContext() != ((MacOSXCGLContext)source).isNSContext() ) {
        throw new GLException("Source/Destination OpenGL Context tyoe mismatch: source "+source+", dest: "+this);
    }
    if(!impl.copyImpl(source.getHandle(), mask)) {
        throw new GLException("Error copying OpenGL Context: source "+source+", dest: "+this);
    }
  }

  protected void swapBuffers() {
    DefaultGraphicsConfiguration config = (DefaultGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable)config.getChosenCapabilities();
    if(!impl.swapBuffers(caps.isOnscreen())) {
        throw new GLException("Error swapping buffers: "+this);        
    }
  }

  protected void setSwapIntervalImpl(int interval) {
    if( ! isCreated() ) {
        throw new GLException("OpenGL context not created");
    } 
    if(!impl.setSwapInterval(interval)) {
        throw new GLException("Error set swap-interval: "+this);        
    }
    if ( isNSContext() ) {
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

  protected final void updateGLXProcAddressTable() {
    final AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    final AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
    final String key = "MacOSX-"+adevice.getUniqueID();
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
    
  protected final StringBuffer getPlatformExtensionsStringImpl() {
    return new StringBuffer();
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
  public void setOpenGLMode(GLBackendType mode) {
      if (mode == openGLMode) {
        return;
      }
      if (haveSetOpenGLMode) {
        throw new GLException("Can't switch between using NSOpenGLPixelBuffer and CGLPBufferObj more than once");
      }
      destroyImpl();
      ((MacOSXCGLDrawable)drawable).setOpenGLMode(mode);
      if (DEBUG) {
        System.err.println("Switching context mode " + openGLMode + " -> " + mode);
      }
      initOpenGLImpl(mode);
      openGLMode = mode;
      haveSetOpenGLMode = true;      
  }
  public final GLBackendType getOpenGLMode() { return openGLMode; }
  
  protected void initOpenGLImpl(GLBackendType backend) {
    switch (backend) {
      case NSOPENGL:
        impl = new NSOpenGLImpl();
        break;
      case CGL:
        impl = new CGLImpl();
        break;
      default:
        throw new InternalError("Illegal implementation mode " + backend);
    }
  }  
  
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(getClass().getSimpleName());
    sb.append(" [");
    super.append(sb);
    sb.append(", mode ");
    sb.append(openGLMode);
    sb.append("] ");
    return sb.toString();
  }
  
  // NSOpenGLContext-based implementation
  class NSOpenGLImpl implements GLBackendImpl {
    public boolean isNSContext() { return true; }

    public boolean create(long share) {
        MacOSXCGLGraphicsConfiguration config = (MacOSXCGLGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
        GLCapabilitiesImmutable chosenCaps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
        long pixelFormat = MacOSXCGLGraphicsConfiguration.GLCapabilities2NSPixelFormat(chosenCaps);
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
                                  chosenCaps.isBackgroundOpaque(),
                                  viewNotReady, 0);
          if (0 == contextHandle) {
            if (viewNotReady[0] == 1) {
              if (DEBUG) {
                System.err.println("!!! View not ready for " + getClass().getName());
              }
              // View not ready at the window system level -- this is OK
              return false;
            }
            throw new GLException("Error creating NSOpenGLContext with requested pixel format");
          }
    
          if (!chosenCaps.isPBuffer() && !chosenCaps.isBackgroundOpaque()) {
              // Set the context opacity
              CGL.setContextOpacity(contextHandle, 0);
          }
    
          GLCapabilitiesImmutable caps = MacOSXCGLGraphicsConfiguration.NSPixelFormat2GLCapabilities(chosenCaps.getGLProfile(), pixelFormat);
          caps = GLGraphicsConfigurationUtil.fixOpaqueGLCapabilities(caps, chosenCaps.isBackgroundOpaque());
          config.setChosenCapabilities(caps);          
          if(caps.isPBuffer()) {
              // Must now associate the pbuffer with our newly-created context
              CGL.setContextPBuffer(contextHandle, drawable.getHandle());
          }      
        } finally {
          CGL.deletePixelFormat(pixelFormat);
        }
        if (!CGL.makeCurrentContext(contextHandle)) {
          throw new GLException("Error making Context (NS) current");
        }
        setGLFunctionAvailability(true, true, 0, 0, CTX_PROFILE_COMPAT|CTX_OPTION_ANY);
        GLContextShareSet.contextCreated(MacOSXCGLContext.this);
        return true;        
    }
    
    public boolean destroy() {
      return CGL.deleteContext(contextHandle, true);
    }

    public boolean copyImpl(long src, int mask) {
        CGL.copyContext(contextHandle, src, mask);
        return true;
    }
    
    public boolean makeCurrent() {
      return CGL.makeCurrentContext(contextHandle);
    }

    public boolean release() {
      return CGL.clearCurrentContext(contextHandle);
    }

    public boolean setSwapInterval(int interval) {
      CGL.setSwapInterval(contextHandle, interval);      
      return true;
    }
    public boolean swapBuffers(boolean isOnscreen) {
        if(isOnscreen) {
            return CGL.flushBuffer(contextHandle);
        }
        return true;
    }
  }

  class CGLImpl implements GLBackendImpl {
    public boolean isNSContext() { return false; }
    
    public boolean create(long share) {
      DefaultGraphicsConfiguration config = (DefaultGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
      GLCapabilitiesImmutable chosenCaps = (GLCapabilitiesImmutable)config.getChosenCapabilities();
      
      // Set up pixel format attributes
      int[] attrs = new int[256];
      int i = 0;
      if(chosenCaps.isPBuffer()) {
          attrs[i++] = CGL.kCGLPFAPBuffer;
      }
      if (chosenCaps.getPbufferFloatingPointBuffers()) {
        attrs[i++] = CGL.kCGLPFAColorFloat;
      }
      if (chosenCaps.getDoubleBuffered()) {
        attrs[i++] = CGL.kCGLPFADoubleBuffer;
      }
      if (chosenCaps.getStereo()) {
        attrs[i++] = CGL.kCGLPFAStereo;
      }
      attrs[i++] = CGL.kCGLPFAColorSize;
      attrs[i++] = (chosenCaps.getRedBits() +
                    chosenCaps.getGreenBits() +
                    chosenCaps.getBlueBits());
      attrs[i++] = CGL.kCGLPFAAlphaSize;
      attrs[i++] = chosenCaps.getAlphaBits();
      attrs[i++] = CGL.kCGLPFADepthSize;
      attrs[i++] = chosenCaps.getDepthBits();
      // FIXME: should validate stencil size as is done in MacOSXWindowSystemInterface.m
      attrs[i++] = CGL.kCGLPFAStencilSize;
      attrs[i++] = chosenCaps.getStencilBits();
      attrs[i++] = CGL.kCGLPFAAccumSize;
      attrs[i++] = (chosenCaps.getAccumRedBits() +
                    chosenCaps.getAccumGreenBits() +
                    chosenCaps.getAccumBlueBits() +
                    chosenCaps.getAccumAlphaBits());
      if (chosenCaps.getSampleBuffers()) {
        attrs[i++] = CGL.kCGLPFASampleBuffers;
        attrs[i++] = 1;
        attrs[i++] = CGL.kCGLPFASamples;
        attrs[i++] = chosenCaps.getNumSamples();
      }

      // Use attribute array to select pixel format
      PointerBuffer fmt = PointerBuffer.allocateDirect(1);
      long[] numScreens = new long[1];
      int res = CGL.CGLChoosePixelFormat(attrs, 0, fmt, numScreens, 0);
      if (res != CGL.kCGLNoError) {
        throw new GLException("Error code " + res + " while choosing pixel format");
      }
      try {      
          // Create new context
          PointerBuffer ctx = PointerBuffer.allocateDirect(1);
          if (DEBUG) {
            System.err.println("Share context for CGL-based pbuffer context is " + toHexString(share));
          }
          res = CGL.CGLCreateContext(fmt.get(0), share, ctx);
          if (res != CGL.kCGLNoError) {
            throw new GLException("Error code " + res + " while creating context");
          }
          if(chosenCaps.isPBuffer()) {
              // Attach newly-created context to the pbuffer
              res = CGL.CGLSetPBuffer(ctx.get(0), drawable.getHandle(), 0, 0, 0);
              if (res != CGL.kCGLNoError) {
                throw new GLException("Error code " + res + " while attaching context to pbuffer");
              }
          }
          contextHandle = ctx.get(0);
      } finally {
          CGL.CGLDestroyPixelFormat(fmt.get(0));          
      }
      return true;
    }
    
    public boolean destroy() {
      return CGL.CGLDestroyContext(contextHandle) == CGL.kCGLNoError;
    }

    public boolean copyImpl(long src, int mask) {
        CGL.CGLCopyContext(src, contextHandle, mask);
        return true;
    }
    
    public boolean makeCurrent() {
      return CGL.CGLSetCurrentContext(contextHandle) == CGL.kCGLNoError;
    }

    public boolean release() {
      return (CGL.CGLSetCurrentContext(0) == CGL.kCGLNoError);
    }
    
    public boolean setSwapInterval(int interval) {
        int[] lval = new int[] { interval } ;
        CGL.CGLSetParameter(contextHandle, CGL.kCGLCPSwapInterval, lval, 0);
        return true;
    }    
    public boolean swapBuffers(boolean isOnscreen) {
        if(isOnscreen) {
            return CGL.kCGLNoError == CGL.CGLFlushDrawable(contextHandle);
        }
        return true;
    }    
  }  
}

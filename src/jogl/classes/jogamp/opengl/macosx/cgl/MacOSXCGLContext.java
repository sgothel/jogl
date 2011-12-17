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
import java.util.Map;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.OffscreenLayerSurface;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLGraphicsConfigurationUtil;
import jogamp.opengl.macosx.cgl.MacOSXCGLDrawable.GLBackendType;

import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;

public abstract class MacOSXCGLContext extends GLContextImpl
{    
  // Abstract interface for implementation of this context (either
  // NSOpenGL-based or CGL-based)
  protected interface GLBackendImpl {
        boolean isNSContext();
        long create(long share, int ctp, int major, int minor);
        boolean destroy(long ctx);
        boolean copyImpl(long src, int mask);
        boolean makeCurrent(long ctx);
        boolean release(long ctx);
        boolean setSwapInterval(int interval);
        boolean swapBuffers();
  }
      
  /* package */ static final boolean isTigerOrLater;
  /* package */ static final boolean isLionOrLater;

  static {
    final VersionNumber osvn = Platform.getOSVersionNumber();
    isTigerOrLater = osvn.getMajor() > 10 || ( osvn.getMajor() == 10 && osvn.getMinor() >= 4 );
    isLionOrLater  = osvn.getMajor() > 10 || ( osvn.getMajor() == 10 && osvn.getMinor() >= 7 );
  }

  static boolean isGLProfileSupported(int ctp, int major, int minor) {
    boolean ctBwdCompat = 0 != ( CTX_PROFILE_COMPAT & ctp ) ;
    boolean ctCore      = 0 != ( CTX_PROFILE_CORE & ctp ) ;
    
    // We exclude 3.0, since we would map it's core to GL2. Hence we force mapping 2.1 to GL2
    if(3==major && 1<=minor && minor<=2) {
        // [3.1..3.2] -> GL3*
        if(!isLionOrLater) {
            // no GL3* on pre lion
            return false;
        }
        if(ctBwdCompat) {
            // no compatibility profile on OS X
            return false;
        }
        return ctCore;
    } else if(major<3) {
        // < 3.0 -> GL2
        return true;
    }
    return false; // 3.0 && > 3.2
  }
  static int GLProfile2CGLOGLProfileValue(int ctp, int major, int minor) {
    if(!MacOSXCGLContext.isGLProfileSupported(ctp, major, minor)) {
        throw new GLException("OpenGL profile not supported: "+getGLVersion(major, minor, ctp, "@GLProfile2CGLOGLProfileVersion"));
    }
    boolean ctCore      = 0 != ( CTX_PROFILE_CORE & ctp ) ;
    if( major == 3 && minor >= 1 && ctCore ) {
        return CGL.kCGLOGLPVersion_3_2_Core;
    } else {
        return CGL.kCGLOGLPVersion_Legacy;
    }        
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
    if(!isGLProfileSupported(ctp, major, minor)) {
        if(DEBUG) {
            System.err.println(getThreadName() + ": createContextARBImpl: Not supported "+getGLVersion(major, minor, ctp, "@creation on OSX "+Platform.getOSVersionNumber()));
        }
        return 0;
    }

    // Will throw exception upon error
    long ctx = impl.create(share, ctp, major, minor);
    if(0 != ctx) {
        if (!impl.makeCurrent(ctx)) {
          if(DEBUG) {
              System.err.println(getThreadName() + ": createContextARB couldn't make current "+getGLVersion(major, minor, ctp, "@creation"));
          }
          impl.release(ctx);
          impl.destroy(ctx);
          ctx = 0;
        } else if(DEBUG) {
            System.err.println(getThreadName() + ": createContextARBImpl: OK "+getGLVersion(major, minor, ctp, "@creation")+", share "+share+", direct "+direct+" on OSX "+Platform.getOSVersionNumber());
        }
    } else if(DEBUG) {
        System.err.println(getThreadName() + ": createContextARBImpl: NO "+getGLVersion(major, minor, ctp, "@creation on OSX "+Platform.getOSVersionNumber()));
    }
    return ctx;
  }

  protected void destroyContextARBImpl(long _context) {
      impl.release(_context);
      impl.destroy(_context);
  }

  public final boolean isGLReadDrawableAvailable() {
    return false;
  }

  protected long createImplPreset(GLContextImpl shareWith) throws GLException {
    long share = 0;
    if (shareWith != null) {
      // Change our OpenGL mode to match that of any share context before we create ourselves
      setOpenGLMode(((MacOSXCGLContext)shareWith).getOpenGLMode());
      share = shareWith.getHandle();
      if (share == 0) {
        throw new GLException("GLContextShareSet returned a NULL OpenGL context");
      }
    }
    
    MacOSXCGLGraphicsConfiguration config = (MacOSXCGLGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration();
    GLCapabilitiesImmutable capabilitiesChosen = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    if (capabilitiesChosen.getPbufferFloatingPointBuffers() && !isTigerOrLater) {
       throw new GLException("Floating-point pbuffers supported only on OS X 10.4 or later");
    }
    GLProfile glp = capabilitiesChosen.getGLProfile();
    if(glp.isGLES1() || glp.isGLES2() || glp.isGL4() || glp.isGL3() && !isLionOrLater) {
        throw new GLException("OpenGL profile not supported on MacOSX "+Platform.getOSVersionNumber()+": "+glp);
    }
    
    if (DEBUG) {
      System.err.println("!!! Share context is " + toHexString(share) + " for " + this);
    }
    return share;      
  }
  
  protected boolean createImpl(GLContextImpl shareWith) throws GLException {
    long share = createImplPreset(shareWith);
    contextHandle = createContextARB(share, true);
    return 0 != contextHandle;
  }
  
  protected void makeCurrentImpl() throws GLException {
    if (getOpenGLMode() != ((MacOSXCGLDrawable)drawable).getOpenGLMode()) {
      setOpenGLMode(((MacOSXCGLDrawable)drawable).getOpenGLMode());
    }
    if (!impl.makeCurrent(contextHandle)) {
      throw new GLException("Error making Context current: "+this);
    }      
  }
    
  protected void releaseImpl() throws GLException {
    if (!impl.release(contextHandle)) {
      throw new GLException("Error releasing OpenGL Context: "+this);
    }
  }

  protected void destroyImpl() throws GLException {
    if(!impl.destroy(contextHandle)) {
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
    // single-buffer is already filtered out @ GLDrawableImpl#swapBuffers()
    if(!impl.swapBuffers()) {
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
    currentSwapInterval = interval ;
  }

  public ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3) {
    // FIXME: apparently the Apple extension doesn't require a custom memory allocator
    throw new GLException("Not yet implemented");
  }

  protected final void updateGLXProcAddressTable() {
    final AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration();
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
    long nsOpenGLLayer = 0;
    long nsOpenGLLayerPFmt = 0;
    
    public boolean isNSContext() { return true; }

    public long create(long share, int ctp, int major, int minor) {
        long ctx = 0;
        final MacOSXCGLDrawable drawable = (MacOSXCGLDrawable) MacOSXCGLContext.this.drawable;
        final NativeSurface surface = drawable.getNativeSurface();
        final MacOSXCGLGraphicsConfiguration config = (MacOSXCGLGraphicsConfiguration) surface.getGraphicsConfiguration();
        final OffscreenLayerSurface backingLayerHost = NativeWindowFactory.getOffscreenLayerSurface(surface, true);        
        final GLCapabilitiesImmutable chosenCaps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
        long pixelFormat = MacOSXCGLGraphicsConfiguration.GLCapabilities2NSPixelFormat(chosenCaps, ctp, major, minor);
        if (pixelFormat == 0) {
          throw new GLException("Unable to allocate pixel format with requested GLCapabilities");
        }
        config.setChosenPixelFormat(pixelFormat);
        if(DEBUG) {
            System.err.println("NS create OSX>=lion "+isLionOrLater);
            System.err.println("NS create backingLayerHost: "+backingLayerHost);
            System.err.println("NS create share: "+share);
            System.err.println("NS create chosenCaps: "+chosenCaps);
            System.err.println("NS create pixelFormat: "+toHexString(pixelFormat));
            System.err.println("NS create drawable native-handle: "+toHexString(drawable.getHandle()));
            System.err.println("NS create drawable NSView-handle: "+toHexString(drawable.getNSViewHandle()));
            // Thread.dumpStack();
        }
        try {
          int[] viewNotReady = new int[1];
          // Try to allocate a context with this
          ctx = CGL.createContext(share,
                                  drawable.getNSViewHandle(), null!=backingLayerHost,
                                  pixelFormat,
                                  chosenCaps.isBackgroundOpaque(),
                                  viewNotReady, 0);
          if (0 == ctx) {
            if(DEBUG) {
                System.err.println("NS create failed: viewNotReady: "+ (1 == viewNotReady[0]));
            }
            return 0;
          }
    
          if (!chosenCaps.isPBuffer() && !chosenCaps.isBackgroundOpaque()) {
              // Set the context opacity
              CGL.setContextOpacity(ctx, 0);
          }

          if(DEBUG) {
              GLCapabilitiesImmutable caps0 = MacOSXCGLGraphicsConfiguration.NSPixelFormat2GLCapabilities(null, pixelFormat);
              System.err.println("NS create pixelformat2GLCaps: "+caps0);
          }
          GLCapabilitiesImmutable fixedCaps = MacOSXCGLGraphicsConfiguration.NSPixelFormat2GLCapabilities(chosenCaps.getGLProfile(), pixelFormat);
          fixedCaps = GLGraphicsConfigurationUtil.fixOpaqueGLCapabilities(fixedCaps, chosenCaps.isBackgroundOpaque());
          config.setChosenCapabilities(fixedCaps);          
          if(DEBUG) {
              System.err.println("NS create fixedCaps: "+fixedCaps);
          }
          if(fixedCaps.isPBuffer()) {
              // Must now associate the pbuffer with our newly-created context
              CGL.setContextPBuffer(ctx, drawable.getHandle());
          }
          //
          // handled layered surface
          // 
          if(null != backingLayerHost) {
              nsOpenGLLayerPFmt = pixelFormat;
              pixelFormat = 0;
              final int texWidth, texHeight;
              if(drawable instanceof MacOSXPbufferCGLDrawable) {
                  final MacOSXPbufferCGLDrawable osxPDrawable = (MacOSXPbufferCGLDrawable)drawable;
                  texWidth = osxPDrawable.getTextureWidth();
                  texHeight = osxPDrawable.getTextureHeight();
              } else {
                  texWidth = drawable.getWidth();
                  texHeight = drawable.getHeight();                  
              }              
              nsOpenGLLayer = CGL.createNSOpenGLLayer(ctx, nsOpenGLLayerPFmt, drawable.getHandle(), fixedCaps.isBackgroundOpaque(), texWidth, texHeight);
              if(0>=texWidth || 0>=texHeight || !drawable.isRealized()) {
                  throw new GLException("Drawable not realized yet or invalid texture size, texSize "+texWidth+"x"+texHeight+", "+drawable);
              }
              if (DEBUG) {
                  System.err.println("NS create nsOpenGLLayer "+toHexString(nsOpenGLLayer)+", texSize "+texWidth+"x"+texHeight+", "+drawable);
              }
              backingLayerHost.attachSurfaceLayer(nsOpenGLLayer);
          }
        } finally {
          if(0!=pixelFormat) {
              CGL.deletePixelFormat(pixelFormat);
          }
        }
        return ctx;        
    }
    
    public boolean destroy(long ctx) {
      if(0 != nsOpenGLLayer) {
          final NativeSurface surface = drawable.getNativeSurface();
          if (DEBUG) {
              System.err.println("NS destroy nsOpenGLLayer "+toHexString(nsOpenGLLayer));
          }
          final OffscreenLayerSurface ols = NativeWindowFactory.getOffscreenLayerSurface(surface, true);
          if(null == ols) {
              throw new InternalError("XXX: "+ols);
          }
          CGL.releaseNSOpenGLLayer(nsOpenGLLayer);
          ols.detachSurfaceLayer(nsOpenGLLayer);
          CGL.deletePixelFormat(nsOpenGLLayerPFmt);
          nsOpenGLLayerPFmt = 0;
          nsOpenGLLayer = 0;
      }
      return CGL.deleteContext(ctx, true);
    }

    public boolean copyImpl(long src, int mask) {
        CGL.copyContext(contextHandle, src, mask);
        return true;
    }
    
    public boolean makeCurrent(long ctx) {
      final long cglCtx = CGL.getCGLContext(ctx);
      if(0 == cglCtx) {
          throw new InternalError("Null CGLContext for: "+this);
      }
      int err = CGL.CGLLockContext(cglCtx);
      if(CGL.kCGLNoError == err) {
          return CGL.makeCurrentContext(ctx);
      } else if(DEBUG) {
          System.err.println("NSGL: Could not lock context: err 0x"+Integer.toHexString(err)+": "+this);
      }
      return false;
    }

    public boolean release(long ctx) {
      final boolean res = CGL.clearCurrentContext(ctx);
      final long cglCtx = CGL.getCGLContext(ctx);
      if(0 == cglCtx) {
          throw new InternalError("Null CGLContext for: "+this);
      }
      final int err = CGL.CGLUnlockContext(cglCtx);
      if(DEBUG && CGL.kCGLNoError != err) {
          System.err.println("CGL: Could not unlock context: err 0x"+Integer.toHexString(err)+": "+this);
      }      
      return res && CGL.kCGLNoError == err;
    }

    public boolean setSwapInterval(int interval) {
      if(0 != nsOpenGLLayer) {
        CGL.setNSOpenGLLayerSwapInterval(nsOpenGLLayer, interval);
      }
      CGL.setSwapInterval(contextHandle, interval);
      return true;
    }
    
    public boolean swapBuffers() {
      if(0 != nsOpenGLLayer) {
        // sync w/ CALayer renderer - wait until next frame is required (v-sync)
        CGL.waitUntilNSOpenGLLayerIsReady(nsOpenGLLayer, 16); // timeout 16ms -> 60Hz
      }
      if(CGL.flushBuffer(contextHandle)) {
          if(0 != nsOpenGLLayer) {
              // trigger CALayer to update
              CGL.setNSOpenGLLayerNeedsDisplay(nsOpenGLLayer);
          }
          return true;
      }
      return false;
    }
  }

  class CGLImpl implements GLBackendImpl {
    public boolean isNSContext() { return false; }
    
    public long create(long share, int ctp, int major, int minor) {
      long ctx = 0;
      MacOSXCGLGraphicsConfiguration config = (MacOSXCGLGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration();
      GLCapabilitiesImmutable chosenCaps = (GLCapabilitiesImmutable)config.getChosenCapabilities();
      long pixelFormat = MacOSXCGLGraphicsConfiguration.GLCapabilities2CGLPixelFormat(chosenCaps, ctp, major, minor);
      if (pixelFormat == 0) {
        throw new GLException("Unable to allocate pixel format with requested GLCapabilities");
      }
      config.setChosenPixelFormat(pixelFormat);
      try {      
          // Create new context
          PointerBuffer ctxPB = PointerBuffer.allocateDirect(1);
          if (DEBUG) {
            System.err.println("Share context for CGL-based pbuffer context is " + toHexString(share));
          }
          int res = CGL.CGLCreateContext(pixelFormat, share, ctxPB);
          if (res != CGL.kCGLNoError) {
            throw new GLException("Error code " + res + " while creating context");
          }
          if(chosenCaps.isPBuffer()) {
              // Attach newly-created context to the pbuffer
              res = CGL.CGLSetPBuffer(ctxPB.get(0), drawable.getHandle(), 0, 0, 0);
              if (res != CGL.kCGLNoError) {
                throw new GLException("Error code " + res + " while attaching context to pbuffer");
              }
          }
          ctx = ctxPB.get(0);
          if(0!=ctx) {
              if(DEBUG) {
                  GLCapabilitiesImmutable caps0 = MacOSXCGLGraphicsConfiguration.CGLPixelFormat2GLCapabilities(pixelFormat);
                  System.err.println("NS created: "+caps0);
              }              
          }
      } finally {
          CGL.CGLDestroyPixelFormat(pixelFormat);          
      }
      return ctx;
    }
    
    public boolean destroy(long ctx) {
      return CGL.CGLDestroyContext(ctx) == CGL.kCGLNoError;
    }

    public boolean copyImpl(long src, int mask) {
        CGL.CGLCopyContext(src, contextHandle, mask);
        return true;
    }
    
    public boolean makeCurrent(long ctx) {
      int err = CGL.CGLLockContext(ctx);
      if(CGL.kCGLNoError == err) {
          err = CGL.CGLSetCurrentContext(ctx);
          if(CGL.kCGLNoError == err) {
              return true;
          } else if(DEBUG) {
              System.err.println("CGL: Could not make context current: err 0x"+Integer.toHexString(err)+": "+this);
          }
      } else if(DEBUG) {
          System.err.println("CGL: Could not lock context: err 0x"+Integer.toHexString(err)+": "+this);
      }
      return false;
    }

    public boolean release(long ctx) {
      int err = CGL.CGLSetCurrentContext(0);
      if(DEBUG && CGL.kCGLNoError != err) {
          System.err.println("CGL: Could not release current context: err 0x"+Integer.toHexString(err)+": "+this);
      }
      int err2 = CGL.CGLUnlockContext(ctx);
      if(DEBUG && CGL.kCGLNoError != err2) {
          System.err.println("CGL: Could not unlock context: err 0x"+Integer.toHexString(err2)+": "+this);
      }
      return CGL.kCGLNoError == err && CGL.kCGLNoError == err2;
    }
    
    public boolean setSwapInterval(int interval) {
        int[] lval = new int[] { interval } ;
        CGL.CGLSetParameter(contextHandle, CGL.kCGLCPSwapInterval, lval, 0);
        return true;
    }    
    public boolean swapBuffers() {
        return CGL.kCGLNoError == CGL.CGLFlushDrawable(contextHandle);
    }    
  }  
}

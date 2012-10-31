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
import java.nio.IntBuffer;
import java.util.Map;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.OffscreenLayerSurface;
import javax.media.nativewindow.ProxySurface;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLUniformData;

import jogamp.nativewindow.macosx.OSXUtil;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLFBODrawableImpl;
import jogamp.opengl.GLGraphicsConfigurationUtil;
import jogamp.opengl.macosx.cgl.MacOSXCGLDrawable.GLBackendType;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

public abstract class MacOSXCGLContext extends GLContextImpl
{
  // Abstract interface for implementation of this context (either
  // NSOpenGL-based or CGL-based)
  protected interface GLBackendImpl {
        boolean isNSContext();
        long create(long share, int ctp, int major, int minor);
        boolean destroy(long ctx);
        boolean contextRealized(boolean realized);
        boolean copyImpl(long src, int mask);
        boolean makeCurrent(long ctx);
        boolean release(long ctx);
        boolean detachPBuffer();
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

  private static final String shaderBasename = "texture01_xxx";
  
  private static ShaderProgram createCALayerShader(GL3 gl) {
      // Create & Link the shader program
      final ShaderProgram sp = new ShaderProgram();
      final ShaderCode vp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, MacOSXCGLContext.class, 
                                              "../../shader", "../../shader/bin", shaderBasename, true);
      final ShaderCode fp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, MacOSXCGLContext.class, 
                                              "../../shader", "../../shader/bin", shaderBasename, true);
      vp.defaultShaderCustomization(gl, true, ShaderCode.es2_default_precision_vp);
      fp.defaultShaderCustomization(gl, true, ShaderCode.es2_default_precision_fp);
      sp.add(vp);
      sp.add(fp);
      if(!sp.link(gl, System.err)) {
          throw new GLException("Couldn't link program: "+sp);
      }
      sp.useProgram(gl, true);

      // setup mgl_PMVMatrix
      final PMVMatrix pmvMatrix = new PMVMatrix();
      pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
      pmvMatrix.glLoadIdentity();
      pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
      pmvMatrix.glLoadIdentity();       
      final GLUniformData pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf()); // P, Mv
      pmvMatrixUniform.setLocation( gl.glGetUniformLocation( sp.program(), pmvMatrixUniform.getName() ) );
      gl.glUniform(pmvMatrixUniform);

      sp.useProgram(gl, false);
      return sp;
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
    cglExtProcAddressTable = null;
    super.resetStates();
  }

  @Override
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

  @Override
  public final ProcAddressTable getPlatformExtProcAddressTable() {
    return getCGLExtProcAddressTable();
  }

  public final CGLExtProcAddressTable getCGLExtProcAddressTable() {
    return cglExtProcAddressTable;
  }

  @Override
  protected Map<String, String> getFunctionNameMap() { return null; }

  @Override
  protected Map<String, String> getExtensionNameMap() { return null; }

  @Override
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

  @Override
  protected void destroyContextARBImpl(long _context) {
      impl.release(_context);
      impl.destroy(_context);
  }

  @Override
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
      System.err.println("Share context is " + toHexString(share) + " for " + this);
    }
    return share;
  }

  @Override
  protected boolean createImpl(GLContextImpl shareWith) throws GLException {
    long share = createImplPreset(shareWith);
    contextHandle = createContextARB(share, true);
    return 0 != contextHandle;
  }

  @Override
  protected void makeCurrentImpl() throws GLException {
    /** FIXME: won't work w/ special drawables (like FBO) - check for CGL mode regressions!
     *  
    if (getOpenGLMode() != ((MacOSXCGLDrawable)drawable).getOpenGLMode()) {
      setOpenGLMode(((MacOSXCGLDrawable)drawable).getOpenGLMode());
    } */
    if (!impl.makeCurrent(contextHandle)) {
      throw new GLException("Error making Context current: "+this);
    }
  }

  @Override
  protected void releaseImpl() throws GLException {
    if (!impl.release(contextHandle)) {
      throw new GLException("Error releasing OpenGL Context: "+this);
    }
  }

  @Override
  protected void destroyImpl() throws GLException {
    if(!impl.destroy(contextHandle)) {
        throw new GLException("Error destroying OpenGL Context: "+this);
    }
  }
  
  @Override
  protected void contextRealized(boolean realized) {
      // context stuff depends on drawable stuff
      if(realized) {
          super.contextRealized(true);   // 1) init drawable stuff
          impl.contextRealized(true);    // 2) init context stuff
      } else {
          impl.contextRealized(false);   // 1) free context stuff
          super.contextRealized(false);  // 2) free drawable stuff
      }
  }

  /* pp */ void detachPBuffer() {
      impl.detachPBuffer();
  }

  
  @Override
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

  @Override
  protected boolean setSwapIntervalImpl(int interval) {
    return impl.setSwapInterval(interval);
  }

  @Override
  public ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3) {
    // FIXME: apparently the Apple extension doesn't require a custom memory allocator
    throw new GLException("Not yet implemented");
  }

  @Override
  protected final void updateGLXProcAddressTable() {
    final AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration();
    final AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
    final String key = "MacOSX-"+adevice.getUniqueID();
    if (DEBUG) {
      System.err.println(getThreadName() + ": Initializing CGL extension address table: "+key);
    }
    ProcAddressTable table = null;
    synchronized(mappedContextTypeObjectLock) {
        table = mappedGLXProcAddress.get( key );
    }
    if(null != table) {
        cglExtProcAddressTable = (CGLExtProcAddressTable) table;
        if(DEBUG) {
            System.err.println(getThreadName() + ": GLContext CGL ProcAddressTable reusing key("+key+") -> "+toHexString(table.hashCode()));
        }
    } else {
        cglExtProcAddressTable = new CGLExtProcAddressTable(new GLProcAddressResolver());
        resetProcAddressTable(getCGLExtProcAddressTable());
        synchronized(mappedContextTypeObjectLock) {
            mappedGLXProcAddress.put(key, getCGLExtProcAddressTable());
            if(DEBUG) {
                System.err.println(getThreadName() + ": GLContext CGL ProcAddressTable mapping key("+key+") -> "+toHexString(getCGLExtProcAddressTable().hashCode()));
            }
        }
    }
  }

  @Override
  protected final StringBuilder getPlatformExtensionsStringImpl() {
    return new StringBuilder();
  }

  @Override
  public boolean isExtensionAvailable(String glExtensionName) {
    if (glExtensionName.equals(GLExtensions.ARB_pbuffer) ||
        glExtensionName.equals(GLExtensions.ARB_pixel_format)) {
      return true;
    }
    return super.isExtensionAvailable(glExtensionName);
  }

  @Override
  public int getOffscreenContextPixelDataType() {
    throw new GLException("Should not call this");
  }

  public int getOffscreenContextReadBuffer() {
    throw new GLException("Should not call this");
  }

  @Override
  public boolean offscreenImageNeedsVerticalFlip() {
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
        System.err.println("MacOSXCGLContext: Switching context mode " + openGLMode + " -> " + mode);
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

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
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
      private OffscreenLayerSurface backingLayerHost = null;
      private long nsOpenGLLayer = 0;
      private long nsOpenGLLayerPFmt = 0;
      private float screenVSyncTimeout; // microSec
      private int vsyncTimeout;    // microSec - for nsOpenGLLayer mode
      private int lastWidth=0, lastHeight=0; // allowing to detect size change
      private boolean needsSetContextPBuffer = false;
      private ShaderProgram gl3ShaderProgram = null;
      
      @Override
      public boolean isNSContext() { return true; }

      @Override
      public long create(long share, int ctp, int major, int minor) {
          long ctx = 0;
          final NativeSurface surface = drawable.getNativeSurface();        
          final MacOSXCGLGraphicsConfiguration config = (MacOSXCGLGraphicsConfiguration) surface.getGraphicsConfiguration();
          final GLCapabilitiesImmutable chosenCaps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
          final long nsViewHandle;
          final boolean isPBuffer;
          final boolean isFBO;
          if(drawable instanceof GLFBODrawableImpl) {
              nsViewHandle = 0;
              isPBuffer = false;
              isFBO = true;
              if(DEBUG) {
                  System.err.println("NS create GLFBODrawableImpl drawable: isFBO "+isFBO+", isPBuffer "+isPBuffer+", "+drawable.getClass().getName()+",\n\t"+drawable);
              }
          } else if(drawable instanceof MacOSXCGLDrawable) {
              // we allow null here! (special pbuffer case)
              nsViewHandle = ((MacOSXCGLDrawable)MacOSXCGLContext.this.drawable).getNSViewHandle();
              isPBuffer = CGL.isNSOpenGLPixelBuffer(drawable.getHandle());
              isFBO = false;
              if(DEBUG) {
                  System.err.println("NS create MacOSXCGLDrawable drawable handle isFBO "+isFBO+", isPBuffer "+isPBuffer+", "+drawable.getClass().getName()+",\n\t"+drawable);
              }
          } else {
              // we only allow a valid NSView here
              final long drawableHandle = drawable.getHandle();
              final boolean isNSView = OSXUtil.isNSView(drawableHandle);
              final boolean isNSWindow = OSXUtil.isNSWindow(drawableHandle);
              isPBuffer = CGL.isNSOpenGLPixelBuffer(drawableHandle);
              isFBO = false;

              if(DEBUG) {
                  System.err.println("NS create Anonymous drawable handle "+toHexString(drawableHandle)+": isNSView "+isNSView+", isNSWindow "+isNSWindow+", isFBO "+isFBO+", isPBuffer "+isPBuffer+", "+drawable.getClass().getName()+",\n\t"+drawable);
              }
              if( isNSView ) {
                  nsViewHandle = drawableHandle;
              } else if( isNSWindow ) {
                  nsViewHandle = OSXUtil.GetNSView(drawableHandle);
              } else if( isPBuffer ) {
                  nsViewHandle = 0;
              } else {
                  throw new RuntimeException("Anonymous drawable instance's handle neither NSView, NSWindow nor PBuffer: "+toHexString(drawableHandle)+", "+drawable.getClass().getName()+",\n\t"+drawable);
              }
          }
          needsSetContextPBuffer = isPBuffer;
          backingLayerHost = NativeWindowFactory.getOffscreenLayerSurface(surface, true);

          boolean incompleteView = null != backingLayerHost;
          if( !incompleteView && surface instanceof ProxySurface ) {
              incompleteView = ((ProxySurface)surface).containsUpstreamOptionBits( ProxySurface.OPT_UPSTREAM_WINDOW_INVISIBLE );
          }
          long pixelFormat = MacOSXCGLGraphicsConfiguration.GLCapabilities2NSPixelFormat(chosenCaps, ctp, major, minor);
          if (pixelFormat == 0) {
              if(DEBUG) {
                  System.err.println("Unable to allocate pixel format with requested GLCapabilities: "+chosenCaps);
              }
              return 0;
          }
          GLCapabilities fixedCaps = MacOSXCGLGraphicsConfiguration.NSPixelFormat2GLCapabilities(chosenCaps.getGLProfile(), pixelFormat);
          if( !fixedCaps.isPBuffer() && isPBuffer ) {
              throw new InternalError("handle is PBuffer, fixedCaps not: "+drawable);
          }
          { // determine on-/offscreen caps, since pformat is ambiguous 
              fixedCaps.setFBO( isFBO );         // exclusive 
              fixedCaps.setPBuffer( isPBuffer ); // exclusive
              fixedCaps.setBitmap( false );      // n/a in our OSX impl.
              fixedCaps.setOnscreen( !isFBO && !isPBuffer );
          }
          fixedCaps = GLGraphicsConfigurationUtil.fixOpaqueGLCapabilities(fixedCaps, chosenCaps.isBackgroundOpaque());
          int sRefreshRate = OSXUtil.GetScreenRefreshRate(drawable.getNativeSurface().getGraphicsConfiguration().getScreen().getIndex());
          screenVSyncTimeout = 1000000f / sRefreshRate;
          if(DEBUG) {
              System.err.println("NS create OSX>=lion "+isLionOrLater);
              System.err.println("NS create incompleteView: "+incompleteView);
              System.err.println("NS create backingLayerHost: "+backingLayerHost);
              System.err.println("NS create share: "+share);
              System.err.println("NS create drawable type: "+drawable.getClass().getName());
              System.err.println("NS create drawable handle: isPBuffer "+isPBuffer+", isFBO "+isFBO);
              System.err.println("NS create pixelFormat: "+toHexString(pixelFormat));
              System.err.println("NS create chosenCaps: "+chosenCaps);
              System.err.println("NS create fixedCaps: "+fixedCaps);
              System.err.println("NS create drawable native-handle: "+toHexString(drawable.getHandle()));
              System.err.println("NS create drawable NSView-handle: "+toHexString(nsViewHandle));
              System.err.println("NS create screen refresh-rate: "+sRefreshRate+" hz, "+screenVSyncTimeout+" micros");
              // Thread.dumpStack();
          }
          config.setChosenCapabilities(fixedCaps);
          /**
          if(null != backingLayerHost) {
              backingLayerHost.setChosenCapabilities(fixedCaps);
          }  */                  
          
          try {
              final IntBuffer viewNotReady = Buffers.newDirectIntBuffer(1);
              // Try to allocate a context with this
              ctx = CGL.createContext(share,
                      nsViewHandle, incompleteView,
                      pixelFormat,
                      chosenCaps.isBackgroundOpaque(),
                      viewNotReady);
              if (0 == ctx) {
                  if(DEBUG) {
                      System.err.println("NS create failed: viewNotReady: "+ (1 == viewNotReady.get(0)));
                  }
                  return 0;
              }

              if(null != backingLayerHost) {
                  nsOpenGLLayerPFmt = pixelFormat;
                  pixelFormat = 0;
              }
              
              if (chosenCaps.isOnscreen() && !chosenCaps.isBackgroundOpaque()) {
                  // Set the context opacity
                  CGL.setContextOpacity(ctx, 0);
              }
          } finally {
              if(0!=pixelFormat) {
                  CGL.deletePixelFormat(pixelFormat);
                  pixelFormat = 0;
              }
          }
          return ctx;
      }

      @Override
      public boolean destroy(long ctx) {
          return CGL.deleteContext(ctx, true);
      }

      @Override
      public boolean contextRealized(boolean realized) {
          if( realized ) {
              if( null != backingLayerHost ) {
                  //
                  // handled layered surface
                  //
                  final GLCapabilitiesImmutable chosenCaps = drawable.getChosenGLCapabilities();
                  final long ctx = MacOSXCGLContext.this.getHandle();
                  final int texID;
                  final long drawableHandle = drawable.getHandle();
                  final long pbufferHandle;
                  if(drawable instanceof GLFBODrawableImpl) {
                      final GLFBODrawableImpl fbod = (GLFBODrawableImpl)drawable;
                      texID = fbod.getTextureBuffer(GL.GL_FRONT).getName();
                      pbufferHandle = 0;
                      fbod.setSwapBufferContext(new GLFBODrawableImpl.SwapBufferContext() {
                          public void swapBuffers(boolean doubleBuffered) {
                              MacOSXCGLContext.NSOpenGLImpl.this.swapBuffers();                            
                          } } ) ;                    
                  } else if( CGL.isNSOpenGLPixelBuffer(drawableHandle) ) {
                      texID = 0;
                      pbufferHandle = drawableHandle;
                      if(0 != drawableHandle) { // complete 'validatePBufferConfig(..)' procedure
                          CGL.setContextPBuffer(ctx, pbufferHandle);
                          needsSetContextPBuffer = false;
                      }
                  } else {
                      throw new GLException("BackingLayerHost w/ unknown handle (!FBO, !PBuffer): "+drawable);
                  }
                  lastWidth = drawable.getWidth();
                  lastHeight = drawable.getHeight();
                  if(0>=lastWidth || 0>=lastHeight || !drawable.isRealized()) {
                      throw new GLException("Drawable not realized yet or invalid texture size, texSize "+lastWidth+"x"+lastHeight+", "+drawable);
                  }
                  final int gl3ShaderProgramName;
                  if( MacOSXCGLContext.this.isGL3core() ) {
                      if( null == gl3ShaderProgram) {
                          gl3ShaderProgram = createCALayerShader(MacOSXCGLContext.this.gl.getGL3());
                      }
                      gl3ShaderProgramName = gl3ShaderProgram.program();
                  } else {
                      gl3ShaderProgramName = 0;
                  }
                  nsOpenGLLayer = CGL.createNSOpenGLLayer(ctx, gl3ShaderProgramName, nsOpenGLLayerPFmt, pbufferHandle, texID, chosenCaps.isBackgroundOpaque(), lastWidth, lastHeight);
                  if (DEBUG) {
                      System.err.println("NS create nsOpenGLLayer "+toHexString(nsOpenGLLayer)+" w/ pbuffer "+toHexString(pbufferHandle)+", texID "+texID+", texSize "+lastWidth+"x"+lastHeight+", "+drawable);
                  }
                  backingLayerHost.attachSurfaceLayer(nsOpenGLLayer);
                  setSwapInterval(1); // enabled per default in layered surface                
              } else {
                  lastWidth = drawable.getWidth();
                  lastHeight = drawable.getHeight();                  
              }
          } else {
              if( 0 != nsOpenGLLayer ) {
                  final NativeSurface surface = drawable.getNativeSurface();
                  if (DEBUG) {
                      System.err.println("NS destroy nsOpenGLLayer "+toHexString(nsOpenGLLayer)+", "+drawable);
                  }
                  final OffscreenLayerSurface ols = NativeWindowFactory.getOffscreenLayerSurface(surface, true);
                  if(null != ols && ols.isSurfaceLayerAttached()) {
                      // still having a valid OLS attached to surface (parent OLS could have been removed)
                      ols.detachSurfaceLayer();
                  }
                  CGL.releaseNSOpenGLLayer(nsOpenGLLayer);
                  if( null != gl3ShaderProgram ) {
                      gl3ShaderProgram.destroy(MacOSXCGLContext.this.gl.getGL3());
                      gl3ShaderProgram = null;
                  }
                  nsOpenGLLayer = 0;
              }
              if(0 != nsOpenGLLayerPFmt) {
                  CGL.deletePixelFormat(nsOpenGLLayerPFmt);
                  nsOpenGLLayerPFmt = 0;
              }
          }
          backingLayerHost = null;
          return true;
      }

      private final void validatePBufferConfig(long ctx) {
          final long drawableHandle = drawable.getHandle();
          if( needsSetContextPBuffer && 0 != drawableHandle && CGL.isNSOpenGLPixelBuffer(drawableHandle) ) {
              // Must associate the pbuffer with our newly-created context
              needsSetContextPBuffer = false;
              CGL.setContextPBuffer(ctx, drawableHandle);
              if(DEBUG) {
                  System.err.println("NS.validateDrawableConfig bind pbuffer "+toHexString(drawableHandle)+" -> ctx "+toHexString(ctx)); 
              }
          }
      }
      
      /** Returns true if size has been updated, otherwise false (same size). */
      private final boolean validateDrawableSizeConfig(long ctx) {
          final int width = drawable.getWidth();
          final int height = drawable.getHeight();
          if( lastWidth != width || lastHeight != height ) {
              lastWidth = drawable.getWidth();
              lastHeight = drawable.getHeight();
              if(DEBUG) {
                  System.err.println("NS.validateDrawableConfig size changed"); 
              }
              return true;
          }
          return false;
      }
      
      @Override
      public boolean copyImpl(long src, int mask) {
          CGL.copyContext(contextHandle, src, mask);
          return true;
      }

      @Override
      public boolean makeCurrent(long ctx) {
          final long cglCtx = CGL.getCGLContext(ctx);
          if(0 == cglCtx) {
              throw new InternalError("Null CGLContext for: "+this);
          }
          int err = CGL.CGLLockContext(cglCtx);
          if(CGL.kCGLNoError == err) {
              validatePBufferConfig(ctx); // required to handle pbuffer change ASAP
              return CGL.makeCurrentContext(ctx);
          } else if(DEBUG) {
              System.err.println("NSGL: Could not lock context: err 0x"+Integer.toHexString(err)+": "+this);
          }
          return false;
      }

      @Override
      public boolean release(long ctx) {
          try {
              gl.glFlush(); // w/o glFlush()/glFinish() OSX < 10.7 (NVidia driver) may freeze
          } catch (GLException gle) {
              if(DEBUG) {
                  System.err.println("MacOSXCGLContext.NSOpenGLImpl.release: INFO: glFlush() catched exception:");
                  gle.printStackTrace();
              }
          }
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

      @Override
      public boolean detachPBuffer() {
          needsSetContextPBuffer = true;
          // CGL.setContextPBuffer(contextHandle, 0); // doesn't work, i.e. not taking nil
          return true;
      }
      
      @Override
      public boolean setSwapInterval(int interval) {
          if(0 != nsOpenGLLayer) {
              CGL.setNSOpenGLLayerSwapInterval(nsOpenGLLayer, interval);
              vsyncTimeout = interval * (int)screenVSyncTimeout + 1000; // +1ms
              if(DEBUG) { System.err.println("NS setSwapInterval: "+vsyncTimeout+" micros"); }
          }
          CGL.setSwapInterval(contextHandle, interval);
          return true;
      }

      private int skipSync=0;
      
      @Override
      public boolean swapBuffers() {
          final boolean res;
          if( 0 != nsOpenGLLayer ) {
              if( validateDrawableSizeConfig(contextHandle) ) {
                  // skip wait-for-vsync for a few frames if size has changed,
                  // allowing to update the texture IDs ASAP.
                  skipSync = 10;
              }
              
              final int texID;
              final boolean valid;
              final boolean isFBO = drawable instanceof GLFBODrawableImpl;
              if( isFBO ){
                  texID = ((GLFBODrawableImpl)drawable).getTextureBuffer(GL.GL_FRONT).getName();
                  valid = 0 != texID;
              } else {
                  texID = 0;
                  valid = 0 != drawable.getHandle();
              }
              if(valid) {
                  if(0 == skipSync) {
                      // If v-sync is disabled, frames will be drawn as quickly as possible
                      // w/o delay but in sync w/ CALayer. Otherwise wait until next swap interval (v-sync).
                      CGL.waitUntilNSOpenGLLayerIsReady(nsOpenGLLayer, vsyncTimeout);
                  } else {
                      skipSync--;
                  }
                  res = CGL.flushBuffer(contextHandle);
                  if(res) {
                      if(isFBO) {
                          // trigger CALayer to update incl. possible surface change (texture)
                          CGL.setNSOpenGLLayerNeedsDisplayFBO(nsOpenGLLayer, texID);                          
                      } else {
                          // trigger CALayer to update incl. possible surface change (new pbuffer handle)
                          CGL.setNSOpenGLLayerNeedsDisplayPBuffer(nsOpenGLLayer, drawable.getHandle());                          
                      }
                  }
              } else {
                  res = true;
              }
          } else {
              res = CGL.flushBuffer(contextHandle);
          }
          return res;
      }

  }

  class CGLImpl implements GLBackendImpl {
      @Override
      public boolean isNSContext() { return false; }

      @Override
      public long create(long share, int ctp, int major, int minor) {
          long ctx = 0;
          MacOSXCGLGraphicsConfiguration config = (MacOSXCGLGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration();
          GLCapabilitiesImmutable chosenCaps = (GLCapabilitiesImmutable)config.getChosenCapabilities();
          long pixelFormat = MacOSXCGLGraphicsConfiguration.GLCapabilities2CGLPixelFormat(chosenCaps, ctp, major, minor);
          if (pixelFormat == 0) {
              throw new GLException("Unable to allocate pixel format with requested GLCapabilities");
          }
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
              ctx = ctxPB.get(0);

              if (0 != ctx) {
                  GLCapabilities fixedCaps = MacOSXCGLGraphicsConfiguration.CGLPixelFormat2GLCapabilities(pixelFormat);
                  fixedCaps = GLGraphicsConfigurationUtil.fixOpaqueGLCapabilities(fixedCaps, chosenCaps.isBackgroundOpaque());
                  { // determine on-/offscreen caps, since pformat is ambiguous 
                      fixedCaps.setFBO( false );         // n/a for CGLImpl 
                      fixedCaps.setPBuffer( fixedCaps.isPBuffer() && !chosenCaps.isOnscreen() );
                      fixedCaps.setBitmap( false );      // n/a in our OSX impl.
                      fixedCaps.setOnscreen( !fixedCaps.isPBuffer() );
                  }
                  config.setChosenCapabilities(fixedCaps);
                  if(DEBUG) {
                      System.err.println("CGL create fixedCaps: "+fixedCaps);
                  }
                  if(fixedCaps.isPBuffer()) {
                      // Must now associate the pbuffer with our newly-created context
                      res = CGL.CGLSetPBuffer(ctx, drawable.getHandle(), 0, 0, 0);
                      if (res != CGL.kCGLNoError) {
                          throw new GLException("Error code " + res + " while attaching context to pbuffer");
                      }
                  }              
              }
          } finally {
              CGL.CGLDestroyPixelFormat(pixelFormat);
          }
          return ctx;
      }

      @Override
      public boolean destroy(long ctx) {
          return CGL.CGLDestroyContext(ctx) == CGL.kCGLNoError;
      }

      @Override
      public boolean contextRealized(boolean realized) {
          return true;
      }

      @Override
      public boolean copyImpl(long src, int mask) {
          CGL.CGLCopyContext(src, contextHandle, mask);
          return true;
      }

      @Override
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

      @Override
      public boolean release(long ctx) {
          try {
              gl.glFlush(); // w/o glFlush()/glFinish() OSX < 10.7 (NVidia driver) may freeze
          } catch (GLException gle) {
              if(DEBUG) {
                  System.err.println("MacOSXCGLContext.CGLImpl.release: INFO: glFlush() catched exception:");
                  gle.printStackTrace();
              }
          }
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

      @Override
      public boolean detachPBuffer() {
          /* Doesn't work, i.e. not taking NULL
          final int res = CGL.CGLSetPBuffer(contextHandle, 0, 0, 0, 0);
          if (res != CGL.kCGLNoError) {
              throw new GLException("Error code " + res + " while detaching context from pbuffer");
          } */
          return true;
      }
      
      @Override
      public boolean setSwapInterval(int interval) {
          final IntBuffer lval = Buffers.newDirectIntBuffer(1);
          lval.put(0, interval);
          CGL.CGLSetParameter(contextHandle, CGL.kCGLCPSwapInterval, lval);
          return true;
      }
      @Override
      public boolean swapBuffers() {
          return CGL.kCGLNoError == CGL.CGLFlushDrawable(contextHandle);
      }
  }
}

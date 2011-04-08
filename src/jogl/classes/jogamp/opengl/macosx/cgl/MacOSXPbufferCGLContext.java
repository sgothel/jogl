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
 */

package jogamp.opengl.macosx.cgl;

import com.jogamp.common.nio.PointerBuffer;
import java.security.*;
import java.util.*;

import javax.media.opengl.*;
import javax.media.nativewindow.*;
import jogamp.opengl.*;

public class MacOSXPbufferCGLContext extends MacOSXCGLContext {

  // State for render-to-texture and render-to-texture-rectangle support
  private int textureTarget; // e.g. GL_TEXTURE_2D, GL_TEXTURE_RECTANGLE_NV
  private int texture;       // actual texture object

  private static boolean isTigerOrLater;

  static {
    String osVersion = Debug.getProperty("os.version", false, AccessController.getContext());
    StringTokenizer tok = new StringTokenizer(osVersion, ". ");
    int major = Integer.parseInt(tok.nextToken());
    int minor = Integer.parseInt(tok.nextToken());
    isTigerOrLater = ((major > 10) || (minor > 3));
  }

  public MacOSXPbufferCGLContext(MacOSXPbufferCGLDrawable drawable,
                                GLContext shareWith) {
    super(drawable, shareWith);
    initOpenGLImpl();
  }

  public void bindPbufferToTexture() {
    GL gl = getGL();
    gl.glBindTexture(textureTarget, texture);
    // FIXME: not clear whether this is really necessary, but since
    // the API docs seem to imply it is and since it doesn't seem to
    // impact performance, leaving it in
    CGL.setContextTextureImageToPBuffer(contextHandle, drawable.getHandle(), GL.GL_FRONT);
  }

  public void releasePbufferFromTexture() {
  }

  protected void makeCurrentImpl(boolean newCreated) throws GLException {
    if (getOpenGLMode() != ((MacOSXPbufferCGLDrawable)drawable).getOpenGLMode()) {
      setOpenGLMode(((MacOSXPbufferCGLDrawable)drawable).getOpenGLMode());
    }

    if (!impl.makeCurrent(contextHandle)) {
      throw new GLException("Error making Context (NS) current");
    }
            
    if (newCreated) {
      // Initialize render-to-texture support if requested
      DefaultGraphicsConfiguration config = (DefaultGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
      GLCapabilitiesImmutable capabilities = (GLCapabilitiesImmutable)config.getChosenCapabilities();
      GL gl = getGL();
      boolean rect = gl.isGL2GL3() && capabilities.getPbufferRenderToTextureRectangle();
      if (rect) {
        if (!gl.isExtensionAvailable("GL_EXT_texture_rectangle")) {
          System.err.println("MacOSXPbufferCGLContext: WARNING: GL_EXT_texture_rectangle extension not " +
                             "supported; skipping requested render_to_texture_rectangle support for pbuffer");
          rect = false;
        }
      }
      textureTarget = (rect ? GL2.GL_TEXTURE_RECTANGLE : GL.GL_TEXTURE_2D);
      int[] tmp = new int[1];
      gl.glGenTextures(1, tmp, 0);
      texture = tmp[0];
      gl.glBindTexture(textureTarget, texture);
      gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
      gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
      gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
      gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
      gl.glCopyTexImage2D(textureTarget, 0, GL.GL_RGB, 0, 0, drawable.getWidth(), drawable.getHeight(), 0);
    }
  }

  protected void releaseImpl() throws GLException {
    if (!impl.release(contextHandle)) {
      throw new GLException("Error releasing OpenGL Context (NS)");
    }
  }

  protected void destroyImpl() throws GLException {
      if (!impl.destroy(contextHandle)) {
        throw new GLException("Unable to delete OpenGL context");
      }
      if (DEBUG) {
        System.err.println("!!! Destroyed OpenGL context " + contextHandle);
      }
  }

  protected void setSwapIntervalImpl(int interval) {
    impl.setSwapInterval(contextHandle, interval);
    currentSwapInterval = impl.getSwapInterval() ;
  }

  public int getFloatingPointMode() {
    return GLPbuffer.APPLE_FLOAT;
  }

  protected boolean createImpl() throws GLException {
    DefaultGraphicsConfiguration config = (DefaultGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    GLCapabilitiesImmutable capabilities = (GLCapabilitiesImmutable)config.getChosenCapabilities();
    if (capabilities.getPbufferFloatingPointBuffers() &&
    !isTigerOrLater) {
      throw new GLException("Floating-point pbuffers supported only on OS X 10.4 or later");
    }
    // Change our OpenGL mode to match that of any share context before we create ourselves
    MacOSXCGLContext other = (MacOSXCGLContext) GLContextShareSet.getShareContext(this);
    if (other != null) {
      setOpenGLMode(other.getOpenGLMode());
    }
    // Will throw exception upon error
    isNSContext = impl.isNSContext();
    contextHandle = impl.create();

    if (!impl.makeCurrent(contextHandle)) {
      throw new GLException("Error making Context (NS:"+isNSContext()+") current");
    }
    if(!isNSContext()) { // FIXME: ??
        throw new GLException("Not a NS Context");
    }
    setGLFunctionAvailability(true, 0, 0, CTX_PROFILE_COMPAT|CTX_OPTION_ANY);
    return true;
  }

  //---------------------------------------------------------------------------
  // OpenGL "mode switching" functionality
  //
  private boolean haveSetOpenGLMode = false;
  // FIXME: should consider switching the default mode based on
  // whether the Java2D/JOGL bridge is active -- need to ask ourselves
  // whether it's more likely that we will share with a GLCanvas or a
  // GLJPanel when the bridge is turned on
  private int     openGLMode = MacOSXCGLDrawable.NSOPENGL_MODE;
  // Implementation object (either NSOpenGL-based or CGL-based)
  protected Impl impl;

  public void setOpenGLMode(int mode) {
    if (mode == openGLMode) {
      return;
    }
    if (haveSetOpenGLMode) {
      throw new GLException("Can't switch between using NSOpenGLPixelBuffer and CGLPBufferObj more than once");
    }
    destroyImpl();
    ((MacOSXPbufferCGLDrawable)drawable).setOpenGLMode(mode);
    openGLMode = mode;
    haveSetOpenGLMode = true;
    if (DEBUG) {
      System.err.println("Switching PBuffer context mode to " +
                         ((mode == MacOSXCGLDrawable.NSOPENGL_MODE) ? "NSOPENGL_MODE" : "CGL_MODE"));
    }
    initOpenGLImpl();
  }

  public int  getOpenGLMode() {
    return openGLMode;
  }

  private void initOpenGLImpl() {
    switch (openGLMode) {
      case MacOSXCGLDrawable.NSOPENGL_MODE:
        impl = new NSOpenGLImpl();
        break;
      case MacOSXCGLDrawable.CGL_MODE:
        impl = new CGLImpl();
        break;
      default:
        throw new InternalError("Illegal implementation mode " + openGLMode);
    }
  }

  // Abstract interface for implementation of this context (either
  // NSOpenGL-based or CGL-based)
  interface Impl {
    public boolean isNSContext();
    public long    create();
    public boolean destroy(long ctx);
    public boolean makeCurrent(long ctx);
    public boolean release(long ctx);
    public void    setSwapInterval(long ctx, int interval);
    public int     getSwapInterval();
  }

  // NSOpenGLContext-based implementation
  class NSOpenGLImpl implements Impl {
    public boolean isNSContext() { return true; }
    public long create() {
      DefaultGraphicsConfiguration config = (DefaultGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
      GLCapabilitiesImmutable capabilities = (GLCapabilitiesImmutable)config.getChosenCapabilities();
      if (capabilities.getPbufferFloatingPointBuffers() &&
          !isTigerOrLater) {
        throw new GLException("Floating-point pbuffers supported only on OS X 10.4 or later");
      }
      if (!MacOSXPbufferCGLContext.this.create(true, capabilities.getPbufferFloatingPointBuffers())) {
        throw new GLException("Error creating context for pbuffer");
      }
      // Must now associate the pbuffer with our newly-created context
      CGL.setContextPBuffer(contextHandle, drawable.getHandle());
      return contextHandle;
    }

    public boolean destroy(long ctx) {
      return CGL.deleteContext(ctx);
    }

    public boolean makeCurrent(long ctx) {
      return CGL.makeCurrentContext(ctx);
    }

    public boolean release(long ctx) {
      return CGL.clearCurrentContext(ctx);
    }

    private int currentSwapInterval = 0 ;

    public void setSwapInterval(long ctx, int interval) {
      CGL.setSwapInterval(ctx, interval);      
      currentSwapInterval = interval ;
    }
    public int getSwapInterval() {
        return currentSwapInterval;
    }
  }

  class CGLImpl implements Impl {
    public boolean isNSContext() { return false; }
    public long create() {
      // Find and configure share context
      MacOSXCGLContext other = (MacOSXCGLContext) GLContextShareSet.getShareContext(MacOSXPbufferCGLContext.this);
      long share = 0;
      if (other != null) {
        // Reconfigure pbuffer-based GLContexts
        if (other instanceof MacOSXPbufferCGLContext) {
          MacOSXPbufferCGLContext ctx = (MacOSXPbufferCGLContext) other;
          ctx.setOpenGLMode(MacOSXCGLDrawable.CGL_MODE);
        } else {
          if (other.isNSContext()) {
            throw new GLException("Can't share between NSOpenGLContexts and CGLContextObjs");
          }
        }
        share = other.getHandle();
        // Note we don't check for a 0 return value, since switching
        // the context's mode causes it to be destroyed and not
        // re-initialized until the next makeCurrent
      }

      // Set up pixel format attributes
      // FIXME: shall go into MacOSXCGLGraphicsConfiguration
      int[] attrs = new int[256];
      int i = 0;
      attrs[i++] = CGL.kCGLPFAPBuffer;
      DefaultGraphicsConfiguration config = (DefaultGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
      GLCapabilitiesImmutable capabilities = (GLCapabilitiesImmutable)config.getChosenCapabilities();
      if (capabilities.getPbufferFloatingPointBuffers())
        attrs[i++] = CGL.kCGLPFAColorFloat;
      if (capabilities.getDoubleBuffered())
        attrs[i++] = CGL.kCGLPFADoubleBuffer;
      if (capabilities.getStereo())
        attrs[i++] = CGL.kCGLPFAStereo;
      attrs[i++] = CGL.kCGLPFAColorSize;
      attrs[i++] = (capabilities.getRedBits() +
                    capabilities.getGreenBits() +
                    capabilities.getBlueBits());
      attrs[i++] = CGL.kCGLPFAAlphaSize;
      attrs[i++] = capabilities.getAlphaBits();
      attrs[i++] = CGL.kCGLPFADepthSize;
      attrs[i++] = capabilities.getDepthBits();
      // FIXME: should validate stencil size as is done in MacOSXWindowSystemInterface.m
      attrs[i++] = CGL.kCGLPFAStencilSize;
      attrs[i++] = capabilities.getStencilBits();
      attrs[i++] = CGL.kCGLPFAAccumSize;
      attrs[i++] = (capabilities.getAccumRedBits() +
                    capabilities.getAccumGreenBits() +
                    capabilities.getAccumBlueBits() +
                    capabilities.getAccumAlphaBits());
      if (capabilities.getSampleBuffers()) {
        attrs[i++] = CGL.kCGLPFASampleBuffers;
        attrs[i++] = 1;
        attrs[i++] = CGL.kCGLPFASamples;
        attrs[i++] = capabilities.getNumSamples();
      }

      // Use attribute array to select pixel format
      PointerBuffer fmt = PointerBuffer.allocateDirect(1);
      long[] numScreens = new long[1];
      int res = CGL.CGLChoosePixelFormat(attrs, 0, fmt, numScreens, 0);
      if (res != CGL.kCGLNoError) {
        throw new GLException("Error code " + res + " while choosing pixel format");
      }
      
      // Create new context
      PointerBuffer ctx = PointerBuffer.allocateDirect(1);
      if (DEBUG) {
        System.err.println("Share context for CGL-based pbuffer context is " + toHexString(share));
      }
      res = CGL.CGLCreateContext(fmt.get(0), share, ctx);
      CGL.CGLDestroyPixelFormat(fmt.get(0));
      if (res != CGL.kCGLNoError) {
        throw new GLException("Error code " + res + " while creating context");
      }
      // Attach newly-created context to the pbuffer
      res = CGL.CGLSetPBuffer(ctx.get(0), drawable.getHandle(), 0, 0, 0);
      if (res != CGL.kCGLNoError) {
        throw new GLException("Error code " + res + " while attaching context to pbuffer");
      }
      return ctx.get(0);
    }
    
    public boolean destroy(long ctx) {
      return (CGL.CGLDestroyContext(ctx) == CGL.kCGLNoError);
    }

    public boolean makeCurrent(long ctx) {
      return CGL.CGLSetCurrentContext(ctx) == CGL.kCGLNoError;
    }

    public boolean release(long ctx) {
      return (CGL.CGLSetCurrentContext(0) == CGL.kCGLNoError);
    }

    public void setSwapInterval(long ctx, int interval) {
      // For now not supported (not really relevant for off-screen contexts anyway)
    }
    public int getSwapInterval() {
        return 0;
    }
  }
}

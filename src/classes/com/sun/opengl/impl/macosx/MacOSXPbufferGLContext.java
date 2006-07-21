package com.sun.opengl.impl.macosx;

import java.security.*;
import java.util.*;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public class MacOSXPbufferGLContext extends MacOSXGLContext {
  protected MacOSXPbufferGLDrawable drawable;

  // State for render-to-texture and render-to-texture-rectangle support
  private int textureTarget; // e.g. GL_TEXTURE_2D, GL_TEXTURE_RECTANGLE_NV
  private int texture;       // actual texture object

  private static boolean isTigerOrLater;

  static {
    String osVersion =
      (String) AccessController.doPrivileged(new PrivilegedAction() {
	  public Object run() {
	    return System.getProperty("os.version");
	  }
	});
    StringTokenizer tok = new StringTokenizer(osVersion, ". ");
    int major = Integer.parseInt(tok.nextToken());
    int minor = Integer.parseInt(tok.nextToken());
    isTigerOrLater = ((major > 10) || (minor > 3));
  }

  public MacOSXPbufferGLContext(MacOSXPbufferGLDrawable drawable,
                                GLContext shareWith) {
    super(drawable, shareWith);
    this.drawable = drawable;
    initOpenGLImpl();
  }

  public void bindPbufferToTexture() {
    GL gl = getGL();
    gl.glBindTexture(textureTarget, texture);
    // FIXME: not clear whether this is really necessary, but since
    // the API docs seem to imply it is and since it doesn't seem to
    // impact performance, leaving it in
    CGL.setContextTextureImageToPBuffer(nsContext, drawable.getPbuffer(), GL.GL_FRONT);
  }

  public void releasePbufferFromTexture() {
  }

  protected int makeCurrentImpl() throws GLException {
    if (drawable.getPbuffer() == 0) {
      if (DEBUG) {
        System.err.println("Pbuffer not instantiated yet for " + this);
      }
      // pbuffer not instantiated yet
      return CONTEXT_NOT_CURRENT;
    }

    if (getOpenGLMode() != drawable.getOpenGLMode()) {
      setOpenGLMode(drawable.getOpenGLMode());
    }

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
    
    if (!impl.makeCurrent(nsContext)) {
      throw new GLException("Error making nsContext current");
    }
            
    if (created) {
      resetGLFunctionAvailability();

      // Initialize render-to-texture support if requested
      boolean rect = drawable.getCapabilities().getPbufferRenderToTextureRectangle();
      GL gl = getGL();
      if (rect) {
        if (!gl.isExtensionAvailable("GL_EXT_texture_rectangle")) {
          System.err.println("MacOSXPbufferGLContext: WARNING: GL_EXT_texture_rectangle extension not " +
                             "supported; skipping requested render_to_texture_rectangle support for pbuffer");
          rect = false;
        }
      }
      textureTarget = (rect ? GL.GL_TEXTURE_RECTANGLE_EXT : GL.GL_TEXTURE_2D);
      int[] tmp = new int[1];
      gl.glGenTextures(1, tmp, 0);
      texture = tmp[0];
      gl.glBindTexture(textureTarget, texture);
      gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
      gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
      gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
      gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
      gl.glCopyTexImage2D(textureTarget, 0, GL.GL_RGB, 0, 0, drawable.getWidth(), drawable.getHeight(), 0);

      return CONTEXT_CURRENT_NEW;
    }
    return CONTEXT_CURRENT;
  }

  protected void releaseImpl() throws GLException {
    if (!impl.release(nsContext)) {
      throw new GLException("Error releasing OpenGL nsContext");
    }
  }

  protected void destroyImpl() throws GLException {
    if (nsContext != 0) {
      if (!impl.destroy(nsContext)) {
        throw new GLException("Unable to delete OpenGL context");
      }
      if (DEBUG) {
        System.err.println("!!! Destroyed OpenGL context " + nsContext);
      }
      nsContext = 0;
      GLContextShareSet.contextDestroyed(this);
    }
  }

  public void setSwapInterval(int interval) {
    if (nsContext == 0) {
      throw new GLException("OpenGL context not current");
    }
    impl.setSwapInterval(nsContext, interval);
  }

  public int getFloatingPointMode() {
    return GLPbuffer.APPLE_FLOAT;
  }

  protected boolean create() {
    GLCapabilities capabilities = drawable.getCapabilities();
    if (capabilities.getPbufferFloatingPointBuffers() &&
	!isTigerOrLater) {
      throw new GLException("Floating-point pbuffers supported only on OS X 10.4 or later");
    }
    // Change our OpenGL mode to match that of any share context before we create ourselves
    MacOSXGLContext other = (MacOSXGLContext) GLContextShareSet.getShareContext(this);
    if (other != null) {
      setOpenGLMode(other.getOpenGLMode());
    }
    // Will throw exception upon error
    nsContext = impl.create();
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
  private int     openGLMode = MacOSXGLDrawable.NSOPENGL_MODE;
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
    drawable.setOpenGLMode(mode);
    openGLMode = mode;
    haveSetOpenGLMode = true;
    if (DEBUG) {
      System.err.println("Switching PBuffer context mode to " +
                         ((mode == MacOSXGLDrawable.NSOPENGL_MODE) ? "NSOPENGL_MODE" : "CGL_MODE"));
    }
    initOpenGLImpl();
  }

  public int  getOpenGLMode() {
    return openGLMode;
  }

  private void initOpenGLImpl() {
    switch (openGLMode) {
      case MacOSXGLDrawable.NSOPENGL_MODE:
        impl = new NSOpenGLImpl();
        break;
      case MacOSXGLDrawable.CGL_MODE:
        impl = new CGLImpl();
        break;
      default:
        throw new InternalError("Illegal implementation mode " + openGLMode);
    }
  }

  // Abstract interface for implementation of this context (either
  // NSOpenGL-based or CGL-based)
  interface Impl {
    public long    create();
    public boolean destroy(long ctx);
    public boolean makeCurrent(long ctx);
    public boolean release(long ctx);
    public void    setSwapInterval(long ctx, int interval);
  }

  // NSOpenGLContext-based implementation
  class NSOpenGLImpl implements Impl {
    public long create() {
      GLCapabilities capabilities = drawable.getCapabilities();
      if (capabilities.getPbufferFloatingPointBuffers() &&
          !isTigerOrLater) {
        throw new GLException("Floating-point pbuffers supported only on OS X 10.4 or later");
      }
      if (!MacOSXPbufferGLContext.this.create(true, capabilities.getPbufferFloatingPointBuffers())) {
        throw new GLException("Error creating context for pbuffer");
      }
      // Must now associate the pbuffer with our newly-created context
      CGL.setContextPBuffer(nsContext, drawable.getPbuffer());
      return nsContext;
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

    public void setSwapInterval(long ctx, int interval) {
      CGL.setSwapInterval(ctx, interval);      
    }
  }

  class CGLImpl implements Impl {
    public long create() {
      // Find and configure share context
      MacOSXGLContext other = (MacOSXGLContext) GLContextShareSet.getShareContext(MacOSXPbufferGLContext.this);
      long share = 0;
      if (other != null) {
        // Reconfigure pbuffer-based GLContexts
        if (other instanceof MacOSXPbufferGLContext) {
          MacOSXPbufferGLContext ctx = (MacOSXPbufferGLContext) other;
          ctx.setOpenGLMode(MacOSXGLDrawable.CGL_MODE);
        } else {
          if (other.getOpenGLMode() != MacOSXGLDrawable.CGL_MODE) {
            throw new GLException("Can't share between NSOpenGLContexts and CGLContextObjs");
          }
        }
        share = other.getNSContext();
        // Note we don't check for a 0 return value, since switching
        // the context's mode causes it to be destroyed and not
        // re-initialized until the next makeCurrent
      }

      // Set up pixel format attributes
      int[] attrs = new int[256];
      int i = 0;
      attrs[i++] = CGL.kCGLPFAPBuffer;
      GLCapabilities capabilities = drawable.getCapabilities();
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
      long[] fmt = new long[1];
      long[] numScreens = new long[1];
      int res = CGL.CGLChoosePixelFormat(attrs, 0, fmt, 0, numScreens, 0);
      if (res != CGL.kCGLNoError) {
        throw new GLException("Error code " + res + " while choosing pixel format");
      }
      
      // Create new context
      long[] ctx = new long[1];
      if (DEBUG) {
        System.err.println("Share context for CGL-based pbuffer context is " + toHexString(share));
      }
      res = CGL.CGLCreateContext(fmt[0], share, ctx, 0);
      CGL.CGLDestroyPixelFormat(fmt[0]);
      if (res != CGL.kCGLNoError) {
        throw new GLException("Error code " + res + " while creating context");
      }
      // Attach newly-created context to the pbuffer
      res = CGL.CGLSetPBuffer(ctx[0], drawable.getPbuffer(), 0, 0, 0);
      if (res != CGL.kCGLNoError) {
        throw new GLException("Error code " + res + " while attaching context to pbuffer");
      }
      return ctx[0];
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
  }
}

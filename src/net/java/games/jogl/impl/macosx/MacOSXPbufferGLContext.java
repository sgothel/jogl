package net.java.games.jogl.impl.macosx;

import java.security.*;
import java.util.*;

import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class MacOSXPbufferGLContext extends MacOSXGLContext {
  private static final boolean DEBUG = Debug.debug("MacOSXPbufferGLContext");
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
  
  protected int  initWidth;
  protected int  initHeight;

  private long pBuffer;
  
  protected int  width;
  protected int  height;

  // State for render-to-texture and render-to-texture-rectangle support
  private boolean created;
  private int textureTarget; // e.g. GL_TEXTURE_2D, GL_TEXTURE_RECTANGLE_NV
  private int texture;       // actual texture object

  public MacOSXPbufferGLContext(GLCapabilities capabilities, int initialWidth, int initialHeight) {
    super(null, capabilities, null, null);
    this.initWidth  = initialWidth;
    this.initHeight = initialHeight;
  }

  public boolean canCreatePbufferContext() {
    return false;
  }

  public GLContext createPbufferContext(GLCapabilities capabilities,
                                        int initialWidth,
                                        int initialHeight) {
    throw new GLException("Not supported");
  }

  public void bindPbufferToTexture() {
    GL gl = getGL();
    gl.glBindTexture(textureTarget, texture);
    // FIXME: not clear whether this is really necessary, but since
    // the API docs seem to imply it is and since it doesn't seem to
    // impact performance, leaving it in
    CGL.setContextTextureImageToPBuffer(nsContext, pBuffer, GL.GL_FRONT);
  }

  public void releasePbufferFromTexture() {
  }

  public void createPbuffer(long parentView, long parentContext) {
    GL gl = getGL();
    // Must initally grab OpenGL function pointers while parent's
    // context is current because otherwise we don't have the cgl
    // extensions available to us
    resetGLFunctionAvailability();

    int renderTarget;
    if (capabilities.getOffscreenRenderToTextureRectangle()) {
      width = initWidth;
      height = initHeight;
      renderTarget = GL.GL_TEXTURE_RECTANGLE_EXT;
    } else {
      width = getNextPowerOf2(initWidth);
      height = getNextPowerOf2(initHeight);
      renderTarget = GL.GL_TEXTURE_2D;
    }

    int internalFormat = GL.GL_RGBA;
    if (capabilities.getOffscreenFloatingPointBuffers()) {
      if (!gl.isExtensionAvailable("GL_APPLE_float_pixels")) {
	throw new GLException("Floating-point support (GL_APPLE_float_pixels) not available");
      }
      switch (capabilities.getRedBits()) {
        case 16: internalFormat = GL.GL_RGBA_FLOAT16_APPLE; break;
        case 32: internalFormat = GL.GL_RGBA_FLOAT32_APPLE; break;
        default: throw new GLException("Invalid floating-point bit depth (only 16 and 32 supported)");
      }
    }
		
    pBuffer = CGL.createPBuffer(renderTarget, internalFormat, width, height);
    if (pBuffer == 0) {
      throw new GLException("pbuffer creation error: CGL.createPBuffer() failed");
    }
	
    if (DEBUG) {
      System.err.println("Created pbuffer 0x" + Long.toHexString(pBuffer) + ", " + width + " x " + height + " for " + this);
    }
  }

  protected synchronized boolean makeCurrent(Runnable initAction) throws GLException {
    created = false;

    if (pBuffer == 0) {
      if (DEBUG) {
        System.err.println("Pbuffer not instantiated yet for " + this);
      }
      // pbuffer not instantiated yet
      return false;
    }

    boolean res = super.makeCurrent(initAction);
    if (created) {
      // Initialize render-to-texture support if requested
      boolean rect = capabilities.getOffscreenRenderToTextureRectangle();
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
      gl.glGenTextures(1, tmp);
      texture = tmp[0];
      gl.glBindTexture(textureTarget, texture);
      gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
      gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
      gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
      gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
      gl.glCopyTexImage2D(textureTarget, 0, GL.GL_RGB, 0, 0, width, height, 0);
    }
    return res;
  }

  public void destroyPBuffer() {
    if (this.pBuffer != 0) {
      CGL.destroyPBuffer(nsContext, pBuffer);
    }
    this.pBuffer = 0;
    
    if (DEBUG) {
      System.err.println("Destroyed pbuffer " + width + " x " + height);
    }
  }

  public void handleModeSwitch(long parentView, long parentContext) {
    throw new GLException("Not yet implemented");
  }

  protected boolean isOffscreen() {
    // FIXME: currently the only caller of this won't cause proper
    // resizing of the pbuffer anyway.
    return false;
  }
  
  protected void destroyImpl() throws GLException {
    destroyPBuffer();
  }

  public void swapBuffers() throws GLException {
    // FIXME: do we need to do anything if the pbuffer is double-buffered?
  }

  public int getFloatingPointMode() {
    return GLPbuffer.APPLE_FLOAT;
  }

  private int getNextPowerOf2(int number) {
    if (((number-1) & number) == 0) {
      //ex: 8 -> 0b1000; 8-1=7 -> 0b0111; 0b1000&0b0111 == 0
      return number;
    }
    int power = 0;
    while (number > 0) {
      number = number>>1;
      power++;
    }
    return (1<<power);
  }

  protected boolean create() {
    if (capabilities.getOffscreenFloatingPointBuffers() &&
	!isTigerOrLater) {
      throw new GLException("Floating-point pbuffers supported only on OS X 10.4 or later");
    }
    if (!super.create(true, capabilities.getOffscreenFloatingPointBuffers())) {
      return false;
    }
    created = true;
    // Must now associate the pbuffer with our newly-created context
    CGL.setContextPBuffer(nsContext, pBuffer);
    return true;
  }
}

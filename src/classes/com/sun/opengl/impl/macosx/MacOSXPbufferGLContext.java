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

    int res = super.makeCurrentImpl();
    if (res == CONTEXT_CURRENT_NEW) {
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
    }
    return res;
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
    if (!super.create(true, capabilities.getPbufferFloatingPointBuffers())) {
      return false;
    }
    // Must now associate the pbuffer with our newly-created context
    CGL.setContextPBuffer(nsContext, drawable.getPbuffer());
    return true;
  }
}

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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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

package net.java.games.jogl.impl.x11;

import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class X11PbufferGLContext extends X11GLContext {
  private static final boolean DEBUG = Debug.debug("X11PbufferGLContext");

  private int  initWidth;
  private int  initHeight;

  private long buffer;   // GLXPbuffer
  private GLXFBConfig fbConfig;
  private int  width;
  private int  height;

  // FIXME: kept around because we create the OpenGL context lazily to
  // better integrate with the X11GLContext framework
  private long parentContext;

  private static final int MAX_PFORMATS = 256;
  private static final int MAX_ATTRIBS  = 256;

  // FIXME: figure out how to support render-to-texture and
  // render-to-texture-rectangle (which appear to be supported, though
  // it looks like floating-point buffers are not)

  public X11PbufferGLContext(GLCapabilities capabilities, int initialWidth, int initialHeight) {
    super(null, capabilities, null, null);
    this.initWidth  = initialWidth;
    this.initHeight = initialHeight;
    if (initWidth <= 0 || initHeight <= 0) {
      throw new GLException("Initial width and height of pbuffer must be positive (were (" +
			    initWidth + ", " + initHeight + "))");
    }
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
    // FIXME: figure out how to implement this
    throw new GLException("Not yet implemented");
  }

  public void releasePbufferFromTexture() {
    // FIXME: figure out how to implement this
    throw new GLException("Not yet implemented");
  }

  public void createPbuffer(long display, long parentContext) {
    if (display == 0) {
      throw new GLException("Null display");
    }
    
    if (parentContext == 0) {
      throw new GLException("Null parentContext");
    }

    if (capabilities.getOffscreenFloatingPointBuffers()) {
      throw new GLException("Floating-point pbuffers not supported yet on X11");
    }

    if (capabilities.getOffscreenRenderToTexture()) {
      throw new GLException("Render-to-texture pbuffers not supported yet on X11");
    }

    if (capabilities.getOffscreenRenderToTextureRectangle()) {
      throw new GLException("Render-to-texture-rectangle pbuffers not supported yet on X11");
    }

    int[]   iattributes = new int  [2*MAX_ATTRIBS];
    float[] fattributes = new float[2*MAX_ATTRIBS];
    int nfattribs = 0;
    int niattribs = 0;

    // Since we are trying to create a pbuffer, the GLXFBConfig we
    // request (and subsequently use) must be "p-buffer capable".
    iattributes[niattribs++] = GL.GLX_DRAWABLE_TYPE;
    iattributes[niattribs++] = GL.GLX_PBUFFER_BIT;

    iattributes[niattribs++] = GL.GLX_RENDER_TYPE;
    iattributes[niattribs++] = GL.GLX_RGBA_BIT;

    iattributes[niattribs++] = GLX.GLX_DOUBLEBUFFER;
    if (capabilities.getDoubleBuffered()) {
      iattributes[niattribs++] = GL.GL_TRUE;
    } else {
      iattributes[niattribs++] = GL.GL_FALSE;
    }

    iattributes[niattribs++] = GLX.GLX_DEPTH_SIZE;
    iattributes[niattribs++] = capabilities.getDepthBits();

    iattributes[niattribs++] = GLX.GLX_RED_SIZE;
    iattributes[niattribs++] = capabilities.getRedBits();

    iattributes[niattribs++] = GLX.GLX_GREEN_SIZE;
    iattributes[niattribs++] = capabilities.getGreenBits();

    iattributes[niattribs++] = GLX.GLX_BLUE_SIZE;
    iattributes[niattribs++] = capabilities.getBlueBits();

    iattributes[niattribs++] = GLX.GLX_ALPHA_SIZE;
    iattributes[niattribs++] = capabilities.getAlphaBits();

    if (capabilities.getStencilBits() > 0) {
      iattributes[niattribs++] = GLX.GLX_STENCIL_SIZE;
      iattributes[niattribs++] = capabilities.getStencilBits();
    }

    if (capabilities.getAccumRedBits()   > 0 ||
        capabilities.getAccumGreenBits() > 0 ||
        capabilities.getAccumBlueBits()  > 0) {
      iattributes[niattribs++] = GLX.GLX_ACCUM_RED_SIZE;
      iattributes[niattribs++] = capabilities.getAccumRedBits();
      iattributes[niattribs++] = GLX.GLX_ACCUM_GREEN_SIZE;
      iattributes[niattribs++] = capabilities.getAccumGreenBits();
      iattributes[niattribs++] = GLX.GLX_ACCUM_BLUE_SIZE;
      iattributes[niattribs++] = capabilities.getAccumBlueBits();
    }

    // FIXME: add FSAA support? Don't want to get into a situation
    // where we have to retry the glXChooseFBConfig call if it fails
    // due to a lack of an antialiased visual...

    iattributes[niattribs++] = 0; // null-terminate

    int screen = 0; // FIXME: provide way to specify this?
    int[] nelementsTmp = new int[1];
    GLXFBConfig[] fbConfigs = GLX.glXChooseFBConfig(display, screen, iattributes, nelementsTmp);
    if (fbConfigs == null || fbConfigs.length == 0 || fbConfigs[0] == null) {
      throw new GLException("pbuffer creation error: glXChooseFBConfig() failed");
    }
    // Note that we currently don't allow selection of anything but
    // the first GLXFBConfig in the returned list
    GLXFBConfig fbConfig = fbConfigs[0];
    int nelements = nelementsTmp[0];
    if (nelements <= 0) {
      throw new GLException("pbuffer creation error: couldn't find a suitable frame buffer configuration");
    }

    if (DEBUG) {
      System.err.println("Found " + fbConfigs.length + " matching GLXFBConfigs");
      System.err.println("Parameters of default one:");
      System.err.println("render type: 0x" + Integer.toHexString(queryFBConfig(display, fbConfig, GLX.GLX_RENDER_TYPE)));
      System.err.println("rgba: " + ((queryFBConfig(display, fbConfig, GLX.GLX_RENDER_TYPE) & GLX.GLX_RGBA_BIT) != 0));
      System.err.println("r: " + queryFBConfig(display, fbConfig, GLX.GLX_RED_SIZE));
      System.err.println("g: " + queryFBConfig(display, fbConfig, GLX.GLX_GREEN_SIZE));
      System.err.println("b: " + queryFBConfig(display, fbConfig, GLX.GLX_BLUE_SIZE));
      System.err.println("a: " + queryFBConfig(display, fbConfig, GLX.GLX_ALPHA_SIZE));
      System.err.println("depth: " + queryFBConfig(display, fbConfig, GLX.GLX_DEPTH_SIZE));
      System.err.println("double buffered: " + queryFBConfig(display, fbConfig, GLX.GLX_DOUBLEBUFFER));
    }

    // Create the p-buffer.
    niattribs = 0;

    iattributes[niattribs++] = GL.GLX_PBUFFER_WIDTH;
    iattributes[niattribs++] = initWidth;
    iattributes[niattribs++] = GL.GLX_PBUFFER_HEIGHT;
    iattributes[niattribs++] = initHeight;

    iattributes[niattribs++] = 0;

    long tmpBuffer = GLX.glXCreatePbuffer(display, fbConfig, iattributes);
    if (tmpBuffer == 0) {
      // FIXME: query X error code for detail error message
      throw new GLException("pbuffer creation error: glXCreatePbuffer() failed");
    }

    // Set up instance variables
    this.display = display;
    mostRecentDisplay = display;
    this.parentContext = parentContext;
    buffer = tmpBuffer;
    this.fbConfig = fbConfig;

    // Determine the actual width and height we were able to create.
    int[] tmp = new int[1];
    GLX.glXQueryDrawable(display, (int) buffer, GL.GLX_WIDTH, tmp);
    width = tmp[0];
    GLX.glXQueryDrawable(display, (int) buffer, GL.GLX_HEIGHT, tmp);
    height = tmp[0];

    if (DEBUG) {
      System.err.println("Created pbuffer " + width + " x " + height);
    }
  }

  protected synchronized boolean makeCurrent(Runnable initAction) throws GLException {
    if (buffer == 0) {
      // pbuffer not instantiated yet
      return false;
    }

    lockAWT();
    try {
      boolean created = false;
      if (context == 0) {
        create();
        if (DEBUG) {
          System.err.println("!!! Created GL context for " + getClass().getName());
        }
        created = true;
      }

      if (!GLX.glXMakeContextCurrent(display, buffer, buffer, context)) {
        throw new GLException("Error making context current");
      }

      if (created) {
        resetGLFunctionAvailability();
        initAction.run();
      }
      return true;
    } finally {
      unlockAWT();
    }
  }

  protected synchronized void free() throws GLException {
    lockAWT();
    try {
      if (!GLX.glXMakeContextCurrent(display, 0, 0, 0)) {
        throw new GLException("Error freeing OpenGL context");
      }
    } finally {
      unlockAWT();
    }
  }

  public void handleModeSwitch(long parentHdc, long parentHglrc) {
    throw new GLException("Not yet implemented");
  }

  protected boolean isOffscreen() {
    // FIXME: currently the only caller of this won't cause proper
    // resizing of the pbuffer anyway.
    return false;
  }

  public int getOffscreenContextBufferedImageType() {
    throw new GLException("Should not call this");
  }

  public int getOffscreenContextReadBuffer() {
    throw new GLException("Should not call this");
  }

  public boolean offscreenImageNeedsVerticalFlip() {
    throw new GLException("Should not call this");
  }

  protected void create() {
    if (DEBUG) {
      System.err.println("Creating context for pbuffer " + width + " x " + height);
    }

    // Create a gl context for the p-buffer.
    // FIXME: provide option to not share display lists with subordinate pbuffer?
    context = GLX.glXCreateNewContext(display, fbConfig, GL.GLX_RGBA_TYPE, parentContext, true);
    if (context == 0) {
      throw new GLException("pbuffer creation error: glXCreateNewContext() failed");
    }

    if (DEBUG) {
      System.err.println("Created context for pbuffer " + width + " x " + height);
    }
  }

  protected void destroyImpl() throws GLException {
    lockAWT();
    try {
      if (context != 0) {
        super.destroyImpl();
        GLX.glXDestroyPbuffer(display, buffer);
        buffer = 0;
      }
    } finally {
      unlockAWT();
    }
  }

  public void swapBuffers() throws GLException {
    // FIXME: do we need to do anything if the pbuffer is double-buffered?
  }

  private int queryFBConfig(long display, GLXFBConfig fbConfig, int attrib) {
    int[] tmp = new int[1];
    if (GLX.glXGetFBConfigAttrib(display, fbConfig, attrib, tmp) != 0) {
      throw new GLException("glXGetFBConfigAttrib failed");
    }
    return tmp[0];
  }
}

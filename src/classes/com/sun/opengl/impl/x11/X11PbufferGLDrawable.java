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

package com.sun.opengl.impl.x11;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public class X11PbufferGLDrawable extends X11GLDrawable {
  private int  initWidth;
  private int  initHeight;

  // drawable in superclass is a GLXPbuffer
  private GLXFBConfig fbConfig;
  private int  width;
  private int  height;

  protected static final int MAX_PFORMATS = 256;
  protected static final int MAX_ATTRIBS  = 256;

  public X11PbufferGLDrawable(GLCapabilities capabilities, int initialWidth, int initialHeight) {
    super(capabilities, null);
    this.initWidth  = initialWidth;
    this.initHeight = initialHeight;
    if (initWidth <= 0 || initHeight <= 0) {
      throw new GLException("Initial width and height of pbuffer must be positive (were (" +
			    initWidth + ", " + initHeight + "))");
    }

    if (DEBUG) {
      System.out.println("Pbuffer caps on init: " + capabilities +
                         (capabilities.getPbufferRenderToTexture() ? " [rtt]" : "") +
                         (capabilities.getPbufferRenderToTextureRectangle() ? " [rect]" : "") +
                         (capabilities.getPbufferFloatingPointBuffers() ? " [float]" : ""));
    }

    createPbuffer(X11GLDrawableFactory.getDisplayConnection());
  }

  public GLContext createContext(GLContext shareWith) {
    return new X11PbufferGLContext(this, shareWith);
  }

  public void destroy() {
    lockToolkit();
    if (drawable != 0) {
      GLX.glXDestroyPbuffer(display, drawable);
      drawable = 0;
    }
    unlockToolkit();
    display = 0;
  }

  public void setSize(int width, int height) {
    // FIXME
    throw new GLException("Not yet implemented");
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public void createPbuffer(long display) {
    lockToolkit();
    try {
      if (display == 0) {
        throw new GLException("Null display");
      }
    
      if (capabilities.getPbufferRenderToTexture()) {
        throw new GLException("Render-to-texture pbuffers not supported yet on X11");
      }

      if (capabilities.getPbufferRenderToTextureRectangle()) {
        throw new GLException("Render-to-texture-rectangle pbuffers not supported yet on X11");
      }

      int screen = 0; // FIXME: provide way to specify this?
      int[] iattributes = X11GLDrawableFactory.glCapabilities2AttribList(capabilities,
                                                                         X11GLDrawableFactory.isMultisampleAvailable(),
                                                                         true, display, screen);

      int[] nelementsTmp = new int[1];
      GLXFBConfig[] fbConfigs = GLX.glXChooseFBConfig(display, screen, iattributes, 0, nelementsTmp, 0);
      if (fbConfigs == null || fbConfigs.length == 0 || fbConfigs[0] == null) {
        throw new GLException("pbuffer creation error: glXChooseFBConfig() failed");
      }
      int nelements = nelementsTmp[0];
      if (nelements <= 0) {
        throw new GLException("pbuffer creation error: couldn't find a suitable frame buffer configuration");
      }
      // Note that we currently don't allow selection of anything but
      // the first GLXFBConfig in the returned list
      GLXFBConfig fbConfig = fbConfigs[0];

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
      int niattribs = 0;

      iattributes[niattribs++] = GLXExt.GLX_PBUFFER_WIDTH;
      iattributes[niattribs++] = initWidth;
      iattributes[niattribs++] = GLXExt.GLX_PBUFFER_HEIGHT;
      iattributes[niattribs++] = initHeight;

      iattributes[niattribs++] = 0;

      long tmpBuffer = GLX.glXCreatePbuffer(display, fbConfig, iattributes, 0);
      if (tmpBuffer == 0) {
        // FIXME: query X error code for detail error message
        throw new GLException("pbuffer creation error: glXCreatePbuffer() failed");
      }

      // Set up instance variables
      this.display = display;
      drawable = tmpBuffer;
      this.fbConfig = fbConfig;
      
      // Pick innocent query values if multisampling or floating point buffers not available
      int sbAttrib      = X11GLDrawableFactory.isMultisampleAvailable() ? GLXExt.GLX_SAMPLE_BUFFERS_ARB : GLX.GLX_RED_SIZE;
      int samplesAttrib = X11GLDrawableFactory.isMultisampleAvailable() ? GLXExt.GLX_SAMPLES_ARB : GLX.GLX_RED_SIZE;
      int floatNV       = capabilities.getPbufferFloatingPointBuffers() ? GLX.GLX_FLOAT_COMPONENTS_NV : GLX.GLX_RED_SIZE;

      // Query the fbconfig to determine its GLCapabilities
      int[] iattribs = {
        GLX.GLX_DOUBLEBUFFER,
        GLX.GLX_STEREO,
        GLX.GLX_RED_SIZE,
        GLX.GLX_GREEN_SIZE,
        GLX.GLX_BLUE_SIZE,
        GLX.GLX_ALPHA_SIZE,
        GLX.GLX_DEPTH_SIZE,
        GLX.GLX_STENCIL_SIZE,
        GLX.GLX_ACCUM_RED_SIZE,
        GLX.GLX_ACCUM_GREEN_SIZE,
        GLX.GLX_ACCUM_BLUE_SIZE,
        GLX.GLX_ACCUM_ALPHA_SIZE,
        sbAttrib,
        samplesAttrib,        
        floatNV
      };

      int[] ivalues = new int[iattribs.length];
      queryFBConfig(display, fbConfig, iattribs, iattribs.length, ivalues);
      setChosenGLCapabilities(X11GLDrawableFactory.attribList2GLCapabilities(iattribs, iattribs.length, ivalues, true));

      // Determine the actual width and height we were able to create.
      int[] tmp = new int[1];
      GLX.glXQueryDrawable(display, drawable, GLXExt.GLX_WIDTH, tmp, 0);
      width = tmp[0];
      GLX.glXQueryDrawable(display, drawable, GLXExt.GLX_HEIGHT, tmp, 0);
      height = tmp[0];

      if (DEBUG) {
        System.err.println("Created pbuffer " + width + " x " + height);
      }
    } finally {
      unlockToolkit();
    }
  }

  public int getFloatingPointMode() {
    // Floating-point pbuffers currently require NVidia hardware on X11
    return GLPbuffer.NV_FLOAT;
  }
  
  public GLXFBConfig getFBConfig() {
    return fbConfig;
  }

  private int queryFBConfig(long display, GLXFBConfig fbConfig, int attrib) {
    int[] tmp = new int[1];
    if (GLX.glXGetFBConfigAttrib(display, fbConfig, attrib, tmp, 0) != 0) {
      throw new GLException("glXGetFBConfigAttrib failed");
    }
    return tmp[0];
  }

  private void queryFBConfig(long display, GLXFBConfig fbConfig, int[] attribs, int nattribs, int[] values) {
    int[] tmp = new int[1];
    for (int i = 0; i < nattribs; i++) {
      if (GLX.glXGetFBConfigAttrib(display, fbConfig, attribs[i], tmp, 0) != 0) {
        throw new GLException("glXGetFBConfigAttrib failed");
      }
      values[i] = tmp[0];
    }
  }
}

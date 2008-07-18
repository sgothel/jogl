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

package com.sun.opengl.impl.x11.glx;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.opengl.impl.x11.*;

import java.nio.LongBuffer;

public class X11PbufferGLXDrawable extends X11GLXDrawable {
  // drawable in superclass is a GLXPbuffer
  private long fbConfig;

  protected static final int MAX_PFORMATS = 256;
  protected static final int MAX_ATTRIBS  = 256;

  protected X11PbufferGLXDrawable(GLDrawableFactory factory, GLCapabilities capabilities, 
                                  int initialWidth, int initialHeight) {
    super(factory, new NullWindow(), true, capabilities, null);
    if (initialWidth <= 0 || initialHeight <= 0) {
      throw new GLException("Initial width and height of pbuffer must be positive (were (" +
			    initialWidth + ", " + initialHeight + "))");
    }
    NullWindow nw = (NullWindow) getNativeWindow();
    nw.setSize(initialWidth, initialHeight);

    if (DEBUG) {
      System.out.println("Pbuffer caps on init: " + capabilities +
                         (capabilities.getPbufferRenderToTexture() ? " [rtt]" : "") +
                         (capabilities.getPbufferRenderToTextureRectangle() ? " [rect]" : "") +
                         (capabilities.getPbufferFloatingPointBuffers() ? " [float]" : ""));
    }

    nw.setDisplayHandle(X11GLXDrawableFactory.getDisplayConnection());
    createPbuffer();
  }

  public GLContext createContext(GLContext shareWith) {
    return new X11PbufferGLXContext(this, shareWith);
  }

  public void destroy() {
    getFactory().lockToolkit();
    try {
        NullWindow nw = (NullWindow) getNativeWindow();
        if (nw.getSurfaceHandle() != 0) {
          GLX.glXDestroyPbuffer(nw.getDisplayHandle(), nw.getSurfaceHandle());
          nw.setSurfaceHandle(0);
        }
        nw.setDisplayHandle(0);
    } finally {
        getFactory().unlockToolkit();
    }
    super.destroy();
  }

  public void setSize(int width, int height) {
    super.setSize(width, height);
    destroy();
    ((NullWindow)getNativeWindow()).setDisplayHandle(X11GLXDrawableFactory.getDisplayConnection());
    createPbuffer();
  }

  public void createPbuffer() {
    getFactory().lockToolkit();
    try {
      NullWindow nw = (NullWindow) getNativeWindow();
      long display = nw.getDisplayHandle();
      if (nw.getDisplayHandle()== 0) {
        throw new GLException("Null display");
      }
      int screen = X11Lib.DefaultScreen(display);
      nw.setScreenIndex(screen);
    
      if (getCapabilities().getPbufferRenderToTexture()) {
        throw new GLException("Render-to-texture pbuffers not supported yet on X11");
      }

      if (getCapabilities().getPbufferRenderToTextureRectangle()) {
        throw new GLException("Render-to-texture-rectangle pbuffers not supported yet on X11");
      }

      int[] iattributes = X11GLXDrawableFactory.glCapabilities2AttribList(getCapabilities(),
                                                                         ((X11GLXDrawableFactory)getFactory()).isMultisampleAvailable(),
                                                                         true, display, screen);

      int[] nelementsTmp = new int[1];
      LongBuffer fbConfigs = GLX.glXChooseFBConfig(display, screen, iattributes, 0, nelementsTmp, 0);
      if (fbConfigs == null || fbConfigs.limit() == 0 || fbConfigs.get(0) == 0) {
        throw new GLException("pbuffer creation error: glXChooseFBConfig() failed");
      }
      int nelements = nelementsTmp[0];
      if (nelements <= 0) {
        throw new GLException("pbuffer creation error: couldn't find a suitable frame buffer configuration");
      }
      // Note that we currently don't allow selection of anything but
      // the first GLXFBConfig in the returned list
      long fbConfig = fbConfigs.get(0);

      if (DEBUG) {
        System.err.println("Found " + fbConfigs.limit() + " matching GLXFBConfigs");
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
      iattributes[niattribs++] = nw.getWidth();
      iattributes[niattribs++] = GLXExt.GLX_PBUFFER_HEIGHT;
      iattributes[niattribs++] = nw.getHeight();

      iattributes[niattribs++] = 0;

      long drawable = GLX.glXCreatePbuffer(display, fbConfig, iattributes, 0);
      if (drawable == 0) {
        // FIXME: query X error code for detail error message
        throw new GLException("pbuffer creation error: glXCreatePbuffer() failed");
      }

      // Set up instance variables
      nw.setSurfaceHandle(drawable);
      this.fbConfig = fbConfig;
      
      // Pick innocent query values if multisampling or floating point buffers not available
      int sbAttrib      = ((X11GLXDrawableFactory)getFactory()).isMultisampleAvailable() ? GLXExt.GLX_SAMPLE_BUFFERS: GLX.GLX_RED_SIZE;
      int samplesAttrib = ((X11GLXDrawableFactory)getFactory()).isMultisampleAvailable() ? GLXExt.GLX_SAMPLES: GLX.GLX_RED_SIZE;
      int floatNV       = getCapabilities().getPbufferFloatingPointBuffers() ? GLXExt.GLX_FLOAT_COMPONENTS_NV : GLX.GLX_RED_SIZE;

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
      setChosenGLCapabilities(X11GLXDrawableFactory.attribList2GLCapabilities(iattribs, iattribs.length, ivalues, true));

      // Determine the actual width and height we were able to create.
      int[] tmp = new int[1];
      GLX.glXQueryDrawable(display, drawable, GLXExt.GLX_WIDTH, tmp, 0);
      int width = tmp[0];
      GLX.glXQueryDrawable(display, drawable, GLXExt.GLX_HEIGHT, tmp, 0);
      int height = tmp[0];
      nw.setSize(width, height);

      if (DEBUG) {
        System.err.println("Created pbuffer " + width + " x " + height);
      }
    } finally {
      getFactory().unlockToolkit();
    }
  }

  public int getFloatingPointMode() {
    // Floating-point pbuffers currently require NVidia hardware on X11
    return GLPbuffer.NV_FLOAT;
  }
  
  public long getFBConfig() {
    return fbConfig;
  }

  private int queryFBConfig(long display, long fbConfig, int attrib) {
    int[] tmp = new int[1];
    if (GLX.glXGetFBConfigAttrib(display, fbConfig, attrib, tmp, 0) != 0) {
      throw new GLException("glXGetFBConfigAttrib failed");
    }
    return tmp[0];
  }

  private void queryFBConfig(long display, long fbConfig, int[] attribs, int nattribs, int[] values) {
    int[] tmp = new int[1];
    for (int i = 0; i < nattribs; i++) {
      if (GLX.glXGetFBConfigAttrib(display, fbConfig, attribs[i], tmp, 0) != 0) {
        throw new GLException("glXGetFBConfigAttrib failed");
      }
      values[i] = tmp[0];
    }
  }
}

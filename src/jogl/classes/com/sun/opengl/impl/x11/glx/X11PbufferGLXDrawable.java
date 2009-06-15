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
import javax.media.nativewindow.*;
import com.sun.opengl.impl.*;
import com.sun.opengl.impl.x11.glx.*;
import com.sun.nativewindow.impl.NullWindow;
import com.sun.nativewindow.impl.x11.*;

import java.nio.LongBuffer;

public class X11PbufferGLXDrawable extends X11GLXDrawable {
  protected X11PbufferGLXDrawable(GLDrawableFactory factory, AbstractGraphicsScreen screen,
                                  GLCapabilities caps, 
                                  GLCapabilitiesChooser chooser,
                                  int width, int height) {
    super(factory, new NullWindow(X11GLXGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(caps, chooser, screen, true)), true);
    if (width <= 0 || height <= 0) {
      throw new GLException("Width and height of pbuffer must be positive (were (" +
			    width + ", " + height + "))");
    }
    NullWindow nw = (NullWindow) getNativeWindow();
    nw.setSize(width, height);

    if (DEBUG) {
      System.out.println("Pbuffer config: " + getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration());
    }

    createPbuffer();
  }

  public GLContext createContext(GLContext shareWith) {
    return new X11PbufferGLXContext(this, shareWith);
  }

  public void destroy() {
    getFactoryImpl().lockToolkit();
    try {
        NullWindow nw = (NullWindow) getNativeWindow();
        if (nw.getSurfaceHandle() != 0) {
          GLX.glXDestroyPbuffer(nw.getDisplayHandle(), nw.getSurfaceHandle());
        }
        nw.invalidate();
    } finally {
        getFactoryImpl().unlockToolkit();
    }
  }

  private void createPbuffer() {
    getFactoryImpl().lockToolkit();
    try {
      X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration) getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration();
      AbstractGraphicsScreen aScreen = config.getScreen();
      AbstractGraphicsDevice aDevice = aScreen.getDevice();
      long display = aDevice.getHandle();
      int screen = aScreen.getIndex();

      if (display==0) {
        throw new GLException("Null display");
      }

      NullWindow nw = (NullWindow) getNativeWindow();
    
      GLCapabilities capabilities = (GLCapabilities)config.getChosenCapabilities();

      if (capabilities.getPbufferRenderToTexture()) {
        throw new GLException("Render-to-texture pbuffers not supported yet on X11");
      }

      if (capabilities.getPbufferRenderToTextureRectangle()) {
        throw new GLException("Render-to-texture-rectangle pbuffers not supported yet on X11");
      }

      // Create the p-buffer.
      int niattribs = 0;
      int[] iattributes = new int[5];

      iattributes[niattribs++] = GLX.GLX_PBUFFER_WIDTH;
      iattributes[niattribs++] = nw.getWidth();
      iattributes[niattribs++] = GLX.GLX_PBUFFER_HEIGHT;
      iattributes[niattribs++] = nw.getHeight();
      iattributes[niattribs++] = 0;

      long drawable = GLX.glXCreatePbuffer(display, config.getFBConfig(), iattributes, 0);
      if (drawable == 0) {
        // FIXME: query X error code for detail error message
        throw new GLException("pbuffer creation error: glXCreatePbuffer() failed");
      }

      // Set up instance variables
      nw.setSurfaceHandle(drawable);
      
      // Determine the actual width and height we were able to create.
      int[] tmp = new int[1];
      GLX.glXQueryDrawable(display, drawable, GLX.GLX_WIDTH, tmp, 0);
      int width = tmp[0];
      GLX.glXQueryDrawable(display, drawable, GLX.GLX_HEIGHT, tmp, 0);
      int height = tmp[0];
      nw.setSize(width, height);

      if (DEBUG) {
        System.err.println("Created pbuffer " + width + " x " + height);
      }
    } finally {
      getFactoryImpl().unlockToolkit();
    }
  }

  public int getFloatingPointMode() {
    // Floating-point pbuffers currently require NVidia hardware on X11
    return GLPbuffer.NV_FLOAT;
  }
}

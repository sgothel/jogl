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

package jogamp.opengl.x11.glx;

import java.nio.IntBuffer;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.MutableSurface;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;

import com.jogamp.common.nio.Buffers;

public class X11PbufferGLXDrawable extends X11GLXDrawable {
  protected X11PbufferGLXDrawable(final GLDrawableFactory factory, final NativeSurface target) {
                                  /* GLCapabilities caps,
                                  GLCapabilitiesChooser chooser,
                                  int width, int height */
    super(factory, target, false);
  }

  @Override
  protected void setRealizedImpl() {
    if(realized) {
        createPbuffer();
    } else {
        destroyPbuffer();
    }
  }

  @Override
  public GLContext createContext(final GLContext shareWith) {
    return new X11GLXContext(this, shareWith);
  }

  protected void destroyPbuffer() {
    final NativeSurface ns = getNativeSurface();
    if (ns.getSurfaceHandle() != 0) {
      GLX.glXDestroyPbuffer(ns.getDisplayHandle(), ns.getSurfaceHandle());
    }
    ((MutableSurface)ns).setSurfaceHandle(0);
    if (DEBUG) {
        System.err.println(getThreadName()+": Destroyed pbuffer " + this);
    }
  }

  private void createPbuffer() {
      final MutableSurface ms = (MutableSurface) getNativeSurface();
      final X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration) ms.getGraphicsConfiguration();
      final AbstractGraphicsScreen aScreen = config.getScreen();
      final AbstractGraphicsDevice aDevice = aScreen.getDevice();
      final long display = aDevice.getHandle();

      if (DEBUG) {
        System.out.println(getThreadName()+": Pbuffer config: " + config);
      }

      if (display==0) {
        throw new GLException("Null display");
      }

      // Create the p-buffer.
      int niattribs = 0;
      final IntBuffer iattributes = Buffers.newDirectIntBuffer(7);

      iattributes.put(niattribs++, GLX.GLX_PBUFFER_WIDTH);
      iattributes.put(niattribs++, ms.getSurfaceWidth());
      iattributes.put(niattribs++, GLX.GLX_PBUFFER_HEIGHT);
      iattributes.put(niattribs++, ms.getSurfaceHeight());
      iattributes.put(niattribs++, GLX.GLX_LARGEST_PBUFFER); // exact
      iattributes.put(niattribs++, 0);
      iattributes.put(niattribs++, 0);

      final long pbuffer = GLX.glXCreatePbuffer(display, config.getFBConfig(), iattributes);
      if (pbuffer == 0) {
        // FIXME: query X error code for detail error message
        throw new GLException("pbuffer creation error: glXCreatePbuffer() failed");
      }

      // Set up instance variables
      ms.setSurfaceHandle(pbuffer);

      if (DEBUG) {
        System.err.println(getThreadName()+": Created pbuffer " + this);
      }
  }
}

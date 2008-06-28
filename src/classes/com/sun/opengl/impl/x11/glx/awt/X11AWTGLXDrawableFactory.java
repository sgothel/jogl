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
 */

package com.sun.opengl.impl.x11.glx.awt;

import com.sun.opengl.impl.*;
import com.sun.opengl.impl.x11.*;
import com.sun.opengl.impl.x11.glx.*;
import com.sun.opengl.impl.jawt.*;
import com.sun.opengl.impl.jawt.x11.*;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.nio.*;
import java.security.*;
import java.util.*;
import javax.media.opengl.*;
import javax.media.opengl.awt.*;
import com.sun.gluegen.runtime.*;

public class X11AWTGLXDrawableFactory extends X11GLXDrawableFactory {

  static {
    // See DRIHack.java for an explanation of why this is necessary
    DRIHack.begin();

    com.sun.opengl.impl.NativeLibLoader.loadGL2();

    DRIHack.end();
  }

  public AbstractGraphicsConfiguration chooseGraphicsConfiguration(GLCapabilities capabilities,
                                                                   GLCapabilitiesChooser chooser,
                                                                   AbstractGraphicsDevice absDevice) {
    if (capabilities == null) {
      capabilities = new GLCapabilities();
    }
    if (chooser == null) {
      chooser = new DefaultGLCapabilitiesChooser();
    }
    GraphicsDevice device = null;
    if (absDevice != null &&
        !(absDevice instanceof AWTGraphicsDevice)) {
      throw new IllegalArgumentException("This GLDrawableFactory accepts only AWTGraphicsDevice objects");
    }

    if ((absDevice == null) ||
        (((AWTGraphicsDevice) absDevice).getGraphicsDevice() == null)) {
      device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    } else {
      device = ((AWTGraphicsDevice) absDevice).getGraphicsDevice();
    }

    int screen;
    if (isXineramaEnabled()) {
      screen = 0;
    } else {
      screen = X11SunJDKReflection.graphicsDeviceGetScreen(device);
    }

    // Until we have a rock-solid visual selection algorithm written
    // in pure Java, we're going to provide the underlying window
    // system's selection to the chooser as a hint

    int[] attribs = glCapabilities2AttribList(capabilities, isMultisampleAvailable(), false, 0, 0);
    XVisualInfo[] infos = null;
    GLCapabilities[] caps = null;
    int recommendedIndex = -1;
    lockToolkit();
    try {
      long display = getDisplayConnection();
      XVisualInfo recommendedVis = GLX.glXChooseVisual(display, screen, attribs, 0);
      if (DEBUG) {
        System.err.print("!!! glXChooseVisual recommended ");
        if (recommendedVis == null) {
          System.err.println("null visual");
        } else {
          System.err.println("visual id 0x" + Long.toHexString(recommendedVis.visualid()));
        }
      }
      int[] count = new int[1];
      XVisualInfo template = XVisualInfo.create();
      template.screen(screen);
      infos = X11Lib.XGetVisualInfo(display, X11Lib.VisualScreenMask, template, count, 0);
      if (infos == null) {
        throw new GLException("Error while enumerating available XVisualInfos");
      }
      caps = new GLCapabilities[infos.length];
      for (int i = 0; i < infos.length; i++) {
        caps[i] = xvi2GLCapabilities(display, infos[i]);
        // Attempt to find the visual chosen by glXChooseVisual
        if (recommendedVis != null && recommendedVis.visualid() == infos[i].visualid()) {
          recommendedIndex = i;
        }
      }
    } finally {
      unlockToolkit();
    }
    // Store these away for later
    for (int i = 0; i < infos.length; i++) {
      if (caps[i] != null) {
        visualToGLCapsMap.put(new ScreenAndVisualIDKey(screen, infos[i].visualid()),
                              caps[i].clone());
      }
    }
    int chosen = chooser.chooseCapabilities(capabilities, caps, recommendedIndex);
    if (chosen < 0 || chosen >= caps.length) {
      throw new GLException("GLCapabilitiesChooser specified invalid index (expected 0.." + (caps.length - 1) + ")");
    }
    XVisualInfo vis = infos[chosen];
    if (vis == null) {
      throw new GLException("GLCapabilitiesChooser chose an invalid visual");
    }
    // FIXME: need to look at glue code and see type of this field
    long visualID = vis.visualid();
    // FIXME: the storage for the infos array, as well as that for the
    // recommended visual, is leaked; should free them here with XFree()

    // Now figure out which GraphicsConfiguration corresponds to this
    // visual by matching the visual ID
    GraphicsConfiguration[] configs = device.getConfigurations();
    for (int i = 0; i < configs.length; i++) {
      GraphicsConfiguration config = configs[i];
      if (config != null) {
        if (X11SunJDKReflection.graphicsConfigurationGetVisualID(config) == visualID) {
          return new AWTGraphicsConfiguration(config);
        }
      }
    }

    // Either we weren't able to reflectively introspect on the
    // X11GraphicsConfig or something went wrong in the steps above;
    // we're going to return null without signaling an error condition
    // in this case (although we should distinguish between the two
    // and possibly report more of an error in the latter case)
    return null;
  }

  public void lockToolkit() {
    super.lockToolkit();
    JAWTUtil.lockToolkit();
  }

  public void unlockToolkit() {
    JAWTUtil.unlockToolkit();
    super.unlockToolkit();
  }

}

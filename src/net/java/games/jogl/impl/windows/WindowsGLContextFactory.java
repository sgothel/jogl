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

package net.java.games.jogl.impl.windows;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class WindowsGLContextFactory extends GLContextFactory {
  // On Windows we want to be able to use some extension routines like
  // wglChoosePixelFormatARB during the creation of the user's first
  // GLContext. However, this and other routines' function pointers
  // aren't loaded by the driver until the first OpenGL context is
  // created. The standard way of working around this chicken-and-egg
  // problem is to create a dummy window, show it, send it a paint
  // message, create an OpenGL context, fetch the needed function
  // pointers, and then destroy the dummy window and context. In JOGL
  // since we closely associate the contexts with components we leave
  // the dummy window around as it should not have a large footprint
  // impact.
  private static Map/*<GraphicsDevice, GL>*/ dummyContextMap   = new HashMap();
  private static Set/*<GraphicsDevice    >*/ pendingContextSet = new HashSet();
  
  public GraphicsConfiguration chooseGraphicsConfiguration(GLCapabilities capabilities,
                                                           GLCapabilitiesChooser chooser,
                                                           GraphicsDevice device) {
    return null;
  }

  public GLContext createGLContext(Component component,
                                   GLCapabilities capabilities,
                                   GLCapabilitiesChooser chooser,
                                   GLContext shareWith) {
    if (component != null) {
      return new WindowsOnscreenGLContext(component, capabilities, chooser, shareWith);
    } else {
      return new WindowsOffscreenGLContext(capabilities, chooser, shareWith);
    }
  }

  public static GL getDummyGLContext(final GraphicsDevice device) {
    GL gl = (GL) dummyContextMap.get(device);
    if (gl != null) {
      return gl;
    }
    
    if (!pendingContextSet.contains(device)) {
      pendingContextSet.add(device);
      GraphicsConfiguration config = device.getDefaultConfiguration();
      final Frame frame = new Frame(config);
      frame.setUndecorated(true);
      GLCanvas canvas = GLDrawableFactory.getFactory().createGLCanvas(new GLCapabilities(),
                                                                      null,
                                                                      null,
                                                                      device);
      canvas.addGLEventListener(new GLEventListener() {
          public void init(GLDrawable drawable) {
            pendingContextSet.remove(device);
            dummyContextMap.put(device, drawable.getGL());
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                  frame.dispose();
                }
              });
          }

          public void display(GLDrawable drawable) {
          }

          public void reshape(GLDrawable drawable, int x, int y, int width, int height) {
          }

          public void displayChanged(GLDrawable drawable, boolean modeChanged, boolean deviceChanged) {
          }
        });
      canvas.setSize(0, 0);
      frame.add(canvas);
      frame.pack();
      frame.show();
      canvas.display();
    }

    return (GL) dummyContextMap.get(device);
  }
}

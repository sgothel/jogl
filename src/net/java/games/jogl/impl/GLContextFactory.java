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

package net.java.games.jogl.impl;

import java.awt.Component;
import net.java.games.jogl.*;

public abstract class GLContextFactory {
  private static GLContextFactory factory;

  public static GLContextFactory getFactory() {
    if (factory == null) {
      try {
        String osName = System.getProperty("os.name");
        String osNameLowerCase = osName.toLowerCase();
        Class factoryClass = null;

        // Because there are some complications with generating all
        // platforms' Java glue code on all platforms (among them that we
        // would have to include jawt.h and jawt_md.h in the jogl
        // sources, which we currently don't have to do) we break the only
        // static dependencies with platform-specific code here using reflection.

        if (osNameLowerCase.startsWith("wind")) {
          factoryClass = Class.forName("net.java.games.jogl.impl.windows.WindowsGLContextFactory");
        } else if (osNameLowerCase.startsWith("mac os x")) {
          factoryClass = Class.forName("net.java.games.jogl.impl.macosx.MacOSXGLContextFactory");
        } else {
          // Assume Linux, Solaris, etc. Should probably test for these explicitly.
          factoryClass = Class.forName("net.java.games.jogl.impl.x11.X11GLContextFactory");
        }

        if (factoryClass == null) {
          throw new GLException("OS " + osName + " not yet supported");
        }

        factory = (GLContextFactory) factoryClass.newInstance();
      } catch (ClassNotFoundException e) {
        throw new GLException(e);
      } catch (InstantiationException e) {
        throw new GLException(e);
      } catch (IllegalAccessException e) {
        throw new GLException(e);
      }
    }

    return factory;
  }

  public abstract GLContext createGLContext(Component component,
                                            GLCapabilities capabilities,
                                            GLCapabilitiesChooser chooser,
                                            GLContext shareWith);
}

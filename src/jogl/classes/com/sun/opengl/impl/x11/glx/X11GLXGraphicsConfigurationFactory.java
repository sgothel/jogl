/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.sun.opengl.impl.x11.glx;

import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;
import com.sun.nativewindow.impl.NativeWindowFactoryImpl;
import com.sun.nativewindow.impl.x11.*;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.opengl.impl.x11.glx.*;

/** Subclass of GraphicsConfigurationFactory used when non-AWT tookits
    are used on X11 platforms. Toolkits will likely need to delegate
    to this one to change the accepted and returned types of the
    GraphicsDevice and GraphicsConfiguration abstractions. */

public class X11GLXGraphicsConfigurationFactory extends GraphicsConfigurationFactory {
    protected static final boolean DEBUG = Debug.debug("GraphicsConfiguration");

    public X11GLXGraphicsConfigurationFactory() {
        GraphicsConfigurationFactory.registerFactory(javax.media.nativewindow.x11.X11GraphicsDevice.class, this);
    }

    public AbstractGraphicsConfiguration chooseGraphicsConfiguration(Capabilities capabilities,
                                                                     CapabilitiesChooser chooser,
                                                                     AbstractGraphicsScreen absScreen) {
        return chooseGraphicsConfigurationStatic(capabilities, chooser, absScreen, false);
    }

    protected static X11GLXGraphicsConfiguration createDefaultGraphicsConfiguration(AbstractGraphicsScreen absScreen, boolean usePBuffer) {
      if (absScreen == null) {
        throw new IllegalArgumentException("AbstractGraphicsScreen is null");
      }
      if (!(absScreen instanceof X11GraphicsScreen)) {
        throw new IllegalArgumentException("Only X11GraphicsScreen are allowed here");
      }
      X11GraphicsScreen x11Screen = (X11GraphicsScreen)absScreen;

      GLProfile glProfile = GLProfile.getDefault();
      GLCapabilities caps=null;
      XVisualInfo xvis=null;
      long fbcfg = 0;
      int fbid = -1;

      // Utilizing FBConfig
      //
      GLCapabilities capsFB = null;
      NativeWindowFactory.getDefaultFactory().getToolkitLock().lock();
      try {
          long display = x11Screen.getDevice().getHandle();
          int screen = x11Screen.getIndex();
          boolean isMultisampleAvailable = GLXUtil.isMultisampleAvailable(display);

          long visID = X11Lib.DefaultVisualID(display, x11Screen.getIndex());
          xvis = X11GLXGraphicsConfiguration.XVisualID2XVisualInfo(display, visID);
          caps = X11GLXGraphicsConfiguration.XVisualInfo2GLCapabilities(glProfile, display, xvis, isMultisampleAvailable);

          int[] attribs = X11GLXGraphicsConfiguration.GLCapabilities2AttribList(caps, true, isMultisampleAvailable, usePBuffer, 0, 0);
          int[] count = { -1 };
          java.nio.LongBuffer fbcfgsL = GLX.glXChooseFBConfigCopied(display, screen, attribs, 0, count, 0);
          if (fbcfgsL == null || fbcfgsL.limit()<1) {
              throw new Exception("Could not fetch FBConfig for "+caps);
          }
          fbcfg = fbcfgsL.get(0);
          capsFB = X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities(glProfile, display, fbcfg, isMultisampleAvailable);

          int[] tmpID = new int[1];
          fbid = X11GLXGraphicsConfiguration.glXGetFBConfig(display, fbcfg, GLX.GLX_FBCONFIG_ID, tmpID, 0);

          xvis = GLX.glXGetVisualFromFBConfigCopied(display, fbcfg);
          if (xvis==null) {
            throw new GLException("Error: Choosen FBConfig has no visual");
          }
      } catch (Throwable t) {
      } finally {
          NativeWindowFactory.getDefaultFactory().getToolkitLock().unlock();
      }

        return new X11GLXGraphicsConfiguration(x11Screen, (null!=capsFB)?capsFB:caps, caps, null, xvis, fbcfg, fbid);
    }

    protected static X11GLXGraphicsConfiguration chooseGraphicsConfigurationStatic(Capabilities capabilities,
                                                                                   CapabilitiesChooser chooser,
                                                                                   AbstractGraphicsScreen absScreen, 
                                                                                   boolean usePBuffer) {
        if (absScreen == null) {
            throw new IllegalArgumentException("AbstractGraphicsScreen is null");
        }
        if (!(absScreen instanceof X11GraphicsScreen)) {
            throw new IllegalArgumentException("Only X11GraphicsScreen are allowed here");
        }
        X11GraphicsScreen x11Screen = (X11GraphicsScreen)absScreen;


        if (capabilities != null &&
            !(capabilities instanceof GLCapabilities)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects");
        }

        if (chooser != null &&
            !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilitiesChooser objects");
        }

        if (capabilities == null) {
            capabilities = new GLCapabilities(null);
        }

    
        X11GLXGraphicsConfiguration res;
        res = chooseGraphicsConfigurationFBConfig((GLCapabilities) capabilities,
                                                  (GLCapabilitiesChooser) chooser,
                                                  x11Screen, usePBuffer);
        if(null==res) {
            if(usePBuffer) {
                throw new GLException("Error: Couldn't create X11GLXGraphicsConfiguration based on FBConfig");
            }
            res = chooseGraphicsConfigurationXVisual((GLCapabilities) capabilities,
                                                     (GLCapabilitiesChooser) chooser,
                                                     x11Screen);
        }
        if(null==res) {
            throw new GLException("Error: Couldn't create X11GLXGraphicsConfiguration");
        }
        if(DEBUG) {
            System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationStatic("+x11Screen+","+capabilities+", pbuffer "+usePBuffer+"): "+res);
        }
        return res;
    }

    protected static X11GLXGraphicsConfiguration chooseGraphicsConfigurationFBConfig(GLCapabilities capabilities,
                                                                                     GLCapabilitiesChooser chooser,
                                                                                     X11GraphicsScreen x11Screen,
                                                                                     boolean usePBuffer) {
        int recommendedIndex = -1;
        GLCapabilities[] caps = null;
        java.nio.LongBuffer fbcfgsL = null;
        int chosen=-1;
        int retFBID=-1;
        XVisualInfo retXVisualInfo = null;
        GLProfile glProfile = capabilities.getGLProfile();

        // Utilizing FBConfig
        //
        NativeWindowFactory.getDefaultFactory().getToolkitLock().lock();
        try {
            int screen = x11Screen.getIndex();
            AbstractGraphicsDevice absDevice = x11Screen.getDevice();
            long display = absDevice.getHandle();
            boolean isMultisampleAvailable = GLXUtil.isMultisampleAvailable(display);
            int[] attribs = X11GLXGraphicsConfiguration.GLCapabilities2AttribList(capabilities, true, isMultisampleAvailable, usePBuffer, 0, 0);
            int[] count = { -1 };

            fbcfgsL = GLX.glXChooseFBConfigCopied(display, screen, attribs, 0, count, 0);
            if (fbcfgsL == null || fbcfgsL.limit()<1) {
                if(DEBUG) {
                    System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: glXChooseFBConfig ("+x11Screen+","+capabilities+"): "+fbcfgsL+", "+count[0]);
                }
                return null;
            }
            recommendedIndex = 0; // 1st match is always recommended ..
            caps = new GLCapabilities[fbcfgsL.limit()];
            for (int i = 0; i < fbcfgsL.limit(); i++) {
                caps[i] = X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities(glProfile, display, fbcfgsL.get(i), isMultisampleAvailable);
            }

            if(null==chooser) {
                chosen = recommendedIndex;
            } else {
                try {
                  chosen = chooser.chooseCapabilities(capabilities, caps, recommendedIndex);
                } catch (NativeWindowException e) {
                  throw new GLException(e);
                }
            }

            if (chosen < 0 || chosen >= caps.length) {
                throw new GLException("GLCapabilitiesChooser specified invalid index (expected 0.." + (caps.length - 1) + ")");
            }

            int[] tmpID = new int[1];
            retFBID = X11GLXGraphicsConfiguration.glXGetFBConfig(display, fbcfgsL.get(chosen), GLX.GLX_FBCONFIG_ID, tmpID, 0);

            retXVisualInfo = GLX.glXGetVisualFromFBConfigCopied(display, fbcfgsL.get(chosen));
            if (retXVisualInfo==null) {
                if(DEBUG) {
                    System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: glXGetVisualFromFBConfig ("+x11Screen+", "+fbcfgsL.get(chosen) +": "+fbcfgsL);
                }
                return null;
            }
        } finally {
            NativeWindowFactory.getDefaultFactory().getToolkitLock().unlock();
        }

        return new X11GLXGraphicsConfiguration(x11Screen, caps[chosen], capabilities, chooser, retXVisualInfo, fbcfgsL.get(chosen), retFBID);
    }

    protected static X11GLXGraphicsConfiguration chooseGraphicsConfigurationXVisual(GLCapabilities capabilities,
                                                                                    GLCapabilitiesChooser chooser,
                                                                                    X11GraphicsScreen x11Screen) {
        if (chooser == null) {
            chooser = new DefaultGLCapabilitiesChooser();
        }

        // Until we have a rock-solid visual selection algorithm written
        // in pure Java, we're going to provide the underlying window
        // system's selection to the chooser as a hint

        GLProfile glProfile = capabilities.getGLProfile();
        GLCapabilities[] caps = null;
        int recommendedIndex = -1;
        XVisualInfo retXVisualInfo = null;
        int chosen;

        NativeWindowFactory.getDefaultFactory().getToolkitLock().lock();
        try {
            int screen = x11Screen.getIndex();
            AbstractGraphicsDevice absDevice = x11Screen.getDevice();
            long display = absDevice.getHandle();
            boolean isMultisampleAvailable = GLXUtil.isMultisampleAvailable(display);
            int[] attribs = X11GLXGraphicsConfiguration.GLCapabilities2AttribList(capabilities, false, isMultisampleAvailable, false, 0, 0);
            XVisualInfo[] infos = null;

            XVisualInfo recommendedVis = GLX.glXChooseVisualCopied(display, screen, attribs, 0);
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
            infos = X11Lib.XGetVisualInfoCopied(display, X11Lib.VisualScreenMask, template, count, 0);
            if (infos == null || infos.length<1) {
                throw new GLException("Error while enumerating available XVisualInfos");
            }
            caps = new GLCapabilities[infos.length];
            for (int i = 0; i < infos.length; i++) {
                caps[i] = X11GLXGraphicsConfiguration.XVisualInfo2GLCapabilities(glProfile, display, infos[i], isMultisampleAvailable);
                // Attempt to find the visual chosen by glXChooseVisual
                if (recommendedVis != null && recommendedVis.visualid() == infos[i].visualid()) {
                    recommendedIndex = i;
                }
            }
            try {
              chosen = chooser.chooseCapabilities(capabilities, caps, recommendedIndex);
            } catch (NativeWindowException e) {
              throw new GLException(e);
            }
            if (chosen < 0 || chosen >= caps.length) {
                throw new GLException("GLCapabilitiesChooser specified invalid index (expected 0.." + (caps.length - 1) + ")");
            }
            if (infos[chosen] == null) {
                throw new GLException("GLCapabilitiesChooser chose an invalid visual");
            }
            retXVisualInfo = XVisualInfo.create(infos[chosen]);
        } finally {
            NativeWindowFactory.getDefaultFactory().getToolkitLock().unlock();
        }
        return new X11GLXGraphicsConfiguration(x11Screen, caps[chosen], capabilities, chooser, retXVisualInfo, 0, -1);
    }
}


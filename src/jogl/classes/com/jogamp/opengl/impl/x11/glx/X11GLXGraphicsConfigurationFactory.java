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

package com.jogamp.opengl.impl.x11.glx;

import com.jogamp.common.nio.PointerBuffer;
import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;
import com.jogamp.nativewindow.impl.x11.*;

import javax.media.opengl.*;
import com.jogamp.opengl.impl.*;


/** Subclass of GraphicsConfigurationFactory used when non-AWT toolkits
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
        if (!(absScreen instanceof X11GraphicsScreen)) {
            throw new IllegalArgumentException("Only X11GraphicsScreen are allowed here");
        }

        if (capabilities != null && !(capabilities instanceof GLCapabilities)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects");
        }

        if (chooser != null && !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilitiesChooser objects");
        }
        return chooseGraphicsConfigurationStatic((GLCapabilities)capabilities, (GLCapabilitiesChooser)chooser, (X11GraphicsScreen)absScreen);
    }

    /**
    protected static X11GLXGraphicsConfiguration createDefaultGraphicsConfigurationFBConfig(AbstractGraphicsScreen absScreen, boolean onscreen, boolean usePBuffer) {
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
      long display = x11Screen.getDevice().getHandle();

      try {
          int screen = x11Screen.getIndex();
          boolean isMultisampleAvailable = GLXUtil.isMultisampleAvailable(display);

          long visID = X11Lib.DefaultVisualID(display, x11Screen.getIndex());
          xvis = X11GLXGraphicsConfiguration.XVisualID2XVisualInfo(display, visID);
          caps = X11GLXGraphicsConfiguration.XVisualInfo2GLCapabilities(glProfile, display, xvis, onscreen, usePBuffer, isMultisampleAvailable);

          int[] attribs = X11GLXGraphicsConfiguration.GLCapabilities2AttribList(caps, true, isMultisampleAvailable, display, screen);
          int[] count = { -1 };
          PointerBuffer fbcfgsL = GLX.glXChooseFBConfig(display, screen, attribs, 0, count, 0);
          if (fbcfgsL == null || fbcfgsL.limit()<1) {
              throw new Exception("Could not fetch FBConfig for "+caps);
          }
          fbcfg = fbcfgsL.get(0);
          capsFB = X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities(glProfile, display, fbcfg, true, onscreen, usePBuffer, isMultisampleAvailable);

          fbid = X11GLXGraphicsConfiguration.glXFBConfig2FBConfigID(display, fbcfg);

          xvis = GLX.glXGetVisualFromFBConfig(display, fbcfg);
          if (xvis==null) {
            throw new GLException("Error: Choosen FBConfig has no visual");
          }
      } catch (Throwable t) {
      }

      return new X11GLXGraphicsConfiguration(x11Screen, (null!=capsFB)?capsFB:caps, caps, null, xvis, fbcfg, fbid);
    } */

    protected static X11GLXGraphicsConfiguration chooseGraphicsConfigurationStatic(GLCapabilities capabilities,
                                                                                  GLCapabilitiesChooser chooser,
                                                                                  X11GraphicsScreen x11Screen) {
        if (x11Screen == null) {
            throw new IllegalArgumentException("AbstractGraphicsScreen is null");
        }

        if (capabilities == null) {
            capabilities = new GLCapabilities(null);
        }

        GLCapabilities caps2 = (GLCapabilities) capabilities.clone();

        boolean usePBuffer = caps2.isPBuffer();

        if(!caps2.isOnscreen()) {
            // OFFSCREEN !DOUBLE_BUFFER // FIXME DBLBUFOFFSCRN
            caps2.setDoubleBuffered(false);
        }
    
        X11GLXGraphicsConfiguration res = chooseGraphicsConfigurationFBConfig(caps2, chooser, x11Screen);
        if(null==res) {
            if(usePBuffer) {
                throw new GLException("Error: Couldn't create X11GLXGraphicsConfiguration based on FBConfig for "+caps2);
            }
            res = chooseGraphicsConfigurationXVisual(caps2, chooser, x11Screen);
        }
        if(null==res) {
            throw new GLException("Error: Couldn't create X11GLXGraphicsConfiguration based on FBConfig and XVisual for "+caps2);
        }
        if(DEBUG) {
            System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationStatic("+x11Screen+","+caps2+"): "+res);
        }
        return res;
    }

    protected static X11GLXGraphicsConfiguration chooseGraphicsConfigurationFBConfig(GLCapabilities capabilities,
                                                                                     GLCapabilitiesChooser chooser,
                                                                                     X11GraphicsScreen x11Screen) {
        long recommendedFBConfig = 0;
        int recommendedIndex = -1;
        GLCapabilities[] caps = null;
        PointerBuffer fbcfgsL = null;
        int chosen=-1;
        int retFBID=-1;
        XVisualInfo retXVisualInfo = null;
        GLProfile glProfile = capabilities.getGLProfile();
        boolean onscreen = capabilities.isOnscreen();
        boolean usePBuffer = capabilities.isPBuffer();

        // Utilizing FBConfig
        //
        AbstractGraphicsDevice absDevice = x11Screen.getDevice();
        long display = absDevice.getHandle();

        int screen = x11Screen.getIndex();
        boolean isMultisampleAvailable = GLXUtil.isMultisampleAvailable(display);
        int[] attribs = X11GLXGraphicsConfiguration.GLCapabilities2AttribList(capabilities, true, isMultisampleAvailable, display, screen);
        int[] count = { -1 };

        // determine the recommended FBConfig ..
        fbcfgsL = GLX.glXChooseFBConfig(display, screen, attribs, 0, count, 0);
        if (fbcfgsL == null || fbcfgsL.limit()<1) {
            if(DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: Failed glXChooseFBConfig ("+x11Screen+","+capabilities+"): "+fbcfgsL+", "+count[0]);
            }
        } else if( !X11GLXGraphicsConfiguration.GLXFBConfigValid( display, fbcfgsL.get(0) ) ) {
            if(DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: Failed - GLX FBConfig invalid: ("+x11Screen+","+capabilities+"): "+fbcfgsL+", fbcfg: "+toHexString(fbcfgsL.get(0)));
            }
        } else {
            recommendedFBConfig = fbcfgsL.get(0);
        }

        // get all, glXChooseFBConfig(.. attribs==null ..) == glXGetFBConfig(..)
        fbcfgsL = GLX.glXChooseFBConfig(display, screen, null, 0, count, 0);
        if (fbcfgsL == null || fbcfgsL.limit()<1) {
            if(DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: Failed glXGetFBConfig ("+x11Screen+"): "+fbcfgsL+", "+count[0]);
            }
            return null;
        }

        // make GLCapabilities and seek the recommendedIndex
        caps = new GLCapabilities[fbcfgsL.limit()];
        for (int i = 0; i < fbcfgsL.limit(); i++) {
            if( !X11GLXGraphicsConfiguration.GLXFBConfigValid( display, fbcfgsL.get(i) ) ) {
                if(DEBUG) {
                    System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: FBConfig invalid: ("+x11Screen+","+capabilities+"): fbcfg: "+toHexString(fbcfgsL.get(i)));
                }
            } else {
                caps[i] = X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities(glProfile, display, fbcfgsL.get(i),
                                                                                 false, onscreen, usePBuffer, isMultisampleAvailable);
                if(caps[i]!=null && recommendedFBConfig==fbcfgsL.get(i)) {
                    recommendedIndex=i;
                    if (DEBUG) {
                        System.err.println("!!! glXChooseFBConfig recommended "+i+", "+caps[i]);
                    }
                }
            }
        }

        if(null==chooser) {
            chosen = recommendedIndex; // may still be -1 in case nothing was recommended (-1)
        }

        if (chosen < 0) {
            if(null==chooser) {
                // nothing recommended .. so use our default implementation
                chooser = new DefaultGLCapabilitiesChooser();
            }
            try {
              chosen = chooser.chooseCapabilities(capabilities, caps, recommendedIndex);
            } catch (NativeWindowException e) {
              if(DEBUG) {
                  e.printStackTrace();
              }
              chosen = -1;
            }
        }
        if (chosen < 0) {
          // keep on going ..
          if(DEBUG) {
              System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig Failed .. unable to choose config, using first");
          }
          // seek first available one ..
          for(chosen = 0; chosen < caps.length && caps[chosen]==null; chosen++) ;
          if(chosen==caps.length) {
            // give up ..
            if(DEBUG) {
              System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig Failed .. nothing available, bail out");
            }
            return null;
          }
        } else if (chosen >= caps.length) {
            if(DEBUG) {
              System.err.println("GLCapabilitiesChooser specified invalid index (expected 0.." + (caps.length - 1) + ", got "+chosen+")");
            }
            return null;
        }

        retFBID = X11GLXGraphicsConfiguration.glXFBConfig2FBConfigID(display, fbcfgsL.get(chosen));

        retXVisualInfo = GLX.glXGetVisualFromFBConfig(display, fbcfgsL.get(chosen));
        if (retXVisualInfo==null) {
            if(DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: Failed glXGetVisualFromFBConfig ("+x11Screen+", "+fbcfgsL.get(chosen) +" (Continue: "+(false==caps[chosen].isOnscreen())+"):\n\t"+caps[chosen]);
            }
            if(caps[chosen].isOnscreen()) {
                // Onscreen drawables shall have a XVisual ..
                return null;
            }
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
        boolean onscreen = capabilities.isOnscreen();
        GLCapabilities[] caps = null;
        int recommendedIndex = -1;
        XVisualInfo retXVisualInfo = null;
        int chosen=-1;

        AbstractGraphicsDevice absDevice = x11Screen.getDevice();
        long display = absDevice.getHandle();

        int screen = x11Screen.getIndex();
        boolean isMultisampleAvailable = GLXUtil.isMultisampleAvailable(display);
        int[] attribs = X11GLXGraphicsConfiguration.GLCapabilities2AttribList(capabilities, false, isMultisampleAvailable, display, screen);
        XVisualInfo[] infos = null;

        XVisualInfo recommendedVis = GLX.glXChooseVisual(display, screen, attribs, 0);
        if (DEBUG) {
            System.err.print("!!! glXChooseVisual recommended ");
            if (recommendedVis == null) {
                System.err.println("null visual");
            } else {
                System.err.println("visual id " + toHexString(recommendedVis.getVisualid()));
            }
        }
        int[] count = new int[1];
        XVisualInfo template = XVisualInfo.create();
        template.setScreen(screen);
        infos = X11Lib.XGetVisualInfo(display, X11Lib.VisualScreenMask, template, count, 0);
        if (infos == null || infos.length<1) {
            throw new GLException("Error while enumerating available XVisualInfos");
        }
        caps = new GLCapabilities[infos.length];
        for (int i = 0; i < infos.length; i++) {
            caps[i] = X11GLXGraphicsConfiguration.XVisualInfo2GLCapabilities(glProfile, display, infos[i], onscreen, false, isMultisampleAvailable);
            // Attempt to find the visual chosen by glXChooseVisual
            if (recommendedVis != null && recommendedVis.getVisualid() == infos[i].getVisualid()) {
                recommendedIndex = i;
            }
        }
        try {
          chosen = chooser.chooseCapabilities(capabilities, caps, recommendedIndex);
        } catch (NativeWindowException e) {
          if(DEBUG) {
              e.printStackTrace();
          }
          chosen = -1;
        }
        if (chosen < 0) {
          // keep on going ..
          if(DEBUG) {
              System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationXVisual Failed .. unable to choose config, using first");
          }
          chosen = 0; // default ..
        } else if (chosen >= caps.length) {
            throw new GLException("GLCapabilitiesChooser specified invalid index (expected 0.." + (caps.length - 1) + ")");
        }
        if (infos[chosen] == null) {
            throw new GLException("GLCapabilitiesChooser chose an invalid visual for "+caps[chosen]);
        }
        retXVisualInfo = XVisualInfo.create(infos[chosen]);
        return new X11GLXGraphicsConfiguration(x11Screen, caps[chosen], capabilities, chooser, retXVisualInfo, 0, -1);
    }

    public static String toHexString(int val) {
        return "0x"+Integer.toHexString(val);
    }

    public static String toHexString(long val) {
        return "0x"+Long.toHexString(val);
    }

}


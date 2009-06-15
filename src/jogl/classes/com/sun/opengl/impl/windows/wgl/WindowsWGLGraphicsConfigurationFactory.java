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

package com.sun.opengl.impl.windows.wgl;

import javax.media.nativewindow.*;
import javax.media.nativewindow.windows.*;
import com.sun.nativewindow.impl.*;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;

/** Subclass of GraphicsConfigurationFactory used when non-AWT tookits
    are used on Windows platforms. Toolkits will likely need to delegate
    to this one to change the accepted and returned types of the
    GraphicsDevice and GraphicsConfiguration abstractions. */

public class WindowsWGLGraphicsConfigurationFactory extends GraphicsConfigurationFactory {
    protected static final boolean DEBUG = com.sun.opengl.impl.Debug.debug("GraphicsConfiguration");

    public WindowsWGLGraphicsConfigurationFactory() {
        GraphicsConfigurationFactory.registerFactory(javax.media.nativewindow.windows.WindowsGraphicsDevice.class, this);
    }

    public AbstractGraphicsConfiguration chooseGraphicsConfiguration(Capabilities capabilities,
                                                                     CapabilitiesChooser chooser,
                                                                     AbstractGraphicsScreen absScreen) {
        return chooseGraphicsConfigurationStatic((GLCapabilities)capabilities, chooser, absScreen, false);
    }

    protected static WindowsWGLGraphicsConfiguration createDefaultGraphicsConfiguration(AbstractGraphicsScreen absScreen, boolean useOffScreen) {
        GLCapabilities caps = new GLCapabilities(null);
        if(null==absScreen) {
            absScreen = DefaultGraphicsScreen.createScreenDevice(0);
        }
        return new WindowsWGLGraphicsConfiguration(absScreen, caps, caps, WindowsWGLGraphicsConfiguration.GLCapabilities2PFD(caps, useOffScreen), -1, null);
    }

    protected static WindowsWGLGraphicsConfiguration chooseGraphicsConfigurationStatic(GLCapabilities caps,
                                                                                       CapabilitiesChooser chooser,
                                                                                       AbstractGraphicsScreen absScreen, boolean useOffScreen) {
        if(null==absScreen) {
            absScreen = DefaultGraphicsScreen.createScreenDevice(0);
        }
        return new WindowsWGLGraphicsConfiguration(absScreen, caps, caps, WindowsWGLGraphicsConfiguration.GLCapabilities2PFD(caps, useOffScreen), -1, 
                                                   (GLCapabilitiesChooser)chooser);
    }

    protected static void updateGraphicsConfiguration(CapabilitiesChooser chooser,
                                                      GLDrawableFactory factory, NativeWindow nativeWindow, boolean useOffScreen) {
        if (nativeWindow == null) {
            throw new IllegalArgumentException("NativeWindow is null");
        }

        if (chooser != null &&
            !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilitiesChooser objects");
        }

        WindowsWGLGraphicsConfiguration config = (WindowsWGLGraphicsConfiguration) nativeWindow.getGraphicsConfiguration().getNativeGraphicsConfiguration();
        GLCapabilities capabilities = (GLCapabilities) config.getRequestedCapabilities();
        GLProfile glProfile = capabilities.getGLProfile();
        long hdc = nativeWindow.getSurfaceHandle();

        if (DEBUG) {
          Exception ex = new Exception("WindowsWGLGraphicsConfigurationFactory got HDC 0x"+Long.toHexString(hdc));
          ex.printStackTrace();
          System.err.println("WindowsWGLGraphicsConfigurationFactory got NW    "+nativeWindow);
        }

        PIXELFORMATDESCRIPTOR pfd = null;
        int pixelFormat = 0;
        GLCapabilities chosenCaps = null;

        if (!useOffScreen) {
          if ((pixelFormat = WGL.GetPixelFormat(hdc)) != 0) {
            // Pixelformat already set by either 
            //  - a previous updateGraphicsConfiguration() call on the same HDC,
            //  - the graphics driver, copying the HDC's pixelformat to the new one,
            //  - or the Java2D/OpenGL pipeline's configuration
            if (DEBUG) {
              System.err.println("!!!! NOTE: pixel format already chosen for HDC: 0x" + Long.toHexString(hdc)+
                                 ", pixelformat "+WGL.GetPixelFormat(hdc));
            }
            pfd = WindowsWGLGraphicsConfiguration.createPixelFormatDescriptor();
            if (WGL.DescribePixelFormat(hdc, pixelFormat, pfd.size(), pfd) == 0) {
              // FIXME: should this just be a warning? Not really critical...
              throw new GLException("Unable to describe pixel format " + pixelFormat +
                                    " of window set by Java2D/OpenGL pipeline");
            }
            config.setCapsPFD(WindowsWGLGraphicsConfiguration.PFD2GLCapabilities(glProfile, pfd), pfd, pixelFormat);
            return;
          }

          GLCapabilities[] availableCaps = null;
          int numFormats = 0;
          pfd = WindowsWGLGraphicsConfiguration.createPixelFormatDescriptor();
          // Produce a recommended pixel format selection for the GLCapabilitiesChooser.
          // Use wglChoosePixelFormatARB if user requested multisampling and if we have it available
          WindowsWGLDrawable dummyDrawable = null;
          GLContextImpl     dummyContext  = null;
          WGLExt            dummyWGLExt   = null;
          if (capabilities.getSampleBuffers()) {
            dummyDrawable = new WindowsDummyWGLDrawable(factory);
            dummyContext  = (GLContextImpl) dummyDrawable.createContext(null);
            if (dummyContext != null) {
              dummyContext.makeCurrent();
              dummyWGLExt = (WGLExt) dummyContext.getPlatformGLExtensions();
            }
          }      
          int recommendedPixelFormat = -1;
          boolean haveWGLChoosePixelFormatARB = false;
          boolean haveWGLARBMultisample = false;
          boolean gotAvailableCaps = false;
          if (dummyWGLExt != null) {
            try {
              haveWGLChoosePixelFormatARB = dummyWGLExt.isExtensionAvailable("WGL_ARB_pixel_format");
              if (haveWGLChoosePixelFormatARB) {
                haveWGLARBMultisample = dummyWGLExt.isExtensionAvailable("WGL_ARB_multisample");

                int[]   iattributes = new int  [2*WindowsWGLGraphicsConfiguration.MAX_ATTRIBS];
                int[]   iresults    = new int  [2*WindowsWGLGraphicsConfiguration.MAX_ATTRIBS];
                float[] fattributes = new float[1];

                if(WindowsWGLGraphicsConfiguration.GLCapabilities2AttribList(capabilities,
                                                                             iattributes,
                                                                             dummyWGLExt,
                                                                             false,
                                                                             null)) {
                  int[] pformats = new int[WindowsWGLGraphicsConfiguration.MAX_PFORMATS];
                  int[] numFormatsTmp = new int[1];
                  if (dummyWGLExt.wglChoosePixelFormatARB(hdc,
                                                       iattributes, 0,
                                                       fattributes, 0,
                                                       WindowsWGLGraphicsConfiguration.MAX_PFORMATS,
                                                       pformats, 0,
                                                       numFormatsTmp, 0)) {
                    numFormats = numFormatsTmp[0];
                    if (numFormats > 0) {
                      // Remove one-basing of pixel format (added on later)
                      recommendedPixelFormat = pformats[0] - 1;
                      if (DEBUG) {
                        System.err.println(getThreadName() + ": Used wglChoosePixelFormatARB to recommend pixel format " + recommendedPixelFormat);
                      }
                    }
                  } else {
                    if (DEBUG) {
                      System.err.println(getThreadName() + ": wglChoosePixelFormatARB failed: " + WGL.GetLastError() );
                      Thread.dumpStack();
                    }
                  }
                  if (DEBUG) {
                    if (recommendedPixelFormat < 0) {
                      System.err.print(getThreadName() + ": wglChoosePixelFormatARB didn't recommend a pixel format");
                      if (capabilities.getSampleBuffers()) {
                        System.err.print(" for multisampled GLCapabilities");
                      }
                      System.err.println();
                    }
                  }

                  // Produce a list of GLCapabilities to give to the
                  // GLCapabilitiesChooser.
                  // Use wglGetPixelFormatAttribivARB instead of
                  // DescribePixelFormat to get higher-precision information
                  // about the pixel format (should make the GLCapabilities
                  // more precise as well...i.e., remove the
                  // "HardwareAccelerated" bit, which is basically
                  // meaningless, and put in whether it can render to a
                  // window, to a pbuffer, or to a pixmap)
                  int niattribs = 0;
                  iattributes[0] = WGLExt.WGL_NUMBER_PIXEL_FORMATS_ARB;
                  if (dummyWGLExt.wglGetPixelFormatAttribivARB(hdc, 0, 0, 1, iattributes, 0, iresults, 0)) {
                    numFormats = iresults[0];

                    if (DEBUG) {
                      System.err.println("wglGetPixelFormatAttribivARB reported WGL_NUMBER_PIXEL_FORMATS = " + numFormats);
                    }

                    // Should we be filtering out the pixel formats which aren't
                    // applicable, as we are doing here?
                    // We don't have enough information in the GLCapabilities to
                    // represent those that aren't...
                    iattributes[niattribs++] = WGLExt.WGL_DRAW_TO_WINDOW_ARB;
                    iattributes[niattribs++] = WGLExt.WGL_ACCELERATION_ARB;
                    iattributes[niattribs++] = WGLExt.WGL_SUPPORT_OPENGL_ARB;
                    iattributes[niattribs++] = WGLExt.WGL_DEPTH_BITS_ARB;
                    iattributes[niattribs++] = WGLExt.WGL_STENCIL_BITS_ARB;
                    iattributes[niattribs++] = WGLExt.WGL_DOUBLE_BUFFER_ARB;
                    iattributes[niattribs++] = WGLExt.WGL_STEREO_ARB;
                    iattributes[niattribs++] = WGLExt.WGL_PIXEL_TYPE_ARB;
                    iattributes[niattribs++] = WGLExt.WGL_RED_BITS_ARB;
                    iattributes[niattribs++] = WGLExt.WGL_GREEN_BITS_ARB;
                    iattributes[niattribs++] = WGLExt.WGL_BLUE_BITS_ARB;
                    iattributes[niattribs++] = WGLExt.WGL_ALPHA_BITS_ARB;
                    iattributes[niattribs++] = WGLExt.WGL_ACCUM_RED_BITS_ARB;
                    iattributes[niattribs++] = WGLExt.WGL_ACCUM_GREEN_BITS_ARB;
                    iattributes[niattribs++] = WGLExt.WGL_ACCUM_BLUE_BITS_ARB;
                    iattributes[niattribs++] = WGLExt.WGL_ACCUM_ALPHA_BITS_ARB;
                    if (haveWGLARBMultisample) {
                      iattributes[niattribs++] = WGLExt.WGL_SAMPLE_BUFFERS_ARB;
                      iattributes[niattribs++] = WGLExt.WGL_SAMPLES_ARB;
                    }

                    availableCaps = new GLCapabilities[numFormats];
                    for (int i = 0; i < numFormats; i++) {
                      if (!dummyWGLExt.wglGetPixelFormatAttribivARB(hdc, i+1, 0, niattribs, iattributes, 0, iresults, 0)) {
                        throw new GLException("Error getting pixel format attributes for pixel format " + (i + 1) + " of device context");
                      }
                      availableCaps[i] = WindowsWGLGraphicsConfiguration.AttribList2GLCapabilities(glProfile, iattributes, niattribs, iresults, true);
                    }
                    gotAvailableCaps = true;
                  } else {
                    long lastErr = WGL.GetLastError();
                    // Intel Extreme graphics fails with a zero error code
                    if (lastErr != 0) {
                      throw new GLException("Unable to enumerate pixel formats of window using wglGetPixelFormatAttribivARB: error code " + WGL.GetLastError());
                    }
                  }
                }
              }
            } finally {
              dummyContext.release();
              dummyContext.destroy();
              dummyDrawable.destroy();
            }
          }

          // Fallback path for older cards, in particular Intel Extreme motherboard graphics
          if (!gotAvailableCaps) {
            if (DEBUG) {
              if (!capabilities.getSampleBuffers()) {
                System.err.println(getThreadName() + ": Using ChoosePixelFormat because multisampling not requested");
              } else {
                System.err.println(getThreadName() + ": Using ChoosePixelFormat because no wglChoosePixelFormatARB");
              }
            }
            pfd = WindowsWGLGraphicsConfiguration.GLCapabilities2PFD(capabilities, !useOffScreen);
            recommendedPixelFormat = WGL.ChoosePixelFormat(hdc, pfd);
            if (DEBUG) {
              System.err.println(getThreadName() + ": Recommended pixel format = " + recommendedPixelFormat);
            }
            // Remove one-basing of pixel format (added on later)
            recommendedPixelFormat -= 1;

            numFormats = WGL.DescribePixelFormat(hdc, 1, 0, null);
            if (numFormats == 0) {
              throw new GLException("Unable to enumerate pixel formats of window " +
                                    toHexString(hdc) + " for GLCapabilitiesChooser");
            }
            availableCaps = new GLCapabilities[numFormats];
            for (int i = 0; i < numFormats; i++) {
              if (WGL.DescribePixelFormat(hdc, 1 + i, pfd.size(), pfd) == 0) {
                throw new GLException("Error describing pixel format " + (1 + i) + " of device context");
              }
              availableCaps[i] = WindowsWGLGraphicsConfiguration.PFD2GLCapabilities(glProfile, pfd);
            }
          }

          // NOTE: officially, should make a copy of all of these
          // GLCapabilities to avoid mutation by the end user during the
          // chooseCapabilities call, but for the time being, assume they
          // won't be changed

          if(null!=chooser) {
              // Supply information to chooser
              try {
                pixelFormat = chooser.chooseCapabilities(capabilities, availableCaps, recommendedPixelFormat);
              } catch (NativeWindowException e) {
                throw new GLException(e);
              }
          } else {
              pixelFormat = recommendedPixelFormat;
          }
          if ((pixelFormat < 0) || (pixelFormat >= numFormats)) {
            throw new GLException("Invalid result " + pixelFormat +
                                  " from GLCapabilitiesChooser (should be between 0 and " +
                                  (numFormats - 1) + ")");
          }
          pixelFormat += 1; // one-base the index
          chosenCaps = availableCaps[pixelFormat-1];
          if (DEBUG) {
            System.err.println(getThreadName() + ": Chosen pixel format (" + pixelFormat + "):");
            System.err.println(chosenCaps);
          }
          if (WGL.DescribePixelFormat(hdc, pixelFormat, pfd.size(), pfd) == 0) {
            throw new GLException("Error re-describing the chosen pixel format: " + WGL.GetLastError());
          }
        } else {
          // For now, use ChoosePixelFormat for offscreen surfaces until
          // we figure out how to properly choose an offscreen-
          // compatible pixel format
          pfd = WindowsWGLGraphicsConfiguration.GLCapabilities2PFD(capabilities, !useOffScreen);
          pixelFormat = WGL.ChoosePixelFormat(hdc, pfd);
        }
        if (!WGL.SetPixelFormat(hdc, pixelFormat, pfd)) {
          long lastError = WGL.GetLastError();
          if (DEBUG) {
            System.err.println(getThreadName() + ": SetPixelFormat failed: current context = " + WGL.wglGetCurrentContext() +
                               ", current DC = " + WGL.wglGetCurrentDC());
            System.err.println(getThreadName() + ": GetPixelFormat(hdc " + toHexString(hdc) + ") returns " + WGL.GetPixelFormat(hdc));
          }
          throw new GLException("Unable to set pixel format " + pixelFormat + " for device context " + toHexString(hdc) + ": error code " + lastError);
        }
        // Reuse the previously-constructed GLCapabilities because it
        // turns out that using DescribePixelFormat on some pixel formats
        // (which, for example, support full-scene antialiasing) for some
        // reason return that they are not OpenGL-capable
        if (chosenCaps != null) {
          capabilities = chosenCaps;
        } else {
          capabilities = WindowsWGLGraphicsConfiguration.PFD2GLCapabilities(glProfile, pfd);
        }
        config.setCapsPFD(capabilities, pfd, pixelFormat);
    }

  protected static String getThreadName() {
    return Thread.currentThread().getName();
  }
  public static String toHexString(long hex) {
      return "0x" + Long.toHexString(hex);
  }
}


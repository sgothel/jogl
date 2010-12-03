/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.jogamp.opengl.impl.windows.wgl;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.CapabilitiesChooser;
import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.jogamp.nativewindow.impl.windows.GDI;
import com.jogamp.nativewindow.impl.windows.PIXELFORMATDESCRIPTOR;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;

/** Subclass of GraphicsConfigurationFactory used when non-AWT tookits
    are used on Windows platforms. Toolkits will likely need to delegate
    to this one to change the accepted and returned types of the
    GraphicsDevice and GraphicsConfiguration abstractions. */

public class WindowsWGLGraphicsConfigurationFactory extends GraphicsConfigurationFactory {
    protected static final boolean DEBUG = com.jogamp.opengl.impl.Debug.debug("GraphicsConfiguration");

    WindowsWGLGraphicsConfigurationFactory() {
        GraphicsConfigurationFactory.registerFactory(javax.media.nativewindow.windows.WindowsGraphicsDevice.class, this);
    }

    protected AbstractGraphicsConfiguration chooseGraphicsConfigurationImpl(
            CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested, CapabilitiesChooser chooser, AbstractGraphicsScreen absScreen) {

        if (! (capsChosen instanceof GLCapabilitiesImmutable) ) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects - chosen");
        }

        if (! (capsRequested instanceof GLCapabilitiesImmutable) ) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects - requested");
        }

        return chooseGraphicsConfigurationStatic((GLCapabilitiesImmutable)capsChosen, (GLCapabilitiesImmutable)capsRequested, chooser, absScreen);
    }

    static WindowsWGLGraphicsConfiguration createDefaultGraphicsConfiguration(GLCapabilitiesImmutable caps,
                                                                              AbstractGraphicsScreen absScreen) {
        if(null==absScreen) {
            absScreen = DefaultGraphicsScreen.createDefault(NativeWindowFactory.TYPE_WINDOWS);
        }
        return new WindowsWGLGraphicsConfiguration(absScreen, caps, caps, WindowsWGLGraphicsConfiguration.GLCapabilities2PFD(caps), -1, null);
    }

    static WindowsWGLGraphicsConfiguration chooseGraphicsConfigurationStatic(GLCapabilitiesImmutable capsChosen,
                                                                             GLCapabilitiesImmutable capsReq,
                                                                             CapabilitiesChooser chooser,
                                                                             AbstractGraphicsScreen absScreen) {
        if(null==absScreen) {
            absScreen = DefaultGraphicsScreen.createDefault(NativeWindowFactory.TYPE_WINDOWS);
        }

        if(!capsChosen.isOnscreen() && capsChosen.getDoubleBuffered()) {
            // OFFSCREEN !DOUBLE_BUFFER // FIXME DBLBUFOFFSCRN
            GLCapabilities caps2 = (GLCapabilities) capsChosen.cloneMutable();
            caps2.setDoubleBuffered(false);
            capsChosen = caps2;
        }

        return new WindowsWGLGraphicsConfiguration(absScreen, capsChosen, capsReq,
                                                   WindowsWGLGraphicsConfiguration.GLCapabilities2PFD(capsChosen), -1,
                                                   (GLCapabilitiesChooser)chooser);
    }

    static void updateGraphicsConfiguration(CapabilitiesChooser chooser,
                                            GLDrawableFactory _factory, NativeSurface ns) {
        if (ns == null) {
            throw new IllegalArgumentException("NativeSurface is null");
        }
        long hdc = ns.getSurfaceHandle();
        if (0 == hdc) {
            throw new GLException("Error: HDC is null");
        }
        WindowsWGLGraphicsConfiguration config = (WindowsWGLGraphicsConfiguration) ns.getGraphicsConfiguration().getNativeGraphicsConfiguration();
        if (chooser != null && !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilitiesChooser objects");
        }

        if (DEBUG) {
            System.err.println("updateGraphicsConfiguration: hdc "+toHexString(hdc));
            System.err.println("!!! user chosen caps " + config.getChosenCapabilities());
        }

        if( !updateGraphicsConfigurationARB(hdc, config, chooser, (WindowsWGLDrawableFactory) _factory) ) {
            updateGraphicsConfigurationGDI(hdc, config, chooser, (WindowsWGLDrawableFactory) _factory);
        }
    }

    private static boolean updateGraphicsConfigurationARB(long hdc, WindowsWGLGraphicsConfiguration config,
                                                          CapabilitiesChooser chooser, WindowsWGLDrawableFactory factory) {
        AbstractGraphicsDevice device = config.getScreen().getDevice();
        WindowsWGLContext sharedContext = (WindowsWGLContext) factory.getOrCreateSharedContextImpl(device);
        if (null == sharedContext) {
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationARB: SharedContext is null: "+device);
            }
            return false;
        }
        GLCapabilitiesImmutable capsChosen = (GLCapabilitiesImmutable) config.getChosenCapabilities();
        boolean onscreen = capsChosen.isOnscreen();
        boolean usePBuffer = capsChosen.isPBuffer();
        GLProfile glProfile = capsChosen.getGLProfile();

        int pixelFormatSet = -1; // 1-based pixel format
        GLCapabilitiesImmutable pixelFormatCaps = null;

        GLCapabilitiesImmutable[] availableCaps = null;
        int[] pformats = null; // if != null, then index matches availableCaps
        int numFormats = -1;
        int recommendedIndex = -1;

        synchronized (sharedContext) {
            sharedContext.makeCurrent();
            try {
                if (!sharedContext.isExtensionAvailable("WGL_ARB_pixel_format")) {
                    if (DEBUG) {
                        System.err.println("updateGraphicsConfigurationARB: wglChoosePixelFormatARB not available");
                    }
                    return false;
                }
                if ((pixelFormatSet = GDI.GetPixelFormat(hdc)) >= 1) {
                    // Pixelformat already set by either
                    //  - a previous updateGraphicsConfiguration() call on the same HDC,
                    //  - the graphics driver, copying the HDC's pixelformat to the new one,
                    //  - or the Java2D/OpenGL pipeline's configuration
                    if (DEBUG) {
                        System.err.println("updateGraphicsConfigurationARB: Pixel format already chosen for HDC: " + toHexString(hdc)
                                + ", pixelformat " + pixelFormatSet);
                    }

                    // only fetch the specific one ..
                    pixelFormatCaps = WindowsWGLGraphicsConfiguration.wglARBPFID2GLCapabilities(sharedContext, hdc, pixelFormatSet,                                                                                                glProfile, onscreen, usePBuffer);
                } else {
                    int[] iattributes = new int[2 * WindowsWGLGraphicsConfiguration.MAX_ATTRIBS];
                    float[] fattributes = new float[1];
                    pformats = new int[WindowsWGLGraphicsConfiguration.MAX_PFORMATS];

                    // 1st choice: get GLCapabilities based on users GLCapabilities setting recommendedIndex as preferred choice
                    numFormats = WindowsWGLGraphicsConfiguration.wglChoosePixelFormatARB(hdc, sharedContext, capsChosen,
                                                                                         iattributes, -1, fattributes, pformats);
                    if (0 < numFormats) {
                        availableCaps = WindowsWGLGraphicsConfiguration.wglARBPFIDs2GLCapabilities(sharedContext, hdc, pformats, numFormats,
                                                                                                   glProfile, onscreen, usePBuffer);
                        if (null != availableCaps) {
                            recommendedIndex = 0;
                            pixelFormatCaps = availableCaps[0];
                            if (DEBUG) {
                                System.err.println("updateGraphicsConfigurationARB: NumFormats (wglChoosePixelFormatARB) " + numFormats + " / " + WindowsWGLGraphicsConfiguration.MAX_PFORMATS);
                                System.err.println("updateGraphicsConfigurationARB: Used wglChoosePixelFormatARB to recommend pixel format " + pformats[recommendedIndex] + ", idx " + recommendedIndex);
                                System.err.println("!!! recommended caps " + pixelFormatCaps);
                            }
                        }
                    }

                    // 2nd choice: get all GLCapabilities available, no preferred recommendedIndex available
                    if (null == availableCaps) {
                        if (DEBUG) {
                            System.err.println("updateGraphicsConfigurationARB: wglChoosePixelFormatARB failed (Query all formats without recommendation): " + GDI.GetLastError());
                        }
                        availableCaps = WindowsWGLGraphicsConfiguration.wglARBAllPFIDs2GLCapabilities(sharedContext, hdc,
                                                                                                      glProfile, onscreen, usePBuffer, pformats);
                        if (null != availableCaps) {
                            numFormats = availableCaps.length;
                        }
                    }
                }
            } finally {
                sharedContext.release();
            }
        } // synchronized(factory.sharedContext)

        if (pixelFormatSet <= 0 && null == availableCaps) {
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationARB: No PixelFormat chosen via ARB ... (LastError: " + GDI.GetLastError() + ")");
            }
            return false;
        }

        int pfdID;

        if (pixelFormatSet <= 0) {
            if (null == pixelFormatCaps && null == chooser) {
                chooser = new DefaultGLCapabilitiesChooser();
            }

            int chosenIndex = recommendedIndex;
            try {
                if (null != chooser) {
                    chosenIndex = chooser.chooseCapabilities(capsChosen, availableCaps, recommendedIndex);
                    pixelFormatCaps = availableCaps[chosenIndex];
                    if (DEBUG) {
                        System.err.println("updateGraphicsConfigurationARB: chooser: idx " + chosenIndex);
                        System.err.println("!!! chosen   caps " + pixelFormatCaps);
                    }
                }
            } catch (NativeWindowException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }

            if (chosenIndex < 0) {
                // keep on going ..
                // seek first available one ..
                for (chosenIndex = 0; chosenIndex < availableCaps.length && availableCaps[chosenIndex] == null; chosenIndex++) {
                    // nop
                }
                if (chosenIndex == availableCaps.length) {
                    // give up ..
                    if (DEBUG) {
                        System.err.println("updateGraphicsConfigurationARB: Failed .. nothing available, bail out");
                    }
                    return false;
                }
                pixelFormatCaps = availableCaps[chosenIndex];
                if (DEBUG) {
                    System.err.println("updateGraphicsConfigurationARB: Failed .. unable to choose config, using first available idx: " + chosenIndex);
                    System.err.println("!!! fallback caps " + pixelFormatCaps);
                }
            }
            pfdID = pformats[chosenIndex];
        } else {
            pfdID = pixelFormatSet;
        }
        if (DEBUG) {
            System.err.println("updateGraphicsConfigurationARB: using pfdID "+pfdID);
        }

        PIXELFORMATDESCRIPTOR pfd = WindowsWGLGraphicsConfiguration.createPixelFormatDescriptor();

        if (GDI.DescribePixelFormat(hdc, pfdID, pfd.size(), pfd) == 0) {
            throw new GLException("updateGraphicsConfigurationARB: Error describing the chosen pixel format: " + pfdID + ", " + GDI.GetLastError());
        }

        if (pixelFormatSet <= 0) {
            if (!GDI.SetPixelFormat(hdc, pfdID, pfd)) {
                throw new GLException("Unable to set pixel format " + pfdID +
                                      " for device context " + toHexString(hdc) +
                                      ": error code " + GDI.GetLastError());
            }
        }

        config.setCapsPFD(pixelFormatCaps, pfd, pfdID, true);
        return true;
    }

    private static boolean updateGraphicsConfigurationGDI(long hdc, WindowsWGLGraphicsConfiguration config,
                                                          CapabilitiesChooser chooser, WindowsWGLDrawableFactory factory) {
        GLCapabilitiesImmutable capsChosen = (GLCapabilitiesImmutable) config.getChosenCapabilities();
        boolean onscreen = capsChosen.isOnscreen();
        boolean usePBuffer = capsChosen.isPBuffer();
        GLProfile glProfile = capsChosen.getGLProfile();

        int pixelFormatSet = -1; // 1-based pixel format
        GLCapabilitiesImmutable pixelFormatCaps = null;

        GLCapabilitiesImmutable[] availableCaps = null;
        int numFormats = -1;
        int recommendedIndex = -1;

        if ((pixelFormatSet = GDI.GetPixelFormat(hdc)) != 0) {
            // Pixelformat already set by either
            //  - a previous updateGraphicsConfiguration() call on the same HDC,
            //  - the graphics driver, copying the HDC's pixelformat to the new one,
            //  - or the Java2D/OpenGL pipeline's configuration
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationGDI: NOTE: pixel format already chosen for HDC: " + toHexString(hdc)
                        + ", pixelformat " + pixelFormatSet);
            }
        }

        int recommendedPixelFormat = pixelFormatSet;

        numFormats = GDI.DescribePixelFormat(hdc, 1, 0, null);
        if (numFormats == 0) {
            throw new GLException("Unable to enumerate pixel formats of window "
                    + toHexString(hdc) + " for GLCapabilitiesChooser (LastError: " + GDI.GetLastError() + ")");
        }
        if (DEBUG) {
            System.err.println("updateGraphicsConfigurationGDI: NumFormats (DescribePixelFormat) " + numFormats);
        }
        
        PIXELFORMATDESCRIPTOR pfd = WindowsWGLGraphicsConfiguration.createPixelFormatDescriptor();
        availableCaps = new GLCapabilitiesImmutable[numFormats];
        for (int i = 0; i < numFormats; i++) {
            if (GDI.DescribePixelFormat(hdc, 1 + i, pfd.size(), pfd) == 0) {
                throw new GLException("Error describing pixel format " + (1 + i) + " of device context");
            }
            availableCaps[i] = WindowsWGLGraphicsConfiguration.PFD2GLCapabilities(glProfile, pfd, onscreen, usePBuffer);
        }
        
        int pfdID;

        if (pixelFormatSet <= 0) {
            pfd = WindowsWGLGraphicsConfiguration.GLCapabilities2PFD(capsChosen);
            recommendedPixelFormat = GDI.ChoosePixelFormat(hdc, pfd);
            recommendedIndex = recommendedPixelFormat - 1;
            pixelFormatCaps = availableCaps[recommendedIndex];
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationGDI: ChoosePixelFormat(HDC " + toHexString(hdc) + ") = " + recommendedPixelFormat + " (LastError: " + GDI.GetLastError() + ")");
                System.err.println("!!! recommended caps " + pixelFormatCaps);
            }

            int chosenIndex = recommendedIndex;
            try {
                if (null != chooser) {
                    chosenIndex = chooser.chooseCapabilities(capsChosen, availableCaps, recommendedIndex);
                    pixelFormatCaps = availableCaps[chosenIndex];
                    if (DEBUG) {
                        System.err.println("updateGraphicsConfigurationGDI: chooser: idx " + chosenIndex);
                        System.err.println("!!! chosen   caps " + pixelFormatCaps);
                    }
                }
            } catch (NativeWindowException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }

            if (chosenIndex < 0) {
                // keep on going ..
                // seek first available one ..
                for (chosenIndex = 0; chosenIndex < availableCaps.length && availableCaps[chosenIndex] == null; chosenIndex++) {
                    // nop
                }
                if (chosenIndex == availableCaps.length) {
                    // give up ..
                    if (DEBUG) {
                        System.err.println("updateGraphicsConfigurationGDI: Failed .. nothing available, bail out");
                    }
                    return false;
                }
                pixelFormatCaps = availableCaps[chosenIndex];
                if (DEBUG) {
                    System.err.println("updateGraphicsConfigurationGDI: Failed .. unable to choose config, using first available idx: " + chosenIndex);
                    System.err.println("!!! fallback caps " + pixelFormatCaps);
                }
            }            
            pfdID = chosenIndex + 1;
        } else {
            pfdID = pixelFormatSet;
            pixelFormatCaps = availableCaps[pixelFormatSet-1];
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationGDI: Using preset PFID: " + pixelFormatSet);
                System.err.println("!!! preset caps " + pixelFormatCaps);
            }
        }
        if (DEBUG) {
            System.err.println("updateGraphicsConfigurationGDI: using pfdID "+pfdID);
        }

        if (GDI.DescribePixelFormat(hdc, pfdID, pfd.size(), pfd) == 0) {
            throw new GLException("Error describing the chosen pixel format: " + pfdID + ", " + GDI.GetLastError());
        }

        if (pixelFormatSet <= 0) {
            if (!GDI.SetPixelFormat(hdc, pfdID, pfd)) {
                throw new GLException("Unable to set pixel format " + pfdID +
                                      " for device context " + toHexString(hdc) +
                                      ": error code " + GDI.GetLastError());
            }
        }

        config.setCapsPFD(pixelFormatCaps, pfd, pfdID, true);
        return true;

    }

    static String getThreadName() {
        return Thread.currentThread().getName();
    }

    static String toHexString(long hex) {
        return "0x" + Long.toHexString(hex);
    }
}


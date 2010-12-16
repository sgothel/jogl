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
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.jogamp.nativewindow.impl.windows.GDI;
import com.jogamp.nativewindow.impl.windows.PIXELFORMATDESCRIPTOR;
import com.jogamp.opengl.impl.GLGraphicsConfigurationFactoryImpl;

/** Subclass of GraphicsConfigurationFactory used when non-AWT tookits
    are used on Windows platforms. Toolkits will likely need to delegate
    to this one to change the accepted and returned types of the
    GraphicsDevice and GraphicsConfiguration abstractions. */

public class WindowsWGLGraphicsConfigurationFactory extends GLGraphicsConfigurationFactoryImpl {
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
        return chooseGraphicsConfigurationStatic(caps, caps, null, absScreen);
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

        return new WindowsWGLGraphicsConfiguration( absScreen, capsChosen, capsReq, (GLCapabilitiesChooser)chooser );
    }

    /**
     *
     * @param chooser
     * @param _factory
     * @param ns
     * @param pfIDs optional pool of preselected PixelFormat IDs, maybe null for unrestricted selection
     */
    static void updateGraphicsConfiguration(CapabilitiesChooser chooser,
                                            GLDrawableFactory factory, NativeSurface ns, int[] pfdIDs) {
        if (chooser != null && !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilitiesChooser objects");
        }
        if (factory == null) {
            throw new IllegalArgumentException("GLDrawableFactory is null");
        }
        if (ns == null) {
            throw new IllegalArgumentException("NativeSurface is null");
        }
        long hdc = ns.getSurfaceHandle();
        if (0 == hdc) {
            throw new GLException("Error: HDC is null");
        }
        WindowsWGLGraphicsConfiguration config = (WindowsWGLGraphicsConfiguration) ns.getGraphicsConfiguration().getNativeGraphicsConfiguration();

        if(!config.isDetermined()) {
            updateGraphicsConfiguration(config, chooser, factory, hdc, false, pfdIDs);
        } else {
            // set PFD if not set yet
            int pfdID = -1;
            boolean set = false;
            if ( 1 > ( pfdID = GDI.GetPixelFormat(hdc) ) ) {
                if (!GDI.SetPixelFormat(hdc, config.getPixelFormatID(), config.getPixelFormat())) {
                    throw new GLException("Unable to set pixel format " + config.getPixelFormatID() +
                                          " for device context " + toHexString(hdc) +
                                          ": error code " + GDI.GetLastError());
                }
                set = true;
                pfdID = config.getPixelFormatID();
            }
            if (DEBUG) {
                System.err.println("!!! setPixelFormat (post): hdc "+toHexString(hdc) +", "+config.getPixelFormatID()+" -> "+pfdID+", set: "+set);
                Thread.dumpStack();
            }
        }
    }

    static void preselectGraphicsConfiguration(CapabilitiesChooser chooser,
                                               GLDrawableFactory _factory, AbstractGraphicsDevice device,
                                               WindowsWGLGraphicsConfiguration config, int[] pfdIDs) {
        if (chooser != null && !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilitiesChooser objects");
        }
        if (_factory == null) {
            throw new IllegalArgumentException("GLDrawableFactory is null");
        }
        if (config == null) {
            throw new IllegalArgumentException("WindowsWGLGraphicsConfiguration is null");
        }
        WindowsWGLDrawableFactory factory = (WindowsWGLDrawableFactory) _factory;
        WindowsWGLDrawable sharedDrawable = factory.getSharedDrawable(device);
        if(null == sharedDrawable) {
            throw new IllegalArgumentException("Shared Drawable is null");
        }
        sharedDrawable.lockSurface();
        try {
            long hdc = sharedDrawable.getHandle();
            if (0 == hdc) {
                throw new GLException("Error: HDC is null");
            }
            updateGraphicsConfiguration(config, chooser, factory, hdc, true, pfdIDs);
        } finally {
            sharedDrawable.unlockSurface();
        }
    }

    private static void updateGraphicsConfiguration(WindowsWGLGraphicsConfiguration config, CapabilitiesChooser chooser,
                                                    GLDrawableFactory factory, long hdc, boolean extHDC, int[] pfdIDs) {
        if (DEBUG) {
            if(extHDC) {
                System.err.println("updateGraphicsConfiguration(using shared): hdc "+toHexString(hdc));
            } else {
                System.err.println("updateGraphicsConfiguration(using target): hdc "+toHexString(hdc));
            }
            System.err.println("!!! user chosen caps " + config.getChosenCapabilities());
        }
        if( !updateGraphicsConfigurationARB(hdc, extHDC, config, chooser, (WindowsWGLDrawableFactory)factory, pfdIDs) ) {
            updateGraphicsConfigurationGDI(hdc, extHDC, config, chooser, pfdIDs);
        }
    }

    private static boolean updateGraphicsConfigurationARB(long hdc, boolean extHDC, WindowsWGLGraphicsConfiguration config,
                                                          CapabilitiesChooser chooser, WindowsWGLDrawableFactory factory, int[] pformats) {
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

        GLCapabilitiesImmutable[] availableCaps = null; // caps array matching PFD ID of pformats array
        int pfdID; // chosen or preset PFD ID
        GLCapabilitiesImmutable pixelFormatCaps = null; // chosen or preset PFD ID's caps
        boolean pixelFormatSet = false; // indicates a preset PFD ID [caps]

        if (!sharedContext.isExtensionAvailable(WindowsWGLGraphicsConfiguration.WGL_ARB_pixel_format)) {
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationARB: "+WindowsWGLGraphicsConfiguration.WGL_ARB_pixel_format+" not available");
            }
            return false;
        }
        sharedContext.makeCurrent();
        try {
            if ( !extHDC && 1 <= ( pfdID = GDI.GetPixelFormat(hdc) ) ) {
                // Pixelformat already set by either
                //  - a previous preselectGraphicsConfiguration() call on the same HDC,
                //  - the graphics driver, copying the HDC's pixelformat to the new one,
                //  - or the Java2D/OpenGL pipeline's configuration
                if (DEBUG) {
                    System.err.println("updateGraphicsConfigurationARB: Pixel format already chosen for HDC: " + toHexString(hdc)
                            + ", pixelformat " + pfdID);
                }
                pixelFormatSet = true;
                pixelFormatCaps = WindowsWGLGraphicsConfiguration.wglARBPFID2GLCapabilities(sharedContext, hdc, pfdID, glProfile, onscreen, usePBuffer);
            } else {
                int recommendedIndex = -1; // recommended index

                if(null == pformats) {
                    // No given PFD IDs
                    //
                    // 1st choice: get GLCapabilities based on users GLCapabilities setting recommendedIndex as preferred choice
                    int[] iattributes = new int[2 * WindowsWGLGraphicsConfiguration.MAX_ATTRIBS];
                    float[] fattributes = new float[1];
                    pformats = WindowsWGLGraphicsConfiguration.wglChoosePixelFormatARB(hdc, sharedContext, capsChosen,
                                                                                       iattributes, -1, fattributes);
                    if (null != pformats) {
                        recommendedIndex = 0;
                        if (DEBUG) {
                            System.err.println("updateGraphicsConfigurationARB: NumFormats (wglChoosePixelFormatARB) " + pformats.length);
                            System.err.println("updateGraphicsConfigurationARB: Used wglChoosePixelFormatARB to recommend pixel format " + pformats[recommendedIndex] + ", idx " + recommendedIndex);
                        }
                    } else {
                        // 2nd choice: get all GLCapabilities available, no preferred recommendedIndex available
                        pformats = WindowsWGLGraphicsConfiguration.wglAllARBPFIDs(sharedContext, hdc);
                        if (DEBUG) {
                            System.err.println("updateGraphicsConfigurationARB: NumFormats (wglAllARBPFIDs) " + pformats.length);
                        }
                    }
                    if (null == pformats) {
                        if (DEBUG) {
                            Thread.dumpStack();
                        }
                        return false;
                    }
                }
                // translate chosen/all or given PFD IDs
                availableCaps = WindowsWGLGraphicsConfiguration.wglARBPFIDs2GLCapabilities(sharedContext, hdc, pformats,
                                                                                           glProfile, onscreen, usePBuffer);

                int chosenIndex = chooseCapabilities(chooser, capsChosen, availableCaps, recommendedIndex);
                if ( 0 > chosenIndex ) {
                    if (DEBUG) {
                        Thread.dumpStack();
                    }
                    return false;
                }
                pixelFormatCaps = availableCaps[chosenIndex];
                pfdID = pformats[chosenIndex];
                if( null == pixelFormatCaps) {
                    throw new GLException("Null Capabilities with "+
                                          " chosen pfdID: native recommended "+ (recommendedIndex+1) +
                                          " chosen "+pfdID);
                }
                if (DEBUG) {
                    System.err.println("!!! chosen pfdID (ARB): native recommended "+ (recommendedIndex+1) +
                                       " chosen "+pfdID+", caps " + pixelFormatCaps);
                }
            }
        } finally {
            sharedContext.release();
        }

        PIXELFORMATDESCRIPTOR pfd = WindowsWGLGraphicsConfiguration.createPixelFormatDescriptor();

        if (GDI.DescribePixelFormat(hdc, pfdID, pfd.size(), pfd) == 0) {
            throw new GLException("updateGraphicsConfigurationARB: Error describing the chosen pixel format: " + pfdID + ", " + GDI.GetLastError());
        }

        if ( !extHDC && !pixelFormatSet ) {
            if (!GDI.SetPixelFormat(hdc, pfdID, pfd)) {
                throw new GLException("Unable to set pixel format " + pfdID +
                                      " for device context " + toHexString(hdc) +
                                      ": error code " + GDI.GetLastError());
            }
            if (DEBUG) {
                System.err.println("!!! setPixelFormat (ARB): hdc "+toHexString(hdc) +", "+config.getPixelFormatID()+" -> "+pfdID);
            }
        }
        config.setCapsPFD(pixelFormatCaps, pfd, pfdID, true);
        return true;
    }

    private static boolean updateGraphicsConfigurationGDI(long hdc, boolean extHDC, WindowsWGLGraphicsConfiguration config,
                                                          CapabilitiesChooser chooser, int[] pformats) {
        GLCapabilitiesImmutable capsChosen = (GLCapabilitiesImmutable) config.getChosenCapabilities();
        boolean onscreen = capsChosen.isOnscreen();
        boolean usePBuffer = capsChosen.isPBuffer();
        GLProfile glProfile = capsChosen.getGLProfile();

        GLCapabilitiesImmutable[] availableCaps = null; // caps array matching PFD ID of pformats array
        int pfdID; // chosen or preset PFD ID
        GLCapabilitiesImmutable pixelFormatCaps = null; // chosen or preset PFD ID's caps
        boolean pixelFormatSet = false; // indicates a preset PFD ID [caps]

        PIXELFORMATDESCRIPTOR pfd = WindowsWGLGraphicsConfiguration.createPixelFormatDescriptor(); // PFD storage

        if ( !extHDC && 1 <= ( pfdID = GDI.GetPixelFormat(hdc) ) ) {
            // Pixelformat already set by either
            //  - a previous preselectGraphicsConfiguration() call on the same HDC,
            //  - the graphics driver, copying the HDC's pixelformat to the new one,
            //  - or the Java2D/OpenGL pipeline's configuration
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationGDI: NOTE: pixel format already chosen for HDC: " + toHexString(hdc)
                        + ", pixelformat " + pfdID);
            }
            pixelFormatSet = true;
            pixelFormatCaps = WindowsWGLGraphicsConfiguration.PFD2GLCapabilities(glProfile, hdc, pfdID, onscreen, usePBuffer, pfd);
        } else {
            if(null == pformats) {
                pformats = WindowsWGLGraphicsConfiguration.wglAllGDIPFIDs(hdc);
            }
            int numFormats = pformats.length;
            availableCaps = new GLCapabilitiesImmutable[numFormats];
            for (int i = 0; i < numFormats; i++) {
                availableCaps[i] = WindowsWGLGraphicsConfiguration.PFD2GLCapabilities(glProfile, hdc, pformats[i], onscreen, usePBuffer, pfd);
            }
            pfd = WindowsWGLGraphicsConfiguration.GLCapabilities2PFD(capsChosen, pfd);
            pfdID = GDI.ChoosePixelFormat(hdc, pfd);
            int recommendedIndex = -1 ;
            if( 1 <= pfdID ) {
                // seek index ..
                for (recommendedIndex = numFormats - 1 ;
                     0 <= recommendedIndex && pfdID != pformats[recommendedIndex];
                     recommendedIndex--)
                { /* nop */ }
            }
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationGDI: ChoosePixelFormat(HDC " + toHexString(hdc) + ") = " + pfdID + ", idx " + recommendedIndex + " (LastError: " + GDI.GetLastError() + ")");
            }
            int chosenIndex = chooseCapabilities(chooser, capsChosen, availableCaps, recommendedIndex);
            if ( 0 > chosenIndex ) {
                if (DEBUG) {
                    Thread.dumpStack();
                }
                return false;
            }
            pixelFormatCaps = availableCaps[chosenIndex];
            pfdID = pformats[chosenIndex];
            if( null == pixelFormatCaps) {
                throw new GLException("Null Capabilities with "+
                                      " chosen pfdID: native recommended "+ (recommendedIndex+1) +
                                      " chosen "+pfdID);
            }
            if (DEBUG) {
                System.err.println("!!! chosen pfdID (GDI): native recommended "+ (recommendedIndex+1) +
                                   " chosen "+pfdID+", caps " + pixelFormatCaps);
            }
        }
        
        if (GDI.DescribePixelFormat(hdc, pfdID, pfd.size(), pfd) == 0) {
            throw new GLException("Error describing the chosen pixel format: " + pfdID + ", " + GDI.GetLastError());
        }

        if ( !extHDC && !pixelFormatSet ) {
            if (!GDI.SetPixelFormat(hdc, pfdID, pfd)) {
                throw new GLException("Unable to set pixel format " + pfdID +
                                      " for device context " + toHexString(hdc) +
                                      ": error code " + GDI.GetLastError());
            }
            if (DEBUG) {
                System.err.println("!!! setPixelFormat (GDI): hdc "+toHexString(hdc) +", "+config.getPixelFormatID()+" -> "+pfdID);
            }
        }
        config.setCapsPFD(pixelFormatCaps, pfd, pfdID, false);
        return true;

    }

    static String getThreadName() {
        return Thread.currentThread().getName();
    }

    static String toHexString(long hex) {
        return "0x" + Long.toHexString(hex);
    }
}


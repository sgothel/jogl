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

package jogamp.opengl.windows.wgl;

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
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import jogamp.nativewindow.windows.GDI;
import jogamp.nativewindow.windows.PIXELFORMATDESCRIPTOR;
import jogamp.opengl.GLGraphicsConfigurationFactory;
import jogamp.opengl.GLGraphicsConfigurationUtil;
import jogamp.opengl.SharedResourceRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Subclass of GraphicsConfigurationFactory used when non-AWT tookits
    are used on Windows platforms. Toolkits will likely need to delegate
    to this one to change the accepted and returned types of the
    GraphicsDevice and GraphicsConfiguration abstractions. */

public class WindowsWGLGraphicsConfigurationFactory extends GLGraphicsConfigurationFactory {
    protected static final boolean DEBUG = jogamp.opengl.Debug.debug("GraphicsConfiguration");
    static WGLGLCapabilities.PfdIDComparator PfdIDComparator = new WGLGLCapabilities.PfdIDComparator();

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
        AbstractGraphicsDevice absDevice = absScreen.getDevice();

        capsChosen = GLGraphicsConfigurationUtil.fixGLCapabilities(
                capsChosen, GLDrawableFactory.getDesktopFactory().canCreateGLPbuffer(absDevice) );

        return new WindowsWGLGraphicsConfiguration( absScreen, capsChosen, capsReq, (GLCapabilitiesChooser)chooser );
    }

    protected static List/*<WGLGLCapabilities>*/ getAvailableCapabilities(WindowsWGLDrawableFactory factory, AbstractGraphicsDevice device) {
        SharedResourceRunner.Resource sharedResource = factory.getOrCreateSharedResource(device);
        if(null == sharedResource) {
            throw new GLException("Shared resource for device n/a: "+device);
        }
        WindowsWGLDrawable sharedDrawable = (WindowsWGLDrawable) sharedResource.getDrawable();
        GLCapabilitiesImmutable capsChosen = sharedDrawable.getChosenGLCapabilities();
        WindowsWGLContext sharedContext = (WindowsWGLContext) sharedResource.getContext();
        List availableCaps = null;

        sharedDrawable.lockSurface();
        try {
            long hdc = sharedDrawable.getHandle();
            if (0 == hdc) {
                throw new GLException("Error: HDC is null");
            }
            if (sharedContext.isExtensionAvailable(WindowsWGLGraphicsConfiguration.WGL_ARB_pixel_format)) {
                availableCaps = getAvailableGLCapabilitiesARB(hdc, sharedContext, capsChosen.getGLProfile());
            }
            if( null == availableCaps || 0 == availableCaps.size() ) {
                availableCaps = getAvailableGLCapabilitiesGDI(hdc, capsChosen);
            }
        } finally {
            sharedDrawable.unlockSurface();
        }

        if( null != availableCaps ) {
            Collections.sort(availableCaps, PfdIDComparator);
        }
        return availableCaps;
    }

    static List/*<WGLGLCapabilities>*/ getAvailableGLCapabilitiesARB(long hdc, WindowsWGLContext sharedContext, GLProfile glProfile) {
        int[] pformats = WindowsWGLGraphicsConfiguration.wglAllARBPFIDs(sharedContext, hdc);
        return WindowsWGLGraphicsConfiguration.wglARBPFIDs2AllGLCapabilities(sharedContext, hdc, pformats, glProfile);
    }

    static List/*<WGLGLCapabilities>*/ getAvailableGLCapabilitiesGDI(long hdc, GLCapabilitiesImmutable capsChosen) {
        boolean onscreen = capsChosen.isOnscreen();
        if(capsChosen.isPBuffer()) {
            return null;
        }
        GLProfile glProfile = capsChosen.getGLProfile();

        int[] pformats = WindowsWGLGraphicsConfiguration.wglAllGDIPFIDs(hdc);
        int numFormats = pformats.length;
        ArrayList bucket = new ArrayList(numFormats);
        for (int i = 0; i < numFormats; i++) {
            WGLGLCapabilities wglglcapabilities = WindowsWGLGraphicsConfiguration.PFD2GLCapabilities(glProfile, hdc, pformats[i], onscreen);
            // formats that don't draw to a window come back null; don't add them or they'll crash debug output
            if( null != wglglcapabilities ) {
                bucket.add( wglglcapabilities );
            }
        }
        return bucket;
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

        if(NativeSurface.LOCK_SURFACE_NOT_READY >= ns.lockSurface()) {
            throw new GLException("Surface not ready (lockSurface)");
        }
        try {
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
        } finally {
            ns.unlockSurface();
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
        WindowsWGLDrawable sharedDrawable = factory.getOrCreateSharedDrawable(device);
        if(null == sharedDrawable) {
            throw new IllegalArgumentException("Shared Drawable is null");
        }

        if(NativeSurface.LOCK_SURFACE_NOT_READY >= sharedDrawable.lockSurface()) {
            throw new GLException("Surface not ready (lockSurface)");
        }
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
        if (!sharedContext.isExtensionAvailable(WindowsWGLGraphicsConfiguration.WGL_ARB_pixel_format)) {
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationARB: "+WindowsWGLGraphicsConfiguration.WGL_ARB_pixel_format+" not available");
            }
            return false;
        }

        GLCapabilitiesImmutable capsChosen = (GLCapabilitiesImmutable) config.getChosenCapabilities();
        boolean onscreen = capsChosen.isOnscreen();
        boolean usePBuffer = capsChosen.isPBuffer();
        GLProfile glProfile = capsChosen.getGLProfile();

        WGLGLCapabilities pixelFormatCaps = null; // chosen or preset PFD ID's caps
        boolean pixelFormatSet = false; // indicates a preset PFD ID [caps]
        final int presetPFDID = extHDC ? -1 : GDI.GetPixelFormat(hdc) ;
        if ( 1 <= presetPFDID ) {
            // Pixelformat already set by either
            //  - a previous preselectGraphicsConfiguration() call on the same HDC,
            //  - the graphics driver, copying the HDC's pixelformat to the new one,
            //  - or the Java2D/OpenGL pipeline's configuration
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationARB: Pixel format already chosen for HDC: " + toHexString(hdc)
                        + ", pixelformat " + presetPFDID);
            }
            pixelFormatSet = true;
            pixelFormatCaps = WindowsWGLGraphicsConfiguration.wglARBPFID2GLCapabilities(sharedContext, hdc, presetPFDID, glProfile, onscreen, usePBuffer);
        } else {
            int recommendedIndex = -1; // recommended index

            if(null == pformats) {
                // No given PFD IDs
                //
                // 1st choice: get GLCapabilities based on users GLCapabilities setting recommendedIndex as preferred choice
                int[] iattributes = new int[2 * WindowsWGLGraphicsConfiguration.MAX_ATTRIBS];
                float[] fattributes = new float[1];
                int accelerationMode = WGLExt.WGL_FULL_ACCELERATION_ARB;
                pformats = WindowsWGLGraphicsConfiguration.wglChoosePixelFormatARB(hdc, sharedContext, capsChosen,
                                                                                   iattributes, accelerationMode, fattributes);
                if (null == pformats) {
                    accelerationMode = WGLExt.WGL_GENERIC_ACCELERATION_ARB;
                    pformats = WindowsWGLGraphicsConfiguration.wglChoosePixelFormatARB(hdc, sharedContext, capsChosen,
                                                                                       iattributes, accelerationMode, fattributes);
                }
                if (null == pformats) {
                    accelerationMode = -1; // use what we are offered ..
                    pformats = WindowsWGLGraphicsConfiguration.wglChoosePixelFormatARB(hdc, sharedContext, capsChosen,
                                                                                       iattributes, accelerationMode, fattributes);
                }
                if (null != pformats) {
                    recommendedIndex = 0;
                } else {
                    if(DEBUG) {
                        System.err.println("updateGraphicsConfigurationARB: wglChoosePixelFormatARB failed with: "+capsChosen);
                    }
                    // 2nd choice: get all GLCapabilities available, no preferred recommendedIndex available
                    pformats = WindowsWGLGraphicsConfiguration.wglAllARBPFIDs(sharedContext, hdc);
                    if (DEBUG) {
                        final int len = ( null != pformats ) ? pformats.length : 0;
                        System.err.println("updateGraphicsConfigurationARB: NumFormats (wglAllARBPFIDs) " + len);
                    }
                }
                if (null == pformats) {
                    if (DEBUG) {
                        Thread.dumpStack();
                    }
                    return false;
                }
            }

            List /*<WGLGLCapabilities>*/ availableCaps =
                WindowsWGLGraphicsConfiguration.wglARBPFIDs2GLCapabilities(sharedContext, hdc, pformats,
                                                                           glProfile, onscreen, usePBuffer);
            if( null == availableCaps || 0 == availableCaps.size() ) {
                if (DEBUG) {
                    System.err.println("updateGraphicsConfigurationARB: wglARBPFIDs2GLCapabilities failed with " + pformats.length +
                                       " pfd ids, onscreen " + onscreen + ", pbuffer " + usePBuffer);
                    Thread.dumpStack();
                }
                return false;
            }

            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationARB: " + pformats.length +
                                   " pfd ids, onscreen " + onscreen + ", pbuffer " + usePBuffer + ", " + availableCaps.size() + " glcaps");
                if(0 <= recommendedIndex) {
                    System.err.println("updateGraphicsConfigurationARB: Used wglChoosePixelFormatARB to recommend pixel format " +
                                       pformats[recommendedIndex] + ", idx " + recommendedIndex +", "+availableCaps.get(recommendedIndex));
                }
            }

            int chosenIndex = chooseCapabilities(chooser, capsChosen, availableCaps, recommendedIndex);
            if ( 0 > chosenIndex ) {
                if (DEBUG) {
                    Thread.dumpStack();
                }
                return false;
            }
            pixelFormatCaps = (WGLGLCapabilities) availableCaps.get(chosenIndex);
            if( null == pixelFormatCaps) {
                throw new GLException("Null Capabilities with "+
                                      " chosen pfdID: native recommended "+ (recommendedIndex+1) +
                                      " chosen "+pixelFormatCaps.getPFDID());
            }
            if (DEBUG) {
                System.err.println("!!! chosen pfdID (ARB): native recommended "+ (recommendedIndex+1) +
                                   " chosen "+pixelFormatCaps);
            }
        }

        if ( !extHDC && !pixelFormatSet ) {
            if (!GDI.SetPixelFormat(hdc, pixelFormatCaps.getPFDID(), pixelFormatCaps.getPFD())) {
                throw new GLException("Unable to set pixel format " + pixelFormatCaps.getPFDID() +
                                      " for device context " + toHexString(hdc) +
                                      ": error code " + GDI.GetLastError());
            }
            if (DEBUG) {
                System.err.println("!!! setPixelFormat (ARB): hdc "+toHexString(hdc) +", "+config.getPixelFormatID()+" -> "+pixelFormatCaps.getPFDID());
            }
        }
        config.setCapsPFD(pixelFormatCaps);
        return true;
    }

    private static boolean updateGraphicsConfigurationGDI(long hdc, boolean extHDC, WindowsWGLGraphicsConfiguration config,
                                                          CapabilitiesChooser chooser, int[] pformats) {
        GLCapabilitiesImmutable capsChosen = (GLCapabilitiesImmutable) config.getChosenCapabilities();
        if(capsChosen.isPBuffer()) {
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationGDI: no pbuffer supported on GDI: " + capsChosen);
            }
            return false;
        }
        boolean onscreen = capsChosen.isOnscreen();
        GLProfile glProfile = capsChosen.getGLProfile();

        ArrayList/*<WGLGLCapabilities>*/ availableCaps = new ArrayList();
        int pfdID; // chosen or preset PFD ID
        WGLGLCapabilities pixelFormatCaps = null; // chosen or preset PFD ID's caps
        boolean pixelFormatSet = false; // indicates a preset PFD ID [caps]

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
            pixelFormatCaps = WindowsWGLGraphicsConfiguration.PFD2GLCapabilities(glProfile, hdc, pfdID, onscreen);
        } else {
            if(null == pformats) {
                pformats = WindowsWGLGraphicsConfiguration.wglAllGDIPFIDs(hdc);
            }
            final int winattrmask = GLGraphicsConfigurationUtil.getWinAttributeBits(onscreen, false);

            for (int i = 0; i < pformats.length; i++) {
                WindowsWGLGraphicsConfiguration.PFD2GLCapabilities(availableCaps, glProfile, hdc, pformats[i], winattrmask);
            }

            // 1st choice: get GLCapabilities based on users GLCapabilities setting recommendedIndex as preferred choice
            PIXELFORMATDESCRIPTOR pfd = WindowsWGLGraphicsConfiguration.createPixelFormatDescriptor();
            pfd = WindowsWGLGraphicsConfiguration.GLCapabilities2PFD(capsChosen, pfd);
            pfdID = GDI.ChoosePixelFormat(hdc, pfd);
            int recommendedIndex = -1 ;
            if( 1 <= pfdID ) {
                // seek index ..
                for (recommendedIndex = availableCaps.size() - 1 ;
                     0 <= recommendedIndex && pfdID != ((WGLGLCapabilities) availableCaps.get(recommendedIndex)).getPFDID();
                     recommendedIndex--)
                { /* nop */ }
            }
            // 2nd choice: if no preferred recommendedIndex available
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
            pixelFormatCaps = (WGLGLCapabilities) availableCaps.get(chosenIndex);
            if (DEBUG) {
                System.err.println("!!! chosen pfdID (GDI): native recommended "+ (recommendedIndex+1) +
                                   ", caps " + pixelFormatCaps);
            }
        }

        if ( !extHDC && !pixelFormatSet ) {
            if (!GDI.SetPixelFormat(hdc, pixelFormatCaps.getPFDID(), pixelFormatCaps.getPFD())) {
                throw new GLException("Unable to set pixel format " + pixelFormatCaps.getPFDID() +
                                      " for device context " + toHexString(hdc) +
                                      ": error code " + GDI.GetLastError());
            }
            if (DEBUG) {
                System.err.println("!!! setPixelFormat (GDI): hdc "+toHexString(hdc) +", "+config.getPixelFormatID()+" -> " + pixelFormatCaps.getPFDID());
            }
        }
        config.setCapsPFD(pixelFormatCaps);
        return true;
    }
}


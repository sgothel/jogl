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

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.CapabilitiesChooser;
import com.jogamp.nativewindow.DefaultGraphicsScreen;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.VisualIDHolder;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GLRendererQuirks;

import jogamp.nativewindow.windows.GDI;
import jogamp.nativewindow.windows.GDIUtil;
import jogamp.nativewindow.windows.PIXELFORMATDESCRIPTOR;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLGraphicsConfigurationFactory;
import jogamp.opengl.GLGraphicsConfigurationUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Subclass of GraphicsConfigurationFactory used when non-AWT tookits
    are used on Windows platforms. Toolkits will likely need to delegate
    to this one to change the accepted and returned types of the
    GraphicsDevice and GraphicsConfiguration abstractions. */

public class WindowsWGLGraphicsConfigurationFactory extends GLGraphicsConfigurationFactory {
    static VisualIDHolder.VIDComparator PfdIDComparator = new VisualIDHolder.VIDComparator(VisualIDHolder.VIDType.WIN32_PFD);

    static void registerFactory() {
        GraphicsConfigurationFactory.registerFactory(com.jogamp.nativewindow.windows.WindowsGraphicsDevice.class,
                                                     GLCapabilitiesImmutable.class, new WindowsWGLGraphicsConfigurationFactory());
    }
    private WindowsWGLGraphicsConfigurationFactory() {
    }

    @Override
    protected AbstractGraphicsConfiguration chooseGraphicsConfigurationImpl(
            final CapabilitiesImmutable capsChosen, final CapabilitiesImmutable capsRequested, final CapabilitiesChooser chooser,
            final AbstractGraphicsScreen absScreen, final int nativeVisualID)
    {

        if (! (capsChosen instanceof GLCapabilitiesImmutable) ) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects - chosen");
        }

        if (! (capsRequested instanceof GLCapabilitiesImmutable) ) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects - requested");
        }

        if (chooser != null && !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilitiesChooser objects");
        }

        return chooseGraphicsConfigurationStatic(
                (GLCapabilitiesImmutable)capsChosen, (GLCapabilitiesImmutable)capsRequested, (GLCapabilitiesChooser)chooser, absScreen);
    }

    static WindowsWGLGraphicsConfiguration createDefaultGraphicsConfiguration(final GLCapabilitiesImmutable caps,
                                                                              final AbstractGraphicsScreen absScreen) {
        return chooseGraphicsConfigurationStatic(caps, caps, null, absScreen);
    }

    static WindowsWGLGraphicsConfiguration chooseGraphicsConfigurationStatic(GLCapabilitiesImmutable capsChosen,
                                                                             final GLCapabilitiesImmutable capsReq,
                                                                             final GLCapabilitiesChooser chooser,
                                                                             AbstractGraphicsScreen absScreen) {
        if(null==absScreen) {
            absScreen = DefaultGraphicsScreen.createDefault(NativeWindowFactory.TYPE_WINDOWS);
        }
        final AbstractGraphicsDevice absDevice = absScreen.getDevice();
        capsChosen = GLGraphicsConfigurationUtil.fixGLCapabilities( capsChosen, GLDrawableFactory.getDesktopFactory(), absDevice);
        return new WindowsWGLGraphicsConfiguration( absScreen, capsChosen, capsReq, chooser );
    }

    protected static List<GLCapabilitiesImmutable> getAvailableCapabilities(final WindowsWGLDrawableFactory factory,
                                                                            final AbstractGraphicsDevice device) {
        final WindowsWGLDrawableFactory.SharedResource sharedResource = factory.getOrCreateSharedResourceImpl(device);
        if(null == sharedResource) {
            throw new GLException("Shared resource for device n/a: "+device);
        }
        final GLDrawableImpl sharedDrawable = sharedResource.getDrawable();
        final GLProfile glp = GLProfile.getDefault(device);

        List<GLCapabilitiesImmutable> availableCaps = null;

        final GLContext sharedContext;
        if ( factory.hasRendererQuirk(device, null, GLRendererQuirks.NeedCurrCtx4ARBPixFmtQueries) ) {
            sharedContext = sharedResource.getContext();
            if(GLContext.CONTEXT_NOT_CURRENT == sharedContext.makeCurrent()) {
                throw new GLException("Could not make Shared Context current: "+device);
            }
        } else {
            sharedContext = null;
            sharedDrawable.lockSurface();
        }
        try {
            final long hdc = sharedDrawable.getHandle();
            if (0 == hdc) {
                throw new GLException("Error: HDC is null");
            }
            if (sharedResource.hasARBPixelFormat()) {
                availableCaps = WindowsWGLGraphicsConfigurationFactory.getAvailableGLCapabilitiesARB(
                                                                            sharedResource, sharedResource.getDevice(), glp, hdc);
            }
            final boolean hasARBCaps = null != availableCaps && !availableCaps.isEmpty() ;
            final List<GLCapabilitiesImmutable> availableCapsGDI = getAvailableGLCapabilitiesGDI(device, glp, hdc, hasARBCaps);
            if( !hasARBCaps ) {
                availableCaps = availableCapsGDI;
            } else {
                availableCaps.addAll(availableCapsGDI);
            }
        } finally {
            if ( null != sharedContext ) {
                sharedContext.release();
            } else {
                sharedDrawable.unlockSurface();
            }
        }

        if( null != availableCaps && availableCaps.size() > 1 ) {
            Collections.sort(availableCaps, PfdIDComparator);
        }
        return availableCaps;
    }

    private static List<GLCapabilitiesImmutable> getAvailableGLCapabilitiesARB(
                                    final WindowsWGLDrawableFactory.SharedResource sharedResource,
                                    final AbstractGraphicsDevice device, final GLProfile glProfile, final long hdc)
    {
        final int pfdIDCount = WindowsWGLGraphicsConfiguration.wglARBPFDIDCount((WindowsWGLContext)sharedResource.getContext(), hdc);
        final int[] pformats = WindowsWGLGraphicsConfiguration.wglAllARBPFDIDs(pfdIDCount);
        return WindowsWGLGraphicsConfiguration.wglARBPFIDs2GLCapabilities(sharedResource, device, glProfile, hdc, pformats,
                GLGraphicsConfigurationUtil.ALL_BITS & ~GLGraphicsConfigurationUtil.BITMAP_BIT, false); // w/o BITMAP
    }

    private static List<GLCapabilitiesImmutable> getAvailableGLCapabilitiesGDI(
            final AbstractGraphicsDevice device, final GLProfile glProfile, final long hdc, final boolean bitmapOnly)
    {
        final int[] pformats = WindowsWGLGraphicsConfiguration.wglAllGDIPFIDs(hdc);
        final int numFormats = pformats.length;
        final List<GLCapabilitiesImmutable> bucket = new ArrayList<GLCapabilitiesImmutable>(numFormats);
        for (int i = 0; i < numFormats; i++) {
            final GLCapabilitiesImmutable caps = WindowsWGLGraphicsConfiguration.PFD2GLCapabilities(device, glProfile, hdc, pformats[i],
                 bitmapOnly ? GLGraphicsConfigurationUtil.BITMAP_BIT : GLGraphicsConfigurationUtil.ALL_BITS );
            if(null != caps) {
                bucket.add(caps);
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
    static void updateGraphicsConfiguration(final CapabilitiesChooser chooser,
                                            final GLDrawableFactory factory, final NativeSurface ns, final int[] pfdIDs) {
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
            final long hdc = ns.getSurfaceHandle();
            if (0 == hdc) {
                if( !(ns instanceof ProxySurface) ||
                    !((ProxySurface)ns).containsUpstreamOptionBits( ProxySurface.OPT_UPSTREAM_SURFACELESS ) ) {
                    throw new GLException(String.format("non-surfaceless drawable has zero-handle (HDC): %s", ns.toString()));
                }
                return; // NOP .. will reach ns.unlockSurface()
            }
            final WindowsWGLGraphicsConfiguration config = (WindowsWGLGraphicsConfiguration) ns.getGraphicsConfiguration();

            if( !config.isExternal() ) {
                if( !config.isDetermined() ) {
                    updateGraphicsConfiguration(config, chooser, factory, hdc, false, pfdIDs);
                } else {
                    // set PFD if not set yet
                    int pfdID = -1;
                    boolean set = false;
                    if ( 1 > ( pfdID = WGLUtil.GetPixelFormat(hdc) ) ) {
                        if (!WGLUtil.SetPixelFormat(hdc, config.getPixelFormatID(), config.getPixelFormat())) {
                            throw new GLException("Unable to set pixel format " + config.getPixelFormatID() +
                                                  " for device context " + toHexString(hdc) +
                                                  ": error code " + GDI.GetLastError());
                        }
                        set = true;
                    }
                    if (DEBUG) {
                        System.err.println("setPixelFormat (post): hdc "+toHexString(hdc) +", "+pfdID+" -> "+config.getPixelFormatID()+", set: "+set);
                    }
                }
            }
        } finally {
            ns.unlockSurface();
        }
    }

    static void preselectGraphicsConfiguration(final CapabilitiesChooser chooser,
                                               final GLDrawableFactory _factory, final AbstractGraphicsDevice device,
                                               final WindowsWGLGraphicsConfiguration config, final int[] pfdIDs) {
        if (chooser != null && !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilitiesChooser objects");
        }
        if (_factory == null) {
            throw new IllegalArgumentException("GLDrawableFactory is null");
        }
        if (config == null) {
            throw new IllegalArgumentException("WindowsWGLGraphicsConfiguration is null");
        }
        if ( !(_factory instanceof WindowsWGLDrawableFactory) ) {
            throw new GLException("GLDrawableFactory is not a WindowsWGLDrawableFactory, but: "+_factory.getClass().getSimpleName());
        }
        final WindowsWGLDrawableFactory factory = (WindowsWGLDrawableFactory) _factory;
        final WindowsWGLDrawable sharedDrawable = factory.getOrCreateSharedDrawable(device);
        if(null == sharedDrawable) {
            throw new IllegalArgumentException("Shared Drawable is null");
        }

        if(NativeSurface.LOCK_SURFACE_NOT_READY >= sharedDrawable.lockSurface()) {
            throw new GLException("Shared Surface not ready (lockSurface): "+device+" -> "+sharedDrawable);
        }
        try {
            final long hdc = sharedDrawable.getHandle();
            if (0 == hdc) {
                throw new GLException("Error: HDC is null");
            }
            updateGraphicsConfiguration(config, chooser, factory, hdc, true, pfdIDs);
        } finally {
            sharedDrawable.unlockSurface();
        }
    }

    private static void updateGraphicsConfiguration(final WindowsWGLGraphicsConfiguration config, final CapabilitiesChooser chooser,
                                                    final GLDrawableFactory factory, final long hdc, final boolean extHDC,
                                                    final int[] pfdIDs) {
        if (DEBUG) {
            if(extHDC) {
                System.err.println("updateGraphicsConfiguration(using shared): hdc "+toHexString(hdc));
            } else {
                System.err.println("updateGraphicsConfiguration(using target): hdc "+toHexString(hdc));
            }
            System.err.println("user chosen caps " + config.getChosenCapabilities());
        }
        final AbstractGraphicsDevice device = config.getScreen().getDevice();
        final WindowsWGLDrawableFactory.SharedResource sharedResource = ((WindowsWGLDrawableFactory)factory).getOrCreateSharedResourceImpl(device);
        final GLContext sharedContext;
        if ( factory.hasRendererQuirk(device, null, GLRendererQuirks.NeedCurrCtx4ARBPixFmtQueries) ) {
            sharedContext = sharedResource.getContext();
            if(GLContext.CONTEXT_NOT_CURRENT == sharedContext.makeCurrent()) {
                throw new GLException("Could not make Shared Context current: "+device);
            }
        } else {
            sharedContext = null;
        }
        try {
            final GLCapabilitiesImmutable capsChosen = (GLCapabilitiesImmutable) config.getChosenCapabilities();
            boolean done = false;
            if( capsChosen.getHardwareAccelerated() && !capsChosen.isBitmap() ) {
                done = updateGraphicsConfigurationARB((WindowsWGLDrawableFactory)factory, config, chooser, hdc, extHDC, pfdIDs);
            }
            if( !done ) {
                updateGraphicsConfigurationGDI(config, chooser, hdc, extHDC, pfdIDs);
            }
        } finally {
            if (null != sharedContext) {
                sharedContext.release();
            }
        }
    }

    private static boolean updateGraphicsConfigurationARB(final WindowsWGLDrawableFactory factory,
                                                          final WindowsWGLGraphicsConfiguration config, final CapabilitiesChooser chooser,
                                                          final long hdc, final boolean extHDC, int[] pformats) {
        final AbstractGraphicsDevice device = config.getScreen().getDevice();
        final WindowsWGLDrawableFactory.SharedResource sharedResource = factory.getOrCreateSharedResourceImpl(device);

        if (null == sharedResource) {
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationARB: SharedResource is null: "+device);
            }
            return false;
        }
        if (!sharedResource.hasARBPixelFormat()) {
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationARB: "+WindowsWGLDrawableFactory.WGL_ARB_pixel_format+" not available");
            }
            return false;
        }

        final GLCapabilitiesImmutable capsChosen = (GLCapabilitiesImmutable) config.getChosenCapabilities();
        final boolean isOpaque = capsChosen.isBackgroundOpaque() && GDIUtil.DwmIsCompositionEnabled();
        final int winattrbits = GLGraphicsConfigurationUtil.getExclusiveWinAttributeBits(capsChosen)
                                & ~GLGraphicsConfigurationUtil.BITMAP_BIT; // w/o BITMAP
        final GLProfile glProfile = capsChosen.getGLProfile();

        final int pfdIDCount = WindowsWGLGraphicsConfiguration.wglARBPFDIDCount((WindowsWGLContext)sharedResource.getContext(), hdc);

        if(DEBUG) {
            System.err.println("updateGraphicsConfigurationARB: hdc "+toHexString(hdc)+", pfdIDCount(hdc) "+pfdIDCount+", capsChosen "+capsChosen+", "+GLGraphicsConfigurationUtil.winAttributeBits2String(null, winattrbits).toString());
            System.err.println("\tisOpaque "+isOpaque+" (translucency requested: "+(!capsChosen.isBackgroundOpaque())+", compositioning enabled: "+GDIUtil.DwmIsCompositionEnabled()+")");
            final int pformatsNum = null != pformats ? pformats.length : -1;
            System.err.println("\textHDC "+extHDC+", chooser "+(null!=chooser)+", pformatsNum "+pformatsNum);
        }

        if(0 >= pfdIDCount) {
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationARB: failed due to 0 pfdIDs for hdc "+toHexString(hdc)+" - hdc incompatible w/ ARB ext.");
            }
            return false;
        }

        WGLGLCapabilities pixelFormatCaps = null; // chosen or preset PFD ID's caps
        boolean pixelFormatSet = false; // indicates a preset PFD ID [caps]
        final int presetPFDID = extHDC ? -1 : WGLUtil.GetPixelFormat(hdc) ;
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
            pixelFormatCaps = WindowsWGLGraphicsConfiguration.wglARBPFID2GLCapabilities(sharedResource, device, glProfile,
                                                                                        hdc, presetPFDID, winattrbits);
            pixelFormatCaps = (WGLGLCapabilities) GLGraphicsConfigurationUtil.fixOpaqueGLCapabilities(pixelFormatCaps, isOpaque);
        } else {
            int recommendedIndex = -1; // recommended index
            if(null == pformats) {
                // No given PFD IDs
                //
                // 1st choice: get GLCapabilities based on users GLCapabilities setting recommendedIndex as preferred choice
                final IntBuffer iattributes = Buffers.newDirectIntBuffer(2*WindowsWGLGraphicsConfiguration.MAX_ATTRIBS);
                final FloatBuffer fattributes = Buffers.newDirectFloatBuffer(1);
                int accelerationMode = WGLExt.WGL_FULL_ACCELERATION_ARB;
                pformats = WindowsWGLGraphicsConfiguration.wglChoosePixelFormatARB(sharedResource, device, capsChosen,
                                                                                   hdc, iattributes, accelerationMode, fattributes);
                if (null == pformats) {
                    accelerationMode = WGLExt.WGL_GENERIC_ACCELERATION_ARB;
                    pformats = WindowsWGLGraphicsConfiguration.wglChoosePixelFormatARB(sharedResource, device, capsChosen,
                                                                                       hdc, iattributes, accelerationMode, fattributes);
                }
                if (null == pformats) {
                    accelerationMode = -1; // use what we are offered ..
                    pformats = WindowsWGLGraphicsConfiguration.wglChoosePixelFormatARB(sharedResource, device, capsChosen,
                                                                                       hdc, iattributes, accelerationMode, fattributes);
                }
                if (null != pformats) {
                    recommendedIndex = 0;
                } else {
                    if(DEBUG) {
                        System.err.println("updateGraphicsConfigurationARB: wglChoosePixelFormatARB failed with: "+capsChosen);
                    }
                    // 2nd choice: get all GLCapabilities available, no preferred recommendedIndex available
                    pformats = WindowsWGLGraphicsConfiguration.wglAllARBPFDIDs(pfdIDCount);
                    if (DEBUG) {
                        final int len = ( null != pformats ) ? pformats.length : 0;
                        System.err.println("updateGraphicsConfigurationARB: NumFormats (wglAllARBPFIDs) " + len);
                    }
                }
                if (null == pformats) {
                    if (DEBUG) {
                        System.err.println("updateGraphicsConfigurationARB: failed, return false");
                        ExceptionUtils.dumpStack(System.err);
                    }
                    return false;
                }
            }
            final boolean skipCapsChooser = 0 <= recommendedIndex && null == chooser && capsChosen.isBackgroundOpaque(); // fast path: skip choosing if using recommended idx and null chooser is used and if not translucent

            final List<GLCapabilitiesImmutable> availableCaps =
                    WindowsWGLGraphicsConfiguration.wglARBPFIDs2GLCapabilities(sharedResource, device, glProfile,
                                                                               hdc, pformats, winattrbits, skipCapsChooser /* onlyFirstValid */);

            if( null == availableCaps || 0 == availableCaps.size() ) {
                if (DEBUG) {
                    System.err.println("updateGraphicsConfigurationARB: wglARBPFIDs2GLCapabilities failed with " + pformats.length + " pfd ids");
                    ExceptionUtils.dumpStack(System.err);
                }
                return false;
            }

            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationARB: " + pformats.length +
                                   " pfd ids, skipCapsChooser " + skipCapsChooser + ", " + GLGraphicsConfigurationUtil.winAttributeBits2String(null, winattrbits).toString() + ", " + availableCaps.size() + " glcaps");
                if(0 <= recommendedIndex) {
                    System.err.println("updateGraphicsConfigurationARB: Used wglChoosePixelFormatARB to recommend pixel format " +
                                       pformats[recommendedIndex] + ", idx " + recommendedIndex +", "+availableCaps.get(recommendedIndex));
                }
            }

            final int chosenIndex;
            if( skipCapsChooser ) {
                chosenIndex = recommendedIndex;
            } else {
                chosenIndex = chooseCapabilities(chooser, capsChosen, availableCaps, recommendedIndex);
            }
            if ( 0 > chosenIndex ) {
                if (DEBUG) {
                    ExceptionUtils.dumpStack(System.err);
                }
                return false;
            }
            pixelFormatCaps = (WGLGLCapabilities) availableCaps.get(chosenIndex);
            if( null == pixelFormatCaps) {
                throw new GLException("Null Capabilities with "+
                                      " chosen pfdID: native recommended "+ (recommendedIndex+1) +
                                      " chosen idx "+chosenIndex+", skipCapsChooser "+skipCapsChooser);
            }
            pixelFormatCaps = (WGLGLCapabilities) GLGraphicsConfigurationUtil.fixOpaqueGLCapabilities(pixelFormatCaps, isOpaque);
            if (DEBUG) {
                System.err.println("chosen pfdID (ARB): native recommended "+ (recommendedIndex+1) +
                                   " chosen "+pixelFormatCaps+", skipCapsChooser "+skipCapsChooser);
            }
        }

        if ( !extHDC && !pixelFormatSet ) {
            config.setPixelFormat(hdc, pixelFormatCaps);
        } else {
            config.setCapsPFD(pixelFormatCaps);
        }
        return true;
    }

    private static boolean updateGraphicsConfigurationGDI(final WindowsWGLGraphicsConfiguration config, final CapabilitiesChooser chooser,
                                                          final long hdc, final boolean extHDC, int[] pformats) {
        final GLCapabilitiesImmutable capsChosen = (GLCapabilitiesImmutable) config.getChosenCapabilities();
        if( !capsChosen.isOnscreen() && capsChosen.isPBuffer() ) {
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationGDI: no pbuffer supported on GDI: " + capsChosen);
            }
            return false;
        }
        // final boolean onscreen = capsChosen.isOnscreen();
        // final boolean useFBO = capsChosen.isFBO();
        final GLProfile glProfile = capsChosen.getGLProfile();
        final int winattrmask = GLGraphicsConfigurationUtil.getExclusiveWinAttributeBits(capsChosen);

        if(DEBUG) {
            System.err.println("updateGraphicsConfigurationGDI: hdc "+toHexString(hdc)+", capsChosen "+capsChosen+", "+GLGraphicsConfigurationUtil.winAttributeBits2String(null, winattrmask).toString());
            final int pformatsNum = null != pformats ? pformats.length : -1;
            System.err.println("\textHDC "+extHDC+", chooser "+(null!=chooser)+", pformatsNum "+pformatsNum);
        }

        final AbstractGraphicsDevice device = config.getScreen().getDevice();
        WGLGLCapabilities pixelFormatCaps = null; // chosen or preset PFD ID's caps
        boolean pixelFormatSet = false; // indicates a preset PFD ID [caps]
        final int presetPFDID = extHDC ? -1 : WGLUtil.GetPixelFormat(hdc) ;
        if ( 1 <= presetPFDID ) {
            // Pixelformat already set by either
            //  - a previous preselectGraphicsConfiguration() call on the same HDC,
            //  - the graphics driver, copying the HDC's pixelformat to the new one,
            //  - or the Java2D/OpenGL pipeline's configuration
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationGDI: NOTE: pixel format already chosen for HDC: " + toHexString(hdc)
                        + ", pixelformat " + presetPFDID);
            }
            pixelFormatSet = true;
            pixelFormatCaps = WindowsWGLGraphicsConfiguration.PFD2GLCapabilities(device, glProfile, hdc, presetPFDID, winattrmask);
            if(null == pixelFormatCaps) {
                throw new GLException("Could not map PFD2GLCaps w/ already chosen pfdID "+presetPFDID);
            }
        } else {
            final boolean givenPFormats = null != pformats;
            if( !givenPFormats ) {
                pformats = WindowsWGLGraphicsConfiguration.wglAllGDIPFIDs(hdc);
            }

            // 1st choice: get GLCapabilities based on users GLCapabilities setting recommendedIndex as preferred choice
            final List<GLCapabilitiesImmutable> availableCaps = new ArrayList<GLCapabilitiesImmutable>();
            PIXELFORMATDESCRIPTOR pfd = WindowsWGLGraphicsConfiguration.createPixelFormatDescriptor();
            pfd = WindowsWGLGraphicsConfiguration.GLCapabilities2PFD(capsChosen, pfd);
            int chosenPFDID = WGLUtil.ChoosePixelFormat(hdc, pfd);
            int recommendedIndex = -1 ;
            final boolean skipCapsChooser;
            if( 1 <= chosenPFDID ) {
                // _skipCapsChooser: fast path: skip choosing if using recommended idx and null chooser is used and if not translucent
                final boolean _skipCapsChooser = null == chooser && capsChosen.isBackgroundOpaque();
                // seek index .. in all formats _or_ in given formats!
                int chosenIdx;
                for (chosenIdx = pformats.length - 1 ; 0 <= chosenIdx && chosenPFDID != pformats[chosenIdx]; chosenIdx--) { /* nop */ }
                if( 0 <= chosenIdx ) {
                    if( _skipCapsChooser ) {
                        final WGLGLCapabilities caps = WindowsWGLGraphicsConfiguration.PFD2GLCapabilities(device, glProfile, hdc,
                                                                                                          chosenPFDID, winattrmask);
                        if(null != caps) {
                            availableCaps.add(caps);
                            recommendedIndex = 0;
                            skipCapsChooser = true;
                        } else {
                        	skipCapsChooser = false;
                        }
                    } else {
                        skipCapsChooser = false;
                    }
                    if( DEBUG ) {
                        System.err.println("Chosen PFDID "+chosenPFDID+" (idx "+chosenIdx+") -> recommendedIndex "+recommendedIndex+", skipCapsChooser "+skipCapsChooser);
                    }
                } else {
                    if(DEBUG) {
                        final GLCapabilitiesImmutable reqPFDCaps = WindowsWGLGraphicsConfiguration.PFD2GLCapabilitiesNoCheck(device, glProfile, pfd, chosenPFDID);
                        final GLCapabilitiesImmutable chosenCaps = WindowsWGLGraphicsConfiguration.PFD2GLCapabilities(device, glProfile, hdc, chosenPFDID, winattrmask);
                        System.err.println("Chosen PFDID "+chosenPFDID+" (idx "+chosenIdx+"), but not found in available caps (use given pfdIDs "+givenPFormats+", reqPFDCaps "+reqPFDCaps+", chosenCaps: "+chosenCaps);
                    }
                    chosenPFDID = 0; // not found in pformats -> clear
                    skipCapsChooser = false;
                }
            } else {
                skipCapsChooser = false;
            }
            if (DEBUG) {
                System.err.println("updateGraphicsConfigurationGDI: ChoosePixelFormat(HDC " + toHexString(hdc) + ") = pfdID " + chosenPFDID + ", skipCapsChooser "+skipCapsChooser+", idx " + recommendedIndex + " (LastError: " + GDI.GetLastError() + ")");
            }

            if( !skipCapsChooser ) {
                for (int i = 0; i < pformats.length; i++) {
                    final int pfdid = pformats[i];
                    final WGLGLCapabilities caps = WindowsWGLGraphicsConfiguration.PFD2GLCapabilities(device, glProfile,
                                                                                                      hdc, pfdid, winattrmask);
                    if(null != caps) {
                        availableCaps.add(caps);
                        if(DEBUG) {
                            final int j = availableCaps.size() - 1;
                            System.err.println("updateGraphicsConfigurationGDI: availableCaps["+i+" -> "+j+"]: "+caps);
                        }
                    } else if(DEBUG) {
                        final GLCapabilitiesImmutable skipped = WindowsWGLGraphicsConfiguration.PFD2GLCapabilitiesNoCheck(device, glProfile, hdc, pformats[i]);
                        System.err.println("updateGraphicsConfigurationGDI: availableCaps["+i+" -> skip]: pfdID "+pformats[i]+", "+skipped);
                    }
                }
                // seek recommendedIndex in all _or_ given formats!
                if( 1 <= chosenPFDID && 0 > recommendedIndex) {
                    for (recommendedIndex = availableCaps.size() - 1 ;
                         0 <= recommendedIndex && chosenPFDID != ((WGLGLCapabilities) availableCaps.get(recommendedIndex)).getPFDID();
                         recommendedIndex--)
                    { /* nop */ }
                }
            }

            // 2nd choice: if no preferred recommendedIndex available
            final int chosenIndex;
            if( skipCapsChooser ) {
                chosenIndex = recommendedIndex;
            } else {
                chosenIndex = chooseCapabilities(chooser, capsChosen, availableCaps, recommendedIndex);
            }
            if ( 0 > chosenIndex ) {
                if (DEBUG) {
                    System.err.println("updateGraphicsConfigurationGDI: failed, return false");
                    ExceptionUtils.dumpStack(System.err);
                }
                return false;
            }
            pixelFormatCaps = (WGLGLCapabilities) availableCaps.get(chosenIndex);
            if (DEBUG) {
                System.err.println("chosen pfdID (GDI): recommendedIndex "+recommendedIndex+" -> chosenIndex "+ chosenIndex + ", skipCapsChooser "+skipCapsChooser+", caps " + pixelFormatCaps +
                                   " (" + WGLGLCapabilities.PFD2String(pixelFormatCaps.getPFD(), pixelFormatCaps.getPFDID()) +")");
            }
        }

        if ( !extHDC && !pixelFormatSet ) {
            config.setPixelFormat(hdc, pixelFormatCaps);
        } else {
            config.setCapsPFD(pixelFormatCaps);
        }
        return true;
    }
}

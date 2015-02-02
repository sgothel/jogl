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

package jogamp.opengl.x11.glx;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.CapabilitiesChooser;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.VisualIDHolder;
import com.jogamp.nativewindow.VisualIDHolder.VIDType;
import com.jogamp.opengl.DefaultGLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsScreen;

import jogamp.nativewindow.x11.X11Lib;
import jogamp.nativewindow.x11.XVisualInfo;
import jogamp.opengl.GLGraphicsConfigurationFactory;
import jogamp.opengl.GLGraphicsConfigurationUtil;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/** Subclass of GraphicsConfigurationFactory used when non-AWT toolkits
    are used on X11 platforms. Toolkits will likely need to delegate
    to this one to change the accepted and returned types of the
    GraphicsDevice and GraphicsConfiguration abstractions. */

public class X11GLXGraphicsConfigurationFactory extends GLGraphicsConfigurationFactory {
    static VisualIDHolder.VIDComparator XVisualIDComparator = new VisualIDHolder.VIDComparator(VisualIDHolder.VIDType.X11_XVISUAL);

    static GraphicsConfigurationFactory fallbackX11GraphicsConfigurationFactory = null;
    static void registerFactory() {
        final GraphicsConfigurationFactory newFactory = new X11GLXGraphicsConfigurationFactory();
        final GraphicsConfigurationFactory oldFactory = GraphicsConfigurationFactory.registerFactory(com.jogamp.nativewindow.x11.X11GraphicsDevice.class, GLCapabilitiesImmutable.class, newFactory);
        if(oldFactory == newFactory) {
            throw new InternalError("GraphicsConfigurationFactory lifecycle impl. error");
        }
        if(null != oldFactory) {
            fallbackX11GraphicsConfigurationFactory = oldFactory;
        } else {
            fallbackX11GraphicsConfigurationFactory = GraphicsConfigurationFactory.getFactory(com.jogamp.nativewindow.x11.X11GraphicsDevice.class, CapabilitiesImmutable.class);
            if( null == fallbackX11GraphicsConfigurationFactory ) {
                throw new InternalError("Missing fallback GraphicsConfigurationFactory");
            }
        }
    }
    private X11GLXGraphicsConfigurationFactory() {
    }

    @Override
    protected AbstractGraphicsConfiguration chooseGraphicsConfigurationImpl(
            final CapabilitiesImmutable capsChosen, final CapabilitiesImmutable capsRequested, final CapabilitiesChooser chooser, final AbstractGraphicsScreen absScreen, final int nativeVisualID) {
        if (!(absScreen instanceof X11GraphicsScreen)) {
            throw new IllegalArgumentException("Only X11GraphicsScreen are allowed here");
        }

        if ( !(capsChosen instanceof GLCapabilitiesImmutable) ) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects - chosen");
        }

        if ( !(capsRequested instanceof GLCapabilitiesImmutable)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects - requested");
        }

        if (chooser != null && !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilitiesChooser objects");
        }

        if(!GLXUtil.isGLXAvailableOnServer((X11GraphicsDevice)absScreen.getDevice())) {
            if(null != fallbackX11GraphicsConfigurationFactory) {
                if(DEBUG) {
                    System.err.println("No GLX available, fallback to "+fallbackX11GraphicsConfigurationFactory.getClass().getSimpleName()+" for: "+absScreen);
                }
                return fallbackX11GraphicsConfigurationFactory.chooseGraphicsConfiguration(capsChosen, capsRequested, chooser, absScreen, VisualIDHolder.VID_UNDEFINED);
            }
            throw new InternalError("No GLX and no fallback GraphicsConfigurationFactory available for: "+absScreen);
        }
        return chooseGraphicsConfigurationStatic((GLCapabilitiesImmutable)capsChosen, (GLCapabilitiesImmutable)capsRequested,
                                                 (GLCapabilitiesChooser)chooser, (X11GraphicsScreen)absScreen, nativeVisualID);
    }

    protected static List<GLCapabilitiesImmutable> getAvailableCapabilities(final X11GLXDrawableFactory factory, final AbstractGraphicsDevice device) {
        final X11GLXDrawableFactory.SharedResource sharedResource = factory.getOrCreateSharedResourceImpl(device);
        if(null == sharedResource) {
            throw new GLException("Shared resource for device n/a: "+device);
        }
        final X11GraphicsScreen sharedScreen = (X11GraphicsScreen) sharedResource.getScreen();
        final X11GraphicsDevice sharedDevice = (X11GraphicsDevice) sharedScreen.getDevice();
        final boolean isMultisampleAvailable = sharedResource.isGLXMultisampleAvailable();
        final GLProfile glp = GLProfile.getDefault(device);

        List<GLCapabilitiesImmutable> availableCaps = null;

        sharedDevice.lock();
        try {
            if( sharedResource.isGLXVersionGreaterEqualOneThree() ) {
                availableCaps = getAvailableGLCapabilitiesFBConfig(sharedScreen, glp, isMultisampleAvailable);
            }
            if( null == availableCaps || availableCaps.isEmpty() ) {
                availableCaps = getAvailableGLCapabilitiesXVisual(sharedScreen, glp, isMultisampleAvailable);
            }
        } finally {
            sharedDevice.unlock();
        }
        if( null != availableCaps && availableCaps.size() > 1 ) {
            Collections.sort(availableCaps, XVisualIDComparator);
        }
        return availableCaps;
    }

    static List<GLCapabilitiesImmutable> getAvailableGLCapabilitiesFBConfig(final X11GraphicsScreen x11Screen, final GLProfile glProfile, final boolean isMultisampleAvailable) {
        PointerBuffer fbcfgsL = null;

        // Utilizing FBConfig
        //
        final X11GraphicsDevice absDevice = (X11GraphicsDevice) x11Screen.getDevice();
        final long display = absDevice.getHandle();

        final int screen = x11Screen.getIndex();
        final IntBuffer count = Buffers.newDirectIntBuffer(1);
        count.put(0, -1);
        final ArrayList<GLCapabilitiesImmutable> availableCaps = new ArrayList<GLCapabilitiesImmutable>();

        fbcfgsL = GLX.glXChooseFBConfig(display, screen, null, count);
        if (fbcfgsL == null || fbcfgsL.limit()<=0) {
            if(DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.getAvailableGLCapabilitiesFBConfig: Failed glXChooseFBConfig ("+x11Screen+"): "+fbcfgsL+", "+count.get(0));
            }
            return null;
        }
        for (int i = 0; i < fbcfgsL.limit(); i++) {
            final GLCapabilitiesImmutable caps = X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities(absDevice, glProfile, fbcfgsL.get(i), GLGraphicsConfigurationUtil.ALL_BITS, isMultisampleAvailable);
            if(null != caps) {
                availableCaps.add(caps);
            } else if(DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.getAvailableGLCapabilitiesFBConfig: FBConfig invalid (2): ("+x11Screen+"): fbcfg: "+toHexString(fbcfgsL.get(i)));
            }
        }
        return availableCaps;
    }

    static List<GLCapabilitiesImmutable> getAvailableGLCapabilitiesXVisual(final X11GraphicsScreen x11Screen, final GLProfile glProfile, final boolean isMultisampleAvailable) {
        final X11GraphicsDevice absDevice = (X11GraphicsDevice) x11Screen.getDevice();
        final long display = absDevice.getHandle();

        final int screen = x11Screen.getIndex();

        final int[] count = new int[1];
        final XVisualInfo template = XVisualInfo.create();
        template.setScreen(screen);
        final XVisualInfo[] infos = X11Lib.XGetVisualInfo(display, X11Lib.VisualScreenMask, template, count, 0);
        if (infos == null || infos.length<1) {
            throw new GLException("Error while enumerating available XVisualInfos");
        }
        final ArrayList<GLCapabilitiesImmutable> availableCaps = new ArrayList<GLCapabilitiesImmutable>();
        for (int i = 0; i < infos.length; i++) {
            final GLCapabilitiesImmutable caps = X11GLXGraphicsConfiguration.XVisualInfo2GLCapabilities(absDevice, glProfile, infos[i], GLGraphicsConfigurationUtil.ALL_BITS, isMultisampleAvailable);
            if(null != caps) {
                availableCaps.add(caps);
            } if(DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.getAvailableGLCapabilitiesXVisual: XVisual invalid: ("+x11Screen+"): fbcfg: "+toHexString(infos[i].getVisualid()));
            }
        }
        return availableCaps;
    }


    static X11GLXGraphicsConfiguration chooseGraphicsConfigurationStatic(GLCapabilitiesImmutable capsChosen,
                                                                         final GLCapabilitiesImmutable capsReq,
                                                                         final GLCapabilitiesChooser chooser,
                                                                         final X11GraphicsScreen x11Screen, final int xvisualID) {
        if (x11Screen == null) {
            throw new IllegalArgumentException("AbstractGraphicsScreen is null");
        }
        if (capsChosen == null) {
            capsChosen = new GLCapabilities(null);
        }
        final X11GraphicsDevice x11Device = (X11GraphicsDevice) x11Screen.getDevice();
        final X11GLXDrawableFactory factory = (X11GLXDrawableFactory) GLDrawableFactory.getDesktopFactory();

        capsChosen = GLGraphicsConfigurationUtil.fixGLCapabilities( capsChosen, factory, x11Device);
        final boolean usePBuffer = !capsChosen.isOnscreen() && capsChosen.isPBuffer();

        X11GLXGraphicsConfiguration res = null;
        x11Device.lock();
        try {
            if( factory.isGLXVersionGreaterEqualOneThree(x11Device) ) {
                res = chooseGraphicsConfigurationFBConfig(capsChosen, capsReq, chooser, x11Screen, xvisualID);
            }
            if(null==res) {
                if(usePBuffer) {
                    throw new GLException("Error: Couldn't create X11GLXGraphicsConfiguration based on FBConfig for visualID "+toHexString(xvisualID)+", "+capsChosen);
                }
                res = chooseGraphicsConfigurationXVisual(capsChosen, capsReq, chooser, x11Screen, xvisualID);
            }
        } finally {
            x11Device.unlock();
        }
        if(null==res) {
            throw new GLException("Error: Couldn't create X11GLXGraphicsConfiguration based on FBConfig and XVisual for visualID "+toHexString(xvisualID)+", "+x11Screen+", "+capsChosen);
        }
        if(DEBUG) {
            System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationStatic(visualID "+toHexString(xvisualID)+", "+x11Screen+","+capsChosen+"): "+res);
        }
        return res;
    }

    static X11GLXGraphicsConfiguration fetchGraphicsConfigurationFBConfig(final X11GraphicsScreen x11Screen, final int fbID, final GLProfile glp) {
        final X11GraphicsDevice x11Device = (X11GraphicsDevice) x11Screen.getDevice();
        final long display = x11Device.getHandle();
        final int screen = x11Screen.getIndex();

        final long fbcfg = X11GLXGraphicsConfiguration.glXFBConfigID2FBConfig(display, screen, fbID);
        if( 0 == fbcfg || !X11GLXGraphicsConfiguration.GLXFBConfigValid( display, fbcfg ) ) {
            if(DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: Failed.0 - GLX FBConfig invalid: ("+x11Screen+","+toHexString(fbID)+"): fbcfg: "+toHexString(fbcfg));
            }
            return null;
        }
        final X11GLXDrawableFactory factory = (X11GLXDrawableFactory) GLDrawableFactory.getDesktopFactory();

        final X11GLCapabilities caps = X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities(x11Device, glp, fbcfg, GLGraphicsConfigurationUtil.ALL_BITS, factory.isGLXMultisampleAvailable(x11Device));
        if(null==caps) {
            if(DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: Failed.1 - GLX FBConfig invalid: ("+x11Screen+","+toHexString(fbID)+"): fbcfg: "+toHexString(fbcfg));
            }
            return null;
        }
        return new X11GLXGraphicsConfiguration(x11Screen, caps, caps, new DefaultGLCapabilitiesChooser());
    }

    private static X11GLXGraphicsConfiguration chooseGraphicsConfigurationFBConfig(final GLCapabilitiesImmutable capsChosen,
                                                                                   final GLCapabilitiesImmutable capsReq,
                                                                                   final GLCapabilitiesChooser chooser,
                                                                                   final X11GraphicsScreen x11Screen, final int xvisualID) {
        int recommendedIndex = -1;
        PointerBuffer fbcfgsL = null;
        final GLProfile glProfile = capsChosen.getGLProfile();

        // Utilizing FBConfig
        //
        final X11GraphicsDevice x11Device = (X11GraphicsDevice) x11Screen.getDevice();
        final long display = x11Device.getHandle();
        final int screen = x11Screen.getIndex();

        final X11GLXDrawableFactory factory = (X11GLXDrawableFactory) GLDrawableFactory.getDesktopFactory();
        final boolean isMultisampleAvailable = factory.isGLXMultisampleAvailable(x11Device);
        final IntBuffer attribs = X11GLXGraphicsConfiguration.GLCapabilities2AttribList(capsChosen, true, isMultisampleAvailable, display, screen);
        final IntBuffer count = Buffers.newDirectIntBuffer(1);
        count.put(0, -1);
        final int winattrmask = GLGraphicsConfigurationUtil.getExclusiveWinAttributeBits(capsChosen);
        List<GLCapabilitiesImmutable> availableCaps;
        // 1st choice: get GLCapabilities based on users GLCapabilities setting recommendedIndex as preferred choice,
        // skipped if xvisualID is given
        final boolean hasGLXChosenCaps;
        if( VisualIDHolder.VID_UNDEFINED == xvisualID ) {
            fbcfgsL = GLX.glXChooseFBConfig(display, screen, attribs, count);
            hasGLXChosenCaps = fbcfgsL != null && fbcfgsL.limit()>0;
        } else {
            hasGLXChosenCaps = false;
        }
        final boolean useRecommendedIndex = hasGLXChosenCaps && capsChosen.isBackgroundOpaque(); // only use recommended idx if not translucent
        final boolean skipCapsChooser = null == chooser && useRecommendedIndex; // fast path: skip choosing if using recommended idx and null chooser is used
        if (hasGLXChosenCaps) {
            availableCaps = X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities(x11Device, glProfile, fbcfgsL, winattrmask, isMultisampleAvailable, skipCapsChooser /* onlyFirstValid */);
            if(availableCaps.size() > 0) {
                recommendedIndex = useRecommendedIndex ? 0 : -1;
                if (DEBUG) {
                    System.err.println("glXChooseFBConfig recommended fbcfg " + toHexString(fbcfgsL.get(0)) + ", idx " + recommendedIndex);
                    System.err.println("useRecommendedIndex "+useRecommendedIndex+", skipCapsChooser "+skipCapsChooser);
                    System.err.println("user  caps " + capsChosen);
                    System.err.println("fbcfg caps " + fbcfgsL.limit()+", availCaps "+availableCaps.get(0));
                }
            } else if (DEBUG) {
                System.err.println("glXChooseFBConfig no caps for recommended fbcfg " + toHexString(fbcfgsL.get(0)));
                System.err.println("useRecommendedIndex "+useRecommendedIndex+", skipCapsChooser "+skipCapsChooser);
                System.err.println("user  caps " + capsChosen);
            }
        } else {
            availableCaps = new ArrayList<GLCapabilitiesImmutable>();
        }

        // 2nd choice: get all GLCapabilities available, no preferred recommendedIndex available
        if( 0 == availableCaps.size() ) {
            // reset ..
            recommendedIndex = -1;

            fbcfgsL = GLX.glXChooseFBConfig(display, screen, null, count);
            if (fbcfgsL == null || fbcfgsL.limit()<=0) {
                if(DEBUG) {
                    System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: Failed glXChooseFBConfig ("+x11Screen+","+capsChosen+"): "+fbcfgsL+", "+count.get(0));
                }
                return null;
            }
            availableCaps = X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities(x11Device, glProfile, fbcfgsL, winattrmask, isMultisampleAvailable, false /* onlyOneValid */);
        }

        if(DEBUG) {
            System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: got configs: "+availableCaps.size());
            for(int i=0; i<availableCaps.size(); i++) {
                System.err.println(i+": "+availableCaps.get(i));
            }
        }

        if( VisualIDHolder.VID_UNDEFINED != xvisualID ) { // implies !hasGLXChosenCaps
            for(int i=0; i<availableCaps.size(); ) {
                final VisualIDHolder vidh = availableCaps.get(i);
                if(vidh.getVisualID(VIDType.X11_XVISUAL) != xvisualID ) {
                    availableCaps.remove(i);
                } else {
                    i++;
                }
            }
            if(0==availableCaps.size()) {
                if(DEBUG) {
                    System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: post filter visualID "+toHexString(xvisualID )+" no config found, failed - return null");
                }
                return null;
            } else if(DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: post filter visualID "+toHexString(xvisualID)+" got configs: "+availableCaps.size());
            }
        }

        final int chosenIndex;
        if( skipCapsChooser && 0 <= recommendedIndex ) {
            chosenIndex = recommendedIndex;
        } else {
            chosenIndex = chooseCapabilities(chooser, capsChosen, availableCaps, recommendedIndex);
        }
        if ( 0 > chosenIndex ) {
            if (DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: failed, return null");
                ExceptionUtils.dumpStack(System.err);
            }
            return null;
        }
        final X11GLCapabilities chosenCaps = (X11GLCapabilities) availableCaps.get(chosenIndex);

        return new X11GLXGraphicsConfiguration(x11Screen, chosenCaps, capsReq, chooser);
    }

    private static X11GLXGraphicsConfiguration chooseGraphicsConfigurationXVisual(final GLCapabilitiesImmutable capsChosen,
                                                                                  final GLCapabilitiesImmutable capsReq,
                                                                                  GLCapabilitiesChooser chooser,
                                                                                  final X11GraphicsScreen x11Screen, final int xvisualID) {
        if (chooser == null) {
            chooser = new DefaultGLCapabilitiesChooser();
        }

        final GLProfile glProfile = capsChosen.getGLProfile();
        final int winattrmask = GLGraphicsConfigurationUtil.getExclusiveWinAttributeBits(capsChosen.isOnscreen(), capsChosen.isFBO(), false /* pbuffer */, capsChosen.isBitmap());
        final List<GLCapabilitiesImmutable> availableCaps = new ArrayList<GLCapabilitiesImmutable>();
        int recommendedIndex = -1;

        final X11GraphicsDevice absDevice = (X11GraphicsDevice) x11Screen.getDevice();
        final long display = absDevice.getHandle();
        final int screen = x11Screen.getIndex();

        final X11GLXDrawableFactory factory = (X11GLXDrawableFactory) GLDrawableFactory.getDesktopFactory();
        final boolean isMultisampleAvailable = factory.isGLXMultisampleAvailable(absDevice);
        final IntBuffer attribs = X11GLXGraphicsConfiguration.GLCapabilities2AttribList(capsChosen, false, isMultisampleAvailable, display, screen);

        XVisualInfo recommendedVis = null;
        // 1st choice: get GLCapabilities based on users GLCapabilities setting recommendedIndex as preferred choice
        // skipped if xvisualID is given
        if( VisualIDHolder.VID_UNDEFINED == xvisualID ) {
            recommendedVis = GLX.glXChooseVisual(display, screen, attribs);
            if (DEBUG) {
                System.err.print("glXChooseVisual recommended ");
                if (recommendedVis == null) {
                    System.err.println("null visual");
                } else {
                    System.err.println("visual id " + toHexString(recommendedVis.getVisualid()));
                }
            }
        }

        // 2nd choice: get all GLCapabilities available, preferred recommendedIndex might be available if 1st choice was successful
        final int[] count = new int[1];
        final XVisualInfo template = XVisualInfo.create();
        template.setScreen(screen);
        final XVisualInfo[] infos = X11Lib.XGetVisualInfo(display, X11Lib.VisualScreenMask, template, count, 0);
        if (infos == null || infos.length<1) {
            throw new GLException("Error while enumerating available XVisualInfos");
        }

        for (int i = 0; i < infos.length; i++) {
            final GLCapabilitiesImmutable caps = X11GLXGraphicsConfiguration.XVisualInfo2GLCapabilities(absDevice, glProfile, infos[i], winattrmask, isMultisampleAvailable);
            if( null != caps ) {
                availableCaps.add(caps);
                // Attempt to find the visual chosenIndex by glXChooseVisual, if not translucent
                if (capsChosen.isBackgroundOpaque() && recommendedVis != null && recommendedVis.getVisualid() == infos[i].getVisualid()) {
                    recommendedIndex = availableCaps.size() - 1;
                }
            } else if(DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationXVisual: XVisual invalid: ("+x11Screen+"): fbcfg: "+toHexString(infos[i].getVisualid()));
            }
        }

        if(DEBUG) {
            System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationXVisual: got configs: "+availableCaps.size());
            for(int i=0; i<availableCaps.size(); i++) {
                System.err.println(i+": "+availableCaps.get(i));
            }
        }

        if( VisualIDHolder.VID_UNDEFINED != xvisualID ) {
            for(int i=0; i<availableCaps.size(); ) {
                final VisualIDHolder vidh = availableCaps.get(i);
                if(vidh.getVisualID(VIDType.X11_XVISUAL) != xvisualID ) {
                    availableCaps.remove(i);
                } else {
                    i++;
                }
            }
            if(0==availableCaps.size()) {
                if(DEBUG) {
                    System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationXVisual: post filter visualID "+toHexString(xvisualID )+" no config found, failed - return null");
                }
                return null;
            } else if(DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationXVisual: post filter visualID "+toHexString(xvisualID)+" got configs: "+availableCaps.size());
            }
        }

        final int chosenIndex = chooseCapabilities(chooser, capsChosen, availableCaps, recommendedIndex);
        if ( 0 > chosenIndex ) {
            if (DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationXVisual: failed, return null");
                ExceptionUtils.dumpStack(System.err);
            }
            return null;
        }
        final X11GLCapabilities chosenCaps = (X11GLCapabilities) availableCaps.get(chosenIndex);

        return new X11GLXGraphicsConfiguration(x11Screen, chosenCaps, capsReq, chooser);
    }

}


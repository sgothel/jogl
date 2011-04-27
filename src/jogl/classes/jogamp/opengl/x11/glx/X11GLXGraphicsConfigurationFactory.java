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

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.CapabilitiesChooser;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.x11.X11GraphicsScreen;
import javax.media.nativewindow.x11.X11GraphicsDevice;
import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.jogamp.common.nio.PointerBuffer;
import jogamp.nativewindow.x11.X11Lib;
import jogamp.nativewindow.x11.X11Util;
import jogamp.nativewindow.x11.XVisualInfo;
import jogamp.opengl.Debug;
import jogamp.opengl.GLGraphicsConfigurationFactory;
import jogamp.opengl.GLGraphicsConfigurationUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/** Subclass of GraphicsConfigurationFactory used when non-AWT toolkits
    are used on X11 platforms. Toolkits will likely need to delegate
    to this one to change the accepted and returned types of the
    GraphicsDevice and GraphicsConfiguration abstractions. */

public class X11GLXGraphicsConfigurationFactory extends GLGraphicsConfigurationFactory {
    protected static final boolean DEBUG = Debug.debug("GraphicsConfiguration");
    static X11GLCapabilities.XVisualIDComparator XVisualIDComparator = new X11GLCapabilities.XVisualIDComparator();

    X11GLXGraphicsConfigurationFactory() {
        GraphicsConfigurationFactory.registerFactory(javax.media.nativewindow.x11.X11GraphicsDevice.class, this);
    }

    protected AbstractGraphicsConfiguration chooseGraphicsConfigurationImpl(
            CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested, CapabilitiesChooser chooser, AbstractGraphicsScreen absScreen) {
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
        return chooseGraphicsConfigurationStatic((GLCapabilitiesImmutable)capsChosen, (GLCapabilitiesImmutable)capsRequested,
                                                 (GLCapabilitiesChooser)chooser, (X11GraphicsScreen)absScreen);
    }

    protected static List/*<X11GLCapabilities>*/ getAvailableCapabilities(X11GLXDrawableFactory factory, AbstractGraphicsDevice device) {
        X11GLXDrawableFactory.SharedResource sharedResource = factory.getOrCreateSharedResource(device);
        if(null == sharedResource) {
            throw new GLException("Shared resource for device n/a: "+device);
        }
        X11GraphicsScreen sharedScreen = (X11GraphicsScreen) sharedResource.getScreen();
        X11GLXDrawable sharedDrawable = (X11GLXDrawable) sharedResource.getDrawable();
        GLCapabilitiesImmutable capsChosen = sharedDrawable.getChosenGLCapabilities();
        GLProfile glp = capsChosen.getGLProfile();

        List/*GLCapabilitiesImmutable*/ availableCaps = null;

        if( sharedResource.isGLXVersionGreaterEqualOneThree() ) {
            availableCaps = getAvailableGLCapabilitiesFBConfig(sharedScreen, glp);
        }
        if( null == availableCaps || availableCaps.isEmpty() ) {
            availableCaps = getAvailableGLCapabilitiesXVisual(sharedScreen, glp);
        }
        if( null != availableCaps && availableCaps.size() > 1 ) {
            Collections.sort(availableCaps, XVisualIDComparator);
        }
        return availableCaps;
    }

    static List/*<X11GLCapabilities>*/ getAvailableGLCapabilitiesFBConfig(X11GraphicsScreen x11Screen, GLProfile glProfile) {
        PointerBuffer fbcfgsL = null;

        // Utilizing FBConfig
        //
        AbstractGraphicsDevice absDevice = x11Screen.getDevice();
        long display = absDevice.getHandle();

        int screen = x11Screen.getIndex();
        boolean isMultisampleAvailable = GLXUtil.isMultisampleAvailable(display);
        int[] count = { -1 };
        ArrayList availableCaps = new ArrayList();

        fbcfgsL = GLX.glXChooseFBConfig(display, screen, null, 0, count, 0);
        if (fbcfgsL == null || fbcfgsL.limit()<=0) {
            if(DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.getAvailableGLCapabilitiesFBConfig: Failed glXChooseFBConfig ("+x11Screen+"): "+fbcfgsL+", "+count[0]);
            }
            return null;
        }
        for (int i = 0; i < fbcfgsL.limit(); i++) {
            if( !X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities(availableCaps, glProfile, display, fbcfgsL.get(i), GLGraphicsConfigurationUtil.ALL_BITS, isMultisampleAvailable) ) {
                if(DEBUG) {
                    System.err.println("X11GLXGraphicsConfiguration.getAvailableGLCapabilitiesFBConfig: FBConfig invalid (2): ("+x11Screen+"): fbcfg: "+toHexString(fbcfgsL.get(i)));
                }
            }
        }
        return availableCaps;
    }

    static List/*<X11GLCapabilities>*/ getAvailableGLCapabilitiesXVisual(X11GraphicsScreen x11Screen, GLProfile glProfile) {
        AbstractGraphicsDevice absDevice = x11Screen.getDevice();
        long display = absDevice.getHandle();

        int screen = x11Screen.getIndex();
        boolean isMultisampleAvailable = GLXUtil.isMultisampleAvailable(display);

        int[] count = new int[1];
        XVisualInfo template = XVisualInfo.create();
        template.setScreen(screen);
        XVisualInfo[] infos = X11Util.XGetVisualInfo(display, X11Lib.VisualScreenMask, template, count, 0);
        if (infos == null || infos.length<1) {
            throw new GLException("Error while enumerating available XVisualInfos");
        }
        ArrayList availableCaps = new ArrayList();
        for (int i = 0; i < infos.length; i++) {
            if( !X11GLXGraphicsConfiguration.XVisualInfo2GLCapabilities(availableCaps, glProfile, display, infos[i], GLGraphicsConfigurationUtil.ALL_BITS, isMultisampleAvailable) ) {
                if(DEBUG) {
                    System.err.println("X11GLXGraphicsConfiguration.getAvailableGLCapabilitiesXVisual: XVisual invalid: ("+x11Screen+"): fbcfg: "+toHexString(infos[i].getVisualid()));
                }
            }
        }
        return availableCaps;
    }


    static X11GLXGraphicsConfiguration chooseGraphicsConfigurationStatic(GLCapabilitiesImmutable capsChosen,
                                                                         GLCapabilitiesImmutable capsReq,
                                                                         GLCapabilitiesChooser chooser,
                                                                         X11GraphicsScreen x11Screen) {
        if (x11Screen == null) {
            throw new IllegalArgumentException("AbstractGraphicsScreen is null");
        }

        if (capsChosen == null) {
            capsChosen = new GLCapabilities(null);
        }
        X11GraphicsDevice x11Device = (X11GraphicsDevice) x11Screen.getDevice();
        X11GLXDrawableFactory factory = (X11GLXDrawableFactory) GLDrawableFactory.getDesktopFactory();

        capsChosen = GLGraphicsConfigurationUtil.fixGLCapabilities( capsChosen, factory.canCreateGLPbuffer(x11Device) );
        boolean usePBuffer = capsChosen.isPBuffer();
    
        X11GLXGraphicsConfiguration res = null;
        if( factory.isGLXVersionGreaterEqualOneThree(x11Device) ) {
            res = chooseGraphicsConfigurationFBConfig(capsChosen, capsReq, chooser, x11Screen);
        }
        if(null==res) {
            if(usePBuffer) {
                throw new GLException("Error: Couldn't create X11GLXGraphicsConfiguration based on FBConfig for "+capsChosen);
            }
            res = chooseGraphicsConfigurationXVisual(capsChosen, capsReq, chooser, x11Screen);
        }
        if(null==res) {
            throw new GLException("Error: Couldn't create X11GLXGraphicsConfiguration based on FBConfig and XVisual for "+capsChosen);
        }
        if(DEBUG) {
            System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationStatic("+x11Screen+","+capsChosen+"): "+res);
        }
        return res;
    }

    static X11GLXGraphicsConfiguration fetchGraphicsConfigurationFBConfig(X11GraphicsScreen x11Screen, int fbID, GLProfile glp) {
        AbstractGraphicsDevice absDevice = x11Screen.getDevice();
        long display = absDevice.getHandle();
        int screen = x11Screen.getIndex();

        long fbcfg = X11GLXGraphicsConfiguration.glXFBConfigID2FBConfig(display, screen, fbID);
        if( 0 == fbcfg || !X11GLXGraphicsConfiguration.GLXFBConfigValid( display, fbcfg ) ) {
            if(DEBUG) {
                System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: Failed - GLX FBConfig invalid: ("+x11Screen+","+toHexString(fbID)+"): fbcfg: "+toHexString(fbcfg));
            }
            return null;
        }
        X11GLCapabilities caps = X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities(glp, display, fbcfg, true, true, true, GLXUtil.isMultisampleAvailable(display));
        return new X11GLXGraphicsConfiguration(x11Screen, caps, caps, new DefaultGLCapabilitiesChooser());
    }

    private static X11GLXGraphicsConfiguration chooseGraphicsConfigurationFBConfig(GLCapabilitiesImmutable capsChosen,
                                                                                   GLCapabilitiesImmutable capsReq,
                                                                                   GLCapabilitiesChooser chooser,
                                                                                   X11GraphicsScreen x11Screen) {
        long recommendedFBConfig = -1;
        int recommendedIndex = -1;
        PointerBuffer fbcfgsL = null;
        GLProfile glProfile = capsChosen.getGLProfile();
        boolean onscreen = capsChosen.isOnscreen();
        boolean usePBuffer = capsChosen.isPBuffer();

        // Utilizing FBConfig
        //
        AbstractGraphicsDevice absDevice = x11Screen.getDevice();
        long display = absDevice.getHandle();

        int screen = x11Screen.getIndex();
        boolean isMultisampleAvailable = GLXUtil.isMultisampleAvailable(display);
        int[] attribs = X11GLXGraphicsConfiguration.GLCapabilities2AttribList(capsChosen, true, isMultisampleAvailable, display, screen);
        int[] count = { -1 };
        ArrayList/*<X11GLCapabilities>*/ availableCaps = new ArrayList();
        final int winattrmask = GLGraphicsConfigurationUtil.getWinAttributeBits(onscreen, usePBuffer);

        // 1st choice: get GLCapabilities based on users GLCapabilities setting recommendedIndex as preferred choice
        fbcfgsL = GLX.glXChooseFBConfig(display, screen, attribs, 0, count, 0);
        if (fbcfgsL != null && fbcfgsL.limit()>0) {
            for (int i = 0; i < fbcfgsL.limit(); i++) {
                if( !X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities(availableCaps, glProfile, display, fbcfgsL.get(i), winattrmask, isMultisampleAvailable) ) {
                    if(DEBUG) {
                        System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: FBConfig invalid (1): ("+x11Screen+","+capsChosen+"): fbcfg: "+toHexString(fbcfgsL.get(i)));
                    }
                }
            }
            if(availableCaps.size() > 0) {
                recommendedFBConfig = fbcfgsL.get(0);
                recommendedIndex=0;
                if (DEBUG) {
                    System.err.println("!!! glXChooseFBConfig recommended fbcfg " + toHexString(recommendedFBConfig) + ", idx " + recommendedIndex);
                    System.err.println("!!! user  caps " + capsChosen);
                    System.err.println("!!! fbcfg caps " + availableCaps.get(recommendedIndex));
                }
            } else if (DEBUG) {
                System.err.println("!!! glXChooseFBConfig no caps for recommended fbcfg " + toHexString(fbcfgsL.get(0)));
                System.err.println("!!! user  caps " + capsChosen);
            }
        }

        // 2nd choice: get all GLCapabilities available, no preferred recommendedIndex available
        if( 0 == availableCaps.size() ) {
            // reset ..
            recommendedFBConfig = -1;
            recommendedIndex = -1;

            fbcfgsL = GLX.glXChooseFBConfig(display, screen, null, 0, count, 0);
            if (fbcfgsL == null || fbcfgsL.limit()<=0) {
                if(DEBUG) {
                    System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: Failed glXChooseFBConfig ("+x11Screen+","+capsChosen+"): "+fbcfgsL+", "+count[0]);
                }
                return null;
            }

            for (int i = 0; i < fbcfgsL.limit(); i++) {
                if( !X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities(availableCaps, glProfile, display, fbcfgsL.get(i), winattrmask, isMultisampleAvailable) ) {
                    if(DEBUG) {
                        System.err.println("X11GLXGraphicsConfiguration.chooseGraphicsConfigurationFBConfig: FBConfig invalid (2): ("+x11Screen+"): fbcfg: "+toHexString(fbcfgsL.get(i)));
                    }
                }
            }
        }
        int chosenIndex = chooseCapabilities(chooser, capsChosen, availableCaps, recommendedIndex);
        if ( 0 > chosenIndex ) {
            if (DEBUG) {
                Thread.dumpStack();
            }
            return null;
        }
        X11GLCapabilities chosenCaps = (X11GLCapabilities) availableCaps.get(chosenIndex);

        return new X11GLXGraphicsConfiguration(x11Screen, chosenCaps, capsReq, chooser);
    }

    private static X11GLXGraphicsConfiguration chooseGraphicsConfigurationXVisual(GLCapabilitiesImmutable capsChosen,
                                                                                  GLCapabilitiesImmutable capsReq,
                                                                                  GLCapabilitiesChooser chooser,
                                                                                  X11GraphicsScreen x11Screen) {
        if (chooser == null) {
            chooser = new DefaultGLCapabilitiesChooser();
        }

        GLProfile glProfile = capsChosen.getGLProfile();
        final int winattrmask = GLGraphicsConfigurationUtil.getWinAttributeBits(capsChosen.isOnscreen(), false /* pbuffer */);
        ArrayList availableCaps = new ArrayList();
        int recommendedIndex = -1;

        AbstractGraphicsDevice absDevice = x11Screen.getDevice();
        long display = absDevice.getHandle();

        int screen = x11Screen.getIndex();
        boolean isMultisampleAvailable = GLXUtil.isMultisampleAvailable(display);
        int[] attribs = X11GLXGraphicsConfiguration.GLCapabilities2AttribList(capsChosen, false, isMultisampleAvailable, display, screen);

        // 1st choice: get GLCapabilities based on users GLCapabilities setting recommendedIndex as preferred choice
        XVisualInfo recommendedVis = GLX.glXChooseVisual(display, screen, attribs, 0);
        if (DEBUG) {
            System.err.print("!!! glXChooseVisual recommended ");
            if (recommendedVis == null) {
                System.err.println("null visual");
            } else {
                System.err.println("visual id " + toHexString(recommendedVis.getVisualid()));
            }
        }

        // 2nd choice: get all GLCapabilities available, preferred recommendedIndex might be available if 1st choice was successful
        int[] count = new int[1];
        XVisualInfo template = XVisualInfo.create();
        template.setScreen(screen);
        XVisualInfo[] infos = X11Util.XGetVisualInfo(display, X11Lib.VisualScreenMask, template, count, 0);
        if (infos == null || infos.length<1) {
            throw new GLException("Error while enumerating available XVisualInfos");
        }

        for (int i = 0; i < infos.length; i++) {
            if( !X11GLXGraphicsConfiguration.XVisualInfo2GLCapabilities(availableCaps, glProfile, display, infos[i], winattrmask, isMultisampleAvailable) ) {
                if(DEBUG) {
                    System.err.println("X11GLXGraphicsConfiguration.getAvailableGLCapabilitiesXVisual: XVisual invalid: ("+x11Screen+"): fbcfg: "+toHexString(infos[i].getVisualid()));
                }
            } else {
                // Attempt to find the visual chosenIndex by glXChooseVisual
                if (recommendedVis != null && recommendedVis.getVisualid() == infos[i].getVisualid()) {
                    recommendedIndex = availableCaps.size() - 1;
                }
            }
        }

        int chosenIndex = chooseCapabilities(chooser, capsChosen, availableCaps, recommendedIndex);
        if ( 0 > chosenIndex ) {
            if (DEBUG) {
                Thread.dumpStack();
            }
            return null;
        }
        X11GLCapabilities chosenCaps = (X11GLCapabilities) availableCaps.get(chosenIndex);

        return new X11GLXGraphicsConfiguration(x11Screen, chosenCaps, capsReq, chooser);
    }

}


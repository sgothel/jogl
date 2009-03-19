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
import javax.media.nativewindow.x11.X11GraphicsDevice;
import javax.media.nativewindow.x11.X11GraphicsConfiguration;
import com.sun.nativewindow.impl.NullWindow;
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
    // Keep this under the same debug flag as the drawable factory for convenience
    protected static final boolean DEBUG = Debug.debug("X11GLXDrawableFactory");

    public X11GLXGraphicsConfigurationFactory() {
        GraphicsConfigurationFactory.registerFactory(javax.media.nativewindow.x11.X11GraphicsDevice.class,
                                                     this);
    }

    public AbstractGraphicsConfiguration chooseGraphicsConfiguration(Capabilities capabilities,
                                                                     CapabilitiesChooser chooser,
                                                                     AbstractGraphicsDevice absDevice) {
        if (absDevice != null &&
            !(absDevice instanceof X11GraphicsDevice)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only X11GraphicsDevice objects");
        }

        if (capabilities != null &&
            !(capabilities instanceof GLCapabilities)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects");
        }

        if (chooser != null &&
            !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilitiesChooser objects");
        }

        int screen = 0;
        if (absDevice != null) {
            screen = ((X11GraphicsDevice) absDevice).getScreen();
        }

        long visualID = chooseGraphicsConfigurationImpl((GLCapabilities) capabilities,
                                                        (GLCapabilitiesChooser) chooser,
                                                        screen);
        return new X11GraphicsConfiguration(visualID);
    }

    /** Returns the visual ID of the chosen GraphicsConfiguration. */
    protected long chooseGraphicsConfigurationImpl(GLCapabilities capabilities,
                                                   GLCapabilitiesChooser chooser,
                                                   int screen) {
        if (capabilities == null) {
            capabilities = new GLCapabilities();
        }
        if (chooser == null) {
            chooser = new DefaultGLCapabilitiesChooser();
        }

        if (X11Util.isXineramaEnabled()) {
            screen = 0;
        }

        // Until we have a rock-solid visual selection algorithm written
        // in pure Java, we're going to provide the underlying window
        // system's selection to the chooser as a hint

        int[] attribs = X11GLXDrawableFactory.glCapabilities2AttribList(capabilities, GLXUtil.isMultisampleAvailable(), false, 0, 0);
        XVisualInfo[] infos = null;
        GLCapabilities[] caps = null;
        int recommendedIndex = -1;
        NativeWindowFactory.getDefaultFactory().getToolkitLock().lock();
        try {
            long display = X11Util.getDisplayConnection();
            XVisualInfo recommendedVis = GLX.glXChooseVisual(display, screen, attribs, 0);
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
            infos = X11Lib.XGetVisualInfo(display, X11Lib.VisualScreenMask, template, count, 0);
            if (infos == null) {
                throw new GLException("Error while enumerating available XVisualInfos");
            }
            caps = new GLCapabilities[infos.length];
            for (int i = 0; i < infos.length; i++) {
                caps[i] = ((X11GLXDrawableFactory) GLDrawableFactory.getFactory()).xvi2GLCapabilities(display, infos[i]);
                // Attempt to find the visual chosen by glXChooseVisual
                if (recommendedVis != null && recommendedVis.visualid() == infos[i].visualid()) {
                    recommendedIndex = i;
                }
            }
        } finally {
            NativeWindowFactory.getDefaultFactory().getToolkitLock().unlock();
        }
        // Store these away for later
        ((X11GLXDrawableFactory) GLDrawableFactory.getFactory()).
            initializeVisualToGLCapabilitiesMap(screen, infos, caps);
        int chosen;
        try {
          chosen = chooser.chooseCapabilities(capabilities, caps, recommendedIndex);
        } catch (NativeWindowException e) {
          throw new GLException(e);
        }
        if (chosen < 0 || chosen >= caps.length) {
            throw new GLException("GLCapabilitiesChooser specified invalid index (expected 0.." + (caps.length - 1) + ")");
        }
        XVisualInfo vis = infos[chosen];
        if (vis == null) {
            throw new GLException("GLCapabilitiesChooser chose an invalid visual");
        }
        return vis.visualid();
    }
}

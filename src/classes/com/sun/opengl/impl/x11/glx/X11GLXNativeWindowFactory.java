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

import javax.media.nwi.*;
import javax.media.opengl.*;

import com.sun.opengl.impl.*;
import com.sun.opengl.impl.x11.*;

/** Subclass of NativeWindowFactory used when non-AWT tookits are used
    on X11 platforms. Toolkits will likely need to subclass this one
    to add synchronization in certain places and change the accepted
    and returned types of the GraphicsDevice and GraphicsConfiguration
    abstractions. */

public class X11GLXNativeWindowFactory extends NativeWindowFactoryImpl {
    public AbstractGraphicsConfiguration chooseGraphicsConfiguration(NWCapabilities capabilities,
                                                                     NWCapabilitiesChooser chooser,
                                                                     AbstractGraphicsDevice absDevice) {
        if (absDevice != null &&
            !(absDevice instanceof X11GraphicsDevice)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only X11GraphicsDevice objects");
        }

        int screen = 0;
        if (absDevice != null) {
            screen = ((X11GraphicsDevice) absDevice).getScreen();
        }

        long visualID = chooseGraphicsConfigurationImpl(capabilities, chooser, screen);
        return new X11GraphicsConfiguration(visualID);
    }

    /** Returns the visual ID of the chosen GraphicsConfiguration. */
    protected long chooseGraphicsConfigurationImpl(NWCapabilities capabilities,
                                                   NWCapabilitiesChooser chooser,
                                                   int screen) {
        if (capabilities == null) {
            capabilities = new NWCapabilities();
        }
        if (chooser == null) {
            chooser = new DefaultNWCapabilitiesChooser();
        }

        if (X11Util.isXineramaEnabled()) {
            screen = 0;
        }

        // Until we have a rock-solid visual selection algorithm written
        // in pure Java, we're going to provide the underlying window
        // system's selection to the chooser as a hint

        int[] attribs = X11GLXDrawableFactory.glCapabilities2AttribList(capabilities, X11Util.isMultisampleAvailable(), false, 0, 0);
        XVisualInfo[] infos = null;
        NWCapabilities[] caps = null;
        int recommendedIndex = -1;
        getDefaultFactory().getToolkitLock().lock();
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
            caps = new NWCapabilities[infos.length];
            for (int i = 0; i < infos.length; i++) {
                caps[i] = ((X11GLXDrawableFactory) GLDrawableFactory.getFactory()).xvi2NWCapabilities(display, infos[i]);
                // Attempt to find the visual chosen by glXChooseVisual
                if (recommendedVis != null && recommendedVis.visualid() == infos[i].visualid()) {
                    recommendedIndex = i;
                }
            }
        } finally {
            getDefaultFactory().getToolkitLock().unlock();
        }
        // Store these away for later
        ((X11GLXDrawableFactory) GLDrawableFactory.getFactory()).
            initializeVisualToNWCapabilitiesMap(screen, infos, caps);
        int chosen = chooser.chooseCapabilities(capabilities, caps, recommendedIndex);
        if (chosen < 0 || chosen >= caps.length) {
            throw new GLException("NWCapabilitiesChooser specified invalid index (expected 0.." + (caps.length - 1) + ")");
        }
        XVisualInfo vis = infos[chosen];
        if (vis == null) {
            throw new GLException("NWCapabilitiesChooser chose an invalid visual");
        }
        return vis.visualid();
    }

    // On X11 platforms we need to do some locking; this basic
    // implementation should suffice for some simple window toolkits
    private ToolkitLock toolkitLock = new ToolkitLock() {
            private Thread owner;
            private int recursionCount;
            
            public synchronized void lock() {
                Thread cur = Thread.currentThread();
                if (owner == cur) {
                    ++recursionCount;
                    return;
                }
                while (owner != null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                owner = cur;
            }

            public synchronized void unlock() {
                if (owner != Thread.currentThread()) {
                    throw new RuntimeException("Not owner");
                }
                if (recursionCount > 0) {
                    --recursionCount;
                    return;
                }
                owner = null;
            }
        };

    public ToolkitLock getToolkitLock() {
        return toolkitLock;
    }
}

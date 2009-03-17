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

package com.sun.opengl.impl.x11.glx.awt;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import javax.media.nativewindow.*;
import javax.media.nativewindow.awt.AWTGraphicsDevice;
import javax.media.nativewindow.awt.AWTGraphicsConfiguration;
import javax.media.opengl.*;
import javax.media.opengl.awt.*;

import com.sun.opengl.impl.*;
import com.sun.nativewindow.impl.jawt.*;
import com.sun.nativewindow.impl.jawt.x11.*;
import com.sun.opengl.impl.x11.*;
import com.sun.opengl.impl.x11.glx.*;

public class X11AWTGLXNativeWindowFactory extends X11GLXNativeWindowFactory {

    public X11AWTGLXNativeWindowFactory() {
        Class componentClass = null;
        try {
            componentClass = Class.forName("java.awt.Component");
        } catch (Exception e) { 
            throw new GLException(e);
        }

        NativeWindowFactory.registerFactory(componentClass, this);
    }

    public AbstractGraphicsConfiguration chooseGraphicsConfiguration(NWCapabilities capabilities,
                                                                     NWCapabilitiesChooser chooser,
                                                                     AbstractGraphicsDevice absDevice) {
        GraphicsDevice device = null;
        if (absDevice != null &&
            !(absDevice instanceof AWTGraphicsDevice)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only AWTGraphicsDevice objects");
        }

        if ((absDevice == null) ||
            (((AWTGraphicsDevice) absDevice).getGraphicsDevice() == null)) {
            device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        } else {
            device = ((AWTGraphicsDevice) absDevice).getGraphicsDevice();
        }

        long visualID = chooseGraphicsConfigurationImpl(capabilities, chooser,
                                                        X11SunJDKReflection.graphicsDeviceGetScreen(device));

        // Now figure out which GraphicsConfiguration corresponds to this
        // visual by matching the visual ID
        GraphicsConfiguration[] configs = device.getConfigurations();
        for (int i = 0; i < configs.length; i++) {
            GraphicsConfiguration config = configs[i];
            if (config != null) {
                if (X11SunJDKReflection.graphicsConfigurationGetVisualID(config) == visualID) {
                    return new AWTGraphicsConfiguration(config);
                }
            }
        }

        // Either we weren't able to reflectively introspect on the
        // X11GraphicsConfig or something went wrong in the steps above;
        // we're going to return null without signaling an error condition
        // in this case (although we should distinguish between the two
        // and possibly report more of an error in the latter case)
        return null;
    }

    // When running the AWT on X11 platforms, we use the AWT native
    // interface (JAWT) to lock and unlock the toolkit
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
                JAWTUtil.lockToolkit();
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
                JAWTUtil.unlockToolkit();
            }
        };

    public ToolkitLock getToolkitLock() {
        return toolkitLock;
    }
}

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
import javax.media.nativewindow.x11.*;
import javax.media.nativewindow.awt.*;
import javax.media.opengl.*;
import javax.media.opengl.awt.*;

import com.sun.opengl.impl.*;
import com.sun.nativewindow.impl.jawt.*;
import com.sun.nativewindow.impl.jawt.x11.*;
import com.sun.nativewindow.impl.x11.*;
import com.sun.opengl.impl.x11.*;
import com.sun.opengl.impl.x11.glx.*;

public class X11AWTGLXGraphicsConfigurationFactory extends GraphicsConfigurationFactory {
    protected static final boolean DEBUG = Debug.debug("GraphicsConfiguration");

    public X11AWTGLXGraphicsConfigurationFactory() {
        GraphicsConfigurationFactory.registerFactory(javax.media.nativewindow.awt.AWTGraphicsDevice.class, this);
    }

    public AbstractGraphicsConfiguration chooseGraphicsConfiguration(Capabilities capabilities,
                                                                     CapabilitiesChooser chooser,
                                                                     AbstractGraphicsScreen absScreen) {
        GraphicsDevice device = null;
        if (absScreen != null &&
            !(absScreen instanceof AWTGraphicsScreen)) {
            throw new IllegalArgumentException("This GraphicsConfigurationFactory accepts only AWTGraphicsScreen objects");
        }

        if(null==absScreen) {
            absScreen = AWTGraphicsScreen.createScreenDevice(-1);
        }
        AWTGraphicsScreen awtScreen = (AWTGraphicsScreen) absScreen;
        device = ((AWTGraphicsDevice)awtScreen.getDevice()).getGraphicsDevice();

        if (capabilities != null &&
            !(capabilities instanceof GLCapabilities)) {
            throw new IllegalArgumentException("This GraphicsConfigurationFactory accepts only GLCapabilities objects");
        }

        if (chooser != null &&
            !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This GraphicsConfigurationFactory accepts only GLCapabilitiesChooser objects");
        }

        if(DEBUG) {
            System.err.println("X11AWTGLXGraphicsConfigurationFactory: got "+absScreen);
        }
        
        GraphicsConfiguration gc;
        X11GraphicsConfiguration x11Config;

        // Need the lock here, since it could be an AWT owned display connection
        NativeWindowFactory.getDefaultFactory().getToolkitLock().lock();
        try {
            long displayHandle = X11SunJDKReflection.graphicsDeviceGetDisplay(device);
            if(0==displayHandle) {
                displayHandle = X11Util.getThreadLocalDefaultDisplay();
                if(DEBUG) {
                    System.err.println("X11AWTGLXGraphicsConfigurationFactory: using a thread local X11 display");
                }
            } else {
                if(DEBUG) {
                    System.err.println("X11AWTGLXGraphicsConfigurationFactory: using AWT X11 display 0x"+Long.toHexString(displayHandle));
                }
            }
            ((AWTGraphicsDevice)awtScreen.getDevice()).setHandle(displayHandle);
            X11GraphicsDevice x11Device = new X11GraphicsDevice(displayHandle);

            X11GraphicsScreen x11Screen = new X11GraphicsScreen(x11Device, awtScreen.getIndex());
            if(DEBUG) {
                System.err.println("X11AWTGLXGraphicsConfigurationFactory: made "+x11Screen);
            }

            gc = device.getDefaultConfiguration();
            AWTGraphicsConfiguration.setupCapabilitiesRGBABits(capabilities, gc);
            if(DEBUG) {
                System.err.println("AWT Colormodel compatible: "+capabilities);
            }

            x11Config = (X11GraphicsConfiguration)
                GraphicsConfigurationFactory.getFactory(x11Device).chooseGraphicsConfiguration(capabilities,
                                                                                               chooser,
                                                                                               x11Screen);
            if (x11Config == null) {
                throw new GLException("Unable to choose a GraphicsConfiguration: "+capabilities+",\n\t"+chooser+"\n\t"+x11Screen);
            }

            long visualID = x11Config.getVisualID();
            // Now figure out which GraphicsConfiguration corresponds to this
            // visual by matching the visual ID
            GraphicsConfiguration[] configs = device.getConfigurations();
            for (int i = 0; i < configs.length; i++) {
                GraphicsConfiguration config = configs[i];
                if (config != null) {
                    if (X11SunJDKReflection.graphicsConfigurationGetVisualID(config) == visualID) {
                        return new AWTGraphicsConfiguration(awtScreen, x11Config.getChosenCapabilities(), x11Config.getRequestedCapabilities(), 
                                                            config, x11Config);
                    }
                }
            }
        } finally {
            NativeWindowFactory.getDefaultFactory().getToolkitLock().unlock();
        }
        // Either we weren't able to reflectively introspect on the
        // X11GraphicsConfig or something went wrong in the steps above;
        // Let's take the default configuration as used on Windows and MacOSX then ..
        if(DEBUG) {
            System.err.println("!!! Using default configuration");
        }
        return new AWTGraphicsConfiguration(awtScreen, x11Config.getChosenCapabilities(), x11Config.getRequestedCapabilities(), gc, x11Config);
    }
}

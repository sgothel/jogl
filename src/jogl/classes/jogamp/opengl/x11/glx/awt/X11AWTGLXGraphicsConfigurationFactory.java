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

package jogamp.opengl.x11.glx.awt;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;
import javax.media.nativewindow.awt.*;
import javax.media.opengl.*;

import jogamp.opengl.*;
import jogamp.nativewindow.jawt.x11.*;
import jogamp.nativewindow.x11.*;

public class X11AWTGLXGraphicsConfigurationFactory extends GLGraphicsConfigurationFactory {
    protected static final boolean DEBUG = Debug.debug("GraphicsConfiguration");

    public X11AWTGLXGraphicsConfigurationFactory() {
        GraphicsConfigurationFactory.registerFactory(javax.media.nativewindow.awt.AWTGraphicsDevice.class, this);
    }

    protected AbstractGraphicsConfiguration chooseGraphicsConfigurationImpl(
            CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested,
            CapabilitiesChooser chooser, AbstractGraphicsScreen absScreen) {
        GraphicsDevice device = null;
        if (absScreen != null &&
            !(absScreen instanceof AWTGraphicsScreen)) {
            throw new IllegalArgumentException("This GraphicsConfigurationFactory accepts only AWTGraphicsScreen objects");
        }

        if(null==absScreen) {
            absScreen = AWTGraphicsScreen.createScreenDevice(-1, AbstractGraphicsDevice.DEFAULT_UNIT);
        }
        AWTGraphicsScreen awtScreen = (AWTGraphicsScreen) absScreen;
        device = ((AWTGraphicsDevice)awtScreen.getDevice()).getGraphicsDevice();

        if ( !(capsChosen instanceof GLCapabilitiesImmutable) ) {
            throw new IllegalArgumentException("This GraphicsConfigurationFactory accepts only GLCapabilities objects - chosen");
        }

        if ( !(capsRequested instanceof GLCapabilitiesImmutable) ) {
            throw new IllegalArgumentException("This GraphicsConfigurationFactory accepts only GLCapabilities objects - requested");
        }

        if (chooser != null &&
            !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This GraphicsConfigurationFactory accepts only GLCapabilitiesChooser objects");
        }

        if(DEBUG) {
            System.err.println("X11AWTGLXGraphicsConfigurationFactory: got "+absScreen);
        }
        
        long displayHandle = X11SunJDKReflection.graphicsDeviceGetDisplay(device);
        boolean owner = false;
        if(0==displayHandle) {
            displayHandle = X11Util.createDisplay(null);
            owner = true;
            if(DEBUG) {
                System.err.println(Thread.currentThread().getName() + " - X11AWTGLXGraphicsConfigurationFactory: using a thread local X11 display");
            }
        } else {
            if(DEBUG) {
                System.err.println(Thread.currentThread().getName() + " - X11AWTGLXGraphicsConfigurationFactory: using AWT X11 display 0x"+Long.toHexString(displayHandle));
            }
            /**
             * Using the AWT display handle works fine with NVidia and AMD drivers today 2011-02-22,
             * hence no need for our own display instance anymore.
               String name = X11Util.XDisplayString(displayHandle);
               displayHandle = X11Util.createDisplay(name);
               owner = true;
             */
        }
        ((AWTGraphicsDevice)awtScreen.getDevice()).setSubType(NativeWindowFactory.TYPE_X11, displayHandle);
        X11GraphicsDevice x11Device = new X11GraphicsDevice(displayHandle, AbstractGraphicsDevice.DEFAULT_UNIT);
        x11Device.setCloseDisplay(owner);
        X11GraphicsScreen x11Screen = new X11GraphicsScreen(x11Device, awtScreen.getIndex());
        if(DEBUG) {
            System.err.println("X11AWTGLXGraphicsConfigurationFactory: made "+x11Screen);
        }
        GraphicsConfigurationFactory factory = GraphicsConfigurationFactory.getFactory(x11Device);
        GraphicsConfiguration[] configs = device.getConfigurations();

        //
        // Match the X11/GL Visual with AWT:
        //   - choose a config AWT agnostic and then
        //   - try to find the visual within the GraphicsConfiguration
        //
        // The resulting GraphicsConfiguration has to be 'forced' on the AWT native peer,
        // ie. returned by GLCanvas's getGraphicsConfiguration() befor call by super.addNotify().
        //
        X11GraphicsConfiguration x11Config = (X11GraphicsConfiguration) factory.chooseGraphicsConfiguration(capsChosen, capsRequested, chooser, x11Screen);
        if (x11Config == null) {
            throw new GLException("Unable to choose a GraphicsConfiguration (1): "+capsChosen+",\n\t"+chooser+"\n\t"+x11Screen);
        }
        long visualID = x11Config.getVisualID();
        for (int i = 0; i < configs.length; i++) {
            GraphicsConfiguration gc = configs[i];
            if (gc != null) {
                if (X11SunJDKReflection.graphicsConfigurationGetVisualID(gc) == visualID) {
                    if(DEBUG) {
                        System.err.println("Found matching AWT visual: 0x"+Long.toHexString(visualID) +" -> "+x11Config);
                    }
                    return new AWTGraphicsConfiguration(awtScreen,
                                                        x11Config.getChosenCapabilities(), x11Config.getRequestedCapabilities(),
                                                        gc, x11Config);
                }
            }
        }

        // try again using an AWT Colormodel compatible configuration
        GraphicsConfiguration gc = device.getDefaultConfiguration();
        capsChosen = AWTGraphicsConfiguration.setupCapabilitiesRGBABits(capsChosen, gc);
        x11Config = (X11GraphicsConfiguration) factory.chooseGraphicsConfiguration(capsChosen, capsRequested, chooser, x11Screen);
        if (x11Config == null) {
            throw new GLException("Unable to choose a GraphicsConfiguration (2): "+capsChosen+",\n\t"+chooser+"\n\t"+x11Screen);
        }
        visualID = x11Config.getVisualID();
        for (int i = 0; i < configs.length; i++) {
            gc = configs[i];
            if (X11SunJDKReflection.graphicsConfigurationGetVisualID(gc) == visualID) {
                if(DEBUG) {
                    System.err.println("Found matching default AWT visual: 0x"+Long.toHexString(visualID) +" -> "+x11Config);
                }
                return new AWTGraphicsConfiguration(awtScreen,
                                                    x11Config.getChosenCapabilities(), x11Config.getRequestedCapabilities(),
                                                    gc, x11Config);
            }
        }

        // Either we weren't able to reflectively introspect on the
        // X11GraphicsConfig or something went wrong in the steps above;
        // Let's take the default configuration as used on Windows and MacOSX then ..
        if(DEBUG) {
            System.err.println("!!! Using default configuration");
        }

        gc = device.getDefaultConfiguration();
        return new AWTGraphicsConfiguration(awtScreen, x11Config.getChosenCapabilities(), x11Config.getRequestedCapabilities(), gc, x11Config);
    }
}

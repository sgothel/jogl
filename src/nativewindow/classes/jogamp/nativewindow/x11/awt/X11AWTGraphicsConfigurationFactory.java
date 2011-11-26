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

package jogamp.nativewindow.x11.awt;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.CapabilitiesChooser;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.ToolkitLock;
import javax.media.nativewindow.awt.AWTGraphicsConfiguration;
import javax.media.nativewindow.awt.AWTGraphicsDevice;
import javax.media.nativewindow.awt.AWTGraphicsScreen;
import javax.media.nativewindow.x11.X11GraphicsConfiguration;
import javax.media.nativewindow.x11.X11GraphicsDevice;
import javax.media.nativewindow.x11.X11GraphicsScreen;

import jogamp.nativewindow.jawt.x11.X11SunJDKReflection;
import jogamp.nativewindow.x11.X11Lib;
import jogamp.nativewindow.x11.X11Util;

public class X11AWTGraphicsConfigurationFactory extends GraphicsConfigurationFactory {
    
    public static void registerFactory() {
        GraphicsConfigurationFactory.registerFactory(javax.media.nativewindow.awt.AWTGraphicsDevice.class, new X11AWTGraphicsConfigurationFactory());
    }    
    private X11AWTGraphicsConfigurationFactory() {
    }

    protected AbstractGraphicsConfiguration chooseGraphicsConfigurationImpl(
            CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested,
            CapabilitiesChooser chooser, AbstractGraphicsScreen absScreen) {    
        if (absScreen != null &&
            !(absScreen instanceof AWTGraphicsScreen)) {
            throw new IllegalArgumentException("This GraphicsConfigurationFactory accepts only AWTGraphicsScreen objects");
        }
        if(null==absScreen) {
            absScreen = AWTGraphicsScreen.createDefault();
        }

        return chooseGraphicsConfigurationStatic(capsChosen, capsRequested, chooser, (AWTGraphicsScreen)absScreen);
    }
    
    public static AWTGraphicsConfiguration chooseGraphicsConfigurationStatic(
            CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested,
            CapabilitiesChooser chooser, AWTGraphicsScreen awtScreen) {
        if(DEBUG) {
            System.err.println("X11AWTGraphicsConfigurationFactory: got "+awtScreen);
        }
        
        final GraphicsDevice device = ((AWTGraphicsDevice)awtScreen.getDevice()).getGraphicsDevice();

        long displayHandle = X11SunJDKReflection.graphicsDeviceGetDisplay(device);
        boolean owner = false;
        if(0==displayHandle) {
            displayHandle = X11Util.openDisplay(null);
            owner = true;
            if(DEBUG) {
                System.err.println(Thread.currentThread().getName() + " - X11AWTGraphicsConfigurationFactory: create local X11 display");
            }
        } else {
            /**
             * Using the AWT display handle works fine with NVidia.
             * However we experienced different results w/ AMD drivers, 
             * some work, but some behave erratic. 
             * I.e. hangs in XQueryExtension(..) via X11GraphicsScreen.
             */
            final String displayName = X11Lib.XDisplayString(displayHandle);
            if(DEBUG) {
                System.err.println(Thread.currentThread().getName() + " - X11AWTGraphicsConfigurationFactory: create X11 display @ "+displayName+" / 0x"+Long.toHexString(displayHandle));
            }
            displayHandle = X11Util.openDisplay(displayName);
            owner = true;
        }
        final ToolkitLock lock = owner ? 
                NativeWindowFactory.getDefaultToolkitLock(NativeWindowFactory.TYPE_AWT) : // own non-shared X11 display connection, no X11 lock
                NativeWindowFactory.createDefaultToolkitLock(NativeWindowFactory.TYPE_X11, NativeWindowFactory.TYPE_AWT, displayHandle);
        final X11GraphicsDevice x11Device = new X11GraphicsDevice(displayHandle, AbstractGraphicsDevice.DEFAULT_UNIT, lock, owner); 
        final X11GraphicsScreen x11Screen = new X11GraphicsScreen(x11Device, awtScreen.getIndex());
        if(DEBUG) {
            System.err.println("X11AWTGraphicsConfigurationFactory: made "+x11Screen);
        }
        
        final GraphicsConfigurationFactory factory = GraphicsConfigurationFactory.getFactory(x11Device);
        X11GraphicsConfiguration x11Config = (X11GraphicsConfiguration) factory.chooseGraphicsConfiguration(capsChosen, capsRequested, chooser, x11Screen);
        if (x11Config == null) {
            throw new NativeWindowException("Unable to choose a GraphicsConfiguration (1): "+capsChosen+",\n\t"+chooser+"\n\t"+x11Screen);
        }
        if(DEBUG) {
            System.err.println("X11AWTGraphicsConfigurationFactory: chosen x11Config: "+x11Config);
        }
        
        //
        // Match the X11/GL Visual with AWT:
        //   - choose a config AWT agnostic and then
        //   - try to find the visual within the GraphicsConfiguration
        //
        // The resulting GraphicsConfiguration has to be 'forced' on the AWT native peer,
        // ie. returned by GLCanvas's getGraphicsConfiguration() befor call by super.addNotify().
        //        
        final GraphicsConfiguration[] configs = device.getConfigurations();
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
            throw new NativeWindowException("Unable to choose a GraphicsConfiguration (2): "+capsChosen+",\n\t"+chooser+"\n\t"+x11Screen);
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


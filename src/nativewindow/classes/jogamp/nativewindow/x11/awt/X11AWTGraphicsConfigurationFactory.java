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

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.CapabilitiesChooser;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.VisualIDHolder;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.ToolkitLock;
import com.jogamp.nativewindow.VisualIDHolder.VIDType;

import com.jogamp.nativewindow.awt.AWTGraphicsConfiguration;
import com.jogamp.nativewindow.awt.AWTGraphicsDevice;
import com.jogamp.nativewindow.awt.AWTGraphicsScreen;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsScreen;

import jogamp.nativewindow.jawt.x11.X11SunJDKReflection;
import jogamp.nativewindow.x11.X11Lib;
import jogamp.nativewindow.x11.X11Util;

public class X11AWTGraphicsConfigurationFactory extends GraphicsConfigurationFactory {

    public static void registerFactory() {
        GraphicsConfigurationFactory.registerFactory(com.jogamp.nativewindow.awt.AWTGraphicsDevice.class, CapabilitiesImmutable.class, new X11AWTGraphicsConfigurationFactory());
    }
    private X11AWTGraphicsConfigurationFactory() {
    }

    @Override
    protected AbstractGraphicsConfiguration chooseGraphicsConfigurationImpl(
            final CapabilitiesImmutable capsChosen, final CapabilitiesImmutable capsRequested,
            final CapabilitiesChooser chooser, AbstractGraphicsScreen absScreen, final int nativeVisualID) {
        if (absScreen != null &&
            !(absScreen instanceof AWTGraphicsScreen)) {
            throw new IllegalArgumentException("This GraphicsConfigurationFactory accepts only AWTGraphicsScreen objects");
        }
        if(null==absScreen) {
            absScreen = AWTGraphicsScreen.createDefault();
        }

        return chooseGraphicsConfigurationStatic(capsChosen, capsRequested, chooser, (AWTGraphicsScreen)absScreen, nativeVisualID);
    }

    public static AWTGraphicsConfiguration chooseGraphicsConfigurationStatic(
            CapabilitiesImmutable capsChosen, final CapabilitiesImmutable capsRequested,
            final CapabilitiesChooser chooser, final AWTGraphicsScreen awtScreen, final int nativeVisualID) {
        if(DEBUG) {
            System.err.println("X11AWTGraphicsConfigurationFactory: got "+awtScreen);
        }

        final GraphicsDevice device = ((AWTGraphicsDevice)awtScreen.getDevice()).getGraphicsDevice();

        final long displayHandleAWT = X11SunJDKReflection.graphicsDeviceGetDisplay(device);
        final long displayHandle;
        final boolean owner;
        if(0==displayHandleAWT) {
            displayHandle = X11Util.openDisplay(null);
            owner = true;
            if(DEBUG) {
                System.err.println(getThreadName()+" - X11AWTGraphicsConfigurationFactory: Null AWT dpy, create local X11 display: "+toHexString(displayHandle));
            }
        } else {
            /**
             * Using the AWT display handle works fine with NVidia.
             * However we experienced different results w/ AMD drivers,
             * some work, but some behave erratic.
             * I.e. hangs in XQueryExtension(..) via X11GraphicsScreen.
             */
            final String displayName = X11Lib.XDisplayString(displayHandleAWT);
            displayHandle = X11Util.openDisplay(displayName);
            owner = true;
            if(DEBUG) {
                System.err.println(getThreadName()+" - X11AWTGraphicsConfigurationFactory: AWT dpy "+displayName+" / "+toHexString(displayHandleAWT)+", create X11 display "+toHexString(displayHandle));
            }
        }
        // Global JAWT lock required - No X11 resource locking due to private display connection
        final ToolkitLock lock = NativeWindowFactory.getDefaultToolkitLock(NativeWindowFactory.TYPE_AWT);
        final X11GraphicsDevice x11Device = new X11GraphicsDevice(displayHandle, AbstractGraphicsDevice.DEFAULT_UNIT, lock, owner);
        final X11GraphicsScreen x11Screen = new X11GraphicsScreen(x11Device, awtScreen.getIndex());
        if(DEBUG) {
            System.err.println("X11AWTGraphicsConfigurationFactory: made "+x11Screen);
        }

        final GraphicsConfigurationFactory factory = GraphicsConfigurationFactory.getFactory(x11Device, capsChosen);
        AbstractGraphicsConfiguration aConfig = factory.chooseGraphicsConfiguration(capsChosen, capsRequested, chooser, x11Screen, nativeVisualID);
        if (aConfig == null) {
            throw new NativeWindowException("Unable to choose a GraphicsConfiguration (1): "+capsChosen+",\n\t"+chooser+"\n\t"+x11Screen);
        }
        if(DEBUG) {
            System.err.println("X11AWTGraphicsConfigurationFactory: chosen config: "+aConfig);
            // Thread.dumpStack();
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
        int visualID = aConfig.getVisualID(VIDType.NATIVE);
        if(VisualIDHolder.VID_UNDEFINED != visualID) {
            for (int i = 0; i < configs.length; i++) {
                final GraphicsConfiguration gc = configs[i];
                if (gc != null) {
                    if (X11SunJDKReflection.graphicsConfigurationGetVisualID(gc) == visualID) {
                        if(DEBUG) {
                            System.err.println("Found matching AWT visual: 0x"+Integer.toHexString(visualID) +" -> "+aConfig);
                        }
                        return new AWTGraphicsConfiguration(awtScreen,
                                                            aConfig.getChosenCapabilities(), aConfig.getRequestedCapabilities(),
                                                            gc, aConfig);
                    }
                }
            }
        }

        // try again using an AWT Colormodel compatible configuration
        GraphicsConfiguration gc = device.getDefaultConfiguration();
        capsChosen = AWTGraphicsConfiguration.setupCapabilitiesRGBABits(capsChosen, gc);
        aConfig = factory.chooseGraphicsConfiguration(capsChosen, capsRequested, chooser, x11Screen, nativeVisualID);
        if (aConfig == null) {
            throw new NativeWindowException("Unable to choose a GraphicsConfiguration (2): "+capsChosen+",\n\t"+chooser+"\n\t"+x11Screen);
        }
        visualID = aConfig.getVisualID(VIDType.NATIVE);
        if(VisualIDHolder.VID_UNDEFINED != visualID) {
            for (int i = 0; i < configs.length; i++) {
                gc = configs[i];
                if (X11SunJDKReflection.graphicsConfigurationGetVisualID(gc) == visualID) {
                    if(DEBUG) {
                        System.err.println("Found matching default AWT visual: 0x"+Integer.toHexString(visualID) +" -> "+aConfig);
                    }
                    return new AWTGraphicsConfiguration(awtScreen,
                                                        aConfig.getChosenCapabilities(), aConfig.getRequestedCapabilities(),
                                                        gc, aConfig);
                }
            }
        }

        // Either we weren't able to reflectively introspect on the
        // X11GraphicsConfig or something went wrong in the steps above;
        // Let's take the default configuration as used on Windows and MacOSX then ..
        if(DEBUG) {
            System.err.println("Using default configuration");
        }

        gc = device.getDefaultConfiguration();
        return new AWTGraphicsConfiguration(awtScreen, aConfig.getChosenCapabilities(), aConfig.getRequestedCapabilities(), gc, aConfig);
    }
}


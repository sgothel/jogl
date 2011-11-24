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

package jogamp.opengl.macosx.cgl.awt;

import jogamp.opengl.GLGraphicsConfigurationFactory;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.CapabilitiesChooser;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.awt.AWTGraphicsConfiguration;
import javax.media.nativewindow.awt.AWTGraphicsDevice;
import javax.media.nativewindow.awt.AWTGraphicsScreen;
import javax.media.nativewindow.macosx.MacOSXGraphicsDevice;

import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLException;

import jogamp.opengl.macosx.cgl.MacOSXCGLGraphicsConfiguration;

public class MacOSXAWTCGLGraphicsConfigurationFactory extends GLGraphicsConfigurationFactory {
    protected static final boolean DEBUG = jogamp.opengl.Debug.debug("GraphicsConfiguration");

    public static void registerFactory() {
        GraphicsConfigurationFactory.registerFactory(javax.media.nativewindow.awt.AWTGraphicsDevice.class, new MacOSXAWTCGLGraphicsConfigurationFactory());
    }    
    private MacOSXAWTCGLGraphicsConfigurationFactory() {
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
            absScreen = AWTGraphicsScreen.createDefault();
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
            System.err.println("MacOSXAWTCGLGraphicsConfigurationFactory: got "+absScreen);
        }

        MacOSXGraphicsDevice macDevice = new MacOSXGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
        DefaultGraphicsScreen macScreen = new DefaultGraphicsScreen(macDevice, awtScreen.getIndex());
        if(DEBUG) {
            System.err.println("MacOSXAWTCGLGraphicsConfigurationFactory: made "+macScreen);
        }

        GraphicsConfiguration gc = device.getDefaultConfiguration();
        MacOSXCGLGraphicsConfiguration macConfig = (MacOSXCGLGraphicsConfiguration)
            GraphicsConfigurationFactory.getFactory(macDevice).chooseGraphicsConfiguration(capsChosen,
                                                                                           capsRequested,
                                                                                           chooser, macScreen);

        if (macConfig == null) {
            throw new GLException("Unable to choose a GraphicsConfiguration: "+capsChosen+",\n\t"+chooser+"\n\t"+macScreen);
        }

        // We have nothing to match .. so choose the default
        return new AWTGraphicsConfiguration(awtScreen, macConfig.getChosenCapabilities(), macConfig.getRequestedCapabilities(),
                                            gc, macConfig);
    }
}

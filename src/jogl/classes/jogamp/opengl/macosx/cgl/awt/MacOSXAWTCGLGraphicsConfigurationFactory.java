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

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.CapabilitiesChooser;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.DefaultGraphicsScreen;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;

import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLException;

import com.jogamp.nativewindow.awt.AWTGraphicsConfiguration;
import com.jogamp.nativewindow.awt.AWTGraphicsDevice;
import com.jogamp.nativewindow.awt.AWTGraphicsScreen;
import com.jogamp.nativewindow.macosx.MacOSXGraphicsDevice;

import jogamp.opengl.macosx.cgl.MacOSXCGLGraphicsConfiguration;

public class MacOSXAWTCGLGraphicsConfigurationFactory extends GLGraphicsConfigurationFactory {
    public static void registerFactory() {
        GraphicsConfigurationFactory.registerFactory(com.jogamp.nativewindow.awt.AWTGraphicsDevice.class, GLCapabilitiesImmutable.class, new MacOSXAWTCGLGraphicsConfigurationFactory());
    }
    private MacOSXAWTCGLGraphicsConfigurationFactory() {
    }

    @Override
    protected AbstractGraphicsConfiguration chooseGraphicsConfigurationImpl(
            final CapabilitiesImmutable capsChosen, final CapabilitiesImmutable capsRequested,
            final CapabilitiesChooser chooser, AbstractGraphicsScreen absScreen, final int nativeVisualID) {
        GraphicsDevice device = null;
        if (absScreen != null &&
            !(absScreen instanceof AWTGraphicsScreen)) {
            throw new IllegalArgumentException("This GraphicsConfigurationFactory accepts only AWTGraphicsScreen objects");
        }

        if(null==absScreen) {
            absScreen = AWTGraphicsScreen.createDefault();
        }
        final AWTGraphicsScreen awtScreen = (AWTGraphicsScreen) absScreen;
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

        final MacOSXGraphicsDevice macDevice = new MacOSXGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
        final DefaultGraphicsScreen macScreen = new DefaultGraphicsScreen(macDevice, awtScreen.getIndex());
        if(DEBUG) {
            System.err.println("MacOSXAWTCGLGraphicsConfigurationFactory: made "+macScreen);
        }

        final GraphicsConfiguration gc = device.getDefaultConfiguration();
        final MacOSXCGLGraphicsConfiguration macConfig = (MacOSXCGLGraphicsConfiguration)
            GraphicsConfigurationFactory.getFactory(macDevice, capsChosen).chooseGraphicsConfiguration(capsChosen,
                                                                                           capsRequested,
                                                                                           chooser, macScreen, nativeVisualID);

        if (macConfig == null) {
            throw new GLException("Unable to choose a GraphicsConfiguration: "+capsChosen+",\n\t"+chooser+"\n\t"+macScreen);
        }

        // We have nothing to match .. so choose the default
        return new AWTGraphicsConfiguration(awtScreen, macConfig.getChosenCapabilities(), macConfig.getRequestedCapabilities(),
                                            gc, macConfig);
    }
}

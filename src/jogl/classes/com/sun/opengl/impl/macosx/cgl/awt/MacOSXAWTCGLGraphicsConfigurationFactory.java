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

package com.sun.opengl.impl.macosx.cgl.awt;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import javax.media.nativewindow.*;
import javax.media.nativewindow.macosx.*;
import javax.media.nativewindow.awt.*;
import javax.media.opengl.*;
import javax.media.opengl.awt.*;

import com.sun.opengl.impl.*;
import com.sun.opengl.impl.macosx.cgl.*;
import com.sun.nativewindow.impl.jawt.*;
import com.sun.nativewindow.impl.jawt.macosx.*;

public class MacOSXAWTCGLGraphicsConfigurationFactory extends GraphicsConfigurationFactory {
    protected static final boolean DEBUG = com.sun.opengl.impl.Debug.debug("GraphicsConfiguration");

    public MacOSXAWTCGLGraphicsConfigurationFactory() {
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
            System.err.println("MacOSXAWTCGLGraphicsConfigurationFactory: got "+absScreen);
        }

        long displayHandle = 0;

        MacOSXGraphicsDevice macDevice = new MacOSXGraphicsDevice();
        DefaultGraphicsScreen macScreen = new DefaultGraphicsScreen(macDevice, awtScreen.getIndex());
        if(DEBUG) {
            System.err.println("MacOSXAWTCGLGraphicsConfigurationFactory: made "+macScreen);
        }

        GraphicsConfiguration gc = device.getDefaultConfiguration();
        AWTGraphicsConfiguration.setupCapabilitiesRGBABits(capabilities, gc);
        if(DEBUG) {
            System.err.println("AWT Colormodel compatible: "+capabilities);
        }

        MacOSXCGLGraphicsConfiguration macConfig = (MacOSXCGLGraphicsConfiguration)
            GraphicsConfigurationFactory.getFactory(macDevice).chooseGraphicsConfiguration(capabilities,
                                                                                           chooser,
                                                                                           macScreen);

        if (macConfig == null) {
            throw new GLException("Unable to choose a GraphicsConfiguration: "+capabilities+",\n\t"+chooser+"\n\t"+macScreen);
        }

        // FIXME: we have nothing to match .. so choose the default
        return new AWTGraphicsConfiguration(awtScreen, macConfig.getChosenCapabilities(), macConfig.getRequestedCapabilities(), gc, macConfig);
    }
}

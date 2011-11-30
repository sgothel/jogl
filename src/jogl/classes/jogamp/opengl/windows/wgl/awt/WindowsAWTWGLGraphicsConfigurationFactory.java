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

package jogamp.opengl.windows.wgl.awt;


import com.jogamp.common.util.ArrayHashSet;
import jogamp.nativewindow.jawt.windows.Win32SunJDKReflection;
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
import javax.media.nativewindow.windows.WindowsGraphicsDevice;

import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLException;

import jogamp.opengl.windows.wgl.WindowsWGLGraphicsConfiguration;
import javax.media.opengl.GLDrawableFactory;

public class WindowsAWTWGLGraphicsConfigurationFactory extends GLGraphicsConfigurationFactory {
    protected static final boolean DEBUG = jogamp.opengl.Debug.debug("GraphicsConfiguration");

    public static void registerFactory() {
        GraphicsConfigurationFactory.registerFactory(javax.media.nativewindow.awt.AWTGraphicsDevice.class, new WindowsAWTWGLGraphicsConfigurationFactory());
    }
    private WindowsAWTWGLGraphicsConfigurationFactory() {        
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
            if(DEBUG) {
                System.err.println("WindowsAWTWGLGraphicsConfigurationFactory: creating default device: "+absScreen);
            }
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
            System.err.println("WindowsAWTWGLGraphicsConfigurationFactory: got "+absScreen);
        }

        WindowsGraphicsDevice winDevice = new WindowsGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
        DefaultGraphicsScreen winScreen = new DefaultGraphicsScreen(winDevice, awtScreen.getIndex());
        GraphicsConfigurationFactory configFactory = GraphicsConfigurationFactory.getFactory(winDevice);
        GLDrawableFactory drawableFactory = GLDrawableFactory.getFactory( ((GLCapabilitiesImmutable)capsChosen).getGLProfile() );

        WindowsWGLGraphicsConfiguration winConfig = (WindowsWGLGraphicsConfiguration)
                                                       configFactory.chooseGraphicsConfiguration(capsChosen,
                                                                                                 capsRequested,
                                                                                                 chooser, winScreen);
        if (winConfig == null) {
            throw new GLException("Unable to choose a GraphicsConfiguration: "+capsChosen+",\n\t"+chooser+"\n\t"+winScreen);
        }

        GraphicsConfiguration chosenGC = null;

        // 1st Choice: Create an AWT GraphicsConfiguration with the desired PFD
        // This gc will probably not be able to support GDI (WGL_SUPPORT_GDI_ARB, PFD_SUPPORT_GDI)
        // however on most GPUs this is the current situation for Windows,
        // otherwise no hardware accelerated PFD could be achieved.
        //   - preselect with no constrains
        //   - try to create dedicated GC
        try {
            winConfig.preselectGraphicsConfiguration(drawableFactory, null);
            if ( 1 <= winConfig.getPixelFormatID() ) {
                chosenGC = Win32SunJDKReflection.graphicsConfigurationGet(device, winConfig.getPixelFormatID());
                if(DEBUG) {
                    System.err.println("WindowsAWTWGLGraphicsConfigurationFactory: Found new AWT PFD ID "+winConfig.getPixelFormatID()+" -> "+winConfig);
                }
            }
        } catch (GLException gle0) {
            gle0.printStackTrace();
            // go on ..
        }

        if( null == chosenGC ) {
            // 2nd Choice: Choose and match the GL Visual with AWT:
            //   - collect all AWT PFDs
            //   - choose a GL config from the pool of AWT PFDs
            //
            // The resulting GraphicsConfiguration has to be 'forced' on the AWT native peer,
            // ie. returned by GLCanvas's getGraphicsConfiguration() befor call by super.addNotify().
            //

            // collect all available PFD IDs
            GraphicsConfiguration[] configs = device.getConfigurations();
            int[] pfdIDs = new int[configs.length];
            ArrayHashSet pfdIDOSet = new ArrayHashSet();
            for (int i = 0; i < configs.length; i++) {
                GraphicsConfiguration gc = configs[i];
                pfdIDs[i] = Win32SunJDKReflection.graphicsConfigurationGetPixelFormatID(gc);
                pfdIDOSet.add(new Integer(pfdIDs[i]));
                if(DEBUG) {
                    System.err.println("AWT pfd["+i+"] "+pfdIDs[i]);
                }
            }
            if(DEBUG) {
                System.err.println("WindowsAWTWGLGraphicsConfigurationFactory: PFD IDs: "+pfdIDs.length+", unique: "+pfdIDOSet.size());
            }
            winConfig.preselectGraphicsConfiguration(drawableFactory, pfdIDs);
            int gcIdx = pfdIDOSet.indexOf(new Integer(winConfig.getPixelFormatID()));
            if( 0 > gcIdx ) {
                chosenGC = configs[gcIdx];
                if(DEBUG) {
                    System.err.println("WindowsAWTWGLGraphicsConfigurationFactory: Found matching AWT PFD ID "+winConfig.getPixelFormatID()+" -> "+winConfig);
                }
            }
        }

        if ( null == chosenGC ) {
            throw new GLException("Unable to determine GraphicsConfiguration: "+winConfig);
        }
        return new AWTGraphicsConfiguration(awtScreen, winConfig.getChosenCapabilities(), winConfig.getRequestedCapabilities(),
                                            chosenGC, winConfig);
    }
}

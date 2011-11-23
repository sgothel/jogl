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

package jogamp.opengl.macosx.cgl;

import jogamp.opengl.GLGraphicsConfigurationFactory;
import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.CapabilitiesChooser;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;


/** Subclass of GraphicsConfigurationFactory used when non-AWT tookits
    are used on OSX platforms. Toolkits will likely need to delegate
    to this one to change the accepted and returned types of the
    GraphicsDevice and GraphicsConfiguration abstractions. */

public class MacOSXCGLGraphicsConfigurationFactory extends GLGraphicsConfigurationFactory {
    protected static final boolean DEBUG = jogamp.opengl.Debug.debug("GraphicsConfiguration");

    static void registerFactory() {
        GraphicsConfigurationFactory.registerFactory(javax.media.nativewindow.macosx.MacOSXGraphicsDevice.class, new MacOSXCGLGraphicsConfigurationFactory());
    }
    private MacOSXCGLGraphicsConfigurationFactory() {        
    }

    protected AbstractGraphicsConfiguration chooseGraphicsConfigurationImpl(
            CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested,
            CapabilitiesChooser chooser, AbstractGraphicsScreen absScreen) {
        return chooseGraphicsConfigurationStatic(capsChosen, capsRequested, chooser, absScreen, false);
    }

    static MacOSXCGLGraphicsConfiguration chooseGraphicsConfigurationStatic(CapabilitiesImmutable capsChosen,
                                                                            CapabilitiesImmutable capsRequested,
                                                                            CapabilitiesChooser chooser,
                                                                            AbstractGraphicsScreen absScreen, boolean usePBuffer) {
        if (absScreen == null) {
            throw new IllegalArgumentException("AbstractGraphicsScreen is null");
        }

        if (! (capsChosen instanceof GLCapabilitiesImmutable) ) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects - chosen");
        }

        if (! (capsRequested instanceof GLCapabilitiesImmutable) ) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects - requested");
        }

        if (chooser != null &&
            !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilitiesChooser objects");
        }

        return new MacOSXCGLGraphicsConfiguration(absScreen, (GLCapabilitiesImmutable)capsChosen, (GLCapabilitiesImmutable)capsRequested, 0);
    }
}

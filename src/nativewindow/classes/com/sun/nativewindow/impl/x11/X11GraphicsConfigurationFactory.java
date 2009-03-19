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

package com.sun.nativewindow.impl.x11;

import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;

public class X11GraphicsConfigurationFactory extends GraphicsConfigurationFactory {
    // FIXME: there is a "quality of implementation" issue here. We
    // want to allow the use of the GraphicsConfigurationFactory in
    // conjunction with OpenGL as well as other rendering mechanisms.
    // On X11 platforms the OpenGL pixel format is associated with the
    // window's visual. On other platforms, the OpenGL pixel format is
    // chosen lazily. As in the OpenGL binding, the default
    // X11GraphicsConfigurationFactory would need to provide a default
    // mechanism for selecting a visual based on a set of
    // capabilities. Here we always return 0 for the visual ID, which
    // presumably corresponds to the default visual (which may be a
    // bad assumption). When using OpenGL, the OpenGL binding is
    // responsible for registering a GraphicsConfigurationFactory
    // which actually performs visual selection, though based on
    // GLCapabilities.
    public AbstractGraphicsConfiguration
        chooseGraphicsConfiguration(Capabilities capabilities,
                                    CapabilitiesChooser chooser,
                                    AbstractGraphicsDevice device)
        throws IllegalArgumentException, NativeWindowException {
        return new X11GraphicsConfiguration(0);
    }
}

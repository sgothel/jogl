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

package com.jogamp.nativewindow.x11;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.DefaultGraphicsScreen;
import com.jogamp.nativewindow.NativeWindowException;

import jogamp.nativewindow.x11.X11Lib;

/** Encapsulates a screen index on X11
    platforms. Objects of this type are passed to {@link
    com.jogamp.nativewindow.GraphicsConfigurationFactory#chooseGraphicsConfiguration
    GraphicsConfigurationFactory.chooseGraphicsConfiguration()} on X11
    platforms when toolkits other than the AWT are being used.  */

public class X11GraphicsScreen extends DefaultGraphicsScreen implements Cloneable {

    /** Constructs a new X11GraphicsScreen corresponding to the given native screen index. */
    public X11GraphicsScreen(final X11GraphicsDevice device, final int screen) {
        super(device, device.isXineramaEnabled() ? 0 : screen);
    }

    public static AbstractGraphicsScreen createScreenDevice(final long display, final int screenIdx, final boolean owner) {
        if(0==display) throw new NativeWindowException("display is null");
        return new X11GraphicsScreen(new X11GraphicsDevice(display, AbstractGraphicsDevice.DEFAULT_UNIT, owner), screenIdx);
    }

    public int getVisualID() {
        // It still could be an AWT hold handle ..
        return X11Lib.DefaultVisualID(getDevice().getHandle(), getIndex());
    }

    @Override
    public Object clone() {
      return super.clone();
    }
}

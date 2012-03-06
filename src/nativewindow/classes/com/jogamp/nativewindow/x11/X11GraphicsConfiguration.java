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

package javax.media.nativewindow.x11;

import javax.media.nativewindow.CapabilitiesImmutable;

import jogamp.nativewindow.MutableGraphicsConfiguration;
import jogamp.nativewindow.x11.XVisualInfo;

/** Encapsulates a graphics configuration, or OpenGL pixel format, on
    X11 platforms. Objects of this type are returned from {@link
    javax.media.nativewindow.GraphicsConfigurationFactory#chooseGraphicsConfiguration
    GraphicsConfigurationFactory.chooseGraphicsConfiguration()} on X11
    platforms when toolkits other than the AWT are being used.  */

public class X11GraphicsConfiguration extends MutableGraphicsConfiguration implements Cloneable {
    private XVisualInfo info;

    public X11GraphicsConfiguration(X11GraphicsScreen screen, 
                                    CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested,
                                    XVisualInfo info) {
        super(screen, capsChosen, capsRequested);
        this.info = info;
    }

    @Override
    public Object clone() {
      return super.clone();
    }

    public XVisualInfo getXVisualInfo() {
        return info;
    }

    protected void setXVisualInfo(XVisualInfo info) {
        this.info = info;
    }

    public long getVisualID() {
        return (null!=info)?info.getVisualid():0;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName()+"["+getScreen()+", visualID 0x" + Long.toHexString(getVisualID()) +
                                       ",\n\tchosen    " + capabilitiesChosen+
                                       ",\n\trequested " + capabilitiesRequested+ 
                                       "]";
    }
}

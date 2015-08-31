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

import com.jogamp.common.util.Bitfield;
import com.jogamp.nativewindow.CapabilitiesImmutable;

import com.jogamp.nativewindow.MutableGraphicsConfiguration;

import jogamp.nativewindow.x11.X11Capabilities;
import jogamp.nativewindow.x11.X11Lib;
import jogamp.nativewindow.x11.XRenderDirectFormat;
import jogamp.nativewindow.x11.XRenderPictFormat;
import jogamp.nativewindow.x11.XVisualInfo;

/** Encapsulates a graphics configuration, or OpenGL pixel format, on
    X11 platforms. Objects of this type are returned from {@link
    com.jogamp.nativewindow.GraphicsConfigurationFactory#chooseGraphicsConfiguration
    GraphicsConfigurationFactory.chooseGraphicsConfiguration()} on X11
    platforms when toolkits other than the AWT are being used.  */

public class X11GraphicsConfiguration extends MutableGraphicsConfiguration implements Cloneable {
    private XVisualInfo info;

    // FBConfig

    protected static XRenderDirectFormat XVisual2XRenderMask(final long dpy, final long visual) {
        final XRenderPictFormat xRenderPictFormat = XRenderPictFormat.create();
        return XVisual2XRenderMask(dpy, visual, xRenderPictFormat);
    }
    protected static XRenderDirectFormat XVisual2XRenderMask(final long dpy, final long visual, final XRenderPictFormat dest) {
        if( !X11Lib.XRenderFindVisualFormat(dpy, visual, dest) ) {
            return null;
        } else {
            return dest.getDirect();
        }
    }

    public static X11Capabilities XVisualInfo2X11Capabilities(final X11GraphicsDevice device, final XVisualInfo info) {
        final long display = device.getHandle();
        final X11Capabilities res = new X11Capabilities(info);

        final XRenderDirectFormat xrmask = ( null != info ) ? XVisual2XRenderMask( display, info.getVisual() ) : null ;
        final int alphaMask = ( null != xrmask ) ? xrmask.getAlphaMask() : 0;
        if( 0 < alphaMask ) {
            res.setBackgroundOpaque(false);
            res.setTransparentRedValue(xrmask.getRedMask());
            res.setTransparentGreenValue(xrmask.getGreenMask());
            res.setTransparentBlueValue(xrmask.getBlueMask());
            res.setTransparentAlphaValue(alphaMask);
        } else {
            res.setBackgroundOpaque(true);
        }
        // ALPHA shall be set at last - due to it's auto setting by the above (!opaque / samples)
        res.setRedBits       (Bitfield.Util.bitCount((int)info.getRed_mask()));
        res.setGreenBits     (Bitfield.Util.bitCount((int)info.getGreen_mask()));
        res.setBlueBits      (Bitfield.Util.bitCount((int)info.getBlue_mask()));
        res.setAlphaBits     (Bitfield.Util.bitCount(alphaMask));

        return res;
    }

    public X11GraphicsConfiguration(final X11GraphicsScreen screen,
                                    final CapabilitiesImmutable capsChosen, final CapabilitiesImmutable capsRequested,
                                    final XVisualInfo info) {
        super(screen, capsChosen, capsRequested);
        this.info = info;
    }

    @Override
    public Object clone() {
      return super.clone();
    }

    final public XVisualInfo getXVisualInfo() {
        return info;
    }

    final protected void setXVisualInfo(final XVisualInfo info) {
        this.info = info;
    }

    final public int getXVisualID() {
        return (null!=info)?(int)info.getVisualid():0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"["+getScreen()+", visualID 0x" + Long.toHexString(getXVisualID()) +
                                       ",\n\tchosen    " + capabilitiesChosen+
                                       ",\n\trequested " + capabilitiesRequested+
                                       "]";
    }
}

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

package jogamp.nativewindow.x11;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.CapabilitiesChooser;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.VisualIDHolder;

import com.jogamp.nativewindow.x11.X11GraphicsConfiguration;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsScreen;

public class X11GraphicsConfigurationFactory extends GraphicsConfigurationFactory {
    public static void registerFactory() {
        GraphicsConfigurationFactory.registerFactory(com.jogamp.nativewindow.x11.X11GraphicsDevice.class, CapabilitiesImmutable.class, new X11GraphicsConfigurationFactory());
    }
    private X11GraphicsConfigurationFactory() {
    }

    @Override
    protected AbstractGraphicsConfiguration chooseGraphicsConfigurationImpl(
        final CapabilitiesImmutable  capsChosen, final CapabilitiesImmutable capsRequested, final CapabilitiesChooser chooser, final AbstractGraphicsScreen screen, final int nativeVisualID)
        throws IllegalArgumentException, NativeWindowException {

        if(!(screen instanceof X11GraphicsScreen)) {
            throw new NativeWindowException("Only valid X11GraphicsScreen are allowed");
        }
        final XVisualInfo x11VisualInfo;
        if(VisualIDHolder.VID_UNDEFINED == nativeVisualID) {
            x11VisualInfo = getXVisualInfo(screen, capsChosen);
        } else {
            x11VisualInfo = getXVisualInfo(screen, nativeVisualID);
        }

        final X11Capabilities x11CapsChosen = X11GraphicsConfiguration.XVisualInfo2X11Capabilities((X11GraphicsDevice)screen.getDevice(), x11VisualInfo);
        final AbstractGraphicsConfiguration res = new X11GraphicsConfiguration((X11GraphicsScreen)screen,  x11CapsChosen, capsRequested, x11CapsChosen.getXVisualInfo());
        if(DEBUG) {
            System.err.println("X11GraphicsConfigurationFactory.chooseGraphicsConfigurationImpl(visualID 0x"+Integer.toHexString(nativeVisualID)+", "+x11VisualInfo+", "+screen+","+capsChosen+"): "+res);
        }
        return res;
    }

    public static XVisualInfo getXVisualInfo(final AbstractGraphicsScreen screen, final int visualID)
    {
        final XVisualInfo xvi_temp = XVisualInfo.create();
        xvi_temp.setVisualid(visualID);
        xvi_temp.setScreen(screen.getIndex());
        final int num[] = { -1 };
        final long display = screen.getDevice().getHandle();

        final XVisualInfo[] xvis = X11Lib.XGetVisualInfo(display, X11Lib.VisualIDMask|X11Lib.VisualScreenMask, xvi_temp, num, 0);

        if(xvis==null || num[0]<1) {
            return null;
        }
        return XVisualInfo.create(xvis[0]);
    }

    public static XVisualInfo getXVisualInfo(final AbstractGraphicsScreen screen, final CapabilitiesImmutable capabilities)
    {
        final XVisualInfo xv = getXVisualInfoImpl(screen, capabilities, 4 /* TrueColor */);
        if(null!=xv) return xv;
        return getXVisualInfoImpl(screen, capabilities, 5 /* DirectColor */);
    }

    private static XVisualInfo getXVisualInfoImpl(final AbstractGraphicsScreen screen, final CapabilitiesImmutable capabilities, final int c_class)
    {
        XVisualInfo ret = null;
        final int[] num = { -1 };

        final XVisualInfo vinfo_template = XVisualInfo.create();
        vinfo_template.setScreen(screen.getIndex());
        vinfo_template.setC_class(c_class);
        final long display = screen.getDevice().getHandle();

        final XVisualInfo[] vinfos = X11Lib.XGetVisualInfo(display, X11Lib.VisualScreenMask, vinfo_template, num, 0);
        XVisualInfo best=null;
        final int rdepth = capabilities.getRedBits() + capabilities.getGreenBits() + capabilities.getBlueBits() + capabilities.getAlphaBits();
        for (int i = 0; vinfos!=null && i < num[0]; i++) {
            if ( best == null ||
                 best.getDepth() < vinfos[i].getDepth() )
            {
                best = vinfos[i];
                if(rdepth <= best.getDepth())
                    break;
            }
        }
        if ( null!=best && ( rdepth <= best.getDepth() || 24 == best.getDepth()) ) {
            ret = XVisualInfo.create(best);
        }
        best = null;

        return ret;
    }
}


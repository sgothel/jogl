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

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.CapabilitiesChooser;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.x11.X11GraphicsConfiguration;
import javax.media.nativewindow.x11.X11GraphicsScreen;

public class X11GraphicsConfigurationFactory extends GraphicsConfigurationFactory {
    public static void registerFactory() {
        GraphicsConfigurationFactory.registerFactory(javax.media.nativewindow.x11.X11GraphicsDevice.class, new X11GraphicsConfigurationFactory());
    }    
    private X11GraphicsConfigurationFactory() {
    }
    
    protected AbstractGraphicsConfiguration chooseGraphicsConfigurationImpl(
        CapabilitiesImmutable  capsChosen, CapabilitiesImmutable capsRequested, CapabilitiesChooser chooser, AbstractGraphicsScreen screen)
        throws IllegalArgumentException, NativeWindowException {

        if(!(screen instanceof X11GraphicsScreen)) {
            throw new NativeWindowException("Only valid X11GraphicsScreen are allowed");
        }
        return new X11GraphicsConfiguration((X11GraphicsScreen)screen,  capsChosen, capsRequested, getXVisualInfo(screen, capsChosen));
    }

    public static XVisualInfo getXVisualInfo(AbstractGraphicsScreen screen, long visualID)
    {
        XVisualInfo xvi_temp = XVisualInfo.create();
        xvi_temp.setVisualid(visualID);
        xvi_temp.setScreen(screen.getIndex());
        int num[] = { -1 };
        long display = screen.getDevice().getHandle();

        XVisualInfo[] xvis = X11Lib.XGetVisualInfo(display, X11Lib.VisualIDMask|X11Lib.VisualScreenMask, xvi_temp, num, 0);

        if(xvis==null || num[0]<1) {
            return null;
        }

        return XVisualInfo.create(xvis[0]);
    }

    public static XVisualInfo getXVisualInfo(AbstractGraphicsScreen screen, CapabilitiesImmutable capabilities)
    {
        XVisualInfo xv = getXVisualInfoImpl(screen, capabilities, 4 /* TrueColor */);
        if(null!=xv) return xv;
        return getXVisualInfoImpl(screen, capabilities, 5 /* DirectColor */);
    }

    private static XVisualInfo getXVisualInfoImpl(AbstractGraphicsScreen screen, CapabilitiesImmutable capabilities, int c_class)
    {
        XVisualInfo ret = null;
        int[] num = { -1 };

        XVisualInfo vinfo_template = XVisualInfo.create();
        vinfo_template.setScreen(screen.getIndex());
        vinfo_template.setC_class(c_class);
        long display = screen.getDevice().getHandle();

        XVisualInfo[] vinfos = X11Lib.XGetVisualInfo(display, X11Lib.VisualScreenMask, vinfo_template, num, 0);
        XVisualInfo best=null;
        int rdepth = capabilities.getRedBits() + capabilities.getGreenBits() + capabilities.getBlueBits() + capabilities.getAlphaBits();
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


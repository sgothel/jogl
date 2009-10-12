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
import com.sun.nativewindow.impl.x11.XVisualInfo;
import com.sun.nativewindow.impl.x11.X11Lib;

public class X11GraphicsConfigurationFactory extends GraphicsConfigurationFactory {
    public AbstractGraphicsConfiguration
        chooseGraphicsConfiguration(Capabilities capabilities,
                                    CapabilitiesChooser chooser,
                                    AbstractGraphicsScreen screen)
        throws IllegalArgumentException, NativeWindowException {

        if(null==screen || !(screen instanceof X11GraphicsScreen)) {
            throw new NativeWindowException("Only valid X11GraphicsScreen are allowed");
        }
        return new X11GraphicsConfiguration((X11GraphicsScreen)screen, capabilities, capabilities, getXVisualInfo(screen, capabilities));
    }

    public static XVisualInfo getXVisualInfo(AbstractGraphicsScreen screen, long visualID)
    {
        XVisualInfo xvi_temp = XVisualInfo.create();
        xvi_temp.visualid(visualID);
        xvi_temp.screen(screen.getIndex());
        int num[] = { -1 };
        long display = screen.getDevice().getHandle();

        try {
            X11Lib.XLockDisplay(display);
            XVisualInfo[] xvis = X11Lib.XGetVisualInfoCopied(display, X11Lib.VisualIDMask|X11Lib.VisualScreenMask, xvi_temp, num, 0);

            if(xvis==null || num[0]<1) {
                return null;
            }

            return XVisualInfo.create(xvis[0]);
        } finally {
            X11Lib.XUnlockDisplay(display);
        }

    }

    public static XVisualInfo getXVisualInfo(AbstractGraphicsScreen screen, Capabilities capabilities)
    {
        XVisualInfo xv = getXVisualInfoImpl(screen, capabilities, 4 /* TrueColor */);
        if(null!=xv) return xv;
        return getXVisualInfoImpl(screen, capabilities, 5 /* DirectColor */);
    }

    private static XVisualInfo getXVisualInfoImpl(AbstractGraphicsScreen screen, Capabilities capabilities, int c_class)
    {
        XVisualInfo ret = null;
        int[] num = { -1 };

        XVisualInfo vinfo_template = XVisualInfo.create();
        vinfo_template.screen(screen.getIndex());
        vinfo_template.c_class(c_class);
        long display = screen.getDevice().getHandle();

        try {
            X11Lib.XLockDisplay(display);
            XVisualInfo[] vinfos = X11Lib.XGetVisualInfoCopied(display, X11Lib.VisualScreenMask, vinfo_template, num, 0);
            XVisualInfo best=null;
            int rdepth = capabilities.getRedBits() + capabilities.getGreenBits() + capabilities.getBlueBits() + capabilities.getAlphaBits();
            for (int i = 0; vinfos!=null && i < num[0]; i++) {
                if ( best == null || 
                     best.depth() < vinfos[i].depth() ) 
                {
                    best = vinfos[i];
                    if(rdepth <= best.depth())
                        break;
                }
            }
            if ( null!=best && ( rdepth <= best.depth() || 24 == best.depth()) ) {
                ret = XVisualInfo.create(best);
            }
            best = null;

            return ret;
        } finally {
            X11Lib.XUnlockDisplay(display);
        }
    }
}


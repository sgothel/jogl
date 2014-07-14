/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.opengl.test.junit.jogl.offscreen;

import com.jogamp.opengl.test.junit.util.*;

import org.junit.Assert;

import javax.media.opengl.*;
import javax.media.nativewindow.*;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;

public class WindowUtilNEWT {

    public static GLCapabilities fixCaps(final GLCapabilities caps, final boolean onscreen, final boolean pbuffer, final boolean undecorated) {
        final GLCapabilities caps2 = (GLCapabilities) caps.cloneMutable();
        caps2.setOnscreen(onscreen);
        caps2.setPBuffer(!onscreen && pbuffer);
        caps2.setDoubleBuffered(!onscreen);
        return caps2;
    }

    public static void setDemoFields(final GLEventListener demo, final Window window, final GLWindow glWindow, final boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(window);
        if(debug) {
            MiscUtils.setFieldIfExists(demo, "glDebug", true);
            MiscUtils.setFieldIfExists(demo, "glTrace", true);
        }
        if(!MiscUtils.setFieldIfExists(demo, "window", window)) {
            MiscUtils.setFieldIfExists(demo, "glWindow", glWindow);
        }
    }

    public static void run(final String testName, final GLWindow windowOffScreen, final GLEventListener demo,
                           final GLWindow windowOnScreenBlit, final WindowListener wl, final MouseListener ml,
                           final SurfaceUpdatedListener ul, final int frames, final boolean snapshot, final boolean debug) {
        Assert.assertNotNull(windowOffScreen);
        Assert.assertNotNull(demo);

        setDemoFields(demo, windowOffScreen, windowOffScreen, debug);
        windowOffScreen.addGLEventListener(demo);

        if ( null != windowOnScreenBlit ) {
            if(null!=wl) {
                windowOnScreenBlit.addWindowListener(wl);
            }
            if(null!=ml) {
                windowOnScreenBlit.addMouseListener(ml);
            }
            windowOnScreenBlit.setVisible(true);
        }

        final GLDrawable readDrawable = windowOffScreen.getContext().getGLDrawable() ;
        if ( null != windowOnScreenBlit ) {
            final ReadBuffer2Screen readDemo = new ReadBuffer2Screen( readDrawable ) ;
            windowOnScreenBlit.addGLEventListener(readDemo);
        }
        if(snapshot) {
            final boolean alpha = windowOffScreen.getChosenGLCapabilities().getAlphaBits()>0;
            final Surface2File s2f = new Surface2File(testName, alpha);
            windowOffScreen.addSurfaceUpdatedListener(s2f);
        }
        if(null!=ul) {
            windowOffScreen.addSurfaceUpdatedListener(ul);
        }

        if(debug) {
            System.err.println("+++++++++++++++++++++++++++ "+testName);
            System.err.println(windowOffScreen);
            System.err.println("+++++++++++++++++++++++++++ "+testName);
        }

        while ( windowOffScreen.getTotalFPSFrames() < frames) {
            windowOffScreen.display();
        }
        windowOffScreen.removeSurfaceUpdatedListener(ul);

    }

}

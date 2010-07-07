/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
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
 * Neither the name Sven Gothel or the names of
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
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.jogamp.test.junit.jogl.offscreen;

import com.jogamp.test.junit.util.*;

import org.junit.Assert;

import javax.media.opengl.*;
import javax.media.nativewindow.*;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;

public class WindowUtilNEWT {

    public static GLCapabilities fixCaps(GLCapabilities caps, boolean onscreen, boolean pbuffer, boolean undecorated) {
        GLCapabilities caps2 = (GLCapabilities) caps.clone();
        caps2.setOnscreen(onscreen);
        caps2.setPBuffer(!onscreen && pbuffer);
        caps2.setDoubleBuffered(!onscreen);
        return caps2;
    }

    public static void setDemoFields(GLEventListener demo, Window window, GLWindow glWindow, boolean debug) {
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

    public static void run(GLWindow windowOffScreen, GLEventListener demo, 
                           GLWindow windowOnScreen, WindowListener wl, MouseListener ml, 
                           SurfaceUpdatedListener ul, int frames, boolean snapshot, boolean debug) {
        Assert.assertNotNull(windowOffScreen);
        Assert.assertNotNull(demo);

        setDemoFields(demo, windowOffScreen, windowOffScreen, debug);
        windowOffScreen.addGLEventListener(demo);

        if ( null != windowOnScreen ) {
            if(null!=wl) {
                windowOnScreen.addWindowListener(wl);
            }
            if(null!=ml) {
                windowOnScreen.addMouseListener(ml);
            }
            windowOnScreen.setVisible(true);
        }

        GLDrawable readDrawable = windowOffScreen.getContext().getGLDrawable() ;

        if ( null == windowOnScreen ) {
            if(snapshot) {
                Surface2File s2f = new Surface2File();
                windowOffScreen.addSurfaceUpdatedListener(s2f);
            }
        } else {
            ReadBuffer2Screen readDemo = new ReadBuffer2Screen( readDrawable ) ;
            windowOnScreen.addGLEventListener(readDemo);
        }
        if(null!=ul) {
            windowOffScreen.addSurfaceUpdatedListener(ul);
        }

        if(debug) {
            System.out.println("+++++++++++++++++++++++++++");
            System.out.println(windowOffScreen);
            System.out.println("+++++++++++++++++++++++++++");
        }

        while ( windowOffScreen.getTotalFrames() < frames) {
            windowOffScreen.display();
        }
        windowOffScreen.removeAllSurfaceUpdatedListener();

    }

}

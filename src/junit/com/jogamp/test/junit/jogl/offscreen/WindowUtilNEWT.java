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

import java.lang.reflect.*;
import javax.media.opengl.*;
import javax.media.nativewindow.*;
import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;

public class WindowUtilNEWT {

    public static Window createWindow(GLCapabilities caps, int w, int h, boolean onscreen, boolean pbuffer, boolean undecorated) {
        GLCapabilities caps2 = (GLCapabilities) caps.clone();
        caps2.setOnscreen(onscreen);
        caps2.setPBuffer(!onscreen && pbuffer);
        caps2.setDoubleBuffered(!onscreen);

        Display display = NewtFactory.createDisplay(null); // local display
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Window window = NewtFactory.createWindow(screen, caps2, onscreen && undecorated);

        GLCapabilities glCaps = (GLCapabilities) window.getGraphicsConfiguration().getNativeGraphicsConfiguration().getChosenCapabilities();

        GLDrawableFactory factory = GLDrawableFactory.getFactory(glCaps.getGLProfile());
        GLDrawable drawable = factory.createGLDrawable(window);
        drawable.setRealized(true);
        GLContext context = drawable.createContext(null);

        window.setSize(w, h);
        window.setVisible(true);
        return window;
    }

    public static GLWindow createGLWindow(GLCapabilities caps, int w, int h, boolean onscreen, boolean pbuffer, boolean undecorated) {
        GLCapabilities caps2 = (GLCapabilities) caps.clone();
        caps2.setOnscreen(onscreen);
        caps2.setPBuffer(!onscreen && pbuffer);
        caps2.setDoubleBuffered(!onscreen);
        GLWindow window = GLWindow.create(caps2, onscreen && undecorated);
        window.setSize(w, h);
        window.setVisible(true);
        return window;
    }

    public static void run(GLWindow windowOffscreen, GLEventListener demo, 
                           GLWindow windowOnScreen, WindowListener wl, MouseListener ml, 
                           SurfaceUpdatedListener ul, int frames, boolean snapshot, boolean debug) {
        try {
            if(debug && null!=demo) {
                MiscUtils.setField(demo, "glDebug", new Boolean(true));
                MiscUtils.setField(demo, "glTrace", new Boolean(true));
            }
            if(null!=demo) {
                if(!MiscUtils.setField(demo, "window", windowOffscreen)) {
                    MiscUtils.setField(demo, "glWindow", windowOffscreen);
                }
                windowOffscreen.addGLEventListener(demo);
            }

            if ( null != windowOnScreen ) {
                if(null!=wl) {
                    windowOnScreen.addWindowListener(wl);
                }
                if(null!=ml) {
                    windowOnScreen.addMouseListener(ml);
                }
                windowOnScreen.setVisible(true);
            }

            GLDrawable readDrawable = windowOffscreen.getContext().getGLDrawable() ;

            if ( null == windowOnScreen ) {
                if(snapshot) {
                    Surface2File s2f = new Surface2File();
                    windowOffscreen.addSurfaceUpdatedListener(s2f);
                }
            } else {
                ReadBuffer2Screen readDemo = new ReadBuffer2Screen( readDrawable ) ;
                windowOnScreen.addGLEventListener(readDemo);
            }
            if(null!=ul) {
                windowOffscreen.addSurfaceUpdatedListener(ul);
            }

            if(debug) {
                System.out.println("+++++++++++++++++++++++++++");
                System.out.println(windowOffscreen);
                System.out.println("+++++++++++++++++++++++++++");
            }

            while ( windowOffscreen.getTotalFrames() < frames) {
                windowOffscreen.display();
            }
            windowOffscreen.removeAllSurfaceUpdatedListener();

        } catch (GLException e) {
            e.printStackTrace();
        }
    }

    public static void shutdown(GLWindow windowOffscreen, GLWindow windowOnscreen) {
        // Shut things down cooperatively
        if(null!=windowOnscreen) {
            windowOnscreen.destroy();
        }
        if(null!=windowOffscreen) {
            windowOffscreen.destroy();
        }
        if(null!=windowOnscreen) {
            windowOnscreen.getFactory().shutdown();
        }
    }
}

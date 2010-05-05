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

package com.jogamp.test.junit.newt;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import javax.media.opengl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.*;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;

import java.io.IOException;

import com.jogamp.test.junit.util.*;
import com.jogamp.test.junit.jogl.demos.es1.RedSquare;
import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;

public class TestParenting01NEWT {
    static int width, height;
    static long durationPerTest = 500;

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
    }

    static Window createWindow(NativeWindow parent, Screen screen, Capabilities caps, int width, int height) {
        Assert.assertNotNull(caps);
        //
        // Create native windowing resources .. X11/Win/OSX
        // 
        Window window;
        window = ( null == parent ) ? NewtFactory.createWindow(screen, caps) : NewtFactory.createWindow(parent, screen, caps) ;
        Assert.assertNotNull(window);
        window.setSize(width, height);
        Assert.assertTrue(false==window.isVisible());
        Assert.assertTrue(width==window.getWidth());
        Assert.assertTrue(height==window.getHeight());

        //
        // Create native OpenGL resources .. XGL/WGL/CGL .. 
        // equivalent to GLAutoDrawable methods: setVisible(true)
        // 
        caps = window.getGraphicsConfiguration().getNativeGraphicsConfiguration().getChosenCapabilities();
        Assert.assertNotNull(caps);
        Assert.assertTrue(caps.getGreenBits()>5);
        Assert.assertTrue(caps.getBlueBits()>5);
        Assert.assertTrue(caps.getRedBits()>5);
        Assert.assertTrue(caps.isOnscreen()==true);

        return window;
    }

    static void destroyWindow(Display display, Screen screen, Window window, GLWindow glWindow) {
        if(null!=glWindow) {
            glWindow.destroy();
        }
        if(null!=window) {
            window.destroy();
        }
        if(null!=screen) {
            screen.destroy();
        }
        if(null!=display) {
            display.destroy();
        }
    }

    @Test
    public void testWindowParenting01NewtOnNewtParentChildDraw() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(null);
        Assert.assertNotNull(caps);
        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);

        int x = 1;
        int y = 1;

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        Window window1 = createWindow( null, screen, caps, width, height );
        Assert.assertNotNull(window1);
        GLWindow glWindow1 = GLWindow.create(window1);
        Assert.assertNotNull(glWindow1);
        Assert.assertTrue(width==glWindow1.getWidth());
        Assert.assertTrue(height==glWindow1.getHeight());
        glWindow1.setTitle("testWindowParenting01NewtOnNewtParentChildDraw - PARENT");
        glWindow1.setPosition(x,y);
        glWindow1.addKeyListener(new TraceKeyAdapter(new KeyAction(eventFifo)));
        glWindow1.addWindowListener(new TraceWindowAdapter());

        Window window2 = createWindow(window1, screen, caps, width/2, height/2);
        Assert.assertNotNull(window2);
        GLWindow glWindow2 = GLWindow.create(window2);
        Assert.assertNotNull(glWindow2);
        Assert.assertTrue(width/2==glWindow2.getWidth());
        Assert.assertTrue(height/2==glWindow2.getHeight());
        glWindow2.setTitle("testWindowParenting01NewtOnNewtParentChildDraw - CHILD");
        glWindow2.setPosition(glWindow1.getWidth()/2, glWindow1.getHeight()/2);
        glWindow2.addKeyListener(new TraceKeyAdapter(new KeyAction(eventFifo)));
        glWindow2.addWindowListener(new TraceWindowAdapter(new WindowAction(eventFifo)));
        // glWindow2.addMouseListener(new TraceMouseAdapter());

        GLEventListener demo1 = new RedSquare();
        setDemoFields(demo1, window1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        GLEventListener demo2 = new Gears();
        setDemoFields(demo2, window2, glWindow2, false);
        glWindow2.addGLEventListener(demo2);

        glWindow2.setVisible(true);
        glWindow1.setVisible(true);

        glWindow2.setVisible(true);
        glWindow1.setVisible(true);

        glWindow2.display();
        glWindow1.display();

        boolean shouldQuit = false;
        long duration = durationPerTest;
        long step = 20;
        NEWTEvent event;

        while (duration>0 && !shouldQuit) {
            glWindow1.display();
            glWindow2.display();
            Thread.sleep(step);
            duration -= step;
            x += 1;
            y += 1;
            glWindow1.setPosition(x,y);
            glWindow2.setPosition(glWindow1.getWidth()/2,glWindow1.getHeight()/2-y);

            while( null != ( event = (NEWTEvent) eventFifo.get() ) ) {
                Window source = (Window) event.getSource();
                if(WindowEvent.EVENT_WINDOW_DESTROY_NOTIFY == event.getEventType()) {
                    shouldQuit = true;
                } else if(event instanceof KeyEvent) {
                    KeyEvent keyEvent = (KeyEvent) event;
                    switch(keyEvent.getKeyChar()) {
                        case 'q':
                            shouldQuit = true;
                            break;
                        case 'f':
                            source.setFullscreen(!source.isFullscreen());
                            break;
                    }
                } 
            }
        }
        destroyWindow(null, null, window2, glWindow2);
        destroyWindow(display, screen, window1, glWindow1);
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

    public static void main(String args[]) throws IOException {
        durationPerTest = 5000;
        String tstname = TestParenting01NEWT.class.getName();
        org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main(new String[] {
            tstname,
            "filtertrace=true",
            "haltOnError=false",
            "haltOnFailure=false",
            "showoutput=true",
            "outputtoformatters=true",
            "logfailedtests=true",
            "logtestlistenerevents=true",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } );
    }

}

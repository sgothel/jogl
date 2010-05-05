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

import java.awt.Button;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.Frame;

import javax.media.opengl.*;
import javax.media.nativewindow.*;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;

import java.io.IOException;

import com.jogamp.test.junit.util.*;
import com.jogamp.test.junit.jogl.demos.es1.RedSquare;
import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;

public class TestParenting01AWT {
    static int width, height;
    static long durationPerTest = 500;

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
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
    public void testWindowParenting01NewtChildOnAWTParentLayouted() throws InterruptedException {
        runNewtChildOnAWTParent(true);
    }

    @Test
    public void testWindowParenting02NewtChildOnAWTParentDirect() throws InterruptedException {
        runNewtChildOnAWTParent(false);
    }

    public void runNewtChildOnAWTParent(boolean useLayout) throws InterruptedException {
        Frame frame = new Frame("AWT Parent Frame");
        Assert.assertNotNull(frame);
        Component overlayedAWTComponent = null;

        if(useLayout) {
            overlayedAWTComponent = new Canvas();

            frame.setLayout(new BorderLayout());
            frame.add(new Button("North"), BorderLayout.NORTH);
            frame.add(new Button("South"), BorderLayout.SOUTH);
            frame.add(new Button("East"), BorderLayout.EAST);
            frame.add(new Button("West"), BorderLayout.WEST);
            frame.add(overlayedAWTComponent, BorderLayout.CENTER);

        } else {
            overlayedAWTComponent = frame;
        }

        Assert.assertNotNull(overlayedAWTComponent);
        frame.setSize(width, height);
        frame.setVisible(true); // should have native peers after this!

        GLCapabilities caps = new GLCapabilities(null);
        Assert.assertNotNull(caps);
        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        System.out.println("Display: "+display);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);

        final NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        Window window2 = NewtFactory.createWindow(overlayedAWTComponent, screen, caps);
        Assert.assertNotNull(window2);

        GLWindow glWindow2 = GLWindow.create(window2);
        Assert.assertNotNull(glWindow2);

        glWindow2.setSize(width, height);
        Assert.assertTrue(false==glWindow2.isVisible());
        Assert.assertTrue(width==glWindow2.getWidth());
        Assert.assertTrue(height==glWindow2.getHeight());

        glWindow2.setTitle("NEWT - CHILD");
        glWindow2.addKeyListener(new TraceKeyAdapter(new KeyAction(eventFifo)));
        glWindow2.addWindowListener(new TraceWindowAdapter(new WindowAdapter() {
                    public void windowDestroyNotify(WindowEvent e) {
                        eventFifo.put(e);
                    }
                }));

        GLEventListener demo2 = new Gears();
        setDemoFields(demo2, window2, glWindow2, false);
        glWindow2.addGLEventListener(demo2);

        glWindow2.setVisible(true);
        glWindow2.display();

        long duration = durationPerTest;
        long step = 20;
        NEWTEvent event;
        boolean shouldQuit = false;

        while (duration>0 && !shouldQuit) {
            glWindow2.display();
            Thread.sleep(step);
            duration -= step;

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
        frame.dispose();
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
        String tstname = TestParenting01AWT.class.getName();
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

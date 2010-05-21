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

    @Test
    public void testWindowParenting01NewtChildOnAWTParentLayouted() throws InterruptedException {
        runNewtChildOnAWTParent(true, false);
    }

    @Test
    public void testWindowParenting02NewtChildOnAWTParentLayoutedDef() throws InterruptedException {
        runNewtChildOnAWTParent(true, true);
    }

    @Test
    public void testWindowParenting03NewtChildOnAWTParentDirect() throws InterruptedException {
        runNewtChildOnAWTParent(false, false);
    }

    @Test
    public void testWindowParenting04NewtChildOnAWTParentDirectDef() throws InterruptedException {
        runNewtChildOnAWTParent(false, true);
    }

    public void runNewtChildOnAWTParent(boolean useLayout, boolean deferredPeer) throws InterruptedException {
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
            if(!deferredPeer) {
                frame.add(overlayedAWTComponent, BorderLayout.CENTER);
            }

        } else {
            overlayedAWTComponent = frame;
        }

        Assert.assertNotNull(overlayedAWTComponent);
        frame.setSize(width, height);
        if(!deferredPeer) {
            // ensure native peers are valid and component is displayable
            frame.setVisible(true);
        }

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        GLCapabilities caps = new GLCapabilities(null);
        Assert.assertNotNull(caps);

        GLWindow glWindow = GLWindow.create(overlayedAWTComponent, caps);
        Assert.assertNotNull(glWindow);
        Assert.assertEquals(overlayedAWTComponent.isVisible(),glWindow.isVisible());
        if(!deferredPeer) {
            Assert.assertTrue(0!=glWindow.getWindowHandle());
        } else {
            Assert.assertTrue(0==glWindow.getWindowHandle());
        }
        glWindow.setTitle("NEWT - CHILD");
        glWindow.addKeyListener(new TraceKeyAdapter(new KeyAction(eventFifo)));
        glWindow.addWindowListener(new TraceWindowAdapter(new WindowAction(eventFifo)));

        if(deferredPeer) {
            if(useLayout) {
                frame.add(overlayedAWTComponent, BorderLayout.CENTER);
            }
            frame.setVisible(true); // should have native peers after this - and all childs shall be visible!
        }

        GLEventListener demo = new Gears();
        setDemoFields(demo, glWindow, false);
        glWindow.addGLEventListener(demo);

        long duration = durationPerTest;
        long step = 20;
        NEWTEvent event;

        while (duration>0 && !glWindow.isDestroyed()) {
            glWindow.display();
            Thread.sleep(step);
            duration -= step;

            while( null != ( event = (NEWTEvent) eventFifo.get() ) ) {
                Window source = (Window) event.getSource();
                if(event instanceof KeyEvent) {
                    KeyEvent keyEvent = (KeyEvent) event;
                    switch(keyEvent.getKeyChar()) {
                        case 'q':
                            glWindow.destroy();
                            break;
                        case 'f':
                            source.setFullscreen(!source.isFullscreen());
                            break;
                    }
                } 
            }
        }
        glWindow.destroy();
        if(useLayout) {
            frame.remove(overlayedAWTComponent);
        }
        frame.dispose();
    }

    public static void setDemoFields(GLEventListener demo, GLWindow glWindow, boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        Window window = glWindow.getWindow();
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

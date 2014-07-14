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

package com.jogamp.opengl.test.junit.newt.parenting;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import java.awt.Button;
import java.awt.BorderLayout;
import java.awt.Frame;

import javax.media.opengl.*;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestParenting02AWT extends UITestCase {
    static int width, height;
    static long durationPerTest = 500;
    static long waitReparent = 300;
    static boolean verbose = false;

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
    }

    @Test
    public void test01NewtChildOnAWTParentLayouted() throws InterruptedException, InvocationTargetException {
        runNewtChildOnAWTParent(true, false);
    }

    @Test
    public void test02NewtChildOnAWTParentLayoutedDef() throws InterruptedException, InvocationTargetException {
        runNewtChildOnAWTParent(true, true);
    }

    @Test
    public void test03NewtChildOnAWTParentDirect() throws InterruptedException, InvocationTargetException {
        runNewtChildOnAWTParent(false, false);
    }

    @Test
    public void test04NewtChildOnAWTParentDirectDef() throws InterruptedException, InvocationTargetException {
        runNewtChildOnAWTParent(false, true);
    }

    public void runNewtChildOnAWTParent(final boolean useLayout, final boolean deferredPeer) throws InterruptedException, InvocationTargetException {
        final NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        // setup NEWT GLWindow ..
        final GLWindow glWindow = GLWindow.create(new GLCapabilities(null));
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("NEWT - CHILD");
        glWindow.addKeyListener(new TraceKeyAdapter(new KeyAction(eventFifo)));
        glWindow.addWindowListener(new TraceWindowAdapter(new WindowAction(eventFifo)));
        final GLEventListener demo = new GearsES2();
        setDemoFields(demo, glWindow, false);
        glWindow.addGLEventListener(demo);

        // attach NEWT GLWindow to AWT Canvas
        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow);
        Assert.assertNotNull(newtCanvasAWT);
        Assert.assertEquals(false, glWindow.isVisible());
        Assert.assertEquals(false, glWindow.isNativeValid());
        Assert.assertNull(glWindow.getParent());

        final Frame frame = new Frame("AWT Parent Frame");
        Assert.assertNotNull(frame);
        if(useLayout) {
            frame.setLayout(new BorderLayout());
            frame.add(new Button("North"), BorderLayout.NORTH);
            frame.add(new Button("South"), BorderLayout.SOUTH);
            frame.add(new Button("East"), BorderLayout.EAST);
            frame.add(new Button("West"), BorderLayout.WEST);
            if(!deferredPeer) {
                frame.add(newtCanvasAWT, BorderLayout.CENTER);
            }
        } else {
            if(!deferredPeer) {
                frame.add(newtCanvasAWT);
            }
        }

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                // frame.setSize(width, height);
                frame.setBounds(100, 100, width, height);
                frame.setVisible(true);
            }});
        // X11: true, Windows: false - Assert.assertEquals(true, glWindow.isVisible());

        if(deferredPeer) {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    if(useLayout) {
                        frame.add(newtCanvasAWT, BorderLayout.CENTER);
                    } else {
                        frame.add(newtCanvasAWT);
                    }
                    frame.validate();
                }});
        }

        // Since it is not defined when AWT's addNotify call happen
        // we just have to wait for it in this junit test
        // because we have assertions on the state.
        // Regular application shall not need to do that.
        do {
            Thread.yield();
            // 1st display .. creation
            glWindow.display();
        } while(!glWindow.isNativeValid()) ;

        final boolean wasOnscreen = glWindow.getChosenCapabilities().isOnscreen();

        Assert.assertEquals(true, glWindow.isNativeValid());
        Assert.assertNotNull(glWindow.getParent());
        if(verbose) {
            System.out.println("+++++++++++++++++++ 1st ADDED");
        }
        Thread.sleep(waitReparent);

        if(useLayout) {
            // test some fancy re-layout ..
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.remove(newtCanvasAWT);
                    frame.validate();
                }});
            Assert.assertEquals(false, glWindow.isVisible());
            if( wasOnscreen ) {
                Assert.assertEquals(true, glWindow.isNativeValid());
            } // else OK to be destroyed - due to offscreen/onscreen transition
            Assert.assertNull(glWindow.getParent());
            if(verbose) {
                System.out.println("+++++++++++++++++++ REMOVED!");
            }
            Thread.sleep(waitReparent);

            // should recreate properly ..
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.add(newtCanvasAWT, BorderLayout.CENTER);
                    frame.validate();
                }});
            glWindow.display();
            Assert.assertEquals(true, glWindow.isVisible());
            Assert.assertEquals(true, glWindow.isNativeValid());
            Assert.assertNotNull(glWindow.getParent());
            if(verbose) {
                System.out.println("+++++++++++++++++++ 2nd ADDED");
            }
            Thread.sleep(waitReparent);
        }

        long duration = durationPerTest;
        final long step = 20;
        NEWTEvent event;
        boolean shouldQuit = false;

        while (duration>0 && !shouldQuit) {
            glWindow.display();
            Thread.sleep(step);
            duration -= step;

            while( null != ( event = eventFifo.get() ) ) {
                final Window source = (Window) event.getSource();
                if(event instanceof KeyEvent) {
                    final KeyEvent keyEvent = (KeyEvent) event;
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
        if(verbose) {
            System.out.println("+++++++++++++++++++ END");
        }
        Thread.sleep(waitReparent);

        glWindow.destroy();
        if(useLayout) {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.remove(newtCanvasAWT);
                    frame.validate();
                }});
        }
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.dispose();
            } } );
    }

    public static void setDemoFields(final GLEventListener demo, final GLWindow glWindow, final boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        final Window window = glWindow.getDelegatedWindow();
        if(debug) {
            MiscUtils.setFieldIfExists(demo, "glDebug", true);
            MiscUtils.setFieldIfExists(demo, "glTrace", true);
        }
        if(!MiscUtils.setFieldIfExists(demo, "window", window)) {
            MiscUtils.setFieldIfExists(demo, "glWindow", glWindow);
        }
    }

    static int atoi(final String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(final String args[]) throws IOException {
        verbose = true;
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            } else if(args[i].equals("-wait")) {
                waitReparent = atoi(args[++i]);
            }
        }
        final String tstname = TestParenting02AWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
        /*
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
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } ); */
    }

}

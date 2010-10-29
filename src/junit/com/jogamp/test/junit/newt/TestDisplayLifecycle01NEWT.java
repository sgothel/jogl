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

import javax.media.nativewindow.*;
import javax.media.opengl.*;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;
import java.io.IOException;

import com.jogamp.test.junit.util.UITestCase;
import com.jogamp.test.junit.util.MiscUtils;
import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;

public class TestDisplayLifecycle01NEWT extends UITestCase {
    static GLProfile glp;
    static GLCapabilities caps;
    static int width, height;
    static long durationPerTest = 100; // ms

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton(true);
        width  = 640;
        height = 480;
        glp = GLProfile.getDefault();
        caps = new GLCapabilities(glp);
    }

    static GLWindow createWindow(Screen screen, GLCapabilities caps, int width, int height)
        throws InterruptedException
    {
        Assert.assertNotNull(caps);

        //
        // Create native windowing resources .. X11/Win/OSX
        // 
        GLWindow glWindow;
        if(null!=screen) {
            Window window = NewtFactory.createWindow(screen, caps);
            Assert.assertNotNull(window);
            glWindow = GLWindow.create(window);
        } else {
            glWindow = GLWindow.create(caps);
        }

        GLEventListener demo = new Gears();
        setDemoFields(demo, glWindow);
        glWindow.addGLEventListener(demo);
        glWindow.addWindowListener(new TraceWindowAdapter());
        glWindow.setSize(width, height);
        return glWindow;
    }

    private void testDisplayCreate01(Display display, Screen screen, boolean destroyWhenUnused) throws InterruptedException {
        // start-state == end-state
        Assert.assertEquals(0,Display.getActiveDisplayNumber());
        Assert.assertEquals(0,display.getReferenceCount());
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(false,display.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen.getReferenceCount());
        Assert.assertEquals(false,screen.isNativeValid());

        // Setup/Verify default DestroyWhenUnused behavior
        if(destroyWhenUnused) {
            screen.setDestroyWhenUnused(true);
        }
        Assert.assertEquals(destroyWhenUnused,display.getDestroyWhenUnused());
        Assert.assertEquals(destroyWhenUnused,screen.getDestroyWhenUnused());

        // Create Window, pending lazy native creation
        GLWindow window = createWindow(screen, caps, width, height);
        Assert.assertEquals(screen,window.getScreen());
        Assert.assertEquals(0,Display.getActiveDisplayNumber());
        Assert.assertEquals(0,display.getReferenceCount());
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertEquals(true,display.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen.getReferenceCount());
        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());

        // lazy native creation sequence: Display, Screen and Window
        Assert.assertEquals(0, window.getTotalFrames());
        window.setVisible(true);
        int wait=0;
        while(wait<10 && window.getTotalFrames()<1) { Thread.sleep(100); wait++; }
        System.err.println("Frames for setVisible(true) 1: "+window.getTotalFrames());
        Assert.assertTrue(0 < window.getTotalFrames());

        Assert.assertEquals(screen,window.getScreen());
        Assert.assertEquals(1,Display.getActiveDisplayNumber());
        Assert.assertEquals(1,display.getReferenceCount());
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,display.getEDTUtil().isRunning());
        Assert.assertEquals(1,screen.getReferenceCount());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());

        while(window.getDuration()<1*durationPerTest) {
            window.display();
            Thread.sleep(100);
        }
        System.err.println("duration: "+window.getDuration());

        // just make the Window invisible
        window.setVisible(false);
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());

        // just make the Window visible again
        window.resetCounter();
        Assert.assertEquals(0, window.getTotalFrames());
        window.setVisible(true);
        wait=0;
        while(wait<10 && window.getTotalFrames()<1) { Thread.sleep(100); wait++; }
        System.err.println("Frames for setVisible(true) 1: "+window.getTotalFrames());
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());
        Assert.assertTrue(0 < window.getTotalFrames());

        while(window.getDuration()<2*durationPerTest) {
            window.display();
            Thread.sleep(100);
        }
        System.err.println("duration: "+window.getDuration());

        // recoverable destruction, ie Display/Screen untouched
        window.destroy(false);
        Assert.assertEquals(screen,window.getScreen());
        Assert.assertEquals(1,Display.getActiveDisplayNumber());
        Assert.assertEquals(1,display.getReferenceCount());
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(true,display.getEDTUtil().isRunning());
        Assert.assertEquals(1,screen.getReferenceCount());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());
        window.resetCounter();
        Assert.assertEquals(0, window.getTotalFrames());

        // a display call shall not change a thing
        window.display();
        Assert.assertEquals(0, window.getTotalFrames());
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());

        // recover Window
        window.setVisible(true);
        wait=0;
        while(wait<10 && window.getTotalFrames()<1) { Thread.sleep(100); wait++; }
        System.err.println("Frames for setVisible(true) 2: "+window.getTotalFrames());
        Assert.assertTrue(0 < window.getTotalFrames());

        Assert.assertEquals(screen,window.getScreen());
        Assert.assertEquals(1,Display.getActiveDisplayNumber());
        Assert.assertEquals(1,display.getReferenceCount());
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,display.getEDTUtil().isRunning());
        Assert.assertEquals(1,screen.getReferenceCount());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());

        while(window.getDuration()<1*durationPerTest) {
            window.display();
            Thread.sleep(100);
        }
        System.err.println("duration: "+window.getDuration());

        // unrecoverable destruction, ie Display/Screen will be unreferenced
        window.destroy(true);
        Assert.assertEquals(null,window.getScreen());
        display.dumpDisplayList("Post destroy(true)");
        if(!destroyWhenUnused) {
            // display/screen untouched when unused, default
            Assert.assertEquals(1,Display.getActiveDisplayNumber());
            Assert.assertEquals(1,display.getReferenceCount());
            Assert.assertEquals(true,display.isNativeValid());
            Assert.assertNotNull(display.getEDTUtil());
            Assert.assertEquals(true,display.getEDTUtil().isRunning());
            Assert.assertEquals(0,screen.getReferenceCount());
            Assert.assertEquals(true,screen.isNativeValid());

            // manual destruction: Screen
            screen.destroy();
            Assert.assertEquals(1,Display.getActiveDisplayNumber());
            Assert.assertEquals(0,display.getReferenceCount());
            Assert.assertEquals(true,display.isNativeValid());
            Assert.assertNotNull(display.getEDTUtil());
            Assert.assertEquals(true,display.getEDTUtil().isRunning());
            Assert.assertEquals(0,screen.getReferenceCount());
            Assert.assertEquals(false,screen.isNativeValid());

            // manual destruction: Display
            display.destroy();
        } else {
            // display/screen destroyed when unused
        }

        // end-state == start-state
        Assert.assertEquals(0,Display.getActiveDisplayNumber());
        Assert.assertEquals(0,display.getReferenceCount());
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(false,display.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen.getReferenceCount());
        Assert.assertEquals(false,screen.isNativeValid());

        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());
    }

    @Test
    public void testDisplayCreate01_DestroyWhenUnused_False() throws InterruptedException {
        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        // Create Display/Screen, pending lazy native creation
        Display display = NewtFactory.createDisplay(null);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        testDisplayCreate01(display, screen, false);
        testDisplayCreate01(display, screen, false);

        Assert.assertEquals(0,Display.getActiveDisplayNumber());
    }

    @Test
    public void testDisplayCreate01_DestroyWhenUnused_True() throws InterruptedException {
        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        // Create Display/Screen, pending lazy native creation
        Display display = NewtFactory.createDisplay(null);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        testDisplayCreate01(display, screen, true);
        testDisplayCreate01(display, screen, true);

        Assert.assertEquals(0,Display.getActiveDisplayNumber());
    }

    public static void setDemoFields(GLEventListener demo, GLWindow glWindow) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        if(!MiscUtils.setFieldIfExists(demo, "window", glWindow)) {
            MiscUtils.setFieldIfExists(demo, "glWindow", glWindow);
        }
    }

    static int atoi(String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        System.err.println("durationPerTest: "+durationPerTest);
        String tstname = TestDisplayLifecycle01NEWT.class.getName();
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

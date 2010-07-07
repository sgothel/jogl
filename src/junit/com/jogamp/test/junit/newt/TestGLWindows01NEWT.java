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

import javax.media.nativewindow.*;
import javax.media.opengl.*;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;
import java.io.IOException;

import com.jogamp.test.junit.util.MiscUtils;
import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;

public class TestGLWindows01NEWT {
    static {
        GLProfile.initSingleton();
    }

    static GLProfile glp;
    static int width, height;
    static long durationPerTest = 100; // ms

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
        glp = GLProfile.getDefault();
    }

    static GLWindow createWindow(Screen screen, GLCapabilities caps, 
                                 int width, int height, boolean onscreen, boolean undecorated,
                                 boolean addGLEventListenerAfterVisible) 
        throws InterruptedException
    {
        Assert.assertNotNull(caps);
        caps.setOnscreen(onscreen);
        // System.out.println("Requested: "+caps);

        //
        // Create native windowing resources .. X11/Win/OSX
        // 
        GLWindow glWindow;
        if(null!=screen) {
            Window window = NewtFactory.createWindow(screen, caps, onscreen && undecorated);
            Assert.assertNotNull(window);
            glWindow = GLWindow.create(window);
        } else {
            glWindow = GLWindow.create(caps, onscreen && undecorated);
        }
        Assert.assertNotNull(glWindow);
        Assert.assertEquals(false,glWindow.isVisible());
        Assert.assertEquals(false,glWindow.isNativeWindowValid());

        GLEventListener demo = new Gears();
        setDemoFields(demo, glWindow);
        if(!addGLEventListenerAfterVisible) {
            glWindow.addGLEventListener(demo);
        }
        glWindow.addWindowListener(new TraceWindowAdapter());

        glWindow.setSize(width, height);

        glWindow.setVisible(true);
        Assert.assertEquals(true,glWindow.isVisible());
        Assert.assertEquals(true,glWindow.isNativeWindowValid());
        while(glWindow.getTotalFrames()<1) { Thread.sleep(100); }
        Assert.assertEquals(1,glWindow.getTotalFrames()); // native expose ..
        // Assert.assertEquals(width,glWindow.getWidth());
        // Assert.assertEquals(height,glWindow.getHeight());
        // System.out.println("Created: "+glWindow);

        //
        // Create native OpenGL resources .. XGL/WGL/CGL .. 
        // equivalent to GLAutoDrawable methods: setVisible(true)
        // 
        caps = (GLCapabilities) glWindow.getGraphicsConfiguration().getNativeGraphicsConfiguration().getChosenCapabilities();
        Assert.assertNotNull(caps);
        Assert.assertTrue(caps.getGreenBits()>5);
        Assert.assertTrue(caps.getBlueBits()>5);
        Assert.assertTrue(caps.getRedBits()>5);
        Assert.assertEquals(caps.isOnscreen(),onscreen);

        if(addGLEventListenerAfterVisible) {
            glWindow.addGLEventListener(demo);
            glWindow.display();
        }

        return glWindow;
    }

    static void destroyWindow(GLWindow glWindow, boolean deep) {
        if(null!=glWindow) {
            glWindow.destroy(deep);
            Assert.assertEquals(false,glWindow.isNativeWindowValid());
        }
    }

    @Test
    public void testWindowNativeRecreate01aSimple() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        GLWindow window = createWindow(null, caps, width, height, 
                                       true /* onscreen */, false /* undecorated */, 
                                       false /*addGLEventListenerAfterVisible*/);

        Assert.assertEquals(true,window.isNativeWindowValid());
        Assert.assertEquals(true,window.isVisible());
        window.destroy(false);
        Assert.assertEquals(false,window.isNativeWindowValid());
        Assert.assertEquals(false,window.isVisible());

        window.display();
        Assert.assertEquals(false,window.isNativeWindowValid());
        Assert.assertEquals(false,window.isVisible());

        window.setVisible(true);
        Assert.assertEquals(true,window.isNativeWindowValid());
        Assert.assertEquals(true,window.isVisible());

        window.setVisible(false);
        Assert.assertEquals(true,window.isNativeWindowValid());
        Assert.assertEquals(false,window.isVisible());

        destroyWindow(window, true);
    }

    @Test
    public void testWindowNativeRecreate01bSimple() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        GLWindow window = createWindow(null, caps, width, height, 
                                       true /* onscreen */, false /* undecorated */, 
                                       true /*addGLEventListenerAfterVisible*/);

        Assert.assertEquals(true,window.isNativeWindowValid());
        Assert.assertEquals(true,window.isVisible());
        window.destroy(false);
        Assert.assertEquals(false,window.isNativeWindowValid());
        Assert.assertEquals(false,window.isVisible());

        window.display();
        Assert.assertEquals(false,window.isNativeWindowValid());
        Assert.assertEquals(false,window.isVisible());

        window.setVisible(true);
        Assert.assertEquals(true,window.isNativeWindowValid());
        Assert.assertEquals(true,window.isVisible());

        window.setVisible(false);
        Assert.assertEquals(true,window.isNativeWindowValid());
        Assert.assertEquals(false,window.isVisible());

        destroyWindow(window, true);
    }

    @Test
    public void testWindowDecor01aSimple() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        GLWindow window = createWindow(null, caps, width, height, 
                                       true /* onscreen */, false /* undecorated */, 
                                       false /*addGLEventListenerAfterVisible*/);
        System.out.println("Created: "+window);
        while(window.getDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        System.out.println("duration: "+window.getDuration());
        destroyWindow(window, true);
    }

    @Test
    public void testWindowDecor01bSimple() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        GLWindow window = createWindow(null, caps, width, height, 
                                       true /* onscreen */, false /* undecorated */, 
                                       true /*addGLEventListenerAfterVisible*/);
        System.out.println("Created: "+window);
        while(window.getDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        System.out.println("duration: "+window.getDuration());
        destroyWindow(window, true);
    }

    @Test
    public void testWindowDecor02DestroyWinTwiceA() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        GLWindow window = createWindow(null, caps, width, height, 
                                       true /* onscreen */, false /* undecorated */, 
                                       false /*addGLEventListenerAfterVisible*/);
        while(window.getDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        System.out.println("duration: "+window.getDuration());
        destroyWindow(window, false);
        destroyWindow(window, true);
    }

    @Test
    public void testWindowDecor03TwoWin() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);

        Display display1 = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display1);
        Display display2 = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display2);
        Assert.assertEquals(display1, display2); // must be equal: same thread - same display

        Screen screen1  = NewtFactory.createScreen(display1, 0); // screen 0
        Assert.assertNotNull(screen1);
        GLWindow window1 = createWindow(screen1, caps, width, height, 
                                       true /* onscreen */, false /* undecorated */, 
                                       false /*addGLEventListenerAfterVisible*/);
        Assert.assertNotNull(window1);

        Screen screen2  = NewtFactory.createScreen(display2, 0); // screen 0
        Assert.assertNotNull(screen2);
        GLWindow window2 = createWindow(screen2, caps, width, height, 
                                       true /* onscreen */, false /* undecorated */, 
                                       false /*addGLEventListenerAfterVisible*/);
        Assert.assertNotNull(window2);

        while(window1.getDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        System.out.println("duration1: "+window1.getDuration());
        System.out.println("duration2: "+window2.getDuration());

        destroyWindow(window2, true);

        destroyWindow(window1, true);
    }

    public static void setDemoFields(GLEventListener demo, GLWindow glWindow) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        if(!MiscUtils.setFieldIfExists(demo, "window", glWindow.getInnerWindow())) {
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
        System.out.println("durationPerTest: "+durationPerTest);
        String tstname = TestGLWindows01NEWT.class.getName();
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

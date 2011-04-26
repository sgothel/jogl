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
import java.awt.Container;
import java.awt.Frame;
import java.awt.Dimension;

import javax.media.opengl.*;
import javax.media.nativewindow.*;

import com.jogamp.opengl.util.Animator;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.jogl.demos.es1.RedSquare;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.gears.Gears;

public class TestParenting01aAWT extends UITestCase {
    static int width, height;
    static long durationPerTest = 800;
    static long waitReparent = 0;
    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() throws InterruptedException {
        GLProfile.initSingleton(true);
        width  = 640;
        height = 480;
        glCaps = new GLCapabilities(null);
        // Thread.sleep(10000);
    }

    @Test
    public void testWindowParenting01CreateVisibleDestroy1() throws InterruptedException {
        int x = 0;
        int y = 0;

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        GLWindow glWindow1 = GLWindow.create(glCaps);
        Assert.assertNotNull(glWindow1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());
        glWindow1.setTitle("testWindowParenting01CreateVisibleDestroy");
        GLEventListener demo1 = new RedSquare();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        Assert.assertNotNull(newtCanvasAWT);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());

        Frame frame1 = new Frame("AWT Parent Frame");
        frame1.setLayout(new BorderLayout());
        frame1.add(new Button("North"), BorderLayout.NORTH);
        frame1.add(new Button("South"), BorderLayout.SOUTH);
        frame1.add(new Button("East"), BorderLayout.EAST);
        frame1.add(new Button("West"), BorderLayout.WEST);

        Container container1 = new Container();
        container1.setLayout(new BorderLayout());
        container1.add(new Button("north"), BorderLayout.NORTH);
        container1.add(new Button("south"), BorderLayout.SOUTH);
        container1.add(new Button("east"), BorderLayout.EAST);
        container1.add(new Button("west"), BorderLayout.WEST);
        container1.add(newtCanvasAWT, BorderLayout.CENTER);

        frame1.add(container1, BorderLayout.CENTER);
        frame1.setSize(width, height);

        // visible test
        frame1.setVisible(true);
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());

        Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);        
        animator1.start();
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        frame1.setVisible(false);
        Assert.assertEquals(true, glWindow1.isNativeValid());

        frame1.setVisible(true);
        Assert.assertEquals(true, glWindow1.isNativeValid());

        frame1.remove(newtCanvasAWT);
        // Assert.assertNull(glWindow1.getParent());
        Assert.assertEquals(true, glWindow1.isNativeValid());

        frame1.dispose();
        Assert.assertEquals(true, glWindow1.isNativeValid());

        glWindow1.destroy();
        Assert.assertEquals(false, glWindow1.isNativeValid());
    }

    @Test
    public void testWindowParenting02CreateVisibleDestroy2Defered() throws InterruptedException {
        int x = 0;
        int y = 0;

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        GLWindow glWindow1 = GLWindow.create(glCaps);
        Assert.assertNotNull(glWindow1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());
        GLEventListener demo1 = new RedSquare();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        Assert.assertNotNull(newtCanvasAWT);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());

        Frame frame = new Frame("AWT Parent Frame");
        Assert.assertNotNull(frame);
        frame.setSize(width, height);

        // visible test
        frame.setVisible(true);

        frame.add(newtCanvasAWT);
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());

        Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);
        animator1.start();
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        frame.dispose();
        glWindow1.destroy();
    }

    @Test
    public void testWindowParenting02CreateVisibleDestroy3Odd() throws InterruptedException {
        int x = 0;
        int y = 0;

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        GLWindow glWindow1 = GLWindow.create(glCaps);
        GLEventListener demo1 = new RedSquare();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);

        Frame frame = new Frame("AWT Parent Frame");
        Assert.assertNotNull(frame);
        frame.setSize(width, height);

        // visible test
        frame.setVisible(true);

        frame.add(newtCanvasAWT);

        Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);
        animator1.start();
        Assert.assertEquals(true, animator1.isStarted());
        Assert.assertEquals(true, animator1.isAnimating());
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }

        Assert.assertEquals(true, animator1.isAnimating()); // !!!

        frame.dispose();
        glWindow1.destroy();
    }

    @Test
    public void testWindowParenting03ReparentNewtWin2Top() throws InterruptedException {
        int x = 0;
        int y = 0;

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        GLWindow glWindow1 = GLWindow.create(glCaps);
        GLEventListener demo1 = new RedSquare();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);

        Frame frame = new Frame("AWT Parent Frame");
        frame.setSize(width, height);
        frame.setLocation(640, 480);
        frame.setVisible(true);

        frame.add(newtCanvasAWT);
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());

        Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);        
        animator1.start();

        int state = 0;
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<3*durationPerTest) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    glWindow1.reparentWindow(null);
                    Assert.assertEquals(true, glWindow1.isNativeValid());
                    Assert.assertNull(glWindow1.getParent());
                    break;
                case 1:
                    glWindow1.reparentWindow(newtCanvasAWT.getNativeWindow());
                    Assert.assertEquals(true, glWindow1.isNativeValid());
                    Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());
                    break;
            }
            state++;
        }

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        frame.dispose();
        glWindow1.destroy();
    }

    @Test
    public void testWindowParenting04ReparentNewtWin2TopLayouted() throws InterruptedException {
        int x = 0;
        int y = 0;

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        GLWindow glWindow1 = GLWindow.create(glCaps);
        GLEventListener demo1 = new RedSquare();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);

        Frame frame = new Frame("AWT Parent Frame");
        frame.setLayout(new BorderLayout());
        frame.add(new Button("North"), BorderLayout.NORTH);
        frame.add(new Button("South"), BorderLayout.SOUTH);
        frame.add(new Button("East"), BorderLayout.EAST);
        frame.add(new Button("West"), BorderLayout.WEST);
        frame.setSize(width, height);
        frame.setLocation(640, 480);
        frame.setVisible(true);

        frame.add(newtCanvasAWT, BorderLayout.CENTER);
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());

        Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);        
        animator1.start();

        int state = 0;
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<3*durationPerTest) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    glWindow1.reparentWindow(null);
                    Assert.assertEquals(true, glWindow1.isNativeValid());
                    Assert.assertNull(glWindow1.getParent());
                    break;
                case 1:
                    glWindow1.reparentWindow(newtCanvasAWT.getNativeWindow());
                    Assert.assertEquals(true, glWindow1.isNativeValid());
                    Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());
                    break;
            }
            state++;
        }

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        frame.dispose();
        glWindow1.destroy();
    }

    @Test
    public void testWindowParenting05ReparentAWTWinHopFrame2Frame() throws InterruptedException {
        int x = 0;
        int y = 0;

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setUndecorated(true);
        GLEventListener demo1 = new RedSquare();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);

        Frame frame1 = new Frame("AWT Parent Frame");
        frame1.setLayout(new BorderLayout());
        frame1.add(new Button("North"), BorderLayout.NORTH);
        frame1.add(new Button("South"), BorderLayout.SOUTH);
        frame1.add(new Button("East"), BorderLayout.EAST);
        frame1.add(new Button("West"), BorderLayout.WEST);
        frame1.setSize(width, height);
        frame1.setLocation(0, 0);
        frame1.setVisible(true);

        Frame frame2 = new Frame("AWT Parent Frame");
        frame2.setLayout(new BorderLayout());
        frame2.add(new Button("North"), BorderLayout.NORTH);
        frame2.add(new Button("South"), BorderLayout.SOUTH);
        frame2.add(new Button("East"), BorderLayout.EAST);
        frame2.add(new Button("West"), BorderLayout.WEST);
        frame2.setSize(width, height);
        frame2.setLocation(640, 480);
        frame2.setVisible(true);

        frame1.add(newtCanvasAWT, BorderLayout.CENTER);
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());

        Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);        
        animator1.start();

        int state = 0;
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<3*durationPerTest) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    frame1.remove(newtCanvasAWT);
                    frame2.add(newtCanvasAWT, BorderLayout.CENTER);
                    break;
                case 1:
                    frame2.remove(newtCanvasAWT);
                    frame1.add(newtCanvasAWT, BorderLayout.CENTER);
                    break;
            }
            state++;
        }

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        frame1.dispose();
        frame2.dispose();
        glWindow1.destroy();
    }

    public static void setDemoFields(GLEventListener demo, GLWindow glWindow, boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);        
        if(debug) {
            MiscUtils.setFieldIfExists(demo, "glDebug", true);
            MiscUtils.setFieldIfExists(demo, "glTrace", true);
        }
        if(!MiscUtils.setFieldIfExists(demo, "window", glWindow.getWindow())) {
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
            } else if(args[i].equals("-wait")) {
                waitReparent = atoi(args[++i]);
            }
        }
        String tstname = TestParenting01aAWT.class.getName();
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

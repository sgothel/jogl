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

import com.jogamp.test.junit.util.*;
import com.jogamp.test.junit.jogl.demos.es1.RedSquare;
import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;

public class TestParenting01AWT {
    static int width, height;
    static long durationPerTest = 800;
    static long waitReparent = 0;
    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
        glCaps = new GLCapabilities(null);
    }

    @Test
    public void testWindowParenting01CreateVisibleDestroy1() throws InterruptedException {
        int x = 0;
        int y = 0;

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        GLWindow glWindow1 = GLWindow.create(glCaps);
        Assert.assertNotNull(glWindow1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeWindowValid());
        Assert.assertNull(glWindow1.getParentNativeWindow());
        glWindow1.setTitle("testWindowParenting01CreateVisibleDestroy");
        GLEventListener demo1 = new RedSquare();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        // glWindow1.setSize(600, 300);

        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        Assert.assertNotNull(newtCanvasAWT);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeWindowValid());
        Assert.assertNull(glWindow1.getParentNativeWindow());

        Frame frame = new Frame("AWT Parent Frame");
        Assert.assertNotNull(frame);
        frame.add(newtCanvasAWT);
        frame.setSize(width, height);
        // frame.pack();

        // visible test
        frame.setVisible(true);
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParentNativeWindow());

        Animator animator1 = new Animator(glWindow1);
        animator1.start();
        while(animator1.isAnimating() && animator1.getDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        frame.setVisible(false);
        Assert.assertEquals(false, glWindow1.isDestroyed());

        frame.setVisible(true);
        Assert.assertEquals(false, glWindow1.isDestroyed());

        frame.remove(newtCanvasAWT);
        // Assert.assertNull(glWindow1.getParentNativeWindow());
        Assert.assertEquals(false, glWindow1.isDestroyed());

        frame.dispose();
        Assert.assertEquals(false, glWindow1.isDestroyed());

        glWindow1.destroy(true);
        //Assert.assertEquals(true, glWindow1.isDestroyed());
    }

    @Test
    public void testWindowParenting02CreateVisibleDestroy2Defered() throws InterruptedException {
        int x = 0;
        int y = 0;

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        GLWindow glWindow1 = GLWindow.create(glCaps);
        Assert.assertNotNull(glWindow1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeWindowValid());
        Assert.assertNull(glWindow1.getParentNativeWindow());
        GLEventListener demo1 = new RedSquare();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        // glWindow1.setSize(600, 300);

        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        Assert.assertNotNull(newtCanvasAWT);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeWindowValid());
        Assert.assertNull(glWindow1.getParentNativeWindow());

        Frame frame = new Frame("AWT Parent Frame");
        Assert.assertNotNull(frame);
        frame.setSize(width, height);

        // visible test
        frame.setVisible(true);

        frame.add(newtCanvasAWT);
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParentNativeWindow());
        // frame.pack();

        Animator animator1 = new Animator(glWindow1);
        animator1.start();
        while(animator1.isAnimating() && animator1.getDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        frame.dispose();
        glWindow1.destroy(true);
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
        // glWindow1.setSize(600, 300);

        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);

        Frame frame = new Frame("AWT Parent Frame");
        Assert.assertNotNull(frame);
        frame.setSize(width, height);

        // visible test
        frame.setVisible(true);

        frame.add(newtCanvasAWT);
        // frame.pack();

        Animator animator1 = new Animator(glWindow1);
        animator1.start();
        while(animator1.isAnimating() && animator1.getDuration()<durationPerTest) {
            Thread.sleep(100);
        }

        Assert.assertEquals(true, animator1.isAnimating()); // !!!

        frame.dispose();
        glWindow1.destroy(true);
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
        // glWindow1.setSize(600, 300);

        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);

        Frame frame = new Frame("AWT Parent Frame");
        frame.setSize(width, height);
        frame.setLocation(640, 480);
        frame.setVisible(true);

        frame.add(newtCanvasAWT);
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParentNativeWindow());
        // frame.pack();

        Animator animator1 = new Animator(glWindow1);
        animator1.start();

        int state = 0;
        while(animator1.isAnimating() && animator1.getDuration()<3*durationPerTest) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    glWindow1.reparentWindow(null, null);
                    Assert.assertEquals(true, glWindow1.isNativeWindowValid());
                    Assert.assertNull(glWindow1.getParentNativeWindow());
                    break;
                case 1:
                    glWindow1.reparentWindow(newtCanvasAWT.getNativeWindow(), null);
                    Assert.assertEquals(true, glWindow1.isNativeWindowValid());
                    Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParentNativeWindow());
                    break;
            }
            state++;
        }

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        frame.dispose();
        glWindow1.destroy(true);
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
        // glWindow1.setSize(600, 300);

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
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParentNativeWindow());
        // frame.pack();

        Animator animator1 = new Animator(glWindow1);
        animator1.start();

        int state = 0;
        while(animator1.isAnimating() && animator1.getDuration()<3*durationPerTest) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    glWindow1.reparentWindow(null, null);
                    Assert.assertEquals(true, glWindow1.isNativeWindowValid());
                    Assert.assertNull(glWindow1.getParentNativeWindow());
                    break;
                case 1:
                    glWindow1.reparentWindow(newtCanvasAWT.getNativeWindow(), null);
                    Assert.assertEquals(true, glWindow1.isNativeWindowValid());
                    Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParentNativeWindow());
                    break;
            }
            state++;
        }

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        frame.dispose();
        glWindow1.destroy(true);
    }

    @Test
    public void testWindowParenting05ReparentAWTWinHopFrame2Frame() throws InterruptedException {
        int x = 0;
        int y = 0;

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        GLWindow glWindow1 = GLWindow.create(glCaps, true);
        GLEventListener demo1 = new RedSquare();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        // glWindow1.setSize(600, 300);

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
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParentNativeWindow());
        // frame1.pack();

        Animator animator1 = new Animator(glWindow1);
        animator1.start();

        int state = 0;
        while(animator1.isAnimating() && animator1.getDuration()<3*durationPerTest) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    frame1.remove(newtCanvasAWT);
                    frame2.add(newtCanvasAWT, BorderLayout.CENTER);
                    //frame2.pack();
                    break;
                case 1:
                    frame2.remove(newtCanvasAWT);
                    frame1.add(newtCanvasAWT, BorderLayout.CENTER);
                    //frame1.pack();
                    break;
            }
            state++;
        }

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        frame1.dispose();
        frame2.dispose();
        glWindow1.destroy(true);
    }

    public static void setDemoFields(GLEventListener demo, GLWindow glWindow, boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        Window window = glWindow.getInnerWindow();
        if(debug) {
            MiscUtils.setFieldIfExists(demo, "glDebug", true);
            MiscUtils.setFieldIfExists(demo, "glTrace", true);
        }
        if(!MiscUtils.setFieldIfExists(demo, "window", window)) {
            MiscUtils.setFieldIfExists(demo, "glWindow", glWindow);
        }
    }

    static int atoi(String a) {
        int i=0;
        try {
            durationPerTest = Integer.parseInt(a);
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

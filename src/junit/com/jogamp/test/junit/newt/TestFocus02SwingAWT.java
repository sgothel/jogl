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

import java.awt.AWTException;
import java.awt.Button;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.Robot;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import javax.media.opengl.*;
import javax.media.nativewindow.*;

import com.jogamp.opengl.util.Animator;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;

import com.jogamp.test.junit.util.*;
import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;

public class TestFocus02SwingAWT {
    static int width, height;
    static long durationPerTest = 800;
    static long waitReparent = 0;
    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() throws AWTException {
        width  = 640;
        height = 480;

        JFrame f = new JFrame();
        f.setSize(100,100);
        f.setVisible(true);
        f.dispose();
        f=null;

        GLProfile.initSingleton();
        glCaps = new GLCapabilities(null);
    }

    
    private void testFocus01ProgrFocusImpl(Robot robot) throws InterruptedException, InvocationTargetException {
        int x = 0;
        int y = 0;

        /**
         * JFrame . JPanel . Container . NewtCanvasAWT . GLWindow
         */
        GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setTitle("testWindowParenting01CreateVisibleDestroy");
        GLEventListener demo1 = new Gears();
        glWindow1.addGLEventListener(demo1);
        NEWTFocusAdapter glWindow1FA = new NEWTFocusAdapter("GLWindow1");
        glWindow1.addWindowListener(glWindow1FA);

        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        AWTFocusAdapter newtCanvasAWTFA = new AWTFocusAdapter("NewtCanvasAWT");
        newtCanvasAWT.addFocusListener(newtCanvasAWTFA);

        Button buttonNorthInner = new Button("north");
        AWTFocusAdapter buttonNorthInnerFA = new AWTFocusAdapter("ButtonNorthInner");
        buttonNorthInner.addFocusListener(buttonNorthInnerFA);
        Container container1 = new Container();
        container1.setLayout(new BorderLayout());
        container1.add(buttonNorthInner, BorderLayout.NORTH);
        container1.add(new Button("south"), BorderLayout.SOUTH);
        container1.add(new Button("east"), BorderLayout.EAST);
        container1.add(new Button("west"), BorderLayout.WEST);
        container1.add(newtCanvasAWT, BorderLayout.CENTER);

        Button buttonNorthOuter = new Button("north");
        AWTFocusAdapter buttonNorthOuterFA = new AWTFocusAdapter("ButtonNorthOuter");
        buttonNorthOuter.addFocusListener(buttonNorthOuterFA);
        JPanel jPanel1 = new JPanel();
        jPanel1.setLayout(new BorderLayout());
        jPanel1.add(buttonNorthOuter, BorderLayout.NORTH);
        jPanel1.add(new Button("south"), BorderLayout.SOUTH);
        jPanel1.add(new Button("east"), BorderLayout.EAST);
        jPanel1.add(new Button("west"), BorderLayout.WEST);
        jPanel1.add(container1, BorderLayout.CENTER);

        JFrame jFrame1 = new JFrame("Swing Parent JFrame");
        AWTFocusAdapter jFrame1FA = new AWTFocusAdapter("JFrame1");
        jFrame1.addFocusListener(jFrame1FA);
        // jFrame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame1.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // equivalent to Frame, use windowClosing event!
        jFrame1.setContentPane(jPanel1);
        jFrame1.setSize(width, height);
        jFrame1.setVisible(true); // from here on, we need to run modifications on EDT

        int wait=0;
        while(wait<10 && glWindow1.getTotalFrames()<1) { Thread.sleep(100); wait++; }
        System.err.println("Frames for initial setVisible(true): "+glWindow1.getTotalFrames());
        Assert.assertTrue(0 < glWindow1.getTotalFrames());

        // Continuous animation ..
        Animator animator1 = new Animator(glWindow1);
        animator1.start();

        // Button Outer Focus
        Thread.sleep(100); // allow event sync
        System.err.println("FOCUS AWT  Button Outer request");
        AWTRobotUtil.requestFocus(robot, buttonNorthOuter);
        for (wait=0; wait<10 && !buttonNorthOuter.hasFocus(); wait++) {
            Thread.sleep(100);
        }
        Assert.assertTrue(buttonNorthOuter.hasFocus());
        Assert.assertFalse(newtCanvasAWT.getNEWTChild().hasFocus());
        Assert.assertFalse(newtCanvasAWT.hasFocus());
        Assert.assertEquals(0, glWindow1FA.getCount());
        Assert.assertEquals(0, newtCanvasAWTFA.getCount());
        Assert.assertEquals(1, buttonNorthOuterFA.getCount());
        Assert.assertEquals(0, buttonNorthInnerFA.getCount());
        Assert.assertEquals(0, jFrame1FA.getCount());
        System.err.println("FOCUS AWT  Button Outer sync");

        // NEWT Focus
        Thread.sleep(100); // allow event sync
        System.err.println("FOCUS NEWT Canvas/GLWindow request");
        AWTRobotUtil.requestFocus(robot, newtCanvasAWT);
        for (wait=0; wait<10 && !newtCanvasAWT.getNEWTChild().hasFocus(); wait++) {
            Thread.sleep(100);
        }
        Assert.assertTrue(newtCanvasAWT.getNEWTChild().hasFocus());
        Assert.assertFalse("AWT Frame has focus", jFrame1.hasFocus());
        Assert.assertFalse("AWT Button Inner has focus", buttonNorthInner.hasFocus());
        Assert.assertFalse("AWT Button Outer has focus", buttonNorthOuter.hasFocus());
        Assert.assertFalse("AWT parent canvas has focus", newtCanvasAWT.hasFocus());
        Assert.assertEquals(1, glWindow1FA.getCount());
        Assert.assertEquals(0, newtCanvasAWTFA.getCount());
        Assert.assertEquals(0, buttonNorthInnerFA.getCount());
        Assert.assertEquals(0, buttonNorthOuterFA.getCount());
        Assert.assertEquals(0, jFrame1FA.getCount());
        System.err.println("FOCUS NEWT Canvas/GLWindow sync");

        // Button Inner Focus
        Thread.sleep(100); // allow event sync
        System.err.println("FOCUS AWT  Button request");
        AWTRobotUtil.requestFocus(robot, buttonNorthInner);
        for (wait=0; wait<10 && !buttonNorthInner.hasFocus(); wait++) {
            Thread.sleep(100);
        }
        Assert.assertTrue(buttonNorthInner.hasFocus());
        Assert.assertFalse(buttonNorthOuter.hasFocus());
        Assert.assertFalse(newtCanvasAWT.getNEWTChild().hasFocus());
        Assert.assertFalse(newtCanvasAWT.hasFocus());
        Assert.assertEquals(0, glWindow1FA.getCount());
        Assert.assertEquals(0, newtCanvasAWTFA.getCount());
        Assert.assertEquals(1, buttonNorthInnerFA.getCount());
        Assert.assertEquals(0, buttonNorthOuterFA.getCount());
        Assert.assertEquals(0, jFrame1FA.getCount());
        System.err.println("FOCUS AWT  Button sync");

        // NEWT Focus
        Thread.sleep(100); // allow event sync
        System.err.println("FOCUS NEWT Canvas/GLWindow request");
        AWTRobotUtil.requestFocus(robot, newtCanvasAWT);
        for (wait=0; wait<10 && !newtCanvasAWT.getNEWTChild().hasFocus(); wait++) {
            Thread.sleep(100);
        }
        Assert.assertTrue(newtCanvasAWT.getNEWTChild().hasFocus());
        Assert.assertFalse("AWT Frame has focus", jFrame1.hasFocus());
        Assert.assertFalse("AWT Button has focus", buttonNorthInner.hasFocus());
        Assert.assertFalse("AWT Button Outer has focus", buttonNorthOuter.hasFocus());
        Assert.assertFalse("AWT parent canvas has focus", newtCanvasAWT.hasFocus());
        Assert.assertEquals(1, glWindow1FA.getCount());
        Assert.assertEquals(0, newtCanvasAWTFA.getCount());
        Assert.assertEquals(0, buttonNorthInnerFA.getCount());
        Assert.assertEquals(0, buttonNorthOuterFA.getCount());
        Assert.assertEquals(0, jFrame1FA.getCount());
        System.err.println("FOCUS NEWT Canvas/GLWindow sync");

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        final JFrame _jFrame1 = jFrame1;
        final JPanel _jPanel1 = jPanel1;
        final Container _container1 = container1;
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    _jFrame1.setVisible(false);
                    _jPanel1.remove(_container1);
                    _jFrame1.dispose();
                } });

        glWindow1.destroy(true);
    }

    @Test
    public void testFocus01ProgrFocus() throws InterruptedException, InvocationTargetException {
        testFocus01ProgrFocusImpl(null);
    }

    @Test
    public void testFocus02RobotFocus() throws AWTException, InterruptedException, InvocationTargetException {
        Robot robot = new Robot();
        robot.setAutoWaitForIdle(true);
        testFocus01ProgrFocusImpl(robot);
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
        System.err.println("durationPerTest "+durationPerTest);
        System.err.println("waitReparent "+waitReparent);
        String tstname = TestFocus02SwingAWT.class.getName();
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


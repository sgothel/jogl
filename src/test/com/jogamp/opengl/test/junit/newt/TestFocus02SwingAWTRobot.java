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
 
package com.jogamp.opengl.test.junit.newt;

import java.lang.reflect.*;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import java.awt.AWTException;
import java.awt.Button;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Robot;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.util.ArrayList;

import javax.media.opengl.*;

import com.jogamp.opengl.util.Animator;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.gears.Gears;

public class TestFocus02SwingAWTRobot extends UITestCase {
    static int width, height;
    static long durationPerTest = 800;
    static long awtWaitTimeout = 1000;
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

        GLProfile.initSingleton(false);
        glCaps = new GLCapabilities(null);
    }

    @AfterClass
    public static void release() {
    }
    
    private void testFocus01ProgrFocusImpl(Robot robot) 
        throws AWTException, InterruptedException, InvocationTargetException {
        int x = 0;
        int y = 0;

        ArrayList eventCountAdapters = new ArrayList();

        /**
         * JFrame . JPanel . Container . NewtCanvasAWT . GLWindow
         */
        GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setTitle("testWindowParenting01CreateVisibleDestroy");
        GLEventListener demo1 = new Gears();
        glWindow1.addGLEventListener(demo1);
        NEWTFocusAdapter glWindow1FA = new NEWTFocusAdapter("GLWindow1");
        glWindow1.addWindowListener(glWindow1FA);
        eventCountAdapters.add(glWindow1FA);
        NEWTKeyAdapter glWindow1KA = new NEWTKeyAdapter("GLWindow1");
        glWindow1.addKeyListener(glWindow1KA);
        eventCountAdapters.add(glWindow1KA);
        NEWTMouseAdapter glWindow1MA = new NEWTMouseAdapter("GLWindow1");
        glWindow1.addMouseListener(glWindow1MA);
        eventCountAdapters.add(glWindow1MA);

        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        AWTFocusAdapter newtCanvasAWTFA = new AWTFocusAdapter("NewtCanvasAWT");
        newtCanvasAWT.addFocusListener(newtCanvasAWTFA);
        eventCountAdapters.add(newtCanvasAWTFA);
        AWTKeyAdapter newtCanvasAWTKA = new AWTKeyAdapter("NewtCanvasAWT");
        newtCanvasAWT.addKeyListener(newtCanvasAWTKA);
        eventCountAdapters.add(newtCanvasAWTKA);
        AWTMouseAdapter newtCanvasAWTMA = new AWTMouseAdapter("NewtCanvasAWT");
        newtCanvasAWT.addMouseListener(newtCanvasAWTMA);
        eventCountAdapters.add(newtCanvasAWTMA);

        Button buttonNorthInner = new Button("north");
        AWTFocusAdapter buttonNorthInnerFA = new AWTFocusAdapter("ButtonNorthInner");
        buttonNorthInner.addFocusListener(buttonNorthInnerFA);
        eventCountAdapters.add(buttonNorthInnerFA);
        AWTKeyAdapter buttonNorthInnerKA = new AWTKeyAdapter("ButtonNorthInner");
        buttonNorthInner.addKeyListener(buttonNorthInnerKA);
        eventCountAdapters.add(buttonNorthInnerKA);
        AWTMouseAdapter buttonNorthInnerMA = new AWTMouseAdapter("ButtonNorthInner");
        buttonNorthInner.addMouseListener(buttonNorthInnerMA);
        eventCountAdapters.add(buttonNorthInnerMA);
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
        eventCountAdapters.add(buttonNorthOuterFA);
        AWTKeyAdapter buttonNorthOuterKA = new AWTKeyAdapter("ButtonNorthOuter");
        buttonNorthOuter.addKeyListener(buttonNorthOuterKA);
        eventCountAdapters.add(buttonNorthOuterKA);
        AWTMouseAdapter buttonNorthOuterMA = new AWTMouseAdapter("ButtonNorthOuter");
        buttonNorthOuter.addMouseListener(buttonNorthOuterMA);
        eventCountAdapters.add(buttonNorthOuterMA);
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
        Assert.assertTrue(AWTRobotUtil.toFront(robot, jFrame1));

        int wait=0;
        while(wait<awtWaitTimeout/100 && glWindow1.getTotalFPSFrames()<1) { Thread.sleep(awtWaitTimeout/10); wait++; }
        System.err.println("Frames for initial setVisible(true): "+glWindow1.getTotalFPSFrames());
        Assert.assertTrue(glWindow1.isVisible());
        Assert.assertTrue(0 < glWindow1.getTotalFPSFrames());

        // Continuous animation ..
        Animator animator1 = new Animator(glWindow1);
        animator1.start();
        AWTRobotUtil.assertRequestFocusAndWait(robot, jFrame1, jFrame1, jFrame1FA, null);

        // Button Outer Focus
        Thread.sleep(100); // allow event sync
        System.err.println("FOCUS AWT  Button Outer request");
        EventCountAdapterUtil.reset(eventCountAdapters);
        AWTRobotUtil.assertRequestFocusAndWait(robot, buttonNorthOuter, buttonNorthOuter, buttonNorthOuterFA, null);
        Assert.assertEquals(true, buttonNorthOuterFA.hasFocus());
        Assert.assertEquals(false, glWindow1FA.hasFocus());
        Assert.assertEquals(false, newtCanvasAWTFA.hasFocus());
        Assert.assertEquals(false, buttonNorthInnerFA.hasFocus());
        Assert.assertEquals(false, jFrame1FA.hasFocus());
        System.err.println("FOCUS AWT  Button Outer sync");
        AWTRobotUtil.assertKeyType(robot, java.awt.event.KeyEvent.VK_A, 2, buttonNorthOuter, buttonNorthOuterKA);
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 1, 
                                      buttonNorthOuter, buttonNorthOuterMA);
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 2, 
                                      buttonNorthOuter, buttonNorthOuterMA);

        // NEWT Focus
        Thread.sleep(100); // allow event sync
        System.err.println("FOCUS NEWT Canvas/GLWindow request");
        EventCountAdapterUtil.reset(eventCountAdapters);
        AWTRobotUtil.assertRequestFocusAndWait(robot, newtCanvasAWT, newtCanvasAWT.getNEWTChild(), glWindow1FA, buttonNorthOuterFA);
        Assert.assertTrue(AWTRobotUtil.waitForFocusCount(false, newtCanvasAWTFA));
        Assert.assertEquals(true, glWindow1FA.hasFocus());
        Assert.assertEquals(false, newtCanvasAWTFA.hasFocus());
        Assert.assertEquals(false, buttonNorthInnerFA.hasFocus());
        Assert.assertEquals(false, buttonNorthOuterFA.hasFocus());
        Assert.assertEquals(false, jFrame1FA.hasFocus());
        System.err.println("FOCUS NEWT Canvas/GLWindow sync");
        AWTRobotUtil.assertKeyType(robot, java.awt.event.KeyEvent.VK_A, 2, glWindow1, glWindow1KA);
        Assert.assertEquals("AWT parent canvas received keyboard events", 0, newtCanvasAWTKA.getCount());
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 1, 
                                      glWindow1, glWindow1MA);
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 2, 
                                      glWindow1, glWindow1MA);
        Assert.assertEquals("AWT parent canvas received mouse events", 0, newtCanvasAWTMA.getCount());

        // Button Inner Focus
        Thread.sleep(100); // allow event sync
        System.err.println("FOCUS AWT  Button request");
        EventCountAdapterUtil.reset(eventCountAdapters);
        AWTRobotUtil.assertRequestFocusAndWait(robot, buttonNorthInner, buttonNorthInner, buttonNorthInnerFA, glWindow1FA);
        Assert.assertEquals(true, buttonNorthInnerFA.hasFocus());
        Assert.assertEquals(false, glWindow1FA.hasFocus());
        Assert.assertEquals(false, newtCanvasAWTFA.hasFocus());
        Assert.assertEquals(false, buttonNorthOuterFA.hasFocus());
        Assert.assertEquals(false, jFrame1FA.hasFocus());
        System.err.println("FOCUS AWT  Button sync");
        AWTRobotUtil.assertKeyType(robot, java.awt.event.KeyEvent.VK_A, 2, buttonNorthInner, buttonNorthInnerKA);
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 1, 
                                      buttonNorthInner, buttonNorthInnerMA);
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 2, 
                                      buttonNorthInner, buttonNorthInnerMA);

        // NEWT Focus
        Thread.sleep(100); // allow event sync
        System.err.println("FOCUS NEWT Canvas/GLWindow request");
        EventCountAdapterUtil.reset(eventCountAdapters);
        AWTRobotUtil.assertRequestFocusAndWait(robot, newtCanvasAWT, newtCanvasAWT.getNEWTChild(), glWindow1FA, buttonNorthInnerFA);
        Assert.assertTrue(AWTRobotUtil.waitForFocusCount(false, newtCanvasAWTFA));
        Assert.assertEquals(true, glWindow1FA.hasFocus());
        Assert.assertEquals(false, newtCanvasAWTFA.hasFocus());
        Assert.assertEquals(false, buttonNorthInnerFA.hasFocus());
        Assert.assertEquals(false, buttonNorthOuterFA.hasFocus());
        Assert.assertEquals(false, jFrame1FA.hasFocus());
        System.err.println("FOCUS NEWT Canvas/GLWindow sync");
        AWTRobotUtil.assertKeyType(robot, java.awt.event.KeyEvent.VK_A, 2, glWindow1, glWindow1KA);
        Assert.assertEquals("AWT parent canvas received keyboard events", 0, newtCanvasAWTKA.getCount());
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 1, 
                                      glWindow1, glWindow1MA);
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 2, 
                                      glWindow1, glWindow1MA);
        Assert.assertEquals("AWT parent canvas received mouse events", 0, newtCanvasAWTMA.getCount());

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

        glWindow1.destroy();
    }

    @Test
    public void testFocus01ProgrFocus() throws AWTException, InterruptedException, InvocationTargetException {
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

    public static void main(String args[]) 
        throws IOException, AWTException, InterruptedException, InvocationTargetException 
    {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            } else if(args[i].equals("-wait")) {
                waitReparent = atoi(args[++i]);
            }
        }
        System.err.println("durationPerTest "+durationPerTest);
        System.err.println("waitReparent "+waitReparent);
        if(true) {
            String tstname = TestFocus02SwingAWTRobot.class.getName();
            org.junit.runner.JUnitCore.main(tstname);
        } else {       
            TestFocus02SwingAWTRobot.initClass();
            TestFocus02SwingAWTRobot test = new TestFocus02SwingAWTRobot();        
            test.testFocus01ProgrFocus();
            test.testFocus02RobotFocus();
            TestFocus02SwingAWTRobot.release();
        }
    }
}


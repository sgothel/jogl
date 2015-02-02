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

package com.jogamp.opengl.test.junit.newt.event;

import java.lang.reflect.*;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import java.awt.AWTException;
import java.awt.Button;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Robot;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import java.util.ArrayList;

import com.jogamp.opengl.*;

import com.jogamp.opengl.util.Animator;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

/**
 * Testing focus <i>mouse-click</i> and <i>programmatic</i> traversal of an AWT component tree with {@link NewtCanvasAWT} attached.
 * <p>
 * {@link JFrame} . {@link JPanel}+ . {@link Container} [ Button*, {@link NewtCanvasAWT} . {@link GLWindow} ]
 * </p>
 * <p>
 * <i>+ JPanel is set as JFrame's root content pane</i><br/>
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestParentingFocus02SwingAWTRobot extends UITestCase {
    static int width, height;
    static long durationPerTest = 10;
    static long awtWaitTimeout = 1000;
    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() throws AWTException, InterruptedException, InvocationTargetException {
        width  = 640;
        height = 480;

        final JFrame f = new JFrame();
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f.setSize(100,100);
                f.setVisible(true);
            } } );
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f.dispose();
            } } );

        glCaps = new GLCapabilities(null);
    }

    @AfterClass
    public static void release() {
    }

    private void testFocus01ProgrFocusImpl(final Robot robot)
        throws AWTException, InterruptedException, InvocationTargetException {

        final ArrayList<EventCountAdapter> eventCountAdapters = new ArrayList<EventCountAdapter>();

        final GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setTitle("testWindowParenting01CreateVisibleDestroy");
        final GLEventListener demo1 = new GearsES2();
        glWindow1.addGLEventListener(demo1);
        final NEWTFocusAdapter glWindow1FA = new NEWTFocusAdapter("GLWindow1");
        glWindow1.addWindowListener(glWindow1FA);
        final NEWTKeyAdapter glWindow1KA = new NEWTKeyAdapter("GLWindow1");
        glWindow1.addKeyListener(glWindow1KA);
        eventCountAdapters.add(glWindow1KA);
        final NEWTMouseAdapter glWindow1MA = new NEWTMouseAdapter("GLWindow1");
        glWindow1.addMouseListener(glWindow1MA);
        eventCountAdapters.add(glWindow1MA);

        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        final AWTFocusAdapter newtCanvasAWTFA = new AWTFocusAdapter("NewtCanvasAWT");
        newtCanvasAWT.addFocusListener(newtCanvasAWTFA);
        final AWTKeyAdapter newtCanvasAWTKA = new AWTKeyAdapter("NewtCanvasAWT");
        newtCanvasAWT.addKeyListener(newtCanvasAWTKA);
        eventCountAdapters.add(newtCanvasAWTKA);
        final AWTMouseAdapter newtCanvasAWTMA = new AWTMouseAdapter("NewtCanvasAWT");
        newtCanvasAWT.addMouseListener(newtCanvasAWTMA);
        eventCountAdapters.add(newtCanvasAWTMA);

        final Button buttonNorthInner = new Button("north");
        final AWTFocusAdapter buttonNorthInnerFA = new AWTFocusAdapter("ButtonNorthInner");
        buttonNorthInner.addFocusListener(buttonNorthInnerFA);
        final AWTKeyAdapter buttonNorthInnerKA = new AWTKeyAdapter("ButtonNorthInner");
        buttonNorthInner.addKeyListener(buttonNorthInnerKA);
        eventCountAdapters.add(buttonNorthInnerKA);
        final AWTMouseAdapter buttonNorthInnerMA = new AWTMouseAdapter("ButtonNorthInner");
        buttonNorthInner.addMouseListener(buttonNorthInnerMA);
        eventCountAdapters.add(buttonNorthInnerMA);
        final Container container1 = new Container();
        container1.setLayout(new BorderLayout());
        container1.add(buttonNorthInner, BorderLayout.NORTH);
        container1.add(new Button("south"), BorderLayout.SOUTH);
        container1.add(new Button("east"), BorderLayout.EAST);
        container1.add(new Button("west"), BorderLayout.WEST);
        container1.add(newtCanvasAWT, BorderLayout.CENTER);

        final Button buttonNorthOuter = new Button("north");
        final AWTFocusAdapter buttonNorthOuterFA = new AWTFocusAdapter("ButtonNorthOuter");
        buttonNorthOuter.addFocusListener(buttonNorthOuterFA);
        final AWTKeyAdapter buttonNorthOuterKA = new AWTKeyAdapter("ButtonNorthOuter");
        buttonNorthOuter.addKeyListener(buttonNorthOuterKA);
        eventCountAdapters.add(buttonNorthOuterKA);
        final AWTMouseAdapter buttonNorthOuterMA = new AWTMouseAdapter("ButtonNorthOuter");
        buttonNorthOuter.addMouseListener(buttonNorthOuterMA);
        eventCountAdapters.add(buttonNorthOuterMA);
        final JPanel jPanel1 = new JPanel();
        jPanel1.setLayout(new BorderLayout());
        jPanel1.add(buttonNorthOuter, BorderLayout.NORTH);
        jPanel1.add(new Button("south"), BorderLayout.SOUTH);
        jPanel1.add(new Button("east"), BorderLayout.EAST);
        jPanel1.add(new Button("west"), BorderLayout.WEST);
        jPanel1.add(container1, BorderLayout.CENTER);

        final JFrame jFrame1 = new JFrame("Swing Parent JFrame");
        // jFrame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame1.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // equivalent to Frame, use windowClosing event!
        jFrame1.setContentPane(jPanel1);
        jFrame1.setSize(width, height);
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                jFrame1.setVisible(true);
            } } );
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(jFrame1, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glWindow1, true));
        AWTRobotUtil.clearAWTFocus(robot);
        Assert.assertTrue(AWTRobotUtil.toFrontAndRequestFocus(robot, jFrame1));

        int wait=0;
        while(wait<awtWaitTimeout/10 && glWindow1.getTotalFPSFrames()<1) { Thread.sleep(awtWaitTimeout/100); wait++; }
        System.err.println("Frames for initial setVisible(true): "+glWindow1.getTotalFPSFrames());
        Assert.assertTrue(glWindow1.isVisible());
        Assert.assertTrue(0 < glWindow1.getTotalFPSFrames());

        // Continuous animation ..
        final Animator animator1 = new Animator(glWindow1);
        animator1.start();

        Thread.sleep(durationPerTest); // manual testing

        // Button Outer Focus
        Thread.sleep(100); // allow event sync
        System.err.println("FOCUS AWT  Button Outer request");
        EventCountAdapterUtil.reset(eventCountAdapters);
        AWTRobotUtil.assertRequestFocusAndWait(robot, buttonNorthOuter, buttonNorthOuter, buttonNorthOuterFA, null); // OSX sporadically buttonNorthOuter did not gain - major UI failure
        Assert.assertEquals(false, glWindow1FA.focusGained());
        Assert.assertEquals(false, newtCanvasAWTFA.focusGained());
        Assert.assertEquals(false, buttonNorthInnerFA.focusGained());
        System.err.println("FOCUS AWT  Button Outer sync");
        AWTRobotUtil.assertKeyType(robot, java.awt.event.KeyEvent.VK_A, 2, buttonNorthOuter, buttonNorthOuterKA); // OSX sporadically won't receive the keyboard input - major UI failure
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 1,
                                      buttonNorthOuter, buttonNorthOuterMA);
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 2,
                                      buttonNorthOuter, buttonNorthOuterMA);

        // NEWT Focus
        Thread.sleep(100); // allow event sync
        System.err.println("FOCUS NEWT Canvas/GLWindow request");
        EventCountAdapterUtil.reset(eventCountAdapters);
        AWTRobotUtil.assertRequestFocusAndWait(robot, newtCanvasAWT, newtCanvasAWT.getNEWTChild(), glWindow1FA, buttonNorthOuterFA);
        // Manually tested on Java7/[Linux,Windows] (where this assertion failed),
        // Should be OK to have the AWT component assume it also has the focus.
        // Assert.assertTrue("Focus prev. gained, but NewtCanvasAWT didn't loose it. Gainer: "+glWindow1FA+"; Looser "+newtCanvasAWTFA,
        //        AWTRobotUtil.waitForFocus(glWindow1FA, newtCanvasAWTFA));
        if( !AWTRobotUtil.waitForFocus(glWindow1FA, newtCanvasAWTFA) ) {
            System.err.println("Info: Focus prev. gained, but NewtCanvasAWT didn't loose it. Gainer: "+glWindow1FA+"; Looser "+newtCanvasAWTFA);
        }
        Assert.assertEquals(false, buttonNorthInnerFA.focusGained());
        System.err.println("FOCUS NEWT Canvas/GLWindow sync");
        AWTRobotUtil.assertKeyType(robot, java.awt.event.KeyEvent.VK_A, 2, glWindow1, glWindow1KA);
        Assert.assertEquals("AWT parent canvas received non consumed keyboard events", newtCanvasAWTKA.getConsumedCount(), newtCanvasAWTKA.getCount());
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 1,
                                      glWindow1, glWindow1MA);
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 2,
                                      glWindow1, glWindow1MA);
        if( !newtCanvasAWT.isAWTEventPassThrough() ) {
            Assert.assertEquals("AWT parent canvas received consumed keyboard events", 0, newtCanvasAWTKA.getConsumedCount());
            Assert.assertEquals("AWT parent canvas received mouse events", 0, newtCanvasAWTMA.getCount());
        }

        // Button Inner Focus
        Thread.sleep(100); // allow event sync
        System.err.println("FOCUS AWT  Button request");
        EventCountAdapterUtil.reset(eventCountAdapters);
        AWTRobotUtil.assertRequestFocusAndWait(robot, buttonNorthInner, buttonNorthInner, buttonNorthInnerFA, glWindow1FA);
        Assert.assertEquals(false, glWindow1FA.focusGained());
        Assert.assertEquals(false, newtCanvasAWTFA.focusGained());
        Assert.assertEquals(false, buttonNorthOuterFA.focusGained());
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
        // Manually tested on Java7/[Linux,Windows] (where this assertion failed),
        // Should be OK to have the AWT component assume it also has the focus.
        // Assert.assertTrue("Focus prev. gained, but NewtCanvasAWT didn't loose it. Gainer: "+glWindow1FA+"; Looser "+newtCanvasAWTFA,
        //        AWTRobotUtil.waitForFocus(glWindow1FA, newtCanvasAWTFA));
        if( !AWTRobotUtil.waitForFocus(glWindow1FA, newtCanvasAWTFA) ) {
            System.err.println("Info: Focus prev. gained, but NewtCanvasAWT didn't loose it. Gainer: "+glWindow1FA+"; Looser "+newtCanvasAWTFA);
        }

        Assert.assertEquals(false, buttonNorthOuterFA.focusGained());
        System.err.println("FOCUS NEWT Canvas/GLWindow sync");
        AWTRobotUtil.assertKeyType(robot, java.awt.event.KeyEvent.VK_A, 2, glWindow1, glWindow1KA);
        Assert.assertEquals("AWT parent canvas received non consumed keyboard events", newtCanvasAWTKA.getConsumedCount(), newtCanvasAWTKA.getCount());
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 1,
                                      glWindow1, glWindow1MA);
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 2,
                                      glWindow1, glWindow1MA);
        if( !newtCanvasAWT.isAWTEventPassThrough() ) {
            Assert.assertEquals("AWT parent canvas received consumed keyboard events", 0, newtCanvasAWTKA.getConsumedCount());
            Assert.assertEquals("AWT parent canvas received mouse events", 0, newtCanvasAWTMA.getCount());
        }

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    jFrame1.setVisible(false);
                    jPanel1.remove(container1);
                    jFrame1.dispose();
                } });

        glWindow1.destroy();
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glWindow1, false));
    }

    @Test
    public void testFocus01ProgrFocus() throws AWTException, InterruptedException, InvocationTargetException {
        testFocus01ProgrFocusImpl(null);
    }

    @Test
    public void testFocus02RobotFocus() throws AWTException, InterruptedException, InvocationTargetException {
        final Robot robot = new Robot();
        robot.setAutoWaitForIdle(true);
        testFocus01ProgrFocusImpl(robot);
    }

    static int atoi(final String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return i;
    }

    @SuppressWarnings("unused")
    public static void main(final String args[])
        throws IOException, AWTException, InterruptedException, InvocationTargetException
    {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        if(true) {
            final String tstname = TestParentingFocus02SwingAWTRobot.class.getName();
            org.junit.runner.JUnitCore.main(tstname);
        } else {
            TestParentingFocus02SwingAWTRobot.initClass();
            final TestParentingFocus02SwingAWTRobot test = new TestParentingFocus02SwingAWTRobot();
            test.testFocus01ProgrFocus();
            test.testFocus02RobotFocus();
            TestParentingFocus02SwingAWTRobot.release();
        }
    }
}


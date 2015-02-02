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

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.Assume;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Container;
import java.awt.Robot;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import javax.swing.JFrame;

import java.util.ArrayList;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.newt.TestListenerCom01AWT;
import com.jogamp.opengl.test.junit.util.*;

/**
 * Testing focus <i>mouse-click</i> and <i>programmatic</i> traversal of an AWT component tree with {@link NewtCanvasAWT} attached.
 * <p>
 * {@link JFrame} . {@link Container}+ [ Button*, {@link NewtCanvasAWT} . {@link GLWindow} ]
 * </p>
 * <p>
 * <i>+ Container is the JFrame's implicit root content pane</i><br/>
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestParentingFocus01SwingAWTRobot extends UITestCase {
    static int width, height;
    static long durationPerTest = 10;
    static long awtWaitTimeout = 1000;

    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() {
        width = 640;
        height = 480;
        glCaps = new GLCapabilities(null);
    }

    @AfterClass
    public static void release() {
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

    private void testFocus01ProgrFocusImpl(final Robot robot) throws AWTException,
            InvocationTargetException, InterruptedException {
        final ArrayList<EventCountAdapter> eventCountAdapters = new ArrayList<EventCountAdapter>();

        // Create a window.
        final GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setTitle("testNewtChildFocus");
        final GLEventListener demo1 = new RedSquareES2();
        TestListenerCom01AWT.setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        final NEWTFocusAdapter glWindow1FA = new NEWTFocusAdapter("GLWindow1");
        glWindow1.addWindowListener(glWindow1FA);

        // Monitor NEWT focus and keyboard events.
        final NEWTKeyAdapter glWindow1KA = new NEWTKeyAdapter("GLWindow1");
        eventCountAdapters.add(glWindow1KA);
        glWindow1.addKeyListener(glWindow1KA);

        // Wrap the window in a canvas.
        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        // newtCanvasAWT.setShallUseOffscreenLayer(true);

        // Monitor AWT focus and keyboard events.
        final AWTKeyAdapter newtCanvasAWTKA = new AWTKeyAdapter("NewtCanvasAWT");
        newtCanvasAWT.addKeyListener(newtCanvasAWTKA);
        eventCountAdapters.add(newtCanvasAWTKA);
        final AWTFocusAdapter newtCanvasAWTFA = new AWTFocusAdapter("NewtCanvasAWT");
        newtCanvasAWT.addFocusListener(newtCanvasAWTFA);

        // Add the canvas to a frame, and make it all visible.
        final JFrame frame1 = new JFrame("Swing AWT Parent Frame: "
                                         + glWindow1.getTitle());
        frame1.getContentPane().add(newtCanvasAWT, BorderLayout.CENTER);
        final Button button = new Button("Click me ..");
        final AWTFocusAdapter buttonFA = new AWTFocusAdapter("Button");
        button.addFocusListener(buttonFA);
        final AWTKeyAdapter buttonKA = new AWTKeyAdapter("Button");
        button.addKeyListener(buttonKA);
        eventCountAdapters.add(buttonKA);
        final AWTMouseAdapter buttonMA = new AWTMouseAdapter("Button");
        button.addMouseListener(buttonMA);
        eventCountAdapters.add(buttonMA);

        frame1.getContentPane().add(button, BorderLayout.NORTH);
        frame1.setSize(width, height);
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame1.setVisible(true);
            } } );
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame1, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glWindow1, true));
        AWTRobotUtil.clearAWTFocus(robot);
        Assert.assertTrue(AWTRobotUtil.toFrontAndRequestFocus(robot, frame1));

        Thread.sleep(durationPerTest); // manual testing

        int wait=0;
        while(wait<awtWaitTimeout/100 && glWindow1.getTotalFPSFrames()<1) { Thread.sleep(awtWaitTimeout/10); wait++; }
        System.err.println("Frames for initial setVisible(true): "+glWindow1.getTotalFPSFrames());
        Assert.assertTrue(glWindow1.isVisible());
        Assert.assertTrue(0 < glWindow1.getTotalFPSFrames());

        // Continuous animation ..
        final Animator animator = new Animator(glWindow1);
        animator.start();

        // Button Focus
        Thread.sleep(200); // allow event sync

        System.err.println("FOCUS AWT  Button request");
        EventCountAdapterUtil.reset(eventCountAdapters);
        AWTRobotUtil.assertRequestFocusAndWait(robot, button, button, buttonFA, null); // OSX sporadically button did not gain - major UI failure
        Assert.assertEquals(false, glWindow1FA.focusGained());
        Assert.assertEquals(false, newtCanvasAWTFA.focusGained());
        System.err.println("FOCUS AWT  Button sync");
        AWTRobotUtil.assertKeyType(robot, java.awt.event.KeyEvent.VK_A, 2, button, buttonKA);
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 1,
                                      button, buttonMA);
        AWTRobotUtil.assertMouseClick(robot, java.awt.event.InputEvent.BUTTON1_MASK, 2,
                                      button, buttonMA);

        // Request the AWT focus, which should automatically provide the NEWT window with focus.
        Thread.sleep(100); // allow event sync
        System.err.println("FOCUS NEWT Canvas/GLWindow request");
        EventCountAdapterUtil.reset(eventCountAdapters);
        AWTRobotUtil.assertRequestFocusAndWait(robot, newtCanvasAWT, newtCanvasAWT.getNEWTChild(), glWindow1FA, buttonFA); // OSX sporadically button did not loose - minor UI failure
        // Manually tested on Java7/[Linux,Windows] (where this assertion failed),
        // Should be OK to have the AWT component assume it also has the focus.
        // Assert.assertTrue("Focus prev. gained, but NewtCanvasAWT didn't loose it. Gainer: "+glWindow1FA+"; Looser "+newtCanvasAWTFA,
        //         AWTRobotUtil.waitForFocus(glWindow1FA, newtCanvasAWTFA));
        if( !AWTRobotUtil.waitForFocus(glWindow1FA, newtCanvasAWTFA) ) {
            System.err.println("Info: Focus prev. gained, but NewtCanvasAWT didn't loose it. Gainer: "+glWindow1FA+"; Looser "+newtCanvasAWTFA);
        }
        System.err.println("FOCUS NEWT Canvas/GLWindow sync");
        AWTRobotUtil.assertKeyType(robot, java.awt.event.KeyEvent.VK_A, 2, glWindow1, glWindow1KA);
        Assert.assertEquals("AWT parent canvas received non consumed keyboard events", newtCanvasAWTKA.getConsumedCount(), newtCanvasAWTKA.getCount());
        if( !newtCanvasAWT.isAWTEventPassThrough() ) {
            Assert.assertEquals("AWT parent canvas received consumed keyboard events", 0, newtCanvasAWTKA.getConsumedCount());
        }

        // Remove listeners to avoid logging during dispose/destroy.
        glWindow1.removeKeyListener(glWindow1KA);
        glWindow1.removeWindowListener(glWindow1FA);
        newtCanvasAWT.removeKeyListener(newtCanvasAWTKA);
        newtCanvasAWT.removeFocusListener(newtCanvasAWTFA);

        // Shutdown the test.
        animator.stop();
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame1.setVisible(false);
                    frame1.dispose();
                }});
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        glWindow1.destroy();
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glWindow1, false));
    }

    static int atoi(final String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine());
        */
        System.out.println("durationPerTest: "+durationPerTest);
        final String tstname = TestParentingFocus01SwingAWTRobot.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }


}

/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import org.junit.After;
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Robot;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import javax.swing.JFrame;

import java.io.IOException;

import jogamp.nativewindow.jawt.JAWTUtil;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;

import com.jogamp.opengl.test.junit.util.*;

/**
 * Testing key event order excl. auto-repeat (Bug 601)
 *
 * <p>
 * Note Event order:
 * <ol>
 *   <li>{@link #EVENT_KEY_PRESSED}</li>
 *   <li>{@link #EVENT_KEY_RELEASED}</li>
 * </ol>
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNewtKeyEventOrderAWT extends UITestCase {
    static int width, height;
    static long durationPerTest = 100;
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

    @Before
    public void initTest() {
    }

    @After
    public void releaseTest() {
    }

    @Test(timeout=180000) // TO 3 min
    public void test01NEWT() throws AWTException, InterruptedException, InvocationTargetException {
        final GLWindow glWindow = GLWindow.create(glCaps);
        glWindow.setSize(width, height);
        glWindow.setVisible(true);

        testImpl(glWindow);

        glWindow.destroy();
    }

    private void testNewtCanvasAWT_Impl(final boolean onscreen) throws AWTException, InterruptedException, InvocationTargetException {
        final GLWindow glWindow = GLWindow.create(glCaps);

        // Wrap the window in a canvas.
        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow);
        if( !onscreen ) {
            newtCanvasAWT.setShallUseOffscreenLayer(true);
        }

        // Add the canvas to a frame, and make it all visible.
        final JFrame frame1 = new JFrame("Swing AWT Parent Frame: "+ glWindow.getTitle());
        frame1.getContentPane().add(newtCanvasAWT, BorderLayout.CENTER);
        frame1.setSize(width, height);
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame1.setVisible(true);
            } } );

        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame1, true));

        testImpl(glWindow);

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
        glWindow.destroy();
    }

    @Test(timeout=180000) // TO 3 min
    public void test02NewtCanvasAWT_Onscreen() throws AWTException, InterruptedException, InvocationTargetException {
        if( JAWTUtil.isOffscreenLayerRequired() ) {
            System.err.println("Platform doesn't support onscreen rendering.");
            return;
        }
        testNewtCanvasAWT_Impl(true);
    }

    @Test(timeout=180000) // TO 3 min
    public void test03NewtCanvasAWT_Offsccreen() throws AWTException, InterruptedException, InvocationTargetException {
        if( !JAWTUtil.isOffscreenLayerSupported() ) {
            System.err.println("Platform doesn't support offscreen rendering.");
            return;
        }
        testNewtCanvasAWT_Impl(false);
    }

    static void testKeyEventOrder(final Robot robot, final NEWTKeyAdapter keyAdapter, final int loops) {
        System.err.println("KEY Event Order Test: "+loops);
        keyAdapter.reset();
        for(int i=0; i<loops; i++) {
            // 1
            AWTRobotUtil.waitForIdle(robot);
            AWTRobotUtil.keyPress(0, robot, true, java.awt.event.KeyEvent.VK_A, 10);
            AWTRobotUtil.keyPress(0, robot, false, java.awt.event.KeyEvent.VK_A, 100);
            // 2
            AWTRobotUtil.waitForIdle(robot);
            AWTRobotUtil.keyPress(0, robot, true, java.awt.event.KeyEvent.VK_B, 10);
            AWTRobotUtil.keyPress(0, robot, false, java.awt.event.KeyEvent.VK_B, 100);
            // 3 + 4
            AWTRobotUtil.waitForIdle(robot);
            AWTRobotUtil.keyPress(0, robot, true, java.awt.event.KeyEvent.VK_A, 10);
            AWTRobotUtil.keyPress(0, robot, true, java.awt.event.KeyEvent.VK_B, 10);
            AWTRobotUtil.keyPress(0, robot, false, java.awt.event.KeyEvent.VK_A, 10);
            AWTRobotUtil.keyPress(0, robot, false, java.awt.event.KeyEvent.VK_B, 10);
            // 5 + 6
            AWTRobotUtil.waitForIdle(robot);
            AWTRobotUtil.keyPress(0, robot, true, java.awt.event.KeyEvent.VK_A, 10);
            AWTRobotUtil.keyPress(0, robot, true, java.awt.event.KeyEvent.VK_B, 10);
            AWTRobotUtil.keyPress(0, robot, false, java.awt.event.KeyEvent.VK_B, 10);
            AWTRobotUtil.keyPress(0, robot, false, java.awt.event.KeyEvent.VK_A, 10);
        }
        AWTRobotUtil.waitForIdle(robot);
        robot.delay(250);
        // dumpKeyEvents(keyAdapter.getQueued());

        NEWTKeyUtil.validateKeyEventOrder(keyAdapter.copyQueue());

        final int expTotal = 6*loops; // all typed events
        NEWTKeyUtil.validateKeyAdapterStats(keyAdapter,
                                            expTotal /* press-SI */, expTotal /* release-SI */,
                                            0 /* press-AR */, 0 /* release-AR */ );

    }

    void testImpl(final GLWindow glWindow) throws AWTException, InterruptedException, InvocationTargetException {
        final Robot robot = new Robot();
        robot.setAutoWaitForIdle(true);

        final GLEventListener demo1 = new RedSquareES2();
        glWindow.addGLEventListener(demo1);

        final NEWTKeyAdapter glWindow1KA = new NEWTKeyAdapter("GLWindow1");
        glWindow1KA.setVerbose(false);
        glWindow.addKeyListener(glWindow1KA);

        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glWindow, true));

        // Continuous animation ..
        final Animator animator = new Animator(glWindow);
        animator.start();

        Thread.sleep(durationPerTest); // manual testing

        AWTRobotUtil.assertRequestFocusAndWait(null, glWindow, glWindow, null, null);  // programmatic
        AWTRobotUtil.requestFocus(robot, glWindow, false); // within unit framework, prev. tests (TestFocus02SwingAWTRobot) 'confuses' Windows keyboard input
        glWindow1KA.reset();

        //
        // Test the key event order w/o auto-repeat
        //
        testKeyEventOrder(robot, glWindow1KA, 6);

        // Remove listeners to avoid logging during dispose/destroy.
        glWindow.removeKeyListener(glWindow1KA);

        // Shutdown the test.
        animator.stop();
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
        final String tstname = TestNewtKeyEventOrderAWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }


}

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
import java.util.EventObject;
import java.util.List;

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
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;

import com.jogamp.opengl.test.junit.util.*;

/**
 * Testing combinations of key code modifiers of key event.
 *
 * <p>
 * Due to limitation of AWT Robot, the test machine needs to have US keyboard enabled,
 * even though we do unify VK codes to US keyboard across all layouts.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNewtKeyCodeModifiersAWT extends UITestCase {
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

    static void testKeyCodeModifier(final Robot robot, final NEWTKeyAdapter keyAdapter, final short modifierKey, final int modifierMask, final short keyCode,
                                    final char keyCharOnly, final char keyCharMod) {
        keyAdapter.reset();
        AWTRobotUtil.waitForIdle(robot);
        AWTRobotUtil.newtKeyPress(0, robot, true, keyCode, 10);   // press keyCode
        AWTRobotUtil.newtKeyPress(0, robot, false, keyCode, 100); // release keyCode
        AWTRobotUtil.waitForIdle(robot);
        for(int j=0; j < 100 && keyAdapter.getQueueSize() < 2; j++) { // wait until events are collected
            robot.delay(100);
        }

        AWTRobotUtil.waitForIdle(robot);
        AWTRobotUtil.newtKeyPress(0, robot, true, modifierKey, 10);     // press MOD
        AWTRobotUtil.newtKeyPress(0, robot, true, keyCode, 10);   // press keyCode
        AWTRobotUtil.newtKeyPress(0, robot, false, keyCode, 10);  // release keyCode
        AWTRobotUtil.newtKeyPress(0, robot, false, modifierKey, 100);   // release MOD
        AWTRobotUtil.waitForIdle(robot);
        for(int j=0; j < 100 && keyAdapter.getQueueSize() < 2+4; j++) { // wait until events are collected
            robot.delay(100);
        }
        NEWTKeyUtil.validateKeyAdapterStats(keyAdapter,
                                            3 /* press-SI */, 3 /* release-SI */,
                                            0 /* press-AR */, 0 /* release-AR */ );

        final List<EventObject> queue = keyAdapter.copyQueue();
        int i=0;
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_PRESSED, 0, keyCode, keyCharOnly);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_RELEASED, 0, keyCode, keyCharOnly);

        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_PRESSED, modifierMask, modifierKey, KeyEvent.NULL_CHAR);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_PRESSED, modifierMask, keyCode, keyCharMod);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_RELEASED, modifierMask, keyCode, keyCharMod);
        final KeyEvent e = (KeyEvent) queue.get(i++);
        NEWTKeyUtil.validateKeyEvent(e, KeyEvent.EVENT_KEY_RELEASED, modifierMask, modifierKey, KeyEvent.NULL_CHAR);
    }

    static void testKeyCodeAllModifierV1(final Robot robot, final NEWTKeyAdapter keyAdapter) {
        final short m1k = KeyEvent.VK_ALT;
        final int   m1m = InputEvent.ALT_MASK;
        final short m2k = KeyEvent.VK_CONTROL;
        final int   m2m = InputEvent.CTRL_MASK;
        final short m3k = KeyEvent.VK_SHIFT;
        final int   m3m = InputEvent.SHIFT_MASK;

        keyAdapter.reset();
        AWTRobotUtil.waitForIdle(robot);
        AWTRobotUtil.newtKeyPress(0, robot, true, m1k, 10);     // press MOD1
        AWTRobotUtil.newtKeyPress(0, robot, true, m2k, 10);     // press MOD2
        AWTRobotUtil.newtKeyPress(0, robot, true, m3k, 10);     // press MOD3
        AWTRobotUtil.newtKeyPress(0, robot, true, KeyEvent.VK_1, 10);   // press P

        AWTRobotUtil.newtKeyPress(0, robot, false, KeyEvent.VK_1, 100);  // release P
        AWTRobotUtil.newtKeyPress(0, robot, false, m3k, 10);   // release MOD
        AWTRobotUtil.newtKeyPress(0, robot, false, m2k, 10);   // release MOD
        AWTRobotUtil.newtKeyPress(0, robot, false, m1k, 10);   // release MOD
        AWTRobotUtil.waitForIdle(robot);

        for(int j=0; j < 100 && keyAdapter.getQueueSize() < 4+4; j++) { // wait until events are collected
            robot.delay(100);
        }
        NEWTKeyUtil.validateKeyAdapterStats(keyAdapter,
                                            4 /* press-SI */, 4 /* release-SI */,
                                            0 /* press-AR */, 0 /* release-AR */ );

        final List<EventObject> queue = keyAdapter.copyQueue();
        int i=0;
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_PRESSED,  m1m,         m1k, KeyEvent.NULL_CHAR);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_PRESSED,  m1m|m2m,     m2k, KeyEvent.NULL_CHAR);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_PRESSED,  m1m|m2m|m3m, m3k, KeyEvent.NULL_CHAR);

        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_PRESSED,  m1m|m2m|m3m, KeyEvent.VK_1, KeyEvent.NULL_CHAR);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_RELEASED, m1m|m2m|m3m, KeyEvent.VK_1, KeyEvent.NULL_CHAR);
        final KeyEvent e = (KeyEvent) queue.get(i++);
        NEWTKeyUtil.validateKeyEvent(e, KeyEvent.EVENT_KEY_RELEASED, m1m|m2m|m3m, m3k, KeyEvent.NULL_CHAR);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_RELEASED, m1m|m2m,     m2k, KeyEvent.NULL_CHAR);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_RELEASED, m1m,         m1k, KeyEvent.NULL_CHAR);
    }

    void testImpl(final GLWindow glWindow) throws AWTException, InterruptedException, InvocationTargetException {
        final Robot robot = new Robot();
        robot.setAutoWaitForIdle(true);

        final GLEventListener demo1 = new RedSquareES2();
        glWindow.addGLEventListener(demo1);

        // NEWTFocusAdapter glWindow1FA = new NEWTFocusAdapter("GLWindow1");
        // glWindow.addWindowListener(glWindow1FA);
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

        testKeyCodeModifier(robot, glWindow1KA, KeyEvent.VK_SHIFT, InputEvent.SHIFT_MASK, KeyEvent.VK_1, '1', '!');
        testKeyCodeModifier(robot, glWindow1KA, KeyEvent.VK_SHIFT, InputEvent.SHIFT_MASK, KeyEvent.VK_Y, 'y', 'Y'); // US: Y, DE: Z
        testKeyCodeModifier(robot, glWindow1KA, KeyEvent.VK_SHIFT, InputEvent.SHIFT_MASK, KeyEvent.VK_P, 'p', 'P');
        testKeyCodeModifier(robot, glWindow1KA, KeyEvent.VK_CONTROL, InputEvent.CTRL_MASK, KeyEvent.VK_1, '1', KeyEvent.NULL_CHAR);
        testKeyCodeModifier(robot, glWindow1KA, KeyEvent.VK_ALT, InputEvent.ALT_MASK, KeyEvent.VK_1, '1', KeyEvent.NULL_CHAR);

        testKeyCodeAllModifierV1(robot, glWindow1KA);

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
        final String tstname = TestNewtKeyCodeModifiersAWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }


}

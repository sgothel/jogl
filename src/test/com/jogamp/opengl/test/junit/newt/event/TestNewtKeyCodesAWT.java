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
import java.util.ArrayList;
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
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.util.NEWTKeyUtil.CodeSeg;

/**
 * Testing key code of key events.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNewtKeyCodesAWT extends UITestCase {
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
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame1.setSize(width, height);
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

    /** Almost all keyCodes reachable w/o modifiers [shift, alt, ..] on US keyboard! */
    static CodeSeg[] codeSegments = new CodeSeg[] {
      // new CodeSeg(KeyEvent.VK_HOME, KeyEvent.VK_PRINTSCREEN, "home, end, final, prnt"),
      new CodeSeg(KeyEvent.VK_BACK_SPACE, KeyEvent.VK_BACK_SPACE, "bs"),
      // new CodeSeg(KeyEvent.VK_TAB, KeyEvent.VK_TAB, "tab"), // TAB functions as focus traversal key
      new CodeSeg(KeyEvent.VK_ENTER, KeyEvent.VK_ENTER, "cr"),
      new CodeSeg(KeyEvent.VK_PAGE_DOWN, KeyEvent.VK_PAGE_DOWN, "pg_down"),
      new CodeSeg(KeyEvent.VK_SHIFT, KeyEvent.VK_ALT, "shift, pg_up, ctrl, alt"),
      // new CodeSeg(KeyEvent.VK_ALT_GRAPH, KeyEvent.VK_ALT_GRAPH, "alt_gr"), // AWT Robot produces 0xff7e on X11
      // new CodeSeg(KeyEvent.VK_SCROLL_LOCK, KeyEvent.VK_SCROLL_LOCK, "scroll lock"),
      new CodeSeg(KeyEvent.VK_ESCAPE, KeyEvent.VK_ESCAPE, "esc"),
      new CodeSeg(KeyEvent.VK_SPACE, KeyEvent.VK_SPACE, "space"),
      new CodeSeg(KeyEvent.VK_QUOTE, KeyEvent.VK_QUOTE, "quote"),
      new CodeSeg(KeyEvent.VK_COMMA, KeyEvent.VK_SLASH, ", - . /"),
      new CodeSeg(KeyEvent.VK_0, KeyEvent.VK_9, "0 - 9"),
      new CodeSeg(KeyEvent.VK_SEMICOLON, KeyEvent.VK_SEMICOLON, ";"),
      new CodeSeg(KeyEvent.VK_EQUALS, KeyEvent.VK_EQUALS, "="),
      new CodeSeg(KeyEvent.VK_A, KeyEvent.VK_Z, "a - z"),
      new CodeSeg(KeyEvent.VK_OPEN_BRACKET, KeyEvent.VK_CLOSE_BRACKET, "[ \\ ]"),
      new CodeSeg(KeyEvent.VK_BACK_QUOTE, KeyEvent.VK_BACK_QUOTE, "`"),
      new CodeSeg(KeyEvent.VK_F1, KeyEvent.VK_F8, "f1..f8"),
      // new CodeSeg(KeyEvent.VK_F1, KeyEvent.VK_F12, "f1..f12"), // f9-f12 may cause some odd desktop functions!
      new CodeSeg(KeyEvent.VK_DELETE, KeyEvent.VK_DELETE, "del"),
      // new CodeSeg(KeyEvent.VK_NUMPAD0, KeyEvent.VK_NUMPAD9, "numpad0-9"), // can be mapped to normal keycodes
      // new CodeSeg(KeyEvent.VK_DECIMAL, KeyEvent.VK_DIVIDE, "numpad ops"), // can be mapped to normal keycodes
      // new CodeSeg(KeyEvent.VK_NUM_LOCK, KeyEvent.VK_NUM_LOCK, "num lock"),
      // new CodeSeg(KeyEvent.VK_KP_LEFT, KeyEvent.VK_KP_DOWN, "numpad cursor arrows"),
      new CodeSeg(KeyEvent.VK_LEFT, KeyEvent.VK_DOWN, "cursor arrows"),
      // new CodeSeg(KeyEvent.VK_WINDOWS, KeyEvent.VK_HELP, "windows, meta, hlp"),
    };

    static void testKeyCodes(final Robot robot, final Object obj, final NEWTKeyAdapter keyAdapter) throws InterruptedException, InvocationTargetException {
        final List<List<EventObject>> cse = new ArrayList<List<EventObject>>();

        keyAdapter.setVerbose(true); // FIXME
        final int[] objCenter = AWTRobotUtil.getCenterLocation(obj, false /* onTitleBarIfWindow */);

        for(int i=0; i<codeSegments.length; i++) {
            keyAdapter.reset();
            final CodeSeg codeSeg = codeSegments[i];
            // System.err.println("*** Segment "+codeSeg.description);
            int eventCount = 0;
            for(short c=codeSeg.min; c<=codeSeg.max; c++) {
                AWTRobotUtil.waitForIdle(robot);
                // System.err.println("*** KeyCode 0x"+Integer.toHexString(c));
                try {
                    AWTRobotUtil.newtKeyPress(0, robot, true, c, 10);
                } catch (final Exception e) {
                    System.err.println("Exception @ AWT Robot.PRESS "+MiscUtils.toHexString(c)+" - "+e.getMessage());
                    break;
                }
                eventCount++;
                try {
                    AWTRobotUtil.newtKeyPress(0, robot, false, c, 100);
                } catch (final Exception e) {
                    System.err.println("Exception @ AWT Robot.RELEASE "+MiscUtils.toHexString(c)+" - "+e.getMessage());
                    break;
                }
                eventCount++;
            }
            AWTRobotUtil.waitForIdle(robot);
            for(int j=0; j < NEWTKeyUtil.POLL_DIVIDER && keyAdapter.getQueueSize() < eventCount; j++) { // wait until events are collected
                robot.delay(NEWTKeyUtil.TIME_SLICE);
                // Bug 919 - TestNewtKeyCodesAWT w/ NewtCanvasAWT Fails on Windows Due to Clogged Key-Release Event by AWT Robot
                final int off = 0==j%2 ? 1 : -1;
                AWTRobotUtil.awtRobotMouseMove(robot, objCenter[0]+off, objCenter[1]);
            }
            AWTRobotUtil.awtRobotMouseMove(robot, objCenter[0], objCenter[1]); // Bug 919: Reset mouse position
            cse.add(keyAdapter.copyQueue());
        }
        Assert.assertEquals("KeyCode impl. incomplete", true, NEWTKeyUtil.validateKeyCodes(codeSegments, cse, true));
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

        testKeyCodes(robot, glWindow, glWindow1KA);

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
        final String tstname = TestNewtKeyCodesAWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }


}

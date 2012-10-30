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
 
package com.jogamp.opengl.test.junit.newt;

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

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.swing.JFrame;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.util.NEWTKeyUtil.CodeSeg;

/**
 * Testing key event order incl. auto-repeat (Bug 601)
 * 
 * <p>
 * Note Event order:
 * <ol>
 *   <li>{@link #EVENT_KEY_PRESSED}</li>
 *   <li>{@link #EVENT_KEY_RELEASED}</li>
 *   <li>{@link #EVENT_KEY_TYPED}</li>
 * </ol>
 * </p>
 */
public class TestNewtKeyCodeAWT extends UITestCase {
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
    
    @Test
    public void test01NEWT() throws AWTException, InterruptedException, InvocationTargetException {
        GLWindow glWindow = GLWindow.create(glCaps);
        glWindow.setSize(width, height);
        glWindow.setVisible(true);
        
        testImpl(glWindow);
        
        glWindow.destroy();
    }
        
    // @Test
    public void test02NewtCanvasAWT() throws AWTException, InterruptedException, InvocationTargetException {
        GLWindow glWindow = GLWindow.create(glCaps);
        
        // Wrap the window in a canvas.
        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow);
        
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
        } catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }        
        glWindow.destroy();
    }
        
    static CodeSeg[] codeSegments = new CodeSeg[] {
      new CodeSeg(0x008, 0x008, "bs"),
      // new CodeSeg(0x009, 0x009, "tab"), // TAB functions as focus traversal key
      new CodeSeg(0x00a, 0x00a, "cr"),
      new CodeSeg(0x010, 0x011, "shift, ctrl"), // single alt n/a on windows
      new CodeSeg(0x01B, 0x01B, "esc"),
      new CodeSeg(0x020, 0x024, "space, up, down, end, home"),
      new CodeSeg(0x025, 0x028, "cursor"),
      new CodeSeg(0x02C, 0x02F, ", - . /"),
      new CodeSeg(0x030, 0x039, "0 - 9"),
      new CodeSeg(0x03B, 0x03B, ";"),
      new CodeSeg(0x03D, 0x03D, "="),
      new CodeSeg(0x041, 0x05A, "a - z"),
      new CodeSeg(0x05B, 0x05D, "[ \\ ]"),
      // new CodeSeg(0x060, 0x06B, "numpad1"), // can be mapped to normal keycodes
      // new CodeSeg(0x06D, 0x06F, "numpad2"), // can be mapped to normal keycodes
      new CodeSeg(0x07F, 0x07F, "del"),
      // new CodeSeg(0x090, 0x091, "num lock, scroll lock"),
      // new CodeSeg(0x070, 0x07B, "F1 - F12"),
      // new CodeSeg(0x09A, 0x09D, "prt ins hlp meta"),
      new CodeSeg(0x0C0, 0x0C0, "back quote"),
      new CodeSeg(0x0DE, 0x0DE, "quote"),
      // new CodeSeg(0x0E0, 0x0E3, "cursor kp"),
      // new CodeSeg(0x080, 0x08F, "dead-1"),
      // new CodeSeg(0x096, 0x0A2, "& ^ \" < > { }"), 
      // new CodeSeg(0x200, 0x20D, "extra-2"), // @ ; ..
    };
    
    static void testKeyCode(Robot robot, NEWTKeyAdapter keyAdapter) {
        final List<List<EventObject>> cse = new ArrayList<List<EventObject>>();
        final List<EventObject> queue = keyAdapter.getQueued();
        
        for(int i=0; i<codeSegments.length; i++) {
            keyAdapter.reset();
            final CodeSeg codeSeg = codeSegments[i];
            // System.err.println("*** Segment "+codeSeg.description);
            for(int c=codeSeg.min; c<=codeSeg.max; c++) {
                // System.err.println("*** KeyCode 0x"+Integer.toHexString(c));
                AWTRobotUtil.keyPress(0, robot, true, c, 10);
                AWTRobotUtil.keyPress(0, robot, false, c, 100);
                robot.waitForIdle();
            }
            final int codeCount = codeSeg.max - codeSeg.min + 1;
            for(int j=0; j < 10 && queue.size() < 3 * codeCount; j++) { // wait until events are collected
                robot.delay(100);
            }
            final ArrayList<EventObject> events = new ArrayList<EventObject>(queue);
            cse.add(events);
        }
        Assert.assertEquals("KeyCode impl. incomplete", true, NEWTKeyUtil.validateKeyCode(codeSegments, cse, true));        
    }
        
    void testImpl(GLWindow glWindow) throws AWTException, InterruptedException, InvocationTargetException {
        final Robot robot = new Robot();
        robot.setAutoWaitForIdle(true);

        GLEventListener demo1 = new RedSquareES2();
        TestListenerCom01AWT.setDemoFields(demo1, glWindow, false);
        glWindow.addGLEventListener(demo1);

        // NEWTFocusAdapter glWindow1FA = new NEWTFocusAdapter("GLWindow1");
        // glWindow.addWindowListener(glWindow1FA);
        NEWTKeyAdapter glWindow1KA = new NEWTKeyAdapter("GLWindow1");
        glWindow1KA.setVerbose(false);
        glWindow.addKeyListener(glWindow1KA);

        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glWindow, true));        

        // Continuous animation ..
        Animator animator = new Animator(glWindow);
        animator.start();

        Thread.sleep(durationPerTest); // manual testing
        
        AWTRobotUtil.assertRequestFocusAndWait(null, glWindow, glWindow, null, null);  // programmatic
        AWTRobotUtil.requestFocus(robot, glWindow); // within unit framework, prev. tests (TestFocus02SwingAWTRobot) 'confuses' Windows keyboard input
        glWindow1KA.reset();        

        // 
        // Test the key event order w/o auto-repeat
        //
        testKeyCode(robot, glWindow1KA);
        
        // Remove listeners to avoid logging during dispose/destroy.
        glWindow.removeKeyListener(glWindow1KA);

        // Shutdown the test.
        animator.stop();
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
        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine()); 
        */
        System.out.println("durationPerTest: "+durationPerTest);
        String tstname = TestNewtKeyCodeAWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }


}

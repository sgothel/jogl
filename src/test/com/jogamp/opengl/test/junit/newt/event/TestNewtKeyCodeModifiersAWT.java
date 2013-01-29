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

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.swing.JFrame;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;

import com.jogamp.opengl.test.junit.util.*;

/**
 * Testing combinations of key code modifiers of key event.
 */
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
    
    @Test
    public void test01NEWT() throws AWTException, InterruptedException, InvocationTargetException {
        GLWindow glWindow = GLWindow.create(glCaps);
        glWindow.setSize(width, height);
        glWindow.setVisible(true);
        
        testImpl(glWindow);
        
        glWindow.destroy();
    }
        
    @Test
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
        
    static void testKeyCodeModifier(Robot robot, NEWTKeyAdapter keyAdapter, int modifierKey, int modifierMask) {
        keyAdapter.reset();
        AWTRobotUtil.keyPress(0, robot, true, KeyEvent.VK_P, 10);   // press P
        AWTRobotUtil.keyPress(0, robot, false, KeyEvent.VK_P, 100); // release+typed P
        robot.waitForIdle();        
        for(int j=0; j < 10 && keyAdapter.getQueueSize() < 3; j++) { // wait until events are collected
            robot.delay(100);
        }
        
        AWTRobotUtil.keyPress(0, robot, true, modifierKey, 10);     // press MOD
        AWTRobotUtil.keyPress(0, robot, true, KeyEvent.VK_P, 10);   // press P
        AWTRobotUtil.keyPress(0, robot, false, KeyEvent.VK_P, 10);  // release+typed P 
        AWTRobotUtil.keyPress(0, robot, false, modifierKey, 100);   // release+typed MOD
        robot.waitForIdle();        
        for(int j=0; j < 20 && keyAdapter.getQueueSize() < 3+6; j++) { // wait until events are collected
            robot.delay(100);
        }
        NEWTKeyUtil.validateKeyAdapterStats(keyAdapter, 3+6, 0);        
        
        final List<EventObject> queue = keyAdapter.getQueued();        
        int i=0;
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_PRESSED, 0, KeyEvent.VK_P);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_RELEASED, 0, KeyEvent.VK_P);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_TYPED, 0, KeyEvent.VK_P);
        
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_PRESSED, modifierMask, modifierKey);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_PRESSED, modifierMask, KeyEvent.VK_P);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_RELEASED, modifierMask, KeyEvent.VK_P);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_TYPED, modifierMask, KeyEvent.VK_P);                
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_RELEASED, modifierMask, modifierKey);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_TYPED, modifierMask, modifierKey);                
    }
    
    static void testKeyCodeAllModifierV1(Robot robot, NEWTKeyAdapter keyAdapter) {
        final int m1k = KeyEvent.VK_ALT;
        final int m1m = InputEvent.ALT_MASK;
        final int m2k = KeyEvent.VK_CONTROL;
        final int m2m = InputEvent.CTRL_MASK;
        final int m3k = KeyEvent.VK_SHIFT;
        final int m3m = InputEvent.SHIFT_MASK;
        
        keyAdapter.reset();
        AWTRobotUtil.keyPress(0, robot, true, m1k, 10);     // press MOD1
        AWTRobotUtil.keyPress(0, robot, true, m2k, 10);     // press MOD2
        AWTRobotUtil.keyPress(0, robot, true, m3k, 10);     // press MOD3
        AWTRobotUtil.keyPress(0, robot, true, KeyEvent.VK_P, 10);   // press P
        
        AWTRobotUtil.keyPress(0, robot, false, KeyEvent.VK_P, 100);  // release+typed P        
        AWTRobotUtil.keyPress(0, robot, false, m3k, 10);   // release+typed MOD
        AWTRobotUtil.keyPress(0, robot, false, m2k, 10);   // release+typed MOD
        AWTRobotUtil.keyPress(0, robot, false, m1k, 10);   // release+typed MOD
        
        robot.waitForIdle();        
        for(int j=0; j < 20 && keyAdapter.getQueueSize() < 3*4; j++) { // wait until events are collected
            robot.delay(100);
        }
        NEWTKeyUtil.validateKeyAdapterStats(keyAdapter, 3*4, 0);        
        
        final List<EventObject> queue = keyAdapter.getQueued();        
        int i=0;
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_PRESSED,  m1m,         m1k);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_PRESSED,  m1m|m2m,     m2k);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_PRESSED,  m1m|m2m|m3m, m3k);
        
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_PRESSED,  m1m|m2m|m3m, KeyEvent.VK_P);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_RELEASED, m1m|m2m|m3m, KeyEvent.VK_P);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_TYPED,    m1m|m2m|m3m, KeyEvent.VK_P);
        
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_RELEASED, m1m|m2m|m3m, m3k);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_TYPED,    m1m|m2m|m3m, m3k);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_RELEASED, m1m|m2m,     m2k);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_TYPED,    m1m|m2m,     m2k);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_RELEASED, m1m,         m1k);
        NEWTKeyUtil.validateKeyEvent((KeyEvent) queue.get(i++), KeyEvent.EVENT_KEY_TYPED,    m1m,         m1k);                
    }
    
    void testImpl(GLWindow glWindow) throws AWTException, InterruptedException, InvocationTargetException {
        final Robot robot = new Robot();
        robot.setAutoWaitForIdle(true);

        GLEventListener demo1 = new RedSquareES2();
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
        AWTRobotUtil.requestFocus(robot, glWindow, false); // within unit framework, prev. tests (TestFocus02SwingAWTRobot) 'confuses' Windows keyboard input
        glWindow1KA.reset();        

        testKeyCodeModifier(robot, glWindow1KA, KeyEvent.VK_SHIFT, InputEvent.SHIFT_MASK);
        testKeyCodeModifier(robot, glWindow1KA, KeyEvent.VK_CONTROL, InputEvent.CTRL_MASK);
        testKeyCodeModifier(robot, glWindow1KA, KeyEvent.VK_ALT, InputEvent.ALT_MASK);
        
        testKeyCodeAllModifierV1(robot, glWindow1KA);
        
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
        String tstname = TestNewtKeyCodeModifiersAWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }


}

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

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Robot;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.swing.JFrame;

import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.test.junit.jogl.demos.es1.RedSquare;

import com.jogamp.test.junit.util.*;

public class TestFocus01SwingAWTRobot extends UITestCase {

    static {
        GLProfile.initSingleton();
    }

    static int width, height;

    static long durationPerTest = 800;

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
        Robot robot = new Robot();
        robot.setAutoWaitForIdle(true);
        testFocus01ProgrFocusImpl(robot);
    }

    private void testFocus01ProgrFocusImpl(Robot robot) throws AWTException,
            InvocationTargetException, InterruptedException {
        // Create a window.
        GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setTitle("testNewtChildFocus");
        GLEventListener demo1 = new RedSquare();
        TestListenerCom01AWT.setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        NEWTFocusAdapter glWindow1FA = new NEWTFocusAdapter("GLWindow1");
        glWindow1.addWindowListener(glWindow1FA);

        // Monitor NEWT focus and keyboard events.
        NEWTKeyAdapter glWindow1KA = new NEWTKeyAdapter("GLWindow1");
        glWindow1.addKeyListener(glWindow1KA);

        // Wrap the window in a canvas.
        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);

        // Monitor AWT focus and keyboard events.
        AWTKeyAdapter newtCanvasAWTKA = new AWTKeyAdapter("NewtCanvasAWT");
        newtCanvasAWT.addKeyListener(newtCanvasAWTKA);
        AWTFocusAdapter newtCanvasAWTFA = new AWTFocusAdapter("NewtCanvasAWT");
        newtCanvasAWT.addFocusListener(newtCanvasAWTFA);

        // Add the canvas to a frame, and make it all visible.
        JFrame frame1 = new JFrame("Swing AWT Parent Frame: "
                + glWindow1.getTitle());
        frame1.getContentPane().add(newtCanvasAWT, BorderLayout.CENTER);
        Button button = new Button("Click me ..");
        AWTFocusAdapter buttonFA = new AWTFocusAdapter("Button");
        button.addFocusListener(buttonFA);
        AWTKeyAdapter buttonKA = new AWTKeyAdapter("Button");
        button.addKeyListener(buttonKA);
        frame1.getContentPane().add(button, BorderLayout.NORTH);
        frame1.setSize(width, height);
        frame1.setVisible(true);
        Assert.assertTrue(AWTRobotUtil.toFront(robot, frame1));

        int wait=0;
        while(wait<10 && glWindow1.getTotalFrames()<1) { Thread.sleep(100); wait++; }
        System.out.println("Frames for initial setVisible(true): "+glWindow1.getTotalFrames());
        Assert.assertTrue(0 < glWindow1.getTotalFrames());

        // Continuous animation ..
        Animator animator = new Animator(glWindow1);
        animator.start();

        // Button Focus
        Thread.sleep(100); // allow event sync
        System.err.println("FOCUS AWT  Button request");
        Assert.assertTrue(AWTRobotUtil.requestFocusAndWait(robot, button, button));
        Assert.assertEquals(0, glWindow1FA.getCount());
        Assert.assertEquals(0, newtCanvasAWTFA.getCount());
        System.err.println("FOCUS AWT  Button sync");
        Assert.assertEquals(2, AWTRobotUtil.testKeyType(robot, 2, button, buttonKA));

        // Request the AWT focus, which should automatically provide the NEWT window with focus.
        Thread.sleep(100); // allow event sync
        System.err.println("FOCUS NEWT Canvas/GLWindow request");
        Assert.assertTrue(AWTRobotUtil.requestFocusAndWait(robot, newtCanvasAWT, newtCanvasAWT.getNEWTChild()));
        Assert.assertEquals(0, newtCanvasAWTFA.getCount());
        Assert.assertEquals(0, buttonFA.getCount());
        System.err.println("FOCUS NEWT Canvas/GLWindow sync");
        Assert.assertEquals(2, AWTRobotUtil.testKeyType(robot, 2, glWindow1, glWindow1KA));
        Assert.assertEquals("AWT parent canvas received keyboard events", 0, newtCanvasAWTKA.getCount());

        // Remove listeners to avoid logging during dispose/destroy.
        glWindow1.removeKeyListener(glWindow1KA);
        glWindow1.removeWindowListener(glWindow1FA);
        newtCanvasAWT.removeKeyListener(newtCanvasAWTKA);
        newtCanvasAWT.removeFocusListener(newtCanvasAWTFA);

        // Shutdown the test.
        animator.stop();
        frame1.dispose();
        glWindow1.destroy(true);
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
        System.out.println("durationPerTest: "+durationPerTest);
        String tstname = TestFocus01SwingAWTRobot.class.getName();
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

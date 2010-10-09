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
 
package com.jogamp.test.junit.jogl.awt;

import com.jogamp.test.junit.util.UITestCase;
import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;
import com.jogamp.test.junit.util.*;

import java.lang.reflect.InvocationTargetException;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.AWTException;
import java.awt.LayoutManager;
import java.awt.Robot;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.media.opengl.GLEventListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

public class TestSwingAWTRobotUsageBeforeJOGLInitBug411 extends UITestCase {
    static long durationPerTest = 500; // ms
    static Robot robot;
    static Border border;
    static JFrame frame;
    static JButton button;
    static JPanel panel;
    static JPanel colorPanel;
    static boolean windowClosing;

    boolean modLightBrighter = true;

    Color modLight(Color c) {
        Color c2;
        if(modLightBrighter) {
            c2 = c.brighter();
        } else {
            c2 = c.darker();
        }
        if(c2.equals(c)) {
            modLightBrighter = !modLightBrighter;
        }
        return c2;
    }

    class SwingGLAction implements GLEventListener {
        public void init(GLAutoDrawable glad) {
        }

        public void dispose(GLAutoDrawable glad) {
        }

        public void display(GLAutoDrawable glad) {
            colorPanel.setBackground(modLight(colorPanel.getBackground()));
            colorPanel.repaint();
        }

        public void reshape(GLAutoDrawable glad, final int x, final int y, final int width, final int height) {
        }
    }

    @BeforeClass
    public static void setup() throws InterruptedException, InvocationTargetException, AWTException {
        int count;

        // simulate AWT usage before JOGL's initialization of X11 threading
        windowClosing=false;
        border = BorderFactory.createLineBorder (Color.yellow, 2);

        panel = new JPanel();
        panel.setLayout(new BorderLayout());

        button = new JButton("Click me");
        button.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                System.err.println("Test: "+e);
            }
        });
        panel.add(button, BorderLayout.NORTH);

        colorPanel = new JPanel();
        Dimension size = new Dimension(400,100);
        colorPanel.setPreferredSize(size);
        colorPanel.setBorder(border);
        panel.add(colorPanel, BorderLayout.SOUTH);

        frame = new JFrame("PRE JOGL");
        frame.addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent ev) {
                windowClosing=true;
            }
        });
        frame.setContentPane(panel);
        frame.setSize(512, 512);
        frame.pack();
        frame.setVisible(true);

        // AWT/Swing: From here on (post setVisible(true)
        //            you need to use AWT/Swing's invokeAndWait()

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                colorPanel.setBackground(Color.white);
                colorPanel.repaint();
            }});

        robot = new Robot();
        robot.setAutoWaitForIdle(true);

        AWTRobotUtil.toFront(robot, frame);
        AWTRobotUtil.requestFocus(robot, button);

        System.err.println("Clean End of Pre-JOGL-Swing");

        GLProfile.initSingleton();
    }

    @AfterClass
    public static void release() {
        robot = null;
        Assert.assertNotNull(frame);
        frame.dispose();
        frame=null;
    }

    protected void runTestGL(final Canvas canvas, GLAutoDrawable drawable) 
        throws AWTException, InterruptedException, InvocationTargetException {

        Dimension size = new Dimension(400,400);
        canvas.setPreferredSize(size);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                panel.add(canvas, BorderLayout.CENTER);
                frame.pack();
            }
        });

        AWTRobotUtil.toFront(robot, frame);

        drawable.addGLEventListener(new Gears());

        for(int i=0; i<100; i++) {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    colorPanel.setBackground(modLight(colorPanel.getBackground()));
                    colorPanel.repaint();
                }
            });
            drawable.display(); // one in process display
            Thread.sleep(10);
        }

        colorPanel.setBackground(Color.blue);
        drawable.addGLEventListener(new SwingGLAction());

        Animator animator = new Animator(drawable);
        animator.start();

        Point p0 = canvas.getLocationOnScreen();
        Rectangle r0 = canvas.getBounds();
        robot.mouseMove( (int) ( p0.getX() + .5 ) ,
                         (int) ( p0.getY() + .5 ) );
        robot.mousePress(InputEvent.BUTTON1_MASK);
        for(int i=0; !windowClosing && i<durationPerTest/10; i++) {
            p0.translate(1,1);
            robot.mouseMove( (int) ( p0.getX() + .5 ) ,
                             (int) ( p0.getY() + .5 ) );
            Thread.sleep(10);
        }
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        for(int i=0; !windowClosing && i<durationPerTest/10; i++) {
            Thread.sleep(10);
        }

        animator.stop();

        Assert.assertNotNull(canvas);
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                panel.remove(canvas);
                frame.pack();
            }
        });
    }

    @Test
    public void test01GLCanvas() throws AWTException, InterruptedException, InvocationTargetException {
        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);

        GLWindow win0 = GLWindow.create(caps);
        win0.setSize(100,100);
        win0.setVisible(true);
        win0.addGLEventListener(new Gears());
        Animator anim0 = new Animator(win0);
        anim0.start();

        GLCanvas glCanvas = new GLCanvas(caps);

        runTestGL(glCanvas, glCanvas);

        anim0.stop();
        win0.destroy(true);
    }

    @Test
    public void test02NewtCanvasAWT() throws AWTException, InterruptedException, InvocationTargetException {
        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);

        GLWindow winDummy = GLWindow.create(caps);
        winDummy.addGLEventListener(new Gears());

        GLWindow win0 = GLWindow.create(caps);
        win0.setSize(100,100);
        win0.setVisible(true);
        win0.addGLEventListener(new Gears());
        Animator anim0 = new Animator(win0);
        anim0.start();

        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(GLWindow.create(caps));

        runTestGL(newtCanvasAWT, (GLAutoDrawable)newtCanvasAWT.getNEWTChild());
        newtCanvasAWT.destroy(true);

        winDummy.destroy(true);

        anim0.stop();
        win0.destroy(true);
    }

    static int atoi(String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        System.out.println("durationPerTest: "+durationPerTest);
        org.junit.runner.JUnitCore.main(TestSwingAWTRobotUsageBeforeJOGLInitBug411.class.getName());
    }
}

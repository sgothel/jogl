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
 
package com.jogamp.test.junit.util;

import java.lang.reflect.InvocationTargetException;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import javax.swing.JFrame;

public class AWTRobotUtil {

    public static int TIME_OUT = 1000; // 1s

    /**
     * toFront, call setVisible(true) and toFront(),
     * after positioning the mouse in the middle of the window via robot.
     * If the given robot is null, a new one is created (waitForIdle=true).
     *
     * @return True if the Window became the global focused Window within TIME_OUT
     */
    public static boolean toFront(Robot robot, Window window) 
        throws AWTException, InterruptedException, InvocationTargetException {

        if(null == robot) {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
        }
        Point p0 = window.getLocationOnScreen();
        Rectangle r0 = window.getBounds();
        int dx = (int) ( r0.getWidth()  / 2.0 + .5 );
        int dy = (int) ( r0.getHeight() / 2.0 + .5 );
        int x0 = (int) ( p0.getX() + dx + .5 ) ;
        int y0 = (int) ( p0.getY() + dy + .5 ) ;
        System.err.println("robot pos: "+x0+"/"+y0);
        robot.mouseMove( x0, y0 );
        robot.delay(50);

        final Window f_window = window;
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f_window.setVisible(true);
                f_window.toFront();
                f_window.requestFocus();
            }});
        robot.delay(200);

        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        int wait;
        for (wait=0; wait<10 && window != kfm.getFocusedWindow(); wait++) {
            Thread.sleep(TIME_OUT/10);
        }
        return wait<10;
    }

    /**
     * requestFocus, if robot is valid, use mouse operation,
     * otherwise programatic, ie call requestFocus
     */
    public static void requestFocus(Robot robot, Object obj) 
        throws InterruptedException, InvocationTargetException {
        Component comp = null;
        com.jogamp.newt.Window win = null;

        if(obj instanceof com.jogamp.newt.Window) {
            win = (com.jogamp.newt.Window) obj;
        } else if(obj instanceof Component) {
            comp = (Component) obj;
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }

        if(null == robot) {
            if(null!=comp) {
                final Component f_comp = comp;
                javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        f_comp.requestFocus();
                    }});
            } else {
                win.requestFocus();
            }
            return;
        }

        int x0, y0;
        if(null!=comp) {
            Point p0 = comp.getLocationOnScreen();
            Rectangle r0 = comp.getBounds();
            if( comp instanceof JFrame ) {
                JFrame jFrame = (JFrame) comp;
                Container cont = jFrame.getContentPane();
                Point p1 = cont.getLocationOnScreen();
                int dx = (int) ( r0.getWidth() / 2.0 + .5 );
                int dy = (int) ( ( p1.getY() - p0.getY() ) / 2.0 + .5 );
                x0 = (int) ( p0.getX() + dx + .5 ) ;
                y0 = (int) ( p0.getY() + dy + .5 ) ;
            } else {
                x0 = (int) ( p0.getX() + r0.getWidth()  / 2.0 + .5 ) ;
                y0 = (int) ( p0.getY() + r0.getHeight() / 2.0 + .5 ) ;
            }
        } else {
            x0 = win.getX() + win.getWidth()  / 2 ;
            y0 = win.getY() + win.getHeight() / 2 ;
        }

        System.err.println("robot pos: "+x0+"/"+y0);
        robot.mouseMove( x0, y0 );
        robot.delay(50);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(50);
    }

    /**
     *
     * @return True if the Window became the global focused Window within TIME_OUT
     */
    public static boolean waitForFocus(Object obj) throws InterruptedException {
        int wait;
        if(obj instanceof Component) {
            Component comp = (Component) obj;
            KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            for (wait=0; wait<10 && comp != kfm.getPermanentFocusOwner(); wait++) {
                Thread.sleep(TIME_OUT/10);
            }
        } else if(obj instanceof com.jogamp.newt.Window) {
            com.jogamp.newt.Window win = (com.jogamp.newt.Window) obj;
            for (wait=0; wait<10 && !win.hasFocus(); wait++) {
                Thread.sleep(TIME_OUT/10);
            }
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }
        return wait<10;
    }

    public static boolean requestFocusAndWait(Robot robot, Object requestFocus, Object waitForFocus)
        throws InterruptedException, InvocationTargetException {

        requestFocus(robot, requestFocus);
        return waitForFocus(waitForFocus);
    }

    /**
     * @param keyTypedCounter shall return the number of keys typed (press + release)
     * @return True if the object received 2 keys within TIME_OUT
     */
    public static boolean testKeyInput(Robot robot, EventCountAdapter keyTypedCounter) 
        throws AWTException, InterruptedException, InvocationTargetException {
        Component comp = null;
        com.jogamp.newt.Window win = null;

        int c0 = keyTypedCounter.getCount();

        if(null == robot) {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
        }

        robot.keyPress(java.awt.event.KeyEvent.VK_A);
        robot.delay(50);
        robot.keyRelease(java.awt.event.KeyEvent.VK_A);
        robot.delay(50);
        robot.keyPress(java.awt.event.KeyEvent.VK_B);
        robot.delay(50);
        robot.keyRelease(java.awt.event.KeyEvent.VK_B);
        robot.delay(50);

        // Wait for the key events to be processed.
        int wait;
        for (wait=0; wait<10 && (keyTypedCounter.getCount()-c0)<2; wait++) {
            Thread.sleep(TIME_OUT/10);
        }
        return wait<10;
    }

}


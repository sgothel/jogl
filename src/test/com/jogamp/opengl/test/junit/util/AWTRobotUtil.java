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
 
package com.jogamp.opengl.test.junit.util;

import jogamp.newt.WindowImplAccess;
import java.lang.reflect.InvocationTargetException;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.KeyboardFocusManager;
import java.awt.Robot;
import java.awt.Toolkit;

import javax.media.opengl.awt.GLCanvas;

import org.junit.Assert;

public class AWTRobotUtil {

    static final boolean DEBUG = false;
    
    public static final int RETRY_NUMBER  =   5;
    public static final int ROBOT_DELAY   = 100; // ms
    public static final int TIME_OUT     = 2000; // 2s
    public static final int POLL_DIVIDER   = 20; // TO/20
    public static final int TIME_SLICE   = TIME_OUT / POLL_DIVIDER ;
    public static Integer AWT_CLICK_TO = null; 
    
    public static java.awt.Point getCenterLocation(Object obj, boolean onTitleBarIfWindow) 
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

        int x0, y0;
        if(null!=comp) {
            java.awt.Point p0 = comp.getLocationOnScreen();            
            java.awt.Rectangle r0 = comp.getBounds();
            if( onTitleBarIfWindow && comp instanceof java.awt.Window) {
                java.awt.Window window = (java.awt.Window) comp;
                java.awt.Insets insets = window.getInsets();
                y0 = (int) ( p0.getY() +    insets.top / 2.0 + .5 ) ;            
            } else {
                y0 = (int) ( p0.getY() + r0.getHeight() / 2.0 + .5 ) ;
            }
            x0 = (int) ( p0.getX() + r0.getWidth() / 2.0 + .5 ) ;
        } else {
            javax.media.nativewindow.util.Point p0 = win.getLocationOnScreen(null);            
            if( onTitleBarIfWindow ) {
                javax.media.nativewindow.util.InsetsImmutable insets = win.getInsets();
                p0.translate(win.getWidth()/2, insets.getTopHeight()/2);                
            } else {
                p0.translate(win.getWidth()/2, win.getHeight()/2);
            }
            x0 = p0.getX();
            y0 = p0.getY();
        }

        return new java.awt.Point(x0, y0);
    }

    /**
     * toFront, call setVisible(true) and toFront(),
     * after positioning the mouse in the middle of the window via robot.
     * If the given robot is null, a new one is created (waitForIdle=true).
     *
     * @return True if the Window became the global focused Window within TIME_OUT
     */
    public static boolean toFront(Robot robot, final java.awt.Window window)
        throws AWTException, InterruptedException, InvocationTargetException {

        AWTWindowFocusAdapter winFA = new AWTWindowFocusAdapter("window");
        window.addWindowFocusListener(winFA);
        
        if(null == robot) {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
        }
        java.awt.Point p0 = getCenterLocation(window, false);
        System.err.println("robot pos: "+p0);
        robot.mouseMove( (int) p0.getX(), (int) p0.getY() );
        robot.delay(ROBOT_DELAY);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                window.setVisible(true);
                window.toFront();
                window.requestFocus();
            }});
        robot.delay(ROBOT_DELAY);

        int wait;
        for (wait=0; wait<POLL_DIVIDER && !winFA.focusGained(); wait++) {
            Thread.sleep(TIME_SLICE);
        }
        window.removeWindowFocusListener(winFA);
        return wait<POLL_DIVIDER;
    }

    /**
     * centerMouse
     * @param onTitleBarIfWindow TODO
     */
    public static void centerMouse(Robot robot, Object obj, boolean onTitleBarIfWindow) 
        throws AWTException, InterruptedException, InvocationTargetException {

        if(null == robot) {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
        }

        java.awt.Point p0 = getCenterLocation(obj, onTitleBarIfWindow);
        System.err.println("robot pos: "+p0);

        robot.mouseMove( (int) p0.getX(), (int) p0.getY() );
        robot.delay(ROBOT_DELAY);
    }

    public static int getClickTimeout(Object obj) {
        if(obj instanceof com.jogamp.newt.Window) {
            return com.jogamp.newt.event.MouseEvent.getClickTimeout();
        } else if(obj instanceof Component) {
            if(null == AWT_CLICK_TO) {
                AWT_CLICK_TO =
                    (Integer) Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
                if(null == AWT_CLICK_TO) { 
                    AWT_CLICK_TO = new Integer(500);
                }
            }
            return AWT_CLICK_TO.intValue();
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }        
    }
    
    /**
     * requestFocus, if robot is valid, use mouse operation,
     * otherwise programatic, ie call requestFocus
     */
    public static void requestFocus(Robot robot, Object obj) 
        throws AWTException, InterruptedException, InvocationTargetException {
        final Component comp;
        final com.jogamp.newt.Window win;

        if(obj instanceof com.jogamp.newt.Window) {
            win = (com.jogamp.newt.Window) obj;
            comp = null;
        } else if(obj instanceof Component) {
            win = null;
            comp = (Component) obj;
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }
        
        if(null == robot) {        
            if(null!=comp) {
                javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        comp.requestFocus();
                    }});
            } else {
                win.requestFocus();
            }
        } else {
            final int mouseButton = java.awt.event.InputEvent.BUTTON1_MASK;    
            centerMouse(robot, obj, true);
    
            robot.waitForIdle();
            robot.mousePress(mouseButton);
            robot.mouseRelease(mouseButton);
            robot.delay( getClickTimeout(obj) + 1 );                
        }
    }

    public static boolean hasFocus(Object obj) {
        if(obj instanceof Component) {
            final Component comp = (Component) obj;
            final KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            return comp == kfm.getPermanentFocusOwner();
        } else if(obj instanceof com.jogamp.newt.Window) {
            return ((com.jogamp.newt.Window) obj).hasFocus();
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }
    }

    /**
     *
     * @return True if the Window became the global focused Window within TIME_OUT
     */
    public static boolean waitForFocus(Object obj) throws InterruptedException {
        int wait;
        if(obj instanceof Component) {
            final Component comp = (Component) obj;
            final KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            for (wait=0; wait<POLL_DIVIDER && comp != kfm.getPermanentFocusOwner(); wait++) {
                Thread.sleep(TIME_SLICE);
            }
        } else if(obj instanceof com.jogamp.newt.Window) {
            final com.jogamp.newt.Window win = (com.jogamp.newt.Window) obj;
            for (wait=0; wait<POLL_DIVIDER && !win.hasFocus(); wait++) {
                Thread.sleep(TIME_SLICE);
            }
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }
        return wait<POLL_DIVIDER;
    }

    /**
     *
     * @return True if the Window became the global focused Window within TIME_OUT
     */
    public static boolean waitForFocus(Object obj, FocusEventCountAdapter gain, 
                                                   FocusEventCountAdapter lost) throws InterruptedException {
        if(!waitForFocus(obj)) {
            return false;
        }
        if(null == gain) {
            return true;
        }
        
        int wait;
        for (wait=0; wait<POLL_DIVIDER; wait++) {
            if( ( null == lost || lost.focusLost() ) && gain.focusGained() ) {
                return true;
            }
            Thread.sleep(TIME_SLICE);
        }
        return false;
    }

    public static void assertRequestFocusAndWait(Robot robot, Object requestFocus, Object waitForFocus, 
                                              FocusEventCountAdapter gain, FocusEventCountAdapter lost)
        throws AWTException, InterruptedException, InvocationTargetException {

        int i = 0;
        boolean hasFocus = false;
        
        for(i=0; i < RETRY_NUMBER && !hasFocus; i++) {
            requestFocus(robot, requestFocus);
            hasFocus = waitForFocus(waitForFocus, gain, lost);
        }
        if(!hasFocus) {
            System.err.println("requestFocus: "+requestFocus);
            System.err.println("waitForFocus: "+waitForFocus);
            System.err.println("gain: "+gain);
            System.err.println("lost: "+lost);
        }
        Assert.assertTrue("Did not gain focus", hasFocus);
    }

    public static int keyType(int i, Robot robot, int keyCode,
                              Object obj, InputEventCountAdapter counter) throws InterruptedException, AWTException, InvocationTargetException 
    {
        int tc = 0;
        int j;
        final long t0 = System.currentTimeMillis();
        
        for(j=0; 1 > tc && j<RETRY_NUMBER; j++) {
            if(!hasFocus(obj)) {
                // focus lost for some reason, regain it programmatic
                if(DEBUG) { System.err.println(i+":"+j+" KC1.0: "+counter+" - regain focus"); }
                requestFocus(null, obj);
            }
            final int c0 = null!=counter ? counter.getCount() : 0;
            if(DEBUG) { System.err.println(i+":"+j+" KC1.1: "+counter); }
            robot.waitForIdle();
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
            if(DEBUG) { System.err.println(i+":"+j+" KC1.2: "+counter); }
            tc = ( null!=counter ? counter.getCount() : 1 ) - c0;
            for (int wait=0; wait<POLL_DIVIDER && 1 > tc; wait++) {
                robot.delay(TIME_SLICE);
                tc = counter.getCount() - c0;
            }
            if(DEBUG) { System.err.println(i+":"+j+" KC1.X: tc "+tc+", "+counter); }
        }
        Assert.assertEquals("Key ("+i+":"+j+") not typed one time", 1, tc);
        return (int) ( System.currentTimeMillis() - t0 ) ;
    }
    
    /**
     * @param keyCode TODO
     * @param counter shall return the number of keys typed (press + release)
     */
    public static void assertKeyType(Robot robot, int keyCode, int typeCount, 
                                     Object obj, InputEventCountAdapter counter) 
        throws AWTException, InterruptedException, InvocationTargetException {

        if(null == robot) {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
        }

        centerMouse(robot, obj, false);

        Assert.assertEquals("Key already pressed", false, counter.isPressed());
        
        if(DEBUG) {
            System.err.println("**************************************");
            System.err.println("KC0: "+counter);
        }
        
        final int c0 = counter.getCount();

        for(int i=0; i<typeCount; i++) {
            keyType(i, robot, keyCode, obj, counter); 
        }

        if(DEBUG) { System.err.println("KC3.0: "+counter); }
        Assert.assertEquals("Wrong key count", typeCount, counter.getCount()-c0);
    }

    static int mouseClick(int i, Robot robot, int mouseButton,
                          Object obj, InputEventCountAdapter counter) throws InterruptedException, AWTException, InvocationTargetException 
    {
        int j;
        int tc = 0;
        final long t0 = System.currentTimeMillis();
        
        for(j=0; 1 > tc && j<RETRY_NUMBER; j++) {
            if(!hasFocus(obj)) {
                // focus lost for some reason, regain it programmatic
                if(DEBUG) { System.err.println(i+":"+j+" MC1.0: "+counter+" - regain focus"); }
                requestFocus(null, obj);
            }
            final int c0 = null != counter ? counter.getCount() : 0;
            if(DEBUG) { System.err.println(i+":"+j+" MC1.1: "+counter); }
            robot.waitForIdle();
            robot.mousePress(mouseButton);
            robot.mouseRelease(mouseButton);
            if(DEBUG) { System.err.println(i+":"+j+" MC1.2: "+counter); }
            tc = ( null != counter ? counter.getCount() : 1 ) - c0;
            for (int wait=0; wait<POLL_DIVIDER && 1 > tc; wait++) {
                robot.delay(TIME_SLICE);
                tc = counter.getCount() - c0;
            }
            if(DEBUG) { System.err.println(i+":"+j+" MC1.X: tc "+tc+", "+counter); }
        }
        Assert.assertEquals("Mouse ("+i+":"+j+") not clicked one time", 1, tc);
        return (int) ( System.currentTimeMillis() - t0 ) ;
    }
    
    /**
     * @param mouseButton ie InputEvent.BUTTON1_MASK
     * @param clickCount ie 1, or 2
     */
    public static void assertMouseClick(Robot robot, int mouseButton, int clickCount,
                                        Object obj, InputEventCountAdapter counter) 
        throws AWTException, InterruptedException, InvocationTargetException {

        if(null == robot) {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
        }

        final int clickTO = getClickTimeout(obj);

        centerMouse(robot, obj, false);

        Assert.assertEquals("Mouse already pressed", false, counter.isPressed());
        
        if(DEBUG) {
            System.err.println("**************************************");
            System.err.println("MC0: "+counter);
        }
        
        final int c0 = counter.getCount();
        
        for(int i=0; i<clickCount; i++) {
            final int waited = mouseClick(i, robot, mouseButton, obj, counter);
            if(DEBUG) { System.err.println(i+": MC2.X: "+counter+", consumed: "+waited); }
            robot.delay( clickTO + 1 );
        }

        if(DEBUG) { System.err.println("MC3.0: "+counter); }
        Assert.assertEquals("Wrong mouse click count", clickCount, counter.getCount() - c0);
    }

    /**
     *
     * @return True if the FocusEventCountAdapter became the desired value within TIME_OUT
     */
    public static boolean waitForFocusCount(boolean desired, FocusEventCountAdapter eca) throws InterruptedException {
        for (int wait=0; wait<POLL_DIVIDER; wait++) {
            if( desired && eca.focusGained() || !desired && eca.focusLost() ) {
                return true;
            }
            Thread.sleep(TIME_SLICE);
        }
        return false;
    }

    /**
     *
     * @return True if the Component becomes <code>visible</code> within TIME_OUT
     */
    public static boolean waitForVisible(Object obj, boolean visible) throws InterruptedException {
        int wait;
        if(obj instanceof Component) {
            Component comp = (Component) obj;
            for (wait=0; wait<POLL_DIVIDER && visible != comp.isVisible(); wait++) {
                Thread.sleep(TIME_SLICE);
            }
        } else if(obj instanceof com.jogamp.newt.Window) {
            com.jogamp.newt.Window win = (com.jogamp.newt.Window) obj;
            for (wait=0; wait<POLL_DIVIDER && visible != win.isVisible(); wait++) {
                Thread.sleep(TIME_SLICE);
            }
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }
        return wait<POLL_DIVIDER;
    }

    /**
     *
     * @return True if the Component becomes realized (not displayable, native invalid) within TIME_OUT
     */
    public static boolean waitForRealized(Object obj, boolean realized) throws InterruptedException {
        int wait;
        if (obj instanceof Component) {
            Component comp = (Component) obj;
            for (wait=0; wait<POLL_DIVIDER && realized != comp.isDisplayable(); wait++) {
                Thread.sleep(TIME_SLICE);
            }
            // if GLCanvas, ensure it got also painted -> drawable.setRealized(true);
            if(wait<POLL_DIVIDER && comp instanceof GLCanvas) {
                GLCanvas glcanvas = (GLCanvas) comp;
                for (wait=0; wait<POLL_DIVIDER && realized != glcanvas.isRealized(); wait++) {
                    Thread.sleep(TIME_SLICE);
                }
                if(wait>=POLL_DIVIDER) {
                    // for some reason GLCanvas hasn't been painted yet, force it!
                    System.err.println("XXX: FORCE REPAINT PRE - canvas: "+glcanvas);
                    glcanvas.repaint();
                    for (wait=0; wait<POLL_DIVIDER && realized != glcanvas.isRealized(); wait++) {
                        Thread.sleep(TIME_SLICE);
                    }
                    System.err.println("XXX: FORCE REPAINT POST - canvas: "+glcanvas);
                }
                for (wait=0; wait<POLL_DIVIDER && realized != glcanvas.isRealized(); wait++) {
                    Thread.sleep(TIME_SLICE);
                }
            }            
        } else if(obj instanceof com.jogamp.newt.Window) {
            com.jogamp.newt.Window win = (com.jogamp.newt.Window) obj;
            for (wait=0; wait<POLL_DIVIDER && realized != win.isNativeValid(); wait++) {
                Thread.sleep(TIME_SLICE);
            }
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }
        return wait<POLL_DIVIDER;
    }

    /**
     * Programmatically issue windowClosing on AWT or NEWT.
     * Wait until the window is closing within TIME_OUT.
     *
     * @param obj either an AWT Window (Frame, JFrame) or NEWT Window
     * @param willClose indicating that the window will close, hence this method waits for the window to be closed
     * @return True if the Window is closing and closed (if willClose is true), each within TIME_OUT
     * @throws InterruptedException
     */
    public static boolean closeWindow(Object obj, boolean willClose) throws InterruptedException, InvocationTargetException {
        WindowClosingListener closingListener = addClosingListener(obj);
        if(obj instanceof java.awt.Window) {
            final java.awt.Window win = (java.awt.Window) obj;
            Toolkit tk = Toolkit.getDefaultToolkit();
            final EventQueue evtQ = tk.getSystemEventQueue();
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    evtQ.postEvent(new java.awt.event.WindowEvent(win, java.awt.event.WindowEvent.WINDOW_CLOSING));
                } });
        } else if(obj instanceof com.jogamp.newt.Window) {
            com.jogamp.newt.Window win = (com.jogamp.newt.Window) obj;
            WindowImplAccess.windowDestroyNotify(win);
        }
        int wait;
        for (wait=0; wait<POLL_DIVIDER && !closingListener.isWindowClosing(); wait++) {
            Thread.sleep(TIME_SLICE);
        }
        if(wait<POLL_DIVIDER && willClose) {
            for (wait=0; wait<POLL_DIVIDER && !closingListener.isWindowClosed(); wait++) {
                Thread.sleep(TIME_SLICE);
            }
        }
        return wait<POLL_DIVIDER;
    }

    public static WindowClosingListener addClosingListener(Object obj) throws InterruptedException {
        WindowClosingListener cl = null;
        if(obj instanceof java.awt.Window) {
            java.awt.Window win = (java.awt.Window) obj;
            AWTWindowClosingAdapter acl = new AWTWindowClosingAdapter();
            win.addWindowListener(acl);
            cl = acl;
        } else if(obj instanceof com.jogamp.newt.Window) {
            com.jogamp.newt.Window win = (com.jogamp.newt.Window) obj;
            NEWTWindowClosingAdapter ncl = new NEWTWindowClosingAdapter();
            win.addWindowListener(ncl);
            cl = ncl;
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }
        return cl;
    }
    public static interface WindowClosingListener {
        void reset();
        public boolean isWindowClosing();
        public boolean isWindowClosed();
    }
    static class AWTWindowClosingAdapter
            extends java.awt.event.WindowAdapter implements WindowClosingListener
    {
        volatile boolean closing = false;
        volatile boolean closed = false;

        public void reset() {
            closing = false;
            closed = false;
        }
        public boolean isWindowClosing() {
            return closing;
        }
        public boolean isWindowClosed() {
            return closed;
        }
        public void windowClosing(java.awt.event.WindowEvent e) {
            closing = true;
        }
        public void windowClosed(java.awt.event.WindowEvent e) {
            closed = true;
        }
    }
    static class NEWTWindowClosingAdapter
            extends com.jogamp.newt.event.WindowAdapter implements WindowClosingListener
    {
        volatile boolean closing = false;
        volatile boolean closed = false;

        public void reset() {
            closing = false;
            closed = false;
        }
        public boolean isWindowClosing() {
            return closing;
        }
        public boolean isWindowClosed() {
            return closed;
        }
        public void windowDestroyNotify(com.jogamp.newt.event.WindowEvent e) {
            closing = true;
        }
        public void windowDestroyed(com.jogamp.newt.event.WindowEvent e) {
            closed = true;
        }
    }

}


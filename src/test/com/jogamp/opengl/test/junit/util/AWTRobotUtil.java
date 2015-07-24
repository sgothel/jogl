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
import jogamp.newt.awt.event.AWTNewtEventFactory;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;

import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;

import org.junit.Assert;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.util.awt.AWTEDTExecutor;
import com.jogamp.newt.event.WindowEvent;

public class AWTRobotUtil {

    static final boolean DEBUG = false;

    public static final int RETRY_NUMBER  =   5;
    public static final int ROBOT_DELAY   = 100; // ms
    public static final int TIME_OUT     = 2000; // 2s
    public static final int POLL_DIVIDER   = 20; // TO/20
    public static final int TIME_SLICE   = TIME_OUT / POLL_DIVIDER ;
    public static Integer AWT_CLICK_TO = null;

    static class OurUncaughtExceptionHandler implements UncaughtExceptionHandler {
        @Override
        public void uncaughtException(final Thread t, final Throwable e) {
            System.err.println("*** AWTRobotUtil: UncaughtException (this Thread "+Thread.currentThread().getName()+") : Thread <"+t.getName()+">, "+e.getClass().getName()+": "+e.getMessage());
            e.printStackTrace();
        }
    }

    static {
        Thread.setDefaultUncaughtExceptionHandler( new OurUncaughtExceptionHandler() );
        // System.err.println("AWT EDT alive: "+isAWTEDTAlive());
    }

    /** Probes whether AWT's EDT is alive or not. */
    public static boolean isAWTEDTAlive() {
        if( EventQueue.isDispatchThread() ) {
            return true;
        }
        synchronized ( awtEDTAliveSync ) {
            awtEDTAliveFlag = false;
            EventQueue.invokeLater(aliveRun);
            for (int wait=0; wait<POLL_DIVIDER && !awtEDTAliveFlag; wait++) {
                try {
                    Thread.sleep(TIME_SLICE);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return awtEDTAliveFlag;
        }
    }
    private static Runnable aliveRun = new Runnable() { public void run() { awtEDTAliveFlag = true; } };
    private static Object awtEDTAliveSync = new Object();
    private static volatile boolean awtEDTAliveFlag = false;

    /** Throws Error if {@link #isAWTEDTAlive()} returns false. */
    public static void validateAWTEDTIsAlive() {
        if( !isAWTEDTAlive() ) {
            throw new Error("AWT EDT not alive");
        }
    }

    /** Issuing {@link #validateAWTEDTIsAlive()} before calling {@link Robot#waitForIdle()}. */
    public static void waitForIdle(final Robot robot) {
        validateAWTEDTIsAlive();
        robot.waitForIdle();
    }

    public static void clearAWTFocus(Robot robot) throws InterruptedException, InvocationTargetException, AWTException {
        if(null == robot) {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
        }
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                System.err.println("******** clearAWTFocus.0");
                java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
            }});
        robot.delay(ROBOT_DELAY);
        System.err.println("******** clearAWTFocus.X");
    }

    public static int[] getCenterLocation(final Object obj, final boolean onTitleBarIfWindow)
        throws InterruptedException, InvocationTargetException {
        if(obj instanceof com.jogamp.newt.Window) {
            return getCenterLocationNEWT((com.jogamp.newt.Window)obj, onTitleBarIfWindow);
        } else if(NativeWindowFactory.isAWTAvailable() && obj instanceof java.awt.Component) {
            return getCenterLocationAWT((java.awt.Component)obj, onTitleBarIfWindow);
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }
    }
    private static int[] getCenterLocationNEWT(final com.jogamp.newt.Window win, final boolean onTitleBarIfWindow)
        throws InterruptedException, InvocationTargetException {

        final com.jogamp.nativewindow.util.Point p0 = win.getLocationOnScreen(null);
        if( onTitleBarIfWindow ) {
            final com.jogamp.nativewindow.util.InsetsImmutable insets = win.getInsets();
            p0.translate(win.getWidth()/2, insets.getTopHeight()/2);
        } else {
            p0.translate(win.getWidth()/2, win.getHeight()/2);
        }
        return new int[] { p0.getX(), p0.getY() };
    }
    private static int[] getCenterLocationAWT(final java.awt.Component comp, final boolean onTitleBarIfWindow)
        throws InterruptedException, InvocationTargetException {
        int x0, y0;
        final java.awt.Point p0 = comp.getLocationOnScreen();
        final java.awt.Rectangle r0 = comp.getBounds();
        if( onTitleBarIfWindow && comp instanceof java.awt.Window) {
            final java.awt.Window window = (java.awt.Window) comp;
            final java.awt.Insets insets = window.getInsets();
            y0 = (int) ( p0.getY() +    insets.top / 2.0 + .5 ) ;
        } else {
            y0 = (int) ( p0.getY() + r0.getHeight() / 2.0 + .5 ) ;
        }
        x0 = (int) ( p0.getX() + r0.getWidth() / 2.0 + .5 ) ;
        return new int[] { x0, y0 };
    }

    public static int[] getClientLocation(final Object obj, final int x, final int y)
        throws InterruptedException, InvocationTargetException {
        if(obj instanceof com.jogamp.newt.Window) {
            return getClientLocationNEWT((com.jogamp.newt.Window)obj, x, y);
        } else if(NativeWindowFactory.isAWTAvailable() && obj instanceof java.awt.Component) {
            return getClientLocationAWT((java.awt.Component)obj, x, y);
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }
    }
    private static int[] getClientLocationNEWT(final com.jogamp.newt.Window win, final int x, final int y)
        throws InterruptedException, InvocationTargetException {
        final com.jogamp.nativewindow.util.Point p0 = win.getLocationOnScreen(null);
        return new int[] { p0.getX(), p0.getY() };
    }
    private static int[] getClientLocationAWT(final java.awt.Component comp, final int x, final int y)
        throws InterruptedException, InvocationTargetException {
        final java.awt.Point p0 = comp.getLocationOnScreen();
        return new int[] { (int)p0.getX(), (int)p0.getY() };
    }

    public static void awtRobotMouseMove(final Robot robot, final int x, final int y) {
        robot.mouseMove( x, y );
        robot.delay(ROBOT_DELAY);
    }

    /**
     * toFront, call setVisible(true) and toFront(),
     * after positioning the mouse in the middle of the window via robot.
     * If the given robot is null, a new one is created (waitForIdle=true).
     *
     * @return True if the Window became the global focused Window within TIME_OUT
     */
    public static boolean toFrontAndRequestFocus(Robot robot, final java.awt.Window window)
        throws AWTException, InterruptedException, InvocationTargetException {

        // just for event tracing ..
        final AWTWindowFocusAdapter winFA = new AWTWindowFocusAdapter("window");
        window.addWindowFocusListener(winFA);

        if(null == robot) {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
        }
        final int[] p0 = getCenterLocation(window, false);
        System.err.println("toFront: robot pos: "+p0[0]+"/"+p0[1]);
        awtRobotMouseMove(robot, p0[0], p0[1] );

        int wait=0;
        do {
            final int _wait = wait;
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    if(0==_wait) {
                        window.setVisible(true);
                        window.toFront();
                    }
                    window.requestFocus();
                }});
            Thread.sleep(TIME_SLICE);
            wait++;
        } while (wait<POLL_DIVIDER && !window.hasFocus());
        final boolean success = wait<POLL_DIVIDER;

        window.removeWindowFocusListener(winFA);
        if(!success) {
            System.err.println("*** AWTRobotUtil.toFrontAndRequestFocus() UI failure");
            System.err.println("*** window: "+window);
            System.err.println("*** window.hasFocus(): "+window.hasFocus());
            ExceptionUtils.dumpStack(System.err);
        }
        return success;
    }

    /**
     * centerMouse
     * @param onTitleBarIfWindow TODO
     */
    public static void centerMouse(Robot robot, final Object obj, final boolean onTitleBarIfWindow)
        throws AWTException, InterruptedException, InvocationTargetException {

        if(null == robot) {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
        }

        final int[] p0 = getCenterLocation(obj, onTitleBarIfWindow);
        System.err.println("centerMouse: robot pos: "+p0[0]+"x"+p0[1]+", onTitleBarIfWindow: "+onTitleBarIfWindow);
        awtRobotMouseMove(robot, p0[0], p0[1] );
    }

    public static void setMouseToClientLocation(Robot robot, final Object obj, final int x, final int y)
        throws AWTException, InterruptedException, InvocationTargetException {

        if(null == robot) {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
        }

        final int[] p0 = getClientLocation(obj, x, y);
        awtRobotMouseMove(robot, p0[0], p0[1] );
    }

    public static int getClickTimeout(final Object obj) {
        if(obj instanceof com.jogamp.newt.Window) {
            return com.jogamp.newt.event.MouseEvent.getClickTimeout();
        } else if(NativeWindowFactory.isAWTAvailable() && obj instanceof java.awt.Component) {
            if(null == AWT_CLICK_TO) {
                AWT_CLICK_TO =
                    (Integer) java.awt.Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
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
     * otherwise programmatic, ie call requestFocus
     */
    public static void requestFocus(final Robot robot, final Object obj)
        throws AWTException, InterruptedException, InvocationTargetException {
        requestFocus(robot, obj, true);
    }

    /**
     * requestFocus, if robot is valid, use mouse operation,
     * otherwise programmatic, ie call requestFocus
     */
    public static void requestFocus(final Robot robot, final Object obj, final boolean onTitleBarIfWindow)
        throws AWTException, InterruptedException, InvocationTargetException {
        if(null != robot) {
            final int mouseButton = java.awt.event.InputEvent.BUTTON1_MASK;
            centerMouse(robot, obj, onTitleBarIfWindow);

            waitForIdle(robot);
            robot.mousePress(mouseButton);
            robot.mouseRelease(mouseButton);
            final int d = getClickTimeout(obj) + 1;
            robot.delay( d );
            System.err.println("requestFocus: click, d: "+d+" ms");
        } else {
            if(obj instanceof com.jogamp.newt.Window) {
                requestFocusNEWT((com.jogamp.newt.Window) obj, onTitleBarIfWindow);
            } else if(NativeWindowFactory.isAWTAvailable() && obj instanceof java.awt.Component) {
                requestFocusAWT((java.awt.Component) obj, onTitleBarIfWindow);
            } else {
                throw new RuntimeException("Neither AWT nor NEWT: "+obj);
            }
        }
    }
    private static void requestFocusNEWT(final com.jogamp.newt.Window win, final boolean onTitleBarIfWindow)
        throws AWTException, InterruptedException, InvocationTargetException {
        win.requestFocus();
        System.err.println("requestFocus: NEWT Component");
    }
    private static void requestFocusAWT(final java.awt.Component comp, final boolean onTitleBarIfWindow)
        throws AWTException, InterruptedException, InvocationTargetException {
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                comp.requestFocus();
                System.err.println("requestFocus: AWT Component");
            }});
    }

    public static void requestFocus(final Robot robot, final Object obj, final int x, final int y)
        throws AWTException, InterruptedException, InvocationTargetException {
        validateAWTEDTIsAlive();

        final boolean idling = robot.isAutoWaitForIdle();
        final int mouseButton = java.awt.event.InputEvent.BUTTON1_MASK;
        robot.mouseMove( x, y );
        if( idling ) {
            robot.waitForIdle();
        } else {
            try { Thread.sleep(50); } catch (final InterruptedException e) { }
        }
        robot.mousePress(mouseButton);
        robot.mouseRelease(mouseButton);
        final int d = getClickTimeout(obj) + 1;
        robot.delay( d );
    }

    public static boolean hasFocus(final Object obj) {
        if(obj instanceof com.jogamp.newt.Window) {
            return ((com.jogamp.newt.Window) obj).hasFocus();
        } else if(NativeWindowFactory.isAWTAvailable() && obj instanceof java.awt.Component) {
            final java.awt.Component comp = (java.awt.Component) obj;
            final java.awt.KeyboardFocusManager kfm = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager();
            return comp == kfm.getPermanentFocusOwner();
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }
    }

    /**
     *
     * @return True if the Window became the global focused Window within TIME_OUT
     */
    public static boolean waitForFocus(final Object obj) throws InterruptedException {
        int wait;
        if(obj instanceof com.jogamp.newt.Window) {
            final com.jogamp.newt.Window win = (com.jogamp.newt.Window) obj;
            for (wait=0; wait<POLL_DIVIDER && !win.hasFocus(); wait++) {
                Thread.sleep(TIME_SLICE);
            }
        } else if(NativeWindowFactory.isAWTAvailable() && obj instanceof java.awt.Component) {
            final java.awt.Component comp = (java.awt.Component) obj;
            final java.awt.KeyboardFocusManager kfm = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager();
            for (wait=0; wait<POLL_DIVIDER && comp != kfm.getPermanentFocusOwner(); wait++) {
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
    public static boolean waitForFocus(final FocusEventCountAdapter gain,
                                       final FocusEventCountAdapter lost) throws InterruptedException {
        int wait;
        for (wait=0; wait<POLL_DIVIDER; wait++) {
            if( ( null == lost || lost.focusLost() ) && ( null == gain || gain.focusGained() ) ) {
                return true;
            }
            Thread.sleep(TIME_SLICE);
        }
        return false;
    }

    /**
     *
     * @return True if the Window became the global focused Window within TIME_OUT
     */
    public static boolean waitForFocus(final Object obj, final FocusEventCountAdapter gain,
                                                   final FocusEventCountAdapter lost) throws InterruptedException {
        if(!waitForFocus(obj)) {
            return false;
        }
        return waitForFocus(gain, lost);
    }

    public static void assertRequestFocusAndWait(final Robot robot, final Object requestFocus, final Object waitForFocus,
                                              final FocusEventCountAdapter gain, final FocusEventCountAdapter lost)
        throws AWTException, InterruptedException, InvocationTargetException {

        int i = 0;
        boolean hasFocus = false;

        for(i=0; i < RETRY_NUMBER && !hasFocus; i++) {
            requestFocus(robot, requestFocus);
            hasFocus = waitForFocus(waitForFocus, gain, lost);
        }
        if(!hasFocus) {
            System.err.print("*** AWTRobotUtil.assertRequestFocusAndWait() ");
            if( ( null == gain || gain.focusGained() ) && ( null == lost || !lost.focusLost() ) ) {
                // be error tolerant here, some impl. may lack focus-lost events (OS X)
                System.err.println("minor UI failure");
                hasFocus = true;
            } else {
                System.err.println("major UI failure");
            }
            if(requestFocus instanceof NativeWindow) {
                System.err.println("*** requestFocus.hasFocus() -  NW: "+((NativeWindow)requestFocus).hasFocus());
            } else if(NativeWindowFactory.isAWTAvailable() && requestFocus instanceof java.awt.Component) {
                System.err.println("*** requestFocus.hasFocus() - AWT: "+((java.awt.Component)requestFocus).hasFocus());
            }
            if(waitForFocus instanceof NativeWindow) {
                System.err.println("*** waitForFocus.hasFocus() -  NW: "+((NativeWindow)waitForFocus).hasFocus());
            } else if(NativeWindowFactory.isAWTAvailable() && waitForFocus instanceof java.awt.Component) {
                System.err.println("*** waitForFocus.hasFocus() - AWT: "+((java.awt.Component)waitForFocus).hasFocus());
            }
            System.err.println("*** gain: "+gain);
            System.err.println("*** lost: "+lost);
            ExceptionUtils.dumpStack(System.err);
        }
        Assert.assertTrue("Did not gain focus", hasFocus);
    }

    private static void awtRobotKeyPress(final Robot robot, final int keyCode, final int msDelay) {
        robot.keyPress(keyCode);
        robot.delay(msDelay);
    }
    private static void awtRobotKeyRelease(final Robot robot, final int keyCode, final int msDelay) {
        robot.keyRelease(keyCode);
        robot.delay(msDelay);
    }

    public static int keyType(final int i, final Robot robot, final int keyCode,
                              final Object obj, final KeyEventCountAdapter counter) throws InterruptedException, AWTException, InvocationTargetException
    {
        int tc = 0;
        int j;
        final long t0 = System.currentTimeMillis();
        final int c0 = null!=counter ? counter.getCount() : 0;

        for(j=0; 1 > tc && j<RETRY_NUMBER; j++) {
            if(!hasFocus(obj)) {
                // focus lost for some reason, regain it programmatic
                if(DEBUG) { System.err.println(i+":"+j+" KC1.0: "+counter+" - regain focus on thread "+Thread.currentThread().getName()); }
                requestFocus(null, obj);
            }
            waitForIdle(robot);
            if(DEBUG) { System.err.println(i+":"+j+" KC1.1: "+counter+" on thread "+Thread.currentThread().getName()); }
            awtRobotKeyPress(robot, keyCode, 50);
            if(DEBUG) { System.err.println(i+":"+j+" KC1.2: "+counter+" on thread "+Thread.currentThread().getName()); }
            awtRobotKeyRelease(robot, keyCode, 100);
            waitForIdle(robot);
            if(DEBUG) { System.err.println(i+":"+j+" KC1.3: "+counter); }
            tc = ( null!=counter ? counter.getCount() : 1 ) - c0;
            for (int wait=0; wait<POLL_DIVIDER && 1 > tc; wait++) {
                if(DEBUG) { System.err.println(i+":"+j+" KC1.4."+wait+": "+counter+", sleep for "+TIME_OUT+"ms"); }
                robot.delay(TIME_SLICE);
                tc = counter.getCount() - c0;
            }
            if(DEBUG) { System.err.println(i+":"+j+" KC1.X: tc "+tc+", "+counter+" on thread "+Thread.currentThread().getName()); }
        }
        Assert.assertEquals("Key ("+i+":"+j+") not typed one time on thread "+Thread.currentThread().getName(), 1, tc);
        return (int) ( System.currentTimeMillis() - t0 ) ;
    }

    /** No validation is performed .. */
    public static int keyPress(final int i, final Robot robot, final boolean press, final int keyCode, final int msDelay) {
        final long t0 = System.currentTimeMillis();
        if(press) {
            awtRobotKeyPress(robot, keyCode, msDelay);
        } else {
            awtRobotKeyRelease(robot, keyCode, msDelay);
        }

        return (int) ( System.currentTimeMillis() - t0 ) ;
    }

    /** No validation is performed .. */
    public static int newtKeyPress(final int i, final Robot robot, final boolean press, final short newtKeyCode, final int msDelay) {
        final int keyCode = AWTNewtEventFactory.newtKeyCode2AWTKeyCode(newtKeyCode);
        final long t0 = System.currentTimeMillis();
        if(press) {
            awtRobotKeyPress(robot, keyCode, msDelay);
        } else {
            awtRobotKeyRelease(robot, keyCode, msDelay);
        }

        return (int) ( System.currentTimeMillis() - t0 ) ;
    }

    /**
     * @param keyCode TODO
     * @param counter shall return the number of keys typed (press + release)
     */
    public static void assertKeyType(Robot robot, final int keyCode, final int typeCount,
                                     final Object obj, final KeyEventCountAdapter counter)
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

    /**
     * @param keyCode TODO
     * @param counter shall return the number of keys typed (press + release)
     */
    public static void assertKeyPress(Robot robot, final int keyCode, final int typeCount,
                                      final Object obj, final KeyEventCountAdapter counter)
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

    public static void mouseMove(final Robot robot, final Point destination, final int iter, final int delay) {
        final Point origin = MouseInfo.getPointerInfo().getLocation();
        for (int i = 1; i <= iter; i++) {
            final float alpha = i / (float) iter;
            robot.mouseMove((int) (origin.x * (1 - alpha) + destination.x * alpha),
                            (int) (origin.y * (1 - alpha) + destination.y * alpha));
            robot.delay(delay);
        }
    }
    public static void mouseClick(final Robot robot, final Point pos, final int moveIter, final int moveDelay, final int actionDelay) {
        robot.delay(actionDelay);
        mouseMove(robot, pos, moveIter, moveDelay);

        robot.delay(actionDelay);
        robot.mousePress(java.awt.event.InputEvent.BUTTON1_MASK);
        robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_MASK);
        robot.delay(actionDelay);
    }

    static int mouseClick(final int i, final Robot robot, final int mouseButton,
                          final Object obj, final InputEventCountAdapter counter) throws InterruptedException, AWTException, InvocationTargetException
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
            waitForIdle(robot);
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
    public static void assertMouseClick(Robot robot, final int mouseButton, final int clickCount,
                                        final Object obj, final InputEventCountAdapter counter)
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
     * @return True if the Component becomes <code>visible</code> within TIME_OUT
     */
    public static boolean waitForVisible(final Object obj, final boolean visible) throws InterruptedException {
        int wait;
        if(obj instanceof com.jogamp.newt.Window) {
            final com.jogamp.newt.Window win = (com.jogamp.newt.Window) obj;
            for (wait=0; wait<POLL_DIVIDER && visible != win.isVisible(); wait++) {
                Thread.sleep(TIME_SLICE);
            }
        } else if(NativeWindowFactory.isAWTAvailable() && obj instanceof java.awt.Component) {
            final java.awt.Component comp = (java.awt.Component) obj;
            for (wait=0; wait<POLL_DIVIDER && visible != comp.isShowing(); wait++) {
                Thread.sleep(TIME_SLICE);
            }
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }
        return wait<POLL_DIVIDER;
    }

    /**
     *
     * @return True if the GLDrawable receives the expected size within TIME_OUT
     */
    public static boolean waitForSize(final GLDrawable drawable, final int width, final int height) throws InterruptedException {
        int wait;
        for (wait=0; wait<POLL_DIVIDER && ( width != drawable.getSurfaceWidth() || height != drawable.getSurfaceHeight() ) ; wait++) {
            Thread.sleep(TIME_SLICE);
        }
        return wait<POLL_DIVIDER;
    }

    /**
     * @param obj the component to wait for
     * @param realized true if waiting for component to become realized, otherwise false
     * @return True if the Component becomes realized (not displayable, native invalid) within TIME_OUT
     * @throws InterruptedException
     */
    public static boolean waitForRealized(final Object obj, final boolean realized) throws InterruptedException {
        return waitForRealized(obj, null, realized);
    }

    /**
     * @param obj the component to wait for
     * @param waitAction if not null, Runnable shall wait {@link #TIME_SLICE} ms, if appropriate
     * @param realized true if waiting for component to become realized, otherwise false
     * @return True if the Component becomes realized (not displayable, native invalid) within TIME_OUT
     * @throws InterruptedException
     */
    public static boolean waitForRealized(final Object obj, final Runnable waitAction, final boolean realized) throws InterruptedException {
        long t0 = System.currentTimeMillis();
        long t1 = t0;
        if(obj instanceof com.jogamp.newt.Screen) {
            final com.jogamp.newt.Screen screen = (com.jogamp.newt.Screen) obj;
            while( (t1-t0) < TIME_OUT && realized != screen.isNativeValid() ) {
                if( null != waitAction ) {
                    waitAction.run();
                } else {
                    Thread.sleep(TIME_SLICE);
                }
                t1 = System.currentTimeMillis();
            }
        } else if(obj instanceof com.jogamp.newt.Window) {
            final com.jogamp.newt.Window win = (com.jogamp.newt.Window) obj;
            while( (t1-t0) < TIME_OUT && realized != win.isNativeValid() ) {
                if( null != waitAction ) {
                    waitAction.run();
                } else {
                    Thread.sleep(TIME_SLICE);
                }
                t1 = System.currentTimeMillis();
            }
        } else if (NativeWindowFactory.isAWTAvailable() && obj instanceof java.awt.Component) {
            final java.awt.Component comp = (java.awt.Component) obj;
            while( (t1-t0) < TIME_OUT && realized != comp.isShowing() ) {
                if( null != waitAction ) {
                    waitAction.run();
                } else {
                    Thread.sleep(TIME_SLICE);
                }
                t1 = System.currentTimeMillis();
            }
            // if GLCanvas, ensure it got also painted -> drawable.setRealized(true);
            if( (t1-t0) < TIME_OUT && comp instanceof GLAutoDrawable) {
                final GLAutoDrawable glad = (GLAutoDrawable) comp;
                t0 = System.currentTimeMillis();
                while( (t1-t0) < TIME_OUT && realized != glad.isRealized() ) {
                    if( null != waitAction ) {
                        waitAction.run();
                    } else {
                        Thread.sleep(TIME_SLICE);
                    }
                    t1 = System.currentTimeMillis();
                }
                if( (t1-t0) >= TIME_OUT ) {
                    // for some reason GLCanvas hasn't been painted yet, force it!
                    System.err.println("XXX: FORCE REPAINT PRE - glad: "+glad);
                    comp.repaint();
                    t0 = System.currentTimeMillis();
                    while( (t1-t0) < TIME_OUT && realized != glad.isRealized() ) {
                        if( null != waitAction ) {
                            waitAction.run();
                        } else {
                            Thread.sleep(TIME_SLICE);
                        }
                        t1 = System.currentTimeMillis();
                    }
                    System.err.println("XXX: FORCE REPAINT POST - glad: "+glad);
                }
            }
        } else if(obj instanceof GLAutoDrawable) {
            final GLAutoDrawable glad = (GLAutoDrawable) obj;
            while( (t1-t0) < TIME_OUT && realized != glad.isRealized() ) {
                if( null != waitAction ) {
                    waitAction.run();
                } else {
                    Thread.sleep(TIME_SLICE);
                }
                t1 = System.currentTimeMillis();
            }
        } else {
            throw new RuntimeException("Neither AWT nor NEWT nor GLAutoDrawable: "+obj);
        }
        return (t1-t0) < TIME_OUT;
    }

    /**
     *
     * @return True if the GLContext becomes created or not within TIME_OUT
     */
    public static boolean waitForContextCreated(final GLAutoDrawable autoDrawable, final boolean created) throws InterruptedException {
        if( null == autoDrawable ) {
            return !created;
        }
        int wait;
        for (wait=0; wait<POLL_DIVIDER ; wait++) {
            final GLContext ctx = autoDrawable.getContext();
            if( created ) {
                if( null != ctx && ctx.isCreated() ) {
                    break;
                }
            } else {
                if( null == ctx || !ctx.isCreated() ) {
                    break;
                }
            }
            Thread.sleep(TIME_SLICE);
        }
        return wait<POLL_DIVIDER;
    }

    /**
     * Programmatically issue windowClosing on AWT or NEWT.
     * Wait until the window is closing within TIME_OUT.
     *
     * @param obj either an AWT Window (Frame, JFrame) or NEWT Window
     * @param willClose indicating that the window will close, hence this method waits for the window to be closed
     * @param wcl the WindowClosingListener to determine whether the AWT or NEWT widget has been closed. It should be attached
     *            to the widget ASAP before any other listener, e.g. via {@link #addClosingListener(Object)}.
     *            The WindowClosingListener will be reset before attempting to close the widget.
     * @return True if the Window is closing and closed (if willClose is true), each within TIME_OUT
     * @throws InterruptedException
     */
    public static boolean closeWindow(final Object obj, final boolean willClose, final WindowClosingListener closingListener) throws InterruptedException {
        closingListener.reset();
        if(obj instanceof java.awt.Window) {
            final java.awt.Window win = (java.awt.Window) obj;
            final java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();
            final java.awt.EventQueue evtQ = tk.getSystemEventQueue();
            AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                public void run() {
                    evtQ.postEvent(new java.awt.event.WindowEvent(win, java.awt.event.WindowEvent.WINDOW_CLOSING));
                } });
        } else if(obj instanceof com.jogamp.newt.Window) {
            final com.jogamp.newt.Window win = (com.jogamp.newt.Window) obj;
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

    public static WindowClosingListener addClosingListener(final Object obj) {
        WindowClosingListener cl = null;
        if(obj instanceof java.awt.Window) {
            final java.awt.Window win = (java.awt.Window) obj;
            final AWTWindowClosingAdapter acl = new AWTWindowClosingAdapter();
            AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                public void run() {
                    win.addWindowListener(acl);
                } } );
            cl = acl;
        } else if(obj instanceof com.jogamp.newt.Window) {
            final com.jogamp.newt.Window win = (com.jogamp.newt.Window) obj;
            final NEWTWindowClosingAdapter ncl = new NEWTWindowClosingAdapter();
            win.addWindowListener(ncl);
            cl = ncl;
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }
        return cl;
    }
    public static interface WindowClosingListener {
        void reset();
        public int getWindowClosingCount();
        public int getWindowClosedCount();
        public boolean isWindowClosing();
        public boolean isWindowClosed();
    }
    static class AWTWindowClosingAdapter
            extends java.awt.event.WindowAdapter implements WindowClosingListener
    {
        AtomicInteger closing = new AtomicInteger(0);
        AtomicInteger closed = new AtomicInteger(0);

        public void reset() {
            closing.set(0);
            closed.set(0);
        }
        public int getWindowClosingCount() {
            return closing.get();
        }
        public int getWindowClosedCount() {
            return closed.get();
        }
        public boolean isWindowClosing() {
            return 0 < closing.get();
        }
        public boolean isWindowClosed() {
            return 0 < closed.get();
        }
        public void windowClosing(final java.awt.event.WindowEvent e) {
            closing.incrementAndGet();
            System.err.println("AWTWindowClosingAdapter.windowClosing: "+this);
        }
        public void windowClosed(final java.awt.event.WindowEvent e) {
            closed.incrementAndGet();
            System.err.println("AWTWindowClosingAdapter.windowClosed: "+this);
        }
        public String toString() {
            return "AWTWindowClosingAdapter[closing "+closing+", closed "+closed+"]";
        }
    }
    static class NEWTWindowClosingAdapter
            extends com.jogamp.newt.event.WindowAdapter implements WindowClosingListener
    {
        AtomicInteger closing = new AtomicInteger(0);
        AtomicInteger closed = new AtomicInteger(0);

        public void reset() {
            closing.set(0);
            closed.set(0);
        }
        public int getWindowClosingCount() {
            return closing.get();
        }
        public int getWindowClosedCount() {
            return closed.get();
        }
        public boolean isWindowClosing() {
            return 0 < closing.get();
        }
        public boolean isWindowClosed() {
            return 0 < closed.get();
        }
        public void windowDestroyNotify(final WindowEvent e) {
            closing.incrementAndGet();
            System.err.println("NEWTWindowClosingAdapter.windowDestroyNotify: "+this);
        }
        public void windowDestroyed(final WindowEvent e) {
            closed.incrementAndGet();
            System.err.println("NEWTWindowClosingAdapter.windowDestroyed: "+this);
        }
        public String toString() {
            return "NEWTWindowClosingAdapter[closing "+closing+", closed "+closed+"]";
        }
    }

}


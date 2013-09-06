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
import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Robot;

import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawable;

import org.junit.Assert;

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
    
    static Object awtEDTAliveSync = new Object();
    static volatile boolean awtEDTAliveFlag = false;    
    
    static class OurUncaughtExceptionHandler implements UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
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
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    awtEDTAliveFlag = true;
                }                            
            });
            for (int wait=0; wait<POLL_DIVIDER && !awtEDTAliveFlag; wait++) {
                try {
                    Thread.sleep(TIME_SLICE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return awtEDTAliveFlag;
        }
    }
    /** Throws Error if {@link #isAWTEDTAlive()} returns false. */
    public static void validateAWTEDTIsAlive() {
        if( !isAWTEDTAlive() ) {
            throw new Error("AWT EDT not alive");
        }
    }
    
    /** Issuing {@link #validateAWTEDTIsAlive()} before calling {@link Robot#waitForIdle()}. */
    public static void waitForIdle(Robot robot) {
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
    
    public static int[] getCenterLocation(Object obj, boolean onTitleBarIfWindow) 
        throws InterruptedException, InvocationTargetException {
        if(obj instanceof com.jogamp.newt.Window) {
            return getCenterLocationNEWT((com.jogamp.newt.Window)obj, onTitleBarIfWindow);
        } else if(NativeWindowFactory.isAWTAvailable() && obj instanceof java.awt.Component) {
            return getCenterLocationAWT((java.awt.Component)obj, onTitleBarIfWindow);
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }        
    }
    private static int[] getCenterLocationNEWT(com.jogamp.newt.Window win, boolean onTitleBarIfWindow) 
        throws InterruptedException, InvocationTargetException {

        javax.media.nativewindow.util.Point p0 = win.getLocationOnScreen(null);            
        if( onTitleBarIfWindow ) {
            javax.media.nativewindow.util.InsetsImmutable insets = win.getInsets();
            p0.translate(win.getWidth()/2, insets.getTopHeight()/2);                
        } else {
            p0.translate(win.getWidth()/2, win.getHeight()/2);
        }
        return new int[] { p0.getX(), p0.getY() }; 
    }    
    private static int[] getCenterLocationAWT(java.awt.Component comp, boolean onTitleBarIfWindow) 
        throws InterruptedException, InvocationTargetException {
        int x0, y0;
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
        return new int[] { x0, y0 }; 
    }

    public static int[] getClientLocation(Object obj, int x, int y) 
        throws InterruptedException, InvocationTargetException {
        if(obj instanceof com.jogamp.newt.Window) {
            return getClientLocationNEWT((com.jogamp.newt.Window)obj, x, y);
        } else if(NativeWindowFactory.isAWTAvailable() && obj instanceof java.awt.Component) {
            return getClientLocationAWT((java.awt.Component)obj, x, y);
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }    
    }
    private static int[] getClientLocationNEWT(com.jogamp.newt.Window win, int x, int y) 
        throws InterruptedException, InvocationTargetException {
        javax.media.nativewindow.util.Point p0 = win.getLocationOnScreen(null);            
        return new int[] { p0.getX(), p0.getY() }; 
    }
    private static int[] getClientLocationAWT(java.awt.Component comp, int x, int y) 
        throws InterruptedException, InvocationTargetException {
        java.awt.Point p0 = comp.getLocationOnScreen();            
        return new int[] { (int)p0.getX(), (int)p0.getY() }; 
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
        AWTWindowFocusAdapter winFA = new AWTWindowFocusAdapter("window");
        window.addWindowFocusListener(winFA);
        
        if(null == robot) {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
        }
        int[] p0 = getCenterLocation(window, false);
        System.err.println("toFront: robot pos: "+p0[0]+"x"+p0[1]);
        robot.mouseMove( p0[0], p0[1] );
        robot.delay(ROBOT_DELAY);

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
            Thread.dumpStack();
        }
        return success;
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

        int[] p0 = getCenterLocation(obj, onTitleBarIfWindow);
        System.err.println("centerMouse: robot pos: "+p0[0]+"x"+p0[1]+", onTitleBarIfWindow: "+onTitleBarIfWindow);

        robot.mouseMove( p0[0], p0[1] );
        robot.delay(ROBOT_DELAY);
    }

    public static void setMouseToClientLocation(Robot robot, Object obj, int x, int y) 
        throws AWTException, InterruptedException, InvocationTargetException {
        
        if(null == robot) {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
        }
        
        int[] p0 = getClientLocation(obj, x, y);

        robot.mouseMove( p0[0], p0[1] );
        robot.delay(ROBOT_DELAY);
    }
    
    public static int getClickTimeout(Object obj) {
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
    public static void requestFocus(Robot robot, Object obj) 
        throws AWTException, InterruptedException, InvocationTargetException {
        requestFocus(robot, obj, true);
    }
    
    /**
     * requestFocus, if robot is valid, use mouse operation,
     * otherwise programmatic, ie call requestFocus
     */
    public static void requestFocus(Robot robot, Object obj, boolean onTitleBarIfWindow) 
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
    private static void requestFocusNEWT(com.jogamp.newt.Window win, boolean onTitleBarIfWindow) 
        throws AWTException, InterruptedException, InvocationTargetException {
        win.requestFocus();
        System.err.println("requestFocus: NEWT Component");
    }
    private static void requestFocusAWT(final java.awt.Component comp, boolean onTitleBarIfWindow) 
        throws AWTException, InterruptedException, InvocationTargetException {
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                comp.requestFocus();
                System.err.println("requestFocus: AWT Component");
            }});
    }
    
    public static void requestFocus(Robot robot, Object obj, int x, int y) 
        throws AWTException, InterruptedException, InvocationTargetException {
        validateAWTEDTIsAlive();
        
        final boolean idling = robot.isAutoWaitForIdle();
        final int mouseButton = java.awt.event.InputEvent.BUTTON1_MASK;
        robot.mouseMove( x, y );
        if( idling ) {
            robot.waitForIdle();
        } else {
            try { Thread.sleep(50); } catch (InterruptedException e) { }
        }
        robot.mousePress(mouseButton);
        robot.mouseRelease(mouseButton);
        final int d = getClickTimeout(obj) + 1;
        robot.delay( d );
    }
    
    public static boolean hasFocus(Object obj) {
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
    public static boolean waitForFocus(Object obj) throws InterruptedException {
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
    public static boolean waitForFocus(FocusEventCountAdapter gain, 
                                       FocusEventCountAdapter lost) throws InterruptedException {
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
    public static boolean waitForFocus(Object obj, FocusEventCountAdapter gain, 
                                                   FocusEventCountAdapter lost) throws InterruptedException {
        if(!waitForFocus(obj)) {
            return false;
        }
        return waitForFocus(gain, lost);
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
            Thread.dumpStack();            
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
    
    public static int keyType(int i, Robot robot, int keyCode,
                              Object obj, KeyEventCountAdapter counter) throws InterruptedException, AWTException, InvocationTargetException 
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
    public static int keyPress(int i, Robot robot, boolean press, int keyCode, int msDelay) {
        final long t0 = System.currentTimeMillis();        
        if(press) {
            awtRobotKeyPress(robot, keyCode, msDelay);
        } else {
            awtRobotKeyRelease(robot, keyCode, msDelay);
        }
        
        return (int) ( System.currentTimeMillis() - t0 ) ;
    }
    
    /** No validation is performed .. */ 
    public static int newtKeyPress(int i, Robot robot, boolean press, short newtKeyCode, int msDelay) {
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
    public static void assertKeyType(Robot robot, int keyCode, int typeCount, 
                                     Object obj, KeyEventCountAdapter counter) 
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
    public static void assertKeyPress(Robot robot, int keyCode, int typeCount, 
                                      Object obj, KeyEventCountAdapter counter) 
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
     * @return True if the Component becomes <code>visible</code> within TIME_OUT
     */
    public static boolean waitForVisible(Object obj, boolean visible) throws InterruptedException {
        int wait;
        if(obj instanceof com.jogamp.newt.Window) {
            com.jogamp.newt.Window win = (com.jogamp.newt.Window) obj;
            for (wait=0; wait<POLL_DIVIDER && visible != win.isVisible(); wait++) {
                Thread.sleep(TIME_SLICE);
            }
        } else if(NativeWindowFactory.isAWTAvailable() && obj instanceof java.awt.Component) {
            java.awt.Component comp = (java.awt.Component) obj;
            for (wait=0; wait<POLL_DIVIDER && visible != comp.isVisible(); wait++) {
                Thread.sleep(TIME_SLICE);
            }
        } else {
            throw new RuntimeException("Neither AWT nor NEWT: "+obj);
        }
        return wait<POLL_DIVIDER;
    }

    /**
     *
     * @return True if the GLDrawable recives the expected size within TIME_OUT
     */
    public static boolean waitForSize(GLDrawable drawable, int width, int height) throws InterruptedException {
        int wait;
        for (wait=0; wait<POLL_DIVIDER && ( width != drawable.getWidth() || height != drawable.getHeight() ) ; wait++) {
            Thread.sleep(TIME_SLICE);
        }
        return wait<POLL_DIVIDER;
    }

    /**
     *
     * @return True if the Component becomes realized (not displayable, native invalid) within TIME_OUT
     */
    public static boolean waitForRealized(Object obj, boolean realized) throws InterruptedException {
        int wait;
        if(obj instanceof com.jogamp.newt.Screen) {
            com.jogamp.newt.Screen screen = (com.jogamp.newt.Screen) obj;
            for (wait=0; wait<POLL_DIVIDER && realized != screen.isNativeValid(); wait++) {
                Thread.sleep(TIME_SLICE);
            }
        } else if(obj instanceof com.jogamp.newt.Window) {
            com.jogamp.newt.Window win = (com.jogamp.newt.Window) obj;
            for (wait=0; wait<POLL_DIVIDER && realized != win.isNativeValid(); wait++) {
                Thread.sleep(TIME_SLICE);
            }
        } else if (NativeWindowFactory.isAWTAvailable() && obj instanceof java.awt.Component) {
            java.awt.Component comp = (java.awt.Component) obj;
            for (wait=0; wait<POLL_DIVIDER && realized != comp.isDisplayable(); wait++) {
                Thread.sleep(TIME_SLICE);
            }
            // if GLCanvas, ensure it got also painted -> drawable.setRealized(true);
            if(wait<POLL_DIVIDER && comp instanceof GLAutoDrawable) {
                GLAutoDrawable glad = (GLAutoDrawable) comp;
                for (wait=0; wait<POLL_DIVIDER && realized != glad.isRealized(); wait++) {
                    Thread.sleep(TIME_SLICE);
                }
                if(wait>=POLL_DIVIDER) {
                    // for some reason GLCanvas hasn't been painted yet, force it!
                    System.err.println("XXX: FORCE REPAINT PRE - glad: "+glad);
                    comp.repaint();
                    for (wait=0; wait<POLL_DIVIDER && realized != glad.isRealized(); wait++) {
                        Thread.sleep(TIME_SLICE);
                    }
                    System.err.println("XXX: FORCE REPAINT POST - glad: "+glad);
                }
                for (wait=0; wait<POLL_DIVIDER && realized != glad.isRealized(); wait++) {
                    Thread.sleep(TIME_SLICE);
                }
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
     * @param wcl the WindowClosingListener to determine whether the AWT or NEWT widget has been closed. It should be attached 
     *            to the widget ASAP before any other listener, e.g. via {@link #addClosingListener(Object)}. 
     *            The WindowClosingListener will be reset before attempting to close the widget.   
     * @return True if the Window is closing and closed (if willClose is true), each within TIME_OUT
     * @throws InterruptedException
     */
    public static boolean closeWindow(Object obj, boolean willClose, WindowClosingListener closingListener) throws InterruptedException {
        closingListener.reset();
        if(obj instanceof java.awt.Window) {
            final java.awt.Window win = (java.awt.Window) obj;
            java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();
            final java.awt.EventQueue evtQ = tk.getSystemEventQueue();
            AWTEDTExecutor.singleton.invoke(true, new Runnable() {
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

    public static WindowClosingListener addClosingListener(Object obj) {
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
        public int getWindowClosingCount();
        public int getWindowClosedCount();
        public boolean isWindowClosing();
        public boolean isWindowClosed();
    }
    static class AWTWindowClosingAdapter
            extends java.awt.event.WindowAdapter implements WindowClosingListener
    {
        volatile int closing = 0;
        volatile int closed = 0;

        public void reset() {
            closing = 0;
            closed = 0;
        }
        public int getWindowClosingCount() {
            return closing;
        }
        public int getWindowClosedCount() {
            return closed;
        }
        public boolean isWindowClosing() {
            return 0 < closing;
        }
        public boolean isWindowClosed() {
            return 0 < closed;
        }
        public void windowClosing(java.awt.event.WindowEvent e) {
            closing++;
            System.err.println("AWTWindowClosingAdapter.windowClosing: "+this);
        }
        public void windowClosed(java.awt.event.WindowEvent e) {
            closed++;
            System.err.println("AWTWindowClosingAdapter.windowClosed: "+this);
        }
        public String toString() {
            return "AWTWindowClosingAdapter[closing "+closing+", closed "+closed+"]";
        }
    }
    static class NEWTWindowClosingAdapter
            extends com.jogamp.newt.event.WindowAdapter implements WindowClosingListener
    {
        volatile int closing = 0;
        volatile int closed = 0;

        public void reset() {
            closing = 0;
            closed = 0;
        }
        public int getWindowClosingCount() {
            return closing;
        }
        public int getWindowClosedCount() {
            return closed;
        }
        public boolean isWindowClosing() {
            return 0 < closing;
        }
        public boolean isWindowClosed() {
            return 0 < closed;
        }
        public void windowDestroyNotify(WindowEvent e) {
            closing++;
            System.err.println("NEWTWindowClosingAdapter.windowDestroyNotify: "+this);
        }
        public void windowDestroyed(WindowEvent e) {
            closed++;
            System.err.println("NEWTWindowClosingAdapter.windowDestroyed: "+this);
        }
        public String toString() {
            return "NEWTWindowClosingAdapter[closing "+closing+", closed "+closed+"]";
        }
    }

}


/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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

import java.util.concurrent.atomic.AtomicInteger;

import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.WindowEvent;

import jogamp.newt.WindowImplAccess;

public class NewtTestUtil extends TestUtil {
    public static class NEWTWindowClosingAdapter
            extends com.jogamp.newt.event.WindowAdapter implements TestUtil.WindowClosingListener
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
    /**
     *
     * @param waitAction if not null, Runnable shall wait {@link #TIME_SLICE} ms, if appropriate
     * @return True if the Window became the global focused Window within TIME_OUT
     */
    public static boolean waitForFocus(final Window win, final Runnable waitAction) throws InterruptedException {
        int wait;
        for (wait=0; wait<POLL_DIVIDER && !win.hasFocus(); wait++) {
            if( null != waitAction ) {
                waitAction.run();
            } else {
                Thread.sleep(TIME_SLICE);
            }
        }
        return wait<POLL_DIVIDER;
    }

    /**
     *
     * @param waitAction if not null, Runnable shall wait {@link #TIME_SLICE} ms, if appropriate
     * @return True if the Window became the global focused Window within TIME_OUT
     */
    public static boolean waitForFocus(final Window win, final FocusEventCountAdapter gain,
                                       final FocusEventCountAdapter lost, final Runnable waitAction) throws InterruptedException {
        if(!waitForFocus(win, waitAction)) {
            return false;
        }
        return TestUtil.waitForFocus(gain, lost, waitAction);
    }

    /**
     *
     * @param waitAction if not null, Runnable shall wait {@link #TIME_SLICE} ms, if appropriate
     * @return True if the Window receives the expected surface size within TIME_OUT
     */
    public static boolean waitForSize(final Window window, final int width, final int height, final Runnable waitAction) throws InterruptedException {
        int wait;
        for (wait=0; wait<POLL_DIVIDER && ( width != window.getSurfaceWidth() || height != window.getSurfaceHeight() ) ; wait++) {
            if( null != waitAction ) {
                waitAction.run();
            } else {
                Thread.sleep(TIME_SLICE);
            }
        }
        return wait<POLL_DIVIDER;
    }

    /**
     * @param waitAction if not null, Runnable shall wait {@link #TIME_SLICE} ms, if appropriate
     * @return True if the Component becomes <code>visible</code> within TIME_OUT
     */
    public static boolean waitForVisible(final Window win, final boolean visible, final Runnable waitAction) throws InterruptedException {
        int wait;
        for (wait=0; wait<POLL_DIVIDER && visible != win.isVisible(); wait++) {
            if( null != waitAction ) {
                waitAction.run();
            } else {
                Thread.sleep(TIME_SLICE);
            }
        }
        return wait<POLL_DIVIDER;
    }

    /**
     * @param screen the Screen to wait for
     * @param realized true if waiting for component to become realized, otherwise false
     * @param waitAction if not null, Runnable shall wait {@link #TIME_SLICE} ms, if appropriate
     * @return True if the Component becomes realized (not displayable, native invalid) within TIME_OUT
     * @throws InterruptedException
     */
    public static boolean waitForRealized(final Screen screen, final boolean realized, final Runnable waitAction) throws InterruptedException {
        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while( (t1-t0) < TIME_OUT && realized != screen.isNativeValid() ) {
            if( null != waitAction ) {
                waitAction.run();
            } else {
                Thread.sleep(TIME_SLICE);
            }
            t1 = System.currentTimeMillis();
        }
        return (t1-t0) < TIME_OUT;
    }
    /**
     * @param win the Window to wait for
     * @param realized true if waiting for component to become realized, otherwise false
     * @param waitAction if not null, Runnable shall wait {@link #TIME_SLICE} ms, if appropriate
     * @return True if the Component becomes realized (not displayable, native invalid) within TIME_OUT
     * @throws InterruptedException
     */
    public static boolean waitForRealized(final Window win, final boolean realized, final Runnable waitAction) throws InterruptedException {
        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while( (t1-t0) < TIME_OUT && realized != win.isNativeValid() ) {
            if( null != waitAction ) {
                waitAction.run();
            } else {
                Thread.sleep(TIME_SLICE);
            }
            t1 = System.currentTimeMillis();
        }
        return (t1-t0) < TIME_OUT;
    }

    public static TestUtil.WindowClosingListener addClosingListener(final Window win) {
        final NewtTestUtil.NEWTWindowClosingAdapter ncl = new NewtTestUtil.NEWTWindowClosingAdapter();
        win.addWindowListener(ncl);
        return ncl;
    }

    /**
     * Programmatically issue windowClosing on AWT or NEWT.
     * Wait until the window is closing within TIME_OUT.
     * @param willClose indicating that the window will close, hence this method waits for the window to be closed
     * @param waitAction if not null, Runnable shall wait {@link #TIME_SLICE} ms, if appropriate
     * @param obj either an AWT Window (Frame, JFrame) or NEWT Window
     * @param wcl the WindowClosingListener to determine whether the AWT or NEWT widget has been closed. It should be attached
     *            to the widget ASAP before any other listener, e.g. via {@link #addClosingListener(Object)}.
     *            The WindowClosingListener will be reset before attempting to close the widget.
     *
     * @return True if the Window is closing and closed (if willClose is true), each within TIME_OUT
     * @throws InterruptedException
     */
    public static boolean closeWindow(final Window win, final boolean willClose, final TestUtil.WindowClosingListener closingListener, final Runnable waitAction) throws InterruptedException {
        closingListener.reset();
        WindowImplAccess.windowDestroyNotify(win);
        return waitUntilClosed(willClose, closingListener, waitAction);
    }

    /**
     * Validates whether the window position is on the given position within tolerances.
     * <p>
     * Since WM may not obey our positional request exactly, we allow a tolerance of 2 times insets[left/top], or 64 pixels, whichever is greater.
     * </p>
     */
    public static boolean hasPositionMax2xInsetsOr64Pix(final Window win, final int shouldX, final int shouldY) {
        final int maxDX, maxDY;
        {
            final InsetsImmutable insets = win.getInsets();
            maxDX = Math.max(64, insets.getLeftWidth() * 2);
            maxDY = Math.max(64, insets.getTopHeight() * 2);
        }
        return hasPosition(win, shouldX, shouldY, maxDX, maxDY);
    }
    /**
     * Validates whether the window position is on the given position within the given tolerances.
     */
    public static boolean hasPosition(final Window win, final int shouldX, final int shouldY, final int maxDX, final int maxDY) {
        final boolean ok = Math.abs(shouldX - win.getX()) <= maxDX &&
                     Math.abs(shouldY - win.getY()) <= maxDY ;
        return ok;
    }
}




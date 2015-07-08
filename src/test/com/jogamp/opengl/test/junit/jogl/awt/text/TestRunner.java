/*
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
package com.jogamp.opengl.test.junit.jogl.awt.text;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;


/**
 * Utility for using windows in JUnit tests.
 *
 * <p><i>TestRunner</i> makes using windows in unit tests easy by performing
 * several tasks for the developer.
 *
 * <p>Most importantly, it allows the developer to open a window and have JUnit
 * wait for it to close.  Normally this is not a concern with a normal
 * <tt>main</tt> method because the JVM waits for the Swing thread to finish
 * before exiting.  However, the JUnit runner exits as soon it gets to the end
 * of the test methods.  Therefore windows in JUnit tests are closed
 * immediately, or never show up in the first place.
 *
 * <p><i>TestRunner</i> solves the problem by providing a static <i>run</i>
 * method that shows the frame and then calls
 * <i>wait</i> on it.  When a test launches the window in this manner, the JUnit
 * thread pauses.  Then when the frame is closed, instead of exiting, it
 * hides itself and calls {@link notifyAll}.  The paused JUnit thread therefore
 * restarts, disposes of the frame, and continues onto the next test.
 *
 * <p>Besides this important functionality, JTestFrame also
 * <ul>
 *    <li>allows the user to close the frame by pressing Escape,
 *    <li>can stop waiting after a certain amount of time, and
 *    <li>packs the frame if an explicit size has not been set.
 * </ul>
 */
class TestRunner {

    // Default amount of time to wait before closing window
    private static final long DEFAULT_WAIT_TIME = 1000;

    // Initial location on screen
    private static final int DEFAULT_LOCATION_X = 50;
    private static final int DEFAULT_LOCATION_Y = 50;

    // Key that closes the window
    private static final int CLOSE_KEY = KeyEvent.VK_ESCAPE;

    /**
     * Prevents instantiation.
     */
    private TestRunner() {
        ;
    }

    /**
     * Shows frame, waits for it to close, then disposes of it.
     *
     * @param frame Window to use
     * @throws NullPointerException if frame is <tt>null</tt>
     */
    public static void run(final JFrame frame) {
        run(frame, DEFAULT_WAIT_TIME);
    }

    /**
     * Shows frame, waits for it a certain amount of time, then disposes of it.
     *
     * @param frame Window to use
     * @param timeout Amount of milliseconds to wait (<= 0 for indefinitely)
     * @throws NullPointerException if frame is <tt>null</tt>
     */
    public static void run(final JFrame frame, final long timeout) {

        // Change behavior
        frame.setLocation(DEFAULT_LOCATION_X, DEFAULT_LOCATION_Y);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Add listeners
        frame.addKeyListener(new KeyObserver(frame));
        frame.addWindowListener(new WindowObserver(frame));

        // Show the frame
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!hasExplicitSize(frame)) {
                    frame.pack();
                }
                frame.setVisible(true);
            }
        });

        // Wait for it to close
        synchronized (frame) {
            try {
                if (timeout < 0) {
                    frame.wait();
                } else {
                    frame.wait(timeout);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting!");
            }
        }

        // Hide and dispose of it
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(false);
                frame.dispose();
            }
        });
    }

    //-----------------------------------------------------------------
    // Helpers
    //

    /**
     * Checks if a frame has an explicit size.
     *
     * @param frame Frame to check
     * @return <tt>true</tt> if an explicit size has been set
     * @throws NullPointerException if frame is <tt>null</tt>
     */
    private static boolean hasExplicitSize(final JFrame frame) {
        final int width = frame.getWidth();
        final int height = frame.getHeight();
        return (width > 0) || (height > 0);
    }

    /**
     * Wakes any threads waiting on object.
     *
     * @param object Object that other objects are waiting on
     * @throws NullPointerException if object is <tt>null</tt>
     */
    private static void wakeWaitingThreads(final Object object) {
        synchronized (object) {
            object.notifyAll();
        }
    }

    //-----------------------------------------------------------------
    // Nested classes
    //

    /**
     * Listener for key events.
     */
    private static class KeyObserver extends KeyAdapter {

        // Frame to listen to
        private final JFrame frame;

        /**
         * Constructs a key observer from a frame.
         *
         * @param frame Frame to listen to
         * @throws AssertionError if frame is <tt>null</tt>
         */
        public KeyObserver(final JFrame frame) {
            assert (frame != null);
            this.frame = frame;
        }

        /**
         * Wakes threads when {@link #CLOSE_KEY} key is released.
         *
         * @param event Event with key status
         * @throws NullPointerException if event is <tt>null</tt>
         */
        public void keyReleased(final KeyEvent event) {
            switch (event.getKeyCode()) {
            case CLOSE_KEY:
                wakeWaitingThreads(frame);
                break;
            }
        }
    }

    /**
     * Listener for window events.
     */
    private static class WindowObserver extends WindowAdapter {

        // Frame to listen to
        private final JFrame frame;

        /**
         * Constructs a window observer from a frame.
         *
         * @param frame Frame to listen to
         * @throws AssertionError if frame is <tt>null</tt>
         */
        public WindowObserver(final JFrame frame) {
            assert (frame != null);
            this.frame = frame;
        }

        /**
         * Wakes threads when window is closing.
         *
         * @param event Event with window status
         * @throws NullPointerException if event is <tt>null</tt>
         */
        @Override
        public void windowClosing(final WindowEvent event) {
            wakeWaitingThreads(frame);
        }
    }
}

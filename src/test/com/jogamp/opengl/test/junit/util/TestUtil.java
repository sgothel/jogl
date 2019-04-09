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

public abstract class TestUtil {
    public static interface WindowClosingListener {
        void reset();
        public int getWindowClosingCount();
        public int getWindowClosedCount();
        public boolean isWindowClosing();
        public boolean isWindowClosed();
    }
    public static final int RETRY_NUMBER  =   5;
    public static final int TIME_OUT     = 2000; // 2s
    public static final int POLL_DIVIDER   = 20; // TO/20
    public static final int TIME_SLICE   = TIME_OUT / POLL_DIVIDER ;

    /**
     *
     * @param waitAction if not null, Runnable shall wait {@link #TIME_SLICE} ms, if appropriate
     * @return True if the Window became the global focused Window within TIME_OUT
     */
    public static boolean waitForFocus(final FocusEventCountAdapter gain,
                                       final FocusEventCountAdapter lost, final Runnable waitAction) throws InterruptedException {
        int wait;
        for (wait=0; wait<POLL_DIVIDER; wait++) {
            if( ( null == lost || lost.focusLost() ) && ( null == gain || gain.focusGained() ) ) {
                return true;
            }
            if( null != waitAction ) {
                waitAction.run();
            } else {
                Thread.sleep(TIME_SLICE);
            }
        }
        return false;
    }

    /**
     * Wait until the window is closing within TIME_OUT.
     *
     * @param willClose indicating that the window will close, hence this method waits for the window to be closed
     * @param waitAction if not null, Runnable shall wait {@link #TIME_SLICE} ms, if appropriate
     * @param wcl the WindowClosingListener to determine whether the AWT or NEWT widget has been closed. It should be attached
     *            to the widget ASAP before any other listener, e.g. via {@link #addClosingListener(Object)}.
     *            The WindowClosingListener will be reset before attempting to close the widget.
     * @return True if the Window is closing and closed (if willClose is true), each within TIME_OUT
     * @throws InterruptedException
     */
    public static boolean waitUntilClosed(final boolean willClose, final TestUtil.WindowClosingListener closingListener, final Runnable waitAction) throws InterruptedException {
        int wait;
        for (wait=0; wait<POLL_DIVIDER && !closingListener.isWindowClosing(); wait++) {
            if( null != waitAction ) {
                waitAction.run();
            } else {
                Thread.sleep(TIME_SLICE);
            }
        }
        if(wait<POLL_DIVIDER && willClose) {
            for (wait=0; wait<POLL_DIVIDER && !closingListener.isWindowClosed(); wait++) {
                if( null != waitAction ) {
                    waitAction.run();
                } else {
                    Thread.sleep(TIME_SLICE);
                }
            }
        }
        return wait<POLL_DIVIDER;
    }
}




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

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;

public class GLTestUtil extends TestUtil {

    /**
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
     * @param glad the GLAutoDrawable to wait for
     * @param waitAction if not null, Runnable shall wait {@link #TIME_SLICE} ms, if appropriate
     * @param realized true if waiting for component to become realized, otherwise false
     * @return True if the Component becomes realized (not displayable, native invalid) within TIME_OUT
     * @throws InterruptedException
     */
    public static boolean waitForRealized(final GLAutoDrawable glad, final Runnable waitAction, final boolean realized) throws InterruptedException {
        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while( (t1-t0) < TIME_OUT && realized != glad.isRealized() ) {
            if( null != waitAction ) {
                waitAction.run();
            } else {
                Thread.sleep(TIME_SLICE);
            }
            t1 = System.currentTimeMillis();
        }
        return (t1-t0) < TIME_OUT;
    }

}




/**
 * Copyright 2020 JogAmp Community. All rights reserved.
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

import org.eclipse.swt.widgets.Display;

import com.jogamp.newt.util.EDTUtil;

public class SWTTestUtil {
    public static class WaitAction implements Runnable {
        final Display display;
        final boolean blocking;
        final long sleepMS;

        public WaitAction(final Display display, final boolean blocking, final long sleepMS) {
            this.display = display;
            this.blocking = blocking;
            this.sleepMS = sleepMS;
        }

        final Runnable waitAction0 = new Runnable() {
            @Override
            public void run() {
                if( !display.readAndDispatch() ) {
                    try {
                        Thread.sleep(sleepMS);
                    } catch (final InterruptedException e) { }
                }
            } };

        @Override
        public void run() {
            if( blocking ) {
                display.syncExec( waitAction0 );
            } else {
                display.asyncExec( waitAction0 );
            }
        };
    }

    public static class WaitAction2 implements Runnable {
        final EDTUtil edt;
        final Display display;
        final boolean blocking;
        final long sleepMS;

        public WaitAction2(final EDTUtil edt, final Display display, final boolean blocking, final long sleepMS) {
            this.edt = edt;
            this.display = display;
            this.blocking = blocking;
            this.sleepMS = sleepMS;
        }

        final Runnable waitAction0 = new Runnable() {
            @Override
            public void run() {
                if( !display.readAndDispatch() ) {
                    try {
                        Thread.sleep(sleepMS);
                    } catch (final InterruptedException e) { }
                }
            } };

        @Override
        public void run() {
            edt.invoke(blocking, waitAction0);
        };
    }
}




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

package jogamp.newt.awt;

import java.awt.EventQueue;
import javax.media.nativewindow.NativeWindowException;
import com.jogamp.newt.util.EDTUtil;
import jogamp.newt.Debug;

public class AWTEDTUtil implements EDTUtil {
    public static final boolean DEBUG = Debug.debug("EDT");

    private static AWTEDTUtil singletonMainThread = new AWTEDTUtil(); // one singleton MainThread

    public static AWTEDTUtil getSingleton() {
        return singletonMainThread;
    }

    AWTEDTUtil() {
        // package private access ..
    }

    final public void reset() {
        // nop
    }

    final public void start() {
        // nop
    }

    final public boolean isCurrentThreadEDT() {
        return EventQueue.isDispatchThread();
    }

    final public boolean isRunning() {
        return true; // AWT is always running
    }

    final public void invokeStop(Runnable r) {
        invokeImpl(true, r, true);
    }

    final public void invoke(boolean wait, Runnable r) {
        invokeImpl(wait, r, false);
    }

    /**
     * Public access to provide simple dispatching from other EDTUtil implementations
     * @param wait true if invokeLater
     * @param r the Runnable action
     * @param stop true if EDT shall stop (ignored with AWT)
     */
    final public void invokeImpl(boolean wait, Runnable r, boolean stop) {
        if(r == null) {
            return;
        }

        // handover to AWT MainThread ..
        try {
            if ( isCurrentThreadEDT() ) {
                r.run();
                return;
            }
            if(wait) {
                EventQueue.invokeAndWait(r);
            } else {
                EventQueue.invokeLater(r);
            }
        } catch (Exception e) {
            throw new NativeWindowException(e);
        }
    }

    final public void waitUntilIdle() {
    }

    final public void waitUntilStopped() {
    }

}



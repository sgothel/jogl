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

package jogamp.nativewindow;

import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.ToolkitLock;

/**
 * Implementing a singleton global NOP {@link javax.media.nativewindow.ToolkitLock}
 * without any locking. Since there is no locking it all, it is intrinsically recursive.
 */
public class NullToolkitLock implements ToolkitLock {
    /** Singleton via {@link NativeWindowFactoryImpl#getNullToolkitLock()} */
    protected NullToolkitLock() { }

    @Override
    public final void lock() {
        if(TRACE_LOCK) {
            System.err.println("NullToolkitLock.lock()");
            // Thread.dumpStack();
        }
    }

    @Override
    public final void unlock() {
        if(TRACE_LOCK) { System.err.println("NullToolkitLock.unlock()"); }
    }

    @Override
    public final void validateLocked() throws RuntimeException {
        if( NativeWindowFactory.requiresToolkitLock() ) {
            throw new RuntimeException("NullToolkitLock does not lock, but locking is required.");
        }
    }

    @Override
    public final void dispose() {
        // nop
    }

    @Override
    public String toString() {
        return "NullToolkitLock[]";
    }

}

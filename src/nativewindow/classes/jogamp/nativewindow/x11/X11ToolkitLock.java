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
package jogamp.nativewindow.x11;

import javax.media.nativewindow.ToolkitLock;

import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;

/**
 * Implementing a recursive {@link javax.media.nativewindow.ToolkitLock}
 * utilizing {@link X11Util#XLockDisplay(long)}.
 * <br>
 * This strategy should not be used in case XInitThreads() is being used,
 * or a higher level toolkit lock is required, ie AWT lock.
 */
public class X11ToolkitLock implements ToolkitLock {
    long displayHandle;
    RecursiveLock lock;

    public X11ToolkitLock(long displayHandle) {
        this.displayHandle = displayHandle;
        if(!X11Util.isNativeLockAvailable()) {
            lock = LockFactory.createRecursiveLock();
        }
    }

    public final void lock() {
        if(TRACE_LOCK) { System.err.println("X11ToolkitLock.lock() - native: "+(null==lock)); }
        if(null == lock) {
            X11Lib.XLockDisplay(displayHandle);
        } else {
            lock.lock();
        }
    }

    public final void unlock() {
        if(TRACE_LOCK) { System.err.println("X11ToolkitLock.unlock() - native: "+(null==lock)); }
        if(null == lock) {
            X11Lib.XUnlockDisplay(displayHandle);
        } else {
            lock.unlock();
        }
    }
}

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

package com.jogamp.nativewindow;

import jogamp.nativewindow.Debug;

/**
 * Marker for a singleton global recursive blocking lock implementation,
 * optionally locking a native windowing toolkit as well.
 * <p>
 * Toolkit locks are created solely via {@link NativeWindowFactory}.
 * </p>
 * <p>
 * One use case is the AWT locking on X11, see {@link NativeWindowFactory#getDefaultToolkitLock(String, long)}.
 * </p>
 */
public interface ToolkitLock {
    public static final boolean DEBUG = Debug.debug("ToolkitLock");
    public static final boolean TRACE_LOCK = Debug.isPropertyDefined("nativewindow.debug.ToolkitLock.TraceLock", true);

    /**
     * Blocking until the lock is acquired by this Thread or a timeout is reached.
     * <p>
     * Timeout is implementation specific, if used at all.
     * </p>
     *
     * @throws RuntimeException in case of a timeout
     */
    public void lock();

    /**
     * Release the lock.
     *
     * @throws RuntimeException in case the lock is not acquired by this thread.
     */
    public void unlock();

    /**
     * @throws RuntimeException if current thread does not hold the lock
     */
    public void validateLocked() throws RuntimeException;

    /**
     * Dispose this instance.
     * <p>
     * Shall be called when instance is no more required.
     * </p>
     * This allows implementations sharing a lock via resources
     * to decrease the reference counter.
     */
    public void dispose();
}

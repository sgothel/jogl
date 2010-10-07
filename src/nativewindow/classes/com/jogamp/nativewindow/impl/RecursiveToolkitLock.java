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

package com.jogamp.nativewindow.impl;

import javax.media.nativewindow.*;

//
// Reentrance locking toolkit
// 
public class RecursiveToolkitLock {
    static class SyncData {
        Thread owner = null;
        int recursionCount = 0;
        Exception lockedStack = null;
    }
    private SyncData sdata = new SyncData(); // synchronized (flow/mem)  mutable access

    private long timeout;
    private static final long defaultTimeout = 5000;  // default maximum wait 5s
    // private static final long defaultTimeout = 10000;  // default maximum wait 10s
    // private static final long defaultTimeout = 300000;  // default maximum wait 300s / 5min
    private static final boolean TRACE_LOCK = Debug.debug("TraceLock");

    public RecursiveToolkitLock() {
        this.timeout = defaultTimeout;
    }

    public RecursiveToolkitLock(long timeout) {
        this.timeout = timeout;
    }

    public final Exception getLockedStack() {
        synchronized(sdata) {
            return sdata.lockedStack;
        }
    }

    public final Thread getOwner() {
        synchronized(sdata) {
            return sdata.owner;
        }
    }

    public final boolean isOwner() {
        return isOwner(Thread.currentThread());
    }

    public final boolean isOwner(Thread thread) {
        synchronized(sdata) {
            return sdata.owner == thread ;
        }
    }

    public final boolean isLocked() {
        synchronized(sdata) {
            return null != sdata.owner;
        }
    }

    public final boolean isLockedByOtherThread() {
        synchronized(sdata) {
            return null != sdata.owner && Thread.currentThread() != sdata.owner ;
        }
    }

    public final int getRecursionCount() {
        synchronized(sdata) {
            return sdata.recursionCount;
        }
    }

    public final void validateLocked() {
        synchronized(sdata) {
            if ( null == sdata.owner ) {
                throw new RuntimeException(Thread.currentThread()+": Not locked");
            }
            if ( Thread.currentThread() != sdata.owner ) {
                getLockedStack().printStackTrace();
                throw new RuntimeException(Thread.currentThread()+": Not owner, owner is "+sdata.owner);
            }
        }
    }

    /** Recursive and blocking lockSurface() implementation */
    public final void lock() {
        synchronized(sdata) {
            Thread cur = Thread.currentThread();
            if(TRACE_LOCK) {
                System.out.println("... LOCK 0 ["+this+"], recursions "+sdata.recursionCount+", "+cur);
            }
            if (sdata.owner == cur) {
                ++sdata.recursionCount;
                if(TRACE_LOCK) {
                    System.out.println("+++ LOCK 1 ["+this+"], recursions "+sdata.recursionCount+", "+cur);
                }
                return;
            }

            long ts = System.currentTimeMillis();
            while (sdata.owner != null && (System.currentTimeMillis()-ts) < timeout) {
                try {
                    sdata.wait(timeout);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if(sdata.owner != null) {
                sdata.lockedStack.printStackTrace();
                throw new RuntimeException("Waited "+timeout+"ms for: "+sdata.owner+" - "+cur+", with recursionCount "+sdata.recursionCount+", lock: "+this);
            }
            if(TRACE_LOCK) {
                System.out.println("+++ LOCK X ["+this+"], recursions "+sdata.recursionCount+", "+cur);
            }
            sdata.owner = cur;
            sdata.lockedStack = new Exception("Previously locked by "+sdata.owner+", lock: "+this);
        }
    }
    

    /** Recursive and unblocking unlockSurface() implementation */
    public final void unlock() {
        unlock(null);
    }

    /** Recursive and unblocking unlockSurface() implementation */
    public final void unlock(Runnable taskAfterUnlockBeforeNotify) {
        synchronized(sdata) {
            validateLocked();

            if (sdata.recursionCount > 0) {
                --sdata.recursionCount;
                if(TRACE_LOCK) {
                    System.out.println("--- LOCK 1 ["+this+"], recursions "+sdata.recursionCount+", "+Thread.currentThread());
                }
                return;
            }
            sdata.owner = null;
            sdata.lockedStack = null;
            if(null!=taskAfterUnlockBeforeNotify) {
                taskAfterUnlockBeforeNotify.run();
            }
            if(TRACE_LOCK) {
                System.out.println("--- LOCK X ["+this+"], recursions "+sdata.recursionCount+", "+Thread.currentThread());
            }
            // Assuming notify() implementation weaks up the longest waiting thread, to avoid starvation. 
            // Otherwise we would need to have a Thread queue implemented, using sleep(timeout) and interrupt.
            sdata.notify();
        }
    }
}


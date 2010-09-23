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
    private Thread owner = null;
    private int recursionCount = 0;
    private Exception lockedStack = null;
    private static final long timeout = 3000;  // maximum wait 3s
    // private static final long timeout = 300000;  // maximum wait 300s
    private static final boolean TRACE_LOCK = Debug.debug("TraceLock");

    public Exception getLockedStack() {
        return lockedStack;
    }

    public Thread getOwner() {
        return owner;
    }

    public boolean isOwner() {
        return isOwner(Thread.currentThread());
    }

    public synchronized boolean isOwner(Thread thread) {
        return owner == thread ;
    }

    public synchronized boolean isLocked() {
        return null != owner;
    }

    public synchronized boolean isLockedByOtherThread() {
        return null != owner && Thread.currentThread() != owner ;
    }

    public synchronized int getRecursionCount() {
        return recursionCount;
    }

    public synchronized void validateLocked() {
        if ( !isLocked() ) {
            throw new RuntimeException(Thread.currentThread()+": Not locked");
        }
        if ( !isOwner() ) {
            getLockedStack().printStackTrace();
            throw new RuntimeException(Thread.currentThread()+": Not owner, owner is "+owner);
        }
    }

    /** Recursive and blocking lockSurface() implementation */
    public synchronized void lock() {
        Thread cur = Thread.currentThread();
        if(TRACE_LOCK) {
            System.out.println("... LOCK 0 ["+this+"], recursions "+recursionCount+", "+cur);
        }
        if (owner == cur) {
            ++recursionCount;
            if(TRACE_LOCK) {
                System.out.println("+++ LOCK 1 ["+this+"], recursions "+recursionCount+", "+cur);
            }
            return;
        }

        long ts = System.currentTimeMillis();
        while (owner != null && (System.currentTimeMillis()-ts) < timeout) {
            try {
                wait(timeout);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if(owner != null) {
            lockedStack.printStackTrace();
            throw new RuntimeException("Waited "+timeout+"ms for: "+owner+" - "+cur+", with recursionCount "+recursionCount+", lock: "+this);
        }
        if(TRACE_LOCK) {
            System.out.println("+++ LOCK X ["+this+"], recursions "+recursionCount+", "+cur);
        }
        owner = cur;
        lockedStack = new Exception("Previously locked by "+owner+", lock: "+this);
    }
    

    /** Recursive and unblocking unlockSurface() implementation */
    public synchronized void unlock() {
        unlock(null);
    }

    /** Recursive and unblocking unlockSurface() implementation */
    public synchronized void unlock(Runnable taskAfterUnlockBeforeNotify) {
        validateLocked();

        if (recursionCount > 0) {
            --recursionCount;
            if(TRACE_LOCK) {
                System.out.println("--- LOCK 1 ["+this+"], recursions "+recursionCount+", "+Thread.currentThread());
            }
            return;
        }
        owner = null;
        lockedStack = null;
        if(null!=taskAfterUnlockBeforeNotify) {
            taskAfterUnlockBeforeNotify.run();
        }
        if(TRACE_LOCK) {
            System.out.println("--- LOCK X ["+this+"], recursions "+recursionCount+", "+Thread.currentThread());
        }
        notifyAll();
    }
}


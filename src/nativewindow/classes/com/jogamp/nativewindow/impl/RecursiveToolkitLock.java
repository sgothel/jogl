package com.jogamp.nativewindow.impl;

import javax.media.nativewindow.*;

//
// Reentrance locking toolkit
// 
public class RecursiveToolkitLock {
    private Thread owner = null;
    private int recursionCount = 0;
    private Exception lockedStack = null;
    private static final long timeout = 300000;  // maximum wait 3s
    private static final boolean TRACE_LOCK = false;

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


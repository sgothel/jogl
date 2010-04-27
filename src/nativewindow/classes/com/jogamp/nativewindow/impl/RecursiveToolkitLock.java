package com.jogamp.nativewindow.impl;

import javax.media.nativewindow.*;

//
// Reentrance locking toolkit
// 
public class RecursiveToolkitLock implements ToolkitLock {
    private Thread owner;
    private int recursionCount;
    private Exception lockedStack = null;
    private static final long timeout = 3000;  // maximum wait 3s

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

    /** Recursive and blocking lockSurface() implementation */
    public synchronized void lock() {
        Thread cur = Thread.currentThread();
        if (owner == cur) {
            ++recursionCount;
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
            throw new RuntimeException("Waited "+timeout+"ms for: "+owner+" - "+cur);
        }
        owner = cur;
        lockedStack = new Exception("Previously locked by "+owner);
    }
    

    /** Recursive and unblocking unlockSurface() implementation */
    public synchronized void unlock() {
        unlock(null);
    }

    /** Recursive and unblocking unlockSurface() implementation */
    public synchronized void unlock(Runnable releaseAfterUnlockBeforeNotify) {
        Thread cur = Thread.currentThread();
        if (owner != cur) {
            lockedStack.printStackTrace();
            throw new RuntimeException(cur+": Not owner, owner is "+owner);
        }
        if (recursionCount > 0) {
            --recursionCount;
            return;
        }
        owner = null;
        lockedStack = null;
        if(null!=releaseAfterUnlockBeforeNotify) {
            releaseAfterUnlockBeforeNotify.run();
        }
        notifyAll();
    }
}


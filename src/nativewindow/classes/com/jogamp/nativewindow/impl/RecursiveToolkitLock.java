package com.jogamp.nativewindow.impl;

import javax.media.nativewindow.*;

//
// Reentrance locking toolkit
// 
public class RecursiveToolkitLock implements ToolkitLock {
    private Thread owner = null;
    private int recursionCount = 0;
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

    public synchronized int getRecursionCount() {
        return recursionCount;
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
            throw new RuntimeException("Waited "+timeout+"ms for: "+owner+" - "+cur+", with recursionCount "+recursionCount+", lock: "+this);
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
        if(null!=taskAfterUnlockBeforeNotify) {
            taskAfterUnlockBeforeNotify.run();
        }
        notifyAll();
    }
}


/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.jogamp.nativewindow.ToolkitLock;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.util.LongObjectHashMap;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;

/**
 * Implementing a shared resource based recursive {@link com.jogamp.nativewindow.ToolkitLock}.
 * <p>
 * A resource handle maybe used within many objects
 * and can be synchronized across threads via an unique instance of SharedResourceToolkitLock.
 * </p>
 * <p>
 * Implementation holds a synchronized map from handle to reference counted {@link SharedResourceToolkitLock}.
 * New elements are added via {@link #get(long)} if new
 * and removed via {@link #dispose()} if no more referenced.
 * </p>
 */
public class SharedResourceToolkitLock implements ToolkitLock {
    private static final LongObjectHashMap handle2Lock;
    static {
        handle2Lock = new LongObjectHashMap();
        handle2Lock.setKeyNotFoundValue(null);
    }

    /**
     * @return number of unclosed EGL Displays.<br>
     */
    public static int shutdown(final boolean verbose) {
        if(DEBUG || verbose || handle2Lock.size() > 0 ) {
            System.err.println("SharedResourceToolkitLock: Shutdown (open: "+handle2Lock.size()+")");
            if(DEBUG) {
                ExceptionUtils.dumpStack(System.err);
            }
            if( handle2Lock.size() > 0) {
                dumpOpenDisplayConnections();
            }
        }
        return handle2Lock.size();
    }

    public static void dumpOpenDisplayConnections() {
        System.err.println("SharedResourceToolkitLock: Open ResourceToolkitLock's: "+handle2Lock.size());
        int i=0;
        for(final Iterator<LongObjectHashMap.Entry> iter = handle2Lock.iterator(); iter.hasNext(); i++) {
            final LongObjectHashMap.Entry e = iter.next();
            System.err.println("SharedResourceToolkitLock: Open["+i+"]: "+e.value);
        }
    }

    public static final SharedResourceToolkitLock get(final long handle) {
        SharedResourceToolkitLock res;
        synchronized(handle2Lock) {
            res = (SharedResourceToolkitLock) handle2Lock.get(handle);
            if( null == res ) {
                res = new SharedResourceToolkitLock(handle);
                res.refCount.incrementAndGet();
                handle2Lock.put(handle, res);
                if(DEBUG || TRACE_LOCK) { System.err.println("SharedResourceToolkitLock.get() * NEW   *: "+res); }
            } else {
                res.refCount.incrementAndGet();
                if(DEBUG || TRACE_LOCK) { System.err.println("SharedResourceToolkitLock.get() * EXIST *: "+res); }
            }
        }
        return res;
    }

    private final RecursiveLock lock;
    private final long handle;
    private final AtomicInteger refCount;

    private SharedResourceToolkitLock(final long handle) {
        this.lock = LockFactory.createRecursiveLock();
        this.handle = handle;
        this.refCount = new AtomicInteger(0);
    }


    @Override
    public final void lock() {
        lock.lock();
        if(TRACE_LOCK) { System.err.println(Thread.currentThread()+" SharedResourceToolkitLock: lock() "+toStringImpl()); }
    }

    @Override
    public final void unlock() {
        if(TRACE_LOCK) { System.err.println(Thread.currentThread()+" SharedResourceToolkitLock: unlock() "+toStringImpl()); }
        lock.unlock();
    }

    @Override
    public final void validateLocked() throws RuntimeException {
        lock.validateLocked();
    }

    @Override
    public final void dispose() {
        if(0 < refCount.get()) { // volatile OK
            synchronized(handle2Lock) {
                if( 0 == refCount.decrementAndGet() ) {
                    if(DEBUG || TRACE_LOCK) { System.err.println("SharedResourceToolkitLock.dispose() * REMOV *: "+this); }
                    handle2Lock.remove(handle);
                } else {
                    if(DEBUG || TRACE_LOCK) { System.err.println("SharedResourceToolkitLock.dispose() * DOWN  *: "+this); }
                }
            }
        } else {
            if(DEBUG || TRACE_LOCK) { System.err.println("SharedResourceToolkitLock.dispose() * NULL  *: "+this); }
        }
    }

    @Override
    public String toString() {
        return "SharedResourceToolkitLock["+toStringImpl()+"]";
    }
    private String toStringImpl() {
        return "refCount "+refCount+", handle 0x"+Long.toHexString(handle)+", obj 0x"+Integer.toHexString(hashCode())+", isOwner "+lock.isOwner(Thread.currentThread())+", "+lock.toString();
    }
}

/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

package jogamp.opengl.util.av;

/** 
 * Simple synchronized ring buffer implementation.
 * <p>
 * Caller can chose whether to block until get / put is able to proceed or not.
 * </p>
 * <p>
 * Caller can chose whether to pass an empty array and clear references at get,
 * or using a preset array for circular access of same objects.
 * </p>
 * <p>
 * Circular write position is equal to the read position if buffer is full or if buffer is empty. 
 * </p>
 */
public class SyncedRingbuffer<T> {

    protected final Object sync = new Object();
    protected final T[] array;
    protected final int capacity;
    protected int readPos;
    protected int writePos;
    protected int size;
    
    public final String toString() {
        return "SyncedRingbuffer<?>[filled "+size+" / "+capacity+", writePos "+writePos+", readPos "+readPos+"]";
    }
    
    /** 
     * Create instance w/ the given array and it's capacity, e.g.:
     * <pre>
     *      SyncedRingbuffer r = new SyncedRingbuffer<Integer>(new Integer[10]);
     * </pre>
     * <p>
     * The array may either be clear, or preset w/ elements!
     * </p>
     * @param full if true, given array is assumed to be full, i.e. {@link #isFull()} will return true.
     * @param array
     */
    public SyncedRingbuffer(T[] array, boolean full) {
        this.array = array;
        this.capacity = array.length;
        clearImpl(false);
        if(full) {
            size = capacity;
        }
    }
    
    public final T[] getArray() { return array; }
    
    public final int capacity() {
        return capacity;
    }
    
    /**
     * Resets all ring buffer pointer to zero.
     * <p>
     * {@link #isEmpty()} will return <code>true</code> after calling this method.
     * </p>
     * <p>
     * If <code>clearRefs</code> is true, all ring buffer slots will be set to <code>null</code>.
     * </p>
     * @param clearRefs if true, all ring buffer slots will be flushed, otherwise they remain intact.
     */
    public final void clear(boolean clearRefs) {
        synchronized ( sync ) {
            clearImpl(clearRefs);
        }
    }
    
    private final void clearImpl(boolean clearRefs) {
        readPos = 0;
        writePos = 0;
        size = 0;
        if( clearRefs ) {
            for(int i=0; i<capacity; i++) {
                this.array[i] = null;
            }
        }
    }
    
    /** Returns the number of elements in this ring buffer. */
    public final int size() {
        synchronized ( sync ) {
            return size;
        }
    }
    
    /** Returns the number of free slots available to put.  */
    public final int getFreeSlots() {
        synchronized ( sync ) {
            return capacity - size;
        }
    }

    /** Returns true if this ring buffer is empty, otherwise false. */
    public final boolean isEmpty() {
        synchronized ( sync ) {
            return 0 == size;
        }
    }
    
    /** Returns true if this ring buffer is full, otherwise false. */
    public final boolean isFull() {
        synchronized ( sync ) {
            return capacity == size;
        }
    }
    
    /**
     * Returns the oldest put element if available, otherwise null.
     * <p>
     * Impl. returns the element at the current read position
     * and advances the read position - if available.
     * </p>
     * <p>
     * If <code>clearRef</code> is true, the returned ring buffer slot will be set to <code>null</code>.
     * </p>
     * <p>
     * Method is non blocking and returns immediately;.
     * </p>
     * @param clearRef if true, the returned ring buffer slot will be flushed, otherwise it remains intact.
     * @return the oldest put element if available, otherwise null.  
     */
    public final T get(boolean clearRef) {
        try {
            return getImpl(clearRef, false, false);
        } catch (InterruptedException ie) { throw new RuntimeException(ie); }
    }

    /**
     * Returns the oldest put element.
     * <p>
     * Impl. returns the element at the current read position
     * and advances the read position.
     * </p>
     * <p>
     * If <code>clearRef</code> is true, the returned ring buffer slot will be set to <code>null</code>.
     * </p>
     * <p>
     * Methods blocks until an element becomes available via put.
     * </p>
     * @param clearRef if true, the returned ring buffer slot will be flushed, otherwise it remains intact.
     * @return the oldest put element  
     * @throws InterruptedException 
     */
    public final T getBlocking(boolean clearRef) throws InterruptedException {
        return getImpl(clearRef, true, false);
    }
    
    public final T peek() {
        try {
            return getImpl(false, false, true);
        } catch (InterruptedException ie) { throw new RuntimeException(ie); }
    }
    public final T peekBlocking() throws InterruptedException {
        return getImpl(false, true, true);
    }
    
    private final T getImpl(boolean clearRef, boolean blocking, boolean peek) throws InterruptedException {
        synchronized ( sync ) {
            if( 0 == size ) {
                if( blocking ) {
                    while( 0 == size ) {
                        sync.wait();
                    }
                } else {
                    return null;
                }
            }
            final T r = array[readPos];
            if( !peek ) {
                if( clearRef ) {
                    array[readPos] = null;
                }
                readPos = (readPos + 1) % capacity;
                size--;
                sync.notifyAll(); // notify waiting putter
            }
            return r;
        }
    }
    
    /** 
     * Puts the element <code>e</code> at the current write position
     * and advances the write position.
     * <p>
     * Returns true if successful, otherwise false in case buffer is full.
     * </p>
     * <p>
     * Method is non blocking and returns immediately;.
     * </p>
     */
    public final boolean put(T e) {
        try {
            return putImpl(e, false, false);
        } catch (InterruptedException ie) { throw new RuntimeException(ie); }
    }
    
    /** 
     * Puts the element <code>e</code> at the current write position
     * and advances the write position.
     * <p>
     * Method blocks until a free slot becomes available via get.
     * </p>
     * @throws InterruptedException 
     */
    public final void putBlocking(T e) throws InterruptedException {
        if( !putImpl(e, false, true) ) {
            throw new InternalError("Blocking put failed: "+this);
        }
    }
    
    /** 
     * Keeps the element at the current write position intact
     * and advances the write position. 
     * <p>
     * Returns true if successful, otherwise false in case buffer is full.
     * </p>
     * <p>
     * If <code>blocking</code> is true, method blocks until a free slot becomes available via get.
     * </p>
     * @param blocking if true, wait until a free slot becomes available via get.
     * @throws InterruptedException 
     */
    public final boolean putSame(boolean blocking) throws InterruptedException {
        return putImpl(null, true, blocking);
    }
    
    private final boolean putImpl(T e, boolean sameRef, boolean blocking) throws InterruptedException {
        synchronized ( sync ) {
            if( capacity <= size ) {
                if( blocking ) {
                    while( capacity <= size ) {
                        sync.wait();
                    }
                } else { 
                    return false;
                }
            }
            if( !sameRef ) {
                array[ writePos ] = e;
            }
            writePos = (writePos + 1) % capacity;
            size++;
            sync.notifyAll(); // notify waiting getter
            return true;
        }
    }
    
    public final void waitForFreeSlots(int count) throws InterruptedException {
        synchronized ( sync ) {
            if( capacity - size < count ) {
                while( capacity - size < count ) {
                    System.err.println("XXXX AAA XXX");
                    sync.wait();
                }
            }
        }
    }
    
}

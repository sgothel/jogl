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

package com.jogamp.newt.util;

/**
 * EDT stands for Event Dispatch Thread.
 * <p>
 * EDTUtil comprises the functionality of:
 * <ul>
 *   <li> Periodically issuing an event dispatch command on the EDT.</li>
 *   <li> Ability to enqueue tasks, executed on the EDT.</li>
 *   <li> Controlling the EDT, ie start and stop in a sane manner.</li>
 * </ul>
 * The EDT pattern is a common tool to comply with todays windowing toolkits,
 * where the common denominator in regards to multithreading is to:
 * <ul>
 *   <li> Create a Window on one thread </li>
 *   <li> Modify the Window within the same thread </li>
 *   <li> Handle incoming events from within the same thread </li>
 * </ul>
 * Note: This is not true on MacOSX, where all these actions have to be
 * performed by a unique, so called main thread.<br>
 */
public interface EDTUtil {

    public static final long defaultEDTPollPeriod = 10; // 10ms, 1/100s

    /**
     * @return poll period in milliseconds
     */
    public long getPollPeriod();

    /**
     * @param ms poll period in milliseconds
     */
    public void setPollPeriod(long ms);
    
    /**
     * Create a new EDT. One should invoke <code>reset()</code><br>
     * after <code>invokeStop(..)</code> in case another <code>start()</code> or <code>invoke(..)</code>
     * is expected.
     *
     * @see #start()
     * @see #invoke(boolean, java.lang.Runnable)
     * @see #invokeStop(java.lang.Runnable)
     */
    public void reset();

    /**
     * Start the EDT
     */
    public void start();

    /**
     * @return True if the current thread is the EDT thread
     */
    public boolean isCurrentThreadEDT();

    /**
     * @return True if EDT is running
     */
    public boolean isRunning();

    /** 
     * Append the final task to the EDT task queue,
     * signals EDT to stop and wait until stopped.<br>
     * Due to the nature of this method:
     * <ul>
     *   <li>All previous queued tasks will be finished.</li>
     *   <li>No new tasks are allowed, an Exception is thrown.</li>
     *   <li>Can be issued from within EDT, ie from within an enqueued task.</li>
     *   <li>{@link #reset()} may follow immediately, ie creating a new EDT</li>
     * </ul>
     */
    public void invokeStop(Runnable finalTask);

    /** 
     * Append task to the EDT task queue.<br>
     * Wait until execution is finished if <code>wait == true</code>.<br>
     * Shall start the thread if not running.<br>
     * Can be issued from within EDT, ie from within an enqueued task.<br>
     *
     * @throws RuntimeException in case EDT is stopped and not {@link #reset()}
     */
    public void invoke(boolean wait, Runnable task);

    /** 
     * Wait until the EDT task queue is empty.<br>
     * The last task may still be in execution when this method returns.
     */
    public void waitUntilIdle();

    /**
     * Wait until EDT task is stopped.<br>
     * No <code>stop</code> action is performed, {@link #invokeStop(java.lang.Runnable)} should be used before.
     */
    public void waitUntilStopped();
}


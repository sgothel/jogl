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

import jogamp.newt.DisplayImpl;
import com.jogamp.newt.event.NEWTEvent;

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
     * Starts the EDT after it's creation or after {@link #invokeStop(boolean, Runnable) stopping}.
     * <p>
     * If the EDT is running, it must be {@link #invokeStop(boolean, Runnable) stopped} first
     * and the caller should wait {@link #waitUntilStopped() until it's stopped}.
     * </p>
     *
     * @throws IllegalStateException if EDT is running and not subject to be stopped, i.e. {@link #isRunning()} returns true
     * @throws RuntimeException if EDT could not be started
     *
     * @see #invokeStop(boolean, java.lang.Runnable)
     * @see #waitUntilStopped()
     */
    public void start() throws IllegalStateException;

    /**
     * Returns true if the current thread is the event dispatch thread (EDT).
     * <p>
     * The EDT is the platform specific thread dispatching toolkit-events
     * and executing toolkit-tasks enqueued via {@link #invoke(boolean, Runnable)}.
     * </p>
     * <p>
     * Usually it is the same thread as used to dequeue informal {@link NEWTEvent}s (NEDT), see {@link #isCurrentThreadNEDT()},
     * however, this may differ, e.g. SWT and AWT implementation.
     * </p>
     */
    public boolean isCurrentThreadEDT();

    /**
     * Returns true if the current thread is the internal NEWT event dequeue thread (NEDT).
     * <p>
     * The NEDT is the NEWT thread used to dequeue informal {@link NEWTEvent}s enqueued internally
     * via {@link DisplayImpl#enqueueEvent(boolean, NEWTEvent)}.
     * </p>
     * <p>
     * Usually it is the same thread as the EDT, see {@link #isCurrentThreadEDT()},
     * however, this may differ, e.g. SWT and AWT implementation.
     * </p>
     */
    public boolean isCurrentThreadNEDT();

    /**
     * Returns <code>true</code> if either {@link #isCurrentThreadEDT()} or {@link #isCurrentThreadNEDT()} is <code>true</code>,
     * otherwise <code>false</code>.
     */
    public boolean isCurrentThreadEDTorNEDT();

    /**
     * @return True if EDT is running and not subject to be stopped.
     */
    public boolean isRunning();

    /**
     * Append the final task to the EDT task queue,
     * signals EDT to stop.
     * <p>
     * If <code>wait</code> is <code>true</code> methods
     * blocks until EDT is stopped.
     * </p>
     * <p>
     * <code>task</code> maybe <code>null</code><br/>
     * Due to the nature of this method:
     * <ul>
     *   <li>All previous queued tasks will be finished.</li>
     *   <li>No new tasks are allowed, an Exception is thrown.</li>
     *   <li>Can be issued from within EDT, ie from within an enqueued task.</li>
     *   <li>{@link #start()} may follow immediately, ie creating a new EDT</li>
     * </ul>
     * </p>
     * @return true if <code>task</code> has been executed or queued for later execution, otherwise false
     */
    public boolean invokeStop(boolean wait, Runnable finalTask);

    /**
     * Appends task to the EDT task queue if current thread is not EDT,
     * otherwise execute task immediately.
     * <p>
     * Wait until execution is finished if <code>wait == true</code>.
     * </p>
     * Can be issued from within EDT, ie from within an enqueued task.<br>
     * @return true if <code>task</code> has been executed or queued for later execution, otherwise false
     */
    public boolean invoke(boolean wait, Runnable task);

    /**
     * Wait until the EDT task queue is empty.<br>
     * The last task may still be in execution when this method returns.
     * @return true if waited for idle, otherwise false, i.e. in case of current thread is EDT or NEDT
     */
    public boolean waitUntilIdle();

    /**
     * Wait until EDT task is stopped.<br>
     * No <code>stop</code> action is performed, {@link #invokeStop(boolean, java.lang.Runnable)} should be used before.
     * <p>
     * If caller thread is EDT or NEDT, this call will not block.
     * </p>
     * @return true if stopped, otherwise false, i.e. in case of current thread is EDT or NEDT
     */
    public boolean waitUntilStopped();
}


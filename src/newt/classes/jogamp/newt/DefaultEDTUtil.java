/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package jogamp.newt;

import java.util.ArrayList;

import com.jogamp.nativewindow.NativeWindowException;

import jogamp.common.util.locks.LockDebugUtil;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.InterruptedRuntimeException;
import com.jogamp.common.util.RunnableTask;
import com.jogamp.common.util.locks.Lock;
import com.jogamp.newt.util.EDTUtil;

public class DefaultEDTUtil implements EDTUtil {
    public static final boolean DEBUG = Debug.debug("EDT");

    /** Used to implement {@link #invokeStop(boolean, Runnable)}. */
    private static final Object TASK_ATTACHMENT_STOP = new Object();
    /** Used to provoke an exception on the EDT while waiting / blocking. Merely exists to test code.*/
    private static final Object TASK_ATTACHMENT_TEST_ERROR = new Object();

    private final Object edtLock = new Object(); // locking the EDT start/stop state
    private /* final */ ThreadGroup threadGroup;
    private final String name;
    private final Runnable dispatchMessages;
    private NEDT edt = null;
    private int start_iter=0;
    private static long pollPeriod = EDTUtil.defaultEDTPollPeriod;

    public DefaultEDTUtil(final ThreadGroup tg, final String name, final Runnable dispatchMessages) {
        this.threadGroup = tg;
        this.name=Thread.currentThread().getName()+"-"+name+"-EDT-";
        this.dispatchMessages=dispatchMessages;
        this.edt = new NEDT(threadGroup, this.name);
        this.edt.setDaemon(true); // don't stop JVM from shutdown ..
    }

    @Override
    final public long getPollPeriod() {
        return pollPeriod;
    }

    @Override
    final public void setPollPeriod(final long ms) {
        pollPeriod = ms; // writing to static field is intended
    }

    @Override
    public final void start() throws IllegalStateException {
        synchronized(edtLock) {
            if( edt.isRunning() ) {
                throw new IllegalStateException("EDT still running and not subject to stop. Curr "+Thread.currentThread().getName()+", EDT "+edt.getName()+", isRunning "+edt.isRunning+", shouldStop "+edt.shouldStop);
            }
            if(DEBUG) {
                if(edt.tasks.size()>0) {
                    System.err.println(Thread.currentThread()+": Default-EDT reset, remaining tasks: "+edt.tasks.size()+" - "+edt);
                }
                System.err.println(Thread.currentThread()+": Default-EDT reset - edt: "+edt);
            }
            if( edt.getState() != Thread.State.NEW ) {
                if( null != threadGroup && threadGroup.isDestroyed() ) {
                    // best thing we can do is to use this thread's TG
                    threadGroup = Thread.currentThread().getThreadGroup();
                }
                edt = new NEDT(threadGroup, name);
                edt.setDaemon(true); // don't stop JVM from shutdown ..
            }
            startImpl();
        }
        if( !edt.isRunning() ) {
            throw new RuntimeException("EDT could not be started: "+edt);
        }
    }

    private final void startImpl() {
        if(edt.isAlive()) {
            throw new RuntimeException("Default-EDT Thread.isAlive(): true, isRunning: "+edt.isRunning+", shouldStop "+edt.shouldStop+", edt: "+edt+", tasks: "+edt.tasks.size());
        }
        start_iter++;
        edt.setName(name+start_iter);
        if(DEBUG) {
            System.err.println(Thread.currentThread()+": Default-EDT START - edt: "+edt);
        }
        edt.start();
    }

    @Override
    public final boolean isCurrentThreadEDT() {
        return edt == Thread.currentThread(); // EDT == NEDT
    }

    @Override
    public final boolean isCurrentThreadNEDT() {
        return edt == Thread.currentThread(); // EDT == NEDT
    }

    @Override
    public final boolean isCurrentThreadEDTorNEDT() {
        return edt == Thread.currentThread(); // EDT == NEDT
    }

    @Override
    public final boolean isRunning() {
        return edt.isRunning() ;
    }

    @Override
    public final boolean invokeStop(final boolean wait, final Runnable task) {
        if(DEBUG) {
            System.err.println(Thread.currentThread()+": Default-EDT.invokeStop wait "+wait);
            ExceptionUtils.dumpStack(System.err);
        }
        return invokeImpl(wait, task, true /* stop */, false /* provokeError */);
    }

    public final boolean invokeAndWaitError(final Runnable task) {
        if(DEBUG) {
            System.err.println(Thread.currentThread()+": Default-EDT.invokeAndWaitError");
            ExceptionUtils.dumpStack(System.err);
        }
        return invokeImpl(true /* wait */, task, false /* stop */, true /* provokeError */);
    }

    @Override
    public final boolean invoke(final boolean wait, final Runnable task) {
        return invokeImpl(wait, task, false /* stop */, false /* provokeError */);
    }

    private static Runnable nullTask = new Runnable() {
        @Override
        public void run() { }
    };

    private final boolean invokeImpl(boolean wait, Runnable task, final boolean stop, final boolean provokeError) {
        final RunnableTask rTask;
        final Object rTaskLock = new Object();
        synchronized(rTaskLock) { // lock the optional task execution
            synchronized(edtLock) { // lock the EDT status
                if( edt.shouldStop ) {
                    // drop task ..
                    System.err.println(Thread.currentThread()+": Warning: Default-EDT about (1) to stop, won't enqueue new task: "+edt);
                    if(DEBUG) {
                        ExceptionUtils.dumpStack(System.err);
                    }
                    return false;
                }
                if( isCurrentThreadEDT() ) {
                    if(null != task) {
                        task.run();
                    }
                    wait = false; // running in same thread (EDT) -> no wait
                    rTask = null;
                    if( stop ) {
                        edt.shouldStop = true;
                        if( edt.tasks.size()>0 ) {
                            System.err.println(Thread.currentThread()+": Warning: Default-EDT about (2) to stop, task executed. Remaining tasks: "+edt.tasks.size()+" - "+edt);
                            if(DEBUG) {
                                ExceptionUtils.dumpStack(System.err);
                            }
                        }
                    }
                } else {
                    if( !edt.isRunning ) {
                        if( null != task ) {
                            if( stop ) {
                                System.err.println(Thread.currentThread()+": Warning: Default-EDT is about (3) to stop and stopped already, dropping task. Remaining tasks: "+edt.tasks.size()+" - "+edt);
                            } else {
                                System.err.println(Thread.currentThread()+": Warning: Default-EDT is not running, dropping task. NEDT "+edt);
                            }
                            if(DEBUG) {
                                ExceptionUtils.dumpStack(System.err);
                            }
                        }
                        return false;
                    } else if( stop && null == task ) {
                        task = nullTask; // ensures execution triggering stop
                    }

                    if(null != task) {
                        synchronized(edt.tasks) {
                            rTask = new RunnableTask(task,
                                                     wait ? rTaskLock : null,
                                                     true /* always catch and report Exceptions, don't disturb EDT */,
                                                     wait ? null : System.err);
                            if(stop) {
                                rTask.setAttachment(TASK_ATTACHMENT_STOP); // mark final task, will imply shouldStop:=true
                            } else if(provokeError) {
                                rTask.setAttachment(TASK_ATTACHMENT_TEST_ERROR);
                            }
                            // append task ..
                            edt.tasks.add(rTask);
                            edt.tasks.notifyAll();
                        }
                    } else {
                        wait = false;
                        rTask = null;
                    }
                }
            }
            if( wait ) {
                try {
                    while( rTask.isInQueue() ) {
                        rTaskLock.wait(); // free lock, allow execution of rTask
                    }
                } catch (final InterruptedException ie) {
                    throw new InterruptedRuntimeException(ie);
                }
                final Throwable throwable = rTask.getThrowable();
                if(null!=throwable) {
                    if(throwable instanceof NativeWindowException) {
                        throw (NativeWindowException)throwable;
                    }
                    throw new RuntimeException(throwable);
                }
            }
            if(DEBUG) {
                if( stop) {
                    System.err.println(Thread.currentThread()+": Default-EDT signal STOP X edt: "+edt);
                }
            }
            return true;
        }
    }

    @Override
    final public boolean waitUntilIdle() {
        final NEDT _edt;
        synchronized(edtLock) {
            _edt = edt;
        }
        if(!_edt.isRunning || _edt == Thread.currentThread()) {
            return false;
        }
        synchronized(_edt.tasks) {
            try {
                while(_edt.isRunning && _edt.tasks.size()>0) {
                    _edt.tasks.notifyAll();
                    _edt.tasks.wait();
                }
            } catch (final InterruptedException e) {
                throw new InterruptedRuntimeException(e);
            }
            return true;
        }
    }

    @Override
    final public boolean waitUntilStopped() {
        synchronized(edtLock) {
            if(edt.isRunning && edt != Thread.currentThread() ) {
                try {
                    while( edt.isRunning ) {
                        edtLock.wait();
                    }
                } catch (final InterruptedException e) {
                    throw new InterruptedRuntimeException(e);
                }
                return true;
            } else {
                return false;
            }
        }
    }

    class NEDT extends InterruptSource.Thread {
        volatile boolean shouldStop = false;
        volatile boolean isRunning = false;
        final ArrayList<RunnableTask> tasks = new ArrayList<RunnableTask>(); // one shot tasks

        public NEDT(final ThreadGroup tg, final String name) {
            super(tg, null, name);
        }

        final public boolean isRunning() {
            return isRunning && !shouldStop;
        }

        @Override
        final public void start() throws IllegalThreadStateException {
            isRunning = true;
            super.start();
        }

        private final void validateNoRecursiveLocksHold() {
            if(LockDebugUtil.getRecursiveLockTrace().size()>0) {
                LockDebugUtil.dumpRecursiveLockTrace(System.err);
                throw new InternalError("XXX");
            }
        }

        /**
         * Utilizing locking only on tasks and its execution,
         * not for event dispatching.
         */
        @Override
        final public void run() {
            if(DEBUG) {
                System.err.println(getName()+": Default-EDT run() START "+ getName());
            }
            if(Lock.DEBUG) {
                validateNoRecursiveLocksHold();
            }
            RuntimeException error = null;
            try {
                do {
                    // event dispatch
                    if(!shouldStop) {
                        dispatchMessages.run();
                    }
                    // wait and work on tasks
                    RunnableTask task = null;
                    synchronized(tasks) {
                        // wait for tasks
                        if( !shouldStop && tasks.size()==0 ) {
                            try {
                                tasks.wait(pollPeriod);
                            } catch (final InterruptedException e) {
                                throw new InterruptedRuntimeException(e);
                            }
                        }
                        // execute one task, if available
                        if(tasks.size()>0) {
                            task = tasks.remove(0);
                            tasks.notifyAll();
                            final Object attachment = task.getAttachment();
                            if( TASK_ATTACHMENT_STOP == attachment ) {
                                shouldStop = true;
                            } else if( TASK_ATTACHMENT_TEST_ERROR == attachment ) {
                                tasks.add(0, task);
                                task = null;
                                throw new RuntimeException("TASK_ATTACHMENT_TEST_ERROR");
                            }
                        }
                    }
                    if(null!=task) {
                        task.run();
                        if(Lock.DEBUG) {
                            validateNoRecursiveLocksHold();
                        }
                        if(!task.hasWaiter() && null != task.getThrowable()) {
                            // at least dump stack-trace in case nobody waits for result
                            System.err.println("DefaultEDT.run(): Caught exception occured on thread "+java.lang.Thread.currentThread().getName()+": "+task.toString());
                            task.getThrowable().printStackTrace();
                        }
                    }
                } while(!shouldStop) ;
            } catch (final Throwable t) {
                // handle errors ..
                shouldStop = true;
                if(t instanceof RuntimeException) {
                    error = (RuntimeException) t;
                } else {
                    error = new RuntimeException("Within Default-EDT", t);
                }
            } finally {
                final String msg = getName()+": Default-EDT finished w/ "+tasks.size()+" left";
                if(DEBUG) {
                    System.err.println(msg+", "+error);
                }
                synchronized(edtLock) {
                    int i = 0;
                    while( tasks.size() > 0 ) {
                        // notify all waiter
                        final String msg2 = msg+", task #"+i;
                        final Throwable t = null != error ? new Throwable(msg2, error) : new Throwable(msg2);
                        final RunnableTask rt = tasks.remove(0);
                        if( null != rt ) {
                            rt.flush(t);
                            i++;
                        }
                    }
                    isRunning = false;
                    edtLock.notifyAll();
                }
                if(DEBUG) {
                    System.err.println(msg+" EXIT, exception: "+error);
                }
                if(null!=error) {
                    throw error;
                }
            } // finally
        } // run()
    } // EventDispatchThread
}


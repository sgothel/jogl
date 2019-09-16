/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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

package jogamp.newt.driver.macosx;

import com.jogamp.nativewindow.NativeWindowException;

import jogamp.nativewindow.macosx.OSXUtil;
import jogamp.newt.Debug;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.InterruptedRuntimeException;
import com.jogamp.common.util.RunnableTask;
import com.jogamp.newt.util.EDTUtil;

public class AppKitEDTUtil implements EDTUtil {
    public static final boolean DEBUG = Debug.debug("EDT");

    private final Object edtLock = new Object(); // locking the EDT start/stop state
    private /* final */ ThreadGroup threadGroup;
    private final String name;
    private final Runnable dispatchMessages;
    private NEDT nedt = null;
    private int start_iter=0;
    private static long pollPeriod = EDTUtil.defaultEDTPollPeriod;

    public AppKitEDTUtil(final ThreadGroup tg, final String name, final Runnable dispatchMessages) {
        this.threadGroup = tg;
        this.name=Thread.currentThread().getName()+"-"+name+"-EDT-";
        this.dispatchMessages=dispatchMessages;
        this.nedt = new NEDT(threadGroup, this.name);
        this.nedt.setDaemon(true); // don't stop JVM from shutdown ..
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
            if( nedt.isRunning() ) {
                throw new IllegalStateException("EDT still running and not subject to stop. Curr "+Thread.currentThread().getName()+", EDT "+nedt.getName()+", isRunning "+nedt.isRunning+", shouldStop "+nedt.shouldStop);
            }
            if(DEBUG) {
                System.err.println(Thread.currentThread()+": AppKit-EDT reset - edt: "+nedt);
            }
            if( nedt.getState() != Thread.State.NEW ) {
                if( null != threadGroup && threadGroup.isDestroyed() ) {
                    // best thing we can do is to use this thread's TG
                    threadGroup = Thread.currentThread().getThreadGroup();
                }
                nedt = new NEDT(threadGroup, name);
                nedt.setDaemon(true); // don't stop JVM from shutdown ..
            }
            startImpl();
        }
        if( !nedt.isRunning() ) {
            throw new RuntimeException("EDT could not be started: "+nedt);
        }
    }

    private final void startImpl() {
        if(nedt.isAlive()) {
            throw new RuntimeException("AppKit-EDT Thread.isAlive(): true, isRunning: "+nedt.isRunning+", shouldStop "+nedt.shouldStop+", edt: "+nedt);
        }
        start_iter++;
        nedt.setName(name+start_iter);
        if(DEBUG) {
            System.err.println(Thread.currentThread()+": AppKit-EDT START - edt: "+nedt);
        }
        nedt.start();
    }

    @Override
    public final boolean isCurrentThreadEDT() {
        return OSXUtil.IsMainThread();
    }

    @Override
    public final boolean isCurrentThreadNEDT() {
        return nedt == Thread.currentThread();
    }

    @Override
    public final boolean isCurrentThreadEDTorNEDT() {
        return OSXUtil.IsMainThread() || nedt == Thread.currentThread();
    }

    @Override
    public final boolean isRunning() {
        return nedt.isRunning() ;
    }

    @Override
    public final boolean invokeStop(final boolean wait, final Runnable task) {
        if(DEBUG) {
            System.err.println(Thread.currentThread()+": AppKit-EDT.invokeStop wait "+wait);
            ExceptionUtils.dumpStack(System.err);
        }
        return invokeImpl(wait, task, true /* stop */, false /* provokeError */);
    }

    public final boolean invokeAndWaitError(final Runnable task) {
        if(DEBUG) {
            System.err.println(Thread.currentThread()+": AppKit-EDT.invokeAndWaitError");
            ExceptionUtils.dumpStack(System.err);
        }
        return invokeImpl(true /* wait */, task, false /* stop */, true /* provokeError */);
    }

    @Override
    public final boolean invoke(final boolean wait, final Runnable task) {
        return invokeImpl(wait, task, false /* stop */, false /* provokeError */);
    }

    private final boolean invokeImpl(boolean wait, final Runnable task, final boolean stop, final boolean provokeError) {
        final RunnableTask rTask;
        final Object rTaskLock = new Object();
        synchronized(rTaskLock) { // lock the optional task execution
            synchronized(edtLock) { // lock the EDT status
                if( nedt.shouldStop ) {
                    // drop task ..
                    System.err.println(Thread.currentThread()+": Warning: AppKit-EDT about (1) to stop, won't enqueue new task: "+nedt);
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
                        nedt.shouldStop = true;
                    }
                } else {
                    if( !nedt.isRunning ) {
                        if( null != task ) {
                            if( stop ) {
                                System.err.println(Thread.currentThread()+": Warning: AppKit-EDT is about (3) to stop and stopped already, dropping task - "+nedt);
                            } else {
                                System.err.println(Thread.currentThread()+": Warning: AppKit-EDT is not running, dropping task. NEDT "+nedt);
                            }
                            if(DEBUG) {
                                ExceptionUtils.dumpStack(System.err);
                            }
                        }
                        return false;
                    } else if( stop ) {
                        if( nedt.isRunning ) {
                            if(DEBUG) {
                                System.err.println(Thread.currentThread()+": AppKit-EDT signal STOP (on edt: "+isCurrentThreadEDT()+") - "+nedt+", isRunning "+nedt.isRunning+", shouldStop "+nedt.shouldStop);
                            }
                            synchronized(nedt.sync) {
                                nedt.shouldStop = true;
                                nedt.sync.notifyAll(); // stop immediate if waiting (poll freq)
                            }
                        }
                    }

                    if(null != task) {
                        rTask = new RunnableTask(task,
                                wait ? rTaskLock : null,
                                        true /* always catch and report Exceptions, don't disturb EDT */,
                                        wait ? null : System.err);
                        OSXUtil.RunOnMainThread(false, false, rTask);
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
                    System.err.println(Thread.currentThread()+": AppKit-EDT signal STOP X edt: "+nedt);
                }
            }
            return true;
        }
    }

    @Override
    final public boolean waitUntilIdle() {
        final NEDT _nedt;
        synchronized(edtLock) {
            _nedt = nedt;
        }
        if( !_nedt.isRunning || _nedt == Thread.currentThread() || OSXUtil.IsMainThread() ) {
            return false;
        }
        OSXUtil.WaitUntilFinish();
        return true;
    }

    @Override
    final public boolean waitUntilStopped() {
        synchronized(edtLock) {
            final Thread curT = Thread.currentThread();
            final boolean onApkKitEDT = OSXUtil.IsMainThread();
            if( nedt.isRunning && nedt != curT && !onApkKitEDT ) {
                try {
                    while( nedt.isRunning ) {
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
        Object sync = new Object();

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

        /**
         * Utilizing locking only on tasks and its execution,
         * not for event dispatching.
         */
        @Override
        final public void run() {
            if(DEBUG) {
                System.err.println(getName()+": AppKit-EDT run() START "+ getName());
            }
            RuntimeException error = null;
            try {
                do {
                    // event dispatch
                    if(!shouldStop) {
                        dispatchMessages.run();
                    }
                    // wait
                    synchronized(sync) {
                        if(!shouldStop) {
                            try {
                                sync.wait(pollPeriod);
                            } catch (final InterruptedException e) {
                                throw new InterruptedRuntimeException(e);
                            }
                        }
                    }
                } while(!shouldStop) ;
            } catch (final Throwable t) {
                // handle errors ..
                shouldStop = true;
                if(t instanceof RuntimeException) {
                    error = (RuntimeException) t;
                } else {
                    error = new RuntimeException("Within AppKit-EDT", t);
                }
            } finally {
                if(DEBUG) {
                    System.err.println(getName()+": AppKit-EDT run() END "+ getName()+", "+error);
                }
                synchronized(edtLock) {
                    isRunning = false;
                    edtLock.notifyAll();
                }
                if(DEBUG) {
                    System.err.println(getName()+": AppKit-EDT run() EXIT "+ getName()+", exception: "+error);
                }
                if(null!=error) {
                    throw error;
                }
            } // finally
        } // run()
    } // EventDispatchThread
}


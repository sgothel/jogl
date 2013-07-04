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
import javax.media.nativewindow.NativeWindowException;

import jogamp.common.util.locks.LockDebugUtil;

import com.jogamp.common.util.RunnableTask;
import com.jogamp.common.util.locks.Lock;
import com.jogamp.newt.util.EDTUtil;

public class DefaultEDTUtil implements EDTUtil {
    public static final boolean DEBUG = Debug.debug("EDT");

    private final Object edtLock = new Object(); // locking the EDT start/stop state
    private final ThreadGroup threadGroup; 
    private final String name;
    private final Runnable dispatchMessages;
    private EventDispatchThread edt = null;
    private int start_iter=0;
    private static long pollPeriod = EDTUtil.defaultEDTPollPeriod;

    public DefaultEDTUtil(ThreadGroup tg, String name, Runnable dispatchMessages) {
        this.threadGroup = tg;
        this.name=Thread.currentThread().getName()+"-"+name+"-EDT-";
        this.dispatchMessages=dispatchMessages;
        this.edt = new EventDispatchThread(threadGroup, name);
        this.edt.setDaemon(true); // don't stop JVM from shutdown ..
    }

    @Override
    final public long getPollPeriod() {
        return pollPeriod;
    }

    @Override
    final public void setPollPeriod(long ms) {
        pollPeriod = ms;
    }
    
    @Override
    public final void reset() {
        synchronized(edtLock) { 
            waitUntilStopped();
            if(DEBUG) {
                if(edt.tasks.size()>0) {
                    System.err.println(Thread.currentThread()+": Default-EDT reset, remaining tasks: "+edt.tasks.size()+" - "+edt);
                    // Thread.dumpStack();
                }
                System.err.println(Thread.currentThread()+": Default-EDT reset - edt: "+edt);
            }
            this.edt = new EventDispatchThread(threadGroup, name);
            this.edt.setDaemon(true); // don't stop JVM from shutdown ..
        }
    }

    private final void startImpl() {
        if(edt.isAlive()) {
            throw new RuntimeException("Default-EDT Thread.isAlive(): true, isRunning: "+edt.isRunning()+", edt: "+edt+", tasks: "+edt.tasks.size());
        }
        start_iter++;
        edt.setName(name+start_iter);
        edt.shouldStop = false;
        if(DEBUG) {
            System.err.println(Thread.currentThread()+": Default-EDT START - edt: "+edt);
            // Thread.dumpStack();
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
    public final void invokeStop(boolean wait, Runnable task) {
        invokeImpl(wait, task, true);
    }

    @Override
    public final void invoke(boolean wait, Runnable task) {
        invokeImpl(wait, task, false);
    }

    private static Runnable nullTask = new Runnable() {
        @Override
        public void run() { }        
    };
    
    private void invokeImpl(boolean wait, Runnable task, boolean stop) {
        Throwable throwable = null;
        RunnableTask rTask = null;
        Object rTaskLock = new Object();
        synchronized(rTaskLock) { // lock the optional task execution
            synchronized(edtLock) { // lock the EDT status
                if( edt.shouldStop ) {
                    // drop task ..
                    if(DEBUG) {
                        System.err.println(Thread.currentThread()+": Warning: Default-EDT about (1) to stop, won't enqueue new task: "+edt);
                        Thread.dumpStack();
                    }
                    return;
                }
                // System.err.println(Thread.currentThread()+" XXX stop: "+stop+", tasks: "+edt.tasks.size()+", task: "+task);
                // Thread.dumpStack();
                if( isCurrentThreadEDT() ) {
                    if(null != task) {
                        task.run();
                    }
                    wait = false; // running in same thread (EDT) -> no wait
                    if(stop) {
                        edt.shouldStop = true;
                        if( edt.tasks.size()>0 ) {
                            System.err.println(Thread.currentThread()+": Warning: Default-EDT about (2) to stop, task executed. Remaining tasks: "+edt.tasks.size()+" - "+edt);
                            if(DEBUG) {
                                Thread.dumpStack();
                            }
                        }
                    }
                } else {
                    if( !edt.isRunning() ) {
                        if( !stop ) {
                            startImpl();
                            if( wait && null == task ) {
                                task = nullTask; // ensures wait until started 
                            }
                        } else {
                            // drop task and don't wait
                            task = null;
                            System.err.println(Thread.currentThread()+": Warning: Default-EDT is about (3) to stop and stopped already, dropping task. Remaining tasks: "+edt.tasks.size()+" - "+edt);
                            if(DEBUG) {
                                Thread.dumpStack();
                            }
                        }
                    } else if(stop && null == task) {
                        task = nullTask; // ensures execution triggering stop
                    }

                    if(null != task) {
                        synchronized(edt.tasks) {
                            rTask = new RunnableTask(task,
                                                     wait ? rTaskLock : null,
                                                     true /* always catch and report Exceptions, don't disturb EDT */, 
                                                     wait ? null : System.err);
                            if(stop) {
                                rTask.setAttachment(new Boolean(true)); // mark final task, will imply shouldStop:=true
                            }
                            // append task ..
                            edt.tasks.add(rTask);
                            edt.tasks.notifyAll();
                        }
                    } else {
                        wait = false;
                    }
                }
            }
            if( wait ) {
                try {
                    rTaskLock.wait(); // free lock, allow execution of rTask
                } catch (InterruptedException ie) {
                    throwable = ie;
                }
                if(null==throwable) {
                    throwable = rTask.getThrowable();
                }
                if(null!=throwable) {
                    if(throwable instanceof NativeWindowException) {
                        throw (NativeWindowException)throwable;
                    }
                    throw new RuntimeException(throwable);
                }
            }
        }
        if(DEBUG && stop) {
            System.err.println(Thread.currentThread()+": Default-EDT signal STOP X edt: "+edt);
        }
    }

    @Override
    final public void waitUntilIdle() {
        final EventDispatchThread _edt;
        synchronized(edtLock) {
            _edt = edt;
        }
        if(!_edt.isRunning() || _edt == Thread.currentThread()) {
            return;
        }
        synchronized(_edt.tasks) {
            while(_edt.isRunning() && _edt.tasks.size()>0) {
                try {
                    _edt.tasks.notifyAll();
                    _edt.tasks.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    final public void waitUntilStopped() {
        synchronized(edtLock) {
            if(edt.isRunning() && edt != Thread.currentThread() ) {
                while(edt.isRunning()) {
                    try {
                        edtLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    class EventDispatchThread extends Thread {
        volatile boolean shouldStop = false;
        volatile boolean isRunning = false;
        ArrayList<RunnableTask> tasks = new ArrayList<RunnableTask>(); // one shot tasks

        public EventDispatchThread(ThreadGroup tg, String name) {
            super(tg, name);
        }

        final public boolean isRunning() {
            return isRunning;
        }

        @Override
        final public void start() throws IllegalThreadStateException {
            isRunning = true;
            super.start();
        }

        private final void validateNoRecursiveLocksHold() {
            if(Lock.DEBUG) {
                if(LockDebugUtil.getRecursiveLockTrace().size()>0) {
                    LockDebugUtil.dumpRecursiveLockTrace(System.err);
                    throw new InternalError("XXX");
                }
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
            validateNoRecursiveLocksHold();
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
                        if(!shouldStop && tasks.size()==0) {
                            try {
                                tasks.wait(pollPeriod);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        // execute one task, if available
                        if(tasks.size()>0) {
                            task = tasks.remove(0);
                            tasks.notifyAll();
                            if( null != task.getAttachment() ) {
                                shouldStop = true;
                            }
                        }
                    }
                    if(null!=task) {
                        task.run();
                        validateNoRecursiveLocksHold();
                        if(!task.hasWaiter() && null != task.getThrowable()) {
                            // at least dump stack-trace in case nobody waits for result
                            System.err.println("DefaultEDT.run(): Catched exception occured on thread "+Thread.currentThread().getName()+": "+task.toString());
                            task.getThrowable().printStackTrace();
                        }
                    }
                } while(!shouldStop) ;
            } catch (Throwable t) {
                // handle errors ..
                shouldStop = true;
                if(t instanceof RuntimeException) {
                    error = (RuntimeException) t;
                } else {
                    error = new RuntimeException("Within Default-EDT", t);
                }
            } finally {
                if(DEBUG) {
                    RunnableTask rt = ( tasks.size() > 0 ) ? tasks.get(0) : null ;
                    System.err.println(getName()+": Default-EDT run() END "+ getName()+", tasks: "+tasks.size()+", "+rt+", "+error); 
                }
                synchronized(edtLock) {
                    isRunning = false;
                    edtLock.notifyAll();
                }
                if(DEBUG) {
                    System.err.println(getName()+": Default-EDT run() EXIT "+ getName()+", exception: "+error);
                }
                if(null!=error) {
                    throw error;
                }
            } // finally
        } // run()
    } // EventDispatchThread
}


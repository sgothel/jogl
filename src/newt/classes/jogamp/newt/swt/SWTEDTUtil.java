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
package jogamp.newt.swt;

import javax.media.nativewindow.NativeWindowException;

import jogamp.newt.Debug;

import com.jogamp.common.util.RunnableTask;
import com.jogamp.newt.util.EDTUtil;

/**
 * Simple {@link EDTUtil} implementation utilizing the SWT UI thread
 * of the given {@link Display}.
 */
public class SWTEDTUtil implements EDTUtil {
    public static final boolean DEBUG = Debug.debug("EDT");

    private final Object edtLock = new Object(); // locking the EDT start/stop state
    private final ThreadGroup threadGroup;
    private final String name;
    private final Runnable dispatchMessages;
    private final org.eclipse.swt.widgets.Display swtDisplay;
    private NEDT nedt = null;
    private int start_iter=0;
    private static long pollPeriod = EDTUtil.defaultEDTPollPeriod;

    public SWTEDTUtil(final com.jogamp.newt.Display newtDisplay, org.eclipse.swt.widgets.Display swtDisplay) {
        this.threadGroup = Thread.currentThread().getThreadGroup();
        this.name=Thread.currentThread().getName()+"-SWTDisplay-"+newtDisplay.getFQName()+"-EDT-";
        this.dispatchMessages = new Runnable() {
            @Override
            public void run() {
                ((jogamp.newt.DisplayImpl) newtDisplay).dispatchMessages();
            } };
        this.swtDisplay = swtDisplay;
        this.nedt = new NEDT(threadGroup, name);
        this.nedt.setDaemon(true); // don't stop JVM from shutdown ..
    }

    public final org.eclipse.swt.widgets.Display getDisplay() {
        return swtDisplay;
    }

    @Override
    public long getPollPeriod() {
        return pollPeriod;
    }

    @Override
    public void setPollPeriod(long ms) {
        pollPeriod = ms;
    }

    @Override
    public final boolean start() throws IllegalStateException {
        final boolean swtDisposed = swtDisplay.isDisposed();
        synchronized(edtLock) {
            if( nedt.isRunning() ) {
                final Thread curT = Thread.currentThread();
                final Thread swtT = !swtDisposed ? swtDisplay.getThread() : null;
                final boolean onSWTEDT = swtT == curT;
                throw new IllegalStateException("EDT still running and not subject to stop. Curr "+curT.getName()+", NEDT "+nedt.getName()+", isRunning "+nedt.isRunning+", shouldStop "+nedt.shouldStop+", SWT-EDT "+swtT.getName()+", on SWT-EDT "+onSWTEDT);
            }
            if(DEBUG) {
                System.err.println(Thread.currentThread()+": SWT-EDT reset - edt: "+nedt+", swtDisposed (skipping) "+swtDisposed);
            }
            if( !swtDisposed ) {
                if( nedt.getState() != Thread.State.NEW ) {
                    nedt = new NEDT(threadGroup, name);
                    nedt.setDaemon(true); // don't stop JVM from shutdown ..
                }
                startImpl();
            }
        }
        if( !swtDisposed ) {
            return invoke(true, nullTask);
        } else {
            return false;
        }
    }

    private final void startImpl() {
        if(nedt.isAlive()) {
            throw new RuntimeException("SWT-EDT Thread.isAlive(): true, isRunning: "+nedt.isRunning+", shouldStop "+nedt.shouldStop+", edt: "+nedt);
        }
        start_iter++;
        nedt.setName(name+start_iter);
        if(DEBUG) {
            System.err.println(Thread.currentThread()+": SWT-EDT START - edt: "+nedt);
            // Thread.dumpStack();
        }
        nedt.start();
    }

    @Override
    public boolean isCurrentThreadEDT() {
        return !swtDisplay.isDisposed() && swtDisplay.getThread() == Thread.currentThread();
    }

    @Override
    public final boolean isCurrentThreadNEDT() {
        return nedt == Thread.currentThread();
    }

    @Override
    public final boolean isCurrentThreadEDTorNEDT() {
        final Thread ct = Thread.currentThread();
        return ( !swtDisplay.isDisposed() && ct == swtDisplay.getThread() ) || ct == nedt ;
    }

    @Override
    public boolean isRunning() {
        return nedt.isRunning();
    }

    @Override
    public final boolean invokeStop(boolean wait, Runnable task) {
        return invokeImpl(wait, task, true);
    }

    @Override
    public final boolean invoke(boolean wait, Runnable task) {
        return invokeImpl(wait, task, false);
    }

    private static Runnable nullTask = new Runnable() {
        @Override
        public void run() { }
    };

    private final boolean invokeImpl(boolean wait, Runnable task, boolean stop) {
        Throwable throwable = null;
        RunnableTask rTask = null;
        final Object rTaskLock = new Object();
        synchronized(rTaskLock) { // lock the optional task execution
            synchronized(edtLock) { // lock the EDT status
                if( nedt.shouldStop ) {
                    // drop task ..
                    if(DEBUG) {
                        System.err.println(Thread.currentThread()+": Warning: SWT-EDT about (1) to stop, won't enqueue new task: "+nedt+", isRunning "+nedt.isRunning+", shouldStop "+nedt.shouldStop);
                        Thread.dumpStack();
                    }
                    return false;
                }
                if( swtDisplay.isDisposed() ) {
                    stop = true;
                }

                if( isCurrentThreadEDT() ) {
                    if(null != task) {
                        task.run();
                    }
                    wait = false; // running in same thread (EDT) -> no wait
                    if( stop ) {
                        nedt.shouldStop = true;
                    }
                } else {
                    if( !nedt.isRunning && !swtDisplay.isDisposed() ) {
                        if( null != task ) {
                            if( stop ) {
                                System.err.println(Thread.currentThread()+": Warning: SWT-EDT is about (3) to stop and stopped already, dropping task. NEDT "+nedt);
                            } else {
                                System.err.println(Thread.currentThread()+": Warning: SWT-EDT is not running, dropping task. NEDT "+nedt);
                            }
                            if(DEBUG) {
                                Thread.dumpStack();
                            }
                        }
                        return false;
                    } else if( stop ) {
                        if( nedt.isRunning ) {
                            if(DEBUG) {
                                System.err.println(Thread.currentThread()+": SWT-EDT signal STOP (on edt: "+isCurrentThreadEDT()+") - "+nedt+", isRunning "+nedt.isRunning+", shouldStop "+nedt.shouldStop);
                            }
                            synchronized(nedt.sync) {
                                nedt.shouldStop = true;
                                nedt.sync.notifyAll(); // stop immediate if waiting (poll freq)
                            }
                        }
                        if( swtDisplay.isDisposed() ) {
                            System.err.println(Thread.currentThread()+": Warning: SWT-EDT is about (3) to stop and stopped already, dropping task. "+nedt);
                            if(DEBUG) {
                                Thread.dumpStack();
                            }
                            return false;
                        }
                    }

                    if( null != task ) {
                        rTask = new RunnableTask(task,
                                                 wait ? rTaskLock : null,
                                                 true /* always catch and report Exceptions, don't disturb EDT */,
                                                 wait ? null : System.err);
                        swtDisplay.asyncExec(rTask);
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
            return true;
        }
    }

    @Override
    final public boolean waitUntilIdle() {
        final NEDT _nedt;
        synchronized(edtLock) {
            _nedt = nedt;
        }
        final Thread ct = Thread.currentThread();
        if( !_nedt.isRunning || _nedt == ct || swtDisplay.isDisposed() || swtDisplay.getThread() == ct ) {
            return false;
        }
        try {
            swtDisplay.syncExec(new Runnable() {
                @Override
                public void run() { }
            });
        } catch (Exception e) { }
        return true;
    }

    @Override
    final public boolean waitUntilStopped() {
        synchronized(edtLock) {
            final Thread curT = Thread.currentThread();
            final Thread swtT = !swtDisplay.isDisposed() ? swtDisplay.getThread() : null;
            final boolean onSWTEDT = swtT == curT;
            if( nedt.isRunning && nedt != curT && !onSWTEDT ) {
                while( nedt.isRunning ) {
                    try {
                        edtLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    class NEDT extends Thread {
        volatile boolean shouldStop = false;
        volatile boolean isRunning = false;
        Object sync = new Object();

        public NEDT(ThreadGroup tg, String name) {
            super(tg, name);
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
                System.err.println(getName()+": SWT-EDT run() START "+ getName());
            }
            RuntimeException error = null;
            try {
                do {
                    // event dispatch
                    if(!shouldStop) {
                        // EDT invoke thread is SWT-EDT,
                        // hence dispatching is required to run on SWT-EDT as well.
                        // Otherwise a deadlock may happen due to dispatched event's
                        // triggering a locking action.
                        if ( !swtDisplay.isDisposed() ) {
                            swtDisplay.syncExec(dispatchMessages);
                        } else {
                            dispatchMessages.run();
                        }
                    }
                    // wait
                    synchronized(sync) {
                        if(!shouldStop) {
                            try {
                                sync.wait(pollPeriod);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } while(!shouldStop) ;
            } catch (Throwable t) {
                // handle errors ..
                shouldStop = true;
                if(t instanceof RuntimeException) {
                    error = (RuntimeException) t;
                } else {
                    error = new RuntimeException("Within SWT-EDT", t);
                }
            } finally {
                if(DEBUG) {
                    System.err.println(getName()+": SWT-EDT run() END "+ getName()+", "+error);
                }
                synchronized(edtLock) {
                    isRunning = false;
                    edtLock.notifyAll();
                }
                if(DEBUG) {
                    System.err.println(getName()+": SWT-EDT run() EXIT "+ getName()+", exception: "+error);
                }
                if(null!=error) {
                    throw error;
                }
            } // finally
        } // run()
    } // EventDispatchThread

}

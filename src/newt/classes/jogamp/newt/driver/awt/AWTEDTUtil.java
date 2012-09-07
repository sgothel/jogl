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

package jogamp.newt.driver.awt;

import java.awt.EventQueue;

import com.jogamp.newt.util.EDTUtil;

import jogamp.common.awt.AWTEDTExecutor;
import jogamp.newt.Debug;

public class AWTEDTUtil implements EDTUtil {
    public static final boolean DEBUG = Debug.debug("EDT");
    
    private final Object edtLock = new Object(); // locking the EDT start/stop state
    private final ThreadGroup threadGroup; 
    private final String name;
    private final Runnable dispatchMessages;
    private NewtEventDispatchThread nedt = null;
    private int start_iter=0;
    private static long pollPeriod = EDTUtil.defaultEDTPollPeriod;

    public AWTEDTUtil(ThreadGroup tg, String name, Runnable dispatchMessages) {
        this.threadGroup = tg;
        this.name=Thread.currentThread().getName()+"-"+name+"-EDT-";
        this.dispatchMessages=dispatchMessages;
        this.nedt = new NewtEventDispatchThread(threadGroup, name);
        this.nedt.setDaemon(true); // don't stop JVM from shutdown ..
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
    final public void reset() {
        synchronized(edtLock) { 
            waitUntilStopped();
            if(DEBUG) {
                System.err.println(Thread.currentThread()+": EDT reset - edt: "+nedt);
            }
            this.nedt = new NewtEventDispatchThread(threadGroup, name);
            this.nedt.setDaemon(true); // don't stop JVM from shutdown ..
        }
    }

    private final void startImpl() {
        if(nedt.isAlive()) {
            throw new RuntimeException("EDT Thread.isAlive(): true, isRunning: "+nedt.isRunning()+", edt: "+nedt);
        }
        start_iter++;
        nedt.setName(name+start_iter);
        nedt.shouldStop = false;
        if(DEBUG) {
            System.err.println(Thread.currentThread()+": EDT START - edt: "+nedt);
            // Thread.dumpStack();
        }
        nedt.start();
    }

    @Override
    final public boolean isCurrentThreadEDT() {
        return EventQueue.isDispatchThread();
    }

    @Override
    public final boolean isCurrentThreadNEDT() {
        return nedt == Thread.currentThread();
    }
    
    @Override
    public final boolean isCurrentThreadEDTorNEDT() {
        return EventQueue.isDispatchThread() || nedt == Thread.currentThread();        
    }
    
    @Override
    final public boolean isRunning() {
        return nedt.isRunning() ; // AWT is always running
    }

    @Override
    public final void invokeStop(Runnable task) {
        invokeImpl(true, task, true);
    }

    @Override
    public final void invoke(boolean wait, Runnable task) {
        invokeImpl(wait, task, false);
    }
    
    private void invokeImpl(boolean wait, Runnable task, boolean stop) {
        if(task == null) {
            throw new RuntimeException("Null Runnable");
        }
        synchronized(edtLock) { // lock the EDT status
            if( nedt.shouldStop ) {
                // drop task ..
                if(DEBUG) {
                    System.err.println("Warning: EDT about (1) to stop, won't enqueue new task: "+nedt);
                    Thread.dumpStack();
                }
                return; 
            }
            // System.err.println(Thread.currentThread()+" XXX stop: "+stop+", tasks: "+edt.tasks.size()+", task: "+task);
            // Thread.dumpStack();
            if(stop) {
                nedt.shouldStop = true;
                if(DEBUG) {
                    System.err.println(Thread.currentThread()+": EDT signal STOP (on edt: "+isCurrentThreadEDT()+") - "+nedt);
                    // Thread.dumpStack();
                }
            }
            
            // start if should not stop && not started yet                    
            if( !stop && !nedt.isRunning() ) {
                startImpl();
            }
        }
        AWTEDTExecutor.singleton.invoke(wait, task);
    }    

    @Override
    final public void waitUntilIdle() {
        final NewtEventDispatchThread _edt;
        synchronized(edtLock) {
            _edt = nedt;
        }
        if(!_edt.isRunning() || EventQueue.isDispatchThread()  || _edt == Thread.currentThread()) {
            return;
        }
        try {
            AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                public void run() { }
            });
        } catch (Exception e) { }
    }

    @Override
    final public void waitUntilStopped() {
        synchronized(edtLock) {
            if(nedt.isRunning() && nedt != Thread.currentThread() ) {
                while(nedt.isRunning()) {
                    try {
                        edtLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    class NewtEventDispatchThread extends Thread {
        volatile boolean shouldStop = false;
        volatile boolean isRunning = false;
        Object sync = new Object();

        public NewtEventDispatchThread(ThreadGroup tg, String name) {
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

        /** 
         * Utilizing locking only on tasks and its execution,
         * not for event dispatching.
         */
        @Override
        final public void run() {
            if(DEBUG) {
                System.err.println(getName()+": EDT run() START "+ getName());
            }
            RuntimeException error = null;
            try {
                do {
                    // event dispatch
                    if(!shouldStop) {
                        // FIXME: Determine whether we require to run the 
                        // delivery of events (dispatch) on AWT-EDT.
                        // Since the WindowDriver itself delivers all Window related events,
                        // this shall not be required.
                        //   AWTEDTExecutor.singleton.invoke(true, dispatchMessages);
                        dispatchMessages.run();
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
                    error = new RuntimeException("Within EDT", t);
                }
            } finally {
                if(DEBUG) {
                    System.err.println(getName()+": EDT run() END "+ getName()+", "+error); 
                }
                synchronized(edtLock) {
                    isRunning = !shouldStop;
                    if(!isRunning) {
                        edtLock.notifyAll();
                    }
                }
                if(DEBUG) {
                    System.err.println(getName()+": EDT run() EXIT "+ getName()+", exception: "+error);
                }
                if(null!=error) {
                    throw error;
                }
            } // finally
        } // run()
    } // EventDispatchThread
    
}



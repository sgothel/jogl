/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.jogamp.newt.util;

import com.jogamp.common.util.RunnableTask;
import com.jogamp.newt.Display;
import com.jogamp.newt.impl.Debug;
import java.util.*;

public class DefaultEDTUtil implements EDTUtil {
    public static final boolean DEBUG = Debug.debug("EDT");

    private ThreadGroup threadGroup; 
    private volatile boolean shouldStop = false;
    private EventDispatchThread edt = null;
    private Object edtLock = new Object(); // locking the EDT start/stop state
    private ArrayList edtTasks = new ArrayList(); // one shot tasks
    private String name;
    private Runnable pumpMessages;

    public DefaultEDTUtil(ThreadGroup tg, String name, Runnable pumpMessages) {
        this.threadGroup = tg;
        this.name=new String(Thread.currentThread().getName()+"-"+"EDT-"+name);
        this.pumpMessages=pumpMessages;
        this.edt = new EventDispatchThread(threadGroup, name);
    }

    public final void reset() {
        synchronized(edtLock) { 
            waitUntilStopped();
            if(edtTasks.size()>0) {
                throw new RuntimeException("Remaining EDTTasks: "+edtTasks.size());
            }
            this.edt = new EventDispatchThread(threadGroup, name);
        }
    }

    public final void start() {
        synchronized(edtLock) { 
            if(!edt.isRunning()) {
                shouldStop = false;
                edt.start();
                if(DEBUG) {
                    System.out.println(Thread.currentThread()+": EDT START");
                }
            }
        }
    }

    public final void stop() {
        synchronized(edtLock) { 
            if(edt.isRunning()) {
                shouldStop = true;
                if(DEBUG) {
                    System.out.println(Thread.currentThread()+": EDT signal STOP");
                }
            }
            edtLock.notifyAll();
        }
    }

    public final boolean isCurrentThreadEDT() {
        return edt == Thread.currentThread();
    }

    public final boolean isRunning() {
        return !shouldStop && edt.isRunning() ;
    }

    public void invoke(boolean wait, Runnable task) {
        if(task == null) {
            return;
        }
        Throwable throwable = null;
        RunnableTask rTask = null;
        Object rTaskLock = new Object();
        synchronized(rTaskLock) { // lock the optional task execution
            synchronized(edtLock) { // lock the EDT status
                start(); // start if not started yet
                if(isRunning() && edt != Thread.currentThread() ) {
                    rTask = new RunnableTask(task, wait?rTaskLock:null, true);
                    synchronized(edtTasks) {
                        edtTasks.add(rTask);
                        edtTasks.notifyAll();
                    }
                } else {
                    // if !running or isEDTThread, do it right away
                    wait = false;
                    task.run();
                }
            }
            // wait until task finished, if requested
            // and no stop() call slipped through.
            if( wait && !shouldStop ) {
                try {
                    rTaskLock.wait();
                } catch (InterruptedException ie) {
                    throwable = ie;
                }
                if(null==throwable) {
                    throwable = rTask.getThrowable();
                }
                if(null!=throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        }
    }

    public void waitUntilIdle() {
        if(edt.isRunning() && edt != Thread.currentThread()) {
            synchronized(edtTasks) {
                while(edt.isRunning() && edtTasks.size()>0) {
                    try {
                        edtTasks.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void waitUntilStopped() {
        if(edt.isRunning() && edt != Thread.currentThread() ) {
            synchronized(edtLock) {
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
        volatile boolean isRunning = false;

        public EventDispatchThread(ThreadGroup tg, String name) {
            super(tg, name);
        }

        public final boolean isRunning() {
            return isRunning;
        }

        public void start() throws IllegalThreadStateException {
            isRunning = true;
            super.start();
        }

        /** 
         * Utilizing locking only on edtTasks and its execution,
         * not for event dispatching.
         */
        public void run() {
            if(DEBUG) {
                System.out.println(Thread.currentThread()+": EDT run() START");
            }
            try {
                do {
                    // event dispatch
                    if(!shouldStop) {
                        pumpMessages.run();
                    }
                    // wait and work on tasks
                    synchronized(edtTasks) {
                        // wait for tasks
                        while(!shouldStop && edtTasks.size()==0) {
                            try {
                                edtTasks.wait(defaultEDTPollGranularity);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        // execute one task, if available
                        if(edtTasks.size()>0) {
                            Runnable task = (Runnable) edtTasks.remove(0);
                            task.run();
                            edtTasks.notifyAll();
                        }
                    }
                } while(!shouldStop || edtTasks.size()>0) ;
            } catch (Throwable t) {
                // handle errors ..
                shouldStop = true;
                throw new RuntimeException(t);
            } finally {
                // check for tasks
                // sync for waitUntilStopped()
                synchronized(edtLock) {
                    isRunning = !shouldStop;
                    if(!isRunning) {
                        edtLock.notifyAll();
                    }
                }
                if(DEBUG) {
                    System.out.println(Thread.currentThread()+": EDT run() EXIT");
                }
            }
        }
    }
}

